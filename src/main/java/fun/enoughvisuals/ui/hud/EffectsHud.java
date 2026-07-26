package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectsHud extends HudElement {

    // Добавляем настройку масштаба прямо в модуль
    public final SliderSetting scale = addSetting(new SliderSetting("Масштаб", "scale", 1.0, 0.5, 1.5, 0.05));

    public EffectsHud(float x, float y) {
        super(x, y, 100f, 16f);
    }

    @Override
    protected boolean usesSplitAppearanceAnimation() {
        return true;
    }

    private String getRomanNumeral(int amplifier) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (amplifier >= 0 && amplifier < numerals.length) return numerals[amplifier];
        return String.valueOf(amplifier + 1);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        float frameDelta = RenderUtil.getFrameDelta();

        float s = scale.get().floatValue(); // Получаем текущий масштаб
        float headerH = 16f * s;
        float rowH = 14f * s;
        float minW = 100f * s;

        Collection<StatusEffectInstance> effects = mc.player.getActiveStatusEffects().values();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;
        boolean visible = !effects.isEmpty() || isEditing;
        setContentVisibleTarget(visible);

        // Если эффектов нет и мы не в чате - схлопываем
        if (!visible) {
            float targetH = getContentAnim() <= 0.01f ? 0f : headerH;
            setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
            setWidth(RenderUtil.animate(getWidth(), minW, 0.2f, frameDelta));
            return;
        }

        // Высота с учетом масштаба и заглушки
        float targetH = headerH;
        if (effects.isEmpty() && isEditing) {
            targetH += rowH + (4f * s);
        } else if (!effects.isEmpty()) {
            targetH += (effects.size() * rowH) + (4f * s);
        }

        // Считаем ширину текста
        float maxNameW = 0f;
        if (effects.isEmpty() && isEditing) {
            maxNameW = CustomFontRenderer.getTextWidth("Нет эффектов", 6.0f * s, FontWeight.SEMIBOLD);
        } else {
            for (StatusEffectInstance effect : effects) {
                String name = effect.getEffectType().value().getName().getString();
                int amp = effect.getAmplifier();
                if (amp > 0) name += " " + getRomanNumeral(amp);
                maxNameW = Math.max(maxNameW, CustomFontRenderer.getTextWidth(name, 6.0f * s, FontWeight.SEMIBOLD));
            }
        }

        // Отступы (масштабированные): 5 (лев) + 9 (иконка) + 4 (пробел) + Имя + 6 (пробел) + 18 (время) + 5 (прав)
        float targetW = Math.max(minW, (5f + 9f + 4f + 6f + 18f + 5f) * s + maxNameW);

        setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
        setWidth(RenderUtil.animate(getWidth(), targetW, 0.2f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || h < 5f) return;

        float s = scale.get().floatValue();
        float headerH = 16f * s;
        float rowH = 14f * s;

        // Фон с закруглением, зависящим от масштаба
        drawMenuStyleBackground(ctx, x, y, w, h, 6f * s, alpha);

        RenderUtil.enableScissor(x, y, w, h);

        float headerAlpha = Math.min(1f, h / headerH) * alpha * getHeaderRenderAlpha();
        RenderUtil.drawRoundedRect(ctx, x, y, w, Math.min(h, headerH), 6f * s, 6f * s, 0f, 0f,
                RenderUtil.withAlpha(0x000000, headerAlpha * 0.18f));

        // Заголовок
        CustomFontRenderer.drawText(ctx, "\uF002", x + 5f * s, y + 3.8f * s, 9.5f * s, RenderUtil.withAlpha(CustomTheme.hudAccentLight(), headerAlpha), FontWeight.POTIONS);
        CustomFontRenderer.drawText(ctx, "Эффекты", x + 18f * s, y + 4.8f * s, 6.5f * s, RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.SEMIBOLD);

        // Иконка зелья (сделана меньше - 9f, выровнена по Y)

        // Разделительная линия (толщину линии 0.5f не скейлим, чтобы она не "мылилась")

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

        float currentY = y + headerH + 2f * s;
        boolean isEditing = mc.currentScreen instanceof ChatScreen;
        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getActiveStatusEffects().values());

        // Заглушка, если эффектов нет, но мы в режиме редактирования
        if (effects.isEmpty() && isEditing) {
            CustomFontRenderer.drawCenteredText(ctx, "Нет эффектов", x + w / 2f, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, bodyAlpha), FontWeight.SEMIBOLD);
        } else {
            for (StatusEffectInstance effect : effects) {
                if (currentY > y + h) break;

                // Ванильная иконка (сделали 9x9)
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
                if (sprite != null) {
                    ctx.drawSpriteStretched(
                            net.minecraft.client.render.RenderLayer::getGuiTextured,
                            sprite,
                            (int)(x + 5f * s),
                            (int)(currentY + 2.5f * s),
                            (int)(9f * s), (int)(9f * s),
                            RenderUtil.withAlpha(0xFFFFFF, bodyAlpha)
                    );
                }

                // Название + Уровень
                String name = effect.getEffectType().value().getName().getString();
                int amp = effect.getAmplifier();
                if (amp > 0) name += " " + getRomanNumeral(amp);
                CustomFontRenderer.drawText(ctx, name, x + 18f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFFFFF, bodyAlpha), FontWeight.SEMIBOLD);

                // Динамический таймер
                int duration = effect.getDuration();
                int seconds = duration / 20;
                String timeStr;
                int timeColor = 0xFFAAAAAA; // Серый по умолчанию

                if (effect.isInfinite()) {
                    timeStr = "∞";
                } else {
                    int m = seconds / 60;
                    int s_time = seconds % 60;
                    timeStr = String.format("%d:%02d", m, s_time);

                    // Если меньше 10 секунд (200 тиков) — плавно перекрашиваем в красный
                    float prog = Math.max(0f, Math.min(1f, duration / 200f));
                    timeColor = RenderUtil.lerpColor(0xFFFF4444, 0xFFAAAAAA, prog);
                }

                CustomFontRenderer.drawRightText(ctx, timeStr, x + w - 5f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(timeColor, bodyAlpha), FontWeight.SEMIBOLD);

                currentY += rowH;
            }
        }

        // Flush queued DrawContext sprites/text here, before other HUD modules
        // can enable their own scissor regions in the same frame.
        ctx.draw();
        ctx.getMatrices().pop();
        RenderUtil.disableScissor();
        RenderUtil.disableScissor();
    }
}
