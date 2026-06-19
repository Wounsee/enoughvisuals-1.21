package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.modules.modules.visual.CustomTab;
import fun.enoughvisuals.ui.hud.WatermarkHud;
import net.minecraft.client.gui.hud.BossBarHud;
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
        int offset = 27;
        float tabProgress = CustomTab.getAnimationProgress();
        if (tabProgress > 0.01f && WatermarkHud.customTabEnabled()) {
            float[] tabSize = CustomTab.getPreferredSize();
            if (tabSize != null) {
                offset += (int) (tabSize[1] * tabProgress) + 6;
            }
        }
        return y + offset;
    }
}