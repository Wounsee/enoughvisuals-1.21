package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.ui.mainmenu.WindowTitleAnimator;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(MinecraftClient.class)
public class WindowTitleMixin {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getCustomTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(WindowTitleAnimator.currentTitle());
    }
}
