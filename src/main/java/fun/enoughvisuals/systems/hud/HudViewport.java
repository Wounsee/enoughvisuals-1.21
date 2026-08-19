package fun.enoughvisuals.systems.hud;

/**
 * Resolution-independent HUD placement helpers.
 *
 * <p>Positions are normalized against the available travel of an element
 * rather than against the raw screen size. That preserves exact left/right
 * and top/bottom anchoring without changing the element's visual size.</p>
 */
public final class HudViewport {

    private HudViewport() {
    }

    public static float normalizedPosition(float position, float elementSize, float viewportSize) {
        float travel = travel(elementSize, viewportSize);
        if (travel <= 0.001f || !Float.isFinite(position)) return 0f;
        return clamp01(position / travel);
    }

    public static float positionFromNormalized(float normalized, float elementSize, float viewportSize) {
        if (!Float.isFinite(normalized)) normalized = 0f;
        return travel(elementSize, viewportSize) * clamp01(normalized);
    }

    public static float remap(float position, float elementSize, float oldViewportSize, float newViewportSize) {
        return positionFromNormalized(
                normalizedPosition(position, elementSize, oldViewportSize),
                elementSize,
                newViewportSize
        );
    }

    private static float travel(float elementSize, float viewportSize) {
        if (!Float.isFinite(elementSize) || !Float.isFinite(viewportSize)) return 0f;
        return Math.max(0f, viewportSize - Math.max(0f, elementSize));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
