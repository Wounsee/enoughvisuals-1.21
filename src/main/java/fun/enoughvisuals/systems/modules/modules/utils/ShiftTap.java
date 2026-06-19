package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.network.ServerInfo;

import java.util.Locale;

@Register(name = "Shift Tap", id = "shift_tap", description = "Авто-отжатие shift при ударе (Funtime / SP)", category = Category.UTILS)
public class ShiftTap extends Module {
    private static ShiftTap INSTANCE;
    private static int unsneakTicks = 0;

    public ShiftTap() {
        INSTANCE = this;
    }

    public static void onAttack() {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame()) return;
        if (!isAllowed()) return;
        unsneakTicks = 3;
    }

    @Override
    public void onTick() {
        if (unsneakTicks > 0) {
            unsneakTicks--;
            if (mc.options != null && mc.options.sneakKey != null) {
                mc.options.sneakKey.setPressed(false);
            }
        }
    }

    private static boolean isAllowed() {
        if (mc.isIntegratedServerRunning()) return true;
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return false;
        return info.address.toLowerCase(Locale.ROOT).contains("funtime.su");
    }
}
