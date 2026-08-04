package punchy.mixin.client;

import net.minecraft.class_10042;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/LivingEntityRendererMixin.class */
@Mixin({class_922.class})
public abstract class LivingEntityRendererMixin {
    @Inject(method = {"method_4054(Lnet/minecraft/class_10042;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/class_583;method_2819(Lnet/minecraft/class_10017;)V", shift = At.Shift.AFTER)})
    private void punchy$applyThirdPersonAnimation(class_10042 renderState, class_4587 poseStack, class_4597 buffer, int packedLight, CallbackInfo ci) {
    }
}
