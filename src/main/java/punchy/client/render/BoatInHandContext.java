package punchy.client.render;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/BoatInHandContext.class */
public final class BoatInHandContext {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> {
        return Boolean.FALSE;
    });

    private BoatInHandContext() {
    }

    public static void push() {
        ACTIVE.set(Boolean.TRUE);
    }

    public static void pop() {
        ACTIVE.set(Boolean.FALSE);
    }

    public static boolean isActive() {
        return ACTIVE.get().booleanValue();
    }
}
