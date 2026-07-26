package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;

@Register(name = "Health Indicator", id = "health_indicator", description = "Отображает HP игрока, которого вы атаковали", category = Category.VISUAL)
public class HealthIndicator extends Module {

    private static HealthIndicator INSTANCE;

    public static final int COLOR_RED         = 0xFFFF5555;
    public static final int COLOR_GOLD        = 0xFFFFAA00;
    public static final int COLOR_YELLOW      = 0xFFFFFF55;
    public static final int COLOR_GREEN       = 0xFF55FF55;
    public static final int COLOR_DARK_GREEN  = 0xFF00AA00;

    public final SliderSetting targetDelay = add(new SliderSetting("Время показа (сек)", "target_delay", "Сколько секунд показывать HP после последнего удара", 10.0, 1.0, 30.0, 1.0));
    public final BooleanSetting colorByHp = add(new BooleanSetting("Цвет по HP", "color_by_hp", "Окрашивать HP в зависимости от здоровья цели", true));
    public final BooleanSetting hpPrefix = add(new BooleanSetting("Префикс HP", "hp_prefix", "Показывать текст 'HP' перед цифрой", false));

    private volatile PlayerEntity target = null;
    private volatile long lastAttackMs = 0L;

    public HealthIndicator() {
        INSTANCE = this;
    }

    public static HealthIndicator getInstance() {
        return INSTANCE;
    }

    public void recordAttack(PlayerEntity victim) {
        if (victim == null) {
            return;
        }
        this.target = victim;
        this.lastAttackMs = System.currentTimeMillis();
    }

    public boolean hasActiveTarget() {
        PlayerEntity t = this.target;
        if (t == null) {
            return false;
        }
        if (!t.isAlive()) {
            return false;
        }
        long windowMs = targetDelay.get().longValue() * 1000L;
        return System.currentTimeMillis() - lastAttackMs < windowMs;
    }

    public String getDisplayText() {
        if (!hasActiveTarget()) {
            return "";
        }
        PlayerEntity t = this.target;
        if (t == null) {
            return "";
        }
        int hp = (int) t.getHealth();
        return hpPrefix.get() ? "HP " + hp : String.valueOf(hp);
    }

    public String getPlaceholderText() {
        return hpPrefix.get() ? "HP 20" : "20";
    }

    public int getCurrentHpColor() {
        if (!colorByHp.get()) {
            return 0xFFFFFFFF;
        }
        PlayerEntity t = this.target;
        if (t == null) {
            return 0xFFFFFFFF;
        }
        float hp = t.getHealth();
        if (hp <= 5.0f) {
            return COLOR_RED;
        }
        if (hp <= 10.0f) {
            return COLOR_GOLD;
        }
        if (hp <= 15.0f) {
            return COLOR_YELLOW;
        }
        if (hp <= 20.0f) {
            return COLOR_GREEN;
        }
        return COLOR_DARK_GREEN;
    }
}
