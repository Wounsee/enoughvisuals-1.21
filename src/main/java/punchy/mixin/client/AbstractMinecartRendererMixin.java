package punchy.mixin.client;

import net.minecraft.class_10045;
import net.minecraft.class_2246;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_925;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.MinecartInHandContext;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/AbstractMinecartRendererMixin.class */
@Mixin({class_925.class})
public class AbstractMinecartRendererMixin {
    @Inject(method = {"method_4064(Lnet/minecraft/class_10045;Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("HEAD")})
    private void punchy$offsetTnt(class_10045 state, class_2680 blockState, class_4587 matrices, class_4597 buffer, int light, CallbackInfo ci) {
        if (!MinecartInHandContext.isActive() || blockState == null || blockState.method_26204() != class_2246.field_10375) {
            return;
        }
        float offset = MinecartInHandContext.getTntOffset();
        if (offset == 0.0f) {
            return;
        }
        matrices.method_22903();
        matrices.method_46416(0.0f, offset, 0.0f);
    }

    @Inject(method = {"method_4064(Lnet/minecraft/class_10045;Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("RETURN")})
    private void punchy$offsetTntTail(class_10045 state, class_2680 blockState, class_4587 matrices, class_4597 buffer, int light, CallbackInfo ci) {
        if (!MinecartInHandContext.isActive() || blockState == null || blockState.method_26204() != class_2246.field_10375) {
            return;
        }
        float offset = MinecartInHandContext.getTntOffset();
        if (offset == 0.0f) {
            return;
        }
        matrices.method_22909();
    }
}
