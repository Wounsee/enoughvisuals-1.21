package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderHandsRenderer;

@Register(name = "Echo Hands", id = "echo_hands", description = "Отстающий glow вокруг рук и предметов", category = Category.VISUAL)
public class EchoHands extends Module {

    public static EchoHands INSTANCE;

    public final SliderSetting strength = add(new SliderSetting(
            "Сила",
            "strength",
            "Яркость отстающего свечения",
            0.75,
            0.05,
            1.5,
            0.05
    ));

    public final SliderSetting radius = add(new SliderSetting(
            "Радиус",
            "radius",
            "Насколько широко glow разливается вокруг рук",
            11.0,
            2.0,
            28.0,
            0.5
    ));

    public final SliderSetting trail = add(new SliderSetting(
            "Хвост",
            "trail",
            "Как долго прошлый кадр остается вокруг рук",
            0.84,
            0.55,
            0.96,
            0.01
    ));

    public final SliderSetting lag = add(new SliderSetting(
            "Отставание",
            "lag",
            "Сила дрожащего screen-space отставания glow",
            1.15,
            0.0,
            3.0,
            0.05
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Цвет отстающего свечения",
            0xC8E6DDD2
    ));

    public EchoHands() {
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        if (ShaderHands.active() == null) {
            ShaderHandsRenderer.reset();
        } else {
            ShaderHandsRenderer.resetEcho();
        }
    }

    public static EchoHands active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
