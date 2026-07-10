package fun.enoughvisuals.ui.clickgui.components;

import fun.enoughvisuals.systems.config.ConfigSystem;
import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.gps.GpsSystem.WaypointView;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class IntegratedToolPage {
    public enum Type {
        CONFIGS("\uF001", "ui.client_tools.configs.title", "Конфиги"),
        FRIENDS("\uF003", "ui.client_tools.friends.title", "Друзья"),
        GPS("\uF000", "ui.client_tools.gps.title", "Метки");

        private final String icon;
        private final String languageKey;
        private final String fallback;

        Type(String icon, String languageKey, String fallback) {
            this.icon = icon;
            this.languageKey = languageKey;
            this.fallback = fallback;
        }

        public String icon() { return icon; }
        public String title() { return ClientLanguage.text(languageKey, fallback); }
    }

    private final EnumMap<Type, PageState> states = new EnumMap<>(Type.class);
    private Type type = Type.CONFIGS;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scale = 1f;
    private float listX;
    private float listY;
    private float listW;
    private float listH;
    private float editorX;
    private float editorY;
    private float editorW;
    private float editorH;
    private float listScroll;
    private float listScrollTarget;
    private float listMaxScroll;
    private final List<Row> visibleRows = new ArrayList<>();
    private Input focusedInput;
    private String searchQuery = "";
    private long pageChangedAt = System.currentTimeMillis();

    public IntegratedToolPage() {
        for (Type value : Type.values()) states.put(value, new PageState());
    }

    public void activate(Type next) {
        if (next == null) return;
        if (type != next) {
            type = next;
            pageChangedAt = System.currentTimeMillis();
            listScroll = listScrollTarget = 0f;
            focusedInput = null;
        }
        if (type == Type.CONFIGS) {
            ConfigSystem config = ConfigSystem.getInstance();
            config.initialize();
            PageState state = state();
            if (state.name.value.isEmpty()) state.name.set(config.getLastConfigName());
        }
    }

    public Type type() { return type; }

    public void render(DrawContext context, float x, float y, float width, float height, float scale,
                       int mouseX, int mouseY, float alpha, String searchQuery) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.searchQuery = searchQuery == null ? "" : searchQuery.trim();

        float gap = 8f * scale;
        listW = (width - gap) * 0.585f;
        editorW = width - gap - listW;
        listX = x;
        listY = y;
        listH = height;
        editorX = x + listW + gap;
        editorY = y;
        editorH = height;

        float frame = RenderUtil.getFrameDelta();
        listScroll = RenderUtil.animate(listScroll, listScrollTarget, 0.29f, frame);
        float pageProgress = clamp((System.currentTimeMillis() - pageChangedAt) / 330f);
        float reveal = RenderUtil.easeOutCubic(pageProgress);
        float pageAlpha = alpha * RenderUtil.easeOutQuad(pageProgress);
        float offset = (1f - reveal) * 12f * scale;

        renderList(context, listX - offset, listY, listW, listH, scale, mouseX, mouseY, pageAlpha);
        renderEditor(context, editorX + offset, editorY, editorW, editorH, scale, mouseX, mouseY, pageAlpha);
    }

    private void renderList(DrawContext context, float px, float py, float pw, float ph, float s,
                            int mouseX, int mouseY, float alpha) {
        listX = px;
        listY = py;
        listW = pw;
        listH = ph;
        RenderUtil.drawRoundedRect(context, px, py, pw, ph, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.87f * alpha));
        RenderUtil.drawRoundedOutline(context, px, py, pw, ph, 8f * s, 0.7f,
                RenderUtil.withAlpha(0xFFFFFF, 0.035f * alpha));

        float headerH = 30f * s;
        CustomFontRenderer.drawText(context, type.title(), px + 11f * s, py + 9.4f * s,
                7.1f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        List<Row> rows = rows();
        String status = status(rows.size());
        CustomFontRenderer.drawRightText(context, status, px + pw - 10f * s, py + 10.2f * s,
                5.6f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.9f * alpha), FontWeight.REGULAR);
        RenderUtil.drawRect(context, px + 9f * s, py + headerH, pw - 18f * s, 0.6f,
                RenderUtil.withAlpha(0xFFFFFF, 0.055f * alpha));

        float rowH = 34f * s;
        float contentY = py + headerH + 6f * s;
        float contentH = ph - headerH - 12f * s;
        visibleRows.clear();
        visibleRows.addAll(rows);
        listMaxScroll = Math.max(0f, rows.size() * rowH - contentH);
        listScrollTarget = Math.max(-listMaxScroll, Math.min(0f, listScrollTarget));

        context.draw();
        StencilUtil.enableScissor(px + 4f * s, contentY, pw - 8f * s, contentH);
        float rowY = contentY + listScroll;
        PageState state = state();
        for (Row row : rows) {
            if (rowY + rowH >= contentY - rowH && rowY <= contentY + contentH + rowH) {
                boolean selected = row.id.equals(state.selectedId);
                boolean hovered = hit(mouseX, mouseY, px + 7f * s, rowY, pw - 14f * s, rowH - 3f * s);
                if (selected || hovered) {
                    RenderUtil.drawRoundedRect(context, px + 7f * s, rowY, pw - 14f * s, rowH - 3f * s, 6f * s,
                            RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : 0xFFFFFF,
                                    alpha * (selected ? 0.14f : 0.027f)));
                }
                RenderUtil.drawRoundedRect(context, px + 11f * s, rowY + 7f * s, 20f * s, 20f * s, 6f * s,
                        RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : MenuScreen.CHIP_BG,
                                alpha * (selected ? 0.24f : 0.88f)));
                CustomFontRenderer.drawCenteredText(context, type.icon(), px + 21f * s, rowY + 12.2f * s,
                        9.5f * s, RenderUtil.withAlpha(selected ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM, alpha),
                        FontWeight.CLICK_GUI);
                String title = CustomFontRenderer.trimToWidth(row.title, pw - 54f * s, 6.5f * s, FontWeight.SEMIBOLD);
                String subtitle = CustomFontRenderer.trimToWidth(row.subtitle, pw - 54f * s, 5.4f * s, FontWeight.REGULAR);
                CustomFontRenderer.drawText(context, title, px + 39f * s, rowY + 7.4f * s,
                        6.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * (selected ? 1f : 0.91f)), FontWeight.SEMIBOLD);
                CustomFontRenderer.drawText(context, subtitle, px + 39f * s, rowY + 19.2f * s,
                        5.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.88f * alpha), FontWeight.REGULAR);
            }
            rowY += rowH;
        }
        context.draw();
        StencilUtil.disableScissor();

        if (rows.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, emptyText(), px + pw * 0.5f, py + ph * 0.45f,
                    6.3f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.REGULAR);
        }
        if (listMaxScroll > 0.5f) {
            float thumbH = Math.max(18f * s, contentH * contentH / (contentH + listMaxScroll));
            float thumbY = contentY + (-listScroll / listMaxScroll) * (contentH - thumbH);
            RenderUtil.drawRoundedRect(context, px + pw - 3.5f * s, thumbY, 1.5f * s, thumbH, 0.75f * s,
                    RenderUtil.withAlpha(MenuScreen.ACCENT, 0.34f * alpha));
        }
    }

    private void renderEditor(DrawContext context, float px, float py, float pw, float ph, float s,
                              int mouseX, int mouseY, float alpha) {
        RenderUtil.drawRoundedRect(context, px, py, pw, ph, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.87f * alpha));
        RenderUtil.drawRoundedOutline(context, px, py, pw, ph, 8f * s, 0.7f,
                RenderUtil.withAlpha(0xFFFFFF, 0.035f * alpha));
        float cx = px + 10f * s;
        float cy = py + 10f * s;
        float cw = pw - 20f * s;
        PageState state = state();
        state.buttons.clear();

        String heading = switch (type) {
            case CONFIGS -> ClientLanguage.text("ui.client_tools.configs.editor", "Конфиг");
            case FRIENDS -> ClientLanguage.text("ui.client_tools.friends.editor", "Друг");
            case GPS -> ClientLanguage.text("ui.client_tools.gps.editor", "Метка");
        };
        CustomFontRenderer.drawText(context, heading, cx, cy, 7f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        cy += 17f * s;

        if (type == Type.CONFIGS) {
            cy = renderInput(context, state.name, ClientLanguage.text("ui.client_tools.configs.name", "Название"),
                    cx, cy, cw, s, alpha);
            cy += 7f * s;
            cy = renderButton(context, "config_save", ClientLanguage.text("ui.client_tools.configs.save_current", "Сохранить текущий"), cx, cy, cw, s, mouseX, mouseY, true, alpha);
            cy = renderButton(context, "config_save_as", ClientLanguage.text("ui.client_tools.configs.save_as", "Сохранить как"), cx, cy, cw, s, mouseX, mouseY, !state.name.text().isBlank(), alpha);
            cy = renderButton(context, "config_load", ClientLanguage.text("ui.client_tools.configs.load", "Загрузить"), cx, cy, cw, s, mouseX, mouseY, selected() != null, alpha);
            cy = renderButton(context, "config_delete", ClientLanguage.text("ui.client_tools.delete", "Удалить"), cx, cy, cw, s, mouseX, mouseY, selected() != null, alpha);
            renderButton(context, "config_folder", ClientLanguage.text("ui.client_tools.configs.open_folder", "Открыть папку"), cx, cy, cw, s, mouseX, mouseY, true, alpha);
        } else if (type == Type.FRIENDS) {
            cy = renderInput(context, state.name, ClientLanguage.text("ui.client_tools.friends.name", "Никнейм"),
                    cx, cy, cw, s, alpha);
            cy += 7f * s;
            cy = renderButton(context, "friend_add", ClientLanguage.text("ui.client_tools.add", "Добавить"), cx, cy, cw, s, mouseX, mouseY, !state.name.text().isBlank(), alpha);
            renderButton(context, "friend_delete", ClientLanguage.text("ui.client_tools.delete", "Удалить"), cx, cy, cw, s, mouseX, mouseY, selected() != null, alpha);
        } else {
            cy = renderInput(context, state.name, ClientLanguage.text("ui.client_tools.gps.name", "Название"),
                    cx, cy, cw, s, alpha);
            cy += 6f * s;
            float gap = 4f * s;
            float fieldW = (cw - gap * 2f) / 3f;
            renderCompactInput(context, state.x, "X", cx, cy, fieldW, s, alpha);
            renderCompactInput(context, state.y, "Y", cx + fieldW + gap, cy, fieldW, s, alpha);
            renderCompactInput(context, state.z, "Z", cx + (fieldW + gap) * 2f, cy, fieldW, s, alpha);
            cy += 28f * s;
            cy = renderButton(context, "gps_current", ClientLanguage.text("ui.client_tools.gps.current_pos", "Текущая позиция"), cx, cy, cw, s, mouseX, mouseY, true, alpha);
            cy = renderButton(context, "gps_save", ClientLanguage.text("ui.client_tools.gps.add_update", "Добавить / обновить"), cx, cy, cw, s, mouseX, mouseY, gpsValid(), alpha);
            renderButton(context, "gps_delete", ClientLanguage.text("ui.client_tools.delete", "Удалить"), cx, cy, cw, s, mouseX, mouseY, selected() != null, alpha);
        }

        if (!state.notice.isEmpty()) {
            float noticeProgress = clamp((System.currentTimeMillis() - state.noticeAt) / 260f);
            float noticeAlpha = Math.min(1f, RenderUtil.easeOutCubic(noticeProgress));
            int color = state.noticeGood ? MenuScreen.ACCENT_LIGHT : 0xFFFF7891;
            String notice = CustomFontRenderer.trimToWidth(state.notice, pw - 20f * s, 5.4f * s, FontWeight.REGULAR);
            CustomFontRenderer.drawCenteredText(context, notice, px + pw * 0.5f, py + ph - 14f * s,
                    5.4f * s, RenderUtil.withAlpha(color, alpha * noticeAlpha), FontWeight.REGULAR);
        }
    }

    private float renderInput(DrawContext context, Input input, String label, float x, float y, float w, float s, float alpha) {
        CustomFontRenderer.drawText(context, label, x, y, 5.6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.9f * alpha), FontWeight.REGULAR);
        renderCompactInput(context, input, input.placeholder, x, y + 9f * s, w, s, alpha);
        return y + 31f * s;
    }

    private void renderCompactInput(DrawContext context, Input input, String placeholder,
                                    float x, float y, float w, float s, float alpha) {
        boolean focused = focusedInput == input;
        RenderUtil.drawRoundedRect(context, x, y, w, 19f * s, 5f * s,
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, alpha * (focused ? 0.98f : 0.82f)));
        if (focused) RenderUtil.drawRoundedOutline(context, x, y, w, 19f * s, 5f * s, 0.7f,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.32f * alpha));
        String value = input.value.isEmpty() ? placeholder : input.value;
        value = CustomFontRenderer.trimToWidth(value, w - 12f * s, 5.7f * s, FontWeight.REGULAR);
        CustomFontRenderer.drawText(context, value, x + 6f * s, y + 6.4f * s, 5.7f * s,
                RenderUtil.withAlpha(input.value.isEmpty() ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE, alpha), FontWeight.REGULAR);
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caret = x + 6f * s + CustomFontRenderer.getTextWidth(value, 5.7f * s, FontWeight.REGULAR) + 1f;
            RenderUtil.drawRect(context, Math.min(caret, x + w - 6f * s), y + 5f * s, 0.7f, 9f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
        input.bounds = new Bounds(x, y, w, 19f * s);
    }

    private float renderButton(DrawContext context, String id, String label, float x, float y, float w, float s,
                               int mouseX, int mouseY, boolean enabled, float alpha) {
        float h = 21f * s;
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, w, h);
        int background = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.CHIP_BG, alpha * (enabled ? 0.88f : 0.42f)),
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.24f * alpha), hovered ? 1f : 0f);
        RenderUtil.drawRoundedRect(context, x, y, w, h, 5.5f * s, background);
        CustomFontRenderer.drawCenteredText(context, label, x + w * 0.5f, y + 7.1f * s,
                5.8f * s, RenderUtil.withAlpha(enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM,
                        alpha * (enabled ? 0.96f : 0.52f)), FontWeight.SEMIBOLD);
        state().buttons.add(new Button(id, new Bounds(x, y, w, h), enabled));
        return y + 25f * s;
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !hit(mouseX, mouseY, x, y, width, height)) return false;
        PageState state = state();
        float rowH = 34f * scale;
        float rowY = listY + 36f * scale + listScroll;
        if (mouseY >= listY + 36f * scale && mouseY <= listY + listH - 6f * scale) {
            for (Row row : visibleRows) {
                if (hit(mouseX, mouseY, listX + 7f * scale, rowY, listW - 14f * scale, rowH - 3f * scale)) {
                    state.selectedId = row.id;
                    fillFromSelection(row);
                    focusedInput = null;
                    EnoughSoundPlayer.playScroll();
                    return true;
                }
                rowY += rowH;
            }
        }
        for (Input input : inputs(state)) {
            if (input.bounds != null && input.bounds.contains(mouseX, mouseY)) {
                focusedInput = input;
                return true;
            }
        }
        for (Button action : state.buttons) {
            if (action.enabled && action.bounds.contains(mouseX, mouseY)) {
                perform(action.id);
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        focusedInput = null;
        return true;
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
        if (!hit(mouseX, mouseY, listX, listY, listW, listH) || listMaxScroll <= 0f) return false;
        listScrollTarget += (float) amount * 20f * scale;
        listScrollTarget = Math.max(-listMaxScroll, Math.min(0f, listScrollTarget));
        EnoughSoundPlayer.playScroll();
        return true;
    }

    public boolean keyPressed(int key) {
        if (focusedInput == null) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            focusedInput = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !focusedInput.value.isEmpty()) {
            focusedInput.value = focusedInput.value.substring(0, focusedInput.value.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            focusedInput.value = "";
            return true;
        }
        return true;
    }

    public boolean charTyped(char chr) {
        if (focusedInput == null) return false;
        if (!Character.isISOControl(chr) && focusedInput.value.length() < focusedInput.maxLength) {
            focusedInput.value += chr;
        }
        return true;
    }

    public void clearFocus() { focusedInput = null; }

    private List<Row> rows() {
        String query = searchQuery.toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        if (type == Type.CONFIGS) {
            String active = ConfigSystem.getInstance().getLastConfigName();
            for (String name : ConfigSystem.getInstance().listConfigs()) {
                String subtitle = name.equalsIgnoreCase(active)
                        ? ClientLanguage.text("ui.client_tools.configs.active", "Активный конфиг")
                        : ClientLanguage.text("ui.client_tools.configs.saved", "Сохранённый конфиг");
                addIfMatches(rows, new Row(name, name, subtitle), query);
            }
        } else if (type == Type.FRIENDS) {
            for (String name : FriendRepo.listFriends()) {
                addIfMatches(rows, new Row(name, name, ClientLanguage.text("ui.client_tools.friends.row_subtitle", "Друг")), query);
            }
        } else {
            for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
                String subtitle = String.format("%d %d %d - %d блоков",
                        (int) waypoint.x(), (int) waypoint.y(), (int) waypoint.z(), waypoint.distance());
                addIfMatches(rows, new Row(String.valueOf(waypoint.activeIndex()), waypoint.name(), subtitle), query);
            }
        }
        return rows;
    }

    private static void addIfMatches(List<Row> rows, Row row, String query) {
        if (query.isEmpty() || row.title.toLowerCase(Locale.ROOT).contains(query)
                || row.subtitle.toLowerCase(Locale.ROOT).contains(query)) rows.add(row);
    }

    private String status(int count) {
        return switch (type) {
            case CONFIGS -> count + " / " + ConfigSystem.getInstance().getLastConfigName();
            case FRIENDS -> String.valueOf(count);
            case GPS -> count + " / " + trimScope(GpsSystem.getInstance().currentScopeLabel());
        };
    }

    private String emptyText() {
        return switch (type) {
            case CONFIGS -> ClientLanguage.text("ui.client_tools.configs.empty", "Конфиги не найдены");
            case FRIENDS -> ClientLanguage.text("ui.client_tools.friends.empty", "Список друзей пуст");
            case GPS -> ClientLanguage.text("ui.client_tools.gps.empty", "Меток пока нет");
        };
    }

    private Row selected() {
        String id = state().selectedId;
        if (id.isEmpty()) return null;
        for (Row row : rows()) if (row.id.equals(id)) return row;
        return null;
    }

    private void fillFromSelection(Row row) {
        PageState state = state();
        if (type == Type.CONFIGS || type == Type.FRIENDS) {
            state.name.set(row.title);
            return;
        }
        for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
            if (String.valueOf(waypoint.activeIndex()).equals(row.id)) {
                state.name.set(waypoint.name());
                state.x.set(String.valueOf((int) waypoint.x()));
                state.y.set(String.valueOf((int) waypoint.y()));
                state.z.set(String.valueOf((int) waypoint.z()));
                return;
            }
        }
    }

    private void perform(String id) {
        PageState state = state();
        switch (id) {
            case "config_save" -> notice(ConfigSystem.getInstance().saveCurrent(), "Конфиг сохранён", "Не удалось сохранить");
            case "config_save_as" -> {
                boolean ok = ConfigSystem.getInstance().saveAs(state.name.text());
                if (ok) state.selectedId = ConfigSystem.getInstance().getLastConfigName();
                notice(ok, "Конфиг сохранён", "Не удалось сохранить");
            }
            case "config_load" -> {
                Row row = selected();
                boolean ok = row != null && ConfigSystem.getInstance().load(row.id);
                notice(ok, "Конфиг загружен", "Не удалось загрузить");
            }
            case "config_delete" -> {
                Row row = selected();
                boolean ok = row != null && ConfigSystem.getInstance().remove(row.id);
                if (ok) state.selectedId = "";
                notice(ok, "Конфиг удалён", "Не удалось удалить");
            }
            case "config_folder" -> {
                try {
                    Util.getOperatingSystem().open(ConfigSystem.getInstance().getConfigDirectory().toFile());
                    notice(true, "Папка открыта", "");
                } catch (Exception exception) {
                    notice(false, "", "Не удалось открыть папку");
                }
            }
            case "friend_add" -> {
                boolean ok = FriendRepo.addFriend(state.name.text());
                if (ok) {
                    state.selectedId = state.name.text().toLowerCase(Locale.ROOT);
                    state.name.set("");
                }
                notice(ok, "Друг добавлен", "Ник уже в списке");
            }
            case "friend_delete" -> {
                Row row = selected();
                boolean ok = row != null && FriendRepo.removeFriend(row.id);
                if (ok) {
                    state.selectedId = "";
                    state.name.set("");
                }
                notice(ok, "Друг удалён", "Не удалось удалить");
            }
            case "gps_current" -> useCurrentPosition();
            case "gps_save" -> saveWaypoint();
            case "gps_delete" -> {
                Row row = selected();
                int index = row == null ? -1 : parseInt(row.id, -1);
                boolean ok = index >= 0 && GpsSystem.getInstance().removeActiveWaypoint(index);
                if (ok) state.selectedId = "";
                notice(ok, "Метка удалена", "Не удалось удалить");
            }
        }
    }

    private void useCurrentPosition() {
        BlockPos position = GpsSystem.getInstance().currentPlayerBlockPos();
        if (position == null) {
            notice(false, "", "Позиция игрока недоступна");
            return;
        }
        PageState state = state();
        if (state.name.text().isBlank()) state.name.set("Метка");
        state.x.set(String.valueOf(position.getX()));
        state.y.set(String.valueOf(position.getY()));
        state.z.set(String.valueOf(position.getZ()));
        notice(true, "Координаты заполнены", "");
    }

    private void saveWaypoint() {
        PageState state = state();
        Double px = parseDouble(state.x.text());
        Double py = parseDouble(state.y.text());
        Double pz = parseDouble(state.z.text());
        if (state.name.text().isBlank() || px == null || py == null || pz == null) {
            notice(false, "", "Проверь название и координаты");
            return;
        }
        GpsSystem.getInstance().addOrReplaceWaypoint(state.name.text(), px, py, pz);
        for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
            if (waypoint.name().equalsIgnoreCase(state.name.text())) {
                state.selectedId = String.valueOf(waypoint.activeIndex());
                break;
            }
        }
        notice(true, "Метка сохранена", "");
    }

    private boolean gpsValid() {
        PageState state = state();
        return !state.name.text().isBlank() && parseDouble(state.x.text()) != null
                && parseDouble(state.y.text()) != null && parseDouble(state.z.text()) != null;
    }

    private void notice(boolean good, String goodText, String badText) {
        PageState state = state();
        state.noticeGood = good;
        state.notice = good ? goodText : badText;
        state.noticeAt = System.currentTimeMillis();
    }

    private List<Input> inputs(PageState state) {
        return type == Type.GPS ? List.of(state.name, state.x, state.y, state.z) : List.of(state.name);
    }

    private PageState state() {
        return states.get(type);
    }

    private static String trimScope(String scope) {
        if (scope == null) return "unknown";
        return scope.length() > 19 ? scope.substring(0, 16) + "..." : scope;
    }

    private static Double parseDouble(String raw) {
        try { return Double.parseDouble(raw.replace(',', '.')); }
        catch (Exception ignored) { return null; }
    }

    private static int parseInt(String raw, int fallback) {
        try { return Integer.parseInt(raw); }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static final class PageState {
        private String selectedId = "";
        private final Input name = new Input("Название", 64);
        private final Input x = new Input("X", 18);
        private final Input y = new Input("Y", 18);
        private final Input z = new Input("Z", 18);
        private final List<Button> buttons = new ArrayList<>();
        private String notice = "";
        private boolean noticeGood;
        private long noticeAt;
    }

    private static final class Input {
        private final String placeholder;
        private final int maxLength;
        private String value = "";
        private Bounds bounds;

        private Input(String placeholder, int maxLength) {
            this.placeholder = placeholder;
            this.maxLength = maxLength;
        }

        private String text() { return value.trim(); }
        private void set(String value) { this.value = value == null ? "" : value; }
    }

    private record Row(String id, String title, String subtitle) {}
    private record Button(String id, Bounds bounds, boolean enabled) {}
    private record Bounds(float x, float y, float w, float h) {
        private boolean contains(float px, float py) { return hit(px, py, x, y, w, h); }
    }
}
