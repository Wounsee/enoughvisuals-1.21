package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Register(name = "Mouse Particles", id = "mouse_particles", description = "Particles behind the cursor", category = Category.VISUAL)
public class MouseParticles extends Module {

    private static final Identifier BLOOM_TEXTURE = Identifier.of("enough", "textures/bloom.png");
    private static final Identifier STAR1_TEXTURE = Identifier.of("enough", "textures/particles/star1.png");
    private static final Identifier STAR2_TEXTURE = Identifier.of("enough", "textures/particles/star2.png");
    private static final Identifier HEART_TEXTURE = Identifier.of("enough", "textures/particles/heart.png");
    private static final Identifier CROWN_TEXTURE = Identifier.of("enough", "textures/particles/crown.png");
    private static final Identifier LIGHTNING_TEXTURE = Identifier.of("enough", "textures/particles/lightning.png");
    private static final Identifier SNOWFLAKE_TEXTURE = Identifier.of("enough", "textures/particles/snowflake.png");

    private static final Identifier[] ALL_TEXTURES = {
            STAR1_TEXTURE,
            STAR2_TEXTURE,
            HEART_TEXTURE,
            CROWN_TEXTURE,
            LIGHTNING_TEXTURE,
            SNOWFLAKE_TEXTURE
    };

    private static final int MAX_PARTICLES = 520;
    private static final float MIN_MOUSE_MOVE = 0.35f;

    private static MouseParticles INSTANCE;

    public final SliderSetting amount = add(new SliderSetting("Amount", "amount", "Particles per second while moving the cursor", 12.0, 1.0, 50.0, 1.0, "/s"));
    public final SliderSetting spread = add(new SliderSetting("Spread", "spread", "How strongly particles fly away from the cursor", 1.0, 0.1, 4.0, 0.05));
    public final SliderSetting size = add(new SliderSetting("Size", "size", "Particle size", 8.0, 2.5, 32.0, 0.5, "px"));
    public final SliderSetting lifetime = add(new SliderSetting("Lifetime", "lifetime", "Particle lifetime", 0.8, 0.1, 3.0, 0.1, "s"));
    public final ModeSetting selectionMode = add(new ModeSetting("Mode", "selection_mode", "Particle selection mode", "Selected", "Selected", "Random"));
    public final ModeSetting type = add(new ModeSetting("Texture", "texture", "Particle texture", "Star 1", "Star 1", "Star 2", "Heart", "Crown", "Lightning", "Snowflake"));

    private final Random random = new Random();
    private final List<MouseParticle> particles = new ArrayList<>();

    private boolean hasScreenCursor;
    private float lastScreenX;
    private float lastScreenY;
    private long lastUpdateMs;
    private long lastSpawnMs;
    private float amountRemainder;

    public MouseParticles() {
        INSTANCE = this;
        type.visibleWhen(() -> selectionMode.is("Selected"));
    }

    public static void renderScreen(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderScreen(ctx, (float) mouseX, (float) mouseY, delta);
    }

    public static void renderScreen(DrawContext ctx, float mouseX, float mouseY, float delta) {
        MouseParticles module = active();
        if (module == null) return;
        module.renderAtCursor(ctx, mouseX, mouseY, true);
    }

    public static void renderHud(DrawContext ctx, float delta) {
        MouseParticles module = active();
        if (module == null) return;
        if (mc.player == null || mc.world == null) {
            module.hasScreenCursor = false;
            return;
        }
        if (mc.currentScreen != null) {
            module.renderAtWindowCursor(ctx);
            return;
        }
        module.hasScreenCursor = false;
        module.updateAndDraw(ctx);
    }

    private static MouseParticles active() {
        return INSTANCE != null && INSTANCE.isEnabled() ? INSTANCE : null;
    }

    @Override
    protected void onDisable() {
        particles.clear();
        lastUpdateMs = 0L;
        lastSpawnMs = 0L;
        hasScreenCursor = false;
        amountRemainder = 0.0f;
    }

