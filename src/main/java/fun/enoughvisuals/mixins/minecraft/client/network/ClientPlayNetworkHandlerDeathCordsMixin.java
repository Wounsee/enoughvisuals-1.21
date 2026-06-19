package fun.enoughvisuals.mixins.minecraft.client.network;

import fun.enoughvisuals.systems.modules.modules.utils.DeathCords;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerDeathCordsMixin {

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void enough$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Entity entity = packet.getEntity(mc.world);
        if (entity != mc.player) return;

        byte status = packet.getStatus();
        // EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES = 3
        if (status == 3) {
            DeathCords.onDeath();
        }
    }
}
