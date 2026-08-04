package punchy.mixin.client;

import net.minecraft.class_10463;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_638;
import net.minecraft.class_811;
import net.minecraft.class_906;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.mixin.client.accessor.PlayerFishingAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/FishingRodCastPropertyMixin.class */
@Mixin({class_10463.class})
public class FishingRodCastPropertyMixin {
    @Inject(method = {"method_65638(Lnet/minecraft/class_1799;Lnet/minecraft/class_638;Lnet/minecraft/class_1309;ILnet/minecraft/class_811;)Z"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$fixCastWhenStackIsCopied(class_1799 stack, class_638 level, class_1309 entity, int seed, class_811 displayContext, CallbackInfoReturnable<Boolean> cir) {
        class_1306 holdingArm;
        class_1799 held;
        if (entity instanceof class_1657) {
            PlayerFishingAccessor playerFishingAccessor = (class_1657) entity;
            if (stack == null || stack.method_7960()) {
                return;
            }
            if ((displayContext == class_811.field_4322 || displayContext == class_811.field_4321 || displayContext == class_811.field_4320 || displayContext == class_811.field_4323) && playerFishingAccessor.punchy$getFishing() != null && (holdingArm = class_906.method_65567(playerFishingAccessor)) != null && (held = playerFishingAccessor.method_61420(holdingArm)) != null && !held.method_7960() && class_1799.method_31577(held, stack)) {
                cir.setReturnValue(true);
            }
        }
    }
}
