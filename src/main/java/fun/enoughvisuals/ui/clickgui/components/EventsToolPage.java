package fun.enoughvisuals.ui.clickgui.components;

import fun.enoughvisuals.systems.events.EventFeedService;
import fun.enoughvisuals.systems.events.EventFeedService.EventAction;
import fun.enoughvisuals.systems.events.EventFeedService.EventItem;
import fun.enoughvisuals.systems.events.EventFeedService.Kind;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Polished two-column event browser inspired by the standalone Events surface. */
public final class EventsToolPage {
    private static final Kind[] TAB_KINDS = {Kind.CURRENT, Kind.UPCOMING, Kind.MINE};
    private static final float TAB_HEIGHT = 28f;
    private static final float CARD_HEIGHT = 46f;
    private static final float CARD_GAP_X = 10f;
    private static final float CARD_GAP_Y = 8f;

    private final Map<String, Float> hoverAnimations = new HashMap<>();
    private final Map<Integer, Float> tabHoverAnimations = new HashMap<>();
    private final Map<Integer, Float> tabSelectAnimations = new HashMap<>();
    private final Map<String, Integer> timerWindows = new HashMap<>();
    private final Map<String, Bounds> actionBounds = new HashMap<>();
    private final Map<String, Long> pressTimes = new HashMap<>();
    private Bounds refreshBounds;
    private float refreshHover;
    private int tab;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scale;
    private float headerX;
    private float headerWidth;
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
        actionBounds.clear();
        refreshBounds = null;

        float frame = RenderUtil.getFrameDelta();
        scroll = RenderUtil.animate(scroll, scrollTarget, 0.24f, frame);
        float intro = RenderUtil.easeOutCubic(clamp((System.currentTimeMillis() - openedAt) / 210f));
        float pageAlpha = alpha * intro;
        float offset = (1f - intro) * 4f * scale;

        renderHeader(context, x, y + offset, width, scale, mouseX, mouseY, pageAlpha);
        List<EventItem> events = EventFeedService.getInstance().items(TAB_KINDS[tab]);
        float outerInset = 4f * scale;
        float panelWidth = Math.max(0f, width - outerInset * 2f);
        float panelHeight = Math.max(0f, height - 38f * scale);
        float panelX = x + outerInset;
        float panelY = y + 36f * scale;
        float cardsX = panelX + 12f * scale;
        float cardsY = panelY + 34f * scale;
        float cardsWidth = panelWidth - 24f * scale;
        float cardsHeight = panelHeight - 38f * scale;
        float gapX = CARD_GAP_X * scale;
        float gapY = CARD_GAP_Y * scale;
        float cardW = (cardsWidth - gapX) * 0.5f;
        float cardH = CARD_HEIGHT * scale;
        int rows = (events.size() + 1) / 2;
        maxScroll = Math.max(0f, rows * (cardH + gapY) - gapY - cardsHeight);
        scrollTarget = Math.max(-maxScroll, Math.min(0f, scrollTarget));

