package punchy.compat;

import java.lang.reflect.Field;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/SpellEngineCompat.class */
public final class SpellEngineCompat {
    private static final String CLIENT_CLASS = "net.spell_engine.client.SpellEngineClient";
    private static final String CONFIG_FIELD = "config";
    private static final String FIRST_PERSON_FIELD = "firstPersonAnimations";
    private static boolean available = false;
    private static boolean initialized = false;
    private static Object previousFirstPersonMode;

    private SpellEngineCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        available = isClassPresent(CLIENT_CLASS);
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void applyFirstPersonAnimations(boolean enabled) {
        if (!available) {
            return;
        }
        try {
            Class<?> client = Class.forName(CLIENT_CLASS);
            Field configField = client.getField(CONFIG_FIELD);
            Object config = configField.get(null);
            if (config == null) {
                return;
            }
            Field firstPersonField = config.getClass().getField(FIRST_PERSON_FIELD);
            if (enabled) {
                if (previousFirstPersonMode == null) {
                    previousFirstPersonMode = firstPersonField.get(config);
                }
                Object noValue = findEnumValue(firstPersonField.getType(), "NO");
                if (noValue != null) {
                    firstPersonField.set(config, noValue);
                }
            } else if (previousFirstPersonMode != null) {
                firstPersonField.set(config, previousFirstPersonMode);
                previousFirstPersonMode = null;
            }
        } catch (Exception e) {
        }
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object findEnumValue(Class<?> type, String name) {
        Object[] constants;
        if (type == null || name == null || name.isEmpty() || !type.isEnum() || (constants = type.getEnumConstants()) == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant != null && name.equalsIgnoreCase(constant.toString())) {
                return constant;
            }
        }
        return null;
    }
}
