package fun.enoughvisuals.mixins.minecraft.client.world;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fun.enoughvisuals.systems.modules.modules.visual.WorldTweaks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Shadow private long time;

    @Shadow private long timeOfDay;

    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long hookGetTime(long original) {
        WorldTweaks wt = WorldTweaks.active();
        return wt != null && wt.overridesTime() ? wt.timeOfDay() : this.timeOfDay;
    }
}
