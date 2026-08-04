package punchy.mixin.client;

import net.minecraft.class_10468;
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
import net.minecraft.class_811;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.client.render.UseAnimationSuppressor;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/IsUsingItemPropertyMixin.class */
@Mixin({class_10468.class})
public abstract class IsUsingItemPropertyMixin {
    @Inject(method = {"method_65638(Lnet/minecraft/class_1799;Lnet/minecraft/class_638;Lnet/minecraft/class_1309;ILnet/minecraft/class_811;)Z"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$suppressUseProperty(class_1799 stack, class_638 world, class_1309 entity, int seed, class_811 displayContext, CallbackInfoReturnable<Boolean> cir) {
        class_1799 useStack;
        class_1268 hand;
        if (isUsingBlacklistedItem(entity) || shouldSkipBlacklist(stack, entity) || !(entity instanceof class_1657)) {
            return;
        }
        class_746 class_746Var = (class_1657) entity;
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1690 == null || mc.field_1690.method_31044() != class_5498.field_26664 || class_746Var != mc.field_1724 || !class_746Var.method_6115() || (useStack = class_746Var.method_6030()) == null || useStack.method_7960() || stack.method_7960() || useStack != stack || (hand = resolveHand(class_746Var, stack)) == null) {
            return;
        }
        if ((!UseAnimationSuppressor.isSuppressed(hand) && !UseAnimationSuppressor.isForced()) || shouldPreserveUseProperties(useStack)) {
            return;
        }
        cir.setReturnValue(false);
    }

    private static boolean shouldSkipBlacklist(class_1799 stack, class_1309 entity) {
        class_310 mc;
        if (stack == null || stack.method_7960() || !(entity instanceof class_1657)) {
            return false;
        }
        return PunchyConfig.isItemBlacklisted(stack) && (mc = class_310.method_1551()) != null && mc.field_1690 != null && mc.field_1690.method_31044() == class_5498.field_26664 && mc.field_1724 == ((class_1657) entity);
    }

    private static boolean isUsingBlacklistedItem(class_1309 entity) {
        if (!(entity instanceof class_1657)) {
            return false;
        }
        class_1657 player = (class_1657) entity;
        if (!player.method_6115()) {
            return false;
        }
        class_1799 useStack = player.method_6030();
        return PunchyConfig.isItemBlacklisted(useStack);
    }

    private static class_1268 resolveHand(class_1657 player, class_1799 stack) {
        if (stack == null) {
            return null;
        }
        class_1799 main = player.method_6047();
        class_1799 off = player.method_6079();
        if (!main.method_7960() && main.method_7909() == stack.method_7909()) {
            return class_1268.field_5808;
        }
        if (!off.method_7960() && off.method_7909() == stack.method_7909()) {
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
