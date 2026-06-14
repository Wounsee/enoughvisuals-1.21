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

    public final SliderSetting spread = add(new SliderSetting("Размах", "spread", "Насколько широко руки уходят влево и вправо", 0.65, 0.55, 1.65, 0.05));
    public final SliderSetting animationDistance = add(new SliderSetting("Дальность анимации", "animation_distance", "Сила и дальность движения руки во время удара", 1.0, 0.15, 2.0, 0.05));
    public final SliderSetting animationSpeed = add(new SliderSetting("Скорость анимации", "animation_speed", "Скорость swing-анимации и мягкой инерции рук", 1.0, 0.25, 2.5, 0.05));

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

    public float animationDistance() {
        return animationDistance.get().floatValue();
    }

    public float animationSpeed() {
        return animationSpeed.get().floatValue();
    }
}
