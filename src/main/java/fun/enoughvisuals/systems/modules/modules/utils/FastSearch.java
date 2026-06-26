package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BindSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.item.ItemStack;

@Register(name = "Fast Search", id = "fast_search", description = "Searches the held item on auction", category = Category.UTILS)
public class FastSearch extends Module {
    private static FastSearch INSTANCE;

    private final BindSetting bind = add(new BindSetting("Key", "bind", -1));

    public FastSearch() {
        INSTANCE = this;
    }

    public static void onKey(int key) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.bind.isKey(key) || !ClientActionUtil.inGame()) return;
        INSTANCE.searchHeldItem();
    }

    private void searchHeldItem() {
        if (mc.player == null || mc.player.networkHandler == null) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack == null || stack.isEmpty()) {
            ClientActionUtil.clientMessage("No item in hand");
            return;
        }

        String name = stack.getName().getString().trim();
        if (name.isBlank()) {
            ClientActionUtil.clientMessage("No item name");
            return;
        }

        mc.player.networkHandler.sendChatCommand("ah search " + name);
    }
}
