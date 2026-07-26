package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Register(name = "Healing Helper", id = "healing_helper", description = "Подсвечивает зелья исцеления и золотые яблоки при низком HP/сытости", category = Category.UTILS)
public class HealingHelper extends Module {

    private static HealingHelper INSTANCE;
    private static final int CONSUMABLE_FINISH_TICKS = 32;
    private static final int FINISH_TOLERANCE = 2;
    private static final long PULSE_PERIOD_MS = 500L;
    private static final float MAX_ALPHA = 0.55F;

    private static final int RGB_GREEN = 0x33FF55;
    private static final int RGB_YELLOW = 0xFFD840;
    private static final int RGB_RED = 0xFF3030;
    private static final int RGB_ORANGE = 0xFF8A00;

    public final SliderSetting hpThreshold = add(new SliderSetting("Порог HP зелья", "hp_threshold", "Подсвечивать зелья лечения при HP ниже этого значения", 14.0, 1.0, 20.0, 1.0));
    public final SliderSetting gappleCooldownSec = add(new SliderSetting("КД зачар. яблок", "gapple_cooldown", "Задержка повторной подсветки зачарованных яблок после съедения (в сек)", 60.0, 1.0, 120.0, 1.0));
    public final SliderSetting saturationThreshold = add(new SliderSetting("Сытость для яблок", "saturation_threshold", "Подсвечивать золотые яблоки при сытости ниже этого значения", 6.0, 1.0, 20.0, 1.0));

    private int prevEgappleUseTime = -1;
    private long lastEgappleEatenMs = 0L;

    public HealingHelper() {
        INSTANCE = this;
    }

    public static HealingHelper getInstance() {
        return INSTANCE;
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            prevEgappleUseTime = -1;
            return;
        }

        PlayerEntity p = mc.player;
        boolean usingEgappleNow = p.isUsingItem()
                && p.getActiveItem() != null
                && p.getActiveItem().isOf(Items.ENCHANTED_GOLDEN_APPLE);

        if (prevEgappleUseTime >= CONSUMABLE_FINISH_TICKS - FINISH_TOLERANCE && !usingEgappleNow) {
            lastEgappleEatenMs = System.currentTimeMillis();
        }

        prevEgappleUseTime = usingEgappleNow ? p.getItemUseTime() : -1;
    }

    public int colorForStack(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty() || !isInGame()) {
            return 0;
        }

        PlayerEntity p = mc.player;
        float health = p.getHealth();
        float saturation = p.getHungerManager().getSaturationLevel();
        int hpThr = hpThreshold.get().intValue();
        int satThr = saturationThreshold.get().intValue();
        boolean lowHp = health <= hpThr;
        boolean lowSat = saturation <= satThr;

        int rgb = 0;

        if (isHealingPotion(stack)) {
            if (lowHp) {
                rgb = lowSat ? RGB_GREEN : RGB_YELLOW;
            }
        } else if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            long since = System.currentTimeMillis() - lastEgappleEatenMs;
            long cooldownMs = gappleCooldownSec.get().longValue() * 1000L;
            if (lastEgappleEatenMs == 0L || since >= cooldownMs) {
                rgb = RGB_RED;
            }
        } else if (stack.isOf(Items.GOLDEN_APPLE)) {
            if (lowSat) {
                rgb = RGB_ORANGE;
            }
        }

        if (rgb == 0) {
            return 0;
        }
        return packArgb(rgb, MAX_ALPHA * pulseAlpha());
    }

    private static float pulseAlpha() {
        long t = System.currentTimeMillis() % PULSE_PERIOD_MS;
        return (float) Math.sin(Math.PI * t / (double) PULSE_PERIOD_MS);
    }

    private static int packArgb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (StatusEffectInstance effect : contents.getEffects()) {
            if (effect.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) {
                return true;
            }
        }
        return false;
    }
}
