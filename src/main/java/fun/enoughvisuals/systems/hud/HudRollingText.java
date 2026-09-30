package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.util.function.ToDoubleFunction;

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
        float currentWidth = width(size, weight);
        if (changedAt == 0L) return currentWidth;

        float p = progress();
        if (p >= 1f) return currentWidth;
        float previousWidth = CustomFontRenderer.getTextWidth(previous, size, weight);
        return lerp(previousWidth, currentWidth, easeOutCubic(p));
    }

    public float draw(DrawContext ctx, float x, float y, float clipWidth,
                      float size, float alpha, int color, FontWeight weight) {
        float currentWidth = width(size, weight);
        if (changedAt == 0L) {
            drawGlyph(ctx, current, x, y, size, color, alpha, weight);
            return currentWidth;
        }

        float p = progress();
        if (p >= 1f) {
            previous = current;
            changedAt = 0L;
            drawGlyph(ctx, current, x, y, size, color, alpha, weight);
            return currentWidth;
        }

        float line = size + 3f;
        Layout layout = layout(previous, current, easeOutCubic(p),
                text -> CustomFontRenderer.getTextWidth(text, size, weight));
        float animatedWidth = layout.width();
        RenderUtil.enableScissor(x - 0.5f, y - 1f, clipWidth + 1f, line);
        try {
            int length = Math.max(previous.length(), current.length());
            String oldValue = leftPad(previous, length);
            String newValue = leftPad(current, length);
            float eased = easeOutBack(p);
            for (int index = 0; index < length; index++) {
                String oldGlyph = String.valueOf(oldValue.charAt(index));
                String newGlyph = String.valueOf(newValue.charAt(index));
                float cursor = x + layout.positions()[index];
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
            }
        } finally {
            RenderUtil.disableScissor();
        }
        return animatedWidth;
    }

    public float drawCentered(DrawContext ctx, float centerX, float y, float clipWidth,
                              float size, float alpha, int color, FontWeight weight) {
        float textWidth = layoutWidth(size, weight);
        return draw(ctx, centerX - textWidth * 0.5f, y, clipWidth,
                size, alpha, color, weight);
    }

    private static void drawGlyph(DrawContext ctx, String glyph, float x, float y,
                                  float size, int color, float alpha, FontWeight weight) {
        CustomFontRenderer.drawText(ctx, glyph, x, y, size,
                RenderUtil.withAlpha(color, alpha), weight);
    }

    record Layout(float width, float[] positions) { }

    static Layout layout(String previous, String current, float progress,
                         ToDoubleFunction<String> measure) {
        int length = Math.max(previous.length(), current.length());
        float[] positions = new float[length];
        for (int index = 0; index < length; index++) {
            int oldIndex = index - (length - previous.length());
            int newIndex = index - (length - current.length());
            // Missing leading digits occupy no width. Real glyph positions,
            // including kerning, move with the same easing as the suffix.
            positions[index] = lerp(glyphPosition(previous, oldIndex, measure),
                    glyphPosition(current, newIndex, measure), progress);
        }
        return new Layout(lerp((float) measure.applyAsDouble(previous),
                (float) measure.applyAsDouble(current), progress), positions);
    }

    private static float glyphPosition(String text, int index, ToDoubleFunction<String> measure) {
        if (index < 0) return 0f;
        return (float) (measure.applyAsDouble(text.substring(0, index + 1))
                - measure.applyAsDouble(text.substring(index, index + 1)));
    }

    private static String leftPad(String value, int length) {
        if (value.length() >= length) return value;
        return " ".repeat(length - value.length()) + value;
    }

    private float progress() {
        return Math.min(1f, Math.max(0f,
                (System.currentTimeMillis() - changedAt) / (float) DURATION_MS));
    }

    private static float easeOutCubic(float p) {
        float q = 1f - Math.max(0f, Math.min(1f, p));
        return 1f - q * q * q;
    }

    private static float easeOutBack(float p) {
        float c1 = 1.32f;
        float c3 = c1 + 1f;
        float q = Math.max(0f, Math.min(1f, p)) - 1f;
        return 1f + c3 * q * q * q + c1 * q * q;
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }
}
