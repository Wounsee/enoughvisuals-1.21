package punchy.client.animation.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3283;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import punchy.Punchy;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.data.BoneTrack;
import punchy.client.animation.data.Keyframe;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyAnimationLoader.class */
public class PunchyAnimationLoader {
    public static final String DEFAULT_PACK_OPTION_ID = "punchy_default";
    private static final String BUILTIN_PACK_OPTION_ID = "pack:punchy:punchy";
    private static final Logger LOGGER = LoggerFactory.getLogger("Punchy/AnimationLoader");
    private static final List<class_2960> INTERNAL_ANIMATION_IDS = List.of(class_2960.method_60655(Punchy.MOD_ID, "animations/player_3_p.animation.json"), class_2960.method_60655("minecraft", "punchy/animations/player.animation.json"));

    public static AnimationClip load(class_3300 manager, class_2960 id, String animationNameInJson) {
        if (manager == null || id == null || animationNameInJson == null || animationNameInJson.isBlank()) {
            return null;
        }
        try {
            class_3298 resource = manager.getResourceOrThrow(id);
            Map<String, AnimationClip> clips = readClipsFromResource(resource);
            return clips.get(animationNameInJson.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, AnimationClip> loadAll(class_3300 manager) {
        Map<String, AnimationClip> selected = new HashMap<>();
        Map<String, LinkedHashMap<String, AnimationClip>> variants = loadAllWithPackVariants(manager);
        for (Map.Entry<String, LinkedHashMap<String, AnimationClip>> entry : variants.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                AnimationClip clip = entry.getValue().get(DEFAULT_PACK_OPTION_ID);
                if (clip == null) {
                    clip = entry.getValue().values().iterator().next();
                }
                if (clip != null) {
                    selected.put(entry.getKey(), AnimationClip.copyOf(clip));
                }
            }
        }
        return selected;
    }

    public static Map<String, LinkedHashMap<String, AnimationClip>> loadAllWithPackVariants(class_3300 manager) {
        Map<String, LinkedHashMap<String, AnimationClip>> result = new HashMap<>();
        if (manager == null) {
            return result;
        }
        List<AnimationResourceEntry> entries = collectAnimationResourceEntries(manager);
        Map<String, AnimationClip> selectedByPriority = new HashMap<>();
        for (AnimationResourceEntry entry : entries) {
            if (entry != null && entry.resource != null) {
                Map<String, AnimationClip> clips = readClipsFromResource(entry.resource);
                if (!clips.isEmpty()) {
                    String optionId = optionIdForAnimationEntry(entry);
                    for (Map.Entry<String, AnimationClip> clipEntry : clips.entrySet()) {
                        if (clipEntry != null && clipEntry.getKey() != null && clipEntry.getValue() != null) {
                            String clipName = clipEntry.getKey().trim();
                            if (!clipName.isBlank()) {
                                LinkedHashMap<String, AnimationClip> variants = result.computeIfAbsent(clipName, ignored -> {
                                    return new LinkedHashMap();
                                });
                                variants.putIfAbsent(optionId, AnimationClip.copyOf(clipEntry.getValue()));
                                selectedByPriority.putIfAbsent(clipName, AnimationClip.copyOf(clipEntry.getValue()));
                            }
                        }
                    }
                }
            }
        }
        for (Map.Entry<String, AnimationClip> entry2 : selectedByPriority.entrySet()) {
            if (entry2 != null && entry2.getKey() != null && entry2.getValue() != null) {
                LinkedHashMap<String, AnimationClip> variants2 = result.computeIfAbsent(entry2.getKey(), ignored2 -> {
                    return new LinkedHashMap();
                });
                LinkedHashMap<String, AnimationClip> ordered = new LinkedHashMap<>();
                ordered.put(DEFAULT_PACK_OPTION_ID, AnimationClip.copyOf(entry2.getValue()));
                for (Map.Entry<String, AnimationClip> variant : variants2.entrySet()) {
                    if (variant != null && variant.getKey() != null && variant.getValue() != null) {
                        ordered.putIfAbsent(variant.getKey(), AnimationClip.copyOf(variant.getValue()));
                    }
                }
                result.put(entry2.getKey(), ordered);
            }
        }
        return result;
    }

    private static String optionIdForAnimationEntry(AnimationResourceEntry entry) {
        if (entry == null || entry.id == null || !Punchy.MOD_ID.equals(entry.id.method_12836()) || entry.id.method_12832() == null || !entry.id.method_12832().startsWith("animations/")) {
            return optionIdFromPackId((entry == null || entry.resource == null) ? null : entry.resource.method_14480());
        }
        return BUILTIN_PACK_OPTION_ID;
    }

    private static List<AnimationResourceEntry> collectAnimationResourceEntries(class_3300 manager) {
        Map<class_2960, Boolean> animationResources = discoverAnimationResourceIds(manager);
        if (animationResources.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> packPriorities = resolvePackPriorities();
        int nextFallbackPriority = packPriorities.values().stream().mapToInt((v0) -> {
            return v0.intValue();
        }).min().orElse(0) - 1;
        int discoveryOrder = 0;
        List<AnimationResourceEntry> entries = new ArrayList<>();
        for (class_2960 id : animationResources.keySet()) {
            List<class_3298> stack = tryGetResourceStack(manager, id);
            if (stack != null && !stack.isEmpty()) {
                int stackOrder = 0;
                for (int i = stack.size() - 1; i >= 0; i--) {
                    class_3298 resource = stack.get(i);
                    if (resource != null) {
                        int i2 = nextFallbackPriority;
                        nextFallbackPriority--;
                        int priority = packPriorities.getOrDefault(resource.method_14480(), Integer.valueOf(i2)).intValue();
                        int i3 = stackOrder;
                        stackOrder++;
                        entries.add(new AnimationResourceEntry(id, resource, priority, discoveryOrder, i3));
                    }
                }
                discoveryOrder++;
            } else {
                try {
                    class_3298 resource2 = manager.getResourceOrThrow(id);
                    int i4 = nextFallbackPriority;
                    nextFallbackPriority--;
                    int priority2 = packPriorities.getOrDefault(resource2.method_14480(), Integer.valueOf(i4)).intValue();
                    entries.add(new AnimationResourceEntry(id, resource2, priority2, discoveryOrder, 0));
                } catch (Exception e) {
                }
                discoveryOrder++;
            }
        }
        entries.sort(Comparator.comparingInt(entry -> {
            return entry.packPriority;
        }).reversed().thenComparingInt(entry2 -> {
            return entry2.discoveryOrder;
        }).thenComparingInt(entry3 -> {
            return entry3.stackOrder;
        }).thenComparing(entry4 -> {
            return entry4.id.toString();
        }));
        return entries;
    }

    private static Map<class_2960, Boolean> discoverAnimationResourceIds(class_3300 manager) {
        Map<class_2960, Boolean> result = new LinkedHashMap<>();
        if (manager == null) {
            return result;
        }
        collectAnimationResourceIds(manager, "animations", Punchy.MOD_ID, "animations/", result);
        collectAnimationResourceIds(manager, "punchy/animations", "minecraft", "punchy/animations/", result);
        return result;
    }

    private static void collectAnimationResourceIds(class_3300 manager, String searchPath, String namespace, String prefix, Map<class_2960, Boolean> result) {
        Map<class_2960, class_3298> found = manager.method_14488(searchPath, loc -> {
            return loc.method_12836().equals(namespace) && loc.method_12832().startsWith(prefix) && loc.method_12832().endsWith(".json");
        });
        for (class_2960 id : found.keySet()) {
            result.putIfAbsent(id, Boolean.TRUE);
        }
    }

    private static List<class_3298> tryGetResourceStack(class_3300 manager, class_2960 id) {
        if (manager == null || id == null) {
            return null;
        }
        try {
            List<class_3298> stack = manager.method_14489(id);
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            return new ArrayList(stack);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Integer> resolvePackPriorities() {
        Map<String, Integer> priorities = new HashMap<>();
        List<String> selected = getSelectedResourcePackIdsHighToLow();
        int weight = selected.size();
        for (String packId : selected) {
            if (packId != null && !packId.isBlank()) {
                int i = weight;
                weight--;
                priorities.putIfAbsent(packId, Integer.valueOf(i));
            }
        }
        return priorities;
    }

    private static List<String> getSelectedResourcePackIdsHighToLow() {
        class_310 client = class_310.method_1551();
        if (client == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        try {
            class_3283 repository = client.method_1520();
            if (repository != null) {
                for (String packId : repository.method_29210()) {
                    if (packId != null && !packId.isBlank()) {
                        result.add(packId);
                    }
                }
            }
        } catch (Exception e) {
        }
        Collections.reverse(result);
        String builtinPackId = resolveActiveBuiltInPackId(client);
        if (builtinPackId != null && !builtinPackId.isBlank() && !result.contains(builtinPackId)) {
            result.add(0, builtinPackId);
        }
        return result;
    }

    private static String resolveActiveBuiltInPackId(class_310 client) {
        if (client == null || client.method_1478() == null) {
            return null;
        }
        try {
            class_2960 id = class_2960.method_60655("minecraft", "punchy/compat/default_tuning.json");
            return (String) client.method_1478().method_14486(id).map((v0) -> {
                return v0.method_14480();
            }).filter(packId -> {
                return (packId == null || packId.isBlank()) ? false : true;
            }).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, AnimationClip> readClipsFromResource(class_3298 resource) {
        InputStreamReader reader;
        JsonObject animationsObj;
        Map<String, AnimationClip> clips = new HashMap<>();
        if (resource == null) {
            return clips;
        }
        try {
            reader = new InputStreamReader(resource.method_14482());
            try {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                animationsObj = json.getAsJsonObject("animations");
            } finally {
            }
        } catch (Exception e) {
        }
        if (animationsObj != null) {
            for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getValue().isJsonObject()) {
                    AnimationClip clip = buildClip(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (clip != null) {
                        clips.put(entry.getKey(), clip);
                    }
                }
            }
            reader.close();
            return clips;
        }
        reader.close();
        return clips;
        return clips;
    }

    private static BoneTrack parseBoneTrack(String boneName, JsonObject data, float animationLength, boolean stretchToLength) {
        Float lastTime;
        KeyframeBuilder last;
        BoneTrack track = new BoneTrack(boneName);
        TreeMap<Float, KeyframeBuilder> builders = new TreeMap<>();
        if (data.has("rotation")) {
            JsonElement rotEl = data.get("rotation");
            if (rotEl.isJsonArray()) {
                KeyframeBuilder b = getBuilder(builders, 0.0f);
                b.rotationEuler = parseRotation(rotEl);
                b.rotation = parseEuler(rotEl);
                Vector3f rotV2 = parseRotationV2(rotEl);
                b.rotationPreEulerV2 = rotV2 != null ? new Vector3f(rotV2) : null;
                b.rotationPostEulerV2 = rotV2 != null ? new Vector3f(rotV2) : null;
            } else if (rotEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : rotEl.getAsJsonObject().entrySet()) {
                    KeyframeBuilder b2 = getBuilder(builders, Float.parseFloat(entry.getKey()));
                    b2.rotationEuler = parseRotation(entry.getValue());
                    b2.rotation = parseEuler(entry.getValue());
                    Vector3f preV2 = parseRotationEndpointV2(entry.getValue(), "pre");
                    Vector3f postV2 = parseRotationEndpointV2(entry.getValue(), "post");
                    if (preV2 != null || postV2 != null) {
                        b2.rotationPreEulerV2 = preV2 != null ? preV2 : postV2 != null ? new Vector3f(postV2) : null;
                        b2.rotationPostEulerV2 = postV2 != null ? postV2 : preV2 != null ? new Vector3f(preV2) : null;
                    } else {
                        Vector3f rotV22 = parseRotationV2(entry.getValue());
                        if (rotV22 != null) {
                            b2.rotationPreEulerV2 = new Vector3f(rotV22);
                            b2.rotationPostEulerV2 = new Vector3f(rotV22);
                        }
                    }
                    String interpolation = readInterpolation(entry.getValue());
                    if (interpolation != null) {
                        b2.interpolation = interpolation;
                    }
                }
            }
        }
        if (data.has("position")) {
            JsonElement posEl = data.get("position");
            if (posEl.isJsonArray()) {
                KeyframeBuilder b3 = getBuilder(builders, 0.0f);
                b3.position = parsePosition(posEl, boneName);
                b3.positionV2 = parsePositionV2(posEl);
            } else if (posEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry2 : posEl.getAsJsonObject().entrySet()) {
                    KeyframeBuilder b4 = getBuilder(builders, Float.parseFloat(entry2.getKey()));
                    b4.position = parsePosition(entry2.getValue(), boneName);
                    b4.positionV2 = parsePositionV2(entry2.getValue());
                    String interpolation2 = readInterpolation(entry2.getValue());
                    if (interpolation2 != null) {
                        b4.interpolation = interpolation2;
                    }
                }
            }
        }
        if (data.has("scale")) {
            JsonElement scaleEl = data.get("scale");
            if (scaleEl.isJsonArray()) {
                getBuilder(builders, 0.0f).scale = parseScale(scaleEl, boneName);
            } else if (scaleEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry3 : scaleEl.getAsJsonObject().entrySet()) {
                    KeyframeBuilder b5 = getBuilder(builders, Float.parseFloat(entry3.getKey()));
                    b5.scale = parseScale(entry3.getValue(), boneName);
                    String interpolation3 = readInterpolation(entry3.getValue());
                    if (interpolation3 != null) {
                        b5.interpolation = interpolation3;
                    }
                }
            }
        }
        if (stretchToLength && animationLength > 0.0f && !builders.isEmpty() && (lastTime = builders.lastKey()) != null && animationLength - lastTime.floatValue() > 1.0E-4f && !builders.containsKey(Float.valueOf(animationLength)) && (last = builders.remove(lastTime)) != null) {
            builders.put(Float.valueOf(animationLength), last);
        }
        for (Map.Entry<Float, KeyframeBuilder> entry4 : builders.entrySet()) {
            KeyframeBuilder b6 = entry4.getValue();
            Vector3f pos = b6.position != null ? new Vector3f(b6.position) : null;
            Vector3f rotEuler = b6.rotationEuler != null ? new Vector3f(b6.rotationEuler) : null;
            Quaternionf rot = b6.rotation != null ? new Quaternionf(b6.rotation) : null;
            Vector3f scl = b6.scale != null ? new Vector3f(b6.scale) : null;
            String interpolation4 = b6.interpolation != null ? b6.interpolation : "linear";
            Vector3f posV2 = b6.positionV2 != null ? new Vector3f(b6.positionV2) : null;
            Vector3f rotPreV2 = b6.rotationPreEulerV2 != null ? new Vector3f(b6.rotationPreEulerV2) : null;
            Vector3f rotPostV2 = b6.rotationPostEulerV2 != null ? new Vector3f(b6.rotationPostEulerV2) : null;
            track.addKeyframe(new Keyframe(entry4.getKey().floatValue(), pos, rotEuler, rot, scl, interpolation4, posV2, rotPreV2, rotPostV2, null, null, null));
        }
        return track;
    }

    private static KeyframeBuilder getBuilder(Map<Float, KeyframeBuilder> map, float time) {
        return map.computeIfAbsent(Float.valueOf(time), k -> {
            return new KeyframeBuilder();
        });
    }

    private static Vector3f parsePosition(JsonElement el, String boneName) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            float x = value.getAsJsonArray().get(0).getAsFloat();
            float y = value.getAsJsonArray().get(1).getAsFloat();
            float z = value.getAsJsonArray().get(2).getAsFloat();
            if (shouldFlipGripZ(boneName)) {
                return new Vector3f(-x, -z, y);
            }
            return new Vector3f(x, -z, -y);
        }
        return new Vector3f(0.0f);
    }

    private static Vector3f parsePositionV2(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            float x = value.getAsJsonArray().get(0).getAsFloat();
            float y = value.getAsJsonArray().get(1).getAsFloat();
            float z = value.getAsJsonArray().get(2).getAsFloat();
            return new Vector3f(x, -y, z);
        }
        return new Vector3f(0.0f);
    }

    private static Quaternionf parseEuler(JsonElement el) {
        Vector3f e = parseRotation(el);
        float rx = (float) Math.toRadians(e.x);
        float ry = (float) Math.toRadians(e.y);
        float rz = (float) Math.toRadians(e.z);
        return new Quaternionf().rotationZYX(rz, ry, rx);
    }

    private static Vector3f parseRotationEndpoint(JsonElement el, String member) {
        JsonElement endpoint;
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (!el.isJsonObject()) {
            return parseRotation(el);
        }
        JsonObject obj = el.getAsJsonObject();
        if (!obj.has(member) || (endpoint = obj.get(member)) == null || endpoint.isJsonNull()) {
            return null;
        }
        return parseRotation(endpoint);
    }

    private static Vector3f parseRotationEndpointV2(JsonElement el, String member) {
        JsonElement endpoint;
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (!el.isJsonObject()) {
            return parseRotationV2(el);
        }
        JsonObject obj = el.getAsJsonObject();
        if (!obj.has(member) || (endpoint = obj.get(member)) == null || endpoint.isJsonNull()) {
            return null;
        }
        return parseRotationV2(endpoint);
    }

    private static Vector3f parseRotation(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            float x = value.getAsJsonArray().get(0).getAsFloat();
            float y = value.getAsJsonArray().get(1).getAsFloat();
            float z = value.getAsJsonArray().get(2).getAsFloat();
            return new Vector3f(x, -z, y);
        }
        return new Vector3f(0.0f);
    }

