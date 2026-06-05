package fun.enoughvisuals.mixins.minecraft.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import static fun.enoughvisuals.Enough.setIcon;


@Mixin(MinecraftClient.class)
public class WindowTitleMixin {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getCustomTitle(CallbackInfoReturnable<String> cir) {

        cir.setReturnValue("Enough Visuals | 2.0");
        setIcon();
    }
}