package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

public class CustomScoreboardHud extends HudElement {
    public CustomScoreboardHud(float x, float y) { super(x, y, 150f, 86f); }
    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        drawMenuStyleBackground(ctx, x, y, w, h, 11f, alpha);
        CustomFontRenderer.drawText(ctx, "Custom ScoreBoard", x + 10f, y + 8f, 7.2f, RenderUtil.withAlpha(0xFFFFFF, alpha), FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, "Свободных нет", x + 10f, y + 24f, 6.5f, RenderUtil.withAlpha(0xCFCFCF, alpha), FontWeight.REGULAR);
    }
}
