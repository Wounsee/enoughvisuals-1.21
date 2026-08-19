package fun.enoughvisuals.mixins.minecraft.world;

import fun.enoughvisuals.systems.modules.modules.visual.WorldTweaks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldCustomWeatherMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void enough$customTimeOfDay(CallbackInfoReturnable<Long> cir) {
        WorldTweaks wt = WorldTweaks.active();
        if (wt != null && wt.overridesTime()) {
            cir.setReturnValue(wt.timeOfDay());
        }
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void enough$customRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        WorldTweaks wt = WorldTweaks.active();
        if (wt != null && wt.overridesWeather()) {
            float g = wt.rainGradient();
            if (g >= 0f) cir.setReturnValue(g);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void enough$customThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        WorldTweaks wt = WorldTweaks.active();
        if (wt != null && wt.overridesWeather()) {
            float g = wt.thunderGradient();
            if (g >= 0f) cir.setReturnValue(g);
        }
    }
}
