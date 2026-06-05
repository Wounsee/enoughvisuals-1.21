package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.Ambience;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void onGetPrecipitationAt(World world, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        Ambience ambience = Ambience.INSTANCE;
        if (ambience == null || !ambience.isEnabled()) {
            return;
        }

        if (ambience.shouldSnow()) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        } else if (ambience.rain.get()) {
            cir.setReturnValue(Biome.Precipitation.RAIN);
        }
    }
}
