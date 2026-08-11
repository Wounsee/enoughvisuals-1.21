package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.render.blur.GlassUtil;

@Register(name = "Theme", id = "custom_theme", description = "Меняет оформление ClickGUI и подсветки предметов", category = Category.CLIENT)
public class CustomTheme extends Module {
    private static final int LEGACY_PURPLE_ACCENT = 0xFF927BE0;
    private static final int PREVIOUS_DEFAULT_ACCENT = 0xFF6F73E1;
    private static final float DEFAULT_HUD_BLUR = 1.5f;
    private static final float DEFAULT_CLICK_GUI_BLUR = 1.5f;
    private static final float DEFAULT_HUD_OPACITY = 0.90f;
    private static final float DEFAULT_CLICK_GUI_OPACITY = 1.0f;

    public final BooleanSetting menu = add(new BooleanSetting(
            "Меню",
            "menu",
            "Менять цвет ClickGUI",
            true
    ));

    public final BooleanSetting item = add(new BooleanSetting(
            "Предметы",
            "item",
            "Менять цвет подсветки предметов",
            false
    ));

    public final ColorSetting accent = add(new ColorSetting(
            "Акцент",
            "accent",
            "Акцентный цвет ClickGUI и модулей",
            MenuScreen.DEFAULT_ACCENT,
            false
    ));

    public final SliderSetting clickGuiRadius = add(new SliderSetting(
            "Закругление ClickGUI", "click_gui_radius", 14.0, 3.0, 28.0, 0.5));
    public final ColorSetting itemColor = add(new ColorSetting(
            "Цвет предметов",
            "item_color",
            "Цвет подсветки предметов",
            MenuScreen.DEFAULT_ACCENT,
            false
    ));

    public final BooleanSetting disableBlur = add(new BooleanSetting(
            "Отключить блюр",
            "disable_blur",
            "Полностью отключает blur-проходы и выгружает framebuffer блюра для слабых устройств",
            false
    ));

    public CustomTheme() {
        menu.visibleWhen(() -> false);
        enableSilent();
    }

    @Override
    protected void onEnable() {
        MenuScreen.syncTheme();
    }

    @Override
    protected void onDisable() {
        MenuScreen.syncTheme();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) enableSilent();
        if (!menu.get()) menu.set(true);
        if (accent.get() == LEGACY_PURPLE_ACCENT || accent.get() == PREVIOUS_DEFAULT_ACCENT) {
            accent.set(MenuScreen.DEFAULT_ACCENT);
        }
        MenuScreen.syncTheme();
        if (disableBlur.get()) {
            GlassUtil.cleanup();
        }
    }

    public static int hudAccent() {
        CustomTheme theme = activeTheme();
        if (theme != null) return theme.guiAccentColor();
        return MenuScreen.DEFAULT_ACCENT;
    }

    public static int hudAccentLight() {
        return lighten(hudAccent(), 0.35f);
    }

    public static int hudPanelTint() {
        return 0x121214;
    }

    public static float hudPanelTintAlpha() {
        if (blurDisabled()) {
            return 1.0f;
        }
        return 0.74f * hudOpacity();
    }

    public static float hudBlurAlpha() {
        return 1.0f;
    }

    public static float hudBlurLevel() {
        return DEFAULT_HUD_BLUR;
    }

    public static float clickGuiBlurLevel() {
        return DEFAULT_CLICK_GUI_BLUR;
    }

    public static float hudOpacity() {
        return blurDisabled() ? 1.0f : DEFAULT_HUD_OPACITY;
    }

    public static float clickGuiOpacity() {
        return blurDisabled() ? 1.0f : DEFAULT_CLICK_GUI_OPACITY;
    }

    public static boolean blurDisabled() {
        CustomTheme theme = activeTheme();
        return theme != null && theme.disableBlur.get();
    }

    public static boolean hudBlurEnabled() {
        CustomTheme theme = activeTheme();
        return theme != null && !theme.disableBlur.get() && DEFAULT_HUD_BLUR > 0.01f;
    }

    public static boolean clickGuiBlurEnabled() {
        CustomTheme theme = activeTheme();
        return theme != null && !theme.disableBlur.get() && DEFAULT_CLICK_GUI_BLUR > 0.01f;
    }

    public static int hudSliderBg() {
        return darken(hudAccent(), 0.60f);
    }

    public static int hudToggleOff() {
        return darken(hudAccent(), 0.68f);
    }

    public static int itemPanelTint() {
        CustomTheme theme = activeTheme();
        return theme != null && theme.item.get()
                ? theme.itemAccentColor()
                : 0x000000;
    }

    public static float itemPanelTintAlpha() {
        return 0.60f;
    }

    public int guiAccentColor() {
        return 0xFF000000 | (accent.get() & 0x00FFFFFF);
    }

    public static int guiAccent() {
        CustomTheme theme = activeTheme();
        return theme == null ? MenuScreen.DEFAULT_ACCENT : theme.guiAccentColor();
    }

    public static float clickGuiRadius() {
        CustomTheme theme = activeTheme();
        return theme == null ? 10f : theme.clickGuiRadius.get().floatValue();
    }

    public int itemAccentColor() {
        return 0xFF000000 | (itemColor.get() & 0x00FFFFFF);
    }

    private static CustomTheme activeTheme() {
        CustomTheme theme = ModuleManager.getInstance().get(CustomTheme.class);
        return theme;
    }

    @Override
    public void toggle() {
        if (!isEnabled()) enableSilent();
    }

    @Override
    public void disable() {
    }

    @Override
    public void disableSilent() {
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!isEnabled()) enableSilent();
    }

    @Override
    public void setEnabledSilent(boolean enabled) {
        if (!isEnabled()) enableSilent();
    }

    private static int lighten(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += Math.round((255 - r) * amount);
        g += Math.round((255 - g) * amount);
        b += Math.round((255 - b) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(((color >> 16) & 0xFF) * (1f - amount));
        int g = Math.round(((color >> 8) & 0xFF) * (1f - amount));
        int b = Math.round((color & 0xFF) * (1f - amount));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
