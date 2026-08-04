package punchy.client.state;

import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1764;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.state.CustomAnimationResolver;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/CrossbowStateMachine.class */
public class CrossbowStateMachine {
    private boolean wasUsing;
    private AnimationClip activeClip;
    private boolean lastChargedMain;
    private boolean lastChargedOff;
    private boolean lastUseDown;
    private boolean suppressChargeLoop;

    public void tick(class_310 client) {
        int chargeDuration;
        if (client == null || client.field_1724 == null || HandEquipStateMachine.isEquipInProgress()) {
            return;
        }
        class_1799 usingStack = client.field_1724.method_6030();
        boolean isCrossbow = usingStack.method_7909() instanceof class_1764;
        boolean charged = isCrossbow && class_1764.method_7781(usingStack);
        boolean autoCharged = false;
        if (isCrossbow && client.field_1724.method_6115() && (chargeDuration = class_1764.method_7775(usingStack, client.field_1724)) > 0) {
            int elapsed = usingStack.method_7935(client.field_1724) - client.field_1724.method_6014();
            if (elapsed >= chargeDuration) {
                charged = true;
                autoCharged = true;
                this.suppressChargeLoop = true;
            }
        }
        boolean useDown = client.field_1690 != null && client.field_1690.field_1904.method_1434();
        boolean usingCrossbow = client.field_1724.method_6115() && isCrossbow && useDown && !charged;
        class_1268 usedHand = client.field_1724.method_6058();
        if (usingCrossbow && PunchyAnimationManager.isHandBlacklisted(client, usedHand)) {
            if (this.wasUsing && !autoCharged) {
                PunchyAnimationManager.POSE_HANDLER.setLooping(false);
                PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
                this.activeClip = null;
                PunchyAnimationManager.clearSourceHand();
            }
            this.suppressChargeLoop = false;
            this.wasUsing = false;
            this.lastUseDown = useDown;
            return;
        }
        if (usingCrossbow && HandEquipStateMachine.isEquipInProgress() && !HandEquipStateMachine.abortEquipForAction(client, usedHand)) {
            return;
        }
        if (!useDown || !isCrossbow) {
            this.suppressChargeLoop = false;
        }
        boolean click = useDown && !this.lastUseDown;
        if (this.suppressChargeLoop) {
            usingCrossbow = false;
        }
        if (usingCrossbow) {
            ItemAnimationManager.setPersistentType(client.field_1724.method_6058(), "useItem", true, true);
            if (!this.wasUsing || !PunchyAnimationManager.POSE_HANDLER.isPlaying() || this.activeClip == null) {
                playCrossbow(client, client.field_1724.method_6058());
            }
        } else if (this.wasUsing && !autoCharged) {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
            PunchyAnimationManager.POSE_HANDLER.setLooping(false);
            PunchyAnimationManager.POSE_HANDLER.stopImmediateWithBlendOut();
            this.activeClip = null;
            PunchyAnimationManager.clearSourceHand();
        } else {
            ItemAnimationManager.clearPersistentType(class_1268.field_5808, "useItem");
            ItemAnimationManager.clearPersistentType(class_1268.field_5810, "useItem");
        }
        if (click) {
            handleImmediateShot(client);
        }
        handleShot(client, useDown || this.lastUseDown);
        this.wasUsing = usingCrossbow && !autoCharged;
        this.lastUseDown = useDown;
    }

    private void playCrossbow(class_310 client, class_1268 hand) {
        int chargeTicks;
        AnimationClip clip = resolveCrossbowClip(client, hand);
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
        boolean loop = !hold;
        if (client.field_1724 != null) {
            class_1799 stack = client.field_1724.method_6030();
            if ((stack.method_7909() instanceof class_1764) && (chargeTicks = class_1764.method_7775(stack, client.field_1724)) > 0) {
                float chargeSeconds = chargeTicks / 20.0f;
                loop = clip.getLength() < chargeSeconds;
            }
        }
        PunchyAnimationManager.POSE_HANDLER.setLooping(loop);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        boolean skipBlendIn = CustomAnimationResolver.shouldSkipBlendInOnIdlePose();
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, skipBlendIn, hold);
        this.activeClip = clip;
    }

    private AnimationClip resolveCrossbowClip(class_310 client, class_1268 hand) {
        if (client != null && client.field_1724 != null) {
            class_1799 stack = client.field_1724.method_5998(hand);
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "use_crossbow");
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "use_crossbow", entry, client, hand);
            if (lookup.clip() != null) {
                return lookup.clip();
            }
        }
        return PunchyAnimationManager.resolveHandedClip("use_crossbow", client, hand);
    }

    private void handleShot(class_310 client, boolean shouldFire) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_1799 mainStack = client.field_1724.method_6047();
        class_1799 offStack = client.field_1724.method_6079();
        boolean mainCharged = (mainStack.method_7909() instanceof class_1764) && class_1764.method_7781(mainStack);
        boolean offCharged = (offStack.method_7909() instanceof class_1764) && class_1764.method_7781(offStack);
        if (shouldFire && this.lastChargedMain && !mainCharged) {
            playShot(client, class_1268.field_5808);
        }
        if (shouldFire && this.lastChargedOff && !offCharged) {
            playShot(client, class_1268.field_5810);
        }
        this.lastChargedMain = mainCharged;
        this.lastChargedOff = offCharged;
    }

    private void handleImmediateShot(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        class_1799 mainStack = client.field_1724.method_6047();
        class_1799 offStack = client.field_1724.method_6079();
        boolean mainCharged = (mainStack.method_7909() instanceof class_1764) && class_1764.method_7781(mainStack);
        boolean offCharged = (offStack.method_7909() instanceof class_1764) && class_1764.method_7781(offStack);
        if (mainCharged) {
            playShot(client, class_1268.field_5808);
            this.lastChargedMain = false;
        } else if (offCharged) {
            playShot(client, class_1268.field_5810);
            this.lastChargedOff = false;
        }
    }

    private void playShot(class_310 client, class_1268 hand) {
        AnimationClip clip = null;
        if (client != null && client.field_1724 != null) {
            class_1799 stack = client.field_1724.method_5998(hand);
            PunchyTuningConfig.CustomAnimationEntry entry = CustomAnimationResolver.findEntry(stack, "use_crossbow_shot");
            CustomAnimationResolver.ClipLookup lookup = CustomAnimationResolver.resolveNamedOrTypeFallback(stack, "use_crossbow_shot", entry, client, hand);
            clip = lookup.clip();
        }
        if (clip == null) {
            clip = PunchyAnimationManager.resolveHandedClip("use_crossbow_shot", client, hand);
        }
        if (clip == null) {
            return;
        }
        if (PunchyAnimationManager.POSE_HANDLER.getCurrentClip() == clip && PunchyAnimationManager.POSE_HANDLER.isPlaying()) {
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
        PunchyAnimationManager.POSE_HANDLER.setLooping(false);
        float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(clip);
        PunchyAnimationManager.POSE_HANDLER.play(clip, speedMultiplier, true, false);
    }
}
