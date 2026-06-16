package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void enough$skipPauseMenuBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null && client.getWindow() != null) {
            GlassUtil.drawScreenBackgroundBlur(context,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight());
        }
        ci.cancel();
    }
}
