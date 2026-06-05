package fun.enoughvisuals.utils.player;

import fun.enoughvisuals.utils.font.TextGradientUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public final class ClientActionUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private ClientActionUtil() {
    }

    public static boolean inGame() {
        return mc.player != null && mc.world != null;
    }

    public static void clientMessage(String message) {
        if (mc.inGameHud != null) {
            MutableText prefix = TextGradientUtil.gradient("EnoughVisuals \u00BB ", 0x6D3BFF, 0xD2D5FF);
            MutableText body = Text.literal(message == null ? "" : message).setStyle(Style.EMPTY.withColor(0xFFFFFF));
            mc.inGameHud.getChatHud().addMessage(prefix.append(body));
        }
    }

    public static void sendCommand(String command) {
        if (mc.player == null || mc.player.networkHandler == null || command == null) return;
        String clean = command.trim();
        while (clean.startsWith("/")) clean = clean.substring(1);
        if (!clean.isBlank()) mc.player.networkHandler.sendChatCommand(clean);
    }

    public static Optional<Integer> findHotbar(Predicate<ItemStack> predicate) {
        if (mc.player == null) return Optional.empty();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && predicate.test(stack)) return Optional.of(i);
        }
        return Optional.empty();
    }

    public static Optional<Integer> findInventory(Predicate<ItemStack> predicate) {
        if (mc.player == null) return Optional.empty();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && predicate.test(stack)) return Optional.of(i);
        }
        return Optional.empty();
    }

    public static void selectHotbar(int slot) {
        if (mc.player != null && slot >= 0 && slot <= 8) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    public static boolean useSelected() {
        if (!inGame() || mc.interactionManager == null) return false;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        return true;
    }

    public static boolean isItemId(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty() || id == null || id.isBlank()) return false;
        Identifier identifier = Registries.ITEM.getId(stack.getItem());
        return identifier != null && identifier.toString().equalsIgnoreCase(id);
    }

    public static boolean contains(ItemStack stack, String text) {
        if (stack == null || stack.isEmpty() || text == null) return false;
        String haystack = (Registries.ITEM.getId(stack.getItem()) + " " + stack.getName().getString()).toLowerCase(Locale.ROOT);
        return haystack.contains(text.toLowerCase(Locale.ROOT));
    }

    public static int inventoryToScreenSlot(int inventorySlot) {
        return inventorySlot >= 0 && inventorySlot <= 8 ? 36 + inventorySlot : inventorySlot;
    }

    public static boolean swapToHotbar(int inventorySlot, int hotbarSlot) {
        if (!inGame() || mc.interactionManager == null || hotbarSlot < 0 || hotbarSlot > 8) return false;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                inventoryToScreenSlot(inventorySlot),
                hotbarSlot,
                SlotActionType.SWAP,
                mc.player
        );
        return true;
    }

    public static boolean swapToOffhand(int inventorySlot) {
        if (!inGame() || mc.interactionManager == null) return false;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                inventoryToScreenSlot(inventorySlot),
                40,
                SlotActionType.SWAP,
                mc.player
        );
        return true;
    }
}
