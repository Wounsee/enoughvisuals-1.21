package fun.enoughvisuals.systems.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Applies servers issued by the admin panel to the visible multiplayer list.
 *
 * <p>Every address the panel added is remembered on disk, so a server removed
 * from the panel is removed from the list even after a restart. Servers the
 * player added themselves are never tracked and therefore never removed, even
 * if the panel later publishes the same address.
 */
public final class AutoServerSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/AutoServers");
    public static volatile boolean isSyncing;
    private static Set<String> managedAddresses;

    private AutoServerSync() {
    }

    static void apply(List<RemoteConfig.AutoServer> servers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        List<RemoteConfig.AutoServer> target = List.copyOf(servers);
        client.execute(() -> applyOnClientThread(client, target));
    }

    private static void applyOnClientThread(MinecraftClient client, List<RemoteConfig.AutoServer> servers) {
        isSyncing = true;
        try {
            Set<String> managed = managed(client);
            ServerList list = new ServerList(client);
            list.loadFile();

            Set<String> targetAddresses = new LinkedHashSet<>();
            for (RemoteConfig.AutoServer server : servers) {
                targetAddresses.add(key(server.address()));
            }

            for (int index = list.size() - 1; index >= 0; index--) {
                ServerInfo info = list.get(index);
                String address = key(info.address);
                if (managed.contains(address) && !targetAddresses.contains(address)) {
                    list.remove(info);
                }
            }
            managed.retainAll(targetAddresses);

            for (int targetIndex = 0; targetIndex < servers.size(); targetIndex++) {
                RemoteConfig.AutoServer server = servers.get(targetIndex);
                String address = key(server.address());
                ServerInfo current = list.get(server.address());
                if (current == null) {
                    current = new ServerInfo(server.name(), server.address(), ServerInfo.ServerType.OTHER);
                    list.add(current, false);
                    managed.add(address);
                } else if (managed.contains(address)) {
                    current.name = server.name();
                }

                int currentIndex = indexOf(list, current);
                if (currentIndex >= 0 && currentIndex != targetIndex && targetIndex < list.size()) {
                    list.swapEntries(currentIndex, targetIndex);
                }
            }

            list.saveFile();
            saveManaged(client, managed);
        } catch (Exception error) {
            LOGGER.warn("Failed to sync auto servers", error);
        } finally {
            isSyncing = false;
        }
    }

    private static int indexOf(ServerList list, ServerInfo info) {
        for (int index = 0; index < list.size(); index++) {
            if (list.get(index) == info) return index;
        }
        return -1;
    }

    private static String key(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    private static Path stateFile(MinecraftClient client) {
        return client.runDirectory.toPath().resolve("enoughvisuals").resolve("auto_servers.json");
    }

    private static Set<String> managed(MinecraftClient client) {
        if (managedAddresses != null) return managedAddresses;
        managedAddresses = new LinkedHashSet<>();
        Path file = stateFile(client);
        try {
            if (Files.isRegularFile(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (root.isJsonArray()) {
                    for (JsonElement element : root.getAsJsonArray()) {
                        managedAddresses.add(key(element.getAsString()));
                    }
                }
            }
        } catch (Exception error) {
            LOGGER.warn("Could not read {}", file, error);
        }
        return managedAddresses;
    }

    private static void saveManaged(MinecraftClient client, Set<String> managed) {
        Path file = stateFile(client);
        try {
            Files.createDirectories(file.getParent());
            JsonArray array = new JsonArray();
            managed.forEach(array::add);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, array.toString(), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) {
            LOGGER.warn("Could not write {}", file, error);
        }
    }
}
