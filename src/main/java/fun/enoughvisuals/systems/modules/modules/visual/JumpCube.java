package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Register(name = "Jump Cube", id = "jump_cube", description = "Кубики вокруг игрока - прыгают и крутятся", category = Category.VISUAL)
public class JumpCube extends Module {

    private static JumpCube INSTANCE;

    public final ColorSetting tintColor = add(new ColorSetting("Цвет", "color", "Цвет кубиков", 0xFF00FFFF));
    public final SliderSetting count = add(new SliderSetting("Кубиков", "count", "Количество спавнящихся кубиков в тик", 3, 1, 10, 1));
    public final SliderSetting size = add(new SliderSetting("Размер", "size", "Размер кубиков", 0.18, 0.05, 0.40, 0.01));

    private static final int RES_PX = 16;
    private static final long LIFETIME_MS = 2000L;
    private static final double SPAWN_MIN_DST = 0.6;
    private static final double SPAWN_MAX_DST = 2.6;
    private static final double MAX_OWNER_DIST = 6.0;
    private static final long RANGE_FADE_MS = 400L;
    private static final int[] JUMP_YAWS = {0, 90, 180, 270};

    private final Random random = new Random();
    private final List<Cube> cubes = new ArrayList<>();
    private final List<Cube> visible = new ArrayList<>();
    private final BufferAllocator allocator = new BufferAllocator(1 << 16);

    public JumpCube() {
        INSTANCE = this;
    }

    public static JumpCube active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    @Override
    protected void onDisable() {
        cubes.clear();
    }

    public static void onPlayerJump() {
        JumpCube module = active();
        if (module != null) {
            for (Cube c : module.cubes) {
                c.tryStartJump();
            }
        }
    }

    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getPos();
        List<BlockPos> candidates = getPlaceableAround(playerPos, SPAWN_MIN_DST, SPAWN_MAX_DST, 3);
        int toSpawn = count.get().intValue();
        for (int i = 0; i < toSpawn && !candidates.isEmpty(); i++) {
            int idx = random.nextInt(candidates.size());
            Vec3d sp = findSpawnPoint(candidates.get(idx), playerPos);
            if (sp != null) cubes.add(new Cube(sp));
        }

