package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;

import java.util.Arrays;

@Register(name = "No Fluid", id = "no_fluid", description = "Убирает оверлей и туман при погружении в воду или лаву", category = Category.VISUAL)
public class NoFluid extends Module {

    private static NoFluid INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Режим", "mode", "Какую жидкость скрывать", "Обе", "Вода", "Лава", "Обе"));

    public NoFluid() {
        INSTANCE = this;
    }

    public static NoFluid getInstance() {
        return INSTANCE;
    }

    public boolean shouldHideWater() {
        if (!isEnabled()) return false;
        String selected = mode.get();
        return "Вода".equals(selected) || "Обе".equals(selected);
    }

    public boolean shouldHideLava() {
        if (!isEnabled()) return false;
        String selected = mode.get();
        return "Лава".equals(selected) || "Обе".equals(selected);
    }
}
