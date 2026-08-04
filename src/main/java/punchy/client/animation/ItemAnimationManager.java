package punchy.client.animation;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_5321;
import net.minecraft.class_630;
import net.minecraft.class_638;
import net.minecraft.class_6862;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.data.AnimationClip;
import punchy.client.bedrockitems.BedrockItemAnimationManager;
import punchy.client.modelparts.ModelPartsParticleSystem;
import punchy.client.render.ToolTuning;
import punchy.client.state.MovementStateMachine;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/ItemAnimationManager.class */
public final class ItemAnimationManager {
    private static final EnumMap<class_1306, PoseHandler> HANDLERS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, AnimationClip> ACTIVE_CLIPS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, String> ACTIVE_TYPES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, PunchyTuningConfig.CustomAnimationEntry> ACTIVE_ENTRIES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Boolean> SWIM_SUPPRESSION = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, AnimationClip> IDLE_CLIPS = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, Float> IDLE_TIMES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1268, ForcedAction> FORCED_TYPES = new EnumMap<>(class_1268.class);
    private static final EnumMap<class_1268, PersistentAction> PERSISTENT_ACTIONS = new EnumMap<>(class_1268.class);
    private static final float ITEM_BLEND_IN_SECONDS = 0.0f;
    private static final float ITEM_BLEND_TRANSITION_SECONDS = 0.4f;
    private static final float ITEM_BLEND_OUT_SECONDS = 3.0f;
    private static final float ITEM_BLEND_OUT_SLOW = 3.6f;
    private static final float ITEM_BASE_DELTA_SECONDS = 0.05f;

    private ItemAnimationManager() {
    }

    public static void forceTypeOnce(class_1268 hand, String type) {
        forceTypeOnce(hand, type, false);
    }

    public static void forceTypeOnce(class_1268 hand, String type, boolean immediate) {
        if (type == null || type.isBlank()) {
            return;
        }
        if (hand == null) {
            FORCED_TYPES.put(class_1268.field_5808, new ForcedAction(type, immediate));
            FORCED_TYPES.put(class_1268.field_5810, new ForcedAction(type, immediate));
            BedrockItemAnimationManager.forceTypeOnce(class_1268.field_5808, type, immediate);
            BedrockItemAnimationManager.forceTypeOnce(class_1268.field_5810, type, immediate);
            return;
        }
        FORCED_TYPES.put(hand, new ForcedAction(type, immediate));
        BedrockItemAnimationManager.forceTypeOnce(hand, type, immediate);
    }

    public static void setPersistentType(class_1268 hand, String type, Boolean loopOverride) {
        setPersistentType(hand, type, loopOverride, false);
    }

    public static void setPersistentType(class_1268 hand, String type, Boolean loopOverride, boolean immediate) {
        if (hand == null || type == null || type.isBlank()) {
            return;
        }
        PERSISTENT_ACTIONS.put(hand, new PersistentAction(type, loopOverride, immediate));
        BedrockItemAnimationManager.setPersistentType(hand, type, loopOverride, immediate);
    }

    public static void clearPersistentType(class_1268 hand, String type) {
        PersistentAction current;
        if (hand == null || (current = PERSISTENT_ACTIONS.get(hand)) == null) {
            return;
        }
        if (type == null || type.isBlank() || type.equals(current.type)) {
            PERSISTENT_ACTIONS.remove(hand);
            BedrockItemAnimationManager.clearPersistentType(hand, type);
        }
    }

    public static void clearAllPersistentTypes() {
        PERSISTENT_ACTIONS.clear();
        BedrockItemAnimationManager.clearAllPersistentTypes();
    }

