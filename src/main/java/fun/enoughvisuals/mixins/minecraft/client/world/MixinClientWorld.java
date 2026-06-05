package fun.enoughvisuals.mixins.minecraft.client.world;

import fun.enoughvisuals.systems.modules.modules.visual.CustomWorld;
import fun.enoughvisuals.systems.modules.modules.utils.HitSound;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class MixinClientWorld {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void enough$customSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        CustomWorld customWorld = CustomWorld.active();
        if (customWorld == null || !customWorld.overridesSkyColor()) return;
        Vector4f color = customWorld.skyColorVector();
        int r = Math.max(0, Math.min(255, Math.round(color.x * 255f)));
        int g = Math.max(0, Math.min(255, Math.round(color.y * 255f)));
        int b = Math.max(0, Math.min(255, Math.round(color.z * 255f)));
        cir.setReturnValue((r << 16) | (g << 8) | b);
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(double x, double y, double z, SoundEvent sound, net.minecraft.sound.SoundCategory category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        if (HitSound.INSTANCE != null && HitSound.INSTANCE.isEnabled() && HitSound.INSTANCE.shouldReplaceCrit()) {
                                                                         
            if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT || sound == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK) {
                ci.cancel();
            }
        }
    }
}
