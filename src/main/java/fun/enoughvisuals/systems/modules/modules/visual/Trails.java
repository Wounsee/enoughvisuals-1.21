package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Register(name = "Trails", id = "trails", description = "Trail behind the player", category = Category.VISUAL)
public class Trails extends Module {

    private static final String TYPE_3D = "3D";
    private static final String TYPE_2D = "2D";
    private static final String TYPE_COMET = "Comet";
    private static final Identifier GLOW_TEXTURE = Identifier.of("enough", "textures/bloom.png");
    private static Trails INSTANCE;

    public final ModeSetting typeMode = add(new ModeSetting("Type", "type", "Trail render type", TYPE_3D, TYPE_3D, TYPE_2D, TYPE_COMET));
    public final ColorSetting color = add(new ColorSetting("Color", "color", "Trail color", 0xFFE85CFF));
    public final SliderSetting lifetime = add(new SliderSetting("Lifetime", "lifetime", "Trail lifetime", 1.2, 0.1, 3.0, 0.1, "s"));
    public final SliderSetting size = add(new SliderSetting("Size", "size", "Trail size", 0.5, 0.1, 2.0, 0.05, "m"));
    public final SliderSetting step = add(new SliderSetting("Density", "step", "Distance between 3D glow sprites", 0.05, 0.01, 0.5, 0.01, "m"));
    public final SliderSetting height = add(new SliderSetting("Height", "height", "Trail height offset", 0.8, 0.0, 2.0, 0.1, "m"));
    public final BooleanSetting firstPerson = add(new BooleanSetting("First Person", "first_person", "Render in first person", true));
    public final BooleanSetting throughWalls = add(new BooleanSetting("Through Walls", "through_walls", "Render through blocks", false));

    private final List<TrailPoint> points = new ArrayList<>();

