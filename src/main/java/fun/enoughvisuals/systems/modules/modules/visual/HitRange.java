package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.HitRangeCircleRenderer;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Arrays;

@Register(name = "Hit Range", id = "hit_range", description = "Отображает радиус удара вокруг игроков", category = Category.VISUAL)
public class HitRange extends Module {

    public enum Mode { LINE, THICK, FILLED }

    private static HitRange INSTANCE;

    public final SliderSetting radius = add(new SliderSetting("Радиус", "radius", "Радиус круга в блоках", 3.0, 0.05, 5.0, 0.05));
    public final ModeSetting renderMode = add(new ModeSetting("Режим рендера", "render_mode", "Вид круга", "Thick", "Line", "Thick", "Filled"));
    public final SliderSetting thickness = add(new SliderSetting("Толщина", "thickness", "Ширина кольца в блоках (для режима Thick)", 0.15, 0.01, 2.0, 0.01));
    public final SliderSetting height = add(new SliderSetting("Высота", "height", "Смещение по высоте от ног", 0.0, 0.0, 5.0, 0.01));

    public final BooleanSetting nearestOnly = add(new BooleanSetting("Только ближайший", "nearest_only", "Показывать только для ближайшего игрока", false));
    public final BooleanSetting showSelf = add(new BooleanSetting("Показывать на себе", "show_self", "Показывать круг вокруг себя", true));
    public final SliderSetting maxSearchDistance = add(new SliderSetting("Дист. поиска", "max_search_distance", "Максимальная дистанция для поиска ближайшего игрока", 50.0, 1.0, 100.0, 1.0));
    public final SliderSetting maxDistance = add(new SliderSetting("Дист. рендера", "max_distance", "Скрывать круги дальше этого расстояния", 100.0, 1.0, 200.0, 1.0));

    public final ColorSetting color = add(new ColorSetting("Цвет", "color", "Цвет круга по умолчанию", 0x80FF0000));
    public final ColorSetting inRangeColor = add(new ColorSetting("Цвет в радиусе", "in_range_color", "Цвет круга, когда цель в радиусе удара", 0x8000FF00));
    public final BooleanSetting colorWhenInRange = add(new BooleanSetting("Цвет при сближении", "color_when_in_range", "Менять цвет, когда цель в радиусе", true));
    public final BooleanSetting randomColors = add(new BooleanSetting("Случайные цвета", "random_colors", "Уникальный цвет для каждого игрока", false));

    public final SliderSetting circleSegments = add(new SliderSetting("Сегменты круга", "circle_segments", "Количество сегментов для отрисовки круга", 60.0, 3.0, 180.0, 1.0));

    private PlayerEntity nearest;

    public HitRange() {
        INSTANCE = this;

        thickness.visibleWhen(() -> "Thick".equals(renderMode.get()));
        color.visibleWhen(() -> !randomColors.get());
        inRangeColor.visibleWhen(() -> colorWhenInRange.get() && !randomColors.get());
        colorWhenInRange.visibleWhen(() -> !randomColors.get());
        maxSearchDistance.visibleWhen(nearestOnly::get);

        radius.onChange((old, val) -> HitRangeCircleRenderer.computeAngles());
        thickness.onChange((old, val) -> HitRangeCircleRenderer.computeAngles());
        circleSegments.onChange((old, val) -> HitRangeCircleRenderer.computeAngles());
        renderMode.onChange((old, val) -> HitRangeCircleRenderer.computeAngles());
    }

    public static HitRange getInstance() {
        return INSTANCE;
    }

    public Mode mode() {
        return switch (renderMode.get()) {
            case "Line" -> Mode.LINE;
            case "Filled" -> Mode.FILLED;
            default -> Mode.THICK;
        };
    }

    public PlayerEntity getNearest() {
        return nearest;
    }

    public void setNearest(PlayerEntity nearest) {
        this.nearest = nearest;
    }

    @Override
    public void onTick() {
        if (!isInGame() || !nearestOnly.get()) {
            nearest = null;
            return;
        }

        PlayerEntity closest = null;
        double minDist = maxSearchDistance.get();
        double minDistSq = minDist * minDist;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double distSq = player.squaredDistanceTo(mc.player);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                closest = player;
            }
        }
        nearest = closest;
    }
}
