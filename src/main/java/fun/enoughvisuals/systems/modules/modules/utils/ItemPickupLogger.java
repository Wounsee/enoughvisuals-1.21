package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Register(name = "Item Pickup Logger", id = "item_pickup_logger", description = "\u041b\u043e\u0433\u0438\u0440\u0443\u0435\u0442 \u0432 \u0447\u0430\u0442 \u043f\u043e\u0434\u043e\u0431\u0440\u0430\u043d\u043d\u044b\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b", category = Category.UTILS)
public class ItemPickupLogger extends Module {

    private static final Set<String> DONATE_KEYWORDS = Set.of(
            "\u043a\u0440\u0443\u0448\u0438\u0442\u0435\u043b\u044c", "\u0441\u0444\u0435\u0440\u0430", "\u0442\u0430\u043b\u0438\u0441\u043c\u0430\u043d",
            "\u0441\u0432\u044f\u0442\u0430\u044f \u0432\u043e\u0434\u0430", "\u0445\u043b\u043e\u043f\u0443\u0448\u043a\u0430", "\u0440\u0430\u0434\u0438\u0430\u0446\u0438\u044f",
            "\u0437\u0435\u043b\u044c\u0435 \u043f\u0430\u043b\u0430\u0434\u0438\u043d\u0430", "\u0437\u0435\u043b\u044c\u0435 \u0433\u043d\u0435\u0432\u0430", "\u0437\u0435\u043b\u044c\u0435 \u0430\u0441\u0441\u0430\u0441\u0438\u043d\u0430",
            "\u0431\u043e\u0436\u044c\u044f \u0430\u0443\u0440\u0430", "\u0434\u0435\u0437\u043e\u0440\u0438\u0435\u043d\u0442\u0430\u0446\u0438\u044f", "\u044f\u0432\u043d\u0430\u044f \u043f\u044b\u043b\u044c",
            "\u043e\u0433\u043d\u0435\u043d\u043d\u044b\u0439 \u0441\u043c\u0435\u0440\u0447", "\u0441\u043d\u0435\u0436\u043e\u043a \u0437\u0430\u043c\u043e\u0440\u043e\u0437\u043a\u0438"
    );

    private final BooleanSetting donateOnly = add(new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u0434\u043e\u043d\u0430\u0442\u043d\u044b\u0435", "donate_only", false));

    private final Map<String, Integer> lastCounts = new HashMap<>();
    private boolean initialized = false;

    @Override
    protected void onEnable() {
        initialized = false;
        lastCounts.clear();
    }

    @Override
    protected void onDisable() {
        initialized = false;
        lastCounts.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            initialized = false;
            lastCounts.clear();
            return;
        }

        Map<String, Integer> current = snapshotInventory();
        if (!initialized) {
            lastCounts.clear();
            lastCounts.putAll(current);
            initialized = true;
            return;
        }

        for (Map.Entry<String, Integer> entry : current.entrySet()) {
            int before = lastCounts.getOrDefault(entry.getKey(), 0);
            int added = entry.getValue() - before;
            if (added <= 0) continue;

            String name = entry.getKey().substring(entry.getKey().indexOf('|') + 1);
            if (donateOnly.get() && !isDonateItem(name)) continue;

            ClientActionUtil.clientMessage(ClientLanguage.text("chat.item_pickup.picked",
                    "Подобран предмет: §d%s §7x%d").formatted(name, added));
        }

        lastCounts.clear();
        lastCounts.putAll(current);
    }

    private Map<String, Integer> snapshotInventory() {
        Map<String, Integer> counts = new HashMap<>();
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack == null || stack.isEmpty()) continue;
            String name = stack.getName().getString();
            String key = Registries.ITEM.getId(stack.getItem()) + "|" + name;
            counts.merge(key, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static boolean isDonateItem(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String kw : DONATE_KEYWORDS) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }
}
