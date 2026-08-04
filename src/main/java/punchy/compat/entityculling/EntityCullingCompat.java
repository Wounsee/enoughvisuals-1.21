package punchy.compat.entityculling;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/entityculling/EntityCullingCompat.class */
public final class EntityCullingCompat {
    private static final String MOD_ID = "entityculling";
    private static final String MOD_BASE = "dev.tr7zw.entityculling.EntityCullingModBase";
    private static volatile boolean initialized;
    private static volatile boolean applied;
    private static volatile boolean loaded;
    private static volatile Field configField;
    private static volatile Field frustumField;

    private EntityCullingCompat() {
    }

    public static void tick() {
        if (applied) {
            return;
        }
        if (!ensureLoaded()) {
            applied = true;
        } else {
            tryWhitelistEnchantingTable();
        }
    }

    public static <T> T withBlockEntityFrustumCullingDisabled(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        if (!ensureLoaded()) {
            return supplier.get();
        }
        try {
            Object config = resolveConfig();
            if (config == null) {
                return supplier.get();
            }
            Field field = resolveFrustumField(config);
            if (field == null) {
                return supplier.get();
            }
            boolean prev = field.getBoolean(config);
            if (!prev) {
                return supplier.get();
            }
            field.setBoolean(config, false);
            try {
                T t = supplier.get();
                field.setBoolean(config, prev);
                return t;
            } catch (Throwable th) {
                field.setBoolean(config, prev);
                throw th;
            }
        } catch (Throwable th2) {
            return supplier.get();
        }
    }

    private static void tryWhitelistEnchantingTable() {
        try {
            Class<?> base = Class.forName(MOD_BASE);
            Object instance = base.getField("instance").get(null);
            if (instance == null) {
                return;
            }
            Method addWhitelist = base.getMethod("addDynamicBlockEntityWhitelist", Function.class);
            Function<Object, Boolean> whitelist = blockEntity -> {
                if (blockEntity == null) {
                    return false;
                }
                String name = blockEntity.getClass().getName();
                return Boolean.valueOf(name.endsWith("EnchantmentTableBlockEntity") || name.endsWith("EnchantingTableBlockEntity"));
            };
            addWhitelist.invoke(instance, whitelist);
            applied = true;
        } catch (Throwable th) {
        }
    }

    private static boolean ensureLoaded() {
        if (!initialized) {
            initialized = true;
            loaded = Services.PLATFORM.isModLoaded(MOD_ID);
        }
        return loaded;
    }

    private static Object resolveConfig() throws ReflectiveOperationException {
        Class<?> base = Class.forName(MOD_BASE);
        Object instance = base.getField("instance").get(null);
        if (instance == null) {
            return null;
        }
        Field field = configField;
        if (field == null) {
            field = base.getSuperclass().getField("config");
            configField = field;
        }
        return field.get(instance);
    }

    private static Field resolveFrustumField(Object config) throws ReflectiveOperationException {
        if (config == null) {
            return null;
        }
        Field field = frustumField;
        if (field == null) {
            field = config.getClass().getField("blockEntityFrustumCulling");
            frustumField = field;
        }
        return field;
    }
}
