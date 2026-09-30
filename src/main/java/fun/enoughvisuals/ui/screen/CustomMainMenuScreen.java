package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.ui.mainmenu.MainMenuAnimation;
import fun.enoughvisuals.ui.mainmenu.MainMenuButton;
import fun.enoughvisuals.ui.mainmenu.MenuBrand;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.ui.mainmenu.MenuCredits;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class CustomMainMenuScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final String DISCORD_URL = "https://dsc.gg/enoughvisuals";
    private static final String TELEGRAM_URL = "https://t.me/enoughvisuals";

    private final MainMenuAnimation animation = new MainMenuAnimation();
    private final AnimatedFloat discordHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final AnimatedFloat telegramHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final AnimatedFloat languageHover = new AnimatedFloat(0f, 180L, Easing.OUT_QUART);
    private final List<MainMenuButton> buttons = new ArrayList<>();
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
                ClientLanguage.text("ui.main_menu.singleplayer", "Одиночная игра"),
                "\uF004",
                false,
                this::openSingleplayer));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.multiplayer", "Многопользовательская игра"),
                "\uF003",
                false,
                this::openMultiplayer));
        buttons.add(new MainMenuButton(
                ClientLanguage.text("ui.main_menu.account", "Аккаунты"),
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
        drawBackground(ctx, mouseX, mouseY);
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
        renderSocialButtons(ctx, virtualMouseX, virtualMouseY - yOffset, scale, alpha);
        MenuCredits.render(ctx, virtualWidth, virtualHeight, scale, 1f,
                virtualMouseX, virtualMouseY - yOffset);
        ctx.getMatrices().pop();
    }

    private void renderSocialButtons(DrawContext ctx, float mouseX, float mouseY, float scale, float alpha) {
        float size = 18.5f * scale;
        float gap = 7f * scale;
        float x = virtualWidth * 0.5f - (size * 3f + gap * 2f) * 0.5f;
        float y = virtualHeight * 0.5f + 69f * scale;
        renderSocialButton(ctx, SolarIcons.DISCORD, x, y, size, mouseX, mouseY, alpha, discordHover, 8.6f);
        renderLanguageButton(ctx, x + size + gap, y, size, mouseX, mouseY, alpha);
        renderSocialButton(ctx, SolarIcons.TELEGRAM, x + (size + gap) * 2f, y, size, mouseX, mouseY, alpha, telegramHover, 8.9f);
    }

    private void renderLanguageButton(DrawContext ctx, float x, float y, float size,
                                      float mouseX, float mouseY, float alpha) {
        boolean hover = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        languageHover.setTarget(hover ? 1f : 0f);
        float hoverValue = languageHover.value();
        float grow = 1f + hoverValue * 0.025f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + size * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + size * 0.5f), -(y + size * 0.5f), 0f);
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, size, size, 5f * layoutScale(),
                    RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF000000, 0xFF101010, hoverValue), alpha * 0.98f));
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
        float grow = 1f + hoverValue * 0.025f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + size * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + size * 0.5f), -(y + size * 0.5f), 0f);
        float previousExponent = RenderUtil.pushSquircleExponent(2f);
        try {
            RenderUtil.drawRoundedRect(ctx, x, y, size, size, 5f * layoutScale(),
                    RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF000000, 0xFF101010, hoverValue), alpha * 0.98f));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
        float iconY = y + (size - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawCenteredText(ctx, icon, x + size * 0.5f, iconY, iconSize,
                RenderUtil.withAlpha(WHITE, alpha * (0.78f + hoverValue * 0.22f)), FontWeight.SOLAR_ICONS);
        ctx.getMatrices().pop();
    }

    void resumeFromBackgroundPicker() {
        suppressNextIntro = true;
        if (client != null) client.setScreen(this);
    }

    void resumeFromLanguagePicker() {
        suppressNextIntro = false;
        if (client != null) client.setScreen(this);
    }

    void resumeFromAccountManager() {
        suppressNextIntro = false;
        if (client != null) client.setScreen(this);
    }

    void renderBackgroundPickerBackdrop(DrawContext ctx) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        drawBackground(ctx, width * 0.5, height * 0.5);
        layoutButtons();

        float scale = layoutScale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(screenScale, screenScale, 1.0f);
        renderHeader(ctx, scale, 1f, 1f);
        for (MainMenuButton button : buttons) {
            button.render(ctx, scale, 1f, 1f);
        }
        renderSocialButtons(ctx, -10000f, -10000f, scale, 1f);
        MenuCredits.render(ctx, virtualWidth, virtualHeight, scale, 1f, -10000f, -10000f);
        ctx.getMatrices().pop();
    }

    void renderLanguagePickerBackdrop(DrawContext ctx) {
        renderBackgroundPickerBackdrop(ctx);
    }

    private void renderHeader(DrawContext ctx, float scale, float alpha, float appear) {
        float centerX = virtualWidth * 0.5f;
        float centerY = virtualHeight * 0.5f;
        MenuBrand.render(ctx, centerX, centerY - 51f * scale, scale, alpha, appear);
    }

    private void layoutButtons() {
        if (buttons.size() < 5) return;

        float scale = layoutScale();
        float fullW = 130.0f * scale;
        float fullH = 18.5f * scale;
        float gap = 4.0f * scale;
        float splitGap = 8.0f * scale;
        float halfW = (fullW - splitGap) * 0.5f;
        float x = virtualWidth * 0.5f - fullW * 0.5f;
        float y = virtualHeight * 0.5f - 21.5f * scale;

        buttons.get(0).setBounds(x, y, fullW, fullH);
        buttons.get(1).setBounds(x, y + (fullH + gap), fullW, fullH);
        buttons.get(2).setBounds(x, y + (fullH + gap) * 2.0f, fullW, fullH);

        float rowY = y + (fullH + gap) * 3.0f + 0.5f * scale;
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

    private void drawBackground(DrawContext ctx, double mouseX, double mouseY) {
        MenuBackgroundManager.drawSelectedBackground(ctx, width, height, mouseX, mouseY);
    }

    private void openMultiplayer() {
        if (client == null) return;
        client.setScreen(new MultiplayerScreen(this));
    }

    private void openSingleplayer() {
        if (client == null) return;
        client.setScreen(new SelectWorldScreen(this));
    }

    private void openOptions() {
        if (client == null) return;
        client.setScreen(new OptionsScreen(this, client.options));
    }

    private void quitGame() {
        if (client == null) return;
        client.stop();
    }

    private void openAccountManager() {
        if (client == null) return;
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
            if (MenuCredits.click(virtualMouseX, virtualMouseY - yOffset,
                    virtualWidth, virtualHeight, scale)) {
                return true;
            }
            float socialSize = 18.5f * scale;
            float socialGap = 7f * scale;
            float socialX = virtualWidth * 0.5f - (socialSize * 3f + socialGap * 2f) * 0.5f;
            float socialY = virtualHeight * 0.5f + 69f * scale + yOffset;
            if (virtualMouseY >= socialY && virtualMouseY <= socialY + socialSize) {
                if (virtualMouseX >= socialX && virtualMouseX <= socialX + socialSize) {
                    openUrl(DISCORD_URL);
                    return true;
                }
                float languageX = socialX + socialSize + socialGap;
                if (virtualMouseX >= languageX && virtualMouseX <= languageX + socialSize) {
                    client.setScreen(new LanguagePickerScreen(this));
                    return true;
                }
                float telegramX = socialX + (socialSize + socialGap) * 2f;
                if (virtualMouseX >= telegramX && virtualMouseX <= telegramX + socialSize) {
                    openUrl(TELEGRAM_URL);
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
