package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

@Register(name = "View Model", id = "view_model", description = "Настройка положения рук", category = Category.VISUAL)
public class ViewModel extends Module {

    public static ViewModel INSTANCE;

    public final SliderSetting mainTranslateX = add(new SliderSetting("Основная X", "main_translate_x", "Смещение основной руки по X", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting mainTranslateY = add(new SliderSetting("Основная Y", "main_translate_y", "Смещение основной руки по Y", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting mainTranslateZ = add(new SliderSetting("Основная Z", "main_translate_z", "Смещение основной руки по Z", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting mainRotateX = add(new SliderSetting("Поворот основной X", "main_rotate_x", "Поворот основной руки по X", 0.0, -180.0, 180.0, 1.0));
    public final SliderSetting mainRotateY = add(new SliderSetting("Поворот основной Y", "main_rotate_y", "Поворот основной руки по Y", 0.0, -180.0, 180.0, 1.0));
    public final SliderSetting mainRotateZ = add(new SliderSetting("Поворот основной Z", "main_rotate_z", "Поворот основной руки по Z", 0.0, -180.0, 180.0, 1.0));

    public final SliderSetting offTranslateX = add(new SliderSetting("Вторая X", "off_translate_x", "Смещение второй руки по X", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting offTranslateY = add(new SliderSetting("Вторая Y", "off_translate_y", "Смещение второй руки по Y", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting offTranslateZ = add(new SliderSetting("Вторая Z", "off_translate_z", "Смещение второй руки по Z", 0.0, -2.0, 2.0, 0.05));
    public final SliderSetting offRotateX = add(new SliderSetting("Поворот второй X", "off_rotate_x", "Поворот второй руки по X", 0.0, -180.0, 180.0, 1.0));
    public final SliderSetting offRotateY = add(new SliderSetting("Поворот второй Y", "off_rotate_y", "Поворот второй руки по Y", 0.0, -180.0, 180.0, 1.0));
    public final SliderSetting offRotateZ = add(new SliderSetting("Поворот второй Z", "off_rotate_z", "Поворот второй руки по Z", 0.0, -180.0, 180.0, 1.0));

    public ViewModel() {
        INSTANCE = this;
    }

    public void applyTransform(MatrixStack matrices, Arm arm) {
        boolean isMain = arm == Arm.RIGHT;

        float translateX = (isMain ? mainTranslateX.get() : offTranslateX.get()).floatValue();
        float translateY = (isMain ? mainTranslateY.get() : offTranslateY.get()).floatValue();
        float translateZ = (isMain ? mainTranslateZ.get() : offTranslateZ.get()).floatValue();

        float rotateX = (isMain ? mainRotateX.get() : offRotateX.get()).floatValue();
        float rotateY = (isMain ? mainRotateY.get() : offRotateY.get()).floatValue();
        float rotateZ = (isMain ? mainRotateZ.get() : offRotateZ.get()).floatValue();

        float direction = isMain ? 1.0f : -1.0f;
        matrices.translate(translateX * direction, translateY, translateZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
    }
}
