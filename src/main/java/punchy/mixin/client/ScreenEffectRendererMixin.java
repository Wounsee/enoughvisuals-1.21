package punchy.mixin.client;

import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4603;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ScreenEffectRendererMixin.class */
@Mixin({class_4603.class})
public class ScreenEffectRendererMixin {
    @Invoker("method_23070")
    public static void punchy$renderFire(class_4587 poseStack, class_4597 bufferSource) {
        throw new AssertionError();
    }

    @Redirect(method = {"method_23067(Lnet/minecraft/class_310;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4603;method_23070(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;)V"))
    private static void punchy$skipFireOverlay(class_4587 poseStack, class_4597 bufferSource) {
        if (!PunchyConfig.isModEnabled() || PunchyConfig.disableEnhancedFireArmEffects()) {
            punchy$renderFire(poseStack, bufferSource);
        }
    }
}
