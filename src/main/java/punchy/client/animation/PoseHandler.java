package punchy.client.animation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_630;
import net.minecraft.class_746;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import punchy.client.access.TransformablePart;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.data.BoneTrack;
import punchy.client.animation.data.Keyframe;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.MovementStateMachine;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PoseHandler.class */
public class PoseHandler {
    private static final boolean DEBUG_COMPAT_IDLE = false;
    private static final float BLEND_OUT_SECONDS = 0.28f;
    private static final float BLEND_IN_SECONDS = 0.2f;
    private static final float BLEND_OUT_SLOW = 1.8f;
    private static final float BLEND_IN_SLOW = 1.0f;
    private static final float ANIM_POS_X_OFFSET = 0.0f;
    private static final float ANIM_POS_Z_OFFSET = 0.0f;
    private static final String ANIM_SPEED_PREFIX = "anim_speed_";
    private static final float MIN_TIMELINE_SPEED = 0.5f;
    private static final float MAX_TIMELINE_SPEED = 5.5f;
    private static final float DEFAULT_LOOP_SPEED_MAX = 2.0f;
    private static final float DEFAULT_LOOP_SPEED_RAMP = 0.6f;
    private static final float LOOP_SPEED_MAX_LIMIT = 1.0f;
    private static final float LOOP_SPEED_RAMP_LIMIT = 1.0f;
    private static final float ARM_TUNING_SUPPRESS_IN_SECONDS = 0.18f;
    private static final float ARM_TUNING_SUPPRESS_OUT_SECONDS = 0.48f;
    private static final float REBASE_RETURN_SPEED_MULTIPLIER = 1.7f;
    private static final String HIDE_OFFHAND_IF_EMPTY_EFFECT = "hide_offhand_if_empty";
    private static final String SHOW_OFFHAND_IF_EMPTY_EFFECT = "show_offhand_if_empty";
    private static final String START_WITH_OFFHAND_EFFECT = "start_here_if_off_hand_item";
    private static final String START_WITH_OFFHAND_EFFECT_ALIAS = "start_if_offhand_item";
    private static final String STOP_WITH_OFFHAND_EFFECT = "stop_if_offhand_item";
    private AnimationClip currentClip;
    private float currentTime;
    private float prevTime;
    private float expressionTime;
    private float prevExpressionTime;
    private boolean isPlaying;
    private float blendRemaining;
    private float prevBlendRemaining;
    private float blendInRemaining;
    private float prevBlendInRemaining;
    private boolean rebaseActive;
    private boolean rebaseReturnToOrigin;
    private float rebaseStartTime;
    private Float nextRebaseReturnSpeedMultiplier;
    private class_1306 nextRebaseSourceArm;
    private boolean nextRebaseUseBothArms;
    private boolean blendInFromActive;
    private boolean blendInDynamic;
    private AnimationClip blendInClip;
    private float blendInTime;
    private float blendInPrevTime;
    private class_1306 activeArm;
    private class_1306 clipArm;
    private boolean clipArmFilter;
    private class_1306 blendArm;
    private boolean blendArmFilter;
    private Float nextBlendInDuration;
    private Float nextBlendOutDuration;
    private Float nextBlendOutSlow;
    private BlendInEasing nextBlendInEasing;
    private boolean stopAfterCurrent;
    private boolean forceLoop;
    private boolean preBlendInActive;
    private boolean holdOnLastFrame;
    private boolean holdReleasePending;
    private boolean hasLooped;
    private boolean skipClearOnStop;
    private class_1306 pendingClearArm;
    private class_1306 pendingSkipArm;
    private class_1306 forcedSkipArm;
    private class_1306 nextForcedSkipArm;
    private boolean nextForcedSkipArmSet;
    private float leftArmBlendRemaining;
    private float leftArmBlendPrevRemaining;
    private float rightArmBlendRemaining;
    private float rightArmBlendPrevRemaining;
    private int tickCounter;
    private boolean frozen;
    private boolean swapTranslationYZ;
    private boolean javaBlock;
    private boolean mirrorX;
    private boolean ignoreDualHanded;
    private boolean paused;
    private class_1306 pendingOverlapSourceArm;
    private AnimationClip overlapClip;
    private float overlapTime;
    private float overlapPrevTime;
    private float overlapBlendRemaining;
    private float overlapPrevBlendRemaining;
    private class_1306 overlapArm;
    private boolean overlapPaused;
    private boolean overlapCompatIdle;
    private float overlapBlendInRemaining;
    private float overlapPrevBlendInRemaining;
    private float armTuningSuppressAlpha;
    private float prevArmTuningSuppressAlpha;
    private boolean latchedKeepTriggerArmTuning;
    private PunchyTuningConfig.CustomFlags overlapCustomFlags;
    private class_1306 latchedTriggerArm;
    private static final Logger LOGGER = LoggerFactory.getLogger("Punchy/PoseHandler");
    private static final Pattern ANIM_SPEED_VALUE_PATTERN = Pattern.compile("^[-+]?\\d+(?:[\\.,]\\d+)?");
    private final Map<String, Sample> blendFrom = new HashMap();
    private final Map<String, Sample> blendInFrom = new HashMap();
    private final Map<String, Sample> rebaseFrom = new HashMap();
    private final Map<String, Sample> rebaseOrigin = new HashMap();
    private float rebaseReturnSpeedMultiplier = REBASE_RETURN_SPEED_MULTIPLIER;
    private float blendInSpeedMultiplier = 1.0f;
    private float blendOutDuration = BLEND_OUT_SECONDS;
    private float blendInDuration = BLEND_IN_SECONDS;
    private float blendOutSlow = BLEND_OUT_SLOW;
    private BlendInEasing blendInEasing = BlendInEasing.BACK;
    private float speedMultiplier = 1.0f;
    private float timelineSpeedScale = 1.0f;
    private int pendingClearArmTick = -1;
    private int pendingSkipArmTick = -1;
    private float loopSpeedBoost = 1.0f;
    private float loopSpeedMax = DEFAULT_LOOP_SPEED_MAX;
    private float loopSpeedRamp = DEFAULT_LOOP_SPEED_RAMP;
    private float overlapSpeedMultiplier = 1.0f;
    private final Map<String, Sample> overlapBlendFrom = new HashMap();
    private float overlapBlendInDuration = BLEND_IN_SECONDS;
    private String lastCompatIdleSnapshot = "";

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PoseHandler$BlendInEasing.class */
    public enum BlendInEasing {
        BACK,
        SMOOTH,
        STRONG_OUT_BACK
    }

    public PunchyTuningConfig.CustomFlags getOverlapCustomFlags() {
        return this.overlapCustomFlags;
    }

    public void setSecondaryClipAtTimeWithFlags(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime, PunchyTuningConfig.CustomFlags flags) {
        setSecondaryClipAtTime(clip, arm, speedMultiplier, startTime);
        this.overlapCustomFlags = flags;
    }

    public void setSecondaryClipAtTimeWithBlendFromCurrentAndFlags(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime, boolean armPoseOnlyBlend, float blendInDuration, PunchyTuningConfig.CustomFlags flags) {
        setSecondaryClipAtTimeWithBlendFromCurrent(clip, arm, speedMultiplier, startTime, armPoseOnlyBlend, blendInDuration);
        this.overlapCustomFlags = flags;
    }

    public void play(AnimationClip clip) {
        play(clip, 1.0f);
    }

    public void play(AnimationClip clip, float speedMultiplier) {
        play(clip, speedMultiplier, false);
    }

    public void play(AnimationClip clip, float speedMultiplier, boolean skipBlendIn) {
        play(clip, speedMultiplier, skipBlendIn, false);
    }

    public void play(AnimationClip clip, float speedMultiplier, boolean skipBlendIn, boolean holdOnLastFrame) {
        playInternal(clip, speedMultiplier, skipBlendIn, holdOnLastFrame, false);
    }

    public void playAtTime(AnimationClip clip, float speedMultiplier, boolean skipBlendIn, boolean holdOnLastFrame, float startTime) {
        float time;
        playInternal(clip, speedMultiplier, skipBlendIn, holdOnLastFrame, false);
        if (clip == null) {
            return;
        }
        float length = clip.getLength();
        if (length > 0.0f) {
            if (clip.isLoop()) {
                time = startTime % length;
                if (time < 0.0f) {
                    time += length;
                }
            } else {
                time = class_3532.method_15363(startTime, 0.0f, length);
            }
        } else {
            time = DEBUG_COMPAT_IDLE;
        }
        this.currentTime = time;
        this.prevTime = time;
        this.expressionTime = startTime;
        this.prevExpressionTime = startTime;
        this.preBlendInActive = false;
    }

    public void playAtTimeWithBlendFromCurrent(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame, float startTime) {
        float time;
        playWithBlendFromCurrentDynamic(clip, speedMultiplier, holdOnLastFrame);
        if (clip == null) {
            return;
        }
        float length = clip.getLength();
        if (length > 0.0f) {
            if (clip.isLoop()) {
                time = startTime % length;
                if (time < 0.0f) {
                    time += length;
                }
            } else {
                time = class_3532.method_15363(startTime, 0.0f, length);
            }
        } else {
            time = DEBUG_COMPAT_IDLE;
        }
        this.currentTime = time;
        this.prevTime = time;
        this.expressionTime = startTime;
        this.prevExpressionTime = startTime;
        this.preBlendInActive = false;
    }

    public void playAtTimeRebasedFromCurrent(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame, float startTime) {
        playAtTimeRebasedFromCurrentInternal(clip, speedMultiplier, holdOnLastFrame, startTime, false);
    }

    public void playAtTimeRebasedFromCurrentReturnToOrigin(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame, float startTime) {
        playAtTimeRebasedFromCurrentInternal(clip, speedMultiplier, holdOnLastFrame, startTime, true);
    }

    public void playAtTimeRebasedFromHandlerReturnToOrigin(PoseHandler sourceHandler, AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame, float startTime) {
        float fMax;
        if (sourceHandler == null || sourceHandler == this) {
            playAtTimeRebasedFromCurrentReturnToOrigin(clip, speedMultiplier, holdOnLastFrame, startTime);
            return;
        }
        AnimationClip previousClip = sourceHandler.currentClip;
        Map<String, Sample> previousRebaseFrom = new HashMap<>(sourceHandler.rebaseFrom);
        Map<String, Sample> previousRebaseOrigin = new HashMap<>(sourceHandler.rebaseOrigin);
        boolean previousRebaseActive = sourceHandler.rebaseActive;
        boolean previousRebaseReturnToOrigin = sourceHandler.rebaseReturnToOrigin;
        float previousRebaseStartTime = sourceHandler.rebaseStartTime;
        float previousRebaseReturnSpeedMultiplier = sourceHandler.rebaseReturnSpeedMultiplier;
        class_1306 previousArm = sourceHandler.clipArmFilter ? sourceHandler.clipArm : sourceHandler.activeArm;
        float previousTime = sourceHandler.currentTime;
        float previousExpressionTime = sourceHandler.expressionTime;
        boolean previousActive = previousClip != null && (sourceHandler.isPlaying || sourceHandler.blendRemaining > 0.0f);
        playAtTime(clip, speedMultiplier, true, holdOnLastFrame, startTime);
        this.rebaseFrom.clear();
        this.rebaseOrigin.clear();
        this.rebaseActive = false;
        this.rebaseReturnToOrigin = false;
        this.rebaseStartTime = startTime;
        if (this.nextRebaseReturnSpeedMultiplier != null) {
            fMax = Math.max(0.01f, this.nextRebaseReturnSpeedMultiplier.floatValue());
        } else {
            fMax = REBASE_RETURN_SPEED_MULTIPLIER;
        }
        this.rebaseReturnSpeedMultiplier = fMax;
        this.nextRebaseReturnSpeedMultiplier = null;
        if (!previousActive || clip == null) {
            return;
        }
        for (Map.Entry<String, BoneTrack> entry : clip.getTracks().entrySet()) {
            String boneName = entry.getKey();
            BoneTrack targetTrack = entry.getValue();
            if (targetTrack != null) {
                class_1306 boneArm = resolveBoneArm(boneName);
                if (previousArm == null || boneArm == null || boneArm == previousArm) {
                    Sample from = sourceHandler.sampleVisibleSourceForRebase(boneName, boneArm, previousClip, previousTime, previousExpressionTime, previousRebaseFrom, previousRebaseOrigin, previousRebaseActive, previousRebaseReturnToOrigin, previousRebaseStartTime, previousRebaseReturnSpeedMultiplier);
                    Sample origin = sampleTrack(targetTrack, startTime, startTime, clip);
                    this.rebaseFrom.put(boneName, from);
                    this.rebaseOrigin.put(boneName, origin);
                }
            }
        }
        this.rebaseActive = !this.rebaseFrom.isEmpty();
        this.rebaseReturnToOrigin = this.rebaseActive;
    }

