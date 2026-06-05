package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.mixins.accessors.GameRendererAccessor;
import fun.enoughvisuals.systems.modules.modules.visual.SkyBox;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public final class SkyBoxRenderer {

    private static final ShaderProgramKey CAUSTIC_SHADER = new ShaderProgramKey(
            net.minecraft.util.Identifier.of("enough", "core/sky/sky_caustic"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );
    private static final ShaderProgramKey CLOUDS_SHADER = new ShaderProgramKey(
            net.minecraft.util.Identifier.of("enough", "core/sky/sky_clouds"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );

    private static long startMillis = -1L;

    private SkyBoxRenderer() {
    }

    public static void reset() {
        startMillis = -1L;
    }

    public static void drawOverlay(WorldRenderContext context) {
        SkyBox module = SkyBox.active();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (module == null || mc.player == null || mc.world == null) return;

        if (startMillis < 0L) {
            startMillis = System.currentTimeMillis();
        }

        ShaderProgram shader = RenderSystem.setShader(module.mode.is("Clouds") ? CLOUDS_SHADER : CAUSTIC_SHADER);
        if (shader == null) return;

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();

        int rgb = module.color.get();
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float yawRad = (float) Math.toRadians(-mc.gameRenderer.getCamera().getYaw());
        float pitchRad = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        float tickDelta = context.tickCounter() == null ? 1.0f : context.tickCounter().getTickDelta(true);
        float fov = ((GameRendererAccessor) mc.gameRenderer).invokeGetFov(mc.gameRenderer.getCamera(), tickDelta, true);

        setUniform(shader, "uTime", time);
        setUniform(shader, "uResolution", width, height);
        setUniform(shader, "uColor", r, g, b);
        setUniform(shader, "uAlpha", module.alpha.get().floatValue());
        setUniform(shader, "uSpeed", module.speed.get().floatValue());
        setUniform(shader, "uScale", module.scale.get().floatValue());
        setUniform(shader, "uIntensity", module.intensity.get().floatValue());
        setUniform(shader, "uCameraDir", yawRad, pitchRad);
        setUniform(shader, "uFov", fov);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(-1f, -1f, 1f);
        buffer.vertex(1f, -1f, 1f);
        buffer.vertex(1f, 1f, 1f);
        buffer.vertex(-1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform == null) return;

        switch (values.length) {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            case 4 -> uniform.set(values[0], values[1], values[2], values[3]);
            default -> {
            }
        }
    }
}

