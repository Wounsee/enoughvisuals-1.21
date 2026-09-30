package fun.enoughvisuals.systems.modules;

import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.utils.system.ClientLanguage;
import fun.enoughvisuals.utils.system.ModuleToggleFeed;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String id;
    private final String description;
    private final Category category;

    private boolean enabled = false;
    private int keyBind = -1;
    private float enableAnimation = 0f;

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module() {
        Register reg = getClass().getAnnotation(Register.class);
        if (reg == null) {
            throw new RuntimeException(
                    "Module " + getClass().getSimpleName() + " missing @Register!"
            );
        }
        this.name = reg.name();
        this.id = reg.id();
        this.description = reg.description();
        this.category = reg.category();
    }

    protected <T extends Setting<?>> T add(T setting) {
        setting.setOwnerModuleId(id);
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public List<Setting<?>> getVisibleSettings() {
        List<Setting<?>> visible = new ArrayList<>();
        for (Setting<?> s : settings) {
            if (s.isVisible()) visible.add(s);
        }
        return visible;
    }

    public Setting<?> getSettingById(String id) {
        for (Setting<?> s : settings) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public void toggle() {
        if (ModuleManager.getInstance().isBlocked(this)) return;
        if (enabled) disable(); else enable();
    }

    public void enable() {
        enable(false);
    }

    public void enableSilent() {
        enable(true);
    }

    private void enable(boolean silent) {
        if (ModuleManager.getInstance().isBlocked(this)) return;
        if (enabled) return;
        if (!canEnable()) return;
        enabled = true;
        if (!silent) {
            EnoughSoundPlayer.playModuleToggle(true);
            ModuleToggleFeed.push(name, true);
        }
        onEnable();
    }

    public void disable() {
        disable(false);
    }

    public void disableSilent() {
        disable(true);
    }

    private void disable(boolean silent) {
        if (!enabled) return;
        enabled = false;
        if (!silent) {
            EnoughSoundPlayer.playModuleToggle(false);
            ModuleToggleFeed.push(name, false);
        }
        onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    protected boolean canEnable() { return true; }
    public void onTick() {}
    public void onRender(float tickDelta) {}
    public void onKeyPress(int key) {}

    public String getName() { return ClientLanguage.moduleName(id, name); }
    public String getRawName() { return name; }
    public String getId() { return id; }
    public String getDescription() { return ClientLanguage.moduleDescription(id, description); }
    public String getRawDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public boolean isEnabledByDefault() {
        Register reg = getClass().getAnnotation(Register.class);
        return reg != null && reg.enabledByDefault();
    }
    public boolean isAvailable() { return !ModuleManager.getInstance().isBlocked(this) && (enabled || canEnable()); }
    public void setEnabled(boolean e) { if (e) enable(); else disable(); }
    public void setEnabledSilent(boolean e) { if (e) enableSilent(); else disableSilent(); }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int k) { this.keyBind = k; }
    public float getEnableAnimation() { return enableAnimation; }
    public void setEnableAnimation(float a) { this.enableAnimation = a; }

    protected boolean isInGame() {
        return mc.player != null && mc.world != null;
    }

    @Override
    public String toString() {
        return name + " [" + category.getDisplayName() + "] settings=" + settings.size();
    }
}
