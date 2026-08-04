package fun.wounsee.cosmetic.mixin.render.renderers;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import fun.wounsee.cosmetic.ducks.FiguraItemStackRenderStateExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements FiguraItemStackRenderStateExtension {
    @Shadow
    boolean isLeftHand;
    @Shadow
    ItemDisplayContext displayContext;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    @Unique
    ItemStack figura$itemStack;


    @Override
    public void figura$setItemStack(@Nullable ItemStack itemStack) {
        this.figura$itemStack = itemStack;
    }

    @Override
    public ItemStack figura$getItemStack() {
        return figura$itemStack;
    }

    @Override
    public boolean figura$isLeftHanded() {
        return isLeftHand;
    }

    @Override
    public ItemDisplayContext figura$getDisplayContext() {
        return displayContext;
    }

    @Override
    public BakedModel figura$getModel() {
        for (ItemStackRenderState.LayerRenderState layerRenderState : layers) {
            if (((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).getModel() != null)
                return ((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).getModel();
        }
        return null;
    }
}
