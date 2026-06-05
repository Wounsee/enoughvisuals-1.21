package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BossBarHud.class)
public class MixinBossBarHud {

    @ModifyVariable(
            method = "render",
            at = @At(
                    value = "STORE"
            ),
            ordinal = 1
    )
    private int moveBossBarDown(int y) {
        return y + 27;
    }
}