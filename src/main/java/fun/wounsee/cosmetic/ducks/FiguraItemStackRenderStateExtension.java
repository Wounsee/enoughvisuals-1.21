package fun.wounsee.cosmetic.ducks;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface FiguraItemStackRenderStateExtension {
    void figura$setItemStack(@Nullable ItemStack itemStack);
    ItemStack figura$getItemStack();
    boolean figura$isLeftHanded();
    ItemDisplayContext figura$getDisplayContext();
    BakedModel figura$getModel();
}
