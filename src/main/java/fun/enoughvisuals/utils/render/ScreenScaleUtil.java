package fun.enoughvisuals.utils.render;

import net.minecraft.client.MinecraftClient;

public final class ScreenScaleUtil {
    private static final float REFERENCE_GUI_SCALE = 2.0f;

    private ScreenScaleUtil() {
    }

    public static float scale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 1.0f;
        }
        float guiScale = (float) client.getWindow().getScaleFactor();
        if (guiScale <= 0.001f) {
            return 1.0f;
        }
        return REFERENCE_GUI_SCALE / guiScale;
    }

    public static float virtualWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0.0f;
        }
        return client.getWindow().getScaledWidth() / scale();
    }

    public static float virtualHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0.0f;
        }
        return client.getWindow().getScaledHeight() / scale();
    }

    public static float screenToVirtual(double coordinate) {
        return (float) coordinate / scale();
    }
}
