package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SelectSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.LinkedHashSet;
import java.util.Set;

@Register(name = "Shader Glow", id = "glow", description = "Легитное шейдерное свечение видимых сущностей", category = Category.VISUAL)
public class Glow extends Module {
    public static final String MODE_FILL = "Заливка";
    public static final String MODE_OUTLINE = "Обводка";

    public static Glow INSTANCE;

    public static final String ENTITY_PLAYERS = "Игроки";
    public static final String ENTITY_MOBS = "Мобы";

    public final SelectSetting entities = add(new SelectSetting(
            "Сущности",
            "entities",
            "",
            new LinkedHashSet<>(Set.of(ENTITY_PLAYERS, ENTITY_MOBS)),
            ENTITY_PLAYERS,
            ENTITY_MOBS
    ));

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "",
            MODE_FILL,
            MODE_FILL,
            MODE_OUTLINE
    ));

    public final SliderSetting strength = add(new SliderSetting(
            "Сила",
            "strength",
            "Яркость внешнего свечения",
            1.15,
            0.1,
            2.5,
            0.05
    ));

    public final SliderSetting radius = add(new SliderSetting(
            "Радиус",
            "radius",
            "Радиус мягкого свечения",
            10.0,
            2.0,
            26.0,
            0.5
    ));

    public final SliderSetting outlineWidth = add(new SliderSetting(
            "Толщина",
            "outline_width",
            "Толщина обводки по силуэту",
            1.6,
            0.5,
            5.0,
            0.25
    ));

    public final BooleanSetting self = add(new BooleanSetting(
            "Себя",
            "self",
            "Показывать себя в третьем лице",
            true
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "",
            0xFFE85CFF
    ));

    public Glow() {
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        if (ShaderESP.active() == null) {
            ShaderEspRenderer.reset();
        }
    }

    public double distanceSquared() {
        return 128.0 * 128.0;
    }

    public int color() {
        return color.get();
    }

    public boolean isFillMode() {
        return mode.is(MODE_FILL);
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.isSpectator()) return false;
        if (entity == mc.player) return self.get() && entities.isSelected(ENTITY_PLAYERS);
        if (entity instanceof PlayerEntity) return entities.isSelected(ENTITY_PLAYERS);
        if (entity instanceof MobEntity) return entities.isSelected(ENTITY_MOBS);
        return false;
    }

    public static Glow active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
