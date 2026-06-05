package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.modules.modules.visual.CustomTab;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudCustomTabMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void enough$cancelVanillaTab(CallbackInfo ci) {
        if (CustomTab.shouldRenderCustomTab()) {
            ci.cancel();
        }
    }
}
