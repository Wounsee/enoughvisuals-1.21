package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Function;

@Register(name = "World Cubes", id = "world_cubes", description = "Сетка по миру со свечением", category = Category.VISUAL)
public class WorldCubes extends Module {

    private static WorldCubes INSTANCE;

    public final ColorSetting tintColor = add(new ColorSetting("Цвет", "color", "Цвет сетки", 0xFF00FFFF));
    public final SliderSetting range = add(new SliderSetting("Радиус (ячейки)", "range", "Радиус сетки в ячейках", 4, 1, 30, 1));
    public final SliderSetting vRange = add(new SliderSetting("Высота (ячейки)", "v_range", "Высота сетки в ячейках", 2, 1, 20, 1));
    public final SliderSetting lineAlphaS = add(new SliderSetting("Прозрачность линий", "line_alpha", "Прозрачность линий сетки", 0.22, 0.04, 1.0, 0.02));
    public final BooleanSetting showGlow = add(new BooleanSetting("Свечение в углах", "show_glow", "Отображать мягкое свечение в узлах сетки", true));
    public final SliderSetting glowSize = add(new SliderSetting("Размер свечения", "glow_size", "Размер свечения в узлах", 0.08, 0.02, 1.0, 0.02));
    public final SliderSetting glowBright = add(new SliderSetting("Яркость свечения", "glow_bright", "Яркость свечения", 1.8, 0.5, 4.0, 0.1));

    private static final int CELL = 12;
    private final BufferAllocator allocator = new BufferAllocator(1 << 18);

    private int currOx, currOy, currOz;
    private int prevOx, prevOy, prevOz;
    private boolean firstFrame = true;
    private long shiftStartTime = 0L;
    private static final long SHIFT_DURATION_MS = 250L;

    public WorldCubes() {
        INSTANCE = this;
        glowSize.visibleWhen(showGlow::get);
        glowBright.visibleWhen(showGlow::get);
    }

    public static WorldCubes active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    public int getColor() {
        return tintColor.get();
    }

    @Override
    protected void onEnable() {
        firstFrame = true;
    }

    public static void renderInWorld(WorldRenderContext context) {
        WorldCubes module = active();
        if (module == null || mc.player == null || mc.world == null) return;

        int ox = snapDown(mc.player.getX());
        int oy = snapDown(mc.player.getY());
        int oz = snapDown(mc.player.getZ());

        long now = System.currentTimeMillis();
        if (module.firstFrame) {
            module.currOx = ox; module.currOy = oy; module.currOz = oz;
            module.prevOx = ox; module.prevOy = oy; module.prevOz = oz;
            module.shiftStartTime = now - SHIFT_DURATION_MS;
            module.firstFrame = false;
        } else if (ox != module.currOx || oy != module.currOy || oz != module.currOz) {
            module.prevOx = module.currOx; module.prevOy = module.currOy; module.prevOz = module.currOz;
            module.currOx = ox; module.currOy = oy; module.currOz = oz;
            module.shiftStartTime = now;
        }

        float progress = MathHelper.clamp((float) (now - module.shiftStartTime) / SHIFT_DURATION_MS, 0f, 1f);
        float t = 1f - (1f - progress) * (1f - progress);

        int col = module.getColor();
        int cr = (col >> 16) & 0xFF;
        int cg = (col >> 8) & 0xFF;
        int cb = col & 0xFF;

        float baseAlpha = module.lineAlphaS.get().floatValue() * 255f;
        int rI = module.range.get().intValue();
        int vI = module.vRange.get().intValue();

        Vec3d cam = context.camera().getPos();
        MatrixStack stack = context.matrixStack();
        Matrix4f mat = stack.peek().getPositionMatrix();

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(module.allocator);
        VertexConsumer lb = immediate.getBuffer(LINE_LAYER);

        if (t < 0.999f) {
            int oldA = (int) (baseAlpha * (1f - t));
            module.drawLines(lb, mat, cam, module.prevOx, module.prevOy, module.prevOz, cr, cg, cb, oldA, rI, vI);
        }

        int newA = (int) (baseAlpha * t);
        module.drawLines(lb, mat, cam, module.currOx, module.currOy, module.currOz, cr, cg, cb, newA, rI, vI);

        if (module.showGlow.get()) {
            float gs = module.glowSize.get().floatValue();
            float brite = module.glowBright.get().floatValue();

            int newGlowA = Math.min(255, (int) (15 * t * brite));
            module.drawGlows(context, immediate, ROMB_ESP.apply(GLOW_TEX_BIG), mat, cam, module.currOx, module.currOy, module.currOz,
                    cr, cg, cb, newGlowA, gs * 2, rI, vI);

            newGlowA = Math.min(255, (int) (255 * t * brite));
            module.drawGlows(context, immediate, ROMB_ESP.apply(GLOW_TEX_CIRCLE), mat, cam, module.currOx, module.currOy, module.currOz,
                    cr, cg, cb, newGlowA, gs, rI, vI);
        }

        immediate.draw();
    }

