package fun.enoughvisuals.systems.hud;

/** Horizontal blocks per real second, sampled independently of rendering FPS. */
public final class HudSpeedMeter {
    private boolean initialized;
    private long sampledAt;
    private double lastX;
    private double lastZ;
    private float speed;

    public float sample(double x, double z, long now) {
        if (!initialized) {
            initialized = true;
            sampledAt = now;
            lastX = x;
            lastZ = z;
            return speed = 0;
        }
        long elapsed = now - sampledAt;
        if (elapsed >= 100_000_000L) {
            double measured = Math.hypot(x - lastX, z - lastZ) * 1_000_000_000.0 / elapsed;
            // Text handles smoothing. Smoothing this sample with a render-frame
            // delta made the value lag badly, especially at high FPS.
            speed = Double.isFinite(measured) && measured >= 0.05 ? (float) measured : 0;
            lastX = x;
            lastZ = z;
            sampledAt = now;
        }
        return speed;
    }

    public void reset() {
        initialized = false;
        speed = 0;
    }
}
