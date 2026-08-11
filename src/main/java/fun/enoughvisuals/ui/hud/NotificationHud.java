package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudListAnimator;
import fun.enoughvisuals.systems.hud.HudMotion;
import fun.enoughvisuals.systems.hud.HudNotificationCenter;
import fun.enoughvisuals.systems.hud.HudNotificationCenter.Notification;
import fun.enoughvisuals.systems.hud.HudNotificationCenter.Type;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Main/HUD notification stack (Figma node 151:204). */
public final class NotificationHud extends HudElement {
    private static final float BASE_WIDTH = 170f;
    private static final float ROW_HEIGHT = 23f;
    private static final float ROW_STEP = 30.5f;
    private static final float TOGGLE_WIDTH = 18f;
    private static final float TOGGLE_HEIGHT = 9f;

    private static final List<Notification> PREVIEW = List.of(
            preview(-1, Type.INFO, "Пример уведомления"),
            preview(-2, Type.MODULE_ENABLED, "Включено Animations"),
            preview(-3, Type.MODULE_DISABLED, "Выключено Animations")
    );

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private final HudListAnimator animator = new HudListAnimator();
    private final Map<String, Notification> visuals = new LinkedHashMap<>();
    private final Map<String, HudMotion.Spring> stackPositions = new LinkedHashMap<>();

