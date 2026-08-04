package punchy.client.render;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.class_10444;
import net.minecraft.class_1058;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1921;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_777;
import net.minecraft.class_7923;
import net.minecraft.class_804;
import org.joml.Vector3f;
import punchy.client.render.AxolotlBucketRenderer;
import punchy.mixin.client.accessor.ItemStackRenderStateAccessor;
import punchy.mixin.client.accessor.ItemStackRenderStateLayerAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/CreatureBucketAnimator.class */
public final class CreatureBucketAnimator {
    private CreatureBucketAnimator() {
    }

    public static void apply(class_1799 stack, class_10444 renderState, class_1268 hand) {
        ItemStackRenderStateAccessor accessor;
        int layerCount;
        class_1058 sprite;
        if (!shouldAnimate(stack) || (layerCount = (accessor = (ItemStackRenderStateAccessor) renderState).punchy$getActiveLayerCount()) <= 0) {
            return;
        }
        ItemStackRenderStateLayerAccessor[] itemStackRenderStateLayerAccessorArrPunchy$getLayers = accessor.punchy$getLayers();
        List<ExtractedLayer> extracted = new ArrayList<>();
        for (int i = 0; i < layerCount && i < itemStackRenderStateLayerAccessorArrPunchy$getLayers.length; i++) {
            ItemStackRenderStateLayerAccessor itemStackRenderStateLayerAccessor = itemStackRenderStateLayerAccessorArrPunchy$getLayers[i];
            if (itemStackRenderStateLayerAccessor != null) {
                List<class_777> quads = itemStackRenderStateLayerAccessor.method_67997();
                if (!quads.isEmpty()) {
                    List<class_777> bodyQuads = new ArrayList<>();
                    Iterator<class_777> it = quads.iterator();
                    while (it.hasNext()) {
                        class_777 quad = it.next();
                        if (quad != null && (sprite = quad.comp_3724()) != null && sprite.method_45851() != null && isCreatureTexture(stack, sprite.method_45851().method_45816())) {
                            bodyQuads.add(quad);
                            it.remove();
                        }
                    }
                    if (!bodyQuads.isEmpty()) {
                        ItemStackRenderStateLayerAccessor layerAccessor = itemStackRenderStateLayerAccessor;
                        class_1921 renderType = layerAccessor.punchy$getRenderType();
                        if (renderType == null) {
                            quads.addAll(bodyQuads);
                        } else {
                            class_10444.class_10445 foilType = layerAccessor.punchy$getFoilType();
                            class_804 transform = layerAccessor.punchy$getTransform();
                            int[] tintLayers = layerAccessor.punchy$getTintLayers();
                            int[] tintCopy = (tintLayers == null || tintLayers.length == 0) ? new int[0] : (int[]) tintLayers.clone();
                            class_1058 particle = layerAccessor.punchy$getParticleIcon();
                            Supplier<Vector3f[]> extents = layerAccessor.punchy$getExtents();
                            boolean usesBlockLight = layerAccessor.punchy$usesBlockLight();
                            extracted.add(new ExtractedLayer(List.copyOf(bodyQuads), renderType, foilType, transform, tintCopy, particle, extents, usesBlockLight));
                        }
                    }
                }
            }
        }
        if (extracted.isEmpty()) {
            return;
        }
        for (ExtractedLayer entry : extracted) {
            class_10444.class_10446 target = renderState.method_65601();
            target.method_67992(entry.renderType());
            target.method_65615(entry.foilType());
            target.method_67993(entry.transform());
            target.method_67996(entry.usesBlockLight());
            if (entry.particle() != null) {
                target.method_67994(entry.particle());
            }
            if (entry.extents() != null) {
                target.method_67995(entry.extents());
            }
            int[] tint = entry.tintLayers();
            int[] dst = target.method_65613(tint.length);
            if (tint.length > 0) {
                System.arraycopy(tint, 0, dst, 0, tint.length);
            }
            target.method_65617(AxolotlBucketRenderer.INSTANCE, new AxolotlBucketRenderer.LayerData(hand, entry.renderType(), entry.foilType(), dst, entry.quads(), entry.usesBlockLight()));
        }
    }

    public static boolean shouldAnimate(class_1799 stack) {
        if (stack == null || stack.method_7960() || !isCreatureBucket(stack)) {
            return false;
        }
        return isResourcePackItemDefinition(stack);
    }

