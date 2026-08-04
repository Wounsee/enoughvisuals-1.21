package fun.wounsee.cosmetic.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.utils.RenderUtils;
import fun.wounsee.cosmetic.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private float xRot;
    @Shadow private float yRot;

    @Unique private Avatar avatar;

    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void move(float x, float y, float z);

    @Inject(method = "setup", at = @At(value = "HEAD"))
    private void setupAvatarVar(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        avatar = AvatarManager.getAvatar(focusedEntity);
    }

    // Neo adds roll in addition to pitch and yaw
    @Inject(method = "setup", at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", shift = At.Shift.AFTER), @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FFF)V", shift = At.Shift.AFTER)}, require = 1)
    private void setupRot(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            avatar = null;
            return;
        }

        float x = xRot;
        float y = yRot;

        FiguraVec3 rot = avatar.luaRuntime.renderer.cameraRot;
        if (rot != null && rot.notNaN()) {
            x = (float) rot.x;
            y = (float) rot.y;
        }

        FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetRot;
        if (offset != null && offset.notNaN()) {
            x += (float) offset.x;
            y += (float) offset.y;
        }

        setRotation(y, x);
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 0)
    private double setupPivotX(double originalX) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double x = originalX;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                x = piv.x;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                x += offset.x;
            }
            return x;
        }
        return originalX;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 1)
    private double setupPivotY(double originalY) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double y = originalY;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                y = piv.y;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                y += offset.y;
            }
            return y;
        }
        return originalY;
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 2)
    private double setupPivotZ(double originalZ) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double z = originalZ;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                z = piv.z;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                z += offset.z;
            }
            return z;
        }
        return originalZ;
    }


    @Inject(method = "setup", at = @At(value = "RETURN"))
    private void setupPos(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            FiguraVec3 pos = avatar.luaRuntime.renderer.cameraPos;
            if (pos != null && pos.notNaN())
                move((float) -pos.z, (float) pos.y, (float) -pos.x);

            avatar = null;
        }
    }

    @Inject(method = "getXRot", at = @At("HEAD"), cancellable = true)
    private void getXRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "getYRot", at = @At("HEAD"), cancellable = true)
    private void getYRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }
}
