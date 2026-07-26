package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Register(name = "Mace Indicator", id = "mace_indicator", description = "Подсвечивает булаву в зависимости от времени перезарядки", category = Category.VISUAL)
public class MaceIndicator extends Module {

    private static MaceIndicator INSTANCE;

    private static final int RGB_RED = 0xFF3030;
    private static final int RGB_YELLOW = 0xFFD840;
    private static final int RGB_GREEN = 0x33FF55;

    public final SliderSetting opacity = add(new SliderSetting("Непрозрачность", "opacity", "Процент непрозрачности", 55.0, 0.0, 100.0, 1.0));

    public MaceIndicator() {
        INSTANCE = this;
    }

    public static MaceIndicator getInstance() {
        return INSTANCE;
    }

    public int colorForStack(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) {
            return 0;
        }
        if (!stack.isOf(Items.MACE)) {
            return 0;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return 0;
        }
        PlayerEntity p = mc.player;
        if (p == null) {
            return 0;
        }

        ItemStack held = p.getMainHandStack();
        if (held == null || !held.isOf(Items.MACE)) {
            return 0;
        }

        float charge = chargeOf(p);
        int rgb;
        if (charge <= 0.30F) {
            rgb = RGB_RED;
        } else if (charge <= 0.60F) {
            rgb = RGB_YELLOW;
        } else {
            rgb = RGB_GREEN;
        }
        return packArgb(rgb, opacity.get().floatValue() / 100.0F);
    }

    public float chargeOf(PlayerEntity p) {
        if (p == null) {
            return 0.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, p.getAttackCooldownProgress(0.0F)));
    }

    private static int packArgb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
