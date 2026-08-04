package punchy.client.state;

import net.minecraft.class_1268;
import net.minecraft.class_1294;
import net.minecraft.class_310;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/AttackActionTracker.class */
public final class AttackActionTracker {
    private static boolean triggered;
    private static class_1268 lastHand = class_1268.field_5808;
    private static boolean lastCritical;
    private static boolean pendingCritical;
    private static long triggerEpoch;

    private AttackActionTracker() {
    }

    public static void recordCriticalCandidate(class_310 client) {
        if (client == null || client.field_1724 == null) {
            pendingCritical = false;
        } else {
            pendingCritical = isCriticalHit(client);
        }
    }

    public static void clearPendingCritical() {
        pendingCritical = false;
    }

    public static void recordAttack() {
        boolean wasTriggered = triggered;
        triggered = true;
        if (!wasTriggered) {
            triggerEpoch++;
            lastHand = class_1268.field_5808;
            lastCritical = pendingCritical;
            pendingCritical = false;
        }
    }

    public static void recordAttack(class_1268 hand) {
        boolean wasTriggered = triggered;
        triggered = true;
        if (hand != null) {
            lastHand = hand;
        }
        if (!wasTriggered) {
            triggerEpoch++;
            lastCritical = pendingCritical;
            pendingCritical = false;
        }
    }

    public static boolean isTriggered() {
        return triggered;
    }

    public static boolean consumeTrigger() {
        if (!triggered) {
            return false;
        }
        triggered = false;
        return true;
    }

    public static class_1268 peekHand() {
        if (!triggered) {
            return null;
        }
        return lastHand;
    }

    public static long peekEpoch() {
        return triggerEpoch;
    }

    public static class_1268 peekLastRecordedHand() {
        return lastHand;
    }

    public static class_1268 consumeHand() {
        if (!triggered) {
            return null;
        }
        triggered = false;
        return lastHand;
    }

    public static boolean consumeCritical() {
        boolean critical = lastCritical;
        lastCritical = false;
        return critical;
    }

    public static void clear() {
        triggered = false;
        lastHand = class_1268.field_5808;
        lastCritical = false;
        pendingCritical = false;
    }

    private static boolean isCriticalHit(class_310 client) {
        return (client == null || client.field_1724 == null || client.field_1724.method_7261(1.0f) < 0.9f || client.field_1724.method_24828() || client.field_1724.field_6017 <= 0.0d || client.field_1724.method_5799() || client.field_1724.method_5771() || client.field_1724.method_6101() || client.field_1724.method_5765() || client.field_1724.method_5624() || client.field_1724.method_6059(class_1294.field_5919)) ? false : true;
    }
}
