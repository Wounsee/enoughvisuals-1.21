package punchy.client.tuning;

import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import punchy.client.config.PunchyConfigScreen;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/tuning/TuningEditorKeybinds.class */
public final class TuningEditorKeybinds {
    private static final String CATEGORY = "key.categories.punchy.tuning";
    private static class_304 openConfigKey;
    private static boolean registered = false;

    private TuningEditorKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        openConfigKey = new class_304("key.punchy.open_main_menu", class_3675.class_307.field_1668, 297, CATEGORY);
        Services.CLIENT.registerKeyMapping(openConfigKey);
        Services.CLIENT.registerClientTickEnd(TuningEditorKeybinds::onClientTick);
        registered = true;
    }

    private static void onClientTick(class_310 client) {
        if (client == null || !registered) {
            return;
        }
        while (openConfigKey.method_1436()) {
            if (client.field_1755 instanceof PunchyConfigScreen) {
                client.method_1507((class_437) null);
            } else {
                client.method_1507(new PunchyConfigScreen(client.field_1755));
            }
        }
    }
}
