package punchy.mixin.client;

import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.client.state.MiningActionTracker;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MultiPlayerGameModeMiningMixin.class */
@Mixin({class_636.class})
public class MultiPlayerGameModeMiningMixin {

    @Unique
    private boolean punchy$startTargetWasTrackable;

    @Unique
    private boolean punchy$continueTargetWasTrackable;

    @Inject(method = {"method_2910(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z"}, at = {@At("HEAD")})
    private void punchy$captureStartDestroy(class_2338 pos, class_2350 direction, CallbackInfoReturnable<Boolean> cir) {
        this.punchy$startTargetWasTrackable = punchy$isTrackableMiningTarget(pos);
    }

    @Inject(method = {"method_2910(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z"}, at = {@At("RETURN")})
    private void punchy$onStartDestroy(class_2338 pos, class_2350 direction, CallbackInfoReturnable<Boolean> cir) {
        if (!this.punchy$startTargetWasTrackable) {
            return;
        }
        class_243 hitLocation = punchy$resolveHitLocation(pos, direction);
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            MiningActionTracker.recordStart(pos, direction, hitLocation);
        } else {
            MiningActionTracker.recordAttempt(pos, direction, hitLocation);
        }
    }

    @Inject(method = {"method_2902(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z"}, at = {@At("HEAD")})
    private void punchy$captureContinueDestroy(class_2338 pos, class_2350 direction, CallbackInfoReturnable<Boolean> cir) {
        this.punchy$continueTargetWasTrackable = punchy$isTrackableMiningTarget(pos);
    }

    @Inject(method = {"method_2902(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z"}, at = {@At("RETURN")})
    private void punchy$onContinueDestroy(class_2338 pos, class_2350 direction, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.punchy$continueTargetWasTrackable) {
            MiningActionTracker.recordContinue(pos, direction, punchy$resolveHitLocation(pos, direction));
        }
    }

    @Inject(method = {"method_2925()V"}, at = {@At("HEAD")})
    private void punchy$onStopDestroy(CallbackInfo ci) {
        MiningActionTracker.recordStop();
    }

    @Unique
    private boolean punchy$isTrackableMiningTarget(class_2338 pos) {
        class_2680 state;
        class_310 client = class_310.method_1551();
        return (client == null || client.field_1687 == null || pos == null || (state = client.field_1687.method_8320(pos)) == null || state.method_26215()) ? false : true;
    }

    @Unique
    private class_243 punchy$resolveHitLocation(class_2338 pos, class_2350 direction) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1765 == null || pos == null) {
            return null;
        }
        class_3965 class_3965Var = client.field_1765;
        if (!(class_3965Var instanceof class_3965)) {
            return null;
        }
        class_3965 blockHit = class_3965Var;
        if (class_3965Var.method_17783() != class_239.class_240.field_1332 || !pos.equals(blockHit.method_17777())) {
            return null;
        }
        if (direction != null && blockHit.method_17780() != direction) {
            return null;
        }
        return blockHit.method_17784();
    }
}
