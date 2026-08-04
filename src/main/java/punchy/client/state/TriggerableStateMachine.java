package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1041;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.client.state.TriggerableActionTracker;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableStateMachine.class */
public class TriggerableStateMachine {
    private static final Map<Integer, Boolean> LAST_KEY_STATE = new HashMap();

    public static void trigger(class_1268 hand) {
        TriggerableActionTracker.record(hand, -1);
    }

    public void tick(class_310 client) {
        ClipSelection selection;
        if (client == null || client.field_1724 == null) {
            return;
        }
        TriggerableActionTracker.Trigger trigger = TriggerableActionTracker.consumeTrigger();
        if (trigger == null) {
            trigger = pollKeyTrigger(client);
        }
        if (trigger == null) {
            return;
        }
        ItemAnimationManager.forceTypeOnce(trigger.hand(), "triggerable");
        if (trigger.hand() != null) {
            class_1799 stack = client.field_1724.method_5998(trigger.hand());
            selection = resolveFromStack(client, stack, trigger.hand(), "triggerable", trigger.keyCode());
        } else {
            selection = resolveCustomClip(client, "triggerable", trigger.keyCode());
        }
        if (selection.clip == null) {
            return;
        }
        if (HandEquipStateMachine.isEquipInProgress() && shouldAbortEquipForHand(client, selection.hand) && !HandEquipStateMachine.abortEquipForAction(client, selection.hand)) {
            return;
        }
        playClip(client, selection);
    }

    private void playClip(class_310 client, ClipSelection selection) {
        AnimationClip clip = selection.clip;
        if (clip == null) {
            return;
        }
        CustomAnimationResolver.applyLoopSpeed(selection.entry);
        class_1306 arm = selection.hand == null ? null : PunchyAnimationManager.getUseArm(client, selection.hand);
        if (PunchyAnimationManager.isDualHandedActive(client, clip, 0.0f)) {
            arm = null;
        }
        PunchyAnimationManager.setSourceHand(client, selection.hand == null ? class_1268.field_5808 : selection.hand);
        primeOppositeArmOverlapDuringEquip(client, selection.hand, arm);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendIn(selection.entry);
        CustomAnimationResolver.playConfiguredClip(clip, selection.entry, speedMultiplier, skipBlendIn, false);
    }

    private ClipSelection resolveCustomClip(class_310 client, String type, int keyCode) {
        if (client == null || client.field_1724 == null) {
            return new ClipSelection(null, null, null);
        }
        class_1799 main = client.field_1724.method_6047();
        ClipSelection selection = resolveFromStack(client, main, class_1268.field_5808, type, keyCode);
        if (selection.clip != null) {
            return selection;
        }
        class_1799 off = client.field_1724.method_6079();
        return resolveFromStack(client, off, class_1268.field_5810, type, keyCode);
    }

    private ClipSelection resolveFromStack(class_310 client, class_1799 stack, class_1268 hand, String type, int keyCode) {
        if (stack == null) {
            return new ClipSelection(null, null, null);
        }
        if (PunchyConfig.isItemBlacklisted(stack)) {
            return new ClipSelection(null, null, null);
        }
        PunchyTuningConfig.CustomAnimationEntry entry = resolveEntryForKey(CustomAnimationResolver.findEntries(stack, type), keyCode);
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, type, entry, client, hand);
        if (lookup.clip() == null) {
            return new ClipSelection(null, null, null);
        }
        return new ClipSelection(lookup.clip(), hand, entry);
    }

    private PunchyTuningConfig.CustomAnimationEntry resolveEntryForKey(List<PunchyTuningConfig.CustomAnimationEntry> entries, int keyCode) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        if (keyCode >= 0) {
            for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
                if (entry != null && TriggerableKeyResolver.matches(entry.key, keyCode)) {
                    return entry;
                }
            }
            return null;
        }
        for (PunchyTuningConfig.CustomAnimationEntry entry2 : entries) {
            if (entry2 != null && (entry2.key == null || entry2.key.isBlank())) {
                return entry2;
            }
        }
        return entries.get(0);
    }

    private TriggerableActionTracker.Trigger pollKeyTrigger(class_310 client) {
        class_1041 window;
        if (client == null || client.field_1724 == null || client.field_1755 != null || (window = client.method_22683()) == null) {
            return null;
        }
        List<PunchyTuningConfig.CustomAnimationEntry> mainEntries = CustomAnimationResolver.findEntries(client.field_1724.method_6047(), "triggerable");
        List<PunchyTuningConfig.CustomAnimationEntry> offEntries = CustomAnimationResolver.findEntries(client.field_1724.method_6079(), "triggerable");
        if (mainEntries.isEmpty() && offEntries.isEmpty()) {
            LAST_KEY_STATE.clear();
            return null;
        }
        List<KeyCandidate> candidates = new ArrayList<>();
        addCandidates(candidates, mainEntries, class_1268.field_5808);
        addCandidates(candidates, offEntries, class_1268.field_5810);
        if (candidates.isEmpty()) {
            LAST_KEY_STATE.clear();
            return null;
        }
        Set<Integer> activeCodes = new HashSet<>();
        KeyCandidate fired = null;
        for (KeyCandidate candidate : candidates) {
            int code = candidate.keyCode;
            activeCodes.add(Integer.valueOf(code));
            boolean down = class_3675.method_15987(window.method_4490(), code);
            boolean wasDown = LAST_KEY_STATE.getOrDefault(Integer.valueOf(code), false).booleanValue();
            if (down && !wasDown && fired == null) {
                fired = candidate;
            }
            LAST_KEY_STATE.put(Integer.valueOf(code), Boolean.valueOf(down));
        }
        LAST_KEY_STATE.keySet().removeIf(code2 -> {
            return !activeCodes.contains(code2);
        });
        if (fired == null) {
            return null;
        }
        return new TriggerableActionTracker.Trigger(fired.hand, fired.keyCode);
    }

    private void addCandidates(List<KeyCandidate> out, List<PunchyTuningConfig.CustomAnimationEntry> entries, class_1268 hand) {
        int keyCode;
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && (keyCode = TriggerableKeyResolver.resolveKeyCode(entry.key)) >= 0) {
                out.add(new KeyCandidate(hand, keyCode));
            }
        }
    }

    private boolean shouldAbortEquipForHand(class_310 client, class_1268 hand) {
        class_1306 equipArm;
        if (client == null || hand == null || (equipArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm()) == null) {
            return false;
        }
        class_1306 actionArm = PunchyAnimationManager.getUseArm(client, hand);
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableStateMachine$ClipSelection.class */
    private static final class ClipSelection extends Record {
        private final AnimationClip clip;
        private final class_1268 hand;
        private final PunchyTuningConfig.CustomAnimationEntry entry;

        private ClipSelection(AnimationClip clip, class_1268 hand, PunchyTuningConfig.CustomAnimationEntry entry) {
            this.clip = clip;
            this.hand = hand;
            this.entry = entry;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ClipSelection.class, Object.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public PunchyTuningConfig.CustomAnimationEntry entry() {
            return this.entry;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableStateMachine$KeyCandidate.class */
    private static final class KeyCandidate extends Record {
        private final class_1268 hand;
        private final int keyCode;

        private KeyCandidate(class_1268 hand, int keyCode) {
            this.hand = hand;
            this.keyCode = keyCode;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, KeyCandidate.class), KeyCandidate.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->keyCode:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, KeyCandidate.class), KeyCandidate.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->keyCode:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, KeyCandidate.class, Object.class), KeyCandidate.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableStateMachine$KeyCandidate;->keyCode:I").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public int keyCode() {
            return this.keyCode;
        }
    }
}
