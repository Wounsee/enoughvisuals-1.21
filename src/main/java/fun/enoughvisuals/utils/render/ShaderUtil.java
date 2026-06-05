package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShaderUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/ShaderUtil");

    private static int msdfProgram = 0;
    private static boolean attempted = false;
    private static boolean sodiumDetected = false;
    private static final Map<String, Integer> msdfUniforms = new HashMap<>();

    private static boolean isSodiumPresent() {
        try {
            return FabricLoader.getInstance().isModLoaded("sodium");
        } catch (Exception e) {
            return false;
        }
    }

    public static int getMsdfProgram() {
        if (msdfProgram > 0) return msdfProgram;
        if (attempted) return 0;
        attempted = true;

        
        sodiumDetected = isSodiumPresent();
        if (sodiumDetected) {
            LOGGER.info("Sodium detected — MSDF shader DISABLED");
            return 0;
        }

        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.debug("Not on render thread, skipping MSDF shader load");
            return 0;
        }

        try {
            GL20.glGetError();
        } catch (Exception e) {
            LOGGER.debug("GL context not ready");
            attempted = false;
            return 0;
        }

        int prevProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        try {
            String vertSrc = readResource("/assets/enough/shaders/core/enough_msdf.vsh");
            String fragSrc = readResource("/assets/enough/shaders/core/enough_msdf.fsh");

            if (vertSrc == null) {
                LOGGER.warn("MSDF vertex shader not found");
                return 0;
            }
            if (fragSrc == null) {
                LOGGER.warn("MSDF fragment shader not found");
                return 0;
            }

            int vert = compileShader(GL20.GL_VERTEX_SHADER, vertSrc, "enough_msdf.vsh");
            if (vert == 0) return 0;

            int frag = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc, "enough_msdf.fsh");
            if (frag == 0) {
                GL20.glDeleteShader(vert);
                return 0;
            }

            int program = GL20.glCreateProgram();
            GL20.glBindAttribLocation(program, 0, "Position");
            GL20.glBindAttribLocation(program, 1, "UV0");

            GL20.glAttachShader(program, vert);
            GL20.glAttachShader(program, frag);
            GL20.glLinkProgram(program);

            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                String log = GL20.glGetProgramInfoLog(program, 32768);
                LOGGER.error("MSDF link error: {}", log);
                GL20.glDeleteProgram(program);
                GL20.glDeleteShader(vert);
                GL20.glDeleteShader(frag);
                return 0;
            }

            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);

            msdfProgram = program;
            msdfUniforms.clear();

            LOGGER.info("MSDF shader loaded (program={})", program);
            return program;

        } catch (Exception e) {
            LOGGER.error("Failed to load MSDF shader: {}", e.getMessage(), e);
            return 0;
        } finally {
            GL20.glUseProgram(prevProgram);
        }
    }

    public static boolean isMsdfReady() {
        if (sodiumDetected) return false;
        return msdfProgram > 0 || getMsdfProgram() > 0;
    }

    public static void useMsdf() {
        if (msdfProgram > 0) GL20.glUseProgram(msdfProgram);
    }

    public static void stopProgram() {
        GL20.glUseProgram(0);
    }

    public static void setMsdfColor(float r, float g, float b, float a) {
        int loc = getLoc("uColor");
        if (loc >= 0) GL20.glUniform4f(loc, r, g, b, a);
    }

    public static void setMsdfPxRange(float pxRange) {
        int loc = getLoc("uPxRange");
        if (loc >= 0) GL20.glUniform1f(loc, pxRange);
    }

    public static void setMsdfSampler(int unit) {
        int loc = getLoc("Sampler0");
        if (loc >= 0) GL20.glUniform1i(loc, unit);
    }

    public static void setMsdfModelViewMat(float[] matrix) {
        int loc = getLoc("ModelViewMat");
        if (loc >= 0) GL20.glUniformMatrix4fv(loc, false, matrix);
    }

    public static void setMsdfProjMat(float[] matrix) {
        int loc = getLoc("ProjMat");
        if (loc >= 0) GL20.glUniformMatrix4fv(loc, false, matrix);
    }

    private static int getLoc(String name) {
        if (msdfProgram <= 0) return -1;
        return msdfUniforms.computeIfAbsent(name,
                n -> GL20.glGetUniformLocation(msdfProgram, n));
    }

    private static int compileShader(int type, String source, String name) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(id, 32768);
            LOGGER.error("Compile error ({}): {}", name, log);
            GL20.glDeleteShader(id);
            return 0;
        }
        return id;
    }

    private static String readResource(String path) {
        try {
            InputStream is = ShaderUtil.class.getResourceAsStream(path);
            if (is == null) {
                String clean = path.startsWith("/") ? path.substring(1) : path;
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(clean);
            }
            if (is == null) return null;
            byte[] bytes = is.readAllBytes();
            is.close();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static void reset() {
        if (msdfProgram > 0) {
            GL20.glDeleteProgram(msdfProgram);
            msdfProgram = 0;
        }
        msdfUniforms.clear();
        attempted = false;
        sodiumDetected = false;
    }
}