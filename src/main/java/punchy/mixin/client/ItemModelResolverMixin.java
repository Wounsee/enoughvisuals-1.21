package punchy.mixin.client;

import net.minecraft.class_10442;
import net.minecraft.class_10444;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_310;
import net.minecraft.class_804;
import net.minecraft.class_811;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.CreatureBucketAnimator;
import punchy.client.render.PunchyArmRenderer;
import punchy.client.render.UseAnimationSuppressor;
import punchy.compat.BetterCombatCompat;
import punchy.config.PunchyConfig;
import punchy.mixin.client.accessor.ItemStackRenderStateAccessor;
import punchy.mixin.client.accessor.ItemStackRenderStateLayerAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ItemModelResolverMixin.class */
@Mixin({class_10442.class})
public class ItemModelResolverMixin {
    private static final ThreadLocal<Boolean> PUNCHY_REENTRANT = ThreadLocal.withInitial(() -> {
        return false;
    });

    @Inject(method = {"method_65597(Lnet/minecraft/class_10444;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_1309;)V"}, at = {@At("RETURN")})
    private void punchy$neutralizeUseTransforms(class_10444 state, class_1799 stack, class_811 displayContext, class_1309 entity, CallbackInfo ci) {
        class_1799 useStack;
        if (PUNCHY_REENTRANT.get().booleanValue() || stack == null || stack.method_7960() || PunchyConfig.isItemBlacklisted(stack)) {
            return;
        }
        if ((displayContext != class_811.field_4322 && displayContext != class_811.field_4321 && displayContext != class_811.field_4320 && displayContext != class_811.field_4323) || entity == null || stack == null || stack.method_7960() || !entity.method_6115() || (useStack = entity.method_6030()) == null || useStack.method_7960() || !class_1799.method_31577(stack, useStack)) {
            return;
        }
        ItemStackRenderStateAccessor class_10444Var = new class_10444();
        boolean prevForced = UseAnimationSuppressor.isForced();
        PUNCHY_REENTRANT.set(true);
        UseAnimationSuppressor.force(true);
        try {
            ((class_10442) this).method_65597(class_10444Var, stack, displayContext, entity);
            UseAnimationSuppressor.force(prevForced);
            PUNCHY_REENTRANT.set(false);
            ItemStackRenderStateAccessor stateAccessor = (ItemStackRenderStateAccessor) state;
            ItemStackRenderStateAccessor baselineAccessor = class_10444Var;
            ItemStackRenderStateLayerAccessor[] itemStackRenderStateLayerAccessorArrPunchy$getLayers = stateAccessor.punchy$getLayers();
            ItemStackRenderStateLayerAccessor[] itemStackRenderStateLayerAccessorArrPunchy$getLayers2 = baselineAccessor.punchy$getLayers();
            int count = Math.min(stateAccessor.punchy$getActiveLayerCount(), baselineAccessor.punchy$getActiveLayerCount());
            if (itemStackRenderStateLayerAccessorArrPunchy$getLayers == null || itemStackRenderStateLayerAccessorArrPunchy$getLayers2 == null || count <= 0) {
                return;
            }
            for (int i = 0; i < count && i < itemStackRenderStateLayerAccessorArrPunchy$getLayers.length && i < itemStackRenderStateLayerAccessorArrPunchy$getLayers2.length; i++) {
                ItemStackRenderStateLayerAccessor itemStackRenderStateLayerAccessor = itemStackRenderStateLayerAccessorArrPunchy$getLayers[i];
                ItemStackRenderStateLayerAccessor itemStackRenderStateLayerAccessor2 = itemStackRenderStateLayerAccessorArrPunchy$getLayers2[i];
                if (itemStackRenderStateLayerAccessor != null && itemStackRenderStateLayerAccessor2 != null) {
                    class_804 transform = itemStackRenderStateLayerAccessor.punchy$getTransform();
                    class_804 baseTransform = itemStackRenderStateLayerAccessor2.punchy$getTransform();
                    if (transform != null && baseTransform != null) {
                        Vector3fc rotation = baseTransform.comp_3747();
                        Vector3fc translation = baseTransform.comp_3748();
                        Vector3fc scale = transform.comp_3749();
                        if (rotation != null && translation != null && scale != null) {
                            itemStackRenderStateLayerAccessor.method_67993(new class_804(new Vector3f(rotation), new Vector3f(translation), new Vector3f(scale)));
                        }
                    }
                }
            }
            if (displayContext == class_811.field_4322 || displayContext == class_811.field_4321 || displayContext == class_811.field_4320 || displayContext == class_811.field_4323) {
                CreatureBucketAnimator.apply(stack, state, resolveHand(displayContext));
                applyVanillaWeaponDisplay(state, stack, displayContext, entity);
            }
        } catch (Throwable th) {
            UseAnimationSuppressor.force(prevForced);
            PUNCHY_REENTRANT.set(false);
            throw th;
        }
    }

