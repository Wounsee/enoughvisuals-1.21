package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_1268;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_746;
import org.joml.Quaternionf;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/SlimeInHandPhysics.class */
public final class SlimeInHandPhysics {
    private static final SlimeInHandPhysics INSTANCE = new SlimeInHandPhysics();
    private static final float STIFFNESS = 192.375f;
    private static final float DAMPING = 32.90625f;
    private static final float JUMP_IMPULSE = -1.8f;
    private static final float LAND_IMPULSE = -9.6f;
    private static final float FALL_STRETCH_MAX = 0.6f;
    private static final float FALL_SPEED_MAX = 0.6f;
    private static final float FALL_DISTANCE_MAX = 6.0f;
    private static final float RISE_SQUASH_MAX = 0.45f;
    private static final float RISE_SPEED_MAX = 0.6f;
    private static final float MAX_DEFORM = 0.9f;
    private static final float XZ_COMP = 0.5f;
    private float frameDt;
    private boolean frameOnGround;
    private boolean frameNoGravity;
    private float frameDeltaY;
    private float frameFallDistance;
    private boolean frameJumpPressed;
    private boolean frameValid;
    private long frameId;
    private long lastUpdateTime = System.nanoTime();
    private final EnumMap<class_1268, State> states = new EnumMap<>(class_1268.class);

    public static SlimeInHandPhysics get() {
        return INSTANCE;
    }

    private SlimeInHandPhysics() {
    }

    public void updateFrame(class_746 player) {
        if (player == null) {
            reset();
            return;
        }
        long now = System.nanoTime();
        float dt = (now - this.lastUpdateTime) / 1.0E9f;
        this.lastUpdateTime = now;
        if (dt <= 0.0f) {
            return;
        }
        if (dt > 0.1f) {
            dt = 0.1f;
        }
        this.frameDt = dt;
        this.frameOnGround = player.method_24828();
        this.frameNoGravity = player.method_5740();
        this.frameDeltaY = (float) player.method_18798().field_1351;
        this.frameFallDistance = (float) player.field_6017;
        this.frameJumpPressed = class_310.method_1551().field_1690.field_1903.method_1434();
        this.frameValid = true;
        this.frameId++;
    }

    public void applySlimeBall(class_4587 poseStack, class_1268 hand, float partialTicks) {
        if (poseStack == null || hand == null) {
            return;
        }
        float value = sampleDeform(hand, partialTicks);
        if (Math.abs(value) < 1.0E-4f) {
            return;
        }
        float scaleV = verticalScaleForDeform(value);
        float scaleH = horizontalScaleForDeform(value);
        applyCameraAlignedScale(poseStack, scaleH, scaleV);
    }

    public void applySlimeBlock(class_4587 poseStack, class_1268 hand, float partialTicks) {
        if (poseStack == null || hand == null) {
            return;
        }
        float value = sampleDeform(hand, partialTicks);
        if (Math.abs(value) < 1.0E-4f) {
            return;
        }
        float scaleV = verticalScaleForDeform(value);
        float scaleH = horizontalScaleForDeform(value);
        poseStack.method_46416(0.0f, -0.5f, 0.0f);
        applyCameraAlignedScale(poseStack, scaleH, scaleV);
        poseStack.method_46416(0.0f, 0.5f, 0.0f);
    }

    public float sampleSlimeBlockDeform(class_1268 hand, float partialTicks) {
        return sampleDeform(hand, partialTicks);
    }

    public static float horizontalScaleForDeform(float value) {
        return 1.0f - (value * 0.5f);
    }

    public static float verticalScaleForDeform(float value) {
        return 1.0f + value;
    }

    private float sampleDeform(class_1268 hand, float partialTicks) {
        if (hand == null || !this.frameValid) {
            return 0.0f;
        }
        State state = (State) this.states.computeIfAbsent(hand, key -> {
            return new State();
        });
        if (state.lastFrameId != this.frameId) {
            state.update(this.frameDt, this.frameOnGround, this.frameNoGravity, this.frameDeltaY, this.frameFallDistance, this.frameJumpPressed);
            state.lastFrameId = this.frameId;
        }
        return class_3532.method_16439(partialTicks, state.prevValue, state.value);
    }

