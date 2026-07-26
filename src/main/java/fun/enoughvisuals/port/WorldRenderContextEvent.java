package fun.enoughvisuals.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

public final class WorldRenderContextEvent extends Event {
    private final MinecraftClient client;
    private final GameRenderer gameRenderer;
    private final WorldRenderCapture capture;
    private final float tickDelta;

    public WorldRenderContextEvent(MinecraftClient client, GameRenderer gameRenderer,
                                   WorldRenderCapture capture, float tickDelta) {
        this.client = client;
        this.gameRenderer = gameRenderer;
        this.capture = capture;
        this.tickDelta = tickDelta;
    }

    public MinecraftClient getClient() { return client; }
    public GameRenderer getGameRenderer() { return gameRenderer; }
    public WorldRenderCapture getWorldRenderCapture() { return capture; }
    public Matrix4f resolve2() { return capture.resolve2(); }
    public Matrix4f resolve3() { return capture.resolve4(); }
    public float getFloatValue() { return tickDelta; }
}
