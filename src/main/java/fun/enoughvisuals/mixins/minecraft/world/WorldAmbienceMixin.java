package fun.enoughvisuals.mixins.minecraft.world;

import fun.enoughvisuals.systems.modules.modules.visual.Ambience;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldAmbienceMixin {

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.INSTANCE;
        if (ambience != null && ambience.shouldWeather()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.INSTANCE;
        if (ambience != null && ambience.shouldWeather()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        Ambience ambience = Ambience.INSTANCE;
        if (ambience != null && ambience.shouldTime()) {
            cir.setReturnValue(ambience.getWorldTime());
        }
    }
}
