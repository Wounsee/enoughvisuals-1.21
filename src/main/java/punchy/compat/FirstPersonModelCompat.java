package punchy.compat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.class_1268;
import net.minecraft.class_310;
import punchy.client.animation.ItemAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.render.PunchyArmRenderer;
import punchy.config.PunchyConfig;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/FirstPersonModelCompat.class */
public final class FirstPersonModelCompat {
    private static final String MOD_ID = "firstperson";
    private static final String FPM_API_CLASS = "dev.tr7zw.firstperson.api.FirstPersonAPI";
    private static final String FPM_ACTIVATION_HANDLER = "dev.tr7zw.firstperson.api.ActivationHandler";
    private static boolean available;
    private static boolean checked;
    private static boolean active;
    private static boolean hidden;
    private static int transitionTicks;
    private static boolean pendingHide;
    private static boolean pendingShow;
    private static State state = State.VISIBLE;
    private static boolean fpmLookupDone;
    private static Method fpmIsRenderingPlayer;
    private static boolean activationRegistered;
    private static boolean bodyRenderActive;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/FirstPersonModelCompat$State.class */
    private enum State {
        VISIBLE,
        HIDING,
        HIDDEN,
        SHOWING
    }

    private FirstPersonModelCompat() {
    }

    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            available = Services.PLATFORM != null && Services.PLATFORM.isModLoaded(MOD_ID);
        }
        return available;
    }

    public static void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            deactivate();
            return;
        }
        boolean activeNow = isAvailable() && client.field_1690.method_31044().method_31034();
        if (!activeNow) {
            deactivate();
            return;
        }
        ensureActivationHandlerRegistered();
        active = true;
        if (!PunchyConfig.isFirstPersonModelHideEnabled()) {
            pendingHide = false;
            pendingShow = false;
            hidden = false;
            transitionTicks = 0;
            state = State.VISIBLE;
            bodyRenderActive = false;
            PunchyArmRenderer.setFirstPersonHidden(false);
            return;
        }
        if (PunchyConfig.isFirstPersonModelHideNever()) {
            pendingHide = false;
            pendingShow = false;
            hidden = false;
            transitionTicks = 0;
            state = State.VISIBLE;
            bodyRenderActive = true;
            PunchyArmRenderer.setFirstPersonHidden(false);
            return;
        }
        float pitch = client.field_1724.method_36455();
        float hidePitch = PunchyConfig.getFirstPersonModelHidePitch();
        boolean wantsHide = pitch > hidePitch;
        if (wantsHide) {
            pendingHide = true;
            pendingShow = false;
            if ((state == State.VISIBLE || state == State.SHOWING) && beginHide(client)) {
                pendingHide = false;
            }
        } else {
            pendingShow = true;
            pendingHide = false;
            if ((state == State.HIDDEN || state == State.HIDING) && beginShow(client)) {
                pendingShow = false;
            }
        }
        advanceTransitions();
        PunchyArmRenderer.setFirstPersonHidden(hidden);
    }

    public static boolean shouldHideHands() {
        return hidden && active;
    }

    public static boolean shouldApplyThirdPersonAnimations(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1690 == null || !isAvailable() || !client.field_1690.method_31044().method_31034()) {
            return false;
        }
        return state == State.HIDDEN || state == State.HIDING;
    }

    public static boolean isRenderingPlayer() {
        if (!isAvailable()) {
            return false;
        }
        if (!fpmLookupDone) {
            fpmLookupDone = true;
            try {
                Class<?> api = Class.forName(FPM_API_CLASS);
                fpmIsRenderingPlayer = api.getMethod("isRenderingPlayer", new Class[0]);
            } catch (Throwable th) {
                fpmIsRenderingPlayer = null;
            }
        }
        if (fpmIsRenderingPlayer == null) {
            return false;
        }
        try {
            Object value = fpmIsRenderingPlayer.invoke(null, new Object[0]);
            if (value instanceof Boolean) {
                if (((Boolean) value).booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable th2) {
            return false;
        }
    }

    private static void ensureActivationHandlerRegistered() {
        if (activationRegistered || !isAvailable()) {
            return;
        }
        try {
            Class<?> api = Class.forName(FPM_API_CLASS);
            Class<?> activationHandler = Class.forName(FPM_ACTIVATION_HANDLER);
            Object handler = Proxy.newProxyInstance(FirstPersonModelCompat.class.getClassLoader(), new Class[]{activationHandler}, (proxy, method, args) -> {
                if ("preventFirstperson".equals(method.getName())) {
                    return Boolean.valueOf(!shouldAllowBodyRender(class_310.method_1551()));
                }
                return null;
            });
            api.getMethod("registerPlayerHandler", Object.class).invoke(null, handler);
            activationRegistered = true;
        } catch (Throwable th) {
            activationRegistered = false;
        }
    }

    private static boolean shouldAllowBodyRender(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        if (!PunchyConfig.isFirstPersonModelHideEnabled()) {
            bodyRenderActive = false;
            return false;
        }
        if (PunchyConfig.isFirstPersonModelHideNever()) {
            bodyRenderActive = true;
            return true;
        }
        float pitch = client.field_1724.method_36455();
        float hidePitch = PunchyConfig.getFirstPersonModelHidePitch();
        boolean wantsBody = pitch > hidePitch;
        bodyRenderActive = wantsBody;
        return wantsBody;
    }

    private static void deactivate() {
        if (active && state != State.VISIBLE) {
            ItemAnimationManager.forceTypeOnce(class_1268.field_5808, "hand_in");
            ItemAnimationManager.forceTypeOnce(class_1268.field_5810, "hand_in");
        }
        active = false;
        hidden = false;
        transitionTicks = 0;
        pendingHide = false;
        pendingShow = false;
        bodyRenderActive = false;
        state = State.VISIBLE;
        PunchyArmRenderer.setFirstPersonHidden(false);
    }

    private static boolean beginHide(class_310 client) {
        hidden = true;
        state = State.HIDDEN;
        transitionTicks = 0;
        return true;
    }

    private static boolean beginShow(class_310 client) {
        if (!canPlayEquipClip()) {
            hidden = false;
            state = State.VISIBLE;
            transitionTicks = 0;
            return true;
        }
        ItemAnimationManager.forceTypeOnce(class_1268.field_5808, "hand_in");
        ItemAnimationManager.forceTypeOnce(class_1268.field_5810, "hand_in");
        playEquipClip(client, "hand_in");
        state = State.SHOWING;
        transitionTicks = 0;
        hidden = false;
        return true;
    }

    private static void advanceTransitions() {
        if (state == State.SHOWING) {
            transitionTicks++;
            if (isPoseClipPlaying("hand_in") || transitionTicks <= 2) {
                return;
            }
            if (transitionTicks > 30 || !isPoseClipPlaying("hand_in")) {
                hidden = false;
                state = State.VISIBLE;
                transitionTicks = 0;
            }
        }
        if (state == State.VISIBLE && pendingHide) {
            class_310 client = class_310.method_1551();
            if (beginHide(client)) {
                pendingHide = false;
                return;
            }
            return;
        }
        if (state == State.HIDDEN && pendingShow) {
            class_310 client2 = class_310.method_1551();
            if (beginShow(client2)) {
                pendingShow = false;
            }
        }
    }

    private static boolean canPlayEquipClip() {
        AnimationClip current;
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        return handler == null || !handler.isPlaying() || (current = handler.getCurrentClip()) == null || PunchyAnimationManager.isClipNamed(current, "hand_in") || PunchyAnimationManager.isClipNamed(current, "hand_out");
    }

    private static void playEquipClip(class_310 client, String name) {
        if (client == null || name == null || name.isBlank()) {
            return;
        }
        AnimationClip clip = PunchyAnimationManager.resolveHandedClip(name, client, class_1268.field_5808);
        if (clip == null) {
            clip = "hand_in".equals(name) ? PunchyAnimationManager.HAND_IN_CLIP : PunchyAnimationManager.HAND_OUT_CLIP;
        }
        if (clip == null) {
            return;
        }
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        handler.setActiveArm(null);
        handler.setLooping(false);
        float speed = PunchyAnimationManager.clipSpeedMultiplier(clip);
        handler.play(clip, speed, true, false);
    }

    private static boolean isPoseClipPlaying(String name) {
        PoseHandler handler = PunchyAnimationManager.POSE_HANDLER;
        if (handler == null || !handler.isPlaying()) {
            return false;
        }
        AnimationClip clip = handler.getCurrentClip();
        return PunchyAnimationManager.isClipNamed(clip, name);
    }
}
