package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.FramebufferAccessor;
import fun.enoughvisuals.mixins.accessors.ShaderProgramAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.ShaderESP;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;
import java.util.Map;

public final class ShaderEspRenderer {

    private static final int GL_BLEND_EQUATION = 0x8009;
    private static final ShaderProgramKey BLUR_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/hand_glass_blur"),
            VertexFormats.POSITION_COLOR,
            net.minecraft.client.gl.Defines.EMPTY
    );
    private static final ShaderProgramKey ESP_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/shader_esp"),
            VertexFormats.POSITION_COLOR,
            net.minecraft.client.gl.Defines.EMPTY
    );

    private static SimpleFramebuffer mask;
    private static SimpleFramebuffer blurHorizontal;
    private static SimpleFramebuffer blurVertical;
    private static SimpleFramebuffer echoTrail;
    private static SimpleFramebuffer echoScratch;
    private static int width;
    private static int height;
    private static boolean frameReady;
    private static boolean captured;
    private static boolean depthCopied;
    private static long lastRenderNanos;

    private ShaderEspRenderer() {
    }

    public static void beginWorldFrame(WorldRenderContext context) {
        ShaderESP module = ShaderESP.active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || client.world == null || client.player == null || client.getFramebuffer() == null) {
            frameReady = false;
            captured = false;
            return;
        }

        if (!prepare(client.getFramebuffer())) {
            frameReady = false;
            return;
        }

        clearFramebuffer(mask, true);
        frameReady = true;
        captured = false;
        depthCopied = false;
    }

    public static void capturePlayer(
            PlayerEntityRenderState state,
            MatrixStack matrices,
            Model model,
            List<FeatureRenderer<?, ?>> features,
            Identifier texture
    ) {
        ShaderESP module = ShaderESP.active();
        if (module == null || !frameReady || state.invisible || !module.shouldRender(state.id, state.squaredDistanceToCamera)) {
            return;
        }

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        try {
            if (!depthCopied) {
                copyDepth(previousDraw, mask);
                depthCopied = true;
            }

            mask.beginWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            try (BufferAllocator allocator = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE)) {
                VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);
                VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
                model.render(
                        matrices,
                        consumer,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE,
                        OverlayTexture.DEFAULT_UV,
                        0xFFFFFFFF
                );
                renderFeatures(matrices, consumers, state, features);
                consumers.draw();
            }
            captured = true;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            stateSnapshot.restore();
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        render();
    }

    private static void render() {
        ShaderESP module = ShaderESP.active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || !frameReady || !captured || client.getFramebuffer() == null) {
            return;
        }
        if (!prepare(client.getFramebuffer())) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        Matrix4f matrix = new Matrix4f().ortho(0.0F, screenWidth, screenHeight, 0.0F, 1000.0F, 3000.0F);

        RenderStateSnapshot stateSnapshot = RenderStateSnapshot.capture();
        try {
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            renderBlur(matrix, screenWidth, screenHeight, module.glowRadius.get().floatValue());

            if (module.mode.is("Echo")) {
                updateEcho(matrix, screenWidth, screenHeight, module);
            } else {
                clearFramebuffer(echoTrail, false);
                clearFramebuffer(echoScratch, false);
                lastRenderNanos = System.nanoTime();
            }

            client.getFramebuffer().beginWrite(true);
            ShaderProgram shader = RenderSystem.setShader(ESP_SHADER);
            if (shader == null) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, mask.getColorAttachment());
            RenderSystem.setShaderTexture(1, blurVertical.getColorAttachment());
            RenderSystem.setShaderTexture(2, echoTrail.getColorAttachment());
            setEspUniforms(shader, module, 1, 1.0F);
            drawFullscreenQuad(matrix, screenWidth, screenHeight);
        } finally {
            client.getFramebuffer().beginWrite(true);
            stateSnapshot.restore();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            frameReady = false;
        }
    }

    private static void renderBlur(Matrix4f matrix, int screenWidth, int screenHeight, float radius) {
        ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER);
        if (shader == null) return;

        blurHorizontal.beginWrite(true);
        RenderSystem.setShaderTexture(0, mask.getColorAttachment());
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "Direction", 1.0F, 0.0F);
        setUniform(shader, "Radius", radius);
        drawFullscreenQuad(matrix, screenWidth, screenHeight);

        blurVertical.beginWrite(true);
        shader = RenderSystem.setShader(BLUR_SHADER);
        if (shader == null) return;
        RenderSystem.setShaderTexture(0, blurHorizontal.getColorAttachment());
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "Direction", 0.0F, 1.0F);
        setUniform(shader, "Radius", radius);
        drawFullscreenQuad(matrix, screenWidth, screenHeight);
    }

    private static void updateEcho(Matrix4f matrix, int screenWidth, int screenHeight, ShaderESP module) {
        long now = System.nanoTime();
        float deltaSeconds = lastRenderNanos == 0L
                ? 1.0F / 60.0F
                : MathHelper.clamp((now - lastRenderNanos) / 1_000_000_000.0F, 1.0F / 240.0F, 0.1F);
        lastRenderNanos = now;
        float decay = (float) Math.pow(module.echoDecay.get(), deltaSeconds * 60.0F);

        echoScratch.beginWrite(true);
        ShaderProgram shader = RenderSystem.setShader(ESP_SHADER);
        if (shader == null) return;
        RenderSystem.setShaderTexture(0, mask.getColorAttachment());
        RenderSystem.setShaderTexture(1, blurVertical.getColorAttachment());
        RenderSystem.setShaderTexture(2, echoTrail.getColorAttachment());
        setEspUniforms(shader, module, 0, decay);
        drawFullscreenQuad(matrix, screenWidth, screenHeight);

        SimpleFramebuffer previous = echoTrail;
        echoTrail = echoScratch;
        echoScratch = previous;
    }

    private static void setEspUniforms(ShaderProgram shader, ShaderESP module, int pass, float echoDecay) {
        int fill = module.fillColor.get();
        int glow = module.glowColor.get();
        setUniform(shader, "FillColor",
                ((fill >> 16) & 0xFF) / 255.0F,
                ((fill >> 8) & 0xFF) / 255.0F,
                (fill & 0xFF) / 255.0F,
                1.0F);
        setUniform(shader, "GlowColor",
                ((glow >> 16) & 0xFF) / 255.0F,
                ((glow >> 8) & 0xFF) / 255.0F,
                (glow & 0xFF) / 255.0F,
                1.0F);
        setUniform(shader, "TexelSize", 1.0F / width, 1.0F / height);
        setUniform(shader, "FillOpacity", module.fillOpacity.get().floatValue());
        setUniform(shader, "GlowStrength", module.glowStrength.get().floatValue());
        setUniform(shader, "OutlineStrength", module.outlineStrength.get().floatValue());
        setUniform(shader, "OutlineWidth", module.outlineWidth.get().floatValue());
        setUniform(shader, "EchoDecay", echoDecay);
        setUniform(shader, "Mode", module.modeIndex());
        setUniform(shader, "Pass", pass);
    }

    private static boolean prepare(Framebuffer main) {
        if (main.textureWidth <= 0 || main.textureHeight <= 0) return false;
        if (mask != null && width == main.textureWidth && height == main.textureHeight) return true;

        reset();
        width = main.textureWidth;
        height = main.textureHeight;
        mask = new SimpleFramebuffer(width, height, true);
        blurHorizontal = new SimpleFramebuffer(width, height, false);
        blurVertical = new SimpleFramebuffer(width, height, false);
        echoTrail = new SimpleFramebuffer(width, height, false);
        echoScratch = new SimpleFramebuffer(width, height, false);
        mask.setTexFilter(GL11.GL_LINEAR);
        blurHorizontal.setTexFilter(GL11.GL_LINEAR);
        blurVertical.setTexFilter(GL11.GL_LINEAR);
        echoTrail.setTexFilter(GL11.GL_LINEAR);
        echoScratch.setTexFilter(GL11.GL_LINEAR);
        clearFramebuffer(mask, true);
        clearFramebuffer(echoTrail, false);
        clearFramebuffer(echoScratch, false);
        return true;
    }

    private static void copyDepth(int sourceFbo, Framebuffer destination) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((FramebufferAccessor) destination).getFbo());
        GL30.glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
    }

    private static void clearFramebuffer(Framebuffer framebuffer, boolean depth) {
        if (framebuffer == null) return;
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        float[] clearColor = new float[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        framebuffer.beginWrite(false);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | (depth ? GL11.GL_DEPTH_BUFFER_BIT : 0));
        GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    private static void drawFullscreenQuad(Matrix4f matrix, int screenWidth, int screenHeight) {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, 0.0F, screenHeight, 0.0F).color(0xFFFFFFFF);
        builder.vertex(matrix, 0.0F, 0.0F, 0.0F).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, 0.0F, 0.0F).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, screenHeight, 0.0F).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void reset() {
        if (mask != null) mask.delete();
        if (blurHorizontal != null) blurHorizontal.delete();
        if (blurVertical != null) blurVertical.delete();
        if (echoTrail != null) echoTrail.delete();
        if (echoScratch != null) echoScratch.delete();
        mask = null;
        blurHorizontal = null;
        blurVertical = null;
        echoTrail = null;
        echoScratch = null;
        width = 0;
        height = 0;
        frameReady = false;
        captured = false;
        depthCopied = false;
        lastRenderNanos = 0L;
    }

    private static void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = getUniform(shader, name);
        if (uniform == null) return;
        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    private static void setUniform(ShaderProgram shader, String name, int value) {
        GlUniform uniform = getUniform(shader, name);
        if (uniform != null) uniform.set(value);
    }

    private static GlUniform getUniform(ShaderProgram shader, String name) {
        try {
            Map<String, GlUniform> uniforms = ((ShaderProgramAccessor) shader).getUniformsByName();
            return uniforms == null ? null : uniforms.get(name);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderFeatures(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate consumers,
            PlayerEntityRenderState state,
            List<FeatureRenderer<?, ?>> features
    ) {
        for (FeatureRenderer feature : features) {
            feature.render(
                    matrices,
                    consumers,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    state,
                    state.yawDegrees,
                    state.pitch
            );
        }
    }

    private record RenderStateSnapshot(
            boolean blend,
            boolean depthTest,
            boolean cull,
            boolean depthMask,
            int depthFunc,
            int blendSrcRgb,
            int blendDstRgb,
            int blendSrcAlpha,
            int blendDstAlpha,
            int blendEquation,
            ShaderProgram shader,
            int texture0,
            int texture1,
            int texture2,
            float[] shaderColor
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
            if (blend) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (cull) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }

            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendEquation(blendEquation);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (shader != null) {
                RenderSystem.setShader(shader);
            }
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.setShaderTexture(1, texture1);
            RenderSystem.setShaderTexture(2, texture2);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }
}
