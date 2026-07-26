package fun.enoughvisuals.port;

public final class SpringSpec {
    private final float stiffness;
    private final float damping;
    private final float settleDistance;
    private final float settleVelocity;

    public SpringSpec(float stiffness, float damping, float settleDistance, float settleVelocity) {
        this.stiffness = stiffness;
        this.damping = damping;
        this.settleDistance = settleDistance;
        this.settleVelocity = settleVelocity;
    }

    public static SpringSpec resolve2() {
        return new SpringSpec(0.045F, 0.85F, 0.001F, 0.001F);
    }

    public float getFloatValue() { return stiffness; }
    public float getFloatValue2() { return damping; }
    public float getFloatValue3() { return settleDistance; }
    public float getFloatValue4() { return settleVelocity; }
}
