package fun.enoughvisuals.mixins.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps cookies, logins, local storage and IndexedDB in MCEF's persistent data directory. */
@Mixin(targets = "com.cinemamod.mcef.MCEFSettings", remap = false)
public abstract class McefSettingsMixin {
    @Shadow
    private boolean useCache;

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void enough$forcePersistentBrowserStorage(CallbackInfo ci) {
        useCache = true;
    }
}
