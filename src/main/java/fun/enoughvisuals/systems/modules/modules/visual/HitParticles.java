package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Register(name = "Hit Particles", id = "hit_particles", description = "Партиклы при ударе", category = Category.VISUAL)
public class HitParticles extends Module {

    private static HitParticles INSTANCE;
    private static boolean registered = false;

    private static final Identifier BLOOM_TEXTURE = Identifier.of("enough", "textures/bloom.png");
    private static final Identifier STAR1_TEXTURE = Identifier.of("enough", "textures/particles/star1.png");
    private static final Identifier STAR2_TEXTURE = Identifier.of("enough", "textures/particles/star2.png");
    private static final Identifier HEART_TEXTURE = Identifier.of("enough", "textures/particles/heart.png");
    private static final Identifier CROWN_TEXTURE = Identifier.of("enough", "textures/particles/crown.png");
    private static final Identifier LIGHTNING_TEXTURE = Identifier.of("enough", "textures/particles/lightning.png");
    private static final Identifier SNOWFLAKE_TEXTURE = Identifier.of("enough", "textures/particles/snowflake.png");
    private static final Identifier[] ALL_PARTICLE_TEXTURES = new Identifier[]{
            STAR1_TEXTURE, STAR2_TEXTURE, HEART_TEXTURE, CROWN_TEXTURE, LIGHTNING_TEXTURE, SNOWFLAKE_TEXTURE, BLOOM_TEXTURE
    };

    private static final int MAX_PARTICLES = 600;

    public final ModeSetting mode = add(new ModeSetting(
            "Режим",
            "mode",
            "Как выбирать тип частицы",
            "Выбранный",
            "Выбранный",
            "Рандомно"
    ));

    public final ModeSetting type = add(new ModeSetting(
            "Тип",
            "type",
            "Тип частицы",
            "Звезда 1",
            "Звезда 1",
            "Звезда 2",
            "Сердце",
            "Корона",
            "Молния",
            "Снежинка",
            "Bloom"
    ));

    public final ColorSetting color = add(new ColorSetting(
            "Цвет",
            "color",
            "Цвет партиклов",
            0xFFFF3B30
    ));

    public final BooleanSetting rainbow = add(new BooleanSetting(
            "Радуга",
            "rainbow",
            "Плавно менять цвет частиц",
            false
    ));
    public final SliderSetting count = add(new SliderSetting(
            "Количество",
            "count",
            "Сколько партиклов спавнится за удар",
            26, 1, 80, 1
    ));

    public final SliderSetting size = add(new SliderSetting(
            "Размер",
            "size",
            "Базовый размер партиклов",
            14.0, 4.0, 40.0, 1.0
    ));

    public final SliderSetting life = add(new SliderSetting(
            "Время жизни",
            "life",
            "Сколько тиков живут партиклы",
            42.0, 10.0, 120.0, 1.0
    ));

    public final SliderSetting speed = add(new SliderSetting(
            "Скорость",
            "speed",
            "Начальная скорость разлёта партиклов",
            1.0, 0.1, 3.0, 0.05
    ));

    public final BooleanSetting bloom = add(new BooleanSetting(
            "Свечение",
            "bloom",
            "Рисовать bloom-ореол вокруг партиклов",
            true
    ));

    public final BooleanSetting randomRotation = add(new BooleanSetting(
            "Случайный поворот",
            "random_rotation",
            "Каждый партикл повёрнут случайно",
            true
    ));

    public final BooleanSetting gravity = add(new BooleanSetting(
            "Гравитация",
            "gravity",
            "Партиклы падают вниз со временем",
            true
    ));

    private final Random random = new Random();
    private final List<HitParticle> particles = new ArrayList<>();
    private long lastUpdateMs = 0L;

