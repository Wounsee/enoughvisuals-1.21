package fun.enoughvisuals.systems.modules.settings;

public class SliderSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public SliderSetting(String name, String id, String description,
                         double defaultValue, double min, double max, double step, String suffix) {
        super(name, id, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public SliderSetting(String name, String id, String description,
                         double defaultValue, double min, double max, double step) {
        this(name, id, description, defaultValue, min, max, step, "");
    }

    public SliderSetting(String name, String id, double defaultValue, double min, double max, double step) {
        this(name, id, "", defaultValue, min, max, step, "");
    }

    @Override
    protected Double validate(Double value) {
        
        value = Math.max(min, Math.min(max, value));
        
        if (step > 0) {
            value = Math.round(value / step) * step;
        }
        return value;
    }

    
    public float getProgress() {
        if (max == min) return 0;
        return (float) ((get() - min) / (max - min));
    }

    
    public void setFromProgress(float progress) {
        progress = Math.max(0, Math.min(1, progress));
        set(min + (max - min) * progress);
    }

    
    public String getDisplayValue() {
        if (step >= 1.0) {
            return String.valueOf((int) get().doubleValue()) + suffix;
        } else if (step >= 0.1) {
            return String.format("%.1f%s", get(), suffix);
        } else {
            return String.format("%.2f%s", get(), suffix);
        }
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public String getSuffix() { return suffix; }

    @Override
    public Object serialize() {
        return get();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof Number n) {
            set(n.doubleValue());
        } else if (data instanceof String s) {
            try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) {}
        }
    }
}