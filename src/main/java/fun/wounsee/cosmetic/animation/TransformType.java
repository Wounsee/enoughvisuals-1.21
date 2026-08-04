package fun.wounsee.cosmetic.animation;

import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.model.CosmeticModelPart;

public enum TransformType {
    POSITION(CosmeticModelPart::animPosition),
    ROTATION(CosmeticModelPart::animRotation),
    GLOBAL_ROT(CosmeticModelPart::globalAnimRot),
    SCALE(CosmeticModelPart::animScale);

    private final ITransform function;

    TransformType(ITransform function) {
        this.function = function;
    }

    public void apply(CosmeticModelPart part, FiguraVec3 vec, boolean merge) {
        this.function.apply(part, vec, merge);
    }

    private interface ITransform {
        void apply(CosmeticModelPart part, FiguraVec3 vec, boolean merge);
    }
}
