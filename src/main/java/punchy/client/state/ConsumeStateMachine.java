package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1839;
import net.minecraft.class_310;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ConsumeStateMachine.class */
public class ConsumeStateMachine {
    private static volatile boolean equipBlockedByConsumeAnimation;
    private static volatile boolean suppressUseAfterConsume;
    private static volatile class_1306 forcedRenderArm;
    private static volatile class_1799 forcedRenderStack = class_1799.field_8037;
    private boolean wasConsuming;
    private boolean finishingConsume;
    private boolean swapOnLastFramePending;
    private boolean swappedOnLastFrame;
    private AnimationClip activeClip;
    private class_1268 activeHand;
    private boolean skipBlendOut;
    private AnimationClip consumeV2StartClip;
    private AnimationClip consumeV2LoopClip;
    private AnimationClip consumeV2EndClip;
    private PunchyTuningConfig.CustomAnimationEntry consumeV2Entry;
    private ConsumeV2Phase consumeV2Phase = ConsumeV2Phase.NONE;
    private class_1799 lastConsumeV2Stack = class_1799.field_8037;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ConsumeStateMachine$ConsumeV2Phase.class */
    private enum ConsumeV2Phase {
        NONE,
        START,
        LOOP,
        END
    }

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            equipBlockedByConsumeAnimation = false;
            clearForcedRenderStack();
            return;
        }
        boolean useDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
        if (!useDown) {
            suppressUseAfterConsume = false;
        }
        boolean usingItem = client.field_1724.method_6115();
        class_1799 usingStack = client.field_1724.method_6030();
        boolean consuming = usingItem && useDown && isConsumeStack(usingStack);
        if (this.finishingConsume) {
            if (consuming) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.finishingConsume = false;
                this.swapOnLastFramePending = false;
                this.swappedOnLastFrame = false;
                this.activeClip = null;
                this.skipBlendOut = false;
                resetConsumeV2();
                PunchyAnimationManager.clearSourceHand();
            }
            if (shouldReleaseFinishingForEquip(client)) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.finishingConsume = false;
                this.swapOnLastFramePending = false;
                this.swappedOnLastFrame = false;
                this.activeClip = null;
                this.skipBlendOut = false;
                resetConsumeV2();
                PunchyAnimationManager.clearSourceHand();
                clearForcedRenderStack();
                equipBlockedByConsumeAnimation = false;
            }
            UseActionTracker.clear();
            AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
            boolean stillFinishing = current != null && this.activeClip != null && current == this.activeClip && (PunchyAnimationManager.POSE_HANDLER.isPlaying() || PunchyAnimationManager.POSE_HANDLER.isBlendOutActive());
            if (stillFinishing) {
                if (this.swapOnLastFramePending && !this.swappedOnLastFrame) {
                    float time = PunchyAnimationManager.POSE_HANDLER.getCurrentTime();
                    float length = this.activeClip.getLength();
                    if (length <= 0.0f || time >= length - 1.0E-4f) {
                        clearForcedRenderStack();
                        HandEquipStateMachine.requestRenderedSync();
                        this.swappedOnLastFrame = true;
                    }
                }
                equipBlockedByConsumeAnimation = true;
                return;
            }
            this.finishingConsume = false;
            this.swapOnLastFramePending = false;
            this.swappedOnLastFrame = false;
            this.activeClip = null;
            this.activeHand = null;
            this.skipBlendOut = false;
            resetConsumeV2();
            PunchyAnimationManager.clearSourceHand();
            clearForcedRenderStack();
            suppressUseAfterConsume = true;
        }
        class_1268 consumeHand = client.field_1724.method_6058();
        if (consumeHand == null) {
            consumeHand = class_1268.field_5808;
        }
        if (consuming && this.wasConsuming && this.activeHand != null && consumeHand != this.activeHand) {
            interruptConsumeForHandSwap(client, consumeHand);
            return;
        }
        if (consuming && HandEquipStateMachine.isEquipInProgress() && !HandEquipStateMachine.abortEquipForAction(client, consumeHand)) {
            return;
        }
        if (consuming) {
            UseActionTracker.clear();
            suppressUseAfterConsume = true;
            equipBlockedByConsumeAnimation = true;
            class_1268 hand = consumeHand;
            if (PunchyAnimationManager.isHandBlacklisted(client, hand)) {
                if (this.wasConsuming) {
                    PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                    PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                    PunchyAnimationManager.clearSourceHand();
                }
                this.wasConsuming = false;
                this.finishingConsume = false;
                this.activeClip = null;
                this.activeHand = null;
                equipBlockedByConsumeAnimation = false;
                resetConsumeV2();
                clearForcedRenderStack();
                return;
            }
            ConsumeSelection selection = resolveConsumeClip(usingStack);
            if (selection.isV2()) {
                tickConsumeV2(client, hand, usingStack, selection);
            } else {
                AnimationClip clip = selection.clip;
                if (clip != null && (!this.wasConsuming || this.activeClip != clip || !PunchyAnimationManager.POSE_HANDLER.isPlaying())) {
                    playConsume(client, hand, usingStack, clip, selection.entry);
                }
                if (this.consumeV2Phase != ConsumeV2Phase.NONE) {
                    resetConsumeV2();
                }
            }
            this.wasConsuming = true;
            this.finishingConsume = false;
            this.swapOnLastFramePending = false;
            this.swappedOnLastFrame = false;
            this.activeHand = hand;
            return;
        }
        if (this.wasConsuming) {
            boolean functionalSwapDuringConsume = isFunctionalSwapDuringConsume(client);
            boolean consumeCompleted = (usingItem || !useDown || functionalSwapDuringConsume) ? false : true;
            if (this.consumeV2Phase != ConsumeV2Phase.NONE && this.consumeV2EndClip != null) {
                transitionToConsumeV2End(this.activeHand);
                this.finishingConsume = true;
                this.swapOnLastFramePending = consumeCompleted;
                this.swappedOnLastFrame = false;
                equipBlockedByConsumeAnimation = true;
                suppressUseAfterConsume = true;
            } else if (consumeCompleted) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                if (this.skipBlendOut) {
                    PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
                }
                PunchyAnimationManager.POSE_HANDLER.releaseHold();
                PunchyAnimationManager.POSE_HANDLER.requestStopAfterCurrent();
                this.finishingConsume = true;
                this.swapOnLastFramePending = true;
                this.swappedOnLastFrame = false;
                equipBlockedByConsumeAnimation = true;
                suppressUseAfterConsume = true;
            } else {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                if (this.skipBlendOut) {
                    PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
                }
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.finishingConsume = false;
                this.swapOnLastFramePending = false;
                this.swappedOnLastFrame = false;
                equipBlockedByConsumeAnimation = false;
                resetConsumeV2();
                clearForcedRenderStack();
                if (functionalSwapDuringConsume) {
                    HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client.field_1724);
                    HandEquipStateMachine.requestRenderedSync();
                }
            }
        }
        this.wasConsuming = false;
        if (!this.finishingConsume) {
            this.swapOnLastFramePending = false;
            this.swappedOnLastFrame = false;
            equipBlockedByConsumeAnimation = false;
            resetConsumeV2();
            clearForcedRenderStack();
        }
    }

    public static boolean isEquipBlockedByConsumeAnimation() {
        return equipBlockedByConsumeAnimation;
    }

    public static boolean isUseSuppressedAfterConsume() {
        return suppressUseAfterConsume;
    }

    public static class_1799 getForcedRenderStack(class_1306 arm) {
        if (arm == null) {
            return class_1799.field_8037;
        }
        if (forcedRenderArm != arm || forcedRenderStack == null || forcedRenderStack.method_7960()) {
            return class_1799.field_8037;
        }
        return forcedRenderStack;
    }

    public static String getForcedConsumeTypeForArm(class_1306 arm) {
        if (arm == null || forcedRenderArm != arm || forcedRenderStack == null || forcedRenderStack.method_7960()) {
            return null;
        }
        class_1839 useAnim = forcedRenderStack.method_7976();
        if (useAnim == class_1839.field_8946) {
            return "useDrink";
        }
        if (useAnim == class_1839.field_8950) {
            return "useEat";
        }
        return null;
    }

    private void updateForcedRenderStack(class_1268 hand, class_1799 sourceStack) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || hand == null) {
            return;
        }
        class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
        if (sourceStack == null || sourceStack.method_7960()) {
            return;
        }
        forcedRenderArm = arm;
        forcedRenderStack = sourceStack.method_7972();
    }

    private static void clearForcedRenderStack() {
        forcedRenderArm = null;
        forcedRenderStack = class_1799.field_8037;
    }

    private void playConsume(class_310 client, class_1268 hand, class_1799 sourceStack, AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry) {
        PunchyAnimationManager.setSourceHand(client, hand);
        updateForcedRenderStack(hand, sourceStack);
        class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose() || entry == null || CustomAnimationResolver.shouldSkipBlendIn(entry);
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, clip.isHoldOnLastFrame());
        this.activeClip = clip;
        this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(entry);
    }

    private void tickConsumeV2(class_310 client, class_1268 hand, class_1799 stack, ConsumeSelection selection) {
        if (!selection.isV2Valid()) {
            resetConsumeV2();
            return;
        }
        boolean needsRestart = this.consumeV2Phase == ConsumeV2Phase.NONE || this.consumeV2Entry != selection.entry || hasConsumeStackChanged(this.lastConsumeV2Stack, stack);
        if (needsRestart) {
            this.consumeV2Entry = selection.entry;
            this.consumeV2StartClip = selection.start;
            this.consumeV2LoopClip = selection.loop;
            this.consumeV2EndClip = selection.end;
            this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(selection.entry);
            startConsumeV2(client, hand, stack, this.consumeV2StartClip, selection.entry);
            this.lastConsumeV2Stack = copyStack(stack);
            return;
        }
        this.skipBlendOut = CustomAnimationResolver.shouldSkipBlendOut(selection.entry);
        PoseHandlerWrapper handler = new PoseHandlerWrapper();
        switch (this.consumeV2Phase.ordinal()) {
            case 1:
                if (!handler.isPlaying(this.consumeV2StartClip)) {
                    transitionToConsumeV2Loop(client, hand, this.consumeV2LoopClip, selection.entry);
                }
                break;
            case 2:
                if (!handler.isPlaying(this.consumeV2LoopClip)) {
                    transitionToConsumeV2Loop(client, hand, this.consumeV2LoopClip, selection.entry);
                } else {
                    PunchyAnimationManager.POSE_HANDLER.setLooping(true);
                }
                break;
            case 3:
                if (!handler.isPlaying(this.consumeV2EndClip) && !handler.isCurrentOrBlending(this.consumeV2EndClip)) {
                    resetConsumeV2();
                }
                break;
        }
        this.lastConsumeV2Stack = copyStack(stack);
    }

    private void startConsumeV2(class_310 client, class_1268 hand, class_1799 sourceStack, AnimationClip startClip, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (client == null || startClip == null) {
            return;
        }
        PunchyAnimationManager.setSourceHand(client, hand);
        updateForcedRenderStack(hand, sourceStack);
        class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(startClip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose() || entry == null || CustomAnimationResolver.shouldSkipBlendIn(entry);
        PunchyAnimationManager.POSE_HANDLER.play(startClip, speedMultiplier, skipBlendIn, false);
        this.activeClip = startClip;
        this.consumeV2Phase = ConsumeV2Phase.START;
    }

    private void transitionToConsumeV2Loop(class_310 client, class_1268 hand, AnimationClip loopClip, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (client == null || loopClip == null) {
            return;
        }
        class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(true);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(loopClip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose() || entry == null || CustomAnimationResolver.shouldSkipBlendIn(entry);
        PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.06f);
        PunchyAnimationManager.POSE_HANDLER.play(loopClip, speedMultiplier, skipBlendIn, false);
        this.activeClip = loopClip;
        this.consumeV2Phase = ConsumeV2Phase.LOOP;
    }

    private void transitionToConsumeV2End(class_1268 hand) {
        if (this.consumeV2EndClip == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client != null) {
            PunchyAnimationManager.setSourceHand(client, hand == null ? class_1268.field_5808 : hand);
            class_1306 arm = PunchyAnimationManager.getUseArm(client, hand == null ? class_1268.field_5808 : hand);
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        }
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(this.consumeV2EndClip);
        PunchyAnimationManager.POSE_HANDLER.setBlendInDurationNext(0.0f);
        PunchyAnimationManager.POSE_HANDLER.play(this.consumeV2EndClip, speedMultiplier, true, false);
        this.activeClip = this.consumeV2EndClip;
        this.consumeV2Phase = ConsumeV2Phase.END;
    }

    private boolean hasConsumeStackChanged(class_1799 previous, class_1799 current) {
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

    private void resetConsumeV2() {
        this.consumeV2Phase = ConsumeV2Phase.NONE;
        this.consumeV2StartClip = null;
        this.consumeV2LoopClip = null;
        this.consumeV2EndClip = null;
        this.consumeV2Entry = null;
        this.lastConsumeV2Stack = class_1799.field_8037;
    }

    public static boolean isConsumeStack(class_1799 stack) {
        if (stack == null || stack.method_7960() || isThrowableDrinkLikeStack(stack)) {
            return false;
        }
        class_1839 useAnim = stack.method_7976();
        return useAnim == class_1839.field_8950 || useAnim == class_1839.field_8946;
    }

    private static boolean isThrowableDrinkLikeStack(class_1799 stack) {
        return stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150) || stack.method_31574(class_1802.field_8287) || stack.method_31574(class_1802.field_8543) || stack.method_31574(class_1802.field_8803) || stack.method_31574(class_1802.field_8634) || stack.method_31574(class_1802.field_8449);
    }

    private ConsumeSelection resolveConsumeClip(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return ConsumeSelection.single(null, null);
        }
        class_1839 useAnim = stack.method_7976();
        class_310 client = class_310.method_1551();
        class_1268 hand = (client == null || client.field_1724 == null) ? class_1268.field_5808 : client.field_1724.method_6058();
        if (hand == null) {
            hand = class_1268.field_5808;
        }
        PunchyTuningConfig.CustomAnimationEntry v2EntryExplicit = findConsumeV2Entry(stack, useAnim, false);
        if (v2EntryExplicit != null) {
            ConsumeSelection v2Selection = resolveConsumeV2Selection(client, hand, stack, v2EntryExplicit);
            if (v2Selection.isV2Valid()) {
                return v2Selection;
            }
        }
        PunchyTuningConfig.CustomAnimationEntry entryExplicit = findConsumeLegacyEntry(stack, useAnim, false);
        if (entryExplicit != null) {
            String targetType = useAnim == class_1839.field_8950 ? "useEat" : "useDrink";
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, targetType, entryExplicit, client, hand);
            if (lookup.clip() != null) {
                return ConsumeSelection.single(lookup.clip(), entryExplicit);
            }
        }
        PunchyTuningConfig.CustomAnimationEntry entryMerged = findConsumeLegacyEntry(stack, useAnim, true);
        if (entryMerged != null) {
            String targetType2 = useAnim == class_1839.field_8950 ? "useEat" : "useDrink";
            CustomAnimationResolver.ClipLookup lookup2 = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, targetType2, entryMerged, client, hand);
            if (lookup2.clip() != null) {
                return ConsumeSelection.single(lookup2.clip(), entryMerged);
            }
        }
        PunchyTuningConfig.CustomAnimationEntry v2EntryMerged = findConsumeV2Entry(stack, useAnim, true);
        if (v2EntryMerged != null) {
            ConsumeSelection v2Selection2 = resolveConsumeV2Selection(client, hand, stack, v2EntryMerged);
            if (v2Selection2.isV2Valid()) {
                return v2Selection2;
            }
        }
        if (useAnim == class_1839.field_8950) {
            return ConsumeSelection.single(PunchyAnimationManager.resolveHandedClip("use_eat", client, hand), null);
        }
        if (useAnim == class_1839.field_8946) {
            return ConsumeSelection.single(PunchyAnimationManager.resolveHandedClip("use_drink", client, hand), null);
        }
        return ConsumeSelection.single(null, null);
    }

    private PunchyTuningConfig.CustomAnimationEntry findConsumeV2Entry(class_1799 stack, class_1839 useAnim, boolean includeDefaults) {
        if (useAnim == class_1839.field_8950) {
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "eating", includeDefaults);
            if (entry == null) {
                entry = CustomAnimationResolver.findEntry(stack, "eating_v2", includeDefaults);
            }
            if (entry == null) {
                entry = CustomAnimationResolver.findEntry(stack, "useitem_v2", includeDefaults);
            }
            if (entry == null) {
                entry = CustomAnimationResolver.findEntry(stack, "useeat_v2", includeDefaults);
            }
            if (entry == null) {
                entry = CustomAnimationResolver.findEntry(stack, "use_eat_v2", includeDefaults);
            }
            return entry;
        }
        if (useAnim == class_1839.field_8946) {
            PunchyTuningConfig.CustomAnimationEntry entry2 = CustomAnimationResolver.findEntry(stack, "drinking", includeDefaults);
            if (entry2 == null) {
                entry2 = CustomAnimationResolver.findEntry(stack, "drinking_v2", includeDefaults);
            }
            if (entry2 == null) {
                entry2 = CustomAnimationResolver.findEntry(stack, "useitem_v2", includeDefaults);
            }
            if (entry2 == null) {
                entry2 = CustomAnimationResolver.findEntry(stack, "usedrink_v2", includeDefaults);
            }
            if (entry2 == null) {
                entry2 = CustomAnimationResolver.findEntry(stack, "use_drink_v2", includeDefaults);
            }
            return entry2;
        }
        return null;
    }

    private PunchyTuningConfig.CustomAnimationEntry findConsumeLegacyEntry(class_1799 stack, class_1839 useAnim, boolean includeDefaults) {
        if (useAnim == class_1839.field_8950) {
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "useEat", includeDefaults);
            if (entry == null) {
                entry = CustomAnimationResolver.findEntry(stack, "use_eat", includeDefaults);
            }
            return entry;
        }
        if (useAnim == class_1839.field_8946) {
            PunchyTuningConfig.CustomAnimationEntry entry2 = CustomAnimationResolver.findEntry(stack, "useDrink", includeDefaults);
            if (entry2 == null) {
                entry2 = CustomAnimationResolver.findEntry(stack, "use_drink", includeDefaults);
            }
            return entry2;
        }
        return null;
    }

    public boolean isActive() {
        return this.wasConsuming && this.activeClip != null;
    }

    private boolean shouldReleaseFinishingForEquip(class_310 client) {
        if (!this.finishingConsume || client == null || client.field_1724 == null || this.activeHand == null) {
            return false;
        }
        class_1799 reference = forcedRenderStack == null ? class_1799.field_8037 : forcedRenderStack;
        if (reference.method_7960()) {
            return false;
        }
        class_1799 current = client.field_1724.method_5998(this.activeHand);
        class_1799 now = current == null ? class_1799.field_8037 : current;
        return !class_1799.method_31577(reference, now);
    }

    private void interruptConsumeForHandSwap(class_310 client, class_1268 newHand) {
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
        PunchyAnimationManager.clearSourceHand();
        this.wasConsuming = false;
        this.finishingConsume = false;
        this.swapOnLastFramePending = false;
        this.swappedOnLastFrame = false;
        this.activeClip = null;
        this.activeHand = null;
        this.skipBlendOut = false;
        suppressUseAfterConsume = false;
        equipBlockedByConsumeAnimation = false;
        resetConsumeV2();
        clearForcedRenderStack();
        HandEquipStateMachine.cancelOffhandTransitionForPoseOverride(client.field_1724);
        HandEquipStateMachine.requestRenderedSync();
        if (newHand == class_1268.field_5810) {
            HandEquipStateMachine.triggerPickupEquipCycle(client, class_1268.field_5810);
        }
    }

    private boolean isFunctionalSwapDuringConsume(class_310 client) {
        class_1268 class_1268Var;
        if (client == null || client.field_1724 == null || this.activeHand == null) {
            return false;
        }
        class_1799 reference = forcedRenderStack == null ? class_1799.field_8037 : forcedRenderStack;
        if (reference.method_7960()) {
            return false;
        }
        class_1799 activeNow = client.field_1724.method_5998(this.activeHand);
        if (this.activeHand == class_1268.field_5808) {
            class_1268Var = class_1268.field_5810;
        } else {
            class_1268Var = class_1268.field_5808;
        }
        class_1268 oppositeHand = class_1268Var;
        class_1799 oppositeNow = client.field_1724.method_5998(oppositeHand);
        return !class_1799.method_31577(reference, activeNow) && class_1799.method_31577(reference, oppositeNow);
    }

    private ConsumeSelection resolveConsumeV2Selection(class_310 client, class_1268 hand, class_1799 stack, PunchyTuningConfig.CustomAnimationEntry entry) {
        if (client == null || entry == null) {
            return ConsumeSelection.single(null, null);
        }
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForEntry(stack, entry);
        AnimationClip start = resolveNamedClip(CustomAnimationResolver.resolveStartName(entry), client, hand, preferredOption);
        AnimationClip loop = resolveNamedClip(CustomAnimationResolver.resolveLoopingName(entry), client, hand, preferredOption);
        AnimationClip end = resolveNamedClip(CustomAnimationResolver.resolveEndName(entry), client, hand, preferredOption);
        return ConsumeSelection.v2(start, loop, end, entry);
    }

    private AnimationClip resolveNamedClip(String name, class_310 client, class_1268 hand, String preferredOption) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return PunchyAnimationManager.resolveNamedClip(name, client, hand, preferredOption);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ConsumeStateMachine$ConsumeSelection.class */
    private static final class ConsumeSelection extends Record {
        private final AnimationClip clip;
        private final PunchyTuningConfig.CustomAnimationEntry entry;
        private final AnimationClip start;
        private final AnimationClip loop;
        private final AnimationClip end;

        private ConsumeSelection(AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry, AnimationClip start, AnimationClip loop, AnimationClip end) {
            this.clip = clip;
            this.entry = entry;
            this.start = start;
            this.loop = loop;
            this.end = end;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ConsumeSelection.class), ConsumeSelection.class, "clip;entry;start;loop;end", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ConsumeSelection.class), ConsumeSelection.class, "clip;entry;start;loop;end", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ConsumeSelection.class, Object.class), ConsumeSelection.class, "clip;entry;start;loop;end", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->start:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->loop:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/ConsumeStateMachine$ConsumeSelection;->end:Lpunchy/client/animation/data/AnimationClip;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public PunchyTuningConfig.CustomAnimationEntry entry() {
            return this.entry;
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

        private static ConsumeSelection single(AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry) {
            return new ConsumeSelection(clip, entry, null, null, null);
        }

        private static ConsumeSelection v2(AnimationClip start, AnimationClip loop, AnimationClip end, PunchyTuningConfig.CustomAnimationEntry entry) {
            return new ConsumeSelection(start, entry, start, loop, end);
        }

        private boolean isV2() {
            return (this.start == null && this.loop == null && this.end == null) ? false : true;
        }

        private boolean isV2Valid() {
            return (this.start == null || this.loop == null || this.end == null) ? false : true;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ConsumeStateMachine$PoseHandlerWrapper.class */
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