    private void renderAtWindowCursor(DrawContext ctx) {
        Window window = mc.getWindow();
        double[] cursorX = new double[1];
        double[] cursorY = new double[1];
        GLFW.glfwGetCursorPos(window.getHandle(), cursorX, cursorY);

        float scaleX = window.getScaledWidth() / (float) Math.max(1, window.getFramebufferWidth());
        float scaleY = window.getScaledHeight() / (float) Math.max(1, window.getFramebufferHeight());
        renderAtCursor(ctx, (float) cursorX[0] * scaleX, (float) cursorY[0] * scaleY, true);
    }

    private void renderAtCursor(DrawContext ctx, float x, float y, boolean screenCursor) {
        if (!isInsideScreen(x, y)) {
            if (screenCursor) hasScreenCursor = false;
            updateAndDraw(ctx);
            return;
        }

        if (!screenCursor) {
            hasScreenCursor = false;
        }

        if (!hasScreenCursor) {
            hasScreenCursor = true;
            lastScreenX = x;
            lastScreenY = y;
            updateAndDraw(ctx);
            return;
        }

        float dx = x - lastScreenX;
        float dy = y - lastScreenY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance >= MIN_MOUSE_MOVE) {
            spawnBurst(x, y, dx, dy);
            lastScreenX = x;
            lastScreenY = y;
        }

