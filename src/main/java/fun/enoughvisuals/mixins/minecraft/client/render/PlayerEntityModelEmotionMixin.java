package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelEmotionMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void enough$applyEmotion(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        EmotionWheelManager.getInstance().apply(state, model.head, model.hat, model.body,
                model.rightArm, model.leftArm, model.rightLeg, model.leftLeg);
    }
}
