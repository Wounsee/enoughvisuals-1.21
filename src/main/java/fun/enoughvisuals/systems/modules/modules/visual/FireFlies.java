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
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Register(
        name = "FireFlies",
        id = "fire_flies",
        description = "Shader-rounded fireflies living around the player",
        category = Category.VISUAL
)
public class FireFlies extends Module {

    private static final Identifier PARTICLE_ATLAS = Identifier.of("enough", "textures/fireflies/particle_atlas.png");
    private static final String TRAIL_NONE = "None";
    private static final String TRAIL_3D = "3D";
    private static final String TRAIL_PARTICLES = "Particles";
    private static final String PARTICLE_RANDOM = "Random";
    private static final String PARTICLE_GLYPHS = "Glyphs";
    private static final String PARTICLE_STARS = "Stars";
    private static final String PARTICLE_ORB = "Orb";
    private static final String PARTICLE_DASH = "Dash";
    private static final String PARTICLE_CROSS = "Cross";
    private static final String PARTICLE_BAR = "Bar";
    private static final String PARTICLE_ASTERISK = "Asterisk";
    private static final String PARTICLE_STARBURST = "Starburst";
    private static final String PARTICLE_COMET = "Comet";
    private static final String PARTICLE_CLUSTER = "Cluster";
    private static final AtlasSprite ATLAS_ORB = sprite(132, 176, 292, 336);
    private static final AtlasSprite ATLAS_DASH_LEFT = sprite(300, 198, 448, 320);
    private static final AtlasSprite ATLAS_DASH_RIGHT = sprite(456, 198, 605, 320);
    private static final AtlasSprite ATLAS_GLYPHS = sprite(608, 172, 805, 338);
    private static final AtlasSprite ATLAS_CROSS_SMALL = sprite(772, 165, 930, 342);
    private static final AtlasSprite ATLAS_CROSS_LARGE = sprite(950, 160, 1155, 345);
    private static final AtlasSprite ATLAS_BAR = sprite(275, 375, 482, 525);
    private static final AtlasSprite ATLAS_TABBED_BAR = sprite(530, 375, 775, 525);
    private static final AtlasSprite ATLAS_VERTICAL_BAR = sprite(835, 355, 945, 535);
    private static final AtlasSprite ATLAS_ASTERISK = sprite(1000, 350, 1165, 535);
    private static final AtlasSprite ATLAS_GLOW = sprite(172, 575, 310, 750);
    private static final AtlasSprite ATLAS_SPARK = sprite(395, 565, 555, 755);
    private static final AtlasSprite ATLAS_STARBURST = sprite(625, 530, 970, 790);
    private static final AtlasSprite ATLAS_COMET = sprite(960, 565, 1175, 755);
    private static final AtlasSprite ATLAS_CLUSTER = sprite(1170, 530, 1470, 790);
    private static final AtlasSprite[] ALL_SPRITES = {
            ATLAS_ORB, ATLAS_DASH_LEFT, ATLAS_DASH_RIGHT, ATLAS_GLYPHS,
            ATLAS_CROSS_SMALL, ATLAS_CROSS_LARGE, ATLAS_BAR, ATLAS_TABBED_BAR,
            ATLAS_VERTICAL_BAR, ATLAS_ASTERISK, ATLAS_GLOW, ATLAS_SPARK,
            ATLAS_STARBURST, ATLAS_COMET, ATLAS_CLUSTER
    };
    private static final AtlasSprite[] GLYPH_SPRITES = {
            ATLAS_DASH_LEFT, ATLAS_DASH_RIGHT, ATLAS_GLYPHS, ATLAS_CROSS_SMALL,
            ATLAS_CROSS_LARGE, ATLAS_BAR, ATLAS_TABBED_BAR, ATLAS_VERTICAL_BAR, ATLAS_ASTERISK
    };
    private static final AtlasSprite[] STAR_SPRITES = {
            ATLAS_GLOW, ATLAS_SPARK, ATLAS_STARBURST, ATLAS_COMET, ATLAS_CLUSTER
    };
    private static final AtlasSprite[] DASH_SPRITES = {ATLAS_DASH_LEFT, ATLAS_DASH_RIGHT};
    private static final AtlasSprite[] CROSS_SPRITES = {ATLAS_CROSS_SMALL, ATLAS_CROSS_LARGE};
    private static final AtlasSprite[] BAR_SPRITES = {ATLAS_BAR, ATLAS_TABBED_BAR, ATLAS_VERTICAL_BAR};
    private static FireFlies INSTANCE;

