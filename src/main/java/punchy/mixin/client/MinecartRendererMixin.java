package punchy.mixin.client;

import net.minecraft.class_10045;
import net.minecraft.class_1299;
import net.minecraft.class_2246;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_925;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.MinecartInHandContext;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MinecartRendererMixin.class */
@Mixin({class_925.class})
public class MinecartRendererMixin {

    @Unique
    private static final ThreadLocal<Boolean> PUNCHY_TNT_OFFSET = ThreadLocal.withInitial(() -> {
        return false;
    });

    @Inject(method = {"method_4064(Lnet/minecraft/class_10045;Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$offsetTntHead(class_10045 cart, class_2680 blockState, class_4587 matrices, class_4597 buffer, int light, CallbackInfo ci) {
        if (!MinecartInHandContext.isActive()) {
            return;
        }
        if (class_1299.field_6126.equals(MinecartInHandContext.getType())) {
            ci.cancel();
            return;
        }
        if (blockState == null || blockState.method_26204() != class_2246.field_10375) {
            return;
        }
        float offset = MinecartInHandContext.getTntOffset();
        if (offset == 0.0f) {
            return;
        }
        matrices.method_22903();
        matrices.method_46416(0.0f, offset, 0.0f);
        PUNCHY_TNT_OFFSET.set(true);
    }

    @Inject(method = {"method_4064(Lnet/minecraft/class_10045;Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("RETURN")})
    private void punchy$offsetTntTail(class_10045 cart, class_2680 blockState, class_4587 matrices, class_4597 buffer, int light, CallbackInfo ci) {
        if (!PUNCHY_TNT_OFFSET.get().booleanValue()) {
            return;
        }
        PUNCHY_TNT_OFFSET.set(false);
        matrices.method_22909();
    }
}
