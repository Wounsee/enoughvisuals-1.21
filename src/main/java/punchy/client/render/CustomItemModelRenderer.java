package punchy.client.render;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.class_1047;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_630;
import net.minecraft.class_746;
import net.minecraft.class_753;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import net.minecraft.class_7923;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.model.ItemModelDefinition;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.client.modelparts.ModelPartsSystem;
import punchy.client.modelparts.util.ModelPartMath;
import punchy.util.ResourceLocationCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/CustomItemModelRenderer.class */
public final class CustomItemModelRenderer {
    private static final class_2960 ITEM_ATLAS_ID = ResourceLocationCompat.fromNamespaceAndPath("minecraft", "items");
    private static final class_2960 ITEM_ATLAS_TEXTURE = ResourceLocationCompat.fromNamespaceAndPath("minecraft", "textures/atlas/items.png");
    private static final float MODEL_UNIT = 0.0625f;
    private static final int MAX_MODEL_EMISSION = 15;

    private CustomItemModelRenderer() {
    }

    public static boolean tryRender(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int packedLight, float partialTicks) {
        return tryRender(stack, player, arm, poseStack, buffer, packedLight, partialTicks, true);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public static boolean tryRender(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int packedLight, float partialTicks, boolean applyDisplay) throws MatchException {
        class_2960 modelId;
        ItemModelDefinition model;
        if (stack == null || stack.method_7960() || player == null || arm == null || (modelId = ModelPartsSystem.get().resolveItemModelId(stack, player)) == null || (model = ModelPartsSystem.get().getItemModel(modelId)) == null || model.getElements().isEmpty()) {
            return false;
        }
        ItemModelDefinition.Pose pose = model.getPose(arm);
        ItemAnimationManager.applyToParts(arm, pose.getParts(), partialTicks);
        poseStack.method_22903();
        poseStack.method_22905(MODEL_UNIT, MODEL_UNIT, MODEL_UNIT);
        if (applyDisplay) {
            applyDisplayTransform(model, arm, poseStack);
        }
        poseStack.method_46416(-0.5f, -0.5f, -0.5f);
        Map<String, Matrix4f> particleBoneMatrices = new HashMap<>();
        Map<String, Matrix4f> renderBoneTransforms = collectRenderBoneTransforms(pose, particleBoneMatrices, poseStack.method_23760().method_23761());
        ModelPartsParticleSystem.setRenderRoot(arm, poseStack.method_23760().method_23761());
        ModelPartsParticleSystem.emitDefinitionForRenderBones(class_310.method_1551(), arm, renderBoneTransforms, ToolTuning.getParticleEmitters(stack));
        ModelPartsParticleSystem.emitPendingForRenderBones(class_310.method_1551(), arm, renderBoneTransforms);
        ModelPartsParticleSystem.renderForArm(class_310.method_1551(), arm, buffer, poseStack, packedLight);
        renderElements(model, pose, poseStack, buffer, packedLight);
        poseStack.method_22909();
        return true;
    }

    private static Map<String, Matrix4f> collectRenderBoneTransforms(ItemModelDefinition.Pose pose, Map<String, Matrix4f> cache, Matrix4f rootRenderMatrix) {
        Map<String, Matrix4f> out = new HashMap<>();
        if (pose == null || rootRenderMatrix == null) {
            return out;
        }
        for (String boneName : pose.getParts().keySet()) {
            Matrix4f boneMatrix = computeWorldMatrix(boneName, pose, cache);
            if (boneMatrix != null) {
                Matrix4f finalMatrix = new Matrix4f(rootRenderMatrix).mul(boneMatrix);
                out.put(boneName, finalMatrix);
            }
        }
        return out;
    }

    private static void renderElements(ItemModelDefinition model, ItemModelDefinition.Pose pose, class_4587 poseStack, class_4597 buffer, int packedLight) {
        EnumMap<class_2350, ItemModelDefinition.Face> faces;
        class_310 mc = class_310.method_1551();
        if (mc == null) {
            return;
        }
        try {
            class_1059 atlas = mc.method_1554().method_24153(ITEM_ATLAS_ID);
            class_1058 missing = atlas.method_4608(class_1047.method_4539());
            Map<String, Matrix4f> boneMatrices = new HashMap<>();
            for (ItemModelDefinition.Element element : model.getElements()) {
                Matrix4f boneMatrix = computeWorldMatrix(element.getBoneName(), pose, boneMatrices);
                if (boneMatrix != null && (faces = element.getFaces()) != null && !faces.isEmpty()) {
                    poseStack.method_22903();
                    poseStack.method_23760().method_23761().mul(boneMatrix);
                    poseStack.method_23760().method_23762().mul(new Matrix3f(boneMatrix));
                    int elementPackedLight = applyEmission(packedLight, element.getLightEmission());
                    Iterator it = faces.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<class_2350, ItemModelDefinition.Face> entry = (Map.Entry) it.next();
                        ItemModelDefinition.Face face = entry.getValue();
                        if (face != null) {
                            class_2960 spriteId = resolveTexture(model, face.getTextureRef());
                            class_1058 sprite = spriteId != null ? atlas.method_4608(spriteId) : missing;
                            if (sprite != null && sprite != missing) {
                                class_4588 consumer = buffer.getBuffer(class_1921.method_23580(ITEM_ATLAS_TEXTURE));
                                emitFace(poseStack.method_23760(), consumer, entry.getKey(), element.getFrom(), element.getTo(), face, sprite, elementPackedLight, model.getTextureWidth(), model.getTextureHeight());
                            }
                        }
                    }
                    poseStack.method_22909();
                }
            }
        } catch (NullPointerException e) {
        }
    }

