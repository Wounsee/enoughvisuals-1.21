package punchy.client.state;

import java.util.LinkedHashSet;
import java.util.Locale;
import net.minecraft.class_10255;
import net.minecraft.class_1306;
import net.minecraft.class_1747;
import net.minecraft.class_1753;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_1835;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_5819;
import net.minecraft.class_746;
import org.joml.Matrix4f;
import punchy.client.PunchyClient;
import punchy.client.animation.Easing;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MovementStateMachine.class */
public class MovementStateMachine {
    private static final boolean LOG_UNDERWATER_SINK_POSE = true;
    private static final float FORWARD_OFFSET = 1.6f;
    private static final float STRAFE_OFFSET = 1.5f;
    private static final float SMOOTH = 0.3f;
    private static final float CAMERA_LOOK_REFERENCE_DT = 0.016666668f;
    private static final float CAMERA_LOOK_SPRING_STEP_DT = 0.008333334f;
    private static final float CAMERA_LOOK_MIN_DT = 1.0E-4f;
    private static final float IDLE_BALANCE_INTENSITY_SCALE = 3.3f;
    private static final float IDLE_BALANCE_SPEED_BONUS = 0.0f;
    private static final float IDLE_BALANCE_DECAY = 1.0f;
    private static final float IDLE_BALANCE_RAMP = 0.55f;
    private static final float IDLE_BALANCE_JITTER_SCALE = 0.0078f;
    private static final float IDLE_BALANCE_JITTER_RAMP = 0.35f;
    private static final float IDLE_BALANCE_JITTER_MIN = 0.35f;
    private static final float IDLE_BALANCE_JITTER_MAX = 0.8f;
    private static final float SPRINT_BLEND_IN_TIME = 0.07f;
    private static final float SPRINT_BLEND_OUT_TIME = 0.35f;
    private static final float SPRINT_SPEED_SCALE = 1.6f;
    private static final float SPRINT_PHASE_SPEED = 6.63f;
    private static final float SPRINT_PHASE_SPEED_MAX = 9.69f;
    private static final float SPRINT_PITCH_AMP = 0.135f;
    private static final float SPRINT_PITCH_AMP_2 = 0.06f;
    private static final float SPRINT_YAW_AMP = 0.0375f;
    private static final float SPRINT_YAW_AMP_2 = 0.015f;
    private static final float SPRINT_ROLL_AMP = 0.0525f;
    private static final float SPRINT_ROLL_AMP_2 = 0.0225f;
    private static final float SPRINT_FWD_AMP = 3.6f;
    private static final float SPRINT_FWD_AMP_2 = 1.56f;
    private static final float SPRINT_FWD_PITCH_UP = 0.28f;
    private static final float SPRINT_INWARD_YAW = 0.045f;
    private static final float SPRINT_INWARD_ROLL = 0.03f;
    private static final float SPRINT_UP_AMP = 0.12f;
    private static final float SPRINT_TILT_BOOST = 1.28f;
    private static final float WALK_BLEND_IN_TIME = 0.35f;
    private static final float WALK_BLEND_OUT_TIME = 0.6f;
    private static final float WALK_PHASE_SPEED = 5.31f;
    private static final float WALK_PHASE_SPEED_MAX = 8.13f;
    private static final float WALK_PITCH_AMP = 0.085f;
    private static final float WALK_PITCH_AMP_2 = 0.035f;
    private static final float WALK_YAW_AMP = 0.03f;
    private static final float WALK_YAW_AMP_2 = 0.012f;
    private static final float WALK_ROLL_AMP = 0.038f;
    private static final float WALK_ROLL_AMP_2 = 0.016f;
    private static final float WALK_SIDE_AMP = 1.85f;
    private static final float WALK_SIDE_UP_AMP = 0.11f;
    private static final float WALK_UP_AMP = 0.06f;
    private static final float WALK_INWARD_YAW = 0.022f;
    private static final float WALK_TILT_BOOST = 1.22f;
    private static final float FALL_ORBIT_BLEND_IN_TIME = 0.16f;
    private static final float FALL_ORBIT_BLEND_OUT_TIME = 0.2f;
    private static final float FALL_ORBIT_PHASE_SPEED = -5.4f;
    private static final float FALL_ORBIT_PHASE_SPEED_MAX = -8.8f;
    private static final float FALL_ORBIT_FWD_AMP = 1.17f;
    private static final float FALL_ORBIT_UP_AMP = 0.68f;
    private static final float FALL_ORBIT_PITCH_AMP = 0.14f;
    private static final float FALL_ORBIT_ROLL_AMP = 0.1f;
    private static final float FLIGHT_POSE_BLEND_IN_TIME = 0.52f;
    private static final float FLIGHT_POSE_BLEND_OUT_TIME = 0.24f;
    private static final float ELYTRA_POSE_SPEED_MIN = 0.55f;
    private static final float ELYTRA_POSE_SPEED_MAX = 1.75f;
    private static final float CREATIVE_POSE_SPEED_MIN = 0.16f;
    private static final float FLIGHT_POSE_BACK_HIGH_SCALE = 1.12f;
    private static final float FLIGHT_POSE_INWARD_DEGREES = 35.0f;
    private static final float FLIGHT_POSE_SIDE = 0.0f;
    private static final float FLIGHT_POSE_FORWARD = 5.8f;
    private static final float FLIGHT_POSE_UP = -0.55f;
    private static final float FLIGHT_POSE_LIFT = 0.0f;
    private static final float FLIGHT_POSE_SWAY_SPEED = 1.15f;
    private static final float FLIGHT_POSE_SWAY_SIDE_AMP = 0.12f;
    private static final float FLIGHT_POSE_SWAY_UP_AMP = 0.012f;
    private static final float UNDERWATER_SINK_POSE_BLEND_OUT_TIME = 0.26f;
    private static final float UNDERWATER_SINK_POSE_BLEND_IN_TIME = 2.25f;
    private static final float UNDERWATER_SINK_POSE_DESCEND_MIN = 0.0f;
    private static final float UNDERWATER_SINK_POSE_DESCEND_MAX = 0.08f;
    private static final float UNDERWATER_SINK_POSE_FORWARD = 4.6400003f;
    private static final float UNDERWATER_SINK_POSE_UP = 3.168f;
    private static final float UNDERWATER_SINK_POSE_SIDE = 0.6f;
    private static final float UNDERWATER_SINK_POSE_PITCH = -0.09f;
    private static final float UNDERWATER_SINK_POSE_ROLL = 0.03f;
    private static final String SYNC_WALK_ARMS_EFFECT = "sync_walk_arms";
    private static final float SYNC_WALK_MOTION_SCALE = 0.7f;
    private static final float SYNC_ROOT_MOTION_SCALE = 0.75f;
    private static final float SYNC_WALK_ROTATION_SCALE = 0.38f;
    private static final float SYNC_WALK_SIDE_SCALE = 1.35f;
    private static final float SYNC_WALK_UP_SCALE = 1.12f;
    private float offsetX;
    private float offsetZ;
    private float jumpValue;
    private float jumpVelocity;
    private float impactValue;
    private float impactVelocity;
    private class_1306 impactArm;
    private float shieldImpactValue;
    private float shieldImpactVelocity;
    private class_1306 shieldImpactArm;
    private int airborneTicks;
    private float airborneMaxFallDistance;
    private boolean airborneJumped;
    private boolean animGrounded;
    private float cameraTiltY;
    private float cameraTiltZ;
    private float cameraArmTiltY;
    private float cameraArmTiltYVelocity;
    private float cameraPitchOffset;
    private float cameraPitchTiltX;
    private float cameraForwardX;
    private float cameraForwardY;
    private float cameraForwardZ;
    private float cameraRightX;
    private float cameraRightY;
    private float cameraRightZ;
    private float cameraUpX;
    private float cameraUpY;
    private float cameraUpZ;
    private float idleTime;
    private float idleWeight;
    private float idleBoostLeft;
    private float idleBoostRight;
    private float idleBoostLeftTarget;
    private float idleBoostRightTarget;
    private float idleJitterLeft;
    private float idleJitterRight;
    private float idleJitterLeftTarget;
    private float idleJitterRightTarget;
    private float idleJitterLeftTimer;
    private float idleJitterRightTimer;
    private boolean suppressWalkSway;
    private float sprintBlend;
    private float sprintPhase;
    private float sprintPhaseVelocity;
    private float sprintTarget;
    private float sprintSpeedFactor;
    private float sprintDt;
    private float walkBlend;
    private float walkPhase;
    private float walkPhaseVelocity;
    private float walkSpeedFactor;
    private boolean isGrounded;
    private float fallOrbitBlend;
    private float fallOrbitPhase;
    private float fallOrbitPhaseVelocity;
    private float fallOrbitSpeedFactor;
    private float flightPoseBlend;
    private float underwaterSinkPoseBlend;
    private String lastLoggedUnderwaterSinkReason;
    private static final double MICRO_AIR_VEL_EPS = 0.1d;
    private static final double VEL_IMPULSE_THRESHOLD = 0.1d;
    private static final int MICRO_AIR_TICKS_MAX = 1;
    private static final float LAND_FALLDIST_THRESHOLD = 0.05f;
    private static final float AIR_FREEZE_FALLDIST_THRESHOLD = 3.0f;
    private static final float JUMP_LAND_TRANSITION_COOLDOWN_S = 0.08f;
    private float jumpLandTransitionCooldown;
    private static final MovementStateMachine INSTANCE = new MovementStateMachine();
    public static boolean APPLY_TO_ARMS = true;
    public static float WALK_SCALE = 1.0f;
    public static float JUMP_IMPULSE = -2.8f;
    public static float LAND_IMPULSE = 2.4f;
    public static float JUMP_STIFFNESS = 75.0f;
    public static float JUMP_DAMPING = 16.0f;
    private static final float CREATIVE_POSE_SPEED_MAX = 0.85f;
    public static float JUMP_OFFSET_SCALE = CREATIVE_POSE_SPEED_MAX;
    public static float JUMP_ROT_SCALE = 1.65f;
    public static float CAMERA_TILT_SMOOTH = 0.45f;
    private static final float WALK_INWARD_ROLL = 0.018f;
    public static float CAMERA_YAW_TILT_Y_SCALE = WALK_INWARD_ROLL;
    public static float CAMERA_YAW_TILT_Z_SCALE = 0.005f;
    public static float CAMERA_YAW_ARM_TILT_Y_SCALE = 0.03264f;
    public static float CAMERA_YAW_ARM_TILT_MAX = (float) Math.toRadians(48.0d);
    public static float CAMERA_YAW_ARM_TILT_STIFFNESS = 64.0f;
    public static float CAMERA_YAW_ARM_TILT_DAMPING = 15.0f;
    public static float CAMERA_PITCH_OFFSET_SCALE = 0.01105f;
    public static float CAMERA_PITCH_TILT_X_SCALE = 0.027625f;
    public static float SPEAR_CHARGE_IMPACT_SCALE = 0.5f;
    public static float IMPACT_STIFFNESS = 60.0f;
    public static float IMPACT_DAMPING = 15.5f;
    public static float SHIELD_BLOCK_IMPACT_SCALE = 0.5f;
    public static float SHIELD_IMPACT_STIFFNESS = 60.0f;
    public static float SHIELD_IMPACT_DAMPING = 15.5f;
    public static float ACTION_ANIM_IMPULSE_SCALE = 0.5f;
    public static float ACTION_ANIM_STIFFNESS = 60.0f;
    public static float ACTION_ANIM_DAMPING = 15.5f;
    public static float IDLE_INTENSITY = 2.6f;
    public static float IDLE_SPEED = 2.0f;
    private static final float SPRINT_INWARD_YAW_STATIC = (float) Math.toRadians(25.0d);
    private static final float UNDERWATER_SINK_POSE_INWARD_YAW = (float) Math.toRadians(20.0d);
    private final float[] actionAnimValue = new float[2];
    private final float[] actionAnimVelocity = new float[2];
    private final float[] actionAnimKick = new float[2];
    private boolean prevOnGround = true;
    private double airborneMinVy = 0.0d;
    private long lastUpdateTime = System.nanoTime();
    private float lastCameraYaw = Float.NaN;
    private float lastCameraPitch = Float.NaN;
    private final class_5819 idleJitterRandom = class_5819.method_43047();
    private final SneakSpringState sneakSpring = new SneakSpringState();
    private final float[] sprintRotX = new float[2];
    private final float[] sprintRotY = new float[2];
    private final float[] sprintRotZ = new float[2];
    private final float[] sprintOffFwd = new float[2];
    private final float[] sprintOffUp = new float[2];
    private final float[] sprintLateralGate = {1.0f, 1.0f};
    private final float[] walkRotX = new float[2];
    private final float[] walkRotY = new float[2];
    private final float[] walkRotZ = new float[2];
    private final float[] walkOffSide = new float[2];
    private final float[] walkOffUp = new float[2];
    private float lastLoggedUnderwaterSinkTarget = Float.NaN;
    private float lastLoggedUnderwaterSinkBlend = Float.NaN;

