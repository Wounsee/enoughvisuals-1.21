package fun.wounsee.cosmetic.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.ducks.LivingEntityRendererAccessor;
import fun.wounsee.cosmetic.gui.PopupMenu;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaPart;
import fun.wounsee.cosmetic.math.matrix.FiguraMat4;
import fun.wounsee.cosmetic.model.rendering.PartFilterScheme;
import fun.wounsee.cosmetic.permissions.Permissions;
import fun.wounsee.cosmetic.utils.RenderUtils;
import fun.wounsee.cosmetic.utils.ui.UIHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> extends EntityRenderer<T,S> implements RenderLayerParent<S, M> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Shadow @Final protected List<RenderLayer<S, M>> layers;

    @Shadow
    public static int getOverlayCoords(LivingEntityRenderState arg, float whiteOverlayProgress) {
        return 0;
    }

    @Shadow protected abstract float getWhiteOverlayProgress(S arg);

    @Shadow protected abstract boolean isBodyVisible(S livingEntityRenderState);

    @Shadow @Final protected ItemModelResolver itemModelResolver;
    @Shadow public abstract M getModel();

    @Unique
    private Avatar currentAvatar;
    @Unique
    private Matrix4f lastPose;

    @Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void onRender(S livingEntityRenderState, PoseStack poseStack, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        currentAvatar = AvatarManager.getAvatar(livingEntityRenderState);
        if (currentAvatar == null)
            return;

        lastPose = poseStack.last().pose();
    }

    @ModifyArg(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            ),
            method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            index = 3
    )
    private int customOverlay(int thing) {
        return LivingEntityRendererAccessor.overrideOverlay.orElse(thing);
    }

    @WrapOperation(
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"
            ),
            method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
    )
    private void suppressVanillaBodyInPreview(EntityModel<?> instance, PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k, Operation<Void> original) {
        if (UIHelper.paperdoll && UIHelper.isPreviewHideVanillaModel()) {
            return;
        }
        original.call(instance, poseStack, vertexConsumer, i, j, k);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V", shift = At.Shift.AFTER), method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
    private void preRender(S livingEntityRenderState, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (currentAvatar == null)
            return;

        if (Avatar.firstPerson) {
            currentAvatar.updateMatrices((LivingEntityRenderer<?, ?, ?>) (Object) this, poseStack);
            currentAvatar = null;
            lastPose = null;
            poseStack.popPose();
            ci.cancel();
            return;
        }

        if (currentAvatar.luaRuntime != null) {
            VanillaPart part = currentAvatar.luaRuntime.vanilla_model.PLAYER;
            EntityModel<?> model = getModel();
            part.save(model);
            if (currentAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
                part.preTransform(model);
        }

        boolean showBody = isBodyVisible(livingEntityRenderState);
        boolean translucent = !showBody && !livingEntityRenderState.isInvisibleToPlayer;
        boolean glowing = !showBody && livingEntityRenderState.appearsGlowing;
        boolean invisible = !translucent && !showBody && !glowing;
        Integer id = livingEntityRenderState instanceof PlayerRenderState playerRenderState ? playerRenderState.id : ((FiguraEntityRenderStateExtension)livingEntityRenderState).figura$getEntityId();
        if (id == null) return;

        Entity entity = AvatarManager.getEntity(livingEntityRenderState);
        if (entity == null) return;
        float tickDelta = ((FiguraEntityRenderStateExtension)livingEntityRenderState).figura$getTickDelta();

        // When viewed 3rd person, render all non-world parts.
        PartFilterScheme filter = invisible ? PartFilterScheme.PIVOTS : PartFilterScheme.MODEL;
        int overlay = getOverlayCoords(livingEntityRenderState, getWhiteOverlayProgress(livingEntityRenderState));

        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);
        CosmeticMod.pushProfiler(currentAvatar);

        CosmeticMod.pushProfiler("calculateMatrix");
        Matrix4f diff = new Matrix4f(lastPose).invert().mul(poseStack.last().pose());
        FiguraMat4 poseMatrix = new FiguraMat4().set(diff);

        CosmeticMod.popPushProfiler("renderEvent");
        currentAvatar.renderEvent(tickDelta, poseMatrix);

        CosmeticMod.popPushProfiler("render");
        float renderAlpha = (translucent ? 0.15f : 1f) * (UIHelper.paperdoll ? UIHelper.getPreviewAlpha() : 1f);
        // Vertex alpha only fades correctly on an alpha-capable layer. Preview transitions used
        // to keep CUTOUT/EYES layers when the entity itself was otherwise opaque, leaving opaque
        // avatar fragments until the final frame.
        boolean alphaTranslucent = translucent || renderAlpha < 0.999f;
        currentAvatar.render(entity, livingEntityRenderState.yRot, tickDelta, renderAlpha, poseStack, bufferSource, light, overlay, (LivingEntityRenderer<?, ?, ?>) (Object) this, filter, alphaTranslucent, glowing);

        CosmeticMod.popPushProfiler("postRenderEvent");
        currentAvatar.postRenderEvent(tickDelta, poseMatrix);

        CosmeticMod.popProfiler(3);

        if (currentAvatar.luaRuntime != null && currentAvatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1)
            currentAvatar.luaRuntime.vanilla_model.PLAYER.posTransform(getModel());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"), method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void endRender(S livingEntityRenderState, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        if (currentAvatar == null)
            return;

        // Render avatar with params
        if (currentAvatar.luaRuntime != null)
            currentAvatar.luaRuntime.vanilla_model.PLAYER.restore(getModel());

        currentAvatar = null;
        lastPose = null;
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void shouldShowName(T livingEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(Configs.PREVIEW_NAMEPLATE.value);
        else if (!Minecraft.renderNames() || livingEntity.getUUID().equals(PopupMenu.getEntityId()))
            cir.setReturnValue(false);
        else if (!AvatarManager.panic) {
            if (Configs.SELF_NAMEPLATE.value && livingEntity == Minecraft.getInstance().player)
                cir.setReturnValue(true);
            else if (Configs.NAMEPLATE_RENDER.value == 2 || (Configs.NAMEPLATE_RENDER.value == 1 && livingEntity != CosmeticMod.extendedPickEntity))
                cir.setReturnValue(false);
        }
    }

    // Add the skull item back in after it being cleared
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;clear()V", ordinal = 0, shift = At.Shift.AFTER))
    private void shouldShowName(T entity, S livingEntityRenderState, float f, CallbackInfo ci) {
       this.itemModelResolver.updateForLiving(livingEntityRenderState.headItem, entity.getItemBySlot(EquipmentSlot.HEAD), ItemDisplayContext.HEAD, false, entity);
    }

    @Inject(method = "isEntityUpsideDown", at = @At("HEAD"), cancellable = true)
    private static void isEntityUpsideDown(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            Boolean upsideDown = avatar.luaRuntime.renderer.upsideDown;
            if (upsideDown != null)
                cir.setReturnValue(upsideDown);
        }
    }
}