    private void drawLines(VertexConsumer lb, Matrix4f mat, Vec3d cam,
                           int ox, int oy, int oz,
                           int cr, int cg, int cb, int alpha,
                           int rI, int vI) {
        if (alpha <= 0) return;

        int xMin = ox - rI * CELL, xMax = ox + rI * CELL;
        int zMin = oz - rI * CELL, zMax = oz + rI * CELL;
        int yMin = oy - (vI - 1) * CELL;
        int yMax = oy + vI * CELL;

        for (int x = xMin; x <= xMax; x += CELL) {
            for (int z = zMin; z <= zMax; z += CELL) {
                float rx = (float) (x - cam.x), rz = (float) (z - cam.z);
                lb.vertex(mat, rx, (float) (yMin - cam.y), rz).color(cr, cg, cb, alpha);
                lb.vertex(mat, rx, (float) (yMax - cam.y), rz).color(cr, cg, cb, alpha);
            }
        }

        for (int y = yMin; y <= yMax; y += CELL) {
            float ry = (float) (y - cam.y);
            for (int z = zMin; z <= zMax; z += CELL) {
                float rz = (float) (z - cam.z);
                lb.vertex(mat, (float) (xMin - cam.x), ry, rz).color(cr, cg, cb, alpha);
                lb.vertex(mat, (float) (xMax - cam.x), ry, rz).color(cr, cg, cb, alpha);
            }
        }

        for (int y = yMin; y <= yMax; y += CELL) {
            float ry = (float) (y - cam.y);
            for (int x = xMin; x <= xMax; x += CELL) {
                float rx = (float) (x - cam.x);
                lb.vertex(mat, rx, ry, (float) (zMin - cam.z)).color(cr, cg, cb, alpha);
                lb.vertex(mat, rx, ry, (float) (zMax - cam.z)).color(cr, cg, cb, alpha);
            }
        }
    }

    private void drawGlows(WorldRenderContext ctx, VertexConsumerProvider.Immediate immediate, RenderLayer glowLayer,
                           Matrix4f mat, Vec3d cam,
                           int ox, int oy, int oz,
                           int cr, int cg, int cb, int alpha,
                           float gs, int rI, int vI) {
        if (alpha <= 0) return;

        int xMin = ox - rI * CELL, xMax = ox + rI * CELL;
        int zMin = oz - rI * CELL, zMax = oz + rI * CELL;
        int yMin = oy - (vI - 1) * CELL;
        int yMax = oy + vI * CELL;

        Camera camera = ctx.camera();
        Quaternionf q = camera.getRotation();
        Vector3f c0 = new Vector3f(-gs, -gs, 0).rotate(q);
        Vector3f c1 = new Vector3f(gs, -gs, 0).rotate(q);
        Vector3f c2 = new Vector3f(gs, gs, 0).rotate(q);
        Vector3f c3 = new Vector3f(-gs, gs, 0).rotate(q);

        float yawRad = (float) Math.toRadians(camera.getYaw());
        float pitchRad = (float) Math.toRadians(camera.getPitch());
        float cosPitch = (float) Math.cos(pitchRad);
        float lx = (float) (-Math.sin(yawRad) * cosPitch);
        float ly = (float) -Math.sin(pitchRad);
        float lz = (float) (Math.cos(yawRad) * cosPitch);

        VertexConsumer gb = immediate.getBuffer(glowLayer);

        for (int x = xMin; x <= xMax; x += CELL) {
            for (int y = yMin; y <= yMax; y += CELL) {
                for (int z = zMin; z <= zMax; z += CELL) {
                    float rx = (float) (x - cam.x);
                    float ry = (float) (y - cam.y);
                    float rz = (float) (z - cam.z);

                    if (rx * lx + ry * ly + rz * lz < -gs * 2) continue;

                    gb.vertex(mat, rx + c0.x, ry + c0.y, rz + c0.z).texture(0, 1).color(cr, cg, cb, alpha);
                    gb.vertex(mat, rx + c1.x, ry + c1.y, rz + c1.z).texture(1, 1).color(cr, cg, cb, alpha);
                    gb.vertex(mat, rx + c2.x, ry + c2.y, rz + c2.z).texture(1, 0).color(cr, cg, cb, alpha);
                    gb.vertex(mat, rx + c3.x, ry + c3.y, rz + c3.z).texture(0, 0).color(cr, cg, cb, alpha);
                }
            }
        }
    }

    private static int snapDown(double v) {
        return (int) Math.floor(v / CELL) * CELL;
    }

    private static final Identifier GLOW_TEX_BIG = Identifier.of("enough", "textures/particles/glow.png");
    private static final Identifier GLOW_TEX_CIRCLE = Identifier.of("enough", "textures/particles/circle.png");

    public static final Function<Identifier, RenderLayer> ROMB_ESP =
            Util.memoize(texture -> RenderLayer.of(
                    "world_cubes_glow_" + texture,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    VertexFormat.DrawMode.QUADS,
                    1536,
                    false,
                    true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .texture(new RenderPhase.Texture(texture, TriState.FALSE, false))
                            .program(RenderPhase.POSITION_TEXTURE_COLOR_PROGRAM)
                            .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
                            .cull(RenderPhase.DISABLE_CULLING)
                            .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .build(false)
            ));

    private static final RenderLayer LINE_LAYER = RenderLayer.of(
            "world_cubes_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.DEBUG_LINES,
            1 << 14,
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
}
