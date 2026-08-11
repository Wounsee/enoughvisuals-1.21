package fun.enoughvisuals.systems.graffiti;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable resource-backed catalog produced by {@code scripts/import_graffiti.py}. */
public final class GraffitiCatalog {
    private static final String RESOURCE = "/assets/enough/graffiti/catalog.json";
    private static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_ID;

    static {
        List<Entry> entries = new ArrayList<>();
        Map<String, Entry> byId = new LinkedHashMap<>();
        try (InputStream stream = GraffitiCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray values = root.getAsJsonArray("entries");
            for (JsonElement value : values) {
                JsonObject item = value.getAsJsonObject();
                String id = item.get("id").getAsString();
                String texturePath = item.get("texture").getAsString();
                Entry entry = new Entry(
                        id,
                        item.get("title").getAsString(),
                        Identifier.of("enough", texturePath),
                        Math.max(1, item.get("frames").getAsInt()),
                        Math.max(0f, item.get("fps").getAsFloat()),
                        Math.max(1, item.get("columns").getAsInt()),
                        Math.max(1, item.get("rows").getAsInt()),
                        Math.max(0.05f, item.get("aspect").getAsFloat())
                );
                if (byId.putIfAbsent(id, entry) == null) entries.add(entry);
            }
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
        ENTRIES = Collections.unmodifiableList(entries);
        BY_ID = Collections.unmodifiableMap(byId);
    }

    private GraffitiCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static Entry find(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public record Entry(String id, String title, Identifier texture, int frames, float fps,
                        int columns, int rows, float aspect) {
        public boolean animated() {
            return frames > 1 && fps > 0f;
        }

        public int frame(long elapsedMillis) {
            if (!animated()) return 0;
            long index = (long) Math.floor(elapsedMillis * fps / 1000.0);
            return Math.floorMod(index, frames);
        }

        public float u(int frame) {
            return (frame % columns) / (float) columns;
        }

        public float v(int frame) {
            return (frame / columns) / (float) rows;
        }

        public float uSize() {
            return 1f / columns;
        }

        public float vSize() {
            return 1f / rows;
        }
    }
}
