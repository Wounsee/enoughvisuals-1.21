package punchy.client.animation.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/data/AnimationClip.class */
public class AnimationClip {
    private final String name;
    private final float length;
    private final boolean loop;
    private boolean holdOnLastFrame;
    private boolean lengthDefined;
    private boolean loopDefined;
    private final Map<String, BoneTrack> tracks = new HashMap();
    private final Set<String> effects = new HashSet();
    private final TreeMap<Float, Set<String>> timelineEffects = new TreeMap<>();
    private final TreeMap<Float, Set<String>> soundEffects = new TreeMap<>();

    public AnimationClip(String name, float length, boolean loop) {
        this.name = name;
        this.length = length;
        this.loop = loop;
    }

    public void addTrack(BoneTrack track) {
        this.tracks.put(track.getBoneName(), track);
    }

    public BoneTrack getTrack(String boneName) {
        return this.tracks.get(boneName);
    }

    public Map<String, BoneTrack> getTracks() {
        return this.tracks;
    }

    public float getLength() {
        return this.length;
    }

    public boolean isLoop() {
        return this.loop;
    }

    public String getName() {
        return this.name;
    }

    public void setHoldOnLastFrame(boolean holdOnLastFrame) {
        this.holdOnLastFrame = holdOnLastFrame;
    }

    public boolean isHoldOnLastFrame() {
        return this.holdOnLastFrame;
    }

    public void setLengthDefined(boolean lengthDefined) {
        this.lengthDefined = lengthDefined;
    }

    public boolean isLengthDefined() {
        return this.lengthDefined;
    }

    public void setLoopDefined(boolean loopDefined) {
        this.loopDefined = loopDefined;
    }

    public boolean isLoopDefined() {
        return this.loopDefined;
    }

    public void addEffect(String effect) {
        if (effect != null && !effect.isEmpty()) {
            this.effects.add(effect);
        }
    }

    public boolean hasEffect(String effect) {
        return effect != null && this.effects.contains(effect);
    }

    public Set<String> getEffects() {
        return Collections.unmodifiableSet(this.effects);
    }

