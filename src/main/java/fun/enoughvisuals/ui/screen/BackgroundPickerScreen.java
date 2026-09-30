package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundManager.Entry;
import fun.enoughvisuals.ui.mainmenu.MenuBackgroundPreviewCache;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class BackgroundPickerScreen extends Screen {
    private static final int PANEL = 0xFF151519;
    private static final int ACCENT = 0xFF7F94DD;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFA7A2AE;
    private static final float FOOTER_HEIGHT = 34f;
    private static final long OPEN_MS = 440L;
    private static final long CLOSE_MS = 320L;
    private static final long CARD_APPEAR_MS = 260L;

    private final CustomMainMenuScreen parent;
    private final Map<Entry, AnimatedFloat> hover = new IdentityHashMap<>();
    private final AnimatedFloat closeHover = new AnimatedFloat(0f, 210L, Easing.OUT_QUART);
    private float targetScroll;
    private float scrollPosition;
    private long lastScrollFrameNanos;
    private boolean closing;
    private long openedAt;
    private long closeStartedAt;
    private float closeFrom = 1f;

    public BackgroundPickerScreen(CustomMainMenuScreen parent) {
        super(Text.literal(ClientLanguage.text("ui.background.title", "Выбор фона")));
        this.parent = parent;
        for (Entry entry : MenuBackgroundManager.ENTRIES) {
            hover.put(entry, new AnimatedFloat(0f, 280L, Easing.OUT_QUART));
        }
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        closeStartedAt = 0L;
        closeFrom = 1f;
        closing = false;
        scrollPosition = targetScroll;
        lastScrollFrameNanos = System.nanoTime();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        parent.renderBackgroundPickerBackdrop(ctx);
        float reveal = reveal();
        if (closing && System.currentTimeMillis() - closeStartedAt >= CLOSE_MS) {
            parent.resumeFromBackgroundPicker();
            return;
        }
        RenderUtil.drawRect(ctx, 0, 0, width, height, RenderUtil.withAlpha(0xFF000000, reveal * 0.58f));

        Layout base = layout();
        Layout l = base.offsetY((1f - reveal) * 11f);
        float scroll = updateScrollPosition();
        float panelAlpha = reveal;

        float previousExponent = RenderUtil.pushSquircleExponent(3.4f);
        try {
            RenderUtil.drawRoundedRect(ctx, l.x + 1.5f, l.y + 5f, l.w, l.h, 18f,
                    RenderUtil.withAlpha(0xFF000000, panelAlpha * 0.48f));
            RenderUtil.drawRoundedRect(ctx, l.x, l.y, l.w, l.h, 18f,
                    RenderUtil.withAlpha(PANEL, panelAlpha));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }
        renderHeader(ctx, l, mouseX, mouseY, panelAlpha);

        ctx.draw();
        ctx.enableScissor(Math.round(l.listX), Math.round(l.listY),
                Math.round(l.listX + l.listW), Math.round(l.listY + l.listH));
        List<Entry> entries = MenuBackgroundManager.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int column = i % l.columns;
            int row = i / l.columns;
            float cardX = l.listX + column * (l.cardW + l.gap);
            float cardY = l.listY + row * (l.cardH + l.gap) - scroll;
            if (cardY + l.cardH < l.listY - 4 || cardY > l.listY + l.listH + 4) continue;

            boolean isHovered = !closing && mouseX >= cardX && mouseX <= cardX + l.cardW
                    && mouseY >= Math.max(cardY, l.listY)
                    && mouseY <= Math.min(cardY + l.cardH, l.listY + l.listH);
            hover.get(entry).setTarget(isHovered ? 1f : 0f);
            MenuBackgroundPreviewCache.request(client, entry);

            float stagger = cardReveal(i, reveal);
            renderCard(ctx, entry, i, cardX, cardY, l.cardW, l.cardH,
                    hover.get(entry).value(), stagger * panelAlpha);
        }
        renderFooter(ctx, l, scroll, panelAlpha);
        ctx.draw();
        ctx.disableScissor();
        renderScrollbar(ctx, l, scroll, panelAlpha);
    }

    private void renderHeader(DrawContext ctx, Layout l, int mouseX, int mouseY, float alpha) {
        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.background.gallery", "Галерея фонов"), l.x + 16, l.y + 12, 9.4f,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.background.subtitle", "Выберите фон, который вам по душе"),
                l.x + 16, l.y + 25, 7.2f, RenderUtil.withAlpha(MUTED, alpha), FontWeight.SEMIBOLD);

        float bx = l.x + l.w - 29;
        float by = l.y + 11.5f;
        float size = 17f;
        boolean over = !closing && inside(mouseX, mouseY, bx, by, size, size);
        closeHover.setTarget(over ? 1f : 0f);
        float hoverValue = closeHover.value();
        float grow = 1f + hoverValue * 0.045f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(bx + size * 0.5f, by + size * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(bx + size * 0.5f), -(by + size * 0.5f), 0f);
        RenderUtil.drawRoundedRect(ctx, bx, by, size, size, size * 0.5f,
                RenderUtil.withAlpha(RenderUtil.lerpColor(0xFF222126, 0xFF302E36, hoverValue), alpha));
        float iconSize = 7.5f;
        float iconY = by + (size - CustomFontRenderer.getTextHeight(iconSize, FontWeight.SOLAR_ICONS)) * 0.5f;
        CustomFontRenderer.drawCenteredText(ctx, SolarIcons.CLOSE, bx + size * 0.5f, iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha * (0.82f + hoverValue * 0.18f)), FontWeight.SOLAR_ICONS);
        ctx.getMatrices().pop();
    }

    private void renderCard(DrawContext ctx, Entry entry, int index, float x, float y, float w, float h,
                            float hoverValue, float alpha) {
        if (alpha <= 0.01f) return;
        float lift = hoverValue * 1.6f;
        y -= lift;
        float border = 0f;

        RenderUtil.drawRoundedRect(ctx, x, y + 2.2f, w, h, 8.5f,
                RenderUtil.withAlpha(0xFF000000, alpha * (0.22f + hoverValue * 0.12f)));
        float ix = x + border;
        float iy = y + border;
        float iw = w - border * 2f;
        float ih = h - border * 2f;

        Identifier preview = MenuBackgroundPreviewCache.texture(entry);
        if (preview != null) {
            int tw = entry == MenuBackgroundManager.DEFAULT ? 1920 : MenuBackgroundPreviewCache.WIDTH;
            int th = entry == MenuBackgroundManager.DEFAULT ? 1080 : MenuBackgroundPreviewCache.HEIGHT;
            drawCover(ctx, preview, ix, iy, iw, ih, tw, th, alpha);
        } else {
            renderSkeleton(ctx, ix, iy, iw, ih, index, alpha);
        }

        String status = null;
        if (MenuBackgroundManager.pending() == entry) status = Math.round(MenuBackgroundManager.progress() * 100f) + "%";
        else if (MenuBackgroundManager.selected() == entry) status = ClientLanguage.text("ui.background.active", "Активен");

        if (status != null) {
            float chipW = CustomFontRenderer.getTextWidth(status, 6.7f, FontWeight.SEMIBOLD) + 12f;
            RenderUtil.drawRoundedRect(ctx, x + w - chipW - 7f, y + 7f, chipW, 14f, 7f,
                    RenderUtil.withAlpha(MenuBackgroundManager.selected() == entry ? ACCENT : 0xC216151B, alpha));
            CustomFontRenderer.drawCenteredText(ctx, status, x + w - chipW * 0.5f - 7f, y + 10.6f,
                    6.7f, RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        }

        float titleY = y + h - 13.5f - hoverValue * 5.4f;
        CustomFontRenderer.drawText(ctx, entry.displayTitle(), x + 9f, titleY, 7.2f,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);
        if (hoverValue > 0.01f) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.background.choose", "Нажмите, чтобы выбрать"), x + 9f, y + h - 10.5f, 6.8f,
                    RenderUtil.withAlpha(0xFFD3CDD8, alpha * hoverValue), FontWeight.SEMIBOLD);
        }
    }

    private void drawCover(DrawContext ctx, Identifier texture, float x, float y, float w, float h,
                           int tw, int th, float alpha) {
        float targetAspect = w / h;
        float textureAspect = tw / (float) th;
        float u = 0, v = 0, uw = tw, vh = th;
        if (targetAspect > textureAspect) {
            vh = tw / targetAspect;
            v = (th - vh) * 0.5f;
        } else {
            uw = th * targetAspect;
            u = (tw - uw) * 0.5f;
        }
        RenderUtil.drawMenuPreviewShader(ctx, texture, x, y, w, h, 6.8f,
                alpha, 0.88f, u, v, uw, vh, tw, th);
    }

    private void renderFooter(DrawContext ctx, Layout l, float scroll, float alpha) {
        int rows = (MenuBackgroundManager.ENTRIES.size() + l.columns - 1) / l.columns;
        float y = l.listY + rows * (l.cardH + l.gap) - scroll + 9f;
        if (y > l.listY + l.listH || y + 20f < l.listY) return;
        RenderUtil.drawRoundedRect(ctx, l.listX + l.listW * 0.5f - 18f, y, 36f, 1f, 0.5f,
                RenderUtil.withAlpha(0xFF514C59, alpha * 0.62f));
        CustomFontRenderer.drawCenteredText(ctx, ClientLanguage.text("ui.background.source", "Фоны взяты из t.me/BeatifulMinecraft"),
                l.listX + l.listW * 0.5f, y + 9f, 6.8f,
                RenderUtil.withAlpha(MUTED, alpha), FontWeight.SEMIBOLD);
    }

    private void renderSkeleton(DrawContext ctx, float x, float y, float w, float h, int index, float alpha) {
        int[] base = {0xFF302B3A, 0xFF263541, 0xFF3B302C, 0xFF293935, 0xFF3B2931, 0xFF2C2E3E};
        int color = base[Math.floorMod(index, base.length)];
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 7f, RenderUtil.withAlpha(color, alpha));
    }

    private void renderScrollbar(DrawContext ctx, Layout l, float scroll, float alpha) {
        float max = maxScroll(l);
        if (max <= 0.5f) return;
        float trackX = l.x + l.w - 6.5f;
        float thumbH = Math.max(24f, l.listH * l.listH / (l.listH + max));
        float thumbY = l.listY + (l.listH - thumbH) * (scroll / max);
        int accent = CustomTheme.guiAccent();
        RenderUtil.drawRoundedRect(ctx, trackX, l.listY, 1.5f, l.listH, 0.75f,
                RenderUtil.withAlpha(accent, alpha * 0.18f));
        RenderUtil.drawRoundedRect(ctx, trackX, thumbY, 1.5f, thumbH, 0.75f,
                RenderUtil.withAlpha(accent, alpha * 0.86f));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || closing) return true;
        Layout l = layout().offsetY((1f - reveal()) * 11f);
        if (inside(mouseX, mouseY, l.x + l.w - 29, l.y + 11.5f, 17, 17)) {
            close();
            return true;
        }
        if (!inside(mouseX, mouseY, l.x, l.y, l.w, l.h)) {
            close();
            return true;
        }
        if (mouseY < l.listY || mouseY > l.listY + l.listH || MenuBackgroundManager.loading()) return true;
        float scroll = scrollPosition;
        List<Entry> entries = MenuBackgroundManager.ENTRIES;
        for (int i = 0; i < entries.size(); i++) {
            float x = l.listX + (i % l.columns) * (l.cardW + l.gap);
            float y = l.listY + (i / l.columns) * (l.cardH + l.gap) - scroll;
            if (inside(mouseX, mouseY, x, y, l.cardW, l.cardH)) {
                EnoughSoundPlayer.playScroll();
                MenuBackgroundManager.select(client, entries.get(i));
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout();
        targetScroll = MathHelper.clamp(targetScroll - (float) verticalAmount * 44f, 0, maxScroll(l));
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
    public boolean shouldPause() { return false; }

    private float reveal() {
        long now = System.currentTimeMillis();
        if (closing) {
            float t = MathHelper.clamp((now - closeStartedAt) / (float) CLOSE_MS, 0f, 1f);
            return closeFrom * (1f - Easing.IN_OUT_CUBIC.apply(t));
        }
        float t = MathHelper.clamp((now - openedAt) / (float) OPEN_MS, 0f, 1f);
        return Easing.IN_OUT_CUBIC.apply(t);
    }

    private float cardReveal(int index, float panelReveal) {
        if (closing) return panelReveal;
        long delay = Math.min(index, 8) * 12L;
        float t = MathHelper.clamp((System.currentTimeMillis() - openedAt - delay) / (float) CARD_APPEAR_MS, 0f, 1f);
        return Easing.OUT_CUBIC.apply(t) * panelReveal;
    }

    private float updateScrollPosition() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, Math.max(0f, (now - lastScrollFrameNanos) / 1_000_000_000f));
        lastScrollFrameNanos = now;
        float blend = 1f - (float) Math.exp(-14f * dt);
        scrollPosition += (targetScroll - scrollPosition) * blend;
        if (Math.abs(targetScroll - scrollPosition) < 0.01f) scrollPosition = targetScroll;
        return scrollPosition;
    }

    private float maxScroll(Layout l) {
        int rows = (MenuBackgroundManager.ENTRIES.size() + l.columns - 1) / l.columns;
        float content = rows * (l.cardH + l.gap) - l.gap + FOOTER_HEIGHT;
        return Math.max(0, content - l.listH);
    }

    private Layout layout() {
        float w = Math.min(520, width - 24);
        float h = Math.min(348, height - 24);
        float x = (width - w) * 0.5f;
        float y = (height - h) * 0.5f;
        int columns = width < 380 ? 2 : 3;
        float gap = 8f;
        float listX = x + 14f;
        float listY = y + 48f;
        float listW = w - 28f;
        float listH = h - 60f;
        float cardW = (listW - gap * (columns - 1)) / columns;
        float cardH = Math.max(76f, Math.min(92f, cardW * 0.61f));
        return new Layout(x, y, w, h, listX, listY, listW, listH, cardW, cardH, gap, columns);
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private record Layout(float x, float y, float w, float h, float listX, float listY,
                          float listW, float listH, float cardW, float cardH, float gap, int columns) {
        Layout offsetY(float offset) {
            return new Layout(x, y + offset, w, h, listX, listY + offset,
                    listW, listH, cardW, cardH, gap, columns);
        }
    }
}
