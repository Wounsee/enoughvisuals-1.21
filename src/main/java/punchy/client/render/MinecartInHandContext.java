package punchy.client.render;

import net.minecraft.class_1299;
import net.minecraft.class_1688;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/MinecartInHandContext.class */
public final class MinecartInHandContext {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

    private MinecartInHandContext() {
    }

    public static void push() {
        STATE.get().depth++;
    }

    public static void pop() {
        State state = STATE.get();
        state.depth = Math.max(0, state.depth - 1);
        if (state.depth == 0) {
            state.vy = 0.0d;
            state.tntOffset = 0.0f;
            state.type = null;
        }
    }

    public static boolean isActive() {
        return STATE.get().depth > 0;
    }

    public static void setVy(double vy) {
        STATE.get().vy = vy;
    }

    public static double getVy() {
        return STATE.get().vy;
    }

    public static void setTntOffset(float offset) {
        STATE.get().tntOffset = offset;
    }

    public static float getTntOffset() {
        return STATE.get().tntOffset;
    }

    public static void setType(class_1299<? extends class_1688> type) {
        STATE.get().type = type;
    }

    public static class_1299<? extends class_1688> getType() {
        return STATE.get().type;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/MinecartInHandContext$State.class */
    private static final class State {
        int depth;
        double vy;
        float tntOffset;
        class_1299<? extends class_1688> type;

        private State() {
        }
    }
}
