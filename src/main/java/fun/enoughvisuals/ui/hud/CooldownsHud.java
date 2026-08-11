package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.ItemCooldownEntryAccessor;
import fun.enoughvisuals.mixins.accessors.ItemCooldownManagerAccessor;
import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudListAnimator;
import fun.enoughvisuals.systems.hud.HudMotion;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CooldownsHud extends HudElement {
    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private final HudListAnimator cards = new HudListAnimator();
    private final Map<String, CooldownLine> lines = new LinkedHashMap<>();

    public CooldownsHud(float x, float y) {
        super(x, y, 99f, 41.5f);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        float frameDelta = RenderUtil.getFrameDelta();
        float s = HudDesign.scale(scale);

        List<CooldownLine> collected = collectCooldowns(client.player.getItemCooldownManager(), delta);
        if (collected.isEmpty() && client.currentScreen instanceof ChatScreen) {
            collected = List.of(
                    new CooldownLine("preview-pearl", new ItemStack(Items.ENDER_PEARL), 0.85f),
                    new CooldownLine("preview-apple", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 16.4f),
                    new CooldownLine("preview-book", new ItemStack(Items.BOOK), 31.75f)
            );
        }

        Map<String, CooldownLine> next = new LinkedHashMap<>();
        collected.stream().limit(6).forEach(line -> next.put(line.key(), line));
        lines.putAll(next);
        List<String> keys = new ArrayList<>(next.keySet());
        cards.update(keys, 34.5f * s, frameDelta);
        lines.keySet().removeIf(key -> cards.rows().stream().noneMatch(row -> row.key().equals(key)));

        List<HudListAnimator.Row> animatedRows = cards.rows();
        boolean visible = !animatedRows.isEmpty();
        setContentVisibleTarget(visible);
        float targetW = visible
                ? animatedRows.get(animatedRows.size() - 1).position() + 30f * s
                : 0f;
        setWidth(HudMotion.damp(getWidth(), targetW, 0.22f, frameDelta));
        setHeight(HudMotion.damp(getHeight(), visible ? 41.5f * s : 0f, 0.22f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        if (h <= 0.5f) return;
        float s = HudDesign.scale(scale);
        float cardW = 30f * s;
        float cardH = 41.5f * s;

        for (HudListAnimator.Row row : cards.rows()) {
            CooldownLine line = lines.get(row.key());
            if (line == null) continue;
            float visibility = RenderUtil.easeOutQuad(row.visibility());
            float cardAlpha = alpha * visibility;
            if (cardAlpha <= 0.01f) continue;
            float cardX = x + row.position();
            float centerX = cardX + cardW * 0.5f;
            float centerY = y + cardH * 0.5f;
            float cardScale = 0.88f + 0.12f * visibility;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(centerX, centerY, 0f);
            ctx.getMatrices().scale(cardScale, cardScale, 1f);
            ctx.getMatrices().translate(-centerX, -centerY, 0f);
            RenderUtil.drawShadow(ctx, cardX, y, cardW, cardH, 8f * s, 5f * s,
                    RenderUtil.withAlpha(0x000000, cardAlpha * 0.18f));
            RenderUtil.drawRoundedRect(ctx, cardX, y, cardW, cardH, 8f * s,
                    RenderUtil.withAlpha(HudDesign.PANEL, cardAlpha * 0.94f));
            drawItem(ctx, line.stack(), cardX + 6f * s, y + 3.5f * s, 18f * s, cardAlpha);
            HudDesign.drawDivider(ctx, cardX + 6.5f * s, y + 25.5f * s, 17f * s, cardAlpha);
            String time = formatTime(line.remainingSeconds());
            float timeSize = time.length() > 5 ? 5.25f * s : 6f * s;
            CustomFontRenderer.drawCenteredText(ctx, time,
                    centerX, y + 30.5f * s, timeSize,
                    RenderUtil.withAlpha(HudDesign.ACCENT, cardAlpha), FontWeight.SEMIBOLD);
            ctx.getMatrices().pop();
        }
    }

    private static void drawItem(DrawContext ctx, ItemStack stack, float x, float y, float size, float alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0f);
        ctx.getMatrices().scale(size / 16f, size / 16f, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawItem(stack, 0, 0);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ctx.getMatrices().pop();
    }

    private static List<CooldownLine> collectCooldowns(ItemCooldownManager manager, float tickDelta) {
        List<CooldownLine> result = new ArrayList<>();
        if (manager == null) return result;
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;
        Map<Identifier, Object> entries = accessor.enough$getEntries();
        if (entries == null) return result;
        int tick = accessor.enough$getTick();
        for (Map.Entry<Identifier, Object> entry : entries.entrySet()) {
            Item item = Registries.ITEM.get(entry.getKey());
            if (item == null || item == Items.AIR) continue;
            float remainingTicks = ((ItemCooldownEntryAccessor) entry.getValue()).enough$getEndTick()
                    - tick - Math.max(0f, Math.min(1f, tickDelta));
            float remaining = Math.max(0f, remainingTicks / 20f);
            if (remaining > 0f) {
                result.add(new CooldownLine(entry.getKey().toString(), new ItemStack(item), remaining));
            }
        }
        result.sort(Comparator.comparingDouble(CooldownLine::remainingSeconds));
        return result;
    }

    private static String formatTime(float secondsFloat) {
        float seconds = Math.max(0f, secondsFloat);
        if (seconds < 1f) {
            int millis = Math.max(0, Math.round(seconds * 1_000f / 50f) * 50);
            return millis + "ms";
        }
        if (seconds < 10f) return String.format(java.util.Locale.ROOT, "%.2fs", seconds);
        if (seconds < 60f) return String.format(java.util.Locale.ROOT, "%.1fs", seconds);
        int whole = (int) Math.ceil(seconds);
        return String.format(java.util.Locale.ROOT, "%d:%02d", whole / 60, whole % 60);
    }

    private record CooldownLine(String key, ItemStack stack, float remainingSeconds) {
    }

    @Override
    protected boolean allowsDragRotation() {
        return false;
    }
}
