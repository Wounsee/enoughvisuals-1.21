package fun.enoughvisuals.systems.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates a HARD, stable hardware identifier (HWID) used for hardware bans.
 *
 * <p>Stability is the priority: it must not change on reboots, network changes,
 * VPNs or USB adapters. So it is derived from the operating system's persistent
 * machine identifier (Windows {@code MachineGuid}, Linux {@code /etc/machine-id},
 * macOS {@code IOPlatformUUID}) rather than volatile data like MAC addresses.
 * A persisted UUID is used only as a last-resort fallback. The raw values are
 * hashed with SHA-256 so they never leave the machine.
 */
public final class HwidUtil {

    private static volatile String cached;

    private HwidUtil() {
    }

    /** Returns the cached HWID, computing it once per process. */
    public static String get() {
        String value = cached;
        if (value != null) {
            return value;
        }
        synchronized (HwidUtil.class) {
            if (cached == null) {
                cached = compute();
            }
            return cached;
        }
    }

    private static String compute() {
        String machineId = stableMachineId();
        if (machineId == null || machineId.isBlank()) {
            machineId = persistedFallbackId();
        }
        // OS arch is included only to slightly harden the seed; it does not change.
        String seed = "enoughvisuals|v1|"
                + machineId.trim().toLowerCase(Locale.ROOT) + "|"
                + prop("os.arch").toLowerCase(Locale.ROOT);
        return sha256(seed);
    }

    /** Returns the OS-level persistent machine identifier, or null if unavailable. */
    private static String stableMachineId() {
        String os = prop("os.name").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                return windowsMachineGuid();
            }
            if (os.contains("mac") || os.contains("darwin")) {
                return macPlatformUuid();
            }
            // Linux / *nix
            String id = readFirstLine(Paths.get("/etc/machine-id"));
            if (id == null || id.isBlank()) {
                id = readFirstLine(Paths.get("/var/lib/dbus/machine-id"));
            }
            return id;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String windowsMachineGuid() {
        // HKLM\SOFTWARE\Microsoft\Cryptography\MachineGuid is stable across reboots
        // and hardware changes; it only resets on a fresh OS install.
        String out = runProcess(new String[]{
                "reg", "query",
                "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid"
        });
        if (out == null) {
            return null;
        }
        for (String line : out.split("\\R")) {
            int idx = line.indexOf("REG_SZ");
            if (idx >= 0) {
                String value = line.substring(idx + "REG_SZ".length()).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String macPlatformUuid() {
        String out = runProcess(new String[]{
                "/bin/sh", "-c",
                "ioreg -rd1 -c IOPlatformExpertDevice | awk -F'\"' '/IOPlatformUUID/{print $4}'"
        });
        return out == null ? null : out.trim();
    }

    /**
     * Last-resort fallback when no OS machine id is readable: a UUID persisted to
     * a fixed per-user file so it stays constant for the lifetime of that account.
     */
    private static String persistedFallbackId() {
        try {
            Path dir = Paths.get(prop("user.home"), ".enoughvisuals");
            Path file = dir.resolve("hwid");
            String existing = readFirstLine(file);
            if (existing != null && !existing.isBlank()) {
                return existing.trim();
            }
            Files.createDirectories(dir);
            String generated = UUID.randomUUID().toString();
            Files.writeString(file, generated, StandardCharsets.UTF_8);
            return generated;
        } catch (Throwable t) {
            // truly nothing stable available: derive from user + os (weakest case)
            return prop("user.name") + "@" + prop("os.name");
        }
    }

    private static String readFirstLine(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                return null;
            }
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line != null && !line.isBlank()) {
                    return line.trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String runProcess(String[] command) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            process.waitFor();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            return "unknown-" + Integer.toHexString(input.hashCode());
        }
    }

    private static String prop(String key) {
        try {
            String v = System.getProperty(key);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String env(String key) {
        try {
            String v = System.getenv(key);
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            return "";
        }
    }
}
