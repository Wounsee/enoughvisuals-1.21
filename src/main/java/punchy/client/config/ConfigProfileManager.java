package punchy.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.class_310;
import punchy.Punchy;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/config/ConfigProfileManager.class */
final class ConfigProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXTENSION = ".json";
    private static final String ACTIVE_PROFILE_FILE = "active_profile.txt";

    private ConfigProfileManager() {
    }

    static Path profilesDirectoryPath() {
        Path pathOf;
        class_310 client = class_310.method_1551();
        if (client != null) {
            pathOf = client.field_1697.toPath().resolve("config");
        } else {
            pathOf = Path.of("config", new String[0]);
        }
        Path configDir = pathOf;
        return configDir.resolve(Punchy.MOD_ID).resolve("config_profiles");
    }

    static List<String> listProfiles() {
        Path dir = profilesDirectoryPath();
        List<String> result = new ArrayList<>();
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
            Stream<Path> stream = Files.list(dir);
            try {
                Stream streamSorted = stream.filter(path -> {
                    return Files.isRegularFile(path, new LinkOption[0]) && path.getFileName().toString().endsWith(EXTENSION);
                }).map(path2 -> {
                    return stripExtension(path2.getFileName().toString());
                }).filter(name -> {
                    return !name.isBlank();
                }).sorted(Comparator.naturalOrder());
                Objects.requireNonNull(result);
                streamSorted.forEach((v1) -> {
                    r1.add(v1);
                });
                if (stream != null) {
                    stream.close();
                }
            } finally {
            }
        } catch (IOException e) {
        }
        return result;
    }

    static boolean saveCurrentAs(String profileName, PunchyConfig.Data data) {
        String safe = sanitizeProfileName(profileName);
        if (safe.isBlank() || data == null) {
            return false;
        }
        Path file = profilePath(safe);
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            PunchyConfig.Data snapshot = (PunchyConfig.Data) GSON.fromJson(GSON.toJson(data), PunchyConfig.Data.class);
            if (snapshot == null) {
                return false;
            }
            snapshot.animationPackSelections = new LinkedHashMap();
            snapshot.animationPackTypeSelections = new LinkedHashMap();
            snapshot.mixSpecificForceOverride = new LinkedHashMap();
            Writer writer = Files.newBufferedWriter(file, new OpenOption[0]);
            try {
                GSON.toJson(snapshot, writer);
                if (writer != null) {
                    writer.close();
                    return true;
                }
                return true;
            } finally {
            }
        } catch (IOException e) {
            return false;
        }
    }

    static PunchyConfig.Data load(String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe.isBlank()) {
            return null;
        }
        Path file = profilePath(safe);
        if (!Files.isRegularFile(file, new LinkOption[0])) {
            return null;
        }
        try {
            Reader reader = Files.newBufferedReader(file);
            try {
                PunchyConfig.Data data = (PunchyConfig.Data) GSON.fromJson(reader, PunchyConfig.Data.class);
                if (reader != null) {
                    reader.close();
                }
                return data;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    static boolean deleteProfile(String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe.isBlank()) {
            return false;
        }
        Path file = profilePath(safe);
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                String active = getActiveProfileName();
                if (safe.equals(active)) {
                    setActiveProfileName(null);
                }
            }
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    static String getActiveProfileName() {
        Path file = activeProfilePath();
        if (!Files.isRegularFile(file, new LinkOption[0])) {
            return null;
        }
        try {
            String raw = Files.readString(file);
            String safe = sanitizeProfileName(raw);
            if (safe.isBlank()) {
                return null;
            }
            return safe;
        } catch (IOException e) {
            return null;
        }
    }

    static void setActiveProfileName(String profileName) {
        Path file = activeProfilePath();
        String safe = sanitizeProfileName(profileName);
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            if (safe.isBlank()) {
                Files.deleteIfExists(file);
            } else {
                Files.writeString(file, safe, new OpenOption[0]);
            }
        } catch (IOException e) {
        }
    }

    static String sanitizeProfileName(String input) {
        if (input == null) {
            return "";
        }
        String value = input.trim();
        if (value.isBlank()) {
            return "";
        }
        String value2 = value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        if (value2.length() > 64) {
            value2 = value2.substring(0, 64);
        }
        return value2;
    }

    private static Path profilePath(String safeName) {
        return profilesDirectoryPath().resolve(safeName + ".json");
    }

    private static Path activeProfilePath() {
        return profilesDirectoryPath().resolve(ACTIVE_PROFILE_FILE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        if (fileName.endsWith(EXTENSION)) {
            return fileName.substring(0, fileName.length() - EXTENSION.length());
        }
        return fileName;
    }
}
