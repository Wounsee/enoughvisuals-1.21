package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;

@Register(name = "Gui Size", id = "gui_size", description = "Меняет размер меню", category = Category.CLIENT)
public class GuiSize extends Module {

    public final ModeSetting size = add(new ModeSetting(
            "Размер",
            "size",
            "Размер меню",
            "2",
            "1",
            "2",
            "3"
    ));

    public float percent() {
        return switch (size.get()) {
            case "1" -> 45f;
            case "3" -> 55f;
            default -> 50f;
        };
    }
}
