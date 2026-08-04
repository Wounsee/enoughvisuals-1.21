package punchy.client.state;

import java.util.Map;
import java.util.Set;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3610;
import net.minecraft.class_638;
import net.minecraft.class_746;
import punchy.client.PunchyClient;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/DryOutStateMachine.class */
public final class DryOutStateMachine {
    private static final int MIN_WET_TICKS = 40;
    private static final float DRY_OUT_REBASE_SPEED = 3.4f;
    private static final String START_WITH_OFFHAND_EFFECT = "start_here_if_off_hand_item";
    private static final String START_WITH_OFFHAND_EFFECT_ALIAS = "start_if_offhand_item";
    private static final String STOP_WITH_OFFHAND_EFFECT = "stop_if_offhand_item";
    private int wetTicks;
    private boolean wasTouchingWater;
    private boolean pendingDryOut;
    private AnimationClip activeClip;

    public void tick(class_310 client) {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (client == null || client.field_1724 == null || client.field_1687 == null || handler == null) {
            resetTracking();
            return;
        }
        if (!client.field_1690.method_31044().method_31034()) {
            resetTracking();
            return;
        }
        if (client.field_1724.method_7325() || !client.field_1724.method_5805()) {
            stopIfOwned(handler);
            resetTracking();
            return;
        }
        if (resolveDryOutActiveArm(client) == null) {
            logDryOutState("suppressed", client, null);
            stopIfOwned(handler);
            resetTracking();
            return;
        }
        AnimationClip clip = PunchyAnimationManager.resolveNamedClip("dry_out", client, class_1268.field_5808);
        if (clip == null) {
            stopIfOwned(handler);
            resetTracking();
            return;
        }
        if (shouldStopForOffhand(handler)) {
            stopIfOwned(handler);
            this.pendingDryOut = false;
            this.wetTicks = 0;
            return;
        }
        boolean touchingWater = hasWaterContactAnyDepth(client);
        boolean leftWaterThisTick = this.wasTouchingWater && !touchingWater;
        this.wasTouchingWater = touchingWater;
        if (touchingWater) {
            this.wetTicks++;
            this.pendingDryOut = false;
            stopIfOwned(handler);
            return;
        }
        if (leftWaterThisTick) {
            this.pendingDryOut = this.wetTicks >= MIN_WET_TICKS;
            this.wetTicks = 0;
        } else if (!this.pendingDryOut) {
            this.wetTicks = 0;
        }
        if (!this.pendingDryOut) {
            clearFinishedClip(handler);
            return;
        }
        if (!isReadyToDryOut(client)) {
            clearFinishedClip(handler);
            return;
        }
        WalkOnWaterStateMachine walkOnWater = PunchyClient.getWalkOnWaterStateMachine();
        if (walkOnWater != null && walkOnWater.isActiveOrHandingOff()) {
            clearFinishedClip(handler);
            return;
        }
        playDryOut(client, handler, clip);
        this.pendingDryOut = false;
        this.wetTicks = 0;
    }

