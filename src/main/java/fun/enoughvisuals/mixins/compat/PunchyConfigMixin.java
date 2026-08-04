package fun.enoughvisuals.mixins.compat;

import fun.enoughvisuals.systems.modules.modules.visual.BetterHands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes the embedded Punchy runtime obey the EnoughVisuals module toggle. */
@Mixin(targets = "punchy.config.PunchyConfig", remap = false)
public abstract class PunchyConfigMixin {
    @Inject(method = "isModEnabled", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private static void enough$gatePunchy(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && BetterHands.isHoldMyItemsEnabled());
    }
}
