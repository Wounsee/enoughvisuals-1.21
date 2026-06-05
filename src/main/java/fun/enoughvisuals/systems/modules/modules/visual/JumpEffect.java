package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Register(name = "Jump Effect", id = "jump_effect", description = "Wave over blocks when you jump", category = Category.VISUAL)
public class JumpEffect extends Module {

    private static final float SHAPE_OFFSET = 0.003f;
    private static final float BAND_WIDTH = 2.5f;
    private static final int MAX_RENDERED_PER_FRAME = 6000;

    private static JumpEffect INSTANCE;

    public final SliderSetting radius = add(new SliderSetting("Radius", "radius", "Wave radius", 5.0, 5.0, 100.0, 1.0));
    public final SliderSetting speed = add(new SliderSetting("Speed", "speed", "Animation duration in ms", 800.0, 300.0, 2000.0, 10.0, "ms"));
    public final ColorSetting color1 = add(new ColorSetting("Color 1", "color_1", "First wave color", 0xFF64C8FF));
    public final ColorSetting color2 = add(new ColorSetting("Color 2", "color_2", "Second wave color", 0xFFFF64C8));

    private final List<WaveEffect> waveEffects = new ArrayList<>();

    public JumpEffect() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    public static void onLocalPlayerJump() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;

        BlockPos pos = mc.player.getBlockPos().down().toImmutable();
        INSTANCE.waveEffects.add(INSTANCE.new WaveEffect(
                pos,
                System.currentTimeMillis(),
                INSTANCE.speed.get().longValue(),
                INSTANCE.radius.get()
        ));
    }

    @Override
    protected void onDisable() {
        waveEffects.clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            waveEffects.clear();
            return;
        }
        waveEffects.removeIf(WaveEffect::isExpired);
    }

    private void render(WorldRenderContext context) {
        if (waveEffects.isEmpty()) return;

        Vec3d camera = context.camera().getPos();
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Iterator<WaveEffect> iterator = waveEffects.iterator();
        while (iterator.hasNext()) {
            WaveEffect wave = iterator.next();
            if (wave.isExpired()) {
                iterator.remove();
                continue;
            }
            wave.render(matrix, camera);
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthFunc(previousDepthFunc);
        RenderSystem.depthMask(previousDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private final class WaveEffect {
        private final BlockPos centerPos;
        private final long startTime;
        private final long durationMs;
        private final double maxRadius;
        private final int searchRadius;

        private WaveEffect(BlockPos centerPos, long startTime, long durationMs, double maxRadius) {
            this.centerPos = centerPos;
            this.startTime = startTime;
            this.durationMs = Math.max(1L, durationMs);
            this.maxRadius = maxRadius;
            this.searchRadius = (int) Math.ceil(maxRadius);
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - startTime > durationMs;
        }

        private void render(Matrix4f matrix, Vec3d camera) {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, Math.max(0.0f, elapsed / (float) durationMs));
            float currentRadius = easeOutCubic(progress) * (float) maxRadius;
            float globalAlpha = progress < 0.15f
                    ? progress / 0.15f
                    : progress >= 0.75f ? 1.0f - easeInCubic((progress - 0.75f) / 0.25f) : 1.0f;

            int rendered = 0;
            for (int x = -searchRadius; x <= searchRadius && rendered < MAX_RENDERED_PER_FRAME; x++) {
                for (int z = -searchRadius; z <= searchRadius && rendered < MAX_RENDERED_PER_FRAME; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance > currentRadius + 0.5f || distance < currentRadius - BAND_WIDTH) continue;

                    BlockPos blockPos = centerPos.add(x, 0, z);
                    BlockState state = mc.world.getBlockState(blockPos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, blockPos, ShapeContext.of(mc.player));
                    if (shape.isEmpty()) continue;

                    float waveProgress = (float) (distance / Math.max(0.001, maxRadius));
                    float localAlpha = 1.0f - Math.abs((float) distance - currentRadius) / BAND_WIDTH;
                    localAlpha = Math.max(0.0f, Math.min(1.0f, localAlpha));
                    float pulse = (float) Math.sin(progress * Math.PI * 4.0 - waveProgress * 2.0f);
                    localAlpha *= 0.5f + ((pulse + 1.0f) * 0.5f) * 0.5f;
                    localAlpha *= globalAlpha;
                    if (localAlpha <= 0.02f) continue;

                    int gradientColor = RenderUtil.lerpColor(color1.get(), color2.get(), (float) (Math.sin((waveProgress - 0.5f) * Math.PI) * 0.5f + 0.5f));
                    drawWaveShape(matrix, camera, blockPos, shape,
                            RenderUtil.withAlphaInt(gradientColor, (int) (localAlpha * 110.0f)),
                            RenderUtil.withAlphaInt(gradientColor, (int) (localAlpha * 180.0f)));
                    rendered++;
                }
            }
        }

        private float easeOutCubic(float x) {
            return 1.0f - (float) Math.pow(1.0f - x, 3.0f);
        }

        private float easeInCubic(float x) {
            return x * x * x;
        }
    }

    private void drawWaveShape(Matrix4f matrix, Vec3d camera, BlockPos pos, VoxelShape shape, int fillColor, int outlineColor) {
        float[] fill = rgba(fillColor);
        float[] line = rgba(outlineColor);
        double dx = pos.getX() - camera.x;
        double dy = pos.getY() - camera.y;
        double dz = pos.getZ() - camera.z;

        BufferBuilder fillBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float) (dx + minX) - SHAPE_OFFSET;
            float y1 = (float) (dy + minY) - SHAPE_OFFSET;
            float z1 = (float) (dz + minZ) - SHAPE_OFFSET;
            float x2 = (float) (dx + maxX) + SHAPE_OFFSET;
            float y2 = (float) (dy + maxY) + SHAPE_OFFSET;
            float z2 = (float) (dz + maxZ) + SHAPE_OFFSET;
            colorBox(fillBuffer, matrix, x1, y1, z1, x2, y2, z2, fill);
        });
        BufferRenderer.drawWithGlobalProgram(fillBuffer.end());

        RenderSystem.lineWidth(1.5f);
        BufferBuilder lineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) ->
                line(lineBuffer, matrix, dx, dy, dz, x1, y1, z1, x2, y2, z2, line));
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());
    }

    private void colorBox(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        colorQuad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
        colorQuad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        colorQuad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        colorQuad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        colorQuad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        colorQuad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    private void colorQuad(BufferBuilder buffer, Matrix4f matrix, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float[] color) {
        buffer.vertex(matrix, ax, ay, az).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, bx, by, bz).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, cx, cy, cz).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, dx, dy, dz).color(color[0], color[1], color[2], color[3]);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix, double dx, double dy, double dz, double x1, double y1, double z1, double x2, double y2, double z2, float[] color) {
        buffer.vertex(matrix, (float) (dx + x1), (float) (dy + y1), (float) (dz + z1)).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, (float) (dx + x2), (float) (dy + y2), (float) (dz + z2)).color(color[0], color[1], color[2], color[3]);
    }

    private float[] rgba(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        };
    }
}