    private void playDryOut(class_310 client, PoseHandler handler, AnimationClip clip) {
        if (handler.getCurrentClip() == clip && (handler.isPlaying() || handler.isBlendOutActive())) {
            this.activeClip = clip;
            return;
        }
        float startTime = resolveStartTime(client, clip);
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client.field_1724);
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        handler.setIgnoreDualHanded(isAnyDryOutArmBlocked(client));
        handler.setLooping(false);
        handler.setFrozen(false);
        handler.setPaused(false);
        class_1306 activeArm = resolveActiveArm(client, clip, startTime);
        handler.setActiveArm(activeArm);
        handler.setBlendInDurationNext(0.0f);
        handler.setRebaseReturnSpeedNext(DRY_OUT_REBASE_SPEED);
        handler.playAtTimeRebasedFromCurrentReturnToOrigin(clip, PunchyAnimationManager.clipSpeedMultiplier(clip), false, startTime);
        this.activeClip = clip;
        logDryOutState("play", client, activeArm);
    }

    private class_1306 resolveActiveArm(class_310 client, AnimationClip clip, float time) {
        class_1306 filteredArm = resolveDryOutActiveArm(client);
        if (filteredArm == null) {
            return null;
        }
        if (isAnyDryOutArmBlocked(client)) {
            return filteredArm;
        }
        if (PunchyAnimationManager.isDualHandedActive(client, clip, time)) {
            return null;
        }
        return filteredArm;
    }

    private boolean isAnyDryOutArmBlocked(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        return PunchyTuningConfig.isDryOutDisabledForStack(client.field_1724.method_6047()) || PunchyTuningConfig.isDryOutDisabledForStack(client.field_1724.method_6079());
    }

    private class_1306 resolveDryOutActiveArm(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return null;
        }
        boolean mainBlocked = PunchyTuningConfig.isDryOutDisabledForStack(client.field_1724.method_6047());
        boolean offBlocked = PunchyTuningConfig.isDryOutDisabledForStack(client.field_1724.method_6079());
        class_1306 mainArm = resolveArm(client.field_1724, class_1268.field_5808);
        class_1306 offArm = resolveArm(client.field_1724, class_1268.field_5810);
        if (mainBlocked && offBlocked) {
            logDryOutState("resolveArm", client, null);
            return null;
        }
        if (mainBlocked) {
            return offArm;
        }
        if (offBlocked) {
            return mainArm;
        }
        class_1306 resolved = PunchyAnimationManager.getUseArm(client, class_1268.field_5808);
        logDryOutState("resolveArm", client, resolved);
        return resolved;
    }

    private void logDryOutState(String event, class_310 client, class_1306 resolvedArm) {
    }

    private class_1306 resolveArm(class_746 player, class_1268 hand) {
        if (player == null || hand == null) {
            return null;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return hand == class_1268.field_5808 ? mainRight ? class_1306.field_6183 : class_1306.field_6182 : mainRight ? class_1306.field_6182 : class_1306.field_6183;
    }

    private float resolveStartTime(class_310 client, AnimationClip clip) {
        if (client == null || client.field_1724 == null || clip == null || client.field_1724.method_6079() == null || client.field_1724.method_6079().method_7960()) {
            return 0.0f;
        }
        Float startTime = findTimelineEffectTime(clip, START_WITH_OFFHAND_EFFECT);
        if (startTime != null) {
            return startTime.floatValue();
        }
        Float alias = findTimelineEffectTime(clip, START_WITH_OFFHAND_EFFECT_ALIAS);
        if (alias == null) {
            return 0.0f;
        }
        return alias.floatValue();
    }

    private boolean shouldStopForOffhand(PoseHandler handler) {
        class_310 client;
        Float stopTime;
        return (handler == null || this.activeClip == null || (client = class_310.method_1551()) == null || client.field_1724 == null || !handler.isPlaying() || handler.getCurrentClip() != this.activeClip || client.field_1724.method_6079() == null || client.field_1724.method_6079().method_7960() || (stopTime = findTimelineEffectTime(this.activeClip, STOP_WITH_OFFHAND_EFFECT)) == null || handler.getCurrentTime() < stopTime.floatValue()) ? false : true;
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

    private boolean isReadyToDryOut(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1724.method_31549().field_7479 || client.field_1724.method_6128() || client.field_1724.method_5854() != null) {
            return false;
        }
        return hasDryGroundSupport(client);
    }

    private boolean hasDryGroundSupport(class_310 client) {
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
        boolean solidSupport = false;
        for (double sampleX : sampleXs) {
            for (double sampleZ : sampleZs) {
                class_2338 supportPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                if (class_638Var.method_8316(supportPos).method_15767(class_3486.field_15517)) {
                    return false;
                }
                if (class_638Var.method_8320(supportPos).method_51366()) {
                    solidSupport = true;
                }
            }
        }
        return solidSupport;
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
        for (double sampleY : sampleYs) {
            for (double sampleX : sampleXs) {
                for (double sampleZ : sampleZs) {
                    class_3610 fluidState = class_638Var.method_8316(class_2338.method_49637(sampleX, sampleY, sampleZ));
                    if (fluidState.method_15767(class_3486.field_15517)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void stopIfOwned(PoseHandler handler) {
        if (handler == null || this.activeClip == null) {
            return;
        }
        if (handler.getCurrentClip() == this.activeClip && (handler.isPlaying() || handler.isBlendOutActive())) {
            handler.stopImmediateWithBlendOut();
        }
        this.activeClip = null;
    }

    private void clearFinishedClip(PoseHandler handler) {
        if (handler == null || this.activeClip == null) {
            return;
        }
        if (handler.getCurrentClip() != this.activeClip || (!handler.isPlaying() && !handler.isBlendOutActive())) {
            this.activeClip = null;
        }
    }

    private void resetTracking() {
        this.wetTicks = 0;
        this.wasTouchingWater = false;
        this.pendingDryOut = false;
        this.activeClip = null;
    }
}
