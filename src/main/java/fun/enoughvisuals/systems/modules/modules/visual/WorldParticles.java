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
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Register(name = "World Particles", id = "world_particles", description = "Красивые частицы по миру с большим радиусом", category = Category.VISUAL)
public class WorldParticles extends Module {

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

    private static final String SELECTED = "Выбранный";
    private static final String RANDOM = "Рандомно";
    private static final String FLOATING = "Парящие";
    private static final String SNOW = "Снег";
    private static final String STAR_1 = "Звезда 1";
    private static final String STAR_2 = "Звезда 2";
    private static final String HEART = "Сердце";
    private static final String CROWN = "Корона";
    private static final String LIGHTNING = "Молния";
    private static final String SNOWFLAKE = "Снежинка";
    private static final String BLOOM = "Bloom";
    private static final String CUBES = "Кубы";

    private static final String[] TYPES = new String[]{
            STAR_1, STAR_2, HEART, CROWN, LIGHTNING, SNOWFLAKE, BLOOM, CUBES
    };

    private static WorldParticles INSTANCE;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Как выбирать вид частиц",
            RANDOM,
            RANDOM,
            SELECTED
    ));

    public final ModeSetting type = add(new ModeSetting(
            "Тип",
            "type",
            "Вид частиц",
            CUBES,
            STAR_1,
            STAR_2,
            HEART,
            CROWN,
            LIGHTNING,
            SNOWFLAKE,
            BLOOM,
            CUBES
    ));

    public final ModeSetting physics = add(new ModeSetting(
            "Физика",
            "physics",
            "Поведение частиц в мире",
            FLOATING,
            FLOATING,
            SNOW
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Основной цвет частиц",
            0xFF7BE7FF
    ));

    public final BooleanSetting rainbow = add(new BooleanSetting(
            "Радуга",
            "rainbow",
            "Плавно менять цвет частиц",
            false
    ));

    public final SliderSetting amount = add(new SliderSetting(
            "Количество",
            "amount",
            "Максимальное количество частиц",
            260.0,
            30.0,
            1000.0,
            5.0
    ));

    public final SliderSetting spawnRate = add(new SliderSetting(
            "Частота",
            "spawn_rate",
            "Сколько частиц появляется в секунду",
            24.0,
            2.0,
            90.0,
            1.0
    ));

    public final SliderSetting renderLimit = add(new SliderSetting(
            "Лимит рендера",
            "render_limit",
            "Максимум частиц, которые рисуются за кадр",
            260.0,
            40.0,
            1000.0,
            5.0
    ));

    public final SliderSetting radius = add(new SliderSetting(
            "Радиус",
            "radius",
            "Радиус появления вокруг игрока",
            40.0,
            8.0,
            256.0,
            1.0,
            "m"
    ));

    public final SliderSetting height = add(new SliderSetting(
            "Высота",
            "height",
            "Вертикальный разброс частиц",
            16.0,
            4.0,
            64.0,
            1.0,
            "m"
    ));

    public final SliderSetting size = add(new SliderSetting(
            "Размер",
            "size",
            "Размер частицы в мире",
            0.62,
            0.12,
            2.8,
            0.02,
            "m"
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Скорость",
            "speed",
            "Скорость плавного движения",
            0.55,
            0.05,
            2.5,
            0.05
    ));

    public final SliderSetting wind = add(new SliderSetting(
            "Ветер",
            "wind",
            "Горизонтальный снос частиц",
            0.22,
            0.0,
            2.0,
            0.05
    ));

    public final SliderSetting glow = add(new SliderSetting(
            "Свечение",
            "glow",
            "Мягкое свечение вокруг частиц",
            0.85,
            0.0,
            2.0,
            0.05
    ));

    public final SliderSetting opacity = add(new SliderSetting(
            "Прозрачность",
            "opacity",
            "Общая прозрачность частиц",
            0.82,
            0.05,
            1.0,
            0.05
    ));

    private final Random random = new Random();
    private final List<WorldParticle> particles = new ArrayList<>();
    private long lastUpdateNs;
    private double spawnAccumulator;

    public WorldParticles() {
        INSTANCE = this;
        type.visibleWhen(() -> mode.is(SELECTED));
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        particles.clear();
        lastUpdateNs = 0L;
        spawnAccumulator = 0.0;
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            particles.clear();
            lastUpdateNs = 0L;
            spawnAccumulator = 0.0;
            return;
        }

        update(System.nanoTime());
        spawn();
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
        double maxDistance = radius.get() + 28.0;
        double maxDistanceSq = maxDistance * maxDistance;

        Iterator<WorldParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            WorldParticle particle = iterator.next();
            particle.prevPos = particle.pos;
            particle.prevRotationX = particle.rotationX;
            particle.prevRotationY = particle.rotationY;
            particle.prevRotationZ = particle.rotationZ;
            particle.age += seconds;

            if (particle.age >= particle.life || playerPos.squaredDistanceTo(particle.pos) > maxDistanceSq) {
                iterator.remove();
                continue;
            }

            double phase = particle.seed + particle.age * (0.75 + speed.get() * 0.45);
            Vec3d velocity = particle.velocity;
            if (particle.snow) {
                double side = Math.sin(phase) * 0.018 * wind.get();
                particle.pos = particle.pos.add(velocity.x * seconds + side, velocity.y * seconds, velocity.z * seconds + Math.cos(phase * 0.7) * 0.012 * wind.get());
                if (touchesGround(particle.pos, particle.size)) {
                    iterator.remove();
                    continue;
                }
            } else {
                double floatSpeed = speed.get();
                double dx = Math.sin(phase * 0.72) * 0.028 * floatSpeed + velocity.x * seconds;
                double dy = Math.cos(phase * 0.55) * 0.012 * floatSpeed + velocity.y * seconds;
                double dz = Math.cos(phase * 0.81) * 0.028 * floatSpeed + velocity.z * seconds;
                particle.pos = particle.pos.add(dx, dy, dz);
            }

            particle.rotationX += particle.rotationSpeedX * seconds;
            particle.rotationY += particle.rotationSpeedY * seconds;
            particle.rotationZ += particle.rotationSpeedZ * seconds;
        }
    }

    private void spawn() {
        if (mc.player == null || mc.world == null) return;

        spawnAccumulator += spawnRate.get() / 20.0;
        int maxParticles = amount.get().intValue();
        int safety = 0;

        while (spawnAccumulator >= 1.0 && particles.size() < maxParticles && safety++ < 24) {
            spawnAccumulator -= 1.0;
            particles.add(createParticle());
        }

        int overflow = particles.size() - maxParticles;
        if (overflow > 0) {
            particles.subList(0, overflow).clear();
        }
    }

    private WorldParticle createParticle() {
        String pickedType = pickType();
        boolean cube = pickedType.equals(CUBES);
        boolean snow = physics.is(SNOW);

        Vec3d base = mc.player.getPos();
        double range = radius.get();
        double yRange = height.get();
        double minDistance = cube ? Math.min(8.0, range * 0.18) : 1.5;

        double x;
        double z;
        double distanceSq;
        do {
            x = (random.nextDouble() - 0.5) * 2.0 * range;
            z = (random.nextDouble() - 0.5) * 2.0 * range;
            distanceSq = x * x + z * z;
        } while (distanceSq < minDistance * minDistance || distanceSq > range * range);

        double y = snow
                ? yRange * 0.35 + random.nextDouble() * yRange
                : (random.nextDouble() - 0.35) * yRange;
        Vec3d pos = base.add(x, y + 1.0, z);

        for (int i = 0; i < 8; i++) {
            BlockPos blockPos = BlockPos.ofFloored(pos);
            if (mc.world.getBlockState(blockPos).isAir()) break;
            pos = pos.add(0.0, 1.0, 0.0);
        }

        double speedValue = speed.get();
        Vec3d velocity = snow
                ? new Vec3d((random.nextDouble() - 0.5) * 0.025 * wind.get(), -1.35 * speedValue - random.nextDouble() * 0.45 * speedValue, (random.nextDouble() - 0.5) * 0.025 * wind.get())
                : new Vec3d((random.nextDouble() - 0.5) * 0.035 * speedValue, (random.nextDouble() - 0.5) * 0.018 * speedValue, (random.nextDouble() - 0.5) * 0.035 * speedValue);

        float baseSize = size.get().floatValue() * (cube ? 1.0f : 0.72f) * (0.75f + random.nextFloat() * 0.55f);
        float life = snow ? 24.0f + random.nextFloat() * 10.0f : 9.0f + random.nextFloat() * 7.0f;
        Identifier texture = textureFor(pickedType);
        int rgb = rainbow.get() ? rainbowColor(random.nextFloat() * 360f) : varyColor(color.get() & 0x00FFFFFF, 0.12f);

        return new WorldParticle(
                pos,
                velocity,
                texture,
                cube,
                snow,
                rgb,
                baseSize,
                life,
                random.nextFloat() * 1000.0f,
                random.nextFloat() * 360.0f,
                random.nextFloat() * 360.0f,
                random.nextFloat() * 360.0f,
                (random.nextFloat() - 0.5f) * (cube ? 32.0f : 22.0f),
                (random.nextFloat() - 0.5f) * (cube ? 44.0f : 18.0f),
                (random.nextFloat() - 0.5f) * (cube ? 28.0f : 65.0f)
        );
    }

    private void render(WorldRenderContext context) {
        long now = System.nanoTime();
        if (lastUpdateNs == 0L) {
            update(now);
        }

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int maxRendered = renderLimit.get().intValue();
        float opacityValue = opacity.get().floatValue();
        float glowValue = glow.get().floatValue();
        double renderDistance = Math.min(radius.get() + 12.0, 160.0);
        double renderDistanceSq = renderDistance * renderDistance;
        int rendered = renderCubes(matrices, cam, tickDelta, maxRendered, renderDistanceSq, opacityValue, glowValue);

        for (Identifier texture : TEXTURE_RENDER_ORDER) {
            if (rendered >= maxRendered) break;
            rendered += renderBillboardBatch(matrices, camera, cam, tickDelta, texture, maxRendered - rendered, renderDistanceSq, opacityValue);
        }
        renderGlowBatch(matrices, camera, cam, tickDelta, Math.max(24, maxRendered / 3), renderDistanceSq, opacityValue, glowValue);

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private int renderCubes(MatrixStack matrices, Vec3d cam, float tickDelta, int maxRendered, double renderDistanceSq, float opacityValue, float glowValue) {
        int rendered = 0;
        for (WorldParticle particle : particles) {
            if (rendered >= maxRendered) break;
            if (!particle.cube) continue;

            float alpha = particleAlpha(particle) * opacityValue;
            if (alpha <= 0.01f) continue;

            Vec3d pos = lerp(particle.prevPos, particle.pos, tickDelta);
            double distanceSq = cam.squaredDistanceTo(pos);
            if (distanceSq > renderDistanceSq) continue;

            boolean allowGlow = glowValue > 0.01f && rendered < maxRendered * 0.35 && distanceSq < 34.0 * 34.0;
            renderCube(matrices, cam, pos, particle, alpha, tickDelta, allowGlow);
            rendered++;
        }
        return rendered;
    }

    private int renderBillboardBatch(MatrixStack matrices, Camera camera, Vec3d cam, float tickDelta, Identifier texture,
                                     int maxRendered, double renderDistanceSq, float opacityValue) {
        BufferBuilder buffer = null;
        int rendered = 0;

        for (WorldParticle particle : particles) {
            if (rendered >= maxRendered) break;
            if (particle.cube || !particle.texture.equals(texture)) continue;

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

            appendBillboard(buffer, matrices, camera, cam, pos, particle.rgb, particle.size, alpha, lerp(particle.prevRotationZ, particle.rotationZ, tickDelta));
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
        double glowDistanceSq = Math.min(renderDistanceSq, 38.0 * 38.0);

        for (WorldParticle particle : particles) {
            if (rendered >= maxRendered) break;
            if (particle.cube || particle.texture.equals(BLOOM_TEXTURE)) continue;

            float alpha = particleAlpha(particle) * opacityValue * 0.25f * glowValue;
            if (alpha <= 0.004f) continue;

            Vec3d pos = lerp(particle.prevPos, particle.pos, tickDelta);
            if (cam.squaredDistanceTo(pos) > glowDistanceSq) continue;

            if (buffer == null) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
                buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            appendBillboard(buffer, matrices, camera, cam, pos, particle.rgb, particle.size * 1.85f, alpha, lerp(particle.prevRotationZ, particle.rotationZ, tickDelta) * 0.2f);
            rendered++;
        }

        if (buffer != null) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
        }
    }

    private void renderCube(MatrixStack matrices, Vec3d camera, Vec3d pos, WorldParticle particle, float alpha, float tickDelta, boolean allowGlow) {
        matrices.push();
        matrices.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lerp(particle.prevRotationX, particle.rotationX, tickDelta)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(lerp(particle.prevRotationY, particle.rotationY, tickDelta)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(lerp(particle.prevRotationZ, particle.rotationZ, tickDelta)));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float h = particle.size * 0.5f;
        float[] fill = rgba(particle.rgb, alpha * 0.18f);
        float[] bright = rgba(brighten(particle.rgb, 1.35f), alpha * 0.72f);
        float[] soft = rgba(brighten(particle.rgb, 0.85f), alpha * 0.42f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder quads = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        cubeFaces(quads, matrix, -h, -h, -h, h, h, h, fill);
        BufferRenderer.drawWithGlobalProgram(quads.end());

        RenderSystem.lineWidth(1.35f);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        cubeEdges(lines, matrix, -h, -h, -h, h, h, h, bright);
        crossLines(lines, matrix, -h, -h, -h, h, h, h, soft);
        BufferRenderer.drawWithGlobalProgram(lines.end());

        matrices.pop();

        if (allowGlow) {
            renderBillboard(matrices, mc.gameRenderer.getCamera(), camera, pos, BLOOM_TEXTURE, particle.rgb, particle.size * 2.0f, alpha * 0.32f * glow.get().floatValue(), 0.0f, false);
        }
    }

    private void renderBillboard(MatrixStack matrices, Camera camera, Vec3d cam, Vec3d pos, Identifier texture, int rgb, float size, float alpha, float rotation, boolean allowGlow) {
        if (alpha <= 0.004f) return;

        if (allowGlow && !texture.equals(BLOOM_TEXTURE)) {
            renderBillboard(matrices, camera, cam, pos, BLOOM_TEXTURE, rgb, size * 1.85f, alpha * 0.25f * glow.get().floatValue(), rotation * 0.2f, false);
        }

        boolean glowLayer = texture.equals(BLOOM_TEXTURE);
        if (glowLayer) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }

        matrices.push();
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        matrices.multiply(camera.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float h = size * 0.5f;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -h, -h, 0f).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, -h, 0f).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, h, h, 0f).texture(1f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, -h, h, 0f).texture(0f, 0f).color(r, g, b, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();

        if (glowLayer) {
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

    private void cubeFaces(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        quad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, color);
        quad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        quad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
    }

    private void cubeEdges(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        line(buffer, matrix, x1, y1, z1, x2, y1, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y1, z2, color);
        line(buffer, matrix, x2, y1, z2, x1, y1, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y1, z1, color);
        line(buffer, matrix, x1, y2, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y2, z1, x2, y2, z2, color);
        line(buffer, matrix, x2, y2, z2, x1, y2, z2, color);
        line(buffer, matrix, x1, y2, z2, x1, y2, z1, color);
        line(buffer, matrix, x1, y1, z1, x1, y2, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y1, z2, x2, y2, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y2, z2, color);
    }

    private void crossLines(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float[] color) {
        line(buffer, matrix, x1, y1, z1, x2, y2, z2, color);
        line(buffer, matrix, x2, y1, z1, x1, y2, z2, color);
        line(buffer, matrix, x1, y1, z2, x2, y2, z1, color);
        line(buffer, matrix, x2, y1, z2, x1, y2, z1, color);
    }

    private void quad(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4,
                      float[] color) {
        buffer.vertex(matrix, x1, y1, z1).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x2, y2, z2).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x3, y3, z3).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x4, y4, z4).color(color[0], color[1], color[2], color[3]);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float[] color) {
        buffer.vertex(matrix, x1, y1, z1).color(color[0], color[1], color[2], color[3]);
        buffer.vertex(matrix, x2, y2, z2).color(color[0], color[1], color[2], color[3]);
    }

    private float particleAlpha(WorldParticle particle) {
        float progress = particle.age / particle.life;
        float fadeIn = MathHelper.clamp(progress / 0.18f, 0.0f, 1.0f);
        float fadeOut = MathHelper.clamp((1.0f - progress) / 0.26f, 0.0f, 1.0f);
        return fadeIn * fadeOut;
    }

    private String pickType() {
        if (mode.is(RANDOM)) {
            return TYPES[random.nextInt(TYPES.length)];
        }
        return type.get();
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

    private int varyColor(int rgb, float amount) {
        float factor = 1.0f + (random.nextFloat() - 0.5f) * amount;
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private boolean touchesGround(Vec3d pos, float size) {
        if (mc.world == null) return false;

        BlockPos feet = BlockPos.ofFloored(pos.x, pos.y - Math.max(0.04f, size * 0.42f), pos.z);
        return !mc.world.getBlockState(feet).isAir();
    }

    private int rainbowColor(float offset) {
        float hue = ((System.currentTimeMillis() % 6000L) / 6000f + offset / 360f) % 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.58f, 1.0f) & 0x00FFFFFF;
    }

    private int brighten(int rgb, float factor) {
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private float[] rgba(int rgb, float alpha) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f,
                alpha
        };
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

    private static final class WorldParticle {
        Vec3d pos;
        Vec3d prevPos;
        final Vec3d velocity;
        final Identifier texture;
        final boolean cube;
        final boolean snow;
        final int rgb;
        final float size;
        final float life;
        final float seed;
        float age;
        float rotationX;
        float rotationY;
        float rotationZ;
        float prevRotationX;
        float prevRotationY;
        float prevRotationZ;
        final float rotationSpeedX;
        final float rotationSpeedY;
        final float rotationSpeedZ;

        private WorldParticle(Vec3d pos, Vec3d velocity, Identifier texture, boolean cube, boolean snow, int rgb,
                              float size, float life, float seed,
                              float rotationX, float rotationY, float rotationZ,
                              float rotationSpeedX, float rotationSpeedY, float rotationSpeedZ) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.texture = texture;
            this.cube = cube;
            this.snow = snow;
            this.rgb = rgb;
            this.size = size;
            this.life = life;
            this.seed = seed;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.prevRotationX = rotationX;
            this.prevRotationY = rotationY;
            this.prevRotationZ = rotationZ;
            this.rotationSpeedX = rotationSpeedX;
            this.rotationSpeedY = rotationSpeedY;
            this.rotationSpeedZ = rotationSpeedZ;
        }
    }
}