    public static MovementStateMachine get() {
        return INSTANCE;
    }

    public void triggerSpearImpact(class_1306 arm) {
        this.impactArm = arm;
        this.impactVelocity += SPEAR_CHARGE_IMPACT_SCALE * 40.0f;
    }

    public void triggerShieldBlockImpact(class_1306 arm) {
        this.shieldImpactArm = arm;
        this.shieldImpactVelocity += SHIELD_BLOCK_IMPACT_SCALE * 20.0f;
    }

    public void triggerActionAnimation(class_1306 arm) {
        if (arm == null) {
            return;
        }
        int idx = arm == class_1306.field_6182 ? 0 : 1;
        float[] fArr = this.actionAnimVelocity;
        fArr[idx] = fArr[idx] + (ACTION_ANIM_IMPULSE_SCALE * 20.0f);
        float[] fArr2 = this.actionAnimKick;
        fArr2[idx] = fArr2[idx] + 1.0f;
    }

    public void tick(class_310 client) {
    }

    public void updateFrame(class_746 player, float partialTicks) {
        class_243 right;
        class_243 up;
        float f;
        long now = System.nanoTime();
        float dt = (now - this.lastUpdateTime) / 1.0E9f;
        this.lastUpdateTime = now;
        if (dt <= 0.0f) {
            return;
        }
        if (dt > 0.033333335f) {
            dt = 0.033333335f;
        }
        float lerpFactor = 1.0f - ((float) Math.pow(0.699999988079071d, dt * 20.0f));
        float camLerp = 1.0f - ((float) Math.pow(1.0f - CAMERA_TILT_SMOOTH, dt * 20.0f));
        this.sprintDt = dt;
        float targetX = 0.0f;
        float targetZ = 0.0f;
        if (player != null) {
            float forwardBack = class_3532.method_15363(player.field_6250, -1.0f, 1.0f);
            float strafe = class_3532.method_15363(player.field_6212, -1.0f, 1.0f);
            targetX = strafe * STRAFE_OFFSET;
            targetZ = (-forwardBack) * 1.6f;
            this.suppressWalkSway = player.method_5854() instanceof class_10255;
        } else {
            this.suppressWalkSway = false;
        }
        float targetX2 = targetX * WALK_SCALE;
        float targetZ2 = targetZ * WALK_SCALE;
        this.offsetX = class_3532.method_16439(lerpFactor, this.offsetX, targetX2);
        this.offsetZ = class_3532.method_16439(lerpFactor, this.offsetZ, targetZ2);
        if (player != null) {
            if (this.jumpLandTransitionCooldown > 0.0f) {
                this.jumpLandTransitionCooldown = Math.max(0.0f, this.jumpLandTransitionCooldown - dt);
            }
            float clampedPitch = class_3532.method_15363(player.method_36455(), -89.5f, 89.5f);
            class_243 forward = class_243.method_1030(clampedPitch, player.method_36454());
            this.cameraForwardX = (float) forward.field_1352;
            this.cameraForwardY = (float) forward.field_1351;
            this.cameraForwardZ = (float) forward.field_1350;
            class_243 worldUp = new class_243(0.0d, 1.0d, 0.0d);
            class_243 right2 = forward.method_1036(worldUp);
            if (right2.method_1027() < 1.0E-6d) {
                right = new class_243(1.0d, 0.0d, 0.0d);
            } else {
                right = right2.method_1029();
            }
            class_243 up2 = right.method_1036(forward);
            if (up2.method_1027() < 1.0E-6d) {
                up = worldUp;
            } else {
                up = up2.method_1029();
            }
            this.cameraRightX = (float) right.field_1352;
            this.cameraRightY = (float) right.field_1351;
            this.cameraRightZ = (float) right.field_1350;
            this.cameraUpX = (float) up.field_1352;
            this.cameraUpY = (float) up.field_1351;
            this.cameraUpZ = (float) up.field_1350;
            boolean slimeBelow = isStandingOnSlime(player);
            boolean onGround = player.method_24828();
            if (!onGround && slimeBelow) {
                double vy = player.method_18798().field_1351;
                if (Math.abs(vy) <= 0.1d && player.field_6017 <= 0.0d) {
                    onGround = true;
                }
            }
            this.isGrounded = onGround;
            if (this.prevOnGround && !onGround) {
                this.airborneTicks = 0;
                this.airborneMaxFallDistance = 0.0f;
                this.airborneMinVy = player.method_18798().field_1351;
                boolean jumped = class_310.method_1551().field_1690.field_1903.method_1434();
                this.airborneJumped = jumped || player.method_18798().field_1351 > 0.1d;
                float impulse = jumped ? JUMP_IMPULSE : JUMP_IMPULSE * 0.5f;
                if ((!slimeBelow || jumped) && this.jumpLandTransitionCooldown <= 0.0f && (jumped || player.method_18798().field_1351 > 0.1d)) {
                    this.jumpVelocity += impulse;
                    this.jumpLandTransitionCooldown = 0.08f;
                }
            } else if (!this.prevOnGround && onGround) {
                boolean realAir = this.airborneTicks > 1 || this.airborneMaxFallDistance > LAND_FALLDIST_THRESHOLD || this.airborneMinVy < -0.1d;
                if (realAir && this.jumpLandTransitionCooldown <= 0.0f) {
                    float landImpulse = LAND_IMPULSE;
                    if (slimeBelow) {
                        float f2 = class_3532.method_15363(this.airborneMaxFallDistance / 1.0f, 0.0f, 1.0f);
                        landImpulse *= 0.35f + (0.65f * f2);
                    }
                    this.jumpVelocity += landImpulse;
                    this.jumpLandTransitionCooldown = 0.08f;
                }
                this.airborneTicks = 0;
                this.airborneMaxFallDistance = 0.0f;
                this.airborneMinVy = 0.0d;
                this.airborneJumped = false;
            }
            this.prevOnGround = onGround;
            if (!onGround) {
                this.airborneTicks++;
                this.airborneMaxFallDistance = Math.max(this.airborneMaxFallDistance, (float) player.field_6017);
                this.airborneMinVy = Math.min(this.airborneMinVy, player.method_18798().field_1351);
            }
            boolean allowAirAdvance = (this.isGrounded || this.airborneJumped || this.airborneMaxFallDistance >= AIR_FREEZE_FALLDIST_THRESHOLD) ? false : true;
            this.animGrounded = this.isGrounded || allowAirAdvance;
            boolean submergedAirborne = isSubmergedWithoutGround(player);
            float fallOrbitTargetBlend = 0.0f;
            if (PunchyConfig.enableFallArmAnimation() && !onGround && !submergedAirborne && !player.method_6128() && !this.suppressWalkSway) {
                float downwardSpeed = (float) Math.max(0.0d, -player.method_18798().field_1351);
                fallOrbitTargetBlend = class_3532.method_15363((downwardSpeed - 0.04f) / CREATIVE_POSE_SPEED_MAX, 0.0f, 1.0f);
                if (player.field_6017 > 0.15000000596046448d) {
                    fallOrbitTargetBlend = Math.max(fallOrbitTargetBlend, (float) class_3532.method_15350(player.field_6017 / 3.0d, 0.0d, 1.0d));
                }
            }
            float fallBlendTime = fallOrbitTargetBlend > this.fallOrbitBlend ? 0.16f : FALL_ORBIT_BLEND_OUT_TIME;
            float fallBlendStep = fallBlendTime <= 0.0f ? 1.0f : dt / fallBlendTime;
            this.fallOrbitBlend = class_3532.method_15363(this.fallOrbitBlend + ((fallOrbitTargetBlend - this.fallOrbitBlend) * fallBlendStep), 0.0f, 1.0f);
            this.fallOrbitSpeedFactor = class_3532.method_16439(fallBlendStep, this.fallOrbitSpeedFactor, fallOrbitTargetBlend);
            if (this.fallOrbitBlend > 0.001f) {
                float targetFallPhase = class_3532.method_16439(this.fallOrbitSpeedFactor, FALL_ORBIT_PHASE_SPEED, FALL_ORBIT_PHASE_SPEED_MAX);
                float fallPhaseLerp = 1.0f - ((float) Math.pow(0.7400000095367432d, dt * 20.0f));
                this.fallOrbitPhaseVelocity = class_3532.method_16439(fallPhaseLerp, this.fallOrbitPhaseVelocity, targetFallPhase);
                this.fallOrbitPhase += this.fallOrbitPhaseVelocity * dt;
            } else {
                this.fallOrbitPhaseVelocity = 0.0f;
            }
            float flightPoseTargetBlend = resolveFlightPoseTargetBlend(player);
            float flightBlendTime = flightPoseTargetBlend > this.flightPoseBlend ? FLIGHT_POSE_BLEND_IN_TIME : FLIGHT_POSE_BLEND_OUT_TIME;
            float flightBlendStep = flightBlendTime <= 0.0f ? 1.0f : dt / flightBlendTime;
            this.flightPoseBlend = class_3532.method_15363(this.flightPoseBlend + ((flightPoseTargetBlend - this.flightPoseBlend) * flightBlendStep), 0.0f, 1.0f);
            float underwaterSinkTargetBlend = resolveUnderwaterSinkPoseTargetBlend(player);
            if (underwaterSinkTargetBlend > this.underwaterSinkPoseBlend) {
                f = UNDERWATER_SINK_POSE_BLEND_IN_TIME;
            } else {
                f = UNDERWATER_SINK_POSE_BLEND_OUT_TIME;
            }
            float underwaterSinkBlendTime = f;
            float underwaterSinkBlendStep = underwaterSinkBlendTime <= 0.0f ? 1.0f : dt / underwaterSinkBlendTime;
            this.underwaterSinkPoseBlend = approach(this.underwaterSinkPoseBlend, underwaterSinkTargetBlend, underwaterSinkBlendStep);
            debugUnderwaterSinkPoseBlend(player, underwaterSinkTargetBlend);
            float yaw = player.method_36454();
            if (Float.isNaN(this.lastCameraYaw)) {
                this.lastCameraYaw = yaw;
            }
            float yawDelta = normalizeLookDelta(class_3532.method_15393(yaw - this.lastCameraYaw), dt);
            this.lastCameraYaw = yaw;
            float targetTiltY = (-yawDelta) * CAMERA_YAW_TILT_Y_SCALE;
            this.cameraTiltY = class_3532.method_16439(camLerp, this.cameraTiltY, targetTiltY);
            float targetTiltZ = yawDelta * CAMERA_YAW_TILT_Z_SCALE;
            this.cameraTiltZ = class_3532.method_16439(camLerp, this.cameraTiltZ, targetTiltZ);
            float targetArmTiltY = class_3532.method_15363((-yawDelta) * CAMERA_YAW_ARM_TILT_Y_SCALE, -CAMERA_YAW_ARM_TILT_MAX, CAMERA_YAW_ARM_TILT_MAX);
            stepCameraArmTiltSpring(targetArmTiltY, dt);
            float pitch = player.method_36455();
            if (Float.isNaN(this.lastCameraPitch)) {
                this.lastCameraPitch = pitch;
            }
            float pitchDelta = normalizeLookDelta(pitch - this.lastCameraPitch, dt);
            this.lastCameraPitch = pitch;
            float targetPitchTiltX = (-pitchDelta) * CAMERA_PITCH_TILT_X_SCALE;
            this.cameraPitchTiltX = class_3532.method_16439(camLerp, this.cameraPitchTiltX, targetPitchTiltX);
            float targetPitchOffset = pitch * CAMERA_PITCH_OFFSET_SCALE;
            this.cameraPitchOffset = class_3532.method_16439(camLerp, this.cameraPitchOffset, targetPitchOffset);
            float moveMag = class_3532.method_15355((targetZ2 * targetZ2) + (targetX2 * targetX2));
            float idleTarget = 1.0f - class_3532.method_15363(moveMag / FALL_ORBIT_BLEND_OUT_TIME, 0.0f, 1.0f);
            this.idleWeight = class_3532.method_16439(camLerp, this.idleWeight, idleTarget);
            boolean sneaking = player.method_18276() || player.method_5715();
            this.sneakSpring.setSneaking(sneaking);
            if (this.animGrounded && !submergedAirborne) {
                boolean sprintAllowed = shouldApplySprintSwing(player);
                float walkSpeed = player.field_42108.method_48566();
                float targetSprint = sprintAllowed ? 1.0f : 0.0f;
                this.sprintTarget = targetSprint;
                float blendTime = targetSprint > this.sprintBlend ? SPRINT_BLEND_IN_TIME : 0.35f;
                float blendStep = blendTime <= 0.0f ? 1.0f : dt / blendTime;
                if (targetSprint < this.sprintBlend) {
                    blendStep *= blendStep;
                }
                this.sprintBlend = class_3532.method_15363(this.sprintBlend + ((targetSprint - this.sprintBlend) * blendStep), 0.0f, 1.0f);
                float targetSprintSpeed = sprintAllowed ? class_3532.method_15363(walkSpeed * 1.6f, 0.0f, 1.0f) : 0.0f;
                float sprintSpeedLerp = blendTime <= 0.0f ? 1.0f : dt / blendTime;
                this.sprintSpeedFactor = class_3532.method_16439(sprintSpeedLerp, this.sprintSpeedFactor, targetSprintSpeed);
                float targetPhaseSpeed = class_3532.method_16439(this.sprintSpeedFactor, SPRINT_PHASE_SPEED, SPRINT_PHASE_SPEED_MAX);
                float sprintSpeedAdjust = PunchyConfig.getSprintArmSwingSpeed();
                float speedScale = 1.0f + sprintSpeedAdjust;
                if (speedScale < 0.0f) {
                    speedScale = 0.0f;
                }
                float targetPhaseSpeed2 = targetPhaseSpeed * speedScale;
                float phaseLerp = 1.0f - ((float) Math.pow(0.75d, dt * 20.0f));
                this.sprintPhaseVelocity = class_3532.method_16439(phaseLerp, this.sprintPhaseVelocity, targetPhaseSpeed2);
                this.sprintPhase += this.sprintPhaseVelocity * dt;
                float inputMag = class_3532.method_15355((player.field_6250 * player.field_6250) + (player.field_6212 * player.field_6212));
                float walkAnim = player.field_42108.method_48566();
                float moveFactor = class_3532.method_15363(Math.max(inputMag, walkAnim), 0.0f, 1.0f);
                boolean walkAllowed = shouldApplyWalkSwing(player, moveFactor);
                float targetWalk = walkAllowed ? 1.0f : 0.0f;
                float walkBlendTime = targetWalk > this.walkBlend ? 0.35f : 0.6f;
                float walkBlendStep = walkBlendTime <= 0.0f ? 1.0f : dt / walkBlendTime;
                if (targetWalk < this.walkBlend) {
                    walkBlendStep *= walkBlendStep;
                }
                this.walkBlend = class_3532.method_15363(this.walkBlend + ((targetWalk - this.walkBlend) * walkBlendStep), 0.0f, 1.0f);
                float walkSpeedLerp = walkBlendTime <= 0.0f ? 1.0f : dt / walkBlendTime;
                float targetWalkSpeedFactor = walkAllowed ? moveFactor : 0.0f;
                this.walkSpeedFactor = class_3532.method_16439(walkSpeedLerp, this.walkSpeedFactor, targetWalkSpeedFactor);
                float walkTargetPhase = class_3532.method_16439(this.walkSpeedFactor, WALK_PHASE_SPEED, WALK_PHASE_SPEED_MAX);
                float walkPhaseLerp = 1.0f - ((float) Math.pow(0.75d, dt * 20.0f));
                this.walkPhaseVelocity = class_3532.method_16439(walkPhaseLerp, this.walkPhaseVelocity, walkTargetPhase);
                this.walkPhase += this.walkPhaseVelocity * dt;
                if (this.walkBlend <= 0.001f && !walkAllowed) {
                    this.walkBlend = 0.0f;
                    this.walkPhaseVelocity = 0.0f;
                    this.walkRotX[0] = 0.0f;
                    this.walkRotX[1] = 0.0f;
                    this.walkRotY[0] = 0.0f;
                    this.walkRotY[1] = 0.0f;
                    this.walkRotZ[0] = 0.0f;
                    this.walkRotZ[1] = 0.0f;
                    this.walkOffSide[0] = 0.0f;
                    this.walkOffSide[1] = 0.0f;
                    this.walkOffUp[0] = 0.0f;
                    this.walkOffUp[1] = 0.0f;
                }
            } else if (submergedAirborne) {
                float fadeStep = class_3532.method_15363(dt / 0.22f, 0.0f, 1.0f);
                this.sprintTarget = 0.0f;
                this.sprintBlend = class_3532.method_16439(fadeStep, this.sprintBlend, 0.0f);
                this.sprintSpeedFactor = class_3532.method_16439(fadeStep, this.sprintSpeedFactor, 0.0f);
                this.walkBlend = class_3532.method_16439(fadeStep, this.walkBlend, 0.0f);
                this.walkSpeedFactor = class_3532.method_16439(fadeStep, this.walkSpeedFactor, 0.0f);
                if (this.sprintBlend <= 0.001f) {
                    this.sprintBlend = 0.0f;
                    this.sprintPhaseVelocity = 0.0f;
                }
                if (this.walkBlend <= 0.001f) {
                    this.walkBlend = 0.0f;
                    this.walkPhaseVelocity = 0.0f;
                }
            }
        } else {
            this.underwaterSinkPoseBlend = 0.0f;
        }
        this.idleTime += dt;
        if (this.idleBoostLeftTarget > 0.0f) {
            this.idleBoostLeftTarget = Math.max(0.0f, this.idleBoostLeftTarget - (dt * 1.0f));
        }
        if (this.idleBoostRightTarget > 0.0f) {
            this.idleBoostRightTarget = Math.max(0.0f, this.idleBoostRightTarget - (dt * 1.0f));
        }
        float boostLerp = 1.0f - ((float) Math.pow(0.44999998807907104d, dt * 20.0f));
        this.idleBoostLeft = class_3532.method_16439(boostLerp, this.idleBoostLeft, this.idleBoostLeftTarget);
        this.idleBoostRight = class_3532.method_16439(boostLerp, this.idleBoostRight, this.idleBoostRightTarget);
        updateIdleJitter(dt);
        float accel = ((-JUMP_STIFFNESS) * this.jumpValue) - (JUMP_DAMPING * this.jumpVelocity);
        this.jumpVelocity += accel * dt;
        this.jumpValue += this.jumpVelocity * dt;
        if (Math.abs(this.jumpValue) < CAMERA_LOOK_MIN_DT && Math.abs(this.jumpVelocity) < CAMERA_LOOK_MIN_DT) {
            this.jumpValue = 0.0f;
            this.jumpVelocity = 0.0f;
        }
        float impactAccel = ((-IMPACT_STIFFNESS) * this.impactValue) - (IMPACT_DAMPING * this.impactVelocity);
        this.impactVelocity += impactAccel * dt;
        this.impactValue += this.impactVelocity * dt;
        if (Math.abs(this.impactValue) < CAMERA_LOOK_MIN_DT && Math.abs(this.impactVelocity) < CAMERA_LOOK_MIN_DT) {
            this.impactValue = 0.0f;
            this.impactVelocity = 0.0f;
            this.impactArm = null;
        }
        float shieldImpactAccel = ((-SHIELD_IMPACT_STIFFNESS) * this.shieldImpactValue) - (SHIELD_IMPACT_DAMPING * this.shieldImpactVelocity);
        this.shieldImpactVelocity += shieldImpactAccel * dt;
        this.shieldImpactValue += this.shieldImpactVelocity * dt;
        if (Math.abs(this.shieldImpactValue) < CAMERA_LOOK_MIN_DT && Math.abs(this.shieldImpactVelocity) < CAMERA_LOOK_MIN_DT) {
            this.shieldImpactValue = 0.0f;
            this.shieldImpactVelocity = 0.0f;
            this.shieldImpactArm = null;
        }
        for (int i = 0; i < 2; i++) {
            float actionAccel = ((-ACTION_ANIM_STIFFNESS) * this.actionAnimValue[i]) - (ACTION_ANIM_DAMPING * this.actionAnimVelocity[i]);
            float[] fArr = this.actionAnimVelocity;
            int i2 = i;
            fArr[i2] = fArr[i2] + (actionAccel * dt);
            float[] fArr2 = this.actionAnimValue;
            int i3 = i;
            fArr2[i3] = fArr2[i3] + (this.actionAnimVelocity[i] * dt);
            if (Math.abs(this.actionAnimValue[i]) < CAMERA_LOOK_MIN_DT && Math.abs(this.actionAnimVelocity[i]) < CAMERA_LOOK_MIN_DT) {
                this.actionAnimValue[i] = 0.0f;
                this.actionAnimVelocity[i] = 0.0f;
            }
        }
        this.sneakSpring.tick(dt);
    }

