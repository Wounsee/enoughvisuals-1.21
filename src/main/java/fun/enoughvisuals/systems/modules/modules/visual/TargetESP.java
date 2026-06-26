package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.CrystalRenderer;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Optional;

@Register(
        name = "Target ESP",
        id = "target_esp",
        description = "Highlights the current target",
        category = Category.VISUAL
)
public class TargetESP extends Module {
    private static final String MODE_CRYSTALS = "crystals";
    private static final String MODE_SQUARE_1 = "square_1";
    private static final String MODE_SQUARE_2 = "square_2";
    private static final Identifier SQUARE_1 = Identifier.of("enough", "textures/targetesp/target.png");
    private static final Identifier SQUARE_2 = Identifier.of("enough", "textures/targetesp/target2.png");
    private static TargetESP INSTANCE;

    public static LivingEntity globalTarget = null;

    public final ModeSetting style = add(new ModeSetting(
            "Style", "style", "Target ESP style", MODE_CRYSTALS, MODE_CRYSTALS, MODE_SQUARE_1, MODE_SQUARE_2
    ));
    public final ColorSetting colorA = add(new ColorSetting(
            "Color 1", "color_a", "First crystal color", 0xFF8A5CFF
    ));
    public final ColorSetting colorB = add(new ColorSetting(
            "Color 2", "color_b", "Second crystal color", 0xFF5CA0FF
    ));
    public final ColorSetting squareColor = add(new ColorSetting(
            "Square Color", "square_color", "Square texture color", 0xFFFFFFFF
    ));
    public final SliderSetting squareSize = add(new SliderSetting(
            "Square Size", "square_size", "Square texture size", 0.85, 0.35, 1.60, 0.05, "x"
    ));
    public final SliderSetting attackRange = add(new SliderSetting(
            "Attack Range", "attack_range", "Real target highlight distance", 3.0, 2.0, 6.0, 0.05, "b"
    ));

    private LivingEntity displayTarget = null;
    private float displayAlpha = 0f;
    private LivingEntity prevTarget = null;
    private float prevAlpha = 0f;
    private LivingEntity logicTarget = null;
    private long lastTargetTime = 0L;
    private long hitFlashUntilMs = 0L;
    private float hitFlash = 0f;

    public TargetESP() {
        INSTANCE = this;
        colorA.visibleWhen(() -> style.is(MODE_CRYSTALS));
        colorB.visibleWhen(() -> style.is(MODE_CRYSTALS));
        squareColor.visibleWhen(() -> !style.is(MODE_CRYSTALS));
        squareSize.visibleWhen(() -> !style.is(MODE_CRYSTALS));
    }

    @Override
    public void onDisable() {
        displayTarget = null;
        prevTarget = null;
        displayAlpha = 0f;
        prevAlpha = 0f;
        logicTarget = null;
        globalTarget = null;
        lastTargetTime = 0L;
        hitFlashUntilMs = 0L;
        hitFlash = 0f;
    }

    @Override
    public void onTick() {
        LivingEntity found = findTarget();
        long now = System.currentTimeMillis();

        if (found != null) {
            if (found != logicTarget) {
                if (displayTarget != null && displayAlpha > 0.01f) {
                    prevTarget = displayTarget;
                    prevAlpha = displayAlpha;
                }

                logicTarget = found;
                displayTarget = found;
                displayAlpha = 0f;
            }

            lastTargetTime = now;
        } else if (now - lastTargetTime > 2000L) {
            if (logicTarget != null && displayTarget != null) {
                prevTarget = displayTarget;
                prevAlpha = displayAlpha;
            }

            logicTarget = null;
            displayTarget = null;
            displayAlpha = 0f;
        }

        globalTarget = displayAlpha > 0f ? displayTarget : null;
    }

