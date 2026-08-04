package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;

@Register(name = "Закругления", id = "corner_style",
        description = "Выбирает форму углов интерфейса", category = Category.CLIENT)
public final class CornerStyle extends Module {
    public final ModeSetting shape = add(new ModeSetting(
            "Форма", "shape", "Обычное закругление или плавный сквиркл",
            "Сквиркл", "Обычный", "Сквиркл"));

    public CornerStyle() {
        enableSilent();
    }

    public static boolean squircle() {
        CornerStyle module = ModuleManager.getInstance().get(CornerStyle.class);
        return module == null || module.shape.is("Сквиркл");
    }

    public static float exponent() {
        return squircle() ? CustomTheme.squircleExponent() : 2.0f;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) enableSilent();
    }

    @Override
    public void toggle() {
        if (!isEnabled()) enableSilent();
    }

    @Override
    public void disable() {
    }

    @Override
    public void disableSilent() {
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!isEnabled()) enableSilent();
    }

    @Override
    public void setEnabledSilent(boolean enabled) {
        if (!isEnabled()) enableSilent();
    }
}
