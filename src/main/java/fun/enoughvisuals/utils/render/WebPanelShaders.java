package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/** Analytic, derivative-antialiased silhouettes for spatial browser surfaces. */
public final class WebPanelShaders {
    private static final ShaderProgramKey COLOR = key("web_panel_sdf_color");
    private static final ShaderProgramKey TEXTURE = key("web_panel_sdf_texture");
    private static final ShaderProgramKey BLUR = key("web_panel_sdf_blur");
    private static final ShaderProgramKey CORNER = key("web_panel_sdf_corner");

    private WebPanelShaders() {
    }

    public static ShaderProgram useColor(float width, float height, float radius) {
        return configure(RenderSystem.setShader(COLOR), width, height, radius);
    }

    public static ShaderProgram useTexture(float width, float height, float radius) {
        return configure(RenderSystem.setShader(TEXTURE), width, height, radius);
    }

    public static ShaderProgram useBlur(float width, float height, float radius) {
        return configure(RenderSystem.setShader(BLUR), width, height, radius);
    }

    public static ShaderProgram useCorner(float width, float height, float radius,
                                          float thickness, boolean rightSide) {
        ShaderProgram shader = configure(RenderSystem.setShader(CORNER), width, height, radius);
        if (shader == null) return null;
        GlUniform stroke = shader.getUniform("Thickness");
        if (stroke != null) stroke.set(thickness);
        GlUniform side = shader.getUniform("RightSide");
        if (side != null) side.set(rightSide ? 1 : 0);
        return shader;
    }

    private static ShaderProgram configure(ShaderProgram shader, float width, float height, float radius) {
        if (shader == null) return null;
        GlUniform size = shader.getUniform("Size");
        if (size != null) size.set(width, height);
        GlUniform corner = shader.getUniform("Radius");
        if (corner != null) corner.set(radius);
        GlUniform exponent = shader.getUniform("Exponent");
        if (exponent != null) exponent.set(3.35f);
        return shader;
    }

    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(
                Identifier.of("enough", "core/" + name),
                VertexFormats.POSITION_TEXTURE_COLOR,
                Defines.EMPTY
        );
    }
}
