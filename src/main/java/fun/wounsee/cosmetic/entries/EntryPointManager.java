package fun.wounsee.cosmetic.entries;

import dev.architectury.injectables.annotations.ExpectPlatform;
import fun.wounsee.cosmetic.gui.widgets.PanelSelectorWidget;
import fun.wounsee.cosmetic.lua.FiguraAPIManager;
import fun.wounsee.cosmetic.lua.api.event.EventsAPI;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaModelAPI;
import fun.wounsee.cosmetic.lua.docs.FiguraDocsManager;
import fun.wounsee.cosmetic.permissions.PermissionManager;

import java.util.Set;

public class EntryPointManager {

    public static void init() {
        // APIs
        Set<FiguraAPI> apis = load("figura_api", FiguraAPI.class);
        FiguraAPIManager.initEntryPoints(apis);
        FiguraDocsManager.initEntryPoints(apis);

        // other
        PermissionManager.initEntryPoints(load("figura_permissions", FiguraPermissions.class));
        PanelSelectorWidget.initEntryPoints(load("figura_screen", FiguraScreen.class));
        VanillaModelAPI.initEntryPoints(load("figura_vanilla_part", FiguraVanillaPart.class));
        EventsAPI.initEntryPoints(load("figura_event", FiguraEvent.class));
    }

    public static void reinit() {
        PermissionManager.initEntryPoints(load("figura_permissions", FiguraPermissions.class));
    }

    @ExpectPlatform
    private static <T> Set<T> load(String name, Class<T> clazz) {
        throw new AssertionError();
    }
}
