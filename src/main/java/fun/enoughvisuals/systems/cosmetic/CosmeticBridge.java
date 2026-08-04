package fun.enoughvisuals.systems.cosmetic;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Client-side boundary to the embedded cosmetic runtime. */
public final class CosmeticBridge {
    private static final String BRIDGE = "fun.wounsee.cosmetic.bridge.EnoughCosmeticBridge";

    private CosmeticBridge() {}

    public static boolean available() {
        try {
            Class.forName(BRIDGE, false, CosmeticBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static Path avatarsDirectory() {
        return invoke("avatarsDirectory", Path.class);
    }

    public static Path equippedAvatar() {
        return invoke("equippedAvatar", Path.class);
    }

    public static void equip(Path avatar) {
        invokeVoid("equip", new Class<?>[]{Path.class}, avatar);
    }

    public static void equip(List<Path> avatars) {
        invokeVoid("equipComposite", new Class<?>[]{List.class}, avatars);
    }

    public static void equipRemote(UUID player, List<Path> avatars) {
        invokeVoid("equipRemote", new Class<?>[]{UUID.class, List.class}, player, avatars);
    }

    public static void equipPreview(UUID player, List<Path> avatars) {
        if (!invokeOptionalVoid("equipPreview", new Class<?>[]{UUID.class, List.class}, player, avatars)) {
            equipRemote(player, avatars);
        }
    }

    public static void bindPreviewEntity(UUID player, Object entity) {
        invokeOptionalVoid("bindPreviewEntity", new Class<?>[]{UUID.class, Object.class}, player, entity);
    }

    public static void unbindPreviewEntity(UUID player, Object entity) {
        invokeOptionalVoid("unbindPreviewEntity", new Class<?>[]{UUID.class, Object.class}, player, entity);
    }

    public static void setPreviewAlpha(float alpha) {
        invokeOptionalVoid("setPreviewAlpha", new Class<?>[]{float.class}, alpha);
    }

    public static boolean previewReady(UUID player) {
        return invokeOptional("previewReady", Boolean.class,
                new Class<?>[]{UUID.class}, player).orElse(false);
    }

    public static void clearRemote(UUID player) {
        invokeVoid("clearRemote", new Class<?>[]{UUID.class}, player);
    }

    public static void reload() {
        invokeVoid("reload", new Class<?>[0]);
    }

    public static void unequip() {
        invokeVoid("unequip", new Class<?>[0]);
    }

    private static <T> T invoke(String name, Class<T> result) {
        try {
            Object value = Class.forName(BRIDGE).getMethod(name).invoke(null);
            return result.cast(value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Embedded cosmetic runtime is unavailable", exception);
        }
    }

    private static void invokeVoid(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = Class.forName(BRIDGE).getMethod(name, parameterTypes);
            method.invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Embedded cosmetic runtime is unavailable", exception);
        }
    }

    private static boolean invokeOptionalVoid(String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = Class.forName(BRIDGE).getMethod(name, parameterTypes);
            method.invoke(null, args);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Embedded cosmetic runtime is unavailable", exception);
        }
    }

    private static <T> java.util.Optional<T> invokeOptional(
            String name, Class<T> result, Class<?>[] parameterTypes, Object... args) {
        try {
            Object value = Class.forName(BRIDGE).getMethod(name, parameterTypes).invoke(null, args);
            return java.util.Optional.ofNullable(result.cast(value));
        } catch (NoSuchMethodException ignored) {
            return java.util.Optional.empty();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Embedded cosmetic runtime is unavailable", exception);
        }
    }
}
