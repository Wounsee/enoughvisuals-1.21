package fun.enoughvisuals.systems.cosmetic.pulse;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reflection boundary around the embedded intermediary-mapped Pulse runtime. */
public final class PulseCosmeticBridge {
    public record Item(int id, String name, String category) {}

    private static final String CATALOG = "dev.pulse.cosmetics.CosmeticCatalog";
    private static final String CATEGORY = "dev.pulse.cosmetics.CosmeticCategory";
    private static final String SELECTION = "dev.pulse.cosmetics.CosmeticSelection";

    private PulseCosmeticBridge() {}

    public static boolean available() {
        try {
            Class.forName(CATALOG, false, PulseCosmeticBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static List<Item> items(String categoryName) {
        if (!available()) return List.of();
        try {
            Object category = category(categoryName);
            Object value = Class.forName(CATALOG).getMethod("byCategory", category.getClass()).invoke(null, category);
            List<Item> result = new ArrayList<>();
            for (Object entry : (List<?>) value) {
                result.add(new Item((int) call(entry, "id"), (String) call(entry, "name"), categoryName));
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException exception) {
            throw failure("read catalog", exception);
        }
    }

    public static int selected(String categoryName) {
        if (!available()) return -1;
        try {
            Object category = category(categoryName);
            return (int) Class.forName(SELECTION).getMethod("get", category.getClass()).invoke(null, category);
        } catch (ReflectiveOperationException exception) {
            throw failure("read selection", exception);
        }
    }

    public static void select(String categoryName, int id) {
        mutate(categoryName, id);
    }

    public static void clear(String categoryName) {
        mutate(categoryName, -1);
    }

    public static Object entry(int id) {
        try {
            return Class.forName(CATALOG).getMethod("byId", int.class).invoke(null, id);
        } catch (ReflectiveOperationException exception) {
            throw failure("find entry", exception);
        }
    }

    public static String textureResource(int id) {
        Object entry = entry(id);
        return entry == null ? null : (String) callUnchecked(entry, "textureResource");
    }

    public static String modelResource(int id) {
        Object entry = entry(id);
        return entry == null ? null : (String) callUnchecked(entry, "modelResource");
    }

    public static int textureWidth(int id) {
        Object entry = entry(id);
        return entry == null ? 1 : (int) callUnchecked(entry, "textureWidth");
    }

    public static int textureHeight(int id) {
        Object entry = entry(id);
        return entry == null ? 1 : (int) callUnchecked(entry, "textureHeight");
    }

    public static boolean render(int id, MatrixStack matrices, VertexConsumerProvider providers,
                                 int light, float timeSeconds) {
        return renderWith("render", id, matrices, providers, light,
                false, false, false, false, timeSeconds);
    }

    public static boolean renderPet(int id, MatrixStack matrices, VertexConsumerProvider providers,
                                    int light, float timeSeconds) {
        return renderWith("renderPet", id, matrices, providers, light,
                false, false, false, false, timeSeconds);
    }

    public static boolean renderPet(int id, MatrixStack matrices, VertexConsumerProvider providers,
                                    int light, boolean gliding, boolean swimming, boolean sneaking,
                                    boolean moving, float timeSeconds) {
        return renderWith("renderPet", id, matrices, providers, light,
                gliding, swimming, sneaking, moving, timeSeconds);
    }

    private static boolean renderWith(String methodName, int id, MatrixStack matrices,
                                      VertexConsumerProvider providers, int light, boolean gliding,
                                      boolean swimming, boolean sneaking, boolean moving,
                                      float timeSeconds) {
        Object entry = entry(id);
        if (entry == null) return false;
        try {
            Class<?> renderer = Class.forName("dev.pulse.cosmetics.pulserender.PulseGeoRenderer");
            for (Method method : renderer.getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 9) continue;
                return (boolean) method.invoke(null, entry, matrices, providers, light,
                        gliding, swimming, sneaking, moving, timeSeconds);
            }
            return false;
        } catch (ReflectiveOperationException exception) {
            throw failure("render cosmetic", exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void mutate(String categoryName, int id) {
        if (!available()) return;
        try {
            Class<?> selection = Class.forName(SELECTION);
            Object category = category(categoryName);
            Field selectedField = selection.getDeclaredField("SELECTED");
            selectedField.setAccessible(true);
            Map selected = (Map) selectedField.get(null);
            if (id < 0) selected.remove(category);
            else {
                Object entry = entry(id);
                if (entry == null || call(entry, "category") != category) return;
                selected.put(category, id);
            }
            Method save = selection.getDeclaredMethod("save");
            save.setAccessible(true);
            save.invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw failure("update selection", exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object category(String name) throws ReflectiveOperationException {
        Class<? extends Enum> type = (Class<? extends Enum>) Class.forName(CATEGORY).asSubclass(Enum.class);
        return Enum.valueOf(type, name);
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object callUnchecked(Object target, String method) {
        try {
            return call(target, method);
        } catch (ReflectiveOperationException exception) {
            throw failure("read entry", exception);
        }
    }

    private static IllegalStateException failure(String action, ReflectiveOperationException cause) {
        return new IllegalStateException("Unable to " + action + " from the embedded Pulse runtime", cause);
    }
}
