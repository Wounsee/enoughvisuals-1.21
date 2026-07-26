package fun.enoughvisuals.port.api;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import fun.enoughvisuals.port.Setting;

/** Compatibility shell. Rendering code is kept in the original Ported classes. */
public class Module {
    public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    public boolean enabled;
    private final List<Setting> settings = new ArrayList<>();

    protected final void addSettings(Setting[] values) {
        if (values != null) {
            for (Setting value : values) {
                if (value != null) settings.add(value);
            }
        }
    }

    public List<Setting> getAllSettings() {
        return settings;
    }

    public void onEnable() {
        enabled = true;
    }

    public void onDisable() {
        enabled = false;
    }

    public void toggle() {
        if (enabled) onDisable(); else onEnable();
    }

    public void loadFromJson(JsonObject jsonObject) {
    }
}
