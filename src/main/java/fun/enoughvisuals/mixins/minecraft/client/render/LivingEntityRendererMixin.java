package fun.enoughvisuals.mixins.minecraft.client.render;

import fun.enoughvisuals.mixins.accessors.LivingEntityRendererAccessor;
import fun.enoughvisuals.systems.modules.modules.utils.CustomSphereSkin;
import fun.enoughvisuals.utils.render.CustomModelRenderer;
import fun.enoughvisuals.utils.render.PetRenderer;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private boolean enough$customSphereSkinChangedHead;

    @Unique
    private ProfileComponent enough$customSphereSkinOriginalHeadProfile;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void enough$hideVanillaPlayer(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        enough$customSphereSkinChangedHead = false;
        enough$customSphereSkinOriginalHeadProfile = null;

        if (state instanceof PlayerEntityRenderState playerState) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!(client.currentScreen instanceof InventoryScreen)) {
                PetRenderer.renderWorldPet(playerState.id, playerState.pose, matrices, vertexConsumers, light);
            }
            if (CustomModelRenderer.shouldHideVanillaPlayer(playerState.id)) {
                ci.cancel();
                return;
            }

            if (client.player == null || playerState.id != client.player.getId() || playerState.wearingSkullType == null) {
                return;
            }

            ProfileComponent profile = CustomSphereSkin.getActiveProfileForRender();
            if (profile != null) {
                enough$customSphereSkinOriginalHeadProfile = playerState.wearingSkullProfile;
                enough$customSphereSkinChangedHead = true;
                playerState.wearingSkullProfile = profile;
            }
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void enough$restoreCustomSphereSkinWornHead(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!enough$customSphereSkinChangedHead || !(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        playerState.wearingSkullProfile = enough$customSphereSkinOriginalHeadProfile;
        enough$customSphereSkinChangedHead = false;
        enough$customSphereSkinOriginalHeadProfile = null;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    shift = At.Shift.AFTER
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void enough$captureShaderEsp(
            net.minecraft.client.render.entity.state.LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(state instanceof PlayerEntityRenderState playerState) || !((Object) this instanceof PlayerEntityRenderer renderer)) {
            return;
        }

        LivingEntityRenderer livingRenderer = (LivingEntityRenderer) (Object) this;
        ShaderEspRenderer.capturePlayer(
                playerState,
                matrices,
                (Model) livingRenderer.getModel(),
                ((LivingEntityRendererAccessor) livingRenderer).enoughvisuals$getFeatures(),
                renderer.getTexture(playerState)
        );
    }
}