    public boolean isSprintSwingActive() {
        return this.sprintTarget > 0.0f || this.sprintBlend > 0.001f;
    }

    public boolean isWalkSwingActive() {
        return this.walkBlend > 0.001f;
    }

    public float getSprintBlendAlpha() {
        return smoothStep(this.sprintBlend) * smoothStep(this.sprintSpeedFactor);
    }

    public void applyToMatrix(Matrix4f matrix, float partialTicks, class_1306 currentArm) {
        if (matrix == null || this.suppressWalkSway) {
            return;
        }
        boolean syncWalkArms = isWalkArmSyncActive();
        float walkSprintScale = getWalkAndSprintForceScale();
        float othersScale = getRemainingProceduralForceScale();
        float rootScale = (syncWalkArms ? SYNC_ROOT_MOTION_SCALE : 1.0f) * walkSprintScale;
        if (0 == 0 && (this.offsetX != 0.0f || this.offsetZ != 0.0f)) {
            if (syncWalkArms) {
                matrix.translateLocal((this.cameraRightX * this.offsetX * rootScale) + (this.cameraForwardX * this.offsetZ * rootScale), (this.cameraRightY * this.offsetX * rootScale) + (this.cameraForwardY * this.offsetZ * rootScale), (this.cameraRightZ * this.offsetX * rootScale) + (this.cameraForwardZ * this.offsetZ * rootScale));
            } else {
                matrix.translate(this.offsetX * rootScale, this.offsetZ * rootScale, 0.0f);
            }
        }
        SwingTransform walk = computeWalkSwing(currentArm);
        SwingTransform sprint = computeSprintSwing(currentArm);
        float sprintAlpha = resolveSprintAlpha(currentArm, partialTicks);
        SwingTransform blended = SwingTransform.blend(walk, sprint, sprintAlpha);
        if (blended != null && currentArm != null) {
            int armIndex = currentArm == class_1306.field_6182 ? 0 : 1;
            float dt = this.sprintDt > 0.0f ? this.sprintDt : LAND_FALLDIST_THRESHOLD;
            float smooth = 1.0f - ((float) Math.pow(0.7200000286102295d, dt * 20.0f));
            float targetGate = isSprintLateralTiltEnabled(currentArm) ? 1.0f : 0.0f;
            this.sprintLateralGate[armIndex] = class_3532.method_16439(smooth, this.sprintLateralGate[armIndex], targetGate);
            float lateralScale = class_3532.method_16439(sprintAlpha, 1.0f, this.sprintLateralGate[armIndex]);
            blended = new SwingTransform(blended.rotX, blended.rotY * lateralScale, blended.rotZ * lateralScale, blended.offX * lateralScale, blended.offY, blended.offZ);
        }
        if (blended != null) {
            float swingScale = (syncWalkArms ? SYNC_WALK_MOTION_SCALE : 1.0f) * walkSprintScale;
            blended = blended.scaled(swingScale);
        }
        if (blended != null && blended.hasMotion()) {
            if (syncWalkArms) {
                applySwingRootSpace(matrix, blended);
            } else {
                matrix.rotateX(blended.rotX);
                matrix.rotateY(blended.rotY);
                matrix.rotateZ(blended.rotZ);
                matrix.translate(blended.offX, blended.offY, blended.offZ);
            }
        }
        applyFallOrbitMotion(matrix, currentArm, syncWalkArms, othersScale);
        applyFlightPoseMotion(matrix, currentArm);
        applyUnderwaterSinkPoseMotion(matrix, currentArm);
    }

