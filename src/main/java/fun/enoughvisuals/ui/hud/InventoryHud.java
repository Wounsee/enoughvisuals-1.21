package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class InventoryHud extends HudElement {
    private static final float BASE_W = 176f;
    private static final float HEADER_H = 17f;
    private static final float SLOT = 18f;
    private static final float PAD = 7f;

    public final SliderSetting scale = addSetting(new SliderSetting("Scale", "scale", 1.0, 0.6, 1.5, 0.05));
    public final BooleanSetting title = addSetting(new BooleanSetting("Title", "title", true));
    public final BooleanSetting hotbar = addSetting(new BooleanSetting("Hotbar", "hotbar", false));

    public InventoryHud(float x, float y) {
        super(x, y, BASE_W, 78f);
    }

    @Override
    protected void update(float delta) {
        float frameDelta = RenderUtil.getFrameDelta();
        float s = scale.get().floatValue();
        float header = title.get() ? HEADER_H * s : PAD * s;
        float rows = hotbar.get() ? 4f : 3f;
        float targetW = (PAD * 2f + SLOT * 9f) * s;
        float targetH = (header + PAD * s + SLOT * rows * s) + 1f * s;
        setWidth(RenderUtil.animate(getWidth(), targetW, 0.22f, frameDelta));
        setHeight(RenderUtil.animate(getHeight(), targetH, 0.22f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || h < 12f) {
            return;
        }

        float s = scale.get().floatValue();
        drawMenuStyleBackground(ctx, x, y, w, h, 7f * s, alpha);
        RenderUtil.enableScissor(x, y, w, h);

        float headerAlpha = alpha * getHeaderRenderAlpha();
        float contentY = y + PAD * s;
        if (title.get()) {
            CustomFontRenderer.drawText(ctx, "Inventory", x + PAD * s, y + 5.1f * s,
                    6.5f * s, RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.SEMIBOLD);
            contentY = y + (HEADER_H + 6f) * s;
        }

        float bodyReveal = getBodyRevealProgress();
        if (bodyReveal <= 0.01f) {
            RenderUtil.disableScissor();
            return;
        }
        float bodyAlpha = alpha * getBodyRenderAlpha();
        float bodyClipY = title.get() ? y + HEADER_H * s : y;
        RenderUtil.enableScissor(x, bodyClipY, w, Math.max(0f, h - (bodyClipY - y)) * bodyReveal);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, getBodySlideOffset() * s, 0f);

        PlayerInventory inventory = mc.player.getInventory();
        int rowCount = hotbar.get() ? 4 : 3;
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < 9; col++) {
                int invIndex = row < 3 ? 9 + row * 9 + col : col;
                ItemStack stack = inventory.main.get(invIndex);
                float slotX = x + PAD * s + col * SLOT * s;
                float slotY = contentY + row * SLOT * s;
                drawSlot(ctx, slotX, slotY, s, bodyAlpha);
                if (!stack.isEmpty()) {
                    drawStack(ctx, mc, stack, slotX + 1f * s, slotY + 1f * s, s, bodyAlpha);
                }
            }
        }

        ctx.draw();
        ctx.getMatrices().pop();
        RenderUtil.disableScissor();
        RenderUtil.disableScissor();
    }

    private void drawSlot(DrawContext ctx, float x, float y, float scale, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x, y, 16.8f * scale, 16.8f * scale, 3.5f * scale,
                RenderUtil.withAlpha(0xFFFFFF, alpha * 0.055f));
        RenderUtil.drawRoundedRect(ctx, x + 0.6f * scale, y + 0.6f * scale,
                15.6f * scale, 15.6f * scale, 3f * scale,
                RenderUtil.withAlpha(0x05050A, alpha * 0.22f));
    }

    private void drawStack(DrawContext ctx, MinecraftClient mc, ItemStack stack, float x, float y, float scale, float alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0f);
        ctx.getMatrices().scale(scale, scale, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawItem(stack, 0, 0);
        ctx.drawStackOverlay(mc.textRenderer, stack, 0, 0);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ctx.getMatrices().pop();
    }
}
