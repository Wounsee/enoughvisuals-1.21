package fun.enoughvisuals.systems.modules.settings;

import fun.enoughvisuals.utils.system.ClientLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {

    private final String name;
    private final String id;
    private final String description;
    private T value;
    private final T defaultValue;
    private String ownerModuleId = "";

    
    private BooleanSupplier visibility = () -> true;

    
    private final List<SettingChangeListener<T>> listeners = new ArrayList<>();

    public Setting(String name, String id, String description, T defaultValue) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    
    
    

    public T get() {
        return value;
    }

    public void set(T value) {
        T old = this.value;
        this.value = validate(value);
        if (!this.value.equals(old)) {
            for (SettingChangeListener<T> listener : listeners) {
                listener.onChange(old, this.value);
            }
        }
    }

    
    protected T validate(T value) {
        return value;
    }

    public void reset() {
        set(defaultValue);
    }

    
    
    

    public String getName() { return ClientLanguage.settingName(ownerModuleId, id, name); }
    public String getRawName() { return name; }
    public String getId() { return id; }
    public String getDescription() { return ClientLanguage.settingDescription(ownerModuleId, id, description); }
    public String getRawDescription() { return description; }
    public String getOwnerModuleId() { return ownerModuleId; }
    public void setOwnerModuleId(String ownerModuleId) { this.ownerModuleId = ownerModuleId == null ? "" : ownerModuleId; }
    public T getDefaultValue() { return defaultValue; }

    
    
    

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public Setting<T> visibleWhen(BooleanSupplier condition) {
        this.visibility = condition;
        return this;
    }

    
    
    

    public Setting<T> onChange(SettingChangeListener<T> listener) {
        listeners.add(listener);
        return this;
    }

    @FunctionalInterface
    public interface SettingChangeListener<T> {
        void onChange(T oldValue, T newValue);
    }

    
    
    

    public abstract Object serialize();
    public abstract void deserialize(Object data);

    @Override
    public String toString() {
        return name + " = " + value;
    }
}
