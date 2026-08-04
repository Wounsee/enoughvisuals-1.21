package punchy.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import punchy.client.state.UseItemStateMachine;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/SwordBlockingMechanicsCompat.class */
public final class SwordBlockingMechanicsCompat {
    private static final String EVENT_RESULT_CLASS = "fuzs.puzzleslib.api.event.v1.core.EventResult";
    private static Method isActiveItemStackBlocking;
    private static Method getParryStrengthScale;
    private static Object eventResultPass;
    private static class_1268 callbackBlockingHand;
    private static final String[] HANDLER_CLASSES = {"fuzs.swordblockingmechanics.handler.SwordBlockingHandler", "fuzs.swordblockingmechanics.common.handler.SwordBlockingHandler"};
    private static boolean available = false;
    private static boolean initialized = false;

    private SwordBlockingMechanicsCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        Class<?> handler = findClass(HANDLER_CLASSES);
        available = handler != null;
        if (handler == null) {
            return;
        }
        try {
            isActiveItemStackBlocking = findMethod(handler, "isActiveItemStackBlocking", 1);
            getParryStrengthScale = findMethod(handler, "getParryStrengthScale", 1);
        } catch (Exception e) {
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean shouldUseBlockingAnimation(class_1268 hand) {
        return hand != null && callbackBlockingHand == hand;
    }

    public static void recordBlockingStart(class_1657 player, class_1268 hand) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || player != client.field_1724 || hand == null) {
            return;
        }
        callbackBlockingHand = hand;
        UseItemStateMachine.requestSwordBlockingCompat(hand);
    }

    public static void recordBlockingStop(class_1657 player, class_1268 hand) {
        class_310 client;
        if (player != null && hand != null && (client = class_310.method_1551()) != null && client.field_1724 != null && player == client.field_1724 && callbackBlockingHand == hand) {
            callbackBlockingHand = null;
        }
    }

    public static boolean isBlocking(class_1309 livingEntity) {
        if (!available || livingEntity == null || isActiveItemStackBlocking == null) {
            return false;
        }
        try {
            Object result = isActiveItemStackBlocking.invoke(null, livingEntity);
            if (result instanceof Boolean) {
                Boolean value = (Boolean) result;
                if (value.booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static double getParryStrengthScale(class_1309 livingEntity) {
        if (!available || livingEntity == null || getParryStrengthScale == null) {
            return 1.0d;
        }
        try {
            Object result = getParryStrengthScale.invoke(null, livingEntity);
            if (result instanceof Double) {
                Double value = (Double) result;
                return value.doubleValue();
            }
            if (result instanceof Float) {
                Float value2 = (Float) result;
                return value2.doubleValue();
            }
            return 1.0d;
        } catch (Exception e) {
            return 1.0d;
        }
    }

    public static Object getEventResultPass() {
        if (!available) {
            return null;
        }
        if (eventResultPass != null) {
            return eventResultPass;
        }
        try {
            Class<?> result = Class.forName(EVENT_RESULT_CLASS);
            Field passField = result.getField("PASS");
            eventResultPass = passField.get(null);
            return eventResultPass;
        } catch (Exception e) {
            return null;
        }
    }

    private static Class<?> findClass(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        if (type == null || name == null) {
            return null;
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        return null;
    }
}
