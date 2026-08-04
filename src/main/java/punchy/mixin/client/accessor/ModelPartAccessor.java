package punchy.mixin.client.accessor;

import java.util.List;
import java.util.Map;
import net.minecraft.class_630;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/ModelPartAccessor.class */
@Mixin({class_630.class})
public interface ModelPartAccessor {
    @Accessor("field_3663")
    List<class_630.class_628> punchy$getCubes();

    @Accessor("field_3661")
    Map<String, class_630> punchy$getChildren();
}
