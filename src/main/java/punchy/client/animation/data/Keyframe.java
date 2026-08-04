package punchy.client.animation.data;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/Keyframe.class */
public final class Keyframe extends Record {
    private final float timeStamp;
    private final Vector3f position;
    private final Vector3f rotationEuler;
    private final Quaternionf rotation;
    private final Vector3f scale;
    private final String interpolation;
    private final Vector3f positionV2;
    private final Vector3f rotationPreEulerV2;
    private final Vector3f rotationPostEulerV2;
    private final VectorExpression positionExpression;
    private final VectorExpression rotationExpression;
    private final VectorExpression scaleExpression;

    public Keyframe(float timeStamp, Vector3f position, Vector3f rotationEuler, Quaternionf rotation, Vector3f scale, String interpolation, Vector3f positionV2, Vector3f rotationPreEulerV2, Vector3f rotationPostEulerV2, VectorExpression positionExpression, VectorExpression rotationExpression, VectorExpression scaleExpression) {
        this.timeStamp = timeStamp;
        this.position = position;
        this.rotationEuler = rotationEuler;
        this.rotation = rotation;
        this.scale = scale;
        this.interpolation = interpolation;
        this.positionV2 = positionV2;
        this.rotationPreEulerV2 = rotationPreEulerV2;
        this.rotationPostEulerV2 = rotationPostEulerV2;
        this.positionExpression = positionExpression;
        this.rotationExpression = rotationExpression;
        this.scaleExpression = scaleExpression;
    }

    @Override // java.lang.Record
    public final String toString() {
        return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, Keyframe.class), Keyframe.class, "timeStamp;position;rotationEuler;rotation;scale;interpolation;positionV2;rotationPreEulerV2;rotationPostEulerV2;positionExpression;rotationExpression;scaleExpression", "FIELD:Lpunchy/client/animation/data/Keyframe;->timeStamp:F", "FIELD:Lpunchy/client/animation/data/Keyframe;->position:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationEuler:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotation:Lorg/joml/Quaternionf;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scale:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->interpolation:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPreEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPostEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scaleExpression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
    }

    @Override // java.lang.Record
    public final int hashCode() {
        return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, Keyframe.class), Keyframe.class, "timeStamp;position;rotationEuler;rotation;scale;interpolation;positionV2;rotationPreEulerV2;rotationPostEulerV2;positionExpression;rotationExpression;scaleExpression", "FIELD:Lpunchy/client/animation/data/Keyframe;->timeStamp:F", "FIELD:Lpunchy/client/animation/data/Keyframe;->position:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationEuler:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotation:Lorg/joml/Quaternionf;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scale:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->interpolation:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPreEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPostEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scaleExpression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
    }

    @Override // java.lang.Record
    public final boolean equals(Object o) {
        return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, Keyframe.class, Object.class), Keyframe.class, "timeStamp;position;rotationEuler;rotation;scale;interpolation;positionV2;rotationPreEulerV2;rotationPostEulerV2;positionExpression;rotationExpression;scaleExpression", "FIELD:Lpunchy/client/animation/data/Keyframe;->timeStamp:F", "FIELD:Lpunchy/client/animation/data/Keyframe;->position:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationEuler:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotation:Lorg/joml/Quaternionf;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scale:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->interpolation:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPreEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationPostEulerV2:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/animation/data/Keyframe;->positionExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->rotationExpression:Lpunchy/client/animation/data/VectorExpression;", "FIELD:Lpunchy/client/animation/data/Keyframe;->scaleExpression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
    }

    public float timeStamp() {
        return this.timeStamp;
    }

    public Vector3f position() {
        return this.position;
    }

    public Vector3f rotationEuler() {
        return this.rotationEuler;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public Vector3f scale() {
        return this.scale;
    }

    public String interpolation() {
        return this.interpolation;
    }

    public Vector3f positionV2() {
        return this.positionV2;
    }

    public Vector3f rotationPreEulerV2() {
        return this.rotationPreEulerV2;
    }

    public Vector3f rotationPostEulerV2() {
        return this.rotationPostEulerV2;
    }

    public VectorExpression positionExpression() {
        return this.positionExpression;
    }

    public VectorExpression rotationExpression() {
        return this.rotationExpression;
    }

    public VectorExpression scaleExpression() {
        return this.scaleExpression;
    }

    public Keyframe(float timeStamp, Vector3f position, Vector3f rotationEuler, Quaternionf rotation, Vector3f scale, String interpolation) {
        this(timeStamp, position, rotationEuler, rotation, scale, interpolation, null, null, null, null, null, null);
    }

    public Keyframe(float timeStamp, Vector3f position, Vector3f rotationEuler, Quaternionf rotation, Vector3f scale, String interpolation, VectorExpression positionExpression, VectorExpression rotationExpression, VectorExpression scaleExpression) {
        this(timeStamp, position, rotationEuler, rotation, scale, interpolation, null, null, null, positionExpression, rotationExpression, scaleExpression);
    }

    public Vector3f sampledPosition(float animTimeSeconds) {
        return sampled(this.position, this.positionExpression, animTimeSeconds, new Vector3f(0.0f, 0.0f, 0.0f));
    }

    public Vector3f sampledPositionV2(float animTimeSeconds) {
        if (this.positionV2 != null) {
            return new Vector3f(this.positionV2);
        }
        return sampledPosition(animTimeSeconds);
    }

    public Vector3f sampledRotationEuler(float animTimeSeconds) {
        return sampled(this.rotationEuler, this.rotationExpression, animTimeSeconds, new Vector3f(0.0f, 0.0f, 0.0f));
    }

    public Vector3f sampledScale(float animTimeSeconds) {
        return sampled(this.scale, this.scaleExpression, animTimeSeconds, new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public boolean hasRotationV2() {
        return (this.rotationPreEulerV2 == null && this.rotationPostEulerV2 == null) ? false : true;
    }

    public Vector3f sampledRotationPreEulerV2(float animTimeSeconds) {
        if (this.rotationPreEulerV2 != null) {
            return new Vector3f(this.rotationPreEulerV2);
        }
        Vector3f legacy = sampledRotationEuler(animTimeSeconds);
        if (legacy == null) {
            return null;
        }
        return new Vector3f(legacy);
    }

    public Vector3f sampledRotationPostEulerV2(float animTimeSeconds) {
        if (this.rotationPostEulerV2 != null) {
            return new Vector3f(this.rotationPostEulerV2);
        }
        Vector3f legacy = sampledRotationEuler(animTimeSeconds);
        if (legacy == null) {
            return null;
        }
        return new Vector3f(legacy);
    }

    private static Vector3f sampled(Vector3f base, VectorExpression expr, float animTimeSeconds, Vector3f fallback) {
        Vector3f resolvedBase = base != null ? base : fallback;
        if (expr == null || !expr.isPresent()) {
            return new Vector3f(resolvedBase);
        }
        return expr.evaluate(resolvedBase, animTimeSeconds);
    }
}
