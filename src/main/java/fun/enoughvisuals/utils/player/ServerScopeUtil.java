package fun.enoughvisuals.utils.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.*;

public final class ServerScopeUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Список модулей, которые скрываются только на holyworld
    private static final Set<String> HOLYWORLD_HIDDEN = Set.of(
            "fake_player", "auto_resell", "auto_potion", "fast_swap",
            "item_swap", "fast_xp", "ft_helper", "item_scroller", "click_pearl", "shift_tap"
    );

    private ServerScopeUtil() {}

    public static Scope getCurrentScope() {
        // Одиночный мир — integrated server
        if (mc.getCurrentServerEntry() == null && mc.isIntegratedServerRunning()) {
            return Scope.FULL;
        }

        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return Scope.FULL;

        String addr = info.address.toLowerCase(Locale.ROOT).trim();

        // Проверяем только на holyworld, для всего остального выдаем полный доступ
        if (addr.contains("holyworld")) {
            return Scope.HOLYWORLD;
        }

        return Scope.FULL;
    }

    public static Set<String> getHiddenModules() {
        return switch (getCurrentScope()) {
            case FULL -> Collections.emptySet(); // Ничего не скрываем
            case HOLYWORLD -> HOLYWORLD_HIDDEN;  // Скрываем модули для holyworld
        };
    }

    public enum Scope {
        FULL, HOLYWORLD
    }
}
