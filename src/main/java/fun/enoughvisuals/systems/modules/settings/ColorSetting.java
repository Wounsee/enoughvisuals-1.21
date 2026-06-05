package fun.enoughvisuals.systems.modules.settings;

public class ColorSetting extends Setting<Integer> {

    private final boolean allowAlpha;

    public ColorSetting(String name, String id, String description,
                        int defaultColor, boolean allowAlpha) {
        super(name, id, description, defaultColor);
        this.allowAlpha = allowAlpha;
    }

    public ColorSetting(String name, String id, int defaultColor) {
        this(name, id, "", defaultColor, true);
    }

    public ColorSetting(String name, String id, String description, int defaultColor) {
        this(name, id, description, defaultColor, true);
    }

    @Override
    protected Integer validate(Integer value) {
        if (!allowAlpha) {
            return 0xFF000000 | (value & 0x00FFFFFF);
        }
        return value;
    }

    
    
    

    public int getRed() { return (get() >> 16) & 0xFF; }
    public int getGreen() { return (get() >> 8) & 0xFF; }
    public int getBlue() { return get() & 0xFF; }
    public int getAlpha() { return (get() >> 24) & 0xFF; }

    public float getRedF() { return getRed() / 255f; }
    public float getGreenF() { return getGreen() / 255f; }
    public float getBlueF() { return getBlue() / 255f; }
    public float getAlphaF() { return getAlpha() / 255f; }

    public void setRed(int r) { set((get() & 0xFF00FFFF) | ((r & 0xFF) << 16)); }
    public void setGreen(int g) { set((get() & 0xFFFF00FF) | ((g & 0xFF) << 8)); }
    public void setBlue(int b) { set((get() & 0xFFFFFF00) | (b & 0xFF)); }
    public void setAlpha(int a) { set((get() & 0x00FFFFFF) | ((a & 0xFF) << 24)); }

    public void setRGBA(int r, int g, int b, int a) {
        set(((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    public void setFromHSB(float h, float s, float b) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b);
        set((get() & 0xFF000000) | (rgb & 0x00FFFFFF));
    }

    public float[] getHSB() {
        return java.awt.Color.RGBtoHSB(getRed(), getGreen(), getBlue(), null);
    }

    public boolean hasAlpha() { return allowAlpha; }

    @Override
    public Object serialize() {
        return String.format("#%08X", get());
    }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            try {
                s = s.replace("#", "");
                if (s.length() == 6) {
                    set(0xFF000000 | Integer.parseInt(s, 16));
                } else if (s.length() == 8) {
                    set((int) Long.parseLong(s, 16));
                }
            } catch (NumberFormatException ignored) {}
        } else if (data instanceof Number n) {
            set(n.intValue());
        }
    }
}