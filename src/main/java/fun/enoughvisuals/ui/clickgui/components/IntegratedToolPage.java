package fun.enoughvisuals.ui.clickgui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.config.ConfigSystem;
import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.gps.GpsSystem.WaypointView;
import fun.enoughvisuals.systems.inventory.InventoryLayoutManager;
import fun.enoughvisuals.systems.inventory.InventoryLayoutManager.LayoutView;
import fun.enoughvisuals.systems.modules.modules.utils.Macros;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class IntegratedToolPage {
    public enum Type {
        CONFIGS("\uF001", "ui.client_tools.configs.title", "Конфиги", FontWeight.CLICK_GUI),
        FRIENDS("\uF003", "ui.client_tools.friends.title", "Друзья", FontWeight.CLICK_GUI),
        GPS("\uF000", "ui.client_tools.gps.title", "Метки", FontWeight.CLICK_GUI),
        MACROS("\uF005", "ui.macros.title", "Макросы", FontWeight.CLICK_GUI),
        INVENTORIES("\uF000", "ui.inventory_manager.title", "Менеджер инвентарей", FontWeight.BOOK);

        private final String icon;
        private final String languageKey;
        private final String fallback;
        private final FontWeight iconWeight;

        Type(String icon, String languageKey, String fallback, FontWeight iconWeight) {
            this.icon = icon;
            this.languageKey = languageKey;
            this.fallback = fallback;
            this.iconWeight = iconWeight;
        }

        public String icon() { return icon; }
        public String title() { return ClientLanguage.text(languageKey, fallback); }
        public FontWeight iconWeight() { return iconWeight; }
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
    private final Map<String, Float> rowHoverAnimations = new HashMap<>();
    private final Map<String, Long> rowBornAt = new HashMap<>();
    private final Map<String, Float> buttonHoverAnimations = new HashMap<>();
    private final Map<String, Long> buttonPressAt = new HashMap<>();
    private final Map<Input, Float> inputFocusAnimations = new HashMap<>();
    private final Map<String, Bounds> inventoryLoadBounds = new HashMap<>();
    private final Map<String, Bounds> inventoryDeleteBounds = new HashMap<>();
    private Bounds macroKeyBounds;
    private boolean macroBinding;
    private int editorElementIndex;
    private String pendingDeleteAction = "";
    private String pendingDeleteRowId = "";
    private Type pendingDeleteType;
    private long pendingDeleteAt;

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
            pendingDeleteAction = "";
            pendingDeleteRowId = "";
            pendingDeleteType = null;
            String prefix = type.name() + ":";
            rowBornAt.keySet().removeIf(key -> key.startsWith(prefix));
        }
        if (type == Type.CONFIGS) {
            ConfigSystem config = ConfigSystem.getInstance();
            config.initialize();
            PageState state = state();
            if (state.name.value.isEmpty()) state.name.set(config.getLastConfigName());
        }
        if (type == Type.MACROS && Macros.INSTANCE != null && !Macros.INSTANCE.isEnabled()) {
            Macros.INSTANCE.setEnabledSilent(true);
        }
        if (type == Type.INVENTORIES) InventoryLayoutManager.getInstance().initialize();
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
        flushPendingDelete();

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
        float reveal = RenderUtil.easeOutQuart(pageProgress);
        float pageAlpha = alpha * RenderUtil.easeOutCubic(pageProgress);
        float offset = (1f - reveal) * 14f * scale;

        if (type == Type.INVENTORIES) {
            renderInventoryManager(context, x, y, width, height, scale, mouseX, mouseY, pageAlpha, offset);
            return;
        }

        renderList(context, listX - offset, listY, listW, listH, scale, mouseX, mouseY, pageAlpha);
        renderEditor(context, editorX + offset, editorY, editorW, editorH, scale, mouseX, mouseY, pageAlpha);
    }

    private void renderInventoryManager(DrawContext context, float px, float py, float pw, float ph, float s,
                                        int mouseX, int mouseY, float alpha, float offset) {
        PageState state = state();
        state.buttons.clear();
        inventoryLoadBounds.clear();
        inventoryDeleteBounds.clear();
        List<Row> rows = rows();
        if (selected() == null && !rows.isEmpty()) state.selectedId = rows.get(0).id;

        float gap = 7f * s;
        float leftW = (pw - gap) * 0.59f;
        float rightW = pw - gap - leftW;
        float leftX = px - offset;
        float rightX = px + leftW + gap + offset;
        RenderUtil.drawRoundedRect(context, leftX, py, leftW, ph, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * alpha));
        RenderUtil.drawRoundedRect(context, rightX, py, rightW, ph, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.89f * alpha));

        CustomFontRenderer.drawText(context, Type.INVENTORIES.icon(), leftX + 10f * s, py + 9.2f * s,
                10.5f * s, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.BOOK);
        CustomFontRenderer.drawText(context, "Менеджер инвентарей", leftX + 25f * s, py + 9.8f * s,
                7.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(context, rows.size() + " сохранений", leftX + leftW - 10f * s,
                py + 10.5f * s, 5.6f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.92f * alpha), FontWeight.SEMIBOLD);

        float inputY = py + 28f * s;
        float saveW = 61f * s;
        renderCompactInput(context, state.name, "Название инвентаря", leftX + 9f * s, inputY,
                leftW - saveW - 22f * s, s, alpha);
        renderInventoryButton(context, "inventory_save", "+  Сохранить", leftX + leftW - saveW - 9f * s,
                inputY, saveW, 19f * s, s, mouseX, mouseY, !state.name.text().isBlank(), alpha);

        float rowsTop = inputY + 27f * s;
        float rowsBottom = py + ph - 23f * s;
        float rowH = 33f * s;
        listX = leftX + 5f * s;
        listY = rowsTop;
        listW = leftW - 10f * s;
        listH = rowsBottom - rowsTop;
        visibleRows.clear();
        visibleRows.addAll(rows);
        listMaxScroll = Math.max(0f, rows.size() * rowH - listH);
        listScrollTarget = Math.max(-listMaxScroll, Math.min(0f, listScrollTarget));

        context.draw();
        StencilUtil.enableScissor(listX, listY, listW, listH);
        float rowY = rowsTop + listScroll;
        int index = 0;
        for (Row row : rows) {
            String key = Type.INVENTORIES.name() + ":" + row.id;
            long born = rowBornAt.computeIfAbsent(key, ignored -> System.currentTimeMillis());
            float reveal = clamp((System.currentTimeMillis() - born - index * 26L) / 310f);
            float ease = RenderUtil.easeOutQuart(reveal);
            float rowAlpha = alpha * RenderUtil.easeOutCubic(reveal);
            float rowX = listX - (1f - ease) * 16f * s;
            if (pendingDeleteType == type && row.id.equals(pendingDeleteRowId)) {
                float remove = clamp((System.currentTimeMillis() - pendingDeleteAt) / 230f);
                rowAlpha *= 1f - RenderUtil.easeInCubic(remove);
                rowX -= RenderUtil.easeInCubic(remove) * 18f * s;
            }
            boolean selected = row.id.equals(state.selectedId);
            boolean hovered = hit(mouseX, mouseY, rowX + 3f * s, rowY, listW - 6f * s, rowH - 4f * s);
            float hover = RenderUtil.animate(rowHoverAnimations.getOrDefault(key, 0f), hovered ? 1f : 0f,
                    0.29f, RenderUtil.getFrameDelta());
            rowHoverAnimations.put(key, hover);
            int bg = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.82f * rowAlpha),
                    RenderUtil.withAlpha(MenuScreen.ACCENT, 0.14f * rowAlpha), selected ? 1f : hover * 0.42f);
            RenderUtil.drawRoundedRect(context, rowX + 3f * s, rowY, listW - 6f * s, rowH - 4f * s,
                    6f * s, bg);
            String title = CustomFontRenderer.trimToWidth(row.title, listW - 118f * s,
                    6.4f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, title, rowX + 10f * s, rowY + 6.2f * s,
                    6.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, rowAlpha), FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, row.subtitle, rowX + 10f * s, rowY + 17.4f * s,
                    5.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.94f * rowAlpha), FontWeight.SEMIBOLD);

            float deleteW = 17f * s;
            float loadW = 48f * s;
            float buttonY = rowY + 6f * s;
            float deleteX = rowX + listW - deleteW - 10f * s;
            float loadX = deleteX - loadW - 4f * s;
            drawInventoryRowButton(context, key + ":load", "Загрузить", loadX, buttonY, loadW,
                    17f * s, s, mouseX, mouseY, rowAlpha, false);
            drawInventoryRowButton(context, key + ":delete", "X", deleteX, buttonY, deleteW,
                    17f * s, s, mouseX, mouseY, rowAlpha, true);
            inventoryLoadBounds.put(row.id, new Bounds(loadX, buttonY, loadW, 17f * s));
            inventoryDeleteBounds.put(row.id, new Bounds(deleteX, buttonY, deleteW, 17f * s));
            rowY += rowH;
            index++;
        }
        context.draw();
        StencilUtil.disableScissor();
        if (rows.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, "Сохрани текущее расположение предметов",
                    leftX + leftW * 0.5f, rowsTop + 36f * s, 5.7f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.SEMIBOLD);
        }

        renderInventoryPreview(context, rightX, py, rightW, ph, s, alpha);
        InventoryLayoutManager manager = InventoryLayoutManager.getInstance();
        String noticeText = state.notice;
        boolean noticeGood = state.noticeGood;
        if (manager.statusAt() > state.noticeAt) {
            noticeText = manager.status();
            noticeGood = manager.statusGood();
        }
        if (!noticeText.isEmpty()) {
            int color = noticeGood ? MenuScreen.ACCENT_LIGHT : 0xFFFF7891;
            RenderUtil.drawRoundedRect(context, px + 8f * s, py + ph - 20f * s, pw - 16f * s, 14f * s,
                    4.5f * s, RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.94f * alpha));
            String notice = CustomFontRenderer.trimToWidth(noticeText, pw - 28f * s, 5.3f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawCenteredText(context, notice, px + pw * 0.5f, py + ph - 15.7f * s,
                    5.3f * s, RenderUtil.withAlpha(color, alpha), FontWeight.SEMIBOLD);
        }
    }

    private void renderInventoryPreview(DrawContext context, float x, float y, float w, float h, float s, float alpha) {
        LayoutView layout = InventoryLayoutManager.getInstance().find(state().selectedId);
        CustomFontRenderer.drawText(context, "Предпросмотр", x + 10f * s, y + 10f * s,
                7.2f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        if (layout == null || layout.stacks().size() < 41) {
            RenderUtil.drawRoundedOutline(context, x + 9f * s, y + 29f * s, w - 18f * s, 83f * s,
                    6f * s, 0.7f * s, RenderUtil.withAlpha(0xFFFFFF, 0.055f * alpha));
            CustomFontRenderer.drawCenteredText(context, "Выбери сохранение", x + w * 0.5f, y + 67f * s,
                    5.8f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.SEMIBOLD);
            return;
        }
        String name = CustomFontRenderer.trimToWidth(layout.name(), w - 65f * s, 5.8f * s, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, name, x + 48f * s, y + 10.8f * s,
                5.8f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);

        int[] missing = InventoryLayoutManager.getInstance().missingAmounts(layout);
        int missingSlots = 0;
        int missingItems = 0;
        for (int amount : missing) {
            if (amount > 0) missingSlots++;
            missingItems += amount;
        }
        boolean ready = missingItems == 0;
        float statusX = x + 9f * s;
        float statusY = y + 25f * s;
        float statusW = w - 18f * s;
        int statusColor = ready ? MenuScreen.ACCENT : 0xFFFF4058;
        RenderUtil.drawRoundedRect(context, statusX, statusY, statusW, 15f * s, 5f * s,
                RenderUtil.withAlpha(statusColor, ready ? 0.12f * alpha : 0.22f * alpha));
        RenderUtil.drawRoundedOutline(context, statusX, statusY, statusW, 15f * s, 5f * s, 0.7f * s,
                RenderUtil.withAlpha(statusColor, ready ? 0.30f * alpha : 0.74f * alpha));
        String status = ready ? "Все предметы на месте"
                : "Не хватает " + missingItems + " в " + missingSlots + " слотах";
        CustomFontRenderer.drawCenteredText(context, status, x + w * 0.5f, statusY + 4.7f * s,
                5.2f * s, RenderUtil.withAlpha(ready ? MenuScreen.ACCENT_LIGHT : 0xFFFFA1AE, alpha), FontWeight.SEMIBOLD);

        float slot = Math.min(15f * s, (w - 18f * s) / 9f);
        float gridW = slot * 9f;
        float gx = x + (w - gridW) * 0.5f;
        float top = y + 54f * s;
        CustomFontRenderer.drawText(context, "ЭКИПИРОВКА", gx, y + 45.5f * s,
                4.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.78f * alpha), FontWeight.SEMIBOLD);
        for (int i = 0; i < 4; i++) drawInventorySlot(context, layout.stacks().get(i), gx + i * slot,
                top, slot, s, alpha, missing.length > i ? missing[i] : 0);
        drawInventorySlot(context, layout.stacks().get(40), gx + 8f * slot, top, slot, s, alpha,
                missing.length > 40 ? missing[40] : 0);
        CustomFontRenderer.drawRightText(context, "OFFHAND", gx + gridW, y + 45.5f * s,
                4.2f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.68f * alpha), FontWeight.SEMIBOLD);

        float mainY = top + slot + 15f * s;
        CustomFontRenderer.drawText(context, "ИНВЕНТАРЬ", gx, mainY - 8.5f * s,
                4.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.78f * alpha), FontWeight.SEMIBOLD);
        for (int i = 0; i < 27; i++) {
            int stackIndex = 4 + i;
            drawInventorySlot(context, layout.stacks().get(stackIndex), gx + (i % 9) * slot,
                    mainY + (i / 9) * slot, slot, s, alpha,
                    missing.length > stackIndex ? missing[stackIndex] : 0);
        }
        float hotbarY = mainY + slot * 3f + 14f * s;
        CustomFontRenderer.drawText(context, "HOTBAR", gx, hotbarY - 8.5f * s,
                4.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.78f * alpha), FontWeight.SEMIBOLD);
        for (int i = 0; i < 9; i++) {
            int stackIndex = 31 + i;
            drawInventorySlot(context, layout.stacks().get(stackIndex), gx + i * slot, hotbarY, slot, s, alpha,
                    missing.length > stackIndex ? missing[stackIndex] : 0);
        }
        CustomFontRenderer.drawCenteredText(context, layout.itemCount() + " предметов", x + w * 0.5f,
                hotbarY + slot + 11f * s, 5.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
    }

    private void drawInventorySlot(DrawContext context, ItemStack stack, float x, float y, float size, float s,
                                   float alpha, int missingAmount) {
        boolean missing = missingAmount > 0;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 210.0);
        if (missing) RenderUtil.drawRoundedRect(context, x - 0.7f * s, y - 0.7f * s,
                size + 1.4f * s, size + 1.4f * s, 3.2f * s,
                RenderUtil.withAlpha(0xFFFF263F, (0.48f + pulse * 0.20f) * alpha));
        RenderUtil.drawRoundedRect(context, x + 0.6f * s, y + 0.6f * s, size - 1.2f * s, size - 1.2f * s,
                2.4f * s, RenderUtil.withAlpha(missing ? 0xFF271015 : MenuScreen.INSET_BG,
                        (missing ? 0.91f : 0.96f) * alpha));
        if (missing) {
            RenderUtil.drawRoundedOutline(context, x + 0.35f * s, y + 0.35f * s, size - 0.7f * s,
                    size - 0.7f * s, 2.7f * s, 1.15f * s,
                    RenderUtil.withAlpha(0xFFFF5268, (0.82f + pulse * 0.16f) * alpha));
        }
        if (stack == null || stack.isEmpty()) return;
        float itemScale = Math.max(0.45f, (size - 2f * s) / 16f);
        context.getMatrices().push();
        context.getMatrices().translate(x + s, y + s, 20f);
        context.getMatrices().scale(itemScale, itemScale, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        context.drawItem(stack, 0, 0);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, 0, 0);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.getMatrices().pop();
        if (missing) {
            RenderUtil.drawRoundedRect(context, x + 0.7f * s, y + 0.7f * s, size - 1.4f * s,
                    size - 1.4f * s, 2.2f * s, RenderUtil.withAlpha(0xFFFF1F3D, 0.16f * alpha));
        }
    }

    private void renderInventoryButton(DrawContext context, String id, String text, float x, float y, float w, float h,
                                       float s, int mouseX, int mouseY, boolean enabled, float alpha) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, w, h);
        String key = Type.INVENTORIES.name() + ":" + id;
        float hover = RenderUtil.animate(buttonHoverAnimations.getOrDefault(key, 0f), hovered ? 1f : 0f,
                0.28f, RenderUtil.getFrameDelta());
        buttonHoverAnimations.put(key, hover);
        RenderUtil.drawRoundedRect(context, x, y, w, h, 5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.ACCENT, enabled ? 0.24f * alpha : 0.08f * alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, 0.39f * alpha), hover));
        CustomFontRenderer.drawCenteredText(context, text, x + w * 0.5f, y + 5.9f * s,
                5.8f * s, RenderUtil.withAlpha(enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        state().buttons.add(new Button(id, new Bounds(x, y, w, h), enabled));
    }

    private void drawInventoryRowButton(DrawContext context, String key, String text, float x, float y, float w, float h,
                                        float s, int mouseX, int mouseY, float alpha, boolean danger) {
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        float hover = RenderUtil.animate(buttonHoverAnimations.getOrDefault(key, 0f), hovered ? 1f : 0f,
                0.3f, RenderUtil.getFrameDelta());
        buttonHoverAnimations.put(key, hover);
        int base = danger ? 0xFF51212B : MenuScreen.CHIP_BG;
        int over = danger ? 0xFF8C3345 : MenuScreen.ACCENT;
        RenderUtil.drawRoundedRect(context, x, y, w, h, 4.5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(base, 0.75f * alpha),
                        RenderUtil.withAlpha(over, danger ? 0.54f * alpha : 0.24f * alpha), hover));
        CustomFontRenderer.drawCenteredText(context, text, x + w * 0.5f, y + 5.4f * s,
                5.3f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
    }

    private void renderList(DrawContext context, float px, float py, float pw, float ph, float s,
                            int mouseX, int mouseY, float alpha) {
        listX = px;
        listY = py;
        listW = pw;
        listH = ph;
        RenderUtil.drawRoundedRect(context, px, py, pw, ph, 8f * s,
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.87f * alpha));

        float headerH = 30f * s;
        CustomFontRenderer.drawText(context, type.title(), px + 11f * s, py + 9.4f * s,
                7.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        List<Row> rows = rows();
        String status = status(rows.size());
        CustomFontRenderer.drawRightText(context, status, px + pw - 10f * s, py + 10.2f * s,
                5.9f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.92f * alpha), FontWeight.SEMIBOLD);
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
        int rowIndex = 0;
        for (Row row : rows) {
            if (rowY + rowH >= contentY - rowH && rowY <= contentY + contentH + rowH) {
                String animationKey = type.name() + ":" + row.id;
                long born = rowBornAt.computeIfAbsent(animationKey, ignored -> System.currentTimeMillis());
                float bornProgress = clamp((System.currentTimeMillis() - born - rowIndex * 28L) / 310f);
                float bornEase = RenderUtil.easeOutQuart(bornProgress);
                float rowAlpha = alpha * RenderUtil.easeOutCubic(bornProgress);
                float rowX = px - (1f - bornEase) * 18f * s;
                if (pendingDeleteType == type && row.id.equals(pendingDeleteRowId)) {
                    float remove = clamp((System.currentTimeMillis() - pendingDeleteAt) / 230f);
                    rowAlpha *= 1f - RenderUtil.easeInCubic(remove);
                    rowX -= RenderUtil.easeInCubic(remove) * 20f * s;
                }
                boolean selected = row.id.equals(state.selectedId);
                boolean hovered = hit(mouseX, mouseY, rowX + 7f * s, rowY, pw - 14f * s, rowH - 3f * s);
                float hover = RenderUtil.animate(rowHoverAnimations.getOrDefault(animationKey, 0f), hovered ? 1f : 0f,
                        0.29f, RenderUtil.getFrameDelta());
                rowHoverAnimations.put(animationKey, hover);
                if (selected || hover > 0.01f) {
                    RenderUtil.drawRoundedRect(context, rowX + 7f * s, rowY - hover * 0.5f * s,
                            pw - 14f * s, rowH - 3f * s + hover * s, 6f * s,
                            RenderUtil.lerpColor(RenderUtil.withAlpha(0xFFFFFF, rowAlpha * 0.038f * hover),
                                    RenderUtil.withAlpha(MenuScreen.ACCENT, rowAlpha * 0.15f), selected ? 1f : 0f));
                }
                RenderUtil.drawRoundedRect(context, rowX + 11f * s, rowY + 7f * s, 20f * s, 20f * s, 6f * s,
                        RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : MenuScreen.CHIP_BG,
                                rowAlpha * (selected ? 0.24f : 0.88f)));
                CustomFontRenderer.drawCenteredText(context, type.icon(), rowX + 21f * s, rowY + 11.5f * s,
                        11f * s, RenderUtil.withAlpha(selected ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM, rowAlpha),
                        type.iconWeight());
                String title = CustomFontRenderer.trimToWidth(row.title, pw - 54f * s, 6.9f * s, FontWeight.SEMIBOLD);
                String subtitle = CustomFontRenderer.trimToWidth(row.subtitle, pw - 54f * s, 5.8f * s, FontWeight.SEMIBOLD);
                CustomFontRenderer.drawText(context, title, rowX + 39f * s, rowY + 6.9f * s,
                        6.9f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, rowAlpha * (selected ? 1f : 0.93f)), FontWeight.SEMIBOLD);
                CustomFontRenderer.drawText(context, subtitle, rowX + 39f * s, rowY + 19f * s,
                        5.8f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.92f * rowAlpha), FontWeight.SEMIBOLD);
            }
            rowY += rowH;
            rowIndex++;
        }
        context.draw();
        StencilUtil.disableScissor();

        if (rows.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, emptyText(), px + pw * 0.5f, py + ph * 0.45f,
                    6.3f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.82f * alpha), FontWeight.SEMIBOLD);
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
        float cx = px + 10f * s;
        float cy = py + 10f * s;
        float cw = pw - 20f * s;
        PageState state = state();
        state.buttons.clear();
        editorElementIndex = 0;

        String heading = switch (type) {
            case CONFIGS -> ClientLanguage.text("ui.client_tools.configs.editor", "Конфиг");
            case FRIENDS -> ClientLanguage.text("ui.client_tools.friends.editor", "Друг");
            case GPS -> ClientLanguage.text("ui.client_tools.gps.editor", "Метка");
            case MACROS -> ClientLanguage.text("ui.macros.title", "Macros");
            case INVENTORIES -> "Менеджер инвентарей";
        };
        CustomFontRenderer.drawText(context, heading, cx, cy, 7.4f * s,
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
        } else if (type == Type.GPS) {
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
        } else {
            CustomFontRenderer.drawText(context, ClientLanguage.text("ui.macros.key", "Клавиша"), cx, cy,
                    5.9f * s, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.94f * alpha), FontWeight.SEMIBOLD);
            cy += 9f * s;
            cy = renderMacroKey(context, state, cx, cy, cw, s, mouseX, mouseY, alpha);
            cy += 4f * s;
            cy = renderInput(context, state.action, ClientLanguage.text("ui.macros.action", "Команда / сообщение"),
                    cx, cy, cw, s, alpha);
            cy += 5f * s;
            cy = renderButton(context, "macro_add", ClientLanguage.text("ui.macros.add", "Добавить"),
                    cx, cy, cw, s, mouseX, mouseY,
                    state.macroKey != BindKeyUtil.NONE && !state.action.text().isBlank(), alpha);
            renderButton(context, "macro_delete", ClientLanguage.text("ui.client_tools.delete", "Удалить"),
                    cx, cy, cw, s, mouseX, mouseY, selected() != null, alpha);
        }

        if (!state.notice.isEmpty()) {
            float noticeProgress = clamp((System.currentTimeMillis() - state.noticeAt) / 260f);
            float noticeAlpha = Math.min(1f, RenderUtil.easeOutCubic(noticeProgress));
            int color = state.noticeGood ? MenuScreen.ACCENT_LIGHT : 0xFFFF7891;
            String notice = CustomFontRenderer.trimToWidth(state.notice, pw - 20f * s, 5.4f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawCenteredText(context, notice, px + pw * 0.5f, py + ph - 14f * s,
                    5.4f * s, RenderUtil.withAlpha(color, alpha * noticeAlpha), FontWeight.SEMIBOLD);
        }
    }

    private float renderInput(DrawContext context, Input input, String label, float x, float y, float w, float s, float alpha) {
        float appear = nextEditorElement();
        x += (1f - RenderUtil.easeOutQuart(appear)) * 13f * s;
        alpha *= RenderUtil.easeOutCubic(appear);
        CustomFontRenderer.drawText(context, label, x, y, 6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.9f * alpha), FontWeight.SEMIBOLD);
        renderCompactInput(context, input, input.placeholder, x, y + 9f * s, w, s, alpha);
        return y + 31f * s;
    }

    private void renderCompactInput(DrawContext context, Input input, String placeholder,
                                    float x, float y, float w, float s, float alpha) {
        boolean focused = focusedInput == input;
        float focus = RenderUtil.animate(inputFocusAnimations.getOrDefault(input, 0f), focused ? 1f : 0f,
                0.30f, RenderUtil.getFrameDelta());
        inputFocusAnimations.put(input, focus);
        RenderUtil.drawRoundedRect(context, x, y, w, 19f * s, 5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, alpha * 0.84f),
                        RenderUtil.withAlpha(MenuScreen.CHIP_BG, alpha * 0.98f), focus));
        String value = input.value.isEmpty() ? placeholder : input.value;
        value = CustomFontRenderer.trimToWidth(value, w - 12f * s, 6.1f * s, FontWeight.SEMIBOLD);
        if (input.value.isEmpty()) {
            CustomFontRenderer.drawText(context, value, x + 6f * s, y + 6.1f * s, 6.1f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        } else {
            RenderUtil.drawAnimatedInputText(context, value, x + 6f * s, y + 6.1f * s, 6.1f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD,
                    input.typedIndex, input.typedAt);
        }
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caret = x + 6f * s + CustomFontRenderer.getTextWidth(input.value,
                    6.1f * s, FontWeight.SEMIBOLD) + 1f;
            RenderUtil.drawRect(context, Math.min(caret, x + w - 6f * s), y + 5f * s, 0.7f, 9f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
        input.bounds = new Bounds(x, y, w, 19f * s);
    }

    private float renderMacroKey(DrawContext context, PageState state, float x, float y, float w, float s,
                                 int mouseX, int mouseY, float alpha) {
        float appear = nextEditorElement();
        x += (1f - RenderUtil.easeOutQuart(appear)) * 13f * s;
        alpha *= RenderUtil.easeOutCubic(appear);
        float h = 19f * s;
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        float hover = RenderUtil.animate(buttonHoverAnimations.getOrDefault("macro-key", 0f), hovered ? 1f : 0f,
                0.28f, RenderUtil.getFrameDelta());
        buttonHoverAnimations.put("macro-key", hover);
        RenderUtil.drawRoundedRect(context, x, y - hover * 0.35f * s, w, h + hover * 0.7f * s, 5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, 0.88f * alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, 0.20f * alpha), Math.max(hover, macroBinding ? 1f : 0f)));
        String key = macroBinding ? "..." : BindKeyUtil.getBindName(state.macroKey);
        CustomFontRenderer.drawCenteredText(context, key, x + w * 0.5f, y + 6.2f * s,
                6.2f * s, RenderUtil.withAlpha(macroBinding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE, alpha),
                FontWeight.SEMIBOLD);
        macroKeyBounds = new Bounds(x, y, w, h);
        return y + 23f * s;
    }

    private float renderButton(DrawContext context, String id, String label, float x, float y, float w, float s,
                               int mouseX, int mouseY, boolean enabled, float alpha) {
        float appear = nextEditorElement();
        x += (1f - RenderUtil.easeOutQuart(appear)) * 13f * s;
        alpha *= RenderUtil.easeOutCubic(appear);
        float h = 21f * s;
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, w, h);
        String animationKey = type.name() + ":" + id;
        float hover = RenderUtil.animate(buttonHoverAnimations.getOrDefault(animationKey, 0f), hovered ? 1f : 0f,
                0.28f, RenderUtil.getFrameDelta());
        buttonHoverAnimations.put(animationKey, hover);
        long pressedAt = buttonPressAt.getOrDefault(animationKey, 0L);
        float pressProgress = pressedAt == 0L ? 1f : clamp((System.currentTimeMillis() - pressedAt) / 240f);
        float press = pressedAt == 0L ? 0f : (float) Math.sin(pressProgress * Math.PI) * (1f - pressProgress);
        int background = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.CHIP_BG, alpha * (enabled ? 0.88f : 0.42f)),
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.27f * alpha), hover);
        float inset = press * 1.2f * s;
        RenderUtil.drawRoundedRect(context, x + inset, y - hover * 0.45f * s, w - inset * 2f,
                h + hover * 0.9f * s, 5.5f * s, background);
        CustomFontRenderer.drawCenteredText(context, label, x + w * 0.5f, y + 6.8f * s - hover * 0.2f * s,
                (6.1f + hover * 0.18f) * s, RenderUtil.withAlpha(enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM,
                        alpha * (enabled ? 0.96f : 0.52f)), FontWeight.SEMIBOLD);
        state().buttons.add(new Button(id, new Bounds(x, y, w, h), enabled));
        return y + 25f * s;
    }

    private float nextEditorElement() {
        long delay = editorElementIndex++ * 34L;
        return clamp((System.currentTimeMillis() - pageChangedAt - 45L - delay) / 280f);
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (!hit(mouseX, mouseY, x, y, width, height)) return false;
        if (type == Type.MACROS && macroBinding) {
            state().macroKey = BindKeyUtil.mouseButtonToCode(button);
            macroBinding = false;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        PageState state = state();
        if (type == Type.INVENTORIES) {
            boolean inInventoryList = hit(mouseX, mouseY, listX, listY, listW, listH);
            if (inInventoryList) {
                for (Row row : visibleRows) {
                    Bounds load = inventoryLoadBounds.get(row.id);
                    if (load != null && load.contains(mouseX, mouseY)) {
                        state.selectedId = row.id;
                        perform("inventory_load");
                        EnoughSoundPlayer.playScroll();
                        return true;
                    }
                    Bounds delete = inventoryDeleteBounds.get(row.id);
                    if (delete != null && delete.contains(mouseX, mouseY)) {
                        state.selectedId = row.id;
                        startDelete("inventory_delete");
                        EnoughSoundPlayer.playScroll();
                        return true;
                    }
                }
                float inventoryRowY = listY + listScroll;
                for (Row row : visibleRows) {
                    if (hit(mouseX, mouseY, listX, inventoryRowY, listW, 29f * scale)) {
                        state.selectedId = row.id;
                        focusedInput = null;
                        EnoughSoundPlayer.playScroll();
                        return true;
                    }
                    inventoryRowY += 33f * scale;
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
                    buttonPressAt.put(type.name() + ":" + action.id, System.currentTimeMillis());
                    perform(action.id);
                    EnoughSoundPlayer.playScroll();
                    return true;
                }
            }
            focusedInput = null;
            return true;
        }
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
        if (type == Type.MACROS && macroKeyBounds != null && macroKeyBounds.contains(mouseX, mouseY)) {
            macroBinding = true;
            focusedInput = null;
            return true;
        }
        for (Button action : state.buttons) {
            if (action.enabled && action.bounds.contains(mouseX, mouseY)) {
                buttonPressAt.put(type.name() + ":" + action.id, System.currentTimeMillis());
                if (isDeleteAction(action.id) && selected() != null) startDelete(action.id);
                else perform(action.id);
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
        return true;
    }

    public boolean keyPressed(int key) {
        if (type == Type.MACROS && macroBinding) {
            state().macroKey = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE
                    || key == GLFW.GLFW_KEY_BACKSPACE ? BindKeyUtil.NONE : key;
            macroBinding = false;
            return true;
        }
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
            focusedInput.typedIndex = focusedInput.value.length() - 1;
            focusedInput.typedAt = System.currentTimeMillis();
        }
        return true;
    }

    public void clearFocus() {
        focusedInput = null;
        macroBinding = false;
    }

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
        } else if (type == Type.GPS) {
            for (WaypointView waypoint : GpsSystem.getInstance().listActiveWaypoints()) {
                String subtitle = String.format("%d %d %d - %d блоков",
                        (int) waypoint.x(), (int) waypoint.y(), (int) waypoint.z(), waypoint.distance());
                addIfMatches(rows, new Row(String.valueOf(waypoint.activeIndex()), waypoint.name(), subtitle), query);
            }
        } else if (type == Type.MACROS && Macros.INSTANCE != null) {
            List<Macros.MacroEntry> macros = Macros.INSTANCE.getMacros();
            for (int i = 0; i < macros.size(); i++) {
                Macros.MacroEntry macro = macros.get(i);
                addIfMatches(rows, new Row(String.valueOf(i), BindKeyUtil.getBindName(macro.key()), macro.action()), query);
            }
        } else if (type == Type.INVENTORIES) {
            for (LayoutView layout : InventoryLayoutManager.getInstance().list()) {
                addIfMatches(rows, new Row(layout.id(), layout.name(), layout.itemCount() + " предметов"), query);
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
            case MACROS -> String.valueOf(count);
            case INVENTORIES -> count + " сохранений";
        };
    }

    private String emptyText() {
        return switch (type) {
            case CONFIGS -> ClientLanguage.text("ui.client_tools.configs.empty", "Конфиги не найдены");
            case FRIENDS -> ClientLanguage.text("ui.client_tools.friends.empty", "Список друзей пуст");
            case GPS -> ClientLanguage.text("ui.client_tools.gps.empty", "Меток пока нет");
            case MACROS -> ClientLanguage.text("ui.macros.empty", "Макросов пока нет");
            case INVENTORIES -> "Сохранений пока нет";
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
        if (type == Type.INVENTORIES) return;
        if (type == Type.MACROS) {
            int index = parseInt(row.id, -1);
            List<Macros.MacroEntry> macros = Macros.INSTANCE == null ? List.of() : Macros.INSTANCE.getMacros();
            if (index >= 0 && index < macros.size()) {
                Macros.MacroEntry macro = macros.get(index);
                state.macroKey = macro.key();
                state.action.set(macro.action());
            }
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
                if (ok) {
                    rowBornAt.remove(Type.CONFIGS.name() + ":" + row.id);
                    state.selectedId = "";
                }
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
                    rowBornAt.remove(Type.FRIENDS.name() + ":" + row.id);
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
                if (ok) {
                    rowBornAt.remove(Type.GPS.name() + ":" + row.id);
                    state.selectedId = "";
                }
                notice(ok, "Метка удалена", "Не удалось удалить");
            }
            case "macro_add" -> {
                boolean ok = Macros.INSTANCE != null && state.macroKey != BindKeyUtil.NONE && !state.action.text().isBlank();
                if (ok) {
                    Macros.INSTANCE.addMacro(state.macroKey, state.action.text());
                    state.selectedId = String.valueOf(Macros.INSTANCE.getMacros().size() - 1);
                    state.action.set("");
                    state.macroKey = BindKeyUtil.NONE;
                }
                notice(ok, "Макрос добавлен", "Выбери клавишу и действие");
            }
            case "macro_delete" -> {
                Row row = selected();
                int index = row == null ? -1 : parseInt(row.id, -1);
                boolean ok = Macros.INSTANCE != null && index >= 0 && index < Macros.INSTANCE.getMacros().size();
                if (ok) {
                    rowBornAt.remove(Type.MACROS.name() + ":" + row.id);
                    Macros.INSTANCE.removeMacro(index);
                    state.selectedId = "";
                    state.action.set("");
                    state.macroKey = BindKeyUtil.NONE;
                }
                notice(ok, "Макрос удалён", "Не удалось удалить");
            }
            case "inventory_save" -> {
                InventoryLayoutManager.Result result = InventoryLayoutManager.getInstance().saveCurrent(state.name.text());
                if (result.success()) {
                    state.selectedId = result.id();
                    state.name.set("");
                }
                notice(result.success(), result.message(), result.message());
            }
            case "inventory_load" -> {
                Row row = selected();
                InventoryLayoutManager.Result result = InventoryLayoutManager.getInstance()
                        .startLoad(row == null ? "" : row.id);
                notice(result.success(), result.message(), result.message());
            }
            case "inventory_delete" -> {
                Row row = selected();
                InventoryLayoutManager.Result result = InventoryLayoutManager.getInstance()
                        .delete(row == null ? "" : row.id);
                if (result.success()) {
                    if (row != null) rowBornAt.remove(Type.INVENTORIES.name() + ":" + row.id);
                    state.selectedId = "";
                }
                notice(result.success(), result.message(), result.message());
            }
        }
    }

    private static boolean isDeleteAction(String id) {
        return id.equals("config_delete") || id.equals("friend_delete")
                || id.equals("gps_delete") || id.equals("macro_delete") || id.equals("inventory_delete");
    }

    private void startDelete(String action) {
        Row row = selected();
        if (row == null || pendingDeleteType != null) return;
        pendingDeleteAction = action;
        pendingDeleteRowId = row.id;
        pendingDeleteType = type;
        pendingDeleteAt = System.currentTimeMillis();
    }

    private void flushPendingDelete() {
        if (pendingDeleteType == null || System.currentTimeMillis() - pendingDeleteAt < 235L) return;
        if (pendingDeleteType == type) perform(pendingDeleteAction);
        pendingDeleteAction = "";
        pendingDeleteRowId = "";
        pendingDeleteType = null;
        pendingDeleteAt = 0L;
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
        if (type == Type.GPS) return List.of(state.name, state.x, state.y, state.z);
        if (type == Type.MACROS) return List.of(state.action);
        return List.of(state.name);
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
        private final Input action = new Input("/spawn или сообщение", 256);
        private int macroKey = BindKeyUtil.NONE;
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
        private int typedIndex = -1;
        private long typedAt;

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
