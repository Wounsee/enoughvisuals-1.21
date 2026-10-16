package fun.enoughvisuals.utils.player;

import fun.enoughvisuals.systems.modules.Module;

import java.util.Set;

/**
 * The only per-server rule left: every module works everywhere, except a few
 * inventory automations and the macros tab on HolyWorld and ReallyWorld.
 */
public final class ServerRestrictions {
    public static final String MACROS_TOOL = "tool:macros";
    private static final Set<String> RESTRICTED_MODULES = Set.of("fast_swap", "item_swap", "item_scroller", "macros");

    private ServerRestrictions() {
    }

    public static boolean isRestrictedServer() {
        return ServerUtil.isHolyWorldServer() || ServerUtil.isReallyWorldServer();
    }

    public static boolean isModuleBlocked(Module module) {
        return module != null && RESTRICTED_MODULES.contains(module.getId()) && isRestrictedServer();
    }

    public static boolean isToolBlocked(String toolId) {
        return MACROS_TOOL.equals(toolId) && isRestrictedServer();
    }
}
