package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningActionTracker.class */
public final class MiningActionTracker {
    private static boolean miningActive;
    private static boolean triggered;
    private static boolean miningMud;
    private static class_2338 miningPos;
    private static class_243 miningHitLocation;
    private static long triggerEpoch;
    private static long lastRecordNano;
    private static class_2338 lastTrackablePos;
    private static class_243 lastTrackableHitLocation;
    private static class_2680 lastTrackableState;
    private static long lastTrackableNano;
    private static class_2350 miningFace = class_2350.field_11043;
    private static class_2350 lastTrackableFace = class_2350.field_11043;

    private MiningActionTracker() {
    }

    public static void recordStart() {
        recordStart(null, null);
    }

    public static void recordContinue() {
        recordContinue(null, null);
    }

    public static void recordStart(class_2338 pos) {
        recordStart(pos, null);
    }

    public static void recordStart(class_2338 pos, class_2350 face) {
        recordStart(pos, face, null);
    }

    public static void recordStart(class_2338 pos, class_2350 face, class_243 hitLocation) {
        miningActive = true;
        triggered = true;
        triggerEpoch++;
        miningPos = pos;
        if (face != null) {
            miningFace = face;
        }
        miningHitLocation = hitLocation;
        class_2680 state = resolveState(pos);
        miningMud = isMudBlock(state);
        cacheTrackableTarget(pos, face, hitLocation, state);
        lastRecordNano = System.nanoTime();
    }

    public static void recordContinue(class_2338 pos) {
        recordContinue(pos, null);
    }

    public static void recordContinue(class_2338 pos, class_2350 face) {
        recordContinue(pos, face, null);
    }

    public static void recordContinue(class_2338 pos, class_2350 face, class_243 hitLocation) {
        miningActive = true;
        miningPos = pos;
        if (face != null) {
            miningFace = face;
        }
        miningHitLocation = hitLocation;
        class_2680 state = resolveState(pos);
        miningMud = isMudBlock(state);
        cacheTrackableTarget(pos, face, hitLocation, state);
        lastRecordNano = System.nanoTime();
    }

    public static void recordAttempt(class_2338 pos, class_2350 face, class_243 hitLocation) {
        triggered = true;
        triggerEpoch++;
        miningPos = pos;
        if (face != null) {
            miningFace = face;
        }
        miningHitLocation = hitLocation;
        class_2680 state = resolveState(pos);
        miningMud = isMudBlock(state);
        cacheTrackableTarget(pos, face, hitLocation, state);
        lastRecordNano = System.nanoTime();
    }

    public static void recordStop() {
        miningActive = false;
        miningMud = false;
        miningPos = null;
        miningFace = class_2350.field_11043;
        miningHitLocation = null;
    }

    public static boolean isMiningActive() {
        return miningActive;
    }

    public static boolean isMiningMud() {
        return miningMud;
    }

    public static boolean consumeTrigger() {
        if (!triggered) {
            return false;
        }
        triggered = false;
        return true;
    }

    public static Target peekTarget() {
        if (!miningActive) {
            return null;
        }
        class_2680 state = resolveState(miningPos);
        if (miningPos == null || state == null || state.method_26215()) {
            return null;
        }
        return new Target(miningPos, miningFace, state, miningHitLocation);
    }

    public static Target peekRecentTarget(long windowNanos) {
        if (windowNanos <= 0 || lastTrackableNano <= 0 || System.nanoTime() - lastTrackableNano > windowNanos || lastTrackablePos == null || lastTrackableState == null || lastTrackableState.method_26215()) {
            return null;
        }
        return new Target(lastTrackablePos, lastTrackableFace, lastTrackableState, lastTrackableHitLocation);
    }

    public static class_2338 peekPos() {
        return miningPos;
    }

    public static class_2350 peekFace() {
        return miningFace;
    }

    public static class_2680 peekState() {
        return resolveState(miningPos);
    }

    public static long peekEpoch() {
        return triggerEpoch;
    }

    public static boolean wasRecentMining(long windowNanos) {
        return windowNanos > 0 && lastRecordNano > 0 && System.nanoTime() - lastRecordNano <= windowNanos;
    }

    private static class_2680 resolveState(class_2338 pos) {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1687 == null || pos == null) {
            return null;
        }
        return mc.field_1687.method_8320(pos);
    }

    private static boolean isMudBlock(class_2680 state) {
        if (state == null) {
            return false;
        }
        return state.method_27852(class_2246.field_37576) || state.method_27852(class_2246.field_37556) || state.method_27852(class_2246.field_37557) || state.method_27852(class_2246.field_37547);
    }

    private static void cacheTrackableTarget(class_2338 pos, class_2350 face, class_243 hitLocation, class_2680 state) {
        if (pos == null || state == null || state.method_26215()) {
            return;
        }
        lastTrackablePos = pos.method_10062();
        lastTrackableFace = face != null ? face : miningFace;
        lastTrackableHitLocation = hitLocation;
        lastTrackableState = state;
        lastTrackableNano = System.nanoTime();
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MiningActionTracker$Target.class */
    public static final class Target extends Record {
        private final class_2338 pos;
        private final class_2350 face;
        private final class_2680 state;
        private final class_243 hitLocation;

        public Target(class_2338 pos, class_2350 face, class_2680 state, class_243 hitLocation) {
            this.pos = pos;
            this.face = face;
            this.state = state;
            this.hitLocation = hitLocation;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, Target.class), Target.class, "pos;face;state;hitLocation", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->pos:Lnet/minecraft/class_2338;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->face:Lnet/minecraft/class_2350;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->state:Lnet/minecraft/class_2680;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->hitLocation:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, Target.class), Target.class, "pos;face;state;hitLocation", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->pos:Lnet/minecraft/class_2338;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->face:Lnet/minecraft/class_2350;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->state:Lnet/minecraft/class_2680;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->hitLocation:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, Target.class, Object.class), Target.class, "pos;face;state;hitLocation", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->pos:Lnet/minecraft/class_2338;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->face:Lnet/minecraft/class_2350;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->state:Lnet/minecraft/class_2680;", "FIELD:Lpunchy/client/state/MiningActionTracker$Target;->hitLocation:Lnet/minecraft/class_243;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_2338 pos() {
            return this.pos;
        }

        public class_2350 face() {
            return this.face;
        }

        public class_2680 state() {
            return this.state;
        }

        public class_243 hitLocation() {
            return this.hitLocation;
        }
    }
}
