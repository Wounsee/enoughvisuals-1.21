package punchy.client.render;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.class_1011;
import net.minecraft.class_10442;
import net.minecraft.class_10444;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1068;
import net.minecraft.class_1268;
import net.minecraft.class_1304;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1806;
import net.minecraft.class_1835;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5538;
import net.minecraft.class_5602;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_6862;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_759;
import net.minecraft.class_7833;
import net.minecraft.class_7924;
import net.minecraft.class_804;
import net.minecraft.class_811;
import net.minecraft.class_922;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import punchy.Punchy;
import punchy.client.PunchyClient;
import punchy.client.access.TransformablePart;
import punchy.client.animation.Easing;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.bedrockitems.BedrockItemAnimationManager;
import punchy.client.bedrockitems.BedrockItemSystem;
import punchy.client.debug.PunchyDebug;
import punchy.client.model.SimpleBedrockGeo;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.client.modelparts.ModelPartsSystem;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.ArmMeshTuning;
import punchy.client.renderer.layer.ArmToolTuning;
import punchy.client.renderer.layer.PlayerArmModelLayer;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.ClimbStateMachine;
import punchy.client.state.CrawlStateMachine;
import punchy.client.state.HandEquipStateMachine;
import punchy.client.state.MovementStateMachine;
import punchy.client.state.MudStateMachine;
import punchy.client.state.SweatStateMachine;
import punchy.compat.EmfFirstPersonHandCompat;
import punchy.compat.FirstPersonModelCompat;
import punchy.compat.SkinLayersCompat;
import punchy.config.PunchyConfig;
import punchy.mixin.client.accessor.ItemInHandRendererInvoker;
import punchy.mixin.client.accessor.ItemStackRenderStateAccessor;
import punchy.mixin.client.accessor.ItemStackRenderStateLayerAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyArmRenderer.class */
public class PunchyArmRenderer {
    private static class_591 fpModelDefault;
    private static class_591 fpModelSlim;
    private static final float PIVOT_BASE_OFFSET_Y = -0.14f;
    private static final float PIVOT_BASE_OFFSET_Z = 0.11f;
    private static final float PIVOT_ITEM_COMP_Y = 0.14f;
    private static final float ITEM_BASE_OFFSET_Y = -0.17f;
    private static final float ITEM_BASE_OFFSET_Z = -0.02f;
    private static final float PIVOT_BASE_OFFSET_X_RIGHT = -0.04f;
    private static final float PIVOT_BASE_OFFSET_X_LEFT = 0.04f;
    private static final float ITEM_BASE_OFFSET_X_RIGHT = -0.04f;
    private static final float ITEM_BASE_OFFSET_X_LEFT = 0.04f;
    private static final int FREEZE_TIP_RIGHT_U0 = 40;
    private static final int FREEZE_TIP_RIGHT_V0 = 28;
    private static final int FREEZE_TIP_LEFT_U0 = 32;
    private static final int FREEZE_TIP_LEFT_V0 = 60;
    private static final int FREEZE_TIP_W = 16;
    private static final int FREEZE_TIP_H = 4;
    private static final int SWEAT_TIP_RIGHT_U0 = 40;
    private static final int SWEAT_TIP_LEFT_U0 = 40;
    private static final int SWEAT_TIP_V0 = 28;
    private static final int SWEAT_TIP_W = 16;
    private static final int SWEAT_TIP_H = 16;
    private static final float FREEZE_VOXEL_THICKNESS = 0.45f;
    private static final float SWEAT_VOXEL_THICKNESS = 0.09f;
    private static final float SWEAT_PIXEL_SCALE_X = 0.65f;
    private static final float SWEAT_PIXEL_SCALE_Y = 0.15f;
    private static final float BURN_FADE_IN_TICKS = 30.0f;
    private static final float BURN_FADE_OUT_TICKS = 60.0f;
    private static final float FREEZE_SHAKE_TRANSLATION = 7.65E-4f;
    private static final float FREEZE_SHAKE_ROTATION_DEG = 0.1275f;
    private static final float OFFHAND_LOWER_Y = 0.0f;
    private static final float OFFHAND_LOWER_Z = 0.0f;
    private static final float OFFHAND_LOWER_X = 0.0f;
    private static final float OFFHAND_LOWER_ROT_X = 90.0f;
    private static final int OFFHAND_HIDE_AFTER_LOWER_FRAMES = 6;
    private static final float ELYTRA_OFFHAND_SHOW_SPEED_MIN = 0.55f;
    private static volatile boolean FIRSTPERSON_HIDE;
    private static final float CRAWL_PITCH_START = 0.0f;
    private static final float CRAWL_PITCH_END = 0.0f;
    private static final float CLIMB_PITCH_START = 0.0f;
    private static final float CLIMB_PITCH_END = 0.0f;
    private static final float ARM_ROTATION_RESET_ENTER_STEP_MIN = 0.1f;
    private static final float ARM_ROTATION_RESET_ENTER_STEP_MAX = 0.16f;
    private static final float ARM_ROTATION_RESET_EXIT_STEP_MIN = 0.08f;
    private static final float ARM_ROTATION_RESET_BACK_MIX = 0.45f;
    private static final class_1799 DEBUG_PIVOT_STACK = new class_1799(class_1802.field_8197);
    private static final class_1799 DEBUG_GRIP_STACK = new class_1799(class_1802.field_8737);
    private static final float ARM_ROTATION_RESET_EXIT_STEP_MAX = 0.13f;
    private static final Vector3f DEBUG_PIVOT_NUDGE = new Vector3f(0.0f, 0.0f, ARM_ROTATION_RESET_EXIT_STEP_MAX);
    private static final class_2960 FREEZE_ARM_STAGE_1 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_freeze_tip_1.png");
    private static final class_2960 FREEZE_ARM_STAGE_2 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_freeze_tip_2.png");
    private static final class_2960 FREEZE_ARM_STAGE_3 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_freeze_tip_3.png");
    private static final class_2960 MUD_ARM_STAGE_1 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_mud_tip_1.png");
    private static final class_2960 MUD_ARM_STAGE_2 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_mud_tip_2.png");
    private static final class_2960 MUD_ARM_STAGE_3 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_mud_tip_3.png");
    private static final class_2960 SWEAT_ARM_STAGE_1 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_sweat_tip_1.png");
    private static final class_2960 SWEAT_ARM_STAGE_2 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_sweat_tip_2.png");
    private static final class_2960 SWEAT_ARM_STAGE_3 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_sweat_tip_3.png");
    private static final class_2960 SWEAT_ARM_STAGE_4 = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_sweat_tip_4.png");
    private static final class_2960 BURN_ARM_HAND_OVERLAY = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_burn_hand_overlay.png");
    private static final class_2960 FIRE_ATLAS_ID = class_1059.field_5275;
    private static final class_2960 FIRE_ATLAS_TEXTURE = class_1059.field_5275;
    private static final class_2960 FIRE_SPRITE_ID = class_2960.method_60655("minecraft", "block/fire_1");
    private static final class_2960 BURN_ARM_HAND_OVERLAY_LEGACY = class_2960.method_60655(Punchy.MOD_ID, "textures/misc/arm_burn_hand_overlay_legacy.png");
    private static final Map<String, FreezeVoxelMask> FREEZE_MASK_CACHE = new HashMap();
    private static final Map<class_2960, Integer> SKIN_HEIGHT_CACHE = new HashMap();
    private static int offhandLowerExitDelayFrames = 0;
    private static int offhandHideAfterLowerFrames = 0;
    private static boolean lastOffhandEmptyForLower = true;
    private static int offhandAwaitHandInFrames = 0;
    private static boolean lastOffhandHasItemForLower = false;
    private static int offhandAwaitHandOutFrames = 0;
    private static boolean lastDualHandedForHide = false;
    private static boolean hideOffhandAfterDualExit = false;
    private static int hideOffhandAfterDualFrames = 0;
    private static boolean keepEmptyOffhandVisibleForFlight = false;
    private static boolean flightOffhandLandingHideRequested = false;
    private static final EnumMap<class_1306, Matrix4f> ARM_WORLD_CACHE = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Float> DRY_OUT_ARM_TUNING_BLEND = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Float> ARM_ROTATION_RESET_PROGRESS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, BurnFadeState> BURN_FADE_STATE = new EnumMap<>(class_1306.class);
    private static long lastArmCacheTick = -1;
    private static final ThreadLocal<Boolean> FORCE_VANILLA_DISPLAY = ThreadLocal.withInitial(() -> {
        return false;
    });
    private static final ThreadLocal<Boolean> NEUTRALIZE_USE_TRANSFORMS = ThreadLocal.withInitial(() -> {
        return false;
    });
    private static final class_6862<class_1792> SPEAR_TAG = class_6862.method_40092(class_7924.field_41197, class_2960.method_60655("minecraft", "spears"));
    private static final Map<class_1792, EnumMap<class_811, class_804>> VANILLA_THIRD_PERSON_CACHE = new HashMap();
    private static final Map<class_1792, EnumMap<class_811, class_804>> IDLE_USE_TRANSFORM_CACHE = new HashMap();
    private static final ThreadLocal<class_804> BASELINE_USE_TRANSFORM = new ThreadLocal<>();

    public static boolean shouldForceVanillaDisplay() {
        return FORCE_VANILLA_DISPLAY.get().booleanValue();
    }

    private static void setForceVanillaDisplay(boolean value) {
        FORCE_VANILLA_DISPLAY.set(Boolean.valueOf(value));
    }

    public static boolean shouldNeutralizeUseTransforms() {
        return NEUTRALIZE_USE_TRANSFORMS.get().booleanValue();
    }

    private static void setNeutralizeUseTransforms(boolean value) {
        NEUTRALIZE_USE_TRANSFORMS.set(Boolean.valueOf(value));
    }