        updateAndDraw(ctx);
    }

    private boolean isInsideScreen(float x, float y) {
        Window window = mc.getWindow();
        return x >= -24.0f
                && y >= -24.0f
                && x <= window.getScaledWidth() + 24.0f
                && y <= window.getScaledHeight() + 24.0f;
    }

    private void spawnBurst(float x, float y, float moveX, float moveY) {
        long now = System.currentTimeMillis();
        if (lastSpawnMs == 0L) {
            lastSpawnMs = now;
            return;
        }

        float elapsedSeconds = MathHelper.clamp((now - lastSpawnMs) / 1000.0f, 0.0f, 0.25f);
        lastSpawnMs = now;
        amountRemainder += amount.get().floatValue() * elapsedSeconds;
        int count = (int) amountRemainder;
        if (count <= 0) return;
        amountRemainder -= count;

        float force = spread.get().floatValue();
        float baseSize = size.get().floatValue();
        float life = lifetime.get().floatValue() * 60.0f;

        float moveLen = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        float trailX = moveLen > 0.001f ? -moveX / moveLen : 0.0f;
        float trailY = moveLen > 0.001f ? -moveY / moveLen : 0.0f;

        for (int i = 0; i < count; i++) {
            while (particles.size() >= MAX_PARTICLES) {
                particles.removeFirst();
            }

            float angle = random.nextFloat() * (float) Math.PI * 2.0f;
            float speed = (0.34f + random.nextFloat() * 1.05f) * force;
            float vx = (float) Math.cos(angle) * speed + trailX * speed * 0.45f;
            float vy = (float) Math.sin(angle) * speed + trailY * speed * 0.45f;
            float spawnOffset = random.nextFloat() * Math.max(1.0f, baseSize * 0.35f);
            float px = x + ((random.nextFloat() - 0.5f) * spawnOffset);
            float py = y + ((random.nextFloat() - 0.5f) * spawnOffset);
            float particleSize = baseSize * (0.74f + random.nextFloat() * 0.58f);
            float rotation = random.nextFloat() * 360.0f;
            float rotationSpeed = (random.nextFloat() - 0.5f) * (10.0f + force * 10.0f);

            particles.add(new MouseParticle(px, py, vx, vy, particleSize, life, life, rotation, rotationSpeed, pickTexture(), 0xFFFFFFFF));
        }
    }

    private Identifier pickTexture() {
        if (selectionMode.is("Random")) {
            return ALL_TEXTURES[random.nextInt(ALL_TEXTURES.length)];
        }

        return switch (type.get()) {
            case "Star 2" -> STAR2_TEXTURE;
            case "Heart" -> HEART_TEXTURE;
            case "Crown" -> CROWN_TEXTURE;
            case "Lightning" -> LIGHTNING_TEXTURE;
            case "Snowflake" -> SNOWFLAKE_TEXTURE;
            default -> STAR1_TEXTURE;
        };
    }

    private void updateAndDraw(DrawContext ctx) {
        long now = System.currentTimeMillis();
        updateParticles(now);
        drawParticles(ctx);
    }

    private void updateParticles(long now) {
        if (lastUpdateMs == 0L) {
            lastUpdateMs = now;
            return;
        }

        float dt = (now - lastUpdateMs) / 16.6667f;
        lastUpdateMs = now;
        if (dt <= 0.0f) return;
        dt = Math.min(dt, 3.0f);

        Iterator<MouseParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            MouseParticle particle = iterator.next();
            particle.life -= dt;
            if (particle.life <= 0.0f) {
                iterator.remove();
                continue;
            }

            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vx *= (float) Math.pow(0.91f, dt);
            particle.vy = particle.vy * (float) Math.pow(0.91f, dt) + 0.018f * dt;
            particle.rotation += particle.rotationSpeed * dt;
        }
    }

    private void drawParticles(DrawContext ctx) {
        if (particles.isEmpty()) return;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0.0f, 0.0f, 900.0f);

        for (MouseParticle particle : particles) {
            float lifeRatio = MathHelper.clamp(particle.life / particle.maxLife, 0.0f, 1.0f);
            float alpha = lifeRatio * lifeRatio * ((particle.color >>> 24) & 0xFF) / 255.0f;
            if (alpha <= 0.004f) continue;

            float sizeNow = particle.size * (0.72f + (1.0f - lifeRatio) * 0.42f);
            drawTintedImage(ctx, BLOOM_TEXTURE, particle.x, particle.y, sizeNow * 1.85f, particle.color, alpha * 0.36f, particle.rotation * 0.35f);
            drawTintedImage(ctx, particle.texture, particle.x, particle.y, sizeNow, particle.color, alpha, particle.rotation);
        }

        ctx.getMatrices().pop();
    }

    private void drawTintedImage(DrawContext ctx, Identifier texture, float centerX, float centerY, float drawSize, int argbColor, float alpha, float rotationDeg) {
        if (alpha <= 0.004f) return;

        float half = drawSize * 0.5f;
        float x = centerX - half;
        float y = centerY - half;
        float r = ((argbColor >> 16) & 0xFF) / 255.0f;
        float g = ((argbColor >> 8) & 0xFF) / 255.0f;
        float b = (argbColor & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX, centerY, 0.0f);
        ctx.getMatrices().multiply(new Quaternionf().rotateZ((float) Math.toRadians(rotationDeg)));
        ctx.getMatrices().translate(-centerX, -centerY, 0.0f);

        var matrices = ctx.getMatrices().peek().getPositionMatrix();
        var buffer = net.minecraft.client.render.Tessellator.getInstance()
                .begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrices, x, y, 0.0f).texture(0.0f, 0.0f).color(r, g, b, alpha);
        buffer.vertex(matrices, x, y + drawSize, 0.0f).texture(0.0f, 1.0f).color(r, g, b, alpha);
        buffer.vertex(matrices, x + drawSize, y + drawSize, 0.0f).texture(1.0f, 1.0f).color(r, g, b, alpha);
        buffer.vertex(matrices, x + drawSize, y, 0.0f).texture(1.0f, 0.0f).color(r, g, b, alpha);
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());

        ctx.getMatrices().pop();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static final class MouseParticle {
        float x;
        float y;
        float vx;
        float vy;
        final float size;
        float life;
        final float maxLife;
        float rotation;
        final float rotationSpeed;
        final Identifier texture;
        final int color;

        private MouseParticle(float x, float y, float vx, float vy, float size, float life, float maxLife,
                              float rotation, float rotationSpeed, Identifier texture, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = life;
            this.maxLife = maxLife;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.texture = texture;
            this.color = color;
        }
    }
}
