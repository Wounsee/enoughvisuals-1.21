package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.gui.screen.DeathScreen;

@Register(name = "AutoRespawn", id = "auto_respawn", description = "", category = Category.UTILS)
public class AutoRespawn extends Module {
    private final BooleanSetting closeScreen = add(new BooleanSetting("Закрывать экран смерти", "close_screen", "Закрывать DeathScreen после респавна", true));
    private final BooleanSetting commandEnabled = add(new BooleanSetting("Команда после смерти", "command_enabled", "Отправлять команду после возрождения", false));
    private final StringSetting command = (StringSetting) add(new StringSetting("Команда", "command", "Команда без /", "/kit start", 64, "/kit start")).visibleWhen(commandEnabled::get);

    private boolean respawned;

    @Override
    public void onTick() {
        if (!isInGame()) {
            respawned = false;
            return;
        }

        if ((mc.currentScreen instanceof DeathScreen || !mc.player.isAlive()) && !respawned) {
            mc.player.requestRespawn();
            if (closeScreen.get()) mc.setScreen(null);
            if (commandEnabled.get()) ClientActionUtil.sendCommand(command.get());
            respawned = true;
        }

        if (mc.player.isAlive()) respawned = false;
    }
}
