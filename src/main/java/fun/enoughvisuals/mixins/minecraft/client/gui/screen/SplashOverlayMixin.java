package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    private static final int BACKGROUND = 0xFF0E0F12;
    private static final int TRACK = 0xFF242329;
    private static final int ACCENT = 0xFFB99BEA;
    private static final int ACCENT_HEAD = 0xFFE7D9FF;
    private static final int SPLASH_LOGO_SIZE = 256;
    private static final Identifier SPLASH_LOGO = Identifier.of("enough", "splash/logo_glyph");

    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;
    @Unique private float enoughvisuals$displayedProgress;
    @Unique private long enoughvisuals$lastFrameTime = -1L;

    @Inject(method = "init", at = @At("TAIL"))
    private static void enoughvisuals$prepareLogo(TextureManager textureManager, CallbackInfo ci) {
        // Rasterize the alpha distance channel of the exact F002/LOGO atlas
        // once. This preserves the menu glyph while avoiding the custom MSDF
        // shader, which is intentionally unavailable during the first reload.
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(
                "/assets/enough/fonts/msdf/logo.png")) {
            if (stream == null) return;
            try (NativeImage source = NativeImage.read(stream)) {
                NativeImage glyph = new NativeImage(SPLASH_LOGO_SIZE, SPLASH_LOGO_SIZE, true);
                for (int y = 0; y < SPLASH_LOGO_SIZE; y++) {
                    for (int x = 0; x < SPLASH_LOGO_SIZE; x++) {
                        float sourceX = ((x + 0.5f) * source.getWidth() / SPLASH_LOGO_SIZE) - 0.5f;
                        float sourceY = ((y + 0.5f) * source.getHeight() / SPLASH_LOGO_SIZE) - 0.5f;
                        float distance = enoughvisuals$sampleDistance(source, sourceX, sourceY);
                        float coverage = enoughvisuals$smoothstep(0.482f, 0.518f, distance);
                        int alpha = MathHelper.clamp(Math.round(coverage * 255.0f), 0, 255);
                        glyph.setColorArgb(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                }
                textureManager.registerTexture(SPLASH_LOGO, new NativeImageBackedTexture(glyph));
            }
        } catch (IOException exception) {
            System.err.println("[EnoughVisuals] Failed to prepare splash logo: " + exception.getMessage());
        }
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(Lnet/minecraft/client/render/RenderLayer;IIIII)V")
    )
    private void enoughvisuals$hideMojangBackground(DrawContext context, RenderLayer layer,
                                                     int x1, int y1, int x2, int y2, int color) {
        // Drawn once at TAIL with the custom fade; suppress Mojang's red layer.
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V")
    )
    private void enoughvisuals$hideMojangLogo(DrawContext context,
                                               Function<Identifier, RenderLayer> layerFactory,
                                               Identifier texture, int x, int y, float u, float v,
                                               int width, int height, int regionWidth, int regionHeight,
                                               int textureWidth, int textureHeight, int color) {
        // The original method draws both halves of the Mojang logo.
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V")
    )
    private void enoughvisuals$useDarkClear(float red, float green, float blue, float alpha) {
        GlStateManager._clearColor(0.055f, 0.059f, 0.071f, 1.0f);
    }

    @Inject(method = "renderProgressBar", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$hideMojangProgress(DrawContext context, int minX, int minY,
                                                   int maxX, int maxY, float opacity,
                                                   CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void enoughvisuals$renderSplash(DrawContext context, int mouseX, int mouseY,
                                            float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long now = Util.getMeasuringTimeMs();

        float outroTime = reloadCompleteTime < 0L
                ? -1.0f
                : (now - reloadCompleteTime) / 1000.0f;
        // Vanilla only starts drawing the destination screen beneath this
        // overlay after one second. Hold the opaque splash until that point,
        // then cross-fade throughout vanilla's remaining one-second window.
        float outro = outroTime < 0.0f
                ? 0.0f
                : enoughvisuals$smoothstep(1.0f, 2.0f, outroTime);
        float fadeOut = 1.0f - outro;
        float fadeIn = reloadStartTime < 0L
                ? 1.0f
                : MathHelper.clamp((now - reloadStartTime) / 360.0f, 0.0f, 1.0f);
        float opacity = fadeOut * fadeIn;
        int alpha = MathHelper.clamp(Math.round(opacity * 255.0f), 0, 255);
        if (alpha <= 0) return;

        context.fill(0, 0, width, height, enoughvisuals$withAlpha(BACKGROUND, alpha));
        context.draw();

        float contentOutro = outroTime < 0.0f
                ? 0.0f
                : enoughvisuals$smoothstep(0.82f, 1.82f, outroTime);
        float contentOpacity = fadeIn * (1.0f - contentOutro);
        float logoSize = MathHelper.clamp(Math.min(width, height) * 0.135f, 42.0f, 72.0f);
        float logoHeight = logoSize;
        float logoY = height * 0.5f - logoHeight * 0.5f - 26.0f;
        int logoAlpha = MathHelper.clamp(
                Math.round(255.0f * contentOpacity * (0.90f + 0.10f * fadeIn)), 0, 255);
        int drawnSize = Math.round(logoSize);
        int logoX = (width - drawnSize) / 2;
        int drawnY = Math.round(logoY);
        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_LOGO,
                logoX, drawnY, 0.0f, 0.0f,
                drawnSize, drawnSize, SPLASH_LOGO_SIZE, SPLASH_LOGO_SIZE,
                SPLASH_LOGO_SIZE, SPLASH_LOGO_SIZE,
                enoughvisuals$withAlpha(ACCENT_HEAD, logoAlpha));

        float targetProgress = MathHelper.clamp(progress, 0.0f, 1.0f);
        if (enoughvisuals$lastFrameTime < 0L) {
            enoughvisuals$displayedProgress = targetProgress;
        } else {
            float elapsed = MathHelper.clamp(now - enoughvisuals$lastFrameTime, 0L, 100L);
            float response = 1.0f - (float) Math.exp(-elapsed / 105.0f);
            enoughvisuals$displayedProgress = MathHelper.lerp(response,
                    enoughvisuals$displayedProgress, targetProgress);
            if (targetProgress >= 1.0f) {
                enoughvisuals$displayedProgress = Math.max(enoughvisuals$displayedProgress, 0.995f);
            }
        }
        enoughvisuals$lastFrameTime = now;
        enoughvisuals$displayedProgress = Math.max(0.0f,
                Math.min(targetProgress, enoughvisuals$displayedProgress));

        float eased = enoughvisuals$smoothstep(0.0f, 1.0f, enoughvisuals$displayedProgress);
        int barWidth = MathHelper.clamp(Math.round(width * 0.20f), 118, 238);
        int barX = (width - barWidth) / 2;
        int barY = Math.round(height * 0.5f + 48.0f + contentOutro * 5.0f);
        int fillWidth = MathHelper.clamp(Math.round(barWidth * eased), 0, barWidth);
        int barHeight = 5;
        int contentAlpha = MathHelper.clamp(Math.round(contentOpacity * 255.0f), 0, 255);

        RenderUtil.drawRoundedRect(context, barX, barY, barWidth, barHeight, barHeight * 0.5f,
                enoughvisuals$withAlpha(TRACK, Math.round(contentAlpha * 0.72f)));
        if (fillWidth > 0) {
            RenderUtil.drawRoundedRect(context, barX, barY, fillWidth, barHeight,
                    Math.min(barHeight * 0.5f, fillWidth * 0.5f),
                    enoughvisuals$withAlpha(ACCENT, contentAlpha));
            int highlightWidth = Math.min(22, fillWidth);
            float shimmerPhase = (now % 1350L) / 1350.0f;
            float shimmerEase = enoughvisuals$smoothstep(0.0f, 1.0f, shimmerPhase);
            float highlightX = barX + Math.max(0, fillWidth - highlightWidth) * shimmerEase;
            float shimmerAlpha = (float) Math.sin(shimmerPhase * Math.PI);
            RenderUtil.drawRoundedRect(context, highlightX, barY + 0.75f,
                    highlightWidth, barHeight - 1.5f, (barHeight - 1.5f) * 0.5f,
                    enoughvisuals$withAlpha(ACCENT_HEAD,
                            Math.round(contentAlpha * shimmerAlpha * 0.58f)));
        }
        context.draw();
    }

    private static int enoughvisuals$withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    private static float enoughvisuals$smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float enoughvisuals$sampleDistance(NativeImage image, float x, float y) {
        int x0 = MathHelper.clamp((int) Math.floor(x), 0, image.getWidth() - 1);
        int y0 = MathHelper.clamp((int) Math.floor(y), 0, image.getHeight() - 1);
        int x1 = Math.min(x0 + 1, image.getWidth() - 1);
        int y1 = Math.min(y0 + 1, image.getHeight() - 1);
        float tx = MathHelper.clamp(x - (float) Math.floor(x), 0.0f, 1.0f);
        float ty = MathHelper.clamp(y - (float) Math.floor(y), 0.0f, 1.0f);
        float a00 = enoughvisuals$medianDistance(image.getColorArgb(x0, y0));
        float a10 = enoughvisuals$medianDistance(image.getColorArgb(x1, y0));
        float a01 = enoughvisuals$medianDistance(image.getColorArgb(x0, y1));
        float a11 = enoughvisuals$medianDistance(image.getColorArgb(x1, y1));
        float top = MathHelper.lerp(tx, a00, a10);
        float bottom = MathHelper.lerp(tx, a01, a11);
        return MathHelper.lerp(ty, top, bottom);
    }

    @Unique
    private static float enoughvisuals$medianDistance(int color) {
        float red = ((color >>> 16) & 0xFF) / 255.0f;
        float green = ((color >>> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        return Math.max(Math.min(red, green), Math.min(Math.max(red, green), blue));
    }

}
