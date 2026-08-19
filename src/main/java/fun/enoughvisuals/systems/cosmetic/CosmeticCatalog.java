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
import fun.enoughvisuals.systems.cosmetic.pulse.PulseCosmeticBridge;
import fun.enoughvisuals.utils.system.ClientLanguage;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
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
        MODEL("Модели"), WINGS("Крылья"), BODYWEAR("На тело"), PET("Питомцы"), HAT("Шапки");
        private final String title;
        Kind(String title) { this.title = title; }
        public String title() {
            return ClientLanguage.text("ui.cosmetics.category." + name().toLowerCase(Locale.ROOT), title);
        }
    }

    public record Entry(String id, String name, Kind kind, Path directory, int pulseId) {
        public boolean isPulse() { return pulseId >= 0; }
    }

    private static final String CATALOG_VERSION = "polairis-custom-models-pulse-v2-hats";
    private static final int CATALOG_PART_COUNT = 3;
    private static final String CATALOG_RESOURCE_PREFIX =
            "/assets/enough/cosmetics/catalog.zip.part";
    private static final Path ROOT = FabricLoader.getInstance().getGameDir().resolve("EnoughVisuals").resolve("cosmetics");
    private static final Path LEGACY_STATE = ROOT.resolve(".equipped");
    private static final Path STATE = ROOT.resolve(".equipped-slots.json");
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, Identifier> PREVIEWS = new HashMap<>();
    private static final EnumMap<Kind, String> EQUIPPED = new EnumMap<>(Kind.class);
    private static final Map<UUID, String> REMOTE_SIGNATURES = new HashMap<>();
    private static final Map<UUID, EnumMap<Kind, Integer>> REMOTE_PULSE = new HashMap<>();
    private static final String GENERIC_PREVIEW_SHA256 =
            "22c2e8c2358322d34941d1c6df6f6155c423ac3cb5183535a2daef5197674557";
    private static final Set<String> MODEL_OVERRIDES = Set.of(
            "weapon-komachi - оружие",
            "weapon-momiji - оружие",
            "weapon-murasa - оружие",
            "weapon-capitano - оружие",
            "weapon-yorihime - оружие"
    );
    /** Pulse cosmetics that must not appear in the catalog, keyed by "CATEGORY:id". */
    private static final Set<String> PULSE_HIDDEN = Set.of(
            "WINGS:123",
            "PET:81",
            "PET:90",
            "PET:126",
            "PET:132"
    );
    /** Display names that replace the ones baked into the Pulse runtime. */
    private static final Map<String, String> PULSE_NAMES = Map.ofEntries(
            Map.entry("WINGS:82", "Chains"),
            Map.entry("WINGS:86", "Trident"),
            Map.entry("WINGS:91", "Aqua"),
            Map.entry("WINGS:136", "Yellow"),
            Map.entry("HAT:80", "Shlapa"),
            Map.entry("HAT:85", "Face"),
            Map.entry("HAT:88", "Pinguin"),
            Map.entry("HAT:125", "Hat"),

            // New Hats from pasta
            Map.entry("HAT:201", "Bear"),
            Map.entry("HAT:202", "Blackhole Halo"),
            Map.entry("HAT:203", "Frog"),
            Map.entry("HAT:204", "Pilot"),

            // New Wings from pasta
            Map.entry("WINGS:210", "Archangel"),
            Map.entry("WINGS:211", "Balloon"),
            Map.entry("WINGS:212", "Blackhole"),
            Map.entry("WINGS:213", "Butterfly"),
            Map.entry("WINGS:214", "Draco"),
            Map.entry("WINGS:215", "Rocket"),
            Map.entry("WINGS:216", "Steampunk"),
            Map.entry("WINGS:217", "Techno"),

            // New Pets from pasta
            Map.entry("PET:220", "Axolotl"),
            Map.entry("PET:221", "Big Cat"),
            Map.entry("PET:222", "Capybara"),
            Map.entry("PET:223", "Dragon"),
            Map.entry("PET:224", "Frog"),
            Map.entry("PET:225", "Guinea Pig"),
            Map.entry("PET:226", "Panda"),
            Map.entry("PET:227", "Patrick"),
            Map.entry("PET:228", "Pig"),
            Map.entry("PET:229", "Spongebob"),
            Map.entry("PET:230", "Bigear Goat"),
            Map.entry("PET:231", "Chekushka"),
            Map.entry("PET:232", "Owl Jump Rope"),
            Map.entry("PET:233", "UFO")
    );
    private static boolean initialized;
    private static boolean restored;

    private CosmeticCatalog() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        extractBundledCatalog();
        scan();
        scanPulse();
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
        if (kind != Kind.MODEL) {
            int selected = PulseCosmeticBridge.selected(pulseCategory(kind));
            return ENTRIES.stream().filter(entry -> entry.kind() == kind && entry.pulseId() == selected)
                    .findFirst().orElse(null);
        }
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
        if (entry == null || (!entry.isPulse() && !CosmeticBridge.available())) return false;
        try {
            if (entry.isPulse()) {
                PulseCosmeticBridge.select(pulseCategory(entry.kind()), entry.pulseId());
                syncPulseRenderer();
                return true;
            }
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
            if (kind != Kind.MODEL) {
                PulseCosmeticBridge.clear(pulseCategory(kind));
                syncPulseRenderer();
                return true;
            }
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
        syncPulseRenderer();
    }

    public static void appendSync(JsonObject request) {
        initialize();
        JsonObject cosmetics = new JsonObject();
        for (Kind kind : Kind.values()) {
            if (kind == Kind.MODEL) continue;
            int selected = PulseCosmeticBridge.selected(pulseCategory(kind));
            if (selected >= 0) cosmetics.addProperty(kind.name().toLowerCase(Locale.ROOT), selected);
        }
        request.add("cosmetics", cosmetics);
    }

    public static void applySync(JsonObject response) {
        initialize();
        JsonArray values = response.has("active_cosmetics") && response.get("active_cosmetics").isJsonArray()
                ? response.getAsJsonArray("active_cosmetics") : new JsonArray();
        for (UUID uuid : new ArrayList<>(REMOTE_SIGNATURES.keySet())) {
            CosmeticBridge.clearRemote(uuid);
        }
        REMOTE_SIGNATURES.clear();
        Map<UUID, EnumMap<Kind, Integer>> pulse = new HashMap<>();
        for (JsonElement value : values) {
            try {
                JsonObject object = value.getAsJsonObject();
                UUID uuid = UUID.fromString(object.get("uuid").getAsString());
                EnumMap<Kind, Integer> pulseSlots = new EnumMap<>(Kind.class);
                addRemotePulse(pulseSlots, object, "wings", Kind.WINGS);
                addRemotePulse(pulseSlots, object, "bodywear", Kind.BODYWEAR);
                addRemotePulse(pulseSlots, object, "pet", Kind.PET);
                addRemotePulse(pulseSlots, object, "hat", Kind.HAT);
                if (!pulseSlots.isEmpty()) pulse.put(uuid, pulseSlots);
            } catch (Exception ignored) {
            }
        }
        REMOTE_PULSE.clear();
        REMOTE_PULSE.putAll(pulse);
        syncPulseRenderer();
    }

    private static void syncPulseRenderer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            EnumMap<Kind, Integer> local = new EnumMap<>(Kind.class);
            for (Kind kind : Kind.values()) {
                if (kind == Kind.MODEL) continue;
                int selected = PulseCosmeticBridge.selected(pulseCategory(kind));
                if (selected >= 0) local.put(kind, selected);
            }
            if (local.isEmpty()) REMOTE_PULSE.remove(client.player.getUuid());
            else REMOTE_PULSE.put(client.player.getUuid(), local);
        }
        Map<UUID, Map<String, Integer>> bridgeState = new HashMap<>();
        for (Map.Entry<UUID, EnumMap<Kind, Integer>> player : REMOTE_PULSE.entrySet()) {
            Map<String, Integer> slots = new HashMap<>();
            player.getValue().forEach((kind, id) -> slots.put(pulseCategory(kind), id));
            bridgeState.put(player.getKey(), slots);
        }
        PulseCosmeticBridge.syncRemote(bridgeState);
    }

    public static Entry pulseEquipped(UUID uuid, Kind kind) {
        initialize();
        if (uuid == null || kind == Kind.MODEL) return null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && uuid.equals(client.player.getUuid())) return equipped(kind);
        Integer id = REMOTE_PULSE.getOrDefault(uuid, new EnumMap<>(Kind.class)).get(kind);
        if (id == null) return null;
        return ENTRIES.stream().filter(entry -> entry.kind() == kind && entry.pulseId() == id)
                .findFirst().orElse(null);
    }

    private static void addRemotePulse(EnumMap<Kind, Integer> target, JsonObject source,
                                       String field, Kind kind) {
        if (!source.has(field) || source.get(field).isJsonNull()) return;
        try {
            int id = source.get(field).getAsInt();
            boolean valid = ENTRIES.stream().anyMatch(entry -> entry.kind() == kind && entry.pulseId() == id);
            if (valid) target.put(kind, id);
        } catch (RuntimeException ignored) {
        }
    }

    public static Identifier preview(Entry entry) {
        if (entry == null) return null;
        if (entry.isPulse()) return null;
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
                        boolean customModel = MODEL_OVERRIDES.contains(rootFolder)
                                || (!lower.startsWith("head-") && !lower.startsWith("weapon-"));
                        if (!customModel) return;
                        Kind kind = Kind.MODEL;
                        String folder = directory.getFileName().toString();
                        String name = cleanName(folder, kind);
                        if (kind == Kind.MODEL) ENTRIES.add(new Entry(source, name, kind, directory, -1));
                    });
        } catch (Exception ignored) {}
        ENTRIES.sort(Comparator.comparing(Entry::kind).thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
    }

    private static String cleanName(String raw, Kind kind) {
        String value = raw.replace('_', ' ').trim();
        int note = value.indexOf(" - ");
        return note > 0 ? value.substring(0, note) : value;
    }

    private static void scanPulse() {
        addPulse(Kind.WINGS, "WINGS");
        addPulse(Kind.BODYWEAR, "BODYWEAR");
        addPulse(Kind.PET, "PET");
        addPulse(Kind.HAT, "HAT");
        ENTRIES.sort(Comparator.comparing(Entry::kind).thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
    }

    private static void addPulse(Kind kind, String category) {
        for (PulseCosmeticBridge.Item item : PulseCosmeticBridge.items(category)) {
            String key = category + ":" + item.id();
            if (PULSE_HIDDEN.contains(key)) {
                if (PulseCosmeticBridge.selected(category) == item.id()) PulseCosmeticBridge.clear(category);
                continue;
            }
            ENTRIES.add(new Entry("pulse:" + category.toLowerCase(Locale.ROOT) + ":" + item.id(),
                    PULSE_NAMES.getOrDefault(key, cleanPulseName(item.name())), kind, null, item.id()));
        }
    }

    private static String cleanPulseName(String raw) {
        if (raw == null || raw.isBlank()) return "Косметика";
        int separator = raw.indexOf(" - ");
        return (separator >= 0 ? raw.substring(separator + 3) : raw).trim();
    }

    private static String pulseCategory(Kind kind) {
        return switch (kind) {
            case WINGS -> "WINGS";
            case BODYWEAR -> "BODYWEAR";
            case PET -> "PET";
            case HAT -> "HAT";
            case MODEL -> throw new IllegalArgumentException("Figura models are not Pulse cosmetics");
        };
    }

    private static InputStream openBundledCatalog() throws IOException {
        List<InputStream> parts = new ArrayList<>(CATALOG_PART_COUNT);
        for (int index = 0; index < CATALOG_PART_COUNT; index++) {
            String resource = CATALOG_RESOURCE_PREFIX + String.format(Locale.ROOT, "%02d", index);
            InputStream part = CosmeticCatalog.class.getResourceAsStream(resource);
            if (part == null) {
                for (InputStream opened : parts) {
                    try {
                        opened.close();
                    } catch (IOException ignored) {
                    }
                }
                throw new IOException("Missing bundled cosmetic catalog part: " + resource);
            }
            parts.add(part);
        }
        return new SequenceInputStream(Collections.enumeration(parts));
    }

    private static void extractBundledCatalog() {
        Path marker = ROOT.resolve(".catalog-version");
        try {
            if (Files.isRegularFile(marker) && Files.readString(marker).trim().equals(CATALOG_VERSION)) return;
            Files.createDirectories(ROOT);
            try (InputStream raw = openBundledCatalog()) {
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
        List<Path> paths = equippedEntries().stream().filter(entry -> !entry.isPulse())
                .map(Entry::directory).toList();
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
