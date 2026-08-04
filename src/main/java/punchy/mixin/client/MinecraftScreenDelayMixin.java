package punchy.mixin.client;

import net.minecraft.class_310;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.state.InterfaceDelayState;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MinecraftScreenDelayMixin.class */
@Mixin({class_310.class})
public class MinecraftScreenDelayMixin {
    @Inject(method = {"method_1507(Lnet/minecraft/class_437;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$delayInterface(class_437 screen, CallbackInfo ci) {
        if (InterfaceDelayState.shouldDelay(screen)) {
            InterfaceDelayState.queue(screen);
            ci.cancel();
        }
    }
}
