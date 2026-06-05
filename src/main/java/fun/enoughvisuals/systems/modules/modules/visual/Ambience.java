package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;

@Register(name = "Ambience", id = "ambience", description = "Клиентская погода и время мира", category = Category.VISUAL)
public class Ambience extends Module {

    public static Ambience INSTANCE;

    public final BooleanSetting rain = add(new BooleanSetting(
            "Дождь",
            "rain",
            "Включить клиентский дождь",
            false
    ));

    public final BooleanSetting snow = add(new BooleanSetting(
            "Снег",
            "snow",
            "Включить клиентский снег",
            false
    ));

    public final BooleanSetting time = add(new BooleanSetting(
            "Время",
            "time",
            "Менять клиентское время мира",
            true
    ));

    public final SliderSetting worldTime = add(new SliderSetting(
            "Время мира",
            "world_time",
            "0 - рассвет, 6000 - день, 12000 - закат, 18000 - ночь",
            18000.0,
            0.0,
            24000.0,
            100.0,
            "t"
    ) {
        @Override
        public boolean isVisible() {
            return time.get();
        }
    });

    public Ambience() {
        INSTANCE = this;
    }

    public boolean shouldWeather() {
        return isEnabled() && (rain.get() || snow.get());
    }

    public boolean shouldSnow() {
        return isEnabled() && snow.get();
    }

    public boolean shouldTime() {
        return isEnabled() && time.get();
    }

    public long getWorldTime() {
        return Math.round(worldTime.get()) % 24000L;
    }
}
