package fun.enoughvisuals.utils.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Decides whether the Windows media-session native library may be called.
 *
 * <p>{@code MediaPlayerInfo.dll} activates the WinRT runtime class
 * {@code Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager}.
 * Trimmed Windows builds ship without that registration, and the library
 * answers by letting a C++ exception escape across the JNI boundary, which
 * kills the JVM with {@code EXCEPTION_UNCAUGHT_CXX_EXCEPTION} instead of
 * raising something Java code could catch. Every guard here therefore has to
 * run <em>before</em> the first native call.
 */
public final class MediaSessionSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("NowPlaying");

    private static final String DISABLE_PROPERTY = "enough.nowplaying.disabled";
    private static final String MEDIA_CONTROL_LIBRARY = "Windows.Media.MediaControl.dll";
    private static final String SENTINEL_FILE = "nowplaying-native.crash";

    private static final Object LOCK = new Object();

    private static Boolean available;
    private static boolean nativeProven;
    private static Path sentinel;

    private MediaSessionSupport() {}

    public static boolean isAvailable() {
        Boolean cached = available;
        if (cached != null) {
            return cached;
        }
        synchronized (LOCK) {
            if (available == null) {
                available = detect();
            }
            return available;
        }
    }

    /**
     * Records that a native call is about to start. If the JVM dies inside it
     * the marker survives, and the next launch disables the feature instead of
     * crashing again.
     */
    public static void armProbe() {
        if (nativeProven) {
            return;
        }
        synchronized (LOCK) {
            if (nativeProven) {
                return;
            }
            Path marker = sentinelPath();
            if (marker == null) {
                return;
            }
            try {
                Files.createDirectories(marker.getParent());
                Files.writeString(marker, "native media-session probe in progress");
            } catch (Throwable t) {
                LOGGER.warn("cannot write native probe marker: {}", t.toString());
            }
        }
    }

    /** Clears the marker after the native library has returned at least once. */
    public static void confirmNativeCall() {
        if (nativeProven) {
            return;
        }
        synchronized (LOCK) {
            if (nativeProven) {
                return;
            }
            nativeProven = true;
            Path marker = sentinelPath();
            if (marker == null) {
                return;
            }
            try {
                Files.deleteIfExists(marker);
            } catch (Throwable t) {
                LOGGER.warn("cannot clear native probe marker: {}", t.toString());
            }
        }
    }

    public static void disable(String reason) {
        synchronized (LOCK) {
            if (Boolean.FALSE.equals(available)) {
                return;
            }
            available = false;
            LOGGER.warn("media session support disabled: {}", reason);
        }
    }

    private static boolean detect() {
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            LOGGER.info("media session polling disabled by -D{}", DISABLE_PROPERTY);
            return false;
        }
        if (!System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
            return false;
        }

        Path marker = sentinelPath();
        boolean crashedBefore = marker != null && Files.exists(marker);
        if (crashedBefore) {
            LOGGER.warn(
                    "previous session crashed inside the media-session native library; "
                            + "keeping it disabled (delete {} to retry)",
                    marker.toAbsolutePath());
        }

        boolean libraryPresent = systemLibraryPresent();
        if (!libraryPresent) {
            LOGGER.warn(
                    "{} is missing; this Windows build has no media-session support",
                    MEDIA_CONTROL_LIBRARY);
        }

        // A trimmed build can keep the library while dropping the WinRT
        // registration, which is exactly the case that crashes the JVM.
        Boolean registered = WindowsRuntimeProbe.isMediaSessionRegistered();
        if (Boolean.FALSE.equals(registered)) {
            LOGGER.warn("media session runtime class is not registered on this system");
        }

        return supportsMediaSessions(crashedBefore, libraryPresent, registered);
    }

    /**
     * @param registered {@code null} when the registry could not be read, which
     *                   must not veto an otherwise healthy system.
     */
    static boolean supportsMediaSessions(
            boolean crashedBefore, boolean libraryPresent, Boolean registered) {
        return !crashedBefore && libraryPresent && !Boolean.FALSE.equals(registered);
    }

    private static boolean systemLibraryPresent() {
        try {
            String root = System.getenv("SystemRoot");
            Path system32 = (root == null || root.isBlank())
                    ? Paths.get("C:", "Windows", "System32")
                    : Paths.get(root, "System32");
            return Files.isRegularFile(system32.resolve(MEDIA_CONTROL_LIBRARY));
        } catch (Throwable t) {
            LOGGER.warn("cannot inspect system libraries: {}", t.toString());
            return false;
        }
    }

    private static Path sentinelPath() {
        if (sentinel != null) {
            return sentinel;
        }
        try {
            sentinel = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("enoughvisuals")
                    .resolve(SENTINEL_FILE);
        } catch (Throwable t) {
            LOGGER.warn("cannot resolve native probe marker location: {}", t.toString());
        }
        return sentinel;
    }
}
