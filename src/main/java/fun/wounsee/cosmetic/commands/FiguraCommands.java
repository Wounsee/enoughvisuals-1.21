package fun.wounsee.cosmetic.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.backend2.BackendCommands;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.lua.FiguraLuaRuntime;
import fun.wounsee.cosmetic.lua.docs.FiguraDocsManager;
import fun.wounsee.cosmetic.model.rendering.AvatarRenderer;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;
import fun.wounsee.cosmetic.utils.FiguraText;

public class FiguraCommands {

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommandRoot() {
        // root
        LiteralArgumentBuilder<FiguraClientCommandSource> root = LiteralArgumentBuilder.literal(CosmeticMod.MOD_ID);

        // docs
        root.then(FiguraDocsManager.getCommand());

        // links
        root.then(LinkCommand.getCommand());

        // run
        root.then(RunCommand.getCommand());

        // load
        root.then(LoadCommand.getCommand());

        // reload
        root.then(ReloadCommand.getCommand());

        // debug
        root.then(DebugCommand.getCommand());

        // export
        root.then(ExportCommand.getCommand());

        if (CosmeticMod.debugModeEnabled()) {
            // backend debug
            root.then(BackendCommands.getCommand());

            // set avatar command
            root.then(AvatarManager.getCommand());
        }

        // emoji list
        root.then(EmojiListCommand.getCommand());

        return root;
    }

    protected static Avatar checkAvatar(CommandContext<FiguraClientCommandSource> context) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(CosmeticMod.getLocalPlayerUUID());
        if (avatar == null) {
            context.getSource().figura$sendError(FiguraText.of("command.no_avatar_error"));
            return null;
        }
        return avatar;
    }

    protected static FiguraLuaRuntime getRuntime(CommandContext<FiguraClientCommandSource> context) {
        Avatar avatar = checkAvatar(context);
        if (avatar == null)
            return null;
        if (avatar.luaRuntime == null || avatar.scriptError) {
            context.getSource().figura$sendError(FiguraText.of("command.no_script_error"));
            return null;
        }
        return avatar.luaRuntime;
    }

    protected static AvatarRenderer getRenderer(CommandContext<FiguraClientCommandSource> context) {
        Avatar avatar = checkAvatar(context);
        if (avatar == null)
            return null;
        if (avatar.renderer == null) {
            context.getSource().figura$sendError(FiguraText.of("command.no_renderer_error"));
            return null;
        }
        return avatar.renderer;
    }
}
