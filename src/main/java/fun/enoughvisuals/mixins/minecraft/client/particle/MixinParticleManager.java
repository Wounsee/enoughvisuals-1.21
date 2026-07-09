package fun.enoughvisuals.mixins.minecraft.client.particle;

import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import fun.enoughvisuals.systems.modules.modules.visual.HitParticles;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.breakParticles.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("HEAD"), cancellable = true)
    private void onAddCritEmitter(Entity entity, ParticleEffect parameters, CallbackInfo ci) {
        if (isCritSuppressed(parameters)) {
            ci.cancel();
        }
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V", at = @At("HEAD"), cancellable = true)
    private void onAddCritEmitter(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci) {
        if (isCritSuppressed(parameters)) {
            ci.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void onAddCritParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (isCritSuppressed(parameters)) {
            cir.setReturnValue(null);
        }
    }

    private boolean isCritSuppressed(ParticleEffect parameters) {
        return HitParticles.shouldDisableCritParticles() && parameters == ParticleTypes.CRIT;
    }
}
