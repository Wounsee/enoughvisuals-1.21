package fun.enoughvisuals.mixins.minecraft.client.render.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.systems.modules.modules.client.Globals;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class NametagLogoMixin {
    private static final float LOGO_SIZE = 15.0f;
    private static final float OFFSET_Y = -1.0f;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void enough$skipLabelDuringShaderMask(EntityRenderState state, Text text, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (ShaderEspRenderer.isRenderingMask()) {
            ci.cancel();
        }
    }

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
        if (!enough$hasLogoTarget(original == null ? "" : original.getString(), playerState)) return original;
        return Text.literal(Globals.LOGO_SPACES).append(original);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    private void enough$renderLogo(EntityRenderState state, Text text, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        if (playerState.name == null || playerState.name.isEmpty()) return;
        if (!Globals.nametagsEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.gameRenderer == null || text == null) return;

        String label = text.getString();
        if (!enough$hasLogoTarget(label, playerState)) return;

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        Camera camera = mc.gameRenderer.getCamera();
        TextRenderer textRenderer = mc.textRenderer;
        float spaceWidth = textRenderer.getWidth(Globals.LOGO_SPACES);
        float fullWidth = textRenderer.getWidth(text);
        float startX = -fullWidth / 2f;
        float logoX = startX + spaceWidth * 0.5f - LOGO_SIZE * 0.5f;

        matrices.push();
        matrices.translate(0f, state.height + 0.565f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch()));
        matrices.translate(0f, 0f, -0.015f);
        matrices.scale(0.025f, -0.025f, 0.025f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        DrawContext ctx = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        ctx.getMatrices().push();
        ctx.getMatrices().peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
        CustomFontRenderer.drawText(ctx, Globals.LOGO_ICON, logoX, OFFSET_Y, LOGO_SIZE, Globals.LOGO_COLOR, FontWeight.LOGO);
        ctx.getMatrices().pop();

        matrices.pop();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private boolean enough$hasLogoTarget(String label, PlayerEntityRenderState playerState) {
        if (Globals.findEnoughUser(label).found()) return true;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSession() == null) return false;
        if (!"FakePlayer".equals(playerState.name)) return false;
        return Globals.isEnoughUser(mc.getSession().getUsername());
    }
}
