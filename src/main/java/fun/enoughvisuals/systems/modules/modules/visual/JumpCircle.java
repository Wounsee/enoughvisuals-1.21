package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Register(name = "Jump Circle", id = "jump_circle", description = "Circle under your feet when jumping", category = Category.VISUAL)
public class JumpCircle extends Module {

    private static final Identifier CIRCLE_TEXTURE = Identifier.of("enough", "textures/jump.png");
    private static JumpCircle INSTANCE;

    public final ColorSetting color = add(new ColorSetting("Color", "color", "Circle color", 0xFFE85CFF));
    public final SliderSetting speed = add(new SliderSetting("Speed", "speed", "Circle animation speed", 1.0, 0.1, 5.0, 0.1));
    public final SliderSetting lifetime = add(new SliderSetting("Lifetime", "lifetime", "Circle lifetime", 1.5, 0.5, 5.0, 0.1, "s"));
    public final SliderSetting maxSize = add(new SliderSetting("Size", "size", "Maximum circle size", 2.5, 0.5, 5.0, 0.1, "m"));

    private final List<Circle> circles = new ArrayList<>();

    public JumpCircle() {
        INSTANCE = this;
    }

    public static void onLocalPlayerJump() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        INSTANCE.circles.add(new Circle(mc.player.getPos().add(0.0, 0.05, 0.0)));
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    @Override
    protected void onDisable() {
        circles.clear();
    }

    @Override
    public void onTick() {
        if (!isInGame()) {
            circles.clear();
        }
    }

    private void render(WorldRenderContext context) {
        if (circles.isEmpty()) return;

        long now = System.currentTimeMillis();
        float maxLifeMs = Math.max(1.0f, lifetime.get().floatValue() * 1000.0f);
        Vec3d camera = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);

        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            float ageMs = now - circle.spawnTime;
            if (ageMs > maxLifeMs) {
                iterator.remove();
                continue;
            }

            float lifeProgress = ageMs / maxLifeMs;
            float animProgress = ageMs / 1000.0f * speed.get().floatValue();
            float currentSize = Math.min(maxSize.get().floatValue(), animProgress * maxSize.get().floatValue());
            if (currentSize <= 0.01f) continue;

            matrices.push();
            matrices.translate(circle.pos.x - camera.x, circle.pos.y - camera.y, circle.pos.z - camera.z);
            drawTexture(matrices, currentSize, color.get() & 0x00FFFFFF, (1.0f - lifeProgress) * color.getAlphaF());
            matrices.pop();
        }

        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawTexture(MatrixStack matrices, float size, int rgb, float alpha) {
        float s = size * 0.5f;
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -s, 0f, -s).texture(0f, 0f).color(r, g, b, alpha);
        buffer.vertex(matrix, -s, 0f, s).texture(0f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, s, 0f, s).texture(1f, 1f).color(r, g, b, alpha);
        buffer.vertex(matrix, s, 0f, -s).texture(1f, 0f).color(r, g, b, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private record Circle(Vec3d pos, long spawnTime) {
        private Circle(Vec3d pos) {
            this(pos, System.currentTimeMillis());
        }
    }
}
