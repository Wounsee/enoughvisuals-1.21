package fun.enoughvisuals.mixins.minecraft.client.render.item;

import fun.enoughvisuals.systems.modules.modules.utils.CustomSphereSkin;
import net.minecraft.client.render.item.model.special.HeadModelRenderer;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HeadModelRenderer.class)
public class HeadModelRendererMixin {

    @Inject(method = "getData", at = @At("RETURN"), cancellable = true)
    private void enough$customSphereSkinProfile(ItemStack stack, CallbackInfoReturnable<ProfileComponent> cir) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.PLAYER_HEAD) {
            return;
        }

        ProfileComponent profile = CustomSphereSkin.getActiveProfileForRender();
        if (profile != null) {
            cir.setReturnValue(profile);
        }
    }
}
