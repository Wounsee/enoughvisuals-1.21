package fun.enoughvisuals.systems.emotions.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Arm;

/** Render-thread scope that lets held items consume the BOBJ forearm instead of the upper arm. */
@Environment(EnvType.CLIENT)
public final class EmotionAttachmentContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private EmotionAttachmentContext() {
    }

    public static void begin(PlayerEntityModel model, BobjRig.Pose pose,
                             ModelTransform rightArmBase, ModelTransform leftArmBase) {
        if (model == null || pose == null) {
            CURRENT.remove();
            return;
        }
        CURRENT.set(new State(model, pose, rightArmBase, leftArmBase));
    }

    public static HandScope beginHand(PlayerEntityModel model, Arm arm) {
        State state = CURRENT.get();
        if (state == null || state.model != model) return null;

        ModelPart part = arm == Arm.LEFT ? model.leftArm : model.rightArm;
        String boneName = arm == Arm.LEFT ? "low_left_arm" : "low_right_arm";
        org.joml.Matrix4f skinning = state.pose.skinningMatrix(boneName).orElse(null);
        if (skinning == null) return null;

        ModelTransform original = part.getTransform();
        ModelTransform base = arm == Arm.LEFT ? state.leftArmBase : state.rightArmBase;
        part.setTransform(base);
        BobjSpace.applySkinningToPart(skinning, part);
        return new HandScope(part, original);
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record State(PlayerEntityModel model, BobjRig.Pose pose,
                         ModelTransform rightArmBase, ModelTransform leftArmBase) {
    }

    public static final class HandScope {
        private ModelPart part;
        private ModelTransform original;

        private HandScope(ModelPart part, ModelTransform original) {
            this.part = part;
            this.original = original;
        }

        public void restore() {
            if (part == null || original == null) return;
            part.setTransform(original);
            part = null;
            original = null;
        }
    }
}
