package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.item.ItemStack;

@Register(name = "Low Armor Notify", id = "low_armor_notify", description = "Предупреждает о низкой прочности брони", category = Category.UTILS)
public class LowArmorNotify extends Module {
    private final SliderSetting threshold = add(new SliderSetting("Порог %", "threshold", 15, 5, 50, 1));
    private final SliderSetting delaySec = add(new SliderSetting("Задержка (сек)", "delay", 30, 5, 120, 5));

    private long lastNotify = 0;

    @Override
    public void onTick() {
        if (!ClientActionUtil.inGame() || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now - lastNotify < delaySec.get().intValue() * 1000L) return;

        int thresholdPercent = threshold.get().intValue();
        for (ItemStack armor : mc.player.getArmorItems()) {
            if (armor.isEmpty()) continue;
            int max = armor.getMaxDamage();
            int current = max - armor.getDamage();
            if (max > 0) {
                int percent = (current * 100) / max;
                if (percent <= thresholdPercent) {
                    String name = armor.getName().getString();
                    ClientActionUtil.clientMessage("§c" + name + " §fпрочность §e" + percent + "% §7(" + current + "/" + max + ")");
                    lastNotify = now;
                    return; // Одно предупреждение за раз
                }
            }
        }
    }
}
