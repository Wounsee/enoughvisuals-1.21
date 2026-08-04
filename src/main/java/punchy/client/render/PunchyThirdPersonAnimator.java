package punchy.client.render;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_742;
import org.joml.Matrix4f;
import punchy.Punchy;
import punchy.client.access.TransformablePart;
import punchy.client.animation.PunchyThirdPersonAnimationManager;
import punchy.client.model.SimpleBedrockGeoThirdPerson;
import punchy.compat.FirstPersonModelCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/PunchyThirdPersonAnimator.class */
public final class PunchyThirdPersonAnimator {
    private static final String GEO_PATH = "geo/player_3_per.geo.json";

    private PunchyThirdPersonAnimator() {
    }

    public static void apply(class_742 player, class_591 model, float partialTicks) {
        class_310 mc;
        if (player == null || model == null || (mc = class_310.method_1551()) == null || mc.field_1690 == null) {
            return;
        }
        if (mc.field_1690.method_31044().method_31034()) {
            if (FirstPersonModelCompat.shouldApplyThirdPersonAnimations(mc)) {
                clearModel(model);
                return;
            } else {
                clearModel(model);
                return;
            }
        }
        if (mc.field_1724 != player) {
            clearModel(model);
            return;
        }
        ensureGeoLoaded();
        Map<String, class_630> geoMap = SimpleBedrockGeoThirdPerson.loadedParts;
        if (geoMap.isEmpty()) {
            clearModel(model);
            return;
        }
        PunchyThirdPersonAnimationManager.POSE_HANDLER.applyTransformations(geoMap, partialTicks);
        class_630 geoHead = geoMap.get("head");
        class_630 geoBody = geoMap.get("body");
        class_630 geoLeftArm = geoMap.get("left_arm");
        class_630 geoRightArm = geoMap.get("right_arm");
        class_630 geoLeftLeg = geoMap.get("left_leg");
        class_630 geoRightLeg = geoMap.get("right_leg");
        applyWorldPart("head", model.field_3398, geoHead, geoMap);
        applyWorldPart("body", model.field_3391, geoBody, geoMap);
        applyWorldPart("left_arm", model.field_27433, geoLeftArm, geoMap);
        applyWorldPart("right_arm", model.field_3401, geoRightArm, geoMap);
        applyWorldPart("left_leg", model.field_3397, geoLeftLeg, geoMap);
        applyWorldPart("right_leg", model.field_3392, geoRightLeg, geoMap);
        applyLocalPart("hat", "head", model.field_3394, geoMap);
        applyLocalPart("jacket", "body", model.field_3483, geoMap);
        applyLocalPart("left_sleeve", "left_arm", model.field_3484, geoMap);
        applyLocalPart("right_sleeve", "right_arm", model.field_3486, geoMap);
        applyLocalPart("left_pants", "left_leg", model.field_3482, geoMap);
        applyLocalPart("right_pants", "right_leg", model.field_3479, geoMap);
    }

    private static void ensureGeoLoaded() {
        if (!SimpleBedrockGeoThirdPerson.loadedParts.isEmpty()) {
            return;
        }
        class_2960 geoId = class_2960.method_60655(Punchy.MOD_ID, GEO_PATH);
        SimpleBedrockGeoThirdPerson.load(geoId);
    }

