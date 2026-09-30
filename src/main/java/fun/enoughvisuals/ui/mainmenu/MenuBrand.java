package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

/** Shared Figma-derived Enough Visuals lockup used by menu surfaces. */
public final class MenuBrand {
    private static final String LOGO = "\uF002";
    private static final String NAME = "Enough Visuals";
    private static final int COLOR = 0xFFFFFFFF;

    // Visible plane bounds produced by generate_solar_icons.py for enough_logo.svg.
    private static final float LOGO_LEFT = -0.06396484375f;
    private static final float LOGO_BOTTOM = -0.06640625f;
    private static final float LOGO_RIGHT = 1.35009765625f;
    private static final float LOGO_TOP = 1.06640625f;
    private static final float LOGO_ASCENDER = 1f;

    private MenuBrand() {
    }

    public static void render(DrawContext context, float centerX, float topY,
                              float scale, float alpha, float appear) {
        if (alpha <= 0.003f) return;
        appear = Math.max(0f, Math.min(1f, appear));

        float logoFontSize = 9.27f * scale;
        float logoWidth = (LOGO_RIGHT - LOGO_LEFT) * logoFontSize;
        float logoHeight = (LOGO_TOP - LOGO_BOTTOM) * logoFontSize;
        float textSize = 9.7f * scale;
        float textWidth = CustomFontRenderer.getTextWidth(NAME, textSize, FontWeight.SEMIBOLD);
        float textHeight = CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD);
        float gap = 7f * scale;
        float groupWidth = logoWidth + gap + textWidth;
        float left = centerX - groupWidth * 0.5f;
        float lift = (1f - appear) * 3.5f * scale;
        float entranceScale = 0.985f + appear * 0.015f;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, topY + logoHeight * 0.5f, 0f);
        context.getMatrices().scale(entranceScale, entranceScale, 1f);
        context.getMatrices().translate(-centerX, -(topY + logoHeight * 0.5f), 0f);

        float logoX = left - LOGO_LEFT * logoFontSize;
        float logoY = topY + lift - (LOGO_ASCENDER - LOGO_TOP) * logoFontSize;
        CustomFontRenderer.drawText(context, LOGO, logoX, logoY, logoFontSize,
                RenderUtil.withAlpha(COLOR, alpha * 0.75f), FontWeight.LOGO);

        float textY = topY + lift + (logoHeight - textHeight) * 0.5f;
        CustomFontRenderer.drawText(context, NAME, left + logoWidth + gap, textY, textSize,
                RenderUtil.withAlpha(COLOR, alpha * 0.75f), FontWeight.SEMIBOLD);
        context.getMatrices().pop();
    }
}
