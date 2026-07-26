package fun.enoughvisuals.port;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting {
    public final List<String> options;
    public String value;

    public ModeSetting(String name, String value, String... options) {
        this.name = name;
        this.options = Arrays.asList(options);
        this.value = this.options.contains(value) ? value : this.options.getFirst();
    }

    public String getValue() {
        return value;
    }

    public boolean is(String value) {
        return this.value.equalsIgnoreCase(value);
    }

    public ModeSetting setVisibilityCondition(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }
}
