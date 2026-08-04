package punchy.client.modelparts;

import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_746;
import punchy.client.model.ItemModelDefinition;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/ModelPartsSystem.class */
public final class ModelPartsSystem {
    private static final ModelPartsSystem INSTANCE = new ModelPartsSystem();
    private final ResourceStateMachine resources = new ResourceStateMachine();
    private final SelectionStateMachine selection = new SelectionStateMachine();
    private final AnimationStateMachine animation = new AnimationStateMachine();
    private final RenderStateMachine render = new RenderStateMachine();

    private ModelPartsSystem() {
    }

    public static ModelPartsSystem get() {
        return INSTANCE;
    }

    public void onResourceReload(class_3300 manager) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return;
        }
        this.resources.load(manager);
        this.selection.onResourcesReload();
    }

    public void tick(class_310 client) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return;
        }
        this.animation.tick(client);
    }

    public class_2960 resolveItemModelId(class_1799 stack, class_1657 player) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return null;
        }
        return this.selection.resolveModelId(stack, player);
    }

    public ItemModelDefinition getItemModel(class_2960 modelId) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return null;
        }
        return this.selection.getItemModel(modelId);
    }

    public boolean tryRenderItem(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int packedLight, float partialTicks, boolean applyDisplay) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return false;
        }
        return this.render.tryRenderItem(stack, player, arm, poseStack, buffer, packedLight, partialTicks, applyDisplay);
    }

    public int getModelWorldEmission(class_1657 player, class_1799 stack) {
        if (PunchyConfig.disableResourcePackModelParts()) {
            return 0;
        }
        return this.selection.getModelWorldEmission(player, stack);
    }
}
