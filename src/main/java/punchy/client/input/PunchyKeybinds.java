package punchy.client.input;

import java.util.Locale;
import net.minecraft.class_1657;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_5250;
import net.minecraft.class_746;
import punchy.client.debug.PunchyDebug;
import punchy.client.debug.PunchyTuningIO;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/input/PunchyKeybinds.class */
public class PunchyKeybinds {
    private static final String CATEGORY = "key.categories.punchy.tuning";
    private static class_304 axisXKey;
    private static class_304 axisYKey;
    private static class_304 axisZKey;
    private static class_304 toggleModeKey;
    private static class_304 targetArmRightKey;
    private static class_304 targetArmLeftKey;
    private static class_304 targetItemRightKey;
    private static class_304 targetItemLeftKey;
    private static class_304 increaseKey;
    private static class_304 decreaseKey;
    private static class_304 resetKey;
    private static class_304 printKey;
    private static class_304 saveKey;
    private static class_304 togglePivotKey;
    private static Mode mode = Mode.TRANSLATE;
    private static Axis axis = Axis.X;
    private static Target target = Target.ARM_RIGHT;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/input/PunchyKeybinds$Axis.class */
    private enum Axis {
        X,
        Y,
        Z
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/input/PunchyKeybinds$Mode.class */
    private enum Mode {
        TRANSLATE,
        ROTATE,
        SCALE,
        PIVOT
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/input/PunchyKeybinds$Target.class */
    private enum Target {
        ARM_RIGHT,
        ARM_LEFT,
        ITEM_RIGHT,
        ITEM_LEFT
    }

