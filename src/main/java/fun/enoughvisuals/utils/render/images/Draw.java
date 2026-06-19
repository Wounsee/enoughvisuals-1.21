package fun.enoughvisuals.utils.render.images;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.StencilUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;

public class Draw {

    private static final Set<Identifier> CONFIGURED = new HashSet<>();

    public static void image(DrawContext ctx, Identifier texture, float x, float y, float w, float h, int texW, int texH, float alpha) {
        imageInternal(ctx, texture, x, y, w, h, alpha);
    }

    public static void image(DrawContext ctx, Identifier texture, float x, float y, float w, float h, float alpha) {
        imageInternal(ctx, texture, x, y, w, h, alpha);
    }

    public static void imageTinted(DrawContext ctx, Identifier texture, float x, float y, float w, float h, int color, float alpha) {
        imageInternal(ctx, texture, x, y, w, h, color, alpha);
    }

    private static void imageInternal(DrawContext ctx, Identifier texture, float x, float y, float w, float h, float alpha) {
        imageInternal(ctx, texture, x, y, w, h, 0xFFFFFFFF, alpha);
    }

    private static void imageInternal(DrawContext ctx, Identifier texture, float x, float y, float w, float h, int color, float alpha) {
        if (alpha <= 0.005f) return;
        alpha = Math.min(1f, alpha);
        ensureTexture(texture);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture);
        float a = ((color >> 24) & 0xFF) / 255f * alpha;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(mat, x, y, 0).texture(0f, 0f);
        buffer.vertex(mat, x, y + h, 0).texture(0f, 1f);
        buffer.vertex(mat, x + w, y + h, 0).texture(1f, 1f);
        buffer.vertex(mat, x + w, y, 0).texture(1f, 0f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static void imageCentered(DrawContext ctx, Identifier texture, float cx, float cy, float size, float alpha) {
        image(ctx, texture, cx - size / 2f, cy - size / 2f, size, size, alpha);
    }

    public static void imageCenteredRevealUp(DrawContext ctx, Identifier texture, float cx, float cy, float size, float progress, float alpha) {
        if (alpha <= 0.005f || progress <= 0.005f) return;
        progress = Math.max(0f, Math.min(1f, progress));
        float half = size / 2f;
        float x = cx - half, y = cy - half;
        float visible = size * progress;
        float clipY = y + size - visible;

        StencilUtil.enableScissor(x, clipY, size, visible);
        image(ctx, texture, x, y, size, size, alpha);
        StencilUtil.disableScissor();
    }

    // НОВЫЙ МЕТОД: Отрисовка любой картинки с закруглениями по краям
    public static void imageRounded(DrawContext ctx, Identifier texture, float x, float y, float w, float h, float radius, float alpha) {
        if (alpha <= 0.005f) return;
        ctx.draw();

        StencilUtil.beginStencil();
        // Принудительно отключаем запись цвета (лечит белый фон)
        RenderSystem.colorMask(false, false, false, false);
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, radius, 0xFFFFFFFF);
        ctx.draw();

        StencilUtil.applyStencil();
        RenderSystem.colorMask(true, true, true, true);

        image(ctx, texture, x, y, w, h, alpha);

        ctx.draw();
        StencilUtil.endStencil();
    }

    // НОВЫЙ МЕТОД: Специально для головы игрока (берет кусок скина)
    public static void imagePlayerHeadRounded(DrawContext ctx, Identifier skin, float x, float y, float size, float radius, float alpha) {
        if (alpha <= 0.005f) return;
        float safeRadius = Math.min(radius, size * 0.5f);
        RenderUtil.drawRoundedTextureUVCPU(ctx.getMatrices().peek().getPositionMatrix(), skin,
                x, y, size, size, safeRadius, safeRadius, safeRadius, safeRadius,
                8f, 8f, 8f, 8f, 64, 64, alpha);
        RenderUtil.drawRoundedTextureUVCPU(ctx.getMatrices().peek().getPositionMatrix(), skin,
                x, y, size, size, safeRadius, safeRadius, safeRadius, safeRadius,
                40f, 8f, 8f, 8f, 64, 64, alpha);
    }

    private static void drawTextureUV(DrawContext ctx, Identifier texture, float x, float y, float w, float h,
                                       float u, float v, float uW, float vH, float tW, float tH, float alpha) {
        if (alpha <= 0.005f) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        float u0 = u / tW;
        float u1 = (u + uW) / tW;
        float v0 = v / tH;
        float v1 = (v + vH) / tH;

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(mat, x, y, 0).texture(u0, v0);
        buffer.vertex(mat, x, y + h, 0).texture(u0, v1);
        buffer.vertex(mat, x + w, y + h, 0).texture(u1, v1);
        buffer.vertex(mat, x + w, y, 0).texture(u1, v0);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void ensureTexture(Identifier id) {
        if (CONFIGURED.contains(id)) return;
        try {
            AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(id);
            if (tex != null) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlId());
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            }
        } catch (Exception ignored) {}
        CONFIGURED.add(id);
    }
}
