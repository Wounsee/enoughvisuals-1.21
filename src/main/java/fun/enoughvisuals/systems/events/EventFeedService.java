package fun.enoughvisuals.systems.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.utils.player.ServerUtil;
import fun.enoughvisuals.utils.system.IslandFeed;
import net.funtimeevents.api.FteConfig;
import net.funtimeevents.api.FunTimeEventsAPI;
import net.funtimeevents.model.EventCoordinates;
import net.funtimeevents.model.EventResponse;
import net.funtimeevents.model.MineResponse;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Normalized event feed for the ClickGUI and Dynamic Island. */
public final class EventFeedService {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Events");
    private static final EventFeedService INSTANCE = new EventFeedService();
    private static final URI HOLYWORLD_EVENTS = URI.create("https://api.holyworld.me/v1/events");
    private static final URI HOLYWORLD_SERVERS = URI.create("https://api.holyworld.me/v1/servers");
    private static final long HOLYWORLD_REFRESH_MS = 12_000L;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final AtomicBoolean holyRequestActive = new AtomicBoolean();
    private final AtomicBoolean funtimeStarting = new AtomicBoolean();
    private final Set<String> announced = Collections.synchronizedSet(new HashSet<>());

    private volatile List<EventItem> items = List.of();
    private volatile Status status = Status.IDLE;
    private volatile String statusText = "Зайдите на FunTime или HolyWorld";
    private volatile long lastHolyRefresh;
    private volatile boolean funtimeStarted;
    private volatile boolean snapshotPrimed;
    private volatile String scopeHost = "";
    private volatile int tickCounter;

    private EventFeedService() {}

    public static EventFeedService getInstance() {
        return INSTANCE;
    }

    public void onClientTick(MinecraftClient client) {
        String host = ServerUtil.getCurrentServerHost();
        if (!host.equals(scopeHost)) {
            scopeHost = host;
            snapshotPrimed = false;
            announced.clear();
            items = List.of();
        }
        if (client == null || client.world == null || client.player == null) {
            status = Status.IDLE;
            statusText = "Зайдите на FunTime или HolyWorld";
            items = List.of();
            announced.clear();
            snapshotPrimed = false;
            return;
        }
        if (ServerUtil.isFunTimeEventsServer()) {
            ensureFuntimeStarted();
            if (funtimeStarted && ++tickCounter % 10 == 0) refreshFuntimeSnapshot();
            return;
        }
        if (ServerUtil.isHolyWorldEventsServer()) {
            long now = System.currentTimeMillis();
            if (now - lastHolyRefresh >= HOLYWORLD_REFRESH_MS) refreshHolyWorld();
            return;
        }
        status = Status.UNSUPPORTED;
        statusText = "Ивенты доступны на FunTime и HolyWorld";
        items = List.of();
    }

    public List<EventItem> items(Kind kind) {
        return items.stream().filter(item -> item.kind() == kind).toList();
    }

    public Status status() { return status; }
    public String statusText() { return statusText; }

