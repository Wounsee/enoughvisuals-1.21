package fun.wounsee.cosmetic.mixin.render.renderers;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackRenderState$LayerRenderStateAccessor {

    @Intrinsic
    @Accessor("model")
    BakedModel getModel();
}
