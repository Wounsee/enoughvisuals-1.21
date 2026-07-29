package fun.enoughvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class Enough implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The embedded launcher/loader resolves client mixins while the client
        // entrypoint list is still being invoked. Initializing every module here
        // can load KeyBinding before Fabric API's accessor mixin is applied,
        // leaving its stub methods to throw AssertionError in other mods.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> EnoughCore.initializeClient());
    }

    public static void setIcon() {
        EnoughCore.setIcon();
    }
}
