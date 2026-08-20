package fun.enoughvisuals.systems.webpanel;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.WebPanels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Renders a dedicated Minecraft selfie view for an in-process Chromium
 * MediaStream. It deliberately does not enumerate or open an operating-system camera.
 */
public final class MinecraftVirtualCamera {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 360;
    private static final long FRAME_INTERVAL_NANOS = 83_333_333L; // 12 FPS

    private static final ExecutorService ENCODER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Enough-Minecraft-Camera");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicBoolean ENCODING = new AtomicBoolean();

    private static SimpleFramebuffer captureFramebuffer;
    private static long lastCaptureNanos;
    private static volatile String latestFrameDataUrl;
    private static volatile long latestFrameSequence;
    private static boolean renderingVirtualView;

    private MinecraftVirtualCamera() {
    }

    public static long latestFrameSequence() {
        return latestFrameSequence;
    }

    public static String latestFrameDataUrl() {
        return latestFrameDataUrl;
    }

    public static boolean isRenderingVirtualView() {
        return renderingVirtualView;
    }

    /** Renders a separate front-facing third-person view without changing the player's visible camera. */
    public static void captureSelfieFrame(RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        WebPanels webPanels = WebPanels.getInstance();
        if (client == null || client.world == null || client.player == null
                || webPanels == null || !webPanels.needsVirtualCameraFrames()) return;
        if (!RenderSystem.isOnRenderThread() || ENCODING.get() || renderingVirtualView) return;

        long now = System.nanoTime();
        if (now - lastCaptureNanos < FRAME_INTERVAL_NANOS) return;
        lastCaptureNanos = now;

        Framebuffer source = client.getFramebuffer();
        Perspective previousPerspective = client.options.getPerspective();
        Perspective capturePerspective = webPanels.virtualCameraPerspective();
        Entity previousTargetedEntity = client.targetedEntity;
        HitResult previousCrosshairTarget = client.crosshairTarget;
        int oldReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldPackAlignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        try {
            ensureFramebuffer(source);
            renderingVirtualView = true;
            client.options.setPerspective(capturePerspective);
            ((MinecraftClientAccessor) client).enough$setFramebuffer(captureFramebuffer);
            captureFramebuffer.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            captureFramebuffer.clear();
            client.gameRenderer.renderWorld(tickCounter);

            ByteBuffer pixels = MemoryUtil.memAlloc(WIDTH * HEIGHT * 3);
            try {
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFramebuffer.fbo);
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);

                byte[] rawBottomUp = new byte[pixels.remaining()];
                pixels.get(rawBottomUp);
                if (ENCODING.compareAndSet(false, true)) {
                    ENCODER.execute(() -> encodeFrame(rawBottomUp));
                }
            } finally {
                MemoryUtil.memFree(pixels);
            }
        } catch (Throwable ignored) {
            ENCODING.set(false);
        } finally {
            ((MinecraftClientAccessor) client).enough$setFramebuffer(source);
            client.options.setPerspective(previousPerspective);
            client.targetedEntity = previousTargetedEntity;
            client.crosshairTarget = previousCrosshairTarget;
            Entity cameraEntity = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
            if (client.world != null && cameraEntity != null) {
                client.gameRenderer.getCamera().update(client.world, cameraEntity,
                        !previousPerspective.isFirstPerson(), previousPerspective.isFrontView(),
                        tickCounter.getTickDelta(true));
            }
            renderingVirtualView = false;
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, oldPackAlignment);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDrawFbo);
            GlStateManager._viewport(0, 0, source.viewportWidth, source.viewportHeight);
        }
    }

    private static void ensureFramebuffer(Framebuffer source) {
        if (captureFramebuffer != null
                && captureFramebuffer.textureWidth == WIDTH
                && captureFramebuffer.textureHeight == HEIGHT) return;
        if (captureFramebuffer != null) captureFramebuffer.delete();
        captureFramebuffer = new SimpleFramebuffer(WIDTH, HEIGHT, false);
        captureFramebuffer.setTexFilter(GL11.GL_LINEAR);
        // SimpleFramebuffer construction changes the bound target and viewport.
        source.beginWrite(true);
    }

    private static void encodeFrame(byte[] rawBottomUp) {
        ByteBuffer topDown = MemoryUtil.memAlloc(WIDTH * HEIGHT * 3);
        STBIWriteCallback callback = null;
        try {
            int stride = WIDTH * 3;
            for (int y = 0; y < HEIGHT; y++) {
                topDown.put(rawBottomUp, (HEIGHT - 1 - y) * stride, stride);
            }
            topDown.flip();

            ByteArrayOutputStream jpeg = new ByteArrayOutputStream(96 * 1024);
            callback = STBIWriteCallback.create((context, data, size) -> {
                ByteBuffer chunk = STBIWriteCallback.getData(data, size);
                byte[] bytes = new byte[size];
                chunk.get(bytes);
                jpeg.writeBytes(bytes);
            });
            int written = STBImageWrite.stbi_write_jpg_to_func(
                    callback, 0L, WIDTH, HEIGHT, 3, topDown, 72);
            if (written != 0 && jpeg.size() > 0) {
                latestFrameDataUrl = "data:image/jpeg;base64,"
                        + Base64.getEncoder().encodeToString(jpeg.toByteArray());
                latestFrameSequence++;
            }
        } catch (Throwable ignored) {
        } finally {
            if (callback != null) callback.free();
            MemoryUtil.memFree(topDown);
            ENCODING.set(false);
        }
    }
}
