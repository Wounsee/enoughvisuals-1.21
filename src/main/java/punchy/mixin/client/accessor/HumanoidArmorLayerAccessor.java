package punchy.mixin.client.accessor;

import net.minecraft.class_10197;
import net.minecraft.class_572;
import net.minecraft.class_970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/HumanoidArmorLayerAccessor.class */
@Mixin({class_970.class})
public interface HumanoidArmorLayerAccessor {
    @Accessor("field_4830")
    class_572<?> punchy$getInnerModel();

    @Accessor("field_4831")
    class_572<?> punchy$getOuterModel();

    @Accessor("field_53218")
    class_572<?> punchy$getInnerModelBaby();

    @Accessor("field_53219")
    class_572<?> punchy$getOuterModelBaby();

    @Accessor("field_54183")
    class_10197 punchy$getEquipmentRenderer();
}
