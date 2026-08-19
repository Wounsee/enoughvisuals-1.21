package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.AtmoDawnFog;
import fun.enoughvisuals.systems.modules.modules.visual.AttackEffect;
import fun.enoughvisuals.systems.modules.modules.visual.MotionBlur;
import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import fun.enoughvisuals.systems.modules.modules.visual.SkyBox;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void enough$captureStableWorldMatrices(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                                    boolean renderBlockOutline, Camera camera,
                                                    GameRenderer gameRenderer, Matrix4f positionMatrix,
                                                    Matrix4f projectionMatrix, CallbackInfo ci) {
        AtmoDawnFog.captureWorldMatrices(positionMatrix, projectionMatrix);
        MotionBlur.captureWorldMatrices(positionMatrix, projectionMatrix);
        AttackEffect.captureWorldMatrices(positionMatrix, projectionMatrix);
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void enough$customWorldClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                          CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color,
                                          float cloudHeight, CallbackInfo ci) {
        if (SkyBox.active() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.weather.get()) {
            ci.cancel();
        }
    }
}
