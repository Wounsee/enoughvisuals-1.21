package punchy.platform;

import net.fabricmc.loader.api.FabricLoader;
import punchy.platform.services.IPlatformHelper;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/platform/FabricPlatformHelper.class */
public class FabricPlatformHelper implements IPlatformHelper {
    @Override // punchy.platform.services.IPlatformHelper
    public String getPlatformName() {
        return "Fabric";
    }

    @Override // punchy.platform.services.IPlatformHelper
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override // punchy.platform.services.IPlatformHelper
    public String getModDisplayName(String modId) {
        if (modId == null || modId.isBlank()) {
            return "";
        }
        return (String) FabricLoader.getInstance().getModContainer(modId).map(container -> {
            return container.getMetadata().getName();
        }).filter(name -> {
            return (name == null || name.isBlank()) ? false : true;
        }).orElse(modId);
    }

    @Override // punchy.platform.services.IPlatformHelper
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
