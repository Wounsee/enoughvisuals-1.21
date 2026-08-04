package punchy.mixin.client.accessor;

import net.minecraft.class_10485;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/UseDurationAccessor.class */
@Mixin({class_10485.class})
public interface UseDurationAccessor {
    @Accessor("comp_3414")
    boolean punchy$isRemaining();
}
