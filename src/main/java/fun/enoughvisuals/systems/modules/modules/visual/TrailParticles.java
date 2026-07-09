package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SelectSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.block.BlockState;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Register(name = "Trail Particles", id = "trail_particles", description = "Particles on the ground while walking", category = Category.VISUAL)
public class TrailParticles extends Module {

    private static final Identifier BLOOM_TEXTURE = Identifier.of("enough", "textures/bloom.png");
    private static final Identifier STAR1_TEXTURE = Identifier.of("enough", "textures/particles/star1.png");
    private static final Identifier STAR2_TEXTURE = Identifier.of("enough", "textures/particles/star2.png");
    private static final Identifier HEART_TEXTURE = Identifier.of("enough", "textures/particles/heart.png");
    private static final Identifier CROWN_TEXTURE = Identifier.of("enough", "textures/particles/crown.png");
    private static final Identifier LIGHTNING_TEXTURE = Identifier.of("enough", "textures/particles/lightning.png");
    private static final Identifier SNOWFLAKE_TEXTURE = Identifier.of("enough", "textures/particles/snowflake.png");
    private static final Identifier[] TEXTURE_RENDER_ORDER = new Identifier[]{
            STAR1_TEXTURE, STAR2_TEXTURE, HEART_TEXTURE, CROWN_TEXTURE, LIGHTNING_TEXTURE, SNOWFLAKE_TEXTURE, BLOOM_TEXTURE
    };

    private static final String GROUND = "Ground";
    private static final String BODY = "Body";
    private static final String STAR_1 = "Star 1";
    private static final String STAR_2 = "Star 2";
    private static final String HEART = "Heart";
    private static final String CROWN = "Crown";
    private static final String LIGHTNING = "Lightning";
    private static final String SNOWFLAKE = "Snowflake";
    private static final String BLOOM = "Bloom";
    private static final String[] TYPES = new String[]{STAR_1, STAR_2, HEART, CROWN, LIGHTNING, SNOWFLAKE, BLOOM};

    private static final int MAX_PARTICLES = 1200;
    private static TrailParticles INSTANCE;

    public final ModeSetting source = add(new ModeSetting(
            "Source",
            "source",
            "Where particles appear from",
            GROUND,
            GROUND,
            BODY
    ));

    public final SelectSetting particleTypes = add(new SelectSetting(
            "Particles",
            "particles",
            "Particle textures to spawn",
            defaults(STAR_1),
            TYPES
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Color",
            "color",
            "Particle color",
            0xFFE85CFF
    ));

    public final SliderSetting amount = add(new SliderSetting(
            "Amount",
            "amount",
            "Particles per walking pulse",
            3.0,
            1.0,
            18.0,
            1.0
    ));

    public final SliderSetting spawnRate = add(new SliderSetting(
            "Spawn Rate",
            "spawn_rate",
            "How often particles appear while moving",
            22.0,
            2.0,
            90.0,
            1.0,
            "/s"
    ));

    public final SliderSetting size = add(new SliderSetting(
            "Size",
            "size",
            "Particle size in world",
            0.34,
            0.08,
            1.5,
            0.02,
            "m"
    ));

    public final SliderSetting spread = add(new SliderSetting(
            "Spread",
            "spread",
            "Ground offset and horizontal scatter",
            0.48,
            0.0,
            2.4,
            0.02,
            "m"
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Speed",
            "speed",
            "Initial particle launch speed",
            0.85,
            0.05,
            2.6,
            0.05
    ));

    public final SliderSetting gravity = add(new SliderSetting(
            "Gravity",
            "gravity",
            "How strongly particles fall down",
            0.18,
            0.0,
            1.5,
            0.02
    ));

    public final SliderSetting lifetime = add(new SliderSetting(
            "Lifetime",
            "lifetime",
            "How long particles stay visible",
            1.15,
            0.2,
            5.0,
            0.05,
            "s"
    ));

