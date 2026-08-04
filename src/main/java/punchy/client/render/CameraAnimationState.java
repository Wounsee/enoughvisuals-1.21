package punchy.client.render;

import net.minecraft.class_630;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import punchy.client.access.TransformablePart;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/CameraAnimationState.class */
public final class CameraAnimationState {
    private static final float EPSILON = 1.0E-4f;
    private static boolean active;
    public static float CAMERA_POS_SCALE = 0.0625f;
    public static float CAMERA_ROT_SCALE = 1.0f;
    private static final Quaternionf ROTATION = new Quaternionf();
    private static final Vector3f OFFSET = new Vector3f();

    private CameraAnimationState() {
    }

    public static void clear() {
        ROTATION.identity();
        OFFSET.zero();
        active = false;
    }

    public static void update(class_630 cameraPart) {
        if (cameraPart == null || !(cameraPart instanceof TransformablePart)) {
            clear();
            return;
        }
        TransformablePart transformable = (TransformablePart) cameraPart;
        Matrix4f explicit = transformable.punchy$getExplicitTransform();
        if (explicit == null) {
            clear();
            return;
        }
        Matrix4f base = new Matrix4f().translate(cameraPart.field_3657, cameraPart.field_3656, cameraPart.field_3655).rotateZ(cameraPart.field_3674).rotateY(cameraPart.field_3675).rotateX(cameraPart.field_3654).scale(cameraPart.field_37938, cameraPart.field_37939, cameraPart.field_37940);
        Matrix4f baseInv = new Matrix4f(base).invert();
        Matrix4f anim = baseInv.mul(explicit, new Matrix4f());
        Vector3f delta = anim.getTranslation(new Vector3f()).mul(CAMERA_POS_SCALE);
        delta.x = -delta.x;
        Quaternionf rot = new Quaternionf();
        anim.getUnnormalizedRotation(rot);
        rot.normalize();
        Vector3f euler = rot.getEulerAnglesZYX(new Vector3f());
        euler.x = -euler.x;
        float swap = euler.y;
        euler.y = euler.z;
        euler.z = swap;
        euler.y = -euler.y;
        euler.z = -euler.z;
        euler.mul(CAMERA_ROT_SCALE);
        rot.rotationZYX(euler.z, euler.y, euler.x);
        ROTATION.set(rot);
        OFFSET.set(delta);
        active = delta.lengthSquared() > EPSILON || Math.abs(ROTATION.w - 1.0f) > EPSILON;
    }

    public static boolean isActive() {
        return active;
    }

    public static Quaternionf rotation() {
        return new Quaternionf(ROTATION);
    }

    public static Vector3f offset() {
        return new Vector3f(OFFSET);
    }
}
