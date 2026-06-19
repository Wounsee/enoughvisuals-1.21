package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ClickSetting;
import fun.enoughvisuals.systems.modules.settings.ItemSelectSetting;
import fun.enoughvisuals.systems.modules.settings.Setting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Register(name = "Fast Swap", id = "fast_swap", description = "", category = Category.UTILS)
public class FastSwap extends Module {
    private static FastSwap INSTANCE;

    // Состояние пошагового свапа (хотбар)
    private int swapStep = -1;
    private int swapDelay;
    private int targetHotbarSlot;
    private int originalHotbarSlot;

    // Состояние переноса из инвентаря
    private int pendingInvSlot = -1;
    private int invDelay;

    private final BindSetting gapple = add(new BindSetting("Золотое яблоко", "gapple", -1));
    private final BindSetting exp = add(new BindSetting("Пузырьки опыта", "exp", -1));
    private final BindSetting totem = add(new BindSetting("Тотем", "totem", -1));
    private final BindSetting chorus = add(new BindSetting("Хорус", "chorus", -1));
    private final BindSetting trap = add(new BindSetting("Трапка", "trap", -1));
    private final BindSetting plast = add(new BindSetting("Пласт", "plast", -1));
    private final BindSetting snowball = add(new BindSetting("Снежок", "snowball", -1));

    private final List<ItemSelectSetting> customItems = new ArrayList<>();
    private final List<BindSetting> customKeys = new ArrayList<>();
    private int customSlotCount = 0;
    private static final int MAX_CUSTOM_SLOTS = 12;

    private final ClickSetting addSlot = add(new ClickSetting("+ Добавить слот", "add_slot", "Добавить кастомный слот", () -> {
        if (customSlotCount < MAX_CUSTOM_SLOTS) addCustomSlot();
    }));

    public FastSwap() {
        INSTANCE = this;
        addCustomSlot();
        addCustomSlot();
    }

    private void addCustomSlot() {
        customSlotCount++;
        int n = customSlotCount;
        ItemSelectSetting item = add(new ItemSelectSetting("Предмет " + n, "custom_item_" + n, "Выберите предмет для слота " + n, ""));
        BindSetting key = add(new BindSetting("Клавиша " + n, "custom_key_" + n, -1));
        customItems.add(item);
        customKeys.add(key);
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame() || INSTANCE.swapStep >= 0) return;
        if (INSTANCE.gapple.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)); return; }
        if (INSTANCE.exp.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.EXPERIENCE_BOTTLE)); return; }
        if (INSTANCE.totem.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.TOTEM_OF_UNDYING)); return; }
        if (INSTANCE.chorus.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.CHORUS_FRUIT)); return; }
        if (INSTANCE.trap.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.NETHERITE_SCRAP)); return; }
        if (INSTANCE.plast.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.DRIED_KELP)); return; }
        if (INSTANCE.snowball.isKey(key)) { INSTANCE.startSelect(stack -> stack.isOf(Items.SNOWBALL)); return; }
        for (int i = 0; i < INSTANCE.customItems.size(); i++) {
            ItemSelectSetting itemSetting = INSTANCE.customItems.get(i);
            BindSetting keySetting = INSTANCE.customKeys.get(i);
            if (!keySetting.isKey(key)) continue;
            String itemId = itemSetting.get();
            if (itemId == null || itemId.isBlank()) continue;
            Identifier id = Identifier.tryParse(itemId);
            if (id == null) continue;
            Item item = Registries.ITEM.get(id);
            if (item == null || item == Items.AIR) continue;
            INSTANCE.startSelect(stack -> stack.isOf(item));
            return;
        }
    }

    private void startSelect(Predicate<ItemStack> predicate) {
        java.util.Optional<Integer> hotbar = ClientActionUtil.findHotbar(predicate);
        if (hotbar.isPresent()) {
            targetHotbarSlot = hotbar.get();
            originalHotbarSlot = mc.player.getInventory().selectedSlot;
            swapStep = 0;
            swapDelay = 2;
            return;
        }

        java.util.Optional<Integer> inventory = ClientActionUtil.findInventory(predicate);
        if (inventory.isPresent()) {
            int invSlot = inventory.get();
            if (invSlot < 9 || invSlot > 35) return;
            pendingInvSlot = invSlot;
            invDelay = 3;
            ClientActionUtil.setSilentInventory(true);
            mc.setScreen(new InventoryScreen(mc.player));
            stopMovement();
        }
    }

    @Override
    public void onTick() {
        if (!ClientActionUtil.inGame()) {
            clearAll();
            return;
        }

        if (pendingInvSlot >= 0) {
            stopMovement();
            if (!(mc.currentScreen instanceof InventoryScreen)) {
                clearAll();
                return;
            }
            if (invDelay-- > 0) return;

            int freeHotbar = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    freeHotbar = i;
                    break;
                }
            }
            if (freeHotbar == -1) {
                freeHotbar = mc.player.getInventory().selectedSlot;
            }

            if (ClientActionUtil.swapToHotbar(pendingInvSlot, freeHotbar)) {
                pendingInvSlot = -1;
                ClientActionUtil.setSilentInventory(false);
                mc.setScreen(null);
                targetHotbarSlot = freeHotbar;
                originalHotbarSlot = mc.player.getInventory().selectedSlot;
                swapStep = 0;
                swapDelay = 2;
            }
            return;
        }

        if (swapStep >= 0) {
            stopMovement();
            if (swapDelay-- > 0) return;

            if (swapStep == 0) {
                mc.player.getInventory().selectedSlot = targetHotbarSlot;
                swapStep = -1;
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
}
