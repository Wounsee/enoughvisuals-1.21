package fun.enoughvisuals.systems.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(obfuscated);
            }
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
            return GSON.fromJson(json, ConfigSnapshot.class);
        } catch (Exception e) {
            LOGGER.error("Failed to read config {}", file, e);
            return null;
        }
    }

    private ConfigState readState() {
        if (stateFile == null || !Files.exists(stateFile)) return null;
        try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, ConfigState.class);
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
            try (Writer writer = Files.newBufferedWriter(stateFile, StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write config state {}", stateFile, e);
        }
    }

    private int fingerprint(ConfigSnapshot snapshot) {
        return GSON.toJson(snapshot).hashCode();
    }

    private String normalizeName(String raw) {
        if (raw == null) return null;

        String name = raw.trim();
        if (name.isEmpty()) return null;
        if (name.endsWith(CONFIG_EXTENSION)) {
            name = name.substring(0, name.length() - CONFIG_EXTENSION.length());
        }

        name = name.replaceAll("\\s+", "_");
        name = name.replaceAll("[^a-zA-Z0-9._-]", "");

        if (name.isBlank()) return null;
        if (name.length() > 64) {
            name = name.substring(0, 64);
        }
        return name;
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
        private String lastConfigName = "default";
        private long updatedAt = 0L;
    }

    private static final class ConfigSnapshot {
        private int version = CURRENT_VERSION;
        private String name = "default";
        private long savedAt = 0L;
        private List<ModuleSnapshot> modules = new ArrayList<>();
        private HudSnapshot hud = new HudSnapshot();
    }

    private static final class ModuleSnapshot {
        private String id = "";
        private boolean enabled = false;
        private int keyBind = -1;
        private Map<String, Object> settings = new LinkedHashMap<>();
    }

    private static final class HudSnapshot {
        private List<HudElementSnapshot> elements = new ArrayList<>();
        private Map<String, Map<String, Object>> rememberedSettings = new LinkedHashMap<>();
    }

    private static final class HudElementSnapshot {
        private String id = "";
        private float x = 0f;
        private float y = 0f;
        private Map<String, Object> settings = new LinkedHashMap<>();
    }
}
