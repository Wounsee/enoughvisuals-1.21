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
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleCard {
    private static final float HEADER_H = 41f;
    private static final float DROPDOWN_LABEL_H = 12.5f;
    private static final float DROPDOWN_CLOSED_H = 31f;
    private static final float DROPDOWN_HEADER_H = 16.5f;
    private static final float DROPDOWN_OPTION_H = 13.5f;
    private static final float CHOICE_LABEL_H = 12.5f;
    private static final float CHOICE_HEADER_H = 16.5f;
    private static final float CHOICE_CLOSED_H = 31f;
    private static final float CHOICE_SEARCH_H = 17f;
    private static final float CHOICE_OPTION_H = 15f;
    private static final float SLIDER_TRACK_X = 4f;
    private static final float SLIDER_TRACK_RIGHT = 4f;
    private static final String ICON_GEAR = "\uF007";
    private static final String ICON_BIND = "\uF005";

    private final Module module;
    private final MenuScreen screen;
    private final Map<Setting<?>, Float> hoverAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> valueAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> secondaryAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> choiceAnimations = new HashMap<>();
    private final Map<Setting<?>, Float> dropdownAnimations = new HashMap<>();
    private final Map<Setting<?>, Boolean> booleanStates = new HashMap<>();
    private final Map<Setting<?>, Long> booleanChangedAt = new HashMap<>();
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

    public TransientState captureTransientState() {
        String openSettingId = openChoice == null ? null : openChoice.getId();
        int savedOptionScroll = openChoice == null ? 0 : optionScroll.getOrDefault(openChoice, 0);
        return new TransientState(openSettingId, savedOptionScroll);
    }

    public void restoreTransientState(TransientState state) {
        expanded = true;
        expansionValue = 1f;
        expansionFrom = 1f;
        expansionTarget = 1f;
        expansionStartedAt = System.currentTimeMillis();
        expansionDuration = 1L;
        openChoice = null;
        dropdownAnimations.clear();
        if (state == null || state.openSettingId() == null) return;
        Setting<?> setting = module.getSettingById(state.openSettingId());
        if (setting == null || !setting.isVisible()) return;
        openChoice = setting;
        dropdownAnimations.put(setting, 1f);
        if (state.optionScroll() > 0) optionScroll.put(setting, state.optionScroll());
    }

    public record TransientState(String openSettingId, int optionScroll) {
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
        return (HEADER_H + (total + 2f) * e) * scale;
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
        float intro = clamp((now - introStart) / 360f);
        float reveal = intro;
        drawX = x;
        drawY = y + (1f - RenderUtil.easeOutQuart(reveal)) * 14f * scale;
        float alpha = parentAlpha * RenderUtil.easeOutExpo(reveal);
        float frame = RenderUtil.getFrameDelta();
        updateDropdownAnimations(frame);
        float height = currentHeight(scale);
        boolean hovered = contains(mouseX, mouseY, scale);
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
        if ("optimization".equals(module.getId())) {
            renderPremiumShimmer(context, height, scale, alpha, now);
        }

        renderHeader(context, scale, mouseX, mouseY, alpha, now);
        float expand = clamp(expansionValue());
        if (expand > 0.005f) renderSettings(context, scale, mouseX, mouseY, alpha * RenderUtil.easeOutQuad(expand), expand);
    }

    private void renderPremiumShimmer(DrawContext context, float height, float scale, float alpha, long now) {
        float phase = (now % 2_800L) / 2_800f;
        float travel = width + 76f * scale;
        float centerX = drawX - 38f * scale + travel * RenderUtil.easeInOutCubic(phase);
        float stripeWidth = 18f * scale;

        context.draw();
        StencilUtil.enableScissor(drawX, drawY, width, height);
        context.getMatrices().push();
        context.getMatrices().translate(centerX, drawY + height * 0.5f, 0f);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-17f));
        float stripeHeight = height + 52f * scale;
        RenderUtil.drawRoundedGradientH(context, -stripeWidth, -stripeHeight * 0.5f,
                stripeWidth, stripeHeight, 3f * scale,
                RenderUtil.withAlpha(0xFFFFFF, 0f),
                RenderUtil.withAlpha(0xFFFFFF, 0.115f * alpha));
        RenderUtil.drawRoundedGradientH(context, 0f, -stripeHeight * 0.5f,
                stripeWidth, stripeHeight, 3f * scale,
                RenderUtil.withAlpha(0xFFFFFF, 0.115f * alpha),
                RenderUtil.withAlpha(0xFFFFFF, 0f));
        context.getMatrices().pop();
        context.draw();
        StencilUtil.disableScissor();

        float pulse = 0.5f + 0.5f * (float) Math.sin(now / 430.0);
        RenderUtil.drawRoundedOutline(context, drawX + 0.55f * scale, drawY + 0.55f * scale,
                width - 1.1f * scale, height - 1.1f * scale, 8f * scale, 0.65f * scale,
                RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha * (0.13f + pulse * 0.07f)));
    }

    private void renderHeader(DrawContext context, float scale, int mouseX, int mouseY, float alpha, long now) {
        boolean blocked = ModuleManager.getInstance().isBlocked(module);
        boolean available = !blocked && module.isAvailable();
        float headerH = HEADER_H * scale;
        float toggleW = 18f * scale;
        float toggleH = 9f * scale;
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

        renderAnimatedToggle(context, toggleX, toggleY, toggleW, toggleH, toggleAnimation,
                module.isEnabled(), toggleChangedAt, scale, alpha);
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
        boolean previous = booleanStates.getOrDefault(setting, setting.get());
        if (previous != setting.get()) {
            booleanStates.put(setting, setting.get());
            booleanChangedAt.put(setting, System.currentTimeMillis());
        } else {
            booleanStates.putIfAbsent(setting, setting.get());
        }
        float animation = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.get() ? 1f : 0f),
                setting.get() ? 1f : 0f, 0.22f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, animation);
        float tw = 18f * s, th = 9f * s;
        float tx = x + w - tw - 4f * s, ty = y + 8f * s;
        renderAnimatedToggle(context, tx, ty, tw, th, animation, setting.get(),
                booleanChangedAt.getOrDefault(setting, 0L), s, alpha);
    }

    private static void drawCapsule(DrawContext context, float x, float y,
                                    float width, float height, int color) {
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(context, x, y, width, height,
                    Math.min(width, height) * 0.5f, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
    }

    private static void drawCapsuleOutline(DrawContext context, float x, float y,
                                           float width, float height, float lineWidth, int color) {
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedOutline(context, x, y, width, height,
                    Math.min(width, height) * 0.5f, lineWidth, color);
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
    }

    private static void renderAnimatedToggle(DrawContext context, float x, float y, float width, float height,
                                             float animation, boolean enabled, long changedAt,
                                             float scale, float alpha) {
        float progress = RenderUtil.easeInOutCubic(clamp(animation));
        float switchProgress = changedAt == 0L ? 1f : clamp((System.currentTimeMillis() - changedAt) / 310f);
        float pulse = changedAt == 0L ? 0f
                : (float) Math.sin(switchProgress * Math.PI) * (1f - switchProgress);
        float motion = clamp(Math.abs((enabled ? 1f : 0f) - animation) * 4.2f);
        float trackX = x - pulse * 0.22f * scale;
        float trackW = width + pulse * 0.44f * scale;
        int track = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.TOGGLE_OFF, 0.90f * alpha),
                RenderUtil.withAlpha(MenuScreen.TOGGLE_ON, 0.92f * alpha), progress);
        drawCapsule(context, trackX, y, trackW, height, track);
        drawCapsuleOutline(context, trackX, y, trackW, height, 0.45f * scale,
                RenderUtil.withAlpha(0xFFFFFF, 0.065f * alpha));

        float knobW = (7.25f + motion * 1.05f + pulse * 0.35f) * scale;
        float knobH = (5.25f - motion * 0.24f + pulse * 0.10f) * scale;
        float inset = 1.25f * scale;
        float left = trackX + inset;
        float right = trackX + trackW - inset - knobW;
        float knobX = left + (right - left) * progress;
        float knobY = y + (height - knobH) * 0.5f;
        drawCapsule(context, knobX, knobY, knobW, knobH,
                RenderUtil.withAlpha(MenuScreen.TOGGLE_KNOB, alpha * (0.78f + progress * 0.20f)));
    }

    private void renderSlider(DrawContext context, SliderSetting setting, float x, float y, float w, float s,
                              float alpha, float hover) {
        float progress = RenderUtil.animate(valueAnimations.getOrDefault(setting, setting.getProgress()),
                setting.getProgress(), activeDrag == setting ? 0.48f : 0.24f, RenderUtil.getFrameDelta());
        valueAnimations.put(setting, progress);
        double animatedValue = setting.getMin() + (setting.getMax() - setting.getMin()) * progress;
        String value = format(animatedValue, setting.getStep()) + setting.getSuffix();
        float textSize = 5.8f * s;
        value = CustomFontRenderer.trimToWidth(value, 34f * s, textSize, FontWeight.SEMIBOLD);
        float valueWidth = CustomFontRenderer.getTextWidth(value, textSize, FontWeight.SEMIBOLD);
        String settingName = CustomFontRenderer.trimToWidth(setting.getName(),
                Math.max(12f * s, w - valueWidth - 16f * s), 6.4f * s, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, settingName, x + 4f * s, y + 5f * s, 6.4f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.94f * alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(context, value, x + w - 4f * s, y + 5.2f * s, textSize,
                RenderUtil.withAlpha(MenuScreen.VALUE_DIM, alpha), FontWeight.SEMIBOLD);

        float sx = x + SLIDER_TRACK_X * s;
        float sw = Math.max(12f * s, w - (SLIDER_TRACK_X + SLIDER_TRACK_RIGHT) * s);
        float movement = clamp(Math.abs(setting.getProgress() - progress) * 8f);
        float trackH = (2.6f + hover * 0.10f + movement * 0.08f) * s;
        float sy = y + 18.5f * s;
        int track = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.SLIDER_BG, 0.92f * alpha),
                RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.SLIDER_BG, 0.10f), alpha), hover);
        drawCapsule(context, sx, sy, sw, trackH, track);
        if (progress > 0.001f) {
            int fill = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.ACCENT, 0.88f * alpha),
                    RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), hover * 0.28f);
            drawCapsule(context, sx, sy, Math.max(trackH, sw * progress), trackH, fill);
        }
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
        label(context, setting.getName(), x + 4f * s, y + 2.9f * s, s, alpha);
        float progress = dropdownProgress(setting);
        List<String> options = setting.getModes();
        renderDropdownFrame(context, setting, setting.getDisplayValue(), options.size(),
                x, y, w, s, mouseX, mouseY, alpha, progress);
        renderDropdownOptions(context, setting, options, true, x, y, w, s,
                mouseX, mouseY, alpha, progress);
    }

    private void renderSelect(DrawContext context, SelectSetting setting, float x, float y, float w, float s,
                              int mouseX, int mouseY, float alpha) {
        label(context, setting.getName(), x + 4f * s, y + 2.9f * s, s, alpha);
        float progress = dropdownProgress(setting);
        List<String> options = setting.getOptions();
        String summary = selectSummary(setting);
        renderDropdownFrame(context, setting, summary, options.size(),
                x, y, w, s, mouseX, mouseY, alpha, progress);
        renderDropdownOptions(context, setting, options, false, x, y, w, s,
                mouseX, mouseY, alpha, progress);
    }

    private void renderDropdownFrame(DrawContext context, Setting<?> setting, String summary, int optionCount,
                                     float x, float y, float w, float s, int mouseX, int mouseY,
                                     float alpha, float progress) {
        float panelX = x + 4f * s;
        float panelY = y + DROPDOWN_LABEL_H * s;
        float panelW = w - 8f * s;
        float panelH = (DROPDOWN_HEADER_H + dropdownExtraHeight(optionCount) * progress) * s;
        boolean headerHovered = hit(mouseX, mouseY, panelX, panelY, panelW, DROPDOWN_HEADER_H * s);
        float hover = control(setting, "dropdown", "header-hover", headerHovered ? 1f : 0f, 0.28f);
        int background = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.93f * alpha),
                RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.INSET_BG, 0.025f), 0.96f * alpha), hover);
        background = RenderUtil.lerpColor(background,
                RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.INSET_BG, 0.05f), 0.97f * alpha), progress * 0.28f);
        RenderUtil.drawRoundedRect(context, panelX, panelY, panelW, panelH, 6.8f * s, background);

        String display = CustomFontRenderer.trimToWidth(summary, panelW - 29f * s,
                6.1f * s, FontWeight.SEMIBOLD);
        int summaryColor = RenderUtil.lerpColor(
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * (0.94f + hover * 0.06f)),
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha), Math.max(progress, hover * 0.42f));
        CustomFontRenderer.drawText(context, display, panelX + 7f * s, panelY + 4.25f * s,
                6.1f * s, summaryColor, FontWeight.SEMIBOLD);

        int arrowColor = RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.92f * alpha),
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), Math.max(progress, hover));
        drawDropdownChevron(context, panelX + panelW - 9f * s,
                panelY + DROPDOWN_HEADER_H * 0.5f * s, 6.5f * s, progress, arrowColor);

        if (progress > 0.01f) {
            float dividerY = panelY + DROPDOWN_HEADER_H * s;
            RenderUtil.drawRoundedRect(context, panelX + 7f * s, dividerY,
                    panelW - 14f * s, 0.6f * s, 0.3f * s,
                    RenderUtil.withAlpha(0xFFFFFF, 0.075f * alpha * progress));
        }
    }

    private void renderDropdownOptions(DrawContext context, Setting<?> setting, List<String> options, boolean single,
                                       float x, float y, float w, float s, int mouseX, int mouseY,
                                       float alpha, float progress) {
        if (progress <= 0.01f || options.isEmpty()) return;

        float panelX = x + 4f * s;
        float panelY = y + DROPDOWN_LABEL_H * s;
        float panelW = w - 8f * s;
        float panelH = (DROPDOWN_HEADER_H + dropdownExtraHeight(options.size()) * progress) * s;
        context.draw();
        StencilUtil.enableScissor(panelX, panelY, panelW, panelH);
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            float reveal = clamp(progress * 1.22f - i * 0.055f);
            if (reveal <= 0.001f) continue;
            float rowY = y + (DROPDOWN_CLOSED_H + i * DROPDOWN_OPTION_H) * s;
            float rowH = DROPDOWN_OPTION_H * s;
            boolean hovered = hit(mouseX, mouseY, panelX + 4f * s, rowY,
                    panelW - 8f * s, rowH) && mouseY <= panelY + panelH;
            boolean selected = single ? ((ModeSetting) setting).is(option)
                    : ((SelectSetting) setting).isSelected(option);
            float selectedAnim = control(setting, option, "dropdown-selected", selected ? 1f : 0f, 0.23f);
            float hoverAnim = control(setting, option, "dropdown-hover", hovered ? 1f : 0f, 0.31f);
            float shift = (1f - RenderUtil.easeOutQuart(reveal)) * 4f * s;
            float rowEmphasis = Math.max(selectedAnim * 0.72f, hoverAnim);
            if (rowEmphasis > 0.01f) {
                RenderUtil.drawRoundedRect(context, panelX + 4f * s, rowY + 1f * s,
                        panelW - 8f * s, rowH - 2f * s, 5f * s,
                        RenderUtil.withAlpha(0xFFFFFF, 0.026f * alpha * reveal * rowEmphasis));
            }
            int textColor = RenderUtil.lerpColor(
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.92f * alpha * reveal),
                    RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * reveal),
                    Math.max(selectedAnim, hoverAnim * 0.55f));
            String display = dropdownOptionLabel(setting, option);
            display = CustomFontRenderer.trimToWidth(display, panelW - 18f * s,
                    5.8f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, display, panelX + (7f * s) + shift,
                    rowY + 3.55f * s, 5.8f * s, textColor, FontWeight.SEMIBOLD);
        }
        context.draw();
        StencilUtil.disableScissor();
    }

    private static float dropdownExtraHeight(int optionCount) {
        return 2f + Math.max(0, optionCount) * DROPDOWN_OPTION_H;
    }

    private static String dropdownOptionLabel(Setting<?> setting, String option) {
        if (setting instanceof ModeSetting mode) return mode.getDisplayMode(option);
        return ((SelectSetting) setting).getDisplayOption(option);
    }

    private static String selectSummary(SelectSetting setting) {
        List<String> selected = new ArrayList<>();
        for (String option : setting.getOptions()) {
            if (setting.isSelected(option)) selected.add(setting.getDisplayOption(option));
        }
        return selected.isEmpty()
                ? ClientLanguage.text("ui.none_selected", "Не выбрано")
                : String.join(", ", selected);
    }

    private static void drawDropdownChevron(DrawContext context, float centerX, float centerY,
                                            float size, float progress, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0f);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                90f - 180f * RenderUtil.easeInOutCubic(clamp(progress))));
        float textY = -CustomFontRenderer.getTextHeight(size, FontWeight.SOLAR_ICONS) * 0.5f;
        CustomFontRenderer.drawCenteredText(context, SolarIcons.CHEVRON_RIGHT, 0f, textY,
                size, color, FontWeight.SOLAR_ICONS);
        context.getMatrices().pop();
    }

    private void renderItem(DrawContext context, ItemSelectSetting setting, float x, float y, float w, float s,
                            int mouseX, int mouseY, float alpha) {
        float progress = choiceProgress(setting);
        renderChoiceHeader(context, setting, setting.getSelectedLabel(), x, y, w, s, alpha, progress);
        if (progress > 0.01f) renderItemOptions(context, setting, false, x, y + CHOICE_CLOSED_H * s, w, s,
                mouseX, mouseY, alpha * RenderUtil.easeOutCubic(progress), progress);
    }

    private void renderMultiItem(DrawContext context, MultiItemSelectSetting setting, float x, float y, float w, float s,
                                 int mouseX, int mouseY, float alpha) {
        String value = String.format(ClientLanguage.text("ui.selected_count", "Выбрано: %d"), setting.get().size());
        float progress = choiceProgress(setting);
        renderChoiceHeader(context, setting, value, x, y, w, s, alpha, progress);
        if (progress > 0.01f) renderItemOptions(context, setting, true, x, y + CHOICE_CLOSED_H * s, w, s,
                mouseX, mouseY, alpha * RenderUtil.easeOutCubic(progress), progress);
    }

    private void renderChoiceHeader(DrawContext context, Setting<?> setting, String value, float x, float y, float w,
                                    float s, float alpha, float progress) {
        label(context, setting.getName(), x + 4f * s, y + 2.9f * s, s, alpha);
        float fy = y + CHOICE_LABEL_H * s;
        RenderUtil.drawRoundedRect(context, x + 4f * s, fy, w - 8f * s, CHOICE_HEADER_H * s, 6.8f * s,
                RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.INSET_BG, 0.90f * alpha),
                        RenderUtil.withAlpha(RenderUtil.lighten(MenuScreen.INSET_BG, 0.05f), 0.96f * alpha), progress * 0.45f));
        float valueX = x + 9f * s;
        if (setting instanceof ItemSelectSetting item) {
            ItemStack selectedStack = item.getItemStack(item.get());
            if (!selectedStack.isEmpty()) {
                context.getMatrices().push();
                float itemScale = 0.61f * s;
                context.getMatrices().translate(x + 7f * s, fy + 2.5f * s, 20f);
                context.getMatrices().scale(itemScale, itemScale, 1f);
                context.drawItem(selectedStack, 0, 0);
                context.getMatrices().pop();
                valueX = x + 21f * s;
            }
        }
        value = CustomFontRenderer.trimToWidth(value, w - (valueX - x) - 19f * s, 5.6f * s, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, value, valueX, fy + 5f * s, 5.6f * s,
                RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, alpha), FontWeight.SEMIBOLD);
        drawDropdownChevron(context, x + w - 10f * s, fy + CHOICE_HEADER_H * 0.5f * s,
                6.5f * s, progress, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.94f * alpha));
    }

    private void renderItemOptions(DrawContext context, Setting<?> setting, boolean multi, float x, float y, float w, float s,
                                   int mouseX, int mouseY, float alpha, float progress) {
        y -= (1f - RenderUtil.easeOutCubic(progress)) * 6f * s;
        String query = multi ? ((MultiItemSelectSetting) setting).getSearchQuery() : ((ItemSelectSetting) setting).getSearchQuery();
        RenderUtil.drawRoundedRect(context, x + 4f * s, y, w - 8f * s, CHOICE_SEARCH_H * s, 6f * s,
                RenderUtil.withAlpha(MenuScreen.SETTING_INPUT_BG, 0.96f * alpha));
        String display = query.isEmpty() ? ClientLanguage.text("ui.search", "Поиск...") : query;
        if (query.isEmpty()) {
            CustomFontRenderer.drawText(context, display, x + 9f * s, y + 5.3f * s, 5.5f * s,
                    RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha), FontWeight.SEMIBOLD);
        } else {
            RenderUtil.drawAnimatedInputText(context, display, x + 9f * s, y + 5.3f * s, 5.5f * s,
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
            float oy = y + (CHOICE_SEARCH_H + 2f + (i - fraction) * CHOICE_OPTION_H) * s;
            if (oy < y + CHOICE_SEARCH_H * s
                    || oy > y + (CHOICE_SEARCH_H + 2f + visibleOptions(setting) * CHOICE_OPTION_H) * s) continue;
            float rowReveal = clamp(progress * 1.35f - i * 0.055f);
            float rowShift = (1f - RenderUtil.easeOutQuart(rowReveal)) * 7f * s;
            float rowAlpha = alpha * RenderUtil.easeOutCubic(rowReveal);
            boolean selected = multi ? ((MultiItemSelectSetting) setting).isSelected(option.id)
                    : option.id.equals(((ItemSelectSetting) setting).get());
            boolean hovered = hit(mouseX, mouseY, x + 4f * s, oy, w - 8f * s, (CHOICE_OPTION_H - 1f) * s);
            float selectedAnim = control(setting, option.id, "item-selected", selected ? 1f : 0f, 0.23f);
            float hoverAnim = control(setting, option.id, "item-hover", hovered ? 1f : 0f, 0.29f);
            if (selectedAnim > 0.01f || hoverAnim > 0.01f) {
                RenderUtil.drawRoundedRect(context, x + 4f * s + rowShift, oy, w - 8f * s,
                        (CHOICE_OPTION_H - 1f) * s, 5f * s,
                        RenderUtil.lerpColor(RenderUtil.withAlpha(0xFFFFFF, rowAlpha * 0.04f * hoverAnim),
                                RenderUtil.withAlpha(MenuScreen.ACCENT, rowAlpha * 0.20f), selectedAnim));
            }
            ItemStack stack = itemStack(setting, option.id);
            float textX = x + 9f * s;
            if (!stack.isEmpty()) {
                context.getMatrices().push();
                float itemScale = 0.56f * s;
                context.getMatrices().translate(x + 7f * s + rowShift, oy + 2f * s, 20f);
                context.getMatrices().scale(itemScale, itemScale, 1f);
                context.drawItem(stack, 0, 0);
                context.getMatrices().pop();
                textX = x + 21f * s + rowShift;
            } else {
                textX += rowShift;
            }
            String label = CustomFontRenderer.trimToWidth(option.label, w - (textX - x) - 18f * s, 5.5f * s, FontWeight.SEMIBOLD);
            CustomFontRenderer.drawText(context, label, textX, oy + 4.2f * s, 5.5f * s,
                    RenderUtil.lerpColor(RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, rowAlpha),
                            RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, rowAlpha), selectedAnim), FontWeight.SEMIBOLD);
            if (selectedAnim > 0.01f) RenderUtil.drawCircle(context, x + w - 11f * s + rowShift,
                    oy + (CHOICE_OPTION_H - 1f) * 0.5f * s,
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
            float toggleX = drawX + width - 9f * scale - 18f * scale;
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
        if (setting instanceof BooleanSetting value) {
            value.toggle();
            return false;
        }
        else if (setting instanceof SliderSetting value) {
            if (localX < SLIDER_TRACK_X - 2f || localX > baseW - SLIDER_TRACK_RIGHT + 2f) return false;
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
            if (localX >= 4f && localX <= baseW - 4f
                    && localY >= DROPDOWN_LABEL_H
                    && localY <= DROPDOWN_LABEL_H + DROPDOWN_HEADER_H) {
                toggleDropdown(value);
                return false;
            }
            if (openChoice == value && localX >= 4f && localX <= baseW - 4f) {
                int index = dropdownOptionIndex(localY, value.getModes().size());
                if (index >= 0) {
                    value.set(value.getModes().get(index));
                    openChoice = null;
                    EnoughSoundPlayer.playScroll();
                    return false;
                }
            }
        } else if (setting instanceof SelectSetting value) {
            if (localX >= 4f && localX <= baseW - 4f
                    && localY >= DROPDOWN_LABEL_H
                    && localY <= DROPDOWN_LABEL_H + DROPDOWN_HEADER_H) {
                toggleDropdown(value);
                return false;
            }
            if (openChoice == value && localX >= 4f && localX <= baseW - 4f) {
                int index = dropdownOptionIndex(localY, value.getOptions().size());
                if (index >= 0) {
                    value.toggle(value.getOptions().get(index));
                    EnoughSoundPlayer.playScroll();
                    return false;
                }
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

    private void toggleDropdown(Setting<?> setting) {
        dropdownAnimations.putIfAbsent(setting, 0f);
        openChoice = openChoice == setting ? null : setting;
        editingChoice = null;
        EnoughSoundPlayer.playScroll();
    }

    private static int dropdownOptionIndex(float localY, int optionCount) {
        int index = (int) Math.floor((localY - DROPDOWN_CLOSED_H) / DROPDOWN_OPTION_H);
        return index >= 0 && index < optionCount ? index : -1;
    }

    private boolean handleItemClick(Setting<?> setting, boolean multi, float x, float y, float w) {
        if (y >= CHOICE_LABEL_H && y <= CHOICE_LABEL_H + CHOICE_HEADER_H) {
            choiceAnimations.putIfAbsent(setting, 0f);
            openChoice = openChoice == setting ? null : setting;
            editingChoice = openChoice;
            return true;
        }
        if (openChoice != setting) return false;
        if (y >= CHOICE_CLOSED_H && y <= CHOICE_CLOSED_H + CHOICE_SEARCH_H + 2f) {
            editingChoice = setting;
            return true;
        }
        int row = (int) ((y - CHOICE_CLOSED_H - CHOICE_SEARCH_H - 2f) / CHOICE_OPTION_H);
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
        setting.setFromProgress(clamp((localX - SLIDER_TRACK_X) /
                Math.max(1f, w - SLIDER_TRACK_X - SLIDER_TRACK_RIGHT)));
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
        float height = 5f;
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
        if (setting instanceof ModeSetting value) {
            return DROPDOWN_CLOSED_H + dropdownExtraHeight(value.getModes().size()) * dropdownProgress(value);
        }
        if (setting instanceof SelectSetting value) {
            return DROPDOWN_CLOSED_H + dropdownExtraHeight(value.getOptions().size()) * dropdownProgress(value);
        }
        if (setting instanceof ItemSelectSetting || setting instanceof MultiItemSelectSetting) {
            int count = Math.min(visibleOptions(setting), itemOptions(setting, setting instanceof MultiItemSelectSetting).size());
            return CHOICE_CLOSED_H + (CHOICE_SEARCH_H + 2f + count * CHOICE_OPTION_H) * choiceProgress(setting);
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

    private void updateDropdownAnimations(float frameDelta) {
        for (Setting<?> setting : displaySettings()) {
            if (!(setting instanceof ModeSetting) && !(setting instanceof SelectSetting)) continue;
            float target = openChoice == setting ? 1f : 0f;
            float current = dropdownAnimations.getOrDefault(setting, target);
            dropdownAnimations.put(setting, RenderUtil.animate(current, target,
                    target > 0f ? 0.22f : 0.18f, frameDelta));
        }
    }

    private float dropdownProgress(Setting<?> setting) {
        return clamp(dropdownAnimations.getOrDefault(setting, openChoice == setting ? 1f : 0f));
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
