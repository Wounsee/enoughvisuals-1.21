package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.GuiAlphaLayer;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Borderless four-slot armor strip matching the inventory grid. */
public final class ArmorHud extends HudElement {
    private static final float SLOT = 22f;
    private static final float GAP = 2f;
    private static final float BASE_WIDTH = SLOT * 4f + GAP * 3f;
    private static final float BASE_HEIGHT = SLOT;
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    public ArmorHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        float s = HudDesign.scale(scale);
        setWidth(BASE_WIDTH * s);
        setHeight(BASE_HEIGHT * s);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        float s = HudDesign.scale(scale);

        for (int i = 0; i < SLOTS.length; i++) {
            float slotX = x + i * (SLOT + GAP) * s;
            drawSlot(ctx, slotX, y, SLOT * s, alpha);
        }

        GuiAlphaLayer.render(ctx, alpha, () -> {
            for (int i = 0; i < SLOTS.length; i++) {
                ItemStack stack = client.player.getEquippedStack(SLOTS[i]);
                if (stack.isEmpty()) continue;
                float slotX = x + i * (SLOT + GAP) * s;
                drawStack(ctx, stack, slotX + 3f * s, y + 3f * s, s);
            }
        });

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = client.player.getEquippedStack(SLOTS[i]);
            if (stack.isEmpty()) continue;
            float slotX = x + i * (SLOT + GAP) * s;
            drawDurability(ctx, stack, slotX + 3f * s, y + 19.1f * s,
                    16f * s, 1.2f * s, alpha);
        }
    }

    private static void drawSlot(DrawContext ctx, float x, float y, float size, float alpha) {
        HudDesign.drawItemSlot(ctx, x, y, size, alpha, false);
    }

    private static void drawStack(DrawContext ctx, ItemStack stack, float x, float y, float scale) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 30f);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();
    }

    private static void drawDurability(DrawContext ctx, ItemStack stack, float x, float y,
                                       float width, float height, float alpha) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) return;
        float remaining = 1f - stack.getDamage() / (float) stack.getMaxDamage();
        int color = remaining < 0.22f ? HudDesign.DANGER : HudDesign.accent();
        RenderUtil.drawRoundedRect(ctx, x, y, width, height, height * 0.5f,
                RenderUtil.withAlpha(HudDesign.TRACK, alpha * 0.72f));
        RenderUtil.drawRoundedRect(ctx, x, y, width * Math.max(0f, remaining), height, height * 0.5f,
                RenderUtil.withAlpha(color, alpha));
    }
}
