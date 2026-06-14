package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugHud.class)
public class DebugHudMixin {

    @Shadow
    private boolean showDebugHud;

    @Unique
    private boolean enough$leftTransform;

    @Unique
    private boolean enough$rightTransform;

    @Inject(method = "toggleDebugHud", at = @At("RETURN"))
    private void enough$debugHudToggled(CallbackInfo ci) {
        Animations.markDebugHudToggled(showDebugHud);
    }

    @Inject(method = "shouldShowDebugHud", at = @At("HEAD"), cancellable = true)
    private void enough$keepDebugHudDuringClose(CallbackInfoReturnable<Boolean> cir) {
        Animations animations = Animations.active();
        if (animations != null && animations.debugHudAnimation.get() && animations.shouldRenderDebugHud()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawLeftText", at = @At("HEAD"))
    private void enough$pushLeftDebugAnimation(DrawContext context, CallbackInfo ci) {
        enough$leftTransform = false;
        Animations animations = Animations.active();
        if (animations == null || !animations.debugHudAnimation.get()) return;

        float progress = RenderUtil.easeOutCubic(animations.debugHudProgress());
        if (progress >= 0.999f) return;

        context.getMatrices().push();
        context.getMatrices().translate((1f - progress) * -(context.getScaledWindowWidth() * 0.45f + 32f), 0f, 0f);
        enough$leftTransform = true;
    }

    @Inject(method = "drawLeftText", at = @At("RETURN"))
    private void enough$popLeftDebugAnimation(DrawContext context, CallbackInfo ci) {
        if (!enough$leftTransform) return;
        context.getMatrices().pop();
        enough$leftTransform = false;
    }

    @Inject(method = "drawRightText", at = @At("HEAD"))
    private void enough$pushRightDebugAnimation(DrawContext context, CallbackInfo ci) {
        enough$rightTransform = false;
        Animations animations = Animations.active();
        if (animations == null || !animations.debugHudAnimation.get()) return;

        float progress = RenderUtil.easeOutCubic(animations.debugHudProgress());
        if (progress >= 0.999f) return;

        context.getMatrices().push();
        context.getMatrices().translate((1f - progress) * (context.getScaledWindowWidth() * 0.45f + 32f), 0f, 0f);
        enough$rightTransform = true;
    }

    @Inject(method = "drawRightText", at = @At("RETURN"))
    private void enough$popRightDebugAnimation(DrawContext context, CallbackInfo ci) {
        if (!enough$rightTransform) return;
        context.getMatrices().pop();
        enough$rightTransform = false;
    }
}
