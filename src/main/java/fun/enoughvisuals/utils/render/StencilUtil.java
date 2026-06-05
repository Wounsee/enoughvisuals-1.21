package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.hud.HudElement;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;

public class StencilUtil {

    private static final Deque<float[]> scissorStack = new ArrayDeque<>();

    public static void enableScissor(float x, float y, float width, float height) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        float scale = (float) mc.getWindow().getScaleFactor();
        float hudScale = HudElement.activeScissorScale();
        x *= hudScale;
        y *= hudScale;
        width *= hudScale;
        height *= hudScale;

        int sx = (int) (x * scale);
        int sy = (int) (mc.getWindow().getFramebufferHeight() - (y + height) * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        if (!scissorStack.isEmpty()) {
            float[] parent = scissorStack.peek();
            int px = (int) parent[0], py = (int) parent[1];
            int pw = (int) parent[2], ph = (int) parent[3];

            int newX = Math.max(sx, px);
            int newY = Math.max(sy, py);
            int newW = Math.max(0, Math.min(sx + sw, px + pw) - newX);
            int newH = Math.max(0, Math.min(sy + sh, py + ph) - newY);

            sx = newX; sy = newY; sw = newW; sh = newH;
        }

        scissorStack.push(new float[]{sx, sy, sw, sh});

        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(sx, sy, sw, sh);
    }

    public static void disableScissor() {
        if (scissorStack.isEmpty()) {
            GlStateManager._disableScissorTest();
            return;
        }

        scissorStack.pop();

        if (scissorStack.isEmpty()) {
            GlStateManager._disableScissorTest();
        } else {
            float[] parent = scissorStack.peek();
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox((int) parent[0], (int) parent[1],
                    (int) parent[2], (int) parent[3]);
        }
    }

    public static void resetScissors() {
        scissorStack.clear();
        GlStateManager._disableScissorTest();
    }

    public static void resetRenderState() {
        resetScissors();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GlStateManager._colorMask(true, true, true, true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    

    public static void beginStencil() {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GlStateManager._colorMask(false, false, false, false);
    }

    public static void applyStencil() {
        GlStateManager._colorMask(true, true, true, true);
        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
    }

    public static void endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GlStateManager._colorMask(true, true, true, true);
    }
}
