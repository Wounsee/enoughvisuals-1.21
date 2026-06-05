package fun.enoughvisuals.rt.guard;

public final class QGuard {

    private QGuard() {
    }

    public static native boolean isDebuggerPresent();

    public static native boolean hasSuspiciousModules();

    public static native boolean quickCheck();

    public static native int heartbeat(int seed);

    public static native void startWatchdog();

    public static native boolean timingCheck();

    public static native int requestChallenge();

    public static native boolean verifyResponse(int challenge, int response);
}
