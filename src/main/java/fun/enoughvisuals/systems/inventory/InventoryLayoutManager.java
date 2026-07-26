package fun.enoughvisuals.systems.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores exact player-inventory layouts and restores them through ordinary
 * vanilla slot clicks. No local inventory mutation or custom packet is used.
 */
public final class InventoryLayoutManager {
    private static final InventoryLayoutManager INSTANCE = new InventoryLayoutManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("enoughvisuals")
            .resolve("inventories.json");
    private static final long CLICK_DELAY_MS = 110L;
    private static final int SCRATCH_HOTBAR_BUTTON = 0;
    private static final int SCRATCH_SLOT = PlayerScreenHandler.HOTBAR_START;

    private final List<StoredLayout> layouts = new ArrayList<>();
    private final ArrayDeque<ClickAction> actions = new ArrayDeque<>();
    private boolean initialized;
    private int loadingSyncId = -1;
    private int plannedActions;
    private int completedActions;
    private long nextClickAt;
    private String loadingName = "";
    private String status = "";
    private boolean statusGood;
    private long statusAt;

    private InventoryLayoutManager() {
    }

    public static InventoryLayoutManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        loadFile();
    }

    public synchronized List<LayoutView> list() {
        initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        List<LayoutView> result = new ArrayList<>();
        for (StoredLayout layout : layouts) {
            List<ItemStack> stacks = decode(layout, client);
            result.add(new LayoutView(layout.id, layout.name, layout.createdAt,
                    stacks.stream().mapToInt(stack -> stack.isEmpty() ? 0 : stack.getCount()).sum(), stacks));
        }
        result.sort(Comparator.comparingLong(LayoutView::createdAt).reversed());
        return result;
    }

    public synchronized LayoutView find(String id) {
        if (id == null || id.isBlank()) return null;
        for (LayoutView view : list()) if (view.id().equals(id)) return view;
        return null;
    }

    public synchronized Result saveCurrent(String rawName) {
        initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        String name = cleanName(rawName);
        if (name.isEmpty()) return fail("Введите название инвентаря");
        if (!hasPlayerInventory(player)) return fail("Инвентарь игрока сейчас недоступен");

        List<String> encoded = new ArrayList<>(slotIds().size());
        try {
            for (int slotId : slotIds()) {
                ItemStack stack = player.playerScreenHandler.getSlot(slotId).getStack();
                encoded.add(stack.isEmpty() ? "" : stack.toNbt(player.getRegistryManager()).toString());
            }
        } catch (Exception exception) {
            return fail("Не удалось прочитать предметы");
        }

        StoredLayout stored = null;
        for (StoredLayout candidate : layouts) {
            if (candidate.name.equalsIgnoreCase(name)) {
                stored = candidate;
                break;
            }
        }
        long now = System.currentTimeMillis();
        if (stored == null) {
            stored = new StoredLayout();
            stored.id = UUID.randomUUID().toString();
            stored.createdAt = now;
            layouts.add(stored);
        }
        stored.name = name;
        stored.updatedAt = now;
        stored.stacks = encoded;
        stored.cachedStacks = currentStacks(player);
        if (!saveFile()) return fail("Не удалось записать файл сохранений");
        setStatus("Инвентарь " + name + " сохранён", true);
        return new Result(true, status, stored.id);
    }

    public synchronized Result delete(String id) {
        initialize();
        boolean removed = layouts.removeIf(layout -> layout.id.equals(id));
        if (!removed) return fail("Сохранение не найдено");
        if (!saveFile()) return fail("Не удалось записать файл сохранений");
        setStatus("Сохранение удалено", true);
        return new Result(true, status, "");
    }

    public synchronized Result startLoad(String id) {
        initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!actions.isEmpty()) return fail("Уже загружается другой инвентарь");
        if (!hasPlayerInventory(player)) return fail("Инвентарь игрока сейчас недоступен");

        StoredLayout layout = stored(id);
        if (layout == null) return fail("Сохранение не найдено");
        List<ItemStack> desired = decode(layout, client);
        if (desired.size() != slotIds().size()) return fail("Сохранение повреждено");

        List<ItemStack> current = currentStacks(player);
        List<String> missing = missingItems(desired, current, player);
        if (!missing.isEmpty()) {
            String text = "Не хватает: " + String.join(", ", missing.subList(0, Math.min(4, missing.size())));
            if (missing.size() > 4) text += " и ещё " + (missing.size() - 4);
            return fail(text);
        }

        List<String> surplus = surplusItems(desired, current, player);
        if (!surplus.isEmpty()) {
            String text = "Убери лишнее: " + String.join(", ", surplus.subList(0, Math.min(4, surplus.size())));
            if (surplus.size() > 4) text += " и ещё " + (surplus.size() - 4);
            return fail(text);
        }

        ArrayDeque<ClickAction> plan = buildPlan(current, desired);
        if (plan == null) {
            return fail("Нужен один свободный слот для перестановки");
        }
        if (plan.isEmpty()) {
            setStatus("Инвентарь уже расположен как в сохранении", true);
            return new Result(true, status, layout.id);
        }

        actions.addAll(plan);
        loadingSyncId = player.playerScreenHandler.syncId;
        plannedActions = actions.size();
        completedActions = 0;
        loadingName = layout.name;
        nextClickAt = System.currentTimeMillis() + 90L;
        setStatus("Загрузка " + layout.name + ": 0%", true);
        client.setScreen(new InventoryScreen(player));
        return new Result(true, status, layout.id);
    }

    public synchronized void tick(MinecraftClient client) {
        if (actions.isEmpty()) return;
        ClientPlayerEntity player = client.player;
        if (!hasPlayerInventory(player) || player.playerScreenHandler.syncId != loadingSyncId
                || player.currentScreenHandler != player.playerScreenHandler) {
            cancel("Загрузка отменена: инвентарь изменился");
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextClickAt || client.interactionManager == null) return;

        ClickAction action = actions.removeFirst();
        client.interactionManager.clickSlot(loadingSyncId, action.slotId, action.button, action.type, player);
        completedActions++;
        nextClickAt = now + CLICK_DELAY_MS;
        if (actions.isEmpty()) {
            setStatus("Инвентарь " + loadingName + " загружен", true);
            resetLoading();
        } else {
            int progress = Math.round(completedActions * 100f / Math.max(1, plannedActions));
            setStatus("Загрузка " + loadingName + ": " + progress + "%", true);
        }
    }

    public synchronized boolean isLoading() {
        return !actions.isEmpty();
    }

    public synchronized float progress() {
        return plannedActions == 0 ? 0f : Math.min(1f, completedActions / (float) plannedActions);
    }

    public synchronized String status() {
        return status;
    }

    public synchronized boolean statusGood() {
        return statusGood;
    }

    public synchronized long statusAt() {
        return statusAt;
    }

    public synchronized boolean[] missingSlots(LayoutView layout) {
        int[] amounts = missingAmounts(layout);
        boolean[] missing = new boolean[amounts.length];
        for (int i = 0; i < amounts.length; i++) missing[i] = amounts[i] > 0;
        return missing;
    }

    public synchronized int[] missingAmounts(LayoutView layout) {
        int[] missing = new int[layout == null ? 0 : layout.stacks().size()];
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (layout == null || !hasPlayerInventory(player)) return missing;
        Map<String, Integer> available = countsByComponent(currentStacks(player), player);
        for (int i = 0; i < layout.stacks().size(); i++) {
            ItemStack wanted = layout.stacks().get(i);
            if (wanted == null || wanted.isEmpty()) continue;
            String key = componentKey(wanted, player);
            int have = available.getOrDefault(key, 0);
            missing[i] = Math.max(0, wanted.getCount() - have);
            available.put(key, Math.max(0, have - wanted.getCount()));
        }
        return missing;
    }

    private ArrayDeque<ClickAction> buildPlan(List<ItemStack> source, List<ItemStack> desired) {
        List<ItemStack> simulated = source.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toList());
        List<Integer> slots = slotIds();
        ArrayDeque<ClickAction> result = new ArrayDeque<>();
        boolean[] fixed = new boolean[desired.size()];
        for (int targetIndex = 0; targetIndex < desired.size(); targetIndex++) {
            ItemStack wanted = desired.get(targetIndex);
            if (wanted.isEmpty()) continue;

            if (!sameComponent(simulated.get(targetIndex), wanted)) {
                int sourceIndex = findComponent(simulated, wanted, fixed, targetIndex);
                if (sourceIndex < 0) return null;
                appendSwap(result, slots.get(sourceIndex), slots.get(targetIndex), simulated.get(targetIndex));
                ItemStack old = simulated.set(targetIndex, simulated.get(sourceIndex));
                simulated.set(sourceIndex, old);
            }

            while (simulated.get(targetIndex).getCount() < wanted.getCount()) {
                int needed = wanted.getCount() - simulated.get(targetIndex).getCount();
                int donor = findComponent(simulated, wanted, fixed, targetIndex);
                if (donor < 0) return null;
                int amount = Math.min(needed, simulated.get(donor).getCount());
                appendTransfer(result, slots.get(donor), slots.get(targetIndex), amount,
                        simulated.get(donor).getCount());
                transferSimulated(simulated, donor, targetIndex, amount);
            }

            while (simulated.get(targetIndex).getCount() > wanted.getCount()) {
                int excess = simulated.get(targetIndex).getCount() - wanted.getCount();
                int receiver = findReceiver(simulated, desired, fixed, targetIndex, wanted);
                if (receiver < 0) return null;
                int capacity = simulated.get(receiver).isEmpty()
                        ? wanted.getMaxCount()
                        : simulated.get(receiver).getMaxCount() - simulated.get(receiver).getCount();
                int amount = Math.min(excess, capacity);
                if (amount <= 0) return null;
                appendTransfer(result, slots.get(targetIndex), slots.get(receiver), amount,
                        simulated.get(targetIndex).getCount());
                transferSimulated(simulated, targetIndex, receiver, amount);
            }
            fixed[targetIndex] = true;
        }

        for (int i = 0; i < desired.size(); i++) if (!sameStack(simulated.get(i), desired.get(i))) return null;
        return result;
    }

    private static int findComponent(List<ItemStack> stacks, ItemStack wanted, boolean[] fixed, int excluded) {
        for (int i = 0; i < stacks.size(); i++) {
            if (i != excluded && !fixed[i] && sameComponent(stacks.get(i), wanted)) return i;
        }
        return -1;
    }

    private static int findReceiver(List<ItemStack> current, List<ItemStack> desired, boolean[] fixed,
                                    int excluded, ItemStack component) {
        for (int i = 0; i < current.size(); i++) {
            if (i == excluded || fixed[i] || !sameComponent(desired.get(i), component)) continue;
            ItemStack stack = current.get(i);
            if (stack.isEmpty() || sameComponent(stack, component) && stack.getCount() < stack.getMaxCount()) return i;
        }
        for (int i = 0; i < current.size(); i++) {
            if (i != excluded && !fixed[i] && current.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static void appendTransfer(ArrayDeque<ClickAction> out, int source, int target, int amount,
                                       int sourceCount) {
        out.add(new ClickAction(source, 0, SlotActionType.PICKUP));
        if (amount == sourceCount) {
            out.add(new ClickAction(target, 0, SlotActionType.PICKUP));
            return;
        }
        for (int i = 0; i < amount; i++) out.add(new ClickAction(target, 1, SlotActionType.PICKUP));
        out.add(new ClickAction(source, 0, SlotActionType.PICKUP));
    }

    private static void transferSimulated(List<ItemStack> stacks, int source, int target, int amount) {
        ItemStack from = stacks.get(source);
        ItemStack to = stacks.get(target);
        int fromLeft = from.getCount() - amount;
        int targetCount = (to.isEmpty() ? 0 : to.getCount()) + amount;
        stacks.set(target, from.copyWithCount(targetCount));
        stacks.set(source, fromLeft <= 0 ? ItemStack.EMPTY : from.copyWithCount(fromLeft));
    }

    private static void appendSwap(ArrayDeque<ClickAction> out, int source, int target, ItemStack targetStack) {
        if (source == target) return;
        if (isArmorSlot(source) || isArmorSlot(target)) {
            out.add(new ClickAction(source, 0, SlotActionType.PICKUP));
            out.add(new ClickAction(target, 0, SlotActionType.PICKUP));
            if (!targetStack.isEmpty()) out.add(new ClickAction(source, 0, SlotActionType.PICKUP));
            return;
        }
        if (source == SCRATCH_SLOT) {
            out.add(new ClickAction(target, SCRATCH_HOTBAR_BUTTON, SlotActionType.SWAP));
        } else if (target == SCRATCH_SLOT) {
            out.add(new ClickAction(source, SCRATCH_HOTBAR_BUTTON, SlotActionType.SWAP));
        } else {
            out.add(new ClickAction(source, SCRATCH_HOTBAR_BUTTON, SlotActionType.SWAP));
            out.add(new ClickAction(target, SCRATCH_HOTBAR_BUTTON, SlotActionType.SWAP));
            out.add(new ClickAction(source, SCRATCH_HOTBAR_BUTTON, SlotActionType.SWAP));
        }
    }

    private static List<String> missingItems(List<ItemStack> desired, List<ItemStack> current,
                                             ClientPlayerEntity player) {
        Map<String, Integer> required = countsByComponent(desired, player);
        Map<String, Integer> available = countsByComponent(current, player);
        Map<String, String> names = new LinkedHashMap<>();
        for (ItemStack stack : desired) {
            if (!stack.isEmpty()) names.putIfAbsent(componentKey(stack, player), stack.getName().getString());
        }
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int amount = entry.getValue() - available.getOrDefault(entry.getKey(), 0);
            if (amount > 0) missing.add(names.getOrDefault(entry.getKey(), "Предмет") + " x" + amount);
        }
        return missing;
    }

    private static List<String> surplusItems(List<ItemStack> desired, List<ItemStack> current,
                                             ClientPlayerEntity player) {
        Map<String, Integer> required = countsByComponent(desired, player);
        Map<String, Integer> available = countsByComponent(current, player);
        Map<String, String> names = new LinkedHashMap<>();
        for (ItemStack stack : current) {
            if (!stack.isEmpty()) names.putIfAbsent(componentKey(stack, player), stack.getName().getString());
        }
        List<String> surplus = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : available.entrySet()) {
            int amount = entry.getValue() - required.getOrDefault(entry.getKey(), 0);
            if (amount > 0) surplus.add(names.getOrDefault(entry.getKey(), "Предмет") + " x" + amount);
        }
        return surplus;
    }

    private static Map<String, Integer> countsByComponent(List<ItemStack> stacks, ClientPlayerEntity player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            counts.merge(componentKey(stack, player), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static String componentKey(ItemStack stack, ClientPlayerEntity player) {
        try {
            return stack.copyWithCount(1).toNbt(player.getRegistryManager()).toString();
        } catch (Exception ignored) {
            return stack.getItem().toString();
        }
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        return a.getCount() == b.getCount() && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static boolean sameComponent(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static boolean hasPlayerInventory(ClientPlayerEntity player) {
        return player != null && player.playerScreenHandler != null
                && player.playerScreenHandler.slots.size() > PlayerScreenHandler.OFFHAND_ID;
    }

    private static boolean isArmorSlot(int slot) {
        return slot >= PlayerScreenHandler.EQUIPMENT_START && slot < PlayerScreenHandler.EQUIPMENT_END;
    }

    private static List<Integer> slotIds() {
        List<Integer> ids = new ArrayList<>(41);
        for (int slot = PlayerScreenHandler.EQUIPMENT_START; slot < PlayerScreenHandler.HOTBAR_END; slot++) ids.add(slot);
        ids.add(PlayerScreenHandler.OFFHAND_ID);
        return ids;
    }

    private static List<ItemStack> currentStacks(ClientPlayerEntity player) {
        List<ItemStack> result = new ArrayList<>(41);
        for (int slot : slotIds()) result.add(player.playerScreenHandler.getSlot(slot).getStack().copy());
        return result;
    }

    private List<ItemStack> decode(StoredLayout layout, MinecraftClient client) {
        if (layout != null && layout.cachedStacks != null && layout.cachedStacks.size() == 41) {
            return layout.cachedStacks;
        }
        List<ItemStack> result = new ArrayList<>(41);
        if (layout == null || layout.stacks == null || client.world == null) return result;
        for (String encoded : layout.stacks) {
            if (encoded == null || encoded.isBlank()) {
                result.add(ItemStack.EMPTY);
                continue;
            }
            try {
                result.add(ItemStack.fromNbt(client.world.getRegistryManager(), StringNbtReader.parse(encoded))
                        .orElse(ItemStack.EMPTY));
            } catch (Exception ignored) {
                result.add(ItemStack.EMPTY);
            }
        }
        if (result.size() == 41) layout.cachedStacks = result;
        return result;
    }

    private StoredLayout stored(String id) {
        if (id == null) return null;
        for (StoredLayout layout : layouts) if (layout.id.equals(id)) return layout;
        return null;
    }

    private Result fail(String message) {
        setStatus(message, false);
        return new Result(false, message, "");
    }

    private void cancel(String reason) {
        actions.clear();
        resetLoading();
        setStatus(reason, false);
    }

    private void resetLoading() {
        actions.clear();
        loadingSyncId = -1;
        plannedActions = 0;
        completedActions = 0;
        nextClickAt = 0L;
        loadingName = "";
    }

    private void setStatus(String message, boolean good) {
        status = message;
        statusGood = good;
        statusAt = System.currentTimeMillis();
    }

    private void loadFile() {
        if (!Files.exists(FILE)) return;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            StoredData data = GSON.fromJson(reader, StoredData.class);
            if (data == null || data.layouts == null) return;
            layouts.clear();
            for (StoredLayout layout : data.layouts) {
                if (layout == null || layout.id == null || layout.name == null || layout.stacks == null) continue;
                layouts.add(layout);
            }
        } catch (Exception ignored) {
            layouts.clear();
        }
    }

    private boolean saveFile() {
        try {
            Files.createDirectories(FILE.getParent());
            StoredData data = new StoredData();
            data.layouts = new ArrayList<>(layouts);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
            return true;
        } catch (Exception exception) {
            setStatus("Не удалось записать файл сохранений", false);
            return false;
        }
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String value = raw.strip().replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s{2,}", " ");
        return value.length() > 36 ? value.substring(0, 36) : value;
    }

    public record LayoutView(String id, String name, long createdAt, int itemCount, List<ItemStack> stacks) {
    }

    public record Result(boolean success, String message, String id) {
    }

    private record ClickAction(int slotId, int button, SlotActionType type) {
    }

    private static final class StoredData {
        private List<StoredLayout> layouts = new ArrayList<>();
    }

    private static final class StoredLayout {
        private String id;
        private String name;
        private long createdAt;
        private long updatedAt;
        private List<String> stacks = new ArrayList<>();
        private transient List<ItemStack> cachedStacks;
    }
}
