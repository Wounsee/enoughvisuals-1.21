package punchy.client.modelparts;

import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import punchy.client.model.ItemModelDefinition;
import punchy.client.model.ResourcePackGeoRegistry;
import punchy.client.model.ResourcePackItemModelRegistry;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/modelparts/SelectionStateMachine.class */
final class SelectionStateMachine {
    SelectionStateMachine() {
    }

    void onResourcesReload() {
    }

    class_2960 resolveModelId(class_1799 stack, class_1657 player) {
        if (stack == null || stack.method_7960() || player == null) {
            return null;
        }
        return ResourcePackGeoRegistry.getModelForTarget(stack, player);
    }

    ItemModelDefinition getItemModel(class_2960 modelId) {
        return ResourcePackItemModelRegistry.getModel(modelId);
    }

    int getModelWorldEmission(class_1657 player, class_1799 stack) {
        class_2960 modelId;
        ItemModelDefinition model;
        if (player == null || stack == null || stack.method_7960() || (modelId = resolveModelId(stack, player)) == null || (model = getItemModel(modelId)) == null) {
            return 0;
        }
        int max = 0;
        for (ItemModelDefinition.Element element : model.getElements()) {
            if (element.hasWorldLight()) {
                max = Math.max(max, element.getLightEmission());
            }
        }
        return max;
    }
}
