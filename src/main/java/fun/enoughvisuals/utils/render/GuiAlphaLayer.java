package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.port.FramebufferUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Applies opacity to renderers which ignore {@link RenderSystem#setShaderColor}.
 *
 * <p>Items and entity models select their own cutout/glint render layers, so a global
 * shader color cannot reliably fade them. During a fade this helper renders the whole
 * callback into a transparent depth framebuffer and composites that texture once with
 * vertex alpha. The ordinary direct path stays allocation- and copy-free at full opacity.</p>
 */
public final class GuiAlphaLayer {
    private static final float DIRECT_ALPHA = 0.997f;
    private static final float MIN_ALPHA = 0.003f;

    /**
     * One target per nesting level. A single reusable FBO is not sufficient:
     * an entity/card renderer can itself invoke another alpha-layer and clear
     * the still-live parent texture.
     */
    private static final ThreadLocal<LayerPool> LAYERS = ThreadLocal.withInitial(LayerPool::new);

    private GuiAlphaLayer() {
    }

    public static void render(DrawContext context, float alpha, Runnable renderer) {
        float opacity = Math.max(0f, Math.min(1f, alpha));
        if (opacity <= MIN_ALPHA) return;
        if (opacity >= DIRECT_ALPHA) {
            renderer.run();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer destination = client.getFramebuffer();
        if (destination == null || destination.textureWidth <= 0 || destination.textureHeight <= 0) return;

        context.draw();

        /*
         * Capture before acquiring the pooled target. Constructing/resizing a
         * SimpleFramebuffer binds its FBO and finishes by binding framebuffer 0.
         * Capturing afterwards therefore loses the actual GUI destination on the
         * first fading frame and the composite is drawn into the default buffer,
         * producing the black/tiny closing frame seen in Cosmetics.
         */
        FramebufferUtils.GlStateSnapshot renderState = FramebufferUtils.captureGlState();
        LayerPool pool = LAYERS.get();
        SimpleFramebuffer alphaLayer;
        try {
            alphaLayer = pool.acquire(destination.textureWidth, destination.textureHeight);
        } catch (RuntimeException | Error failure) {
            restoreState(renderState);
            throw failure;
        }
        boolean framebufferSwapped = false;
        boolean callbackStarted = false;
        boolean callbackFlushed = false;
        try {
            // A parent HUD/card scissor must not limit the transparent clear. It is
            // re-applied immediately afterwards for the actual callback geometry.
            GlStateManager._disableScissorTest();
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._depthMask(true);
            alphaLayer.setClearColor(0f, 0f, 0f, 0f);
            alphaLayer.clear();

            // Item glint, entity and cosmetic RenderLayers resolve their output
            // through MinecraftClient#getFramebuffer while their buffers flush.
            // Merely binding the FBO here is therefore insufficient: a layer can
            // silently bind the ordinary GUI framebuffer again. Swap the logical
            // client target for the entire isolated pass, exactly as the ESP mask
            // renderer does, and restore the precise prior target in finally.
            ((MinecraftClientAccessor) client).enough$setFramebuffer(alphaLayer);
            framebufferSwapped = true;
            alphaLayer.beginWrite(true);
            applyRenderFlags(renderState);

            callbackStarted = true;
            renderer.run();
            context.draw();
            callbackFlushed = true;
        } catch (RuntimeException | Error failure) {
            // Do not leave vertices queued after restoring the main framebuffer:
            // a later DrawContext flush would otherwise leak partial opaque/glint
            // geometry into it. Best-effort drain while the alpha target is active.
            if (callbackStarted && !callbackFlushed) {
                try {
                    context.draw();
                } catch (RuntimeException | Error drainFailure) {
                    failure.addSuppressed(drainFailure);
                }
            }
            throw failure;
        } finally {
            try {
                if (framebufferSwapped) {
                    ((MinecraftClientAccessor) client).enough$setFramebuffer(destination);
                }
            } finally {
                try {
                    restoreState(renderState);
                } finally {
                    pool.release();
                }
            }
        }

        FramebufferUtils.GlStateSnapshot compositeState = FramebufferUtils.captureGlState();
        try {
            drawComposite(alphaLayer.getColorAttachment(), opacity,
                    client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        } finally {
            restoreState(compositeState);
        }
    }

    private static final class LayerPool {
        private final List<LayerSlot> slots = new ArrayList<>();
        private int depth;

        private SimpleFramebuffer acquire(int width, int height) {
            int index = depth;
            while (slots.size() <= index) slots.add(new LayerSlot());
            SimpleFramebuffer result = slots.get(index).ensure(width, height);
            depth++;
            return result;
        }

        private void release() {
            if (depth <= 0) throw new IllegalStateException("GuiAlphaLayer scope underflow");
            depth--;
        }
    }

    private static final class LayerSlot {
        private SimpleFramebuffer framebuffer;
        private int width;
        private int height;

        private SimpleFramebuffer ensure(int requestedWidth, int requestedHeight) {
            if (framebuffer != null && width == requestedWidth && height == requestedHeight) {
                return framebuffer;
            }
            if (framebuffer != null) framebuffer.delete();
            framebuffer = null;
            width = 0;
            height = 0;

            SimpleFramebuffer created = new SimpleFramebuffer(requestedWidth, requestedHeight, true);
            try {
                created.setTexFilter(GL11.GL_NEAREST);
                framebuffer = created;
                width = requestedWidth;
                height = requestedHeight;
                return created;
            } catch (RuntimeException | Error failure) {
                created.delete();
                throw failure;
            }
        }
    }

    private static void drawComposite(int texture, float alpha, int width, int height) {
        GlStateManager._enableBlend();
        // Rendering translucent geometry into a transparent framebuffer produces
        // premultiplied RGB. Tint RGB and alpha together, then composite with ONE
        // to avoid applying the texture alpha a second time around soft edges.
        GlStateManager._blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        GlStateManager._colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        int channel = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        int color = (channel << 24) | (channel << 16) | (channel << 8) | channel;
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        // Framebuffer textures are vertically inverted relative to GUI space.
        buffer.vertex(0f, 0f, 0f).texture(0f, 1f).color(color);
        buffer.vertex(0f, height, 0f).texture(0f, 0f).color(color);
        buffer.vertex(width, height, 0f).texture(1f, 0f).color(color);
        buffer.vertex(width, 0f, 0f).texture(1f, 1f).color(color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    /** Re-applies the caller's pipeline flags while keeping the alpha FBO bound. */
    private static void applyRenderFlags(FramebufferUtils.GlStateSnapshot state) {
        if (state.flag4) GlStateManager._enableBlend();
        else GlStateManager._disableBlend();
        if (state.flag2) GlStateManager._enableDepthTest();
        else GlStateManager._disableDepthTest();
        if (state.flag3) GlStateManager._enableCull();
        else GlStateManager._disableCull();
        GlStateManager._blendFuncSeparate(state.intValue5, state.intValue6,
                state.intValue7, state.intValue8);
        GlStateManager._colorMask(state.flag6, state.flag7, state.flag8, state.flag9);
        GlStateManager._depthMask(state.flag10);
        if (state.flag) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(state.ints2[0], state.ints2[1], state.ints2[2], state.ints2[3]);
        } else {
            GlStateManager._disableScissorTest();
        }
    }

    private static void restoreState(FramebufferUtils.GlStateSnapshot state) {
        FramebufferUtils.restoreGlState(state);
        /*
         * FramebufferUtils restores framebuffer bindings through raw LWJGL calls.
         * GlStateManager caches DRAW/READ bindings independently, so without this
         * bridge its cache still points at the alpha layer. On the next Cosmetics
         * pass SimpleFramebuffer.clear() then sees a cache hit, skips the bind and
         * clears the real GUI target instead (black close frame with stale models).
         * Rebinding the already-restored ids through Mojang's wrapper keeps both
         * the driver and its cache in agreement.
         */
        syncFramebufferBindings(state.intValue, state.intValue2,
                FramebufferUtils::compute, GlStateManager::_glBindFramebuffer);
        // FramebufferUtils restores raw GL values. Mirror them into Mojang's state
        // cache as well, otherwise later RenderSystem calls may incorrectly no-op.
        GlStateManager._depthMask(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        GlStateManager._depthFunc(GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
        if (GL11.glIsEnabled(GL11.GL_DEPTH_TEST)) GlStateManager._enableDepthTest();
        else GlStateManager._disableDepthTest();
        if (GL11.glIsEnabled(GL11.GL_BLEND)) GlStateManager._enableBlend();
        else GlStateManager._disableBlend();
        if (GL11.glIsEnabled(GL11.GL_CULL_FACE)) GlStateManager._enableCull();
        else GlStateManager._disableCull();
        GlStateManager._blendFuncSeparate(
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
            GlStateManager._colorMask(mask.get(0) != 0, mask.get(1) != 0,
                    mask.get(2) != 0, mask.get(3) != 0);
        }
    }

    static void syncFramebufferBindings(int capturedDraw, int capturedRead,
                                        IntUnaryOperator validator, FramebufferBinder binder) {
        int drawFramebuffer = validator.applyAsInt(capturedDraw);
        int readFramebuffer = capturedRead == capturedDraw
                ? drawFramebuffer
                : validator.applyAsInt(capturedRead);
        binder.bind(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        binder.bind(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
    }

    @FunctionalInterface
    interface FramebufferBinder {
        void bind(int target, int framebuffer);
    }
}