    private void playAtTimeRebasedFromCurrentInternal(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame, float startTime, boolean returnToOrigin) {
        float fMax;
        AnimationClip previousClip = this.currentClip;
        Map<String, Sample> previousRebaseFrom = new HashMap<>(this.rebaseFrom);
        Map<String, Sample> previousRebaseOrigin = new HashMap<>(this.rebaseOrigin);
        boolean previousRebaseActive = this.rebaseActive;
        boolean previousRebaseReturnToOrigin = this.rebaseReturnToOrigin;
        float previousRebaseStartTime = this.rebaseStartTime;
        float previousRebaseReturnSpeedMultiplier = this.rebaseReturnSpeedMultiplier;
        class_1306 previousArm = this.nextRebaseSourceArm;
        boolean forceBothArms = this.nextRebaseUseBothArms;
        this.nextRebaseSourceArm = null;
        this.nextRebaseUseBothArms = false;
        if (forceBothArms) {
            previousArm = DEBUG_COMPAT_IDLE;
        }
        if (previousArm == null) {
            if (previousClip != null) {
                previousArm = this.clipArmFilter ? this.clipArm : this.activeArm;
            } else {
                previousArm = this.activeArm != null ? this.activeArm : this.clipArmFilter ? this.clipArm : null;
            }
            if (forceBothArms) {
                previousArm = DEBUG_COMPAT_IDLE;
            }
        }
        float previousTime = this.currentTime;
        float previousExpressionTime = this.expressionTime;
        boolean previousActive = previousClip != null && (this.isPlaying || this.blendRemaining > 0.0f);
        if (!previousActive && this.overlapCompatIdle && this.overlapClip != null && this.overlapArm != null) {
            previousClip = this.overlapClip;
            previousArm = this.overlapArm;
            previousTime = this.overlapTime;
            previousExpressionTime = this.overlapTime;
            previousRebaseFrom.clear();
            previousRebaseOrigin.clear();
            previousRebaseActive = DEBUG_COMPAT_IDLE;
            previousRebaseReturnToOrigin = DEBUG_COMPAT_IDLE;
            previousRebaseStartTime = DEBUG_COMPAT_IDLE;
            previousRebaseReturnSpeedMultiplier = 1.7f;
            previousActive = true;
        }
        playAtTime(clip, speedMultiplier, true, holdOnLastFrame, startTime);
        this.rebaseFrom.clear();
        this.rebaseOrigin.clear();
        this.rebaseActive = false;
        this.rebaseReturnToOrigin = false;
        this.rebaseStartTime = startTime;
        if (this.nextRebaseReturnSpeedMultiplier != null) {
            fMax = Math.max(0.01f, this.nextRebaseReturnSpeedMultiplier.floatValue());
        } else {
            fMax = REBASE_RETURN_SPEED_MULTIPLIER;
        }
        this.rebaseReturnSpeedMultiplier = fMax;
        this.nextRebaseReturnSpeedMultiplier = null;
        if (!previousActive || clip == null || previousClip == clip) {
            return;
        }
        for (Map.Entry<String, BoneTrack> entry : clip.getTracks().entrySet()) {
            String boneName = entry.getKey();
            BoneTrack targetTrack = entry.getValue();
            if (targetTrack != null) {
                class_1306 boneArm = resolveBoneArm(boneName);
                if (previousArm == null || boneArm == null || boneArm == previousArm) {
                    Sample from = sampleVisibleSourceForRebase(boneName, boneArm, previousClip, previousTime, previousExpressionTime, previousRebaseFrom, previousRebaseOrigin, previousRebaseActive, previousRebaseReturnToOrigin, previousRebaseStartTime, previousRebaseReturnSpeedMultiplier);
                    Sample origin = sampleTrack(targetTrack, startTime, startTime, clip);
                    this.rebaseFrom.put(boneName, from);
                    this.rebaseOrigin.put(boneName, origin);
                }
            }
        }
        this.rebaseActive = !this.rebaseFrom.isEmpty();
        this.rebaseReturnToOrigin = this.rebaseActive && returnToOrigin;
    }

    private Sample sampleVisibleSourceForRebase(String boneName, class_1306 boneArm, AnimationClip previousClip, float previousTime, float previousExpressionTime, Map<String, Sample> previousRebaseFrom, Map<String, Sample> previousRebaseOrigin, boolean previousRebaseActive, boolean previousRebaseReturnToOrigin, float previousRebaseStartTime, float previousRebaseReturnSpeedMultiplier) {
        BoneTrack overlapTrack;
        if (this.overlapCompatIdle && this.overlapClip != null && this.overlapArm != null && boneArm != null && boneArm == this.overlapArm && !isItemGripOrHeldItemBone(boneName) && (overlapTrack = this.overlapClip.getTracks().get(boneName)) != null) {
            return sampleTrack(overlapTrack, this.overlapTime, this.overlapTime, this.overlapClip);
        }
        if (previousClip == null) {
            return Sample.identity();
        }
        BoneTrack previousTrack = previousClip.getTracks().get(boneName);
        if (previousTrack == null) {
            return Sample.identity();
        }
        return samplePreviousVisibleTrack(previousTrack, boneName, previousTime, previousExpressionTime, previousClip, previousRebaseFrom, previousRebaseOrigin, previousRebaseActive, previousRebaseReturnToOrigin, previousRebaseStartTime, previousRebaseReturnSpeedMultiplier);
    }

    private Sample samplePreviousVisibleTrack(BoneTrack track, String boneName, float time, float evalTime, AnimationClip clip, Map<String, Sample> capturedRebaseFrom, Map<String, Sample> capturedRebaseOrigin, boolean capturedRebaseActive, boolean capturedRebaseReturnToOrigin, float capturedRebaseStartTime, float capturedRebaseReturnSpeedMultiplier) {
        Sample sample = sampleTrack(track, time, evalTime, clip);
        if (!capturedRebaseActive) {
            return sample;
        }
        Sample from = capturedRebaseFrom.get(boneName);
        Sample origin = capturedRebaseOrigin.get(boneName);
        if (from == null || origin == null) {
            return sample;
        }
        Sample rebased = rebaseSample(from, origin, sample);
        if (!capturedRebaseReturnToOrigin || clip == null) {
            return rebased;
        }
        float progress = resolveCapturedRebaseReturnProgress(clip, evalTime, capturedRebaseStartTime, capturedRebaseReturnSpeedMultiplier);
        if (progress <= 0.0f) {
            return rebased;
        }
        if (progress >= 1.0f) {
            return sample;
        }
        return blendBetweenSamples(rebased, sample, progress);
    }

