package fun.enoughvisuals.ui.clickgui.components;

import fun.enoughvisuals.systems.events.EventFeedService;
import fun.enoughvisuals.systems.events.EventFeedService.EventItem;
import fun.enoughvisuals.systems.events.EventFeedService.Kind;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Animated two-column event browser shared by FunTime and HolyWorld. */
public final class EventsToolPage {
    private static final String EVENT_ICON = "\uF000";
    private static final String[] TAB_NAMES = {"Текущие", "Предстоящие", "Шахты"};
    private static final Kind[] TAB_KINDS = {Kind.CURRENT, Kind.UPCOMING, Kind.MINE};

    private final Map<String, Float> hoverAnimations = new HashMap<>();
    private final Map<Integer, Float> tabHoverAnimations = new HashMap<>();
    private final Map<String, Bounds> waypointBounds = new HashMap<>();
    private Bounds refreshBounds;
    private float refreshHover;
    private int tab;
    private float tabIndicator;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scale;
    private long openedAt = System.currentTimeMillis();

    public void activate() {
        openedAt = System.currentTimeMillis();
        scroll = scrollTarget = 0f;
    }

    public void render(DrawContext context, float x, float y, float width, float height, float scale,
                       int mouseX, int mouseY, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
        waypointBounds.clear();
        refreshBounds = null;

        float frame = RenderUtil.getFrameDelta();
        scroll = RenderUtil.animate(scroll, scrollTarget, 0.24f, frame);
        tabIndicator = RenderUtil.animate(tabIndicator, tab, 0.26f, frame);
        float intro = RenderUtil.easeOutQuart(clamp((System.currentTimeMillis() - openedAt) / 360f));
        float pageAlpha = alpha * RenderUtil.easeOutCubic(clamp((System.currentTimeMillis() - openedAt) / 290f));
        float offset = (1f - intro) * 13f * scale;

        renderHeader(context, x, y + offset, width, scale, mouseX, mouseY, pageAlpha);
        float contentY = y + 43f * scale;
        float contentH = height - 43f * scale;
        List<EventItem> items = EventFeedService.getInstance().items(TAB_KINDS[tab]);
        float gap = 7f * scale;
        float cardW = (width - gap) * 0.5f;
        float cardH = 57f * scale;
        int rows = (items.size() + 1) / 2;
        maxScroll = Math.max(0f, rows * (cardH + gap) - gap - contentH);
        scrollTarget = Math.max(-maxScroll, Math.min(0f, scrollTarget));

        context.draw();
        StencilUtil.enableScissor(x, contentY, width, contentH);
        if (items.isEmpty()) {
            renderEmpty(context, x, contentY, width, contentH, scale, mouseX, mouseY, pageAlpha);
        } else {
            for (int i = 0; i < items.size(); i++) {
                int row = i / 2;
                int column = i % 2;
                float born = clamp((System.currentTimeMillis() - openedAt - 70L - i * 34L) / 320f);
                float reveal = RenderUtil.easeOutQuart(born);
                float cardAlpha = pageAlpha * RenderUtil.easeOutCubic(born);
                float cardX = x + column * (cardW + gap) + (1f - reveal) * (column == 0 ? -13f : 13f) * scale;
                float cardY = contentY + row * (cardH + gap) + scroll;
                renderCard(context, items.get(i), cardX, cardY, cardW, cardH, scale,
                        mouseX, mouseY, cardAlpha);
            }
        }
        context.draw();
        StencilUtil.disableScissor();
        renderScrollbar(context, x, contentY, width, contentH, pageAlpha);
    }

