package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.render.WorldEffectShaders;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Register(
        name = "World Squares",
        id = "world_squares",
        description = "Animated volumetric network of round nodes and smooth links",
        category = Category.VISUAL
)
public class WorldSquares extends Module {

    private static final int LINKS_PER_NODE = 3;
    private static final double MAX_RENDER_DISTANCE_SQ = 220.0 * 220.0;
    private static WorldSquares INSTANCE;

    public final ColorSetting gridColor = add(new ColorSetting(
            "Link Color", "grid_color", "Color of smooth volumetric connections", 0xFF7299FF
    ));
    public final ColorSetting accentColor = add(new ColorSetting(
            "Node Color", "accent_color", "Color of round shader nodes", 0xFFB7C8FF
    ));
    public final SliderSetting opacity = add(new SliderSetting(
            "Opacity", "opacity", "Overall effect visibility", 0.34, 0.04, 1.0, 0.01
    ));
    public final SliderSetting radius = add(new SliderSetting(
            "Radius", "radius", "Horizontal node spawn radius", 24.0, 6.0, 128.0, 1.0, "m"
    ));
    public final SliderSetting height = add(new SliderSetting(
            "Vertical Radius", "height", "Node spawn radius above and below the player",
            10.0, 3.0, 64.0, 1.0, "m"
    ));
    public final SliderSetting amount = add(new SliderSetting(
            "Amount", "amount", "Number of animated round nodes",
            95.0, 16.0, 180.0, 1.0
    ));
    public final SliderSetting speed = add(new SliderSetting(
            "Speed", "speed", "Free node movement speed",
            0.72, 0.1, 2.4, 0.05
    ));
    public final SliderSetting connectionDistance = add(new SliderSetting(
            "Link Distance", "link_distance", "Maximum smooth connection distance",
            10.5, 2.0, 28.0, 0.1, "m"
    ));
    public final SliderSetting nodeSize = add(new SliderSetting(
            "Node Size", "node_size", "Diameter of round shader nodes",
            0.24, 0.06, 0.65, 0.01, "m"
    ));
    public final SliderSetting glow = add(new SliderSetting(
            "Glow", "glow", "Shader glow strength",
            0.8, 0.0, 2.0, 0.05
    ));
    public final BooleanSetting throughWalls = add(new BooleanSetting(
            "Through Walls", "through_walls", "Render through blocks", false
    ));

    private final Random random = new Random();
    private final List<NetworkNode> nodes = new ArrayList<>();
    private final Map<Long, LinkState> links = new HashMap<>();
    private long lastUpdateNs;
    private int nextNodeId;

