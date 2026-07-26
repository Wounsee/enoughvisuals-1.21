package fun.enoughvisuals.mixins.minecraft.client.render.entity;

import fun.enoughvisuals.systems.modules.modules.visual.TotemTracker;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Text enough$appendTotemSuffix(Text original) {
        TotemTracker tracker = TotemTracker.getInstance();
        if (tracker == null || !tracker.isEnabled() || !tracker.nametagSuffix.get()) {
            return original;
        }
        if (original == null) {
            return original;
        }
        String displayed = original.getString();
        int count = tracker.getLossCountFromText(displayed);
        if (count <= 0) {
            return original;
        }
        MutableText decorated = original.copy();
        decorated.append(Text.literal(" | ").formatted(Formatting.GRAY));
        decorated.append(Text.literal("-" + count).formatted(Formatting.RED));
        return decorated;
    }
}
