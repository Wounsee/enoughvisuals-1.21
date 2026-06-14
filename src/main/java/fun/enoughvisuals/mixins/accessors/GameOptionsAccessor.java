package fun.enoughvisuals.mixins.accessors;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {
    @Accessor("fullscreenResolution")
    void enoughvisuals$setFullscreenResolution(String fullscreenResolution);
}
