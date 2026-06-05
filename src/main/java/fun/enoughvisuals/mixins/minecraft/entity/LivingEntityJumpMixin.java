package fun.enoughvisuals.mixins.minecraft.entity;

import fun.enoughvisuals.systems.modules.modules.visual.JumpEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Inject(method = "jump", at = @At("TAIL"))
    private void enough$onJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ClientPlayerEntity clientPlayer)) return;
        if (MinecraftClient.getInstance().player != clientPlayer) return;
        JumpEffect.onLocalPlayerJump();
    }
}