    public void refreshNow() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            status = Status.IDLE;
            statusText = "Сначала подключитесь к серверу";
            return;
        }
        if (ServerUtil.isFunTimeEventsServer()) {
            if (funtimeStarted) {
                refreshFuntimeSnapshot();
            } else {
                ensureFuntimeStarted();
            }
            return;
        }
        if (ServerUtil.isHolyWorldEventsServer()) {
            lastHolyRefresh = 0L;
            refreshHolyWorld();
            return;
        }
        status = Status.UNSUPPORTED;
        statusText = "Ивенты доступны на FunTime и HolyWorld";
    }

    public void addWaypoint(EventItem item) {
        if (item == null || item.x() == null || item.z() == null) return;
        int y = item.y() == null ? 64 : item.y();
        GpsSystem.getInstance().addWaypoint("Ивент: " + item.name(), item.x(), y, item.z(), true);
        IslandFeed.push("Метка: " + item.name(), 3_000L);
    }

    private void ensureFuntimeStarted() {
        if (funtimeStarted || !funtimeStarting.compareAndSet(false, true)) return;
        status = Status.LOADING;
        statusText = "Подключаем FunTime Events";
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("bridge_action", "funtime_events_lease");
                request.addProperty("server", ServerUtil.getCurrentServerHost());
                String response = launcherRequest(request.toString());
                JsonObject envelope = JsonParser.parseString(response).getAsJsonObject();
                String key = envelope.has("api_key") ? envelope.get("api_key").getAsString() : "";
                if (key.isBlank()) throw new IllegalStateException("empty FunTime Events lease");
                FunTimeEventsAPI.builder()
                        .userAgent("EnoughVisuals")
                        .apiKey(key)
                        .logLevel(FteConfig.LogLevel.WARN)
                        .build();
                funtimeStarted = true;
                status = Status.READY;
                statusText = "Данные обновляются в реальном времени";
            } catch (Throwable error) {
                status = Status.ERROR;
                statusText = "FunTime Events временно недоступен";
                LOGGER.warn("FunTime Events SDK startup failed: {}", error.toString());
            } finally {
                funtimeStarting.set(false);
            }
        });
    }

    private void refreshFuntimeSnapshot() {
        List<EventItem> next = new ArrayList<>();
        for (EventResponse event : FunTimeEventsAPI.getEvents()) next.add(fromFuntime(event));
        for (EventResponse event : FunTimeEventsAPI.getUserEvents()) next.add(fromFuntime(event));
        for (MineResponse mine : FunTimeEventsAPI.getMines()) {
            next.add(new EventItem("fte:mine:" + mine.serverId() + ':' + safe(mine.rarity()), Source.FUNTIME,
                    Kind.MINE, serverName(mine.serverType(), mine.serverId()), "Автошахта",
                    "Шахта активна", displayRarity(mine.rarity()), Math.max(0, mine.timeLeft()),
                    null, null, null));
        }
        publish(next, "Данные обновляются в реальном времени");
    }

    private EventItem fromFuntime(EventResponse event) {
        String statusValue = safe(event.status()).toLowerCase(Locale.ROOT);
        String phase = safe(event.phase()).toLowerCase(Locale.ROOT);
        Kind kind = statusValue.contains("active") || statusValue.contains("started")
                || phase.contains("active") || phase.contains("spawn") ? Kind.CURRENT : Kind.UPCOMING;
        EventCoordinates pos = event.coordinates();
        String name = firstNonBlank(event.name(), event.eventId(), event.eventType(), "Ивент");
        String subtitle = firstNonBlank(event.message(), humanize(event.phase()), humanize(event.status()), "Событие FunTime");
        return new EventItem("fte:event:" + event.serverId() + ':' + safe(event.eventId()) + ':' + name,
                Source.FUNTIME, kind, serverName(event.serverType(), event.serverId()), name, subtitle,
                displayRarity(event.level()), Math.max(0, event.timeLeft()),
                pos == null ? null : pos.x(), pos == null ? null : pos.y(), pos == null ? null : pos.z());
    }

    private void refreshHolyWorld() {
        if (!holyRequestActive.compareAndSet(false, true)) return;
        lastHolyRefresh = System.currentTimeMillis();
        status = items.isEmpty() ? Status.LOADING : Status.READY;
        statusText = "Обновляем события HolyWorld";
        HttpRequest eventsRequest = request(HOLYWORLD_EVENTS);
        HttpRequest serversRequest = request(HOLYWORLD_SERVERS);
        http.sendAsync(serversRequest, HttpResponse.BodyHandlers.ofString())
                .thenCombine(http.sendAsync(eventsRequest, HttpResponse.BodyHandlers.ofString()), (servers, events) -> {
                    if (servers.statusCode() / 100 != 2 || events.statusCode() / 100 != 2) {
                        throw new IllegalStateException("HolyWorld API HTTP error");
                    }
                    return parseHolyWorld(servers.body(), events.body());
                })
                .whenComplete((next, error) -> {
                    holyRequestActive.set(false);
                    if (error != null) {
                        status = items.isEmpty() ? Status.ERROR : Status.READY;
                        statusText = items.isEmpty() ? "HolyWorld API временно недоступен" : "Показаны последние данные";
                        LOGGER.debug("HolyWorld events refresh failed: {}", error.toString());
                        return;
                    }
                    publish(next, "События HolyWorld обновлены");
                });
    }

    private List<EventItem> parseHolyWorld(String serversJson, String eventsJson) {
        Map<String, String> servers = new HashMap<>();
        JsonObject serverObject = JsonParser.parseString(serversJson).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : serverObject.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) servers.put(entry.getKey(), entry.getValue().getAsString());
        }
        List<EventItem> result = new ArrayList<>();
        JsonElement root = JsonParser.parseString(eventsJson);
        if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> serverEntry : root.getAsJsonObject().entrySet()) {
                if (!serverEntry.getValue().isJsonArray()) continue;
                appendHolyEvents(result, serverEntry.getKey(), servers.getOrDefault(serverEntry.getKey(), humanize(serverEntry.getKey())), serverEntry.getValue().getAsJsonArray());
            }
        } else if (root.isJsonArray()) {
            appendHolyEvents(result, "holyworld", "HolyWorld", root.getAsJsonArray());
        }
        return result;
    }

    private void appendHolyEvents(List<EventItem> result, String serverId, String serverName, JsonArray events) {
        for (JsonElement element : events) {
            if (!element.isJsonObject()) continue;
            JsonObject event = element.getAsJsonObject();
            JsonObject metadata = event.has("metadata") && event.get("metadata").isJsonObject()
                    ? event.getAsJsonObject("metadata") : new JsonObject();
            String id = string(event, "instanceId", string(event, "id", "event"));
            String name = string(metadata, "displayName", humanize(string(event, "id", "Ивент")));
            String rarity = displayRarity(string(metadata, "rare", ""));
            result.add(new EventItem("holy:" + serverId + ':' + id, Source.HOLYWORLD, Kind.CURRENT,
                    serverName, name, "Событие уже активно", rarity, -1, null, null, null));
        }
    }

    private void publish(List<EventItem> next, String readyText) {
        next.sort(Comparator.comparing(EventItem::kind).thenComparing(EventItem::server));
        items = List.copyOf(next);
        status = Status.READY;
        statusText = next.isEmpty() ? "Активных событий пока нет" : readyText;
        for (EventItem item : next) {
            boolean fresh = announced.add(item.key());
            if (snapshotPrimed && fresh) IslandFeed.push(item.name() + " · " + item.server(), 4_500L);
        }
        Set<String> live = next.stream().map(EventItem::key).collect(java.util.stream.Collectors.toSet());
        announced.retainAll(live);
        snapshotPrimed = true;
    }

    private static HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "EnoughVisuals")
                .GET().build();
    }

    private static String launcherRequest(String body) throws Exception {
        Class<?> bridge = Class.forName("net.fabricmc.loader.impl.secure.SecureModStore");
        Object response = bridge.getMethod("launcherSync", String.class).invoke(null, body);
        if (!(response instanceof String text) || text.isBlank()) throw new IllegalStateException("launcher lease bridge unavailable");
        return text;
    }

    private static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String serverName(String type, int id) {
        String prefix = safe(type).isBlank() ? "Анархия" : humanize(type);
        return prefix + ' ' + id;
    }

    private static String displayRarity(String value) {
        String normalized = safe(value).replace('_', ' ').trim();
        if (normalized.isEmpty()) return "";
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "common", "обычный" -> "Обычный";
            case "rare", "редкий", "rare desert" -> "Редкий";
            case "epic", "эпический" -> "Эпический";
            case "legendary", "легендарный" -> "Легендарный";
            case "explosive" -> "Взрывной";
            default -> humanize(normalized);
        };
    }

    private static String humanize(String value) {
        String text = safe(value).replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public enum Source { FUNTIME, HOLYWORLD }
    public enum Kind { CURRENT, UPCOMING, MINE }
    public enum Status { IDLE, LOADING, READY, ERROR, UNSUPPORTED }

    public record EventItem(String key, Source source, Kind kind, String server, String name,
                            String subtitle, String rarity, int secondsLeft,
                            Integer x, Integer y, Integer z) {
        public boolean hasCoordinates() { return x != null && z != null; }
    }
}
