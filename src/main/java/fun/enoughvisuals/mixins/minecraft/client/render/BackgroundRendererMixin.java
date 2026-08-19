package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.systems.modules.modules.visual.WorldTweaks;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private static void enough$customFogColor(Camera camera,
                                              float tickDelta,
                                              ClientWorld world,
                                              int viewDistance,
                                              float skyDarkness,
                                              CallbackInfoReturnable<Vector4f> cir) {
        WorldTweaks customWorld = WorldTweaks.active();
        if (customWorld != null && customWorld.shouldApplyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN)) {
            cir.setReturnValue(customWorld.fogColorVector());
        }
    }

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void enough$customFog(Camera camera,
                                         BackgroundRenderer.FogType fogType,
                                         Vector4f color,
                                         float viewDistance,
                                         boolean thickenFog,
                                         float tickDelta,
                                         CallbackInfoReturnable<Fog> cir) {
        WorldTweaks customWorld = WorldTweaks.active();
        if (customWorld != null && customWorld.shouldApplyFog(camera, fogType)) {
            cir.setReturnValue(customWorld.applyFog(cir.getReturnValue()));
        }
    }
}
