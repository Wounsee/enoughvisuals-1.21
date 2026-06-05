package fun.enoughvisuals.mixins.minecraft.client.gui;

import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextNameProtectMixin {

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V", at = @At("HEAD"), argsOnly = true)
    private String enough$protectCenteredString(String text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V", at = @At("HEAD"), argsOnly = true)
    private Text enough$protectCenteredText(Text text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)V", at = @At("HEAD"), argsOnly = true)
    private OrderedText enough$protectCenteredOrdered(OrderedText text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", at = @At("HEAD"), argsOnly = true)
    private String enough$protectShadowString(String text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private String enough$protectString(String text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I", at = @At("HEAD"), argsOnly = true)
    private OrderedText enough$protectShadowOrdered(OrderedText text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private OrderedText enough$protectOrdered(OrderedText text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I", at = @At("HEAD"), argsOnly = true)
    private Text enough$protectShadowText(Text text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private Text enough$protectText(Text text) {
        return NameProtect.protect(text);
    }
}