    private static Vector3f parseRotationV2(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            float x = value.getAsJsonArray().get(0).getAsFloat();
            float y = value.getAsJsonArray().get(1).getAsFloat();
            float z = value.getAsJsonArray().get(2).getAsFloat();
            return new Vector3f(x, y, z);
        }
        return new Vector3f(0.0f);
    }

    private static Vector3f parseScale(JsonElement el, String boneName) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            float x = value.getAsJsonArray().get(0).getAsFloat();
            float y = value.getAsJsonArray().get(1).getAsFloat();
            float z = value.getAsJsonArray().get(2).getAsFloat();
            if (shouldSwapScaleAxes(boneName)) {
                return new Vector3f(x, z, y);
            }
            return new Vector3f(x, y, z);
        }
        return new Vector3f(1.0f, 1.0f, 1.0f);
    }

    private static String readInterpolation(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        JsonObject obj = el.getAsJsonObject();
        if (obj.has("lerp_mode")) {
            return obj.get("lerp_mode").getAsString().trim().toLowerCase(Locale.ROOT);
        }
        if (obj.has("easing")) {
            return obj.get("easing").getAsString().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static boolean shouldSwapScaleAxes(String boneName) {
        if (boneName == null) {
            return false;
        }
        String name = boneName.toLowerCase(Locale.ROOT);
        return name.contains("item");
    }

    private static boolean shouldFlipGripZ(String boneName) {
        if (boneName == null) {
            return false;
        }
        String name = boneName.toLowerCase(Locale.ROOT);
        return name.contains("itemgrip");
    }

    private static JsonElement unwrapKeyframeElement(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (!el.isJsonObject()) {
            return el;
        }
        JsonObject obj = el.getAsJsonObject();
        if (obj.has("post")) {
            return obj.get("post");
        }
        if (obj.has("vector")) {
            return obj.get("vector");
        }
        if (obj.has("pre")) {
            return obj.get("pre");
        }
        return el;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyAnimationLoader$KeyframeBuilder.class */
    private static class KeyframeBuilder {
        Vector3f position;
        Vector3f positionV2;
        Vector3f rotationEuler;
        Quaternionf rotation;
        Vector3f scale;
        String interpolation;
        Vector3f rotationPreEulerV2;
        Vector3f rotationPostEulerV2;

        private KeyframeBuilder() {
        }
    }

    static void parseEffects(AnimationClip clip, JsonObject animData) {
        if (clip == null || animData == null) {
            return;
        }
        if (animData.has("effects")) {
            collectEffectStrings(clip, animData.get("effects"));
        }
        if (animData.has("effect")) {
            collectEffectStrings(clip, animData.get("effect"));
        }
        if (animData.has("events")) {
            collectEffectStrings(clip, animData.get("events"));
        }
        if (animData.has("event")) {
            collectEffectStrings(clip, animData.get("event"));
        }
        if (animData.has("sound_effects")) {
            collectSoundEffects(clip, animData.get("sound_effects"));
        }
    }

    static void parseTimelineEffects(AnimationClip clip, JsonObject animData) {
        if (clip == null || animData == null || !animData.has("timeline")) {
            return;
        }
        JsonObject timeline = animData.getAsJsonObject("timeline");
        for (Map.Entry<String, JsonElement> entry : timeline.entrySet()) {
            float time = parseTime(entry.getKey());
            collectTimelineEffects(clip, time, entry.getValue());
        }
    }

    private static void collectEffectStrings(AnimationClip clip, JsonElement element) {
        if (clip == null || element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value != null && !value.isEmpty()) {
                String[] parts = value.split(";");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        clip.addEffect(trimmed.toLowerCase(Locale.ROOT));
                    }
                }
                return;
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectEffectStrings(clip, child);
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                collectEffectStrings(clip, entry.getValue());
            }
        }
    }

    private static void collectTimelineEffects(AnimationClip clip, float time, JsonElement element) {
        if (clip == null || element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value != null && !value.isEmpty()) {
                String[] parts = value.split(";");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        clip.addTimelineEffect(time, trimmed.toLowerCase(Locale.ROOT));
                    }
                }
                return;
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectTimelineEffects(clip, time, child);
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                collectTimelineEffects(clip, time, entry.getValue());
            }
        }
    }

    private static void collectSoundEffects(AnimationClip clip, JsonElement element) {
        String effect;
        String effect2;
        if (clip == null || element == null || element.isJsonNull() || !element.isJsonObject()) {
            return;
        }
        JsonObject obj = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            float time = parseTime(entry.getKey());
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonObject()) {
                JsonObject soundObj = value.getAsJsonObject();
                if (soundObj.has("effect") && (effect2 = soundObj.get("effect").getAsString()) != null && !effect2.isBlank()) {
                    clip.addSoundEffect(time, effect2.trim());
                }
            } else if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && (effect = value.getAsString()) != null && !effect.isBlank()) {
                clip.addSoundEffect(time, effect.trim());
            }
        }
    }

    private static float parseTime(String key) {
        if (key == null || key.isEmpty()) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(key);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    private static LoopInfo parseLoopInfo(JsonObject animData) {
        boolean loop = false;
        boolean holdOnLastFrame = false;
        if (animData != null && animData.has("loop")) {
            JsonElement loopEl = animData.get("loop");
            if (loopEl.isJsonPrimitive()) {
                if (loopEl.getAsJsonPrimitive().isBoolean()) {
                    loop = loopEl.getAsBoolean();
                } else if (loopEl.getAsJsonPrimitive().isString()) {
                    String value = loopEl.getAsString().toLowerCase(Locale.ROOT).trim();
                    if ("hold_on_last_frame".equals(value)) {
                        holdOnLastFrame = true;
                    } else if ("loop".equals(value) || "true".equals(value)) {
                        loop = true;
                    }
                }
            }
        }
        return new LoopInfo(loop, holdOnLastFrame);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyAnimationLoader$LoopInfo.class */
    private static final class LoopInfo extends Record {
        private final boolean loop;
        private final boolean holdOnLastFrame;

        private LoopInfo(boolean loop, boolean holdOnLastFrame) {
            this.loop = loop;
            this.holdOnLastFrame = holdOnLastFrame;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, LoopInfo.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, LoopInfo.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, LoopInfo.class, Object.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public boolean loop() {
            return this.loop;
        }

        public boolean holdOnLastFrame() {
            return this.holdOnLastFrame;
        }
    }

    private static AnimationClip buildClip(String name, JsonObject animData) {
        if (name == null || animData == null) {
            return null;
        }
        float length = animData.has("animation_length") ? animData.get("animation_length").getAsFloat() : 0.0f;
        LoopInfo loopInfo = parseLoopInfo(animData);
        AnimationClip clip = new AnimationClip(name, length, loopInfo.loop);
        clip.setHoldOnLastFrame(loopInfo.holdOnLastFrame);
        clip.setLengthDefined(animData.has("animation_length"));
        clip.setLoopDefined(animData.has("loop"));
        if (animData.has("bones")) {
            JsonObject bonesObj = animData.getAsJsonObject("bones");
            for (String boneName : bonesObj.keySet()) {
                JsonObject boneData = bonesObj.getAsJsonObject(boneName);
                clip.addTrack(parseBoneTrack(boneName, boneData, length, loopInfo.holdOnLastFrame));
            }
        }
        parseEffects(clip, animData);
        parseTimelineEffects(clip, animData);
        return clip;
    }

    public static String optionIdFromPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            return DEFAULT_PACK_OPTION_ID;
        }
        String normalized = packId.trim().toLowerCase(Locale.ROOT);
        if (isPunchyBuiltInPackId(normalized)) {
            return BUILTIN_PACK_OPTION_ID;
        }
        if ("vanilla".equals(normalized) || "mod_resources".equals(normalized)) {
            return DEFAULT_PACK_OPTION_ID;
        }
        if (normalized.startsWith("pack:")) {
            return normalized;
        }
        return "pack:" + normalized;
    }

    private static boolean isPunchyBuiltInPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }
        String normalized = packId.trim().toLowerCase(Locale.ROOT);
        return Punchy.MOD_ID.equals(normalized) || normalized.startsWith("punchy:") || normalized.endsWith(":punchy") || normalized.endsWith("/punchy") || normalized.endsWith("\\punchy") || "builtin/punchy".equals(normalized) || "builtin\\punchy".equals(normalized);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry.class */
    private static final class AnimationResourceEntry extends Record {
        private final class_2960 id;
        private final class_3298 resource;
        private final int packPriority;
        private final int discoveryOrder;
        private final int stackOrder;

        private AnimationResourceEntry(class_2960 id, class_3298 resource, int packPriority, int discoveryOrder, int stackOrder) {
            this.id = id;
            this.resource = resource;
            this.packPriority = packPriority;
            this.discoveryOrder = discoveryOrder;
            this.stackOrder = stackOrder;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, AnimationResourceEntry.class), AnimationResourceEntry.class, "id;resource;packPriority;discoveryOrder;stackOrder", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->id:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->resource:Lnet/minecraft/class_3298;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->packPriority:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->discoveryOrder:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->stackOrder:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, AnimationResourceEntry.class), AnimationResourceEntry.class, "id;resource;packPriority;discoveryOrder;stackOrder", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->id:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->resource:Lnet/minecraft/class_3298;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->packPriority:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->discoveryOrder:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->stackOrder:I").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, AnimationResourceEntry.class, Object.class), AnimationResourceEntry.class, "id;resource;packPriority;discoveryOrder;stackOrder", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->id:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->resource:Lnet/minecraft/class_3298;", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->packPriority:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->discoveryOrder:I", "FIELD:Lpunchy/client/animation/io/PunchyAnimationLoader$AnimationResourceEntry;->stackOrder:I").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public class_2960 id() {
            return this.id;
        }

        public class_3298 resource() {
            return this.resource;
        }

        public int packPriority() {
            return this.packPriority;
        }

        public int discoveryOrder() {
            return this.discoveryOrder;
        }

        public int stackOrder() {
            return this.stackOrder;
        }
    }
}
