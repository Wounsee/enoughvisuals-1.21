package punchy.mixin.client.accessor;

import net.minecraft.class_1536;
import net.minecraft.class_1657;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/accessor/PlayerFishingAccessor.class */
@Mixin({class_1657.class})
public interface PlayerFishingAccessor {
    @Accessor("field_7513")
    class_1536 punchy$getFishing();
}
