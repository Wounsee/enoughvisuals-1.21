package fun.wounsee.cosmetic.config.fabric;

import fun.wounsee.cosmetic.config.ConfigKeyBind;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigKeyBindImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger("cosmetic/ConfigKeyBindImpl");

    private ConfigKeyBindImpl() {
    }

    public static void addKeyBind(ConfigKeyBind keyBind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.options != null) {
            registerNow(keyBind);
            return;
        }

        // Configs are created from static initializers. Registering through
        // Fabric API at that point loads KeyBindingAccessor before Minecraft's
        // options/category map is ready, which leaves the accessor stub loaded
        // and later makes unrelated mods (for example EntityCulling) crash.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> registerNow(keyBind));
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
