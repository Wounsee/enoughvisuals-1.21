package fun.enoughvisuals.mixins.minecraft.client.gui.widget;

import fun.enoughvisuals.ui.screen.VanillaScreenStyle;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextWidget.class)
public abstract class TextWidgetStyleMixin {
    @Shadow private float horizontalAlignment;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void enoughvisuals$renderCustomText(DrawContext context, int mouseX, int mouseY,
                                                 float delta, CallbackInfo ci) {
        if (!VanillaScreenStyle.shouldStyle()) return;
        TextWidget widget = (TextWidget) (Object) this;
        float size = 7.15f;
        String text = CustomFontRenderer.trimToWidth(widget.getMessage().getString(),
                Math.max(1f, widget.getWidth()), size, FontWeight.SEMIBOLD);
        float textWidth = CustomFontRenderer.getTextWidth(text, size, FontWeight.SEMIBOLD);
        float x = widget.getX() + horizontalAlignment * (widget.getWidth() - textWidth);
        float reveal = VanillaScreenStyle.appearance();
        float y = widget.getY() + (widget.getHeight()
                - CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)) * 0.5f - 0.2f;
        CustomFontRenderer.drawText(context, text, x, y, size,
                RenderUtil.withAlpha(0xFFF7F4FC, 0.98f * reveal), FontWeight.SEMIBOLD);
        ci.cancel();
    }
}
