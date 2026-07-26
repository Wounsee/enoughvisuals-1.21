package fun.enoughvisuals.ui.clickgui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Locale;

/**
 * Floating HSB color editor used by the click GUI.
 *
 * <p>The popup keeps its own HSB cursor so a chosen hue is not lost when the
 * saturation reaches zero. RGB writes always retain the setting's current
 * alpha channel; the alpha channel is only changed by its dedicated control.</p>
 */
public final class ColorPickerPopup {

    private static final long OPEN_MS = 240L;
    private static final long CLOSE_MS = 180L;

    private static final float BASE_W = 134f;
    private static final float BASE_H_OPAQUE = 164f;
    private static final float BASE_H_ALPHA = 182f;
    private static final float PAD = 8f;
    private static final float SV_SIZE = 101f;
    private static final float HUE_GAP = 6f;
    private static final float HUE_W = 9f;
    private static final float BAR_H = 8f;
    private static final float RADIUS = 11f;

    private static final int DRAG_NONE = 0;
    private static final int DRAG_SV = 1;
    private static final int DRAG_HUE = 2;
    private static final int DRAG_ALPHA = 3;

    private final ColorSetting setting;
    private final float anchorX;
    private final float anchorY;
    private final float virtualW;
    private final float virtualH;
    private final long openedAt = System.currentTimeMillis();

    private long closeStartedAt = -1L;
    private int dragPart = DRAG_NONE;

    private float hue;
    private float saturation;
    private float brightness;
    private float alphaValue;
    private int observedArgb;

    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float svX;
    private float svY;
    private float svSize;
    private float hueX;
    private float hueY;
    private float hueW;
    private float hueH;
    private float alphaX;
    private float alphaY;
    private float alphaW;
    private float alphaH;
    private float themeX;
    private float themeY;
    private float themeW;
    private float themeH;
    private float themeHover;
    private float lastMouseX;
    private float lastMouseY;
    private float lastScale = 1f;

    public ColorPickerPopup(ColorSetting setting,
                            float anchorX,
                            float anchorY,
                            float virtualW,
                            float virtualH) {
        this.setting = setting;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.virtualW = virtualW;
        this.virtualH = virtualH;
        syncFromSetting();
        updateLayout(1f);
    }

    public ColorSetting getSetting() {
        return setting;
    }

    public boolean isClosing() {
        return closeStartedAt >= 0L;
    }

    public void close() {
        if (isClosing()) return;
        closeStartedAt = System.currentTimeMillis();
        dragPart = DRAG_NONE;
    }

    public boolean isFinished() {
        return isClosing() && System.currentTimeMillis() - closeStartedAt >= CLOSE_MS;
    }

    public void render(DrawContext ctx,
                       float scale,
                       int mouseX,
                       int mouseY,
                       float parentAlpha) {
        if (isFinished()) return;

        scale = Math.max(0.45f, scale);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        updateLayout(scale);
        syncExternalChange();

        long now = System.currentTimeMillis();
        float opacity;
        float popupScale;

        if (isClosing()) {
            float progress = clamp((now - closeStartedAt) / (float) CLOSE_MS);
            float eased = RenderUtil.easeInCubic(progress);
            opacity = 1f - eased;
            popupScale = 1f - 0.10f * eased;
        } else {
            float progress = clamp((now - openedAt) / (float) OPEN_MS);
            float eased = RenderUtil.easeOutCubic(progress);
            opacity = eased;
            popupScale = 0.88f + 0.12f * eased;
        }

        float alpha = clamp(parentAlpha) * clamp(opacity);
        if (alpha <= 0.003f) return;

        ctx.getMatrices().push();
        float centerX = panelX + panelW * 0.5f;
        float centerY = panelY + panelH * 0.5f;
        ctx.getMatrices().translate(centerX, centerY, 0f);
        ctx.getMatrices().scale(popupScale, popupScale, 1f);
        ctx.getMatrices().translate(-centerX, -centerY, 0f);

        renderPanel(ctx, scale, alpha);

        ctx.getMatrices().pop();
    }

