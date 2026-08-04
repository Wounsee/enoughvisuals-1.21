package punchy.client.modelparts.util;

import net.minecraft.class_630;
import org.joml.Matrix4f;
import punchy.client.access.TransformablePart;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/util/ModelPartMath.class */
public final class ModelPartMath {
    private ModelPartMath() {
    }

    public static Matrix4f getLocalMatrix(class_630 part) {
        if (part == null) {
            return null;
        }
        if (part instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) part;
            Matrix4f explicit = transformable.punchy$getExplicitTransform();
            if (explicit != null) {
                return new Matrix4f(explicit);
            }
        }
        return new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
    }
}
