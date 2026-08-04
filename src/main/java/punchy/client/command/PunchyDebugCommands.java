package punchy.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import punchy.Punchy;
import punchy.client.debug.PunchyDebug;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/command/PunchyDebugCommands.class */
public final class PunchyDebugCommands {
    private PunchyDebugCommands() {
    }

    public static void register(CommandDispatcher<class_2168> dispatcher) {
        dispatcher.register(class_2170.method_9247(Punchy.MOD_ID).then(class_2170.method_9247("debugposition").executes(context -> {
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
            ((class_2168) context.getSource()).method_9226(() -> {
                class_5250 class_5250VarMethod_43471;
                Object[] objArr = new Object[1];
                if (enabled) {
                    class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.on");
                } else {
                    class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.off");
                }
                objArr[0] = class_5250VarMethod_43471;
                return class_2561.method_43469("punchy.commands.debug_position", objArr);
            }, false);
            return 1;
        })).then(class_2170.method_9247("pos").then(class_2170.method_9244("x", FloatArgumentType.floatArg()).then(class_2170.method_9244("y", FloatArgumentType.floatArg()).then(class_2170.method_9244("z", FloatArgumentType.floatArg()).executes(context2 -> {
            PunchyDebug.POS_X = FloatArgumentType.getFloat(context2, "x");
            PunchyDebug.POS_Y = FloatArgumentType.getFloat(context2, "y");
            PunchyDebug.POS_Z = FloatArgumentType.getFloat(context2, "z");
            ((class_2168) context2.getSource()).method_9226(() -> {
                return class_2561.method_43469("punchy.commands.pos_set", new Object[]{Float.valueOf(PunchyDebug.POS_X), Float.valueOf(PunchyDebug.POS_Y), Float.valueOf(PunchyDebug.POS_Z)});
            }, false);
            return 1;
        }))))).then(class_2170.method_9247("rot").then(class_2170.method_9244("x", FloatArgumentType.floatArg()).then(class_2170.method_9244("y", FloatArgumentType.floatArg()).then(class_2170.method_9244("z", FloatArgumentType.floatArg()).executes(context3 -> {
            PunchyDebug.ROT_X = FloatArgumentType.getFloat(context3, "x");
            PunchyDebug.ROT_Y = FloatArgumentType.getFloat(context3, "y");
            PunchyDebug.ROT_Z = FloatArgumentType.getFloat(context3, "z");
            ((class_2168) context3.getSource()).method_9226(() -> {
                return class_2561.method_43469("punchy.commands.rot_set", new Object[]{Float.valueOf(PunchyDebug.ROT_X), Float.valueOf(PunchyDebug.ROT_Y), Float.valueOf(PunchyDebug.ROT_Z)});
            }, false);
            return 1;
        }))))).then(class_2170.method_9247("scale").then(class_2170.method_9244("s", FloatArgumentType.floatArg()).executes(context4 -> {
            PunchyDebug.SCALE = FloatArgumentType.getFloat(context4, "s");
            ((class_2168) context4.getSource()).method_9226(() -> {
                return class_2561.method_43469("punchy.commands.scale_set", new Object[]{Float.valueOf(PunchyDebug.SCALE)});
            }, false);
            return 1;
        }))));
    }
}
