package fun.enoughvisuals.systems.modules.modules.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.ClickSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import fun.enoughvisuals.ui.screen.MacrosScreen;
import fun.enoughvisuals.utils.player.ClientActionUtil;

import java.util.ArrayList;
import java.util.List;

@Register(name = "Macros", id = "macros", description = "Binds commands and messages to keys", category = Category.UTILS)
public class Macros extends Module {
    public static Macros INSTANCE;

    private static final Gson GSON = new Gson();

    private final StringSetting storage = (StringSetting) add(new StringSetting(
            "Storage", "storage", "", "[]", 8192, ""
    )).visibleWhen(() -> false);

    public final ClickSetting openMenu = add(new ClickSetting(
            "Open Menu", "open_menu", "Opens macro editor", this::openMenu
    ));
    public final BindSetting openMenuBind = add(new BindSetting(
            "Open Menu Bind", "open_menu_bind", "Opens the macro editor from a bind", -1
    ));

    private final List<MacroEntry> macros = new ArrayList<>();

    public Macros() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !ClientActionUtil.inGame()) return;
        if (INSTANCE.openMenuBind.isKey(key)) {
            INSTANCE.openMenu();
            return;
        }
        INSTANCE.runMacro(key);
    }

    public List<MacroEntry> getMacros() {
        syncFromStorage();
        return List.copyOf(macros);
    }

    public void addMacro(int key, String action) {
        if (key < 0 || action == null || action.isBlank()) return;
        syncFromStorage();
        macros.add(new MacroEntry(key, action.trim()));
        saveToStorage();
    }

    public void removeMacro(int index) {
        syncFromStorage();
        if (index < 0 || index >= macros.size()) return;
        macros.remove(index);
        saveToStorage();
    }

    private void runMacro(int key) {
        syncFromStorage();
        for (MacroEntry macro : macros) {
            if (macro.key() == key) {
                String action = macro.action().trim();
                if (action.startsWith("/")) {
                    ClientActionUtil.sendCommand(action);
                } else if (mc.player != null && mc.player.networkHandler != null && !action.isBlank()) {
                    mc.player.networkHandler.sendChatMessage(action);
                }
            }
        }
    }

    private void openMenu() {
        if (mc != null) {
            mc.setScreen(new MacrosScreen(this));
        }
    }

    private void syncFromStorage() {
        macros.clear();
        try {
            JsonElement root = JsonParser.parseString(storage.get());
            if (!root.isJsonArray()) return;
            JsonArray array = root.getAsJsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                int key = object.has("key") ? object.get("key").getAsInt() : -1;
                String action = object.has("action") ? object.get("action").getAsString() : "";
                if (key >= 0 && !action.isBlank()) {
                    macros.add(new MacroEntry(key, action));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void saveToStorage() {
        JsonArray array = new JsonArray();
        for (MacroEntry macro : macros) {
            JsonObject object = new JsonObject();
            object.addProperty("key", macro.key());
            object.addProperty("action", macro.action());
            array.add(object);
        }
        storage.set(GSON.toJson(array));
    }

    public record MacroEntry(int key, String action) {
    }
}
