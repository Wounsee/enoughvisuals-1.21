package fun.enoughvisuals.systems.animations;

public final class FadeAnimation {
    private final AnimatedFloat alpha;

    public FadeAnimation(long durationMs) {
        this.alpha = new AnimatedFloat(0f, durationMs, Easing.OUT_CUBIC);
    }

    public void show() {
        alpha.setTarget(1f);
    }

    public void hide() {
        alpha.setTarget(0f);
    }

    public float alpha() {
        return alpha.value();
    }
}
