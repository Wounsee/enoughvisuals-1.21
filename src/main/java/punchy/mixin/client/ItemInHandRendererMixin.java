package punchy.mixin.client;

import net.minecraft.class_10255;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_759;
import net.minecraft.class_7833;
import net.minecraft.class_804;
import net.minecraft.class_811;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.render.HandRenderBobContext;
import punchy.client.render.PunchyArmRenderer;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.UseAnimationSuppressor;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.HandEquipStateMachine;
import punchy.client.state.MovementStateMachine;
import punchy.config.PunchyConfig;
import punchy.mixin.client.accessor.ItemInHandRendererInvoker;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ItemInHandRendererMixin.class */
@Mixin({class_759.class})
public class ItemInHandRendererMixin {
    private static final ThreadLocal<class_804> PUNCHY_PREV_BASELINE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PUNCHY_BASELINE_ACTIVE = ThreadLocal.withInitial(() -> {
        return false;
    });

    @Inject(method = {"method_22976(FLnet/minecraft/class_4587;Lnet/minecraft/class_4597$class_4598;Lnet/minecraft/class_746;I)V"}, at = {@At("HEAD")})
    private void punchy$renderFirstPersonModel(float partialTicks, class_4587 poseStack, class_4597.class_4598 nodeCollector, class_746 player, int combinedLight, CallbackInfo ci) {
        HandRenderBobContext.begin();
        if (punchy$shouldSuppressVanillaHandBob(player)) {
            punchy$applyInverseVanillaBob(poseStack);
        }
        PunchyArmRenderer.renderFirstPerson((class_759) this, player, partialTicks, poseStack, nodeCollector, combinedLight);
    }

    @Inject(method = {"method_22976(FLnet/minecraft/class_4587;Lnet/minecraft/class_4597$class_4598;Lnet/minecraft/class_746;I)V"}, at = {@At("RETURN")}, require = 0)
    private void punchy$endHandRenderContext(float partialTicks, class_4587 poseStack, class_4597.class_4598 nodeCollector, class_746 player, int combinedLight, CallbackInfo ci) {
        HandRenderBobContext.end();
    }

