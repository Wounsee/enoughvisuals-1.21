package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudNameProtectMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void enough$protectPlayerListName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(NameProtect.protect(cir.getReturnValue()));
    }
}
