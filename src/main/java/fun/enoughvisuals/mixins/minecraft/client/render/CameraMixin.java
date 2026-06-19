package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void enough$updatePerspectiveAnimation(BlockView area, Entity focusedEntity,
                                                   boolean thirdPerson, boolean inverseView,
                                                   float tickDelta, CallbackInfo ci) {
        Animations animations = Animations.active();
        if (animations != null && animations.perspectiveAnimation.get()) {
            animations.updatePerspective(thirdPerson);
        }
    }

    @ModifyArg(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"),
            index = 0)
    private float enough$animatePerspectiveDistance(float distance) {
        Animations animations = Animations.active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (animations == null || !animations.perspectiveAnimation.get()
                || client == null || client.options == null
                || client.options.getPerspective().isFirstPerson()) {
            return distance;
        }
        return animations.perspectiveDistance(distance);
    }
}
