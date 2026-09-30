package fun.enoughvisuals.mixins.accessors;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Указываем путь к приватному классу через targets
@Mixin(targets = "net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen$CreativeSlot")
public interface CreativeSlotAccessor {

    @Accessor("slot")
    Slot getRealSlot();
}
