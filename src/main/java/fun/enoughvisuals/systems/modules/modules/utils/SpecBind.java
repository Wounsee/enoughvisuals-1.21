package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;

@Register(name = "Spec Bind", id = "spec_bind", description = "Отправляет !Спек по бинду", category = Category.UTILS)
public class SpecBind extends Module {
    private static SpecBind INSTANCE;

    private final BindSetting bind = add(new BindSetting("Клавиша", "bind", -1));

    public SpecBind() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.bind.isKey(key) || !ClientActionUtil.inGame()) return;
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatMessage("!Спек");
        }
    }
}
