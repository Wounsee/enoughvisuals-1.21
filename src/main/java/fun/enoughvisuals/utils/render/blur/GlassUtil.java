package fun.enoughvisuals.utils.render.blur;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.ShaderProgramAccessor;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GlassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("blurnishiy");

    private static boolean initialized = false;
    private static boolean initFailed = false;

    private static ShaderProgramKey kawaseDownKey = null;
    private static ShaderProgramKey kawaseUpKey = null;

    private static ShaderProgramKey glassPanelKey = null;

    private static SimpleFramebuffer[] fboChain = null;

    private static int resultTexture = 0;
    private static boolean frameReady = false;

    public static float BLUR_OFFSET = 1.0f;
    public static int CHAIN_LENGTH = 2;

    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static int lastChainLength = 0;
    private static float lastBlurOffset = 0;

    private static int frameCounter = 0;

    private static void init() {
        if (initialized || initFailed) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getShaderLoader() == null) return;

        try {
            kawaseDownKey = new ShaderProgramKey(
                    Identifier.of("enough", "core/blur/kawase_down"),
                    VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
            kawaseUpKey = new ShaderProgramKey(
                    Identifier.of("enough", "core/blur/kawase_up"),
                    VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
            glassPanelKey = new ShaderProgramKey(
                    Identifier.of("enough", "core/blur/glass_panel"),
                    VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

            ShaderLoader loader = mc.getShaderLoader();
            ShaderProgram down = loader.getProgramToLoad(kawaseDownKey);
            ShaderProgram up = loader.getProgramToLoad(kawaseUpKey);
            ShaderProgram panel = loader.getProgramToLoad(glassPanelKey);

            if (down == null || up == null) {
                LOGGER.error("blur shaders null! down={}, up={}", down, up);
                initFailed = true;
                return;
            }

            if (panel == null) {
                LOGGER.warn("shader panel not found");
                                   
            }

            initialized = true;
        } catch (Exception e) {
            LOGGER.error("failed to init blur", e);
            initFailed = true;
        }
    }

    public static boolean isReady() {
        return initialized && frameReady;
    }

    public static void skipFrame() {
        frameReady = false;
    }

    public static void beginFrame() {
        if (CustomTheme.blurDisabled() || BLUR_OFFSET <= 0.01f) {
            frameReady = false;
            cleanup();
            return;
        }

        if (!initialized) init();
        if (!initialized) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer mainFbo = mc.getFramebuffer();
            if (mainFbo == null) return;

            int w = mainFbo.textureWidth;
            int h = mainFbo.textureHeight;
            if (w <= 0 || h <= 0) return;

            int screenTex = mainFbo.getColorAttachment();
            if (screenTex <= 0) {
                frameReady = false;
                return;
            }

            ensureFBOs(w, h);

            boolean wasDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();

            int sourceTexture = screenTex;
            int sourceWidth = w;
            int sourceHeight = h;
            int passes = Math.max(1, CHAIN_LENGTH);
            for (int i = 0; i < passes; i++) {
                fboChain[0].beginWrite(true);
                ShaderProgram horizontal = RenderSystem.setShader(kawaseDownKey);
                if (horizontal == null) {
                    restoreState(mainFbo, wasDepthTest, wasBlend);
                    return;
                }
                setUniform(horizontal, "Resolution", 1.0f / sourceWidth, 1.0f / sourceHeight);
                setUniform(horizontal, "Offset", BLUR_OFFSET);
                RenderSystem.setShaderTexture(0, sourceTexture);
                drawFullscreenQuad(fboChain[0].textureWidth, fboChain[0].textureHeight);

                fboChain[1].beginWrite(true);
                ShaderProgram vertical = RenderSystem.setShader(kawaseUpKey);
                if (vertical == null) {
                    restoreState(mainFbo, wasDepthTest, wasBlend);
                    return;
                }
                setUniform(vertical, "Resolution", 1.0f / fboChain[0].textureWidth, 1.0f / fboChain[0].textureHeight);
                setUniform(vertical, "Offset", BLUR_OFFSET);
                RenderSystem.setShaderTexture(0, fboChain[0].getColorAttachment());
                drawFullscreenQuad(fboChain[1].textureWidth, fboChain[1].textureHeight);

                sourceTexture = fboChain[1].getColorAttachment();
                sourceWidth = fboChain[1].textureWidth;
                sourceHeight = fboChain[1].textureHeight;
            }

            resultTexture = fboChain[1].getColorAttachment();
            frameReady = true;
            lastBlurOffset = BLUR_OFFSET;

            restoreState(mainFbo, wasDepthTest, wasBlend);
            frameCounter++;

        } catch (Exception e) {
            LOGGER.error("beginFrame() failed", e);
            frameReady = false;
            initFailed = true;
            initialized = false;
            cleanup();
        }
    }

    private static void restoreState(Framebuffer mainFbo,
                                     boolean depthTest, boolean blend) {
        mainFbo.beginWrite(true);
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
    }

    public static void drawBlurPanel(DrawContext ctx, float x, float y, float w, float h, float radius) {
        drawBlurPanel(ctx, x, y, w, h, radius, 1f);
    }

    public static void drawBlurPanel(DrawContext ctx, float x, float y, float w, float h,
                                     float radius, float blurAlpha) {
        drawGlassPanel(ctx, x, y, w, h, radius, 0x000000, 0f, blurAlpha);
    }

    public static void drawScreenBackgroundBlur(DrawContext ctx, float width, float height) {
        if (width <= 0f || height <= 0f) return;

        float blurOffset = screenBackgroundBlurLevel();
        boolean blurEnabled = !CustomTheme.blurDisabled() && blurOffset > 0.01f;
        BLUR_OFFSET = blurOffset;

        if (blurEnabled) {
            beginFrame();
        } else {
            skipFrame();
        }

        float tintAlpha = 0.12f * CustomTheme.clickGuiOpacity();
        drawGlassPanel(ctx, 0f, 0f, width, height, 0f,
                0x000000, tintAlpha, blurEnabled ? 1f : 0f);
    }

    private static float screenBackgroundBlurLevel() {
        CustomTheme theme = ModuleManager.getInstance().get(CustomTheme.class);
        if (theme == null || !theme.isEnabled()) {
            return 1.25f;
        }
        return CustomTheme.clickGuiBlurLevel();
    }

    public static void drawGlassPanel(DrawContext ctx, float x, float y, float w, float h,
                                      float radius, int tintColor, float tintAlpha) {
        drawGlassPanel(ctx, x, y, w, h, radius, tintColor, tintAlpha, 1f);
    }

    public static void drawGlassPanel(DrawContext ctx, float x, float y, float w, float h,
                                      float radius, int tintColor, float tintAlpha, float blurAlpha) {
        float blur = Math.max(0f, Math.min(1f, blurAlpha));
        if (blur <= 0.001f && tintAlpha <= 0.001f) return;

        if (CustomTheme.blurDisabled()) {
            drawStaticPanel(ctx, x, y, w, h, radius, tintColor, tintAlpha, blur);
            return;
        }

        if (!frameReady || resultTexture == 0) {
            drawFallbackPanel(ctx, x, y, w, h, radius, tintColor, tintAlpha, blur);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        float guiScale = (float) mc.getWindow().getScaleFactor();

        float texW = lastWidth;
        float texH = lastHeight;

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        float[] bounds = transformedBounds(mat, x, y, w, h);
        float px = bounds[0] * guiScale;
        float py = bounds[1] * guiScale;
        float pw = (bounds[2] - bounds[0]) * guiScale;
        float ph = (bounds[3] - bounds[1]) * guiScale;

        float u0 = px / texW;
        float u1 = (px + pw) / texW;
        float v0 = 1.0f - py / texH;
        float v1 = 1.0f - (py + ph) / texH;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (radius > 0.5f && glassPanelKey != null) {
            ShaderProgram panelShader = RenderSystem.setShader(glassPanelKey);
            if (panelShader != null) {
                setUniform(panelShader, "PanelSize", w, h);
                setUniform(panelShader, "Radius", radius);
                setUniform(panelShader, "BlurAlpha", blur);
                float cr = ((tintColor >> 16) & 0xFF) / 255f;
                float cg = ((tintColor >> 8) & 0xFF) / 255f;
                float cb = (tintColor & 0xFF) / 255f;
                setUniform(panelShader, "TintColor", cr, cg, cb, tintAlpha);

                RenderSystem.setShaderTexture(0, resultTexture);
                if (blur > 0.001f) {
                    drawPanelQuad(mat, x, y, w, h, u0, v0, u1, v1);
                }
                if (tintAlpha > 0.001f) {
                    RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius,
                            RenderUtil.withAlpha(tintColor, tintAlpha));
                }
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                return;
            }
        }

        if (radius > 0.5f) {
            RenderUtil.enableScissor(x, y, w, h);
            drawBlurQuad(mat, x, y, w, h, u0, v0, u1, v1);
            RenderUtil.disableScissor();
            RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius,
                    RenderUtil.withAlpha(tintColor, tintAlpha));
        } else {
            drawBlurQuad(mat, x, y, w, h, u0, v0, u1, v1);
            drawTintQuad(mat, x, y, w, h, tintColor, tintAlpha);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void drawFallbackPanel(DrawContext ctx, float x, float y, float w, float h,
                                          float radius, int tintColor, float tintAlpha, float blurAlpha) {
        float adjustedAlpha = Math.min(1f, (tintAlpha + 0.15f) * blurAlpha);
        if (adjustedAlpha <= 0.001f) return;
        try {
            int color = RenderUtil.withAlpha(tintColor, adjustedAlpha);
            RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius, color);
        } catch (Exception e) {
            int a = (int) (adjustedAlpha * 255);
            int r = (tintColor >> 16) & 0xFF;
            int g = (tintColor >> 8) & 0xFF;
            int b = tintColor & 0xFF;
            ctx.fill((int) x, (int) y, (int) (x + w), (int) (y + h),
                    (a << 24) | (r << 16) | (g << 8) | b);
        }
    }

    private static void drawStaticPanel(DrawContext ctx, float x, float y, float w, float h,
                                        float radius, int tintColor, float tintAlpha, float blurAlpha) {
        float alpha = tintAlpha > 0.001f ? tintAlpha : Math.min(0.18f, blurAlpha * 0.14f);
        if (alpha <= 0.001f) return;
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius, RenderUtil.withAlpha(tintColor, alpha));
    }

    private static void drawPanelQuad(Matrix4f mat, float x, float y, float w, float h,
                                      float u0, float v0, float u1, float v1) {
        BufferBuilder builder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Кодируем LocalCoord (X, Y) от 0.0f до 1.0f в RG каналы цвета
        int c00 = packLocalCoord(0f, 0f); // Левый верхний
        int c01 = packLocalCoord(0f, 1f); // Левый нижний
        int c11 = packLocalCoord(1f, 1f); // Правый нижний
        int c10 = packLocalCoord(1f, 0f); // Правый верхний

        builder.vertex(mat, x, y, 0)        .texture(u0, v0).color(c00);
        builder.vertex(mat, x, y + h, 0)    .texture(u0, v1).color(c01);
        builder.vertex(mat, x + w, y + h, 0).texture(u1, v1).color(c11);
        builder.vertex(mat, x + w, y, 0)    .texture(u1, v0).color(c10);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static int packLocalCoord(float lx, float ly) {
        int r = (int) (lx * 255f);
        int g = (int) (ly * 255f);
        return 0xFF000000 | (r << 16) | (g << 8) | 0xFF;
    }

    private static float[] transformedBounds(Matrix4f mat, float x, float y, float w, float h) {
        Vector3f p0 = mat.transformPosition(x, y, 0f, new Vector3f());
        Vector3f p1 = mat.transformPosition(x + w, y, 0f, new Vector3f());
        Vector3f p2 = mat.transformPosition(x + w, y + h, 0f, new Vector3f());
        Vector3f p3 = mat.transformPosition(x, y + h, 0f, new Vector3f());
        float minX = Math.min(Math.min(p0.x, p1.x), Math.min(p2.x, p3.x));
        float minY = Math.min(Math.min(p0.y, p1.y), Math.min(p2.y, p3.y));
        float maxX = Math.max(Math.max(p0.x, p1.x), Math.max(p2.x, p3.x));
        float maxY = Math.max(Math.max(p0.y, p1.y), Math.max(p2.y, p3.y));
        return new float[]{ minX, minY, maxX, maxY };
    }

    private static void drawBlurQuad(Matrix4f mat, float x, float y, float w, float h,
                                     float u0, float v0, float u1, float v1) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, resultTexture);

        BufferBuilder builder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(mat, x, y, 0)        .texture(u0, v0).color(0xFFFFFFFF);
        builder.vertex(mat, x, y + h, 0)    .texture(u0, v1).color(0xFFFFFFFF);
        builder.vertex(mat, x + w, y + h, 0).texture(u1, v1).color(0xFFFFFFFF);
        builder.vertex(mat, x + w, y, 0)    .texture(u1, v0).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void drawTintQuad(Matrix4f mat, float x, float y, float w, float h,
                                     int tintColor, float tintAlpha) {
        if (tintAlpha <= 0.001f) return;
        float cr = ((tintColor >> 16) & 0xFF) / 255f;
        float cg = ((tintColor >> 8) & 0xFF) / 255f;
        float cb = (tintColor & 0xFF) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder tint = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        tint.vertex(mat, x, y, 0)        .color(cr, cg, cb, tintAlpha);
        tint.vertex(mat, x, y + h, 0)    .color(cr, cg, cb, tintAlpha);
        tint.vertex(mat, x + w, y + h, 0).color(cr, cg, cb, tintAlpha);
        tint.vertex(mat, x + w, y, 0)    .color(cr, cg, cb, tintAlpha);
        BufferRenderer.drawWithGlobalProgram(tint.end());
    }

    private static void drawFullscreenQuad(int fbW, int fbH) {

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        BufferBuilder builder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(0, 0, 0)     .texture(0.0f, 1.0f).color(0xFFFFFFFF);
        builder.vertex(0, sh, 0)    .texture(0.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(sw, sh, 0)   .texture(1.0f, 0.0f).color(0xFFFFFFFF);
        builder.vertex(sw, 0, 0)    .texture(1.0f, 1.0f).color(0xFFFFFFFF);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void setUniform(ShaderProgram prog, String name, float... values) {
        if (prog == null) return;
        GlUniform uniform = null;
        try {
            Map<String, GlUniform> map = ((ShaderProgramAccessor) prog).getUniformsByName();
            if (map != null) uniform = map.get(name);
        } catch (Exception ignored) {}
        if (uniform == null) {
            try {
                uniform = prog.getUniform(name);
            } catch (Exception ignored) {}
        }
        if (uniform == null) return;
        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
        }
    }


    private static void ensureFBOs(int w, int h) {
        if (lastWidth == w && lastHeight == h
                && lastChainLength == CHAIN_LENGTH && fboChain != null) return;

        cleanup();
        lastWidth = w;
        lastHeight = h;
        lastChainLength = CHAIN_LENGTH;

        fboChain = new SimpleFramebuffer[2];
        int fbW = Math.max(1, w / 2);
        int fbH = Math.max(1, h / 2);
        for (int i = 0; i < fboChain.length; i++) {
            fboChain[i] = new SimpleFramebuffer(fbW, fbH, false);
            fboChain[i].setTexFilter(GL11.GL_LINEAR);
        }

    
    }

    public static void cleanup() {
        if (fboChain != null) {
            for (SimpleFramebuffer fbo : fboChain) {
                if (fbo != null) fbo.delete();
            }
            fboChain = null;
        }
        resultTexture = 0;
        frameReady = false;
        lastWidth = 0;
        lastHeight = 0;
        lastChainLength = 0;
        lastBlurOffset = 0f;
    }
}