    private void applyFallOrbitMotion(Matrix4f matrix, class_1306 currentArm, boolean syncWalkArms, float othersScale) {
        if (matrix == null || currentArm == null || this.fallOrbitBlend <= 0.001f) {
            return;
        }
        int armIndex = currentArm == class_1306.field_6182 ? 0 : 1;
        float f = (syncWalkArms || armIndex == 0) ? 1.0f : -1.0f;
        float armSign = f;
        float f2 = this.fallOrbitPhase;
        float f3 = (!syncWalkArms && armSign < 0.0f) ? 3.1415927f : 0.0f;
        float phase = f2 + f3;
        float w = smoothStep(this.fallOrbitBlend) * othersScale;
        float forward = class_3532.method_15374(phase) * FALL_ORBIT_FWD_AMP * w;
        float up = class_3532.method_15362(phase) * FALL_ORBIT_UP_AMP * w;
        float pitch = class_3532.method_15374(phase + 0.35f) * FALL_ORBIT_PITCH_AMP * w;
        float roll = class_3532.method_15362(phase + 0.12f) * FALL_ORBIT_ROLL_AMP * w * armSign;
        if (syncWalkArms) {
            float pitch2 = pitch * 0.65f;
            float roll2 = roll * 0.65f;
            float forward2 = forward * SYNC_ROOT_MOTION_SCALE;
            float up2 = up * SYNC_ROOT_MOTION_SCALE;
            matrix.rotate(pitch2, this.cameraRightX, this.cameraRightY, this.cameraRightZ);
            matrix.rotate(roll2, this.cameraForwardX, this.cameraForwardY, this.cameraForwardZ);
            matrix.translateLocal((this.cameraForwardX * forward2) + (this.cameraUpX * up2), (this.cameraForwardY * forward2) + (this.cameraUpY * up2), (this.cameraForwardZ * forward2) + (this.cameraUpZ * up2));
            return;
        }
        matrix.rotateX(pitch);
        matrix.rotateZ(roll);
        matrix.translate(0.0f, forward, up);
    }

    private void applyFlightPoseMotion(Matrix4f matrix, class_1306 currentArm) {
        float fMethod_15363;
        if (matrix == null || currentArm == null || this.flightPoseBlend <= 0.001f) {
            return;
        }
        float easedProgress = class_3532.method_15363(this.flightPoseBlend * 1.12f, 0.0f, 1.0f);
        float w = class_3532.method_15363(Easing.easeOutBack(easedProgress), 0.0f, 1.0f);
        class_746 player = class_310.method_1551().field_1724;
        class_243 velocity = player == null ? null : player.method_18798();
        float speed = velocity == null ? 0.0f : (float) velocity.method_1033();
        if (player != null && player.method_6128()) {
            fMethod_15363 = class_3532.method_15363((speed - 0.55f) / 1.2f, 0.0f, 1.0f);
        } else {
            fMethod_15363 = class_3532.method_15363((speed - 0.16f) / 0.69000006f, 0.0f, 1.0f);
        }
        float speedBlend = fMethod_15363;
        float smoothSpeedBlend = smoothStep(speedBlend);
        float swayWeight = w * smoothSpeedBlend * smoothSpeedBlend * smoothSpeedBlend;
        float swayTime = player == null ? 0.0f : player.field_6012 * FLIGHT_POSE_SWAY_SPEED;
        float armSign = currentArm == class_1306.field_6182 ? -1.0f : 1.0f;
        float inwardRot = ((float) Math.toRadians(35.0d)) * w;
        if (currentArm == class_1306.field_6182) {
            matrix.rotateY(inwardRot);
        } else {
            matrix.rotateY(-inwardRot);
        }
        float side = (0.0f + (class_3532.method_15374(swayTime) * 0.12f * armSign)) * swayWeight;
        float forward = FLIGHT_POSE_FORWARD * w;
        float up = (FLIGHT_POSE_UP * w) + (class_3532.method_15374((swayTime * 0.25f) + 0.6f) * 0.012f * swayWeight);
        float lift = 0.0f * w;
        matrix.translate(side, forward + lift, up);
    }

