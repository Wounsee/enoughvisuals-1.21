package fun.enoughvisuals.mixins.minecraft.client.gui.widget;

import fun.enoughvisuals.ui.screen.VanillaScreenStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetStyleMixin {
    @Shadow protected double value;
    @Unique private float enoughvisuals$hover;
    @Unique private long enoughvisuals$lastFrame = System.nanoTime();

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$renderCustomSlider(DrawContext context, int mouseX, int mouseY,
                                                   float delta, CallbackInfo ci) {
        if (!VanillaScreenStyle.shouldStyle()) return;
        ClickableWidget widget = (ClickableWidget) (Object) this;
        long now = System.nanoTime();
        enoughvisuals$hover = VanillaScreenStyle.animateHover(enoughvisuals$hover,
                widget.isHovered(), now - enoughvisuals$lastFrame);
        enoughvisuals$lastFrame = now;
        VanillaScreenStyle.drawSlider(context, widget, value, 1f, enoughvisuals$hover);
        ci.cancel();
    }
}
