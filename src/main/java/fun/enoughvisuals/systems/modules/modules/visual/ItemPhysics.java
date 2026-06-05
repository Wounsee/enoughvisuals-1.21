package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Register(name = "Item Physics", id = "item_physics", description = "Делает dropped items более живыми и физичными", category = Category.VISUAL)
public class ItemPhysics extends Module {

    public static ItemPhysics INSTANCE;

    public final SliderSetting scale = add(new SliderSetting(
            "Размер",
            "scale",
            "Размер dropped item",
            0.84,
            0.45,
            1.5,
            0.01
    ));

    public final SliderSetting yOffset = add(new SliderSetting(
            "Высота",
            "y_offset",
            "Смещение предмета по высоте",
            0.10,
            0.0,
            0.5,
            0.01
    ));

    public ItemPhysics() {
        INSTANCE = this;
    }

    public void applyTransform(MatrixStack matrices) {
        float scaleValue = scale.get().floatValue();
        matrices.translate(0.0f, yOffset.get().floatValue(), 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
        matrices.scale(scaleValue, scaleValue, scaleValue);
    }
}
