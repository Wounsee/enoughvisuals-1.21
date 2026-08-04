package fun.wounsee.cosmetic.compat;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import fun.wounsee.cosmetic.compat.wrappers.ClassWrapper;
import fun.wounsee.cosmetic.compat.wrappers.FieldWrapper;
import fun.wounsee.cosmetic.compat.wrappers.MethodWrapper;
import java.util.Map;

public class GeckoLibCompat {

    // For newer versions of GeckoLib
    private static ClassWrapper GLRenderUtil;
    private static MethodWrapper getGeoModelForArmor;

    // For older versions of GeckoLib
    private static ClassWrapper GLGeoArmorRenderer;
    private static FieldWrapper renderers;


    public static void init() {
        // Modern
        GLRenderUtil = new ClassWrapper("software.bernie.geckolib.util.RenderUtil");
        getGeoModelForArmor = GLRenderUtil.getMethod("getGeoModelForArmor", ItemStack.class, EquipmentSlot.class, EquipmentClientInfo.LayerType.class);

        // Legacy
        GLGeoArmorRenderer = new ClassWrapper("software.bernie.geckolib3.renderers.geo.GeoArmorRenderer");
        renderers = GLGeoArmorRenderer.getField("renderers");
    }

    public static boolean armorHasCustomModel(ItemStack stack, EquipmentSlot slot, EquipmentClientInfo.LayerType layerType) {
        if (GLRenderUtil.isLoaded) {
            if (getGeoModelForArmor.exists()) {
                return getGeoModelForArmor.invoke(null, stack, slot, layerType) != null;
            }
            return false;
        }

        if (GLGeoArmorRenderer.isLoaded) {
            if (renderers.exists()) {
                if (renderers.getValue(null) instanceof Map<?, ?> map) {
                    return map.containsKey(stack.getItem().getClass());
                }
                renderers.markErrored();
            }

            return false;
        }


        return false;
    }
}
