package fun.wounsee.cosmetic.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.level.Level;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientPacketListener.class, priority = 999)
public abstract class ClientPacketListenerMixin {

    @Shadow public abstract ClientLevel getLevel();

    @Inject(at = @At("HEAD"), method = "sendUnsignedCommand", cancellable = true)
    private void sendUnsignedCommand(String command, CallbackInfoReturnable<Boolean> cir) {
        if (command.startsWith(CosmeticMod.MOD_ID))
            cir.setReturnValue(false);
    }

    @Inject(method = "handleEntityEvent", at = @At(value = "FIELD", target = "Lnet/minecraft/core/particles/ParticleTypes;TOTEM_OF_UNDYING:Lnet/minecraft/core/particles/SimpleParticleType;"), cancellable = true)
    private void handleTotem(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Level level = getLevel();
        Avatar avatar = AvatarManager.getAvatar(packet.getEntity(level));
        if (avatar != null && avatar.totemEvent())
            ci.cancel();
    }
}
