package fun.enoughvisuals;

import fun.enoughvisuals.systems.accounts.AccountSessionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import java.util.concurrent.atomic.AtomicBoolean;

public class Enough implements ClientModInitializer {
    private static final AtomicBoolean ENOUGH_INITIALIZED = new AtomicBoolean();

    @Override
    public void onInitializeClient() {
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
