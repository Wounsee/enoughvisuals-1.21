package fun.enoughvisuals.systems.animations;

public enum Easing {
    LINEAR {
        @Override
        public float apply(float t) {
            return clamp(t);
        }
    },
    IN_QUAD {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return t * t;
        }
    },
    OUT_QUAD {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return 1f - (1f - t) * (1f - t);
        }
    },
    IN_OUT_QUAD {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2f) * 0.5f;
        }
    },
    IN_CUBIC {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return t * t * t;
        }
    },
    OUT_CUBIC {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return 1f - (float) Math.pow(1f - t, 3f);
        }
    },
    IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) * 0.5f;
        }
    },
    OUT_QUART {
        @Override
        public float apply(float t) {
            t = clamp(t);
            return 1f - (float) Math.pow(1f - t, 4f);
        }
    },
    OUT_BACK {
        @Override
        public float apply(float t) {
            t = clamp(t);
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            return 1f + c3 * (float) Math.pow(t - 1f, 3f) + c1 * (float) Math.pow(t - 1f, 2f);
        }
    },
    IN_BACK {
        @Override
        public float apply(float t) {
            t = clamp(t);
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            return c3 * t * t * t - c1 * t * t;
        }
    },
    OUT_ELASTIC {
        @Override
        public float apply(float t) {
            t = clamp(t);
            if (t <= 0f || t >= 1f) return t;
            float c4 = (float) ((2.0 * Math.PI) / 3.0);
            return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
        }
    };

    public abstract float apply(float t);

    public static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
