package punchy.mixin.client;

import net.minecraft.class_4587;
import net.minecraft.class_630;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.access.TransformablePart;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ModelPartMixin.class */
@Mixin({class_630.class})
public class ModelPartMixin implements TransformablePart {

    @Unique
    private Matrix4f punchy$explicitMatrix;

    @Unique
    private boolean punchy$hasDirectPoseBase;

    @Unique
    private float punchy$directPoseBaseX;

    @Unique
    private float punchy$directPoseBaseY;

    @Unique
    private float punchy$directPoseBaseZ;

    @Unique
    private float punchy$directPoseBaseXRot;

    @Unique
    private float punchy$directPoseBaseYRot;

    @Unique
    private float punchy$directPoseBaseZRot;

    @Override // punchy.client.access.TransformablePart
    public void punchy$setExplicitTransform(Matrix4f matrix) {
        this.punchy$explicitMatrix = matrix;
    }

    @Override // punchy.client.access.TransformablePart
    public Matrix4f punchy$getExplicitTransform() {
        return this.punchy$explicitMatrix;
    }

    @Override // punchy.client.access.TransformablePart
    public void punchy$setDirectPoseBase(float x, float y, float z, float xRot, float yRot, float zRot) {
        this.punchy$hasDirectPoseBase = true;
        this.punchy$directPoseBaseX = x;
        this.punchy$directPoseBaseY = y;
        this.punchy$directPoseBaseZ = z;
        this.punchy$directPoseBaseXRot = xRot;
        this.punchy$directPoseBaseYRot = yRot;
        this.punchy$directPoseBaseZRot = zRot;
    }

    @Override // punchy.client.access.TransformablePart
    public boolean punchy$hasDirectPoseBase() {
        return this.punchy$hasDirectPoseBase;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseX() {
        return this.punchy$directPoseBaseX;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseY() {
        return this.punchy$directPoseBaseY;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseZ() {
        return this.punchy$directPoseBaseZ;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseXRot() {
        return this.punchy$directPoseBaseXRot;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseYRot() {
        return this.punchy$directPoseBaseYRot;
    }

    @Override // punchy.client.access.TransformablePart
    public float punchy$getDirectPoseBaseZRot() {
        return this.punchy$directPoseBaseZRot;
    }

    @Override // punchy.client.access.TransformablePart
    public void punchy$clearDirectPoseBase() {
        this.punchy$hasDirectPoseBase = false;
        this.punchy$directPoseBaseX = 0.0f;
        this.punchy$directPoseBaseY = 0.0f;
        this.punchy$directPoseBaseZ = 0.0f;
        this.punchy$directPoseBaseXRot = 0.0f;
        this.punchy$directPoseBaseYRot = 0.0f;
        this.punchy$directPoseBaseZRot = 0.0f;
    }

    @Inject(method = {"method_22703(Lnet/minecraft/class_4587;)V"}, at = {@At("TAIL")})
    private void punchy$applyExplicitTransformAtTail(class_4587 poseStack, CallbackInfo ci) {
        if (this.punchy$explicitMatrix == null) {
            return;
        }
        class_630 self = (class_630) this;
        Matrix4f explicit = new Matrix4f(this.punchy$explicitMatrix);
        Vector3f translation = explicit.getTranslation(new Vector3f());
        translation.div(16.0f);
        explicit.setTranslation(translation);
        Matrix4f vanilla = new Matrix4f().translate(self.field_3657 / 16.0f, self.field_3656 / 16.0f, self.field_3655 / 16.0f).rotateZ(self.field_3674).rotateY(self.field_3675).rotateX(self.field_3654).scale(self.field_37938, self.field_37939, self.field_37940);
        Matrix4f correction = vanilla.invert(new Matrix4f()).mul(explicit);
        poseStack.method_34425(correction);
    }
}
