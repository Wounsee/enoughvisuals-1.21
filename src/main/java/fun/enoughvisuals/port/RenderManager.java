package fun.enoughvisuals.port;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public final class RenderManager {
    private RenderManager() { }
    public static void invoke76(MatrixStack matrices, float x, float y, float z) {
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        matrices.translate(x - camera.x, y - camera.y, z - camera.z);
    }
}
