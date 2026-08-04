package punchy.client.state;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/InspectActionTracker.class */
public final class InspectActionTracker {
    private static boolean triggered;

    private InspectActionTracker() {
    }

    public static void record() {
        triggered = true;
    }

    public static boolean consume() {
        if (!triggered) {
            return false;
        }
        triggered = false;
        return true;
    }

    public static void clear() {
        triggered = false;
    }
}
