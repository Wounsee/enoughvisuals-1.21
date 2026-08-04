package punchy.mixin.client;

import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/MultiPlayerGameModeAttackMixin.class */
@Mixin({class_636.class})
public class MultiPlayerGameModeAttackMixin {
    @Inject(method = {"method_2918(Lnet/minecraft/class_1657;Lnet/minecraft/class_1297;)V"}, at = {@At("HEAD")})
    private void punchy$onAttack(class_1657 player, class_1297 target, CallbackInfo ci) {
    }
}
