package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShaderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Shaders");
    private static final String SHADER_PATH = "/assets/enough/shaders/core/";

    private static final Map<String, Integer> programs = new HashMap<>();
    private static final Map<String, Map<String, Integer>> uniformCache = new HashMap<>();
    private static final Map<String, Long> screenSizeCache = new HashMap<>();
    private static final Set<Integer> configuredTextureSamplers = new HashSet<>();

    private static boolean initialized = false;
    private static boolean failed = false;

    private static int quadVAO = 0;
    private static int texVAO = 0;
    private static int texVBO = 0;
    private static int quadVBO = 0;

    private static int previousProgram = 0;
    private static final FloatBuffer QUAD_UPLOAD = BufferUtils.createFloatBuffer(6 * 2);
    private static final FloatBuffer TEX_QUAD_UPLOAD = BufferUtils.createFloatBuffer(6 * 6);

    private static boolean programActive = false;

    public static final String ROUNDED_RECT = "rounded_rect";
    public static final String ROUNDED_RECT_OUTLINE = "rounded_rect_outline";
    public static final String ROUNDED_RECT_PROGRESS = "rounded_rect_progress";
    public static final String ROUNDED_RECT_GRADIENT = "rounded_rect_gradient";
    public static final String CIRCLE = "circle";
    public static final String SHADOW = "shadow";
    public static final String COLOR_PICKER = "color_picker";
    public static final String ROUNDED_CHECKERBOARD = "rounded_checkerboard";
    public static final String ROUNDED_TEXTURE = "rounded_texture";
    public static final String MENU_PREVIEW = "menu_preview";

    public static void init() {
        if (initialized || failed) return;
        try {
            RenderSystem.assertOnRenderThread();
            String vertSrc = readShader("rounded_rect.vsh");
            int vertShader = compileShader(GL20.GL_VERTEX_SHADER, vertSrc, "rounded_rect.vsh");

            // Твои старые шейдеры
            linkProgram(ROUNDED_RECT, vertShader, "rounded_rect.fsh");
            linkProgram(ROUNDED_RECT_OUTLINE, vertShader, "rounded_rect_outline.fsh");
            linkProgram(ROUNDED_RECT_PROGRESS, vertShader, "rounded_rect_progress.fsh");
            linkProgram(ROUNDED_RECT_GRADIENT, vertShader, "rounded_rect_gradient.fsh");
            linkProgram(CIRCLE, vertShader, "circle.fsh");
            linkProgram(SHADOW, vertShader, "shadow.fsh");
            linkProgram(COLOR_PICKER, vertShader, "color_picker.fsh");
            linkProgram(ROUNDED_CHECKERBOARD, vertShader, "rounded_checkerboard.fsh");
            GL20.glDeleteShader(vertShader);

            // --- ДОБАВЛЕНО ДЛЯ АВАТАРКИ ---
            String texVertSrc = readShader("rounded_texture.vsh");
            int texVertShader = compileShader(GL20.GL_VERTEX_SHADER, texVertSrc, "rounded_texture.vsh");
            linkProgram(ROUNDED_TEXTURE, texVertShader, "rounded_texture.fsh");
            linkProgram(MENU_PREVIEW, texVertShader, "menu_preview.fsh");
            GL20.glDeleteShader(texVertShader);

            initQuad();
            initTexQuad(); // --- ДОБАВЛЕНО ---

            initialized = true;
        } catch (Exception e) {
            LOGGER.error("Shader init failed", e);
            failed = true;
            cleanup();
        }
    }

    public static void drawTexturedQuad(String shaderName, DrawContext ctx, float x, float y, float w, float h, float pad,
                                        float u, float v, float uW, float vH, float tW, float tH) {
        Integer program = programs.get(shaderName);
        if (program == null) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            int fbW = mc.getWindow().getFramebufferWidth();
            int fbH = mc.getWindow().getFramebufferHeight();
            float scale = (float) mc.getWindow().getScaleFactor();

            Matrix4f mat = ctx != null ? ctx.getMatrices().peek().getPositionMatrix() : new Matrix4f().identity();

            // Расширяем геометрию на pad для сглаживания краев
            float minX = x - pad; float minY = y - pad;
            float maxX = x + w + pad; float maxY = y + h + pad;

            // UV тоже сдвигаем
            float uvStartX = (u - pad * (uW / w)) / tW;
            float uvStartY = (v - pad * (vH / h)) / tH;
            float uvEndX = (u + uW + pad * (uW / w)) / tW;
            float uvEndY = (v + vH + pad * (vH / h)) / tH;

            // Применяем ту же 2D матрицу напрямую, без четырёх временных Vector4f.
            float p1x = mat.m00() * minX + mat.m10() * minY + mat.m30();
            float p1y = mat.m01() * minX + mat.m11() * minY + mat.m31();
            float p2x = mat.m00() * minX + mat.m10() * maxY + mat.m30();
            float p2y = mat.m01() * minX + mat.m11() * maxY + mat.m31();
            float p3x = mat.m00() * maxX + mat.m10() * maxY + mat.m30();
            float p3y = mat.m01() * maxX + mat.m11() * maxY + mat.m31();
            float p4x = mat.m00() * maxX + mat.m10() * minY + mat.m30();
            float p4y = mat.m01() * maxX + mat.m11() * minY + mat.m31();

            // Переводим в экранные координаты (NDC)
            float ndcP1X = (p1x * scale / fbW) * 2f - 1f; float ndcP1Y = 1f - (p1y * scale / fbH) * 2f;
            float ndcP2X = (p2x * scale / fbW) * 2f - 1f; float ndcP2Y = 1f - (p2y * scale / fbH) * 2f;
            float ndcP3X = (p3x * scale / fbW) * 2f - 1f; float ndcP3Y = 1f - (p3y * scale / fbH) * 2f;
            float ndcP4X = (p4x * scale / fbW) * 2f - 1f; float ndcP4Y = 1f - (p4y * scale / fbH) * 2f;

            int locTexture = getUniformLocation(program, shaderName, "uTexture");
            if (locTexture >= 0 && configuredTextureSamplers.add(program)) {
                GL20.glUniform1i(locTexture, 0);
            }

            int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(texVAO);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, texVBO);

            TEX_QUAD_UPLOAD.clear();
            TEX_QUAD_UPLOAD
                    .put(ndcP1X).put(ndcP1Y).put(uvStartX).put(uvStartY).put(-pad).put(-pad)
                    .put(ndcP2X).put(ndcP2Y).put(uvStartX).put(uvEndY).put(-pad).put(h + pad)
                    .put(ndcP3X).put(ndcP3Y).put(uvEndX).put(uvEndY).put(w + pad).put(h + pad)
                    .put(ndcP1X).put(ndcP1Y).put(uvStartX).put(uvStartY).put(-pad).put(-pad)
                    .put(ndcP3X).put(ndcP3Y).put(uvEndX).put(uvEndY).put(w + pad).put(h + pad)
                    .put(ndcP4X).put(ndcP4Y).put(uvEndX).put(uvStartY).put(w + pad).put(-pad)
                    .flip();
            GL20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, TEX_QUAD_UPLOAD);

            GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 6);

            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(prevVAO);
        } catch (Exception e) {
            LOGGER.error("drawTexturedQuad failed", e);
        }
    }

    private static void initTexQuad() {
        texVAO = GL30.glGenVertexArrays();
        texVBO = GL20.glGenBuffers();
        int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        GL30.glBindVertexArray(texVAO);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, texVBO);
        // 6 вершин * 6 float (x,y, u,v, localX,localY) * 4 bytes
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, 6 * 6 * 4, GL20.GL_DYNAMIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 6 * 4, 0); // Position

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 6 * 4, 2 * 4); // UV

        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL20.GL_FLOAT, false, 6 * 4, 4 * 4); // LocalPos

        GL30.glBindVertexArray(prevVAO);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    private static void linkProgram(String name, int vertShader, String fragFile) {
        String fragSrc = readShader(fragFile);
        if (fragSrc == null) {
            LOGGER.warn("Fragment shader not found: {}", fragFile);
            return;
        }

        int fragShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc, fragFile);
        int program = GL20.glCreateProgram();

        GL20.glBindAttribLocation(program, 0, "Position");
        if (ROUNDED_TEXTURE.equals(name) || MENU_PREVIEW.equals(name)) {
            GL20.glBindAttribLocation(program, 1, "UV");
            GL20.glBindAttribLocation(program, 2, "LocalPos");
        }

        GL20.glAttachShader(program, vertShader);
        GL20.glAttachShader(program, fragShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            String log = GL20.glGetProgramInfoLog(program, 32768);
            LOGGER.error("Link error ({}): {}", name, log);
            GL20.glDeleteProgram(program);
            GL20.glDeleteShader(fragShader);
            return;
        }

        GL20.glDetachShader(program, vertShader);
        GL20.glDetachShader(program, fragShader);
        GL20.glDeleteShader(fragShader);

        programs.put(name, program);
        uniformCache.put(name, new HashMap<>());

        LOGGER.debug("Shader '{}' linked (id={})", name, program);
    }

    private static int compileShader(int type, String source, String name) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(id, 32768);
            GL20.glDeleteShader(id);
            throw new RuntimeException("Compile error (" + name + "): " + log);
        }
        return id;
    }

    private static void initQuad() {
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL20.glGenBuffers();

        int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        GL30.glBindVertexArray(quadVAO);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, quadVBO);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, 6 * 2 * 4, GL20.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL20.GL_FLOAT, false, 2 * 4, 0);

        GL30.glBindVertexArray(prevVAO);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
    }

    public static void useProgram(String name) {
        Integer program = programs.get(name);
        if (program == null) return;

        RenderSystem.assertOnRenderThread();

        previousProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        programActive = true;
    }

    public static void stopProgram() {
        if (!programActive) return;

        GL20.glUseProgram(previousProgram);
        previousProgram = 0;
        programActive = false;
    }

    public static void drawQuad(String shaderName, DrawContext ctx, float x, float y, float w, float h, float pad) {
        Integer program = programs.get(shaderName);
        if (program == null) return;

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            int fbW = mc.getWindow().getFramebufferWidth();
            int fbH = mc.getWindow().getFramebufferHeight();
            float scale = (float) mc.getWindow().getScaleFactor();

            Matrix4f mat = ctx != null ? ctx.getMatrices().peek().getPositionMatrix() : new Matrix4f().identity();
            float x0 = x - pad, y0 = y - pad;
            float x1 = x + w + pad, y1 = y + h + pad;
            float p1x = mat.m00() * x0 + mat.m10() * y0 + mat.m30();
            float p1y = mat.m01() * x0 + mat.m11() * y0 + mat.m31();
            float p2x = mat.m00() * x1 + mat.m10() * y0 + mat.m30();
            float p2y = mat.m01() * x1 + mat.m11() * y0 + mat.m31();
            float p3x = mat.m00() * x1 + mat.m10() * y1 + mat.m30();
            float p3y = mat.m01() * x1 + mat.m11() * y1 + mat.m31();
            float p4x = mat.m00() * x0 + mat.m10() * y1 + mat.m30();
            float p4y = mat.m01() * x0 + mat.m11() * y1 + mat.m31();

            float minX = Math.min(Math.min(p1x, p2x), Math.min(p3x, p4x));
            float minY = Math.min(Math.min(p1y, p2y), Math.min(p3y, p4y));
            float maxX = Math.max(Math.max(p1x, p2x), Math.max(p3x, p4x));
            float maxY = Math.max(Math.max(p1y, p2y), Math.max(p3y, p4y));

            float px1 = minX * scale;
            float py1 = minY * scale;
            float px2 = maxX * scale;
            float py2 = maxY * scale;

            float ndcX1 = (px1 / fbW) * 2f - 1f;
            float ndcY1 = 1f - (py1 / fbH) * 2f;
            float ndcX2 = (px2 / fbW) * 2f - 1f;
            float ndcY2 = 1f - (py2 / fbH) * 2f;

            int loc = getUniformLocation(program, shaderName, "uScreenSize");
            long packedScreenSize = ((long) fbW << 32) | (fbH & 0xFFFFFFFFL);
            if (loc >= 0 && screenSizeCache.getOrDefault(shaderName, Long.MIN_VALUE) != packedScreenSize) {
                GL20.glUniform2f(loc, (float) fbW, (float) fbH);
                screenSizeCache.put(shaderName, packedScreenSize);
            }

            int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

            GL30.glBindVertexArray(quadVAO);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, quadVBO);

            QUAD_UPLOAD.clear();
            QUAD_UPLOAD
                    .put(ndcX1).put(ndcY1)
                    .put(ndcX1).put(ndcY2)
                    .put(ndcX2).put(ndcY2)
                    .put(ndcX1).put(ndcY1)
                    .put(ndcX2).put(ndcY2)
                    .put(ndcX2).put(ndcY1)
                    .flip();
            GL20.glBufferSubData(GL20.GL_ARRAY_BUFFER, 0, QUAD_UPLOAD);

            GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 6);

            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(prevVAO);
        } catch (Exception e) {
            LOGGER.error("drawQuad failed for '{}': {}", shaderName, e.getMessage());

            try {
                GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
                GL30.glBindVertexArray(0);
            } catch (Exception ignored) {}
        }
    }

    public static void setUniform1f(String shaderName, String uniformName, float value) {
        int program = programs.getOrDefault(shaderName, 0);
        if (program == 0) return;
        int loc = getUniformLocation(program, shaderName, uniformName);
        if (loc >= 0) GL20.glUniform1f(loc, value);
    }

    public static void setUniform2f(String shaderName, String uniformName, float x, float y) {
        int program = programs.getOrDefault(shaderName, 0);
        if (program == 0) return;
        int loc = getUniformLocation(program, shaderName, uniformName);
        if (loc >= 0) GL20.glUniform2f(loc, x, y);
    }

    public static void setUniform4f(String shaderName, String uniformName, float x, float y, float z, float w) {
        int program = programs.getOrDefault(shaderName, 0);
        if (program == 0) return;
        int loc = getUniformLocation(program, shaderName, uniformName);
        if (loc >= 0) GL20.glUniform4f(loc, x, y, z, w);
    }

    private static int getUniformLocation(int program, String shaderName, String uniformName) {
        Map<String, Integer> cache = uniformCache.computeIfAbsent(shaderName, k -> new HashMap<>());
        return cache.computeIfAbsent(uniformName, name -> GL20.glGetUniformLocation(program, name));
    }

    public static boolean isAvailable() {
        if (!initialized && !failed) {
            if (RenderSystem.isOnRenderThread()) {
                init();
            } else {
                RenderSystem.recordRenderCall(ShaderManager::init);
                return false;
            }
        }
        return initialized;
    }

    public static boolean hasProgram(String name) {
        return isAvailable() && programs.containsKey(name);
    }

    public static void cleanup() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(ShaderManager::cleanupInternal);
        } else {
            cleanupInternal();
        }
    }

    private static void cleanupInternal() {

        if (programActive) {
            stopProgram();
        }

        for (int program : programs.values()) {
            GL20.glDeleteProgram(program);
        }
        programs.clear();
        uniformCache.clear();
        screenSizeCache.clear();
        configuredTextureSamplers.clear();

        if (quadVAO != 0) {
            GL30.glDeleteVertexArrays(quadVAO);
            quadVAO = 0;
        }
        if (quadVBO != 0) {
            GL20.glDeleteBuffers(quadVBO);
            quadVBO = 0;
        }
        if (texVAO != 0) {
            GL30.glDeleteVertexArrays(texVAO);
            texVAO = 0;
        }
        if (texVBO != 0) {
            GL20.glDeleteBuffers(texVBO);
            texVBO = 0;
        }

        initialized = false;
        failed = false;
    }

    public static void reload() {
        cleanupInternal();
        init();
    }

    public static int getProgram(String name) {
        return programs.getOrDefault(name, 0);
    }

    private static String readShader(String fileName) {
        String path = SHADER_PATH + fileName;
        try {
            InputStream is = ShaderManager.class.getResourceAsStream(path);
            if (is == null) {
                String alt = path.startsWith("/") ? path.substring(1) : "/" + path;
                is = ShaderManager.class.getResourceAsStream(alt);
            }
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            }
            if (is == null) {
                LOGGER.warn("Shader not found: {}", path);
                return null;
            }
            byte[] bytes = is.readAllBytes();
            is.close();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Error reading shader: {}", path, e);
            return null;
        }
    }
}
