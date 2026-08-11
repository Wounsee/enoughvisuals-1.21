package fun.enoughvisuals.mixins.minecraft.world;

import fun.enoughvisuals.systems.modules.modules.utils.Optimization;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockOptimizationMixin {
    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void enough$hideInternalLeafFace(BlockState state, BlockState stateFrom,
                                             Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Optimization.useSimpleLeaves() && stateFrom.getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(true);
        }
    }
}
