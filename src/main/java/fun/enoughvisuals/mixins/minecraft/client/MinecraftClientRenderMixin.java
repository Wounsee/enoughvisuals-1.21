package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.modules.modules.utils.Optimization;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientRenderMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void enough$tickOptimizationFrame(boolean tick, CallbackInfo ci) {
        Optimization.tickFrame();
    }
}
