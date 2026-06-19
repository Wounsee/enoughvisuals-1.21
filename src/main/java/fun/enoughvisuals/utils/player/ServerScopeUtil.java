package fun.enoughvisuals.utils.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.*;

public final class ServerScopeUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Set<String> STRICT_HIDDEN = Set.of(
            "fake_player", "auto_resell", "auto_potion", "fast_swap",
            "item_swap", "fast_xp", "ft_helper", "item_scroller", "click_pearl"
    );
    private static final Set<String> LITE_HIDDEN = Set.of(
            "fast_swap", "item_swap", "ft_helper", "click_pearl"
    );

    private ServerScopeUtil() {}

    public static Scope getCurrentScope() {
        // Одиночный мир — integrated server
        if (mc.getCurrentServerEntry() == null && mc.isIntegratedServerRunning()) {
            return Scope.FULL;
        }

        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null) return Scope.LITE;

        String addr = info.address.toLowerCase(Locale.ROOT).trim();
        if (addr.contains("funtime.su") || addr.contains("localhost") || addr.contains("127.0.0.1")) {
            return Scope.FULL;
        }
        if (addr.contains("holyworld") || addr.contains("reallyworld")) {
            return Scope.STRICT;
        }
        return Scope.LITE;
    }

    public static Set<String> getHiddenModules() {
        return switch (getCurrentScope()) {
            case FULL -> Collections.emptySet();
            case STRICT -> STRICT_HIDDEN;
            case LITE -> LITE_HIDDEN;
        };
    }

    public enum Scope {
        FULL, STRICT, LITE
    }
}
