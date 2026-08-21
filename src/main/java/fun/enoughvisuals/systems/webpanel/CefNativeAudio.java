package fun.enoughvisuals.systems.webpanel;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefNative;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small ABI bridge for the CefBrowserHost audio mute that JCEF 116 does not expose in Java. */
final class CefNativeAudio {
    private static final String RESOURCE = "/natives/windows-x86_64/enough_cef_audio.dll";
    private static volatile Function setMuted;
    private static volatile boolean loadAttempted;

    private CefNativeAudio() {
    }

    static boolean available() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;
        if (setMuted != null) return true;
        if (loadAttempted) return false;
        synchronized (CefNativeAudio.class) {
            if (setMuted != null) return true;
            if (loadAttempted) return false;
            loadAttempted = true;
            try (InputStream input = CefNativeAudio.class.getResourceAsStream(RESOURCE)) {
                if (input == null) return false;
                Path directory = Path.of(System.getProperty("java.io.tmpdir"), "enoughvisuals-native");
                Files.createDirectories(directory);
                Path library = directory.resolve("enough_cef_audio_116.dll");
                Files.copy(input, library, StandardCopyOption.REPLACE_EXISTING);
                NativeLibrary nativeLibrary = NativeLibrary.getInstance(library.toAbsolutePath().toString());
                setMuted = nativeLibrary.getFunction("enough_cef_set_audio_muted");
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    static boolean setMuted(CefBrowser browser, boolean muted) {
        if (!(browser instanceof CefNative nativeBrowser) || !available()) return false;
        long handle = nativeBrowser.getNativeRef("CefBrowser");
        if (handle == 0L) return false;
        try {
            setMuted.invokeVoid(new Object[]{new Pointer(handle), muted ? 1 : 0});
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
