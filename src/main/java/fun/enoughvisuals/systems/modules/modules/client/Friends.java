package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.utils.ClientGuiModule;
import fun.enoughvisuals.ui.clienttools.FriendsScreen;
import net.minecraft.client.gui.screen.Screen;

@Register(name = "Friends", id = "friends", description = "Friend list manager", category = Category.CLIENT)
public class Friends extends ClientGuiModule {
    @Override
    protected Screen createScreen() {
        return new FriendsScreen();
    }
}
