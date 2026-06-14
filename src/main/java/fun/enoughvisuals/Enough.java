package fun.enoughvisuals;

import net.fabricmc.api.ClientModInitializer;

public class Enough implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EnoughCore.initializeClient();
    }

    public static void setIcon() {
        EnoughCore.setIcon();
    }
}
