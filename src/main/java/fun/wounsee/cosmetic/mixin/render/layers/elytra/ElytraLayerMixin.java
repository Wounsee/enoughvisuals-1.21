package fun.wounsee.cosmetic.mixin.render.layers.elytra;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaPart;
import fun.wounsee.cosmetic.mixin.render.layers.EquipmentLayerRendererAccessor;
import fun.wounsee.cosmetic.model.ParentType;
import fun.wounsee.cosmetic.permissions.Permissions;
import fun.wounsee.cosmetic.utils.PlatformUtils;
import fun.wounsee.cosmetic.utils.RenderUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(WingsLayer.class)
public abstract class ElytraLayerMixin<T extends LivingEntity, S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    public ElytraLayerMixin(RenderLayerParent<S, M> context) {
        super(context);
    }

    @Shadow @Final private ElytraModel elytraModel;

    @Shadow @Final private EquipmentLayerRenderer equipmentRenderer;

    @Unique
    private VanillaPart vanillaPart;
    @Unique
    private Avatar figura$avatar;

    @Unique
    private boolean renderedPivot;

    @Inject(at = @At(value = "HEAD"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V")
    public void setAvatar(PoseStack matrices, MultiBufferSource vertexConsumers, int i, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        figura$avatar = AvatarManager.getAvatar(humanoidRenderState);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ElytraModel;setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", shift = At.Shift.AFTER), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V")
    public void onRender(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        vanillaPart = null;
        if (figura$avatar == null)
            return;

        if (figura$avatar.luaRuntime != null) {
            VanillaPart part = figura$avatar.luaRuntime.vanilla_model.ELYTRA;
            part.save(elytraModel);
            if (figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                vanillaPart = part;
                vanillaPart.preTransform(elytraModel);
            }
        }

        Integer id = humanoidRenderState instanceof PlayerRenderState playerRenderState ? playerRenderState.id : ((FiguraEntityRenderStateExtension)humanoidRenderState).figura$getEntityId();
        if (id != null)
            figura$avatar.elytraRender(Minecraft.getInstance().level.getEntity(id), multiBufferSource, poseStack, light, ((FiguraEntityRenderStateExtension)humanoidRenderState).figura$getTickDelta(), elytraModel);

        if (vanillaPart != null)
            vanillaPart.posTransform(elytraModel);

    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", cancellable = true)
    public void cancelVanillaPart(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, S humanoidRenderState, float f, float g, CallbackInfo ci) {
        if (vanillaPart != null)
            vanillaPart.restore(elytraModel);
        renderedPivot = true;
        renderElytraPivot(humanoidRenderState, poseStack, multiBufferSource, light);
        if (renderedPivot) {
            poseStack.popPose();
            ci.cancel();
        }
    }

    public void renderElytraPivot(S state, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {

        ItemStack itemStack = state.chestEquipment;
        if (!itemStack.is(Items.ELYTRA) && !PlatformUtils.isModLoaded("origins")) {
            return;
        }
        if (figura$avatar != null && figura$avatar.luaRuntime != null && figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1 && figura$avatar.luaRuntime.vanilla_model.ELYTRA.checkVisible()) {
            // Try to render the pivot part
            this.elytraModel.setupAnim(state);

            ResourceLocation playerTexture =  RenderUtils.getPlayerSkinTexture((WingsLayer<?, ?>) (Object)this, state);

            VanillaPart part = RenderUtils.pivotToPart(figura$avatar, ParentType.LeftElytraPivot);
            if (part != null && part.checkVisible()) {
                boolean leftWing = figura$avatar.pivotPartRender(ParentType.LeftElytraPivot, stack -> {
                    stack.pushPose();
                    stack.scale(16, 16, 16);
                    stack.mulPose(Axis.XP.rotationDegrees(180f));
                    stack.mulPose(Axis.YP.rotationDegrees(180f));
                    stack.translate(0.0f, 0.0f, 0.125f);
                    figura$renderElytraPart(((ElytraModelAccessor)this.elytraModel).getLeftWing(), stack, multiBufferSource, light, itemStack, playerTexture);
                    stack.popPose();
                });
                if (!leftWing) {
                    figura$renderElytraPart(((ElytraModelAccessor)this.elytraModel).getLeftWing(), poseStack, multiBufferSource, light, itemStack, playerTexture);
                }
            }
            part = RenderUtils.pivotToPart(figura$avatar, ParentType.RightElytraPivot);
            if (part != null && part.checkVisible()) {
                    boolean rightWing = figura$avatar.pivotPartRender(ParentType.RightElytraPivot, stack -> {
                    stack.pushPose();
                    stack.scale(16, 16, 16);
                    stack.mulPose(Axis.XP.rotationDegrees(180f));
                    stack.mulPose(Axis.YP.rotationDegrees(180f));
                    stack.translate(0.0f, 0.0f, 0.125f);
                    figura$renderElytraPart(((ElytraModelAccessor)this.elytraModel).getRightWing(), stack, multiBufferSource, light, itemStack, playerTexture);
                    stack.popPose();
                });
                if (!rightWing) {
                    figura$renderElytraPart(((ElytraModelAccessor)this.elytraModel).getRightWing(), poseStack, multiBufferSource, light, itemStack, playerTexture);
                }
            }
        } else renderedPivot = figura$avatar != null && figura$avatar.luaRuntime != null && figura$avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1 && !figura$avatar.luaRuntime.vanilla_model.ELYTRA.checkVisible();
    }

    // rewritten to work with mojang's shiny new layer system
    @Unique
    private void figura$renderElytraPart(ModelPart modelPart, PoseStack poseStack, MultiBufferSource vertexConsumers, int light, ItemStack itemStack, @Nullable ResourceLocation playerLocation) {
        boolean hasGlint = itemStack.hasFoil();

        EquipmentClientInfo.LayerType layerType = EquipmentClientInfo.LayerType.WINGS;
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);

        if (equippable == null)
            return;

        Optional<ResourceKey<EquipmentAsset>> location = equippable.assetId();
        if (location.isEmpty())
            return;

        List<EquipmentClientInfo.Layer> list = ((EquipmentLayerRendererAccessor)this.equipmentRenderer).figura$getAssetsManager().get(location.get()).getLayers(layerType);

        int i = itemStack.is(ItemTags.DYEABLE) ? DyedItemColor.getOrDefault(itemStack, -6265536) : -1;

        for(EquipmentClientInfo.Layer layer : list) {
            int k = EquipmentLayerRendererAccessor.getColorForLayer(layer, i);

            if (k != 0) {
                ResourceLocation normalArmorResource = layer.usePlayerTexture() && playerLocation != null ? playerLocation : ((EquipmentLayerRendererAccessor)this.equipmentRenderer).layerTextureLookup().apply(new EquipmentLayerRenderer.LayerTextureKey(layerType, layer));
                VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(vertexConsumers, RenderType.armorCutoutNoCull(normalArmorResource), hasGlint);
                modelPart.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, k);
                hasGlint = false;
            }
        }

        ArmorTrim trim = itemStack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlasSprite textureAtlasSprite = ((EquipmentLayerRendererAccessor)equipmentRenderer).trimSpriteLookup()
                    .apply(new EquipmentLayerRenderer.TrimSpriteKey(trim, layerType, location.get()));

            VertexConsumer trimConsumer = textureAtlasSprite.wrap(vertexConsumers.getBuffer(Sheets.armorTrimsSheet(false)));
            modelPart.render(poseStack, trimConsumer, light, OverlayTexture.NO_OVERLAY, -1);
        }

        if (hasGlint) {
            modelPart.render(poseStack, vertexConsumers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
