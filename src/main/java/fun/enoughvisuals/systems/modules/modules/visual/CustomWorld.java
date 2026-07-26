package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import org.joml.Vector4f;

@Register(
        name = "Custom World",
        id = "custom_world",
        description = "Кастомное время, погода и туман",
        category = Category.VISUAL
)
public class CustomWorld extends Module {

    public static CustomWorld INSTANCE;

    public final BooleanSetting customTime = add(new BooleanSetting("Время", "custom_time", "Фиксирует клиентское время", true));
    public final SliderSetting time = (SliderSetting) add(new SliderSetting("Час", "time", "Время суток", 22, 0, 24, 0.5, "ч"))
            .visibleWhen(customTime::get);

    public final BooleanSetting customWeather = add(new BooleanSetting("Погода", "custom_weather", "Меняет клиентскую погоду", false));
    public final ModeSetting weather = add(new ModeSetting(
            "Погода", "weather", "Клиентская погода",
            "Дождь", "Ясно", "Дождь", "Гроза"
    ));
    public final SliderSetting weatherPower = add(new SliderSetting("Сила погоды", "weather_power", "Плотность дождя или грозы", 0.85, 0, 1, 0.05));

    public final BooleanSetting customFog = add(new BooleanSetting("Туман", "custom_fog", "Меняет цвет и дальность тумана", true));
    public final ColorSetting fogColor = (ColorSetting) add(new ColorSetting("Цвет тумана", "fog_color", "Цвет тумана мира", 0xFF521057))
            .visibleWhen(customFog::get);
    public final SliderSetting fogDistance = (SliderSetting) add(new SliderSetting("Дальность тумана", "fog_distance", "Конечная дистанция тумана", 75, 10, 250, 5))
            .visibleWhen(customFog::get);
    public final SliderSetting fogDensity = (SliderSetting) add(new SliderSetting("Плотность тумана", "fog_density", "Плотность тумана", 0.5, 0.0, 1.0, 0.01))
            .visibleWhen(customFog::get);
    public final BooleanSetting affectSky = (BooleanSetting) add(new BooleanSetting("Туман на небе", "affect_sky", "Применять кастомный туман к небесной сфере", true))
            .visibleWhen(customFog::get);

    public final BooleanSetting customSkyColor = add(new BooleanSetting("Цвет неба", "custom_sky_color", "Меняет цвет неба отдельно от тумана", true));
    public final ColorSetting skyColor = (ColorSetting) add(new ColorSetting("Цвет неба", "sky_color", "Отдельный цвет неба", 0xFF521057))
            .visibleWhen(customSkyColor::get);


    public CustomWorld() {
        INSTANCE = this;
        weather.visibleWhen(customWeather::get);
        weatherPower.visibleWhen(() -> customWeather.get() && !weather.is("Ясно"));
    }

    public static CustomWorld active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public boolean overridesWeather() {
        return customWeather.get();
    }

    public boolean shouldCancelWeatherRender() {
        return weather.is("Ясно");
    }

    public boolean shouldRain() {
        return weather.is("Дождь") || weather.is("Гроза");
    }

    public float rainGradient() {
        if (weather.is("Ясно")) return 0f;
        if (weather.is("Дождь") || weather.is("Гроза")) return weatherPower.get().floatValue();
        return -1f;
    }

    public float thunderGradient() {
        if (weather.is("Гроза")) return weatherPower.get().floatValue();
        if (weather.is("Ясно") || weather.is("Дождь")) return 0f;
        return -1f;
    }

    public long timeOfDay() {
        return Math.floorMod(Math.round(time.get() * 1000.0), 24000L);
    }

    public Fog applyFog(Fog vanilla) {
        if (!customFog.get() || vanilla == null) return vanilla;

        Vector4f color = fogColorVector();

        float end = Math.max(1.0f, fogDistance.get().floatValue());
        float start = end * (1.0f - fogDensity.get().floatValue());

        return new Fog(start, end, net.minecraft.client.render.FogShape.CYLINDER, color.x, color.y, color.z, color.w);
    }

    public boolean shouldApplyFog(Camera camera, net.minecraft.client.render.BackgroundRenderer.FogType fogType) {
        if (!customFog.get() || camera == null) return false;

        if (camera.getSubmersionType() != CameraSubmersionType.NONE) {
            return false;
        }

        if (fogType == net.minecraft.client.render.BackgroundRenderer.FogType.FOG_SKY && !affectSky.get()) {
            return false;
        }

        if (camera.getFocusedEntity() instanceof net.minecraft.entity.LivingEntity living) {
            if (living.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                    || living.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.DARKNESS)) {
                return false;
            }
        }

        return true;
    }

    public Vector4f fogColorVector() {
        int color = fogColor.get();
        return new Vector4f(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                1f
        );
    }

    public Vector4f skyColorVector() {
        int color = skyColor.get();
        return new Vector4f(
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                1f
        );
    }

    public boolean overridesSkyColor() {
        return customSkyColor.get();
    }

}
