package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.systems.animations.Easing;

public final class MainMenuAnimation {
    private static final long OPEN_MS = 500L;
    private static final long BUTTON_STAGGER_MS = 48L;
    private long openedAt = System.currentTimeMillis();

    public void restart() {
        openedAt = System.currentTimeMillis();
    }

    public void restartDelayed(long delayMs) {
        openedAt = System.currentTimeMillis() + Math.max(0L, delayMs);
    }

    public float openProgress() {
        float raw = (System.currentTimeMillis() - openedAt) / (float) OPEN_MS;
        return Easing.OUT_CUBIC.apply(raw);
    }

    public float alpha() {
        return openProgress();
    }

    public float slideOffset(float scale) {
        return 0.0f;
    }

    public float buttonProgress(int index) {
        long now = System.currentTimeMillis();
        float raw = (now - openedAt - Math.max(0, index) * BUTTON_STAGGER_MS) / (float) OPEN_MS;
        return Easing.OUT_CUBIC.apply(raw);
    }

    public float buttonAlpha(int index) {
        long now = System.currentTimeMillis();
        float raw = (now - openedAt - Math.max(0, index) * BUTTON_STAGGER_MS) / (float) (OPEN_MS * 0.62f);
        return Easing.OUT_CUBIC.apply(raw);
    }
}