    public final ColorSetting headColor = add(new ColorSetting(
            "Head Color", "head_color", "Firefly head color", 0xFFFFF2B0
    ));
    public final SliderSetting headSize = add(new SliderSetting(
            "Head Size", "head_size", "Radius of the shader-rounded head",
            0.22, 0.08, 0.8, 0.01, "m"
    ));
    public final SliderSetting glow = add(new SliderSetting(
            "Glow", "glow", "Shader glow strength", 0.85, 0.0, 2.5, 0.05
    ));
    public final BooleanSetting randomColors = add(new BooleanSetting(
            "Random Colors", "random_colors", "Give every firefly its own color", true
    ));
    public final SliderSetting amount = add(new SliderSetting(
            "Amount", "count", "Number of living fireflies",
            100.0, 10.0, 300.0, 10.0
    ));
    public final SliderSetting radius = add(new SliderSetting(
            "Spawn Radius", "radius", "Horizontal spawn radius around the player",
            25.0, 10.0, 256.0, 5.0, "m"
    ));
    public final SliderSetting height = add(new SliderSetting(
            "Vertical Radius", "height", "Vertical spawn radius above and below the player",
            10.0, 5.0, 128.0, 5.0, "m"
    ));
    public final SliderSetting speed = add(new SliderSetting(
            "Speed", "movement_speed", "Movement per game tick",
            0.15, 0.05, 0.5, 0.05
    ));
    public final ModeSetting trail = add(new ModeSetting(
            "Trail", "trail", "Trail rendering style",
            TRAIL_3D, TRAIL_NONE, TRAIL_3D, TRAIL_PARTICLES
    ));
    public final ColorSetting trailColor = add(new ColorSetting(
            "Trail Color", "trail_color", "Color of rounded trail points", 0xFFB6E9FF
    ));
    public final SliderSetting trailLength = add(new SliderSetting(
            "Trail Length", "trail_points", "Number of stored trail points",
            20.0, 5.0, 40.0, 1.0, " pts"
    ));
    public final SliderSetting trailSize = add(new SliderSetting(
            "Trail Size", "trail_size", "Size of rounded or atlas trail particles",
            0.15, 0.04, 0.6, 0.01, "m"
    ));
    public final ModeSetting atlasParticle = add(new ModeSetting(
            "Particle Type", "particle_type", "Particle type from the attached atlas",
            PARTICLE_STARS,
            PARTICLE_RANDOM, PARTICLE_GLYPHS, PARTICLE_STARS, PARTICLE_ORB,
            PARTICLE_DASH, PARTICLE_CROSS, PARTICLE_BAR, PARTICLE_ASTERISK,
            PARTICLE_STARBURST, PARTICLE_COMET, PARTICLE_CLUSTER
    ));
    public final SliderSetting trailSpacing = add(new SliderSetting(
            "Particle Spacing", "trail_spacing", "Gap between atlas trail particles",
            0.32, 0.08, 1.2, 0.01, "m"
    ));
    public final BooleanSetting trailParticles = add(new BooleanSetting(
            "Trail Particles", "trail_particles", "Small rounded particles around trails", true
    ));
    public final ColorSetting particleColor = add(new ColorSetting(
            "Particle Color", "particle_color", "Color of small trail particles", 0xFFFFFFFF
    ));
    public final SliderSetting particleSize = add(new SliderSetting(
            "Particle Size", "particle_size", "Size of small trail particles",
            0.065, 0.02, 0.28, 0.005, "m"
    ));
    public final SliderSetting particleRate = add(new SliderSetting(
            "Particle Rate", "particle_rate", "Small particles per firefly per second",
            1.2, 0.2, 4.0, 0.1
    ));
    public final BooleanSetting optimization = add(new BooleanSetting(
            "Optimization", "optimization", "Reduce distant trails while keeping heads visible", true
    ));
    public final BooleanSetting throughWalls = add(new BooleanSetting(
            "Through Walls", "through_walls", "Render through blocks", false
    ));

