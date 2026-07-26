package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.HitRange;
import fun.enoughvisuals.utils.render.HitRangeCircleRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "renderEntities", at = @At(value = "TAIL"))
    private void enough$drawHitRangeSelf(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo ci
    ) {
        HitRange config = HitRange.getInstance();
        if (config == null || !config.isEnabled() || !config.showSelf.get()) {
            return;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }

        float tickDelta = tickCounter.getTickDelta(false);
        double px = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
        double py = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
        double pz = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

        Vec3d playerPos = new Vec3d(px, py, pz).subtract(camera.getPos());

        EntityRenderer<? super ClientPlayerEntity, ?> renderer = this.entityRenderDispatcher.getRenderer(player);
        PlayerEntityRenderState state = (PlayerEntityRenderState) renderer.getAndUpdateRenderState(player, tickDelta);

        matrices.push();
        matrices.translate(playerPos.x, playerPos.y, playerPos.z);
        VertexConsumerProvider vertexConsumers = this.bufferBuilders.getEntityVertexConsumers();
        HitRangeCircleRenderer.drawCircle(matrices, vertexConsumers, state);
        matrices.pop();
    }
}
