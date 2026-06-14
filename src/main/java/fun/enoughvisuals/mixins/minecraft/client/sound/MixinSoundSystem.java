package fun.enoughvisuals.mixins.minecraft.client.sound;

import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import fun.enoughvisuals.systems.modules.modules.utils.FakePlayer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class MixinSoundSystem {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void onPlaySound(SoundInstance sound, CallbackInfo ci) {
        if (sound != null && FakePlayer.shouldSuppressLocalAttackSound() && isAttackSound(sound.getId().getPath())) {
            ci.cancel();
            return;
        }

        if (sound == null || NoRender.INSTANCE == null || !NoRender.INSTANCE.isEnabled()) return;

        String id = sound.getId().getPath();

        if (NoRender.INSTANCE.beaconSound.get() && id.contains("beacon")) ci.cancel();
        if (NoRender.INSTANCE.phantomSound.get() && id.contains("phantom")) ci.cancel();
        if (NoRender.INSTANCE.weatherSound.get() && (id.contains("rain") || id.contains("thunder"))) ci.cancel();
    }

    private static boolean isAttackSound(String id) {
        return id != null
                && id.startsWith("entity.player.attack.")
                && (id.endsWith("crit")
                || id.endsWith("knockback")
                || id.endsWith("nodamage")
                || id.endsWith("strong")
                || id.endsWith("sweep")
                || id.endsWith("weak"));
    }
}
