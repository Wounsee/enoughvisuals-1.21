package fun.wounsee.cosmetic.backend2.websocket;

import net.minecraft.network.chat.Component;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.backend2.NetworkStuff;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.gui.FiguraToast;
import fun.wounsee.cosmetic.utils.ColorUtils;
import fun.wounsee.cosmetic.utils.FiguraText;
import fun.wounsee.cosmetic.utils.TextUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class S2CMessageHandler {

    public static final byte
        AUTH = 0,
        PING = 1,
        EVENT = 2,
        TOAST = 3,
        CHAT = 4,
        NOTICE = 5;

    public static void handle(ByteBuffer bytes) {
        if (!bytes.hasRemaining())
            return;

        byte b = bytes.get();
        if (NetworkStuff.debug)
            CosmeticMod.debug("Got ws message of type: " + b);

        switch (b) {
            case AUTH -> auth();
            case PING -> ping(bytes);
            case EVENT -> event(bytes);
            case TOAST -> toast(bytes);
            case CHAT -> chat(bytes);
            case NOTICE -> notice(bytes);
        }
    }

    private static void auth() {
        CosmeticMod.LOGGER.info("Connected to " + CosmeticMod.MOD_NAME + " ws backend");
        NetworkStuff.backendStatus = 3;
        NetworkStuff.subscribeAll();
        if (Configs.CONNECTION_TOASTS.value)
            FiguraToast.sendToast(FiguraText.of("backend.connected"));
    }

    private static void ping(ByteBuffer bytes) {
        UUID uuid = new UUID(bytes.getLong(), bytes.getLong());

        Avatar avatar = AvatarManager.getLoadedAvatar(uuid);
        if (avatar == null)
            return;

        int id = bytes.getInt();
        bytes.get(); // sync value is ignored

        byte[] data = new byte[bytes.remaining()];
        bytes.get(data);

        avatar.runPing(id, data);
        NetworkStuff.pingsReceived++;
        if (NetworkStuff.lastPing == 0) NetworkStuff.lastPing = CosmeticMod.ticks;
    }

    private static void event(ByteBuffer bytes) {
        UUID uuid = new UUID(bytes.getLong(), bytes.getLong());
        AvatarManager.reloadAvatar(uuid);
    }

    private static void toast(ByteBuffer bytes) {
        byte type = bytes.get();
        String[] str = StandardCharsets.UTF_8.decode(bytes).toString().split("\0", 2);
        FiguraToast.sendToast(str[0], str.length > 1 ? str[1] : "", FiguraToast.ToastType.values()[type]);
    }

    private static void chat(ByteBuffer bytes) {
        String message = StandardCharsets.UTF_8.decode(bytes).toString();
        CosmeticMod.sendChatMessage(Component.empty().append(Component.literal("-- " + CosmeticMod.MOD_NAME + " backend message --\n\n").withStyle(ColorUtils.Colors.SOFT_BLUE.style)).append(TextUtils.tryParseJson(message)));
    }

    private static void notice(ByteBuffer bytes) {
        if (!Configs.CONNECTION_TOASTS.value)
            return;

        byte type = bytes.get();
        switch (type) {
            case 0 -> FiguraToast.sendToast(FiguraText.of("backend.warning"), FiguraText.of("backend.ping_size"), FiguraToast.ToastType.ERROR);
            case 1 -> FiguraToast.sendToast(FiguraText.of("backend.warning"), FiguraText.of("backend.ping_rate"), FiguraToast.ToastType.ERROR);
        }
    }
}
