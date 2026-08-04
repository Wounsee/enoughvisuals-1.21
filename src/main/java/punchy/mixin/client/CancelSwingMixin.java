package punchy.mixin.client;

import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/CancelSwingMixin.class */
@Mixin({class_1309.class})
public class CancelSwingMixin {
    @Inject(method = {"method_6055(F)F"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$freezeSwing(float partialTick, CallbackInfoReturnable<Float> cir) {
    }
}
