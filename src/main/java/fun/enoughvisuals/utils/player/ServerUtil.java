package fun.enoughvisuals.utils.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.regex.Pattern;

public class ServerUtil {

    private static final Pattern FUNTIME_EVENTS_HOST = Pattern.compile(
            "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*)funtime\\.(?:su|sh)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOLYWORLD_HOST = Pattern.compile(
            "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*)holyworld\\.[a-z]{2,63}$",
            Pattern.CASE_INSENSITIVE);

    public static String getCurrentServerHost() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return "";
        if (mc.isInSingleplayer()) return "";

        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry != null && entry.address != null) {
            return stripPort(entry.address);
        }

        var handler = mc.getNetworkHandler();
        if (handler == null) return "";

        ServerInfo handlerInfo = handler.getServerInfo();
        if (handlerInfo != null && handlerInfo.address != null) {
            return stripPort(handlerInfo.address);
        }

        SocketAddress peer = handler.getConnection().getAddress();
        if (peer instanceof InetSocketAddress isa && isa.getHostString() != null) {
            return isa.getHostString().toLowerCase().trim();
        }
        return "";
    }

    private static String stripPort(String addr) {
        String s = addr.toLowerCase().trim();
        int sep = s.indexOf(':');
        return sep >= 0 ? s.substring(0, sep) : s;
    }

    private static boolean hasServerSegment(String expectedSegment) {
        String host = getCurrentServerHost();
        if (host.isEmpty()) {
            return false;
        }

        String[] parts = host.split("\\.");
        for (String part : parts) {
            if (part.equals(expectedSegment)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFunTimeServer() {
        return hasServerSegment("funtime") || hasServerSegment("funsky");
    }

    public static boolean isFunTimeEventsServer() {
        return FUNTIME_EVENTS_HOST.matcher(getCurrentServerHost()).matches();
    }

    public static boolean isFunTrainerServer() {
        return hasServerSegment("funtrainer");
    }

    public static boolean isFillCubeServer() {
        return hasServerSegment("fillcube");
    }

    public static boolean isHolyWorldServer() {
        return hasServerSegment("holyworld");
    }

    public static boolean isHolyWorldEventsServer() {
        return HOLYWORLD_HOST.matcher(getCurrentServerHost()).matches();
    }

    public static boolean isShiftTapSupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return isFunTimeServer()
                || hasServerSegment("funmoon")
                || hasServerSegment("prostotrainer");
    }

    public static boolean isAutoSwapSupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return hasServerSegment("funtime")
                || hasServerSegment("funsky")
                || hasServerSegment("holytime")
                || hasServerSegment("spookytime")
                || hasServerSegment("prostotrainer")
                || hasServerSegment("funtrainer");
    }

    public static boolean isAutoPotionSupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return hasServerSegment("funtime")
                || hasServerSegment("funsky")
                || hasServerSegment("holytime")
                || hasServerSegment("space-times")
                || hasServerSegment("spookytime")
                || hasServerSegment("fillcube");
    }

    public static boolean isElytraUtilitySupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return hasServerSegment("funtime")
                || hasServerSegment("funsky")
                || hasServerSegment("holytime")
                || hasServerSegment("spookytime")
                || hasServerSegment("funmoon");
    }

    public static boolean isItemScrollerSupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return hasServerSegment("funtime")
                || hasServerSegment("holyworld")
                || hasServerSegment("holytime")
                || hasServerSegment("funsky")
                || hasServerSegment("space-times")
                || hasServerSegment("spookytime")
                || hasServerSegment("stray")
                || hasServerSegment("funmoon");
    }

    public static boolean isMouseClickerSupported() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;
        if (mc.isInSingleplayer()) return true;

        return hasServerSegment("funtime")
                || hasServerSegment("funsky")
                || hasServerSegment("holytime")
                || hasServerSegment("space-times")
                || hasServerSegment("spookytime")
                || hasServerSegment("funmoon")
                || hasServerSegment("fillcube");
    }

    public static String getServerAddress() {
        return getCurrentServerHost();
    }
}