    private static float updateWaterArmTuningBlend(class_1306 arm, float partialTicks) {
        float blend = ((Float) DRY_OUT_ARM_TUNING_BLEND.getOrDefault(arm, Float.valueOf(1.0f))).floatValue();
        float dt = class_3532.method_15363(partialTicks, 0.0f, 1.0f);
        float target = 1.0f;
        float lerpAlpha = 0.35f + (0.25f * dt);
        boolean waterAnimationActive = false;
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            AnimationClip swimClip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
            waterAnimationActive = swimClip == PunchyAnimationManager.SWIMING_CLIP;
            if (waterAnimationActive && arm != null && swimClip != null) {
                boolean suppressWaterTuningFade = ItemAnimationManager.isSwimSuppressed(arm) || BedrockItemAnimationManager.isSwimSuppressed(arm);
                if (suppressWaterTuningFade) {
                    waterAnimationActive = false;
                }
            }
        }
        if (waterAnimationActive) {
            target = 0.0f;
            lerpAlpha = 0.5f + (0.3f * dt);
        }
        float blend2 = class_3532.method_16439(class_3532.method_15363(lerpAlpha, 0.0f, 1.0f), blend, target);
        if (Math.abs(blend2 - target) < 0.001f) {
            blend2 = target;
        }
        DRY_OUT_ARM_TUNING_BLEND.put(arm, Float.valueOf(blend2));
        return blend2;
    }

    private static float updateArmRotationResetBlend(class_1306 arm, float partialTicks) {
        float progress;
        float progress2 = ((Float) ARM_ROTATION_RESET_PROGRESS.getOrDefault(arm, Float.valueOf(0.0f))).floatValue();
        float clampedPartial = class_3532.method_15363(partialTicks, 0.0f, 1.0f);
        if (shouldZeroArmRotationForActiveAnimation()) {
            float frameStep = class_3532.method_16439(clampedPartial, ARM_ROTATION_RESET_ENTER_STEP_MIN, ARM_ROTATION_RESET_ENTER_STEP_MAX);
            progress = class_3532.method_15363(progress2 + frameStep, 0.0f, 1.0f);
        } else {
            float frameStep2 = class_3532.method_16439(clampedPartial, ARM_ROTATION_RESET_EXIT_STEP_MIN, ARM_ROTATION_RESET_EXIT_STEP_MAX);
            progress = class_3532.method_15363(progress2 - frameStep2, 0.0f, 1.0f);
        }
        ARM_ROTATION_RESET_PROGRESS.put(arm, Float.valueOf(progress));
        float smooth = progress * progress * (3.0f - (2.0f * progress));
        float back = class_3532.method_15363(Easing.easeOutBack(progress), 0.0f, 1.0f);
        float eased = class_3532.method_16439(0.45f, smooth, back);
        return 1.0f - eased;
    }

    private static boolean shouldZeroArmRotationForActiveAnimation() {
        return shouldZeroArmRotationForClip(PunchyAnimationManager.POSE_HANDLER.getCurrentClip(), PunchyAnimationManager.POSE_HANDLER.isPlaying()) || shouldZeroArmRotationForClip(PunchyAnimationManager.SWIM_HANDLER.getCurrentClip(), PunchyAnimationManager.SWIM_HANDLER.isPlaying());
    }

    private static float resolveGlobalHandFactorForFlightPose() {
        MovementStateMachine movement = MovementStateMachine.get();
        if (movement == null || !movement.isFlightPoseActive()) {
            return 1.0f;
        }
        float easedProgress = class_3532.method_15363(movement.getFlightPoseBlend() * 1.12f, 0.0f, 1.0f);
        float eased = class_3532.method_15363(Easing.easeOutBack(easedProgress), 0.0f, 1.0f);
        return 1.0f - eased;
    }

    private static float resolveFlightPoseTuningFactor(float partialTicks, class_1306 arm) {
        float tuningFactor = PunchyAnimationManager.POSE_HANDLER.getArmTuningFactor(partialTicks, arm);
        float globalHandFactor = resolveGlobalHandFactorForFlightPose();
        if (globalHandFactor < 0.999f) {
            tuningFactor *= globalHandFactor;
        }
        return tuningFactor;
    }

    private static boolean isActualFlightActive(class_746 player) {
        if (player == null || !PunchyConfig.enableElytraFlightAnimation() || player.method_24828() || player.method_5681() || player.method_5799() || !player.method_6128()) {
            return false;
        }
        class_243 velocity = player.method_18798();
        float speed = velocity == null ? 0.0f : (float) velocity.method_1033();
        return speed >= ELYTRA_OFFHAND_SHOW_SPEED_MIN;
    }

    private static boolean hasEquippedElytra(class_746 player) {
        class_1799 chest;
        return (player == null || (chest = player.method_6118(class_1304.field_6174)) == null || !chest.method_31574(class_1802.field_8833)) ? false : true;
    }

    private static void resetFlightOffhandState() {
        keepEmptyOffhandVisibleForFlight = false;
        flightOffhandLandingHideRequested = false;
    }

    private static boolean shouldZeroArmRotationForClip(AnimationClip clip, boolean playing) {
        if (!playing || clip == null) {
            return false;
        }
        return clip == PunchyAnimationManager.FALLING_IN_WATER_CLIP || clip == PunchyAnimationManager.PUT_OUT_FIRE_CLIP || PunchyAnimationManager.isClipNamed(clip, "crawl") || PunchyAnimationManager.isClipNamed(clip, "climb");
    }

    public static class_804 getCachedVanillaThirdPersonTransform(class_1799 stack, class_811 context) {
        class_1792 item;
        EnumMap<class_811, class_804> perContext;
        class_804 cached;
        if (stack == null || stack.method_7960() || context == null || (item = stack.method_7909()) == null || (perContext = VANILLA_THIRD_PERSON_CACHE.get(item)) == null || (cached = perContext.get(context)) == null) {
            return null;
        }
        return cloneTransform(cached);
    }

    public static void cacheVanillaThirdPersonTransform(class_1799 stack, class_811 context, class_804 transform) {
        class_1792 item;
        if (stack == null || stack.method_7960() || context == null || transform == null || (item = stack.method_7909()) == null) {
            return;
        }
        EnumMap<class_811, class_804> perContext = VANILLA_THIRD_PERSON_CACHE.computeIfAbsent(item, key -> {
            return new EnumMap(class_811.class);
        });
        perContext.put(context, cloneTransform(transform));
    }

    private static class_804 cloneTransform(class_804 transform) {
        return transform;
    }

    public static class_804 getBaselineUseTransform() {
        return BASELINE_USE_TRANSFORM.get();
    }

    public static void setBaselineUseTransform(class_804 transform) {
        BASELINE_USE_TRANSFORM.set(transform);
    }

    public static void cacheIdleUseTransform(class_1799 stack, class_811 context, class_804 transform) {
        class_1792 item;
        if (stack == null || stack.method_7960() || context == null || transform == null || (item = stack.method_7909()) == null) {
            return;
        }
        EnumMap<class_811, class_804> perContext = IDLE_USE_TRANSFORM_CACHE.computeIfAbsent(item, key -> {
            return new EnumMap(class_811.class);
        });
        perContext.put(context, cloneTransform(transform));
    }

    public static class_804 getCachedIdleTransform(class_1799 stack, class_811 context) {
        class_1792 item;
        EnumMap<class_811, class_804> perContext;
        class_804 cached;
        if (stack == null || stack.method_7960() || context == null || (item = stack.method_7909()) == null || (perContext = IDLE_USE_TRANSFORM_CACHE.get(item)) == null || (cached = perContext.get(context)) == null) {
            return null;
        }
        return cloneTransform(cached);
    }

    public static class_804 resolveBaselineUseTransform(class_1799 stack, class_811 context, class_1309 entity) {
        class_10442 resolver;
        class_804 transform;
        if (stack == null || stack.method_7960() || context == null) {
            return null;
        }
        class_804 resourcePackTransform = ResourcePackItemTransformResolver.resolve(stack, context);
        if (resourcePackTransform != null) {
            return resourcePackTransform;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null || (resolver = mc.method_65386()) == null || entity == null) {
            return null;
        }
        ItemStackRenderStateAccessor class_10444Var = new class_10444();
        boolean prevForced = UseAnimationSuppressor.isForced();
        UseAnimationSuppressor.force(true);
        try {
            resolver.method_65597(class_10444Var, stack, context, entity);
            UseAnimationSuppressor.force(prevForced);
            ItemStackRenderStateAccessor accessor = class_10444Var;
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
        } catch (Throwable th) {
            UseAnimationSuppressor.force(prevForced);
            throw th;
        }
    }

    public static Matrix4f getCachedArmWorld(class_746 player, class_1306 arm) {
        Matrix4f cached;
        if (player == null || arm == null || player.method_37908() == null || lastArmCacheTick != player.method_37908().method_8510() || (cached = ARM_WORLD_CACHE.get(arm)) == null) {
            return null;
        }
        return new Matrix4f(cached);
    }

    public static void renderFirstPerson(class_759 handRenderer, class_746 player, float partialTicks, class_4587 poseStack, class_4597 buffer, int combinedLight) {
        EmfFirstPersonHandCompat.Scope ignored = EmfFirstPersonHandCompat.enter(player);
        try {
            renderFirstPersonInternal(handRenderer, player, partialTicks, poseStack, buffer, combinedLight);
            if (ignored != null) {
                ignored.close();
            }
        } catch (Throwable th) {
            if (ignored != null) {
                try {
                    ignored.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void renderFirstPersonInternal(class_759 handRenderer, class_746 player, float partialTicks, class_4587 poseStack, class_4597 buffer, int combinedLight) throws MatchException {
        if (player == null || handRenderer == null || !PunchyConfig.isModEnabled()) {
            return;
        }
        MovementStateMachine.get().updateFrame(player, partialTicks);
        if (!PunchyConfig.disableNativeItemPhysics()) {
            PendulumPhysics.get().updateFrame(player, partialTicks);
            SlimeInHandPhysics.get().updateFrame(player);
        }
        class_310 mc = class_310.method_1551();
        if (!mc.field_1690.method_31044().method_31034() || FIRSTPERSON_HIDE || FirstPersonModelCompat.shouldHideHands()) {
            return;
        }
        class_591 playerModel = getFirstPersonModel(isSlim(player));
        if (SimpleBedrockGeo.loadedParts.isEmpty() || (!SimpleBedrockGeo.loadedParts.containsKey("camera_animation") && !SimpleBedrockGeo.loadedParts.containsKey("camera"))) {
            class_2960 playerGeoId = class_2960.method_60655(Punchy.MOD_ID, "geo/player.geo.json");
            SimpleBedrockGeo.load(playerGeoId);
        }
        poseStack.method_22903();
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(180.0f));
        applyCameraAnimationCompensation(poseStack);
        resetPlayerModelPose(playerModel);
        playerModel.field_3391.field_3665 = false;
        playerModel.field_3398.field_3665 = false;
        playerModel.field_3394.field_3665 = false;
        playerModel.field_3397.field_3665 = false;
        playerModel.field_3392.field_3665 = false;
        playerModel.field_3483.field_3665 = false;
        playerModel.field_3482.field_3665 = false;
        playerModel.field_3479.field_3665 = false;
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        class_1306 offArm = mainRight ? class_1306.field_6182 : class_1306.field_6183;
        boolean hasOffhandItem = !player.method_6079().method_7960();
        boolean offhandOverrideActive = PunchyAnimationManager.isEquipOverrideActive(offArm);
        boolean offhandOverrideRenderable = PunchyAnimationManager.hasRenderableEquipOverride(offArm);
        boolean offhandEquipTransitionActive = HandEquipStateMachine.isOffhandEquipTransitionActive();
        boolean effectiveOffhandVisualActive = hasOffhandItem || offhandOverrideActive || offhandEquipTransitionActive;
        boolean currentOffhandEmpty = !effectiveOffhandVisualActive;
        MovementStateMachine.get().isFlightPoseActive();
        boolean actualFlightActive = isActualFlightActive(player);
        boolean elytraEquipped = hasEquippedElytra(player);
        if (!elytraEquipped && !actualFlightActive) {
            resetFlightOffhandState();
        }
        if (!currentOffhandEmpty) {
            resetFlightOffhandState();
        } else if (actualFlightActive) {
            keepEmptyOffhandVisibleForFlight = true;
            flightOffhandLandingHideRequested = false;
        } else if (keepEmptyOffhandVisibleForFlight && player.method_24828() && !flightOffhandLandingHideRequested) {
            HandEquipStateMachine.requestFlightOffhandLandingHide(player);
            keepEmptyOffhandVisibleForFlight = false;
            flightOffhandLandingHideRequested = true;
        } else if (!keepEmptyOffhandVisibleForFlight) {
            flightOffhandLandingHideRequested = false;
        }
        AnimationClip activeClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        boolean exactHandOutActive = PunchyAnimationManager.POSE_HANDLER.isPlaying() && activeClip != null && (PunchyAnimationManager.isClipNamed(activeClip, "hand_out") || (activeClip.getName() != null && activeClip.getName().startsWith("hand_out_")));
        boolean handInStateActive = "hand_in".equals(HandEquipStateMachine.getActiveType());
        boolean handOutActive = HandEquipStateMachine.shouldTreatHandOutActive();
        boolean offhandRemovalHandOutActive = HandEquipStateMachine.shouldKeepOffhandVisibleUntilHandOutEnds();
        boolean hideIncomingOffhandBeforeHandIn = HandEquipStateMachine.shouldHideIncomingOffhandUntilHandInStarts(player);
        boolean dualEffectActive = PunchyAnimationManager.POSE_HANDLER.isCurrentDualHandedActive() || ItemAnimationManager.isAnyDualHandedActive() || BedrockItemAnimationManager.isAnyDualHandedActive();
        boolean dualHanded = dualEffectActive || PunchyAnimationManager.isSwimDualHandedActive();
        boolean hideOffhandIfEmpty = PunchyAnimationManager.isOffhandHiddenIfEmptyActive(partialTicks);
        boolean suppressOffhand = PunchyClient.shouldSuppressOffhandRender(player);
        if (dualHanded) {
            suppressOffhand = false;
        }
        boolean keepEmptyOffhandVisibleAfterFlight = flightOffhandLandingHideRequested && (HandEquipStateMachine.shouldKeepOffhandVisibleUntilHandOutEnds() || HandEquipStateMachine.isOffhandEquipTransitionActive());
        boolean hideEmptyOffhandForFlightLanding = flightOffhandLandingHideRequested && !actualFlightActive && player.method_24828() && currentOffhandEmpty;
        boolean keepFlightPoseOffhandVisible = (actualFlightActive || (keepEmptyOffhandVisibleForFlight && !player.method_24828())) && !hideEmptyOffhandForFlightLanding;
        if (keepEmptyOffhandVisibleForFlight && !actualFlightActive && player.method_24828() && !keepEmptyOffhandVisibleAfterFlight) {
            resetFlightOffhandState();
        }
        boolean renderOffhandArm = (effectiveOffhandVisualActive || keepFlightPoseOffhandVisible || keepEmptyOffhandVisibleAfterFlight) && !suppressOffhand;
        boolean renderOffhandItem = (hasOffhandItem || offhandOverrideRenderable) && !suppressOffhand;
        boolean renderSuppressedOffhand = suppressOffhand && currentOffhandEmpty && !offhandOverrideRenderable;
        if (hideEmptyOffhandForFlightLanding) {
            renderOffhandArm = false;
            renderOffhandItem = false;
        }
        boolean offhandInUse = player.method_6115() && player.method_6058() == resolveHand(player, offArm);
        boolean usingSpyglass = isUsingSpyglass(player);
        boolean anyAnimationPlaying = PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.SWIM_HANDLER.isPlaying();
        boolean keepEmptyOffhandVisibleForSprint = (!currentOffhandEmpty || !MovementStateMachine.get().isSprintSwingActive() || dualHanded || offhandInUse || handOutActive || handInStateActive || keepFlightPoseOffhandVisible || keepEmptyOffhandVisibleAfterFlight || hideEmptyOffhandForFlightLanding) ? false : true;
        if (lastDualHandedForHide && !dualHanded && currentOffhandEmpty) {
            hideOffhandAfterDualExit = true;
            hideOffhandAfterDualFrames = 3;
        }
        if (dualHanded) {
            hideOffhandAfterDualExit = false;
            hideOffhandAfterDualFrames = 0;
        } else if (hideOffhandAfterDualFrames > 0) {
            hideOffhandAfterDualFrames--;
        } else {
            hideOffhandAfterDualExit = false;
        }
        boolean hideOffhandOnDualExit = !dualHanded && (hideOffhandAfterDualExit || hideOffhandAfterDualFrames > 0);
        if (lastOffhandEmptyForLower && !currentOffhandEmpty) {
            offhandAwaitHandInFrames = 5;
        }
        lastOffhandEmptyForLower = currentOffhandEmpty;
        if (lastOffhandHasItemForLower && currentOffhandEmpty) {
            offhandAwaitHandOutFrames = 5;
        }
        lastOffhandHasItemForLower = !currentOffhandEmpty;
        if (anyAnimationPlaying) {
            offhandAwaitHandInFrames = 0;
            offhandAwaitHandOutFrames = 0;
        } else if (offhandAwaitHandInFrames > 0) {
            offhandAwaitHandInFrames--;
        } else if (offhandAwaitHandOutFrames > 0) {
            offhandAwaitHandOutFrames--;
        }
        boolean baseLowerOffhand = (!currentOffhandEmpty || dualHanded || offhandInUse || anyAnimationPlaying) ? false : true;
        if (!currentOffhandEmpty) {
            offhandLowerExitDelayFrames = 0;
        } else if (baseLowerOffhand) {
            offhandLowerExitDelayFrames = 3;
        } else if (!anyAnimationPlaying && offhandLowerExitDelayFrames > 0) {
            offhandLowerExitDelayFrames--;
        }
        boolean awaitingHandIn = (currentOffhandEmpty || offhandAwaitHandInFrames <= 0 || anyAnimationPlaying) ? false : true;
        boolean awaitingHandOut = currentOffhandEmpty && offhandAwaitHandOutFrames > 0 && !anyAnimationPlaying;
        boolean keepEmptyOffhandVisibleForHandOut = currentOffhandEmpty && !dualHanded && (exactHandOutActive || offhandRemovalHandOutActive);
        boolean effectiveRenderSuppressedOffhand = renderSuppressedOffhand && !keepEmptyOffhandVisibleForHandOut;
        boolean lowerOffhand = (hideEmptyOffhandForFlightLanding || keepFlightPoseOffhandVisible || keepEmptyOffhandVisibleAfterFlight || keepEmptyOffhandVisibleForSprint || handOutActive || handInStateActive || dualHanded || anyAnimationPlaying || (!baseLowerOffhand && offhandLowerExitDelayFrames <= 0 && !awaitingHandIn && !awaitingHandOut)) ? false : true;
        boolean loweredOffhandActive = currentOffhandEmpty && (effectiveRenderSuppressedOffhand || lowerOffhand);
        boolean hideLoweredOffhandTransiently = currentOffhandEmpty && !keepEmptyOffhandVisibleForHandOut && (effectiveRenderSuppressedOffhand || awaitingHandIn || awaitingHandOut || hideOffhandOnDualExit);
        if (!currentOffhandEmpty) {
            offhandHideAfterLowerFrames = 0;
        } else if (hideLoweredOffhandTransiently) {
            offhandHideAfterLowerFrames = OFFHAND_HIDE_AFTER_LOWER_FRAMES;
        } else if (loweredOffhandActive) {
            offhandHideAfterLowerFrames = Math.min(OFFHAND_HIDE_AFTER_LOWER_FRAMES, offhandHideAfterLowerFrames + 1);
        }
        boolean hideLoweredOffhandArm = loweredOffhandActive && (hideLoweredOffhandTransiently || offhandHideAfterLowerFrames >= OFFHAND_HIDE_AFTER_LOWER_FRAMES);
        if (effectiveRenderSuppressedOffhand || lowerOffhand) {
            renderOffhandArm = keepEmptyOffhandVisibleForHandOut || !hideLoweredOffhandArm;
            renderOffhandItem = false;
        }
        if (keepEmptyOffhandVisibleForSprint) {
            renderOffhandArm = true;
            renderOffhandItem = false;
        }
        if (hideEmptyOffhandForFlightLanding) {
            renderOffhandArm = false;
            renderOffhandItem = false;
        }
        boolean renderRightArm = mainRight || (offArm == class_1306.field_6183 && renderOffhandArm);
        boolean renderLeftArm = !mainRight || (offArm == class_1306.field_6182 && renderOffhandArm);
        boolean renderRightItem = mainRight || (offArm == class_1306.field_6183 && renderOffhandItem);
        boolean renderLeftItem = !mainRight || (offArm == class_1306.field_6182 && renderOffhandItem);
        boolean vanillaMainWindow = HandEquipStateMachine.shouldAllowVanillaHandOut(class_1268.field_5808);
        boolean vanillaOffWindow = HandEquipStateMachine.shouldAllowVanillaHandOut(class_1268.field_5810);
        if (vanillaMainWindow) {
            if (mainRight) {
                renderRightArm = false;
                renderRightItem = false;
            } else {
                renderLeftArm = false;
                renderLeftItem = false;
            }
        }
        if (vanillaOffWindow) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = false;
                renderRightItem = false;
            } else {
                renderLeftArm = false;
                renderLeftItem = false;
            }
        }
        boolean disableRightHand = isBlacklistedForArm(player, class_1306.field_6183);
        boolean disableLeftHand = isBlacklistedForArm(player, class_1306.field_6182);
        if (disableRightHand) {
            renderRightArm = false;
            renderRightItem = false;
        }
        if (disableLeftHand) {
            renderLeftArm = false;
            renderLeftItem = false;
        }
        if (hideOffhandOnDualExit) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = mainRight;
                renderRightItem = mainRight;
            } else {
                renderLeftArm = !mainRight;
                renderLeftItem = !mainRight;
            }
        }
        boolean keepOffhandVisibleForSwim = false;
        if (currentOffhandEmpty && !hideEmptyOffhandForFlightLanding && !keepFlightPoseOffhandVisible && !keepEmptyOffhandVisibleAfterFlight && !dualHanded && !keepEmptyOffhandVisibleForHandOut) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = mainRight;
                renderRightItem = mainRight;
            } else {
                renderLeftArm = !mainRight;
                renderLeftItem = !mainRight;
            }
        }
        if (!hideEmptyOffhandForFlightLanding && !keepFlightPoseOffhandVisible && !dualHanded && hideIncomingOffhandBeforeHandIn) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = mainRight;
                renderRightItem = mainRight;
            } else {
                renderLeftArm = !mainRight;
                renderLeftItem = !mainRight;
            }
        }
        lastDualHandedForHide = dualHanded;
        if (usingSpyglass) {
            renderRightArm = false;
            renderLeftArm = false;
            renderRightItem = false;
            renderLeftItem = false;
        }
        if (!suppressOffhand && PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            class_1306 swimArm = PunchyAnimationManager.SWIM_HANDLER.getActiveArm();
            if (swimArm == offArm) {
                keepOffhandVisibleForSwim = true;
                renderRightArm = mainRight || (offArm == class_1306.field_6183 && 1 != 0);
                renderLeftArm = !mainRight || (offArm == class_1306.field_6182 && 1 != 0);
                renderRightItem = mainRight || (offArm == class_1306.field_6183 && 1 != 0);
                renderLeftItem = !mainRight || (offArm == class_1306.field_6182 && 1 != 0);
            }
        }
        if (dualHanded) {
            renderRightArm = true;
            renderLeftArm = true;
            if (!suppressOffhand) {
                renderRightItem = true;
                renderLeftItem = true;
            }
        }
        if (hideOffhandIfEmpty) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = mainRight;
                renderRightItem = mainRight;
            } else {
                renderLeftArm = !mainRight;
                renderLeftItem = !mainRight;
            }
        }
        boolean keepRightArmForDualHanded = PunchyAnimationManager.shouldKeepBlacklistedOppositeHandForDualHanded(mc, resolveHand(player, class_1306.field_6183));
        boolean keepLeftArmForDualHanded = PunchyAnimationManager.shouldKeepBlacklistedOppositeHandForDualHanded(mc, resolveHand(player, class_1306.field_6182));
        boolean forceShowRightItem = PunchyAnimationManager.isItemForceShown(class_1306.field_6183);
        boolean forceShowLeftItem = PunchyAnimationManager.isItemForceShown(class_1306.field_6182);
        if (disableRightHand) {
            renderRightArm = keepRightArmForDualHanded;
            renderRightItem = keepRightArmForDualHanded && forceShowRightItem;
        }
        if (disableLeftHand) {
            renderLeftArm = keepLeftArmForDualHanded;
            renderLeftItem = keepLeftArmForDualHanded && forceShowLeftItem;
        }
        if (hideEmptyOffhandForFlightLanding) {
            if (offArm == class_1306.field_6183) {
                renderRightArm = mainRight;
                renderRightItem = mainRight;
            } else {
                renderLeftArm = !mainRight;
                renderLeftItem = !mainRight;
            }
        }
        if (currentOffhandEmpty && keepOffhandVisibleForSwim) {
            renderRightArm = mainRight || (offArm == class_1306.field_6183 && 1 != 0);
            renderLeftArm = !mainRight || (offArm == class_1306.field_6182 && 1 != 0);
            renderRightItem = mainRight || (offArm == class_1306.field_6183 && 1 != 0);
            renderLeftItem = !mainRight || (offArm == class_1306.field_6182 && 1 != 0);
        }
        boolean disableRightHand2 = isBlacklistedForArm(player, class_1306.field_6183);
        boolean disableLeftHand2 = isBlacklistedForArm(player, class_1306.field_6182);
        if (disableRightHand2) {
            renderRightArm = false;
            renderRightItem = false;
        }
        if (disableLeftHand2) {
            renderLeftArm = false;
            renderLeftItem = false;
        }
        playerModel.field_3401.field_3665 = renderRightArm;
        playerModel.field_27433.field_3665 = renderLeftArm;
        playerModel.field_3486.field_3665 = false;
        playerModel.field_3484.field_3665 = false;
        class_630 cameraAnimPart = SimpleBedrockGeo.loadedParts.get("camera_animation");
        class_630 cameraPart = SimpleBedrockGeo.loadedParts.get("camera");
        if (cameraAnimPart == null) {
            cameraAnimPart = cameraPart;
        }
        class_630 bodyPart = SimpleBedrockGeo.loadedParts.get("body");
        class_630 geoRightArm = SimpleBedrockGeo.loadedParts.get("right_arm");
        class_630 geoLeftArm = SimpleBedrockGeo.loadedParts.get("left_arm");
        class_630 geoRightItem = SimpleBedrockGeo.loadedParts.get("item_right");
        class_630 geoLeftItem = SimpleBedrockGeo.loadedParts.get("item_left");
        class_630 geoRightGrip = SimpleBedrockGeo.loadedParts.get("itemgrip_right");
        class_630 geoLeftGrip = SimpleBedrockGeo.loadedParts.get("itemgrip_left");
        if (cameraAnimPart != null) {
            cameraAnimPart.method_41923();
        }
        if (cameraPart != null) {
            cameraPart.method_41923();
        }
        if (bodyPart != null) {
            bodyPart.method_41923();
        }
        if (geoRightArm != null) {
            geoRightArm.method_41923();
        }
        if (geoLeftArm != null) {
            geoLeftArm.method_41923();
        }
        if (geoRightItem != null) {
            geoRightItem.method_41923();
        }
        if (geoLeftItem != null) {
            geoLeftItem.method_41923();
        }
        if (geoRightGrip != null) {
            geoRightGrip.method_41923();
        }
        if (geoLeftGrip != null) {
            geoLeftGrip.method_41923();
        }
        Map<String, class_630> geoMap = new HashMap<>();
        if (cameraAnimPart != null) {
            geoMap.put("camera_animation", cameraAnimPart);
        }
        if (cameraPart != null) {
            geoMap.put("camera", cameraPart);
        }
        if (bodyPart != null) {
            geoMap.put("body", bodyPart);
        }
        if (geoRightArm != null) {
            geoMap.put("right_arm", geoRightArm);
        }
        if (geoLeftArm != null) {
            geoMap.put("left_arm", geoLeftArm);
        }
        if (geoRightItem != null) {
            geoMap.put("item_right", geoRightItem);
        }
        if (geoLeftItem != null) {
            geoMap.put("item_left", geoLeftItem);
        }
        if (geoRightGrip != null) {
            geoMap.put("itemgrip_right", geoRightGrip);
        }
        if (geoLeftGrip != null) {
            geoMap.put("itemgrip_left", geoLeftGrip);
        }
        PunchyAnimationManager.POSE_HANDLER.applyTransformations(geoMap, partialTicks);
        if (shouldApplySwimTransforms()) {
            PunchyAnimationManager.SWIM_HANDLER.applyTransformations(geoMap, partialTicks, false);
        }
        CameraAnimationState.update(cameraPart);
        Matrix4f cameraLocal = getLocalMatrix(cameraPart);
        Matrix4f bodyLocal = getLocalMatrix(bodyPart);
        Matrix4f rootWorld = new Matrix4f();
        if (cameraLocal != null) {
            rootWorld.set(cameraLocal);
        }
        if (bodyLocal != null) {
            rootWorld.mul(bodyLocal);
        }
        applyCrawlCameraDecouple(rootWorld, player);
        applyCameraLookOffset(rootWorld);
        applyGeoArmToPlayer(playerModel.field_3401, geoRightArm, rootWorld, player, class_1306.field_6183, partialTicks);
        applyGeoArmToPlayer(playerModel.field_27433, geoLeftArm, rootWorld, player, class_1306.field_6182, partialTicks);
        if ((effectiveRenderSuppressedOffhand || lowerOffhand) && !hideLoweredOffhandArm) {
            applyLoweredOffhandPose(player, playerModel, offArm);
        }
        clearExplicitTransform(playerModel.field_3486);
        clearExplicitTransform(playerModel.field_3484);
        playerModel.field_3486.field_3665 = renderRightArm;
        playerModel.field_3484.field_3665 = renderLeftArm;
        if (PunchyDebug.DEBUG_POSITION_ENABLED) {
            poseStack.method_46416(PunchyDebug.POS_X * 0.0625f, PunchyDebug.POS_Y * 0.0625f, PunchyDebug.POS_Z * 0.0625f);
            poseStack.method_22907(class_7833.field_40714.rotationDegrees(PunchyDebug.ROT_X));
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(PunchyDebug.ROT_Y));
            poseStack.method_22907(class_7833.field_40718.rotationDegrees(PunchyDebug.ROT_Z));
        }
        class_2960 texture = getPlayerTexture(player);
        boolean slim = isSlim(player);
        SkinLayersCompat.applyFirstPersonSleeves(player, playerModel, slim, renderLeftArm, renderRightArm);
        if (renderLeftArm) {
            renderArm(playerModel, player, class_1306.field_6182, poseStack, buffer, combinedLight, texture, slim, partialTicks);
        }
        if (renderRightArm) {
            renderArm(playerModel, player, class_1306.field_6183, poseStack, buffer, combinedLight, texture, slim, partialTicks);
        }
        if (renderLeftItem) {
            renderItemInHand(handRenderer, playerModel, player, class_1306.field_6182, poseStack, buffer, combinedLight, rootWorld, geoLeftArm, geoLeftItem, geoLeftGrip, partialTicks);
        }
        if (renderRightItem) {
            renderItemInHand(handRenderer, playerModel, player, class_1306.field_6183, poseStack, buffer, combinedLight, rootWorld, geoRightArm, geoRightItem, geoRightGrip, partialTicks);
        }
        poseStack.method_22909();
    }

    public static void render(class_742 player, float partialTicks, class_4587 poseStack, class_4597 buffer, int combinedLight, class_1306 arm) {
    }

    public static void renderPlayerArm(class_742 player, float partialTicks, class_4587 poseStack, class_4597 buffer, int combinedLight, class_1306 arm) {
        class_310 mc = class_310.method_1551();
        class_922<?, ?, ?> class_922VarMethod_3953 = mc.method_1561().method_3953(player);
        if (!(class_922VarMethod_3953 instanceof class_922)) {
            return;
        }
        class_922<?, ?, ?> livingRenderer = class_922VarMethod_3953;
        Object modelObj = livingRenderer.method_4038();
        if (modelObj instanceof class_591) {
            class_591 playerModel = (class_591) modelObj;
            poseStack.method_22903();
            if (PunchyDebug.DEBUG_POSITION_ENABLED) {
                poseStack.method_46416(PunchyDebug.POS_X * 0.0625f, PunchyDebug.POS_Y * 0.0625f, PunchyDebug.POS_Z * 0.0625f);
            }
            playerModel.field_3401.method_41923();
            playerModel.field_27433.method_41923();
            playerModel.field_3486.method_41923();
            playerModel.field_3484.method_41923();
            playerModel.field_3401.field_3665 = arm == class_1306.field_6183;
            playerModel.field_27433.field_3665 = arm == class_1306.field_6182;
            playerModel.field_3486.field_3665 = arm == class_1306.field_6183;
            playerModel.field_3484.field_3665 = arm == class_1306.field_6182;
            Map<String, class_630> partsMap = new HashMap<>();
            partsMap.put("right_arm", playerModel.field_3401);
            partsMap.put("left_arm", playerModel.field_27433);
            PunchyAnimationManager.POSE_HANDLER.applyTransformations(partsMap, partialTicks);
            if (shouldApplySwimTransforms()) {
                PunchyAnimationManager.SWIM_HANDLER.applyTransformations(partsMap, partialTicks, false);
            }
            copyMatrixToSleeve(playerModel.field_3401, playerModel.field_3486);
            copyMatrixToSleeve(playerModel.field_27433, playerModel.field_3484);
            boolean slim = isSlim(player);
            SkinLayersCompat.applyFirstPersonSleeves(player, playerModel, slim, arm == class_1306.field_6182, arm == class_1306.field_6183);
            class_2960 texture = getPlayerTexture(player);
            if (arm == class_1306.field_6183) {
                class_4588 consumer = buffer.getBuffer(class_1921.method_23580(texture));
                playerModel.field_3401.method_22698(poseStack, consumer, combinedLight, class_4608.field_21444);
                playerModel.field_3486.method_22698(poseStack, consumer, combinedLight, class_4608.field_21444);
            } else {
                class_4588 consumer2 = buffer.getBuffer(class_1921.method_23580(texture));
                playerModel.field_27433.method_22698(poseStack, consumer2, combinedLight, class_4608.field_21444);
                playerModel.field_3484.method_22698(poseStack, consumer2, combinedLight, class_4608.field_21444);
            }
            poseStack.method_22909();
        }
    }

    private static void copyMatrixToSleeve(class_630 arm, class_630 sleeve) {
        if (arm instanceof TransformablePart) {
            TransformablePart tArm = (TransformablePart) arm;
            if (!(sleeve instanceof TransformablePart)) {
                return;
            }
            TransformablePart tSleeve = (TransformablePart) sleeve;
            Matrix4f armMatrix = tArm.punchy$getExplicitTransform();
            if (armMatrix == null) {
                tSleeve.punchy$setExplicitTransform(null);
            } else {
                tSleeve.punchy$setExplicitTransform(new Matrix4f(armMatrix));
            }
        }
    }

    private static boolean shouldApplySwimTransforms() {
        if (CrawlStateMachine.shouldSuppressSwimForSwap() || ClimbStateMachine.shouldSuppressSwimForSwap()) {
            return false;
        }
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying() && PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() != null) {
            AnimationClip overlay = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
            if (PunchyAnimationManager.isClipNamed(overlay, "swiming") || PunchyAnimationManager.isClipNamed(overlay, "crawl") || PunchyAnimationManager.isClipNamed(overlay, "climb")) {
                return true;
            }
        }
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            return true;
        }
        if (isDualHandedEquipPoseActive(clip)) {
            return false;
        }
        String name = clip.getName();
        if (name == null || name.isBlank()) {
            return true;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (key.startsWith("use_") || key.startsWith("set_fire") || key.startsWith("put_out_fire")) {
            return false;
        }
        return true;
    }

    private static boolean isDualHandedEquipPoseActive(AnimationClip clip) {
        String name;
        if (!PunchyAnimationManager.POSE_HANDLER.isPlaying() || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if ((!key.equals("hand_in") && !key.equals("hand_out") && !key.startsWith("hand_in_") && !key.startsWith("hand_out_")) || PunchyAnimationManager.POSE_HANDLER.getActiveArm() != null) {
            return false;
        }
        return PunchyAnimationManager.isDualHandedActive(clip, PunchyAnimationManager.POSE_HANDLER.getCurrentTime());
    }

    private static void applyGeoArmToPlayer(class_630 playerArm, class_630 geoArm, Matrix4f rootWorld, class_746 player, class_1306 arm, float partialTicks) {
        if (playerArm == null || !(playerArm instanceof TransformablePart)) {
            return;
        }
        TransformablePart tPlayer = (TransformablePart) playerArm;
        if (geoArm == null) {
            tPlayer.punchy$setExplicitTransform(null);
            return;
        }
        Matrix4f armLocal = getLocalMatrix(geoArm);
        if (armLocal == null) {
            tPlayer.punchy$setExplicitTransform(null);
            return;
        }
        if (isBlacklistedForArm(player, arm)) {
            tPlayer.punchy$setExplicitTransform(null);
            return;
        }
        Matrix4f world = new Matrix4f(rootWorld);
        if (!PunchyConfig.disableArmPhysics()) {
            MovementStateMachine.get().applyRootSpaceMotion(world, partialTicks, arm);
            MovementStateMachine.get().applyShieldImpactToRoot(world, partialTicks, arm);
        }
        world.mul(armLocal);
        applyArmOffsets(world, player, arm, partialTicks);
        MovementStateMachine.get().applyCameraYawLagToArm(world);
        if (MovementStateMachine.APPLY_TO_ARMS) {
            MovementStateMachine.get().applyToMatrix(world, partialTicks, arm);
        }
        cacheArmWorld(player, arm, world);
        tPlayer.punchy$setExplicitTransform(world);
    }

    private static void cacheArmWorld(class_746 player, class_1306 arm, Matrix4f world) {
        if (player == null || arm == null || world == null || player.method_37908() == null) {
            return;
        }
        long tick = player.method_37908().method_8510();
        if (tick != lastArmCacheTick) {
            ARM_WORLD_CACHE.clear();
            lastArmCacheTick = tick;
        }
        ARM_WORLD_CACHE.put(arm, new Matrix4f(world));
    }

    private static void applyArmOffsets(Matrix4f world, class_746 player, class_1306 arm, float partialTicks) {
        MovementStateMachine movement;
        if (player == null || arm == null) {
            return;
        }
        boolean leftArm = arm == class_1306.field_6182;
        class_1268 hand = resolveHand(player, arm);
        class_1799 baseStack = player.method_5998(hand);
        class_1799 stack = PunchyAnimationManager.resolveRenderStack(player, arm, baseStack);
        PlayerArmModelLayer.ToolKind armKind = ToolKindResolver.resolveArmKind(stack, PlayerArmModelLayer.ToolKind.AXE);
        boolean hasSpecificArmOverride = !stack.method_7960() && ArmToolTuning.hasSpecificProfile(stack, leftArm);
        ArmToolTuning.Profile profile = ArmToolTuning.resolveProfile(stack, armKind, leftArm);
        if (shouldResetBurningEmptyMainHandTuning(player, arm, baseStack)) {
            profile = zeroArmRotations(profile);
        }
        if (!PunchyConfig.disableNativeItemPhysics()) {
            MacePhysics.get().applyTo(world, player, arm, partialTicks);
        }
        ArmToolTuning.Profile globalProfile = ArmToolTuning.getGlobal(leftArm);
        if (globalProfile == null) {
            globalProfile = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        if (profile == null) {
            profile = globalProfile;
        }
        float globalHandFactor = resolveGlobalHandFactorForFlightPose();
        if (globalHandFactor < 0.999f) {
            globalProfile = new ArmToolTuning.Profile(globalProfile.scale, globalProfile.offX * globalHandFactor, globalProfile.offY * globalHandFactor, globalProfile.offZ * globalHandFactor, globalProfile.rotX * globalHandFactor, globalProfile.rotY * globalHandFactor, globalProfile.rotZ * globalHandFactor);
        }
        if (globalHandFactor < 0.999f && armKind == PlayerArmModelLayer.ToolKind.EMPTY_HAND && profile != null) {
            profile = new ArmToolTuning.Profile(profile.scale, profile.offX * globalHandFactor, profile.offY * globalHandFactor, profile.offZ * globalHandFactor, profile.rotX * globalHandFactor, profile.rotY * globalHandFactor, profile.rotZ * globalHandFactor);
        }
        float tuningFactor = resolveFlightPoseTuningFactor(partialTicks, arm);
        if (armKind == PlayerArmModelLayer.ToolKind.EMPTY_HAND && (movement = MovementStateMachine.get()) != null) {
            tuningFactor *= 1.0f - class_3532.method_15363(movement.getSprintBlendAlpha(), 0.0f, 1.0f);
        }
        float factor = class_3532.method_15363(tuningFactor * updateWaterArmTuningBlend(arm, partialTicks), 0.0f, 1.0f);
        if (hasSpecificArmOverride) {
            factor = 1.0f;
        }
        float profileRotationFactor = class_3532.method_15363(updateArmRotationResetBlend(arm, partialTicks), 0.0f, 1.0f);
        ArmToolTuning.Profile profile2 = blendArmRotations(profile, profileRotationFactor);
        float posX = (globalProfile.offX + ((profile2.offX - globalProfile.offX) * factor)) * 16.0f;
        float posY = (globalProfile.offY + ((profile2.offY - globalProfile.offY) * factor)) * 16.0f;
        float posZ = (globalProfile.offZ + ((profile2.offZ - globalProfile.offZ) * factor)) * 16.0f;
        float rotX = globalProfile.rotX + ((profile2.rotX - globalProfile.rotX) * factor);
        float rotY = globalProfile.rotY + ((profile2.rotY - globalProfile.rotY) * factor);
        float rotZ = globalProfile.rotZ + ((profile2.rotZ - globalProfile.rotZ) * factor);
        float scale = globalProfile.scale + ((profile2.scale - globalProfile.scale) * factor);
        world.translateLocal(posX, posY, posZ);
        Matrix4f debug = new Matrix4f().rotateX((float) Math.toRadians(rotX)).rotateY((float) Math.toRadians(rotY)).rotateZ((float) Math.toRadians(rotZ)).scale(scale, scale, scale);
        world.mul(debug);
    }

    private static boolean shouldResetBurningEmptyMainHandTuning(class_746 player, class_1306 arm, class_1799 actualStack) {
        return player != null && arm != null && actualStack != null && actualStack.method_7960() && player.method_5809() && resolveHand(player, arm) == class_1268.field_5808;
    }

    private static ArmToolTuning.Profile zeroArmRotations(ArmToolTuning.Profile profile) {
        if (profile == null) {
            return null;
        }
        return new ArmToolTuning.Profile(profile.scale, profile.offX, profile.offY, profile.offZ, 0.0f, 0.0f, 0.0f);
    }

    private static ArmToolTuning.Profile blendArmRotations(ArmToolTuning.Profile profile, float factor) {
        if (profile == null) {
            return null;
        }
        float clamped = class_3532.method_15363(factor, 0.0f, 1.0f);
        return new ArmToolTuning.Profile(profile.scale, profile.offX, profile.offY, profile.offZ, profile.rotX * clamped, profile.rotY * clamped, profile.rotZ * clamped);
    }

    private static ToolTuning.Profile zeroItemRotations(ToolTuning.Profile profile) {
        if (profile == null) {
            return null;
        }
        ToolTuning.Profile adjusted = new ToolTuning.Profile(profile.scale, profile.offX, profile.offY, profile.offZ, 0.0f, 0.0f, 0.0f, profile.pivotX, profile.pivotY, profile.pivotZ, profile.physicsMode);
        adjusted.disableSprintLateralTilt = profile.disableSprintLateralTilt;
        adjusted.keepArmTuning = profile.keepArmTuning;
        adjusted.keepTriggerArmTuning = profile.keepTriggerArmTuning;
        if (profile.physicsSettings != null) {
            adjusted.physicsSettings = profile.physicsSettings.copy();
        }
        return adjusted;
    }

    private static void applyCrawlCameraDecouple(Matrix4f world, class_746 player) {
        AnimationClip clip;
        float t;
        if (world == null || player == null || !PunchyAnimationManager.SWIM_HANDLER.isPlaying() || (clip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip()) == null) {
            return;
        }
        String clipName = clip.getName();
        boolean crawl = "crawl".equals(clipName);
        boolean climb = "climb".equals(clipName);
        if (!crawl && !climb) {
            return;
        }
        float pitch = player.method_36455();
        float absPitch = Math.abs(pitch);
        float start = crawl ? 0.0f : 0.0f;
        float end = crawl ? 0.0f : 0.0f;
        if (absPitch <= start) {
            return;
        }
        if (end <= start) {
            t = 1.0f;
        } else {
            t = (absPitch - start) / (end - start);
        }
        float t2 = class_3532.method_15363(t, 0.0f, 1.0f);
        float rotPitch = Math.min(absPitch, 75.0f) * Math.signum(pitch);
        float counter = (-rotPitch) * t2;
        MovementStateMachine movement = MovementStateMachine.get();
        if (movement == null) {
            world.rotateX((float) Math.toRadians(counter));
            return;
        }
        world.rotate((float) Math.toRadians(counter), 1.0f, 0.0f, 0.0f);
        if (crawl && pitch > 0.0f) {
            float extra = class_3532.method_15363(pitch / 90.0f, 0.0f, 1.0f);
            world.translate(0.0f, 20.0f * extra, 0.0f);
        }
    }

    private static void applyCameraLookOffset(Matrix4f world) {
        MovementStateMachine movement;
        if (world == null || PunchyConfig.disableArmPhysics() || (movement = MovementStateMachine.get()) == null) {
            return;
        }
        movement.applyCameraLookToRoot(world);
    }

    static Matrix4f getLocalMatrix(class_630 part) {
        if (part == null) {
            return null;
        }
        if (part instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) part;
            Matrix4f explicit = transformable.punchy$getExplicitTransform();
            if (explicit != null) {
                return new Matrix4f(explicit);
            }
        }
        return new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
    }

    private static Vector3f getAnimatedTranslation(class_630 part) {
        if (part == null || !(part instanceof TransformablePart)) {
            return new Vector3f();
        }
        TransformablePart transformable = (TransformablePart) part;
        Matrix4f explicit = transformable.punchy$getExplicitTransform();
        if (explicit == null) {
            if (transformable.punchy$hasDirectPoseBase()) {
                return new Vector3f(part.field_3657 - transformable.punchy$getDirectPoseBaseX(), part.field_3656 - transformable.punchy$getDirectPoseBaseY(), part.field_3655 - transformable.punchy$getDirectPoseBaseZ());
            }
            return new Vector3f();
        }
        Matrix4f base = new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
        Matrix4f baseInv = new Matrix4f(base).invert();
        Matrix4f anim = baseInv.mul(explicit, new Matrix4f());
        return anim.getTranslation(new Vector3f());
    }

    private static Vector3f getAnimatedGripTranslation(class_630 part) {
        if (part == null || !(part instanceof TransformablePart)) {
            return new Vector3f();
        }
        TransformablePart transformable = (TransformablePart) part;
        Matrix4f explicit = transformable.punchy$getExplicitTransform();
        if (explicit == null && transformable.punchy$hasDirectPoseBase()) {
            Vector3f directDelta = new Vector3f(part.field_3657 - transformable.punchy$getDirectPoseBaseX(), part.field_3656 - transformable.punchy$getDirectPoseBaseY(), part.field_3655 - transformable.punchy$getDirectPoseBaseZ());
            return new Vector3f(-directDelta.x, -directDelta.z, -directDelta.y);
        }
        return getAnimatedTranslation(part);
    }

    private static void applyGripLocalTransform(class_4587 poseStack, class_630 geoGrip, Vector3f pivotOffset) {
        if (geoGrip == null) {
            return;
        }
        if (geoGrip instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) geoGrip;
            if (transformable.punchy$getExplicitTransform() == null && transformable.punchy$hasDirectPoseBase()) {
                float deltaX = geoGrip.field_3654 - transformable.punchy$getDirectPoseBaseXRot();
                float deltaY = geoGrip.field_3675 - transformable.punchy$getDirectPoseBaseYRot();
                float deltaZ = geoGrip.field_3674 - transformable.punchy$getDirectPoseBaseZRot();
                float pivotX = pivotOffset != null ? pivotOffset.x : 0.0f;
                float pivotY = pivotOffset != null ? pivotOffset.y : 0.0f;
                float pivotZ = pivotOffset != null ? pivotOffset.z : 0.0f;
                if (pivotX != 0.0f || pivotY != 0.0f || pivotZ != 0.0f) {
                    poseStack.method_46416(pivotX, pivotY, pivotZ);
                }
                poseStack.method_22907(class_7833.field_40714.rotation(-deltaX));
                poseStack.method_22907(class_7833.field_40716.rotation(-deltaZ));
                poseStack.method_22907(class_7833.field_40718.rotation(-deltaY));
                if (pivotX != 0.0f || pivotY != 0.0f || pivotZ != 0.0f) {
                    poseStack.method_46416(-pivotX, -pivotY, -pivotZ);
                    return;
                }
                return;
            }
        }
        Matrix4f gripLocal = getLocalMatrix(geoGrip);
        if (gripLocal == null) {
            return;
        }
        Quaternionf rot = new Quaternionf();
        gripLocal.getUnnormalizedRotation(rot);
        rot.invert();
        float pivotX2 = pivotOffset != null ? pivotOffset.x : 0.0f;
        float pivotY2 = pivotOffset != null ? pivotOffset.y : 0.0f;
        float pivotZ2 = pivotOffset != null ? pivotOffset.z : 0.0f;
        if (pivotX2 != 0.0f || pivotY2 != 0.0f || pivotZ2 != 0.0f) {
            poseStack.method_46416(pivotX2, pivotY2, pivotZ2);
        }
        poseStack.method_22907(rot);
        if (pivotX2 != 0.0f || pivotY2 != 0.0f || pivotZ2 != 0.0f) {
            poseStack.method_46416(-pivotX2, -pivotY2, -pivotZ2);
        }
    }

    private static void applyGripTranslation(class_4587 poseStack, Vector3f gripPos) {
        if (gripPos == null) {
            return;
        }
        poseStack.method_46416(gripPos.x, gripPos.y, gripPos.z);
    }

    private static void applyItemPreTransform(class_4587 poseStack, Vector3f pivot, ToolTuning.Profile tuning, boolean legacyOrder) {
        if (tuning == null) {
            return;
        }
        float rotX = tuning.rotX;
        float rotY = tuning.rotY;
        float rotZ = tuning.rotZ;
        float pivotX = pivot != null ? pivot.x : 0.0f;
        float pivotY = pivot != null ? pivot.y : 0.0f;
        float pivotZ = pivot != null ? pivot.z : 0.0f;
        if (pivotX != 0.0f || pivotY != 0.0f || pivotZ != 0.0f) {
            poseStack.method_46416(pivotX, pivotY, pivotZ);
        }
        if (legacyOrder) {
            poseStack.method_22907(class_7833.field_40718.rotationDegrees(rotZ));
            poseStack.method_22907(class_7833.field_40714.rotationDegrees(rotX));
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(rotY));
        } else {
            poseStack.method_22907(class_7833.field_40714.rotationDegrees(rotX));
            poseStack.method_22907(class_7833.field_40716.rotationDegrees(rotY));
            poseStack.method_22907(class_7833.field_40718.rotationDegrees(rotZ));
        }
        if (pivotX != 0.0f || pivotY != 0.0f || pivotZ != 0.0f) {
            poseStack.method_46416(-pivotX, -pivotY, -pivotZ);
        }
    }

    private static void applyItemTranslation(class_4587 poseStack, ToolTuning.Profile tuning, boolean isLeft) {
        if (tuning == null) {
            return;
        }
        float itemBaseX = isLeft ? 0.04f : -0.04f;
        poseStack.method_46416(tuning.offX + itemBaseX, tuning.offY + PIVOT_ITEM_COMP_Y + ITEM_BASE_OFFSET_Y, tuning.offZ + ITEM_BASE_OFFSET_Z);
    }

    private static Vector3f getGeoItemScale(class_630 geoGrip, class_630 geoItem) {
        class_630 source = geoGrip != null ? geoGrip : geoItem;
        if (source == null) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }
        Matrix4f local = getLocalMatrix(source);
        if (local == null) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }
        return getScaleFromMatrix(local);
    }

    private static void applyGeoItemScale(class_4587 poseStack, class_630 geoGrip, class_630 geoItem) {
        Vector3f scale = getGeoItemScale(geoGrip, geoItem);
        poseStack.method_22905(scale.x, scale.y, scale.z);
    }

    private static Vector3f getScaleFromMatrix(Matrix4f matrix) {
        Vector3f x = new Vector3f(matrix.m00(), matrix.m01(), matrix.m02());
        Vector3f y = new Vector3f(matrix.m10(), matrix.m11(), matrix.m12());
        Vector3f z = new Vector3f(matrix.m20(), matrix.m21(), matrix.m22());
        return new Vector3f(x.length(), y.length(), z.length());
    }

    private static float getFreezeProgress(class_742 player, float partialTicks) {
        int ticksFrozen;
        int required;
        if (player == null || (ticksFrozen = player.method_32312()) <= 0 || (required = player.method_32315()) <= 0) {
            return 0.0f;
        }
        float progress = (ticksFrozen + partialTicks) / required;
        return class_3532.method_15363(progress, 0.0f, 1.0f);
    }

    private static float smoothStep(float edge0, float edge1, float x) {
        float t = class_3532.method_15363((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - (2.0f * t));
    }

    private static void renderFreezeOverlay(class_630 armPart, boolean isLeft, boolean slim, class_4587 poseStack, class_4597 buffer, int combinedLight, class_742 player, float partialTicks) {
        float a2;
        float a3;
        if (!PunchyConfig.enableFreezeArmOverlay()) {
            return;
        }
        float progress = getFreezeProgress(player, partialTicks);
        if (player != null) {
            if (player.method_32312() + partialTicks < 24.0f) {
                return;
            }
            int required = player.method_32315();
            if (required > 0) {
                float delayed = ((player.method_32312() + partialTicks) - 24.0f) / required;
                progress = class_3532.method_15363(delayed, 0.0f, 1.0f);
            }
        }
        if (progress <= 0.0f) {
            return;
        }
        if (progress < 0.33f) {
            a2 = 0.0f;
            a3 = 0.0f;
        } else if (progress < 0.66f) {
            a2 = 1.0f;
            a3 = 0.0f;
        } else {
            a2 = 0.0f;
            a3 = 1.0f;
        }
        class_2960 tex = a3 > 0.0f ? FREEZE_ARM_STAGE_3 : a2 > 0.0f ? FREEZE_ARM_STAGE_2 : FREEZE_ARM_STAGE_1;
        submitFreezeLayer3d(armPart, poseStack, buffer, combinedLight, tex, isLeft, slim);
    }

    private static void renderMudOverlay(class_630 armPart, boolean isLeft, boolean slim, class_4587 poseStack, class_4597 buffer, int combinedLight) {
        if (!PunchyConfig.enableMud()) {
            return;
        }
        int stage = MudStateMachine.getStageForArm(isLeft ? class_1306.field_6182 : class_1306.field_6183);
        if (stage <= 0) {
            return;
        }
        class_2960 tex = stage == 1 ? MUD_ARM_STAGE_1 : stage == 2 ? MUD_ARM_STAGE_2 : MUD_ARM_STAGE_3;
        submitFreezeLayer3d(armPart, poseStack, buffer, combinedLight, tex, isLeft, slim);
    }

    private static void renderSweatOverlay(class_630 armPart, boolean isLeft, boolean slim, class_4587 poseStack, class_4597 buffer, int combinedLight) {
        int stage;
        class_2960 tex;
        if (!PunchyConfig.enableSweat() || (stage = SweatStateMachine.getStage()) <= 0) {
            return;
        }
        switch (stage) {
            case 1:
                tex = SWEAT_ARM_STAGE_1;
                break;
            case 2:
                tex = SWEAT_ARM_STAGE_2;
                break;
            case 3:
                tex = SWEAT_ARM_STAGE_3;
                break;
            default:
                tex = SWEAT_ARM_STAGE_4;
                break;
        }
        float alpha = SweatStateMachine.getFadeAlpha();
        if (alpha <= 0.0f) {
            return;
        }
        FreezeVoxelMask mask = getSweatMask(tex, isLeft);
        submitFreezeLayer3dWithMask(armPart, poseStack, buffer, combinedLight, tex, mask, isLeft, slim, SWEAT_VOXEL_THICKNESS, SWEAT_PIXEL_SCALE_X, SWEAT_PIXEL_SCALE_Y, true, alpha);
    }

    private static void renderLavaHandOverlay(class_630 armPart, class_4587 poseStack, class_4597 buffer, int combinedLight, class_742 player, class_1306 arm, float partialTicks) {
        if (player == null || arm == null || !PunchyConfig.enableBurn()) {
            return;
        }
        class_1799 stack = getStackForArm(player, arm);
        float burnAmount = updateBurnFade(player, arm, stack, partialTicks);
        if (burnAmount <= 0.001f) {
            return;
        }
        int alpha = class_3532.method_15340((int) (burnAmount * 255.0f), 0, 255);
        int color = (alpha << 24) | 16777215;
        class_2960 burnTex = resolveBurnOverlayTexture(player);
        class_4588 consumer = buffer.getBuffer(class_1921.method_23580(burnTex));
        armPart.method_22699(poseStack, consumer, combinedLight, class_4608.field_21444, color);
    }

    private static class_2960 resolveBurnOverlayTexture(class_742 player) {
        if (player == null) {
            return BURN_ARM_HAND_OVERLAY;
        }
        class_2960 skinId = getPlayerTexture(player);
        if (skinId == null) {
            return BURN_ARM_HAND_OVERLAY;
        }
        Integer cachedHeight = SKIN_HEIGHT_CACHE.get(skinId);
        if (cachedHeight == null) {
            cachedHeight = readTextureHeight(skinId);
            SKIN_HEIGHT_CACHE.put(skinId, cachedHeight);
        }
        return (cachedHeight == null || cachedHeight.intValue() > FREEZE_TIP_LEFT_U0) ? BURN_ARM_HAND_OVERLAY : BURN_ARM_HAND_OVERLAY_LEGACY;
    }

    private static Integer readTextureHeight(class_2960 texture) {
        class_3300 manager = class_310.method_1551().method_1478();
        Optional<class_3298> res = manager.method_14486(texture);
        if (res.isEmpty()) {
            return null;
        }
        try {
            InputStream stream = res.get().method_14482();
            try {
                class_1011 image = class_1011.method_4309(stream);
                Integer numValueOf = Integer.valueOf(image.method_4323());
                if (stream != null) {
                    stream.close();
                }
                return numValueOf;
            } finally {
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static void submitFreezeLayer3d(class_630 armPart, class_4587 poseStack, class_4597 buffer, int combinedLight, class_2960 texture, boolean isLeft, boolean slim) {
        FreezeVoxelMask mask = getFreezeMask(texture, isLeft);
        submitFreezeLayer3dWithMask(armPart, poseStack, buffer, combinedLight, texture, mask, isLeft, slim, 0.45f, 1.0f, 1.0f, false, 1.0f);
    }

    private static void submitFreezeLayer3dWithMask(class_630 armPart, class_4587 poseStack, class_4597 buffer, int combinedLight, class_2960 texture, FreezeVoxelMask mask, boolean isLeft, boolean slim, float thickness, float pixelScaleX, float pixelScaleY, boolean translucent, float alpha) {
        submitFreezeLayer3dWithMask(armPart, poseStack, buffer, combinedLight, texture, mask, isLeft, slim, thickness, pixelScaleX, pixelScaleY, translucent, alpha, 6.0f);
    }

    private static void submitFreezeLayer3dWithMask(class_630 armPart, class_4587 poseStack, class_4597 buffer, int combinedLight, class_2960 texture, FreezeVoxelMask mask, boolean isLeft, boolean slim, float thickness, float pixelScaleX, float pixelScaleY, boolean translucent, float alpha, float yBandMin) {
        float x0;
        float x1;
        float z0;
        float z1;
        if (mask == null || mask.pixels.isEmpty()) {
            return;
        }
        float width = slim ? 3.0f : 4.0f;
        float xMin = isLeft ? -1.0f : slim ? -2.0f : -3.0f;
        float xMax = xMin + width;
        poseStack.method_22903();
        armPart.method_22703(poseStack);
        poseStack.method_22905(0.0625f, 0.0625f, 0.0625f);
        class_4588 consumer = buffer.getBuffer(translucent ? class_1921.method_23580(texture) : class_1921.method_23578(texture));
        class_4587.class_4665 pose = poseStack.method_23760();
        for (FreezeVoxelMask.Pixel p : mask.pixels) {
            int seg = p.x / FREEZE_TIP_H;
            int segX = p.x % FREEZE_TIP_H;
            if (!slim || ((seg != 1 && seg != 3) || (p.y != 0 && p.y != 3 && p.y != 1))) {
                float y0 = yBandMin + (p.y * 1.0f);
                float y1 = y0 + 1.0f;
                if (seg == 0) {
                    z0 = (-2.0f) + segX;
                    z1 = z0 + 1.0f;
                    x0 = xMax;
                    x1 = xMax + thickness;
                    if (segX == 0) {
                        z0 -= thickness;
                    }
                    if (segX == 3) {
                        z1 += thickness;
                    }
                } else if (seg == 1) {
                    x0 = xMin + segX;
                    x1 = x0 + 1.0f;
                    z0 = (-2.0f) - thickness;
                    z1 = -2.0f;
                    if (segX == 0) {
                        x0 -= thickness;
                    }
                    if (segX == 3) {
                        x1 += thickness;
                    }
                } else if (seg == 2) {
                    z0 = (-2.0f) + segX;
                    z1 = z0 + 1.0f;
                    x0 = xMin - thickness;
                    x1 = xMin;
                    if (segX == 0) {
                        z0 -= thickness;
                    }
                    if (segX == 3) {
                        z1 += thickness;
                    }
                } else {
                    x0 = xMin + segX;
                    x1 = x0 + 1.0f;
                    z0 = 2.0f;
                    z1 = 2.0f + thickness;
                    if (segX == 0) {
                        x0 -= thickness;
                    }
                    if (segX == 3) {
                        x1 += thickness;
                    }
                }
                if (pixelScaleX < 1.0f || pixelScaleY < 1.0f) {
                    float insetX = (1.0f - pixelScaleX) * 0.5f;
                    float insetY = (1.0f - pixelScaleY) * 0.5f;
                    if (seg == 0 || seg == 2) {
                        z0 += insetX;
                        z1 -= insetX;
                        y0 += insetY;
                        y1 -= insetY;
                    } else {
                        x0 += insetX;
                        x1 -= insetX;
                        y0 += insetY;
                        y1 -= insetY;
                    }
                }
                renderVoxel(pose, consumer, x0, x1, y0, y1, z0, z1, p.u0, p.v0, p.u1, p.v1, combinedLight, alpha);
            }
        }
        poseStack.method_22909();
    }

    private static void renderVoxel(class_4587.class_4665 pose, class_4588 consumer, float x0, float x1, float y0, float y1, float z0, float z1, float u0, float v0, float u1, float v1, int packedLight) {
        addVertex(pose, consumer, x0, y1, z0, u0, v0, packedLight, 0.0f, 1.0f, 0.0f);
        addVertex(pose, consumer, x1, y1, z0, u1, v0, packedLight, 0.0f, 1.0f, 0.0f);
        addVertex(pose, consumer, x1, y1, z1, u1, v1, packedLight, 0.0f, 1.0f, 0.0f);
        addVertex(pose, consumer, x0, y1, z1, u0, v1, packedLight, 0.0f, 1.0f, 0.0f);
        addVertex(pose, consumer, x0, y0, z1, u0, v1, packedLight, 0.0f, -1.0f, 0.0f);
        addVertex(pose, consumer, x1, y0, z1, u1, v1, packedLight, 0.0f, -1.0f, 0.0f);
        addVertex(pose, consumer, x1, y0, z0, u1, v0, packedLight, 0.0f, -1.0f, 0.0f);
        addVertex(pose, consumer, x0, y0, z0, u0, v0, packedLight, 0.0f, -1.0f, 0.0f);
        addVertex(pose, consumer, x0, y0, z0, u0, v0, packedLight, 0.0f, 0.0f, -1.0f);
        addVertex(pose, consumer, x1, y0, z0, u1, v0, packedLight, 0.0f, 0.0f, -1.0f);
        addVertex(pose, consumer, x1, y1, z0, u1, v1, packedLight, 0.0f, 0.0f, -1.0f);
        addVertex(pose, consumer, x0, y1, z0, u0, v1, packedLight, 0.0f, 0.0f, -1.0f);
        addVertex(pose, consumer, x0, y1, z1, u0, v1, packedLight, 0.0f, 0.0f, 1.0f);
        addVertex(pose, consumer, x1, y1, z1, u1, v1, packedLight, 0.0f, 0.0f, 1.0f);
        addVertex(pose, consumer, x1, y0, z1, u1, v0, packedLight, 0.0f, 0.0f, 1.0f);
        addVertex(pose, consumer, x0, y0, z1, u0, v0, packedLight, 0.0f, 0.0f, 1.0f);
        addVertex(pose, consumer, x0, y0, z1, u0, v1, packedLight, -1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x0, y1, z1, u1, v1, packedLight, -1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x0, y1, z0, u1, v0, packedLight, -1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x0, y0, z0, u0, v0, packedLight, -1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x1, y0, z0, u0, v0, packedLight, 1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x1, y1, z0, u1, v0, packedLight, 1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x1, y1, z1, u1, v1, packedLight, 1.0f, 0.0f, 0.0f);
        addVertex(pose, consumer, x1, y0, z1, u0, v1, packedLight, 1.0f, 0.0f, 0.0f);
    }

    private static void renderVoxel(class_4587.class_4665 pose, class_4588 consumer, float x0, float x1, float y0, float y1, float z0, float z1, float u0, float v0, float u1, float v1, int packedLight, float alpha) {
        addVertexAlpha(pose, consumer, x0, y1, z0, u0, v0, packedLight, 0.0f, 1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z0, u1, v0, packedLight, 0.0f, 1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z1, u1, v1, packedLight, 0.0f, 1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y1, z1, u0, v1, packedLight, 0.0f, 1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z1, u0, v1, packedLight, 0.0f, -1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z1, u1, v1, packedLight, 0.0f, -1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z0, u1, v0, packedLight, 0.0f, -1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z0, u0, v0, packedLight, 0.0f, -1.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z0, u0, v0, packedLight, 0.0f, 0.0f, -1.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z0, u1, v0, packedLight, 0.0f, 0.0f, -1.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z0, u1, v1, packedLight, 0.0f, 0.0f, -1.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y1, z0, u0, v1, packedLight, 0.0f, 0.0f, -1.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y1, z1, u0, v1, packedLight, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z1, u1, v1, packedLight, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z1, u1, v0, packedLight, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z1, u0, v0, packedLight, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z1, u0, v1, packedLight, -1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y1, z1, u1, v1, packedLight, -1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y1, z0, u1, v0, packedLight, -1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x0, y0, z0, u0, v0, packedLight, -1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z0, u0, v0, packedLight, 1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z0, u1, v0, packedLight, 1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y1, z1, u1, v1, packedLight, 1.0f, 0.0f, 0.0f, alpha);
        addVertexAlpha(pose, consumer, x1, y0, z1, u0, v1, packedLight, 1.0f, 0.0f, 0.0f, alpha);
    }

    private static void addVertex(class_4587.class_4665 pose, class_4588 consumer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz) {
        consumer.method_22918(pose.method_23761(), x, y, z).method_1336(255, 255, 255, 255).method_22913(u, v).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, nx, ny, nz);
    }

    private static void addVertexAlpha(class_4587.class_4665 pose, class_4588 consumer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz, float alpha) {
        int a = class_3532.method_15340((int) (alpha * 255.0f), 0, 255);
        consumer.method_22918(pose.method_23761(), x, y, z).method_1336(255, 255, 255, a).method_22913(u, v).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, nx, ny, nz);
    }

    private static FreezeVoxelMask getFreezeMask(class_2960 texture, boolean isLeft) {
        return getOverlayMask(texture, isLeft, "freeze", 40, 28, FREEZE_TIP_LEFT_U0, FREEZE_TIP_LEFT_V0, 16, FREEZE_TIP_H);
    }

    private static FreezeVoxelMask getSweatMask(class_2960 texture, boolean isLeft) {
        return getOverlayMask(texture, isLeft, "sweat", 40, 28, 40, 28, 16, 16);
    }

    private static FreezeVoxelMask getOverlayMask(class_2960 texture, boolean isLeft, String prefix, int rightU0, int rightV0, int leftU0, int leftV0, int width, int height) {
        String key = prefix + ":" + String.valueOf(texture) + ":" + (isLeft ? "L" : "R");
        FreezeVoxelMask cached = FREEZE_MASK_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        class_3300 manager = class_310.method_1551().method_1478();
        Optional<class_3298> res = manager.method_14486(texture);
        if (res.isEmpty()) {
            return null;
        }
        try {
            InputStream stream = res.get().method_14482();
            try {
                class_1011 image = class_1011.method_4309(stream);
                int w = image.method_4307();
                int h = image.method_4323();
                List<FreezeVoxelMask.Pixel> pixels = new ArrayList<>();
                int u0 = isLeft ? leftU0 : rightU0;
                int v0 = isLeft ? leftV0 : rightV0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = image.method_61940(u0 + x, v0 + y);
                        int a = (argb >> 24) & 255;
                        if (a > 0) {
                            float uA = (u0 + x) / w;
                            float vA = (v0 + y) / h;
                            float uB = ((u0 + x) + 1) / w;
                            float vB = ((v0 + y) + 1) / h;
                            pixels.add(new FreezeVoxelMask.Pixel(x, y, uA, vA, uB, vB));
                        }
                    }
                }
                FreezeVoxelMask mask = new FreezeVoxelMask(pixels);
                FREEZE_MASK_CACHE.put(key, mask);
                if (stream != null) {
                    stream.close();
                }
                return mask;
            } finally {
            }
        } catch (IOException e) {
            return null;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyArmRenderer$FreezeVoxelMask.class */
    private static final class FreezeVoxelMask extends Record {
        private final List<Pixel> pixels;

        /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel.class */
        private static final class Pixel extends Record {
            private final int x;
            private final int y;
            private final float u0;
            private final float v0;
            private final float u1;
            private final float v1;

            private Pixel(int x, int y, float u0, float v0, float u1, float v1) {
                this.x = x;
                this.y = y;
                this.u0 = u0;
                this.v0 = v0;
                this.u1 = u1;
                this.v1 = v1;
            }

            @Override // java.lang.Record
            public final String toString() {
                return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, Pixel.class), Pixel.class, "x;y;u0;v0;u1;v1", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->x:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->y:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u1:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v1:F").dynamicInvoker().invoke(this) /* invoke-custom */;
            }

            @Override // java.lang.Record
            public final int hashCode() {
                return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, Pixel.class), Pixel.class, "x;y;u0;v0;u1;v1", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->x:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->y:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u1:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v1:F").dynamicInvoker().invoke(this) /* invoke-custom */;
            }

            @Override // java.lang.Record
            public final boolean equals(Object o) {
                return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, Pixel.class, Object.class), Pixel.class, "x;y;u0;v0;u1;v1", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->x:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->y:I", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v0:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->u1:F", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask$Pixel;->v1:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
            }

            public int x() {
                return this.x;
            }

            public int y() {
                return this.y;
            }

            public float u0() {
                return this.u0;
            }

            public float v0() {
                return this.v0;
            }

            public float u1() {
                return this.u1;
            }

            public float v1() {
                return this.v1;
            }
        }

        private FreezeVoxelMask(List<Pixel> pixels) {
            this.pixels = pixels;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FreezeVoxelMask.class), FreezeVoxelMask.class, "pixels", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask;->pixels:Ljava/util/List;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FreezeVoxelMask.class), FreezeVoxelMask.class, "pixels", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask;->pixels:Ljava/util/List;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FreezeVoxelMask.class, Object.class), FreezeVoxelMask.class, "pixels", "FIELD:Lpunchy/client/render/PunchyArmRenderer$FreezeVoxelMask;->pixels:Ljava/util/List;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public List<Pixel> pixels() {
            return this.pixels;
        }
    }

    private static boolean isHotItem(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_31574(class_1802.field_8187) || stack.method_31574(class_1802.field_8354) || stack.method_31574(class_1802.field_8135) || stack.method_31574(class_1802.field_8894) || stack.method_31574(class_1802.field_8183) || stack.method_31574(class_1802.field_8814) || stack.method_31574(class_1802.field_17346) || stack.method_31574(class_1802.field_23842);
    }

    private static float updateBurnFade(class_742 player, class_1306 arm, class_1799 stack, float partialTicks) {
        if (player == null || arm == null || player.method_37908() == null) {
            return 0.0f;
        }
        boolean target = isHotItem(stack);
        String equipType = HandEquipStateMachine.getActiveType();
        class_1306 equipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        boolean equipMatchesArm = equipArm != null && equipArm == arm;
        BurnFadeState state = (BurnFadeState) BURN_FADE_STATE.computeIfAbsent(arm, key -> {
            return new BurnFadeState();
        });
        float time = player.method_37908().method_8510() + partialTicks;
        if (state.lastTime < 0.0f) {
            state.lastTime = time;
            state.value = 0.0f;
            return 0.0f;
        }
        if (equipMatchesArm && target && "hand_in".equals(equipType)) {
            state.value = 0.0f;
            state.lastTime = time;
            return 0.0f;
        }
        if (equipMatchesArm && !target && "hand_out".equals(equipType)) {
            state.lastTime = time;
            return state.value;
        }
        float dt = time - state.lastTime;
        state.lastTime = time;
        float desired = target ? 1.0f : 0.0f;
        float fadeTicks = target ? BURN_FADE_IN_TICKS : 60.0f;
        float step = fadeTicks <= 0.0f ? 1.0f : class_3532.method_15363(dt / fadeTicks, 0.0f, 1.0f);
        state.value = class_3532.method_15363(state.value + ((desired - state.value) * step), 0.0f, 1.0f);
        return state.value;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyArmRenderer$BurnFadeState.class */
    private static final class BurnFadeState {
        private float value = 0.0f;
        private float lastTime = -1.0f;

        private BurnFadeState() {
        }
    }

    private static void applyFreezeShake(class_4587 poseStack, class_742 player, float partialTicks) {
        if (!PunchyConfig.enableFreezeShake()) {
            return;
        }
        float progress = getFreezeProgress(player, partialTicks);
        if (progress <= 0.0f) {
            return;
        }
        float time = player.field_6012 + partialTicks;
        float translateAmp = FREEZE_SHAKE_TRANSLATION * progress;
        float rotAmp = FREEZE_SHAKE_ROTATION_DEG * progress;
        float offsetX = class_3532.method_15374(time * 3.2f) * translateAmp;
        float offsetY = class_3532.method_15374((time * 4.6f) + 1.7f) * translateAmp * 0.6f;
        float offsetZ = class_3532.method_15374((time * 2.4f) + 2.1f) * translateAmp;
        poseStack.method_46416(offsetX, offsetY, offsetZ);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(class_3532.method_15374(time * 5.1f) * rotAmp));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(class_3532.method_15374((time * 4.3f) + 0.5f) * rotAmp));
    }

    private static void renderArm(class_591 playerModel, class_742 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int combinedLight, class_2960 texture, boolean slim, float partialTicks) {
        class_630 armPart = arm == class_1306.field_6182 ? playerModel.field_27433 : playerModel.field_3401;
        boolean isLeft = arm == class_1306.field_6182;
        poseStack.method_22903();
        if (slim) {
            poseStack.method_46416(((isLeft ? 1.0f : -1.0f) * 0.5f) / 16.0f, 0.0f, 0.0f);
        }
        applyArmMeshOffsets(poseStack, isLeft);
        applyFreezeShake(poseStack, player, partialTicks);
        class_4588 consumer = buffer.getBuffer(class_1921.method_23580(texture));
        armPart.method_22698(poseStack, consumer, combinedLight, class_4608.field_21444);
        renderLavaHandOverlay(armPart, poseStack, buffer, combinedLight, player, isLeft ? class_1306.field_6182 : class_1306.field_6183, partialTicks);
        renderFreezeOverlay(armPart, isLeft, slim, poseStack, buffer, combinedLight, player, partialTicks);
        renderMudOverlay(armPart, isLeft, slim, poseStack, buffer, combinedLight);
        renderSweatOverlay(armPart, isLeft, slim, poseStack, buffer, combinedLight);
        if (player.method_5809()) {
            poseStack.method_22903();
            armPart.method_22703(poseStack);
            if (!PunchyConfig.disableEnhancedFireArmEffects()) {
                renderFlameOnArm(poseStack, buffer);
            }
            poseStack.method_22909();
        }
        poseStack.method_22909();
    }

    private static void clearExplicitTransform(class_630 part) {
        if (part != null && (part instanceof TransformablePart)) {
            TransformablePart transformable = (TransformablePart) part;
            transformable.punchy$setExplicitTransform(null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    /* JADX WARN: Finally extract failed */
    private static void renderItemInHand(class_759 handRenderer, class_591 playerModel, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int combinedLight, Matrix4f rootWorld, class_630 geoArm, class_630 geoItem, class_630 geoGrip, float partialTicks) throws MatchException {
        class_1799 baseStack;
        VanillaFirstPersonItemLayer.ToolKind toolKindResolveItemKind;
        ToolTuning.Profile profileResolveProfile;
        class_811 class_811Var;
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        if (arm == class_1306.field_6183) {
            if (mainRight) {
                baseStack = player.method_6047();
            } else if (player.method_6115() && player.method_6058() == class_1268.field_5810) {
                baseStack = player.method_6079();
            } else {
                baseStack = HandEquipStateMachine.getVisualOffhandStack(player);
            }
        } else if (!mainRight) {
            baseStack = player.method_6047();
        } else if (player.method_6115() && player.method_6058() == class_1268.field_5810) {
            baseStack = player.method_6079();
        } else {
            baseStack = HandEquipStateMachine.getVisualOffhandStack(player);
        }
        class_1799 stack = PunchyAnimationManager.resolveRenderStack(player, arm, baseStack);
        boolean isLeft = arm == class_1306.field_6182;
        class_1268 interactionHand = resolveHand(player, arm);
        boolean isSlimeBlock = stack.method_31574(class_1802.field_8828);
        boolean isSlimeBall = stack.method_31574(class_1802.field_8777);
        boolean forceBlockTuning = shouldForceBlockTuning(stack);
        if (forceBlockTuning) {
            toolKindResolveItemKind = VanillaFirstPersonItemLayer.ToolKind.BLOCK;
        } else {
            toolKindResolveItemKind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        }
        VanillaFirstPersonItemLayer.ToolKind kind = toolKindResolveItemKind;
        if (forceBlockTuning) {
            profileResolveProfile = ToolTuning.get(VanillaFirstPersonItemLayer.ToolKind.BLOCK, isLeft);
        } else {
            profileResolveProfile = ToolTuning.resolveProfile(stack, kind, isLeft);
        }
        ToolTuning.Profile tuning = profileResolveProfile;
        if (shouldResetBurningEmptyMainHandTuning(player, arm, baseStack)) {
            tuning = zeroItemRotations(tuning);
        }
        ToolTuning.Profile effectiveTuning = tuning;
        poseStack.method_22903();
        playerModel.method_2803(arm, poseStack);
        applyFreezeShake(poseStack, player, partialTicks);
        if (!PunchyConfig.disableNativeItemPhysics()) {
            ShieldImpactPhysics.get().applyTo(poseStack, arm, partialTicks);
        }
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(-90.0f));
        poseStack.method_22907(class_7833.field_40716.rotationDegrees(180.0f));
        boolean hasSlimeBonePhysics = BedrockItemSystem.get().hasSlimeBonePhysics(stack);
        if (!isSlimeBall && !isSlimeBlock && !hasSlimeBonePhysics && !PunchyConfig.disableNativeItemPhysics()) {
            SlimeInHandPhysics.get().resetHand(interactionHand);
        }
        float side = isLeft ? -1.0f : 1.0f;
        poseStack.method_46416(side / 16.0f, 0.125f, -0.625f);
        if (!PunchyConfig.disableBoatFirstPersonAnimations()) {
        }
        if (stack.method_7960()) {
            poseStack.method_22909();
            return;
        }
        Vector3f gripPos = geoGrip != null ? getGripPos(geoGrip, geoItem, isLeft) : null;
        if (geoGrip != null && gripPos != null) {
            Vector3f animPos = getAnimatedGripTranslation(geoGrip).mul(0.0625f);
            gripPos = new Vector3f(gripPos).add(animPos);
        }
        applyGripTranslation(poseStack, gripPos);
        Vector3f tuningPivot = new Vector3f();
        Vector3f pivotFull = new Vector3f();
        if (effectiveTuning != null) {
            tuningPivot.set(effectiveTuning.pivotX, effectiveTuning.pivotY, effectiveTuning.pivotZ);
            pivotFull = getTuningPivot(effectiveTuning, isLeft);
        }
        applyGripLocalTransform(poseStack, geoGrip, pivotFull);
        Vector3f geoScale = getGeoItemScale(geoGrip, geoItem);
        poseStack.method_22905(geoScale.x, geoScale.y, geoScale.z);
        if (effectiveTuning != null && effectiveTuning.scale != 1.0f) {
            poseStack.method_22905(effectiveTuning.scale, effectiveTuning.scale, effectiveTuning.scale);
        }
        if (tuningPivot.x != 0.0f || tuningPivot.y != 0.0f || tuningPivot.z != 0.0f) {
            poseStack.method_46416(tuningPivot.x, tuningPivot.y, tuningPivot.z);
        }
        Vector3f markerPivot = new Vector3f(pivotFull).sub(tuningPivot);
        if (geoGrip != null && effectiveTuning != null) {
            poseStack.method_22903();
            poseStack.method_46416(markerPivot.x, markerPivot.y, markerPivot.z);
            InHandBlockEntityRenderer.updateHeldPivot(player, arm, poseStack);
            if (PunchyDebug.DEBUG_POSITION_ENABLED) {
                poseStack.method_22905(geoScale.x * ARM_ROTATION_RESET_ENTER_STEP_MIN, geoScale.y * ARM_ROTATION_RESET_ENTER_STEP_MIN, geoScale.z * ARM_ROTATION_RESET_ENTER_STEP_MIN);
                handRenderer.method_3233(player, DEBUG_PIVOT_STACK, class_811.field_4319, poseStack, buffer, combinedLight);
            }
            poseStack.method_22909();
        }
        boolean legacyRotationOrder = ChestInHandRenderer.isChestStack(stack) || stack.method_31574(class_1802.field_8249) || stack.method_31574(class_1802.field_8105);
        boolean pendulumHandSpace = (effectiveTuning == null || effectiveTuning.physicsMode == null || effectiveTuning.physicsMode == ToolTuning.PhysicsMode.NONE || (effectiveTuning.physicsSettings != null && Boolean.FALSE.equals(effectiveTuning.physicsSettings.handSpace))) ? false : true;
        boolean disablePendulumByAnimation = PunchyAnimationManager.POSE_HANDLER.isPendulumDisabled(partialTicks, arm);
        Matrix4f baseBeforeItem = new Matrix4f(poseStack.method_23760().method_23761());
        Matrix4f finalPose = null;
        if (pendulumHandSpace) {
            if (!PunchyConfig.disableNativeItemPhysics() && !disablePendulumByAnimation) {
                PendulumPhysics.get().applyTo(poseStack, effectiveTuning.physicsMode, markerPivot, effectiveTuning.physicsSettings, arm, partialTicks);
            }
            applyItemPreTransform(poseStack, markerPivot, effectiveTuning, legacyRotationOrder);
        } else {
            applyItemPreTransform(poseStack, markerPivot, effectiveTuning, legacyRotationOrder);
            if (!PunchyConfig.disableNativeItemPhysics() && !disablePendulumByAnimation) {
                PendulumPhysics.get().applyTo(poseStack, effectiveTuning.physicsMode, markerPivot, effectiveTuning.physicsSettings, arm, partialTicks);
            }
        }
        applyItemTranslation(poseStack, effectiveTuning, isLeft);
        if (isSlimeBall) {
            if (!PunchyConfig.disableNativeItemPhysics()) {
                SlimeInHandPhysics.get().applySlimeBall(poseStack, interactionHand, partialTicks);
                finalPose = new Matrix4f(poseStack.method_23760().method_23761());
            }
        } else if (isSlimeBlock && !PunchyConfig.disableNativeItemPhysics()) {
            SlimeInHandPhysics.get().applySlimeBlock(poseStack, interactionHand, partialTicks);
            finalPose = new Matrix4f(poseStack.method_23760().method_23761());
        }
        if (finalPose == null) {
            finalPose = new Matrix4f(poseStack.method_23760().method_23761());
        }
        if (arm == class_1306.field_6183) {
            class_811Var = class_811.field_4320;
        } else {
            class_811Var = class_811.field_4323;
        }
        class_811 context = class_811Var;
        if (!PunchyConfig.disableBoatMinecartRaftModels()) {
            if (BoatInHandRenderer.isBoatStack(stack) && BoatInHandRenderer.tryRender(stack, interactionHand, poseStack, buffer, combinedLight)) {
                poseStack.method_22909();
                return;
            } else if (MinecartInHandRenderer.isMinecartStack(stack) && MinecartInHandRenderer.tryRender(stack, interactionHand, poseStack, buffer, combinedLight)) {
                poseStack.method_22909();
                return;
            }
        }
        if (!PunchyConfig.disableChestModels() && ChestInHandRenderer.isChestStack(stack) && ChestInHandRenderer.tryRender(stack, interactionHand, poseStack, buffer, combinedLight)) {
            poseStack.method_22909();
            return;
        }
        if (InHandBlockEntityRenderer.renderIfSupported(stack, player, arm, poseStack, buffer, combinedLight, partialTicks)) {
            poseStack.method_22909();
            return;
        }
        Matrix4f noPhysicsPose = new Matrix4f(baseBeforeItem);
        class_4587 tmp = new class_4587();
        tmp.method_23760().method_23761().set(noPhysicsPose);
        applyItemPreTransform(tmp, markerPivot, effectiveTuning, legacyRotationOrder);
        applyItemTranslation(tmp, effectiveTuning, isLeft);
        Matrix4f noPhysicsPose2 = new Matrix4f(tmp.method_23760().method_23761());
        Matrix4f physicsRemoval = new Matrix4f(finalPose).invert().mul(noPhysicsPose2);
        boolean usingItem = player.method_6115() && player.method_6058() == interactionHand;
        BedrockItemSystem.setCurrentPhysicsDelta(arm, physicsRemoval);
        if (0 == 0) {
            try {
                if (BedrockItemSystem.get().tryRenderItem(stack, player, arm, poseStack, buffer, combinedLight, partialTicks, true)) {
                    poseStack.method_22909();
                    BedrockItemSystem.clearCurrentPhysicsDelta(arm);
                    return;
                }
            } catch (Throwable th) {
                BedrockItemSystem.clearCurrentPhysicsDelta(arm);
                throw th;
            }
        }
        BedrockItemSystem.clearCurrentPhysicsDelta(arm);
        if (0 == 0 && ModelPartsSystem.get().tryRenderItem(stack, player, arm, poseStack, buffer, combinedLight, partialTicks, true)) {
            poseStack.method_22909();
            return;
        }
        boolean prevNeutralize = shouldNeutralizeUseTransforms();
        if (isFunctionalMap(player, stack)) {
            ((ItemInHandRendererInvoker) handRenderer).punchy$renderMap(poseStack, buffer, combinedLight, stack);
            Matrix4f particleRoot = new Matrix4f(poseStack.method_23760().method_23761());
            ModelPartsParticleSystem.setRenderRoot(arm, particleRoot);
            ModelPartsParticleSystem.setExactInHandGhostRoot(arm, particleRoot);
            ModelPartsParticleSystem.emitDefinitionForItemRender(class_310.method_1551(), arm, particleRoot, ToolTuning.getParticleEmitters(stack));
            ModelPartsParticleSystem.renderForArm(class_310.method_1551(), arm, buffer, poseStack, combinedLight);
            poseStack.method_22909();
            return;
        }
        setForceVanillaDisplay(!isSpearStack(stack));
        setNeutralizeUseTransforms(usingItem);
        Matrix4f particleBaseRoot = new Matrix4f(poseStack.method_23760().method_23761());
        try {
            handRenderer.method_3233(player, stack, context, poseStack, buffer, combinedLight);
            setForceVanillaDisplay(false);
            setNeutralizeUseTransforms(prevNeutralize);
            Matrix4f particleRoot2 = resolveVanillaItemParticleRoot(player, stack, context, particleBaseRoot, isLeft);
            ModelPartsParticleSystem.setRenderRoot(arm, particleRoot2);
            ModelPartsParticleSystem.setExactInHandGhostRoot(arm, particleRoot2);
            ModelPartsParticleSystem.emitDefinitionForItemRender(class_310.method_1551(), arm, particleRoot2, ToolTuning.getParticleEmitters(stack));
            ModelPartsParticleSystem.renderForArm(class_310.method_1551(), arm, buffer, poseStack, combinedLight);
            poseStack.method_22909();
        } catch (Throwable th2) {
            setForceVanillaDisplay(false);
            setNeutralizeUseTransforms(prevNeutralize);
            throw th2;
        }
    }

    private static boolean isSpearStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        if (stack.method_7909() instanceof class_1835) {
            return true;
        }
        return stack.method_31573(SPEAR_TAG);
    }

    private static Matrix4f resolveVanillaItemParticleRoot(class_746 player, class_1799 stack, class_811 context, Matrix4f baseRoot, boolean isLeft) {
        Matrix4f resolved = baseRoot == null ? new Matrix4f() : new Matrix4f(baseRoot);
        class_804 displayTransform = resolveVanillaDisplayTransform(player, stack, context);
        if (displayTransform == null) {
            return resolved;
        }
        class_4587 tmp = new class_4587();
        tmp.method_23760().method_23761().set(resolved);
        displayTransform.method_23075(isLeft, tmp.method_23760());
        return new Matrix4f(tmp.method_23760().method_23761());
    }

    private static class_804 resolveVanillaDisplayTransform(class_746 player, class_1799 stack, class_811 context) {
        if (player == null || stack == null || stack.method_7960() || context == null) {
            return null;
        }
        class_10442 resolver = class_310.method_1551() == null ? null : class_310.method_1551().method_65386();
        if (resolver == null) {
            return null;
        }
        class_10444 baseline = new class_10444();
        boolean prevForced = UseAnimationSuppressor.isForced();
        UseAnimationSuppressor.force(true);
        try {
            resolver.method_65597(baseline, stack, context, player);
            UseAnimationSuppressor.force(prevForced);
            return resolveItemTransform(baseline);
        } catch (Throwable th) {
            UseAnimationSuppressor.force(prevForced);
            throw th;
        }
    }

    private static class_804 resolveItemTransform(class_10444 state) {
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

    private static boolean isFunctionalMap(class_746 player, class_1799 stack) {
        return (player == null || stack == null || stack.method_7960() || class_1806.method_8001(stack, player.method_37908()) == null) ? false : true;
    }

    private static boolean isBlacklistedForArm(class_746 player, class_1306 arm) {
        class_1268 hand;
        if (player == null || arm == null) {
            return false;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        if (arm == class_1306.field_6183) {
            hand = mainRight ? class_1268.field_5808 : class_1268.field_5810;
        } else {
            hand = mainRight ? class_1268.field_5810 : class_1268.field_5808;
        }
        return PunchyConfig.isHandBlacklisted(player, hand);
    }

    public static void setFirstPersonHidden(boolean hidden) {
        FIRSTPERSON_HIDE = hidden;
    }

    private static boolean isUsingSpyglass(class_742 player) {
        class_1799 useStack;
        if (player == null) {
            return false;
        }
        if (player.method_6115() && (useStack = player.method_6030()) != null && (useStack.method_7909() instanceof class_5538)) {
            return true;
        }
        return player.method_31550();
    }

    private static boolean shouldForceBlockTuning(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        if (PunchyConfig.disableBoatMinecartRaftModels() && (BoatInHandRenderer.isBoatStack(stack) || MinecartInHandRenderer.isMinecartStack(stack))) {
            return true;
        }
        if (PunchyConfig.disableChestModels() && ChestInHandRenderer.isChestStack(stack)) {
            return true;
        }
        if (PunchyConfig.disablePistonModels() && (stack.method_31574(class_1802.field_8249) || stack.method_31574(class_1802.field_8105))) {
            return true;
        }
        if (PunchyConfig.disableEnchantingTableModels() && stack.method_31574(class_1802.field_8657)) {
            return true;
        }
        return false;
    }

    private static void renderFlameOnArm(class_4587 poseStack, class_4597 buffer) {
        if (poseStack == null || buffer == null) {
            return;
        }
        class_1058 sprite = class_310.method_1551().method_1554().method_24153(FIRE_ATLAS_ID).method_4608(FIRE_SPRITE_ID);
        class_4588 consumer = buffer.getBuffer(class_1921.method_23580(FIRE_ATLAS_TEXTURE));
        float[] segments = {ITEM_BASE_OFFSET_Z, -0.22f, -0.42f, -0.62f};
        float[] angles = {0.0f, 1.5707964f, 3.1415927f, 4.712389f};
        poseStack.method_22903();
        poseStack.method_46416(0.004f, 0.56f, -0.04f);
        Quaternionf baseRotation = new Quaternionf().rotateX(-1.5707964f);
        for (float y : segments) {
            for (float angle : angles) {
                float x = ((float) Math.cos(angle)) * 0.06f;
                float z = ((float) Math.sin(angle)) * 0.06f;
                Quaternionf rotation = new Quaternionf(baseRotation).rotateY(angle);
                poseStack.method_22903();
                poseStack.method_46416(x, y, z);
                poseStack.method_22907(rotation);
                renderArmFlameQuad(poseStack, consumer, sprite, 0.24f, 0.32f, 0.95f);
                poseStack.method_22909();
            }
            poseStack.method_22903();
            poseStack.method_46416(0.0f, y, -0.02f);
            poseStack.method_22907(baseRotation);
            renderArmFlameQuad(poseStack, consumer, sprite, 0.18f, 0.28f, 0.7f);
            poseStack.method_22909();
        }
        poseStack.method_22909();
    }

    private static void renderArmFlameQuad(class_4587 poseStack, class_4588 consumer, class_1058 sprite, float width, float height, float alpha) {
        class_4587.class_4665 pose = poseStack.method_23760();
        float halfW = width * 0.5f;
        float top = height * 0.5f;
        float bottom = (-height) * 0.5f;
        float u0 = sprite.method_4594();
        float u1 = sprite.method_4577();
        float v0 = sprite.method_4593();
        float v1 = sprite.method_4575();
        addVertexAlpha(pose, consumer, -halfW, top, 0.0f, u0, v0, 15728880, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, halfW, top, 0.0f, u1, v0, 15728880, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, halfW, bottom, 0.0f, u1, v1, 15728880, 0.0f, 0.0f, 1.0f, alpha);
        addVertexAlpha(pose, consumer, -halfW, bottom, 0.0f, u0, v1, 15728880, 0.0f, 0.0f, 1.0f, alpha);
    }

    private static Vector3f getGripPos(class_630 geoGrip, class_630 geoItem, boolean leftArm) {
        String gripName = leftArm ? "itemgrip_left" : "itemgrip_right";
        Vector3f bbLocal = SimpleBedrockGeo.bedrockLocalPivots.get(gripName);
        if (bbLocal != null) {
            return convertPivot(bbLocal).mul(0.0625f);
        }
        return new Vector3f(-geoGrip.field_3657, -geoGrip.field_3656, geoGrip.field_3655).mul(0.0625f);
    }

    private static Vector3f convertPivot(Vector3f bb) {
        return new Vector3f(-bb.x, bb.z, bb.y);
    }

    private static Vector3f getTuningPivot(ToolTuning.Profile tuning, boolean isLeft) {
        if (tuning == null) {
            return new Vector3f();
        }
        float pivotBaseX = isLeft ? 0.04f : -0.04f;
        return new Vector3f(tuning.pivotX + pivotBaseX, tuning.pivotY + PIVOT_BASE_OFFSET_Y, tuning.pivotZ + PIVOT_BASE_OFFSET_Z);
    }

    private static void applyPivotMarkerTransform(class_4587 poseStack, ToolTuning.Profile tuning, boolean isLeft) {
        if (tuning == null) {
            return;
        }
        poseStack.method_22905(tuning.scale, tuning.scale, tuning.scale);
        Vector3f pivot = getTuningPivot(tuning, isLeft);
        pivot.add(DEBUG_PIVOT_NUDGE);
        poseStack.method_46416(pivot.x, pivot.y, pivot.z);
    }

    private static class_1268 resolveHand(class_746 player, class_1306 arm) {
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? class_1268.field_5808 : class_1268.field_5810 : mainRight ? class_1268.field_5810 : class_1268.field_5808;
    }

    private static class_1799 getStackForArm(class_742 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? player.method_6047() : player.method_6079() : mainRight ? player.method_6079() : player.method_6047();
    }

    private static void applyArmMeshOffsets(class_4587 poseStack, boolean leftArm) {
        ArmMeshTuning.Profile profile = ArmMeshTuning.getGlobal(leftArm);
        if (profile == null) {
            return;
        }
        poseStack.method_46416(profile.offX, profile.offY, profile.offZ);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(profile.rotX));
        poseStack.method_22907(class_7833.field_40716.rotationDegrees(profile.rotY));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(profile.rotZ));
        poseStack.method_22905(profile.scale, profile.scale, profile.scale);
    }

    private static void renderPivotMarker(class_4597 buffer, class_4587 poseStack) {
    }

    /* JADX INFO: renamed from: punchy.client.render.PunchyArmRenderer$1, reason: invalid class name */
    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyArmRenderer$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$minecraft$core$Direction = new int[class_2350.values().length];

        static {
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11033.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11036.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11043.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11035.ordinal()] = PunchyArmRenderer.FREEZE_TIP_H;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11039.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11034.ordinal()] = PunchyArmRenderer.OFFHAND_HIDE_AFTER_LOWER_FRAMES;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private static void renderFace(Matrix4f pose, class_4588 consumer, class_2350 direction, float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alpha) {
        switch (AnonymousClass1.$SwitchMap$net$minecraft$core$Direction[direction.ordinal()]) {
            case 1:
                addVertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y1, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y1, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y1, z2, red, green, blue, alpha);
                break;
            case 2:
                addVertex(consumer, pose, x1, y2, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y2, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z1, red, green, blue, alpha);
                break;
            case 3:
                addVertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y2, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y1, z1, red, green, blue, alpha);
                break;
            case FREEZE_TIP_H /* 4 */:
                addVertex(consumer, pose, x1, y1, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y1, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y2, z2, red, green, blue, alpha);
                break;
            case 5:
                addVertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y1, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y2, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x1, y2, z1, red, green, blue, alpha);
                break;
            case OFFHAND_HIDE_AFTER_LOWER_FRAMES /* 6 */:
                addVertex(consumer, pose, x2, y1, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z1, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
                addVertex(consumer, pose, x2, y1, z2, red, green, blue, alpha);
                break;
        }
    }

    private static void addVertex(class_4588 consumer, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
        consumer.method_22918(pose, x, y, z).method_22915(red, green, blue, alpha);
    }

    private static void resetPlayerModelPose(class_591 model) {
        if (model == null) {
            return;
        }
        model.field_3398.method_41923();
        model.field_3394.method_41923();
        model.field_3391.method_41923();
        model.field_3401.method_41923();
        model.field_27433.method_41923();
        model.field_3392.method_41923();
        model.field_3397.method_41923();
        model.field_3483.method_41923();
        model.field_3486.method_41923();
        model.field_3484.method_41923();
        model.field_3479.method_41923();
        model.field_3482.method_41923();
    }

    private static class_2960 getPlayerTexture(class_742 player) {
        class_2960 texture = class_1068.method_4649();
        try {
            if (player.method_52814() != null && player.method_52814().comp_1626() != null) {
                texture = player.method_52814().comp_1626();
            }
        } catch (Exception e) {
        }
        return texture;
    }

    private static class_591 getFirstPersonModel(boolean slim) {
        if (slim) {
            if (fpModelSlim == null) {
                class_630 root = class_310.method_1551().method_31974().method_32072(class_5602.field_27581);
                fpModelSlim = new class_591(root, true);
            }
            return fpModelSlim;
        }
        if (fpModelDefault == null) {
            class_630 root2 = class_310.method_1551().method_31974().method_32072(class_5602.field_27577);
            fpModelDefault = new class_591(root2, false);
        }
        return fpModelDefault;
    }

    private static boolean isSlim(class_742 player) {
        if (player == null) {
            return false;
        }
        try {
            Object skin = player.method_52814();
            if (skin == null) {
                return false;
            }
            Object model = skin.getClass().getMethod("model", new Class[0]).invoke(skin, new Object[0]);
            if (isSlimModelValue(model)) {
                return true;
            }
        } catch (Exception e) {
        }
        try {
            Method getModelName = player.getClass().getMethod("getModelName", new Class[0]);
            Object modelName = getModelName.invoke(player, new Object[0]);
            if (isSlimModelValue(modelName)) {
                return true;
            }
            return false;
        } catch (Exception e2) {
            return false;
        }
    }

    private static boolean isSlimModelValue(Object modelValue) {
        if (modelValue == null) {
            return false;
        }
        if (modelValue instanceof CharSequence) {
            CharSequence text = (CharSequence) modelValue;
            return "slim".equalsIgnoreCase(text.toString());
        }
        if (modelValue instanceof Enum) {
            Enum<?> enumValue = (Enum) modelValue;
            return "SLIM".equalsIgnoreCase(enumValue.name()) || "slim".equalsIgnoreCase(enumValue.toString());
        }
        String value = String.valueOf(modelValue);
        return "slim".equalsIgnoreCase(value) || value.endsWith(".SLIM");
    }

    private static void applyCameraAnimationCompensation(class_4587 poseStack) {
        if (poseStack == null || !CameraAnimationState.isActive()) {
            return;
        }
        Vector3f offset = CameraAnimationState.offset();
        Quaternionf rotation = CameraAnimationState.rotation();
        if (Math.abs(rotation.w - 1.0f) > 1.0E-4f || rotation.x != 0.0f || rotation.y != 0.0f || rotation.z != 0.0f) {
            Quaternionf inverse = new Quaternionf(rotation).invert();
            Vector3f euler = inverse.getEulerAnglesZYX(new Vector3f());
            euler.x = -euler.x;
            euler.y = -euler.y;
            inverse.rotationZYX(euler.z, euler.y, euler.x);
            poseStack.method_22907(inverse);
        }
        if (offset.lengthSquared() > 0.0f) {
            poseStack.method_46416(offset.x, offset.y, -offset.z);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x002b, code lost:
    
        r0 = (punchy.client.access.TransformablePart) r8;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static void applyLoweredOffhandPose(net.minecraft.class_746 r5, net.minecraft.class_591 r6, net.minecraft.class_1306 r7) {
        /*
            r0 = r5
            if (r0 == 0) goto Lc
            r0 = r6
            if (r0 == 0) goto Lc
            r0 = r7
            if (r0 != 0) goto Ld
        Lc:
            return
        Ld:
            r0 = r7
            net.minecraft.class_1306 r1 = net.minecraft.class_1306.field_6182
            if (r0 != r1) goto L1b
            r0 = r6
            net.minecraft.class_630 r0 = r0.field_27433
            goto L1f
        L1b:
            r0 = r6
            net.minecraft.class_630 r0 = r0.field_3401
        L1f:
            r8 = r0
            r0 = r8
            r10 = r0
            r0 = r10
            boolean r0 = r0 instanceof punchy.client.access.TransformablePart
            if (r0 == 0) goto L35
            r0 = r10
            punchy.client.access.TransformablePart r0 = (punchy.client.access.TransformablePart) r0
            r9 = r0
            goto L36
        L35:
            return
        L36:
            r0 = r9
            org.joml.Matrix4f r0 = r0.punchy$getExplicitTransform()
            r10 = r0
            r0 = r10
            if (r0 != 0) goto L45
            return
        L45:
            r0 = r7
            net.minecraft.class_1306 r1 = net.minecraft.class_1306.field_6182
            if (r0 != r1) goto L52
            r0 = -1082130432(0xffffffffbf800000, float:-1.0)
            goto L53
        L52:
            r0 = 1065353216(0x3f800000, float:1.0)
        L53:
            r11 = r0
            org.joml.Matrix4f r0 = new org.joml.Matrix4f
            r1 = r0
            r1.<init>()
            r1 = r11
            r2 = 0
            float r1 = r1 * r2
            r2 = 0
            r3 = 0
            org.joml.Matrix4f r0 = r0.translate(r1, r2, r3)
            r1 = 4636033603912859648(0x4056800000000000, double:90.0)
            double r1 = java.lang.Math.toRadians(r1)
            float r1 = (float) r1
            org.joml.Matrix4f r0 = r0.rotateX(r1)
            r12 = r0
            r0 = r10
            r1 = r12
            org.joml.Matrix4f r0 = r0.mul(r1)
            r0 = r9
            r1 = r10
            r0.punchy$setExplicitTransform(r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.render.PunchyArmRenderer.applyLoweredOffhandPose(net.minecraft.class_746, net.minecraft.class_591, net.minecraft.class_1306):void");
    }
}
