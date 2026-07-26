package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class WorldEffectShaders {

    private static final ShaderProgramKey SOFT_PARTICLE = key("world_soft_particle");
    private static final ShaderProgramKey SOLID_PARTICLE = key("world_solid_particle");
    private static final ShaderProgramKey LINK = key("world_link");
    private static final ShaderProgramKey COMET = key("world_comet");
    private static final ShaderProgramKey CUBE = key("world_cube");

    private WorldEffectShaders() {
    }

    public static ShaderProgram useSoftParticle(float time, float glowStrength) {
        ShaderProgram shader = RenderSystem.setShader(SOFT_PARTICLE);
        set(shader, "Time", time);
        set(shader, "GlowStrength", glowStrength);
        return shader;
    }

    public static ShaderProgram useSolidParticle() {
        return RenderSystem.setShader(SOLID_PARTICLE);
    }

    public static ShaderProgram useLink(float time, float glowStrength) {
        ShaderProgram shader = RenderSystem.setShader(LINK);
        set(shader, "Time", time);
        set(shader, "GlowStrength", glowStrength);
        return shader;
    }

    public static ShaderProgram useComet(float time, float glowStrength) {
        ShaderProgram shader = RenderSystem.setShader(COMET);
        set(shader, "Time", time);
        set(shader, "GlowStrength", glowStrength);
        return shader;
    }

    public static ShaderProgram useCube(float time, float glowStrength) {
        ShaderProgram shader = RenderSystem.setShader(CUBE);
        set(shader, "Time", time);
        set(shader, "GlowStrength", glowStrength);
        return shader;
    }

    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(
                Identifier.of("enough", "core/" + name),
                VertexFormats.POSITION_TEXTURE_COLOR,
                Defines.EMPTY
        );
    }

    private static void set(ShaderProgram shader, String name, float value) {
        if (shader == null) return;
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
