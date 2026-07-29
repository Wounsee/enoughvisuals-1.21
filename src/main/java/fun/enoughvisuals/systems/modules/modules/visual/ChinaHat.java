package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Register(name = "ChinaHat", id = "china_hat", description = "Шляпа мелстроя", category = Category.VISUAL)
public final class ChinaHat extends Module {
    private final fun.enoughvisuals.port.ChinaHat renderer = new fun.enoughvisuals.port.ChinaHat();

    public ChinaHat() {
        fun.enoughvisuals.port.VisualPortRegistry.chinaHat = renderer;
    }

    @Override
    protected void onEnable() {
        renderer.enabled = true;
        renderer.onEnable();
    }

    @Override
    protected void onDisable() {
        renderer.onDisable();
        renderer.enabled = false;
    }

    public static void renderForPlayer(PlayerEntityRenderState state, PlayerEntityModel model,
                                       MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        fun.enoughvisuals.port.ChinaHat.renderForPlayer(state, model, matrices, buffers, light);
    }
}