    private final Random random = new Random();
    private final List<Firefly> fireflies = new ArrayList<>();
    private final List<TrailSpark> sparks = new ArrayList<>();
    private double sparkAccumulator;

    public FireFlies() {
        INSTANCE = this;
        trailColor.visibleWhen(() -> !trail.is(TRAIL_NONE));
        trailLength.visibleWhen(() -> !trail.is(TRAIL_NONE));
        trailSize.visibleWhen(() -> !trail.is(TRAIL_NONE));
        atlasParticle.visibleWhen(() -> trail.is(TRAIL_PARTICLES));
        trailSpacing.visibleWhen(() -> trail.is(TRAIL_PARTICLES));
        trailParticles.visibleWhen(() -> !trail.is(TRAIL_NONE));
        particleColor.visibleWhen(() -> !trail.is(TRAIL_NONE) && trailParticles.get());
        particleSize.visibleWhen(() -> !trail.is(TRAIL_NONE) && trailParticles.get());
        particleRate.visibleWhen(() -> !trail.is(TRAIL_NONE) && trailParticles.get());
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

        resizeFireflies();
        Vec3d playerPos = mc.player.getPos();
        double allowedDistance = Math.max(60.0, Math.sqrt(radius.get() * radius.get() + height.get() * height.get()) * 1.35);
        for (Firefly firefly : fireflies) {
            updateFirefly(firefly, playerPos, allowedDistance);
        }
        updateSparks();
        spawnSparks();
    }

    private void clear() {
        fireflies.clear();
        sparks.clear();
        sparkAccumulator = 0.0;
    }

    private void resizeFireflies() {
        int target = amount.get().intValue();
        while (fireflies.size() < target) {
            fireflies.add(createFirefly());
        }
        if (fireflies.size() > target) {
            fireflies.subList(target, fireflies.size()).clear();
        }
    }

    private Firefly createFirefly() {
        Vec3d playerPos = mc.player.getPos();
        double distance = 5.0 + random.nextDouble() * Math.max(0.0, radius.get() - 5.0);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double vertical = (random.nextDouble() * 2.0 - 1.0) * height.get();
        Vec3d pos = playerPos.add(-Math.sin(angle) * distance, vertical, Math.cos(angle) * distance);

        double yaw = random.nextDouble() * Math.PI * 2.0;
        double pitch = Math.toRadians(-30.0 + random.nextDouble() * 60.0);
        double movement = speed.get();
        Vec3d velocity = new Vec3d(
                -Math.sin(yaw) * Math.cos(pitch) * movement,
                Math.sin(pitch) * movement * 0.5,
                Math.cos(yaw) * Math.cos(pitch) * movement
        );
        int rgb = randomVividColor();
        return new Firefly(pos, velocity, rgb, random.nextInt());
    }

