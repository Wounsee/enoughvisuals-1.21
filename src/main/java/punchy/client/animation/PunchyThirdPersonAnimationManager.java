package punchy.client.animation;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1306;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.io.PunchyThirdPersonAnimationLoader;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyThirdPersonAnimationManager.class */
public class PunchyThirdPersonAnimationManager {
    public static final PoseHandler POSE_HANDLER = new PoseHandler();
    private static final Map<String, AnimationClip> ALL_CLIPS = new HashMap();
    private static String lastSourceClip;
    private static String manualClipName;

    public static void loadAnimations(class_3300 manager) {
        ALL_CLIPS.clear();
        POSE_HANDLER.stopImmediateWithBlendOut();
        ALL_CLIPS.putAll(PunchyThirdPersonAnimationLoader.loadAll(manager));
    }

    public static AnimationClip resolveClip(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return ALL_CLIPS.get(name.trim());
    }

    public static void tick(class_310 client) {
        if (client == null) {
            return;
        }
        if (!isManualPlaying()) {
            syncWithFirstPerson(client);
        }
        POSE_HANDLER.tick(0.05f, PunchyAnimationManager.ANIMATION_SPEED);
        if (manualClipName != null && !isManualPlaying()) {
            manualClipName = null;
        }
    }

    private static void syncWithFirstPerson(class_310 client) {
        if (client == null) {
            return;
        }
        AnimationClip source = PunchyAnimationManager.POSE_HANDLER.getCurrentClip();
        boolean playing = PunchyAnimationManager.POSE_HANDLER.isPlaying();
        if (!playing || source == null) {
            if (POSE_HANDLER.isPlaying()) {
                POSE_HANDLER.stopImmediateWithBlendOut();
            }
            lastSourceClip = null;
            return;
        }
        String sourceName = source.getName();
        AnimationClip target = resolveClip(sourceName);
        if (target == null) {
            if (POSE_HANDLER.isPlaying()) {
                POSE_HANDLER.stopImmediateWithBlendOut();
            }
            lastSourceClip = null;
        } else if (lastSourceClip == null || !lastSourceClip.equals(sourceName) || POSE_HANDLER.getCurrentClip() != target || !POSE_HANDLER.isPlaying()) {
            class_1306 arm = PunchyAnimationManager.POSE_HANDLER.getActiveArm();
            POSE_HANDLER.setActiveArm(arm);
            POSE_HANDLER.setLooping(PunchyAnimationManager.POSE_HANDLER.isForceLooping());
            float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(source);
            boolean hold = target.isHoldOnLastFrame();
            POSE_HANDLER.play(target, speedMultiplier, true, hold);
            lastSourceClip = sourceName;
        }
    }

    public static void playManual(String name) {
        AnimationClip target = resolveClip(name);
        if (target == null) {
            return;
        }
        manualClipName = target.getName();
        POSE_HANDLER.setActiveArm(null);
        POSE_HANDLER.setLooping(target.isLoop());
        POSE_HANDLER.play(target, 1.0f, true, target.isHoldOnLastFrame());
    }

    private static boolean isManualPlaying() {
        if (manualClipName == null) {
            return false;
        }
        AnimationClip current = POSE_HANDLER.getCurrentClip();
        return POSE_HANDLER.isPlaying() && current != null && manualClipName.equals(current.getName());
    }
}
