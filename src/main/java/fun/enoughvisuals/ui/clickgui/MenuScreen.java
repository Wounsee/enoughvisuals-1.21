package fun.enoughvisuals.ui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.Info;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.client.GuiSize;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.ui.clickgui.components.ColorPickerPopup;
import fun.enoughvisuals.ui.clickgui.components.IntegratedToolPage;
import fun.enoughvisuals.systems.network.HolyWorldFeatureControl;
import fun.enoughvisuals.ui.clickgui.components.ModuleCard;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MenuScreen extends Screen {
    /** Flip to true to put the experimental Events entry back into the sidebar. */
    public static boolean SHOW_EVENTS_TAB = false;
    public static final int DEFAULT_ACCENT = 0xFF927BE0;
    public static int ACCENT = DEFAULT_ACCENT;
    public static int ACCENT_LIGHT = 0xFFC0B2F2;
    public static final int TEXT_WHITE = 0xFFF4F1FA;
    public static final int TEXT_GRAY = 0xFFB9B3C7;
    public static final int TEXT_DIM = 0xFF7D778A;
    public static final int TEXT_DARK = 0xFF555162;
    public static final int SEPARATOR = 0x20FFFFFF;
    public static int TOGGLE_ON = ACCENT;
    public static int TOGGLE_OFF = 0xFF302D3A;
    public static final int TOGGLE_KNOB = 0xFFFFFFFF;
    public static int SETTING_BG = 0xFF1C1924;
    public static int SLIDER_BG = 0xFF2B2735;
    public static int SLIDER_FILL = ACCENT;
    public static final int CHECKBOX_BG = 0xFF2B2735;
    public static final int MODULE_HOVER = 0xFFFFFF;
    public static int SETTING_PANEL_OUTER = 0xFF0D0C12;
    public static int SETTING_PANEL_INNER = 0xFF15131C;
    public static int SETTING_INPUT_BG = 0xFF100E16;
    public static int PANEL_TINT = 0x121018;
    public static float PANEL_TINT_ALPHA = 0.93f;

    public static int WINDOW_BG = 0xFF111018;
    public static int SIDEBAR_BG = 0xFF181521;
    public static int CARD_BG = 0xFF181620;
    public static int CARD_HOVER = 0xFF1D1A27;
    public static int INSET_BG = 0xFF0F0E15;
    public static int CHIP_BG = 0xFF211E2A;
    public static int CHIP_TEXT = 0xFFAAA2BC;
    public static int CHIP_TEXT_ON = 0xFFF7F3FF;
    public static int VALUE_DIM = 0xFF958DA6;
    public static int STROKE = 0x12FFFFFF;

    private static final float DESIGN_W = 520f;
    private static final float DESIGN_H = 300f;
    private static final float SIDEBAR_W = 122f;
    private static final float HEADER_H = 34f;
    private static final float PAD = 8.5f;
    private static final float GAP = 6.5f;
    private static float WINDOW_RADIUS = 10f;
    private static final float NAV_CATEGORY_Y = 50f;
    private static final float NAV_TOOLS_Y = 139f;
    private static final float NAV_ROW_H = 25f;
    private static final float NAV_TOOL_ROW_H = 21f;
    private static final long OPEN_MS = 470L;
    private static final long CLOSE_MS = 285L;
    private static final long CARD_STAGGER_MS = 34L;

    private static final String ICON_VISUAL = "\uF00A";
    private static final String ICON_WAYPOINT = "\uF000";
    private static final String ICON_CONFIG = "\uF001";
    private static final String ICON_UTILS = "\uF002";
    private static final String ICON_FRIENDS = "\uF003";
    private static final String LOGO_GLYPH = "\uF002";
    private static final String ICON_BIND = "\uF005";
    private static final String ICON_CLIENT = "\uF007";
    private static final String ICON_PUZZLE = "\uF004";
    private static final String ICON_SEARCH = "\uF008";
    private static final Set<String> TOOL_MODULES = Set.of("configs", "friends", "gps", "macros");

    private final Category[] categories = Arrays.stream(Category.values())
            .filter(category -> !HolyWorldFeatureControl.getInstance().isCategoryBlocked(category))
            .toArray(Category[]::new);
    private final Map<Module, ModuleCard> cardCache = new LinkedHashMap<>();
    private final List<ModuleCard> cards = new ArrayList<>();
    private final float[] navHover = new float[16];
    private final IntegratedToolPage toolPage = new IntegratedToolPage();
    private final IntegratedToolPage.Type initialTool;

    private int activeCategory;
    private float navIndicator;
    private IntegratedToolPage.Type activeTool;
    private float scrollOffset;
    private float scrollTarget;
    private float maxScroll;
    private float scrollbarAlpha;
    private boolean searchActive;
    private String searchQuery = "";
    private float searchFocus;
    private int typedCharIndex = -1;
    private long typedCharStartMs;

    private Module bindingModule;
    private BindSetting bindingSetting;
    private ModuleCard focusedCard;
    private ModuleCard expandedCard;
    private ModuleCard dragCard;
    private ColorPickerPopup picker;

    private String hoverDescription;
    private String shownDescription;
    private String descriptionCandidate;
    private long descriptionSince;
    private float descriptionAnimation;

    private long openTime;
    private boolean closing;
    private long closeStart;
    private float fixedScreenScale = 1f;
    private float virtualW;
    private float virtualH;
    private float scale = 1f;
    private float windowX;
    private float windowY;
    private float windowW;
    private float windowH;
    private boolean initialToolApplied;
    private int contentTransitionPhase;
    private long contentTransitionAt;
    private int contentTransitionDirection = 1;
    private int pendingCategory = -1;
    private IntegratedToolPage.Type pendingTool;

    public MenuScreen() {
        this(null);
    }

    public MenuScreen(IntegratedToolPage.Type initialTool) {
        super(Text.literal("Enough Visuals"));
        IntegratedToolPage.Type allowedInitial =
                initialTool != null && isToolVisible(initialTool) ? initialTool : null;
        if (categories.length == 0 && allowedInitial == null) {
            allowedInitial = visibleTools().stream().findFirst().orElse(null);
        }
        this.initialTool = allowedInitial;
    }

    public static void syncTheme() {
        int accent = CustomTheme.guiAccent();
        WINDOW_RADIUS = CustomTheme.clickGuiRadius();
        ACCENT = accent;
        ACCENT_LIGHT = lighten(accent, 0.34f);
        TOGGLE_ON = accent;
        SLIDER_FILL = accent;

        WINDOW_BG = 0xFF08080B;
        SIDEBAR_BG = 0xFF0B0B0F;
        CARD_BG = 0xFF101015;
        CARD_HOVER = 0xFF15151C;
        INSET_BG = 0xFF09090D;
        CHIP_BG = 0xFF17171F;
        CHIP_TEXT = mix(0xFFAAA2BC, accent, 0.14f);
        CHIP_TEXT_ON = lighten(accent, 0.78f);
        VALUE_DIM = mix(0xFF958DA6, accent, 0.12f);
        TOGGLE_OFF = 0xFF27272F;
        SLIDER_BG = 0xFF24242C;
        SETTING_BG = 0xFF111117;
        SETTING_PANEL_OUTER = darken(WINDOW_BG, 0.28f);
        SETTING_PANEL_INNER = INSET_BG;
        SETTING_INPUT_BG = darken(INSET_BG, 0.10f);
        PANEL_TINT = 0x000000;
        PANEL_TINT_ALPHA = CustomTheme.blurDisabled() ? 1f : 0.93f * CustomTheme.clickGuiOpacity();
    }

    private static int mix(int base, int tint, float amount) {
        return 0xFF000000 | (RenderUtil.lerpColor(base, tint, amount) & 0x00FFFFFF);
    }

    private static int lighten(int color, float amount) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        r += Math.round((255 - r) * amount);
        g += Math.round((255 - g) * amount);
        b += Math.round((255 - b) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int darken(int color, float amount) {
        int r = Math.round(((color >> 16) & 255) * (1f - amount));
        int g = Math.round(((color >> 8) & 255) * (1f - amount));
        int b = Math.round((color & 255) * (1f - amount));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        closing = false;
        closeStart = 0L;
        searchActive = false;
        searchQuery = "";
        searchFocus = 0f;
        typedCharIndex = -1;
        bindingModule = null;
        bindingSetting = null;
        focusedCard = null;
        expandedCard = null;
        dragCard = null;
        picker = null;
        descriptionCandidate = null;
        shownDescription = null;
        descriptionAnimation = 0f;
        scrollOffset = scrollTarget = 0f;
        if (!initialToolApplied && initialTool != null && isToolVisible(initialTool)) {
            activeTool = initialTool;
            toolPage.activate(initialTool);
            initialToolApplied = true;
        }
        navIndicator = navTargetBaseY();
        Arrays.fill(navHover, 0f);

        cardCache.clear();
        for (Category category : categories) {
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
            modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
            for (Module module : modules) {
                if (!TOOL_MODULES.contains(module.getId())) {
                    cardCache.put(module, new ModuleCard(module, this));
                }
            }
        }
        rebuildCards(0, openTime + 55L);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void updateLayout() {
        fixedScreenScale = ScreenScaleUtil.scale();
        virtualW = ScreenScaleUtil.virtualWidth();
        virtualH = ScreenScaleUtil.virtualHeight();
        if (virtualW <= 0f) virtualW = width;
        if (virtualH <= 0f) virtualH = height;

        float requested = 1f;
        GuiSize size = ModuleManager.getInstance().get(GuiSize.class);
        if (size != null && size.isEnabled()) requested = size.percent() / 50f;
        float fit = Math.min((virtualW - 30f) / DESIGN_W, (virtualH - 22f) / DESIGN_H);
        scale = Math.max(0.50f, Math.min(requested, fit));
        windowW = DESIGN_W * scale;
        windowH = DESIGN_H * scale;
        windowX = (virtualW - windowW) * 0.5f;
        windowY = (virtualH - windowH) * 0.5f;
    }

    private float mainX() { return windowX + SIDEBAR_W * scale; }
    private float mainW() { return windowW - SIDEBAR_W * scale; }
    private float contentX() { return mainX() + PAD * scale; }
    private float contentY() { return windowY + (HEADER_H + 7f) * scale; }
    private float contentW() { return mainW() - PAD * 2f * scale; }
    private float contentH() { return windowH - (HEADER_H + 7f) * scale - 8f * scale; }
    public float colWidth() { return (contentW() - GAP * scale) * 0.5f; }
    public float uiScale() { return scale; }

    private void rebuildCards(int direction, long introBase) {
        cards.clear();
        List<Module> selected = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (Module module : cardCache.keySet()) {
            if (!query.isEmpty()) {
                String name = module.getName().toLowerCase(Locale.ROOT);
                String description = module.getDescription().toLowerCase(Locale.ROOT);
                if (name.contains(query) || description.contains(query)) selected.add(module);
            } else if (module.getCategory() == categories[activeCategory]) {
                selected.add(module);
            }
        }
        if (!query.isEmpty()) {
            selected.sort((a, b) -> {
                boolean ap = a.getName().toLowerCase(Locale.ROOT).startsWith(query);
                boolean bp = b.getName().toLowerCase(Locale.ROOT).startsWith(query);
                if (ap != bp) return ap ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
        }
        for (int i = 0; i < selected.size(); i++) {
            ModuleCard card = cardCache.get(selected.get(i));
            card.column = i & 1;
            card.beginIntro(introBase + i * CARD_STAGGER_MS, direction);
            cards.add(card);
        }
        scrollOffset = Math.max(scrollOffset, -16f * scale);
        scrollTarget = 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client == null || client.world == null) RenderUtil.beginFrame();
        StencilUtil.resetRenderState();
        syncTheme();
        updateLayout();
        int mx = Math.round(ScreenScaleUtil.screenToVirtual(mouseX));
        int my = Math.round(ScreenScaleUtil.screenToVirtual(mouseY));

        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (CustomTheme.clickGuiBlurEnabled()) GlassUtil.beginFrame(); else GlassUtil.skipFrame();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        long now = System.currentTimeMillis();
        if (closing && now - closeStart >= CLOSE_MS + 35L) {
            StencilUtil.resetRenderState();
            if (client != null && client.currentScreen == this) client.setScreen(null);
            RenderSystem.depthMask(true);
            StencilUtil.resetRenderState();
            return;
        }

        float progress = !closing
                ? clamp((now - openTime) / (float) OPEN_MS)
                : 1f - clamp((now - closeStart) / (float) CLOSE_MS);
        float alpha = closing ? RenderUtil.easeInOutCubic(progress) : RenderUtil.easeOutExpo(progress);
        float geometry = closing ? RenderUtil.easeInOutCubic(progress) : RenderUtil.easeOutBack(progress);
        float windowScale = 0.925f + 0.075f * geometry;
        float yOffset = (1f - RenderUtil.easeOutQuart(progress)) * 11f * scale;

        float frame = RenderUtil.getFrameDelta();
        scrollOffset = RenderUtil.animate(scrollOffset, scrollTarget, 0.30f, frame);
        navIndicator = RenderUtil.animate(navIndicator, navTargetBaseY(), 0.25f, frame);
        searchFocus = RenderUtil.animate(searchFocus, searchActive ? 1f : 0f, 0.27f, frame);
        boolean scrolling = Math.abs(scrollTarget - scrollOffset) > 0.4f;
        scrollbarAlpha = RenderUtil.animate(scrollbarAlpha, scrolling || maxScroll > 0.5f ? 1f : 0f, 0.18f, frame);
        hoverDescription = null;

        context.getMatrices().push();
        context.getMatrices().scale(fixedScreenScale, fixedScreenScale, 1f);
        float previousScissorScale = HudElement.setActiveScissorScale(fixedScreenScale);
        try {
            float centerX = windowX + windowW * 0.5f;
            float centerY = windowY + windowH * 0.5f;
            context.getMatrices().push();
            context.getMatrices().translate(centerX, centerY + yOffset, 0f);
            context.getMatrices().scale(windowScale, windowScale, 1f);
            context.getMatrices().translate(-centerX, -centerY, 0f);
            renderWindow(context, mx, my, alpha, now);
            context.getMatrices().pop();

            renderHint(context, alpha, now);
            if (picker != null) {
                if (picker.isFinished()) picker = null;
                else picker.render(context, scale, mx, my, alpha);
            }
        } finally {
            HudElement.setActiveScissorScale(previousScissorScale);
            context.getMatrices().pop();
        }

        RenderSystem.depthMask(true);
        StencilUtil.resetRenderState();
        MouseParticles.renderScreen(context, mouseX, mouseY, delta);
    }

    private void renderWindow(DrawContext context, int mx, int my, float alpha, long now) {
        RenderUtil.drawShadow(context, windowX, windowY, windowW, windowH, WINDOW_RADIUS * scale, 15f * scale,
                RenderUtil.withAlpha(0x050309, 0.50f * alpha));
        GlassUtil.drawGlassPanel(context, windowX, windowY, windowW, windowH, WINDOW_RADIUS * scale,
                WINDOW_BG & 0x00FFFFFF, PANEL_TINT_ALPHA * alpha, alpha * alpha);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        renderSidebar(context, mx, my, alpha, now);
        renderHeader(context, mx, my, alpha, now);
        updateContentTransition(now);
        float transitionAlpha = 1f;
        float transitionOffset = 0f;
        if (contentTransitionPhase == 1) {
            float p = clamp((now - contentTransitionAt) / 145f);
            transitionAlpha = 1f - RenderUtil.easeInCubic(p);
            transitionOffset = -RenderUtil.easeInCubic(p) * 9f * scale * contentTransitionDirection;
        } else if (contentTransitionPhase == 2) {
            float p = clamp((now - contentTransitionAt) / 360f);
            transitionAlpha = RenderUtil.easeOutCubic(p);
            transitionOffset = (1f - RenderUtil.easeOutQuart(p)) * 11f * scale * contentTransitionDirection;
        }
        context.getMatrices().push();
        context.getMatrices().translate(transitionOffset, 0f, 0f);
        renderContent(context, mx, my, alpha * transitionAlpha, now);
        context.getMatrices().pop();
    }

    private void renderSidebar(DrawContext context, int mx, int my, float alpha, long now) {
        float sw = SIDEBAR_W * scale;
        StencilUtil.enableScissor(windowX, windowY, sw, windowH);
        GlassUtil.drawGlassPanel(context, windowX, windowY + 0.5f * scale, sw + WINDOW_RADIUS * scale, windowH - scale,
                WINDOW_RADIUS * scale, 0x030305, 0.82f * alpha, 0.94f * alpha);
        StencilUtil.disableScissor();
        RenderUtil.drawRoundedRect(context, windowX, windowY, sw, windowH,
                WINDOW_RADIUS * scale, 0f, 0f, WINDOW_RADIUS * scale,
                RenderUtil.withAlpha(SIDEBAR_BG, 0.82f * alpha));
        RenderUtil.drawRect(context, windowX + sw - 0.5f, windowY + 1f, 0.5f, windowH - 2f,
                RenderUtil.withAlpha(0xFFFFFF, 0.045f * alpha));

        float brandIntro = intro(now, openTime + 35L, 260L);
        float logoX = windowX + 6f * scale;
        float logoY = windowY + 4.9f * scale;
        float logoSize = 36f * scale * (0.82f + 0.18f * RenderUtil.easeOutBack(brandIntro));
        CustomFontRenderer.drawText(context, LOGO_GLYPH, logoX, logoY, logoSize,
                RenderUtil.withAlpha(ACCENT_LIGHT, alpha * brandIntro), FontWeight.LOGO);
        CustomFontRenderer.drawText(context, Info.DISPLAY_NAME, windowX + 43f * scale, windowY + 12.7f * scale,
                8.5f * scale, RenderUtil.withAlpha(TEXT_WHITE, alpha * brandIntro), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, Info.VERSION, windowX + 43f * scale, windowY + 24.9f * scale,
                6f * scale, RenderUtil.withAlpha(TEXT_DIM, 0.94f * alpha * brandIntro), FontWeight.REGULAR);

        float categoryY = windowY + NAV_CATEGORY_Y * scale;
        float rowH = NAV_ROW_H * scale;
        float indicatorRowH = activeTool == null ? rowH : NAV_TOOL_ROW_H * scale;
        float indicatorY = windowY + navIndicator * scale;
        RenderUtil.drawRoundedRect(context, windowX + 10f * scale, indicatorY + 2f * scale,
                sw - 20f * scale, indicatorRowH - 4f * scale, 7f * scale,
                RenderUtil.withAlpha(ACCENT, 0.13f * alpha));

        float frame = RenderUtil.getFrameDelta();
        for (int i = 0; i < categories.length; i++) {
            float y = categoryY + i * rowH;
            boolean hovered = hit(mx, my, windowX + 8f * scale, y, sw - 16f * scale, rowH);
            navHover[i] = RenderUtil.animate(navHover[i], hovered ? 1f : 0f, 0.32f, frame);
            float active = activeTool == null && activeCategory == i ? 1f : 0f;
            float emphasis = Math.max(navHover[i], active);
            int iconColor = RenderUtil.lerpColor(RenderUtil.withAlpha(TEXT_DIM, 0.86f * alpha),
                    RenderUtil.withAlpha(ACCENT_LIGHT, alpha), emphasis);
            int textColor = RenderUtil.lerpColor(RenderUtil.withAlpha(TEXT_GRAY, 0.85f * alpha),
                    RenderUtil.withAlpha(TEXT_WHITE, alpha), emphasis);
            String categoryIcon = switch (categories[i]) {
                case VISUAL -> ICON_VISUAL;
                case UTILS -> ICON_UTILS;
                case CLIENT -> ICON_CLIENT;
            };
            CustomFontRenderer.drawCenteredText(context, categoryIcon, windowX + 22f * scale,
                    y + (rowH - 11f * scale) * 0.5f, 11f * scale, iconColor, FontWeight.CLICK_GUI);
            CustomFontRenderer.drawText(context, categories[i].getDisplayName(), windowX + 35f * scale,
                    y + 8.6f * scale, 7.2f * scale, textColor, FontWeight.SEMIBOLD);
        }

        float separatorY = windowY + 132f * scale;
        RenderUtil.drawRect(context, windowX + 14f * scale, separatorY, sw - 28f * scale, 0.6f,
                RenderUtil.withAlpha(0xFFFFFF, 0.065f * alpha));

        float toolsY = windowY + NAV_TOOLS_Y * scale;
        float toolRowH = NAV_TOOL_ROW_H * scale;
        List<IntegratedToolPage.Type> visibleTools = visibleTools();
        for (int i = 0; i < visibleTools.size(); i++) {
            IntegratedToolPage.Type visibleTool = visibleTools.get(i);
            renderSidebarAction(context, mx, my, i + 3, toolsY + i * toolRowH, visibleTool.icon(),
                    visibleTool.title(), visibleTool.iconWeight(), activeTool == visibleTool, alpha);
        }
        if (SHOW_EVENTS_TAB) {
            renderSidebarAction(context, mx, my, visibleTools.size() + 3,
                    toolsY + visibleTools.size() * toolRowH, "\uF000", "Ивенты",
                    FontWeight.EVENT, false, alpha);
        }

        float profileH = 25f * scale;
        float profileX = windowX + 10f * scale;
        float profileY = windowY + windowH - profileH - 9f * scale;
        float profileW = sw - 20f * scale;
        RenderUtil.drawRoundedRect(context, profileX, profileY, profileW, profileH, 7f * scale,
                RenderUtil.withAlpha(CARD_BG, 0.96f * alpha));
        float profileRadius = Math.max(0f, 7f * scale - 0.5f * scale);
        RenderUtil.drawRoundedGradient(context, profileX + 0.5f * scale, profileY + 0.5f * scale,
                profileW - scale, profileH - scale, profileRadius, profileRadius, 0f, 0f,
                RenderUtil.withAlpha(0x815FD3, 0.052f * alpha),
                RenderUtil.withAlpha(0x815FD3, 0.052f * alpha),
                RenderUtil.withAlpha(0x815FD3, 0f),
                RenderUtil.withAlpha(0x815FD3, 0f));
        float avatar = 15f * scale;
        if (client != null && client.player != null) {
            Draw.imagePlayerHeadRounded(context, client.player.getSkinTextures().texture(),
                    profileX + 5f * scale, profileY + 5f * scale, avatar, avatar * 0.5f, alpha);
        } else {
            RenderUtil.drawRoundedRect(context, profileX + 5f * scale, profileY + 5f * scale, avatar, avatar,
                    5f * scale, RenderUtil.withAlpha(ACCENT, 0.35f * alpha));
        }
        String username = client == null ? "player" : client.getSession().getUsername();
        username = CustomFontRenderer.trimToWidth(username, profileW - 43f * scale, 6.5f * scale, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, username, profileX + 25f * scale, profileY + 6.8f * scale,
                6.8f * scale, RenderUtil.withAlpha(TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, "User", profileX + 25f * scale, profileY + 14.5f * scale,
                5.7f * scale, RenderUtil.withAlpha(TEXT_DIM, 0.92f * alpha), FontWeight.REGULAR);
    }

    private void renderSidebarAction(DrawContext context, int mx, int my, int hoverIndex, float y,
                                     String icon, String name, FontWeight iconWeight, boolean active, float alpha) {
        float rowH = NAV_TOOL_ROW_H * scale;
        float sw = SIDEBAR_W * scale;
        boolean hovered = hit(mx, my, windowX + 8f * scale, y, sw - 16f * scale, rowH);
        navHover[hoverIndex] = RenderUtil.animate(navHover[hoverIndex], hovered ? 1f : 0f, 0.32f, RenderUtil.getFrameDelta());
        CustomFontRenderer.drawCenteredText(context, icon, windowX + 22f * scale,
                y + (rowH - 11f * scale) * 0.5f, 11f * scale,
                RenderUtil.lerpColor(RenderUtil.withAlpha(TEXT_DIM, 0.86f * alpha),
                        RenderUtil.withAlpha(ACCENT_LIGHT, alpha), Math.max(navHover[hoverIndex], active ? 1f : 0f)), iconWeight);
        CustomFontRenderer.drawText(context, name, windowX + 35f * scale, y + 6.6f * scale,
                7.2f * scale,
                RenderUtil.lerpColor(RenderUtil.withAlpha(TEXT_GRAY, 0.85f * alpha),
                        RenderUtil.withAlpha(TEXT_WHITE, alpha), Math.max(navHover[hoverIndex], active ? 1f : 0f)), FontWeight.SEMIBOLD);
    }

    private void renderHeader(DrawContext context, int mx, int my, float alpha, long now) {
        float x = mainX();
        float w = mainW();
        float h = HEADER_H * scale;
        RenderUtil.drawRect(context, x, windowY + h - 0.5f, w, 0.5f,
                RenderUtil.withAlpha(0xFFFFFF, 0.055f * alpha));
        float intro = intro(now, openTime + 100L, 280L);
        CustomFontRenderer.drawCenteredText(context, ICON_PUZZLE, x + 15.5f * scale,
                windowY + 10.8f * scale, 12f * scale,
                RenderUtil.withAlpha(ACCENT_LIGHT, 0.94f * alpha * intro), FontWeight.CLICK_GUI);
        boolean clientRoot = activeTool == null && categories[activeCategory] == Category.CLIENT && searchQuery.isEmpty();
        float rootX = x + 27f * scale;
        String rootName = "Клиент";
        CustomFontRenderer.drawText(context, rootName, rootX, windowY + 13.2f * scale,
                7.4f * scale, RenderUtil.withAlpha(clientRoot ? TEXT_WHITE : TEXT_DIM,
                        (clientRoot ? 1f : 0.90f) * alpha * intro), clientRoot ? FontWeight.SEMIBOLD : FontWeight.REGULAR);
        String current = !searchQuery.isEmpty() ? ClientLanguage.text("ui.search", "Поиск")
                : activeTool != null ? activeTool.title() : categories[activeCategory].getDisplayName();
        if (!clientRoot) {
            float separatorX = rootX + CustomFontRenderer.getTextWidth(rootName, 7.4f * scale,
                    clientRoot ? FontWeight.SEMIBOLD : FontWeight.REGULAR) + 9f * scale;
            CustomFontRenderer.drawText(context, ">", separatorX, windowY + 13.5f * scale,
                    7.1f * scale, RenderUtil.withAlpha(TEXT_DIM, 0.62f * alpha * intro), FontWeight.REGULAR);
            CustomFontRenderer.drawText(context, current, separatorX + 10f * scale, windowY + 13.2f * scale,
                    7.7f * scale, RenderUtil.withAlpha(TEXT_WHITE, alpha * intro), FontWeight.SEMIBOLD);
        }

        float fieldW = 118f * scale;
        float fieldH = 19f * scale;
        float fieldX = windowX + windowW - fieldW - 12f * scale;
        float fieldY = windowY + (h - fieldH) * 0.5f;
        boolean hovered = hit(mx, my, fieldX, fieldY, fieldW, fieldH);
        float focus = Math.max(searchFocus, hovered ? 0.35f : 0f);
        RenderUtil.drawRoundedRect(context, fieldX, fieldY, fieldW, fieldH, fieldH * 0.5f,
                RenderUtil.withAlpha(INSET_BG, (0.82f + 0.10f * focus) * alpha));
        CustomFontRenderer.drawCenteredText(context, ICON_SEARCH, fieldX + 13f * scale,
                fieldY + (fieldH - 9f * scale) * 0.5f, 9f * scale,
                RenderUtil.withAlpha(searchActive ? ACCENT_LIGHT : TEXT_DIM, 0.88f * alpha), FontWeight.CLICK_GUI);
        float textX = fieldX + 25f * scale;
        float textY = fieldY + (fieldH - 6.7f * scale) * 0.5f;
        if (searchQuery.isEmpty()) {
            String placeholder = activeTool == null
                    ? ClientLanguage.text("ui.search_modules", "Поиск модулей")
                    : ClientLanguage.text("ui.search", "Поиск...");
            CustomFontRenderer.drawText(context, placeholder, textX, textY,
                    6.7f * scale, RenderUtil.withAlpha(TEXT_DIM, 0.86f * alpha), FontWeight.SEMIBOLD);
        } else {
            String visible = CustomFontRenderer.trimToWidth(searchQuery, fieldW - 37f * scale, 6.7f * scale, FontWeight.SEMIBOLD);
            RenderUtil.drawAnimatedInputText(context, visible, textX, textY, 6.7f * scale,
                    RenderUtil.withAlpha(TEXT_WHITE, alpha), FontWeight.SEMIBOLD, typedCharIndex, typedCharStartMs);
        }
        if (searchActive && (now / 500L) % 2L == 0L) {
            float caret = textX + CustomFontRenderer.getTextWidth(searchQuery, 6.7f * scale, FontWeight.SEMIBOLD) + 1f;
            caret = Math.min(caret, fieldX + fieldW - 9f * scale);
            RenderUtil.drawRect(context, caret, fieldY + 5.5f * scale, 0.7f, fieldH - 11f * scale,
                    RenderUtil.withAlpha(TEXT_WHITE, 0.9f * alpha));
        }
    }

    private void renderContent(DrawContext context, int mx, int my, float alpha, long now) {
        float x = contentX();
        float y = contentY();
        float w = contentW();
        float h = contentH();
        if (activeTool != null) {
            maxScroll = 0f;
            scrollbarAlpha = 0f;
            toolPage.render(context, x, y, w, h, scale, mx, my, alpha, searchQuery);
            return;
        }
        float colW = colWidth();
        float[] colY = {0f, 0f};

        context.draw();
        StencilUtil.enableScissor(x - 2f * scale, y - 3f * scale, w + 4f * scale, h + 6f * scale);
        for (ModuleCard card : cards) {
            int column = card.column;
            float cardX = x + column * (colW + GAP * scale);
            float cardY = y + colY[column] + scrollOffset;
            float cardH = card.currentHeight(scale);
            card.setPosition(cardX, cardY, colW);
            colY[column] += cardH + GAP * scale;
            if (cardY + cardH < y - 28f * scale || cardY > y + h + 28f * scale) {
                card.tickOffscreen();
                continue;
            }
            card.render(context, scale, mx, my, alpha, now);
        }
        context.draw();
        StencilUtil.disableScissor();

        float tallest = Math.max(colY[0], colY[1]);
        maxScroll = Math.max(0f, tallest - h - GAP * scale + 3f * scale);
        scrollTarget = Math.max(-maxScroll, Math.min(0f, scrollTarget));

        if (cards.isEmpty()) {
            CustomFontRenderer.drawCenteredText(context, ClientLanguage.text("ui.nothing_found", "Ничего не найдено"),
                    x + w * 0.5f, y + h * 0.42f, 7f * scale,
                    RenderUtil.withAlpha(TEXT_DIM, 0.85f * alpha), FontWeight.REGULAR);
        }
        if (maxScroll > 0.5f && scrollbarAlpha > 0.02f) {
            float thumbH = Math.max(20f * scale, h * h / (h + maxScroll));
            float thumbY = y + (-scrollOffset / maxScroll) * (h - thumbH);
            RenderUtil.drawRoundedRect(context, windowX + windowW - 4f * scale, thumbY,
                    2f * scale, thumbH, scale, RenderUtil.withAlpha(ACCENT, 0.42f * alpha * scrollbarAlpha));
        }
    }

    private void renderHint(DrawContext context, float alpha, long now) {
        String desired = null;
        boolean binding = bindingModule != null || bindingSetting != null;
        if (binding) {
            String target = bindingModule != null ? bindingModule.getName() : bindingSetting.getName();
            desired = ClientLanguage.text("ui.bind", "Бинд") + ": " + target;
        } else {
            descriptionCandidate = null;
        }
        if (desired != null && !desired.equals(shownDescription)) {
            shownDescription = desired;
            descriptionAnimation = Math.min(descriptionAnimation, 0.10f);
        }
        descriptionAnimation = RenderUtil.animate(descriptionAnimation, desired == null ? 0f : 1f,
                desired == null ? 0.16f : 0.25f, RenderUtil.getFrameDelta());
        if (shownDescription == null || descriptionAnimation < 0.02f) {
            if (desired == null) shownDescription = null;
            return;
        }
        String draw = shownDescription;
        float size = 6.7f * scale;
        float tw = CustomFontRenderer.getTextWidth(draw, size, FontWeight.REGULAR);
        float pw = Math.min(windowW - 40f * scale, tw + 20f * scale);
        float ph = 16f * scale;
        float px = windowX + (windowW - pw) * 0.5f;
        float eased = RenderUtil.easeOutCubic(descriptionAnimation);
        float py = windowY + windowH + 7f * scale + (1f - eased) * 8f * scale;
        float a = alpha * eased;
        GlassUtil.drawGlassPanel(context, px, py, pw, ph, ph * 0.5f, WINDOW_BG & 0xFFFFFF, 0.88f * a, a * a);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        draw = CustomFontRenderer.trimToWidth(draw, pw - 12f * scale, size, FontWeight.REGULAR);
        CustomFontRenderer.drawCenteredText(context, draw, px + pw * 0.5f, py + (ph - size) * 0.5f,
                size, RenderUtil.withAlpha(TEXT_WHITE, a), FontWeight.REGULAR);
    }

    public void hoverDescription(String description) {
        hoverDescription = description;
    }

    public boolean isBindingModule(Module module) { return bindingModule == module; }
    public boolean isBindingSetting(BindSetting setting) { return bindingSetting == setting; }

    public void requestModuleBind(Module module) {
        bindingModule = module;
        bindingSetting = null;
    }

    public void requestSettingBind(BindSetting setting) {
        bindingSetting = setting;
        bindingModule = null;
    }

    public void openColorPicker(ColorSetting setting, float anchorX, float anchorY) {
        if (picker != null && picker.getSetting() == setting && !picker.isClosing()) {
            picker.close();
            return;
        }
        picker = new ColorPickerPopup(setting, anchorX, anchorY, virtualW, virtualH);
    }

    public boolean isPickerOpenFor(ColorSetting setting) {
        return picker != null && picker.getSetting() == setting && !picker.isClosing();
    }

    public void setFocusedCard(ModuleCard card) {
        if (focusedCard != null && focusedCard != card) focusedCard.clearFocus();
        focusedCard = card;
    }

    public void toggleExpanded(ModuleCard card) {
        if (expandedCard == card) {
            card.setExpanded(false);
            expandedCard = null;
        } else {
            if (expandedCard != null) expandedCard.setExpanded(false);
            expandedCard = card;
            card.setExpanded(true);
            setFocusedCard(card);
        }
        if (picker != null) picker.close();
        EnoughSoundPlayer.playScroll();
    }

    private void selectCategory(int index) {
        if (index < 0 || index >= categories.length
                || contentTransitionPhase != 0
                || activeTool == null && index == activeCategory && searchQuery.isEmpty()) return;
        contentTransitionDirection = activeTool != null ? -1 : (Integer.compare(index, activeCategory) == 0 ? 1 : Integer.compare(index, activeCategory));
        pendingCategory = index;
        pendingTool = null;
        searchQuery = "";
        searchActive = false;
        contentTransitionPhase = 1;
        contentTransitionAt = System.currentTimeMillis();
        EnoughSoundPlayer.playScroll();
    }

    private void selectTool(IntegratedToolPage.Type type) {
        if (type == null || !isToolVisible(type) || contentTransitionPhase != 0 || activeTool == type) return;
        contentTransitionDirection = 1;
        pendingTool = type;
        pendingCategory = -1;
        searchQuery = "";
        searchActive = false;
        contentTransitionPhase = 1;
        contentTransitionAt = System.currentTimeMillis();
        EnoughSoundPlayer.playScroll();
    }

    private void updateContentTransition(long now) {
        if (contentTransitionPhase == 1 && now - contentTransitionAt >= 145L) {
            closeCards();
            if (pendingTool != null) {
                activeTool = pendingTool;
                toolPage.activate(pendingTool);
            } else if (pendingCategory >= 0) {
                activeTool = null;
                activeCategory = pendingCategory;
                // The content transition already supplies the horizontal entrance. Keeping
                // card intros completed avoids a second, unrelated vertical reveal.
                rebuildCards(contentTransitionDirection, now - 5000L);
            }
            scrollOffset = scrollTarget = 0f;
            pendingTool = null;
            pendingCategory = -1;
            contentTransitionPhase = 2;
            contentTransitionAt = now;
        } else if (contentTransitionPhase == 2 && now - contentTransitionAt >= 365L) {
            contentTransitionPhase = 0;
        }
    }

    private void closeCards() {
        if (expandedCard != null) expandedCard.setExpanded(false);
        expandedCard = null;
        setFocusedCard(null);
        if (picker != null) picker.close();
    }

    private float navTargetBaseY() {
        return activeTool == null
                ? NAV_CATEGORY_Y + activeCategory * NAV_ROW_H
                : NAV_TOOLS_Y + Math.max(0, visibleTools().indexOf(activeTool)) * NAV_TOOL_ROW_H;
    }

    private static boolean isToolVisible(IntegratedToolPage.Type type) {
        return !HolyWorldFeatureControl.getInstance().isToolBlocked(type.featureId());
    }

    private static List<IntegratedToolPage.Type> visibleTools() {
        return Arrays.stream(IntegratedToolPage.Type.values())
                .filter(MenuScreen::isToolVisible)
                .toList();
    }

    private int indexOf(Category category) {
        for (int i = 0; i < categories.length; i++) if (categories[i] == category) return i;
        return 0;
    }

    private void scrollToCard(ModuleCard target) {
        float[] columns = {0f, 0f};
        for (ModuleCard card : cards) {
            if (card == target) {
                float desired = -(columns[card.column] - contentH() * 0.18f);
                scrollTarget = Math.max(-Math.max(maxScroll, columns[card.column]), Math.min(0f, desired));
                return;
            }
            columns[card.column] += card.currentHeight(scale) + GAP * scale;
        }
    }

    private void focusSearchResult() {
        if (activeTool != null || cards.isEmpty()) return;
        ModuleCard target = cards.get(0);
        Module module = target.getModule();
        activeCategory = indexOf(module.getCategory());
        searchQuery = "";
        searchActive = false;
        closeCards();
        rebuildCards(0, System.currentTimeMillis() + 20L);
        target = cardCache.get(module);
        expandedCard = target;
        target.setExpanded(true);
        setFocusedCard(target);
        scrollToCard(target);
        target.highlight();
    }

    private void startClosing() {
        if (closing) return;
        StencilUtil.resetRenderState();
        closing = true;
        EnoughSoundPlayer.playMenuClose();
        closeStart = System.currentTimeMillis();
        bindingModule = null;
        bindingSetting = null;
        if (picker != null) picker.close();
    }

    @Override
    public void removed() {
        StencilUtil.resetRenderState();
        super.removed();
    }

    private boolean canInteract() {
        return !closing && System.currentTimeMillis() - openTime > 230L;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (bindingModule != null) {
            bindingModule.setKeyBind(isUnbindKey(key) ? BindKeyUtil.NONE : key);
            bindingModule = null;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (bindingSetting != null) {
            if (isUnbindKey(key)) bindingSetting.unbind(); else bindingSetting.set(key);
            bindingSetting = null;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (hasControlDown() && key == GLFW.GLFW_KEY_F) {
            searchActive = true;
            return true;
        }
        if (searchActive) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchActive = false;
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    if (activeTool == null) rebuildCards(0, System.currentTimeMillis() - 100L);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                focusSearchResult();
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    typedCharIndex = -1;
                    if (activeTool == null) rebuildCards(0, System.currentTimeMillis() - 90L);
                    EnoughSoundPlayer.playScroll();
                }
                return true;
            }
        }
        if (picker != null && !picker.isClosing() && key == GLFW.GLFW_KEY_ESCAPE) {
            picker.close();
            return true;
        }
        if (activeTool != null && toolPage.keyPressed(key)) return true;
        if (focusedCard != null && focusedCard.keyPressed(key)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            startClosing();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchActive) {
            if (!Character.isISOControl(chr) && searchQuery.length() < 40) {
                typedCharIndex = searchQuery.length();
                typedCharStartMs = System.currentTimeMillis();
                searchQuery += chr;
                if (activeTool == null) rebuildCards(0, System.currentTimeMillis() - 90L);
                EnoughSoundPlayer.playScroll();
            }
            return true;
        }
        if (!canInteract()) return true;
        if (activeTool != null && toolPage.charTyped(chr)) return true;
        if (focusedCard != null && focusedCard.charTyped(chr)) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (bindingModule != null) {
            bindingModule.setKeyBind(BindKeyUtil.mouseButtonToCode(button));
            bindingModule = null;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (bindingSetting != null) {
            bindingSetting.set(BindKeyUtil.mouseButtonToCode(button));
            bindingSetting = null;
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (!canInteract()) return true;
        if (picker != null && !picker.isClosing()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                picker.close();
                return true;
            }
            if (picker.mouseClicked(mx, my, button)) return true;
            picker.close();
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        }

        float sw = SIDEBAR_W * scale;
        if (hit(mx, my, windowX, windowY, sw, windowH)) {
            float rowH = NAV_ROW_H * scale;
            float categoryY = windowY + NAV_CATEGORY_Y * scale;
            for (int i = 0; i < categories.length; i++) {
                if (hit(mx, my, windowX + 8f * scale, categoryY + i * rowH, sw - 16f * scale, rowH)) {
                    selectCategory(i);
                    return true;
                }
            }
            float toolsY = windowY + NAV_TOOLS_Y * scale;
            float toolRowH = NAV_TOOL_ROW_H * scale;
            List<IntegratedToolPage.Type> types = visibleTools();
            int visibleToolCount = SHOW_EVENTS_TAB ? types.size() + 1 : types.size();
            for (int i = 0; i < visibleToolCount; i++) {
                if (hit(mx, my, windowX + 8f * scale, toolsY + i * toolRowH, sw - 16f * scale, toolRowH)) {
                    if (i < types.size()) selectTool(types.get(i));
                    return true;
                }
            }
            return true;
        }

        float fieldW = 118f * scale;
        float fieldH = 19f * scale;
        float fieldX = windowX + windowW - fieldW - 12f * scale;
        float fieldY = windowY + (HEADER_H * scale - fieldH) * 0.5f;
        if (hit(mx, my, fieldX, fieldY, fieldW, fieldH)) {
            searchActive = true;
            setFocusedCard(null);
            return true;
        }

        if (hit(mx, my, contentX() - 2f * scale, contentY() - 3f * scale,
                contentW() + 4f * scale, contentH() + 6f * scale)) {
            if (activeTool != null) {
                searchActive = false;
                return toolPage.mouseClicked(mx, my, button);
            }
            searchActive = false;
            for (ModuleCard card : cards) {
                if (card.contains(mx, my, scale)) {
                    setFocusedCard(card);
                    if (card.mouseClicked(mx, my, button, scale)) dragCard = card;
                    return true;
                }
            }
            setFocusedCard(null);
            return true;
        }
        if (!hit(mx, my, windowX, windowY, windowW, windowH)) {
            setFocusedCard(null);
            toolPage.clearFocus();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (picker != null) picker.mouseReleased();
        if (dragCard != null) {
            dragCard.mouseReleased(mx, my, button, scale);
            dragCard = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!canInteract()) return true;
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (picker != null && !picker.isClosing() && picker.mouseDragged(mx, my)) return true;
        if (dragCard != null && dragCard.mouseDragged(mx, my, scale)) return true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!canInteract()) return true;
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (hit(mx, my, contentX() - 2f * scale, contentY() - 3f * scale,
                contentW() + 4f * scale, contentH() + 6f * scale)) {
            if (activeTool != null) return toolPage.mouseScrolled(mx, my, verticalAmount);
            for (ModuleCard card : cards) {
                if (card.contains(mx, my, scale) && card.mouseScrolled(mx, my, verticalAmount, scale)) return true;
            }
            scrollTarget += (float) verticalAmount * 22f * scale;
            scrollTarget = Math.max(-maxScroll, Math.min(0f, scrollTarget));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int categoryCount(Category category) {
        int count = 0;
        for (Module module : cardCache.keySet()) if (module.getCategory() == category) count++;
        return count;
    }

    private static boolean isUnbindKey(int key) {
        return key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE;
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float intro(long now, long start, long duration) {
        return clamp((now - start) / (float) duration);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
