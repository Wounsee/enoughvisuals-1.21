package punchy.client.debug;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/debug/PunchyDebug.class */
public class PunchyDebug {
    public static boolean DEBUG_POSITION_ENABLED = false;
    public static float POS_X = 0.0f;
    public static float POS_Y = 0.0f;
    public static float POS_Z = 0.0f;
    public static float ROT_X = 0.0f;
    public static float ROT_Y = 0.0f;
    public static float ROT_Z = 0.0f;
    public static float SCALE = 1.0f;
    public static float ARM_R_POS_X = 0.0f;
    public static float ARM_R_POS_Y = 0.0f;
    public static float ARM_R_POS_Z = 0.0f;
    public static float ARM_R_ROT_X = 0.0f;
    public static float ARM_R_ROT_Y = 0.0f;
    public static float ARM_R_ROT_Z = 0.0f;
    public static float ARM_L_POS_X = 0.0f;
    public static float ARM_L_POS_Y = 0.0f;
    public static float ARM_L_POS_Z = 0.0f;
    public static float ARM_L_ROT_X = 0.0f;
    public static float ARM_L_ROT_Y = 0.0f;
    public static float ARM_L_ROT_Z = 0.0f;
    public static float ARM_L_SCALE_X = 1.0f;
    public static float ARM_L_SCALE_Y = 1.0f;
    public static float ARM_L_SCALE_Z = 1.0f;
    public static float ARM_R_SCALE_X = 1.0f;
    public static float ARM_R_SCALE_Y = 1.0f;
    public static float ARM_R_SCALE_Z = 1.0f;
    public static float ITEM_R_POS_X = 0.0f;
    public static float ITEM_R_POS_Y = 0.0f;
    public static float ITEM_R_POS_Z = 0.0f;
    public static float ITEM_R_ROT_X = 0.0f;
    public static float ITEM_R_ROT_Y = 0.0f;
    public static float ITEM_R_ROT_Z = 0.0f;
    public static float ITEM_R_SCALE_X = 1.0f;
    public static float ITEM_R_SCALE_Y = 1.0f;
    public static float ITEM_R_SCALE_Z = 1.0f;
    public static float ITEM_L_POS_X = 0.0f;
    public static float ITEM_L_POS_Y = 0.0f;
    public static float ITEM_L_POS_Z = 0.0f;
    public static float ITEM_L_ROT_X = 0.0f;
    public static float ITEM_L_ROT_Y = 0.0f;
    public static float ITEM_L_ROT_Z = 0.0f;
    public static float ITEM_L_SCALE_X = 1.0f;
    public static float ITEM_L_SCALE_Y = 1.0f;
    public static float ITEM_L_SCALE_Z = 1.0f;
    public static float ITEM_R_PIVOT_X = 0.0f;
    public static float ITEM_R_PIVOT_Y = 0.0f;
    public static float ITEM_R_PIVOT_Z = 0.0f;
    public static float ITEM_L_PIVOT_X = 0.0f;
    public static float ITEM_L_PIVOT_Y = 0.0f;
    public static float ITEM_L_PIVOT_Z = 0.0f;
    public static boolean SHOW_GRIP_PIVOT = false;

    public static void setDebugPositionEnabled(boolean enabled) {
        DEBUG_POSITION_ENABLED = enabled;
        if (!enabled) {
            SHOW_GRIP_PIVOT = false;
        }
    }

    public static boolean toggleDebugPosition() {
        setDebugPositionEnabled(!DEBUG_POSITION_ENABLED);
        return DEBUG_POSITION_ENABLED;
    }
}
