package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EffectsHud extends HudElement {
    private static final float FIGMA_WIDTH = 237f;
    private static final float FIGMA_BASE_HEIGHT = 97f;
    private static final float FIGMA_ROW_STEP = 28f;
    private static final float FIGMA_TO_HUD = 1.8f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private final HudListAnimator rows = new HudListAnimator();
    private final Map<String, EffectLine> lines = new LinkedHashMap<>();
    private final Map<String, HudRollingText> timerAnimations = new LinkedHashMap<>();

    public EffectsHud(float x, float y) {
        super(x, y, 118.5f, 76.5f);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        float frameDelta = RenderUtil.getFrameDelta();
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;
        boolean editing = client.currentScreen instanceof ChatScreen;

        List<StatusEffectInstance> active = new ArrayList<>(client.player.getActiveStatusEffects().values());
        active.sort(Comparator.comparingInt(EffectsHud::sortDuration).reversed());
        if (active.isEmpty() && editing) {
            active.add(new StatusEffectInstance(StatusEffects.STRENGTH, 91 * 20, 0));
            active.add(new StatusEffectInstance(StatusEffects.SPEED, 14 * 20, 1));
            active.add(new StatusEffectInstance(StatusEffects.BLINDNESS, 4 * 20, 0));
        }

        Map<String, EffectLine> next = new LinkedHashMap<>();
        for (StatusEffectInstance effect : active) {
            String key = "effect:" + Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
            next.put(key, EffectLine.effect(effect));
        }
        DonatePotionRegistry.Match donate = DonatePotionRegistry.match(active);
        if (donate == null && editing) {
            donate = new DonatePotionRegistry.Match("\u0417\u0435\u043b\u044c\u0435 \u0410\u0441\u0441\u0430\u0441\u0438\u043d\u0430", 72 * 20);
        }
        if (donate != null) next.put("donate:" + donate.name(), EffectLine.donate(donate));

        lines.putAll(next);
        next.forEach((key, line) -> timerAnimations
                .computeIfAbsent(key, ignored -> new HudRollingText(line.time()))
                .set(line.time()));
        List<String> keys = new ArrayList<>(next.keySet());
        rows.update(keys, FIGMA_ROW_STEP * u, frameDelta);
        lines.keySet().removeIf(key -> rows.rows().stream().noneMatch(row -> row.key().equals(key)));
        timerAnimations.keySet().removeIf(key -> rows.rows().stream()
                .noneMatch(row -> row.key().equals(key)));

        List<HudListAnimator.Row> animatedRows = rows.rows();
        boolean hasRows = !animatedRows.isEmpty();
        float collapsedH = 43f * u;
        float targetH = collapsedH;
        if (hasRows) {
            HudListAnimator.Row last = animatedRows.get(animatedRows.size() - 1);
            targetH = FIGMA_BASE_HEIGHT * u + last.position();
        }
        setWidth(HudMotion.damp(getWidth(), FIGMA_WIDTH * u, 0.22f, frameDelta));
        setHeight(HudMotion.damp(getHeight(), targetH, 0.22f, frameDelta));
        setContentVisibleTarget(hasRows || getHeight() > collapsedH + 0.6f * u);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || h <= 0.5f) return;
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / FIGMA_TO_HUD;

        HudDesign.drawRounderPanel(ctx, x, y, w, h, 23f * u, alpha);
        RenderUtil.enableScissor(x, y, w, h);
        CustomFontRenderer.drawText(ctx, HudIcons.FLASK, x + 17f * u, y + 8f * u,
                26f * u, RenderUtil.withAlpha(HudDesign.ACCENT, alpha), FontWeight.HUD_ICONS);
        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.hud.effects", "Эффекты"), x + 51f * u, y + 11f * u,
                16f * u, RenderUtil.withAlpha(HudDesign.TEXT, alpha), FontWeight.MEDIUM);
        // y + 41u remains the collapse guide; the divider itself is intentionally invisible.

        float bodyStart = y + 59f * u;
        for (HudListAnimator.Row row : rows.rows()) {
            EffectLine line = lines.get(row.key());
            if (line == null) continue;
            float panelReveal = clamp01((getContentAnim() - 0.18f) / 0.82f);
            float rowAlpha = alpha * RenderUtil.easeOutQuad(row.visibility())
                    * RenderUtil.easeOutQuad(panelReveal);
            float rowY = Math.round(bodyStart + row.position());
            if (rowAlpha <= 0.01f || rowY > y + h) continue;

            if (line.donate()) {
                drawItem(ctx, line.stack(), x + 18f * u, rowY + 1f * u, 20f * u, rowAlpha);
            } else {
                Sprite sprite = client.getStatusEffectSpriteManager().getSprite(line.effect().getEffectType());
                if (sprite != null) {
                    ctx.drawSpriteStretched(net.minecraft.client.render.RenderLayer::getGuiTextured,
                            sprite, Math.round(x + 18f * u), Math.round(rowY + 1f * u),
                            Math.round(20f * u), Math.round(20f * u),
                            RenderUtil.withAlpha(0xFFFFFF, rowAlpha));
                }
            }

            int semanticColor = line.urgent() ? HudDesign.DANGER : HudDesign.ACCENT;
            HudRollingText timer = timerAnimations.get(row.key());
            float timerTextWidth = timer == null
                    ? CustomFontRenderer.getTextWidth(line.time(), 13.6f * u, FontWeight.MEDIUM)
                    : timer.width(13.6f * u, FontWeight.MEDIUM);
            float timerW = Math.max(34f * u, timerTextWidth + 10f * u);
            float timerX = x + w - 13f * u - timerW;
            String name = CustomFontRenderer.trimToWidth(line.name(),
                    Math.max(10f * u, timerX - (x + 44f * u) - 8f * u),
                    14f * u, FontWeight.MEDIUM);
            int nameColor = line.harmful() ? HudDesign.DANGER : HudDesign.TEXT;
            CustomFontRenderer.drawText(ctx, name, x + 44f * u, rowY + 4f * u,
                    14f * u, RenderUtil.withAlpha(nameColor, rowAlpha), FontWeight.MEDIUM);
            RenderUtil.drawRoundedRect(ctx, timerX, rowY, timerW, 22f * u, 6f * u,
                    RenderUtil.withAlpha(semanticColor, rowAlpha * 0.20f));
            if (timer == null) {
                CustomFontRenderer.drawCenteredText(ctx, line.time(), timerX + timerW * 0.5f,
                        rowY + 3f * u, 13.6f * u,
                        RenderUtil.withAlpha(semanticColor, rowAlpha), FontWeight.MEDIUM);
            } else {
                timer.drawCentered(ctx, timerX + timerW * 0.5f, rowY + 3f * u,
                        timerW - 4f * u, 13.6f * u, rowAlpha,
                        semanticColor, FontWeight.MEDIUM);
            }
        }
        ctx.draw();
        RenderUtil.disableScissor();
    }

    private static int sortDuration(StatusEffectInstance effect) {
        return effect.isInfinite() ? Integer.MAX_VALUE : effect.getDuration();
    }

    @Override
    protected float getHiddenContentScale() {
        return 0.84f;
    }

    private static void drawItem(DrawContext ctx, ItemStack stack, float x, float y,
                                 float size, float alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0f);
        ctx.getMatrices().scale(size / 16f, size / 16f, 1f);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawItem(stack, 0, 0);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ctx.getMatrices().pop();
    }

    private static void drawGradientText(DrawContext ctx, String text, float x, float y,
                                         float size, int from, int to, float alpha) {
        float cursor = x;
        int denominator = Math.max(1, text.length() - 1);
        for (int index = 0; index < text.length(); index++) {
            String glyph = String.valueOf(text.charAt(index));
            int color = RenderUtil.lerpColor(from, to, index / (float) denominator);
            CustomFontRenderer.drawText(ctx, glyph, cursor, y, size,
                    RenderUtil.withAlpha(color, alpha), FontWeight.MEDIUM);
            cursor += CustomFontRenderer.getTextWidth(glyph, size, FontWeight.MEDIUM);
        }
    }

    private record EffectLine(StatusEffectInstance effect, ItemStack stack, String name,
                              String time, boolean harmful, boolean urgent, boolean donate) {
        static EffectLine effect(StatusEffectInstance effect) {
            String name = effect.getEffectType().value().getName().getString()
                    + " " + roman(effect.getAmplifier() + 1);
            String time = formatTime(effect.getDuration(), effect.isInfinite());
            boolean harmful = effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
            return new EffectLine(effect, ItemStack.EMPTY, name, time, harmful,
                    !effect.isInfinite() && effect.getDuration() <= 15 * 20, false);
        }

        static EffectLine donate(DonatePotionRegistry.Match match) {
            return new EffectLine(null, new ItemStack(Items.POTION), match.name(),
                    formatTime(match.durationTicks(), false), false,
                    match.durationTicks() <= 15 * 20, true);
        }
    }

    private static String formatTime(int ticks, boolean infinite) {
        if (infinite) return "\u221E";
        int seconds = Math.max(0, ticks / 20);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String roman(int level) {
        String[] values = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level >= 1 && level <= values.length ? values[level - 1] : Integer.toString(level);
    }
}
