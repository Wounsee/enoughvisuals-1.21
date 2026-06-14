package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.FramebufferAccessor;
import fun.enoughvisuals.mixins.accessors.ShaderProgramAccessor;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.EchoHands;
import fun.enoughvisuals.systems.modules.modules.visual.ShaderHands;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Map;

public final class ShaderHandsRenderer {

    private static final boolean FRAMEBUFFER_PASS_ENABLED = true;

    private static final ShaderProgramKey HAND_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/hand_blur"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );
    private static final ShaderProgramKey GLASS_BLUR_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/hand_glass_blur"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );
    private static final ShaderProgramKey HAND_ECHO_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/hand_echo"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    private static SimpleFramebuffer beforeHands;
    private static SimpleFramebuffer afterHands;
    private static SimpleFramebuffer glassBlurHorizontal;
    private static SimpleFramebuffer glassBlurVertical;
    private static SimpleFramebuffer echoTrail;
    private static SimpleFramebuffer echoScratch;
    private static int width;
    private static int height;
    private static boolean beforeCaptured;
    private static boolean afterCaptured;
    private static long lastGlassBlurMs;
    private static float lastGlassBlurRadius = -1.0f;
    private static int cachedGlassBlurTexture;
    private static final long GLASS_BLUR_INTERVAL_MS = 16L;
    private static boolean echoCameraReady;
    private static float echoLastYaw;
    private static float echoLastPitch;
    private static float echoSmoothShiftX;
    private static float echoSmoothShiftY;
    private static float echoVelocityX;
    private static float echoVelocityY;

    private ShaderHandsRenderer() {
    }

    public static void beginHudFrame() {
        beforeCaptured = false;
        afterCaptured = false;
    }

    public static void captureBeforeHands() {
        if (!FRAMEBUFFER_PASS_ENABLED) {
            return;
        }

        if (!handEffectsActive() || beforeCaptured || !isFirstPersonReady()) {
            return;
        }

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        if (prepare(main)) {
            copyFramebuffer(main, beforeHands);
            beforeCaptured = true;
        }
    }

