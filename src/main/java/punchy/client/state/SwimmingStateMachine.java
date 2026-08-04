package punchy.client.state;

import java.util.Locale;
import net.minecraft.class_10255;
import net.minecraft.class_1306;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import punchy.client.PunchyClient;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.bedrockitems.BedrockItemAnimationManager;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/SwimmingStateMachine.class */
public class SwimmingStateMachine {
    private static final int MIN_FALL_HEIGHT_BLOCKS = 5;
    private static final int MAX_WATER_SCAN_BLOCKS = 64;
    private static final float FALLING_IN_WATER_BLEND_IN_SECONDS = 0.64f;
    private static final float SWIM_NO_SHIFT_SPEED_MULTIPLIER = 0.6f;
    private boolean wasSwimming;
    private boolean wasFallingInWater;
    private boolean suppressSwimThisTick;
    private class_1306 lastSwimArm;
    private class_1306 suppressedMainArm;

    public void tick(class_310 client) {
        AnimationClip animationClipResolveAboveWaterClip;
        if (client == null || client.field_1724 == null) {
            return;
        }
        if (!PunchyConfig.enableSwimAnimation()) {
            stopAllSwimOverlays();
            return;
        }
        if (client.field_1724.method_5854() instanceof class_10255) {
            stopAllSwimOverlays();
            return;
        }
        suppressSwimArmForMainAnimation();
        this.suppressSwimThisTick = false;
        boolean submerged = client.field_1724.method_5869();
        boolean aboveWater = !submerged && isAboveWaterSwimming(client);
        WalkOnWaterStateMachine walkOnWater = PunchyClient.getWalkOnWaterStateMachine();
        if (aboveWater && walkOnWater != null && walkOnWater.isCurrentPoseOwnedByWalkOnWater()) {
            aboveWater = false;
        }
        boolean inWater = client.field_1724.method_5799();
        boolean onGround = client.field_1724.method_24828();
        boolean solidSupportAtFeet = hasSolidSupportAtFeet(client);
        boolean forward = client.field_1690 != null && client.field_1690.field_1894.method_1434();
        boolean left = client.field_1690 != null && client.field_1690.field_1913.method_1434();
        boolean right = client.field_1690 != null && client.field_1690.field_1849.method_1434();
        float strafe = client.field_1724.field_6212;
        if (Math.abs(strafe) > 0.001f) {
            left = strafe > 0.0f;
            right = strafe < 0.0f;
        }
        boolean moving = forward || left || right;
        handleFallingInWater(client, inWater, onGround);
        if (!submerged && !aboveWater) {
            if (this.wasSwimming) {
                stopSwim();
                this.wasSwimming = false;
                this.lastSwimArm = null;
                return;
            }
            return;
        }
        boolean activeSwimPose = client.field_1724.method_5681();
        if ((!activeSwimPose && solidSupportAtFeet) || !moving || this.suppressSwimThisTick) {
            if (this.wasSwimming) {
                stopSwim();
                this.wasSwimming = false;
                this.lastSwimArm = null;
                return;
            }
            return;
        }
        if (aboveWater) {
            animationClipResolveAboveWaterClip = resolveAboveWaterClip();
        } else {
            animationClipResolveAboveWaterClip = PunchyAnimationManager.SWIMING_CLIP;
        }
        AnimationClip swimClip = animationClipResolveAboveWaterClip;
        if (playOverlay(swimClip, forward, left, right, false)) {
            this.wasSwimming = true;
        }
    }

