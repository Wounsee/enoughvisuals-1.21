package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.systems.animations.AnimatedFloat;
import fun.enoughvisuals.systems.animations.Easing;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

public final class MainMenuButton {
    private static final int NORMAL = 0xFF151515;
    private static final int HOVER = 0xFF1F1F1F;
    private static final int ACTIVE = 0xFF7F94DD;
    private static final int TEXT = 0xFFFFFFFF;

    private final String label;
    private final String icon;
    private final Runnable action;
    private final boolean active;

    private float x;
    private float y;
    private float width;
    private float height;
    private final AnimatedFloat hoverAnim = new AnimatedFloat(0f, 180L, Easing.OUT_CUBIC);

    public MainMenuButton(String label, String icon, boolean active, Runnable action) {
        this.label = label;
        this.icon = icon;
        this.active = active;
        this.action = action;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void tick(double mouseX, double mouseY, float frameDelta) {
        hoverAnim.setTarget(contains(mouseX, mouseY) ? 1.0f : 0.0f);
    }

    public void render(DrawContext ctx, float scale, float alpha, float appear) {
        if (alpha <= 0.003f) return;

        float hoverValue = hoverAnim.value();
        int base = active ? ACTIVE : NORMAL;
        int hoverColor = active ? RenderUtil.lighten(ACTIVE, 0.08f) : HOVER;
        int bg = RenderUtil.withAlpha(RenderUtil.lerpColor(base, hoverColor, hoverValue), alpha * (active ? 0.98f : 0.92f));
        float radius = 6.0f * scale;
        float appearScale = 1.3f - 0.3f * appear;
        float grow = appearScale * (1.0f + hoverValue * 0.026f);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + width * 0.5f, y + height * 0.5f, 0f);
        ctx.getMatrices().scale(grow, grow, 1f);
        ctx.getMatrices().translate(-(x + width * 0.5f), -(y + height * 0.5f), 0f);

        RenderUtil.drawRoundedRect(ctx, x, y, width, height, radius, bg);

        float textSize = 5.8f * scale;
        float textY = y + (height - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f - 0.2f * scale;
        CustomFontRenderer.drawText(ctx, label, x + 7.6f * scale, textY, textSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.SEMIBOLD);

        float iconSize = 8.0f * scale;
        float iconY = y + (height - CustomFontRenderer.getTextHeight(iconSize, FontWeight.MENU)) * 0.5f - 0.4f * scale;
        CustomFontRenderer.drawRightText(ctx, icon, x + width - 7.2f * scale, iconY, iconSize,
                RenderUtil.withAlpha(TEXT, alpha), FontWeight.MENU);
        ctx.getMatrices().pop();
    }

    public boolean click(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) return false;
        action.run();
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
