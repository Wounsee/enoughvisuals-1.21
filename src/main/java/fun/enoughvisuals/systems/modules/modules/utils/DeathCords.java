package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.util.math.BlockPos;

@Register(name = "Death Cords", id = "death_cords", description = "GPS метка и координаты при смерти", category = Category.UTILS)
public class DeathCords extends Module {
    private static DeathCords INSTANCE;
    private static long lastDeath = 0;

    public DeathCords() {
        INSTANCE = this;
    }

    public static void onDeath() {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame()) return;
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now - lastDeath < 2000L) return;
        lastDeath = now;

        BlockPos pos = mc.player.getBlockPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        String label = "Смерть";

        // GPS метка на 20 минут (isEvent = true, автоудаление через 20 мин = 1 200 000 мс)
        // GpsSystem удаляет event метки через 15 мин (900_000 мс), поэтому создадим обычную
        GpsSystem.getInstance().addWaypoint(label, x, y, z, false);

        ClientActionUtil.clientMessage("§cСмерть: §f" + x + " §7" + y + " §f" + z);
    }
}
