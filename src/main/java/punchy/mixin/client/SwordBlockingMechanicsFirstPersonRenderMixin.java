package punchy.mixin.client;

import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_742;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.compat.SwordBlockingMechanicsCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/SwordBlockingMechanicsFirstPersonRenderMixin.class */
@Pseudo
@Mixin(targets = {"fuzs/swordblockingmechanics/client/handler/FirstPersonRenderingHandler"}, remap = false)
public class SwordBlockingMechanicsFirstPersonRenderMixin {
    @Inject(method = {"onRenderBothHands(Lnet/minecraft/client/renderer/ItemInHandRenderer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFFFF)Lfuzs/puzzleslib/api/event/v1/core/EventResult;"}, at = {@At("HEAD")}, cancellable = true, require = 0)
    private static void punchy$disableSwordBlockingFirstPersonRender(class_759 itemInHandRenderer, class_1268 interactionHand, class_742 player, class_1306 humanoidArm, class_1799 itemStack, class_4587 poseStack, class_4597 bufferSource, int combinedLight, float partialTick, float interpolatedPitch, float swingProgress, float equipProgress, CallbackInfoReturnable<Object> cir) {
        Object pass;
        if (SwordBlockingMechanicsCompat.isAvailable() && (pass = SwordBlockingMechanicsCompat.getEventResultPass()) != null) {
            cir.setReturnValue(pass);
            cir.cancel();
        }
    }
}
