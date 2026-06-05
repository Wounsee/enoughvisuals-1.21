package fun.enoughvisuals;

import fun.enoughvisuals.rt.entry.EntryBridge;
import net.fabricmc.api.ClientModInitializer;

public class Enough implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EnoughCore.initializeClient();
    }

    public static void setIcon() {
        EnoughCore.setIcon();
    } // EntryBridge
}