        cubes.forEach(Cube::updateLogic);
        cubes.removeIf(Cube::isDead);
    }

    public static void renderInWorld(WorldRenderContext context) {
        JumpCube module = active();
        if (module == null || mc.player == null || mc.world == null) return;
        if (module.cubes.isEmpty()) return;

        float pt = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        Vec3d cam = context.camera().getPos();
        MatrixStack pose = context.matrixStack();
        float s = module.size.get().floatValue();

        int base = module.tintColor.get();
        int cr = (base >> 16) & 0xFF;
        int cg = (base >> 8) & 0xFF;
        int cb = base & 0xFF;

        module.visible.clear();
        for (Cube c : module.cubes) {
            if (c.isDead()) continue;
            float alpha = c.getAlpha();
            if (alpha < 0.01f) continue;

            double wx = c.spawnPos.x - cam.x;
            double wy = c.spawnPos.y + c.getJumpYOffset(pt) - cam.y;
            double wz = c.spawnPos.z - cam.z;

            pose.push();
            pose.translate(wx, wy, wz);
            if (c.jumpTicksMax > 0) {
                float tLerped = MathHelper.lerp(pt, (float) c.prevJumpTicks, (float) c.jumpTicks);
                float phase = 1.0f - tLerped / (float) c.jumpTicksMax;
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(c.jumpYaw));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f * phase));
            }
            pose.scale(alpha, alpha, alpha);
            c.cubeMatrix.set(pose.peek().getPositionMatrix());
            pose.pop();

            pose.push();
            pose.translate(wx, wy, wz);
            pose.multiply(context.camera().getRotation());
            pose.scale(alpha, alpha, alpha);
            c.glowMatrix.set(pose.peek().getPositionMatrix());
            pose.pop();

            c.cachedAlpha = alpha;
            module.visible.add(c);
        }

        if (module.visible.isEmpty()) return;

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(module.allocator);

        VertexConsumer buf = immediate.getBuffer(FILL_LAYER);
        for (Cube c : module.visible) {
            int fa = (int) (c.cachedAlpha * 0.2f * 255);
            if (fa > 0) drawCubeFill(buf, c.cubeMatrix, s, cr, cg, cb, fa);
        }

        buf = immediate.getBuffer(LINE_LAYER);
        for (Cube c : module.visible) {
            int ea = (int) (c.cachedAlpha * 255);
            if (ea > 0) drawCubeEdges(buf, c.cubeMatrix, s, cr, cg, cb, ea);
        }

        buf = immediate.getBuffer(GLOW_LAYER_BIG);
        for (Cube c : module.visible) drawGlow(buf, c.glowMatrix, cr, cg, cb, (int) (35 * c.cachedAlpha), s * 5.5f);

        buf = immediate.getBuffer(GLOW_LAYER_SMALL);
        for (Cube c : module.visible) drawGlow(buf, c.glowMatrix, cr, cg, cb, (int) (40 * c.cachedAlpha), s * 1.8f);

        immediate.draw();
    }

    private List<BlockPos> getPlaceableAround(Vec3d center, double minDst, double maxDst, int offsetDown) {
        List<BlockPos> result = new ArrayList<>();
        if (mc.world == null) return result;
        double xE = center.x, yE = center.y + 1.0, zE = center.z;
        for (double x = xE - maxDst; x < xE + maxDst; x++) {
            for (double z = zE - maxDst; z < zE + maxDst; z++) {
                for (double y = yE - offsetDown; y < yE; y++) {
                    BlockPos pos = BlockPos.ofFloored(x, y, z);
                    if (canPlace(pos) && !result.contains(pos)) result.add(pos);
                }
            }
        }
        return result;
    }

    private boolean canPlace(BlockPos pos) {
        if (mc.world == null) return false;
        if (!mc.world.isAir(pos)) return false;
        if (mc.world.isAir(pos.down())) return false;
        return mc.world.getOtherEntities(null, new Box(pos)).isEmpty();
    }

    private Vec3d findSpawnPoint(BlockPos pos, Vec3d playerPos) {
        double resOff = 1.0 / RES_PX;
        double half = resOff / 2.0;
        for (int att = 64; att > 0; att--) {
            Vec3d v = new Vec3d(
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble()
            );
            double dst = v.distanceTo(playerPos);
            if (dst >= SPAWN_MIN_DST && dst <= SPAWN_MAX_DST) {
                double sx = Math.floor(v.x * RES_PX) / RES_PX + half;
                double sy = Math.floor(v.y) + half - resOff;
                double sz = Math.floor(v.z * RES_PX) / RES_PX + half;
                return new Vec3d(sx, sy, sz);
            }
        }
        return null;
    }

    private static void drawCubeFill(VertexConsumer b, Matrix4f m, float s, int r, int g, int bl, int a) {
        float h = s / 2f;
        b.vertex(m, -h, -h, -h).color(r, g, bl, a); b.vertex(m, h, -h, -h).color(r, g, bl, a);
        b.vertex(m, h, -h, h).color(r, g, bl, a); b.vertex(m, -h, -h, h).color(r, g, bl, a);

        b.vertex(m, -h, h, -h).color(r, g, bl, a); b.vertex(m, h, h, -h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a); b.vertex(m, -h, h, h).color(r, g, bl, a);

        b.vertex(m, -h, -h, -h).color(r, g, bl, a); b.vertex(m, h, -h, -h).color(r, g, bl, a);
        b.vertex(m, h, h, -h).color(r, g, bl, a); b.vertex(m, -h, h, -h).color(r, g, bl, a);

        b.vertex(m, -h, -h, h).color(r, g, bl, a); b.vertex(m, h, -h, h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a); b.vertex(m, -h, h, h).color(r, g, bl, a);

        b.vertex(m, -h, -h, -h).color(r, g, bl, a); b.vertex(m, -h, -h, h).color(r, g, bl, a);
        b.vertex(m, -h, h, h).color(r, g, bl, a); b.vertex(m, -h, h, -h).color(r, g, bl, a);

        b.vertex(m, h, -h, -h).color(r, g, bl, a); b.vertex(m, h, -h, h).color(r, g, bl, a);
        b.vertex(m, h, h, h).color(r, g, bl, a); b.vertex(m, h, h, -h).color(r, g, bl, a);
    }

    private static void drawCubeEdges(VertexConsumer b, Matrix4f m, float s, int r, int g, int bl, int a) {
        float h = s / 2f;
        line(b, m, -h, -h, -h, h, -h, -h, r, g, bl, a); line(b, m, h, -h, -h, h, -h, h, r, g, bl, a);
        line(b, m, h, -h, h, -h, -h, h, r, g, bl, a); line(b, m, -h, -h, h, -h, -h, -h, r, g, bl, a);
        line(b, m, -h, h, -h, h, h, -h, r, g, bl, a); line(b, m, h, h, -h, h, h, h, r, g, bl, a);
        line(b, m, h, h, h, -h, h, h, r, g, bl, a); line(b, m, -h, h, h, -h, h, -h, r, g, bl, a);
        line(b, m, -h, -h, -h, -h, h, -h, r, g, bl, a); line(b, m, h, -h, -h, h, h, -h, r, g, bl, a);
        line(b, m, h, -h, h, h, h, h, r, g, bl, a); line(b, m, -h, -h, h, -h, h, h, r, g, bl, a);
    }

    private static void line(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int bl, int a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
    }

    private static void drawGlow(VertexConsumer buf, Matrix4f m, int r, int g, int b, int alpha, float s) {
        if (alpha <= 0) return;
        buf.vertex(m, -s, -s, 0).texture(0, 1).color(r, g, b, alpha);
        buf.vertex(m, s, -s, 0).texture(1, 1).color(r, g, b, alpha);
        buf.vertex(m, s, s, 0).texture(1, 0).color(r, g, b, alpha);
        buf.vertex(m, -s, s, 0).texture(0, 0).color(r, g, b, alpha);
    }

    private class Cube {
        final Vec3d spawnPos;
        final long spawnTime = System.currentTimeMillis();
        float cachedAlpha;
        int jumpTicks = 0;
        int prevJumpTicks = 0;
        int jumpTicksMax = 0;
        int jumpYaw = 0;
        double jumpHeight = 0;
        boolean outOfRange = false;
        long outOfRangeStart = 0L;
        final Matrix4f cubeMatrix = new Matrix4f();
        final Matrix4f glowMatrix = new Matrix4f();

        Cube(Vec3d spawnPos) {
            this.spawnPos = spawnPos;
        }

        void updateLogic() {
            prevJumpTicks = jumpTicks;
            if (jumpTicks > 0) jumpTicks--;

            if (mc.player != null && mc.player.getPos().distanceTo(spawnPos) > MAX_OWNER_DIST) {
                if (!outOfRange) {
                    outOfRange = true;
                    outOfRangeStart = System.currentTimeMillis();
                }
            }
        }

        void tryStartJump() {
            if (jumpTicks <= 0) {
                jumpTicksMax = jumpTicks = (int) (14.0f * (0.5f + 0.5f * random.nextFloat()));
                jumpHeight = (2 + random.nextInt(11)) / 16.0;
                jumpYaw = JUMP_YAWS[random.nextInt(JUMP_YAWS.length)];
            }
        }

        float getTimePc() {
            return MathHelper.clamp((float) (System.currentTimeMillis() - spawnTime) / LIFETIME_MS, 0f, 1f);
        }

        float getRangeFade() {
            if (!outOfRange) return 1f;
            return MathHelper.clamp(1f - (float) (System.currentTimeMillis() - outOfRangeStart) / RANGE_FADE_MS, 0f, 1f);
        }

        float getAlpha() {
            float t = getTimePc();
            float a = t < 0.1f ? t / 0.1f : t > 0.8f ? 1f - (t - 0.8f) / 0.2f : 1f;
            return MathHelper.clamp(a, 0f, 1f) * getRangeFade();
        }

        double getJumpYOffset(float pt) {
            if (jumpTicksMax <= 0) return 0.0;
            float t = MathHelper.lerp(pt, (float) prevJumpTicks, (float) jumpTicks) / (float) jumpTicksMax;
            if (t > 0.5f) t = 1.0f - t;
            return t * 2.0 * jumpHeight;
        }

        boolean isDead() {
            return getTimePc() >= 1f || (outOfRange && getRangeFade() <= 0.01f);
        }
    }

    private static final Identifier GLOW_TEX_BIG = Identifier.of("enough", "textures/visuals/particles_1.png");
    private static final Identifier GLOW_TEX_SMALL = Identifier.of("enough", "textures/visuals/particles_2.png");

    private static final RenderLayer FILL_LAYER = RenderLayer.of(
            "jump_cube_fill",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            1 << 13,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static final RenderLayer LINE_LAYER = RenderLayer.of(
            "jump_cube_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.DEBUG_LINES,
            1 << 12,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static final RenderLayer GLOW_LAYER_BIG = RenderLayer.of(
            "jump_cube_glow_big",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS,
            1 << 12,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(GLOW_TEX_BIG, TriState.FALSE, false))
                    .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static final RenderLayer GLOW_LAYER_SMALL = RenderLayer.of(
            "jump_cube_glow_small",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS,
            1 << 12,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(GLOW_TEX_SMALL, TriState.FALSE, false))
                    .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );
}
