package fun.enoughvisuals.systems.modules.settings;

import fun.enoughvisuals.utils.system.ClientLanguage;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String id, String description,
                       String defaultMode, String... modes) {
        super(name, id, description, defaultMode);
        this.modes = Arrays.asList(modes);
        if (!this.modes.contains(defaultMode)) {
            throw new IllegalArgumentException(
                    "Default mode '" + defaultMode + "' not in modes list: " + this.modes
            );
        }
    }

    @Override
    protected String validate(String value) {
        return modes.contains(value) ? value : getDefaultValue();
    }

    
    public void cycle() {
        int idx = modes.indexOf(get());
        set(modes.get((idx + 1) % modes.size()));
    }

    
    public void cycleBack() {
        int idx = modes.indexOf(get());
        set(modes.get((idx - 1 + modes.size()) % modes.size()));
    }

    public int getIndex() {
        return modes.indexOf(get());
    }

    public void setByIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            set(modes.get(index));
        }
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    public List<String> getModes() {
        return modes;
    }

    public String getDisplayValue() {
        return getDisplayMode(get());
    }

    public String getDisplayMode(String mode) {
        return ClientLanguage.modeValue(getOwnerModuleId(), getId(), mode);
    }

    @Override
    public Object serialize() {
        return get();
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            set(s);
        }
    }
}
