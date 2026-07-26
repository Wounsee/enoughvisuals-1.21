package fun.enoughvisuals.port;

public final class ColorUtils {
    private ColorUtils() { }
    public static int compute29(int color, int alpha) { return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF; }
    public static int compute40(int offset) {
        int first = fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccent();
        int second = fun.enoughvisuals.systems.modules.modules.client.CustomTheme.hudAccentLight();
        float phase = (float)((System.currentTimeMillis() / 10L + offset) % 1000L) / 1000.0F;
        float mix = 0.5F - 0.5F * (float)Math.cos(phase * Math.PI * 2.0);
        int r = Math.round(((first >> 16) & 255) + (((second >> 16) & 255) - ((first >> 16) & 255)) * mix);
        int g = Math.round(((first >> 8) & 255) + (((second >> 8) & 255) - ((first >> 8) & 255)) * mix);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * mix);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