    public Trails() {
        INSTANCE = this;
        height.visibleWhen(() -> !typeMode.is(TYPE_COMET));
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        points.clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            points.clear();
        }
    }

    private void render(WorldRenderContext context) {
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !firstPerson.get()) return;

        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        long now = System.currentTimeMillis();
        boolean comet = typeMode.is(TYPE_COMET);
        long maxAge = Math.max(1L, (long) (lifetime.get() * (comet ? 1400.0 : 1000.0)));
        if (comet) {
            maxAge = Math.max(maxAge, 1300L);
        }

        double playerX = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double playerY = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY())
                + (comet ? mc.player.getHeight() * 0.54 : height.get());
        double playerZ = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());
        Vec3d currentPos = new Vec3d(playerX, playerY, playerZ);

        if (points.isEmpty() || points.get(points.size() - 1).pos.distanceTo(currentPos) > 0.1) {
            points.add(new TrailPoint(currentPos, now));
        }

        Iterator<TrailPoint> iterator = points.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().time > maxAge) {
                iterator.remove();
            } else {
                break;
            }
        }

        List<TrailPoint> renderPoints = new ArrayList<>(points);
        renderPoints.add(new TrailPoint(currentPos, now));
        if (renderPoints.size() < 2) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }

        if (typeMode.is(TYPE_3D)) {
            render3D(matrices, camera, cam, renderPoints, now, maxAge);
        } else if (comet) {
            renderComet(matrices, camera, cam, renderPoints, now, maxAge);
        } else {
            render2D(matrices, cam, renderPoints, now, maxAge);
        }

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void render3D(MatrixStack matrices, Camera camera, Vec3d cam, List<TrailPoint> pointsToRender, long now, long maxAge) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        BufferBuilder buffer = null;
        int rendered = 0;

        float particleSize = size.get().floatValue();
        double density = Math.max(0.01, step.get());

        for (int i = 0; i < pointsToRender.size() - 1; i++) {
            TrailPoint start = pointsToRender.get(i);
            TrailPoint end = pointsToRender.get(i + 1);
            double distance = start.pos.distanceTo(end.pos);
            if (distance < 0.001) continue;

            for (double d = 0.0; d < distance; d += density) {
                double fraction = d / distance;
                double x = MathHelper.lerp(fraction, start.pos.x, end.pos.x);
                double y = MathHelper.lerp(fraction, start.pos.y, end.pos.y);
                double z = MathHelper.lerp(fraction, start.pos.z, end.pos.z);
                double time = MathHelper.lerp(fraction, (double) start.time, (double) end.time);

                float age = (float) ((now - time) / (double) maxAge);
                float alpha = MathHelper.clamp(1.0f - age, 0.0f, 1.0f) * color.getAlphaF();
                if (alpha <= 0.01f) continue;

                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                }

                matrices.push();
                matrices.translate(x - cam.x, y - cam.y, z - cam.z);
                matrices.multiply(camera.getRotation());
                appendTexturedQuad(buffer, matrices.peek().getPositionMatrix(), particleSize * (1.0f - age * 0.3f), getColor(i), alpha);
                matrices.pop();
                rendered++;
            }
        }

        if (buffer != null && rendered > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void render2D(MatrixStack matrices, Vec3d cam, List<TrailPoint> pointsToRender, long now, long maxAge) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float ribbonHeight = size.get().floatValue();

        for (int i = 0; i < pointsToRender.size(); i++) {
            TrailPoint point = pointsToRender.get(i);
            float age = (now - point.time) / (float) maxAge;
            float alpha = MathHelper.clamp(1.0f - age, 0.0f, 1.0f) * color.getAlphaF();
            int rgb = getColor(i);
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float x = (float) (point.pos.x - cam.x);
            float y = (float) (point.pos.y - cam.y);
            float z = (float) (point.pos.z - cam.z);

            buffer.vertex(matrix, x, y, z).color(r, g, b, alpha);
            buffer.vertex(matrix, x, y + ribbonHeight, z).color(r, g, b, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderComet(MatrixStack matrices, Camera camera, Vec3d cam, List<TrailPoint> pointsToRender, long now, long maxAge) {
        List<PlumeSample> samples = buildPlumeSamples(pointsToRender, cam, now, maxAge);
        if (samples.size() < 2) return;

        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder plume = null;
        int rendered = 0;
        int widthSegments = 8;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float[][] layers = new float[][]{
                {0.00f, 1.00f, 1.00f, 0.00f},
                {0.22f, 0.34f, 0.88f, 0.62f},
                {-0.18f, 0.28f, 0.82f, 1.24f},
                {0.03f, 0.18f, 1.18f, 1.86f}
        };

        for (float[] layer : layers) {
            float offset = layer[0];
            float alphaMul = layer[1];
            float widthMul = layer[2];
            float phase = layer[3];

            for (int i = 0; i < samples.size() - 1; i++) {
                PlumeSample a = samples.get(i);
                PlumeSample b = samples.get(i + 1);
                if (a.alpha <= 0.004f && b.alpha <= 0.004f) continue;

                for (int j = 0; j < widthSegments; j++) {
                    float u1 = -1.0f + j * 2.0f / widthSegments;
                    float u2 = -1.0f + (j + 1) * 2.0f / widthSegments;

                    if (plume == null) {
                        plume = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                    }
                    appendCometVertex(plume, matrix, a, u1, offset, alphaMul, widthMul, phase, cam);
                    appendCometVertex(plume, matrix, b, u1, offset, alphaMul, widthMul, phase, cam);
                    appendCometVertex(plume, matrix, b, u2, offset, alphaMul, widthMul, phase, cam);
                    appendCometVertex(plume, matrix, a, u2, offset, alphaMul, widthMul, phase, cam);
                    rendered++;
                }
            }
        }

        if (plume != null && rendered > 0) {
            BufferRenderer.drawWithGlobalProgram(plume.end());
        }
    }

    private List<PlumeSample> buildPlumeSamples(List<TrailPoint> pointsToRender, Vec3d cam, long now, long maxAge) {
        List<PlumeSample> samples = new ArrayList<>();
        double density = Math.max(0.045, step.get() * 1.05);
        float baseSize = size.get().floatValue();
        int rgb = color.get() & 0x00FFFFFF;

        for (int i = 0; i < pointsToRender.size() - 1; i++) {
            TrailPoint start = pointsToRender.get(i);
            TrailPoint end = pointsToRender.get(i + 1);
            double distance = start.pos.distanceTo(end.pos);
            if (distance < 0.001) continue;

            for (double d = 0.0; d < distance; d += density) {
                double fraction = d / distance;
                double time = MathHelper.lerp(fraction, (double) start.time, (double) end.time);
                float age = MathHelper.clamp((float) ((now - time) / (double) maxAge), 0.0f, 1.0f);
                float life = 1.0f - age;
                if (life <= 0.01f) continue;

                Vec3d center = new Vec3d(
                        MathHelper.lerp(fraction, start.pos.x, end.pos.x),
                        MathHelper.lerp(fraction, start.pos.y, end.pos.y),
                        MathHelper.lerp(fraction, start.pos.z, end.pos.z)
                );
                Vec3d tangent = end.pos.subtract(start.pos);
                if (tangent.lengthSquared() < 1.0E-5) tangent = new Vec3d(0.0, 0.0, 1.0);
                tangent = tangent.normalize();

                Vec3d side = new Vec3d(-tangent.z, 0.0, tangent.x);
                if (side.lengthSquared() < 1.0E-5) side = new Vec3d(1.0, 0.0, 0.0);
                side = side.normalize();
                Vec3d up = new Vec3d(0.0, 1.0, 0.0);

                float tail = age;
                float radius = baseSize * (0.08f + (float) Math.pow(tail, 0.72f) * 0.96f + MathHelper.sin(tail * (float) Math.PI) * 0.12f);
                radius = MathHelper.clamp(radius, baseSize * 0.06f, Math.min(baseSize * 1.18f, 1.08f));
                float wave = (now * 0.0027f) + samples.size() * 0.32f;
                center = center
                        .add(side.multiply(MathHelper.sin(wave) * radius * 0.12f))
                        .add(up.multiply(MathHelper.cos(wave * 0.72f) * radius * 0.07f));

                float headFade = smooth(MathHelper.clamp(age / 0.18f, 0.0f, 1.0f));
                float tailFade = smooth(MathHelper.clamp(life / 0.36f, 0.0f, 1.0f));
                float body = 0.34f + MathHelper.sin(tail * (float) Math.PI) * 0.66f;
                float alpha = color.getAlphaF() * headFade * tailFade * body * 0.34f;
                int sampleColor = plumeColor(rgb, samples.size(), tail, now);
                samples.add(new PlumeSample(center, side, up, radius, sampleColor, alpha));
            }
        }

        return samples;
    }

    private int plumeColor(int rgb, int index, float tail, long now) {
        float warm = 0.06f + MathHelper.sin((now + index * 41L) * 0.003f) * 0.035f;
        float bright = 0.78f + (1.0f - tail) * 0.18f;
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * (bright + warm)), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * bright), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * (bright + 0.06f)), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private void appendCometVertex(BufferBuilder buffer, Matrix4f matrix, PlumeSample sample, float u,
                                   float layerOffset, float alphaMul, float widthMul, float phase, Vec3d cam) {
        float edge = 1.0f - MathHelper.clamp(Math.abs(u), 0.0f, 1.0f);
        float center = smooth(edge);

        float ripple = MathHelper.sin((float) (sample.center.x * 1.6 + sample.center.z * 1.35 + u * 1.9 + phase));
        float arch = (1.0f - u * u) * 0.18f;
        Vec3d pos = sample.center
                .add(sample.side.multiply(u * sample.radius * 1.28f * widthMul))
                .add(sample.up.multiply((layerOffset + arch + ripple * 0.045f) * sample.radius))
                .subtract(cam);

        int rgb = brighten(sample.rgb, 0.92f + center * 0.22f);
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float alpha = sample.alpha * alphaMul * center;
        buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(r, g, b, alpha);
    }

    private int brighten(int rgb, float factor) {
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private float smooth(float value) {
        value = MathHelper.clamp(value, 0.0f, 1.0f);
        return value * value * (3.0f - 2.0f * value);
    }

    private void appendTexturedQuad(BufferBuilder buffer, Matrix4f matrix, float scale, int rgb, float alpha) {
        float h = scale * 0.5f;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        buffer.vertex(matrix, -h, -h, 0f).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, -h, 0f).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, h, 0f).texture(1f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, -h, h, 0f).texture(0f, 0f).color(r, g, b, alpha);
    }

    private int getColor(int index) {
        int rgb = color.get() & 0x00FFFFFF;
        float pulse = 0.9f + (float) Math.sin((System.currentTimeMillis() + index * 35L) * 0.004) * 0.1f;
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * pulse), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * pulse), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * pulse), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private record TrailPoint(Vec3d pos, long time) {
    }

    private record PlumeSample(Vec3d center, Vec3d side, Vec3d up, float radius, int rgb, float alpha) {
    }
}
