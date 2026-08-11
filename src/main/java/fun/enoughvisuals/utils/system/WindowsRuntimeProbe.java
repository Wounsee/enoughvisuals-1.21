package fun.enoughvisuals.utils.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the WinRT activation registry through JNA, which Minecraft already
 * ships. JNA is kept in this dedicated class so a missing or incompatible
 * version degrades to "unknown" instead of breaking media-session detection.
 */
final class WindowsRuntimeProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger("NowPlaying");

    private static final String ACTIVATABLE_CLASS_KEY =
            "SOFTWARE\\Microsoft\\WindowsRuntime\\ActivatableClassId\\"
                    + "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";

    private WindowsRuntimeProbe() {}

    /** {@code null} when the registry could not be inspected at all. */
    static Boolean isMediaSessionRegistered() {
        try {
            return queryRegistry();
        } catch (Throwable t) {
            LOGGER.warn("cannot read WinRT activation registry: {}", t.toString());
            return null;
        }
    }

    private static Boolean queryRegistry() {
        return com.sun.jna.platform.win32.Advapi32Util.registryKeyExists(
                com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE,
                ACTIVATABLE_CLASS_KEY);
    }
}
