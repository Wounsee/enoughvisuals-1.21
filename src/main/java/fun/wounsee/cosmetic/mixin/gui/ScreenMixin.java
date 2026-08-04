package fun.wounsee.cosmetic.mixin.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.utils.TextUtils;
import org.luaj.vm2.LuaValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(at = @At("HEAD"), method = "handleComponentClicked", cancellable = true)
    private void handleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (style == null)
            return;

        ClickEvent event = style.getClickEvent();
        if (event == null)
            return;

        if (event instanceof TextUtils.FiguraClickEvent figuraEvent) {
            figuraEvent.onClick.run();
            cir.setReturnValue(true);
        } else if (event.getAction() == ClickEvent.Action.valueOf("FIGURA_FUNCTION")) {
            cir.setReturnValue(true);

            Avatar avatar = AvatarManager.getAvatarForPlayer(CosmeticMod.getLocalPlayerUUID());
            if (avatar == null)
                return;
            LuaValue value = null;
            try {
                value = avatar.loadScript("figura_function", event.getValue());
            } catch (Exception e) {
                CosmeticMod.sendChatMessage(Component.literal(e.getMessage()).withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)));
            }
            if (value != null)
                avatar.run(value, avatar.tick);
        }
    }
}
