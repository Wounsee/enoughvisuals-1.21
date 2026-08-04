package punchy.client.state;

import net.minecraft.class_1306;
import net.minecraft.class_310;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/MudStateMachine.class */
public final class MudStateMachine {
    private static final float MUD_GAIN_PER_TICK = 0.02f;
    private static float mudAmount = 0.0f;
    private static class_1306 mudArm = class_1306.field_6183;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null || !PunchyConfig.enableMud()) {
            return;
        }
        boolean inWater = client.field_1724.method_5799() || client.field_1724.method_5869();
        boolean miningMud = MiningActionTracker.isMiningActive() && MiningActionTracker.isMiningMud() && !client.field_1724.method_7325();
        if (miningMud) {
            mudArm = client.field_1724.method_6068();
            mudAmount = Math.min(1.0f, mudAmount + MUD_GAIN_PER_TICK);
        } else if (mudAmount > 0.0f && inWater) {
            mudAmount = 0.0f;
        }
    }

    public static int getStageForArm(class_1306 arm) {
        if (arm == null || arm != mudArm || mudAmount <= 0.0f) {
            return 0;
        }
        if (mudAmount < 0.34f) {
            return 1;
        }
        if (mudAmount < 0.67f) {
            return 2;
        }
        return 3;
    }

    public static boolean hasMud() {
        return mudAmount > 0.0f;
    }

    public static float getMudAmount() {
        return mudAmount;
    }
}
