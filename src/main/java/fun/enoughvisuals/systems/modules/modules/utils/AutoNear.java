package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.player.ServerUtil;
import net.minecraft.client.MinecraftClient;

@Register(name = "Auto Near", id = "autonear", description = "Автоматически отправляет команду /near max через заданный интервал", category = Category.UTILS)
public class AutoNear extends Module {

    private static AutoNear INSTANCE;

    public final SliderSetting delaySeconds = add(new SliderSetting("Задержка (сек)", "delay", "Интервал между отправкой команд /near в секундах", 60.0, 10.0, 300.0, 10.0));

    private long lastCommandMs = 0L;

    public AutoNear() {
        INSTANCE = this;
    }

    public static AutoNear getInstance() {
        return INSTANCE;
    }

    private boolean isServerAllowed() {
        if (mc.isInSingleplayer()) {
            return true;
        }
        return ServerUtil.isFunTimeServer();
    }

    @Override
    public void onTick() {
        if (!isInGame() || mc.getNetworkHandler() == null || !isServerAllowed()) {
            return;
        }

        long now = System.currentTimeMillis();
        long delayMs = Math.max(10L, delaySeconds.get().longValue()) * 1000L;

        if (now - lastCommandMs < delayMs) {
            return;
        }

        mc.getNetworkHandler().sendChatCommand("near max");
        lastCommandMs = now;
    }

    @Override
    protected void onDisable() {
        lastCommandMs = 0L;
    }
}
