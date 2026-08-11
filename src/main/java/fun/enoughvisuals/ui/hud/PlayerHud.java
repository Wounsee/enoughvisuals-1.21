package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import fun.enoughvisuals.utils.system.NowPlayingService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/** Functional Figma media player HUD. */
public final class PlayerHud extends HudElement {
    /** 1369x297 Figma frame reduced uniformly to Minecraft HUD units. */
    private static final float BASE_WIDTH = 207.5f;
    private static final float BASE_HEIGHT = 45f;
    private static final float CONTROL_SIZE = 9.5f;
    private static final Identifier FALLBACK_ART = Identifier.of("enough", "logo/icon.png");

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private Identifier artworkId;
    private int artworkHash;
    private int artworkCounter;
    private float timelineProgress;
    private String timelineTrack = "";

    public PlayerHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        float s = HudDesign.scale(scale);
        setWidth(BASE_WIDTH * s);
        setHeight(BASE_HEIGHT * s);
        NowPlayingService.TrackInfo track = NowPlayingService.getNowPlaying();
        setContentVisibleTarget(true);
        if (track.active() && track.hasArtwork()) updateArtwork(track.artworkPng());
        String key = track.title() + '\u0000' + track.artist();
        if (!key.equals(timelineTrack)) {
            timelineTrack = key;
            timelineProgress = track.progress();
        } else {
            timelineProgress = RenderUtil.animate(timelineProgress, track.progress(),
                    0.16f, RenderUtil.getFrameDelta());
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        NowPlayingService.TrackInfo track = NowPlayingService.getNowPlaying();
        float s = HudDesign.scale(scale);
        float panelRadius = 12.5f * s;
        HudDesign.drawRounderPanel(ctx, x, y, w, h, panelRadius, alpha);

        float artX = x + 6.8f * s;
        float artY = y + 5.75f * s;
        float artSize = 33.1f * s;
        float radius = 8f * s;
        float outlineWidth = 1.75f * s;
        float imageInset = 1.35f * s;
        Identifier art = track.active() && artworkId != null ? artworkId : FALLBACK_ART;
        float previousExponent = RenderUtil.pushSquircleExponent(HudDesign.rounderHudExponent());
        try {
            RenderUtil.drawRoundedTextureShader(ctx, art,
                    artX + imageInset, artY + imageInset,
                    artSize - imageInset * 2f, artSize - imageInset * 2f,
                    radius - 0.9f * s, alpha,
                    0f, 0f, 1f, 1f, 1f, 1f);
            HudDesign.drawProgressOutline(ctx, artX, artY, artSize, artSize, radius, outlineWidth,
                    1f, RenderUtil.withAlpha(0xFF29292C, alpha));
            float progress = track.active() ? timelineProgress : 0f;
            HudDesign.drawProgressOutline(ctx, artX, artY, artSize, artSize, radius, outlineWidth,
                    progress, RenderUtil.withAlpha(HudDesign.ACCENT, alpha));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }

        drawVerticalDivider(ctx, x + 48.1f * s, y + 13.45f * s, 17.65f * s, s, alpha);
        drawVerticalDivider(ctx, x + 112f * s, y + 26.45f * s, 9.35f * s, s, alpha);

        String title = track.active() ? track.title()
                : ClientLanguage.text("ui.player_hud.no_track", "Название: включите трек");
        drawMarquee(ctx, title, x + 58.9f * s, y + 9.5f * s, 139f * s,
                9.7f * s, RenderUtil.withAlpha(0xFFC3C3CB, alpha), FontWeight.MEDIUM);

        String elapsed = formatTime(track.active() ? elapsedSeconds(track) : 0L);
        String total = track.active() && track.duration() > 0L
                ? formatTime(track.duration()) : "--:--";
        CustomFontRenderer.drawText(ctx, elapsed + " / " + total,
                x + 58.9f * s, y + 27f * s, 8.2f * s,
                RenderUtil.withAlpha(0xFFA3A3AA, alpha), FontWeight.MEDIUM);

        float controlY = y + 27.1f * s;
        drawControl(ctx, HudIcons.PREVIOUS, x + 120.2f * s, controlY, CONTROL_SIZE * s, alpha);
        drawControl(ctx, track.active() && track.isPlaying() ? HudIcons.PAUSE : HudIcons.PLAY,
                x + 136.1f * s, controlY, CONTROL_SIZE * s, alpha);
        drawRotatedControl(ctx, HudIcons.NEXT, x + 152f * s, controlY,
                CONTROL_SIZE * s, alpha);
    }

