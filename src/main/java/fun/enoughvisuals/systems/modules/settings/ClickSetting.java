package fun.enoughvisuals.systems.modules.settings;

public class ClickSetting extends Setting<Boolean> {
    private final Runnable action;

    public ClickSetting(String name, String id, String description, Runnable action) {
        super(name, id, description, false);
        this.action = action;
    }

    public void click() {
        if (action != null) {
            action.run();
        }
    }

    @Override
    public Object serialize() {
        return false;
    }

    @Override
    public void deserialize(Object data) {
    }
}
