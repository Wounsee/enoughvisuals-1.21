package fun.wounsee.cosmetic.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.gui.FiguraToast;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;
import fun.wounsee.cosmetic.utils.FiguraText;

class ReloadCommand {

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> cmd = LiteralArgumentBuilder.literal("reload");
        cmd.executes(context -> {
            AvatarManager.reloadAvatar(CosmeticMod.getLocalPlayerUUID());
            FiguraToast.sendToast(FiguraText.of("toast.reload"));
            return 1;
        });
        return cmd;
    }
}
