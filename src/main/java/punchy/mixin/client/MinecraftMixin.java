package punchy.mixin.client;

import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_239;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.client.render.ToolTuning;
import punchy.client.state.AttackActionTracker;
import punchy.client.state.SpearStateMachine;
import punchy.client.state.UseActionTracker;
import punchy.client.state.UseItemStateMachine;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MinecraftMixin.class */
@Mixin({class_310.class})
public class MinecraftMixin {
    @Inject(method = {"method_1508()V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/class_746;method_7290(Z)Z")})
    private void punchy$onDropKeyUse(CallbackInfo ci) {
        class_310 mc = (class_310) this;
        if (mc.field_1724 == null) {
            return;
        }
        class_746 player = mc.field_1724;
        class_1799 stack = player.method_6047();
        if (stack == null || stack.method_7960()) {
            return;
        }
        UseItemStateMachine.capturePendingDropSnapshot(class_1268.field_5808, stack);
        UseActionTracker.recordUse(class_1268.field_5808, UseActionTracker.TriggerType.DROP_ITEM);
    }

    @Inject(method = {"method_1536()Z"}, at = {@At("HEAD")})
    private void punchy$onStartAttackHead(CallbackInfoReturnable<Boolean> cir) {
        class_310 mc = (class_310) this;
        AttackActionTracker.recordCriticalCandidate(mc);
    }

    @Inject(method = {"method_1536()Z"}, at = {@At("RETURN")})
    private void punchy$onStartAttackReturn(CallbackInfoReturnable<Boolean> cir) {
        class_310 mc = (class_310) this;
        if (cir.getReturnValue() == null || !((Boolean) cir.getReturnValue()).booleanValue()) {
            AttackActionTracker.clearPendingCritical();
            return;
        }
        if (mc.field_1724 != null && mc.field_1765 != null && mc.field_1765.method_17783() == class_239.class_240.field_1332 && shouldSuppressBlockAttack(mc.field_1724.method_6047())) {
            AttackActionTracker.clearPendingCritical();
        } else {
            AttackActionTracker.recordAttack();
        }
    }

    private static boolean shouldSuppressBlockAttack(class_1799 stack) {
        if (stack == null || stack.method_7960() || SpearStateMachine.isSpearStack(stack)) {
            return false;
        }
        return ToolTuning.hasCustomAnimationType(stack, "mining_v2");
    }
}
