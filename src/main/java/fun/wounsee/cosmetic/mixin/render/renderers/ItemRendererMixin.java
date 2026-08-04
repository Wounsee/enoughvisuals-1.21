package fun.wounsee.cosmetic.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import fun.wounsee.cosmetic.avatar.Avatar;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.ducks.FiguraItemRendererExtension;
import fun.wounsee.cosmetic.ducks.FiguraItemStackRenderStateExtension;
import fun.wounsee.cosmetic.lua.api.world.ItemStackAPI;
import fun.wounsee.cosmetic.lua.api.world.WorldAPI;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin implements FiguraItemRendererExtension {

    @Shadow @Final private ItemModelResolver resolver;

    @Shadow @Final private ItemStackRenderState scratchItemStackRenderState;

    @Inject(at = @At("HEAD"), method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V", cancellable = true)
    private void renderStatic(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, boolean leftHanded, PoseStack stack, MultiBufferSource buffer, Level world, int light, int overlay, int seed, CallbackInfo ci) {
        if (livingEntity == null || itemStack.isEmpty())
            return;

        Avatar avatar = AvatarManager.getAvatar(livingEntity);
        if (avatar == null)
            return;

        this.resolver.updateForTopItem(this.scratchItemStackRenderState, itemStack, itemDisplayContext, leftHanded, world, livingEntity, seed);
        ItemTransform transform = this.scratchItemStackRenderState.transform();

        if (avatar.itemRenderEvent(ItemStackAPI.verify(itemStack), itemDisplayContext.name(), FiguraVec3.fromVec3f(transform.translation), FiguraVec3.of(transform.rotation.z, transform.rotation.y, transform.rotation.x), FiguraVec3.fromVec3f(transform.scale), leftHanded, stack, buffer, light, overlay))
            ci.cancel();
    }

    @Unique
    public int figura$getModelComplexity(ItemStack stack, RandomSource randomSource) {
        this.resolver.updateForTopItem(this.scratchItemStackRenderState, stack, ItemDisplayContext.NONE, false, WorldAPI.getCurrentWorld(), null, 1);
        if (((FiguraItemStackRenderStateExtension)(this.scratchItemStackRenderState)).figura$getModel() != null)
            return ((FiguraItemStackRenderStateExtension)(this.scratchItemStackRenderState)).figura$getModel().getQuads(null, null, randomSource).size();
        return 20;
    }
}