    private static void applyWorldPart(String boneName, class_630 target, class_630 source, Map<String, class_630> geoMap) {
        if (target == null) {
            return;
        }
        boolean[] hasExplicit = new boolean[1];
        Matrix4f world = buildWorldMatrix(boneName, geoMap, new HashSet(), hasExplicit);
        Matrix4f restWorld = SimpleBedrockGeoThirdPerson.restWorldByName.get(boneName);
        if (world == null || restWorld == null || !hasExplicit[0]) {
            clearPart(target);
            return;
        }
        Matrix4f delta = new Matrix4f(restWorld).invert().mul(world);
        Matrix4f base = getVanillaBaseMatrix(target);
        Matrix4f finalWorld = base.mul(delta);
        if (target instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) target;
            transformable.punchy$setExplicitTransform(finalWorld);
        }
    }

    private static void applyLocalPart(String boneName, String parentBone, class_630 target, Map<String, class_630> geoMap) {
        if (target == null) {
            return;
        }
        boolean[] hasExplicit = new boolean[1];
        Matrix4f localDelta = buildLocalDeltaMatrix(boneName, parentBone, geoMap, hasExplicit);
        if (localDelta == null || !hasExplicit[0]) {
            clearPart(target);
            return;
        }
        Matrix4f base = getVanillaBaseMatrix(target);
        Matrix4f finalLocal = base.mul(localDelta);
        if (target instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) target;
            transformable.punchy$setExplicitTransform(finalLocal);
        }
    }

    private static Matrix4f getExplicitTransform(class_630 part) {
        if (part != null && (part instanceof TransformablePart)) {
            TransformablePart transformable = (TransformablePart) part;
            Matrix4f explicit = transformable.punchy$getExplicitTransform();
            if (explicit != null) {
                return new Matrix4f(explicit);
            }
            return null;
        }
        return null;
    }

    private static Matrix4f buildWorldMatrix(String boneName, Map<String, class_630> geoMap, Set<String> visited, boolean[] hasExplicit) {
        if (boneName == null || geoMap == null || visited.contains(boneName)) {
            return null;
        }
        visited.add(boneName);
        class_630 part = geoMap.get(boneName);
        if (part == null) {
            return null;
        }
        Matrix4f local = getLocalMatrix(part, hasExplicit);
        String parent = SimpleBedrockGeoThirdPerson.parentByName.get(boneName);
        if (parent == null) {
            return local;
        }
        Matrix4f parentWorld = buildWorldMatrix(parent, geoMap, visited, hasExplicit);
        if (parentWorld == null) {
            return local;
        }
        return parentWorld.mul(local, new Matrix4f());
    }

    private static Matrix4f buildLocalDeltaMatrix(String boneName, String parentBone, Map<String, class_630> geoMap, boolean[] hasExplicit) {
        if (boneName == null || parentBone == null || geoMap == null) {
            return null;
        }
        boolean[] childExplicit = new boolean[1];
        boolean[] parentExplicit = new boolean[1];
        Matrix4f childWorld = buildWorldMatrix(boneName, geoMap, new HashSet(), childExplicit);
        Matrix4f parentWorld = buildWorldMatrix(parentBone, geoMap, new HashSet(), parentExplicit);
        Matrix4f childRest = SimpleBedrockGeoThirdPerson.restWorldByName.get(boneName);
        Matrix4f parentRest = SimpleBedrockGeoThirdPerson.restWorldByName.get(parentBone);
        boolean explicit = childExplicit[0] || parentExplicit[0];
        if (hasExplicit != null) {
            hasExplicit[0] = explicit;
        }
        if (childWorld == null || parentWorld == null || childRest == null || parentRest == null) {
            return null;
        }
        Matrix4f localAnim = new Matrix4f(parentWorld).invert().mul(childWorld);
        Matrix4f localRest = new Matrix4f(parentRest).invert().mul(childRest);
        return new Matrix4f(localRest).invert().mul(localAnim);
    }

    private static Matrix4f getLocalMatrix(class_630 part, boolean[] hasExplicit) {
        Matrix4f explicit = getExplicitTransform(part);
        if (explicit != null) {
            if (hasExplicit != null) {
                hasExplicit[0] = true;
            }
            return new Matrix4f(explicit);
        }
        return new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
    }

    private static Matrix4f getVanillaBaseMatrix(class_630 part) {
        return new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
    }

    private static void clearPart(class_630 part) {
        if (part != null && (part instanceof TransformablePart)) {
            TransformablePart transformable = (TransformablePart) part;
            transformable.punchy$setExplicitTransform(null);
        }
    }

    private static void clearModel(class_591 model) {
        if (model == null) {
            return;
        }
        clearPart(model.field_3398);
        clearPart(model.field_3391);
        clearPart(model.field_27433);
        clearPart(model.field_3401);
        clearPart(model.field_3397);
        clearPart(model.field_3392);
        clearPart(model.field_3394);
        clearPart(model.field_3483);
        clearPart(model.field_3484);
        clearPart(model.field_3486);
        clearPart(model.field_3482);
        clearPart(model.field_3479);
    }
}
