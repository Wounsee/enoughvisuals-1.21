package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.graffiti.GraffitiManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class GraffitiWheelScreen extends Screen {
    public GraffitiWheelScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        super.init();
        hideNativeCursor();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hideNativeCursor();
        GraffitiManager manager = GraffitiManager.getInstance();
        manager.updatePointer(mouseX, mouseY, width, height);
        manager.renderWheel(context);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        GraffitiManager.getInstance().updatePointer(mouseX, mouseY, width, height);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return GraffitiManager.getInstance().scrollPage(verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        GraffitiManager.getInstance().cancelWheel();
        super.close();
    }

    private static void hideNativeCursor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() != null) {
            GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }
}
