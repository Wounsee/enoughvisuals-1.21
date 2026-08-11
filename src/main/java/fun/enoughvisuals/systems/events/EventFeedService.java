package fun.enoughvisuals.systems.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.hud.HudNotificationCenter;
import fun.enoughvisuals.systems.network.LauncherNetworkClient;
import fun.enoughvisuals.utils.player.ServerUtil;
import net.funtimeevents.api.FteConfig;
import net.funtimeevents.api.FunTimeEventsAPI;
import net.funtimeevents.model.EventCoordinates;
import net.funtimeevents.model.EventResponse;
import net.funtimeevents.model.MineResponse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalized event feed for the ClickGUI and HUD notifications. */
public final class EventFeedService {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Events");
    private static final EventFeedService INSTANCE = new EventFeedService();
    private static final URI HOLYWORLD_EVENTS = URI.create("https://api.holyworld.me/v1/events");
    private static final URI HOLYWORLD_SERVERS = URI.create("https://api.holyworld.me/v1/servers");
    private static final long HOLYWORLD_REFRESH_MS = 12_000L;
    private static final long CONNECT_DEBOUNCE_MS = 1_200L;
    private static final Pattern FUNTIME_SHARD = Pattern.compile(
            "(?iu)(?:анархия|anarchy)\\s*[-—–:#№]?\\s*(\\d{1,4})");
    private static final Pattern HOLY_NEW_LITE = Pattern.compile(
            "(?iu)(?:lite[_\\s-]*new[_\\s-]*anarchy|new[_\\s-]*lite|новая\\s+лайт)[_\\s—–:#№-]*(\\d{1,4})");
    private static final Pattern HOLY_LITE = Pattern.compile(
            "(?iu)(?:lite[_\\s-]*anarchy|lite|лайт(?:\\s+анархия)?)[_\\s—–:#№-]*(\\d{1,4})");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d{1,4})$");
    private static final Pattern FUNTIME_DELAY = Pattern.compile(
            "(?iu)до\\s+следующего\\s+ивента\\s*:?\\s*(\\d{1,3})\\s*мин(?:ут(?:а|ы)?)?\\s*(\\d{1,2})\\s*сек(?:унд(?:а|ы)?)?");
    private static final Pattern FUNTIME_COORDINATES = Pattern.compile(
            "(?iu)(?:x\\s*[:=]?\\s*)?(-?\\d{1,7})\\s*(?:[,;|]|\\s)\\s*"
                    + "(?:y\\s*[:=]?\\s*)?(-?\\d{1,4})\\s*(?:[,;|]|\\s)\\s*"
                    + "(?:z\\s*[:=]?\\s*)?(-?\\d{1,7})");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final AtomicBoolean holyRequestActive = new AtomicBoolean();
    private final AtomicBoolean funtimeStarting = new AtomicBoolean();
    private final Set<String> announced = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, DeadlineState> deadlines = new HashMap<>();

    private volatile List<EventItem> items = List.of();
    private volatile Status status = Status.IDLE;
    private volatile String statusText = "Зайдите на FunTime или HolyWorld";
    private volatile long lastHolyRefresh;
    private volatile boolean funtimeStarted;
    private volatile boolean snapshotPrimed;
    private volatile String scopeHost = "";
    private volatile int tickCounter;
    private volatile long lastExpiryRefresh;
    private volatile long lastConnectAt;
    private volatile long lastChatFallbackAt;

    private EventFeedService() {
    }

    public static EventFeedService getInstance() {
        return INSTANCE;
    }

    public void onClientTick(MinecraftClient client) {
        String host = ServerUtil.getCurrentServerHost();
        if (!host.equals(scopeHost)) {
            scopeHost = host;
            snapshotPrimed = false;
            announced.clear();
            deadlines.clear();
            items = List.of();
        }
        if (client == null || client.world == null || client.player == null) {
            status = Status.IDLE;
            statusText = "Зайдите на FunTime или HolyWorld";
            items = List.of();
            announced.clear();
            deadlines.clear();
            snapshotPrimed = false;
            return;
        }
        long now = System.currentTimeMillis();
        boolean expired = items.stream().anyMatch(item -> item.deadlineEpochMillis() > 0L
                && item.deadlineEpochMillis() <= now);
        if (expired && now - lastExpiryRefresh > 1_500L) {
            lastExpiryRefresh = now;
            refreshNow();
        }
        if (ServerUtil.isFunTimeEventsServer()) {
            ensureFuntimeStarted();
            if (funtimeStarted && ++tickCounter % 10 == 0) refreshFuntimeSnapshot();
            return;
        }
        if (ServerUtil.isHolyWorldEventsServer()) {
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

    public Status status() {
        return status;
    }

    public String statusText() {
        return statusText;
    }

    public String currentProviderName() {
        if (ServerUtil.isFunTimeEventsServer()) return "FunTime";
        if (ServerUtil.isHolyWorldEventsServer()) return "HolyWorld";
        return "—";
    }


    /**
     * Chat fallback used when the FunTime relay/API does not deliver snapshots.
     * The server itself prints a stable countdown, so the Events page remains
     * useful even during an SDK outage.
     */
    public synchronized void onChatLine(String line) {
        if (line == null || line.isBlank() || !ServerUtil.isFunTimeEventsServer()) return;
        String normalized = line.replace("\n", " ").replaceAll("\\s+", " ").trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        ServerContext context = detectCurrentContext(MinecraftClient.getInstance());
        int serverId = context.serverId();
        String serverType = context.serverType().isBlank() ? "ANARCHY" : context.serverType();

        Matcher delay = FUNTIME_DELAY.matcher(lower);
        if (delay.find()) {
            int minutes = parseInt(delay.group(1));
            int seconds = parseInt(delay.group(2));
            if (minutes < 0) minutes = 0;
            if (seconds < 0) seconds = 0;
            int total = minutes * 60 + seconds;
            if (total > 0) {
                String key = EventFeedAlgorithms.stableKey(
                        "funtime-chat", serverType, serverId, "next-event", Kind.UPCOMING.name());
                EventItem fallback = new EventItem(
                        key, Source.FUNTIME, Kind.UPCOMING, serverType, serverId,
                        "next-event", serverName(serverType, serverId), "Следующий ивент",
                        "Данные получены из чата FunTime", "", now, now + total * 1_000L,
                        null, null, null);
                lastChatFallbackAt = now;
                publish(List.of(fallback), "События получены из чата FunTime");
                return;
            }
        }

        if ((lower.contains("появил") || lower.contains("координат"))) {
            Matcher coordinates = FUNTIME_COORDINATES.matcher(normalized);
            if (coordinates.find()) {
                int x = parseInt(coordinates.group(1));
                int y = parseInt(coordinates.group(2));
                int z = parseInt(coordinates.group(3));
                String key = EventFeedAlgorithms.stableKey(
                        "funtime-chat", serverType, serverId, "active-event", Kind.CURRENT.name());
                EventItem fallback = new EventItem(
                        key, Source.FUNTIME, Kind.CURRENT, serverType, serverId,
                        "active-event", serverName(serverType, serverId), "Активный ивент",
                        "Координаты получены из чата FunTime", "", now, -1L, x, y, z);
                lastChatFallbackAt = now;
                publish(List.of(fallback), "Активный ивент получен из чата FunTime");
            }
        }
    }

    public void refreshNow() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            status = Status.IDLE;
            statusText = "Сначала подключитесь к серверу";
            return;
        }
        if (ServerUtil.isFunTimeEventsServer()) {
            if (funtimeStarted) refreshFuntimeSnapshot();
            else ensureFuntimeStarted();
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

    public EventAction actionFor(EventItem item) {
        if (item == null) return EventAction.NONE;
        ServerContext context = detectCurrentContext(MinecraftClient.getInstance());
        boolean sameProvider = context.source() == item.source();
        boolean sameServer = sameProvider && context.serverId() > 0 && item.serverId() > 0
                && context.serverId() == item.serverId()
                && compatibleServerTypes(context.serverType(), item.serverType());
        if (sameServer) return item.hasCoordinates() ? EventAction.WAYPOINT : EventAction.CURRENT;
        String command = EventFeedAlgorithms.connectCommand(item.source(), item.serverType(), item.serverId());
        return sameProvider && !command.isBlank() ? EventAction.CONNECT : EventAction.NONE;
    }

    public boolean performAction(EventItem item) {
        EventAction action = actionFor(item);
        if (action == EventAction.WAYPOINT) {
            addWaypoint(item);
            return true;
        }
        if (action != EventAction.CONNECT) return false;
        long now = System.currentTimeMillis();
        if (now - lastConnectAt < CONNECT_DEBOUNCE_MS) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) return false;
        String command = EventFeedAlgorithms.connectCommand(item.source(), item.serverType(), item.serverId());
        if (command.isBlank()) return false;
        lastConnectAt = now;
        client.getNetworkHandler().sendChatCommand(command);
        client.setScreen(null);
        return true;
    }

    public void addWaypoint(EventItem item) {
        if (item == null || item.x() == null || item.z() == null) return;
        int y = item.y() == null ? 64 : item.y();
        GpsSystem.getInstance().addWaypoint("Ивент: " + item.name(), item.x(), y, item.z(), true);
        HudNotificationCenter.info("Метка " + item.name());
    }

    private void ensureFuntimeStarted() {
        if (funtimeStarted || !funtimeStarting.compareAndSet(false, true)) return;
        status = Status.LOADING;
        statusText = "Подключаем FunTime Events";
        CompletableFuture.runAsync(() -> {
            try {
                String key = LauncherNetworkClient.funtimeEventsApiKey(ServerUtil.getCurrentServerHost());
                FunTimeEventsAPI.builder()
                        .userAgent("EnoughVisuals/" + fun.enoughvisuals.Info.VERSION)
                        .apiKey(key)
                        .baseUrl(LauncherNetworkClient.FUNTIME_EVENTS_BASE_URL)
                        .logLevel(FteConfig.LogLevel.WARN)
                        .disableWebSocket()
                        .build();
                funtimeStarted = true;
                status = Status.READY;
                statusText = "События обновляются автоматически";
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
        long receivedAt = System.currentTimeMillis();
        List<EventItem> next = new ArrayList<>();
        try {
            for (EventResponse event : FunTimeEventsAPI.getEvents()) next.add(fromFuntime(event, receivedAt));
            for (EventResponse event : FunTimeEventsAPI.getUserEvents()) next.add(fromFuntime(event, receivedAt));
            for (MineResponse mine : FunTimeEventsAPI.getMines()) next.add(fromFuntime(mine, receivedAt));
            publish(next, "События обновляются автоматически");
        } catch (Throwable error) {
            boolean chatFallbackFresh = System.currentTimeMillis() - lastChatFallbackAt < 120_000L;
            status = items.isEmpty() && !chatFallbackFresh ? Status.ERROR : Status.READY;
            statusText = items.isEmpty() && !chatFallbackFresh
                    ? "Не удалось получить события FunTime"
                    : chatFallbackFresh ? "Показаны данные из чата FunTime" : "Показаны последние данные";
            LOGGER.debug("FunTime Events snapshot failed: {}", error.toString());
        }
    }

    private EventItem fromFuntime(EventResponse event, long receivedAt) {
        String statusValue = safe(event.status()).toLowerCase(Locale.ROOT);
        String phase = safe(event.phase()).toLowerCase(Locale.ROOT);
        Kind kind = statusValue.contains("active") || statusValue.contains("started")
                || phase.contains("active") || phase.contains("spawn") ? Kind.CURRENT : Kind.UPCOMING;
        String eventId = firstNonBlank(event.eventId(), event.eventType(), event.name(), "event");
        String key = EventFeedAlgorithms.stableKey("funtime", event.serverType(), event.serverId(), eventId, kind.name());
        long updatedAt = EventFeedAlgorithms.parseUpdatedAt(event.updatedAt(), receivedAt);
        long deadline = resolveDeadline(key, updatedAt, receivedAt, event.timeLeft());
        EventCoordinates pos = event.coordinates();
        String name = firstNonBlank(event.name(), event.eventId(), event.eventType(), "Ивент");
        String subtitle = firstNonBlank(event.message(), humanize(event.phase()), humanize(event.status()), "Событие FunTime");
        return new EventItem(key, Source.FUNTIME, kind, normalizeServerType(event.serverType()), event.serverId(),
                eventId, serverName(event.serverType(), event.serverId()), name, subtitle,
                displayRarity(event.level()), updatedAt, deadline,
                pos == null ? null : pos.x(), pos == null ? null : pos.y(), pos == null ? null : pos.z());
    }

    private EventItem fromFuntime(MineResponse mine, long receivedAt) {
        String eventId = "mine:" + safe(mine.rarity());
        String key = EventFeedAlgorithms.stableKey("funtime", mine.serverType(), mine.serverId(), eventId, Kind.MINE.name());
        long updatedAt = EventFeedAlgorithms.parseUpdatedAt(mine.updatedAt(), receivedAt);
        long deadline = resolveDeadline(key, updatedAt, receivedAt, mine.timeLeft());
        return new EventItem(key, Source.FUNTIME, Kind.MINE, normalizeServerType(mine.serverType()), mine.serverId(),
                eventId, serverName(mine.serverType(), mine.serverId()), "Автошахта", "Шахта активна",
                displayRarity(mine.rarity()), updatedAt, deadline, null, null, null);
    }

    private synchronized long resolveDeadline(String key, long updatedAt, long receivedAt, int secondsLeft) {
        DeadlineState previous = deadlines.get(key);
        long deadline = EventFeedAlgorithms.resolveDeadline(
                previous == null ? -1L : previous.deadline(),
                previous == null ? -1L : previous.updatedAt(),
                updatedAt, receivedAt, secondsLeft);
        deadlines.put(key, new DeadlineState(updatedAt, deadline));
        return deadline;
    }

    private void refreshHolyWorld() {
        if (!holyRequestActive.compareAndSet(false, true)) return;
        lastHolyRefresh = System.currentTimeMillis();
        status = items.isEmpty() ? Status.LOADING : Status.READY;
        statusText = "Обновляем события HolyWorld";
        http.sendAsync(request(HOLYWORLD_SERVERS), HttpResponse.BodyHandlers.ofString())
                .thenCombine(http.sendAsync(request(HOLYWORLD_EVENTS), HttpResponse.BodyHandlers.ofString()), (servers, events) -> {
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
        Map<String, String> servers = parseHolyServers(serversJson);
        List<EventItem> result = new ArrayList<>();
        JsonElement root = JsonParser.parseString(eventsJson);
        if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> serverEntry : root.getAsJsonObject().entrySet()) {
                if (!serverEntry.getValue().isJsonArray()) continue;
                appendHolyEvents(result, serverEntry.getKey(),
                        servers.getOrDefault(serverEntry.getKey(), humanize(serverEntry.getKey())),
                        serverEntry.getValue().getAsJsonArray());
            }
        } else if (root.isJsonArray()) {
            appendHolyEvents(result, "holyworld", "HolyWorld", root.getAsJsonArray());
        }
        return result;
    }

    private Map<String, String> parseHolyServers(String serversJson) {
        Map<String, String> servers = new HashMap<>();
        JsonElement root = JsonParser.parseString(serversJson);
        if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) servers.put(entry.getKey(), entry.getValue().getAsString());
                else if (entry.getValue().isJsonObject()) {
                    servers.put(entry.getKey(), string(entry.getValue().getAsJsonObject(), "displayName", humanize(entry.getKey())));
                }
            }
        } else if (root.isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject server = element.getAsJsonObject();
                String id = string(server, "id", string(server, "name", ""));
                if (!id.isBlank()) servers.put(id, string(server, "displayName", humanize(id)));
            }
        }
        return servers;
    }

    private void appendHolyEvents(List<EventItem> result, String rawServerId, String displayName, JsonArray events) {
        ServerDescriptor descriptor = ServerDescriptor.from(rawServerId);
        long now = System.currentTimeMillis();
        for (JsonElement element : events) {
            if (!element.isJsonObject()) continue;
            JsonObject event = element.getAsJsonObject();
            JsonObject metadata = event.has("metadata") && event.get("metadata").isJsonObject()
                    ? event.getAsJsonObject("metadata") : new JsonObject();
            String id = string(event, "instanceId", string(event, "id", "event"));
            String name = string(metadata, "displayName", humanize(string(event, "id", "Ивент")));
            String key = EventFeedAlgorithms.stableKey("holyworld", descriptor.type(), descriptor.id(), id, Kind.CURRENT.name());
            result.add(new EventItem(key, Source.HOLYWORLD, Kind.CURRENT, descriptor.type(), descriptor.id(), id,
                    displayName, name, "Событие уже активно", displayRarity(string(metadata, "rare", "")),
                    now, -1L, null, null, null));
        }
    }

    private synchronized void publish(List<EventItem> incoming, String readyText) {
        ServerContext context = detectCurrentContext(MinecraftClient.getInstance());
        List<EventItem> next = new ArrayList<>(EventFeedAlgorithms.deduplicate(
                incoming, EventItem::key, EventFeedService::mergeItems));
        next.sort(Comparator
                .comparingInt((EventItem item) -> EventFeedAlgorithms.currentServerRank(
                        context.source() == null ? "" : context.source().name(), context.serverType(), context.serverId(),
                        item.source().name(), item.serverType(), item.serverId()))
                .thenComparingInt(item -> kindPriority(item.kind()))
                .thenComparingLong(item -> item.deadlineEpochMillis() < 0L ? Long.MAX_VALUE : item.deadlineEpochMillis())
                .thenComparingInt(EventItem::serverId)
                .thenComparing(EventItem::name));
        items = List.copyOf(next);
        status = Status.READY;
        statusText = next.isEmpty() ? "Активных событий пока нет" : readyText;
        for (EventItem item : next) {
            boolean fresh = announced.add(item.key());
            if (snapshotPrimed && fresh) HudNotificationCenter.info(item.name() + " · " + item.server());
        }
        Set<String> live = next.stream().map(EventItem::key).collect(java.util.stream.Collectors.toSet());
        announced.retainAll(live);
        deadlines.keySet().retainAll(live);
        snapshotPrimed = true;
    }

    private static EventItem mergeItems(EventItem first, EventItem second) {
        EventItem newer = second.apiUpdatedAtEpochMillis() >= first.apiUpdatedAtEpochMillis() ? second : first;
        EventItem other = newer == second ? first : second;
        Integer x = newer.x() != null ? newer.x() : other.x();
        Integer y = newer.y() != null ? newer.y() : other.y();
        Integer z = newer.z() != null ? newer.z() : other.z();
        return new EventItem(newer.key(), newer.source(), newer.kind(), newer.serverType(), newer.serverId(),
                newer.eventId(), newer.server(), newer.name(), newer.subtitle(), newer.rarity(),
                newer.apiUpdatedAtEpochMillis(), newer.deadlineEpochMillis(), x, y, z);
    }

    private static boolean matches(ServerContext context, EventItem item) {
        return context.source() == item.source() && context.serverId() > 0 && context.serverId() == item.serverId()
                && compatibleServerTypes(context.serverType(), item.serverType());
    }

    private static boolean compatibleServerTypes(String first, String second) {
        String a = EventFeedAlgorithms.normalize(first);
        String b = EventFeedAlgorithms.normalize(second);
        return a.isBlank() || b.isBlank() || a.equals(b) || a.contains(b) || b.contains(a);
    }

    private static int kindPriority(Kind kind) {
        return switch (kind) {
            case CURRENT -> 0;
            case UPCOMING -> 1;
            case MINE -> 2;
        };
    }

    private static ServerContext detectCurrentContext(MinecraftClient client) {
        Source source = ServerUtil.isFunTimeEventsServer() ? Source.FUNTIME
                : ServerUtil.isHolyWorldEventsServer() ? Source.HOLYWORLD : null;
        if (source == null || client == null || client.world == null) return ServerContext.NONE;
        List<String> context = new ArrayList<>();
        try {
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard == null ? null
                    : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                if (objective.getDisplayName() != null) context.add(objective.getDisplayName().getString());
                for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                    if (!entry.hidden() && entry.name() != null) context.add(entry.name().getString());
                }
            }
        } catch (Throwable ignored) {
        }
        String combined = String.join(" | ", context);
        if (source == Source.FUNTIME) {
            Matcher matcher = FUNTIME_SHARD.matcher(combined);
            return matcher.find() ? new ServerContext(source, "ANARCHY", parseInt(matcher.group(1)))
                    : new ServerContext(source, "ANARCHY", -1);
        }
        Matcher newLite = HOLY_NEW_LITE.matcher(combined);
        if (newLite.find()) return new ServerContext(source, "LITE_NEW_ANARCHY", parseInt(newLite.group(1)));
        Matcher lite = HOLY_LITE.matcher(combined);
        if (lite.find()) return new ServerContext(source, "LITE_ANARCHY", parseInt(lite.group(1)));
        return new ServerContext(source, "", -1);
    }

    private static HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "EnoughVisuals")
                .GET().build();
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalizeServerType(String type) {
        String normalized = safe(type).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isBlank() ? "ANARCHY" : normalized;
    }

    private static String serverName(String type, int id) {
        String normalized = normalizeServerType(type);
        String prefix = normalized.contains("ANARCHY") ? "Анархия" : humanize(normalized);
        return id > 0 ? prefix + ' ' + id : prefix;
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

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public enum Source { FUNTIME, HOLYWORLD }
    public enum Kind { CURRENT, UPCOMING, MINE }
    public enum Status { IDLE, LOADING, READY, ERROR, UNSUPPORTED }
    public enum EventAction { NONE, CONNECT, WAYPOINT, CURRENT }

    public record EventItem(String key, Source source, Kind kind, String serverType, int serverId,
                            String eventId, String server, String name, String subtitle, String rarity,
                            long apiUpdatedAtEpochMillis, long deadlineEpochMillis,
                            Integer x, Integer y, Integer z) {
        public boolean hasCoordinates() {
            return x != null && z != null;
        }

        public int secondsLeft(long nowEpochMillis) {
            return EventFeedAlgorithms.remainingSeconds(deadlineEpochMillis, nowEpochMillis);
        }
    }

    private record DeadlineState(long updatedAt, long deadline) {
    }

    private record ServerContext(Source source, String serverType, int serverId) {
        private static final ServerContext NONE = new ServerContext(null, "", -1);
    }

    private record ServerDescriptor(String type, int id) {
        private static ServerDescriptor from(String raw) {
            String normalized = safe(raw).trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            Matcher number = TRAILING_NUMBER.matcher(normalized);
            int id = number.find() ? parseInt(number.group(1)) : -1;
            String type = id > 0 ? normalized.substring(0, number.start()).replaceAll("_+$", "") : normalized;
            return new ServerDescriptor(type, id);
        }
    }
}
