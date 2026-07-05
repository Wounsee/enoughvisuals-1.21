package fun.enoughvisuals.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.ui.mainmenu.MainMenuAnimation;
import fun.enoughvisuals.ui.mainmenu.MainMenuButton;
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
    private static final Identifier BACKGROUND = Identifier.of("enough", "textures/mainmenu/bg.png");
    private static final int BACKGROUND_WIDTH = 1920;
    private static final int BACKGROUND_HEIGHT = 1080;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SUBTITLE = 0xFFB195E1;

    private final MainMenuAnimation animation = new MainMenuAnimation();
    private final List<MainMenuButton> buttons = new ArrayList<>();
    private boolean backgroundFilterConfigured;
    private boolean pendingAnimationRestart;
    private boolean firstVisibleAnimation = true;
    private long pendingAnimationDelayMs;
    private int animationWarmupFrames;
    private float screenScale = 1.0f;
    private float virtualWidth;
    private float virtualHeight;

    public CustomMainMenuScreen() {
        super(Text.literal("Enough Visuals"));
    }

    @Override
    protected void init() {
        pendingAnimationRestart = true;
        boolean startupAnimation = firstVisibleAnimation;
        firstVisibleAnimation = false;
        pendingAnimationDelayMs = startupAnimation ? 110L : 0L;
        animationWarmupFrames = startupAnimation ? 18 : 0;
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
        if (animationWarmupFrames > 0) {
            animation.restartDelayed(90L);
            animationWarmupFrames--;
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
        configureBackgroundFiltering();

        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = BACKGROUND_WIDTH / (float) BACKGROUND_HEIGHT;
        float sourceX = 0.0f;
        float sourceY = 0.0f;
        int sourceW = BACKGROUND_WIDTH;
        int sourceH = BACKGROUND_HEIGHT;

        if (screenAspect > textureAspect) {
            sourceH = Math.round(BACKGROUND_WIDTH / screenAspect);
            sourceY = (BACKGROUND_HEIGHT - sourceH) * 0.5f;
        } else if (screenAspect < textureAspect) {
            sourceW = Math.round(BACKGROUND_HEIGHT * screenAspect);
            sourceX = (BACKGROUND_WIDTH - sourceW) * 0.5f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.0f, 1.0f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.45f, 0.0f, 0.055f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, -0.45f, 0.0f, 0.055f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.45f, 0.055f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.0f, -0.45f, 0.055f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.35f, 0.35f, 0.035f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, -0.35f, 0.35f, 0.035f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, 0.35f, -0.35f, 0.035f);
        drawBackgroundPass(ctx, sourceX, sourceY, sourceW, sourceH, -0.35f, -0.35f, 0.035f);
        ctx.draw();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawBackgroundPass(DrawContext ctx, float sourceX, float sourceY, int sourceW, int sourceH,
                                    float offsetX, float offsetY, float alpha) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(offsetX, offsetY, 0.0f);
        ctx.drawTexture(RenderLayer::getGuiTextured, BACKGROUND, 0, 0, sourceX, sourceY,
                width, height, sourceW, sourceH, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        ctx.getMatrices().pop();
    }

    private void configureBackgroundFiltering() {
        if (backgroundFilterConfigured || client == null) return;

        try {
            AbstractTexture texture = client.getTextureManager().getTexture(BACKGROUND);
            if (texture != null) {
                texture.setFilter(true, true);
                backgroundFilterConfigured = true;
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
