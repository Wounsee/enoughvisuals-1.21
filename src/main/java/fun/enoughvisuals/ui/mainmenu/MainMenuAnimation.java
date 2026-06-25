package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.utils.render.RenderUtil;

public final class MainMenuAnimation {
    private static final long OPEN_MS = 260L;

    private final long openedAt = System.currentTimeMillis();

    public float openProgress() {
        float raw = (System.currentTimeMillis() - openedAt) / (float) OPEN_MS;
        return RenderUtil.easeOutCubic(clamp(raw));
    }

    public float alpha() {
        return openProgress();
    }

    public float slideOffset(float scale) {
        return (1.0f - openProgress()) * 12.0f * scale;
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
