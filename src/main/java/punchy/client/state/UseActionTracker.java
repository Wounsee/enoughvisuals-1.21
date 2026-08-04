package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.class_1268;
import net.minecraft.class_3965;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/UseActionTracker.class */
public final class UseActionTracker {
    private static final long TRIGGER_VALID_NANOS = 1250000000;
    private static final long FALLBACK_VALID_NANOS = 900000000;
    private static Trigger mainConfirmed;
    private static Trigger offConfirmed;
    private static Trigger mainFallback;
    private static Trigger offFallback;
    private static class_1268 triggerHand = class_1268.field_5808;
    private static long triggerEpoch;
    private static long lastRecordNano;
    private static long lastMainUseNano;
    private static long lastOffUseNano;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/UseActionTracker$TriggerType.class */
    public enum TriggerType {
        USE_ITEM,
        USE_ITEM_ON,
        DROP_ITEM,
        INTERACT,
        INTERACT_AT
    }

    private UseActionTracker() {
    }

    public static void recordUse(class_1268 hand, TriggerType type) {
        recordUse(hand, type, null);
    }

    public static void recordUse(class_1268 hand, TriggerType type, class_3965 blockHit) {
        if (ConsumeStateMachine.isUseSuppressedAfterConsume()) {
            return;
        }
        long now = System.nanoTime();
        class_1268 resolvedHand = hand == null ? class_1268.field_5808 : hand;
        Trigger trigger = new Trigger(resolvedHand, type == null ? TriggerType.USE_ITEM : type, blockHit, false, now);
        setConfirmed(resolvedHand, trigger);
        discardFallbackForHand(resolvedHand);
        triggerEpoch++;
        triggerHand = resolvedHand;
        lastRecordNano = now;
        markRecentUse(resolvedHand, now);
    }

    public static void recordFallbackUse(class_1268 hand, TriggerType type, class_3965 blockHit) {
        if (ConsumeStateMachine.isUseSuppressedAfterConsume()) {
            return;
        }
        long now = System.nanoTime();
        class_1268 resolvedHand = hand == null ? class_1268.field_5808 : hand;
        Trigger fallback = new Trigger(resolvedHand, type == null ? TriggerType.USE_ITEM : type, blockHit, true, now);
        setFallback(resolvedHand, fallback);
        markRecentUse(resolvedHand, now);
    }

    public static Trigger peekTrigger() {
        trimExpired();
        return latest(mainConfirmed, offConfirmed);
    }

    public static class_1268 peekLastHand() {
        return triggerHand;
    }

    public static long peekEpoch() {
        return triggerEpoch;
    }

    public static boolean wasRecentUse(long windowNanos) {
        return windowNanos > 0 && lastRecordNano > 0 && System.nanoTime() - lastRecordNano <= windowNanos;
    }

    public static Trigger consumeTrigger() {
        trimExpired();
        Trigger latestConfirmed = latest(mainConfirmed, offConfirmed);
        if (latestConfirmed == null) {
            return null;
        }
        clearConfirmed(latestConfirmed.hand());
        return latestConfirmed;
    }

    public static Trigger consumeLatestTrigger() {
        trimExpired();
        Trigger latestConfirmed = latest(mainConfirmed, offConfirmed);
        Trigger latestFallback = latest(mainFallback, offFallback);
        if (latestFallback != null && (latestConfirmed == null || latestFallback.recordNano() >= latestConfirmed.recordNano())) {
            clearFallback(latestFallback.hand());
            return latestFallback;
        }
        if (latestConfirmed != null) {
            clearConfirmed(latestConfirmed.hand());
        }
        return latestConfirmed;
    }

    public static void pushBack(Trigger trigger) {
        if (trigger == null) {
            return;
        }
        if (trigger.fallback()) {
            setFallback(trigger.hand(), trigger);
        } else {
            setConfirmed(trigger.hand(), trigger);
        }
    }

    public static Trigger consumeFallbackTrigger() {
        trimExpired();
        Trigger latestFallback = latest(mainFallback, offFallback);
        if (latestFallback == null) {
            return null;
        }
        clearFallback(latestFallback.hand());
        return latestFallback;
    }

    public static void discardOppositeOlderThan(class_1268 hand, long recordNano) {
        if (hand == null || recordNano <= 0) {
            return;
        }
        class_1268 opposite = hand == class_1268.field_5808 ? class_1268.field_5810 : class_1268.field_5808;
        Trigger oppositeConfirmed = getConfirmed(opposite);
        if (oppositeConfirmed != null && oppositeConfirmed.recordNano() <= recordNano) {
            clearConfirmed(opposite);
        }
        Trigger oppositeFallback = getFallback(opposite);
        if (oppositeFallback != null && oppositeFallback.recordNano() <= recordNano) {
            clearFallback(opposite);
        }
    }

