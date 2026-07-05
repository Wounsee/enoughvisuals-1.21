package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.ItemCooldownEntryAccessor;
import fun.enoughvisuals.mixins.accessors.ItemCooldownManagerAccessor;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CooldownsHud extends HudElement {

    public final SliderSetting scale = addSetting(new SliderSetting("Масштаб", "scale", 1.0, 0.5, 1.5, 0.05));

    private static final float HEADER_H = 16f;
    private static final float ROW_H = 14f;
    private static final float PAD = 5f;

    public CooldownsHud(float x, float y) {
        super(x, y, 100f, 16f);
    }

    @Override
    protected boolean usesSplitAppearanceAnimation() {
        return true;
    }

    @Override
    protected void update(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float frameDelta = RenderUtil.getFrameDelta();
        float s = scale.get().floatValue();
        float minW = 100f * s;

        List<CooldownLine> lines = collectCooldowns(mc.player.getItemCooldownManager());
        boolean isEditing = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean visible = !lines.isEmpty() || isEditing;
        setContentVisibleTarget(visible);

        if (!visible) {
            float targetH = getContentAnim() <= 0.01f ? 0f : HEADER_H * s;
            setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
            setWidth(RenderUtil.animate(getWidth(), minW, 0.2f, frameDelta));
            return;
        }

        float targetH = HEADER_H * s;
        if (lines.isEmpty()) {
            targetH += ROW_H * s + 4f * s;
        } else {
            targetH += lines.size() * ROW_H * s + 4f * s;
        }

        float maxNameW = CustomFontRenderer.getTextWidth("Кулдауны", 6.5f * s, FontWeight.SEMIBOLD);
        for (CooldownLine line : lines) {
            maxNameW = Math.max(maxNameW,
                    CustomFontRenderer.getTextWidth(line.label, 6.0f * s, FontWeight.SEMIBOLD)
                            + 8f * s
                            + CustomFontRenderer.getTextWidth(line.timeText, 6.0f * s, FontWeight.SEMIBOLD));
        }

        float targetW = Math.max(minW, (PAD + 12f + 4f + PAD) * s + maxNameW);
        setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
        setWidth(RenderUtil.animate(getWidth(), targetW, 0.2f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || h < 5f) return;

        float s = scale.get().floatValue();
        float headerH = HEADER_H * s;
        float rowH = ROW_H * s;

        drawMenuStyleBackground(ctx, x, y, w, h, 6f * s, alpha);
        RenderUtil.enableScissor(x, y, w, h);

        float headerAlpha = Math.min(1f, h / headerH) * alpha * getHeaderRenderAlpha();
        RenderUtil.drawRoundedRect(ctx, x, y, w, Math.min(h, headerH), 6f * s, 6f * s, 0f, 0f,
                RenderUtil.withAlpha(0x000000, headerAlpha * 0.18f));
        CustomFontRenderer.drawText(ctx, "\uF000", x + PAD * s, y + 3.8f * s, 8.2f * s,
                RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.TIME);
        CustomFontRenderer.drawText(ctx, "Кулдауны", x + 18f * s, y + 4.8f * s, 6.5f * s,
                RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.SEMIBOLD);

        if (h <= headerH + 2f) {
            RenderUtil.disableScissor();
            return;
        }

        float bodyReveal = getBodyRevealProgress();
        if (bodyReveal <= 0.01f) {
            RenderUtil.disableScissor();
            return;
        }
        float bodyAlpha = alpha * getBodyRenderAlpha();
        RenderUtil.enableScissor(x, y + headerH, w, Math.max(0f, h - headerH) * bodyReveal);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, getBodySlideOffset() * s, 0f);

        List<CooldownLine> lines = collectCooldowns(mc.player.getItemCooldownManager());
        float currentY = y + headerH + 2f * s;

        if (lines.isEmpty()) {
            CustomFontRenderer.drawCenteredText(ctx, "Нет кулдаунов", x + w * 0.5f, currentY + 3.5f * s,
                    6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, bodyAlpha), FontWeight.SEMIBOLD);
        } else {
            for (CooldownLine line : lines) {
                if (currentY > y + h) break;

                float iconSize = 10f * s;
                float iconX = x + PAD * s;
                float iconY = currentY + (rowH - iconSize) * 0.5f + 0.1f * s;
                ctx.getMatrices().push();
                ctx.getMatrices().translate(iconX, iconY, 0f);
                ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f, 1f);
                RenderSystem.setShaderColor(1f, 1f, 1f, bodyAlpha);
                ctx.drawItem(line.stack, 0, 0);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                ctx.getMatrices().pop();

                CustomFontRenderer.drawText(ctx, line.label, x + 20f * s, currentY + 3.5f * s, 6.0f * s,
                        RenderUtil.withAlpha(0xFFFFFF, bodyAlpha), FontWeight.SEMIBOLD);
                CustomFontRenderer.drawRightText(ctx, line.timeText, x + w - PAD * s, currentY + 3.5f * s, 6.0f * s,
                        RenderUtil.withAlpha(line.color, bodyAlpha), FontWeight.SEMIBOLD);
                currentY += rowH;
            }
        }

        ctx.draw();
        ctx.getMatrices().pop();
        RenderUtil.disableScissor();
        RenderUtil.disableScissor();
    }

    private static List<CooldownLine> collectCooldowns(ItemCooldownManager manager) {
        List<CooldownLine> out = new ArrayList<>();
        if (manager == null) return out;

        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;
        Map<Identifier, Object> entries = accessor.enough$getEntries();
        if (entries == null || entries.isEmpty()) return out;

        int nowTick = accessor.enough$getTick();
        for (Map.Entry<Identifier, Object> entry : entries.entrySet()) {
            Item item = Registries.ITEM.get(entry.getKey());
            if (item == null || item == net.minecraft.item.Items.AIR) continue;

            ItemCooldownEntryAccessor cooldown = (ItemCooldownEntryAccessor) entry.getValue();
            int endTick = cooldown.enough$getEndTick();
            float remainingTicks = Math.max(0f, endTick - nowTick);
            float remainingSec = remainingTicks / 20f;
            if (remainingSec <= 0f) continue;

            String label = getItemDisplayName(item);
            String timeText = remainingSec >= 1f ? String.format("%.1f", remainingSec) : String.format("%.2f", remainingSec);
            int color = getAdaptiveColor(remainingSec);
            out.add(new CooldownLine(new ItemStack(item), label, timeText, color, remainingSec));
        }

        out.sort(Comparator.comparingDouble(CooldownLine::remainingSec).reversed());
        return out;
    }

    private static String getItemDisplayName(Item item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            var inventory = mc.player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    return stack.getName().getString();
                }
            }
        }
        return new ItemStack(item).getName().getString();
    }

    private static int getAdaptiveColor(float sec) {
        if (sec > 2.0f) return 0xFFFF4D4D;
        if (sec > 1.0f) return 0xFFFFA53A;
        if (sec > 0.45f) return 0xFFFFFF66;
        return 0xFF7CFF7C;
    }

    private record CooldownLine(ItemStack stack, String label, String timeText, int color, float remainingSec) {}
}
