package punchy.mixin.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1787;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_630;
import net.minecraft.class_898;
import net.minecraft.class_906;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import punchy.Punchy;
import punchy.client.access.TransformablePart;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.model.SimpleBedrockGeo;
import punchy.client.render.CameraAnimationState;
import punchy.client.render.FishingLineProxyTuning;
import punchy.client.render.PunchyArmRenderer;
import punchy.client.state.MovementStateMachine;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/FishingHookRendererMixin.class */
@Mixin({class_906.class})
public abstract class FishingHookRendererMixin {
    @Inject(method = {"method_59755(Lnet/minecraft/class_1657;FF)Lnet/minecraft/class_243;"}, at = {@At("HEAD")}, cancellable = true)
    private void punchy$useAnimatedGrip(class_1657 player, float handAngle, float partialTick, CallbackInfoReturnable<class_243> cir) {
        class_898 dispatcher;
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 != player || (dispatcher = mc.method_1561()) == null || dispatcher.field_4692 == null || !dispatcher.field_4692.method_31044().method_31034()) {
            return;
        }
        if (SimpleBedrockGeo.loadedParts.isEmpty() || (!SimpleBedrockGeo.loadedParts.containsKey("camera_animation") && !SimpleBedrockGeo.loadedParts.containsKey("camera"))) {
            class_2960 playerGeoId = class_2960.method_60655(Punchy.MOD_ID, "geo/player.geo.json");
            SimpleBedrockGeo.load(playerGeoId);
        }
        class_630 cameraAnim = SimpleBedrockGeo.loadedParts.get("camera_animation");
        class_630 camera = SimpleBedrockGeo.loadedParts.get("camera");
        if (cameraAnim == null) {
            cameraAnim = camera;
        }
        class_630 body = SimpleBedrockGeo.loadedParts.get("body");
        class_630 rightArm = SimpleBedrockGeo.loadedParts.get("right_arm");
        class_630 leftArm = SimpleBedrockGeo.loadedParts.get("left_arm");
        class_630 rightItem = SimpleBedrockGeo.loadedParts.get("item_right");
        class_630 leftItem = SimpleBedrockGeo.loadedParts.get("item_left");
        class_630 rightGrip = SimpleBedrockGeo.loadedParts.get("itemgrip_right");
        class_630 leftGrip = SimpleBedrockGeo.loadedParts.get("itemgrip_left");
        if (cameraAnim == null || camera == null || body == null) {
            return;
        }
        resetPose(cameraAnim);
        resetPose(camera);
        resetPose(body);
        resetPose(rightArm);
        resetPose(leftArm);
        resetPose(rightItem);
        resetPose(leftItem);
        resetPose(rightGrip);
        resetPose(leftGrip);
        Map<String, class_630> geoMap = new HashMap<>();
        geoMap.put("camera_animation", cameraAnim);
        geoMap.put("camera", camera);
        geoMap.put("body", body);
        if (rightArm != null) {
            geoMap.put("right_arm", rightArm);
        }
        if (leftArm != null) {
            geoMap.put("left_arm", leftArm);
        }
        if (rightItem != null) {
            geoMap.put("item_right", rightItem);
        }
        if (leftItem != null) {
            geoMap.put("item_left", leftItem);
        }
        if (rightGrip != null) {
            geoMap.put("itemgrip_right", rightGrip);
        }
        if (leftGrip != null) {
            geoMap.put("itemgrip_left", leftGrip);
        }
        PunchyAnimationManager.POSE_HANDLER.applyTransformations(geoMap, partialTick);
        PunchyAnimationManager.SWIM_HANDLER.applyTransformations(geoMap, partialTick, false);
        CameraAnimationState.update(camera);
        class_1306 arm = resolveHoldingArm(player);
        class_630 armPart = arm == class_1306.field_6182 ? leftArm : rightArm;
        class_630 itemPart = arm == class_1306.field_6182 ? leftItem : rightItem;
        class_630 gripPart = arm == class_1306.field_6182 ? leftGrip : rightGrip;
        if (armPart == null || itemPart == null || gripPart == null) {
            return;
        }
        Matrix4f cameraTransform = resolveJointTransform(cameraAnim, camera);
        Matrix4f rootTransform = resolveJointTransform(cameraAnim, camera, body);
        Matrix4f armLocal = getLocalMatrix(armPart);
        Matrix4f itemLocal = getLocalMatrix(itemPart);
        Matrix4f gripLocal = getLocalMatrix(gripPart);
        Matrix4f itemTransform = null;
        Matrix4f cachedArmWorld = player == null ? null : PunchyArmRenderer.getCachedArmWorld(mc.field_1724, arm);
        if (cachedArmWorld != null && itemLocal != null && gripLocal != null) {
            itemTransform = new Matrix4f(cachedArmWorld).mul(itemLocal).mul(gripLocal);
        } else if (rootTransform != null && armLocal != null && itemLocal != null && gripLocal != null) {
            itemTransform = new Matrix4f(rootTransform);
            MovementStateMachine movement = MovementStateMachine.get();
            if (!PunchyConfig.disableArmPhysics()) {
                movement.applyCameraLookToRoot(itemTransform);
            }
            if (!PunchyConfig.disableArmPhysics()) {
                movement.applyRootSpaceMotion(itemTransform, partialTick, arm);
                movement.applyShieldImpactToRoot(itemTransform, partialTick, arm);
            }
            itemTransform.mul(armLocal);
            if (MovementStateMachine.APPLY_TO_ARMS) {
                movement.applyToMatrix(itemTransform, partialTick, arm);
            }
            movement.applyCameraYawLagToArm(itemTransform);
            itemTransform.mul(itemLocal);
            itemTransform.mul(gripLocal);
        }
        if (cameraTransform == null || itemTransform == null) {
            return;
        }
        Matrix4f cameraCenter = new Matrix4f().scale(0.0625f).scale(1.0f, -1.0f, -1.0f).mul(cameraTransform).translate(0.0f, 0.0f, -5.0f);
        Matrix4f itemGrip = new Matrix4f().scale(0.0625f).scale(1.0f, -1.0f, -1.0f).mul(itemTransform);
        float fovScale = ((Integer) dispatcher.field_4692.method_41808().method_41753()).intValue();
        Matrix4f blended = cameraCenter.lerp(itemGrip, 2.0408163E-4f * fovScale * fovScale);
        FishingLineProxyTuning.Profile lineProfile = FishingLineProxyTuning.get(FishingLineProxyTuning.Kind.ROD, arm == class_1306.field_6182);
        if (lineProfile != null) {
            Matrix4f tuning = new Matrix4f().scale(lineProfile.scale).rotateX((float) Math.toRadians(lineProfile.rotX)).rotateY((float) Math.toRadians(lineProfile.rotY)).rotateZ((float) Math.toRadians(lineProfile.rotZ)).translate(lineProfile.offX, lineProfile.offY, lineProfile.offZ);
            blended.mul(tuning);
        }
        float pitch = player.method_36455() * 0.017453292f;
        float yaw = player.method_36454() * (-0.017453292f);
        Quaternionf playerRotation = new Quaternionf().rotateY(yaw).rotateX(pitch);
        class_243 cameraPos = dispatcher.field_4686.method_19326();
        Matrix4f playerTransform = new Matrix4f().translate(new Vector3f((float) cameraPos.field_1352, (float) cameraPos.field_1351, (float) cameraPos.field_1350)).rotate(playerRotation);
        Vector3f gripPos = playerTransform.translate(blended.getTranslation(new Vector3f())).rotate(blended.getNormalizedRotation(new Quaternionf())).getTranslation(new Vector3f());
        cir.setReturnValue(new class_243(gripPos));
    }

    private static void resetPose(class_630 part) {
        if (part != null) {
            part.method_41923();
        }
    }

    private static Matrix4f resolveJointTransform(class_630... parts) {
        if (parts == null || parts.length == 0) {
            return null;
        }
        Matrix4f out = new Matrix4f();
        for (class_630 part : parts) {
            if (part == null) {
                return null;
            }
            out.mul(getLocalMatrix(part));
        }
        return out;
    }

    private static Matrix4f getLocalMatrix(class_630 part) {
        if (part == null) {
            return null;
        }
        if (part instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) part;
            Matrix4f explicit = transformable.punchy$getExplicitTransform();
            if (explicit != null) {
                return new Matrix4f(explicit);
            }
        }
        return new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
    }

    private static class_1306 resolveHoldingArm(class_1657 player) {
        if (player == null) {
            return class_1306.field_6183;
        }
        class_1799 main = player.method_6047();
        class_1799 off = player.method_6079();
        boolean mainRod = isFishingRod(main);
        boolean offRod = isFishingRod(off);
        if (mainRod && !offRod) {
            return player.method_6068();
        }
        if (offRod && !mainRod) {
            return player.method_6068() == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183;
        }
        class_1268 used = player.method_6058();
        if (used == class_1268.field_5810) {
            return player.method_6068() == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183;
        }
        return player.method_6068();
    }

    private static boolean isFishingRod(class_1799 stack) {
        return stack != null && (stack.method_7909() instanceof class_1787);
    }
}
