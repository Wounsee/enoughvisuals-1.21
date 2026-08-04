package punchy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import punchy.Punchy;
import punchy.client.PunchyClient;
import punchy.client.command.PunchyCommands;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/fabric/PunchyFabricClient.class */
public class PunchyFabricClient implements ClientModInitializer {
    private static final class_2960 BUILTIN_PACK_ID = class_2960.method_60655(Punchy.MOD_ID, Punchy.MOD_ID);

    public void onInitializeClient() {
        FabricLoader.getInstance().getModContainer(Punchy.MOD_ID).ifPresent(container -> {
            ResourceManagerHelper.registerBuiltinResourcePack(BUILTIN_PACK_ID, container, class_2561.method_43470("§bPunchy!"), ResourcePackActivationType.DEFAULT_ENABLED);
        });
        PunchyClient.init();
        PunchyCommands.register();
    }
}
