package fun.wounsee.cosmetic.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.server.packs.PackType;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.config.ConfigManager;
import fun.wounsee.cosmetic.utils.fabric.FiguraResourceListenerImpl;

public class CosmeticModFabric extends CosmeticMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.init();
        onClientInit();
        // we cast here to the impl that implements synchronous as the manager wants
        // register reload listener
        ResourceManagerHelper managerHelper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        getResourceListeners().forEach(figuraResourceListener -> managerHelper.registerReloadListener((FiguraResourceListenerImpl)figuraResourceListener));
    }
}
