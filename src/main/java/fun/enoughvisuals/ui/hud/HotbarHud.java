package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.gui.DrawContext;

/** Layout/settings proxy for the custom hotbar rendered by InGameHud. */
public final class HotbarHud extends HudElement {
    public static final float BASE_WIDTH = 214f;
    public static final float BASE_HEIGHT = 22f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    public HotbarHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        float value = HudDesign.scale(scale);
        setWidth(BASE_WIDTH * value);
        setHeight(BASE_HEIGHT * value);
    }

    public float renderScale() {
        return HudDesign.scale(scale);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        // Actual item rendering happens at the vanilla hotbar stage so the
        // item renderer, overlays and attack indicator preserve their order.
    }
}
