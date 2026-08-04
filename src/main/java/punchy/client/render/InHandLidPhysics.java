package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_3532;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandLidPhysics.class */
public final class InHandLidPhysics {
    private static final float FALL_SPEED_MAX = 0.3f;
    private static final float FALL_DISTANCE_MAX = 2.0f;
    private static final float FALL_DISTANCE_FACTOR = 0.75f;
    private static final float LID_RISE_LERP = 0.05f;
    private static final float LID_DROP_LERP = 0.7f;

    private InHandLidPhysics() {
    }

    public static float computeFallTarget(class_1657 player) {
        if (player == null || player.method_24828()) {
            return 0.0f;
        }
        float fallSpeed = class_3532.method_15363((float) (-player.method_18798().field_1351), 0.0f, FALL_SPEED_MAX);
        float speedFactor = fallSpeed / FALL_SPEED_MAX;
        float distanceFactor = class_3532.method_15363(((float) player.field_6017) / FALL_DISTANCE_MAX, 0.0f, 1.0f);
        return class_3532.method_15363(speedFactor + (distanceFactor * FALL_DISTANCE_FACTOR), 0.0f, 1.0f);
    }

    public static float updateLid(EnumMap<class_1268, Float> openMap, EnumMap<class_1268, Long> lastUpdateMap, class_1268 hand, float target) {
        if (hand == null || openMap == null || lastUpdateMap == null) {
            return 0.0f;
        }
        long now = System.nanoTime();
        long last = ((Long) lastUpdateMap.getOrDefault(hand, 0L)).longValue();
        float current = ((Float) openMap.getOrDefault(hand, Float.valueOf(0.0f))).floatValue();
        float next = updateLid(current, target, last, now);
        openMap.put(hand, Float.valueOf(next));
        lastUpdateMap.put(hand, Long.valueOf(now));
        return next;
    }

    public static float updateLid(float current, float target, long lastNanos, long nowNanos) {
        float dt;
        if (lastNanos == 0) {
            dt = 0.05f;
        } else {
            dt = (nowNanos - lastNanos) / 1.0E9f;
        }
        if (dt <= 0.0f) {
            return current;
        }
        if (dt > 0.1f) {
            dt = 0.1f;
        }
        float tickLerp = dt * 20.0f;
        float riseLerp = 1.0f - ((float) Math.pow(0.949999988079071d, tickLerp));
        float dropLerp = 1.0f - ((float) Math.pow(0.30000001192092896d, tickLerp));
        float lerp = target > current ? riseLerp : dropLerp;
        return class_3532.method_16439(lerp, current, target);
    }
}
