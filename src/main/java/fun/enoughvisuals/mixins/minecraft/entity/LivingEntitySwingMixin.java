package fun.enoughvisuals.mixins.minecraft.entity;

import fun.enoughvisuals.systems.modules.modules.visual.SwingAnimation;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void enough$replaceSwingSpeed(CallbackInfoReturnable<Integer> cir) {
        SwingAnimation module = SwingAnimation.INSTANCE;
        if (module == null || !module.isEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ClientPlayerEntity)) return;

        ItemStack stack = self.getMainHandStack();
        if (!module.shouldApplyAnimation(stack)) return;

        int original = cir.getReturnValue();
        int modified = Math.max(1, (int) (original / module.getSpeedMultiplier()));
        cir.setReturnValue(modified);
    }
}
