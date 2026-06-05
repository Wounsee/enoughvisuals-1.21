package fun.enoughvisuals.utils.system;

import org.lwjgl.glfw.GLFW;

public final class BindKeyUtil {
    public static final int NONE = -1;
    private BindKeyUtil() {}

    public static String getBindName(int key) {
        if (key == NONE) return "None";
        if (key >= 1000) {
            int button = key - 1000;
            return switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "ЛКМ";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "ПКМ";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "СКМ";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "M4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "M5";
                default -> "M" + (button + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : ("KEY_" + key);
    }

    public static int mouseButtonToCode(int button) {
        return 1000 + button;
    }
}
