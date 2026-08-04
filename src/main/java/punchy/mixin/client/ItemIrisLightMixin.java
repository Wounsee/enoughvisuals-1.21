package punchy.mixin.client;

import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import punchy.client.modelparts.ModelPartsSystem;
import punchy.compat.iris.IrisItemLightProviderCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/ItemIrisLightMixin.class */
@Mixin({class_1792.class})
@Implements({@Interface(iface = IrisItemLightProviderCompat.class, prefix = "punchy$")})
public class ItemIrisLightMixin {
    private static final int MAX_MODEL_EMISSION = 15;
    private static final Vector3f DEFAULT_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);

    public int punchy$getLightEmission(class_1657 player, class_1799 stack) {
        int vanilla = getVanillaEmission(stack);
        int modelEmission = getModelWorldEmission(player, stack);
        return Math.max(vanilla, scaleEmission(modelEmission));
    }

    public Vector3f punchy$getLightColor(class_1657 player, class_1799 stack) {
        return DEFAULT_COLOR;
    }

    private static int getVanillaEmission(class_1799 stack) {
        if (stack == null) {
            return 0;
        }
        class_1747 class_1747VarMethod_7909 = stack.method_7909();
        if (class_1747VarMethod_7909 instanceof class_1747) {
            class_1747 blockItem = class_1747VarMethod_7909;
            return blockItem.method_7711().method_9564().method_26213();
        }
        return 0;
    }

    private static int getModelWorldEmission(class_1657 player, class_1799 stack) {
        if (player == null || stack == null) {
            return 0;
        }
        return ModelPartsSystem.get().getModelWorldEmission(player, stack);
    }

    private static int scaleEmission(int emission) {
        if (emission <= 0) {
            return 0;
        }
        int clamped = Math.min(emission, MAX_MODEL_EMISSION);
        return Math.round(clamped * 1.0f);
    }
}
