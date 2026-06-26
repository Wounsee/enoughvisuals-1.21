package fun.enoughvisuals.ui.clienttools;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class ClientToolScreen extends Screen {
    protected static final Identifier ICON_SEARCH = icon("search");
    protected static final Identifier ICON_PLUS = icon("plus");
    protected static final Identifier ICON_TRASH = icon("trash");
    protected static final Identifier ICON_SAVE = icon("save");
    protected static final Identifier ICON_LOAD = icon("load");
    protected static final Identifier ICON_FOLDER = icon("folder");
    protected static final Identifier ICON_PIN = icon("pin");
    protected static final Identifier ICON_CROSSHAIR = icon("crosshair");

    private static final float BASE_W = 430f;
    private static final float BASE_H = 255f;
    private static final float HEADER_H = 45f;
    private static final float ROW_H = 30f;
    private static final float RADIUS = 10f;
    private static final int PANEL_BG = 0xCC000000;
    private static final int HEADER_BG = 0x8C000000;
    private static final int LIST_BG = 0xA6000000;
    private static final int ACTION_BG = 0xA6000000;
    private static final int INPUT_BG = 0xD0060709;
    private static final int INPUT_FOCUSED = 0xD80C0E12;
    private static final int ROW_BG = 0x99000000;
    private static final int ROW_HOVER = 0xB00A0C10;
    private static final int ROW_SELECTED = 0xC00E1015;
    private static final int BUTTON_BG = 0xC00B0D11;
    private static final int BUTTON_HOVER = 0xD012151A;
    private static final int BUTTON_DISABLED = 0x90000000;
    private static final long OPEN_MS = 220L;
    private static final long CLOSE_MS = 170L;

    protected final TextBox searchBox = new TextBox(
            "search",
            "",
            ClientLanguage.text("ui.client_tools.search_placeholder", "Search...")
    );

    private final Map<String, Float> rowHoverAnims = new HashMap<>();
    private final Map<String, Float> buttonHoverAnims = new HashMap<>();
    private final Map<String, Float> buttonPressAnims = new HashMap<>();

    private long openStartMs;
    private long closeStartMs;
    private boolean closing;
    private float scrollOffset;
    private float scrollTarget;
    private float maxScroll;
    private TextBox focusedBox;
    protected String selectedId = "";
    private String notice = "";
    private boolean noticeGood = true;
    private long noticeStartMs = 0L;
    private Screen parentScreen;

    protected ClientToolScreen(String title) {
        super(Text.literal(title));
        MinecraftClient client = MinecraftClient.getInstance();
        parentScreen = client == null ? null : client.currentScreen;
    }

    private static Identifier icon(String name) {
        return Identifier.of("enough", "icons/client/" + name + ".png");
    }

    @Override
    protected void init() {
        super.init();
        MenuScreen.syncTheme();
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closing = false;
        scrollOffset = 0f;
        scrollTarget = 0f;
        maxScroll = 0f;
        focusedBox = null;
        searchBox.text = "";
        searchBox.cursor = 0;
        selectedId = "";
        notice = "";
        onOpen();
    }

    protected void onOpen() {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client == null || client.world == null) {
            RenderUtil.beginFrame();
        }
        MenuScreen.syncTheme();
        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (CustomTheme.clickGuiBlurEnabled()) {
            GlassUtil.beginFrame();
        } else {
            GlassUtil.skipFrame();
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        long now = System.currentTimeMillis();
        float progress = getProgress(now);
        if (closing && progress <= 0.001f) {
            if (client != null && client.currentScreen == this) {
                client.setScreen(parentScreen == this ? null : parentScreen);
            }
            RenderSystem.depthMask(true);
            return;
        }

        float ease = RenderUtil.easeInOutCubic(progress);
        float alpha = RenderUtil.easeOutQuad(progress);
        float scale = resolveScale();
        Layout layout = layout(scale, ease);

        List<ToolRow> rows = filteredRows();
        ensureSelection(rows);

        drawPanel(ctx, layout, rows, mouseX, mouseY, alpha, scale);
        RenderSystem.depthMask(true);
        StencilUtil.resetRenderState();
        MouseParticles.renderScreen(ctx, mouseX, mouseY, delta);
    }

    private float getProgress(long now) {
        if (closing) {
            float t = (now - closeStartMs) / (float) CLOSE_MS;
            return 1f - clamp(t);
        }
        return clamp((now - openStartMs) / (float) OPEN_MS);
    }

    private float resolveScale() {
        float byWidth = (width - 24f) / BASE_W;
        float byHeight = (height - 24f) / BASE_H;
        return Math.min(1f, Math.max(0.62f, Math.min(byWidth, byHeight)));
    }

    private Layout layout(float scale, float ease) {
        float w = BASE_W * scale;
        float h = BASE_H * scale;
        float x = (width - w) * 0.5f;
        float y = (height - h) * 0.5f + (1f - ease) * 10f * scale;
        float pad = 12f * scale;
        float headerH = HEADER_H * scale;
        float listX = x + pad;
        float listY = y + headerH + 8f * scale;
        float listW = 252f * scale;
        float listH = h - headerH - 20f * scale;
        float actionX = listX + listW + 12f * scale;
        float actionY = listY;
        float actionW = x + w - pad - actionX;
        float actionH = listH;
        return new Layout(x, y, w, h, pad, headerH, listX, listY, listW, listH, actionX, actionY, actionW, actionH, scale);
    }

    private void drawPanel(DrawContext ctx, Layout l, List<ToolRow> rows, int mx, int my, float alpha, float scale) {
        GlassUtil.drawGlassPanel(ctx, l.x, l.y, l.w, l.h, RADIUS * scale, 0x000000, alpha * 0.68f, alpha);
        RenderUtil.drawRoundedRect(ctx, l.x, l.y, l.w, l.h, RADIUS * scale, PANEL_BG);
        RenderUtil.drawRoundedRect(ctx, l.x, l.y, l.w, l.headerH, RADIUS * scale, RADIUS * scale, 0f, 0f, HEADER_BG);
        drawHeader(ctx, l, rows.size(), mx, my, alpha);
        drawList(ctx, l, rows, mx, my, alpha);
        drawActionPanel(ctx, l, mx, my, alpha);
    }

    private void drawHeader(DrawContext ctx, Layout l, int count, int mx, int my, float alpha) {
        float iconSize = 20f * l.scale;
        float iconX = l.x + l.pad;
        float iconY = l.y + 12f * l.scale;
        Draw.imageTinted(ctx, icon(), iconX, iconY, iconSize, iconSize, MenuScreen.ACCENT_LIGHT, alpha);

        float titleX = iconX + iconSize + 8f * l.scale;
        CustomFontRenderer.drawText(ctx, title(), titleX, l.y + 9f * l.scale, 10.5f * l.scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);

        String detail = activeNotice();
        if (detail == null || detail.isBlank()) {
            detail = statusText(count);
        }
        int detailColor = activeNotice() == null
                ? MenuScreen.TEXT_DIM
                : (noticeGood ? MenuScreen.ACCENT_LIGHT : 0xFFFF6F87);
        CustomFontRenderer.drawText(ctx, detail, titleX, l.y + 25f * l.scale, 6.3f * l.scale,
                RenderUtil.withAlpha(detailColor, alpha * 0.92f), FontWeight.REGULAR);

        float searchW = 145f * l.scale;
        float searchH = 17f * l.scale;
        float searchX = l.x + l.w - l.pad - searchW;
        float searchY = l.y + 14f * l.scale;
        drawTextBox(ctx, searchBox, searchX, searchY, searchW, searchH, alpha, mx, my, false);
        Draw.imageTinted(ctx, ICON_SEARCH, searchX + 5f * l.scale, searchY + 4.2f * l.scale,
                8.5f * l.scale, 8.5f * l.scale, MenuScreen.TEXT_DIM, alpha * 0.82f);

        RenderUtil.drawRect(ctx, l.x + l.pad, l.y + l.headerH - 0.5f * l.scale,
                l.w - l.pad * 2f, 0.5f * l.scale, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.07f));
    }

    private String activeNotice() {
        if (notice.isBlank() || System.currentTimeMillis() - noticeStartMs > 1600L) {
            return null;
        }
        return notice;
    }

    private void drawList(DrawContext ctx, Layout l, List<ToolRow> rows, int mx, int my, float alpha) {
        RenderUtil.drawRoundedRect(ctx, l.listX, l.listY, l.listW, l.listH, 8f * l.scale, LIST_BG);

        float frameDelta = RenderUtil.getFrameDelta();
        maxScroll = Math.max(0f, rows.size() * ROW_H * l.scale - (l.listH - 8f * l.scale));
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
        scrollOffset = RenderUtil.animate(scrollOffset, scrollTarget, 0.32f, frameDelta);

        if (rows.isEmpty()) {
            drawEmptyState(ctx, l, alpha);
            return;
        }

        ctx.draw();
        StencilUtil.enableScissor(l.listX, l.listY + 4f * l.scale, l.listW, l.listH - 8f * l.scale);
        float y = l.listY + 4f * l.scale + scrollOffset;
        for (ToolRow row : rows) {
            if (y + ROW_H * l.scale >= l.listY && y <= l.listY + l.listH) {
                drawRow(ctx, l, row, l.listX + 4f * l.scale, y, l.listW - 8f * l.scale, ROW_H * l.scale, mx, my, alpha);
            }
            y += ROW_H * l.scale;
        }
        ctx.draw();
        StencilUtil.disableScissor();
    }

    private void drawEmptyState(DrawContext ctx, Layout l, float alpha) {
        String text = emptyText();
        float textW = CustomFontRenderer.getTextWidth(text, 7.2f * l.scale, FontWeight.REGULAR);
        CustomFontRenderer.drawText(ctx, text, l.listX + (l.listW - textW) * 0.5f,
                l.listY + l.listH * 0.5f - 4f * l.scale, 7.2f * l.scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.82f), FontWeight.REGULAR);
    }

    private void drawRow(DrawContext ctx, Layout l, ToolRow row, float x, float y, float w, float h, int mx, int my, float alpha) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean selected = row.id().equals(selectedId);
        float frameDelta = RenderUtil.getFrameDelta();
        float hover = RenderUtil.animate(rowHoverAnims.getOrDefault(row.id(), 0f), hovered ? 1f : 0f, 0.45f, frameDelta);
        rowHoverAnims.put(row.id(), hover);

        int bg = selected ? ROW_SELECTED : (hover > 0.04f ? ROW_HOVER : ROW_BG);
        RenderUtil.drawRoundedRect(ctx, x, y + 2f * l.scale, w, h - 4f * l.scale, 6f * l.scale, bg);

        drawRowIcon(ctx, l, row, x, y, selected, hover, alpha);

        float textX = x + rowTextOffset(l, row);
        float textWidth = w - rowTextOffset(l, row) - 9f * l.scale;
        String title = CustomFontRenderer.trimToWidth(row.title(), textWidth, 7.1f * l.scale, FontWeight.SEMIBOLD);
        String subtitle = CustomFontRenderer.trimToWidth(row.subtitle(), textWidth, 5.7f * l.scale, FontWeight.REGULAR);
        CustomFontRenderer.drawText(ctx, title, textX, y + 6.4f * l.scale, 7.1f * l.scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * (selected ? 1f : 0.9f)), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, subtitle, textX, y + 17.1f * l.scale, 5.7f * l.scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.88f), FontWeight.REGULAR);
    }

    protected void drawRowIcon(DrawContext ctx, Layout l, ToolRow row, float x, float y,
                               boolean selected, float hover, float alpha) {
        Identifier rowIcon = row.icon() == null ? icon() : row.icon();
        int iconColor = selected ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_DIM;
        Draw.imageTinted(ctx, rowIcon, x + 7f * l.scale, y + 8f * l.scale, 13f * l.scale, 13f * l.scale,
                iconColor, alpha * (selected ? 1f : 0.78f + hover * 0.18f));
    }

    protected float rowTextOffset(Layout l, ToolRow row) {
        return 26f * l.scale;
    }

    private void drawActionPanel(DrawContext ctx, Layout l, int mx, int my, float alpha) {
        RenderUtil.drawRoundedRect(ctx, l.actionX, l.actionY, l.actionW, l.actionH, 8f * l.scale, ACTION_BG);
        renderActions(ctx, l, mx, my, alpha);
    }

    protected void drawTextBox(DrawContext ctx, TextBox box, float x, float y, float w, float h,
                               float alpha, int mx, int my, boolean label) {
        if (label && !box.label.isBlank()) {
            CustomFontRenderer.drawText(ctx, box.label, x, y, 6f * currentScale(), RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.REGULAR);
            y += 9f * currentScale();
        }

        boolean focused = focusedBox == box;
        int bg = focused ? INPUT_FOCUSED : INPUT_BG;
        float radius = 5f * currentScale();
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius, bg);

        String display = box.text.isEmpty() ? box.placeholder : box.text;
        int color = box.text.isEmpty() ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE;
        float leftPad = box == searchBox ? 17f * currentScale() : 6f * currentScale();
        float textSize = 6.3f * currentScale();
        float textY = y + (h - textSize) * 0.5f - 0.55f * currentScale();
        String trimmed = CustomFontRenderer.trimToWidth(display, w - leftPad - 5f * currentScale(), textSize, FontWeight.REGULAR);
        RenderUtil.drawAnimatedInputText(ctx, trimmed, x + leftPad, textY,
                textSize, RenderUtil.withAlpha(color, alpha * (box.text.isEmpty() ? 0.82f : 0.98f)), FontWeight.REGULAR,
                box.typedIndex, box.typedStartMs);

        if (focused && (System.currentTimeMillis() / 520L) % 2L == 0) {
            int cursor = Math.max(0, Math.min(box.cursor, box.text.length()));
            float cursorX = x + leftPad + CustomFontRenderer.getTextWidth(box.text.substring(0, cursor), textSize, FontWeight.REGULAR);
            cursorX = Math.min(cursorX, x + w - 6f * currentScale());
            RenderUtil.drawRect(ctx, cursorX, y + 4f * currentScale(), 0.6f * currentScale(), h - 8f * currentScale(),
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.86f));
        }
    }

    protected boolean drawButton(DrawContext ctx, String id, String label, Identifier icon,
                                 float x, float y, float w, float h, boolean enabled,
                                 int mx, int my, float alpha) {
        boolean hovered = enabled && mx >= x && mx <= x + w && my >= y && my <= y + h;
        float frameDelta = RenderUtil.getFrameDelta();
        float hover = RenderUtil.animate(buttonHoverAnims.getOrDefault(id, 0f), hovered ? 1f : 0f, 0.45f, frameDelta);
        float press = RenderUtil.animate(buttonPressAnims.getOrDefault(id, 0f), 0f, 0.55f, frameDelta);
        buttonHoverAnims.put(id, hover);
        buttonPressAnims.put(id, press);

        float scale = 1f - press * 0.035f;
        float bx = x + w * (1f - scale) * 0.5f;
        float by = y + h * (1f - scale) * 0.5f;
        float bw = w * scale;
        float bh = h * scale;
        int bg = enabled ? (hover > 0.04f ? BUTTON_HOVER : BUTTON_BG) : BUTTON_DISABLED;
        RenderUtil.drawRoundedRect(ctx, bx, by, bw, bh, 5f * currentScale(), bg);
        float textSize = 6.2f * currentScale();
        float textY = by + (bh - textSize) * 0.5f - 0.55f * currentScale();
        if (icon != null) {
            Draw.imageTinted(ctx, icon, bx + 6f * currentScale(), by + (bh - 8.5f * currentScale()) * 0.5f,
                    8.5f * currentScale(), 8.5f * currentScale(),
                    enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha * (enabled ? 0.92f : 0.45f));
        }
        float textX = icon == null ? bx + bw * 0.5f : bx + 18f * currentScale();
        String trimmed = CustomFontRenderer.trimToWidth(label, icon == null ? bw - 10f * currentScale() : bw - 23f * currentScale(),
                textSize, FontWeight.SEMIBOLD);
        if (icon == null) {
            CustomFontRenderer.drawCenteredText(ctx, trimmed, textX, textY,
                    textSize, RenderUtil.withAlpha(enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        } else {
            CustomFontRenderer.drawText(ctx, trimmed, textX, textY,
                    textSize, RenderUtil.withAlpha(enabled ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        }
        return hovered;
    }

    protected boolean clickButton(String id, float mx, float my, float x, float y, float w, float h, boolean enabled) {
        if (!enabled || mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        buttonPressAnims.put(id, 1f);
        EnoughSoundPlayer.playScroll();
        return true;
    }

    protected boolean clickTextBox(TextBox box, float mx, float my, float x, float y, float w, float h) {
        if (mx < x || mx > x + w || my < y || my > y + h) {
            return false;
        }
        focusedBox = box;
        box.cursor = box.text.length();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || closing) {
            return true;
        }
        Layout l = layout(resolveScale(), 1f);
        float mx = (float) mouseX;
        float my = (float) mouseY;

        float searchW = 145f * l.scale;
        float searchH = 17f * l.scale;
        float searchX = l.x + l.w - l.pad - searchW;
        float searchY = l.y + 14f * l.scale;
        if (clickTextBox(searchBox, mx, my, searchX, searchY, searchW, searchH)) {
            return true;
        }

        List<ToolRow> rows = filteredRows();
        if (mx >= l.listX && mx <= l.listX + l.listW && my >= l.listY && my <= l.listY + l.listH) {
            float y = l.listY + 4f * l.scale + scrollOffset;
            for (ToolRow row : rows) {
                if (my >= y && my <= y + ROW_H * l.scale) {
                    selectedId = row.id();
                    onRowSelected(row);
                    focusedBox = null;
                    EnoughSoundPlayer.playScroll();
                    return true;
                }
                y += ROW_H * l.scale;
            }
        }

        if (handleActionClick(mx, my, l)) {
            return true;
        }
        focusedBox = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout(resolveScale(), 1f);
        if (mouseX >= l.listX && mouseX <= l.listX + l.listW && mouseY >= l.listY && mouseY <= l.listY + l.listH) {
            scrollTarget += (float) (verticalAmount * ROW_H * 0.85f * l.scale);
            scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
            EnoughSoundPlayer.playScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            startClosing();
            return true;
        }
        if (focusedBox != null) {
            return focusedBox.keyPressed(keyCode);
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focusedBox != null && !Character.isISOControl(chr)) {
            focusedBox.insert(chr);
            return true;
        }
        return true;
    }

    protected void startClosing() {
        if (!closing) {
            closing = true;
            closeStartMs = System.currentTimeMillis();
            focusedBox = null;
        }
    }

    protected void notifyStatus(String text, boolean good) {
        notice = text == null ? "" : text;
        noticeGood = good;
        noticeStartMs = System.currentTimeMillis();
        if (!good) {
            EnoughSoundPlayer.playWarn();
        }
    }

    protected float currentScale() {
        return resolveScale();
    }

    protected List<ToolRow> filteredRows() {
        String query = searchBox.text.trim().toLowerCase(Locale.ROOT);
        List<ToolRow> rows = new ArrayList<>();
        for (ToolRow row : rows()) {
            if (query.isEmpty()
                    || row.title().toLowerCase(Locale.ROOT).contains(query)
                    || row.subtitle().toLowerCase(Locale.ROOT).contains(query)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private void ensureSelection(List<ToolRow> rows) {
        if (rows.stream().noneMatch(row -> row.id().equals(selectedId))) {
            selectedId = rows.isEmpty() ? "" : rows.get(0).id();
        }
    }

    protected ToolRow selectedRow() {
        return filteredRows().stream().filter(row -> row.id().equals(selectedId)).findFirst().orElse(null);
    }

    protected abstract Identifier icon();
    protected abstract String title();
    protected abstract String statusText(int visibleRows);
    protected abstract String emptyText();
    protected abstract List<ToolRow> rows();
    protected abstract void renderActions(DrawContext ctx, Layout layout, int mx, int my, float alpha);
    protected abstract boolean handleActionClick(float mx, float my, Layout layout);

    protected void onRowSelected(ToolRow row) {
    }

    protected static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    protected static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected record ToolRow(String id, String title, String subtitle, Identifier icon) {
    }

    protected record Layout(float x, float y, float w, float h, float pad, float headerH,
                            float listX, float listY, float listW, float listH,
                            float actionX, float actionY, float actionW, float actionH,
                            float scale) {
    }

    protected static final class TextBox {
        private final String id;
        private final String label;
        private final String placeholder;
        protected String text = "";
        private int cursor = 0;
        private int typedIndex = -1;
        private long typedStartMs = 0L;

        public TextBox(String id, String label, String placeholder) {
            this.id = id;
            this.label = label == null ? "" : label;
            this.placeholder = placeholder == null ? "" : placeholder;
        }

        public String id() {
            return id;
        }

        public String text() {
            return text.trim();
        }

        public void setText(String text) {
            this.text = text == null ? "" : text;
            this.cursor = this.text.length();
        }

        private void insert(char chr) {
            cursor = Math.max(0, Math.min(cursor, text.length()));
            text = text.substring(0, cursor) + chr + text.substring(cursor);
            typedIndex = cursor;
            typedStartMs = System.currentTimeMillis();
            cursor++;
        }

        private boolean keyPressed(int keyCode) {
            cursor = Math.max(0, Math.min(cursor, text.length()));
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (cursor > 0) {
                        text = text.substring(0, cursor - 1) + text.substring(cursor);
                        cursor--;
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (cursor < text.length()) {
                        text = text.substring(0, cursor) + text.substring(cursor + 1);
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    cursor = Math.max(0, cursor - 1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    cursor = Math.min(text.length(), cursor + 1);
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    cursor = 0;
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    cursor = text.length();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    return true;
                }
                default -> {
                    return true;
                }
            }
        }
    }
}
