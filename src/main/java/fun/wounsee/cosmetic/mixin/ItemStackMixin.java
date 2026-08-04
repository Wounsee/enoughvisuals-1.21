package fun.wounsee.cosmetic.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ItemStack.class, priority = 999)
public class ItemStackMixin {

    @ModifyReturnValue(method = "getHoverName", at = @At("RETURN"))
    private Component getHoverName(Component original) {
        if (Configs.EMOJIS.value > 0)
            return Emojis.applyEmojis(original);
        return original;
    }
}
