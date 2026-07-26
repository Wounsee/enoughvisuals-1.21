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

    public final BooleanSetting watermark = addWatermarkSetting();
    public final BooleanSetting targetHud = addElementSetting("Target HUD", "targethud", false);
    public final BooleanSetting effects = addElementSetting("Effects", "effects", false);
    public final BooleanSetting binds = addElementSetting("Binds", "binds", false);
    public final BooleanSetting cooldowns = addElementSetting("Cooldowns", "cooldowns", false);
    public final BooleanSetting inventoryHud = addElementSetting("Inventory HUD", "inventory_hud", false);
    public final BooleanSetting trapTimer = addElementSetting("Таймер Трапки", "trap_timer", true);

    private BooleanSetting addWatermarkSetting() {
        BooleanSetting setting = add(new BooleanSetting("Dynamic Island", "watermark",
                "Shows the Dynamic Island watermark", true));
        setting.onChange((oldValue, newValue) -> {
            if (!syncing) setElementEnabled("watermark", newValue);
        });
        return setting;
    }

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
        syncing = true;
        try {
            setElementEnabled("watermark", watermark.get());
            setElementEnabled("targethud", targetHud.get());
            setElementEnabled("effects", effects.get());
            setElementEnabled("binds", binds.get());
            setElementEnabled("cooldowns", cooldowns.get());
            setElementEnabled("inventory_hud", inventoryHud.get());
            setElementEnabled("trap_timer", trapTimer.get());
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
            removeIfActive("trap_timer");
            setElementEnabled("watermark", watermark.get());
        } finally {
            syncing = false;
        }
    }

    @Override
    public void onTick() {
        syncManualDeletion(watermark, "watermark");
        syncManualDeletion(targetHud, "targethud");
        syncManualDeletion(effects, "effects");
        syncManualDeletion(binds, "binds");
        syncManualDeletion(cooldowns, "cooldowns");
        syncManualDeletion(inventoryHud, "inventory_hud");
        syncManualDeletion(trapTimer, "trap_timer");
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

    public void syncWatermarkVisibility() {
        setElementEnabled("watermark", watermark.get());
    }

    private void addIfMissing(String id) {
        if (!hud.isElementActive(id)) {
            hud.addElement(id, 8f, 8f);
        }
    }

    private void removeIfActive(String id) {
        if ("watermark".equals(id)) {
            hud.removeElementForced(hud.getActiveElementByTypeId(id));
        } else {
            hud.removeElement(hud.getActiveElementByTypeId(id));
        }
    }
}
