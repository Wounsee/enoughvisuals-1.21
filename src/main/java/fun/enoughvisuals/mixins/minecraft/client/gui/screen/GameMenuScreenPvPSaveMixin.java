package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenPvPSaveMixin {

    @Invoker("disconnect")
    protected abstract void enough$invokeDisconnect();

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void enough$confirmMenuExit(CallbackInfo ci) {
        Screen current = (Screen) (Object) this;
        if (PvPSave.requestExit(current, this::enough$invokeDisconnect)) {
            ci.cancel();
        }
    }
}
