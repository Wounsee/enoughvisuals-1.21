package punchy.mixin.client.accessor;

import java.util.function.Supplier;
import net.minecraft.class_10444;
import net.minecraft.class_1058;
import net.minecraft.class_1921;
import net.minecraft.class_804;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/ItemStackRenderStateLayerAccessor.class */
@Mixin({class_10444.class_10446.class})
public interface ItemStackRenderStateLayerAccessor {
    @Accessor("field_55347")
    class_1921 punchy$getRenderType();

    @Accessor("field_55348")
    class_10444.class_10445 punchy$getFoilType();

    @Accessor("field_55349")
    int[] punchy$getTintLayers();

    @Accessor("field_56967")
    class_804 punchy$getTransform();

    @Accessor("field_56967")
    void punchy$setTransform(class_804 class_804Var);

    @Accessor("field_56965")
    boolean punchy$usesBlockLight();

    @Accessor("field_56966")
    class_1058 punchy$getParticleIcon();

    @Accessor("field_56968")
    Supplier<Vector3f[]> punchy$getExtents();
}
