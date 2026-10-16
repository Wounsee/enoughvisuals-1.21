package fun.enoughvisuals.utils.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ServerUtil {

    private static final Pattern FUNTIME_EVENTS_HOST = Pattern.compile(
            "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*)funtime\\.(?:su|sh)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOLYWORLD_HOST = Pattern.compile(
            "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*)holyworld\\.[a-z]{2,63}$",
            Pattern.CASE_INSENSITIVE);

    private record HostInfo(String source, boolean portStripped, String host, Set<String> segments) {
        static final HostInfo NONE = new HostInfo(null, false, "", Set.of());
    }

    /**
     * Host checks run from render and tick code for every module, so the
     * normalized host and its dot-separated segments are cached per address.
     */
    private static volatile HostInfo cachedHost = HostInfo.NONE;

    public static String getCurrentServerHost() {
        return currentHost().host();
    }

    private static HostInfo currentHost() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.isInSingleplayer()) return HostInfo.NONE;

        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry != null && entry.address != null) {
            return hostInfo(entry.address, true);
        }

        var handler = mc.getNetworkHandler();
        if (handler == null) return HostInfo.NONE;

        ServerInfo handlerInfo = handler.getServerInfo();
        if (handlerInfo != null && handlerInfo.address != null) {
            return hostInfo(handlerInfo.address, true);
        }

        SocketAddress peer = handler.getConnection().getAddress();
        if (peer instanceof InetSocketAddress isa && isa.getHostString() != null) {
            return hostInfo(isa.getHostString(), false);
        }
        return HostInfo.NONE;
    }

    private static HostInfo hostInfo(String source, boolean stripPort) {
        HostInfo cached = cachedHost;
        if (cached.portStripped() == stripPort && source.equals(cached.source())) {
            return cached;
        }

        String host = source.toLowerCase(Locale.ROOT).trim();
        if (stripPort) {
            int sep = host.indexOf(':');
            if (sep >= 0) host = host.substring(0, sep);
        }
        Set<String> segments = host.isEmpty()
                ? Set.of()
                : Set.copyOf(Arrays.asList(host.split("\\.")));
        HostInfo info = new HostInfo(source, stripPort, host, segments);
        cachedHost = info;
        return info;
    }

    private static boolean hasServerSegment(String expectedSegment) {
        return currentHost().segments().contains(expectedSegment);
    }

    public static boolean isFunTimeServer() {
        return hasServerSegment("funtime") || hasServerSegment("funsky");
    }

    public static boolean isFunTimeEventsServer() {
        return FUNTIME_EVENTS_HOST.matcher(getCurrentServerHost()).matches();
    }

    public static boolean isHolyWorldServer() {
        return hasServerSegment("holyworld");
    }

    public static boolean isReallyWorldServer() {
        return hasServerSegment("reallyworld");
    }

    public static boolean isHolyWorldEventsServer() {
        return HOLYWORLD_HOST.matcher(getCurrentServerHost()).matches();
    }

    public static String getServerAddress() {
        return getCurrentServerHost();
    }
}
