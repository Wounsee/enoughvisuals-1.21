package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Z", at = @At("HEAD"), cancellable = true)
    private void enough$skipPlayerFeaturesDuringShaderMask(PlayerEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (ShaderEspRenderer.isRenderingMask()) {
            cir.setReturnValue(false);
        }
    }
}
