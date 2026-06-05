package fun.enoughvisuals.ui.hud;

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

        // Схлопываем, если пусто и мы не в чате
        if (boundModules.isEmpty() && !isEditing) {
            setHeight(RenderUtil.animate(getHeight(), 0f, 0.2f, frameDelta));
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

        float headerAlpha = Math.min(1f, h / headerH) * alpha;

        // Заголовок
        CustomFontRenderer.drawText(ctx, "Бинды", x + 5f * s, y + 4.8f * s, 6.5f * s, RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawRightText(ctx, "\uF003", x + w - 5f * s, y + 4.2f * s, 8.5f * s, RenderUtil.withAlpha(0xFFFFFF, headerAlpha), FontWeight.BINDS);
        RenderUtil.drawRect(ctx, x + 5f * s, y + headerH - 1f, w - 10f * s, 0.5f, RenderUtil.withAlpha(0xFFFFFF, headerAlpha * 0.15f));

        if (h <= headerH + 2f) {
            RenderUtil.disableScissor();
            return;
        }

        float currentY = y + headerH + 2f * s;
        List<Module> boundModules = getBoundModules();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;

        if (boundModules.isEmpty() && isEditing) {
            CustomFontRenderer.drawCenteredText(ctx, "Нет биндов", x + w / 2f, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, alpha), FontWeight.SEMIBOLD);
        } else {
            for (Module m : boundModules) {
                if (currentY > y + h) break;

                // 1. Иконка категории через Draw.image
                Identifier catIcon = m.getCategory() == Category.VISUAL
                        ? Identifier.of("enough", "icons/category/visual.png")
                        : Identifier.of("enough", "icons/category/utils.png");

                float iconSize = 9f * s;
                // Параметры 43, 43 взяты из MenuScreen.CAT_ICON_TEX
                Draw.image(ctx, catIcon, x + 5f * s, currentY + 2.5f * s, iconSize, iconSize, 43, 43, alpha);

                // 2. Название модуля (сдвинуто на x + 16f, чтобы не налезть на иконку)
                CustomFontRenderer.drawText(ctx, m.getName(), x + 16f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFFFFF, alpha), FontWeight.SEMIBOLD);

                // 3. Клавиша бинда
                String keyName = BindKeyUtil.getBindName(m.getKeyBind());
                CustomFontRenderer.drawRightText(ctx, keyName, x + w - 5f * s, currentY + 3.5f * s, 6.0f * s, RenderUtil.withAlpha(0xFFAAAAAA, alpha), FontWeight.SEMIBOLD);

                currentY += rowH;
            }
        }

        RenderUtil.disableScissor();
    }
}
