package fun.enoughvisuals.port;

import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;

public final class WorldRenderCapture {
    private final Camera camera;
    private final Matrix4f positionMatrix;
    private final Matrix4f projectionMatrix;

    public WorldRenderCapture(Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        this.camera = camera;
        this.positionMatrix = new Matrix4f(positionMatrix);
        this.projectionMatrix = new Matrix4f(projectionMatrix);
    }

    public Camera getCamera() {
        return camera;
    }

    public Matrix4f resolve2() {
        return new Matrix4f(positionMatrix);
    }

    public Matrix4f resolve4() {
        return new Matrix4f(projectionMatrix);
    }
}
