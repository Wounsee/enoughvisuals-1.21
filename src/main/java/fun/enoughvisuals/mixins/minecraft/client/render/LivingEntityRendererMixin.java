package fun.enoughvisuals.mixins.minecraft.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import fun.enoughvisuals.mixins.accessors.LivingEntityRendererAccessor;
import fun.enoughvisuals.systems.cosmetic.CosmeticBridge;
import fun.enoughvisuals.systems.emotions.EmotionWheelManager;
import fun.enoughvisuals.systems.emotions.render.BobjRig;
import fun.enoughvisuals.systems.emotions.render.EmotionAttachmentContext;
import fun.enoughvisuals.systems.emotions.render.EmotionPlayerModelScope;
import fun.enoughvisuals.systems.emotions.render.PulseEmotionRenderer;
import fun.enoughvisuals.systems.modules.modules.utils.CustomSphereSkin;
import fun.enoughvisuals.utils.render.PetRenderer;
import fun.enoughvisuals.utils.render.ShaderEspRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ProfileComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private boolean enough$customSphereSkinChangedHead;

    @Unique
    private ProfileComponent enough$customSphereSkinOriginalHeadProfile;

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    shift = At.Shift.BEFORE
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void enough$renderPulseEmotion(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci,
            @Share("enough$emotionScope") LocalRef<EmotionPlayerModelScope> scopeRef
    ) {
        // Clear a stale scope defensively if another renderer aborted before reaching its tail.
        EmotionAttachmentContext.clear();
        if (!(state instanceof PlayerEntityRenderState playerState)
                || playerState.spectator
                || playerState.invisibleToPlayer) return;

        EmotionWheelManager.RenderEmotion emotion = EmotionWheelManager.getInstance().getRenderEmotion(playerState);
        if (emotion == null) return;
        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
        if (!(renderer.getModel() instanceof PlayerEntityModel playerModel)) return;
        // Every Wounsee entry currently exposed by CosmeticCatalog is a full MODEL slot. A ready
        // avatar therefore owns the base body regardless of what its Lua script does with vanilla
        // visibility. Suppress both BOBJ and the vanilla base for this draw, leaving exactly the
        // already-rendered Figura body plus its normal feature layers.
        if (CosmeticBridge.hasRenderReadyAvatar(playerState)) {
            scopeRef.set(new EmotionPlayerModelScope());
            return;
        }
        // Preserve another integration's explicit core-part ownership as a final fallback.
        if (!EmotionPlayerModelScope.hasCompleteBase(playerModel)) return;

        float alpha = state.invisible ? 0.15f : 1.0f;
        int overlay = LivingEntityRenderer.getOverlay(state, 0.0f);
        boolean rendered = PulseEmotionRenderer.render(
                playerState,
                matrices,
                vertexConsumers,
                light,
                overlay,
                renderer.getTexture(state),
                emotion.model(),
                emotion.elapsedSeconds(),
                emotion.blend(),
                alpha
        );
        if (!rendered) return;

        // Apply the vanilla attachment proxy only after the BOBJ body actually rendered. Doing
        // this from PlayerEntityModel#setAngles affected Figura and every non-BOBJ render path.
        // Hat is already a child of head, so copying head into it would apply the transform twice.
        EmotionPlayerModelScope scope = new EmotionPlayerModelScope();
        scope.captureTransforms(playerModel);
        ModelTransform hatLocalTransform = playerModel.hat.getTransform();
        ModelTransform rightArmBaseTransform = playerModel.rightArm.getTransform();
        ModelTransform leftArmBaseTransform = playerModel.leftArm.getTransform();
        BobjRig.Pose attachmentPose = EmotionWheelManager.getInstance().apply(
                playerState, playerModel.head, playerModel.hat,
                playerModel.body, playerModel.rightArm, playerModel.leftArm,
                playerModel.rightLeg, playerModel.leftLeg);
        playerModel.hat.setTransform(hatLocalTransform);
        EmotionAttachmentContext.begin(
                playerModel, attachmentPose, rightArmBaseTransform, leftArmBaseTransform);

        // Defer visibility mutation until the wrapped base draw itself. Other BEFORE hooks (for
        // example ShaderESP capture) must never observe an accidentally hidden player model.
        scopeRef.set(scope);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void enough$renderVanillaWithScopedVisibility(
            EntityModel<?> model,
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            int light,
            int overlay,
            int color,
            Operation<Void> original,
            @Share("enough$emotionScope") LocalRef<EmotionPlayerModelScope> scopeRef
    ) {
        EmotionPlayerModelScope scope = scopeRef.get();
        if (scope != null && model instanceof PlayerEntityModel playerModel) {
            scope.hideBase(playerModel);
        }
        try {
            original.call(model, matrices, vertexConsumer, light, overlay, color);
        } finally {
            if (scope != null) scope.restoreVisibility();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void enough$restoreEmotionAttachmentProxy(
            LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci,
            @Share("enough$emotionScope") LocalRef<EmotionPlayerModelScope> scopeRef
    ) {
        EmotionPlayerModelScope scope = scopeRef.get();
        if (scope != null) {
            scope.restoreVisibility();
            scope.restoreTransforms();
            scopeRef.set(null);
        }
        EmotionAttachmentContext.clear();
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void enough$hideVanillaPlayer(EntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        enough$customSphereSkinChangedHead = false;
        enough$customSphereSkinOriginalHeadProfile = null;

        if (state instanceof PlayerEntityRenderState playerState) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!(client.currentScreen instanceof InventoryScreen)) {
                PetRenderer.renderWorldPet(playerState.id, playerState.pose, matrices, vertexConsumers, light);
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
                    shift = At.Shift.BEFORE
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
        LivingEntityRenderer livingRenderer = (LivingEntityRenderer) (Object) this;
        ShaderEspRenderer.captureLiving(
                state,
                matrices,
                (Model) livingRenderer.getModel(),
                ((LivingEntityRendererAccessor) livingRenderer).enoughvisuals$getFeatures(),
                livingRenderer.getTexture(state)
        );
    }

    @Inject(method = "shouldRenderFeatures", at = @At("HEAD"), cancellable = true)
    private void enough$skipFeaturesDuringShaderMask(net.minecraft.client.render.entity.state.LivingEntityRenderState state,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (ShaderEspRenderer.isRenderingMask()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"
            )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void enough$renderChinaHat(
            net.minecraft.client.render.entity.state.LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
        if (renderer.getModel() instanceof net.minecraft.client.render.entity.model.PlayerEntityModel playerModel) {
            fun.enoughvisuals.systems.modules.modules.visual.ChinaHat.renderForPlayer(
                    playerState, playerModel, matrices, vertexConsumers, light);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void enough$drawHitRange(
            net.minecraft.client.render.entity.state.LivingEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        fun.enoughvisuals.systems.modules.modules.visual.HitRange config = fun.enoughvisuals.systems.modules.modules.visual.HitRange.getInstance();
        if (config == null || !config.isEnabled()) {
            return;
        }

        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        net.minecraft.client.network.ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.getId() == playerState.id) {
            return;
        }

        net.minecraft.util.math.Vec3d pos = new net.minecraft.util.math.Vec3d(state.x, state.y, state.z);
        if (!pos.isInRange(player.getPos(), config.maxDistance.get())) {
            return;
        }

        if (config.nearestOnly.get()) {
            if (config.getNearest() == null || config.getNearest().getId() != playerState.id) {
                return;
            }
        }

        if (playerState.deathTime > 0.0f || playerState.invisibleToPlayer || playerState.sleepingDirection != null) {
            return;
        }

        fun.enoughvisuals.utils.render.HitRangeCircleRenderer.drawCircle(matrices, vertexConsumers, playerState);
    }
}
