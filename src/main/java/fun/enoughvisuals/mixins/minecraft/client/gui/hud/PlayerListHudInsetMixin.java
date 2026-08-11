package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.hud.HudTopInset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudInsetMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Nullable
    private Text header;

    @Shadow
    @Nullable
    private Text footer;

    /**
     * Reproduces the vertical layout of the vanilla list — header block, then
     * one 9px line per row, then the footer — so client widgets know where it
     * ends. Only the bottom edge is needed, which keeps this independent of the
     * column and width maths.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void enough$reportOccupiedHeight(DrawContext context, int scaledWindowWidth,
                                             Scoreboard scoreboard,
                                             @Nullable ScoreboardObjective objective,
                                             CallbackInfo ci) {
        if (client.getNetworkHandler() == null) {
            return;
        }
        int entries = client.getNetworkHandler().getListedPlayerListEntries().size();
        if (entries <= 0) {
            return;
        }
        int rows = entries;
        int columns = 1;
        while (rows > 20) {
            rows = (entries + ++columns - 1) / columns;
        }

        int wrapWidth = scaledWindowWidth - 50;
        float bottom = 10f;
        if (header != null) {
            bottom += client.textRenderer.wrapLines(header, wrapWidth).size() * 9 + 1;
        }
        bottom += rows * 9;
        if (footer != null) {
            bottom += 1 + client.textRenderer.wrapLines(footer, wrapWidth).size() * 9;
        }
        HudTopInset.reportPlayerListBottom(bottom);
    }
}
