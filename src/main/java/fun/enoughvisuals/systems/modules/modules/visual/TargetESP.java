package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
    private static final String MODE_CUBE = "cube";
    private static final Identifier SQUARE_1 = Identifier.of("enough", "textures/targetesp/target.png");
    private static final Identifier SQUARE_2 = Identifier.of("enough", "textures/targetesp/target2.png");
    private static final Identifier BLOOM_TEXTURE = Identifier.of("enough", "textures/bloom.png");
    private static final float CUBE_HALF_SIZE = 0.3f;
    private static final float CUBE_TURN_SPEED = 0.045f;
    private static final int MAX_PENDING_CUBE_TURNS = 8;
    private static TargetESP INSTANCE;

    public static LivingEntity globalTarget = null;

    public final ModeSetting style = add(new ModeSetting(
            "Style", "style", "Target ESP style", MODE_CRYSTALS, MODE_CRYSTALS, MODE_SQUARE_1, MODE_SQUARE_2, MODE_CUBE
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
    private float cubeRotationX = 0f;
    private float cubeRotationY = 0f;
    private float cubeRotationZ = 0f;
    private float cubeStartX = 0f;
    private float cubeStartY = 0f;
    private float cubeStartZ = 0f;
    private float cubeTargetX = 0f;
    private float cubeTargetY = 0f;
    private float cubeTargetZ = 0f;
    private float cubeTurnProgress = 1f;
    private int cubeNextAxis = 0;
    private int pendingCubeTurns = 0;
    private final Random random = new Random();
    private final List<CubeShard> cubeShards = new ArrayList<>();

    public TargetESP() {
        INSTANCE = this;
        colorA.visibleWhen(() -> style.is(MODE_CRYSTALS));
        colorB.visibleWhen(() -> style.is(MODE_CRYSTALS));
        squareColor.visibleWhen(() -> !style.is(MODE_CRYSTALS));
        squareSize.visibleWhen(() -> style.is(MODE_SQUARE_1) || style.is(MODE_SQUARE_2));
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
        cubeTurnProgress = 1f;
        cubeNextAxis = 0;
        pendingCubeTurns = 0;
        cubeShards.clear();
    }

    @Override
    public void onTick() {
        if (logicTarget != null && (!logicTarget.isAlive() || logicTarget.isRemoved())) {
            if (style.is(MODE_CUBE)) {
                spawnCubeShatter(logicTarget);
            }
            logicTarget = null;
            displayTarget = null;
            displayAlpha = 0f;
            globalTarget = null;
            lastTargetTime = 0L;
            return;
        }

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
            INSTANCE.kickCubeSpin();
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
        updateCubeRotation(frameDelta);
        if (style.is(MODE_CUBE)) {
            updateCubeShards(frameDelta);
        }

        if (style.is(MODE_CRYSTALS)) {
            int cA = colorA.get() & 0x00FFFFFF;
            int cB = colorB.get() & 0x00FFFFFF;
            if (isRenderable(prevTarget, prevAlpha)) {
                CrystalRenderer.render(ms, prevTarget, prevAlpha, tickDelta, cA, cB, 0.85f, 4);
            }
            if (isRenderable(displayTarget, displayAlpha)) {
                CrystalRenderer.render(ms, displayTarget, displayAlpha, tickDelta, cA, cB, 0.85f, 4);
            }
        } else if (style.is(MODE_CUBE)) {
            int cubeColor = squareColor.get();
            if (isRenderable(prevTarget, prevAlpha)) {
                renderTargetCube(ctx, prevTarget, prevAlpha, tickDelta, 0f, cubeColor);
            }
            if (isRenderable(displayTarget, displayAlpha)) {
                renderTargetCube(ctx, displayTarget, displayAlpha, tickDelta, hitFlash, cubeColor);
            }
            renderCubeShards(ctx, cubeColor);
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

    private void kickCubeSpin() {
        pendingCubeTurns = Math.min(MAX_PENDING_CUBE_TURNS, pendingCubeTurns + 1);
        if (cubeTurnProgress >= 1f) {
            startNextCubeTurn();
        }
    }

    private void startNextCubeTurn() {
        if (pendingCubeTurns <= 0) return;
        pendingCubeTurns--;

        cubeStartX = cubeRotationX;
        cubeStartY = cubeRotationY;
        cubeStartZ = cubeRotationZ;
        cubeTargetX = cubeRotationX;
        cubeTargetY = cubeRotationY;
        cubeTargetZ = cubeRotationZ;

        int axisMask = nextCubeAxisMask();
        float turn = Integer.bitCount(axisMask) == 1 ? 180f : 90f;
        if ((axisMask & 1) != 0) {
            cubeTargetX += turn;
        }
        if ((axisMask & 2) != 0) {
            cubeTargetY += turn;
        }
        if ((axisMask & 4) != 0) {
            cubeTargetZ += turn;
        }
        cubeTurnProgress = 0f;
    }

    private int nextCubeAxisMask() {
        int singleAxis = 1 << cubeNextAxis;
        cubeNextAxis = (cubeNextAxis + 1) % 3;

        float roll = random.nextFloat();
        if (roll < 0.45f) {
            return singleAxis;
        }
        if (roll < 0.63f) {
            return 1 | 2;
        }
        if (roll < 0.81f) {
            return 2 | 4;
        }
        return 1 | 4;
    }

    private void updateCubeRotation(float frameDelta) {
        if (cubeTurnProgress >= 1f) {
            startNextCubeTurn();
            return;
        }

        cubeTurnProgress = Math.min(1f, cubeTurnProgress + CUBE_TURN_SPEED * MathHelper.clamp(frameDelta, 0.25f, 3.0f));
        float eased = RenderUtil.easeOutCubic(cubeTurnProgress);
        cubeRotationX = MathHelper.lerp(eased, cubeStartX, cubeTargetX);
        cubeRotationY = MathHelper.lerp(eased, cubeStartY, cubeTargetY);
        cubeRotationZ = MathHelper.lerp(eased, cubeStartZ, cubeTargetZ);

        if (cubeTurnProgress >= 1f) {
            cubeRotationX = wrapDegrees(cubeTargetX);
            cubeRotationY = wrapDegrees(cubeTargetY);
            cubeRotationZ = wrapDegrees(cubeTargetZ);
            startNextCubeTurn();
        }
    }

    private float wrapDegrees(float value) {
        value %= 360f;
        return value < 0f ? value + 360f : value;
    }

    private void renderTargetCube(WorldRenderContext ctx, LivingEntity entity, float alpha, float tickDelta, float flash, int color) {
        MatrixStack matrices = ctx.matrixStack();
        Camera camera = ctx.camera();
        if (matrices == null || camera == null || alpha <= 0.01f) return;

        Vec3d cameraPos = camera.getPos();
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) + entity.getHeight() * 0.52 - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cameraPos.z;

        float redMix = MathHelper.clamp(flash, 0f, 1f);
        float half = CUBE_HALF_SIZE;
        float corner = half * 0.42f;
        float thickness = Math.max(0.03f, half * 0.07f) * (1.0f + redMix * 0.16f);
        float baseR = ((color >> 16) & 0xFF) / 255f;
        float baseG = ((color >> 8) & 0xFF) / 255f;
        float baseB = (color & 0xFF) / 255f;
        float baseA = ((color >> 24) & 0xFF) / 255f;
        float r = MathHelper.lerp(redMix, MathHelper.lerp(0.34f, baseR, 0.74f), 1f);
        float g = MathHelper.lerp(redMix, MathHelper.lerp(0.34f, baseG, 0.54f), 0.20f);
        float b = MathHelper.lerp(redMix, MathHelper.lerp(0.34f, baseB, 1.0f), 0.26f);
        float a = MathHelper.clamp(alpha, 0f, 1f) * baseA;
        float[] edge = new float[]{Math.min(1f, r * 1.55f), Math.min(1f, g * 1.55f), Math.min(1f, b * 1.55f), a * 0.92f};

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        renderCubeGlowBars(matrices, camera, x, y, z, half, corner, r, g, b, a, redMix);

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cubeRotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cubeRotationY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cubeRotationZ));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder bars = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        cubeCornerBars(bars, matrix, -half, -half, -half, half, half, half, corner, thickness, edge);
        BufferRenderer.drawWithGlobalProgram(bars.end());

        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderCubeGlowBars(MatrixStack matrices, Camera camera,
                                    double centerX, double centerY, double centerZ,
                                    float half, float length, float r, float g, float b, float alpha, float flash) {
        for (int ix = -1; ix <= 1; ix += 2) {
            float x = ix * half;
            float xInner = x - ix * length;
            for (int iy = -1; iy <= 1; iy += 2) {
                float y = iy * half;
                float yInner = y - iy * length;
                for (int iz = -1; iz <= 1; iz += 2) {
                    float z = iz * half;
                    float zInner = z - iz * length;
                    renderBloomSegment(matrices, camera, centerX, centerY, centerZ, x, y, z, xInner, y, z,
                            cubeRotationX, cubeRotationY, cubeRotationZ, half * (0.20f + flash * 0.04f), r, g, b, alpha * (0.16f + flash * 0.08f), true);
                    renderBloomSegment(matrices, camera, centerX, centerY, centerZ, x, y, z, x, yInner, z,
                            cubeRotationX, cubeRotationY, cubeRotationZ, half * (0.20f + flash * 0.04f), r, g, b, alpha * (0.16f + flash * 0.08f), true);
                    renderBloomSegment(matrices, camera, centerX, centerY, centerZ, x, y, z, x, y, zInner,
                            cubeRotationX, cubeRotationY, cubeRotationZ, half * (0.20f + flash * 0.04f), r, g, b, alpha * (0.16f + flash * 0.08f), true);
                }
            }
        }
    }

    private void renderBloomSegment(MatrixStack matrices, Camera camera,
                                    double centerX, double centerY, double centerZ,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float rotationX, float rotationY, float rotationZ,
                                    float size, float r, float g, float b, float alpha, boolean hotEnds) {
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            float t = steps == 1 ? 0f : i / (float) (steps - 1);
            float lx = MathHelper.lerp(t, x1, x2);
            float ly = MathHelper.lerp(t, y1, y2);
            float lz = MathHelper.lerp(t, z1, z2);
            Vec3d rotated = rotatePoint(lx, ly, lz, rotationX, rotationY, rotationZ);
            float endBoost = hotEnds && (i == 0 || i == steps - 1) ? 1.18f : 1.0f;

            renderCubeGlowPoint(matrices, camera, centerX + rotated.x, centerY + rotated.y, centerZ + rotated.z,
                    size * endBoost, r, g, b, alpha * endBoost);
        }
    }

    private Vec3d rotateCubePoint(float x, float y, float z) {
        return rotatePoint(x, y, z, cubeRotationX, cubeRotationY, cubeRotationZ);
    }

    private Vec3d rotatePoint(float x, float y, float z, float rotationX, float rotationY, float rotationZ) {
        double rx = Math.toRadians(rotationX);
        double ry = Math.toRadians(rotationY);
        double rz = Math.toRadians(rotationZ);

        double cos = Math.cos(rz);
        double sin = Math.sin(rz);
        double px = x * cos - y * sin;
        double py = x * sin + y * cos;
        double pz = z;

        cos = Math.cos(ry);
        sin = Math.sin(ry);
        double px2 = px * cos + pz * sin;
        double pz2 = -px * sin + pz * cos;

        cos = Math.cos(rx);
        sin = Math.sin(rx);
        double py3 = py * cos - pz2 * sin;
        double pz3 = py * sin + pz2 * cos;

        return new Vec3d(px2, py3, pz3);
    }

    private void renderCubeGlowPoint(MatrixStack matrices, Camera camera, double x, double y, double z,
                                     float size, float r, float g, float b, float alpha) {
        if (alpha <= 0.004f) return;

        float h = size * 0.5f;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -h, -h, 0f).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, -h, 0f).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, h, 0f).texture(1f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, -h, h, 0f).texture(0f, 0f).color(r, g, b, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private void spawnCubeShatter(LivingEntity entity) {
        if (entity == null || mc.world == null) return;

        Vec3d center = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.52, entity.getZ());
        int rgb = squareColor.get() & 0x00FFFFFF;
        float half = CUBE_HALF_SIZE;
        float length = half * 0.42f;
        float thickness = Math.max(0.03f, half * 0.07f);

        cubeShards.clear();
        for (int ix = -1; ix <= 1; ix += 2) {
            for (int iy = -1; iy <= 1; iy += 2) {
                for (int iz = -1; iz <= 1; iz += 2) {
                    for (int axis = 0; axis < 3; axis++) {
                        float lx = ix * half;
                        float ly = iy * half;
                        float lz = iz * half;
                        if (axis == 0) lx -= ix * length * 0.5f;
                        if (axis == 1) ly -= iy * length * 0.5f;
                        if (axis == 2) lz -= iz * length * 0.5f;

                        Vec3d local = rotateCubePoint(lx, ly, lz);
                        Vec3d outward = local.lengthSquared() > 1.0E-5 ? local.normalize() : new Vec3d(ix, iy, iz).normalize();
                        Vec3d velocity = outward.multiply(1.25 + random.nextDouble() * 0.62)
                                .add((random.nextDouble() - 0.5) * 0.28, 0.36 + random.nextDouble() * 0.42, (random.nextDouble() - 0.5) * 0.28);
                        cubeShards.add(new CubeShard(
                                center.add(local),
                                velocity,
                                5.0f,
                                length,
                                thickness,
                                axis,
                                cubeRotationX,
                                cubeRotationY,
                                cubeRotationZ,
                                randomSpin(210f, 430f),
                                randomSpin(210f, 430f),
                                randomSpin(210f, 430f),
                                rgb
                        ));
                    }
                }
            }
        }
    }

    private float randomSpin(float min, float max) {
        float value = min + random.nextFloat() * (max - min);
        return random.nextBoolean() ? value : -value;
    }

    private void updateCubeShards(float frameDelta) {
        if (cubeShards.isEmpty()) return;
        float dt = MathHelper.clamp(frameDelta, 0.25f, 3.0f) / 60.0f;

        Iterator<CubeShard> iterator = cubeShards.iterator();
        while (iterator.hasNext()) {
            CubeShard shard = iterator.next();
            shard.age += dt;
            if (shard.age >= shard.life) {
                iterator.remove();
                continue;
            }

            shard.prevPos = shard.pos;
            shard.velocity = shard.velocity.add(0.0, -4.1 * dt, 0.0).multiply(Math.pow(0.985, frameDelta));
            shard.rotationX = wrapDegrees(shard.rotationX + shard.spinX * dt);
            shard.rotationY = wrapDegrees(shard.rotationY + shard.spinY * dt);
            shard.rotationZ = wrapDegrees(shard.rotationZ + shard.spinZ * dt);
            moveShard(shard, dt);
        }
    }

    private void moveShard(CubeShard shard, float dt) {
        moveShardAxis(shard, shard.velocity.x * dt, 0.0, 0.0);
        moveShardAxis(shard, 0.0, shard.velocity.y * dt, 0.0);
        moveShardAxis(shard, 0.0, 0.0, shard.velocity.z * dt);
    }

    private void moveShardAxis(CubeShard shard, double dx, double dy, double dz) {
        if (dx == 0.0 && dy == 0.0 && dz == 0.0) return;
        Vec3d next = shard.pos.add(dx, dy, dz);
        if (!shardCollides(next)) {
            shard.pos = next;
            return;
        }

        if (dx != 0.0) {
            shard.velocity = new Vec3d(-shard.velocity.x * 0.58, shard.velocity.y * 0.92, shard.velocity.z * 0.82);
            dampShardSpin(shard, 0.74f);
        } else if (dy != 0.0) {
            double impact = Math.abs(shard.velocity.y);
            double bounce = -shard.velocity.y * 0.64;
            if (impact > 1.15) {
                bounce = Math.max(bounce, 0.58);
            } else if (impact > 0.42) {
                bounce = Math.max(bounce, 0.24);
            } else {
                bounce = 0.0;
            }

            shard.velocity = new Vec3d(shard.velocity.x * 0.54, bounce, shard.velocity.z * 0.54);
            dampShardSpin(shard, bounce > 0.0 ? 0.62f : 0.16f);
        } else {
            shard.velocity = new Vec3d(shard.velocity.x * 0.82, shard.velocity.y * 0.92, -shard.velocity.z * 0.58);
            dampShardSpin(shard, 0.74f);
        }
    }

    private void dampShardSpin(CubeShard shard, float factor) {
        shard.spinX *= factor;
        shard.spinY *= factor;
        shard.spinZ *= factor;
        if (Math.abs(shard.spinX) < 8f) shard.spinX = 0f;
        if (Math.abs(shard.spinY) < 8f) shard.spinY = 0f;
        if (Math.abs(shard.spinZ) < 8f) shard.spinZ = 0f;
    }

    private boolean shardCollides(Vec3d pos) {
        return mc.world != null && !mc.world.getBlockState(BlockPos.ofFloored(pos)).isAir();
    }

    private void renderCubeShards(WorldRenderContext ctx, int color) {
        if (cubeShards.isEmpty()) return;
        MatrixStack matrices = ctx.matrixStack();
        Camera camera = ctx.camera();
        if (matrices == null || camera == null) return;

        Vec3d cam = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        for (CubeShard shard : cubeShards) {
            float progress = MathHelper.clamp(shard.age / shard.life, 0f, 1f);
            float alpha = MathHelper.clamp((1f - progress) / 0.42f, 0f, 1f);
            if (alpha <= 0.01f) continue;
            float r = ((shard.rgb >> 16) & 0xFF) / 255f;
            float g = ((shard.rgb >> 8) & 0xFF) / 255f;
            float b = (shard.rgb & 0xFF) / 255f;
            Vec3d pos = shard.pos.subtract(cam);
            renderShardBloom(matrices, camera, pos, shard, r, g, b, alpha * 0.18f);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder core = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        boolean hasCoreVertices = false;
        for (CubeShard shard : cubeShards) {
            float progress = MathHelper.clamp(shard.age / shard.life, 0f, 1f);
            float alpha = MathHelper.clamp((1f - progress) / 0.42f, 0f, 1f);
            if (alpha <= 0.01f) continue;
            float r = ((shard.rgb >> 16) & 0xFF) / 255f;
            float g = ((shard.rgb >> 8) & 0xFF) / 255f;
            float b = (shard.rgb & 0xFF) / 255f;
            float[] colorCore = new float[]{Math.min(1f, r * 1.55f), Math.min(1f, g * 1.55f), Math.min(1f, b * 1.55f), alpha * 0.92f};

            matrices.push();
            Vec3d pos = shard.pos.subtract(cam);
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(shard.rotationX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(shard.rotationY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shard.rotationZ));
            renderShardBar(core, matrices.peek().getPositionMatrix(), shard, shard.thickness, colorCore);
            hasCoreVertices = true;
            matrices.pop();
        }
        if (hasCoreVertices) {
            BufferRenderer.drawWithGlobalProgram(core.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderShardBloom(MatrixStack matrices, Camera camera, Vec3d pos, CubeShard shard,
                                  float r, float g, float b, float alpha) {
        float halfLength = shard.length * 0.5f;
        float x1 = shard.axis == 0 ? -halfLength : 0f;
        float y1 = shard.axis == 1 ? -halfLength : 0f;
        float z1 = shard.axis == 2 ? -halfLength : 0f;
        float x2 = shard.axis == 0 ? halfLength : 0f;
        float y2 = shard.axis == 1 ? halfLength : 0f;
        float z2 = shard.axis == 2 ? halfLength : 0f;

        renderBloomSegment(matrices, camera, pos.x, pos.y, pos.z, x1, y1, z1, x2, y2, z2,
                shard.rotationX, shard.rotationY, shard.rotationZ, shard.thickness * 5.2f, r, g, b, alpha, true);
    }

    private void renderShardBar(BufferBuilder buffer, Matrix4f matrix, CubeShard shard, float thickness, float[] color) {
        float halfThickness = thickness * 0.5f;
        float halfLength = shard.length * 0.5f;
        if (shard.axis == 0) {
            cubeBox(buffer, matrix, -halfLength, -halfThickness, -halfThickness, halfLength, halfThickness, halfThickness, color);
        } else if (shard.axis == 1) {
            cubeBox(buffer, matrix, -halfThickness, -halfLength, -halfThickness, halfThickness, halfLength, halfThickness, color);
        } else {
            cubeBox(buffer, matrix, -halfThickness, -halfThickness, -halfLength, halfThickness, halfThickness, halfLength, color);
        }
    }

    private void cubeCornerBars(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float length, float thickness, float[] color) {
        float halfThickness = thickness * 0.5f;
        for (int ix = 0; ix < 2; ix++) {
            float x = ix == 0 ? x1 : x2;
            float xInner = x + (ix == 0 ? length : -length);
            for (int iy = 0; iy < 2; iy++) {
                float y = iy == 0 ? y1 : y2;
                float yInner = y + (iy == 0 ? length : -length);
                for (int iz = 0; iz < 2; iz++) {
                    float z = iz == 0 ? z1 : z2;
                    float zInner = z + (iz == 0 ? length : -length);
                    cubeBarX(buffer, matrix, x, xInner, y, z, halfThickness, color);
                    cubeBarY(buffer, matrix, x, y, yInner, z, halfThickness, color);
                    cubeBarZ(buffer, matrix, x, y, z, zInner, halfThickness, color);
                }
            }
        }
    }

    private void cubeBarX(BufferBuilder buffer, Matrix4f matrix, float fromX, float toX, float y, float z, float h, float[] color) {
        cubeBox(buffer, matrix, Math.min(fromX, toX), y - h, z - h, Math.max(fromX, toX), y + h, z + h, color);
    }

    private void cubeBarY(BufferBuilder buffer, Matrix4f matrix, float x, float fromY, float toY, float z, float h, float[] color) {
        cubeBox(buffer, matrix, x - h, Math.min(fromY, toY), z - h, x + h, Math.max(fromY, toY), z + h, color);
    }

    private void cubeBarZ(BufferBuilder buffer, Matrix4f matrix, float x, float y, float fromZ, float toZ, float h, float[] color) {
        cubeBox(buffer, matrix, x - h, y - h, Math.min(fromZ, toZ), x + h, y + h, Math.max(fromZ, toZ), color);
    }

    private void cubeBox(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        cubeQuad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
        cubeQuad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        cubeQuad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        cubeQuad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        cubeQuad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        cubeQuad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    private void cubeQuad(BufferBuilder buffer, Matrix4f matrix,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float[] color) {
        buffer.vertex(matrix, x1, y1, z1).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x2, y2, z2).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x3, y3, z3).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x4, y4, z4).color(color[0], color[1], color[2], color[3]);
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

    private static final class CubeShard {
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        final float life;
        final float length;
        final float thickness;
        final int axis;
        float rotationX;
        float rotationY;
        float rotationZ;
        float spinX;
        float spinY;
        float spinZ;
        final int rgb;
        float age;

        private CubeShard(Vec3d pos, Vec3d velocity, float life, float length, float thickness, int axis,
                          float rotationX, float rotationY, float rotationZ,
                          float spinX, float spinY, float spinZ, int rgb) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.life = life;
            this.length = length;
            this.thickness = thickness;
            this.axis = axis;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.spinX = spinX;
            this.spinY = spinY;
            this.spinZ = spinZ;
            this.rgb = rgb;
        }
    }
}
