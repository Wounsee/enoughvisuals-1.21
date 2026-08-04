package punchy.client.state;

import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_746;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ClimbStateMachine.class */
public class ClimbStateMachine {
    private static final String CLIMB_CLIP_NAME = "climb";
    private static final float RESUME_BLEND_IN_SECONDS = 0.25f;
    private static final int EQUIP_RELEASE_TICKS = 6;
    private static final int SWAP_HOLD_TICKS = 12;
    private boolean wasClimbing;
    private boolean suspendedByMain;
    private class_1306 suppressedMainArm;
    private boolean equipSequenceActive;
    private int equipReleaseTicks;
    private boolean equipTransitionActiveThisTick;
    private boolean swapHoldActiveThisTick;
    private int swapHoldTicks;
    private boolean swapKeyDown;
    private class_1799 lastMain = class_1799.field_8037;
    private class_1799 lastOff = class_1799.field_8037;
    private boolean swapSuspendActive;
    private static boolean suppressSwimForSwap;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            stopClimbIfNeeded();
            return;
        }
        if (!PunchyConfig.enableClimbAnimation()) {
            stopClimbIfNeeded();
            return;
        }
        if (!client.field_1690.method_31044().method_31034()) {
            stopClimbIfNeeded();
            return;
        }
        class_746 player = client.field_1724;
        if (!isClimbingUp(client, player)) {
            stopClimbIfNeeded();
            return;
        }
        boolean mainActive = isMainAnimationActive();
        if (mainActive && isMainDualHanded()) {
            suspendForMainAnimation();
            return;
        }
        AnimationClip clip = PunchyAnimationManager.resolveCustomClip(CLIMB_CLIP_NAME);
        if (clip == null) {
            stopClimbIfNeeded();
            return;
        }
        if (!this.wasClimbing) {
            this.lastMain = copyStack(player.method_6047());
            this.lastOff = copyStack(player.method_6079());
            this.swapHoldTicks = 0;
            this.swapSuspendActive = false;
        }
        PoseHandler handler = PunchyAnimationManager.SWIM_HANDLER;
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        this.swapHoldActiveThisTick = updateSwapHold(client, player);
        suppressSwimForSwap = false;
        boolean equipTransition = updateEquipSequenceState();
        this.equipTransitionActiveThisTick = equipTransition;
        if (equipTransition) {
            class_1306 equipArm = resolveMainArm();
            if (equipArm != null) {
                this.suppressedMainArm = equipArm;
                handler.requestSkipArm(equipArm);
            } else if (this.suppressedMainArm != null) {
                handler.requestSkipArm(this.suppressedMainArm);
            }
        }
        if (mainActive) {
            suppressClimbArmForMainAnimation();
        } else if (!equipTransition && this.suppressedMainArm != null) {
            handler.requestBlendInArm(this.suppressedMainArm);
            this.suppressedMainArm = null;
        }
        if (!equipTransition && this.suppressedMainArm != null) {
            handler.setBlendInDurationNext(RESUME_BLEND_IN_SECONDS);
        }
        if (!mainActive && this.suspendedByMain) {
            handler.setBlendInDurationNext(RESUME_BLEND_IN_SECONDS);
            this.suspendedByMain = false;
        }
        handler.setLoopSpeedParams(Float.valueOf(1.0f), Float.valueOf(0.0f));
        handler.setIgnoreDualHanded(false);
        handler.setActiveArm(null);
        handler.setLooping(true);
        handler.setFrozen(false);
        handler.setPaused(!isMoving(client));
        if (!handler.isPlaying() || handler.getCurrentClip() != clip) {
            handler.play(clip, speedMultiplier, false, false);
        }
        this.lastMain = copyStack(player.method_6047());
        this.lastOff = copyStack(player.method_6079());
        this.wasClimbing = true;
    }

    private boolean isClimbingUp(class_310 client, class_746 player) {
        if (client == null || player == null || !player.method_6101()) {
            return false;
        }
        double dy = player.method_18798().field_1351;
        if (dy > 0.02d) {
            return true;
        }
        if (client.field_1690 == null) {
            return false;
        }
        return client.field_1690.field_1894.method_1434() || client.field_1690.field_1903.method_1434();
    }

    private void suspendForMainAnimation() {
        PoseHandler handler = PunchyAnimationManager.SWIM_HANDLER;
        AnimationClip clip = handler.getCurrentClip();
        if (clip != null && PunchyAnimationManager.isClipNamed(clip, CLIMB_CLIP_NAME)) {
            handler.setLooping(false);
            handler.setPaused(false);
            handler.setFrozen(false);
            handler.stopImmediateWithBlendOut();
            this.suspendedByMain = true;
            this.suppressedMainArm = null;
        }
    }

    private void stopClimbIfNeeded() {
        if (!this.wasClimbing) {
            return;
        }
        PoseHandler handler = PunchyAnimationManager.SWIM_HANDLER;
        AnimationClip clip = handler.getCurrentClip();
        if (clip != null && PunchyAnimationManager.isClipNamed(clip, CLIMB_CLIP_NAME)) {
            handler.setLooping(false);
            handler.setPaused(false);
            handler.setFrozen(false);
            handler.stopImmediateWithBlendOut();
        }
        this.wasClimbing = false;
        this.suspendedByMain = false;
        this.suppressedMainArm = null;
        this.equipSequenceActive = false;
        this.equipReleaseTicks = 0;
        this.equipTransitionActiveThisTick = false;
        this.swapHoldActiveThisTick = false;
        this.swapHoldTicks = 0;
        this.swapKeyDown = false;
        this.lastMain = class_1799.field_8037;
        this.lastOff = class_1799.field_8037;
        this.swapSuspendActive = false;
        suppressSwimForSwap = false;
    }

    private boolean isMoving(class_310 client) {
        if (client == null || client.field_1690 == null || client.field_1724 == null) {
            return false;
        }
        boolean forward = client.field_1690.field_1894.method_1434();
        boolean back = client.field_1690.field_1881.method_1434();
        boolean left = client.field_1690.field_1913.method_1434();
        boolean right = client.field_1690.field_1849.method_1434();
        boolean jump = client.field_1690.field_1903.method_1434();
        boolean sneak = client.field_1690.field_1832.method_1434();
        if (forward || back || left || right || jump || sneak) {
            return true;
        }
        return client.field_1724.method_6101() ? Math.abs(client.field_1724.method_18798().field_1351) > 0.001d : Math.abs(client.field_1724.field_6212) > 0.001f || Math.abs(client.field_1724.field_6250) > 0.001f;
    }

    private boolean isMainAnimationActive() {
        return PunchyAnimationManager.POSE_HANDLER.isPlaying() && PunchyAnimationManager.POSE_HANDLER.getCurrentClip() != null;
    }

    private boolean isEquipClipActive() {
        AnimationClip clip;
        if (!isMainAnimationActive() || (clip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        String name = clip.getName();
        return "hand_in".equals(name) || "hand_out".equals(name);
    }

    private boolean isMainDualHanded() {
        if (!isMainAnimationActive()) {
            return false;
        }
        if (PunchyAnimationManager.isDualHandedActive()) {
            return true;
        }
        return PunchyAnimationManager.POSE_HANDLER.getActiveArm() == null && PunchyAnimationManager.getSourceArm() == null && PunchyAnimationManager.POSE_HANDLER.getActiveArm() == null;
    }

    private class_1306 resolveMainArm() {
        if (!isMainAnimationActive() || isMainDualHanded()) {
            return null;
        }
        class_1306 active = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
        if (active != null) {
            return active;
        }
        return PunchyAnimationManager.getSourceArm();
    }

    private void suppressClimbArmForMainAnimation() {
        class_1306 equipArm;
        PoseHandler handler = PunchyAnimationManager.SWIM_HANDLER;
        if (!handler.isPlaying()) {
            this.suppressedMainArm = null;
            return;
        }
        AnimationClip clip = handler.getCurrentClip();
        if (!PunchyAnimationManager.isClipNamed(clip, CLIMB_CLIP_NAME)) {
            this.suppressedMainArm = null;
            return;
        }
        class_1306 mainArm = resolveMainArm();
        boolean equipTransition = this.equipTransitionActiveThisTick;
        if (isEquipClipActive() && equipTransition && (equipArm = resolveMainArm()) != null) {
            mainArm = equipArm;
        }
        if (mainArm != null) {
            handler.requestSkipArm(mainArm);
            this.suppressedMainArm = mainArm;
        } else if (this.suppressedMainArm != null && !equipTransition) {
            handler.requestBlendInArm(this.suppressedMainArm);
            this.suppressedMainArm = null;
        }
    }

    private boolean isEquipTransitionActive() {
        return HandEquipStateMachine.isEquipInProgress() || HandEquipStateMachine.getActiveType() != null;
    }

    private boolean isEquipActiveRaw() {
        if (HandEquipStateMachine.isEquipInProgress() || HandEquipStateMachine.getActiveType() != null) {
            return true;
        }
        return isEquipClipActive();
    }

    private boolean updateEquipSequenceState() {
        boolean activeNow = isEquipTransitionActive() || isEquipClipActive();
        if (activeNow) {
            this.equipSequenceActive = true;
            this.equipReleaseTicks = EQUIP_RELEASE_TICKS;
            return true;
        }
        if (!this.equipSequenceActive) {
            return false;
        }
        if (this.equipReleaseTicks > 0) {
            this.equipReleaseTicks--;
            return true;
        }
        this.equipSequenceActive = false;
        return false;
    }

    private boolean updateSwapHold(class_310 client, class_746 player) {
        if (player == null || client == null || client.field_1690 == null) {
            this.swapHoldTicks = 0;
            return false;
        }
        boolean down = client.field_1690.field_1831.method_1434();
        boolean pressed = down && !this.swapKeyDown;
        this.swapKeyDown = down;
        if (pressed) {
            this.swapHoldTicks = SWAP_HOLD_TICKS;
            return true;
        }
        class_1799 currentMain = player.method_6047();
        class_1799 currentOff = player.method_6079();
        boolean mainChanged = !isSameStack(this.lastMain, currentMain);
        boolean offChanged = !isSameStack(this.lastOff, currentOff);
        boolean swapDetected = false;
        if (mainChanged || offChanged) {
            boolean lastMainEmpty = isEmpty(this.lastMain);
            boolean lastOffEmpty = isEmpty(this.lastOff);
            boolean currentMainEmpty = isEmpty(currentMain);
            boolean currentOffEmpty = isEmpty(currentOff);
            if (!lastMainEmpty && !lastOffEmpty && isSameStack(currentMain, this.lastOff) && isSameStack(currentOff, this.lastMain)) {
                swapDetected = true;
            } else if (!lastMainEmpty && lastOffEmpty && currentMainEmpty && isSameStack(currentOff, this.lastMain)) {
                swapDetected = true;
            } else if (lastMainEmpty && !lastOffEmpty && currentOffEmpty && isSameStack(currentMain, this.lastOff)) {
                swapDetected = true;
            }
        }
        if (swapDetected) {
            this.swapHoldTicks = EQUIP_RELEASE_TICKS;
        }
        boolean active = this.swapHoldTicks > 0;
        if (this.swapHoldTicks > 0 && !swapDetected) {
            this.swapHoldTicks--;
        }
        return active;
    }

    private boolean isSameStack(class_1799 a, class_1799 b) {
        if (isEmpty(a) && isEmpty(b)) {
            return true;
        }
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        return class_1799.method_31577(a, b);
    }

    private boolean isEmpty(class_1799 stack) {
        return stack == null || stack.method_7960();
    }

    private class_1799 copyStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return class_1799.field_8037;
        }
        return stack.method_7972();
    }

    public static boolean shouldSuppressSwimForSwap() {
        return suppressSwimForSwap;
    }
}
