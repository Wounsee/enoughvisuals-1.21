package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1787;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1835;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_9362;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.render.ToolKindResolver;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.CustomAnimationResolver;
import punchy.client.state.UseActionTracker;
import punchy.compat.BetterCombatCompat;
import punchy.compat.SwordBlockingMechanicsCompat;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/UseItemStateMachine.class */
public class UseItemStateMachine {
    private static final long FAST_TRIGGER_WINDOW_TICKS = 4;
    private static final float STOP_IMMEDIATE_BLEND_OUT_SECONDS = 0.3f;
    private static class_1799 pendingThrowableMain = class_1799.field_8037;
    private static class_1799 pendingThrowableOff = class_1799.field_8037;
    private static class_1799 pendingDropMain = class_1799.field_8037;
    private static class_1799 pendingDropOff = class_1799.field_8037;
    private static class_1799 pendingBoneMealUseOnMain = class_1799.field_8037;
    private static class_1799 pendingBoneMealUseOnOff = class_1799.field_8037;
    private static class_1268 pendingSwordBlockingCompatHand;
    private static long visualUseItemEpoch;
    private boolean wasUseDown;
    private boolean wasUsing;
    private long lastTriggerTick = -1;
    private boolean holdOnLastFrame;
    private AnimationClip activeClip;
    private boolean stopImmediate;
    private boolean skipBlendOut;
    private boolean swordBlockingSuppressed;
    private class_1268 lastSwordBlockingHand;

