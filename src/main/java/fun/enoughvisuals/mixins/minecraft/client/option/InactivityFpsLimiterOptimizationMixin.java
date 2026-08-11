package fun.enoughvisuals.mixins.minecraft.client.option;

import fun.enoughvisuals.systems.modules.modules.utils.Optimization;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public abstract class InactivityFpsLimiterOptimizationMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void enough$keepUnlimitedFps(CallbackInfoReturnable<Integer> cir) {
        if (Optimization.forceUnlimitedFps()) cir.setReturnValue(260);
    }
}