    public static void renderInWorld(WorldRenderContext ctx) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.world == null || mc.player == null) return;
        INSTANCE.doRender(ctx);
    }

    public static void onAttack(Entity target) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !(target instanceof LivingEntity living)) return;
        if (living == INSTANCE.logicTarget || living == INSTANCE.displayTarget) {
            INSTANCE.hitFlashUntilMs = System.currentTimeMillis() + 340L;
            INSTANCE.hitFlash = Math.max(INSTANCE.hitFlash, 0.65f);
        }
    }

    private void doRender(WorldRenderContext ctx) {
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;

        float frameDelta = RenderUtil.getFrameDelta();
        float targetAlpha = logicTarget != null ? 1f : 0f;
        displayAlpha = RenderUtil.animate(displayAlpha, targetAlpha, 0.15f, frameDelta);

        if (prevTarget != null) {
            prevAlpha = RenderUtil.animate(prevAlpha, 0f, 0.15f, frameDelta);
            if (prevAlpha <= 0.01f) {
                prevTarget = null;
            }
        }

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        float targetFlash = System.currentTimeMillis() < hitFlashUntilMs ? 1f : 0f;
        hitFlash = RenderUtil.animate(hitFlash, targetFlash, 0.22f, frameDelta);

        if (style.is(MODE_CRYSTALS)) {
            int cA = colorA.get() & 0x00FFFFFF;
            int cB = colorB.get() & 0x00FFFFFF;
            if (isRenderable(prevTarget, prevAlpha)) {
                CrystalRenderer.render(ms, prevTarget, prevAlpha, tickDelta, cA, cB, 0.85f, 4);
            }
            if (isRenderable(displayTarget, displayAlpha)) {
                CrystalRenderer.render(ms, displayTarget, displayAlpha, tickDelta, cA, cB, 0.85f, 4);
            }
        } else {
            Identifier texture = style.is(MODE_SQUARE_2) ? SQUARE_2 : SQUARE_1;
            if (isRenderable(prevTarget, prevAlpha)) {
                renderSquare(ctx, prevTarget, texture, prevAlpha, tickDelta, 0f);
            }
            if (isRenderable(displayTarget, displayAlpha)) {
                renderSquare(ctx, displayTarget, texture, displayAlpha, tickDelta, hitFlash);
            }
        }
    }

    private boolean isRenderable(LivingEntity entity, float alpha) {
        return entity != null && alpha > 0.01f && entity.isAlive() && !entity.isRemoved();
    }

    private void renderSquare(WorldRenderContext ctx, LivingEntity entity, Identifier texture,
                              float alpha, float tickDelta, float flash) {
        MatrixStack matrices = ctx.matrixStack();
        Camera camera = ctx.camera();
        if (matrices == null || camera == null || alpha <= 0.01f) return;

        Vec3d cameraPos = camera.getPos();
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) + entity.getHeight() * 0.52 - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cameraPos.z;
        float size = Math.max(entity.getWidth() * 1.45f, entity.getHeight() * 0.82f) * squareSize.get().floatValue();
        float half = size * 0.5f;
        float rotation = (System.currentTimeMillis() % 3600L) * 0.10f;

        int color = squareColor.get();
        float redMix = Math.max(0f, Math.min(1f, flash));
        float baseR = ((color >> 16) & 0xFF) / 255f;
        float baseG = ((color >> 8) & 0xFF) / 255f;
        float baseB = (color & 0xFF) / 255f;
        float baseA = ((color >> 24) & 0xFF) / 255f;
        float r = MathHelper.lerp(redMix, baseR, 1f);
        float g = MathHelper.lerp(redMix, baseG, 0.12f);
        float b = MathHelper.lerp(redMix, baseB, 0.12f);
        float a = Math.max(0f, Math.min(1f, alpha)) * 0.92f * baseA;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0f).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, half, -half, 0f).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, half, half, 0f).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -half, half, 0f).texture(0f, 0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private LivingEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        return raycastTarget(effectiveAttackRange());
    }

    public static double effectiveAttackRange() {
        double configured = INSTANCE == null ? 3.0 : INSTANCE.attackRange.get().doubleValue();
        double vanilla = 3.0;
        try {
            if (mc.player != null) {
                vanilla = mc.player.getEntityInteractionRange();
            }
        } catch (Throwable ignored) {
        }
        return Math.max(1.0, Math.min(configured, vanilla));
    }

    private LivingEntity raycastTarget(double maxDist) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d eyes = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = eyes.add(look.multiply(maxDist));

        LivingEntity best = null;
        double bestDist = maxDist * maxDist;

        Box searchBox = mc.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(1.2);
        for (Entity entity : mc.world.getOtherEntities(mc.player, searchBox, e -> e instanceof LivingEntity)) {
            LivingEntity targetEntity = (LivingEntity) entity;
            if (!targetEntity.isAlive() || targetEntity.isRemoved()) continue;
            if (targetEntity.isInvisible() && !isWearingFullHighTierArmor(targetEntity)) continue;

            Optional<Vec3d> hit = targetEntity.getBoundingBox().expand(0.05).raycast(eyes, end);
            if (hit.isPresent()) {
                double distance = eyes.squaredDistanceTo(hit.get());
                if (distance < bestDist) {
                    bestDist = distance;
                    best = targetEntity;
                }
            }
        }

        return best;
    }

    private boolean isWearingFullHighTierArmor(LivingEntity entity) {
        int count = 0;
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack == null || stack.isEmpty()) return false;

            String name = stack.getItem().getTranslationKey().toLowerCase();
            if (!name.contains("diamond") && !name.contains("netherite")) {
                return false;
            }
            count++;
        }
        return count == 4;
    }
}
