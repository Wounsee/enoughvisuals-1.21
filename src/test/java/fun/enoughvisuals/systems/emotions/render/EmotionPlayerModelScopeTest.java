package fun.enoughvisuals.systems.emotions.render;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotionPlayerModelScopeTest {
    @Test
    void restoresEveryIncomingPlayerPartFlagExactly() {
        PlayerEntityModel model = model();
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightPants.visible = false;

        EmotionPlayerModelScope scope = new EmotionPlayerModelScope();
        scope.hideBase(model);

        assertFalse(model.head.visible);
        assertFalse(model.body.visible);
        assertFalse(model.rightArm.visible);
        assertFalse(model.leftArm.visible);
        assertFalse(model.rightLeg.visible);
        assertFalse(model.leftLeg.visible);
        assertFalse(model.hat.visible);
        assertFalse(model.jacket.visible);
        assertFalse(model.rightSleeve.visible);
        assertFalse(model.leftSleeve.visible);
        assertFalse(model.rightPants.visible);
        assertFalse(model.leftPants.visible);

        scope.restoreVisibility();

        assertTrue(model.head.visible);
        assertTrue(model.body.visible);
        assertTrue(model.rightArm.visible);
        assertTrue(model.leftArm.visible);
        assertTrue(model.rightLeg.visible);
        assertTrue(model.leftLeg.visible);
        assertFalse(model.hat.visible);
        assertFalse(model.jacket.visible);
        assertTrue(model.rightSleeve.visible);
        assertFalse(model.leftSleeve.visible);
        assertFalse(model.rightPants.visible);
        assertTrue(model.leftPants.visible);
    }

    @Test
    void outerLayerChoicesDoNotRelinquishBaseOwnership() {
        PlayerEntityModel model = model();
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;

        assertTrue(EmotionPlayerModelScope.hasCompleteBase(model));
        model.body.visible = false;
        assertFalse(EmotionPlayerModelScope.hasCompleteBase(model));
    }

    @Test
    void restoresTemporaryAttachmentTransformsAfterFeatures() {
        PlayerEntityModel model = model();
        ModelTransform originalHead = model.head.getTransform();
        ModelTransform originalBody = model.body.getTransform();
        ModelTransform originalRightArm = model.rightArm.getTransform();

        EmotionPlayerModelScope scope = new EmotionPlayerModelScope();
        scope.captureTransforms(model);
        model.head.pivotX = 17.0f;
        model.head.pivotY = 18.0f;
        model.head.pivotZ = 19.0f;
        model.body.setAngles(0.4f, 0.5f, 0.6f);
        model.rightArm.xScale = 1.75f;

        scope.restoreTransforms();

        assertTransformEquals(originalHead, model.head.getTransform());
        assertTransformEquals(originalBody, model.body.getTransform());
        assertTransformEquals(originalRightArm, model.rightArm.getTransform());
    }

    private static void assertTransformEquals(ModelTransform expected, ModelTransform actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected.pivotX(), actual.pivotX());
        org.junit.jupiter.api.Assertions.assertEquals(expected.pivotY(), actual.pivotY());
        org.junit.jupiter.api.Assertions.assertEquals(expected.pivotZ(), actual.pivotZ());
        org.junit.jupiter.api.Assertions.assertEquals(expected.pitch(), actual.pitch());
        org.junit.jupiter.api.Assertions.assertEquals(expected.yaw(), actual.yaw());
        org.junit.jupiter.api.Assertions.assertEquals(expected.roll(), actual.roll());
        org.junit.jupiter.api.Assertions.assertEquals(expected.xScale(), actual.xScale());
        org.junit.jupiter.api.Assertions.assertEquals(expected.yScale(), actual.yScale());
        org.junit.jupiter.api.Assertions.assertEquals(expected.zScale(), actual.zScale());
    }

    private static PlayerEntityModel model() {
        return new PlayerEntityModel(
                PlayerEntityModel.getTexturedModelData(Dilation.NONE, false)
                        .getRoot().createPart(64, 64),
                false);
    }
}
