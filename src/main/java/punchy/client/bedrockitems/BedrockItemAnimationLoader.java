package punchy.client.bedrockitems;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.data.AnimationExpression;
import punchy.client.animation.data.BoneTrack;
import punchy.client.animation.data.Keyframe;
import punchy.client.animation.data.VectorExpression;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationLoader.class */
final class BedrockItemAnimationLoader {
    private BedrockItemAnimationLoader() {
    }

    static Map<String, AnimationClip> loadAll(class_3300 manager, class_2960 id) {
        Map<String, AnimationClip> result = new HashMap<>();
        if (manager == null || id == null) {
            return result;
        }
        try {
            class_3298 resource = manager.getResourceOrThrow(id);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(resource.method_14482())).getAsJsonObject();
            JsonObject animationsObj = json.getAsJsonObject("animations");
            if (animationsObj == null) {
                return result;
            }
            for (Map.Entry<String, JsonElement> entry : animationsObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    String name = entry.getKey();
                    AnimationClip clip = buildClip(name, entry.getValue().getAsJsonObject());
                    if (clip != null) {
                        result.put(name, clip);
                        result.put(id.method_12836() + ":" + name, clip);
                    }
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private static AnimationClip buildClip(String name, JsonObject animData) {
        BoneTrack track;
        JsonElement loopEl;
        if (animData == null) {
            return null;
        }
        float length = animData.has("animation_length") ? animData.get("animation_length").getAsFloat() : 0.0f;
        boolean loop = false;
        boolean holdOnLastFrame = false;
        if (animData.has("loop") && (loopEl = animData.get("loop")) != null && loopEl.isJsonPrimitive()) {
            JsonPrimitive prim = loopEl.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                loop = prim.getAsBoolean();
            } else if (prim.isString()) {
                String value = prim.getAsString().trim().toLowerCase(Locale.ROOT);
                if ("hold_on_last_frame".equals(value)) {
                    holdOnLastFrame = true;
                    loop = false;
                } else {
                    loop = Boolean.parseBoolean(value);
                }
            }
        }
        AnimationClip clip = new AnimationClip(name, length, loop);
        if (holdOnLastFrame) {
            clip.setHoldOnLastFrame(true);
        }
        if (!animData.has("bones")) {
            return clip;
        }
        JsonObject bonesObj = animData.getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
            if (entry.getValue().isJsonObject() && (track = parseBoneTrack(entry.getKey(), entry.getValue().getAsJsonObject())) != null) {
                clip.addTrack(track);
            }
        }
        parseTimelineEffects(clip, animData);
        return clip;
    }