    public final SliderSetting glow = add(new SliderSetting(
            "Glow",
            "glow",
            "Soft glow around particles",
            0.85,
            0.0,
            2.0,
            0.05
    ));

    public final SliderSetting opacity = add(new SliderSetting(
            "Opacity",
            "opacity",
            "Particle opacity",
            0.88,
            0.05,
            1.0,
            0.05
    ));

    public final SliderSetting renderLimit = add(new SliderSetting(
            "Render Limit",
            "render_limit",
            "Maximum particles rendered per frame",
            360.0,
            40.0,
            1000.0,
            5.0
    ));

    public final BooleanSetting throughWalls = add(new BooleanSetting(
            "Through Walls",
            "through_walls",
            "Render particles through blocks",
            false
    ));

    public final BooleanSetting firstPerson = add(new BooleanSetting(
            "First Person",
            "first_person",
            "Render particles in first person",
            true
    ));

    private final Random random = new Random();
    private final List<TrailParticle> activeParticles = new ArrayList<>();
    private Vec3d lastPlayerPos;
    private long lastUpdateNs;
    private double spawnAccumulator;

    public TrailParticles() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onEnable() {
        resetMovement();
    }

    @Override
    protected void onDisable() {
        activeParticles.clear();
        resetMovement();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            activeParticles.clear();
            resetMovement();
            return;
        }

