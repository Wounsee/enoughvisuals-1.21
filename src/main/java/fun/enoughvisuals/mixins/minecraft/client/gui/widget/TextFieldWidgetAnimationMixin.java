package fun.enoughvisuals.mixins.minecraft.client.gui.widget;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetAnimationMixin {

    @Shadow private String text;
    @Shadow private int firstCharacterIndex;
    @Shadow private int selectionStart;
    @Shadow private int editableColor;
    @Shadow private int uneditableColor;
    @Shadow private boolean editable;
    @Shadow private String suggestion;

    @Shadow public abstract boolean isVisible();
    @Shadow public abstract int getInnerWidth();

    @Unique private String enough$lastText = "";
    @Unique private int enough$typedIndex = -1;
    @Unique private long enough$typedStartMs = 0L;
    @Unique private String enough$removedChar = "";
    @Unique private int enough$removedIndex = -1;
    @Unique private long enough$removedStartMs = 0L;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void enough$renderAnimatedChatInput(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof ChatScreen) || Animations.active() == null) {
            enough$lastText = text == null ? "" : text;
            return;
        }

        TextFieldWidget self = (TextFieldWidget) (Object) this;
        if (!isVisible()) {
            ci.cancel();
            return;
        }

        ci.cancel();
        String value = text == null ? "" : text;
        enough$updateInputAnimation(value);

        TextRenderer textRenderer = client.textRenderer;
        int x = self.getX();
        int y = self.getY();
        int maxWidth = Math.max(4, getInnerWidth());
        int first = Math.max(0, Math.min(firstCharacterIndex, value.length()));
        String visible = value.substring(first);
        String clipped = textRenderer.trimToWidth(visible, maxWidth);
        int color = enough$opaque(editable ? editableColor : uneditableColor);

        int typedVisibleIndex = enough$typedIndex - first;
        if (typedVisibleIndex < 0 || typedVisibleIndex >= clipped.length()) {
            typedVisibleIndex = -1;
        }

        enough$drawVanillaAnimatedInput(ctx, textRenderer, clipped, x, y, color, typedVisibleIndex, enough$typedStartMs);
        enough$drawRemovedChar(ctx, textRenderer, clipped, x, y, color, first);
        enough$drawSuggestion(ctx, textRenderer, clipped, x, y);
        enough$drawCursor(ctx, textRenderer, self, value, clipped, x, y, color, first);
        enough$cleanupAnimationTimers();
    }

    @Unique
    private void enough$updateInputAnimation(String value) {
        if (value.equals(enough$lastText)) return;

        long now = System.currentTimeMillis();
        int prefix = enough$commonPrefix(enough$lastText, value);
        if (value.length() > enough$lastText.length()) {
            enough$typedIndex = prefix;
            enough$typedStartMs = now;
            enough$removedChar = "";
            enough$removedStartMs = 0L;
        } else if (value.length() < enough$lastText.length()) {
            enough$removedIndex = prefix;
            enough$removedChar = enough$lastText.substring(prefix, Math.min(prefix + 1, enough$lastText.length()));
            enough$removedStartMs = now;
            enough$typedIndex = -1;
            enough$typedStartMs = 0L;
        } else {
            enough$typedIndex = -1;
            enough$typedStartMs = 0L;
            enough$removedChar = "";
            enough$removedStartMs = 0L;
        }
        enough$lastText = value;
    }

    @Unique
    private void enough$drawVanillaAnimatedInput(DrawContext ctx, TextRenderer textRenderer, String text,
                                                 int x, int y, int color, int typedIndex, long typedStartMs) {
        if (text == null || text.isEmpty()) return;

        if (typedIndex < 0 || typedIndex >= text.length()) {
            ctx.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }

        String prefix = typedIndex > 0 ? text.substring(0, typedIndex) : "";
        String symbol = text.substring(typedIndex, typedIndex + 1);
        String suffix = typedIndex + 1 < text.length() ? text.substring(typedIndex + 1) : "";
        int prefixW = prefix.isEmpty() ? 0 : textRenderer.getWidth(prefix);
        int symbolW = Math.max(1, textRenderer.getWidth(symbol));
        int symbolX = x + prefixW;

        if (!prefix.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, prefix, x, y, color);
        }

        float progress = typedStartMs <= 0L ? 1f
                : MathHelper.clamp((System.currentTimeMillis() - typedStartMs) / 220f, 0f, 1f);
        float eased = RenderUtil.easeOutBack(progress);
        float scale = 0.1f + 0.9f * eased;
        int symbolColor = RenderUtil.withAlpha(color, RenderUtil.getAlpha(color) * (0.7f + 0.3f * Math.min(1f, eased)));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(symbolX + symbolW * 0.5f, y + 4.5f, 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * (1f - Math.min(1f, eased))));
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawTextWithShadow(textRenderer, symbol, (int) (-symbolW * 0.5f), -4, symbolColor);
        ctx.getMatrices().pop();

        if (!suffix.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, suffix, symbolX + symbolW, y, color);
        }
    }

    @Unique
    private void enough$drawRemovedChar(DrawContext ctx, TextRenderer textRenderer, String clipped,
                                        int x, int y, int color, int first) {
        if (enough$removedChar.isEmpty() || enough$removedStartMs <= 0L) return;
        int removedVisibleIndex = enough$removedIndex - first;
        if (removedVisibleIndex < 0 || removedVisibleIndex > clipped.length()) return;

        float progress = MathHelper.clamp((System.currentTimeMillis() - enough$removedStartMs) / 170f, 0f, 1f);
        float eased = RenderUtil.easeInQuad(progress);
        String prefix = clipped.substring(0, Math.min(removedVisibleIndex, clipped.length()));
        int symbolW = Math.max(1, textRenderer.getWidth(enough$removedChar));
        int symbolX = x + textRenderer.getWidth(prefix);
        float scale = 1f - 0.72f * eased;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(symbolX + symbolW * 0.5f, y + 4.5f, 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * eased));
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawTextWithShadow(textRenderer, enough$removedChar, (int) (-symbolW * 0.5f), -4,
                RenderUtil.withAlpha(color, RenderUtil.getAlpha(color) * (1f - eased)));
        ctx.getMatrices().pop();
    }

    @Unique
    private void enough$drawSuggestion(DrawContext ctx, TextRenderer textRenderer, String clipped, int x, int y) {
        if (suggestion == null || suggestion.isEmpty()) return;
        int textW = textRenderer.getWidth(clipped);
        ctx.drawTextWithShadow(textRenderer, suggestion, x + textW, y, 0xFF808080);
    }

    @Unique
    private void enough$drawCursor(DrawContext ctx, TextRenderer textRenderer, TextFieldWidget self,
                                   String value, String clipped, int x, int y, int color, int first) {
        if (!self.isFocused() || (System.currentTimeMillis() / 300L) % 2L == 0L) return;
        int cursor = Math.max(0, Math.min(selectionStart, value.length())) - first;
        cursor = Math.max(0, Math.min(cursor, clipped.length()));
        String beforeCursor = clipped.substring(0, cursor);
        int cursorX = x + textRenderer.getWidth(beforeCursor);
        ctx.fill(cursorX, y - 1, cursorX + 1, y + 10, color);
    }

    @Unique
    private void enough$cleanupAnimationTimers() {
        long now = System.currentTimeMillis();
        if (enough$typedStartMs > 0L && now - enough$typedStartMs > 230L) {
            enough$typedIndex = -1;
            enough$typedStartMs = 0L;
        }
        if (enough$removedStartMs > 0L && now - enough$removedStartMs > 190L) {
            enough$removedChar = "";
            enough$removedIndex = -1;
            enough$removedStartMs = 0L;
        }
    }

    @Unique
    private int enough$commonPrefix(String a, String b) {
        int max = Math.min(a == null ? 0 : a.length(), b == null ? 0 : b.length());
        for (int i = 0; i < max; i++) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        return max;
    }

    @Unique
    private int enough$opaque(int color) {
        return (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
    }
}
