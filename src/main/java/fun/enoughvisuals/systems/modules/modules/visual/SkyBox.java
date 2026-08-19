package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.SkyBoxRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@Register(name = "Shader Sky", id = "skybox", description = "Шейдерное процедурное небо", category = Category.VISUAL)
public class SkyBox extends Module {

    public static SkyBox INSTANCE;
    private static boolean eventRegistered;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Вид шейдерного неба",
            "Aurora",
            "Aurora",
            "Night",
            "Snow",
            "Sky",
            "Star",
            "Glow",
            "Caustic",
            "Clouds"
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Скорость",
            "speed",
            "Скорость движения шейдера",
            1.0,
            0.0,
            5.0,
            0.05
    ));

    public final SliderSetting scale = add(new SliderSetting(
            "Масштаб",
            "scale",
            "Размер узора неба",
            1.1,
            0.1,
            10.0,
            0.05
    ));

    public final SliderSetting intensity = add(new SliderSetting(
            "Интенсивность",
            "intensity",
            "Сила шейдерного свечения",
            1.0,
            0.01,
            2.0,
            0.05
    ));

    public final SliderSetting stars = add(new SliderSetting(
            "Звёзды",
            "stars",
            "Количество и мерцание звёзд",
            0.75,
            0.0,
            1.0,
            0.05
    ));

    public final SliderSetting alpha = add(new SliderSetting(
            "Прозрачность",
            "alpha",
            "Прозрачность шейдерного неба",
            1.0,
            0.0,
            1.0,
            0.05
    ));

    public final ColorSetting color1 = add(new ColorSetting(
            "Цвет 1",
            "color1",
            "Первый цвет неба",
            0xFF79E6FF
    ));

    public final ColorSetting color2 = add(new ColorSetting(
            "Цвет 2",
            "color2",
            "Второй цвет неба",
            0xFFFF7AE6
    ));

    public final BooleanSetting vanillaBlend = add(new BooleanSetting(
            "Ванильное небо",
            "vanilla_sky",
            "Смешивать с ванильным небом",
            false
    ));

    public SkyBox() {
        INSTANCE = this;
        stars.visibleWhen(this::isProceduralSky);

        if (!eventRegistered) {
            WorldRenderEvents.BEFORE_ENTITIES.register(SkyBoxRenderer::drawOverlay);
            eventRegistered = true;
        }
    }

    public boolean isProceduralSky() {
        String m = mode.get();
        return !m.equals("Caustic") && !m.equals("Clouds");
    }

    public float getModeIndex() {
        return switch (mode.get()) {
            case "Aurora" -> 0.0f;
            case "Night" -> 1.0f;
            case "Snow" -> 2.0f;
            case "Sky" -> 3.0f;
            case "Star" -> 4.0f;
            default -> 5.0f;
        };
    }

    public int getSkyColor1() {
        return color1.get();
    }

    public int getSkyColor2() {
        return color2.get();
    }

    @Override
    protected void onEnable() {
        SkyBoxRenderer.reset();
    }

    @Override
    protected void onDisable() {
        SkyBoxRenderer.reset();
    }

    public static SkyBox active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
