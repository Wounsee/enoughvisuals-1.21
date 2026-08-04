package punchy.client.render;

import java.util.EnumMap;
import net.minecraft.class_1268;
import net.minecraft.class_3532;
import net.minecraft.class_746;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/InHandFallPhysics.class */
public final class InHandFallPhysics {
    private static final float FLOAT_MAX = 0.65f;
    private static final float RISE_LERP = 0.1f;
    private static final float FALL_LERP = 0.12f;
    private static final EnumMap<class_1268, Float> OFFSETS = new EnumMap<>(class_1268.class);

    private InHandFallPhysics() {
    }

    public static float update(class_746 player, class_1268 hand) {
        if (player == null || hand == null) {
            return 0.0f;
        }
        float current = ((Float) OFFSETS.getOrDefault(hand, Float.valueOf(0.0f))).floatValue();
        float target = 0.0f;
        if (!player.method_24828()) {
            float fallSpeed = class_3532.method_15363((float) (-player.method_18798().field_1351), 0.0f, 0.3f);
            float speedFactor = fallSpeed / 0.3f;
            float distanceFactor = class_3532.method_15363(((float) player.field_6017) / 2.0f, 0.0f, 1.0f);
            target = FLOAT_MAX * class_3532.method_15363(speedFactor + (distanceFactor * 0.75f), 0.0f, 1.0f);
        }
        float lerp = target > current ? RISE_LERP : FALL_LERP;
        float next = class_3532.method_16439(lerp, current, target);
        OFFSETS.put(hand, Float.valueOf(next));
        return next;
    }

    public static void reset(class_1268 hand) {
        if (hand == null) {
            return;
        }
        OFFSETS.put(hand, Float.valueOf(0.0f));
    }
}
