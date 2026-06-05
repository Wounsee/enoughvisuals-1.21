package fun.enoughvisuals.systems.modules.settings;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ItemSelectSetting extends Setting<String> {
    public record ItemOption(String id, String label) {}

    private String searchQuery = "";
    private final int maxVisibleOptions;
    private final List<ItemOption> allOptions;

    public ItemSelectSetting(String name, String id, String description, String defaultItemId) {
        this(name, id, description, defaultItemId, 8);
    }

    public ItemSelectSetting(String name, String id, String description, String defaultItemId, int maxVisibleOptions) {
        super(name, id, description, defaultItemId);
        this.maxVisibleOptions = Math.max(3, maxVisibleOptions);
        this.allOptions = buildOptions();
    }

    private static List<ItemOption> buildOptions() {
        List<ItemOption> out = new ArrayList<>();
        for (Identifier key : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(key);
            if (item == null) continue;
            String id = key.toString();
            String label = item.getName().getString();
            out.add(new ItemOption(id, label));
        }
        out.sort(Comparator.comparing(ItemOption::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public List<ItemOption> getFilteredOptions() {
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return allOptions;
        List<ItemOption> out = new ArrayList<>();
        for (ItemOption option : allOptions) {
            if (option.label().toLowerCase(Locale.ROOT).contains(q) || option.id().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(option);
            }
        }
        return out;
    }

    public ItemStack getItemStack(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public String getSelectedLabel() {
        String selected = get();
        if (selected == null || selected.isBlank()) return "Не выбрано";
        for (ItemOption option : allOptions) {
            if (option.id().equals(selected)) return option.label();
        }
        return selected;
    }

    public String getSearchQuery() { return searchQuery == null ? "" : searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery == null ? "" : searchQuery; }
    public int getMaxVisibleOptions() { return maxVisibleOptions; }

    @Override
    public Object serialize() { return get(); }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) set(s);
    }
}
