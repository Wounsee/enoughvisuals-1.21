package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import fun.enoughvisuals.systems.modules.modules.visual.TargetESP;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Figma Main/Target HUD. The six cells are main hand, offhand and four armor slots. */
public final class TargetHud extends HudElement {
    private static final Identifier MOB_ICON = Identifier.of("enough", "icons/hud/whodark.png");
    private static final float BASE_WIDTH = 148f;
    private static final float BASE_HEIGHT = 37f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());

    private LivingEntity logicTarget;
    private LivingEntity displayTarget;
    private LivingEntity cachedLookTarget;
    private long lastTargetTime;
    private long lastTargetSearchMs;
    private float healthAnim;
    private float maxHealthAnim = 20f;
    private float nameWidth = 36f;

    public TargetHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        findTarget();
        long now = System.currentTimeMillis();
        if (logicTarget != null && (!logicTarget.isAlive() || logicTarget.isRemoved())) logicTarget = null;
        if (now - lastTargetTime > 2_000L) logicTarget = null;

        if (logicTarget != null) {
            displayTarget = logicTarget;
            setContentVisibleTarget(true);
            float health = resolveDisplayHealth(displayTarget);
            float max = Math.max(displayTarget.getMaxHealth(), health);
            float frame = RenderUtil.getFrameDelta();
            healthAnim = RenderUtil.animate(healthAnim, health, 0.18f, frame);
            maxHealthAnim = RenderUtil.animate(maxHealthAnim, max, 0.18f, frame);
        } else {
            setContentVisibleTarget(false);
        }

        float s = HudDesign.scale(scale, 0.90f);
        String name = protectedName(displayTarget);
        int count = name.codePointCount(0, name.length());
        String visibleName = count > 12 ? name.substring(0, name.offsetByCodePoints(0, 12)) : name;
        nameWidth = Math.max(36f, CustomFontRenderer.getTextWidth(visibleName, 7f, FontWeight.SEMIBOLD));
        setWidth((BASE_WIDTH + nameWidth - 36f) * s);
        setHeight(BASE_HEIGHT * s);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        if (displayTarget == null) return;
        float s = HudDesign.scale(scale, 0.90f);
        float u = s / 1.8f;
        HudDesign.drawRounderPanel(ctx, x, y, w, h, 23f * u, alpha);

        float headX = x + 8.5f * s;
        float headY = y + 5.5f * s;
        float headSize = 26f * s;
        float headRadius = 8f * s;

        float hpProgress = maxHealthAnim <= 0f ? 0f : clamp01(healthAnim / maxHealthAnim);
        float imageX = headX + 0.85f * s;
        float imageY = headY + 0.85f * s;
        float imageSize = headSize - 1.7f * s;
        float imageRadius = headRadius - 0.5f * s;
        float previousExponent = RenderUtil.pushSquircleExponent(HudDesign.rounderHudExponent());
        try {
            if (displayTarget instanceof AbstractClientPlayerEntity player) {
                Draw.imagePlayerHeadRounded(ctx, player.getSkinTextures().texture(),
                        imageX, imageY, imageSize, imageRadius, alpha);
            } else {
                RenderUtil.drawRoundedTextureShader(ctx, MOB_ICON,
                        imageX, imageY, imageSize, imageSize, imageRadius, alpha,
                        0f, 0f, 1f, 1f, 1f, 1f);
            }

            RenderUtil.drawCrispSquircleProgressOutline(ctx, headX, headY, headSize, headSize, headRadius,
                    1.75f * s, 1f, RenderUtil.withAlpha(HudDesign.ROW_MARK, alpha));
            RenderUtil.drawCrispSquircleProgressOutline(ctx, headX, headY, headSize, headSize, headRadius,
                    1.75f * s, hpProgress, RenderUtil.withAlpha(HudDesign.accent(), alpha));
        } finally {
            RenderUtil.restoreSquircleExponent(previousExponent);
        }

        drawVerticalDivider(ctx, x + 41f * s, y + 6f * s, 25f * s, alpha);
        drawIdentity(ctx, x + 48.5f * s, y, s, alpha);
        drawVerticalDivider(ctx, x + (95f + nameWidth - 36f) * s, y + 6f * s, 25f * s, alpha);
        drawEquipment(ctx, x + (102.5f + nameWidth - 36f) * s, y + 10.5f * s, s, alpha);
    }

    private void drawIdentity(DrawContext ctx, float x, float panelY, float s, float alpha) {
        float iconSize = 8f * s;
        float textSize = 7f * s;
        int dim = RenderUtil.withAlpha(HudDesign.TEXT_DIM, alpha);
        int text = RenderUtil.withAlpha(HudDesign.TEXT, alpha);
        CustomFontRenderer.drawText(ctx, HudIcons.USER, x, panelY + 7.5f * s,
                iconSize, dim, FontWeight.HUD_ICONS);
        String name = protectedName(displayTarget);
        float nameX = x + 10f * s;
        if (name.codePointCount(0, name.length()) > 12) {
            RenderUtil.drawMarqueeText(ctx, name, nameX, panelY + 7.25f * s,
                    nameWidth * s, 8f * s, textSize, text, FontWeight.SEMIBOLD, 14f * s);
        } else {
            CustomFontRenderer.drawText(ctx, name, nameX, panelY + 7.25f * s,
                    textSize, text, FontWeight.SEMIBOLD);
        }

        CustomFontRenderer.drawText(ctx, HudIcons.HEART, x, panelY + 20.5f * s,
                iconSize, RenderUtil.withAlpha(HudDesign.accent(), alpha), FontWeight.HUD_ICONS);
        String hp = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0f, healthAnim));
        float hpY = panelY + 20.25f * s;
        CustomFontRenderer.drawText(ctx, hp, nameX, hpY, textSize, text, FontWeight.SEMIBOLD);
        float hpW = CustomFontRenderer.getTextWidth(hp, textSize, FontWeight.SEMIBOLD);
        CustomFontRenderer.drawText(ctx, "HP", nameX + hpW + 1f * s, hpY,
                textSize, RenderUtil.withAlpha(HudDesign.accent(), alpha), FontWeight.SEMIBOLD);
    }

    private void drawEquipment(DrawContext ctx, float x, float y, float s, float alpha) {
        List<ItemStack> slots = new ArrayList<>(6);
        slots.add(displayTarget.getMainHandStack());
        slots.add(displayTarget.getOffHandStack());
        List<ItemStack> armor = new ArrayList<>();
        displayTarget.getArmorItems().forEach(armor::add);
        Collections.reverse(armor); // helmet, chestplate, leggings, boots
        while (armor.size() < 4) armor.add(ItemStack.EMPTY);
        slots.addAll(armor.subList(0, 4));

        for (int i = 0; i < slots.size(); i++) {
            int column = i < 2 ? i : i - 2;
            int row = i < 2 ? 0 : 1;
            float cellX = x + column * 9.5f * s;
            float cellY = y + row * 10f * s;
            ItemStack stack = slots.get(i);
            if (stack == null || stack.isEmpty()) {
                CustomFontRenderer.drawText(ctx, HudIcons.CLOSE, cellX, cellY,
                        8f * s, RenderUtil.withAlpha(0xFF4D4D52, alpha), FontWeight.HUD_ICONS);
            } else {
                drawItem(ctx, stack, cellX, cellY, 8f * s, alpha);
            }
        }
    }

    private static void drawItem(DrawContext ctx, ItemStack stack, float x, float y,
                                 float size, float alpha) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 70f);
        ctx.getMatrices().scale(size / 16f, size / 16f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawItem(stack, 0, 0);
        ctx.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        ctx.getMatrices().pop();
    }

    private static void drawVerticalDivider(DrawContext ctx, float x, float y, float height, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x, y, 1f, height, 0.5f,
                RenderUtil.withAlpha(HudDesign.DIVIDER, alpha));
    }

    private void findTarget() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (mc.currentScreen instanceof ChatScreen) {
            setNewTarget(mc.player);
            return;
        }
        if (mc.player == null || mc.world == null) return;
        if (TargetESP.globalTarget != null) {
            setNewTarget(TargetESP.globalTarget);
            return;
        }
        if (now - lastTargetSearchMs < 60L) {
            if (cachedLookTarget != null && cachedLookTarget.isAlive() && !cachedLookTarget.isRemoved()) {
                setNewTarget(cachedLookTarget);
            }
            return;
        }
        lastTargetSearchMs = now;
        double maxDist = TargetESP.effectiveAttackRange();
        Vec3d eyes = mc.player.getCameraPosVec(1f);
        Vec3d look = mc.player.getRotationVec(1f);
        Vec3d end = eyes.add(look.multiply(maxDist));
        LivingEntity best = null;
        double bestDistance = maxDist * maxDist;
        Box search = mc.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(1.0);
        for (Entity entity : mc.world.getOtherEntities(mc.player, search,
                candidate -> candidate instanceof LivingEntity)) {
            LivingEntity living = (LivingEntity) entity;
            if (!living.isAlive() || living.isRemoved()) continue;
            Optional<Vec3d> hit = living.getBoundingBox().expand(0.05).raycast(eyes, end);
            if (hit.isPresent()) {
                double distance = eyes.squaredDistanceTo(hit.get());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = living;
                }
            }
        }
        cachedLookTarget = best;
        if (best != null) setNewTarget(best);
    }

    private void setNewTarget(LivingEntity entity) {
        if (entity == null) return;
        if (logicTarget != entity) {
            logicTarget = entity;
            displayTarget = entity;
            healthAnim = resolveDisplayHealth(entity);
            maxHealthAnim = Math.max(entity.getMaxHealth(), healthAnim);
        }
        lastTargetTime = System.currentTimeMillis();
    }

    private static String protectedName(LivingEntity entity) {
        return entity == null || entity.getName() == null ? "" : NameProtect.protect(entity.getName().getString());
    }

    private float resolveDisplayHealth(LivingEntity entity) {
        float vanilla = Math.max(0f, entity.getHealth() + entity.getAbsorptionAmount());
        if (!(entity instanceof PlayerEntity player)) return vanilla;
        Float scoreboard = readScoreboardHealth(player);
        return scoreboard != null && scoreboard > 0f ? scoreboard : vanilla;
    }

    private Float readScoreboardHealth(PlayerEntity player) {
        try {
            Scoreboard scoreboard = player.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective == null) return null;
            ReadableScoreboardScore score = scoreboard.getScore((ScoreHolder) player, objective);
            if (score == null) return null;
            NumberFormat format = objective.getNumberFormatOr(StyledNumberFormat.EMPTY);
            String raw = ReadableScoreboardScore.getFormattedScore(score, format).getString();
            String digits = raw.replace(',', '.').replaceAll("[^0-9.]", "");
            return digits.isEmpty() ? null : Float.parseFloat(digits);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
