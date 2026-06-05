package fun.enoughvisuals.mixins.minecraft.client;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.FastSwap;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyInputMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void enough$onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (key == GLFW.GLFW_KEY_UNKNOWN || key < 0) return;

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (client.currentScreen == null) {
                client.setScreen(new MenuScreen());
            }
            return;
        }

        if (client.currentScreen == null) {
            if (GpsSystem.getInstance().openGuiIfBound(key, client)) {
                return;
            }
            FastSwap.onKey(key);
            ModuleManager.getInstance().onKeyPress(key);
        }
    }
}
