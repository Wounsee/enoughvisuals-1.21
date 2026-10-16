package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.FramebufferAccessor;
import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.ShaderPlayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Captures the models targeted by {@link ShaderPlayer} into an isolated mask
 * framebuffer while the world renders, then hands the mask to
 * {@link ShaderPlayerRenderer} for the glow and smoke passes.
 */
public final class ShaderEspRenderer {
    private static final int GL_BLEND_EQUATION = 0x8009;
    private static final int[] VIEWPORT = new int[4];

    private static SimpleFramebuffer mask;
    private static int width;
    private static int height;
    private static boolean frameReady;
    private static boolean captured;
    private static boolean depthCopied;
    private static ShaderPlayer frameModule;
    private static IsolatedEntityBuffers buffers;

    private ShaderEspRenderer() {
    }

    public static void beginWorldFrame(WorldRenderContext context) {
        ShaderPlayer module = ShaderPlayer.active();
        MinecraftClient client = MinecraftClient.getInstance();
        frameReady = false;
        captured = false;
        frameModule = null;
        if (module == null || client.world == null || client.player == null || client.getFramebuffer() == null) {
            return;
        }
        if (!prepare(client.getFramebuffer())) return;

        clearMask();
        frameReady = true;
        depthCopied = false;
        frameModule = module;
    }

    public static void captureLiving(
            LivingEntityRenderState state,
            MatrixStack matrices,
            Model model,
            List<FeatureRenderer<?, ?>> features,
            Identifier texture
    ) {
        ShaderPlayer module = frameModule;
        if (module == null || !frameReady || state.invisible) return;
        if (state.squaredDistanceToCamera > module.distanceSquared()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (state instanceof PlayerEntityRenderState player) {
            boolean self = player.id == client.player.getId();
            if (self ? !module.self() : !module.targets.isSelected(ShaderPlayer.PLAYERS)) return;
        } else if (!module.targets.isSelected(ShaderPlayer.MOBS)) {
            return;
        }

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);
        int viewportX = VIEWPORT[0], viewportY = VIEWPORT[1], viewportW = VIEWPORT[2], viewportH = VIEWPORT[3];
        Framebuffer originalMain = client.getFramebuffer();
        boolean mainFramebufferSwapped = false;
        try {
            if (!depthCopied) {
                copyDepth(previousDraw, mask);
                depthCopied = true;
            }

            // Entity layers bind MinecraftClient#getFramebuffer when they flush. Held items and
            // armor use other layers, and switching layers flushes the model buffer mid-capture,
            // so the mask must be the client framebuffer for the whole capture, not just the end.
            ((MinecraftClientAccessor) client).enough$setFramebuffer(mask);
            mainFramebufferSwapped = true;
            mask.beginWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            if (buffers == null) buffers = new IsolatedEntityBuffers();
            VertexConsumerProvider.Immediate consumers = buffers.consumers;
            VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
            model.render(matrices, consumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
            for (FeatureRenderer feature : features) {
                feature.render(matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                        state, state.limbFrequency, state.limbAmplitudeMultiplier);
            }
            mask.beginWrite(false);
            consumers.draw();
            captured = true;
        } finally {
            if (mainFramebufferSwapped) {
                ((MinecraftClientAccessor) client).enough$setFramebuffer(originalMain);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glViewport(viewportX, viewportY, viewportW, viewportH);
            stateSnapshot.restore();
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        ShaderPlayer module = frameModule;
        MinecraftClient client = MinecraftClient.getInstance();
        frameReady = false;
        frameModule = null;
        if (module == null || client.getFramebuffer() == null) return;
        // Smoke keeps fading out after the last model left the frame.
        if (!captured && !module.smoke()) return;
        if (!prepare(client.getFramebuffer())) return;

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        try {
            ShaderPlayerRenderer.render(mask, client.getFramebuffer(), module.settings());
        } finally {
            client.getFramebuffer().beginWrite(true);
            stateSnapshot.restore();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static boolean prepare(Framebuffer main) {
        if (main.textureWidth <= 0 || main.textureHeight <= 0) return false;
        if (mask != null && width == main.textureWidth && height == main.textureHeight) return true;

        if (mask != null) mask.delete();
        width = main.textureWidth;
        height = main.textureHeight;
        mask = new SimpleFramebuffer(width, height, true);
        mask.setTexFilter(GL11.GL_LINEAR);
        clearMask();
        return true;
    }

    private static void copyDepth(int sourceFbo, Framebuffer destination) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((FramebufferAccessor) destination).getFbo());
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
    }

    private static void clearMask() {
        if (mask == null) return;
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);
        int viewportX = VIEWPORT[0], viewportY = VIEWPORT[1], viewportW = VIEWPORT[2], viewportH = VIEWPORT[3];
        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        mask.beginWrite(false);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        GL11.glViewport(viewportX, viewportY, viewportW, viewportH);
    }

    public static void reset() {
        if (mask != null) mask.delete();
        mask = null;
        if (buffers != null) buffers.close();
        buffers = null;
        width = 0;
        height = 0;
        frameReady = false;
        captured = false;
        depthCopied = false;
        frameModule = null;
        ShaderPlayerRenderer.close();
    }

    /**
     * Armor glint returns a dual consumer that requests two layers before
     * writing either; dedicated allocators keep both builders alive. The
     * allocators are reused across frames instead of reallocated per entity.
     */
    private static final class IsolatedEntityBuffers implements AutoCloseable {
        private final BufferAllocator fallback = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE);
        private final LinkedHashMap<RenderLayer, BufferAllocator> fixed = new LinkedHashMap<>();
        private final VertexConsumerProvider.Immediate consumers;

        private IsolatedEntityBuffers() {
            add(RenderLayer.getArmorEntityGlint());
            add(RenderLayer.getGlint());
            add(RenderLayer.getGlintTranslucent());
            add(RenderLayer.getEntityGlint());
            consumers = VertexConsumerProvider.immediate(fixed, fallback);
        }

        private void add(RenderLayer layer) {
            fixed.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }

        @Override
        public void close() {
            for (BufferAllocator allocator : fixed.values()) allocator.close();
            fallback.close();
        }
    }

    private record RenderStateSnapshot(
            boolean blend, boolean depthTest, boolean cull, boolean depthMask, int depthFunc,
            int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha, int blendEquation,
            ShaderProgram shader, int texture0, int texture1, int texture2, float[] shaderColor
    ) {
        private static RenderStateSnapshot capture() {
            return new RenderStateSnapshot(
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL_BLEND_EQUATION),
                    RenderSystem.getShader(),
                    RenderSystem.getShaderTexture(0),
                    RenderSystem.getShaderTexture(1),
                    RenderSystem.getShaderTexture(2),
                    RenderSystem.getShaderColor().clone()
            );
        }

        private void restore() {
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendEquation(blendEquation);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (shader != null) RenderSystem.setShader(shader);
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.setShaderTexture(1, texture1);
            RenderSystem.setShaderTexture(2, texture2);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }
}
