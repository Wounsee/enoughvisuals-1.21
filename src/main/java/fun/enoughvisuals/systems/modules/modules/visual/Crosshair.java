package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;

@Register(
        name = "Crosshair",
        id = "crosshair",
        description = "Кастомный прицел",
        category = Category.VISUAL
)
public class Crosshair extends Module {

                                
    public final ModeSetting type = add(new ModeSetting(
            "Тип", "type", "Тип прицела",
            "Круг", new String[]{"Круг", "Орбиз"}
    ));

                                                                         
    public final ColorSetting color = add(new ColorSetting(
            "Цвет", "color", "Цвет",
            0xFFFFFFFF, true
    ));

    public Crosshair() {
        super();
    }
}