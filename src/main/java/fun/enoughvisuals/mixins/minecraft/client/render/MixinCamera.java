package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow
    protected void moveBy(float x, float y, float z) {
    }

    @Shadow
    protected void setRotation(float yaw, float pitch) {
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void enough$animatePerspectiveCamera(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                                 boolean inverseView, float tickDelta, CallbackInfo ci) {
        Animations animations = Animations.active();
        if (animations == null || !thirdPerson || focusedEntity == null) return;

        float progress = animations.perspectiveDistanceProgress();
        if (progress >= 0.999f) return;

        Camera camera = (Camera) (Object) this;
        Vec3d cameraPos = camera.getPos();
        double eyeX = MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX());
        double eyeY = MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY());
        double eyeZ = MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ());
        if (focusedEntity instanceof LivingEntity living) {
            eyeY += living.getStandingEyeHeight() * living.getScale();
        } else {
            eyeY += focusedEntity.getStandingEyeHeight();
        }

        double currentDistance = cameraPos.distanceTo(new Vec3d(eyeX, eyeY, eyeZ));
        if (currentDistance <= 0.02) return;

        float remaining = 1f - progress;
        boolean frontTransition = inverseView && animations.isFrontPerspectiveTransition();
        float animatedOffset = frontTransition
                ? (float) (currentDistance * 2.0 * remaining)
                : (float) Math.min(currentDistance - 0.02, currentDistance * remaining);

        if (animatedOffset > 0.001f) {
            moveBy(animatedOffset, 0f, 0f);
        }

        if (frontTransition) {
            float finalYaw = camera.getYaw();
            float finalPitch = camera.getPitch();
            float startYaw = finalYaw - 180f;
            float startPitch = -finalPitch;
            setRotation(
                    MathHelper.lerp(progress, startYaw, finalYaw),
                    MathHelper.lerp(progress, startPitch, finalPitch)
            );
        }
    }
}
