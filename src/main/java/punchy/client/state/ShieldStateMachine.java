package punchy.client.state;

import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_1819;
import net.minecraft.class_310;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/ShieldStateMachine.class */
public class ShieldStateMachine {
    private boolean wasUsing;
    private AnimationClip activeClip;
    private class_1268 lastShieldHand;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        boolean isShield = usingStack.method_7909() instanceof class_1819;
        class_1268 usedHand = client.field_1724.method_6115() ? client.field_1724.method_6058() : this.lastShieldHand;
        boolean visualHold = false;
        boolean usingShield = client.field_1724.method_6115() && isShield;
        if (usingShield) {
            this.lastShieldHand = usedHand;
        } else if (this.wasUsing && shouldKeepShieldVisualHold(client)) {
            usedHand = this.lastShieldHand;
            usingShield = true;
            visualHold = true;
        }
        if (usingShield && PunchyAnimationManager.isHandBlacklisted(client, usedHand)) {
            if (this.wasUsing) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                PunchyAnimationManager.clearSourceHand();
                this.activeClip = null;
            }
            this.wasUsing = false;
            this.lastShieldHand = null;
            return;
        }
        if (usingShield && !visualHold && HandEquipStateMachine.isEquipInProgress() && !HandEquipStateMachine.isOppositeShieldEquipInProgress(usedHand) && !HandEquipStateMachine.abortEquipForAction(client, usedHand)) {
            return;
        }
        if (usingShield) {
            if (!this.wasUsing || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || this.activeClip == null) {
                playShield(client, usedHand);
            }
        } else if (this.wasUsing) {
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            PunchyAnimationManager.clearSourceHand();
            this.activeClip = null;
            this.lastShieldHand = null;
        }
        this.wasUsing = usingShield;
    }

    private boolean shouldKeepShieldVisualHold(class_310 client) {
        class_1799 stack;
        return (client == null || client.field_1724 == null || client.field_1690 == null || this.lastShieldHand == null || !client.field_1690.field_1904.method_1434() || (stack = client.field_1724.method_5998(this.lastShieldHand)) == null || !(stack.method_7909() instanceof class_1819)) ? false : true;
    }

    private void playShield(class_310 client, class_1268 hand) {
        AnimationClip clip = resolveShieldClip(client, hand);
        if (clip == null) {
            return;
        }
        PunchyAnimationManager.setSourceHand(client, hand);
        boolean dualHanded = PunchyAnimationManager.isDualHandedActive(client, clip, 0.0f);
        if (dualHanded) {
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(null);
        } else {
            class_1306 arm = PunchyAnimationManager.getUseArm(client, hand);
            PunchyAnimationManager.POSE_HANDLER.setActiveArm(arm);
        }
        boolean hold = clip.isHoldOnLastFrame();
        PunchyAnimationManager.POSE_HANDLER.setLooping(!hold);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, true, hold);
        this.activeClip = clip;
    }

    private AnimationClip resolveShieldClip(class_310 client, class_1268 hand) {
        if (client != null && client.field_1724 != null) {
            class_1799 stack = client.field_1724.method_5998(hand);
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "use_shield");
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "use_shield", entry, client, hand);
            if (lookup.clip() != null) {
                return lookup.clip();
            }
        }
        return PunchyAnimationManager.resolveHandedClip("use_shield", client, hand);
    }
}
