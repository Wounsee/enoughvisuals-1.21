package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Predicate;

@Register(name = "Fast Swap", id = "fast_swap", description = "", category = Category.UTILS)
public class FastSwap extends Module {
    private static FastSwap INSTANCE;

    private final BindSetting pearl = add(new BindSetting("Эндер-пёрл", "pearl", -1));
    private final BindSetting gapple = add(new BindSetting("Золотое яблоко", "gapple", -1));
    private final BindSetting exp = add(new BindSetting("Пузырьки опыта", "exp", -1));
    private final BindSetting totem = add(new BindSetting("Тотем", "totem", -1));
    private final BindSetting chorus = add(new BindSetting("Хорус", "chorus", -1));
    private final BindSetting trap = add(new BindSetting("Трапка", "trap", -1));
    private final BindSetting plast = add(new BindSetting("Пласт", "plast", -1));
    private final BindSetting snowball = add(new BindSetting("Снежок", "snowball", -1));

    public FastSwap() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame()) return;
        if (INSTANCE.pearl.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.ENDER_PEARL));
        if (INSTANCE.gapple.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
        if (INSTANCE.exp.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.EXPERIENCE_BOTTLE));
        if (INSTANCE.totem.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.TOTEM_OF_UNDYING));
        if (INSTANCE.chorus.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.CHORUS_FRUIT));
        if (INSTANCE.trap.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.NETHERITE_SCRAP));
        if (INSTANCE.plast.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.DRIED_KELP));
        if (INSTANCE.snowball.isKey(key)) INSTANCE.select(stack -> stack.isOf(Items.SNOWBALL));
    }

    private void select(Predicate<ItemStack> predicate) {
        ClientActionUtil.findHotbar(predicate).ifPresentOrElse(
                ClientActionUtil::selectHotbar,
                () -> ClientActionUtil.findInventory(predicate).ifPresent(slot -> ClientActionUtil.swapToHotbar(slot, mc.player.getInventory().selectedSlot))
        );
    }
}
