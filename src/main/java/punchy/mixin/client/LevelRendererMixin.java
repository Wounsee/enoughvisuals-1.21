package punchy.mixin.client;

import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_9779;
import net.minecraft.class_9922;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/LevelRendererMixin.class */
@Mixin({class_761.class})
public class LevelRendererMixin {
    @Inject(method = {"method_22710(Lnet/minecraft/class_9922;Lnet/minecraft/class_9779;ZLnet/minecraft/class_4184;Lnet/minecraft/class_757;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}, at = {@At("TAIL")})
    private void punchy$renderReleasedItemParticles(class_9922 graphicsResourceAllocator, class_9779 deltaTracker, boolean renderBlockOutline, class_4184 camera, class_757 gameRenderer, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        class_310 client;
        if (!PunchyConfig.isModEnabled() || (client = class_310.method_1551()) == null || client.field_1687 == null) {
            return;
        }
        Matrix4f particleModelView = new Matrix4f(modelViewMatrix);
        particleModelView.setTranslation(0.0f, 0.0f, 0.0f);
        class_4587 worldBasePose = new class_4587();
        worldBasePose.method_23760().method_23761().set(particleModelView);
        worldBasePose.method_23760().method_23762().set(new Matrix3f(particleModelView));
        class_4597.class_4598 buffer = client.method_22940().method_23000();
        ModelPartsParticleSystem.renderReleasedParticlesInWorld(client, buffer, worldBasePose);
        buffer.method_22993();
    }
}