    private boolean playOverlay(AnimationClip clip, boolean forward, boolean left, boolean right, boolean freezeAtStart) {
        class_1306 arm;
        if (clip == null) {
            return false;
        }
        PunchyAnimationManager.SWIM_HANDLER.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        class_1306 mainArm = resolveMainArm();
        boolean leftSuppressed = isArmBlockedBySwimFlag(class_310.method_1551(), class_1306.field_6182) || BedrockItemAnimationManager.isSwimSuppressed(class_1306.field_6182) || ItemAnimationManager.isSwimSuppressed(class_1306.field_6182);
        boolean rightSuppressed = isArmBlockedBySwimFlag(class_310.method_1551(), class_1306.field_6183) || BedrockItemAnimationManager.isSwimSuppressed(class_1306.field_6183) || ItemAnimationManager.isSwimSuppressed(class_1306.field_6183);
        if (leftSuppressed && rightSuppressed) {
            return false;
        }
        boolean allowDual = forward || (left && right);
        if (leftSuppressed || rightSuppressed) {
            allowDual = false;
        }
        PunchyAnimationManager.SWIM_HANDLER.setIgnoreDualHanded((allowDual && mainArm == null) ? false : true);
        if (mainArm == null && isMainDualHanded() && !isDualHandedEquipPoseActive()) {
            if (this.wasSwimming) {
                stopSwimWithoutClear();
                this.wasSwimming = false;
                this.lastSwimArm = null;
                return false;
            }
            return false;
        }
        float effectTime = 0.0f;
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying() && PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() == clip) {
            effectTime = PunchyAnimationManager.SWIM_HANDLER.getCurrentTime();
        }
        boolean dualHanded = allowDual && PunchyAnimationManager.isDualHandedActive(class_310.method_1551(), clip, effectTime);
        if (mainArm != null) {
            arm = opposite(mainArm);
            if (arm == class_1306.field_6182 && leftSuppressed && !rightSuppressed) {
                arm = class_1306.field_6183;
            } else if (arm == class_1306.field_6183 && rightSuppressed && !leftSuppressed) {
                arm = class_1306.field_6182;
            } else {
                if (arm == class_1306.field_6182 && leftSuppressed) {
                    return false;
                }
                if (arm == class_1306.field_6183 && rightSuppressed) {
                    return false;
                }
            }
        } else {
            arm = dualHanded ? null : resolveSwimArm(forward, left, right, mainArm);
            if (arm == null && (leftSuppressed ^ rightSuppressed)) {
                arm = leftSuppressed ? class_1306.field_6183 : class_1306.field_6182;
            }
            if (!dualHanded) {
                if (this.lastSwimArm == null && arm != null) {
                    class_1306 clearArm = opposite(arm);
                    if (clearArm == mainArm) {
                        PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(clearArm);
                    } else {
                        PunchyAnimationManager.SWIM_HANDLER.requestClearArm(clearArm);
                    }
                } else if (this.lastSwimArm != null && arm != null && this.lastSwimArm != arm) {
                    if (this.lastSwimArm == mainArm) {
                        PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(this.lastSwimArm);
                    } else {
                        PunchyAnimationManager.SWIM_HANDLER.requestClearArm(this.lastSwimArm);
                    }
                }
            }
        }
        if (arm == null) {
            if (leftSuppressed) {
                PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(class_1306.field_6182);
            }
            if (rightSuppressed) {
                PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(class_1306.field_6183);
            }
        } else if (arm == class_1306.field_6182 && leftSuppressed) {
            arm = rightSuppressed ? null : class_1306.field_6183;
        } else if (arm == class_1306.field_6183 && rightSuppressed) {
            arm = leftSuppressed ? null : class_1306.field_6182;
        }
        if (arm == null && leftSuppressed && rightSuppressed) {
            return false;
        }
        this.lastSwimArm = arm;
        PunchyAnimationManager.SWIM_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.SWIM_HANDLER.setLooping(true);
        PunchyAnimationManager.SWIM_HANDLER.setFrozen(freezeAtStart);
        PunchyAnimationManager.SWIM_HANDLER.setPaused(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        float timelineSpeedScale = 1.0f;
        if ((clip == PunchyAnimationManager.SWIMING_CLIP || clip == PunchyAnimationManager.SWIMING_ABOVE_CLIP) && (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1724.method_5624())) {
            timelineSpeedScale = 0.6f;
        }
        PunchyAnimationManager.SWIM_HANDLER.setSpeedMultiplier(speedMultiplier);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(timelineSpeedScale);
        if (!PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            if (clip == resolveAboveWaterClip() && canRebaseFromWalkOnWater()) {
                PunchyAnimationManager.SWIM_HANDLER.playAtTimeRebasedFromHandlerReturnToOrigin(PunchyAnimationManager.POSE_HANDLER, clip, speedMultiplier, false, 0.0f);
                return true;
            }
            PunchyAnimationManager.SWIM_HANDLER.play(clip, speedMultiplier, false, false);
            return true;
        }
        if (PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() != clip) {
            AnimationClip previousClip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
            float startAt = remapSwimTransitionStartTime(previousClip, clip, PunchyAnimationManager.SWIM_HANDLER.getCurrentTime());
            if (previousClip == resolveAboveWaterClip() && clip == PunchyAnimationManager.SWIMING_CLIP) {
                PunchyAnimationManager.SWIM_HANDLER.playWithBlendFromCurrentDynamic(clip, speedMultiplier, false);
                return true;
            }
            PunchyAnimationManager.SWIM_HANDLER.playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, false, startAt);
            return true;
        }
        return true;
    }

    private void handleFallingInWater(class_310 client, boolean inWater, boolean onGround) {
        AnimationClip clip = PunchyAnimationManager.FALLING_IN_WATER_CLIP;
        if (clip == null) {
            this.wasFallingInWater = false;
            return;
        }
        if (isMainAnimationActive()) {
            if (this.wasFallingInWater) {
                stopFallingInWaterWithoutClear();
                this.wasFallingInWater = false;
                this.lastSwimArm = null;
                return;
            }
            return;
        }
        if (this.wasFallingInWater) {
            if (inWater || onGround || !isActuallyFallingTowardWater(client)) {
                stopFallingInWater();
                this.suppressSwimThisTick = true;
                this.wasFallingInWater = false;
                this.lastSwimArm = null;
                return;
            }
            return;
        }
        if (inWater || !shouldStartFallingInWater(client)) {
            return;
        }
        boolean leftBlockedByFlag = isArmBlockedBySwimFlag(client, class_1306.field_6182);
        boolean rightBlockedByFlag = isArmBlockedBySwimFlag(client, class_1306.field_6183);
        if (leftBlockedByFlag && rightBlockedByFlag) {
            return;
        }
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying() && PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() != clip) {
            return;
        }
        class_1306 forcedArm = null;
        if (leftBlockedByFlag && !rightBlockedByFlag) {
            forcedArm = class_1306.field_6183;
        } else if (rightBlockedByFlag && !leftBlockedByFlag) {
            forcedArm = class_1306.field_6182;
        }
        PunchyAnimationManager.SWIM_HANDLER.setIgnoreDualHanded(forcedArm != null);
        PunchyAnimationManager.SWIM_HANDLER.setActiveArm(forcedArm);
        if (leftBlockedByFlag) {
            PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(class_1306.field_6182);
        }
        if (rightBlockedByFlag) {
            PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(class_1306.field_6183);
        }
        PunchyAnimationManager.SWIM_HANDLER.setLooping(true);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        if (!PunchyAnimationManager.SWIM_HANDLER.isPlaying() || PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() != clip) {
            PunchyAnimationManager.SWIM_HANDLER.setBlendInDurationNext(FALLING_IN_WATER_BLEND_IN_SECONDS);
            PunchyAnimationManager.SWIM_HANDLER.play(clip, speedMultiplier, false, false);
        }
        this.wasFallingInWater = true;
    }

    private void stopFallingInWater() {
        PunchyAnimationManager.SWIM_HANDLER.setLooping(false);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
        PunchyAnimationManager.SWIM_HANDLER.stopImmediateWithBlendOut();
    }

    private void stopFallingInWaterWithoutClear() {
        PunchyAnimationManager.SWIM_HANDLER.setLooping(false);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
        PunchyAnimationManager.SWIM_HANDLER.stopWithoutClear();
    }

    private boolean shouldStartFallingInWater(class_310 client) {
        if (client.field_1724 == null || !isActuallyFallingTowardWater(client)) {
            return false;
        }
        int distanceToWater = findWaterDistanceBelow(client.field_1724.method_37908(), client.field_1724.method_24515());
        return distanceToWater >= MIN_FALL_HEIGHT_BLOCKS;
    }

    private boolean isActuallyFallingTowardWater(class_310 client) {
        return (client == null || client.field_1724 == null || client.field_1724.method_24828() || client.field_1724.method_5799() || client.field_1724.method_6128() || client.field_1724.method_31549().field_7479 || client.field_1724.method_18798().field_1351 >= -0.08d || client.field_1724.field_6017 <= 1.5d) ? false : true;
    }

    private int findWaterDistanceBelow(class_1937 level, class_2338 start) {
        if (level == null || start == null) {
            return -1;
        }
        int startY = start.method_10264();
        class_2338.class_2339 pos = new class_2338.class_2339(start.method_10263(), startY, start.method_10260());
        for (int i = 0; i <= MAX_WATER_SCAN_BLOCKS; i++) {
            int y = startY - i;
            pos.method_33098(y);
            if (level.method_8316(pos).method_15767(class_3486.field_15517)) {
                int waterSurfaceY = y + 1;
                return Math.max(0, startY - waterSurfaceY);
            }
            if (level.method_8320(pos).method_51366()) {
                return -1;
            }
        }
        return -1;
    }

    private void stopSwim() {
        PunchyAnimationManager.SWIM_HANDLER.setLooping(false);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
        PunchyAnimationManager.SWIM_HANDLER.stopImmediateWithBlendOut();
    }

    private void stopSwimWithoutClear() {
        PunchyAnimationManager.SWIM_HANDLER.setLooping(false);
        PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
        PunchyAnimationManager.SWIM_HANDLER.stopWithoutClear();
    }

    private void stopAllSwimOverlays() {
        AnimationClip clip;
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying() && ((clip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip()) == PunchyAnimationManager.SWIMING_CLIP || clip == PunchyAnimationManager.SWIMING_ABOVE_CLIP || clip == PunchyAnimationManager.FALLING_IN_WATER_CLIP)) {
            PunchyAnimationManager.SWIM_HANDLER.setLooping(false);
            PunchyAnimationManager.SWIM_HANDLER.setTimelineSpeedScale(1.0f);
            PunchyAnimationManager.SWIM_HANDLER.stopWithoutClear();
        }
        this.wasSwimming = false;
        this.wasFallingInWater = false;
        this.lastSwimArm = null;
    }

    private class_1306 resolveSwimArm(boolean forward, boolean left, boolean right, class_1306 mainArm) {
        if (mainArm != null) {
            return opposite(mainArm);
        }
        if (forward) {
            return null;
        }
        if (left && right) {
            return null;
        }
        if (left) {
            return class_1306.field_6183;
        }
        if (right) {
            return class_1306.field_6182;
        }
        return null;
    }

    private boolean isMainAnimationActive() {
        return PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() != null;
    }

    private boolean isMainDualHanded() {
        if (!isMainAnimationActive()) {
            return false;
        }
        if (PunchyAnimationManager.isDualHandedActive()) {
            return true;
        }
        return PunchyAnimationManager.POSE_HANDLER.getActiveArm() == null && PunchyAnimationManager.getSourceArm() == null && PunchyAnimationManager.POSE_HANDLER.getActiveArm() == null;
    }

    private boolean isDualHandedEquipPoseActive() {
        String name;
        AnimationClip clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if ((!key.equals("hand_in") && !key.equals("hand_out") && !key.startsWith("hand_in_") && !key.startsWith("hand_out_")) || PunchyAnimationManager.POSE_HANDLER.getActiveArm() != null) {
            return false;
        }
        return PunchyAnimationManager.isDualHandedActive(clip, PunchyAnimationManager.POSE_HANDLER.getCurrentTime());
    }

    private class_1306 resolveMainArm() {
        if (!isMainAnimationActive() || isMainDualHanded()) {
            return null;
        }
        class_1306 active = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (active != null) {
            return active;
        }
        return PunchyAnimationManager.getSourceArm();
    }

    private void suppressSwimArmForMainAnimation() {
        if (!PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            this.suppressedMainArm = null;
            return;
        }
        AnimationClip clip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
        if (clip != PunchyAnimationManager.SWIMING_CLIP && clip != PunchyAnimationManager.SWIMING_ABOVE_CLIP) {
            this.suppressedMainArm = null;
            return;
        }
        class_1306 mainArm = resolveMainArm();
        if (mainArm != null) {
            PunchyAnimationManager.SWIM_HANDLER.requestSkipArm(mainArm);
            this.suppressedMainArm = mainArm;
        } else if (this.suppressedMainArm != null) {
            PunchyAnimationManager.SWIM_HANDLER.requestBlendInArm(this.suppressedMainArm);
            this.suppressedMainArm = null;
        }
    }

    private class_1306 opposite(class_1306 arm) {
        return arm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
    }

    private boolean isArmBlockedBySwimFlag(class_310 client, class_1306 arm) {
        if (client == null || client.field_1724 == null || arm == null) {
            return false;
        }
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        class_1306 mainArm = mainRight ? class_1306.field_6183 : class_1306.field_6182;
        if (arm == mainArm) {
            return PunchyTuningConfig.isSwimingDisabledForStack(client.field_1724.method_6047());
        }
        return PunchyTuningConfig.isSwimingDisabledForStack(client.field_1724.method_6079());
    }

    private boolean isDeepWaterForSwim(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1937 level = client.field_1724.method_37908();
        class_2338 feet = client.field_1724.method_24515();
        if (!level.method_8316(feet).method_15767(class_3486.field_15517)) {
            return false;
        }
        class_2338.class_2339 pos = new class_2338.class_2339(feet.method_10263(), feet.method_10264(), feet.method_10260());
        int waterAbove = 0;
        for (int i = 0; i < 4; i++) {
            pos.method_33098(feet.method_10264() + i);
            if (!level.method_8316(pos).method_15767(class_3486.field_15517)) {
                break;
            }
            waterAbove++;
        }
        return waterAbove >= 2;
    }

    private boolean isAboveWaterSwimming(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517) || hasSolidSupportAtFeet(client)) {
            return false;
        }
        return hasWaterContactAnyDepth(client);
    }

    private AnimationClip resolveAboveWaterClip() {
        AnimationClip clip = PunchyAnimationManager.SWIMING_ABOVE_CLIP;
        if (clip != null) {
            return clip;
        }
        return PunchyAnimationManager.resolveCustomClip("swiming_above");
    }

    private boolean canRebaseFromWalkOnWater() {
        WalkOnWaterStateMachine walkOnWater;
        PoseHandler source = PunchyAnimationManager.POSE_HANDLER;
        if (source == null || source.getCurrentClip() == null) {
            return false;
        }
        return (source.isPlaying() || source.isBlendOutActive()) && (walkOnWater = PunchyClient.getWalkOnWaterStateMachine()) != null && walkOnWater.isCurrentPoseOwnedByWalkOnWater();
    }

    private float remapSwimTransitionStartTime(AnimationClip fromClip, AnimationClip toClip, float fromTime) {
        if (fromClip == null || toClip == null) {
            return 0.0f;
        }
        float fromLength = fromClip.getLength();
        float toLength = toClip.getLength();
        if (fromLength <= 0.0f || toLength <= 0.0f) {
            return 0.0f;
        }
        float normalized = fromTime % fromLength;
        if (normalized < 0.0f) {
            normalized += fromLength;
        }
        return (normalized / fromLength) * toLength;
    }

    private boolean isShallowWater(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1937 level = client.field_1724.method_37908();
        class_238 bounds = client.field_1724.method_5829();
        double sampleY = bounds.field_1322 + 0.05d;
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        for (double sampleX : sampleXs) {
            for (double sampleZ : sampleZs) {
                class_2338 waterPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                if (level.method_8316(waterPos).method_15767(class_3486.field_15517)) {
                    class_2338 below = waterPos.method_10074();
                    if (!level.method_8316(below).method_15767(class_3486.field_15517) && level.method_8320(below).method_51366()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasSolidSupportAtFeet(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1937 level = client.field_1724.method_37908();
        class_238 bounds = client.field_1724.method_5829();
        double sampleY = bounds.field_1322 - 0.05d;
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        for (double sampleX : sampleXs) {
            for (double sampleZ : sampleZs) {
                class_2338 supportPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                if (level.method_8320(supportPos).method_51366()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasWaterContactAnyDepth(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1937 level = client.field_1724.method_37908();
        class_238 bounds = client.field_1724.method_5829();
        double[] sampleYs = {bounds.field_1322 + 0.01d, bounds.field_1322 + 0.08d, bounds.field_1322 + 0.16d, Math.min(bounds.field_1325 - 0.05d, bounds.field_1322 + 0.45d)};
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        for (double sampleY : sampleYs) {
            for (double sampleX : sampleXs) {
                for (double sampleZ : sampleZs) {
                    if (level.method_8316(class_2338.method_49637(sampleX, sampleY, sampleZ)).method_15767(class_3486.field_15517)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
