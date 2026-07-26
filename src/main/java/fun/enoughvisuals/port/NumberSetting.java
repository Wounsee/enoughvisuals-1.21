package fun.enoughvisuals.port;

import java.util.function.Supplier;

public class NumberSetting extends Setting {
    public float value;
    public final float minimum;
    public final float maximum;
    public final float step;

    public NumberSetting(String name, float value, float minimum, float maximum, float step, boolean percent) {
        this.name = name;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void invoke(float value) {
        this.value = Math.max(minimum, Math.min(maximum, value));
    }

    public NumberSetting setVisibilityCondition(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }
}
