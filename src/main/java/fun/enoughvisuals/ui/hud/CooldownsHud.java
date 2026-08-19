package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.ItemCooldownEntryAccessor;
import fun.enoughvisuals.mixins.accessors.ItemCooldownManagerAccessor;
import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudListAnimator;
import fun.enoughvisuals.systems.hud.HudMotion;
import fun.enoughvisuals.systems.hud.HudRollingText;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
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
    private static final float FIGMA_WIDTH = 237f;
    private static final float FIGMA_BASE_HEIGHT = 97f;
    private static final float FIGMA_ROW_STEP = 28f;
    private static final float FIGMA_TO_HUD = 1.8f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private final HudListAnimator rows = new HudListAnimator();
    private final Map<String, CooldownLine> lines = new LinkedHashMap<>();
    private final Map<String, HudRollingText> timerAnimations = new LinkedHashMap<>();

    public CooldownsHud(float x, float y) {
        super(x, y, 118.5f, 76.5f);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        float frameDelta = RenderUtil.getFrameDelta();
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;

        List<CooldownLine> collected = collectCooldowns(client.player.getItemCooldownManager(), delta);
        if (collected.isEmpty() && client.currentScreen instanceof ChatScreen) {
            collected = List.of(
                    preview("preview-pearl", Items.ENDER_PEARL, 0.85f),
                    preview("preview-apple", Items.ENCHANTED_GOLDEN_APPLE, 16.4f),
                    preview("preview-book", Items.BOOK, 31.75f)
            );
        }

        Map<String, CooldownLine> next = new LinkedHashMap<>();
        collected.stream().limit(6).forEach(line -> next.put(line.key(), line));
        lines.putAll(next);
        next.forEach((key, line) -> timerAnimations
                .computeIfAbsent(key, ignored -> new HudRollingText(formatTime(line.remainingSeconds())))
                .set(formatTime(line.remainingSeconds())));
        List<String> keys = new ArrayList<>(next.keySet());
        rows.update(keys, FIGMA_ROW_STEP * u, frameDelta);
        List<HudListAnimator.Row> animatedRows = rows.rows();
        lines.keySet().removeIf(key -> animatedRows.stream().noneMatch(row -> row.key().equals(key)));
        timerAnimations.keySet().removeIf(key -> animatedRows.stream()
                .noneMatch(row -> row.key().equals(key)));

        boolean hasRows = !animatedRows.isEmpty();
        float collapsedH = 43f * u;
        float targetH = collapsedH;
        if (hasRows) {
            targetH = FIGMA_BASE_HEIGHT * u
                    + animatedRows.get(animatedRows.size() - 1).position();
        }
        setWidth(HudMotion.damp(getWidth(), FIGMA_WIDTH * u, 0.22f, frameDelta));
        setHeight(HudMotion.damp(getHeight(), targetH, 0.22f, frameDelta));
        setContentVisibleTarget(hasRows || getHeight() > collapsedH + 0.6f * u);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        if (h <= 0.5f) return;
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;

        HudDesign.drawRounderPanel(ctx, x, y, w, h, 23f * u, alpha);
        RenderUtil.enableScissor(x, y, w, h);
        CustomFontRenderer.drawText(ctx, HudIcons.CLOCK, x + 17f * u, y + 8f * u,
                25f * u, RenderUtil.withAlpha(HudDesign.ACCENT, alpha), FontWeight.HUD_ICONS);
        CustomFontRenderer.drawText(ctx, ClientLanguage.text("settings.hud.cooldowns.name", "Кулдауны"),
                x + 51f * u, y + 11f * u, 16f * u,
                RenderUtil.withAlpha(HudDesign.TEXT, alpha), FontWeight.MEDIUM);
        // y + 41u remains the collapse guide; the divider itself is intentionally invisible.

        float bodyStart = y + 59f * u;
        for (HudListAnimator.Row row : rows.rows()) {
            CooldownLine line = lines.get(row.key());
            if (line == null) continue;
            float panelReveal = clamp01((getContentAnim() - 0.18f) / 0.82f);
            float rowAlpha = alpha * RenderUtil.easeOutQuad(row.visibility())
                    * RenderUtil.easeOutQuad(panelReveal);
            float rowY = Math.round(bodyStart + row.position());
            if (rowAlpha <= 0.01f || rowY > y + h) continue;

            drawItem(ctx, line.stack(), x + 18f * u, rowY + 1f * u, 20f * u, rowAlpha);
            String time = formatTime(line.remainingSeconds());
            HudRollingText timer = timerAnimations.get(row.key());
            float timerTextWidth = timer == null
                    ? CustomFontRenderer.getTextWidth(time, 13.6f * u, FontWeight.MEDIUM)
                    : timer.width(13.6f * u, FontWeight.MEDIUM);
            float timerW = Math.max(34f * u, timerTextWidth + 10f * u);
            float timerX = x + w - 13f * u - timerW;
            String name = CustomFontRenderer.trimToWidth(line.name(),
                    Math.max(10f * u, timerX - (x + 44f * u) - 8f * u),
                    14f * u, FontWeight.MEDIUM);
            CustomFontRenderer.drawText(ctx, name, x + 44f * u, rowY + 4f * u,
                    14f * u, RenderUtil.withAlpha(HudDesign.TEXT, rowAlpha), FontWeight.MEDIUM);
            RenderUtil.drawRoundedRect(ctx, timerX, rowY, timerW, 22f * u, 6f * u,
                    RenderUtil.withAlpha(HudDesign.ACCENT, rowAlpha * 0.20f));
            if (timer == null) {
                CustomFontRenderer.drawCenteredText(ctx, time, timerX + timerW * 0.5f,
                        rowY + 3f * u, 13.6f * u,
                        RenderUtil.withAlpha(HudDesign.ACCENT, rowAlpha), FontWeight.MEDIUM);
            } else {
                timer.drawCentered(ctx, timerX + timerW * 0.5f, rowY + 3f * u,
                        timerW - 4f * u, 13.6f * u, rowAlpha,
                        HudDesign.ACCENT, FontWeight.MEDIUM);
            }
        }
        ctx.draw();
        RenderUtil.disableScissor();
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
                ItemStack stack = new ItemStack(item);
                result.add(new CooldownLine(entry.getKey().toString(), stack,
                        stack.getName().getString(), remaining));
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

    private static CooldownLine preview(String key, Item item, float remainingSeconds) {
        ItemStack stack = new ItemStack(item);
        return new CooldownLine(key, stack, stack.getName().getString(), remainingSeconds);
    }

    private record CooldownLine(String key, ItemStack stack, String name, float remainingSeconds) {
    }

    @Override
    protected float getHiddenContentScale() {
        return 0.84f;
    }
}
