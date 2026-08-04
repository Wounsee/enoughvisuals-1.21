package fun.wounsee.cosmetic.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class FiguraGui {

    public static void onRender(GuiGraphics guiGraphics, float tickDelta, CallbackInfo ci) {
        if (AvatarManager.panic)
            return;

        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);

        // render popup menu below everything, as if it were in the world
        CosmeticMod.pushProfiler("popupMenu");
        PopupMenu.render(guiGraphics);
        CosmeticMod.popProfiler();

        // get avatar
        Entity entity = Minecraft.getInstance().getCameraEntity();
        Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);

        if (avatar != null) {
            // hud parent type
            avatar.hudRender(guiGraphics.pose(), Minecraft.getInstance().renderBuffers().bufferSource(), entity, tickDelta);

            // hud hidden by script
            if (avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                // render figura overlays
                renderOverlays(guiGraphics);
                // cancel this method
                ci.cancel();
            }
        }

        CosmeticMod.popProfiler();
    }

    public static void renderOverlays(GuiGraphics guiGraphics) {
        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);

        // render paperdoll
        CosmeticMod.pushProfiler("paperdoll");
        PaperDoll.render(guiGraphics, false);

        // render wheel
        CosmeticMod.popPushProfiler("actionWheel");
        ActionWheel.render(guiGraphics);

        CosmeticMod.popProfiler(2);
    }
}
