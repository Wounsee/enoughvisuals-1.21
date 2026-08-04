package punchy.mixin.client.accessor;

import java.util.List;
import net.minecraft.class_922;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/LivingEntityRendererAccessor.class */
@Mixin({class_922.class})
public interface LivingEntityRendererAccessor {
    @Accessor("field_4738")
    List<Object> punchy$getLayers();
}
