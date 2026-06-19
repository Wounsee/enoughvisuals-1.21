package fun.enoughvisuals.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.enoughvisuals.systems.modules.modules.visual.HitColor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererHitColorMixin {

    @Unique
    private static final Identifier WHITE_TEXTURE = Identifier.of("minecraft", "textures/misc/white.png");

    @Unique
    private boolean enough$hitColorHurt = false;

    @Unique
    private VertexConsumerProvider enough$hitColorVcp = null;

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void enough$hitColorCaptureState(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        enough$hitColorHurt = state.hurt;
        enough$hitColorVcp = vertexConsumers;
        HitColor.setEntityHurt(state.hurt);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void enough$hitColorClearState(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        enough$hitColorHurt = false;
        enough$hitColorVcp = null;
        HitColor.setEntityHurt(false);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void enough$hitColorWrapModelRender(EntityModel<?> model, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color, Operation<Void> original) {
        HitColor module = HitColor.active();
        if (module != null && module.shouldColorBody() && enough$hitColorHurt && enough$hitColorVcp != null) {
            vertexConsumer = enough$hitColorVcp.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE));
            color = module.getHitColor();
        }
        original.call(model, matrices, vertexConsumer, light, overlay, color);
    }
}
