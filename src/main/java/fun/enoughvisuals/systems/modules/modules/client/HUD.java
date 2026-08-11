package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.ui.hud.HotbarHud;

@Register(name = "HUD", id = "hud", description = "Управляет элементами нового HUD", category = Category.CLIENT)
public final class HUD extends Module {
    private final DragAndDrop hud = DragAndDrop.getInstance();
    private boolean syncing;
    private boolean hotbarLayoutReady;

    public final BooleanSetting watermark = addElementSetting("Ватермарка", "watermark", true);
    public final BooleanSetting targetHud = addElementSetting("Таргет HUD", "targethud", false);
    public final BooleanSetting effects = addElementSetting("Эффекты", "effects", false);
    public final BooleanSetting binds = addElementSetting("Бинды", "binds", false);
    public final BooleanSetting cooldowns = addElementSetting("Кулдауны", "cooldowns", false);
    public final BooleanSetting player = addElementSetting("Плеер", "player", false);
    public final BooleanSetting notifications = addElementSetting("Уведомления", "notifications", true);
    public final BooleanSetting armor = addElementSetting("Броня", "armor", false);
    public final BooleanSetting inventory = addElementSetting("Инвентарь", "inventory", false);
    public final BooleanSetting customHotbar = addElementSetting(
            "Кастомный хотбар", "custom_hotbar", true);

    private BooleanSetting addElementSetting(String name, String id, boolean defaultValue) {
        BooleanSetting setting = add(new BooleanSetting(name, id, "Показывает элемент HUD", defaultValue));
        setting.onChange((oldValue, newValue) -> {
            if (!syncing && isEnabled()) setElementEnabled(id, newValue);
        });
        return setting;
    }

    @Override
    protected void onEnable() {
        syncing = true;
        try {
            syncConfiguredElements();
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
            removeIfActive("player");
            removeIfActive("notifications");
            removeIfActive("armor");
            removeIfActive("inventory");
            removeIfActive("custom_hotbar");
            setElementEnabled("watermark", watermark.get());
        } finally {
            syncing = false;
        }
    }

    @Override
    public void onTick() {
        if (!hotbarLayoutReady) {
            hotbarLayoutReady = true;
            if (customHotbar.get() && !hud.isElementActive("custom_hotbar")) {
                addIfMissing("custom_hotbar");
            }
        }
        syncManualDeletion(watermark, "watermark");
        syncManualDeletion(targetHud, "targethud");
        syncManualDeletion(effects, "effects");
        syncManualDeletion(binds, "binds");
        syncManualDeletion(cooldowns, "cooldowns");
        syncManualDeletion(player, "player");
        syncManualDeletion(notifications, "notifications");
        syncManualDeletion(armor, "armor");
        syncManualDeletion(inventory, "inventory");
        syncManualDeletion(customHotbar, "custom_hotbar");
    }

    private void syncConfiguredElements() {
        setElementEnabled("watermark", watermark.get());
        setElementEnabled("targethud", targetHud.get());
        setElementEnabled("effects", effects.get());
        setElementEnabled("binds", binds.get());
        setElementEnabled("cooldowns", cooldowns.get());
        setElementEnabled("player", player.get());
        setElementEnabled("notifications", notifications.get());
        setElementEnabled("armor", armor.get());
        setElementEnabled("inventory", inventory.get());
        setElementEnabled("custom_hotbar", customHotbar.get());
        hotbarLayoutReady = hud.getRegistry().stream().anyMatch(entry -> "custom_hotbar".equals(entry.id));
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
        if (enabled) addIfMissing(id);
        else removeIfActive(id);
    }

    public void syncWatermarkVisibility() {
        setElementEnabled("watermark", watermark.get());
    }

    private void addIfMissing(String id) {
        if (hud.isElementActive(id)) return;
        float screenW = HudElement.hudScreenWidth();
        float screenH = HudElement.hudScreenHeight();
        float x = switch (id) {
            case "targethud", "cooldowns" -> screenW * 0.5f - 74f;
            case "custom_hotbar" -> screenW * 0.5f - HotbarHud.BASE_WIDTH * 0.5f;
            case "notifications", "inventory" -> screenW - 190f;
            default -> 8f;
        };
        float y = switch (id) {
            case "binds" -> 48f;
            case "effects" -> 128f;
            case "targethud" -> screenH * 0.5f + 54f;
            case "cooldowns" -> screenH - 52f;
            case "player" -> screenH - 52f;
            case "notifications" -> 58f;
            case "armor" -> screenH - 88f;
            case "inventory" -> screenH - 86f;
            case "custom_hotbar" -> screenH - HotbarHud.BASE_HEIGHT - 2f;
            default -> 8f;
        };
        hud.addElement(id, x, y);
    }

    private void removeIfActive(String id) {
        if ("watermark".equals(id)) hud.removeElementForced(hud.getActiveElementByTypeId(id));
        else hud.removeElement(hud.getActiveElementByTypeId(id));
    }
}
