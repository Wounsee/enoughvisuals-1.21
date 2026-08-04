package punchy.client.access;

import org.joml.Matrix4f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/access/TransformablePart.class */
public interface TransformablePart {
    void punchy$setExplicitTransform(Matrix4f matrix4f);

    Matrix4f punchy$getExplicitTransform();

    void punchy$setDirectPoseBase(float f, float f2, float f3, float f4, float f5, float f6);

    boolean punchy$hasDirectPoseBase();

    float punchy$getDirectPoseBaseX();

    float punchy$getDirectPoseBaseY();

    float punchy$getDirectPoseBaseZ();

    float punchy$getDirectPoseBaseXRot();

    float punchy$getDirectPoseBaseYRot();

    float punchy$getDirectPoseBaseZRot();

    void punchy$clearDirectPoseBase();
}
