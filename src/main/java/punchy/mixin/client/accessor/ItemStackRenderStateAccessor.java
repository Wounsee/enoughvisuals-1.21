package punchy.mixin.client.accessor;

import net.minecraft.class_10444;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/ItemStackRenderStateAccessor.class */
@Mixin({class_10444.class})
public interface ItemStackRenderStateAccessor {
    @Accessor("field_55339")
    int punchy$getActiveLayerCount();

    @Accessor("field_55340")
    class_10444.class_10446[] punchy$getLayers();
}
