package fun.enoughvisuals.systems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.HUD;
import fun.enoughvisuals.systems.modules.settings.Setting;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ConfigSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Config");
    private static final ConfigSystem INSTANCE = new ConfigSystem();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 1;
    private static final long AUTOSAVE_INTERVAL_MS = 12_000L;
    private static final String CONFIG_EXTENSION = ".enough";
    private static final String OBFUSCATION_HEADER = "EV1:";
    private static final byte[] OBFUSCATION_KEY = "enoughvisuals".getBytes(StandardCharsets.UTF_8);

    private Path configDir;
    private Path stateFile;
    private boolean initialized = false;

    private String lastConfigName = "default";
    private long lastAutosaveMs = 0L;
    private int lastSavedFingerprint = 0;

    private ConfigSystem() {}

    public static ConfigSystem getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (initialized) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        this.configDir = mc.runDirectory.toPath().resolve("enoughvisuals").resolve("configs");
        this.stateFile = configDir.resolve("state.json");

        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            LOGGER.error("Failed to create config directory {}", configDir, e);
        }

        ConfigState state = readState();
        if (state != null && state.lastConfigName != null && !state.lastConfigName.isBlank()) {
            String normalized = normalizeName(state.lastConfigName);
            if (normalized != null) {
                this.lastConfigName = normalized;
            }
        }

        initialized = true;
    }

    public synchronized boolean loadLastOrDefault() {
        ensureInitialized();

        String preferred = normalizeName(lastConfigName);
        if (preferred == null) preferred = "default";

        if (load(preferred)) {
            return true;
        }

        if (!"default".equals(preferred) && load("default")) {
            return true;
        }

        List<String> configs = listConfigs();
        for (String cfg : configs) {
            if (load(cfg)) return true;
        }

        return false;
    }

    public synchronized boolean load(String name) {
        ensureInitialized();
        String normalized = normalizeName(name);
        if (normalized == null) return false;

        Path file = getConfigPath(normalized);
        if (!Files.exists(file)) return false;

        ConfigSnapshot snapshot = readConfig(file);
        if (snapshot == null) return false;

        applySnapshot(snapshot);
        setLastConfigName(normalized);
        lastSavedFingerprint = fingerprint(snapshot);
        return true;
    }

    public synchronized void save(String name) {
        saveAs(name);
    }

    public synchronized boolean saveAs(String name) {
        ensureInitialized();
        String normalized = normalizeName(name);
        if (normalized == null) return false;

        ConfigSnapshot snapshot = captureSnapshot(normalized);
        if (!writeConfig(snapshot)) {
            return false;
        }

        setLastConfigName(normalized);
        lastSavedFingerprint = fingerprint(snapshot);
        return true;
    }

    public synchronized boolean saveCurrent() {
        String normalized = normalizeName(lastConfigName);
        if (normalized == null) normalized = "default";
        return saveAs(normalized);
    }

    public synchronized boolean remove(String nameOrIndex) {
        ensureInitialized();
        String resolved = resolveNameOrIndex(nameOrIndex);
        if (resolved == null) return false;

        Path file = getConfigPath(resolved);
        if (!Files.exists(file)) return false;

        try {
            Files.delete(file);
        } catch (Exception e) {
            LOGGER.error("Failed to delete config {}", file, e);
            return false;
        }

        if (resolved.equalsIgnoreCase(lastConfigName)) {
            List<String> left = listConfigs();
            if (left.contains("default")) setLastConfigName("default");
            else if (!left.isEmpty()) setLastConfigName(left.get(0));
            else setLastConfigName("default");
        }
        return true;
    }

    public synchronized List<String> listConfigs() {
        ensureInitialized();
        if (!Files.exists(configDir)) return List.of();

        List<String> out = new ArrayList<>();
        try (Stream<Path> stream = Files.list(configDir)) {
            stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(CONFIG_EXTENSION))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase("state.json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        String file = path.getFileName().toString();
                        out.add(file.substring(0, file.length() - CONFIG_EXTENSION.length()));
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to list configs in {}", configDir, e);
        }
        return out;
    }

    public synchronized String resolveNameOrIndex(String nameOrIndex) {
        if (nameOrIndex == null || nameOrIndex.isBlank()) return null;
        String trimmed = nameOrIndex.trim();

        try {
            int idx = Integer.parseInt(trimmed);
            List<String> list = listConfigs();
            if (idx >= 1 && idx <= list.size()) {
                return list.get(idx - 1);
            }
            return null;
        } catch (NumberFormatException ignored) {
        }

        return normalizeName(trimmed);
    }

    public synchronized void setLastConfigName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) normalized = "default";
        this.lastConfigName = normalized;
        writeState();
    }

    public synchronized String getLastConfigName() {
        return lastConfigName;
    }

    public synchronized Path getConfigDirectory() {
        ensureInitialized();
        return configDir;
    }

    public synchronized void onClientTick(MinecraftClient client) {
        if (!initialized) return;
        if (client == null || client.player == null) return;

        long now = System.currentTimeMillis();
        if (now - lastAutosaveMs < AUTOSAVE_INTERVAL_MS) return;
        lastAutosaveMs = now;

        String current = normalizeName(lastConfigName);
        if (current == null) current = "default";

        ConfigSnapshot snapshot = captureSnapshot(current);
        int fp = fingerprint(snapshot);
        if (fp == lastSavedFingerprint) {
            return;
        }

        if (writeConfig(snapshot)) {
            lastSavedFingerprint = fp;
            setLastConfigName(current);
        }
    }

    public synchronized void autosaveOnExit() {
        if (!initialized) return;
        saveCurrent();
    }

    private void ensureInitialized() {
        if (!initialized) initialize();
    }

    private Path getConfigPath(String name) {
        return configDir.resolve(name + CONFIG_EXTENSION);
    }

    private ConfigSnapshot captureSnapshot(String configName) {
        ConfigSnapshot snapshot = new ConfigSnapshot();
        snapshot.version = CURRENT_VERSION;
        snapshot.name = configName;
        snapshot.savedAt = System.currentTimeMillis();

        ModuleManager moduleManager = ModuleManager.getInstance();
        for (Module module : moduleManager.getModules()) {
            ModuleSnapshot moduleSnapshot = new ModuleSnapshot();
            moduleSnapshot.id = module.getId();
            moduleSnapshot.enabled = module.isEnabled();
            moduleSnapshot.keyBind = module.getKeyBind();
            moduleSnapshot.settings = new LinkedHashMap<>();
            for (Setting<?> setting : module.getSettings()) {
                moduleSnapshot.settings.put(setting.getId(), setting.serialize());
            }
            snapshot.modules.add(moduleSnapshot);
        }

        DragAndDrop dad = DragAndDrop.getInstance();
        snapshot.hud.rememberedSettings = dad.getRememberedSettingsCopy();
        for (HudElement element : dad.getActiveElements()) {
            if (element.getTypeId() == null || element.getTypeId().isBlank()) continue;

            HudElementSnapshot hudElement = new HudElementSnapshot();
            hudElement.id = element.getTypeId();
            hudElement.x = element.getX();
            hudElement.y = element.getY();
            hudElement.settings = new LinkedHashMap<>();
            for (Setting<?> setting : element.getSettings()) {
                hudElement.settings.put(setting.getId(), setting.serialize());
            }
            snapshot.hud.elements.add(hudElement);
        }

        return snapshot;
    }

    private void applySnapshot(ConfigSnapshot snapshot) {
        ModuleManager moduleManager = ModuleManager.getInstance();

        Map<String, ModuleSnapshot> modulesById = new HashMap<>();
        if (snapshot.modules != null) {
            for (ModuleSnapshot entry : snapshot.modules) {
                if (entry == null || entry.id == null) continue;
                modulesById.put(entry.id, entry);
            }
        }

        for (Module module : moduleManager.getModules()) {
            ModuleSnapshot data = modulesById.get(module.getId());
            if (data == null) {
                module.setEnabledSilent(false);
                continue;
            }

            module.setKeyBind(data.keyBind);
            applySettings(module.getSettings(), data.settings);
            module.setEnabledSilent(data.enabled);
        }

        DragAndDrop dad = DragAndDrop.getInstance();
        dad.clearAllImmediate();

        if (snapshot.hud != null && snapshot.hud.rememberedSettings != null) {
            dad.setRememberedSettings(snapshot.hud.rememberedSettings);
        } else {
            dad.setRememberedSettings(new HashMap<>());
        }

        if (snapshot.hud != null && snapshot.hud.elements != null) {
            for (HudElementSnapshot hudData : snapshot.hud.elements) {
                if (hudData == null || hudData.id == null || hudData.id.isBlank()) continue;

                HudElement element = dad.addElement(hudData.id, hudData.x, hudData.y);
                if (element == null) continue;

                element.setX(hudData.x);
                element.setY(hudData.y);
                applySettings(element.getSettings(), hudData.settings);
            }
        }

        HUD hudModule = moduleManager.get(HUD.class);
        if (hudModule != null) hudModule.syncWatermarkVisibility();
    }

    private void applySettings(List<Setting<?>> settings, Map<String, Object> values) {
        if (values == null || values.isEmpty()) return;

        for (Setting<?> setting : settings) {
            Object value = values.get(setting.getId());
            if (value == null) continue;
            try {
                setting.deserialize(value);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean writeConfig(ConfigSnapshot snapshot) {
        Path file = getConfigPath(snapshot.name);
        try {
            Files.createDirectories(configDir);
            String plainJson = GSON.toJson(snapshot);
            String obfuscated = obfuscate(plainJson);
            writeAtomically(file, obfuscated);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to write config {}", file, e);
            return false;
        }
    }

    private ConfigSnapshot readConfig(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StringBuilder raw = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while ((read = reader.read(buf)) >= 0) {
                raw.append(buf, 0, read);
            }

            String text = raw.toString();
            String json = deobfuscate(text);
            if (json == null || json.isBlank()) {
                json = text;
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            ConfigSnapshot snapshot = GSON.fromJson(root, ConfigSnapshot.class);
            if (snapshot != null && snapshot.modules != null && !snapshot.modules.isEmpty()) {
                return snapshot;
            }

            ConfigSnapshot migrated = migrateLegacySnapshot(root, file);
            return migrated != null ? migrated : snapshot;
        } catch (Exception e) {
            LOGGER.error("Failed to read config {}", file, e);
            return null;
        }
    }

    private ConfigState readState() {
        if (stateFile == null || !Files.exists(stateFile)) return null;
        try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ConfigState state = GSON.fromJson(root, ConfigState.class);
            if (root.has("lastConfigName") && state != null
                    && state.lastConfigName != null && !state.lastConfigName.isBlank()) return state;
            ConfigState migrated = new ConfigState();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    migrated.lastConfigName = value.getAsString();
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    migrated.updatedAt = value.getAsLong();
                }
            }
            if (migrated.lastConfigName == null || migrated.lastConfigName.isBlank()) return state;
            writeAtomically(stateFile, GSON.toJson(migrated));
            return migrated;
        } catch (Exception e) {
            LOGGER.error("Failed to read config state {}", stateFile, e);
            return null;
        }
    }

    private void writeState() {
        if (stateFile == null) return;
        try {
            Files.createDirectories(configDir);
            ConfigState state = new ConfigState();
            state.lastConfigName = lastConfigName;
            state.updatedAt = System.currentTimeMillis();
            writeAtomically(stateFile, GSON.toJson(state));
        } catch (Exception e) {
            LOGGER.error("Failed to write config state {}", stateFile, e);
        }
    }

    private ConfigSnapshot migrateLegacySnapshot(JsonObject root, Path sourceFile) {
        JsonArray modules = null;
        JsonObject hud = null;
        ConfigSnapshot migrated = new ConfigSnapshot();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonArray()) {
                modules = value.getAsJsonArray();
            } else if (value.isJsonObject()) {
                hud = value.getAsJsonObject();
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                migrated.name = value.getAsString();
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                long number = value.getAsLong();
                if (number > 10_000L) migrated.savedAt = number;
                else migrated.version = (int) number;
            }
        }
        if (modules == null) return null;

        migrated.modules = new ArrayList<>();
        for (JsonElement element : modules) {
            if (!element.isJsonObject()) continue;
            ModuleSnapshot module = new ModuleSnapshot();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    module.settings = jsonObjectToMap(value.getAsJsonObject());
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    module.id = value.getAsString();
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                    module.enabled = value.getAsBoolean();
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    module.keyBind = value.getAsInt();
                }
            }
            if (module.id != null && !module.id.isBlank()) migrated.modules.add(module);
        }
        migrated.hud = migrateLegacyHud(hud);
        if (migrated.modules.isEmpty()) return null;

        try {
            writeAtomically(sourceFile, obfuscate(GSON.toJson(migrated)));
            LOGGER.info("Migrated protected config {} to stable schema", sourceFile.getFileName());
        } catch (Exception error) {
            LOGGER.warn("Loaded legacy config {}, but could not rewrite it", sourceFile.getFileName(), error);
        }
        return migrated;
    }

    private HudSnapshot migrateLegacyHud(JsonObject source) {
        HudSnapshot hud = new HudSnapshot();
        if (source == null) return hud;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                hud.rememberedSettings = jsonObjectToNestedMap(value.getAsJsonObject());
                continue;
            }
            if (!value.isJsonArray()) continue;
            for (JsonElement element : value.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                HudElementSnapshot item = new HudElementSnapshot();
                int coordinate = 0;
                for (Map.Entry<String, JsonElement> itemEntry : element.getAsJsonObject().entrySet()) {
                    JsonElement itemValue = itemEntry.getValue();
                    if (itemValue.isJsonObject()) {
                        item.settings = jsonObjectToMap(itemValue.getAsJsonObject());
                    } else if (itemValue.isJsonPrimitive() && itemValue.getAsJsonPrimitive().isString()) {
                        item.id = itemValue.getAsString();
                    } else if (itemValue.isJsonPrimitive() && itemValue.getAsJsonPrimitive().isNumber()) {
                        if (coordinate++ == 0) item.x = itemValue.getAsFloat();
                        else item.y = itemValue.getAsFloat();
                    }
                }
                if (item.id != null && !item.id.isBlank()) hud.elements.add(item);
            }
        }
        return hud;
    }

    private Map<String, Object> jsonObjectToMap(JsonObject source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            result.put(entry.getKey(), GSON.fromJson(entry.getValue(), Object.class));
        }
        return result;
    }

    private Map<String, Map<String, Object>> jsonObjectToNestedMap(JsonObject source) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                result.put(entry.getKey(), jsonObjectToMap(entry.getValue().getAsJsonObject()));
            }
        }
        return result;
    }

    private int fingerprint(ConfigSnapshot snapshot) {
        int result = Integer.hashCode(snapshot.version);
        result = 31 * result + GSON.toJson(snapshot.modules).hashCode();
        result = 31 * result + GSON.toJson(snapshot.hud).hashCode();
        return result;
    }

    private String normalizeName(String raw) {
        if (raw == null) return null;

        String name = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (name.isEmpty()) return null;
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(CONFIG_EXTENSION)) {
            name = name.substring(0, name.length() - CONFIG_EXTENSION.length());
        }

        name = name.replaceAll("\\s+", "_");
        name = name.replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "");
        name = name.replaceAll("^\\.+|\\.+$", "");

        if (name.isBlank()) return null;
        if (name.length() > 64) {
            name = name.substring(0, 64);
        }
        return name;
    }

    private void writeAtomically(Path target, String contents) throws Exception {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, contents, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String obfuscate(String json) {
        if (json == null) return "";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            int key = OBFUSCATION_KEY[i % OBFUSCATION_KEY.length] & 0xFF;
            int salt = (i * 31 + 17) & 0x7F;
            out[i] = (byte) (bytes[i] ^ key ^ salt);
        }
        return OBFUSCATION_HEADER + Base64.getEncoder().encodeToString(out);
    }

    private String deobfuscate(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (!trimmed.startsWith(OBFUSCATION_HEADER)) {
            return null;
        }

        try {
            String payload = trimmed.substring(OBFUSCATION_HEADER.length());
            byte[] encoded = Base64.getDecoder().decode(payload);
            byte[] out = new byte[encoded.length];
            for (int i = 0; i < encoded.length; i++) {
                int key = OBFUSCATION_KEY[i % OBFUSCATION_KEY.length] & 0xFF;
                int salt = (i * 31 + 17) & 0x7F;
                out[i] = (byte) (encoded[i] ^ key ^ salt);
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class ConfigState {
        @SerializedName("lastConfigName")
        private String lastConfigName = "default";
        @SerializedName("updatedAt")
        private long updatedAt = 0L;
    }

    private static final class ConfigSnapshot {
        @SerializedName("version")
        private int version = CURRENT_VERSION;
        @SerializedName("name")
        private String name = "default";
        @SerializedName("savedAt")
        private long savedAt = 0L;
        @SerializedName("modules")
        private List<ModuleSnapshot> modules = new ArrayList<>();
        @SerializedName("hud")
        private HudSnapshot hud = new HudSnapshot();
    }

    private static final class ModuleSnapshot {
        @SerializedName("id")
        private String id = "";
        @SerializedName("enabled")
        private boolean enabled = false;
        @SerializedName("keyBind")
        private int keyBind = -1;
        @SerializedName("settings")
        private Map<String, Object> settings = new LinkedHashMap<>();
    }

    private static final class HudSnapshot {
        @SerializedName("elements")
        private List<HudElementSnapshot> elements = new ArrayList<>();
        @SerializedName("rememberedSettings")
        private Map<String, Map<String, Object>> rememberedSettings = new LinkedHashMap<>();
    }

    private static final class HudElementSnapshot {
        @SerializedName("id")
        private String id = "";
        @SerializedName("x")
        private float x = 0f;
        @SerializedName("y")
        private float y = 0f;
        @SerializedName("settings")
        private Map<String, Object> settings = new LinkedHashMap<>();
    }
}
