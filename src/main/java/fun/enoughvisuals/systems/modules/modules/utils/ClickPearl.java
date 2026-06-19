package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Optional;
import java.util.function.Predicate;

@Register(name = "Click Pearl", id = "click_pearl", description = "Бросает эндер-пёрл по нажатию клавиши", category = Category.UTILS)
public class ClickPearl extends Module {
    private static ClickPearl INSTANCE;

    private final BindSetting bind = add(new BindSetting("Клавиша", "bind", -1));

    // Состояние пошагового броска
    private int step = -1; // -1=неактивен, 0=свап в active, 1=бросок, 2=возврат
    private int stepDelay;
    private int originalSlot;
    private int targetSlot; // слот с перлом (хотбар) или куда свапаем (инвентарь)
    private int invSlot = -1; // слот перла в инвентаре (-1 если в хотбаре)

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
        if (hotbar.isPresent()) {
            targetSlot = hotbar.get();
            invSlot = -1;
            step = 0;
            stepDelay = 2;
            return;
        }

        Optional<Integer> inventory = ClientActionUtil.findInventory(predicate);
        if (inventory.isPresent()) {
            int slot = inventory.get();
            if (slot < 9 || slot > 35) return;
            invSlot = slot;
            targetSlot = originalSlot; // переносим в текущий active slot
            step = 0;
            stepDelay = 3;
        } else {
            ClientActionUtil.clientMessage("Нет §cэндер-пёрла §fв инвентаре");
        }
    }

    @Override
    public void onTick() {
        if (!ClientActionUtil.inGame()) {
            clearAll();
            return;
        }

        if (step < 0) return;
        stopMovement();
        if (stepDelay-- > 0) return;

        switch (step) {
            case 0:
                // Шаг 0: перенести перл в targetSlot (если из инвентаря)
                if (invSlot >= 0) {
                    if (!(mc.currentScreen instanceof InventoryScreen)) {
                        ClientActionUtil.setSilentInventory(true);
                        mc.setScreen(new InventoryScreen(mc.player));
                    }
                    if (ClientActionUtil.swapToHotbar(invSlot, targetSlot)) {
                        ClientActionUtil.setSilentInventory(false);
                        mc.setScreen(null);
                    }
                }
                mc.player.getInventory().selectedSlot = targetSlot;
                step = 1;
                stepDelay = 2;
                break;
            case 1:
                // Шаг 1: бросок
                if (mc.interactionManager != null) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                step = 2;
                stepDelay = 2;
                break;
            case 2:
                // Шаг 2: вернуть старый предмет обратно (если перл был в инвентаре)
                if (invSlot >= 0) {
                    if (!(mc.currentScreen instanceof InventoryScreen)) {
                        ClientActionUtil.setSilentInventory(true);
                        mc.setScreen(new InventoryScreen(mc.player));
                    }
                    if (ClientActionUtil.swapToHotbar(invSlot, targetSlot)) {
                        ClientActionUtil.setSilentInventory(false);
                        mc.setScreen(null);
                    }
                }
                mc.player.getInventory().selectedSlot = originalSlot;
                step = -1;
                break;
        }
    }

    @Override
    protected void onDisable() {
        clearAll();
    }

    private void clearAll() {
        step = -1;
        stepDelay = 0;
        invSlot = -1;
        ClientActionUtil.setSilentInventory(false);
    }

    private void stopMovement() {
        if (mc.player == null || mc.player.input == null) return;
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
    }
}
