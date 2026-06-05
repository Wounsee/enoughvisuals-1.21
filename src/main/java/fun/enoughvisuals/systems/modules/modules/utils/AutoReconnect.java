package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;

@Register(name = "AutoReconnect", id = "auto_reconnect", description = "", category = Category.UTILS)
public class AutoReconnect extends Module {
    public static AutoReconnect INSTANCE;

    public AutoReconnect() {
        INSTANCE = this;
    }

    public static boolean enabled() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }
}
