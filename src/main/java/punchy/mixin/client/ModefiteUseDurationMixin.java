package punchy.mixin.client;

import java.lang.reflect.Method;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_5498;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.render.UseAnimationSuppressor;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ModefiteUseDurationMixin.class */
@Pseudo
@Mixin(targets = {"timmychips/modefiteitemdefinitions/property/resolver/rangeentry/UseDurationFloat"}, remap = false)
public class ModefiteUseDurationMixin {
    @Inject(method = {"getValue"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$gateUseDuration(class_1799 stack, class_1309 entity, @Coerce Object definition, CallbackInfoReturnable<Float> cir) {
        if (shouldGateUseDuration(stack, entity)) {
            cir.setReturnValue(Float.valueOf(0.0f));
            return;
        }
        Float adjusted = resolveMeshStartValue(stack, entity, definition);
        if (adjusted != null) {
            cir.setReturnValue(adjusted);
        }
    }

    private static boolean shouldGateUseDuration(class_1799 stack, class_1309 entity) {
        class_310 mc;
        class_746 class_746Var;
        AnimationClip clip;
        if ((stack != null && PunchyConfig.isItemBlacklisted(stack)) || (mc = class_310.method_1551()) == null || mc.field_1690 == null || mc.field_1690.method_31044() != class_5498.field_26664 || !(entity instanceof class_1657) || mc.field_1724 != (class_746Var = (class_1657) entity) || !class_746Var.method_6115()) {
            return false;
        }
        class_1799 useStack = class_746Var.method_6030();
        if (stack == null || stack.method_7960() || useStack == null || useStack.method_7960() || !class_1799.method_31577(stack, useStack) || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || !PunchyAnimationManager.isClipNamed(clip, "use_bow")) {
            return false;
        }
        boolean hasTrigger = clip.hasEffectAny("start_mesh_animation") || clip.hasEffectAny("loadstart") || clip.hasEffectAny("load_start");
        if (!hasTrigger) {
            return false;
        }
        float time = PunchyAnimationManager.POSE_HANDLER.getCurrentTime();
        boolean started = clip.isEffectActive("start_mesh_animation", time) || clip.isEffectActive("loadstart", time) || clip.isEffectActive("load_start", time);
        return !started;
    }

    private static Float resolveMeshStartValue(class_1799 stack, class_1309 entity, Object definition) {
        class_310 mc;
        class_746 class_746Var;
        AnimationClip clip;
        int startRemaining;
        if ((stack != null && PunchyConfig.isItemBlacklisted(stack)) || (mc = class_310.method_1551()) == null || mc.field_1690 == null || mc.field_1690.method_31044() != class_5498.field_26664 || !(entity instanceof class_1657) || mc.field_1724 != (class_746Var = (class_1657) entity) || !class_746Var.method_6115()) {
            return null;
        }
        class_1799 useStack = class_746Var.method_6030();
        if (stack == null || stack.method_7960() || useStack == null || useStack.method_7960() || !class_1799.method_31577(stack, useStack) || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || !PunchyAnimationManager.isClipNamed(clip, "use_bow")) {
            return null;
        }
        boolean hasTrigger = clip.hasEffectAny("start_mesh_animation") || clip.hasEffectAny("loadstart") || clip.hasEffectAny("load_start");
        if (!hasTrigger || (startRemaining = UseAnimationSuppressor.getMeshStartRemainingTicks()) < 0) {
            return null;
        }
        int remaining = class_746Var.method_6014();
        int elapsed = Math.max(0, startRemaining - remaining);
        boolean useRemaining = resolveUseRemaining(definition);
        return Float.valueOf(useRemaining ? remaining : elapsed);
    }

    private static boolean resolveUseRemaining(Object definition) {
        if (definition == null) {
            return false;
        }
        try {
            Method method = definition.getClass().getMethod("useRemaining", new Class[0]);
            Object value = method.invoke(definition, new Object[0]);
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            return false;
        }
    }
}