    private static int applyEmission(int packedLight, int emission) {
        if (emission <= 0) {
            return packedLight;
        }
        int normalized = Math.round(Math.min(emission, MAX_MODEL_EMISSION) * 1.0f);
        int sky = class_765.method_24187(packedLight);
        int block = class_765.method_24186(packedLight);
        int outSky = Math.max(sky, normalized);
        int outBlock = Math.max(block, normalized);
        return class_765.method_23687(outBlock, outSky);
    }

    private static Matrix4f computeWorldMatrix(String boneName, ItemModelDefinition.Pose pose, Map<String, Matrix4f> cache) {
        Matrix4f world;
        Matrix4f parentMatrix;
        if (boneName == null || pose == null) {
            return null;
        }
        Matrix4f cached = cache.get(boneName);
        if (cached != null) {
            return cached;
        }
        Map<String, class_630> parts = pose.getParts();
        class_630 part = parts.get(boneName);
        if (part == null) {
            return null;
        }
        Matrix4f local = ModelPartMath.getLocalMatrix(part);
        String parent = pose.getParentByBone().get(boneName);
        if (parent == null || (parentMatrix = computeWorldMatrix(parent, pose, cache)) == null) {
            world = local;
        } else {
            world = new Matrix4f(parentMatrix).mul(local);
        }
        cache.put(boneName, world);
        return world;
    }

