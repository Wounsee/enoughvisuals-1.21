package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.systems.modules.modules.visual.Cooldowns;
import fun.enoughvisuals.systems.modules.modules.visual.ColorPlus;
import fun.enoughvisuals.systems.modules.modules.visual.MotionBlur;
import fun.enoughvisuals.systems.modules.modules.visual.Crosshair;
import fun.enoughvisuals.systems.modules.modules.visual.CustomTab;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.ItemLight;
import fun.enoughvisuals.systems.modules.modules.visual.MouseParticles;
import fun.enoughvisuals.systems.modules.modules.visual.NoRender;
import fun.enoughvisuals.systems.modules.modules.visual.Prediction;
import fun.enoughvisuals.systems.modules.modules.utils.StreamerMode;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.ScreenScaleUtil;
import fun.enoughvisuals.utils.render.ShaderHandsRenderer;
import fun.enoughvisuals.utils.render.StencilUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private PlayerListHud playerListHud;

    @Unique
    private float enough$smoothCooldown = 1.0f;

    @Inject(method = "render", at = @At("HEAD"))
    private void enough$beginBlur(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        MotionBlur.renderExact();
        ColorPlus.renderExact();
        RenderUtil.beginFrame();
        // Safety reset: prevents leaked scissor state from clipping vanilla HUD
        // (scoreboard/effect icons) after custom tab/watermark animations.
        StencilUtil.resetRenderState();
        GlassUtil.BLUR_OFFSET = CustomTheme.hudBlurLevel();
        if (CustomTheme.hudBlurEnabled()) {
            GlassUtil.beginFrame();
        } else {
            GlassUtil.skipFrame();
        }

        // Render hand post-effect before vanilla HUD/chat so it cannot cover
        // chat preview, scoreboard or our HUD widgets.
        ShaderHandsRenderer.render(ctx);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        StencilUtil.resetRenderState();
        StreamerMode.captureFromScoreboard(objective);
        if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.scoreboard.get()) {
            ci.cancel();
            return;
        }
        if (DragAndDrop.getInstance().getActiveElementByTypeId("custom_scoreboard") != null) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("RETURN"))
    private void enough$resetStateAfterScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        StencilUtil.resetRenderState();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortal(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        if (NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.portal.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        StencilUtil.resetRenderState();
        LockSlot module = LockSlot.getInstance();

        MinecraftClient mc = MinecraftClient.getInstance();
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200f);

        for (int slot = 0; slot < 9; slot++) {
            boolean isLocked = false;
            if (module != null && module.isEnabled()) {
                if (module.isLocked(slot)) {
                    int x = scaledWidth / 2 - 88 + slot * 20;
                    int y = scaledHeight - 19;
                    context.fill(x, y, x + 16, y + 16, 0x60FF0000);
                    isLocked = true;
                }
            }

            if (!isLocked && mc.player != null) {
                net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(slot);
                if (stack != null && !stack.isEmpty()) {
                    int x = scaledWidth / 2 - 88 + slot * 20;
                    int y = scaledHeight - 19;

                    fun.enoughvisuals.systems.modules.modules.utils.HealingHelper healingHelper = fun.enoughvisuals.systems.modules.modules.utils.HealingHelper.getInstance();
                    if (healingHelper != null && healingHelper.isEnabled()) {
                        int color = healingHelper.colorForStack(stack);
                        if ((color & 0xFF000000) != 0) {
                            context.fill(x, y, x + 16, y + 16, color);
                        }
                    }

                    fun.enoughvisuals.systems.modules.modules.visual.MaceIndicator maceIndicator = fun.enoughvisuals.systems.modules.modules.visual.MaceIndicator.getInstance();
                    if (maceIndicator != null && maceIndicator.isEnabled()) {
                        int color = maceIndicator.colorForStack(stack);
                        if ((color & 0xFF000000) != 0) {
                            context.fill(x, y, x + 16, y + 16, color);
                        }
                    }
                }
            }
        }

        context.getMatrices().pop();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (texture.getPath().contains("pumpkinblur") && NoRender.INSTANCE != null && NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.pumpkin.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void enough$renderHud(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        StencilUtil.resetRenderState();

        // Independent from hudHidden — render even when HUD is hidden (F1)
        // Scale to virtual coordinate space so size is consistent regardless of GUI scale
        float fixedScale = ScreenScaleUtil.scale();
        ctx.getMatrices().push();
        ctx.getMatrices().scale(fixedScale, fixedScale, 1f);
        ItemLight.renderOverlay(ctx, counter.getTickDelta(true));
        Prediction.renderOverlay2D(ctx, counter.getTickDelta(true));
        ctx.getMatrices().pop();

        if (client.options != null && client.options.hudHidden) {
            StencilUtil.resetRenderState();
            return;
        }
        GpsSystem.getInstance().renderOverlay(ctx, counter.getTickDelta(true));
        Cooldowns.renderHotbarOverlay(ctx, counter.getTickDelta(true));
        CustomTab.tickAnimation(counter.getTickDelta(true));
        // Vanilla chat preview and scoreboard queue text in DrawContext. Flush it
        // before any later overlay work.
        ctx.draw();
        DragAndDrop.getInstance().renderAll(ctx, counter.getTickDelta(true));
        MouseParticles.renderHud(ctx, counter.getTickDelta(true));
        StencilUtil.resetRenderState();
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void enough$renderAnimatedVanillaPlayerList(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        Animations animations = Animations.active();
        if (animations == null || !animations.vanillaTabAnimation.get() || CustomTab.shouldRenderCustomTab()) {
            return;
        }
        if (client == null || client.world == null || client.player == null || client.options == null) {
            return;
        }

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
        boolean hiddenSingleplayerList = client.isInSingleplayer()
                && client.getNetworkHandler() != null
                && client.getNetworkHandler().getListedPlayerListEntries().size() <= 1
                && objective == null;
        boolean shown = client.options.playerListKey.isPressed() && !hiddenSingleplayerList;
        animations.updatePlayerListShown(shown);

        if (!shown && !animations.shouldRenderPlayerList()) {
            playerListHud.setVisible(false);
            ci.cancel();
            return;
        }

        float progress = RenderUtil.easeInOutCubic(animations.playerListProgress());
        if (progress <= 0.001f) {
            playerListHud.setVisible(false);
            ci.cancel();
            return;
        }

        ci.cancel();
        playerListHud.setVisible(true);

        float centerX = ctx.getScaledWindowWidth() * 0.5f;
        float tabOffsetY = 24f;
        ctx.getMatrices().push();
        try {
            ctx.getMatrices().translate(centerX, tabOffsetY - (1f - progress) * 8f, 0f);
            ctx.getMatrices().scale(progress, progress, 1f);
            ctx.getMatrices().translate(-centerX, 0f, 0f);
            playerListHud.render(ctx, ctx.getScaledWindowWidth(), scoreboard, objective);
        } finally {
            ctx.getMatrices().pop();
        }

        if (!shown && !animations.shouldRenderPlayerList()) {
            playerListHud.setVisible(false);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Crosshair module = ModuleManager.getInstance().get(Crosshair.class);
        if (module == null || !module.isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && client.options.hudHidden) {
            ci.cancel();
            return;
        }

        ci.cancel();
        if (client.player == null) return;

        float fixedScale = ScreenScaleUtil.scale();
        float x = ScreenScaleUtil.virtualWidth() / 2.0f;
        float y = ScreenScaleUtil.virtualHeight() / 2.0f;
        int color = module.color.get();
        String type = module.type.get();

        context.getMatrices().push();
        context.getMatrices().scale(fixedScale, fixedScale, 1f);

        if ("Орбиз".equals(type)) {
            RenderUtil.drawCircle(context, x, y, 1.2f, color);
            context.getMatrices().pop();
            return;
        }

        if ("Круг".equals(type)) {
            float rawProgress = client.player.getAttackCooldownProgress(0.0f);
            float follow = Math.min(1f, 0.18f + tickCounter.getTickDelta(true) * 0.28f);
            enough$smoothCooldown += (rawProgress - enough$smoothCooldown) * follow;
            float progress = enough$smoothCooldown;

            float radius = 3.35f;
            float lineWidth = 0.95f;

            if (progress >= 1.0f) {
                RenderUtil.drawCircleOutline(context, x, y, radius, lineWidth, color);
            } else if (progress > 0.001f) {
                RenderUtil.drawArc(context, x, y, radius, lineWidth, color, progress);
            }
        }
        context.getMatrices().pop();
    }
}
