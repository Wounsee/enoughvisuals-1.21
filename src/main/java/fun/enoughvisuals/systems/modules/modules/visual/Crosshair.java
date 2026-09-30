package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(
        name = "Crosshair",
        id = "crosshair",
        description = "Кастомный прицел",
        category = Category.VISUAL
)
public class Crosshair extends Module {

    public final ModeSetting type = add(new ModeSetting(
            "Тип", "type", "Тип прицела",
            "Круг", new String[]{"Круг", "Орбиз", "Крест"}
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет", "color", "Цвет",
            0xFFFFFFFF, true
    ));

    public final SliderSetting crossGap = add(new SliderSetting(
            "Зазор", "cross_gap", "Расстояние от центра", 3.0, -2.0, 12.0, 0.25));
    public final SliderSetting crossLength = add(new SliderSetting(
            "Длина", "cross_length", "Длина лучей", 5.0, 1.0, 16.0, 0.25));
    public final SliderSetting crossThickness = add(new SliderSetting(
            "Толщина", "cross_thickness", "Толщина лучей", 1.0, 0.5, 4.0, 0.25));
    public final BooleanSetting crossOutline = add(new BooleanSetting(
            "Обводка", "cross_outline", "Обводка креста", true));
    public final SliderSetting crossOutlineWidth = add(new SliderSetting(
            "Толщина обводки", "cross_outline_width", "Толщина обводки", 0.75, 0.25, 3.0, 0.25));
    public final ColorSetting crossOutlineColor = add(new ColorSetting(
            "Цвет обводки", "cross_outline_color", "Цвет обводки", 0xFF000000, true));
    public final BooleanSetting crossDot = add(new BooleanSetting(
            "Точка", "cross_dot", "Точка в центре", false));
    public final SliderSetting crossDotSize = add(new SliderSetting(
            "Размер точки", "cross_dot_size", "Размер центральной точки", 1.0, 0.5, 4.0, 0.25));
    public final BooleanSetting crossTop = add(new BooleanSetting(
            "Верхний луч", "cross_top", "Показывать верхнюю часть креста", true));
    public final BooleanSetting crossDynamic = add(new BooleanSetting(
            "Динамический", "cross_dynamic", "Расширять прицел во время атаки", false));
    public final SliderSetting crossDynamicAmount = add(new SliderSetting(
            "Динамика", "cross_dynamic_amount", "Сила расширения", 4.0, 0.0, 12.0, 0.25));

    public Crosshair() {
        super();
        crossGap.visibleWhen(this::isCross);
        crossLength.visibleWhen(this::isCross);
        crossThickness.visibleWhen(this::isCross);
        crossOutline.visibleWhen(this::isCross);
        crossOutlineWidth.visibleWhen(() -> isCross() && crossOutline.get());
        crossOutlineColor.visibleWhen(() -> isCross() && crossOutline.get());
        crossDot.visibleWhen(this::isCross);
        crossDotSize.visibleWhen(() -> isCross() && crossDot.get());
        crossTop.visibleWhen(this::isCross);
        crossDynamic.visibleWhen(this::isCross);
        crossDynamicAmount.visibleWhen(() -> isCross() && crossDynamic.get());
    }

    private boolean isCross() {
        return type.is("Крест");
    }
}
