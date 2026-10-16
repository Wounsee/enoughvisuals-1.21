package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;

/** Pixel constants shared by the Figma Main/HUD implementation. */
public final class HudDesign {
    private static final float ROUNDER_HUD_EXPONENT = 2.35f;
    public static final int PANEL = 0xFF121214;
    public static final int ITEM_SLOT_COLOR = 0xFF121214;
    public static final int ITEM_SLOT_OUTLINE = 0xFF2A2A2E;
    public static final float ITEM_SLOT_BASE_SIZE = 22f;
    public static final float ITEM_SLOT_RADIUS = 7.25f;
    public static final float ITEM_SLOT_INSET = 0.75f;
    public static final int DIVIDER = 0xFF34343A;
    public static final int ROW_MARK = 0xFF29292C;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF8D8D94;
    /** Secondary text one step brighter than {@link #TEXT_DIM}. */
    public static final int TEXT_SOFT = 0xFFC3C3CB;
    /** Background track of bars and toggles. */
    public static final int TRACK = 0xFF2A2A2E;
    public static final int DANGER = 0xFFED2E2E;

    private HudDesign() {
    }

    /** HUD accent follows the ClickGUI theme so both read as one interface. */
    public static int accent() {
        return fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent();
    }

    public static float rounderHudExponent() {
        return ROUNDER_HUD_EXPONENT;
    }

    public static SliderSetting scaleSetting() {
        return new SliderSetting("Масштаб", "scale", "Масштаб HUD", 100, 50, 200, 5, "%");
    }

    public static float scale(SliderSetting setting) {
        return scale(setting, 1f);
    }

    public static float scale(SliderSetting setting, float baseScale) {
        if (setting == null || setting.get() == null) return 1f;
        return Math.max(0.5f, Math.min(2f, setting.get().floatValue() / 100f)) * baseScale;
    }

    public static void drawPanel(net.minecraft.client.gui.DrawContext ctx,
                                 float x, float y, float width, float height,
                                 float radius, float alpha) {
        if (width <= 0f || height <= 0f || alpha <= 0.001f) return;
        RenderUtil.drawShadow(ctx, x, y, width, height, radius, 6f,
                RenderUtil.withAlpha(0x000000, alpha * 0.18f));
        GlassUtil.drawGlassPanel(ctx, x, y, width, height, radius,
                PANEL, alpha * 0.90f, alpha);
    }

    public static void drawRounderPanel(net.minecraft.client.gui.DrawContext ctx,
                                        float x, float y, float width, float height,
                                        float radius, float alpha) {
        float previous = RenderUtil.pushSquircleExponent(ROUNDER_HUD_EXPONENT);
        try {
            drawPanel(ctx, x, y, width, height, radius, alpha);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    public static void drawRounderRect(net.minecraft.client.gui.DrawContext ctx,
                                       float x, float y, float width, float height,
                                       float radius, int color) {
        float previous = RenderUtil.pushSquircleExponent(ROUNDER_HUD_EXPONENT);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, width, height, radius, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    public static void drawRounderRect(net.minecraft.client.gui.DrawContext ctx,
                                       float x, float y, float width, float height,
                                       float topLeft, float topRight, float bottomRight, float bottomLeft,
                                       int color) {
        float previous = RenderUtil.pushSquircleExponent(ROUNDER_HUD_EXPONENT);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, width, height,
                    topLeft, topRight, bottomRight, bottomLeft, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    public static void drawRounderOutline(net.minecraft.client.gui.DrawContext ctx,
                                          float x, float y, float width, float height,
                                          float radius, float lineWidth, int color) {
        float previous = RenderUtil.pushSquircleExponent(ROUNDER_HUD_EXPONENT);
        try {
            RenderUtil.drawRoundedOutline(ctx, x, y, width, height, radius, lineWidth, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    public static void drawDivider(net.minecraft.client.gui.DrawContext ctx,
                                   float x, float y, float width, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x, y, width, 1f, 0.5f,
                RenderUtil.withAlpha(DIVIDER, alpha));
    }

    public static void drawKeycap(net.minecraft.client.gui.DrawContext ctx,
                                  float x, float y, float size, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x, y, size, size, 3f,
                RenderUtil.withAlpha(accent(), alpha * 0.20f));
    }

    /**
     * Shared Armor/Inventory/hotbar slot primitive. Blur is the bottom layer; an
     * explicit full-size #121214 rounded layer keeps the slot dark on bright
     * worlds and in GlassUtil's no-frame fallback.
     */
    public static void drawItemSlot(net.minecraft.client.gui.DrawContext ctx,
                                    float x, float y, float size, float alpha) {
        drawItemSlot(ctx, x, y, size, alpha, true);
    }

    public static void drawItemSlot(net.minecraft.client.gui.DrawContext ctx,
                                    float x, float y, float size, float alpha, boolean outline) {
        ItemSlotStyle style = itemSlotStyle(size, alpha);
        if (style.size() <= 0f || style.alpha() <= 0.001f) return;

        float previous = RenderUtil.pushSquircleExponent(2f);
        try {
            GlassUtil.drawGlassPanel(ctx, x, y, style.size(), style.size(), style.radius(),
                    ITEM_SLOT_COLOR, style.glassTintAlpha(), style.blurAlpha());
            RenderUtil.drawRoundedRect(ctx,
                    x, y, style.size(), style.size(), style.radius(),
                    RenderUtil.withAlpha(ITEM_SLOT_COLOR, style.insetAlpha()));
            if (outline) {
                RenderUtil.drawRoundedOutline(ctx, x, y, style.size(), style.size(), style.radius(),
                        style.outlineWidth(),
                        RenderUtil.withAlpha(ITEM_SLOT_OUTLINE, style.outlineAlpha()));
            }
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    public static ItemSlotStyle itemSlotStyle(float size, float alpha) {
        float safeSize = Math.max(0f, size);
        float unit = safeSize / ITEM_SLOT_BASE_SIZE;
        float safeAlpha = Math.max(0f, Math.min(1f, alpha));
        float inset = ITEM_SLOT_INSET * unit;
        return new ItemSlotStyle(
                safeSize,
                ITEM_SLOT_RADIUS * unit,
                inset,
                Math.max(0f, safeSize - inset * 2f),
                Math.max(0f, (ITEM_SLOT_RADIUS - ITEM_SLOT_INSET) * unit),
                safeAlpha,
                safeAlpha * 0.72f,
                safeAlpha * 0.34f,
                safeAlpha * 0.90f,
                0.55f * unit,
                safeAlpha * 0.78f
        );
    }

    public record ItemSlotStyle(float size, float radius, float inset, float insetSize,
                                float insetRadius, float alpha, float blurAlpha,
                                float glassTintAlpha, float insetAlpha,
                                float outlineWidth, float outlineAlpha) {
    }

    /** Draws a clockwise squircle perimeter, starting at the top center. */
    public static void drawProgressOutline(net.minecraft.client.gui.DrawContext ctx,
                                           float x, float y, float width, float height,
                                           float radius, float lineWidth, float progress, int color) {
        RenderUtil.drawSquircleProgressOutline(ctx, x, y, width, height,
                radius, lineWidth, progress, color);
    }
}
