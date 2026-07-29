package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;

@Register(name = "Attack Effect", id = "attack_effect", description = "Эффект в точке удара", category = Category.VISUAL)
public final class AttackEffect extends Module {
    private static AttackEffect INSTANCE;
    private static final Matrix4f POSITION = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean matricesReady;
    private final fun.enoughvisuals.port.AttackEffect renderer = new fun.enoughvisuals.port.AttackEffect();

    public final ModeSetting mode = add(new ModeSetting("Режим", "mode", "", "Торус", "Торус", "Плазма"));
    public final SliderSetting waveRadius = add(new SliderSetting("Радиус волны", "wave_radius", "", 1.8, .1, 3.5, .05));
    public final SliderSetting refraction = add(new SliderSetting("Сила преломления", "refraction", "", .06, .01, .16, .001));
    public final SliderSetting duration = add(new SliderSetting("Длительность", "duration", "", 420, 200, 1600, 10, "ms"));
    public final BooleanSetting spectral = add(new BooleanSetting("Спектральная дисперсия", "spectral", "", true));
    public final SliderSetting plasmaRadius = add(new SliderSetting("Радиус плазмы", "plasma_radius", "", 1.4, 1, 3, .05));
    public final SliderSetting temperature = add(new SliderSetting("Температура плазмы", "temperature", "", 1, .5, 2, .05));
    public final SliderSetting brightness = add(new SliderSetting("Яркость плазмы", "brightness", "", .8, .3, 1.6, .02));
    public final SliderSetting plasmaDuration = add(new SliderSetting("Длительность плазмы", "plasma_duration", "", 480, 220, 900, 10, "ms"));

    public AttackEffect() {
        INSTANCE = this;
        waveRadius.visibleWhen(() -> mode.is("Торус"));
        refraction.visibleWhen(() -> mode.is("Торус"));
        duration.visibleWhen(() -> mode.is("Торус"));
        spectral.visibleWhen(() -> mode.is("Торус"));
        plasmaRadius.visibleWhen(() -> mode.is("Плазма"));
        temperature.visibleWhen(() -> mode.is("Плазма"));
        brightness.visibleWhen(() -> mode.is("Плазма"));
        plasmaDuration.visibleWhen(() -> mode.is("Плазма"));
    }

    @Override
    protected void onEnable() {
        renderer.enabled = true;
    }

    @Override
    protected void onDisable() {
        renderer.onDisable();
        renderer.enabled = false;
        matricesReady = false;
    }

    public static void onAttack(Entity entity) {
        AttackEffect module = INSTANCE;
        if (module == null || !module.isEnabled() || entity == null || entity.isInvisible()) return;
        module.syncSettings();
        module.renderer.invoke(entity);
    }

    public static void captureWorldMatrices(Matrix4f position, Matrix4f projection) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || position == null || projection == null) return;
        POSITION.set(position);
        PROJECTION.set(projection);
        matricesReady = true;
    }

    public static void renderInWorld(WorldRenderContext context) {
        AttackEffect module = INSTANCE;
        if (module == null || !module.isEnabled() || !matricesReady || context == null || context.camera() == null
                || mc.world == null || mc.player == null) return;
        module.syncSettings();
        float tick = context.tickCounter() == null ? 1.0F : context.tickCounter().getTickDelta(true);
        fun.enoughvisuals.port.WorldRenderCapture capture = new fun.enoughvisuals.port.WorldRenderCapture(
                context.camera(), new Matrix4f(POSITION), new Matrix4f(PROJECTION));
        module.renderer.onWorldRenderContext(new fun.enoughvisuals.port.WorldRenderContextEvent(
                mc, mc.gameRenderer, capture, tick));
    }

    private void syncSettings() {
        renderer.enabled = true;
        renderer.rezhim.value = mode.get();
        renderer.radiusVolny.invoke(waveRadius.get().floatValue());
        renderer.silaPrelomleniya.invoke(refraction.get().floatValue());
        renderer.dlitelnost.invoke(duration.get().floatValue());
        renderer.spektralnayaDispersiya.setValue(spectral.get());
        renderer.neSkvozIgroka.setValue(false);
        renderer.radiusPlazmy.invoke(plasmaRadius.get().floatValue());
        renderer.temperaturaPlazmy.invoke(temperature.get().floatValue());
        renderer.yarkostPlazmy.invoke(brightness.get().floatValue());
        renderer.dlitelnostPlazmy.invoke(plasmaDuration.get().floatValue());
    }
}