    public static boolean isTypeActive(String type) {
        PoseHandler handler;
        if (type == null || type.isBlank()) {
            return false;
        }
        for (class_1306 arm : class_1306.values()) {
            String activeType = ACTIVE_TYPES.get(arm);
            if (type.equals(activeType) && (handler = HANDLERS.get(arm)) != null && handler.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyDualHandedActive() {
        for (class_1306 arm : class_1306.values()) {
            PoseHandler handler = HANDLERS.get(arm);
            if (handler != null) {
                if (handler.isPlaying() && handler.isCurrentDualHandedActive()) {
                    return true;
                }
                AnimationClip secondary = handler.getSecondaryClip();
                if (secondary != null && PunchyAnimationManager.isDualHandedActive(secondary, handler.getSecondaryTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX WARN: Failed to analyze thrown exceptions
    java.util.ConcurrentModificationException
    	at java.base/java.util.ArrayList$Itr.checkForComodification(Unknown Source)
    	at java.base/java.util.ArrayList$Itr.next(Unknown Source)
    	at jadx.core.dex.visitors.MethodThrowsVisitor.processInstructions(MethodThrowsVisitor.java:130)
    	at jadx.core.dex.visitors.MethodThrowsVisitor.visit(MethodThrowsVisitor.java:68)
    	at jadx.core.dex.visitors.MethodThrowsVisitor.checkInsn(MethodThrowsVisitor.java:178)
    	at jadx.core.dex.visitors.MethodThrowsVisitor.processInstructions(MethodThrowsVisitor.java:131)
    	at jadx.core.dex.visitors.MethodThrowsVisitor.visit(MethodThrowsVisitor.java:68)
     */
    public static void tick(class_310 client) {
        boolean zShouldLoop;
        if (client == null || client.field_1724 == null) {
            return;
        }
        float speed = PunchyAnimationManager.ANIMATION_SPEED;
        class_1306[] class_1306VarArrValues = class_1306.values();
        int length = class_1306VarArrValues.length;
        for (int i = 0; i < length; i++) {
            class_1306 arm = class_1306VarArrValues[i];
            SWIM_SUPPRESSION.put(arm, false);
            PoseHandler handler = (PoseHandler) HANDLERS.computeIfAbsent(arm, key -> {
                return new PoseHandler();
            });
            boolean javaBlock = false;
            class_1799 stack = resolveStack(client, arm);
            class_1268 hand = PunchyAnimationManager.getHandForArm(client, arm);
            if (PunchyConfig.isHandBlacklisted(client.field_1724, hand)) {
                FORCED_TYPES.remove(hand);
                PERSISTENT_ACTIONS.remove(hand);
                IDLE_CLIPS.remove(arm);
                IDLE_TIMES.remove(arm);
                if (handler.isPlaying()) {
                    logPlaybackOnce(arm, stack, ACTIVE_TYPES.get(arm), ACTIVE_CLIPS.get(arm), ACTIVE_ENTRIES.get(arm), null, false, false, "blacklist_stop");
                    handler.setLooping(false);
                    handler.stopImmediateWithBlendOut();
                }
                ACTIVE_CLIPS.put(arm, (AnimationClip) null);
                ACTIVE_TYPES.remove(arm);
                ACTIVE_ENTRIES.remove(arm);
                ModelPartsParticleSystem.resetArm(arm);
            } else {
                if (hasItemSuppression(stack, true)) {
                    SWIM_SUPPRESSION.put(arm, true);
                }
                ActionState desiredState = resolveDesiredState(client, arm, stack);
                String type = desiredState.type;
                AnimationClip desired = resolveDesiredClip(client, arm, stack, type);
                AnimationClip idleClip = resolveDesiredClip(client, arm, stack, "idle");
                float idleTime = updateIdleTime(arm, handler, idleClip, ITEM_BASE_DELTA_SECONDS, speed);
                PunchyTuningConfig.CustomAnimationEntry entry = resolveEntry(stack, type, client, arm);
                if (entry != null && entry.isJavaBlock != null) {
                    javaBlock = entry.isJavaBlock.booleanValue();
                }
                if (entry != null) {
                    handler.setLoopSpeedParams(entry.loop_speed_max, entry.loop_speed_ramp);
                } else {
                    handler.resetLoopSpeedParams();
                }
                if (entry != null && Boolean.TRUE.equals(entry.disable_swim_animation)) {
                    SWIM_SUPPRESSION.put(arm, true);
                }
                handler.setSwapTranslationYZ(true);
                handler.setJavaBlock(javaBlock);
                AnimationClip active = ACTIVE_CLIPS.get(arm);
                String activeType = ACTIVE_TYPES.get(arm);
                boolean allowActiveToFinish = (active == null || !handler.isPlaying() || handler.getCurrentClip() != active || active.isLoop() || active.isHoldOnLastFrame() || handler.isForceLooping() || (desired != null && !"idle".equals(type)) || "falling".equals(activeType)) ? false : true;
                if (allowActiveToFinish) {
                    desired = active;
                }
                if (!stack.method_31574(class_1802.field_8378) && shouldSuppressMeshAnimation(desired)) {
                    if (active != null) {
                        logPlaybackOnce(arm, stack, activeType, active, ACTIVE_ENTRIES.get(arm), null, false, false, "mesh_suppressed");
                        handler.setBlendOutDurationNext(ITEM_BLEND_OUT_SECONDS);
                        handler.setBlendOutSlowNext(ITEM_BLEND_OUT_SLOW);
                        handler.setLooping(false);
                        handler.stopImmediateWithBlendOut();
                        ACTIVE_CLIPS.put(arm, (AnimationClip) null);
                        ACTIVE_TYPES.remove(arm);
                    }
                    ModelPartsParticleSystem.clearArmAnimationState(arm);
                } else {
                    if (desired != active) {
                        PunchyTuningConfig.CustomAnimationEntry activeEntry = ACTIVE_ENTRIES.get(arm);
                        boolean forceCutFromActive = activeEntry != null && Boolean.TRUE.equals(activeEntry.skip_blend_out);
                        if (desired != null) {
                            handler.setActiveArm(null);
                            if (desiredState.loopOverride != null) {
                                zShouldLoop = desiredState.loopOverride.booleanValue();
                            } else {
                                zShouldLoop = shouldLoop(desired, type);
                            }
                            boolean loop = zShouldLoop;
                            handler.setLooping(loop);
                            float speedMultiplier = PunchyAnimationManager.clipSpeedMultiplier(desired);
                            boolean hasPrevious = handler.isPlaying() && handler.getCurrentClip() != null;
                            boolean isIdleDesired = desired == idleClip;
                            boolean skipBlendIn = entry != null && Boolean.TRUE.equals(entry.skip_blend_in);
                            if (forceCutFromActive) {
                                skipBlendIn = true;
                            }
                            if (skipBlendIn) {
                                handler.setBlendInDurationNext(0.0f);
                            }
                            if (desiredState.immediate) {
                                if (hasPrevious) {
                                    if (!skipBlendIn) {
                                        handler.setBlendInDurationNext(ITEM_BLEND_TRANSITION_SECONDS);
                                    }
                                    if (isIdleDesired) {
                                        if (skipBlendIn) {
                                            handler.playAtTime(desired, speedMultiplier, true, desired.isHoldOnLastFrame(), idleTime);
                                        } else {
                                            handler.playAtTimeWithBlendFromCurrent(desired, speedMultiplier, desired.isHoldOnLastFrame(), idleTime);
                                        }
                                    } else if (skipBlendIn) {
                                        handler.play(desired, speedMultiplier, true, desired.isHoldOnLastFrame());
                                    } else {
                                        handler.playWithBlendFromCurrentDynamic(desired, speedMultiplier, desired.isHoldOnLastFrame());
                                    }
                                } else {
                                    if (!skipBlendIn) {
                                        handler.setBlendInDurationNext(0.0f);
                                    }
                                    if (isIdleDesired) {
                                        handler.playAtTime(desired, speedMultiplier, true, desired.isHoldOnLastFrame(), idleTime);
                                    } else {
                                        handler.play(desired, speedMultiplier, true, desired.isHoldOnLastFrame());
                                    }
                                }
                            } else {
                                if (!skipBlendIn) {
                                    handler.setBlendInDurationNext(hasPrevious ? ITEM_BLEND_TRANSITION_SECONDS : 0.0f);
                                }
                                if (isIdleDesired) {
                                    if (skipBlendIn) {
                                        handler.playAtTime(desired, speedMultiplier, true, desired.isHoldOnLastFrame(), idleTime);
                                    } else {
                                        handler.playAtTimeWithBlendFromCurrent(desired, speedMultiplier, desired.isHoldOnLastFrame(), idleTime);
                                    }
                                } else if (skipBlendIn) {
                                    handler.play(desired, speedMultiplier, true, desired.isHoldOnLastFrame());
                                } else {
                                    handler.playWithBlendFromCurrentDynamic(desired, speedMultiplier, desired.isHoldOnLastFrame());
                                }
                            }
                            if (!isIdleDesired && (activeType == null || !activeType.equals(type))) {
                                MovementStateMachine.get().triggerActionAnimation(arm);
                            }
                            ACTIVE_TYPES.put(arm, type);
                            ACTIVE_ENTRIES.put(arm, entry);
                            logPlaybackOnce(arm, stack, type, desired, entry, PunchyTuningConfig.getMixPreferredOptionForEntry(stack, entry), loop, desiredState.immediate, hasPrevious ? "switch" : "start");
                        } else if (active != null) {
                            boolean skipBlendOut = activeEntry != null && Boolean.TRUE.equals(activeEntry.skip_blend_out);
                            logPlaybackOnce(arm, stack, activeType, active, activeEntry, PunchyTuningConfig.getMixPreferredOptionForEntry(stack, activeEntry), false, false, skipBlendOut ? "stop_skip_blend" : "stop_blend_out");
                            if (skipBlendOut) {
                                handler.setBlendOutDurationNext(0.0f);
                            } else {
                                handler.setBlendOutDurationNext(ITEM_BLEND_OUT_SECONDS);
                            }
                            handler.setBlendOutSlowNext(ITEM_BLEND_OUT_SLOW);
                            handler.setLooping(false);
                            handler.stopImmediateWithBlendOut();
                            ACTIVE_TYPES.remove(arm);
                            ACTIVE_ENTRIES.remove(arm);
                        }
                        ACTIVE_CLIPS.put(arm, desired);
                    }
                    handler.setMirrorX(arm == class_1306.field_6182);
                    syncIdleOverlay(handler, arm, idleClip, idleTime, ACTIVE_CLIPS.get(arm));
                    handler.tick(ITEM_BASE_DELTA_SECONDS, speed);
                    ModelPartsParticleSystem.onHandlerTick(client, arm, handler);
                }
            }
        }
    }

    public static void applyToParts(class_1306 arm, Map<String, class_630> parts, float partialTicks) {
        PoseHandler handler;
        if (arm == null || parts == null || parts.isEmpty() || (handler = HANDLERS.get(arm)) == null) {
            return;
        }
        handler.setMirrorX(arm == class_1306.field_6182);
        handler.applyTransformations(parts, partialTicks, true);
    }

    private static AnimationClip resolveDesiredClip(class_310 client, class_1306 arm, class_1799 stack, String type) {
        if (type == null || stack == null || stack.method_7960()) {
            return null;
        }
        PunchyTuningConfig.CustomAnimationEntry entry = resolveEntry(stack, type, client, arm);
        String name = resolveEntryName(entry);
        class_1268 hand = PunchyAnimationManager.getHandForArm(client, arm);
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForEntry(stack, entry);
        if (name == null || name.isBlank()) {
            PunchyAnimationManager.ClipResolution fallback = PunchyAnimationManager.resolveTypeFallbackClipWithSource(type, client, hand, preferredOption);
            if (fallback != null && fallback.clip() != null) {
                logResolutionOnce(arm, stack, type, type, preferredOption, entry, fallback);
                return fallback.clip();
            }
            return null;
        }
        if (arm != null) {
            String suffix = arm == class_1306.field_6182 ? "_left" : "_right";
            PunchyAnimationManager.ClipResolution resolution = PunchyAnimationManager.resolveNamedClipWithSource(name + suffix, client, hand, preferredOption);
            if (resolution != null && resolution.clip() != null) {
                logResolutionOnce(arm, stack, type, name + suffix, preferredOption, entry, resolution);
                return resolution.clip();
            }
        }
        PunchyAnimationManager.ClipResolution resolution2 = PunchyAnimationManager.resolveNamedClipWithSource(name, client, hand, preferredOption);
        if (resolution2 != null && resolution2.clip() != null) {
            logResolutionOnce(arm, stack, type, name, preferredOption, entry, resolution2);
            return resolution2.clip();
        }
        return null;
    }

    private static void logResolutionOnce(class_1306 arm, class_1799 stack, String type, String clipLookupName, String preferredOption, PunchyTuningConfig.CustomAnimationEntry entry, PunchyAnimationManager.ClipResolution resolution) {
    }

    private static void logPlaybackOnce(class_1306 arm, class_1799 stack, String type, AnimationClip clip, PunchyTuningConfig.CustomAnimationEntry entry, String preferredOption, boolean loop, boolean immediate, String note) {
    }

    private static float updateIdleTime(class_1306 arm, PoseHandler handler, AnimationClip idleClip, float baseDeltaSeconds, float speed) {
        float idleTime;
        if (idleClip == null) {
            IDLE_CLIPS.remove(arm);
            IDLE_TIMES.remove(arm);
            return 0.0f;
        }
        AnimationClip previous = IDLE_CLIPS.put(arm, idleClip);
        Float stored = IDLE_TIMES.get(arm);
        float idleTime2 = stored == null ? 0.0f : stored.floatValue();
        if (previous != idleClip) {
            idleTime2 = 0.0f;
        }
        if (handler != null && handler.isPlaying() && handler.getCurrentClip() == idleClip) {
            idleTime = handler.getCurrentTime();
        } else if (handler != null && handler.getSecondaryClip() == idleClip) {
            idleTime = handler.getSecondaryTime();
        } else {
            float delta = baseDeltaSeconds * speed * PunchyAnimationManager.clipSpeedMultiplier(idleClip);
            idleTime = advanceLoopTime(idleClip, idleTime2, delta);
        }
        IDLE_TIMES.put(arm, Float.valueOf(idleTime));
        return idleTime;
    }

    private static void syncIdleOverlay(PoseHandler handler, class_1306 arm, AnimationClip idleClip, float idleTime, AnimationClip primaryClip) {
        if (handler == null) {
            return;
        }
        if (idleClip == null || primaryClip == null || primaryClip == idleClip) {
            if (handler.getSecondaryClip() != null) {
                handler.clearSecondaryClip();
            }
        } else if (handler.getSecondaryClip() != idleClip) {
            handler.setSecondaryClipAtTime(idleClip, arm, PunchyAnimationManager.clipSpeedMultiplier(idleClip), idleTime);
        }
    }

    private static float advanceLoopTime(AnimationClip clip, float time, float delta) {
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

    private static ActionState resolveDesiredState(class_310 client, class_1306 arm, class_1799 stack) {
        if (client == null || client.field_1724 == null) {
            return ActionState.EMPTY;
        }
        if (!shouldAnimateArm(arm)) {
            return ActionState.EMPTY;
        }
        ForcedAction forced = consumeForcedTypeForArm(client, arm);
        if (forced != null) {
            return new ActionState(resolveTypeWithFallback(stack, forced.type), null, forced.immediate);
        }
        PersistentAction persistent = getPersistentForArm(client, arm);
        if (persistent != null) {
            return new ActionState(resolveTypeWithFallback(stack, persistent.type), persistent.loopOverride, persistent.immediate);
        }
        return new ActionState(resolveTypeWithFallback(stack, "idle"), null, false);
    }

    private static ForcedAction consumeForcedTypeForArm(class_310 client, class_1306 arm) {
        class_1268 hand = PunchyAnimationManager.getHandForArm(client, arm);
        return FORCED_TYPES.remove(hand);
    }

    private static PersistentAction getPersistentForArm(class_310 client, class_1306 arm) {
        class_1268 hand = PunchyAnimationManager.getHandForArm(client, arm);
        return PERSISTENT_ACTIONS.get(hand);
    }

    private static boolean shouldAnimateArm(class_1306 arm) {
        if (arm == null) {
            return false;
        }
        return true;
    }

    private static class_1799 resolveStack(class_310 client, class_1306 arm) {
        if (client == null || client.field_1724 == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = client.field_1724.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? client.field_1724.method_6047() : client.field_1724.method_6079() : mainRight ? client.field_1724.method_6079() : client.field_1724.method_6047();
    }

    private static boolean shouldLoop(AnimationClip clip, String type) {
        if (clip == null || type == null) {
            return false;
        }
        return "mining".equals(type) || "useItem".equals(type) || "idle".equals(type);
    }

    private static boolean shouldSuppressMeshAnimation(AnimationClip desired) {
        AnimationClip poseClip;
        if (desired == null) {
            return false;
        }
        if ((!desired.hasEffectAny("suppress_mesh_animation") && !desired.hasEffectAny("start_mesh_animation") && !desired.hasEffectAny("loadstart") && !desired.hasEffectAny("load_start")) || (poseClip = PunchyAnimationManager.POSE_HANDLER.getCurrentClip()) == null || poseClip != desired) {
            return false;
        }
        float time = PunchyAnimationManager.POSE_HANDLER.getCurrentTime();
        float lastSuppress = getLastEffectTime(desired, "suppress_mesh_animation", time);
        float lastStart = Math.max(getLastEffectTime(desired, "start_mesh_animation", time), Math.max(getLastEffectTime(desired, "loadstart", time), getLastEffectTime(desired, "load_start", time)));
        return lastSuppress > lastStart || lastStart < 0.0f;
    }

    private static float getLastEffectTime(AnimationClip clip, String effect, float time) {
        if (clip == null || effect == null || effect.isEmpty()) {
            return Float.NEGATIVE_INFINITY;
        }
        if (clip.hasEffect(effect)) {
            return Float.POSITIVE_INFINITY;
        }
        float lastTime = Float.NEGATIVE_INFINITY;
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffectsUpTo(time)) {
            if (entry.getValue().contains(effect)) {
                lastTime = entry.getKey().floatValue();
            }
        }
        return lastTime;
    }

    private static String resolveTypeWithFallback(class_1799 stack, String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String preferredOption = PunchyTuningConfig.getMixPreferredOptionForStack(stack);
        if (hasEntry(stack, type) || PunchyAnimationManager.hasTypeFallbackClip(type, preferredOption)) {
            return type;
        }
        if (!"idle".equals(type)) {
            if (hasEntry(stack, "idle") || PunchyAnimationManager.hasTypeFallbackClip("idle", preferredOption)) {
                return "idle";
            }
            return null;
        }
        return null;
    }

    private static PunchyTuningConfig.CustomAnimationEntry resolveEntry(class_1799 stack, String targetType, class_310 client, class_1306 arm) {
        if (stack == null || stack.method_7960() || targetType == null || targetType.isBlank()) {
            return null;
        }
        List<PunchyTuningConfig.CustomAnimationEntry> entries = ToolTuning.getCustomItemAnimation(stack);
        if (entries == null || entries.isEmpty()) {
            logEntrySelectionOnce(arm, stack, targetType, entries, null, "no_entries");
            return null;
        }
        String normalizedTarget = normalizeType(targetType);
        PunchyTuningConfig.CustomAnimationEntry bestTriggered = null;
        int bestTriggeredScore = -1;
        PunchyTuningConfig.CustomAnimationEntry bestFallback = null;
        int bestFallbackScore = -1;
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && entry.type != null && normalizedTarget.equals(normalizeType(entry.type)) && (entry.items == null || entry.items.isEmpty() || matchesItemList(stack, entry.items))) {
                if (matchesWorldTriggers(entry, client, arm)) {
                    int score = triggerSpecificityScore(entry);
                    if (score > 0) {
                        if (score > bestTriggeredScore) {
                            bestTriggered = entry;
                            bestTriggeredScore = score;
                        }
                    } else if (score > bestFallbackScore) {
                        bestFallback = entry;
                        bestFallbackScore = score;
                    }
                }
            }
        }
        PunchyTuningConfig.CustomAnimationEntry resolved = bestTriggered != null ? bestTriggered : bestFallback;
        logEntrySelectionOnce(arm, stack, targetType, entries, resolved, bestTriggered != null ? "triggered" : "fallback");
        return resolved;
    }

    private static void logEntrySelectionOnce(class_1306 arm, class_1799 stack, String targetType, List<PunchyTuningConfig.CustomAnimationEntry> entries, PunchyTuningConfig.CustomAnimationEntry resolved, String reason) {
    }

    private static String resolveEntryName(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null) {
            return null;
        }
        if (entry.animation != null && !entry.animation.isBlank()) {
            return entry.animation.trim();
        }
        if (entry.name != null && !entry.name.isBlank()) {
            return entry.name.trim();
        }
        if (entry.var_1 != null && !entry.var_1.isBlank()) {
            return entry.var_1.trim();
        }
        return null;
    }

    private static boolean hasEntry(class_1799 stack, String targetType) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries;
        String name;
        if (stack == null || stack.method_7960() || targetType == null || targetType.isBlank() || (entries = ToolTuning.getCustomItemAnimation(stack)) == null || entries.isEmpty()) {
            return false;
        }
        String normalizedTarget = normalizeType(targetType);
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && entry.type != null && normalizedTarget.equals(normalizeType(entry.type)) && (name = resolveEntryName(entry)) != null && !name.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeType(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    public static boolean isSwimSuppressed(class_1306 arm) {
        if (arm == null) {
            return false;
        }
        return Boolean.TRUE.equals(SWIM_SUPPRESSION.get(arm));
    }

    private static boolean hasItemSuppression(class_1799 stack, boolean swim) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries;
        if (stack == null || stack.method_7960() || (entries = ToolTuning.getCustomItemAnimation(stack)) == null || entries.isEmpty()) {
            return false;
        }
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && (entry.items == null || entry.items.isEmpty() || matchesItemList(stack, entry.items))) {
                if (swim && Boolean.TRUE.equals(entry.disable_swim_animation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int triggerSpecificityScore(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null) {
            return 0;
        }
        int score = 0;
        if (entry.in_water != null) {
            score = 0 + 1;
        }
        if (entry.inside_water != null) {
            score++;
        }
        if (entry.block_detection != null) {
            score++;
        }
        if (entry.weather_detection != null) {
            score++;
        }
        if (entry.time_detection != null) {
            score++;
        }
        if (entry.biome_detection != null) {
            score++;
        }
        return score;
    }

    private static boolean matchesWorldTriggers(PunchyTuningConfig.CustomAnimationEntry entry, class_310 client, class_1306 arm) {
        if (entry == null) {
            return false;
        }
        if (entry.in_water == null && entry.inside_water == null && entry.block_detection == null && entry.weather_detection == null && entry.time_detection == null && entry.biome_detection == null) {
            return true;
        }
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_746 player = client.field_1724;
        class_638 class_638Var = client.field_1687;
        if (class_638Var == null) {
            return false;
        }
        if (entry.in_water != null) {
            boolean inWater = isInWaterContact(class_638Var, player);
            if (entry.in_water.booleanValue() != inWater) {
                return false;
            }
        }
        if (entry.inside_water != null) {
            boolean inside = player.method_5777(class_3486.field_15517);
            if (entry.inside_water.booleanValue() != inside) {
                return false;
            }
        }
        if (entry.weather_detection != null && !matchesWeather(entry.weather_detection, class_638Var)) {
            return false;
        }
        if (entry.time_detection != null && !matchesTime(entry.time_detection, class_638Var)) {
            return false;
        }
        if (entry.block_detection != null && !matchesBlock(entry.block_detection, class_638Var, player.method_24515())) {
            return false;
        }
        if (entry.biome_detection != null && !matchesBiome(entry.biome_detection, class_638Var, player.method_24515())) {
            return false;
        }
        return true;
    }

    private static boolean isInWaterContact(class_1937 level, class_1657 player) {
        if (level == null || player == null) {
            return false;
        }
        if (player.method_5799() || player.method_5777(class_3486.field_15517)) {
            return true;
        }
        double x = player.method_23317();
        double y = player.method_23318();
        double z = player.method_23321();
        double[] ys = {y + 0.05d, y + 0.6d, y + 1.1d};
        double[] xs = {x, x - 0.3d, x + 0.3d};
        double[] zs = {z, z - 0.3d, z + 0.3d};
        for (double sampleY : ys) {
            for (double sampleX : xs) {
                for (double sampleZ : zs) {
                    if (level.method_8316(class_2338.method_49637(sampleX, sampleY, sampleZ)).method_15767(class_3486.field_15517)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesWeather(PunchyTuningConfig.WeatherDetection weather, class_1937 level) {
        if (weather == null || level == null) {
            return true;
        }
        boolean raining = level.method_8419();
        boolean thundering = level.method_8546();
        if (weather.raining != null && weather.raining.booleanValue() != raining) {
            return false;
        }
        if (weather.thundering != null && weather.thundering.booleanValue() != thundering) {
            return false;
        }
        if (weather.clear != null) {
            boolean clear = (raining || thundering) ? false : true;
            if (weather.clear.booleanValue() != clear) {
                return false;
            }
            return true;
        }
        return true;
    }

    private static boolean matchesTime(PunchyTuningConfig.TimeDetection time, class_1937 level) {
        if (time == null || level == null) {
            return true;
        }
        int min = time.min != null ? time.min.intValue() : -1;
        int max = time.max != null ? time.max.intValue() : -1;
        if (time.range != null && time.range.size() >= 2) {
            min = time.range.get(0).intValue();
            max = time.range.get(1).intValue();
        }
        if (min < 0 && max < 0) {
            return true;
        }
        int dayTime = (int) (level.method_8532() % 24000);
        return min < 0 ? dayTime <= max : max < 0 ? dayTime >= min : min <= max ? dayTime >= min && dayTime <= max : dayTime >= min || dayTime <= max;
    }

    private static boolean matchesBlock(PunchyTuningConfig.BlockDetection block, class_1937 level, class_2338 origin) {
        if (block == null || level == null || origin == null) {
            return true;
        }
        List<String> targets = block.blocks;
        if ((targets == null || targets.isEmpty()) && block.block != null) {
            targets = List.of(block.block);
        }
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        int radius = block.radius != null ? Math.max(0, block.radius.intValue()) : 0;
        int offX = block.offsetX != null ? block.offsetX.intValue() : 0;
        int offY = block.offsetY != null ? block.offsetY.intValue() : 0;
        int offZ = block.offsetZ != null ? block.offsetZ.intValue() : 0;
        class_2338 center = origin.method_10069(offX, offY, offZ);
        if (radius == 0) {
            return matchesBlockList(level.method_8320(center), targets);
        }
        class_2338.class_2339 cursor = new class_2338.class_2339();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.method_10103(center.method_10263() + dx, center.method_10264() + dy, center.method_10260() + dz);
                    if (matchesBlockList(level.method_8320(cursor), targets)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesBiome(PunchyTuningConfig.BiomeDetection biome, class_1937 level, class_2338 origin) {
        if (biome == null || level == null || origin == null) {
            return true;
        }
        List<String> targets = biome.biomes;
        if ((targets == null || targets.isEmpty()) && biome.biome != null) {
            targets = List.of(biome.biome);
        }
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        int radius = biome.radius != null ? Math.max(0, biome.radius.intValue()) : 0;
        int offX = biome.offsetX != null ? biome.offsetX.intValue() : 0;
        int offY = biome.offsetY != null ? biome.offsetY.intValue() : 0;
        int offZ = biome.offsetZ != null ? biome.offsetZ.intValue() : 0;
        class_2338 center = origin.method_10069(offX, offY, offZ);
        if (radius == 0) {
            return matchesBiomeList(level, center, targets);
        }
        class_2338.class_2339 cursor = new class_2338.class_2339();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.method_10103(center.method_10263() + dx, center.method_10264() + dy, center.method_10260() + dz);
                    if (matchesBiomeList(level, cursor, targets)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesBiomeList(class_1937 level, class_2338 pos, List<String> targets) {
        if (level == null || pos == null || targets == null || targets.isEmpty()) {
            return false;
        }
        class_6880<class_1959> holder = level.method_23753(pos);
        for (String entry : targets) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_1959> tag = class_6862.method_40092(class_7924.field_41236, tagId);
                        if (holder.method_40220(tag)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null) {
                        class_5321<class_1959> key = class_5321.method_29179(class_7924.field_41236, parsed);
                        if (holder.method_40225(key)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesBlockList(class_2680 state, List<String> targets) {
        if (state == null || targets == null || targets.isEmpty()) {
            return false;
        }
        class_2960 id = class_7923.field_41175.method_10221(state.method_26204());
        String current = id == null ? null : id.toString();
        for (String entry : targets) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_2248> tag = class_6862.method_40092(class_7924.field_41254, tagId);
                        if (state.method_26164(tag)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null && current != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean matchesItemList(class_1799 stack, List<String> items) {
        class_2960 itemId;
        if (stack == null || stack.method_7960() || items == null || items.isEmpty() || (itemId = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return false;
        }
        String current = itemId.toString();
        for (String entry : items) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                if (trimmed.startsWith("#")) {
                    class_2960 tagId = class_2960.method_12829(trimmed.substring(1));
                    if (tagId != null) {
                        class_6862<class_1792> tag = class_6862.method_40092(class_7924.field_41197, tagId);
                        if (stack.method_31573(tag)) {
                            return true;
                        }
                    } else {
                        continue;
                    }
                } else {
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/ItemAnimationManager$ActionState.class */
    private static final class ActionState extends Record {
        private final String type;
        private final Boolean loopOverride;
        private final boolean immediate;
        private static final ActionState EMPTY = new ActionState(null, null, false);

        private ActionState(String type, Boolean loopOverride, boolean immediate) {
            this.type = type;
            this.loopOverride = loopOverride;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ActionState.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ActionState.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ActionState.class, Object.class), ActionState.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ActionState;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public Boolean loopOverride() {
            return this.loopOverride;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/ItemAnimationManager$PersistentAction.class */
    private static final class PersistentAction extends Record {
        private final String type;
        private final Boolean loopOverride;
        private final boolean immediate;

        private PersistentAction(String type, Boolean loopOverride, boolean immediate) {
            this.type = type;
            this.loopOverride = loopOverride;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, PersistentAction.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, PersistentAction.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, PersistentAction.class, Object.class), PersistentAction.class, "type;loopOverride;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->loopOverride:Ljava/lang/Boolean;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$PersistentAction;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public Boolean loopOverride() {
            return this.loopOverride;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/ItemAnimationManager$ForcedAction.class */
    private static final class ForcedAction extends Record {
        private final String type;
        private final boolean immediate;

        private ForcedAction(String type, boolean immediate) {
            this.type = type;
            this.immediate = immediate;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ForcedAction.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ForcedAction.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ForcedAction.class, Object.class), ForcedAction.class, "type;immediate", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/ItemAnimationManager$ForcedAction;->immediate:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public boolean immediate() {
            return this.immediate;
        }
    }
}
