package fun.wounsee.cosmetic.mixin.render.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.model.ParentType;
import fun.wounsee.cosmetic.utils.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParrotOnShoulderLayer.class)
public abstract class ParrotOnShoulderLayerMixin<S extends PlayerRenderState> extends RenderLayer<S, PlayerModel> {

    public ParrotOnShoulderLayerMixin(RenderLayerParent<S, PlayerModel> renderLayerParent) {
        super(renderLayerParent);
    }

    @Shadow @Final private ParrotModel model;

    @Shadow @Final private ParrotRenderState parrotState;

    @Inject(at = @At("HEAD"), method = "renderOnShoulder", cancellable = true)
    private void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, PlayerRenderState playerRenderState, Parrot.Variant variant, float yRot, float xRot, boolean leftShoulder, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(playerRenderState);
        if (!RenderUtils.vanillaModel(avatar))
            return;

        // script hide
        if (avatar.luaRuntime != null &&
                (leftShoulder && !avatar.luaRuntime.vanilla_model.LEFT_PARROT.checkVisible() ||
                !leftShoulder && !avatar.luaRuntime.vanilla_model.RIGHT_PARROT.checkVisible())
        ) {
            ci.cancel();
            return;
        }

        // pivot part
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.renderType(ParrotRenderer.getVariantTexture(variant)));
        if (avatar.pivotPartRender(leftShoulder ? ParentType.LeftParrotPivot : ParentType.RightParrotPivot, stack -> {
            stack.translate(0d, 24d, 0d);
            float s = 16f;
            stack.scale(s, s, s);
            stack.mulPose(Axis.XP.rotationDegrees(180f));
            stack.mulPose(Axis.YP.rotationDegrees(180f));
            this.parrotState.ageInTicks = playerRenderState.ageInTicks;
            this.parrotState.walkAnimationPos = playerRenderState.walkAnimationPos;
            this.parrotState.walkAnimationSpeed = playerRenderState.walkAnimationSpeed;
            this.parrotState.yRot = yRot;
            this.parrotState.xRot = xRot;
            this.model.setupAnim(this.parrotState);
            this.model.renderToBuffer(stack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        })) {
            ci.cancel();
        }
    }
}
