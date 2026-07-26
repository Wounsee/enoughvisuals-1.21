package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.WorldEffectShaders;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
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
    private static Trails INSTANCE;

    public final ModeSetting typeMode = add(new ModeSetting(
            "Type", "type", "Trail render type",
            TYPE_3D, TYPE_3D, TYPE_2D, TYPE_COMET
    ));
    public final ColorSetting color = add(new ColorSetting(
            "Color", "color", "Trail color", 0xFFE85CFF
    ));
    public final SliderSetting lifetime = add(new SliderSetting(
            "Lifetime", "lifetime", "Trail lifetime", 1.2, 0.1, 3.0, 0.1, "s"
    ));
    public final SliderSetting size = add(new SliderSetting(
            "Size", "size", "Trail size", 0.5, 0.1, 2.0, 0.05, "m"
    ));
    public final SliderSetting step = add(new SliderSetting(
            "Density", "step", "Distance between trail elements", 0.05, 0.01, 0.5, 0.01, "m"
    ));
    public final SliderSetting height = add(new SliderSetting(
            "Height", "height", "Trail height offset", 0.8, 0.0, 2.0, 0.1, "m"
    ));
    public final SliderSetting glow = add(new SliderSetting(
            "Glow", "glow", "Shader glow strength", 0.75, 0.0, 2.0, 0.05
    ));
    public final BooleanSetting firstPerson = add(new BooleanSetting(
            "First Person", "first_person", "Render in first person", true
    ));
    public final BooleanSetting throughWalls = add(new BooleanSetting(
            "Through Walls", "through_walls", "Render through blocks", false
    ));

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
        long maxAge = Math.max(1L, (long) (lifetime.get() * 1000.0));
        if (comet) maxAge = Math.max(maxAge, 650L);

        Vec3d currentPos = new Vec3d(
                MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY())
                        + (comet ? mc.player.getHeight() * 0.54 : height.get()),
                MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ())
        );

        double pointDistance = comet ? 0.035 : 0.07;
        if (points.isEmpty() || points.get(points.size() - 1).pos.distanceTo(currentPos) > pointDistance) {
            points.add(new TrailPoint(currentPos, now));
        }
        prunePoints(now, maxAge);

        List<TrailPoint> renderPoints = new ArrayList<>(points);
        renderPoints.add(new TrailPoint(currentPos, now));
        if (renderPoints.size() < 2) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

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

        if (typeMode.is(TYPE_3D)) {
            render3D(matrices, camera, cam, renderPoints, now, maxAge);
        } else if (typeMode.is(TYPE_2D)) {
            render2D(matrices, cam, renderPoints, now, maxAge);
        } else {
            renderComet(matrices, camera, cam, renderPoints, now, maxAge);
        }

        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void prunePoints(long now, long maxAge) {
        Iterator<TrailPoint> iterator = points.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().time > maxAge) {
                iterator.remove();
            } else {
                break;
            }
        }
        if (points.size() > 128) {
            points.subList(0, points.size() - 128).clear();
        }
    }

    private void render3D(MatrixStack matrices, Camera camera, Vec3d cam, List<TrailPoint> renderPoints,
                          long now, long maxAge) {
        float seconds = (now % 120_000L) / 1000.0f;
        if (WorldEffectShaders.useSoftParticle(seconds, glow.get().floatValue()) == null) return;
        BufferBuilder buffer = null;
        float particleSize = size.get().floatValue();
        double density = Math.max(0.025, step.get());

        for (int i = 0; i < renderPoints.size() - 1; i++) {
            TrailPoint start = renderPoints.get(i);
            TrailPoint end = renderPoints.get(i + 1);
            double distance = start.pos.distanceTo(end.pos);
            if (distance < 0.001) continue;

            for (double traveled = 0.0; traveled < distance; traveled += density) {
                float fraction = (float) (traveled / distance);
                Vec3d pos = start.pos.lerp(end.pos, fraction);
                long time = (long) MathHelper.lerp(fraction, (double) start.time, (double) end.time);
                float fade = ageFade(time, now, maxAge);
                if (fade <= 0.01f) continue;
                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS,
                            VertexFormats.POSITION_TEXTURE_COLOR
                    );
                }
                appendBillboard(buffer, matrices, camera, cam, pos,
                        particleSize * (0.55f + fade * 0.45f), getColor(i),
                        fade * color.getAlphaF() * 0.8f, i * 11.0f);
            }
        }
        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void render2D(MatrixStack matrices, Vec3d cam, List<TrailPoint> renderPoints, long now, long maxAge) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_STRIP,
                VertexFormats.POSITION_COLOR
        );
        float ribbonHeight = size.get().floatValue();
        for (int i = 0; i < renderPoints.size(); i++) {
            TrailPoint point = renderPoints.get(i);
            float alpha = ageFade(point.time, now, maxAge) * color.getAlphaF();
            int rgb = getColor(i);
            float red = ((rgb >> 16) & 0xFF) / 255f;
            float green = ((rgb >> 8) & 0xFF) / 255f;
            float blue = (rgb & 0xFF) / 255f;
            float x = (float) (point.pos.x - cam.x);
            float y = (float) (point.pos.y - cam.y);
            float z = (float) (point.pos.z - cam.z);
            buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha);
            buffer.vertex(matrix, x, y + ribbonHeight, z).color(red, green, blue, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderComet(MatrixStack matrices, Camera camera, Vec3d cam, List<TrailPoint> renderPoints,
                             long now, long maxAge) {
        List<CurvePoint> curve = interpolateCurve(renderPoints, 4);
        if (curve.size() < 2) return;

        float seconds = (now % 120_000L) / 1000.0f;
        if (WorldEffectShaders.useComet(seconds, glow.get().floatValue()) == null) return;
        float cloudDiameter = size.get().floatValue() * 1.85f;
        double cloudSpacing = Math.max(0.055, size.get() * 0.18);
        List<CurvePoint> fogPath = resampleCurveBackwards(curve, cloudSpacing, 92);
        if (fogPath.isEmpty()) return;

        BufferBuilder fog = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );
        int rgb = color.get() & 0x00FFFFFF;
        for (int i = 0; i < fogPath.size(); i++) {
            CurvePoint point = fogPath.get(i);
            float fade = ageFade(point.time, now, maxAge);
            if (fade <= 0.01f) continue;

            float progress = i / (float) Math.max(1, fogPath.size() - 1);
            float smoothFade = fade * fade * (3.0f - 2.0f * fade);
            int seed = (int) (point.time / 31L) * 0x9E3779B9 + i * 0x85EBCA6B;
            float pulse = 0.92f + MathHelper.sin(seconds * 0.72f + seed * 0.000013f) * 0.08f;
            float diameter = cloudDiameter * pulse * (0.58f + smoothFade * 0.54f);
            float alpha = smoothFade * color.getAlphaF() * (0.105f + progress * 0.045f);

            Vec3d previous = fogPath.get(Math.max(0, i - 1)).pos;
            Vec3d next = fogPath.get(Math.min(fogPath.size() - 1, i + 1)).pos;
            Vec3d tangent = next.subtract(previous);
            if (tangent.lengthSquared() < 1.0E-8) tangent = new Vec3d(0.0, 1.0, 0.0);
            else tangent = tangent.normalize();
            Vec3d side = tangent.crossProduct(cam.subtract(point.pos));
            if (side.lengthSquared() < 1.0E-8) side = new Vec3d(1.0, 0.0, 0.0);
            else side = side.normalize();

            float offsetWave = MathHelper.sin(seed * 0.000021f + seconds * 0.46f);
            Vec3d offset = side.multiply(diameter * 0.11f * offsetWave)
                    .add(0.0, diameter * 0.055f * MathHelper.cos(seed * 0.000017f + seconds * 0.38f), 0.0);
            appendFogBillboard(fog, matrices, camera, cam, point.pos.add(offset),
                    diameter, rgb, alpha, seed, seconds * 2.5f);
            appendFogBillboard(fog, matrices, camera, cam, point.pos.subtract(offset.multiply(0.72)),
                    diameter * 0.74f, rgb, alpha * 0.68f, seed ^ 0x27D4EB2D, -seconds * 1.7f);
        }
        BufferRenderer.drawWithGlobalProgram(fog.end());
    }

    private List<CurvePoint> resampleCurveBackwards(List<CurvePoint> path, double spacing, int maxSamples) {
        List<CurvePoint> reversed = new ArrayList<>();
        CurvePoint cursor = path.get(path.size() - 1);
        reversed.add(cursor);
        double untilNext = Math.max(0.02, spacing);

        for (int index = path.size() - 2; index >= 0 && reversed.size() < maxSamples; index--) {
            CurvePoint end = path.get(index);
            double remaining = cursor.pos.distanceTo(end.pos);
            if (remaining < 1.0E-7) {
                cursor = end;
                continue;
            }

            while (remaining >= untilNext && reversed.size() < maxSamples) {
                double fraction = untilNext / remaining;
                Vec3d position = cursor.pos.lerp(end.pos, fraction);
                long time = (long) MathHelper.lerp(fraction, (double) cursor.time, (double) end.time);
                cursor = new CurvePoint(position, time);
                reversed.add(cursor);
                remaining = cursor.pos.distanceTo(end.pos);
                untilNext = Math.max(0.02, spacing);
            }
            untilNext -= remaining;
            cursor = end;
        }

        List<CurvePoint> chronological = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            chronological.add(reversed.get(index));
        }
        return chronological;
    }

    private void appendFogBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam,
                                    Vec3d pos, float size, int rgb, float alpha, int seed, float motion) {
        if (size <= 0.001f || alpha <= 0.001f) return;
        float half = size * 0.5f;
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        int cellX = Math.floorMod(seed, 127) * 2;
        int cellY = Math.floorMod(seed >>> 9, 127) * 2;
        float rotation = Math.floorMod(seed, 360) + MathHelper.sin(seed * 0.000019f + motion) * 9.0f;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -half, -half, 0.0f)
                .texture(cellX, cellY + 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, -half, 0.0f)
                .texture(cellX + 1.0f, cellY + 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, half, 0.0f)
                .texture(cellX + 1.0f, cellY).color(red, green, blue, alpha);
        buffer.vertex(matrix, -half, half, 0.0f)
                .texture(cellX, cellY).color(red, green, blue, alpha);
        matrices.pop();
    }

    private List<CurvePoint> interpolateCurve(List<TrailPoint> source, int samplesPerSegment) {
        List<CurvePoint> result = new ArrayList<>();
        for (int i = 0; i < source.size() - 1; i++) {
            TrailPoint p0 = source.get(Math.max(0, i - 1));
            TrailPoint p1 = source.get(i);
            TrailPoint p2 = source.get(i + 1);
            TrailPoint p3 = source.get(Math.min(source.size() - 1, i + 2));
            for (int sample = 0; sample < samplesPerSegment; sample++) {
                float delta = sample / (float) samplesPerSegment;
                result.add(new CurvePoint(
                        catmullRom(p0.pos, p1.pos, p2.pos, p3.pos, delta),
                        (long) MathHelper.lerp(delta, (double) p1.time, (double) p2.time)
                ));
            }
        }
        TrailPoint last = source.get(source.size() - 1);
        result.add(new CurvePoint(last.pos, last.time));
        return result;
    }

    private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vec3d(
                0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t
                        + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2
                        + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
                0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t
                        + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2
                        + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
                0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t
                        + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2
                        + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3)
        );
    }

    private void appendBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam, Vec3d pos,
                                 float scale, int rgb, float alpha, float rotation) {
        float half = scale * 0.5f;
        float red = ((rgb >> 16) & 0xFF) / 255f;
        float green = ((rgb >> 8) & 0xFF) / 255f;
        float blue = (rgb & 0xFF) / 255f;
        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(red, green, blue, alpha);
        matrices.pop();
    }

    private float ageFade(long time, long now, long maxAge) {
        float age = MathHelper.clamp((now - time) / (float) maxAge, 0.0f, 1.0f);
        return (float) Math.pow(1.0f - age, 1.12);
    }

    private int getColor(int index) {
        int rgb = color.get() & 0x00FFFFFF;
        float pulse = 0.94f + (float) Math.sin((System.currentTimeMillis() + index * 35L) * 0.004) * 0.06f;
        int red = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * pulse), 0, 255);
        int green = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * pulse), 0, 255);
        int blue = MathHelper.clamp((int) ((rgb & 0xFF) * pulse), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private int brighten(int rgb, float factor) {
        int red = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int green = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int blue = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private record TrailPoint(Vec3d pos, long time) {
    }

    private record CurvePoint(Vec3d pos, long time) {
    }
}
