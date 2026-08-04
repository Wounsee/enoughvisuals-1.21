package punchy.client.render;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.class_10444;
import net.minecraft.class_10515;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_746;
import net.minecraft.class_777;
import net.minecraft.class_7833;
import net.minecraft.class_811;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/AxolotlBucketRenderer.class */
public final class AxolotlBucketRenderer implements class_10515<LayerData> {
    static final AxolotlBucketRenderer INSTANCE = new AxolotlBucketRenderer();
    private static final EnumMap<class_1268, AxolotlPhysics> PHYSICS = new EnumMap<>(class_1268.class);
    private static final float AXOLOTL_ROLL_SCALE = 1.4f;

    private AxolotlBucketRenderer() {
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/AxolotlBucketRenderer$LayerData.class */
    public static final class LayerData extends Record {
        private final class_1268 hand;
        private final class_1921 renderType;
        private final class_10444.class_10445 foilType;
        private final int[] tintLayers;
        private final List<class_777> quads;
        private final boolean usesBlockLight;

        public LayerData(class_1268 hand, class_1921 renderType, class_10444.class_10445 foilType, int[] tintLayers, List<class_777> quads, boolean usesBlockLight) {
            this.hand = hand;
            this.renderType = renderType;
            this.foilType = foilType;
            this.tintLayers = tintLayers;
            this.quads = quads;
            this.usesBlockLight = usesBlockLight;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, LayerData.class), LayerData.class, "hand;renderType;foilType;tintLayers;quads;usesBlockLight", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->tintLayers:[I", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->usesBlockLight:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, LayerData.class), LayerData.class, "hand;renderType;foilType;tintLayers;quads;usesBlockLight", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->tintLayers:[I", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->usesBlockLight:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, LayerData.class, Object.class), LayerData.class, "hand;renderType;foilType;tintLayers;quads;usesBlockLight", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->hand:Lnet/minecraft/class_1268;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->tintLayers:[I", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/AxolotlBucketRenderer$LayerData;->usesBlockLight:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_1268 hand() {
            return this.hand;
        }

        public class_1921 renderType() {
            return this.renderType;
        }

        public class_10444.class_10445 foilType() {
            return this.foilType;
        }

        public int[] tintLayers() {
            return this.tintLayers;
        }

        public List<class_777> quads() {
            return this.quads;
        }

        public boolean usesBlockLight() {
            return this.usesBlockLight;
        }
    }

    /* JADX INFO: renamed from: render, reason: merged with bridge method [inline-methods] */
    public void method_65694(@Nullable LayerData data, class_811 context, class_4587 poseStack, class_4597 buffer, int packedLight, int packedOverlay, boolean hasFoilType) {
        if (data == null || data.renderType == null || data.quads == null || data.quads.isEmpty()) {
            return;
        }
        class_1268 hand = data.hand == null ? class_1268.field_5808 : data.hand;
        boolean left = hand == class_1268.field_5810;
        AxolotlPhysics physics = stateFor(hand);
        class_746 player = class_310.method_1551().field_1724;
        physics.update(player);
        float roll = physics.getRoll() * AXOLOTL_ROLL_SCALE;
        float lift = physics.getLift();
        float rollLift = Math.abs(roll) * 0.15f;
        poseStack.method_22903();
        if (lift != 0.0f || rollLift != 0.0f) {
            poseStack.method_46416(0.0f, lift + rollLift, 0.0f);
        }
        if (left) {
            poseStack.method_22907(class_7833.field_40716.rotation(3.1415927f));
        }
        if (roll != 0.0f) {
            poseStack.method_22907(class_7833.field_40718.rotation(roll));
        }
        if (left) {
            poseStack.method_22907(class_7833.field_40715.rotation(3.1415927f));
        }
        class_4588 vertexConsumer = buffer.getBuffer(data.renderType);
        for (class_777 quad : data.quads) {
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            if (quad.method_3360()) {
                int tintIndex = quad.comp_3722();
                if (data.tintLayers != null && tintIndex >= 0 && tintIndex < data.tintLayers.length) {
                    int color = data.tintLayers[tintIndex];
                    r = ((color >> 16) & 255) / 255.0f;
                    g = ((color >> 8) & 255) / 255.0f;
                    b = (color & 255) / 255.0f;
                }
            }
            vertexConsumer.method_22919(poseStack.method_23760(), quad, r, g, b, 1.0f, packedLight, packedOverlay);
        }
        poseStack.method_22909();
    }

    @Nullable
    /* JADX INFO: renamed from: extractArgument, reason: merged with bridge method [inline-methods] */
    public LayerData method_65695(class_1799 stack) {
        return null;
    }

    private static AxolotlPhysics stateFor(class_1268 hand) {
        class_1268 resolved = hand == null ? class_1268.field_5808 : hand;
        return (AxolotlPhysics) PHYSICS.computeIfAbsent(resolved, h -> {
            return new AxolotlPhysics();
        });
    }
}
