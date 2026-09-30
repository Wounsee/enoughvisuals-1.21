package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.mixins.accessors.CreativeSlotAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;

import java.util.HashSet;
import java.util.Set;

@Register(name = "Lock Slot", id = "lock_slot", description = "Блокирует слоты от перемещения и выброса (СКМ по слоту)", category = Category.UTILS)
public class LockSlot extends Module {

    private static LockSlot INSTANCE;

    // Храним индексы заблокированных слотов (0-35)
    private final Set<Integer> lockedSlots = new HashSet<>();

    public LockSlot() {
        INSTANCE = this;
    }

    public static LockSlot getInstance() {
        return INSTANCE;
    }

    public boolean isLocked(int inventoryIndex) {
        return isEnabled() && lockedSlots.contains(inventoryIndex);
    }

    public void toggleLock(int inventoryIndex) {
        if (lockedSlots.contains(inventoryIndex)) {
            lockedSlots.remove(inventoryIndex);
        } else {
            lockedSlots.add(inventoryIndex);
        }
    }

    // Тот самый распаковщик: достает РЕАЛЬНЫЙ индекс слота
    public int getTruePlayerSlotIndex(Slot slot) {
        if (slot == null) return -1;

        // 1. Проверяем, креативный ли это слот, через наш интерфейс-аксессор
        if (slot instanceof CreativeSlotAccessor creativeAccessor) {
            Slot realSlot = creativeAccessor.getRealSlot();
            if (realSlot != null && realSlot.inventory instanceof PlayerInventory) {
                return realSlot.getIndex();
            }
        }
        // 2. Обычное выживание или сундук
        else if (slot.inventory instanceof PlayerInventory) {
            return slot.getIndex();
        }

        // Если это какой-то левый слот (печка, каталог блоков креатива) — игнорим
        return -1;
    }
}
