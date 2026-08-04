package punchy.mixin.client.accessor;

import net.minecraft.class_1299;
import net.minecraft.class_1688;
import net.minecraft.class_1808;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/MinecartItemAccessor.class */
@Mixin({class_1808.class})
public interface MinecartItemAccessor {
    @Accessor("field_8897")
    class_1299<? extends class_1688> punchy$getType();
}
