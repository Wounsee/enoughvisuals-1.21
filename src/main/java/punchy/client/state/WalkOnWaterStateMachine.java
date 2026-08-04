package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3610;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/WalkOnWaterStateMachine.class */
public class WalkOnWaterStateMachine {
    private static final boolean LOG_WALK_ON_WATER_DEBUG = true;
    private static final int IN_WATER_TRIGGER_GAP_TICKS = 20;
    private static final float REBASE_RETURN_SPEED_DEFAULT = 1.7f;
    private static final float REBASE_RETURN_SPEED_END = 4.5f;
    private static final float REBASE_RETURN_SPEED_RESUME = 1.15f;
    private static final String START_WITH_OFFHAND_EFFECT = "start_here_if_off_hand_item";
    private static final String START_WITH_OFFHAND_EFFECT_ALIAS = "start_if_offhand_item";
    private static final String END_STOP_WITH_OFFHAND_EFFECT = "stop_if_offhand_item";
    private PunchyTuningConfig.CustomAnimationEntry activeEntry;
    private AnimationClip startClip;
    private AnimationClip loopClip;
    private AnimationClip endClip;
    private AnimationClip activeClip;
    private Boolean lastTriggerActive;
    private String lastTriggerReason;
    private String lastResolvedSummary;
    private boolean suspendedByForeignClip;
    private float suspendedTime;
    private boolean pauseLoopAfterResumeBlend;
    private int inWaterTriggerGapTicks;
    private int unsupportedWaterGraceTicks;
    private AnimationClip handoffClip;
    private Phase phase = Phase.NONE;
    private class_1799 lastStack = class_1799.field_8037;
    private Phase lastLoggedPhase = Phase.NONE;
    private Phase suspendedPhase = Phase.NONE;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/WalkOnWaterStateMachine$Phase.class */
    private enum Phase {
        NONE,
        START,
        LOOP,
        END
    }

