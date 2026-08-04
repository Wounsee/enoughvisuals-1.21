package punchy.client.tuning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.class_310;
import punchy.Punchy;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningProfileManager.class */
public final class TuningProfileManager {
    private static final String EXT = ".json";
    private static final String ACTIVE_PROFILE_FILE = ".active_profile";

    private TuningProfileManager() {
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
                    return name.toLowerCase().endsWith(EXT);
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
            boolean saved = PunchyTuningConfig.saveCurrentProfileDeltaToFile(target);
            if (!saved) {
                return false;
            }
            boolean ok = activateProfile(safe, true);
            return ok;
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
        boolean ok = Files.isRegularFile(source, new LinkOption[0]) && activateProfile(safe, true);
        return ok;
    }

    static boolean deleteProfile(String name) {
        String safe = sanitizeName(name);
        if (safe.isEmpty()) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(profilePath(safe));
            if (deleted) {
                Files.deleteIfExists(activeProfilePath());
            }
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    static boolean importProfile(Path source, String targetName) {
        if (source == null || !Files.isRegularFile(source, new LinkOption[0])) {
            return false;
        }
        String fallback = source.getFileName() == null ? "" : source.getFileName().toString();
        if (fallback.toLowerCase().endsWith(EXT)) {
            fallback = fallback.substring(0, fallback.length() - EXT.length());
        }
        String safe = sanitizeName((targetName == null || targetName.isBlank()) ? fallback : targetName);
        if (safe.isEmpty()) {
            return false;
        }
        Path target = profilePath(safe);
        try {
            Files.createDirectories(target.getParent(), new FileAttribute[0]);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            boolean ok = activateProfile(safe, true);
            return ok;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getActiveProfileName() {
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

    public static Path getActiveProfileFilePath() {
        String active = getActiveProfileName();
        if (active == null || active.isBlank()) {
            return null;
        }
        Path source = profilePath(active);
        if (Files.isRegularFile(source, new LinkOption[0])) {
            return source;
        }
        try {
            Files.deleteIfExists(activeProfilePath());
            return null;
        } catch (IOException e) {
            return null;
        }
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
        return punchyDir().resolve("profiles");
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
            class_310 mc = class_310.method_1551();
            PunchyTuningConfig.reloadActiveProfileFast(mc != null ? mc.method_1478() : null);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static Path activeProfilePath() {
        return profilesDir().resolve(ACTIVE_PROFILE_FILE);
    }

    private static Path profilePath(String name) {
        return profilesDir().resolve(name + ".json");
    }

    private static Path punchyDir() {
        class_310 mc = class_310.method_1551();
        Path game = mc != null ? mc.field_1697.toPath() : Path.of(".", new String[0]);
        return game.resolve("config").resolve(Punchy.MOD_ID);
    }

    private static boolean activateProfile(String profileName, boolean reloadNow) {
        Path sourceProfile = profilePath(profileName);
        if (sourceProfile == null) {
            return false;
        }
        try {
            if (!Files.exists(sourceProfile, new LinkOption[0])) {
                return false;
            }
            Files.createDirectories(activeProfilePath().getParent(), new FileAttribute[0]);
            Files.writeString(activeProfilePath(), sanitizeName(profileName), new OpenOption[0]);
            if (reloadNow) {
                class_310 mc = class_310.method_1551();
                PunchyTuningConfig.reloadActiveProfileFast(mc != null ? mc.method_1478() : null);
                return true;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