    public static void captureAfterHands() {
        if (!FRAMEBUFFER_PASS_ENABLED) {
            return;
        }

        if (!handEffectsActive() || !beforeCaptured || !isFirstPersonReady()) {
            return;
        }

        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        if (prepare(main)) {
            copyFramebuffer(main, afterHands);
            afterCaptured = true;
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        renderInternal(context.getMatrices().peek().getPositionMatrix(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }

    public static void renderDirect() {
        if (!FRAMEBUFFER_PASS_ENABLED) {
            reset();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Matrix4f matrix = new Matrix4f().ortho(0.0f, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), 0.0f, 1000.0f, 3000.0f);
        renderInternal(matrix, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }

    private static void renderInternal(Matrix4f matrix, int screenWidth, int screenHeight) {
        ShaderHands module = ShaderHands.active();
        EchoHands echo = EchoHands.active();
        if ((module == null && echo == null) || !beforeCaptured || !afterCaptured || beforeHands == null || afterHands == null) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isFirstPersonReady()) {
            return;
        }

        if (module != null) {
            renderShaderHands(matrix, screenWidth, screenHeight, module);
        }

        if (echo != null) {
            renderEchoHands(matrix, screenWidth, screenHeight, echo);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getFramebuffer().beginWrite(true);
        beginHudFrame();
    }

    private static void renderShaderHands(Matrix4f matrix, int screenWidth, int screenHeight, ShaderHands module) {
        int blurredBackground = module.isGlassMode()
                ? renderGlassBackgroundBlur(matrix, screenWidth, screenHeight, module.glassBlur.get().floatValue())
                : beforeHands.getColorAttachment();

        ShaderProgram shader = RenderSystem.setShader(HAND_SHADER);
        if (shader == null) {
            return;
        }

        int color = module.color.get();
        float a = 1.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        int glassColor = module.glassColor.get();
        float glassR = ((glassColor >> 16) & 0xFF) / 255.0f;
        float glassG = ((glassColor >> 8) & 0xFF) / 255.0f;
        float glassB = (glassColor & 0xFF) / 255.0f;
        float glassA = Math.min(0.65f, ((glassColor >> 24) & 0xFF) / 255.0f);
        float glowStrength = Math.min(1.0f, Math.max(0.0f, module.glowStrength.get().floatValue()));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, afterHands.getColorAttachment());
        RenderSystem.setShaderTexture(1, blurredBackground);
        RenderSystem.setShaderTexture(2, beforeHands.getColorAttachment());

        setUniform(shader, "BlurRadius", ShaderHands.RADIUS);
        setUniform(shader, "Threshold", ShaderHands.THRESHOLD);
        setUniform(shader, "TintColor", r, g, b, a);
        setUniform(shader, "GlassColor", glassR, glassG, glassB, glassA);
        setUniform(shader, "Mode", module.modeIndex());
        setUniform(shader, "GlassBlur", module.glassBlur.get().floatValue());
        setUniform(shader, "GlassZoom", module.glassZoom.get().floatValue());
        setUniform(shader, "GlowStrength", glowStrength);
        setUniform(shader, "UseItemColor", module.itemColor.get() ? 1 : 0);
        setUniform(shader, "Time", (System.currentTimeMillis() % 100_000L) / 1000.0f);
        setUniform(shader, "WebOpacity", module.webOpacity.get().floatValue());
        setUniform(shader, "WebGlow", module.webGlow.get().floatValue());
        setUniform(shader, "FlowEnabled", module.flowOutline.get() ? 1 : 0);
        setUniform(shader, "FlowSpeed", module.flowSpeed.get().floatValue());
        setUniform(shader, "FlowStrength", module.flowStrength.get().floatValue());

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, screenHeight, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, 0.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, screenHeight, 0.0f).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderEchoHands(Matrix4f matrix, int screenWidth, int screenHeight, EchoHands module) {
        if (echoTrail == null || echoScratch == null) {
            return;
        }

        ShaderProgram shader = RenderSystem.setShader(HAND_ECHO_SHADER);
        if (shader == null) {
            return;
        }

        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        echoScratch.beginWrite(true);
        RenderSystem.setShaderTexture(0, afterHands.getColorAttachment());
        RenderSystem.setShaderTexture(1, beforeHands.getColorAttachment());
        RenderSystem.setShaderTexture(2, echoTrail.getColorAttachment());
        float[] cameraShift = updateEchoCameraShift(module);
        setEchoUniforms(shader, module, 0, cameraShift[0], cameraShift[1]);
        drawFullscreenQuad(matrix, screenWidth, screenHeight);

        SimpleFramebuffer previousTrail = echoTrail;
        echoTrail = echoScratch;
        echoScratch = previousTrail;

        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        shader = RenderSystem.setShader(HAND_ECHO_SHADER);
        if (shader == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, afterHands.getColorAttachment());
        RenderSystem.setShaderTexture(1, beforeHands.getColorAttachment());
        RenderSystem.setShaderTexture(2, echoTrail.getColorAttachment());
        setEchoUniforms(shader, module, 1, cameraShift[0], cameraShift[1]);
        drawFullscreenQuad(matrix, screenWidth, screenHeight);
        RenderSystem.defaultBlendFunc();
    }

    private static float[] updateEchoCameraShift(EchoHands module) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            echoCameraReady = false;
            return new float[]{0.0F, 0.0F};
        }

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (!echoCameraReady) {
            echoCameraReady = true;
            echoLastYaw = yaw;
            echoLastPitch = pitch;
            return new float[]{0.0F, 0.0F};
        }

        float yawDelta = MathHelper.wrapDegrees(yaw - echoLastYaw);
        float pitchDelta = pitch - echoLastPitch;
        echoLastYaw = yaw;
        echoLastPitch = pitch;

        float lag = module.lag.get().floatValue();
        float targetX = (float) MathHelper.clamp(yawDelta * 0.0020F * lag, -0.065F, 0.065F);
        float targetY = (float) MathHelper.clamp(-pitchDelta * 0.0020F * lag, -0.065F, 0.065F);

        echoVelocityX += (targetX - echoSmoothShiftX) * 0.18F;
        echoVelocityY += (targetY - echoSmoothShiftY) * 0.18F;
        echoVelocityX *= 0.74F;
        echoVelocityY *= 0.74F;

        echoSmoothShiftX = (float) MathHelper.clamp((echoSmoothShiftX + echoVelocityX) * 0.92F, -0.075F, 0.075F);
        echoSmoothShiftY = (float) MathHelper.clamp((echoSmoothShiftY + echoVelocityY) * 0.92F, -0.075F, 0.075F);
        return new float[]{echoSmoothShiftX, echoSmoothShiftY};
    }

    private static void setEchoUniforms(
            ShaderProgram shader,
            EchoHands module,
            int compositeMode,
            float cameraShiftX,
            float cameraShiftY
    ) {
        int color = module.color.get();
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        setUniform(shader, "Threshold", ShaderHands.THRESHOLD);
        setUniform(shader, "Radius", module.radius.get().floatValue());
        setUniform(shader, "Strength", module.strength.get().floatValue());
        setUniform(shader, "TrailDecay", module.trail.get().floatValue());
        setUniform(shader, "Lag", module.lag.get().floatValue());
        setUniform(shader, "EchoColor", r, g, b, a);
        setUniform(shader, "Time", (System.currentTimeMillis() % 100_000L) / 1000.0f);
        setUniform(shader, "CompositeMode", compositeMode);
        setUniform(shader, "CameraShift", cameraShiftX, cameraShiftY);
    }

    private static int renderGlassBackgroundBlur(Matrix4f matrix, int screenWidth, int screenHeight, float radius) {
        if (CustomTheme.blurDisabled() || radius < 0.01f || glassBlurHorizontal == null || glassBlurVertical == null) {
            return beforeHands.getColorAttachment();
        }

        long now = System.currentTimeMillis();
        if (cachedGlassBlurTexture != 0
                && Math.abs(lastGlassBlurRadius - radius) < 0.01f
                && now - lastGlassBlurMs < GLASS_BLUR_INTERVAL_MS) {
            return cachedGlassBlurTexture;
        }

        ShaderProgram shader = RenderSystem.setShader(GLASS_BLUR_SHADER);
        if (shader == null) {
            return beforeHands.getColorAttachment();
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        glassBlurHorizontal.beginWrite(true);
        setUniform(shader, "TexelSize", 1.0f / beforeHands.textureWidth, 1.0f / beforeHands.textureHeight);
        setUniform(shader, "Direction", 1.0f, 0.0f);
        setUniform(shader, "Radius", radius);
        RenderSystem.setShaderTexture(0, beforeHands.getColorAttachment());
        drawFullscreenQuad(matrix, screenWidth, screenHeight);

        glassBlurVertical.beginWrite(true);
        shader = RenderSystem.setShader(GLASS_BLUR_SHADER);
        if (shader == null) {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            return beforeHands.getColorAttachment();
        }
        setUniform(shader, "TexelSize", 1.0f / glassBlurHorizontal.textureWidth, 1.0f / glassBlurHorizontal.textureHeight);
        setUniform(shader, "Direction", 0.0f, 1.0f);
        setUniform(shader, "Radius", radius * 0.5f);
        RenderSystem.setShaderTexture(0, glassBlurHorizontal.getColorAttachment());
        drawFullscreenQuad(matrix, screenWidth, screenHeight);

        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        cachedGlassBlurTexture = glassBlurVertical.getColorAttachment();
        lastGlassBlurRadius = radius;
        lastGlassBlurMs = now;
        return cachedGlassBlurTexture;
    }

    private static void drawFullscreenQuad(Matrix4f matrix, int screenWidth, int screenHeight) {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, screenHeight, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, 0.0f, 0.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, 0.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(matrix, screenWidth, screenHeight, 0.0f).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void reset() {
        if (beforeHands != null) {
            beforeHands.delete();
            beforeHands = null;
        }
        if (afterHands != null) {
            afterHands.delete();
            afterHands = null;
        }
        if (glassBlurHorizontal != null) {
            glassBlurHorizontal.delete();
            glassBlurHorizontal = null;
        }
        if (glassBlurVertical != null) {
            glassBlurVertical.delete();
            glassBlurVertical = null;
        }
        resetEcho();
        width = 0;
        height = 0;
        beforeCaptured = false;
        afterCaptured = false;
        lastGlassBlurMs = 0L;
        lastGlassBlurRadius = -1.0f;
        cachedGlassBlurTexture = 0;
    }

    public static void resetEcho() {
        if (echoTrail != null) {
            echoTrail.delete();
            echoTrail = null;
        }
        if (echoScratch != null) {
            echoScratch.delete();
            echoScratch = null;
        }
        echoCameraReady = false;
        echoLastYaw = 0.0F;
        echoLastPitch = 0.0F;
        echoSmoothShiftX = 0.0F;
        echoSmoothShiftY = 0.0F;
        echoVelocityX = 0.0F;
        echoVelocityY = 0.0F;
    }

    private static boolean handEffectsActive() {
        return ShaderHands.active() != null || EchoHands.active() != null;
    }

    private static boolean isFirstPersonReady() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null
                && mc.world != null
                && mc.getFramebuffer() != null
                && mc.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private static boolean prepare(Framebuffer main) {
        if (main == null || main.textureWidth <= 0 || main.textureHeight <= 0) {
            return false;
        }

        if (beforeHands != null && afterHands != null && glassBlurHorizontal != null && glassBlurVertical != null
                && echoTrail != null && echoScratch != null
                && width == main.textureWidth && height == main.textureHeight) {
            return true;
        }

        reset();
        width = main.textureWidth;
        height = main.textureHeight;
        beforeHands = new SimpleFramebuffer(width, height, false);
        afterHands = new SimpleFramebuffer(width, height, false);
        glassBlurHorizontal = new SimpleFramebuffer(width, height, false);
        glassBlurVertical = new SimpleFramebuffer(width, height, false);
        echoTrail = new SimpleFramebuffer(width, height, false);
        echoScratch = new SimpleFramebuffer(width, height, false);
        beforeHands.setTexFilter(GL11.GL_LINEAR);
        afterHands.setTexFilter(GL11.GL_LINEAR);
        glassBlurHorizontal.setTexFilter(GL11.GL_LINEAR);
        glassBlurVertical.setTexFilter(GL11.GL_LINEAR);
        echoTrail.setTexFilter(GL11.GL_LINEAR);
        echoScratch.setTexFilter(GL11.GL_LINEAR);
        clearFramebuffer(echoTrail);
        clearFramebuffer(echoScratch);
        return true;
    }

    private static void copyFramebuffer(Framebuffer from, Framebuffer to) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, ((FramebufferAccessor) from).getFbo());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((FramebufferAccessor) to).getFbo());
        GL30.glBlitFramebuffer(
                0, 0, from.textureWidth, from.textureHeight,
                0, 0, to.textureWidth, to.textureHeight,
                GL11.GL_COLOR_BUFFER_BIT,
                GL11.GL_NEAREST
        );

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
        from.beginWrite(true);
    }

    private static void clearFramebuffer(Framebuffer framebuffer) {
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        float[] clearColor = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        framebuffer.beginWrite(false);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
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
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static GlUniform getUniform(ShaderProgram shader, String name) {
        try {
            Map<String, GlUniform> map = ((ShaderProgramAccessor) shader).getUniformsByName();
            if (map != null && map.containsKey(name)) {
                return map.get(name);
            }
        } catch (Exception ignored) {
        }
        return shader.getUniform(name);
    }
}
