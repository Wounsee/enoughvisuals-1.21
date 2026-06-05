package fun.enoughvisuals.rt.guard;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class QRuntime {

    private static final String INTEGRITY_PATH = "/META-INF/.ev/rt.s";
    private static final String INTEGRITY_SIG_PREFIX = "!sig:";
    private static volatile boolean watchdogStarted = false;
    private static volatile int heartbeatSeed = 0x5A17C0DE;

    private QRuntime() {
    }

    public static boolean javaChecks() {
        try {
            List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            for (String arg : inputArgs) {
                String v = arg.toLowerCase(Locale.ROOT);
                if (hasDebugToken(v)) {
                    return false;
                }
                if (isStrict("WOUNSEE_STRICT_AGENTS") && hasAgentToken(v)) {
                    return false;
                }
            }

            String toolOpts = System.getenv("JAVA_TOOL_OPTIONS");
            if (toolOpts != null) {
                String v = toolOpts.toLowerCase(Locale.ROOT);
                if (hasDebugToken(v)) {
                    return false;
                }
                if (isStrict("WOUNSEE_STRICT_AGENTS") && hasAgentToken(v)) {
                    return false;
                }
            }

            String jdkOpts = System.getenv("JDK_JAVA_OPTIONS");
            if (jdkOpts != null) {
                String v = jdkOpts.toLowerCase(Locale.ROOT);
                if (hasDebugToken(v)) {
                    return false;
                }
                if (isStrict("WOUNSEE_STRICT_AGENTS") && hasAgentToken(v)) {
                    return false;
                }
            }

            String command = System.getProperty("sun.java.command", "").toLowerCase(Locale.ROOT);
            if (hasDebugToken(command)) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }

        return true;
    }

    public static boolean integrityChecks() {
        try (InputStream manifest = QRuntime.class.getResourceAsStream(INTEGRITY_PATH)) {
            if (manifest == null) {
                return !isStrict("WOUNSEE_STRICT_INTEGRITY");
            }

            Path jarPath = ownJarPath();
            if (jarPath == null || Files.isDirectory(jarPath)) {
                return !isStrict("WOUNSEE_STRICT_INTEGRITY");
            }

            String content = new String(manifest.readAllBytes(), StandardCharsets.UTF_8);
            String expectedSignature = "";
            List<String> signedLines = new ArrayList<>();
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                for (String raw : content.split("\n")) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.startsWith(INTEGRITY_SIG_PREFIX)) {
                        expectedSignature = line.substring(INTEGRITY_SIG_PREFIX.length());
                        continue;
                    }

                    int split = line.indexOf('=');
                    if (split <= 0 || split == line.length() - 1) {
                        return false;
                    }

                    String name = line.substring(0, split);
                    String expected = line.substring(split + 1);
                    signedLines.add(name + "=" + expected);
                    JarEntry entry = jar.getJarEntry(name);
                    if (entry == null) {
                        return false;
                    }
                    try (InputStream in = jar.getInputStream(entry)) {
                        if (!expected.equals(sha256(in.readAllBytes()))) {
                            return false;
                        }
                    }
                }
            }

            if (!expectedSignature.isEmpty()) {
                String canonical = String.join("\n", signedLines);
                String actual = sha256((canonical + "|" + integritySecret()).getBytes(StandardCharsets.UTF_8));
                if (!expectedSignature.equals(actual)) {
                    return false;
                }
            } else if (isStrict("WOUNSEE_STRICT_SIGNATURE")) {
                return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean nativeChecks() {
        QLoader.load();
        if (!QLoader.isLoaded()) {
            return false;
        }

        try {
            if (!QLoader.isNativeImageIntact()) {
                return false;
            }

            boolean strict = isStrict("WOUNSEE_STRICT_NATIVE");
            if (!QGuard.quickCheck() && strict) {
                return false;
            }
            if (!QGuard.timingCheck() && isStrict("WOUNSEE_STRICT_TIMING")) {
                return false;
            }
            if (!nativeChallengeCheck()) {
                return false;
            }
            if (!nativeHeartbeat()) {
                return false;
            }
            if (QGuard.hasSuspiciousModules() && isStrict("WOUNSEE_STRICT_MODULES")) {
                return false;
            }

            String enableWatchdog = System.getenv("WOUNSEE_ENABLE_WATCHDOG");
            if (enableWatchdog != null && ("1".equals(enableWatchdog) || "true".equalsIgnoreCase(enableWatchdog))) {
                QGuard.startWatchdog();
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void startWatchdog() {
        if (watchdogStarted) return;
        watchdogStarted = true;

        Thread thread = new Thread(() -> {
            for (;;) {
                try {
                    boolean ok = javaChecks() && integrityChecks();
                    if (QLoader.isLoaded()) {
                        ok = ok && QLoader.isNativeImageIntact() && nativeHeartbeat();
                    }
                    if (!ok && isStrict("WOUNSEE_STRICT_RUNTIME")) {
                        Runtime.getRuntime().halt(0xE04);
                    }
                    Thread.sleep(3500L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable ignored) {
                    if (isStrict("WOUNSEE_STRICT_RUNTIME")) {
                        Runtime.getRuntime().halt(0xE05);
                    }
                }
            }
        }, "Wounsee-Runtime");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean hasDebugToken(String value) {
        return value.contains("jdwp")
                || value.contains("-xdebug")
                || value.contains("agentlib:jdwp");
    }

    private static boolean nativeHeartbeat() {
        try {
            int seed = heartbeatSeed ^ (int) System.nanoTime();
            int actual = QGuard.heartbeat(seed);
            int expected = Integer.rotateLeft(seed ^ 0x6D2B79F5, 7) + 0x13579BDF;
            heartbeatSeed = actual ^ 0x4F1BBCDC;
            return actual == expected;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean nativeChallengeCheck() {
        try {
            int challenge = QGuard.requestChallenge();
            int response = Integer.rotateLeft(challenge ^ 0x1F3D5B79, 9) + 0x2468ACE1;
            return QGuard.verifyResponse(challenge, response);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasAgentToken(String value) {
        return value.contains("-javaagent")
                || value.contains("agentpath")
                || value.contains("hotswap")
                || value.contains("byteman")
                || value.contains("frida");
    }

    private static Path ownJarPath() throws Exception {
        ProtectionDomain domain = QRuntime.class.getProtectionDomain();
        if (domain == null || domain.getCodeSource() == null || domain.getCodeSource().getLocation() == null) {
            return null;
        }
        URI uri = domain.getCodeSource().getLocation().toURI();
        return Path.of(uri).toAbsolutePath().normalize();
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

    private static String integritySecret() {
        int v = Integer.rotateLeft(0x73A9C4F1, 5) ^ 0x55AA10CC;
        return "ev.rt.guard." + Integer.toHexString(v);
    }

    private static boolean isStrict(String envName) {
        String value = System.getenv(envName);
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}
