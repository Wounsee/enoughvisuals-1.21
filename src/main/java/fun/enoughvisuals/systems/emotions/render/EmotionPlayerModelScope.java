package fun.enoughvisuals.systems.emotions.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Per-invocation visibility guard for replacing only the vanilla player base mesh.
 *
 * <p>It deliberately stores exact incoming flags instead of restoring every part to true. Figura
 * and other render integrations are allowed to hide individual vanilla parts before the base draw.
 * Visibility is restored immediately after the vanilla base-mesh draw; temporary attachment
 * transforms stay alive through feature rendering and are restored at the renderer tail.</p>
 */
@Environment(EnvType.CLIENT)
public final class EmotionPlayerModelScope {
    private PlayerEntityModel model;
    private boolean[] visibility;
    private ModelTransform[] transforms;

    /** Core parts are the ownership signal used after Figura's pre-transform hook. */
    public static boolean hasCompleteBase(PlayerEntityModel model) {
        return model.head.visible && model.body.visible
                && model.rightArm.visible && model.leftArm.visible
                && model.rightLeg.visible && model.leftLeg.visible;
    }

    /** Captures the current integration-owned flags and hides the complete vanilla skin mesh. */
    public void hideBase(PlayerEntityModel playerModel) {
        restoreVisibility();
        model = playerModel;
        ModelPart[] parts = parts(playerModel);
        visibility = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            visibility[i] = parts[i].visible;
            parts[i].visible = false;
        }
    }

    /** Captures every transform that the temporary BOBJ attachment proxy may expose to features. */
    public void captureTransforms(PlayerEntityModel playerModel) {
        restoreTransforms();
        model = playerModel;
        ModelPart[] parts = parts(playerModel);
        transforms = new ModelTransform[parts.length];
        for (int i = 0; i < parts.length; i++) {
            transforms[i] = parts[i].getTransform();
        }
    }

    /** Restores exactly the flags observed by {@link #hideBase(PlayerEntityModel)}. */
    public void restoreVisibility() {
        if (model == null || visibility == null) return;
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length && i < visibility.length; i++) {
            parts[i].visible = visibility[i];
        }
        visibility = null;
        clearModelIfIdle();
    }

    /** Restores the shared renderer model after all feature renderers have consumed the proxy. */
    public void restoreTransforms() {
        if (model == null || transforms == null) return;
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length && i < transforms.length; i++) {
            parts[i].setTransform(transforms[i]);
        }
        transforms = null;
        clearModelIfIdle();
    }

    private void clearModelIfIdle() {
        if (visibility == null && transforms == null) model = null;
    }

    private static ModelPart[] parts(PlayerEntityModel model) {
        return new ModelPart[]{
                model.head, model.hat, model.body,
                model.rightArm, model.leftArm, model.rightLeg, model.leftLeg,
                model.jacket, model.rightSleeve, model.leftSleeve,
                model.rightPants, model.leftPants
        };
    }
}
