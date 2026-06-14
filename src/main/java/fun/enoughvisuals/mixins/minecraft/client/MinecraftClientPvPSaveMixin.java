package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientPvPSaveMixin {

    @Inject(method = "scheduleStop", at = @At("HEAD"), cancellable = true)
    private void enough$confirmScheduledStop(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        Screen previous = client.currentScreen;
        if (PvPSave.requestExit(previous, client::scheduleStop)) {
            ci.cancel();
        }
    }

    @Inject(method = "disconnect()V", at = @At("HEAD"), cancellable = true)
    private void enough$confirmDisconnect(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        Screen previous = client.currentScreen;
        if (PvPSave.requestExit(previous, client::disconnect)) {
            ci.cancel();
        }
    }
}
