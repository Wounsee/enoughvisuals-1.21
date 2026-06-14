package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.settings.ClickSetting;
import net.minecraft.client.gui.screen.Screen;

public abstract class ClientGuiModule extends Module {
    protected ClientGuiModule() {
    }

    @Override
    public void toggle() {
        openGui();
    }

    @Override
    public void enable() {
        openGui();
    }

    @Override
    public void enableSilent() {
        disableSilent();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            openGui();
        } else {
            disableSilent();
        }
    }

    @Override
    public void setEnabledSilent(boolean enabled) {
        disableSilent();
    }

    protected void openGui() {
        if (mc == null || !canOpenGui()) {
            return;
        }
        Screen screen = createScreen();
        if (screen != null) {
            mc.setScreen(screen);
        }
    }

    @Override
    protected boolean canEnable() {
        return canOpenGui();
    }

    protected boolean canOpenGui() {
        return !ModuleManager.getInstance().isBlocked(this);
    }

    protected abstract Screen createScreen();
}
