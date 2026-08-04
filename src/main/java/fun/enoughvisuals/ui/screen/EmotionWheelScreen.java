package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class EmotionWheelScreen extends Screen {
    public EmotionWheelScreen() { super(Text.empty()); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        EmotionWheelManager manager = EmotionWheelManager.getInstance();
        manager.updatePointer(mouseX, mouseY, width, height);
        manager.render(context);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void close() {
        EmotionWheelManager.getInstance().cancelWheel();
        super.close();
    }
}
