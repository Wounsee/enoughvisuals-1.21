package fun.enoughvisuals.systems.webpanel;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.visual.WebPanels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
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
 * Captures the already-rendered first-person Minecraft framebuffer for an
 * in-process Chromium MediaStream. It never enumerates an OS camera and never
 * runs a second world pass, so shader packs and hand shaders remain re-entrant.
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

    private MinecraftVirtualCamera() {
    }

    public static long latestFrameSequence() {
        return latestFrameSequence;
    }

    public static String latestFrameDataUrl() {
        return latestFrameDataUrl;
    }

    public static boolean isRenderingVirtualView() {
        return false;
    }

    /** Copies either the world-only stage or the final GUI stage into the browser stream. */
    public static void captureFrame(boolean guiStage) {
        MinecraftClient client = MinecraftClient.getInstance();
        WebPanels webPanels = WebPanels.getInstance();
        if (client == null || client.world == null || client.player == null
                || webPanels == null || !webPanels.needsVirtualCameraFrames()) return;
        if (webPanels.streamVirtualCameraGui() != guiStage) return;
        if (!RenderSystem.isOnRenderThread() || ENCODING.get()) return;

        long now = System.nanoTime();
        if (now - lastCaptureNanos < FRAME_INTERVAL_NANOS) return;
        lastCaptureNanos = now;

        Framebuffer source = client.getFramebuffer();
        int oldReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldPackAlignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        try {
            ensureFramebuffer();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFramebuffer.fbo);
            GL30.glBlitFramebuffer(
                    0, 0, source.textureWidth, source.textureHeight,
                    0, 0, WIDTH, HEIGHT,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);

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
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, oldPackAlignment);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDrawFbo);
        }
    }

    private static void ensureFramebuffer() {
        if (captureFramebuffer != null
                && captureFramebuffer.textureWidth == WIDTH
                && captureFramebuffer.textureHeight == HEIGHT
                && captureFramebuffer.useDepthAttachment) return;
        if (captureFramebuffer != null) captureFramebuffer.delete();
        captureFramebuffer = new SimpleFramebuffer(WIDTH, HEIGHT, true);
        captureFramebuffer.setTexFilter(GL11.GL_LINEAR);
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