    public static void register() {
        axisXKey = new class_304("key.punchy.tuning.axis_x", 324, CATEGORY);
        axisYKey = new class_304("key.punchy.tuning.axis_y", 325, CATEGORY);
        axisZKey = new class_304("key.punchy.tuning.axis_z", 326, CATEGORY);
        toggleModeKey = new class_304("key.punchy.tuning.toggle_mode", 261, CATEGORY);
        targetArmRightKey = new class_304("key.punchy.tuning.target_arm_right", 327, CATEGORY);
        targetArmLeftKey = new class_304("key.punchy.tuning.target_arm_left", 328, CATEGORY);
        targetItemRightKey = new class_304("key.punchy.tuning.target_item_right", 329, CATEGORY);
        targetItemLeftKey = new class_304("key.punchy.tuning.target_item_left", 320, CATEGORY);
        increaseKey = new class_304("key.punchy.tuning.increase", 265, CATEGORY);
        decreaseKey = new class_304("key.punchy.tuning.decrease", 264, CATEGORY);
        resetKey = new class_304("key.punchy.tuning.reset", 82, CATEGORY);
        printKey = new class_304("key.punchy.tuning.print", 335, CATEGORY);
        saveKey = new class_304("key.punchy.tuning.save_session", 295, CATEGORY);
        togglePivotKey = new class_304("key.punchy.tuning.toggle_pivot", 296, CATEGORY);
        Services.CLIENT.registerKeyMapping(axisXKey);
        Services.CLIENT.registerKeyMapping(axisYKey);
        Services.CLIENT.registerKeyMapping(axisZKey);
        Services.CLIENT.registerKeyMapping(toggleModeKey);
        Services.CLIENT.registerKeyMapping(targetArmRightKey);
        Services.CLIENT.registerKeyMapping(targetArmLeftKey);
        Services.CLIENT.registerKeyMapping(targetItemRightKey);
        Services.CLIENT.registerKeyMapping(targetItemLeftKey);
        Services.CLIENT.registerKeyMapping(increaseKey);
        Services.CLIENT.registerKeyMapping(decreaseKey);
        Services.CLIENT.registerKeyMapping(resetKey);
        Services.CLIENT.registerKeyMapping(printKey);
        Services.CLIENT.registerKeyMapping(saveKey);
        Services.CLIENT.registerKeyMapping(togglePivotKey);
        Services.CLIENT.registerClientTickEnd(PunchyKeybinds::handleTick);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void handleTick(class_310 client) throws MatchException {
        class_5250 class_5250VarMethod_43471;
        Mode mode2;
        if (client == null || client.field_1724 == null || !PunchyDebug.DEBUG_POSITION_ENABLED || client.field_1755 != null) {
            return;
        }
        class_746 class_746Var = client.field_1724;
        while (axisXKey.method_1436()) {
            axis = Axis.X;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.axis", new Object[]{axisLabel(axis)}));
        }
        while (axisYKey.method_1436()) {
            axis = Axis.Y;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.axis", new Object[]{axisLabel(axis)}));
        }
        while (axisZKey.method_1436()) {
            axis = Axis.Z;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.axis", new Object[]{axisLabel(axis)}));
        }
        while (toggleModeKey.method_1436()) {
            switch (mode) {
                case TRANSLATE:
                    mode2 = Mode.ROTATE;
                    break;
                case ROTATE:
                    mode2 = Mode.SCALE;
                    break;
                case SCALE:
                    mode2 = Mode.PIVOT;
                    break;
                case PIVOT:
                    mode2 = Mode.TRANSLATE;
                    break;
                default:
                    throw new MatchException((String) null, (Throwable) null);
            }
            mode = mode2;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.mode", new Object[]{modeLabel(mode)}));
        }
        while (targetArmRightKey.method_1436()) {
            target = Target.ARM_RIGHT;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.target", new Object[]{targetLabel(target)}));
        }
        while (targetArmLeftKey.method_1436()) {
            target = Target.ARM_LEFT;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.target", new Object[]{targetLabel(target)}));
        }
        while (targetItemRightKey.method_1436()) {
            target = Target.ITEM_RIGHT;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.target", new Object[]{targetLabel(target)}));
        }
        while (targetItemLeftKey.method_1436()) {
            target = Target.ITEM_LEFT;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.target", new Object[]{targetLabel(target)}));
        }
        while (resetKey.method_1436()) {
            resetTarget();
            toast(class_746Var, formatOffset());
        }
        boolean inc = increaseKey.method_1434() && !decreaseKey.method_1434();
        boolean dec = decreaseKey.method_1434() && !increaseKey.method_1434();
        if (inc || dec) {
            float delta = computeStep(client, mode);
            if (dec) {
                delta *= -1.0f;
            }
            applyDelta(delta, mode);
            toast(class_746Var, formatOffset());
        }
        while (printKey.method_1436()) {
            class_746Var.method_7353(formatOffset(), false);
        }
        while (saveKey.method_1436()) {
            PunchyTuningIO.saveSession();
            class_746Var.method_7353(class_2561.method_43469("punchy.tuning.toast.saved_session", new Object[]{PunchyTuningIO.getSessionPath()}), false);
        }
        while (togglePivotKey.method_1436()) {
            PunchyDebug.SHOW_GRIP_PIVOT = !PunchyDebug.SHOW_GRIP_PIVOT;
            Object[] objArr = new Object[1];
            if (PunchyDebug.SHOW_GRIP_PIVOT) {
                class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.on");
            } else {
                class_5250VarMethod_43471 = class_2561.method_43471("punchy.toggle.off");
            }
            objArr[0] = class_5250VarMethod_43471;
            toast(class_746Var, class_2561.method_43469("punchy.tuning.toast.grip_pivot", objArr));
        }
    }

    private static float computeStep(class_310 client, Mode mode2) {
        float base = mode2 == Mode.ROTATE ? 1.0f : 0.05f;
        if (mode2 == Mode.SCALE || mode2 == Mode.PIVOT) {
            base = 0.05f;
        }
        if (client.field_1690 != null) {
            if (client.field_1690.field_1867.method_1434()) {
                base *= 5.0f;
            }
            if (client.field_1690.field_1832.method_1434()) {
                base *= 0.2f;
            }
        }
        return base;
    }

    private static void applyDelta(float delta, Mode mode2) {
        if (mode2 == Mode.SCALE) {
            addScale(delta);
        }
        if (mode2 == Mode.PIVOT) {
            addPivot(delta);
            return;
        }
        switch (axis) {
            case X:
                if (mode2 != Mode.ROTATE) {
                    addPosX(delta);
                } else {
                    addRotX(delta);
                }
                break;
            case Y:
                if (mode2 != Mode.ROTATE) {
                    addPosY(delta);
                } else {
                    addRotY(delta);
                }
                break;
            case Z:
                if (mode2 != Mode.ROTATE) {
                    addPosZ(delta);
                } else {
                    addRotZ(delta);
                }
                break;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 formatOffset() throws MatchException {
        float posX = getPosX();
        float posY = getPosY();
        float posZ = getPosZ();
        float rotX = getRotX();
        float rotY = getRotY();
        float rotZ = getRotZ();
        float scale = getScale();
        float pivotX = getPivotX();
        float pivotY = getPivotY();
        float pivotZ = getPivotZ();
        String posXStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(posX));
        String posYStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(posY));
        String posZStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(posZ));
        String rotXStr = String.format(Locale.ROOT, "%.1f", Float.valueOf(rotX));
        String rotYStr = String.format(Locale.ROOT, "%.1f", Float.valueOf(rotY));
        String rotZStr = String.format(Locale.ROOT, "%.1f", Float.valueOf(rotZ));
        String scaleStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(scale));
        String pivotXStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(pivotX));
        String pivotYStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(pivotY));
        String pivotZStr = String.format(Locale.ROOT, "%.3f", Float.valueOf(pivotZ));
        return class_2561.method_43469("punchy.tuning.offset_status", new Object[]{targetLabel(target), modeLabel(mode), axisLabel(axis), posXStr, posYStr, posZStr, rotXStr, rotYStr, rotZStr, scaleStr, pivotXStr, pivotYStr, pivotZStr});
    }

    private static void resetTarget() {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_POS_X = 0.0f;
                PunchyDebug.ARM_R_POS_Y = 0.0f;
                PunchyDebug.ARM_R_POS_Z = 0.0f;
                PunchyDebug.ARM_R_ROT_X = 0.0f;
                PunchyDebug.ARM_R_ROT_Y = 0.0f;
                PunchyDebug.ARM_R_ROT_Z = 0.0f;
                PunchyDebug.ARM_R_SCALE_X = 1.0f;
                PunchyDebug.ARM_R_SCALE_Y = 1.0f;
                PunchyDebug.ARM_R_SCALE_Z = 1.0f;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_POS_X = 0.0f;
                PunchyDebug.ARM_L_POS_Y = 0.0f;
                PunchyDebug.ARM_L_POS_Z = 0.0f;
                PunchyDebug.ARM_L_ROT_X = 0.0f;
                PunchyDebug.ARM_L_ROT_Y = 0.0f;
                PunchyDebug.ARM_L_ROT_Z = 0.0f;
                PunchyDebug.ARM_L_SCALE_X = 1.0f;
                PunchyDebug.ARM_L_SCALE_Y = 1.0f;
                PunchyDebug.ARM_L_SCALE_Z = 1.0f;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_POS_X = 0.0f;
                PunchyDebug.ITEM_R_POS_Y = 0.0f;
                PunchyDebug.ITEM_R_POS_Z = 0.0f;
                PunchyDebug.ITEM_R_ROT_X = 0.0f;
                PunchyDebug.ITEM_R_ROT_Y = 0.0f;
                PunchyDebug.ITEM_R_ROT_Z = 0.0f;
                PunchyDebug.ITEM_R_SCALE_X = 1.0f;
                PunchyDebug.ITEM_R_SCALE_Y = 1.0f;
                PunchyDebug.ITEM_R_SCALE_Z = 1.0f;
                PunchyDebug.ITEM_R_PIVOT_X = 0.0f;
                PunchyDebug.ITEM_R_PIVOT_Y = 0.0f;
                PunchyDebug.ITEM_R_PIVOT_Z = 0.0f;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_POS_X = 0.0f;
                PunchyDebug.ITEM_L_POS_Y = 0.0f;
                PunchyDebug.ITEM_L_POS_Z = 0.0f;
                PunchyDebug.ITEM_L_ROT_X = 0.0f;
                PunchyDebug.ITEM_L_ROT_Y = 0.0f;
                PunchyDebug.ITEM_L_ROT_Z = 0.0f;
                PunchyDebug.ITEM_L_SCALE_X = 1.0f;
                PunchyDebug.ITEM_L_SCALE_Y = 1.0f;
                PunchyDebug.ITEM_L_SCALE_Z = 1.0f;
                PunchyDebug.ITEM_L_PIVOT_X = 0.0f;
                PunchyDebug.ITEM_L_PIVOT_Y = 0.0f;
                PunchyDebug.ITEM_L_PIVOT_Z = 0.0f;
                break;
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getPosX() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_POS_X;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_POS_X;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_POS_X;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_POS_X;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getPosY() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_POS_Y;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_POS_Y;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_POS_Y;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_POS_Y;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getPosZ() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_POS_Z;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_POS_Z;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_POS_Z;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_POS_Z;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getRotX() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_ROT_X;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_ROT_X;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_ROT_X;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_ROT_X;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getRotY() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_ROT_Y;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_ROT_Y;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_ROT_Y;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_ROT_Y;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getRotZ() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_ROT_Z;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_ROT_Z;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_ROT_Z;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_ROT_Z;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static float getScale() throws MatchException {
        switch (target) {
            case ARM_RIGHT:
                return PunchyDebug.ARM_R_SCALE_X;
            case ARM_LEFT:
                return PunchyDebug.ARM_L_SCALE_X;
            case ITEM_RIGHT:
                return PunchyDebug.ITEM_R_SCALE_X;
            case ITEM_LEFT:
                return PunchyDebug.ITEM_L_SCALE_X;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private static float getPivotX() {
        switch (target.ordinal()) {
            case 2:
                return PunchyDebug.ITEM_R_PIVOT_X;
            case 3:
                return PunchyDebug.ITEM_L_PIVOT_X;
            default:
                return 0.0f;
        }
    }

    private static float getPivotY() {
        switch (target.ordinal()) {
            case 2:
                return PunchyDebug.ITEM_R_PIVOT_Y;
            case 3:
                return PunchyDebug.ITEM_L_PIVOT_Y;
            default:
                return 0.0f;
        }
    }

    private static float getPivotZ() {
        switch (target.ordinal()) {
            case 2:
                return PunchyDebug.ITEM_R_PIVOT_Z;
            case 3:
                return PunchyDebug.ITEM_L_PIVOT_Z;
            default:
                return 0.0f;
        }
    }

    private static void addPosX(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_POS_X += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_POS_X += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_POS_X += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_POS_X += delta;
                break;
        }
    }

    private static void addPosY(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_POS_Y += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_POS_Y += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_POS_Y += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_POS_Y += delta;
                break;
        }
    }

    private static void addPosZ(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_POS_Z += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_POS_Z += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_POS_Z += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_POS_Z += delta;
                break;
        }
    }

    private static void addRotX(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_ROT_X += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_ROT_X += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_ROT_X += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_ROT_X += delta;
                break;
        }
    }

    private static void addRotY(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_ROT_Y += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_ROT_Y += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_ROT_Y += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_ROT_Y += delta;
                break;
        }
    }

    private static void addRotZ(float delta) {
        switch (target) {
            case ARM_RIGHT:
                PunchyDebug.ARM_R_ROT_Z += delta;
                break;
            case ARM_LEFT:
                PunchyDebug.ARM_L_ROT_Z += delta;
                break;
            case ITEM_RIGHT:
                PunchyDebug.ITEM_R_ROT_Z += delta;
                break;
            case ITEM_LEFT:
                PunchyDebug.ITEM_L_ROT_Z += delta;
                break;
        }
    }

    private static void addScale(float delta) {
        switch (target) {
            case ARM_RIGHT:
                float next = clampScale(PunchyDebug.ARM_R_SCALE_X + delta);
                PunchyDebug.ARM_R_SCALE_X = next;
                PunchyDebug.ARM_R_SCALE_Y = next;
                PunchyDebug.ARM_R_SCALE_Z = next;
                break;
            case ARM_LEFT:
                float next2 = clampScale(PunchyDebug.ARM_L_SCALE_X + delta);
                PunchyDebug.ARM_L_SCALE_X = next2;
                PunchyDebug.ARM_L_SCALE_Y = next2;
                PunchyDebug.ARM_L_SCALE_Z = next2;
                break;
            case ITEM_RIGHT:
                float next3 = clampScale(PunchyDebug.ITEM_R_SCALE_X + delta);
                PunchyDebug.ITEM_R_SCALE_X = next3;
                PunchyDebug.ITEM_R_SCALE_Y = next3;
                PunchyDebug.ITEM_R_SCALE_Z = next3;
                break;
            case ITEM_LEFT:
                float next4 = clampScale(PunchyDebug.ITEM_L_SCALE_X + delta);
                PunchyDebug.ITEM_L_SCALE_X = next4;
                PunchyDebug.ITEM_L_SCALE_Y = next4;
                PunchyDebug.ITEM_L_SCALE_Z = next4;
                break;
        }
    }

    private static void addPivot(float delta) {
        switch (axis) {
            case X:
                addPivotX(delta);
                break;
            case Y:
                addPivotY(delta);
                break;
            case Z:
                addPivotZ(delta);
                break;
        }
    }

    private static void addPivotX(float delta) {
        switch (target.ordinal()) {
            case 2:
                PunchyDebug.ITEM_R_PIVOT_X += delta;
                break;
            case 3:
                PunchyDebug.ITEM_L_PIVOT_X += delta;
                break;
        }
    }

    private static void addPivotY(float delta) {
        switch (target.ordinal()) {
            case 2:
                PunchyDebug.ITEM_R_PIVOT_Y += delta;
                break;
            case 3:
                PunchyDebug.ITEM_L_PIVOT_Y += delta;
                break;
        }
    }

    private static void addPivotZ(float delta) {
        switch (target.ordinal()) {
            case 2:
                PunchyDebug.ITEM_R_PIVOT_Z += delta;
                break;
            case 3:
                PunchyDebug.ITEM_L_PIVOT_Z += delta;
                break;
        }
    }

    private static float clampScale(float value) {
        return Math.max(0.01f, value);
    }

    private static void toast(class_1657 player, class_2561 message) {
        player.method_7353(message, true);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 axisLabel(Axis axis2) throws MatchException {
        switch (axis2) {
            case X:
                return class_2561.method_43471("punchy.tuning.axis.x");
            case Y:
                return class_2561.method_43471("punchy.tuning.axis.y");
            case Z:
                return class_2561.method_43471("punchy.tuning.axis.z");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 modeLabel(Mode mode2) throws MatchException {
        switch (mode2) {
            case TRANSLATE:
                return class_2561.method_43471("punchy.tuning.mode.translate");
            case ROTATE:
                return class_2561.method_43471("punchy.tuning.mode.rotate");
            case SCALE:
                return class_2561.method_43471("punchy.tuning.mode.scale");
            case PIVOT:
                return class_2561.method_43471("punchy.tuning.mode.pivot");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static class_2561 targetLabel(Target target2) throws MatchException {
        switch (target2) {
            case ARM_RIGHT:
                return class_2561.method_43471("punchy.tuning.target.arm_right");
            case ARM_LEFT:
                return class_2561.method_43471("punchy.tuning.target.arm_left");
            case ITEM_RIGHT:
                return class_2561.method_43471("punchy.tuning.target.item_right");
            case ITEM_LEFT:
                return class_2561.method_43471("punchy.tuning.target.item_left");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}
