package fun.wounsee.cosmetic.mixin.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected EditBox input;

    @ModifyVariable(at = @At("HEAD"), method = "handleChatInput", argsOnly = true)
    private String handleChatInput(String text) {
        String s = text;
        Avatar avatar = AvatarManager.getAvatarForPlayer(CosmeticMod.getLocalPlayerUUID());
        if (avatar != null && !text.isBlank())
            s = avatar.chatSendMessageEvent(text);

        if (!text.equals(s))
            CosmeticMod.LOGGER.info("Changed chat message from \"{}\" to \"{}\"", text, s);

        return s;
    }

    @Inject(at = @At("HEAD"), method = "render")
    private void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(CosmeticMod.getLocalPlayerUUID());
        if (avatar == null || avatar.luaRuntime == null)
            return;

        Integer color = avatar.luaRuntime.host.chatColor;
        if (color == null)
            return;

        this.input.setTextColor(color);
    }
}