    public void addTimelineEffect(float time, String effect) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        ((Set) this.timelineEffects.computeIfAbsent(Float.valueOf(time), k -> {
            return new LinkedHashSet();
        })).add(effect);
    }

    public void addSoundEffect(float time, String effect) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        ((Set) this.soundEffects.computeIfAbsent(Float.valueOf(time), k -> {
            return new HashSet();
        })).add(effect);
    }

    public NavigableMap<Float, Set<String>> getSoundEffectsBetween(float start, float end) {
        if (this.soundEffects.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        float safeStart = Math.max(0.0f, start);
        float safeEnd = Math.max(0.0f, end);
        if (safeEnd < safeStart) {
            return Collections.emptyNavigableMap();
        }
        return Collections.unmodifiableNavigableMap(this.soundEffects.subMap(Float.valueOf(safeStart), false, Float.valueOf(safeEnd), true));
    }

    public Set<String> getSoundEffectsAt(float time) {
        if (this.soundEffects.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> effects = this.soundEffects.get(Float.valueOf(time));
        if (effects == null || effects.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(effects);
    }

    public boolean hasEffectAny(String effect) {
        if (effect == null || effect.isEmpty()) {
            return false;
        }
        if (this.effects.contains(effect)) {
            return true;
        }
        for (Set<String> effectsAtTime : this.timelineEffects.values()) {
            if (effectsAtTime.contains(effect)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEffectActive(String effect, float time) {
        if (effect == null || effect.isEmpty()) {
            return false;
        }
        if (this.effects.contains(effect)) {
            return true;
        }
        for (Map.Entry<Float, Set<String>> entry : this.timelineEffects.headMap(Float.valueOf(time), true).entrySet()) {
            if (entry.getValue().contains(effect)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getActiveEffects(float time) {
        Set<String> active = new HashSet<>(this.effects);
        for (Set<String> effectsAtTime : this.timelineEffects.headMap(Float.valueOf(time), true).values()) {
            active.addAll(effectsAtTime);
        }
        return active;
    }

    public Iterable<Map.Entry<Float, Set<String>>> getTimelineEffectsUpTo(float time) {
        NavigableMap<Float, Set<String>> head = this.timelineEffects.headMap(Float.valueOf(time), true);
        return Collections.unmodifiableNavigableMap(head).entrySet();
    }

    public NavigableMap<Float, Set<String>> getTimelineEffects() {
        return Collections.unmodifiableNavigableMap(this.timelineEffects);
    }

    public boolean hasEffectTriggeredBetween(String effect, float startExclusive, float endInclusive) {
        if (effect == null || effect.isEmpty() || this.timelineEffects.isEmpty()) {
            return false;
        }
        if (endInclusive < startExclusive) {
            return hasEffectTriggeredBetween(effect, startExclusive, Float.MAX_VALUE) || hasEffectTriggeredBetween(effect, Float.NEGATIVE_INFINITY, endInclusive);
        }
        NavigableMap<Float, Set<String>> window = this.timelineEffects.subMap(Float.valueOf(startExclusive), false, Float.valueOf(endInclusive), true);
        for (Set<String> effectsAtTime : window.values()) {
            if (effectsAtTime.contains(effect)) {
                return true;
            }
        }
        return false;
    }

    public float getLastEffectTime(String effect, float time) {
        if (effect == null || effect.isEmpty() || this.timelineEffects.isEmpty()) {
            return Float.NaN;
        }
        float last = Float.NaN;
        for (Map.Entry<Float, Set<String>> entry : this.timelineEffects.headMap(Float.valueOf(time), true).entrySet()) {
            if (entry.getValue().contains(effect)) {
                last = entry.getKey().floatValue();
            }
        }
        return last;
    }

    public NavigableMap<Float, Set<String>> getSoundEffects() {
        return Collections.unmodifiableNavigableMap(this.soundEffects);
    }

    public static AnimationClip copyOf(AnimationClip source) {
        if (source == null) {
            return null;
        }
        AnimationClip copy = new AnimationClip(source.getName(), source.getLength(), source.isLoop());
        copy.setHoldOnLastFrame(source.isHoldOnLastFrame());
        copy.setLengthDefined(source.isLengthDefined());
        copy.setLoopDefined(source.isLoopDefined());
        for (BoneTrack track : source.getTracks().values()) {
            if (track != null) {
                copy.addTrack(copyTrack(track));
            }
        }
        for (String effect : source.getEffects()) {
            copy.addEffect(effect);
        }
        for (Map.Entry<Float, Set<String>> entry : source.getTimelineEffects().entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                for (String effect2 : entry.getValue()) {
                    copy.addTimelineEffect(entry.getKey().floatValue(), effect2);
                }
            }
        }
        for (Map.Entry<Float, Set<String>> entry2 : source.getSoundEffects().entrySet()) {
            if (entry2 != null && entry2.getKey() != null && entry2.getValue() != null) {
                for (String effect3 : entry2.getValue()) {
                    copy.addSoundEffect(entry2.getKey().floatValue(), effect3);
                }
            }
        }
        return copy;
    }

    public static AnimationClip merge(AnimationClip base, AnimationClip overlay) {
        if (overlay == null) {
            return copyOf(base);
        }
        if (base == null) {
            return copyOf(overlay);
        }
        String name = (overlay.getName() == null || overlay.getName().isBlank()) ? base.getName() : overlay.getName();
        float length = overlay.isLengthDefined() ? overlay.getLength() : base.getLength();
        boolean loop = overlay.isLoopDefined() ? overlay.isLoop() : base.isLoop();
        AnimationClip merged = new AnimationClip(name, length, loop);
        merged.setLengthDefined(base.isLengthDefined() || overlay.isLengthDefined());
        merged.setLoopDefined(base.isLoopDefined() || overlay.isLoopDefined());
        merged.setHoldOnLastFrame(overlay.isLoopDefined() ? overlay.isHoldOnLastFrame() : base.isHoldOnLastFrame());
        for (BoneTrack track : base.getTracks().values()) {
            if (track != null) {
                merged.addTrack(copyTrack(track));
            }
        }
        for (BoneTrack track2 : overlay.getTracks().values()) {
            if (track2 != null) {
                merged.addTrack(copyTrack(track2));
            }
        }
        for (String effect : base.getEffects()) {
            merged.addEffect(effect);
        }
        for (String effect2 : overlay.getEffects()) {
            merged.addEffect(effect2);
        }
        mergeTimedEffectsInto(merged, base.getTimelineEffects(), false);
        mergeTimedEffectsInto(merged, overlay.getTimelineEffects(), false);
        mergeTimedEffectsInto(merged, base.getSoundEffects(), true);
        mergeTimedEffectsInto(merged, overlay.getSoundEffects(), true);
        return merged;
    }

    private static void mergeTimedEffectsInto(AnimationClip target, NavigableMap<Float, Set<String>> timedEffects, boolean sound) {
        if (target == null || timedEffects == null || timedEffects.isEmpty()) {
            return;
        }
        for (Map.Entry<Float, Set<String>> entry : timedEffects.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                for (String effect : entry.getValue()) {
                    if (sound) {
                        target.addSoundEffect(entry.getKey().floatValue(), effect);
                    } else {
                        target.addTimelineEffect(entry.getKey().floatValue(), effect);
                    }
                }
            }
        }
    }

    private static BoneTrack copyTrack(BoneTrack source) {
        BoneTrack copy = new BoneTrack(source.getBoneName());
        for (Keyframe keyframe : source.getKeyframes()) {
            copy.addKeyframe(copyKeyframe(keyframe));
        }
        return copy;
    }

    private static Keyframe copyKeyframe(Keyframe source) {
        Vector3f position = source.position() == null ? null : new Vector3f(source.position());
        Vector3f rotationEuler = source.rotationEuler() == null ? null : new Vector3f(source.rotationEuler());
        Quaternionf rotation = source.rotation() == null ? null : new Quaternionf(source.rotation());
        Vector3f scale = source.scale() == null ? null : new Vector3f(source.scale());
        return new Keyframe(source.timeStamp(), position, rotationEuler, rotation, scale, source.interpolation(), source.positionV2() == null ? null : new Vector3f(source.positionV2()), source.rotationPreEulerV2() == null ? null : new Vector3f(source.rotationPreEulerV2()), source.rotationPostEulerV2() == null ? null : new Vector3f(source.rotationPostEulerV2()), source.positionExpression(), source.rotationExpression(), source.scaleExpression());
    }
}
