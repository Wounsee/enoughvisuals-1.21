package punchy.client.tuning;

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
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.class_310;
import punchy.Punchy;
import punchy.client.animation.PunchyAnimationManager;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixProfileManager.class */
final class MixProfileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXT = ".json";
    private static final String ACTIVE_PROFILE_FILE = ".active_mix_profile";

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/MixProfileManager$MixProfileData.class */
    private static final class MixProfileData {
        LinkedHashMap<String, String> animationPackSelections = new LinkedHashMap<>();
        LinkedHashMap<String, String> animationPackTypeSelections = new LinkedHashMap<>();
        LinkedHashMap<String, Boolean> mixSpecificForceOverride = new LinkedHashMap<>();

        private MixProfileData() {
        }
    }

    private MixProfileManager() {
    }

    static List<String> listProfiles() {
        List<String> out = new ArrayList<>();
        Path dir = profilesDir();
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
            Stream<Path> stream = Files.list(dir);
            try {
                Stream streamSorted = stream.filter(x$0 -> {
                    return Files.isRegularFile(x$0, new LinkOption[0]);
                }).map(path -> {
                    return path.getFileName().toString();
                }).filter(name -> {
                    return name.toLowerCase(Locale.ROOT).endsWith(EXT);
                }).map(name2 -> {
                    return name2.substring(0, name2.length() - EXT.length());
                }).sorted(Comparator.naturalOrder());
                Objects.requireNonNull(out);
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
        return out;
    }

    static boolean saveCurrentAs(String name) {
        String safe = sanitizeName(name);
        if (safe.isEmpty()) {
            return false;
        }
        Path target = profilePath(safe);
        try {
            Files.createDirectories(target.getParent(), new FileAttribute[0]);
            MixProfileData data = readFromCurrentConfig();
            Writer writer = Files.newBufferedWriter(target, new OpenOption[0]);
            try {
                GSON.toJson(data, writer);
                if (writer != null) {
                    writer.close();
                }
                Files.createDirectories(activeProfilePath().getParent(), new FileAttribute[0]);
                Files.writeString(activeProfilePath(), safe, new OpenOption[0]);
                return true;
            } finally {
            }
        } catch (IOException e) {
            return false;
        }
    }

    static boolean loadProfile(String name) {
        String safe = sanitizeName(name);
        if (safe.isEmpty()) {
            return false;
        }
        Path source = profilePath(safe);
        return loadProfileInternal(safe, source, true);
    }

    static boolean deleteProfile(String name) {
        String safe = sanitizeName(name);
        if (safe.isEmpty()) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(profilePath(safe));
            String active = getActiveProfileName();
            if (active != null && active.equals(safe)) {
                Files.deleteIfExists(activeProfilePath());
            }
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    static String getActiveProfileName() {
        Path marker = activeProfilePath();
        try {
            if (!Files.isRegularFile(marker, new LinkOption[0])) {
                return null;
            }
            String raw = Files.readString(marker).trim();
            String safe = sanitizeName(raw);
            if (safe.isEmpty()) {
                return null;
            }
            return safe;
        } catch (IOException e) {
            return null;
        }
    }

    static Path profilesDirectoryPath() {
        Path dir = profilesDir();
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
        } catch (IOException e) {
        }
        return dir;
    }

    static boolean unloadActiveProfileToDefault() {
        Path marker = activeProfilePath();
        try {
            Files.deleteIfExists(marker);
            PunchyAnimationManager.requestTuningReload();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean restoreActiveProfileForStartup() {
        String active = getActiveProfileName();
        if (active == null || active.isBlank()) {
            return false;
        }
        Path source = profilePath(active);
        return loadProfileInternal(active, source, false);
    }

    private static boolean loadProfileInternal(String safeName, Path source, boolean reloadNow) {
        if (safeName == null || safeName.isBlank() || source == null || !Files.isRegularFile(source, new LinkOption[0])) {
            return false;
        }
        try {
            Reader reader = Files.newBufferedReader(source);
            try {
                MixProfileData data = (MixProfileData) GSON.fromJson(reader, MixProfileData.class);
                applyToCurrentConfig(data);
                Files.createDirectories(activeProfilePath().getParent(), new FileAttribute[0]);
                Files.writeString(activeProfilePath(), safeName, new OpenOption[0]);
                if (reloadNow) {
                    PunchyAnimationManager.requestTuningReload();
                }
                if (reader != null) {
                    reader.close();
                }
                return true;
            } finally {
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static MixProfileData readFromCurrentConfig() {
        PunchyConfig.Data snapshot = PunchyConfig.snapshot();
        MixProfileData data = new MixProfileData();
        if (snapshot.animationPackSelections != null) {
            data.animationPackSelections.putAll(snapshot.animationPackSelections);
        }
        if (snapshot.animationPackTypeSelections != null) {
            data.animationPackTypeSelections.putAll(snapshot.animationPackTypeSelections);
        }
        if (snapshot.mixSpecificForceOverride != null) {
            data.mixSpecificForceOverride.putAll(snapshot.mixSpecificForceOverride);
        }
        return data;
    }

    private static void applyToCurrentConfig(MixProfileData data) {
        PunchyConfig.Data snapshot = PunchyConfig.snapshot();
        snapshot.animationPackSelections = new LinkedHashMap();
        snapshot.animationPackTypeSelections = new LinkedHashMap();
        snapshot.mixSpecificForceOverride = new LinkedHashMap();
        if (data != null) {
            if (data.animationPackSelections != null) {
                snapshot.animationPackSelections.putAll(data.animationPackSelections);
            }
            if (data.animationPackTypeSelections != null) {
                snapshot.animationPackTypeSelections.putAll(data.animationPackTypeSelections);
            }
            if (data.mixSpecificForceOverride != null) {
                snapshot.mixSpecificForceOverride.putAll(data.mixSpecificForceOverride);
            }
        }
        PunchyConfig.applyAndSave(snapshot);
    }

    private static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String strReplaceAll = raw.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        while (true) {
            String cleaned = strReplaceAll;
            if (cleaned.startsWith(".")) {
                strReplaceAll = cleaned.substring(1);
            } else {
                return cleaned;
            }
        }
    }

    private static Path profilesDir() {
        class_310 mc = class_310.method_1551();
        Path game = mc != null ? mc.field_1697.toPath() : Path.of(".", new String[0]);
        return game.resolve("config").resolve(Punchy.MOD_ID).resolve("mix_profiles");
    }

    private static Path profilePath(String name) {
        return profilesDir().resolve(name + ".json");
    }

    private static Path activeProfilePath() {
        return profilesDir().resolve(ACTIVE_PROFILE_FILE);
    }
}
