package punchy.client.state;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.class_1268;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableActionTracker.class */
public final class TriggerableActionTracker {
    private static boolean triggered;
    private static class_1268 triggerHand;
    private static int triggerKeyCode = -1;

    private TriggerableActionTracker() {
    }

    public static void record(class_1268 hand, int keyCode) {
        triggered = true;
        triggerHand = hand;
        triggerKeyCode = keyCode;
    }

    public static Trigger consumeTrigger() {
        if (!triggered) {
            return null;
        }
        triggered = false;
        return new Trigger(triggerHand, triggerKeyCode);
    }

    public static void clear() {
        triggered = false;
        triggerHand = null;
        triggerKeyCode = -1;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/TriggerableActionTracker$Trigger.class */
    public static final class Trigger extends Record {
        private final class_1268 hand;
        private final int keyCode;

        public Trigger(class_1268 hand, int keyCode) {
            this.hand = hand;
            this.keyCode = keyCode;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, Trigger.class), Trigger.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->keyCode:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, Trigger.class), Trigger.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->keyCode:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, Trigger.class, Object.class), Trigger.class, "hand;keyCode", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/state/TriggerableActionTracker$Trigger;->keyCode:I").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public int keyCode() {
            return this.keyCode;
        }
    }
}
