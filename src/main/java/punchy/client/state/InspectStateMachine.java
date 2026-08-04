package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/InspectStateMachine.class */
public class InspectStateMachine {
    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null || HandEquipStateMachine.isEquipInProgress() || !InspectActionTracker.consume()) {
            return;
        }
        ItemAnimationManager.forceTypeOnce(class_1268.field_5808, "inspect");
        ClipSelection selection = resolveCustomClip(client, "inspect");
        if (selection.clip == null) {
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
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, false);
    }

    private ClipSelection resolveCustomClip(class_310 client, String type) {
        if (client == null || client.field_1724 == null) {
            return new ClipSelection(null, null, null);
        }
        class_1799 main = client.field_1724.method_6047();
        ClipSelection selection = resolveFromStack(client, main, class_1268.field_5808, type);
        if (selection.clip != null) {
            return selection;
        }
        class_1799 off = client.field_1724.method_6079();
        return resolveFromStack(client, off, class_1268.field_5810, type);
    }

    private ClipSelection resolveFromStack(class_310 client, class_1799 stack, class_1268 hand, String type) {
        if (stack == null) {
            return new ClipSelection(null, null, null);
        }
        if (PunchyConfig.isItemBlacklisted(stack)) {
            return new ClipSelection(null, null, null);
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, type);
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, type, entry, client, hand);
        if (lookup.clip() == null) {
            return new ClipSelection(null, null, null);
        }
        return new ClipSelection(lookup.clip(), hand, entry);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/InspectStateMachine$ClipSelection.class */
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
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ClipSelection.class, Object.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/InspectStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
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
}
