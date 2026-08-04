package punchy.mixin.client;

import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.HandRenderBobContext;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/GameRendererMixin.class */
@Mixin({class_757.class})
public class GameRendererMixin {
    @Inject(method = {"method_3186(Lnet/minecraft/class_4587;F)V"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$disableViewBobDuringSprint(class_4587 poseStack, float tickDelta, CallbackInfo ci) {
        class_310 mc;
        if (tickDelta < 0.0f || (mc = class_310.method_1551()) == null) {
            return;
        }
        class_742 class_742VarMethod_1560 = mc.method_1560();
        if (!(class_742VarMethod_1560 instanceof class_742)) {
            return;
        }
        class_742 player = class_742VarMethod_1560;
        float delta = player.field_53039 - player.field_53038;
        float walk = -(player.field_53039 + (delta * tickDelta));
        float bob = class_3532.method_16439(tickDelta, player.field_7505, player.field_7483);
        HandRenderBobContext.updateBobSample(walk, bob);
    }
}
