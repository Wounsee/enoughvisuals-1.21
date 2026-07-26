package fun.enoughvisuals.systems.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fun.enoughvisuals.Info;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.player.ServerUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HolyWorld LiteAPI feature-control client.
 *
 * Feature names sent to the server are stable EnoughVisuals module ids,
 * {@code category:<id>} for whole ClickGUI categories and {@code tool:<id>}
 * for integrated tools.
 */
public final class HolyWorldFeatureControl {
    public static final String INVENTORY_MANAGER = "tool:inventory_manager";

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/HolyWorld");
    private static final Gson GSON = new Gson();
    private static final String CLIENT_ID = Info.MOD_ID;
    private static final HolyWorldFeatureControl INSTANCE = new HolyWorldFeatureControl();
    private static final Set<String> TOOLS = Set.of(
            "tool:configs", "tool:friends", "tool:gps", "tool:macros", INVENTORY_MANAGER);

    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
    private volatile Set<String> serverBlocklist = Set.of();
    private volatile boolean connected;

    private HolyWorldFeatureControl() {
    }

    public static HolyWorldFeatureControl getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        PayloadTypeRegistry.playC2S().register(JsonPayload.ID, JsonPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JsonPayload.ID, JsonPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(JsonPayload.ID,
                (payload, context) -> context.client().execute(() -> handle(payload.json())));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connected = true;
            serverBlocklist = Set.of();
            pendingRequests.clear();
            sendCheckFeatures();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            connected = false;
            serverBlocklist = Set.of();
            pendingRequests.clear();
        });
    }

    public boolean isModuleBlocked(Module module) {
        return module != null && (isBlocked(module.getId()) || isCategoryBlocked(module.getCategory()));
    }

    public boolean isCategoryBlocked(Category category) {
        if (category == null) return false;
        String id = category.name().toLowerCase(Locale.ROOT);
        return isBlocked("category:" + id);
    }

    public boolean isToolBlocked(String toolId) {
        if (INVENTORY_MANAGER.equals(toolId) && ServerUtil.isHolyWorldServer()) return true;
        return isBlocked(toolId);
    }

    public boolean isBlocked(String featureId) {
        if (featureId == null || featureId.isBlank()) return false;
        return serverBlocklist.contains(featureId.toLowerCase(Locale.ROOT));
    }

    private void sendCheckFeatures() {
        if (!connected || !ClientPlayNetworking.canSend(JsonPayload.ID)) {
            LOGGER.debug("HolyWorld LiteAPI channel is unavailable on this server");
            return;
        }

        String requestId = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject();
        payload.addProperty("client", CLIENT_ID);
        JsonArray features = new JsonArray();
        allFeatureIds().forEach(features::add);
        payload.add("features", features);

        JsonObject request = new JsonObject();
        request.addProperty("id", requestId);
        request.addProperty("method", "checkFeatures");
        request.add("payload", payload);
        pendingRequests.add(requestId);
        ClientPlayNetworking.send(new JsonPayload(GSON.toJson(request)));
    }

    private Collection<String> allFeatureIds() {
        List<String> result = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getAllModules()) result.add(module.getId());
        for (Category category : Category.values()) {
            result.add("category:" + category.name().toLowerCase(Locale.ROOT));
        }
        result.addAll(TOOLS);
        return result;
    }

    private void handle(String rawJson) {
        try {
            JsonObject response = GSON.fromJson(rawJson, JsonObject.class);
            if (response == null || !response.has("id")) return; // Push event from another method.
            String id = response.get("id").getAsString();
            if (!pendingRequests.remove(id)) return;
            if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
                String error = response.has("error") ? response.get("error").getAsString() : "UNKNOWN";
                LOGGER.warn("HolyWorld checkFeatures failed: {}", error);
                return;
            }

            Set<String> blocked = new HashSet<>();
            JsonObject payload = response.getAsJsonObject("payload");
            JsonArray blocklist = payload == null ? null : payload.getAsJsonArray("blocklist");
            if (blocklist != null) {
                for (JsonElement feature : blocklist) {
                    if (feature.isJsonPrimitive()) {
                        blocked.add(feature.getAsString().toLowerCase(Locale.ROOT));
                    }
                }
            }
            serverBlocklist = Set.copyOf(blocked);
            ModuleManager.getInstance().applyFeatureRestrictions();
            if (MinecraftClient.getInstance().currentScreen instanceof MenuScreen) {
                MinecraftClient.getInstance().setScreen(new MenuScreen());
            }
            LOGGER.info("HolyWorld LiteAPI applied {} blocked features", blocked.size());
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid HolyWorld LiteAPI response", exception);
        }
    }

    public record JsonPayload(String json) implements CustomPayload {
        public static final Id<JsonPayload> ID =
                new Id<>(Identifier.of("liteapi", "feature-control"));
        public static final PacketCodec<RegistryByteBuf, JsonPayload> CODEC =
                PacketCodec.of(JsonPayload::write, JsonPayload::read);

        private static void write(JsonPayload payload, RegistryByteBuf buf) {
            byte[] bytes = payload.json.getBytes(StandardCharsets.UTF_8);
            buf.writeBytes(bytes);
        }

        private static JsonPayload read(RegistryByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new JsonPayload(new String(bytes, StandardCharsets.UTF_8));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
