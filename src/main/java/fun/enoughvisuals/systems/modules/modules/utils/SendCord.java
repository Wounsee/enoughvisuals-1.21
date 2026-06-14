package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;

import java.util.List;

@Register(
        name = "SendCord",
        id = "send_cord",
        description = "Отправляет координаты в чат по бинду",
        category = Category.UTILS
)
public final class SendCord extends Module {
    private static SendCord INSTANCE;

    private final BindSetting sendBind = add(new BindSetting("Бинд отправки", "send_bind", -1));
    private final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Куда отправлять координаты",
            "Клан",
            "Глобальный",
            "Клан"
    ));

    public SendCord() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.sendBind.isKey(key)) return;
        INSTANCE.sendCoordinates();
    }

    private void sendCoordinates() {
        if (!isInGame() || mc.player.networkHandler == null) return;

        String coords = String.format(
                "X: %d Y: %d Z: %d",
                mc.player.getBlockX(),
                mc.player.getBlockY(),
                mc.player.getBlockZ()
        );

        if (mode.is("Глобальный")) {
            mc.player.networkHandler.sendChatMessage("!" + coords);
            return;
        }

        if (mode.is("Клан")) {
            mc.player.networkHandler.sendChatCommand("cc " + coords);
            return;
        }

        List<String> friends = FriendRepo.listFriends();
        if (friends.isEmpty()) {
            ClientActionUtil.clientMessage("Список друзей пуст, координаты не отправлены.");
            return;
        }

        for (String friend : friends) {
            mc.player.networkHandler.sendChatCommand("msg " + friend + " " + coords);
        }
    }
}
