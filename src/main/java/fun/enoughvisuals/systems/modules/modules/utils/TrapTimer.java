package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Arrays;

@Register(name = "Trap Timer", id = "trap_timer", description = "Таймер перезарядки трапки на FunTime", category = Category.UTILS)
public class TrapTimer extends Module {

    private static TrapTimer INSTANCE;

    public enum TrapType {
        NORMAL(15_000L),
        DRAGON(20_000L);

        public final long durationMs;

        TrapType(long durationMs) {
            this.durationMs = durationMs;
        }
    }

    public final ModeSetting trapType = add(new ModeSetting("Тип трапки", "trap_type", "Какая трапка используется: Обычная (15с) или Драконья (20с)", "Обычная", "Обычная", "Драконья"));
    public final StringSetting trapName = add(new StringSetting("Название трапки", "trap_name", "трапка"));

    private long timerStartMs = 0L;
    private boolean timerActive = false;
    private String activeItemText = "";

    public TrapTimer() {
        INSTANCE = this;
    }

    public static TrapTimer getInstance() {
        return INSTANCE;
    }

    public TrapType getSelectedType() {
        return "Драконья".equalsIgnoreCase(trapType.get()) ? TrapType.DRAGON : TrapType.NORMAL;
    }

    public void onItemUse(PlayerEntity player, Hand hand) {
        if (!isEnabled() || player == null || hand == null) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.NETHERITE_SCRAP) {
            return;
        }
        String needle = trapName.get();
        if (needle == null || needle.isEmpty()) {
            return;
        }
        String itemName = stack.getName().getString().toLowerCase();
        if (!itemName.contains(needle.toLowerCase())) {
            return;
        }
        startTimer(stack.getName().getString());
    }

    @Override
    public void onTick() {
        if (!timerActive || !isEnabled()) {
            return;
        }
        if (getRemainingSecondsPrecise() > 0.0F) {
            return;
        }
        timerActive = false;
        activeItemText = "";
    }

    public float getRemainingSecondsPrecise() {
        if (!timerActive) {
            return 0.0F;
        }
        long elapsed = System.currentTimeMillis() - timerStartMs;
        long remainingMs = getSelectedType().durationMs - elapsed;
        return Math.max(0.0F, remainingMs / 1000.0F);
    }

    public float getProgress() {
        if (!timerActive) {
            return 0.0F;
        }
        long elapsed = System.currentTimeMillis() - timerStartMs;
        long duration = getSelectedType().durationMs;
        return Math.min(1.0F, (float) elapsed / (float) duration);
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    public String getActiveItemText() {
        return activeItemText;
    }

    private void startTimer(String itemName) {
        timerStartMs = System.currentTimeMillis();
        timerActive = true;
        activeItemText = itemName;
    }

    @Override
    protected void onDisable() {
        timerActive = false;
        activeItemText = "";
    }
}
