package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.mixins.accessors.GameOptionsAccessor;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.ClickPearl;
import fun.enoughvisuals.systems.modules.modules.utils.FastSearch;
import fun.enoughvisuals.systems.modules.modules.utils.FastSwap;
import fun.enoughvisuals.systems.modules.modules.utils.ItemSwap;
import fun.enoughvisuals.systems.modules.modules.utils.Macros;
import fun.enoughvisuals.systems.modules.modules.utils.SendCord;
import fun.enoughvisuals.systems.modules.modules.utils.SpecBind;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import fun.enoughvisuals.systems.graffiti.GraffitiManager;
import fun.enoughvisuals.systems.modules.modules.visual.WebPanels;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Keyboard.class)
public abstract class KeyInputMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void enough$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        WebPanels webPanels = WebPanels.getInstance();
        if (webPanels != null && webPanels.isBrowserFocused()) {
            if (webPanels.forwardKeySafe(key, scancode, modifiers, action)) {
                ci.cancel();
                return;
            }
        }
        if (key != GLFW.GLFW_KEY_UNKNOWN && key >= 0 && EmotionWheelManager.getInstance().onKey(key, action)) {
            ci.cancel();
            return;
        }
        if (key != GLFW.GLFW_KEY_UNKNOWN && key >= 0 && GraffitiManager.getInstance().onKey(key, action)) {
            ci.cancel();
            return;
        }
        if (action != GLFW.GLFW_PRESS) return;
        if (key == GLFW.GLFW_KEY_UNKNOWN || key < 0) return;

        if (key == GLFW.GLFW_KEY_F11) {
            enough$forceCurrentFullscreenMode();
        }

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (client.currentScreen == null) {
                EnoughSoundPlayer.playMenuOpen();
                client.setScreen(new MenuScreen());
            }
            return;
        }

        if (client.currentScreen == null) {
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
    }

    private void enough$forceCurrentFullscreenMode() {
        try {
            ((GameOptionsAccessor) client.options).enoughvisuals$setFullscreenResolution("");
            client.getWindow().setFullscreenVideoMode(Optional.empty());
            client.options.write();
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void enough$onChar(long window, int codepoint, int modifiers, CallbackInfo ci) {
        WebPanels webPanels = WebPanels.getInstance();
        if (webPanels != null && webPanels.isBrowserFocused()) {
            webPanels.forwardChar(codepoint, modifiers);
            ci.cancel();
        }
    }
}
