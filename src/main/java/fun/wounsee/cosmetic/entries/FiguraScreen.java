package fun.wounsee.cosmetic.entries;

import net.minecraft.client.gui.screens.Screen;
import fun.wounsee.cosmetic.gui.widgets.PanelSelectorWidget;

public interface FiguraScreen {

    /**
     * @param parentScreen the screen that will open when closing this screen
     * @return the screen that will be opened when clicking in the panel list
     */
    Screen getScreen(Screen parentScreen);

    /**
     * @return the icon used in the panel selector widget
     */
    default PanelSelectorWidget.PanelIcon getPanelIcon() {
        return PanelSelectorWidget.PanelIcon.OTHER;
    }
}