    private static BoneTrack parseBoneTrack(String boneName, JsonObject data) {
        if (boneName == null || data == null) {
            return null;
        }
        BoneTrack track = new BoneTrack(boneName);
        TreeMap<Float, KeyframeBuilder> builders = new TreeMap<>();
        if (data.has("rotation")) {
            JsonElement rotEl = data.get("rotation");
            if (rotEl.isJsonArray()) {
                KeyframeBuilder b = getBuilder(builders, 0.0f);
                ParsedVec3 parsed = parseRotation(rotEl);
                b.rotationEuler = parsed.value;
                b.rotationExpression = parsed.expression;
                b.rotation = parseEuler(parsed.value);
            } else if (rotEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : rotEl.getAsJsonObject().entrySet()) {
                    KeyframeBuilder b2 = getBuilder(builders, Float.parseFloat(entry.getKey()));
                    ParsedVec3 parsed2 = parseRotation(entry.getValue());
                    b2.rotationEuler = parsed2.value;
                    b2.rotationExpression = parsed2.expression;
                    b2.rotation = parseEuler(parsed2.value);
                }
            }
        }
        if (data.has("position")) {
            JsonElement posEl = data.get("position");
            if (posEl.isJsonArray()) {
                ParsedVec3 parsed3 = parsePosition(posEl);
                KeyframeBuilder b3 = getBuilder(builders, 0.0f);
                b3.position = parsed3.value;
                b3.positionExpression = parsed3.expression;
            } else if (posEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry2 : posEl.getAsJsonObject().entrySet()) {
                    ParsedVec3 parsed4 = parsePosition(entry2.getValue());
                    KeyframeBuilder b4 = getBuilder(builders, Float.parseFloat(entry2.getKey()));
                    b4.position = parsed4.value;
                    b4.positionExpression = parsed4.expression;
                }
            }
        }
        if (data.has("scale")) {
            JsonElement scaleEl = data.get("scale");
            if (scaleEl.isJsonArray()) {
                ParsedVec3 parsed5 = parseScale(scaleEl);
                KeyframeBuilder b5 = getBuilder(builders, 0.0f);
                b5.scale = parsed5.value;
                b5.scaleExpression = parsed5.expression;
            } else if (scaleEl.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry3 : scaleEl.getAsJsonObject().entrySet()) {
                    ParsedVec3 parsed6 = parseScale(entry3.getValue());
                    KeyframeBuilder b6 = getBuilder(builders, Float.parseFloat(entry3.getKey()));
                    b6.scale = parsed6.value;
                    b6.scaleExpression = parsed6.expression;
                }
            }
        }
        for (Map.Entry<Float, KeyframeBuilder> entry4 : builders.entrySet()) {
            KeyframeBuilder b7 = entry4.getValue();
            Vector3f pos = b7.position != null ? new Vector3f(b7.position) : null;
            Vector3f rotEuler = b7.rotationEuler != null ? new Vector3f(b7.rotationEuler) : null;
            Quaternionf rot = b7.rotation != null ? new Quaternionf(b7.rotation) : null;
            Vector3f scl = b7.scale != null ? new Vector3f(b7.scale) : null;
            track.addKeyframe(new Keyframe(entry4.getKey().floatValue(), pos, rotEuler, rot, scl, "linear", b7.positionExpression, b7.rotationExpression, b7.scaleExpression));
        }
        return track;
    }

    private static KeyframeBuilder getBuilder(Map<Float, KeyframeBuilder> map, float time) {
        return map.computeIfAbsent(Float.valueOf(time), k -> {
            return new KeyframeBuilder();
        });
    }

    private static ParsedVec3 parsePosition(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            ValueOrExpr x = parseValue(value.getAsJsonArray().get(0));
            ValueOrExpr y = parseValue(value.getAsJsonArray().get(1));
            ValueOrExpr z = parseValue(value.getAsJsonArray().get(2));
            return new ParsedVec3(new Vector3f(-((float) x.value), (float) y.value, (float) z.value), new VectorExpression(negate(x.expr), y.expr, z.expr));
        }
        return new ParsedVec3(new Vector3f(0.0f), null);
    }

    private static Quaternionf parseEuler(Vector3f e) {
        float rx = (float) Math.toRadians(e.x);
        float ry = (float) Math.toRadians(e.y);
        float rz = (float) Math.toRadians(e.z);
        return new Quaternionf().rotationXYZ(rx, ry, rz);
    }

    private static ParsedVec3 parseRotation(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            ValueOrExpr x = parseValue(value.getAsJsonArray().get(0));
            ValueOrExpr y = parseValue(value.getAsJsonArray().get(1));
            ValueOrExpr z = parseValue(value.getAsJsonArray().get(2));
            return new ParsedVec3(new Vector3f(-((float) x.value), -((float) y.value), (float) z.value), new VectorExpression(negate(x.expr), negate(y.expr), z.expr));
        }
        return new ParsedVec3(new Vector3f(0.0f), null);
    }

    private static ParsedVec3 parseScale(JsonElement el) {
        JsonElement value = unwrapKeyframeElement(el);
        if (value != null && value.isJsonArray()) {
            ValueOrExpr x = parseValue(value.getAsJsonArray().get(0));
            ValueOrExpr y = parseValue(value.getAsJsonArray().get(1));
            ValueOrExpr z = parseValue(value.getAsJsonArray().get(2));
            return new ParsedVec3(new Vector3f((float) x.value, (float) y.value, (float) z.value), new VectorExpression(x.expr, y.expr, z.expr));
        }
        return new ParsedVec3(new Vector3f(1.0f, 1.0f, 1.0f), null);
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

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationLoader$KeyframeBuilder.class */
    private static class KeyframeBuilder {
        Vector3f position;
        Vector3f rotationEuler;
        Quaternionf rotation;
        Vector3f scale;
        VectorExpression positionExpression;
        VectorExpression rotationExpression;
        VectorExpression scaleExpression;

        private KeyframeBuilder() {
        }
    }

    private static ValueOrExpr parseValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new ValueOrExpr(0.0d, null);
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new ValueOrExpr(element.getAsDouble(), null);
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            if (text == null) {
                return new ValueOrExpr(0.0d, null);
            }
            AnimationExpression expr = AnimationExpression.compile(text);
            if (expr != null) {
                return new ValueOrExpr(0.0d, expr);
            }
            return new ValueOrExpr(Double.parseDouble(text), null);
        }
        return new ValueOrExpr(0.0d, null);
    }

    private static AnimationExpression negate(AnimationExpression expr) {
        return AnimationExpression.negated(expr);
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr.class */
    private static final class ValueOrExpr extends Record {
        private final double value;
        private final AnimationExpression expr;

        private ValueOrExpr(double value, AnimationExpression expr) {
            this.value = value;
            this.expr = expr;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ValueOrExpr.class), ValueOrExpr.class, "value;expr", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->value:D", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->expr:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ValueOrExpr.class), ValueOrExpr.class, "value;expr", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->value:D", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->expr:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ValueOrExpr.class, Object.class), ValueOrExpr.class, "value;expr", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->value:D", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ValueOrExpr;->expr:Lpunchy/client/animation/data/AnimationExpression;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public double value() {
            return this.value;
        }

        public AnimationExpression expr() {
            return this.expr;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3.class */
    private static final class ParsedVec3 extends Record {
        private final Vector3f value;
        private final VectorExpression expression;

        private ParsedVec3(Vector3f value, VectorExpression expression) {
            this.value = value;
            this.expression = expression;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ParsedVec3.class), ParsedVec3.class, "value;expression", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->value:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->expression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ParsedVec3.class), ParsedVec3.class, "value;expression", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->value:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->expression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ParsedVec3.class, Object.class), ParsedVec3.class, "value;expression", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->value:Lorg/joml/Vector3f;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemAnimationLoader$ParsedVec3;->expression:Lpunchy/client/animation/data/VectorExpression;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public Vector3f value() {
            return this.value;
        }

        public VectorExpression expression() {
            return this.expression;
        }
    }

    private static void parseTimelineEffects(AnimationClip clip, JsonObject animData) {
        if (clip == null || animData == null || !animData.has("timeline")) {
            return;
        }
        JsonObject timeline = animData.getAsJsonObject("timeline");
        for (Map.Entry<String, JsonElement> entry : timeline.entrySet()) {
            float time = parseTime(entry.getKey());
            collectTimelineEffects(clip, time, entry.getValue());
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
}