    private void renderPanel(DrawContext ctx, float scale, float alpha) {
        float radius = RADIUS * scale;

        RenderUtil.drawShadow(ctx, panelX, panelY, panelW, panelH, radius, 15f * scale,
                RenderUtil.withAlpha(0x000000, 0.54f * alpha));
        RenderUtil.drawRoundedRect(ctx, panelX, panelY, panelW, panelH, radius,
                RenderUtil.withAlpha(MenuScreen.WINDOW_BG, 0.985f * alpha));
        float headerRadius = Math.max(0f, radius - 0.5f * scale);
        RenderUtil.drawRoundedGradient(ctx, panelX + 0.5f * scale, panelY + 0.5f * scale,
                panelW - scale, 28f * scale, headerRadius, headerRadius, 0f, 0f,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.105f * alpha),
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.105f * alpha),
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0f),
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0f));

        renderHeader(ctx, scale, alpha);
        renderSvSquare(ctx, scale, alpha);
        renderHueBar(ctx, scale, alpha);

        if (setting.hasAlpha()) {
            renderAlphaBar(ctx, scale, alpha);
        }
        renderValueLabels(ctx, scale, alpha);
    }

    private void renderHeader(DrawContext ctx, float scale, float alpha) {
        float textX = panelX + PAD * scale;
        float textY = panelY + 9.2f * scale;
        float previewW = 14f * scale;
        float previewH = 12f * scale;
        float previewX = panelX + panelW - PAD * scale - previewW;
        float previewY = panelY + 7f * scale;

        String title = CustomFontRenderer.trimToWidth(setting.getName(), panelW - 78f * scale,
                7.6f * scale, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, title, textX, textY - 0.2f * scale, 7.6f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.96f * alpha), FontWeight.SEMIBOLD);

        float previewRadius = 4f * scale;
        float selectedAlpha = setting.hasAlpha() ? alphaValue : 1f;
        RenderUtil.drawRoundedRect(ctx, previewX, previewY, previewW, previewH, previewRadius,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, alpha));
        RenderUtil.drawRoundedRect(ctx, previewX, previewY, previewW, previewH, previewRadius,
                RenderUtil.withAlpha(setting.get(), alpha * selectedAlpha));

        int accent = CustomTheme.guiAccent();
        RenderUtil.drawRoundedRect(ctx, themeX, themeY, themeW, themeH, 4f * scale,
                RenderUtil.withAlpha(accent, alpha * (0.18f + themeHover * 0.09f)));
        CustomFontRenderer.drawCenteredText(ctx, "Тема", themeX + themeW * 0.5f,
                themeY + 3.25f * scale, 5.5f * scale,
                RenderUtil.withAlpha(themeHover > 0.02f ? MenuScreen.TEXT_WHITE : MenuScreen.ACCENT_LIGHT, alpha),
                FontWeight.SEMIBOLD);
    }

    private void renderSvSquare(DrawContext ctx, float scale, float alpha) {
        float radius = 7f * scale;
        int hueColor = Color.HSBtoRGB(hue, 1f, 1f);

        RenderUtil.drawShadow(ctx, svX, svY, svSize, svSize, radius, 5f * scale,
                RenderUtil.withAlpha(0x000000, 0.24f * alpha));
        RenderUtil.drawColorPickerSquare(ctx, svX, svY, svSize, svSize, radius, hueColor, alpha);

        float indicatorX = svX + saturation * svSize;
        float indicatorY = svY + (1f - brightness) * svSize;
        int selectedRgb = Color.HSBtoRGB(hue, saturation, brightness);
        float outerRadius = 4.2f * scale;

        RenderUtil.drawCircle(ctx, indicatorX + 0.65f * scale, indicatorY + 0.85f * scale,
                outerRadius + 0.8f * scale, RenderUtil.withAlpha(0x000000, 0.43f * alpha));
        RenderUtil.drawCircle(ctx, indicatorX, indicatorY, outerRadius,
                RenderUtil.withAlpha(0xFFFFFF, 0.96f * alpha));
        RenderUtil.drawCircle(ctx, indicatorX, indicatorY, 2.65f * scale,
                RenderUtil.withAlpha(selectedRgb, alpha));
    }

    private void renderHueBar(DrawContext ctx, float scale, float alpha) {
        float radius = hueW * 0.5f;
        RenderUtil.drawShadow(ctx, hueX, hueY, hueW, hueH, radius, 4f * scale,
                RenderUtil.withAlpha(0x000000, 0.22f * alpha));

        for (int i = 0; i < 6; i++) {
            float top = i / 6f;
            float bottom = (i + 1f) / 6f;
            float y = hueY + hueH * top;
            float h = hueH / 6f + 0.15f * scale;
            float topRadius = i == 0 ? radius : 0f;
            float bottomRadius = i == 5 ? radius : 0f;
            int cTop = Color.HSBtoRGB(top, 1f, 1f);
            int cBottom = Color.HSBtoRGB(bottom, 1f, 1f);

            RenderUtil.drawRoundedGradient(ctx, hueX, y, hueW, h,
                    topRadius, topRadius, bottomRadius, bottomRadius,
                    RenderUtil.withAlpha(cTop, alpha), RenderUtil.withAlpha(cTop, alpha),
                    RenderUtil.withAlpha(cBottom, alpha), RenderUtil.withAlpha(cBottom, alpha));
        }


        float markerY = hueY + hue * hueH;
        RenderUtil.drawRoundedRect(ctx, hueX - 2.1f * scale, markerY - 1.8f * scale,
                hueW + 4.2f * scale, 3.6f * scale, 1.8f * scale,
                RenderUtil.withAlpha(0x000000, 0.40f * alpha));
        RenderUtil.drawRoundedRect(ctx, hueX - 1.4f * scale, markerY - 1.2f * scale,
                hueW + 2.8f * scale, 2.4f * scale, 1.2f * scale,
                RenderUtil.withAlpha(0xFFFFFF, 0.96f * alpha));
    }

    private void renderAlphaBar(DrawContext ctx, float scale, float alpha) {
        RenderUtil.drawRoundedRect(ctx, alphaX, alphaY, alphaW, alphaH, alphaH * 0.5f,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.95f * alpha));
        RenderUtil.drawRoundedCheckerboard(ctx, alphaX, alphaY, alphaW, alphaH,
                alphaH * 0.5f, 3f * scale, alpha);

        int rgb = setting.get() & 0x00FFFFFF;
        RenderUtil.drawRoundedGradientH(ctx, alphaX, alphaY, alphaW, alphaH, alphaH * 0.5f,
                RenderUtil.withAlpha(rgb, 0f), RenderUtil.withAlpha(rgb, alpha));

        float markerX = alphaX + alphaValue * alphaW;
        RenderUtil.drawCircle(ctx, markerX + 0.55f * scale, alphaY + alphaH * 0.5f + 0.65f * scale,
                3.5f * scale, RenderUtil.withAlpha(0x000000, 0.38f * alpha));
        RenderUtil.drawCircle(ctx, markerX, alphaY + alphaH * 0.5f, 3.25f * scale,
                RenderUtil.withAlpha(0xFFFFFF, 0.96f * alpha));
        RenderUtil.drawCircle(ctx, markerX, alphaY + alphaH * 0.5f, 2f * scale,
                RenderUtil.withAlpha(rgb, alpha));
    }

    private void renderValueLabels(DrawContext ctx, float scale, float alpha) {
        float boxY = setting.hasAlpha() ? panelY + 148f * scale : panelY + 130f * scale;
        float boxX = panelX + PAD * scale;
        float boxW = panelW - PAD * 2f * scale;
        float boxH = 25f * scale;

        RenderUtil.drawRoundedRect(ctx, boxX, boxY, boxW, boxH, 5f * scale,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.76f * alpha));

        String argb = String.format(Locale.ROOT, "#%08X", setting.get());
        String rgba = setting.getRed() + ", " + setting.getGreen() + ", "
                + setting.getBlue() + ", " + setting.getAlpha();

        CustomFontRenderer.drawText(ctx, "ARGB", boxX + 5f * scale, boxY + 4.1f * scale,
                5.8f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(ctx, argb, boxX + boxW - 5f * scale, boxY + 4.1f * scale,
                5.8f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.96f * alpha), FontWeight.REGULAR);

        CustomFontRenderer.drawText(ctx, "RGBA", boxX + 5f * scale, boxY + 15.2f * scale,
                5.5f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(ctx, rgba, boxX + boxW - 5f * scale, boxY + 15.2f * scale,
                5.5f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.95f * alpha), FontWeight.REGULAR);
    }

    private void drawCheckerboard(DrawContext ctx,
                                  float x,
                                  float y,
                                  float width,
                                  float height,
                                  float cell,
                                  float alpha) {
        if (width <= 0f || height <= 0f || cell <= 0f) return;

        int columns = Math.max(1, (int) Math.ceil(width / cell));
        int rows = Math.max(1, (int) Math.ceil(height / cell));
        int light = RenderUtil.withAlpha(0xC8C4D2, 0.82f * alpha);
        int dark = RenderUtil.withAlpha(0x666172, 0.82f * alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (int row = 0; row < rows; row++) {
            float cellY = y + row * cell;
            float cellH = Math.min(cell, y + height - cellY);
            for (int column = 0; column < columns; column++) {
                float cellX = x + column * cell;
                float cellW = Math.min(cell, x + width - cellX);
                checkerQuad(buffer, matrix, cellX, cellY, cellW, cellH,
                        ((row + column) & 1) == 0 ? light : dark);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void checkerQuad(BufferBuilder buffer,
                                    Matrix4f matrix,
                                    float x,
                                    float y,
                                    float width,
                                    float height,
                                    int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        buffer.vertex(matrix, x, y, 0f).color(r, g, b, a);
        buffer.vertex(matrix, x, y + height, 0f).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y + height, 0f).color(r, g, b, a);
        buffer.vertex(matrix, x + width, y, 0f).color(r, g, b, a);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (isClosing() || !contains(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            return false;
        }

        if (button != 0) return true;

        if (contains(mouseX, mouseY, themeX, themeY, themeW, themeH)) {
            applyThemeAccent();
        } else if (contains(mouseX, mouseY, svX, svY, svSize, svSize)) {
            dragPart = DRAG_SV;
            updateSv(mouseX, mouseY);
        } else if (contains(mouseX, mouseY, hueX - 2f * lastScale, hueY,
                hueW + 4f * lastScale, hueH)) {
            dragPart = DRAG_HUE;
            updateHue(mouseY);
        } else if (setting.hasAlpha() && contains(mouseX, mouseY,
                alphaX, alphaY - 2f * lastScale, alphaW, alphaH + 4f * lastScale)) {
            dragPart = DRAG_ALPHA;
            updateAlpha(mouseX);
        }

        return true;
    }

    public boolean mouseDragged(float mouseX, float mouseY) {
        if (isClosing()) return false;

        switch (dragPart) {
            case DRAG_SV -> updateSv(mouseX, mouseY);
            case DRAG_HUE -> updateHue(mouseY);
            case DRAG_ALPHA -> updateAlpha(mouseX);
            default -> {
                return false;
            }
        }
        return true;
    }

    public void mouseReleased() {
        dragPart = DRAG_NONE;
    }

    private void updateSv(float mouseX, float mouseY) {
        saturation = clamp((mouseX - svX) / Math.max(1f, svSize));
        brightness = 1f - clamp((mouseY - svY) / Math.max(1f, svSize));
        writeHsbPreservingAlpha();
    }

    private void updateHue(float mouseY) {
        hue = clamp((mouseY - hueY) / Math.max(1f, hueH));
        writeHsbPreservingAlpha();
    }

    private void updateAlpha(float mouseX) {
        if (!setting.hasAlpha()) return;
        alphaValue = clamp((mouseX - alphaX) / Math.max(1f, alphaW));
        setting.setAlpha(Math.round(alphaValue * 255f));
        observedArgb = setting.get();
        alphaValue = setting.getAlphaF();
    }

    private void writeHsbPreservingAlpha() {
        int preservedAlpha = setting.getAlpha();
        int rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
        setting.set((preservedAlpha << 24) | rgb);
        observedArgb = setting.get();
        alphaValue = setting.getAlphaF();
    }

    private void applyThemeAccent() {
        int preservedAlpha = setting.hasAlpha() ? setting.getAlpha() : 255;
        setting.set((preservedAlpha << 24) | (CustomTheme.guiAccent() & 0x00FFFFFF));
        syncFromSetting();
    }

    private void syncExternalChange() {
        if (dragPart == DRAG_NONE && setting.get() != observedArgb) {
            syncFromSetting();
        }
    }

    private void syncFromSetting() {
        float oldHue = hue;
        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        if (hsb[1] > 0.0001f || observedArgb == 0) {
            hue = hsb[0];
        } else {
            hue = oldHue;
        }
        saturation = hsb[1];
        brightness = hsb[2];
        alphaValue = setting.hasAlpha() ? setting.getAlphaF() : 1f;
        observedArgb = setting.get();
    }

    private void updateLayout(float scale) {
        lastScale = scale;
        panelW = BASE_W * scale;
        panelH = (setting.hasAlpha() ? BASE_H_ALPHA : BASE_H_OPAQUE) * scale;

        float margin = 7f * scale;
        float anchorGap = 10f * scale;
        float preferredX = anchorX + anchorGap;
        if (preferredX + panelW > virtualW - margin) {
            preferredX = anchorX - panelW - anchorGap;
        }

        float maxX = Math.max(margin, virtualW - margin - panelW);
        float maxY = Math.max(margin, virtualH - margin - panelH);
        panelX = clamp(preferredX, margin, maxX);
        panelY = clamp(anchorY - 20f * scale, margin, maxY);

        svX = panelX + PAD * scale;
        svY = panelY + 30f * scale;
        svSize = SV_SIZE * scale;

        hueX = svX + svSize + HUE_GAP * scale;
        hueY = svY;
        hueW = HUE_W * scale;
        hueH = svSize;

        themeW = 29f * scale;
        themeH = 12f * scale;
        themeX = panelX + panelW - PAD * scale - 14f * scale - 4f * scale - themeW;
        themeY = panelY + 7f * scale;
        themeHover = RenderUtil.animate(themeHover,
                contains(lastMouseX, lastMouseY, themeX, themeY, themeW, themeH) ? 1f : 0f,
                0.28f, RenderUtil.getFrameDelta());

        alphaX = panelX + PAD * scale;
        alphaY = panelY + 135f * scale;
        alphaW = panelW - PAD * 2f * scale;
        alphaH = BAR_H * scale;
    }

    private static boolean contains(float x, float y, float left, float top, float width, float height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }
}
