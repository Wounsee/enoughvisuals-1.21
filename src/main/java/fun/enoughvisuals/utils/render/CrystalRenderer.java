package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class CrystalRenderer {

    private CrystalRenderer() {}

    public static void render(
            MatrixStack matrices,
            LivingEntity entity,
            float alpha,
            float partialTicks,
            int colorA,
            int colorB,
            float glowStrength,
            int shardCount
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return;

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();

        Vec3d targetPos = interpolatePos(entity, partialTicks);
        Vec3d cameraPos = camera.getPos();
        Vec3d renderPos = targetPos.subtract(cameraPos);

        float time         = (mc.player.age + partialTicks) * 3.5f;
        float entityHeight = entity.getHeight();
        float entityWidth  = entity.getWidth();
        float halfWidth    = entityWidth * 0.5f;

        int   tiers        = 3;
        int   perTier      = Math.max(1, shardCount);

                                                  
        float radiusBoost  = 0.40f;
        float crystalScale = 0.22f;

        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

                                                                     
                                                                  
                                                                     
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );
                                                                                         
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (int tier = 0; tier < tiers; tier++) {
            float tierY     = (entityHeight / (tiers + 1)) * (tier + 1);
            float tierSpeed = (1.0f + tier * 0.3f) * (tier % 2 == 0 ? -1f : 1f);

            for (int i = 0; i < perTier; i++) {
                float angleOffset = i * (360f / perTier);
                float angle       = time * tierSpeed + angleOffset;
                float radius      = halfWidth + radiusBoost;
                float x = radius * MathHelper.cos((float) Math.toRadians(angle));
                float z = radius * MathHelper.sin((float) Math.toRadians(angle));

                int   glowColor = mixColor(colorA, colorB, 0.5f);
                float glowAlpha = alpha * 0.22f * glowStrength;
                float glowSize  = crystalScale * alpha * 3.5f * (0.8f + glowStrength * 0.5f);

                drawProceduralGlow(
                        matrices, camera,
                        x, tierY, z,
                        glowSize, glowColor, glowAlpha
                );
            }
        }

                                                                     
                           
                                                                     
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int tier = 0; tier < tiers; tier++) {
            float tierY     = (entityHeight / (tiers + 1)) * (tier + 1);
            float tierSpeed = (1.0f + tier * 0.3f) * (tier % 2 == 0 ? -1f : 1f);

            for (int i = 0; i < perTier; i++) {
                float angleOffset = i * (360f / perTier);
                float angle       = time * tierSpeed + angleOffset;
                float radius      = halfWidth + radiusBoost;
                float x = radius * MathHelper.cos((float) Math.toRadians(angle));
                float z = radius * MathHelper.sin((float) Math.toRadians(angle));

                float colorT = (float)(
                        Math.sin(Math.toRadians(i * 30 + tier * 60 + mc.player.age * 3))
                                * 0.5 + 0.5
                );
                int c1 = mixColor(colorA, colorB, colorT);
                int c2 = mixColor(colorB, colorA, colorT);

                drawCrystalMesh(
                        buffer, matrices,
                        x, tierY, z,
                        entityHeight * 0.5f,
                        crystalScale * alpha * (0.9f + glowStrength * 0.1f),
                        angle * 2f,
                        c1, c2,
                        alpha * 0.85f
                );
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static void renderMarkerAboveHead(
            MatrixStack matrices,
            LivingEntity entity,
            float alpha,
            float partialTicks,
            int colorA,
            int colorB,
            float glowStrength,
            float scale
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return;

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();

        Vec3d targetPos = interpolatePos(entity, partialTicks);
        Vec3d cameraPos = camera.getPos();
        float time = (mc.player.age + partialTicks) * 4.2f;
        double markerY = targetPos.y + entity.getHeight() + 0.65;

        matrices.push();
        matrices.translate(targetPos.x - cameraPos.x, markerY - cameraPos.y, targetPos.z - cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int glowColor = mixColor(colorA, colorB, 0.5f);
        float glowAlpha = alpha * 0.28f * glowStrength;
        float glowSize = scale * 2.6f * (0.85f + glowStrength * 0.35f);
        drawProceduralGlow(matrices, camera, 0.0f, 0.0f, 0.0f, glowSize, glowColor, glowAlpha);

        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        drawCrystalMesh(
                buffer,
                matrices,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                scale,
                time * 1.8f,
                colorA,
                colorB,
                alpha
        );

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

                                                                                                          
    private static void drawProceduralGlow(
            MatrixStack matrices,
            net.minecraft.client.render.Camera camera,
            float x, float y, float z,
            float size, int color, float alpha
    ) {
        int a = MathHelper.clamp((int)(alpha * 255f), 0, 255);
        if (a <= 0) return;

        int centerColor = (a << 24) | (color & 0x00FFFFFF);
        int outerColor  = (0 << 24) | (color & 0x00FFFFFF);                              
        float r = size * 0.5f;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        BufferBuilder bb = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        var m = matrices.peek().getPositionMatrix();
        int segments = 12;                                    

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * Math.PI * 2.0 / segments);
            float angle2 = (float) ((i + 1) * Math.PI * 2.0 / segments);

            float x1 = MathHelper.cos(angle1) * r;
            float y1 = MathHelper.sin(angle1) * r;
            float x2 = MathHelper.cos(angle2) * r;
            float y2 = MathHelper.sin(angle2) * r;

                                        
            bb.vertex(m, 0, 0, 0).color(centerColor);
                                         
            bb.vertex(m, x1, y1, 0).color(outerColor);
            bb.vertex(m, x2, y2, 0).color(outerColor);
        }

        BufferRenderer.drawWithGlobalProgram(bb.end());
        matrices.pop();
    }

    private static void drawCrystalMesh(
            BufferBuilder buffer,
            MatrixStack matrices,
            float x, float y, float z,
            float targetCenterY,
            float scale,
            float rotation,
            int color1, int color2,
            float alpha
    ) {
        matrices.push();
        matrices.translate(x, y, z);

        Vec3d look = new Vec3d(-x, targetCenterY - y, -z).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(look.x, look.z));
        float pitch = (float) -Math.toDegrees(
                Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        matrices.scale(scale, scale, scale);

        MatrixStack.Entry entry = matrices.peek();

        int r1 = (color1>>16)&255, g1 = (color1>>8)&255, b1 = color1&255;
        int r2 = (color2>>16)&255, g2 = (color2>>8)&255, b2 = color2&255;
        int rm = (r1+r2)/2,        gm = (g1+g2)/2,        bm = (b1+b2)/2;
        int a  = MathHelper.clamp((int)(180 * alpha), 0, 255);

        float w = 0.5f, h = 1.0f;

        tri(buffer,entry, 0,0, h,r1,g1,b1,a, -w,0,0,rm,gm,bm,a, 0, w,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0, h,r1,g1,b1,a,  0,w,0,rm,gm,bm,a, w, 0,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0, h,r1,g1,b1,a,  w,0,0,rm,gm,bm,a, 0,-w,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0, h,r1,g1,b1,a,  0,-w,0,rm,gm,bm,a,-w, 0,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0,-h,r2,g2,b2,a,  0,w,0,rm,gm,bm,a,-w, 0,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0,-h,r2,g2,b2,a,  w,0,0,rm,gm,bm,a, 0, w,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0,-h,r2,g2,b2,a,  0,-w,0,rm,gm,bm,a,w, 0,0,rm,gm,bm,a);
        tri(buffer,entry, 0,0,-h,r2,g2,b2,a, -w,0,0,rm,gm,bm,a, 0,-w,0,rm,gm,bm,a);

        matrices.pop();
    }

    private static void tri(
            BufferBuilder buf, MatrixStack.Entry e,
            float x1,float y1,float z1,int r1,int g1,int b1,int a1,
            float x2,float y2,float z2,int r2,int g2,int b2,int a2,
            float x3,float y3,float z3,int r3,int g3,int b3,int a3
    ) {
        var m = e.getPositionMatrix();
        buf.vertex(m,x1,y1,z1).color(r1,g1,b1,a1);
        buf.vertex(m,x2,y2,z2).color(r2,g2,b2,a2);
        buf.vertex(m,x3,y3,z3).color(r3,g3,b3,a3);
    }

    public static Vec3d interpolatePos(LivingEntity e, float td) {
        return new Vec3d(
                MathHelper.lerp(td, e.prevX, e.getX()),
                MathHelper.lerp(td, e.prevY, e.getY()),
                MathHelper.lerp(td, e.prevZ, e.getZ())
        );
    }

    public static int mixColor(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int r = (int)(((c1>>16)&255)+(((c2>>16)&255)-((c1>>16)&255))*t);
        int g = (int)(((c1>> 8)&255)+(((c2>> 8)&255)-((c1>> 8)&255))*t);
        int b = (int)(( c1     &255)+(( c2     &255)-( c1     &255))*t);
        return (r<<16)|(g<<8)|b;
    }
}
