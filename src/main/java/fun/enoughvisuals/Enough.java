package fun.enoughvisuals;

import fun.enoughvisuals.systems.accounts.AccountSessionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import punchy.fabric.PunchyFabricClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class Enough implements ClientModInitializer {
    private static final AtomicBoolean PUNCHY_INITIALIZED = new AtomicBoolean();
    private static final AtomicBoolean ENOUGH_INITIALIZED = new AtomicBoolean();

    @Override
    public void onInitializeClient() {
        // Punchy is bundled into EnoughVisuals, so its Fabric entrypoint is not
        // discovered as a separate mod. Initialize it here instead of injecting
        // into MinecraftClient's constructor. Constructor FIELD injections are
        // mapping-sensitive and can run before Minecraft has finished creating
        // its options/keybinding state.
        if (PUNCHY_INITIALIZED.compareAndSet(false, true)) {
            new PunchyFabricClient().onInitializeClient();
        }

        // The main module registry touches keybindings and other client-owned
        // objects. Wait until MinecraftClient is fully constructed.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (ENOUGH_INITIALIZED.compareAndSet(false, true)) {
                AccountSessionManager.restoreLastSelected();
                EnoughCore.initializeClient();
            }
        });
    }

    public static void setIcon() {
        EnoughCore.setIcon();
    }
}
