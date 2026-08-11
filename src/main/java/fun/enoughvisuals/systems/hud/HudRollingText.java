package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

/** Per-glyph rolling text used by compact HUD counters and timers. */
public final class HudRollingText {
    private static final long DURATION_MS = 420L;

    private String previous;
    private String current;
    private long changedAt;

    public HudRollingText(String initial) {
        String value = initial == null ? "" : initial;
        previous = value;
        current = value;
    }

    public void set(String value) {
        String next = value == null ? "" : value;
        if (next.equals(current)) return;
        previous = current;
        current = next;
        changedAt = System.currentTimeMillis();
    }

    public float width(float size, FontWeight weight) {
        return CustomFontRenderer.getTextWidth(current, size, weight);
    }

    public float layoutWidth(float size, FontWeight weight) {
        if (changedAt != 0L && System.currentTimeMillis() - changedAt < DURATION_MS) {
            return Math.max(CustomFontRenderer.getTextWidth(previous, size, weight),
                    CustomFontRenderer.getTextWidth(current, size, weight));
        }
        return width(size, weight);
    }

    public float draw(DrawContext ctx, float x, float y, float clipWidth,
                      float size, float alpha, int color, FontWeight weight) {
        float currentWidth = width(size, weight);
        if (changedAt == 0L) {
            drawGlyph(ctx, current, x, y, size, color, alpha, weight);
            return currentWidth;
        }

        float p = Math.min(1f, (System.currentTimeMillis() - changedAt) / (float) DURATION_MS);
        if (p >= 1f) {
            previous = current;
            changedAt = 0L;
            drawGlyph(ctx, current, x, y, size, color, alpha, weight);
            return currentWidth;
        }

        float line = size + 3f;
        RenderUtil.enableScissor(x - 0.5f, y - 1f, clipWidth + 1f, line);
        try {
            int length = Math.max(previous.length(), current.length());
            String oldValue = leftPad(previous, length);
            String newValue = leftPad(current, length);
            float cursor = x;
            float eased = easeOutBack(p);
            for (int index = 0; index < length; index++) {
                String oldGlyph = String.valueOf(oldValue.charAt(index));
                String newGlyph = String.valueOf(newValue.charAt(index));
                float cellWidth = Math.max(glyphWidth(oldGlyph, size, weight),
                        glyphWidth(newGlyph, size, weight));
                if (oldGlyph.equals(newGlyph)) {
                    if (!" ".equals(newGlyph)) {
                        drawGlyph(ctx, newGlyph, cursor, y, size, color, alpha, weight);
                    }
                } else {
                    if (!" ".equals(oldGlyph)) {
                        drawGlyph(ctx, oldGlyph, cursor, y + p * line, size, color,
                                alpha * (1f - p), weight);
                    }
                    if (!" ".equals(newGlyph)) {
                        drawGlyph(ctx, newGlyph, cursor, y - (1f - eased) * line,
                                size, color, alpha * Math.min(1f, p * 2.5f), weight);
                    }
                }
                cursor += cellWidth;
            }
        } finally {
            RenderUtil.disableScissor();
        }
        return currentWidth;
    }

    public float drawCentered(DrawContext ctx, float centerX, float y, float clipWidth,
                              float size, float alpha, int color, FontWeight weight) {
        float textWidth = width(size, weight);
        return draw(ctx, centerX - textWidth * 0.5f, y, clipWidth,
                size, alpha, color, weight);
    }

    private static void drawGlyph(DrawContext ctx, String glyph, float x, float y,
                                  float size, int color, float alpha, FontWeight weight) {
        CustomFontRenderer.drawText(ctx, glyph, x, y, size,
                RenderUtil.withAlpha(color, alpha), weight);
    }

    private static float glyphWidth(String glyph, float size, FontWeight weight) {
        return " ".equals(glyph)
                ? CustomFontRenderer.getTextWidth("0", size, weight)
                : CustomFontRenderer.getTextWidth(glyph, size, weight);
    }

    private static String leftPad(String value, int length) {
        if (value.length() >= length) return value;
        return " ".repeat(length - value.length()) + value;
    }

    private static float easeOutBack(float p) {
        float c1 = 1.32f;
        float c3 = c1 + 1f;
        float q = p - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }
}
