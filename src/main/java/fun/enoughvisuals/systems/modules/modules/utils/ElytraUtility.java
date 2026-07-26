package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ServerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

@Register(name = "Elytra Swap", id = "elytrautility", description = "Быстрая смена нагрудника и элитры по горячей клавише", category = Category.UTILS)
public class ElytraUtility extends Module {

    private static ElytraUtility INSTANCE;
    private static final int CHEST_SLOT_ID = 6;
    private static final long SWAP_DELAY_MS = 120L;
    private static final long DEBOUNCE_MS = 200L;

    public final BindSetting keybind = add(new BindSetting("Бинд", "keybind", "Клавиша для смены нагрудника и элитры", GLFW.GLFW_KEY_R));

    private boolean swapping = false;
    private long lastActionMs = 0L;

    public ElytraUtility() {
        INSTANCE = this;
    }

    public static ElytraUtility getInstance() {
        return INSTANCE;
    }

    private boolean isServerAllowed() {
        return ServerUtil.isElytraUtilitySupported();
    }

    @Override
    public void onKeyPress(int key) {
        if (!isEnabled()) {
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof CreativeInventoryScreen)) {
            return;
        }
        if (!isServerAllowed()) {
            return;
        }
        int bound = keybind.get();
        if (bound == -1 || bound == GLFW.GLFW_KEY_UNKNOWN || key != bound) {
            return;
        }
        beginInventorySwap();
    }

    private void beginInventorySwap() {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (swapping || now - lastActionMs < DEBOUNCE_MS) {
            return;
        }

        ItemStack chestStack = mc.player.playerScreenHandler.getSlot(CHEST_SLOT_ID).getStack();
        boolean elytraEquipped = chestStack.isOf(Items.ELYTRA);
        int sourceSlotId = elytraEquipped ? findChestplateSlotId() : findElytraSlotId();
        if (sourceSlotId == -1) {
            return;
        }

        swapping = true;
        lastActionMs = now;
        mc.setScreen(new InventoryScreen(mc.player));

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(SWAP_DELAY_MS);
                mc.execute(() -> {
                    phaze$ensureCreativeInventoryTab();
                    performSwap(sourceSlotId);
                });
                Thread.sleep(SWAP_DELAY_MS + 60L);
                mc.execute(this::finishSwap);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                mc.execute(this::finishSwap);
            }
        }, "EnoughVisuals-ElytraSwap");
        worker.setDaemon(true);
        worker.start();
    }

    private void phaze$ensureCreativeInventoryTab() {
        if (mc == null || mc.player == null || !mc.player.isCreative()) {
            return;
        }
        if (mc.currentScreen instanceof CreativeInventoryScreen creativeScreen) {
            try {
                ItemGroup inventoryGroup = Registries.ITEM_GROUP.get(ItemGroups.INVENTORY);
                if (inventoryGroup == null) {
                    return;
                }
                for (Method method : creativeScreen.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 1 && method.getReturnType() == Void.TYPE) {
                        Class<?> param = method.getParameterTypes()[0];
                        if (param.isInstance(inventoryGroup)) {
                            method.setAccessible(true);
                            method.invoke(creativeScreen, inventoryGroup);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void performSwap(int sourceSlotId) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (sourceSlotId < 0 || sourceSlotId >= mc.player.playerScreenHandler.slots.size()) {
            return;
        }
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, sourceSlotId, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, CHEST_SLOT_ID, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, sourceSlotId, 0, SlotActionType.PICKUP, mc.player);
    }

    private void finishSwap() {
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }
        mc.setScreen(null);
        swapping = false;
    }

    private int findElytraSlotId() {
        if (mc.player == null) return -1;
        for (int slotId = 9; slotId <= 44; slotId++) {
            if (mc.player.playerScreenHandler.getSlot(slotId).getStack().isOf(Items.ELYTRA)) {
                return slotId;
            }
        }
        return -1;
    }

    private int findChestplateSlotId() {
        if (mc.player == null) return -1;
        for (int slotId = 9; slotId <= 44; slotId++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(slotId).getStack();
            EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST && !stack.isOf(Items.ELYTRA)) {
                return slotId;
            }
        }
        return -1;
    }
}
