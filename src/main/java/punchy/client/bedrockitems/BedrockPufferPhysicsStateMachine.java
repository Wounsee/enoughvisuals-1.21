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

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockPufferPhysicsStateMachine.class */
final class BedrockPufferPhysicsStateMachine {
    private static final float BASE_DT = 0.05f;
    private static final float TICK_TIME_SCALE = 2.16f;
    private static final float SPRING_INPUT = 0.32f;
    private static final float SPRING_GRAVITY = 0.055f;
    private static final float SPRING_DAMPING = 0.82f;
    private final EnumMap<class_1306, ArmState> states = new EnumMap<>(class_1306.class);

    BedrockPufferPhysicsStateMachine() {
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
            boolean enabled = state.resolveEnabled(client, stack);
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose.class */
    static final class FeedbackPose extends Record {
        private final float translateXUnits;
        private final float translateYUnits;
        private final float translateZUnits;
        private final float pitchDeg;
        private final float rollDeg;
        private final float scaleX;
        private final float scaleY;
        private final float scaleZ;
        private final float finLift;
        private final float impactWave;

        FeedbackPose(float translateXUnits, float translateYUnits, float translateZUnits, float pitchDeg, float rollDeg, float scaleX, float scaleY, float scaleZ, float finLift, float impactWave) {
            this.translateXUnits = translateXUnits;
            this.translateYUnits = translateYUnits;
            this.translateZUnits = translateZUnits;
            this.pitchDeg = pitchDeg;
            this.rollDeg = rollDeg;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.finLift = finLift;
            this.impactWave = impactWave;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, FeedbackPose.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;finLift;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->finLift:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, FeedbackPose.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;finLift;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->finLift:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, FeedbackPose.class, Object.class), FeedbackPose.class, "translateXUnits;translateYUnits;translateZUnits;pitchDeg;rollDeg;scaleX;scaleY;scaleZ;finLift;impactWave", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateXUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateYUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->translateZUnits:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->pitchDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->rollDeg:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleX:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleY:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->scaleZ:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->finLift:F", "FIELD:Lpunchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$FeedbackPose;->impactWave:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
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

        public float finLift() {
            return this.finLift;
        }

        public float impactWave() {
            return this.impactWave;
        }

        boolean isIdentity() {
            return Math.abs(this.translateXUnits) < 1.0E-4f && Math.abs(this.translateYUnits) < 1.0E-4f && Math.abs(this.translateZUnits) < 1.0E-4f && Math.abs(this.pitchDeg) < 1.0E-4f && Math.abs(this.rollDeg) < 1.0E-4f && Math.abs(this.scaleX - 1.0f) < 1.0E-4f && Math.abs(this.scaleY - 1.0f) < 1.0E-4f && Math.abs(this.scaleZ - 1.0f) < 1.0E-4f && Math.abs(this.finLift) < 1.0E-4f && Math.abs(this.impactWave) < 1.0E-4f;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockPufferPhysicsStateMachine$ArmState.class */
    private static final class ArmState {
        private float weight;
        private float spring;
        private float springSpeed;
        private float wobbleTime;
        private float landingPunch;
        private float finLift;
        private float walkWeight;
        private float walkPhase;
        private float camLat;
        private float camLatSpeed;
        private float camVert;
        private float camVertSpeed;
        private float highFallTiltGate;
        private float highFallPitchDrive;
        private float prevYaw;
        private float prevPitch;
        private boolean cameraPrimed;
        private float prevWeight;
        private float prevSpring;
        private float prevWobbleTime;
        private float prevLandingPunch;
        private float prevLandingPitchVisual;
        private float prevFinLift;
        private float prevWalkWeight;
        private float prevWalkPhase;
        private float prevCamLat;
        private float prevCamVert;
        private float prevHighFallTiltGate;
        private float prevHighFallPitchDrive;
        private float jumpTiltCap;
        private float prevJumpTiltCap;
        private boolean prevHighFallMode;
        private boolean prevOnGround;
        private int airTicks;
        private float landingPitchVisual;
        private class_1799 lastResolvedStack = class_1799.field_8037;
        private boolean lastResolvedEnabled;

        private ArmState() {
        }

        boolean resolveEnabled(class_310 client, class_1799 stack) {
            if (stack == null) {
                stack = class_1799.field_8037;
            }
            if (!this.lastResolvedStack.method_7960() && class_1799.method_31577(this.lastResolvedStack, stack)) {
                return this.lastResolvedEnabled;
            }
            BedrockItemDefinition def = BedrockItemSystem.get().resolveDefinition(stack);
            this.lastResolvedEnabled = def != null;
            this.lastResolvedStack = stack.method_7972();
            return this.lastResolvedEnabled;
        }

        void tick(class_310 client, boolean enabled) {
            float fMethod_15363;
            this.prevWeight = this.weight;
            this.prevSpring = this.spring;
            this.prevWobbleTime = this.wobbleTime;
            this.prevLandingPunch = this.landingPunch;
            this.prevLandingPitchVisual = this.landingPitchVisual;
            this.prevFinLift = this.finLift;
            this.prevWalkWeight = this.walkWeight;
            this.prevWalkPhase = this.walkPhase;
            this.prevCamLat = this.camLat;
            this.prevCamVert = this.camVert;
            this.prevHighFallTiltGate = this.highFallTiltGate;
            this.prevHighFallPitchDrive = this.highFallPitchDrive;
            this.prevJumpTiltCap = this.jumpTiltCap;
            if (!enabled || client == null || client.field_1724 == null) {
                tickDisabled();
                return;
            }
            class_746 player = client.field_1724;
            float vy = (float) player.method_18798().field_1351;
            float vx = (float) player.method_18798().field_1352;
            float vz = (float) player.method_18798().field_1350;
            float yaw = player.method_36454();
            float pitchLook = player.method_36455();
            boolean onGround = player.method_24828();
            float fallDistance = (float) player.field_6017;
            boolean creativeFlight = player.method_31549().field_7479;
            boolean descending = (-vy) > 0.08f;
            boolean airbornePhysics = (onGround || creativeFlight) ? false : true;
            boolean meaningfulFall = airbornePhysics && (descending || fallDistance > 1.25f);
            this.weight = class_3532.method_16439(rate(0.48f), this.weight, 1.0f);
            this.springSpeed += (-vy) * BedrockPufferPhysicsStateMachine.SPRING_INPUT * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.springSpeed -= (BedrockPufferPhysicsStateMachine.SPRING_GRAVITY * this.spring) * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.springSpeed *= (float) Math.pow(0.8199999928474426d, 2.1600000858306885d);
            this.spring += this.springSpeed * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.spring = class_3532.method_15363(this.spring, -2.2f, 2.8f);
            boolean justLanded = false;
            if (!this.prevOnGround && onGround) {
                this.landingPunch = 1.0f;
                this.spring *= 0.5f;
                this.springSpeed *= 0.45f;
                this.springSpeed -= 0.55f;
                this.spring -= 0.28f;
                justLanded = true;
                this.airTicks = 0;
            } else if (onGround || creativeFlight) {
                this.airTicks = 0;
            } else {
                this.airTicks = Math.min(this.airTicks + 1, 200);
            }
            if (!justLanded) {
                this.landingPunch = class_3532.method_16439(rate(0.22f), this.landingPunch, 0.0f);
            }
            float landingRise = rate(0.07f);
            float landingFall = rate(0.11f);
            this.landingPitchVisual = class_3532.method_16439(this.landingPunch > this.landingPitchVisual ? landingRise : landingFall, this.landingPitchVisual, this.landingPunch);
            float highFallTarget = meaningfulFall ? 1.0f : 0.0f;
            this.highFallTiltGate = moveTowards(this.highFallTiltGate, highFallTarget, 0.1f);
            if (meaningfulFall) {
                float velNorm = class_3532.method_15363(((-vy) - BedrockPufferPhysicsStateMachine.BASE_DT) / 0.45f, 0.0f, 1.0f);
                float distNorm = class_3532.method_15363((fallDistance - 5.0f) / 7.0f, 0.0f, 1.0f);
                float targetDrive = Math.max(velNorm, distNorm);
                if (!this.prevHighFallMode) {
                    float springSeed = class_3532.method_15363(Math.max(0.0f, this.spring) / 2.4f, 0.0f, 1.0f);
                    this.highFallPitchDrive = Math.max(this.highFallPitchDrive, springSeed);
                }
                float step = targetDrive >= this.highFallPitchDrive ? 0.03125f : 0.008333334f;
                this.highFallPitchDrive = moveTowards(this.highFallPitchDrive, targetDrive, step);
            } else {
                this.highFallPitchDrive = moveTowards(this.highFallPitchDrive, 0.0f, 0.06666667f);
            }
            if (airbornePhysics) {
                float jumpRef = Math.abs(this.spring * 11.5f);
                this.jumpTiltCap = Math.max(this.jumpTiltCap, jumpRef);
            } else if (!justLanded) {
                this.jumpTiltCap = class_3532.method_16439(rate(0.3f), this.jumpTiltCap, 0.0f);
            }
            float finTarget = (!meaningfulFall || this.airTicks < 2) ? 0.0f : 1.0f;
            float finStep = finTarget > this.finLift ? 0.055555556f : 0.0625f;
            this.finLift = moveTowards(this.finLift, finTarget, finStep);
            float horizontalSpeed = class_3532.method_15355((vx * vx) + (vz * vz));
            if (onGround && horizontalSpeed > 0.02f) {
                fMethod_15363 = class_3532.method_15363((horizontalSpeed - 0.02f) / 0.1f, 0.0f, 1.0f);
            } else {
                fMethod_15363 = 0.0f;
            }
            float walkTarget = fMethod_15363;
            this.walkWeight = class_3532.method_16439(rate(onGround ? 0.24f : 0.36f), this.walkWeight, walkTarget);
            this.walkPhase += 0.108f * (1.2f + (this.walkWeight * 3.0f));
            if (!this.cameraPrimed) {
                this.prevYaw = yaw;
                this.prevPitch = pitchLook;
                this.cameraPrimed = true;
            }
            float yawDelta = class_3532.method_15393(yaw - this.prevYaw);
            float pitchDelta = pitchLook - this.prevPitch;
            this.prevYaw = yaw;
            this.prevPitch = pitchLook;
            float yawInput = Math.abs(yawDelta) > 0.9f ? yawDelta : 0.0f;
            float pitchInput = Math.abs(pitchDelta) > 0.9f ? pitchDelta : 0.0f;
            float camLatTarget = class_3532.method_15363((-yawInput) * 0.019f, -1.1f, 1.1f);
            float camVertTarget = class_3532.method_15363((-pitchInput) * 0.019f, -1.0f, 1.0f);
            this.camLatSpeed += camLatTarget * 0.14f * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVertSpeed += camVertTarget * 0.14f * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLatSpeed -= (0.07f * this.camLat) * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVertSpeed -= (0.07f * this.camVert) * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLatSpeed *= (float) Math.pow(0.8399999737739563d, 2.1600000858306885d);
            this.camVertSpeed *= (float) Math.pow(0.8399999737739563d, 2.1600000858306885d);
            this.camLat += this.camLatSpeed * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camVert += this.camVertSpeed * BedrockPufferPhysicsStateMachine.TICK_TIME_SCALE;
            this.camLat = class_3532.method_15363(this.camLat, -1.1f, 1.1f);
            this.camVert = class_3532.method_15363(this.camVert, -1.0f, 1.0f);
            this.wobbleTime += 0.108f * (1.1f + (Math.abs(this.spring) * 0.7f));
            this.prevOnGround = onGround;
            this.prevHighFallMode = meaningfulFall;
        }

        void tickDisabled() {
            this.prevWeight = this.weight;
            this.prevSpring = this.spring;
            this.prevWobbleTime = this.wobbleTime;
            this.prevLandingPunch = this.landingPunch;
            this.prevLandingPitchVisual = this.landingPitchVisual;
            this.prevFinLift = this.finLift;
            this.prevWalkWeight = this.walkWeight;
            this.prevWalkPhase = this.walkPhase;
            this.prevCamLat = this.camLat;
            this.prevCamVert = this.camVert;
            this.prevHighFallTiltGate = this.highFallTiltGate;
            this.prevHighFallPitchDrive = this.highFallPitchDrive;
            this.prevJumpTiltCap = this.jumpTiltCap;
            this.weight = class_3532.method_16439(rate(0.28f), this.weight, 0.0f);
            this.springSpeed = class_3532.method_16439(rate(0.28f), this.springSpeed, 0.0f);
            this.spring = class_3532.method_16439(rate(0.28f), this.spring, 0.0f);
            this.landingPunch = class_3532.method_16439(rate(0.24f), this.landingPunch, 0.0f);
            this.landingPitchVisual = class_3532.method_16439(rate(0.12f), this.landingPitchVisual, this.landingPunch);
            this.finLift = class_3532.method_16439(rate(0.4f), this.finLift, 0.0f);
            this.walkWeight = class_3532.method_16439(rate(0.36f), this.walkWeight, 0.0f);
            this.walkPhase += 0.108f;
            this.camLatSpeed = class_3532.method_16439(rate(0.35f), this.camLatSpeed, 0.0f);
            this.camVertSpeed = class_3532.method_16439(rate(0.35f), this.camVertSpeed, 0.0f);
            this.camLat = class_3532.method_16439(rate(0.35f), this.camLat, 0.0f);
            this.camVert = class_3532.method_16439(rate(0.35f), this.camVert, 0.0f);
            this.highFallTiltGate = moveTowards(this.highFallTiltGate, 0.0f, 0.06666667f);
            this.highFallPitchDrive = moveTowards(this.highFallPitchDrive, 0.0f, 0.06666667f);
            this.jumpTiltCap = class_3532.method_16439(rate(BedrockPufferPhysicsStateMachine.SPRING_INPUT), this.jumpTiltCap, 0.0f);
            this.wobbleTime += 0.108f;
            this.airTicks = 0;
            this.prevOnGround = false;
            this.cameraPrimed = false;
            this.prevHighFallMode = false;
        }

        FeedbackPose sample(class_1306 arm, float partialTicks) {
            float t = class_3532.method_15363(partialTicks, 0.0f, 1.0f);
            float w = class_3532.method_15363(class_3532.method_16439(t, this.prevWeight, this.weight), 0.0f, 1.0f);
            float s = class_3532.method_16439(t, this.prevSpring, this.spring);
            float wt = class_3532.method_16439(t, this.prevWobbleTime, this.wobbleTime);
            float punch = class_3532.method_15363(class_3532.method_16439(t, this.prevLandingPunch, this.landingPunch), 0.0f, 1.0f);
            float landingPitch = smoothStep01(class_3532.method_15363(class_3532.method_16439(t, this.prevLandingPitchVisual, this.landingPitchVisual), 0.0f, 1.0f));
            float fin = class_3532.method_15363(class_3532.method_16439(t, this.prevFinLift, this.finLift), 0.0f, 1.0f);
            float fin2 = smootherStep01(fin);
            float walkW = class_3532.method_15363(class_3532.method_16439(t, this.prevWalkWeight, this.walkWeight), 0.0f, 1.0f);
            float walkT = class_3532.method_16439(t, this.prevWalkPhase, this.walkPhase);
            float camLatNow = class_3532.method_16439(t, this.prevCamLat, this.camLat);
            float camVertNow = class_3532.method_16439(t, this.prevCamVert, this.camVert);
            float fSin = ((float) Math.sin(wt * 8.2f)) * 0.012f * w;
            float walkBob = ((float) Math.sin(walkT)) * 0.5f * walkW;
            float walkRoll = ((float) Math.sin(walkT + 0.75f)) * 3.0f * walkW;
            float camLatNorm = class_3532.method_15363(camLatNow / 1.1f, -1.0f, 1.0f);
            float camLatEased = easeInOutSigned(camLatNorm) * 1.1f;
            float camVertNorm = class_3532.method_15363(camVertNow, -1.0f, 1.0f);
            float camVertEased = easeInOutSigned(camVertNorm);
            float armSign = arm == class_1306.field_6182 ? -1.0f : 1.0f;
            float translateX = camLatEased * 0.24f * w * armSign;
            float translateY = ((((s * 0.62f) - (punch * 0.92f)) * w) + walkBob) - ((camVertEased * 0.2f) * w);
            float translateZ = s * 0.26f * w;
            float highFallTiltNow = class_3532.method_15363(class_3532.method_16439(t, this.prevHighFallTiltGate, this.highFallTiltGate), 0.0f, 1.0f);
            float highFallPitchDriveNow = class_3532.method_15363(class_3532.method_16439(t, this.prevHighFallPitchDrive, this.highFallPitchDrive), 0.0f, 1.0f);
            float jumpCapNow = Math.max(0.0f, class_3532.method_16439(t, this.prevJumpTiltCap, this.jumpTiltCap));
            float highFallMul = class_3532.method_16439(highFallTiltNow, 1.0f, 1.22f);
            float fallPitchSource = Math.max(0.0f, s);
            float fallPitchTerm = ((fallPitchSource * 11.5f) + (highFallPitchDriveNow * 5.0f)) * highFallMul * w;
            float rawPitch = ((fallPitchTerm * (1.0f - landingPitch)) - ((landingPitch * 30.0f) * w)) - ((camVertEased * 8.0f) * w);
            float pitchCap = Math.max(2.0f, Math.max(8.0f + (landingPitch * 15.0f), jumpCapNow));
            float pitch = class_3532.method_15363(rawPitch, -pitchCap, pitchCap);
            float roll = (((float) Math.sin(wt * 1.6f)) * 1.8f * w) + walkRoll + (camLatEased * 5.0f * w);
            return new FeedbackPose(translateX, translateY, translateZ, pitch, roll, 1.0f, 1.0f, 1.0f, fin2, Math.abs(s) * w);
        }

        private static float rate(float base) {
            float b = class_3532.method_15363(base, 0.0f, 0.999f);
            return 1.0f - ((float) Math.pow(1.0f - b, 2.1600000858306885d));
        }

        private static float easeInOutSigned(float x) {
            float a = class_3532.method_15363(Math.abs(x), 0.0f, 1.0f);
            float eased = 0.5f - (0.5f * ((float) Math.cos(3.141592653589793d * ((double) a))));
            return Math.signum(x) * eased;
        }

        private static float moveTowards(float current, float target, float step) {
            float s = Math.abs(step);
            if (current < target) {
                return Math.min(current + s, target);
            }
            return Math.max(current - s, target);
        }

        private static float smoothStep01(float t) {
            float x = class_3532.method_15363(t, 0.0f, 1.0f);
            return x * x * (3.0f - (2.0f * x));
        }

        private static float smootherStep01(float t) {
            float x = class_3532.method_15363(t, 0.0f, 1.0f);
            return x * x * x * ((x * ((x * 6.0f) - 15.0f)) + 10.0f);
        }
    }
}
