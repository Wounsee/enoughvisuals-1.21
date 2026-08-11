package fun.enoughvisuals.systems.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Applies servers issued by the admin panel to the visible multiplayer list. */
public final class AutoServerSync {
    public static volatile boolean isSyncing;
    private static final List<String> previousAutoAddresses = new ArrayList<>();

    private AutoServerSync() {
    }

    static void apply(List<RemoteConfig.AutoServer> servers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        client.execute(() -> {
            isSyncing = true;
            try {
                ServerList list = new ServerList(client);
                list.loadFile();

                List<String> targetAddresses = new ArrayList<>();
                for (RemoteConfig.AutoServer server : servers) {
                    targetAddresses.add(server.address().toLowerCase(Locale.ROOT));
                }

                for (String oldAddress : previousAutoAddresses) {
                    if (!targetAddresses.contains(oldAddress)) {
                        ServerInfo oldServer = list.get(oldAddress);
                        if (oldServer != null) list.remove(oldServer);
                    }
                }
                previousAutoAddresses.clear();
                previousAutoAddresses.addAll(targetAddresses);

                for (int targetIndex = 0; targetIndex < servers.size(); targetIndex++) {
                    RemoteConfig.AutoServer server = servers.get(targetIndex);
                    ServerInfo current = list.get(server.address());
                    if (current == null) {
                        current = new ServerInfo(server.name(), server.address(), ServerInfo.ServerType.OTHER);
                        list.add(current, false);
                    } else {
                        current.name = server.name();
                    }

                    int currentIndex = -1;
                    for (int index = 0; index < list.size(); index++) {
                        if (list.get(index) == current) {
                            currentIndex = index;
                            break;
                        }
                    }
                    if (currentIndex >= 0 && currentIndex != targetIndex) {
                        list.swapEntries(currentIndex, targetIndex);
                    }
                }

                list.saveFile();
            } finally {
                isSyncing = false;
            }
        });
    }
}
