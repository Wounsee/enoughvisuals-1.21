package fun.enoughvisuals.systems.emotions.render;

import net.minecraft.client.model.ModelPart;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Coordinate transforms shared by the BOBJ mesh and attachment proxies. */
public final class BobjSpace {
    public static final float MINECRAFT_MODEL_FLOOR_Y = 1.5f;

    private BobjSpace() {
    }

    /**
     * Converts a BOBJ-world delta to Minecraft model space.
     *
     * <p>The mesh mapping is {@code F(p) = (p.x, 1.5 - p.y, -p.z)}, therefore a
     * transform delta must be converted as {@code F * delta * F^-1}.</p>
     */
    public static Matrix4f deltaToMinecraft(Matrix4f bobjDelta) {
        Matrix4f basis = new Matrix4f().scaling(1.0f, -1.0f, -1.0f);
        return new Matrix4f().translation(0.0f, MINECRAFT_MODEL_FLOOR_Y, 0.0f)
                .mul(basis)
                .mul(bobjDelta)
                .mul(basis)
                .translate(0.0f, -MINECRAFT_MODEL_FLOOR_Y, 0.0f);
    }

    /** Applies a BOBJ skinning delta to a vanilla attachment part's original pivot. */
    public static void applySkinningToPart(Matrix4f bobjSkinning, ModelPart part) {
        Matrix4f target = deltaToMinecraft(bobjSkinning)
                .mul(new Matrix4f().translation(
                        part.pivotX / 16.0f,
                        part.pivotY / 16.0f,
                        part.pivotZ / 16.0f));

        Vector3f translation = target.getTranslation(new Vector3f());
        Vector3f scale = target.getScale(new Vector3f());
        Quaternionf rotation = target.getUnnormalizedRotation(new Quaternionf()).normalize();
        Vector3f euler = rotation.getEulerAnglesZYX(new Vector3f());

        part.pivotX = translation.x * 16.0f;
        part.pivotY = translation.y * 16.0f;
        part.pivotZ = translation.z * 16.0f;
        part.pitch = euler.x;
        part.yaw = euler.y;
        part.roll = euler.z;
        part.xScale = scale.x;
        part.yScale = scale.y;
        part.zScale = scale.z;
    }
}
