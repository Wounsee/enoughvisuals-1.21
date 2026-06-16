package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void enough$skipInGameScreenBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (enough$hasWorld()) {
            enough$drawBlurredBackground(context);
            ci.cancel();
        }
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void enough$skipInGameGradient(DrawContext context, CallbackInfo ci) {
        if (enough$hasWorld()) {
            enough$drawBlurredBackground(context);
            ci.cancel();
        }
    }

    private boolean enough$hasWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.world != null;
    }

    private void enough$drawBlurredBackground(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        GlassUtil.drawScreenBackgroundBlur(context,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight());
    }
}
