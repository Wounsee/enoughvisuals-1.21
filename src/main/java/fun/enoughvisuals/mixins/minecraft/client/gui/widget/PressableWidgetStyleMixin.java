package fun.enoughvisuals.mixins.minecraft.client.gui.widget;

import fun.enoughvisuals.ui.screen.VanillaScreenStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetStyleMixin {
    @Unique private float enoughvisuals$hover;
    @Unique private long enoughvisuals$lastFrame = System.nanoTime();

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$renderCustomButton(DrawContext context, int mouseX, int mouseY,
                                                   float delta, CallbackInfo ci) {
        ClickableWidget widget = (ClickableWidget) (Object) this;
        if (!VanillaScreenStyle.shouldStyleButton(widget)) return;
        long now = System.nanoTime();
        enoughvisuals$hover = VanillaScreenStyle.animateHover(enoughvisuals$hover,
                widget.isHovered(), now - enoughvisuals$lastFrame);
        enoughvisuals$lastFrame = now;
        VanillaScreenStyle.drawButton(context, widget, 1f, enoughvisuals$hover);
        ci.cancel();
    }
}
