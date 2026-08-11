package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.GuiAlphaLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/** Borderless 9-column inventory grid from the Main/HUD Figma frame. */
public final class InventoryHud extends HudElement {
    private static final float SLOT = 22f;
    private static final float GAP = 2f;
    private static final float BASE_WIDTH = SLOT * 9f + GAP * 8f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());
    public final BooleanSetting hotbar = addSetting(new BooleanSetting(
            "Хотбар", "hotbar", "Добавляет четвёртый ряд хотбара", false));

    public InventoryHud(float x, float y) {
        super(x, y, BASE_WIDTH, SLOT * 3f + GAP * 2f);
    }

    @Override
    protected void update(float delta) {
        float s = HudDesign.scale(scale);
        int rows = hotbar.get() ? 4 : 3;
        setWidth(BASE_WIDTH * s);
        setHeight((SLOT * rows + GAP * (rows - 1)) * s);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        float s = HudDesign.scale(scale);
        int rows = hotbar.get() ? 4 : 3;
        PlayerInventory inventory = client.player.getInventory();

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                int index = row < 3 ? 9 + row * 9 + column : column;
                float slotX = x + column * (SLOT + GAP) * s;
                float slotY = y + row * (SLOT + GAP) * s;
                drawSlot(ctx, slotX, slotY, SLOT * s, alpha);
            }
        }

        GuiAlphaLayer.render(ctx, alpha, () -> {
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < 9; column++) {
                    int index = row < 3 ? 9 + row * 9 + column : column;
                    ItemStack stack = inventory.getStack(index);
                    if (stack.isEmpty()) continue;
                    float slotX = x + column * (SLOT + GAP) * s;
                    float slotY = y + row * (SLOT + GAP) * s;
                    drawStack(ctx, client, stack, slotX + 3f * s, slotY + 3f * s, s);
                }
            }
        });
    }

    private static void drawSlot(DrawContext ctx, float x, float y, float size, float alpha) {
        HudDesign.drawItemSlot(ctx, x, y, size, alpha, false);
    }

    private static void drawStack(DrawContext ctx, MinecraftClient client, ItemStack stack,
                                  float x, float y, float scale) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 30f);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawItem(stack, 0, 0);
        ctx.drawStackOverlay(client.textRenderer, stack, 0, 0);
        ctx.getMatrices().pop();
    }
}