    private static boolean isResourcePackItemDefinition(class_1799 stack) {
        class_2960 itemId;
        String packId;
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.method_1478() == null || stack == null || stack.method_7960() || (itemId = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return false;
        }
        class_2960 defId = class_2960.method_60655(itemId.method_12836(), "items/" + itemId.method_12832() + ".json");
        List<class_3298> resources = mc.method_1478().method_14489(defId);
        if (resources == null || resources.isEmpty()) {
            return false;
        }
        for (class_3298 resource : resources) {
            if (resource != null && (packId = resource.method_14480()) != null && !"vanilla".equals(packId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCreatureTexture(class_1799 stack, class_2960 name) {
        String path;
        if (name == null || (path = name.method_12832()) == null) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("item/") || normalized.contains("_water") || normalized.contains("water")) {
            return false;
        }
        boolean hasCreatureToken = normalized.contains("axolotl") || normalized.contains("axo") || normalized.contains("tropical_fish") || normalized.contains("tropical") || normalized.contains("pufferfish") || normalized.contains("puffer") || normalized.contains("cod") || normalized.contains("salmon") || normalized.contains("tadpole");
        if (normalized.contains("bucket")) {
            return hasCreatureToken;
        }
        if (normalized.contains("balde") || normalized.contains("baldes")) {
            return hasCreatureToken;
        }
        if (hasCreatureToken) {
            return true;
        }
        return stack != null && isCreatureBucket(stack);
    }

    private static boolean isCreatureBucket(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_31574(class_1802.field_28354) || stack.method_31574(class_1802.field_8478) || stack.method_31574(class_1802.field_8108) || stack.method_31574(class_1802.field_8666) || stack.method_31574(class_1802.field_8714) || stack.method_31574(class_1802.field_37533);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/CreatureBucketAnimator$ExtractedLayer.class */
    private static final class ExtractedLayer extends Record {
        private final List<class_777> quads;
        private final class_1921 renderType;
        private final class_10444.class_10445 foilType;
        private final class_804 transform;
        private final int[] tintLayers;
        private final class_1058 particle;
        private final Supplier<Vector3f[]> extents;
        private final boolean usesBlockLight;

        private ExtractedLayer(List<class_777> quads, class_1921 renderType, class_10444.class_10445 foilType, class_804 transform, int[] tintLayers, class_1058 particle, Supplier<Vector3f[]> extents, boolean usesBlockLight) {
            this.quads = quads;
            this.renderType = renderType;
            this.foilType = foilType;
            this.transform = transform;
            this.tintLayers = tintLayers;
            this.particle = particle;
            this.extents = extents;
            this.usesBlockLight = usesBlockLight;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ExtractedLayer.class), ExtractedLayer.class, "quads;renderType;foilType;transform;tintLayers;particle;extents;usesBlockLight", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->transform:Lnet/minecraft/class_804;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->tintLayers:[I", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->particle:Lnet/minecraft/class_1058;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->extents:Ljava/util/function/Supplier;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->usesBlockLight:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ExtractedLayer.class), ExtractedLayer.class, "quads;renderType;foilType;transform;tintLayers;particle;extents;usesBlockLight", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->transform:Lnet/minecraft/class_804;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->tintLayers:[I", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->particle:Lnet/minecraft/class_1058;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->extents:Ljava/util/function/Supplier;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->usesBlockLight:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ExtractedLayer.class, Object.class), ExtractedLayer.class, "quads;renderType;foilType;transform;tintLayers;particle;extents;usesBlockLight", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->quads:Ljava/util/List;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->renderType:Lnet/minecraft/class_1921;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->foilType:Lnet/minecraft/class_10444$class_10445;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->transform:Lnet/minecraft/class_804;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->tintLayers:[I", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->particle:Lnet/minecraft/class_1058;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->extents:Ljava/util/function/Supplier;", "FIELD:Lpunchy/client/render/CreatureBucketAnimator$ExtractedLayer;->usesBlockLight:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public List<class_777> quads() {
            return this.quads;
        }

        public class_1921 renderType() {
            return this.renderType;
        }

        public class_10444.class_10445 foilType() {
            return this.foilType;
        }

        public class_804 transform() {
            return this.transform;
        }

        public int[] tintLayers() {
            return this.tintLayers;
        }

        public class_1058 particle() {
            return this.particle;
        }

        public Supplier<Vector3f[]> extents() {
            return this.extents;
        }

        public boolean usesBlockLight() {
            return this.usesBlockLight;
        }
    }
}
