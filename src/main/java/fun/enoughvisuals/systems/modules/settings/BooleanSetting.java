package fun.enoughvisuals.systems.modules.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String id, String description, boolean defaultValue) {
        super(name, id, description, defaultValue);
    }

    public BooleanSetting(String name, String id, boolean defaultValue) {
        this(name, id, "", defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public Object serialize() {
        return get();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof Boolean b) {
            set(b);
        } else if (data instanceof String s) {
            set(Boolean.parseBoolean(s));
        }
    }
}