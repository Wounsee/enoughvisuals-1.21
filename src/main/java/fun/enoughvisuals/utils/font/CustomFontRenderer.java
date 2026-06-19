package fun.enoughvisuals.utils.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.ShaderProgramAccessor;
import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class CustomFontRenderer {

    public enum FontWeight {
        REGULAR("regular"),
        BOLD("bold"),
        LIGHT("comfortaa_light"),
        SEMIBOLD("semibold"),
        MARK("mark"),
        EVENT("event"),
        DRAG("drag"),
        POTIONS("potions"),
        BINDS("binds"),
        HEART("heart"),
        CLIENT("client"),
        WP("wp"),
        VC("vc"),
        LOGO("logo"),
        TIME("time");

        final String atlasName;

        FontWeight(String atlasName) {
            this.atlasName = atlasName;
        }
    }

    private static final class Glyph {
        final int unicode;
        final float advance;
        final boolean hasPlaneBounds;
        final float left;
        final float bottom;
        final float right;
        final float top;
        final boolean hasAtlasBounds;
        final float u0;
        final float v0;
        final float u1;
        final float v1;

        private Glyph(int unicode,
                      float advance,
                      boolean hasPlaneBounds,
                      float left,
                      float bottom,
                      float right,
                      float top,
                      boolean hasAtlasBounds,
                      float u0,
                      float v0,
                      float u1,
                      float v1) {
            this.unicode = unicode;
            this.advance = advance;
            this.hasPlaneBounds = hasPlaneBounds;
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            this.top = top;
            this.hasAtlasBounds = hasAtlasBounds;
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
        }
    }

    private static final class MsdfAtlas {
        final Identifier textureId;
        final float distanceRange;
        final float ascender;
        final float lineHeight;
        final Map<Integer, Glyph> glyphs;
        final Map<Long, Float> kerningPairs;
        boolean textureConfigured = false;

        private MsdfAtlas(Identifier textureId,
                          float distanceRange,
                          float ascender,
                          float lineHeight,
                          Map<Integer, Glyph> glyphs,
                          Map<Long, Float> kerningPairs) {
            this.textureId = textureId;
            this.distanceRange = distanceRange;
            this.ascender = ascender;
            this.lineHeight = lineHeight;
            this.glyphs = glyphs;
            this.kerningPairs = kerningPairs;
        }
    }

    private static final Map<FontWeight, MsdfAtlas> ATLASES = new EnumMap<>(FontWeight.class);
    private static boolean initialized = false;
    private static boolean failedInit = false;
    private static ShaderProgramKey msdfShaderKey;

    public static void init() {
        if (initialized || failedInit) return;

        try {
            msdfShaderKey = new ShaderProgramKey(
                    Identifier.of("enough", "core/enough_msdf"),
                    VertexFormats.POSITION_TEXTURE,
                    Defines.EMPTY
            );

            for (FontWeight weight : FontWeight.values()) {
                MsdfAtlas atlas = loadAtlas(weight);
                if (atlas == null) {
                    System.err.println("[CustomFont] Failed to load MSDF atlas: " + weight.atlasName);
                    failedInit = true;
                    ATLASES.clear();
                    return;
                }
                ATLASES.put(weight, atlas);
            }

            initialized = true;
            System.out.println("[CustomFont] MSDF atlases loaded successfully");
        } catch (Exception e) {
            System.err.println("[CustomFont] MSDF init failed:");
            e.printStackTrace();
            failedInit = true;
            ATLASES.clear();
        }
    }

    private static MsdfAtlas loadAtlas(FontWeight weight) {
        String basePath = "/assets/enough/fonts/msdf/" + weight.atlasName;
        if (CustomFontRenderer.class.getResourceAsStream(basePath + ".json") == null) {
            basePath = "/assets/enough/icons/msdf/" + weight.atlasName;
        }

        String jsonPath = basePath + ".json";

        try (InputStream is = CustomFontRenderer.class.getResourceAsStream(jsonPath)) {
            if (is == null) return null;

            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject atlasObj = root.getAsJsonObject("atlas");
            JsonObject metricsObj = root.getAsJsonObject("metrics");

            if (atlasObj == null || metricsObj == null) return null;

            float distanceRange = getFloat(atlasObj, "distanceRange", 4f);
            float atlasWidth = getFloat(atlasObj, "width", 1f);
            float atlasHeight = getFloat(atlasObj, "height", 1f);
            float ascender = getFloat(metricsObj, "ascender", 0.8f);
            float lineHeight = getFloat(metricsObj, "lineHeight", 1.0f);

            Map<Integer, Glyph> glyphs = new HashMap<>();
            JsonArray glyphArray = root.getAsJsonArray("glyphs");
            if (glyphArray != null) {
                for (JsonElement element : glyphArray) {
                    if (!element.isJsonObject()) continue;
                    JsonObject glyphObj = element.getAsJsonObject();

                    int unicode = glyphObj.has("unicode") ? glyphObj.get("unicode").getAsInt() : -1;
                    if (unicode < 0) continue;

                    float advance = getFloat(glyphObj, "advance", 0f);

                    boolean hasPlaneBounds = glyphObj.has("planeBounds") && glyphObj.get("planeBounds").isJsonObject();
                    float left = 0f;
                    float bottom = 0f;
                    float right = 0f;
                    float top = 0f;
                    if (hasPlaneBounds) {
                        JsonObject plane = glyphObj.getAsJsonObject("planeBounds");
                        left = getFloat(plane, "left", 0f);
                        bottom = getFloat(plane, "bottom", 0f);
                        right = getFloat(plane, "right", 0f);
                        top = getFloat(plane, "top", 0f);
                    }

                    boolean hasAtlasBounds = glyphObj.has("atlasBounds") && glyphObj.get("atlasBounds").isJsonObject();
                    float u0 = 0f;
                    float v0 = 0f;
                    float u1 = 0f;
                    float v1 = 0f;
                    if (hasAtlasBounds) {
                        JsonObject bounds = glyphObj.getAsJsonObject("atlasBounds");
                        float leftPx = getFloat(bounds, "left", 0f);
                        float bottomPx = getFloat(bounds, "bottom", 0f);
                        float rightPx = getFloat(bounds, "right", 0f);
                        float topPx = getFloat(bounds, "top", 0f);

                        u0 = leftPx / atlasWidth;
                        u1 = rightPx / atlasWidth;
                        v0 = 1f - (topPx / atlasHeight);
                        v1 = 1f - (bottomPx / atlasHeight);
                    }

                    glyphs.put(unicode, new Glyph(
                            unicode, advance,
                            hasPlaneBounds, left, bottom, right, top,
                            hasAtlasBounds, u0, v0, u1, v1
                    ));
                }
            }

            Map<Long, Float> kerningPairs = new HashMap<>();
            JsonArray kerningArray = root.getAsJsonArray("kerning");
            if (kerningArray != null) {
                for (JsonElement element : kerningArray) {
                    if (!element.isJsonObject()) continue;
                    JsonObject kernObj = element.getAsJsonObject();
                    int first = kernObj.has("unicode1") ? kernObj.get("unicode1").getAsInt() : -1;
                    int second = kernObj.has("unicode2") ? kernObj.get("unicode2").getAsInt() : -1;
                    float advance = getFloat(kernObj, "advance", 0f);
                    if (first >= 0 && second >= 0 && advance != 0f) {
                        kerningPairs.put(packPair(first, second), advance);
                    }
                }
            }

            Identifier textureId = Identifier.of("enough", "fonts/msdf/" + weight.atlasName + ".png");
            return new MsdfAtlas(textureId, distanceRange, ascender, lineHeight, glyphs, kerningPairs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static float getFloat(JsonObject object, String name, float fallback) {
        if (object == null || !object.has(name)) return fallback;
        try {
            return object.get(name).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long packPair(int first, int second) {
        return (((long) first) << 32) | (second & 0xFFFFFFFFL);
    }

    private static MsdfAtlas getAtlas(FontWeight weight) {
        MsdfAtlas atlas = ATLASES.get(weight);
        if (atlas != null) return atlas;
        return ATLASES.get(FontWeight.REGULAR);
    }

    public static void drawText(DrawContext ctx, String text, float x, float y,
                                float size, int color, FontWeight weight) {
        text = NameProtect.protect(text);
        if (text == null || text.isEmpty()) return;

        if (!initialized || failedInit || msdfShaderKey == null) {
            drawVanillaFallback(ctx, text, x, y, size, color);
            return;
        }

        MsdfAtlas atlas = getAtlas(weight);
        if (atlas == null) {
            drawVanillaFallback(ctx, text, x, y, size, color);
            return;
        }

        ShaderProgram shader;
        try {
            shader = RenderSystem.setShader(msdfShaderKey);
        } catch (Exception e) {
            drawVanillaFallback(ctx, text, x, y, size, color);
            return;
        }
        if (shader == null) {
            drawVanillaFallback(ctx, text, x, y, size, color);
            return;
        }

        ensureTextureFiltering(atlas);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.001f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, atlas.textureId);

        setUniform(shader, "uColor", r, g, b, a);
        setUniform(shader, "uPxRange", atlas.distanceRange);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        float lineHeightPx = atlas.lineHeight * size;
        float baselineY = y + atlas.ascender * size;
        float penX = x;
        float startX = x;
        int previousCodePoint = -1;
        boolean wroteAny = false;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (codePoint == '\n') {
                penX = startX;
                baselineY += lineHeightPx;
                previousCodePoint = -1;
                continue;
            }

            Glyph glyph = atlas.glyphs.get(codePoint);
            if (glyph == null) glyph = atlas.glyphs.get((int) '?');
            if (glyph == null) continue;

            if (previousCodePoint >= 0) {
                Float kern = atlas.kerningPairs.get(packPair(previousCodePoint, codePoint));
                if (kern != null) penX += kern * size;
            }

            if (glyph.hasPlaneBounds && glyph.hasAtlasBounds) {
                float x0 = penX + glyph.left * size;
                float x1 = penX + glyph.right * size;
                float y0 = baselineY - glyph.top * size;
                float y1 = baselineY - glyph.bottom * size;

                buffer.vertex(matrix, x0, y1, 0).texture(glyph.u0, glyph.v1);
                buffer.vertex(matrix, x1, y1, 0).texture(glyph.u1, glyph.v1);
                buffer.vertex(matrix, x1, y0, 0).texture(glyph.u1, glyph.v0);
                buffer.vertex(matrix, x0, y0, 0).texture(glyph.u0, glyph.v0);
                wroteAny = true;
            }

            penX += glyph.advance * size;
            previousCodePoint = codePoint;
        }

        var builtBuffer = buffer.endNullable();
        if (wroteAny && builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.disableBlend();
    }

    public static void drawText(DrawContext ctx, String text, float x, float y, float size) {
        drawText(ctx, text, x, y, size, 0xFFFFFFFF, FontWeight.REGULAR);
    }

    public static void drawText(DrawContext ctx, String text, float x, float y, float size, int color) {
        drawText(ctx, text, x, y, size, color, FontWeight.REGULAR);
    }

    public static void drawCenteredText(DrawContext ctx, String text, float centerX, float y,
                                        float size, int color, FontWeight weight) {
        float w = getTextWidth(text, size, weight);
        drawText(ctx, text, centerX - w / 2f, y, size, color, weight);
    }

    public static void drawCenteredText(DrawContext ctx, String text, float centerX, float y,
                                        float size, int color) {
        drawCenteredText(ctx, text, centerX, y, size, color, FontWeight.REGULAR);
    }

    public static void drawRightText(DrawContext ctx, String text, float rightX, float y,
                                     float size, int color, FontWeight weight) {
        float w = getTextWidth(text, size, weight);
        drawText(ctx, text, rightX - w, y, size, color, weight);
    }

    public static void drawRightText(DrawContext ctx, String text, float rightX, float y,
                                     float size, int color) {
        drawRightText(ctx, text, rightX, y, size, color, FontWeight.REGULAR);
    }

    public static void drawTextWithShadow(DrawContext ctx, String text, float x, float y,
                                          float size, int color, FontWeight weight) {
        float off = Math.max(0.5f, size / 18f);
        drawText(ctx, text, x + off, y + off, size, 0x80000000, weight);
        drawText(ctx, text, x, y, size, color, weight);
    }

    public static void drawTextWithShadow(DrawContext ctx, String text, float x, float y,
                                          float size, int color) {
        drawTextWithShadow(ctx, text, x, y, size, color, FontWeight.REGULAR);
    }

    public static float getTextWidth(String text, float size, FontWeight weight) {
        text = NameProtect.protect(text);
        if (text == null || text.isEmpty()) return 0f;

        if (!initialized || failedInit) {
            return vanillaWidth(text, size);
        }

        MsdfAtlas atlas = getAtlas(weight);
        if (atlas == null) {
            return vanillaWidth(text, size);
        }

        float maxWidth = 0f;
        float lineWidth = 0f;
        int previousCodePoint = -1;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (codePoint == '\n') {
                maxWidth = Math.max(maxWidth, lineWidth);
                lineWidth = 0f;
                previousCodePoint = -1;
                continue;
            }

            Glyph glyph = atlas.glyphs.get(codePoint);
            if (glyph == null) glyph = atlas.glyphs.get((int) '?');
            if (glyph == null) continue;

            if (previousCodePoint >= 0) {
                Float kern = atlas.kerningPairs.get(packPair(previousCodePoint, codePoint));
                if (kern != null) lineWidth += kern * size;
            }

            lineWidth += glyph.advance * size;
            previousCodePoint = codePoint;
        }

        return Math.max(maxWidth, lineWidth);
    }

    public static float getTextWidth(String text, float size) {
        return getTextWidth(text, size, FontWeight.REGULAR);
    }

    public static float getTextHeight(float size, FontWeight weight) {
        MsdfAtlas atlas = getAtlas(weight);
        if (atlas != null) return atlas.lineHeight * size;
        return size;
    }

    public static float getTextHeight(float size) {
        return getTextHeight(size, FontWeight.REGULAR);
    }

    public static String trimToWidth(String text, float maxWidth, float size, FontWeight weight) {
        text = NameProtect.protect(text);
        if (text == null || text.isEmpty()) return "";
        if (getTextWidth(text, size, weight) <= maxWidth) return text;

        String dots = "...";
        float dotsW = getTextWidth(dots, size, weight);
        for (int len = text.length() - 1; len > 0; len--) {
            String candidate = text.substring(0, len);
            if (getTextWidth(candidate, size, weight) + dotsW <= maxWidth) {
                return candidate + dots;
            }
        }
        return dots;
    }

    public static String trimToWidth(String text, float maxWidth, float size) {
        return trimToWidth(text, maxWidth, size, FontWeight.REGULAR);
    }

    public static boolean isReady() {
        return initialized && !failedInit;
    }

    public static boolean isMsdfReady() {
        return isReady();
    }

    public static void cleanup() {
        ATLASES.clear();
        initialized = false;
        failedInit = false;
        msdfShaderKey = null;
    }

    private static void ensureTextureFiltering(MsdfAtlas atlas) {
        if (atlas.textureConfigured) return;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            AbstractTexture texture = mc.getTextureManager().getTexture(atlas.textureId);
            if (texture != null) {
                texture.setFilter(true, false);
            }
            atlas.textureConfigured = true;
        } catch (Exception ignored) {
        }
    }

    private static void setUniform(ShaderProgram program, String name, float... values) {
        if (program == null) return;
        GlUniform uniform = null;
        try {
            Map<String, GlUniform> map = ((ShaderProgramAccessor) program).getUniformsByName();
            if (map != null) uniform = map.get(name);
        } catch (Exception ignored) {
        }
        if (uniform == null) {
            try {
                uniform = program.getUniform(name);
            } catch (Exception ignored) {
            }
        }
        if (uniform == null) return;

        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> {
            }
        }
    }

    private static float vanillaWidth(String text, float size) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text) * (size / 9f);
    }

    private static void drawVanillaFallback(DrawContext ctx, String text, float x, float y,
                                            float size, int color) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float scale = size / 9f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(mc.textRenderer, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }
}
