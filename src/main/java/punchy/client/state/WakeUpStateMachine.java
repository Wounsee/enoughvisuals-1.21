package punchy.client.state;

import net.minecraft.class_1268;
import net.minecraft.class_310;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/WakeUpStateMachine.class */
public class WakeUpStateMachine {
    private boolean wasSleeping;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            this.wasSleeping = false;
            return;
        }
        boolean sleeping = client.field_1724.method_6113();
        if (sleeping) {
            this.wasSleeping = true;
        } else if (this.wasSleeping) {
            this.wasSleeping = false;
            triggerWakeUp();
        }
    }

    private void triggerWakeUp() {
        AnimationClip clip = PunchyAnimationManager.WAKE_UP_CLIP;
        if (clip == null) {
            return;
        }
        AnimationClip current = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        if (PunchyAnimationManager.POSE_HANDLER.isPlaying() && current != null && !PunchyAnimationManager.isClipNamed(current, "wake_up")) {
            return;
        }
        class_310 client = class_310.method_1551();
        PunchyAnimationManager.setSourceHand(client, class_1268.field_5808);
        PunchyAnimationManager.POSE_HANDLER.setActiveArm(PunchyAnimationManager.getUseArm(client, class_1268.field_5808));
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose();
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, false);
    }
}
