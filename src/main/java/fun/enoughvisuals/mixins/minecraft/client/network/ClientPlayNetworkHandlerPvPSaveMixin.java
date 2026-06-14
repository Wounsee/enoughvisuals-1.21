package fun.enoughvisuals.mixins.minecraft.client.network;

import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerPvPSaveMixin {

    @Inject(method = "onEntityDamage", at = @At("HEAD"))
    private void enough$confirmPlayerDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        PvPSave.confirmDamage(packet.entityId(), packet.sourceCauseId());
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void enough$protectPvPCommand(String command, CallbackInfo ci) {
        if (PvPSave.blockCommand(command)) {
            ci.cancel();
        }
    }
}
