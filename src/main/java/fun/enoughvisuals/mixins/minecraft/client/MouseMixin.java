package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.graffiti.GraffitiManager;

import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.ClickPearl;
import fun.enoughvisuals.systems.modules.modules.utils.FastSearch;
import fun.enoughvisuals.systems.modules.modules.utils.FastSwap;
import fun.enoughvisuals.systems.modules.modules.utils.ItemSwap;
import fun.enoughvisuals.systems.modules.modules.utils.Macros;
import fun.enoughvisuals.systems.modules.modules.utils.SendCord;
import fun.enoughvisuals.systems.modules.modules.utils.SpecBind;
import fun.enoughvisuals.systems.modules.modules.visual.FreeLook;
import fun.enoughvisuals.systems.modules.modules.visual.Zoom;
import fun.enoughvisuals.systems.modules.modules.visual.WebPanels;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Redirect(method = "updateMouse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void enough$redirectFreeLook(ClientPlayerEntity player, double yawDelta, double pitchDelta) {
        WebPanels webPanels = WebPanels.getInstance();
        if (webPanels != null && webPanels.rotateWithMouse(yawDelta, pitchDelta)) return;
        if (webPanels != null && webPanels.isAddressEditing()) return;
        if (!FreeLook.handleLookDelta(yawDelta, pitchDelta)) {
            player.changeLookDirection(yawDelta, pitchDelta);
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void enough$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        WebPanels webPanels = WebPanels.getInstance();
        if (webPanels != null && (webPanels.isAddressEditing() || webPanels.isHovering())
                && client.currentScreen == null) {
            ci.cancel();
            return;
        }

        if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                && GraffitiManager.getInstance().deleteLookedAtOwn()) {
            ci.cancel();
            return;
        }

        int key = BindKeyUtil.mouseButtonToCode(button);
        if (GraffitiManager.getInstance().onKey(key, action)) {
            ci.cancel();
            return;
        }
        if (action != GLFW.GLFW_PRESS || client.currentScreen != null) return;

        if (GpsSystem.getInstance().openGuiIfBound(key, client)) {
            return;
        }
        FastSearch.onKey(key);
        Macros.onKey(key);
        FastSwap.onKey(key);
        ClickPearl.onKey(key);
        ItemSwap.onKey(key);
        SpecBind.onKey(key);
        SendCord.onKey(key);
        ModuleManager.getInstance().onKeyPress(key);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        WebPanels webPanels = WebPanels.getInstance();
        if (webPanels != null && webPanels.isAddressEditing()) {
            ci.cancel();
            return;
        }
        boolean controlDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (webPanels != null && webPanels.scroll(vertical, controlDown)) {
            ci.cancel();
            return;
        }

        Zoom zoom = Zoom.getInstance();

        if (zoom != null && zoom.isZoomKeyHeld()) {
            zoom.onMouseScroll(vertical);
            ci.cancel();
        }
    }
}
