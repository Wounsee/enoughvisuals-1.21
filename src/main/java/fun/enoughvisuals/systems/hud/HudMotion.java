package fun.enoughvisuals.systems.hud;

/** Frame-rate independent springs used by the HUD layout engine. */
public final class HudMotion {
    private HudMotion() {
    }

    public static final class Spring {
        private float value;
        private float velocity;
        private boolean initialized;

        public Spring() {
        }

        public Spring(float initial) {
            set(initial);
        }

        public float step(float target, float frameDelta) {
            if (!initialized) set(target);
            float dt = Math.max(0.05f, Math.min(3f, frameDelta));
            float stiffness = 0.19f;
            float damping = (float) Math.pow(0.66f, dt);
            velocity += (target - value) * stiffness * dt;
            velocity *= damping;
            value += velocity * dt;
            if (Math.abs(target - value) < 0.001f && Math.abs(velocity) < 0.001f) {
                value = target;
                velocity = 0f;
            }
            return value;
        }

        public void set(float newValue) {
            value = newValue;
            velocity = 0f;
            initialized = true;
        }

        public float value() {
            return value;
        }
    }

    public static float damp(float current, float target, float response, float frameDelta) {
        float dt = Math.max(0f, Math.min(4f, frameDelta));
        float factor = 1f - (float) Math.pow(1f - Math.max(0.001f, Math.min(0.999f, response)), dt);
        float next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.0005f ? target : next;
    }
}
