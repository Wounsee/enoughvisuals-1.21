package punchy.client.state;

import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_465;
import net.minecraft.class_481;
import net.minecraft.class_490;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/InterfaceDelayState.class */
public final class InterfaceDelayState {
    private static boolean pending;
    private static long delayUntilNanos;
    private static class_437 queuedScreen;
    private static boolean bypass;

    private InterfaceDelayState() {
    }

    public static void schedule(float seconds) {
        if (seconds <= 0.0f) {
            return;
        }
        pending = true;
        queuedScreen = null;
        delayUntilNanos = System.nanoTime() + ((long) (seconds * 1.0E9f));
    }

    public static boolean shouldDelay(class_437 screen) {
        if (bypass || !pending || screen == null || !isDelayableScreen(screen)) {
            return false;
        }
        if (System.nanoTime() >= delayUntilNanos) {
            pending = false;
            return false;
        }
        return true;
    }

    public static void queue(class_437 screen) {
        queuedScreen = screen;
    }

    public static void tick(class_310 client) {
        if (!pending) {
            return;
        }
        if (queuedScreen == null) {
            if (System.nanoTime() >= delayUntilNanos) {
                pending = false;
            }
        } else {
            if (System.nanoTime() < delayUntilNanos) {
                return;
            }
            class_437 screen = queuedScreen;
            queuedScreen = null;
            pending = false;
            openNow(client, screen);
        }
    }

    public static void openNow(class_310 client, class_437 screen) {
        if (client == null) {
            return;
        }
        bypass = true;
        client.method_1507(screen);
        bypass = false;
    }

    private static boolean isDelayableScreen(class_437 screen) {
        if ((screen instanceof class_490) || (screen instanceof class_481)) {
            return false;
        }
        return screen instanceof class_465;
    }
}