    public void tick(class_310 client) {
        WalkOnWaterSelection walkOnWaterSelectionResolveSelection;
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            stopAndReset(false);
            return;
        }
        if (!client.field_1690.method_31044().method_31034()) {
            stopAndReset(false);
        }
        class_1799 stack = client.field_1724.method_6047();
        if (isWalkOnWaterSuppressedByFishingRod(client)) {
            forceDisableReset();
            return;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "walk_on_water_v2");
        if (entry != null) {
            walkOnWaterSelectionResolveSelection = resolveSelection(client, stack, entry);
        } else {
            walkOnWaterSelectionResolveSelection = WalkOnWaterSelection.INVALID;
        }
        WalkOnWaterSelection selection = walkOnWaterSelectionResolveSelection;
        TriggerState triggerState = applyUnsupportedWaterGrace(client, applyInWaterTriggerGap(client, evaluateTrigger(client)));
        boolean triggerActive = triggerState.active();
        if (triggerActive) {
            ItemAnimationManager.setPersistentType(class_1268.field_5808, "walk_on_water_v2", true, true);
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "walk_on_water_v2");
        }
        logTriggerState(client, entry, selection, triggerState);
        captureOwnedPhaseTime();
        if (shouldYieldToForeignClip()) {
            suspendForForeignClip();
            return;
        }
        if (this.suspendedByForeignClip) {
            if (HandEquipStateMachine.isEquipInProgress()) {
                keepOppositeArmWalkingOnWater();
                return;
            }
            if (!selection.isValid()) {
                stopAndReset(false);
                return;
            }
            if (this.activeEntry != entry || !matchesActiveSelection(selection)) {
                this.activeEntry = entry;
                this.startClip = selection.start();
                this.loopClip = selection.loop();
                this.endClip = selection.end();
                logResolvedSelection(entry, selection);
            }
            resumeFromSuspendedState(triggerActive);
            return;
        }
        if (!selection.isValid()) {
            if (this.phase != Phase.NONE) {
                stopAndReset(true);
                return;
            }
            return;
        }
        boolean needsRestart = (this.phase != Phase.NONE && this.activeEntry == entry && matchesActiveSelection(selection)) ? false : true;
        if (needsRestart) {
            this.activeEntry = entry;
            this.startClip = selection.start();
            this.loopClip = selection.loop();
            this.endClip = selection.end();
            this.lastStack = copyStack(stack);
            logResolvedSelection(entry, selection);
            if (triggerActive) {
                startStartPhase();
                return;
            } else {
                this.phase = Phase.NONE;
                this.activeClip = null;
                return;
            }
        }
        this.lastStack = copyStack(stack);
        boolean pauseForGapWithoutSupport = shouldPauseForInWaterGapWithoutSupport(client, triggerState);
        switch (this.phase.ordinal()) {
            case 0:
                if (triggerActive) {
                    startStartPhase();
                }
                break;
            case LOG_WALK_ON_WATER_DEBUG /* 1 */:
                if (triggerActive) {
                    if (pauseForGapWithoutSupport) {
                        PunchyAnimationManager.POSE_HANDLER.setPaused(true);
                    } else if (!isPlaying(this.startClip)) {
                        startLoopPhase();
                    } else {
                        PunchyAnimationManager.POSE_HANDLER.setPaused(false);
                    }
                } else if (shouldHandOffToSwimAbove(client)) {
                    stopAndReset(false);
                } else {
                    startEndPhase();
                }
                break;
            case 2:
                if (!triggerActive) {
                    if (shouldHandOffToSwimAbove(client)) {
                        stopAndReset(false);
                    } else {
                        startEndPhase();
                    }
                } else if (!isPlaying(this.loopClip)) {
                    startLoopPhase();
                } else {
                    PunchyAnimationManager.POSE_HANDLER.setLooping(true);
                    boolean movementActive = isMovementKeyActive(client);
                    PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
                    if (pauseForGapWithoutSupport) {
                        handler.setPaused(true);
                        handler.setSecondaryPaused(true);
                    } else {
                        class_1306 offArm = resolveArm(client.field_1724, class_1268.field_5810);
                        if (handler.getSecondaryClip() == this.loopClip && offArm != null && handler.getSecondaryArm() == offArm) {
                            handler.setSecondaryPaused(!movementActive);
                        }
                        if (this.pauseLoopAfterResumeBlend) {
                            if (movementActive) {
                                this.pauseLoopAfterResumeBlend = false;
                                handler.setPaused(false);
                            } else if (!PunchyAnimationManager.POSE_HANDLER.isRebaseReturnToOriginActive()) {
                                this.pauseLoopAfterResumeBlend = false;
                                handler.setPaused(true);
                            } else {
                                handler.setPaused(false);
                            }
                        } else {
                            handler.setPaused(!movementActive);
                        }
                    }
                }
                break;
            case 3:
                if (triggerActive) {
                    startStartPhase();
                } else if (shouldStopEndClipEarly(client)) {
                    stopAndReset(false);
                } else if (!isCurrentOrBlending(this.endClip)) {
                    resetState();
                }
                break;
        }
    }

    private void startStartPhase() {
        if (this.startClip == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client != null ? client.field_1724 : null);
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        handler.setIgnoreDualHanded(isAnyWalkOnWaterArmBlocked(client));
        handler.setActiveArm(resolveActiveArmForClip(this.startClip));
        handler.setLooping(false);
        handler.setFrozen(false);
        handler.setPaused(false);
        handler.setBlendInDurationNext(0.0f);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(this.startClip);
        Float startTime = resolveStartClipTime(client);
        float resolvedStartTime = startTime != null ? startTime.floatValue() : 0.0f;
        if (hasPoseToRebase(handler)) {
            handler.setRebaseReturnSpeedNext(REBASE_RETURN_SPEED_DEFAULT);
            handler.setBlendInDurationNext(0.0f);
            handler.playAtTimeRebasedFromCurrentReturnToOrigin(this.startClip, speedMultiplier, false, resolvedStartTime);
        } else if (resolvedStartTime > 0.0f) {
            handler.playAtTime(this.startClip, speedMultiplier, true, false, resolvedStartTime);
        } else {
            handler.play(this.startClip, speedMultiplier, true, false);
        }
        this.phase = Phase.START;
        this.activeClip = this.startClip;
        logPhaseTransition("start", this.startClip, startTime);
    }

    private void startLoopPhase() {
        if (this.loopClip == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client != null ? client.field_1724 : null);
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        handler.setIgnoreDualHanded(isAnyWalkOnWaterArmBlocked(client));
        handler.setActiveArm(resolveActiveArmForClip(this.loopClip));
        handler.setLooping(true);
        handler.setFrozen(false);
        handler.setPaused(!isMovementKeyActive(client));
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(this.loopClip);
        if (hasPoseToRebase(handler)) {
            handler.setRebaseReturnSpeedNext(REBASE_RETURN_SPEED_DEFAULT);
            handler.setBlendInDurationNext(0.0f);
            handler.playAtTimeRebasedFromCurrentReturnToOrigin(this.loopClip, speedMultiplier, false, 0.0f);
        } else {
            handler.play(this.loopClip, speedMultiplier, true, false);
        }
        this.phase = Phase.LOOP;
        this.activeClip = this.loopClip;
        logPhaseTransition("loop", this.loopClip);
    }

    private void startEndPhase() {
        if (this.endClip == null) {
            resetState();
            return;
        }
        PunchyAnimationManager.setSourceHand(class_310.method_1551(), class_1268.field_5808);
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        AnimationClip secondaryBeforeEnd = handler.getSecondaryClip();
        class_1306 secondaryArmBeforeEnd = handler.getSecondaryArm();
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        handler.setIgnoreDualHanded(isAnyWalkOnWaterArmBlocked(class_310.method_1551()));
        handler.setActiveArm(resolveActiveArmForClip(this.endClip));
        handler.setLooping(false);
        handler.setFrozen(false);
        handler.setPaused(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(this.endClip);
        if (hasPoseToRebase(handler)) {
            handler.setRebaseReturnSpeedNext(REBASE_RETURN_SPEED_END);
            handler.setBlendInDurationNext(0.0f);
            handler.playAtTimeRebasedFromCurrentReturnToOrigin(this.endClip, speedMultiplier, false, 0.0f);
        } else {
            handler.play(this.endClip, speedMultiplier, true, false);
        }
        if (isWalkOnWaterPhaseClip(secondaryBeforeEnd) && secondaryArmBeforeEnd != null) {
            if (!isArmBlockedByWalkOnWaterFlag(class_310.method_1551(), secondaryArmBeforeEnd)) {
                handler.setSecondaryClipAtTime(this.endClip, secondaryArmBeforeEnd, speedMultiplier, 0.0f);
                handler.setSecondaryPaused(false);
            } else {
                handler.clearSecondaryClip();
            }
        }
        this.phase = Phase.END;
        this.activeClip = this.endClip;
        logPhaseTransition("end", this.endClip);
    }

    private void stopAndReset(boolean immediate) {
        if (this.phase == Phase.NONE) {
            resetState();
            return;
        }
        this.handoffClip = this.activeClip;
        if (ownsCurrentClip()) {
            PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            if (immediate) {
                PunchyAnimationManager.POSE_HANDLER.stopWithoutClear();
            } else {
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            }
        }
        ItemAnimationManager.clearPersistentType(class_1268.field_5808, "walk_on_water_v2");
        PunchyAnimationManager.clearSourceHand();
        resetState();
    }

    private void resetState() {
        PunchyAnimationManager.POSE_HANDLER.clearSecondaryClip();
        this.phase = Phase.NONE;
        this.activeEntry = null;
        this.startClip = null;
        this.loopClip = null;
        this.endClip = null;
        this.activeClip = null;
        this.lastStack = class_1799.field_8037;
        this.lastLoggedPhase = Phase.NONE;
        this.suspendedByForeignClip = false;
        this.suspendedPhase = Phase.NONE;
        this.suspendedTime = 0.0f;
        this.pauseLoopAfterResumeBlend = false;
        this.inWaterTriggerGapTicks = 0;
        this.unsupportedWaterGraceTicks = 0;
        this.handoffClip = null;
    }

    private void forceDisableReset() {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler != null) {
            handler.clearSecondaryClip();
            handler.setLooping(false);
            if (ownsCurrentClip()) {
                handler.stopWithoutClear();
            }
        }
        ItemAnimationManager.clearPersistentType(class_1268.field_5808, "walk_on_water_v2");
        PunchyAnimationManager.clearSourceHand();
        this.handoffClip = null;
        resetState();
    }

    private boolean isWalkOnWaterSuppressedByFishingRod(class_310 client) {
        class_1306 arm = resolveWalkOnWaterActiveArm(client);
        boolean suppressed = arm == null;
        if (client != null && client.field_1724 != null) {
            debugWalkOnWater("suppressCheck", "suppressed=" + suppressed + " main=" + stackId(client.field_1724.method_6047()) + " off=" + stackId(client.field_1724.method_6079()) + " mainDisabled=" + hasFishingRodWalkDisable(client.field_1724.method_6047()) + " offDisabled=" + hasFishingRodWalkDisable(client.field_1724.method_6079()) + " resolvedArm=" + String.valueOf(arm));
        }
        return suppressed;
    }

    private boolean hasFishingRodWalkDisable(class_1799 stack) {
        return PunchyTuningConfig.isWalkOnWaterDisabledForStack(stack);
    }

    private WalkOnWaterSelection resolveSelection(class_310 client, class_1799 stack, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (client == null || entry == null) {
            return WalkOnWaterSelection.INVALID;
        }
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForEntry(stack, entry);
        AnimationClip start = resolveNamedClip(CustomAnimationResolver.resolveStartName(entry), client, preferredOption);
        AnimationClip loop = resolveNamedClip(CustomAnimationResolver.resolveLoopingName(entry), client, preferredOption);
        AnimationClip end = resolveNamedClip(CustomAnimationResolver.resolveEndName(entry), client, preferredOption);
        if (start == null || loop == null || end == null) {
            return WalkOnWaterSelection.INVALID;
        }
        return new WalkOnWaterSelection(start, loop, end);
    }

    private AnimationClip resolveNamedClip(String name, class_310 client, String preferredOption) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return PunchyAnimationManager.resolveNamedClip(name, client, class_1268.field_5808, preferredOption);
    }

    private TriggerState evaluateTrigger(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return new TriggerState(false, "client_or_player_missing");
        }
        if (client.field_1724.method_7325() || !client.field_1724.method_5805()) {
            return new TriggerState(false, "spectator_or_dead");
        }
        if (client.field_1724.method_5681()) {
            return new TriggerState(false, "swimming_or_submerged");
        }
        if (client.field_1724.method_31549().field_7479 || client.field_1724.method_6128() || client.field_1724.method_5854() != null) {
            return new TriggerState(false, "flying_or_vehicle");
        }
        if (shouldTriggerOnSolidUnderwaterFloor(client)) {
            return new TriggerState(true, "supported_underwater_floor");
        }
        if (client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517)) {
            return new TriggerState(false, "swimming_or_submerged");
        }
        class_638 class_638Var = client.field_1687;
        class_238 bounds = client.field_1724.method_5829();
        double[] sampleYs = {bounds.field_1322 + 0.01d, bounds.field_1322 + 0.08d, bounds.field_1322 + 0.16d};
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        int length = sampleYs.length;
        for (int i = 0; i < length; i += LOG_WALK_ON_WATER_DEBUG) {
            double sampleY = sampleYs[i];
            int length2 = sampleXs.length;
            for (int i2 = 0; i2 < length2; i2 += LOG_WALK_ON_WATER_DEBUG) {
                double sampleX = sampleXs[i2];
                int length3 = sampleZs.length;
                for (int i3 = 0; i3 < length3; i3 += LOG_WALK_ON_WATER_DEBUG) {
                    double sampleZ = sampleZs[i3];
                    class_2338 waterPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                    class_3610 fluidState = class_638Var.method_8316(waterPos);
                    if (fluidState.method_15767(class_3486.field_15517) && !class_638Var.method_8316(waterPos.method_10084()).method_15767(class_3486.field_15517) && !class_638Var.method_8316(waterPos.method_10074()).method_15767(class_3486.field_15517) && class_638Var.method_8320(waterPos.method_10074()).method_51366()) {
                        String reason = "water@" + waterPos.method_10263() + "," + waterPos.method_10264() + "," + waterPos.method_10260() + " below=" + waterPos.method_10074().method_10263() + "," + waterPos.method_10074().method_10264() + "," + waterPos.method_10074().method_10260();
                        return new TriggerState(true, reason);
                    }
                }
            }
        }
        return new TriggerState(false, "no_shallow_water_sample");
    }

    private boolean shouldTriggerOnSolidUnderwaterFloor(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return false;
        }
        if ((!client.field_1724.method_5869() && !client.field_1724.method_5777(class_3486.field_15517)) || !hasSolidSupportAtFeet(client)) {
            return false;
        }
        return hasWaterContactAnyDepth(client);
    }

    private TriggerState applyInWaterTriggerGap(class_310 client, TriggerState triggerState) {
        if (triggerState == null) {
            this.inWaterTriggerGapTicks = 0;
            return new TriggerState(false, "null_trigger_state");
        }
        if (triggerState.active()) {
            this.inWaterTriggerGapTicks = 0;
            return triggerState;
        }
        if (!"no_shallow_water_sample".equals(triggerState.reason())) {
            if (shouldHoldTriggerAfterLosingSupport(client, triggerState) && this.inWaterTriggerGapTicks < IN_WATER_TRIGGER_GAP_TICKS) {
                this.inWaterTriggerGapTicks += LOG_WALK_ON_WATER_DEBUG;
                return new TriggerState(true, "gap_in_water_" + this.inWaterTriggerGapTicks);
            }
            this.inWaterTriggerGapTicks = 0;
            return triggerState;
        }
        if (this.phase == Phase.NONE && !this.suspendedByForeignClip) {
            this.inWaterTriggerGapTicks = 0;
            return triggerState;
        }
        if (!hasWaterContact(client)) {
            this.inWaterTriggerGapTicks = 0;
            return triggerState;
        }
        if (this.inWaterTriggerGapTicks < IN_WATER_TRIGGER_GAP_TICKS) {
            this.inWaterTriggerGapTicks += LOG_WALK_ON_WATER_DEBUG;
            return new TriggerState(true, "gap_in_water_" + this.inWaterTriggerGapTicks);
        }
        return triggerState;
    }

    private TriggerState applyUnsupportedWaterGrace(class_310 client, TriggerState triggerState) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            this.unsupportedWaterGraceTicks = 0;
            return triggerState == null ? new TriggerState(false, "null_trigger_state") : triggerState;
        }
        if (triggerState != null && triggerState.active()) {
            this.unsupportedWaterGraceTicks = 0;
            return triggerState;
        }
        if (this.phase == Phase.NONE && !this.suspendedByForeignClip) {
            this.unsupportedWaterGraceTicks = 0;
            return triggerState;
        }
        if (client.field_1724.method_5681() || client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517)) {
            this.unsupportedWaterGraceTicks = 0;
            return triggerState;
        }
        if (hasSolidSupportAtFeet(client) || !hasWaterContactAnyDepth(client)) {
            this.unsupportedWaterGraceTicks = 0;
            return triggerState;
        }
        if (this.unsupportedWaterGraceTicks < IN_WATER_TRIGGER_GAP_TICKS) {
            this.unsupportedWaterGraceTicks += LOG_WALK_ON_WATER_DEBUG;
            return new TriggerState(true, "unsupported_water_grace_" + this.unsupportedWaterGraceTicks);
        }
        return triggerState;
    }

    private boolean shouldHoldTriggerAfterLosingSupport(class_310 client, TriggerState triggerState) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return false;
        }
        if ((this.phase == Phase.NONE && !this.suspendedByForeignClip) || triggerState == null || triggerState.active() || client.field_1724.method_5681() || client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517) || hasSolidSupportAtFeet(client)) {
            return false;
        }
        return hasWaterContactAnyDepth(client);
    }

    private boolean shouldPauseForInWaterGapWithoutSupport(class_310 client, TriggerState triggerState) {
        return (triggerState == null || !triggerState.active() || triggerState.reason() == null || !triggerState.reason().startsWith("gap_in_water_") || hasSolidSupportAtFeet(client)) ? false : true;
    }

    private boolean hasWaterContact(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null || client.field_1724.method_5681() || client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517)) {
            return false;
        }
        return hasWaterContactAnyDepth(client);
    }

    private boolean hasWaterContactAnyDepth(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return false;
        }
        class_638 class_638Var = client.field_1687;
        class_238 bounds = client.field_1724.method_5829();
        double[] sampleYs = {bounds.field_1322 + 0.01d, bounds.field_1322 + 0.08d, bounds.field_1322 + 0.16d, Math.min(bounds.field_1325 - 0.05d, bounds.field_1322 + 0.45d)};
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        int length = sampleYs.length;
        for (int i = 0; i < length; i += LOG_WALK_ON_WATER_DEBUG) {
            double sampleY = sampleYs[i];
            int length2 = sampleXs.length;
            for (int i2 = 0; i2 < length2; i2 += LOG_WALK_ON_WATER_DEBUG) {
                double sampleX = sampleXs[i2];
                int length3 = sampleZs.length;
                for (int i3 = 0; i3 < length3; i3 += LOG_WALK_ON_WATER_DEBUG) {
                    double sampleZ = sampleZs[i3];
                    if (class_638Var.method_8316(class_2338.method_49637(sampleX, sampleY, sampleZ)).method_15767(class_3486.field_15517)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean shouldHandOffToSwimAbove(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null || this.phase == Phase.NONE || client.field_1724.method_5681() || client.field_1724.method_5869() || client.field_1724.method_5777(class_3486.field_15517) || hasSolidSupportAtFeet(client)) {
            return false;
        }
        return hasWaterContactAnyDepth(client);
    }

    private boolean hasSolidSupportAtFeet(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return false;
        }
        class_638 class_638Var = client.field_1687;
        class_238 bounds = client.field_1724.method_5829();
        double sampleY = bounds.field_1322 - 0.05d;
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        int length = sampleXs.length;
        for (int i = 0; i < length; i += LOG_WALK_ON_WATER_DEBUG) {
            double sampleX = sampleXs[i];
            int length2 = sampleZs.length;
            for (int i2 = 0; i2 < length2; i2 += LOG_WALK_ON_WATER_DEBUG) {
                double sampleZ = sampleZs[i2];
                class_2338 supportPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                if (class_638Var.method_8320(supportPos).method_51366()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean ownsCurrentClip() {
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        return current != null && (current == this.activeClip || current == this.startClip || current == this.loopClip || current == this.endClip);
    }

    private boolean isPlaying(AnimationClip clip) {
        return clip != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip;
    }

    private boolean isCurrentOrBlending(AnimationClip clip) {
        return clip != null && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip;
    }

    private boolean hasPoseToRebase(PoseHandler handler) {
        return (handler == null || handler.getCurrentClip() == null || (!handler.isPlaying() && !handler.isBlendOutActive())) ? false : true;
    }

    private void captureOwnedPhaseTime() {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler == null || this.activeClip == null || handler.getCurrentClip() != this.activeClip) {
            return;
        }
        if (!handler.isPlaying() && !handler.isBlendOutActive()) {
            return;
        }
        this.suspendedPhase = this.phase;
        this.suspendedTime = handler.getCurrentTime();
    }

    private boolean shouldYieldToForeignClip() {
        PoseHandler handler;
        AnimationClip current;
        if (this.phase == Phase.NONE || (handler = PunchyAnimationManager.POSE_HANDLER) == null || (current = handler.getCurrentClip()) == null || current == this.startClip || current == this.loopClip || current == this.endClip) {
            return false;
        }
        return handler.isPlaying();
    }

    private void suspendForForeignClip() {
        if (!this.suspendedByForeignClip) {
            this.suspendedByForeignClip = true;
            this.suspendedPhase = this.phase;
        }
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler != null && handler.getSecondaryClip() == this.activeClip) {
            AnimationClip foreign = handler.getCurrentClip();
            if (PunchyAnimationManager.hasAnyDualHandedEffect(foreign)) {
                handler.clearSecondaryClip();
                debugWalkOnWater("clearSecondaryForDualForeign", "foreign=" + clipName(foreign) + " active=" + clipName(this.activeClip));
                return;
            }
        }
        if (!HandEquipStateMachine.isOffhandEquipTransitionActive()) {
            keepOppositeArmWalkingOnWater();
        }
    }

    private void resumeFromSuspendedState(boolean triggerActive) {
        if (shouldYieldToForeignClip()) {
            return;
        }
        syncSuspendedTimeFromBackgroundClip();
        Phase resumePhase = resolveResumePhaseFromBackgroundClip();
        debugWalkOnWater("resume", "trigger=" + triggerActive + " suspendedPhase=" + String.valueOf(this.suspendedPhase) + " resumePhase=" + String.valueOf(resumePhase) + " suspendedTime=" + this.suspendedTime + " active=" + clipName(this.activeClip) + " secondary=" + clipName(PunchyAnimationManager.POSE_HANDLER.getSecondaryClip()));
        boolean resumeAllowed = triggerActive && (resumePhase == Phase.START || resumePhase == Phase.LOOP);
        if (resumeAllowed) {
            resumePhaseClip(resumePhase, this.suspendedTime);
        } else if (!triggerActive && this.phase != Phase.END) {
            startEndPhase();
        }
        this.suspendedByForeignClip = false;
        this.suspendedPhase = Phase.NONE;
        this.suspendedTime = 0.0f;
        if (!resumeAllowed) {
            this.pauseLoopAfterResumeBlend = false;
        }
    }

    public void reattachMainhandAfterEquip(class_746 player) {
        PoseHandler handler;
        if (player == null || !this.suspendedByForeignClip || (handler = PunchyAnimationManager.POSE_HANDLER) == null) {
            return;
        }
        boolean hasOffhandBackground = false;
        class_1306 offArm = resolveArm(player, class_1268.field_5810);
        if (offArm != null && handler.getSecondaryArm() == offArm && handler.getSecondaryClip() != null) {
            hasOffhandBackground = LOG_WALK_ON_WATER_DEBUG;
            syncSuspendedTimeFromBackgroundClip();
        }
        Phase resumePhase = hasOffhandBackground ? resolveResumePhaseFromBackgroundClip() : this.suspendedPhase;
        if (triggerActiveNow()) {
            if (resumePhase != Phase.START && resumePhase != Phase.LOOP) {
                return;
            }
            if (!hasOffhandBackground && offArm != null) {
                resumePhaseClipWithExplicitOffhand(resumePhase, this.suspendedTime, offArm);
            } else {
                resumePhaseClip(resumePhase, this.suspendedTime);
            }
            this.suspendedByForeignClip = false;
            this.suspendedPhase = Phase.NONE;
            this.suspendedTime = 0.0f;
        }
    }

    private void syncSuspendedTimeFromBackgroundClip() {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler == null || this.activeClip == null || handler.getSecondaryClip() != this.activeClip) {
            return;
        }
        this.suspendedTime = Math.max(0.0f, handler.getSecondaryTime());
    }

    private Phase resolveResumePhaseFromBackgroundClip() {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler == null) {
            return this.suspendedPhase;
        }
        AnimationClip secondary = handler.getSecondaryClip();
        if (secondary == null) {
            return this.suspendedPhase;
        }
        if (secondary == this.loopClip) {
            return Phase.LOOP;
        }
        if (secondary == this.startClip) {
            return Phase.START;
        }
        return this.suspendedPhase;
    }

    private void resumePhaseClip(Phase phaseToResume, float time) {
        AnimationClip animationClip;
        switch (phaseToResume.ordinal()) {
            case LOG_WALK_ON_WATER_DEBUG /* 1 */:
                animationClip = this.startClip;
                break;
            case 2:
                animationClip = this.loopClip;
                break;
            default:
                animationClip = null;
                break;
        }
        AnimationClip clip = animationClip;
        if (clip == null) {
            if (triggerActiveNow()) {
                startStartPhase();
                return;
            } else {
                resetState();
                return;
            }
        }
        class_310 client = class_310.method_1551();
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client != null ? client.field_1724 : null);
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        boolean splitResumeFromOffhandBackground = shouldSplitResumeFromOffhandBackground(handler, clip, client);
        float preservedSecondaryTime = splitResumeFromOffhandBackground ? handler.getSecondaryTime() : 0.0f;
        class_1306 preservedSecondaryArm = splitResumeFromOffhandBackground ? handler.getSecondaryArm() : null;
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        handler.setIgnoreDualHanded(splitResumeFromOffhandBackground || isAnyWalkOnWaterArmBlocked(client));
        handler.setActiveArm(splitResumeFromOffhandBackground ? resolveActiveArm() : resolveActiveArmForClip(clip));
        handler.setLooping(phaseToResume == Phase.LOOP);
        handler.setFrozen(false);
        boolean pausedLoop = phaseToResume == Phase.LOOP && !isMovementKeyActive(client);
        handler.setPaused(false);
        handler.setBlendInDurationNext(0.0f);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        float startAt = Math.max(0.0f, time);
        handler.setRebaseReturnSpeedNext(REBASE_RETURN_SPEED_RESUME);
        handler.playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, false, startAt);
        if (splitResumeFromOffhandBackground && preservedSecondaryArm != null) {
            if (handler.getSecondaryClip() != clip || handler.getSecondaryArm() != preservedSecondaryArm) {
                handler.setSecondaryClipAtTime(clip, preservedSecondaryArm, speedMultiplier, preservedSecondaryTime);
            }
            handler.setSecondaryPaused(phaseToResume == Phase.LOOP && !isMovementKeyActive(client));
        }
        this.pauseLoopAfterResumeBlend = pausedLoop;
        this.phase = phaseToResume;
        this.activeClip = clip;
        logPhaseTransition(phaseToResume == Phase.START ? "resume_start" : "resume_loop", clip, Float.valueOf(startAt));
    }

    private void resumePhaseClipWithExplicitOffhand(Phase phaseToResume, float time, class_1306 offArm) {
        AnimationClip animationClip;
        switch (phaseToResume.ordinal()) {
            case LOG_WALK_ON_WATER_DEBUG /* 1 */:
                animationClip = this.startClip;
                break;
            case 2:
                animationClip = this.loopClip;
                break;
            default:
                animationClip = null;
                break;
        }
        AnimationClip clip = animationClip;
        if (clip == null) {
            if (triggerActiveNow()) {
                startStartPhase();
                return;
            } else {
                resetState();
                return;
            }
        }
        class_310 client = class_310.method_1551();
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        handler.clearRequestedArmFilters();
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        boolean dualHanded = !isAnyWalkOnWaterArmBlocked(client) && PunchyAnimationManager.isDualHandedActive(client, clip, 0.0f);
        debugWalkOnWater("resumeExplicitOffhand.begin", "phaseToResume=" + String.valueOf(phaseToResume) + " time=" + time + " offArm=" + String.valueOf(offArm) + " dualHanded=" + dualHanded + " current=" + clipName(handler.getCurrentClip()) + " secondary=" + clipName(handler.getSecondaryClip()) + " activeArm=" + String.valueOf(handler.getActiveArm()) + " secondaryArm=" + String.valueOf(handler.getSecondaryArm()));
        handler.setIgnoreDualHanded(false);
        handler.setActiveArm(dualHanded ? null : resolveActiveArm());
        handler.setLooping(phaseToResume == Phase.LOOP);
        handler.setFrozen(false);
        handler.setPaused(false);
        handler.setBlendInDurationNext(0.0f);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        float startAt = Math.max(0.0f, time);
        handler.setRebaseReturnSpeedNext(REBASE_RETURN_SPEED_RESUME);
        handler.setRebaseUseBothArmsForNextPlay(dualHanded);
        handler.playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, false, startAt);
        debugWalkOnWater("resumeExplicitOffhand.afterPlay", "current=" + clipName(handler.getCurrentClip()) + " currentTime=" + handler.getCurrentTime() + " activeArm=" + String.valueOf(handler.getActiveArm()) + " secondary=" + clipName(handler.getSecondaryClip()) + " secondaryArm=" + String.valueOf(handler.getSecondaryArm()));
        boolean pausedLoop = phaseToResume == Phase.LOOP && !isMovementKeyActive(client);
        if (!dualHanded) {
            float syncedTime = Math.max(0.0f, handler.getCurrentTime());
            if (!isArmBlockedByWalkOnWaterFlag(client, offArm)) {
                handler.setSecondaryClipAtTimeWithBlendFromCurrent(clip, offArm, speedMultiplier, syncedTime);
                handler.requestBlendInArm(offArm);
                handler.setSecondaryPaused(pausedLoop);
            } else {
                handler.clearSecondaryClip();
            }
            debugWalkOnWater("resumeExplicitOffhand.secondary", "syncedTime=" + syncedTime + " secondary=" + clipName(handler.getSecondaryClip()) + " secondaryArm=" + String.valueOf(handler.getSecondaryArm()) + " secondaryTime=" + handler.getSecondaryTime() + " paused=" + pausedLoop);
        } else {
            handler.clearSecondaryClip();
        }
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client != null ? client.field_1724 : null);
        debugWalkOnWater("resumeExplicitOffhand.end", "current=" + clipName(handler.getCurrentClip()) + " currentTime=" + handler.getCurrentTime() + " activeArm=" + String.valueOf(handler.getActiveArm()) + " secondary=" + clipName(handler.getSecondaryClip()) + " secondaryArm=" + String.valueOf(handler.getSecondaryArm()) + " secondaryTime=" + handler.getSecondaryTime());
        this.pauseLoopAfterResumeBlend = pausedLoop;
        this.phase = phaseToResume;
        this.activeClip = clip;
        logPhaseTransition(phaseToResume == Phase.START ? "resume_start_offhand" : "resume_loop_offhand", clip, Float.valueOf(Math.max(0.0f, handler.getCurrentTime())));
    }

    private boolean shouldSplitResumeFromOffhandBackground(PoseHandler handler, AnimationClip clip, class_310 client) {
        class_1306 offArm;
        if (handler == null || clip == null || client == null || client.field_1724 == null) {
            return false;
        }
        AnimationClip secondary = handler.getSecondaryClip();
        return secondary == clip && (offArm = resolveArm(client.field_1724, class_1268.field_5810)) != null && handler.getSecondaryArm() == offArm;
    }

    private boolean triggerActiveNow() {
        class_310 client = class_310.method_1551();
        return evaluateTrigger(client).active();
    }

    public boolean isTriggerActiveOrInGap(class_310 client) {
        if (client == null) {
            return false;
        }
        return applyInWaterTriggerGap(client, evaluateTrigger(client)).active();
    }

    private void keepOppositeArmWalkingOnWater() {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler == null || this.activeClip == null || this.suspendedPhase == Phase.END || isOffhandEquipCurrentlyVisible(handler)) {
            return;
        }
        boolean movementActive = isMovementKeyActive(class_310.method_1551());
        class_1306 foreignArm = handler.getActiveArm();
        if (foreignArm == null) {
            return;
        }
        class_1306 overlapArm = foreignArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
        class_1306 walkArm = resolveActiveArmForClip(this.activeClip);
        if ((walkArm != null && overlapArm != walkArm) || isArmBlockedByWalkOnWaterFlag(class_310.method_1551(), overlapArm)) {
            return;
        }
        AnimationClip secondary = handler.getSecondaryClip();
        if (secondary == this.activeClip && handler.getSecondaryArm() == overlapArm) {
            handler.setSecondaryPaused(!movementActive && this.activeClip == this.loopClip);
            return;
        }
        float clipLength = this.activeClip.getLength();
        float startAt = Math.max(0.0f, this.suspendedTime);
        if (clipLength > 0.0f) {
            startAt = this.activeClip.isLoop() ? startAt % clipLength : Math.min(startAt, clipLength);
        }
        handler.setSecondaryClipAtTime(this.activeClip, overlapArm, PunchyAnimationManager.clipSpeedMultiplier(this.activeClip), startAt);
        handler.setSecondaryPaused(!movementActive && this.activeClip == this.loopClip);
        debugWalkOnWater("keepOpposite", "active=" + clipName(this.activeClip) + " overlapArm=" + String.valueOf(overlapArm) + " startAt=" + startAt + " movement=" + movementActive);
    }

    private boolean isOffhandEquipCurrentlyVisible(PoseHandler handler) {
        class_1306 offArm;
        class_310 client = class_310.method_1551();
        if (handler == null || client == null || client.field_1724 == null || (offArm = resolveArm(client.field_1724, class_1268.field_5810)) == null) {
            return false;
        }
        AnimationClip current = handler.getCurrentClip();
        if (current != null && isEquipClip(current) && (handler.getActiveArm() == null || handler.getActiveArm() == offArm)) {
            return true;
        }
        AnimationClip secondary = handler.getSecondaryClip();
        return secondary != null && isEquipClip(secondary) && handler.getSecondaryArm() == offArm;
    }

    private boolean isEquipClip(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        String name = clip.getName();
        if (name != null) {
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("hand_in") || normalized.equals("hand_out") || normalized.startsWith("hand_in_") || normalized.startsWith("hand_out_")) {
                return true;
            }
        }
        return PunchyAnimationManager.isClipNamed(clip, "hand_in") || PunchyAnimationManager.isClipNamed(clip, "hand_out");
    }

    private void debugWalkOnWater(String event, String details) {
    }

    private String stackId(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return "empty";
        }
        class_2960 id = class_7923.field_41178.method_10221(stack.method_7909());
        return id == null ? "unknown" : id.toString();
    }

    private boolean shouldStopEndClipEarly(class_310 client) {
        Float stopTime;
        return (client == null || client.field_1724 == null || this.endClip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.getCurrentClip() != this.endClip || client.field_1724.method_6079() == null || client.field_1724.method_6079().method_7960() || (stopTime = findTimelineEffectTime(this.endClip, END_STOP_WITH_OFFHAND_EFFECT)) == null || PunchyAnimationManager.POSE_HANDLER.getCurrentTime() < stopTime.floatValue()) ? false : true;
    }

    private Float resolveStartClipTime(class_310 client) {
        if (client == null || client.field_1724 == null || this.startClip == null || client.field_1724.method_6079() == null || client.field_1724.method_6079().method_7960()) {
            return null;
        }
        Float startTime = findTimelineEffectTime(this.startClip, START_WITH_OFFHAND_EFFECT);
        if (startTime != null) {
            return startTime;
        }
        return findTimelineEffectTime(this.startClip, START_WITH_OFFHAND_EFFECT_ALIAS);
    }

    private Float findTimelineEffectTime(AnimationClip clip, String effectName) {
        if (clip == null || effectName == null || effectName.isBlank()) {
            return null;
        }
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffects().entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                for (String effect : entry.getValue()) {
                    if (effect != null && effectName.equalsIgnoreCase(effect.trim())) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    private boolean isMovementKeyActive(class_310 client) {
        if (client == null || client.field_1690 == null) {
            return false;
        }
        return client.field_1690.field_1894.method_1434() || client.field_1690.field_1881.method_1434() || client.field_1690.field_1913.method_1434() || client.field_1690.field_1849.method_1434();
    }

    private class_1306 resolveActiveArm() {
        return resolveWalkOnWaterActiveArm(class_310.method_1551());
    }

    private class_1306 resolveWalkOnWaterActiveArm(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return null;
        }
        class_1306 mainArm = resolveArm(client.field_1724, class_1268.field_5808);
        class_1306 offArm = resolveArm(client.field_1724, class_1268.field_5810);
        boolean mainBlocked = hasFishingRodWalkDisable(client.field_1724.method_6047());
        boolean offBlocked = hasFishingRodWalkDisable(client.field_1724.method_6079());
        if (mainBlocked && offBlocked) {
            debugWalkOnWater("resolveArm", "both_arms_blocked=true");
            return null;
        }
        if (mainBlocked) {
            return offArm;
        }
        if (offBlocked) {
            return mainArm;
        }
        class_1306 resolved = PunchyAnimationManager.getUseArm(client, class_1268.field_5808);
        debugWalkOnWater("resolveArm", "mainBlocked=" + mainBlocked + " offBlocked=" + offBlocked + " resolved=" + String.valueOf(resolved));
        return resolved;
    }

    private boolean isArmBlockedByWalkOnWaterFlag(class_310 client, class_1306 arm) {
        if (client == null || client.field_1724 == null || arm == null) {
            return false;
        }
        class_1306 mainArm = resolveArm(client.field_1724, class_1268.field_5808);
        class_1306 offArm = resolveArm(client.field_1724, class_1268.field_5810);
        if (arm == mainArm) {
            return hasFishingRodWalkDisable(client.field_1724.method_6047());
        }
        if (arm == offArm) {
            return hasFishingRodWalkDisable(client.field_1724.method_6079());
        }
        return false;
    }

    private class_1306 resolveArm(class_746 player, class_1268 hand) {
        if (player == null || hand == null) {
            return null;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return hand == class_1268.field_5808 ? mainRight ? class_1306.field_6183 : class_1306.field_6182 : mainRight ? class_1306.field_6182 : class_1306.field_6183;
    }

    private class_1306 resolveActiveArmForClip(AnimationClip clip) {
        if (isAnyWalkOnWaterArmBlocked(class_310.method_1551())) {
            return resolveActiveArm();
        }
        if (PunchyAnimationManager.isDualHandedActive(class_310.method_1551(), clip, 0.0f)) {
            return null;
        }
        return resolveActiveArm();
    }

    private boolean isAnyWalkOnWaterArmBlocked(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        return hasFishingRodWalkDisable(client.field_1724.method_6047()) || hasFishingRodWalkDisable(client.field_1724.method_6079());
    }

    private boolean hasStackChanged(class_1799 previous, class_1799 current) {
        class_1799 prev = previous == null ? class_1799.field_8037 : previous;
        class_1799 now = current == null ? class_1799.field_8037 : current;
        return !class_1799.method_31577(prev, now);
    }

    private class_1799 copyStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return class_1799.field_8037;
        }
        return stack.method_7972();
    }

    private boolean matchesActiveSelection(WalkOnWaterSelection selection) {
        return selection != null && selection.isValid() && selection.start() == this.startClip && selection.loop() == this.loopClip && selection.end() == this.endClip;
    }

    private boolean isWalkOnWaterPhaseClip(AnimationClip clip) {
        return clip != null && (clip == this.startClip || clip == this.loopClip || clip == this.endClip);
    }

    public boolean isCurrentPoseOwnedByWalkOnWater() {
        if (this.phase != Phase.NONE && ownsCurrentClip()) {
            return true;
        }
        if (this.handoffClip == null) {
            return false;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current == this.handoffClip && (PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.isBlendOutActive())) {
            return true;
        }
        this.handoffClip = null;
        return false;
    }

    public boolean isActiveOrHandingOff() {
        if (this.phase != Phase.NONE || this.suspendedByForeignClip) {
            return true;
        }
        if (this.handoffClip == null) {
            return false;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (current == this.handoffClip && (PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.isBlendOutActive())) {
            return true;
        }
        this.handoffClip = null;
        return false;
    }

    private void logTriggerState(class_310 client, PunchyTuningConfig.CustomAnimationEntry entry, WalkOnWaterSelection selection, TriggerState triggerState) {
        this.lastTriggerActive = Boolean.valueOf(triggerState.active());
        this.lastTriggerReason = triggerState.reason();
    }

    private void logResolvedSelection(PunchyTuningConfig.CustomAnimationEntry entry, WalkOnWaterSelection selection) {
        String summary = "entry=" + (entry == null ? "null" : entry.type) + " start=" + clipName(selection.start()) + " loop=" + clipName(selection.loop()) + " end=" + clipName(selection.end()) + " valid=" + selection.isValid();
        if (safeEquals(this.lastResolvedSummary, summary)) {
            return;
        }
        this.lastResolvedSummary = summary;
    }

    private void logPhaseTransition(String targetPhase, AnimationClip clip) {
        logPhaseTransition(targetPhase, clip, null);
    }

    private void logPhaseTransition(String targetPhase, AnimationClip clip, Float startTime) {
        if (this.phase == this.lastLoggedPhase && this.activeClip == clip) {
            return;
        }
        this.lastLoggedPhase = this.phase;
    }

    private static String clipName(AnimationClip clip) {
        return clip == null ? "null" : clip.getName();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection.class */
    private static final class WalkOnWaterSelection extends Record {
        private final AnimationClip start;
        private final AnimationClip loop;
        private final AnimationClip end;
        private static final WalkOnWaterSelection INVALID = new WalkOnWaterSelection(null, null, null);

        private WalkOnWaterSelection(AnimationClip start, AnimationClip loop, AnimationClip end) {
            this.start = start;
            this.loop = loop;
            this.end = end;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, WalkOnWaterSelection.class), WalkOnWaterSelection.class, "start;loop;end", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, WalkOnWaterSelection.class), WalkOnWaterSelection.class, "start;loop;end", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, WalkOnWaterSelection.class, Object.class), WalkOnWaterSelection.class, "start;loop;end", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$WalkOnWaterSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip start() {
            return this.start;
        }

        public AnimationClip loop() {
            return this.loop;
        }

        public AnimationClip end() {
            return this.end;
        }

        private boolean isValid() {
            return (this.start == null || this.loop == null || this.end == null) ? false : true;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/WalkOnWaterStateMachine$TriggerState.class */
    private static final class TriggerState extends Record {
        private final boolean active;
        private final String reason;

        private TriggerState(boolean active, String reason) {
            this.active = active;
            this.reason = reason;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, TriggerState.class), TriggerState.class, "active;reason", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->active:Z", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->reason:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, TriggerState.class), TriggerState.class, "active;reason", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->active:Z", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->reason:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, TriggerState.class, Object.class), TriggerState.class, "active;reason", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->active:Z", "FIELD:Lpunchy/client/state/WalkOnWaterStateMachine$TriggerState;->reason:Ljava/lang/String;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public boolean active() {
            return this.active;
        }

        public String reason() {
            return this.reason;
        }
    }
}
