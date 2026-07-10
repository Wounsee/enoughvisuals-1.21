package fun.enoughvisuals.ui.clickgui.components;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.settings.BezierSetting;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ClickSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ItemSelectSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.MultiItemSelectSetting;
import fun.enoughvisuals.systems.modules.settings.RangeSetting;
import fun.enoughvisuals.systems.modules.settings.SelectSetting;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleCard {
    private static final float HEADER_H = 41f;
    private static final float MAX_BODY_H = 188f;
    private static final String ICON_GEAR = "\uF007";
    private static final String ICON_BIND = "\uF005";

    private final Module module;
    private final MenuScreen screen;
    private final Map<Setting<?>, Float> hoverAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> valueAnimations = new HashMap<>();
    private final Map<Setting<?>, Integer> optionScroll = new HashMap<>();

    public int column;
    private float x;
    private float y;
    private float width;
    private float drawX;
    private float drawY;
    private float hover;
    private float gearHover;
    private float bindHover;
    private float toggleAnimation;
    private boolean lastEnabled;
    private long toggleChangedAt;
    private float expansionValue;
    private float expansionFrom;
    private float expansionTarget;
    private long expansionStartedAt;
    private long expansionDuration = 1L;
    private float bodyScroll;
    private float bodyScrollTarget;
    private float bodyMaxScroll;
    private boolean expanded;
    private long introStart;
    private int introDirection;
    private long highlightAt;
    private long shakeAt;

    private Setting<?> activeDrag;
    private int rangeHandle;
    private int bezierHandle;
    private StringSetting editingString;
    private Setting<?> editingChoice;
    private Setting<?> openChoice;

    public ModuleCard(Module module, MenuScreen screen) {
        this.module = module;
        this.screen = screen;
        this.toggleAnimation = module.isEnabled() ? 1f : 0f;
        this.lastEnabled = module.isEnabled();
    }

    public Module getModule() {
        return module;
    }

    public void beginIntro(long start, int direction) {
        introStart = start;
        introDirection = direction;
    }

    public void setPosition(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
        if (drawX == 0f && drawY == 0f) {
            drawX = x;
            drawY = y;
        }
    }

    public void setExpanded(boolean expanded) {
        float current = expansionValue();
        this.expanded = expanded;
        expansionFrom = current;
        expansionTarget = expanded ? 1f : 0f;
        expansionStartedAt = System.currentTimeMillis();
        expansionDuration = expanded ? 370L : 270L;
        if (!expanded) {
            clearFocus();
            openChoice = null;
            bodyScrollTarget = 0f;
        }
    }

    public void highlight() {
        highlightAt = System.currentTimeMillis();
    }

    public void clearFocus() {
        editingString = null;
        editingChoice = null;
    }

    public float currentHeight(float scale) {
        float baseWidth = Math.max(150f, (width > 0f ? width : screen.colWidth()) / Math.max(0.01f, scale) - 16f);
        float total = contentHeight(baseWidth);
        float viewport = Math.min(MAX_BODY_H, total);
        float e = Math.max(0f, expansionValue());
        return (HEADER_H + (viewport + 7f) * e) * scale;
    }

    public boolean contains(float mouseX, float mouseY, float scale) {
        return mouseX >= drawX && mouseX <= drawX + width
                && mouseY >= drawY && mouseY <= drawY + currentHeight(scale);
    }

    public void tickOffscreen() {
        expansionValue();
        bodyScroll = RenderUtil.animate(bodyScroll, bodyScrollTarget, 0.28f, RenderUtil.getFrameDelta());
    }

    public void render(DrawContext context, float scale, int mouseX, int mouseY, float parentAlpha, long now) {
        float intro = clamp((now - introStart) / 330f);
        float introEase = RenderUtil.easeOutBack(intro);
        float slide = (1f - RenderUtil.easeOutCubic(intro)) * 18f * scale * introDirection;
        drawX = x + slide;
        drawY = y + (1f - RenderUtil.easeOutCubic(intro)) * 7f * scale;
        float alpha = parentAlpha * RenderUtil.easeOutQuad(intro);
        float height = currentHeight(scale);
        boolean hovered = contains(mouseX, mouseY, scale);
        float frame = RenderUtil.getFrameDelta();
        hover = RenderUtil.animate(hover, hovered ? 1f : 0f, 0.28f, frame);
        if (lastEnabled != module.isEnabled()) {
            lastEnabled = module.isEnabled();
            toggleChangedAt = now;
        }
        toggleAnimation = RenderUtil.animate(toggleAnimation, module.isEnabled() ? 1f : 0f, 0.21f, frame);
        bodyScroll = RenderUtil.animate(bodyScroll, bodyScrollTarget, 0.27f, frame);

        float shake = 0f;
        if (now - shakeAt < 390L) {
            float p = (now - shakeAt) / 390f;
            shake = (float) Math.sin(p * Math.PI * 7f) * (1f - p) * 3.5f * scale;
        }
        drawX += shake;

        float highlight = 0f;
        if (now - highlightAt < 850L) {
            float p = (now - highlightAt) / 850f;
            highlight = (1f - RenderUtil.easeOutCubic(p)) * (0.5f + 0.5f * (float) Math.sin(p * Math.PI * 3f));
        }
        int cardColor = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.93f * alpha),
                RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.97f * alpha), hover * 0.72f);
        RenderUtil.drawRoundedRect(context, drawX, drawY, width, height, 8.5f * scale, cardColor);
        RenderUtil.drawRoundedOutline(context, drawX, drawY, width, height, 8.5f * scale, 0.7f,
                RenderUtil.withAlpha(highlight > 0.01f ? MenuScreen.ACCENT : 0xFFFFFF,
                        (0.028f + highlight * 0.36f) * alpha));

        renderHeader(context, scale, mouseX, mouseY, alpha);
        float expand = clamp(expansionValue());
        if (expand > 0.005f) renderSettings(context, scale, mouseX, mouseY, alpha * RenderUtil.easeOutQuad(expand), expand);
    }

    private void renderHeader(DrawContext context, float scale, int mouseX, int mouseY, float alpha) {
        boolean blocked = ModuleManager.getInstance().isBlocked(module);
        boolean available = !blocked && module.isAvailable();
        float headerH = HEADER_H * scale;
        float toggleW = 18f * scale;
        float toggleH = 9.2f * scale;
        float toggleX = drawX + width - 9f * scale - toggleW;
        float toggleY = drawY + 9.3f * scale;
        float gearX = toggleX - 11.8f * scale;
        float bindX = gearX - 13.2f * scale;
        gearHover = RenderUtil.animate(gearHover,
                hit(mouseX, mouseY, gearX - 5f * scale, drawY + 4f * scale, 11f * scale, 20f * scale) ? 1f : 0f,
                0.31f, RenderUtil.getFrameDelta());
        bindHover = RenderUtil.animate(bindHover,
                hit(mouseX, mouseY, bindX - 5f * scale, drawY + 4f * scale, 11f * scale, 20f * scale) ? 1f : 0f,
                0.31f, RenderUtil.getFrameDelta());

        float nameX = drawX + 10f * scale;
        float nameMax = bindX - nameX - 6f * scale;
        String name = CustomFontRenderer.trimToWidth(module.getName(), nameMax, 7.2f * scale, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, name, nameX, drawY + 6.9f * scale, 7.2f * scale,
                RenderUtil.withAlpha(available || module.isEnabled() ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM,
                        alpha * (available || module.isEnabled() ? 0.98f : 0.55f)), FontWeight.SEMIBOLD);
        String description = module.getDescription();
        description = CustomFontRenderer.trimToWidth(description, width - 20f * scale, 5.9f * scale, FontWeight.REGULAR);
        CustomFontRenderer.drawText(context, description, drawX + 10f * scale, drawY + 24.1f * scale,
                5.9f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.88f * alpha), FontWeight.REGULAR);

        boolean moduleBinding = screen.isBindingModule(module);
        CustomFontRenderer.drawCenteredText(context, ICON_BIND, bindX, drawY + 9.4f * scale, 8.5f * scale,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.55f * alpha),
                        RenderUtil.withAlpha(moduleBinding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE, alpha),
                        Math.max(bindHover, moduleBinding ? 1f : 0f)), FontWeight.CLICK_GUI);
        CustomFontRenderer.drawCenteredText(context, ICON_GEAR, gearX, drawY + 9.6f * scale, 7.8f * scale,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.62f * alpha),
                        RenderUtil.withAlpha(expanded ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE, alpha),
                        Math.max(gearHover, expanded ? 0.72f : 0f)), FontWeight.CLICK_GUI);

        int track = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, 0.95f * alpha),
                RenderUtil.withAlpha(MenuScreen.TOGGLE_ON, alpha), toggleAnimation);
        float switchProgress = clamp((System.currentTimeMillis() - toggleChangedAt) / 280f);
        float pulse = toggleChangedAt == 0L ? 0f : (float) Math.sin(switchProgress * Math.PI) * (1f - switchProgress);
        float motion = clamp(Math.abs((module.isEnabled() ? 1f : 0f) - toggleAnimation) * 4.5f);
        RenderUtil.drawRoundedRect(context, toggleX - pulse * 0.25f * scale, toggleY,
                toggleW + pulse * 0.5f * scale, toggleH, toggleH * 0.5f, track);
        float knobX = toggleX + 4.7f * scale + toggleAnimation * (toggleW - 9.4f * scale);
        float knobRx = (3.45f + motion * 1.05f + pulse * 0.28f) * scale;
        float knobRy = (3.45f - motion * 0.24f) * scale;
        RenderUtil.drawEllipse(context, knobX, toggleY + toggleH * 0.5f, knobRx, knobRy,
                RenderUtil.withAlpha(MenuScreen.TOGGLE_KNOB, alpha));
        if (moduleBinding) {
            String bind = BindKeyUtil.getBindName(module.getKeyBind());
            CustomFontRenderer.drawRightText(context, bind, bindX + 5f * scale, drawY + 18.2f * scale,
                    4.8f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.REGULAR);
        }
    }

    private void renderSettings(DrawContext context, float scale, int mouseX, int mouseY, float alpha, float expand) {
        float top = drawY + HEADER_H * scale;
        float baseWidth = width / scale - 16f;
        float total = contentHeight(baseWidth);
        float viewportBase = Math.min(MAX_BODY_H, total);
        float viewport = viewportBase * scale * expand;
        bodyMaxScroll = Math.max(0f, total - viewportBase);
        bodyScrollTarget = Math.max(-bodyMaxScroll, Math.min(0f, bodyScrollTarget));

        RenderUtil.drawRect(context, drawX + 9f * scale, top, width - 18f * scale, 0.6f,
                RenderUtil.withAlpha(0xFFFFFF, 0.06f * alpha));
        context.draw();
        StencilUtil.enableScissor(drawX + 3f * scale, top + 1f, width - 6f * scale, Math.max(1f, viewport));
        float rowX = drawX + 8f * scale;
        float rowY = top + 5f * scale + bodyScroll * scale;
        float rowW = width - 16f * scale;
        for (Setting<?> setting : module.getVisibleSettings()) {
            float rowH = rowHeight(setting, baseWidth) * scale;
            if (rowY + rowH >= top - 10f * scale && rowY <= top + viewport + 10f * scale) {
                renderSetting(context, setting, rowX, rowY, rowW, rowH, scale, mouseX, mouseY, alpha);
            }
            rowY += rowH;
        }
        context.draw();
        StencilUtil.disableScissor();

        if (bodyMaxScroll > 0.5f && expand > 0.7f) {
            float trackH = viewport - 8f * scale;
            float thumbH = Math.max(13f * scale, trackH * viewportBase / total);
            float thumbY = top + 5f * scale + (-bodyScroll / bodyMaxScroll) * (trackH - thumbH);
            RenderUtil.drawRoundedRect(context, drawX + width - 3.5f * scale, thumbY, 1.5f * scale, thumbH,
                    0.75f * scale, RenderUtil.withAlpha(MenuScreen.ACCENT, 0.38f * alpha));
        }
    }

    private void renderSetting(DrawContext context, Setting<?> setting, float x, float y, float w, float h,
                               float scale, int mouseX, int mouseY, float alpha) {
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        float hover = RenderUtil.animate(hoverAnimations.getOrDefault(setting, 0f), hovered ? 1f : 0f,
                0.30f, RenderUtil.getFrameDelta());
        hoverAnimations.put(setting, hover);
        if (hovered && !setting.getDescription().isBlank()) screen.hoverDescription(setting.getDescription());
        if (hover > 0.01f) {
            RenderUtil.drawRoundedRect(context, x, y + 1f * scale, w, h - 2f * scale, 5.5f * scale,
                    RenderUtil.withAlpha(0xFFFFFF, 0.023f * alpha * hover));
        }

        if (setting instanceof BooleanSetting value) renderBoolean(context, value, x, y, w, scale, alpha);
        else if (setting instanceof SliderSetting value) renderSlider(context, value, x, y, w, scale, alpha);
        else if (setting instanceof RangeSetting value) renderRange(context, value, x, y, w, scale, alpha);
        else if (setting instanceof ModeSetting value) renderMode(context, value, x, y, w, scale, alpha);
        else if (setting instanceof SelectSetting value) renderSelect(context, value, x, y, w, scale, alpha);
        else if (setting instanceof ItemSelectSetting value) renderItem(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof MultiItemSelectSetting value) renderMultiItem(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof ColorSetting value) renderColor(context, value, x, y, w, scale, alpha);
        else if (setting instanceof StringSetting value) renderString(context, value, x, y, w, scale, alpha);
        else if (setting instanceof BindSetting value) renderBind(context, value, x, y, w, scale, alpha);
        else if (setting instanceof ClickSetting value) renderClick(context, value, x, y, w, scale, alpha);
        else if (setting instanceof BezierSetting value) renderBezier(context, value, x, y, w, scale, alpha);
        else {
            label(context, setting.getName(), x + 4f * scale, y + 8f * scale, scale, alpha);
            CustomFontRenderer.drawRightText(context, String.valueOf(setting.get()), x + w - 4f * scale,
                    y + 7.7f * scale, 5.8f * scale, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.REGULAR);
        }
    }

    private void renderBoolean(DrawContext context, BooleanSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        float animation = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.get() ? 1f : 0f),
                setting.get() ? 1f : 0f, 0.25f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, animation);
        float tw = 18f * s, th = 9.2f * s;
        float tx = x + w - tw - 4f * s, ty = y + 5.5f * s;
        RenderUtil.drawRoundedRect(context, tx, ty, tw, th, th * 0.5f,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, alpha), animation));
        float motion = clamp(Math.abs((setting.get() ? 1f : 0f) - animation) * 4.5f);
        RenderUtil.drawEllipse(context, tx + 4.7f * s + animation * (tw - 9.4f * s), ty + th * 0.5f,
                (3.45f + motion * 1.0f) * s, (3.45f - motion * 0.24f) * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderSlider(DrawContext context, SliderSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        CustomFontRenderer.drawRightText(context, setting.getDisplayValue(), x + w - 4f * s, y + 5.2f * s,
                5.5f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.REGULAR);
        float sx = x + 4f * s, sw = w - 8f * s, sy = y + 22f * s;
        float progress = setting.getProgress();
        RenderUtil.drawRoundedRect(context, sx, sy, sw, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, 0.92f * alpha));
        if (progress > 0.001f) RenderUtil.drawRoundedRect(context, sx, sy, sw * progress, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        RenderUtil.drawCircle(context, sx + sw * progress, sy + 1.5f * s, 3.5f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderRange(DrawContext context, RangeSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        String value = format(setting.getLow(), setting.getStep()) + " — " + format(setting.getHigh(), setting.getStep()) + setting.getSuffix();
        CustomFontRenderer.drawRightText(context, value, x + w - 4f * s, y + 5.2f * s,
                5.4f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.REGULAR);
        float sx = x + 4f * s, sw = w - 8f * s, sy = y + 24f * s;
        float low = setting.getLowProgress(), high = setting.getHighProgress();
        RenderUtil.drawRoundedRect(context, sx, sy, sw, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, 0.92f * alpha));
        RenderUtil.drawRoundedRect(context, sx + sw * low, sy, Math.max(1f, sw * (high - low)), 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        RenderUtil.drawCircle(context, sx + sw * low, sy + 1.5f * s, 3.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        RenderUtil.drawCircle(context, sx + sw * high, sy + 1.5f * s, 3.5f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderMode(DrawContext context, ModeSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        for (Chip chip : modeChips(setting, w / s - 8f)) {
            boolean selected = setting.is(chip.value);
            float cx = x + (4f + chip.x) * s, cy = y + (16f + chip.y) * s;
            RenderUtil.drawRoundedRect(context, cx, cy, chip.w * s, 14f * s, 4f * s,
                    RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : MenuScreen.CHIP_BG,
                            alpha * (selected ? 0.92f : 0.86f)));
            CustomFontRenderer.drawCenteredText(context, chip.label, cx + chip.w * s * 0.5f, cy + 4.5f * s,
                    5.5f * s, RenderUtil.withAlpha(selected ? MenuScreen.CHIP_TEXT_ON : MenuScreen.CHIP_TEXT, alpha), FontWeight.REGULAR);
        }
    }

    private void renderSelect(DrawContext context, SelectSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        for (Chip chip : selectChips(setting, w / s - 8f)) {
            boolean selected = setting.isSelected(chip.value);
            float cx = x + (4f + chip.x) * s, cy = y + (16f + chip.y) * s;
            RenderUtil.drawRoundedRect(context, cx, cy, chip.w * s, 14f * s, 4f * s,
                    RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : MenuScreen.CHIP_BG,
                            alpha * (selected ? 0.88f : 0.84f)));
            CustomFontRenderer.drawCenteredText(context, chip.label, cx + chip.w * s * 0.5f, cy + 4.5f * s,
                    5.5f * s, RenderUtil.withAlpha(selected ? MenuScreen.CHIP_TEXT_ON : MenuScreen.CHIP_TEXT, alpha), FontWeight.REGULAR);
        }
    }

    private void renderItem(DrawContext context, ItemSelectSetting setting, float x, float y, float w, float s,
                            int mouseX, int mouseY, float alpha) {
        renderChoiceHeader(context, setting, setting.getSelectedLabel(), x, y, w, s, alpha);
        if (openChoice == setting) renderItemOptions(context, setting, false, x, y + 31f * s, w, s, mouseX, mouseY, alpha);
    }

    private void renderMultiItem(DrawContext context, MultiItemSelectSetting setting, float x, float y, float w, float s,
                                 int mouseX, int mouseY, float alpha) {
        String value = String.format(ClientLanguage.text("ui.selected_count", "Выбрано: %d"), setting.get().size());
        renderChoiceHeader(context, setting, value, x, y, w, s, alpha);
        if (openChoice == setting) renderItemOptions(context, setting, true, x, y + 31f * s, w, s, mouseX, mouseY, alpha);
    }

    private void renderChoiceHeader(DrawContext context, Setting<?> setting, String value, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 4f * s, s, alpha);
        float fy = y + 14f * s;
        RenderUtil.drawRoundedRect(context, x + 4f * s, fy, w - 8f * s, 16f * s, 4.5f * s,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.90f * alpha));
        value = CustomFontRenderer.trimToWidth(value, w - 28f * s, 5.6f * s, FontWeight.REGULAR);
        CustomFontRenderer.drawText(context, value, x + 9f * s, fy + 5.1f * s, 5.6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.REGULAR);
        CustomFontRenderer.drawRightText(context, openChoice == setting ? "−" : "+", x + w - 9f * s,
                fy + 4.8f * s, 6f * s, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SEMIBOLD);
    }

    private void renderItemOptions(DrawContext context, Setting<?> setting, boolean multi, float x, float y, float w, float s,
                                   int mouseX, int mouseY, float alpha) {
        String query = multi ? ((MultiItemSelectSetting) setting).getSearchQuery() : ((ItemSelectSetting) setting).getSearchQuery();
        RenderUtil.drawRoundedRect(context, x + 4f * s, y, w - 8f * s, 18f * s, 4.5f * s,
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, 0.96f * alpha));
        String display = query.isEmpty() ? ClientLanguage.text("ui.search", "Поиск...") : query;
        CustomFontRenderer.drawText(context, display, x + 9f * s, y + 5.8f * s, 5.5f * s,
                RenderUtil.withAlpha(query.isEmpty() ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE, alpha), FontWeight.REGULAR);
        List<Option> options = itemOptions(setting, multi);
        int start = Math.min(optionScroll.getOrDefault(setting, 0), Math.max(0, options.size() - visibleOptions(setting)));
        int count = Math.min(visibleOptions(setting), options.size() - start);
        for (int i = 0; i < count; i++) {
            Option option = options.get(start + i);
            float oy = y + (20f + i * 17f) * s;
            boolean selected = multi ? ((MultiItemSelectSetting) setting).isSelected(option.id)
                    : option.id.equals(((ItemSelectSetting) setting).get());
            boolean hovered = hit(mouseX, mouseY, x + 4f * s, oy, w - 8f * s, 16f * s);
            if (selected || hovered) {
                RenderUtil.drawRoundedRect(context, x + 4f * s, oy, w - 8f * s, 16f * s, 4f * s,
                        RenderUtil.withAlpha(selected ? MenuScreen.ACCENT : 0xFFFFFF,
                                alpha * (selected ? 0.18f : 0.035f)));
            }
            String label = CustomFontRenderer.trimToWidth(option.label, w - 27f * s, 5.5f * s, FontWeight.REGULAR);
            CustomFontRenderer.drawText(context, label, x + 9f * s, oy + 5.3f * s, 5.5f * s,
                    RenderUtil.withAlpha(selected ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_GRAY, alpha), FontWeight.REGULAR);
            if (selected) RenderUtil.drawCircle(context, x + w - 11f * s, oy + 8f * s, 2.5f * s,
                    RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha));
        }
    }

    private void renderColor(DrawContext context, ColorSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        float swatch = 14f * s, sx = x + w - swatch - 4f * s, sy = y + 4f * s;
        drawChecker(context, sx, sy, swatch, swatch, s, alpha);
        RenderUtil.drawRoundedRect(context, sx, sy, swatch, swatch, 4f * s,
                RenderUtil.withAlpha(setting.get() & 0xFFFFFF, alpha * setting.getAlphaF()));
        RenderUtil.drawRoundedOutline(context, sx, sy, swatch, swatch, 4f * s, 0.65f,
                RenderUtil.withAlpha(screen.isPickerOpenFor(setting) ? MenuScreen.ACCENT_LIGHT : 0xFFFFFF,
                        alpha * (screen.isPickerOpenFor(setting) ? 0.65f : 0.14f)));
        CustomFontRenderer.drawRightText(context, String.format("#%06X", setting.get() & 0xFFFFFF), sx - 7f * s,
                y + 7.9f * s, 5.3f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, 0.86f * alpha), FontWeight.REGULAR);
    }

    private void renderString(DrawContext context, StringSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 4f * s, s, alpha);
        float fy = y + 14f * s;
        boolean editing = editingString == setting;
        RenderUtil.drawRoundedRect(context, x + 4f * s, fy, w - 8f * s, 16f * s, 4.5f * s,
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, alpha * (editing ? 1f : 0.82f)));
        RenderUtil.drawRoundedOutline(context, x + 4f * s, fy, w - 8f * s, 16f * s, 4.5f * s, 0.7f,
                RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * (editing ? 0.34f : 0f)));
        String value = setting.get().isEmpty() ? setting.getPlaceholder() : setting.get();
        value = CustomFontRenderer.trimToWidth(value, w - 18f * s, 5.6f * s, FontWeight.REGULAR);
        CustomFontRenderer.drawText(context, value, x + 9f * s, fy + 5.1f * s, 5.6f * s,
                RenderUtil.withAlpha(setting.get().isEmpty() ? MenuScreen.TEXT_DIM : MenuScreen.TEXT_WHITE, alpha), FontWeight.REGULAR);
        if (editing && (System.currentTimeMillis() / 500L) % 2 == 0) {
            float cx = x + 9f * s + CustomFontRenderer.getTextWidth(value, 5.6f * s, FontWeight.REGULAR) + 1f;
            RenderUtil.drawRect(context, Math.min(cx, x + w - 10f * s), fy + 4f * s, 0.7f, 8f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
    }

    private void renderBind(DrawContext context, BindSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        boolean binding = screen.isBindingSetting(setting);
        String value = binding ? "..." : setting.getKeyName();
        float bw = Math.max(31f * s, CustomFontRenderer.getTextWidth(value, 5.6f * s, FontWeight.REGULAR) + 15f * s);
        RenderUtil.drawRoundedRect(context, x + w - bw - 4f * s, y + 4f * s, bw, 16f * s, 5f * s,
                RenderUtil.withAlpha(binding ? MenuScreen.ACCENT : MenuScreen.CHIP_BG, alpha * (binding ? 0.28f : 0.88f)));
        CustomFontRenderer.drawCenteredText(context, value, x + w - bw * 0.5f - 4f * s, y + 9f * s,
                5.6f * s, RenderUtil.withAlpha(binding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_GRAY, alpha), FontWeight.REGULAR);
    }

    private void renderClick(DrawContext context, ClickSetting setting, float x, float y, float w, float s, float alpha) {
        RenderUtil.drawRoundedRect(context, x + 4f * s, y + 4f * s, w - 8f * s, 20f * s, 5.5f * s,
                RenderUtil.withAlpha(MenuScreen.CHIP_BG, 0.92f * alpha));
        RenderUtil.drawRoundedOutline(context, x + 4f * s, y + 4f * s, w - 8f * s, 20f * s, 5.5f * s, 0.65f,
                RenderUtil.withAlpha(MenuScreen.ACCENT, 0.16f * alpha));
        CustomFontRenderer.drawCenteredText(context, setting.getName(), x + w * 0.5f, y + 10.7f * s,
                6.1f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
    }

    private void renderBezier(DrawContext context, BezierSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 4f * s, s, alpha);
        float gx = x + 5f * s, gy = y + 16f * s, gw = w - 10f * s, gh = 50f * s;
        RenderUtil.drawRoundedRect(context, gx, gy, gw, gh, 5f * s,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.92f * alpha));
        BezierSetting.BezierCurve curve = setting.get();
        float p1x = gx + curve.x1 * gw, p1y = gy + (1f - curve.y1) * gh;
        float p2x = gx + curve.x2 * gw, p2y = gy + (1f - curve.y2) * gh;
        RenderUtil.drawRoundedLine(context, gx, gy + gh, p1x, p1y, 1f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.45f * alpha));
        RenderUtil.drawRoundedLine(context, gx + gw, gy, p2x, p2y, 1f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.45f * alpha));
        float lastX = gx, lastY = gy + gh;
        for (int i = 1; i <= 24; i++) {
            float t = i / 24f;
            float nx = gx + t * gw;
            float ny = gy + (1f - setting.evaluate(t)) * gh;
            RenderUtil.drawRoundedLine(context, lastX, lastY, nx, ny, 1.4f * s,
                    RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha));
            lastX = nx;
            lastY = ny;
        }
        RenderUtil.drawCircle(context, p1x, p1y, 3.3f * s, RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        RenderUtil.drawCircle(context, p2x, p2y, 3.3f * s, RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button, float scale) {
        if (!contains(mouseX, mouseY, scale)) return false;
        float headerH = HEADER_H * scale;
        if (mouseY <= drawY + headerH) {
            float toggleX = drawX + width - 27f * scale;
            float gearX = toggleX - 11.8f * scale;
            float bindX = gearX - 13.2f * scale;
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                    || hit(mouseX, mouseY, bindX - 5.5f * scale, drawY + 4f * scale, 11f * scale, 22f * scale)) {
                if (blocked()) shake(); else screen.requestModuleBind(module);
                return false;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || hit(mouseX, mouseY, gearX - 5.5f * scale, drawY + 4f * scale, 11f * scale, 22f * scale)) {
                if (blocked() || module.getVisibleSettings().isEmpty()) shake(); else screen.toggleExpanded(this);
                return false;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (blocked() || !module.isAvailable() && !module.isEnabled()) shake(); else module.toggle();
            }
            return false;
        }
        if (!expanded || expansionValue() < 0.72f || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        SettingHit hit = findSetting(mouseX, mouseY, scale);
        if (hit == null) return false;
        Setting<?> setting = hit.setting;
        float localX = (mouseX - hit.x) / scale;
        float localY = (mouseY - hit.y) / scale;
        float baseW = hit.w / scale;
        if (setting instanceof BooleanSetting value) value.toggle();
        else if (setting instanceof SliderSetting value) {
            activeDrag = setting;
            updateSlider(value, localX, baseW);
            return true;
        } else if (setting instanceof RangeSetting value) {
            activeDrag = setting;
            float progress = clamp((localX - 4f) / (baseW - 8f));
            rangeHandle = Math.abs(progress - value.getLowProgress()) <= Math.abs(progress - value.getHighProgress()) ? 0 : 1;
            updateRange(value, localX, baseW);
            return true;
        } else if (setting instanceof ModeSetting value) {
            for (Chip chip : modeChips(value, baseW - 8f)) {
                if (hit(localX, localY, 4f + chip.x, 16f + chip.y, chip.w, 14f)) value.set(chip.value);
            }
        } else if (setting instanceof SelectSetting value) {
            for (Chip chip : selectChips(value, baseW - 8f)) {
                if (hit(localX, localY, 4f + chip.x, 16f + chip.y, chip.w, 14f)) value.toggle(chip.value);
            }
        } else if (setting instanceof ItemSelectSetting value) {
            if (handleItemClick(value, false, localX, localY, baseW)) return false;
        } else if (setting instanceof MultiItemSelectSetting value) {
            if (handleItemClick(value, true, localX, localY, baseW)) return false;
        } else if (setting instanceof ColorSetting value) {
            screen.openColorPicker(value, hit.x + hit.w - 2f * scale, hit.y + 3f * scale);
        } else if (setting instanceof StringSetting value) {
            editingString = value;
            editingChoice = null;
        } else if (setting instanceof BindSetting value) {
            screen.requestSettingBind(value);
        } else if (setting instanceof ClickSetting value) {
            value.click();
            EnoughSoundPlayer.playScroll();
        } else if (setting instanceof BezierSetting value) {
            activeDrag = value;
            float gx = 5f, gy = 16f, gw = baseW - 10f, gh = 50f;
            BezierSetting.BezierCurve curve = value.get();
            float d1 = distance(localX, localY, gx + curve.x1 * gw, gy + (1f - curve.y1) * gh);
            float d2 = distance(localX, localY, gx + curve.x2 * gw, gy + (1f - curve.y2) * gh);
            bezierHandle = d1 <= d2 ? 0 : 1;
            updateBezier(value, localX, localY, baseW);
            return true;
        }
        EnoughSoundPlayer.playScroll();
        return false;
    }

    private boolean handleItemClick(Setting<?> setting, boolean multi, float x, float y, float w) {
        if (y >= 13f && y <= 31f) {
            openChoice = openChoice == setting ? null : setting;
            editingChoice = openChoice;
            return true;
        }
        if (openChoice != setting) return false;
        if (y >= 31f && y <= 51f) {
            editingChoice = setting;
            return true;
        }
        int row = (int) ((y - 51f) / 17f);
        List<Option> options = itemOptions(setting, multi);
        int start = Math.min(optionScroll.getOrDefault(setting, 0), Math.max(0, options.size() - visibleOptions(setting)));
        if (row >= 0 && row < visibleOptions(setting) && start + row < options.size()) {
            String id = options.get(start + row).id;
            if (multi) ((MultiItemSelectSetting) setting).toggle(id);
            else {
                ((ItemSelectSetting) setting).set(id);
                openChoice = null;
                editingChoice = null;
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(float mouseX, float mouseY, float scale) {
        if (activeDrag == null) return false;
        SettingHit hit = findSetting(activeDrag, scale);
        if (hit == null) return false;
        float localX = (mouseX - hit.x) / scale;
        float localY = (mouseY - hit.y) / scale;
        float baseW = hit.w / scale;
        if (activeDrag instanceof SliderSetting value) updateSlider(value, localX, baseW);
        else if (activeDrag instanceof RangeSetting value) updateRange(value, localX, baseW);
        else if (activeDrag instanceof BezierSetting value) updateBezier(value, localX, localY, baseW);
        return true;
    }

    public void mouseReleased(float mouseX, float mouseY, int button, float scale) {
        activeDrag = null;
    }

    public boolean mouseScrolled(float mouseX, float mouseY, double amount, float scale) {
        if (!expanded || !contains(mouseX, mouseY, scale)) return false;
        SettingHit hovered = findSetting(mouseX, mouseY, scale);
        if (hovered != null && openChoice == hovered.setting
                && (hovered.setting instanceof ItemSelectSetting || hovered.setting instanceof MultiItemSelectSetting)) {
            List<Option> options = itemOptions(hovered.setting, hovered.setting instanceof MultiItemSelectSetting);
            int max = Math.max(0, options.size() - visibleOptions(hovered.setting));
            int current = optionScroll.getOrDefault(hovered.setting, 0);
            current = Math.max(0, Math.min(max, current - (int) Math.signum(amount)));
            optionScroll.put(hovered.setting, current);
            EnoughSoundPlayer.playScroll();
            return true;
        }
        if (bodyMaxScroll > 0.5f) {
            bodyScrollTarget += (float) amount * 18f;
            bodyScrollTarget = Math.max(-bodyMaxScroll, Math.min(0f, bodyScrollTarget));
            EnoughSoundPlayer.playScroll();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int key) {
        if (editingString != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                editingString = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !editingString.get().isEmpty()) {
                editingString.set(editingString.get().substring(0, editingString.get().length() - 1));
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                editingString.set("");
                return true;
            }
            return true;
        }
        if (editingChoice != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                editingChoice = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String query = choiceQuery(editingChoice);
                if (!query.isEmpty()) setChoiceQuery(editingChoice, query.substring(0, query.length() - 1));
                optionScroll.put(editingChoice, 0);
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (editingString != null) {
            if (!Character.isISOControl(chr) && editingString.isCharAllowed(chr)) editingString.set(editingString.get() + chr);
            return true;
        }
        if (editingChoice != null) {
            if (!Character.isISOControl(chr) && choiceQuery(editingChoice).length() < 36) {
                setChoiceQuery(editingChoice, choiceQuery(editingChoice) + chr);
                optionScroll.put(editingChoice, 0);
            }
            return true;
        }
        return false;
    }

    private void updateSlider(SliderSetting setting, float localX, float w) {
        setting.setFromProgress(clamp((localX - 4f) / (w - 8f)));
    }

    private void updateRange(RangeSetting setting, float localX, float w) {
        float progress = clamp((localX - 4f) / (w - 8f));
        double value = setting.getMin() + (setting.getMax() - setting.getMin()) * progress;
        if (rangeHandle == 0) setting.setLow(value); else setting.setHigh(value);
    }

    private void updateBezier(BezierSetting setting, float localX, float localY, float w) {
        float gx = 5f, gy = 16f, gw = w - 10f, gh = 50f;
        float cx = clamp((localX - gx) / gw);
        float cy = clamp(1f - (localY - gy) / gh);
        BezierSetting.BezierCurve curve = setting.get();
        if (bezierHandle == 0) setting.set(new BezierSetting.BezierCurve(cx, cy, curve.x2, curve.y2));
        else setting.set(new BezierSetting.BezierCurve(curve.x1, curve.y1, cx, cy));
    }

    private SettingHit findSetting(float mouseX, float mouseY, float scale) {
        float rowX = drawX + 8f * scale;
        float rowY = drawY + (HEADER_H + 5f + bodyScroll) * scale;
        float rowW = width - 16f * scale;
        float baseW = rowW / scale;
        for (Setting<?> setting : module.getVisibleSettings()) {
            float rowH = rowHeight(setting, baseW) * scale;
            if (hit(mouseX, mouseY, rowX, rowY, rowW, rowH)) return new SettingHit(setting, rowX, rowY, rowW, rowH);
            rowY += rowH;
        }
        return null;
    }

    private SettingHit findSetting(Setting<?> target, float scale) {
        float rowX = drawX + 8f * scale;
        float rowY = drawY + (HEADER_H + 5f + bodyScroll) * scale;
        float rowW = width - 16f * scale;
        float baseW = rowW / scale;
        for (Setting<?> setting : module.getVisibleSettings()) {
            float rowH = rowHeight(setting, baseW) * scale;
            if (setting == target) return new SettingHit(setting, rowX, rowY, rowW, rowH);
            rowY += rowH;
        }
        return null;
    }

    private float contentHeight(float width) {
        float height = 9f;
        for (Setting<?> setting : module.getVisibleSettings()) height += rowHeight(setting, width);
        return height;
    }

    private float rowHeight(Setting<?> setting, float width) {
        if (setting instanceof BooleanSetting || setting instanceof ColorSetting) return 25f;
        if (setting instanceof SliderSetting) return 34f;
        if (setting instanceof RangeSetting) return 37f;
        if (setting instanceof BindSetting) return 26f;
        if (setting instanceof StringSetting) return 35f;
        if (setting instanceof ClickSetting) return 29f;
        if (setting instanceof BezierSetting) return 72f;
        if (setting instanceof ModeSetting value) return 19f + chipRows(modeChips(value, width - 8f)) * 17f;
        if (setting instanceof SelectSetting value) return 19f + chipRows(selectChips(value, width - 8f)) * 17f;
        if (setting instanceof ItemSelectSetting || setting instanceof MultiItemSelectSetting) {
            if (openChoice == setting) {
                int count = Math.min(visibleOptions(setting), itemOptions(setting, setting instanceof MultiItemSelectSetting).size());
                return 54f + count * 17f;
            }
            return 35f;
        }
        return 25f;
    }

    private List<Chip> modeChips(ModeSetting setting, float available) {
        List<Chip> chips = new ArrayList<>();
        float x = 0f, y = 0f;
        for (String option : setting.getModes()) {
            String label = setting.getDisplayMode(option);
            float w = Math.max(28f, CustomFontRenderer.getTextWidth(label, 5.5f, FontWeight.REGULAR) + 12f);
            if (x > 0f && x + w > available) { x = 0f; y += 17f; }
            chips.add(new Chip(option, label, x, y, w));
            x += w + 4f;
        }
        return chips;
    }

    private List<Chip> selectChips(SelectSetting setting, float available) {
        List<Chip> chips = new ArrayList<>();
        float x = 0f, y = 0f;
        for (String option : setting.getOptions()) {
            String label = setting.getDisplayOption(option);
            float w = Math.max(28f, CustomFontRenderer.getTextWidth(label, 5.5f, FontWeight.REGULAR) + 12f);
            if (x > 0f && x + w > available) { x = 0f; y += 17f; }
            chips.add(new Chip(option, label, x, y, w));
            x += w + 4f;
        }
        return chips;
    }

    private int chipRows(List<Chip> chips) {
        if (chips.isEmpty()) return 1;
        return 1 + Math.round(chips.get(chips.size() - 1).y / 17f);
    }

    private List<Option> itemOptions(Setting<?> setting, boolean multi) {
        List<Option> options = new ArrayList<>();
        if (multi) {
            for (MultiItemSelectSetting.ItemOption option : ((MultiItemSelectSetting) setting).getFilteredOptions()) {
                options.add(new Option(option.id(), option.label()));
            }
        } else {
            for (ItemSelectSetting.ItemOption option : ((ItemSelectSetting) setting).getFilteredOptions()) {
                options.add(new Option(option.id(), option.label()));
            }
        }
        return options;
    }

    private int visibleOptions(Setting<?> setting) {
        int configured = setting instanceof MultiItemSelectSetting multi ? multi.getMaxVisibleOptions()
                : ((ItemSelectSetting) setting).getMaxVisibleOptions();
        return Math.min(5, configured);
    }

    private String choiceQuery(Setting<?> setting) {
        if (setting instanceof MultiItemSelectSetting multi) return multi.getSearchQuery();
        return ((ItemSelectSetting) setting).getSearchQuery();
    }

    private void setChoiceQuery(Setting<?> setting, String value) {
        if (setting instanceof MultiItemSelectSetting multi) multi.setSearchQuery(value);
        else ((ItemSelectSetting) setting).setSearchQuery(value);
    }

    private boolean blocked() {
        return ModuleManager.getInstance().isBlocked(module);
    }

    private void shake() {
        shakeAt = System.currentTimeMillis();
    }

    private float expansionValue() {
        float progress = clamp((System.currentTimeMillis() - expansionStartedAt) / (float) Math.max(1L, expansionDuration));
        if (progress >= 1f) {
            expansionValue = expansionTarget;
            return expansionValue;
        }
        float eased = expansionTarget > expansionFrom
                ? RenderUtil.easeOutBack(progress)
                : RenderUtil.easeInOutCubic(progress);
        expansionValue = expansionFrom + (expansionTarget - expansionFrom) * eased;
        return expansionValue;
    }

    private static void label(DrawContext context, String text, float x, float y, float scale, float alpha) {
        CustomFontRenderer.drawText(context, text, x, y, 6.1f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.94f * alpha), FontWeight.REGULAR);
    }

    private static void drawChecker(DrawContext context, float x, float y, float w, float h, float scale, float alpha) {
        float cell = Math.max(2f, 3f * scale);
        int rows = Math.max(1, (int) Math.ceil(h / cell));
        int cols = Math.max(1, (int) Math.ceil(w / cell));
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                RenderUtil.drawRect(context, x + col * cell, y + row * cell,
                        Math.min(cell, w - col * cell), Math.min(cell, h - row * cell),
                        RenderUtil.withAlpha(((row + col) & 1) == 0 ? 0xB9B5C2 : 0x686371, 0.55f * alpha));
            }
        }
    }

    private static String format(double value, double step) {
        if (step >= 1d) return String.valueOf((int) Math.round(value));
        if (step >= 0.1d) return String.format("%.1f", value);
        return String.format("%.2f", value);
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private record Chip(String value, String label, float x, float y, float w) {}
    private record Option(String id, String label) {}
    private record SettingHit(Setting<?> setting, float x, float y, float w, float h) {}
}
