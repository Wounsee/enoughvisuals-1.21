package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;

@Register(name = "HUD", id = "hud", description = "Controls HUD elements from module settings", category = Category.CLIENT)
public class HUD extends Module {
    private final DragAndDrop hud = DragAndDrop.getInstance();
    private boolean syncing;

    public final BooleanSetting targetHud = addElementSetting("Target HUD", "targethud", false);
    public final BooleanSetting effects = addElementSetting("Effects", "effects", false);
    public final BooleanSetting binds = addElementSetting("Binds", "binds", false);
    public final BooleanSetting cooldowns = addElementSetting("Cooldowns", "cooldowns", false);
    public final BooleanSetting inventoryHud = addElementSetting("Inventory HUD", "inventory_hud", false);

    private BooleanSetting addElementSetting(String name, String id, boolean defaultValue) {
        BooleanSetting setting = add(new BooleanSetting(name, id, "Shows the HUD element", defaultValue));
        setting.onChange((oldValue, newValue) -> {
            if (syncing || !isEnabled()) return;
            setElementEnabled(id, newValue);
        });
        return setting;
    }

    @Override
    protected void onEnable() {
        ensureWatermark();
        syncing = true;
        try {
            setElementEnabled("targethud", targetHud.get());
            setElementEnabled("effects", effects.get());
            setElementEnabled("binds", binds.get());
            setElementEnabled("cooldowns", cooldowns.get());
            setElementEnabled("inventory_hud", inventoryHud.get());
        } finally {
            syncing = false;
        }
    }

    @Override
    protected void onDisable() {
        syncing = true;
        try {
            removeIfActive("targethud");
            removeIfActive("effects");
            removeIfActive("binds");
            removeIfActive("cooldowns");
            removeIfActive("inventory_hud");
            ensureWatermark();
        } finally {
            syncing = false;
        }
    }

    @Override
    public void onTick() {
        ensureWatermark();
        syncManualDeletion(targetHud, "targethud");
        syncManualDeletion(effects, "effects");
        syncManualDeletion(binds, "binds");
        syncManualDeletion(cooldowns, "cooldowns");
        syncManualDeletion(inventoryHud, "inventory_hud");
    }

    private void syncManualDeletion(BooleanSetting setting, String id) {
        if (syncing) return;
        boolean active = hud.isElementActive(id);
        if (setting.get() && !active) {
            syncing = true;
            try {
                setting.set(false);
            } finally {
                syncing = false;
            }
        } else if (!setting.get() && active) {
            removeIfActive(id);
        }
    }

    private void setElementEnabled(String id, boolean enabled) {
        if (enabled) {
            addIfMissing(id);
        } else {
            removeIfActive(id);
        }
    }

    private void ensureWatermark() {
        addIfMissing("watermark");
    }

    private void addIfMissing(String id) {
        if (!hud.isElementActive(id)) {
            hud.addElement(id, 8f, 8f);
        }
    }

    private void removeIfActive(String id) {
        hud.removeElement(hud.getActiveElementByTypeId(id));
    }
}
