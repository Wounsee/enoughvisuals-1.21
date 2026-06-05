package fun.enoughvisuals.mixins.minecraft.client.render.entity;

import fun.enoughvisuals.systems.modules.modules.visual.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Unique
    private boolean enoughvisuals$itemPhysicsPushed;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void enoughvisuals$applyItemPhysics(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ItemPhysics itemPhysics = ItemPhysics.INSTANCE;
        if (itemPhysics == null || !itemPhysics.isEnabled()) return;

        state.age = 0.0f;
        state.uniqueOffset = 0.0f;
        matrices.push();
        itemPhysics.applyTransform(matrices);
        enoughvisuals$itemPhysicsPushed = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void enoughvisuals$popItemPhysics(ItemEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!enoughvisuals$itemPhysicsPushed) return;

        enoughvisuals$itemPhysicsPushed = false;
        matrices.pop();
    }
}
