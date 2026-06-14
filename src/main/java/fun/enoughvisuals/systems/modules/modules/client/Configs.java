package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.utils.ClientGuiModule;
import fun.enoughvisuals.ui.clienttools.ConfigsScreen;
import net.minecraft.client.gui.screen.Screen;

@Register(name = "Configs", id = "configs", description = "Config manager", category = Category.CLIENT)
public class Configs extends ClientGuiModule {
    @Override
    protected Screen createScreen() {
        return new ConfigsScreen();
    }
}
