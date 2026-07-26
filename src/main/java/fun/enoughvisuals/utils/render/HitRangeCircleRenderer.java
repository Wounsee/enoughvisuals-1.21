package fun.enoughvisuals.utils.render;

import fun.enoughvisuals.systems.modules.modules.visual.HitRange;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HitRangeCircleRenderer {
    private static final RenderLayer.MultiPhase DEBUG_LINES = makeLayer(VertexFormat.DrawMode.DEBUG_LINES);
    private static final RenderLayer.MultiPhase DEBUG_QUADS = makeLayer(VertexFormat.DrawMode.QUADS);
    private static final RenderLayer.MultiPhase TRIANGLES = makeLayer(VertexFormat.DrawMode.TRIANGLES);

    private static final List<Angle> angles = new ArrayList<>();

    static {
        computeAngles();
    }

    private HitRangeCircleRenderer() {
    }

    public static void drawCircle(MatrixStack matrices, VertexConsumerProvider vertexConsumers, PlayerEntityRenderState state) {
        HitRange config = HitRange.getInstance();
        if (config == null || !config.isEnabled()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }

        Vec3d entityPos = new Vec3d(state.x, state.y, state.z);

        int color = config.color.get();
        if (config.randomColors.get()) {
            color = state.name.hashCode() | 0xFF000000;
        } else if (
                config.colorWhenInRange.get()
                        && state.id != player.getId()
                        && entityPos.isInRange(player.getPos(), config.radius.get())
        ) {
            color = config.inRangeColor.get();
        }

        float dy = (state.sneaking ? 0.125f : 0.0f) + config.height.get().floatValue();

        HitRange.Mode mode = config.mode();
        RenderLayer layer = switch (mode) {
            case LINE -> DEBUG_LINES;
            case THICK -> DEBUG_QUADS;
            case FILLED -> TRIANGLES;
        };

        VertexConsumer vertices = vertexConsumers.getBuffer(layer);

        matrices.push();
        switch (mode) {
            case LINE -> drawCircleLines(matrices, vertices, dy, color);
            case THICK -> drawCircleQuad(matrices, vertices, dy, color);
            case FILLED -> drawCircleTriangles(matrices, vertices, dy, color);
        }
        matrices.pop();
    }

    private static void drawCircleLines(MatrixStack matrices, VertexConsumer vertices, float dy, int argb) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        int n = angles.size();
        for (int i = 0; i < n; i++) {
            Angle a = angles.get(i);
            Angle b = angles.get((i + 1) % n);
            vertices.vertex(positionMatrix, a.dx, dy, a.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, b.dx, dy, b.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
        }
    }

    private static void drawCircleQuad(MatrixStack matrices, VertexConsumer vertices, float dy, int argb) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        for (int i = 1; i < angles.size() + 1; i++) {
            Angle angle = angles.get(i % angles.size());
            Angle prevAngle = angles.get(i - 1);

            vertices.vertex(positionMatrix, prevAngle.dx, dy, prevAngle.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, prevAngle.farDx, dy, prevAngle.farDz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, angle.farDx, dy, angle.farDz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, angle.dx, dy, angle.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
        }
    }

    private static void drawCircleTriangles(MatrixStack matrices, VertexConsumer vertices, float dy, int argb) {
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        int n = angles.size();
        for (int i = 0; i < n; i++) {
            Angle a = angles.get(i);
            Angle b = angles.get((i + 1) % n);
            vertices.vertex(positionMatrix, 0.0f, dy, 0.0f).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, a.dx, dy, a.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
            vertices.vertex(positionMatrix, b.dx, dy, b.dz).color(argb).normal(matrices.peek(), 0.0f, 0.0f, 0.0f);
        }
    }

    public static void computeAngles() {
        angles.clear();
        HitRange config = HitRange.getInstance();
        if (config == null) return;

        int segments = Math.max(3, config.circleSegments.get().intValue());
        float radius = config.radius.get().floatValue();
        HitRange.Mode mode = config.mode();

        if (mode == HitRange.Mode.THICK) {
            float thickness = Math.max(0.001f, config.thickness.get().floatValue());
            for (int i = 0; i < segments; i++) {
                float angle = 2.0f * MathHelper.PI * ((float) i / segments);
                float inner = radius - (thickness / 2.0f);
                float outer = inner + thickness;

                float dx = inner * MathHelper.sin(angle);
                float dz = inner * MathHelper.cos(angle);

                float farDx = outer * MathHelper.sin(angle);
                float farDz = outer * MathHelper.cos(angle);

                angles.add(new Angle(dx, dz, farDx, farDz));
            }
        } else {
            for (int i = 0; i < segments; i++) {
                float angle = 2.0f * MathHelper.PI * ((float) i / segments);
                float dx = radius * MathHelper.sin(angle);
                float dz = radius * MathHelper.cos(angle);

                angles.add(new Angle(dx, dz));
            }
        }
    }

    private static RenderLayer.MultiPhase makeLayer(VertexFormat.DrawMode mode) {
        String name = "enough_hitrange_" + mode.name().toLowerCase(Locale.ROOT);

        return RenderLayer.of(
                name,
                VertexFormats.POSITION_COLOR,
                mode,
                1536,
                false,
                true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.POSITION_COLOR_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.ENABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .build(false)
        );
    }

    private record Angle(float dx, float dz, float farDx, float farDz) {
        Angle(float dx, float dz) {
            this(dx, dz, 0.0f, 0.0f);
        }
    }
}
