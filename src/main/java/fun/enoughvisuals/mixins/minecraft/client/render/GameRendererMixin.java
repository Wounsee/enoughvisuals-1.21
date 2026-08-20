package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.Optimization;
import fun.enoughvisuals.systems.modules.modules.visual.AspectRatio;
import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import fun.enoughvisuals.systems.modules.modules.visual.Zoom;
import fun.enoughvisuals.systems.webpanel.MinecraftVirtualCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

                                                   
    @Shadow
    public abstract float getFarPlaneDistance();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void enough$skipOptimizedFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!Optimization.shouldRenderFrame()) ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.AFTER))
    private void enough$captureMinecraftWorld(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MinecraftVirtualCamera.captureFrame(false);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void enough$captureMinecraftGui(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MinecraftVirtualCamera.captureFrame(true);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.hurtCam.get()) {
            ci.cancel();
        }
    }
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float result = cir.getReturnValue();
        Zoom zoom = Zoom.getInstance();

        if (zoom != null) {
            float mult = (float) zoom.getFovMultiplier(tickDelta);
            result *= mult;
        }

        cir.setReturnValue(result);
    }
    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void onGetBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatio module = ModuleManager.getInstance().get(AspectRatio.class);

        if (module != null && module.isEnabled()) {
            Matrix4f matrix4f = new Matrix4f();

            float customRatio = module.getRatio();

                                                                                   
            float fovRadians = fovDegrees * ((float)Math.PI / 180F);

                                           
            matrix4f.perspective(fovRadians, customRatio, 0.05F, this.getFarPlaneDistance());

            cir.setReturnValue(matrix4f);
        }
    }

}
