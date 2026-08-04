package fun.wounsee.cosmetic.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.backend2.NetworkStuff;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.gui.ActionWheel;
import fun.wounsee.cosmetic.gui.FiguraToast;
import fun.wounsee.cosmetic.gui.PopupMenu;
import fun.wounsee.cosmetic.gui.screens.WardrobeScreen;
import fun.wounsee.cosmetic.lua.FiguraLuaPrinter;
import fun.wounsee.cosmetic.utils.FiguraText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Final public MouseHandler mouseHandler;
    @Shadow @Final public Options options;
    @Shadow public LocalPlayer player;
    @Shadow public Entity cameraEntity;

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Unique
    private boolean scriptMouseUnlock = false;

    @Inject(at = @At("RETURN"), method = "handleKeybinds")
    private void handleKeybinds(CallbackInfo ci) {
        // don't handle keybinds on panic
        if (AvatarManager.panic)
            return;

        // reload avatar button
        if (Configs.RELOAD_BUTTON.keyBind.consumeClick()) {
            AvatarManager.reloadAvatar(CosmeticMod.getLocalPlayerUUID());
            FiguraToast.sendToast(FiguraText.of("toast.reload"));
        }

        // reload avatar button
        if (Configs.WARDROBE_BUTTON.keyBind.consumeClick())
            this.setScreen(new WardrobeScreen(null));

        // action wheel button
        Boolean wheel = null;
        if (Configs.ACTION_WHEEL_MODE.value % 2 == 1) {
            if (Configs.ACTION_WHEEL_BUTTON.keyBind.consumeClick())
                wheel = !ActionWheel.isEnabled();
        } else if (Configs.ACTION_WHEEL_BUTTON.keyBind.isDown()) {
            wheel = true;
        } else if (ActionWheel.isEnabled()) {
            wheel = false;
        }
        if (wheel != null) {
            if (wheel) {
                ActionWheel.setEnabled(true);
                this.mouseHandler.releaseMouse();
            } else {
                if (Configs.ACTION_WHEEL_MODE.value >= 2)
                    ActionWheel.execute(ActionWheel.getSelected(), true);
                ActionWheel.setEnabled(false);
                this.mouseHandler.grabMouse();
            }
        }

        // popup menu button
        if (Configs.POPUP_BUTTON.keyBind.isDown()) {
            PopupMenu.setEnabled(true);

            if (!PopupMenu.hasEntity()) {
                Entity target = CosmeticMod.extendedPickEntity;
                if (this.player != null && target instanceof Player && !target.isInvisibleTo(this.player)) {
                    PopupMenu.setEntity(target);
                } else if (!this.options.getCameraType().isFirstPerson()) {
                    PopupMenu.setEntity(this.cameraEntity);
                }
            }
        } else if (PopupMenu.isEnabled()) {
            PopupMenu.run();
        }

        // unlock cursor :p
        Avatar avatar = AvatarManager.getAvatarForPlayer(CosmeticMod.getLocalPlayerUUID());
        if (avatar != null && avatar.luaRuntime != null && avatar.luaRuntime.host.unlockCursor) {
            this.mouseHandler.releaseMouse();
            scriptMouseUnlock = true;
        } else if (scriptMouseUnlock) {
            this.mouseHandler.grabMouse();
            scriptMouseUnlock = false;
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;"), method = "handleKeybinds", locals = LocalCapture.CAPTURE_FAILSOFT)
    private void handleHotbarSlots(CallbackInfo ci, int i) {
        if (PopupMenu.isEnabled())
            PopupMenu.hotbarKeyPressed(i);
        if (ActionWheel.isEnabled())
            ActionWheel.hotbarKeyPressed(i);
    }

    @Inject(at = @At("HEAD"), method = "setScreen")
    private void setScreen(Screen screen, CallbackInfo ci) {
        if (ActionWheel.isEnabled())
            ActionWheel.setEnabled(false);

        if (PopupMenu.isEnabled())
            PopupMenu.run();
    }

    @Inject(at = @At("RETURN"), method = "clearClientLevel")
    private void clearLevel(Screen screen, CallbackInfo ci) {
        AvatarManager.clearAllAvatars();
        FiguraLuaPrinter.clearPrintQueue();
    }

    @Inject(at = @At("RETURN"), method = "setLevel")
    private void setLevel(ClientLevel world, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        // Local-only embedded runtime: no remote authentication.
    }

    @Inject(at = @At("HEAD"), method = "runTick")
    private void preTick(boolean tick, CallbackInfo ci) {
        AvatarManager.executeAll("applyBBAnimations", Avatar::applyAnimations);
    }

    @Inject(at = @At("RETURN"), method = "runTick")
    private void afterTick(boolean tick, CallbackInfo ci) {
        AvatarManager.executeAll("clearBBAnimations", Avatar::clearAnimations);
    }

    @Inject(at = @At("RETURN"), method = "tick")
    private void startTick(CallbackInfo ci) {
        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);
        CosmeticMod.tick();
        CosmeticMod.popProfiler();
    }

}