    private void applyUnderwaterSinkPoseMotion(Matrix4f matrix, class_1306 currentArm) {
        if (matrix == null || currentArm == null || this.underwaterSinkPoseBlend <= 0.001f) {
            return;
        }
        float w = this.underwaterSinkPoseBlend;
        float armSign = currentArm == class_1306.field_6182 ? -1.0f : 1.0f;
        float inwardYaw = UNDERWATER_SINK_POSE_INWARD_YAW * w;
        if (currentArm == class_1306.field_6182) {
            matrix.rotateY(inwardYaw);
        } else {
            matrix.rotateY(-inwardYaw);
        }
        matrix.rotateX(UNDERWATER_SINK_POSE_PITCH * w);
        matrix.rotateZ(0.03f * w * armSign);
        matrix.translate((-0.6f) * w * armSign, UNDERWATER_SINK_POSE_FORWARD * w, (-3.168f) * w);
        debugUnderwaterSinkPoseApply(currentArm, w);
    }

    private float resolveFlightPoseTargetBlend(class_746 player) {
        if (player == null || this.suppressWalkSway || player.method_24828() || player.method_5681() || player.method_5799()) {
            return 0.0f;
        }
        if ((PunchyAnimationManager.SWIM_HANDLER.isPlaying() && PunchyAnimationManager.SWIM_HANDLER.getCurrentClip() == PunchyAnimationManager.SWIMING_CLIP) || !PunchyConfig.enableElytraFlightAnimation()) {
            return 0.0f;
        }
        class_243 velocity = player.method_18798();
        float speed = velocity == null ? 0.0f : (float) velocity.method_1033();
        if (player.method_6128()) {
            return class_3532.method_15363((speed - 0.55f) / 1.2f, 0.0f, 1.0f);
        }
        return 0.0f;
    }

    private float resolveUnderwaterSinkPoseTargetBlend(class_746 player) {
        class_310 client = class_310.method_1551();
        if (player == null || client == null || client.field_1690 == null || this.suppressWalkSway) {
            debugUnderwaterSinkPoseState("invalid_context", player, 0.0f, false, false, 0.0f);
            return 0.0f;
        }
        boolean submerged = player.method_5869() || player.method_5777(class_3486.field_15517);
        if (!submerged) {
            debugUnderwaterSinkPoseState("not_submerged", player, 0.0f, false, false, 0.0f);
            return 0.0f;
        }
        if (player.method_5681() || player.method_31549().field_7479 || player.method_6128()) {
            debugUnderwaterSinkPoseState("swimming_or_flying", player, 0.0f, submerged, false, 0.0f);
            return 0.0f;
        }
        if (client.field_1690.field_1903.method_1434()) {
            debugUnderwaterSinkPoseState("jump_pressed", player, 0.0f, submerged, false, 0.0f);
            return 0.0f;
        }
        if (hasSolidSupportAtFeet(player)) {
            debugUnderwaterSinkPoseState("solid_support", player, 0.0f, submerged, false, 0.0f);
            return 0.0f;
        }
        boolean inputActive = Math.abs(player.field_6250) > LAND_FALLDIST_THRESHOLD || Math.abs(player.field_6212) > LAND_FALLDIST_THRESHOLD || client.field_1690.field_1894.method_1434() || client.field_1690.field_1881.method_1434() || client.field_1690.field_1913.method_1434() || client.field_1690.field_1849.method_1434();
        if (inputActive) {
            debugUnderwaterSinkPoseState("wasd_active", player, 0.0f, submerged, false, 0.0f);
            return 0.0f;
        }
        class_243 velocity = player.method_18798();
        float downwardSpeed = velocity == null ? 0.0f : (float) Math.max(0.0d, -velocity.field_1351);
        float target = class_3532.method_15363((downwardSpeed - 0.0f) / 0.08f, 0.0f, 1.0f);
        float resolved = Math.max(1.0f, target);
        debugUnderwaterSinkPoseState("active_idle_sink", player, resolved, submerged, false, downwardSpeed);
        return resolved;
    }

