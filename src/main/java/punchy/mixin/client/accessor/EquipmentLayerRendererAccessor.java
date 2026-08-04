package punchy.mixin.client.accessor;

import net.minecraft.class_10197;
import net.minecraft.class_10201;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/EquipmentLayerRendererAccessor.class */
@Mixin({class_10197.class})
public interface EquipmentLayerRendererAccessor {
    @Accessor("field_54179")
    class_10201 punchy$getEquipmentAssets();
}
