package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.hud.HudTopInset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class MixinBossBarHud {

    @Shadow
    @Final
    Map<UUID, ClientBossBar> bossBars;

    @ModifyVariable(
            method = "render",
            at = @At(
                    value = "STORE"
            ),
            ordinal = 1
    )
    private int moveBossBarDown(int y) {
        return y;
    }

    /**
     * Mirrors the vanilla stacking loop: bars start at y=12, advance by
     * {@code 10 + fontHeight} each, and stop once a third of the screen is
     * used. Reporting the result lets client widgets avoid the same strip.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void enough$reportOccupiedHeight(DrawContext context, CallbackInfo ci) {
        if (bossBars.isEmpty()) {
            return;
        }
        int step = 10 + MinecraftClient.getInstance().textRenderer.fontHeight;
        int limit = context.getScaledWindowHeight() / 3;
        int bottom = 12;
        for (int index = 0; index < bossBars.size(); index++) {
            bottom += step;
            if (bottom >= limit) {
                break;
            }
        }
        HudTopInset.reportBossBarBottom(bottom);
    }
}
