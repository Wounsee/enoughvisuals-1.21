package fun.enoughvisuals.mixins.compat;

import org.cef.CefSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        return enough$withBrowserDefaults(arguments);
    }

    @ModifyArg(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lorg/cef/CefApp;getInstance([Ljava/lang/String;Lorg/cef/CefSettings;)Lorg/cef/CefApp;",
                    remap = false),
            index = 0,
            remap = false)
    private static String[] enough$allowDefaultMediaDeviceAtCreation(String[] arguments) {
        return enough$withBrowserDefaults(arguments);
    }

    @ModifyArg(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lorg/cef/CefApp;getInstance([Ljava/lang/String;Lorg/cef/CefSettings;)Lorg/cef/CefApp;",
                    remap = false),
            index = 1,
            remap = false)
    private static CefSettings enough$useSystemBrowserLocale(CefSettings settings) {
        settings.locale = enough$systemLanguageTag();
        return settings;
    }

    private static String[] enough$withBrowserDefaults(String[] arguments) {
        List<String> result = new ArrayList<>();
        if (arguments != null) {
            for (String argument : arguments) {
                if (argument == null || MEDIA_PERMISSION_SWITCH.equals(argument)
                        || argument.startsWith("--lang=")) continue;
                result.add(argument);
            }
        }
        result.add(MEDIA_PERMISSION_SWITCH);
        result.add("--lang=" + enough$systemLanguageTag());
        return result.toArray(String[]::new);
    }

    private static String enough$systemLanguageTag() {
        Locale locale = Locale.getDefault(Locale.Category.DISPLAY);
        String languageTag = locale == null ? "" : locale.toLanguageTag();
        if (languageTag.isBlank() || "und".equalsIgnoreCase(languageTag)) {
            languageTag = Locale.getDefault().toLanguageTag();
        }
        return languageTag.replace('_', '-');
    }
}
