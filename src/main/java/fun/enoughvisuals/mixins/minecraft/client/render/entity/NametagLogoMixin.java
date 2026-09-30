package fun.enoughvisuals.mixins.minecraft.client.render.entity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.client.Globals;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

@Mixin(EntityRenderer.class)
public class NametagLogoMixin {
    private static final int GL_BLEND_EQUATION = 0x8009;
    private static final float LOGO_SIZE = 16.0f;
    private static final float OFFSET_Y = -1.8f;

    @ModifyVariable(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private Text enough$addSpaces(Text original, EntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return original;
        if (playerState.name == null || playerState.name.isEmpty()) return original;
        if (!Globals.nametagsEnabled()) return original;
        if (original == playerState.playerName) return original;
        if (!enough$hasLogoTarget(original == null ? "" : original.getString(), playerState)) return original;
        return original.copy().append(Text.literal(Globals.LOGO_SPACES));
    }

    @Inject(
            method = "renderLabelIfPresent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V")
    )
    private void enough$renderLogo(EntityRenderState state, Text text, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        if (playerState.name == null || playerState.name.isEmpty()) return;
        if (!Globals.nametagsEnabled()) return;
        if (text == playerState.playerName) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || text == null) return;

        String label = text.getString();
        if (!enough$hasLogoTarget(label, playerState)) return;

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        TextRenderer textRenderer = mc.textRenderer;
        float spaceWidth = textRenderer.getWidth(Globals.LOGO_SPACES);
        boolean alreadyPadded = label.endsWith(Globals.LOGO_SPACES);
        float fullWidth = textRenderer.getWidth(text) + (alreadyPadded ? 0f : spaceWidth);
        float contentWidth = fullWidth - spaceWidth;
        float startX = -fullWidth / 2f;
        float logoX = startX + contentWidth + spaceWidth * 0.5f - LOGO_SIZE * 0.5f;

        RenderStateSnapshot renderState = RenderStateSnapshot.capture();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            DrawContext ctx = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
            ctx.getMatrices().push();
            ctx.getMatrices().peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
            CustomFontRenderer.drawText(ctx, Globals.LOGO_ICON, logoX, OFFSET_Y,
                    LOGO_SIZE, Globals.LOGO_COLOR, FontWeight.LOGO);
            ctx.getMatrices().pop();
        } finally {
            renderState.restore();
        }
    }

    private boolean enough$hasLogoTarget(String label, PlayerEntityRenderState playerState) {
        if (Globals.findEnoughUser(label).found()) return true;
        if (Globals.isEnoughUser(playerState.name)) {
            Globals.rememberVisibleAlias(label, playerState.name);
            return true;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSession() == null) return false;
        if (mc.world != null) {
            net.minecraft.entity.Entity entity = mc.world.getEntityById(playerState.id);
            if (entity instanceof net.minecraft.entity.player.PlayerEntity player
                    && Globals.isEnoughUuid(player.getUuidAsString())) {
                Globals.rememberVisibleAlias(label, playerState.name, player.getUuidAsString());
                return true;
            }
        }
        return "FakePlayer".equals(playerState.name);
    }

    private record RenderStateSnapshot(
            boolean blend,
            boolean depthTest,
            boolean cull,
            boolean depthMask,
            int depthFunc,
            int blendSrcRgb,
            int blendDstRgb,
            int blendSrcAlpha,
            int blendDstAlpha,
            int blendEquation,
            ShaderProgram shader,
            int texture0,
            int texture1,
            int texture2,
            int activeTexture,
            float[] shaderColor
    ) {
        private static RenderStateSnapshot capture() {
            return new RenderStateSnapshot(
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glGetInteger(GL11.GL_DEPTH_FUNC),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glGetInteger(GL_BLEND_EQUATION),
                    RenderSystem.getShader(),
                    RenderSystem.getShaderTexture(0),
                    RenderSystem.getShaderTexture(1),
                    RenderSystem.getShaderTexture(2),
                    GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                    RenderSystem.getShaderColor().clone()
            );
        }

        private void restore() {
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();

            RenderSystem.depthMask(depthMask);
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.blendEquation(blendEquation);
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            if (shader != null) RenderSystem.setShader(shader);
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.setShaderTexture(1, texture1);
            RenderSystem.setShaderTexture(2, texture2);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(texture0);
            GlStateManager._activeTexture(activeTexture);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }
}
