package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SelectSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import fun.enoughvisuals.utils.render.ShaderPlayerRenderer;

import java.util.LinkedHashSet;
import java.util.Set;

@Register(name = "ShaderPlayer", id = "shader_player", description = "Шейдерное свечение и дым вокруг игроков", category = Category.VISUAL)
public final class ShaderPlayer extends Module {
    public static final String PLAYERS = "Игроки", MOBS = "Мобы", SELF = "Себя";
    public static final String GLOW = "Свечение", SMOKE = "Дым";
    private static ShaderPlayer INSTANCE;

    public final ModeSetting style = add(new ModeSetting("Стиль", "style", "Эффект вокруг модели", GLOW, GLOW, SMOKE));
    public final SelectSetting targets = add(new SelectSetting("Цели", "targets", "",
            new LinkedHashSet<>(Set.of(PLAYERS)), PLAYERS, MOBS, SELF));
    public final SliderSetting distance = add(new SliderSetting("Дистанция", "distance", "", 96, 8, 256, 1));
    public final BooleanSetting glow = add(new BooleanSetting("Свечение", "glow", "Мягкое внешнее свечение", true));
    public final SliderSetting glowStrength = add(new SliderSetting("Сила свечения", "glow_strength", "Радиус размытия свечения", 12, 4, 20, 0.5));
    public final SliderSetting intensity = add(new SliderSetting("Интенсивность", "intensity", "Яркость эффекта", 0.85, 0.2, 1.5, 0.05));
    public final SliderSetting speed = add(new SliderSetting("Скорость", "speed", "Скорость анимации дыма и градиента", 1, 0, 3, 0.05));
    public final BooleanSetting thinOutline = add(new BooleanSetting("Тонкий контур", "thin_outline", "Чёткая линия по краю модели", false));
    public final SliderSetting opacity = add(new SliderSetting("Прозрачность", "opacity", "", 0.92, 0.05, 1, 0.01));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "color_mode", "", "Градиент", "Градиент", "Статичный"));
    public final ModeSetting colorSource = add(new ModeSetting("Источник цвета", "color_source", "", "Тема", "Тема", "Свой"));
    public final ColorSetting primary = add(new ColorSetting("Основной цвет", "primary", "", 0xFF5E7CFF));
    public final ColorSetting secondary = add(new ColorSetting("Второй цвет", "secondary", "", 0xFFAA55FF));

    public ShaderPlayer() {
        INSTANCE = this;
        glow.visibleWhen(() -> style.is(SMOKE));
        glowStrength.visibleWhen(this::hasGlow);
        primary.visibleWhen(() -> colorSource.is("Свой"));
        secondary.visibleWhen(() -> colorSource.is("Свой") && colorMode.is("Градиент"));
    }

    @Override
    protected void onDisable() {
        ShaderEspRenderer.reset();
        ShaderPlayerRenderer.resetTrail();
    }

    public static ShaderPlayer active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public boolean smoke() {
        return style.is(SMOKE);
    }

    /** The glow style always glows; smoke adds glow only when asked to. */
    public boolean hasGlow() {
        return !smoke() || glow.get();
    }

    public int primaryColor() {
        return colorSource.is("Тема") ? CustomTheme.hudAccent() : primary.get();
    }

    public int secondaryColor() {
        if (colorMode.is("Статичный")) return primaryColor();
        return colorSource.is("Тема") ? CustomTheme.hudAccentLight() : secondary.get();
    }

    public double distanceSquared() {
        double d = distance.get();
        return d * d;
    }

    public boolean self() {
        return targets.isSelected(SELF);
    }

    public ShaderPlayerRenderer.Settings settings() {
        return new ShaderPlayerRenderer.Settings(
                hasGlow(),
                smoke(),
                thinOutline.get(),
                glowStrength.get().floatValue(),
                intensity.get().floatValue(),
                speed.get().floatValue(),
                primaryColor(),
                secondaryColor(),
                !colorMode.is("Статичный"),
                opacity.get().floatValue());
    }
}
