package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.modules.utils.TrapTimer;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TrapTimerHud extends HudElement {

    public final SliderSetting scale = addSetting(new SliderSetting("Масштаб", "scale", 1.0, 0.5, 1.5, 0.05));

    public TrapTimerHud(float x, float y) {
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
        float height = 16f * s;

        TrapTimer timer = TrapTimer.getInstance();
        boolean isActive = timer != null && timer.isEnabled() && timer.isTimerActive();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;

        boolean visible = isActive || isEditing;
        setContentVisibleTarget(visible);

        if (!visible) {
            float targetH = getContentAnim() <= 0.01f ? 0f : height;
            setHeight(RenderUtil.animate(getHeight(), targetH, 0.2f, frameDelta));
            setWidth(RenderUtil.animate(getWidth(), minW, 0.2f, frameDelta));
            return;
        }

        String text = getDisplayString(timer, isEditing);
        float textWidth = CustomFontRenderer.getTextWidth(text, 6.0f * s, FontWeight.SEMIBOLD);
        float targetW = Math.max(minW, 25f * s + textWidth);

        setHeight(RenderUtil.animate(getHeight(), height, 0.2f, frameDelta));
        setWidth(RenderUtil.animate(getWidth(), targetW, 0.2f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || h < 5f) return;

        float s = scale.get().floatValue();
        TrapTimer timer = TrapTimer.getInstance();
        boolean isEditing = mc.currentScreen instanceof ChatScreen;

        drawMenuStyleBackground(ctx, x, y, w, h, 6f * s, alpha);
        RenderUtil.enableScissor(x, y, w, h);

        float headerAlpha = alpha * getHeaderRenderAlpha();

        // 1. Рисуем иконку Незеритового лома (выровнена по центру)
        float iconSize = 12f * s;
        float iconX = x + 6f * s;
        float iconY = y + (h - iconSize) / 2f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(iconX, iconY, 300);
        ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f, 1f);
        ctx.drawItem(new ItemStack(Items.NETHERITE_SCRAP), 0, 0);
        ctx.getMatrices().pop();

        // 2. Рисуем текст (выровнен по центру относительно иконки и плашки)
        String text = getDisplayString(timer, isEditing);
        float fontSize = 6.5f * s;
        float textX = iconX + iconSize + 6f * s;
        float textY = y + (h - fontSize) / 2f;

        int textColor = 0xFFFFFFFF;
        if (timer != null && timer.isEnabled() && timer.isTimerActive()) {
            float seconds = timer.getRemainingSecondsPrecise();
            if (seconds <= 3.0f) {
                textColor = 0xFFFF5555;
            }
        }
        CustomFontRenderer.drawText(ctx, text, textX, textY, fontSize, RenderUtil.withAlpha(textColor, headerAlpha), FontWeight.SEMIBOLD);

        RenderUtil.disableScissor();
    }

    private String getDisplayString(TrapTimer timer, boolean isEditing) {
        if (timer != null && timer.isEnabled() && timer.isTimerActive()) {
            return String.format("Трапка: %.1fс", timer.getRemainingSecondsPrecise());
        }
        return "Трапка: готово";
    }
}
