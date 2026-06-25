package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.modules.visual.CustomTab;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.IslandFeed;
import fun.enoughvisuals.systems.modules.modules.utils.MineRadar;
import fun.enoughvisuals.utils.system.ModuleToggleFeed;
import fun.enoughvisuals.utils.system.NowPlayingService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WatermarkHud extends HudElement {

    private static WatermarkHud INSTANCE;

    public final BooleanSetting showEvents = addSetting(new BooleanSetting("Ивенты", "show_events", true));
    public final BooleanSetting showFps    = addSetting(new BooleanSetting("FPS", "show_fps", true));
    public final BooleanSetting showPing   = addSetting(new BooleanSetting("Пинг", "show_ping", true));
    public final BooleanSetting showMusic  = addSetting(new BooleanSetting("Музыка", "show_music", true));
    public final BooleanSetting animateNumbers = addSetting(new BooleanSetting("Анимация чисел", "animate_numbers", true));
    public final BooleanSetting customTab = addSetting(new BooleanSetting("Кастом TAB", "custom_tab", true));
    public final SliderSetting islandSize = addSetting(new SliderSetting("Размер острова", "island_size", 1.0, 0.75, 1.55, 0.05));

    private static final Identifier MUSIC_FALLBACK = Identifier.of("enough", "logo/icon.png");
    private static final String LOGO_ICON = "\uF002";

    private static final float H = 17.5f; // Уменьшена высота
    private static final float SECOND_H = 12f;
    private static final float SECOND_GAP = 3f;
    private static final float LEFT_PAD = 6f;
    private static final float RIGHT_PAD = 6f;
    private static final float TRACK_MAX_W = 110f;
    private static final int BAR_COUNT = 6;
    private static final float BAR_W = 0.9f; // Более тонкие палочки
    private static final float BAR_GAP = 2.0f; // Слегка уменьшено расстояние
    private static final float BAR_MAX_H = 3.2f; // Уменьшена амплитуда
    private static final float BAR_MIN_H = 0.6f;
    private static final float BAR_RADIUS = 0.9f;
    private static final double[] BAR_FREQ = { 4.2, 6.1, 3.5, 5.4, 2.8, 7.0 };
    private static final float[]  BAR_PHASE = { 0.0f, 1.26f, 2.51f, 3.77f, 5.03f, 6.2f };
    private final float[] barHeights = new float[BAR_COUNT];
    private final float[] barTargets = new float[BAR_COUNT];

    private String songText = "";
    private boolean hasTrack = false;
    private boolean trackPlaying = false;
    private boolean toggleActive = false;
    private boolean toggleEnabled = false;
    private boolean islandMessageActive = false;
    private boolean mineTierActive = false;
    private String toggleText = "";
    private String mineTierText = "";

    private int targetFps = 0;
    private int targetPing = 0;
    private float rollFps = 0f;
    private float rollPing = 0f;
    private float fpsWidthAnim = 0f;
    private float pingWidthAnim = 0f;
    private float watermarkContentAlpha = 1f;

    private long lastFpsTickMs = 0L;
    private long lastPingTickMs = 0L;
    private long animStartMs = 0L;
    private String lastTrackText = "";
    private Identifier artworkId = null;
    private int artworkHash = 0;
    private static int artworkCounter = 0;

    private enum BlockType { FPS, PING, EVENTS, LOGO, TOGGLE }
    private static class RenderBlock {
        BlockType type; float width;
        RenderBlock(BlockType type, float width) { this.type = type; this.width = width; }
    }
    private final List<RenderBlock> currentBlocks = new ArrayList<>();

    public WatermarkHud(float x, float y) {
        super(x, y, 160f, H);
        INSTANCE = this;
        Arrays.fill(barHeights, BAR_MIN_H);
        Arrays.fill(barTargets, BAR_MIN_H);
    }

    public static boolean customTabEnabled() {
        return INSTANCE != null && INSTANCE.customTab.get();
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;

        long now = System.currentTimeMillis();

        if (now - lastFpsTickMs > 100L) {
            targetFps = Math.max(0, client.getCurrentFps());
            if (rollFps == 0) rollFps = targetFps;
            lastFpsTickMs = now;
        }

        if (now - lastPingTickMs > 500L) {
            if (client.getNetworkHandler() != null && client.player != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) targetPing = entry.getLatency();
            }
            if (rollPing == 0) rollPing = targetPing;
            lastPingTickMs = now;
        }

        if (animateNumbers.get()) {
            rollFps = updateRoll(rollFps, targetFps);
            rollPing = updateRoll(rollPing, targetPing);
        } else {
            rollFps = targetFps;
            rollPing = targetPing;
        }

        NowPlayingService.TrackInfo trackInfo = NowPlayingService.getNowPlaying();
        boolean newHasTrack = showMusic.get() && trackInfo.active();
        String newText = newHasTrack ? extractTitle(trackInfo.text()) : "";
        if (!newText.equals(lastTrackText)) { animStartMs = System.currentTimeMillis(); lastTrackText = newText; }
        hasTrack = newHasTrack; songText = newText;
        boolean wasPlaying = trackPlaying;
        trackPlaying = hasTrack && trackInfo.isPlaying();
        if (trackPlaying && !wasPlaying) animStartMs = System.currentTimeMillis();
        if (hasTrack && trackInfo.hasArtwork()) updateArtwork(trackInfo.artworkPng());

        String mineText = MineRadar.getMineText();
        mineTierActive = mineText != null && !mineText.isBlank();
        mineTierText = mineTierActive ? mineText : "";

        IslandFeed.Entry islandEntry = IslandFeed.getActive();
        if (islandEntry != null) {
            islandMessageActive = true; toggleActive = true; toggleEnabled = true; toggleText = islandEntry.text();
        } else {
            islandMessageActive = false;
            ModuleToggleFeed.ToggleEvent event = ModuleToggleFeed.getActive();
            if (event != null) { toggleActive = true; toggleEnabled = event.enabled(); toggleText = event.formattedText(); }
            else { toggleActive = false; toggleText = ""; }
        }

        updateBarTargets();
        float frameDelta = RenderUtil.getFrameDelta();
        float speed = trackPlaying ? 0.38f : 0.08f;
        for (int i = 0; i < BAR_COUNT; i++) barHeights[i] = RenderUtil.animate(barHeights[i], barTargets[i], speed, frameDelta);

        currentBlocks.clear();
        boolean isEvent = islandMessageActive && showEvents.get();
        boolean isToggle = !islandMessageActive && toggleActive;
        // Скрываем FPS и Пинг, если есть ивент, тоггл ИЛИ играет трек
        boolean hideStats = isEvent || isToggle || hasTrack;

        if (isEvent) {
            float iconSize = 7.0f;
            float textW = CustomFontRenderer.getTextWidth(toggleText, 6.0f, FontWeight.SEMIBOLD);
            currentBlocks.add(new RenderBlock(BlockType.EVENTS, iconSize + 7.5f + textW));
        } else if (isToggle) {
            float dotSize = 4.0f;
            float textW = CustomFontRenderer.getTextWidth(toggleText, 6.0f, FontWeight.SEMIBOLD);
            currentBlocks.add(new RenderBlock(BlockType.TOGGLE, dotSize + 4.0f + textW));
        } else {
            currentBlocks.add(getLogoBlock());
        }

        if (!hideStats) {
            for (Setting<?> s : getSettings()) {
                if (s == showFps && showFps.get()) {
                    float targetW = CustomFontRenderer.getTextWidth(targetFps + " fps", 6.0f, FontWeight.SEMIBOLD);
                    fpsWidthAnim = fpsWidthAnim == 0 ? targetW : RenderUtil.animate(fpsWidthAnim, targetW, 0.22f, frameDelta);
                    currentBlocks.add(new RenderBlock(BlockType.FPS, fpsWidthAnim));
                } else if (s == showPing && showPing.get()) {
                    float targetW = CustomFontRenderer.getTextWidth(targetPing + "ms", 6.0f, FontWeight.SEMIBOLD);
                    pingWidthAnim = pingWidthAnim == 0 ? targetW : RenderUtil.animate(pingWidthAnim, targetW, 0.22f, frameDelta);
                    currentBlocks.add(new RenderBlock(BlockType.PING, pingWidthAnim));
                }
            }
        }

        float uiScale = islandScale();
        float targetW = LEFT_PAD;
        RenderBlock prev = null;
        for (RenderBlock b : currentBlocks) {
            if (prev != null) {
                targetW += CustomFontRenderer.getTextWidth("   /   ", 4.0f, FontWeight.SEMIBOLD);
            }
            targetW += b.width;
            prev = b;
        }
        targetW += RIGHT_PAD;
        if (currentBlocks.isEmpty()) targetW = 0f;

        float maxScreenW = HudElement.hudScreenWidth() - 12f;
        float maxUnscaledW = maxScreenW / uiScale;
        targetW = clamp(targetW, 40f, maxUnscaledW);

        float targetH = H + (mineTierActive ? (SECOND_GAP + SECOND_H) : 0f);
        float islandTargetW = targetW * uiScale;
        float islandTargetH = targetH * uiScale;
        float tabMorph = CustomTab.getAnimationProgress();
        float tabEased = RenderUtil.easeInOutCubic(tabMorph);
        watermarkContentAlpha = RenderUtil.animate(watermarkContentAlpha, 1f - tabEased,
                tabMorph > 0.001f ? 0.30f : 0.26f, frameDelta);
        if (tabMorph > 0.001f) {
            float[] tabSize = CustomTab.getPreferredSize();
            if (tabSize != null) {
                islandTargetW = lerp(islandTargetW, clamp(tabSize[0], 40f, maxScreenW), tabMorph);
                islandTargetH = lerp(islandTargetH, tabSize[1], tabMorph);
            }
        }

        float morphSpeed = CustomTab.getAnimationProgress() > 0.001f ? 0.38f : 0.22f;
        float targetCenterX = HudElement.hudScreenWidth() * 0.5f;
        float nextW = RenderUtil.animate(getWidth(), islandTargetW, morphSpeed, frameDelta);
        float nextH = RenderUtil.animate(getHeight(), islandTargetH, morphSpeed, frameDelta);
        setWidth(nextW);
        setHeight(nextH);
        setX(targetCenterX - nextW * 0.5f);
        setY(RenderUtil.animate(getY(), 6f, morphSpeed, frameDelta));
    }

    private float updateRoll(float current, float target) {
        float diff = target - current;
        if (Math.abs(diff) > 3.0f) {
            current = target - Math.signum(diff) * 3.0f;
            diff = target - current;
        }
        float step = diff * Math.min(1f, 0.15f * RenderUtil.getFrameDelta());
        step = clamp(step, -0.2f, 0.2f);
        return current + step;
    }

    private RenderBlock getLogoBlock() {
        float w;
        if (hasTrack) {
            float wavesWidth = (BAR_COUNT - 1) * BAR_GAP + BAR_W;
            w = 10.5f + 3.5f + clamp(CustomFontRenderer.getTextWidth(songText, 5.9f, FontWeight.REGULAR), 18f, TRACK_MAX_W) + 5f + wavesWidth;
        } else {
            w = 14.2f + 3.2f + CustomFontRenderer.getTextWidth("enoughvisuals.fun", 6.0f, FontWeight.SEMIBOLD);
        }
        return new RenderBlock(BlockType.LOGO, w);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        float tabMorph = CustomTab.getAnimationProgress();
        float tabEased = RenderUtil.easeInOutCubic(tabMorph);
        float tabAlpha = alpha * tabEased;
        float contentAlpha = alpha * watermarkContentAlpha;
        float s = islandScale();
        float topH = H * s;

        boolean tabVisible = tabMorph > 0.001f;
        if (tabVisible || (!currentBlocks.isEmpty() && contentAlpha > 0.01f)) {
            float bgH = tabVisible ? h : topH;
            float radius = tabVisible ? Math.min(11f * s, Math.max(topH * 0.5f, bgH * 0.18f)) : topH * 0.5f;
            drawMenuStyleBackground(ctx, x, y, w, bgH, radius, alpha);
        }

        if (tabAlpha > 0.01f) {
            RenderUtil.enableScissor(x, y, w, h);
            try {
                CustomTab.renderInPanel(ctx, x, y, w, h, tabAlpha);
            } finally {
                RenderUtil.disableScissor();
            }
        }

        if (currentBlocks.isEmpty() || contentAlpha <= 0.01f) return;

        RenderUtil.enableScissor(x, y, w, tabVisible ? h : topH);
        try {
        float totalContentW = 0f;
        RenderBlock prevW = null;
        for (RenderBlock b : currentBlocks) {
            if (prevW != null) {
                totalContentW += CustomFontRenderer.getTextWidth("   /   ", 4.0f * s, FontWeight.SEMIBOLD);
            }
            totalContentW += b.width * s;
            prevW = b;
        }

        float cursor = x + (w - totalContentW) * 0.5f - 2f * s;
        RenderBlock prev = null;

        // Координаты текста сдвинуты на 1 пиксель вверх (например, y + 6.5f -> y + 5.5f) из-за уменьшенной высоты
        for (RenderBlock b : currentBlocks) {
            if (prev != null) {
                String sep = "   /   ";
                CustomFontRenderer.drawText(ctx, sep, cursor, y + 6.5f * s, 4.0f * s,
                        RenderUtil.withAlpha(0xFF888888, contentAlpha * 0.95f), FontWeight.SEMIBOLD);
                cursor += CustomFontRenderer.getTextWidth(sep, 4.0f * s, FontWeight.SEMIBOLD);
            }

            switch (b.type) {
                case FPS -> drawSlotText(ctx, rollFps, " fps", cursor, y, b.width * s, topH, contentAlpha, s);
                case PING -> drawSlotText(ctx, rollPing, "ms", cursor, y, b.width * s, topH, contentAlpha, s);

                case EVENTS -> {
                    int iconColor = 0xFF4CFF88;
                    CustomFontRenderer.drawText(ctx, "\uF000", cursor, y + 5.5f * s, 7.0f * s, RenderUtil.withAlpha(iconColor, contentAlpha * 0.95f), FontWeight.EVENT);
                    CustomFontRenderer.drawText(ctx, toggleText, cursor + (7.0f + 7.5f) * s, y + 5.5f * s, 6.0f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, contentAlpha * 0.98f), FontWeight.SEMIBOLD);
                }

                case TOGGLE -> {
                    int dotColor = toggleEnabled ? 0xFF4CFF88 : 0xFFFF5C5C;
                    float dotRadius = 2.0f * s;
                    RenderUtil.drawCircle(ctx, cursor + dotRadius, y + topH * 0.5f, dotRadius, RenderUtil.withAlpha(dotColor, contentAlpha * 0.95f));
                    CustomFontRenderer.drawText(ctx, toggleText, cursor + dotRadius * 2.0f + 4.0f * s, y + 5.5f * s, 6.0f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, contentAlpha * 0.98f), FontWeight.SEMIBOLD);
                }

                case LOGO -> {
                    if (hasTrack) {
                        float avatarSize = 10.5f * s;
                        Identifier avatar = artworkId != null ? artworkId : MUSIC_FALLBACK;
                        RenderUtil.drawRoundedTextureShader(ctx, avatar, cursor, y + (topH - avatarSize) * 0.5f, avatarSize, avatarSize, 5.3f * s, contentAlpha * 0.98f, 0, 0, 1, 1, 1, 1);
                        float curTrack = cursor + avatarSize + 3.5f * s;
                        float targetTrackW = clamp(CustomFontRenderer.getTextWidth(songText, 5.9f * s, FontWeight.REGULAR), 18f * s, TRACK_MAX_W * s);
                        drawTrackMarquee(ctx, songText, curTrack, y + 5f * s, 5.9f * s, targetTrackW, contentAlpha);
                        drawWaves(ctx, curTrack + targetTrackW + 5f * s, y + topH * 0.5f, contentAlpha, s);
                    } else {
                        float logoSize = 18.2f * s;
                        float logoHeight = CustomFontRenderer.getTextHeight(logoSize, FontWeight.LOGO);
                        float logoY = y + (topH - logoHeight) * 0.5f + 0.5f * s;
                        int logoColor = 0xFFC88DFF;
                        CustomFontRenderer.drawText(ctx, LOGO_ICON, cursor, logoY, logoSize,
                                RenderUtil.withAlpha(logoColor, contentAlpha * 0.98f), FontWeight.LOGO);
                        CustomFontRenderer.drawText(ctx, "enoughvisuals.fun", cursor + logoSize, y + 4.87f * s, 6.0f * s, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, contentAlpha * 0.98f), FontWeight.SEMIBOLD);
                    }
                }
            }
            cursor += b.width * s;
            prev = b;
        }

        if (mineTierActive) {
            float tierY = y + topH + SECOND_GAP * s;
            float textW = CustomFontRenderer.getTextWidth(mineTierText, 5.8f * s, FontWeight.SEMIBOLD);
            float contentW = (4.8f + 4f) * s + textW;
            float tierW = clamp(contentW + 12f * s, 72f * s, Math.max(72f * s, w - 18f * s));
            float tierX = x + (w - tierW) * 0.5f;
            MineTierStyle style = resolveMineTierStyle(mineTierText);

            drawMenuStyleBackground(ctx, tierX, tierY, tierW, SECOND_H * s, SECOND_H * 0.45f * s, alpha * 0.98f);
            float drawX = tierX + Math.max(6f * s, (tierW - contentW) * 0.5f);
            RenderUtil.drawRoundedGradientH(ctx, drawX, tierY + SECOND_H * s * 0.5f - 2.2f * s, 4.8f * s, 4.8f * s, 2.4f * s, RenderUtil.withAlpha(style.from(), alpha * 0.95f), RenderUtil.withAlpha(style.to(), alpha * 0.95f));
            drawGradientText(ctx, mineTierText, drawX + 8.8f * s, tierY + 3.0f * s, 5.8f * s, style.from(), style.to(), alpha * 0.98f, FontWeight.SEMIBOLD);
        }
        } finally {
            RenderUtil.disableScissor();
        }
    }

    private void drawSlotText(DrawContext ctx, float value, String suffix, float x, float y, float w, float topH, float alpha, float scale) {
        float fraction = value % 1.0f;
        if (fraction < 0) fraction += 1.0f;

        int val1 = (int) Math.floor(value);
        int val2 = val1 + 1;

        if (!animateNumbers.get() || fraction < 0.01f) {
            String text = val1 + suffix;
            CustomFontRenderer.drawText(ctx, text, x, y + 5.5f * scale, 6.0f * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.98f), FontWeight.SEMIBOLD);
            return;
        }

        String str1 = String.valueOf(val1);
        String str2 = String.valueOf(val2);
        int maxLen = Math.max(str1.length(), str2.length());

        StringBuilder pad1 = new StringBuilder(str1);
        while (pad1.length() < maxLen) pad1.insert(0, " ");
        StringBuilder pad2 = new StringBuilder(str2);
        while (pad2.length() < maxLen) pad2.insert(0, " ");

        RenderUtil.enableScissor(x - 2f * scale, y, w + 4f * scale, topH);
        int baseColor = RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.98f);
        float cursor = x;

        for (int i = 0; i < maxLen; i++) {
            char c1 = pad1.charAt(i);
            char c2 = pad2.charAt(i);

            String newCharStr = String.valueOf(c2);
            float charW = CustomFontRenderer.getTextWidth(newCharStr, 6.0f * scale, FontWeight.SEMIBOLD);

            if (c2 == ' ') {
                charW = CustomFontRenderer.getTextWidth(String.valueOf(c1), 6.0f * scale, FontWeight.SEMIBOLD);
                if (c1 == ' ') charW = CustomFontRenderer.getTextWidth("0", 6.0f * scale, FontWeight.SEMIBOLD);
            }

            if (c1 == c2) {
                if (c2 != ' ') {
                    CustomFontRenderer.drawText(ctx, newCharStr, cursor, y + 5.5f * scale, 6.0f * scale, baseColor, FontWeight.SEMIBOLD);
                }
            } else {
                float yNew = (1f - fraction) * topH * 0.5f;
                float yOld = -fraction * topH * 0.5f;

                int colorNew = RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.98f * fraction);
                int colorOld = RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.98f * (1f - fraction));

                if (c2 != ' ') CustomFontRenderer.drawText(ctx, newCharStr, cursor, y + 5.5f * scale + yNew, 6.0f * scale, colorNew, FontWeight.SEMIBOLD);
                if (c1 != ' ') CustomFontRenderer.drawText(ctx, String.valueOf(c1), cursor, y + 5.5f * scale + yOld, 6.0f * scale, colorOld, FontWeight.SEMIBOLD);
            }
            cursor += charW;
        }

        float suffixX = x + CustomFontRenderer.getTextWidth(str2, 6.0f * scale, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, suffix, suffixX, y + 5.5f * scale, 6.0f * scale, baseColor, FontWeight.SEMIBOLD);

        RenderUtil.disableScissor();
    }

    private void updateBarTargets() {
        if (trackPlaying && animStartMs > 0L) {
            double t = (System.currentTimeMillis() - animStartMs) / 1000.0;
            for (int i = 0; i < BAR_COUNT; i++) barTargets[i] = BAR_MIN_H + (float)(Math.sin(t * BAR_FREQ[i] + BAR_PHASE[i]) * 0.5 + 0.5) * (BAR_MAX_H - BAR_MIN_H);
        } else Arrays.fill(barTargets, BAR_MIN_H);
    }

    private String extractTitle(String text) {
        if (text == null || text.isBlank()) return text;
        int idx = text.indexOf(" - ");
        return idx > 0 ? text.substring(idx + 3).trim() : text;
    }

    @Override public boolean isDraggable() { return false; }
    @Override public boolean isRemovable() { return false; }

    private MineTierStyle resolveMineTierStyle(String text) {
        if (text == null) return new MineTierStyle(0x56BAFF, 0x12FFBC);
        String lower = text.toLowerCase();
        if (lower.contains("\u043c\u0438\u0444\u0438\u0447\u0435\u0441\u043a")) return new MineTierStyle(0x9656FF, 0xDD3FFF);
        if (lower.contains("\u043b\u0435\u0433\u0435\u043d\u0434\u0430\u0440")) return new MineTierStyle(0xFF533B, 0xFFAF1B);
        return new MineTierStyle(0x56BAFF, 0x12FFBC);
    }

    private void drawGradientText(DrawContext ctx, String text, float x, float y, float size, int fromColor, int toColor, float alpha, FontWeight weight) {
        if (text == null || text.isBlank()) return;
        int len = Math.max(1, text.length() - 1);
        float drawX = x;
        for (int i = 0; i < text.length(); i++) {
            float t = i / (float) len;
            int rgb = RenderUtil.lerpColor(0xFF000000 | fromColor, 0xFF000000 | toColor, t);
            String chr = String.valueOf(text.charAt(i));
            CustomFontRenderer.drawText(ctx, chr, drawX, y, size, RenderUtil.withAlpha(rgb, alpha), weight);
            drawX += CustomFontRenderer.getTextWidth(chr, size, weight);
        }
    }

    private void drawWaves(DrawContext ctx, float x, float centerY, float alpha, float scale) {
        for (int i = 0; i < BAR_COUNT; i++) {
            float rawBarH = barHeights[i];
            float barH = rawBarH * scale;
            float barX = x + i * BAR_GAP * scale;
            float barY = centerY - barH * 0.5f;
            float brightness = 0.5f + (rawBarH - BAR_MIN_H) / (BAR_MAX_H - BAR_MIN_H) * 0.5f;
            RenderUtil.drawRoundedRect(ctx, barX, barY, BAR_W * scale, barH, BAR_RADIUS * scale, RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * brightness));
        }
    }

    private void drawTrackMarquee(DrawContext ctx, String text, float x, float y, float size, float maxWidth, float alpha) {
        if (text == null || text.isBlank() || maxWidth <= 2f) return;
        float effectiveMaxW = maxWidth;
        int color = RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.95f);
        RenderUtil.drawMarqueeText(ctx, text, x, y, effectiveMaxW, Math.max(9f, size + 3f), size, color, FontWeight.REGULAR, 18f);
    }

    private void updateArtwork(byte[] artwork) {
        if (artwork == null || artwork.length == 0) return;
        int hash = Arrays.hashCode(artwork);
        if (hash == artworkHash && artworkId != null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getTextureManager() == null) return;
        try {
            NativeImage image = readArtworkImage(artwork);
            if (image == null) return;
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            Identifier newId = Identifier.of("enough", "dynamic/nowplaying_" + (++artworkCounter));
            client.getTextureManager().registerTexture(newId, texture);
            if (artworkId != null) client.getTextureManager().destroyTexture(artworkId);
            artworkId = newId; artworkHash = hash;
        } catch (Throwable ignored) {}
    }

    private NativeImage readArtworkImage(byte[] artwork) {
        try {
            return NativeImage.read(new ByteArrayInputStream(artwork));
        } catch (Throwable ignored) {
        }

        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(artwork));
            if (buffered == null || buffered.getWidth() <= 0 || buffered.getHeight() <= 0) return null;

            NativeImage image = new NativeImage(NativeImage.Format.RGBA, buffered.getWidth(), buffered.getHeight(), false);
            for (int y = 0; y < buffered.getHeight(); y++) {
                for (int x = 0; x < buffered.getWidth(); x++) {
                    image.setColorArgb(x, y, buffered.getRGB(x, y));
                }
            }
            return image;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private static float lerp(float from, float to, float t) { return from + (to - from) * t; }
    private float islandScale() { return clamp(islandSize.get().floatValue(), 0.75f, 1.55f); }
    private record MineTierStyle(int from, int to) {}
}
