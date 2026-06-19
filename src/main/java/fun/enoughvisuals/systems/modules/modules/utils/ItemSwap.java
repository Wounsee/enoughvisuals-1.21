package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;
import java.util.function.Predicate;

@Register(name = "Item Swap", id = "item_swap", description = "Свап талисмана и сферы в левой руке", category = Category.UTILS)
public class ItemSwap extends Module {
    private static ItemSwap INSTANCE;

    private static final String[] ITEMS = {"Талисман", "Сфера"};

    private final ModeSetting from = add(new ModeSetting("С", "from", "Предмет, который будет взят в левую руку", "Талисман", ITEMS));
    private final ModeSetting to = add(new ModeSetting("На", "to", "Предмет, который будет взят, если в левой руке уже первый", "Сфера", ITEMS));
    private final BindSetting bind = add(new BindSetting("Клавиша", "bind", -1));

    // Состояние пошагового свапа (хотбар)
    private int swapStep = -1; // -1 = неактивен, 0 = выбор слота, 1 = свап, 2 = возврат
    private int swapDelay;
    private int targetHotbarSlot;
    private int originalHotbarSlot;

    // Состояние свапа через инвентарь
    private int pendingInvSlot = -1;
    private int invDelay;

    public ItemSwap() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.bind.isKey(key) || !ClientActionUtil.inGame()) return;
        INSTANCE.startSwap();
    }

    private void startSwap() {
        if (swapStep >= 0) return; // уже в процессе

        String target = getTargetName();
        Predicate<ItemStack> predicate = predicateForName(target);

        Optional<Integer> hotbar = ClientActionUtil.findHotbar(predicate);
        if (hotbar.isPresent()) {
            // Предмет в хотбаре — пошаговый свап
            targetHotbarSlot = hotbar.get();
            originalHotbarSlot = mc.player.getInventory().selectedSlot;
            swapStep = 0;
            swapDelay = 2;
            return;
        }

        // Предмет в инвентаре — открыть инвентарь, свапнуть в оффхард напрямую
        Optional<Integer> inventory = ClientActionUtil.findInventory(predicate);
        if (inventory.isPresent()) {
            int invSlot = inventory.get();
            pendingInvSlot = invSlot;
            invDelay = 3; // 3 тика = 0.15 сек
            ClientActionUtil.setSilentInventory(true);
            mc.setScreen(new InventoryScreen(mc.player));
            stopMovement();
        } else {
            ClientActionUtil.clientMessage("Нет §c" + target + " §fв инвентаре");
        }
    }

    private String getTargetName() {
        String fromName = from.get();
        String toName = to.get();
        ItemStack offhand = mc.player.getOffHandStack();
        return matches(offhand, fromName) ? toName : fromName;
    }

    @Override
    public void onTick() {
        if (!ClientActionUtil.inGame()) {
            clearAll();
            return;
        }

        // Фаза 1: свап через инвентарь напрямую в оффхард
        if (pendingInvSlot >= 0) {
            stopMovement();
            if (!(mc.currentScreen instanceof InventoryScreen)) {
                clearAll();
                return;
            }
            if (invDelay-- > 0) return;

            // Свап напрямую в оффхард (слот 40)
            if (ClientActionUtil.swapToOffhand(pendingInvSlot)) {
                pendingInvSlot = -1;
                ClientActionUtil.setSilentInventory(false);
                mc.setScreen(null);
            }
            return;
        }

        // Фаза 2: пошаговый свап (как в Pulse)
        if (swapStep >= 0) {
            stopMovement();
            if (swapDelay-- > 0) return;

            switch (swapStep) {
                case 0:
                    // Шаг 1: переключиться на слот с предметом
                    mc.player.getInventory().selectedSlot = targetHotbarSlot;
                    swapStep = 1;
                    swapDelay = 2;
                    break;
                case 1:
                    // Шаг 2: отправить SWAP_ITEM_WITH_OFFHAND (как нажатие F)
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                            BlockPos.ORIGIN,
                            Direction.DOWN
                    ));
                    swapStep = 2;
                    swapDelay = 2;
                    break;
                case 2:
                    // Шаг 3: вернуть оригинальный слот
                    mc.player.getInventory().selectedSlot = originalHotbarSlot;
                    swapStep = -1;
                    break;
            }
        }
    }

    @Override
    protected void onDisable() {
        clearAll();
    }

    private void clearAll() {
        swapStep = -1;
        swapDelay = 0;
        pendingInvSlot = -1;
        invDelay = 0;
        ClientActionUtil.setSilentInventory(false);
    }

    private void stopMovement() {
        if (mc.player == null || mc.player.input == null) return;
        mc.player.input.movementForward = 0;
        mc.player.input.movementSideways = 0;
    }

    private static Predicate<ItemStack> predicateForName(String name) {
        return switch (name) {
            case "Талисман" -> stack -> stack.isOf(Items.TOTEM_OF_UNDYING);
            case "Сфера" -> stack -> stack.isOf(Items.PLAYER_HEAD);
            default -> stack -> false;
        };
    }

    private static boolean matches(ItemStack stack, String name) {
        return predicateForName(name).test(stack);
    }
}
