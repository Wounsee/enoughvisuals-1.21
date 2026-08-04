package punchy.client.animation.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.class_2960;
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

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyThirdPersonAnimationLoader.class */
public class PunchyThirdPersonAnimationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("Punchy/ThirdPersonAnimationLoader");

    public static Map<String, AnimationClip> loadAll(class_3300 manager) {
        Map<String, AnimationClip> result = new HashMap<>();
        if (manager == null) {
            return result;
        }
        loadFromManager(manager, class_2960.method_60655(Punchy.MOD_ID, "animations/player_3_p.animation.json"), result);
        if (result.isEmpty()) {
            loadFromManager(manager, class_2960.method_60655("minecraft", "punchy/animations/player_3_p.animation.json"), result);
        }
        return result;
    }

    private static void loadFromManager(class_3300 manager, class_2960 id, Map<String, AnimationClip> result) {
        if (manager == null || id == null || result == null) {
            return;
        }
        try {
            class_3298 resource = manager.getResourceOrThrow(id);
            if (resource == null) {
                return;
            }
            loadFromResource(resource, result);
        } catch (Exception e) {
        }
    }

    private static void loadFromResource(class_3298 resource, Map<String, AnimationClip> result) {
        if (resource == null || result == null) {
            return;
        }
        try {
            InputStreamReader reader = new InputStreamReader(resource.method_14482());
            try {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animationsObj = json.getAsJsonObject("animations");
                if (animationsObj != null) {
                    for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
                        if (entry != null && entry.getKey() != null && entry.getValue().isJsonObject()) {
                            AnimationClip clip = buildClip(entry.getKey(), entry.getValue().getAsJsonObject());
                            if (clip != null) {
                                result.put(entry.getKey(), clip);
                            }
                        }
                    }
                    reader.close();
                    return;
                }
                reader.close();
            } finally {
            }
        } catch (Exception e) {
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
                clip.addTrack(parseBoneTrack(boneName, boneData));
            }
        }
        PunchyAnimationLoader.parseEffects(clip, animData);
        PunchyAnimationLoader.parseTimelineEffects(clip, animData);
        return clip;
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo.class */
    private static final class LoopInfo extends Record {
        private final boolean loop;
        private final boolean holdOnLastFrame;

        private LoopInfo(boolean loop, boolean holdOnLastFrame) {
            this.loop = loop;
            this.holdOnLastFrame = holdOnLastFrame;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, LoopInfo.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, LoopInfo.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, LoopInfo.class, Object.class), LoopInfo.class, "loop;holdOnLastFrame", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->loop:Z", "FIELD:Lpunchy/client/animation/io/PunchyThirdPersonAnimationLoader$LoopInfo;->holdOnLastFrame:Z").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public boolean loop() {
            return this.loop;
        }

        public boolean holdOnLastFrame() {
            return this.holdOnLastFrame;
        }
    }

    private static BoneTrack parseBoneTrack(String boneName, JsonObject data) {
        BoneTrack track = new BoneTrack(boneName);
        Map<Float, KeyframeBuilder> builders = new TreeMap<>();
        if (data.has("rotation")) {
            JsonElement rotEl = data.get("rotation");
            if (rotEl.isJsonArray()) {
                KeyframeBuilder b = getBuilder(builders, 0.0f);
                b.rotationEuler = parseRotation(rotEl);
                b.rotation = parseEuler(rotEl);
            } else if (rotEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : rotEl.getAsJsonObject().entrySet()) {
                    KeyframeBuilder b2 = getBuilder(builders, Float.parseFloat(entry.getKey()));
                    b2.rotationEuler = parseRotation(entry.getValue());
                    b2.rotation = parseEuler(entry.getValue());
                }
            }
        }
        if (data.has("position")) {
            JsonElement posEl = data.get("position");
            if (posEl.isJsonArray()) {
                getBuilder(builders, 0.0f).position = parsePosition(posEl, boneName);
            } else if (posEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry2 : posEl.getAsJsonObject().entrySet()) {
                    getBuilder(builders, Float.parseFloat(entry2.getKey())).position = parsePosition(entry2.getValue(), boneName);
                }
            }
        }
        if (data.has("scale")) {
            JsonElement scaleEl = data.get("scale");
            if (scaleEl.isJsonArray()) {
                getBuilder(builders, 0.0f).scale = parseScale(scaleEl, boneName);
            } else if (scaleEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry3 : scaleEl.getAsJsonObject().entrySet()) {
                    getBuilder(builders, Float.parseFloat(entry3.getKey())).scale = parseScale(entry3.getValue(), boneName);
                }
            }
        }
        for (Map.Entry<Float, KeyframeBuilder> entry4 : builders.entrySet()) {
            KeyframeBuilder b3 = entry4.getValue();
            Vector3f pos = b3.position != null ? new Vector3f(b3.position) : null;
            Vector3f rotEuler = b3.rotationEuler != null ? new Vector3f(b3.rotationEuler) : null;
            Quaternionf rot = b3.rotation != null ? new Quaternionf(b3.rotation) : null;
            Vector3f scl = b3.scale != null ? new Vector3f(b3.scale) : null;
            track.addKeyframe(new Keyframe(entry4.getKey().floatValue(), pos, rotEuler, rot, scl, "linear"));
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

    private static Quaternionf parseEuler(JsonElement el) {
        Vector3f e = parseRotation(el);
        float rx = (float) Math.toRadians(e.x);
        float ry = (float) Math.toRadians(e.y);
        float rz = (float) Math.toRadians(e.z);
        return new Quaternionf().rotationZYX(rz, ry, rx);
    }

    private static Vector3f parseRotation(JsonElement el) {
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

    private static void loadBundledDefaults(Map<String, AnimationClip> result) {
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/io/PunchyThirdPersonAnimationLoader$KeyframeBuilder.class */
    private static class KeyframeBuilder {
        Vector3f position;
        Vector3f rotationEuler;
        Quaternionf rotation;
        Vector3f scale;

        private KeyframeBuilder() {
        }
    }
}
