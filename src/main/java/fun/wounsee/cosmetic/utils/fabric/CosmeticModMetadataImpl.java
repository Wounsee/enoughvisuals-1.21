package fun.wounsee.cosmetic.utils.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import fun.wounsee.cosmetic.utils.CosmeticModMetadata;
import fun.wounsee.cosmetic.utils.Version;

public class CosmeticModMetadataImpl extends CosmeticModMetadata {
    private final ModMetadata fabricMetada;
    protected CosmeticModMetadataImpl(String modID) {
        super(modID);
        this.fabricMetada = FabricLoader.getInstance().getModContainer(modID).get().getMetadata();
    }

    public static CosmeticModMetadata getMetadataForMod(String modID) {
        return new CosmeticModMetadataImpl(modID);
    }

    @Override
    public String getCustomValueAsString(String key) {
        return fabricMetada.getCustomValue(key).getAsString();
    }

    @Override
    public Number getCustomValueAsNumber(String key) {
        return fabricMetada.getCustomValue(key).getAsNumber();
    }

    @Override
    public Boolean getCustomValueAsBoolean(String key) {
        return fabricMetada.getCustomValue(key).getAsBoolean();
    }

    @Override
    public Object getCustomValueAsObject(String key) {
        return fabricMetada.getCustomValue(key).getAsObject();
    }


    @Override
    public Version getModVersion() {
        return new Version(fabricMetada.getVersion().getFriendlyString());
    }
}
