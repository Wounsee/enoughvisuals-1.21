package fun.enoughvisuals.port;

import java.util.function.Supplier;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    public boolean isEnabled() {
        return value;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public BooleanSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }
}
