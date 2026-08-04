package punchy.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_746;
import punchy.client.state.AttackActionTracker;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/BetterCombatCompat.class */
public final class BetterCombatCompat {
    private static final String EVENTS_CLASS = "net.bettercombat.api.client.BetterCombatClientEvents";
    private static final String ATTACK_START_LISTENER = "net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackStart";
    private static final String ATTACK_HIT_LISTENER = "net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackHit";
    private static final String CLIENT_MOD_CLASS = "net.bettercombat.client.BetterCombatClientMod";
    private static final String PLAYER_ANIM_ACCESS = "com.zigythebird.playeranim.api.PlayerAnimationAccess";
    private static final String WEAPON_REGISTRY_CLASS = "net.bettercombat.logic.WeaponRegistry";
    private static boolean available = false;
    private static boolean initialized = false;
    private static Object previousFirstPersonMode;
    private static Boolean previousShowArms;
    private static Boolean previousShowOtherHand;
    private static Method weaponAttributesMethod;

    private BetterCombatCompat() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        available = isClassPresent(EVENTS_CLASS);
        if (!available) {
            return;
        }
        applyVanillaItemDisplay(PunchyConfig.isBetterCombatCompatEnabled());
        try {
            Class<?> events = Class.forName(EVENTS_CLASS);
            Field attackStartField = events.getField("ATTACK_START");
            Object startPublisher = attackStartField.get(null);
            Class<?> startListenerType = Class.forName(ATTACK_START_LISTENER);
            Method startRegister = findRegisterMethod(startPublisher, startListenerType);
            if (startRegister == null) {
                available = false;
                return;
            }
            Object startListener = Proxy.newProxyInstance(startListenerType.getClassLoader(), new Class[]{startListenerType}, (proxy, method, args) -> {
                if ("onPlayerAttackStart".equals(method.getName()) && args != null && args.length >= 2) {
                    handleAttackStart(args[0], args[1]);
                    return null;
                }
                return null;
            });
            startRegister.invoke(startPublisher, startListener);
            Field attackHitField = events.getField("ATTACK_HIT");
            Object hitPublisher = attackHitField.get(null);
            Class<?> hitListenerType = Class.forName(ATTACK_HIT_LISTENER);
            Method hitRegister = findRegisterMethod(hitPublisher, hitListenerType);
            if (hitRegister == null) {
                available = false;
            } else {
                Object hitListener = Proxy.newProxyInstance(hitListenerType.getClassLoader(), new Class[]{hitListenerType}, (proxy2, method2, args2) -> {
                    if ("onPlayerAttackStart".equals(method2.getName()) && args2 != null && args2.length >= 2) {
                        handleAttackHit(args2[0], args2[1]);
                        return null;
                    }
                    return null;
                });
                hitRegister.invoke(hitPublisher, hitListener);
            }
        } catch (Exception e) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean isWeaponStack(class_1799 stack) {
        if (!available || stack == null || stack.method_7960()) {
            return false;
        }
        try {
            if (weaponAttributesMethod == null) {
                Class<?> registry = Class.forName(WEAPON_REGISTRY_CLASS);
                weaponAttributesMethod = registry.getMethod("getAttributes", class_1799.class);
            }
            Object attrs = weaponAttributesMethod.invoke(null, stack);
            return attrs != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void forceStopAttackAnimation(class_746 player) {
        if (!available || player == null || !PunchyConfig.isBetterCombatCompatEnabled() || FirstPersonModelCompat.isRenderingPlayer() || FirstPersonModelCompat.shouldApplyThirdPersonAnimations(class_310.method_1551()) || isPlayerBlacklisted(player)) {
            return;
        }
        stopBetterCombatAnimation(player);
    }

    public static void suppressItemPoses(class_746 player) {
        if (!available || player == null || !PunchyConfig.isBetterCombatCompatEnabled() || FirstPersonModelCompat.isRenderingPlayer() || FirstPersonModelCompat.shouldApplyThirdPersonAnimations(class_310.method_1551()) || isPlayerBlacklisted(player)) {
            return;
        }
        try {
            Class<?> access = Class.forName(PLAYER_ANIM_ACCESS);
            Method getLayer = findMethod(access, "getPlayerAnimationLayer", 2);
            if (getLayer == null) {
                return;
            }
            Object attackLayer = getLayer.invoke(null, player, createIdentifier("bettercombat", "attack", getLayer));
            stopLayerAnimation(attackLayer);
            Object mainBody = getLayer.invoke(null, player, createIdentifier("bettercombat", "pose_main_hand_body", getLayer));
            Object mainItem = getLayer.invoke(null, player, createIdentifier("bettercombat", "pose_main_hand_item", getLayer));
            Object offBody = getLayer.invoke(null, player, createIdentifier("bettercombat", "pose_off_hand_body", getLayer));
            Object offItem = getLayer.invoke(null, player, createIdentifier("bettercombat", "pose_off_hand_item", getLayer));
            boolean leftHanded = player.method_6068() == class_1306.field_6182;
            clearPoseLayer(mainBody, leftHanded);
            clearPoseLayer(mainItem, leftHanded);
            clearPoseLayer(offBody, leftHanded);
            clearPoseLayer(offItem, leftHanded);
        } catch (Exception e) {
        }
    }

    public static void applyVanillaItemDisplay(boolean enabled) {
        Boolean bool;
        Boolean bool2;
        if (!available) {
            return;
        }
        try {
            Class<?> clientMod = Class.forName(CLIENT_MOD_CLASS);
            Field configField = clientMod.getField("config");
            Object config = configField.get(null);
            if (config == null) {
                return;
            }
            Field firstPersonField = config.getClass().getField("firstPersonAnimations");
            Field showArmsField = config.getClass().getField("isShowingArmsInFirstPerson");
            Field showOtherHandField = config.getClass().getField("isShowingOtherHandFirstPerson");
            if (enabled) {
                if (previousFirstPersonMode == null) {
                    previousFirstPersonMode = firstPersonField.get(config);
                }
                if (previousShowArms == null) {
                    Object current = showArmsField.get(config);
                    if (current instanceof Boolean) {
                        Boolean value = (Boolean) current;
                        bool2 = value;
                    } else {
                        bool2 = null;
                    }
                    previousShowArms = bool2;
                }
                if (previousShowOtherHand == null) {
                    Object current2 = showOtherHandField.get(config);
                    if (current2 instanceof Boolean) {
                        Boolean value2 = (Boolean) current2;
                        bool = value2;
                    } else {
                        bool = null;
                    }
                    previousShowOtherHand = bool;
                }
                Object noValue = findEnumValue(firstPersonField.getType(), "NO");
                if (noValue != null) {
                    firstPersonField.set(config, noValue);
                }
                showArmsField.set(config, false);
                showOtherHandField.set(config, true);
            } else if (previousFirstPersonMode != null) {
                firstPersonField.set(config, previousFirstPersonMode);
                previousFirstPersonMode = null;
                if (previousShowArms != null) {
                    showArmsField.set(config, previousShowArms);
                }
                if (previousShowOtherHand != null) {
                    showOtherHandField.set(config, previousShowOtherHand);
                }
                previousShowArms = null;
                previousShowOtherHand = null;
            }
        } catch (Exception e) {
        }
    }

    private static void handleAttackStart(Object playerObj, Object attackHandObj) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || !PunchyConfig.isBetterCombatCompatEnabled() || isPlayerBlacklisted(client.field_1724) || !client.field_1690.method_31044().method_31034()) {
            return;
        }
        AttackActionTracker.recordCriticalCandidate(client);
        if (FirstPersonModelCompat.isRenderingPlayer() || FirstPersonModelCompat.shouldApplyThirdPersonAnimations(client)) {
            return;
        }
        stopBetterCombatAnimation(client.field_1724);
    }

