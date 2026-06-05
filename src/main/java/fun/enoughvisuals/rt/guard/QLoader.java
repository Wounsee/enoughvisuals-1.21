package fun.enoughvisuals.rt.guard;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

public final class QLoader {

    private static volatile boolean loaded = false;
    private static volatile Path loadedPath = null;
    private static volatile String expectedHash = "";

    private QLoader() {
    }

    public static void load() {
        if (loaded) return;
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String arch = System.getProperty("os.arch", "").toLowerCase();
            if (!os.contains("win") || !arch.contains("64")) {
                return;
            }

            String libName = "wg_x64.dll";
            String resourcePath = "/META-INF/.ev/n/" + libName;
            byte[] nativeBytes;
            try (InputStream in = QLoader.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    System.err.println("not found: " + resourcePath);
                    return;
                }
                nativeBytes = in.readAllBytes();
            }
            expectedHash = sha256(nativeBytes);

            Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
            Path nativeDir = gameDir.resolve("mods").resolve(".cache").resolve(".w").normalize();
            if (!nativeDir.startsWith(gameDir)) {
                return;
            }

            Files.createDirectories(nativeDir);
            Path libFile = nativeDir.resolve(libName).normalize();
            if (!libFile.startsWith(nativeDir)) {
                return;
            }

            if (!Files.exists(libFile) || !expectedHash.equals(sha256(Files.readAllBytes(libFile)))) {
                Path tmp = nativeDir.resolve(libName + ".tmp").normalize();
                Files.write(tmp, nativeBytes);
                Files.move(tmp, libFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }

            if (!expectedHash.equals(sha256(Files.readAllBytes(libFile)))) {
                return;
            }

            System.setProperty("__wg.qguard", QGuard.class.getName());
            System.load(libFile.toAbsolutePath().toString());
            System.clearProperty("__wg.qguard");
            loadedPath = libFile;
            loaded = true;
            System.out.println("loaded");
        } catch (Throwable t) {
            System.err.println("load error: " + t.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isNativeImageIntact() {
        Path path = loadedPath;
        if (path == null || expectedHash.isEmpty()) {
            return false;
        }
        try {
            return expectedHash.equals(sha256(Files.readAllBytes(path)));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder out = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            out.append(Character.forDigit((b >>> 4) & 0xF, 16));
            out.append(Character.forDigit(b & 0xF, 16));
        }
        return out.toString();
    }
}