    private boolean hasSolidSupportAtFeet(class_746 player) {
        if (player == null || player.method_37908() == null) {
            return false;
        }
        class_238 bounds = player.method_5829();
        double sampleY = bounds.field_1322 - 0.05d;
        double insetX = Math.min(0.2d, bounds.method_17939() * 0.25d);
        double insetZ = Math.min(0.2d, bounds.method_17941() * 0.25d);
        double[] sampleXs = {bounds.field_1323 + insetX, bounds.field_1320 - insetX, (bounds.field_1323 + bounds.field_1320) * 0.5d};
        double[] sampleZs = {bounds.field_1321 + insetZ, bounds.field_1324 - insetZ, (bounds.field_1321 + bounds.field_1324) * 0.5d};
        for (double sampleX : sampleXs) {
            for (double sampleZ : sampleZs) {
                class_2338 supportPos = class_2338.method_49637(sampleX, sampleY, sampleZ);
                if (player.method_37908().method_8320(supportPos).method_51366()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isFlightPoseActive() {
        return this.flightPoseBlend > 0.001f;
    }

    public float getFlightPoseBlend() {
        return this.flightPoseBlend;
    }

    public boolean isUnderwaterSinkPoseActive() {
        return this.underwaterSinkPoseBlend > 0.001f;
    }

    public float getUnderwaterSinkPoseBlend() {
        return this.underwaterSinkPoseBlend;
    }

    private void debugUnderwaterSinkPoseState(String reason, class_746 player, float target, boolean submerged, boolean moving, float downwardSpeed) {
        if (safeEquals(this.lastLoggedUnderwaterSinkReason, reason) && Math.abs(this.lastLoggedUnderwaterSinkTarget - target) < 0.01f) {
            return;
        }
        this.lastLoggedUnderwaterSinkReason = reason;
        this.lastLoggedUnderwaterSinkTarget = target;
    }

    private void debugUnderwaterSinkPoseBlend(class_746 player, float target) {
        if (Math.abs(this.lastLoggedUnderwaterSinkBlend - this.underwaterSinkPoseBlend) < 0.02f && Math.abs(this.lastLoggedUnderwaterSinkTarget - target) < 0.01f) {
            return;
        }
        this.lastLoggedUnderwaterSinkBlend = this.underwaterSinkPoseBlend;
    }

    private void debugUnderwaterSinkPoseApply(class_1306 arm, float easedBlend) {
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static float approach(float current, float target, float step) {
        float delta = target - current;
        if (Math.abs(delta) <= step) {
            return target;
        }
        return current + Math.copySign(step, delta);
    }

    private static String clipName(AnimationClip clip) {
        return clip == null ? "null" : clip.getName();
    }

    private void applySwingRootSpace(Matrix4f matrix, SwingTransform swing) {
        if (matrix == null || swing == null) {
            return;
        }
        if (swing.rotX != 0.0f) {
            matrix.rotate(swing.rotX, this.cameraRightX, this.cameraRightY, this.cameraRightZ);
        }
        if (swing.rotY != 0.0f) {
            matrix.rotate(swing.rotY, this.cameraUpX, this.cameraUpY, this.cameraUpZ);
        }
        if (swing.rotZ != 0.0f) {
            matrix.rotate(swing.rotZ, this.cameraForwardX, this.cameraForwardY, this.cameraForwardZ);
        }
        if (swing.offX != 0.0f || swing.offY != 0.0f || swing.offZ != 0.0f) {
            matrix.translateLocal((this.cameraRightX * swing.offX) + (this.cameraForwardX * swing.offY) + (this.cameraUpX * swing.offZ), (this.cameraRightY * swing.offX) + (this.cameraForwardY * swing.offY) + (this.cameraUpY * swing.offZ), (this.cameraRightZ * swing.offX) + (this.cameraForwardZ * swing.offY) + (this.cameraUpZ * swing.offZ));
        }
    }

    public void applyRootSpaceMotion(Matrix4f matrix, float partialTicks, class_1306 currentArm) {
        if (matrix == null || this.suppressWalkSway) {
            return;
        }
        boolean syncWalkArms = isWalkArmSyncActive();
        float othersScale = getRemainingProceduralForceScale();
        float rootScale = (syncWalkArms ? SYNC_ROOT_MOTION_SCALE : 1.0f) * othersScale;
        if (this.jumpValue != 0.0f) {
            float offset = this.jumpValue * JUMP_OFFSET_SCALE * rootScale;
            matrix.translateLocal(this.cameraUpX * offset, this.cameraUpY * offset, this.cameraUpZ * offset);
            matrix.rotateX(this.jumpValue * JUMP_ROT_SCALE * rootScale);
        }
        float sneakOffset = this.sneakSpring.offset();
        if (sneakOffset != 0.0f) {
            float offset2 = sneakOffset * JUMP_OFFSET_SCALE * rootScale;
            matrix.translateLocal(this.cameraUpX * offset2, this.cameraUpY * offset2, this.cameraUpZ * offset2);
            matrix.rotateX(sneakOffset * JUMP_ROT_SCALE * rootScale);
        }
        if (this.impactValue != 0.0f && (syncWalkArms || currentArm == this.impactArm)) {
            float recuo = (-this.impactValue) * 2.5f * rootScale;
            float up = this.impactValue * 0.5f * rootScale;
            matrix.translateLocal((this.cameraForwardX * recuo) + (this.cameraUpX * up), (this.cameraForwardY * recuo) + (this.cameraUpY * up), (this.cameraForwardZ * recuo) + (this.cameraUpZ * up));
            matrix.rotateX((-this.impactValue) * 0.6f * rootScale);
        }
        if (!this.suppressWalkSway && this.idleWeight > 0.0f && IDLE_INTENSITY != 0.0f) {
            float boost = syncWalkArms ? (this.idleBoostLeft + this.idleBoostRight) * 0.5f : getIdleBoost(currentArm);
            float speed = IDLE_SPEED * (1.0f + (boost * 0.0f));
            float phase = this.idleTime * Math.max(0.01f, speed);
            float intensity = IDLE_INTENSITY * this.idleWeight * (1.0f + (boost * IDLE_BALANCE_INTENSITY_SCALE)) * rootScale;
            float bob = class_3532.method_15374(phase * SYNC_WALK_SIDE_SCALE) * SPRINT_YAW_AMP_2 * intensity;
            float sway = class_3532.method_15374((phase * 0.9f) + 1.2f) * 0.02f * intensity;
            float tilt = class_3532.method_15374((phase * SYNC_ROOT_MOTION_SCALE) + 0.5f) * 0.012f * intensity;
            matrix.translateLocal((this.cameraForwardX * sway) + (this.cameraUpX * bob), (this.cameraForwardY * sway) + (this.cameraUpY * bob), (this.cameraForwardZ * sway) + (this.cameraUpZ * bob));
            matrix.rotateX(tilt);
            float jitter = syncWalkArms ? (this.idleJitterLeft + this.idleJitterRight) * 0.5f : getIdleJitter(currentArm);
            if (jitter != 0.0f && boost > 0.0f) {
                float lateral = jitter * IDLE_BALANCE_JITTER_SCALE * boost * othersScale;
                matrix.translateLocal(this.cameraRightX * lateral, this.cameraRightY * lateral, this.cameraRightZ * lateral);
            }
        }
    }

    public void applyCameraLookToRoot(Matrix4f matrix) {
        if (matrix == null || this.suppressWalkSway) {
            return;
        }
        float othersScale = getRemainingProceduralForceScale();
        if (this.cameraTiltY != 0.0f) {
            matrix.rotate(this.cameraTiltY * othersScale, this.cameraUpX, this.cameraUpY, this.cameraUpZ);
        }
        if (this.cameraTiltZ != 0.0f) {
            matrix.rotate(this.cameraTiltZ * othersScale, this.cameraForwardX, this.cameraForwardY, this.cameraForwardZ);
        }
        if (this.cameraPitchTiltX != 0.0f) {
            matrix.rotate(this.cameraPitchTiltX * othersScale, this.cameraRightX, this.cameraRightY, this.cameraRightZ);
        }
        if (this.cameraPitchOffset != 0.0f) {
            matrix.translate(this.cameraUpX * this.cameraPitchOffset * othersScale, this.cameraUpY * this.cameraPitchOffset * othersScale, this.cameraUpZ * this.cameraPitchOffset * othersScale);
        }
    }

    public void applyCameraYawLagToArm(Matrix4f matrix) {
        if (matrix != null && !PunchyConfig.disableArmPhysics() && this.cameraArmTiltY != 0.0f) {
            float othersScale = getRemainingProceduralForceScale();
            matrix.rotateY(this.cameraArmTiltY * othersScale);
        }
    }

    private static float normalizeLookDelta(float delta, float dt) {
        return delta * (CAMERA_LOOK_REFERENCE_DT / Math.max(dt, CAMERA_LOOK_MIN_DT));
    }

    private void stepCameraArmTiltSpring(float targetArmTiltY, float dt) {
        float fMax = Math.max(dt, 0.0f);
        while (true) {
            float remainingDt = fMax;
            if (remainingDt > 0.0f) {
                float stepDt = Math.min(remainingDt, CAMERA_LOOK_SPRING_STEP_DT);
                float armYawAccel = (CAMERA_YAW_ARM_TILT_STIFFNESS * (targetArmTiltY - this.cameraArmTiltY)) - (CAMERA_YAW_ARM_TILT_DAMPING * this.cameraArmTiltYVelocity);
                this.cameraArmTiltYVelocity += armYawAccel * stepDt;
                this.cameraArmTiltY += this.cameraArmTiltYVelocity * stepDt;
                if (this.cameraArmTiltY > CAMERA_YAW_ARM_TILT_MAX) {
                    this.cameraArmTiltY = CAMERA_YAW_ARM_TILT_MAX;
                    if (this.cameraArmTiltYVelocity > 0.0f) {
                        this.cameraArmTiltYVelocity = 0.0f;
                    }
                } else if (this.cameraArmTiltY < (-CAMERA_YAW_ARM_TILT_MAX)) {
                    this.cameraArmTiltY = -CAMERA_YAW_ARM_TILT_MAX;
                    if (this.cameraArmTiltYVelocity < 0.0f) {
                        this.cameraArmTiltYVelocity = 0.0f;
                    }
                }
                fMax = remainingDt - stepDt;
            } else {
                return;
            }
        }
    }

    public float getCameraRightX() {
        return this.cameraRightX;
    }

    public float getCameraRightY() {
        return this.cameraRightY;
    }

    public float getCameraRightZ() {
        return this.cameraRightZ;
    }

    public void applyShieldImpactToRoot(Matrix4f matrix, float partialTicks, class_1306 currentArm) {
        if (matrix == null) {
            return;
        }
        boolean syncWalkArms = isWalkArmSyncActive();
        float othersScale = getRemainingProceduralForceScale();
        if (this.shieldImpactValue != 0.0f && this.shieldImpactArm != null && currentArm != null) {
            float multiplier = (syncWalkArms ? CREATIVE_POSE_SPEED_MAX : currentArm == this.shieldImpactArm ? 1.0f : 0.6f) * othersScale;
            float recuo = this.shieldImpactValue * 5.0f * multiplier;
            matrix.translateLocal(this.cameraForwardX * recuo, this.cameraForwardY * recuo, this.cameraForwardZ * recuo);
            matrix.rotateX((-this.shieldImpactValue) * 0.5f * multiplier);
        }
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public float getOffsetZ() {
        return this.offsetZ;
    }

    public float getJumpValue() {
        return this.jumpValue;
    }

    public float getImpactValue() {
        return this.impactValue;
    }

    public float getShieldImpactValue() {
        return this.shieldImpactValue;
    }

    public float getActionAnimValue(class_1306 arm) {
        if (arm == null) {
            return 0.0f;
        }
        return this.actionAnimValue[arm == class_1306.field_6182 ? (char) 0 : (char) 1];
    }

    public float consumeActionAnimKick(class_1306 arm) {
        if (arm == null) {
            return 0.0f;
        }
        int idx = arm == class_1306.field_6182 ? 0 : 1;
        float value = this.actionAnimKick[idx];
        this.actionAnimKick[idx] = 0.0f;
        return value;
    }

    public float getIdleTime() {
        return this.idleTime;
    }

    public float getIdleWeight() {
        return this.idleWeight;
    }

    public void triggerIdleBalance(class_1306 arm) {
        if (arm == null) {
            return;
        }
        if (arm == class_1306.field_6182) {
            this.idleBoostLeftTarget = 1.0f;
            this.idleJitterLeftTimer = 0.0f;
        } else {
            this.idleBoostRightTarget = 1.0f;
            this.idleJitterRightTimer = 0.0f;
        }
    }

    private float getIdleBoost(class_1306 arm) {
        if (arm == null) {
            return 0.0f;
        }
        return arm == class_1306.field_6182 ? this.idleBoostLeft : this.idleBoostRight;
    }

    private float getIdleJitter(class_1306 arm) {
        if (arm == null) {
            return 0.0f;
        }
        return arm == class_1306.field_6182 ? this.idleJitterLeft : this.idleJitterRight;
    }

    private void updateIdleJitter(float dt) {
        this.idleJitterLeftTimer -= dt;
        if (this.idleJitterLeftTimer <= 0.0f) {
            this.idleJitterLeftTarget = (this.idleJitterRandom.method_43057() * 2.0f) - 1.0f;
            this.idleJitterLeftTimer = class_3532.method_16439(this.idleJitterRandom.method_43057(), 0.35f, IDLE_BALANCE_JITTER_MAX);
        }
        this.idleJitterRightTimer -= dt;
        if (this.idleJitterRightTimer <= 0.0f) {
            this.idleJitterRightTarget = (this.idleJitterRandom.method_43057() * 2.0f) - 1.0f;
            this.idleJitterRightTimer = class_3532.method_16439(this.idleJitterRandom.method_43057(), 0.35f, IDLE_BALANCE_JITTER_MAX);
        }
        float jitterLerp = 1.0f - ((float) Math.pow(0.6499999761581421d, dt * 20.0f));
        this.idleJitterLeft = class_3532.method_16439(jitterLerp, this.idleJitterLeft, this.idleJitterLeftTarget);
        this.idleJitterRight = class_3532.method_16439(jitterLerp, this.idleJitterRight, this.idleJitterRightTarget);
    }

    private static boolean isStandingOnSlime(class_746 player) {
        if (player == null || player.method_37908() == null) {
            return false;
        }
        return player.method_37908().method_8320(player.method_24515().method_10074()).method_27852(class_2246.field_10030);
    }

    private boolean shouldApplySprintSwing(class_746 player) {
        if (player == null || !PunchyConfig.enableSprintArmSwing() || PunchyConfig.getSprintArmSwingIntensity() <= 0.0f || this.suppressWalkSway || isSubmergedWithoutGround(player) || !player.method_5624() || player.method_18276() || player.method_5681() || player.method_6128()) {
            return false;
        }
        if ((!player.method_6115() || isSprintCompatibleUseItem(player)) && !PunchyAnimationManager.SWIM_HANDLER.isPlaying()) {
            return player.field_6250 > FALL_ORBIT_ROLL_AMP || player.field_42108.method_48566() > LAND_FALLDIST_THRESHOLD;
        }
        return false;
    }

    private float resolveSprintAlpha(class_1306 currentArm, float partialTicks) {
        float sprintAlpha = smoothStep(this.sprintBlend) * smoothStep(this.sprintSpeedFactor);
        if (sprintAlpha <= CAMERA_LOOK_MIN_DT || currentArm == null) {
            return sprintAlpha;
        }
        if (PunchyAnimationManager.POSE_HANDLER.isCurrentDualHandedActive()) {
            return sprintAlpha;
        }
        AnimationClip currentClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (isSprintBlendAllowedDuringPrimaryPose(currentClip)) {
            return sprintAlpha;
        }
        class_1306 activeArm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (!PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
            if (activeArm != null && activeArm == currentArm && PunchyAnimationManager.POSE_HANDLER.isBlendOutActive()) {
                return sprintAlpha * PunchyAnimationManager.POSE_HANDLER.getBlendOutProgress(partialTicks);
            }
            return sprintAlpha;
        }
        if (activeArm != null && activeArm == currentArm) {
            if (PunchyAnimationManager.POSE_HANDLER.isRebaseReturnToOriginActive()) {
                return sprintAlpha * PunchyAnimationManager.POSE_HANDLER.getRebaseReturnToOriginProgress(partialTicks);
            }
            return 0.0f;
        }
        return sprintAlpha;
    }

    private boolean isSprintBlendAllowedDuringPrimaryPose(AnimationClip clip) {
        return isSprintBlendAllowedDuringEquip(clip) || isSprintBlendAllowedDuringAttack(clip) || isSprintBlendAllowedDuringMining(clip) || isSprintBlendAllowedDuringUseItem(clip);
    }

    private boolean isSprintBlendAllowedDuringEquip(AnimationClip clip) {
        String name;
        if (clip == null || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        return "hand_in".equals(name) || "hand_out".equals(name) || name.startsWith("hand_in_") || name.startsWith("hand_out_");
    }

    private boolean isSprintBlendAllowedDuringAttack(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "attack") || PunchyAnimationManager.isClipNamed(clip, "attack_mace") || PunchyAnimationManager.isClipNamed(clip, "attack_trident") || PunchyAnimationManager.isClipNamed(clip, "attack_spear") || PunchyAnimationManager.isClipNamed(clip, "punch")) {
            return true;
        }
        String name = clip.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("attack") || normalized.startsWith("punch");
    }

    private boolean isSprintBlendAllowedDuringMining(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "mining") || PunchyAnimationManager.isClipNamed(clip, "mining_foward") || PunchyAnimationManager.isClipNamed(clip, "mining_forward") || PunchyAnimationManager.isClipNamed(clip, "mining_sideways") || PunchyAnimationManager.isClipNamed(clip, "mining_digging")) {
            return true;
        }
        String name = clip.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("mining") || normalized.startsWith("mining_") || normalized.contains("mining_v2");
    }

    private boolean isSprintBlendAllowedDuringUseItem(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        if (PunchyAnimationManager.isClipNamed(clip, "interact") || PunchyAnimationManager.isClipNamed(clip, "use_eat") || PunchyAnimationManager.isClipNamed(clip, "use_drink") || PunchyAnimationManager.isClipNamed(clip, "use_item") || PunchyAnimationManager.isClipNamed(clip, "useitem") || PunchyAnimationManager.isClipNamed(clip, "mining_use_foward") || PunchyAnimationManager.isClipNamed(clip, "mining_use_forward") || PunchyAnimationManager.isClipNamed(clip, "mining_use_digging")) {
            return true;
        }
        String name = clip.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("interact") || normalized.equals("interaction") || normalized.equals("use_item") || normalized.equals("useitem") || normalized.equals("use_eat") || normalized.equals("use_drink") || normalized.startsWith("mining_use_")) {
            return true;
        }
        class_310 client = class_310.method_1551();
        return (client == null || client.field_1724 == null || !isSprintCompatibleUseItem(client.field_1724)) ? false : true;
    }

    private boolean shouldApplyWalkSwing(class_746 player, float moveFactor) {
        if (player == null || !PunchyConfig.enableCustomWalk() || this.suppressWalkSway || isSubmergedWithoutGround(player) || player.method_18276() || player.method_5681() || player.method_6128()) {
            return false;
        }
        if (player.method_6115() && !isSprintCompatibleUseItem(player)) {
            return false;
        }
        if (player.method_5799() || player.method_5869() || player.method_5777(class_3486.field_15517)) {
            class_310 client = class_310.method_1551();
            if (!PunchyClient.getWalkOnWaterStateMachine().isTriggerActiveOrInGap(client)) {
                return false;
            }
        }
        return moveFactor > 0.02f;
    }

    private boolean isSubmergedWithoutGround(class_746 player) {
        return (player == null || !player.method_5869() || this.isGrounded) ? false : true;
    }

    private SwingTransform computeWalkSwing(class_1306 currentArm) {
        if (currentArm == null) {
            return SwingTransform.empty();
        }
        boolean syncWalkArms = isWalkArmSyncActive();
        float walkEased = smoothStep(this.walkBlend);
        float effectiveSpeed = this.animGrounded ? this.walkSpeedFactor : 1.0f;
        float weight = walkEased * effectiveSpeed;
        int armIndex = currentArm == class_1306.field_6182 ? 0 : 1;
        int sourceIndex = syncWalkArms ? 0 : armIndex;
        float f = (syncWalkArms || armIndex == 0) ? 1.0f : -1.0f;
        float armSign = f;
        float f2 = this.walkPhase;
        float f3 = (!syncWalkArms && armSign < 0.0f) ? 3.1415927f : 0.0f;
        float phase = f2 + f3;
        float targetPitch = 0.0f;
        float targetYaw = 0.0f;
        float targetRoll = 0.0f;
        float targetSide = 0.0f;
        float targetUp = 0.0f;
        if (weight > CAMERA_LOOK_MIN_DT) {
            float stride = class_3532.method_15374(phase);
            float stride2 = class_3532.method_15374((phase * 2.0f) + 0.35f);
            float side = class_3532.method_15374(phase + 0.4f);
            float contact = Math.abs(stride);
            float targetPitch2 = ((stride * WALK_PITCH_AMP) + (stride2 * WALK_PITCH_AMP_2 * 0.55f)) * weight;
            float targetYaw2 = (((class_3532.method_15374(phase + 1.1f) * 0.03f * SYNC_ROOT_MOTION_SCALE) + (class_3532.method_15374((phase * 2.0f) + FALL_ORBIT_BLEND_OUT_TIME) * 0.012f * 0.5f)) * weight * armSign) + (WALK_INWARD_YAW * weight * armSign);
            float targetRoll2 = (((class_3532.method_15362(phase + 0.35f) * WALK_ROLL_AMP * IDLE_BALANCE_JITTER_MAX) + (class_3532.method_15374((phase * 1.4f) + SMOOTH) * WALK_ROLL_AMP_2 * 0.45f)) * weight * armSign) + (WALK_INWARD_ROLL * weight * armSign);
            targetSide = side * WALK_SIDE_AMP * SYNC_WALK_MOTION_SCALE * weight * armSign;
            float uBias = stride * stride * WALK_SIDE_UP_AMP * 0.35f * weight;
            targetUp = ((((-contact) * 0.06f * 0.55f) + (class_3532.method_15362((phase * 2.0f) + FALL_ORBIT_BLEND_OUT_TIME) * 0.06f * 0.25f)) * weight) + uBias;
            targetPitch = targetPitch2 * WALK_TILT_BOOST;
            targetYaw = targetYaw2 * WALK_TILT_BOOST;
            targetRoll = targetRoll2 * WALK_TILT_BOOST;
            if (syncWalkArms) {
                targetPitch *= SYNC_WALK_ROTATION_SCALE;
                targetYaw *= SYNC_WALK_ROTATION_SCALE;
                targetRoll *= SYNC_WALK_ROTATION_SCALE;
                targetSide *= SYNC_WALK_SIDE_SCALE;
                targetUp *= 1.12f;
            }
        }
        if (this.animGrounded) {
            float dt = this.sprintDt > 0.0f ? this.sprintDt : LAND_FALLDIST_THRESHOLD;
            float smooth = 1.0f - ((float) Math.pow(0.6000000238418579d, dt * 20.0f));
            this.walkRotX[sourceIndex] = class_3532.method_16439(smooth, this.walkRotX[sourceIndex], targetPitch);
            this.walkRotY[sourceIndex] = class_3532.method_16439(smooth, this.walkRotY[sourceIndex], targetYaw);
            this.walkRotZ[sourceIndex] = class_3532.method_16439(smooth, this.walkRotZ[sourceIndex], targetRoll);
            this.walkOffSide[sourceIndex] = class_3532.method_16439(smooth, this.walkOffSide[sourceIndex], targetSide);
            this.walkOffUp[sourceIndex] = class_3532.method_16439(smooth, this.walkOffUp[sourceIndex], targetUp);
            if (syncWalkArms) {
                this.walkRotX[1] = this.walkRotX[0];
                this.walkRotY[1] = this.walkRotY[0];
                this.walkRotZ[1] = this.walkRotZ[0];
                this.walkOffSide[1] = this.walkOffSide[0];
                this.walkOffUp[1] = this.walkOffUp[0];
            }
        }
        return new SwingTransform(this.walkRotX[sourceIndex], this.walkRotY[sourceIndex], this.walkRotZ[sourceIndex], this.walkOffSide[sourceIndex], 0.0f, this.walkOffUp[sourceIndex]);
    }

    private boolean isWalkArmSyncActive() {
        AnimationClip poseClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (poseClip != null && PunchyAnimationManager.POSE_HANDLER.isPlaying() && poseClip.hasEffectAny(SYNC_WALK_ARMS_EFFECT)) {
            return true;
        }
        PunchyTuningConfig.CustomFlags flags = PunchyAnimationManager.POSE_HANDLER.getOverlapCustomFlags();
        if ((flags != null && Boolean.TRUE.equals(flags.syncProceduralArmAnimations)) || isDualHandedSpearChargeActive(poseClip)) {
            return true;
        }
        AnimationClip swimClip = PunchyAnimationManager.SWIM_HANDLER.getCurrentClip();
        return swimClip != null && PunchyAnimationManager.SWIM_HANDLER.isPlaying() && swimClip.hasEffectAny(SYNC_WALK_ARMS_EFFECT);
    }

    public float getWalkAndSprintForceScale() {
        PunchyTuningConfig.CustomFlags flags = PunchyAnimationManager.POSE_HANDLER.getOverlapCustomFlags();
        if (flags != null && flags.walkAndSprintForce != null) {
            return flags.walkAndSprintForce.intValue() / 100.0f;
        }
        return 1.0f;
    }

    public float getRemainingProceduralForceScale() {
        PunchyTuningConfig.CustomFlags flags = PunchyAnimationManager.POSE_HANDLER.getOverlapCustomFlags();
        if (flags != null && flags.remainingProceduralAnimationsForce != null) {
            return flags.remainingProceduralAnimationsForce.intValue() / 100.0f;
        }
        return 1.0f;
    }

    private boolean isSpearChargeUseActive(class_746 player) {
        if (player == null || !player.method_6115()) {
            return false;
        }
        return SpearStateMachine.isSpearStack(player.method_6030());
    }

    private boolean isSprintCompatibleUseItem(class_746 player) {
        if (player == null || !player.method_6115()) {
            return false;
        }
        if (isSpearChargeUseActive(player)) {
            return true;
        }
        class_1799 useStack = player.method_6030();
        return (useStack == null || useStack.method_7960() || (useStack.method_7909() instanceof class_1753) || (useStack.method_7909() instanceof class_1764) || (useStack.method_7909() instanceof class_1835)) ? false : true;
    }

    private boolean isDualHandedSpearChargeActive(AnimationClip clip) {
        String name;
        if (clip == null || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || !PunchyAnimationManager.POSE_HANDLER.isCurrentDualHandedActive() || (name = clip.getName()) == null || name.isBlank()) {
            return false;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("use_spear_pose_");
    }

    private SwingTransform computeSprintSwing(class_1306 currentArm) {
        if (currentArm == null) {
            return SwingTransform.empty();
        }
        boolean allowLateralTilt = isSprintLateralTiltEnabled(currentArm);
        boolean syncWalkArms = isWalkArmSyncActive();
        float eased = smoothStep(this.sprintBlend);
        float intensity = PunchyConfig.getSprintArmSwingIntensity() * SYNC_ROOT_MOTION_SCALE;
        float effectiveSpeed = this.animGrounded ? this.sprintSpeedFactor : 1.0f;
        float weight = eased * effectiveSpeed * intensity * (syncWalkArms ? SYNC_WALK_MOTION_SCALE : 1.0f);
        int armIndex = currentArm == class_1306.field_6182 ? 0 : 1;
        int sourceIndex = syncWalkArms ? 0 : armIndex;
        float f = (syncWalkArms || armIndex == 0) ? 1.0f : -1.0f;
        float armSign = f;
        float f2 = this.sprintPhase;
        float f3 = (!syncWalkArms && armSign < 0.0f) ? 3.1415927f : 0.0f;
        float phase = f2 + f3;
        float targetPitch = 0.0f;
        float targetYaw = 0.0f;
        float targetRoll = 0.0f;
        float targetForward = 0.0f;
        float targetUp = 0.0f;
        if (weight > CAMERA_LOOK_MIN_DT) {
            float stride = class_3532.method_15374(phase);
            float stride2 = class_3532.method_15374((phase * 2.0f) + FALL_ORBIT_BLEND_OUT_TIME);
            float contact = Math.abs(stride);
            float targetPitch2 = ((((stride * SPRINT_PITCH_AMP) * 1.2f) + ((stride2 * 0.06f) * 0.9f)) - (contact * WALK_PITCH_AMP_2)) * weight;
            float targetYaw2 = (((class_3532.method_15374(phase + 1.1f) * SPRINT_YAW_AMP * 0.65f) + (class_3532.method_15374((phase * 2.2f) + 0.22f) * SPRINT_YAW_AMP_2 * 0.45f)) * weight * armSign) + (SPRINT_INWARD_YAW * weight * armSign);
            float targetYaw3 = targetYaw2 + (armSign * SPRINT_INWARD_YAW_STATIC * eased * intensity);
            if (allowLateralTilt) {
                targetRoll = (((class_3532.method_15362(phase + 0.35f) * SPRINT_ROLL_AMP * CREATIVE_POSE_SPEED_MAX) + (class_3532.method_15374((phase * 1.6f) + 0.35f) * SPRINT_ROLL_AMP_2 * 0.55f)) * weight * armSign) + (0.03f * weight * armSign);
            }
            targetForward = ((Math.max(0.0f, stride) * SPRINT_FWD_AMP * 0.9f) + (stride2 * SPRINT_FWD_AMP_2 * 0.45f)) * weight;
            float forwardUpBias = class_3532.method_15363(targetForward, 0.0f, 1.0f) * SPRINT_FWD_PITCH_UP * weight;
            targetUp = (((-contact) * 0.12f * SYNC_WALK_MOTION_SCALE) + (class_3532.method_15362((phase * 2.0f) + SMOOTH) * 0.12f * 0.25f)) * weight;
            targetPitch = (targetPitch2 - forwardUpBias) * SPRINT_TILT_BOOST;
            targetYaw = targetYaw3 * SPRINT_TILT_BOOST;
            targetRoll *= SPRINT_TILT_BOOST;
        }
        if (this.animGrounded) {
            float dt = this.sprintDt > 0.0f ? this.sprintDt : LAND_FALLDIST_THRESHOLD;
            float smooth = 1.0f - ((float) Math.pow(0.5199999809265137d, dt * 20.0f));
            this.sprintRotX[sourceIndex] = class_3532.method_16439(smooth, this.sprintRotX[sourceIndex], targetPitch);
            this.sprintRotY[sourceIndex] = class_3532.method_16439(smooth, this.sprintRotY[sourceIndex], targetYaw);
            this.sprintRotZ[sourceIndex] = class_3532.method_16439(smooth, this.sprintRotZ[sourceIndex], targetRoll);
            this.sprintOffFwd[sourceIndex] = class_3532.method_16439(smooth, this.sprintOffFwd[sourceIndex], targetForward);
            this.sprintOffUp[sourceIndex] = class_3532.method_16439(smooth, this.sprintOffUp[sourceIndex], targetUp);
            if (syncWalkArms) {
                this.sprintRotX[1] = this.sprintRotX[0];
                this.sprintRotY[1] = this.sprintRotY[0];
                this.sprintRotZ[1] = this.sprintRotZ[0];
                this.sprintOffFwd[1] = this.sprintOffFwd[0];
                this.sprintOffUp[1] = this.sprintOffUp[0];
            }
        }
        return new SwingTransform(this.sprintRotX[sourceIndex], this.sprintRotY[sourceIndex], this.sprintRotZ[sourceIndex], 0.0f, this.sprintOffFwd[sourceIndex], this.sprintOffUp[sourceIndex]);
    }

    private static boolean isSprintLateralTiltEnabled(class_1306 currentArm) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || currentArm == null) {
            return true;
        }
        class_746 player = client.field_1724;
        class_1799 baseStack = getStackForArm(player, currentArm);
        boolean left = currentArm == class_1306.field_6182;
        class_1799 renderStack = PunchyAnimationManager.resolveRenderStack(player, currentArm, baseStack);
        if (isSprintLateralTiltDisabledForStack(renderStack, left) || isSprintLateralTiltDisabledForStack(baseStack, left)) {
            return false;
        }
        return true;
    }

    private static boolean isSprintLateralTiltDisabledForStack(class_1799 stack, boolean left) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        LinkedHashSet<VanillaFirstPersonItemLayer.ToolKind> candidates = new LinkedHashSet<>();
        VanillaFirstPersonItemLayer.ToolKind resolved = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        if (resolved != null) {
            candidates.add(resolved);
        }
        if (stack.method_7909() instanceof class_1747) {
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.FULL_BLOCK);
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.BLOCK);
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.BUILDING_BLOCKS);
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.COLORED_BLOCKS);
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.NATURAL_BLOCKS);
            candidates.add(VanillaFirstPersonItemLayer.ToolKind.FUNCTIONAL_BLOCKS);
        }
        candidates.add(VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
        for (VanillaFirstPersonItemLayer.ToolKind kind : candidates) {
            ToolTuning.Profile profile = ToolTuning.resolveProfile(stack, kind, left);
            if (profile != null && Boolean.TRUE.equals(profile.disableSprintLateralTilt)) {
                return true;
            }
        }
        return false;
    }

    private static class_1799 getStackForArm(class_746 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        boolean armIsMain = (arm == class_1306.field_6183) == mainRight;
        return armIsMain ? player.method_6047() : player.method_6079();
    }

    private static float smoothStep(float value) {
        float t = class_3532.method_15363(value, 0.0f, 1.0f);
        return t * t * (AIR_FREEZE_FALLDIST_THRESHOLD - (2.0f * t));
    }

    private static float wrapRadians(float value) {
        return (float) Math.atan2(Math.sin(value), Math.cos(value));
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MovementStateMachine$SneakSpringState.class */
    private static final class SneakSpringState {
        private static final float TARGET_DEPTH = -0.12f;
        private static final float DOWN_STIFFNESS = 22.0f;
        private static final float UP_STIFFNESS = 22.0f;
        private static final float DAMPING = 14.0f;
        private static final float DOWN_IMPULSE = -0.4f;
        private static final float UP_IMPULSE = 1.0f;
        private float value;
        private float velocity;
        private boolean sneaking;

        private SneakSpringState() {
        }

        void setSneaking(boolean sneaking) {
            if (this.sneaking == sneaking) {
                return;
            }
            this.sneaking = sneaking;
            this.velocity += sneaking ? DOWN_IMPULSE : 1.0f;
        }

        void tick(float dt) {
            float target = this.sneaking ? TARGET_DEPTH : 0.0f;
            float stiffness = this.sneaking ? 22.0f : 22.0f;
            float accel = (stiffness * (target - this.value)) - (DAMPING * this.velocity);
            this.velocity += accel * dt;
            this.value += this.velocity * dt;
            if (!this.sneaking && Math.abs(this.value) < MovementStateMachine.CAMERA_LOOK_MIN_DT && Math.abs(this.velocity) < MovementStateMachine.CAMERA_LOOK_MIN_DT) {
                this.value = 0.0f;
                this.velocity = 0.0f;
            }
        }

        float offset() {
            return this.value;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MovementStateMachine$SwingTransform.class */
    private static final class SwingTransform {
        final float rotX;
        final float rotY;
        final float rotZ;
        final float offX;
        final float offY;
        final float offZ;

        private SwingTransform(float rotX, float rotY, float rotZ, float offX, float offY, float offZ) {
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
        }

        static SwingTransform empty() {
            return new SwingTransform(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        boolean hasMotion() {
            return Math.abs(this.rotX) > 1.0E-5f || Math.abs(this.rotY) > 1.0E-5f || Math.abs(this.rotZ) > 1.0E-5f || Math.abs(this.offX) > 1.0E-5f || Math.abs(this.offY) > 1.0E-5f || Math.abs(this.offZ) > 1.0E-5f;
        }

        SwingTransform scaled(float scale) {
            float s = class_3532.method_15363(scale, 0.0f, 2.0f);
            return new SwingTransform(this.rotX * s, this.rotY * s, this.rotZ * s, this.offX * s, this.offY * s, this.offZ * s);
        }

        static SwingTransform blend(SwingTransform a, SwingTransform b, float alpha) {
            if (a == null && b == null) {
                return null;
            }
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            float t = class_3532.method_15363(alpha, 0.0f, 1.0f);
            float inv = 1.0f - t;
            return new SwingTransform((a.rotX * inv) + (b.rotX * t), (a.rotY * inv) + (b.rotY * t), (a.rotZ * inv) + (b.rotZ * t), (a.offX * inv) + (b.offX * t), (a.offY * inv) + (b.offY * t), (a.offZ * inv) + (b.offZ * t));
        }
    }
}