    @Inject(method = {"method_65598(Lnet/minecraft/class_10444;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_1937;Lnet/minecraft/class_1309;I)V"}, at = {@At("RETURN")})
    private void punchy$splitAxolotlLayers(class_10444 state, class_1799 stack, class_811 displayContext, class_1937 level, class_1309 entity, int seed, CallbackInfo ci) {
        if (PUNCHY_REENTRANT.get().booleanValue() || stack == null || stack.method_7960() || PunchyConfig.isItemBlacklisted(stack)) {
            return;
        }
        if (displayContext == class_811.field_4322 || displayContext == class_811.field_4321 || displayContext == class_811.field_4320 || displayContext == class_811.field_4323) {
            CreatureBucketAnimator.apply(stack, state, resolveHand(displayContext));
            applyVanillaWeaponDisplay(state, stack, displayContext, null);
        }
    }

    private static void applyVanillaWeaponDisplay(class_10444 state, class_1799 stack, class_811 displayContext, class_1309 entity) {
        if (state == null || stack == null || stack.method_7960() || PunchyConfig.isItemBlacklisted(stack) || entity == null || !BetterCombatCompat.isAvailable() || !PunchyConfig.isBetterCombatCompatEnabled() || !BetterCombatCompat.isWeaponStack(stack)) {
            return;
        }
        if (displayContext != class_811.field_4320 && displayContext != class_811.field_4323) {
            return;
        }
        class_804 vanilla = PunchyArmRenderer.getCachedVanillaThirdPersonTransform(stack, displayContext);
        if (vanilla == null) {
            vanilla = resolveVanillaTransform(stack, displayContext, entity);
            if (vanilla != null) {
                PunchyArmRenderer.cacheVanillaThirdPersonTransform(stack, displayContext, vanilla);
            }
        }
        if (vanilla == null) {
            return;
        }
        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) state;
        class_10444.class_10446[] layers = accessor.punchy$getLayers();
        int count = accessor.punchy$getActiveLayerCount();
        if (layers == null || count <= 0) {
            return;
        }
        for (int i = 0; i < count && i < layers.length; i++) {
            class_10444.class_10446 layer = layers[i];
            if (layer != null) {
                layer.method_67993(vanilla);
            }
        }
    }

    private static class_804 resolveVanillaTransform(class_1799 stack, class_811 displayContext, class_1309 entity) {
        class_310 mc;
        class_10442 resolver;
        if (stack == null || stack.method_7960() || displayContext == null || entity == null || (mc = class_310.method_1551()) == null || (resolver = mc.method_65386()) == null) {
            return null;
        }
        class_10444 baseline = new class_10444();
        boolean prevForced = UseAnimationSuppressor.isForced();
        PUNCHY_REENTRANT.set(true);
        UseAnimationSuppressor.force(true);
        try {
            resolver.method_65597(baseline, stack, displayContext, entity);
            UseAnimationSuppressor.force(prevForced);
            PUNCHY_REENTRANT.set(false);
            return resolveLayerTransform(baseline);
        } catch (Throwable th) {
            UseAnimationSuppressor.force(prevForced);
            PUNCHY_REENTRANT.set(false);
            throw th;
        }
    }

    private static class_804 resolveLayerTransform(class_10444 state) {
        class_804 transform;
        if (state == null) {
            return null;
        }
        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) state;
        ItemStackRenderStateLayerAccessor[] itemStackRenderStateLayerAccessorArrPunchy$getLayers = accessor.punchy$getLayers();
        int count = accessor.punchy$getActiveLayerCount();
        if (itemStackRenderStateLayerAccessorArrPunchy$getLayers == null || count <= 0) {
            return null;
        }
        for (int i = 0; i < count && i < itemStackRenderStateLayerAccessorArrPunchy$getLayers.length; i++) {
            ItemStackRenderStateLayerAccessor itemStackRenderStateLayerAccessor = itemStackRenderStateLayerAccessorArrPunchy$getLayers[i];
            if (itemStackRenderStateLayerAccessor != null && (transform = itemStackRenderStateLayerAccessor.punchy$getTransform()) != null) {
                return transform;
            }
        }
        return null;
    }

    private static class_1268 resolveHand(class_811 displayContext) {
        if (displayContext == class_811.field_4321 || displayContext == class_811.field_4323) {
            return class_1268.field_5810;
        }
        return class_1268.field_5808;
    }
}
