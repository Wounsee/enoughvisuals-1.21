package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @Shadow
    public abstract Perspective getPerspective();

    @Inject(method = "setPerspective", at = @At("HEAD"))
    private void enough$perspectiveChanged(Perspective perspective, CallbackInfo ci) {
        Animations.markPerspectiveSwitch(getPerspective(), perspective);
    }
}
