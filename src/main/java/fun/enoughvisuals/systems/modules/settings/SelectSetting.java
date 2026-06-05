package fun.enoughvisuals.systems.modules.settings;

import java.util.*;

public class SelectSetting extends Setting<Set<String>> {

    private final List<String> options;

    public SelectSetting(String name, String id, String description,
                         Set<String> defaultSelected, String... options) {
        super(name, id, description, new LinkedHashSet<>(defaultSelected));
        this.options = Arrays.asList(options);
    }

    public SelectSetting(String name, String id,
                         Set<String> defaultSelected, String... options) {
        this(name, id, "", defaultSelected, options);
    }

    @Override
    protected Set<String> validate(Set<String> value) {
        Set<String> valid = new LinkedHashSet<>();
        for (String s : value) {
            if (options.contains(s)) {
                valid.add(s);
            }
        }
        return valid;
    }

    public void toggle(String option) {
        Set<String> copy = new LinkedHashSet<>(get());
        if (copy.contains(option)) {
            copy.remove(option);
        } else {
            copy.add(option);
        }
        set(copy);
    }

    public boolean isSelected(String option) {
        return get().contains(option);
    }

    public List<String> getOptions() {
        return options;
    }

    @Override
    public Object serialize() {
        return String.join(",", get());
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            if (s.isEmpty()) {
                set(new LinkedHashSet<>());
            } else {
                set(new LinkedHashSet<>(Arrays.asList(s.split(","))));
            }
        }
    }
}