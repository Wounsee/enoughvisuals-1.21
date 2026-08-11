package fun.wounsee.cosmetic.config.fabric;

import fun.wounsee.cosmetic.config.ConfigKeyBind;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigKeyBindImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger("cosmetic/ConfigKeyBindImpl");

    private ConfigKeyBindImpl() {
    }

    public static void addKeyBind(ConfigKeyBind keyBind) {
        // ConfigManager.init() runs from the Fabric client entrypoint, which is
        // the supported registration phase. CLIENT_STARTED is already too late:
        // GameOptions has built its key array by then and Fabric deliberately
        // rejects any additional registration.
        registerNow(keyBind);
    }

    private static void registerNow(ConfigKeyBind keyBind) {
        try {
            KeyBindingHelper.registerKeyBinding(keyBind);
        } catch (RuntimeException | AssertionError error) {
            // Cosmetic keybinds are optional; one failed bind must not abort the
            // entire client. Keep a useful diagnostic instead of hiding it.
            LOGGER.error("Failed to register a cosmetic keybinding", error);
        }
    }
}
