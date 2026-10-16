package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A GLSL program outside vanilla's shader pipeline, drawn as one fullscreen
 * quad. Uniform locations are cached so per-frame updates do not allocate.
 */
public final class RawShaderProgram implements AutoCloseable {
    private static VertexBuffer fullscreenQuad;

    private final int program;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public RawShaderProgram(String vertexPath, String fragmentPath) {
        int vertex = 0;
        int fragment = 0;
        int created = GlStateManager.glCreateProgram();
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, read(vertexPath));
            fragment = compile(GL20.GL_FRAGMENT_SHADER, read(fragmentPath));
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glBindAttribLocation(created, 0, "Position");
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
                throw new IllegalStateException("Shader link failed (" + fragmentPath + "): "
                        + GL20.glGetProgramInfoLog(created));
            }
        } catch (RuntimeException error) {
            GL20.glDeleteProgram(created);
            throw error;
        } finally {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
        }
        this.program = created;
    }

    public void bind() {
        GlStateManager._glUseProgram(program);
    }

    public void unbind() {
        GlStateManager._glUseProgram(0);
    }

    public void set(String name, int value) {
        GL20.glUniform1i(location(name), value);
    }

    public void set(String name, float value) {
        GL20.glUniform1f(location(name), value);
    }

    public void set(String name, float x, float y) {
        GL20.glUniform2f(location(name), x, y);
    }

    public void setColor(String name, int argb, float alpha) {
        GL20.glUniform4f(location(name),
                ((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, alpha);
    }

    /** Draws a clip-space quad with this program, then hands VAO state back to vanilla. */
    public void drawFullscreen() {
        VertexBuffer quad = quad();
        quad.bind();
        quad.draw();
        VertexBuffer.unbind();
        BufferRenderer.reset();
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(program);
        uniforms.clear();
    }

    private int location(String name) {
        Integer cached = uniforms.get(name);
        if (cached != null) return cached;
        int location = GL20.glGetUniformLocation(program, name);
        uniforms.put(name, location);
        return location;
    }

    private static VertexBuffer quad() {
        if (fullscreenQuad == null) {
            RenderSystem.assertOnRenderThread();
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            builder.vertex(-1.0F, -1.0F, 0.0F);
            builder.vertex(1.0F, -1.0F, 0.0F);
            builder.vertex(1.0F, 1.0F, 0.0F);
            builder.vertex(-1.0F, 1.0F, 0.0F);
            fullscreenQuad = new VertexBuffer(GlUsage.STATIC_WRITE);
            fullscreenQuad.bind();
            fullscreenQuad.upload(builder.end());
            VertexBuffer.unbind();
        }
        return fullscreenQuad;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 2048);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static String read(String path) {
        try (InputStream in = RawShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing shader " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read shader " + path, error);
        }
    }
}