    private void updateFirefly(Firefly firefly, Vec3d playerPos, double allowedDistance) {
        firefly.previousTrail.clear();
        firefly.previousTrail.addAll(firefly.trail);
        firefly.prevPos = firefly.pos;
        firefly.ageTicks++;

        double randomness = 0.01;
        firefly.velocity = firefly.velocity.add(
                (random.nextDouble() - 0.5) * randomness,
                (random.nextDouble() - 0.5) * randomness,
                (random.nextDouble() - 0.5) * randomness
        );
        Vec3d toCenter = playerPos.subtract(firefly.pos);
        double distanceToCenter = toCenter.length();
        double returnStart = allowedDistance * 0.72;
        if (distanceToCenter > returnStart && distanceToCenter > 1.0E-6) {
            double pressure = MathHelper.clamp(
                    (distanceToCenter - returnStart) / Math.max(1.0, allowedDistance - returnStart),
                    0.0, 1.0
            );
            firefly.velocity = firefly.velocity.add(
                    toCenter.normalize().multiply(speed.get() * (0.035 + pressure * 0.12))
            );
        }
        double maxSpeed = speed.get() * 1.5;
        firefly.velocity = new Vec3d(
                MathHelper.clamp(firefly.velocity.x, -maxSpeed, maxSpeed),
                MathHelper.clamp(firefly.velocity.y, -maxSpeed, maxSpeed),
                MathHelper.clamp(firefly.velocity.z, -maxSpeed, maxSpeed)
        );
        firefly.pos = firefly.pos.add(firefly.velocity);

        Vec3d last = firefly.trail.isEmpty() ? null : firefly.trail.get(firefly.trail.size() - 1);
        if (last == null || firefly.pos.squaredDistanceTo(last) > 0.005) {
            firefly.trail.add(firefly.pos);
        }
        int maxTrailLength = trailLength.get().intValue();
        if (firefly.trail.size() > maxTrailLength) {
            firefly.trail.subList(0, firefly.trail.size() - maxTrailLength).clear();
        }
    }

    private void spawnSparks() {
        if (trail.is(TRAIL_NONE) || !trailParticles.get() || fireflies.isEmpty()) {
            sparkAccumulator = 0.0;
            return;
        }
        sparkAccumulator += fireflies.size() * particleRate.get() / 20.0;
        int maxSparks = Math.min(320, Math.max(32, fireflies.size() * 2));
        int safety = 0;
        while (sparkAccumulator >= 1.0 && sparks.size() < maxSparks && safety++ < 24) {
            sparkAccumulator -= 1.0;
            Firefly source = fireflies.get(random.nextInt(fireflies.size()));
            Vec3d origin = source.pos;
            if (!source.trail.isEmpty()) {
                origin = source.trail.get(random.nextInt(source.trail.size()));
            }
            Vec3d spread = randomDirection().multiply(0.04 + random.nextDouble() * 0.18);
            Vec3d velocity = randomDirection().multiply(0.006 + random.nextDouble() * 0.014);
            sparks.add(new TrailSpark(origin.add(spread), velocity,
                    10 + random.nextInt(14), 0.65f + random.nextFloat() * 0.55f, random.nextInt()));
        }
    }

    private void updateSparks() {
        Iterator<TrailSpark> iterator = sparks.iterator();
        while (iterator.hasNext()) {
            TrailSpark spark = iterator.next();
            spark.prevPos = spark.pos;
            spark.ageTicks++;
            if (spark.ageTicks >= spark.lifeTicks) {
                iterator.remove();
                continue;
            }
            spark.velocity = spark.velocity.multiply(0.93).add(0.0, 0.0015, 0.0);
            spark.pos = spark.pos.add(spark.velocity);
        }
    }

