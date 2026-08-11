package fun.enoughvisuals.systems.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.Info;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import fun.enoughvisuals.systems.cosmetic.CosmeticCatalog;
import fun.enoughvisuals.systems.graffiti.GraffitiManager;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Base64;

/**
 * Polls the EnoughVisuals backend for remote configuration:
 * blocked modules, required client version and a global client block switch.
 *
 * <p>Production networking is owned by EnoughLauncher. The mod only sends a
 * request document through the authenticated loopback bridge created for the
 * current protected launch session.
 */
public final class RemoteConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Remote");
    private static final RemoteConfig INSTANCE = new RemoteConfig();

    private static final long POLL_INTERVAL_MS = 5_000L;
    private static final long MULTIPLAYER_POLL_INTERVAL_MS = 300L;

    private final AtomicBoolean started = new AtomicBoolean(false);

    // ---- live state (volatile: read from render thread) ----
    private volatile Set<String> blockedModules = Collections.emptySet();
    private volatile boolean blockClient = false;
    private volatile String blockMessage = "";
    private volatile String blockUrl = "https://t.me/enoughvisuals";
    private volatile boolean updateRequired = false;
    private volatile String latestVersion = Info.VERSION;
    private volatile String updateMessage = "";
    private volatile String updateUrl = "https://t.me/enoughvisuals";
    private volatile boolean banned = false;
    private volatile String banReason = "";
    private volatile boolean everSynced = false;
    private volatile Set<String> enoughUsers = Collections.emptySet();
    private volatile List<AutoServer> autoServers = Collections.emptyList();
    private volatile String launcherRole = "User";

    public record AutoServer(String name, String address) {
    }

    private RemoteConfig() {
    }

    public static RemoteConfig getInstance() {
        return INSTANCE;
    }

    /** Starts the background polling thread once. */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(this::loop, "EnoughVisuals-Remote");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        while (true) {
            try {
                sync();
            } catch (Throwable t) {
                LOGGER.debug("sync failed: {}", t.toString());
            }
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                Thread.sleep(client != null && client.getCurrentServerEntry() != null
                        ? MULTIPLAYER_POLL_INTERVAL_MS : POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void sync() throws Exception {
        String body = buildRequest();
        String response = LauncherNetworkClient.sync(body);
        if (response == null || response.isBlank()) {
            return;
        }
        apply(JsonParser.parseString(response).getAsJsonObject());
        everSynced = true;
    }

    private String buildRequest() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String username = "";
        String uuid = "";
        try {
            if (mc != null && mc.getSession() != null) {
                username = safe(mc.getSession().getUsername());
                if (mc.getSession().getUuidOrNull() != null) {
                    uuid = mc.getSession().getUuidOrNull().toString();
                }
            }
        } catch (Throwable ignored) {
        }

        JsonObject root = new JsonObject();
        root.addProperty("hwid", HwidUtil.get());
        root.addProperty("username", username);
        root.addProperty("uuid", uuid);
        root.addProperty("version", Info.VERSION);

        JsonArray mods = new JsonArray();
        try {
            List<Module> list = ModuleManager.getInstance().getModules();
            for (Module m : list) {
                JsonObject mo = new JsonObject();
                mo.addProperty("id", m.getId());
                mo.addProperty("name", m.getRawName());
                mo.addProperty("category", m.getCategory().name());
                mods.add(mo);
            }
        } catch (Throwable ignored) {
        }
        root.add("modules", mods);
        appendServerIcon(root, mc);
        EmotionWheelManager.getInstance().appendSync(root);
        CosmeticCatalog.appendSync(root);
        GraffitiManager.getInstance().appendSync(root);
        return root.toString();
    }

    private static void appendServerIcon(JsonObject root, MinecraftClient mc) {
        try {
            Object server = mc == null ? null : mc.getCurrentServerEntry();
            if (server == null) return;
            Object favicon = server.getClass().getMethod("getFavicon").invoke(server);
            if (!(favicon instanceof byte[] bytes) || bytes.length == 0 || bytes.length > 196_608) return;
            root.addProperty("server_icon", "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(bytes));
        } catch (ReflectiveOperationException ignored) {
            // Older mappings/builds do not expose a favicon accessor.
        } catch (Throwable error) {
            LOGGER.debug("server icon unavailable: {}", error.toString());
        }
    }

    private void apply(JsonObject o) {
        banned = bool(o, "banned");
        banReason = str(o, "ban_reason", "");
        blockClient = bool(o, "block_client");
        blockMessage = str(o, "block_message", "");
        blockUrl = str(o, "block_url", "https://t.me/enoughvisuals");
        updateRequired = bool(o, "update_required");
        latestVersion = str(o, "latest_version", Info.VERSION);
        updateMessage = str(o, "update_message", "");
        updateUrl = str(o, "update_url", "https://t.me/enoughvisuals");
        launcherRole = normalizeRole(str(o, "role", "User"));

        List<AutoServer> servers = new ArrayList<>();
        JsonElement serverArray = o.get("auto_servers");
        if (serverArray != null && serverArray.isJsonArray()) {
            for (JsonElement entry : serverArray.getAsJsonArray()) {
                try {
                    JsonObject server = entry.getAsJsonObject();
                    String name = str(server, "name", "").trim();
                    String address = str(server, "address", "").trim();
                    if (!name.isEmpty() && !address.isEmpty()) {
                        servers.add(new AutoServer(name, address));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (!servers.equals(autoServers)) {
            autoServers = List.copyOf(servers);
            AutoServerSync.apply(autoServers);
        }

        Set<String> set = new HashSet<>();
        JsonElement arr = o.get("blocked_modules");
        if (arr != null && arr.isJsonArray()) {
            for (JsonElement e : arr.getAsJsonArray()) {
                try {
                    set.add(e.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        boolean changed = !set.equals(blockedModules);
        blockedModules = Collections.unmodifiableSet(set);

        Set<String> evUsers = new HashSet<>();
        JsonElement evArr = o.get("enough_users");
        if (evArr != null && evArr.isJsonArray()) {
            for (JsonElement e : evArr.getAsJsonArray()) {
                try {
                    evUsers.add(e.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        enoughUsers = Collections.unmodifiableSet(evUsers);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) client.execute(() -> {
            EmotionWheelManager.getInstance().applySync(o);
            CosmeticCatalog.applySync(o);
            GraffitiManager.getInstance().applySync(o);
        });
        if (changed) {
            enforceBlocksAsync();
        }
    }

    /**
     * Immediately disables any currently-enabled module that just became blocked,
     * on the client thread, so a block takes effect at once (no relog needed).
     */
    private void enforceBlocksAsync() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            try {
                for (Module m : ModuleManager.getInstance().getModules()) {
                    if (m.isEnabled() && blockedModules.contains(m.getId())) {
                        m.disableSilent();
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }

    // ---- accessors ----

    public boolean isModuleBlocked(String id) {
        return blockedModules.contains(id);
    }

    public boolean isClientBlocked() {
        return blockClient;
    }

    public boolean isBanned() {
        return banned;
    }

    public boolean needsUpdate() {
        return updateRequired;
    }

    /** True when a gate (ban / block / update) should be shown to the player. */
    public boolean isGated() {
        return banned || blockClient || updateRequired;
    }

    public String getBlockMessage() {
        return blockMessage;
    }

    public String getBlockUrl() {
        return blockUrl;
    }

    public String getBanReason() {
        return banReason;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

    public boolean hasEverSynced() {
        return everSynced;
    }

    public Set<String> getEnoughUsers() {
        return enoughUsers;
    }

    public List<AutoServer> getAutoServers() {
        return autoServers;
    }

    public String getLauncherRole() {
        return launcherRole;
    }

    public boolean isEnoughUser(String username) {
        return username != null && enoughUsers.contains(username);
    }

    private static boolean bool(JsonObject o, String k) {
        try {
            return o.has(k) && o.get(k).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private static String str(JsonObject o, String k, String def) {
        try {
            if (o.has(k) && !o.get(k).isJsonNull()) {
                return o.get(k).getAsString();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String normalizeRole(String value) {
        if ("Admin".equalsIgnoreCase(value)) return "Admin";
        if ("Beta".equalsIgnoreCase(value)) return "Beta";
        return "User";
    }
}
