package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderHandsRenderer;

@Register(name = "Shader Hands", id = "shader_hands", description = "Шейдерный эффект для рук", category = Category.VISUAL)
public class ShaderHands extends Module {

    public static final float RADIUS = 3.0f;
    public static final float THRESHOLD = 0.01f;

    public static ShaderHands INSTANCE;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Вид шейдерного эффекта",
            "Glass",
            "Glass",
            "Web"
    ));

    public final SliderSetting glassBlur = add(new SliderSetting(
            "Размытие стекла",
            "glass_blur",
            "Сила размытия фона внутри рук и предметов",
            4.0,
            0.0,
            30.0,
            0.5
    ));

    public final SliderSetting glassZoom = add(new SliderSetting(
            "Увеличение стекла",
            "glass_zoom",
            "Увеличивает картинку внутри стеклянного предмета",
            0.0,
            0.0,
            10.0,
            0.5
    ));

    public final SliderSetting glowStrength = add(new SliderSetting(
            "Сила обводки",
            "glow_strength",
            "Яркость внешней обводки",
            0.8,
            0.1,
            1.0,
            0.05
    ));

    public final SliderSetting webOpacity = add(new SliderSetting(
            "Прозрачность Web",
            "web_opacity",
            "Прозрачность web-эффекта на руке и предмете",
            0.9,
            0.05,
            1.0,
            0.05
    ));

    public final SliderSetting webGlow = add(new SliderSetting(
            "Свечение Web",
            "web_glow",
            "Сила мягкого свечения вокруг web-эффекта",
            0.85,
            0.0,
            2.0,
            0.05
    ));

    public final BooleanSetting flowOutline = add(new BooleanSetting(
            "Поток обводки",
            "flow_outline",
            "Пускает мягкую стеклянную обводку снизу вверх",
            true
    ));

    public final SliderSetting flowSpeed = add(new SliderSetting(
            "Скорость потока",
            "flow_speed",
            "Скорость движения стеклянной обводки",
            0.75,
            0.05,
            2.0,
            0.05
    ));

    public final SliderSetting flowStrength = add(new SliderSetting(
            "Сила потока",
            "flow_strength",
            "Яркость движущейся стеклянной обводки",
            0.75,
            0.0,
            1.5,
            0.05
    ));

    public final BooleanSetting itemColor = add(new BooleanSetting(
            "Цвет от предмета",
            "item_color",
            "Брать цвет обводки из текстуры предмета",
            false
    ));

    public final ColorSetting glassColor = add(new ColorSetting(
            "Цвет стекла",
            "glass_color",
            "Цвет тонировки размытия внутри предмета",
            0x2438E6FF
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Цвет внешней обводки",
            0xFF38E6FF
    ));

    public ShaderHands() {
        INSTANCE = this;
        glassBlur.visibleWhen(this::isGlassMode);
        glassZoom.visibleWhen(this::isGlassMode);
        glowStrength.visibleWhen(this::isGlassMode);
        itemColor.visibleWhen(this::isGlassMode);
        glassColor.visibleWhen(this::isGlassMode);
        flowOutline.visibleWhen(this::isGlassMode);
        flowSpeed.visibleWhen(() -> isGlassMode() && flowOutline.get());
        flowStrength.visibleWhen(() -> isGlassMode() && flowOutline.get());
        webOpacity.visibleWhen(this::isWebMode);
        webGlow.visibleWhen(this::isWebMode);
        color.visibleWhen(() -> !isGlassMode() || !itemColor.get());
    }

    @Override
    protected void onDisable() {
        ShaderHandsRenderer.reset();
    }

    public int modeIndex() {
        return switch (mode.get()) {
            case "Glass" -> 1;
            case "Web" -> 2;
            default -> 1;
        };
    }

    public boolean isGlassMode() {
        return mode.is("Glass");
    }

    public boolean isWebMode() {
        return mode.is("Web");
    }

    public static ShaderHands active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
