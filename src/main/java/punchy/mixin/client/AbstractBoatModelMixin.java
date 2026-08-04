package punchy.mixin.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_10004;
import net.minecraft.class_10261;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_630;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.render.BoatLidState;
import punchy.client.render.InHandLidPhysics;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/mixin/client/AbstractBoatModelMixin.class */
@Mixin({class_10261.class})
public class AbstractBoatModelMixin {

    @Shadow
    private class_630 field_54480;

    @Shadow
    private class_630 field_54481;

    @Unique
    private static final Map<Object, class_630> LID_CACHE = new IdentityHashMap();

    @Unique
    private static final String[] LID_NAMES = {"chest_lid", "lid", "chestlid", "chest_top", "chest", "top"};

    @Inject(method = {"method_64516(Lnet/minecraft/class_10004;)V"}, at = {@At("TAIL")})
    private void punchy$applyPaddlePhysics(class_10004 boat, CallbackInfo ci) {
        if (!PunchyConfig.disableBoatFirstPersonAnimations() && (boat instanceof BoatLidState)) {
            BoatLidState lidState = (BoatLidState) boat;
            if (!lidState.punchy$isInHand()) {
                return;
            }
            double vy = lidState.punchy$getVy();
            float fall = vy < -0.01d ? ((float) (-vy)) * 1.275f : 0.0f;
            float offset = class_3532.method_15363(fall + 0.0f, -1.275f, 1.275f);
            this.field_54480.field_3654 += offset;
            this.field_54481.field_3654 += offset;
            class_630 chestRoot = findChestRoot(this);
            if (chestRoot != null) {
                setPartVisible(chestRoot, false);
                lidState.punchy$setChestPose(chestRoot.field_3657, chestRoot.field_3656, chestRoot.field_3655, chestRoot.field_3654, chestRoot.field_3675, chestRoot.field_3674);
                lidState.punchy$setChestPoseValid(true);
            } else {
                lidState.punchy$setChestPoseValid(false);
            }
            class_630 lid = resolveLidPart(this);
            if (lid == null) {
                return;
            }
            float target = InHandLidPhysics.computeFallTarget(class_310.method_1551().field_1724);
            float current = lidState.punchy$getLidOpen();
            long now = System.nanoTime();
            float next = InHandLidPhysics.updateLid(current, target, lidState.punchy$getLidLastUpdateNanos(), now);
            lidState.punchy$setLidOpen(next);
            lidState.punchy$setLidLastUpdateNanos(now);
            lidState.punchy$setLidApplied(true);
            lid.field_3654 = (-next) * 1.5707964f;
        }
    }

    @Unique
    private static class_630 resolveLidPart(Object model) {
        if (model == null) {
            return null;
        }
        class_630 cached = LID_CACHE.get(model);
        if (cached != null) {
            return cached;
        }
        class_630 lid = findLidField(model);
        if (lid != null) {
            LID_CACHE.put(model, lid);
            return lid;
        }
        class_630 root = findRootPart(model);
        if (root == null) {
            return null;
        }
        for (String name : LID_NAMES) {
            lid = findChildRecursive(root, name);
            if (lid != null) {
                break;
            }
        }
        if (lid != null) {
            LID_CACHE.put(model, lid);
        }
        return lid;
    }

    @Unique
    private static class_630 findLidField(Object model) {
        Class<?> superclass = model.getClass();
        while (true) {
            Class<?> type = superclass;
            if (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (class_630.class.isAssignableFrom(field.getType())) {
                        String fieldName = field.getName().toLowerCase(Locale.ROOT);
                        if (fieldName.contains("lid") || fieldName.contains("chest")) {
                            try {
                                field.setAccessible(true);
                                class_630 part = (class_630) field.get(model);
                                if (part != null) {
                                    if (fieldName.contains("lid")) {
                                        return part;
                                    }
                                    for (String lidName : LID_NAMES) {
                                        class_630 found = findChildRecursive(part, lidName);
                                        if (found != null) {
                                            return found;
                                        }
                                    }
                                }
                            } catch (IllegalAccessException e) {
                            }
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
    private static class_630 findChestRoot(Object model) {
        Map<?, ?> children;
        class_630 root = findRootPart(model);
        if (root != null && (children = getChildren(root)) != null) {
            for (Map.Entry<?, ?> entry : children.entrySet()) {
                Object keyObj = entry.getKey();
                if (keyObj instanceof String) {
                    String key = (String) keyObj;
                    String lower = key.toLowerCase(Locale.ROOT);
                    if (lower.contains("chest") && !lower.contains("lid")) {
                        Object value = entry.getValue();
                        if (value instanceof class_630) {
                            return (class_630) value;
                        }
                    }
                }
            }
            Iterator<Map.Entry<?, ?>> it = children.entrySet().iterator();
            while (it.hasNext()) {
                Object value2 = it.next().getValue();
                if (value2 instanceof class_630) {
                    class_630 part = (class_630) value2;
                    if (findChildRecursive(part, "lid") != null) {
                        return part;
                    }
                }
            }
            return null;
        }
        return null;
    }

    @Unique
    private static void setPartVisible(class_630 part, boolean visible) {
        if (part == null) {
            return;
        }
        try {
            Field field = class_630.class.getDeclaredField("visible");
            field.setAccessible(true);
            field.setBoolean(part, visible);
        } catch (Throwable th) {
            try {
                Field field2 = class_630.class.getDeclaredField("skipDraw");
                field2.setAccessible(true);
                field2.setBoolean(part, !visible);
            } catch (Throwable th2) {
            }
        }
    }

    @Unique
    private static class_630 findRootPart(Object model) {
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
