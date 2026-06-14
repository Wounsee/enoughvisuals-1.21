package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import net.minecraft.entity.effect.StatusEffects;

@Register(name="NoRender", id="no_render", description="Удаляет мусор", category = Category.VISUAL)
public class NoRender extends Module {

    public static NoRender INSTANCE;

                   
    public final BooleanSetting hurtCam = add(new BooleanSetting("Тряска при уроне", "hurt_cam", "Отключает тряску камеры при получении урона", true));
    public final BooleanSetting scoreboard = add(new BooleanSetting("Скорборд", "scoreboard", "Скрывает скорборд (таблицу справа)", false));
    public final BooleanSetting portal = add(new BooleanSetting("Эффект портала", "portal", "Убирает фиолетовый экран в портале", true));
    public final BooleanSetting fire = add(new BooleanSetting("Огонь на экране", "fire", "Убирает огонь на экране (от 1-го лица)", true));
    public final BooleanSetting pumpkin = add(new BooleanSetting("Эффект тыквы", "pumpkin", "Убирает затемнение от надетой тыквы", true));
    public final BooleanSetting breakParticles = add(new BooleanSetting("Частицы блоков", "break_particles", "Убирает частицы при ломании блоков", false));
    public final BooleanSetting weather = add(new BooleanSetting("Погода", "weather", "Скрывает дождь и снег", false));
    public final BooleanSetting fov = add(new BooleanSetting("Динамический угол обзора", "fov", "Отключает изменение угла обзора при беге", false));
    public final BooleanSetting beaconSound = add(new BooleanSetting("Звук маяка", "beacon_sound", "Заглушает звуки маяка", false));
    public final BooleanSetting phantomSound = add(new BooleanSetting("Звук фантомов", "phantom_sound", "Заглушает звуки фантомов", false));
    public final BooleanSetting weatherSound = add(new BooleanSetting("Звуки погоды", "weather_sound", "Заглушает звуки дождя и грома", false));
    public final BooleanSetting nausea = add(new BooleanSetting("Тошнота", "nausea", "Отключает эффект тошноты", true));

    private double oldFovScale = 1.0;

    public NoRender() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.options != null) {
            oldFovScale = mc.options.getFovEffectScale().getValue();
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.getFovEffectScale().setValue(oldFovScale);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        if (fov.get()) {
            mc.options.getFovEffectScale().setValue(0.0);
        }

        if (nausea.get()) {
            mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
    }
}
