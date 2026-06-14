package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;

@Register(
        name = "Shader ESP",
        id = "shader_esp",
        description = "Shader fill and glow around players",
        category = Category.VISUAL
)
public class ShaderESP extends Module {

    public static ShaderESP INSTANCE;

    public final ModeSetting mode = add(new ModeSetting(
            "Mode",
            "mode",
            "ESP rendering style",
            "Default",
            "Default",
            "Echo",
            "Full"
    ));

    public final SliderSetting fillOpacity = add(new SliderSetting(
            "Fill",
            "fill_opacity",
            "Opacity of the player silhouette",
            0.22,
            0.0,
            1.0,
            0.05
    ));

    public final SliderSetting glowStrength = add(new SliderSetting(
            "Glow",
            "glow_strength",
            "Brightness of the outer glow",
            1.15,
            0.0,
            2.5,
            0.05
    ));

    public final SliderSetting glowRadius = add(new SliderSetting(
            "Glow radius",
            "glow_radius",
            "Width of the soft glow",
            13.0,
            2.0,
            30.0,
            0.5
    ));

    public final SliderSetting outlineStrength = add(new SliderSetting(
            "Outline",
            "outline_strength",
            "Brightness of the sharp outline",
            0.9,
            0.0,
            2.0,
            0.05
    ));

    public final SliderSetting outlineWidth = add(new SliderSetting(
            "Outline width",
            "outline_width",
            "Width of the sharp outline",
            1.5,
            0.5,
            5.0,
            0.25
    ));

    public final SliderSetting echoDecay = add(new SliderSetting(
            "Echo length",
            "echo_decay",
            "How long previous silhouettes remain visible",
            0.88,
            0.55,
            0.97,
            0.01
    ));

    public final SliderSetting distance = add(new SliderSetting(
            "Distance",
            "distance",
            "Maximum ESP render distance",
            64.0,
            8.0,
            128.0,
            4.0
    ));

    public final BooleanSetting self = add(new BooleanSetting(
            "Self",
            "self",
            "Render your own player in third person",
            false
    ));

    public final ColorSetting fillColor = add(new ColorSetting(
            "Fill color",
            "fill_color",
            "Color of the player silhouette",
            0xFF745CFF
    ));

    public final ColorSetting glowColor = add(new ColorSetting(
            "Glow color",
            "glow_color",
            "Color of the outer glow",
            0xFFE9E4FF
    ));

    public ShaderESP() {
        INSTANCE = this;
        echoDecay.visibleWhen(() -> mode.is("Echo"));
    }

    @Override
    protected void onDisable() {
        ShaderEspRenderer.reset();
    }

    public int modeIndex() {
        return switch (mode.get()) {
            case "Echo" -> 1;
            case "Full" -> 2;
            default -> 0;
        };
    }

    public boolean shouldRender(int entityId, double squaredDistance) {
        if (mc.player == null) return false;
        if (!self.get() && entityId == mc.player.getId()) return false;
        double maxDistance = distance.get();
        return squaredDistance <= maxDistance * maxDistance;
    }

    public static ShaderESP active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }
}
