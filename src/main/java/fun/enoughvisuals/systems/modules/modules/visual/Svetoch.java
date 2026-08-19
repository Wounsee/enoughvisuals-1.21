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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Register(name = "Svetoch", id = "svetoch", description = "Летающие светящиеся кубы с физикой", category = Category.VISUAL)
public class Svetoch extends Module {

    private static Svetoch INSTANCE;

    public final ColorSetting tintColor = add(new ColorSetting("Цвет", "color", "Цвет кубиков", 0xFF00FFFF));
    public final SliderSetting count = add(new SliderSetting("Кол кубиков", "count", "Максимальное количество кубиков в мире", 100, 50, 300, 1));
    public final SliderSetting cubeSize = add(new SliderSetting("Размер куба", "size", "Размер летающих кубиков", 0.26, 0.1, 0.6, 0.02));

    private static final long LIFETIME = 1500L;
    private static final double CULL_DIST_SQ = 96 * 96;

    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1},
            {-1, 1, -1}, {1, 1, -1}, {1, 1, 1}, {-1, 1, 1}
    };
    private static final int[][] FACES = {
            {0, 1, 2, 3}, {4, 5, 6, 7},
            {0, 1, 5, 4}, {2, 3, 7, 6},
            {1, 2, 6, 5}, {3, 0, 4, 7}
    };
    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> visible = new ArrayList<>();
    private final BufferAllocator allocator = new BufferAllocator(1 << 16);
    private final Random random = new Random();
    private long lastFrameTime = System.nanoTime();

    public Svetoch() {
        INSTANCE = this;
    }

    public static Svetoch active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public int getColor() {
        return tintColor.get();
    }

    @Override
    protected void onDisable() {
        particles.clear();
    }

    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        int max = count.get().intValue();
        int r = 24;
        for (int i = 0; i < 3 && particles.size() < max; i++) {
            double rx = (random.nextDouble() * 2 - 1) * r;
            double rz = (random.nextDouble() * 2 - 1) * r;
            Vec3d additional = mc.player.getPos().add(rx, 0, rz);

            BlockPos topPos = mc.world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING,
                    BlockPos.ofFloored(additional)
            );

            double x = topPos.getX() + (random.nextDouble() * 2 - 1) * 3;
            double z = topPos.getZ() + (random.nextDouble() * 2 - 1) * 3;
            double y = mc.player.getY() + mc.player.getHeight() + random.nextDouble() * r;

            Vec3d spawnPos = new Vec3d(x, y, z);
            while (!mc.world.isAir(BlockPos.ofFloored(spawnPos)) && spawnPos.y < mc.world.getTopYInclusive()) {
                spawnPos = spawnPos.add(0, 1, 0);
            }

            Vec3d velocity = new Vec3d(
                    mc.player.getVelocity().x + (random.nextDouble() * 2 - 1) * 0.6,
                    -0.01 + random.nextDouble() * 0.45,
                    mc.player.getVelocity().z + (random.nextDouble() * 2 - 1) * 0.6
            );

            particles.add(new Particle(spawnPos, velocity));
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        Svetoch module = active();
        if (module == null || mc.player == null || mc.world == null) return;
        if (module.particles.isEmpty()) return;

        long now = System.currentTimeMillis();
        long nanoNow = System.nanoTime();
        float ticks = (float) Math.min((nanoNow - module.lastFrameTime) / 1_000_000_000.0, 0.1) * 60F;
        module.lastFrameTime = nanoNow;

        MatrixStack matrices = context.matrixStack();
        Vec3d cam = context.camera().getPos();
        Matrix4f base = matrices.peek().getPositionMatrix();
        Quaternionf camRotation = context.camera().getRotation();

        float rotation = (now % 9000L) / 9000F * 360F;
        float rotXRad = (float) Math.toRadians(rotation * 0.5F);

        int baseColor = module.getColor();
        int cr = (baseColor >> 16) & 0xFF;
        int cg = (baseColor >> 8) & 0xFF;
        int cb = baseColor & 0xFF;

        float size = module.cubeSize.get().floatValue();

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(module.allocator);

        module.visible.clear();
        Iterator<Particle> it = module.particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(now, ticks);
            if (p.shouldRemove()) {
                it.remove();
                continue;
            }

            if (p.alphaValue <= 0.01F) continue;

            double dx = p.x - cam.x;
            double dy = p.y - cam.y;
            double dz = p.z - cam.z;
            if (dx * dx + dy * dy + dz * dz > CULL_DIST_SQ) continue;

            p.cubeMatrix.set(base)
                    .translate((float) dx, (float) dy, (float) dz)
                    .rotateY((float) Math.toRadians(rotation + p.phase))
                    .rotateX(rotXRad);

            p.glowMatrix.set(base)
                    .translate((float) dx, (float) dy, (float) dz)
                    .rotate(camRotation);

            module.visible.add(p);
        }

        if (module.visible.isEmpty()) return;

        VertexConsumer buf = immediate.getBuffer(FILL_LAYER);
        for (Particle p : module.visible) {
            int faceAlpha = (int) (p.alphaValue * 0.2F * 255);
            if (faceAlpha <= 0) continue;
            for (int[] face : FACES) {
                for (int idx : face) {
                    float[] c = CORNERS[idx];
                    buf.vertex(p.cubeMatrix, c[0] * size, c[1] * size, c[2] * size).color(cr, cg, cb, faceAlpha);
                }
            }
        }

        buf = immediate.getBuffer(LINE_LAYER);
        for (Particle p : module.visible) {
            int edgeAlpha = (int) (p.alphaValue * 255);
            if (edgeAlpha <= 0) continue;
            for (int[] edge : EDGES) {
                float[] a = CORNERS[edge[0]];
                float[] b = CORNERS[edge[1]];
                buf.vertex(p.cubeMatrix, a[0] * size, a[1] * size, a[2] * size).color(cr, cg, cb, edgeAlpha);
                buf.vertex(p.cubeMatrix, b[0] * size, b[1] * size, b[2] * size).color(cr, cg, cb, edgeAlpha);
            }
        }

        buf = immediate.getBuffer(GLOW_LAYER_BIG);
        for (Particle p : module.visible) {
            drawGlow(buf, p.glowMatrix, cr, cg, cb, (int) (80 * p.alphaValue), size * 6);
        }

        immediate.draw();
    }

    private static void drawGlow(VertexConsumer buf, Matrix4f m, int r, int g, int b, int alpha, float s) {
        if (alpha <= 0) return;
        buf.vertex(m, -s, -s, 0).texture(0, 1).color(r, g, b, alpha);
        buf.vertex(m, s, -s, 0).texture(1, 1).color(r, g, b, alpha);
        buf.vertex(m, s, s, 0).texture(1, 0).color(r, g, b, alpha);
        buf.vertex(m, -s, s, 0).texture(0, 0).color(r, g, b, alpha);
    }

    private static class Particle {
        private static final double BASE_VELOCITY = 0.05;
        private static final double BOUNCINESS = 0.85;
        private static final double GRAVITY = 0.0001;

        double x, y, z, mX, mY, mZ;
        final long start;
        final float phase;
        float alphaValue = 0f;
        final Matrix4f cubeMatrix = new Matrix4f();
        final Matrix4f glowMatrix = new Matrix4f();

        Particle(Vec3d pos, Vec3d velocity) {
            this.start = System.currentTimeMillis();
            this.phase = (float) (Math.random() * 100.0);
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
            this.mX = velocity.x * BASE_VELOCITY;
            this.mY = velocity.y * BASE_VELOCITY;
            this.mZ = velocity.z * BASE_VELOCITY;
        }

        void update(long now, float ticks) {
            double nextX = x + mX * ticks;
            double nextY = y + mY * ticks;
            double nextZ = z + mZ * ticks;

            if (Math.floor(nextX) == Math.floor(x)
                    && Math.floor(nextY) == Math.floor(y)
                    && Math.floor(nextZ) == Math.floor(z)) {
                if (isHit(nextX, nextY, nextZ)) {
                    mX = -mX * BOUNCINESS;
                    mY = -mY * BOUNCINESS;
                    mZ = -mZ * BOUNCINESS;
                } else {
                    x = nextX;
                    y = nextY;
                    z = nextZ;
                }
            } else {
                if (isHit(nextX, y, z)) mX = -mX * BOUNCINESS;
                else x = nextX;
                if (isHit(x, nextY, z)) mY = -mY * BOUNCINESS;
                else y = nextY;
                if (isHit(x, y, nextZ)) mZ = -mZ * BOUNCINESS;
                else z = nextZ;
            }

            mY -= GRAVITY * ticks;

            long elapsed = now - start;
            float fadeIn = Math.min(1.0f, elapsed / 500.0f);
            float fadeOut = elapsed > LIFETIME ? Math.max(0.0f, 1.0f - (elapsed - LIFETIME) / 2000.0f) : 1.0f;
            alphaValue = fadeIn * fadeOut;
        }

        boolean shouldRemove() {
            return (System.currentTimeMillis() - start > LIFETIME + 2000L) || alphaValue <= 0.005F;
        }

        private static boolean isHit(double px, double py, double pz) {
            BlockPos pos = BlockPos.ofFloored(px, py, pz);
            return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
        }
    }

    private static final Identifier GLOW_TEXTURE_BIG = Identifier.of("enough", "textures/particles/glow.png");

    private static final RenderLayer FILL_LAYER = RenderLayer.of(
            "svetoch_fill",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            1 << 13,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static final RenderLayer LINE_LAYER = RenderLayer.of(
            "svetoch_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.DEBUG_LINES,
            1 << 13,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );

    private static final RenderLayer GLOW_LAYER_BIG = RenderLayer.of(
            "svetoch_glow_big",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS,
            1 << 12,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .texture(new RenderPhase.Texture(GLOW_TEXTURE_BIG, TriState.FALSE, false))
                    .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                    .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .build(false)
    );
}
