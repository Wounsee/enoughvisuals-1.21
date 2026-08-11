package fun.enoughvisuals.ui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.accounts.AccountProfile;
import fun.enoughvisuals.systems.accounts.AccountRepository;
import fun.enoughvisuals.systems.accounts.AccountSessionManager;
import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.systems.animations.TimeAnimation;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AccountSwitcherScreen extends Screen {
    private static final int ACCENT = 0xFF7F94DD;
    private static final int NORMAL = 0xE8151515;
    private static final int HOVER = 0xF01F1F1F;
    private static final int FIELD = 0xE3121218;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB7AFCA;
    private static final int SELECTED = 0xFFC49AFF;
    private static final int DELETE = 0xFFFF6E67;
    private static final int VISIBLE_ROWS = 5;


    private final Screen parent;
    private final TimeAnimation openAnimation = new TimeAnimation(420L, Easing.OUT_CUBIC);
    private final AnimatedFloat addHover = new AnimatedFloat(0f, 160L, Easing.OUT_CUBIC);
    private final AnimatedFloat scrollAnimation = new AnimatedFloat(0f, 320L, Easing.OUT_CUBIC);
    private final List<AccountProfile> accounts = new ArrayList<>();
    private final List<RowAnim> rowAnimations = new ArrayList<>();

    private Identifier filteredBackground;
    private float screenScale = 1f;
    private float virtualWidth;
    private float virtualHeight;
    private float panelX;
    private float panelY;
    private float panelW;
    private float inputX;
    private float inputY;
    private float inputW;
    private float inputH;
    private float addX;
    private float addW;
    private float rowX;
    private float rowY;
    private float rowW;
    private float rowH;
    private float rowClipH;
    private float gap;
    private float scrollOffset;
    private float scrollTarget;
    private float maxScroll;
    private boolean inputActive;
    private String input = "";
    private String status = "";
    private int typedCharIndex = -1;
    private long typedCharStartMs;
    private String removedChar = "";
    private long removedCharStartMs;

    public AccountSwitcherScreen(Screen parent) {
        super(Text.literal(ClientLanguage.text("ui.account.title", "Менеджер аккаунтов")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        openAnimation.play(0f, 1f);
        accounts.clear();
        accounts.addAll(AccountRepository.load());
        ensureCurrentAccount();
        rebuildRows();
        status = ClientLanguage.text("ui.account.selected", "Выбран: %s").formatted(currentName());
        updateVirtualScreen();
        layout();
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        updateVirtualScreen();
        layout();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        layout();
        updateScroll();
        drawBackground(ctx);
        drawDim(ctx);

        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        addHover.setTarget(inside(mx, my, addX, inputY, addW, inputH) ? 1f : 0f);

        String current = currentName();
        for (RowAnim row : rowAnimations) {
            float slot = visibleSlot(row);
            float y = rowY + slot * (rowH + gap);
            boolean visible = rowIntersectsList(y) && edgeAlpha(y) > 0.55f;
            row.hover.setTarget(!row.removing && visible && inside(mx, my, rowX, y, rowW, rowH) ? 1f : 0f);
            boolean selected = !row.removing && row.account.name().equalsIgnoreCase(current);
            row.selected.setTarget(selected ? 1f : 0f);
        }

        float open = openAnimation.value();
        float alpha = Easing.clamp(open);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(screenScale, screenScale, 1f);
        renderPanel(ctx, alpha, open);
        ctx.getMatrices().pop();
        rowAnimations.removeIf(RowAnim::isGone);
    }

    private void renderPanel(DrawContext ctx, float alpha, float open) {
        float scale = layoutScale();
        renderScaled(ctx, panelX, panelY - 4f * scale, panelW, 20f * scale,
                1.3f - 0.3f * open, () -> {
                    CustomFontRenderer.drawCenteredText(ctx, ClientLanguage.text("ui.account.title", "Менеджер аккаунтов"), panelX + panelW * 0.5f, panelY, 12.0f * scale,
                            RenderUtil.withAlpha(TEXT, alpha), FontWeight.BOLD);
                });

        float inputAppear = itemProgress(0);
        renderScaled(ctx, inputX, inputY, inputW, inputH, 1.3f - 0.3f * inputAppear, () -> {
            RenderUtil.drawRoundedRect(ctx, inputX, inputY, inputW, inputH, 5.5f * scale,
                    RenderUtil.withAlpha(FIELD, alpha * inputAppear * 0.94f));

            renderInputText(ctx, alpha * inputAppear, scale);
            if (inputActive && ((System.currentTimeMillis() / 460L) % 2L == 0L)) {
                float textSize = 6.0f * scale;
                float cursorX = inputX + 8f * scale + CustomFontRenderer.getTextWidth(input, textSize, FontWeight.SEMIBOLD) + 1.0f * scale;
                cursorX = Math.min(cursorX, inputX + inputW - 6f * scale);
                RenderUtil.drawRect(ctx, cursorX, inputY + 4.7f * scale, 0.65f * scale, inputH - 9.4f * scale,
                        RenderUtil.withAlpha(TEXT, alpha * inputAppear * 0.85f));
            }
        });

        float addAppear = itemProgress(1);
        renderScaled(ctx, addX, inputY, addW, inputH, 1.3f - 0.3f * addAppear,
                () -> renderSmallButton(ctx, ClientLanguage.text("ui.account.add", "Добавить"), addX, inputY, addW, inputH, alpha * addAppear, addHover.value(), true));
        enableRowsScissor(scale);
        renderRows(ctx, alpha, scale);
        RenderUtil.disableScissor();

        if (!status.isBlank()) {
            float statusY = rowY + rowClipH + 7f * scale;
            CustomFontRenderer.drawCenteredText(ctx, status, panelX + panelW * 0.5f, statusY, 4.8f * scale,
                    RenderUtil.withAlpha(MUTED, alpha * 0.82f), FontWeight.SEMIBOLD);
        }
    }

    private void renderInputText(DrawContext ctx, float alpha, float scale) {
        boolean placeholder = input.isBlank() && !inputActive;
        String display = placeholder ? ClientLanguage.text("ui.account.placeholder", "Ник 3-16") : input;
        if (display.isEmpty()) {
            drawAnimatedRemovedChar(ctx, inputX + 8f * scale, inputY + 6.2f * scale, 6.0f * scale, alpha);
            return;
        }

        float textSize = 6.0f * scale;
        float leftPad = 8f * scale;
        float textY = inputY + (inputH - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f - 0.15f * scale;
        float maxW = inputW - leftPad - 7f * scale;
        String trimmed = CustomFontRenderer.trimToWidth(display, maxW, textSize, FontWeight.SEMIBOLD);
        int color = placeholder ? MUTED : TEXT;
        int safeTypedIndex = placeholder ? -1 : Math.min(typedCharIndex, trimmed.length() - 1);
        RenderUtil.drawAnimatedInputText(ctx, trimmed, inputX + leftPad, textY, textSize,
                RenderUtil.withAlpha(color, alpha * (placeholder ? 0.70f : 0.98f)), FontWeight.SEMIBOLD,
                safeTypedIndex, typedCharStartMs);
        drawAnimatedRemovedChar(ctx, inputX + leftPad, textY, textSize, alpha);

        if (typedCharStartMs > 0L && (System.currentTimeMillis() - typedCharStartMs) >= 230L) {
            typedCharIndex = -1;
            typedCharStartMs = 0L;
        }
    }

    private void renderSmallButton(DrawContext ctx, String label, float x, float y, float w, float h,
                                   float alpha, float hover, boolean accent) {
        float scale = layoutScale();
        int base = accent ? ACCENT : NORMAL;
        int hoverColor = accent ? RenderUtil.lighten(ACCENT, 0.08f) : HOVER;
        float grow = 1f + hover * 0.018f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w * 0.5f, y + h * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + w * 0.5f), -(y + h * 0.5f), 0f);
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 5.5f * scale,
                RenderUtil.withAlpha(RenderUtil.lerpColor(base, hoverColor, hover), alpha * 0.95f));
        float textSize = 5.8f * scale;
        float textY = y + (h - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f - 0.15f * scale;
        CustomFontRenderer.drawCenteredText(ctx, label, x + w * 0.5f, textY, textSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        ctx.getMatrices().pop();
    }

    private void renderRows(DrawContext ctx, float alpha, float scale) {
        String current = currentName();
        for (RowAnim anim : rowAnimations) {
            float slot = visibleSlot(anim);
            float y = rowY + slot * (rowH + gap);
            if (!rowIntersectsList(y)) continue;

            int staggerIndex = Math.max(0, Math.round(anim.index.value()));
            float appear = itemProgress(staggerIndex + 2);
            float life = anim.alpha.value();
            float rowAlpha = alpha * appear * life * edgeAlpha(y);
            if (rowAlpha <= 0.003f) continue;

            float hover = anim.hover.value();
            float selectedProgress = anim.selected.value();
            AccountProfile account = anim.account;
            boolean selected = account.name().equalsIgnoreCase(current);
            int bg = selected
                    ? RenderUtil.lerpColor(ACCENT, RenderUtil.lighten(ACCENT, 0.08f), hover * 0.35f)
                    : RenderUtil.lerpColor(NORMAL, HOVER, hover);
            float openScale = 1.3f - 0.3f * appear;
            float lifeScale = anim.removing ? 0.96f + 0.04f * life : 1.3f - 0.3f * life;
            float grow = openScale * lifeScale * (1f + hover * 0.010f);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(rowX + rowW * 0.5f, y + rowH * 0.5f, 0f);
            ctx.getMatrices().scale(grow, grow, 1f);
            ctx.getMatrices().translate(-(rowX + rowW * 0.5f), -(y + rowH * 0.5f), 0f);
            RenderUtil.drawRoundedRect(ctx, rowX, y, rowW, rowH, 5.8f * scale, RenderUtil.withAlpha(bg, rowAlpha * 0.96f));

            float nameSize = 6.1f * scale;
            float currentSize = 4.25f * scale;
            float nameCenterY = y + rowH * 0.5f - CustomFontRenderer.getTextHeight(nameSize, FontWeight.SEMIBOLD) * 0.5f - 0.15f * scale;
            float selectedNameY = y + 5.2f * scale;
            float nameY = nameCenterY + (selectedNameY - nameCenterY) * selectedProgress;
            CustomFontRenderer.drawText(ctx, account.name(), rowX + 8f * scale, nameY,
                    nameSize, RenderUtil.withAlpha(TEXT, rowAlpha), FontWeight.SEMIBOLD);

            float currentAlpha = rowAlpha * selectedProgress;
            if (currentAlpha > 0.004f) {
                CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.account.current", "текущий"), rowX + 8f * scale, y + 13.4f * scale,
                        currentSize, RenderUtil.withAlpha(TEXT, currentAlpha * 0.78f), FontWeight.SEMIBOLD);
            }

            float deleteSize = 6.6f * scale;
            float deleteY = y + (rowH - CustomFontRenderer.getTextHeight(deleteSize, FontWeight.SOLAR_ICONS)) * 0.5f - 0.2f * scale;
            CustomFontRenderer.drawCenteredText(ctx, SolarIcons.CLOSE, rowX + rowW - 12.5f * scale, deleteY,
                    deleteSize, RenderUtil.withAlpha(DELETE, rowAlpha * (0.82f + hover * 0.18f)), FontWeight.SOLAR_ICONS);
            ctx.getMatrices().pop();
        }
    }

    private float itemProgress(int index) {
        return Easing.OUT_CUBIC.apply((openAnimation.progress() - 0.08f - index * 0.045f) / 0.42f);
    }

    private float visibleSlot(RowAnim row) {
        return row.index.value() + scrollOffset / Math.max(1f, rowH + gap);
    }

    private boolean rowIntersectsList(float y) {
        float[] clip = rowsClipBounds(layoutScale());
        return y + rowH >= clip[0] && y <= clip[1];
    }

    private float edgeAlpha(float y) {
        float[] clip = rowsClipBounds(layoutScale());
        float top = (y + rowH - clip[0]) / Math.max(1f, rowH * 0.78f);
        float bottom = (clip[1] - y) / Math.max(1f, rowH * 0.78f);
        return Easing.OUT_CUBIC.apply(Math.min(top, bottom));
    }

    private void enableRowsScissor(float scale) {
        float[] clip = rowsClipBounds(scale);
        RenderUtil.enableScissor(clip[2] * screenScale, clip[0] * screenScale,
                clip[3] * screenScale, (clip[1] - clip[0]) * screenScale);
    }

    private float[] rowsClipBounds(float scale) {
        float topGap = Math.max(0f, rowY - (inputY + inputH));
        float statusY = rowY + rowClipH + 7f * scale;
        float statusTopGap = Math.max(0f, statusY - (rowY + rowClipH));
        float topPad = Math.min(3.5f * scale, Math.max(0f, topGap - 1.2f * scale));
        float bottomPad = Math.min(3.5f * scale, Math.max(0f, statusTopGap - 2.0f * scale));
        float xPad = rowW * 0.17f + 8.0f * scale;
        return new float[]{
                rowY - topPad,
                rowY + rowClipH + bottomPad,
                rowX - xPad,
                rowW + xPad * 2f
        };
    }

    private void renderScaled(DrawContext ctx, float x, float y, float w, float h, float scale, Runnable render) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + w * 0.5f, y + h * 0.5f, 0f);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-(x + w * 0.5f), -(y + h * 0.5f), 0f);
        render.run();
        ctx.getMatrices().pop();
    }

    private void drawAnimatedRemovedChar(DrawContext ctx, float x, float y, float size, float alpha) {
        if (removedChar.isEmpty() || removedCharStartMs <= 0L) return;
        float progress = Math.max(0f, Math.min(1f, (System.currentTimeMillis() - removedCharStartMs) / 180f));
        if (progress >= 0.999f) {
            removedChar = "";
            removedCharStartMs = 0L;
            return;
        }

        float symbolW = CustomFontRenderer.getTextWidth(removedChar, size, FontWeight.SEMIBOLD);
        float symbolX = x + CustomFontRenderer.getTextWidth(input, size, FontWeight.SEMIBOLD);
        float scale = 1f - 0.72f * RenderUtil.easeInQuad(progress);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(symbolX + symbolW * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * progress));
        ctx.getMatrices().scale(scale, scale, 1f);
        CustomFontRenderer.drawText(ctx, removedChar, -symbolW * 0.5f, -size * 0.5f, size,
                RenderUtil.withAlpha(TEXT, alpha * (1f - progress)), FontWeight.SEMIBOLD);
        ctx.getMatrices().pop();
    }

    private void ensureCurrentAccount() {
        String current = currentName();
        if (AccountRepository.isValidName(current) && !AccountRepository.containsName(accounts, current)) {
            UUID uuid = client != null && client.getSession() != null && client.getSession().getUuidOrNull() != null
                    ? client.getSession().getUuidOrNull()
                    : AccountSessionManager.offlineUuid(current);
            accounts.add(new AccountProfile(current, uuid, System.currentTimeMillis()));
            AccountRepository.save(accounts);
        }
    }

    private void rebuildRows() {
        rowAnimations.clear();
        for (int i = 0; i < accounts.size(); i++) {
            rowAnimations.add(RowAnim.visible(accounts.get(i), i));
        }
    }

    private void updateRowTargets() {
        for (int i = 0; i < accounts.size(); i++) {
            AccountProfile account = accounts.get(i);
            RowAnim row = findRow(account);
            if (row == null) {
                row = RowAnim.appearing(account, i);
                rowAnimations.add(row);
            }
            row.showAt(i);
        }
    }

    private RowAnim findRow(AccountProfile account) {
        for (RowAnim row : rowAnimations) {
            if (sameAccount(row.account, account)) {
                return row;
            }
        }
        return null;
    }

    private static boolean sameAccount(AccountProfile a, AccountProfile b) {
        return a != null && b != null && a.name().equalsIgnoreCase(b.name());
    }

    private void addAccount() {
        String name = input.trim();
        if (!AccountRepository.isValidName(name)) {
            return;
        }
        if (AccountRepository.containsName(accounts, name)) {
            return;
        }
        AccountProfile profile = AccountProfile.offline(name);
        accounts.add(0, profile);
        rowAnimations.add(0, RowAnim.appearing(profile, 0f));
        scrollTarget = 0f;
        updateRowTargets();
        AccountRepository.save(accounts);
        updateInput("");
    }

    private void switchAccount(AccountProfile profile) {
        if (profile == null) return;
        AccountSessionManager.switchTo(profile);
        status = ClientLanguage.text("ui.account.selected", "Выбран: %s").formatted(profile.name());
        EnoughSoundPlayer.playScroll();
    }

    private void removeAccount(AccountProfile account) {
        if (account == null) return;
        int index = indexOfAccount(account);
        if (index < 0) return;

        AccountProfile removed = accounts.remove(index);
        RowAnim row = findRow(removed);
        if (row != null) {
            row.remove();
        }
        updateRowTargets();
        AccountRepository.save(accounts);
        if (removed.name().equalsIgnoreCase(currentName())) {
            status = "";
        }
    }

    private int indexOfAccount(AccountProfile account) {
        for (int i = 0; i < accounts.size(); i++) {
            if (sameAccount(accounts.get(i), account)) {
                return i;
            }
        }
        return -1;
    }

    private void layout() {
        float scale = layoutScale();
        panelW = Math.min(220f * scale, virtualWidth - 24f * scale);
        panelX = virtualWidth * 0.5f - panelW * 0.5f;
        panelY = virtualHeight * 0.5f - 92f * scale;
        inputX = panelX;
        inputY = panelY + 30f * scale;
        inputW = 148f * scale;
        inputH = 19.5f * scale;
        addX = inputX + inputW + 5f * scale;
        addW = panelW - inputW - 5f * scale;
        rowX = panelX;
        rowY = inputY + inputH + 6f * scale;
        rowW = panelW;
        rowH = 22.5f * scale;
        gap = 4.0f * scale;
        float maxListH = (rowH + gap) * VISIBLE_ROWS - gap;
        float availableH = Math.max(rowH, virtualHeight - rowY - 30f * scale);
        rowClipH = Math.min(maxListH, availableH);
        maxScroll = Math.max(0f, Math.max(0, accounts.size()) * (rowH + gap) - gap - rowClipH);
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
        if (scrollOffset < -maxScroll || scrollOffset > 0f) {
            scrollOffset = clamp(scrollOffset, -maxScroll, 0f);
            scrollAnimation.snap(scrollOffset);
        }
    }

    private void updateScroll() {
        maxScroll = Math.max(0f, Math.max(0, accounts.size()) * (rowH + gap) - gap - rowClipH);
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
        scrollAnimation.setTarget(scrollTarget);
        scrollOffset = clamp(scrollAnimation.value(), -maxScroll, 0f);
    }

    private float layoutScale() {
        return Math.max(0.58f, Math.min(1.14f, Math.min(virtualWidth / 960f, virtualHeight / 540f)));
    }

    private void updateVirtualScreen() {
        screenScale = ScreenScaleUtil.scale();
        virtualWidth = ScreenScaleUtil.virtualWidth();
        virtualHeight = ScreenScaleUtil.virtualHeight();
        if (virtualWidth <= 0f) virtualWidth = width / Math.max(0.001f, screenScale);
        if (virtualHeight <= 0f) virtualHeight = height / Math.max(0.001f, screenScale);
    }

    private void drawBackground(DrawContext ctx) {
        configureBackgroundFiltering();

        Identifier background = MenuBackgroundManager.texture();
        int backgroundWidth = MenuBackgroundManager.textureWidth();
        int backgroundHeight = MenuBackgroundManager.textureHeight();

        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = backgroundWidth / (float) backgroundHeight;
        float sourceX = 0f;
        float sourceY = 0f;
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
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.0f, 1.0f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.45f, 0.0f, 0.055f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.45f, 0.0f, 0.055f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, 0.45f, 0.055f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.0f, -0.45f, 0.055f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.35f, 0.35f, 0.035f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.35f, 0.35f, 0.035f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, 0.35f, -0.35f, 0.035f);
        drawBackgroundPass(ctx, background, backgroundWidth, backgroundHeight, sourceX, sourceY, sourceW, sourceH, -0.35f, -0.35f, 0.035f);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawBackgroundPass(DrawContext ctx, Identifier background, int backgroundWidth, int backgroundHeight,
                                    float sourceX, float sourceY, int sourceW, int sourceH,
                                    float offsetX, float offsetY, float alpha) {
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(offsetX, offsetY, 0f);
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
        RenderUtil.drawRect(ctx, 0f, 0f, width, height, 0x4A000000);
        RenderUtil.drawGradientRect(ctx, 0f, 0f, width, height * 0.30f, 0x5C000000, 0x00000000);
        RenderUtil.drawGradientRect(ctx, 0f, height * 0.66f, width, height * 0.34f, 0x00000000, 0x6A000000);
    }

    private String currentName() {
        if (client == null || client.getSession() == null || client.getSession().getUsername() == null) {
            return "Player";
        }
        return client.getSession().getUsername();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        inputActive = inside(mx, my, inputX, inputY, inputW, inputH);

        if (inside(mx, my, addX, inputY, addW, inputH)) {
            addAccount();
            return true;
        }

        float scale = layoutScale();
        for (RowAnim row : rowAnimations) {
            if (row.removing) continue;
            float slot = visibleSlot(row);
            float y = rowY + slot * (rowH + gap);
            if (!rowIntersectsList(y) || edgeAlpha(y) <= 0.55f) continue;
            if (!inside(mx, my, rowX, y, rowW, rowH)) continue;
            if (mx >= rowX + rowW - 27f * scale) {
                removeAccount(row.account);
            } else {
                switchAccount(row.account);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        float[] clip = rowsClipBounds(layoutScale());
        if (inside(mx, my, clip[2], clip[0], clip[3], clip[1] - clip[0]) && maxScroll > 0.5f) {
            scrollTarget += (float) (verticalAmount * (rowH + gap) * 0.76f);
            scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
            scrollAnimation.setTarget(scrollTarget);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        if (!inputActive) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            addAccount();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()) {
            updateInput(input.substring(0, input.length() - 1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!inputActive) return super.charTyped(chr, modifiers);
        if (input.length() >= 16) return true;
        if (Character.isLetterOrDigit(chr) || chr == '_') {
            updateInput(input + chr);
            status = "";
            return true;
        }
        return true;
    }

    private void updateInput(String next) {
        next = next == null ? "" : next;
        if (next.equals(input)) return;

        if (next.length() == input.length() + 1 && next.startsWith(input)) {
            typedCharIndex = input.length();
            typedCharStartMs = System.currentTimeMillis();
            removedChar = "";
            removedCharStartMs = 0L;
        } else if (next.length() + 1 == input.length() && input.startsWith(next)) {
            removedChar = input.substring(input.length() - 1);
            removedCharStartMs = System.currentTimeMillis();
            typedCharIndex = -1;
            typedCharStartMs = 0L;
        } else {
            typedCharIndex = -1;
            typedCharStartMs = 0L;
            removedChar = "";
            removedCharStartMs = 0L;
        }

        input = next;
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class RowAnim {
        private final AccountProfile account;
        private final AnimatedFloat hover = new AnimatedFloat(0f, 160L, Easing.OUT_CUBIC);
        private final AnimatedFloat selected = new AnimatedFloat(0f, 190L, Easing.OUT_CUBIC);
        private final AnimatedFloat index;
        private final AnimatedFloat alpha;
        private boolean removing;

        private RowAnim(AccountProfile account, float index, float alpha) {
            this.account = account;
            this.index = new AnimatedFloat(index, 280L, Easing.OUT_CUBIC);
            this.alpha = new AnimatedFloat(alpha, 220L, Easing.OUT_CUBIC);
        }

        private static RowAnim visible(AccountProfile account, int index) {
            return new RowAnim(account, index, 1f);
        }

        private static RowAnim appearing(AccountProfile account, float fromIndex) {
            RowAnim row = new RowAnim(account, fromIndex, 0f);
            row.alpha.setTarget(1f);
            return row;
        }

        private void showAt(int targetIndex) {
            removing = false;
            index.setTarget(targetIndex);
            alpha.setTarget(1f);
        }

        private void remove() {
            removing = true;
            hover.setTarget(0f);
            selected.setTarget(0f);
            alpha.setTarget(0f);
        }

        private boolean isGone() {
            return removing && alpha.value() <= 0.025f;
        }
    }
}