        long now = System.nanoTime();
        update(now);
        spawnFromMovement();
    }

    private void update(long nowNs) {
        if (lastUpdateNs == 0L) {
            lastUpdateNs = nowNs;
            return;
        }

        float seconds = (nowNs - lastUpdateNs) / 1_000_000_000.0f;
        lastUpdateNs = nowNs;
        seconds = MathHelper.clamp(seconds, 0.0f, 0.08f);

        Vec3d playerPos = mc.player.getPos();
        double maxDistanceSq = 96.0 * 96.0;
        float gravityValue = gravity.get().floatValue();

        Iterator<TrailParticle> iterator = activeParticles.iterator();
        while (iterator.hasNext()) {
            TrailParticle particle = iterator.next();
            particle.prevPos = particle.pos;
            particle.prevRotation = particle.rotation;
            particle.age += seconds;

            if (particle.age >= particle.life || playerPos.squaredDistanceTo(particle.pos) > maxDistanceSq) {
                iterator.remove();
                continue;
            }

            Vec3d nextPos = particle.pos.add(particle.velocity.multiply(seconds));
            particle.pos = resolveParticleCollision(particle, nextPos);
            particle.velocity = particle.velocity.multiply(Math.pow(0.62, seconds));
            particle.velocity = particle.velocity.add(0.0, -gravityValue * seconds, 0.0);
            particle.rotation += particle.rotationSpeed * seconds;
        }
    }

    private void spawnFromMovement() {
        PlayerEntity player = mc.player;
        Vec3d current = player.getPos();
        if (lastPlayerPos == null) {
            lastPlayerPos = current;
            return;
        }

        double dx = current.x - lastPlayerPos.x;
        double dy = current.y - lastPlayerPos.y;
        double dz = current.z - lastPlayerPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        lastPlayerPos = current;

        if (player.isSpectator() || player.hasVehicle()) {
            spawnAccumulator = 0.0;
            return;
        }

        boolean walking = player.isOnGround() && horizontalDistance >= 0.012 && player.getVelocity().horizontalLengthSquared() >= 1.0E-5;
        boolean jumping = !player.isOnGround() && (Math.abs(dy) > 0.035 || Math.abs(player.getVelocity().y) > 0.035);
        if (!walking && !jumping) {
            return;
        }

        double movementBoost = MathHelper.clamp(horizontalDistance * 12.0 + Math.abs(dy) * 6.0, 0.35, jumping ? 1.45 : 2.4);
        spawnAccumulator += (spawnRate.get() / 20.0) * movementBoost;

        int safety = 0;
        while (spawnAccumulator >= 1.0 && safety++ < 8) {
            spawnAccumulator -= 1.0;
            spawnBurst(current, dx, dz);
        }
    }

    private void spawnBurst(Vec3d playerPos, double moveX, double moveZ) {
        int count = amount.get().intValue();
        double spreadValue = spread.get();
        double speedValue = speed.get();
        Vec3d move = new Vec3d(moveX, 0.0, moveZ);
        if (move.lengthSquared() > 1.0E-6) {
            move = move.normalize();
        }

        for (int i = 0; i < count; i++) {
            while (activeParticles.size() >= MAX_PARTICLES) {
                activeParticles.remove(0);
            }

            double angle = random.nextDouble() * Math.PI * 2.0;
            double offsetRadius = random.nextDouble() * (0.12 + spreadValue * 0.34);
            double x = Math.cos(angle) * offsetRadius - move.x * (0.08 + random.nextDouble() * 0.18);
            double z = Math.sin(angle) * offsetRadius - move.z * (0.08 + random.nextDouble() * 0.18);
            Vec3d pos = spawnPosition(playerPos, x, z);

            double velocityAngle = angle + (random.nextDouble() - 0.5) * 0.75;
            double horizontalSpeed = (0.05 + random.nextDouble() * 0.18) * Math.max(0.12, spreadValue) * speedValue;
            Vec3d velocity = new Vec3d(
                    Math.cos(velocityAngle) * horizontalSpeed - move.x * 0.035 * speedValue,
                    (0.035 + random.nextDouble() * 0.11) * speedValue,
                    Math.sin(velocityAngle) * horizontalSpeed - move.z * 0.035 * speedValue
            );

            float baseSize = size.get().floatValue() * (0.78f + random.nextFloat() * 0.45f);
            float particleLife = lifetime.get().floatValue() * (0.72f + random.nextFloat() * 0.5f);
            int rgb = varyColor(color.get() & 0x00FFFFFF, 0.14f);
            Identifier texture = textureFor(pickType());

            activeParticles.add(new TrailParticle(
                    pos,
                    velocity,
                    texture,
                    rgb,
                    baseSize,
                    particleLife,
                    random.nextFloat() * 360.0f,
                    (random.nextFloat() - 0.5f) * 115.0f
            ));
        }
    }

    private Vec3d spawnPosition(Vec3d playerPos, double offsetX, double offsetZ) {
        if (source.is(BODY) && mc.player != null) {
            double bodyHeight = Math.max(0.35, mc.player.getHeight());
            double y = playerPos.y + 0.12 + random.nextDouble() * bodyHeight * 0.86;
            double bodySpread = 0.12 + spread.get() * 0.16;
            return new Vec3d(
                    playerPos.x + offsetX * 0.45 + (random.nextDouble() - 0.5) * bodySpread,
                    y,
                    playerPos.z + offsetZ * 0.45 + (random.nextDouble() - 0.5) * bodySpread
            );
        }

        double x = playerPos.x + offsetX;
        double z = playerPos.z + offsetZ;
        double y = surfaceYAt(x, playerPos.y + 1.0, z, 3.0);
        if (Double.isNaN(y)) {
            y = playerPos.y;
        }
        return new Vec3d(x, y + 0.08 + random.nextDouble() * 0.035, z);
    }

    private Vec3d resolveParticleCollision(TrailParticle particle, Vec3d next) {
        float radius = Math.max(0.035f, particle.size * 0.18f);
        if (!isBlocked(next, radius)) {
            return next;
        }

        double surface = surfaceYAt(next.x, particle.pos.y + 0.6, next.z, 1.4);
        if (!Double.isNaN(surface) && next.y <= surface + radius + 0.08) {
            particle.velocity = new Vec3d(particle.velocity.x * 0.46, Math.abs(particle.velocity.y) * 0.20, particle.velocity.z * 0.46);
            return new Vec3d(next.x, surface + radius + 0.018, next.z);
        }

        Vec3d xOnly = new Vec3d(next.x, particle.pos.y, particle.pos.z);
        Vec3d zOnly = new Vec3d(particle.pos.x, particle.pos.y, next.z);
        if (!isBlocked(xOnly, radius)) {
            particle.velocity = new Vec3d(particle.velocity.x * 0.35, particle.velocity.y, particle.velocity.z * -0.18);
            return xOnly;
        }
        if (!isBlocked(zOnly, radius)) {
            particle.velocity = new Vec3d(particle.velocity.x * -0.18, particle.velocity.y, particle.velocity.z * 0.35);
            return zOnly;
        }

        particle.velocity = new Vec3d(-particle.velocity.x * 0.16, Math.max(0.0, particle.velocity.y) * 0.18, -particle.velocity.z * 0.16);
        return particle.pos;
    }

    private boolean isBlocked(Vec3d pos, float radius) {
        if (mc.world == null) return false;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                double sx = pos.x + ox * radius;
                double sz = pos.z + oz * radius;
                BlockPos blockPos = BlockPos.ofFloored(sx, pos.y, sz);
                BlockState state = mc.world.getBlockState(blockPos);
                if (state.isAir()) continue;
                VoxelShape shape = state.getCollisionShape(mc.world, blockPos);
                if (shape.isEmpty()) continue;
                double localY = pos.y - blockPos.getY();
                if (localY >= shape.getMin(Direction.Axis.Y) - radius && localY <= shape.getMax(Direction.Axis.Y) + radius) {
                    return true;
                }
            }
        }
        return false;
    }

    private double surfaceYAt(double x, double fromY, double z, double depth) {
        if (mc.world == null) return Double.NaN;
        int top = MathHelper.floor(fromY);
        int bottom = MathHelper.floor(fromY - depth);
        for (int y = top; y >= bottom; y--) {
            BlockPos blockPos = BlockPos.ofFloored(x, y, z);
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.isAir()) continue;
            VoxelShape shape = state.getCollisionShape(mc.world, blockPos);
            if (shape.isEmpty()) continue;
            return blockPos.getY() + shape.getMax(Direction.Axis.Y);
        }
        return Double.NaN;
    }

    private void render(WorldRenderContext context) {
        if (activeParticles.isEmpty()) return;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !firstPerson.get()) return;

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int maxRendered = renderLimit.get().intValue();
        float opacityValue = opacity.get().floatValue() * color.getAlphaF();
        float glowValue = glow.get().floatValue();
        double renderDistanceSq = 72.0 * 72.0;

        renderGlowBatch(matrices, camera, cam, tickDelta, Math.max(24, maxRendered / 2), renderDistanceSq, opacityValue, glowValue);

        int rendered = 0;
        for (Identifier texture : TEXTURE_RENDER_ORDER) {
            if (rendered >= maxRendered) break;
            rendered += renderBillboardBatch(matrices, camera, cam, tickDelta, texture, maxRendered - rendered, renderDistanceSq, opacityValue);
        }

        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private int renderBillboardBatch(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, Identifier texture,
                                     int maxRendered, double renderDistanceSq, float opacityValue) {
        BufferBuilder buffer = null;
        int rendered = 0;

        for (TrailParticle particle : activeParticles) {
            if (rendered >= maxRendered) break;
            if (!particle.texture.equals(texture)) continue;

            float alpha = particleAlpha(particle) * opacityValue;
            if (alpha <= 0.01f) continue;

            Vec3d pos = lerp(particle.prevPos, particle.pos, tickDelta);
            if (cam.squaredDistanceTo(pos) > renderDistanceSq) continue;

            if (buffer == null) {
                if (texture.equals(BLOOM_TEXTURE)) {
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                } else {
                    RenderSystem.defaultBlendFunc();
                }
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, texture);
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            appendBillboard(buffer, matrices, camera, cam, pos, particle.rgb, particle.size, alpha, lerp(particle.prevRotation, particle.rotation, tickDelta));
            rendered++;
        }

        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
        }
        return rendered;
    }

    private void renderGlowBatch(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, int maxRendered,
                                 double renderDistanceSq, float opacityValue, float glowValue) {
        if (glowValue <= 0.01f || maxRendered <= 0) return;

        BufferBuilder buffer = null;
        int rendered = 0;

        for (TrailParticle particle : activeParticles) {
            if (rendered >= maxRendered) break;

            float alpha = particleAlpha(particle) * opacityValue * 0.34f * glowValue;
            if (alpha <= 0.004f) continue;

            Vec3d pos = lerp(particle.prevPos, particle.pos, tickDelta);
            if (cam.squaredDistanceTo(pos) > renderDistanceSq) continue;

            if (buffer == null) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            appendBillboard(buffer, matrices, camera, cam, pos, particle.rgb, particle.size * 1.85f, alpha, lerp(particle.prevRotation, particle.rotation, tickDelta) * 0.2f);
            rendered++;
        }

        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
        }
    }

    private void appendBillboard(BufferBuilder buffer, MatrixStack matrices, Camera camera, Vec3d cam, Vec3d pos,
                                 int rgb, float size, float alpha, float rotation) {
        float h = size * 0.5f;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        buffer.vertex(matrix, -h, -h, 0f).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, -h, 0f).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, h, 0f).texture(1f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, -h, h, 0f).texture(0f, 0f).color(r, g, b, alpha);

        matrices.pop();
    }

    private float particleAlpha(TrailParticle particle) {
        float progress = particle.age / particle.life;
        float fadeIn = MathHelper.clamp(progress / 0.12f, 0.0f, 1.0f);
        float fadeOut = MathHelper.clamp((1.0f - progress) / 0.34f, 0.0f, 1.0f);
        return fadeIn * fadeOut;
    }

    private int varyColor(int rgb, float amount) {
        float factor = 1.0f + (random.nextFloat() - 0.5f) * amount;
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private String pickType() {
        Set<String> selected = particleTypes.get();
        if (selected == null || selected.isEmpty()) {
            return STAR_1;
        }
        int index = random.nextInt(selected.size());
        int i = 0;
        for (String type : selected) {
            if (i++ == index) {
                return type;
            }
        }
        return STAR_1;
    }

    private Identifier textureFor(String type) {
        return switch (type) {
            case STAR_2 -> STAR2_TEXTURE;
            case HEART -> HEART_TEXTURE;
            case CROWN -> CROWN_TEXTURE;
            case LIGHTNING -> LIGHTNING_TEXTURE;
            case SNOWFLAKE -> SNOWFLAKE_TEXTURE;
            case BLOOM -> BLOOM_TEXTURE;
            default -> STAR1_TEXTURE;
        };
    }

    private static LinkedHashSet<String> defaults(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return set;
    }

    private Vec3d lerp(Vec3d from, Vec3d to, float delta) {
        return new Vec3d(
                MathHelper.lerp(delta, from.x, to.x),
                MathHelper.lerp(delta, from.y, to.y),
                MathHelper.lerp(delta, from.z, to.z)
        );
    }

    private float lerp(float from, float to, float delta) {
        return MathHelper.lerp(delta, from, to);
    }

    private void resetMovement() {
        lastPlayerPos = mc.player == null ? null : mc.player.getPos();
        lastUpdateNs = 0L;
        spawnAccumulator = 0.0;
    }

    private static final class TrailParticle {
        Vec3d pos;
        Vec3d prevPos;
        Vec3d velocity;
        final Identifier texture;
        final int rgb;
        final float size;
        final float life;
        float age;
        float rotation;
        float prevRotation;
        final float rotationSpeed;

        private TrailParticle(Vec3d pos, Vec3d velocity, Identifier texture, int rgb, float size, float life,
                              float rotation, float rotationSpeed) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.texture = texture;
            this.rgb = rgb;
            this.size = size;
            this.life = Math.max(0.05f, life);
            this.rotation = rotation;
            this.prevRotation = rotation;
            this.rotationSpeed = rotationSpeed;
        }
    }
}
