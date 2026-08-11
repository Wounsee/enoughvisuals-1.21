package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class EmotionWheelScreen extends Screen {
    public EmotionWheelScreen() { super(Text.empty()); }

    @Override
    protected void init() {
        super.init();
        hideNativeCursor();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Screen initialization unlocks the mouse; keep its coordinates for the
        // radial selector but hide the platform arrow in favour of our dot.
        hideNativeCursor();
        EmotionWheelManager manager = EmotionWheelManager.getInstance();
        manager.updatePointer(mouseX, mouseY, width, height);
        manager.render(context);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        EmotionWheelManager.getInstance().updatePointer(mouseX, mouseY, width, height);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (EmotionWheelManager.getInstance().scrollPage(verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public void close() {
        EmotionWheelManager.getInstance().cancelWheel();
        super.close();
    }

    private static void hideNativeCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() != null) {
            GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }
}
