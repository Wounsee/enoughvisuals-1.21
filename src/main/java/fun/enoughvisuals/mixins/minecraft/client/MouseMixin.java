package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.gps.GpsSystem;

import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.ClickPearl;
import fun.enoughvisuals.systems.modules.modules.utils.FastSearch;
import fun.enoughvisuals.systems.modules.modules.utils.FastSwap;
import fun.enoughvisuals.systems.modules.modules.utils.ItemSwap;
import fun.enoughvisuals.systems.modules.modules.utils.Macros;
import fun.enoughvisuals.systems.modules.modules.utils.SendCord;
import fun.enoughvisuals.systems.modules.modules.utils.SpecBind;
import fun.enoughvisuals.systems.modules.modules.visual.Zoom;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void enough$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen != null) return;

        int key = BindKeyUtil.mouseButtonToCode(button);

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
        Zoom zoom = Zoom.getInstance();

        if (zoom != null && zoom.isZoomKeyHeld()) {
            zoom.onMouseScroll(vertical);
            ci.cancel();
        }
    }
}
