package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(name = "Pet", id = "pet", description = "Питомец на плече", category = Category.VISUAL)
public class Pet extends Module {

    private static Pet INSTANCE;

    public final ModeSetting creature = add(new ModeSetting(
            "Существо",
            "creature",
            "Существо питомца",
            "Allay",
            "Allay"
    ));

    public final SliderSetting size = add(new SliderSetting("Размер", "size", "Размер питомца", 1.0, 0.25, 2.5, 0.05));

    public Pet() {
        INSTANCE = this;
    }

    public static Pet active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
