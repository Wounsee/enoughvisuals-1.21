package punchy.client.render;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/HandRenderBobContext.class */
public final class HandRenderBobContext {
    private static boolean renderingHands;
    private static float lastWalkDist;
    private static float lastBob;
    private static boolean hasBobSample;

    private HandRenderBobContext() {
    }

    public static void begin() {
        renderingHands = true;
    }

    public static void end() {
        renderingHands = false;
    }

    public static boolean isRenderingHands() {
        return renderingHands;
    }

    public static void updateBobSample(float walkDist, float bob) {
        lastWalkDist = walkDist;
        lastBob = bob;
        hasBobSample = true;
    }

    public static boolean hasBobSample() {
        return hasBobSample;
    }

    public static float getLastWalkDist() {
        return lastWalkDist;
    }

    public static float getLastBob() {
        return lastBob;
    }
}
