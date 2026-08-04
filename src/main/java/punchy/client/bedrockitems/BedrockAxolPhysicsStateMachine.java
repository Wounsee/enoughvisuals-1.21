package punchy.client.bedrockitems;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.EnumMap;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_746;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockAxolPhysicsStateMachine.class */
final class BedrockAxolPhysicsStateMachine {
    private static final float BASE_DT = 0.05f;
    private static final float TICK_TIME_SCALE = 2.16f;
    private static final float FALL_INTENSITY = 0.24f;
    private static final float FALL_GRAVITY = 0.04f;
    private static final float FALL_DAMPING = 0.85f;
    private final EnumMap<class_1306, ArmState> states = new EnumMap<>(class_1306.class);

    BedrockAxolPhysicsStateMachine() {
    }

    void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            for (class_1306 class_1306Var : class_1306.values()) {
                ((ArmState) this.states.computeIfAbsent(class_1306Var, key -> {
                    return new ArmState();
                })).tickDisabled();
            }
            return;
        }
        for (class_1306 arm : class_1306.values()) {
            ArmState state = (ArmState) this.states.computeIfAbsent(arm, key2 -> {
                return new ArmState();
            });
            class_1799 stack = resolveStack(client, arm);
            boolean enabled = state.resolveEnabled(stack);
            state.tick(client, enabled);
        }
    }

    FeedbackPose getPose(class_1306 arm, float partialTicks) {
        ArmState state;
        if (arm == null || (state = this.states.get(arm)) == null) {
            return null;
        }
        return state.sample(arm, partialTicks);
    }

    private static class_1799 resolveStack(class_310 client, class_1306 arm) {
        if (client == null || client.field_1724 == null || arm == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? client.field_1724.method_6047() : client.field_1724.method_6079() : mainRight ? client.field_1724.method_6079() : client.field_1724.method_6047();
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose.class */
    static final class FeedbackPose extends Record {
        private final float translateXUnits;
        private final float translateYUnits;
        private final float translateZUnits;
        private final float highFallLiftUnits;
        private final float armRaiseGate;
        private final float landingPunch;
        private final float walkPitchDeg;
        private final float pitchDeg;
        private final float rollDeg;
        private final float scaleX;
        private final float scaleY;
        private final float scaleZ;
        private final float impactWave;

        FeedbackPose(float translateXUnits, float translateYUnits, float translateZUnits, float highFallLiftUnits, float armRaiseGate, float landingPunch, float walkPitchDeg, float pitchDeg, float rollDeg, float scaleX, float scaleY, float scaleZ, float impactWave) {
            this.translateXUnits = translateXUnits;
            this.translateYUnits = translateYUnits;
            this.translateZUnits = translateZUnits;
            this.highFallLiftUnits = highFallLiftUnits;
            this.armRaiseGate = armRaiseGate;
            this.landingPunch = landingPunch;
            this.walkPitchDeg = walkPitchDeg;
            this.pitchDeg = pitchDeg;
            this.rollDeg = rollDeg;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.impactWave = impactWave;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FeedbackPose.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;highFallLiftUnits;armRaiseGate;landingPunch;walkPitchDeg;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->highFallLiftUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->armRaiseGate:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->landingPunch:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->walkPitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FeedbackPose.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;highFallLiftUnits;armRaiseGate;landingPunch;walkPitchDeg;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->highFallLiftUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->armRaiseGate:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->landingPunch:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->walkPitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FeedbackPose.class, Object.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;highFallLiftUnits;armRaiseGate;landingPunch;walkPitchDeg;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->highFallLiftUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->armRaiseGate:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->landingPunch:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->walkPitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public float translateXUnits() {
            return this.translateXUnits;
        }

        public float translateYUnits() {
            return this.translateYUnits;
        }

        public float translateZUnits() {
            return this.translateZUnits;
        }

        public float highFallLiftUnits() {
            return this.highFallLiftUnits;
        }

        public float armRaiseGate() {
            return this.armRaiseGate;
        }

        public float landingPunch() {
            return this.landingPunch;
        }

        public float walkPitchDeg() {
            return this.walkPitchDeg;
        }

        public float pitchDeg() {
            return this.pitchDeg;
        }

        public float rollDeg() {
            return this.rollDeg;
        }

        public float scaleX() {
            return this.scaleX;
        }

        public float scaleY() {
            return this.scaleY;
        }

        public float scaleZ() {
            return this.scaleZ;
        }

        public float impactWave() {
            return this.impactWave;
        }

        boolean isIdentity() {
            return Math.abs(this.translateXUnits) < 1.0E-4f && Math.abs(this.translateYUnits) < 1.0E-4f && Math.abs(this.translateZUnits) < 1.0E-4f && Math.abs(this.highFallLiftUnits) < 1.0E-4f && Math.abs(this.armRaiseGate) < 1.0E-4f && Math.abs(this.landingPunch) < 1.0E-4f && Math.abs(this.walkPitchDeg) < 1.0E-4f && Math.abs(this.pitchDeg) < 1.0E-4f && Math.abs(this.rollDeg) < 1.0E-4f && Math.abs(this.scaleX - 1.0f) < 1.0E-4f && Math.abs(this.scaleY - 1.0f) < 1.0E-4f && Math.abs(this.scaleZ - 1.0f) < 1.0E-4f && Math.abs(this.impactWave) < 1.0E-4f;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockAxolPhysicsStateMachine$ArmState.class */
    private static final class ArmState {
        private float weight;
        private float fall;
        private float fallSpeed;
        private float fallFastGate;
        private float wobbleTime;
        private float prevWeight;
        private float prevFall;
        private float prevFallSpeed;
        private float prevFallFastGate;
        private float prevArmRaiseGate;
        private float prevLandingPunch;
        private float prevWobbleTime;
        private float prevWalkWeight;
        private float prevWalkPhase;
        private float prevCamLat;
        private float prevCamVert;
        private float prevPitchVisual;
        private float lastVy;
        private float armRaiseGate;
        private float landingPunch;
        private float walkWeight;
        private float walkPhase;
        private float camLat;
        private float camLatSpeed;
        private float camVert;
        private float camVertSpeed;
        private float pitchVisual;
        private float prevYaw;
        private float prevPitch;
        private boolean cameraPrimed;
        private int airTicks;
        private boolean prevOnGround;
        private class_1799 lastResolvedStack = class_1799.field_8037;
        private boolean lastResolvedEnabled;

        private ArmState() {
        }

        boolean resolveEnabled(class_1799 stack) {
            if (stack == null) {
                stack = class_1799.field_8037;
            }
            if (!this.lastResolvedStack.method_7960() && class_1799.method_31577(this.lastResolvedStack, stack)) {
                return this.lastResolvedEnabled;
            }
            BedrockItemDefinition def = BedrockItemSystem.get().resolveDefinition(stack);
            this.lastResolvedEnabled = def != null && def.isPhysicsFeedbackEnabled();
            this.lastResolvedStack = stack.method_7972();
            return this.lastResolvedEnabled;
        }

        void tick(class_310 client, boolean enabled) {
            float fMethod_15363;
            this.prevWeight = this.weight;
            this.prevFall = this.fall;
            this.prevFallSpeed = this.fallSpeed;
            this.prevFallFastGate = this.fallFastGate;
            this.prevArmRaiseGate = this.armRaiseGate;
            this.prevLandingPunch = this.landingPunch;
            this.prevWobbleTime = this.wobbleTime;
            this.prevWalkWeight = this.walkWeight;
            this.prevWalkPhase = this.walkPhase;
            this.prevCamLat = this.camLat;
            this.prevCamVert = this.camVert;
            this.prevPitchVisual = this.pitchVisual;
            class_746 player = client.field_1724;
            float vy = (float) player.method_18798().field_1351;
            float vx = (float) player.method_18798().field_1352;
            float vz = (float) player.method_18798().field_1350;
            float yaw = player.method_36454();
            float pitchLook = player.method_36455();
            boolean onGround = player.method_24828();
            float fallDistance = (float) player.field_6017;
            if (!enabled) {
                tickDisabled();
                return;
            }
            this.weight = class_3532.method_16439(rate(0.42f), this.weight, 1.0f);
            this.fallSpeed += (-vy) * BedrockAxolPhysicsStateMachine.FALL_INTENSITY * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.fallSpeed -= (BedrockAxolPhysicsStateMachine.FALL_GRAVITY * this.fall) * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.fallSpeed *= (float) Math.pow(0.8500000238418579d, 2.1600000858306885d);
            this.fall += this.fallSpeed * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.fall = class_3532.method_15363(this.fall, -1.8f, 2.4f);
            if (!onGround && (-vy) > 0.08f) {
                this.fallFastGate += 0.2592f;
            } else {
                this.fallFastGate -= 0.216f;
            }
            this.fallFastGate = class_3532.method_15363(this.fallFastGate, 0.0f, 1.0f);
            float armTarget = (onGround || fallDistance < 5.0f) ? 0.0f : 1.0f;
            this.armRaiseGate = moveTowards(this.armRaiseGate, armTarget, 0.06666667f);
            if (!onGround) {
                this.airTicks = Math.min(this.airTicks + 1, 200);
            }
            float horizontalSpeed = class_3532.method_15355((vx * vx) + (vz * vz));
            if (onGround && horizontalSpeed > 0.02f) {
                fMethod_15363 = class_3532.method_15363((horizontalSpeed - 0.02f) / 0.1f, 0.0f, 1.0f);
            } else {
                fMethod_15363 = 0.0f;
            }
            float walkTarget = fMethod_15363;
            this.walkWeight = class_3532.method_16439(rate(onGround ? 0.25f : 0.4f), this.walkWeight, walkTarget);
            this.walkPhase += 0.108f * (1.2f + (this.walkWeight * 3.2f));
            if (!this.cameraPrimed) {
                this.prevYaw = yaw;
                this.prevPitch = pitchLook;
                this.cameraPrimed = true;
            }
            float yawDelta = class_3532.method_15393(yaw - this.prevYaw);
            float pitchDelta = pitchLook - this.prevPitch;
            this.prevYaw = yaw;
            this.prevPitch = pitchLook;
            float yawAbs = Math.abs(yawDelta);
            float pitchAbs = Math.abs(pitchDelta);
            float yawInput = yawAbs > 0.9f ? yawDelta : 0.0f;
            float pitchInput = pitchAbs > 0.9f ? pitchDelta : 0.0f;
            float camLatTarget = class_3532.method_15363((-yawInput) * 0.0224f, -1.25f, 1.25f);
            float camVertTarget = class_3532.method_15363((-pitchInput) * 0.0224f, -1.0f, 1.0f);
            this.camLatSpeed += camLatTarget * 0.108f * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVertSpeed += camVertTarget * 0.108f * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLatSpeed -= (0.06f * this.camLat) * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVertSpeed -= (0.06f * this.camVert) * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLatSpeed *= (float) Math.pow(0.8500000238418579d, 2.1600000858306885d);
            this.camVertSpeed *= (float) Math.pow(0.8500000238418579d, 2.1600000858306885d);
            this.camLat += this.camLatSpeed * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVert += this.camVertSpeed * BedrockAxolPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLat = class_3532.method_15363(this.camLat, -1.25f, 1.25f);
            this.camVert = class_3532.method_15363(this.camVert, -1.0f, 1.0f);
            boolean justLanded = false;
            if (!this.prevOnGround && onGround) {
                float largeT = class_3532.method_15363((this.airTicks - 3.0f) / 12.0f, 0.0f, 1.0f);
                float impactScale = 0.5f + (0.5f * largeT);
                float impact = 0.95f * impactScale;
                this.fallSpeed -= impact * 1.65f;
                this.fall -= impact * 0.45f;
                this.fallSpeed *= 0.9f;
                this.landingPunch = 1.0f;
                justLanded = true;
                this.airTicks = 0;
            } else if (onGround) {
                this.airTicks = 0;
            }
            if (!justLanded) {
                this.landingPunch = class_3532.method_16439(rate(0.35f), this.landingPunch, 0.0f);
            }
            float wf = class_3532.method_15363(this.fall * 0.4f, 0.0f, 2.0f) * this.fallFastGate * this.fallFastGate;
            this.wobbleTime += 0.108f * (1.0f + (wf * 1.8f));
            float camVertNormTick = class_3532.method_15363(this.camVert, -1.0f, 1.0f);
            float camVertEasedTick = easeInOutSigned(camVertNormTick);
            float fallSignedTick = class_3532.method_15363(this.fall - (camVertEasedTick * 0.9f), -2.4f, 2.4f);
            float targetPitch = class_3532.method_15363(((16.8f * fallSignedTick) * (1.0f - (this.landingPunch * 1.25f))) - (4.14f * this.landingPunch), -44.0f, 44.0f) * this.weight;
            float pitchFollow = (onGround || fallDistance < 5.0f) ? rate(0.16f) : rate(0.07f);
            this.pitchVisual = class_3532.method_16439(pitchFollow, this.pitchVisual, targetPitch);
            this.prevOnGround = onGround;
            this.lastVy = vy;
        }

        void tickDisabled() {
            this.prevWeight = this.weight;
            this.prevFall = this.fall;
            this.prevFallSpeed = this.fallSpeed;
            this.prevFallFastGate = this.fallFastGate;
            this.prevArmRaiseGate = this.armRaiseGate;
            this.prevLandingPunch = this.landingPunch;
            this.prevWobbleTime = this.wobbleTime;
            this.prevWalkWeight = this.walkWeight;
            this.prevWalkPhase = this.walkPhase;
            this.prevCamLat = this.camLat;
            this.prevCamVert = this.camVert;
            this.prevPitchVisual = this.pitchVisual;
            this.weight = class_3532.method_16439(rate(0.25f), this.weight, 0.0f);
            this.fallSpeed = class_3532.method_16439(rate(0.25f), this.fallSpeed, 0.0f);
            this.fall = class_3532.method_16439(rate(0.25f), this.fall, 0.0f);
            this.fallFastGate = class_3532.method_16439(rate(0.2f), this.fallFastGate, 0.0f);
            this.armRaiseGate = moveTowards(this.armRaiseGate, 0.0f, 0.06666667f);
            this.landingPunch = class_3532.method_16439(rate(0.45f), this.landingPunch, 0.0f);
            this.walkWeight = class_3532.method_16439(rate(0.4f), this.walkWeight, 0.0f);
            this.walkPhase += 0.108f;
            this.wobbleTime += 0.108f;
            this.camLatSpeed = class_3532.method_16439(rate(0.35f), this.camLatSpeed, 0.0f);
            this.camLat = class_3532.method_16439(rate(0.35f), this.camLat, 0.0f);
            this.camVertSpeed = class_3532.method_16439(rate(0.35f), this.camVertSpeed, 0.0f);
            this.camVert = class_3532.method_16439(rate(0.35f), this.camVert, 0.0f);
            this.pitchVisual = class_3532.method_16439(rate(0.2f), this.pitchVisual, 0.0f);
            this.airTicks = 0;
            this.cameraPrimed = false;
        }

        FeedbackPose sample(class_1306 arm, float partialTicks) {
            float t = class_3532.method_15363(partialTicks, 0.0f, 1.0f);
            float w = class_3532.method_15363(class_3532.method_16439(t, this.prevWeight, this.weight), 0.0f, 1.0f);
            float f = class_3532.method_16439(t, this.prevFall, this.fall);
            float ff = class_3532.method_16439(t, this.prevFallFastGate, this.fallFastGate);
            float armGateRaw = class_3532.method_15363(class_3532.method_16439(t, this.prevArmRaiseGate, this.armRaiseGate), 0.0f, 1.0f);
            float armGate = smoothStep01(armGateRaw);
            float punch = class_3532.method_15363(class_3532.method_16439(t, this.prevLandingPunch, this.landingPunch), 0.0f, 1.0f);
            float wt = class_3532.method_16439(t, this.prevWobbleTime, this.wobbleTime);
            float walkW = class_3532.method_15363(class_3532.method_16439(t, this.prevWalkWeight, this.walkWeight), 0.0f, 1.0f);
            float walkT = class_3532.method_16439(t, this.prevWalkPhase, this.walkPhase);
            float camLatNow = class_3532.method_16439(t, this.prevCamLat, this.camLat);
            float camVertNow = class_3532.method_16439(t, this.prevCamVert, this.camVert);
            float pitchNow = class_3532.method_16439(t, this.prevPitchVisual, this.pitchVisual);
            float wf = class_3532.method_15363(f * 0.4f, 0.0f, 2.0f) * ff * ff * ff;
            float walkBob = ((float) Math.sin(walkT)) * 0.70686f * walkW;
            float walkPitch = ((float) Math.sin(walkT + 0.65f)) * 7.084f * walkW;
            float camLatNorm = class_3532.method_15363(camLatNow / 1.25f, -1.0f, 1.0f);
            float camLatEased = easeInOutSigned(camLatNorm) * 1.25f;
            float camVertNorm = class_3532.method_15363(camVertNow, -1.0f, 1.0f);
            float camVertEased = easeInOutSigned(camVertNorm);
            float fallSigned = class_3532.method_15363(f - (camVertEased * 0.9f), -2.4f, 2.4f);
            float armLatSign = arm == class_1306.field_6182 ? -1.0f : 1.0f;
            float translateX = camLatEased * 0.42f * w * armLatSign;
            float translateY = ((((fallSigned * 0.38f) - (punch * BedrockAxolPhysicsStateMachine.FALL_DAMPING)) * w) + walkBob) - ((camVertEased * 0.34f) * w);
            float translateZ = fallSigned * 0.44f * w;
            float armSign = arm == class_1306.field_6182 ? 1.0f : -1.0f;
            float roll = armSign * wf * 3.0f * w;
            float wobble = ((float) Math.sin(wt * 7.2f)) * wf * 0.004f * w;
            float stretchY = 1.0f + (0.02f * fallSigned * w) + wobble;
            float squashXZ = (1.0f - ((0.02f * fallSigned) * w)) - (wobble * 0.25f);
            float stretchY2 = class_3532.method_15363(stretchY, 0.97f, 1.05f);
            float squashXZ2 = class_3532.method_15363(squashXZ, 0.95f, 1.03f);
            return new FeedbackPose(translateX, translateY, translateZ, 0.0f, armGate, punch * w, walkPitch, pitchNow, roll, squashXZ2, stretchY2, squashXZ2, wf * w);
        }

        private static float rate(float base) {
            float b = class_3532.method_15363(base, 0.0f, 0.999f);
            return 1.0f - ((float) Math.pow(1.0f - b, 2.1600000858306885d));
        }

        private static float easeOutBack(float t) {
            float x = class_3532.method_15363(t, 0.0f, 1.0f) - 1.0f;
            float c3 = 2.6f + 1.0f;
            return 1.0f + (c3 * x * x * x) + (2.6f * x * x);
        }

        private static float smoothStep01(float t) {
            float x = class_3532.method_15363(t, 0.0f, 1.0f);
            return x * x * (3.0f - (2.0f * x));
        }

        private static float moveTowards(float current, float target, float step) {
            float s = Math.abs(step);
            if (current < target) {
                return Math.min(current + s, target);
            }
            return Math.max(current - s, target);
        }

        private static float easeInOutSigned(float x) {
            float a = class_3532.method_15363(Math.abs(x), 0.0f, 1.0f);
            float eased = 0.5f - (0.5f * ((float) Math.cos(3.141592653589793d * ((double) a))));
            return Math.signum(x) * eased;
        }
    }
}
