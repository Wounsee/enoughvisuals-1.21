package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

@Register(name = "Item Pickup Logger", id = "item_pickup_logger", description = "\u041b\u043e\u0433\u0438\u0440\u0443\u0435\u0442 \u0432 \u0447\u0430\u0442 \u043f\u043e\u0434\u043e\u0431\u0440\u0430\u043d\u043d\u044b\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u044b", category = Category.UTILS)
public class ItemPickupLogger extends Module {

    private final Map<Identifier, Integer> lastCounts = new HashMap<>();
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

        Map<Identifier, Integer> current = snapshotInventory();
        if (!initialized) {
            lastCounts.clear();
            lastCounts.putAll(current);
            initialized = true;
            return;
        }

        for (Map.Entry<Identifier, Integer> entry : current.entrySet()) {
            int before = lastCounts.getOrDefault(entry.getKey(), 0);
            int added = entry.getValue() - before;
            if (added <= 0) continue;

            Item item = Registries.ITEM.get(entry.getKey());
            String name = item == null ? entry.getKey().toString() : new ItemStack(item).getName().getString();
            ClientActionUtil.clientMessage("\u041f\u043e\u0434\u043e\u0431\u0440\u0430\u043d \u043f\u0440\u0435\u0434\u043c\u0435\u0442: \u00A7d" + name + " \u00A77x" + added);
        }

        lastCounts.clear();
        lastCounts.putAll(current);
    }

    private Map<Identifier, Integer> snapshotInventory() {
        Map<Identifier, Integer> counts = new HashMap<>();
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack == null || stack.isEmpty()) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            counts.merge(id, stack.getCount(), Integer::sum);
        }
        return counts;
    }
}
