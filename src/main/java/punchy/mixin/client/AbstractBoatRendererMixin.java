package punchy.mixin.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_10004;
import net.minecraft.class_10255;
import net.minecraft.class_10256;
import net.minecraft.class_10262;
import net.minecraft.class_583;
import net.minecraft.class_630;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.BoatInHandContext;
import punchy.client.render.BoatLidState;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/AbstractBoatRendererMixin.class */
@Mixin({class_10262.class})
public abstract class AbstractBoatRendererMixin {

    @Unique
    private static final Map<Object, class_630> RENDERER_LID_CACHE = new IdentityHashMap();

    @Unique
    private static final String[] LID_NAMES = {"chest_lid", "lid", "chestlid", "chest_top", "chest", "top"};

    @Shadow
    protected abstract class_583<?> method_64517();

    @Inject(method = {"method_64518(Lnet/minecraft/class_10255;Lnet/minecraft/class_10004;F)V"}, at = {@At("TAIL")})
    private void punchy$storeBoatState(class_10255 boat, class_10004 state, float tickDelta, CallbackInfo ci) {
        if (state instanceof BoatLidState) {
            BoatLidState lidState = (BoatLidState) state;
            lidState.punchy$setInHand(BoatInHandContext.isActive());
            lidState.punchy$setVy(boat.method_18798().field_1351);
            lidState.punchy$setChestBoat(boat instanceof class_10256);
            lidState.punchy$setLidApplied(false);
        }
    }

    @Unique
    private static class_630 resolveRendererLid(Object renderer) {
        if (renderer == null) {
            return null;
        }
        class_630 cached = RENDERER_LID_CACHE.get(renderer);
        if (cached != null) {
            return cached;
        }
        Class<?> superclass = renderer.getClass();
        while (true) {
            Class<?> type = superclass;
            if (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(renderer);
                        String fieldName = field.getName().toLowerCase(Locale.ROOT);
                        if (value instanceof class_583) {
                            class_583<?> model = (class_583) value;
                            class_630 lid = findLidInModel(model);
                            if (lid != null) {
                                RENDERER_LID_CACHE.put(renderer, lid);
                                return lid;
                            }
                        } else if (value instanceof class_630) {
                            class_630 part = (class_630) value;
                            class_630 lid2 = findLidInPart(part, fieldName);
                            if (lid2 != null) {
                                RENDERER_LID_CACHE.put(renderer, lid2);
                                return lid2;
                            }
                        } else {
                            continue;
                        }
                    } catch (IllegalAccessException e) {
                    }
                }
                superclass = type.getSuperclass();
            } else {
                return null;
            }
        }
    }

    @Unique
    private static class_630 findLidInModel(class_583<?> model) {
        class_630 root;
        if (model == null || (root = findRootPart(model)) == null) {
            return null;
        }
        for (String name : LID_NAMES) {
            class_630 lid = findChildRecursive(root, name);
            if (lid != null) {
                return lid;
            }
        }
        return null;
    }

    @Unique
    private static class_630 findLidInPart(class_630 part, String fieldName) {
        if (part == null) {
            return null;
        }
        if (fieldName != null) {
            String name = fieldName.toLowerCase(Locale.ROOT);
            if (name.contains("lid")) {
                return part;
            }
        }
        for (String lidName : LID_NAMES) {
            class_630 found = findChildRecursive(part, lidName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Unique
    private static class_630 findRootPart(class_583<?> model) {
        Class<?> superclass = model.getClass();
        while (true) {
            Class<?> type = superclass;
            if (type != null && type != Object.class) {
                try {
                    Method method = type.getDeclaredMethod("root", new Class[0]);
                    if (class_630.class.isAssignableFrom(method.getReturnType())) {
                        method.setAccessible(true);
                        return (class_630) method.invoke(model, new Object[0]);
                    }
                } catch (Throwable th) {
                }
                for (Field field : type.getDeclaredFields()) {
                    if (class_630.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            return (class_630) field.get(model);
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
                superclass = type.getSuperclass();
            } else {
                return null;
            }
        }
    }

    @Unique
    private static class_630 findChildRecursive(class_630 root, String name) {
        if (root == null || name == null) {
            return null;
        }
        try {
            class_630 direct = root.method_32086(name);
            if (direct != null) {
                return direct;
            }
        } catch (Throwable th) {
        }
        Map<?, ?> children = getChildren(root);
        if (children == null || children.isEmpty()) {
            return null;
        }
        for (Map.Entry<?, ?> entry : children.entrySet()) {
            Object keyObj = entry.getKey();
            if (keyObj instanceof String) {
                String key = (String) keyObj;
                if (key.toLowerCase(Locale.ROOT).contains(name)) {
                    Object value = entry.getValue();
                    if (value instanceof class_630) {
                        class_630 part = (class_630) value;
                        return part;
                    }
                } else {
                    continue;
                }
            }
        }
        for (Object value2 : children.values()) {
            if (value2 instanceof class_630) {
                class_630 child = (class_630) value2;
                class_630 found = findChildRecursive(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Unique
    private static Map<?, ?> getChildren(class_630 part) {
        Class<?> superclass = part.getClass();
        while (true) {
            Class<?> type = superclass;
            if (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(part);
                            if (value instanceof Map) {
                                Map<?, ?> map = (Map) value;
                                return map;
                            }
                            continue;
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
                superclass = type.getSuperclass();
            } else {
                return null;
            }
        }
    }
}
