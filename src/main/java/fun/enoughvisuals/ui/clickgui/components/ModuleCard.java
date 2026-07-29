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
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleCard {
    private static final float HEADER_H = 41f;
    private static final String ICON_GEAR = "\uF007";
    private static final String ICON_BIND = "\uF005";

    private final Module module;
    private final MenuScreen screen;
    private final Map<Setting<?>, Float> hoverAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> valueAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> secondaryAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> choiceAnimations = new HashMap<>();
    private final Map<String, Float> controlAnimations = new HashMap<>();
    private final Map<Setting<?>, Integer> optionScroll = new HashMap<>();
    private final Map<Setting<?>, Float> smoothOptionScroll = new HashMap<>();
    private final Map<Setting<?>, Integer> typedIndices = new HashMap<>();
    private final Map<Setting<?>, Long> typedAt = new HashMap<>();

    public int column;
    private float x;
    private float y;
    private float width;
    private float drawX;
    private float drawY;
    private float hover;
    private float gearHover;
    private float bindHover;
    private float bindIconX;
    private float bindHitW = 12f;
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
    private boolean expanded;
    private long introStart;
    private int introDirection;
    private long highlightAt;
    private long feedbackShadowAt;
    private boolean wasOffscreen = true;
    private long viewportEnteredAt;
    private long descriptionHoverAt;
    private float descriptionOffset;

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
        introDirection = direction == 0 ? (column == 0 ? -1 : 1) : direction;
        wasOffscreen = false;
        viewportEnteredAt = start - 260L;
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
        float e = Math.max(0f, expansionValue());
        return (HEADER_H + (total + 7f) * e) * scale;
    }

    public boolean contains(float mouseX, float mouseY, float scale) {
        return mouseX >= drawX && mouseX <= drawX + width
                && mouseY >= drawY && mouseY <= drawY + currentHeight(scale);
    }

    public void tickOffscreen() {
        expansionValue();
        bodyScroll = bodyScrollTarget = 0f;
        wasOffscreen = true;
    }

    public void render(DrawContext context, float scale, int mouseX, int mouseY, float parentAlpha, long now) {
        if (wasOffscreen) {
            wasOffscreen = false;
            viewportEnteredAt = now;
        }
        float intro = clamp((now - introStart) / 470f);
        float viewport = clamp((now - viewportEnteredAt) / 410f);
        float reveal = Math.min(intro, viewport);
        drawX = x;
        drawY = y + (1f - RenderUtil.easeOutQuart(reveal)) * 11f * scale;
        float alpha = parentAlpha * RenderUtil.easeOutExpo(reveal);
        float height = currentHeight(scale);
        boolean hovered = contains(mouseX, mouseY, scale);
        float frame = RenderUtil.getFrameDelta();
        hover = RenderUtil.animate(hover, hovered ? 1f : 0f, 0.28f, frame);
        if (lastEnabled != module.isEnabled()) {
            lastEnabled = module.isEnabled();
            toggleChangedAt = now;
        }
        toggleAnimation = RenderUtil.animate(toggleAnimation, module.isEnabled() ? 1f : 0f, 0.21f, frame);
        bodyScroll = bodyScrollTarget = 0f;

        float highlight = 0f;
        if (now - highlightAt < 850L) {
            float p = (now - highlightAt) / 850f;
            highlight = (1f - RenderUtil.easeOutCubic(p)) * (0.5f + 0.5f * (float) Math.sin(p * Math.PI * 3f));
        }
        int cardColor = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.CARD_BG, 0.93f * alpha),
                RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.97f * alpha), hover * 0.72f);
        RenderUtil.drawRoundedRect(context, drawX, drawY, width, height, 8.5f * scale, cardColor);
        if (highlight > 0.01f) {
            RenderUtil.drawRoundedRect(context, drawX + 0.7f * scale, drawY + 0.7f * scale,
                    width - 1.4f * scale, height - 1.4f * scale, 7.8f * scale,
                    RenderUtil.withAlpha(MenuScreen.CARD_HOVER, 0.18f * highlight * alpha));
        }

        renderHeader(context, scale, mouseX, mouseY, alpha, now);
        float expand = clamp(expansionValue());
        if (expand > 0.005f) renderSettings(context, scale, mouseX, mouseY, alpha * RenderUtil.easeOutQuad(expand), expand);
    }

    private void renderHeader(DrawContext context, float scale, int mouseX, int mouseY, float alpha, long now) {
        boolean blocked = ModuleManager.getInstance().isBlocked(module);
        boolean available = !blocked && module.isAvailable();
        float headerH = HEADER_H * scale;
        float toggleW = 17f * scale;
        float toggleH = 10.2f * scale;
        float toggleX = drawX + width - 9f * scale - toggleW;
        float toggleY = drawY + (HEADER_H * scale - toggleH) * 0.5f;
        boolean hasSettings = !displaySettings().isEmpty();
        float gearX = toggleX - 11f * scale;
        gearHover = RenderUtil.animate(gearHover,
                hit(mouseX, mouseY, gearX - 5f * scale, drawY + 4f * scale, 11f * scale, 20f * scale) ? 1f : 0f,
                0.31f, RenderUtil.getFrameDelta());

        float nameX = drawX + 10f * scale;
        float nameSize = 7.6f * scale;
        boolean moduleBinding = screen.isBindingModule(module);
        boolean hasBind = module.getKeyBind() != BindKeyUtil.NONE;
        String bindLabel = moduleBinding ? "..." : hasBind ? BindKeyUtil.getBindName(module.getKeyBind()) : "";
        float bindLabelW = bindLabel.isEmpty() ? 0f : CustomFontRenderer.getTextWidth(bindLabel,
                5.2f * scale, FontWeight.SEMIBOLD);
        float nameMax = (hasSettings ? gearX : toggleX) - nameX - (21f * scale + bindLabelW);
        String name = CustomFontRenderer.trimToWidth(module.getName(), nameMax, nameSize, FontWeight.SEMIBOLD);
        float nameWidth = CustomFontRenderer.getTextWidth(name, nameSize, FontWeight.SEMIBOLD);
        float bindLimit = (hasSettings ? gearX : toggleX) - 10f * scale;
        bindIconX = Math.min(bindLimit - bindLabelW - 4f * scale, nameX + nameWidth + 7f * scale);
        bindHitW = 12f * scale + bindLabelW;
        bindHover = RenderUtil.animate(bindHover,
                hit(mouseX, mouseY, bindIconX - 6f * scale, drawY + 2f * scale, bindHitW, 20f * scale) ? 1f : 0f,
                0.31f, RenderUtil.getFrameDelta());
        CustomFontRenderer.drawText(context, name, nameX, drawY + 7.4f * scale, nameSize,
                RenderUtil.withAlpha(available || module.isEnabled() ? MenuScreen.TEXT_WHITE : MenuScreen.TEXT_DIM,
                        alpha * (available || module.isEnabled() ? 0.98f : 0.55f)), FontWeight.SEMIBOLD);
        float descriptionX = drawX + 10f * scale;
        float descriptionRight = (hasSettings ? gearX : toggleX) - 7f * scale;
        renderDescription(context, module.getDescription(), descriptionX, drawY + 19.9f * scale,
                Math.max(36f * scale, descriptionRight - descriptionX), scale,
                mouseX, mouseY, alpha, now);

        CustomFontRenderer.drawCenteredText(context, ICON_BIND, bindIconX, drawY + 7.1f * scale, 10f * scale,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.72f * alpha),
                        RenderUtil.withAlpha(moduleBinding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE, alpha),
                        Math.max(bindHover, moduleBinding ? 1f : 0f)), FontWeight.CLICK_GUI);
        if (!bindLabel.isEmpty()) {
            CustomFontRenderer.drawText(context, bindLabel, bindIconX + 7f * scale, drawY + 9.8f * scale,
                    5.2f * scale, RenderUtil.withAlpha(moduleBinding ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_GRAY,
                            alpha * (0.88f + bindHover * 0.12f)), FontWeight.SEMIBOLD);
        }
        if (hasSettings) {
            CustomFontRenderer.drawCenteredText(context, ICON_GEAR, gearX, drawY + 15.1f * scale, 9f * scale,
                    RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.74f * alpha),
                            RenderUtil.withAlpha(expanded ? MenuScreen.ACCENT_LIGHT : MenuScreen.TEXT_WHITE, alpha),
                            Math.max(gearHover, expanded ? 0.72f : 0f)), FontWeight.CLICK_GUI);
        }

        int track = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, 0.95f * alpha),
                RenderUtil.withAlpha(MenuScreen.TOGGLE_ON, alpha), toggleAnimation);
        float switchProgress = clamp((System.currentTimeMillis() - toggleChangedAt) / 280f);
        float pulse = toggleChangedAt == 0L ? 0f : (float) Math.sin(switchProgress * Math.PI) * (1f - switchProgress);
        float motion = clamp(Math.abs((module.isEnabled() ? 1f : 0f) - toggleAnimation) * 4.5f);
        RenderUtil.drawRoundedRect(context, toggleX - pulse * 0.25f * scale, toggleY,
                toggleW + pulse * 0.5f * scale, toggleH, toggleH * 0.5f, track);
        float knobX = toggleX + 5f * scale + toggleAnimation * (toggleW - 10f * scale);
        float knobRx = (3.75f + motion * 1.05f + pulse * 0.28f) * scale;
        float knobRy = (3.75f - motion * 0.24f) * scale;
        RenderUtil.drawEllipse(context, knobX, toggleY + toggleH * 0.5f, knobRx, knobRy,
                RenderUtil.withAlpha(MenuScreen.TOGGLE_KNOB, alpha));
    }

    private void renderDescription(DrawContext context, String description, float x, float y, float available,
                                   float scale, int mouseX, int mouseY, float alpha, long now) {
        float fontSize = 6.35f * scale;
        float fullWidth = CustomFontRenderer.getTextWidth(description, fontSize, FontWeight.SEMIBOLD);
        boolean overflow = fullWidth > available;
        boolean hovered = overflow && contains(mouseX, mouseY, scale);
        if (hovered) {
            if (descriptionHoverAt == 0L) descriptionHoverAt = now;
        }

        float targetOffset = 0f;
        float distance = Math.max(0f, fullWidth - available + 8f * scale);
        if (hovered) {
            long travel = Math.max(1150L, Math.min(2800L, (long) (distance * 17f / Math.max(scale, 0.01f))));
            long pause = 430L;
            long cycle = travel * 2L + pause * 2L;
            long t = (now - descriptionHoverAt) % cycle;
            if (t < travel) targetOffset = distance * RenderUtil.easeOutCubic(t / (float) travel);
            else if (t < travel + pause) targetOffset = distance;
            else if (t < travel * 2L + pause) {
                float p = (t - travel - pause) / (float) travel;
                targetOffset = distance * (1f - RenderUtil.easeOutCubic(p));
            }
        }
        descriptionOffset = RenderUtil.animate(descriptionOffset, targetOffset,
                hovered ? 0.32f : 0.13f, RenderUtil.getFrameDelta());
        if (!hovered && descriptionOffset < 0.05f * scale) {
            descriptionOffset = 0f;
            descriptionHoverAt = 0L;
        }

        float leftFade = overflow ? Math.min(10f * scale, descriptionOffset * 0.75f) : 0f;
        float remaining = Math.max(0f, distance - descriptionOffset);
        float rightFade = overflow ? Math.min(14f * scale, remaining * 0.85f + 3f * scale) : 0f;
        CustomFontRenderer.drawTextWithFade(context, description, x - descriptionOffset, y, fontSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.94f * alpha), FontWeight.SEMIBOLD,
                x, available, leftFade, rightFade);
    }

    private void renderSettings(DrawContext context, float scale, int mouseX, int mouseY, float alpha, float expand) {
        float top = drawY + HEADER_H * scale;
        float baseWidth = width / scale - 16f;
        float revealH = contentHeight(baseWidth) * scale * expand;
        bodyScroll = bodyScrollTarget = 0f;

        RenderUtil.drawRect(context, drawX + 9f * scale, top, width - 18f * scale, 0.6f,
                RenderUtil.withAlpha(0xFFFFFF, 0.06f * alpha));
        context.draw();
        StencilUtil.enableScissor(drawX + 2f * scale, top + 1f, width - 4f * scale, Math.max(1f, revealH));
        float rowX = drawX + 8f * scale;
        float rowY = top + 5f * scale;
        float rowW = width - 16f * scale;
        for (Setting<?> setting : displaySettings()) {
            float rowH = rowHeight(setting, baseWidth) * scale;
            if (rowY + rowH >= top - 10f * scale && rowY <= top + revealH + 10f * scale) {
                renderSetting(context, setting, rowX, rowY, rowW, rowH, scale, mouseX, mouseY, alpha);
            }
            rowY += rowH;
        }
        context.draw();
        StencilUtil.disableScissor();
    }

    private void renderSetting(DrawContext context, Setting<?> setting, float x, float y, float w, float h,
                               float scale, int mouseX, int mouseY, float alpha) {
        boolean hovered = hit(mouseX, mouseY, x, y, w, h);
        float hover = RenderUtil.animate(hoverAnimations.getOrDefault(setting, 0f), hovered ? 1f : 0f,
                0.30f, RenderUtil.getFrameDelta());
        hoverAnimations.put(setting, hover);
        if (setting instanceof BooleanSetting value) renderBoolean(context, value, x, y, w, scale, alpha);
        else if (setting instanceof SliderSetting value) renderSlider(context, value, x, y, w, scale, alpha, hover);
        else if (setting instanceof RangeSetting value) renderRange(context, value, x, y, w, scale, alpha, hover);
        else if (setting instanceof ModeSetting value) renderMode(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof SelectSetting value) renderSelect(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof ItemSelectSetting value) renderItem(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof MultiItemSelectSetting value) renderMultiItem(context, value, x, y, w, scale, mouseX, mouseY, alpha);
        else if (setting instanceof ColorSetting value) renderColor(context, value, x, y, w, scale, alpha);
        else if (setting instanceof StringSetting value) renderString(context, value, x, y, w, scale, alpha);
        else if (setting instanceof BindSetting value) renderBind(context, value, x, y, w, scale, alpha);
        else if (setting instanceof ClickSetting value) renderClick(context, value, x, y, w, scale, alpha, hover);
        else if (setting instanceof BezierSetting value) renderBezier(context, value, x, y, w, scale, alpha);
        else {
            label(context, setting.getName(), x + 4f * scale, y + 8f * scale, scale, alpha);
            CustomFontRenderer.drawRightText(context, String.valueOf(setting.get()), x + w - 4f * scale,
                    y + 7.7f * scale, 5.8f * scale, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.SEMIBOLD);
        }
    }

    private void renderBoolean(DrawContext context, BooleanSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        float animation = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.get() ? 1f : 0f),
                setting.get() ? 1f : 0f, 0.25f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, animation);
        float tw = 17f * s, th = 10.2f * s;
        float tx = x + w - tw - 4f * s, ty = y + 5.5f * s;
        RenderUtil.drawRoundedRect(context, tx, ty, tw, th, th * 0.5f,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, alpha), animation));
        float motion = clamp(Math.abs((setting.get() ? 1f : 0f) - animation) * 4.5f);
        RenderUtil.drawEllipse(context, tx + 5f * s + animation * (tw - 10f * s), ty + th * 0.5f,
                (3.75f + motion * 1.0f) * s, (3.75f - motion * 0.24f) * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderSlider(DrawContext context, SliderSetting setting, float x, float y, float w, float s,
                              float alpha, float hover) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        CustomFontRenderer.drawRightText(context, setting.getDisplayValue(), x + w - 4f * s, y + 5.2f * s,
                5.5f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.SEMIBOLD);
        float sx = x + 4f * s, sw = w - 8f * s, sy = y + 16.5f * s;
        float progress = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.getProgress()),
                setting.getProgress(), activeDrag == setting ? 0.48f : 0.24f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, progress);
        RenderUtil.drawRoundedRect(context, sx, sy, sw, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, 0.92f * alpha));
        if (progress > 0.001f) RenderUtil.drawRoundedRect(context, sx, sy, sw * progress, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        float movement = clamp(Math.abs(setting.getProgress() - progress) * 9f);
        RenderUtil.drawCircle(context, sx + sw * progress, sy + 1.5f * s,
                (3.5f + hover * 0.45f + movement * 0.65f) * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderRange(DrawContext context, RangeSetting setting, float x, float y, float w, float s,
                             float alpha, float hover) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        String value = format(setting.getLow(), setting.getStep()) + " — " + format(setting.getHigh(), setting.getStep()) + setting.getSuffix();
        CustomFontRenderer.drawRightText(context, value, x + w - 4f * s, y + 5.2f * s,
                5.4f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.SEMIBOLD);
        float sx = x + 4f * s, sw = w - 8f * s, sy = y + 18f * s;
        float low = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.getLowProgress()),
                setting.getLowProgress(), activeDrag == setting ? 0.48f : 0.24f, RenderUtil.getFrameDelta());
        float high = RenderUtil.animate(secondaryAnimations.getOrDefault(setting, setting.getHighProgress()),
                setting.getHighProgress(), activeDrag == setting ? 0.48f : 0.24f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, low);
        secondaryAnimations.put(setting, high);
        RenderUtil.drawRoundedRect(context, sx, sy, sw, 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, 0.92f * alpha));
        RenderUtil.drawRoundedRect(context, sx + sw * low, sy, Math.max(1f, sw * (high - low)), 3f * s, 1.5f * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT, alpha));
        float lowActive = activeDrag == setting && rangeHandle == 0 ? 1f : 0f;
        float highActive = activeDrag == setting && rangeHandle == 1 ? 1f : 0f;
        RenderUtil.drawCircle(context, sx + sw * low, sy + 1.5f * s, (3.5f + hover * 0.25f + lowActive * 0.8f) * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        RenderUtil.drawCircle(context, sx + sw * high, sy + 1.5f * s, (3.5f + hover * 0.25f + highActive * 0.8f) * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
    }

    private void renderMode(DrawContext context, ModeSetting setting, float x, float y, float w, float s,
                            int mouseX, int mouseY, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        for (Chip chip : modeChips(setting, w / s - 8f)) {
            boolean selected = setting.is(chip.value);
            float cx = x + (4f + chip.x) * s, cy = y + (16f + chip.y) * s;
            boolean hovered = hit(mouseX, mouseY, cx, cy, chip.w * s, 14f * s);
            float selectedAnim = control(setting, chip.value, "selected", selected ? 1f : 0f, 0.24f);
            float hoverAnim = control(setting, chip.value, "hover", hovered ? 1f : 0f, 0.38f);
            int background = RenderUtil.lerpColor(
                    RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_BG, 0.84f * alpha),
                            RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.CHIP_BG, 0.09f), 0.84f * alpha), hoverAnim),
                    RenderUtil.withAlpha(MenuScreen.ACCENT, 0.94f * alpha), selectedAnim);
            RenderUtil.drawRoundedRect(context, cx, cy, chip.w * s, 14f * s, 4.5f * s, background);
            int text = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_TEXT, alpha),
                    RenderUtil.withAlpha(MenuScreen.CHIP_TEXT_ON, alpha), selectedAnim);
            CustomFontRenderer.drawCenteredText(context, chip.label, cx + chip.w * s * 0.5f,
                    cy + 4.1f * s, 5.8f * s, text, FontWeight.SEMIBOLD);
        }
    }

    private void renderSelect(DrawContext context, SelectSetting setting, float x, float y, float w, float s,
                              int mouseX, int mouseY, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 5f * s, s, alpha);
        for (Chip chip : selectChips(setting, w / s - 8f)) {
            boolean selected = setting.isSelected(chip.value);
            float cx = x + (4f + chip.x) * s, cy = y + (16f + chip.y) * s;
            boolean hovered = hit(mouseX, mouseY, cx, cy, chip.w * s, 14f * s);
            float selectedAnim = control(setting, chip.value, "selected", selected ? 1f : 0f, 0.24f);
            float hoverAnim = control(setting, chip.value, "hover", hovered ? 1f : 0f, 0.30f);
            int background = RenderUtil.lerpColor(
                    RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_BG, 0.84f * alpha),
                            RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.CHIP_BG, 0.09f), 0.84f * alpha), hoverAnim),
                    RenderUtil.withAlpha(MenuScreen.ACCENT, 0.91f * alpha), selectedAnim);
            RenderUtil.drawRoundedRect(context, cx, cy, chip.w * s, 14f * s, 4f * s, background);
            int text = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_TEXT, alpha),
                    RenderUtil.withAlpha(MenuScreen.CHIP_TEXT_ON, alpha), selectedAnim);
            CustomFontRenderer.drawCenteredText(context, chip.label, cx + chip.w * s * 0.5f, cy + 4.5f * s,
                    5.8f * s, text, FontWeight.SEMIBOLD);
        }
    }

    private void renderItem(DrawContext context, ItemSelectSetting setting, float x, float y, float w, float s,
                            int mouseX, int mouseY, float alpha) {
        float progress = choiceProgress(setting);
        renderChoiceHeader(context, setting, setting.getSelectedLabel(), x, y, w, s, alpha, progress);
        if (progress > 0.01f) renderItemOptions(context, setting, false, x, y + 31f * s, w, s,
                mouseX, mouseY, alpha * RenderUtil.easeOutCubic(progress), progress);
    }

    private void renderMultiItem(DrawContext context, MultiItemSelectSetting setting, float x, float y, float w, float s,
                                 int mouseX, int mouseY, float alpha) {
        String value = String.format(ClientLanguage.text("ui.selected_count", "Выбрано: %d"), setting.get().size());
        float progress = choiceProgress(setting);
        renderChoiceHeader(context, setting, value, x, y, w, s, alpha, progress);
        if (progress > 0.01f) renderItemOptions(context, setting, true, x, y + 31f * s, w, s,
                mouseX, mouseY, alpha * RenderUtil.easeOutCubic(progress), progress);
    }

    private void renderChoiceHeader(DrawContext context, Setting<?> setting, String value, float x, float y, float w,
                                    float s, float alpha, float progress) {
        label(context, setting.getName(), x + 4f * s, y + 4f * s, s, alpha);
        float fy = y + 14f * s;
        RenderUtil.drawRoundedRect(context, x + 4f * s, fy, w - 8f * s, 16f * s, 4.5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.90f * alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, 0.13f * alpha), progress));
        float valueX = x + 9f * s;
        if (setting instanceof ItemSelectSetting item) {
            ItemStack selectedStack = item.getItemStack(item.get());
            if (!selectedStack.isEmpty()) {
                context.getMatrices().push();
                float itemScale = 0.68f * s;
                context.getMatrices().translate(x + 7f * s, fy + 1.8f * s, 20f);
                context.getMatrices().scale(itemScale, itemScale, 1f);
                context.drawItem(selectedStack, 0, 0);
                context.getMatrices().pop();
                valueX = x + 21f * s;
            }
        }
        value = CustomFontRenderer.trimToWidth(value, w - (valueX - x) - 19f * s, 5.6f * s, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, value, valueX, fy + 5.1f * s, 5.6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(context, progress > 0.5f ? "−" : "+", x + w - 9f * s,
                fy + 4.4f * s, (6f + progress * 0.7f) * s,
                RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SEMIBOLD);
    }

    private void renderItemOptions(DrawContext context, Setting<?> setting, boolean multi, float x, float y, float w, float s,
                                   int mouseX, int mouseY, float alpha, float progress) {
        y -= (1f - RenderUtil.easeOutCubic(progress)) * 6f * s;
        String query = multi ? ((MultiItemSelectSetting) setting).getSearchQuery() : ((ItemSelectSetting) setting).getSearchQuery();
        RenderUtil.drawRoundedRect(context, x + 4f * s, y, w - 8f * s, 18f * s, 4.5f * s,
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, 0.96f * alpha));
        String display = query.isEmpty() ? ClientLanguage.text("ui.search", "Поиск...") : query;
        if (query.isEmpty()) {
            CustomFontRenderer.drawText(context, display, x + 9f * s, y + 5.8f * s, 5.5f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        } else {
            RenderUtil.drawAnimatedInputText(context, display, x + 9f * s, y + 5.8f * s, 5.5f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD,
                    typedIndices.getOrDefault(setting, -1), typedAt.getOrDefault(setting, 0L));
        }
        List<Option> options = itemOptions(setting, multi);
        int targetStart = Math.min(optionScroll.getOrDefault(setting, 0), Math.max(0, options.size() - visibleOptions(setting)));
        float scroll = RenderUtil.animate(smoothOptionScroll.getOrDefault(setting, (float) targetStart),
                targetStart, 0.22f, RenderUtil.getFrameDelta());
        smoothOptionScroll.put(setting, scroll);
        int start = Math.max(0, Math.min(options.size(), (int) Math.floor(scroll)));
        float fraction = scroll - start;
        int count = Math.min(visibleOptions(setting) + 1, options.size() - start);
        for (int i = 0; i < count; i++) {
            Option option = options.get(start + i);
            float oy = y + (20f + (i - fraction) * 17f) * s;
            if (oy < y + 18f * s || oy > y + (20f + visibleOptions(setting) * 17f) * s) continue;
            float rowReveal = clamp(progress * 1.35f - i * 0.055f);
            float rowShift = (1f - RenderUtil.easeOutQuart(rowReveal)) * 7f * s;
            float rowAlpha = alpha * RenderUtil.easeOutCubic(rowReveal);
            boolean selected = multi ? ((MultiItemSelectSetting) setting).isSelected(option.id)
                    : option.id.equals(((ItemSelectSetting) setting).get());
            boolean hovered = hit(mouseX, mouseY, x + 4f * s, oy, w - 8f * s, 16f * s);
            float selectedAnim = control(setting, option.id, "item-selected", selected ? 1f : 0f, 0.23f);
            float hoverAnim = control(setting, option.id, "item-hover", hovered ? 1f : 0f, 0.29f);
            if (selectedAnim > 0.01f || hoverAnim > 0.01f) {
                RenderUtil.drawRoundedRect(context, x + 4f * s + rowShift, oy, w - 8f * s, 16f * s, 4f * s,
                        RenderUtil.lerpColor(RenderUtil.withAlpha(0xFFFFFF, rowAlpha * 0.04f * hoverAnim),
                                RenderUtil.withAlpha(MenuScreen.ACCENT, rowAlpha * 0.20f), selectedAnim));
            }
            ItemStack stack = itemStack(setting, option.id);
            float textX = x + 9f * s;
            if (!stack.isEmpty()) {
                context.getMatrices().push();
                float itemScale = 0.68f * s;
                context.getMatrices().translate(x + 7f * s + rowShift, oy + 2.2f * s, 20f);
                context.getMatrices().scale(itemScale, itemScale, 1f);
                context.drawItem(stack, 0, 0);
                context.getMatrices().pop();
                textX = x + 21f * s + rowShift;
            } else {
                textX += rowShift;
            }
            String label = CustomFontRenderer.trimToWidth(option.label, w - (textX - x) - 18f * s, 5.5f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, label, textX, oy + 5.3f * s, 5.5f * s,
                    RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, rowAlpha),
                            RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, rowAlpha), selectedAnim), FontWeight.SEMIBOLD);
            if (selectedAnim > 0.01f) RenderUtil.drawCircle(context, x + w - 11f * s + rowShift, oy + 8f * s,
                    (1.4f + selectedAnim * 1.2f) * s, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, rowAlpha * selectedAnim));
        }
    }

    private void renderColor(DrawContext context, ColorSetting setting, float x, float y, float w, float s, float alpha) {
        SliderSetting linkedAlpha = linkedAlphaSetting();
        if (linkedAlpha != null && screen.isPickerOpenFor(setting)) linkedAlpha.setFromProgress(setting.getAlphaF());
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        float open = control(setting, "picker", "open", screen.isPickerOpenFor(setting) ? 1f : 0f, 0.24f);
        float swatch = (14f + open * 1.1f) * s, sx = x + w - swatch - 4f * s, sy = y + (4f - open * 0.55f) * s;
        float radius = 4.5f * s;
        RenderUtil.drawRoundedRect(context, sx, sy, swatch, swatch, radius,
                RenderUtil.withAlpha(MenuScreen.INSET_BG, alpha));
        RenderUtil.drawRoundedRect(context, sx, sy, swatch, swatch, radius,
                RenderUtil.withAlpha(setting.get() & 0xFFFFFF, alpha * setting.getAlphaF()));
        CustomFontRenderer.drawRightText(context, String.format("#%06X", setting.get() & 0xFFFFFF), sx - 7f * s,
                y + 7.9f * s, 5.3f * s, RenderUtil.withAlpha(MenuScreen.VALUE_DIM, 0.86f * alpha), FontWeight.SEMIBOLD);
    }

    private void renderString(DrawContext context, StringSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 4f * s, s, alpha);
        float fy = y + 14f * s;
        boolean editing = editingString == setting;
        float focus = control(setting, "text", "focus", editing ? 1f : 0f, 0.24f);
        RenderUtil.drawRoundedRect(context, x + 4f * s, fy, w - 8f * s, 16f * s, 4.5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, alpha * 0.82f),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * 0.14f), focus));
        String value = setting.get().isEmpty() ? setting.getPlaceholder() : setting.get();
        value = CustomFontRenderer.trimToWidth(value, w - 18f * s, 5.6f * s, FontWeight.SEMIBOLD);
        if (setting.get().isEmpty()) {
            CustomFontRenderer.drawText(context, value, x + 9f * s, fy + 5.1f * s, 5.6f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        } else {
            RenderUtil.drawAnimatedInputText(context, value, x + 9f * s, fy + 5.1f * s, 5.6f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD,
                    typedIndices.getOrDefault(setting, -1), typedAt.getOrDefault(setting, 0L));
        }
        if (editing && (System.currentTimeMillis() / 500L) % 2 == 0) {
            float cx = x + 9f * s + CustomFontRenderer.getTextWidth(value, 5.6f * s, FontWeight.SEMIBOLD) + 1f;
            RenderUtil.drawRect(context, Math.min(cx, x + w - 10f * s), fy + 4f * s, 0.7f, 8f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha));
        }
    }

    private void renderBind(DrawContext context, BindSetting setting, float x, float y, float w, float s, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 8f * s, s, alpha);
        boolean binding = screen.isBindingSetting(setting);
        float bindingAnim = control(setting, "bind", "active", binding ? 1f : 0f, 0.23f);
        String value = binding ? "..." : setting.getKeyName();
        float bw = Math.max(31f * s, CustomFontRenderer.getTextWidth(value, 5.6f * s, FontWeight.SEMIBOLD) + 15f * s);
        RenderUtil.drawRoundedRect(context, x + w - bw - 4f * s, y + 4f * s, bw, 16f * s, 5f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_BG, alpha * 0.88f),
                        RenderUtil.withAlpha(MenuScreen.ACCENT, alpha * 0.30f), bindingAnim));
        CustomFontRenderer.drawCenteredText(context, value, x + w - bw * 0.5f - 4f * s, y + 9f * s,
                (5.6f + bindingAnim * 0.3f) * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha),
                        RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), bindingAnim), FontWeight.SEMIBOLD);
    }

    private void renderClick(DrawContext context, ClickSetting setting, float x, float y, float w, float s,
                             float alpha, float hover) {
        int background = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.CHIP_BG, 0.92f * alpha),
                RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.CHIP_BG, 0.09f), 0.92f * alpha), hover);
        RenderUtil.drawRoundedRect(context, x + 4f * s, y + 4f * s, w - 8f * s,
                20f * s, 5.5f * s, background);
        CustomFontRenderer.drawCenteredText(context, setting.getName(), x + w * 0.5f, y + 10.7f * s,
                6.4f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), FontWeight.SEMIBOLD);
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
            float toggleX = drawX + width - 26f * scale;
            float gearX = toggleX - 11f * scale;
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                    || hit(mouseX, mouseY, bindIconX - 6f * scale, drawY + 2f * scale, bindHitW, 21f * scale)) {
                if (blocked()) shake(); else screen.requestModuleBind(module);
                return false;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || hit(mouseX, mouseY, gearX - 5.5f * scale, drawY + 4f * scale, 11f * scale, 22f * scale)) {
                if (!displaySettings().isEmpty()) {
                    if (blocked()) shake(); else screen.toggleExpanded(this);
                }
                return false;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (blocked() || !module.isAvailable() && !module.isEnabled()) shake(); else module.toggle();
            }
            return false;
        }
        if (!expanded || expansionValue() < 0.72f) return false;

        SettingHit hit = findSetting(mouseX, mouseY, scale);
        if (hit == null) return false;
        Setting<?> setting = hit.setting;
        if (setting instanceof ColorSetting color
                && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            SliderSetting linkedAlpha = linkedAlphaSetting();
            if (linkedAlpha != null && !screen.isPickerOpenFor(color)) {
                color.setAlpha(Math.round(linkedAlpha.getProgress() * 255f));
            }
            screen.openColorPicker(color, hit.x + hit.w - 2f * scale, hit.y + 3f * scale);
            EnoughSoundPlayer.playScroll();
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float localX = (mouseX - hit.x) / scale;
        float localY = (mouseY - hit.y) / scale;
        float baseW = hit.w / scale;
        if (setting instanceof BooleanSetting value) value.toggle();
        else if (setting instanceof SliderSetting value) {
            activeDrag = setting;
            updateSlider(value, localX, baseW);
            EnoughSoundPlayer.playSlider();
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
            choiceAnimations.putIfAbsent(setting, 0f);
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
            if (!Character.isISOControl(chr) && editingString.isCharAllowed(chr)) {
                editingString.set(editingString.get() + chr);
                typedIndices.put(editingString, editingString.get().length() - 1);
                typedAt.put(editingString, System.currentTimeMillis());
            }
            return true;
        }
        if (editingChoice != null) {
            if (!Character.isISOControl(chr) && choiceQuery(editingChoice).length() < 36) {
                setChoiceQuery(editingChoice, choiceQuery(editingChoice) + chr);
                typedIndices.put(editingChoice, choiceQuery(editingChoice).length() - 1);
                typedAt.put(editingChoice, System.currentTimeMillis());
                optionScroll.put(editingChoice, 0);
            }
            return true;
        }
        return false;
    }

    private void updateSlider(SliderSetting setting, float localX, float w) {
        float before = setting.getProgress();
        setting.setFromProgress(clamp((localX - 4f) / (w - 8f)));
        if (Math.abs(setting.getProgress() - before) >= 0.008f) EnoughSoundPlayer.playSlider();
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
        for (Setting<?> setting : displaySettings()) {
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
        for (Setting<?> setting : displaySettings()) {
            float rowH = rowHeight(setting, baseW) * scale;
            if (setting == target) return new SettingHit(setting, rowX, rowY, rowW, rowH);
            rowY += rowH;
        }
        return null;
    }

    private float contentHeight(float width) {
        float height = 9f;
        for (Setting<?> setting : displaySettings()) height += rowHeight(setting, width);
        return height;
    }

    private List<Setting<?>> displaySettings() {
        List<Setting<?>> source = module.getVisibleSettings();
        boolean hasColor = source.stream().anyMatch(ColorSetting.class::isInstance);
        if (!hasColor) return source;
        List<Setting<?>> result = new ArrayList<>(source.size());
        for (Setting<?> setting : source) {
            if (!(setting instanceof SliderSetting) || !isLinkedAlphaId(setting.getId())) result.add(setting);
        }
        return result;
    }

    private SliderSetting linkedAlphaSetting() {
        for (Setting<?> setting : module.getVisibleSettings()) {
            if (setting instanceof SliderSetting slider && isLinkedAlphaId(setting.getId())) return slider;
        }
        return null;
    }

    private static boolean isLinkedAlphaId(String id) {
        String value = id == null ? "" : id.toLowerCase();
        return value.equals("alpha") || value.equals("opacity") || value.endsWith("_alpha")
                || value.endsWith("_opacity") || value.equals("transparency");
    }

    private float rowHeight(Setting<?> setting, float width) {
        if (setting instanceof BooleanSetting || setting instanceof ColorSetting) return 25f;
        if (setting instanceof SliderSetting) return 27f;
        if (setting instanceof RangeSetting) return 30f;
        if (setting instanceof BindSetting) return 26f;
        if (setting instanceof StringSetting) return 35f;
        if (setting instanceof ClickSetting) return 29f;
        if (setting instanceof BezierSetting) return 72f;
        if (setting instanceof ModeSetting value) return 19f + chipRows(modeChips(value, width - 8f)) * 17f;
        if (setting instanceof SelectSetting value) return 19f + chipRows(selectChips(value, width - 8f)) * 17f;
        if (setting instanceof ItemSelectSetting || setting instanceof MultiItemSelectSetting) {
            int count = Math.min(visibleOptions(setting), itemOptions(setting, setting instanceof MultiItemSelectSetting).size());
            return 35f + (19f + count * 17f) * choiceProgress(setting);
        }
        return 25f;
    }

    private List<Chip> modeChips(ModeSetting setting, float available) {
        List<Chip> chips = new ArrayList<>();
        float x = 0f, y = 0f;
        for (String option : setting.getModes()) {
            String label = setting.getDisplayMode(option);
            float w = Math.max(28f, CustomFontRenderer.getTextWidth(label, 5.5f, FontWeight.SEMIBOLD) + 12f);
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
            float w = Math.max(28f, CustomFontRenderer.getTextWidth(label, 5.5f, FontWeight.SEMIBOLD) + 12f);
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

    private ItemStack itemStack(Setting<?> setting, String id) {
        if (setting instanceof MultiItemSelectSetting multi) return multi.getItemStack(id);
        if (setting instanceof ItemSelectSetting single) return single.getItemStack(id);
        return ItemStack.EMPTY;
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
        feedbackShadowAt = System.currentTimeMillis();
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

    private float control(Setting<?> setting, String value, String channel, float target, float speed) {
        String key = System.identityHashCode(setting) + ":" + value + ":" + channel;
        float animation = RenderUtil.animate(controlAnimations.getOrDefault(key, target), target,
                speed, RenderUtil.getFrameDelta());
        controlAnimations.put(key, animation);
        return animation;
    }

    private float choiceProgress(Setting<?> setting) {
        float target = openChoice == setting ? 1f : 0f;
        float animation = RenderUtil.animate(choiceAnimations.getOrDefault(setting, target), target,
                target > 0f ? 0.24f : 0.18f, RenderUtil.getFrameDelta());
        choiceAnimations.put(setting, animation);
        return animation;
    }

    private static void label(DrawContext context, String text, float x, float y, float scale, float alpha) {
        CustomFontRenderer.drawText(context, text, x, y, 6.4f * scale,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.94f * alpha), FontWeight.SEMIBOLD);
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
