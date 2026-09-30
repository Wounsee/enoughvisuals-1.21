package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
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
    private static final int LOGO_RASTER_SIZE = 512;
    private static final int BACKGROUND_WIDTH = 3840;
    private static final int BACKGROUND_HEIGHT = 2160;
    private static final int BRAND_WIDTH = 736;
    private static final int BRAND_HEIGHT = 104;
    private static final int CREDITS_WIDTH = 1136;
    private static final int CREDITS_HEIGHT = 152;
    private static final int VIGNETTE_HEIGHT = 1024;
    private static final int CAPSULE_WIDTH = 448;
    private static final int CAPSULE_HEIGHT = 12;
    private static final Identifier SPLASH_BACKGROUND = Identifier.of("enough", "splash/figma_background");
    private static final Identifier SPLASH_LOGO = Identifier.of("enough", "splash/logo_glyph");
    private static final Identifier SPLASH_BRAND = Identifier.of("enough", "splash/figma_brand");
    private static final Identifier SPLASH_CREDITS = Identifier.of("enough", "splash/figma_credits");
    private static final Identifier SPLASH_VIGNETTE = Identifier.of("enough", "splash/smooth_vignette");
    private static final Identifier SPLASH_CAPSULE = Identifier.of("enough", "splash/capsule");

    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;
    @Unique private float enoughvisuals$displayedProgress;
    @Unique private long enoughvisuals$lastFrameTime = -1L;

    @Inject(method = "init", at = @At("TAIL"))
    private static void enoughvisuals$prepareTextures(TextureManager textureManager, CallbackInfo ci) {
        enoughvisuals$loadTexture(textureManager,
                "/assets/enough/textures/mainmenu/bg.png", SPLASH_BACKGROUND, false);
        enoughvisuals$loadTexture(textureManager,
                "/assets/enough/textures/mainmenu/loading_brand_bootstrap.png", SPLASH_BRAND, false);
        enoughvisuals$loadTexture(textureManager,
                "/assets/enough/textures/mainmenu/loading_credits_bootstrap.png", SPLASH_CREDITS, false);
        enoughvisuals$prepareVignette(textureManager);
        enoughvisuals$prepareCapsule(textureManager);

        // Rasterize the exact Figma-derived MTSDF logo once because the custom
        // MSDF shader is intentionally unavailable during the first reload.
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(
                "/assets/enough/fonts/msdf/logo.png")) {
            if (stream == null) return;
            try (NativeImage source = NativeImage.read(stream)) {
                NativeImage glyph = new NativeImage(LOGO_RASTER_SIZE, LOGO_RASTER_SIZE, true);
                for (int y = 0; y < LOGO_RASTER_SIZE; y++) {
                    for (int x = 0; x < LOGO_RASTER_SIZE; x++) {
                        float sourceX = ((x + 0.5f) * source.getWidth() / LOGO_RASTER_SIZE) - 0.5f;
                        float sourceY = ((y + 0.5f) * source.getHeight() / LOGO_RASTER_SIZE) - 0.5f;
                        float distance = enoughvisuals$sampleDistance(source, sourceX, sourceY);
                        float coverage = enoughvisuals$smoothstep(0.482f, 0.518f, distance);
                        int alpha = MathHelper.clamp(Math.round(coverage * 255.0f), 0, 255);
                        glyph.setColorArgb(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                }
                NativeImageBackedTexture texture = new NativeImageBackedTexture(glyph);
                texture.setFilter(true, false);
                textureManager.registerTexture(SPLASH_LOGO, texture);
            }
        } catch (IOException exception) {
            System.err.println("[EnoughVisuals] Failed to prepare splash logo: " + exception.getMessage());
        }
    }

    @Unique
    private static void enoughvisuals$prepareVignette(TextureManager textureManager) {
        NativeImage image = new NativeImage(1, VIGNETTE_HEIGHT, true);
        for (int y = 0; y < VIGNETTE_HEIGHT; y++) {
            float position = y / (float) (VIGNETTE_HEIGHT - 1);
            float distance = Math.abs(position - 0.503f) / (position < 0.503f ? 0.503f : 0.497f);
            float eased = distance * distance * (3f - 2f * distance);
            int alpha = MathHelper.clamp(Math.round(MathHelper.lerp(eased, 0.13f, 0.70f) * 255f), 0, 255);
            image.setColorArgb(0, y, alpha << 24);
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        texture.setFilter(true, false);
        textureManager.registerTexture(SPLASH_VIGNETTE, texture);
    }

    @Unique
    private static void enoughvisuals$prepareCapsule(TextureManager textureManager) {
        NativeImage image = new NativeImage(CAPSULE_WIDTH, CAPSULE_HEIGHT, true);
        float radius = CAPSULE_HEIGHT * 0.5f;
        for (int y = 0; y < CAPSULE_HEIGHT; y++) {
            for (int x = 0; x < CAPSULE_WIDTH; x++) {
                float closestX = MathHelper.clamp(x + 0.5f, radius, CAPSULE_WIDTH - radius);
                float dx = x + 0.5f - closestX;
                float dy = y + 0.5f - radius;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float coverage = MathHelper.clamp(radius + 0.5f - distance, 0f, 1f);
                int alpha = Math.round(coverage * 255f);
                image.setColorArgb(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        texture.setFilter(true, false);
        textureManager.registerTexture(SPLASH_CAPSULE, texture);
    }

    @Unique
    private static void enoughvisuals$loadTexture(TextureManager textureManager, String path,
                                                  Identifier identifier, boolean mipmap) {
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(path)) {
            if (stream == null) return;
            NativeImage image = NativeImage.read(stream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            texture.setFilter(true, mipmap);
            textureManager.registerTexture(identifier, texture);
        } catch (IOException exception) {
            System.err.println("[EnoughVisuals] Failed to prepare splash texture " + path + ": "
                    + exception.getMessage());
        }
    }

    @Unique
    private static void enoughvisuals$loadWhiteMaskTexture(TextureManager textureManager,
                                                            String path, Identifier identifier) {
        try (InputStream stream = SplashOverlayMixin.class.getResourceAsStream(path)) {
            if (stream == null) return;
            try (NativeImage source = NativeImage.read(stream)) {
                NativeImage transparent = new NativeImage(source.getWidth(), source.getHeight(), true);
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        int color = source.getColorArgb(x, y);
                        int red = (color >>> 16) & 0xFF;
                        int green = (color >>> 8) & 0xFF;
                        int blue = color & 0xFF;
                        // Figma exports these vectors composited over #1E1E1E.
                        // Recover their original white coverage, including the
                        // authored 20/50/75 percent opacities, without a halo.
                        float value = Math.max(red, Math.max(green, blue));
                        int alpha = MathHelper.clamp(Math.round((value - 30f) / 225f * 255f), 0, 255);
                        transparent.setColorArgb(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                }
                NativeImageBackedTexture texture = new NativeImageBackedTexture(transparent);
                texture.setFilter(true, false);
                textureManager.registerTexture(identifier, texture);
            }
        } catch (IOException exception) {
            System.err.println("[EnoughVisuals] Failed to prepare splash mask " + path + ": "
                    + exception.getMessage());
        }
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(Lnet/minecraft/client/render/RenderLayer;IIIII)V")
    )
    private void enoughvisuals$hideMojangBackground(DrawContext context, RenderLayer layer,
                                                     int x1, int y1, int x2, int y2, int color) {
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
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V")
    )
    private void enoughvisuals$useDarkClear(float red, float green, float blue, float alpha) {
        GlStateManager._clearColor(0.035f, 0.039f, 0.035f, 1.0f);
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

        float outroTime = reloadCompleteTime < 0L ? -1f : (now - reloadCompleteTime) / 1000f;
        float outro = outroTime < 0f ? 0f : enoughvisuals$smoothstep(1f, 2f, outroTime);
        float fadeOut = 1f - outro;
        float fadeIn = reloadStartTime < 0L
                ? 1f : MathHelper.clamp((now - reloadStartTime) / 360f, 0f, 1f);
        float opacity = fadeOut * fadeIn;
        int alpha = MathHelper.clamp(Math.round(opacity * 255f), 0, 255);
        if (alpha <= 0) return;

        enoughvisuals$drawCover(context, SPLASH_BACKGROUND, width, height,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT, enoughvisuals$withAlpha(0xFFFFFFFF, alpha));
        context.draw();
        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_VIGNETTE,
                0, 0, 0f, 0f, width, height,
                1, VIGNETTE_HEIGHT, 1, VIGNETTE_HEIGHT,
                enoughvisuals$withAlpha(0xFFFFFFFF, alpha));
        context.draw();

        float contentOutro = outroTime < 0f ? 0f : enoughvisuals$smoothstep(0.82f, 1.82f, outroTime);
        float contentOpacity = fadeIn * (1f - contentOutro);
        int contentAlpha = MathHelper.clamp(Math.round(contentOpacity * 255f), 0, 255);
        float scale = Math.max(0.58f, Math.min(1.42f,
                Math.min(width / 960f, height / 540f)));
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;

        float brandX = centerX - 46f * scale;
        float brandY = centerY - 14f * scale;
        int tinted = enoughvisuals$withAlpha(0xFFFFFFFF, Math.round(contentAlpha * 0.75f));
        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_LOGO,
                Math.round(brandX), Math.round(brandY + 1.25f * scale),
                0f, 0f, Math.round(13.5f * scale), Math.round(13.5f * scale),
                LOGO_RASTER_SIZE, LOGO_RASTER_SIZE, LOGO_RASTER_SIZE, LOGO_RASTER_SIZE, tinted);
        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_BRAND,
                Math.round(brandX + 19f * scale), Math.round(brandY),
                152f, 0f, Math.round(73f * scale), Math.round(13f * scale),
                584, BRAND_HEIGHT, BRAND_WIDTH, BRAND_HEIGHT,
                enoughvisuals$withAlpha(0xFFFFFFFF, contentAlpha));

        float targetProgress = MathHelper.clamp(progress, 0f, 1f);
        if (enoughvisuals$lastFrameTime < 0L) {
            enoughvisuals$displayedProgress = targetProgress;
        } else {
            float elapsed = MathHelper.clamp(now - enoughvisuals$lastFrameTime, 0L, 100L);
            float response = 1f - (float) Math.exp(-elapsed / 175f);
            enoughvisuals$displayedProgress = MathHelper.lerp(response,
                    enoughvisuals$displayedProgress, targetProgress);
            if (targetProgress >= 1f) {
                enoughvisuals$displayedProgress = Math.max(enoughvisuals$displayedProgress, 0.998f);
            }
        }
        enoughvisuals$lastFrameTime = now;
        enoughvisuals$displayedProgress = Math.max(0f,
                Math.min(targetProgress, enoughvisuals$displayedProgress));

        float barWidth = 112f * scale;
        float barHeight = 3f * scale;
        float barX = centerX - barWidth * 0.5f;
        float barY = centerY + 9.5f * scale + contentOutro * 3f * scale;
        int barLeft = Math.round(barX);
        int barTop = Math.round(barY);
        int barPixelWidth = Math.max(1, Math.round(barWidth));
        int barPixelHeight = Math.max(1, Math.round(barHeight));
        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_CAPSULE,
                barLeft, barTop, 0f, 0f, barPixelWidth, barPixelHeight,
                CAPSULE_WIDTH, CAPSULE_HEIGHT, CAPSULE_WIDTH, CAPSULE_HEIGHT,
                enoughvisuals$withAlpha(0xFF000000, Math.round(contentAlpha * 0.60f)));
        float fillWidth = barWidth * enoughvisuals$displayedProgress;
        if (fillWidth > 0.01f) {
            context.drawTexture(RenderLayer::getGuiTextured, SPLASH_CAPSULE,
                    barLeft, barTop, 0f, 0f, Math.max(1, Math.round(fillWidth)), barPixelHeight,
                    CAPSULE_WIDTH, CAPSULE_HEIGHT, CAPSULE_WIDTH, CAPSULE_HEIGHT,
                    enoughvisuals$withAlpha(0xFFFFFFFF, Math.round(contentAlpha * 0.75f)));
        }

        context.drawTexture(RenderLayer::getGuiTextured, SPLASH_CREDITS,
                Math.round(centerX - 71f * scale), Math.round(height - 29f * scale),
                0f, 0f, Math.round(142f * scale), Math.round(19f * scale),
                CREDITS_WIDTH, CREDITS_HEIGHT, CREDITS_WIDTH, CREDITS_HEIGHT,
                enoughvisuals$withAlpha(0xFFFFFFFF, contentAlpha));
        context.draw();
    }

    @Unique
    private static void enoughvisuals$drawCover(DrawContext context, Identifier texture,
                                                int width, int height, int imageWidth, int imageHeight,
                                                int color) {
        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = imageWidth / (float) imageHeight;
        float sourceX = 0f;
        float sourceY = 0f;
        int sourceWidth = imageWidth;
        int sourceHeight = imageHeight;
        if (screenAspect > textureAspect) {
            sourceHeight = Math.round(imageWidth / screenAspect);
            sourceY = (imageHeight - sourceHeight) * 0.5f;
        } else if (screenAspect < textureAspect) {
            sourceWidth = Math.round(imageHeight * screenAspect);
            sourceX = (imageWidth - sourceWidth) * 0.5f;
        }
        context.drawTexture(RenderLayer::getGuiTextured, texture,
                0, 0, sourceX, sourceY, width, height,
                sourceWidth, sourceHeight, imageWidth, imageHeight, color);
    }

    @Unique
    private static int enoughvisuals$withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    @Unique
    private static float enoughvisuals$smoothstep(float edge0, float edge1, float value) {
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    @Unique
    private static float enoughvisuals$sampleDistance(NativeImage image, float x, float y) {
        int x0 = MathHelper.clamp((int) Math.floor(x), 0, image.getWidth() - 1);
        int y0 = MathHelper.clamp((int) Math.floor(y), 0, image.getHeight() - 1);
        int x1 = Math.min(x0 + 1, image.getWidth() - 1);
        int y1 = Math.min(y0 + 1, image.getHeight() - 1);
        float tx = MathHelper.clamp(x - (float) Math.floor(x), 0f, 1f);
        float ty = MathHelper.clamp(y - (float) Math.floor(y), 0f, 1f);
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
        float red = ((color >>> 16) & 0xFF) / 255f;
        float green = ((color >>> 8) & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        return Math.max(Math.min(red, green), Math.min(Math.max(red, green), blue));
    }
}
