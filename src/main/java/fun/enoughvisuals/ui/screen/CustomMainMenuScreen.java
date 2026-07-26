package fun.enoughvisuals.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.ui.mainmenu.MainMenuAnimation;
import fun.enoughvisuals.ui.mainmenu.MainMenuButton;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class CustomMainMenuScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SUBTITLE = 0xFFB195E1;

    private final MainMenuAnimation animation = new MainMenuAnimation();
    private final AnimatedFloat pickerHover = new AnimatedFloat(0f, 240L, Easing.OUT_QUART);
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
                "\u041c\u043d\u043e\u0433\u043e\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c\u0441\u043a\u0430\u044f \u0438\u0433\u0440\u0430",
                "\uF003",
                true,
                this::openMultiplayer));
        buttons.add(new MainMenuButton(
                "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430",
                "\uF004",
                false,
                this::openSingleplayer));
        buttons.add(new MainMenuButton(
                "\u0421\u043c\u0435\u043d\u0438\u0442\u044c \u0430\u043a\u043a\u0430\u0443\u043d\u0442",
                "\uF002",
                false,
                this::openAccountManager));
        buttons.add(new MainMenuButton(
                "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438",
                "\uF001",
                false,
                this::openOptions));
        buttons.add(new MainMenuButton(
                "\u0412\u044b\u0445\u043e\u0434",
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
        ctx.getMatrices().pop();
    }

    private void renderBackgroundPickerButton(DrawContext ctx, float mouseX, float mouseY, float scale, float alpha) {
        String label = "Выбрать фон";
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
        ctx.getMatrices().pop();
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
                "\u0434\u0430\u0432\u043d\u043e \u043d\u0435 \u0432\u0438\u0434\u0435\u043b\u0438\u0441\u044c, " + username,
                centerX,
                centerY - 63.0f * scale,
                subtitleSize,
                RenderUtil.withAlpha(SUBTITLE, alpha),
                FontWeight.REGULAR);
        CustomFontRenderer.drawCenteredText(ctx,
                "\u0414\u043e\u0431\u0440\u043e \u043f\u043e\u0436\u0430\u043b\u043e\u0432\u0430\u0442\u044c \u0432",
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
        RenderUtil.drawRect(ctx, 0.0f, 0.0f, width, height, 0x4A000000);
        RenderUtil.drawGradientRect(ctx, 0.0f, 0.0f, width, height * 0.30f, 0x5C000000, 0x00000000);
        RenderUtil.drawGradientRect(ctx, 0.0f, height * 0.66f, width, height * 0.34f, 0x00000000, 0x6A000000);
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
            String backgroundLabel = "Выбрать фон";
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
            for (MainMenuButton menuButton : buttons) {
                if (menuButton.click(virtualMouseX, virtualMouseY - yOffset)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
