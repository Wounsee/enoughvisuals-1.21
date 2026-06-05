package fun.enoughvisuals.systems.modules.settings;

public class BezierSetting extends Setting<BezierSetting.BezierCurve> {

    public BezierSetting(String name, String id, String description,
                         float x1, float y1, float x2, float y2) {
        super(name, id, description, new BezierCurve(x1, y1, x2, y2));
    }

    public BezierSetting(String name, String id, float x1, float y1, float x2, float y2) {
        this(name, id, "", x1, y1, x2, y2);
    }

    
    public static BezierSetting easeInOut(String name, String id) {
        return new BezierSetting(name, id, 0.42f, 0f, 0.58f, 1f);
    }

    public static BezierSetting easeOut(String name, String id) {
        return new BezierSetting(name, id, 0f, 0f, 0.58f, 1f);
    }

    public static BezierSetting easeIn(String name, String id) {
        return new BezierSetting(name, id, 0.42f, 0f, 1f, 1f);
    }

    public static BezierSetting linear(String name, String id) {
        return new BezierSetting(name, id, 0f, 0f, 1f, 1f);
    }

    
    public float evaluate(float t) {
        return get().evaluate(t);
    }

    @Override
    public Object serialize() {
        BezierCurve c = get();
        return c.x1 + "," + c.y1 + "," + c.x2 + "," + c.y2;
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            String[] parts = s.split(",");
            if (parts.length == 4) {
                try {
                    set(new BezierCurve(
                            Float.parseFloat(parts[0]),
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                    ));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    
    
    

    public static class BezierCurve {
        public final float x1, y1, x2, y2;

        public BezierCurve(float x1, float y1, float x2, float y2) {
            this.x1 = clamp01(x1);
            this.y1 = y1;
            this.x2 = clamp01(x2);
            this.y2 = y2;
        }

        
        public float evaluate(float t) {
            t = clamp01(t);
            if (t <= 0) return 0;
            if (t >= 1) return 1;

            
            float lo = 0, hi = 1;
            for (int i = 0; i < 20; i++) {
                float mid = (lo + hi) / 2f;
                float bx = bezierComponent(mid, x1, x2);
                if (bx < t) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            float u = (lo + hi) / 2f;
            return bezierComponent(u, y1, y2);
        }

        private float bezierComponent(float t, float p1, float p2) {
            float mt = 1f - t;
            return 3f * mt * mt * t * p1 + 3f * mt * t * t * p2 + t * t * t;
        }

        private static float clamp01(float v) {
            return Math.max(0, Math.min(1, v));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BezierCurve c)) return false;
            return Float.compare(c.x1, x1) == 0 && Float.compare(c.y1, y1) == 0
                    && Float.compare(c.x2, x2) == 0 && Float.compare(c.y2, y2) == 0;
        }

        @Override
        public String toString() {
            return String.format("cubic-bezier(%.2f, %.2f, %.2f, %.2f)", x1, y1, x2, y2);
        }
    }
}