package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;

@Register(name = "Chat Helper", id = "chat_helper", description = "Stacks repeated chat messages", category = Category.UTILS)
public class ChatHelper extends Module {
    public static ChatHelper INSTANCE;

    public ChatHelper() {
        INSTANCE = this;
    }
}
