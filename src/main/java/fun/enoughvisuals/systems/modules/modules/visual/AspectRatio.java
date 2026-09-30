package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(
        name = "Aspect Ratio",
        id = "aspectratio",
        description = "Изменяет соотношение сторон экрана",
        category = Category.VISUAL
)
public class AspectRatio extends Module {

    public final SliderSetting ratio = add(new SliderSetting(
            "Растяг",
            "ratio",
            "Коэффициент соотношения сторон",
            1.77, 0.1, 4.0, 0.01
    ));

    public AspectRatio() {
        super();
    }

    public float getRatio() {
        return ratio.get().floatValue();
    }

    public int getCustomWidth(int realWidth, int realHeight) {
        if (!isEnabled()) return realWidth;

        return (int) (realHeight * getRatio());
    }
}
