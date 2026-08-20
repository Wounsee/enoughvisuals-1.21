package fun.enoughvisuals.mixins.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

/** Enables the real default media device while bypassing CEF's missing OSR permission UI. */
@Mixin(targets = "com.cinemamod.mcef.CefUtil", remap = false)
public abstract class McefCommandLineMixin {
    private static final String MEDIA_PERMISSION_SWITCH =
            "--auto-accept-camera-and-microphone-capture";

    @ModifyArg(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lorg/cef/CefApp;startup([Ljava/lang/String;)Z",
                    remap = false),
            index = 0,
            remap = false)
    private static String[] enough$allowDefaultMediaDeviceAtStartup(String[] arguments) {
        return enough$withMediaPermission(arguments);
    }

    @ModifyArg(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lorg/cef/CefApp;getInstance([Ljava/lang/String;Lorg/cef/CefSettings;)Lorg/cef/CefApp;",
                    remap = false),
            index = 0,
            remap = false)
    private static String[] enough$allowDefaultMediaDeviceAtCreation(String[] arguments) {
        return enough$withMediaPermission(arguments);
    }

    private static String[] enough$withMediaPermission(String[] arguments) {
        if (arguments != null && Arrays.asList(arguments).contains(MEDIA_PERMISSION_SWITCH)) {
            return arguments;
        }
        int length = arguments == null ? 0 : arguments.length;
        String[] result = arguments == null
                ? new String[1]
                : Arrays.copyOf(arguments, length + 1);
        result[length] = MEDIA_PERMISSION_SWITCH;
        return result;
    }
}
