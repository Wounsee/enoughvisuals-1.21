package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Register(name = "Kill Effect", id = "kill_effect", description = "Animated burn copy on player kill", category = Category.VISUAL)
public class KillEffect extends Module {
    private static final long CANDIDATE_TTL_MS = 2_600L;
    private static KillEffect INSTANCE;

    public final ColorSetting accent = add(new ColorSetting(
            "Акцент",
            "accent",
            "Цвет тления и свечения",
            0xFFA777FF
    ));

    public final SliderSetting lifetime = add(new SliderSetting(
            "Время",
            "lifetime",
            "Сколько длится прогорание копии",
            4.2,
            2.0,
            8.0,
            0.05,
            "s"
    ));

    public final SliderSetting lift = add(new SliderSetting(
            "Подлет",
            "lift",
            "Насколько высоко подлетает копия",
            0.85,
            0.0,
            2.0,
            0.05,
            "m"
    ));

    private final Map<Integer, StoredPlayer> candidates = new HashMap<>();
    private final List<ActiveEffect> effects = new ArrayList<>();

    public KillEffect() {
        INSTANCE = this;
    }

    public static boolean active() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static void rememberCandidate(Entity target) {
        if (!active() || !(target instanceof PlayerEntity player) || player == mc.player) return;
        INSTANCE.candidates.put(player.getId(), new StoredPlayer(player, System.currentTimeMillis() + CANDIDATE_TTL_MS));
    }

    public static void onKill(Entity target, int entityId) {
        if (!active()) return;

        StoredPlayer stored;
        if (target instanceof PlayerEntity player && player != mc.player) {
            stored = new StoredPlayer(player, System.currentTimeMillis() + CANDIDATE_TTL_MS);
        } else {
            stored = INSTANCE.candidates.get(entityId);
        }

        if (stored != null) {
            INSTANCE.spawn(stored);
            INSTANCE.candidates.remove(entityId);
        }
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (!active() || mc.player == null || mc.world == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        candidates.clear();
        effects.clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            candidates.clear();
            effects.clear();
            return;
        }
        long now = System.currentTimeMillis();
        candidates.entrySet().removeIf(entry -> now > entry.getValue().expiresAtMs);
    }

