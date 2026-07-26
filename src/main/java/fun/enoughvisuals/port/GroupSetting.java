package fun.enoughvisuals.port;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class GroupSetting extends Setting {
    public final List<BooleanSetting> options;
    public boolean expanded;
    public final Animation animation = new Animation();
    public GroupSetting(String name, BooleanSetting... options) { this.name = name; this.options = Arrays.asList(options); }
    public boolean isEnabled(String name) { return options.stream().anyMatch(option -> option.name.equals(name) && option.isEnabled()); }
    public boolean isEnabled(int index) { return index >= 0 && index < options.size() && options.get(index).isEnabled(); }
    public GroupSetting visibleWhen(Supplier<Boolean> condition) { this.visibilityCondition = condition; return this; }
}