    public NotificationHud(float x, float y) {
        super(x, y, BASE_WIDTH, ROW_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        boolean chat = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        List<Notification> incoming = HudNotificationCenter.active();
        if (incoming.isEmpty() && chat) incoming = PREVIEW;

        List<String> keys = new ArrayList<>();
        for (Notification notification : incoming) {
            keys.add(notification.key());
            visuals.put(notification.key(), notification);
        }
        float frameDelta = RenderUtil.getFrameDelta();
        animator.update(keys, ROW_STEP, frameDelta);
        visuals.keySet().removeIf(key -> animator.rows().stream()
                .noneMatch(row -> row.key().equals(key)));
        List<HudListAnimator.Row> visualRows = orderedRows();
        for (int index = 0; index < visualRows.size(); index++) {
            HudListAnimator.Row row = visualRows.get(index);
            float target = index * ROW_STEP;
            stackPositions.computeIfAbsent(row.key(), ignored -> new HudMotion.Spring(target))
                    .step(target, frameDelta);
        }
        stackPositions.keySet().removeIf(key -> visualRows.stream()
                .noneMatch(row -> row.key().equals(key)));
        setContentVisibleTarget(!animator.rows().isEmpty() || !incoming.isEmpty());

        float s = HudDesign.scale(scale, 0.90f);
        float lastPosition = -ROW_STEP;
        for (HudListAnimator.Row row : visualRows) {
            HudMotion.Spring position = stackPositions.get(row.key());
            float springPosition = position == null ? lastPosition + ROW_STEP : position.value();
            lastPosition = Math.max(springPosition, lastPosition + ROW_STEP);
        }
        float rowsHeight = visualRows.isEmpty() ? ROW_HEIGHT : ROW_HEIGHT + Math.max(0f, lastPosition);
        setWidth(BASE_WIDTH * s);
        setHeight(rowsHeight * s);
        setX((HudElement.hudScreenWidth() - getWidth()) * 0.5f);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        float s = HudDesign.scale(scale, 0.90f);
        float previousPosition = -ROW_STEP;
        for (HudListAnimator.Row row : orderedRows()) {
            Notification notification = visuals.get(row.key());
            if (notification == null) continue;
            float visibility = RenderUtil.easeOutQuart(row.visibility());
            float rowAlpha = alpha * visibility;
            HudMotion.Spring position = stackPositions.get(row.key());
            float springPosition = position == null ? previousPosition + ROW_STEP : position.value();
            float visualPosition = Math.max(springPosition, previousPosition + ROW_STEP);
            previousPosition = visualPosition;
            float rowY = y + visualPosition * s;
            float rowWidth = BASE_WIDTH * s;
            float rowX = x + (w - rowWidth) * 0.5f;
            float centerX = rowX + rowWidth * 0.5f;
            float centerY = rowY + ROW_HEIGHT * 0.5f * s;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(centerX, centerY, 0f);
            float scaleIn = row.present()
                    ? 1.28f - 0.28f * visibility
                    : 0.96f + 0.04f * visibility;
            ctx.getMatrices().scale(scaleIn, scaleIn, 1f);
            ctx.getMatrices().translate(-centerX, -centerY, 0f);
            drawNotification(ctx, notification, rowX, rowY, rowWidth, s, rowAlpha);
            ctx.getMatrices().pop();
        }
    }

    private List<HudListAnimator.Row> orderedRows() {
        Map<String, HudListAnimator.Row> byKey = new LinkedHashMap<>();
        for (HudListAnimator.Row row : animator.rows()) byKey.put(row.key(), row);
        List<HudListAnimator.Row> result = new ArrayList<>();
        for (String key : visuals.keySet()) {
            HudListAnimator.Row row = byKey.get(key);
            if (row != null) result.add(row);
        }
        return result;
    }

    private static void drawNotification(DrawContext ctx, Notification notification,
                                         float x, float y, float width, float s, float alpha) {
        float radius = 9f * s;
        HudDesign.drawRounderPanel(ctx, x, y, width, ROW_HEIGHT * s, radius, alpha);
        HudDesign.drawRounderOutline(ctx, x, y, width, ROW_HEIGHT * s, radius,
                0.5f * s, RenderUtil.withAlpha(0xFF1B1B1C, alpha));
        HudDesign.drawRounderRect(ctx, x + 30f * s, y + 5f * s,
                0.8f * s, 13f * s, 0.4f * s,
                RenderUtil.withAlpha(HudDesign.DIVIDER, alpha * 0.82f));

        int iconColor = notification.type() == Type.WARNING
                ? HudDesign.DANGER : HudDesign.ACCENT;
        boolean compactModuleIcon = notification.type() == Type.MODULE_ENABLED
                || notification.type() == Type.MODULE_DISABLED;
        float iconSize = compactModuleIcon ? 11f * s : 14f * s;
        float iconX = compactModuleIcon ? x + 9.5f * s : x + 8f * s;
        float iconY = compactModuleIcon ? y + 6f * s : y + 4.5f * s;
        CustomFontRenderer.drawText(ctx, icon(notification.type()),
                iconX, iconY, iconSize,
                RenderUtil.withAlpha(iconColor, alpha), FontWeight.HUD_ICONS);

        String text = cleanText(notification.text());
        float textX = x + 37f * s;
        float textY = y + 7f * s;
        float textSize = 7f * s;
        boolean moduleState = notification.type() == Type.MODULE_ENABLED
                || notification.type() == Type.MODULE_DISABLED;
        if (moduleState) {
            drawToggle(ctx, x + width - 25f * s, y + 7f * s,
                    notification.type() == Type.MODULE_ENABLED, s, alpha);
        }
        float maxWidth = moduleState
                ? width - 69f * s
                : width - 44f * s;
        if (moduleState) {
            drawModuleStateText(ctx, text, textX, textY, maxWidth, textSize, alpha,
                    notification.type() == Type.MODULE_ENABLED);
            return;
        }
        if (notification.type() == Type.CONFIG) {
            drawSplitText(ctx, text, textX, textY, maxWidth, textSize, alpha,
                    notification.type());
            return;
        }

        int textColor = notification.type() == Type.WARNING
                ? HudDesign.DANGER : HudDesign.TEXT;
        drawTextOrMarquee(ctx, text, textX, textY, maxWidth, textSize,
                RenderUtil.withAlpha(textColor, alpha));
    }

    private static void drawToggle(DrawContext ctx, float x, float y,
                                   boolean enabled, float s, float alpha) {
        float width = TOGGLE_WIDTH * s;
        float height = TOGGLE_HEIGHT * s;
        int trackColor = enabled ? HudDesign.ACCENT : 0xFF34343A;
        drawCapsule(ctx, x, y, width, height,
                RenderUtil.withAlpha(trackColor, alpha * (enabled ? 0.84f : 0.88f)));
        drawCapsuleOutline(ctx, x, y, width, height, 0.45f * s,
                RenderUtil.withAlpha(0xFFFFFFFF, alpha * 0.07f));

        // Deliberately wider than tall: the Figma/iOS core is a small white pill,
        // not a circular dot floating in a rounded rectangle.
        float knobWidth = 7.25f * s;
        float knobHeight = 5.25f * s;
        float knobInset = 1.25f * s;
        float knobX = enabled ? x + width - knobWidth - knobInset : x + knobInset;
        float knobY = y + (height - knobHeight) * 0.5f;
        drawCapsule(ctx, knobX, knobY, knobWidth, knobHeight,
                RenderUtil.withAlpha(0xFFFFFFFF, alpha * (enabled ? 0.98f : 0.74f)));
    }

    private static void drawCapsule(DrawContext ctx, float x, float y,
                                    float width, float height, int color) {
        float previous = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, width, height, height * 0.5f, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    private static void drawCapsuleOutline(DrawContext ctx, float x, float y,
                                           float width, float height, float lineWidth, int color) {
        float previous = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedOutline(ctx, x, y, width, height,
                    height * 0.5f, lineWidth, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    private static void drawModuleStateText(DrawContext ctx, String text, float x, float y,
                                            float maxWidth, float size, float alpha,
                                            boolean enabled) {
        String module = text.replaceFirst("^(Включено|Выключено)\\s+", "").trim();
        String suffix = enabled ? " включён" : " выключен";
        float suffixWidth = CustomFontRenderer.getTextWidth(suffix, size, FontWeight.MEDIUM);
        module = CustomFontRenderer.trimToWidth(module,
                Math.max(8f, maxWidth - suffixWidth), size, FontWeight.MEDIUM);
        CustomFontRenderer.drawText(ctx, module, x, y, size,
                RenderUtil.withAlpha(HudDesign.ACCENT, alpha), FontWeight.MEDIUM);
        float moduleWidth = CustomFontRenderer.getTextWidth(module, size, FontWeight.MEDIUM);
        CustomFontRenderer.drawText(ctx, suffix, x + moduleWidth, y, size,
                RenderUtil.withAlpha(HudDesign.TEXT_DIM, alpha), FontWeight.MEDIUM);
    }

    private static void drawSplitText(DrawContext ctx, String text, float x, float y,
                                      float maxWidth, float size, float alpha, Type type) {
        String prefix;
        if (type == Type.MODULE_ENABLED && text.startsWith("Включено ")) {
            prefix = "Включено ";
        } else if (type == Type.MODULE_DISABLED && text.startsWith("Выключено ")) {
            prefix = "Выключено ";
        } else if (type == Type.CONFIG) {
            int lastSpace = text.lastIndexOf(' ');
            prefix = lastSpace > 0 ? text.substring(0, lastSpace + 1) : text;
        } else {
            prefix = text;
        }
        String tail = text.substring(Math.min(prefix.length(), text.length()));
        float total = CustomFontRenderer.getTextWidth(text, size, FontWeight.MEDIUM);
        if (total > maxWidth) {
            drawTextOrMarquee(ctx, text, x, y, maxWidth, size,
                    RenderUtil.withAlpha(HudDesign.TEXT, alpha));
            return;
        }
        CustomFontRenderer.drawText(ctx, prefix, x, y, size,
                RenderUtil.withAlpha(HudDesign.TEXT, alpha), FontWeight.MEDIUM);
        float prefixWidth = CustomFontRenderer.getTextWidth(prefix, size, FontWeight.MEDIUM);
        CustomFontRenderer.drawText(ctx, tail, x + prefixWidth, y, size,
                RenderUtil.withAlpha(0xFF9B9B9F, alpha), FontWeight.MEDIUM);
    }

    private static void drawTextOrMarquee(DrawContext ctx, String text, float x, float y,
                                          float maxWidth, float size, int color) {
        if (CustomFontRenderer.getTextWidth(text, size, FontWeight.MEDIUM) > maxWidth) {
            RenderUtil.drawMarqueeText(ctx, text, x, y, maxWidth, size + 2f, size,
                    color, FontWeight.MEDIUM, 16f);
        } else {
            CustomFontRenderer.drawText(ctx, text, x, y, size, color, FontWeight.MEDIUM);
        }
    }

    private static String cleanText(String text) {
        if (text == null) return "";
        return text.replaceFirst("^(Включено|Выключено):\\s*", "$1 ").trim();
    }

    private static String icon(Type type) {
        return switch (type) {
            case MODULE_ENABLED -> HudIcons.MODULE_ENABLED;
            case MODULE_DISABLED -> HudIcons.MODULE_DISABLED;
            case CONFIG -> HudIcons.SAVE;
            case WARNING -> HudIcons.WARNING;
            case INFO -> HudIcons.BELL;
        };
    }

    private static Notification preview(long id, Type type, String text) {
        return new Notification(id, type, text, 0L, Long.MAX_VALUE);
    }

    @Override
    public boolean isHorizontalDragLocked() {
        return true;
    }
}
