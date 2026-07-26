package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class BindsHud extends HudElement {

    public final SliderSetting scale = addSetting(new SliderSetting("Масштаб", "scale", 1.0, 0.5, 1.5, 0.05));

    public BindsHud(float x, float y) {
        super(x, y, 100f, 16f);
    }

    @Override
    protected boolean usesSplitAppearanceAnimation() {
        return true;
    }

    private List<Module> getBoundModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            // Исправлено: метод называется getKeyBind()
            if (m.isEnabled() && m.getKeyBind() != BindKeyUtil.NONE) {
                list.add(m);
            }
        }
        return list;
    }

    @Override
    protected void update(float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        float frameDelta = RenderUtil.getFrameDelta();

        float s = scale.get().floatValue();
        float headerH = 16f * s;
        float rowH = 14f * s;
        float minW = 100f * s;

        List<Module> boundModules = getBoundModules();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;
        boolean visible = !boundModules.isEmpty() || isEditing;
        setContentVisibleTarget(visible);

        // Схлопываем, если пусто и мы не в чате
        if (!visible) {
            float targetH = getContentAnim() <= 0.01f ? 0f : headerH;
            setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
            setWidth(RenderUtil.animate(getWidth(), minW, 0.2f, frameDelta));
            return;
        }

        float targetH = headerH;
        if (boundModules.isEmpty() && isEditing) {
            targetH += rowH + (4f * s);
        } else if (!boundModules.isEmpty()) {
            targetH += (boundModules.size() * rowH) + (4f * s);
        }

        float maxRowW = 0f;
        if (boundModules.isEmpty() && isEditing) {
            maxRowW = CustomFontRenderer.getTextWidth("Нет биндов", 6.0f * s, FontWeight.SEMIBOLD);
        } else {
            for (Module m : boundModules) {
                String name = m.getName();
                String keyName = BindKeyUtil.getBindName(m.getKeyBind());

                // 15f (место под иконку) + Имя + 10f (пробел) + Кнопка
                float currentW = 15f * s +
                        CustomFontRenderer.getTextWidth(name, 6.0f * s, FontWeight.SEMIBOLD) +
                        10f * s +
                        CustomFontRenderer.getTextWidth(keyName, 6.0f * s, FontWeight.SEMIBOLD);
                maxRowW = Math.max(maxRowW, currentW);
            }
        }

        float targetW = Math.max(minW, (5f + 5f) * s + maxRowW);

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

        drawMenuStyleBackground(ctx, x, y, w, h, 6f * s, alpha);
        RenderUtil.enableScissor(x, y, w, h);

        float headerAlpha = Math.min(1f, h / headerH) * alpha * getHeaderRenderAlpha();
        RenderUtil.drawRoundedRect(ctx, x, y, w, Math.min(h, headerH), 6f * s, 6f * s, 0f, 0f,
                RenderUtil.withAlpha(0x000000, headerAlpha * 0.18f));

        // Заголовок
        CustomFontRenderer.drawText(ctx, "\uF003", x + 5f * s, y + 4.2f * s, 8.5f * s, RenderUtil.withAlpha(CustomTheme.hudAccentLight(), headerAlpha), FontWeight.BINDS);
        CustomFontRenderer.drawText(ctx, "Бинды", x + 18f * s, y + 4.8f * s, 6.5f * s, RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.SEMIBOLD);

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
        List<Module> boundModules = getBoundModules();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;

        if (boundModules.isEmpty() && isEditing) {
            CustomFontRenderer.drawCenteredText(ctx, "Нет биндов", x + w / 2f, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, bodyAlpha), FontWeight.SEMIBOLD);
        } else {
            for (Module m : boundModules) {
                if (currentY > y + h) break;

                // 1. Иконка категории через Draw.image
                String icon = "";
                FontWeight font = FontWeight.VC;
                float iconSize = 9f * s;

                switch (m.getCategory()) {
                    case CLIENT -> {
                        icon = "\uF001";
                        font = FontWeight.CLIENT;
                        iconSize = 9f * s;
                    }

                    /* case WORLD -> {
                        icon = "\uF002";
                        font = FontWeight.WP;
                        iconSize = 11f * s;
                    }

                    case PLAYER -> {
                        icon = "\uF001";
                        font = FontWeight.WP;
                        iconSize = 10f * s;
                    } */

                    case VISUAL -> {
                        icon = "\uF001";
                        font = FontWeight.VC;
                        iconSize = 10f * s;
                    }

                    case UTILS -> {
                        icon = "\uF000";
                        font = FontWeight.VC;
                        iconSize = 9f * s;
                    }
                }

                CustomFontRenderer.drawText(
                        ctx,
                        icon,
                        x + 5f * s,
                        currentY + (rowH - iconSize) * 0.5f,
                        iconSize,
                        RenderUtil.withAlpha(0xFFFFFF, bodyAlpha),
                        font
                );

                // 2. Название модуля (сдвинуто на x + 16f, чтобы не налезть на иконку)
                CustomFontRenderer.drawText(ctx, m.getName(), x + 16f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFFFFF, bodyAlpha), FontWeight.SEMIBOLD);

                // 3. Клавиша бинда
                String keyName = BindKeyUtil.getBindName(m.getKeyBind());
                CustomFontRenderer.drawRightText(ctx, keyName, x + w - 5f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, bodyAlpha), FontWeight.SEMIBOLD);

                currentY += rowH;
            }
        }

        ctx.getMatrices().pop();
        RenderUtil.disableScissor();
        RenderUtil.disableScissor();
    }
}