    private static void drawVerticalDivider(DrawContext ctx, float x, float y,
                                            float height, float s, float alpha) {
        HudDesign.drawRounderRect(ctx, x, y, 0.75f * s, height, 0.375f * s,
                RenderUtil.withAlpha(0xFF282731, alpha * 0.82f));
    }

    private static void drawControl(DrawContext ctx, String icon, float x, float y, float size, float alpha) {
        float s = size / CONTROL_SIZE;
        float iconSize = size - 0.45f * s;
        CustomFontRenderer.drawText(ctx, icon, x + 0.2f * s, y + 0.1f * s, iconSize,
                RenderUtil.withAlpha(0xFF9A989F, alpha), FontWeight.HUD_ICONS);
    }

    private static void drawRotatedControl(DrawContext ctx, String icon, float x, float y,
                                           float size, float alpha) {
        float centerX = x + size * 0.5f;
        float centerY = y + size * 0.5f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0f);
        ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.PI));
        ctx.getMatrices().translate(-centerX, -centerY, 0f);
        drawControl(ctx, icon, x, y, size, alpha);
        ctx.getMatrices().pop();
    }

    private static void drawMarquee(DrawContext ctx, String value, float x, float y, float width,
                                    float size, int color, FontWeight weight) {
        String text = value == null || value.isBlank() ? "Unknown" : value;
        if (CustomFontRenderer.getTextWidth(text, size, weight) > width) {
            RenderUtil.drawMarqueeText(ctx, text, x, y, width, size + 2f, size, color, weight, 16f);
        } else {
            CustomFontRenderer.drawText(ctx, text, x, y, size, color, weight);
        }
    }

    private static long elapsedSeconds(NowPlayingService.TrackInfo track) {
        double elapsed = Math.max(0L, track.position());
        if (track.isPlaying() && track.sampledAtMs() > 0L) {
            elapsed += Math.max(0L, System.currentTimeMillis() - track.sampledAtMs()) / 1_000.0;
        }
        if (track.duration() > 0L) elapsed = Math.min(elapsed, track.duration());
        return Math.max(0L, (long) Math.floor(elapsed));
    }

    private static String formatTime(long seconds) {
        long safe = Math.max(0L, seconds);
        long hours = safe / 3_600L;
        long minutes = (safe % 3_600L) / 60L;
        long remainingSeconds = safe % 60L;
        if (hours > 0L) {
            return String.format(java.util.Locale.ROOT, "%d:%02d:%02d",
                    hours, minutes, remainingSeconds);
        }
        return String.format(java.util.Locale.ROOT, "%d:%02d", minutes, remainingSeconds);
    }

    @Override
    public boolean onMouseClicked(float localX, float localY, int button) {
        if (button != 0) return false;
        float s = HudDesign.scale(scale);
        float rawX = localX / s;
        float rawY = localY / s;
        if (rawY < 24f || rawY > 40f) return false;
        if (rawX >= 116f && rawX < 132f) {
            NowPlayingService.previous();
            return true;
        }
        if (rawX >= 132f && rawX < 148f) {
            NowPlayingService.playPause();
            return true;
        }
        if (rawX >= 148f && rawX <= 166f) {
            NowPlayingService.next();
            return true;
        }
        return false;
    }

    private void updateArtwork(byte[] artwork) {
        if (artwork == null || artwork.length == 0) return;
        int hash = Arrays.hashCode(artwork);
        if (hash == artworkHash && artworkId != null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            NativeImage image = readArtworkImage(artwork);
            if (image == null) return;
            Identifier next = Identifier.of("enough", "dynamic/hud_player_" + (++artworkCounter));
            client.getTextureManager().registerTexture(next, new NativeImageBackedTexture(image));
            if (artworkId != null) client.getTextureManager().destroyTexture(artworkId);
            artworkId = next;
            artworkHash = hash;
        } catch (Throwable ignored) {
        }
    }

    private static NativeImage readArtworkImage(byte[] artwork) {
        try {
            return NativeImage.read(new ByteArrayInputStream(artwork));
        } catch (Throwable ignored) {
        }
        try {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(artwork));
            if (buffered == null) return null;
            NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                    buffered.getWidth(), buffered.getHeight(), false);
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
}