    private float resolveCapturedRebaseReturnProgress(AnimationClip clip, float evalTime, float startTime, float returnSpeedMultiplier) {
        if (clip == null) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= 0.0f) {
            return 1.0f;
        }
        float start = Math.max(0.0f, Math.min(startTime, length));
        if (evalTime <= start) {
            return 0.0f;
        }
        float duration = Math.max(1.0E-4f, length - start);
        float rawProgress = class_3532.method_15363(((evalTime - start) / duration) * Math.max(0.01f, returnSpeedMultiplier), 0.0f, 1.0f);
        return easeOutCubic(rawProgress);
    }

    public void playWithBlendFromCurrentDynamic(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame) {
        AnimationClip previousClip = this.currentClip;
        float previousTime = this.currentTime;
        float previousPrevTime = this.prevTime;
        float previousSpeedMultiplier = this.speedMultiplier;
        boolean previousPlaying = previousClip != null && (this.isPlaying || this.blendRemaining > 0.0f);
        playInternal(clip, speedMultiplier, false, holdOnLastFrame, true);
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        if (!previousPlaying || this.currentClip != clip) {
            this.blendInDynamic = false;
            this.blendInClip = null;
            this.blendInTime = 0.0f;
            this.blendInPrevTime = 0.0f;
            this.blendInSpeedMultiplier = 1.0f;
            return;
        }
        this.blendInDynamic = true;
        this.blendInClip = previousClip;
        this.blendInTime = previousTime;
        this.blendInPrevTime = previousPrevTime;
        this.blendInSpeedMultiplier = previousSpeedMultiplier;
        if (this.blendInRemaining <= 0.0f) {
            this.blendInDynamic = false;
            this.blendInClip = null;
            this.blendInTime = 0.0f;
            this.blendInPrevTime = 0.0f;
            this.blendInSpeedMultiplier = 1.0f;
        }
    }

    public void playWithBlendFromCurrent(AnimationClip clip, float speedMultiplier, boolean holdOnLastFrame) {
        if (shouldForceBlendInFromVisibleSource()) {
            captureVisibleArmSamplesForBlendIn(resolveVisibleSourceArm());
        } else if (this.currentClip != null && (this.isPlaying || this.blendRemaining > 0.0f)) {
            this.blendInFrom.clear();
            for (Map.Entry<String, BoneTrack> entry : this.currentClip.getTracks().entrySet()) {
                Sample sample = sampleTrack(entry.getValue(), this.currentTime, this.currentClip);
                this.blendInFrom.put(entry.getKey(), sample);
            }
            this.blendInFromActive = !this.blendInFrom.isEmpty();
        } else {
            this.blendInFrom.clear();
            this.blendInFromActive = false;
        }
        playInternal(clip, speedMultiplier, false, holdOnLastFrame, true);
    }

    private void playInternal(AnimationClip clip, float speedMultiplier, boolean skipBlendIn, boolean holdOnLastFrame, boolean forceBlendIn) {
        class_1306 class_1306Var;
        if (PunchyAnimationManager.shouldBlockClip(clip, this.activeArm)) {
            return;
        }
        if (!forceBlendIn && shouldForceBlendInFromVisibleSource()) {
            captureVisibleArmSamplesForBlendIn(resolveVisibleSourceArm());
            forceBlendIn = this.blendInFromActive;
        }
        boolean preserveOverlap = DEBUG_COMPAT_IDLE;
        class_1306 preserveOverlapArm = DEBUG_COMPAT_IDLE;
        if (!preserveOverlap) {
            if (this.pendingOverlapSourceArm != null) {
                class_1306Var = this.pendingOverlapSourceArm;
            } else {
                class_1306Var = this.clipArmFilter ? this.clipArm : this.activeArm;
            }
            class_1306 previousArm = class_1306Var;
            class_1306 targetArm = this.activeArm;
            boolean previousDual = this.isPlaying && this.currentClip != null && isDualHandedActive(this.currentClip, this.currentTime);
            boolean incomingDual = clip != null && isDualHandedActive(clip, 0.0f);
            if (previousDual && !incomingDual && targetArm != null) {
                preserveOverlap = true;
                preserveOverlapArm = targetArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
            }
            if (!preserveOverlap) {
                preserveOverlap = (!this.isPlaying || this.currentClip == null || previousArm == null || targetArm == null || clip == null || previousArm == targetArm || isDualHandedActive(this.currentClip, this.currentTime)) ? false : true;
                if (preserveOverlap) {
                    preserveOverlapArm = previousArm;
                }
            }
            if (preserveOverlap && preserveOverlapArm != null) {
                preserveOverlapClip(preserveOverlapArm);
            }
        }
        this.pendingOverlapSourceArm = null;
        boolean effectiveSkip = skipBlendIn || (!forceBlendIn && (this.isPlaying || this.blendRemaining > 0.0f));
        this.currentClip = clip;
        if (this.nextForcedSkipArmSet) {
            this.forcedSkipArm = this.nextForcedSkipArm;
            this.nextForcedSkipArm = null;
            this.nextForcedSkipArmSet = false;
        } else {
            this.forcedSkipArm = null;
        }
        float initialTime = resolveConditionalDualStartTime(clip);
        this.currentTime = initialTime;
        this.prevTime = initialTime;
        this.expressionTime = initialTime;
        this.prevExpressionTime = initialTime;
        this.isPlaying = true;
        this.hasLooped = false;
        this.blendRemaining = 0.0f;
        this.prevBlendRemaining = 0.0f;
        if (this.nextBlendInDuration != null) {
            this.blendInDuration = this.nextBlendInDuration.floatValue();
            this.nextBlendInDuration = null;
        } else {
            this.blendInDuration = BLEND_IN_SECONDS;
        }
        if (this.nextBlendInEasing != null) {
            this.blendInEasing = this.nextBlendInEasing;
            this.nextBlendInEasing = null;
        } else {
            this.blendInEasing = BlendInEasing.BACK;
        }
        this.blendInRemaining = effectiveSkip ? 0.0f : this.blendInDuration;
        this.prevBlendInRemaining = effectiveSkip ? 0.0f : this.blendInDuration;
        this.blendFrom.clear();
        this.blendArm = null;
        this.blendArmFilter = false;
        if (!forceBlendIn) {
            this.blendInFrom.clear();
            this.blendInFromActive = false;
        }
        this.rebaseFrom.clear();
        this.rebaseOrigin.clear();
        this.rebaseActive = false;
        this.rebaseReturnToOrigin = false;
        this.rebaseStartTime = 0.0f;
        this.blendInDynamic = false;
        this.blendInClip = null;
        this.blendInTime = 0.0f;
        this.blendInPrevTime = 0.0f;
        this.blendInSpeedMultiplier = 1.0f;
        this.stopAfterCurrent = false;
        this.forceLoop = false;
        this.speedMultiplier = Math.max(0.01f, speedMultiplier);
        this.preBlendInActive = !effectiveSkip && this.blendInRemaining > 0.0f;
        this.holdOnLastFrame = holdOnLastFrame;
        this.holdReleasePending = false;
        this.paused = false;
        this.clipArm = this.activeArm;
        this.clipArmFilter = this.activeArm != null;
        latchCompatKeepTriggerArmState();
        clearArmBlend();
        if (clip != null && this.activeArm != null && !isDualHandedActive(clip, 0.0f) && !isEquipClip(clip)) {
            MovementStateMachine.get().triggerIdleBalance(this.activeArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182);
        }
        if (clip != null && this.activeArm != null && !isEquipClip(clip) && !isIdleLikeClip(clip)) {
            MovementStateMachine.get().triggerActionAnimation(this.activeArm);
        }
    }

    public void setActiveArm(class_1306 arm) {
        if (this.isPlaying && this.currentClip != null && arm != null && this.activeArm != null && arm != this.activeArm && !isDualHandedActive(this.currentClip, this.currentTime)) {
            this.pendingOverlapSourceArm = this.clipArmFilter ? this.clipArm : this.activeArm;
        }
        this.activeArm = arm;
        if (!this.isPlaying) {
            this.clipArm = arm;
            this.clipArmFilter = arm != null;
        }
    }

    public void prepareOverlapBlendOut(class_1306 newArm, AnimationClip newClip) {
        if (!this.isPlaying || this.currentClip == null || newArm == null || newClip == null || isDualHandedActive(this.currentClip, this.currentTime)) {
            return;
        }
        class_1306 previousArm = this.clipArmFilter ? this.clipArm : this.activeArm;
        if (previousArm == null || previousArm == newArm) {
            return;
        }
        this.pendingOverlapSourceArm = previousArm;
    }

    public void requestClearArm(class_1306 arm) {
        this.pendingClearArm = arm;
        this.pendingClearArmTick = this.tickCounter;
    }

    public void requestSkipArm(class_1306 arm) {
        this.pendingSkipArm = arm;
        this.pendingSkipArmTick = this.tickCounter;
    }

    public void clearRequestedArmFilters() {
        this.pendingClearArm = null;
        this.pendingClearArmTick = -1;
        this.pendingSkipArm = null;
        this.pendingSkipArmTick = -1;
        this.forcedSkipArm = null;
    }

    public void setForcedSkipArmForNextPlay(class_1306 arm) {
        this.nextForcedSkipArm = arm;
        this.nextForcedSkipArmSet = true;
    }

    public void requestBlendInArm(class_1306 arm) {
        if (arm == null || this.currentClip == null || !this.isPlaying) {
            return;
        }
        setArmBlendRemaining(arm, BLEND_IN_SECONDS);
        setArmBlendPrevRemaining(arm, BLEND_IN_SECONDS);
    }

    public void setSecondaryClip(AnimationClip clip, class_1306 arm, float speedMultiplier) {
        setSecondaryClipAtTime(clip, arm, speedMultiplier, 0.0f);
    }

    public void setSecondaryClipAtTime(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime) {
        if (clip == null || arm == null) {
            clearSecondaryClip();
            return;
        }
        this.overlapClip = clip;
        this.overlapArm = arm;
        float clampedStartTime = class_3532.method_15363(startTime, 0.0f, clip.getLength());
        this.overlapTime = clampedStartTime;
        this.overlapPrevTime = clampedStartTime;
        this.overlapSpeedMultiplier = Math.max(0.01f, speedMultiplier);
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapPaused = false;
        this.overlapCompatIdle = false;
        this.overlapBlendFrom.clear();
        this.overlapBlendInRemaining = 0.0f;
        this.overlapPrevBlendInRemaining = 0.0f;
        this.overlapBlendInDuration = BLEND_IN_SECONDS;
        debugCompatIdle("setSecondary", "clip=" + clipName(clip) + " arm=" + String.valueOf(arm) + " start=" + clampedStartTime + " compat=" + this.overlapCompatIdle);
    }

    public void setSecondaryClipAtTimeWithBlendFromCurrent(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime) {
        setSecondaryClipAtTimeWithBlendFromCurrent(clip, arm, speedMultiplier, startTime, false, BLEND_IN_SECONDS);
    }

    public void setSecondaryClipAtTimeWithBlendFromCurrent(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime, boolean armPoseOnlyBlend) {
        setSecondaryClipAtTimeWithBlendFromCurrent(clip, arm, speedMultiplier, startTime, armPoseOnlyBlend, BLEND_IN_SECONDS);
    }

    public void setSecondaryClipAtTimeWithBlendFromCurrent(AnimationClip clip, class_1306 arm, float speedMultiplier, float startTime, boolean armPoseOnlyBlend, float blendInDuration) {
        setSecondaryClipAtTime(clip, arm, speedMultiplier, startTime);
        if (clip == null || arm == null) {
            return;
        }
        captureVisibleSamplesForTargetClip(this.overlapBlendFrom, arm, clip, armPoseOnlyBlend);
        if (!this.overlapBlendFrom.isEmpty()) {
            float duration = Math.max(1.0E-4f, blendInDuration);
            this.overlapBlendInDuration = duration;
            this.overlapBlendInRemaining = duration;
            this.overlapPrevBlendInRemaining = duration;
        } else {
            this.overlapBlendInDuration = BLEND_IN_SECONDS;
        }
        debugCompatIdle("setSecondaryBlendFromCurrent", "clip=" + clipName(clip) + " arm=" + String.valueOf(arm) + " armOnly=" + armPoseOnlyBlend + " blendFromSize=" + this.overlapBlendFrom.size());
    }

    public void clearSecondaryClipWithBlendOut() {
        if (this.overlapClip == null || this.overlapArm == null) {
            clearSecondaryClip();
            return;
        }
        this.overlapPaused = true;
        this.overlapPrevTime = this.overlapTime;
        this.overlapBlendFrom.clear();
        this.overlapBlendInRemaining = 0.0f;
        this.overlapPrevBlendInRemaining = 0.0f;
        this.overlapBlendInDuration = BLEND_IN_SECONDS;
        this.overlapPrevBlendRemaining = BLEND_OUT_SECONDS;
        this.overlapBlendRemaining = BLEND_OUT_SECONDS;
        debugCompatIdle("clearSecondaryBlendOut", "clip=" + clipName(this.overlapClip) + " arm=" + String.valueOf(this.overlapArm) + " compat=" + this.overlapCompatIdle);
    }

    public void setSecondaryPaused(boolean paused) {
        this.overlapPaused = paused;
    }

    public void setSecondaryCompatIdle(boolean compatIdle) {
        this.overlapCompatIdle = (!compatIdle || this.overlapClip == null || this.overlapArm == null) ? false : true;
        debugCompatIdle("setCompatIdle", "clip=" + clipName(this.overlapClip) + " arm=" + String.valueOf(this.overlapArm) + " compat=" + this.overlapCompatIdle);
    }

    public boolean isSecondaryCompatIdle() {
        return (!this.overlapCompatIdle || this.overlapClip == null || this.overlapArm == null) ? false : true;
    }

    public void clearSecondaryClip() {
        debugCompatIdle("clearSecondary", "clip=" + clipName(this.overlapClip) + " arm=" + String.valueOf(this.overlapArm) + " compat=" + this.overlapCompatIdle);
        this.overlapClip = null;
        this.overlapTime = 0.0f;
        this.overlapPrevTime = 0.0f;
        this.overlapSpeedMultiplier = 1.0f;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = null;
        this.overlapPaused = false;
        this.overlapCompatIdle = false;
        this.overlapBlendFrom.clear();
        this.overlapBlendInRemaining = 0.0f;
        this.overlapPrevBlendInRemaining = 0.0f;
        this.overlapBlendInDuration = BLEND_IN_SECONDS;
        this.pendingOverlapSourceArm = null;
        this.latchedKeepTriggerArmTuning = false;
        this.latchedTriggerArm = null;
        this.overlapCustomFlags = null;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        if (frozen) {
            this.currentTime = 0.0f;
            this.prevTime = 0.0f;
            this.expressionTime = 0.0f;
            this.prevExpressionTime = 0.0f;
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isBlendOutActive() {
        return this.blendRemaining > 0.0f;
    }

    public float getBlendOutProgress(float partialTicks) {
        if (this.blendRemaining <= 0.0f) {
            return 1.0f;
        }
        float blendRem = class_3532.method_16439(partialTicks, this.prevBlendRemaining, this.blendRemaining);
        float t = 1.0f - (blendRem / Math.max(1.0E-4f, this.blendOutDuration));
        return class_3532.method_15363(Easing.easeOutBack(t), 0.0f, 1.0f);
    }

    public void setRebaseSourceArmForNextPlay(class_1306 arm) {
        this.nextRebaseSourceArm = arm;
    }

    public void setRebaseUseBothArmsForNextPlay(boolean useBothArms) {
        this.nextRebaseUseBothArms = useBothArms;
    }

    public boolean isRebaseReturnToOriginActive() {
        return this.rebaseActive && this.rebaseReturnToOrigin;
    }

    public float getRebaseReturnToOriginProgress(float partialTicks) {
        if (!this.rebaseActive || !this.rebaseReturnToOrigin || this.currentClip == null) {
            return 0.0f;
        }
        float evalTime = class_3532.method_16439(partialTicks, this.prevExpressionTime, this.expressionTime);
        return resolveRebaseReturnProgress(this.currentClip, evalTime);
    }

    public void resetImmediate() {
        stop();
        this.currentClip = null;
        this.blendRemaining = 0.0f;
        this.prevBlendRemaining = 0.0f;
        this.blendInRemaining = 0.0f;
        this.prevBlendInRemaining = 0.0f;
        this.blendFrom.clear();
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        this.rebaseReturnToOrigin = false;
        this.rebaseStartTime = 0.0f;
        this.blendInDynamic = false;
        this.blendInClip = null;
        this.blendInTime = 0.0f;
        this.blendInPrevTime = 0.0f;
        this.blendInSpeedMultiplier = 1.0f;
        this.overlapClip = null;
        this.overlapTime = 0.0f;
        this.overlapPrevTime = 0.0f;
        this.overlapSpeedMultiplier = 1.0f;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = null;
        this.overlapPaused = false;
        this.pendingOverlapSourceArm = null;
    }

    public void setSwapTranslationYZ(boolean swapTranslationYZ) {
        this.swapTranslationYZ = swapTranslationYZ;
    }

    public void setJavaBlock(boolean javaBlock) {
        this.javaBlock = javaBlock;
    }

    public void setMirrorX(boolean mirrorX) {
        this.mirrorX = mirrorX;
    }

    public void setLooping(boolean loop) {
        this.forceLoop = loop;
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = Math.max(0.01f, speedMultiplier);
    }

    public void setTimelineSpeedScale(float timelineSpeedScale) {
        this.timelineSpeedScale = Math.max(0.01f, timelineSpeedScale);
    }

    public void setIgnoreDualHanded(boolean ignore) {
        this.ignoreDualHanded = ignore;
    }

    public boolean isCurrentDualHandedActive() {
        if (!this.isPlaying || this.currentClip == null) {
            return false;
        }
        return isDualHandedActive(this.currentTime);
    }

    public void setLoopSpeedParams(Float max, Float ramp) {
        float resolvedMax = max == null ? DEFAULT_LOOP_SPEED_MAX : max.floatValue();
        float resolvedRamp = ramp == null ? DEFAULT_LOOP_SPEED_RAMP : ramp.floatValue();
        this.loopSpeedMax = class_3532.method_15363(resolvedMax, 1.0f, 1.0f);
        this.loopSpeedRamp = class_3532.method_15363(resolvedRamp, 0.0f, 1.0f);
    }

    public void setBlendInDurationNext(float seconds) {
        this.nextBlendInDuration = Float.valueOf(Math.max(0.0f, seconds));
    }

    public void setBlendInEasingNext(BlendInEasing easing) {
        this.nextBlendInEasing = easing == null ? BlendInEasing.BACK : easing;
    }

    public void setBlendOutDurationNext(float seconds) {
        this.nextBlendOutDuration = Float.valueOf(Math.max(0.0f, seconds));
    }

    public void setBlendOutSlowNext(float slow) {
        this.nextBlendOutSlow = Float.valueOf(Math.max(0.01f, slow));
    }

    public void setRebaseReturnSpeedNext(float multiplier) {
        this.nextRebaseReturnSpeedMultiplier = Float.valueOf(Math.max(0.01f, multiplier));
    }

    public void resetLoopSpeedParams() {
        setLoopSpeedParams(null, null);
    }

    public void requestStopAfterCurrent() {
        this.stopAfterCurrent = true;
        this.forceLoop = false;
    }

    public boolean isLooping(AnimationClip clip) {
        return this.isPlaying && this.currentClip == clip;
    }

    public boolean isForceLooping() {
        return this.forceLoop;
    }

    public boolean hasLooped() {
        return this.hasLooped;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    public AnimationClip getCurrentClip() {
        return this.currentClip;
    }

    public boolean isCurrentClipOwnedBy(AnimationClip clip, class_1306 arm) {
        if (clip == null || this.currentClip != clip) {
            return false;
        }
        if (arm == null) {
            return true;
        }
        class_1306 ownerArm = this.clipArmFilter ? this.clipArm : this.activeArm;
        if (ownerArm == null) {
            return isDualHandedActive(this.currentClip, this.currentTime);
        }
        return ownerArm == arm;
    }

    public float getCurrentTime() {
        return this.currentTime;
    }

    public AnimationClip getSecondaryClip() {
        return this.overlapClip;
    }

    public float getSecondaryTime() {
        return this.overlapTime;
    }

    public class_1306 getSecondaryArm() {
        return this.overlapArm;
    }

    public float getTuningFactor(float partialTicks, class_1306 arm) {
        if (this.currentClip == null) {
            return 1.0f;
        }
        float renderTime = class_3532.method_16439(partialTicks, this.prevTime, this.currentTime);
        class_3532.method_16439(partialTicks, this.prevExpressionTime, this.expressionTime);
        if (this.preBlendInActive) {
            renderTime = DEBUG_COMPAT_IDLE;
        }
        boolean useKeyframesV2 = this.currentClip.isEffectActive("use_keyframes_v2", renderTime);
        if (useKeyframesV2) {
            return 0.0f;
        }
        boolean disableTuning = this.currentClip.hasEffectAny("disable_tuning") && this.isPlaying && this.currentClip.isEffectActive("disable_tuning", renderTime);
        if (disableTuning && this.activeArm != null && arm != null && arm != this.activeArm) {
            boolean dualHanded = isDualHandedActive(renderTime);
            if (!dualHanded) {
                disableTuning = DEBUG_COMPAT_IDLE;
            }
        }
        if (disableTuning) {
            return 0.0f;
        }
        if (!this.isPlaying && this.blendRemaining > 0.0f) {
            return 1.0f;
        }
        return 1.0f;
    }

    public float getArmTuningFactor(float partialTicks, class_1306 arm) {
        float suppressEased;
        AnimationClip armTuningClip = this.currentClip;
        float renderTime = class_3532.method_16439(partialTicks, this.prevTime, this.currentTime);
        class_1306 tuningArm = this.activeArm;
        if (armTuningClip == null && this.overlapCompatIdle && this.overlapClip != null) {
            armTuningClip = this.overlapClip;
            renderTime = class_3532.method_16439(partialTicks, this.overlapPrevTime, this.overlapTime);
            tuningArm = this.overlapArm;
        }
        if ((armTuningClip == null && this.armTuningSuppressAlpha <= 1.0E-4f && this.prevArmTuningSuppressAlpha <= 1.0E-4f) || armTuningClip == null) {
            return 1.0f;
        }
        class_3532.method_16439(partialTicks, this.prevExpressionTime, this.expressionTime);
        if (this.preBlendInActive) {
            renderTime = DEBUG_COMPAT_IDLE;
        }
        boolean keepArmTuning = isArmTuningKeepActive(renderTime);
        boolean useKeyframesV2 = armTuningClip.isEffectActive("use_keyframes_v2", renderTime);
        boolean keepTriggerArmTuning = isCompatKeepTriggerArmTuningActive(renderTime, arm);
        boolean keepLatchedTriggerDuringBlend = this.latchedKeepTriggerArmTuning && this.latchedTriggerArm != null && arm != null && arm == this.latchedTriggerArm && (this.blendRemaining > 0.0f || this.prevBlendRemaining > 0.0f);
        boolean primaryClipActive = armTuningClip == this.currentClip && this.isPlaying;
        boolean disableArmTuning = armTuningClip.hasEffectAny("disable_arm_tuning") && ((armTuningClip == this.overlapClip && this.overlapCompatIdle) || primaryClipActive) && armTuningClip.isEffectActive("disable_arm_tuning", renderTime);
        if (isWalkOnWaterClip(armTuningClip)) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
        }
        if (keepArmTuning && !disableArmTuning) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
        }
        if (disableArmTuning && tuningArm != null && arm != null && arm != tuningArm) {
            boolean dualHanded = isDualHandedActive(armTuningClip, renderTime);
            if (!dualHanded) {
                disableArmTuning = DEBUG_COMPAT_IDLE;
            }
        }
        boolean equipClipActive = primaryClipActive && isEquipClip(armTuningClip);
        if (useKeyframesV2 && !keepArmTuning && (!disableArmTuning || !equipClipActive)) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
        }
        float suppressAlpha = class_3532.method_15363(class_3532.method_16439(partialTicks, this.prevArmTuningSuppressAlpha, this.armTuningSuppressAlpha), 0.0f, 1.0f);
        boolean dualHanded2 = isDualHandedActive(armTuningClip, renderTime);
        if (!dualHanded2 && tuningArm != null && arm != null && arm != tuningArm) {
            suppressAlpha = DEBUG_COMPAT_IDLE;
        }
        if (keepTriggerArmTuning) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
            suppressAlpha = DEBUG_COMPAT_IDLE;
        }
        if (keepLatchedTriggerDuringBlend) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
            suppressAlpha = DEBUG_COMPAT_IDLE;
        }
        if (primaryClipActive) {
            suppressEased = class_3532.method_15363(Easing.easeOutBack(suppressAlpha), 0.0f, 1.0f);
        } else {
            suppressEased = 1.0f - class_3532.method_15363(Easing.easeOutBack(1.0f - suppressAlpha), 0.0f, 1.0f);
        }
        if (disableArmTuning) {
            return 0.0f;
        }
        return 1.0f - suppressEased;
    }

    public boolean isPendulumDisabled(float partialTicks, class_1306 arm) {
        if (this.currentClip == null || !this.isPlaying) {
            return false;
        }
        float renderTime = class_3532.method_16439(partialTicks, this.prevTime, this.currentTime);
        if (this.preBlendInActive) {
            renderTime = DEBUG_COMPAT_IDLE;
        }
        if (this.currentClip.isEffectActive("use_keyframes_v2", renderTime)) {
            return true;
        }
        boolean disablePendulum = (this.currentClip.hasEffectAny("disable_pendulum") && this.currentClip.isEffectActive("disable_pendulum", renderTime)) || (this.currentClip.hasEffectAny("disable_pendulum_physics") && this.currentClip.isEffectActive("disable_pendulum_physics", renderTime));
        if (disablePendulum && this.activeArm != null && arm != null && arm != this.activeArm) {
            boolean dualHanded = isDualHandedActive(renderTime);
            if (!dualHanded) {
                disablePendulum = DEBUG_COMPAT_IDLE;
            }
        }
        return disablePendulum;
    }

    public void stop() {
        this.isPlaying = false;
        this.blendRemaining = 0.0f;
        this.prevBlendRemaining = 0.0f;
        this.blendInRemaining = 0.0f;
        this.prevBlendInRemaining = 0.0f;
        this.blendFrom.clear();
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        this.blendInDynamic = false;
        this.blendInClip = null;
        this.blendInTime = 0.0f;
        this.blendInPrevTime = 0.0f;
        this.blendInSpeedMultiplier = 1.0f;
        this.blendOutSlow = BLEND_OUT_SLOW;
        this.activeArm = null;
        this.clipArm = null;
        this.clipArmFilter = false;
        this.blendArm = null;
        this.blendArmFilter = false;
        this.blendOutDuration = BLEND_OUT_SECONDS;
        if (this.nextBlendInDuration != null) {
            this.blendInDuration = this.nextBlendInDuration.floatValue();
            this.nextBlendInDuration = null;
        } else {
            this.blendInDuration = BLEND_IN_SECONDS;
        }
        this.stopAfterCurrent = false;
        this.forceLoop = false;
        this.speedMultiplier = 1.0f;
        this.preBlendInActive = false;
        this.holdOnLastFrame = false;
        this.holdReleasePending = false;
        this.expressionTime = this.currentTime;
        this.prevExpressionTime = this.prevTime;
        this.skipClearOnStop = false;
        this.pendingClearArm = null;
        this.pendingClearArmTick = -1;
        this.pendingSkipArm = null;
        this.pendingSkipArmTick = -1;
        this.forcedSkipArm = null;
        this.nextForcedSkipArm = null;
        this.nextForcedSkipArmSet = false;
        clearArmBlend();
        this.frozen = false;
        this.paused = false;
        this.loopSpeedBoost = 1.0f;
        resetLoopSpeedParams();
        this.overlapClip = null;
        this.overlapTime = 0.0f;
        this.overlapPrevTime = 0.0f;
        this.overlapSpeedMultiplier = 1.0f;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = null;
        this.overlapPaused = false;
        this.overlapCompatIdle = false;
        this.pendingOverlapSourceArm = null;
        this.overlapBlendInDuration = BLEND_IN_SECONDS;
        this.latchedKeepTriggerArmTuning = false;
        this.latchedTriggerArm = null;
        clearArmTuningSuppression();
    }

    public void stopWithBlendOut() {
        if (this.currentClip == null) {
            stop();
            return;
        }
        this.holdOnLastFrame = false;
        if (this.isPlaying) {
            requestStopAfterCurrent();
        } else {
            beginBlendOut();
        }
    }

    public void stopImmediateWithBlendOut() {
        if (this.currentClip == null) {
            stop();
            return;
        }
        this.holdOnLastFrame = false;
        this.holdReleasePending = false;
        this.skipClearOnStop = false;
        this.pendingClearArm = null;
        this.pendingClearArmTick = -1;
        this.pendingSkipArm = null;
        this.pendingSkipArmTick = -1;
        clearArmBlend();
        this.frozen = false;
        this.paused = false;
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        this.blendInDynamic = false;
        this.blendInClip = null;
        this.blendInTime = 0.0f;
        this.blendInPrevTime = 0.0f;
        this.blendInSpeedMultiplier = 1.0f;
        this.blendOutSlow = BLEND_OUT_SLOW;
        beginBlendOut();
        this.overlapClip = null;
        this.overlapTime = 0.0f;
        this.overlapPrevTime = 0.0f;
        this.overlapSpeedMultiplier = 1.0f;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = null;
        this.overlapPaused = false;
    }

    public void stopWithoutClear() {
        this.isPlaying = false;
        this.blendRemaining = 0.0f;
        this.prevBlendRemaining = 0.0f;
        this.blendInRemaining = 0.0f;
        this.prevBlendInRemaining = 0.0f;
        this.blendFrom.clear();
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        this.blendInDynamic = false;
        this.blendInClip = null;
        this.blendInTime = 0.0f;
        this.blendInPrevTime = 0.0f;
        this.blendInSpeedMultiplier = 1.0f;
        this.blendOutSlow = BLEND_OUT_SLOW;
        this.stopAfterCurrent = false;
        this.forceLoop = false;
        this.speedMultiplier = 1.0f;
        this.preBlendInActive = false;
        this.holdOnLastFrame = false;
        this.holdReleasePending = false;
        this.expressionTime = this.currentTime;
        this.prevExpressionTime = this.prevTime;
        this.skipClearOnStop = true;
        this.pendingClearArm = null;
        this.pendingClearArmTick = -1;
        this.pendingSkipArm = null;
        this.pendingSkipArmTick = -1;
        clearArmBlend();
        this.frozen = false;
        this.paused = false;
        this.loopSpeedBoost = 1.0f;
        resetLoopSpeedParams();
        this.overlapClip = null;
        this.overlapTime = 0.0f;
        this.overlapPrevTime = 0.0f;
        this.overlapSpeedMultiplier = 1.0f;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = null;
        this.overlapPaused = false;
        this.overlapCompatIdle = false;
        this.overlapBlendInDuration = BLEND_IN_SECONDS;
        this.latchedKeepTriggerArmTuning = false;
        this.latchedTriggerArm = null;
        clearArmTuningSuppression();
    }

    public void tick(float baseDeltaSeconds, float animationSpeed) {
        this.tickCounter++;
        tickArmTuningSuppression(baseDeltaSeconds);
        float effectiveSpeed = animationSpeed;
        float effectiveMultiplier = this.speedMultiplier;
        if (isEquipClip(this.currentClip)) {
            effectiveSpeed = 1.0f;
            effectiveMultiplier = 1.0f;
        } else if (isConsumeClip(this.currentClip) || isFixedSpeedClip(this.currentClip)) {
            effectiveSpeed = 1.0f;
        }
        float timelineSpeed = resolveTimelineSpeed(this.currentClip, this.currentTime);
        if (!Float.isNaN(timelineSpeed)) {
            effectiveSpeed = timelineSpeed * this.timelineSpeedScale;
            effectiveMultiplier = 1.0f;
        }
        boolean z = this.isPlaying && this.currentClip != null && (this.currentClip.isLoop() || this.forceLoop) && !this.stopAfterCurrent;
        this.loopSpeedBoost = 1.0f;
        float animDelta = baseDeltaSeconds * effectiveSpeed * effectiveMultiplier * this.loopSpeedBoost;
        if (this.blendInDynamic && this.blendInClip != null) {
            float blendDelta = computeAnimDelta(this.blendInClip, this.blendInTime, baseDeltaSeconds, animationSpeed, this.blendInSpeedMultiplier);
            this.blendInPrevTime = this.blendInTime;
            this.blendInTime = advanceClipTime(this.blendInClip, this.blendInTime, blendDelta);
        }
        if (this.overlapClip != null) {
            float overlapSpeed = animationSpeed;
            float overlapMultiplier = this.overlapSpeedMultiplier;
            if (isEquipClip(this.overlapClip)) {
                overlapSpeed = 1.0f;
                overlapMultiplier = 1.0f;
            } else if (isConsumeClip(this.overlapClip) || isFixedSpeedClip(this.overlapClip)) {
                overlapSpeed = 1.0f;
            }
            float overlapTimelineSpeed = resolveTimelineSpeed(this.overlapClip, this.overlapTime);
            if (!Float.isNaN(overlapTimelineSpeed)) {
                overlapSpeed = overlapTimelineSpeed * this.timelineSpeedScale;
                overlapMultiplier = 1.0f;
            }
            float overlapDelta = this.overlapPaused ? 0.0f : baseDeltaSeconds * overlapSpeed * overlapMultiplier;
            this.overlapPrevTime = this.overlapTime;
            this.overlapTime += overlapDelta;
            if (this.overlapTime >= this.overlapClip.getLength()) {
                if (this.overlapClip.isLoop() && this.overlapClip.getLength() > 0.0f) {
                    this.overlapTime %= this.overlapClip.getLength();
                    this.overlapPrevTime = this.overlapTime;
                } else {
                    this.overlapTime = this.overlapClip.getLength();
                    if (this.overlapBlendRemaining <= 0.0f) {
                        this.overlapBlendRemaining = BLEND_OUT_SECONDS;
                        this.overlapPrevBlendRemaining = this.overlapBlendRemaining;
                    }
                }
            }
            if (this.overlapBlendRemaining > 0.0f) {
                this.overlapPrevBlendRemaining = this.overlapBlendRemaining;
                this.overlapBlendRemaining = Math.max(0.0f, this.overlapBlendRemaining - (baseDeltaSeconds / BLEND_OUT_SLOW));
                if (this.overlapBlendRemaining == 0.0f) {
                    this.overlapClip = null;
                    this.overlapArm = null;
                    this.overlapBlendFrom.clear();
                    this.overlapBlendInRemaining = 0.0f;
                    this.overlapPrevBlendInRemaining = 0.0f;
                }
            }
            if (this.overlapBlendInRemaining > 0.0f) {
                this.overlapPrevBlendInRemaining = this.overlapBlendInRemaining;
                this.overlapBlendInRemaining = Math.max(0.0f, this.overlapBlendInRemaining - (baseDeltaSeconds / 1.0f));
            }
        }
        if (!this.isPlaying || this.currentClip == null) {
            if (this.blendRemaining > 0.0f) {
                this.prevBlendRemaining = this.blendRemaining;
                this.blendRemaining = Math.max(0.0f, this.blendRemaining - (baseDeltaSeconds / this.blendOutSlow));
                return;
            }
            return;
        }
        if (this.blendInRemaining > 0.0f) {
            this.prevBlendInRemaining = this.blendInRemaining;
            this.blendInRemaining = Math.max(0.0f, this.blendInRemaining - (animDelta / 1.0f));
            if (this.blendInRemaining == 0.0f) {
                this.preBlendInActive = false;
                this.blendInFrom.clear();
                this.blendInFromActive = false;
                this.blendInDynamic = false;
                this.blendInClip = null;
                this.blendInTime = 0.0f;
                this.blendInPrevTime = 0.0f;
                this.blendInSpeedMultiplier = 1.0f;
            }
        }
        tickArmBlend(animDelta);
        if (!this.preBlendInActive) {
            if (this.frozen) {
                this.prevTime = 0.0f;
                this.currentTime = 0.0f;
                this.prevExpressionTime = 0.0f;
                this.expressionTime = 0.0f;
            } else {
                float timeDelta = this.paused ? 0.0f : animDelta;
                this.prevTime = this.currentTime;
                this.currentTime += timeDelta;
                this.prevExpressionTime = this.expressionTime;
                this.expressionTime += timeDelta;
            }
        }
        if (this.currentTime >= this.currentClip.getLength()) {
            if (this.holdOnLastFrame) {
                if (!this.holdReleasePending) {
                    this.currentTime = this.currentClip.getLength();
                    this.prevTime = this.currentTime;
                    return;
                } else {
                    this.holdOnLastFrame = false;
                    this.holdReleasePending = false;
                }
            }
            if ((this.currentClip.isLoop() || this.forceLoop) && !this.stopAfterCurrent) {
                this.currentTime %= this.currentClip.getLength();
                this.prevTime = this.currentTime - animDelta;
                this.hasLooped = true;
            } else {
                beginBlendOut();
                this.stopAfterCurrent = false;
                this.forceLoop = false;
                this.prevBlendRemaining = this.blendRemaining;
                this.blendRemaining = Math.max(0.0f, this.blendRemaining - (baseDeltaSeconds / BLEND_OUT_SLOW));
            }
        }
        if (this.rebaseActive && this.rebaseReturnToOrigin && this.currentClip != null && resolveRebaseReturnProgress(this.currentClip, this.expressionTime) >= 1.0f) {
            this.rebaseFrom.clear();
            this.rebaseOrigin.clear();
            this.rebaseActive = false;
            this.rebaseReturnToOrigin = false;
            this.rebaseStartTime = 0.0f;
        }
        if (this.hasLooped && this.currentClip != null && this.activeArm != null) {
            this.hasLooped = false;
            if (!isEquipClip(this.currentClip) && !isDualHandedActive(0.0f)) {
                MovementStateMachine.get().triggerIdleBalance(this.activeArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182);
            }
        }
    }

    public boolean isOffhandHiddenIfEmptyActive() {
        if (this.currentClip == null) {
            return false;
        }
        return isOffhandHiddenIfEmptyActiveAtTime(this.currentTime);
    }

    public boolean isOffhandHiddenIfEmptyActive(float partialTicks) {
        if (this.currentClip == null) {
            return false;
        }
        float renderTime = class_3532.method_16439(partialTicks, this.prevTime, this.currentTime);
        if (this.preBlendInActive) {
            renderTime = DEBUG_COMPAT_IDLE;
        }
        return isOffhandHiddenIfEmptyActiveAtTime(renderTime);
    }

    private boolean isOffhandHiddenIfEmptyActiveAtTime(float time) {
        if (this.currentClip == null || !isRealOffhandEmpty()) {
            return false;
        }
        float lastHide = this.currentClip.getLastEffectTime(HIDE_OFFHAND_IF_EMPTY_EFFECT, time);
        float lastShow = this.currentClip.getLastEffectTime(SHOW_OFFHAND_IF_EMPTY_EFFECT, time);
        boolean hasHide = !Float.isNaN(lastHide);
        boolean hasShow = !Float.isNaN(lastShow);
        return hasHide && (!hasShow || lastHide > lastShow);
    }

    private class_1306 resolveTimelineForcedSkipArm(float time, class_1306 currentForcedArm) {
        if (!isOffhandHiddenIfEmptyActiveAtTime(time)) {
            return currentForcedArm;
        }
        class_1306 offhandArm = resolvePlayerOffhandArm();
        return offhandArm == null ? currentForcedArm : offhandArm;
    }

    private boolean isRealOffhandEmpty() {
        class_310 client = class_310.method_1551();
        return client == null || client.field_1724 == null || client.field_1724.method_6079().method_7960();
    }

    private class_1306 resolvePlayerOffhandArm() {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null) {
            return null;
        }
        class_1306 mainArm = client.field_1724.method_6068();
        return mainArm == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183;
    }

    private boolean isPlayerOffhandArm(class_1306 arm) {
        return arm != null && arm == resolvePlayerOffhandArm();
    }

    private float computeAnimDelta(AnimationClip clip, float time, float baseDeltaSeconds, float animationSpeed, float clipSpeedMultiplier) {
        if (clip == null) {
            return 0.0f;
        }
        float effectiveSpeed = animationSpeed;
        float effectiveMultiplier = clipSpeedMultiplier;
        if (isEquipClip(clip)) {
            effectiveSpeed = 1.0f;
            effectiveMultiplier = 1.0f;
        } else if (isConsumeClip(clip) || isFixedSpeedClip(clip)) {
            effectiveSpeed = 1.0f;
        }
        float timelineSpeed = resolveTimelineSpeed(clip, time);
        if (!Float.isNaN(timelineSpeed)) {
            effectiveSpeed = timelineSpeed;
            effectiveMultiplier = 1.0f;
        }
        return baseDeltaSeconds * effectiveSpeed * effectiveMultiplier;
    }

    private float advanceClipTime(AnimationClip clip, float time, float delta) {
        if (clip == null) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= 0.0f) {
            return 0.0f;
        }
        float next = time + delta;
        if (clip.isLoop()) {
            float next2 = next % length;
            if (next2 < 0.0f) {
                next2 += length;
            }
            return next2;
        }
        return Math.min(next, length);
    }

    private boolean isEquipClip(AnimationClip clip) {
        String name;
        return (clip == null || (name = clip.getName()) == null || (!name.equals("hand_in") && !name.equals("hand_out") && !name.startsWith("hand_in_") && !name.startsWith("hand_out_"))) ? false : true;
    }

    private boolean isWalkOnWaterClip(AnimationClip clip) {
        if (clip == null || clip.getName() == null) {
            return false;
        }
        return clip.getName().contains("walk_on_water");
    }

    private boolean isConsumeClip(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        String name = clip.getName();
        return "use_eat".equals(name) || "use_drink".equals(name);
    }

    private boolean isIdleLikeClip(AnimationClip clip) {
        String name;
        if (clip == null || (name = clip.getName()) == null) {
            return false;
        }
        return "idle".equals(name) || name.startsWith("idle_");
    }

    private boolean isFixedSpeedClip(AnimationClip clip) {
        if (clip == null) {
            return false;
        }
        return "falling_in_water".equals(clip.getName());
    }

    private float resolveTimelineSpeed(AnimationClip clip, float time) {
        if (clip == null) {
            return Float.NaN;
        }
        float resolved = Float.NaN;
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffectsUpTo(time)) {
            for (String effect : entry.getValue()) {
                if (effect != null && !effect.isBlank()) {
                    String normalized = effect.toLowerCase(Locale.ROOT).trim();
                    if (normalized.startsWith(ANIM_SPEED_PREFIX)) {
                        String value = normalized.substring(ANIM_SPEED_PREFIX.length());
                        if (!value.isEmpty()) {
                            try {
                                Matcher matcher = ANIM_SPEED_VALUE_PATTERN.matcher(value);
                                if (matcher.find()) {
                                    String numericToken = matcher.group().replace(',', '.');
                                    float parsed = Float.parseFloat(numericToken);
                                    if (Float.isFinite(parsed)) {
                                        resolved = class_3532.method_15363(parsed, 0.5f, 5.5f);
                                    }
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        }
        return resolved;
    }

    public void releaseHold() {
        if (!this.holdOnLastFrame || this.currentClip == null) {
            return;
        }
        if (this.currentTime >= this.currentClip.getLength()) {
            this.holdOnLastFrame = false;
        } else {
            this.holdReleasePending = true;
        }
    }

    public void applyTransformations(Map<String, class_630> partsMap, float partialTicks) {
        applyTransformations(partsMap, partialTicks, true);
    }

    /* JADX WARN: Removed duplicated region for block: B:477:0x04e3 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:478:0x04cc A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void applyTransformations(java.util.Map<java.lang.String, net.minecraft.class_630> r8, float r9, boolean r10) {
        /*
            Method dump skipped, instruction units count: 3030
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: punchy.client.animation.PoseHandler.applyTransformations(java.util.Map, float, boolean):void");
    }

    private void beginBlendOut() {
        if (this.currentClip == null) {
            return;
        }
        this.isPlaying = false;
        this.blendInRemaining = 0.0f;
        this.prevBlendInRemaining = 0.0f;
        if (this.nextBlendOutDuration != null) {
            this.blendOutDuration = this.nextBlendOutDuration.floatValue();
            this.nextBlendOutDuration = null;
        } else {
            this.blendOutDuration = BLEND_OUT_SECONDS;
        }
        if (this.nextBlendOutSlow != null) {
            this.blendOutSlow = this.nextBlendOutSlow.floatValue();
            this.nextBlendOutSlow = null;
        } else {
            this.blendOutSlow = BLEND_OUT_SLOW;
        }
        this.prevBlendRemaining = this.blendOutDuration;
        this.blendRemaining = this.blendOutDuration;
        this.blendFrom.clear();
        class_1306 arm = this.activeArm != null ? this.activeArm : this.clipArmFilter ? this.clipArm : null;
        boolean dualHandedActive = isDualHandedActive(this.currentTime);
        this.blendArm = arm;
        this.blendArmFilter = (arm == null || dualHandedActive) ? false : true;
        for (Map.Entry<String, BoneTrack> entry : this.currentClip.getTracks().entrySet()) {
            if (!this.blendArmFilter || isBoneForArm(entry.getKey(), this.blendArm)) {
                Sample sample = sampleTrack(entry.getValue(), this.currentTime, this.expressionTime, this.currentClip);
                if (this.rebaseActive) {
                    Sample from = this.rebaseFrom.get(entry.getKey());
                    Sample origin = this.rebaseOrigin.get(entry.getKey());
                    if (from != null && origin != null) {
                        sample = rebaseSampleForCurrentClip(entry.getKey(), from, origin, sample, this.expressionTime);
                    }
                }
                this.blendFrom.put(entry.getKey(), sample);
            }
        }
        this.speedMultiplier = 1.0f;
        this.timelineSpeedScale = 1.0f;
        this.preBlendInActive = false;
        this.holdOnLastFrame = false;
        this.holdReleasePending = false;
        clearArmBlend();
    }

    private void beginOverlapBlendOut(class_1306 arm) {
        if (this.currentClip == null || arm == null) {
            return;
        }
        this.blendOutDuration = BLEND_OUT_SECONDS;
        this.prevBlendRemaining = this.blendOutDuration;
        this.blendRemaining = this.blendOutDuration;
        this.blendFrom.clear();
        boolean dualHandedActive = isDualHandedActive(this.currentTime);
        this.blendArm = arm;
        this.blendArmFilter = (arm == null || dualHandedActive) ? false : true;
        for (Map.Entry<String, BoneTrack> entry : this.currentClip.getTracks().entrySet()) {
            if (!this.blendArmFilter || isBoneForArm(entry.getKey(), this.blendArm)) {
                Sample sample = sampleTrack(entry.getValue(), this.currentTime, this.expressionTime, this.currentClip);
                if (this.rebaseActive) {
                    Sample from = this.rebaseFrom.get(entry.getKey());
                    Sample origin = this.rebaseOrigin.get(entry.getKey());
                    if (from != null && origin != null) {
                        sample = rebaseSampleForCurrentClip(entry.getKey(), from, origin, sample, this.expressionTime);
                    }
                }
                this.blendFrom.put(entry.getKey(), sample);
            }
        }
    }

    private void preserveOverlapClip(class_1306 arm) {
        if (this.currentClip == null || arm == null) {
            return;
        }
        this.overlapClip = this.currentClip;
        this.overlapTime = this.currentTime;
        this.overlapPrevTime = this.prevTime;
        this.overlapSpeedMultiplier = this.speedMultiplier;
        this.overlapBlendRemaining = 0.0f;
        this.overlapPrevBlendRemaining = 0.0f;
        this.overlapArm = arm;
    }

    private void captureCurrentArmSamplesInto(Map<String, Sample> target, class_1306 arm) {
        if (target == null) {
            return;
        }
        target.clear();
        if (arm == null) {
            return;
        }
        AnimationClip sourceClip = this.currentClip;
        float sourceTime = this.currentTime;
        float sourceEvalTime = this.expressionTime;
        boolean sourceIsOverlap = DEBUG_COMPAT_IDLE;
        if (sourceClip == null && this.overlapCompatIdle && this.overlapClip != null && this.overlapArm == arm) {
            sourceClip = this.overlapClip;
            sourceTime = this.overlapTime;
            sourceEvalTime = this.overlapTime;
            sourceIsOverlap = true;
        }
        if (sourceClip == null) {
            return;
        }
        for (Map.Entry<String, BoneTrack> entry : sourceClip.getTracks().entrySet()) {
            String boneName = entry.getKey();
            if (isBoneForArm(boneName, arm)) {
                Sample sample = sampleTrack(entry.getValue(), sourceTime, sourceEvalTime, sourceClip);
                if (!sourceIsOverlap && this.rebaseActive) {
                    Sample from = this.rebaseFrom.get(boneName);
                    Sample origin = this.rebaseOrigin.get(boneName);
                    if (from != null && origin != null) {
                        sample = rebaseSampleForCurrentClip(boneName, from, origin, sample, this.expressionTime);
                    }
                }
                target.put(boneName, sample);
            }
        }
    }

    private void captureVisibleArmSamplesForBlendIn(class_1306 arm) {
        this.blendInFrom.clear();
        this.blendInFromActive = false;
        if (arm == null) {
            return;
        }
        captureVisibleArmSamplesInto(this.blendInFrom, arm);
        this.blendInFromActive = !this.blendInFrom.isEmpty();
    }

    private void captureVisibleArmSamplesInto(Map<String, Sample> target, class_1306 arm) {
        if (target == null) {
            return;
        }
        target.clear();
        if (arm == null) {
            return;
        }
        if (this.overlapCompatIdle && this.overlapClip != null && this.overlapArm == arm) {
            for (Map.Entry<String, BoneTrack> entry : this.overlapClip.getTracks().entrySet()) {
                String boneName = entry.getKey();
                if (isBoneForArm(boneName, arm)) {
                    target.put(boneName, sampleTrack(entry.getValue(), this.overlapTime, this.overlapTime, this.overlapClip));
                }
            }
            if (!target.isEmpty()) {
                return;
            }
        }
        captureCurrentArmSamplesInto(target, arm);
    }

    private void captureVisibleSamplesForTargetClip(Map<String, Sample> target, class_1306 arm, AnimationClip targetClip) {
        captureVisibleSamplesForTargetClip(target, arm, targetClip, false);
    }

    private void captureVisibleSamplesForTargetClip(Map<String, Sample> target, class_1306 arm, AnimationClip targetClip, boolean armPoseOnlyBlend) {
        if (target == null) {
            return;
        }
        target.clear();
        if (arm == null || targetClip == null) {
            return;
        }
        AnimationClip previousClip = this.currentClip;
        float previousTime = this.currentTime;
        float previousExpressionTime = this.expressionTime;
        Map<String, Sample> previousRebaseFrom = this.rebaseFrom;
        Map<String, Sample> previousRebaseOrigin = this.rebaseOrigin;
        boolean previousRebaseActive = this.rebaseActive;
        boolean previousRebaseReturnToOrigin = this.rebaseReturnToOrigin;
        float previousRebaseStartTime = this.rebaseStartTime;
        float previousRebaseReturnSpeedMultiplier = this.rebaseReturnSpeedMultiplier;
        if (previousClip == null && this.overlapCompatIdle && this.overlapClip != null && this.overlapArm == arm) {
            previousClip = this.overlapClip;
            previousTime = this.overlapTime;
            previousExpressionTime = this.overlapTime;
            previousRebaseFrom = Map.of();
            previousRebaseOrigin = Map.of();
            previousRebaseActive = DEBUG_COMPAT_IDLE;
            previousRebaseReturnToOrigin = DEBUG_COMPAT_IDLE;
            previousRebaseStartTime = DEBUG_COMPAT_IDLE;
            previousRebaseReturnSpeedMultiplier = 1.7f;
        }
        for (Map.Entry<String, BoneTrack> entry : targetClip.getTracks().entrySet()) {
            String boneName = entry.getKey();
            if (!armPoseOnlyBlend || !isItemGripOrHeldItemBone(boneName)) {
                class_1306 boneArm = resolveBoneArm(boneName);
                if (boneArm == null || boneArm == arm) {
                    Sample sample = sampleVisibleSourceForRebase(boneName, boneArm, previousClip, previousTime, previousExpressionTime, previousRebaseFrom, previousRebaseOrigin, previousRebaseActive, previousRebaseReturnToOrigin, previousRebaseStartTime, previousRebaseReturnSpeedMultiplier);
                    target.put(boneName, sample);
                }
            }
        }
    }

    private boolean isItemGripOrHeldItemBone(String boneName) {
        if (boneName == null) {
            return false;
        }
        String normalized = boneName.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("item_") || normalized.endsWith("_item") || normalized.startsWith("itemgrip_") || normalized.endsWith("_itemgrip") || normalized.contains("itemgrip");
    }

    private boolean shouldForceBlendInFromVisibleSource() {
        return (this.isPlaying || this.blendRemaining > 0.0f || !this.overlapCompatIdle || this.overlapClip == null || this.overlapArm == null) ? false : true;
    }

    private void debugCompatIdle(String phase, String extra) {
    }

    private String clipName(AnimationClip clip) {
        return clip == null ? "null" : clip.getName();
    }

    private class_1306 resolveVisibleSourceArm() {
        if (this.activeArm != null) {
            return this.activeArm;
        }
        if (this.clipArmFilter && this.clipArm != null) {
            return this.clipArm;
        }
        if (this.overlapCompatIdle && this.overlapArm != null) {
            return this.overlapArm;
        }
        return null;
    }

    private Matrix4f calculateMatrix(BoneTrack track, float time) {
        Sample sample = sampleTrack(track, time, time, this.currentClip);
        return matrixFromSample(sample, this.currentClip != null && this.currentClip.isEffectActive("use_keyframes_v2", time));
    }

    private Matrix4f matrixFromKeyframe(Keyframe kf) {
        Vector3f pos = kf.sampledPosition(this.expressionTime);
        Vector3f euler = kf.sampledRotationEuler(this.expressionTime);
        Vector3f scl = kf.sampledScale(this.expressionTime);
        Quaternionf rot = quatFromEuler(euler, this.currentClip, this.currentTime);
        return new Matrix4f().translate(pos).rotate(rot).scale(scl);
    }

    private Sample sampleTrack(BoneTrack track, float time, AnimationClip clip) {
        return sampleTrack(track, time, time, clip);
    }

    private Sample sampleTrack(BoneTrack track, float time, float evalTime, AnimationClip clip) {
        TreeMap<Float, Keyframe> index = track.getTimeIndex();
        boolean useKeyframesV2 = clip != null && clip.isEffectActive("use_keyframes_v2", time);
        Keyframe prevPos = findPrevWithPosition(index, time);
        Keyframe nextPos = findNextWithPosition(index, time);
        Vector3f pos = sampleVectorChannel(prevPos, nextPos, time, evalTime, new Vector3f(0.0f, 0.0f, 0.0f), useKeyframesV2 ? (kf, t) -> {
            return kf.sampledPositionV2(t.floatValue());
        } : (kf2, t2) -> {
            return kf2.sampledPosition(t2.floatValue());
        });
        Keyframe prevRot = findPrevWithRotation(index, time);
        Keyframe nextRot = findNextWithRotation(index, time);
        Quaternionf rot = sampleRotationChannel(index, prevRot, nextRot, time, evalTime, clip);
        Vector3f rotEulerV2 = useKeyframesV2 ? sampleRotationEulerChannelV2(index, prevRot, nextRot, time, evalTime) : null;
        Keyframe prevScale = findPrevWithScale(index, time);
        Keyframe nextScale = findNextWithScale(index, time);
        Vector3f scl = sampleVectorChannel(prevScale, nextScale, time, evalTime, new Vector3f(1.0f, 1.0f, 1.0f), (kf3, t3) -> {
            return kf3.sampledScale(t3.floatValue());
        });
        return new Sample(pos, rot, scl, rotEulerV2);
    }

    private static String fmt(Vector3f v) {
        return v == null ? "null" : String.format(Locale.ROOT, "[%.3f, %.3f, %.3f]", Float.valueOf(v.x), Float.valueOf(v.y), Float.valueOf(v.z));
    }

    private static String fmt(Quaternionf q) {
        return q == null ? "null" : String.format(Locale.ROOT, "[%.5f, %.5f, %.5f, %.5f]", Float.valueOf(q.x), Float.valueOf(q.y), Float.valueOf(q.z), Float.valueOf(q.w));
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.3f", Float.valueOf(value));
    }

    private Keyframe findPrevWithPosition(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fFloorKey = index.floorKey(Float.valueOf(time));
        while (true) {
            Float key = fFloorKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.position() != null || kf.positionExpression() != null)) {
                    break;
                }
                fFloorKey = index.lowerKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Keyframe findNextWithPosition(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fCeilingKey = index.ceilingKey(Float.valueOf(time));
        while (true) {
            Float key = fCeilingKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.position() != null || kf.positionExpression() != null)) {
                    break;
                }
                fCeilingKey = index.higherKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Keyframe findPrevWithRotation(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fFloorKey = index.floorKey(Float.valueOf(time));
        while (true) {
            Float key = fFloorKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.rotationEuler() != null || kf.rotationExpression() != null)) {
                    break;
                }
                fFloorKey = index.lowerKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Keyframe findNextWithRotation(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fCeilingKey = index.ceilingKey(Float.valueOf(time));
        while (true) {
            Float key = fCeilingKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.rotationEuler() != null || kf.rotationExpression() != null)) {
                    break;
                }
                fCeilingKey = index.higherKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Keyframe findPrevWithScale(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fFloorKey = index.floorKey(Float.valueOf(time));
        while (true) {
            Float key = fFloorKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.scale() != null || kf.scaleExpression() != null)) {
                    break;
                }
                fFloorKey = index.lowerKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Keyframe findNextWithScale(TreeMap<Float, Keyframe> index, float time) {
        Keyframe kf;
        Float fCeilingKey = index.ceilingKey(Float.valueOf(time));
        while (true) {
            Float key = fCeilingKey;
            if (key != null) {
                kf = index.get(key);
                if (kf != null && (kf.scale() != null || kf.scaleExpression() != null)) {
                    break;
                }
                fCeilingKey = index.higherKey(key);
            } else {
                return null;
            }
        }
        return kf;
    }

    private Vector3f sampleVectorChannel(Keyframe prev, Keyframe next, float time, float evalTime, Vector3f fallback, BiFunction<Keyframe, Float, Vector3f> getter) {
        if (prev == null && next == null) {
            return new Vector3f(fallback);
        }
        if (prev == null) {
            Vector3f v = getter.apply(next, Float.valueOf(evalTime));
            return v == null ? new Vector3f(fallback) : new Vector3f(v);
        }
        if (next == null) {
            Vector3f v2 = getter.apply(prev, Float.valueOf(evalTime));
            return v2 == null ? new Vector3f(fallback) : new Vector3f(v2);
        }
        Vector3f v1 = getter.apply(prev, Float.valueOf(evalTime));
        Vector3f v22 = getter.apply(next, Float.valueOf(evalTime));
        if (v1 == null && v22 == null) {
            return new Vector3f(fallback);
        }
        if (v1 == null) {
            return new Vector3f(v22);
        }
        if (v22 == null) {
            return new Vector3f(v1);
        }
        float t1 = prev.timeStamp();
        float t2 = next.timeStamp();
        float delta = t2 - t1;
        float progress = delta == 0.0f ? 0.0f : (time - t1) / delta;
        return new Vector3f(v1).lerp(v22, progress);
    }

    private Quaternionf sampleRotationChannel(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float time, float evalTime, AnimationClip clip) {
        boolean useKeyframesV2 = clip != null && clip.isEffectActive("use_keyframes_v2", time);
        if (useKeyframesV2) {
            return sampleRotationChannelV2(index, prev, next, time, evalTime);
        }
        if (prev == null && next == null) {
            return new Quaternionf();
        }
        if (prev == null) {
            Vector3f euler = next.sampledRotationEuler(evalTime);
            return euler == null ? new Quaternionf() : quatFromEuler(euler, clip, time);
        }
        if (next == null) {
            Vector3f euler2 = prev.sampledRotationEuler(evalTime);
            return euler2 == null ? new Quaternionf() : quatFromEuler(euler2, clip, time);
        }
        Vector3f eulerPrev = prev.sampledRotationEuler(evalTime);
        Vector3f eulerNext = next.sampledRotationEuler(evalTime);
        if (eulerPrev == null && eulerNext == null) {
            return new Quaternionf();
        }
        if (eulerPrev == null) {
            return quatFromEuler(eulerNext, clip, time);
        }
        if (eulerNext == null) {
            return quatFromEuler(eulerPrev, clip, time);
        }
        float t1 = prev.timeStamp();
        float t2 = next.timeStamp();
        float delta = t2 - t1;
        float progress = class_3532.method_15363(delta == 0.0f ? 0.0f : (time - t1) / delta, 0.0f, 1.0f);
        Vector3f out = new Vector3f(lerpAngleDegrees(eulerPrev.x, eulerNext.x, progress), lerpAngleDegrees(eulerPrev.y, eulerNext.y, progress), lerpAngleDegrees(eulerPrev.z, eulerNext.z, progress));
        return quatFromEuler(out, clip, time);
    }

    private Quaternionf sampleRotationChannelV2(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float time, float evalTime) {
        Vector3f euler = sampleRotationEulerChannelV2(index, prev, next, time, evalTime);
        return euler == null ? new Quaternionf() : quatFromEulerV2(euler);
    }

    private Vector3f sampleRotationEulerChannelV2(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float time, float evalTime) {
        if (prev == null && next == null) {
            return null;
        }
        if (prev == null) {
            return next.sampledRotationPreEulerV2(evalTime);
        }
        if (next == null) {
            return prev.sampledRotationPostEulerV2(evalTime);
        }
        if (prev == next || Math.abs(prev.timeStamp() - next.timeStamp()) <= 1.0E-4f) {
            return prev.sampledRotationPreEulerV2(evalTime);
        }
        float t1 = prev.timeStamp();
        float t2 = next.timeStamp();
        float delta = t2 - t1;
        float progress = class_3532.method_15363(delta == 0.0f ? 0.0f : (time - t1) / delta, 0.0f, 1.0f);
        String fromInterpolation = normalizeInterpolation(prev.interpolation());
        String toInterpolation = normalizeInterpolation(next.interpolation());
        if ("spherical_squad".equals(fromInterpolation) || "spherical_squad".equals(toInterpolation)) {
            Quaternionf rot = sampleRotationSphericalSquadV2(index, prev, next, evalTime, progress);
            return rot.getEulerAnglesXYZ(new Vector3f()).mul(57.29578f);
        }
        if ("spherical_linear".equals(fromInterpolation) || "spherical_linear".equals(toInterpolation)) {
            return quatFromEulerV2(prev.sampledRotationPostEulerV2(evalTime)).slerp(quatFromEulerV2(next.sampledRotationPreEulerV2(evalTime)), progress).getEulerAnglesXYZ(new Vector3f()).mul(57.29578f);
        }
        if ("catmullrom".equals(fromInterpolation) || "catmullrom".equals(toInterpolation)) {
            return sampleRotationCatmullRomEulerV2(index, prev, next, evalTime, progress);
        }
        Vector3f from = prev.sampledRotationPostEulerV2(evalTime);
        Vector3f to = next.sampledRotationPreEulerV2(evalTime);
        if (from == null && to == null) {
            return null;
        }
        if (from == null) {
            return to;
        }
        if (to == null) {
            return from;
        }
        return new Vector3f(from).lerp(to, progress);
    }

    private Quaternionf sampleRotationCatmullRomV2(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float evalTime, float progress) {
        Vector3f euler = sampleRotationCatmullRomEulerV2(index, prev, next, evalTime, progress);
        return euler == null ? new Quaternionf() : quatFromEulerV2(euler);
    }

    private Vector3f sampleRotationCatmullRomEulerV2(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float evalTime, float progress) {
        Keyframe beforePrev = findPrevWithRotation(index, prev.timeStamp() - 1.0E-4f);
        Keyframe afterNext = findNextWithRotation(index, next.timeStamp() + 1.0E-4f);
        Vector3f p0 = getRotationValueForSpline(beforePrev != null ? beforePrev : prev, true, evalTime);
        Vector3f p1 = getRotationValueForSpline(prev, false, evalTime);
        Vector3f p2 = getRotationValueForSpline(next, false, evalTime);
        Vector3f p3 = getRotationValueForSpline(afterNext != null ? afterNext : next, false, evalTime);
        Vector3f euler = new Vector3f(splineCurve(p0.x, p1.x, p2.x, p3.x, progress), splineCurve(p0.y, p1.y, p2.y, p3.y, progress), splineCurve(p0.z, p1.z, p2.z, p3.z, progress));
        return euler;
    }

    private Quaternionf sampleRotationSphericalSquadV2(TreeMap<Float, Keyframe> index, Keyframe prev, Keyframe next, float evalTime, float progress) {
        Keyframe beforePrev = findPrevWithRotation(index, prev.timeStamp() - 1.0E-4f);
        Keyframe afterNext = findNextWithRotation(index, next.timeStamp() + 1.0E-4f);
        Quaternionf q0 = quatFromEulerV2(getRotationValueForSpline(beforePrev != null ? beforePrev : prev, true, evalTime));
        Quaternionf q1 = quatFromEulerV2(getRotationValueForSpline(prev, false, evalTime));
        Quaternionf q2 = quatFromEulerV2(getRotationValueForSpline(next, false, evalTime));
        Quaternionf q3 = quatFromEulerV2(getRotationValueForSpline(afterNext != null ? afterNext : next, false, evalTime));
        return squad(q0, q1, q2, q3, progress);
    }

    private Vector3f getRotationValueForSpline(Keyframe keyframe, boolean useOffset, float evalTime) {
        Vector3f vector3fSampledRotationPreEulerV2;
        if (keyframe == null) {
            return new Vector3f();
        }
        if (useOffset) {
            vector3fSampledRotationPreEulerV2 = keyframe.sampledRotationPostEulerV2(evalTime);
        } else {
            vector3fSampledRotationPreEulerV2 = keyframe.sampledRotationPreEulerV2(evalTime);
        }
        Vector3f value = vector3fSampledRotationPreEulerV2;
        return value == null ? new Vector3f() : value;
    }

    private static String normalizeInterpolation(String interpolation) {
        if (interpolation == null || interpolation.isBlank()) {
            return "linear";
        }
        return interpolation.trim().toLowerCase(Locale.ROOT);
    }

    private static float splineCurve(float y0, float y1, float y2, float y3, float alpha) {
        float v0 = (y2 - y0) * 0.5f;
        float v1 = (y3 - y1) * 0.5f;
        float t2 = alpha * alpha;
        float t3 = alpha * t2;
        float h1 = ((DEFAULT_LOOP_SPEED_MAX * t3) - (3.0f * t2)) + 1.0f;
        float h2 = ((-2.0f) * t3) + (3.0f * t2);
        float h3 = (t3 - (DEFAULT_LOOP_SPEED_MAX * t2)) + alpha;
        float h4 = t3 - t2;
        return (h1 * y1) + (h2 * y2) + (h3 * v0) + (h4 * v1);
    }

    private static float lerpAngleDegrees(float a, float b, float t) {
        float delta = b - a;
        float abs = Math.abs(delta);
        if (abs > 180.0f && abs < 359.9999f) {
            delta -= Math.signum(delta) * 360.0f;
        }
        return a + (delta * t);
    }

    private Sample blendToIdentity(Sample from, float t) {
        Vector3f pos = new Vector3f(from.pos).lerp(new Vector3f(0.0f, 0.0f, 0.0f), t);
        Quaternionf rot = new Quaternionf(from.rot).slerp(new Quaternionf(), t);
        Vector3f scl = new Vector3f(from.scl).lerp(new Vector3f(1.0f, 1.0f, 1.0f), t);
        Vector3f rotEulerV2 = from.rotEulerV2 == null ? null : new Vector3f(from.rotEulerV2).lerp(new Vector3f(), t);
        return new Sample(pos, rot, scl, rotEulerV2);
    }

    private Quaternionf quatFromEuler(Vector3f eulerDeg, AnimationClip clip, float sampleTime) {
        float rx = (float) Math.toRadians(eulerDeg.x);
        float ry = (float) Math.toRadians(eulerDeg.y);
        float rz = (float) Math.toRadians(eulerDeg.z);
        if (this.javaBlock) {
            float rawX = (float) Math.toRadians(-eulerDeg.x);
            float rawY = (float) Math.toRadians(-eulerDeg.z);
            float rawZ = (float) Math.toRadians(-eulerDeg.y);
            return new Quaternionf().rotationXYZ(rawX, rawY, rawZ);
        }
        boolean forceXYZ = clip != null && (clip.isEffectActive("rotation_xyz", sampleTime) || clip.isEffectActive("rot_xyz", sampleTime));
        boolean forceZYX = clip != null && (clip.isEffectActive("rotation_zyx", sampleTime) || clip.isEffectActive("rot_zyx", sampleTime));
        String clipName = (clip == null || clip.getName() == null) ? "" : clip.getName();
        boolean useXYZ = clip != null && (clipName.contains("mace") || clipName.contains("trident") || clipName.contains("crossbow"));
        if (forceZYX) {
            useXYZ = DEBUG_COMPAT_IDLE;
        } else if (forceXYZ) {
            useXYZ = true;
        }
        if (useXYZ) {
            return new Quaternionf().rotationXYZ(rx, ry, rz);
        }
        return new Quaternionf().rotationZYX(rz, ry, rx);
    }

    private static Quaternionf quatFromEulerV2(Vector3f eulerDeg) {
        if (eulerDeg == null) {
            return new Quaternionf();
        }
        float rx = (float) Math.toRadians(eulerDeg.x);
        float ry = (float) Math.toRadians(eulerDeg.y);
        float rz = (float) Math.toRadians(eulerDeg.z);
        return new Quaternionf().rotationXYZ(rx, ry, rz);
    }

    private static Quaternionf squad(Quaternionf q0, Quaternionf q1, Quaternionf q2, Quaternionf q3, float t) {
        Quaternionf a = intermediate(q0, q1, q2);
        Quaternionf b = intermediate(q1, q2, q3);
        Quaternionf slerp1 = new Quaternionf(q1).slerp(q2, t);
        Quaternionf slerp2 = new Quaternionf(a).slerp(b, t);
        return slerp1.slerp(slerp2, DEFAULT_LOOP_SPEED_MAX * t * (1.0f - t));
    }

    private static Quaternionf intermediate(Quaternionf q0, Quaternionf q1, Quaternionf q2) {
        Quaternionf a = new Quaternionf(q0);
        Quaternionf b = new Quaternionf(q2);
        if (q1.dot(a) < 0.0f) {
            a.mul(-1.0f);
        }
        if (q1.dot(b) < 0.0f) {
            b.mul(-1.0f);
        }
        Quaternionf q0Conj = new Quaternionf(a).conjugate();
        Quaternionf q1Conj = new Quaternionf(q1).conjugate();
        Quaternionf m0 = q0Conj.mul(new Quaternionf(q1));
        Quaternionf m1 = q1Conj.mul(new Quaternionf(b));
        Quaternionf m0Log = quaternionLog(m0);
        Quaternionf m1Log = quaternionLog(m1);
        Quaternionf sum = new Quaternionf(m0Log).sub(m1Log).mul(0.25f);
        Quaternionf exp = quaternionExp(sum);
        return new Quaternionf(q1).mul(exp);
    }

    private static Quaternionf quaternionLog(Quaternionf q) {
        double sin = Math.sqrt((q.x * q.x) + (q.y * q.y) + (q.z * q.z));
        double theta = Math.atan2(sin, q.w);
        Quaternionf result = new Quaternionf(q);
        if (sin > 5.0E-4d) {
            result.mul((float) (theta / sin));
        }
        result.w = 0.0f;
        return result;
    }

    private static Quaternionf quaternionExp(Quaternionf q) {
        double theta = Math.sqrt((q.x * q.x) + (q.y * q.y) + (q.z * q.z));
        double cos = Math.cos(theta);
        Quaternionf result = new Quaternionf(q);
        if (cos < 0.9995d && theta > 1.0E-7d) {
            result.mul((float) (Math.sin(theta) / theta));
        }
        result.w = (float) cos;
        return result;
    }

    private Sample blendFromIdentity(Sample to, float t) {
        Vector3f pos = new Vector3f(0.0f, 0.0f, 0.0f).lerp(to.pos, t);
        Quaternionf rot = new Quaternionf().slerp(to.rot, t);
        Vector3f scl = new Vector3f(1.0f, 1.0f, 1.0f).lerp(to.scl, t);
        Vector3f rotEulerV2 = to.rotEulerV2 == null ? null : new Vector3f(to.rotEulerV2).mul(t);
        return new Sample(pos, rot, scl, rotEulerV2);
    }

    private Sample blendBetweenSamples(Sample from, Sample to, float t) {
        Vector3f pos = new Vector3f(from.pos).lerp(to.pos, t);
        Quaternionf rot = new Quaternionf(from.rot).slerp(to.rot, t);
        Vector3f scl = new Vector3f(from.scl).lerp(to.scl, t);
        Vector3f rotEulerV2 = DEBUG_COMPAT_IDLE;
        if (from.rotEulerV2 != null || to.rotEulerV2 != null) {
            Vector3f start = from.rotEulerV2 != null ? new Vector3f(from.rotEulerV2) : new Vector3f();
            Vector3f end = to.rotEulerV2 != null ? new Vector3f(to.rotEulerV2) : new Vector3f();
            rotEulerV2 = start.lerp(end, t);
        }
        return new Sample(pos, rot, scl, rotEulerV2);
    }

    private Sample rebaseSample(Sample from, Sample origin, Sample target) {
        Vector3f posDelta = new Vector3f(target.pos).sub(origin.pos);
        Vector3f pos = new Vector3f(from.pos).add(posDelta);
        Quaternionf invOrigin = new Quaternionf(origin.rot).invert();
        Quaternionf rotDelta = invOrigin.mul(new Quaternionf(target.rot), new Quaternionf());
        Quaternionf rot = new Quaternionf(from.rot).mul(rotDelta).normalize();
        Vector3f scl = new Vector3f(target.scl);
        return new Sample(pos, rot, scl, target.rotEulerV2);
    }

    private Sample rebaseSampleForCurrentClip(String boneName, Sample from, Sample origin, Sample target, float evalTime) {
        if (shouldBypassRebaseForBone(boneName)) {
            return target;
        }
        Sample rebased = rebaseSample(from, origin, target);
        if (!this.rebaseReturnToOrigin || this.currentClip == null) {
            return rebased;
        }
        float progress = resolveRebaseReturnProgress(this.currentClip, evalTime);
        if (progress <= 0.0f) {
            return rebased;
        }
        if (progress >= 1.0f) {
            return target;
        }
        return blendBetweenSamples(rebased, target, progress);
    }

    private boolean shouldBypassRebaseForBone(String boneName) {
        if (boneName == null || boneName.isBlank()) {
            return false;
        }
        String normalized = boneName.toLowerCase(Locale.ROOT);
        return normalized.contains("eyelid") || normalized.contains("pupil") || normalized.startsWith("eye_") || normalized.endsWith("_eye") || normalized.equals("eye") || normalized.contains("fin_") || normalized.endsWith("_fin");
    }

    private float resolveRebaseReturnProgress(AnimationClip clip, float evalTime) {
        if (clip == null) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= 0.0f) {
            return 1.0f;
        }
        float start = Math.max(0.0f, Math.min(this.rebaseStartTime, length));
        if (evalTime <= start) {
            return 0.0f;
        }
        float duration = Math.max(1.0E-4f, length - start);
        float rawProgress = class_3532.method_15363(((evalTime - start) / duration) * this.rebaseReturnSpeedMultiplier, 0.0f, 1.0f);
        return easeOutCubic(rawProgress);
    }

    private float easeOutCubic(float value) {
        float t = class_3532.method_15363(value, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - ((inv * inv) * inv);
    }

    private Matrix4f matrixFromSample(Sample sample, boolean useKeyframesV2) {
        Vector3f pos = new Vector3f(sample.pos);
        if (!useKeyframesV2 && this.javaBlock) {
            pos.set(-pos.x, -pos.z, -pos.y);
        } else if (!useKeyframesV2 && this.swapTranslationYZ) {
            pos.set(pos.x, pos.z, pos.y);
        }
        pos.x += 0.0f;
        pos.z += 0.0f;
        Matrix4f transform = new Matrix4f().translate(pos);
        if (useKeyframesV2 && sample.rotEulerV2 != null) {
            float rx = (float) Math.toRadians(sample.rotEulerV2.x);
            float ry = (float) Math.toRadians(sample.rotEulerV2.y);
            float rz = (float) Math.toRadians(sample.rotEulerV2.z);
            transform.rotateXYZ(rx, ry, rz);
        } else {
            transform.rotate(sample.rot);
        }
        transform.scale(sample.scl);
        if (!useKeyframesV2 && this.mirrorX) {
            Matrix4f mirror = new Matrix4f().scale(-1.0f, 1.0f, 1.0f);
            transform = new Matrix4f(mirror).mul(transform).mul(mirror);
        }
        return transform;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PoseHandler$Sample.class */
    private static class Sample {
        final Vector3f pos;
        final Quaternionf rot;
        final Vector3f scl;
        final Vector3f rotEulerV2;

        private Sample(Vector3f pos, Quaternionf rot, Vector3f scl, Vector3f rotEulerV2) {
            this.pos = pos;
            this.rot = rot;
            this.scl = scl;
            this.rotEulerV2 = rotEulerV2 != null ? new Vector3f(rotEulerV2) : null;
        }

        private static Sample identity() {
            return new Sample(new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), null);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PoseHandler$PartPoseSnapshot.class */
    private static class PartPoseSnapshot {
        final float x;
        final float y;
        final float z;
        final float xRot;
        final float yRot;
        final float zRot;
        final float xScale;
        final float yScale;
        final float zScale;

        private PartPoseSnapshot(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
            this.xScale = xScale;
            this.yScale = yScale;
            this.zScale = zScale;
        }

        private static PartPoseSnapshot of(class_630 part) {
            return new PartPoseSnapshot(part.field_3657, part.field_3656, part.field_3655, part.field_3654, part.field_3675, part.field_3674, part.field_37938, part.field_37939, part.field_37940);
        }
    }

    private void clearTransforms(Map<String, class_630> partsMap) {
        if (partsMap == null || partsMap.isEmpty()) {
            return;
        }
        if (this.currentClip == null) {
            for (class_630 part : partsMap.values()) {
                clearPartTransform(part);
            }
            return;
        }
        for (String boneName : this.currentClip.getTracks().keySet()) {
            if (!this.isPlaying || this.activeArm == null || isBoneForArm(boneName, this.activeArm)) {
                class_630 part2 = partsMap.get(boneName);
                clearPartTransform(part2);
            }
        }
    }

    private void clearTransformsForClipArm(Map<String, class_630> partsMap) {
        if (this.currentClip == null) {
            return;
        }
        for (String boneName : this.currentClip.getTracks().keySet()) {
            if (!this.clipArmFilter || isBoneForArm(boneName, this.clipArm)) {
                class_630 part = partsMap.get(boneName);
                clearPartTransform(part);
            }
        }
    }

    private void clearTransformsForArm(Map<String, class_630> partsMap, class_1306 arm) {
        if (this.currentClip == null || arm == null) {
            return;
        }
        for (String boneName : this.currentClip.getTracks().keySet()) {
            if (isBoneForArm(boneName, arm)) {
                class_630 part = partsMap.get(boneName);
                clearPartTransform(part);
            }
        }
    }

    private Map<String, PartPoseSnapshot> capturePartSnapshots(Map<String, class_630> partsMap) {
        Map<String, PartPoseSnapshot> snapshots = new HashMap<>();
        if (partsMap == null || partsMap.isEmpty()) {
            return snapshots;
        }
        for (Map.Entry<String, class_630> entry : partsMap.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                snapshots.put(entry.getKey(), PartPoseSnapshot.of(entry.getValue()));
            }
        }
        return snapshots;
    }

    private void applyV2ToPart(class_630 part, PartPoseSnapshot base, Sample sample) {
        if (part == null || sample == null) {
            return;
        }
        PartPoseSnapshot snapshot = base != null ? base : PartPoseSnapshot.of(part);
        Vector3f pos = sample.pos != null ? sample.pos : new Vector3f();
        Vector3f rotEuler = sample.rotEulerV2 != null ? sample.rotEulerV2 : new Vector3f();
        part.field_3657 = snapshot.x + pos.x;
        part.field_3656 = snapshot.y + pos.y;
        part.field_3655 = snapshot.z + pos.z;
        part.field_3654 = snapshot.xRot + ((float) Math.toRadians(rotEuler.x));
        part.field_3675 = snapshot.yRot + ((float) Math.toRadians(rotEuler.y));
        part.field_3674 = snapshot.zRot + ((float) Math.toRadians(rotEuler.z));
        part.field_37938 = snapshot.xScale * sample.scl.x;
        part.field_37939 = snapshot.yScale * sample.scl.y;
        part.field_37940 = snapshot.zScale * sample.scl.z;
        clearPartTransform(part);
        if (part instanceof TransformablePart) {
            TransformablePart transformable = (TransformablePart) part;
            transformable.punchy$setDirectPoseBase(snapshot.x, snapshot.y, snapshot.z, snapshot.xRot, snapshot.yRot, snapshot.zRot);
        }
    }

    public class_1306 getActiveArm() {
        return this.activeArm;
    }

    private void clearPartTransform(class_630 part) {
        if (part != null && (part instanceof TransformablePart)) {
            TransformablePart transformable = (TransformablePart) part;
            transformable.punchy$setExplicitTransform(null);
            transformable.punchy$clearDirectPoseBase();
        }
    }

    private void clearArmBlend() {
        this.leftArmBlendRemaining = 0.0f;
        this.leftArmBlendPrevRemaining = 0.0f;
        this.rightArmBlendRemaining = 0.0f;
        this.rightArmBlendPrevRemaining = 0.0f;
    }

    private void tickArmBlend(float animDelta) {
        if (!this.isPlaying || this.currentClip == null) {
            return;
        }
        if (this.leftArmBlendRemaining > 0.0f) {
            this.leftArmBlendPrevRemaining = this.leftArmBlendRemaining;
            this.leftArmBlendRemaining = Math.max(0.0f, this.leftArmBlendRemaining - (animDelta / 1.0f));
        }
        if (this.rightArmBlendRemaining > 0.0f) {
            this.rightArmBlendPrevRemaining = this.rightArmBlendRemaining;
            this.rightArmBlendRemaining = Math.max(0.0f, this.rightArmBlendRemaining - (animDelta / 1.0f));
        }
    }

    private float resolveArmBlendInT(class_1306 arm, float partialTicks) {
        float remaining = getArmBlendRemaining(arm);
        if (remaining <= 0.0f) {
            return 1.0f;
        }
        float prevRemaining = getArmBlendPrevRemaining(arm);
        float blendRem = class_3532.method_16439(partialTicks, prevRemaining, remaining);
        float t = 1.0f - (blendRem / Math.max(1.0E-4f, BLEND_IN_SECONDS));
        return applyBlendInEasing(t);
    }

    private float applyBlendInEasing(float t) {
        switch (this.blendInEasing.ordinal()) {
            case DEBUG_COMPAT_IDLE /* 0 */:
                return Easing.easeInBack(t);
            case 1:
                return Easing.smoothStep(t);
            case 2:
                return Easing.easeOutBackStrong(t);
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    private void tickArmTuningSuppression(float deltaSeconds) {
        this.prevArmTuningSuppressAlpha = this.armTuningSuppressAlpha;
        boolean equipClip = this.isPlaying && isEquipClip(this.currentClip);
        boolean disableArmTuning = this.isPlaying && this.currentClip != null && this.currentClip.hasEffectAny("disable_arm_tuning") && this.currentClip.isEffectActive("disable_arm_tuning", this.currentTime);
        if (isWalkOnWaterClip(this.currentClip)) {
            disableArmTuning = DEBUG_COMPAT_IDLE;
        }
        if (equipClip) {
            String name = this.currentClip != null ? this.currentClip.getName() : "";
            String key = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (key.equals("hand_in") || key.startsWith("hand_in_")) {
                if (disableArmTuning) {
                    this.armTuningSuppressAlpha = 1.0f;
                    this.prevArmTuningSuppressAlpha = 1.0f;
                    return;
                } else {
                    clearArmTuningSuppression();
                    return;
                }
            }
        }
        boolean overlapDisableArmTuning = this.overlapCompatIdle && this.overlapClip != null && this.overlapClip.hasEffectAny("disable_arm_tuning") && this.overlapClip.isEffectActive("disable_arm_tuning", this.overlapTime);
        if (isWalkOnWaterClip(this.overlapClip)) {
            overlapDisableArmTuning = DEBUG_COMPAT_IDLE;
        }
        boolean disableArmTuning2 = disableArmTuning || overlapDisableArmTuning;
        boolean keepArmTuning = isArmTuningKeepActive(this.currentTime);
        if (keepArmTuning && !disableArmTuning2) {
            clearArmTuningSuppression();
            return;
        }
        boolean shouldSuppress = (this.isPlaying && this.currentClip != null && (!equipClip || disableArmTuning2)) || overlapDisableArmTuning;
        float target = shouldSuppress ? 1.0f : 0.0f;
        float duration = shouldSuppress ? ARM_TUNING_SUPPRESS_IN_SECONDS : ARM_TUNING_SUPPRESS_OUT_SECONDS;
        float step = duration <= 0.0f ? 1.0f : deltaSeconds / duration;
        this.armTuningSuppressAlpha = class_3532.method_16439(class_3532.method_15363(step, 0.0f, 1.0f), this.armTuningSuppressAlpha, target);
    }

    private void clearArmTuningSuppression() {
        this.armTuningSuppressAlpha = 0.0f;
        this.prevArmTuningSuppressAlpha = 0.0f;
    }

    private boolean isArmTuningKeepActive(float time) {
        if (!this.isPlaying || this.currentClip == null) {
            return this.overlapCompatIdle && this.overlapClip != null && this.overlapClip.hasEffectAny("keep_arm_tuning") && this.overlapClip.isEffectActive("keep_arm_tuning", this.overlapTime);
        }
        boolean timelineKeep = this.currentClip.hasEffectAny("keep_arm_tuning") && this.currentClip.isEffectActive("keep_arm_tuning", time);
        if (timelineKeep) {
            return true;
        }
        if (this.overlapCompatIdle && this.overlapClip != null && this.overlapClip.hasEffectAny("keep_arm_tuning") && this.overlapClip.isEffectActive("keep_arm_tuning", this.overlapTime)) {
            return true;
        }
        return isCompatKeepArmTuningActive(time);
    }

    private boolean isCompatKeepArmTuningActive(float time) {
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return false;
        }
        class_746 player = mc.field_1724;
        if (this.activeArm == null) {
            return hasCompatKeepArmTuning(player, class_1306.field_6182) || hasCompatKeepArmTuning(player, class_1306.field_6183);
        }
        if (hasCompatKeepArmTuning(player, this.activeArm)) {
            return true;
        }
        if (isDualHandedActive(time)) {
            class_1306 other = this.activeArm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
            return hasCompatKeepArmTuning(player, other);
        }
        return false;
    }

    private boolean hasCompatKeepArmTuning(class_746 player, class_1306 arm) {
        ToolTuning.Profile profile = resolveCompatProfileForArm(player, arm);
        return profile != null && Boolean.TRUE.equals(profile.keepArmTuning);
    }

    private boolean isCompatKeepTriggerArmTuningActive(float time, class_1306 arm) {
        if (!this.isPlaying || this.currentClip == null || arm == null) {
            return false;
        }
        if (this.latchedKeepTriggerArmTuning && this.latchedTriggerArm != null) {
            return arm == this.latchedTriggerArm;
        }
        class_310 mc = class_310.method_1551();
        if (mc == null || mc.field_1724 == null) {
            return false;
        }
        class_746 player = mc.field_1724;
        ToolTuning.Profile profile = resolveCompatProfileForArm(player, arm);
        class_1306 triggerArm = this.activeArm != null ? this.activeArm : PunchyAnimationManager.getSourceArm();
        return profile != null && Boolean.TRUE.equals(profile.keepTriggerArmTuning) && triggerArm != null && arm == triggerArm;
    }

    private void latchCompatKeepTriggerArmState() {
        class_310 mc;
        ToolTuning.Profile profile;
        this.latchedKeepTriggerArmTuning = false;
        this.latchedTriggerArm = null;
        if (!this.isPlaying || this.currentClip == null || (mc = class_310.method_1551()) == null || mc.field_1724 == null) {
            return;
        }
        class_1306 triggerArm = this.activeArm != null ? this.activeArm : PunchyAnimationManager.getSourceArm();
        if (triggerArm != null && (profile = resolveCompatProfileForArm(mc.field_1724, triggerArm)) != null && Boolean.TRUE.equals(profile.keepTriggerArmTuning)) {
            this.latchedKeepTriggerArmTuning = true;
            this.latchedTriggerArm = triggerArm;
        }
    }

    private ToolTuning.Profile resolveCompatProfileForArm(class_746 player, class_1306 arm) {
        class_1799 class_1799VarMethod_6079;
        VanillaFirstPersonItemLayer.ToolKind toolKind;
        if (player == null || arm == null) {
            return null;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        if (arm == class_1306.field_6183) {
            class_1799VarMethod_6079 = mainRight ? player.method_6047() : player.method_6079();
        } else {
            class_1799VarMethod_6079 = mainRight ? player.method_6079() : player.method_6047();
        }
        class_1799 baseStack = class_1799VarMethod_6079;
        class_1799 stack = PunchyAnimationManager.resolveRenderStack(player, arm, baseStack);
        boolean left = arm == class_1306.field_6182;
        if (stack == null || stack.method_7960()) {
            toolKind = VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND;
        } else {
            toolKind = VanillaFirstPersonItemLayer.ToolKind.INGREDIENT;
        }
        VanillaFirstPersonItemLayer.ToolKind fallbackKind = toolKind;
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, fallbackKind);
        return ToolTuning.resolveProfile(stack, kind, left);
    }

    private float getArmBlendRemaining(class_1306 arm) {
        return arm == class_1306.field_6182 ? this.leftArmBlendRemaining : this.rightArmBlendRemaining;
    }

    private float getArmBlendPrevRemaining(class_1306 arm) {
        return arm == class_1306.field_6182 ? this.leftArmBlendPrevRemaining : this.rightArmBlendPrevRemaining;
    }

    private void setArmBlendRemaining(class_1306 arm, float value) {
        if (arm == class_1306.field_6182) {
            this.leftArmBlendRemaining = value;
        } else {
            this.rightArmBlendRemaining = value;
        }
    }

    private void setArmBlendPrevRemaining(class_1306 arm, float value) {
        if (arm == class_1306.field_6182) {
            this.leftArmBlendPrevRemaining = value;
        } else {
            this.rightArmBlendPrevRemaining = value;
        }
    }

    private boolean isBoneForArm(String boneName, class_1306 arm) {
        if (boneName == null) {
            return true;
        }
        boolean isLeft = isLeftBone(boneName);
        boolean isRight = isRightBone(boneName);
        if (isLeft || isRight) {
            return arm == class_1306.field_6182 ? isLeft : isRight;
        }
        return true;
    }

    private class_1306 resolveBoneArm(String boneName) {
        if (boneName == null) {
            return null;
        }
        boolean isLeft = isLeftBone(boneName);
        boolean isRight = isRightBone(boneName);
        if (isLeft && !isRight) {
            return class_1306.field_6182;
        }
        if (isRight && !isLeft) {
            return class_1306.field_6183;
        }
        return null;
    }

    private boolean isLeftBone(String name) {
        return name.startsWith("left_") || name.endsWith("_left");
    }

    private boolean isRightBone(String name) {
        return name.startsWith("right_") || name.endsWith("_right");
    }

    private String oppositeBoneName(String name) {
        if (name.startsWith("left_")) {
            return "right_" + name.substring("left_".length());
        }
        if (name.startsWith("right_")) {
            return "left_" + name.substring("right_".length());
        }
        if (name.endsWith("_left")) {
            return name.substring(DEBUG_COMPAT_IDLE, name.length() - "_left".length()) + "_right";
        }
        if (name.endsWith("_right")) {
            return name.substring(DEBUG_COMPAT_IDLE, name.length() - "_right".length()) + "_left";
        }
        return name;
    }

    private boolean isDualHandedActive(float time) {
        return isDualHandedActive(this.currentClip, time);
    }

    private boolean isDualHandedActive(AnimationClip clip, float time) {
        if (this.ignoreDualHanded || clip == null || !PunchyAnimationManager.isDualHandedActive(clip, time)) {
            return false;
        }
        return isConditionalOffhandWindowActive(clip, time);
    }

    private boolean isConditionalOffhandWindowActive(AnimationClip clip, float time) {
        if (clip == null) {
            return false;
        }
        if (isRealOffhandEmpty()) {
            return true;
        }
        float lastStart = resolveLastOffhandStartTime(clip, time);
        float lastStop = clip.getLastEffectTime(STOP_WITH_OFFHAND_EFFECT, time);
        boolean hasStart = !Float.isNaN(lastStart);
        boolean hasStop = !Float.isNaN(lastStop);
        if (hasStart || hasStop) {
            return !hasStart ? !hasStop : !hasStop || lastStart > lastStop;
        }
        return true;
    }

    private float resolveLastOffhandStartTime(AnimationClip clip, float time) {
        if (clip == null) {
            return Float.NaN;
        }
        float canonical = clip.getLastEffectTime(START_WITH_OFFHAND_EFFECT, time);
        float alias = clip.getLastEffectTime(START_WITH_OFFHAND_EFFECT_ALIAS, time);
        if (Float.isNaN(canonical)) {
            return alias;
        }
        if (Float.isNaN(alias)) {
            return canonical;
        }
        return Math.max(canonical, alias);
    }

    private float resolveFirstOffhandStartTime(AnimationClip clip) {
        if (clip == null) {
            return Float.NaN;
        }
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffects().entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                for (String effect : entry.getValue()) {
                    if (effect != null) {
                        String normalized = effect.trim().toLowerCase(Locale.ROOT);
                        if (START_WITH_OFFHAND_EFFECT.equals(normalized) || START_WITH_OFFHAND_EFFECT_ALIAS.equals(normalized)) {
                            return entry.getKey().floatValue();
                        }
                    }
                }
            }
        }
        return Float.NaN;
    }

    private float resolveConditionalDualStartTime(AnimationClip clip) {
        if (clip == null || isRealOffhandEmpty() || !PunchyAnimationManager.hasAnyDualHandedEffect(clip)) {
            return 0.0f;
        }
        float start = resolveFirstOffhandStartTime(clip);
        if (Float.isNaN(start) || start <= 0.0f) {
            return 0.0f;
        }
        float length = clip.getLength();
        if (length <= 0.0f) {
            return 0.0f;
        }
        return clip.isLoop() ? start % length : Math.min(start, length);
    }
}