    private void reset() {
        this.lastUpdateTime = System.nanoTime();
        this.frameValid = false;
        this.frameId = 0L;
        this.states.clear();
    }

    public void resetHand(class_1268 hand) {
        State state;
        if (hand != null && (state = this.states.get(hand)) != null) {
            state.reset();
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/SlimeInHandPhysics$State.class */
    private static final class State {
        float value;
        float velocity;
        float prevValue;
        float lastFallSpeed;
        float lastFallDistance;
        boolean prevOnGround = true;
        long lastFrameId = -1;

        private State() {
        }

        void reset() {
            this.value = 0.0f;
            this.velocity = 0.0f;
            this.prevValue = 0.0f;
            this.lastFallSpeed = 0.0f;
            this.lastFallDistance = 0.0f;
            this.prevOnGround = true;
            this.lastFrameId = -1L;
        }

        void update(float dt, boolean onGround, boolean noGravity, float deltaY, float fallDistance, boolean jumpPressed) {
            this.prevValue = this.value;
            if (this.prevOnGround && !onGround) {
                float impulse = jumpPressed ? SlimeInHandPhysics.JUMP_IMPULSE : -1.17f;
                this.velocity += impulse;
            } else if (!this.prevOnGround && onGround) {
                float impactFactor = Math.max(class_3532.method_15363(this.lastFallDistance / SlimeInHandPhysics.FALL_DISTANCE_MAX, 0.0f, 1.0f), class_3532.method_15363(this.lastFallSpeed / 0.6f, 0.0f, 1.0f));
                this.velocity += SlimeInHandPhysics.LAND_IMPULSE * (0.6f + (0.7f * impactFactor));
                this.lastFallSpeed = 0.0f;
                this.lastFallDistance = 0.0f;
            }
            this.prevOnGround = onGround;
            float target = 0.0f;
            if (!onGround && !noGravity) {
                if (deltaY < -0.001f) {
                    float fallSpeed = class_3532.method_15363(-deltaY, 0.0f, 0.6f);
                    float speedFactor = fallSpeed / 0.6f;
                    float distanceFactor = class_3532.method_15363(fallDistance / SlimeInHandPhysics.FALL_DISTANCE_MAX, 0.0f, 1.0f);
                    target = 0.6f * Math.max(speedFactor, distanceFactor);
                    this.lastFallSpeed = Math.max(this.lastFallSpeed, fallSpeed);
                    this.lastFallDistance = Math.max(this.lastFallDistance, fallDistance);
                } else if (deltaY > 0.001f) {
                    float riseSpeed = class_3532.method_15363(deltaY, 0.0f, 0.6f);
                    float speedFactor2 = riseSpeed / 0.6f;
                    target = (-0.45f) * speedFactor2;
                }
            }
            float accel = ((-192.375f) * (this.value - target)) - (SlimeInHandPhysics.DAMPING * this.velocity);
            this.velocity += accel * dt;
            this.value += this.velocity * dt;
            this.value = class_3532.method_15363(this.value, -0.9f, SlimeInHandPhysics.MAX_DEFORM);
            if (Math.abs(this.value) < 1.0E-4f && Math.abs(this.velocity) < 1.0E-4f && target == 0.0f) {
                this.value = 0.0f;
                this.velocity = 0.0f;
            }
        }
    }

    private static void applyCameraAlignedScale(class_4587 poseStack, float xzScale, float yScale) {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1773 == null || mc.field_1773.method_19418() == null) {
            poseStack.method_22905(xzScale, yScale, xzScale);
            return;
        }
        Quaternionf camRot = mc.field_1773.method_19418().method_23767();
        if (camRot == null) {
            poseStack.method_22905(xzScale, yScale, xzScale);
            return;
        }
        poseStack.method_22907(camRot);
        poseStack.method_22905(xzScale, yScale, xzScale);
        poseStack.method_22907(new Quaternionf(camRot).invert());
    }
}
