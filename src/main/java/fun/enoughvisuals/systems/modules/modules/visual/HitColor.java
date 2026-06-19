package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;

@Register(name = "Hit Color", id = "hit_color", description = "Изменяет цвет сущностей при получении урона", category = Category.VISUAL)
public class HitColor extends Module {

    private static HitColor INSTANCE;

    public final ModeSetting mode = add(new ModeSetting("Режим", "mode", "Что окрашивать при уроне", "Всё", "Тело", "Броня", "Всё"));
    public final ColorSetting color = add(new ColorSetting("Цвет", "color", "Цвет вспышки при получении урона", 0xFFFF0000));

    public HitColor() {
        INSTANCE = this;
    }

    public static HitColor active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public boolean shouldColorBody() {
        String m = mode.get();
        return m.equals("Тело") || m.equals("Всё");
    }

    public boolean shouldColorArmor() {
        String m = mode.get();
        return m.equals("Броня") || m.equals("Всё");
    }

    public int getHitColor() {
        return color.get();
    }

    private static boolean entityHurt = false;

    public static void setEntityHurt(boolean hurt) {
        entityHurt = hurt;
    }

    public static boolean isEntityHurt() {
        return entityHurt;
    }
}
