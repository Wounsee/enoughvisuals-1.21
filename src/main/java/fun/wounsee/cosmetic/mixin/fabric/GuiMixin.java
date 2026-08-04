package fun.wounsee.cosmetic.mixin.fabric;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.gui.ActionWheel;
import fun.wounsee.cosmetic.gui.FiguraGui;
import fun.wounsee.cosmetic.lua.api.RendererAPI;
import fun.wounsee.cosmetic.math.vector.FiguraVec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Shadow @Final private Minecraft minecraft;
    @Unique private FiguraVec2 crosshairOffset;

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        FiguraGui.onRender(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false), ci);
    }

    @Inject(at = @At("RETURN"), method = "render")
    private void afterRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!AvatarManager.panic)
            FiguraGui.renderOverlays(guiGraphics);
    }

    @Inject(at = @At("HEAD"), method = "renderCrosshair", cancellable = true)
    private void renderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        crosshairOffset = null;

        if (ActionWheel.isEnabled()) {
            ci.cancel();
            return;
        }

        Entity entity = this.minecraft.getCameraEntity();
        Avatar avatar;
        if (entity == null || (avatar = AvatarManager.getAvatar(entity)) == null || avatar.luaRuntime == null)
            return;

        RendererAPI renderer = avatar.luaRuntime.renderer;
        if (!renderer.renderCrosshair) {
            ci.cancel();
            return;
        }

        crosshairOffset = renderer.crosshairOffset;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"), method = "renderCrosshair")
    private void blitRenderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (crosshairOffset != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(crosshairOffset.x, crosshairOffset.y, 0d);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V", shift = At.Shift.AFTER), method = "renderCrosshair")
    private void afterBlitRenderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (crosshairOffset != null)
            guiGraphics.pose().popPose();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"), method = "renderCrosshair")
    private void blitRenderCrosshairSliced(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (crosshairOffset != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(crosshairOffset.x, crosshairOffset.y, 0d);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V", shift = At.Shift.AFTER), method = "renderCrosshair")
    private void afterBlitRenderCrosshairSliced(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (crosshairOffset != null)
            guiGraphics.pose().popPose();
    }
}
