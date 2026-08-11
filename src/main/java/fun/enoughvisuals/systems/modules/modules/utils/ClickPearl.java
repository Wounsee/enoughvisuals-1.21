package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Optional;
import java.util.function.Predicate;

@Register(name = "Click Pearl", id = "click_pearl", description = "Бросает эндер-пёрл по нажатию клавиши", category = Category.UTILS)
public class ClickPearl extends Module {
    private static ClickPearl INSTANCE;

    private final BindSetting bind = add(new BindSetting("Клавиша", "bind", -1));

    // Обычный видимый выбор слота -> использование -> возврат. Никаких inventory-click свапов.
    private int step = -1;
    private int stepDelay;
    private int originalSlot;
    private int targetSlot;

    public ClickPearl() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.bind.isKey(key) || !ClientActionUtil.inGame()) return;
        INSTANCE.startClick();
    }

    private void startClick() {
        if (step >= 0) return;

        Predicate<ItemStack> predicate = stack -> stack.isOf(Items.ENDER_PEARL);

        originalSlot = mc.player.getInventory().selectedSlot;

        Optional<Integer> hotbar = ClientActionUtil.findHotbar(predicate);
        if (hotbar.isEmpty()) {
            ClientActionUtil.clientMessage("Нет §cэндер-пёрла §fв хотбаре");
            return;
        }

        targetSlot = hotbar.get();
        step = 0;
        stepDelay = 0;
    }

    @Override
    public void onTick() {
        if (!ClientActionUtil.inGame()) {
            clearAll();
            return;
        }

        if (step < 0) return;
        if (stepDelay-- > 0) return;

        switch (step) {
            case 0:
                ClientActionUtil.selectHotbar(targetSlot);
                step = 1;
                stepDelay = 1;
                break;
            case 1:
                ItemStack selected = mc.player.getInventory().getStack(targetSlot);
                if (mc.player.getInventory().selectedSlot == targetSlot && selected.isOf(Items.ENDER_PEARL)) {
                    ClientActionUtil.useSelected();
                }
                step = 2;
                stepDelay = 1;
                break;
            case 2:
                ClientActionUtil.selectHotbar(originalSlot);
                step = -1;
                break;
        }
    }

    @Override
    protected void onDisable() {
        clearAll();
    }

    private void clearAll() {
        if (step >= 0 && ClientActionUtil.inGame()) {
            ClientActionUtil.selectHotbar(originalSlot);
        }
        step = -1;
        stepDelay = 0;
    }
}
