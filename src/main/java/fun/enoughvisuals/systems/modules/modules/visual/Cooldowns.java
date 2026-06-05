package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.mixins.accessors.HandledScreenAccessor;
import fun.enoughvisuals.mixins.accessors.ItemCooldownEntryAccessor;
import fun.enoughvisuals.mixins.accessors.ItemCooldownManagerAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Map;

@Register(name = "Cooldowns", id = "cooldowns", description = "Таймер КД предметов", category = Category.VISUAL)
public class Cooldowns extends Module {

    private static Cooldowns INSTANCE;

    public Cooldowns() {
        INSTANCE = this;
    }

    public static void renderHotbarOverlay(DrawContext ctx, float tickDelta) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || mc.player == null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        int baseX = sw / 2 - 90;
        int baseY = sh - 16 - 3;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            renderCooldownForSlot(ctx, tickDelta, stack, baseX + i * 20, baseY);
        }
    }

    public static void renderInventoryOverlay(DrawContext ctx, float tickDelta, HandledScreen<?> screen) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || mc.player == null) return;

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int left = accessor.enough$getX();
        int top = accessor.enough$getY();

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.inventory != mc.player.getInventory()) continue;
            if (!slot.hasStack()) continue;
            renderCooldownForSlot(ctx, tickDelta, slot.getStack(), left + slot.x, top + slot.y);
        }
    }

    private static void renderCooldownForSlot(DrawContext ctx, float tickDelta, ItemStack stack, int slotX, int slotY) {
        if (stack.isEmpty()) return;

        float remainingSec = getRemainingSeconds(stack.getItem(), tickDelta);
        if (remainingSec <= 0f) return;

        int color = getAdaptiveColor(remainingSec);
        String text = remainingSec >= 1f ? String.format("%.1f", remainingSec) : String.format("%.2f", remainingSec);

        CustomFontRenderer.drawText(ctx, text, slotX + 1.0f, slotY - 1.0f, 6.0f, color, FontWeight.SEMIBOLD);
    }

    private static float getRemainingSeconds(Item item, float tickDelta) {
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        ItemStack probe = item.getDefaultStack();
        if (!manager.isCoolingDown(probe)) return 0f;

        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;
        Map<Identifier, Object> entries = accessor.enough$getEntries();
        Object entry = entries.get(item.getRegistryEntry().registryKey().getValue());
        if (entry == null) return 0f;

        int endTick = ((ItemCooldownEntryAccessor) entry).enough$getEndTick();
        float remainingTicks = Math.max(0f, endTick - (accessor.enough$getTick() + tickDelta));
        return remainingTicks / 20f;
    }

    private static int getAdaptiveColor(float sec) {
        if (sec > 2.0f) return 0xFFFF4D4D;
        if (sec > 1.0f) return 0xFFFFA53A;
        if (sec > 0.45f) return 0xFFFFFF66;
        return 0xFF7CFF7C;
    }
}
