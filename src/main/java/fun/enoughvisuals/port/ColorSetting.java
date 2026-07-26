package fun.enoughvisuals.port;

import java.awt.Color;
import java.util.function.Supplier;

public class ColorSetting extends Setting {
    public float hueValue;
    public float saturation;
    public float brightness;
    public float alpha;

    public ColorSetting(String name, float hue) {
        this(name, hue, 1.0F, 1.0F, 1.0F);
    }

    public ColorSetting(String name, float hue, float saturation, float brightness) {
        this(name, hue, saturation, brightness, 1.0F);
    }

    public ColorSetting(String name, float hue, float saturation, float brightness, float alpha) {
        this.name = name;
        this.hueValue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
        this.alpha = alpha;
    }

    public Color getColor() {
        float hue = (hueValue / 106.0F) % 1.0F;
        if (hue < 0.0F) hue += 1.0F;
        Color rgb = Color.getHSBColor(hue,
                Math.max(0.0F, Math.min(1.0F, saturation)),
                Math.max(0.0F, Math.min(1.0F, brightness)));
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(),
                Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F));
    }

    public void invoke(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueValue = hsb[0] * 106.0F;
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = color.getAlpha() / 255.0F;
    }

    public int compute() {
        return getColor().getRGB();
    }

    public ColorSetting setVisibilityCondition(Supplier<Boolean> condition) {
        this.visibilityCondition = condition;
        return this;
    }
}
