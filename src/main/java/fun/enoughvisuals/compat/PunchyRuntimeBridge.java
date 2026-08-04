package fun.enoughvisuals.compat;

import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.PunchyThirdPersonAnimationManager;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.client.render.CameraAnimationState;
import punchy.client.render.UseAnimationSuppressor;
import punchy.client.state.AttackActionTracker;
import punchy.client.state.InspectActionTracker;
import punchy.client.state.MiningActionTracker;
import punchy.client.state.TriggerableActionTracker;
import punchy.client.state.UseActionTracker;

/** Lifecycle boundary between the module system and the embedded Punchy runtime. */
public final class PunchyRuntimeBridge {
    private PunchyRuntimeBridge() {}

    public static void resetToVanilla() {
        PunchyAnimationManager.POSE_HANDLER.resetImmediate();
        PunchyAnimationManager.SWIM_HANDLER.resetImmediate();
        PunchyThirdPersonAnimationManager.POSE_HANDLER.resetImmediate();
        PunchyAnimationManager.clearSourceHand();
        PunchyAnimationManager.clearAllEquipOverrides();
        PunchyAnimationManager.setSuppressOffhandForAttack(null, false);
        ItemAnimationManager.clearAllPersistentTypes();
        AttackActionTracker.clear();
        InspectActionTracker.clear();
        TriggerableActionTracker.clear();
        UseActionTracker.clear();
        MiningActionTracker.recordStop();
        CameraAnimationState.clear();
        UseAnimationSuppressor.clear();
        UseAnimationSuppressor.clearReleaseStart();
        UseAnimationSuppressor.clearMeshStart();
        UseAnimationSuppressor.force(false);
        ModelPartsParticleSystem.resetAll();
    }
}
