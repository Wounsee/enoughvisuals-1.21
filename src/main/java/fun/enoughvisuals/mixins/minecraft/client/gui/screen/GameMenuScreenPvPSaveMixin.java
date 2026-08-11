package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.systems.modules.modules.utils.PvPSave;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextWidget;
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

    @Inject(method = "init", at = @At("TAIL"))
    private void enough$centerPauseMenu(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget widget && !(widget instanceof TextWidget)) {
                top = Math.min(top, widget.getY());
                bottom = Math.max(bottom, widget.getY() + widget.getHeight());
            }
        }
        if (top == Integer.MAX_VALUE || bottom <= top) return;

        int offset = (screen.height - (bottom - top)) / 2 - top;
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget widget && !(widget instanceof TextWidget)) {
                widget.setY(widget.getY() + offset);
            } else if (element instanceof TextWidget title) {
                title.setY(Math.max(8, top + offset - title.getHeight() - 8));
            }
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
