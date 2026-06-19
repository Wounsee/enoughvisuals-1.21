package fun.enoughvisuals.mixins.minecraft.client.gui.screen;

import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenHideMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void enough$hideRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ClientActionUtil.isSilentInventory()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void enough$hideBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ClientActionUtil.isSilentInventory()) {
            ci.cancel();
        }
    }
}
