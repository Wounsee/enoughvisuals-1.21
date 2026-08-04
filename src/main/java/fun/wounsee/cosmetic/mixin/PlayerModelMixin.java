package fun.wounsee.cosmetic.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin extends HumanoidModel<PlayerRenderState> {

    public PlayerModelMixin(ModelPart modelPart) {
        super(modelPart);
    }

    @Inject(at = @At("HEAD"), method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V")
    private void showHeadOnAnim(CallbackInfo ci) {
        this.head.visible = true;
    }
}
