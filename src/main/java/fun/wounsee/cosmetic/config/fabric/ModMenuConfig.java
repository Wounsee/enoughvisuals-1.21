package fun.wounsee.cosmetic.config.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.gui.screens.ConfigScreen;

public class ModMenuConfig implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> new ConfigScreen(parentScreen, CosmeticMod.debugModeEnabled());
    }
}