package punchy.mixin.client;

import net.minecraft.class_1113;
import net.minecraft.class_1144;
import net.minecraft.class_1306;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.ShieldImpactPhysics;
import punchy.client.state.MovementStateMachine;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ShieldSoundMixin.class */
@Mixin({class_1144.class})
public class ShieldSoundMixin {
    @Inject(method = {"method_4873(Lnet/minecraft/class_1113;)V"}, at = {@At("HEAD")})
    private void punchy$onPlaySound(class_1113 sound, CallbackInfo ci) {
        class_2960 id;
        class_1306 arm;
        if (sound != null && !PunchyConfig.disableArmPhysics() && !PunchyConfig.disableNativeItemPhysics() && (id = sound.method_4775()) != null && id.method_12832().equals("item.shield.block")) {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 != null) {
                double distSq = mc.field_1724.method_5649(sound.method_4784(), sound.method_4779(), sound.method_4778());
                if (distSq < 4.0d && (arm = ShieldImpactPhysics.resolveImpactArm(mc.field_1724)) != null) {
                    ShieldImpactPhysics.get().trigger(arm);
                    MovementStateMachine.get().triggerShieldBlockImpact(arm);
                }
            }
        }
    }
}
