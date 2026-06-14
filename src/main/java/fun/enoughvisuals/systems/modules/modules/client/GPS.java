package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.utils.ClientGuiModule;
import fun.enoughvisuals.ui.clienttools.GpsScreen;
import net.minecraft.client.gui.screen.Screen;

@Register(name = "GPS", id = "gps", description = "Waypoint manager", category = Category.CLIENT)
public class GPS extends ClientGuiModule {
    @Override
    protected Screen createScreen() {
        return new GpsScreen();
    }
}
