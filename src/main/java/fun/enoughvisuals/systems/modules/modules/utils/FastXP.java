package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.item.Items;

@Register(name = "Fast XP", id = "fast_xp", description = "Ускоряет использование пузырьков опыта", category = Category.UTILS)
public class FastXP extends Module {

    @Override
    public void onTick() {
        if (!isInGame()) return;

        // Проверяем, держит ли игрок опыт в главной или левой руке
        boolean holdingXp = mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE) ||
                mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE);

        // Если держим опыт и зажата кнопка использования (ПКМ)
        if (holdingXp && mc.options.useKey.isPressed()) {
            // Сбрасываем стандартную задержку клиента
            ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
        }
    }
}
