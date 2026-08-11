package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.graffiti.GraffitiManager;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import org.lwjgl.glfw.GLFW;

@Register(name = "Graffiti", id = "graffiti", description = "Колесо граффити и размещение на блоках", category = Category.CLIENT)
public final class Graffiti extends Module {
    private static Graffiti instance;
    private final BindSetting wheelBind = add(new BindSetting(
            "Клавиша колеса", "wheel_bind", "Удерживайте для выбора граффити", GLFW.GLFW_KEY_Z));
    private final BindSetting quickBind = add(new BindSetting(
            "Быстрая установка", "quick_bind", "Сразу ставит последнее использованное граффити", -1));
    private final StringSetting lastAsset = (StringSetting) add(new StringSetting(
            "Последнее граффити", "last_asset", "", "", 32, "").visibleWhen(() -> false));

    public Graffiti() {
        instance = this;
        enableSilent();
    }

    public static Graffiti getInstance() {
        return instance;
    }

    public int wheelKey() {
        return wheelBind.get();
    }

    public int quickKey() {
        return quickBind.get();
    }

    public String lastAsset() {
        return lastAsset.get();
    }

    public void rememberAsset(String asset) {
        lastAsset.set(asset);
    }

    @Override
    public void onTick() {
        GraffitiManager.getInstance().tick();
    }

    @Override
    protected void onDisable() {
        GraffitiManager.getInstance().cancelWheel();
    }
}
