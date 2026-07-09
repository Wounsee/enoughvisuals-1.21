package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;

@Register(name = "Glow", id = "glow", description = "Model silhouette glow and fill", category = Category.VISUAL)
public class Glow extends Module {
    public static final String MODE_FILL = "Заливка";
    public static final String MODE_OUTLINE = "Обводка";

    public static Glow INSTANCE;

    public final ModeSetting mode = add(new ModeSetting(
            "Вид",
            "mode",
            "Тип свечения игрока",
            MODE_FILL,
            MODE_FILL,
            MODE_OUTLINE
    ));

    public final SliderSetting opacity = add(new SliderSetting(
            "Прозрачность",
            "opacity",
            "Насколько сильно цвет заливает модель",
            0.38,
            0.05,
            0.9,
            0.05
    ));

    public final SliderSetting strength = add(new SliderSetting(
            "Сила",
            "strength",
            "Яркость bloom вокруг контура",
            1.15,
            0.1,
            2.5,
            0.05
    ));

    public final SliderSetting radius = add(new SliderSetting(
            "Радиус",
            "radius",
            "Ширина bloom вокруг контура",
            10.0,
            2.0,
            26.0,
            0.5
    ));

    public final SliderSetting outlineWidth = add(new SliderSetting(
            "Толщина",
            "outline_width",
            "Толщина обводки по силуэту",
            1.6,
            0.5,
            5.0,
            0.25
    ));

    public final SliderSetting distance = add(new SliderSetting(
            "Дистанция",
            "distance",
            "Максимальная дистанция рендера Glow",
            64.0,
            8.0,
            128.0,
            4.0
    ));

    public final BooleanSetting self = add(new BooleanSetting(
            "Себя",
            "self",
            "Рисовать Glow на себе в третьем лице",
            true
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Color",
            "color",
            "Glow color",
            0xFFE85CFF
    ));

    public Glow() {
        INSTANCE = this;
        opacity.visibleWhen(this::isFillMode);
    }

    @Override
    protected void onDisable() {
        if (ShaderESP.active() == null) {
            ShaderEspRenderer.reset();
        }
    }

    public boolean isFillMode() {
        return mode.is(MODE_FILL);
    }

    public int modeIndex() {
        return isFillMode() ? 2 : 0;
    }

    public double distanceSquared() {
        double value = distance.get();
        return value * value;
    }

    public int color() {
        return color.get();
    }

    public static Glow active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
