package fun.enoughvisuals.utils.system;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/** Applies the native Windows dark title bar without replacing Minecraft's window. */
public final class TitleBarHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/TitleBar");
    private static final AtomicBoolean APPLIED = new AtomicBoolean();
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19;

    private TitleBarHelper() {
    }

    public static void applyOnce(MinecraftClient client) {
        if (APPLIED.get() || client == null || client.getWindow() == null
                || !System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }
        try {
            long glfwWindow = client.getWindow().getHandle();
            long hwndValue = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
            if (hwndValue == 0L) return;

            Pointer hwnd = Pointer.createConstant(hwndValue);
            IntByReference enabled = new IntByReference(1);
            int result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                    hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, enabled.getPointer(), Integer.BYTES);
            if (result != 0) {
                result = DwmApi.INSTANCE.DwmSetWindowAttribute(
                        hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, enabled.getPointer(), Integer.BYTES);
            }
            if (result == 0) APPLIED.set(true);
            else LOGGER.debug("DwmSetWindowAttribute returned {}", result);
        } catch (Throwable error) {
            LOGGER.debug("Dark title bar is unavailable: {}", error.toString());
        }
    }

    private interface DwmApi extends Library {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);

        int DwmSetWindowAttribute(Pointer hwnd, int attribute, Pointer value, int valueSize);
    }
}
