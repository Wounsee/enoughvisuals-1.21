package punchy.mixin.client;

import net.minecraft.class_10485;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_1819;
import net.minecraft.class_1835;
import net.minecraft.class_310;
import net.minecraft.class_5498;
import net.minecraft.class_638;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.client.render.UseAnimationSuppressor;
import punchy.config.PunchyConfig;
import punchy.mixin.client.accessor.UseDurationAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/UseDurationPropertyMixin.class */
@Mixin({class_10485.class})
public abstract class UseDurationPropertyMixin {
    /* JADX WARN: Multi-variable type inference failed */
    @Inject(method = {"method_65644(Lnet/minecraft/class_1799;Lnet/minecraft/class_638;Lnet/minecraft/class_1309;I)F"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$gateUseDuration(class_1799 stack, class_638 world, class_1309 owner, int seed, CallbackInfoReturnable<Float> cir) {
        class_1268 hand;
        int remainingTicks;
        if (isUsingBlacklistedItem(owner) || shouldSkipBlacklist(stack, owner) || !(owner instanceof class_1309) || !(owner instanceof class_1657)) {
            return;
        }
        class_746 class_746Var = (class_1657) owner;
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1690 == null || mc.field_1690.method_31044() != class_5498.field_26664 || mc.field_1724 != class_746Var) {
            return;
        }
        if (!class_746Var.method_6115()) {
            UseAnimationSuppressor.clearReleaseStart();
            UseAnimationSuppressor.clearMeshStart();
            return;
        }
        class_1799 useStack = class_746Var.method_6030();
        if (useStack.method_7960() || stack.method_7960() || useStack != stack || (hand = resolveHand(class_746Var, stack)) == null) {
            return;
        }
        if ((UseAnimationSuppressor.isSuppressed(hand) || UseAnimationSuppressor.isForced()) && !shouldPreserveUseProperties(useStack)) {
            cir.setReturnValue(Float.valueOf(0.0f));
            return;
        }
        boolean remaining = ((UseDurationAccessor) this).punchy$isRemaining();
        long start = UseAnimationSuppressor.getReleaseStartTick(hand);
        int startRemaining = UseAnimationSuppressor.getReleaseStartRemainingTicks(hand);
        if (start >= 0 && world != null && startRemaining >= 0) {
            long elapsed = Math.max(0L, world.method_8510() - start);
            remainingTicks = Math.max(0, startRemaining - ((int) elapsed));
        } else {
            remainingTicks = class_746Var.method_6014();
        }
        int totalUse = useStack.method_7935(class_746Var);
        float outRemaining = remainingTicks;
        float outElapsed = totalUse - remainingTicks;
        if (!remaining && start >= 0 && startRemaining >= 0) {
            if (useStack.method_7909() instanceof class_1753) {
                outElapsed = Math.max(0, startRemaining - remainingTicks);
            } else if ((useStack.method_7909() instanceof class_1764) && !class_1764.method_7781(useStack)) {
                int chargeDuration = class_1764.method_7775(useStack, class_746Var);
                if (chargeDuration > 0) {
                    int endRemaining = Math.max(0, totalUse - chargeDuration);
                    int anchorRemaining = UseAnimationSuppressor.getMeshStartRemainingTicks(hand);
                    if (anchorRemaining < 0) {
                        anchorRemaining = startRemaining;
                    }
                    if (anchorRemaining >= endRemaining && anchorRemaining <= totalUse) {
                        int window = Math.max(1, anchorRemaining - endRemaining);
                        int shownElapsed = Math.max(0, anchorRemaining - remainingTicks);
                        float progress = shownElapsed / window;
                        if (progress < 0.0f) {
                            progress = 0.0f;
                        }
                        if (progress > 1.0f) {
                            progress = 1.0f;
                        }
                        outElapsed = progress * chargeDuration;
                    } else {
                        outElapsed = Math.max(0, startRemaining - remainingTicks);
                    }
                } else {
                    outElapsed = Math.max(0, startRemaining - remainingTicks);
                }
            }
        }
        cir.setReturnValue(Float.valueOf(remaining ? outRemaining : outElapsed));
    }

    private static boolean shouldSkipBlacklist(class_1799 stack, class_1309 owner) {
        class_310 mc;
        if (stack == null || stack.method_7960() || !(owner instanceof class_1657)) {
            return false;
        }
        return PunchyConfig.isItemBlacklisted(stack) && (mc = class_310.method_1551()) != null && mc.field_1690 != null && mc.field_1690.method_31044() == class_5498.field_26664 && mc.field_1724 == ((class_1657) owner);
    }

    private static boolean isUsingBlacklistedItem(class_1309 owner) {
        if (!(owner instanceof class_1657)) {
            return false;
        }
        class_1657 player = (class_1657) owner;
        if (!player.method_6115()) {
            return false;
        }
        class_1799 useStack = player.method_6030();
        return PunchyConfig.isItemBlacklisted(useStack);
    }

    private static class_1268 resolveHand(class_1657 player, class_1799 stack) {
        if (player == null || stack == null) {
            return null;
        }
        class_1799 main = player.method_6047();
        class_1799 off = player.method_6079();
        if (class_1799.method_31577(main, stack)) {
            return class_1268.field_5808;
        }
        if (class_1799.method_31577(off, stack)) {
            return class_1268.field_5810;
        }
        return null;
    }

    private static boolean shouldPreserveUseProperties(class_1799 stack) {
        if (stack == null || stack.method_7960() || (stack.method_7909() instanceof class_1819) || (stack.method_7909() instanceof class_1835) || (stack.method_7909() instanceof class_1753) || (stack.method_7909() instanceof class_1764) || UseAnimationSuppressor.isForced()) {
            return false;
        }
        return true;
    }
}
