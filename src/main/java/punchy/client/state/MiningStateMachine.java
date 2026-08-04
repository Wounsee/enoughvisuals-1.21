package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Objects;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_239;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_9334;
import net.minecraft.class_9336;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.render.ToolKindResolver;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningStateMachine.class */
public class MiningStateMachine {
    private static final long MINING_TARGET_LOCK_NANOS = 250000000;
    private static final long RECENT_TARGET_GRACE_NANOS = 90000000;
    private static final long RECENT_MINING_CHAIN_NANOS = 180000000;
    private static final long MIN_MINING_DURATION_TICKS = 2;
    private static final long MINING_V2_FASTEST_TRIGGER_TICKS = 1;
    private static final long MINING_V2_SLOWEST_TRIGGER_TICKS = 6;
    private static final float MINING_V2_START_SPEED_MIN = 1.0f;
    private static final float MINING_V2_START_SPEED_MAX = 2.35f;
    private static final float MINING_V2_START_SPEED_RESPONSE = 0.45f;
    private static final float STOP_IMMEDIATE_BLEND_OUT_SECONDS = 0.3f;
    private static final int TARGETED_GRACE_TICKS = 2;
    private static final int RECENT_TARGET_GRACE_TICKS = 1;
    private static final int RECENT_CHAIN_GRACE_TICKS = 2;
    private boolean wasMining;
    private static final long FAST_TRIGGER_WINDOW_TICKS = 4;
    private boolean stopImmediateOnRelease;
    private boolean skipBlendOut;
    private AnimationClip activeClip;
    private class_1306 activeArm;
    private PunchyTuningConfig.CustomAnimationEntry miningV2Entry;
    private AnimationClip miningV2StartClip;
    private AnimationClip miningV2LoopClip;
    private AnimationClip miningV2EndClip;
    private long lastTriggerTick = -1;
    private int stopGraceTicks = 0;
    private class_1799 lastMiningStack = class_1799.field_8037;
    private long minStopTick = -1;
    private MiningV2Phase miningV2Phase = MiningV2Phase.NONE;
    private class_1799 lastMiningV2Stack = class_1799.field_8037;
    private long miningV2LastTriggerTick = -1;
    private float miningV2StartSpeed = 1.0f;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningStateMachine$MiningV2Phase.class */
    private enum MiningV2Phase {
        NONE,
        START,
        LOOP,
        LOOP_END_PENDING,
        END
    }

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1761 == null) {
            return;
        }
        class_1799 stack = client.field_1724.method_6047();
        if (PunchyAnimationManager.isHandBlacklisted(client, class_1268.field_5808)) {
            if (this.wasMining) {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                stopMiningAnimation(client, true);
                clearMiningState();
                resetMiningV2();
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
            }
            this.wasMining = false;
            return;
        }
        boolean rawMining = (!MiningActionTracker.isMiningActive() || client.field_1724 == null || client.field_1724.method_7325()) ? false : true;
        long now = currentTick();
        boolean mining = shouldKeepMiningActive(rawMining, client, now);
        boolean triggered = MiningActionTracker.consumeTrigger();
        if (triggered && !mining && !client.field_1724.method_7325()) {
            mining = RECENT_TARGET_GRACE_TICKS;
        }
        if (HandEquipStateMachine.shouldTreatHandOutActive()) {
            clearMiningForEquipTransition();
            return;
        }
        boolean hasMiningV2 = hasMiningV2Entry(client, stack) || this.miningV2Phase != MiningV2Phase.NONE;
        if (isFunctionalSwapFromMainToOffhand(client, stack)) {
            return;
        }
        if (mining && HandEquipStateMachine.isEquipInProgress() && shouldAbortEquipForMainHand(client) && !HandEquipStateMachine.abortEquipForAction(client, class_1268.field_5808)) {
            return;
        }
        if (triggered && hasMiningV2 && HandEquipStateMachine.isEquipInProgress() && shouldAbortEquipForMainHand(client) && !HandEquipStateMachine.abortEquipForAction(client, class_1268.field_5808)) {
            return;
        }
        if (hasMiningV2) {
            if (!mining && !triggered && this.miningV2Phase != MiningV2Phase.NONE && this.stopImmediateOnRelease && !isAttackDown(client)) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                if (this.skipBlendOut) {
                    PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
                } else {
                    PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(STOP_IMMEDIATE_BLEND_OUT_SECONDS);
                }
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
                clearMiningState();
                resetMiningV2();
                this.wasMining = false;
                return;
            }
            if (mining || triggered || this.miningV2Phase != MiningV2Phase.NONE) {
                ItemAnimationManager.setPersistentType(class_1268.field_5808, "mining", true, true);
                tickMiningV2(client, stack, rawMining, mining, triggered);
            } else {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
            }
            this.wasMining = mining;
            return;
        }
        if (mining) {
            ItemAnimationManager.setPersistentType(class_1268.field_5808, "mining", true, true);
            PunchyAnimationManager.POSE_HANDLER.setLooping(true);
            boolean itemChanged = hasItemChanged(this.lastMiningStack, stack);
            if (itemChanged && isFunctionalSwapFromMainToOffhand(client, stack)) {
                this.wasMining = true;
                return;
            } else if (!this.wasMining || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || itemChanged) {
                playPunch();
            }
        } else if (this.wasMining) {
            if (isFunctionalSwapFromMainToOffhand(client, stack)) {
                this.wasMining = true;
                return;
            }
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            stopMiningAnimation(client, false);
            clearMiningState();
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
        }
        this.wasMining = mining;
    }

    private void playPunch() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null) {
            return;
        }
        class_1799 stack = mc.field_1724.method_6047();
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "mining");
        AnimationClip clip = resolveCustomMiningClip(mc, stack, entry);
        if (clip == null) {
            CustomAnimationResolver.applyLoopSpeed(null);
            String preferredOption = PunchyTuningConfig.getMixPreferredOptionForStack(stack);
            PunchyTuningConfig.getMixPreferredOptionForType(stack, "mining");
            clip = PunchyAnimationManager.resolveHandedClip("punch", mc, class_1268.field_5808, preferredOption);
            if (stack.method_7909() instanceof class_1835) {
                AnimationClip tridentClip = PunchyAnimationManager.resolveHandedClip("attack_trident", mc, class_1268.field_5808, preferredOption);
                if (tridentClip != null) {
                    clip = tridentClip;
                }
            } else if (kind == VanillaFirstPersonItemLayer.ToolKind.PICKAXE || kind == VanillaFirstPersonItemLayer.ToolKind.HOE || kind == VanillaFirstPersonItemLayer.ToolKind.AXE || kind == VanillaFirstPersonItemLayer.ToolKind.SHOVEL) {
                if (kind == VanillaFirstPersonItemLayer.ToolKind.AXE) {
                    clip = PunchyAnimationManager.resolveHandedClip("mining_sideways", mc, class_1268.field_5808, preferredOption);
                    if (clip != null) {
                    }
                }
                if (clip == null) {
                    clip = PunchyAnimationManager.resolveTypeFallbackClip("mining", mc, class_1268.field_5808, preferredOption);
                    if (clip != null) {
                    }
                }
                if (clip == null) {
                    clip = (kind == VanillaFirstPersonItemLayer.ToolKind.PICKAXE || kind == VanillaFirstPersonItemLayer.ToolKind.HOE || kind == VanillaFirstPersonItemLayer.ToolKind.AXE) ? PunchyAnimationManager.resolveHandedClip("mining_foward", mc, class_1268.field_5808, preferredOption) : PunchyAnimationManager.resolveHandedClip("mining_digging", mc, class_1268.field_5808, preferredOption);
                }
            }
        } else {
            CustomAnimationResolver.applyLoopSpeed(entry);
        }
        if (clip != null) {
            this.stopImmediateOnRelease = CustomAnimationResolver.shouldStopImmediately(entry);
            this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(entry);
            class_1306 arm = PunchyAnimationManager.getMiningArm();
            this.activeArm = arm;
            this.activeClip = clip;
            primeOppositeArmOverlapDuringEquip(mc, class_1268.field_5808, arm);
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
            float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
            boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose() || shouldSkipBlendIn() || CustomAnimationResolver.shouldSkipBlendIn(entry);
            CustomAnimationResolver.playConfiguredClip(clip, entry, speedMultiplier, skipBlendIn, false);
            this.lastTriggerTick = currentTick();
            this.minStopTick = this.lastTriggerTick + MIN_MINING_DURATION_TICKS;
            this.lastMiningStack = copyStack(stack);
        }
    }

    private class_1799 copyStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return class_1799.field_8037;
        }
        return stack.method_7972();
    }

    private boolean hasItemChanged(class_1799 previous, class_1799 current) {
        class_1799 prev = previous == null ? class_1799.field_8037 : previous;
        class_1799 now = current == null ? class_1799.field_8037 : current;
        if (!prev.method_31574(now.method_7909())) {
            return true;
        }
        for (class_9336<?> dataComponent : now.method_57353()) {
            if (dataComponent.comp_2443() != class_9334.field_49629 && (!prev.method_57353().method_57832(dataComponent.comp_2443()) || !Objects.equals(prev.method_58694(dataComponent.comp_2443()), dataComponent.comp_2444()))) {
                return true;
            }
        }
        for (class_9336<?> dataComponent2 : prev.method_57353()) {
            if (dataComponent2.comp_2443() != class_9334.field_49629 && !now.method_57353().method_57832(dataComponent2.comp_2443())) {
                return true;
            }
        }
        return false;
    }

    private boolean isFunctionalSwapFromMainToOffhand(class_310 client, class_1799 currentMain) {
        if (client == null || client.field_1724 == null || this.lastMiningStack == null || this.lastMiningStack.method_7960()) {
            return false;
        }
        class_1799 currentOff = client.field_1724.method_6079();
        if (!class_1799.method_31577(this.lastMiningStack, currentOff)) {
            return false;
        }
        return hasItemChanged(this.lastMiningStack, currentMain);
    }

    private AnimationClip resolveCustomMiningClip(class_310 client, class_1799 stack, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (stack == null || entry == null) {
            return null;
        }
        return CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "mining", entry, client, class_1268.field_5808).clip();
    }

    private boolean hasMiningV2Entry(class_310 client, class_1799 stack) {
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "mining_v2");
        if (entry == null) {
            return false;
        }
        MiningV2Selection selection = resolveMiningV2Selection(client, stack, entry);
        return selection.isValid();
    }

    private void tickMiningV2(class_310 client, class_1799 stack, boolean rawMining, boolean mining, boolean triggered) {
        MiningV2Selection miningV2SelectionResolveMiningV2Selection;
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "mining_v2");
        if (entry != null) {
            miningV2SelectionResolveMiningV2Selection = resolveMiningV2Selection(client, stack, entry);
        } else {
            miningV2SelectionResolveMiningV2Selection = MiningV2Selection.INVALID;
        }
        MiningV2Selection selection = miningV2SelectionResolveMiningV2Selection;
        hasTrackableMiningTarget(client);
        if (triggered) {
            registerMiningV2Trigger();
        }
        if (this.miningV2Phase == MiningV2Phase.NONE) {
            if (!selection.isValid()) {
                if (mining) {
                    PunchyAnimationManager.POSE_HANDLER.setLooping(true);
                    if (!this.wasMining || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || hasItemChanged(this.lastMiningStack, stack)) {
                        playPunch();
                    }
                    return;
                }
                return;
            }
            if (!mining && !triggered) {
                return;
            }
        }
        boolean needsRestart = this.miningV2Phase == MiningV2Phase.NONE || hasItemChanged(this.lastMiningV2Stack, stack) || this.miningV2Entry != entry;
        if (needsRestart) {
            if (!selection.isValid()) {
                resetMiningV2();
                return;
            }
            this.miningV2Entry = entry;
            this.stopImmediateOnRelease = CustomAnimationResolver.shouldStopImmediately(entry);
            this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(entry);
            this.miningV2StartClip = selection.start();
            this.miningV2LoopClip = selection.loop();
            this.miningV2EndClip = selection.end();
            startMiningV2(selection.start());
            this.lastMiningV2Stack = copyStack(stack);
            this.lastMiningStack = copyStack(stack);
            return;
        }
        PunchyTuningConfig.CustomAnimationEntry activeEntry = entry != null ? entry : this.miningV2Entry;
        if (activeEntry != null) {
            CustomAnimationResolver.applyLoopSpeed(activeEntry);
            this.stopImmediateOnRelease = CustomAnimationResolver.shouldStopImmediately(activeEntry);
            this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(activeEntry);
        }
        if (mining && this.miningV2Phase == MiningV2Phase.END && selection.isValid()) {
            restartMiningV2FromTrigger(selection.start());
            this.lastMiningV2Stack = copyStack(stack);
            this.lastMiningStack = copyStack(stack);
            return;
        }
        if (triggered && this.miningV2Phase == MiningV2Phase.END) {
            restartMiningV2FromTrigger(selection.start());
            this.lastMiningV2Stack = copyStack(stack);
            this.lastMiningStack = copyStack(stack);
            return;
        }
        this.lastMiningV2Stack = copyStack(stack);
        this.lastMiningStack = copyStack(stack);
        PoseHandlerWrapper handler = new PoseHandlerWrapper();
        switch (this.miningV2Phase.ordinal()) {
            case RECENT_TARGET_GRACE_TICKS /* 1 */:
                applyMiningV2StartSpeed(selection.start());
                if (!handler.isPlaying(selection.start())) {
                    if (isAttackDown(client) && mining) {
                        transitionToMiningV2Loop(selection.loop());
                    } else {
                        transitionToMiningV2End(this.miningV2EndClip);
                    }
                }
                break;
            case 2:
                if (!isAttackDown(client) || !mining) {
                    beginMiningV2LoopEnd();
                } else if (!handler.isPlaying(selection.loop())) {
                    transitionToMiningV2Loop(selection.loop());
                } else {
                    PunchyAnimationManager.POSE_HANDLER.setLooping(true);
                }
                break;
            case 3:
                if (triggered) {
                    if (mining) {
                        restartMiningV2FromTrigger(selection.start());
                        break;
                    }
                } else {
                    if (handler.isPlaying(selection.loop())) {
                        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                    } else {
                        transitionToMiningV2End(this.miningV2EndClip);
                    }
                    break;
                }
                break;
            case 4:
                if (!handler.isPlaying(this.miningV2EndClip) && !handler.isCurrentOrBlending(this.miningV2EndClip)) {
                    resetMiningV2();
                    break;
                }
                break;
        }
    }

    private void startMiningV2(AnimationClip startClip) {
        if (startClip == null) {
            return;
        }
        class_1306 arm = PunchyAnimationManager.getMiningArm();
        this.activeArm = arm;
        this.activeClip = startClip;
        primeOppositeArmOverlapDuringEquip(class_310.method_1551(), class_1268.field_5808, arm);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = resolveMiningV2StartSpeed(startClip);
        PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.0f);
        PunchyAnimationManager.POSE_HANDLER.play(startClip, speedMultiplier, true);
        this.miningV2Phase = MiningV2Phase.START;
        this.lastTriggerTick = currentTick();
        this.minStopTick = this.lastTriggerTick + MIN_MINING_DURATION_TICKS;
    }

    private void restartMiningV2FromTrigger(AnimationClip startClip) {
        if (startClip == null) {
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.resetImmediate();
        startMiningV2(startClip);
    }

    private void transitionToMiningV2Loop(AnimationClip loopClip) {
        if (loopClip == null) {
            return;
        }
        class_1306 arm = PunchyAnimationManager.getMiningArm();
        this.activeArm = arm;
        this.activeClip = loopClip;
        primeOppositeArmOverlapDuringEquip(class_310.method_1551(), class_1268.field_5808, arm);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(true);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(loopClip);
        if (PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.06f);
            PunchyAnimationManager.POSE_HANDLER.setBlendInEasingNext(PoseHandler.BlendInEasing.SMOOTH);
            PunchyAnimationManager.POSE_HANDLER.playWithBlendFromCurrentDynamic(loopClip, speedMultiplier, false);
        } else {
            PunchyAnimationManager.POSE_HANDLER.play(loopClip, speedMultiplier, true);
        }
        this.miningV2Phase = MiningV2Phase.LOOP;
    }

    private void transitionToMiningV2End(AnimationClip endClip) {
        transitionToMiningV2End(endClip, PoseHandler.BlendInEasing.SMOOTH, 0.08f);
    }

    private void transitionToMiningV2End(AnimationClip endClip, PoseHandler.BlendInEasing easing, float blendDuration) {
        if (endClip == null) {
            resetMiningV2();
            return;
        }
        class_1306 arm = PunchyAnimationManager.getMiningArm();
        this.activeArm = arm;
        this.activeClip = endClip;
        primeOppositeArmOverlapDuringEquip(class_310.method_1551(), class_1268.field_5808, arm);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(endClip);
        if (PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            PunchyAnimationManager.POSE_HANDLER.setRebaseReturnSpeedNext(4.5f);
            PunchyAnimationManager.POSE_HANDLER.playAtTimeRebasedFromCurrentReturnToOrigin(endClip, speedMultiplier, false, 0.0f);
        } else {
            PunchyAnimationManager.POSE_HANDLER.play(endClip, speedMultiplier, true);
        }
        this.miningV2Phase = MiningV2Phase.END;
    }

    private void beginMiningV2LoopEnd() {
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        transitionToMiningV2End(this.miningV2EndClip, PoseHandler.BlendInEasing.BACK, 0.12f);
    }

    private void registerMiningV2Trigger() {
        long now = currentTick();
        float targetSpeed = 1.0f;
        if (this.miningV2LastTriggerTick >= 0) {
            long intervalTicks = Math.max(MINING_V2_FASTEST_TRIGGER_TICKS, now - this.miningV2LastTriggerTick);
            float normalized = class_3532.method_15363(1.0f - ((intervalTicks - MINING_V2_FASTEST_TRIGGER_TICKS) / 5.0f), 0.0f, 1.0f);
            targetSpeed = class_3532.method_16439(normalized * normalized, 1.0f, MINING_V2_START_SPEED_MAX);
        }
        this.miningV2StartSpeed = class_3532.method_16439(MINING_V2_START_SPEED_RESPONSE, this.miningV2StartSpeed, targetSpeed);
        this.miningV2StartSpeed = class_3532.method_15363(this.miningV2StartSpeed, 1.0f, MINING_V2_START_SPEED_MAX);
        this.miningV2LastTriggerTick = now;
    }

    private float resolveMiningV2StartSpeed(AnimationClip startClip) {
        return PunchyAnimationManager.clipSpeedMultiplier(startClip) * this.miningV2StartSpeed;
    }

    private void applyMiningV2StartSpeed(AnimationClip startClip) {
        if (startClip == null || PunchyAnimationManager.POSE_HANDLER.getCurrentClip() != startClip) {
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setSpeedMultiplier(resolveMiningV2StartSpeed(startClip));
    }

    private boolean shouldSkipBlendIn() {
        long now = currentTick();
        return this.lastTriggerTick > 0 && now - this.lastTriggerTick <= FAST_TRIGGER_WINDOW_TICKS;
    }

    private boolean shouldKeepMiningActive(boolean rawMining, class_310 client, long now) {
        if (rawMining) {
            this.stopGraceTicks = 0;
            return true;
        }
        if (this.wasMining && this.minStopTick >= 0 && now < this.minStopTick) {
            return true;
        }
        if (this.wasMining && isAttackDown(client) && isAimingTrackableBlockNow(client) && this.stopGraceTicks < 2) {
            this.stopGraceTicks += RECENT_TARGET_GRACE_TICKS;
            return true;
        }
        if (this.wasMining && isAttackDown(client) && hasVeryRecentMiningTarget() && this.stopGraceTicks < RECENT_TARGET_GRACE_TICKS) {
            this.stopGraceTicks += RECENT_TARGET_GRACE_TICKS;
            return true;
        }
        if (this.wasMining && isAttackDown(client) && hasRecentMiningChain() && this.stopGraceTicks < 2) {
            this.stopGraceTicks += RECENT_TARGET_GRACE_TICKS;
            return true;
        }
        return false;
    }

    private boolean isAttackDown(class_310 client) {
        return client.field_1690 != null && client.field_1690.field_1886.method_1434();
    }

    private boolean isAimingTrackableBlockNow(class_310 client) {
        if (client == null || client.field_1687 == null || client.field_1765 == null) {
            return false;
        }
        class_3965 class_3965Var = client.field_1765;
        if (class_3965Var.method_17783() != class_239.class_240.field_1332 || !(class_3965Var instanceof class_3965)) {
            return false;
        }
        class_3965 blockHit = class_3965Var;
        class_2680 state = client.field_1687.method_8320(blockHit.method_17777());
        return (state == null || state.method_26215()) ? false : true;
    }

    private boolean hasVeryRecentMiningTarget() {
        return MiningActionTracker.peekRecentTarget(RECENT_TARGET_GRACE_NANOS) != null;
    }

    private boolean hasRecentMiningChain() {
        return MiningActionTracker.wasRecentMining(RECENT_MINING_CHAIN_NANOS);
    }

    private void stopMiningAnimation(class_310 client, boolean forceReset) {
        if (!forceReset && this.miningV2Phase != MiningV2Phase.NONE) {
            transitionToMiningV2End(this.miningV2EndClip);
            return;
        }
        if (ownsActiveClip()) {
            if (this.skipBlendOut) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
            }
            if (this.stopImmediateOnRelease && !isAttackDown(client)) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(STOP_IMMEDIATE_BLEND_OUT_SECONDS);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            } else {
                PunchyAnimationManager.POSE_HANDLER.requestStopAfterCurrent();
            }
        }
    }

    private void clearMiningState() {
        this.stopImmediateOnRelease = false;
        this.skipBlendOut = false;
        this.activeClip = null;
        this.activeArm = null;
        this.lastMiningStack = class_1799.field_8037;
        this.minStopTick = -1L;
        this.stopGraceTicks = 0;
        this.lastTriggerTick = -1L;
    }

    private long currentTick() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null) {
            return 0L;
        }
        return mc.field_1687.method_8510();
    }

    private void resetMiningV2() {
        this.miningV2Phase = MiningV2Phase.NONE;
        this.miningV2Entry = null;
        this.miningV2StartClip = null;
        this.miningV2LoopClip = null;
        this.miningV2EndClip = null;
        this.activeClip = null;
        this.lastMiningV2Stack = class_1799.field_8037;
        this.miningV2LastTriggerTick = -1L;
        this.miningV2StartSpeed = 1.0f;
    }

    private boolean ownsActiveClip() {
        return PunchyAnimationManager.POSE_HANDLER.isCurrentClipOwnedBy(this.activeClip, this.activeArm);
    }

    private boolean shouldAbortEquipForMainHand(class_310 client) {
        if (client == null) {
            return true;
        }
        class_1306 equipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (equipArm == null) {
            return false;
        }
        class_1306 actionArm = PunchyAnimationManager.getUseArm(client, class_1268.field_5808);
        return actionArm == null || actionArm == equipArm;
    }

    private void primeOppositeArmOverlapDuringEquip(class_310 client, class_1268 hand, class_1306 actionArm) {
        if (client == null || hand == null || actionArm == null || !HandEquipStateMachine.isEquipInProgress()) {
            return;
        }
        if (hand == class_1268.field_5808) {
            HandEquipStateMachine.prepareOffhandForMainhandAction(client);
        }
        if (PunchyAnimationManager.POSE_HANDLER.getActiveArm() != null) {
            return;
        }
        class_1306 opposite = actionArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(opposite);
    }

    private void clearMiningForEquipTransition() {
        ItemAnimationManager.clearPersistentType(class_1268.field_5808, "mining");
        this.wasMining = false;
        clearMiningState();
        resetMiningV2();
    }

    private MiningV2Selection resolveMiningV2Selection(class_310 client, class_1799 stack, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (client == null || entry == null) {
            return MiningV2Selection.INVALID;
        }
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForEntry(stack, entry);
        AnimationClip start = resolveNamedClip(CustomAnimationResolver.resolveStartName(entry), client, preferredOption);
        AnimationClip loop = resolveNamedClip(CustomAnimationResolver.resolveLoopingName(entry), client, preferredOption);
        AnimationClip end = resolveNamedClip(CustomAnimationResolver.resolveEndName(entry), client, preferredOption);
        if (start == null || loop == null || end == null) {
            return MiningV2Selection.INVALID;
        }
        return new MiningV2Selection(start, loop, end);
    }

    private AnimationClip resolveNamedClip(String name, class_310 client, String preferredOption) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return PunchyAnimationManager.resolveNamedClip(name, client, class_1268.field_5808, preferredOption);
    }

    private boolean hasTrackableMiningTarget(class_310 client) {
        if (client == null || client.field_1687 == null || client.field_1765 == null) {
            return MiningActionTracker.peekRecentTarget(MINING_TARGET_LOCK_NANOS) != null;
        }
        class_3965 class_3965Var = client.field_1765;
        if (class_3965Var.method_17783() != class_239.class_240.field_1332 || !(class_3965Var instanceof class_3965)) {
            return MiningActionTracker.peekRecentTarget(MINING_TARGET_LOCK_NANOS) != null;
        }
        class_3965 blockHit = class_3965Var;
        class_2680 state = client.field_1687.method_8320(blockHit.method_17777());
        return ((state == null || state.method_26215()) && MiningActionTracker.peekRecentTarget(MINING_TARGET_LOCK_NANOS) == null) ? false : true;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningStateMachine$MiningV2Selection.class */
    private static final class MiningV2Selection extends Record {
        private final AnimationClip start;
        private final AnimationClip loop;
        private final AnimationClip end;
        private static final MiningV2Selection INVALID = new MiningV2Selection(null, null, null);

        private MiningV2Selection(AnimationClip start, AnimationClip loop, AnimationClip end) {
            this.start = start;
            this.loop = loop;
            this.end = end;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, MiningV2Selection.class), MiningV2Selection.class, "start;loop;end", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, MiningV2Selection.class), MiningV2Selection.class, "start;loop;end", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, MiningV2Selection.class, Object.class), MiningV2Selection.class, "start;loop;end", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/MiningStateMachine$MiningV2Selection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningStateMachine$PoseHandlerWrapper.class */
    private static final class PoseHandlerWrapper {
        private PoseHandlerWrapper() {
        }

        private boolean isPlaying(AnimationClip clip) {
            return clip != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip;
        }

        private boolean isCurrentOrBlending(AnimationClip clip) {
            return clip != null && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip;
        }
    }
}
