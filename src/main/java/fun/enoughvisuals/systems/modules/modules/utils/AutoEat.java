package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

@Register(name = "Auto Eat", id = "auto_eat", description = "Автоматически ест еду или отправляет команду /feed при голоде", category = Category.UTILS)
public class AutoEat extends Module {

    private static AutoEat INSTANCE;
    private static final long FEED_COMMAND_COOLDOWN_MS = 2000L;

    public final BooleanSetting useCommand = add(new BooleanSetting("Использовать команду /feed", "use_command", "Отправлять команду /feed вместо поедания еды", false));
    public final SliderSetting hungerThreshold = add(new SliderSetting("Порог голода", "hunger_threshold", "Есть, когда уровень еды падает ниже этого значения", 17.0, 1.0, 19.0, 1.0));

    private boolean eating = false;
    private int previousSlot = -1;
    private long lastCommandMs = 0L;

    public AutoEat() {
        INSTANCE = this;
    }

    public static AutoEat getInstance() {
        return INSTANCE;
    }

    public boolean isAutoEating() {
        return isEnabled() && eating;
    }

    @Override
    protected void onDisable() {
        finishEating(mc);
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            if (eating) {
                finishEating(mc);
            }
            return;
        }

        boolean screenOpen = mc.currentScreen != null;
        int hunger = mc.player.getHungerManager().getFoodLevel();
        int threshold = hungerThreshold.get().intValue();

        if (useCommand.get()) {
            if (eating) {
                finishEating(mc);
            }
            if (screenOpen) {
                return;
            }
            if (hunger <= threshold && mc.getNetworkHandler() != null) {
                long now = System.currentTimeMillis();
                if (now - lastCommandMs > FEED_COMMAND_COOLDOWN_MS) {
                    mc.getNetworkHandler().sendChatCommand("feed");
                    lastCommandMs = now;
                }
            }
            return;
        }

        // Physical eating mode
        if (eating) {
            if (mc.player.isUsingItem()) {
                return;
            }
            finishEating(mc);
            return;
        }

        if (screenOpen) {
            return;
        }

        if (hunger > threshold) {
            return;
        }

        int foodSlot = findFoodInHotbar(mc.player);
        if (foodSlot < 0) {
            return;
        }

        PlayerInventory inventory = mc.player.getInventory();
        previousSlot = inventory.selectedSlot;
        selectHotbarSlot(mc, foodSlot);
        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (result.isAccepted()) {
            eating = true;
        } else {
            selectHotbarSlot(mc, previousSlot);
            previousSlot = -1;
        }
    }

    private void selectHotbarSlot(MinecraftClient mc, int slot) {
        if (mc == null || mc.player == null) {
            return;
        }
        mc.player.getInventory().selectedSlot = slot;
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private int findFoodInHotbar(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (isFood(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.contains(DataComponentTypes.FOOD);
    }

    private void finishEating(MinecraftClient mc) {
        boolean wasEating = eating;
        eating = false;
        if (wasEating && previousSlot >= 0 && mc != null && mc.player != null) {
            selectHotbarSlot(mc, previousSlot);
        }
        previousSlot = -1;
    }
}
