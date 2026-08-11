package fun.enoughvisuals.systems.events;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/** Pure normalization helpers shared by the live feed and its tests. */
public final class EventFeedAlgorithms {
    static final long DEADLINE_DRIFT_TOLERANCE_MS = 3_000L;

    private EventFeedAlgorithms() {
    }

    public static String stableKey(String provider, String serverType, int serverId,
                                   String eventId, String kind) {
        return normalize(provider) + ':' + normalize(serverType) + ':' + serverId + ':'
                + normalize(eventId) + ':' + normalize(kind);
    }

    public static <T> List<T> deduplicate(List<T> values, Function<T, String> key,
                                          BinaryOperator<T> merge) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) result.merge(key.apply(value), value, merge);
        return List.copyOf(result.values());
    }

    public static int currentServerRank(String currentProvider, String currentType, int currentId,
                                        String itemProvider, String itemType, int itemId) {
        if (currentId <= 0 || itemId <= 0 || currentId != itemId) return 1;
        if (!normalize(currentProvider).equals(normalize(itemProvider))) return 1;
        String current = normalize(currentType);
        String item = normalize(itemType);
        return current.isBlank() || item.isBlank() || current.equals(item)
                || current.contains(item) || item.contains(current) ? 0 : 1;
    }

    public static long resolveDeadline(long previousDeadline, long previousUpdatedAt,
                                       long incomingUpdatedAt, long receivedAt, int secondsLeft) {
        if (secondsLeft < 0) return -1L;
        long candidate = receivedAt + Math.max(0, secondsLeft) * 1_000L;
        if (previousDeadline <= 0L || incomingUpdatedAt > previousUpdatedAt) return candidate;
        return Math.abs(candidate - previousDeadline) >= DEADLINE_DRIFT_TOLERANCE_MS
                ? candidate : previousDeadline;
    }

    public static int remainingSeconds(long deadlineEpochMillis, long nowEpochMillis) {
        if (deadlineEpochMillis < 0L) return -1;
        long remaining = deadlineEpochMillis - nowEpochMillis;
        if (remaining <= 0L) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (remaining + 999L) / 1_000L);
    }

    public static long parseUpdatedAt(String value, long fallback) {
        if (value == null || value.isBlank()) return fallback;
        String text = value.trim();
        try {
            long numeric = Long.parseLong(text);
            return numeric < 10_000_000_000L ? numeric * 1_000L : numeric;
        } catch (NumberFormatException ignored) {
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(text).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    public static String connectCommand(EventFeedService.Source source, String serverType, int serverId) {
        if (source == EventFeedService.Source.FUNTIME) {
            return serverId > 0 ? "an" + serverId : "";
        }
        String normalized = normalize(serverType);
        if (normalized.equals("lite_anarchy") || normalized.equals("lite_anarchy_" + serverId)) {
            return serverId > 0 ? "lite" + serverId : "";
        }
        if (normalized.equals("lite_new_anarchy") || normalized.equals("lite_new_anarchy_" + serverId)) {
            return serverId > 0 ? "newlite" + serverId : "";
        }
        if (normalized.startsWith("lite_new_anarchy_")) {
            return "newlite" + trailingNumber(normalized);
        }
        if (normalized.startsWith("lite_anarchy_")) {
            return "lite" + trailingNumber(normalized);
        }
        return "";
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_').replaceAll("_+", "_");
    }

    private static String trailingNumber(String value) {
        int index = value.length() - 1;
        while (index >= 0 && Character.isDigit(value.charAt(index))) index--;
        return index + 1 < value.length() ? value.substring(index + 1) : "";
    }
}
