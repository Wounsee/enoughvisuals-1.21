package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import org.lwjgl.glfw.GLFW;

@Register(name = "Emotions", id = "emotions", description = "Колесо анимаций персонажа", category = Category.VISUAL)
public final class Emotions extends Module {
    private static Emotions instance;
    private final BindSetting wheelBind = add(new BindSetting("Клавиша колеса", "wheel_bind", "Удерживайте для выбора анимации", GLFW.GLFW_KEY_V));

    public Emotions() {
        instance = this;
        enableSilent();
    }
    public static Emotions getInstance() { return instance; }
    public int wheelKey() { return wheelBind.get(); }

    @Override public void onTick() { EmotionWheelManager.getInstance().tick(); }
    @Override protected void onDisable() { EmotionWheelManager.getInstance().stopAll(); }
}
