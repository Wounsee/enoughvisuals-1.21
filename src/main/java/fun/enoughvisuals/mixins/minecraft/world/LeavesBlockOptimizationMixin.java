package fun.enoughvisuals.mixins.minecraft.world;

import fun.enoughvisuals.systems.modules.modules.utils.Optimization;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// In 1.21.4 LeavesBlock no longer overrides isSideInvisible; the method only
// exists on AbstractBlock, so the injection target has to be the superclass.
@Mixin(AbstractBlock.class)
public abstract class LeavesBlockOptimizationMixin {
    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void enough$hideInternalLeafFace(BlockState state, BlockState stateFrom,
                                             Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Optimization.useSimpleLeaves()
                && (Object) this instanceof LeavesBlock
                && stateFrom.getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(true);
        }
    }
}
