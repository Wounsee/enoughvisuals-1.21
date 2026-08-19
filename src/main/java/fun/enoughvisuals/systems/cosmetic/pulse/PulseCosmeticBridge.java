package fun.enoughvisuals.systems.cosmetic.pulse;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reflection boundary around the embedded intermediary-mapped Pulse runtime. */
public final class PulseCosmeticBridge {
    public record Item(int id, String name, String category) {}

    private static final String CATALOG = "dev.pulse.cosmetics.CosmeticCatalog";
    private static final String CATEGORY = "dev.pulse.cosmetics.CosmeticCategory";
    private static final String SELECTION = "dev.pulse.cosmetics.CosmeticSelection";

    private PulseCosmeticBridge() {}

    private static boolean customRegistered;

    public static boolean available() {
        try {
            Class.forName(CATALOG, false, PulseCosmeticBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private record CustomDef(int id, String category, String name, int texW, int texH) {}

    private static final CustomDef[] CUSTOM_DEFS = {
            // Hats
            new CustomDef(201, "HAT", "Bear Hat", 64, 64),
            new CustomDef(202, "HAT", "Blackhole Halo", 64, 64),
            new CustomDef(203, "HAT", "Frog Hat", 64, 64),
            new CustomDef(204, "HAT", "Pilot Hat", 64, 64),

            // Wings
            new CustomDef(210, "WINGS", "Archangel Wings", 64, 64),
            new CustomDef(211, "WINGS", "Balloon Wings", 64, 64),
            new CustomDef(212, "WINGS", "Blackhole Wings", 64, 64),
            new CustomDef(213, "WINGS", "Butterfly Wings", 64, 64),
            new CustomDef(214, "WINGS", "Draco Wings", 64, 64),
            new CustomDef(215, "WINGS", "Rocket Wings", 64, 64),
            new CustomDef(216, "WINGS", "Steampunk Wings", 64, 64),
            new CustomDef(217, "WINGS", "Techno Wings", 64, 64),

            // Pets
            new CustomDef(220, "PET", "Axolotl Pet", 64, 64),
            new CustomDef(221, "PET", "Big Cat Pet", 64, 64),
            new CustomDef(222, "PET", "Capybara Pet", 64, 64),
            new CustomDef(223, "PET", "Dragon Pet", 32, 32),
            new CustomDef(224, "PET", "Frog Custom Pet", 128, 128),
            new CustomDef(225, "PET", "Guinea Pig Pet", 64, 64),
            new CustomDef(226, "PET", "Panda Pet", 64, 64),
            new CustomDef(227, "PET", "Patrick Pet", 64, 64),
            new CustomDef(228, "PET", "Pig Pet", 64, 64),
            new CustomDef(229, "PET", "Spongebob Pet", 64, 64),
            new CustomDef(230, "PET", "Bigear Goat Pet", 256, 256),
            new CustomDef(231, "PET", "Chekushka Pet", 512, 512),
            new CustomDef(232, "PET", "Owl Jump Rope Pet", 64, 64),
            new CustomDef(233, "PET", "UFO Pet", 128, 128)
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized void ensureCustomRegistered() {
        if (customRegistered || !available()) return;
        customRegistered = true;
        try {
            Class<?> catalogType = Class.forName(CATALOG);
            Class<?> entryType = Class.forName("dev.pulse.cosmetics.CosmeticEntry");
            Class<?> kindType = Class.forName("dev.pulse.cosmetics.CosmeticKind");
            Class<?> categoryType = Class.forName(CATEGORY);
            Method addMethod = catalogType.getDeclaredMethod("add", entryType);
            addMethod.setAccessible(true);

            Method byIdMethod = catalogType.getMethod("byId", int.class);
            Object geometryKind = Enum.valueOf((Class<Enum>) kindType.asSubclass(Enum.class), "GEOMETRY");

            Constructor<?> entryConstructor = entryType.getConstructor(
                    int.class, categoryType, String.class, kindType,
                    String.class, String.class, String.class,
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class
            );

            for (CustomDef def : CUSTOM_DEFS) {
                if (byIdMethod.invoke(null, def.id()) != null) continue;
                String catLower = def.category().toLowerCase(java.util.Locale.ROOT);
                Object catEnum = category(def.category());
                String modelRes = "assets/pulsecosmetics/cosmetics/" + catLower + "/" + def.id() + "/model.json";
                String texRes = "pulsecosmetics:cosmetics/" + catLower + "/" + def.id() + "/texture.png";
                String prevRes = "pulsecosmetics:cosmetics/" + catLower + "/" + def.id() + "/preview.png";

                Object entry = entryConstructor.newInstance(
                        def.id(), catEnum, def.name(), geometryKind,
                        modelRes, texRes, prevRes,
                        def.texW(), def.texH(), 1, def.texW(), def.texH(), 1, 0
                );
                addMethod.invoke(null, entry);
            }
        } catch (ReflectiveOperationException exception) {
            throw failure("register custom cosmetics", exception);
        }
    }

    public static List<Item> items(String categoryName) {
        if (!available()) return List.of();
        ensureCustomRegistered();
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

    /** Replaces the embedded renderer's per-player state with the authoritative Enough sync. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void syncRemote(Map<UUID, Map<String, Integer>> selections) {
        if (!available()) return;
        try {
            Class<?> onlineType = Class.forName("dev.pulse.cosmetics.PulseOnlineCosmetics");
            Object online = onlineType.getMethod("getInstance").invoke(null);
            Field remoteField = onlineType.getDeclaredField("remote");
            remoteField.setAccessible(true);
            Map remote = (Map) remoteField.get(online);

            Class<?> selectionType = Class.forName("dev.pulse.cosmetics.PulseOnlineCosmetics$RemoteSelection");
            Constructor<?> constructor = selectionType.getDeclaredConstructor(int[].class, boolean.class, long.class);
            constructor.setAccessible(true);
            remote.clear();
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Map<String, Integer>> player : selections.entrySet()) {
                Map<String, Integer> value = player.getValue();
                int[] ids = {
                        value.getOrDefault("WINGS", -1),
                        -1,
                        value.getOrDefault("HAT", -1),
                        value.getOrDefault("BODYWEAR", -1),
                        value.getOrDefault("PET", -1)
                };
                boolean equipped = false;
                for (int id : ids) equipped |= id >= 0;
                if (equipped) remote.put(player.getKey(), constructor.newInstance(ids, true, now));
            }
        } catch (ReflectiveOperationException exception) {
            throw failure("sync remote cosmetics", exception);
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