    private void renderHeader(DrawContext context, float x, float y, float width, float s,
                              int mouseX, int mouseY, float alpha) {
        float tabsH = 31f * s;
        RenderUtil.drawRoundedRect(context, x, y, width, tabsH, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.86f * alpha));
        float tabW = width / TAB_NAMES.length;
        RenderUtil.drawRoundedRect(context, x + tabIndicator * tabW + 3f * s, y + 3f * s,
                tabW - 6f * s, tabsH - 6f * s, 6f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.19f * alpha));
        for (int i = 0; i < TAB_NAMES.length; i++) {
            float center = x + tabW * (i + 0.5f);
            boolean hovered = hit(mouseX, mouseY, x + i * tabW, y, tabW, tabsH);
            float tabHover = RenderUtil.animate(tabHoverAnimations.getOrDefault(i, 0f), hovered ? 1f : 0f,
                    0.24f, RenderUtil.getFrameDelta());
            tabHoverAnimations.put(i, tabHover);
            if (tabHover > 0.01f && i != tab) {
                RenderUtil.drawRoundedRect(context, x + i * tabW + 4f * s, y + 4f * s,
                        tabW - 8f * s, tabsH - 8f * s, 6f * s,
                        RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.035f * tabHover * alpha));
            }
            int count = EventFeedService.getInstance().items(TAB_KINDS[i]).size();
            boolean active = i == tab;
            int color = active ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_GRAY;
            String label = TAB_NAMES[i] + (count > 0 ? "  " + count : "");
            CustomFontRenderer.drawCenteredText(context, label, center,
                    y + 9.9f * s, 7.5f * s, RenderUtil.withAlpha(color, alpha), FontWeight.SEMIBOLD);
        }
    }

    private void renderCard(DrawContext context, EventItem item, float x, float y, float w, float h, float s,
                            int mouseX, int mouseY, float alpha) {
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        float hover = RenderUtil.animate(hoverAnimations.getOrDefault(item.key(), 0f), hovered ? 1f : 0f,
                0.25f, RenderUtil.getFrameDelta());
        hoverAnimations.put(item.key(), hover);
        int accent = eventColor(item);
        int base = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.91f * alpha),
                RenderUtil.withAlpha(accent, 0.13f * alpha), hover);
        RenderUtil.drawRoundedRect(context, x, y, w, h, 8f * s, base);
        RenderUtil.drawRoundedRect(context, x + 0.5f * s, y + h - (4f + hover * 2f) * s,
                w - s, (3.5f + hover * 1.5f) * s, 4f * s,
                RenderUtil.withAlpha(accent, (0.08f + hover * 0.08f) * alpha));

        float ringX = x + 25f * s;
        float ringY = y + h * 0.5f;
        float ringR = 15f * s;
        RenderUtil.drawCircleOutline(context, ringX, ringY, ringR, 2.1f * s,
                RenderUtil.withAlpha(0xFFFFFF, 0.075f * alpha));
        float progress = item.secondsLeft() < 0 ? 0.72f
                : Math.max(0.05f, Math.min(1f, (item.secondsLeft() % 300) / 300f));
        RenderUtil.drawArc(context, ringX, ringY, ringR, 2.25f * s,
                RenderUtil.withAlpha(accent, 0.96f * alpha), progress);
        String timer = item.secondsLeft() < 0 ? EVENT_ICON : formatTime(item.secondsLeft());
        CustomFontRenderer.drawCenteredText(context, timer, ringX,
                ringY - (item.secondsLeft() < 0 ? 5.0f : 3.2f) * s,
                (item.secondsLeft() < 0 ? 9.8f : 6.4f) * s,
                RenderUtil.withAlpha(item.secondsLeft() < 0 ? accent : MenuScreen.TEXT_WHITE, alpha),
                item.secondsLeft() < 0 ? FontWeight.EVENT : FontWeight.SEMIBOLD);

        float textX = x + 49f * s;
        float actionSpace = item.hasCoordinates() ? 45f * s : 9f * s;
        String server = CustomFontRenderer.trimToWidth(item.server(), w - 49f * s - actionSpace,
                7.5f * s, FontWeight.SEMIBOLD);
        String name = CustomFontRenderer.trimToWidth(item.name(), w - 49f * s - actionSpace,
                7.0f * s, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, server, textX, y + 9f * s, 7.5f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, name, textX, y + 23f * s, 7.0f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
        String meta = item.rarity().isBlank() ? providerName(item) : item.rarity();
        CustomFontRenderer.drawText(context, meta, textX, y + 37f * s, 6.6f * s,
                RenderUtil.withAlpha(accent, 0.96f * alpha), FontWeight.SEMIBOLD);

        if (item.hasCoordinates()) {
            float bx = x + w - 42f * s;
            float by = y + (h - 22f * s) * 0.5f;
            RenderUtil.drawRoundedRect(context, bx, by, 36f * s, 22f * s, 6f * s,
                    RenderUtil.withAlpha(accent, (0.13f + hover * 0.11f) * alpha));
            CustomFontRenderer.drawCenteredText(context, "Метка", bx + 18f * s, by + 6.9f * s,
                    6.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
            waypointBounds.put(item.key(), new Bounds(bx, by, 36f * s, 22f * s));
        }
    }

    private void renderEmpty(DrawContext context, float x, float y, float w, float h, float s,
                             int mouseX, int mouseY, float alpha) {
        EventFeedService service = EventFeedService.getInstance();
        float cx = x + w * 0.5f;
        float cy = y + h * 0.44f;
        RenderUtil.drawCircle(context, cx, cy - 10f * s, 16f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.12f * alpha));
        CustomFontRenderer.drawCenteredText(context, EVENT_ICON, cx, cy - 18f * s,
                14f * s, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.EVENT);
        String title = switch (service.status()) {
            case LOADING -> "Получаем события";
            case ERROR -> "Не удалось обновить события";
            case UNSUPPORTED, IDLE -> "Ивенты недоступны на этом сервере";
            case READY -> "Здесь пока тихо";
        };
        CustomFontRenderer.drawCenteredText(context, title, cx, cy + 13f * s,
                8.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(context, service.statusText(), cx, cy + 28f * s,
                6.6f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.REGULAR);

        float buttonW = 72f * s;
        float buttonH = 23f * s;
        float buttonX = cx - buttonW * 0.5f;
        float buttonY = cy + 43f * s;
        refreshHover = RenderUtil.animate(refreshHover,
                hit(mouseX, mouseY, buttonX, buttonY, buttonW, buttonH) ? 1f : 0f,
                0.22f, RenderUtil.getFrameDelta());
        RenderUtil.drawRoundedRect(context, buttonX, buttonY, buttonW, buttonH, 6f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, (0.18f + refreshHover * 0.10f) * alpha));
        CustomFontRenderer.drawCenteredText(context,
                service.status() == EventFeedService.Status.LOADING ? "Обновляем…" : "Обновить",
                cx, buttonY + 7.1f * s, 6.6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        refreshBounds = new Bounds(buttonX, buttonY, buttonW, buttonH);
    }

    private void renderScrollbar(DrawContext context, float x, float y, float w, float h, float alpha) {
        if (maxScroll <= 0.5f) return;
        float trackH = h - 8f * scale;
        float thumbH = Math.max(22f * scale, trackH * h / (h + maxScroll));
        float progress = -scroll / maxScroll;
        float thumbY = y + 4f * scale + (trackH - thumbH) * progress;
        RenderUtil.drawRoundedRect(context, x + w - 2.2f * scale, thumbY, 1.7f * scale, thumbH,
                1f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, 0.5f * alpha));
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !hit(mouseX, mouseY, x, y, width, height)) return false;
        float tabH = 31f * scale;
        if (hit(mouseX, mouseY, x, y, width, tabH)) {
            int next = Math.max(0, Math.min(TAB_NAMES.length - 1, (int) ((mouseX - x) / (width / TAB_NAMES.length))));
            if (next != tab) {
                tab = next;
                scroll = scrollTarget = 0f;
                openedAt = System.currentTimeMillis() - 90L;
                EnoughSoundPlayer.playScroll();
            }
            return true;
        }
        for (EventItem item : EventFeedService.getInstance().items(TAB_KINDS[tab])) {
            Bounds bounds = waypointBounds.get(item.key());
            if (bounds != null && bounds.contains(mouseX, mouseY)) {
                EventFeedService.getInstance().addWaypoint(item);
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        if (refreshBounds != null && refreshBounds.contains(mouseX, mouseY)) {
            EventFeedService.getInstance().refreshNow();
            EnoughSoundPlayer.playScroll();
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!hit(mouseX, mouseY, x, y, width, height) || maxScroll <= 0f) return false;
        scrollTarget += (float) amount * 25f * scale;
        scrollTarget = Math.max(-maxScroll, Math.min(0f, scrollTarget));
        return true;
    }

    private static int eventColor(EventItem item) {
        String rarity = item.rarity().toLowerCase(Locale.ROOT);
        if (rarity.contains("легенд")) return 0xFFFFB65C;
        if (rarity.contains("эпич")) return 0xFFCF76FF;
        if (rarity.contains("редк")) return 0xFF62B7FF;
        if (rarity.contains("взрыв")) return 0xFFFF6D72;
        return item.source() == EventFeedService.Source.HOLYWORLD ? 0xFF72D7FF : MenuScreen.ACCENT_LIGHT;
    }

    private static String providerName(EventItem item) {
        return item.source() == EventFeedService.Source.FUNTIME ? "FunTime" : "HolyWorld";
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60);
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private record Bounds(float x, float y, float w, float h) {
        boolean contains(float mx, float my) { return hit(mx, my, x, y, w, h); }
    }
}
