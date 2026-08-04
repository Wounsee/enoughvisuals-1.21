package punchy.client.animation;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/Easing.class */
public final class Easing {
    private static final float C1 = 1.9f;
    private static final float C3 = 2.9f;

    private Easing() {
    }

    public static float easeInBack(float t) {
        return (((C3 * t) * t) * t) - ((C1 * t) * t);
    }

    public static float easeOutBack(float t) {
        float x = t - 1.0f;
        return 1.0f + (C3 * x * x * x) + (C1 * x * x);
    }

    public static float easeOutBackStrong(float t) {
        float strongC3 = 3.61f + 1.0f;
        float x = t - 1.0f;
        return 1.0f + (strongC3 * x * x * x) + (3.61f * x * x);
    }

    public static float smoothStep(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        return clamped * clamped * (3.0f - (2.0f * clamped));
    }
}
