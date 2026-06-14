package fun.enoughvisuals.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.client.GuiSize;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import fun.enoughvisuals.ui.clickgui.components.MenuModule;
import fun.enoughvisuals.ui.clickgui.components.MenuSettings;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class MenuScreen extends Screen {

    public static final int DEFAULT_ACCENT = 0xFF7D5ECB;
    public static int ACCENT       = DEFAULT_ACCENT;
    public static int ACCENT_LIGHT = 0xFFB49BE8;
    public static final int TEXT_WHITE   = 0xFFEFF4F7;
    public static final int TEXT_GRAY    = 0xFFBEC7CD;
    public static final int TEXT_DIM     = 0xFF879399;
    public static final int TEXT_DARK    = 0xFF555566;
    public static final int SEPARATOR    = 0x20FFFFFF;
    public static int TOGGLE_ON    = ACCENT;
    public static int TOGGLE_OFF   = 0xFF1B1C21;
    public static final int TOGGLE_KNOB  = 0xFFFFFFFF;
    public static int SETTING_BG   = 0x16000000;
    public static int SLIDER_BG    = 0xFF2A2A3A;
    public static int SLIDER_FILL  = ACCENT;
    public static final int CHECKBOX_BG  = 0xFF2A2A3A;
    public static final int MODULE_HOVER = 0xFFFFFF;
    public static int SETTING_PANEL_OUTER = 0xFF090A0C;
    public static int SETTING_PANEL_INNER = 0xFF111319;
    public static int SETTING_INPUT_BG = 0xFF000000;
    public static int PANEL_TINT = 0x000000;
    public static float PANEL_TINT_ALPHA = 0.80f;
    public static int HEADER_TINT = 0x000000;
    public static float HEADER_TINT_ALPHA = 0.20f;
    public static int SEARCH_TINT = 0x000000;
    public static float SEARCH_TINT_ALPHA = 0.82f;

    private static final Map<Category, Identifier> CATEGORY_ICONS = new HashMap<>();

    public static final Identifier MODULE_ON_ICON =
            Identifier.of("enough", "icons/category/moduleon.png");
    public static final Identifier MOD_ARROW_ICON =
            Identifier.of("enough", "icons/category/modarrow.png");
    public static final Identifier BACK_ICON =
            Identifier.of("enough", "icons/category/back.png");

    public static final int CAT_ICON_TEX = 43;
    public static final int MOD_ICON_TEX = 43;

    private static final float PANEL_W   = 115;
    private static final float PANEL_H   = 240;
    private static final float PANEL_GAP = 10;
    private static final float PANEL_R = 10f;
    private static final float HEADER_H  = 24;
    private static final float MODULE_H  = 20;
    private static final long PANEL_OPEN_MS = 420L;
    private static final long PANEL_CLOSE_MS = 360L;
    private static final long PANEL_STAGGER_MS = 100L;
    private static final float SEARCH_W = 220f;
    private static final float SEARCH_ROW_H = 14f;
    private static final int SEARCH_MAX_RESULTS = 40;
    private static final int SEARCH_VISIBLE_ROWS = 10;

    private final List<PanelData> panels = new ArrayList<>();
    private final List<SearchResult> searchResults = new ArrayList<>();
    private long openTime;
    private boolean closing = false;
    private long closeStart = 0L;
    private Module bindingModule = null;
    private PanelData bindingPanel = null;

    private String hoveredDescription = null;
    private boolean searchActive = false;
    private String searchQuery = "";
    private int searchSelection = 0;
    private float searchAnim = 0f;
    private float searchHeightAnim = 0f;
    private float searchScrollOffset = 0f;
    private float searchScrollTarget = 0f;

    private int typedCharIndex = -1;
    private long typedCharStartMs = 0L;
    private String removedChar = "";
    private long removedCharStartMs = 0L;
    private float currentGuiScale = 1f;
    private float fixedScreenScale = 1f;
    private float virtualScreenWidth = 0f;
    private float virtualScreenHeight = 0f;

    public MenuScreen() {
        super(Text.literal("ClickGUI"));
    }

    public static void syncTheme() {
        CustomTheme theme = ModuleManager.getInstance().get(CustomTheme.class);
        boolean menuColor = theme != null && theme.isEnabled() && theme.menu.get();
        int accent = menuColor ? theme.guiAccentColor() : DEFAULT_ACCENT;
        float guiOpacity = CustomTheme.clickGuiOpacity();
        ACCENT = accent;
        ACCENT_LIGHT = lighten(accent, 0.35f);
        TOGGLE_ON = accent;
        SLIDER_FILL = accent;
        SETTING_BG = menuColor ? darken(accent, 0.74f) : 0x16000000;
        SLIDER_BG = menuColor ? darken(accent, 0.60f) : 0xFF2A2A3A;
        TOGGLE_OFF = menuColor ? darken(accent, 0.68f) : 0xFF1B1C21;
        if (menuColor) {
            SETTING_PANEL_OUTER = darken(accent, 0.88f);
            SETTING_PANEL_INNER = darken(accent, 0.78f);
            SETTING_INPUT_BG = darken(accent, 0.82f);
            PANEL_TINT = accent;
            PANEL_TINT_ALPHA = 0.80f * guiOpacity;
            HEADER_TINT = accent;
            HEADER_TINT_ALPHA = 0.20f * guiOpacity;
            SEARCH_TINT = accent;
            SEARCH_TINT_ALPHA = 0.82f * guiOpacity;
        } else {
            SETTING_PANEL_OUTER = 0xFF090A0C;
            SETTING_PANEL_INNER = 0xFF111319;
            SETTING_INPUT_BG = 0xFF000000;
            PANEL_TINT = 0x000000;
            PANEL_TINT_ALPHA = 0.80f * guiOpacity;
            HEADER_TINT = 0x000000;
            HEADER_TINT_ALPHA = 0.20f * guiOpacity;
            SEARCH_TINT = 0x000000;
            SEARCH_TINT_ALPHA = 0.82f * guiOpacity;
        }
    }

    private static int lighten(int color, float amount) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += Math.round((255 - r) * amount);
        g += Math.round((255 - g) * amount);
        b += Math.round((255 - b) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, float amount) {
        int r = Math.round(((color >> 16) & 0xFF) * (1f - amount));
        int g = Math.round(((color >> 8) & 0xFF) * (1f - amount));
        int b = Math.round((color & 0xFF) * (1f - amount));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        closing = false;
        closeStart = 0L;
        searchActive = false;
        searchQuery = "";
        searchSelection = 0;
        searchAnim = 0f;
        searchHeightAnim = 0f;
        searchScrollOffset = 0f;
        searchScrollTarget = 0f;
        typedCharIndex = -1;
        typedCharStartMs = 0L;
        removedChar = "";
        removedCharStartMs = 0L;
        searchResults.clear();
        bindingModule = null;
        bindingPanel = null;

        panels.clear();
        ModuleManager mgr = ModuleManager.getInstance();

        for (Category cat : Category.values()) {
            List<Module> mods = mgr.getModulesByCategory(cat);
            mods.sort(Comparator.comparing(Module::getName));

            PanelData p = new PanelData(cat);
            for (Module m : mods) {
                p.modules.add(new MenuModule(m, this, p));
            }
            panels.add(p);
        }

        layoutPanels();
    }

    private void layoutPanels() {
        updateFixedScreenScale();
        float guiScale = resolveGuiScale();
        float panelW = PANEL_W * guiScale;
        float panelH = PANEL_H * guiScale;
        float gap = PANEL_GAP * guiScale;
        float total = panels.size() * (panelW + gap) - gap;
        float sx = (screenWidth() - total) / 2f;
        float sy = (screenHeight() - panelH) / 2f;

        for (int i = 0; i < panels.size(); i++) {
            panels.get(i).x = sx + i * (panelW + gap);
            panels.get(i).y = sy;
        }
    }

    private float resolveGuiScale() {
        GuiSize guiSize = ModuleManager.getInstance().get(GuiSize.class);
        if (guiSize == null || !guiSize.isEnabled()) {
            currentGuiScale = 1f;
            return currentGuiScale;
        }

        float value = guiSize.percent();
        float minScale = 0.35f;
        float maxScale = getMaxGuiScale();
        if (value <= 50f) {
            currentGuiScale = minScale + (1f - minScale) * ((value - 1f) / 49f);
        } else {
            currentGuiScale = 1f + (maxScale - 1f) * ((value - 50f) / 50f);
        }
        return currentGuiScale;
    }

    private float getMaxGuiScale() {
        float totalW = Math.max(PANEL_W, panels.size() * (PANEL_W + PANEL_GAP) - PANEL_GAP);
        float maxByWidth = Math.max(1f, (screenWidth() - 24f) / totalW);
        float maxByHeight = Math.max(1f, (screenHeight() - 28f) / PANEL_H);
        return Math.min(maxByWidth, maxByHeight);
    }

    private void updateFixedScreenScale() {
        fixedScreenScale = ScreenScaleUtil.scale();
        virtualScreenWidth = ScreenScaleUtil.virtualWidth();
        virtualScreenHeight = ScreenScaleUtil.virtualHeight();
        if (virtualScreenWidth <= 0.0f) virtualScreenWidth = width;
        if (virtualScreenHeight <= 0.0f) virtualScreenHeight = height;
    }

    private float screenWidth() {
        return virtualScreenWidth > 0.0f ? virtualScreenWidth : width;
    }

    private float screenHeight() {
        return virtualScreenHeight > 0.0f ? virtualScreenHeight : height;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {}

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (bindingModule != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
                bindingModule.setKeyBind(BindKeyUtil.NONE);
            } else {
                bindingModule.setKeyBind(key);
            }
            finishBinding();
            return true;
        }

        if (hasControlDown() && key == GLFW.GLFW_KEY_F) {
            openSearch();
            EnoughSoundPlayer.playScroll();
            return true;
        }

        if (searchActive) {
            if (handleSearchKey(key)) {
                return true;
            }
        }

        if (key == 256) {
            if (!closing) startClosing();
            return true;
        }
        if (!canInteract()) return true;

        for (PanelData p : panels) {
            if (p.settingsView != null && p.settingsView.keyPressed(key, scan)) return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char chr, int mod) {
        if (searchActive) {
            if (handleSearchChar(chr)) {
                return true;
            }
        }

        if (!canInteract()) return true;
        for (PanelData p : panels) {
            if (p.settingsView != null && p.settingsView.charTyped(chr, mod)) return true;
        }
        return super.charTyped(chr, mod);
    }

    private void startClosing() {
        closing = true;
        closeStart = System.currentTimeMillis();
        finishBinding();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (client == null || client.world == null) {
            RenderUtil.beginFrame();
        }
        StencilUtil.resetRenderState();
        syncTheme();
        updateFixedScreenScale();
        int virtualMx = Math.round(ScreenScaleUtil.screenToVirtual(mx));
        int virtualMy = Math.round(ScreenScaleUtil.screenToVirtual(my));
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
        layoutPanels();

        long now = System.currentTimeMillis();
        if (closing && isCloseFinished(now)) {
            if (client != null && client.currentScreen == this) {
                client.setScreen(null);
            }
            RenderSystem.depthMask(true);
            StencilUtil.resetRenderState();
            return;
        }

        float globalAlpha = 1f;
        if (!closing) {
            float progress = Math.max(0f, Math.min(1f, (now - openTime) / (float) PANEL_OPEN_MS));
            globalAlpha = RenderUtil.easeOutQuad(progress);
        } else {
            float closeProgress = Math.max(0f, Math.min(1f, (now - closeStart) / (float) PANEL_CLOSE_MS));
            globalAlpha = Math.max(0f, Math.min(1f, 1f - RenderUtil.easeInOutCubic(closeProgress)));
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        float frameDelta = RenderUtil.getFrameDelta();
        searchAnim = RenderUtil.animate(searchAnim, searchActive ? 1f : 0f, 0.32f, frameDelta);

        float targetHeight = 30f + Math.min(searchResults.size(), SEARCH_VISIBLE_ROWS) * SEARCH_ROW_H;
        if (!searchActive) targetHeight = 0f;
        searchHeightAnim = RenderUtil.animate(searchHeightAnim, targetHeight, 0.26f, frameDelta);
        searchScrollOffset = RenderUtil.animate(searchScrollOffset, searchScrollTarget, 0.32f, frameDelta);

        hoveredDescription = null;

        ctx.getMatrices().push();
        ctx.getMatrices().scale(fixedScreenScale, fixedScreenScale, 1f);
        float previousScissorScale = HudElement.setActiveScissorScale(fixedScreenScale);
        try {
            for (int i = 0; i < panels.size(); i++) {
                float panelAlpha;
                float panelScale;
                float panelYOffset;

                if (!closing) {
                    float panelProgress = getOpenProgress(now, i);
                    float panelCurve = RenderUtil.easeOutBack(panelProgress);
                    panelAlpha = RenderUtil.easeOutQuad(panelProgress);
                    panelScale = 2f - panelCurve;
                    panelYOffset = (1f - panelCurve) * 18f;
                } else {
                    float panelClose = getCloseProgress(now, i);
                    float closeCurve = RenderUtil.easeInOutCubic(panelClose);
                    float closeClamped = Math.max(0f, Math.min(1f, closeCurve));
                    panelAlpha = 1f - closeClamped;
                    panelScale = 1f + closeClamped;
                    panelYOffset = closeClamped * 8f;
                }

                renderPanel(ctx, panels.get(i), virtualMx, virtualMy, delta, panelAlpha, panelYOffset, panelScale, currentGuiScale);
            }

            renderSearchHint(ctx, globalAlpha);
            if (!closing) {
                renderDescription(ctx, delta);
                renderSearchOverlay(ctx, virtualMx, virtualMy);
                renderBindHint(ctx);
            }
        } finally {
            HudElement.setActiveScissorScale(previousScissorScale);
            ctx.getMatrices().pop();
        }
        RenderSystem.depthMask(true);
        StencilUtil.resetRenderState();
        MouseParticles.renderScreen(ctx, mx, my, delta);
    }

    private void renderUpdateLock(DrawContext ctx) {
        float boxW = Math.min(310f, screenWidth() - 28f);
        float boxH = 58f;
        float x = screenWidth() * 0.5f - boxW * 0.5f;
        float y = Math.max(18f, (panels.isEmpty() ? screenHeight() * 0.5f : panels.get(0).y) - 74f);

        GlassUtil.drawBlurPanel(ctx, x, y, boxW, boxH, 9f, 0.92f);
        RenderUtil.drawRoundedRect(ctx, x, y, boxW, boxH, 9f, RenderUtil.withAlpha(0x000000, 0.78f));
        CustomFontRenderer.drawCenteredText(ctx,
                "Обновите клиент",
                screenWidth() * 0.5f, y + 14f, 10f, TEXT_WHITE, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawCenteredText(ctx,
                "Критическое обновление. Настройки отключены.",
                screenWidth() * 0.5f, y + 34f, 6.6f, RenderUtil.withAlpha(TEXT_GRAY, 0.94f), FontWeight.REGULAR);
    }

    private float getOpenProgress(long now, int index) {
        long delay = index * PANEL_STAGGER_MS;
        float t = (now - openTime - delay) / (float) PANEL_OPEN_MS;
        return Math.max(0f, Math.min(1f, t));
    }

    private float getCloseProgress(long now, int index) {
        long delay = (long) (panels.size() - 1 - index) * PANEL_STAGGER_MS;
        float t = (now - closeStart - delay) / (float) PANEL_CLOSE_MS;
        return Math.max(0f, Math.min(1f, t));
    }

    private boolean isCloseFinished(long now) {
        long total = PANEL_CLOSE_MS + Math.max(0, panels.size() - 1) * PANEL_STAGGER_MS + 40L;
        return now - closeStart >= total;
    }

    private boolean canInteract() {
        long total = PANEL_OPEN_MS + Math.max(0, panels.size() - 1) * PANEL_STAGGER_MS;
        return !closing && (System.currentTimeMillis() - openTime >= total);
    }

    private void renderDescription(DrawContext ctx, float delta) {
        if (hoveredDescription == null || hoveredDescription.isEmpty()) return;

        float panelTop = panels.isEmpty() ? screenHeight() / 2f - 127f : panels.get(0).y;
        float descY = panelTop - 18f;

        int len = hoveredDescription.length();
        float fontSize;
        if (len <= 20) {
            fontSize = 9f;
        } else if (len <= 40) {
            fontSize = 9f - (len - 20) * 0.04f;
        } else if (len <= 70) {
            fontSize = 8.2f - (len - 40) * 0.03f;
        } else {
            fontSize = Math.max(6f, 7.3f - (len - 70) * 0.02f);
        }

        CustomFontRenderer.drawCenteredText(ctx, hoveredDescription,
                screenWidth() / 2f, descY, fontSize,
                TEXT_WHITE,
                FontWeight.SEMIBOLD);
    }

    public void setHoveredDescription(String desc) {
        this.hoveredDescription = desc;
    }

    public boolean isBindingModule(Module module) {
        return bindingModule == module;
    }

    private void renderSearchHint(DrawContext ctx, float alpha) {
        if (panels.isEmpty() || alpha <= 0.01f) return;
        float searchFade = 1f - searchAnim;
        if (searchFade <= 0.01f) return;

        float baseY = panels.get(0).y + PANEL_H * currentGuiScale + 9f * currentGuiScale;

        // Одинаковое смещение (+), так как в обоих случаях (открытие/закрытие)
        // вне экрана текст должен находиться ниже базовой позиции (baseY + 12f)
        float yOffset = (1f - alpha) * 12f;

        CustomFontRenderer.drawCenteredText(ctx, ClientLanguage.text("ui.search_hint", "Ctrl+F для поиска"),
                screenWidth() * 0.5f, baseY + yOffset, 6.2f,
                RenderUtil.withAlpha(TEXT_WHITE, alpha * searchFade * 0.75f), FontWeight.REGULAR);
    }

    private void renderBindHint(DrawContext ctx) {
        if (bindingModule == null || panels.isEmpty()) return;
        float y = panels.get(0).y + PANEL_H * currentGuiScale + 17f * currentGuiScale;
        String text = ClientLanguage.text("ui.bind", "Бинд") + ": " + bindingModule.getName();
        CustomFontRenderer.drawCenteredText(ctx, text, screenWidth() * 0.5f, y, 5.9f,
                RenderUtil.withAlpha(TEXT_WHITE, 0.90f), FontWeight.REGULAR);
    }

    private void renderSearchOverlay(DrawContext ctx, int mx, int my) {
        float visible = RenderUtil.easeOutQuad(searchAnim);
        if (visible <= 0.01f) return;

        SearchLayout layout = buildSearchLayout();
        float boxH = searchHeightAnim;
        if (boxH <= 5f) return;

        GlassUtil.drawBlurPanel(ctx, layout.x, layout.y, SEARCH_W, boxH, 8f,
                0.92f * visible);
        RenderUtil.drawRoundedRect(ctx, layout.x, layout.y, SEARCH_W, boxH, 8f,
                RenderUtil.withAlpha(SEARCH_TINT, SEARCH_TINT_ALPHA * visible));

        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.search_modules", "Поиск модулей"), layout.x + 8f, layout.y + 6f, 6.6f,
                RenderUtil.withAlpha(TEXT_WHITE, 0.98f * visible), FontWeight.SEMIBOLD);

        float inputX = layout.x + 8f;
        float inputY = layout.y + 15.5f;
        float inputSize = 6.4f;

        if (searchQuery.isEmpty()) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.start_typing", "Начни ввод..."), inputX, inputY, inputSize,
                    RenderUtil.withAlpha(TEXT_DIM, 0.88f * visible), FontWeight.REGULAR);
        } else {
            drawAnimatedSearchQuery(ctx, inputX, inputY, inputSize, visible);
        }
        drawAnimatedRemovedChar(ctx, inputX, inputY, inputSize, visible);

        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caretX = layout.x + 8f + CustomFontRenderer.getTextWidth(searchQuery, 6.4f, FontWeight.REGULAR);
            RenderUtil.drawRect(ctx, caretX + 1f, layout.y + 15f, 0.6f, 7.8f,
                    RenderUtil.withAlpha(TEXT_WHITE, 0.95f * visible));
        }

        float rowYStart = layout.y + 29f;
        float rowHeightWindow = boxH - 30f;

        if (rowHeightWindow > 1f) {
            StencilUtil.enableScissor(layout.x, rowYStart, SEARCH_W, rowHeightWindow);

            float currY = rowYStart + searchScrollOffset;
            for (int i = 0; i < searchResults.size(); i++) {
                SearchResult result = searchResults.get(i);
                boolean hovered = mx >= layout.x + 4f && mx <= layout.x + SEARCH_W - 4f
                        && my >= currY && my <= currY + SEARCH_ROW_H;

                if (hovered) {
                    RenderUtil.drawRoundedRect(ctx, layout.x + 4f, currY, SEARCH_W - 8f, SEARCH_ROW_H - 1f, 4f,
                            RenderUtil.withAlpha(0xFFFFFFFF, 0.12f * visible));
                }

                String iconStr;
                FontWeight fontW;
                switch (result.panel().category) {
                    case CLIENT -> { iconStr = "\uF001"; fontW = FontWeight.CLIENT; }
                    // case WORLD -> { iconStr = "\uF002"; fontW = FontWeight.WP; }
                    // case PLAYER -> { iconStr = "\uF001"; fontW = FontWeight.WP; }
                    case VISUAL -> { iconStr = "\uF001"; fontW = FontWeight.VC; }
                    case UTILS -> { iconStr = "\uF000"; fontW = FontWeight.VC; }
                    default -> { iconStr = "•"; fontW = FontWeight.REGULAR; }
                }

                CustomFontRenderer.drawText(ctx, iconStr, layout.x + 8f, currY + 3.6f, 6.2f,
                        RenderUtil.withAlpha(TEXT_DIM, 0.90f * visible), fontW);

                CustomFontRenderer.drawText(ctx, result.module().getModule().getName(), layout.x + 20f, currY + 3.6f, 6.2f,
                        RenderUtil.withAlpha(TEXT_WHITE, 0.98f * visible), FontWeight.REGULAR);

                CustomFontRenderer.drawRightText(ctx, result.panel().category.getDisplayName(), layout.x + SEARCH_W - 8f, currY + 3.6f, 5.8f,
                        RenderUtil.withAlpha(TEXT_DIM, 0.9f * visible), FontWeight.REGULAR);

                currY += SEARCH_ROW_H;
            }
            StencilUtil.disableScissor();
        }
    }

    private boolean handleSearchKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                closeSearch();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                selectSearchResult(searchSelection);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!searchQuery.isEmpty()) {
                    onSearchQueryChanged(searchQuery.substring(0, searchQuery.length() - 1));
                    refreshSearchResults();
                    EnoughSoundPlayer.playScroll();
                }
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    private boolean handleSearchChar(char chr) {
        if (Character.isISOControl(chr)) return false;
        if (searchQuery.length() >= 40) return true;
        onSearchQueryChanged(searchQuery + chr);
        refreshSearchResults();
        EnoughSoundPlayer.playScroll();
        return true;
    }

    private void openSearch() {
        searchActive = true;
        searchAnim = 0f;
        searchHeightAnim = 30f;
        searchScrollOffset = 0f;
        searchScrollTarget = 0f;
        typedCharIndex = -1;
        typedCharStartMs = 0L;
        removedChar = "";
        removedCharStartMs = 0L;
        searchSelection = 0;
        refreshSearchResults();
    }

    private void closeSearch() {
        searchActive = false;
        searchQuery = "";
        typedCharIndex = -1;
        typedCharStartMs = 0L;
        removedChar = "";
        removedCharStartMs = 0L;
        searchSelection = 0;
        searchResults.clear();
    }

    private void onSearchQueryChanged(String newQuery) {
        if (Objects.equals(searchQuery, newQuery)) return;
        if (newQuery.length() == searchQuery.length() + 1 && newQuery.startsWith(searchQuery)) {
            typedCharIndex = searchQuery.length();
            typedCharStartMs = System.currentTimeMillis();
            removedChar = "";
            removedCharStartMs = 0L;
        } else if (newQuery.length() + 1 == searchQuery.length() && searchQuery.startsWith(newQuery)) {
            removedChar = searchQuery.substring(searchQuery.length() - 1);
            removedCharStartMs = System.currentTimeMillis();
            typedCharIndex = -1;
            typedCharStartMs = 0L;
        } else {
            typedCharIndex = -1;
            typedCharStartMs = 0L;
        }
        searchQuery = newQuery;
    }

    private void drawAnimatedSearchQuery(DrawContext ctx, float x, float y, float size, float alpha) {
        RenderUtil.drawAnimatedInputText(ctx, searchQuery, x, y, size,
                RenderUtil.withAlpha(TEXT_WHITE, 0.98f * alpha), FontWeight.REGULAR,
                typedCharIndex, typedCharStartMs);

        float progress = typedCharStartMs <= 0L ? 1f
                : Math.max(0f, Math.min(1f, (System.currentTimeMillis() - typedCharStartMs) / 220f));
        if (progress >= 0.999f) {
            typedCharIndex = -1;
            typedCharStartMs = 0L;
        }
    }

    private void drawAnimatedRemovedChar(DrawContext ctx, float x, float y, float size, float alpha) {
        if (removedChar.isEmpty() || removedCharStartMs <= 0L) return;
        float progress = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - removedCharStartMs) / 180f));
        if (progress >= 0.999f) {
            removedChar = "";
            removedCharStartMs = 0L;
            return;
        }
        float symbolW = CustomFontRenderer.getTextWidth(removedChar, size, FontWeight.REGULAR);
        float symbolX = x + CustomFontRenderer.getTextWidth(searchQuery, size, FontWeight.REGULAR);
        float scale = 1f - 0.72f * RenderUtil.easeInQuad(progress);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(symbolX + symbolW * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * progress));
        ctx.getMatrices().scale(scale, scale, 1f);
        CustomFontRenderer.drawText(ctx, removedChar, -symbolW * 0.5f, -size * 0.5f, size,
                RenderUtil.withAlpha(TEXT_WHITE, alpha * (1f - progress)), FontWeight.REGULAR);
        ctx.getMatrices().pop();
    }

    private void refreshSearchResults() {
        searchResults.clear();
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            searchScrollTarget = 0f;
            return;
        }

        for (PanelData panel : panels) {
            for (MenuModule module : panel.modules) {
                String name = module.getModule().getName().toLowerCase(Locale.ROOT);
                if (name.contains(q)) {
                    searchResults.add(new SearchResult(panel, module));
                }
            }
        }

        searchResults.sort((a, b) -> {
            String an = a.module().getModule().getName().toLowerCase(Locale.ROOT);
            String bn = b.module().getModule().getName().toLowerCase(Locale.ROOT);
            boolean aStarts = an.startsWith(q);
            boolean bStarts = bn.startsWith(q);
            if (aStarts != bStarts) return aStarts ? -1 : 1;
            return an.compareTo(bn);
        });

        if (searchResults.size() > SEARCH_MAX_RESULTS) {
            searchResults.subList(SEARCH_MAX_RESULTS, searchResults.size()).clear();
        }
        searchSelection = 0;

        float maxScroll = Math.max(0, (searchResults.size() - SEARCH_VISIBLE_ROWS) * SEARCH_ROW_H);
        if (-searchScrollTarget > maxScroll) {
            searchScrollTarget = -maxScroll;
        }
    }

    private void selectSearchResult(int index) {
        if (index < 0 || index >= searchResults.size()) return;
        SearchResult result = searchResults.get(index);
        focusModule(result.panel(), result.module());
        closeSearch();
        EnoughSoundPlayer.playScroll();
    }

    private void focusModule(PanelData panel, MenuModule module) {
        int idx = panel.modules.indexOf(module);
        if (idx < 0) return;

        if (panel.settingsView != null) {
            closeSettings(panel);
        }

        float scale = Math.max(0.35f, currentGuiScale);
        float contentH = (PANEL_H - HEADER_H) * scale;
        float total = panel.modules.size() * MODULE_H * scale;
        float max = Math.max(0f, total - contentH);
        float target = -(idx * MODULE_H * scale - contentH * 0.35f);
        panel.scrollTarget = Math.max(-max, Math.min(0f, target));
        module.highlightFromSearch();
    }

    private SearchLayout buildSearchLayout() {
        float x = screenWidth() * 0.5f - SEARCH_W * 0.5f;
        float y = panels.isEmpty() ? 18f : panels.get(0).y - 38f;
        if (y < 8f) y = 8f;
        return new SearchLayout(x, y);
    }

    private void renderPanel(DrawContext ctx, PanelData p, int mx, int my,
                             float delta, float alpha, float yOffset, float scale, float guiScale) {
        if (alpha <= 0.001f) return;

        float anchorX = p.x;
        float anchorY = p.y + yOffset * guiScale;
        float baseW = PANEL_W * guiScale;
        float baseH = PANEL_H * guiScale;
        float safeScale = Math.max(0.08f, scale * guiScale);
        float pw = PANEL_W * safeScale;
        float ph = PANEL_H * safeScale;
        float px = anchorX + (baseW - pw) * 0.5f;
        float py = anchorY + (baseH - ph) * 0.5f;
        float headerH = HEADER_H * safeScale;
        float moduleH = MODULE_H * safeScale;
        float radius = PANEL_R * safeScale;

        float frameDelta = RenderUtil.getFrameDelta();
        p.scrollOffset = RenderUtil.animate(p.scrollOffset, p.scrollTarget, 0.36f, frameDelta);
        p.swapAnim = RenderUtil.animate(p.swapAnim, p.swapTarget, 0.14f, frameDelta);

        if (p.swapTarget == 0f && p.swapAnim < 0.01f) {
            p.swapAnim = 0f;
            p.lastSettingsView = null;
        }

        float blurAlpha = alpha * alpha;
        GlassUtil.drawGlassPanel(ctx, px, py, pw, ph, radius,
                PANEL_TINT, alpha * PANEL_TINT_ALPHA, blurAlpha);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        renderHeader(ctx, p, px, py, pw, headerH, radius, mx, my, alpha, delta, safeScale);

        float contentY = py + headerH;
        float contentH = ph - headerH;

        StencilUtil.enableScissor(px, contentY, pw, contentH);

        float swapVisual = RenderUtil.easeInOutCubic(Math.max(0f, Math.min(1f, p.swapAnim)));
        float swapClamped = Math.max(0f, Math.min(1f, swapVisual));

        if (swapClamped < 0.99f) {
            float offX = -pw * swapVisual;
            float listAlpha = alpha * (1f - swapClamped);
            if (listAlpha > 0.01f) {
                renderModuleList(ctx, p, px + offX, contentY, pw, contentH, moduleH, safeScale, mx, my, delta, listAlpha);
            }
        }

        if (swapClamped > 0.01f && p.lastSettingsView != null) {
            float offX = pw * (1f - swapVisual);
            float settAlpha = alpha * swapClamped;
            if (settAlpha > 0.01f) {
                float settingsX = px + offX;
                float localMx = settingsX + (mx - settingsX) / safeScale;
                float localMy = contentY + (my - contentY) / safeScale;
                ctx.getMatrices().push();
                ctx.getMatrices().translate(settingsX, contentY, 0f);
                ctx.getMatrices().scale(safeScale, safeScale, 1f);
                ctx.getMatrices().translate(-settingsX, -contentY, 0f);
                p.lastSettingsView.setRenderTransform(settingsX, contentY, safeScale);
                p.lastSettingsView.render(ctx, settingsX, contentY, PANEL_W, PANEL_H - HEADER_H,
                        (int) localMx, (int) localMy, delta, settAlpha, p);
                ctx.getMatrices().pop();
            }
        }

        ctx.draw();
        StencilUtil.disableScissor();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderHeader(DrawContext ctx, PanelData p, float px, float py,
                              float pw, float headerH, float radius,
                              int mx, int my, float alpha, float delta, float uiScale) {

        RenderUtil.drawRoundedRect(ctx, px, py, pw, headerH,
                radius, radius, 0, 0,
                RenderUtil.withAlpha(HEADER_TINT, alpha * HEADER_TINT_ALPHA));
        RenderUtil.drawRect(ctx, px + 2f * uiScale, py + headerH - 0.5f * uiScale, pw - 4f * uiScale, 0.5f * uiScale,
                RenderUtil.withAlpha(0xFFFFFF, alpha * 0.06f));

        ctx.draw();
        StencilUtil.enableScissor(px, py, pw, headerH);

        float swapVisual = RenderUtil.easeInOutCubic(Math.max(0f, Math.min(1f, p.swapAnim)));
        float swapClamped = Math.max(0f, Math.min(1f, swapVisual));

        if (swapClamped > 0.01f) {
            float offX = pw * (1f - swapVisual);
            float a = alpha * swapClamped;

            boolean backHovered = mx >= px && mx <= px + 26f * uiScale && my >= py && my <= py + headerH;
            p.backHoverAnim = RenderUtil.animate(p.backHoverAnim, backHovered ? 1f : 0f, 0.55f, RenderUtil.getFrameDelta());

            float backH = 7f * uiScale;
            float backW = backH * (25f / 48f);
            float backX = px + offX + 8f * uiScale;
            float backY = py + (headerH - backH) * 0.5f;
            Draw.image(ctx, BACK_ICON, backX, backY, backW, backH,
                    CAT_ICON_TEX, CAT_ICON_TEX, a * (0.75f + 0.25f * p.backHoverAnim));

            if (p.lastSettingsView != null) {
                CustomFontRenderer.drawText(ctx, p.lastSettingsView.getModule().getName(),
                        px + offX + 17f * uiScale, py + 7f * uiScale, 8f * uiScale,
                        RenderUtil.withAlpha(TEXT_WHITE, a), FontWeight.SEMIBOLD);
            }
        }

        if (swapClamped < 0.99f) {
            float offX = -pw * swapVisual;
            float a = alpha * (1f - swapClamped);

            CustomFontRenderer.drawText(ctx, p.category.getDisplayName(), px + offX + 8f * uiScale, py + 7f * uiScale, 8f * uiScale,
                    RenderUtil.withAlpha(TEXT_WHITE, a), FontWeight.SEMIBOLD);
        }

        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (p.category == Category.CLIENT) {
            float iconSize = 9f * uiScale;
            CustomFontRenderer.drawRightText(ctx, "\uF001",
                    px + pw - 8f * uiScale,
                    py + (headerH - iconSize) * 0.5f,
                    iconSize,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha * 0.98f),
                    FontWeight.CLIENT);
/*
        } else if (p.category == Category.WORLD) {
            float iconSize = 11f * uiScale;
            CustomFontRenderer.drawRightText(ctx, "\uF002",
                    px + pw - 8f * uiScale,
                    py + (headerH - iconSize) * 0.5f,
                    iconSize,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha * 0.98f),
                    FontWeight.WP);

        } else if (p.category == Category.PLAYER) {
            float iconSize = 10f * uiScale;
            CustomFontRenderer.drawRightText(ctx, "\uF001",
                    px + pw - 8f * uiScale,
                    py + (headerH - iconSize) * 0.5f,
                    iconSize,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha * 0.98f),
                    FontWeight.WP);
*/
        } else if (p.category == Category.VISUAL) {
            float iconSize = 10f * uiScale;
            CustomFontRenderer.drawRightText(ctx, "\uF001",
                    px + pw - 8f * uiScale,
                    py + (headerH - iconSize) * 0.5f,
                    iconSize,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha * 0.98f),
                    FontWeight.VC);

        } else if (p.category == Category.UTILS) {
            float iconSize = 9f * uiScale;
            CustomFontRenderer.drawRightText(ctx, "\uF000",
                    px + pw - 8f * uiScale,
                    py + (headerH - iconSize) * 0.5f,
                    iconSize,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha * 0.98f),
                    FontWeight.VC);

        } else {
            Identifier catIcon = CATEGORY_ICONS.get(p.category);
            if (catIcon != null) {
                float iconSize = 12f * uiScale;
                float iconX = px + pw - 7f * uiScale - iconSize;
                float iconY = py + (headerH - iconSize) * 0.5f;
                Draw.image(ctx, catIcon, iconX, iconY, iconSize, iconSize,
                        CAT_ICON_TEX, CAT_ICON_TEX, alpha * 0.98f);
            } else {
                CustomFontRenderer.drawRightText(ctx, String.valueOf(p.modules.size()),
                        px + pw - 8f * uiScale, py + 8f * uiScale, 6f * uiScale,
                        RenderUtil.withAlpha(TEXT_DIM, alpha * 0.72f), FontWeight.LIGHT);
            }
        }
        ctx.draw();
        StencilUtil.disableScissor();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderModuleList(DrawContext ctx, PanelData p, float px, float cy,
                                  float pw, float contentH, float rowH, float uiScale,
                                  int mx, int my, float delta, float alpha) {
        float modY = cy + p.scrollOffset;
        float total = 0;

        for (MenuModule mod : p.modules) {
            mod.render(ctx, px, modY, pw, rowH, mx, my, delta, alpha, uiScale);
            modY += rowH;
            total += rowH;
        }

        p.maxScroll = Math.max(0, total - contentH);
    }

    public void openSettings(MenuModule mod, PanelData p) {
        if (mod.getModule().getSettings().isEmpty()) {
            mod.shake();
            return;
        }
        p.settingsView = new MenuSettings(mod.getModule());
        p.lastSettingsView = p.settingsView;
        p.settingsScrollOffset = 0;
        p.settingsScrollTarget = 0;
        p.swapTarget = 1f;
    }

    public void closeSettings(PanelData p) {
        p.settingsView = null;
        p.swapTarget = 0f;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (bindingModule != null) {
            bindingModule.setKeyBind(BindKeyUtil.mouseButtonToCode(button));
            finishBinding();
            return true;
        }

        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (searchActive) {
            SearchLayout layout = buildSearchLayout();
            float boxH = searchHeightAnim;
            float rowYStart = layout.y + 29f;
            float rowHeightWindow = Math.min(searchResults.size(), SEARCH_VISIBLE_ROWS) * SEARCH_ROW_H;

            if (mx >= layout.x && mx <= layout.x + SEARCH_W && my >= layout.y && my <= layout.y + boxH) {
                if (my >= rowYStart && my <= rowYStart + rowHeightWindow) {
                    float currY = rowYStart + searchScrollOffset;
                    for (int i = 0; i < searchResults.size(); i++) {
                        if (mx >= layout.x + 4f && mx <= layout.x + SEARCH_W - 4f
                                && my >= currY && my <= currY + SEARCH_ROW_H) {
                            selectSearchResult(i);
                            return true;
                        }
                        currY += SEARCH_ROW_H;
                    }
                }
                return true;
            }

            closeSearch();
            return true;
        }

        if (!canInteract()) return true;

        for (PanelData p : panels) {
            float gs = Math.max(0.35f, currentGuiScale);
            float panelW = PANEL_W * gs;
            float panelH = PANEL_H * gs;
            float headerH = HEADER_H * gs;
            float moduleH = MODULE_H * gs;
            if (mx < p.x || mx > p.x + panelW || my < p.y || my > p.y + panelH) continue;

            if (my <= p.y + headerH) {
                if (p.settingsView != null && p.swapAnim > 0.3f) {
                    closeSettings(p);
                    return true;
                }
                continue;
            }

            float contentY = p.y + headerH;

            if (p.settingsView != null && p.swapAnim > 0.5f) {
                float localMx = p.x + (mx - p.x) / gs;
                float localMy = contentY + (my - contentY) / gs;
                p.lastSettingsView.mouseClicked(localMx, localMy, button, p.x, contentY, PANEL_W, PANEL_H - HEADER_H, p);
                return true;
            }

            float modY = contentY + p.scrollOffset;
            for (MenuModule mod : p.modules) {
                if (mx >= p.x && mx <= p.x + panelW && my >= modY && my <= modY + moduleH) {
                    boolean blocked = ModuleManager.getInstance().isBlocked(mod.getModule());
                    if (button == 0) {
                        if (blocked || (!mod.getModule().isAvailable() && !mod.getModule().isEnabled())) {
                            mod.shake();
                            return true;
                        }
                        mod.getModule().toggle();
                        return true;
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        if (blocked) {
                            mod.shake();
                            return true;
                        }
                        openSettings(mod, p);
                        return true;
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        if (blocked) {
                            mod.shake();
                            return true;
                        }
                        startBinding(mod.getModule(), p);
                        return true;
                    }
                }
                modY += moduleH;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!canInteract()) return true;
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        for (PanelData p : panels) {
            if (p.settingsView != null && p.lastSettingsView != null) {
                p.lastSettingsView.mouseReleased(mx, my, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!canInteract()) return true;
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        for (PanelData p : panels) {
            if (p.settingsView != null && p.lastSettingsView != null) {
                float gs = Math.max(0.35f, currentGuiScale);
                float contentY = p.y + HEADER_H * gs;
                float localMx = p.x + (mx - p.x) / gs;
                float localMy = contentY + (my - contentY) / gs;
                if (p.lastSettingsView.mouseDragged(localMx, localMy, button,
                        p.x, contentY, PANEL_W)) return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (searchActive) {
            if (!searchResults.isEmpty()) {
                float maxScroll = Math.max(0, (searchResults.size() - SEARCH_VISIBLE_ROWS) * SEARCH_ROW_H);
                searchScrollTarget += (float) (v * 14);
                searchScrollTarget = Math.max(-maxScroll, Math.min(0, searchScrollTarget));
                EnoughSoundPlayer.playScroll();
            }
            return true;
        }

        if (!canInteract()) return true;
        for (PanelData p : panels) {
            float gs = Math.max(0.35f, currentGuiScale);
            float panelW = PANEL_W * gs;
            float panelH = PANEL_H * gs;
            float headerH = HEADER_H * gs;
            if (mx >= p.x && mx <= p.x + panelW
                    && my >= p.y && my <= p.y + panelH) {

                if (p.settingsView != null && p.swapAnim > 0.5f && p.lastSettingsView != null) {
                    float localMx = p.x + (mx - p.x) / gs;
                    float contentY = p.y + headerH;
                    float localMy = contentY + (my - contentY) / gs;
                    p.lastSettingsView.mouseScrolled(localMx, localMy, v, p.x, contentY, PANEL_W);
                } else {
                    p.scrollTarget += (float) (v * 16);
                    p.scrollTarget = Math.max(-p.maxScroll, Math.min(0, p.scrollTarget));
                }
                EnoughSoundPlayer.playScroll();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    private void startBinding(Module module, PanelData panel) {
        bindingModule = module;
        bindingPanel = panel;
        if (bindingPanel != null && bindingPanel.settingsView != null) {
            closeSettings(bindingPanel);
        }
        closeSearch();
    }

    private void finishBinding() {
        bindingModule = null;
        bindingPanel = null;
    }

    public static class PanelData {
        public final Category category;
        public final List<MenuModule> modules = new ArrayList<>();
        public float x, y;

        public float scrollOffset = 0, scrollTarget = 0, maxScroll = 0;
        public float settingsScrollOffset = 0, settingsScrollTarget = 0;

        public float swapAnim = 0;
        public float swapTarget = 0;
        public MenuSettings settingsView = null;
        public MenuSettings lastSettingsView = null;
        public float backHoverAnim = 0;

        public PanelData(Category category) {
            this.category = category;
        }
    }

    private record SearchResult(PanelData panel, MenuModule module) {}

    private record SearchLayout(float x, float y) {}
}
