package fun.enoughvisuals.systems.animations;

public final class AnimatedFloat {
    private float current;
    private float target;
    private float from;
    private long startMs;
    private long durationMs;
    private Easing easing;

    public AnimatedFloat(float initial, long durationMs, Easing easing) {
        this.current = initial;
        this.target = initial;
        this.from = initial;
        this.durationMs = Math.max(1L, durationMs);
        this.easing = easing == null ? Easing.OUT_CUBIC : easing;
        this.startMs = System.currentTimeMillis();
    }

    public void setTarget(float target) {
        if (Math.abs(this.target - target) < 0.0001f) return;
        this.from = value();
        this.target = target;
        this.startMs = System.currentTimeMillis();
    }

    public float value() {
        float t = Easing.clamp((System.currentTimeMillis() - startMs) / (float) durationMs);
        current = from + (target - from) * easing.apply(t);
        return current;
    }

    public void snap(float value) {
        this.current = value;
        this.from = value;
        this.target = value;
        this.startMs = System.currentTimeMillis();
    }
}
