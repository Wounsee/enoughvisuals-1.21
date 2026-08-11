package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContextMenu {

    private enum View { NONE, ADD, SETTINGS }

    private static final float ADD_WIDTH = 112f;
    private static final float SETTINGS_WIDTH = 122f;
    private static final float HEADER_H = 21f;
    private static final float FOOTER_H = 21f;
    private static final float PADDING = 5f;
    private static final float RADIUS = 9f;
    private static final float MAX_CONTENT_H = 116f;
    private static final float ADD_ROW_H = 17f;
    private static final float BOOL_ROW_H = 16f;
    private static final float MODE_HEADER_H = 14.5f;
    private static final float MODE_OPT_H = 12.5f;
    private static final float MODE_LIST_PAD = 1.5f;
    private static final float SLIDER_ROW_H = 21f;
    private static final float TOGGLE_W = 18f;
    private static final float TOGGLE_H = 9f;

    private static final HashMap<Setting<?>, Float> VISUAL_Y = new HashMap<>();
    private static final HashMap<Setting<?>, Float> TOGGLE_ANIMS = new HashMap<>();
    private static final HashMap<Setting<?>, Float> SLIDER_ANIMS = new HashMap<>();
    private static final HashMap<Setting<?>, Float> SLIDER_VELOCITIES = new HashMap<>();
    private static final HashMap<Setting<?>, Float> MODE_ANIMS = new HashMap<>();
    private static final HashMap<ModeSetting, Float> MODE_EXPAND_ANIMS = new HashMap<>();

    private static View view = View.NONE;
    private static boolean visible = false;
    private static float anim = 0f;

    private static float anchorX, anchorY, spawnX, spawnY;
    private static float panelX, panelY, panelW, panelH, contentY, contentH, footerY;
    private static float scrollOffset, scrollTarget, maxScroll;

    private static HudElement targetElement;
    private static SliderSetting draggingSlider;
    private static ModeSetting expandedMode;

                                
    private static Setting<?> draggingSetting = null;
    private static float dragOffsetY = 0f;

    public static void openAdd(float x, float y) {
        x = HudElement.screenToHud(x);
        y = HudElement.screenToHud(y);
        view = View.ADD; visible = true;
        anchorX = x; anchorY = y; spawnX = x; spawnY = y;
        targetElement = null; draggingSlider = null; draggingSetting = null;
        scrollOffset = 0f; scrollTarget = 0f;
    }

    public static void openSettings(HudElement element) {
        if (element == null) return;
        view = View.SETTINGS; visible = true;
        anchorX = element.getX() + element.getWidth() + 6f; anchorY = element.getVisualY();
        targetElement = element;
        draggingSlider = null; draggingSetting = null; expandedMode = null;
        scrollOffset = 0f; scrollTarget = 0f;
        VISUAL_Y.clear();
    }

    public static void close() {
        visible = false;
        draggingSlider = null; draggingSetting = null; expandedMode = null;
    }

    public static boolean isVisible() {
        return visible || anim > 0.01f;
    }

    public static void render(DrawContext ctx, int mx, int my, float delta) {
        int hudMx = Math.round(HudElement.screenToHud(mx));
        int hudMy = Math.round(HudElement.screenToHud(my));
        float speed = RenderUtil.getFrameDelta();
        anim = RenderUtil.animate(anim, visible ? 1f : 0f, 0.24f, speed);

        if (anim <= 0.01f) {
            if (!visible) {
                view = View.NONE; targetElement = null;
                VISUAL_Y.clear(); TOGGLE_ANIMS.clear(); SLIDER_ANIMS.clear(); SLIDER_VELOCITIES.clear();
                MODE_ANIMS.clear(); MODE_EXPAND_ANIMS.clear(); expandedMode = null;
            }
            return;
        }

        if (view == View.SETTINGS && (targetElement == null || targetElement.isRemoving())) {
            close(); return;
        }

        layout();
        float alpha = RenderUtil.easeOutQuad(anim);
        float scale = 0.92f + 0.08f * RenderUtil.easeOutBack(anim);
        float hudScale = HudElement.hudScale();

        ctx.getMatrices().push();
        ctx.getMatrices().scale(hudScale, hudScale, 1f);
        float previousScissorScale = HudElement.setActiveScissorScale(hudScale);
        ctx.getMatrices().translate(panelX + panelW * 0.5f, panelY + panelH * 0.5f, 420f);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-(panelX + panelW * 0.5f), -(panelY + panelH * 0.5f), 0f);

        GlassUtil.BLUR_OFFSET = CustomTheme.hudBlurLevel();
        GlassUtil.drawGlassPanel(ctx, panelX, panelY, panelW, panelH, RADIUS, CustomTheme.hudPanelTint(),
                alpha * CustomTheme.hudPanelTintAlpha(), alpha * alpha);
        renderHeader(ctx, alpha);

        float previousScroll = scrollOffset;
        scrollOffset = RenderUtil.animate(scrollOffset, scrollTarget, 0.22f, speed);
        float scrollDelta = scrollOffset - previousScroll;
        // Row springs must follow the scroll viewport immediately. Letting them
        // animate the same offset a second time made the last setting briefly
        // lag behind the scissor and look as if its plate disappeared.
        if (view == View.SETTINGS && Math.abs(scrollDelta) > 0.0001f) {
            VISUAL_Y.replaceAll((setting, visualY) -> visualY + scrollDelta);
        }

        StencilUtil.enableScissor(panelX, contentY, panelW, contentH);
        if (view == View.ADD) renderAddContent(ctx, hudMx, hudMy, alpha);
        else if (view == View.SETTINGS) renderSettingsContent(ctx, hudMx, hudMy, alpha, speed);
        StencilUtil.disableScissor();

        if (view == View.SETTINGS) renderFooter(ctx, hudMx, hudMy, alpha);

        HudElement.setActiveScissorScale(previousScissorScale);
        ctx.getMatrices().pop();
    }

    public static boolean mouseClicked(float mx, float my, int button) {
        if (!isVisible()) return false;
        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        layout();
        if (!contains(mx, my)) { close(); return true; }
        if (view == View.ADD) return handleAddClick(mx, my);
        if (view == View.SETTINGS) return handleSettingsClick(mx, my);
        return true;
    }

    public static boolean mouseReleased(float mx, float my, int button) {
        if (!isVisible()) return false;
        draggingSlider = null;
        draggingSetting = null;
        return view == View.SETTINGS;
    }

    public static boolean mouseDragged(float mx, float my, int button) {
        if (!isVisible() || view != View.SETTINGS || button != 0) return false;
        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        if (draggingSlider != null) {
            float trackX = panelX + 8f;
            float trackW = panelW - 16f;
            draggingSlider.setFromProgress(clamp((mx - trackX) / trackW, 0f, 1f));
            return true;
        }
        return draggingSetting != null;
    }

    public static boolean mouseScrolled(float mx, float my, double amount) {
        if (!isVisible()) return false;
        mx = HudElement.screenToHud(mx);
        my = HudElement.screenToHud(my);
        layout();
        if (!contains(mx, my) || maxScroll <= 0f) return false;

        scrollTarget += (float) (amount * 14f);
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
        if (Math.abs(amount) > 0.0001) EnoughSoundPlayer.playScroll();
        return true;
    }

    private static void layout() {
        MinecraftClient client = MinecraftClient.getInstance();
        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();

        if (view == View.SETTINGS && targetElement != null) {
            // Follow the transient visual position while the chat inset opens
            // or closes; the persisted element coordinates remain untouched.
            anchorY = targetElement.getVisualY();
        }

        if (view == View.ADD) {
            panelW = ADD_WIDTH;
            float totalContent = getAddEntries().size() * ADD_ROW_H + PADDING * 2f;
            contentH = Math.min(MAX_CONTENT_H, Math.max(16f, totalContent));
            panelH = HEADER_H + contentH;
            maxScroll = Math.max(0f, totalContent - contentH);
        } else if (view == View.SETTINGS) {
            panelW = SETTINGS_WIDTH;
            float bodyContent = getSettingsTotalHeight();
            boolean hasSettings = bodyContent > 0.5f;
            float totalContent = hasSettings ? bodyContent + PADDING * 2f : 0f;
            contentH = hasSettings ? Math.min(MAX_CONTENT_H, totalContent) : 4f;
            float footer = targetElement != null && targetElement.isRemovable() ? FOOTER_H : 0f;
            panelH = HEADER_H + contentH + footer;
            maxScroll = hasSettings ? Math.max(0f, totalContent - contentH) : 0f;
        } else {
            panelW = 0f; panelH = 0f; return;
        }

        panelX = clamp(anchorX, 6f, screenW - panelW - 6f);
        panelY = clamp(anchorY, 6f, screenH - panelH - 6f);
        contentY = panelY + HEADER_H;
        footerY = panelY + panelH - FOOTER_H;
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
    }

    private static void renderHeader(DrawContext ctx, float alpha) {
        String title = view == View.ADD ? "HUD элементы" : "Настройки";
        CustomFontRenderer.drawText(ctx, title, panelX + 7f, panelY + 6.2f, 7.1f,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
        RenderUtil.drawRect(ctx, panelX + 5f, panelY + HEADER_H - 0.5f, panelW - 10f, 0.5f,
                RenderUtil.withAlpha(0xFFFFFF, alpha * 0.07f));
    }

    private static void renderAddContent(DrawContext ctx, int mx, int my, float alpha) {
        List<DragAndDrop.HudEntry> entries = getAddEntries();
        float rowY = contentY + PADDING + scrollOffset;

        if (entries.isEmpty()) {
            CustomFontRenderer.drawCenteredText(ctx, "Свободных нет", panelX + panelW * 0.5f,
                    contentY + contentH * 0.5f - 3.2f, 6.1f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.95f), FontWeight.REGULAR);
            return;
        }

        for (DragAndDrop.HudEntry entry : entries) {
            boolean hovered = mx >= panelX && mx <= panelX + panelW && my >= rowY && my <= rowY + ADD_ROW_H;
            if (hovered) RenderUtil.drawRoundedRect(ctx, panelX + 3f, rowY, panelW - 6f, ADD_ROW_H - 1f, 5f, RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * 0.12f));
            CustomFontRenderer.drawText(ctx, entry.localizedName(), panelX + 7f, rowY + 5.1f, 6.5f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * (hovered ? 1f : 0.92f)), FontWeight.REGULAR);
            rowY += ADD_ROW_H;
        }
    }

    private static class RenderNode {
        Setting<?> setting; float targetY; float height;
        RenderNode(Setting<?> s, float ty, float h) { setting = s; targetY = ty; height = h; }
    }

    private static float getSettingHeight(Setting<?> setting) {
        if (setting instanceof BooleanSetting) return BOOL_ROW_H;
        if (setting instanceof ModeSetting m) return getModeTotalHeight(m, MODE_EXPAND_ANIMS.getOrDefault(m, expandedMode == m ? 1f : 0f));
        if (setting instanceof SliderSetting) return SLIDER_ROW_H;
        return 0f;
    }

    private static void renderSettingsContent(DrawContext ctx, int mx, int my, float alpha, float speed) {
        if (targetElement == null) return;

        List<Setting<?>> settings = targetElement.getVisibleSettings();
        float currentY = contentY + PADDING + scrollOffset;
        List<RenderNode> nodes = new ArrayList<>();

        for (Setting<?> setting : settings) {
            float h = getSettingHeight(setting);
            nodes.add(new RenderNode(setting, currentY, h));
            currentY += h;
        }

        if (draggingSetting != null) {
            float draggedCenter = (my - dragOffsetY) + getSettingHeight(draggingSetting) / 2f;
            int currentIndex = targetElement.getSettings().indexOf(draggingSetting);

            for (int i = 0; i < nodes.size(); i++) {
                RenderNode other = nodes.get(i);
                if (other.setting != draggingSetting && draggedCenter > other.targetY && draggedCenter < other.targetY + other.height) {
                    targetElement.moveSetting(draggingSetting, i - currentIndex);
                    break;
                }
            }

            currentY = contentY + PADDING + scrollOffset;
            for (Setting<?> setting : targetElement.getVisibleSettings()) {
                float h = getSettingHeight(setting);
                for (RenderNode node : nodes) {
                    if (node.setting == setting) { node.targetY = currentY; break; }
                }
                currentY += h;
            }
        }

        for (RenderNode node : nodes) {
            if (node.setting == draggingSetting) continue;
            float visualY = VISUAL_Y.getOrDefault(node.setting, node.targetY);
            visualY = RenderUtil.animate(visualY, node.targetY, 0.40f, speed);
            VISUAL_Y.put(node.setting, visualY);
            renderSpecificSetting(ctx, node.setting, visualY, mx, my, alpha, speed);
        }

        if (draggingSetting != null) {
            for (RenderNode node : nodes) {
                if (node.setting == draggingSetting) {
                    float visualY = my - dragOffsetY;
                    visualY = clamp(visualY, contentY, contentY + contentH - node.height);
                    VISUAL_Y.put(node.setting, visualY);

                    RenderUtil.drawRoundedRect(ctx, panelX + 3f, visualY, panelW - 6f, node.height - 0.5f, 4.5f, RenderUtil.withAlpha(0x000000, alpha * 0.45f));
                    renderSpecificSetting(ctx, node.setting, visualY, mx, my, alpha, speed);
                    break;
                }
            }
        }
    }

    private static void renderSpecificSetting(DrawContext ctx, Setting<?> setting, float y, int mx, int my, float alpha, float speed) {
        if (setting instanceof BooleanSetting booleanSetting) {
            float anim = TOGGLE_ANIMS.getOrDefault(setting, booleanSetting.get() ? 1f : 0f);
            anim = RenderUtil.animate(anim, booleanSetting.get() ? 1f : 0f, 0.17f, speed);
            TOGGLE_ANIMS.put(setting, anim);
            renderBoolean(ctx, booleanSetting, y, mx, my, alpha, anim);
        } else if (setting instanceof ModeSetting modeSetting) {
            float hoverAnim = MODE_ANIMS.getOrDefault(setting, 0f);
            float hoverTarget = hoveredRow(mx, my, y, MODE_HEADER_H) ? 1f : 0f;
            hoverAnim = RenderUtil.animate(hoverAnim, hoverTarget, 0.22f, speed);
            MODE_ANIMS.put(setting, hoverAnim);
            float expandAnim = MODE_EXPAND_ANIMS.getOrDefault(modeSetting, 0f);
            float expandTarget = expandedMode == modeSetting ? 1f : 0f;
            expandAnim = RenderUtil.animate(expandAnim, expandTarget, 0.14f, speed);
            MODE_EXPAND_ANIMS.put(modeSetting, expandAnim);
            renderMode(ctx, modeSetting, y, alpha, hoverAnim, expandAnim, mx, my);
        } else if (setting instanceof SliderSetting sliderSetting) {
            float prev = SLIDER_ANIMS.getOrDefault(setting, sliderSetting.getProgress());
            float visual = RenderUtil.animate(prev, sliderSetting.getProgress(), 0.26f, speed);
            SLIDER_ANIMS.put(setting, visual);
            float velocity = RenderUtil.animate(SLIDER_VELOCITIES.getOrDefault(setting, 0f), (visual - prev) * 50f, 0.30f, speed);
            SLIDER_VELOCITIES.put(setting, velocity);
            renderSlider(ctx, sliderSetting, y, alpha, visual, velocity);
        }
    }

    private static void renderFooter(DrawContext ctx, int mx, int my, float alpha) {
        if (targetElement == null || !targetElement.isRemovable()) return;
        float buttonX = panelX + 5f, buttonY = footerY + 3f, buttonW = panelW - 10f, buttonH = 14.5f;
        boolean hovered = mx >= buttonX && mx <= buttonX + buttonW && my >= buttonY && my <= buttonY + buttonH;
        RenderUtil.drawRect(ctx, panelX + 5f, footerY - 0.5f, panelW - 10f, 0.5f, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.07f));
        RenderUtil.drawRoundedRect(ctx, buttonX, buttonY, buttonW, buttonH, 4.5f, RenderUtil.withAlpha(0x000000, alpha * (hovered ? 0.42f : 0.30f)));
        CustomFontRenderer.drawText(ctx, SolarIcons.DELETE, buttonX + 5f, buttonY + 3.8f,
                6.9f, RenderUtil.withAlpha(0xFFFF6F87, alpha * 0.95f), FontWeight.SOLAR_ICONS);
        CustomFontRenderer.drawText(ctx, "Удалить", buttonX + 16f, buttonY + 4f, 6.2f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.REGULAR);
    }

    private static void renderBoolean(DrawContext ctx, BooleanSetting setting, float y, int mx, int my, float alpha, float anim) {
        boolean hovered = mx >= panelX + 4f && mx <= panelX + panelW - 4f && my >= y && my <= y + BOOL_ROW_H;
        if (hovered && draggingSetting != setting) {
            RenderUtil.drawRoundedRect(ctx, panelX + 3f, y, panelW - 6f, BOOL_ROW_H - 0.5f, 4.5f, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.03f));
        }

        float textX = panelX + 7f;
        CustomFontRenderer.drawText(ctx, SolarIcons.DRAG, textX, y + 5.5f, 6.0f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.5f), FontWeight.SOLAR_ICONS);
        textX += 10f;

        CustomFontRenderer.drawText(ctx, setting.getName(), textX, y + 4.9f, 6.2f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.94f), FontWeight.REGULAR);
        renderToggle(ctx, panelX + panelW - 25f, y + 3.5f, anim, setting.get(), alpha);
    }

    private static void renderMode(DrawContext ctx, ModeSetting setting, float y, float alpha, float hoverAnim, float expandAnim, int mx, int my) {
        if (hoverAnim > 0.01f) RenderUtil.drawRoundedRect(ctx, panelX + 3f, y, panelW - 6f,
                MODE_HEADER_H - 0.5f, 6.5f,
                RenderUtil.withAlpha(0xFFFFFF, alpha * (0.012f + 0.018f * hoverAnim)));
        CustomFontRenderer.drawText(ctx, setting.getName(), panelX + 7f, y + 4.2f, 6.1f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.94f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, setting.get(), panelX + panelW - 16f, y + 4.2f, 5.8f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.95f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, expandAnim > 0.01f ? SolarIcons.CHEVRON_DOWN : SolarIcons.CHEVRON_RIGHT,
                panelX + panelW - 8f, y + 4.15f, 6.0f,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.90f), FontWeight.SOLAR_ICONS);

        if (expandAnim <= 0.01f) return;
        float optionsY = y + MODE_HEADER_H + MODE_LIST_PAD, optionsH = setting.getModes().size() * MODE_OPT_H, visibleH = optionsH * expandAnim;
        StencilUtil.enableScissor(panelX + 4f, optionsY, panelW - 8f, visibleH);
        RenderUtil.drawRoundedRect(ctx, panelX + 4f, optionsY, panelW - 8f, optionsH + 1.5f, 6f, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.022f));
        float optionY = optionsY + 1f;
        for (String option : setting.getModes()) {
            boolean selected = option.equals(setting.get()), hover = mx >= panelX + 5f && mx <= panelX + panelW - 5f && my >= optionY && my <= optionY + MODE_OPT_H;
            if (selected || hover) RenderUtil.drawRoundedRect(ctx, panelX + 5f, optionY, panelW - 10f,
                    MODE_OPT_H - 0.6f, 5f,
                    RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * (selected ? 0.18f : 0.08f)));
            CustomFontRenderer.drawText(ctx, setting.getDisplayMode(option), panelX + 8f, optionY + 3.2f, 5.8f, RenderUtil.withAlpha(selected ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM, alpha * 0.98f), FontWeight.REGULAR);
            optionY += MODE_OPT_H;
        }
        StencilUtil.disableScissor();
    }

    private static void renderSlider(DrawContext ctx, SliderSetting setting, float y, float alpha, float visual, float velocity) {
        CustomFontRenderer.drawText(ctx, setting.getName(), panelX + 7f, y + 2f, 6.2f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.94f), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(ctx, setting.getDisplayValue(), panelX + panelW - 7f, y + 2f, 5.6f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.95f), FontWeight.REGULAR);
        float trackX = panelX + 8f, trackY = y + 14.5f, trackW = panelW - 16f, progress = clamp(visual, 0f, 1f);
        RenderUtil.drawRoundedRect(ctx, trackX, trackY, trackW, 3f, 1.5f, RenderUtil.withAlpha(MenuScreen.SLIDER_BG, alpha * 0.60f));
        float fillW = trackW * progress;
        if (fillW > 0.5f) RenderUtil.drawRoundedRect(ctx, trackX, trackY, fillW, 3f, 1.5f, RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        float knobX = trackX + fillW, knobY = trackY + 1.5f;
        RenderUtil.drawCircle(ctx, knobX, knobY + 0.5f, 4f, RenderUtil.withAlpha(0x000000, alpha * 0.20f));
        RenderUtil.drawCircle(ctx, knobX, knobY, 3.5f, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private static void renderToggle(DrawContext ctx, float x, float y, float value,
                                     boolean enabled, float alpha) {
        float base = clamp(value, 0f, 1f);
        float progress = RenderUtil.easeInOutCubic(base);
        float motion = clamp(Math.abs((enabled ? 1f : 0f) - value) * 4.2f, 0f, 1f);
        int track = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, alpha * 0.90f),
                RenderUtil.withAlpha(MenuScreen.TOGGLE_ON, alpha * 0.92f), progress);
        drawCapsule(ctx, x, y, TOGGLE_W, TOGGLE_H, track);
        drawCapsuleOutline(ctx, x, y, TOGGLE_W, TOGGLE_H, 0.45f,
                RenderUtil.withAlpha(0xFFFFFFFF, alpha * 0.065f));

        float knobW = 7.25f + motion * 1.05f;
        float knobH = 5.25f - motion * 0.24f;
        float inset = 1.25f;
        float left = x + inset;
        float right = x + TOGGLE_W - inset - knobW;
        float knobX = left + (right - left) * progress;
        float knobY = y + (TOGGLE_H - knobH) * 0.5f;
        drawCapsule(ctx, knobX, knobY, knobW, knobH,
                RenderUtil.withAlpha(MenuScreen.TOGGLE_KNOB, alpha * (0.78f + progress * 0.20f)));
    }

    private static void drawCapsule(DrawContext ctx, float x, float y, float width, float height, int color) {
        float previous = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, width, height, height * 0.5f, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    private static void drawCapsuleOutline(DrawContext ctx, float x, float y, float width, float height,
                                           float lineWidth, int color) {
        float previous = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedOutline(ctx, x, y, width, height, height * 0.5f, lineWidth, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previous);
        }
    }

    private static boolean handleAddClick(float mx, float my) {
        List<DragAndDrop.HudEntry> entries = getAddEntries();
        float rowY = contentY + PADDING + scrollOffset;
        for (DragAndDrop.HudEntry entry : entries) {
            if (mx >= panelX && mx <= panelX + panelW && my >= rowY && my <= rowY + ADD_ROW_H) {
                DragAndDrop.getInstance().addElement(entry.id, spawnX, spawnY); close(); return true;
            }
            rowY += ADD_ROW_H;
        }
        return true;
    }

    private static boolean handleSettingsClick(float mx, float my) {
        if (targetElement == null) { close(); return true; }
        if (targetElement.isRemovable()) {
            float buttonX = panelX + 5f, buttonY = footerY + 3f, buttonW = panelW - 10f, buttonH = 14.5f;
            if (mx >= buttonX && mx <= buttonX + buttonW && my >= buttonY && my <= buttonY + buttonH) {
                DragAndDrop.getInstance().removeElement(targetElement); close(); return true;
            }
        }
        float rowY = contentY + PADDING + scrollOffset;
        for (Setting<?> setting : targetElement.getVisibleSettings()) {
            float h = getSettingHeight(setting);
            if (setting instanceof BooleanSetting booleanSetting) {
                if (mx >= panelX + 4f && mx <= panelX + 16f && my >= rowY && my <= rowY + h) {
                    draggingSetting = setting;
                    dragOffsetY = my - rowY;
                    return true;
                } else if (mx > panelX + 16f && mx <= panelX + panelW && my >= rowY && my <= rowY + h) {
                    booleanSetting.toggle();
                    return true;
                }
            } else if (setting instanceof ModeSetting modeSetting) {
                float expandAnim = MODE_EXPAND_ANIMS.getOrDefault(modeSetting, expandedMode == modeSetting ? 1f : 0f);
                float headerY2 = rowY, optionsY = headerY2 + MODE_HEADER_H + MODE_LIST_PAD;
                if (mx >= panelX && mx <= panelX + panelW && my >= headerY2 && my <= headerY2 + MODE_HEADER_H) { expandedMode = expandedMode == modeSetting ? null : modeSetting; return true; }
                if (expandAnim > 0.01f) {
                    float optionY = optionsY + 1f;
                    for (String option : modeSetting.getModes()) {
                        if (mx >= panelX + 5f && mx <= panelX + panelW - 5f && my >= optionY && my <= optionY + MODE_OPT_H) { modeSetting.set(option); expandedMode = null; return true; }
                        optionY += MODE_OPT_H;
                    }
                }
            } else if (setting instanceof SliderSetting sliderSetting) {
                if (mx >= panelX + 6f && mx <= panelX + panelW - 6f && my >= rowY && my <= rowY + SLIDER_ROW_H) {
                    float trackX = panelX + 8f, trackW = panelW - 16f;
                    sliderSetting.setFromProgress(clamp((mx - trackX) / trackW, 0f, 1f));
                    draggingSlider = sliderSetting; return true;
                }
            }
            rowY += h;
        }
        return true;
    }

    private static List<DragAndDrop.HudEntry> getAddEntries() {
        DragAndDrop dragAndDrop = DragAndDrop.getInstance();
        List<DragAndDrop.HudEntry> entries = new ArrayList<>();
        for (DragAndDrop.HudEntry entry : dragAndDrop.getRegistry()) if (!dragAndDrop.isElementActive(entry.id)) entries.add(entry);
        return entries;
    }

    private static float getSettingsTotalHeight() {
        if (targetElement == null) return 0f;
        float total = 0f;
        for (Setting<?> setting : targetElement.getVisibleSettings()) {
            total += getSettingHeight(setting);
        }
        return total;
    }

    private static float getModeTotalHeight(ModeSetting setting, float expandAnim) {
        return MODE_HEADER_H + (setting.getModes().size() * MODE_OPT_H + MODE_LIST_PAD + 1f) * clamp(expandAnim, 0f, 1f);
    }

    private static boolean hoveredRow(int mx, int my, float y, float h) { return mx >= panelX + 4f && mx <= panelX + panelW - 4f && my >= y && my <= y + h; }
    private static boolean contains(float mx, float my) { return mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH; }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