    public HitParticles() {
        INSTANCE = this;
        type.visibleWhen(() -> mode.is("Выбранный"));
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            HudRenderCallback.EVENT.register(this::onRender2D);
            registered = true;
        }
    }

    @Override
    protected void onDisable() {
        particles.clear();
        lastUpdateMs = 0L;
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            particles.clear();
            lastUpdateMs = 0L;
        }
    }

    public static void onHit(Entity target) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || target == null) return;
        if (!INSTANCE.isVisibleTarget(target)) return;
        INSTANCE.spawnBurst(target);
    }

    private void spawnBurst(Entity target) {
        Vec3d center = target.getBoundingBox().getCenter().add(0.0, -target.getHeight() * 0.08, 0.0);
        int count = (int) this.count.get().doubleValue();
        float speedMul = speed.get().floatValue();
        float baseLife = life.get().floatValue();
        float baseSize = size.get().floatValue();

        for (int i = 0; i < count; i++) {
            while (particles.size() >= MAX_PARTICLES) {
                particles.remove(0);
            }

            double angle = random.nextDouble() * Math.PI * 2.0;
            double verticalAngle = Math.toRadians(16.0 + random.nextDouble() * 44.0);
            double spd = (0.036 + random.nextDouble() * 0.066) * speedMul;
            double horizontalSpeed = Math.cos(verticalAngle) * spd;
            double rise = Math.sin(verticalAngle) * spd + 0.009;

            Vec3d velocity = new Vec3d(Math.cos(angle) * horizontalSpeed, rise, Math.sin(angle) * horizontalSpeed);
            Vec3d spawnPos = center;

            float pLife = baseLife + random.nextFloat() * 20f;
            float pSize = baseSize + random.nextFloat() * 10.0f;
            float rotation = randomRotation.get() ? random.nextFloat() * 360f : 0f;
            float rotationSpeed = (random.nextFloat() - 0.5f) * 24f;
            Identifier texture = pickTexture();
            int particleColor = rainbow.get() ? rainbowColor(i * 27f + random.nextFloat() * 45f) : color.get() & 0x00FFFFFF;

            particles.add(new HitParticle(spawnPos, velocity, pLife, pLife, pSize, rotation, rotationSpeed, texture, particleColor));
        }
    }

    private Identifier pickTexture() {
        if (mode.is("Рандомно")) {
            return ALL_PARTICLE_TEXTURES[random.nextInt(ALL_PARTICLE_TEXTURES.length)];
        }

        return switch (type.get()) {
            case "Звезда 2" -> STAR2_TEXTURE;
            case "Сердце" -> HEART_TEXTURE;
            case "Корона" -> CROWN_TEXTURE;
            case "Молния" -> LIGHTNING_TEXTURE;
            case "Снежинка" -> SNOWFLAKE_TEXTURE;
            case "Bloom" -> BLOOM_TEXTURE;
            default -> STAR1_TEXTURE;
        };
    }

    private void onRender2D(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (particles.isEmpty()) return;

        long now = System.currentTimeMillis();
        updateParticles(now);

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d eye = camera.getPos();
        float tickDelta = tickCounter.getTickDelta(true);
        for (HitParticle particle : particles) {
            if (!isPointVisible(eye, particle.pos)) continue;

            Vec3d screen = projectToScreen(particle.pos.x, particle.pos.y, particle.pos.z, camera, tickDelta);
            if (screen == null) continue;

            float lifeRatio = particle.life / particle.maxLife;
            float fade = lifeRatio * lifeRatio;
            if (fade <= 0.01f) continue;

            float progress = 1f - lifeRatio;
            float distance = (float) camera.getPos().distanceTo(particle.pos);
            float distanceScale = MathHelper.clamp(1.38f - distance * 0.065f, 0.66f, 1.28f);
            float size = particle.baseSize * distanceScale * (0.96f + progress * 0.55f);

            float px = (float) screen.x;
            float py = (float) screen.y;
            float alpha = MathHelper.clamp(fade * 1.28f, 0.0f, 1.0f);

            drawParticle(ctx, particle, px, py, size, alpha);
        }
    }

    private void drawParticle(DrawContext ctx, HitParticle particle, float x, float y, float size, float alpha) {
        if (bloom.get()) {
            float bloomSize = size * 1.9f;
            drawTintedImage(ctx, BLOOM_TEXTURE, x, y, bloomSize, particle.rgbColor, alpha * 0.52f, particle.rotation * 0.4f);
        }
        drawTintedImage(ctx, particle.texture, x, y, size, particle.rgbColor, alpha, particle.rotation);
    }

    private void drawTintedImage(DrawContext ctx, Identifier texture, float centerX, float centerY, float size, int rgbColor, float alpha, float rotationDeg) {
        if (alpha <= 0.004f) return;

        float x = centerX - size * 0.5f;
        float y = centerY - size * 0.5f;
        float r = ((rgbColor >> 16) & 0xFF) / 255f;
        float g = ((rgbColor >> 8) & 0xFF) / 255f;
        float b = (rgbColor & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0f);
        ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(rotationDeg)));
        ctx.getMatrices().translate(-centerX, -centerY, 0f);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        var matrix = ctx.getMatrices().peek().getPositionMatrix();

        buffer.vertex(matrix, x, y, 0f).texture(0f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, x, y + size, 0f).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, x + size, y + size, 0f).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, x + size, y, 0f).texture(1f, 0f).color(r, g, b, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        ctx.getMatrices().pop();
        RenderSystem.disableBlend();
    }

    private void updateParticles(long now) {
        if (lastUpdateMs == 0L) {
            lastUpdateMs = now;
            return;
        }

        float dt = (now - lastUpdateMs) / 16.6667f;
        lastUpdateMs = now;
        if (dt <= 0f) return;
        dt = Math.min(dt, 3.0f);

        Iterator<HitParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            HitParticle p = iterator.next();
            p.life -= dt;
            if (p.life <= 0f) {
                iterator.remove();
                continue;
            }

            p.pos = p.pos.add(p.velocity.multiply(dt));
            p.velocity = p.velocity.multiply(Math.pow(0.945, dt));
            if (gravity.get()) {
                p.velocity = p.velocity.add(0.0, -0.0013 * dt, 0.0);
            }
            p.rotation += p.rotationSpeed * dt;
        }
    }

    private boolean isVisibleTarget(Entity target) {
        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Box box = target.getBoundingBox();
        Vec3d center = box.getCenter();

        if (isPointVisible(eye, center)) return true;
        if (isPointVisible(eye, new Vec3d(center.x, box.maxY - 0.05, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(center.x, box.minY + 0.05, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(box.minX + 0.07, center.y, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(box.maxX - 0.07, center.y, center.z))) return true;
        if (isPointVisible(eye, new Vec3d(center.x, center.y, box.minZ + 0.07))) return true;
        return isPointVisible(eye, new Vec3d(center.x, center.y, box.maxZ - 0.07));
    }

    private int rainbowColor(float offset) {
        float hue = ((System.currentTimeMillis() % 6000L) / 6000f + offset / 360f) % 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.58f, 1.0f) & 0x00FFFFFF;
    }

    private boolean isPointVisible(Vec3d from, Vec3d to) {
        RaycastContext context = new RaycastContext(
                from,
                to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );
        BlockHitResult hit = mc.world.raycast(context);
        if (hit.getType() == HitResult.Type.MISS) return true;

        double hitDistSq = from.squaredDistanceTo(hit.getPos());
        double pointDistSq = from.squaredDistanceTo(to);
        return hitDistSq + 1.0E-4 >= pointDistSq;
    }

    private Vec3d projectToScreen(double x, double y, double z, Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();
        Vector4f pos = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1.0f);

        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        pos.rotate(rotation);

        if (pos.z >= 0) return null;

        GameRendererAccessor gameRendererAccessor = (GameRendererAccessor) mc.gameRenderer;
        double fov = gameRendererAccessor.invokeGetFov(camera, tickDelta, true);
        float halfFovTan = (float) Math.tan(Math.toRadians(fov) / 2.0);

        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        float scale = (screenHeight / 2.0f) / halfFovTan;

        float screenX = screenWidth / 2.0f + (pos.x / -pos.z) * scale;
        float screenY = screenHeight / 2.0f - (pos.y / -pos.z) * scale;

        if (screenX < -256 || screenX > screenWidth + 256 || screenY < -256 || screenY > screenHeight + 256) {
            return null;
        }
        return new Vec3d(screenX, screenY, 0.0);
    }

    private static final class HitParticle {
        Vec3d pos;
        Vec3d velocity;
        float life;
        final float maxLife;
        final float baseSize;
        float rotation;
        final float rotationSpeed;
        final Identifier texture;
        final int rgbColor;

        private HitParticle(Vec3d pos, Vec3d velocity, float life, float maxLife, float baseSize, float rotation, float rotationSpeed, Identifier texture, int rgbColor) {
            this.pos = pos;
            this.velocity = velocity;
            this.life = life;
            this.maxLife = maxLife;
            this.baseSize = baseSize;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.texture = texture;
            this.rgbColor = rgbColor;
        }
    }
}