    private static void applyDisplayTransform(ItemModelDefinition model, class_1306 arm, class_4587 poseStack) {
        ItemModelDefinition.DisplayTransform transform = model.getDisplayTransform(arm);
        if (transform == null) {
            return;
        }
        Vector3f translation = transform.getTranslation();
        Vector3f rotation = transform.getRotation();
        Vector3f scale = transform.getScale();
        float tx = translation.x * MODEL_UNIT;
        float ty = translation.y * MODEL_UNIT;
        float tz = translation.z * MODEL_UNIT;
        float rx = rotation.x;
        float ry = rotation.y;
        float rz = rotation.z;
        if (arm == class_1306.field_6182) {
            tx = -tx;
            ry = -ry;
            rz = -rz;
        }
        poseStack.method_46416(tx, ty, tz);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(rx));
        poseStack.method_22907(class_7833.field_40716.rotationDegrees(ry));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(rz));
        poseStack.method_22905(scale.x, scale.y, scale.z);
    }

    private static class_2960 resolveTargetId(class_1799 stack) {
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId == null) {
            return null;
        }
        return ResourceLocationCompat.fromNamespaceAndPath(itemId.method_12836(), "item/" + itemId.method_12832());
    }

    private static class_2960 resolveTexture(ItemModelDefinition model, String ref) {
        if (model == null || ref == null || ref.isBlank()) {
            return null;
        }
        String lookup = ref;
        Map<String, String> textures = model.getTextures();
        for (int guard = 0; lookup.startsWith("#") && guard < 6; guard++) {
            String key = lookup.substring(1);
            String next = textures.get(key);
            if (next == null) {
                break;
            }
            lookup = next;
        }
        class_2960 id = class_2960.method_12829(lookup);
        if (id != null) {
            return id;
        }
        return ResourceLocationCompat.fromNamespaceAndPath(model.getNamespace(), lookup);
    }

    private static void emitFace(class_4587.class_4665 pose, class_4588 consumer, class_2350 direction, Vector3f from, Vector3f to, ItemModelDefinition.Face face, class_1058 sprite, int packedLight, int textureWidth, int textureHeight) {
        float u0 = face.getU0();
        float u1 = face.getU1();
        float v0 = face.getV0();
        float v1 = face.getV1();
        float maxUv = Math.max(Math.max(Math.abs(u0), Math.abs(u1)), Math.max(Math.abs(v0), Math.abs(v1)));
        float scaleU = (maxUv <= 16.0f || textureWidth <= 0) ? 1.0f : 16.0f / textureWidth;
        float scaleV = (maxUv <= 16.0f || textureHeight <= 0) ? 1.0f : 16.0f / textureHeight;
        class_753 faceInfo = class_753.method_3163(direction);
        float nx = direction.method_10148();
        float ny = direction.method_10164();
        float nz = direction.method_10165();
        for (int i = 0; i < 4; i++) {
            Vector3f vertex = selectVertex(faceInfo.method_3162(i), from, to);
            float u = getRotatedU(i, u0 * scaleU, u1 * scaleU, face.getRotation()) / 16.0f;
            float v = getRotatedV(i, v0 * scaleV, v1 * scaleV, face.getRotation()) / 16.0f;
            addVertex(pose, consumer, vertex.x, vertex.y, vertex.z, sprite.method_4580(u), sprite.method_4570(v), packedLight, nx, ny, nz);
        }
    }

    private static float getRotatedU(int vertexIndex, float minU, float maxU, int rotation) {
        int rotated = rotateVertexIndex(vertexIndex, rotation);
        return (rotated == 0 || rotated == 1) ? minU : maxU;
    }

    private static float getRotatedV(int vertexIndex, float minV, float maxV, int rotation) {
        int rotated = rotateVertexIndex(vertexIndex, rotation);
        return (rotated == 0 || rotated == 3) ? minV : maxV;
    }

    private static int rotateVertexIndex(int vertexIndex, int rotation) {
        int normalized = rotation % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        int steps = (normalized / 90) % 4;
        return (vertexIndex + steps) % 4;
    }

    private static void addVertex(class_4587.class_4665 pose, class_4588 consumer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz) {
        consumer.method_22918(pose.method_23761(), x, y, z).method_1336(255, 255, 255, 255).method_22913(u, v).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, nx, ny, nz);
    }

    private static Vector3f selectVertex(class_753.class_755 info, Vector3f from, Vector3f to) {
        float x = info.field_3975 == class_753.class_754.field_3967 ? from.x : to.x;
        float y = info.field_3974 == class_753.class_754.field_3968 ? from.y : to.y;
        float z = info.field_3973 == class_753.class_754.field_3969 ? from.z : to.z;
        return new Vector3f(x, y, z);
    }
}
