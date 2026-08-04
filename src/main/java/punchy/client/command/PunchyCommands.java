package punchy.client.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import punchy.Punchy;
import punchy.client.debug.PunchyDebug;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/command/PunchyCommands.class */
public class PunchyCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal(Punchy.MOD_ID).then(ClientCommandManager.literal("debugposition").executes(context -> {
                class_5250 class_5250VarMethod_43471;
                boolean enabled = PunchyDebug.toggleDebugPosition();
                PunchyConfig.Data data = PunchyConfig.snapshot();
                if (enabled) {
                    data.enableTuning = true;
                    data.enableMod = true;
                    PunchyConfig.applyAndSave(data);
                } else if (data.enableTuning) {
                    data.enableTuning = false;
                    PunchyConfig.applyAndSave(data);
                }
                FabricClientCommandSource fabricClientCommandSource = (FabricClientCommandSource) context.getSource();
                Object[] objArr = new Object[1];
                if (enabled) {
                    class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.on");
                } else {
                    class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.off");
                }
                objArr[0] = class_5250VarMethod_43471;
                fabricClientCommandSource.sendFeedback(class_2561.method_43469("punchy.commands.debug_position", objArr));
                return 1;
            })).then(ClientCommandManager.literal("pos").then(ClientCommandManager.argument("x", FloatArgumentType.floatArg()).then(ClientCommandManager.argument("y", FloatArgumentType.floatArg()).then(ClientCommandManager.argument("z", FloatArgumentType.floatArg()).executes(context2 -> {
                PunchyDebug.POS_X = FloatArgumentType.getFloat(context2, "x");
                PunchyDebug.POS_Y = FloatArgumentType.getFloat(context2, "y");
                PunchyDebug.POS_Z = FloatArgumentType.getFloat(context2, "z");
                ((FabricClientCommandSource) context2.getSource()).sendFeedback(class_2561.method_43469("punchy.commands.pos_set", new Object[]{Float.valueOf(PunchyDebug.POS_X), Float.valueOf(PunchyDebug.POS_Y), Float.valueOf(PunchyDebug.POS_Z)}));
                return 1;
            }))))).then(ClientCommandManager.literal("rot").then(ClientCommandManager.argument("x", FloatArgumentType.floatArg()).then(ClientCommandManager.argument("y", FloatArgumentType.floatArg()).then(ClientCommandManager.argument("z", FloatArgumentType.floatArg()).executes(context3 -> {
                PunchyDebug.ROT_X = FloatArgumentType.getFloat(context3, "x");
                PunchyDebug.ROT_Y = FloatArgumentType.getFloat(context3, "y");
                PunchyDebug.ROT_Z = FloatArgumentType.getFloat(context3, "z");
                ((FabricClientCommandSource) context3.getSource()).sendFeedback(class_2561.method_43469("punchy.commands.rot_set", new Object[]{Float.valueOf(PunchyDebug.ROT_X), Float.valueOf(PunchyDebug.ROT_Y), Float.valueOf(PunchyDebug.ROT_Z)}));
                return 1;
            }))))).then(ClientCommandManager.literal("scale").then(ClientCommandManager.argument("s", FloatArgumentType.floatArg()).executes(context4 -> {
                PunchyDebug.SCALE = FloatArgumentType.getFloat(context4, "s");
                ((FabricClientCommandSource) context4.getSource()).sendFeedback(class_2561.method_43469("punchy.commands.scale_set", new Object[]{Float.valueOf(PunchyDebug.SCALE)}));
                return 1;
            }))));
        });
    }
}
