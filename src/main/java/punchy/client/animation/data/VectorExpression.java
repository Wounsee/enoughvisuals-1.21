package punchy.client.animation.data;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/VectorExpression.class */
public final class VectorExpression extends Record {
    private final AnimationExpression x;
    private final AnimationExpression y;
    private final AnimationExpression z;

    public VectorExpression(AnimationExpression x, AnimationExpression y, AnimationExpression z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override // java.lang.Record
    public final String toString() {
        return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, VectorExpression.class), VectorExpression.class, "x;y;z", "FIELD:Lpunchy/client/animation/data/VectorExpression;->x:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->y:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->z:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
    }

    @Override // java.lang.Record
    public final int hashCode() {
        return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, VectorExpression.class), VectorExpression.class, "x;y;z", "FIELD:Lpunchy/client/animation/data/VectorExpression;->x:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->y:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->z:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
    }

    @Override // java.lang.Record
    public final boolean equals(Object o) {
        return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, VectorExpression.class, Object.class), VectorExpression.class, "x;y;z", "FIELD:Lpunchy/client/animation/data/VectorExpression;->x:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->y:Lpunchy/client/animation/data/AnimationExpression;", "FIELD:Lpunchy/client/animation/data/VectorExpression;->z:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
    }

    public AnimationExpression x() {
        return this.x;
    }

    public AnimationExpression y() {
        return this.y;
    }

    public AnimationExpression z() {
        return this.z;
    }

    public boolean isPresent() {
        return (this.x == null && this.y == null && this.z == null) ? false : true;
    }

    public Vector3f evaluate(Vector3f base, float animTimeSeconds) {
        Vector3f out = base != null ? new Vector3f(base) : new Vector3f();
        double t = animTimeSeconds;
        if (this.x != null) {
            out.x = (float) this.x.evaluate(t);
        }
        if (this.y != null) {
            out.y = (float) this.y.evaluate(t);
        }
        if (this.z != null) {
            out.z = (float) this.z.evaluate(t);
        }
        return out;
    }
}
