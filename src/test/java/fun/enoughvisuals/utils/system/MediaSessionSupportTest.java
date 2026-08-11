package fun.enoughvisuals.utils.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MediaSessionSupportTest {

    @Test
    void healthyWindowsInstallationKeepsMediaSessionsEnabled() {
        assertTrue(MediaSessionSupport.supportsMediaSessions(false, true, Boolean.TRUE));
    }

    @Test
    void missingRuntimeRegistrationDisablesMediaSessions() {
        assertFalse(MediaSessionSupport.supportsMediaSessions(false, true, Boolean.FALSE));
    }

    @Test
    void missingSystemLibraryDisablesMediaSessions() {
        assertFalse(MediaSessionSupport.supportsMediaSessions(false, false, Boolean.TRUE));
    }

    @Test
    void earlierNativeCrashKeepsMediaSessionsDisabled() {
        assertFalse(MediaSessionSupport.supportsMediaSessions(true, true, Boolean.TRUE));
    }

    @Test
    void unreadableRegistryDoesNotDisableAnOtherwiseHealthySystem() {
        assertTrue(MediaSessionSupport.supportsMediaSessions(false, true, null));
    }
}
