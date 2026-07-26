package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

@Register(name = "JumpCircle", id = "jump_circle", description = "Гравитационная линза при прыжке", category = Category.VISUAL)
public final class JumpCircle extends Module {
    private static JumpCircle INSTANCE;
    private final fun.enoughvisuals.port.JumpCircle renderer = new fun.enoughvisuals.port.JumpCircle();

    public JumpCircle() {
        INSTANCE = this;
    }

    public static void onLocalPlayerJump() {
        JumpCircle module = INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null || mc.world == null
                || mc.player.getVelocity().y <= 0.02) return;
        module.renderer.onJump();
    }

    @Override
    protected void onEnable() {
        renderer.enabled = true;
        renderer.clear();
    }

    @Override
    protected void onDisable() {
        renderer.enabled = false;
        renderer.clear();
    }

    public static void renderInWorld(WorldRenderContext context) {
        JumpCircle module = INSTANCE;
        if (module == null || !module.isEnabled() || context == null || context.matrixStack() == null) return;
        float tick = context.tickCounter() == null ? 1.0F : context.tickCounter().getTickDelta(true);
        module.renderer.onRender3D(new fun.enoughvisuals.port.Render3DEvent(context.matrixStack(), tick));
    }
}
