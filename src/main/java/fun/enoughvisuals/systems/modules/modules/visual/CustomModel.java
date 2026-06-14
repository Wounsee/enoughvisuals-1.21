package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(name = "Custom Model", id = "custom_model", description = "Заменяет модель игрока на 3D модель", category = Category.VISUAL)
public class CustomModel extends Module {
    private static CustomModel INSTANCE;

    public final ModeSetting model = add(new ModeSetting(
            "Модель",
            "model",
            "Модель игрока",
            "Glamrock Freddy",
            "Car",
            "Glamrock Freddy",
            "Repo",
            "Trashcan"
    ));

    public final SliderSetting size = add(new SliderSetting(
            "Размер",
            "size",
            "Масштаб модели",
            1.0,
            0.25,
            3.0,
            0.05
    ));

    public final SliderSetting yawOffset = add(new SliderSetting(
            "Поворот",
            "yaw_offset",
            "Дополнительный поворот модели по горизонтали",
            180.0,
            -180.0,
            180.0,
            5.0
    ));

    public CustomModel() {
        INSTANCE = this;
    }

    public String modelPath() {
        if (model.is("Car")) {
            return "/assets/enough/models/custom/car.glb";
        }
        if (model.is("Repo")) {
            return "/assets/enough/models/custom/repo.glb";
        }
        if (model.is("Trashcan")) {
            return "/assets/enough/models/custom/trashcan.glb";
        }
        return "/assets/enough/models/custom/glamrock_freddy.glb";
    }

    public float modelYawOffset() {
        if (model.is("Car")) {
            return 180.0f;
        }
        if (model.is("Glamrock Freddy")) {
            return 90.0f;
        }
        return 0.0f;
    }

    public static CustomModel active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
