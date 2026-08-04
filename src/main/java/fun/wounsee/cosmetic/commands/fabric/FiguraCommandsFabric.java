package fun.wounsee.cosmetic.commands.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import fun.wounsee.cosmetic.commands.FiguraCommands;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;

public class FiguraCommandsFabric {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void init() {
        // register
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            CommandDispatcher<FiguraClientCommandSource> casted = (CommandDispatcher) dispatcher;
            casted.register(FiguraCommands.getCommandRoot());
        });
    }
}
