package fun.enoughvisuals.systems.modules.modules.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ColorSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Register(name = "China Hat", id = "china_hat", description = "Cone hat above the player", category = Category.VISUAL)
public class ChinaHat extends Module {

    private static ChinaHat INSTANCE;

    public final ColorSetting color = add(new ColorSetting("Color", "color", "Hat color", 0xA0E85CFF));
    public final SliderSetting radius = add(new SliderSetting("Radius", "radius", "Hat radius", 0.7, 0.4, 1.5, 0.05, "m"));
    public final SliderSetting height = add(new SliderSetting("Height", "height", "Hat cone height", 0.35, 0.1, 1.0, 0.05, "m"));
    public final SliderSetting yPos = add(new SliderSetting("Y Offset", "y_pos", "Vertical offset", -0.4, -2.0, 2.0, 0.01, "m"));
    public final BooleanSetting outline = add(new BooleanSetting("Outline", "outline", "Render outline", true));
    public final SliderSetting outlineWidth = add(new SliderSetting("Thickness", "outline_width", "Outline width", 1.5, 0.5, 5.0, 0.1));
    public final BooleanSetting smartRender = add(new BooleanSetting("Smart Render", "smart_render", "Hide in first person", false));

    public ChinaHat() {
        INSTANCE = this;
    }

    public static void renderInWorld(WorldRenderContext context) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        if (mc.player == null || mc.world == null || context.matrixStack() == null || context.camera() == null) return;
        INSTANCE.render(context);
    }

    private void render(WorldRenderContext context) {
        if (!smartRender.get() && mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        float tickDelta = context.tickCounter() != null ? context.tickCounter().getTickDelta(true) : 1.0f;
        Vec3d camera = context.camera().getPos();
        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX()) - camera.x;
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()) + mc.player.getHeight() + 0.16 + yPos.get() - camera.y;
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ()) - camera.z;

        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        context.matrixStack().push();
        context.matrixStack().translate(x, y, z);
        drawHat(context.matrixStack().peek().getPositionMatrix());
        context.matrixStack().pop();

        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthFunc(oldDepthFunc);
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void drawHat(Matrix4f matrix) {
        int colorValue = color.get() & 0x00FFFFFF;
        float r = ((colorValue >> 16) & 0xFF) / 255f;
        float g = ((colorValue >> 8) & 0xFF) / 255f;
        float b = (colorValue & 0xFF) / 255f;
        float alpha = color.getAlphaF();
        float rad = radius.get().floatValue();
        float coneHeight = height.get().floatValue();
        int segments = 48;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder fan = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        fan.vertex(matrix, 0f, coneHeight, 0f).color(r, g, b, Math.min(1.0f, alpha + 0.1f));
        for (int i = 0; i <= segments; i++) {
            double angle = i * MathHelper.TAU / segments;
            fan.vertex(matrix, (float) Math.cos(angle) * rad, 0f, (float) Math.sin(angle) * rad).color(r, g, b, alpha * 0.6f);
        }
        BufferRenderer.drawWithGlobalProgram(fan.end());

        if (!outline.get()) return;

        RenderSystem.lineWidth(outlineWidth.get().floatValue());
        BufferBuilder line = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = i * MathHelper.TAU / segments;
            line.vertex(matrix, (float) Math.cos(angle) * rad, 0f, (float) Math.sin(angle) * rad).color(r, g, b, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(line.end());
    }
}
