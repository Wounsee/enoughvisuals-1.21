package fun.enoughvisuals.systems.modules.settings;

public class RangeSetting extends Setting<RangeSetting.Range> {

    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public RangeSetting(String name, String id, String description,
                        double defaultMin, double defaultMax,
                        double min, double max, double step, String suffix) {
        super(name, id, description, new Range(defaultMin, defaultMax));
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public RangeSetting(String name, String id,
                        double defaultMin, double defaultMax,
                        double min, double max, double step) {
        this(name, id, "", defaultMin, defaultMax, min, max, step, "");
    }

    @Override
    protected Range validate(Range value) {
        double lo = Math.max(min, Math.min(max, value.low));
        double hi = Math.max(min, Math.min(max, value.high));
        if (step > 0) {
            lo = Math.round(lo / step) * step;
            hi = Math.round(hi / step) * step;
        }
        if (lo > hi) {
            double tmp = lo;
            lo = hi;
            hi = tmp;
        }
        return new Range(lo, hi);
    }

    public double getLow() { return get().low; }
    public double getHigh() { return get().high; }

    public void setLow(double low) {
        set(new Range(low, get().high));
    }

    public void setHigh(double high) {
        set(new Range(get().low, high));
    }

    public float getLowProgress() {
        if (max == min) return 0;
        return (float) ((getLow() - min) / (max - min));
    }

    public float getHighProgress() {
        if (max == min) return 0;
        return (float) ((getHigh() - min) / (max - min));
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public String getSuffix() { return suffix; }

    @Override
    public Object serialize() {
        return getLow() + ":" + getHigh();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            String[] parts = s.split(":");
            if (parts.length == 2) {
                try {
                    set(new Range(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public static class Range {
        public final double low;
        public final double high;

        public Range(double low, double high) {
            this.low = low;
            this.high = high;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Range r)) return false;
            return Double.compare(r.low, low) == 0 && Double.compare(r.high, high) == 0;
        }

        @Override
        public String toString() {
            return low + " - " + high;
        }
    }
}