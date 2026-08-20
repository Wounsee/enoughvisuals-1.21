package fun.enoughvisuals.mixins.minecraft.client.gui.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.hud.CustomHotbarRenderer;
import fun.enoughvisuals.systems.hud.DragAndDrop;
import fun.enoughvisuals.systems.hud.HudBottomInset;
import fun.enoughvisuals.systems.hud.HudTopInset;
import fun.enoughvisuals.systems.modules.api.ModuleManager;
import fun.enoughvisuals.systems.modules.modules.utils.LockSlot;
import fun.enoughvisuals.systems.modules.modules.visual.Animations;
import fun.enoughvisuals.systems.modules.modules.visual.Cooldowns;
import fun.enoughvisuals.systems.modules.modules.visual.ColorPlus;
import fun.enoughvisuals.systems.modules.modules.visual.MotionBlur;
import fun.enoughvisuals.systems.modules.modules.visual.Crosshair;
import fun.enoughvisuals.systems.modules.modules.visual.WebPanels;
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
import net.minecraft.util.math.MathHelper;
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
    @Unique
    private float enough$webPanelCrosshairMorph = 0.0f;
    @Unique
    private float enough$webPanelCrosshairVelocity = 0.0f;

    @Inject(method = "render", at = @At("HEAD"))
    private void enough$beginBlur(DrawContext ctx, RenderTickCounter counter, CallbackInfo ci) {
        MotionBlur.renderExact();
        ColorPlus.renderExact();
        RenderUtil.beginFrame();
        HudBottomInset.beginFrame(client.currentScreen instanceof ChatScreen,
                RenderUtil.getFrameDelta());
        HudTopInset.beginFrame();
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

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void enough$renderCustomHotbar(DrawContext context, RenderTickCounter tickCounter,
                                           CallbackInfo ci) {
        if (!CustomHotbarRenderer.shouldRender()) return;
        CustomHotbarRenderer.render(context, tickCounter);
        ci.cancel();
    }

    @WrapOperation(
            method = "renderMainHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
            )
    )
    private void enough$renderVanillaHotbarAboveChat(
            InGameHud instance,
            DrawContext context,
            RenderTickCounter tickCounter,
            Operation<Void> original
    ) {
        // The custom renderer consumes the same shared inset in its baseY.
        // Wrap only vanilla here so neither implementation is translated twice.
        if (CustomHotbarRenderer.shouldRender() || HudBottomInset.current() <= 0.01f) {
            original.call(instance, context, tickCounter);
            return;
        }

        context.getMatrices().push();
        try {
            context.getMatrices().translate(0f, -HudBottomInset.current(), 0f);
            original.call(instance, context, tickCounter);
        } finally {
            context.getMatrices().pop();
        }
    }

    @WrapOperation(
            method = "renderMainHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V"
            )
    )
    private void enough$renderStatusBarsAboveChat(
            InGameHud instance,
            DrawContext context,
            Operation<Void> original
    ) {
        enough$renderAboveChat(context, () -> original.call(instance, context));
    }

    @WrapOperation(
            method = "renderMainHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderExperienceBar(Lnet/minecraft/client/gui/DrawContext;I)V"
            )
    )
    private void enough$renderExperienceBarAboveChat(
            InGameHud instance,
            DrawContext context,
            int x,
            Operation<Void> original
    ) {
        enough$renderAboveChat(context, () -> original.call(instance, context, x));
    }

    // Experience level is registered as a render-layer method reference, not
    // invoked directly from renderMainHud. Inject into the layer method itself
    // so the translation survives that indirection.
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"))
    private void enough$beginExperienceLevelAboveChat(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        context.getMatrices().push();
        context.getMatrices().translate(0f, -HudBottomInset.current(), 0f);
    }

    @Inject(method = "renderExperienceLevel", at = @At("RETURN"))
    private void enough$endExperienceLevelAboveChat(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        context.getMatrices().pop();
    }

    @Unique
    private void enough$renderAboveChat(DrawContext context, Runnable render) {
        float inset = HudBottomInset.current();
        if (inset <= 0.01f) {
            render.run();
            return;
        }

        context.getMatrices().push();
        try {
            context.getMatrices().translate(0f, -inset, 0f);
            render.run();
        } finally {
            context.getMatrices().pop();
        }
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
        if (animations == null || !animations.vanillaTabAnimation.get()) {
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
        float originY = tabOffsetY - (1f - progress) * 8f;
        ctx.getMatrices().push();
        HudTopInset.setPlayerListTransform(originY, progress);
        try {
            ctx.getMatrices().translate(centerX, originY, 0f);
            ctx.getMatrices().scale(progress, progress, 1f);
            ctx.getMatrices().translate(-centerX, 0f, 0f);
            playerListHud.render(ctx, ctx.getScaledWindowWidth(), scoreboard, objective);
        } finally {
            HudTopInset.clearPlayerListTransform();
            ctx.getMatrices().pop();
        }

        if (!shown && !animations.shouldRenderPlayerList()) {
            playerListHud.setVisible(false);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Crosshair module = ModuleManager.getInstance().get(Crosshair.class);
        boolean customEnabled = module != null && module.isEnabled();
        if (!customEnabled) {
            enough$webPanelCrosshairMorph = 0f;
            enough$webPanelCrosshairVelocity = 0f;
            return;
        }
        WebPanels webPanels = WebPanels.getInstance();
        boolean panelHover = webPanels != null && webPanels.isHovering();
        float frameDelta = Math.max(1f / 240f,
                Math.min(0.05f, RenderUtil.getFrameDelta() / 60f));
        float morphTarget = panelHover ? 1f : 0f;
        enough$webPanelCrosshairVelocity += (morphTarget - enough$webPanelCrosshairMorph)
                * 92.0f * frameDelta;
        enough$webPanelCrosshairVelocity *= (float) Math.exp(-16.0f * frameDelta);
        enough$webPanelCrosshairMorph = MathHelper.clamp(
                enough$webPanelCrosshairMorph + enough$webPanelCrosshairVelocity * frameDelta,
                0.0f, 1.0f);
        if ((enough$webPanelCrosshairMorph <= 0.0f && enough$webPanelCrosshairVelocity < 0.0f)
                || (enough$webPanelCrosshairMorph >= 1.0f && enough$webPanelCrosshairVelocity > 0.0f)) {
            enough$webPanelCrosshairVelocity = 0.0f;
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
        float morph = enough$webPanelCrosshairMorph;
        morph = morph * morph * (3f - 2f * morph);

        context.getMatrices().push();
        context.getMatrices().scale(fixedScale, fixedScale, 1f);

        float rawProgress = client.player.getAttackCooldownProgress(0.0f);
        float follow = Math.min(1f, 0.18f + tickCounter.getTickDelta(true) * 0.28f);
        enough$smoothCooldown += (rawProgress - enough$smoothCooldown) * follow;
        float progress = enough$smoothCooldown;

        if ("Круг".equals(type)) {
            float radius = 3.35f + (2.25f - 3.35f) * morph;
            float lineWidth = 0.95f + (0.82f - 0.95f) * morph;
            enough$drawCooldownCircle(context, x, y, radius, lineWidth, color, progress);
            context.getMatrices().pop();
            return;
        }

        if ("Орбиз".equals(type)) {
            RenderUtil.drawCrispCircle(context, x, y, 1.35f + (0.72f - 1.35f) * morph, color);
            if (morph > 0.001f) {
                RenderUtil.drawCrispArc(context, x, y, 2.35f, 0.82f, color, morph);
            }
        } else {
            enough$drawCsCrosshair(context, module, x, y, progress, morph);
        }
        context.getMatrices().pop();
    }

    @Unique
    private static void enough$drawCooldownCircle(DrawContext context, float x, float y,
                                                   float radius, float lineWidth, int color, float progress) {
        if (((color >>> 24) & 0xFF) == 0) return;
        if (progress >= 1.0f) {
            RenderUtil.drawCrispCircleOutline(context, x, y, radius, lineWidth, color);
        } else if (progress > 0.001f) {
            RenderUtil.drawCrispArc(context, x, y, radius, lineWidth, color, progress);
        }
    }

    @Unique
    private static void enough$drawCsCrosshair(DrawContext context, Crosshair module,
                                                float centerX, float centerY, float progress, float morph) {
        float gap = module.crossGap.get().floatValue();
        if (module.crossDynamic.get()) {
            gap += (1f - progress) * module.crossDynamicAmount.get().floatValue();
        }
        float targetRadius = 2.35f;
        gap += (targetRadius - gap) * morph;
        float length = module.crossLength.get().floatValue() * (1f - morph);
        float thickness = module.crossThickness.get().floatValue()
                + (0.82f - module.crossThickness.get().floatValue()) * morph;
        float half = thickness * 0.5f;
        boolean outline = module.crossOutline.get();
        float outlineWidth = module.crossOutlineWidth.get().floatValue() * (1f - morph);
        int outlineColor = module.crossOutlineColor.get();
        int color = module.color.get();

        if (length > 0.02f) {
            enough$drawCrossArm(context, centerX - gap - length, centerY - half,
                    length, thickness, outline, outlineWidth, outlineColor, color);
            enough$drawCrossArm(context, centerX + gap, centerY - half,
                    length, thickness, outline, outlineWidth, outlineColor, color);
            if (module.crossTop.get()) {
                enough$drawCrossArm(context, centerX - half, centerY - gap - length,
                        thickness, length, outline, outlineWidth, outlineColor, color);
            }
            enough$drawCrossArm(context, centerX - half, centerY + gap,
                    thickness, length, outline, outlineWidth, outlineColor, color);
        }

        if (module.crossDot.get() && morph < 0.999f) {
            float size = module.crossDotSize.get().floatValue() * (1f - morph);
            enough$drawCrossArm(context, centerX - size * 0.5f, centerY - size * 0.5f,
                    size, size, outline, outlineWidth, outlineColor, color);
        }
        float circleMorph = MathHelper.clamp((morph - 0.06f) / 0.94f, 0f, 1f);
        circleMorph = circleMorph * circleMorph * (3f - 2f * circleMorph);
        if (circleMorph > 0.001f) {
            float sweep = 92.0f * circleMorph;
            for (int i = 0; i < 4; i++) {
                RenderUtil.drawCrispArcSegment(context, centerX, centerY,
                        targetRadius, 0.82f, -91.0f + i * 90.0f, sweep, color);
            }
        }
    }

    @Unique
    private static void enough$drawCrossArm(DrawContext context, float x, float y,
                                             float width, float height, boolean outline,
                                             float outlineWidth, int outlineColor, int color) {
        if (outline && outlineWidth > 0f) {
            RenderUtil.drawRect(context, x - outlineWidth, y - outlineWidth,
                    width + outlineWidth * 2f, height + outlineWidth * 2f, outlineColor);
        }
        RenderUtil.drawRect(context, x, y, width, height, color);
    }
}