    @Inject(method = {"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$cancelVanillaArms(class_742 player, float partialTick, float pitch, class_1268 hand, float swingProgress, class_1799 item, float equippedProgress, class_4587 poseStack, class_4597 nodeCollector, int combinedLight, CallbackInfo ci) {
        class_310 mc = class_310.method_1551();
        if (!mc.field_1690.method_31044().method_31034() || mc.field_1724 != player || !PunchyConfig.isModEnabled()) {
            return;
        }
        if (HandEquipStateMachine.shouldAllowVanillaHandOut(hand)) {
            if (punchy$isActiveUseHand(player, hand)) {
                UseAnimationSuppressor.clear(hand);
                UseAnimationSuppressor.clearMeshStart(hand);
                UseAnimationSuppressor.force(false);
                return;
            }
            return;
        }
        if (PunchyConfig.isHandBlacklisted(player, hand)) {
            if (PunchyAnimationManager.shouldKeepBlacklistedOppositeHandForDualHanded(mc, hand)) {
                ci.cancel();
                return;
            } else {
                if (punchy$isActiveUseHand(player, hand)) {
                    UseAnimationSuppressor.clear(hand);
                    UseAnimationSuppressor.clearMeshStart(hand);
                    UseAnimationSuppressor.force(false);
                    return;
                }
                return;
            }
        }
        ci.cancel();
    }

    private static boolean punchy$isActiveUseHand(class_742 player, class_1268 hand) {
        return player != null && hand != null && player.method_6115() && player.method_6058() == hand;
    }

    private static boolean punchy$shouldSuppressVanillaHandBob(class_746 player) {
        if (!PunchyConfig.isModEnabled()) {
            return false;
        }
        if (player != null && (PunchyConfig.isHandBlacklisted(player, class_1268.field_5808) || PunchyConfig.isHandBlacklisted(player, class_1268.field_5810))) {
            return false;
        }
        class_310 mc = class_310.method_1551();
        if (mc != null && punchy$isBoatVehicle(mc.field_1724)) {
            return false;
        }
        if (PunchyConfig.enableCustomWalk()) {
            return true;
        }
        return PunchyConfig.enableSprintArmSwing() && MovementStateMachine.get().getSprintBlendAlpha() > 0.001f;
    }

    private static void punchy$applyInverseVanillaBob(class_4587 poseStack) {
        if (poseStack == null || !HandRenderBobContext.hasBobSample()) {
            return;
        }
        float walk = HandRenderBobContext.getLastWalkDist();
        float bob = HandRenderBobContext.getLastBob();
        float phase = walk * 3.1415927f;
        float sin = class_3532.method_15374(phase);
        float cos = class_3532.method_15362(phase);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees((-Math.abs(class_3532.method_15362(phase - 0.2f) * bob)) * 5.0f));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(-(sin * bob * 3.0f)));
        poseStack.method_46416(-(sin * bob * 0.5f), Math.abs(cos * bob), 0.0f);
    }

    @Redirect(method = {"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_759;method_3219(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;IFFLnet/minecraft/class_1306;)V"))
    private void punchy$renderCustomArm(class_759 instance, class_4587 poseStack, class_4597 nodeCollector, int combinedLight, float equipProgress, float swingProgress, class_1306 arm, class_742 player, float partialTicks, float pitch, class_1268 hand, float swingProgressArg, class_1799 stack, float equippedProgress, class_4587 originalPoseStack, class_4597 originalCollector, int originalLight) {
        if (!PunchyConfig.isModEnabled() || HandEquipStateMachine.shouldAllowVanillaHandOut(hand) || PunchyConfig.isHandBlacklisted(player, hand)) {
            ((ItemInHandRendererInvoker) instance).punchy$renderPlayerArm(poseStack, nodeCollector, combinedLight, equipProgress, swingProgress, arm);
        }
    }

    private static boolean punchy$isBoatVehicle(class_1309 entity) {
        return entity != null && (entity.method_5854() instanceof class_10255);
    }

    @Inject(method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("HEAD")})
    private void punchy$lockUseDisplayBaseline(class_1309 entity, class_1799 stack, class_811 displayContext, class_4587 poseStack, class_4597 buffer, int light, CallbackInfo ci) {
        class_1799 useStack;
        class_804 baseline;
        if (shouldSkipRenderItemHooksForInteractiveStuffCompat()) {
            clearBaselineHookState();
            return;
        }
        if (entity == null || stack == null || stack.method_7960() || !entity.method_6115() || PunchyConfig.isItemBlacklisted(stack) || (useStack = entity.method_6030()) == null || useStack.method_7960() || !class_1799.method_31577(stack, useStack) || (baseline = PunchyArmRenderer.resolveBaselineUseTransform(stack, displayContext, entity)) == null) {
            return;
        }
        PUNCHY_PREV_BASELINE.set(PunchyArmRenderer.getBaselineUseTransform());
        PUNCHY_BASELINE_ACTIVE.set(true);
        PunchyArmRenderer.setBaselineUseTransform(baseline);
    }

    @Inject(method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = {@At("RETURN")})
    private void punchy$restoreUseDisplayBaseline(class_1309 entity, class_1799 stack, class_811 displayContext, class_4587 poseStack, class_4597 buffer, int light, CallbackInfo ci) {
        if (shouldSkipRenderItemHooksForInteractiveStuffCompat()) {
            clearBaselineHookState();
        } else {
            if (!PUNCHY_BASELINE_ACTIVE.get().booleanValue()) {
                return;
            }
            PunchyArmRenderer.setBaselineUseTransform(PUNCHY_PREV_BASELINE.get());
            PUNCHY_PREV_BASELINE.remove();
            PUNCHY_BASELINE_ACTIVE.set(false);
        }
    }

    @ModifyVariable(method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"}, at = @At("HEAD"), argsOnly = true)
    private class_811 punchy$forceVanillaDisplay(class_811 displayContext, class_1309 entity, class_1799 stack) {
        if (!PunchyArmRenderer.shouldForceVanillaDisplay()) {
            return displayContext;
        }
        if (!shouldForceVanillaDisplay(displayContext, entity, stack)) {
            return displayContext;
        }
        if (displayContext == class_811.field_4320) {
            return class_811.field_4322;
        }
        if (displayContext == class_811.field_4323) {
            return class_811.field_4321;
        }
        return displayContext;
    }

    private static boolean shouldForceVanillaDisplay(class_811 displayContext, class_1309 entity, class_1799 stack) {
        VanillaFirstPersonItemLayer.ToolKind kind;
        class_811 class_811Var;
        class_1799 using;
        if ((displayContext != class_811.field_4320 && displayContext != class_811.field_4323) || entity == null || stack == null || stack.method_7960()) {
            return false;
        }
        if ((entity.method_6115() && (using = entity.method_6030()) != null && !using.method_7960() && class_1799.method_31577(stack, using)) || (stack.method_7909() instanceof class_1835) || PunchyConfig.isItemBlacklisted(stack) || (kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT)) == VanillaFirstPersonItemLayer.ToolKind.INGREDIENT || kind == VanillaFirstPersonItemLayer.ToolKind.FOOD) {
            return false;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return false;
        }
        if (displayContext == class_811.field_4320) {
            class_811Var = class_811.field_4322;
        } else {
            class_811Var = class_811.field_4321;
        }
        class_811 firstContext = class_811Var;
        class_804 thirdTransform = PunchyArmRenderer.resolveBaselineUseTransform(stack, displayContext, entity);
        class_804 firstTransform = PunchyArmRenderer.resolveBaselineUseTransform(stack, firstContext, entity);
        if (thirdTransform == null || firstTransform == null) {
            return false;
        }
        Vector3f upThird = computeUpVector(thirdTransform);
        Vector3f upFirst = computeUpVector(firstTransform);
        if (upThird == null || upFirst == null) {
            return false;
        }
        float dot = upThird.dot(upFirst);
        return dot < 0.6f;
    }

    private static Vector3f computeUpVector(class_804 transform) {
        if (transform == null || transform.comp_3747() == null) {
            return null;
        }
        Vector3f rotation = new Vector3f(transform.comp_3747());
        Matrix3f matrix = new Matrix3f().rotateX((float) Math.toRadians(rotation.x)).rotateY((float) Math.toRadians(rotation.y)).rotateZ((float) Math.toRadians(rotation.z));
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        matrix.transform(up);
        if (up.lengthSquared() > 0.0f) {
            up.normalize();
        }
        return up;
    }

    private static boolean shouldSkipRenderItemHooksForInteractiveStuffCompat() {
        class_310 mc;
        return PunchyConfig.isInteractiveStuffCompatEnabled() && (mc = class_310.method_1551()) != null && mc.field_1690 != null && mc.field_1690.method_31044().method_31034();
    }

    private static void clearBaselineHookState() {
        PunchyArmRenderer.setBaselineUseTransform(PUNCHY_PREV_BASELINE.get());
        PUNCHY_PREV_BASELINE.remove();
        PUNCHY_BASELINE_ACTIVE.set(false);
    }
}