        RenderUtil.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, 11f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * pageAlpha));
        renderServerHeader(context, panelX, panelY, panelWidth, scale, pageAlpha);

        context.draw();
        StencilUtil.enableScissor(cardsX, cardsY, cardsWidth, cardsHeight);
        if (events.isEmpty()) {
            renderEmpty(context, cardsX, cardsY, cardsWidth, cardsHeight,
                    scale, mouseX, mouseY, pageAlpha);
        } else {
            for (int index = 0; index < events.size(); index++) {
                int row = index / 2;
                int column = index % 2;
                float reveal = RenderUtil.easeOutCubic(clamp(
                        (System.currentTimeMillis() - openedAt - 28L - index * 18L) / 190f));
                float cardX = cardsX + column * (cardW + gapX);
                float cardY = cardsY + row * (cardH + gapY) + scroll + (1f - reveal) * 2f * scale;
                renderCard(context, events.get(index), cardX, cardY, cardW, cardH, scale,
                        mouseX, mouseY, pageAlpha * reveal);
            }
        }
        context.draw();
        StencilUtil.disableScissor();
        renderScrollbar(context, panelX, cardsY, panelWidth, cardsHeight, pageAlpha);
    }

    private void renderHeader(DrawContext context, float x, float y, float width, float scale,
                              int mouseX, int mouseY, float alpha) {
        float tabsH = TAB_HEIGHT * scale;
        headerWidth = Math.max(0f, width - 8f * scale);
        headerX = x + 4f * scale;
        RenderUtil.drawRoundedRect(context, headerX, y, headerWidth, tabsH, 9f * scale,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * alpha));
        float tabW = headerWidth / TAB_KINDS.length;
        for (int index = 0; index < TAB_KINDS.length; index++) {
            float center = headerX + tabW * (index + 0.5f);
            boolean hovered = hit(mouseX, mouseY, headerX + index * tabW, y, tabW, tabsH);
            float tabHover = RenderUtil.animate(tabHoverAnimations.getOrDefault(index, 0f), hovered ? 1f : 0f,
                    0.25f, RenderUtil.getFrameDelta());
            tabHoverAnimations.put(index, tabHover);
            boolean active = index == tab;
            float selected = RenderUtil.animate(tabSelectAnimations.getOrDefault(index, active ? 1f : 0f),
                    active ? 1f : 0f, 0.22f, RenderUtil.getFrameDelta());
            tabSelectAnimations.put(index, selected);
            int idleColor = RenderUtil.lerpColor(MenuScreen.TEXT_GRAY, MenuScreen.TEXT_WHITE, tabHover * 0.24f);
            int color = RenderUtil.lerpColor(idleColor, MenuScreen.ACCENT_LIGHT, selected);
            float fontSize = 7.9f * scale;
            FontWeight fontWeight = active ? FontWeight.SEMIBOLD : FontWeight.MEDIUM;
            CustomFontRenderer.drawCenteredText(context, tabName(index), center,
                    centeredTextY(y, tabsH, fontSize, fontWeight), fontSize,
                    RenderUtil.withAlpha(color, alpha), fontWeight);
        }
    }

    private void renderServerHeader(DrawContext context, float x, float y, float width, float scale, float alpha) {
        EventFeedService service = EventFeedService.getInstance();
        String prefix = ClientLanguage.text("ui.events.server", "Сервер: ");
        String provider = service.currentProviderName();
        float iconSize = 9f * scale;
        float labelSize = 7.8f * scale;
        float prefixWidth = CustomFontRenderer.getTextWidth(prefix, labelSize, FontWeight.MEDIUM);
        float providerWidth = CustomFontRenderer.getTextWidth(provider, labelSize, FontWeight.MEDIUM);
        float totalWidth = iconSize + 4f * scale + prefixWidth + providerWidth;
        float cursor = x + (width - totalWidth) * 0.5f;
        float contentHeight = 27f * scale;
        CustomFontRenderer.drawText(context, SolarIcons.SERVER, cursor,
                centeredTextY(y, contentHeight, iconSize, FontWeight.SOLAR_ICONS),
                iconSize, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SOLAR_ICONS);
        cursor += iconSize + 4f * scale;
        float labelY = centeredTextY(y, contentHeight, labelSize, FontWeight.MEDIUM);
        CustomFontRenderer.drawText(context, prefix, cursor, labelY,
                labelSize, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.MEDIUM);
        cursor += prefixWidth;
        CustomFontRenderer.drawText(context, provider, cursor, labelY,
                labelSize, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.MEDIUM);
        RenderUtil.drawRoundedRect(context, x, y + 27f * scale, width, 0.8f * scale, 0.4f * scale,
                RenderUtil.withAlpha(MenuScreen.SEPARATOR,
                        RenderUtil.getAlpha(MenuScreen.SEPARATOR) * alpha));
    }

    private void renderCard(DrawContext context, EventItem item, float x, float y, float width, float height,
                            float scale, int mouseX, int mouseY, float alpha) {
        boolean hovered = hit(mouseX, mouseY, x, y, width, height);
        float hover = RenderUtil.animate(hoverAnimations.getOrDefault(item.key(), 0f), hovered ? 1f : 0f,
                0.25f, RenderUtil.getFrameDelta());
        hoverAnimations.put(item.key(), hover);
        int accent = eventColor(item);
        int cardTop = RenderUtil.lerpColor(MenuScreen.INSET_BG, MenuScreen.CARD_HOVER,
                0.10f + hover * 0.62f);
        int cardBottom = RenderUtil.lerpColor(MenuScreen.INSET_BG, accent,
                0.010f + hover * 0.018f);
        RenderUtil.drawRoundedGradientV(context, x, y, width, height, 13f * scale,
                RenderUtil.withAlpha(cardTop, alpha), RenderUtil.withAlpha(cardBottom, alpha));
        if (hover > 0.01f) {
            RenderUtil.drawRoundedRect(context, x, y, width, height, 13f * scale,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.020f * hover * alpha));
        }

        long now = System.currentTimeMillis();
        int secondsLeft = item.secondsLeft(now);
        float ringX = x + 20.5f * scale;
        float ringY = y + height * 0.5f;
        float ringRadius = 13.6f * scale;
        RenderUtil.drawCircleOutline(context, ringX, ringY, ringRadius, 1.55f * scale,
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, alpha));
        boolean timedEvent = (item.kind() == Kind.CURRENT || item.kind() == Kind.UPCOMING) && secondsLeft >= 0;
        if (timedEvent) {
            float progress = timerProgress(item.key(), secondsLeft);
            RenderUtil.drawArc(context, ringX, ringY, ringRadius, 1.75f * scale,
                    RenderUtil.withAlpha(0xFF555B79, 0.96f * alpha), progress);
            float timerSize = 6.5f * scale;
            CustomFontRenderer.drawCenteredText(context, formatTime(secondsLeft), ringX,
                    centeredTextY(ringY - ringRadius, ringRadius * 2f, timerSize, FontWeight.BOLD),
                    timerSize, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.82f * alpha), FontWeight.BOLD);
        }

        EventFeedService service = EventFeedService.getInstance();
        EventAction action = service.actionFor(item);
        String actionIcon = actionIcon(action);
        float actionSize = action == EventAction.NONE ? 0f : 18f * scale;
        float textX = x + 40.5f * scale;
        float rightPad = actionSize > 0f ? actionSize + 9f * scale : 5f * scale;
        float textWidth = Math.max(20f * scale, width - (textX - x) - rightPad);
        float serverSize = 6.8f * scale;
        float metadataSize = 6.05f * scale;
        float detailSize = 6.0f * scale;
        float serverHeight = CustomFontRenderer.getTextHeight(serverSize, FontWeight.SEMIBOLD);
        float metadataHeight = CustomFontRenderer.getTextHeight(metadataSize, FontWeight.SEMIBOLD);
        float detailHeight = CustomFontRenderer.getTextHeight(detailSize, FontWeight.MEDIUM);
        float rowGap = 2f * scale;
        float stackHeight = serverHeight + metadataHeight + detailHeight + rowGap * 2f;
        float stackY = y + (height - stackHeight) * 0.5f;
        String server = CustomFontRenderer.trimToWidth(localizedServer(item.server()), textWidth,
                serverSize, FontWeight.SEMIBOLD);
        String eventLine = localizedEventText(item.name());
        if (!item.subtitle().isBlank()) eventLine += " | " + localizedEventText(item.subtitle());
        eventLine = CustomFontRenderer.trimToWidth(eventLine, textWidth, detailSize, FontWeight.MEDIUM);
        CustomFontRenderer.drawText(context, server, textX, stackY, serverSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        String metadata = item.rarity().isBlank() ? providerName(item) : localizedEventText(item.rarity());
        metadata = CustomFontRenderer.trimToWidth(metadata, textWidth, metadataSize, FontWeight.SEMIBOLD);
        float metadataY = stackY + serverHeight + rowGap;
        CustomFontRenderer.drawText(context, metadata, textX, metadataY, metadataSize,
                RenderUtil.withAlpha(rarityTextColor(item), alpha), FontWeight.SEMIBOLD);
        float detailY = metadataY + metadataHeight + rowGap;
        CustomFontRenderer.drawText(context, eventLine, textX, detailY, detailSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, 0.72f * alpha), FontWeight.MEDIUM);

        if (actionSize > 0f) {
            float buttonX = x + width - actionSize - 4.5f * scale;
            float buttonY = y + (height - actionSize) * 0.5f;
            boolean actionHovered = hit(mouseX, mouseY, buttonX, buttonY, actionSize, actionSize);
            float pressed = pressProgress(item.key());
            float pressScale = 1f - 0.02f * pressed;
            float centerX = buttonX + actionSize * 0.5f;
            float centerY = buttonY + actionSize * 0.5f;
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY, 0f);
            context.getMatrices().scale(pressScale, pressScale, 1f);
            context.getMatrices().translate(-centerX, -centerY, 0f);
            float iconSize = 16f * scale;
            CustomFontRenderer.drawCenteredText(context, actionIcon, centerX,
                    centeredTextY(buttonY, actionSize, iconSize, FontWeight.SOLAR_ICONS),
                    iconSize, RenderUtil.withAlpha(MenuScreen.TEXT_DARK,
                            (action == EventAction.CURRENT ? 0.78f : 0.90f + (actionHovered ? 0.10f : 0f)) * alpha),
                    FontWeight.SOLAR_ICONS);
            context.getMatrices().pop();
            if (action == EventAction.CONNECT || action == EventAction.WAYPOINT) {
                actionBounds.put(item.key(), new Bounds(buttonX, buttonY, actionSize, actionSize));
            }
        }
    }

    private void renderEmpty(DrawContext context, float x, float y, float width, float height, float scale,
                             int mouseX, int mouseY, float alpha) {
        EventFeedService service = EventFeedService.getInstance();
        float centerX = x + width * 0.5f;
        float iconDiameter = 24f * scale;
        float titleSize = 7.4f * scale;
        float statusSize = 6.2f * scale;
        float buttonHeight = 19f * scale;
        float titleHeight = CustomFontRenderer.getTextHeight(titleSize, FontWeight.SEMIBOLD);
        float statusHeight = CustomFontRenderer.getTextHeight(statusSize, FontWeight.REGULAR);
        float groupHeight = iconDiameter + 8f * scale + titleHeight + 3f * scale
                + statusHeight + 8f * scale + buttonHeight;
        float groupY = y + (height - groupHeight) * 0.5f;
        float iconCenterY = groupY + iconDiameter * 0.5f;
        RenderUtil.drawCircle(context, centerX, iconCenterY, iconDiameter * 0.5f,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.13f * alpha));
        float emptyIconSize = 9.5f * scale;
        CustomFontRenderer.drawCenteredText(context, SolarIcons.EVENTS, centerX,
                centeredTextY(groupY, iconDiameter, emptyIconSize, FontWeight.SOLAR_ICONS),
                emptyIconSize, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SOLAR_ICONS);
        String title = switch (service.status()) {
            case LOADING -> ClientLanguage.text("ui.events.loading", "Получаем события");
            case ERROR -> ClientLanguage.text("ui.events.error", "Не удалось обновить события");
            case UNSUPPORTED, IDLE -> ClientLanguage.text("ui.events.unsupported", "Ивенты недоступны на этом сервере");
            case READY -> ClientLanguage.text("ui.events.empty", "Здесь пока тихо");
        };
        title = CustomFontRenderer.trimToWidth(title, width - 8f * scale, titleSize, FontWeight.SEMIBOLD);
        float titleY = groupY + iconDiameter + 8f * scale;
        CustomFontRenderer.drawCenteredText(context, title, centerX, titleY,
                titleSize, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        String status = CustomFontRenderer.trimToWidth(localizedStatus(service.statusText()), width - 12f * scale,
                statusSize, FontWeight.REGULAR);
        float statusY = titleY + titleHeight + 3f * scale;
        CustomFontRenderer.drawCenteredText(context, status, centerX, statusY,
                statusSize, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.REGULAR);

        float buttonWidth = 62f * scale;
        float buttonX = centerX - buttonWidth * 0.5f;
        float buttonY = statusY + statusHeight + 8f * scale;
        refreshHover = RenderUtil.animate(refreshHover,
                hit(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight) ? 1f : 0f,
                0.24f, RenderUtil.getFrameDelta());
        RenderUtil.drawRoundedGradientH(context, buttonX, buttonY, buttonWidth, buttonHeight, 6f * scale,
                RenderUtil.withAlpha(MenuScreen.ACCENT, (0.24f + refreshHover * 0.08f) * alpha),
                RenderUtil.withAlpha(MenuScreen.ACCENT, (0.13f + refreshHover * 0.07f) * alpha));
        float refreshIconSize = 8.4f * scale;
        CustomFontRenderer.drawText(context, SolarIcons.REFRESH, buttonX + 7f * scale,
                centeredTextY(buttonY, buttonHeight, refreshIconSize, FontWeight.SOLAR_ICONS),
                refreshIconSize, RenderUtil.withAlpha(0xFFB8BAFF, alpha), FontWeight.SOLAR_ICONS);
        float refreshTextSize = 6.2f * scale;
        CustomFontRenderer.drawCenteredText(context,
                service.status() == EventFeedService.Status.LOADING
                        ? ClientLanguage.text("ui.events.refreshing", "Обновляем…")
                        : ClientLanguage.text("ui.events.refresh", "Обновить"),
                centerX + 5f * scale,
                centeredTextY(buttonY, buttonHeight, refreshTextSize, FontWeight.SEMIBOLD), refreshTextSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        refreshBounds = new Bounds(buttonX, buttonY, buttonWidth, buttonHeight);
    }

    private void renderScrollbar(DrawContext context, float x, float y, float width, float height, float alpha) {
        if (maxScroll <= 0.5f) return;
        float trackHeight = height - 8f * scale;
        float thumbHeight = Math.max(22f * scale, trackHeight * height / (height + maxScroll));
        float progress = -scroll / maxScroll;
        float thumbY = y + 4f * scale + (trackHeight - thumbHeight) * progress;
        RenderUtil.drawRoundedRect(context, x + width - 4f * scale, thumbY, 1.35f * scale, thumbHeight,
                0.8f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT, 0.42f * alpha));
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !hit(mouseX, mouseY, x, y, width, height)) return false;
        float tabHeight = TAB_HEIGHT * scale;
        if (hit(mouseX, mouseY, headerX, y, headerWidth, tabHeight)) {
            int next = Math.max(0, Math.min(TAB_KINDS.length - 1,
                    (int) ((mouseX - headerX) / (headerWidth / TAB_KINDS.length))));
            if (next != tab) {
                tab = next;
                scroll = scrollTarget = 0f;
                openedAt = System.currentTimeMillis() - 55L;
                EnoughSoundPlayer.playScroll();
            }
            return true;
        }
        EventFeedService service = EventFeedService.getInstance();
        for (EventItem item : service.items(TAB_KINDS[tab])) {
            Bounds bounds = actionBounds.get(item.key());
            if (bounds != null && bounds.contains(mouseX, mouseY)) {
                pressTimes.put(item.key(), System.currentTimeMillis());
                if (service.performAction(item)) EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        if (refreshBounds != null && refreshBounds.contains(mouseX, mouseY)) {
            service.refreshNow();
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

    private float pressProgress(String key) {
        long started = pressTimes.getOrDefault(key, 0L);
        if (started == 0L) return 0f;
        float progress = clamp((System.currentTimeMillis() - started) / 180f);
        if (progress >= 1f) {
            pressTimes.remove(key);
            return 0f;
        }
        return (float) Math.sin(progress * Math.PI);
    }

    private static String actionIcon(EventAction action) {
        return switch (action) {
            case CONNECT -> SolarIcons.PLAY;
            case WAYPOINT -> SolarIcons.LOCATION;
            case CURRENT -> SolarIcons.CHECK;
            case NONE -> "";
        };
    }

    private static int eventColor(EventItem item) {
        String rarity = item.rarity().toLowerCase(Locale.ROOT);
        if (rarity.contains("легенд")) return 0xFFFFB65C;
        if (rarity.contains("миф")) return 0xFFFF6068;
        if (rarity.contains("эпич") || rarity.contains("элит")) return 0xFFCF76FF;
        if (rarity.contains("богат")) return 0xFF62D2FF;
        if (rarity.contains("солид")) return 0xFFC0C0C0;
        if (rarity.contains("редк")) return 0xFF62B7FF;
        if (rarity.contains("взрыв")) return 0xFFFF6D72;
        return item.source() == EventFeedService.Source.HOLYWORLD ? 0xFF72D7FF : MenuScreen.ACCENT_LIGHT;
    }

    private static int rarityTextColor(EventItem item) {
        String rarity = item.rarity().toLowerCase(Locale.ROOT);
        if (rarity.contains("легенд")) return 0xFFFFD899;
        if (rarity.contains("миф")) return 0xFFFFB3B4;
        if (rarity.contains("эпич") || rarity.contains("элит")) return 0xFFD6B5FF;
        if (rarity.contains("солид")) return 0xFFC0C0C0;
        if (rarity.contains("богат") || rarity.contains("редк")) return 0xFF77D2FF;
        return 0xFFD7C8FF;
    }

    private static String localizedServer(String server) {
        if (server == null) return "";
        return server.replaceAll("(?iu)anarchy|анархия",
                ClientLanguage.text("ui.events.anarchy", "Анархия"));
    }

    private static String tabName(int index) {
        return switch (index) {
            case 0 -> ClientLanguage.text("ui.events.tab.current", "Текущие");
            case 1 -> ClientLanguage.text("ui.events.tab.upcoming", "Предстоящие");
            default -> ClientLanguage.text("ui.events.tab.mines", "Шахты");
        };
    }

    private static String localizedStatus(String status) {
        if (status == null || status.isBlank()) return "";
        return switch (status) {
            case "Зайдите на FunTime или HolyWorld" -> ClientLanguage.text("ui.events.status.join", status);
            case "Ивенты доступны на FunTime и HolyWorld" -> ClientLanguage.text("ui.events.status.available", status);
            case "Сначала подключитесь к серверу" -> ClientLanguage.text("ui.events.status.connect_first", status);
            case "Подключаем FunTime Events" -> ClientLanguage.text("ui.events.status.connecting", status);
            case "События обновляются автоматически" -> ClientLanguage.text("ui.events.status.auto_refresh", status);
            case "FunTime Events временно недоступен" -> ClientLanguage.text("ui.events.status.funtime_unavailable", status);
            case "Не удалось получить события FunTime" -> ClientLanguage.text("ui.events.status.fetch_failed", status);
            case "Показаны данные из чата FunTime" -> ClientLanguage.text("ui.events.status.chat_data", status);
            case "Показаны последние данные" -> ClientLanguage.text("ui.events.status.cached", status);
            case "Обновляем события HolyWorld" -> ClientLanguage.text("ui.events.status.holyworld_loading", status);
            case "HolyWorld API временно недоступен" -> ClientLanguage.text("ui.events.status.holyworld_unavailable", status);
            case "События HolyWorld обновлены" -> ClientLanguage.text("ui.events.status.holyworld_updated", status);
            case "Активных событий пока нет" -> ClientLanguage.text("ui.events.status.no_active", status);
            default -> status;
        };
    }

    private static String localizedEventText(String text) {
        if (text == null || text.isBlank()) return "";
        return switch (text) {
            case "Следующий ивент" -> ClientLanguage.text("ui.events.item.next", text);
            case "Активный ивент" -> ClientLanguage.text("ui.events.item.active", text);
            case "Автошахта" -> ClientLanguage.text("ui.events.item.mine", text);
            case "Шахта активна" -> ClientLanguage.text("ui.events.item.mine_active", text);
            case "Событие уже активно" -> ClientLanguage.text("ui.events.item.already_active", text);
            case "Данные получены из чата FunTime" -> ClientLanguage.text("ui.events.item.from_chat", text);
            case "Координаты получены из чата FunTime" -> ClientLanguage.text("ui.events.item.coordinates_from_chat", text);
            case "Обычный" -> ClientLanguage.text("ui.events.rarity.common", text);
            case "Редкий" -> ClientLanguage.text("ui.events.rarity.rare", text);
            case "Эпический" -> ClientLanguage.text("ui.events.rarity.epic", text);
            case "Легендарный" -> ClientLanguage.text("ui.events.rarity.legendary", text);
            case "Взрывной" -> ClientLanguage.text("ui.events.rarity.explosive", text);
            default -> text;
        };
    }

    private static String providerName(EventItem item) {
        return item.source() == EventFeedService.Source.FUNTIME ? "FunTime" : "HolyWorld";
    }

    private static String formatTime(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60);
    }

    private float timerProgress(String key, int secondsLeft) {
        int window = timerWindows.merge(key, Math.max(1, secondsLeft), Math::max);
        return Math.max(0.035f, Math.min(1f, secondsLeft / (float) Math.max(1, window)));
    }

    private static float centeredTextY(float boxY, float boxHeight, float fontSize, FontWeight weight) {
        return boxY + (boxHeight - CustomFontRenderer.getTextHeight(fontSize, weight)) * 0.5f;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static boolean hit(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    private record Bounds(float x, float y, float width, float height) {
        boolean contains(float mouseX, float mouseY) {
            return hit(mouseX, mouseY, x, y, width, height);
        }
    }
}