    private static void handleAttackHit(Object playerObj, Object attackHandObj) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || !PunchyConfig.isBetterCombatCompatEnabled() || isPlayerBlacklisted(client.field_1724) || !client.field_1690.method_31044().method_31034()) {
            return;
        }
        class_1268 hand = readIsOffHand(attackHandObj) ? class_1268.field_5810 : class_1268.field_5808;
        AttackActionTracker.recordAttack(hand);
        if (FirstPersonModelCompat.isRenderingPlayer() || FirstPersonModelCompat.shouldApplyThirdPersonAnimations(client)) {
            return;
        }
        stopBetterCombatAnimation(client.field_1724);
    }

    private static boolean isPlayerBlacklisted(class_1657 player) {
        if (player == null) {
            return false;
        }
        return PunchyConfig.isHandBlacklisted(player, class_1268.field_5808) || PunchyConfig.isHandBlacklisted(player, class_1268.field_5810);
    }

    private static boolean readIsOffHand(Object attackHandObj) {
        if (attackHandObj == null) {
            return false;
        }
        try {
            Method isOffHand = attackHandObj.getClass().getMethod("isOffHand", new Class[0]);
            Object result = isOffHand.invoke(attackHandObj, new Object[0]);
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

    private static void stopBetterCombatAnimation(class_746 player) {
        try {
            Method stop = player.getClass().getMethod("stopAttackAnimation", Float.TYPE);
            stop.invoke(player, Float.valueOf(0.0f));
        } catch (Exception e) {
        }
    }

    private static Method findRegisterMethod(Object publisher, Class<?> listenerType) {
        if (publisher == null || listenerType == null) {
            return null;
        }
        for (Method method : publisher.getClass().getMethods()) {
            if (method.getParameterCount() == 1) {
                Class<?> param = method.getParameterTypes()[0];
                if (param.isAssignableFrom(listenerType) || listenerType.isAssignableFrom(param)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object createIdentifier(String namespace, String path, Method getLayer) {
        if (getLayer == null) {
            return null;
        }
        Class<?> idType = getLayer.getParameterTypes()[1];
        try {
            if ("net.minecraft.resources.ResourceLocation".equals(idType.getName())) {
                return class_2960.method_60655(namespace, path);
            }
        } catch (Exception e) {
        }
        try {
            if ("net.minecraft.util.ResourceLocation".equals(idType.getName())) {
                Method of = idType.getMethod("of", String.class, String.class);
                return of.invoke(null, namespace, path);
            }
            return null;
        } catch (Exception e2) {
            return null;
        }
    }

    private static void clearPoseLayer(Object layer, boolean leftHanded) {
        if (layer == null) {
            return;
        }
        try {
            Method setPose = findMethod(layer.getClass(), "setPose", 2);
            if (setPose != null) {
                setPose.invoke(layer, null, Boolean.valueOf(leftHanded));
            }
        } catch (Exception e) {
        }
    }

    private static void stopLayerAnimation(Object layer) {
        if (layer == null) {
            return;
        }
        try {
            Method stop = findMethod(layer.getClass(), "stopTriggeredAnimation", 0);
            if (stop != null) {
                stop.invoke(layer, new Object[0]);
            }
        } catch (Exception e) {
        }
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
