package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.ui.mainmenu.AnimatedFlagRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import fun.enoughvisuals.utils.system.ClientLanguage.LanguageInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LanguagePickerScreen extends Screen {
    private static final int PANEL = 0xFF151519;
    private static final int CARD = 0xFF1D1C22;
    private static final int CARD_HOVER = 0xFF292731;
    private static final int ACCENT = 0xFF8C72E8;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA49EAD;
    private static final int COLUMNS = 2;
    private static final long OPEN_MS = 390L;
    private static final long CLOSE_MS = 270L;
    private static final long CARD_APPEAR_MS = 250L;

    private final CustomMainMenuScreen parent;
    private final Map<LanguageInfo, AnimatedFloat> hover = new IdentityHashMap<>();
    private final AnimatedFloat autoHover = new AnimatedFloat(0f, 210L, Easing.OUT_QUART);
    private final AnimatedFloat closeHover = new AnimatedFloat(0f, 210L, Easing.OUT_QUART);
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
        super(Text.literal(ClientLanguage.text("ui.language.title", "Выберите язык")));
        this.parent = parent;
        for (LanguageInfo language : ClientLanguage.availableLanguages()) {
            hover.put(language, new AnimatedFloat(0f, 240L, Easing.OUT_QUART));
        }
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        closeStartedAt = 0L;
        closeFrom = 1f;
        closing = false;
        lastFrameNanos = System.nanoTime();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        parent.renderLanguagePickerBackdrop(context);
        float reveal = reveal();
        if (closing && System.currentTimeMillis() - closeStartedAt >= CLOSE_MS) {
            parent.resumeFromLanguagePicker();
            return;
        }

        RenderUtil.drawRect(context, 0, 0, width, height,
                RenderUtil.withAlpha(0xFF000000, reveal * 0.60f));
        Layout layout = layout().offsetY((1f - reveal) * 10f);
        updateScroll(layout);

        float previousExponent = RenderUtil.pushSquircleExponent(3.5f);
        try {
            RenderUtil.drawRoundedRect(context, layout.x + 2f, layout.y + 5f, layout.w, layout.h, 18f,
                    RenderUtil.withAlpha(0xFF000000, reveal * 0.46f));
            RenderUtil.drawRoundedRect(context, layout.x, layout.y, layout.w, layout.h, 18f,
                    RenderUtil.withAlpha(PANEL, reveal));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
        renderHeader(context, layout, mouseX, mouseY, reveal);

        context.draw();
        context.enableScissor(Math.round(layout.listX), Math.round(layout.listY),
                Math.round(layout.listX + layout.listW), Math.round(layout.listY + layout.listH));
        List<LanguageInfo> languages = ClientLanguage.availableLanguages();
        for (int index = 0; index < languages.size(); index++) {
            LanguageInfo language = languages.get(index);
            float x = layout.listX + (index % COLUMNS) * (layout.cardW + layout.gap);
            float y = layout.listY + (index / COLUMNS) * (layout.cardH + layout.gap) - scroll;
            if (y + layout.cardH < layout.listY - 3f || y > layout.listY + layout.listH + 3f) continue;
            boolean over = !closing && !applyingSelection
                    && inside(mouseX, mouseY, x, y, layout.cardW, layout.cardH)
                    && mouseY >= layout.listY && mouseY <= layout.listY + layout.listH;
            AnimatedFloat hoverAnimation = hover.get(language);
            hoverAnimation.setTarget(over ? 1f : 0f);
            float topFade = MathHelper.clamp((y + layout.cardH - layout.listY) / 11f, 0f, 1f);
            float bottomFade = MathHelper.clamp((layout.listY + layout.listH - y) / 11f, 0f, 1f);
            float edgeFade = Easing.IN_OUT_QUAD.apply(Math.min(topFade, bottomFade));
            float cardAlpha = reveal * cardReveal(index) * edgeFade;
            renderLanguage(context, language, x, y, layout.cardW, layout.cardH,
                    hoverAnimation.value(), cardAlpha);
        }
        context.draw();
        context.disableScissor();
        renderScrollbar(context, layout, reveal);

        if (!closing && reopenAfterNanos != 0L && System.nanoTime() >= reopenAfterNanos) {
            reopenAfterNanos = 0L;
            close();
        }
    }

    private void renderHeader(DrawContext context, Layout layout, int mouseX, int mouseY, float alpha) {
        String title = ClientLanguage.text("ui.language.title", "Выберите язык");
        String subtitle = status == null
                ? ClientLanguage.text("ui.language.subtitle", "Выберите удобный для вас язык.")
                : status;
        CustomFontRenderer.drawText(context, title, layout.x + 14f, layout.y + 10.5f, 8.9f,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, subtitle, layout.x + 14f, layout.y + 22.8f, 6.1f,
                RenderUtil.withAlpha(MUTED, alpha), FontWeight.SEMIBOLD);

        float buttonSize = 17f;
        float closeX = layout.x + layout.w - 27f;
        float autoX = closeX - buttonSize - 6f;
        float buttonY = layout.y + 10f;
        boolean overAuto = !closing && inside(mouseX, mouseY, autoX, buttonY, buttonSize, buttonSize);
        boolean overClose = !closing && inside(mouseX, mouseY, closeX, buttonY, buttonSize, buttonSize);
        autoHover.setTarget(overAuto ? 1f : 0f);
        closeHover.setTarget(overClose ? 1f : 0f);
        renderRoundButton(context, autoX, buttonY, buttonSize, SolarIcons.ROBOT,
                "auto".equals(ClientLanguage.selectedLanguageCode()), autoHover.value(), alpha, 10.3f);
        renderRoundButton(context, closeX, buttonY, buttonSize, SolarIcons.CLOSE,
                false, closeHover.value(), alpha, 9.3f);
    }

    private void renderRoundButton(DrawContext context, float x, float y, float size, String icon,
                                   boolean active, float hoverValue, float alpha, float iconSize) {
        float grow = 1f + hoverValue * 0.045f;
        context.getMatrices().push();
        context.getMatrices().translate(x + size * 0.5f, y + size * 0.5f, 0f);
        context.getMatrices().scale(grow, grow, 1f);
        context.getMatrices().translate(-(x + size * 0.5f), -(y + size * 0.5f), 0f);
        RenderUtil.drawRoundedRect(context, x, y, size, size, size * 0.5f,
                RenderUtil.withAlpha(active ? ACCENT : RenderUtil.lerpColor(0xFF242229, 0xFF373441, hoverValue), alpha));
        float iconY = y + (size - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawCenteredText(context, icon, x + size * 0.5f, iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha * (0.82f + hoverValue * 0.18f)), FontWeight.SOLAR_ICONS);
        context.getMatrices().pop();
    }

    private void renderLanguage(DrawContext context, LanguageInfo language, float x, float y, float w, float h,
                                float hoverValue, float alpha) {
        if (alpha <= 0.01f) return;
        String configured = ClientLanguage.selectedLanguageCode();
        boolean selected = language.code().equals(configured)
                || ("auto".equals(configured) && language.code().equals(ClientLanguage.activeLanguageCode()));
        float lift = hoverValue * 0.75f;
        y -= lift;
        int background = selected ? 0xFF282238 : RenderUtil.lerpColor(CARD, CARD_HOVER, hoverValue);
        float previousExponent = RenderUtil.pushSquircleExponent(3.4f);
        try {
            RenderUtil.drawRoundedRect(context, x, y + 1.6f, w, h, 12f,
                    RenderUtil.withAlpha(0xFF000000, alpha * (0.16f + hoverValue * 0.10f)));
            RenderUtil.drawRoundedRect(context, x, y, w, h, 12f,
                    RenderUtil.withAlpha(background, alpha));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }

        float flagSize = 21f;
        AnimatedFlagRenderer.draw(context, language.flagAsset(), x + 7f, y + (h - flagSize) * 0.5f, flagSize, alpha);
        CustomFontRenderer.drawText(context, language.displayName(), x + 34f, y + 5.8f,
                6.8f, RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);

        String detail;
        if (language.curated()) {
            detail = ClientLanguage.text("ui.language.curated", "Проверенный перевод");
        } else if (ClientLanguage.isGenerating(language.code())) {
            detail = ClientLanguage.text("ui.language.generating", "Перевод") + " "
                    + Math.round(ClientLanguage.translationProgress() * 100f) + "%";
        } else {
            detail = "Google Translate";
        }
        CustomFontRenderer.drawText(context, detail, x + 34f, y + 16.1f,
                5.5f, RenderUtil.withAlpha(selected ? 0xFFC4B4FF : MUTED, alpha), FontWeight.SEMIBOLD);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || closing || applyingSelection) return true;
        Layout layout = layout().offsetY((1f - reveal()) * 10f);
        float buttonSize = 17f;
        float closeX = layout.x + layout.w - 27f;
        float autoX = closeX - buttonSize - 6f;
        float buttonY = layout.y + 10f;
        if (inside(mouseX, mouseY, closeX, buttonY, buttonSize, buttonSize)) {
            close();
            return true;
        }
        if (inside(mouseX, mouseY, autoX, buttonY, buttonSize, buttonSize)) {
            ClientLanguage.useAutomaticLanguage();
            EnoughSoundPlayer.playScroll();
            close();
            return true;
        }
        if (!inside(mouseX, mouseY, layout.x, layout.y, layout.w, layout.h)) {
            close();
            return true;
        }
        if (mouseY < layout.listY || mouseY > layout.listY + layout.listH) return true;

        List<LanguageInfo> languages = ClientLanguage.availableLanguages();
        for (int index = 0; index < languages.size(); index++) {
            float x = layout.listX + (index % COLUMNS) * (layout.cardW + layout.gap);
            float y = layout.listY + (index / COLUMNS) * (layout.cardH + layout.gap) - scroll;
            if (!inside(mouseX, mouseY, x, y, layout.cardW, layout.cardH)) continue;
            LanguageInfo language = languages.get(index);
            if (ClientLanguage.isGenerating(language.code())) return true;
            EnoughSoundPlayer.playScroll();
            AnimatedFlagRenderer.playOnce(language.flagAsset());
            applyingSelection = true;
            long animationEndsAt = System.nanoTime() + 1_000_000_000L;
            status = language.curated()
                    ? ClientLanguage.text("ui.language.applying", "Применение...")
                    : ClientLanguage.text("ui.language.connecting", "Перевод через Google...");
            ClientLanguage.selectLanguage(language.code()).whenComplete((success, error) -> {
                if (client == null) return;
                client.execute(() -> {
                    if (Boolean.TRUE.equals(success)) {
                        reopenAfterNanos = Math.max(System.nanoTime(), animationEndsAt);
                    } else {
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
        if (inside(mouseX, mouseY, layout.listX, layout.listY, layout.listW, layout.listH)) {
            targetScroll = MathHelper.clamp(targetScroll - (float) verticalAmount * 29f, 0f, maxScroll(layout));
        }
        return true;
    }

    @Override
    public void close() {
        if (closing) return;
        closeFrom = reveal();
        closeStartedAt = System.currentTimeMillis();
        closing = true;
        EnoughSoundPlayer.playScroll();
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
        return Easing.IN_OUT_CUBIC.apply(progress);
    }

    private float cardReveal(int index) {
        if (closing) return reveal();
        long delay = Math.min(index, 10) * 11L;
        float progress = MathHelper.clamp((System.currentTimeMillis() - openedAt - delay) / (float) CARD_APPEAR_MS, 0f, 1f);
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

    private void renderScrollbar(DrawContext context, Layout layout, float alpha) {
        float max = maxScroll(layout);
        if (max <= 0.5f) return;
        float thumbH = Math.max(22f, layout.listH * layout.listH / (layout.listH + max));
        float thumbY = layout.listY + (layout.listH - thumbH) * (scroll / max);
        int accent = CustomTheme.guiAccent();
        RenderUtil.drawRoundedRect(context, layout.x + layout.w - 6f, layout.listY, 1.5f, layout.listH,
                0.75f, RenderUtil.withAlpha(accent, alpha * 0.18f));
        RenderUtil.drawRoundedRect(context, layout.x + layout.w - 6f, thumbY, 1.5f, thumbH,
                0.75f, RenderUtil.withAlpha(accent, alpha));
    }

    private float maxScroll(Layout layout) {
        int rows = (ClientLanguage.availableLanguages().size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0f, rows * (layout.cardH + layout.gap) - layout.gap - layout.listH);
    }

    private Layout layout() {
        float w = Math.min(390f, width - 28f);
        float h = Math.min(292f, height - 24f);
        float x = (width - w) * 0.5f;
        float y = (height - h) * 0.5f;
        float padding = 12f;
        float listY = y + 39f;
        float listH = h - 51f;
        float gap = 5f;
        float listW = w - padding * 2f;
        float cardW = (listW - gap) * 0.5f;
        return new Layout(x, y, w, h, x + padding, listY, listW, listH, cardW, 32f, gap);
    }

    private static boolean inside(double px, double py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private record Layout(float x, float y, float w, float h, float listX, float listY,
                          float listW, float listH, float cardW, float cardH, float gap) {
        Layout offsetY(float offset) {
            return new Layout(x, y + offset, w, h, listX, listY + offset,
                    listW, listH, cardW, cardH, gap);
        }
    }
}
