package fun.enoughvisuals.systems.emotions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stable identifiers for the complete PulseVisuals emote pack.
 *
 * <p>The resource names deliberately do not depend on the source directory's
 * locale or on Pulse's internal animation name. They are safe to use as wire
 * identifiers and as cache keys.</p>
 */
public final class PulseEmotionCatalog {
    private static final String RESOURCE_ROOT = "assets/enough/emotions/pulse/";

    private static final List<Entry> ENTRIES = List.of(
            entry("orange_justice", "2017"),
            entry("67", "67"),
            entry("floss", "Flexin"),
            entry("gangnam_style", "Гангнам Стайл"),
            entry("squat_kick", "Гопак"),
            entry("get_griddy", "Гридди"),
            entry("nowkie_bake", "Новки"),
            entry("crying", "Плачь"),
            entry("hype", "Победный Танец"),
            entry("scuba", "Скуба"),
            entry("twerk", "Твёрк"),
            entry("topless", "Топлес"),
            entry("l_dance", "Фортнайт")
    );

    private static final Map<String, Entry> BY_ID;

    static {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            if (entries.put(entry.id(), entry) != null) {
                throw new IllegalStateException("Duplicate Pulse emote id: " + entry.id());
            }
        }
        BY_ID = Map.copyOf(entries);
    }

    private PulseEmotionCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Optional<Entry> find(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    private static Entry entry(String id, String title) {
        return new Entry(id, title, RESOURCE_ROOT + id + "/model.json");
    }

    public record Entry(String id, String title, String resourcePath) {
        public Entry {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
            if (resourcePath == null || resourcePath.isBlank()) {
                throw new IllegalArgumentException("resourcePath must not be blank");
            }
        }
    }
}
