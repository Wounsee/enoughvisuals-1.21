package punchy.client.bedrockitems;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1044;
import net.minecraft.class_10442;
import net.minecraft.class_10444;
import net.minecraft.class_10799;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_4608;
import net.minecraft.class_5762;
import net.minecraft.class_630;
import net.minecraft.class_746;
import net.minecraft.class_7833;
import net.minecraft.class_804;
import net.minecraft.class_811;
import net.minecraft.class_9334;
import net.minecraft.class_9851;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import punchy.client.bedrockitems.BedrockAxolPhysicsStateMachine;
import punchy.client.bedrockitems.BedrockItemDefinition;
import punchy.client.bedrockitems.BedrockItemGeoModel;
import punchy.client.bedrockitems.BedrockPufferPhysicsStateMachine;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.client.modelparts.util.ModelPartMath;
import punchy.client.render.PendulumPhysics;
import punchy.client.render.ResourcePackItemTransformResolver;
import punchy.client.render.SlimeInHandPhysics;
import punchy.client.render.ToolTuning;
import punchy.client.render.UseAnimationSuppressor;
import punchy.config.PunchyTuningConfig;
import punchy.mixin.client.accessor.ItemStackRenderStateAccessor;
import punchy.mixin.client.accessor.ItemStackRenderStateLayerAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemRenderer.class */
final class BedrockItemRenderer {
    private static final float MODEL_UNIT = 0.0625f;
    private static final float EMISSIVE_BLUR_SCALE = 1.08f;
    private static final float EMISSIVE_BLUR_ALPHA = 0.4f;
    private static final float EMISSIVE_BLUR_COLOR = 0.6f;
    private static final float PUFFER_INCLINATION_SCALE = 1.19f;
    private static final float PUFFER_PITCH_BOOST = 1.3f;
    private static final EnumMap<class_1306, class_2960> LAST_RENDER_DEF_ID = new EnumMap<>(class_1306.class);
    private static final RenderPipeline EMISSIVE_OVERLAY_PIPELINE = createEmissiveOverlayPipeline(false);
    private static final RenderPipeline EMISSIVE_BLUR_OVERLAY_PIPELINE = createEmissiveOverlayPipeline(true);
    private static final Set<String> AXOL_PHYSICS_BONES = Set.of((Object[]) new String[]{"fish", "head", "top_gills", "left_gills", "right_gills", "body", "legs_front", "leg1", "leg2", "leg4", "leg5"});
    private static final Set<String> PUFFER_PHYSICS_BONES = Set.of("fish", "body", "right_fin", "left_fin", "fin_top", "tail", "eye", "fin_left", "fin_right", "spikes");
    private static final Map<EmissiveKey, class_1921> EMISSIVE_OVERLAY_TYPES = new HashMap();

