package fun.enoughvisuals.rt.entry;

import fun.enoughvisuals.rt.guard.QRuntime;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public final class EntryBridge {

    private static final String INDEX_PATH = "/META-INF/.ev/rt.i";
    private static final String GATES_PATH = "/META-INF/.ev/rt.g";
    private static final String CORE_ID = "core";
    private static final String STAGE_JAVA = "java";
    private static final String STAGE_INTEGRITY = "integrity";
    private static final String STAGE_NATIVE = "native";
    private static final Map<String, Class<?>> CACHE = new HashMap<>();

    private static volatile Map<String, EntryRecord> indexCache;
    private static volatile List<String> gateClassCache;
    private static volatile boolean guardStagesPassed;

    private EntryBridge() {
    }

    public static void initializeClient() {
        ensureGuardStages();
        invoke(CORE_ID, "initializeClient");
    }

    public static void setIcon() {
        ensureGuardStages();
        invoke(CORE_ID, "setIcon");
    }

    private static void invoke(String payloadId, String methodName) {
        try {
            Method method = loadPayload(payloadId).getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(null);
        } catch (Throwable t) {
            throw new RuntimeException("invoke failed: " + payloadId + "#" + methodName, t);
        }
    }

    private static synchronized Class<?> loadPayload(String payloadId) throws Exception {
        Class<?> cached = CACHE.get(payloadId);
        if (cached != null) {
            return cached;
        }

        EntryRecord entry = readIndex().get(payloadId);
        if (entry == null) {
            throw new IllegalStateException("missing runtime entry: " + payloadId);
        }

        String fullPath = "/" + entry.resourcePath();
        byte[] encryptedBytes;
        try (InputStream in = EntryBridge.class.getResourceAsStream(fullPath)) {
            if (in == null) {
                throw new IllegalStateException("missing payload resource: " + entry.resourcePath());
            }
            encryptedBytes = in.readAllBytes();
        }

        byte[] classBytes = crypt(
                entry.className(),
                entry.resourcePath(),
                entry.seed(),
                entry.salt(),
                encryptedBytes);

        if (!entry.hash().isEmpty()) {
            String actual = sha256(classBytes);
            if (!entry.hash().equals(actual)) {
                throw new IllegalStateException("payload hash mismatch: " + payloadId);
            }
        }

        Class<?> defined = new GuardedClassLoader(EntryBridge.class.getClassLoader())
                .definePayload(entry.className(), classBytes);
        CACHE.put(payloadId, defined);
        return defined;
    }

    private static void ensureGuardStages() {
        if (guardStagesPassed) {
            return;
        }
        synchronized (EntryBridge.class) {
            if (guardStagesPassed) {
                return;
            }
            if (!runStage(STAGE_JAVA, QRuntime::javaChecks)) {
                throw new IllegalStateException("stage failed: " + STAGE_JAVA);
            }
            if (!runStage(STAGE_INTEGRITY, QRuntime::integrityChecks)) {
                throw new IllegalStateException("stage failed: " + STAGE_INTEGRITY);
            }
            if (!runStage(STAGE_NATIVE, QRuntime::nativeChecks)) {
                throw new IllegalStateException("stage failed: " + STAGE_NATIVE);
            }
            QRuntime.startWatchdog();
            guardStagesPassed = true;
        }
    }

    private static boolean runStage(String stage, BooleanSupplier localCheck) {
        if (!localCheck.getAsBoolean()) {
            return false;
        }
        return runExternalGates(stage);
    }

    private static boolean runExternalGates(String stage) {
        try {
            for (String className : readGateClasses()) {
                if (!invokeGate(className, stage)) {
                    return false;
                }
            }
            return true;
        } catch (Throwable t) {
            throw new RuntimeException("gate stage failed: " + stage, t);
        }
    }

    private static List<String> readGateClasses() throws Exception {
        List<String> local = gateClassCache;
        if (local != null) {
            return local;
        }

        ArrayList<String> result = new ArrayList<>();
        try (InputStream in = EntryBridge.class.getResourceAsStream(GATES_PATH)) {
            if (in != null) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                for (String raw : text.split("\n")) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    result.add(line);
                }
            }
        }

        gateClassCache = result;
        return result;
    }

    private static boolean invokeGate(String className, String stage) throws Exception {
        Class<?> gateClass = Class.forName(className, true, EntryBridge.class.getClassLoader());
        Method method = gateClass.getDeclaredMethod("check", String.class);
        method.setAccessible(true);

        Object target = null;
        if (!Modifier.isStatic(method.getModifiers())) {
            target = gateClass.getDeclaredConstructor().newInstance();
        }

        Object value = method.invoke(target, stage);
        if (!(value instanceof Boolean b)) {
            throw new IllegalStateException("gate must return boolean: " + className);
        }
        return b;
    }

    private static Map<String, EntryRecord> readIndex() throws Exception {
        Map<String, EntryRecord> local = indexCache;
        if (local != null) {
            return local;
        }

        HashMap<String, EntryRecord> result = new HashMap<>();
        try (InputStream in = EntryBridge.class.getResourceAsStream(INDEX_PATH)) {
            if (in == null) {
                indexCache = result;
                return result;
            }

            String[] lines = new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n");
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;

                String hash = parts.length > 5 ? parts[5] : "";
                EntryRecord entry = new EntryRecord(
                        parts[1],
                        parts[2],
                        (int) Long.parseLong(parts[3], 16),
                        (int) Long.parseLong(parts[4], 16),
                        hash
                );
                result.put(parts[0], entry);
            }
        }

        indexCache = result;
        return result;
    }

    private static byte[] crypt(String className, String resourcePath, int seed, int salt, byte[] data) throws Exception {
        byte[] out = data.clone();
        int pos = 0;
        int block = 0;
        while (pos < out.length) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(className.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(resourcePath.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(intBytes(seed ^ 1315927377));
            digest.update(intBytes(salt ^ 323450161));
            digest.update(intBytes(block++));
            byte[] key = digest.digest();

            for (int i = 0; i < key.length && pos < out.length; i++, pos++) {
                out[pos] = (byte) (out[pos] ^ key[i]);
            }
        }
        return out;
    }

    private static byte[] intBytes(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
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

    private record EntryRecord(String className, String resourcePath, int seed, int salt, String hash) {
    }

    private static final class GuardedClassLoader extends ClassLoader {
        private final Map<String, byte[]> pendingDefinitions = new ConcurrentHashMap<>();

        private GuardedClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> definePayload(String className, byte[] bytes) throws ClassNotFoundException {
            String binaryName = normalizeClassName(className);
            pendingDefinitions.put(binaryName, bytes.clone());
            return loadClass(binaryName, true);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    if (pendingDefinitions.containsKey(name)) {
                        loaded = findClass(name);
                    } else {
                        loaded = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = pendingDefinitions.remove(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            definePackageIfNeeded(name);
            return defineClass(name, bytes, 0, bytes.length, EntryBridge.class.getProtectionDomain());
        }

        private static String normalizeClassName(String className) {
            return className.replace('/', '.').replace('\\', '.');
        }

        private void definePackageIfNeeded(String binaryName) {
            int dot = binaryName.lastIndexOf('.');
            if (dot <= 0) {
                return;
            }
            String pkg = binaryName.substring(0, dot);
            if (getPackage(pkg) == null) {
                definePackage(pkg, null, null, null, null, null, null, null);
            }
        }
    }
}
