package fun.enoughvisuals.systems.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CosmeticCatalog {
    public enum Kind {
        MODEL("Модели"), HEAD("Головные"), WEAPON("Оружие");
        private final String title;
        Kind(String title) { this.title = title; }
        public String title() { return title; }
    }

    public record Entry(String id, String name, Kind kind, Path directory) {}

    private static final String CATALOG_VERSION = "polairis-63-v1";
    private static final Path ROOT = FabricLoader.getInstance().getGameDir().resolve("EnoughVisuals").resolve("cosmetics");
    private static final Path LEGACY_STATE = ROOT.resolve(".equipped");
    private static final Path STATE = ROOT.resolve(".equipped-slots.json");
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Identifier> PREVIEWS = new HashMap<>();
    private static final EnumMap<Kind, String> EQUIPPED = new EnumMap<>(Kind.class);
    private static final Map<UUID, String> REMOTE_SIGNATURES = new HashMap<>();
    private static final String GENERIC_PREVIEW_SHA256 =
            "22c2e8c2358322d34941d1c6df6f6155c423ac3cb5183535a2daef5197674557";
    private static final Set<String> MODEL_OVERRIDES = Set.of(
            "weapon-komachi - оружие",
            "weapon-momiji - оружие",
            "weapon-murasa - оружие",
            "weapon-capitano - оружие",
            "weapon-yorihime - оружие"
    );
    private static boolean initialized;
    private static boolean restored;

    private CosmeticCatalog() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        extractBundledCatalog();
        scan();
        readEquippedIds();
    }

    public static List<Entry> all() { initialize(); return List.copyOf(ENTRIES); }
    public static List<Entry> of(Kind kind) { return all().stream().filter(e -> e.kind() == kind).toList(); }

    public static Entry equipped() {
        initialize();
        for (Kind kind : Kind.values()) {
            Entry entry = equipped(kind);
            if (entry != null) return entry;
        }
        return null;
    }

    public static Entry equipped(Kind kind) {
        initialize();
        String id = EQUIPPED.getOrDefault(kind, "");
        return ENTRIES.stream().filter(entry -> entry.kind() == kind && entry.id().equals(id))
                .findFirst().orElse(null);
    }

    public static boolean isEquipped(Entry entry) {
        return entry != null && entry.equals(equipped(entry.kind()));
    }

    public static List<Entry> equippedEntries() {
        initialize();
        List<Entry> result = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            Entry entry = equipped(kind);
            if (entry != null) result.add(entry);
        }
        return List.copyOf(result);
    }

    public static boolean equip(Entry entry) {
        if (entry == null || !CosmeticBridge.available()) return false;
        try {
            if (entry.kind() == Kind.MODEL) EQUIPPED.remove(Kind.HEAD);
            if (entry.kind() == Kind.HEAD) EQUIPPED.remove(Kind.MODEL);
            EQUIPPED.put(entry.kind(), entry.id());
            applyLocalSelection();
            writeState();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean toggle(Entry entry) {
        if (entry == null) return false;
        return isEquipped(entry) ? unequip(entry.kind()) : equip(entry);
    }

    public static boolean unequip(Kind kind) {
        try {
            EQUIPPED.remove(kind);
            applyLocalSelection();
            writeState();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean unequip() {
        try {
            CosmeticBridge.unequip();
            Files.deleteIfExists(STATE);
            Files.deleteIfExists(LEGACY_STATE);
            EQUIPPED.clear();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public static void restoreWhenReady() {
        if (restored || MinecraftClient.getInstance().player == null) return;
        restored = true;
        if (!equippedEntries().isEmpty()) applyLocalSelection();
    }

    public static void appendSync(JsonObject request) {
        initialize();
        JsonObject cosmetics = new JsonObject();
        for (Kind kind : Kind.values()) {
            String id = EQUIPPED.getOrDefault(kind, "");
            if (!id.isBlank()) cosmetics.addProperty(kind.name().toLowerCase(Locale.ROOT), id);
        }
        request.add("cosmetics", cosmetics);
    }

    public static void applySync(JsonObject response) {
        initialize();
        JsonArray values = response.has("active_cosmetics") && response.get("active_cosmetics").isJsonArray()
                ? response.getAsJsonArray("active_cosmetics") : new JsonArray();
        Set<UUID> present = new HashSet<>();
        for (JsonElement value : values) {
            try {
                JsonObject object = value.getAsJsonObject();
                UUID uuid = UUID.fromString(object.get("uuid").getAsString());
                List<Entry> entries = new ArrayList<>();
                addRemoteEntry(entries, object, "model", Kind.MODEL);
                addRemoteEntry(entries, object, "head", Kind.HEAD);
                addRemoteEntry(entries, object, "weapon", Kind.WEAPON);
                String signature = entries.stream().map(Entry::id).sorted().reduce("", (a, b) -> a + "\n" + b);
                present.add(uuid);
                if (!signature.equals(REMOTE_SIGNATURES.get(uuid))) {
                    if (entries.isEmpty()) CosmeticBridge.clearRemote(uuid);
                    else CosmeticBridge.equipRemote(uuid, entries.stream().map(Entry::directory).toList());
                    REMOTE_SIGNATURES.put(uuid, signature);
                }
            } catch (Exception ignored) {
            }
        }
        for (UUID uuid : new ArrayList<>(REMOTE_SIGNATURES.keySet())) {
            if (!present.contains(uuid)) {
                CosmeticBridge.clearRemote(uuid);
                REMOTE_SIGNATURES.remove(uuid);
            }
        }
    }

    public static Identifier preview(Entry entry) {
        if (entry == null) return null;
        if (PREVIEWS.containsKey(entry.id())) return PREVIEWS.get(entry.id());
        try {
            NativeImage source = loadPreviewSource(entry);
            if (source == null) { PREVIEWS.put(entry.id(), null); return null; }
            NativeImage image = cropTransparentPadding(source);
            if (image != source) source.close();
            Identifier id = Identifier.of("enough", "cosmetics/preview_" + sanitize(entry.id())
                    + "_" + Integer.toUnsignedString(entry.id().hashCode(), 36));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            texture.setFilter(true, true);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            PREVIEWS.put(entry.id(), id);
            return id;
        } catch (Exception ignored) {
            PREVIEWS.put(entry.id(), null);
            return null;
        }
    }

    private static NativeImage loadPreviewSource(Entry entry) throws Exception {
        Path avatar = entry.directory().resolve("avatar.png");
        byte[] avatarBytes = Files.isRegularFile(avatar) ? Files.readAllBytes(avatar) : null;
        if (avatarBytes != null && !GENERIC_PREVIEW_SHA256.equals(sha256(avatarBytes))) {
            return NativeImage.read(new ByteArrayInputStream(avatarBytes));
        }

        return null;
    }

    private static NativeImage loadLargestEmbeddedTexture(Path directory) {
        NativeImage best = null;
        long bestScore = -1L;
        try (var paths = Files.walk(directory)) {
            for (Path model : paths.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                    .endsWith(".bbmodel")).toList()) {
                try (var reader = Files.newBufferedReader(model)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!root.has("textures") || !root.get("textures").isJsonArray()) continue;
                    for (JsonElement value : root.getAsJsonArray("textures")) {
                        if (!value.isJsonObject()) continue;
                        JsonObject texture = value.getAsJsonObject();
                        if (!texture.has("source")) continue;
                        String source = texture.get("source").getAsString();
                        int comma = source.indexOf(',');
                        if (!source.startsWith("data:image/") || comma < 0) continue;
                        NativeImage candidate = NativeImage.read(new ByteArrayInputStream(
                                Base64.getDecoder().decode(source.substring(comma + 1))));
                        long score = previewScore(candidate);
                        if (score > bestScore) {
                            if (best != null) best.close();
                            best = candidate;
                            bestScore = score;
                        } else {
                            candidate.close();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        if (best != null && bestScore < 0L) {
            best.close();
            return null;
        }
        return best;
    }

    private static long previewScore(NativeImage image) {
        Set<Integer> colors = new HashSet<>();
        int opaque = 0;
        int stepX = Math.max(1, image.getWidth() / 32);
        int stepY = Math.max(1, image.getHeight() / 32);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int color = image.getColorArgb(x, y);
                if ((color >>> 24) < 20) continue;
                opaque++;
                int quantized = ((color >> 20) & 0xF) << 8
                        | ((color >> 12) & 0xF) << 4
                        | ((color >> 4) & 0xF);
                colors.add(quantized);
            }
        }
        if (opaque < 4 || colors.size() < 4) return -1L;
        long area = (long) image.getWidth() * image.getHeight();
        return colors.size() * 100_000L + Math.min(area, 65_536L);
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(digest.length * 2);
        for (byte item : digest) value.append(String.format("%02x", item & 0xFF));
        return value.toString();
    }

    private static void scan() {
        ENTRIES.clear();
        if (!Files.isDirectory(ROOT)) return;
        try (var paths = Files.walk(ROOT)) {
            paths.filter(path -> path.getFileName().toString().equals("avatar.json"))
                    .map(Path::getParent).filter(path -> !path.equals(ROOT)).forEach(directory -> {
                        Path relative = ROOT.relativize(directory);
                        String source = relative.toString().replace('\\', '/');
                        if (source.startsWith(".")) return;
                        String lower = source.toLowerCase(Locale.ROOT);
                        String rootFolder = lower.contains("/") ? lower.substring(0, lower.indexOf('/')) : lower;
                        Kind kind = MODEL_OVERRIDES.contains(rootFolder)
                                ? Kind.MODEL
                                : lower.startsWith("head-") ? Kind.HEAD
                                : lower.startsWith("weapon-") ? Kind.WEAPON
                                : Kind.MODEL;
                        String folder = directory.getFileName().toString();
                        String name = cleanName(folder, kind);
                        ENTRIES.add(new Entry(source, name, kind, directory));
                    });
        } catch (Exception ignored) {}
        ENTRIES.sort(Comparator.comparing(Entry::kind).thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
    }

    private static String cleanName(String raw, Kind kind) {
        String value = raw.replace('_', ' ').trim();
        if (kind == Kind.HEAD && value.toLowerCase(Locale.ROOT).startsWith("head-")) value = value.substring(5);
        if (kind == Kind.WEAPON && value.toLowerCase(Locale.ROOT).startsWith("weapon-")) value = value.substring(7);
        int note = value.indexOf(" - ");
        return note > 0 ? value.substring(0, note) : value;
    }

    private static void extractBundledCatalog() {
        Path marker = ROOT.resolve(".catalog-version");
        try {
            if (Files.isRegularFile(marker) && Files.readString(marker).trim().equals(CATALOG_VERSION)) return;
            Files.createDirectories(ROOT);
            try (InputStream raw = CosmeticCatalog.class.getResourceAsStream("/assets/enough/cosmetics/catalog.zip")) {
                if (raw == null) return;
                try (ZipInputStream zip = new ZipInputStream(raw)) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        Path output = ROOT.resolve(entry.getName()).normalize();
                        if (!output.startsWith(ROOT)) throw new IllegalStateException("Unsafe cosmetic path");
                        if (entry.isDirectory()) Files.createDirectories(output);
                        else {
                            Files.createDirectories(output.getParent());
                            Files.copy(zip, output, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            Files.writeString(marker, CATALOG_VERSION);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to extract cosmetic catalog", exception);
        }
    }

    private static void applyLocalSelection() {
        List<Path> paths = equippedEntries().stream().map(Entry::directory).toList();
        if (paths.isEmpty()) CosmeticBridge.unequip();
        else CosmeticBridge.equip(paths);
    }

    private static void addRemoteEntry(List<Entry> result, JsonObject object, String key, Kind kind) {
        if (!object.has(key) || object.get(key).isJsonNull()) return;
        String id = object.get(key).getAsString();
        ENTRIES.stream().filter(entry -> entry.kind() == kind && entry.id().equals(id)).findFirst().ifPresent(result::add);
    }

    private static void readEquippedIds() {
        EQUIPPED.clear();
        try {
            if (Files.isRegularFile(STATE)) {
                JsonObject object = JsonParser.parseString(Files.readString(STATE)).getAsJsonObject();
                for (Kind kind : Kind.values()) {
                    String key = kind.name().toLowerCase(Locale.ROOT);
                    if (object.has(key)) EQUIPPED.put(kind, object.get(key).getAsString());
                }
                migrateEquippedKinds();
                return;
            }
            if (Files.isRegularFile(LEGACY_STATE)) {
                String id = Files.readString(LEGACY_STATE).trim();
                ENTRIES.stream().filter(entry -> entry.id().equals(id)).findFirst()
                        .ifPresent(entry -> EQUIPPED.put(entry.kind(), entry.id()));
                writeState();
                Files.deleteIfExists(LEGACY_STATE);
            }
        } catch (Exception ignored) {
            EQUIPPED.clear();
        }
    }

    private static void writeState() throws Exception {
        Files.createDirectories(ROOT);
        JsonObject object = new JsonObject();
        for (Map.Entry<Kind, String> entry : EQUIPPED.entrySet()) {
            object.addProperty(entry.getKey().name().toLowerCase(Locale.ROOT), entry.getValue());
        }
        if (object.size() == 0) Files.deleteIfExists(STATE);
        else Files.writeString(STATE, object.toString());
    }

    private static void migrateEquippedKinds() {
        EnumMap<Kind, String> migrated = new EnumMap<>(Kind.class);
        for (String id : EQUIPPED.values()) {
            ENTRIES.stream().filter(entry -> entry.id().equals(id)).findFirst()
                    .ifPresent(entry -> migrated.put(entry.kind(), entry.id()));
        }
        EQUIPPED.clear();
        EQUIPPED.putAll(migrated);
    }

    private static String sanitize(String raw) {
        StringBuilder out = new StringBuilder();
        for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean asciiLetter = c >= 'a' && c <= 'z';
            boolean digit = c >= '0' && c <= '9';
            out.append(asciiLetter || digit || c == '_' || c == '-' ? c : '_');
        }
        return out.toString();
    }

    private static NativeImage cropTransparentPadding(NativeImage source) {
        int minX = source.getWidth(), minY = source.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getColorArgb(x, y) >>> 24) < 8) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) return source;
        int contentWidth = maxX - minX + 1;
        int contentHeight = maxY - minY + 1;
        int padding = Math.max(2, Math.round(Math.max(contentWidth, contentHeight) * .10f));
        int side = Math.max(contentWidth, contentHeight) + padding * 2;
        if (side >= source.getWidth() && side >= source.getHeight()) return source;
        NativeImage result = new NativeImage(side, side, true);
        int offsetX = (side - contentWidth) / 2;
        int offsetY = (side - contentHeight) / 2;
        for (int y = 0; y < contentHeight; y++) {
            for (int x = 0; x < contentWidth; x++) {
                result.setColorArgb(offsetX + x, offsetY + y, source.getColorArgb(minX + x, minY + y));
            }
        }
        return result;
    }
}