    public void tick(class_310 client) {
        class_1268 class_1268VarMethod_6058;
        if (client == null || client.field_1724 == null || client.field_1761 == null) {
            return;
        }
        if (ConsumeStateMachine.isUseSuppressedAfterConsume()) {
            clearPendingThrowableSnapshot(null);
            UseActionTracker.clear();
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            this.holdOnLastFrame = false;
            this.activeClip = null;
            this.stopImmediate = false;
            this.skipBlendOut = false;
            this.wasUsing = false;
            this.swordBlockingSuppressed = false;
            this.lastSwordBlockingHand = null;
            this.wasUseDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
            return;
        }
        if (ConsumeStateMachine.isEquipBlockedByConsumeAnimation()) {
            clearPendingThrowableSnapshot(null);
            UseActionTracker.clear();
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            this.holdOnLastFrame = false;
            this.activeClip = null;
            this.stopImmediate = false;
            this.skipBlendOut = false;
            this.wasUsing = false;
            this.swordBlockingSuppressed = false;
            this.lastSwordBlockingHand = null;
            this.wasUseDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
            return;
        }
        if (client.field_1724.method_6115()) {
            class_1268 usedHand = client.field_1724.method_6058();
            if (PunchyAnimationManager.isHandBlacklisted(client, usedHand)) {
                resetState();
                return;
            }
        }
        class_1799 usingStack = client.field_1724.method_6030();
        if (ConsumeStateMachine.isConsumeStack(usingStack)) {
            resetState();
            return;
        }
        if (isBowUseActive(client) || isCrossbowUseActive(client) || isSpearUseActive(client)) {
            resetPoseStateOnly();
            return;
        }
        if (shouldStopSwordBlockingCompat(client)) {
            stopSwordBlockingCompat(client);
            return;
        }
        boolean usingItem = client.field_1724.method_6115();
        boolean useDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
        boolean z = useDown && !this.wasUseDown;
        if (!useDown) {
            this.swordBlockingSuppressed = false;
        }
        boolean forceSwordBlockHold = useDown && this.activeClip != null && PunchyAnimationManager.isClipNamed(this.activeClip, "sword_blocking_compat");
        class_1268 swordBlockingCompatHand = consumePendingSwordBlockingCompatHand();
        if (!this.swordBlockingSuppressed && swordBlockingCompatHand != null && SwordBlockingMechanicsCompat.shouldUseBlockingAnimation(swordBlockingCompatHand)) {
            if (PunchyAnimationManager.isHandBlacklisted(client, swordBlockingCompatHand)) {
                resetState();
                this.wasUseDown = useDown;
                return;
            } else if (HandEquipStateMachine.isEquipInProgress() && !HandEquipStateMachine.abortEquipForAction(client, swordBlockingCompatHand)) {
                pendingSwordBlockingCompatHand = swordBlockingCompatHand;
                this.wasUseDown = useDown;
                return;
            } else {
                if (playPendingSwordBlockingCompat(client, swordBlockingCompatHand)) {
                    this.wasUsing = true;
                }
                this.wasUseDown = useDown;
                return;
            }
        }
        UseActionTracker.Trigger trigger = UseActionTracker.consumeLatestTrigger();
        if (trigger != null && trigger.fallback() && !shouldUseFallbackTrigger(client, trigger)) {
            trigger = UseActionTracker.consumeTrigger();
        }
        class_1268 triggerHand = trigger == null ? null : trigger.hand();
        UseActionTracker.TriggerType triggerType = trigger == null ? null : trigger.type();
        class_3965 triggerBlockHit = trigger == null ? null : trigger.blockHit();
        class_2680 triggerBlockState = resolveBlockState(client, triggerBlockHit);
        if (this.swordBlockingSuppressed && useDown) {
            clearUseItemState(false);
            return;
        }
        if (triggerHand != null && PunchyAnimationManager.isHandBlacklisted(client, triggerHand)) {
            resetState();
            this.wasUseDown = useDown;
            return;
        }
        if (triggerHand != null) {
            class_1799 triggerStack = client.field_1724.method_5998(triggerHand);
            if (ConsumeStateMachine.isConsumeStack(triggerStack) && !isInteractionAwareTrigger(triggerType)) {
                this.wasUsing = false;
                return;
            }
        }
        boolean using = triggerHand != null || (usingItem && useDown) || forceSwordBlockHold;
        if (this.holdOnLastFrame && this.activeClip != null && useDown) {
            using = true;
        }
        if (triggerHand != null) {
            class_1268VarMethod_6058 = triggerHand;
        } else {
            class_1268VarMethod_6058 = usingItem ? client.field_1724.method_6058() : class_1268.field_5808;
        }
        class_1268 activeHand = class_1268VarMethod_6058;
        if (using && HandEquipStateMachine.isEquipInProgress() && shouldAbortEquipForHand(client, activeHand) && !HandEquipStateMachine.abortEquipForAction(client, activeHand)) {
            if (trigger != null) {
                UseActionTracker.pushBack(trigger);
                return;
            }
            return;
        }
        if (using) {
            class_1268 usedHandNow = usingItem ? client.field_1724.method_6058() : null;
            if (this.activeClip != null && PunchyAnimationManager.isClipNamed(this.activeClip, "sword_blocking_compat") && usedHandNow != null && this.lastSwordBlockingHand != null && this.lastSwordBlockingHand != usedHandNow) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                PunchyAnimationManager.clearSourceHand();
                HandEquipStateMachine.requestRenderedSync();
                this.activeClip = null;
                this.holdOnLastFrame = false;
                this.stopImmediate = false;
            }
            if (PunchyAnimationManager.isHandBlacklisted(client, activeHand)) {
                resetState();
                this.wasUseDown = useDown;
                return;
            }
            if (forceSwordBlockHold) {
                ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
                ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            } else {
                boolean sustainedUseFlow = usingItem && useDown;
                Boolean useItemLoopOverride = Boolean.valueOf(sustainedUseFlow ? !this.holdOnLastFrame : Boolean.FALSE.booleanValue());
                ItemAnimationManager.setPersistentType(activeHand, "useItem", useItemLoopOverride, true);
            }
            if (triggerHand != null && !shouldIgnoreTrigger(client, triggerHand, triggerType, triggerBlockState)) {
                UseActionTracker.discardOppositeOlderThan(triggerHand, trigger.recordNano());
                playInteract(client, triggerHand, triggerType, triggerBlockState, trigger.fallback());
            } else if (shouldForceSwordBlockingPlay(client)) {
                playInteract(client, activeHand, null, null, false);
            } else if (!PunchyAnimationManager.POSE_HANDLER.isPlaying() || shouldPlayUseDuringOffhandEquip(client, activeHand, usingItem, usedHandNow)) {
                playInteract(client, usingItem ? client.field_1724.method_6058() : class_1268.field_5808, null, null, false);
            }
        } else if (this.wasUsing) {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            if (this.skipBlendOut) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
            }
            if (this.holdOnLastFrame) {
                if (!useDown) {
                    PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                }
            } else if (this.stopImmediate) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(STOP_IMMEDIATE_BLEND_OUT_SECONDS);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            } else {
                PunchyAnimationManager.POSE_HANDLER.requestStopAfterCurrent();
            }
            clearUseItemState(false);
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
        }
        this.wasUseDown = useDown;
        this.wasUsing = using;
        if (using && this.activeClip != null && PunchyAnimationManager.isClipNamed(this.activeClip, "sword_blocking_compat")) {
            this.lastSwordBlockingHand = usingItem ? client.field_1724.method_6058() : this.lastSwordBlockingHand;
        } else {
            this.lastSwordBlockingHand = null;
        }
    }

    private void resetState() {
        if (this.wasUsing) {
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            if (this.stopImmediate) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(STOP_IMMEDIATE_BLEND_OUT_SECONDS);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            } else {
                PunchyAnimationManager.POSE_HANDLER.requestStopAfterCurrent();
            }
        }
        clearUseItemState(true);
    }

    private void resetPoseStateOnly() {
        if (this.wasUsing) {
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            if (this.stopImmediate) {
                PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(STOP_IMMEDIATE_BLEND_OUT_SECONDS);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            } else {
                PunchyAnimationManager.POSE_HANDLER.requestStopAfterCurrent();
            }
        }
        this.holdOnLastFrame = false;
        this.activeClip = null;
        this.stopImmediate = false;
        this.skipBlendOut = false;
        this.wasUsing = false;
        this.swordBlockingSuppressed = false;
        this.lastSwordBlockingHand = null;
    }

    private boolean shouldStopSwordBlockingCompat(class_310 client) {
        class_1268 class_1268VarMethod_6058;
        if (client == null || client.field_1724 == null || this.activeClip == null || !PunchyAnimationManager.isClipNamed(this.activeClip, "sword_blocking_compat")) {
            return false;
        }
        if (client.field_1690 == null || !client.field_1690.field_1904.method_1434()) {
            return true;
        }
        if (this.lastSwordBlockingHand != null) {
            class_1268VarMethod_6058 = this.lastSwordBlockingHand;
        } else {
            class_1268VarMethod_6058 = client.field_1724.method_6115() ? client.field_1724.method_6058() : class_1268.field_5808;
        }
        class_1268 blockingHand = class_1268VarMethod_6058;
        return !SwordBlockingMechanicsCompat.shouldUseBlockingAnimation(blockingHand);
    }

    private void stopSwordBlockingCompat(class_310 client) {
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
        clearUseItemState(false);
        if (client != null && client.field_1690 != null && client.field_1690.field_1904.method_1434()) {
            this.swordBlockingSuppressed = true;
        }
    }

    private void playInteract(class_310 client, class_1268 hand, UseActionTracker.TriggerType triggerType, class_2680 blockState, boolean fallbackOnly) {
        Float delay;
        UseSelection selection = resolveUseSelection(client, hand, triggerType, blockState, fallbackOnly);
        if (triggerType == UseActionTracker.TriggerType.DROP_ITEM) {
            clearPendingDropSnapshot(hand);
        } else if (triggerType == UseActionTracker.TriggerType.USE_ITEM_ON) {
            clearPendingBoneMealUseOnSnapshot(hand);
        }
        AnimationClip clip = selection.clip;
        if (triggerType == UseActionTracker.TriggerType.USE_ITEM_ON && CustomAnimationResolver.isInteractionEntry(selection.entry) && (delay = selection.entry.interfaceDelay) != null && delay.floatValue() > 0.0f) {
            InterfaceDelayState.schedule(delay.floatValue());
        }
        if (clip != null) {
            visualUseItemEpoch++;
            CustomAnimationResolver.applyLoopSpeed(selection.entry);
            PunchyAnimationManager.setSourceHand(client, hand);
            class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
            boolean dualHanded = PunchyAnimationManager.isDualHandedActive(client, clip, 0.0f);
            if (dualHanded && hand == class_1268.field_5808 && HandEquipStateMachine.isOffhandEquipTransitionActive()) {
                HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client.field_1724);
            }
            boolean preserveOppositeUseFlow = hand == class_1268.field_5808 && HandEquipStateMachine.isOffhandEquipTransitionActive() && arm != null;
            if (!preserveOppositeUseFlow) {
                preserveOppositeUseFlow = (!dualHanded || arm == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.getActiveArm() == null || PunchyAnimationManager.POSE_HANDLER.getActiveArm() == arm) ? false : true;
            }
            class_1306 opposite = arm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
            PunchyAnimationManager.POSE_HANDLER.setForcedSkipArmForNextPlay(preserveOppositeUseFlow ? opposite : null);
            if (dualHanded && !preserveOppositeUseFlow) {
                PunchyAnimationManager.POSE_HANDLER.setActiveArm(null);
            } else {
                primeOppositeArmOverlapDuringEquip(client, hand, arm);
                PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
            }
            boolean hold = clip.isHoldOnLastFrame() || PunchyAnimationManager.isClipNamed(clip, "sword_blocking_compat");
            PunchyAnimationManager.POSE_HANDLER.setLooping(!hold);
            float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
            boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose() || shouldSkipBlendIn(client) || PunchyAnimationManager.isClipNamed(clip, "use_bow");
            if (CustomAnimationResolver.shouldSkipBlendIn(selection.entry)) {
                skipBlendIn = true;
            }
            if (CustomAnimationResolver.shouldBlendFromCurrentOrigin(selection.entry, clip)) {
                CustomAnimationResolver.playConfiguredClip(clip, selection.entry, speedMultiplier, skipBlendIn, hold);
            } else if (shouldBlendFromAttackToBlocking(clip)) {
                if (skipBlendIn) {
                    PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.0f);
                }
                PunchyAnimationManager.POSE_HANDLER.playWithBlendFromCurrentDynamic(clip, speedMultiplier, hold);
            } else {
                PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, hold);
            }
            this.lastTriggerTick = currentTick(client);
            this.holdOnLastFrame = hold;
            this.activeClip = clip;
            this.stopImmediate = CustomAnimationResolver.shouldStopImmediately(selection.entry);
            this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(selection.entry);
        }
    }

    private boolean playPendingSwordBlockingCompat(class_310 client, class_1268 hand) {
        if (PunchyAnimationManager.resolveNamedClip("sword_blocking_compat", client, hand) == null) {
            return false;
        }
        UseActionTracker.clear();
        ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
        ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
        playInteract(client, hand, null, null, false);
        return this.activeClip != null && PunchyAnimationManager.isClipNamed(this.activeClip, "sword_blocking_compat");
    }

    public static long getVisualUseItemEpoch() {
        return visualUseItemEpoch;
    }

    private boolean shouldSkipBlendIn(class_310 client) {
        long now = currentTick(client);
        return this.lastTriggerTick > 0 && now - this.lastTriggerTick <= FAST_TRIGGER_WINDOW_TICKS;
    }

    private long currentTick(class_310 client) {
        if (client.field_1687 == null) {
            return 0L;
        }
        return client.field_1687.method_8510();
    }

    private boolean shouldAbortEquipForHand(class_310 client, class_1268 hand) {
        class_1306 equipArm;
        if (client == null || hand == null) {
            return true;
        }
        if (HandEquipStateMachine.isOppositeShieldEquipInProgress(hand) || (equipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm()) == null) {
            return false;
        }
        class_1306 actionArm = PunchyAnimationManager.getUseArm(client, hand);
        return actionArm == null || actionArm == equipArm;
    }

    private boolean shouldPlayUseDuringOffhandEquip(class_310 client, class_1268 activeHand, boolean usingItem, class_1268 usedHandNow) {
        if (client == null || activeHand != class_1268.field_5808 || !HandEquipStateMachine.isOffhandEquipTransitionActive()) {
            return false;
        }
        if (usingItem && usedHandNow != class_1268.field_5808) {
            return false;
        }
        return true;
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

    private AnimationClip resolveUseClip(class_310 client, class_1268 hand, UseActionTracker.TriggerType triggerType, class_2680 blockState) {
        AnimationClip override;
        AnimationClip throwable;
        if (client == null || client.field_1724 == null) {
            return PunchyAnimationManager.INTERACT_CLIP;
        }
        class_1799 stack = resolveUseContextStack(client.field_1724.method_5998(hand), hand, triggerType);
        if (isChargeItem(stack) && shouldBlockChargeForTrigger(triggerType)) {
            return null;
        }
        if (shouldResolveThrowableUse(triggerType) && (throwable = resolveThrowableUseClip(client, hand, stack, triggerType)) != null) {
            return throwable;
        }
        if (isInteractionAwareTrigger(triggerType)) {
            PunchyTuningConfig.CustomAnimationEntry interactionEntry = CustomAnimationResolver.findInteractionEntry(stack, blockState);
            String interactionName = CustomAnimationResolver.resolveInteractionName(interactionEntry);
            if (interactionName != null && !interactionName.isBlank() && (override = PunchyAnimationManager.resolveNamedClip(interactionName, client, hand)) != null) {
                return override;
            }
        }
        if (isBoneMealStack(stack) && triggerType == UseActionTracker.TriggerType.USE_ITEM_ON) {
            return PunchyAnimationManager.resolveTypeFallbackClip("useItem", client, hand, PunchyTuningConfig.getMixPreferredOptionForStack(stack));
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "useItem");
        if (entry != null) {
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "useItem", entry, client, hand);
            if (lookup.clip() != null) {
                return lookup.clip();
            }
        }
        AnimationClip fishingRodClip = resolveFishingRodUseClip(client, hand, stack);
        if (fishingRodClip != null) {
            return fishingRodClip;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForStack(stack);
        if (kind == VanillaFirstPersonItemLayer.ToolKind.HOE || kind == VanillaFirstPersonItemLayer.ToolKind.AXE) {
            return PunchyAnimationManager.resolveHandedClip("mining_use_foward", client, hand, preferredOption);
        }
        if (kind == VanillaFirstPersonItemLayer.ToolKind.SHOVEL) {
            return PunchyAnimationManager.resolveHandedClip("mining_use_digging", client, hand, preferredOption);
        }
        return PunchyAnimationManager.resolveHandedClip("interact", client, hand, preferredOption);
    }

    private boolean shouldIgnoreTrigger(class_310 client, class_1268 hand, UseActionTracker.TriggerType triggerType, class_2680 blockState) {
        AnimationClip clip;
        return triggerType == null && this.holdOnLastFrame && PunchyAnimationManager.POSE_HANDLER.isPlaying() && (clip = resolveUseSelection(client, hand, triggerType, blockState, false).clip) != null && clip == this.activeClip;
    }

    private boolean shouldUseFallbackTrigger(class_310 client, UseActionTracker.Trigger trigger) {
        class_1268 hand;
        class_1799 stack;
        VanillaFirstPersonItemLayer.ToolKind kind;
        if (client == null || client.field_1724 == null || trigger == null || !trigger.fallback() || trigger.type() != UseActionTracker.TriggerType.USE_ITEM_ON || trigger.blockHit() == null || (hand = trigger.hand()) == null || PunchyAnimationManager.isHandBlacklisted(client, hand) || (stack = client.field_1724.method_5998(hand)) == null || ConsumeStateMachine.isConsumeStack(stack) || isChargeItem(stack)) {
            return false;
        }
        if (!stack.method_7960()) {
            return true;
        }
        class_2680 blockState = resolveBlockState(client, trigger.blockHit());
        if (isFallbackAttackCandidate(client, hand, stack)) {
            return true;
        }
        PunchyTuningConfig.CustomAnimationEntry interactionEntry = CustomAnimationResolver.findInteractionEntry(stack, blockState);
        return interactionEntry != null || CustomAnimationResolver.findEntry(stack, "useItem") != null || (kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT)) == VanillaFirstPersonItemLayer.ToolKind.HOE || kind == VanillaFirstPersonItemLayer.ToolKind.AXE || kind == VanillaFirstPersonItemLayer.ToolKind.SHOVEL;
    }

    private UseSelection resolveUseSelection(class_310 client, class_1268 hand, UseActionTracker.TriggerType triggerType, class_2680 blockState) {
        return resolveUseSelection(client, hand, triggerType, blockState, false);
    }

    private UseSelection resolveUseSelection(class_310 client, class_1268 hand, UseActionTracker.TriggerType triggerType, class_2680 blockState, boolean fallbackOnly) {
        PunchyTuningConfig.CustomAnimationEntry interactionEntry;
        String interactionName;
        AnimationClip override;
        AnimationClip compatClip;
        AnimationClip throwable;
        if (client == null || client.field_1724 == null) {
            return new UseSelection(PunchyAnimationManager.INTERACT_CLIP, null);
        }
        class_1799 stack = resolveUseContextStack(client.field_1724.method_5998(hand), hand, triggerType);
        if (isChargeItem(stack) && shouldBlockChargeForTrigger(triggerType)) {
            return new UseSelection(null, null);
        }
        if (shouldResolveThrowableUse(triggerType) && (throwable = resolveThrowableUseClip(client, hand, stack, triggerType)) != null) {
            class_1799 resolvedStack = resolveThrowableContextStack(stack, hand, triggerType);
            return new UseSelection(throwable, CustomAnimationResolver.findEntry(resolvedStack, "useItem"));
        }
        if (!this.swordBlockingSuppressed && SwordBlockingMechanicsCompat.shouldUseBlockingAnimation(hand) && (compatClip = PunchyAnimationManager.resolveNamedClip("sword_blocking_compat", client, hand)) != null) {
            return new UseSelection(compatClip, null);
        }
        if (fallbackOnly && triggerType == UseActionTracker.TriggerType.USE_ITEM_ON && isFallbackAttackCandidate(client, hand, stack)) {
            return new UseSelection(resolveFallbackAttackClip(client, hand, stack), null);
        }
        if (isInteractionAwareTrigger(triggerType) && (interactionName = CustomAnimationResolver.resolveInteractionName((interactionEntry = CustomAnimationResolver.findInteractionEntry(stack, blockState)))) != null && !interactionName.isBlank() && (override = PunchyAnimationManager.resolveNamedClip(interactionName, client, hand)) != null) {
            return new UseSelection(override, interactionEntry);
        }
        if (isBoneMealStack(stack) && triggerType == UseActionTracker.TriggerType.USE_ITEM_ON) {
            return new UseSelection(PunchyAnimationManager.resolveTypeFallbackClip("useItem", client, hand, PunchyTuningConfig.getMixPreferredOptionForStack(stack)), null);
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "useItem");
        if (entry != null) {
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "useItem", entry, client, hand);
            if (lookup.clip() != null) {
                return new UseSelection(lookup.clip(), entry);
            }
        }
        AnimationClip fishingRodClip = resolveFishingRodUseClip(client, hand, stack);
        if (fishingRodClip != null) {
            return new UseSelection(fishingRodClip, CustomAnimationResolver.findEntry(stack, "fishingrod"));
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForStack(stack);
        if (kind == VanillaFirstPersonItemLayer.ToolKind.HOE || kind == VanillaFirstPersonItemLayer.ToolKind.AXE) {
            return new UseSelection(PunchyAnimationManager.resolveHandedClip("mining_use_foward", client, hand, preferredOption), null);
        }
        if (kind == VanillaFirstPersonItemLayer.ToolKind.SHOVEL) {
            return new UseSelection(PunchyAnimationManager.resolveHandedClip("mining_use_digging", client, hand, preferredOption), null);
        }
        return new UseSelection(PunchyAnimationManager.resolveHandedClip("interact", client, hand, preferredOption), null);
    }

    private boolean isFallbackAttackCandidate(class_310 client, class_1268 hand, class_1799 stack) {
        if (client == null || hand == null) {
            return false;
        }
        if (stack == null || stack.method_7960()) {
            return hand == class_1268.field_5808;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        if (kind == VanillaFirstPersonItemLayer.ToolKind.SWORD || kind == VanillaFirstPersonItemLayer.ToolKind.MACE || kind == VanillaFirstPersonItemLayer.ToolKind.SPEAR || kind == VanillaFirstPersonItemLayer.ToolKind.TRIDENT) {
            return true;
        }
        return BetterCombatCompat.isWeaponStack(stack);
    }

    private AnimationClip resolveFallbackAttackClip(class_310 client, class_1268 hand, class_1799 stack) {
        AnimationClip spear;
        AnimationClip trident;
        AnimationClip mace;
        if (client == null) {
            return PunchyAnimationManager.ATTACK_CLIP;
        }
        if (stack == null || stack.method_7960()) {
            AnimationClip punch = PunchyAnimationManager.resolveHandedClip("punch", client, hand);
            return punch != null ? punch : PunchyAnimationManager.PUNCH_CLIP;
        }
        if ((stack.method_7909() instanceof class_9362) && (mace = PunchyAnimationManager.resolveHandedClip("attack_mace", client, hand)) != null) {
            return mace;
        }
        if ((stack.method_7909() instanceof class_1835) && (trident = PunchyAnimationManager.resolveHandedClip("attack_trident", client, hand)) != null) {
            return trident;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        if (kind == VanillaFirstPersonItemLayer.ToolKind.SPEAR && (spear = PunchyAnimationManager.resolveHandedClip("attack_spear", client, hand)) != null) {
            return spear;
        }
        return PunchyAnimationManager.resolveHandedClip("attack", client, hand);
    }

    private AnimationClip resolveThrowableUseClip(class_310 client, class_1268 hand, class_1799 liveStack, UseActionTracker.TriggerType triggerType) {
        String explicitName;
        class_1799 stack = resolveThrowableContextStack(liveStack, hand, triggerType);
        if (client == null || hand == null || !isThrowableUseStack(stack)) {
            return null;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "useItem");
        if (entry != null) {
            if (entry.throw_animation != null && !entry.throw_animation.isBlank()) {
                explicitName = entry.throw_animation.trim();
            } else {
                explicitName = CustomAnimationResolver.resolveSingleName(entry);
            }
            AnimationClip explicit = PunchyAnimationManager.resolveNamedClip(explicitName, client, hand);
            if (explicit != null) {
                return explicit;
            }
        }
        return PunchyAnimationManager.resolveHandedClip("throw_item", client, hand, PunchyTuningConfig.getMixPreferredOptionForStack(stack));
    }

    private boolean isThrowableUseStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150) || stack.method_31574(class_1802.field_8287) || stack.method_31574(class_1802.field_8543) || stack.method_31574(class_1802.field_8803) || stack.method_31574(class_1802.field_8634) || stack.method_31574(class_1802.field_8449);
    }

    private class_1799 resolveThrowableContextStack(class_1799 liveStack, class_1268 hand, UseActionTracker.TriggerType triggerType) {
        if (liveStack != null && !liveStack.method_7960()) {
            return liveStack;
        }
        if (triggerType != UseActionTracker.TriggerType.USE_ITEM || hand == null) {
            return liveStack == null ? class_1799.field_8037 : liveStack;
        }
        class_1799 pending = consumePendingThrowableSnapshot(hand);
        return isThrowableUseStack(pending) ? pending : class_1799.field_8037;
    }

    public static void capturePendingThrowableSnapshot(class_1268 hand, class_1799 stack) {
        if (hand == null || !isThrowableStatic(stack)) {
            clearPendingThrowableSnapshot(hand);
            return;
        }
        class_1799 copy = stack.method_7972();
        if (hand == class_1268.field_5810) {
            pendingThrowableOff = copy;
        } else {
            pendingThrowableMain = copy;
        }
    }

    public static void capturePendingDropSnapshot(class_1268 hand, class_1799 stack) {
        if (hand == null || stack == null || stack.method_7960()) {
            clearPendingDropSnapshot(hand);
            return;
        }
        class_1799 copy = stack.method_7972();
        if (hand == class_1268.field_5810) {
            pendingDropOff = copy;
        } else {
            pendingDropMain = copy;
        }
    }

    public static void capturePendingBoneMealUseOnSnapshot(class_1268 hand, class_1799 stack) {
        if (hand == null || !isBoneMealStatic(stack)) {
            clearPendingBoneMealUseOnSnapshot(hand);
            return;
        }
        class_1799 copy = stack.method_7972();
        if (hand == class_1268.field_5810) {
            pendingBoneMealUseOnOff = copy;
        } else {
            pendingBoneMealUseOnMain = copy;
        }
    }

    public static boolean hasPendingBoneMealUseOnSnapshot(class_1268 hand) {
        return isBoneMealStatic(peekPendingBoneMealUseOnSnapshot(hand));
    }

    public static void requestSwordBlockingCompat(class_1268 hand) {
        pendingSwordBlockingCompatHand = hand == null ? class_1268.field_5808 : hand;
    }

    private static class_1268 consumePendingSwordBlockingCompatHand() {
        class_1268 hand = pendingSwordBlockingCompatHand;
        pendingSwordBlockingCompatHand = null;
        return hand;
    }

    public static void clearPendingThrowableSnapshot(class_1268 hand) {
        if (hand == null || hand == class_1268.field_5808) {
            pendingThrowableMain = class_1799.field_8037;
        }
        if (hand == null || hand == class_1268.field_5810) {
            pendingThrowableOff = class_1799.field_8037;
        }
    }

    private static class_1799 consumePendingThrowableSnapshot(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            class_1799 snapshot = pendingThrowableOff;
            pendingThrowableOff = class_1799.field_8037;
            return snapshot == null ? class_1799.field_8037 : snapshot;
        }
        class_1799 snapshot2 = pendingThrowableMain;
        pendingThrowableMain = class_1799.field_8037;
        return snapshot2 == null ? class_1799.field_8037 : snapshot2;
    }

    private static void clearPendingDropSnapshot(class_1268 hand) {
        if (hand == null || hand == class_1268.field_5808) {
            pendingDropMain = class_1799.field_8037;
        }
        if (hand == null || hand == class_1268.field_5810) {
            pendingDropOff = class_1799.field_8037;
        }
    }

    private static class_1799 peekPendingDropSnapshot(class_1268 hand) {
        return hand == class_1268.field_5810 ? pendingDropOff == null ? class_1799.field_8037 : pendingDropOff : pendingDropMain == null ? class_1799.field_8037 : pendingDropMain;
    }

    private class_1799 resolveUseContextStack(class_1799 liveStack, class_1268 hand, UseActionTracker.TriggerType triggerType) {
        class_1799 stack = resolveDropContextStack(liveStack, hand, triggerType);
        if (triggerType != UseActionTracker.TriggerType.USE_ITEM_ON || hand == null || isBoneMealStack(stack)) {
            return stack;
        }
        class_1799 pending = peekPendingBoneMealUseOnSnapshot(hand);
        return isBoneMealStack(pending) ? pending : stack;
    }

    private class_1799 resolveDropContextStack(class_1799 liveStack, class_1268 hand, UseActionTracker.TriggerType triggerType) {
        if (triggerType != UseActionTracker.TriggerType.DROP_ITEM || hand == null) {
            return liveStack == null ? class_1799.field_8037 : liveStack;
        }
        class_1799 pending = peekPendingDropSnapshot(hand);
        return (pending == null || pending.method_7960()) ? liveStack == null ? class_1799.field_8037 : liveStack : pending;
    }

    private static void clearPendingBoneMealUseOnSnapshot(class_1268 hand) {
        if (hand == null || hand == class_1268.field_5808) {
            pendingBoneMealUseOnMain = class_1799.field_8037;
        }
        if (hand == null || hand == class_1268.field_5810) {
            pendingBoneMealUseOnOff = class_1799.field_8037;
        }
    }

    private static class_1799 peekPendingBoneMealUseOnSnapshot(class_1268 hand) {
        return hand == class_1268.field_5810 ? pendingBoneMealUseOnOff == null ? class_1799.field_8037 : pendingBoneMealUseOnOff : pendingBoneMealUseOnMain == null ? class_1799.field_8037 : pendingBoneMealUseOnMain;
    }

    private boolean shouldResolveThrowableUse(UseActionTracker.TriggerType triggerType) {
        return (isInteractionAwareTrigger(triggerType) || triggerType == UseActionTracker.TriggerType.DROP_ITEM) ? false : true;
    }

    private boolean isBoneMealStack(class_1799 stack) {
        return isBoneMealStatic(stack);
    }

    private static boolean isBoneMealStatic(class_1799 stack) {
        return stack != null && stack.method_31574(class_1802.field_8324);
    }

    private static boolean isThrowableStatic(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150) || stack.method_31574(class_1802.field_8287) || stack.method_31574(class_1802.field_8543) || stack.method_31574(class_1802.field_8803) || stack.method_31574(class_1802.field_8634) || stack.method_31574(class_1802.field_8449);
    }

    private boolean shouldForceSwordBlockingPlay(class_310 client) {
        AnimationClip current;
        if (client == null || client.field_1724 == null || !SwordBlockingMechanicsCompat.shouldUseBlockingAnimation(client.field_1724.method_6058()) || (current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        return isAttackClip(current);
    }

    private boolean shouldBlendFromAttackToBlocking(AnimationClip nextClip) {
        AnimationClip current;
        if (nextClip == null || !PunchyAnimationManager.isClipNamed(nextClip, "sword_blocking_compat") || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || (current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        return isAttackClip(current);
    }

    private void clearUseItemState(boolean clearSuppression) {
        ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
        ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
        AnimationClip currentClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        boolean keepDualHandedSource = PunchyAnimationManager.POSE_HANDLER.isPlaying() && currentClip != null && PunchyAnimationManager.hasAnyDualHandedEffect(currentClip);
        this.holdOnLastFrame = false;
        this.activeClip = null;
        this.stopImmediate = false;
        this.skipBlendOut = false;
        this.wasUsing = false;
        if (clearSuppression) {
            this.swordBlockingSuppressed = false;
        }
        if (!keepDualHandedSource) {
            PunchyAnimationManager.clearSourceHand();
        }
    }

    private boolean isChargeItem(class_1799 stack) {
        if (stack == null) {
            return false;
        }
        return (stack.method_7909() instanceof class_1753) || (stack.method_7909() instanceof class_1764) || (stack.method_7909() instanceof class_1835);
    }

    private boolean shouldBlockChargeForTrigger(UseActionTracker.TriggerType triggerType) {
        return triggerType == null || triggerType == UseActionTracker.TriggerType.USE_ITEM;
    }

    private boolean isInteractionAwareTrigger(UseActionTracker.TriggerType triggerType) {
        return triggerType == UseActionTracker.TriggerType.USE_ITEM_ON || triggerType == UseActionTracker.TriggerType.INTERACT || triggerType == UseActionTracker.TriggerType.INTERACT_AT;
    }

    private class_2680 resolveBlockState(class_310 client, class_3965 hit) {
        if (client == null || client.field_1687 == null || hit == null) {
            return null;
        }
        return client.field_1687.method_8320(hit.method_17777());
    }

    private boolean isAttackClip(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "attack")) {
            return true;
        }
        String name = clip.getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("attack");
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/UseItemStateMachine$UseSelection.class */
    private static final class UseSelection extends Record {
        private final AnimationClip clip;
        private final PunchyTuningConfig.CustomAnimationEntry entry;

        private UseSelection(AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry) {
            this.clip = clip;
            this.entry = entry;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, UseSelection.class), UseSelection.class, "clip;entry", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, UseSelection.class), UseSelection.class, "clip;entry", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, UseSelection.class, Object.class), UseSelection.class, "clip;entry", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/UseItemStateMachine$UseSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public PunchyTuningConfig.CustomAnimationEntry entry() {
            return this.entry;
        }
    }

    private AnimationClip resolveFishingRodUseClip(class_310 client, class_1268 hand, class_1799 stack) {
        PunchyTuningConfig.CustomAnimationEntry entry;
        if (client == null || client.field_1724 == null || stack == null || stack.method_7960() || !(stack.method_7909() instanceof class_1787) || (entry = CustomAnimationResolver.findEntry(stack, "fishingrod")) == null) {
            return null;
        }
        boolean castActive = isFishingRodCastActive(client);
        String explicitName = castActive ? entry.pull_animation : entry.throw_animation;
        if (explicitName == null || explicitName.isBlank()) {
            explicitName = CustomAnimationResolver.resolveSingleName(entry);
        }
        if (explicitName == null || explicitName.isBlank()) {
            return null;
        }
        return PunchyAnimationManager.resolveNamedClip(explicitName.trim(), client, hand);
    }

    private boolean isFishingRodCastActive(class_310 client) {
        return (client == null || client.field_1724 == null || client.field_1724.punchy$getFishing() == null) ? false : true;
    }

    private boolean isBowUseActive(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        return client.field_1724.method_6115() && (usingStack.method_7909() instanceof class_1753) && client.field_1690 != null && client.field_1690.field_1904.method_1434();
    }

    private boolean isSpearUseActive(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        return client.field_1724.method_6115() && SpearStateMachine.isSpearStack(usingStack) && client.field_1690 != null && client.field_1690.field_1904.method_1434();
    }

    private boolean isCrossbowUseActive(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        return client.field_1724.method_6115() && (usingStack.method_7909() instanceof class_1764) && client.field_1690 != null && client.field_1690.field_1904.method_1434();
    }
}