    public static void discardFallbackForHand(class_1268 hand) {
        if (hand == null) {
            return;
        }
        clearFallback(hand);
    }

    public static boolean wasRecentUseForHand(class_1268 hand, long windowNanos) {
        if (hand == null || windowNanos <= 0) {
            return false;
        }
        long stamp = hand == class_1268.field_5810 ? lastOffUseNano : lastMainUseNano;
        return stamp > 0 && System.nanoTime() - stamp <= windowNanos;
    }

    public static void clear() {
        mainConfirmed = null;
        offConfirmed = null;
        mainFallback = null;
        offFallback = null;
    }

    private static void trimExpired() {
        long now = System.nanoTime();
        mainConfirmed = keepIfFresh(mainConfirmed, now, TRIGGER_VALID_NANOS);
        offConfirmed = keepIfFresh(offConfirmed, now, TRIGGER_VALID_NANOS);
        mainFallback = keepIfFresh(mainFallback, now, FALLBACK_VALID_NANOS);
        offFallback = keepIfFresh(offFallback, now, FALLBACK_VALID_NANOS);
    }

    private static Trigger keepIfFresh(Trigger trigger, long now, long window) {
        if (trigger != null && trigger.recordNano() > 0 && now - trigger.recordNano() <= window) {
            return trigger;
        }
        return null;
    }

    private static Trigger latest(Trigger first, Trigger second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.recordNano() >= first.recordNano() ? second : first;
    }

    private static void setConfirmed(class_1268 hand, Trigger trigger) {
        if (hand == class_1268.field_5810) {
            offConfirmed = newer(offConfirmed, trigger);
        } else {
            mainConfirmed = newer(mainConfirmed, trigger);
        }
    }

    private static void clearConfirmed(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offConfirmed = null;
        } else {
            mainConfirmed = null;
        }
    }

    private static Trigger getConfirmed(class_1268 hand) {
        return hand == class_1268.field_5810 ? offConfirmed : mainConfirmed;
    }

    private static void setFallback(class_1268 hand, Trigger trigger) {
        if (hand == class_1268.field_5810) {
            offFallback = newer(offFallback, trigger);
        } else {
            mainFallback = newer(mainFallback, trigger);
        }
    }

    private static void clearFallback(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offFallback = null;
        } else {
            mainFallback = null;
        }
    }

    private static Trigger getFallback(class_1268 hand) {
        return hand == class_1268.field_5810 ? offFallback : mainFallback;
    }

    private static Trigger newer(Trigger current, Trigger incoming) {
        if (incoming == null) {
            return current;
        }
        if (current == null) {
            return incoming;
        }
        return incoming.recordNano() >= current.recordNano() ? incoming : current;
    }

    private static void markRecentUse(class_1268 hand, long nanos) {
        if (hand == null || nanos <= 0) {
            return;
        }
        if (hand == class_1268.field_5810) {
            lastOffUseNano = nanos;
        } else {
            lastMainUseNano = nanos;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/UseActionTracker$Trigger.class */
    public static final class Trigger extends Record {
        private final class_1268 hand;
        private final TriggerType type;
        private final class_3965 blockHit;
        private final boolean fallback;
        private final long recordNano;

        public Trigger(class_1268 hand, TriggerType type, class_3965 blockHit, boolean fallback, long recordNano) {
            this.hand = hand;
            this.type = type;
            this.blockHit = blockHit;
            this.fallback = fallback;
            this.recordNano = recordNano;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, Trigger.class), Trigger.class, "hand;type;blockHit;fallback;recordNano", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->type:Lpunchy/client/state/UseActionTracker$TriggerType;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->blockHit:Lnet/minecraft/class_3965;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->fallback:Z", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->recordNano:J").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, Trigger.class), Trigger.class, "hand;type;blockHit;fallback;recordNano", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->type:Lpunchy/client/state/UseActionTracker$TriggerType;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->blockHit:Lnet/minecraft/class_3965;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->fallback:Z", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->recordNano:J").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, Trigger.class, Object.class), Trigger.class, "hand;type;blockHit;fallback;recordNano", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->type:Lpunchy/client/state/UseActionTracker$TriggerType;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->blockHit:Lnet/minecraft/class_3965;", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->fallback:Z", "FIELD:Lpunchy/client/state/UseActionTracker$Trigger;->recordNano:J").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public TriggerType type() {
            return this.type;
        }

        public class_3965 blockHit() {
            return this.blockHit;
        }

        public boolean fallback() {
            return this.fallback;
        }

        public long recordNano() {
            return this.recordNano;
        }
    }
}
