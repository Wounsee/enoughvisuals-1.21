package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.utils.player.ClientActionUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

@Register(name = "AutoPotion", id = "auto_potion", description = "Автоматически пьёт", category = Category.UTILS)
public class AutoPotion extends Module {
    private final BooleanSetting invisibility = add(new BooleanSetting("Невидимость", "invisibility", "Пить зелье невидимости", true));
    private final BooleanSetting speed = add(new BooleanSetting("Скорость", "speed", "Пить зелье скорости", true));

    private boolean usingPotion;
    private int previousSlot = -1;
    private boolean previousUseKey;

    @Override
    public void onTick() {
        if (!isInGame()) return;
        if (!usingPotion) previousUseKey = mc.options.useKey.isPressed();

        int invisSlot = -1;
        int speedSlot = -1;

        if (invisibility.get() && needsEffect(StatusEffects.INVISIBILITY)) {
            invisSlot = findPotion(StatusEffects.INVISIBILITY);
        }
        if (speed.get() && needsEffect(StatusEffects.SPEED)) {
            speedSlot = findPotion(StatusEffects.SPEED);
        }

        int target = invisSlot != -1 ? invisSlot : speedSlot;
        if (target == -1) {
            restore();
            return;
        }

        if (!usingPotion) previousSlot = mc.player.getInventory().selectedSlot;
        ClientActionUtil.selectHotbar(target);
        mc.options.useKey.setPressed(true);
        usingPotion = true;
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private boolean needsEffect(RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = mc.player.getStatusEffect(effect);
        return instance == null || instance.getDuration() <= 200;
    }

    private int findPotion(RegistryEntry<StatusEffect> effect) {
        return ClientActionUtil.findHotbar(stack -> stack.isOf(Items.POTION) && hasEffect(stack, effect)).orElse(-1);
    }

    private boolean hasEffect(ItemStack stack, RegistryEntry<StatusEffect> effect) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;

        for (StatusEffectInstance custom : contents.customEffects()) {
            if (custom.getEffectType().equals(effect)) return true;
        }

        Optional<Boolean> vanilla = contents.potion()
                .map(entry -> ((Potion) entry.value()).getEffects().stream().anyMatch(instance -> instance.getEffectType().equals(effect)));
        return vanilla.orElse(false);
    }

    private void restore() {
        if (!usingPotion) return;
        if (previousSlot >= 0) ClientActionUtil.selectHotbar(previousSlot);
        mc.options.useKey.setPressed(previousUseKey);
        usingPotion = false;
        previousSlot = -1;
    }
}
