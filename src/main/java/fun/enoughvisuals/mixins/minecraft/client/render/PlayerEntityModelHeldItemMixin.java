package fun.enoughvisuals.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.enoughvisuals.systems.emotions.render.EmotionAttachmentContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Substitutes the lower BOBJ arm only while vanilla builds a held-item matrix. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelHeldItemMixin {
    @Inject(method = "setArmAngle", at = @At("HEAD"))
    private void enough$useEmotionForearm(
            Arm arm,
            MatrixStack matrices,
            CallbackInfo ci,
            @Share("enough$handScope") LocalRef<EmotionAttachmentContext.HandScope> scopeRef
    ) {
        scopeRef.set(EmotionAttachmentContext.beginHand((PlayerEntityModel) (Object) this, arm));
    }

    @Inject(method = "setArmAngle", at = @At("RETURN"))
    private void enough$restoreUpperArm(
            Arm arm,
            MatrixStack matrices,
            CallbackInfo ci,
            @Share("enough$handScope") LocalRef<EmotionAttachmentContext.HandScope> scopeRef
    ) {
        EmotionAttachmentContext.HandScope scope = scopeRef.get();
        if (scope != null) scope.restore();
        scopeRef.set(null);
    }
}
