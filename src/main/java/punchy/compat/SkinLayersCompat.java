package punchy.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.class_591;
import net.minecraft.class_630;
import net.minecraft.class_742;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/SkinLayersCompat.class */
public final class SkinLayersCompat {
    private static final String MOD_ID = "skinlayers3d";
    private static boolean checkedAvailability;
    private static boolean available;
    private static boolean reflectionReady;
    private static boolean reflectionFailed;
    private static Class<?> playerSettingsClass;
    private static Class<?> modelPartInjectorClass;
    private static Method setup3dLayersMethod;
    private static Method setInjectedMeshMethod;
    private static Method getLeftArmMeshMethod;
    private static Method getRightArmMeshMethod;
    private static Field configField;
    private static Field enableLeftSleeveField;
    private static Field enableRightSleeveField;
    private static Field fpLeftArmField;
    private static Field fpLeftArmSlimField;
    private static Field fpRightArmField;
    private static Field fpRightArmSlimField;

    private SkinLayersCompat() {
    }

    public static boolean isAvailable() {
        if (!checkedAvailability) {
            checkedAvailability = true;
            available = Services.PLATFORM != null && Services.PLATFORM.isModLoaded(MOD_ID);
        }
        return available;
    }

    public static void applyFirstPersonSleeves(class_742 player, class_591 playerModel, boolean slim, boolean renderLeft, boolean renderRight) {
        if (playerModel == null) {
            return;
        }
        clearInjectedMesh(playerModel.field_3484);
        clearInjectedMesh(playerModel.field_3486);
        if (!isAvailable() || player == null || !ensureReflection() || !playerSettingsClass.isInstance(player)) {
            return;
        }
        try {
            Object setupResult = setup3dLayersMethod.invoke(null, player, player, Boolean.valueOf(slim));
            if (setupResult instanceof Boolean) {
                Boolean ok = (Boolean) setupResult;
                if (!ok.booleanValue()) {
                    return;
                }
                if (renderLeft && isSleeveEnabled(true)) {
                    Object leftMesh = getLeftArmMeshMethod.invoke(player, new Object[0]);
                    Object leftOffset = (slim ? fpLeftArmSlimField : fpLeftArmField).get(null);
                    setInjectedMeshMethod.invoke(playerModel.field_3484, leftMesh, leftOffset);
                }
                if (renderRight && isSleeveEnabled(false)) {
                    Object rightMesh = getRightArmMeshMethod.invoke(player, new Object[0]);
                    Object rightOffset = (slim ? fpRightArmSlimField : fpRightArmField).get(null);
                    setInjectedMeshMethod.invoke(playerModel.field_3486, rightMesh, rightOffset);
                }
            }
        } catch (Throwable th) {
        }
    }

    private static void clearInjectedMesh(class_630 sleeve) {
        if (sleeve == null || !isAvailable() || !ensureReflection() || !modelPartInjectorClass.isInstance(sleeve)) {
            return;
        }
        try {
            setInjectedMeshMethod.invoke(sleeve, null, null);
        } catch (Throwable th) {
        }
    }

    private static boolean ensureReflection() {
        if (reflectionReady) {
            return true;
        }
        if (reflectionFailed) {
            return false;
        }
        try {
            playerSettingsClass = Class.forName("dev.tr7zw.skinlayers.accessor.PlayerSettings");
            modelPartInjectorClass = Class.forName("dev.tr7zw.skinlayers.accessor.ModelPartInjector");
            Class<?> skinUtilClass = Class.forName("dev.tr7zw.skinlayers.SkinUtil");
            Class<?> skinLayersModBaseClass = Class.forName("dev.tr7zw.skinlayers.SkinLayersModBase");
            Class<?> offsetProviderClass = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
            setup3dLayersMethod = findSetup3dLayers(skinUtilClass);
            if (setup3dLayersMethod == null) {
                throw new NoSuchMethodException("setup3dLayers(player, settings, slim)");
            }
            setInjectedMeshMethod = modelPartInjectorClass.getMethod("setInjectedMesh", Class.forName("dev.tr7zw.skinlayers.api.Mesh"), offsetProviderClass);
            getLeftArmMeshMethod = playerSettingsClass.getMethod("getLeftArmMesh", new Class[0]);
            getRightArmMeshMethod = playerSettingsClass.getMethod("getRightArmMesh", new Class[0]);
            configField = skinLayersModBaseClass.getField("config");
            Class<?> configClass = configField.getType();
            enableLeftSleeveField = configClass.getField("enableLeftSleeve");
            enableRightSleeveField = configClass.getField("enableRightSleeve");
            fpLeftArmField = offsetProviderClass.getField("FIRSTPERSON_LEFT_ARM");
            fpLeftArmSlimField = offsetProviderClass.getField("FIRSTPERSON_LEFT_ARM_SLIM");
            fpRightArmField = offsetProviderClass.getField("FIRSTPERSON_RIGHT_ARM");
            fpRightArmSlimField = offsetProviderClass.getField("FIRSTPERSON_RIGHT_ARM_SLIM");
            reflectionReady = true;
            return true;
        } catch (Throwable th) {
            reflectionFailed = true;
            return false;
        }
    }

    private static Method findSetup3dLayers(Class<?> skinUtilClass) {
        for (Method method : skinUtilClass.getMethods()) {
            if ("setup3dLayers".equals(method.getName())) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 3 && params[1].getName().equals("dev.tr7zw.skinlayers.accessor.PlayerSettings") && (params[2] == Boolean.TYPE || params[2] == Boolean.class)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean isSleeveEnabled(boolean left) throws IllegalAccessException {
        Object config = configField.get(null);
        if (config == null) {
            return true;
        }
        return left ? enableLeftSleeveField.getBoolean(config) : enableRightSleeveField.getBoolean(config);
    }
}
