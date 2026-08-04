package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_746;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/FallingStateMachine.class */
public class FallingStateMachine {
    private static final float MIN_FALL_DISTANCE_BLOCKS = 0.5f;
    private static final float FALLING_ENTRY_EPSILON = 1.0E-4f;
    private static final double FALLING_GROUND_DISTANCE_TRIGGER = 2.0d;
    private static final double GROUND_SCAN_DEPTH = 12.0d;
    private static final double EARLY_TRIGGER_FALL_VELOCITY = -0.04d;
    private static final float FALLING_SPEED_START_BOOST = 15.0f;
    private static final float FALLING_SPEED_END_BOOST = 8.0f;
    private static final long FALLING_SPEED_RAMP_NANOS = 1000000000;
    private static String activeType;
    private AnimationClip activeClip;
    private PunchyTuningConfig.CustomAnimationEntry activeEntry;
    private long fallingStartNanos = -1;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        if (HandEquipStateMachine.isEquipInProgress()) {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "falling");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "falling");
            return;
        }
        boolean onGround = client.field_1724.method_24828();
        boolean inWater = client.field_1724.method_5799() || client.field_1724.method_5869();
        class_746 player = client.field_1724;
        double deltaY = player.method_18798().field_1351;
        boolean descending = deltaY < 0.0d || player.field_6017 > 0.0d;
        boolean largeFall = client.field_1724.field_6017 >= 0.5d || deltaY <= EARLY_TRIGGER_FALL_VELOCITY;
        double groundDistance = distanceToGround(player);
        float triggerFallBlocks = resolveTriggerFallBlocks(player, groundDistance);
        boolean twoBlocksAboveGround = groundDistance >= FALLING_GROUND_DISTANCE_TRIGGER;
        boolean falling = (onGround || inWater || (!twoBlocksAboveGround && (!descending || !largeFall))) ? false : true;
        if (!falling) {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "falling");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "falling");
            stopIfActive();
            return;
        }
        ItemAnimationManager.setPersistentType(class_1268.field_5808, "falling", true, true);
        ItemAnimationManager.setPersistentType(class_1268.field_5810, "falling", true, true);
        activeType = "falling";
        ClipSelection selection = resolveCustomClip(client, "falling", triggerFallBlocks);
        if (selection.clip == null) {
            if (this.activeClip != null) {
                stopIfActive();
                return;
            }
            return;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        boolean playing = PunchyAnimationManager.POSE_HANDLER.isPlaying();
        if (playing && isAttackLikeClip(current)) {
            return;
        }
        if (playing && current != selection.clip) {
            stopIfActive();
            playing = PunchyAnimationManager.POSE_HANDLER.isPlaying();
            current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        }
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(selection.clip) * resolveFallingSpeedBoost();
        if (current != selection.clip || !playing) {
            class_1306 arm = selection.hand == null ? null : PunchyAnimationManager.getUseArm(client, selection.hand);
            if (PunchyAnimationManager.isDualHandedActive(client, selection.clip, 0.0f)) {
                arm = null;
            }
            CustomAnimationResolver.applyLoopSpeed(selection.entry);
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
            PunchyAnimationManager.POSE_HANDLER.setLooping(true);
            PunchyAnimationManager.POSE_HANDLER.play(selection.clip, speedMultiplier, true, false);
            this.activeClip = selection.clip;
            this.activeEntry = selection.entry;
            activeType = "falling";
            return;
        }
        PunchyAnimationManager.POSE_HANDLER.setSpeedMultiplier(speedMultiplier);
    }

    private void stopIfActive() {
        if (this.activeClip != null) {
            AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
            if (current == this.activeClip) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                if (CustomAnimationResolver.shouldSkipBlendOut(this.activeEntry)) {
                    PunchyAnimationManager.POSE_HANDLER.setBlendOutDurationNext(0.0f);
                }
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            }
        }
        this.activeClip = null;
        activeType = null;
        this.activeEntry = null;
        this.fallingStartNanos = -1L;
    }

    public static String getActiveType() {
        return activeType;
    }

    private ClipSelection resolveCustomClip(class_310 client, String type, float triggerFallBlocks) {
        if (client == null || client.field_1724 == null) {
            return new ClipSelection(null, null, null);
        }
        class_1799 main = client.field_1724.method_6047();
        ClipSelection selection = resolveFromStack(client, main, class_1268.field_5808, type, triggerFallBlocks);
        if (selection.clip != null) {
            return selection;
        }
        class_1799 off = client.field_1724.method_6079();
        return resolveFromStack(client, off, class_1268.field_5810, type, triggerFallBlocks);
    }

    private ClipSelection resolveFromStack(class_310 client, class_1799 stack, class_1268 hand, String type, float triggerFallBlocks) {
        if (stack == null) {
            return new ClipSelection(null, null, null);
        }
        PunchyTuningConfig.CustomAnimationEntry entry = resolveBestEntryForFalling(stack, type, triggerFallBlocks);
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, type, entry, client, hand);
        if (lookup.clip() == null) {
            return new ClipSelection(null, null, null);
        }
        return new ClipSelection(lookup.clip(), hand, entry);
    }

    private PunchyTuningConfig.CustomAnimationEntry resolveBestEntryForFalling(class_1799 stack, String type, float triggerFallBlocks) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries = CustomAnimationResolver.findEntries(stack, type);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        PunchyTuningConfig.CustomAnimationEntry best = null;
        float bestMinBlocks = -1.0f;
        for (PunchyTuningConfig.CustomAnimationEntry candidate : entries) {
            if (candidate != null) {
                float minBlocks = resolveMinBlocks(candidate);
                if (triggerFallBlocks + FALLING_ENTRY_EPSILON >= minBlocks && (best == null || minBlocks > bestMinBlocks)) {
                    best = candidate;
                    bestMinBlocks = minBlocks;
                }
            }
        }
        return best;
    }

    private static float resolveMinBlocks(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null || entry.min_blocks == null) {
            return 0.0f;
        }
        return Math.max(0.0f, entry.min_blocks.floatValue());
    }

    private static float resolveTriggerFallBlocks(class_746 player, double groundDistance) {
        if (player == null) {
            return 0.0f;
        }
        float current = (float) Math.max(0.0d, player.field_6017);
        float distanceToGround = (float) Math.max(0.0d, groundDistance);
        return current + distanceToGround;
    }

    private static double distanceToGround(class_746 player) {
        if (player == null || player.method_37908() == null) {
            return -1.0d;
        }
        class_238 box = player.method_5829();
        double feetY = box.field_1322 + 0.01d;
        double insetX = Math.max(0.01d, (box.field_1320 - box.field_1323) * 0.35d);
        double insetZ = Math.max(0.01d, (box.field_1324 - box.field_1321) * 0.35d);
        double centerX = (box.field_1323 + box.field_1320) * 0.5d;
        double centerZ = (box.field_1321 + box.field_1324) * 0.5d;
        double[] xs = {centerX, box.field_1323 + insetX, box.field_1320 - insetX, box.field_1323 + insetX, box.field_1320 - insetX};
        double[] zs = {centerZ, box.field_1321 + insetZ, box.field_1321 + insetZ, box.field_1324 - insetZ, box.field_1324 - insetZ};
        double best = -1.0d;
        for (int i = 0; i < xs.length; i++) {
            class_243 start = new class_243(xs[i], feetY, zs[i]);
            class_243 end = start.method_1023(0.0d, GROUND_SCAN_DEPTH, 0.0d);
            class_3965 hit = player.method_37908().method_17742(new class_3959(start, end, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, player));
            if (hit.method_17783() == class_239.class_240.field_1332) {
                double distance = Math.max(0.0d, start.field_1351 - hit.method_17784().field_1351);
                if (distance > best) {
                    best = distance;
                }
            }
        }
        return best;
    }

    private float resolveFallingSpeedBoost() {
        long now = System.nanoTime();
        if (this.fallingStartNanos < 0) {
            this.fallingStartNanos = now;
            return FALLING_SPEED_START_BOOST;
        }
        long elapsed = Math.max(0L, now - this.fallingStartNanos);
        float t = Math.min(1.0f, elapsed / 1.0E9f);
        return class_3532.method_16439(t, FALLING_SPEED_START_BOOST, FALLING_SPEED_END_BOOST);
    }

    private static boolean isAttackLikeClip(AnimationClip clip) {
        String name;
        if (clip == null || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("attack") || normalized.contains("critical");
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/FallingStateMachine$ClipSelection.class */
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
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ClipSelection.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ClipSelection.class, Object.class), ClipSelection.class, "clip;hand;entry", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/FallingStateMachine$ClipSelection;->entry:Lpunchy/config/PunchyTuningConfig$CustomAnimationEntry;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
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
