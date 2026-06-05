package fun.enoughvisuals.systems.modules.settings;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MultiItemSelectSetting extends Setting<Set<String>> {
    public record ItemOption(String id, String label) {}

    private String searchQuery = "";
    private final int maxVisibleOptions;
    private final List<ItemOption> allOptions;

    public MultiItemSelectSetting(String name, String id, String description, Set<String> defaults) {
        this(name, id, description, defaults, 8);
    }

    public MultiItemSelectSetting(String name, String id, String description, Set<String> defaults, int maxVisibleOptions) {
        super(name, id, description, defaults == null ? new LinkedHashSet<>() : new LinkedHashSet<>(defaults));
        this.maxVisibleOptions = Math.max(3, maxVisibleOptions);
        this.allOptions = buildOptions();
    }

    private static List<ItemOption> buildOptions() {
        List<ItemOption> out = new ArrayList<>();
        for (Identifier key : Registries.ITEM.getIds()) {
            Item item = Registries.ITEM.get(key);
            if (item == null) continue;
            out.add(new ItemOption(key.toString(), item.getName().getString()));
        }
        out.sort(Comparator.comparing(ItemOption::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    @Override
    protected Set<String> validate(Set<String> value) {
        return value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(value);
    }

    public List<ItemOption> getFilteredOptions() {
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return allOptions;
        List<ItemOption> out = new ArrayList<>();
        for (ItemOption option : allOptions) {
            if (option.label().toLowerCase(Locale.ROOT).contains(q) || option.id().toLowerCase(Locale.ROOT).contains(q)) out.add(option);
        }
        return out;
    }

    public ItemStack getItemStack(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public void toggle(String itemId) {
        Set<String> cur = new LinkedHashSet<>(get());
        if (cur.contains(itemId)) cur.remove(itemId); else cur.add(itemId);
        set(cur);
    }

    public boolean isSelected(String itemId) { return get().contains(itemId); }
    public String getSearchQuery() { return searchQuery == null ? "" : searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery == null ? "" : searchQuery; }
    public int getMaxVisibleOptions() { return maxVisibleOptions; }

    @Override
    public Object serialize() { return String.join(",", get()); }

    @Override
    public void deserialize(Object data) {
        if (data instanceof String s) {
            Set<String> set = new LinkedHashSet<>();
            if (!s.isBlank()) {
                for (String p : s.split(",")) if (!p.isBlank()) set.add(p.trim());
            }
            set(set);
        }
    }
}
