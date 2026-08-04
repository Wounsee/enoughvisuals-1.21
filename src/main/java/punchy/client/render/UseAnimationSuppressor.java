package punchy.client.render;

import net.minecraft.class_1268;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/UseAnimationSuppressor.class */
public final class UseAnimationSuppressor {
    private static boolean mainHandSuppressed;
    private static boolean offHandSuppressed;
    private static boolean forced;
    private static boolean mainHandPendingReset;
    private static boolean offHandPendingReset;
    private static long mainHandReleaseStartTick = -1;
    private static long offHandReleaseStartTick = -1;
    private static int mainHandReleaseStartRemainingTicks = -1;
    private static int offHandReleaseStartRemainingTicks = -1;
    private static int mainHandMeshStartRemainingTicks = -1;
    private static int offHandMeshStartRemainingTicks = -1;

    private UseAnimationSuppressor() {
    }

    public static void suppress() {
        suppress(class_1268.field_5808);
        suppress(class_1268.field_5810);
    }

    public static void suppress(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offHandSuppressed = true;
            offHandPendingReset = true;
            offHandReleaseStartTick = -1L;
            offHandReleaseStartRemainingTicks = -1;
            return;
        }
        mainHandSuppressed = true;
        mainHandPendingReset = true;
        mainHandReleaseStartTick = -1L;
        mainHandReleaseStartRemainingTicks = -1;
    }

    public static void release(long gameTime, int remainingTicks) {
        release(class_1268.field_5808, gameTime, remainingTicks);
        release(class_1268.field_5810, gameTime, remainingTicks);
    }

    public static void release(class_1268 hand, long gameTime, int remainingTicks) {
        if (hand == class_1268.field_5810) {
            offHandSuppressed = false;
            if (offHandPendingReset) {
                offHandReleaseStartTick = gameTime;
                offHandReleaseStartRemainingTicks = remainingTicks;
                offHandPendingReset = false;
                return;
            }
            return;
        }
        mainHandSuppressed = false;
        if (mainHandPendingReset) {
            mainHandReleaseStartTick = gameTime;
            mainHandReleaseStartRemainingTicks = remainingTicks;
            mainHandPendingReset = false;
        }
    }

    public static void clear() {
        clear(class_1268.field_5808);
        clear(class_1268.field_5810);
    }

    public static void clear(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offHandSuppressed = false;
            offHandPendingReset = false;
            offHandReleaseStartTick = -1L;
            offHandReleaseStartRemainingTicks = -1;
            offHandMeshStartRemainingTicks = -1;
            return;
        }
        mainHandSuppressed = false;
        mainHandPendingReset = false;
        mainHandReleaseStartTick = -1L;
        mainHandReleaseStartRemainingTicks = -1;
        mainHandMeshStartRemainingTicks = -1;
    }

    public static boolean isSuppressed() {
        return mainHandSuppressed || offHandSuppressed;
    }

    public static boolean isSuppressed(class_1268 hand) {
        return hand == class_1268.field_5810 ? offHandSuppressed : mainHandSuppressed;
    }

    public static void force(boolean value) {
        forced = value;
    }

    public static boolean isForced() {
        return forced;
    }

    public static long getReleaseStartTick() {
        return mainHandReleaseStartTick >= 0 ? mainHandReleaseStartTick : offHandReleaseStartTick;
    }

    public static long getReleaseStartTick(class_1268 hand) {
        return hand == class_1268.field_5810 ? offHandReleaseStartTick : mainHandReleaseStartTick;
    }

    public static int getReleaseStartRemainingTicks() {
        return mainHandReleaseStartRemainingTicks >= 0 ? mainHandReleaseStartRemainingTicks : offHandReleaseStartRemainingTicks;
    }

    public static int getReleaseStartRemainingTicks(class_1268 hand) {
        return hand == class_1268.field_5810 ? offHandReleaseStartRemainingTicks : mainHandReleaseStartRemainingTicks;
    }

    public static void clearReleaseStart() {
        clearReleaseStart(class_1268.field_5808);
        clearReleaseStart(class_1268.field_5810);
    }

    public static void clearReleaseStart(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offHandReleaseStartTick = -1L;
            offHandReleaseStartRemainingTicks = -1;
        } else {
            mainHandReleaseStartTick = -1L;
            mainHandReleaseStartRemainingTicks = -1;
        }
    }

    public static void recordMeshStart(int remainingTicks) {
        recordMeshStart(class_1268.field_5808, remainingTicks);
        recordMeshStart(class_1268.field_5810, remainingTicks);
    }

    public static void recordMeshStart(class_1268 hand, int remainingTicks) {
        if (remainingTicks < 0) {
            return;
        }
        if (hand == class_1268.field_5810) {
            if (offHandMeshStartRemainingTicks < 0) {
                offHandMeshStartRemainingTicks = remainingTicks;
            }
        } else if (mainHandMeshStartRemainingTicks < 0) {
            mainHandMeshStartRemainingTicks = remainingTicks;
        }
    }

    public static int getMeshStartRemainingTicks() {
        return mainHandMeshStartRemainingTicks >= 0 ? mainHandMeshStartRemainingTicks : offHandMeshStartRemainingTicks;
    }

    public static int getMeshStartRemainingTicks(class_1268 hand) {
        return hand == class_1268.field_5810 ? offHandMeshStartRemainingTicks : mainHandMeshStartRemainingTicks;
    }

    public static void clearMeshStart() {
        clearMeshStart(class_1268.field_5808);
        clearMeshStart(class_1268.field_5810);
    }

    public static void clearMeshStart(class_1268 hand) {
        if (hand == class_1268.field_5810) {
            offHandMeshStartRemainingTicks = -1;
        } else {
            mainHandMeshStartRemainingTicks = -1;
        }
    }
}