    private void spawn(StoredPlayer stored) {
        if (mc.world == null) return;
        OtherClientPlayerEntity copy = stored.createCopy();
        effects.add(new ActiveEffect(copy, stored.pos, stored.bodyYaw, stored.seed, lifetime.get().floatValue(), lift.get().floatValue()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void render(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || camera == null || consumers == null) return;

        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        long now = System.nanoTime();
        Vec3d cam = camera.getPos();
        int accentColor = accent.get() & 0x00FFFFFF;

        Iterator<ActiveEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();
            float age = (now - effect.startedNs) / 1_000_000_000.0f;
            float progress = MathHelper.clamp(age / effect.lifeSeconds, 0.0f, 1.0f);
            if (progress >= 1.0f) {
                iterator.remove();
                continue;
            }

            float liftProgress = liftCurve(progress);
            double yOffset = effect.lift * liftProgress;
            float spinDelaySeconds = effect.lifeSeconds * 0.16f;
            float spinProgress = MathHelper.clamp((age - spinDelaySeconds) / (effect.lifeSeconds - spinDelaySeconds), 0.0f, 1.0f);
            float spin = 455.0f * easeOutSine(spinProgress)
                    + MathHelper.sin(spinProgress * (float) Math.PI * 2.4f) * 6.0f * (1.0f - spinProgress);
            float sway = MathHelper.sin(progress * (float) Math.PI * 2.0f) * 0.035f * (1.0f - progress);
            float drift = MathHelper.sin(progress * (float) Math.PI * 3.5f + effect.seed * 0.01f) * 0.018f * (1.0f - progress);

            effect.copy.age = effect.baseAge + (int) (age * 20.0f);
            effect.copy.updatePositionAndAngles(0.0, 0.0, 0.0, effect.storedYaw, effect.copy.getPitch());
            effect.copy.setHeadYaw(effect.storedHeadYaw);
            effect.copy.setBodyYaw(effect.storedBodyYaw);

            matrices.push();
            matrices.translate(effect.pos.x + sway - cam.x, effect.pos.y + yOffset - cam.y, effect.pos.z + drift - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(effect.pos.add(0.0, yOffset, 0.0)));
            float burnProgress = spinProgress;
            VertexConsumerProvider tinted = new AlphaTintProvider(
                    consumers,
                    burnProgress,
                    effect.seed,
                    accentColor,
                    effect.spots,
                    effect.pos.x + sway - cam.x,
                    effect.pos.y + yOffset - cam.y,
                    effect.pos.z + drift - cam.z,
                    spin,
                    false
            );
            EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(effect.copy);
            EntityRenderState state = (EntityRenderState) renderer.getAndUpdateRenderState(effect.copy, tickDelta);
            renderer.render(state, matrices, tinted, light);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }

            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.disableDepthTest();
            VertexConsumerProvider edgeGlow = new AlphaTintProvider(
                    consumers,
                    burnProgress,
                    effect.seed,
                    accentColor,
                    effect.spots,
                    effect.pos.x + sway - cam.x,
                    effect.pos.y + yOffset - cam.y,
                    effect.pos.z + drift - cam.z,
                    spin,
                    true
            );
            renderer.render(state, matrices, edgeGlow, light);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();

            matrices.pop();

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    private float liftCurve(float progress) {
        float floaty = MathHelper.sin(progress * (float) Math.PI) * 0.09f;
        float micro = MathHelper.sin(progress * (float) Math.PI * 5.0f) * 0.018f * (1.0f - progress);
        return MathHelper.clamp(progress + floaty + micro, 0.0f, 1.0f);
    }

    private static float easeOutSine(float value) {
        return MathHelper.sin(MathHelper.clamp(value, 0.0f, 1.0f) * (float) Math.PI * 0.5f);
    }

    private static final class StoredPlayer {
        final GameProfile profile;
        final Vec3d pos;
        final float yaw;
        final float pitch;
        final float headYaw;
        final float bodyYaw;
        final int age;
        final int seed;
        final net.minecraft.item.ItemStack mainHand;
        final net.minecraft.item.ItemStack offHand;
        final long expiresAtMs;

        StoredPlayer(PlayerEntity player, long expiresAtMs) {
            this.profile = player.getGameProfile();
            this.pos = player.getPos();
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.headYaw = player.headYaw;
            this.bodyYaw = player.bodyYaw;
            this.age = player.age;
            this.seed = player.getId() * 9973 + player.age * 37;
            this.mainHand = player.getStackInHand(Hand.MAIN_HAND).copy();
            this.offHand = player.getStackInHand(Hand.OFF_HAND).copy();
            this.expiresAtMs = expiresAtMs;
        }

        OtherClientPlayerEntity createCopy() {
            GameProfile profileCopy = new GameProfile(profile.getId() == null ? UUID.randomUUID() : profile.getId(), profile.getName());
            profileCopy.getProperties().putAll(profile.getProperties());
            OtherClientPlayerEntity copy = new OtherClientPlayerEntity(mc.world, profileCopy);
            copy.updatePositionAndAngles(0.0, 0.0, 0.0, yaw, pitch);
            copy.prevYaw = yaw;
            copy.prevPitch = pitch;
            copy.headYaw = headYaw;
            copy.prevHeadYaw = headYaw;
            copy.bodyYaw = bodyYaw;
            copy.prevBodyYaw = bodyYaw;
            copy.age = age;
            copy.setStackInHand(Hand.MAIN_HAND, mainHand.copy());
            copy.setStackInHand(Hand.OFF_HAND, offHand.copy());
            return copy;
        }
    }

    private static final class ActiveEffect {
        final OtherClientPlayerEntity copy;
        final Vec3d pos;
        final float storedYaw;
        final float storedHeadYaw;
        final float storedBodyYaw;
        final int baseAge;
        final int seed;
        final BurnSpot[] spots;
        final long startedNs;
        final float lifeSeconds;
        final float lift;

        ActiveEffect(OtherClientPlayerEntity copy, Vec3d pos, float bodyYaw, int seed, float lifeSeconds, float lift) {
            this.copy = copy;
            this.pos = pos;
            this.storedYaw = copy.getYaw();
            this.storedHeadYaw = copy.headYaw;
            this.storedBodyYaw = bodyYaw;
            this.baseAge = copy.age;
            this.seed = seed;
            this.spots = BurnSpot.create(seed);
            this.startedNs = System.nanoTime();
            this.lifeSeconds = lifeSeconds;
            this.lift = lift;
        }
    }

    private record BurnSpot(float x, float y, float z, float maxRadius, float power, float yScale) {
        static BurnSpot[] create(int seed) {
            BurnSpot[] spots = new BurnSpot[14];
            for (int i = 0; i < spots.length; i++) {
                float side = hash(seed, i, 3) < 0.5f ? -1.0f : 1.0f;
                float x = side * (0.08f + hash(seed, i, 5) * 0.70f);
                float y = 0.04f + hash(seed, i, 7) * 1.88f;
                float z = (hash(seed, i, 11) - 0.5f) * 0.72f;
                float maxRadius = 1.45f + hash(seed, i, 17) * 1.95f;
                float power = 1.42f + hash(seed, i, 19) * 0.58f;
                float yScale = 0.58f + hash(seed, i, 23) * 0.92f;
                spots[i] = new BurnSpot(x, y, z, maxRadius, power, yScale);
            }
            return spots;
        }

        private static float hash(int seed, int index, int salt) {
            int x = seed + index * 73428767 + salt * 912931;
            x ^= x << 13;
            x ^= x >>> 17;
            x ^= x << 5;
            return (x & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
        }
    }

    private static final class AlphaTintProvider implements VertexConsumerProvider {
        private final VertexConsumerProvider delegate;
        private final float progress;
        private final int seed;
        private final int accent;
        private final BurnSpot[] spots;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final float spin;
        private final boolean edgeOnly;

        private AlphaTintProvider(VertexConsumerProvider delegate, float progress, int seed, int accent, BurnSpot[] spots,
                                  double originX, double originY, double originZ, float spin, boolean edgeOnly) {
            this.delegate = delegate;
            this.progress = progress;
            this.seed = seed;
            this.accent = accent;
            this.spots = spots;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.spin = spin;
            this.edgeOnly = edgeOnly;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return new AlphaTintConsumer(delegate.getBuffer(layer), progress, seed, accent, spots, originX, originY, originZ, spin, edgeOnly);
        }
    }

    private static final class AlphaTintConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float progress;
        private final int seed;
        private final int accent;
        private final BurnSpot[] spots;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final float inverseSpinCos;
        private final float inverseSpinSin;
        private final Vector3f transformed = new Vector3f();
        private final VertexData[] quad = new VertexData[4];
        private float lastX;
        private float lastY;
        private float lastZ;
        private VertexData current;
        private int quadSize;
        private final boolean edgeOnly;

        private AlphaTintConsumer(VertexConsumer delegate, float progress, int seed, int accent, BurnSpot[] spots,
                                  double originX, double originY, double originZ, float spin, boolean edgeOnly) {
            this.delegate = delegate;
            this.progress = progress;
            this.seed = seed;
            this.accent = accent;
            this.spots = spots;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.edgeOnly = edgeOnly;
            float inverseSpin = (float) Math.toRadians(-spin);
            this.inverseSpinCos = MathHelper.cos(inverseSpin);
            this.inverseSpinSin = MathHelper.sin(inverseSpin);
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            captureTransformed(x, y, z);
            current = new VertexData();
            current.x = x;
            current.y = y;
            current.z = z;
            current.localX = lastX;
            current.localY = lastY;
            current.localZ = lastZ;
            return this;
        }

        @Override
        public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
            matrix.transformPosition(x, y, z, transformed);
            return vertex(transformed.x(), transformed.y(), transformed.z());
        }

        @Override
        public VertexConsumer vertex(MatrixStack.Entry matrix, float x, float y, float z) {
            return vertex(matrix.getPositionMatrix(), x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (current != null) {
                current.red = red;
                current.green = green;
                current.blue = blue;
                current.alpha = alpha;
            }
            return this;
        }

        private void captureTransformed(float x, float y, float z) {
            float px = (float) (x - originX);
            float py = (float) (y - originY);
            float pz = (float) (z - originZ);
            lastX = px * inverseSpinCos + pz * inverseSpinSin;
            lastY = py;
            lastZ = -px * inverseSpinSin + pz * inverseSpinCos;
        }

        private float dissolveEdge(float x, float y, float z) {
            float dirtyNoise = dirtyNoise(x, y, z) * 0.42f;
            float cellNoise = cellNoise(x, y, z) * 0.26f;
            float flameNoise = flameNoise(x, y, z) * 0.22f;
            float edge = -10.0f;
            float globalProgress = (float) Math.pow(progress, 1.72f);
            for (BurnSpot spot : spots) {
                float spotProgress = MathHelper.clamp(globalProgress * (0.76f + spot.power() * 0.18f), 0.0f, 1.0f);
                float radius = spot.maxRadius() * spotProgress - 0.18f;
                float spotEdge = circularBurn(x, y, z, spot.x(), spot.y(), spot.z(), radius, spot.yScale());
                edge = Math.max(edge, spotEdge);
            }
            return edge + dirtyNoise + cellNoise + flameNoise;
        }

        private float circularBurn(float x, float y, float z, float cx, float cy, float cz, float radius, float yScale) {
            float dx = x - cx;
            float dy = (y - cy) * yScale;
            float dz = z - cz;
            return radius - MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        }

        private float vertexNoise(float x, float y, float z) {
            double value = Math.sin(x * 12.9898 + y * 78.233 + z * 37.719 + seed * 0.013);
            return (float) (value - Math.floor(value));
        }

        private float dirtyNoise(float x, float y, float z) {
            float n1 = vertexNoise(x * 1.7f + 0.31f, y * 1.7f - 0.17f, z * 1.7f + 0.53f);
            float n2 = vertexNoise(x * 4.6f - 0.21f, y * 4.6f + 0.43f, z * 4.6f - 0.39f);
            float n3 = vertexNoise(x * 9.0f + 0.67f, y * 9.0f - 0.71f, z * 9.0f + 0.19f);
            return (n1 * 0.50f + n2 * 0.34f + n3 * 0.16f) - 0.5f;
        }

        private float flameNoise(float x, float y, float z) {
            float vertical = MathHelper.sin((x * 3.2f + z * 4.1f + seed * 0.017f) + progress * 2.0f);
            float lick = MathHelper.sin((y * 11.5f + x * 5.4f) + seed * 0.021f);
            return (vertical * 0.45f + lick * 0.55f) * (0.35f + progress * 0.65f);
        }

        private float cellNoise(float x, float y, float z) {
            int ix = MathHelper.floor(x * 5.5f);
            int iy = MathHelper.floor(y * 5.5f);
            int iz = MathHelper.floor(z * 5.5f);
            int h = seed;
            h ^= ix * 73428767;
            h ^= iy * 912931;
            h ^= iz * 42343;
            h ^= h << 13;
            h ^= h >>> 17;
            h ^= h << 5;
            return ((h & 0x7FFFFFFF) / (float) 0x7FFFFFFF) - 0.5f;
        }

        private float smoothstep(float edge0, float edge1, float value) {
            float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
            return t * t * (3.0f - 2.0f * t);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            if (current != null) {
                current.u = u;
                current.v = v;
            }
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            if (current != null) {
                current.overlayU = u;
                current.overlayV = v;
            }
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            if (current != null) {
                current.lightU = u;
                current.lightV = v;
            }
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            if (current != null) {
                current.normalX = x;
                current.normalY = y;
                current.normalZ = z;
                pushVertex(current);
                current = null;
            }
            return this;
        }

        private void pushVertex(VertexData vertex) {
            quad[quadSize++] = vertex;
            if (quadSize == 4) {
                emitSubdividedQuad();
                quadSize = 0;
            }
        }

        private void emitSubdividedQuad() {
            int steps = 8;
            for (int y = 0; y < steps; y++) {
                float v0 = y / (float) steps;
                float v1 = (y + 1) / (float) steps;
                for (int x = 0; x < steps; x++) {
                    float u0 = x / (float) steps;
                    float u1 = (x + 1) / (float) steps;
                    emitVertex(sample(u0, v0));
                    emitVertex(sample(u1, v0));
                    emitVertex(sample(u1, v1));
                    emitVertex(sample(u0, v1));
                }
            }
        }

        private VertexData sample(float u, float v) {
            VertexData a = lerp(quad[0], quad[1], u);
            VertexData b = lerp(quad[3], quad[2], u);
            return lerp(a, b, v);
        }

        private VertexData lerp(VertexData a, VertexData b, float t) {
            VertexData out = new VertexData();
            out.x = MathHelper.lerp(t, a.x, b.x);
            out.y = MathHelper.lerp(t, a.y, b.y);
            out.z = MathHelper.lerp(t, a.z, b.z);
            out.localX = MathHelper.lerp(t, a.localX, b.localX);
            out.localY = MathHelper.lerp(t, a.localY, b.localY);
            out.localZ = MathHelper.lerp(t, a.localZ, b.localZ);
            out.u = MathHelper.lerp(t, a.u, b.u);
            out.v = MathHelper.lerp(t, a.v, b.v);
            out.red = MathHelper.lerp(t, a.red, b.red);
            out.green = MathHelper.lerp(t, a.green, b.green);
            out.blue = MathHelper.lerp(t, a.blue, b.blue);
            out.alpha = MathHelper.lerp(t, a.alpha, b.alpha);
            out.overlayU = MathHelper.lerp(t, a.overlayU, b.overlayU);
            out.overlayV = MathHelper.lerp(t, a.overlayV, b.overlayV);
            out.lightU = MathHelper.lerp(t, a.lightU, b.lightU);
            out.lightV = MathHelper.lerp(t, a.lightV, b.lightV);
            out.normalX = MathHelper.lerp(t, a.normalX, b.normalX);
            out.normalY = MathHelper.lerp(t, a.normalY, b.normalY);
            out.normalZ = MathHelper.lerp(t, a.normalZ, b.normalZ);
            return out;
        }

        private void emitVertex(VertexData vertex) {
            int[] color = colorFor(vertex);
            delegate.vertex(vertex.x, vertex.y, vertex.z)
                    .color(color[0], color[1], color[2], color[3])
                    .texture(vertex.u, vertex.v)
                    .overlay((int) vertex.overlayU, (int) vertex.overlayV)
                    .light((int) vertex.lightU, (int) vertex.lightV)
                    .normal(vertex.normalX, vertex.normalY, vertex.normalZ);
        }

        private int[] colorFor(VertexData vertex) {
            float burnEdge = dissolveEdge(vertex.localX, vertex.localY, vertex.localZ);
            float burnMask = smoothstep(0.24f, 0.62f, burnEdge);
            float outerGlow = smoothstep(-1.10f, -0.16f, burnEdge) * (1.0f - smoothstep(0.72f, 1.72f, burnEdge));
            float innerGlow = smoothstep(-0.54f, 0.24f, burnEdge) * (1.0f - smoothstep(0.86f, 1.92f, burnEdge));
            float ember = MathHelper.clamp(outerGlow * 0.82f + innerGlow * 1.18f, 0.0f, 1.0f);
            int ar = (accent >> 16) & 0xFF;
            int ag = (accent >> 8) & 0xFF;
            int ab = accent & 0xFF;
            int hotR = Math.min(255, ar + 62);
            int hotG = Math.min(255, ag + 58);
            int hotB = Math.min(255, ab + 72);

            if (edgeOnly) {
                int edgeAlpha = MathHelper.clamp((int) (vertex.alpha * ember * 2.35f), 0, 255);
                return new int[]{hotR, hotG, hotB, edgeAlpha};
            }

            float mix = MathHelper.clamp(ember * 1.75f, 0.0f, 1.0f);
            int r = (int) MathHelper.lerp(mix, vertex.red, hotR);
            int g = (int) MathHelper.lerp(mix, vertex.green, hotG);
            int b = (int) MathHelper.lerp(mix, vertex.blue, hotB);
            float edgeFill = Math.max(0.0f, ember - burnMask * 0.12f) * 1.18f;
            int a = MathHelper.clamp((int) (vertex.alpha * Math.max(1.0f - burnMask, edgeFill)), 0, 255);
            return new int[]{r, g, b, a};
        }

        private static final class VertexData {
            float x;
            float y;
            float z;
            float localX;
            float localY;
            float localZ;
            float u;
            float v;
            float red = 255.0f;
            float green = 255.0f;
            float blue = 255.0f;
            float alpha = 255.0f;
            float overlayU;
            float overlayV;
            float lightU;
            float lightV;
            float normalX;
            float normalY = 1.0f;
            float normalZ;
        }
    }
}
