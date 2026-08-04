package fun.wounsee.cosmetic.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.ducks.FiguraProjectileRenderStateExtension;
import fun.wounsee.cosmetic.lua.api.entity.EntityAPI;
import fun.wounsee.cosmetic.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArrowRenderer.class)
public abstract class ArrowRendererMixin<T extends AbstractArrow, S extends ArrowRenderState> extends EntityRenderer<T, S> {

    protected ArrowRendererMixin(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ArrowModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"), method = "render(Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", cancellable = true)
    private void render(S arrowRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        Integer id = ((FiguraEntityRenderStateExtension)arrowRenderState).figura$getEntityId();
        if (id == null)
            return;

        Projectile arrow = (Projectile) AvatarManager.ENTITY_CACHE.computeIfAbsent(id, (id2) -> Minecraft.getInstance().level.getEntity(id2));
        if (arrow == null)
            return;

        float tickDelta = ((FiguraProjectileRenderStateExtension)arrowRenderState).figura$getTickDelta();
        Entity owner = arrow.getOwner();

        Avatar avatar = AvatarManager.getAvatar(owner);
        if (avatar == null || avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) == 0)
            return;

        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);
        CosmeticMod.pushProfiler(avatar);
        CosmeticMod.pushProfiler("arrowRender");

        CosmeticMod.pushProfiler("event");
        boolean bool = avatar.arrowRenderEvent(tickDelta, EntityAPI.wrap(arrow));

        CosmeticMod.popPushProfiler("render");
        if (bool || avatar.renderArrow(poseStack, multiBufferSource, tickDelta, light)) {
            poseStack.popPose();
            ci.cancel();
        }

        CosmeticMod.popProfiler(4);
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState(Lnet/minecraft/world/entity/projectile/AbstractArrow;Lnet/minecraft/client/renderer/entity/state/ArrowRenderState;F)V")
    void appendFiguraProperties(T abstractArrow, S arrowRenderState, float f, CallbackInfo ci) {
        ((FiguraProjectileRenderStateExtension)arrowRenderState).figura$setTickDelta(f);
    }
}