    private void render(WorldRenderContext context) {
        if (fireflies.isEmpty()) return;
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
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (trail.is(TRAIL_3D)) {
            renderRoundedTrails(matrices, camera, cam, tickDelta, seconds);
        } else if (trail.is(TRAIL_PARTICLES)) {
            renderAtlasTrails(matrices, camera, cam, tickDelta, seconds);
        }
        renderHeadsAndSparks(matrices, camera, cam, tickDelta, seconds);

        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderRoundedTrails(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, float seconds) {
        if (WorldEffectShaders.useLink(seconds, glow.get().floatValue() * 0.72f) == null) return;
        BufferBuilder buffer = null;
        double trailDistanceSq = optimization.get() ? 96.0 * 96.0 : Double.MAX_VALUE;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (Firefly firefly : fireflies) {
            Vec3d current = lerp(firefly.prevPos, firefly.pos, tickDelta);
            if (cam.squaredDistanceTo(current) > trailDistanceSq) {
                continue;
            }

            double sampleSpacing = Math.max(0.035, trailSize.get() * 0.31);
            int sampleLimit = optimization.get() ? 58 : 96;
            List<Vec3d> smoothTrail = smoothTrail(firefly, tickDelta, sampleSpacing, sampleLimit);
            if (smoothTrail.size() < 2) continue;

            float lifeAlpha = lifeAlpha(firefly, tickDelta);
            int rgb = fireflyColor(firefly, trailColor);
            for (int i = 1; i < smoothTrail.size(); i++) {
                float fade = i / (float) (smoothTrail.size() - 1);
                Vec3d from = smoothTrail.get(i - 1);
                Vec3d to = smoothTrail.get(i);
                float alpha = lifeAlpha * fade * 0.72f * trailColor.getAlphaF();
                if (alpha <= 0.002f || from.squaredDistanceTo(to) < 1.0E-8) continue;
                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS,
                            VertexFormats.POSITION_TEXTURE_COLOR
                    );
                }
                float halfWidth = trailSize.get().floatValue() * (0.18f + fade * 0.5f);
                appendContinuousTrailSegment(buffer, matrix, cam,
                        from, to, halfWidth, rgb, alpha);
            }
        }
        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderAtlasTrails(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, float seconds) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, PARTICLE_ATLAS);
        BufferBuilder buffer = null;
        double trailDistanceSq = optimization.get() ? 112.0 * 112.0 : Double.MAX_VALUE;
        for (Firefly firefly : fireflies) {
            Vec3d current = lerp(firefly.prevPos, firefly.pos, tickDelta);
            if (cam.squaredDistanceTo(current) > trailDistanceSq) continue;

            List<Vec3d> smoothTrail = smoothTrail(
                    firefly,
                    tickDelta,
                    Math.max(0.08, trailSpacing.get()),
                    optimization.get() ? 36 : 64
            );
            if (smoothTrail.size() < 2) continue;

            float lifeAlpha = lifeAlpha(firefly, tickDelta);
            int rgb = fireflyColor(firefly, trailColor);
            int pathSegments = smoothTrail.size() - 1;
            int particleSlots = Math.max(3, Math.min(14, trailLength.get().intValue() / 2));
            for (int ordinal = 0; ordinal < particleSlots; ordinal++) {
                int baseSeed = firefly.seed * 31 + ordinal * 0x9E3779B9;
                float travelDuration = 1.55f;
                float phaseOffset = ordinal / (float) particleSlots;
                float cycles = seconds / travelDuration + phaseOffset;
                int cycle = (int) Math.floor(cycles);
                float phase = cycles - cycle;

                float fadeIn = MathHelper.clamp(phase / 0.08f, 0.0f, 1.0f);
                float fadeOut = MathHelper.clamp((1.0f - phase) / 0.17f, 0.0f, 1.0f);
                fadeIn = fadeIn * fadeIn * (3.0f - 2.0f * fadeIn);
                fadeOut = fadeOut * fadeOut * (3.0f - 2.0f * fadeOut);
                float particleLife = fadeIn * fadeOut;

                int cycleSeed = baseSeed + cycle * 0x27D4EB2D;
                float pathCursor = (1.0f - phase) * pathSegments;
                int pathIndex = MathHelper.clamp((int) Math.floor(pathCursor), 0, pathSegments);
                int nextPathIndex = Math.min(pathSegments, pathIndex + 1);
                float pathDelta = pathCursor - pathIndex;
                float alpha = lifeAlpha * particleLife * 0.84f * trailColor.getAlphaF();
                if (alpha <= 0.002f) continue;
                if (buffer == null) {
                    buffer = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS,
                            VertexFormats.POSITION_TEXTURE_COLOR
                    );
                }

                AtlasSprite sprite = atlasSpriteFor(atlasParticle.get(), cycleSeed);
                Vec3d position = smoothTrail.get(pathIndex).lerp(smoothTrail.get(nextPathIndex), pathDelta);
                float rotation = (cycleSeed & 0xFF) * 1.4f
                        + phase * 8.0f;
                float animatedSize = trailSize.get().floatValue()
                        * (0.78f + (1.0f - phase) * 0.3f)
                        * (0.82f + particleLife * 0.18f);
                appendAtlasBillboard(buffer, matrices, camera, cam, position, sprite,
                        animatedSize, rgb, alpha, rotation);
            }
        }
        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderHeadsAndSparks(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, float seconds) {
        double renderDistance = radius.get() + 32.0;
        double renderDistanceSq = renderDistance * renderDistance;

        if (WorldEffectShaders.useSolidParticle() != null) {
            BufferBuilder heads = null;
            for (Firefly firefly : fireflies) {
                Vec3d pos = lerp(firefly.prevPos, firefly.pos, tickDelta);
                if (cam.squaredDistanceTo(pos) > renderDistanceSq) continue;
                if (heads == null) {
                    heads = Tessellator.getInstance().begin(
                            VertexFormat.DrawMode.QUADS,
                            VertexFormats.POSITION_TEXTURE_COLOR
                    );
                }
                appendBillboard(heads, matrices, camera, cam, pos,
                        headSize.get().floatValue() * 2.0f,
                        fireflyColor(firefly, headColor),
                        1.0f, 0.0f);
            }
            if (heads != null) {
                BufferRenderer.drawWithGlobalProgram(heads.end());
            }
        }

        if (sparks.isEmpty() || WorldEffectShaders.useSoftParticle(seconds, glow.get().floatValue()) == null) return;
        BufferBuilder sparkBuffer = null;
        for (TrailSpark spark : sparks) {
            Vec3d pos = lerp(spark.prevPos, spark.pos, tickDelta);
            if (cam.squaredDistanceTo(pos) > renderDistanceSq) continue;
            float fade = 1.0f - (spark.ageTicks + tickDelta) / spark.lifeTicks;
            float alpha = fade * particleColor.getAlphaF() * 0.72f;
            if (alpha <= 0.002f) continue;
            if (sparkBuffer == null) {
                sparkBuffer = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR
                );
            }
            appendBillboard(sparkBuffer, matrices, camera, cam, pos,
                    particleSize.get().floatValue() * 3.2f * spark.sizeScale,
                    componentColor(particleColor, spark.seed),
                    alpha, spark.seed * 0.17f);
        }
        if (sparkBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(sparkBuffer.end());
        }
    }

    private List<Vec3d> smoothTrail(Firefly firefly, float tickDelta, double spacing, int maxSamples) {
        List<Vec3d> previousCurve = buildTrailCurve(firefly.previousTrail, firefly.prevPos);
        List<Vec3d> currentCurve = buildTrailCurve(firefly.trail, firefly.pos);
        List<Vec3d> previousSamples = resampleBackwards(previousCurve, spacing, maxSamples);
        List<Vec3d> currentSamples = resampleBackwards(currentCurve, spacing, maxSamples);
        int sampleCount = Math.max(previousSamples.size(), currentSamples.size());
        if (sampleCount <= 1) {
            return List.of(lerp(firefly.prevPos, firefly.pos, tickDelta));
        }

        List<Vec3d> interpolated = new ArrayList<>(sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            float normalized = index / (float) (sampleCount - 1);
            Vec3d previous = sampleNormalized(previousSamples, normalized);
            Vec3d current = sampleNormalized(currentSamples, normalized);
            interpolated.add(previous.lerp(current, tickDelta));
        }
        return interpolated;
    }

    private List<Vec3d> buildTrailCurve(List<Vec3d> source, Vec3d endpoint) {
        List<Vec3d> knots = new ArrayList<>(source);
        if (knots.isEmpty()) {
            knots.add(endpoint);
        } else if (knots.get(knots.size() - 1).squaredDistanceTo(endpoint) < 1.0E-9) {
            knots.set(knots.size() - 1, endpoint);
        } else {
            knots.add(endpoint);
        }
        if (knots.size() < 2) return knots;

        List<Vec3d> curve = new ArrayList<>(knots.size() * 4);
        for (int segment = 0; segment < knots.size() - 1; segment++) {
            Vec3d p0 = knots.get(Math.max(0, segment - 1));
            Vec3d p1 = knots.get(segment);
            Vec3d p2 = knots.get(segment + 1);
            Vec3d p3 = knots.get(Math.min(knots.size() - 1, segment + 2));
            for (int sample = 0; sample < 4; sample++) {
                curve.add(catmullRom(p0, p1, p2, p3, sample / 4.0f));
            }
        }
        curve.add(endpoint);
        return curve;
    }

    private Vec3d sampleNormalized(List<Vec3d> samples, float normalized) {
        if (samples.isEmpty()) return Vec3d.ZERO;
        if (samples.size() == 1) return samples.get(0);
        float cursor = MathHelper.clamp(normalized, 0.0f, 1.0f) * (samples.size() - 1);
        int index = MathHelper.clamp((int) Math.floor(cursor), 0, samples.size() - 1);
        int next = Math.min(samples.size() - 1, index + 1);
        return samples.get(index).lerp(samples.get(next), cursor - index);
    }

    private List<Vec3d> resampleBackwards(List<Vec3d> path, double spacing, int maxSamples) {
        List<Vec3d> reversed = new ArrayList<>();
        Vec3d cursor = path.get(path.size() - 1);
        reversed.add(cursor);
        double untilNext = Math.max(0.01, spacing);

        for (int index = path.size() - 2; index >= 0 && reversed.size() < maxSamples; index--) {
            Vec3d end = path.get(index);
            double remaining = cursor.distanceTo(end);
            if (remaining < 1.0E-7) {
                cursor = end;
                continue;
            }

            while (remaining >= untilNext && reversed.size() < maxSamples) {
                cursor = cursor.lerp(end, untilNext / remaining);
                reversed.add(cursor);
                remaining = cursor.distanceTo(end);
                untilNext = Math.max(0.01, spacing);
            }
            untilNext -= remaining;
            cursor = end;
        }

        List<Vec3d> chronological = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) {
            chronological.add(reversed.get(index));
        }
        return chronological;
    }

    private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float delta) {
        double delta2 = delta * delta;
        double delta3 = delta2 * delta;
        return new Vec3d(
                0.5 * ((2.0 * p1.x) + (-p0.x + p2.x) * delta
                        + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * delta2
                        + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * delta3),
                0.5 * ((2.0 * p1.y) + (-p0.y + p2.y) * delta
                        + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * delta2
                        + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * delta3),
                0.5 * ((2.0 * p1.z) + (-p0.z + p2.z) * delta
                        + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * delta2
                        + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * delta3)
        );
    }

    private void appendContinuousTrailSegment(BufferBuilder buffer, Matrix4f matrix, Vec3d cam,
                                              Vec3d from, Vec3d to, float halfWidth,
                                              int rgb, float alpha) {
        if (alpha <= 0.002f) return;
        Vec3d direction = to.subtract(from);
        if (direction.lengthSquared() < 1.0E-8) return;
        direction = direction.normalize();
        Vec3d view = cam.subtract(from.add(to).multiply(0.5));
        Vec3d side = direction.crossProduct(view);
        if (side.lengthSquared() < 1.0E-8) {
            side = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        }
        if (side.lengthSquared() < 1.0E-8) {
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
                .texture(0.16f, 0.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z)
                .texture(0.16f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z)
                .texture(0.84f, 1.0f).color(red, green, blue, alpha);
        buffer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z)
                .texture(0.84f, 0.0f).color(red, green, blue, alpha);
    }

    private float lifeAlpha(Firefly firefly, float tickDelta) {
        return 1.0f;
    }

    private int fireflyColor(Firefly firefly, ColorSetting fallback) {
        return randomColors.get() ? firefly.rgb : fallback.get() & 0x00FFFFFF;
    }

    private int componentColor(ColorSetting setting, int seed) {
        int rgb = setting.get() & 0x00FFFFFF;
        float variation = 0.92f + ((seed >>> 8) & 0xFF) / 255.0f * 0.16f;
        return brighten(rgb, variation);
    }

    private int randomVividColor() {
        float hue = random.nextFloat();
        float saturation = 0.48f + random.nextFloat() * 0.34f;
        float brightness = 0.88f + random.nextFloat() * 0.12f;
        return Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
    }

    private Vec3d randomDirection() {
        Vec3d direction = new Vec3d(
                random.nextDouble() - 0.5,
                (random.nextDouble() - 0.5) * 0.72,
                random.nextDouble() - 0.5
        );
        return direction.lengthSquared() < 1.0E-6 ? new Vec3d(1.0, 0.0, 0.0) : direction.normalize();
    }

    private void appendBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam, Vec3d pos,
                                 float size, int rgb, float alpha, float rotation) {
        if (size <= 0.001f || alpha <= 0.002f) return;
        float half = size * 0.5f;
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

    private void appendAtlasBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam,
                                      Vec3d pos, AtlasSprite sprite, float size, int rgb, float alpha, float rotation) {
        if (size <= 0.001f || alpha <= 0.002f) return;
        float width = sprite.aspect >= 1.0f ? size : size * sprite.aspect;
        float height = sprite.aspect >= 1.0f ? size / sprite.aspect : size;
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        float red = ((rgb >> 16) & 0xFF) / 255f;
        float green = ((rgb >> 8) & 0xFF) / 255f;
        float blue = (rgb & 0xFF) / 255f;
        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -halfWidth, -halfHeight, 0.0f).texture(sprite.u1, sprite.v2).color(red, green, blue, alpha);
        buffer.vertex(matrix, halfWidth, -halfHeight, 0.0f).texture(sprite.u2, sprite.v2).color(red, green, blue, alpha);
        buffer.vertex(matrix, halfWidth, halfHeight, 0.0f).texture(sprite.u2, sprite.v1).color(red, green, blue, alpha);
        buffer.vertex(matrix, -halfWidth, halfHeight, 0.0f).texture(sprite.u1, sprite.v1).color(red, green, blue, alpha);
        matrices.pop();
    }

    private AtlasSprite atlasSpriteFor(String type, int seed) {
        AtlasSprite[] sprites = switch (type) {
            case PARTICLE_GLYPHS -> GLYPH_SPRITES;
            case PARTICLE_STARS -> STAR_SPRITES;
            case PARTICLE_ORB -> new AtlasSprite[]{ATLAS_ORB};
            case PARTICLE_DASH -> DASH_SPRITES;
            case PARTICLE_CROSS -> CROSS_SPRITES;
            case PARTICLE_BAR -> BAR_SPRITES;
            case PARTICLE_ASTERISK -> new AtlasSprite[]{ATLAS_ASTERISK};
            case PARTICLE_STARBURST -> new AtlasSprite[]{ATLAS_STARBURST};
            case PARTICLE_COMET -> new AtlasSprite[]{ATLAS_COMET};
            case PARTICLE_CLUSTER -> new AtlasSprite[]{ATLAS_CLUSTER};
            default -> ALL_SPRITES;
        };
        return sprites[Math.floorMod(seed, sprites.length)];
    }

    private int brighten(int rgb, float factor) {
        int red = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int green = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int blue = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private Vec3d lerp(Vec3d from, Vec3d to, float delta) {
        return new Vec3d(
                MathHelper.lerp(delta, from.x, to.x),
                MathHelper.lerp(delta, from.y, to.y),
                MathHelper.lerp(delta, from.z, to.z)
        );
    }

    private static AtlasSprite sprite(int x1, int y1, int x2, int y2) {
        return new AtlasSprite(
                x1 / 1536.0f, y1 / 1024.0f,
                x2 / 1536.0f, y2 / 1024.0f,
                (x2 - x1) / (float) (y2 - y1)
        );
    }

    private static final class Firefly {
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        final int rgb;
        final int seed;
        final List<Vec3d> trail = new ArrayList<>();
        final List<Vec3d> previousTrail = new ArrayList<>();
        int ageTicks;

        private Firefly(Vec3d pos, Vec3d velocity, int rgb, int seed) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.rgb = rgb;
            this.seed = seed;
            this.trail.add(pos);
            this.previousTrail.add(pos);
        }
    }

    private static final class TrailSpark {
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        final int lifeTicks;
        final float sizeScale;
        final int seed;
        int ageTicks;

        private TrailSpark(Vec3d pos, Vec3d velocity, int lifeTicks, float sizeScale, int seed) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.lifeTicks = lifeTicks;
            this.sizeScale = sizeScale;
            this.seed = seed;
        }
    }

    private record AtlasSprite(float u1, float v1, float u2, float v2, float aspect) {
    }
}
