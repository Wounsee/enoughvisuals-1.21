package punchy.client.state;

import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1753;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/BowStateMachine.class */
public class BowStateMachine {
    private boolean wasUsing;
    private AnimationClip activeClip;
    private class_1268 lastUsedHand;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        boolean isBow = usingStack.method_7909() instanceof class_1753;
        boolean useDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
        boolean usingBow = client.field_1724.method_6115() && isBow && useDown;
        class_1268 usedHand = client.field_1724.method_6058();
        if (usingBow && PunchyAnimationManager.isHandBlacklisted(client, usedHand)) {
            if (this.wasUsing) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.activeClip = null;
                PunchyAnimationManager.clearSourceHand();
            }
            this.wasUsing = false;
            return;
        }
        if (usingBow && HandEquipStateMachine.isEquipInProgress() && !HandEquipStateMachine.abortEquipForAction(client, usedHand)) {
            return;
        }
        if (usingBow) {
            ItemAnimationManager.setPersistentType(usedHand, "useItem", true, true);
            if (this.wasUsing && this.lastUsedHand != null && this.lastUsedHand != usedHand) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.activeClip = null;
                PunchyAnimationManager.clearSourceHand();
                HandEquipStateMachine.requestRenderedSync();
            }
            if (!this.wasUsing || this.lastUsedHand != usedHand || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || this.activeClip == null) {
                playBow(client, usedHand, usingStack);
            }
        } else if (this.wasUsing) {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            this.activeClip = null;
            PunchyAnimationManager.clearSourceHand();
            this.lastUsedHand = null;
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
        }
        this.wasUsing = usingBow;
        this.lastUsedHand = usingBow ? usedHand : null;
    }

    private void playBow(class_310 client, class_1268 hand, class_1799 usingStack) {
        AnimationClip clip = resolveBowClip(client, hand, usingStack);
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
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose();
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, hold);
        this.activeClip = clip;
    }

    private AnimationClip resolveBowClip(class_310 client, class_1268 hand, class_1799 usingStack) {
        PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(usingStack, "use_bow");
        CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(usingStack, "use_bow", entry, client, hand);
        if (lookup.clip() != null) {
            return lookup.clip();
        }
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForStack(usingStack);
        return PunchyAnimationManager.resolveHandedClip("use_bow", client, hand, preferredOption);
    }
}