    public WorldSquares() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            clear();
            return;
        }

        long nowNs = System.nanoTime();
        float deltaSeconds = lastUpdateNs == 0L
                ? 0.05f
                : MathHelper.clamp((nowNs - lastUpdateNs) / 1_000_000_000.0f, 0.001f, 0.1f);
        lastUpdateNs = nowNs;

        resizeNodes();
        updateNodes(deltaSeconds);
        updateLinks(deltaSeconds);
    }

    private void clear() {
        nodes.clear();
        links.clear();
        lastUpdateNs = 0L;
        nextNodeId = 0;
    }

    private void resizeNodes() {
        int target = amount.get().intValue();
        Vec3d center = effectCenter();
        while (nodes.size() < target) {
            nodes.add(createNode(center));
        }
        while (nodes.size() > target) {
            NetworkNode removed = nodes.remove(nodes.size() - 1);
            links.entrySet().removeIf(entry -> entry.getValue().contains(removed.id));
        }
    }

    private NetworkNode createNode(Vec3d center) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * radius.get() * 0.92;
        double vertical = (random.nextDouble() * 2.0 - 1.0) * height.get() * 0.92;
        Vec3d pos = center.add(Math.cos(angle) * distance, vertical, Math.sin(angle) * distance);
        Vec3d velocity = randomDirection().multiply(speed.get() * (0.52 + random.nextDouble() * 0.42));
        return new NetworkNode(nextNodeId++, pos, velocity, random.nextFloat() * 1000.0f);
    }

    private void updateNodes(float deltaSeconds) {
        Vec3d center = effectCenter();
        double horizontalSoftLimit = radius.get() * 0.76;
        double horizontalHardLimit = radius.get() * 1.34;
        double verticalSoftLimit = height.get() * 0.76;
        double verticalHardLimit = height.get() * 1.34 + 1.0;
        double targetSpeed = speed.get();

        for (int index = 0; index < nodes.size(); index++) {
            NetworkNode node = nodes.get(index);
            node.prevPos = node.pos;
            node.phase += deltaSeconds;
            node.age += deltaSeconds;

            Vec3d offset = node.pos.subtract(center);
            double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (horizontalDistance > horizontalHardLimit || Math.abs(offset.y) > verticalHardLimit) {
                nodes.set(index, createNode(center));
                continue;
            }

            double phase = node.phase + node.seed;
            Vec3d wander = new Vec3d(
                    Math.sin(phase * 0.71) + Math.cos(phase * 0.39 + 0.7),
                    Math.sin(phase * 0.49 + 1.4) * 0.62,
                    Math.cos(phase * 0.63) + Math.sin(phase * 0.43 + 0.3)
            );
            if (wander.lengthSquared() > 1.0E-7) {
                wander = wander.normalize().multiply(targetSpeed);
            }

            double horizontalPressure = MathHelper.clamp(
                    (horizontalDistance - horizontalSoftLimit) / Math.max(1.0, radius.get() - horizontalSoftLimit),
                    0.0, 1.0
            );
            double verticalPressure = MathHelper.clamp(
                    (Math.abs(offset.y) - verticalSoftLimit) / Math.max(1.0, height.get() - verticalSoftLimit),
                    0.0, 1.0
            );
            double pressure = Math.max(horizontalPressure, verticalPressure);
            if (pressure > 0.0 && offset.lengthSquared() > 1.0E-7) {
                wander = wander.add(offset.normalize().multiply(-targetSpeed * (0.7 + pressure * 1.8)));
            }

            double steering = 1.0 - Math.exp(-deltaSeconds * 2.45);
            node.velocity = node.velocity.add(wander.subtract(node.velocity).multiply(steering));
            double velocityLength = node.velocity.length();
            double maxSpeed = targetSpeed * 1.45;
            if (velocityLength > maxSpeed) {
                node.velocity = node.velocity.multiply(maxSpeed / velocityLength);
            }
            node.pos = node.pos.add(node.velocity.multiply(deltaSeconds));
        }
    }

    private void updateLinks(float deltaSeconds) {
        for (LinkState state : links.values()) {
            state.prevAlpha = state.alpha;
            state.target = 0.0f;
        }

        double maxDistance = connectionDistance.get();
        double maxDistanceSq = maxDistance * maxDistance;
        for (int first = 0; first < nodes.size(); first++) {
            int[] nearest = {-1, -1, -1};
            double[] nearestDistanceSq = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

            for (int second = 0; second < nodes.size(); second++) {
                if (first == second) continue;
                double distanceSq = nodes.get(first).pos.squaredDistanceTo(nodes.get(second).pos);
                if (distanceSq >= maxDistanceSq || distanceSq >= nearestDistanceSq[LINKS_PER_NODE - 1]) continue;

                for (int slot = 0; slot < LINKS_PER_NODE; slot++) {
                    if (distanceSq >= nearestDistanceSq[slot]) continue;
                    for (int shift = LINKS_PER_NODE - 1; shift > slot; shift--) {
                        nearestDistanceSq[shift] = nearestDistanceSq[shift - 1];
                        nearest[shift] = nearest[shift - 1];
                    }
                    nearestDistanceSq[slot] = distanceSq;
                    nearest[slot] = second;
                    break;
                }
            }

            for (int slot = 0; slot < LINKS_PER_NODE; slot++) {
                int second = nearest[slot];
                if (second < 0) continue;
                NetworkNode a = nodes.get(first);
                NetworkNode b = nodes.get(second);
                double normalized = 1.0 - Math.sqrt(nearestDistanceSq[slot]) / maxDistance;
                float strength = 0.32f + 0.68f * (float) Math.pow(normalized, 0.58);
                long key = linkKey(a.id, b.id);
                LinkState state = links.computeIfAbsent(key, ignored -> new LinkState(a.id, b.id));
                state.target = Math.max(state.target, strength);
            }
        }

        float easing = (float) (1.0 - Math.exp(-deltaSeconds * 4.4));
        Iterator<LinkState> iterator = links.values().iterator();
        while (iterator.hasNext()) {
            LinkState state = iterator.next();
            state.alpha += (state.target - state.alpha) * easing;
            if (state.target <= 0.001f && state.alpha <= 0.006f) {
                iterator.remove();
            }
        }
    }

    private void render(WorldRenderContext context) {
        if (nodes.isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        float seconds = (System.nanoTime() % 120_000_000_000L) / 1_000_000_000.0f;
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
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Map<Integer, RenderNode> renderNodes = new HashMap<>();
        for (NetworkNode node : nodes) {
            Vec3d pos = node.prevPos.lerp(node.pos, tickDelta);
            if (cam.squaredDistanceTo(pos) <= MAX_RENDER_DISTANCE_SQ) {
                renderNodes.put(node.id, new RenderNode(node, pos));
            }
        }

        renderLinks(matrices, cam, renderNodes, tickDelta, seconds);
        renderNodes(matrices, camera, cam, renderNodes, seconds);

        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderLinks(MatrixStack matrices, Vec3d cam, Map<Integer, RenderNode> renderNodes,
                             float tickDelta, float seconds) {
        if (renderNodes.isEmpty() || WorldEffectShaders.useLink(seconds, glow.get().floatValue()) == null) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = null;
        int rgb = gridColor.get() & 0x00FFFFFF;
        float width = MathHelper.clamp(nodeSize.get().floatValue() * 0.17f, 0.018f, 0.085f);
        float baseAlpha = opacity.get().floatValue() * gridColor.getAlphaF();

        for (LinkState link : links.values()) {
            RenderNode first = renderNodes.get(link.firstId);
            RenderNode second = renderNodes.get(link.secondId);
            float alpha = MathHelper.lerp(tickDelta, link.prevAlpha, link.alpha);
            if (first == null || second == null || alpha <= 0.004f) continue;
            if (buffer == null) {
                buffer = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR
                );
            }
            appendLink(buffer, matrix, cam, first.pos, second.pos, width, rgb, baseAlpha * alpha);
        }

        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderNodes(MatrixStack matrices, Camera camera, Vec3d cam,
                             Map<Integer, RenderNode> renderNodes, float seconds) {
        if (renderNodes.isEmpty()
                || WorldEffectShaders.useSoftParticle(seconds, glow.get().floatValue()) == null) return;

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );
        int rgb = accentColor.get() & 0x00FFFFFF;
        float diameter = nodeSize.get().floatValue() * 2.55f;
        float baseAlpha = opacity.get().floatValue() * accentColor.getAlphaF();

        for (RenderNode renderNode : renderNodes.values()) {
            NetworkNode node = renderNode.node;
            float spawn = MathHelper.clamp(node.age * 2.8f, 0.0f, 1.0f);
            float pulse = 0.92f + MathHelper.sin(seconds * 2.2f + node.seed) * 0.08f;
            appendBillboard(buffer, matrices, camera, cam, renderNode.pos,
                    diameter * pulse, rgb, baseAlpha * spawn, node.seed * 17.0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void appendLink(BufferBuilder buffer, Matrix4f matrix, Vec3d cam,
                            Vec3d from, Vec3d to, float halfWidth, int rgb, float alpha) {
        if (alpha <= 0.002f) return;
        Vec3d direction = to.subtract(from);
        if (direction.lengthSquared() < 1.0E-7) return;
        direction = direction.normalize();
        Vec3d view = cam.subtract(from.add(to).multiply(0.5));
        Vec3d side = direction.crossProduct(view);
        if (side.lengthSquared() < 1.0E-7) {
            side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        }
        if (side.lengthSquared() < 1.0E-7) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        side = side.multiply(halfWidth);

        Vec3d a = from.add(side).subtract(cam);
        Vec3d b = from.subtract(side).subtract(cam);
        Vec3d c = to.subtract(side).subtract(cam);
        Vec3d d = to.add(side).subtract(cam);
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z)
                .texture(0.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z)
                .texture(0.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z)
                .texture(1.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z)
                .texture(1.0f, 0.0f).color(red, green, blue, alpha);
    }

    private void appendBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam,
                                 Vec3d pos, float size, int rgb, float alpha, float rotation) {
        if (alpha <= 0.002f) return;
        float half = size * 0.5f;
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -half, -half, 0.0f)
                .texture(0.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, -half, 0.0f)
                .texture(1.0f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, half, half, 0.0f)
                .texture(1.0f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, -half, half, 0.0f)
                .texture(0.0f, 0.0f).color(red, green, blue, alpha);
        matrices.pop();
    }

    private Vec3d effectCenter() {
        return mc.player.getPos().add(0.0, mc.player.getHeight() * 0.52, 0.0);
    }

    private Vec3d randomDirection() {
        Vec3d direction = new Vec3d(
                random.nextDouble() - 0.5,
                (random.nextDouble() - 0.5) * 0.72,
                random.nextDouble() - 0.5
        );
        return direction.lengthSquared() < 1.0E-6
                ? new Vec3d(1.0, 0.0, 0.0)
                : direction.normalize();
    }

    private long linkKey(int firstId, int secondId) {
        int min = Math.min(firstId, secondId);
        int max = Math.max(firstId, secondId);
        return ((long) min << 32) | (max & 0xFFFFFFFFL);
    }

    private record RenderNode(NetworkNode node, Vec3d pos) {
    }

    private static final class NetworkNode {
        final int id;
        final float seed;
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        float phase;
        float age;

        private NetworkNode(int id, Vec3d pos, Vec3d velocity, float seed) {
            this.id = id;
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.seed = seed;
            this.phase = seed * 0.013f;
        }
    }

    private static final class LinkState {
        final int firstId;
        final int secondId;
        float alpha;
        float prevAlpha;
        float target;

        private LinkState(int firstId, int secondId) {
            this.firstId = firstId;
            this.secondId = secondId;
        }

        private boolean contains(int nodeId) {
            return firstId == nodeId || secondId == nodeId;
        }
    }
}
