package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;
import java.util.ArrayList;
import java.util.List;

public class RenderUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float EDGE_SMOOTHING = 0.65f;
    private static final float DEFAULT_SQUIRCLE_EXPONENT = 3.35f;
    private static float activeSquircleExponent = DEFAULT_SQUIRCLE_EXPONENT;
    private static long lastFrameTimeNs = System.nanoTime();
    private static float cachedFrameDelta = 1.0f;

    private static void applySquircleShape(String shader) {
        ShaderManager.setUniform1f(shader, "uExponent", activeSquircleExponent);
    }

    public static void drawRoundedRect(DrawContext ctx, float x, float y, float width, float height,
                                       float radius, int color) {
        drawRoundedRect(ctx, x, y, width, height, radius, radius, radius, radius, color);
    }

    public static void drawRoundedMask(DrawContext ctx, float x, float y, float width, float height, float radius) {
        if (width <= 0 || height <= 0) return;

        float maxR = Math.min(width, height) * 0.5f;
        float r = clampRadius(radius, maxR);
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        float cr = 1f, cg = 1f, cb = 1f, ca = 1f;
        buffer.vertex(matrix, x + width * 0.5f, y + height * 0.5f, 0).color(cr, cg, cb, ca);

        int seg = 22;
        addCornerVertices(buffer, matrix, x + r, y + r, r, 180, 270, seg, cr, cg, cb, ca);
        addCornerVertices(buffer, matrix, x + width - r, y + r, r, 270, 360, seg, cr, cg, cb, ca);
        addCornerVertices(buffer, matrix, x + width - r, y + height - r, r, 0, 90, seg, cr, cg, cb, ca);
        addCornerVertices(buffer, matrix, x + r, y + height - r, r, 90, 180, seg, cr, cg, cb, ca);

        float firstAngle = (float) Math.toRadians(180);
        buffer.vertex(matrix,
                x + r + (float) Math.cos(firstAngle) * r,
                y + r + (float) Math.sin(firstAngle) * r, 0).color(cr, cg, cb, ca);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    public static void drawSmoothFilledCircle(Matrix4f matrix, int colorARGB, double x, double y, double radius) {
        float alpha = (colorARGB >> 24 & 0xFF) / 255.0f;
        float red = (colorARGB >> 16 & 0xFF) / 255.0f;
        float green = (colorARGB >> 8 & 0xFF) / 255.0f;
        float blue = (colorARGB & 0xFF) / 255.0f;

        BufferBuilder bufferBuilder = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < 360; i += 5) {
            double rad1 = Math.toRadians(i);
            double rad2 = Math.toRadians(i + 5);

            float px1 = (float) (x + Math.sin(rad1) * radius);
            float py1 = (float) (y - Math.cos(rad1) * radius);

            float px2 = (float) (x + Math.sin(rad2) * radius);
            float py2 = (float) (y - Math.cos(rad2) * radius);

            bufferBuilder.vertex(matrix, (float) x, (float) y, 0).color(red, green, blue, alpha);

            bufferBuilder.vertex(matrix, px1, py1, 0).color(red, green, blue, alpha);

            bufferBuilder.vertex(matrix, px2, py2, 0).color(red, green, blue, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawSmoothArcOutline(Matrix4f matrix, int colorARGB, double x, double y, double radius, float lineWidth, float startAngleDegrees, float endAngleDegrees) {
        if (endAngleDegrees <= 0) return;

        float alpha = (colorARGB >> 24 & 0xFF) / 255.0f;
        float red = (colorARGB >> 16 & 0xFF) / 255.0f;
        float green = (colorARGB >> 8 & 0xFF) / 255.0f;
        float blue = (colorARGB & 0xFF) / 255.0f;

        BufferBuilder bufferBuilder = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float innerRadius = (float) radius;
        float outerRadius = (float) (radius + lineWidth);

        float step = 2.0f;

        for (float i = startAngleDegrees; i < startAngleDegrees + endAngleDegrees; i += step) {
            float currentAngle = i;
            float nextAngle = Math.min(i + step, startAngleDegrees + endAngleDegrees);

            double rad1 = Math.toRadians(currentAngle);
            double rad2 = Math.toRadians(nextAngle);

            float sin1 = (float) Math.sin(rad1);
            float cos1 = (float) Math.cos(rad1);
            float sin2 = (float) Math.sin(rad2);
            float cos2 = (float) Math.cos(rad2);

            float p1x = (float) (x + sin1 * outerRadius);
            float p1y = (float) (y - cos1 * outerRadius);
            float p2x = (float) (x + sin2 * outerRadius);
            float p2y = (float) (y - cos2 * outerRadius);

            float p3x = (float) (x + sin2 * innerRadius);
            float p3y = (float) (y - cos2 * innerRadius);
            float p4x = (float) (x + sin1 * innerRadius);
            float p4y = (float) (y - cos1 * innerRadius);

            bufferBuilder.vertex(matrix, p1x, p1y, 0).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix, p2x, p2y, 0).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix, p3x, p3y, 0).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix, p4x, p4y, 0).color(red, green, blue, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawRoundedRect(DrawContext ctx, float x, float y, float width, float height,
                                       float topLeft, float topRight, float bottomRight, float bottomLeft,
                                       int color) {
        if (width <= 0 || height <= 0) return;

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        float maxR = Math.min(width, height) / 2f;
        topLeft    = clampRadius(topLeft,    maxR);
        topRight   = clampRadius(topRight,   maxR);
        bottomRight= clampRadius(bottomRight,maxR);
        bottomLeft = clampRadius(bottomLeft, maxR);

        if (topLeft <= 0.01f && topRight <= 0.01f && bottomRight <= 0.01f && bottomLeft <= 0.01f) {
            drawRect(ctx, x, y, width, height, color);
            return;
        }

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (transformed) {
            // При любой rotation — всегда CPU, шейдер не может работать с повёрнутой матрицей
            drawRoundedRectCPUTransformed(matrix, x, y, width, height,
                    topLeft, topRight, bottomRight, bottomLeft, r, g, b, a);
        } else if (ShaderManager.isAvailable()) {
            beginShaderDraw();
            drawRoundedRectShader(ctx, x, y, width, height,
                    topLeft, topRight, bottomRight, bottomLeft, r, g, b, a);
            endShaderDraw();
        } else {
            drawRoundedRectCPU(matrix, x, y, width, height,
                    topLeft, topRight, bottomRight, bottomLeft, r, g, b, a);
        }
    }

    public static void drawRoundedOutline(DrawContext ctx, float x, float y, float width, float height,
                                          float radius, float lineWidth, int color) {
        drawRoundedOutline(ctx, x, y, width, height, radius, radius, radius, radius, lineWidth, color);
    }

    public static void drawRoundedOutline(DrawContext ctx, float x, float y, float width, float height,
                                          float tl, float tr, float br, float bl,
                                          float lineWidth, int color) {
        if (width <= 0 || height <= 0) return;

        float a = ((color >> 24) & 0xFF) / 255f;
        if (a <= 0.003f) return;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float maxR = Math.min(width, height) / 2f;
        tl = clampRadius(tl, maxR);
        tr = clampRadius(tr, maxR);
        br = clampRadius(br, maxR);
        bl = clampRadius(bl, maxR);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        // При rotation — CPU fallback (outline при наклоне всё равно выглядит нормально)
        if (!transformed && ShaderManager.isAvailable()) {
            beginShaderDraw();
            drawRoundedOutlineShader(ctx, x, y, width, height, tl, tr, br, bl, lineWidth, r, g, b, a);
            endShaderDraw();
        } else {
            drawRoundedOutlineCPU(matrix, x, y, width, height, tl, lineWidth, r, g, b, a);
        }
    }

    /** Anti-aliased clockwise squircle outline, starting at the top center. */
    public static void drawSquircleProgressOutline(DrawContext ctx, float x, float y,
                                                   float width, float height, float radius,
                                                   float lineWidth, float progress, int color) {
        drawSquircleProgressOutlineInternal(ctx, x, y, width, height, radius,
                lineWidth, progress, color, EDGE_SMOOTHING);
    }

    public static void drawCrispSquircleProgressOutline(DrawContext ctx, float x, float y,
                                                        float width, float height, float radius,
                                                        float lineWidth, float progress, int color) {
        drawSquircleProgressOutlineInternal(ctx, x, y, width, height, radius,
                lineWidth, progress, color, 0.24f);
    }

    private static void drawSquircleProgressOutlineInternal(DrawContext ctx, float x, float y,
                                                             float width, float height, float radius,
                                                             float lineWidth, float progress, int color,
                                                             float smoothing) {
        progress = Math.max(0f, Math.min(1f, progress));
        if (width <= 0f || height <= 0f || progress <= 0.001f) return;
        float a = ((color >>> 24) & 0xFF) / 255f;
        if (a <= 0.003f) return;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        radius = clampRadius(radius, Math.min(width, height) * 0.5f);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        if (!hasComplex2DTransform(matrix)
                && ShaderManager.hasProgram(ShaderManager.ROUNDED_RECT_PROGRESS)) {
            beginShaderDraw();
            ShaderRect rect = toShaderRect(ctx, x, y, width, height);
            float radiusScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
            String name = ShaderManager.ROUNDED_RECT_PROGRESS;
            ShaderManager.useProgram(name);
            applySquircleShape(name);
            ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
            ShaderManager.setUniform4f(name, "uRadii", radius * radiusScale, radius * radiusScale,
                    radius * radiusScale, radius * radiusScale);
            ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
            ShaderManager.setUniform1f(name, "uSmoothing", smoothing * radiusScale);
            ShaderManager.setUniform1f(name, "uLineWidth", lineWidth * radiusScale);
            ShaderManager.setUniform1f(name, "uProgress", progress);
            ShaderManager.drawQuad(name, ctx, x, y, width, height,
                    lineWidth + smoothing + 1f);
            ShaderManager.stopProgram();
            endShaderDraw();
            return;
        }

        // Safe fallback for unsupported GPUs. Full outlines stay smooth and avoid
        // the former segmented/pixelated CPU path.
        drawRoundedOutline(ctx, x, y, width, height, radius, lineWidth,
                withAlpha(color, a * progress));
    }

    public static void drawRoundedGradient(DrawContext ctx, float x, float y, float width, float height,
                                           float radius, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        drawRoundedGradient(ctx, x, y, width, height, radius, radius, radius, radius,
                topLeft, topRight, bottomRight, bottomLeft);
    }

    public static void drawRoundedGradient(DrawContext ctx, float x, float y, float width, float height,
                                           float rtl, float rtr, float rbr, float rbl,
                                           int colorTL, int colorTR, int colorBR, int colorBL) {
        if (width <= 0 || height <= 0) return;

        float maxR = Math.min(width, height) / 2f;
        rtl = clampRadius(rtl, maxR);
        rtr = clampRadius(rtr, maxR);
        rbr = clampRadius(rbr, maxR);
        rbl = clampRadius(rbl, maxR);

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (ShaderManager.isAvailable() && !transformed) {
            beginShaderDraw();
            drawRoundedGradientShader(ctx, x, y, width, height, rtl, rtr, rbr, rbl,
                    colorTL, colorTR, colorBR, colorBL);
            endShaderDraw();
        } else {
            drawQuadGradient(ctx, x, y, width, height, colorTL, colorTR, colorBR, colorBL);
        }
    }

    public static void drawRoundedGradientV(DrawContext ctx, float x, float y, float width, float height,
                                            float radius, int colorTop, int colorBottom) {
        drawRoundedGradient(ctx, x, y, width, height, radius, colorTop, colorTop, colorBottom, colorBottom);
    }

    public static void drawRoundedGradientH(DrawContext ctx, float x, float y, float width, float height,
                                            float radius, int colorLeft, int colorRight) {
        drawRoundedGradient(ctx, x, y, width, height, radius, colorLeft, colorRight, colorRight, colorLeft);
    }

    public static void drawRect(DrawContext ctx, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        fillQuad(matrix, x, y, x + width, y + height, r, g, b, a);
        RenderSystem.disableBlend();
    }

    public static void drawRectOutline(DrawContext ctx, float x, float y, float width, float height,
                                       float lineWidth, int color) {
        drawRect(ctx, x, y, width, lineWidth, color);
        drawRect(ctx, x, y + height - lineWidth, width, lineWidth, color);
        drawRect(ctx, x, y + lineWidth, lineWidth, height - lineWidth * 2, color);
        drawRect(ctx, x + width - lineWidth, y + lineWidth, lineWidth, height - lineWidth * 2, color);
    }

    public static void drawGradientRect(DrawContext ctx, float x, float y, float width, float height,
                                        int colorTop, int colorBottom) {
        drawQuadGradient(ctx, x, y, width, height, colorTop, colorTop, colorBottom, colorBottom);
    }

    public static void drawHorizontalGradient(DrawContext ctx, float x, float y, float width, float height,
                                              int colorLeft, int colorRight) {
        drawQuadGradient(ctx, x, y, width, height, colorLeft, colorRight, colorRight, colorLeft);
    }

    public static void drawQuadGradient(DrawContext ctx, float x, float y, float width, float height,
                                        int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (width <= 0 || height <= 0) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexColor(buffer, matrix, x, y, topLeft);
        vertexColor(buffer, matrix, x, y + height, bottomLeft);
        vertexColor(buffer, matrix, x + width, y + height, bottomRight);
        vertexColor(buffer, matrix, x + width, y, topRight);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    public static void drawCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
        drawEllipse(ctx, cx, cy, radius, radius, color);
    }

    public static void drawCrispCircle(DrawContext ctx, float cx, float cy, float radius, int color) {
        drawCircleInternal(ctx, cx, cy, radius, 0f, color, 0.24f);
    }

    public static void drawCrispCircleOutline(DrawContext ctx, float cx, float cy, float radius,
                                              float lineWidth, int color) {
        drawCircleInternal(ctx, cx, cy, radius, lineWidth, color, 0.24f);
    }

    private static void drawCircleInternal(DrawContext ctx, float cx, float cy, float radius,
                                           float lineWidth, int color, float smoothing) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        if (a <= 0.003f) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        if (ShaderManager.isAvailable() && !hasComplex2DTransform(matrix)) {
            beginShaderDraw();
            drawCircleShader(ctx, cx, cy, radius, radius, lineWidth, smoothing, r, g, b, a);
            endShaderDraw();
        } else if (lineWidth > 0f) {
            drawCircleOutlineCPU(ctx, cx, cy, radius, lineWidth, r, g, b, a);
        } else {
            drawEllipseCPU(ctx, cx, cy, radius, radius, r, g, b, a);
        }
    }

    public static void drawEllipse(DrawContext ctx, float cx, float cy, float rx, float ry, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (ShaderManager.isAvailable() && !transformed) {
            beginShaderDraw();
            drawCircleShader(ctx, cx, cy, rx, ry, 0, EDGE_SMOOTHING, r, g, b, a);
            endShaderDraw();
        } else {
            drawEllipseCPU(ctx, cx, cy, rx, ry, r, g, b, a);
        }
    }

    public static void drawCircleOutline(DrawContext ctx, float cx, float cy, float radius,
                                         float lineWidth, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (ShaderManager.isAvailable() && !transformed) {
            beginShaderDraw();
            drawCircleShader(ctx, cx, cy, radius, radius, lineWidth, EDGE_SMOOTHING, r, g, b, a);
            endShaderDraw();
        } else {
            drawCircleOutlineCPU(ctx, cx, cy, radius, lineWidth, r, g, b, a);
        }
    }

    public static void drawSemiCircle(DrawContext ctx, float cx, float cy, float radius,
                                      float startAngle, float endAngle, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;
        drawSemiCircleCPU(ctx, cx, cy, radius, startAngle, endAngle, r, g, b, a);
    }

    /** Draws one filled annular sector. Angles are degrees in screen space. */
    public static void drawRingSector(DrawContext ctx, float cx, float cy,
                                      float innerRadius, float outerRadius,
                                      float startAngle, float endAngle, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        if (a <= 0.003f || outerRadius <= 0f || outerRadius <= innerRadius || endAngle <= startAngle) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        int segments = Math.max(8, (int) Math.ceil((endAngle - startAngle) * outerRadius / 90f));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startAngle + (endAngle - startAngle) * i / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buffer.vertex(matrix, cx + cos * innerRadius, cy + sin * innerRadius, 0).color(r, g, b, a);
            buffer.vertex(matrix, cx + cos * outerRadius, cy + sin * outerRadius, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawLine(DrawContext ctx, float x1, float y1, float x2, float y2,
                                float width, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float nx = -dy / len * width / 2f;
        float ny = dx / len * width / 2f;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x1 + nx, y1 + ny, 0).color(r, g, b, a);
        buffer.vertex(matrix, x1 - nx, y1 - ny, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2 - nx, y2 - ny, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2 + nx, y2 + ny, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    public static void drawGradientLine(DrawContext ctx, float x1, float y1, float x2, float y2,
                                        float width, int colorStart, int colorEnd) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float nx = -dy / len * width / 2f;
        float ny = dx / len * width / 2f;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        vertexColor(buffer, matrix, x1 + nx, y1 + ny, colorStart);
        vertexColor(buffer, matrix, x1 - nx, y1 - ny, colorStart);
        vertexColor(buffer, matrix, x2 - nx, y2 - ny, colorEnd);
        vertexColor(buffer, matrix, x2 + nx, y2 + ny, colorEnd);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    public static void drawRoundedLine(DrawContext ctx, float x1, float y1, float x2, float y2,
                                       float width, int color) {
        drawLine(ctx, x1, y1, x2, y2, width, color);
        drawCircle(ctx, x1, y1, width / 2f, color);
        drawCircle(ctx, x2, y2, width / 2f, color);
    }

    public static void drawShadow(DrawContext ctx, float x, float y, float width, float height,
                                  float radius, float shadowSize, int shadowColor) {
        float a = ((shadowColor >> 24) & 0xFF) / 255f;
        float r = ((shadowColor >> 16) & 0xFF) / 255f;
        float g = ((shadowColor >> 8) & 0xFF) / 255f;
        float b = (shadowColor & 0xFF) / 255f;

        if (a <= 0.003f) return;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (ShaderManager.isAvailable() && !transformed) {
            beginShaderDraw();
            drawShadowShader(ctx, x, y, width, height, radius, shadowSize, r, g, b, a);
            endShaderDraw();
        } else {
            drawShadowCPU(ctx, x, y, width, height, radius, shadowSize, r, g, b, a);
        }
    }

    public static void drawShadowOffset(DrawContext ctx, float x, float y, float width, float height,
                                        float radius, float shadowSize, float offsetX, float offsetY,
                                        int shadowColor) {
        drawShadow(ctx, x + offsetX, y + offsetY, width, height, radius, shadowSize, shadowColor);
    }

    public static void drawColorPickerSquare(DrawContext ctx, float x, float y, float width, float height,
                                             float radius, int hueColor, float opacity) {
        if (width <= 0f || height <= 0f || opacity <= 0.003f) return;
        if (!ShaderManager.isAvailable()) {
            drawRoundedGradientH(ctx, x, y, width, height, radius,
                    withAlpha(0xFFFFFF, opacity), withAlpha(hueColor, opacity));
            drawRoundedGradientV(ctx, x, y, width, height, radius,
                    withAlpha(0x000000, 0f), withAlpha(0x000000, opacity));
            return;
        }
        beginShaderDraw();
        ShaderRect rect = toShaderRect(ctx, x, y, width, height);
        float pxScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
        String name = ShaderManager.COLOR_PICKER;
        ShaderManager.useProgram(name);
        ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform4f(name, "uHueColor",
                ((hueColor >>> 16) & 0xFF) / 255f,
                ((hueColor >>> 8) & 0xFF) / 255f,
                (hueColor & 0xFF) / 255f, 1f);
        ShaderManager.setUniform1f(name, "uRadius", radius * pxScale);
        ShaderManager.setUniform1f(name, "uOpacity", Math.max(0f, Math.min(1f, opacity)));
        ShaderManager.setUniform1f(name, "uSmoothing", EDGE_SMOOTHING * pxScale);
        ShaderManager.drawQuad(name, ctx, x, y, width, height, EDGE_SMOOTHING + 1f);
        ShaderManager.stopProgram();
        endShaderDraw();
    }

    public static void drawRoundedCheckerboard(DrawContext ctx, float x, float y, float width, float height,
                                               float radius, float cellSize, float opacity) {
        if (width <= 0f || height <= 0f || opacity <= 0.003f) return;
        if (!ShaderManager.isAvailable()) {
            drawRoundedRect(ctx, x, y, width, height, radius, withAlpha(0x807B8B, opacity));
            return;
        }
        beginShaderDraw();
        ShaderRect rect = toShaderRect(ctx, x, y, width, height);
        float pxScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
        String shader = ShaderManager.ROUNDED_CHECKERBOARD;
        ShaderManager.useProgram(shader);
        applySquircleShape(shader);
        ShaderManager.setUniform4f(shader, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform1f(shader, "uRadius", radius * pxScale);
        ShaderManager.setUniform1f(shader, "uCellSize", cellSize * pxScale);
        ShaderManager.setUniform1f(shader, "uOpacity", Math.max(0f, Math.min(1f, opacity)));
        ShaderManager.setUniform1f(shader, "uSmoothing", EDGE_SMOOTHING * pxScale);
        ShaderManager.drawQuad(shader, ctx, x, y, width, height, EDGE_SMOOTHING + 1f);
        ShaderManager.stopProgram();
        endShaderDraw();
    }

    public static void drawTexture(DrawContext ctx, Identifier texture, float x, float y,
                                   float width, float height) {
        drawTexture(ctx, texture, x, y, width, height, 1.0f);
    }

    public static void drawTexture(DrawContext ctx, Identifier texture, float x, float y,
                                   float width, float height, float alpha) {
        if (alpha <= 0.003f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        ctx.drawTexture(
                RenderLayer::getGuiTextured,
                texture,
                (int) x, (int) y,
                0, 0,
                (int) width, (int) height,
                (int) width, (int) height
        );

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static void drawTextureTinted(DrawContext ctx, Identifier texture, float x, float y,
                                         float width, float height, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (a <= 0.003f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);

        ctx.drawTexture(
                RenderLayer::getGuiTextured,
                texture,
                (int) x, (int) y,
                0, 0,
                (int) width, (int) height,
                (int) width, (int) height
        );

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static void drawTextureUV(DrawContext ctx, Identifier texture, float x, float y,
                                     float width, float height,
                                     float u, float v, float uWidth, float vHeight,
                                     int texWidth, int texHeight, float alpha) {
        if (alpha <= 0.003f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        float u0 = u / texWidth;
        float u1 = (u + uWidth) / texWidth;
        float v0 = v / texHeight;
        float v1 = (v + vHeight) / texHeight;
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x, y, 0f).texture(u0, v0).color(1f, 1f, 1f, alpha);
        buffer.vertex(matrix, x, y + height, 0f).texture(u0, v1).color(1f, 1f, 1f, alpha);
        buffer.vertex(matrix, x + width, y + height, 0f).texture(u1, v1).color(1f, 1f, 1f, alpha);
        buffer.vertex(matrix, x + width, y, 0f).texture(u1, v0).color(1f, 1f, 1f, alpha);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static void enableScissor(float x, float y, float width, float height) {
        StencilUtil.enableScissor(x, y, width, height);
    }

    public static void disableScissor() {
        StencilUtil.disableScissor();
    }

    public static void drawMarqueeText(DrawContext ctx, String text, float x, float y,
                                       float maxWidth, float clipHeight, float size,
                                       int color, FontWeight weight, float pixelsPerSecond) {
        if (text == null || text.isBlank() || maxWidth <= 1f) return;

        float textW = CustomFontRenderer.getTextWidth(text, size, weight);
        if (textW <= maxWidth) {
            CustomFontRenderer.drawText(ctx, text, x, y, size, color, weight);
            return;
        }

        float speed = Math.max(10f, pixelsPerSecond);
        float gap = Math.max(18f, maxWidth * 0.35f);
        float cycleW = textW + gap;
        long cycleMs = Math.max(1L, (long) ((cycleW / speed) * 1000.0f));
        float offset = (System.currentTimeMillis() % cycleMs) / (float) cycleMs * cycleW;

        float fadeW = Math.min(16f, Math.max(8f, maxWidth * 0.32f));
        CustomFontRenderer.drawTextWithFade(ctx, text, x - offset, y, size, color, weight,
                x, maxWidth, fadeW, fadeW);
        CustomFontRenderer.drawTextWithFade(ctx, text, x - offset + cycleW, y, size, color, weight,
                x, maxWidth, fadeW, fadeW);
    }

    public static void drawTextWithEdgeFade(DrawContext ctx, String text, float drawX, float y,
                                            float clipX, float clipWidth, float fadeWidth,
                                            float size, int color, FontWeight weight) {
        if (text == null || text.isEmpty()) return;

        float cursor = drawX;
        float baseAlpha = getAlpha(color);
        for (int i = 0; i < text.length(); i++) {
            String chr = String.valueOf(text.charAt(i));
            float charW = CustomFontRenderer.getTextWidth(chr, size, weight);
            float center = cursor + charW * 0.5f;
            float leftFade = (center - clipX) / Math.max(1f, fadeWidth);
            float rightFade = (clipX + clipWidth - center) / Math.max(1f, fadeWidth);
            float edge = Math.max(0f, Math.min(1f, Math.min(leftFade, rightFade)));
            edge = edge * edge * (3f - 2f * edge);

            if (edge > 0.01f && !chr.isBlank()) {
                CustomFontRenderer.drawText(ctx, chr, cursor, y, size, withAlpha(color, baseAlpha * edge), weight);
            }
            cursor += charW;
        }
    }

    public static void drawAnimatedInputText(DrawContext ctx, String text, float x, float y, float size,
                                             int color, FontWeight weight, int typedIndex, long typedStartMs) {
        if (text == null || text.isEmpty()) return;

        if (typedIndex < 0 || typedIndex >= text.length()) {
            CustomFontRenderer.drawText(ctx, text, x, y, size, color, weight);
            return;
        }

        String prefix = typedIndex > 0 ? text.substring(0, typedIndex) : "";
        String symbol = text.substring(typedIndex, typedIndex + 1);
        String suffix = typedIndex + 1 < text.length() ? text.substring(typedIndex + 1) : "";

        float prefixW = prefix.isEmpty() ? 0f : CustomFontRenderer.getTextWidth(prefix, size, weight);
        float symbolW = CustomFontRenderer.getTextWidth(symbol, size, weight);
        float symbolX = x + prefixW;

        if (!prefix.isEmpty()) {
            CustomFontRenderer.drawText(ctx, prefix, x, y, size, color, weight);
        }

        float progress = typedStartMs <= 0L ? 1f
                : Math.max(0f, Math.min(1f, (System.currentTimeMillis() - typedStartMs) / 220f));
        float eased = easeOutBack(progress);
        float scale = 0.1f + 0.9f * eased;
        int symbolColor = withAlpha(color, getAlpha(color) * (0.7f + 0.3f * Math.min(1f, eased)));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(symbolX + symbolW * 0.5f, y + size * 0.5f, 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-50f * (1f - Math.min(1f, eased))));
        ctx.getMatrices().scale(scale, scale, 1f);
        CustomFontRenderer.drawText(ctx, symbol, -symbolW * 0.5f, -size * 0.5f, size, symbolColor, weight);
        ctx.getMatrices().pop();

        if (!suffix.isEmpty()) {
            CustomFontRenderer.drawText(ctx, suffix, symbolX + symbolW, y, size, color, weight);
        }
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgbaF(float r, float g, float b, float a) {
        return rgba((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static int withAlphaInt(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    public static float getAlpha(int color) {
        return ((color >> 24) & 0xFF) / 255f;
    }

    public static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aF = (from >> 24) & 0xFF, rF = (from >> 16) & 0xFF, gF = (from >> 8) & 0xFF, bF = from & 0xFF;
        int aT = (to >> 24) & 0xFF, rT = (to >> 16) & 0xFF, gT = (to >> 8) & 0xFF, bT = to & 0xFF;
        return ((int) (aF + (aT - aF) * t) << 24) | ((int) (rF + (rT - rF) * t) << 16)
                | ((int) (gF + (gT - gF) * t) << 8) | (int) (bF + (bT - bF) * t);
    }

    public static int darken(int color, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int a = (color >> 24) & 0xFF;
        return (a << 24) | ((int) (((color >> 16) & 0xFF) * factor) << 16)
                | ((int) (((color >> 8) & 0xFF) * factor) << 8) | (int) ((color & 0xFF) * factor);
    }

    public static int lighten(int color, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int a = (color >> 24) & 0xFF;
        return (a << 24)
                | (Math.min(255, (int) (((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * factor)) << 16)
                | (Math.min(255, (int) (((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * factor)) << 8)
                | Math.min(255, (int) ((color & 0xFF) + (255 - (color & 0xFF)) * factor));
    }

    public static int invert(int color) {
        return ((color >> 24) & 0xFF) << 24 | (255 - ((color >> 16) & 0xFF)) << 16
                | (255 - ((color >> 8) & 0xFF)) << 8 | (255 - (color & 0xFF));
    }

    public static int hsv(float h, float s, float v, float a) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v);
        return ((int) (a * 255) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int rainbow(long time, float speed, float saturation, float brightness, int alpha) {
        float hue = (time % (long) (1000 / speed)) / (1000f / speed);
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int rainbowWave(long time, float speed, float offset, float saturation, float brightness, int alpha) {
        float hue = ((time % (long) (1000 / speed)) / (1000f / speed) + offset) % 1.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * Math.max(0f, Math.min(1f, t));
    }

    public static double lerp(double from, double to, double t) {
        return from + (to - from) * Math.max(0.0, Math.min(1.0, t));
    }

    public static float animate(float current, float target, float speed, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) return target;
        return current + diff * Math.min(1f, speed * delta);
    }

    public static float animate(float current, float target, float speed) {
        return animate(current, target, speed, getFrameDelta());
    }

    public static void beginFrame() {
        long now = System.nanoTime();
        long diffNs = now - lastFrameTimeNs;
        if (diffNs <= 0L) return;
        lastFrameTimeNs = now;

        float delta = diffNs / 16_666_667.0f;
        if (delta > 60.0f) {
            delta = 1.0f;
        }
        cachedFrameDelta = Math.max(0.01f, Math.min(6.0f, delta));
    }

    public static float getFrameDelta() {
        return cachedFrameDelta;
    }

    public static float easeInQuad(float t) { return t * t; }
    public static float easeOutQuad(float t) { return 1f - (1f - t) * (1f - t); }
    public static float easeInOutQuad(float t) { return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2 * t + 2, 2) / 2f; }
    public static float easeInCubic(float t) { return t * t * t; }
    public static float easeOutCubic(float t) { return 1f - (float) Math.pow(1 - t, 3); }
    public static float easeInOutCubic(float t) { return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2 * t + 2, 3) / 2f; }
    public static float easeInQuart(float t) { return t * t * t * t; }
    public static float easeOutQuart(float t) { return 1f - (float) Math.pow(1 - t, 4); }
    public static float easeInQuint(float t) { return t * t * t * t * t; }
    public static float easeOutQuint(float t) { return 1f - (float) Math.pow(1 - t, 5); }
    public static float easeOutExpo(float t) { return t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t); }
    public static float easeInExpo(float t) { return t <= 0f ? 0f : (float) Math.pow(2, 10 * t - 10); }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    public static float easeInBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        return c3 * t * t * t - c1 * t * t;
    }

    public static float easeInOutBack(float t) {
        float c1 = 1.70158f, c2 = c1 * 1.525f;
        return t < 0.5f
                ? (float) (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2
                : (float) (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (t * 2 - 2) + c2) + 2) / 2;
    }

    public static float easeOutElastic(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1);
    }

    public static float easeOutBounce(float t) {
        if (t < 1f / 2.75f) return 7.5625f * t * t;
        else if (t < 2f / 2.75f) { t -= 1.5f / 2.75f; return 7.5625f * t * t + 0.75f; }
        else if (t < 2.5f / 2.75f) { t -= 2.25f / 2.75f; return 7.5625f * t * t + 0.9375f; }
        else { t -= 2.625f / 2.75f; return 7.5625f * t * t + 0.984375f; }
    }

    private static void beginShaderDraw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void endShaderDraw() {
        RenderSystem.disableBlend();
    }

    private static float scale() {
        return (float) mc.getWindow().getScaleFactor();
    }

    private static float clampRadius(float r, float max) {
        return Math.max(0, Math.min(r, max));
    }

    private static boolean hasComplex2DTransform(Matrix4f m) {
        final float eps = 0.0001f;
        // m01, m10 - это 2D поворот
        // m02, m12, m20, m21 - это 3D наклон (по осям X и Y)
        return Math.abs(m.m01()) > eps || Math.abs(m.m10()) > eps ||
                Math.abs(m.m02()) > eps || Math.abs(m.m12()) > eps ||
                Math.abs(m.m20()) > eps || Math.abs(m.m21()) > eps;
    }

    private static ShaderRect toShaderRect(DrawContext ctx, float x, float y, float w, float h) {
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        float guiScale = scale();

        Vector4f p1 = m.transform(new Vector4f(x, y, 0f, 1f));
        Vector4f p2 = m.transform(new Vector4f(x + w, y, 0f, 1f));
        Vector4f p3 = m.transform(new Vector4f(x + w, y + h, 0f, 1f));
        Vector4f p4 = m.transform(new Vector4f(x, y + h, 0f, 1f));

        float minX = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        float minY = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        float maxX = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
        float maxY = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));

        float edgeXLen = (float) Math.hypot(p2.x - p1.x, p2.y - p1.y);
        float edgeYLen = (float) Math.hypot(p4.x - p1.x, p4.y - p1.y);
        float scaleX = edgeXLen / Math.max(0.0001f, Math.abs(w));
        float scaleY = edgeYLen / Math.max(0.0001f, Math.abs(h));

        return new ShaderRect(
                minX * guiScale,
                minY * guiScale,
                (maxX - minX) * guiScale,
                (maxY - minY) * guiScale,
                scaleX * guiScale,
                scaleY * guiScale
        );
    }

    private static ShaderEllipse toShaderEllipse(DrawContext ctx, float cx, float cy, float rx, float ry) {
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        float guiScale = scale();

        Vector4f center = m.transform(new Vector4f(cx, cy, 0f, 1f));
        float scaleX = Math.abs(m.m00());
        float scaleY = Math.abs(m.m11());
        float pxScale = ((scaleX + scaleY) * 0.5f) * guiScale;

        return new ShaderEllipse(
                center.x * guiScale,
                center.y * guiScale,
                rx * scaleX * guiScale,
                ry * scaleY * guiScale,
                Math.max(pxScale, 0.0001f)
        );
    }

    private static void drawRoundedRectShader(DrawContext ctx, float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              float r, float g, float b, float a) {
        ShaderRect rect = toShaderRect(ctx, x, y, w, h);
        float radiusScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
        String name = ShaderManager.ROUNDED_RECT;

        ShaderManager.useProgram(name);
        applySquircleShape(name);
        ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform4f(name, "uRadii", tl * radiusScale, tr * radiusScale, br * radiusScale, bl * radiusScale);
        ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
        ShaderManager.setUniform1f(name, "uSmoothing", EDGE_SMOOTHING * radiusScale);
        ShaderManager.drawQuad(name, ctx, x, y, w, h, EDGE_SMOOTHING + 1f);
        ShaderManager.stopProgram();
    }

    private static void drawRoundedOutlineShader(DrawContext ctx, float x, float y, float w, float h,
                                                 float tl, float tr, float br, float bl,
                                                 float lineWidth,
                                                 float r, float g, float b, float a) {
        ShaderRect rect = toShaderRect(ctx, x, y, w, h);
        float radiusScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
        String name = ShaderManager.ROUNDED_RECT_OUTLINE;

        ShaderManager.useProgram(name);
        applySquircleShape(name);
        ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform4f(name, "uRadii", tl * radiusScale, tr * radiusScale, br * radiusScale, bl * radiusScale);
        ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
        ShaderManager.setUniform1f(name, "uSmoothing", EDGE_SMOOTHING * radiusScale);
        ShaderManager.setUniform1f(name, "uLineWidth", lineWidth * radiusScale);
        ShaderManager.drawQuad(name, ctx, x, y, w, h, lineWidth + EDGE_SMOOTHING + 1f);
        ShaderManager.stopProgram();
    }

    private static void drawRoundedGradientShader(DrawContext ctx, float x, float y, float w, float h,
                                                  float tl, float tr, float br, float bl,
                                                  int colorTL, int colorTR, int colorBR, int colorBL) {
        ShaderRect rect = toShaderRect(ctx, x, y, w, h);
        float radiusScale = Math.max(Math.min(rect.scaleXPx, rect.scaleYPx), 0.0001f);
        String name = ShaderManager.ROUNDED_RECT_GRADIENT;

        ShaderManager.useProgram(name);
        applySquircleShape(name);
        ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform4f(name, "uRadii", tl * radiusScale, tr * radiusScale, br * radiusScale, bl * radiusScale);
        ShaderManager.setUniform1f(name, "uSmoothing", EDGE_SMOOTHING * radiusScale);
        setColorUniform(name, "uColor1", colorTL);
        setColorUniform(name, "uColor2", colorTR);
        setColorUniform(name, "uColor3", colorBR);
        setColorUniform(name, "uColor4", colorBL);
        ShaderManager.drawQuad(name, ctx, x, y, w, h, EDGE_SMOOTHING + 1f);
        ShaderManager.stopProgram();
    }

    private static void drawCircleShader(DrawContext ctx, float cx, float cy, float rx, float ry,
                                         float lineWidth, float smoothing,
                                         float r, float g, float b, float a) {
        ShaderEllipse ellipse = toShaderEllipse(ctx, cx, cy, rx, ry);
        String name = ShaderManager.CIRCLE;

        ShaderManager.useProgram(name);
        ShaderManager.setUniform2f(name, "uCenter", ellipse.cxPx, ellipse.cyPx);
        ShaderManager.setUniform2f(name, "uRadius", ellipse.rxPx, ellipse.ryPx);
        ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
        ShaderManager.setUniform1f(name, "uSmoothing", smoothing * ellipse.scalePx);
        ShaderManager.setUniform1f(name, "uLineWidth", lineWidth * ellipse.scalePx);
        ShaderManager.setUniform1f(name, "uProgress", 1.0f);
        ShaderManager.setUniform1f(name, "uArcMode", 0.0f);

        float maxR = Math.max(rx, ry);
        float extra = lineWidth + EDGE_SMOOTHING + 2f;
        ShaderManager.drawQuad(name, ctx, cx - maxR, cy - maxR, maxR * 2, maxR * 2, extra);
        ShaderManager.stopProgram();
    }

    public static void drawArc(DrawContext ctx, float cx, float cy, float radius, float lineWidth, int color, float progress) {
        drawArcInternal(ctx, cx, cy, radius, lineWidth, color, progress, EDGE_SMOOTHING);
    }

    public static void drawCrispArc(DrawContext ctx, float cx, float cy, float radius,
                                    float lineWidth, int color, float progress) {
        drawArcInternal(ctx, cx, cy, radius, lineWidth, color, progress, 0.24f);
    }

    public static void drawCrispArcSegment(DrawContext ctx, float cx, float cy, float radius,
                                           float lineWidth, float startDegrees,
                                           float sweepDegrees, int color) {
        float a = ((color >>> 24) & 0xFF) / 255f;
        if (a <= 0.003f || radius <= 0f || lineWidth <= 0f || sweepDegrees <= 0.05f) return;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float clampedSweep = Math.min(359.9f, sweepDegrees);
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();

        if (ShaderManager.isAvailable() && !hasComplex2DTransform(matrix)) {
            beginShaderDraw();
            String name = ShaderManager.CIRCLE;
            ShaderEllipse ellipse = toShaderEllipse(ctx, cx, cy, radius, radius);
            ShaderManager.useProgram(name);
            ShaderManager.setUniform2f(name, "uCenter", ellipse.cxPx, ellipse.cyPx);
            ShaderManager.setUniform2f(name, "uRadius", ellipse.rxPx, ellipse.ryPx);
            ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
            ShaderManager.setUniform1f(name, "uSmoothing", 0.24f * ellipse.scalePx);
            ShaderManager.setUniform1f(name, "uLineWidth", lineWidth * ellipse.scalePx);
            ShaderManager.setUniform1f(name, "uProgress", 1.0f);
            ShaderManager.setUniform1f(name, "uArcMode", 1.0f);
            ShaderManager.setUniform1f(name, "uArcStart", (float) Math.toRadians(startDegrees));
            ShaderManager.setUniform1f(name, "uArcSweep", (float) Math.toRadians(clampedSweep));
            float extra = lineWidth + EDGE_SMOOTHING + 2f;
            ShaderManager.drawQuad(name, ctx, cx - radius, cy - radius,
                    radius * 2f, radius * 2f, extra);
            ShaderManager.stopProgram();
            endShaderDraw();
        } else {
            drawRingSector(ctx, cx, cy, radius - lineWidth * 0.5f,
                    radius + lineWidth * 0.5f, startDegrees,
                    startDegrees + clampedSweep, color);
        }
    }

    private static void drawArcInternal(DrawContext ctx, float cx, float cy, float radius,
                                        float lineWidth, int color, float progress, float smoothing) {
        float a = ((color >> 24) & 0xFF) / 255f;
        if (a <= 0.003f) return;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        boolean transformed = hasComplex2DTransform(matrix);

        if (ShaderManager.isAvailable() && !transformed) {
            beginShaderDraw();
            String name = ShaderManager.CIRCLE;
            ShaderEllipse ellipse = toShaderEllipse(ctx, cx, cy, radius, radius);

            ShaderManager.useProgram(name);
            ShaderManager.setUniform2f(name, "uCenter", ellipse.cxPx, ellipse.cyPx);
            ShaderManager.setUniform2f(name, "uRadius", ellipse.rxPx, ellipse.ryPx);
            ShaderManager.setUniform4f(name, "uColor", r, g, b, a);
            ShaderManager.setUniform1f(name, "uSmoothing", smoothing * ellipse.scalePx);
            ShaderManager.setUniform1f(name, "uLineWidth", lineWidth * ellipse.scalePx);
            ShaderManager.setUniform1f(name, "uProgress", progress);
            ShaderManager.setUniform1f(name, "uArcMode", 0.0f);

            float extra = lineWidth + EDGE_SMOOTHING + 2f;
            ShaderManager.drawQuad(name, ctx, cx - radius, cy - radius, radius * 2, radius * 2, extra);
            ShaderManager.stopProgram();
            endShaderDraw();
        } else {
            drawCircleOutlineCPU(ctx, cx, cy, radius, lineWidth, r, g, b, a, progress);
        }
    }

    private static void drawShadowShader(DrawContext ctx, float x, float y, float w, float h,
                                         float radius, float shadowSize,
                                         float r, float g, float b, float a) {
        ShaderRect rect = toShaderRect(ctx, x, y, w, h);
        float pxScale = Math.max((rect.scaleXPx + rect.scaleYPx) * 0.5f, 0.0001f);
        String name = ShaderManager.SHADOW;

        ShaderManager.useProgram(name);
        applySquircleShape(name);
        ShaderManager.setUniform4f(name, "uRect", rect.xPx, rect.yPx, rect.wPx, rect.hPx);
        ShaderManager.setUniform4f(name, "uRadii", radius * pxScale, radius * pxScale, radius * pxScale, radius * pxScale);
        ShaderManager.setUniform4f(name, "uShadowColor", r, g, b, a);
        ShaderManager.setUniform1f(name, "uShadowSize", shadowSize * pxScale);
        ShaderManager.setUniform1f(name, "uSmoothing", EDGE_SMOOTHING * pxScale);
        ShaderManager.drawQuad(name, ctx, x, y, w, h, shadowSize + EDGE_SMOOTHING + 2f);
        ShaderManager.stopProgram();
    }

    private static final class ShaderRect {
        final float xPx;
        final float yPx;
        final float wPx;
        final float hPx;
        final float scaleXPx;
        final float scaleYPx;

        private ShaderRect(float xPx, float yPx, float wPx, float hPx, float scaleXPx, float scaleYPx) {
            this.xPx = xPx;
            this.yPx = yPx;
            this.wPx = wPx;
            this.hPx = hPx;
            this.scaleXPx = Math.abs(scaleXPx);
            this.scaleYPx = Math.abs(scaleYPx);
        }
    }

    private static final class ShaderEllipse {
        final float cxPx;
        final float cyPx;
        final float rxPx;
        final float ryPx;
        final float scalePx;

        private ShaderEllipse(float cxPx, float cyPx, float rxPx, float ryPx, float scalePx) {
            this.cxPx = cxPx;
            this.cyPx = cyPx;
            this.rxPx = Math.max(Math.abs(rxPx), 0.0001f);
            this.ryPx = Math.max(Math.abs(ryPx), 0.0001f);
            this.scalePx = Math.max(Math.abs(scalePx), 0.0001f);
        }
    }

    private static void setColorUniform(String shader, String uniform, int color) {
        ShaderManager.setUniform4f(shader, uniform,
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f);
    }

    public static void drawRoundedRectCPU(Matrix4f matrix, float x, float y, float w, float h,
                                           float tl, float tr, float br, float bl,
                                           float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, x + w / 2f, y + h / 2f, 0).color(r, g, b, a);

        int seg = 24;
        addCornerVertices(buffer, matrix, x + tl, y + tl, tl, 180, 270, seg, r, g, b, a);
        addCornerVertices(buffer, matrix, x + w - tr, y + tr, tr, 270, 360, seg, r, g, b, a);
        addCornerVertices(buffer, matrix, x + w - br, y + h - br, br, 0, 90, seg, r, g, b, a);
        addCornerVertices(buffer, matrix, x + bl, y + h - bl, bl, 90, 180, seg, r, g, b, a);

        float firstAngle = (float) Math.toRadians(180);
        buffer.vertex(matrix,
                x + tl + (float) Math.cos(firstAngle) * tl,
                y + tl + (float) Math.sin(firstAngle) * tl, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawRoundedRectCPUTransformed(Matrix4f matrix, float x, float y, float w, float h,
                                                      float tl, float tr, float br, float bl,
                                                      float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int seg = 14;

        List<float[]> points = new ArrayList<>(seg * 4 + 8);

        addPerimeterPoint(points, x + tl, y);
        addPerimeterPoint(points, x + w - tr, y);
        addArcPoints(points, x + w - tr, y + tr, tr, 270f, 360f, seg);
        addPerimeterPoint(points, x + w, y + h - br);
        addArcPoints(points, x + w - br, y + h - br, br, 0f, 90f, seg);
        addPerimeterPoint(points, x + bl, y + h);
        addArcPoints(points, x + bl, y + h - bl, bl, 90f, 180f, seg);
        addPerimeterPoint(points, x, y + tl);
        addArcPoints(points, x + tl, y + tl, tl, 180f, 270f, seg);

        // ФИКС: проверяем что points не пустой перед draw
        if (points.size() >= 2) {
            for (int i = 0; i < points.size() - 1; i++) {
                addFanTriangle(buffer, matrix, cx, cy, points.get(i), points.get(i + 1), r, g, b, a);
            }
            addFanTriangle(buffer, matrix, cx, cy,
                    points.get(points.size() - 1), points.get(0), r, g, b, a);

            BufferRenderer.drawWithGlobalProgram(buffer.end()); // ← только если есть данные
        }

        RenderSystem.disableBlend();
    }
    public static void drawRoundedTexture(Matrix4f matrix, Identifier texture,
                                          float x, float y, float w, float h,
                                          float radius, float alpha) {
        // Устанавливаем текстуру и шейдер
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int seg = 16; // Кол-во сегментов на угол

        List<float[]> points = new ArrayList<>();
        // Генерируем точки скругленного прямоугольника
        addArcPoints(points, x + radius, y + radius, radius, 180f, 270f, seg);
        addArcPoints(points, x + w - radius, y + radius, radius, 270f, 360f, seg);
        addArcPoints(points, x + w - radius, y + h - radius, radius, 0f, 90f, seg);
        addArcPoints(points, x + radius, y + h - radius, radius, 90f, 180f, seg);

        // Рисуем треугольники (Triangle Fan на основе Triangles)
        for (int i = 0; i < points.size(); i++) {
            float[] p1 = points.get(i);
            float[] p2 = points.get((i + 1) % points.size());

            // Центральная точка
            addVertexWithUV(buffer, matrix, cx, cy, x, y, w, h, alpha);
            // Две точки на периметре
            addVertexWithUV(buffer, matrix, p1[0], p1[1], x, y, w, h, alpha);
            addVertexWithUV(buffer, matrix, p2[0], p2[1], x, y, w, h, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void addVertexWithUV(BufferBuilder buffer, Matrix4f matrix,
                                        float px, float py, float minX, float minY,
                                        float w, float h, float alpha) {
        // Рассчитываем UV (от 0.0 до 1.0) в зависимости от положения точки в прямоугольнике
        float u = (px - minX) / w;
        float v = (py - minY) / h;

        // ВАЖНО: В 1.21.4 порядок такой: vertex -> texture -> color
        buffer.vertex(matrix, px, py, 0).texture(u, v).color(1f, 1f, 1f, alpha);
    }

    public static void drawRoundedTextureUV(Matrix4f matrix, Identifier texture,
                                            float x, float y, float w, float h,
                                            float radius, float alpha,
                                            float u, float v, float uW, float vH,
                                            float texWidth, float texHeight) {
        if (alpha <= 0.003f) return;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Выключаем куллинг, чтобы при наклоне текстура не исчезала, если "отвернется" от камеры
        RenderSystem.disableCull();

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int seg = 16;

        List<float[]> points = new ArrayList<>();
        addArcPoints(points, x + w - radius, y + radius, radius, 270f, 360f, seg);
        addArcPoints(points, x + w - radius, y + h - radius, radius, 0f, 90f, seg);
        addArcPoints(points, x + radius, y + h - radius, radius, 90f, 180f, seg);
        addArcPoints(points, x + radius, y + radius, radius, 180f, 270f, seg);

        if (points.size() >= 2) {
            for (int i = 0; i < points.size(); i++) {
                float[] p1 = points.get(i);
                float[] p2 = points.get((i + 1) % points.size());

                addVertexTex(buffer, matrix, cx, cy, x, y, w, h, u, v, uW, vH, texWidth, texHeight, alpha);
                addVertexTex(buffer, matrix, p1[0], p1[1], x, y, w, h, u, v, uW, vH, texWidth, texHeight, alpha);
                addVertexTex(buffer, matrix, p2[0], p2[1], x, y, w, h, u, v, uW, vH, texWidth, texHeight, alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawRoundedTextureShader(DrawContext ctx, Identifier texture,
                                                float x, float y, float w, float h,
                                                float radius, float alpha,
                                                float u, float v, float uW, float vH,
                                                float texWidth, float texHeight) {
        if (alpha <= 0.003f) return;

        if (!ShaderManager.isAvailable()) {
            drawRoundedTextureUVCPU(ctx.getMatrices().peek().getPositionMatrix(), texture,
                    x, y, w, h, radius, radius, radius, radius,
                    u, v, uW, vH, Math.round(texWidth), Math.round(texHeight), alpha);
            return;
        }

        bindTextureUnitZero(texture);
        String shader = ShaderManager.ROUNDED_TEXTURE;

        ShaderManager.useProgram(shader);
        applySquircleShape(shader);
        ShaderManager.setUniform2f(shader, "uSize", w, h);
        ShaderManager.setUniform4f(shader, "uRadii", radius, radius, radius, radius);
        ShaderManager.setUniform1f(shader, "uSmoothing", 0.543f); // Сглаживание
        ShaderManager.setUniform1f(shader, "uAlpha", alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float pad = 2f; // Экстра-пространство для красивых мягких краев
        ShaderManager.drawTexturedQuad(shader, ctx, x, y, w, h, pad, u, v, uW, vH, texWidth, texHeight);

        ShaderManager.stopProgram();
        RenderSystem.disableBlend();
    }

    /** Rounded textured quad with exact UV bounds, intended for atlas regions. */
    public static void drawRoundedTextureShaderExact(DrawContext ctx, Identifier texture,
                                                     float x, float y, float w, float h,
                                                     float radius, float alpha,
                                                     float u, float v, float uW, float vH,
                                                     float texWidth, float texHeight) {
        if (alpha <= 0.003f) return;
        if (!ShaderManager.isAvailable()) {
            drawRoundedTextureUVCPU(ctx.getMatrices().peek().getPositionMatrix(), texture,
                    x, y, w, h, radius, radius, radius, radius,
                    u, v, uW, vH, Math.round(texWidth), Math.round(texHeight), alpha);
            return;
        }

        bindTextureUnitZero(texture);
        String shader = ShaderManager.ROUNDED_TEXTURE;
        ShaderManager.useProgram(shader);
        applySquircleShape(shader);
        ShaderManager.setUniform2f(shader, "uSize", w, h);
        ShaderManager.setUniform4f(shader, "uRadii", radius, radius, radius, radius);
        ShaderManager.setUniform1f(shader, "uSmoothing", 0.62f);
        ShaderManager.setUniform1f(shader, "uAlpha", Math.max(0f, Math.min(1f, alpha)));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderManager.drawTexturedQuad(shader, ctx, x, y, w, h, 0f,
                u, v, uW, vH, texWidth, texHeight);
        ShaderManager.stopProgram();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static void drawMenuPreviewShader(DrawContext ctx, Identifier texture,
                                             float x, float y, float w, float h,
                                             float radius, float alpha, float shadeStrength,
                                             float u, float v, float uW, float vH,
                                             float texWidth, float texHeight) {
        if (alpha <= 0.003f) return;
        if (!ShaderManager.hasProgram(ShaderManager.MENU_PREVIEW)) {
            drawRoundedTextureUVCPU(ctx.getMatrices().peek().getPositionMatrix(), texture,
                    x, y, w, h, radius, radius, radius, radius,
                    u, v, uW, vH, Math.round(texWidth), Math.round(texHeight), alpha);
            return;
        }

        bindTextureUnitZero(texture);
        String shader = ShaderManager.MENU_PREVIEW;
        ShaderManager.useProgram(shader);
        applySquircleShape(shader);
        ShaderManager.setUniform2f(shader, "uSize", w, h);
        ShaderManager.setUniform4f(shader, "uRadii", radius, radius, radius, radius);
        ShaderManager.setUniform1f(shader, "uSmoothing", 0.62f);
        ShaderManager.setUniform1f(shader, "uAlpha", Math.max(0f, Math.min(1f, alpha)));
        ShaderManager.setUniform1f(shader, "uShadeStrength", Math.max(0f, Math.min(1f, shadeStrength)));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderManager.drawTexturedQuad(shader, ctx, x, y, w, h, 0f,
                u, v, uW, vH, texWidth, texHeight);
        ShaderManager.stopProgram();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void addVertexTex(BufferBuilder buffer, Matrix4f matrix,
                                     float px, float py, float minX, float minY, float w, float h,
                                     float u, float v, float uW, float vH, float tW, float tH, float alpha) {
        float percentX = (px - minX) / w;
        float percentY = (py - minY) / h;
        float finalU = (u + percentX * uW) / tW;
        float finalV = (v + percentY * vH) / tH;
        buffer.vertex(matrix, px, py, 0).texture(finalU, finalV).color(1f, 1f, 1f, alpha);
    }

    public static void drawRoundedTextureUVCPU(Matrix4f matrix, Identifier texture,
                                               float x, float y, float w, float h,
                                               float tl, float tr, float br, float bl,
                                               float u, float v, float uWidth, float vHeight,
                                               int texWidth, int texHeight, float alpha) {
        if (alpha <= 0.003f) return;

        float maxR = Math.min(w, h) / 2f;
        tl = clampRadius(tl, maxR);
        tr = clampRadius(tr, maxR);
        br = clampRadius(br, maxR);
        bl = clampRadius(bl, maxR);

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        float cx = x + w * 0.5f;
        float cy = y + h * 0.5f;
        int seg = 14;

        List<float[]> points = new ArrayList<>(seg * 4 + 8);
        addPerimeterPoint(points, x + tl, y);
        addPerimeterPoint(points, x + w - tr, y);
        addArcPoints(points, x + w - tr, y + tr, tr, 270f, 360f, seg);
        addPerimeterPoint(points, x + w, y + h - br);
        addArcPoints(points, x + w - br, y + h - br, br, 0f, 90f, seg);
        addPerimeterPoint(points, x + bl, y + h);
        addArcPoints(points, x + bl, y + h - bl, bl, 90f, 180f, seg);
        addPerimeterPoint(points, x, y + tl);
        addArcPoints(points, x + tl, y + tl, tl, 180f, 270f, seg);

        if (points.size() >= 2) {
            for (int i = 0; i < points.size() - 1; i++) {
                addFanTriangleUV(buffer, matrix, x, y, w, h, cx, cy, points.get(i), points.get(i + 1),
                        u, v, uWidth, vHeight, texWidth, texHeight, alpha);
            }
            addFanTriangleUV(buffer, matrix, x, y, w, h, cx, cy, points.get(points.size() - 1), points.get(0),
                    u, v, uWidth, vHeight, texWidth, texHeight, alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.disableBlend();
    }

    private static void addFanTriangleUV(BufferBuilder buffer, Matrix4f matrix,
                                         float x, float y, float w, float h,
                                         float cx, float cy, float[] p1, float[] p2,
                                         float u, float v, float uW, float vH, int tW, int tH, float alpha) {
        addVertexUV(buffer, matrix, x, y, w, h, cx, cy, u, v, uW, vH, tW, tH, alpha);
        addVertexUV(buffer, matrix, x, y, w, h, p1[0], p1[1], u, v, uW, vH, tW, tH, alpha);
        addVertexUV(buffer, matrix, x, y, w, h, p2[0], p2[1], u, v, uW, vH, tW, tH, alpha);
    }

    private static void addVertexUV(BufferBuilder buffer, Matrix4f matrix,
                                    float x, float y, float w, float h, float px, float py,
                                    float u, float v, float uW, float vH, int tW, int tH, float alpha) {
        // Вычисляем проценты по X и Y от общего прямоугольника
        float nx = (px - x) / w;
        float ny = (py - y) / h;
        // Натягиваем UV
        float finalU = (u + nx * uW) / tW;
        float finalV = (v + ny * vH) / tH;
        buffer.vertex(matrix, px, py, 0).texture(finalU, finalV).color(1f, 1f, 1f, alpha);
    }

    private static void addFanTriangle(BufferBuilder buffer, Matrix4f matrix,
                                       float cx, float cy, float[] p1, float[] p2,
                                       float r, float g, float b, float a) {
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a);
        buffer.vertex(matrix, p1[0], p1[1], 0).color(r, g, b, a);
        buffer.vertex(matrix, p2[0], p2[1], 0).color(r, g, b, a);
    }

    private static void addPerimeterPoint(List<float[]> points, float x, float y) {
        if (points.isEmpty()) {
            points.add(new float[]{x, y});
            return;
        }
        float[] last = points.get(points.size() - 1);
        if (Math.abs(last[0] - x) > 0.0005f || Math.abs(last[1] - y) > 0.0005f) {
            points.add(new float[]{x, y});
        }
    }

    private static void addArcPoints(List<float[]> points, float cx, float cy, float radius,
                                     float startDeg, float endDeg, int segments) {
        if (radius <= 0.001f) {
            float x = cx + (float) Math.cos(Math.toRadians(endDeg)) * radius;
            float y = cy + (float) Math.sin(Math.toRadians(endDeg)) * radius;
            addPerimeterPoint(points, x, y);
            return;
        }

        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float a = (float) Math.toRadians(startDeg + (endDeg - startDeg) * t);
            float x = cx + squircleComponent((float) Math.cos(a)) * radius;
            float y = cy + squircleComponent((float) Math.sin(a)) * radius;
            addPerimeterPoint(points, x, y);
        }
    }

    private static void drawRoundedOutlineCPU(Matrix4f matrix, float x, float y, float w, float h,
                                              float radius, float lineWidth,
                                              float r, float g, float b, float a) {
        int segments = 20;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        fillQuad(matrix, x + radius, y, x + w - radius, y + lineWidth, r, g, b, a);
        fillQuad(matrix, x + radius, y + h - lineWidth, x + w - radius, y + h, r, g, b, a);
        fillQuad(matrix, x, y + radius, x + lineWidth, y + h - radius, r, g, b, a);
        fillQuad(matrix, x + w - lineWidth, y + radius, x + w, y + h - radius, r, g, b, a);

        drawCornerArc(matrix, x + radius, y + radius, radius, lineWidth, 180, 270, segments, r, g, b, a);
        drawCornerArc(matrix, x + w - radius, y + radius, radius, lineWidth, 270, 360, segments, r, g, b, a);
        drawCornerArc(matrix, x + w - radius, y + h - radius, radius, lineWidth, 0, 90, segments, r, g, b, a);
        drawCornerArc(matrix, x + radius, y + h - radius, radius, lineWidth, 90, 180, segments, r, g, b, a);

        RenderSystem.disableBlend();
    }

    private static void drawEllipseCPU(DrawContext ctx, float cx, float cy, float rx, float ry,
                                       float r, float g, float b, float a) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        int segments = Math.max(48, (int) (Math.max(rx, ry) * 4f));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            buffer.vertex(matrix, cx + (float) Math.cos(angle) * rx,
                    cy + (float) Math.sin(angle) * ry, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawCircleOutlineCPU(DrawContext ctx, float cx, float cy, float radius,
                                             float lineWidth, float r, float g, float b, float a) {
        drawCircleOutlineCPU(ctx, cx, cy, radius, lineWidth, r, g, b, a, 1f);
    }

    private static void drawCircleOutlineCPU(DrawContext ctx, float cx, float cy, float radius,
                                             float lineWidth, float r, float g, float b, float a,
                                             float progress) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        progress = Math.max(0f, Math.min(1f, progress));
        if (progress <= 0.001f) return;
        int fullSegments = Math.max(48, (int) (radius * 4));
        int segments = Math.max(1, (int) Math.ceil(fullSegments * progress));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float inner = radius - lineWidth / 2f;
        float outer = radius + lineWidth / 2f;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (-Math.PI * 0.5
                    + i * Math.PI * 2.0 * progress / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buffer.vertex(matrix, cx + cos * inner, cy + sin * inner, 0).color(r, g, b, a);
            buffer.vertex(matrix, cx + cos * outer, cy + sin * outer, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawSemiCircleCPU(DrawContext ctx, float cx, float cy, float radius,
                                          float startAngle, float endAngle,
                                          float r, float g, float b, float a) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        int segments = Math.max(20, (int) (radius * 3f));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a);

        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float rad = (float) Math.toRadians(angle);
            buffer.vertex(matrix, cx + (float) Math.cos(rad) * radius,
                    cy + (float) Math.sin(rad) * radius, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawShadowCPU(DrawContext ctx, float x, float y, float w, float h,
                                      float radius, float shadowSize,
                                      float r, float g, float b, float a) {
        Matrix4f matrix = ctx.getMatrices().peek().getPositionMatrix();
        int layers = Math.max(1, (int) shadowSize);

        for (int i = layers; i >= 1; i--) {
            float progress = i / (float) layers;
            float layerAlpha = a * (1f - progress * progress) * 0.2f;
            if (layerAlpha <= 0.003f) continue;

            float expand = i;
            drawRoundedRectCPU(matrix,
                    x - expand, y - expand,
                    w + expand * 2, h + expand * 2,
                    radius + expand * 0.5f, radius + expand * 0.5f,
                    radius + expand * 0.5f, radius + expand * 0.5f,
                    r, g, b, layerAlpha);
        }
    }

    private static void addCornerVertices(BufferBuilder buffer, Matrix4f matrix,
                                          float cx, float cy, float radius,
                                          int startDeg, int endDeg, int segments,
                                          float r, float g, float b, float a) {
        if (radius <= 0.001f) {
            buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a);
            return;
        }

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startDeg + (endDeg - startDeg) * i / (float) segments);
            buffer.vertex(matrix, cx + squircleComponent((float) Math.cos(angle)) * radius,
                    cy + squircleComponent((float) Math.sin(angle)) * radius, 0).color(r, g, b, a);
        }
    }

    private static void drawCornerArc(Matrix4f matrix, float cx, float cy, float radius, float lineWidth,
                                      int startDeg, int endDeg, int segments,
                                      float r, float g, float b, float a) {
        if (radius <= 0.001f) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        float inner = Math.max(0, radius - lineWidth);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startDeg + (endDeg - startDeg) * i / (float) segments);
            float cos = squircleComponent((float) Math.cos(angle));
            float sin = squircleComponent((float) Math.sin(angle));
            buffer.vertex(matrix, cx + cos * inner, cy + sin * inner, 0).color(r, g, b, a);
            buffer.vertex(matrix, cx + cos * radius, cy + sin * radius, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void bindTextureUnitZero(Identifier texture) {
        RenderSystem.setShaderTexture(0, texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        mc.getTextureManager().getTexture(texture).bindTexture();
    }

    private static float squircleComponent(float value) {
        float exponent = 2f / activeSquircleExponent;
        return Math.copySign((float) Math.pow(Math.abs(value), exponent), value);
    }

    public static float squircleExponent() {
        return activeSquircleExponent;
    }

    public static float pushSquircleExponent(float exponent) {
        float previous = activeSquircleExponent;
        activeSquircleExponent = Math.max(2f, Math.min(6f, exponent));
        return previous;
    }

    public static void restoreSquircleExponent(float exponent) {
        activeSquircleExponent = Math.max(2f, Math.min(6f, exponent));
    }

    private static void fillQuad(Matrix4f matrix, float x1, float y1, float x2, float y2,
                                 float r, float g, float b, float a) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
        buffer.vertex(matrix, x1, y2, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a);
        buffer.vertex(matrix, x2, y1, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void vertexColor(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
    }
}
