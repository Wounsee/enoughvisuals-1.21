package fun.enoughvisuals.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.ui.mainmenu.MainMenuAnimation;
import fun.enoughvisuals.ui.mainmenu.MainMenuButton;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class CustomMainMenuScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SUBTITLE = 0xFFB195E1;
    private static final String DISCORD_URL = "https://dsc.gg/enoughvisuals";
    private static final String TELEGRAM_URL = "https://t.me/enoughvisuals";

    private final MainMenuAnimation animation = new MainMenuAnimation();
    private final AnimatedFloat pickerHover = new AnimatedFloat(0f, 240L, Easing.OUT_QUART);
    private final AnimatedFloat discordHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final AnimatedFloat telegramHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final AnimatedFloat languageHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final List<MainMenuButton> buttons = new ArrayList<>();
    private Identifier filteredBackground;
    private boolean pendingAnimationRestart;
    private boolean firstVisibleAnimation = true;
    private long pendingAnimationDelayMs;
    private boolean suppressNextIntro;
    private float screenScale = 1.0f;
    private float virtualWidth;
    private float virtualHeight;

    public CustomMainMenuScreen() {
        super(Text.literal("Enough Visuals"));
    }

    @Override
    protected void init() {
        MenuBackgroundManager.initialize(client);
        if (suppressNextIntro) {
            suppressNextIntro = false;
            pendingAnimationRestart = false;
            pendingAnimationDelayMs = 0L;
        } else {
            pendingAnimationRestart = true;
            boolean startupAnimation = firstVisibleAnimation;
            firstVisibleAnimation = false;
            pendingAnimationDelayMs = startupAnimation ? 110L : 0L;
        }
        updateVirtualScreen();
        buttons.clear();
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.multiplayer", "Многопользовательская игра"),
                "\uF003",
                true,
                this::openMultiplayer));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.singleplayer", "Одиночная игра"),
                "\uF004",
                false,
                this::openSingleplayer));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.account", "Сменить аккаунт"),
                "\uF002",
                false,
                this::openAccountManager));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.settings", "Настройки"),
                "\uF001",
                false,
                this::openOptions));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.quit", "Выход"),
                "\uF000",
                false,
                this::quitGame));
        layoutButtons();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        updateVirtualScreen();
        layoutButtons();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        drawBackground(ctx);
        drawDim(ctx);
        if (pendingAnimationRestart) {
            animation.restartDelayed(pendingAnimationDelayMs);
            pendingAnimationRestart = false;
        }
        layoutButtons();

        float scale = layoutScale();
        float alpha = animation.alpha();
        float yOffset = animation.slideOffset(scale);
        float virtualMouseX = ScreenScaleUtil.screenToVirtual(mouseX);
        float virtualMouseY = ScreenScaleUtil.screenToVirtual(mouseY);

        for (MainMenuButton button : buttons) {
            button.tick(virtualMouseX, virtualMouseY - yOffset, 1f);
        }

        ctx.getMatrices().push();
        ctx.getMatrices().scale(screenScale, screenScale, 1.0f);
        ctx.getMatrices().translate(0.0f, yOffset, 0.0f);
        renderHeader(ctx, scale, alpha, animation.openProgress());
        for (int i = 0; i < buttons.size(); i++) {
            float buttonAlpha = alpha * animation.buttonAlpha(i);
            float appear = animation.buttonProgress(i);
            buttons.get(i).render(ctx, scale, buttonAlpha, appear);
        }
        renderBackgroundPickerButton(ctx, virtualMouseX, virtualMouseY, scale, alpha);
        renderSocialButtons(ctx, virtualMouseX, virtualMouseY - yOffset, scale, alpha);
        ctx.getMatrices().pop();
    }

    private void renderSocialButtons(DrawContext ctx, float mouseX, float mouseY, float scale, float alpha) {
        float size = 18f * scale;
        float gap = 4f * scale;
        float x = virtualWidth * 0.5f - (size * 3f + gap * 2f) * 0.5f;
        float fullH = 19.5f * scale;
        float rowY = virtualHeight * 0.5f - 4f * scale
                + (fullH + 4f * scale) * 3f + 1f * scale;
        float y = rowY + fullH + 7f * scale;
        renderSocialButton(ctx, SolarIcons.DISCORD, x, y, size, mouseX, mouseY, alpha, discordHover, 10.2f);
        renderSocialButton(ctx, SolarIcons.TELEGRAM, x + size + gap, y, size, mouseX, mouseY, alpha, telegramHover, 10.4f);
        renderLanguageButton(ctx, x + (size + gap) * 2f, y, size, mouseX, mouseY, alpha);
    }

    private void renderLanguageButton(DrawContext ctx, float x, float y, float size,
                                      float mouseX, float mouseY, float alpha) {
        boolean hover = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        languageHover.setTarget(hover ? 1f : 0f);
        float hoverValue = languageHover.value();
        float grow = 1f + hoverValue * 0.055f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + size * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + size * 0.5f), -(y + size * 0.5f), 0f);
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, size, size, size * 0.5f,
                    RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF151519, 0xFF252632, hoverValue), alpha * 0.94f));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
        float iconSize = size * 0.56f;
        float iconY = y + (size - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f + size * 0.04f;
        CustomFontRenderer.drawCenteredText(ctx, SolarIcons.LANGUAGE, x + size * 0.5f, iconY, iconSize,
                RenderUtil.withAlpha(WHITE, alpha * (0.78f + hoverValue * 0.22f)), FontWeight.SOLAR_ICONS);
        ctx.getMatrices().pop();
    }

    private void renderSocialButton(DrawContext ctx, String icon, float x, float y, float size,
                                    float mouseX, float mouseY, float alpha, AnimatedFloat hoverAnimation,
                                    float iconSize) {
        boolean hover = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        hoverAnimation.setTarget(hover ? 1f : 0f);
        float hoverValue = hoverAnimation.value();
        float grow = 1f + hoverValue * 0.055f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + size * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + size * 0.5f), -(y + size * 0.5f), 0f);
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, size, size, size * 0.5f,
                    RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF151519, 0xFF252632, hoverValue), alpha * 0.94f));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
        float iconY = y + (size - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawCenteredText(ctx, icon, x + size * 0.5f, iconY, iconSize,
                RenderUtil.withAlpha(WHITE, alpha * (0.78f + hoverValue * 0.22f)), FontWeight.SOLAR_ICONS);
        ctx.getMatrices().pop();
    }

    private void renderBackgroundPickerButton(DrawContext ctx, float mouseX, float mouseY, float scale, float alpha) {
        String label = ClientLanguage.text("ui.main_menu.choose_background", "Выбрать фон");
        float textSize = 5.8f * scale;
        float iconSize = 9.5f * scale;
        float w = CustomFontRenderer.getTextWidth(label, textSize, FontWeight.SEMIBOLD) + 30f * scale;
        float h = 18f * scale;
        float x = virtualWidth * 0.5f - w * 0.5f;
        float y = virtualHeight - 27f * scale;
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        pickerHover.setTarget(hover ? 1f : 0f);
        float hoverValue = pickerHover.value();
        float grow = 1f + hoverValue * 0.026f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w * 0.5f, y + h * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + w * 0.5f), -(y + h * 0.5f), 0f);
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, h * 0.5f,
                RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF151515, 0xFF1F1F1F, hoverValue), alpha * 0.92f));
        float iconY = y + (h - CustomFontRenderer.getTextHeight(iconSize, FontWeight.CLICK_GUI)) * 0.5f - 0.3f * scale;
        CustomFontRenderer.drawText(ctx, "\uF009", x + 7f * scale, iconY, iconSize,
                RenderUtil.withAlpha(WHITE, alpha), FontWeight.CLICK_GUI);
        float textY = y + (h - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f;
        CustomFontRenderer.drawText(ctx, label, x + 21f * scale, textY, textSize,
                RenderUtil.withAlpha(WHITE, alpha), FontWeight.SEMIBOLD);
        ctx.getMatrices().pop();
    }

    void resumeFromBackgroundPicker() {
        suppressNextIntro = true;
        if (client != null) client.setScreen(this);
    }

    void resumeFromLanguagePicker() {
        suppressNextIntro = true;
        if (client != null) client.setScreen(this);
    }

    void resumeFromAccountManager() {
        suppressNextIntro = false;
        if (client != null) client.setScreen(this);
    }

    void renderBackgroundPickerBackdrop(DrawContext ctx) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        drawBackground(ctx);
        drawDim(ctx);
        layoutButtons();

        float scale = layoutScale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(screenScale, screenScale, 1.0f);
        renderHeader(ctx, scale, 1f, 1f);
        for (MainMenuButton button : buttons) {
            button.render(ctx, scale, 1f, 1f);
        }
        renderBackgroundPickerButton(ctx, -10000f, -10000f, scale, 1f);
        renderSocialButtons(ctx, -10000f, -10000f, scale, 1f);
        ctx.getMatrices().pop();
    }

    void renderLanguagePickerBackdrop(DrawContext ctx) {
        renderBackgroundPickerBackdrop(ctx);
    }

    private void renderHeader(DrawContext ctx, float scale, float alpha, float appear) {
        String username = "player";
        if (client != null && client.getSession() != null && client.getSession().getUsername() != null) {
            username = client.getSession().getUsername();
        }

        float centerX = virtualWidth * 0.5f;
        float centerY = virtualHeight * 0.5f;
        float subtitleSize = 6.0f * scale;
        float titleSize = 12.2f * scale;
        float lineGap = 0.2f * scale;

        float grow = 1.3f - 0.3f * appear;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY - 45.0f * scale, 0.0f);
        ctx.getMatrices().scale(grow, grow, 1.0f);
        ctx.getMatrices().translate(-centerX, -(centerY - 45.0f * scale), 0.0f);
        CustomFontRenderer.drawCenteredText(ctx,
                ClientLanguage.text("ui.main_menu.greeting", "Давно не виделись, %s").formatted(username),
                centerX,
                centerY - 63.0f * scale,
                subtitleSize,
                RenderUtil.withAlpha(SUBTITLE, alpha),
                FontWeight.REGULAR);
        CustomFontRenderer.drawCenteredText(ctx,
                ClientLanguage.text("ui.main_menu.welcome", "Добро пожаловать в"),
                centerX,
                centerY - 55.5f * scale,
                titleSize,
                RenderUtil.withAlpha(WHITE, alpha),
                FontWeight.BOLD);
        CustomFontRenderer.drawCenteredText(ctx,
                "Enough Visuals!",
                centerX,
                centerY - 55.5f * scale + CustomFontRenderer.getTextHeight(titleSize, FontWeight.BOLD) + lineGap,
                titleSize,
                RenderUtil.withAlpha(WHITE, alpha),
                FontWeight.BOLD);
        ctx.getMatrices().pop();
    }

    private void layoutButtons() {
        if (buttons.size() < 5) return;

        float scale = layoutScale();
        float fullW = 160.0f * scale;
        float fullH = 19.5f * scale;
        float gap = 4.0f * scale;
        float splitGap = 4.5f * scale;
        float halfW = (fullW - splitGap) * 0.5f;
        float x = virtualWidth * 0.5f - fullW * 0.5f;
        float y = virtualHeight * 0.5f - 4.0f * scale;

        buttons.get(0).setBounds(x, y, fullW, fullH);
        buttons.get(1).setBounds(x, y + (fullH + gap), fullW, fullH);
        buttons.get(2).setBounds(x, y + (fullH + gap) * 2.0f, fullW, fullH);

        float rowY = y + (fullH + gap) * 3.0f + 1.0f * scale;
        buttons.get(3).setBounds(x, rowY, halfW, fullH);
        buttons.get(4).setBounds(x + halfW + splitGap, rowY, halfW, fullH);
    }

    private float layoutScale() {
        float byWidth = virtualWidth / 960.0f;
        float byHeight = virtualHeight / 540.0f;
        return Math.max(0.58f, Math.min(1.42f, Math.min(byWidth, byHeight)));
    }

    private void updateVirtualScreen() {
        screenScale = ScreenScaleUtil.scale();
        virtualWidth = ScreenScaleUtil.virtualWidth();
        virtualHeight = ScreenScaleUtil.virtualHeight();

        if (virtualWidth <= 0.0f) {
            virtualWidth = width / Math.max(0.001f, screenScale);
        }
        if (virtualHeight <= 0.0f) {
            virtualHeight = height / Math.max(0.001f, screenScale);
        }
    }

    private void drawBackground(DrawContext ctx) {
        drawBackground(ctx, true);
    }

    private void drawBackground(DrawContext ctx, boolean soften) {
        configureBackgroundFiltering();

        Identifier background = MenuBackgroundManager.texture();
        int backgroundWidth = MenuBackgroundManager.textureWidth();
        int backgroundHeight = MenuBackgroundManager.textureHeight();

        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = backgroundWidth / (float) backgroundHeight;
        float sourceX = 0.0f;
        float sourceY = 0.0f;
        int sourceW = backgroundWidth;
        int sourceH = backgroundHeight;

        if (screenAspect > textureAspect) {
            sourceH = Math.round(backgroundWidth / screenAspect);
            sourceY = (backgroundHeight - sourceH) * 0.5f;
        } else if (screenAspect < textureAspect) {
            sourceW = Math.round(backgroundHeight * screenAspect);
            sourceX = (backgroundWidth - sourceW) * 0.5f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.0f, 1.0f);
        if (soften) {
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.45f, 0.0f, 0.055f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.45f, 0.0f, 0.055f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.45f, 0.055f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, -0.45f, 0.055f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.35f, 0.35f, 0.035f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.35f, 0.35f, 0.035f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.35f, -0.35f, 0.035f);
            drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.35f, -0.35f, 0.035f);
        }
        ctx.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawBackgroundPass(DrawContext ctx, Identifier background, int backgroundWidth, int backgroundHeight,
                                    float sourceX, float sourceY, int sourceW, int sourceH,
                                    float offsetX, float offsetY, float alpha) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(offsetX, offsetY, 0.0f);
        ctx.drawTexture(RenderLayer::getGuiTextured, background, 0, 0, sourceX, sourceY,
                width, height, sourceW, sourceH, backgroundWidth, backgroundHeight);
        ctx.getMatrices().pop();
    }

    private void configureBackgroundFiltering() {
        Identifier background = MenuBackgroundManager.texture();
        if (background.equals(filteredBackground) || client == null) return;

        try {
            AbstractTexture texture = client.getTextureManager().getTexture(background);
            if (texture != null) {
                texture.setFilter(true, true);
                filteredBackground = background;
            }
        } catch (Throwable ignored) {
        }
    }

    private void drawDim(DrawContext ctx) {
        MenuBackgroundManager.drawBackgroundDim(ctx, width, height);
    }

    private void openMultiplayer() {
        if (client == null) return;
        EnoughSoundPlayer.playScroll();
        client.setScreen(new MultiplayerScreen(this));
    }

    private void openSingleplayer() {
        if (client == null) return;
        EnoughSoundPlayer.playScroll();
        client.setScreen(new SelectWorldScreen(this));
    }

    private void openOptions() {
        if (client == null) return;
        EnoughSoundPlayer.playScroll();
        client.setScreen(new OptionsScreen(this, client.options));
    }

    private void quitGame() {
        if (client == null) return;
        EnoughSoundPlayer.playEvent();
        client.stop();
    }

    private void openAccountManager() {
        if (client == null) return;
        EnoughSoundPlayer.playScroll();
        client.setScreen(new AccountSwitcherScreen(this));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            updateVirtualScreen();
            float yOffset = animation.slideOffset(layoutScale());
            float virtualMouseX = ScreenScaleUtil.screenToVirtual(mouseX);
            float virtualMouseY = ScreenScaleUtil.screenToVirtual(mouseY);
            float scale = layoutScale();
            String backgroundLabel = ClientLanguage.text("ui.main_menu.choose_background", "Выбрать фон");
            float pickerW = CustomFontRenderer.getTextWidth(backgroundLabel, 5.8f * scale, FontWeight.SEMIBOLD) + 30f * scale;
            float pickerH = 18f * scale;
            float pickerX = virtualWidth * 0.5f - pickerW * 0.5f;
            float pickerY = virtualHeight - 27f * scale;
            if (virtualMouseX >= pickerX && virtualMouseX <= pickerX + pickerW
                    && virtualMouseY >= pickerY && virtualMouseY <= pickerY + pickerH) {
                EnoughSoundPlayer.playScroll();
                client.setScreen(new BackgroundPickerScreen(this));
                return true;
            }
            float socialSize = 18f * scale;
            float socialGap = 4f * scale;
            float socialX = virtualWidth * 0.5f - (socialSize * 3f + socialGap * 2f) * 0.5f;
            float fullH = 19.5f * scale;
            float rowY = virtualHeight * 0.5f - 4f * scale
                    + (fullH + 4f * scale) * 3f + 1f * scale;
            float socialY = rowY + fullH + 7f * scale + yOffset;
            if (virtualMouseY >= socialY && virtualMouseY <= socialY + socialSize) {
                if (virtualMouseX >= socialX && virtualMouseX <= socialX + socialSize) {
                    openUrl(DISCORD_URL);
                    return true;
                }
                float telegramX = socialX + socialSize + socialGap;
                if (virtualMouseX >= telegramX && virtualMouseX <= telegramX + socialSize) {
                    openUrl(TELEGRAM_URL);
                    return true;
                }
                float languageX = socialX + (socialSize + socialGap) * 2f;
                if (virtualMouseX >= languageX && virtualMouseX <= languageX + socialSize) {
                    EnoughSoundPlayer.playScroll();
                    client.setScreen(new LanguagePickerScreen(this));
                    return true;
                }
            }
            for (MainMenuButton menuButton : buttons) {
                if (menuButton.click(virtualMouseX, virtualMouseY - yOffset)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openUrl(String url) {
        EnoughSoundPlayer.playScroll();
        Util.getOperatingSystem().open(URI.create(url));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
