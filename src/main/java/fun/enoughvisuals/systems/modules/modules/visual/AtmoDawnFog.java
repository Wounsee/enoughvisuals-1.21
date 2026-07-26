package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.util.math.MathHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import org.joml.Matrix4f;

@Register(name = "AtmoDawnFog", id = "atmo_dawn_fog", description = "Кинематографичная атмосфера: туман, лучи света, заря", category = Category.VISUAL)
public final class AtmoDawnFog extends Module {
    private static AtmoDawnFog INSTANCE;
    private static final Matrix4f WORLD_POSITION = new Matrix4f();
    private static final Matrix4f WORLD_PROJECTION = new Matrix4f();
    private static boolean matricesReady;
    private final fun.enoughvisuals.port.AtmoDawnFog ported = new fun.enoughvisuals.port.AtmoDawnFog();

    public final ModeSetting mode = add(new ModeSetting("Режим", "mode", "", "Рассвет", "Рассвет", "Сумерки", "Тема"));
    public final SliderSetting density = add(new SliderSetting("Плотность", "density", "", 0.35, 0.05, 0.8, 0.01));
    public final SliderSetting scatterHeight = add(new SliderSetting("Высота рассеивания", "scatter_height", "", 76.0, 60.0, 120.0, 1.0));
    public final SliderSetting godRays = add(new SliderSetting("Лучи света", "god_rays", "", 0.75, 0.0, 1.0, 0.01));
    public final SliderSetting softness = add(new SliderSetting("Мягкость", "softness", "", 0.6, 0.0, 1.0, 0.01));
    public final BooleanSetting rainbow = add(new BooleanSetting("Радуга", "rainbow", "", true));
    public final SliderSetting rainbowBrightness = add(new SliderSetting("Яркость радуги", "rainbow_brightness", "", 0.55, 0.1, 1.0, 0.01));
    public final SliderSetting rainbowSize = add(new SliderSetting("Размер радуги", "rainbow_size", "", 54.0, 46.0, 60.0, 0.5));
    public final ColorSetting dawnColor = add(new ColorSetting("Цвет зари", "dawn_color", "", 0xFFFFAD7A));

    private final float[] palette = new float[18];

    public AtmoDawnFog() {
        INSTANCE = this;
        fun.enoughvisuals.port.VisualPortRegistry.atmoDawnFog = ported;
        rainbowBrightness.visibleWhen(rainbow::get);
        rainbowSize.visibleWhen(rainbow::get);
        dawnColor.visibleWhen(() -> mode.is("Рассвет"));
    }

    public static AtmoDawnFog active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    @Override
    protected void onEnable() {
        ported.enabled = true;
    }

    @Override
    protected void onDisable() {
        ported.onDisable();
        matricesReady = false;
    }

    public static void captureWorldMatrices(Matrix4f position, Matrix4f projection) {
        if (active() == null || position == null || projection == null) return;
        WORLD_POSITION.set(position);
        WORLD_PROJECTION.set(projection);
        matricesReady = true;
    }

    public static void renderInWorld(WorldRenderContext context) {
        AtmoDawnFog module = active();
        if (module == null || !matricesReady || context == null || context.camera() == null
                || mc.world == null || mc.player == null) return;
        module.syncPortedSettings();
        float tickDelta = context.tickCounter() == null ? 1.0F : context.tickCounter().getTickDelta(true);
        Matrix4f position = new Matrix4f(WORLD_POSITION);
        Matrix4f projection = new Matrix4f(WORLD_PROJECTION);
        fun.enoughvisuals.port.WorldRenderCapture capture =
                new fun.enoughvisuals.port.WorldRenderCapture(context.camera(), position, projection);
        module.ported.onWorldRenderContext(new fun.enoughvisuals.port.WorldRenderContextEvent(
                mc, mc.gameRenderer, capture, tickDelta));
    }

    private void syncPortedSettings() {
        ported.enabled = true;
        ported.rezhim.value = mode.get();
        ported.plotnost.invoke(density.get().floatValue());
        ported.vysotaRasseivaniya.invoke(scatterHeight.get().floatValue());
        ported.luchiSveta.invoke(godRays.get().floatValue());
        ported.myagkost.invoke(softness.get().floatValue());
        ported.raduga.setValue(rainbow.get());
        ported.yarkostRadugi.invoke(rainbowBrightness.get().floatValue());
        ported.razmerRadugi.invoke(rainbowSize.get().floatValue());
        ported.tsvetZari.invoke(new Color(dawnColor.get(), true));
    }

    public float density() { return MathHelper.clamp(density.get().floatValue(), 0.05F, 0.8F); }
    public float scatterHeight() { return scatterHeight.get().floatValue(); }
    public float godRays() { return MathHelper.clamp(godRays.get().floatValue(), 0.0F, 1.0F); }
    public float softness() { return MathHelper.clamp(softness.get().floatValue(), 0.0F, 1.0F); }
    public float rainbowBrightness() { return rainbow.get() ? MathHelper.clamp(rainbowBrightness.get().floatValue(), 0.0F, 1.0F) : 0.0F; }
    public float rainbowSize() { return MathHelper.clamp(rainbowSize.get().floatValue(), 40.0F, 64.0F); }
    public float sunPhase() { return mode.is("Сумерки") ? -0.045F : mode.is("Тема") ? 0.13F : 0.11F; }

    public float[] palette() {
        float r = dawnColor.getRedF();
        float g = dawnColor.getGreenF();
        float b = dawnColor.getBlueF();
        if (mode.is("Сумерки")) {
            put(0, .135F, .125F, .3F); put(3, .89F, .46F, .55F); put(6, .38F, .35F, .56F);
            put(9, .8F, .52F, .62F); put(12, .47F, .44F, .64F); put(15, .92F, .56F, .72F);
        } else if (mode.is("Тема")) {
            int theme = CustomTheme.hudAccent();
            float tr = ((theme >> 16) & 255) / 255F, tg = ((theme >> 8) & 255) / 255F, tb = (theme & 255) / 255F;
            mix(0, .085F, .1F, .2F, tr, tg, tb, .3F); mix(3, tr, tg, tb, 1F, .93F, .82F, .35F);
            mix(6, .52F, .58F, .74F, tr, tg, tb, .28F); mix(9, tr, tg, tb, .97F, .93F, .88F, .45F);
            mix(12, .58F, .63F, .76F, tr, tg, tb, .35F); mix(15, tr, tg, tb, 1F, .96F, .88F, .3F);
        } else {
            mix(0, .16F, .19F, .38F, r, g, b, .14F); put(3, clamp(r * 1.12F), clamp(g * .88F), clamp(b * .62F));
            put(6, .56F, .62F, .8F); mix(9, r, g, b, .95F, .55F, .63F, .42F);
            put(12, .6F, .67F, .82F); put(15, clamp(r * 1.08F), clamp(g * .94F), clamp(b * .72F));
        }
        return palette;
    }

    private void put(int i, float r, float g, float b) { palette[i] = r; palette[i + 1] = g; palette[i + 2] = b; }
    private void mix(int i, float r, float g, float b, float r2, float g2, float b2, float t) {
        put(i, r + (r2 - r) * t, g + (g2 - g) * t, b + (b2 - b) * t);
    }
    private static float clamp(float value) { return MathHelper.clamp(value, 0.0F, 1.0F); }
}
