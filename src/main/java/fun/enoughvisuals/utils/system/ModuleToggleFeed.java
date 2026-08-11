package fun.enoughvisuals.utils.system;

import fun.enoughvisuals.systems.hud.HudNotificationCenter;

public final class ModuleToggleFeed {
    private static final long SHOW_MS = 3200L;

    public record ToggleEvent(String moduleName, boolean enabled, long expiresAtMs) {
        public String formattedText() {
            return moduleName + (enabled ? " включен" : " выключен");
        }
    }

    private static volatile ToggleEvent active;

    private ModuleToggleFeed() {}

    public static void push(String moduleName, boolean enabled) {
        if (moduleName == null || moduleName.isBlank()) return;
        active = new ToggleEvent(moduleName, enabled, System.currentTimeMillis() + SHOW_MS);
        HudNotificationCenter.module(moduleName, enabled);
    }

    public static ToggleEvent getActive() {
        ToggleEvent event = active;
        if (event == null) return null;
        if (System.currentTimeMillis() > event.expiresAtMs()) {
            active = null;
            return null;
        }
        return event;
    }
}
