package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;

@Register(name = "ColorPlus", id = "color_plus", description = "Кинематографичная цветокоррекция", category = Category.VISUAL)
public final class ColorPlus extends Module {
    private static ColorPlus INSTANCE;

    public final ModeSetting preset = add(new ModeSetting("Пресет", "preset", "", "Cinematic",
            "Cinematic", "Vibrant", "Natural", "Sunny Field", "Urban Rain", "Sunset", "Frost", "Teal-Orange", "Vintage", "Neon Night", "Custom"));
    public final SliderSetting strength = add(new SliderSetting("Сила эффекта", "strength", "", 1.0, 0.0, 1.0, 0.01));
    public final BooleanSetting cas = add(new BooleanSetting("Резкость (CAS)", "cas", "", true));
    public final SliderSetting exposure = add(new SliderSetting("Экспозиция", "exposure", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting contrast = add(new SliderSetting("Контраст", "contrast", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting saturation = add(new SliderSetting("Насыщенность", "saturation", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting vibrance = add(new SliderSetting("Vibrance", "vibrance", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting gamma = add(new SliderSetting("Гамма", "gamma", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting temperature = add(new SliderSetting("Температура", "temperature", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting tint = add(new SliderSetting("Оттенок (зелёный/маджента)", "tint", "", 0.0, -0.5, 0.5, 0.01));
    public final SliderSetting bloom = add(new SliderSetting("Интенсивность Bloom", "bloom", "", 0.0, -0.3, 0.3, 0.01));
    public final SliderSetting sharpness = add(new SliderSetting("Сила резкости", "sharpness", "", 0.0, -0.3, 0.3, 0.01));
    public final SliderSetting vignette = add(new SliderSetting("Виньетка", "vignette", "", 0.0, -0.3, 0.3, 0.01));

    public ColorPlus() { INSTANCE = this; }
    public static ColorPlus active() { return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null; }
    public float strength() { return strength.get().floatValue(); }

    public static void renderExact() {
        ColorPlus module = active();
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null || client.world == null || client.player == null || client.getFramebuffer() == null
                || client.getWindow() == null) return;
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        int texture = client.getFramebuffer().getColorAttachment();
        if (texture <= 0 || width <= 1 || height <= 1) return;

        Values values = module.values();
        fun.enoughvisuals.port.ColorPlusRenderer.ColorPlusRendererState state =
                new fun.enoughvisuals.port.ColorPlusRenderer.ColorPlusRendererState();
        state.floatValue = module.strength();
        state.floatValue2 = values.exposure();
        state.floatValue3 = values.contrast();
        state.floatValue4 = values.saturation();
        state.floatValue5 = values.vibrance();
        state.floatValue6 = values.gamma();
        state.floatValue7 = values.temperature();
        state.floatValue8 = values.tint();
        state.floatValue9 = values.lift()[0];
        state.floatValue10 = values.lift()[1];
        state.floatValue11 = values.lift()[2];
        state.floatValue12 = values.gammaRgb()[0];
        state.floatValue13 = values.gammaRgb()[1];
        state.floatValue14 = values.gammaRgb()[2];
        state.floatValue15 = values.gain()[0];
        state.floatValue16 = values.gain()[1];
        state.floatValue17 = values.gain()[2];
        state.floatValue18 = values.bloom();
        state.floatValue19 = values.bloomThreshold();
        state.floatValue20 = values.bloomRadius();
        state.floatValue21 = values.sharpness();
        state.floatValue22 = values.vignette();
        state.flag = true;
        fun.enoughvisuals.port.ColorPlusRenderer.resolve().invoke(texture, width, height, state);
    }

    public Values values() {
        Preset p = Preset.byName(preset.get());
        return new Values(
                p.exposure + exposure.get().floatValue(), p.contrast + contrast.get().floatValue(),
                p.saturation + saturation.get().floatValue(), p.vibrance + vibrance.get().floatValue(),
                p.gamma + gamma.get().floatValue(), p.temperature + temperature.get().floatValue(),
                p.tint + tint.get().floatValue(), p.lift, p.gammaRgb, p.gain,
                Math.max(0.0F, p.bloom + bloom.get().floatValue()), p.threshold, p.radius,
                cas.get() ? Math.max(0.0F, p.sharpness + sharpness.get().floatValue()) : 0.0F,
                Math.max(0.0F, p.vignette + vignette.get().floatValue()));
    }

    public record Values(float exposure, float contrast, float saturation, float vibrance, float gamma,
                         float temperature, float tint, float[] lift, float[] gammaRgb, float[] gain,
                         float bloom, float bloomThreshold, float bloomRadius, float sharpness, float vignette) {}

    private enum Preset {
        CINEMATIC("Cinematic", .03F, .08F, .06F, .10F, -.02F, .05F, -.02F, a(-.02F,-.01F,.02F), a(.01F,0,-.01F), a(.03F,.01F,-.02F), .55F,.86F,64,.22F,.08F),
        VIBRANT("Vibrant", .04F,.10F,.16F,.18F,-.02F,.02F,0,a(0,0,0),a(0,0,0),a(.02F,.02F,.02F),.45F,.84F,60,.28F,.04F),
        NATURAL("Natural", .01F,.04F,.03F,.06F,0,.02F,0,a(0,0,0),a(0,0,0),a(0,0,0),.30F,.92F,48,.16F,.02F),
        SUNNY("Sunny Field", .05F,.06F,.10F,.18F,-.03F,.16F,-.04F,a(.02F,.01F,-.03F),a(.02F,.01F,-.02F),a(.04F,.02F,-.03F),.65F,.80F,70,.20F,.06F),
        RAIN("Urban Rain", -.03F,.05F,-.06F,.04F,.01F,-.18F,.04F,a(-.02F,-.01F,.04F),a(0,.01F,.02F),a(-.02F,-.01F,.04F),.40F,.92F,56,.22F,.10F),
        SUNSET("Sunset", .03F,.10F,.10F,.16F,-.02F,.20F,-.06F,a(.04F,.01F,-.04F),a(.02F,0,-.02F),a(.05F,.02F,-.03F),.75F,.78F,80,.20F,.06F),
        FROST("Frost", .01F,.05F,-.04F,.04F,.01F,-.20F,-.02F,a(-.02F,-.01F,.04F),a(-.01F,.01F,.02F),a(-.02F,.01F,.04F),.50F,.86F,64,.24F,.08F),
        TEAL("Teal-Orange", .03F,.12F,.10F,.12F,-.02F,.04F,0,a(-.04F,-.01F,.04F),a(.01F,0,-.01F),a(.06F,.02F,-.04F),.60F,.82F,70,.24F,.08F),
        VINTAGE("Vintage", -.02F,-.02F,-.08F,-.04F,.04F,.10F,.02F,a(.02F,.01F,-.02F),a(.02F,.01F,-.02F),a(.02F,0,-.04F),.30F,.92F,50,.10F,.16F),
        NEON("Neon Night", -.02F,.10F,.16F,.20F,.02F,-.10F,.08F,a(-.02F,-.02F,.05F),a(0,-.01F,.02F),a(.02F,-.02F,.06F),.95F,.74F,90,.22F,.10F),
        CUSTOM("Custom",0,0,0,0,0,0,0,a(0,0,0),a(0,0,0),a(0,0,0),0,.9F,64,0,0);

        final String name; final float exposure, contrast, saturation, vibrance, gamma, temperature, tint;
        final float[] lift, gammaRgb, gain; final float bloom, threshold, radius, sharpness, vignette;
        Preset(String name,float exposure,float contrast,float saturation,float vibrance,float gamma,float temperature,float tint,
               float[] lift,float[] gammaRgb,float[] gain,float bloom,float threshold,float radius,float sharpness,float vignette) {
            this.name=name; this.exposure=exposure; this.contrast=contrast; this.saturation=saturation; this.vibrance=vibrance;
            this.gamma=gamma; this.temperature=temperature; this.tint=tint; this.lift=lift; this.gammaRgb=gammaRgb; this.gain=gain;
            this.bloom=bloom; this.threshold=threshold; this.radius=radius; this.sharpness=sharpness; this.vignette=vignette;
        }
        static Preset byName(String name) { for (Preset p : values()) if (p.name.equalsIgnoreCase(name)) return p; return CINEMATIC; }
        static float[] a(float x,float y,float z) { return new float[]{x,y,z}; }
    }
}
