package fun.enoughvisuals.mixins.minecraft.client.network;

import fun.enoughvisuals.systems.modules.modules.utils.HitSound;
import fun.enoughvisuals.systems.modules.modules.utils.FakePlayer;
import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.visual.HitParticles;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "swingHand", at = @At("HEAD"))
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        if (FakePlayer.shouldSuppressSwingHitModules()) return;
        if (net.minecraft.client.MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hitResult) {
            HitSound.playHit();
            HitParticles.onHit(hitResult.getEntity());
        }
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LockSlot module = LockSlot.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (module != null && module.isEnabled()) {
            // Проверяем выбранный слот в хотбаре
            if (module.isLocked(player.getInventory().selectedSlot)) {
                cir.setReturnValue(false); // Отменяем выбрасывание
            }
        }
    }
}
