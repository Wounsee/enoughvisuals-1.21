package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.ui.mainmenu.AnimatedFlagRenderer;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.ui.mainmenu.MenuCredits;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import fun.enoughvisuals.utils.system.ClientLanguage.LanguageInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LanguagePickerScreen extends Screen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int CARD = 0xFF000000;
    private static final int CARD_HOVER = 0xFF181818;
    private static final int COLUMNS = 3;
    private static final long OPEN_MS = 500L;
    private static final long CLOSE_MS = 300L;
    private static final long CARD_APPEAR_MS = 340L;

    private final CustomMainMenuScreen parent;
    private final Map<LanguageInfo, AnimatedFloat> hover = new IdentityHashMap<>();
    private final AnimatedFloat autoHover = new AnimatedFloat(0f, 160L, Easing.OUT_CUBIC);
    private final AnimatedFloat closeHover = new AnimatedFloat(0f, 160L, Easing.OUT_CUBIC);
    private float screenScale = 1f;
    private float virtualWidth;
    private float virtualHeight;
    private float targetScroll;
    private float scroll;
    private long lastFrameNanos;
    private long openedAt;
    private long closeStartedAt;
    private long reopenAfterNanos;
    private float closeFrom = 1f;
    private boolean applyingSelection;
    private boolean closing;
    private String status;

    public LanguagePickerScreen(CustomMainMenuScreen parent) {
        super(Text.literal(ClientLanguage.text("ui.language.title", "Языки")));
        this.parent = parent;
        for (LanguageInfo language : ClientLanguage.availableLanguages()) {
            hover.put(language, new AnimatedFloat(0f, 180L, Easing.OUT_CUBIC));
        }
    }

    @Override
    protected void init() {
        MenuBackgroundManager.initialize(client);
        openedAt = System.currentTimeMillis();
        closeStartedAt = 0L;
        closeFrom = 1f;
        closing = false;
        lastFrameNanos = System.nanoTime();
        updateVirtualScreen();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        updateVirtualScreen();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        MenuBackgroundManager.drawSelectedBackground(context, width, height, mouseX, mouseY);
        GlassUtil.BLUR_OFFSET = 1.35f;
        GlassUtil.beginFrame();

        float reveal = reveal();
        if (closing && System.currentTimeMillis() - closeStartedAt >= CLOSE_MS) {
            parent.resumeFromLanguagePicker();
            return;
        }

        float mouseVirtualX = ScreenScaleUtil.screenToVirtual(mouseX);
        float mouseVirtualY = ScreenScaleUtil.screenToVirtual(mouseY);
        Layout layout = layout().offsetY((1f - reveal) * 4f * layoutScale());
        updateScroll(layout);
        updateHover(layout, mouseVirtualX, mouseVirtualY);

        context.getMatrices().push();
        context.getMatrices().scale(screenScale, screenScale, 1f);
        context.getMatrices().push();
        float contentScale = 0.985f + reveal * 0.015f;
        context.getMatrices().translate(virtualWidth * 0.5f, virtualHeight * 0.5f, 0f);
        context.getMatrices().scale(contentScale, contentScale, 1f);
        context.getMatrices().translate(-virtualWidth * 0.5f, -virtualHeight * 0.5f, 0f);
        renderPanel(context, layout, reveal);
        context.getMatrices().pop();
        MenuCredits.render(context, virtualWidth, virtualHeight, layout.scale, 1f,
                mouseVirtualX, mouseVirtualY);
        context.getMatrices().pop();

        if (!closing && reopenAfterNanos != 0L && System.nanoTime() >= reopenAfterNanos) {
            reopenAfterNanos = 0L;
            close();
        }
    }

    private void renderPanel(DrawContext context, Layout layout, float alpha) {
        GlassUtil.drawGlassPanel(context, layout.x, layout.y, layout.w, layout.h,
                6.5f * layout.scale, 0x000000, 0.64f * alpha, alpha);

        String title = ClientLanguage.text("ui.language.title", "Языки");
        CustomFontRenderer.drawCenteredText(context, title, virtualWidth * 0.5f,
                layout.y - 19.5f * layout.scale, 9.5f * layout.scale,
                RenderUtil.withAlpha(TEXT, alpha * 0.75f), FontWeight.SEMIBOLD);

        renderSectionLabel(context,
                status == null ? ClientLanguage.text("ui.language.languages", "Языки") : status,
                SolarIcons.LANGUAGE, layout.x + 12f * layout.scale, layout.y + 8f * layout.scale,
                layout.scale, alpha);
        RenderUtil.drawRect(context, layout.x + 49f * layout.scale, layout.y + 11.5f * layout.scale,
                layout.w - 65f * layout.scale, 0.5f * layout.scale,
                RenderUtil.withAlpha(TEXT, alpha * 0.05f));

        enableListScissor(layout);
        List<LanguageInfo> languages = ClientLanguage.availableLanguages();
        for (int index = 0; index < languages.size(); index++) {
            LanguageInfo language = languages.get(index);
            float x = layout.listX + (index % COLUMNS) * (layout.cardW + layout.gap);
            float y = layout.listY + (index / COLUMNS) * (layout.cardH + layout.gap) - scroll;
            if (y + layout.cardH < layout.listY || y > layout.listY + layout.listH) continue;
            float topFade = MathHelper.clamp((y + layout.cardH - layout.listY) / (layout.cardH * 0.72f), 0f, 1f);
            float bottomFade = MathHelper.clamp((layout.listY + layout.listH - y) / (layout.cardH * 0.72f), 0f, 1f);
            float cardAlpha = alpha * cardReveal(index) * Easing.OUT_CUBIC.apply(Math.min(topFade, bottomFade));
            renderLanguage(context, language, x, y, layout, hover.get(language).value(), cardAlpha);
        }
        RenderUtil.disableScissor();
        renderScrollbar(context, layout, alpha);

        float settingsY = layout.y + layout.h - 33.5f * layout.scale;
        renderSectionLabel(context, ClientLanguage.text("ui.account.settings", "Настройки"),
                SolarIcons.SETTINGS, layout.x + 12f * layout.scale, settingsY,
                layout.scale, alpha);
        float settingsLabelWidth = CustomFontRenderer.getTextWidth(
                ClientLanguage.text("ui.account.settings", "Настройки"),
                5.2f * layout.scale, FontWeight.SEMIBOLD);
        float settingsStartX = layout.x + 12f * layout.scale + settingsLabelWidth
                + 2f * layout.scale
                + CustomFontRenderer.getTextWidth(SolarIcons.SETTINGS,
                5.1f * layout.scale, FontWeight.SOLAR_ICONS) + 6f * layout.scale;
        RenderUtil.drawRect(context, settingsStartX,
                settingsY + 3.5f * layout.scale,
                Math.max(1f, layout.x + layout.w - 16f * layout.scale - settingsStartX),
                0.5f * layout.scale,
                RenderUtil.withAlpha(TEXT, alpha * 0.05f));
        renderFooterButton(context, layout.autoX, layout.buttonY, layout.autoW, layout.buttonH,
                ClientLanguage.text("ui.language.auto", "Авто"), SolarIcons.ROBOT,
                "auto".equals(ClientLanguage.selectedLanguageCode()), autoHover.value(), alpha, layout.scale);
        renderFooterButton(context, layout.closeX, layout.buttonY, layout.closeW, layout.buttonH,
                Text.translatable("gui.back").getString(), SolarIcons.CLOSE,
                false, closeHover.value(), alpha, layout.scale);
    }

    private void renderSectionLabel(DrawContext context, String label, String icon,
                                    float x, float y, float scale, float alpha) {
        float size = 5.2f * scale;
        String trimmed = CustomFontRenderer.trimToWidth(label, 112f * scale, size, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, trimmed, x, y, size,
                RenderUtil.withAlpha(TEXT, alpha * 0.25f), FontWeight.SEMIBOLD);
        float iconSize = 5.1f * scale;
        float iconX = x + CustomFontRenderer.getTextWidth(trimmed, size, FontWeight.SEMIBOLD) + 2f * scale;
        float iconY = y + (CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)
                - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawText(context, icon, iconX, iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha * 0.25f), FontWeight.SOLAR_ICONS);
    }

    private void renderLanguage(DrawContext context, LanguageInfo language, float x, float y,
                                Layout layout, float hoverValue, float alpha) {
        if (alpha <= 0.004f) return;
        String configured = ClientLanguage.selectedLanguageCode();
        boolean selected = language.code().equals(configured)
                || ("auto".equals(configured) && language.code().equals(ClientLanguage.activeLanguageCode()));
        float grow = 1f + hoverValue * 0.008f;
        context.getMatrices().push();
        context.getMatrices().translate(x + layout.cardW * 0.5f, y + layout.cardH * 0.5f, 0f);
        context.getMatrices().scale(grow, grow, 1f);
        context.getMatrices().translate(-(x + layout.cardW * 0.5f), -(y + layout.cardH * 0.5f), 0f);

        int background = RenderUtil.lerpColor(CARD, CARD_HOVER, hoverValue);
        RenderUtil.drawRoundedRect(context, x, y, layout.cardW, layout.cardH, 7f * layout.scale,
                RenderUtil.withAlpha(background, alpha * (selected ? 0.54f : 0.40f)));

        float flagSize = 14f * layout.scale;
        AnimatedFlagRenderer.draw(context, language.flagAsset(), x + 4.5f * layout.scale,
                y + (layout.cardH - flagSize) * 0.5f, flagSize, alpha);
        float textX = x + 21.5f * layout.scale;
        float nameSize = 5.35f * layout.scale;
        String name = CustomFontRenderer.trimToWidth(language.displayName(),
                layout.cardW - 29f * layout.scale, nameSize, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, name, textX, y + 4f * layout.scale,
                nameSize, RenderUtil.withAlpha(TEXT, alpha * 0.75f), FontWeight.SEMIBOLD);

        String detail;
        if (language.curated()) detail = ClientLanguage.text("ui.language.curated", "Проверенный перевод");
        else if (ClientLanguage.isGenerating(language.code())) {
            detail = ClientLanguage.text("ui.language.generating", "Перевод") + " "
                    + Math.round(ClientLanguage.translationProgress() * 100f) + "%";
        } else detail = "Google Translate";
        detail = CustomFontRenderer.trimToWidth(detail, layout.cardW - 29f * layout.scale,
                4.1f * layout.scale, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, detail, textX, y + 12.5f * layout.scale,
                4.1f * layout.scale, RenderUtil.withAlpha(TEXT, alpha * 0.35f), FontWeight.SEMIBOLD);

        if (selected) {
            CustomFontRenderer.drawRightText(context, SolarIcons.CHECK,
                    x + layout.cardW - 4f * layout.scale, y + 9.4f * layout.scale,
                    4.6f * layout.scale, RenderUtil.withAlpha(TEXT, alpha * 0.72f),
                    FontWeight.SOLAR_ICONS);
        }
        context.getMatrices().pop();
    }

    private void renderFooterButton(DrawContext context, float x, float y, float width, float height,
                                    String label, String icon, boolean active, float hover,
                                    float alpha, float scale) {
        float grow = 1f + hover * 0.012f;
        context.getMatrices().push();
        context.getMatrices().translate(x + width * 0.5f, y + height * 0.5f, 0f);
        context.getMatrices().scale(grow, grow, 1f);
        context.getMatrices().translate(-(x + width * 0.5f), -(y + height * 0.5f), 0f);
        RenderUtil.drawRoundedRect(context, x, y, width, height, 4f * scale,
                RenderUtil.withAlpha(RenderUtil.lerpColor(CARD, CARD_HOVER, hover),
                        alpha * (active ? 0.56f : 0.40f)));
        float size = 4.75f * scale;
        float textY = y + (height - CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)) * 0.5f;
        CustomFontRenderer.drawText(context, label, x + 4.5f * scale, textY, size,
                RenderUtil.withAlpha(TEXT, alpha * 0.75f), FontWeight.SEMIBOLD);
        float iconSize = 5f * scale;
        float iconY = y + (height - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawRightText(context, icon, x + width - 4f * scale,
                iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha * 0.75f), FontWeight.SOLAR_ICONS);
        context.getMatrices().pop();
    }

    private void updateHover(Layout layout, float mouseX, float mouseY) {
        List<LanguageInfo> languages = ClientLanguage.availableLanguages();
        for (int index = 0; index < languages.size(); index++) {
            float x = layout.listX + (index % COLUMNS) * (layout.cardW + layout.gap);
            float y = layout.listY + (index / COLUMNS) * (layout.cardH + layout.gap) - scroll;
            boolean over = !closing && !applyingSelection
                    && y + layout.cardH >= layout.listY && y <= layout.listY + layout.listH
                    && inside(mouseX, mouseY, x, y, layout.cardW, layout.cardH);
            hover.get(languages.get(index)).setTarget(over ? 1f : 0f);
        }
        autoHover.setTarget(!closing && inside(mouseX, mouseY,
                layout.autoX, layout.buttonY, layout.autoW, layout.buttonH) ? 1f : 0f);
        closeHover.setTarget(!closing && inside(mouseX, mouseY,
                layout.closeX, layout.buttonY, layout.closeW, layout.buttonH) ? 1f : 0f);
    }

    private void enableListScissor(Layout layout) {
        RenderUtil.enableScissor(0f, layout.listY * screenScale,
                virtualWidth * screenScale, layout.listH * screenScale);
    }

    private void renderScrollbar(DrawContext context, Layout layout, float alpha) {
        float max = maxScroll(layout);
        if (max <= 0.5f) return;
        float contentH = layout.listH + max;
        float thumbH = Math.max(19.5f * layout.scale, layout.listH * layout.listH / contentH);
        float thumbY = layout.listY + (layout.listH - thumbH) * (scroll / max);
        float x = layout.x + layout.w - 9.5f * layout.scale;
        RenderUtil.drawRoundedRect(context, x, layout.listY, 4f * layout.scale, layout.listH,
                2f * layout.scale, RenderUtil.withAlpha(0xFF1F1E1E, alpha * 0.30f));
        RenderUtil.drawRoundedRect(context, x + layout.scale, thumbY, 2f * layout.scale, thumbH,
                layout.scale, RenderUtil.withAlpha(0xFF848585, alpha * 0.25f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || closing || applyingSelection) return true;
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        Layout layout = layout().offsetY((1f - reveal()) * 4f * layoutScale());
        if (MenuCredits.click(mx, my, virtualWidth, virtualHeight, layout.scale)) return true;
        if (inside(mx, my, layout.closeX, layout.buttonY, layout.closeW, layout.buttonH)) {
            close();
            return true;
        }
        if (inside(mx, my, layout.autoX, layout.buttonY, layout.autoW, layout.buttonH)) {
            ClientLanguage.useAutomaticLanguage();
            close();
            return true;
        }
        if (!inside(mx, my, layout.x, layout.y, layout.w, layout.h)) {
            close();
            return true;
        }
        if (my < layout.listY || my > layout.listY + layout.listH) return true;

        List<LanguageInfo> languages = ClientLanguage.availableLanguages();
        for (int index = 0; index < languages.size(); index++) {
            float x = layout.listX + (index % COLUMNS) * (layout.cardW + layout.gap);
            float y = layout.listY + (index / COLUMNS) * (layout.cardH + layout.gap) - scroll;
            if (!inside(mx, my, x, y, layout.cardW, layout.cardH)) continue;
            LanguageInfo language = languages.get(index);
            if (ClientLanguage.isGenerating(language.code())) return true;
            AnimatedFlagRenderer.playOnce(language.flagAsset());
            applyingSelection = true;
            long animationEndsAt = System.nanoTime() + 780_000_000L;
            status = language.curated()
                    ? ClientLanguage.text("ui.language.applying", "Применение...")
                    : ClientLanguage.text("ui.language.connecting", "Перевод через Google...");
            ClientLanguage.selectLanguage(language.code()).whenComplete((success, error) -> {
                if (client == null) return;
                client.execute(() -> {
                    if (Boolean.TRUE.equals(success)) reopenAfterNanos = Math.max(System.nanoTime(), animationEndsAt);
                    else {
                        applyingSelection = false;
                        status = ClientLanguage.text("ui.language.failed", "Не удалось получить перевод");
                    }
                });
            });
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = layout();
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (inside(mx, my, layout.listX, layout.listY, layout.listW, layout.listH)) {
            targetScroll = MathHelper.clamp(targetScroll - (float) verticalAmount * 29f * layout.scale,
                    0f, maxScroll(layout));
        }
        return true;
    }

    @Override
    public void close() {
        if (closing) return;
        closeFrom = reveal();
        closeStartedAt = System.currentTimeMillis();
        closing = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private float reveal() {
        long now = System.currentTimeMillis();
        if (closing) {
            float progress = MathHelper.clamp((now - closeStartedAt) / (float) CLOSE_MS, 0f, 1f);
            return closeFrom * (1f - Easing.IN_OUT_CUBIC.apply(progress));
        }
        float progress = MathHelper.clamp((now - openedAt) / (float) OPEN_MS, 0f, 1f);
        return Easing.OUT_CUBIC.apply(progress);
    }

    private float cardReveal(int index) {
        if (closing) return reveal();
        long delay = Math.min(index, 12) * 14L;
        float progress = MathHelper.clamp((System.currentTimeMillis() - openedAt - delay)
                / (float) CARD_APPEAR_MS, 0f, 1f);
        return Easing.OUT_CUBIC.apply(progress);
    }

    private void updateScroll(Layout layout) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0f, (now - lastFrameNanos) / 1_000_000_000f));
        lastFrameNanos = now;
        targetScroll = MathHelper.clamp(targetScroll, 0f, maxScroll(layout));
        scroll += (targetScroll - scroll) * (1f - (float) Math.exp(-14f * dt));
        if (Math.abs(targetScroll - scroll) < 0.01f) scroll = targetScroll;
    }

    private float maxScroll(Layout layout) {
        int rows = (ClientLanguage.availableLanguages().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0f, rows * (layout.cardH + layout.gap) - layout.gap - layout.listH);
    }

    private void updateVirtualScreen() {
        screenScale = ScreenScaleUtil.scale();
        virtualWidth = ScreenScaleUtil.virtualWidth();
        virtualHeight = ScreenScaleUtil.virtualHeight();
        if (virtualWidth <= 0f) virtualWidth = width / Math.max(0.001f, screenScale);
        if (virtualHeight <= 0f) virtualHeight = height / Math.max(0.001f, screenScale);
    }

    private float layoutScale() {
        return Math.max(0.58f, Math.min(1.42f,
                Math.min(virtualWidth / 960f, virtualHeight / 540f)));
    }

    private Layout layout() {
        float scale = layoutScale();
        float w = Math.min(314.5f * scale, virtualWidth - 24f * scale);
        float h = Math.min(220f * scale, virtualHeight - 66f * scale);
        float x = virtualWidth * 0.5f - w * 0.5f;
        float y = virtualHeight * 0.5f - h * 0.5f;
        float listX = x + 12f * scale;
        float listY = y + 19.5f * scale;
        float gap = 5f * scale;
        float listW = w - 28f * scale;
        float cardW = (listW - gap * (COLUMNS - 1)) / COLUMNS;
        float cardH = 28f * scale;
        float listH = Math.max(cardH, h - 58f * scale);
        float buttonY = y + h - 20f * scale;
        float buttonH = 14f * scale;
        float autoX = x + 12f * scale;
        float autoW = 75f * scale;
        float closeX = autoX + 80.5f * scale;
        float closeW = 45.5f * scale;
        return new Layout(x, y, w, h, listX, listY, listW, listH,
                cardW, cardH, gap, autoX, autoW, closeX, closeW, buttonY, buttonH, scale);
    }

    private static void renderScaled(DrawContext context, float x, float y, float width, float height,
                                     float scale, Runnable render) {
        context.getMatrices().push();
        context.getMatrices().translate(x + width * 0.5f, y + height * 0.5f, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-(x + width * 0.5f), -(y + height * 0.5f), 0f);
        render.run();
        context.getMatrices().pop();
    }

    private static boolean inside(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Layout(float x, float y, float w, float h,
                          float listX, float listY, float listW, float listH,
                          float cardW, float cardH, float gap,
                          float autoX, float autoW, float closeX, float closeW,
                          float buttonY, float buttonH, float scale) {
        private Layout offsetY(float offset) {
            return new Layout(x, y + offset, w, h,
                    listX, listY + offset, listW, listH,
                    cardW, cardH, gap,
                    autoX, autoW, closeX, closeW,
                    buttonY + offset, buttonH, scale);
        }
    }
}
