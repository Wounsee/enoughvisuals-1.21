package fun.enoughvisuals.ui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.modules.visual.CustomTheme;
import fun.enoughvisuals.systems.modules.modules.visual.TargetESP;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class TargetHud extends HudElement {

    public final SliderSetting scale = addSetting(new SliderSetting("Масштаб", "scale", 0.9, 0.5, 1.5, 0.05));

    private LivingEntity logicTarget = null;
    private LivingEntity displayTarget = null;
    private long lastTargetTime = 0L;

    private float healthAnim = 0f;
    private float maxHealthAnim = 20f;

    private static final Identifier MOB_ICON = Identifier.of("enough", "icons/hud/whodark.png");
    private static final Identifier BLOOM_PARTICLE = Identifier.of("enough", "textures/bloom.png");

    private final List<HitParticle> hitParticles = new ArrayList<>();
    private final Random random = new Random();
    private UUID trackedTargetId = null;
    private float trackedHealth = -1f;
    private long hitFlashHoldUntilMs = 0L;
    private float hitFlashAnim = 0f;
    private int pendingHitBursts = 0;
    private long lastParticleUpdateMs = 0L;
    private long lastTargetSearchMs = 0L;
    private LivingEntity cachedLookTarget = null;

    public TargetHud(float x, float y) {
        super(x, y, 120f, 34f);
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

        double maxDist = 4.0;
        try {
            maxDist = mc.player.getEntityInteractionRange();
        } catch (Throwable ignored) {
        }

        Vec3d eyes = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = eyes.add(look.multiply(maxDist));

        LivingEntity best = null;
        double bestDist = maxDist * maxDist;

        Box searchBox = mc.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(1.0);
        for (Entity e : mc.world.getOtherEntities(mc.player, searchBox, entity -> entity instanceof LivingEntity)) {
            LivingEntity le = (LivingEntity) e;
            if (!le.isAlive() || le.isRemoved()) continue;

            Box box = le.getBoundingBox().expand(0.3);
            Optional<Vec3d> hit = box.raycast(eyes, end);

            if (hit.isPresent()) {
                double d = eyes.squaredDistanceTo(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = le;
                }
            }
        }

        cachedLookTarget = best;
        if (best != null) {
            setNewTarget(best);
        }
    }

    private void setNewTarget(LivingEntity entity) {
        if (logicTarget != entity) {
            logicTarget = entity;
            displayTarget = entity;

            float resolvedHealth = resolveDisplayHealth(entity);
            healthAnim = resolvedHealth;
            maxHealthAnim = Math.max(entity.getMaxHealth(), resolvedHealth);

            trackedTargetId = entity.getUuid();
            trackedHealth = resolvedHealth;
            hitFlashAnim = 0f;
            hitFlashHoldUntilMs = 0L;
            pendingHitBursts = 0;
            hitParticles.clear();
        }
        lastTargetTime = System.currentTimeMillis();
    }

    @Override
    protected void update(float delta) {
        findTarget();
        float frameDelta = RenderUtil.getFrameDelta();

        long now = System.currentTimeMillis();

        if (logicTarget != null && (!logicTarget.isAlive() || logicTarget.isRemoved())) {
            logicTarget = null;
        }

        if (now - lastTargetTime > 2000L) {
            logicTarget = null;
        }

        if (logicTarget != null) {
            displayTarget = logicTarget;
            setContentVisibleTarget(true);

            float resolvedHealth = resolveDisplayHealth(displayTarget);
            float resolvedMax = Math.max(displayTarget.getMaxHealth(), resolvedHealth);

            healthAnim = RenderUtil.animate(healthAnim, resolvedHealth, 0.2f, frameDelta);
            maxHealthAnim = RenderUtil.animate(maxHealthAnim, resolvedMax, 0.2f, frameDelta);

            trackHitState(displayTarget, resolvedHealth, now);
        } else {
            setContentVisibleTarget(false);
            trackedTargetId = null;
            trackedHealth = -1f;
        }

        updateHitEffects(now);

        float s = scale.get().floatValue();
        float baseW = 120f * s;
        float baseH = 34f * s;

        if (displayTarget != null) {
            float nameW = CustomFontRenderer.getTextWidth(displayTarget.getName().getString(), 7f * s, FontWeight.SEMIBOLD);
            float neededW = (5f + 24f + 6f + 30f + 5f) * s + Math.min(nameW, 72f * s);
            baseW = Math.max(baseW, neededW);
        }

        setWidth(baseW);
        setHeight(baseH);
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        if (displayTarget == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        float s = scale.get().floatValue();
        drawMenuStyleBackground(ctx, x, y, w, h, 6f * s, alpha);

        float padding = 5f * s;
        float headSize = 24f * s;
        float headX = x + padding;
        float headY = y + (h - headSize) * 0.5f;
        float headRadius = 4f * s;

        if (displayTarget instanceof AbstractClientPlayerEntity player) {
            Identifier skin = player.getSkinTextures().texture();
            RenderUtil.drawRoundedTextureShader(ctx, skin, headX, headY, headSize, headSize,
                    headRadius, alpha, 8, 8, 8, 8, 64, 64);
            RenderUtil.drawRoundedTextureShader(ctx, skin, headX, headY, headSize, headSize,
                    headRadius, alpha, 40, 8, 8, 8, 64, 64);
        } else {
            Draw.image(ctx, MOB_ICON, headX, headY, headSize, headSize, alpha);
        }

        float centerX = headX + headSize * 0.5f;
        float centerY = headY + headSize * 0.5f;
        if (pendingHitBursts > 0) {
            for (int i = 0; i < pendingHitBursts; i++) {
                spawnHitParticles(centerX, centerY, s);
            }
            pendingHitBursts = 0;
        }

        renderHitParticles(ctx, alpha);

        if (hitFlashAnim > 0.01f) {
            int redOverlay = RenderUtil.withAlpha(0xFFFF3B30, alpha * hitFlashAnim * 0.42f);
            if (displayTarget instanceof AbstractClientPlayerEntity) {
                RenderUtil.drawRoundedRect(ctx, headX, headY, headSize, headSize, headRadius, redOverlay);
            } else {
                RenderUtil.drawRect(ctx, headX, headY, headSize, headSize, redOverlay);
            }
        }

        float textX = headX + headSize + 6f * s;
        float rightEdge = x + w - padding;

        float hpFontSize = 6.5f * s;
        float heartSize = 9f * s;
        float iconGap = 2f * s;

        float displayHealthValue = Math.max(0f, healthAnim);
        String hpStr = String.format("%.1f", displayHealthValue).replace(',', '.');

        float hpStrW = CustomFontRenderer.getTextWidth(hpStr, hpFontSize, FontWeight.SEMIBOLD);
        float iconW = CustomFontRenderer.getTextWidth("\uF000", heartSize, FontWeight.HEART);
        float topRowY = y + 6f * s;

        float heartX = rightEdge - iconW;
        float hpNumX = heartX - iconGap - hpStrW;

        CustomFontRenderer.drawText(ctx, "\uF000", heartX, topRowY, heartSize, RenderUtil.withAlpha(CustomTheme.hudAccentLight(), alpha), FontWeight.HEART);
        CustomFontRenderer.drawText(ctx, hpStr, hpNumX, topRowY + 1f * s, hpFontSize, RenderUtil.withAlpha(CustomTheme.hudAccentLight(), alpha), FontWeight.SEMIBOLD);

        float nameFontSize = 7f * s;
        float nameMaxX = hpNumX - 4f * s;
        String originalName = displayTarget.getName().getString();
        float nameLimitW = CustomFontRenderer.getTextWidth("abcdefghijkl", nameFontSize, FontWeight.SEMIBOLD);
        float nameMaxW = Math.min(Math.max(8f * s, nameMaxX - textX), nameLimitW);
        int nameColor = RenderUtil.withAlpha(0xFFFFFF, alpha);
        if (originalName.length() > 12 || CustomFontRenderer.getTextWidth(originalName, nameFontSize, FontWeight.SEMIBOLD) > nameMaxW) {
            RenderUtil.drawMarqueeText(ctx, originalName, textX, topRowY + 0.5f * s,
                    nameMaxW, 10f * s, nameFontSize, nameColor, FontWeight.SEMIBOLD, 20f * s);
        } else {
            CustomFontRenderer.drawText(ctx, originalName, textX, topRowY + 0.5f * s, nameFontSize, nameColor, FontWeight.SEMIBOLD);
        }

        float barH = 3f * s;
        float barY = y + h - padding - barH;
        float barX = textX;
        float barW = rightEdge - barX;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 50.0f);

        RenderUtil.drawRoundedRect(ctx, barX, barY, barW, barH, 1.5f * s, RenderUtil.withAlpha(CustomTheme.hudSliderBg(), alpha * 0.7f));

        float safeMax = maxHealthAnim <= 0f ? 1f : maxHealthAnim;
        float hpProgress = Math.max(0f, Math.min(1f, healthAnim / safeMax));
        float fillW = barW * hpProgress;

        if (fillW > 0.5f) {
            ctx.getMatrices().translate(0, 0, 5.0f);
            RenderUtil.drawRoundedRect(ctx, barX, barY, fillW, barH, 1.5f * s, RenderUtil.withAlpha(CustomTheme.hudAccent(), alpha));
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        ctx.getMatrices().pop();
    }

    private void trackHitState(LivingEntity entity, float currentHealth, long now) {
        UUID id = entity.getUuid();
        if (!id.equals(trackedTargetId)) {
            trackedTargetId = id;
            trackedHealth = currentHealth;
            hitFlashAnim = 0f;
            return;
        }

        if (trackedHealth >= 0f && currentHealth + 0.05f < trackedHealth) {
            hitFlashHoldUntilMs = now + 120L;
            pendingHitBursts++;
        }

        trackedHealth = currentHealth;
    }

    private void updateHitEffects(long now) {
        float flashTarget = now < hitFlashHoldUntilMs ? 1f : 0f;
        float speed = flashTarget > hitFlashAnim ? 0.35f : 0.13f;
        hitFlashAnim = RenderUtil.animate(hitFlashAnim, flashTarget, speed, RenderUtil.getFrameDelta());

        updateHitParticles(now);
    }

    private void spawnHitParticles(float centerX, float centerY, float scaleFactor) {
        int count = 9;
        for (int i = 0; i < count; i++) {
            if (hitParticles.size() > 72) {
                hitParticles.remove(0);
            }

            float angle = random.nextFloat() * (float) (Math.PI * 2.0);
            float speed = (0.55f + random.nextFloat() * 0.85f) * scaleFactor;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed - 0.12f * scaleFactor;
            float size = (4.0f + random.nextFloat() * 4.2f) * scaleFactor;
            float life = 14.0f + random.nextFloat() * 11.0f;
            float rot = random.nextFloat() * 360.0f;
            float rotSpeed = (random.nextFloat() - 0.5f) * 18.0f;

            hitParticles.add(new HitParticle(centerX, centerY, vx, vy, size, life, life, rot, rotSpeed));
        }
    }

    private void updateHitParticles(long now) {
        if (lastParticleUpdateMs == 0L) {
            lastParticleUpdateMs = now;
            return;
        }

        float dt = (now - lastParticleUpdateMs) / 16.6667f;
        lastParticleUpdateMs = now;
        if (dt <= 0f) return;

        dt = Math.min(dt, 3.0f);
        Iterator<HitParticle> iterator = hitParticles.iterator();
        while (iterator.hasNext()) {
            HitParticle p = iterator.next();
            p.life -= dt;
            if (p.life <= 0f) {
                iterator.remove();
                continue;
            }

            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 0.03f * dt;
            p.rotation += p.rotationSpeed * dt;
        }
    }

    private void renderHitParticles(DrawContext ctx, float alpha) {
        if (hitParticles.isEmpty()) return;

        for (HitParticle p : hitParticles) {
            float lifeRatio = p.life / p.maxLife;
            float fade = lifeRatio * lifeRatio;
            float particleAlpha = alpha * fade * 0.92f;
            if (particleAlpha <= 0.01f) continue;

            float progress = 1f - lifeRatio;
            float size = p.size * (0.9f + progress * 0.45f);
            float drawX = p.x - size * 0.5f;
            float drawY = p.y - size * 0.5f;
            float redGlowRadius = Math.max(1.0f, size * 0.34f);
            int redGlowColor = RenderUtil.withAlpha(0xFFFF2E2E, particleAlpha * 0.45f);

            ctx.getMatrices().push();
            ctx.getMatrices().translate(p.x, p.y, 0f);
            ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(p.rotation)));
            ctx.getMatrices().translate(-p.x, -p.y, 0f);
            Draw.image(ctx, BLOOM_PARTICLE, drawX, drawY, size, size, particleAlpha);
            ctx.getMatrices().pop();

            RenderUtil.drawCircle(ctx, p.x, p.y, redGlowRadius, redGlowColor);
            int coreColor = RenderUtil.withAlpha(0xFFFF1F1F, particleAlpha * 0.62f);
            RenderUtil.drawCircle(ctx, p.x, p.y, Math.max(0.6f, size * 0.16f), coreColor);
        }
    }

    private float resolveDisplayHealth(LivingEntity entity) {
        float vanillaHealth = Math.max(0f, entity.getHealth() + entity.getAbsorptionAmount());
        if (!(entity instanceof PlayerEntity player)) {
            return vanillaHealth;
        }

        Float scoreboardHealth = readScoreboardHealth(player);
        return scoreboardHealth != null && scoreboardHealth > 0f ? scoreboardHealth : vanillaHealth;
    }

    private Float readScoreboardHealth(PlayerEntity player) {
        try {
            Scoreboard scoreboard = player.getScoreboard();
            if (scoreboard == null) return null;

            ScoreboardObjective belowName = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            return readScoreValue(scoreboard, player, belowName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Float readScoreValue(Scoreboard scoreboard, PlayerEntity player, ScoreboardObjective objective) {
        if (objective == null) return null;

        ReadableScoreboardScore score = scoreboard.getScore((ScoreHolder) player, objective);
        if (score == null) return null;

        NumberFormat format = objective.getNumberFormatOr(StyledNumberFormat.EMPTY);
        String raw = ReadableScoreboardScore.getFormattedScore(score, format).getString();
        if (raw == null || raw.isBlank()) return null;

        String digits = raw.replace(',', '.').replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) return null;

        try {
            return Float.parseFloat(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final class HitParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float life;
        final float maxLife;
        float rotation;
        final float rotationSpeed;

        private HitParticle(float x, float y, float vx, float vy, float size, float life, float maxLife, float rotation, float rotationSpeed) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = life;
            this.maxLife = maxLife;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
        }
    }
}
