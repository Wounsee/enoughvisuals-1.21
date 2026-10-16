package fun.enoughvisuals.mixins.minecraft.client.render.item;

import fun.enoughvisuals.systems.modules.modules.visual.SwingAnimation;
import fun.enoughvisuals.systems.modules.modules.visual.ViewModel;
import fun.enoughvisuals.utils.render.ShaderHandsRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Unique
    private boolean enough$viewModelPushed = false;
    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack item, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void enough$onRenderHandsHead(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHandsRenderer.captureBeforeHands();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("RETURN"))
    private void enough$onRenderHandsReturn(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHandsRenderer.captureAfterHands();
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void enough$onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

        SwingAnimation swing = SwingAnimation.INSTANCE;
        boolean useCustomSwing = swing != null
                && swing.isEnabled()
                && swing.shouldApplyAnimation(item);

        ViewModel viewModel = ViewModel.INSTANCE;
        boolean useViewModel = viewModel != null && viewModel.isEnabled();

        if (useCustomSwing) {
            ci.cancel();

            matrices.push();
            try {
                if (useViewModel) {
                    viewModel.applyTransform(matrices, arm);
                }

                swing.applyTransform(matrices, swingProgress, arm);
                swing.applyHandOffset(matrices, arm);
                float enough$swingScale = swing.itemScale(arm);
                matrices.scale(enough$swingScale, enough$swingScale, enough$swingScale);

                if (!item.isEmpty()) {
                    boolean isRightArm = arm == Arm.RIGHT;
                    this.renderItem(
                            player,
                            item,
                            isRightArm ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                            !isRightArm,
                            matrices,
                            vertexConsumers,
                            light
                    );
                }
            } finally {
                matrices.pop();
            }
            return;
        }

        if (useViewModel) {
            matrices.push();
            viewModel.applyTransform(matrices, arm);
            enough$viewModelPushed = true;
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void enough$onRenderFirstPersonItemReturn(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (enough$viewModelPushed) {
            enough$viewModelPushed = false;
            matrices.pop();
        }
    }
}
