package fun.enoughvisuals.mixins.minecraft.client.network;

import fun.enoughvisuals.systems.modules.modules.visual.TotemTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void enough$captureTotemPop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != TotemTracker.STATUS_USE_TOTEM) {
            return;
        }
        TotemTracker tracker = TotemTracker.getInstance();
        if (tracker == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        if (!mc.isOnThread()) {
            return;
        }
        ClientWorld w = this.world;
        if (w == null) {
            return;
        }
        Entity entity = packet.getEntity(w);
        if (entity instanceof LivingEntity living) {
            tracker.recordTotemUse(living);
        }
    }
}
