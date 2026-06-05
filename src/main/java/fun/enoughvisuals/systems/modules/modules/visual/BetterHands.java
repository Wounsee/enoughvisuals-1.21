package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(name = "Better Hands", id = "better_hands", description = "Красивое положение и анимации рук от первого лица", category = Category.VISUAL)
public class BetterHands extends Module {

    public static BetterHands INSTANCE;

    public final ModeSetting attackMode = add(new ModeSetting(
            "Атака",
            "attack_mode",
            "Как рука ведет себя во время удара",
            "Взмах",
            "Взмах",
            "Вперед",
            "Обычная"
    ));

    public final SliderSetting rightX = add(new SliderSetting("Правая X", "right_x", "Смещение правой руки по X", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting rightY = add(new SliderSetting("Правая Y", "right_y", "Смещение правой руки по Y", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting rightZ = add(new SliderSetting("Правая Z", "right_z", "Смещение правой руки по Z", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting leftX = add(new SliderSetting("Левая X", "left_x", "Смещение левой руки по X", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting leftY = add(new SliderSetting("Левая Y", "left_y", "Смещение левой руки по Y", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting leftZ = add(new SliderSetting("Левая Z", "left_z", "Смещение левой руки по Z", 0.0, -2.0, 2.0, 0.05));

    public BetterHands() {
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        if (SwingAnimation.INSTANCE != null && SwingAnimation.INSTANCE.isEnabled()) {
            SwingAnimation.INSTANCE.disableSilent();
        }
        if (ViewModel.INSTANCE != null && ViewModel.INSTANCE.isEnabled()) {
            ViewModel.INSTANCE.disableSilent();
        }
    }

    public boolean useForwardAttack() {
        return attackMode.is("Вперед");
    }

    public boolean useNormalAttack() {
        return attackMode.is("Обычная");
    }
}
