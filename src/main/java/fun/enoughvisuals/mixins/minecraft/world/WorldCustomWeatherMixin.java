package fun.enoughvisuals.mixins.minecraft.world;

import fun.enoughvisuals.systems.modules.modules.visual.CustomWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldCustomWeatherMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void enough$customTimeOfDay(CallbackInfoReturnable<Long> cir) {
        CustomWorld customWorld = CustomWorld.active();
        if (customWorld != null && customWorld.customTime.get()) {
            cir.setReturnValue(customWorld.timeOfDay());
        }
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void enough$customRaining(CallbackInfoReturnable<Boolean> cir) {
        CustomWorld customWorld = CustomWorld.active();
        if (customWorld != null && customWorld.overridesWeather()) {
            cir.setReturnValue(customWorld.shouldRain());
        }
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void enough$customRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        CustomWorld customWorld = CustomWorld.active();
        if (customWorld == null || !customWorld.overridesWeather()) return;
        float rain = customWorld.rainGradient();
        if (rain >= 0f) {
            cir.setReturnValue(rain);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void enough$customThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        CustomWorld customWorld = CustomWorld.active();
        if (customWorld == null || !customWorld.overridesWeather()) return;
        float thunder = customWorld.thunderGradient();
        if (thunder >= 0f) {
            cir.setReturnValue(thunder);
        }
    }
}
