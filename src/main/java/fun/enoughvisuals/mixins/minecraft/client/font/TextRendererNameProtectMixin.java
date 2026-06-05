package fun.enoughvisuals.mixins.minecraft.client.font;

import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class TextRendererNameProtectMixin {

    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("HEAD"), argsOnly = true)
    private String enough$protectDrawString(String text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("HEAD"), argsOnly = true)
    private Text enough$protectDrawText(Text text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I", at = @At("HEAD"), argsOnly = true)
    private Text enough$protectDrawTextShadowLayer(Text text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I", at = @At("HEAD"), argsOnly = true)
    private OrderedText enough$protectDrawOrdered(OrderedText text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String enough$protectWidthString(String text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/OrderedText;)I", at = @At("HEAD"), argsOnly = true)
    private OrderedText enough$protectWidthOrdered(OrderedText text) {
        return NameProtect.protect(text);
    }

    @ModifyVariable(method = "getWidth(Lnet/minecraft/text/StringVisitable;)I", at = @At("HEAD"), argsOnly = true)
    private StringVisitable enough$protectWidthVisitable(StringVisitable text) {
        if (text == null) return null;
        return StringVisitable.plain(NameProtect.protect(text.getString()));
    }
}