    private BedrockItemRenderer() {
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    static boolean render(BedrockItemDefinition def, class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack, class_4597 buffer, int packedLight, float partialTicks, boolean applyDisplay) throws MatchException {
        class_2960 texture;
        if (def == null || stack == null || stack.method_7960() || player == null || arm == null) {
            return false;
        }
        class_2960 dynamicGeoId = resolveDynamicGeoId(stack, def);
        BedrockItemGeoModel geo = def.getGeoModel(dynamicGeoId);
        if (geo == null || geo.getRoot() == null || (texture = def.getTextureId()) == null) {
            return false;
        }
        class_2960 currentDefId = def.getId();
        class_2960 lastDefId = LAST_RENDER_DEF_ID.get(arm);
        if (arm != null) {
            if (lastDefId != null && currentDefId != null && !lastDefId.equals(currentDefId)) {
                ModelPartsParticleSystem.resetArm(arm);
            }
            LAST_RENDER_DEF_ID.put(arm, currentDefId);
        }
        geo.resetPose();
        BedrockItemAnimationManager.applyToParts(arm, geo.getParts(), partialTicks);
        poseStack.method_22903();
        Matrix4f internalTransform = new Matrix4f();
        poseStack.method_22905(MODEL_UNIT, MODEL_UNIT, MODEL_UNIT);
        internalTransform.scale(MODEL_UNIT, MODEL_UNIT, MODEL_UNIT);
        poseStack.method_46416(0.5f, 0.0f, 0.5f);
        internalTransform.translate(0.5f, 0.0f, 0.5f);
        if (applyDisplay) {
            applyDisplayTransform(def, stack, player, arm, poseStack);
            applyDisplayTransformMatrix(def, stack, player, arm, internalTransform);
        }
        class_1268 interactionHand = resolveInteractionHand(player, arm);
        BedrockItemAnimationManager.isAnimMirrorActive(arm);
        if (arm == class_1306.field_6182) {
            poseStack.method_22905(-1.0f, 1.0f, 1.0f);
            internalTransform.scale(-1.0f, 1.0f, 1.0f);
            poseStack.method_23760().method_23762().scale(-1.0f, 1.0f, 1.0f);
        }
        poseStack.method_46416(-0.5f, -0.5f, -0.5f);
        internalTransform.translate(-0.5f, -0.5f, -0.5f);
        Map<String, List<BedrockItemGeoModel.BedrockCube>> cubesByBone = geo.getCubesByBone();
        boolean flipNormalsX = arm == class_1306.field_6182;
        if (cubesByBone != null && !cubesByBone.isEmpty()) {
            float animTime = player.field_6012 + partialTicks;
            renderCubes(stack, geo, poseStack, buffer, packedLight, texture, def.getBoneEmission(), def.getBoneTextures(), def.getBoneTextureAnim(), def.isBoneEmissionBlur(), def.isBoneEmissionClamp(), def.getBoneEmissionStrength(), def.getTextureAnimFrames(), def.getTextureAnimFrameTime(), def.getTextureAnimFrameHeight(), def.getTexturePixelWidth(), def.getTexturePixelHeight(), animTime, partialTicks, def, arm, interactionHand, internalTransform, flipNormalsX);
        } else {
            class_630 root = geo.getRoot();
            class_4588 consumer = buffer.getBuffer(class_1921.method_23580(texture));
            root.method_22698(poseStack, consumer, packedLight, class_4608.field_21444);
        }
        poseStack.method_22909();
        return true;
    }

    private static void applyDisplayTransform(BedrockItemDefinition def, class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack) {
        BedrockItemDefinition.DisplayTransform transform = def.getDisplayTransform(arm);
        if (transform == null) {
            applyVanillaDisplayTransform(stack, player, arm, poseStack);
            return;
        }
        float tx = transform.transX * MODEL_UNIT;
        float ty = transform.transY * MODEL_UNIT;
        float tz = transform.transZ * MODEL_UNIT;
        float rx = transform.rotX;
        float ry = transform.rotY;
        float rz = transform.rotZ;
        if (arm == class_1306.field_6182) {
            tx = -tx;
            ry = -ry;
            rz = -rz;
        }
        poseStack.method_46416(tx, ty, tz);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(rx));
        poseStack.method_22907(class_7833.field_40716.rotationDegrees(ry));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(rz));
        poseStack.method_22905(transform.scaleX, transform.scaleY, transform.scaleZ);
    }

    private static void applyDisplayTransformMatrix(BedrockItemDefinition def, class_1799 stack, class_746 player, class_1306 arm, Matrix4f matrix) {
        if (matrix == null) {
            return;
        }
        BedrockItemDefinition.DisplayTransform transform = def.getDisplayTransform(arm);
        if (transform == null) {
            class_811 context = arm == class_1306.field_6182 ? class_811.field_4321 : class_811.field_4322;
            class_804 vanilla = resolveDisplayTransform(stack, context, player);
            if (vanilla == null) {
                return;
            }
            Vector3fc rotation = vanilla.comp_3747();
            Vector3fc translation = vanilla.comp_3748();
            Vector3fc scale = vanilla.comp_3749();
            if (rotation == null || translation == null || scale == null) {
                return;
            }
            matrix.translate(translation.x() * MODEL_UNIT, translation.y() * MODEL_UNIT, translation.z() * MODEL_UNIT);
            matrix.rotateXYZ((float) Math.toRadians(rotation.x()), (float) Math.toRadians(rotation.y()), (float) Math.toRadians(rotation.z()));
            matrix.scale(scale.x(), scale.y(), scale.z());
            return;
        }
        float tx = transform.transX * MODEL_UNIT;
        float ty = transform.transY * MODEL_UNIT;
        float tz = transform.transZ * MODEL_UNIT;
        float rx = transform.rotX;
        float ry = transform.rotY;
        float rz = transform.rotZ;
        if (arm == class_1306.field_6182) {
            tx = -tx;
            ry = -ry;
            rz = -rz;
        }
        matrix.translate(tx, ty, tz);
        matrix.rotateXYZ((float) Math.toRadians(rx), (float) Math.toRadians(ry), (float) Math.toRadians(rz));
        matrix.scale(transform.scaleX, transform.scaleY, transform.scaleZ);
    }

    private static void applyVanillaDisplayTransform(class_1799 stack, class_746 player, class_1306 arm, class_4587 poseStack) {
        class_811 class_811Var;
        if (stack == null || stack.method_7960() || player == null || arm == null) {
            return;
        }
        if (arm == class_1306.field_6182) {
            class_811Var = class_811.field_4321;
        } else {
            class_811Var = class_811.field_4322;
        }
        class_811 context = class_811Var;
        class_804 transform = resolveDisplayTransform(stack, context, player);
        if (transform == null) {
            return;
        }
        Vector3fc rotation = transform.comp_3747();
        Vector3fc translation = transform.comp_3748();
        Vector3fc scale = transform.comp_3749();
        if (rotation == null || translation == null || scale == null) {
            return;
        }
        poseStack.method_46416(translation.x() * MODEL_UNIT, translation.y() * MODEL_UNIT, translation.z() * MODEL_UNIT);
        poseStack.method_22907(class_7833.field_40714.rotationDegrees(rotation.x()));
        poseStack.method_22907(class_7833.field_40716.rotationDegrees(rotation.y()));
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(rotation.z()));
        poseStack.method_22905(scale.x(), scale.y(), scale.z());
    }

    private static class_804 resolveDisplayTransform(class_1799 stack, class_811 context, class_1309 entity) {
        if (stack == null || stack.method_7960() || context == null) {
            return null;
        }
        class_804 resourcePack = ResourcePackItemTransformResolver.resolve(stack, context);
        if (resourcePack != null) {
            return resourcePack;
        }
        return resolveVanillaTransform(stack, context, entity);
    }

    private static class_804 resolveVanillaTransform(class_1799 stack, class_811 context, class_1309 entity) {
        class_310 mc;
        class_10442 resolver;
        if (stack == null || stack.method_7960() || context == null || entity == null || (mc = class_310.method_1551()) == null || (resolver = mc.method_65386()) == null) {
            return null;
        }
        class_10444 state = new class_10444();
        boolean prevForced = UseAnimationSuppressor.isForced();
        UseAnimationSuppressor.force(true);
        try {
            resolver.method_65597(state, stack, context, entity);
            UseAnimationSuppressor.force(prevForced);
            return resolveLayerTransform(state);
        } catch (Throwable th) {
            UseAnimationSuppressor.force(prevForced);
            return null;
        }
    }

    private static class_804 resolveLayerTransform(class_10444 state) {
        class_804 transform;
        if (state == null) {
            return null;
        }
        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) state;
        ItemStackRenderStateLayerAccessor[] itemStackRenderStateLayerAccessorArrPunchy$getLayers = accessor.punchy$getLayers();
        int count = accessor.punchy$getActiveLayerCount();
        if (itemStackRenderStateLayerAccessorArrPunchy$getLayers == null || count <= 0) {
            return null;
        }
        for (int i = 0; i < count && i < itemStackRenderStateLayerAccessorArrPunchy$getLayers.length; i++) {
            ItemStackRenderStateLayerAccessor itemStackRenderStateLayerAccessor = itemStackRenderStateLayerAccessorArrPunchy$getLayers[i];
            if (itemStackRenderStateLayerAccessor != null && (transform = itemStackRenderStateLayerAccessor.punchy$getTransform()) != null) {
                return transform;
            }
        }
        return null;
    }

    private static class_1268 resolveInteractionHand(class_746 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1268.field_5808;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? class_1268.field_5808 : class_1268.field_5810 : mainRight ? class_1268.field_5810 : class_1268.field_5808;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void renderCubes(class_1799 stack, BedrockItemGeoModel geo, class_4587 poseStack, class_4597 buffer, int packedLight, class_2960 texture, Map<String, Integer> boneEmission, Map<String, class_2960> boneTextures, Map<String, BedrockItemDefinition.TextureAnimSpec> boneTextureAnim, boolean emissiveBlur, boolean emissiveClamp, int emissiveStrength, int textureAnimFrames, int textureAnimFrameTime, int textureAnimFrameHeight, int texturePixelWidth, int texturePixelHeight, float animTime, float partialTicks, BedrockItemDefinition def, class_1306 arm, class_1268 interactionHand, Matrix4f internalTransform, boolean flipNormalsX) throws MatchException {
        int emission;
        class_2960 boneTexture;
        float fMin;
        class_2960 boneTexture2;
        Matrix4f world;
        PunchyTuningConfig.CustomAnimationEntry entry;
        Map<String, class_630> parts = geo.getParts();
        Map<String, String> parentByBone = geo.getParentByBone();
        Map<String, List<BedrockItemGeoModel.BedrockCube>> cubesByBone = geo.getCubesByBone();
        boolean pufferModel = isPufferModel(parts);
        boolean axolModel = !pufferModel && isAxolModel(parts, cubesByBone);
        Map<String, Matrix4f> matrixCache = new HashMap<>();
        Matrix4f headWorld = axolModel ? computeWorldMatrix("head", parts, parentByBone, matrixCache) : null;
        Matrix4f bodyWorld = axolModel ? computeWorldMatrix("body", parts, parentByBone, matrixCache) : null;
        Matrix4f fishWorld = pufferModel ? computeWorldMatrix("fish", parts, parentByBone, matrixCache) : null;
        int texW = geo.getTextureWidth();
        int texH = geo.getTextureHeight();
        Map<String, Integer> emissionByBone = boneEmission == null ? Map.of() : boneEmission;
        float strengthScale = class_3532.method_15363(emissiveStrength / 100.0f, 0.0f, 1.0f);
        Map<String, class_2960> texturesByBone = mergeDynamicBoneTextures(stack, def, boneTextures);
        Map<String, class_2960> resolvedTextureByBone = new HashMap<>();
        Map<String, BedrockItemDefinition.TextureAnimSpec> animByBone = boneTextureAnim == null ? Map.of() : boneTextureAnim;
        boolean shaderPackInUse = isShaderPackInUse();
        Set<String> cameraTransparentBones = def != null ? def.getCameraTransparentBones() : Set.of();
        Map<String, BedrockItemDefinition.BonePhysicsSpec> bonePhysics = mergeBonePhysics(def != null ? def.getBonePhysics() : null, ToolTuning.getSpecificBonePhysics(stack));
        Set<String> noPhysicsBones = null;
        if (arm != null && (entry = BedrockItemAnimationManager.getActiveEntry(arm)) != null && entry.disable_physics_bones != null) {
            noPhysicsBones = new HashSet();
            for (String bone : entry.disable_physics_bones) {
                if (bone != null && !bone.isBlank()) {
                    noPhysicsBones.add(bone.trim());
                }
            }
        }
        if (noPhysicsBones == null) {
            noPhysicsBones = def != null ? def.getDisablePhysicsBones() : Set.of();
        }
        Matrix4f physicsRemoval = null;
        if (noPhysicsBones != null && !noPhysicsBones.isEmpty() && arm != null) {
            Matrix4f physicsDelta = BedrockItemSystem.getCurrentPhysicsDelta(arm);
            if (physicsDelta != null) {
                physicsRemoval = new Matrix4f(physicsDelta);
            } else {
                Matrix4f pendulum = PendulumPhysics.get().getLastAppliedMatrix(arm);
                if (pendulum != null) {
                    physicsRemoval = new Matrix4f(pendulum).invert();
                }
            }
        }
        Matrix4f internal = internalTransform != null ? new Matrix4f(internalTransform) : new Matrix4f();
        Matrix4f internalInv = new Matrix4f(internal).invert();
        Matrix4f adjustedPhysicsRemoval = null;
        if (physicsRemoval != null) {
            adjustedPhysicsRemoval = new Matrix4f(internalInv).mul(physicsRemoval).mul(internal);
        }
        Matrix4f adjustedRemoval = adjustedPhysicsRemoval == null ? null : new Matrix4f(adjustedPhysicsRemoval);
        BedrockAxolPhysicsStateMachine.FeedbackPose axolPose = null;
        BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose = null;
        if (def != null && def.isPhysicsFeedbackEnabled() && axolModel) {
            axolPose = BedrockItemSystem.get().getAxolPhysicsPose(arm, partialTicks);
            if (axolPose != null && axolPose.isIdentity()) {
                axolPose = null;
            }
        }
        if (def != null && (pufferModel || hasCreatureBonePhysics(bonePhysics))) {
            pufferPose = BedrockItemSystem.get().getPufferPhysicsPose(arm, partialTicks);
            if (pufferPose != null && pufferPose.isIdentity()) {
                pufferPose = null;
            }
        }
        List<Map.Entry<String, List<BedrockItemGeoModel.BedrockCube>>> entries = new ArrayList<>(cubesByBone.entrySet());
        Map<String, Matrix4f> adjustedWorldByBone = new HashMap<>();
        Map<String, Boolean> suppressPhysicsByBone = new HashMap<>();
        Map<String, Matrix4f> renderBoneTransforms = new HashMap<>();
        Matrix4f renderRoot = new Matrix4f(poseStack.method_23760().method_23761());
        Set<String> particleBoneNames = new LinkedHashSet<>(parts.keySet());
        particleBoneNames.addAll(cubesByBone.keySet());
        for (String boneName : particleBoneNames) {
            if (boneName != null && (world = computeWorldMatrix(boneName, parts, parentByBone, matrixCache)) != null) {
                boolean suppressBonePhysics = noPhysicsBones != null && isPendulumSuppressed(boneName, parentByBone, noPhysicsBones);
                Matrix4f adjustedWorld = world;
                if (axolPose != null) {
                    adjustedWorld = applyAxolPhysicsIfNeeded(adjustedWorld, boneName, axolPose, parentByBone, headWorld, bodyWorld);
                }
                if (pufferPose != null) {
                    adjustedWorld = applyPufferPhysicsIfNeeded(adjustedWorld, boneName, pufferPose, parentByBone, fishWorld);
                }
                Matrix4f adjustedWorld2 = applyCustomBonePhysicsIfNeeded(adjustedWorld, boneName, bonePhysics, renderRoot, parts, parentByBone, matrixCache, pufferPose, arm, interactionHand, partialTicks);
                adjustedWorldByBone.put(boneName, adjustedWorld2);
                suppressPhysicsByBone.put(boneName, Boolean.valueOf(suppressBonePhysics));
                Matrix4f finalMatrix = new Matrix4f(renderRoot);
                if (adjustedRemoval != null && suppressBonePhysics) {
                    finalMatrix.mul(adjustedRemoval);
                }
                finalMatrix.mul(adjustedWorld2);
                renderBoneTransforms.put(boneName, finalMatrix);
            }
        }
        List<String> compatParticleEffects = ToolTuning.getParticleEmitters(stack);
        ModelPartsParticleSystem.setRenderRoot(arm, poseStack.method_23760().method_23761());
        List<String> definitionParticleEffects = mergeParticleEffects(def == null ? List.of() : def.getDefinitionParticleEffects(), compatParticleEffects);
        ModelPartsParticleSystem.emitDefinitionForRenderBones(class_310.method_1551(), arm, renderBoneTransforms, definitionParticleEffects);
        ModelPartsParticleSystem.emitPendingForRenderBones(class_310.method_1551(), arm, renderBoneTransforms);
        ModelPartsParticleSystem.renderForArm(class_310.method_1551(), arm, buffer, poseStack, packedLight);
        for (Map.Entry<String, List<BedrockItemGeoModel.BedrockCube>> entry2 : entries) {
            String boneName2 = entry2.getKey();
            List<BedrockItemGeoModel.BedrockCube> cubes = entry2.getValue();
            if (boneName2 != null && cubes != null && !cubes.isEmpty() && (boneTexture2 = resolveBoneTexture(boneName2, texturesByBone, parentByBone, resolvedTextureByBone, texture)) != null) {
                class_1921 baseRenderType = class_1921.method_23580(boneTexture2);
                TextureAnim anim = resolveAnimForBone(animByBone.get(boneName2), textureAnimFrames, textureAnimFrameTime, textureAnimFrameHeight, def, boneTexture2, texW, texH, animTime);
                Matrix4f adjustedWorld3 = adjustedWorldByBone.get(boneName2);
                if (adjustedWorld3 != null) {
                    boolean suppressBonePhysics2 = Boolean.TRUE.equals(suppressPhysicsByBone.get(boneName2));
                    poseStack.method_22903();
                    if (adjustedRemoval != null && suppressBonePhysics2) {
                        poseStack.method_23760().method_23761().mul(adjustedRemoval);
                        poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedRemoval));
                    }
                    poseStack.method_23760().method_23761().mul(adjustedWorld3);
                    poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedWorld3));
                    boolean cameraTransparent = findBonePhysicsSpec(boneName2, bonePhysics, parentByBone) == null && isCameraTransparentBone(boneName2, parentByBone, cameraTransparentBones);
                    class_4588 baseConsumer = buffer.getBuffer(baseRenderType);
                    class_4587.class_4665 basePose = poseStack.method_23760();
                    for (BedrockItemGeoModel.BedrockCube cube : cubes) {
                        renderCubeFaces(basePose, baseConsumer, cube, packedLight, texW, texH, 1.0f, 1.0f, anim, flipNormalsX, cameraTransparent);
                    }
                    poseStack.method_22909();
                }
            }
        }
        for (Map.Entry<String, List<BedrockItemGeoModel.BedrockCube>> entry3 : entries) {
            String boneName3 = entry3.getKey();
            List<BedrockItemGeoModel.BedrockCube> cubes2 = entry3.getValue();
            if (boneName3 != null && cubes2 != null && !cubes2.isEmpty() && (emission = getBoneEmission(emissionByBone, boneName3)) > 0 && (boneTexture = resolveBoneTexture(boneName3, texturesByBone, parentByBone, resolvedTextureByBone, texture)) != null) {
                class_1921 emissiveRenderType = emissiveOverlay(boneTexture, emissiveBlur, emissiveClamp, shaderPackInUse);
                TextureAnim anim2 = resolveAnimForBone(animByBone.get(boneName3), textureAnimFrames, textureAnimFrameTime, textureAnimFrameHeight, def, boneTexture, texW, texH, animTime);
                Matrix4f adjustedWorld4 = adjustedWorldByBone.get(boneName3);
                if (adjustedWorld4 != null) {
                    boolean suppressBonePhysics3 = Boolean.TRUE.equals(suppressPhysicsByBone.get(boneName3));
                    float baseIntensity = emissionToBaseIntensity(emission) * strengthScale;
                    float extraGlow = emissionToExtraGlow(emission) * strengthScale;
                    if (baseIntensity > 0.0f || extraGlow > 0.0f) {
                        float colorScale = Math.min(1.0f, baseIntensity);
                        if (shaderPackInUse) {
                            fMin = 1.0f;
                        } else {
                            fMin = Math.min(1.0f, (emissiveBlur ? 0.42f : 0.58f) + (extraGlow * 0.18f));
                        }
                        float emissiveAlpha = fMin;
                        poseStack.method_22903();
                        if (adjustedRemoval != null && suppressBonePhysics3) {
                            poseStack.method_23760().method_23761().mul(adjustedRemoval);
                            poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedRemoval));
                        }
                        poseStack.method_23760().method_23761().mul(adjustedWorld4);
                        poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedWorld4));
                        boolean cameraTransparent2 = findBonePhysicsSpec(boneName3, bonePhysics, parentByBone) == null && isCameraTransparentBone(boneName3, parentByBone, cameraTransparentBones);
                        class_4588 emissiveConsumer = buffer.getBuffer(emissiveRenderType);
                        class_4587.class_4665 emissivePose = poseStack.method_23760();
                        for (BedrockItemGeoModel.BedrockCube cube2 : cubes2) {
                            renderCubeFaces(emissivePose, emissiveConsumer, cube2, 15728880, texW, texH, colorScale, emissiveAlpha, anim2, flipNormalsX, cameraTransparent2);
                        }
                        poseStack.method_22909();
                        if (emissiveBlur) {
                            float glowBoost = 1.0f + (extraGlow * 1.5f);
                            float blurScale = EMISSIVE_BLUR_SCALE + (extraGlow * 0.08f);
                            float blurColor = Math.min(1.0f, colorScale * (EMISSIVE_BLUR_COLOR + (extraGlow * EMISSIVE_BLUR_ALPHA)));
                            float blurAlpha = Math.min(1.0f, EMISSIVE_BLUR_ALPHA * glowBoost);
                            if (!shaderPackInUse) {
                                blurScale += 0.1f + (extraGlow * 0.05f);
                                blurColor = Math.min(1.0f, blurColor * 1.06f);
                                blurAlpha = Math.min(1.0f, blurAlpha * 1.18f);
                            }
                            renderEmissiveCubeHalo(poseStack, buffer, emissiveRenderType, cubes2, adjustedRemoval, adjustedWorld4, suppressBonePhysics3, texW, texH, blurColor, blurAlpha, blurScale, anim2, flipNormalsX, cameraTransparent2, shaderPackInUse);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void renderEmissiveCubeHalo(class_4587 poseStack, class_4597 buffer, class_1921 emissiveRenderType, List<BedrockItemGeoModel.BedrockCube> cubes, Matrix4f adjustedRemoval, Matrix4f adjustedWorld, boolean suppressBonePhysics, int texW, int texH, float blurColor, float blurAlpha, float blurScale, TextureAnim anim, boolean flipNormalsX, boolean cameraTransparent, boolean shaderPackInUse) throws MatchException {
        float[] fArr;
        float[] fArr2;
        if (poseStack == null || buffer == null || emissiveRenderType == null || cubes == null || cubes.isEmpty()) {
            return;
        }
        if (shaderPackInUse) {
            fArr = new float[]{1.0f};
        } else {
            fArr = new float[]{0.52f, 0.31f, 0.17f};
        }
        float[] alphaWeights = fArr;
        if (shaderPackInUse) {
            fArr2 = new float[]{0.0f};
        } else {
            fArr2 = new float[]{0.0f, 0.08f, 0.18f};
        }
        float[] scaleOffsets = fArr2;
        class_4588 blurConsumer = buffer.getBuffer(emissiveRenderType);
        for (int i = 0; i < alphaWeights.length; i++) {
            float layerAlpha = Math.min(1.0f, blurAlpha * alphaWeights[i]);
            if (layerAlpha > 0.0f) {
                poseStack.method_22903();
                if (adjustedRemoval != null && suppressBonePhysics) {
                    poseStack.method_23760().method_23761().mul(adjustedRemoval);
                    poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedRemoval));
                }
                poseStack.method_23760().method_23761().mul(adjustedWorld);
                poseStack.method_23760().method_23762().mul(new Matrix3f(adjustedWorld));
                float layerScale = blurScale + scaleOffsets[i];
                poseStack.method_22905(layerScale, layerScale, layerScale);
                float layerColorScale = shaderPackInUse ? 1.0f : 1.0f - (i * 0.08f);
                class_4587.class_4665 blurPose = poseStack.method_23760();
                for (BedrockItemGeoModel.BedrockCube cube : cubes) {
                    renderCubeFaces(blurPose, blurConsumer, cube, 15728880, texW, texH, Math.min(1.0f, blurColor * layerColorScale), layerAlpha, anim, flipNormalsX, cameraTransparent);
                }
                poseStack.method_22909();
            }
        }
    }

    private static Matrix4f applyAxolPhysicsIfNeeded(Matrix4f world, String boneName, BedrockAxolPhysicsStateMachine.FeedbackPose axolPose, Map<String, String> parentByBone, Matrix4f headWorld, Matrix4f bodyWorld) {
        if (world == null || axolPose == null || boneName == null) {
            return world;
        }
        if ("fish".equals(boneName)) {
            return world;
        }
        boolean inHeadRig = headWorld != null && ("head".equals(boneName) || isBoneOrDescendantOf(boneName, "head", parentByBone));
        if (inHeadRig) {
            Matrix4f headFeedback = buildAxolPhysicsTransform("head", axolPose);
            Matrix4f invHead = new Matrix4f(headWorld).invert();
            return new Matrix4f(headWorld).mul(headFeedback).mul(invHead).mul(world);
        }
        boolean inBodyRig = bodyWorld != null && ("body".equals(boneName) || isBoneOrDescendantOf(boneName, "body", parentByBone));
        if (inBodyRig) {
            Matrix4f bodyFeedback = buildAxolPhysicsTransform("body", axolPose);
            Matrix4f invBody = new Matrix4f(bodyWorld).invert();
            Matrix4f adjusted = new Matrix4f(bodyWorld).mul(bodyFeedback).mul(invBody).mul(world);
            if ("leg1".equals(boneName) || "leg2".equals(boneName) || "leg4".equals(boneName) || "leg5".equals(boneName)) {
                adjusted.mul(buildAxolPhysicsTransform(boneName, axolPose));
            }
            return adjusted;
        }
        if (!AXOL_PHYSICS_BONES.contains(boneName)) {
            return world;
        }
        Matrix4f fallbackFeedback = buildAxolPhysicsTransform(boneName, axolPose);
        return new Matrix4f(world).mul(fallbackFeedback);
    }

    private static Matrix4f buildAxolPhysicsTransform(String boneName, BedrockAxolPhysicsStateMachine.FeedbackPose axolPose) {
        float tyMul = 1.0f;
        float tzMul = 1.0f;
        float pitchMul = 1.0f;
        float rollMul = 1.0f;
        float scaleMul = 1.0f;
        float extraPitch = 0.0f;
        float extraYaw = 0.0f;
        float extraRoll = 0.0f;
        float extraTranslateX = 0.0f;
        float extraTranslateZ = 0.0f;
        float wf = axolPose.impactWave();
        float armGate = axolPose.armRaiseGate();
        axolPose.landingPunch();
        float walkPitch = axolPose.walkPitchDeg();
        float walkRollMul = 0.5f;
        if ("head".equals(boneName)) {
            tyMul = 1.0f;
            tzMul = 1.0f;
            pitchMul = 0.975f;
            rollMul = 1.6575f;
            scaleMul = 0.45f;
            walkRollMul = -0.5f;
            extraTranslateX = (-walkPitch) * 0.035f;
            extraRoll = 0.0f + (axolPose.translateXUnits() * 31.2f);
        } else if ("body".equals(boneName) || "legs_front".equals(boneName) || "legs_rear".equals(boneName)) {
            tyMul = 0.65f;
            tzMul = 0.6f;
            pitchMul = 0.4f;
            rollMul = 0.55f;
            scaleMul = 0.35f;
            walkRollMul = 0.3f;
            extraRoll = 0.0f + (axolPose.translateXUnits() * 12.0f);
        } else if ("leg1".equals(boneName) || "leg2".equals(boneName) || "leg4".equals(boneName) || "leg5".equals(boneName)) {
            tyMul = 0.25f;
            tzMul = 0.2f;
            pitchMul = 0.25f;
            rollMul = 0.3f;
            scaleMul = 0.2f;
            float side = ("leg2".equals(boneName) || "leg5".equals(boneName)) ? -1.0f : 1.0f;
            extraPitch = 24.0f * armGate;
            extraYaw = 18.0f * armGate * side;
            extraRoll = 12.0f * wf * armGate * side;
            extraTranslateZ = 0.08f * armGate;
        } else if ("tail".equals(boneName)) {
            tyMul = 0.35f;
            tzMul = 0.3f;
            pitchMul = 0.45f;
            rollMul = 0.55f;
            scaleMul = 0.25f;
        }
        float tx = axolPose.translateXUnits();
        float ty = (axolPose.translateYUnits() * tyMul) + axolPose.highFallLiftUnits();
        float tz = axolPose.translateZUnits() * tzMul;
        float pitchBase = axolPose.pitchDeg();
        if (("body".equals(boneName) || "legs_front".equals(boneName) || "legs_rear".equals(boneName)) && pitchBase < 0.0f) {
            pitchBase *= 0.32f;
        }
        float pitch = (pitchBase * pitchMul) + extraPitch;
        float yaw = extraYaw;
        float roll = (axolPose.rollDeg() * rollMul) + (walkPitch * walkRollMul) + extraRoll;
        float sx = 1.0f + ((axolPose.scaleX() - 1.0f) * scaleMul);
        float sy = 1.0f + ((axolPose.scaleY() - 1.0f) * scaleMul);
        float sz = 1.0f + ((axolPose.scaleZ() - 1.0f) * scaleMul);
        return new Matrix4f().translate(extraTranslateX + tx, ty, tz + extraTranslateZ).rotateXYZ((float) Math.toRadians(pitch), (float) Math.toRadians(yaw), (float) Math.toRadians(roll)).scale(sx, sy, sz);
    }

    private static Matrix4f applyPufferPhysicsIfNeeded(Matrix4f world, String boneName, BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose, Map<String, String> parentByBone, Matrix4f fishWorld) {
        if (world == null || boneName == null || pufferPose == null) {
            return world;
        }
        boolean underFish = isBoneOrDescendantOf(boneName, "fish", parentByBone);
        if (!underFish) {
            return world;
        }
        Matrix4fc matrix4f = new Matrix4f(world);
        if (fishWorld != null) {
            Matrix4f base = buildPufferBaseTransform(pufferPose);
            Matrix4f invFish = new Matrix4f(fishWorld).invert();
            matrix4f = new Matrix4f(fishWorld).mul(base).mul(invFish).mul(matrix4f);
        }
        if (isPufferFinBone(boneName)) {
            matrix4f.mul(buildPufferInverseScaleTransform(pufferPose));
            matrix4f.mul(buildPufferFinTransform(boneName, pufferPose));
            return matrix4f;
        }
        String finAnchor = null;
        if (isBoneOrDescendantOf(boneName, "right_fin", parentByBone)) {
            finAnchor = "right_fin";
        } else if (isBoneOrDescendantOf(boneName, "fin_right", parentByBone)) {
            finAnchor = "fin_right";
        } else if (isBoneOrDescendantOf(boneName, "left_fin", parentByBone)) {
            finAnchor = "left_fin";
        } else if (isBoneOrDescendantOf(boneName, "fin_left", parentByBone)) {
            finAnchor = "fin_left";
        }
        if (finAnchor != null) {
            matrix4f.mul(buildPufferInverseScaleTransform(pufferPose));
            matrix4f.mul(buildPufferFinTransform(finAnchor, pufferPose));
        }
        return matrix4f;
    }

    private static Matrix4f applyCustomBonePhysicsIfNeeded(Matrix4f world, String boneName, Map<String, BedrockItemDefinition.BonePhysicsSpec> bonePhysics, Matrix4f renderRoot, Map<String, class_630> parts, Map<String, String> parentByBone, Map<String, Matrix4f> matrixCache, BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose, class_1306 arm, class_1268 hand, float partialTicks) {
        Matrix4f baseWorld;
        if (world == null || boneName == null || bonePhysics == null || bonePhysics.isEmpty()) {
            return world;
        }
        List<String> lineage = collectBoneLineage(boneName, parentByBone);
        if (lineage.isEmpty()) {
            return world;
        }
        Matrix4f adjusted = new Matrix4f(world);
        Matrix4f accumulatedDelta = new Matrix4f();
        for (String currentBone : lineage) {
            BedrockItemDefinition.BonePhysicsSpec spec = bonePhysics.get(currentBone);
            if (spec != null && !spec.isEmpty() && (baseWorld = computeWorldMatrix(currentBone, parts, parentByBone, matrixCache)) != null) {
                Matrix4f inheritedWorld = new Matrix4f(accumulatedDelta).mul(baseWorld);
                Matrix4f delta = computeCustomBonePhysicsDelta(inheritedWorld, baseWorld, renderRoot, spec, pufferPose, arm, hand, partialTicks, currentBone);
                if (delta != null) {
                    adjusted = new Matrix4f(delta).mul(adjusted);
                    accumulatedDelta = new Matrix4f(delta).mul(accumulatedDelta);
                }
            }
        }
        return adjusted;
    }

    private static Matrix4f computeCustomBonePhysicsDelta(Matrix4f applicationWorld, Matrix4f solverWorld, Matrix4f renderRoot, BedrockItemDefinition.BonePhysicsSpec spec, BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose, class_1306 arm, class_1268 hand, float partialTicks, String bonePhysicsChannel) {
        Matrix4f matrix4fMul;
        if (applicationWorld == null || solverWorld == null || spec == null || spec.isEmpty()) {
            return null;
        }
        Matrix4f adjusted = new Matrix4f(applicationWorld);
        if (spec.enableCreaturePhysics() && pufferPose != null) {
            adjusted.mul(buildPufferBaseTransform(pufferPose, spec));
        }
        if (spec.enableSlimePhysics()) {
            float deform = SlimeInHandPhysics.get().sampleSlimeBlockDeform(hand, partialTicks);
            if (Math.abs(deform) > 1.0E-4f) {
                if (deform > 0.0f) {
                    deform *= spec.slimeFallScale();
                } else if (deform < 0.0f) {
                    deform *= spec.slimeImpactScale();
                }
                float horizontal = SlimeInHandPhysics.horizontalScaleForDeform(deform);
                float vertical = SlimeInHandPhysics.verticalScaleForDeform(deform);
                adjusted.scale(horizontal, vertical, horizontal);
            }
        }
        if (spec.pendulumMode() != null && spec.pendulumMode() != ToolTuning.PhysicsMode.NONE && arm != null) {
            Matrix4f pendulumSolverBasis = new Matrix4f(solverWorld);
            if (renderRoot == null) {
                matrix4fMul = pendulumSolverBasis;
            } else {
                matrix4fMul = new Matrix4f(renderRoot).mul(pendulumSolverBasis);
            }
            Matrix4f pendulumReference = matrix4fMul;
            float inheritedPitchDeg = extractInheritedPitchDeg(applicationWorld, solverWorld);
            ToolTuning.PhysicsSettings effectiveSettings = applyRemainingPendulumLimit(spec.pendulumSettings(), inheritedPitchDeg);
            Matrix4f pendulum = PendulumPhysics.get().computeAppliedMatrix(pendulumReference, spec.pendulumMode(), new Vector3f(), effectiveSettings, arm, partialTicks, bonePhysicsChannel, inheritedPitchDeg);
            if (pendulum != null) {
                adjusted.mul(pendulum);
            }
        }
        return adjusted.mul(new Matrix4f(applicationWorld).invert());
    }

    private static float extractInheritedPitchDeg(Matrix4f applicationWorld, Matrix4f solverWorld) {
        if (applicationWorld == null || solverWorld == null) {
            return 0.0f;
        }
        Matrix4f inheritedDelta = new Matrix4f(applicationWorld).mul(new Matrix4f(solverWorld).invert());
        Quaternionf rotation = inheritedDelta.getUnnormalizedRotation(new Quaternionf());
        if (rotation.lengthSquared() < 1.0E-8f) {
            return 0.0f;
        }
        rotation.normalize();
        Vector3f forward = rotation.transform(new Vector3f(0.0f, 0.0f, 1.0f));
        float horizontalLen = (float) Math.sqrt((forward.x * forward.x) + (forward.z * forward.z));
        return (float) Math.toDegrees(Math.atan2(-forward.y, horizontalLen));
    }

    private static ToolTuning.PhysicsSettings applyRemainingPendulumLimit(ToolTuning.PhysicsSettings settings, float inheritedPitchDeg) {
        if (settings == null) {
            return null;
        }
        boolean hasAnyLimit = (settings.limitDeg == null && settings.limitDegUp == null && settings.limitDegDown == null) ? false : true;
        if (!hasAnyLimit) {
            return settings;
        }
        ToolTuning.PhysicsSettings adjusted = settings.copy();
        float consumed = Math.abs(inheritedPitchDeg);
        if (adjusted.limitDeg != null) {
            adjusted.limitDeg = Float.valueOf(Math.max(0.0f, adjusted.limitDeg.floatValue() - consumed));
        }
        if (adjusted.limitDegUp != null) {
            adjusted.limitDegUp = Float.valueOf(Math.max(0.0f, adjusted.limitDegUp.floatValue() - consumed));
        }
        if (adjusted.limitDegDown != null) {
            adjusted.limitDegDown = Float.valueOf(Math.max(0.0f, adjusted.limitDegDown.floatValue() - consumed));
        }
        return adjusted;
    }

    private static List<String> collectBoneLineage(String boneName, Map<String, String> parentByBone) {
        if (boneName == null) {
            return List.of();
        }
        List<String> reversed = new ArrayList<>();
        String current = boneName;
        for (int guard = 0; current != null && guard < 64; guard++) {
            reversed.add(current);
            current = parentByBone != null ? parentByBone.get(current) : null;
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private static BedrockItemDefinition.BonePhysicsSpec findBonePhysicsSpec(String boneName, Map<String, BedrockItemDefinition.BonePhysicsSpec> bonePhysics, Map<String, String> parentByBone) {
        if (boneName == null || bonePhysics == null || bonePhysics.isEmpty()) {
            return null;
        }
        String current = boneName;
        for (int guard = 0; current != null && guard < 64; guard++) {
            BedrockItemDefinition.BonePhysicsSpec spec = bonePhysics.get(current);
            if (spec != null) {
                return spec;
            }
            current = parentByBone != null ? parentByBone.get(current) : null;
        }
        return null;
    }

    private static Map<String, BedrockItemDefinition.BonePhysicsSpec> mergeBonePhysics(Map<String, BedrockItemDefinition.BonePhysicsSpec> definitionPhysics, JsonElement specificPhysicsJson) {
        Map<String, BedrockItemDefinition.BonePhysicsSpec> parsedSpecific = BedrockItemDefinition.parseBonePhysics(specificPhysicsJson);
        if ((definitionPhysics == null || definitionPhysics.isEmpty()) && parsedSpecific.isEmpty()) {
            return Map.of();
        }
        Map<String, BedrockItemDefinition.BonePhysicsSpec> merged = new HashMap<>();
        if (definitionPhysics != null && !definitionPhysics.isEmpty()) {
            merged.putAll(definitionPhysics);
        }
        for (Map.Entry<String, BedrockItemDefinition.BonePhysicsSpec> entry : parsedSpecific.entrySet()) {
            String bone = entry.getKey();
            BedrockItemDefinition.BonePhysicsSpec spec = entry.getValue();
            if (bone != null && !bone.isBlank() && spec != null) {
                BedrockItemDefinition.BonePhysicsSpec previous = merged.get(bone);
                merged.put(bone, previous == null ? spec : previous.merge(spec));
            }
        }
        return merged;
    }

    private static boolean hasCreatureBonePhysics(Map<String, BedrockItemDefinition.BonePhysicsSpec> bonePhysics) {
        if (bonePhysics == null || bonePhysics.isEmpty()) {
            return false;
        }
        for (BedrockItemDefinition.BonePhysicsSpec spec : bonePhysics.values()) {
            if (spec != null && spec.enableCreaturePhysics()) {
                return true;
            }
        }
        return false;
    }

    private static Matrix4f buildPufferBaseTransform(BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose, BedrockItemDefinition.BonePhysicsSpec spec) {
        float pitch = pufferPose.pitchDeg() * PUFFER_INCLINATION_SCALE * PUFFER_PITCH_BOOST;
        float translationScale = 1.0f;
        if (spec != null) {
            if (pitch > 0.0f) {
                pitch *= spec.impactInclinationScale();
                translationScale = spec.impactTranslationScale();
            } else if (pitch < 0.0f) {
                pitch *= spec.fallInclinationScale();
                translationScale = spec.fallTranslationScale();
            }
        }
        float tx = pufferPose.translateXUnits();
        float ty = pufferPose.translateYUnits() * translationScale;
        float tz = pufferPose.translateZUnits() * translationScale;
        float roll = pufferPose.rollDeg() * PUFFER_INCLINATION_SCALE;
        float sx = pufferPose.scaleX();
        float sy = pufferPose.scaleY();
        float sz = pufferPose.scaleZ();
        return new Matrix4f().translate(tx, ty, tz).rotateXYZ((float) Math.toRadians(pitch), 0.0f, (float) Math.toRadians(roll)).scale(sx, sy, sz);
    }

    private static Matrix4f buildPufferBaseTransform(BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose) {
        return buildPufferBaseTransform(pufferPose, new BedrockItemDefinition.BonePhysicsSpec(true, false, ToolTuning.PhysicsMode.NONE, null, 100.0f, 100.0f, 100.0f, 100.0f, 100.0f, 100.0f));
    }

    private static Matrix4f buildPufferInverseScaleTransform(BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose) {
        float sx = pufferPose.scaleX();
        float sy = pufferPose.scaleY();
        float sz = pufferPose.scaleZ();
        float invX = Math.abs(sx) > 1.0E-4f ? 1.0f / sx : 1.0f;
        float invY = Math.abs(sy) > 1.0E-4f ? 1.0f / sy : 1.0f;
        float invZ = Math.abs(sz) > 1.0E-4f ? 1.0f / sz : 1.0f;
        return new Matrix4f().scale(invX, invY, invZ);
    }

    private static Matrix4f buildPufferFinTransform(String boneName, BedrockPufferPhysicsStateMachine.FeedbackPose pufferPose) {
        boolean rightFin = "right_fin".equals(boneName) || "fin_right".equals(boneName);
        float side = rightFin ? 1.0f : -1.0f;
        float fin = class_3532.method_15363(pufferPose.finLift(), 0.0f, 1.0f);
        float finCurve = fin * fin * (3.0f - (2.0f * fin));
        float liftRoll = 32.0f * finCurve * PUFFER_INCLINATION_SCALE;
        float sideRoll = 6.0f * finCurve * PUFFER_INCLINATION_SCALE;
        float roll = (liftRoll + sideRoll) * side;
        return new Matrix4f().translate(0.0f, 0.06f * finCurve, 0.085f * finCurve).rotateXYZ((float) Math.toRadians(0.0f), (float) Math.toRadians(0.0f), (float) Math.toRadians(roll));
    }

    private static boolean isPufferFinBone(String boneName) {
        return "right_fin".equals(boneName) || "fin_right".equals(boneName) || "left_fin".equals(boneName) || "fin_left".equals(boneName);
    }

    private static boolean isPufferModel(Map<String, class_630> parts) {
        return parts != null && !parts.isEmpty() && parts.containsKey("fish") && (parts.containsKey("right_fin") || parts.containsKey("fin_right")) && (parts.containsKey("left_fin") || parts.containsKey("fin_left"));
    }

    private static boolean isAxolModel(Map<String, class_630> parts, Map<String, List<BedrockItemGeoModel.BedrockCube>> cubesByBone) {
        boolean hasHeadPart = parts != null && parts.containsKey("head");
        boolean hasBodyPart = parts != null && (parts.containsKey("body") || parts.containsKey("body_root"));
        boolean hasLegPart = parts != null && (parts.containsKey("legs_front") || parts.containsKey("leg1"));
        if (hasHeadPart && hasBodyPart && hasLegPart) {
            return true;
        }
        return cubesByBone != null && !cubesByBone.isEmpty() && cubesByBone.containsKey("head") && (cubesByBone.containsKey("body") || cubesByBone.containsKey("body_root")) && (cubesByBone.containsKey("legs_front") || cubesByBone.containsKey("leg1"));
    }

    private static boolean isBoneOrDescendantOf(String boneName, String ancestor, Map<String, String> parentByBone) {
        if (boneName == null || ancestor == null) {
            return false;
        }
        if (ancestor.equals(boneName)) {
            return true;
        }
        String current = boneName;
        for (int guard = 0; current != null && guard < 128; guard++) {
            current = parentByBone != null ? parentByBone.get(current) : null;
            if (ancestor.equals(current)) {
                return true;
            }
        }
        return false;
    }

    private static Matrix4f computeWorldMatrix(String boneName, Map<String, class_630> parts, Map<String, String> parentByBone, Map<String, Matrix4f> cache) {
        Matrix4f world;
        Matrix4f parentMatrix;
        Matrix4f cached = cache.get(boneName);
        if (cached != null) {
            return cached;
        }
        class_630 part = parts.get(boneName);
        if (part == null) {
            return null;
        }
        Matrix4f local = ModelPartMath.getLocalMatrix(part);
        String parent = parentByBone.get(boneName);
        if (parent != null && (parentMatrix = computeWorldMatrix(parent, parts, parentByBone, cache)) != null) {
            world = new Matrix4f(parentMatrix).mul(local);
        } else {
            world = local;
        }
        cache.put(boneName, world);
        return world;
    }

    private static Map<String, class_2960> mergeDynamicBoneTextures(class_1799 stack, BedrockItemDefinition def, Map<String, class_2960> baseTextures) {
        Map<String, class_2960> base = baseTextures == null ? Map.of() : baseTextures;
        if (def == null || def.getDynamicBoneTextures().isEmpty()) {
            return base;
        }
        Map<String, class_2960> merged = new HashMap<>(base);
        DynamicValueCache cache = new DynamicValueCache();
        for (BedrockItemDefinition.DynamicBoneTextureSpec spec : def.getDynamicBoneTextures()) {
            if (spec != null && spec.isTextureType() && spec.bone() != null && !spec.bone().isBlank()) {
                String dynamicKey = resolveDynamicKey(stack, spec, cache);
                class_2960 texture = dynamicKey == null ? null : spec.resourceMap().get(dynamicKey);
                if (texture == null) {
                    texture = spec.fallbackResource();
                }
                if (texture != null) {
                    merged.put(spec.bone().trim(), texture);
                }
            }
        }
        return merged;
    }

    private static class_2960 resolveDynamicGeoId(class_1799 stack, BedrockItemDefinition def) {
        if (def == null || def.getDynamicBoneTextures().isEmpty()) {
            return null;
        }
        class_2960 resolved = null;
        DynamicValueCache cache = new DynamicValueCache();
        for (BedrockItemDefinition.DynamicBoneTextureSpec spec : def.getDynamicBoneTextures()) {
            if (spec != null && spec.isGeoType()) {
                String dynamicKey = resolveDynamicKey(stack, spec, cache);
                class_2960 geoId = dynamicKey == null ? null : spec.resourceMap().get(dynamicKey);
                if (geoId == null) {
                    geoId = spec.fallbackResource();
                }
                if (geoId != null) {
                    resolved = geoId;
                }
            }
        }
        return resolved;
    }

    private static String resolveDynamicKey(class_1799 stack, BedrockItemDefinition.DynamicBoneTextureSpec spec, DynamicValueCache cache) {
        if (spec != null && spec.usesAxolVariants()) {
            if (!cache.axolResolved) {
                cache.axolResolved = true;
                cache.axolVariant = resolveAxolotlVariant(stack);
            }
            return cache.axolVariant;
        }
        return null;
    }

    private static String resolveAxolotlVariant(class_1799 stack) {
        class_5762.class_5767 variant;
        if (stack == null || stack.method_7960() || (variant = (class_5762.class_5767) stack.method_58694(class_9334.field_56140)) == null) {
            return null;
        }
        String serialized = variant.method_15434();
        if (serialized != null && !serialized.isBlank()) {
            return serialized;
        }
        String name = variant.method_33238();
        if (name == null || name.isBlank()) {
            return null;
        }
        return name;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemRenderer$DynamicValueCache.class */
    private static final class DynamicValueCache {
        private boolean axolResolved;
        private String axolVariant;

        private DynamicValueCache() {
        }
    }

    private static class_2960 resolveBoneTexture(String boneName, Map<String, class_2960> texturesByBone, Map<String, String> parentByBone, Map<String, class_2960> resolvedTextureByBone, class_2960 fallback) {
        if (boneName == null) {
            return fallback;
        }
        class_2960 direct = findTextureOverride(texturesByBone, boneName);
        if (direct != null) {
            resolvedTextureByBone.put(boneName, direct);
            return direct;
        }
        class_2960 cached = resolvedTextureByBone.get(boneName);
        if (cached != null) {
            return cached;
        }
        String current = boneName;
        for (int guard = 0; current != null && guard < 64; guard++) {
            class_2960 found = findTextureOverride(texturesByBone, current);
            if (found != null) {
                resolvedTextureByBone.put(boneName, found);
                return found;
            }
            current = parentByBone != null ? parentByBone.get(current) : null;
        }
        if (fallback != null) {
            resolvedTextureByBone.put(boneName, fallback);
        }
        return fallback;
    }

    private static class_2960 findTextureOverride(Map<String, class_2960> texturesByBone, String boneName) {
        class_2960 trimmedMatch;
        if (texturesByBone == null || texturesByBone.isEmpty() || boneName == null) {
            return null;
        }
        class_2960 direct = texturesByBone.get(boneName);
        if (direct != null) {
            return direct;
        }
        String trimmed = boneName.trim();
        if (!trimmed.equals(boneName) && (trimmedMatch = texturesByBone.get(trimmed)) != null) {
            return trimmedMatch;
        }
        for (Map.Entry<String, class_2960> entry : texturesByBone.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.trim().equalsIgnoreCase(trimmed)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void renderCubeFaces(class_4587.class_4665 pose, class_4588 consumer, BedrockItemGeoModel.BedrockCube cube, int packedLight, int texW, int texH, float colorScale, float alphaScale, TextureAnim anim, boolean flipNormalsX, boolean cameraTransparent) throws MatchException {
        Map<class_2350, BedrockItemGeoModel.BedrockFace> faces = cube.faces;
        if (faces == null || faces.isEmpty()) {
            return;
        }
        Map<class_2350, Boolean> skipFaces = computeThinFaceSkips(cube, faces);
        for (Map.Entry<class_2350, BedrockItemGeoModel.BedrockFace> entry : faces.entrySet()) {
            class_2350 direction = entry.getKey();
            BedrockItemGeoModel.BedrockFace face = entry.getValue();
            if (direction != null && face != null && (skipFaces == null || !Boolean.TRUE.equals(skipFaces.get(direction)))) {
                emitFace(pose, consumer, direction, cube, face, packedLight, texW, texH, colorScale, alphaScale, anim, flipNormalsX, cameraTransparent);
            }
        }
    }

    private static Map<class_2350, Boolean> computeThinFaceSkips(BedrockItemGeoModel.BedrockCube cube, Map<class_2350, BedrockItemGeoModel.BedrockFace> faces) {
        if (cube == null || faces == null || faces.isEmpty()) {
            return null;
        }
        float dx = Math.abs(cube.x2 - cube.x1);
        float dy = Math.abs(cube.y2 - cube.y1);
        float dz = Math.abs(cube.z2 - cube.z1);
        if (dx > 0.001f && dy > 0.001f && dz > 0.001f) {
            return null;
        }
        Map<class_2350, Boolean> skip = new HashMap<>();
        boolean thinX = dx <= 0.001f;
        boolean thinY = dy <= 0.001f;
        boolean thinZ = dz <= 0.001f;
        int thinCount = (thinX ? 1 : 0) + (thinY ? 1 : 0) + (thinZ ? 1 : 0);
        if (thinCount >= 2) {
            class_2350 keep = findLargestFace(faces);
            for (class_2350 dir : faces.keySet()) {
                if (dir != keep) {
                    skip.put(dir, true);
                }
            }
            return skip;
        }
        if (thinX) {
            skip.put(class_2350.field_11043, true);
            skip.put(class_2350.field_11035, true);
            skip.put(class_2350.field_11036, true);
            skip.put(class_2350.field_11033, true);
            return skip;
        }
        if (thinY) {
            skip.put(class_2350.field_11043, true);
            skip.put(class_2350.field_11035, true);
            skip.put(class_2350.field_11034, true);
            skip.put(class_2350.field_11039, true);
            return skip;
        }
        if (thinZ) {
            skip.put(class_2350.field_11034, true);
            skip.put(class_2350.field_11039, true);
            skip.put(class_2350.field_11036, true);
            skip.put(class_2350.field_11033, true);
            return skip;
        }
        return skip;
    }

    private static class_2350 findLargestFace(Map<class_2350, BedrockItemGeoModel.BedrockFace> faces) {
        class_2350 best = null;
        float bestArea = -1.0f;
        for (Map.Entry<class_2350, BedrockItemGeoModel.BedrockFace> entry : faces.entrySet()) {
            BedrockItemGeoModel.BedrockFace face = entry.getValue();
            if (face != null) {
                float area = Math.abs(face.uSize * face.vSize);
                if (area > bestArea) {
                    bestArea = area;
                    best = entry.getKey();
                }
            }
        }
        return best;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void emitFace(class_4587.class_4665 pose, class_4588 consumer, class_2350 direction, BedrockItemGeoModel.BedrockCube cube, BedrockItemGeoModel.BedrockFace face, int packedLight, int texW, int texH, float colorScale, float alphaScale, TextureAnim anim, boolean flipNormalsX, boolean cameraTransparent) throws MatchException {
        if (cube == null) {
            return;
        }
        float x1 = cube.x1;
        float y1 = cube.y1;
        float z1 = cube.z1;
        float x2 = cube.x2;
        float y2 = cube.y2;
        float z2 = cube.z2;
        float uPx = face.u;
        float vPx = face.v;
        float uSize = face.uSize;
        float vSize = face.vSize;
        int rotation = ((face.rotation % 360) + 360) % 360;
        int rotSteps = (rotation / 90) % 4;
        float uWidth = texW > 0 ? (uPx + uSize) / texW : 0.0f;
        float vHeight = texH > 0 ? (vPx + vSize) / texH : 0.0f;
        float u = texW > 0 ? uPx / texW : 0.0f;
        float v = texH > 0 ? vPx / texH : 0.0f;
        Vector3f normal = new Vector3f(direction.method_10148(), direction.method_10164(), direction.method_10165());
        if (flipNormalsX) {
            normal.x = -normal.x;
        }
        if (!cube.mirror) {
            uWidth = u;
            u = uWidth;
        } else {
            normal.mul(-1.0f, 1.0f, 1.0f);
        }
        float[] uvs = rotateUvs(u, v, uWidth, vHeight, rotSteps);
        Vector3f[] vertices = getQuadVertices(direction, x1, y1, z1, x2, y2, z2, cube.mirror);
        if (vertices == null) {
            return;
        }
        if (flipNormalsX) {
            Vector3f tmpV = vertices[1];
            vertices[1] = vertices[3];
            vertices[3] = tmpV;
            float tmpU = uvs[2];
            float tmpVuv = uvs[3];
            uvs[2] = uvs[6];
            uvs[3] = uvs[7];
            uvs[6] = tmpU;
            uvs[7] = tmpVuv;
        }
        if (cube.hasRotation) {
            Matrix4f rotationMatrix = new Matrix4f().translate(cube.pivotX, cube.pivotY, cube.pivotZ).rotateZ(cube.rotZ).rotateY(cube.rotY).rotateX(cube.rotX).translate(-cube.pivotX, -cube.pivotY, -cube.pivotZ);
            Matrix3f normalMatrix = new Matrix3f().rotateZ(cube.rotZ).rotateY(cube.rotY).rotateX(cube.rotX);
            for (Vector3f vector3f : vertices) {
                rotationMatrix.transformPosition(vector3f);
            }
            normalMatrix.transform(normal);
        }
        if (cameraTransparent && !isInsideVisibleFace(pose, vertices, normal, flipNormalsX)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Vector3f vertex = vertices[i];
            float uNorm = class_3532.method_15363(uvs[i * 2], 0.0f, 1.0f);
            float vNorm = class_3532.method_15363(uvs[(i * 2) + 1], 0.0f, 1.0f);
            if (anim != null) {
                vNorm = (vNorm + anim.frameIndex) * anim.invFrames;
            }
            addVertex(pose, consumer, vertex.x, vertex.y, vertex.z, uNorm, vNorm, packedLight, normal.x, normal.y, normal.z, colorScale, alphaScale);
        }
    }

    private static boolean isInsideVisibleFace(class_4587.class_4665 pose, Vector3f[] vertices, Vector3f normal, boolean flipNormalsX) {
        if (pose == null || vertices == null || vertices.length < 4) {
            return true;
        }
        Vector3fc[] vector3fcArr = new Vector3f[4];
        Vector3f center = new Vector3f();
        int count = 0;
        for (int i = 0; i < 4; i++) {
            Vector3f vertex = vertices[i];
            if (vertex != null) {
                Vector3f tv = new Vector3f(vertex);
                pose.method_23761().transformPosition(tv);
                vector3fcArr[i] = tv;
                center.add(tv);
                count++;
            }
        }
        if (count < 3) {
            return true;
        }
        center.mul(1.0f / count);
        Vector3f worldNormal = null;
        if (vector3fcArr[0] != null && vector3fcArr[1] != null && vector3fcArr[2] != null) {
            Vector3f e1 = new Vector3f(vector3fcArr[1]).sub(vector3fcArr[0]);
            Vector3f e2 = new Vector3f(vector3fcArr[2]).sub(vector3fcArr[0]);
            worldNormal = e1.cross(e2, new Vector3f());
        }
        if ((worldNormal == null || worldNormal.lengthSquared() <= 1.0E-6f) && normal != null) {
            worldNormal = new Vector3f(normal);
            pose.method_23762().transform(worldNormal);
        }
        if (worldNormal.lengthSquared() <= 1.0E-6f || center.lengthSquared() <= 1.0E-6f) {
            return true;
        }
        worldNormal.normalize();
        Vector3f toCamera = new Vector3f(center).negate().normalize();
        float dot = worldNormal.dot(toCamera);
        return dot >= 0.0f;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static Vector3f[] getQuadVertices(class_2350 direction, float x1, float y1, float z1, float x2, float y2, float z2, boolean mirror) throws MatchException {
        if (direction == null) {
            return null;
        }
        Vector3f bottomLeftBack = new Vector3f(x1, y1, z1);
        Vector3f bottomRightBack = new Vector3f(x1, y1, z2);
        Vector3f topLeftBack = new Vector3f(x1, y2, z1);
        Vector3f topRightBack = new Vector3f(x1, y2, z2);
        Vector3f topLeftFront = new Vector3f(x2, y2, z1);
        Vector3f topRightFront = new Vector3f(x2, y2, z2);
        Vector3f bottomLeftFront = new Vector3f(x2, y1, z1);
        Vector3f bottomRightFront = new Vector3f(x2, y1, z2);
        switch (AnonymousClass1.$SwitchMap$net$minecraft$core$Direction[direction.ordinal()]) {
            case 1:
                if (mirror) {
                    return new Vector3f[]{topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
                }
                return new Vector3f[]{topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
            case 2:
                if (mirror) {
                    return new Vector3f[]{topRightBack, topLeftBack, bottomLeftBack, bottomRightBack};
                }
                return new Vector3f[]{topLeftFront, topRightFront, bottomRightFront, bottomLeftFront};
            case 3:
                return new Vector3f[]{topLeftBack, topLeftFront, bottomLeftFront, bottomLeftBack};
            case 4:
                return new Vector3f[]{topRightFront, topRightBack, bottomRightBack, bottomRightFront};
            case 5:
                if (mirror) {
                    return new Vector3f[]{bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};
                }
                return new Vector3f[]{topRightBack, topRightFront, topLeftFront, topLeftBack};
            case 6:
                if (mirror) {
                    return new Vector3f[]{topRightBack, topRightFront, topLeftFront, topLeftBack};
                }
                return new Vector3f[]{bottomLeftBack, bottomLeftFront, bottomRightFront, bottomRightBack};
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: renamed from: punchy.client.bedrockitems.BedrockItemRenderer$1, reason: invalid class name */
    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemRenderer$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$minecraft$core$Direction = new int[class_2350.values().length];

        static {
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11039.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11034.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11043.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11035.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11036.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$net$minecraft$core$Direction[class_2350.field_11033.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private static float[] rotateUvs(float u, float v, float uWidth, float vHeight, int rotSteps) {
        switch (rotSteps & 3) {
            case 0:
                return new float[]{u, v, uWidth, v, uWidth, vHeight, u, vHeight};
            case 1:
                return new float[]{uWidth, v, uWidth, vHeight, u, vHeight, u, v};
            case 2:
                return new float[]{uWidth, vHeight, u, vHeight, u, v, uWidth, v};
            default:
                return new float[]{u, vHeight, u, v, uWidth, v, uWidth, vHeight};
        }
    }

    private static void addVertex(class_4587.class_4665 pose, class_4588 consumer, float x, float y, float z, float u, float v, int packedLight, float nx, float ny, float nz, float colorScale, float alphaScale) {
        int color = scaleToColor(colorScale);
        int alpha = scaleToColor(alphaScale);
        consumer.method_22918(pose.method_23761(), x, y, z).method_1336(color, color, color, alpha).method_22913(u, v).method_22922(class_4608.field_21444).method_60803(packedLight).method_60831(pose, nx, ny, nz);
    }

    private static boolean isPendulumSuppressed(String boneName, Map<String, String> parentByBone, Set<String> suppressed) {
        if (boneName == null || suppressed == null || suppressed.isEmpty()) {
            return false;
        }
        String current = boneName;
        for (int guard = 0; current != null && guard < 64; guard++) {
            if (suppressed.contains(current)) {
                return true;
            }
            current = parentByBone != null ? parentByBone.get(current) : null;
        }
        return false;
    }

    private static boolean isCameraTransparentBone(String boneName, Map<String, String> parentByBone, Set<String> cameraTransparentBones) {
        if (boneName == null || cameraTransparentBones == null || cameraTransparentBones.isEmpty()) {
            return false;
        }
        String current = boneName;
        for (int guard = 0; current != null && guard < 64; guard++) {
            if (cameraTransparentBones.contains(current)) {
                return true;
            }
            current = parentByBone != null ? parentByBone.get(current) : null;
        }
        return false;
    }

    private static int getBoneEmission(Map<String, Integer> emissionByBone, String boneName) {
        Integer value;
        if (emissionByBone == null || emissionByBone.isEmpty() || boneName == null || (value = emissionByBone.get(boneName)) == null) {
            return 0;
        }
        return value.intValue();
    }

    private static float emissionToBaseIntensity(int emission) {
        if (emission <= 0) {
            return 0.0f;
        }
        if (emission <= 15) {
            return emission / 15.0f;
        }
        return 1.0f;
    }

    private static float emissionToExtraGlow(int emission) {
        if (emission <= 15) {
            return 0.0f;
        }
        return Math.min(1.0f, (emission - 15) / 85.0f);
    }

    private static int scaleToColor(float scale) {
        if (scale <= 0.0f) {
            return 0;
        }
        if (scale >= 1.0f) {
            return 255;
        }
        return Math.max(0, Math.min(255, Math.round(255.0f * scale)));
    }

    private static class_1921 emissiveOverlay(class_2960 texture, boolean blur, boolean clamp, boolean shaderPackInUse) {
        if (shaderPackInUse) {
            return class_1921.method_42600(texture);
        }
        EmissiveKey key = new EmissiveKey(texture, blur, clamp);
        class_1921 cached = EMISSIVE_OVERLAY_TYPES.get(key);
        if (cached != null) {
            return cached;
        }
        class_1921 created = tryCreateEmissiveOverlay(texture, blur, clamp);
        if (created == null) {
            created = class_1921.method_42600(texture);
        }
        EMISSIVE_OVERLAY_TYPES.put(key, created);
        return created;
    }

    private static RenderPipeline createEmissiveOverlayPipeline(boolean additive) {
        try {
            Class<?> pipelinesClass = Class.forName("net.minecraft.client.renderer.RenderPipelines");
            Field snippetField = pipelinesClass.getDeclaredField("MATRICES_COLOR_FOG_LIGHT_DIR_SNIPPET");
            snippetField.setAccessible(true);
            Object snippet = snippetField.get(null);
            if (!(snippet instanceof RenderPipeline.Snippet)) {
                return class_10799.field_56907;
            }
            RenderPipeline.Snippet snippetValue = (RenderPipeline.Snippet) snippet;
            Method register = pipelinesClass.getDeclaredMethod("register", RenderPipeline.class);
            register.setAccessible(true);
            RenderPipeline pipeline = RenderPipeline.builder(new RenderPipeline.Snippet[]{snippetValue}).withLocation("pipeline/punchy_emissive_overlay").withVertexShader("core/entity").withFragmentShader("core/entity").withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler0").withBlend(additive ? BlendFunction.ADDITIVE : BlendFunction.TRANSLUCENT).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withCull(false).withVertexFormat(class_290.field_1580, VertexFormat.class_5596.field_27382).build();
            Object result = register.invoke(null, pipeline);
            if (!(result instanceof RenderPipeline)) {
                return class_10799.field_56907;
            }
            RenderPipeline value = (RenderPipeline) result;
            return value;
        } catch (Throwable th) {
            return class_10799.field_56907;
        }
    }

    private static class_1921 tryCreateEmissiveOverlay(class_2960 texture, boolean blur, boolean clamp) {
        try {
            Class<?> compositeStateClass = Class.forName("net.minecraft.client.renderer.RenderType$CompositeState");
            Method builderMethod = compositeStateClass.getDeclaredMethod("builder", new Class[0]);
            builderMethod.setAccessible(true);
            Object builder = builderMethod.invoke(null, new Object[0]);
            Class<?> builderClass = builder.getClass();
            Class<?> emptyTextureStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard");
            Class<?> overlayStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$OverlayStateShard");
            Class<?> layeringStateClass = Class.forName("net.minecraft.client.renderer.RenderStateShard$LayeringStateShard");
            Constructor<?> textureCtor = emptyTextureStateClass.getDeclaredConstructor(Runnable.class, Runnable.class);
            textureCtor.setAccessible(true);
            Runnable setupTexture = () -> {
                class_1044 boundTexture;
                class_310 client = class_310.method_1551();
                if (client == null || (boundTexture = client.method_1531().method_4619(texture)) == null) {
                    return;
                }
                boundTexture.method_65923(blur ? class_9851.field_52394 : class_9851.field_52395, blur);
                boundTexture.method_65924(clamp);
                RenderSystem.setShaderTexture(0, boundTexture.method_68004());
            };
            Object textureState = textureCtor.newInstance(setupTexture, () -> {
            });
            Method setTextureState = builderClass.getDeclaredMethod("setTextureState", emptyTextureStateClass);
            setTextureState.setAccessible(true);
            Object builder2 = setTextureState.invoke(builder, textureState);
            Object overlayState = getRenderStateShardField("NO_OVERLAY");
            if (overlayState != null) {
                Method setOverlayState = builderClass.getDeclaredMethod("setOverlayState", overlayStateClass);
                setOverlayState.setAccessible(true);
                builder2 = setOverlayState.invoke(builder2, overlayState);
            }
            Object layeringState = getRenderStateShardField("VIEW_OFFSET_Z_LAYERING");
            if (layeringState != null) {
                Method setLayeringState = builderClass.getDeclaredMethod("setLayeringState", layeringStateClass);
                setLayeringState.setAccessible(true);
                builder2 = setLayeringState.invoke(builder2, layeringState);
            }
            Method createCompositeState = builderClass.getDeclaredMethod("createCompositeState", Boolean.TYPE);
            createCompositeState.setAccessible(true);
            Object compositeState = createCompositeState.invoke(builder2, true);
            Method create = class_1921.class.getDeclaredMethod("create", String.class, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, RenderPipeline.class, compositeStateClass);
            create.setAccessible(true);
            RenderPipeline pipeline = blur ? EMISSIVE_BLUR_OVERLAY_PIPELINE : EMISSIVE_OVERLAY_PIPELINE;
            Object[] objArr = new Object[6];
            objArr[0] = blur ? "punchy_emissive_blur_overlay" : "punchy_emissive_overlay";
            objArr[1] = 1536;
            objArr[2] = true;
            objArr[3] = true;
            objArr[4] = pipeline;
            objArr[5] = compositeState;
            Object renderType = create.invoke(null, objArr);
            if (!(renderType instanceof class_1921)) {
                return null;
            }
            class_1921 value = (class_1921) renderType;
            return value;
        } catch (Throwable th) {
            return null;
        }
    }

    private static Object getRenderStateShardField(String name) {
        try {
            Class<?> shardClass = Class.forName("net.minecraft.client.renderer.RenderStateShard");
            Field field = shardClass.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable th) {
            return null;
        }
    }

    private static boolean isShaderPackInUse() {
        Boolean irisState = queryIrisShaderPackInUse();
        return Boolean.TRUE.equals(irisState);
    }

    private static Boolean queryIrisShaderPackInUse() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object value = apiClass.getMethod("isShaderPackInUse", new Class[0]).invoke(api, new Object[0]);
            if (value instanceof Boolean) {
                Boolean bool = (Boolean) value;
                return bool;
            }
        } catch (Throwable th) {
        }
        try {
            Class<?> apiClass2 = Class.forName("net.coderbot.iris.api.v0.IrisApi");
            Object api2 = apiClass2.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            Object value2 = apiClass2.getMethod("isShaderPackInUse", new Class[0]).invoke(api2, new Object[0]);
            if (value2 instanceof Boolean) {
                Boolean bool2 = (Boolean) value2;
                return bool2;
            }
            return null;
        } catch (Throwable th2) {
            return null;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey.class */
    private static final class EmissiveKey extends Record {
        private final class_2960 texture;
        private final boolean blur;
        private final boolean clamp;

        private EmissiveKey(class_2960 texture, boolean blur, boolean clamp) {
            this.texture = texture;
            this.blur = blur;
            this.clamp = clamp;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, EmissiveKey.class), EmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->blur:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->clamp:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, EmissiveKey.class), EmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->blur:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->clamp:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, EmissiveKey.class, Object.class), EmissiveKey.class, "texture;blur;clamp", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->texture:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->blur:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$EmissiveKey;->clamp:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_2960 texture() {
            return this.texture;
        }

        public boolean blur() {
            return this.blur;
        }

        public boolean clamp() {
            return this.clamp;
        }
    }

    private static TextureAnim resolveTextureAnim(int frames, int frameTime, int frameHeightOverride, int texturePixelWidth, int texturePixelHeight, int texW, int texH, float animTime) {
        if (frames <= 1 || frameTime <= 0 || texturePixelWidth <= 0 || texturePixelHeight <= 0 || texW <= 0 || texH <= 0) {
            return null;
        }
        int frameHeight = frameHeightOverride > 0 ? frameHeightOverride : texH;
        int expectedHeight = frameHeight * frames;
        if (texturePixelHeight != expectedHeight) {
            return null;
        }
        int frameIndex = ((int) (animTime / frameTime)) % frames;
        float invFrames = 1.0f / frames;
        return new TextureAnim(frameIndex, invFrames);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemRenderer$TextureAnim.class */
    private static final class TextureAnim extends Record {
        private final int frameIndex;
        private final float invFrames;

        private TextureAnim(int frameIndex, float invFrames) {
            this.frameIndex = frameIndex;
            this.invFrames = invFrames;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, TextureAnim.class), TextureAnim.class, "frameIndex;invFrames", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->frameIndex:I", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->invFrames:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, TextureAnim.class), TextureAnim.class, "frameIndex;invFrames", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->frameIndex:I", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->invFrames:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, TextureAnim.class, Object.class), TextureAnim.class, "frameIndex;invFrames", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->frameIndex:I", "FIELD:Lpunchy/client/bedrockitems/BedrockItemRenderer$TextureAnim;->invFrames:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public int frameIndex() {
            return this.frameIndex;
        }

        public float invFrames() {
            return this.invFrames;
        }
    }

    private static TextureAnim resolveAnimForBone(BedrockItemDefinition.TextureAnimSpec spec, int defaultFrames, int defaultFrameTime, int defaultFrameHeight, BedrockItemDefinition def, class_2960 textureId, int texW, int texH, float animTime) {
        int frames = defaultFrames;
        int frameTime = defaultFrameTime;
        int frameHeight = defaultFrameHeight;
        if (spec != null) {
            frames = spec.frames;
            frameTime = spec.frameTime;
            frameHeight = spec.frameHeight;
        }
        int[] size = def != null ? def.getTexturePixelSize(textureId) : null;
        int pixelW = size != null ? size[0] : 0;
        int pixelH = size != null ? size[1] : 0;
        return resolveTextureAnim(frames, frameTime, frameHeight, pixelW, pixelH, texW, texH, animTime);
    }

    private static List<String> mergeParticleEffects(List<String> primary, List<String> secondary) {
        if ((primary == null || primary.isEmpty()) && (secondary == null || secondary.isEmpty())) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (primary != null && !primary.isEmpty()) {
            out.addAll(primary);
        }
        if (secondary != null && !secondary.isEmpty()) {
            out.addAll(secondary);
        }
        return out;
    }
}
