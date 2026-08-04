package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/IdlePoseStateMachine.class */
public final class IdlePoseStateMachine {
    private static final float IDLE_RETURN_BLEND_SECONDS = 0.27f;
    private String activeIdleClipName = null;
    private class_1306 activeIdleArm = null;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/IdlePoseStateMachine$IdleSelection.class */
    private static final class IdleSelection extends Record {
        private final AnimationClip clip;
        private final class_1306 arm;
        private final PunchyTuningConfig.CustomFlags flags;

        private IdleSelection(AnimationClip clip, class_1306 arm, PunchyTuningConfig.CustomFlags flags) {
            this.clip = clip;
            this.arm = arm;
            this.flags = flags;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, IdleSelection.class), IdleSelection.class, "clip;arm;flags", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->arm:Lnet/minecraft/class_1306;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->flags:Lpunchy/config/PunchyTuningConfig$CustomFlags;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, IdleSelection.class), IdleSelection.class, "clip;arm;flags", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->arm:Lnet/minecraft/class_1306;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->flags:Lpunchy/config/PunchyTuningConfig$CustomFlags;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, IdleSelection.class, Object.class), IdleSelection.class, "clip;arm;flags", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->arm:Lnet/minecraft/class_1306;", "FIELD:Lpunchy/client/state/IdlePoseStateMachine$IdleSelection;->flags:Lpunchy/config/PunchyTuningConfig$CustomFlags;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public class_1306 arm() {
            return this.arm;
        }

        public PunchyTuningConfig.CustomFlags flags() {
            return this.flags;
        }
    }

    public void tick(class_310 client) {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (client == null || client.field_1724 == null) {
            clearActiveIdle(handler);
            return;
        }
        String blockReason = resolveBlockReason(handler);
        if (blockReason != null) {
            clearActiveIdle(handler);
            return;
        }
        IdleSelection desired = resolveDesiredIdle(client, handler);
        if (desired == null || desired.clip() == null || desired.arm() == null) {
            clearActiveIdle(handler);
            return;
        }
        if (isForeignSecondaryActive(handler, desired.arm())) {
            if (shouldIgnoreForeignSecondary(handler)) {
                handler.clearSecondaryClip();
            } else {
                this.activeIdleClipName = null;
                this.activeIdleArm = null;
                return;
            }
        }
        String desiredClipName = clipName(desired.clip());
        if (isDesiredIdleAlreadyApplied(handler, desired, desiredClipName)) {
            return;
        }
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(desired.clip());
        handler.setSecondaryClipAtTimeWithBlendFromCurrentAndFlags(desired.clip(), desired.arm(), speedMultiplier, 0.0f, true, IDLE_RETURN_BLEND_SECONDS, desired.flags());
        handler.setSecondaryCompatIdle(true);
        this.activeIdleClipName = desiredClipName;
        this.activeIdleArm = desired.arm();
    }

    private IdleSelection resolveDesiredIdle(class_310 client, PoseHandler handler) {
        class_1306 mainArm = client.field_1724.method_6068();
        return resolveIdleForArm(client, mainArm);
    }

    private IdleSelection resolveIdleForArm(class_310 client, class_1306 arm) {
        class_1268 hand;
        class_1799 stack;
        if (client == null || client.field_1724 == null || arm == null || (hand = PunchyAnimationManager.getHandForArm(client, arm)) == null || PunchyConfig.isHandBlacklisted(client.field_1724, hand) || (stack = resolveStack(client, arm)) == null || stack.method_7960()) {
            return null;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "idle", false);
        if (entry == null) {
            entry = CustomAnimationResolver.findEntry(stack, "idle");
        }
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "idle", entry, client, hand);
        if (lookup == null || lookup.clip() == null) {
            return null;
        }
        return new IdleSelection(lookup.clip(), arm, entry != null ? entry.customFlags : null);
    }

    private void clearActiveIdle(PoseHandler handler) {
        if (handler == null) {
            this.activeIdleClipName = null;
            this.activeIdleArm = null;
            return;
        }
        if (handler.isSecondaryCompatIdle()) {
            handler.clearSecondaryClipWithBlendOut();
            handler.setSecondaryCompatIdle(false);
        }
        this.activeIdleClipName = null;
        this.activeIdleArm = null;
    }

    private boolean isForeignSecondaryActive(PoseHandler handler, class_1306 desiredArm) {
        return (handler == null || desiredArm == null || handler.isSecondaryCompatIdle() || handler.getSecondaryClip() == null || handler.getSecondaryArm() == null || handler.getSecondaryArm() == desiredArm) ? false : true;
    }

    private boolean shouldIgnoreForeignSecondary(PoseHandler handler) {
        AnimationClip secondary;
        return (handler == null || (secondary = handler.getSecondaryClip()) == null || isEquipClip(secondary) || handler.getCurrentClip() != null || handler.isPlaying() || handler.isBlendOutActive()) ? false : true;
    }

    private boolean isDesiredIdleAlreadyApplied(PoseHandler handler, IdleSelection desired, String desiredClipName) {
        AnimationClip secondary;
        return handler != null && desired != null && desiredClipName != null && desiredClipName.equals(this.activeIdleClipName) && desired.arm() == this.activeIdleArm && handler.isSecondaryCompatIdle() && (secondary = handler.getSecondaryClip()) != null && desiredClipName.equals(clipName(secondary)) && handler.getSecondaryArm() == desired.arm();
    }

    private class_1799 resolveStack(class_310 client, class_1306 arm) {
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? client.field_1724.method_6047() : client.field_1724.method_6079() : mainRight ? client.field_1724.method_6079() : client.field_1724.method_6047();
    }

    private String resolveBlockReason(PoseHandler handler) {
        if (handler == null) {
            return "no_handler";
        }
        AnimationClip primary = handler.getCurrentClip();
        if (primary != null && !isEquipClip(primary) && handler.isPlaying()) {
            return "pose_playing";
        }
        boolean walkOnWaterActive = ItemAnimationManager.isTypeActive("walk_on_water_v2");
        boolean compatIdleActive = handler.isSecondaryCompatIdle();
        if (PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            if (walkOnWaterActive && compatIdleActive) {
                return null;
            }
            return "swim_playing";
        }
        if (PunchyAnimationManager.SWIM_HANDLER.isBlendOutActive()) {
            if (walkOnWaterActive && compatIdleActive) {
                return null;
            }
            return "swim_blend_out";
        }
        if (PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() != null) {
            if (walkOnWaterActive && compatIdleActive) {
                return null;
            }
            return "swim_clip_lingering";
        }
        if (walkOnWaterActive) {
            return "item_walk_on_water";
        }
        if (ItemAnimationManager.isTypeActive("mining")) {
            return "item_mining";
        }
        if (ItemAnimationManager.isTypeActive("useItem")) {
            return "item_use";
        }
        return null;
    }

    private boolean isEquipClip(AnimationClip clip) {
        String name;
        if (clip == null || (name = clip.getName()) == null) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("hand_in") || normalized.equals("hand_out") || normalized.startsWith("hand_in_") || normalized.startsWith("hand_out_");
    }

    private String clipName(AnimationClip clip) {
        return clip == null ? "null" : clip.getName();
    }
}
