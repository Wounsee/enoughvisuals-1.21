package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import java.awt.Color;

@Register(name = "Particles", id = "particles", description = "Частицы в мире", category = Category.VISUAL)
public final class Particles extends Module {
    private static Particles INSTANCE;
    private final fun.enoughvisuals.port.Particles ported = new fun.enoughvisuals.port.Particles();

    public final ModeSetting type = add(new ModeSetting("Тип частиц", "type", "", "Bloom",
            "Bloom", "Star", "Snow", "Heart", "Dollar", "Triangle", "Sakura", "Genshin", "Rhombus"));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "color_mode", "", "Клиентовский", "Клиентовский", "Свой"));
    public final ColorSetting customColor = add(new ColorSetting("Кастом цвет", "custom_color", "", 0xFFFF4000));
    public final SliderSetting size = add(new SliderSetting("Размер", "size", "", 0.5, 0.0, 1.0, 0.1));
    public final SliderSetting count = add(new SliderSetting("Количество", "count", "", 10.0, 10.0, 100.0, 10.0));
    public final SliderSetting lifetime = add(new SliderSetting("Время жизни", "lifetime", "", 2.0, 0.5, 10.0, 0.5, "s"));
    public final SliderSetting worldRadius = add(new SliderSetting("Радиус в мире", "world_radius", "", 12.0, 2.0, 50.0, 1.0));
    public final BooleanSetting physics = add(new BooleanSetting("Физика", "physics", "", true));

    public Particles() {
        INSTANCE = this;
        customColor.visibleWhen(() -> colorMode.getIndex() == 1);
    }

    @Override
    protected void onEnable() {
        ported.enabled = true;
        ported.onWorldJoin(new fun.enoughvisuals.port.WorldJoinEvent());
    }

    @Override
    protected void onDisable() {
        ported.onWorldJoin(new fun.enoughvisuals.port.WorldJoinEvent());
        ported.enabled = false;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || !isInGame()) return;
        sync();
        ported.onPlayerMotion(new fun.enoughvisuals.port.PlayerMotionEvent());
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || context == null || context.matrixStack() == null) return;
        INSTANCE.sync();
        float tick = context.tickCounter() == null ? 1.0F : context.tickCounter().getTickDelta(true);
        INSTANCE.ported.onRender3D(new fun.enoughvisuals.port.Render3DEvent(context.matrixStack(), tick));
    }

    private void sync() {
        ported.enabled = true;
        fun.enoughvisuals.port.Particles.spavnitPri.options.get(0).setValue(false);
        fun.enoughvisuals.port.Particles.spavnitPri.options.get(1).setValue(false);
        fun.enoughvisuals.port.Particles.spavnitPri.options.get(2).setValue(true);
        fun.enoughvisuals.port.Particles.tipChastits.value = type.get();
        fun.enoughvisuals.port.Particles.rezhimTsveta.value =
                fun.enoughvisuals.port.Particles.rezhimTsveta.options.get(colorMode.getIndex());
        fun.enoughvisuals.port.Particles.kastomTsvet.invoke(new Color(customColor.get(), true));
        fun.enoughvisuals.port.Particles.razmer.invoke(size.get().floatValue());
        fun.enoughvisuals.port.Particles.kolichestvo.invoke(count.get().floatValue());
        fun.enoughvisuals.port.Particles.vremyaZhizni.invoke(lifetime.get().floatValue());
        fun.enoughvisuals.port.Particles.radiusVMire.invoke(worldRadius.get().floatValue());
        fun.enoughvisuals.port.Particles.fizika.setValue(physics.get());
    }
}
