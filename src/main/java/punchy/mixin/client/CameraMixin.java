package punchy.mixin.client;

import net.minecraft.class_1297;
import net.minecraft.class_1922;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_746;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.CameraAnimationState;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/CameraMixin.class */
@Mixin({class_4184.class})
public abstract class CameraMixin {

    @Shadow
    @Final
    private static Vector3f field_52123;

    @Shadow
    @Final
    private static Vector3f field_52124;

    @Shadow
    @Final
    private static Vector3f field_52125;

    @Shadow
    private class_243 field_18712;

    @Shadow
    @Final
    private Quaternionf field_21518;

    @Shadow
    @Final
    private Vector3f field_18714;

    @Shadow
    @Final
    private Vector3f field_18715;

    @Shadow
    @Final
    private Vector3f field_18716;

    @Shadow
    private class_1297 field_18711;

    @Shadow
    protected abstract void method_19322(class_243 class_243Var);

    @Inject(method = {"method_19321(Lnet/minecraft/class_1922;Lnet/minecraft/class_1297;ZZF)V"}, at = {@At("TAIL")})
    private void punchy$applyCameraAnimation(class_1922 level, class_1297 entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1690 == null || !mc.field_1690.method_31044().method_31034() || !(this.field_18711 instanceof class_746) || mc.field_1724 != this.field_18711 || !CameraAnimationState.isActive()) {
            return;
        }
        Vector3f viewOffset = CameraAnimationState.offset();
        if (viewOffset.lengthSquared() > 0.0f) {
            viewOffset.rotate(this.field_21518);
            method_19322(this.field_18712.method_1031(viewOffset.x, viewOffset.y, viewOffset.z));
        }
        Quaternionf animRot = CameraAnimationState.rotation();
        if (Math.abs(animRot.w - 1.0f) > 1.0E-4f || animRot.x != 0.0f || animRot.y != 0.0f || animRot.z != 0.0f) {
            Quaternionf newRot = new Quaternionf(this.field_21518).mul(animRot);
            this.field_21518.set(newRot);
            this.field_18714.set(field_52123).rotate(this.field_21518);
            this.field_18715.set(field_52124).rotate(this.field_21518);
            this.field_18716.set(field_52125).rotate(this.field_21518);
        }
    }
}
