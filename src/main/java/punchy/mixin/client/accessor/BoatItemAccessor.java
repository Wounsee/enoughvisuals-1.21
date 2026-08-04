package punchy.mixin.client.accessor;

import net.minecraft.class_10255;
import net.minecraft.class_1299;
import net.minecraft.class_1749;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/BoatItemAccessor.class */
@Mixin({class_1749.class})
public interface BoatItemAccessor {
    @Accessor("field_54469")
    class_1299<? extends class_10255> punchy$getEntityType();
}
