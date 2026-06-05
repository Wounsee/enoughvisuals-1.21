package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.SkyBoxRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

@Register(name = "Shader Sky", id = "skybox", description = "Шейдерное небо", category = Category.VISUAL)
public class SkyBox extends Module {

    public static SkyBox INSTANCE;
    private static boolean eventRegistered;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Вид шейдерного неба",
            "Caustic",
            "Caustic",
            "Clouds"
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Скорость",
            "speed",
            "Скорость движения каустики",
            0.1,
            0.0,
            5.0,
            0.05
    ));

    public final SliderSetting scale = add(new SliderSetting(
            "Масштаб",
            "scale",
            "Размер узора неба",
            18.0,
            1.0,
            30.0,
            0.5
    ));

    public final SliderSetting intensity = add(new SliderSetting(
            "Интенсивность",
            "intensity",
            "Сила шейдерного свечения",
            0.2,
            0.001,
            0.5,
            0.001
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

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Основной цвет каустики",
            0xFF3BE8E0
    ));

    public SkyBox() {
        INSTANCE = this;
        if (!eventRegistered) {
            WorldRenderEvents.LAST.register(SkyBoxRenderer::drawOverlay);
            eventRegistered = true;
        }
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
        if (INSTANCE == null || !INSTANCE.isEnabled()) {
            return null;
        }

        return INSTANCE;
    }
}


