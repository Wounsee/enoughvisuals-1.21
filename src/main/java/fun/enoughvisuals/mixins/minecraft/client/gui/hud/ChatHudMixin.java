package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Shadow private int scrolledLines;
    @Shadow private boolean hasUnreadNewMessages;

    @Shadow private boolean isChatHidden() { return false; }
    @Shadow private static double getMessageOpacityMultiplier(int age) { return 0.0; }
    @Shadow public abstract boolean isChatFocused();
    @Shadow public abstract int getWidth();
    @Shadow public abstract double getChatScale();
    @Shadow public abstract int getVisibleLineCount();
    @Shadow private int getLineHeight() { return 0; }
    @Shadow private int getMessageIndex(double x, double y) { return -1; }
    @Shadow private int getIndicatorX(ChatHudLine.Visible line) { return 0; }
    @Shadow private void drawIndicatorIcon(DrawContext context, int x, int y, MessageIndicator.Icon icon) {}

    @Unique
    private final Map<Long, Long> enough$messageAnimationStarts = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void enough$animatedRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        StencilUtil.resetRenderState();

        Animations animations = Animations.active();
        if (animations == null) {
            return;
        }

        ci.cancel();
        if (isChatHidden()) {
            StencilUtil.resetRenderState();
            return;
        }

        int visibleLineCount = getVisibleLineCount();
        int messageCount = visibleMessages.size();
        if (messageCount <= 0) {
            StencilUtil.resetRenderState();
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        double scale = getChatScale();
        int width = MathHelper.ceil(getWidth() / scale);
        int screenHeight = context.getScaledWindowHeight();
        int lineHeight = getLineHeight();
        int lineSpacingOffset = (int) Math.round(-8.0 * (client.options.getChatLineSpacing().getValue() + 1.0)
                + 4.0 * client.options.getChatLineSpacing().getValue());
        int bottomY = MathHelper.floor((screenHeight - 40) / scale);
        int hoveredEndIndex = getMessageIndex(enough$toChatLineX(mouseX), enough$toChatLineY(mouseY));
        double chatOpacity = client.options.getChatOpacity().getValue() * 0.9 + 0.1;
        double backgroundOpacity = client.options.getTextBackgroundOpacity().getValue();
        int renderedLines = 0;

        context.getMatrices().push();
        context.getMatrices().scale((float) scale, (float) scale, 1.0F);
        context.getMatrices().translate(4.0F, 0.0F, 0.0F);

        for (int lineIndex = 0; lineIndex + scrolledLines < visibleMessages.size() && lineIndex < visibleLineCount; lineIndex++) {
            int visibleIndex = lineIndex + scrolledLines;
            ChatHudLine.Visible line = visibleMessages.get(visibleIndex);
            if (line == null) continue;

            int age = currentTick - line.addedTime();
            if (age >= 200 && !focused) continue;

            double timeAlpha = focused ? 1.0 : getMessageOpacityMultiplier(age);
            int textAlpha = (int) (255.0 * timeAlpha * chatOpacity);
            int bgAlpha = (int) (255.0 * timeAlpha * backgroundOpacity);
            if (textAlpha <= 3) continue;

            renderedLines++;
            int y = bottomY - lineIndex * lineHeight;
            int textY = y + lineSpacingOffset;
            float[] messageAnimation = enough$messageAnimation(currentTick, line, focused, width);

            textAlpha = (int) (textAlpha * messageAnimation[3]);
            bgAlpha = (int) (bgAlpha * messageAnimation[3]);
            if (textAlpha <= 3) continue;

            context.getMatrices().push();
            context.getMatrices().translate(messageAnimation[0], messageAnimation[1], 0.0F);
            if (messageAnimation[2] != 1f) {
                context.getMatrices().translate(0f, textY + 4f, 0f);
                context.getMatrices().scale(messageAnimation[2], messageAnimation[2], 1f);
                context.getMatrices().translate(0f, -(textY + 4f), 0f);
            }

            context.fill(-4, y - lineHeight, width + 8, y, bgAlpha << 24);

            MessageIndicator indicator = line.indicator();
            if (indicator != null) {
                int indicatorColor = indicator.indicatorColor() | textAlpha << 24;
                context.fill(-4, y - lineHeight, -2, y, indicatorColor);
                if (visibleIndex == hoveredEndIndex && indicator.icon() != null) {
                    int iconX = getIndicatorX(line);
                    int iconY = textY + 9;
                    drawIndicatorIcon(context, iconX, iconY, indicator.icon());
                }
            }

            context.getMatrices().push();
            context.getMatrices().translate(0.0F, 0.0F, 50.0F);
            context.drawTextWithShadow(textRenderer, line.content(), 0, textY, (textAlpha << 24) | 0xFFFFFF);
            context.getMatrices().pop();
            context.getMatrices().pop();
        }

        if (focused) {
            int totalHeight = messageCount * lineHeight;
            int renderedHeight = renderedLines * lineHeight;
            int scrollbarY = scrolledLines * renderedHeight / messageCount - bottomY;
            int scrollbarH = renderedHeight * renderedHeight / totalHeight;
            if (totalHeight != renderedHeight) {
                int alpha = scrollbarY > 0 ? 170 : 96;
                int color = hasUnreadNewMessages ? 0xCC3333 : 0x3333AA;
                int x = width + 4;
                context.fill(x, -scrollbarY, x + 2, -scrollbarY - scrollbarH, 100, color + (alpha << 24));
                context.fill(x + 2, -scrollbarY, x + 1, -scrollbarY - scrollbarH, 100, 0xCCCCCC + (alpha << 24));
            }
        }

        context.getMatrices().pop();
        StencilUtil.resetRenderState();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void enough$resetStateAfterChat(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        StencilUtil.resetRenderState();
    }

    @Unique
    private float[] enough$messageAnimation(int currentTick, ChatHudLine.Visible line, boolean focused, int width) {
        if (focused || scrolledLines > 0) {
            return enough$messageAnimationValues(0f, 0f, 1f, 1f);
        }

        int age = currentTick - line.addedTime();
        if (age < 0 || age > 28) {
            return enough$messageAnimationValues(0f, 0f, 1f, 1f);
        }

        long key = enough$messageKey(line);
        long now = System.currentTimeMillis();
        long startedAt = enough$messageAnimationStarts.computeIfAbsent(key, ignored -> now);
        if (enough$messageAnimationStarts.size() > 256) {
            enough$messageAnimationStarts.clear();
            enough$messageAnimationStarts.put(key, startedAt);
        }

        float raw = Math.max(0f, Math.min(1f, (now - startedAt) / 430f));
        float move = RenderUtil.easeOutCubic(raw);
        float fade = RenderUtil.easeInOutCubic(raw);
        float offsetX = (1f - move) * (-(width + 12f));
        float offsetY = 0f;
        float scale = 1f;
        float alpha = Math.max(0.05f, fade);
        return enough$messageAnimationValues(offsetX, offsetY, scale, alpha);
    }

    @Unique
    private long enough$messageKey(ChatHudLine.Visible line) {
        long added = line == null ? 0L : line.addedTime();
        int contentHash = line == null || line.content() == null ? 0 : line.content().hashCode();
        return (added << 32) ^ (contentHash & 0xFFFFFFFFL);
    }

    private double enough$toChatLineX(double x) {
        return x / getChatScale() - 4.0;
    }

    private double enough$toChatLineY(double y) {
        double scaledY = client.getWindow().getScaledHeight() - y - 40.0;
        return scaledY / (getChatScale() * getLineHeight());
    }

    @Unique
    private float[] enough$messageAnimationValues(float offsetX, float offsetY, float scale, float alpha) {
        return new float[]{offsetX, offsetY, scale, alpha};
    }
}
