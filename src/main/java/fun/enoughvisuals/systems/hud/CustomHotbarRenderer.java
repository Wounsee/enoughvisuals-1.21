package fun.enoughvisuals.systems.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.client.HUD;
import fun.enoughvisuals.ui.hud.HotbarHud;
import fun.enoughvisuals.systems.modules.modules.utils.HealingHelper;
import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.visual.MaceIndicator;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/** Inventory-HUD styled replacement for the vanilla hotbar. */
public final class CustomHotbarRenderer {
    private static final float SLOT = 22f;
    private static final float GAP = 2f;
    private static final float STEP = SLOT + GAP;
    private static final float WIDTH = SLOT * 9f + GAP * 8f;
    private static final float RADIUS = 7.25f;
    private static final float[] ITEM_POP = new float[10];

    private static float selectedPosition;
    private static int lastSelected = -1;

    private CustomHotbarRenderer() {
    }

    public static boolean shouldRender() {
        MinecraftClient client = MinecraftClient.getInstance();
        HUD hud = ModuleManager.getInstance().get(HUD.class);
        return hud != null
                && hud.isEnabled()
                && Boolean.TRUE.equals(hud.customHotbar.get())
                && client.player != null
                && client.world != null
                && client.options != null
                && !client.options.hudHidden
                && !client.player.isSpectator()
                && layout() != null;
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        int selected = MathHelper.clamp(player.getInventory().selectedSlot, 0, 8);
        float frameDelta = RenderUtil.getFrameDelta();
        if (lastSelected < 0) {
            selectedPosition = selected;
        } else if (selected != lastSelected) {
            ITEM_POP[selected] = 1f;
        }
        lastSelected = selected;
        selectedPosition = RenderUtil.animate(selectedPosition, selected, 0.24f, frameDelta);
        for (int slot = 0; slot < ITEM_POP.length; slot++) {
            ITEM_POP[slot] = RenderUtil.animate(ITEM_POP[slot], 0f, 0.14f, frameDelta);
        }

        HotbarHud layout = layout();
        if (layout == null) return;
        float baseX = layout.getX();
        float baseY = layout.getVisualY();
        float scale = layout.renderScale();

        context.getMatrices().push();
        try {
            context.getMatrices().translate(baseX, baseY, 200f);
            context.getMatrices().scale(scale, scale, 1f);
            baseX = 0f;
            baseY = 0f;

            boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            RenderSystem.disableDepthTest();
            try {
                for (int slot = 0; slot < 9; slot++) {
                    drawSlot(context, baseX + slot * STEP, baseY, 1f);
                }

                float selectionX = baseX + selectedPosition * STEP;
                HudDesign.drawRounderRect(context, selectionX, baseY, SLOT, SLOT, RADIUS,
                        RenderUtil.withAlpha(HudDesign.accent(), 0.18f));
            } finally {
                if (depthTest) RenderSystem.enableDepthTest();
            }

            for (int slot = 0; slot < 9; slot++) {
                float slotX = baseX + slot * STEP;
                ItemStack stack = player.getInventory().getStack(slot);
                drawItem(context, tickCounter, player, stack, slotX, baseY, slot, ITEM_POP[slot]);
                drawModuleOverlays(context, stack, slot, slotX, baseY);
            }

            ItemStack offhand = player.getOffHandStack();
            Arm offhandArm = player.getMainArm().getOpposite();
            if (!offhand.isEmpty()) {
                float offhandX = offhandArm == Arm.LEFT ? baseX - STEP : baseX + WIDTH + GAP;
                drawSlot(context, offhandX, baseY, 1f);
                drawItem(context, tickCounter, player, offhand, offhandX, baseY, 9, ITEM_POP[9]);
                drawModuleOverlays(context, offhand, -1, offhandX, baseY);
            }

            if (client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
                float progress = player.getAttackCooldownProgress(0f);
                if (progress < 1f) {
                    float indicatorX = offhandArm == Arm.RIGHT ? baseX - STEP : baseX + WIDTH + GAP;
                    drawAttackIndicator(context, indicatorX, baseY, progress);
                }
            }

            // Flush while the hotbar transform is still active; later HUD layers must not
            // inherit queued item/text vertices or our +200 Z transform.
            context.draw();
        } finally {
            context.getMatrices().pop();
        }
    }

    private static HotbarHud layout() {
        HudElement element = DragAndDrop.getInstance().getActiveElementByTypeId("custom_hotbar");
        return element instanceof HotbarHud hotbar ? hotbar : null;
    }

    private static void drawSlot(DrawContext context, float x, float y, float alpha) {
        HudDesign.drawItemSlot(context, x, y, SLOT, alpha, false);
    }

    private static void drawItem(DrawContext context, RenderTickCounter tickCounter,
                                 PlayerEntity player, ItemStack stack,
                                 float slotX, float slotY, int seed, float selectionPop) {
        if (stack == null || stack.isEmpty()) return;
        float bobbing = Math.max(0f,
                stack.getBobbingAnimationTime() - tickCounter.getTickDelta(false));
        float vanillaPop = MathHelper.clamp(bobbing / 5f, 0f, 1f);
        float pop = Math.max(selectionPop, vanillaPop);
        float itemScale = 1f + 0.14f * RenderUtil.easeOutCubic(pop);

        context.getMatrices().push();
        try {
            context.getMatrices().translate(slotX + SLOT * 0.5f, slotY + SLOT * 0.5f, 30f);
            context.getMatrices().scale(itemScale, itemScale, 1f);
            context.getMatrices().translate(-8f, -8f, 0f);
            context.drawItem(player, stack, 0, 0, seed + 1);
        } finally {
            context.getMatrices().pop();
        }

        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack,
                Math.round(slotX + 1f), Math.round(slotY + 1f));
    }

    private static void drawModuleOverlays(DrawContext context, ItemStack stack, int slot,
                                           float slotX, float slotY) {
        if (stack == null || stack.isEmpty()) return;
        LockSlot lockSlot = LockSlot.getInstance();
        if (slot >= 0 && lockSlot != null && lockSlot.isEnabled() && lockSlot.isLocked(slot)) {
            drawColoredOverlay(context, slotX, slotY, 0x60FF0000);
            return;
        }

        HealingHelper healing = HealingHelper.getInstance();
        if (healing != null && healing.isEnabled()) {
            int color = healing.colorForStack(stack);
            if ((color >>> 24) != 0) drawColoredOverlay(context, slotX, slotY, color);
        }

        MaceIndicator mace = MaceIndicator.getInstance();
        if (mace != null && mace.isEnabled()) {
            int color = mace.colorForStack(stack);
            if ((color >>> 24) != 0) drawColoredOverlay(context, slotX, slotY, color);
        }
    }

    private static void drawColoredOverlay(DrawContext context, float slotX, float slotY, int color) {
        HudDesign.drawRounderRect(context, slotX + 1f, slotY + 1f,
                SLOT - 2f, SLOT - 2f, RADIUS - 1f, color);
    }

    private static void drawAttackIndicator(DrawContext context, float x, float y, float progress) {
        drawSlot(context, x, y, 1f);
        RenderUtil.drawCrispSquircleProgressOutline(context, x + 1.25f, y + 1.25f,
                SLOT - 2.5f, SLOT - 2.5f, RADIUS - 0.75f,
                1.15f, MathHelper.clamp(progress, 0f, 1f), HudDesign.accent());
    }
}
