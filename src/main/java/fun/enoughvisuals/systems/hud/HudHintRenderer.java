package fun.enoughvisuals.systems.hud;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.gui.DrawContext;

public class HudHintRenderer {

    public static void render(DrawContext ctx, float screenWidth, float screenHeight) {
        float alpha = 0.90f;
        Animations animations = Animations.active();
        if (animations != null) {
            alpha *= animations.chatOpenProgress(true);
        }
        if (alpha <= 0.02f || screenWidth <= 32) {
            return;
        }

        String addHint = "Нажмите ПКМ по пустому месту, чтобы добавить элемент HUD";
        String settingsHint = "Нажмите ПКМ по элементу HUD, чтобы открыть настройки";
        float textSize = 6.15f;
        float maxTextW = Math.min(250f, Math.max(48f, screenWidth - 24f));

        addHint = CustomFontRenderer.trimToWidth(addHint, maxTextW, textSize, FontWeight.REGULAR);
        settingsHint = CustomFontRenderer.trimToWidth(settingsHint, maxTextW, textSize, FontWeight.REGULAR);

        float textW = Math.max(
                CustomFontRenderer.getTextWidth(addHint, textSize, FontWeight.REGULAR),
                CustomFontRenderer.getTextWidth(settingsHint, textSize, FontWeight.REGULAR)
        );

        HudElement watermark = DragAndDrop.getInstance().getActiveElementByTypeId("watermark");
        float centerX = screenWidth * 0.5f;
        float topY = 10f;
        if (watermark != null) {
            centerX = watermark.getX() + watermark.getWidth() * 0.5f;
            topY = watermark.getY() + watermark.getHeight() + 12f;
        }

        float lineGap = 2.5f;
        float panelW = textW + 14f;
        float panelH = textSize * 2f + lineGap + 8f;
        float panelX = centerX - panelW * 0.5f;
        float panelY = Math.min(topY, screenHeight - panelH - 8f);

        GlassUtil.drawGlassPanel(ctx, panelX, panelY, panelW, panelH, 5f,
                CustomTheme.hudPanelTint(), alpha * CustomTheme.hudPanelTintAlpha() * 0.8f, alpha * alpha);
        CustomFontRenderer.drawCenteredText(ctx, addHint, centerX, panelY + 4f, textSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, alpha * 0.96f), FontWeight.REGULAR);
        CustomFontRenderer.drawCenteredText(ctx, settingsHint, centerX, panelY + 4f + textSize + lineGap, textSize,
                RenderUtil.withAlpha(MenuScreen.TEXT_DIM, alpha * 0.94f), FontWeight.REGULAR);
    }
}
