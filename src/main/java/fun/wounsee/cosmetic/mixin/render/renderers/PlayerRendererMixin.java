package fun.wounsee.cosmetic.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.avatar.Badges;
import fun.wounsee.cosmetic.compat.SimpleVCCompat;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.ducks.EntityRendererAccessor;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.lua.api.ClientAPI;
import fun.wounsee.cosmetic.lua.api.nameplate.EntityNameplateCustomization;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaPart;
import fun.wounsee.cosmetic.permissions.Permissions;
import fun.wounsee.cosmetic.utils.RenderUtils;
import fun.wounsee.cosmetic.utils.TextUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> implements EntityRendererAccessor {

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel entityModel, float shadowRadius) {
        super(context, entityModel, shadowRadius);
    }

    @Unique
    private Avatar avatar;

    @Unique
    boolean isNameRendering, hasScore;

    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", ordinal = 1))
    private void enableModifyPlayerName(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        // render name
        CosmeticMod.popPushProfiler("name");
        isNameRendering = true;
    }

    @Override
    public boolean figura$isRenderingName() {
        return isNameRendering;
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "TAIL"))
    private void disableModifyPlayerName(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        isNameRendering = false;
    }


    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", ordinal = 0))
    private void setHasScore(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        hasScore = playerRenderState.scoreText != null;
    }


    @Override
    public boolean figura$hasScore() {
        return hasScore;
    }

    @ModifyArg(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", ordinal = 1))
    private Component modifyPlayerNameText(Component text, @Local(argsOnly = true) PlayerRenderState player) {
        int config = Configs.ENTITY_NAMEPLATE.value;
        if (config == 0 || AvatarManager.panic)
            return text;

        // text
        Avatar avatar = AvatarManager.getAvatar(player);
        EntityNameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.ENTITY;

        // customization boolean, which also is the permission check
        boolean hasCustom = custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;

        Component name = Component.literal(player.name);
        CosmeticMod.popPushProfiler("text");

        Component replacement = hasCustom && custom.getJson() != null ? custom.getJson().copy() : name;

        // name
        replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", name);

        // badges
        CosmeticMod.popPushProfiler("badges");
        if (Minecraft.getInstance().level.getEntity(player.id) != null) { // null while dead
			replacement = Badges.appendBadges(replacement, Minecraft.getInstance().level.getEntity(player.id).getUUID(), config > 1);
		}

        CosmeticMod.popPushProfiler("applyName");
        text = TextUtils.replaceInText(text, "\\b" + Pattern.quote(player.name) + "\\b", replacement);

        return text;
    }

    // Push for scoreboard rendering
    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    private void pushProfilerForRender(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        CosmeticMod.popPushProfiler("render");
        CosmeticMod.pushProfiler("scoreboard");
    }

    // Pop the profiler after everything's done
    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "TAIL"))
    private void popProfiler(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        CosmeticMod.popProfiler(5);
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void renderNameTag(PlayerRenderState playerRenderState, Component component, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        // return on config or high entity distance
        int config = Configs.ENTITY_NAMEPLATE.value;
        Entity entity = Minecraft.getInstance().level.getEntity(playerRenderState.id);

        if (config == 0 || AvatarManager.panic || !(entity instanceof Player player) || this.entityRenderDispatcher.distanceToSqr(player) > 4096)
            return;

        // get customizations
        Avatar avatar = AvatarManager.getAvatarForPlayer(player.getUUID());
        EntityNameplateCustomization custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.ENTITY;

        // customization boolean, which also is the permission check
        boolean hasCustom = custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;
        if (custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 0) {
            avatar.noPermissions.add(Permissions.NAMEPLATE_EDIT);
        } else if (avatar != null) {
            avatar.noPermissions.remove(Permissions.NAMEPLATE_EDIT);
        }

        // enabled
        if (hasCustom && !custom.visible) {
            ci.cancel();
            return;
        }

        // If the user has an avatar equipped, figura nameplate rendering will be enabled so the profiler is pushed
        if (hasCustom) {
            CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);
            CosmeticMod.pushProfiler(player.getName().getString());
            CosmeticMod.pushProfiler("nameplate");
        }
    }



    @Inject(at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), method = "renderHand")
    private void onRenderHand(PoseStack matrices, MultiBufferSource vertexConsumers, int light, ResourceLocation id, ModelPart arm, boolean bl, CallbackInfo ci) {
        avatar = AvatarManager.getAvatarForPlayer(Minecraft.getInstance().player.getUUID());
        if (avatar != null && avatar.luaRuntime != null) {
            VanillaPart part = avatar.luaRuntime.vanilla_model.PLAYER;
            PlayerModel model = this.getModel();

            part.save(model);

            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 1) {
                part.preTransform(model);
                part.posTransform(model);
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "renderHand")
    private void postRenderHand(PoseStack stack, MultiBufferSource multiBufferSource, int light, ResourceLocation id, ModelPart arm, boolean bl, CallbackInfo ci) {
        if (avatar == null)
            return;

        float delta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        avatar.firstPersonRender(stack, multiBufferSource, Minecraft.getInstance().player, (PlayerRenderer) (Object) this, arm, light, delta);

        if (avatar.luaRuntime != null)
            avatar.luaRuntime.vanilla_model.PLAYER.restore(this.getModel());

        avatar = null;
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), cancellable = true)
    private void setupRotations(PlayerRenderState playerRenderState, PoseStack matrices, float f, float g, CallbackInfo cir) {
        Avatar avatar = AvatarManager.getAvatar(playerRenderState);
        if (RenderUtils.vanillaModelAndScript(avatar) && !avatar.luaRuntime.renderer.getRootRotationAllowed()) {
            cir.cancel();
        }
    }
}
