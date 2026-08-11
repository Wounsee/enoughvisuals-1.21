package fun.enoughvisuals.systems.hud;

/**
 * Animated render-time space reserved at the bottom of the HUD while chat is
 * open. The value never changes an element's persisted coordinates.
 */
public final class HudBottomInset {

    static final float CHAT_HEIGHT = 20f;
    private static final float OPEN_SPEED = 0.20f;
    private static final float CLOSE_SPEED = 0.16f;
    private static final float FULL_INSET_BOTTOM_GAP = 32f;
    private static final float NO_INSET_BOTTOM_GAP = 96f;

    private static float current;

    private HudBottomInset() {
    }

    /** Advances the shared inset once at the start of an HUD frame. */
    public static void beginFrame(boolean chatOpen, float frameDelta) {
        float target = chatOpen ? CHAT_HEIGHT : 0f;
        current = advance(current, target, frameDelta,
                chatOpen ? OPEN_SPEED : CLOSE_SPEED);
    }

    /** Current positive distance reserved above the bottom screen edge. */
    public static float current() {
        return current;
    }

    /**
     * Returns the positive amount by which these bounds should be rendered
     * upward. Elements in the upper part of the screen remain untouched; the
     * influence ramps smoothly to full strength toward the bottom edge.
     */
    public static float offsetFor(float y, float height, float screenHeight) {
        return offsetFor(y, height, screenHeight, current);
    }

    /** Converts a desired visual Y back to its persistent layout Y. */
    public static float layoutYForVisual(float visualY, float height, float screenHeight) {
        float layoutY = visualY;
        // offsetFor is a shallow, continuous function, so this fixed-point
        // inversion converges quickly and keeps dragging under the cursor.
        for (int i = 0; i < 6; i++) {
            layoutY = visualY + offsetFor(layoutY, height, screenHeight, current);
        }
        return layoutY;
    }

    static float offsetFor(float y, float height, float screenHeight, float inset) {
        if (!Float.isFinite(y) || !Float.isFinite(height)
                || !Float.isFinite(screenHeight) || !Float.isFinite(inset)
                || screenHeight <= 0f || inset <= 0f) {
            return 0f;
        }

        float bottomGap = screenHeight - (y + Math.max(0f, height));
        float falloff = clamp01((bottomGap - FULL_INSET_BOTTOM_GAP)
                / (NO_INSET_BOTTOM_GAP - FULL_INSET_BOTTOM_GAP));
        falloff = falloff * falloff * (3f - 2f * falloff);
        float influence = 1f - falloff;
        return Math.max(0f, inset) * influence;
    }

    static float advance(float value, float target, float frameDelta, float speed) {
        float safeTarget = Math.max(0f, target);
        if (!Float.isFinite(value)) value = safeTarget;
        if (!Float.isFinite(frameDelta) || frameDelta <= 0f) return Math.max(0f, value);
        float amount = Math.min(1f, Math.max(0f, speed) * frameDelta);
        float next = value + (safeTarget - value) * amount;
        if (Math.abs(next - safeTarget) < 0.01f) return safeTarget;
        return Math.max(0f, next);
    }

    static void resetForTest() {
        current = 0f;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
