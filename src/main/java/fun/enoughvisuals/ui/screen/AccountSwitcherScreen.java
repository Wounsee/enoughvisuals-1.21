package fun.enoughvisuals.ui.screen;

import com.mojang.authlib.GameProfile;
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
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AccountSwitcherScreen extends Screen {
    private static final int ACCENT = 0xFF7F94DD;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFB7AFCA;
    private static final int CARD = 0xFF151515;
    private static final int CARD_HOVER = 0xFF1F1F1F;
    private static final int FIELD = 0xFF121218;
    private static final int DELETE = 0xFFFF6E67;
    private static final int COLUMNS = 3;
    private static final int VISIBLE_ROWS = 3;
    private static final long OPEN_MS = 500L;
    private static final long CLOSE_MS = 300L;
    private static final long FAVORITE_TRANSFER_MS = 560L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yy");

    private final Screen parent;
    private final TimeAnimation openAnimation = new TimeAnimation(OPEN_MS, Easing.OUT_CUBIC);
    private final AnimatedFloat addHover = new AnimatedFloat(0f, 150L, Easing.OUT_CUBIC);
    private final AnimatedFloat scrollAnimation = new AnimatedFloat(0f, 280L, Easing.OUT_CUBIC);
    private final AnimatedFloat likedSection = new AnimatedFloat(0f, 420L, Easing.OUT_CUBIC);
    private final AnimatedFloat likedExtraRows = new AnimatedFloat(0f, 420L, Easing.OUT_CUBIC);
    private final List<AccountProfile> accounts = new ArrayList<>();
    private final List<RowAnim> rowAnimations = new ArrayList<>();
    private final Set<String> likedNames = new HashSet<>();
    private final Map<UUID, Identifier> skins = new HashMap<>();

    private float screenScale = 1f;
    private float virtualWidth;
    private float virtualHeight;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float cardW;
    private float cardH;
    private float columnGap;
    private float rowGap;
    private float likedX;
    private float likedY;
    private float allX;
    private float allLabelY;
    private float allY;
    private float allClipH;
    private float contentTop;
    private float contentBottom;
    private float scrollContentH;
    private float inputX;
    private float inputY;
    private float inputW;
    private float inputH;
    private float addX;
    private float addW;
    private float scrollOffset;
    private float scrollTarget;
    private float maxScroll;
    private boolean inputActive;
    private String input = "";
    private int typedCharIndex = -1;
    private long typedCharStartMs;
    private String removedChar = "";
    private long removedCharStartMs;
    private boolean closing;

    public AccountSwitcherScreen(Screen parent) {
        super(Text.literal(ClientLanguage.text("ui.account.title", "Аккаунты")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        closing = false;
        openAnimation.duration(OPEN_MS).easing(Easing.OUT_CUBIC).play(0f, 1f);
        accounts.clear();
        accounts.addAll(AccountRepository.load());
        ensureCurrentAccount();
        likedNames.clear();
        likedNames.addAll(AccountRepository.loadLikedNames());
        likedSection.snap(likedNames.isEmpty() ? 0f : 1f);
        rebuildRows();
        likedExtraRows.snap(Math.max(0, likedRowCount() - 1));
        primeSkins();
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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RenderUtil.beginFrame();
        updateVirtualScreen();
        layout();
        updateScroll();
        layout();
        updateRowPositions();
        MenuBackgroundManager.drawSelectedBackground(context, width, height);
        GlassUtil.BLUR_OFFSET = 1.35f;
        GlassUtil.beginFrame();

        if (closing && openAnimation.finished() && openAnimation.value() <= 0.01f) {
            returnToParent();
            return;
        }

        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        addHover.setTarget(!closing && inside(mx, my, addX, inputY, addW, inputH) ? 1f : 0f);
        updateCardAnimations(mx, my);

        float open = openAnimation.value();
        float alpha = Easing.clamp(open);
        context.getMatrices().push();
        context.getMatrices().scale(screenScale, screenScale, 1f);
        context.getMatrices().push();
        float contentScale = 0.985f + open * 0.015f;
        context.getMatrices().translate(virtualWidth * 0.5f, virtualHeight * 0.5f, 0f);
        context.getMatrices().scale(contentScale, contentScale, 1f);
        context.getMatrices().translate(-virtualWidth * 0.5f, -virtualHeight * 0.5f, 0f);
        renderPanel(context, alpha, open);
        context.getMatrices().pop();
        context.getMatrices().pop();
        rowAnimations.removeIf(RowAnim::isGone);
    }

    private void renderPanel(DrawContext context, float alpha, float open) {
        float scale = layoutScale();
        GlassUtil.drawGlassPanel(context, panelX, panelY, panelW, panelH, 6.5f * scale,
                0x151515, 0.72f * alpha, alpha);

        float titleY = panelY - 19.5f * scale + (1f - open) * 3f * scale;
        CustomFontRenderer.drawCenteredText(context,
                ClientLanguage.text("ui.account.title", "Аккаунты"),
                virtualWidth * 0.5f, titleY, 10.5f * scale,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.BOLD);

        float contentAlpha = alpha * itemProgress(0);
        enableContentScissor();
        float likedReveal = likedSection.value();
        if (likedReveal > 0.003f) {
            float likedShift = (1f - likedReveal) * 3f * scale;
            float dividerX = renderSectionLabel(context,
                    ClientLanguage.text("ui.account.liked", "Избранные"),
                    SolarIcons.HEART, panelY + 8f * scale + scrollOffset + likedShift,
                    contentAlpha * likedReveal);
            renderDivider(context, panelY + 11.5f * scale + scrollOffset + likedShift,
                    dividerX, contentAlpha * likedReveal);
            renderLikedCards(context, alpha * likedReveal, likedShift);
        }

        float visibleAllLabelY = allLabelY + scrollOffset;
        float allDividerX = renderSectionLabel(context, ClientLanguage.text("ui.account.all", "Все"),
                SolarIcons.USER, visibleAllLabelY, contentAlpha);
        renderDivider(context, visibleAllLabelY + 3.5f * scale, allDividerX, contentAlpha);
        renderAllCards(context, alpha);
        renderTransfers(context, alpha);
        RenderUtil.disableScissor();
        renderScrollbar(context, alpha);

        float settingsY = panelY + panelH - 33.5f * scale;
        float settingsDividerX = renderSectionLabel(context,
                ClientLanguage.text("ui.account.settings", "Настройки"),
                SolarIcons.SETTINGS, settingsY, contentAlpha);
        renderDivider(context, settingsY + 3.5f * scale, settingsDividerX, contentAlpha);
        renderInput(context, alpha * itemProgress(1));
        renderCreateButton(context, alpha * itemProgress(2));
    }

    private float renderSectionLabel(DrawContext context, String label, String icon,
                                     float y, float alpha) {
        float scale = layoutScale();
        float x = panelX + 12f * scale;
        float size = 5.2f * scale;
        CustomFontRenderer.drawText(context, label, x, y, size,
                RenderUtil.withAlpha(MUTED, alpha * 0.62f), FontWeight.SEMIBOLD);
        float iconSize = 5.1f * scale;
        float iconX = x + CustomFontRenderer.getTextWidth(label, size, FontWeight.SEMIBOLD) + 2f * scale;
        float iconY = y + (CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)
                - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawText(context, icon, iconX, iconY, iconSize,
                RenderUtil.withAlpha(MUTED, alpha * 0.62f), FontWeight.SOLAR_ICONS);
        return iconX + CustomFontRenderer.getTextWidth(icon, iconSize, FontWeight.SOLAR_ICONS)
                + 6f * scale;
    }

    private void renderDivider(DrawContext context, float y, float startX, float alpha) {
        float scale = layoutScale();
        float right = panelX + panelW - 16f * scale;
        RenderUtil.drawRect(context, startX, y, Math.max(1f, right - startX), 0.5f * scale,
                RenderUtil.withAlpha(MUTED, alpha * 0.10f));
    }

    private void renderLikedCards(DrawContext context, float alpha, float shiftY) {
        List<RowAnim> liked = likedRows();
        int count = liked.size();
        for (int i = 0; i < count; i++) {
            RowAnim row = liked.get(i);
            if (isTransferring(row)) continue;
            float appear = itemProgress(i + 1);
            renderCard(context, row, row.displayX, visibleY(row),
                    alpha * appear * row.alpha.value(), appear);
        }
    }

    private void renderAllCards(DrawContext context, float alpha) {
        List<RowAnim> all = allRows();
        for (int index = 0; index < all.size(); index++) {
            RowAnim row = all.get(index);
            if (isTransferring(row)) continue;
            float x = row.displayX;
            float y = visibleY(row);
            float clipTop = contentTop;
            float clipBottom = contentBottom;
            if (y + cardH < clipTop || y > clipBottom) continue;
            int visibleIndex = Math.max(0, Math.min(VISIBLE_ROWS * COLUMNS - 1, index));
            float appear = itemProgress(visibleIndex + 3);
            float edge = edgeAlpha(y);
            renderCard(context, row, x, y, alpha * appear * row.alpha.value() * edge, appear);
        }
    }

    private void renderTransfers(DrawContext context, float alpha) {
        for (RowAnim row : rowAnimations) {
            if (!isTransferring(row)) continue;
            float wave = (float) Math.sin(Math.PI * row.transferProgress());
            context.getMatrices().push();
            float y = visibleY(row);
            context.getMatrices().translate(row.displayX + cardW * 0.5f,
                    y + cardH * 0.5f, 20f);
            context.getMatrices().scale(1f + wave * 0.035f, 1f + wave * 0.035f, 1f);
            context.getMatrices().translate(-(row.displayX + cardW * 0.5f),
                    -(y + cardH * 0.5f), 0f);
            renderCard(context, row, row.displayX, y, alpha * row.alpha.value(), 1f);
            context.getMatrices().pop();
        }
    }

    private boolean isTransferring(RowAnim row) {
        return row != null && row.transferring;
    }

    private void renderCard(DrawContext context, RowAnim row, float x, float y,
                            float alpha, float appear) {
        if (alpha <= 0.003f) return;
        float scale = layoutScale();
        float hover = row.hover.value();
        float selected = row.selected.value();
        float creation = row.spawn.value();
        float shiftedY = y + (1f - appear) * 3f * scale + (1f - creation) * 7f * scale;
        float grow = (0.985f + appear * 0.015f)
                * (0.94f + creation * 0.06f) * (1f + hover * 0.008f);
        context.getMatrices().push();
        context.getMatrices().translate(x + cardW * 0.5f, shiftedY + cardH * 0.5f, 0f);
        context.getMatrices().scale(grow, grow, 1f);
        context.getMatrices().translate(-(x + cardW * 0.5f), -(shiftedY + cardH * 0.5f), 0f);

        int background = RenderUtil.lerpColor(CARD, CARD_HOVER, hover);
        RenderUtil.drawRoundedRect(context, x, shiftedY, cardW, cardH, 6f * scale,
                RenderUtil.withAlpha(background, alpha * 0.92f));

        float avatar = 14f * scale;
        Identifier skin = skins.getOrDefault(row.account.uuid(),
                DefaultSkinHelper.getSkinTextures(row.account.uuid()).texture());
        Draw.imagePlayerHeadRounded(context, skin, x + 4.5f * scale,
                shiftedY + 5.25f * scale, avatar, 2.5f * scale, alpha);

        if (selected > 0.02f) {
            float badgeX = x + 17.1f * scale;
            float badgeY = shiftedY + 17.9f * scale;
            float badgeRadius = 3.1f * scale;
            RenderUtil.drawCrispCircle(context, badgeX, badgeY, badgeRadius,
                    RenderUtil.withAlpha(ACCENT, alpha * selected * 0.96f));
            drawCenteredIcon(context, SolarIcons.CHECK, badgeX, badgeY,
                    3.7f * scale, RenderUtil.withAlpha(TEXT, alpha * selected * 0.92f));
        }

        float textX = x + 21.5f * scale;
        float nameSize = 5.6f * scale;
        String name = CustomFontRenderer.trimToWidth(row.account.name(), cardW - 42f * scale,
                nameSize, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, name, textX, shiftedY + 4f * scale, nameSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(context, formatDate(row.account.createdAt()), textX,
                shiftedY + 12.5f * scale, 4.3f * scale,
                RenderUtil.withAlpha(MUTED, alpha * 0.72f), FontWeight.SEMIBOLD);

        boolean liked = likedNames.contains(normalize(row.account.name()));
        float favoriteHover = row.favoriteHover.value();
        float deleteHover = row.deleteHover.value();
        float iconCenterY = shiftedY + cardH * 0.5f;
        float starSize = 4.8f * scale * (1f + favoriteHover * 0.15f);
        drawCenteredIcon(context, liked ? SolarIcons.FEEDBACK_STAR : SolarIcons.STAR,
                x + cardW - 10.5f * scale, iconCenterY, starSize,
                RenderUtil.withAlpha(liked ? ACCENT : MUTED, alpha * (liked ? 0.95f : 0.45f + favoriteHover * 0.45f)));
        float closeSize = 4.5f * scale * (1f + deleteHover * 0.13f);
        drawCenteredIcon(context, SolarIcons.CLOSE,
                x + cardW - 4.4f * scale, iconCenterY, closeSize,
                RenderUtil.withAlpha(RenderUtil.lerpColor(MUTED, DELETE, deleteHover), alpha * (0.45f + deleteHover * 0.50f)));
        context.getMatrices().pop();
    }

    private void renderInput(DrawContext context, float alpha) {
        float scale = layoutScale();
        RenderUtil.drawRoundedRect(context, inputX, inputY, inputW, inputH, 5f * scale,
                RenderUtil.withAlpha(FIELD, alpha * 0.89f));
        boolean placeholder = input.isBlank() && !inputActive;
        String display = placeholder ? ClientLanguage.text("ui.account.placeholder", "Никнейм...") : input;
        float size = 4.75f * scale;
        float textY = inputY + (inputH - CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)) * 0.5f;
        String trimmed = CustomFontRenderer.trimToWidth(display, inputW - 10f * scale, size, FontWeight.SEMIBOLD);
        int safeTypedIndex = placeholder ? -1 : Math.min(typedCharIndex, trimmed.length() - 1);
        RenderUtil.drawAnimatedInputText(context, trimmed, inputX + 5f * scale, textY, size,
                RenderUtil.withAlpha(placeholder ? MUTED : TEXT, alpha * (placeholder ? 0.82f : 1f)), FontWeight.SEMIBOLD,
                safeTypedIndex, typedCharStartMs);
        drawAnimatedRemovedChar(context, inputX + 5f * scale, textY, size, alpha);
        if (inputActive && ((System.currentTimeMillis() / 460L) % 2L == 0L)) {
            float cursorX = inputX + 5f * scale
                    + CustomFontRenderer.getTextWidth(input, size, FontWeight.SEMIBOLD) + 0.6f * scale;
            cursorX = Math.min(cursorX, inputX + inputW - 4f * scale);
            RenderUtil.drawRect(context, cursorX, inputY + 3.2f * scale, 0.5f * scale,
                    inputH - 6.4f * scale, RenderUtil.withAlpha(TEXT, alpha * 0.62f));
        }
    }

    private void renderCreateButton(DrawContext context, float alpha) {
        float scale = layoutScale();
        float hover = addHover.value();
        float grow = 1f + hover * 0.012f;
        context.getMatrices().push();
        context.getMatrices().translate(addX + addW * 0.5f, inputY + inputH * 0.5f, 0f);
        context.getMatrices().scale(grow, grow, 1f);
        context.getMatrices().translate(-(addX + addW * 0.5f), -(inputY + inputH * 0.5f), 0f);
        RenderUtil.drawRoundedRect(context, addX, inputY, addW, inputH, 5f * scale,
                RenderUtil.withAlpha(RenderUtil.lerpColor(ACCENT, RenderUtil.lighten(ACCENT, 0.08f), hover), alpha * 0.98f));
        String label = ClientLanguage.text("ui.account.add", "Создать");
        float textSize = 4.75f * scale;
        float textY = inputY + (inputH - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f;
        CustomFontRenderer.drawText(context, label, addX + 4.5f * scale, textY, textSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        float iconSize = 5f * scale;
        float iconY = inputY + (inputH - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawRightText(context, SolarIcons.USER, addX + addW - 4f * scale,
                iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SOLAR_ICONS);
        context.getMatrices().pop();
    }

    private void renderScrollbar(DrawContext context, float alpha) {
        if (maxScroll <= 0.5f) return;
        float scale = layoutScale();
        float thumbH = Math.max(19.5f * scale, allClipH * allClipH / scrollContentH);
        float travel = Math.max(0f, allClipH - thumbH);
        float progress = clamp(-scrollOffset / maxScroll, 0f, 1f);
        float x = panelX + panelW - 9.5f * scale;
        float y = contentTop + travel * progress;
        RenderUtil.drawRoundedRect(context, x, contentTop, 4f * scale, allClipH, 2f * scale,
                RenderUtil.withAlpha(MUTED, alpha * 0.10f));
        RenderUtil.drawRoundedRect(context, x + 1f * scale, y, 2f * scale, thumbH, 1f * scale,
                RenderUtil.withAlpha(ACCENT, alpha * 0.62f));
    }

    private void updateCardAnimations(float mouseX, float mouseY) {
        String current = currentName();
        List<RowAnim> liked = likedRows();
        List<RowAnim> allRows = allRows();
        for (RowAnim row : rowAnimations) {
            boolean over = false;
            boolean overFavorite = false;
            boolean overDelete = false;
            if (!row.removing && !isTransferring(row)) {
                int likedIndex = liked.indexOf(row);
                if (likedIndex >= 0) {
                    float x = row.displayX;
                    float y = visibleY(row);
                    if (y + cardH >= contentTop && y <= contentBottom
                            && inside(mouseX, mouseY, x, y, cardW, cardH)) {
                        over = true;
                        overFavorite = overFavorite(mouseX, mouseY, x, y);
                        overDelete = overDelete(mouseX, mouseY, x, y);
                    }
                }
                int allIndex = allRows.indexOf(row);
                if (allIndex >= 0) {
                    float x = row.displayX;
                    float y = visibleY(row);
                    if (y + cardH >= contentTop
                            && y <= contentBottom
                            && inside(mouseX, mouseY, x, y, cardW, cardH)) {
                        over = true;
                        overFavorite |= overFavorite(mouseX, mouseY, x, y);
                        overDelete |= overDelete(mouseX, mouseY, x, y);
                    }
                }
            }
            row.hover.setTarget(over ? 1f : 0f);
            row.favoriteHover.setTarget(overFavorite ? 1f : 0f);
            row.deleteHover.setTarget(overDelete ? 1f : 0f);
            row.selected.setTarget(!row.removing
                    && row.account.name().equalsIgnoreCase(current) ? 1f : 0f);
        }
    }

    private boolean overFavorite(float mouseX, float mouseY, float x, float y) {
        float scale = layoutScale();
        return inside(mouseX, mouseY, x + cardW - 14.8f * scale, y,
                7.6f * scale, cardH);
    }

    private boolean overDelete(float mouseX, float mouseY, float x, float y) {
        float scale = layoutScale();
        return inside(mouseX, mouseY, x + cardW - 7.2f * scale, y,
                7.2f * scale, cardH);
    }

    private float[] allPosition(int index) {
        int column = Math.floorMod(index, COLUMNS);
        int row = Math.max(0, index / COLUMNS);
        return new float[]{
                allX + column * (cardW + columnGap),
                allY + row * (cardH + rowGap)
        };
    }

    private float visibleY(RowAnim row) {
        return row.displayY + scrollOffset;
    }

    private List<RowAnim> likedRows() {
        List<RowAnim> result = new ArrayList<>();
        for (RowAnim row : rowAnimations) {
            if (!row.removing && likedNames.contains(normalize(row.account.name()))) result.add(row);
        }
        return result;
    }

    private int likedRowCount() {
        int count = 0;
        for (RowAnim row : rowAnimations) {
            if (!row.removing && likedNames.contains(normalize(row.account.name()))) count++;
        }
        return (count + COLUMNS - 1) / COLUMNS;
    }

    private List<RowAnim> allRows() {
        List<RowAnim> result = new ArrayList<>();
        for (RowAnim row : rowAnimations) {
            if (!row.removing && !likedNames.contains(normalize(row.account.name()))) result.add(row);
        }
        return result;
    }

    private void enableContentScissor() {
        RenderUtil.enableScissor(0f, contentTop * screenScale,
                virtualWidth * screenScale, (contentBottom - contentTop) * screenScale);
    }

    private float edgeAlpha(float y) {
        float top = (y + cardH - contentTop) / Math.max(1f, cardH * 0.72f);
        float bottom = (contentBottom - y) / Math.max(1f, cardH * 0.72f);
        return Easing.OUT_CUBIC.apply(Math.min(top, bottom));
    }

    private void updateRowPositions() {
        List<RowAnim> liked = likedRows();
        List<RowAnim> all = allRows();
        float frame = RenderUtil.getFrameDelta();
        float scale = layoutScale();
        for (RowAnim row : rowAnimations) {
            if (row.removing) continue;
            float targetX;
            float targetY;
            int likedIndex = liked.indexOf(row);
            if (likedIndex >= 0) {
                targetX = likedX + (likedIndex % COLUMNS) * (cardW + columnGap);
                targetY = likedY + (likedIndex / COLUMNS) * (cardH + rowGap);
            } else {
                int allIndex = Math.max(0, all.indexOf(row));
                float[] target = allPosition(allIndex);
                targetX = target[0];
                targetY = target[1];
            }

            if (!row.positionReady) {
                row.displayX = targetX;
                row.displayY = targetY;
                row.positionReady = true;
            } else if (row.transferring) {
                float progress = row.transferProgress();
                float eased = Easing.IN_OUT_CUBIC.apply(progress);
                float wave = (float) Math.sin(Math.PI * eased);
                row.displayX = row.transferFromX + (targetX - row.transferFromX) * eased;
                row.displayY = row.transferFromY + (targetY - row.transferFromY) * eased
                        - wave * 10f * scale;
                if (progress >= 1f) {
                    row.transferring = false;
                    row.displayX = targetX;
                    row.displayY = targetY;
                }
            } else {
                row.displayX = RenderUtil.animate(row.displayX, targetX, 0.12f, frame);
                row.displayY = RenderUtil.animate(row.displayY, targetY, 0.12f, frame);
            }
        }
    }

    private float itemProgress(int index) {
        return Easing.OUT_CUBIC.apply((openAnimation.value() - 0.06f - index * 0.022f) / 0.48f);
    }

    private void layout() {
        float scale = layoutScale();
        panelW = Math.min(314.5f * scale, virtualWidth - 24f * scale);
        float extraLikedRows = likedExtraRows.value();
        panelH = Math.min(179f * scale, virtualHeight - 66f * scale);
        panelX = virtualWidth * 0.5f - panelW * 0.5f;
        panelY = virtualHeight * 0.5f - panelH * 0.5f;
        cardH = 24.5f * scale;
        columnGap = 6.5f * scale;
        rowGap = 5f * scale;
        cardW = (panelW - 24f * scale - columnGap * (COLUMNS - 1)) / COLUMNS;
        contentTop = panelY + 5.5f * scale;
        contentBottom = panelY + panelH - 37f * scale;
        likedX = panelX + 12f * scale;
        allX = likedX;
        float likedReveal = likedSection.value();
        float likedHeight = 41f * likedReveal + 29.5f * extraLikedRows;
        inputX = panelX + 12f * scale;
        inputY = panelY + panelH - 20f * scale;
        inputW = 75f * scale;
        inputH = 14f * scale;
        addX = inputX + 80.5f * scale;
        addW = 45.5f * scale;
        int rows = (allRows().size() + COLUMNS - 1) / COLUMNS;
        float contentH = rows <= 0 ? 0f : rows * (cardH + rowGap) - rowGap;
        int favoriteRows = likedRowCount();
        float unscrolledAllY = panelY + (19.5f + likedHeight) * scale;
        float allBottom = unscrolledAllY + contentH;
        float likedBottom = favoriteRows <= 0 ? panelY + 12f * scale
                : panelY + 19.5f * scale + (favoriteRows - 1) * (cardH + rowGap) + cardH;
        float contentEnd = Math.max(allBottom, likedBottom);
        allClipH = Math.max(1f, contentBottom - contentTop);
        scrollContentH = Math.max(allClipH, contentEnd - contentTop);
        maxScroll = Math.max(0f, contentEnd - contentBottom);
        scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
        scrollOffset = clamp(scrollOffset, -maxScroll, 0f);
        likedY = panelY + 19.5f * scale;
        allLabelY = panelY + (8f + likedHeight) * scale;
        allY = unscrolledAllY;
    }

    private void updateScroll() {
        scrollAnimation.setTarget(scrollTarget);
        scrollOffset = clamp(scrollAnimation.value(), -maxScroll, 0f);
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
        for (int i = 0; i < accounts.size(); i++) rowAnimations.add(RowAnim.visible(accounts.get(i), i));
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
        rowAnimations.sort((first, second) -> Integer.compare(
                first.removing ? Integer.MAX_VALUE : first.index,
                second.removing ? Integer.MAX_VALUE : second.index));
    }

    private void primeSkins() {
        for (AccountProfile account : accounts) primeSkin(account);
    }

    private void primeSkin(AccountProfile account) {
        if (account == null || client == null) return;
        skins.putIfAbsent(account.uuid(), DefaultSkinHelper.getSkinTextures(account.uuid()).texture());
        try {
            GameProfile profile = new GameProfile(account.uuid(), account.name());
            client.getSkinProvider().fetchSkinTextures(profile).thenAccept(optional -> optional.ifPresent(skin -> {
                if (client != null) client.execute(() -> skins.put(account.uuid(), skin.texture()));
            }));
        } catch (Throwable ignored) {
        }
    }

    private void addAccount() {
        String name = input.trim();
        if (!AccountRepository.isValidName(name) || AccountRepository.containsName(accounts, name)) return;
        AccountProfile profile = AccountProfile.offline(name);
        accounts.add(0, profile);
        rowAnimations.add(RowAnim.appearing(profile, 0));
        updateRowTargets();
        primeSkin(profile);
        scrollTarget = 0f;
        AccountRepository.save(accounts);
        updateInput("");
    }

    private void switchAccount(AccountProfile account) {
        if (account == null) return;
        AccountSessionManager.switchTo(account);
    }

    private void toggleLiked(AccountProfile account) {
        String key = normalize(account.name());
        RowAnim row = findRow(account);
        boolean toLiked = !likedNames.contains(key);
        float sourceX = row != null && row.positionReady ? row.displayX : allX;
        float sourceY = row != null && row.positionReady ? row.displayY : allY;
        if (row != null) {
            if (!row.positionReady && toLiked) {
                int index = allRows().indexOf(row);
                if (index >= 0) {
                    float[] source = allPosition(index);
                    sourceX = source[0];
                    sourceY = source[1];
                }
            } else if (!row.positionReady) {
                int index = likedRows().indexOf(row);
                if (index >= 0) {
                    sourceX = likedX + (index % COLUMNS) * (cardW + columnGap);
                    sourceY = likedY + (index / COLUMNS) * (cardH + rowGap);
                }
            }
        }

        if (toLiked) likedNames.add(key);
        else likedNames.remove(key);
        likedSection.setTarget(likedNames.isEmpty() ? 0f : 1f);
        likedExtraRows.setTarget(Math.max(0, likedRowCount() - 1));
        scrollTarget = 0f;
        if (row != null) row.beginTransfer(sourceX, sourceY, toLiked);
        AccountRepository.saveLikedNames(likedNames);
    }

    private void removeAccount(AccountProfile account) {
        int index = indexOfAccount(account);
        if (index < 0) return;
        boolean removedCurrent = account.name().equalsIgnoreCase(currentName());
        AccountProfile removed = accounts.remove(index);
        likedNames.remove(normalize(removed.name()));
        likedSection.setTarget(likedNames.isEmpty() ? 0f : 1f);
        likedExtraRows.setTarget(Math.max(0, likedRowCount() - 1));
        AccountRepository.saveLikedNames(likedNames);
        RowAnim row = findRow(removed);
        if (row != null) row.remove();
        updateRowTargets();
        AccountRepository.save(accounts);
        if (removedCurrent) {
            if (!accounts.isEmpty()) switchAccount(accounts.get(Math.min(index, accounts.size() - 1)));
            else AccountRepository.saveSelected(null);
        }
    }

    private RowAnim findRow(AccountProfile account) {
        for (RowAnim row : rowAnimations) if (sameAccount(row.account, account)) return row;
        return null;
    }

    private int indexOfAccount(AccountProfile account) {
        for (int i = 0; i < accounts.size(); i++) if (sameAccount(accounts.get(i), account)) return i;
        return -1;
    }

    private static boolean sameAccount(AccountProfile first, AccountProfile second) {
        return first != null && second != null && first.name().equalsIgnoreCase(second.name());
    }

    private String currentName() {
        if (client == null || client.getSession() == null || client.getSession().getUsername() == null) return "Player";
        return client.getSession().getUsername();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        float scale = layoutScale();
        inputActive = inside(mx, my, inputX, inputY, inputW, inputH);
        if (inside(mx, my, addX, inputY, addW, inputH)) {
            addAccount();
            return true;
        }

        List<RowAnim> liked = likedRows();
        for (int i = 0; i < liked.size(); i++) {
            if (isTransferring(liked.get(i))) continue;
            RowAnim row = liked.get(i);
            float y = visibleY(row);
            if (y + cardH < contentTop || y > contentBottom) continue;
            if (handleCardClick(row, mx, my, row.displayX, y)) return true;
        }
        List<RowAnim> all = allRows();
        for (int index = 0; index < all.size(); index++) {
            RowAnim row = all.get(index);
            if (isTransferring(row)) continue;
            float y = visibleY(row);
            if (y + cardH < contentTop || y > contentBottom) continue;
            if (handleCardClick(row, mx, my, row.displayX, y)) return true;
        }
        return true;
    }

    private boolean handleCardClick(RowAnim row, float mouseX, float mouseY, float x, float y) {
        if (!inside(mouseX, mouseY, x, y, cardW, cardH)) return false;
        float scale = layoutScale();
        if (mouseX >= x + cardW - 7.2f * scale) removeAccount(row.account);
        else if (mouseX >= x + cardW - 14f * scale) toggleLiked(row.account);
        else switchAccount(row.account);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float mx = ScreenScaleUtil.screenToVirtual(mouseX);
        float my = ScreenScaleUtil.screenToVirtual(mouseY);
        if (inside(mx, my, panelX + 8f * layoutScale(), contentTop,
                panelW - 16f * layoutScale(), allClipH) && maxScroll > 0.5f) {
            scrollTarget += (float) (verticalAmount * (cardH + rowGap) * 0.82f);
            scrollTarget = clamp(scrollTarget, -maxScroll, 0f);
            scrollAnimation.setTarget(scrollTarget);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (!inputActive) return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (closing || !inputActive) return super.charTyped(chr, modifiers);
        if (input.length() >= 16) return true;
        if (Character.isLetterOrDigit(chr) || chr == '_') {
            updateInput(input + chr);
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

    private void drawAnimatedRemovedChar(DrawContext context, float x, float y, float size, float alpha) {
        if (removedChar.isEmpty() || removedCharStartMs <= 0L) return;
        float progress = clamp((System.currentTimeMillis() - removedCharStartMs) / 180f, 0f, 1f);
        if (progress >= 0.999f) {
            removedChar = "";
            removedCharStartMs = 0L;
            return;
        }
        float symbolW = CustomFontRenderer.getTextWidth(removedChar, size, FontWeight.SEMIBOLD);
        float symbolX = x + CustomFontRenderer.getTextWidth(input, size, FontWeight.SEMIBOLD);
        float shrink = 1f - 0.72f * RenderUtil.easeInQuad(progress);
        context.getMatrices().push();
        context.getMatrices().translate(symbolX + symbolW * 0.5f, y + size * 0.5f, 0f);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * progress));
        context.getMatrices().scale(shrink, shrink, 1f);
        CustomFontRenderer.drawText(context, removedChar, -symbolW * 0.5f, -size * 0.5f, size,
                RenderUtil.withAlpha(TEXT, alpha * (1f - progress)), FontWeight.SEMIBOLD);
        context.getMatrices().pop();
    }

    private static String formatDate(long createdAt) {
        try {
            return DATE.format(Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()));
        } catch (Throwable ignored) {
            return "--.--.--";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        inputActive = false;
        addHover.setTarget(0f);
        openAnimation.duration(CLOSE_MS).easing(Easing.IN_OUT_CUBIC).playTo(0f);
    }

    private void returnToParent() {
        if (client == null) return;
        if (parent instanceof CustomMainMenuScreen menu) menu.resumeFromAccountManager();
        else client.setScreen(parent);
    }

    private static void drawCenteredIcon(DrawContext context, String icon,
                                         float centerX, float centerY, float size, int color) {
        float y = centerY - CustomFontRenderer.getTextHeight(size, FontWeight.SOLAR_ICONS) * 0.5f;
        CustomFontRenderer.drawCenteredText(context, icon, centerX, y, size, color, FontWeight.SOLAR_ICONS);
    }

    private static final class RowAnim {
        private final AccountProfile account;
        private final AnimatedFloat hover = new AnimatedFloat(0f, 150L, Easing.OUT_CUBIC);
        private final AnimatedFloat favoriteHover = new AnimatedFloat(0f, 145L, Easing.OUT_CUBIC);
        private final AnimatedFloat deleteHover = new AnimatedFloat(0f, 145L, Easing.OUT_CUBIC);
        private final AnimatedFloat selected = new AnimatedFloat(0f, 180L, Easing.OUT_CUBIC);
        private final AnimatedFloat alpha;
        private final AnimatedFloat spawn;
        private float displayX;
        private float displayY;
        private float transferFromX;
        private float transferFromY;
        private long transferStartedAt;
        private boolean positionReady;
        private boolean transferring;
        private boolean transferToLiked;
        private int index;
        private boolean removing;

        private RowAnim(AccountProfile account, int index, float alpha) {
            this.account = account;
            this.index = index;
            this.alpha = new AnimatedFloat(alpha, 360L, Easing.OUT_QUART);
            this.spawn = new AnimatedFloat(alpha, 520L, Easing.OUT_QUART);
        }

        private static RowAnim visible(AccountProfile account, int index) {
            return new RowAnim(account, index, 1f);
        }

        private static RowAnim appearing(AccountProfile account, int index) {
            RowAnim row = new RowAnim(account, index, 0f);
            row.alpha.setTarget(1f);
            row.spawn.setTarget(1f);
            return row;
        }

        private void showAt(int targetIndex) {
            removing = false;
            index = targetIndex;
            alpha.setTarget(1f);
        }

        private void remove() {
            removing = true;
            transferring = false;
            hover.setTarget(0f);
            favoriteHover.setTarget(0f);
            deleteHover.setTarget(0f);
            selected.setTarget(0f);
            alpha.setTarget(0f);
        }

        private boolean isGone() {
            return removing && alpha.value() <= 0.025f;
        }

        private void beginTransfer(float fromX, float fromY, boolean toLiked) {
            displayX = fromX;
            displayY = fromY;
            transferFromX = fromX;
            transferFromY = fromY;
            transferToLiked = toLiked;
            transferStartedAt = System.currentTimeMillis();
            positionReady = true;
            transferring = true;
        }

        private float transferProgress() {
            return Easing.clamp((System.currentTimeMillis() - transferStartedAt)
                    / (float) FAVORITE_TRANSFER_MS);
        }
    }
}
