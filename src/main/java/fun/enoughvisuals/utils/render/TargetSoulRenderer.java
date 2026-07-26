package fun.enoughvisuals.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import fun.enoughvisuals.port.EasedAnimation;
import fun.enoughvisuals.port.Easings;

/** Ported TargetESP's "Обычный" ghost mode, with only the 1.21.4 buffer adapter changed. */
public final class TargetSoulRenderer {
    private static final Identifier GLOW = Identifier.of("enoughfx", "textures/world/glow.png");
    private final EasedAnimation hurtAnimation = new EasedAnimation();
    private long timestamp;
    private int targetId = Integer.MIN_VALUE;

    public void reset() {
        timestamp = 0L;
        targetId = Integer.MIN_VALUE;
        hurtAnimation.invoke(0.0);
    }

    public void render(WorldRenderContext context, LivingEntity livingEntity, float alpha, float tickDelta, boolean throughWalls) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (livingEntity == null || context == null || context.matrixStack() == null || context.camera() == null) {
            return;
        }
        if (targetId != livingEntity.getId()) {
            targetId = livingEntity.getId();
            timestamp = 0L;
        }

        double doubleValue68 = 0.3 + livingEntity.getWidth() / 2.0F;
        hurtAnimation.check();
        int intValue68 = livingEntity.hurtTime;
        float floatValue76 = (float)Math.sin(intValue68 * (Math.PI / 20));
        hurtAnimation.animateTo(floatValue76, 0.4F, Easings.EASE_OUT_QUART);
        float floatValue77 = hurtAnimation.measure3();
        float floatValue78 = 30.0F;
        float floatValue79 = 0.4F - 0.1F * floatValue77;
        double doubleValue69 = 6 - (int)(1.0F * floatValue77);
        int intValue69 = 40 - (int)(12.0F * floatValue77);
        Vec3d vec3d24 = client.gameRenderer.getCamera().getPos();
        Camera camera8 = client.gameRenderer.getCamera();
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis();
        }

        long longValue8 = System.currentTimeMillis();
        Vec3d vec3d25 = livingEntity.getLerpedPos(tickDelta);
        vec3d25 = new Vec3d(vec3d25.x, vec3d25.y + 0.32 + livingEntity.getHeight() / 2.0F, vec3d25.z);
        double doubleValue70 = vec3d25.x + 0.2;
        double doubleValue71 = vec3d25.y;
        double doubleValue72 = vec3d25.z;
        float floatValue80 = alpha;
        int intValue70 = compute((int)(255.0F * floatValue80), floatValue77);
        int intValue71 = intValue70;
        int intValue72 = compute2(intValue70, (int)(210.0F * floatValue80));
        int intValue73 = compute2(intValue70, (int)(150.0F * floatValue80));
        int intValue74 = compute2(intValue70, (int)(90.0F * floatValue80));

        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW);

        BufferBuilder vertexConsumer9 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        MatrixStack matrixStack = context.matrixStack();
        matrixStack.push();
        matrixStack.translate(doubleValue70 - vec3d24.x, doubleValue71 - vec3d24.y, doubleValue72 - vec3d24.z);
        float floatValue81 = 0.3F;

        for (int intValue75 = 0; intValue75 < intValue69; intValue75++) {
            double doubleValue73 = 0.05F * (longValue8 - timestamp - intValue75 * doubleValue69) / floatValue78;
            double doubleValue74 = Math.sin(doubleValue73 * Math.PI) * doubleValue68;
            double doubleValue75 = Math.cos(doubleValue73 * Math.PI) * doubleValue68;
            double doubleValue76 = Math.cos(doubleValue73 * Math.PI) * doubleValue68;
            float floatValue82 = (float)intValue75 / (intValue69 - 1);
            float floatValue83 = 1.0F - floatValue82 * floatValue81;
            float floatValue84 = floatValue79 * floatValue83;
            matrixStack.push();
            matrixStack.translate(doubleValue74, doubleValue76, -doubleValue75);
            billboard(matrixStack, camera8, floatValue84);
            invoke16(vertexConsumer9, matrixStack.peek().getPositionMatrix(), intValue71, intValue72, intValue73, intValue74, floatValue84);
            matrixStack.pop();
        }

        for (int intValue76 = 0; intValue76 < intValue69; intValue76++) {
            double doubleValue77 = 0.05F * (longValue8 - timestamp - intValue76 * doubleValue69) / floatValue78;
            double doubleValue78 = Math.sin(doubleValue77 * Math.PI) * doubleValue68;
            double doubleValue79 = Math.cos(doubleValue77 * Math.PI) * doubleValue68;
            double doubleValue80 = Math.sin(doubleValue77 * Math.PI) * doubleValue68;
            float floatValue85 = (float)intValue76 / (intValue69 - 1);
            float floatValue86 = 1.0F - floatValue85 * floatValue81;
            float floatValue87 = floatValue79 * floatValue86;
            matrixStack.push();
            matrixStack.translate(-doubleValue78, doubleValue80, -doubleValue79);
            billboard(matrixStack, camera8, floatValue87);
            invoke16(vertexConsumer9, matrixStack.peek().getPositionMatrix(), intValue71, intValue72, intValue73, intValue74, floatValue87);
            matrixStack.pop();
        }

        for (int intValue77 = 0; intValue77 < intValue69; intValue77++) {
            double doubleValue81 = 0.05F * (longValue8 - timestamp - intValue77 * doubleValue69) / floatValue78;
            double doubleValue82 = Math.sin(doubleValue81 * Math.PI) * doubleValue68;
            double doubleValue83 = Math.cos(doubleValue81 * Math.PI) * doubleValue68;
            double doubleValue84 = Math.sin(doubleValue81 * Math.PI) * doubleValue68;
            float floatValue88 = (float)intValue77 / (intValue69 - 1);
            float floatValue89 = 1.0F - floatValue88 * floatValue81;
            float floatValue90 = floatValue79 * floatValue89;
            matrixStack.push();
            matrixStack.translate(doubleValue82, doubleValue84, doubleValue83);
            billboard(matrixStack, camera8, floatValue90);
            invoke16(vertexConsumer9, matrixStack.peek().getPositionMatrix(), intValue71, intValue72, intValue73, intValue74, floatValue90);
            matrixStack.pop();
        }

        matrixStack.pop();
        BufferRenderer.drawWithGlobalProgram(vertexConsumer9.end());

        RenderSystem.depthMask(depthMask);
        RenderSystem.depthFunc(depthFunc);
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void billboard(MatrixStack matrixStack, Camera camera, float size) {
        matrixStack.translate(-size / 2.0F, -size / 2.0F, 0.0F);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.translate(size / 2.0F, size / 2.0F, 0.0F);
    }

    private static int compute(int alpha, float hurt) {
        int intValue60 = CustomTheme.hudAccent() & 0x00FFFFFF;
        float floatValue75 = hurt < 0.0F ? 0.0F : Math.min(hurt, 1.0F);
        int intValue61 = intValue60 >> 16 & 0xFF;
        int intValue62 = intValue60 >> 8 & 0xFF;
        int intValue63 = intValue60 & 0xFF;
        int intValue64 = Math.round(intValue61 + (235 - intValue61) * floatValue75);
        int intValue65 = Math.round(intValue62 + (70 - intValue62) * floatValue75);
        int intValue66 = Math.round(intValue63 + (70 - intValue63) * floatValue75);
        int intValue67 = Math.max(0, Math.min(255, alpha));
        return intValue67 << 24 | intValue64 << 16 | intValue65 << 8 | intValue66;
    }

    private static int compute2(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

    private static void invoke16(BufferBuilder vertexConsumer, Matrix4f matrix4f, int i, int j, int k, int l, float f) {
        vertex(vertexConsumer, matrix4f, 0.0F, -f, 0.0F, 0.0F, i);
        vertex(vertexConsumer, matrix4f, -f, -f, 0.0F, 1.0F, j);
        vertex(vertexConsumer, matrix4f, -f, 0.0F, 1.0F, 1.0F, k);
        vertex(vertexConsumer, matrix4f, 0.0F, 0.0F, 1.0F, 0.0F, l);
    }

    private static void vertex(BufferBuilder consumer, Matrix4f matrix, float x, float y, float u, float v, int color) {
        consumer.vertex(matrix, x, y, 0.0F).texture(u, v).color(color);
    }
}
