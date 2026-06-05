package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.utils.render.CustomModelRenderer;
import fun.enoughvisuals.utils.render.PetRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void enough$hideVanillaPlayer(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState playerState) {
            PetRenderer.renderWorldPet(playerState.id, playerState.pose, matrices, vertexConsumers, light);
            if (CustomModelRenderer.shouldHideVanillaPlayer(playerState.id)) {
                ci.cancel();
            }
        }
    }
}
