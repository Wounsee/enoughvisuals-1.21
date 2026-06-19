package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

@Register(name = "Block Overlay", id = "block_overlay", description = "Портальный оверлей на блок под прицелом", category = Category.VISUAL)
public class BlockOverlay extends Module {
    private static final float OFFSET = 0.01f;
    private static final float SHADER_SCALE = 3.15f;
    private static final float ANIMATION_RATE = 15.5f;

    private static final Identifier COSMOS = Identifier.of("enough", "textures/sky/custom_world/block_overlay_cosmos.png");
    private static final Identifier FOG_NEBULA = Identifier.of("enough", "textures/sky/custom_world/block_overlay_fog_nebula.png");
    private static final ShaderProgramKey EFFECT_SHADER = new ShaderProgramKey(
            Identifier.of("enough", "core/block_overlay"),
            VertexFormats.POSITION_TEXTURE_COLOR,
            Defines.EMPTY
    );

    private static BlockOverlay INSTANCE;

    private BlockPos targetPos;
    private double renderX;
    private double renderY;
    private double renderZ;
    private float visibility;
    private float visualScale = 0.9f;
    private long lastAnimationNs;
    private boolean shapeReady;
    private double shapeMinX;
    private double shapeMinY;
    private double shapeMinZ;
    private double shapeMaxX = 1.0;
    private double shapeMaxY = 1.0;
    private double shapeMaxZ = 1.0;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Обычная цветная заливка или шейдерная текстура",
            "Шейдерный",
            "Шейдерный",
            "Обычный"
    ));
    public final ModeSetting texture = add(new ModeSetting(
            "Текстура",
            "texture",
            "Текстура шейдерной заливки",
            "Cosmos",
            "Cosmos",
            "Fog Nebula",
            "Web",
            "Plasma"
    ));
    public final ColorSetting shaderColor = add(new ColorSetting("Цвет шейдера", "shader_color", "Цвет шейдерного слоя", 0xFFFFFFFF));
    public final SliderSetting shaderAlpha = add(new SliderSetting("Прозрачность", "shader_alpha", "Сила шейдерной заливки", 0.6, 0.05, 1.0, 0.05));
    public final SliderSetting speed = add(new SliderSetting("Скорость", "speed", "Скорость движения текстуры", 0.3, 0.0, 2.0, 0.05));
    public final ColorSetting normalFill = add(new ColorSetting("Заливка", "normal_fill", "Цвет обычной заливки", 0x553B8CFF));
    public final ColorSetting normalOutline = add(new ColorSetting("Обводка", "normal_outline", "Цвет обычной обводки", 0xFFE8D8FF));
    public final SliderSetting normalWidth = add(new SliderSetting("Толщина", "normal_width", "Толщина обычной обводки", 1.5, 1.0, 5.0, 0.1));
    public final BooleanSetting smooth = add(new BooleanSetting("Плавность", "smooth", "Плавная анимация движения и масштаба", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Через стены", "through_walls", "Рисовать сквозь стены", false));

    public BlockOverlay() {
        INSTANCE = this;
        texture.visibleWhen(this::isShaderMode);
        shaderColor.visibleWhen(this::isShaderMode);
        shaderAlpha.visibleWhen(this::isShaderMode);
        speed.visibleWhen(this::isShaderMode);
        normalFill.visibleWhen(this::isNormalMode);
        normalOutline.visibleWhen(this::isNormalMode);
        normalWidth.visibleWhen(this::isNormalMode);
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    private void render(WorldRenderContext context) {
        updateTarget(mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null);
        tickAnimation();
        if (targetPos == null && visibility <= 0.01f) return;

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Vec3d camera = context.camera().getPos();

        beginState();
        BlockPos renderPos = targetPos != null ? targetPos : lastVisiblePos;
        if (renderPos != null) {
            renderBlock(matrix, camera, renderPos, renderX, renderY, renderZ, visibility);
        }
        endState();
    }

    private BlockPos lastVisiblePos;

    private void updateTarget(BlockPos newTarget) {
        if (newTarget == null) {
            targetPos = null;
            return;
        }
        if (newTarget.equals(targetPos)) return;

        targetPos = newTarget.toImmutable();
        lastVisiblePos = targetPos;
        visualScale = Math.min(visualScale, 0.98f);
        if (visibility <= 0.01f) {
            renderX = targetPos.getX();
            renderY = targetPos.getY();
            renderZ = targetPos.getZ();
            shapeReady = false;
        }
    }

    private void tickAnimation() {
        long now = System.nanoTime();
        float seconds = lastAnimationNs == 0L ? 1.0f / 60.0f : (now - lastAnimationNs) / 1_000_000_000.0f;
        lastAnimationNs = now;
        seconds = Math.max(0.0f, Math.min(0.1f, seconds));
        float follow = 1.0f - (float) Math.exp(-ANIMATION_RATE * seconds);

        if (!smooth.get()) {
            if (targetPos == null) {
                visibility = 0f;
                return;
            }
            visibility = 1f;
            visualScale = 1f;
            renderX = targetPos.getX();
            renderY = targetPos.getY();
            renderZ = targetPos.getZ();
            updateShapeBounds(1f);
            return;
        }

        if (targetPos == null) {
            visibility *= 1.0f - follow;
            return;
        }
        visibility += (1.0f - visibility) * follow;
        visualScale += (1.0f - visualScale) * follow;
        renderX += (targetPos.getX() - renderX) * follow;
        renderY += (targetPos.getY() - renderY) * follow;
        renderZ += (targetPos.getZ() - renderZ) * follow;
        updateShapeBounds(follow);
    }

    private void updateShapeBounds(float follow) {
        if (targetPos == null || mc.world == null) return;

        BlockState state = mc.world.getBlockState(targetPos);
        if (state.isAir()) return;

        VoxelShape shape = state.getOutlineShape(mc.world, targetPos, ShapeContext.of(mc.player));
        if (shape.isEmpty()) return;

        Box bounds = shape.getBoundingBox();
        if (!shapeReady || visibility <= 0.01f) {
            setShapeBounds(bounds);
            shapeReady = true;
            return;
        }

        shapeMinX += (bounds.minX - shapeMinX) * follow;
        shapeMinY += (bounds.minY - shapeMinY) * follow;
        shapeMinZ += (bounds.minZ - shapeMinZ) * follow;
        shapeMaxX += (bounds.maxX - shapeMaxX) * follow;
        shapeMaxY += (bounds.maxY - shapeMaxY) * follow;
        shapeMaxZ += (bounds.maxZ - shapeMaxZ) * follow;
    }

    private void setShapeBounds(Box bounds) {
        shapeMinX = bounds.minX;
        shapeMinY = bounds.minY;
        shapeMinZ = bounds.minZ;
        shapeMaxX = bounds.maxX;
        shapeMaxY = bounds.maxY;
        shapeMaxZ = bounds.maxZ;
    }

    private void renderBlock(Matrix4f matrix, Vec3d camera, BlockPos pos, double smoothX, double smoothY, double smoothZ, float progress) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;

        VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player));
        if (shape.isEmpty()) return;

        double dx = smoothX - camera.x;
        double dy = smoothY - camera.y;
        double dz = smoothZ - camera.z;

        VoxelShape renderShape = animatedShape(shape);

        if (isNormalMode()) {
            renderNormal(matrix, dx, dy, dz, renderShape, progress, visualScale);
        } else {
            renderShader(matrix, dx, dy, dz, renderShape, progress, visualScale);
        }
    }

    private VoxelShape animatedShape(VoxelShape fallback) {
        if (!shapeReady) return fallback;

        double minX = Math.min(shapeMinX, shapeMaxX);
        double minY = Math.min(shapeMinY, shapeMaxY);
        double minZ = Math.min(shapeMinZ, shapeMaxZ);
        double maxX = Math.max(shapeMinX, shapeMaxX);
        double maxY = Math.max(shapeMinY, shapeMaxY);
        double maxZ = Math.max(shapeMinZ, shapeMaxZ);

        if (maxX - minX < 0.001 || maxY - minY < 0.001 || maxZ - minZ < 0.001) {
            return fallback;
        }
        return VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void beginState() {
        previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private int previousDepthFunc = GL11.GL_LEQUAL;
    private boolean previousDepthMask = true;

    private void endState() {
        RenderSystem.depthFunc(previousDepthFunc);
        RenderSystem.depthMask(previousDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderShader(Matrix4f matrix, double dx, double dy, double dz, VoxelShape shape, float progress, float scale) {
        float scroll = (System.currentTimeMillis() % 1_000_000L) / 1000f * speed.get().floatValue();
        float[] color = rgba(shaderColor.get(), shaderAlpha.get().floatValue() * progress);

        if (isProceduralTexture()) {
            ShaderProgram shader = RenderSystem.setShader(EFFECT_SHADER);
            if (shader == null) return;
            setUniform(shader, "Time", scroll);
            setUniform(shader, "Mode", effectModeIndex());
            setUniform(shader, "TintColor", color[0], color[1], color[2], 1.0f);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, textureId());
        }

        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        RenderSystem.depthMask(true);
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float) (dx + scaleCoord(minX, scale)) - OFFSET;
            float y1 = (float) (dy + scaleCoord(minY, scale)) - OFFSET;
            float z1 = (float) (dz + scaleCoord(minZ, scale)) - OFFSET;
            float x2 = (float) (dx + scaleCoord(maxX, scale)) + OFFSET;
            float y2 = (float) (dy + scaleCoord(maxY, scale)) + OFFSET;
            float z2 = (float) (dz + scaleCoord(maxZ, scale)) + OFFSET;
            float ux1 = (float) minX;
            float uy1 = (float) minY;
            float uz1 = (float) minZ;
            float ux2 = (float) maxX;
            float uy2 = (float) maxY;
            float uz2 = (float) maxZ;

            texturedBox(bb, matrix, x1, y1, z1, x2, y2, z2, ux1, uy1, uz1, ux2, uy2, uz2, scroll, color);
        });
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.depthMask(false);
        drawShaderOutline(matrix, dx, dy, dz, shape, progress, scale);
    }

    private void renderNormal(Matrix4f matrix, double dx, double dy, double dz, VoxelShape shape, float progress, float scale) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder fill = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float[] fillColor = rgba(normalFill.get(), progress);
        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float) (dx + scaleCoord(minX, scale)) - OFFSET;
            float y1 = (float) (dy + scaleCoord(minY, scale)) - OFFSET;
            float z1 = (float) (dz + scaleCoord(minZ, scale)) - OFFSET;
            float x2 = (float) (dx + scaleCoord(maxX, scale)) + OFFSET;
            float y2 = (float) (dy + scaleCoord(maxY, scale)) + OFFSET;
            float z2 = (float) (dz + scaleCoord(maxZ, scale)) + OFFSET;
            colorBox(fill, matrix, x1, y1, z1, x2, y2, z2, fillColor);
        });
        BufferRenderer.drawWithGlobalProgram(fill.end());

        drawNormalOutline(matrix, dx, dy, dz, shape, progress, scale);
    }

    private Identifier textureId() {
        if (texture.is("Fog Nebula")) return FOG_NEBULA;
        return COSMOS;
    }

    private boolean isProceduralTexture() {
        return effectModeIndex() != 0;
    }

    private int effectModeIndex() {
        return switch (texture.get().toLowerCase(Locale.ROOT)) {
            case "web" -> 2;
            case "plasma" -> 3;
            default -> 0;
        };
    }

    private void drawShaderOutline(Matrix4f matrix, double dx, double dy, double dz, VoxelShape shape, float progress, float scale) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.35f);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        float alpha = 0.82f * progress;
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> line(lines, matrix, dx, dy, dz, x1, y1, z1, x2, y2, z2, scale, 0.86f, 0.78f, 1.0f, alpha));
        BufferRenderer.drawWithGlobalProgram(lines.end());
        RenderSystem.depthFunc(depthFunc);
    }

    private void drawNormalOutline(Matrix4f matrix, double dx, double dy, double dz, VoxelShape shape, float progress, float scale) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(normalWidth.get().floatValue());
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        float[] color = rgba(normalOutline.get(), progress);
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) -> line(lines, matrix, dx, dy, dz, x1, y1, z1, x2, y2, z2, scale, color[0], color[1], color[2], color[3]));
        BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void line(BufferBuilder bb, Matrix4f matrix, double dx, double dy, double dz,
                      double x1, double y1, double z1, double x2, double y2, double z2,
                      float scale, float r, float g, float b, float a) {
        bb.vertex(matrix, (float) (dx + scaleCoord(x1, scale)), (float) (dy + scaleCoord(y1, scale)), (float) (dz + scaleCoord(z1, scale))).color(r, g, b, a);
        bb.vertex(matrix, (float) (dx + scaleCoord(x2, scale)), (float) (dy + scaleCoord(y2, scale)), (float) (dz + scaleCoord(z2, scale))).color(r, g, b, a);
    }

    private static double scaleCoord(double value, float scale) {
        return 0.5 + (value - 0.5) * scale;
    }

    private void texturedBox(BufferBuilder bb, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float ux1, float uy1, float uz1,
                             float ux2, float uy2, float uz2,
                             float scroll, float[] c) {
        texturedQuad(bb, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, ux1, uz1, ux2, uz2, scroll, c);
        texturedQuad(bb, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, ux1, uz1, ux2, uz2, scroll, c);
        texturedQuad(bb, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, ux1, uy1, ux2, uy2, scroll, c);
        texturedQuad(bb, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, ux1, uy1, ux2, uy2, scroll, c);
        texturedQuad(bb, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, uz1, uy1, uz2, uy2, scroll, c);
        texturedQuad(bb, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, uz1, uy1, uz2, uy2, scroll, c);
    }

    private void texturedQuad(BufferBuilder bb, Matrix4f m,
                              float ax, float ay, float az,
                              float bx, float by, float bz,
                              float cx, float cy, float cz,
                              float dx, float dy, float dz,
                              float u1, float v1, float u2, float v2,
                              float scroll, float[] c) {
        texturedVertex(bb, m, ax, ay, az, u1 / SHADER_SCALE + scroll * 0.07f, v1 / SHADER_SCALE - scroll * 0.04f, c);
        texturedVertex(bb, m, bx, by, bz, u1 / SHADER_SCALE + scroll * 0.07f, v2 / SHADER_SCALE - scroll * 0.04f, c);
        texturedVertex(bb, m, cx, cy, cz, u2 / SHADER_SCALE + scroll * 0.07f, v2 / SHADER_SCALE - scroll * 0.04f, c);
        texturedVertex(bb, m, dx, dy, dz, u2 / SHADER_SCALE + scroll * 0.07f, v1 / SHADER_SCALE - scroll * 0.04f, c);
    }

    private void texturedVertex(BufferBuilder bb, Matrix4f m, float x, float y, float z, float u, float v, float[] c) {
        bb.vertex(m, x, y, z).texture(u, v).color(c[0], c[1], c[2], c[3]);
    }

    private void colorBox(BufferBuilder bb, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float[] c) {
        colorQuad(bb, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, c);
        colorQuad(bb, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, c);
        colorQuad(bb, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, c);
        colorQuad(bb, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, c);
        colorQuad(bb, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, c);
        colorQuad(bb, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, c);
    }

    private void colorQuad(BufferBuilder bb, Matrix4f m,
                           float ax, float ay, float az,
                           float bx, float by, float bz,
                           float cx, float cy, float cz,
                           float dx, float dy, float dz,
                           float[] c) {
        bb.vertex(m, ax, ay, az).color(c[0], c[1], c[2], c[3]);
        bb.vertex(m, bx, by, bz).color(c[0], c[1], c[2], c[3]);
        bb.vertex(m, cx, cy, cz).color(c[0], c[1], c[2], c[3]);
        bb.vertex(m, dx, dy, dz).color(c[0], c[1], c[2], c[3]);
    }

    private float[] rgba(int argb, float alphaMultiplier) {
        return new float[]{
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >> 24) & 0xFF) / 255f * alphaMultiplier
        };
    }

    private static void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform == null) return;

        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    private static void setUniform(ShaderProgram shader, String name, int value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private boolean isShaderMode() {
        return mode.is("Шейдерный");
    }

    private boolean isNormalMode() {
        return mode.is("Обычный");
    }

    @Override
    protected void onDisable() {
        targetPos = null;
        lastVisiblePos = null;
        visibility = 0.0f;
        visualScale = 0.9f;
        lastAnimationNs = 0L;
    }
}
