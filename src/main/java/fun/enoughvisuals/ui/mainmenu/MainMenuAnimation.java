package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.systems.animations.Easing;

/**
 * Intro timeline of the main menu: the header rises in first, then every
 * element slides up and fades in one after another (staggered ease-out-cubic,
 * the RivalVisuals title screen rhythm).
 */
public final class MainMenuAnimation {
    private static final long HEADER_DELAY_MS = 80L;
    private static final long HEADER_MS = 760L;
    private static final long ITEM_DELAY_MS = 360L;
    private static final long ITEM_STAGGER_MS = 58L;
    private static final long ITEM_MS = 520L;
    /** Rise distance in layout units before scaling. */
    public static final float HEADER_RISE = 18.0f;
    public static final float ITEM_RISE = 12.0f;

    private long openedAt = System.currentTimeMillis();

    public void restart() {
        openedAt = System.currentTimeMillis();
    }

    public void restartDelayed(long delayMs) {
        openedAt = System.currentTimeMillis() + Math.max(0L, delayMs);
    }

    /** Progress of the greeting header. */
    public float openProgress() {
        return progress(HEADER_DELAY_MS, HEADER_MS);
    }

    public float alpha() {
        return openProgress();
    }

    public float slideOffset(float scale) {
        return 0.0f;
    }

    /** Progress of the n-th element: menu buttons first, then the bottom row. */
    public float buttonProgress(int index) {
        return progress(ITEM_DELAY_MS + Math.max(0, index) * ITEM_STAGGER_MS, ITEM_MS);
    }

    public float buttonAlpha(int index) {
        return buttonProgress(index);
    }

    public static float rise(float progress, float distance, float scale) {
        return (1.0f - progress) * distance * scale;
    }

    private float progress(long delayMs, long durationMs) {
        long elapsed = System.currentTimeMillis() - openedAt - delayMs;
        if (elapsed <= 0L) return 0.0f;
        return Easing.OUT_CUBIC.apply(Math.min(1.0f, elapsed / (float) durationMs));
    }
}
