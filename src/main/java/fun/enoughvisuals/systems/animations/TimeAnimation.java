package fun.enoughvisuals.systems.animations;

public final class TimeAnimation {
    private long startMs;
    private long durationMs;
    private float from;
    private float to;
    private Easing easing;

    public TimeAnimation(long durationMs, Easing easing) {
        this.durationMs = Math.max(1L, durationMs);
        this.easing = easing == null ? Easing.OUT_CUBIC : easing;
        this.startMs = System.currentTimeMillis();
        this.from = 0f;
        this.to = 1f;
    }

    public void play(float from, float to) {
        this.from = from;
        this.to = to;
        this.startMs = System.currentTimeMillis();
    }

    public void playTo(float target) {
        play(value(), target);
    }

    public void reset(float value) {
        this.from = value;
        this.to = value;
        this.startMs = System.currentTimeMillis();
    }

    public float value() {
        float t = progress();
        return from + (to - from) * easing.apply(t);
    }

    public float progress() {
        return Easing.clamp((System.currentTimeMillis() - startMs) / (float) durationMs);
    }

    public boolean finished() {
        return progress() >= 1f;
    }

    public TimeAnimation duration(long durationMs) {
        this.durationMs = Math.max(1L, durationMs);
        return this;
    }

    public TimeAnimation easing(Easing easing) {
        this.easing = easing == null ? Easing.OUT_CUBIC : easing;
        return this;
    }
}
