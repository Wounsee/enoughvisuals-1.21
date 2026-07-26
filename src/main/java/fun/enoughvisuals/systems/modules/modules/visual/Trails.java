package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

@Register(name = "Trails", id = "trails", description = "Плавный шейдерный след за игроком", category = Category.VISUAL)
public final class Trails extends Module {
    private static Trails INSTANCE;
    private final fun.enoughvisuals.port.Trails renderer = new fun.enoughvisuals.port.Trails();

    public Trails() {
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        renderer.enabled = true;
        renderer.onEnable();
        renderer.onWorldJoin(new fun.enoughvisuals.port.WorldJoinEvent());
    }

    @Override
    protected void onDisable() {
        renderer.onDisable();
        renderer.enabled = false;
    }

    public static void renderInWorld(WorldRenderContext context) {
        Trails module = INSTANCE;
        if (module == null || !module.isEnabled() || context == null || context.matrixStack() == null
                || mc.player == null || mc.player.isInvisible()) return;
        float tick = context.tickCounter() == null ? 1.0F : context.tickCounter().getTickDelta(true);
        module.renderer.onRender3D(new fun.enoughvisuals.port.Render3DEvent(context.matrixStack(), tick));
    }
}
