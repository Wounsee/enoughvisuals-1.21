package punchy.client.bedrockitems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import net.minecraft.class_1011;
import net.minecraft.class_1306;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_3532;
import net.minecraft.class_9334;
import org.joml.Quaternionf;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.data.BoneTrack;
import punchy.client.animation.data.Keyframe;
import punchy.client.render.ToolTuning;
import punchy.config.PunchyTuningConfig;
import punchy.util.ResourceLocationCompat;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition.class */
final class BedrockItemDefinition {
    private static final Gson GSON = new GsonBuilder().create();
    private final class_2960 id;
    private final List<String> items;
    private final class_2960 geoId;
    private final class_2960 textureId;
    private final List<class_2960> animationFiles;
    private final List<PunchyTuningConfig.CustomAnimationEntry> modelPartsAnimation;
    private final List<String> definitionParticleEffects;
    private final Map<class_1306, DisplayTransform> displayTransforms;
    private final Map<String, Integer> boneEmission;
    private final Map<String, class_2960> boneTextures;
    private final Map<String, TextureAnimSpec> boneTextureAnim;
    private final List<DynamicBoneTextureSpec> dynamicBoneTextures;
    private final Map<String, BoneMirrorSpec> boneMirror;
    private final Map<String, BonePhysicsSpec> bonePhysics;
    private final Set<String> disablePhysicsBones;
    private final Set<String> cameraTransparentBones;
    private final boolean boneEmissionBlur;
    private final boolean boneEmissionClamp;
    private final int boneEmissionStrength;
    private final int textureAnimFrames;
    private final int textureAnimFrameTime;
    private final int textureAnimFrameHeight;
    private final boolean physicsFeedback;
    private final String customName;
    private BedrockItemGeoModel geoModel;
    private int texturePixelWidth;
    private int texturePixelHeight;
    private final Map<class_2960, BedrockItemGeoModel> geoModelCache = new HashMap();
    private final Map<String, AnimationClip> animations = new HashMap();
    private final Map<class_2960, int[]> textureSizeCache = new HashMap();

    BedrockItemDefinition(class_2960 id, List<String> items, class_2960 geoId, class_2960 textureId, List<class_2960> animationFiles, List<PunchyTuningConfig.CustomAnimationEntry> modelPartsAnimation, List<String> definitionParticleEffects, Map<String, Integer> boneEmission, Map<String, class_2960> boneTextures, Map<String, TextureAnimSpec> boneTextureAnim, List<DynamicBoneTextureSpec> dynamicBoneTextures, Map<String, BoneMirrorSpec> boneMirror, Map<String, BonePhysicsSpec> bonePhysics, Set<String> disablePhysicsBones, Set<String> cameraTransparentBones, boolean boneEmissionBlur, boolean boneEmissionClamp, int boneEmissionStrength, int textureAnimFrames, int textureAnimFrameTime, int textureAnimFrameHeight, boolean physicsFeedback, Map<class_1306, DisplayTransform> displayTransforms, String customName) {
        this.id = id;
        this.items = items == null ? List.of() : items;
        this.geoId = geoId;
        this.textureId = textureId;
        this.animationFiles = animationFiles == null ? List.of() : animationFiles;
        this.modelPartsAnimation = modelPartsAnimation == null ? List.of() : modelPartsAnimation;
        this.definitionParticleEffects = definitionParticleEffects == null ? List.of() : new ArrayList<>(definitionParticleEffects);
        this.displayTransforms = displayTransforms == null ? Map.of() : displayTransforms;
        this.boneEmission = boneEmission == null ? Map.of() : new HashMap<>(boneEmission);
        this.boneTextures = boneTextures == null ? Map.of() : new HashMap<>(boneTextures);
        this.boneTextureAnim = boneTextureAnim == null ? Map.of() : new HashMap<>(boneTextureAnim);
        this.dynamicBoneTextures = dynamicBoneTextures == null ? List.of() : List.copyOf(dynamicBoneTextures);
        this.boneMirror = boneMirror == null ? Map.of() : new HashMap<>(boneMirror);
        this.bonePhysics = bonePhysics == null ? Map.of() : new HashMap<>(bonePhysics);
        this.disablePhysicsBones = disablePhysicsBones == null ? Set.of() : new HashSet<>(disablePhysicsBones);
        this.cameraTransparentBones = cameraTransparentBones == null ? Set.of() : new HashSet<>(cameraTransparentBones);
        this.boneEmissionBlur = boneEmissionBlur;
        this.boneEmissionClamp = boneEmissionClamp;
        this.boneEmissionStrength = class_3532.method_15340(boneEmissionStrength, 0, 100);
        this.textureAnimFrames = textureAnimFrames < 1 ? 1 : textureAnimFrames;
        this.textureAnimFrameTime = textureAnimFrameTime < 1 ? 1 : textureAnimFrameTime;
        this.textureAnimFrameHeight = textureAnimFrameHeight < 1 ? 0 : textureAnimFrameHeight;
        this.physicsFeedback = physicsFeedback;
        this.customName = customName;
    }

    static List<BedrockItemDefinition> loadAll(class_3300 manager) {
        BedrockItemDefinition def;
        if (manager == null) {
            return List.of();
        }
        Map<class_2960, class_3298> found = new HashMap<>();
        found.putAll(manager.method_14488("punchy/model_parts_items/definitions", loc -> {
            return loc.method_12832().startsWith("punchy/model_parts_items/definitions/") && loc.method_12832().endsWith(".json");
        }));
        if (found.isEmpty()) {
            return List.of();
        }
        List<class_2960> ids = new ArrayList<>(found.keySet());
        ids.sort((a, b) -> {
            return a.toString().compareToIgnoreCase(b.toString());
        });
        List<BedrockItemDefinition> defs = new ArrayList<>();
        for (class_2960 id : ids) {
            class_3298 res = found.get(id);
            if (res != null && (def = parse(id, res)) != null) {
                defs.add(def);
            }
        }
        return defs;
    }

    private static BedrockItemDefinition parse(class_2960 id, class_3298 res) {
        String value;
        class_2960 animId;
        String value2;
        JsonElement value3;
        String name;
        JsonElement mirrorEl;
        JsonObject animObj;
        class_2960 texId;
        String boneName;
        JsonObject boneObj;
        JsonArray<JsonElement> arr;
        class_2960 animId2;
        String value4;
        try {
            InputStreamReader reader = new InputStreamReader(res.method_14482());
            try {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<String> items = new ArrayList<>();
                if (json.has("items")) {
                    JsonArray<JsonElement> arr2 = json.getAsJsonArray("items");
                    if (arr2 != null) {
                        for (JsonElement el : arr2) {
                            if (el != null && el.isJsonPrimitive() && (value4 = el.getAsString()) != null && !value4.isBlank()) {
                                items.add(value4.trim());
                            }
                        }
                    }
                } else if (json.has("item") && (value = json.get("item").getAsString()) != null && !value.isBlank()) {
                    items.add(value.trim());
                }
                class_2960 geoId = json.has("geo") ? class_2960.method_12829(json.get("geo").getAsString()) : null;
                class_2960 textureId = json.has("texture") ? class_2960.method_12829(json.get("texture").getAsString()) : null;
                if (textureId != null) {
                    textureId = normalizeTextureId(textureId);
                }
                List<class_2960> animations = new ArrayList<>();
                if (json.has("animations")) {
                    JsonArray<JsonElement> arr3 = json.getAsJsonArray("animations");
                    if (arr3 != null) {
                        for (JsonElement el2 : arr3) {
                            if (el2 != null && el2.isJsonPrimitive() && (animId2 = class_2960.method_12829(el2.getAsString())) != null) {
                                animations.add(animId2);
                            }
                        }
                    }
                } else if (json.has("animation") && (animId = class_2960.method_12829(json.get("animation").getAsString())) != null) {
                    animations.add(animId);
                }
                List<PunchyTuningConfig.CustomAnimationEntry> modelParts = new ArrayList<>();
                if (json.has("modelPartsAnimation") && (arr = json.getAsJsonArray("modelPartsAnimation")) != null) {
                    for (JsonElement el3 : arr) {
                        if (el3 != null && el3.isJsonObject()) {
                            PunchyTuningConfig.CustomAnimationEntry entry = (PunchyTuningConfig.CustomAnimationEntry) GSON.fromJson(el3, PunchyTuningConfig.CustomAnimationEntry.class);
                            if (entry != null) {
                                modelParts.add(entry);
                            }
                        }
                    }
                }
                Map<class_1306, DisplayTransform> display = new HashMap<>();
                if (json.has("display")) {
                    JsonObject displayObj = json.getAsJsonObject("display");
                    DisplayTransform right = readDisplay(displayObj, "thirdperson_righthand");
                    if (right == null) {
                        right = readDisplay(displayObj, "firstperson_righthand");
                    }
                    if (right != null) {
                        display.put(class_1306.field_6183, right);
                    }
                    DisplayTransform left = readDisplay(displayObj, "thirdperson_lefthand");
                    if (left == null) {
                        left = readDisplay(displayObj, "firstperson_lefthand");
                    }
                    if (left != null) {
                        display.put(class_1306.field_6182, left);
                    }
                }
                String customName = null;
                if (json.has("customName")) {
                    String value5 = json.get("customName").getAsString();
                    if (value5 != null && !value5.isBlank()) {
                        customName = value5;
                    }
                } else if (json.has("name") && (value2 = json.get("name").getAsString()) != null && !value2.isBlank()) {
                    customName = value2;
                }
                if (!items.isEmpty() && geoId != null && textureId != null) {
                    boolean physicsFeedback = false;
                    if (json.has("physicsFeedback")) {
                        JsonElement value6 = json.get("physicsFeedback");
                        if (value6 != null && value6.isJsonPrimitive()) {
                            physicsFeedback = value6.getAsBoolean();
                        }
                    } else if (json.has("fallingFeedBack")) {
                        JsonElement value7 = json.get("fallingFeedBack");
                        if (value7 != null && value7.isJsonPrimitive()) {
                            physicsFeedback = value7.getAsBoolean();
                        }
                    } else if (json.has("fallingFeedback") && (value3 = json.get("fallingFeedback")) != null && value3.isJsonPrimitive()) {
                        physicsFeedback = value3.getAsBoolean();
                    }
                    Map<String, Integer> boneEmission = Map.of();
                    boolean boneEmissionBlur = false;
                    boolean boneEmissionClamp = false;
                    int boneEmissionStrength = 100;
                    int textureAnimFrames = 1;
                    int textureAnimFrameTime = 1;
                    int textureAnimFrameHeight = 0;
                    Map<String, class_2960> boneTextures = Map.of();
                    Map<String, TextureAnimSpec> boneTextureAnim = Map.of();
                    List<DynamicBoneTextureSpec> dynamicBoneTextures = List.of();
                    Map<String, BoneMirrorSpec> boneMirror = Map.of();
                    Map.of();
                    Set<String> disablePhysicsBones = Set.of();
                    Set<String> cameraTransparentBones = Set.of();
                    if (json.has("bone_emission") && json.get("bone_emission").isJsonObject() && (boneObj = json.getAsJsonObject("bone_emission")) != null) {
                        Map<String, Integer> tmp = new HashMap<>();
                        for (Map.Entry<String, JsonElement> entry2 : boneObj.entrySet()) {
                            String key = entry2.getKey();
                            if (key != null && !key.isBlank()) {
                                if ("blur".equalsIgnoreCase(key)) {
                                    if (entry2.getValue() != null && entry2.getValue().isJsonPrimitive()) {
                                        boneEmissionBlur = entry2.getValue().getAsBoolean();
                                    }
                                } else if ("clamp".equalsIgnoreCase(key)) {
                                    if (entry2.getValue() != null && entry2.getValue().isJsonPrimitive()) {
                                        boneEmissionClamp = entry2.getValue().getAsBoolean();
                                    }
                                } else if ("strength".equalsIgnoreCase(key) || "intensity".equalsIgnoreCase(key) || "brightness".equalsIgnoreCase(key)) {
                                    if (entry2.getValue() != null && entry2.getValue().isJsonPrimitive()) {
                                        int strength = entry2.getValue().getAsInt();
                                        if (strength < 0) {
                                            strength = 0;
                                        } else if (strength > 100) {
                                            strength = 100;
                                        }
                                        boneEmissionStrength = strength;
                                    }
                                } else {
                                    JsonElement value8 = entry2.getValue();
                                    if (value8 != null && value8.isJsonPrimitive() && value8.getAsJsonPrimitive().isNumber()) {
                                        int emission = value8.getAsInt();
                                        if (emission < 0) {
                                            emission = 0;
                                        } else if (emission > 100) {
                                            emission = 100;
                                        }
                                        if (emission > 0) {
                                            tmp.put(key, Integer.valueOf(emission));
                                        }
                                    }
                                }
                            }
                        }
                        if (!tmp.isEmpty()) {
                            boneEmission = tmp;
                        }
                    }
                    if (json.has("bone_textures")) {
                        JsonElement boneTexEl = json.get("bone_textures");
                        if (boneTexEl.isJsonObject()) {
                            JsonObject texObj = boneTexEl.getAsJsonObject();
                            Map<String, class_2960> tmp2 = new HashMap<>();
                            for (Map.Entry<String, JsonElement> entry3 : texObj.entrySet()) {
                                String key2 = entry3.getKey();
                                if (key2 != null && !key2.isBlank() && entry3.getValue() != null && entry3.getValue().isJsonPrimitive()) {
                                    String key3 = key2.trim();
                                    class_2960 texId2 = class_2960.method_12829(entry3.getValue().getAsString());
                                    if (texId2 != null) {
                                        tmp2.put(key3, normalizeTextureId(texId2));
                                    }
                                }
                            }
                            if (!tmp2.isEmpty()) {
                                boneTextures = tmp2;
                            }
                        } else if (boneTexEl.isJsonArray()) {
                            JsonArray<JsonElement> arr4 = boneTexEl.getAsJsonArray();
                            Map<String, class_2960> tmp3 = new HashMap<>();
                            for (JsonElement el4 : arr4) {
                                if (el4 != null && el4.isJsonObject()) {
                                    JsonObject obj = el4.getAsJsonObject();
                                    if (obj.has("texture") && obj.get("texture").isJsonPrimitive() && (texId = class_2960.method_12829(obj.get("texture").getAsString())) != null) {
                                        class_2960 texId3 = normalizeTextureId(texId);
                                        JsonArray<JsonElement> bones = obj.has("bones") ? obj.getAsJsonArray("bones") : null;
                                        if (bones != null) {
                                            for (JsonElement boneEl : bones) {
                                                if (boneEl != null && boneEl.isJsonPrimitive() && (boneName = boneEl.getAsString()) != null && !boneName.isBlank()) {
                                                    tmp3.put(boneName.trim(), texId3);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!tmp3.isEmpty()) {
                                boneTextures = tmp3;
                            }
                        }
                    }
                    if (json.has("texture_anim") && json.get("texture_anim").isJsonObject()) {
                        JsonObject animObj2 = json.getAsJsonObject("texture_anim");
                        if (animObj2.has("frames")) {
                            textureAnimFrames = Math.max(1, animObj2.get("frames").getAsInt());
                        }
                        if (animObj2.has("frametime")) {
                            textureAnimFrameTime = Math.max(1, animObj2.get("frametime").getAsInt());
                        }
                        if (animObj2.has("frame_height")) {
                            textureAnimFrameHeight = Math.max(1, animObj2.get("frame_height").getAsInt());
                        }
                    }
                    if (json.has("bone_texture_anim") && json.get("bone_texture_anim").isJsonObject() && (animObj = json.getAsJsonObject("bone_texture_anim")) != null) {
                        Map<String, TextureAnimSpec> tmp4 = new HashMap<>();
                        for (Map.Entry<String, JsonElement> entry4 : animObj.entrySet()) {
                            String key4 = entry4.getKey();
                            if (key4 != null && !key4.isBlank() && entry4.getValue() != null && entry4.getValue().isJsonObject()) {
                                TextureAnimSpec spec = TextureAnimSpec.fromJson(entry4.getValue().getAsJsonObject());
                                if (spec != null) {
                                    tmp4.put(key4, spec);
                                }
                            }
                        }
                        if (!tmp4.isEmpty()) {
                            boneTextureAnim = tmp4;
                        }
                    }
                    if (json.has("dynamic_bone_textures")) {
                        dynamicBoneTextures = parseDynamicBoneTextures(json.get("dynamic_bone_textures"));
                    } else if (json.has("dynamicBoneTextures")) {
                        dynamicBoneTextures = parseDynamicBoneTextures(json.get("dynamicBoneTextures"));
                    }
                    if (json.has("bone_mirror") && (mirrorEl = json.get("bone_mirror")) != null && mirrorEl.isJsonObject()) {
                        Map<String, BoneMirrorSpec> tmp5 = new HashMap<>();
                        JsonObject mirrorObj = mirrorEl.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry5 : mirrorObj.entrySet()) {
                            String bone = entry5.getKey();
                            if (bone != null && !bone.isBlank() && entry5.getValue() != null) {
                                BoneMirrorSpec spec2 = BoneMirrorSpec.fromJson(entry5.getValue());
                                if (spec2 != null) {
                                    tmp5.put(bone.trim(), spec2);
                                }
                            }
                        }
                        if (!tmp5.isEmpty()) {
                            boneMirror = tmp5;
                        }
                    }
                    JsonElement bonePhysicsEl = null;
                    if (json.has("bone_physics")) {
                        bonePhysicsEl = json.get("bone_physics");
                    } else if (json.has("bonePhysics")) {
                        bonePhysicsEl = json.get("bonePhysics");
                    }
                    Map<String, BonePhysicsSpec> bonePhysics = parseBonePhysics(bonePhysicsEl);
                    JsonElement physicsEl = null;
                    if (json.has("disable_physics_bones")) {
                        physicsEl = json.get("disable_physics_bones");
                    } else if (json.has("disable_pendulum_bones")) {
                        physicsEl = json.get("disable_pendulum_bones");
                    }
                    if (physicsEl != null && physicsEl.isJsonArray()) {
                        Set<String> tmp6 = new HashSet<>();
                        for (JsonElement el5 : physicsEl.getAsJsonArray()) {
                            if (el5 != null && el5.isJsonPrimitive() && (name = el5.getAsString()) != null && !name.isBlank()) {
                                tmp6.add(name.trim());
                            }
                        }
                        if (!tmp6.isEmpty()) {
                            disablePhysicsBones = tmp6;
                        }
                    }
                    JsonElement cameraTransparentEl = null;
                    if (json.has("camera_transparent_bones")) {
                        cameraTransparentEl = json.get("camera_transparent_bones");
                    } else if (json.has("inside_visible_bones")) {
                        cameraTransparentEl = json.get("inside_visible_bones");
                    }
                    if (cameraTransparentEl != null) {
                        Set<String> tmp7 = parseBoneNameSet(cameraTransparentEl);
                        if (!tmp7.isEmpty()) {
                            cameraTransparentBones = tmp7;
                        }
                    }
                    List<String> definitionParticleEffects = readDefinitionParticleEffects(json);
                    BedrockItemDefinition bedrockItemDefinition = new BedrockItemDefinition(id, items, geoId, textureId, animations, modelParts, definitionParticleEffects, boneEmission, boneTextures, boneTextureAnim, dynamicBoneTextures, boneMirror, bonePhysics, disablePhysicsBones, cameraTransparentBones, boneEmissionBlur, boneEmissionClamp, boneEmissionStrength, textureAnimFrames, textureAnimFrameTime, textureAnimFrameHeight, physicsFeedback, display, customName);
                    reader.close();
                    return bedrockItemDefinition;
                }
                reader.close();
                return null;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    static Map<String, BonePhysicsSpec> parseBonePhysics(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Map.of();
        }
        Map<String, BonePhysicsSpec> out = new HashMap<>();
        if (element.isJsonObject()) {
            appendBonePhysicsObject(out, element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonObject()) {
                    appendBonePhysicsObject(out, child.getAsJsonObject());
                }
            }
        }
        return out.isEmpty() ? Map.of() : out;
    }

    private static List<DynamicBoneTextureSpec> parseDynamicBoneTextures(JsonElement element) {
        DynamicBoneTextureSpec spec;
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<DynamicBoneTextureSpec> out = new ArrayList<>();
        if (element.isJsonObject()) {
            DynamicBoneTextureSpec spec2 = DynamicBoneTextureSpec.fromJson(element.getAsJsonObject());
            if (spec2 != null) {
                out.add(spec2);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonObject() && (spec = DynamicBoneTextureSpec.fromJson(child.getAsJsonObject())) != null) {
                    out.add(spec);
                }
            }
        }
        return out.isEmpty() ? List.of() : out;
    }

    private static void appendBonePhysicsObject(Map<String, BonePhysicsSpec> out, JsonObject object) {
        if (object == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank()) {
                BonePhysicsSpec spec = BonePhysicsSpec.fromJson(entry.getValue());
                if (spec != null && !spec.isEmpty()) {
                    String key2 = key.trim();
                    BonePhysicsSpec previous = out.get(key2);
                    out.put(key2, previous == null ? spec : previous.merge(spec));
                }
            }
        }
    }

    private static Set<String> parseBoneNameSet(JsonElement element) {
        String value;
        if (element == null || !element.isJsonArray()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        for (JsonElement el : element.getAsJsonArray()) {
            if (el != null && el.isJsonPrimitive() && (value = el.getAsString()) != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return out;
    }

    private static List<String> readDefinitionParticleEffects(JsonObject json) {
        if (json == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        appendDefinitionParticleEffects(out, json.get("particle"));
        appendDefinitionParticleEffects(out, json.get("particles"));
        appendDefinitionParticleEffects(out, json.get("particle_effects"));
        appendDefinitionParticleEffects(out, json.get("particle_emission"));
        appendDefinitionParticleEffects(out, json.get("particleEmission"));
        return out.isEmpty() ? List.of() : out;
    }

    private static void appendDefinitionParticleEffects(List<String> out, JsonElement element) {
        String normalized;
        if (out == null || element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                appendDefinitionParticleEffects(out, child);
            }
            return;
        }
        if (element.isJsonObject()) {
            String fromObject = definitionParticleObjectToScript(element.getAsJsonObject());
            if (fromObject != null && !fromObject.isBlank()) {
                out.add(fromObject);
                return;
            }
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && (normalized = normalizeDefinitionParticleEffect(element.getAsString())) != null && !normalized.isBlank()) {
            out.add(normalized);
        }
    }

    private static String normalizeDefinitionParticleEffect(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("particle(") && trimmed.endsWith(")")) {
            return trimmed;
        }
        if (trimmed.contains("=")) {
            return "particle(" + trimmed + ")";
        }
        class_2960 parsed = class_2960.method_12829(trimmed);
        if (parsed != null) {
            return "particle(type=" + String.valueOf(parsed) + ")";
        }
        return null;
    }

    private static String definitionParticleObjectToScript(JsonObject object) {
        if (object == null) {
            return null;
        }
        if (object.has("enabled") && object.get("enabled").isJsonPrimitive() && !object.get("enabled").getAsBoolean()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank() && !"enabled".equalsIgnoreCase(key) && !isIgnoredDefinitionParticleRenderHint(key)) {
                if ("triggers".equalsIgnoreCase(key) && entry.getValue() != null && entry.getValue().isJsonObject()) {
                    appendDefinitionTriggerTokens(joiner, entry.getValue().getAsJsonObject());
                } else if ("on_animation".equalsIgnoreCase(key) && entry.getValue() != null && entry.getValue().isJsonObject()) {
                    appendOnAnimationTokens(joiner, entry.getValue().getAsJsonObject());
                } else {
                    String value = definitionParticleValueToToken(entry.getValue());
                    if (value != null && !value.isBlank()) {
                        joiner.add(key.trim() + "=" + value);
                    }
                }
            }
        }
        String body = joiner.toString();
        if (body.isBlank()) {
            return null;
        }
        return "particle(" + body + ")";
    }

    private static boolean isIgnoredDefinitionParticleRenderHint(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return "emissive_strength".equals(normalized) || "emission_strength".equals(normalized) || "emissive_blur".equals(normalized) || "emission_blur".equals(normalized) || "emissive_clamp".equals(normalized) || "emission_clamp".equals(normalized);
    }

    private static String definitionParticleValueToToken(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonArray()) {
            JsonArray<JsonElement> arr = value.getAsJsonArray();
            boolean allPrimitive = true;
            for (JsonElement element : arr) {
                if (element == null || !element.isJsonPrimitive()) {
                    allPrimitive = false;
                    break;
                }
            }
            if (!allPrimitive) {
                return null;
            }
            if (arr.size() == 3) {
                StringJoiner vecJoiner = new StringJoiner(":");
                for (JsonElement element2 : arr) {
                    vecJoiner.add(element2.getAsString());
                }
                return vecJoiner.toString();
            }
            StringJoiner listJoiner = new StringJoiner("|");
            for (JsonElement element3 : arr) {
                listJoiner.add(element3.getAsString());
            }
            return listJoiner.toString();
        }
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            if (obj.has("x") && obj.has("y") && obj.has("z") && obj.get("x").isJsonPrimitive() && obj.get("y").isJsonPrimitive() && obj.get("z").isJsonPrimitive()) {
                return obj.get("x").getAsString() + ":" + obj.get("y").getAsString() + ":" + obj.get("z").getAsString();
            }
            return null;
        }
        return null;
    }

    private static void appendDefinitionTriggerTokens(StringJoiner joiner, JsonObject triggers) {
        if (joiner == null || triggers == null) {
            return;
        }
        appendTrueTrigger(joiner, triggers, "trigger_jump", "jump", "triggerJump");
        appendTrueTrigger(joiner, triggers, "trigger_landing", "landing", "triggerLanding");
        appendTrueTrigger(joiner, triggers, "trigger_walking", "walking", "walkgin", "triggerWalking");
        appendTrueTrigger(joiner, triggers, "trigger_sprinting", "sprinting", "triggerSprinting");
        appendTrueTrigger(joiner, triggers, "trigger_swimming", "swimming", "swiming", "triggerSwimming");
        appendTrueTrigger(joiner, triggers, "trigger_attacking", "attacking", "attack", "triggerAttacking");
        appendTrueTrigger(joiner, triggers, "trigger_mining", "mining", "triggerMining");
        JsonElement falling = triggers.get("falling");
        if (falling != null && !falling.isJsonNull()) {
            if (falling.isJsonPrimitive()) {
                if (readBoolean(falling, false)) {
                    joiner.add("trigger_falling=true");
                }
            } else if (falling.isJsonObject()) {
                JsonObject obj = falling.getAsJsonObject();
                joiner.add("trigger_falling=true");
                Float minBlocks = readFallingMinBlocks(obj);
                if (minBlocks != null) {
                    joiner.add("trigger_falling_min_blocks=" + minBlocks);
                }
            }
        }
        JsonElement biome = triggers.get("biome");
        List<String> biomeNames = readTriggerBiomeNames(biome);
        if (!biomeNames.isEmpty()) {
            joiner.add("trigger_biomes=" + String.join("|", biomeNames));
        }
    }

    private static void appendOnAnimationTokens(StringJoiner joiner, JsonObject onAnimation) {
        String n;
        if (joiner == null || onAnimation == null) {
            return;
        }
        JsonElement name = onAnimation.get("name");
        JsonElement timecode = onAnimation.get("timecode");
        if (name != null && name.isJsonPrimitive() && (n = name.getAsString()) != null && !n.isBlank()) {
            joiner.add("on_animation_name=" + n.trim());
        }
        if (timecode != null && timecode.isJsonPrimitive()) {
            joiner.add("on_animation_timecode=" + timecode.getAsString());
        }
    }

    private static void appendTrueTrigger(StringJoiner joiner, JsonObject triggers, String tokenKey, String... keys) {
        if (joiner == null || triggers == null || tokenKey == null || keys == null || keys.length == 0) {
            return;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                JsonElement element = triggers.get(key);
                if (readBoolean(element, false)) {
                    joiner.add(tokenKey + "=true");
                    return;
                }
            }
        }
    }

    private static boolean readBoolean(JsonElement element, boolean fallback) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        if (element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt() != 0;
        }
        if (element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value == null) {
                return fallback;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
                return false;
            }
        }
        return fallback;
    }

    private static Float readFallingMinBlocks(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String[] keys = {"min_blocks", "minBlocks", "min_height", "minHeight", "min"};
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                float value = element.getAsFloat();
                return Float.valueOf(value < 0.0f ? 0.0f : value);
            }
        }
        return null;
    }

    private static List<String> readTriggerBiomeNames(JsonElement biome) {
        String value;
        if (biome == null || biome.isJsonNull()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (biome.isJsonPrimitive() && biome.getAsJsonPrimitive().isString()) {
            String value2 = biome.getAsString();
            if (value2 != null && !value2.isBlank()) {
                out.add(value2.trim());
            }
            return out;
        }
        if (biome.isJsonArray()) {
            for (JsonElement element : biome.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && (value = element.getAsString()) != null && !value.isBlank()) {
                    out.add(value.trim());
                }
            }
            return out;
        }
        if (!biome.isJsonObject()) {
            return out;
        }
        JsonObject obj = biome.getAsJsonObject();
        JsonElement names = obj.get("names");
        if (names == null) {
            names = obj.get("biomes");
        }
        if (names != null) {
            out.addAll(readTriggerBiomeNames(names));
            return out;
        }
        JsonElement one = obj.get("name");
        if (one == null) {
            one = obj.get("biome");
        }
        if (one != null) {
            out.addAll(readTriggerBiomeNames(one));
        }
        return out;
    }

    private static DisplayTransform readDisplay(JsonObject displayObj, String key) {
        if (displayObj == null || key == null || !displayObj.has(key)) {
            return null;
        }
        JsonObject entry = displayObj.getAsJsonObject(key);
        return DisplayTransform.fromJson(entry);
    }

    class_2960 getId() {
        return this.id;
    }

    List<String> getItems() {
        return this.items;
    }

    class_2960 getGeoId() {
        return this.geoId;
    }

    class_2960 getTextureId() {
        return this.textureId;
    }

    List<class_2960> getAnimationFiles() {
        return this.animationFiles;
    }

    List<PunchyTuningConfig.CustomAnimationEntry> getModelPartsAnimation() {
        return this.modelPartsAnimation;
    }

    List<String> getDefinitionParticleEffects() {
        return this.definitionParticleEffects;
    }

    Map<String, Integer> getBoneEmission() {
        return this.boneEmission;
    }

    Map<String, class_2960> getBoneTextures() {
        return this.boneTextures;
    }

    Map<String, TextureAnimSpec> getBoneTextureAnim() {
        return this.boneTextureAnim;
    }

    List<DynamicBoneTextureSpec> getDynamicBoneTextures() {
        return this.dynamicBoneTextures;
    }

    Set<String> getDisablePhysicsBones() {
        return this.disablePhysicsBones;
    }

    Set<String> getCameraTransparentBones() {
        return this.cameraTransparentBones;
    }

    boolean isBoneEmissionBlur() {
        return this.boneEmissionBlur;
    }

    boolean isBoneEmissionClamp() {
        return this.boneEmissionClamp;
    }

    int getBoneEmissionStrength() {
        return this.boneEmissionStrength;
    }

    int getTextureAnimFrames() {
        return this.textureAnimFrames;
    }

    int getTextureAnimFrameTime() {
        return this.textureAnimFrameTime;
    }

    int getTextureAnimFrameHeight() {
        return this.textureAnimFrameHeight;
    }

    boolean isPhysicsFeedbackEnabled() {
        return this.physicsFeedback;
    }

    int getTexturePixelWidth() {
        return this.texturePixelWidth;
    }

    int getTexturePixelHeight() {
        return this.texturePixelHeight;
    }

    int[] getTexturePixelSize(class_2960 id) {
        if (id == null) {
            return null;
        }
        return this.textureSizeCache.get(id);
    }

    Map<String, BonePhysicsSpec> getBonePhysics() {
        return this.bonePhysics;
    }

    boolean hasCreatureBonePhysics() {
        if (this.bonePhysics == null || this.bonePhysics.isEmpty()) {
            return false;
        }
        for (BonePhysicsSpec spec : this.bonePhysics.values()) {
            if (spec != null && spec.enableCreaturePhysics()) {
                return true;
            }
        }
        return false;
    }

    boolean hasSlimeBonePhysics() {
        if (this.bonePhysics == null || this.bonePhysics.isEmpty()) {
            return false;
        }
        for (BonePhysicsSpec spec : this.bonePhysics.values()) {
            if (spec != null && spec.enableSlimePhysics()) {
                return true;
            }
        }
        return false;
    }

    boolean matchesCustomName(class_1799 stack) {
        class_2561 name;
        String value;
        if (this.customName == null || this.customName.isBlank()) {
            return true;
        }
        if (stack == null || stack.method_7960() || (name = (class_2561) stack.method_58694(class_9334.field_49631)) == null || (value = name.getString()) == null) {
            return false;
        }
        return value.trim().equalsIgnoreCase(this.customName.trim());
    }

    boolean hasCustomName() {
        return (this.customName == null || this.customName.isBlank()) ? false : true;
    }

    DisplayTransform getDisplayTransform(class_1306 arm) {
        return this.displayTransforms.get(arm);
    }

    void loadResources(class_3300 manager) {
        this.geoModelCache.clear();
        this.geoModel = loadGeoModel(manager, this.geoId);
        this.animations.clear();
        if (manager == null) {
            return;
        }
        this.textureSizeCache.clear();
        if (this.textureId != null) {
            loadTextureSize(manager, this.textureId);
            int[] size = this.textureSizeCache.get(this.textureId);
            if (size != null) {
                this.texturePixelWidth = size[0];
                this.texturePixelHeight = size[1];
            }
        }
        for (class_2960 tex : this.boneTextures.values()) {
            loadTextureSize(manager, tex);
        }
        for (DynamicBoneTextureSpec spec : this.dynamicBoneTextures) {
            if (spec != null) {
                if (spec.isTextureType()) {
                    loadTextureSize(manager, spec.fallbackResource());
                    for (class_2960 tex2 : spec.resourceMap().values()) {
                        loadTextureSize(manager, tex2);
                    }
                } else {
                    if (spec.fallbackResource() != null) {
                        loadGeoModel(manager, spec.fallbackResource());
                    }
                    for (class_2960 geo : spec.resourceMap().values()) {
                        loadGeoModel(manager, geo);
                    }
                }
            }
        }
        for (class_2960 animId : this.animationFiles) {
            Map<String, AnimationClip> loaded = BedrockItemAnimationLoader.loadAll(manager, animId);
            if (loaded != null && !loaded.isEmpty()) {
                this.animations.putAll(loaded);
            }
        }
        if (!this.boneMirror.isEmpty()) {
            applyBoneMirrorOverrides();
        }
    }

    BedrockItemGeoModel getGeoModel() {
        return this.geoModel;
    }

    BedrockItemGeoModel getGeoModel(class_2960 overrideGeoId) {
        if (overrideGeoId == null || overrideGeoId.equals(this.geoId)) {
            return this.geoModel;
        }
        return this.geoModelCache.get(overrideGeoId);
    }

    Map<String, AnimationClip> getAnimations() {
        return this.animations;
    }

    private void applyBoneMirrorOverrides() {
        BoneMirrorSpec spec;
        BoneTrack track;
        for (Map.Entry<String, AnimationClip> clipEntry : this.animations.entrySet()) {
            AnimationClip clip = clipEntry.getValue();
            if (clip != null) {
                for (Map.Entry<String, BoneTrack> trackEntry : clip.getTracks().entrySet()) {
                    String boneName = trackEntry.getKey();
                    if (boneName != null && (spec = this.boneMirror.get(boneName)) != null && !spec.isDefault() && (track = trackEntry.getValue()) != null) {
                        for (Keyframe key : track.getKeyframes()) {
                            if (key != null) {
                                if (spec.mirrorPosition == Boolean.FALSE && key.position() != null) {
                                    key.position().x = -key.position().x;
                                }
                                if (spec.mirrorRotation == Boolean.FALSE && key.rotationEuler() != null) {
                                    key.rotationEuler().x = -key.rotationEuler().x;
                                    key.rotationEuler().y = -key.rotationEuler().y;
                                    if (key.rotation() != null) {
                                        float rx = (float) Math.toRadians(key.rotationEuler().x);
                                        float ry = (float) Math.toRadians(key.rotationEuler().y);
                                        float rz = (float) Math.toRadians(key.rotationEuler().z);
                                        key.rotation().set(new Quaternionf().rotationXYZ(rx, ry, rz));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private BedrockItemGeoModel loadGeoModel(class_3300 manager, class_2960 modelId) {
        if (manager == null || modelId == null) {
            return null;
        }
        BedrockItemGeoModel cached = this.geoModelCache.get(modelId);
        if (cached != null) {
            return cached;
        }
        BedrockItemGeoModel loaded = BedrockItemGeoLoader.load(manager, modelId);
        if (loaded != null) {
            this.geoModelCache.put(modelId, loaded);
        }
        return loaded;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition$DisplayTransform.class */
    static final class DisplayTransform {
        final float rotX;
        final float rotY;
        final float rotZ;
        final float transX;
        final float transY;
        final float transZ;
        final float scaleX;
        final float scaleY;
        final float scaleZ;

        private DisplayTransform(float rotX, float rotY, float rotZ, float transX, float transY, float transZ, float scaleX, float scaleY, float scaleZ) {
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.transX = transX;
            this.transY = transY;
            this.transZ = transZ;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
        }

        static DisplayTransform fromJson(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            float[] rot = readVec3(obj, "rotation", 0.0f, 0.0f, 0.0f);
            float[] trans = readVec3(obj, "translation", 0.0f, 0.0f, 0.0f);
            float[] scale = readVec3(obj, "scale", 1.0f, 1.0f, 1.0f);
            return new DisplayTransform(rot[0], rot[1], rot[2], trans[0], trans[1], trans[2], scale[0], scale[1], scale[2]);
        }

        private static float[] readVec3(JsonObject obj, String key, float defX, float defY, float defZ) {
            if (obj == null || !obj.has(key)) {
                return new float[]{defX, defY, defZ};
            }
            JsonArray arr = obj.getAsJsonArray(key);
            if (arr == null || arr.size() < 3) {
                return new float[]{defX, defY, defZ};
            }
            return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
    }

    private static class_2960 normalizeTextureId(class_2960 id) {
        if (id == null) {
            return null;
        }
        String path = id.method_12832();
        if (path == null || path.isBlank()) {
            return id;
        }
        String normalized = path;
        if (normalized.startsWith("punchy/model_parts_items/textures/")) {
            if (!normalized.endsWith(".png")) {
                normalized = normalized + ".png";
            }
            return ResourceLocationCompat.fromNamespaceAndPath(id.method_12836(), normalized);
        }
        if (!normalized.startsWith("textures/")) {
            normalized = "textures/" + normalized;
        }
        if (!normalized.endsWith(".png")) {
            normalized = normalized + ".png";
        }
        return ResourceLocationCompat.fromNamespaceAndPath(id.method_12836(), normalized);
    }

    private static class_2960 normalizeGeoId(class_2960 id) {
        if (id == null) {
            return null;
        }
        String path = id.method_12832();
        if (path == null || path.isBlank()) {
            return id;
        }
        String normalized = path;
        if (normalized.startsWith("punchy/model_parts_items/geo/")) {
            if (!normalized.endsWith(".geo.json")) {
                normalized = normalized.endsWith(".json") ? normalized.substring(0, normalized.length() - 5) + ".geo.json" : normalized + ".geo.json";
            }
            return ResourceLocationCompat.fromNamespaceAndPath(id.method_12836(), normalized);
        }
        if (!normalized.startsWith("geo/")) {
            normalized = "geo/" + normalized;
        }
        if (!normalized.endsWith(".geo.json")) {
            normalized = normalized.endsWith(".json") ? normalized.substring(0, normalized.length() - 5) + ".geo.json" : normalized + ".geo.json";
        }
        return ResourceLocationCompat.fromNamespaceAndPath(id.method_12836(), normalized);
    }

    private void loadTextureSize(class_3300 manager, class_2960 id) {
        if (manager == null || id == null || this.textureSizeCache.containsKey(id)) {
            return;
        }
        try {
            Optional<class_3298> resourceOpt = manager.method_14486(id);
            if (resourceOpt.isEmpty()) {
                return;
            }
            InputStream stream = resourceOpt.get().method_14482();
            try {
                class_1011 image = class_1011.method_4309(stream);
                if (image != null) {
                    this.textureSizeCache.put(id, new int[]{image.method_4307(), image.method_4323()});
                    image.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } finally {
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition$TextureAnimSpec.class */
    static final class TextureAnimSpec {
        final int frames;
        final int frameTime;
        final int frameHeight;

        private TextureAnimSpec(int frames, int frameTime, int frameHeight) {
            this.frames = frames;
            this.frameTime = frameTime;
            this.frameHeight = frameHeight;
        }

        static TextureAnimSpec fromJson(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            int frames = obj.has("frames") ? Math.max(1, obj.get("frames").getAsInt()) : 1;
            int frameTime = obj.has("frametime") ? Math.max(1, obj.get("frametime").getAsInt()) : 1;
            int frameHeight = obj.has("frame_height") ? Math.max(1, obj.get("frame_height").getAsInt()) : 0;
            return new TextureAnimSpec(frames, frameTime, frameHeight);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec.class */
    static final class DynamicBoneTextureSpec extends Record {
        private final String type;
        private final String bone;
        private final String source;
        private final class_2960 fallbackResource;
        private final Map<String, class_2960> resourceMap;
        static final String TYPE_TEXTURE = "texture";
        static final String TYPE_GEO = "geo";
        static final String SOURCE_AXOL_VARIANTS = "axol_variants";

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, DynamicBoneTextureSpec.class), DynamicBoneTextureSpec.class, "type;bone;source;fallbackResource;resourceMap", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->bone:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->source:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->fallbackResource:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->resourceMap:Ljava/util/Map;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, DynamicBoneTextureSpec.class), DynamicBoneTextureSpec.class, "type;bone;source;fallbackResource;resourceMap", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->bone:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->source:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->fallbackResource:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->resourceMap:Ljava/util/Map;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, DynamicBoneTextureSpec.class, Object.class), DynamicBoneTextureSpec.class, "type;bone;source;fallbackResource;resourceMap", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->type:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->bone:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->source:Ljava/lang/String;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->fallbackResource:Lnet/minecraft/class_2960;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$DynamicBoneTextureSpec;->resourceMap:Ljava/util/Map;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String type() {
            return this.type;
        }

        public String bone() {
            return this.bone;
        }

        public String source() {
            return this.source;
        }

        public class_2960 fallbackResource() {
            return this.fallbackResource;
        }

        public Map<String, class_2960> resourceMap() {
            return this.resourceMap;
        }

        DynamicBoneTextureSpec(String type, String bone, String source, class_2960 fallbackResource, Map<String, class_2960> resourceMap) {
            String type2 = (type == null || type.isBlank()) ? TYPE_TEXTURE : type.trim();
            Map<String, class_2960> resourceMap2 = resourceMap == null ? Map.of() : Map.copyOf(resourceMap);
            this.type = type2;
            this.bone = bone;
            this.source = source;
            this.fallbackResource = fallbackResource;
            this.resourceMap = resourceMap2;
        }

        boolean isTextureType() {
            return !TYPE_GEO.equalsIgnoreCase(this.type);
        }

        boolean isGeoType() {
            return TYPE_GEO.equalsIgnoreCase(this.type);
        }

        boolean usesAxolVariants() {
            return SOURCE_AXOL_VARIANTS.equalsIgnoreCase(this.source);
        }

        static DynamicBoneTextureSpec fromJson(JsonObject obj) {
            class_2960 resource;
            if (obj == null) {
                return null;
            }
            String type = readString(obj, "type");
            if (type == null || type.isBlank()) {
                type = TYPE_TEXTURE;
            }
            boolean geoType = TYPE_GEO.equalsIgnoreCase(type);
            String bone = readString(obj, "bone");
            if (!geoType && (bone == null || bone.isBlank())) {
                return null;
            }
            String source = readString(obj, "source");
            if (source != null) {
                source = source.trim();
            }
            class_2960 fallback = readResourceId(obj, "fallback", geoType);
            Map<String, class_2960> map = new HashMap<>();
            JsonObject mapObj = (obj.has("map") && obj.get("map").isJsonObject()) ? obj.getAsJsonObject("map") : null;
            if (mapObj != null) {
                for (Map.Entry<String, JsonElement> entry : mapObj.entrySet()) {
                    String key = entry.getKey();
                    if (key != null && !key.isBlank() && entry.getValue() != null && entry.getValue().isJsonPrimitive() && (resource = class_2960.method_12829(entry.getValue().getAsString())) != null) {
                        map.put(key.trim(), geoType ? BedrockItemDefinition.normalizeGeoId(resource) : BedrockItemDefinition.normalizeTextureId(resource));
                    }
                }
            }
            if (fallback == null && map.isEmpty()) {
                return null;
            }
            return new DynamicBoneTextureSpec(type.trim(), bone == null ? null : bone.trim(), source, fallback, map);
        }

        private static String readString(JsonObject obj, String key) {
            String value;
            if (obj == null || key == null || !obj.has(key) || obj.get(key) == null || !obj.get(key).isJsonPrimitive() || (value = obj.get(key).getAsString()) == null || value.isBlank()) {
                return null;
            }
            return value.trim();
        }

        private static class_2960 readResourceId(JsonObject obj, String key, boolean geoType) {
            class_2960 id;
            String value = readString(obj, key);
            if (value == null || (id = class_2960.method_12829(value)) == null) {
                return null;
            }
            return geoType ? BedrockItemDefinition.normalizeGeoId(id) : BedrockItemDefinition.normalizeTextureId(id);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition$BoneMirrorSpec.class */
    static final class BoneMirrorSpec {
        final Boolean mirrorPosition;
        final Boolean mirrorRotation;

        private BoneMirrorSpec(Boolean mirrorPosition, Boolean mirrorRotation) {
            this.mirrorPosition = mirrorPosition;
            this.mirrorRotation = mirrorRotation;
        }

        boolean isDefault() {
            return this.mirrorPosition == null && this.mirrorRotation == null;
        }

        static BoneMirrorSpec fromJson(JsonElement el) {
            if (el == null || el.isJsonNull()) {
                return null;
            }
            if (el.isJsonPrimitive()) {
                boolean mirror = el.getAsBoolean();
                return new BoneMirrorSpec(Boolean.valueOf(mirror), Boolean.valueOf(mirror));
            }
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                Boolean pos = obj.has("position") ? Boolean.valueOf(obj.get("position").getAsBoolean()) : null;
                Boolean rot = obj.has("rotation") ? Boolean.valueOf(obj.get("rotation").getAsBoolean()) : null;
                return new BoneMirrorSpec(pos, rot);
            }
            return null;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec.class */
    static final class BonePhysicsSpec extends Record {
        private final boolean enableCreaturePhysics;
        private final boolean enableSlimePhysics;
        private final ToolTuning.PhysicsMode pendulumMode;
        private final ToolTuning.PhysicsSettings pendulumSettings;
        private final float impactInclinationPercent;
        private final float fallInclinationPercent;
        private final float impactTranslationPercent;
        private final float fallTranslationPercent;
        private final float slimeImpactPercent;
        private final float slimeFallPercent;
        private static final float DEFAULT_INCLINATION_PERCENT = 100.0f;

        BonePhysicsSpec(boolean enableCreaturePhysics, boolean enableSlimePhysics, ToolTuning.PhysicsMode pendulumMode, ToolTuning.PhysicsSettings pendulumSettings, float impactInclinationPercent, float fallInclinationPercent, float impactTranslationPercent, float fallTranslationPercent, float slimeImpactPercent, float slimeFallPercent) {
            this.enableCreaturePhysics = enableCreaturePhysics;
            this.enableSlimePhysics = enableSlimePhysics;
            this.pendulumMode = pendulumMode;
            this.pendulumSettings = pendulumSettings;
            this.impactInclinationPercent = impactInclinationPercent;
            this.fallInclinationPercent = fallInclinationPercent;
            this.impactTranslationPercent = impactTranslationPercent;
            this.fallTranslationPercent = fallTranslationPercent;
            this.slimeImpactPercent = slimeImpactPercent;
            this.slimeFallPercent = slimeFallPercent;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, BonePhysicsSpec.class), BonePhysicsSpec.class, "enableCreaturePhysics;enableSlimePhysics;pendulumMode;pendulumSettings;impactInclinationPercent;fallInclinationPercent;impactTranslationPercent;fallTranslationPercent;slimeImpactPercent;slimeFallPercent", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableCreaturePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableSlimePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumMode:Lpunchy/client/render/ToolTuning$PhysicsMode;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumSettings:Lpunchy/client/render/ToolTuning$PhysicsSettings;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeImpactPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeFallPercent:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, BonePhysicsSpec.class), BonePhysicsSpec.class, "enableCreaturePhysics;enableSlimePhysics;pendulumMode;pendulumSettings;impactInclinationPercent;fallInclinationPercent;impactTranslationPercent;fallTranslationPercent;slimeImpactPercent;slimeFallPercent", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableCreaturePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableSlimePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumMode:Lpunchy/client/render/ToolTuning$PhysicsMode;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumSettings:Lpunchy/client/render/ToolTuning$PhysicsSettings;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeImpactPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeFallPercent:F").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, BonePhysicsSpec.class, Object.class), BonePhysicsSpec.class, "enableCreaturePhysics;enableSlimePhysics;pendulumMode;pendulumSettings;impactInclinationPercent;fallInclinationPercent;impactTranslationPercent;fallTranslationPercent;slimeImpactPercent;slimeFallPercent", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableCreaturePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->enableSlimePhysics:Z", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumMode:Lpunchy/client/render/ToolTuning$PhysicsMode;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->pendulumSettings:Lpunchy/client/render/ToolTuning$PhysicsSettings;", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallInclinationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->impactTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->fallTranslationPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeImpactPercent:F", "FIELD:Lpunchy/client/bedrockitems/BedrockItemDefinition$BonePhysicsSpec;->slimeFallPercent:F").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public boolean enableCreaturePhysics() {
            return this.enableCreaturePhysics;
        }

        public boolean enableSlimePhysics() {
            return this.enableSlimePhysics;
        }

        public ToolTuning.PhysicsMode pendulumMode() {
            return this.pendulumMode;
        }

        public ToolTuning.PhysicsSettings pendulumSettings() {
            return this.pendulumSettings;
        }

        public float impactInclinationPercent() {
            return this.impactInclinationPercent;
        }

        public float fallInclinationPercent() {
            return this.fallInclinationPercent;
        }

        public float impactTranslationPercent() {
            return this.impactTranslationPercent;
        }

        public float fallTranslationPercent() {
            return this.fallTranslationPercent;
        }

        public float slimeImpactPercent() {
            return this.slimeImpactPercent;
        }

        public float slimeFallPercent() {
            return this.slimeFallPercent;
        }

        boolean isEmpty() {
            return (this.enableCreaturePhysics || this.enableSlimePhysics || (this.pendulumMode != null && this.pendulumMode != ToolTuning.PhysicsMode.NONE)) ? false : true;
        }

        float impactInclinationScale() {
            return this.impactInclinationPercent / DEFAULT_INCLINATION_PERCENT;
        }

        float fallInclinationScale() {
            return this.fallInclinationPercent / DEFAULT_INCLINATION_PERCENT;
        }

        float impactTranslationScale() {
            return this.impactTranslationPercent / DEFAULT_INCLINATION_PERCENT;
        }

        float fallTranslationScale() {
            return this.fallTranslationPercent / DEFAULT_INCLINATION_PERCENT;
        }

        float slimeImpactScale() {
            return this.slimeImpactPercent / DEFAULT_INCLINATION_PERCENT;
        }

        float slimeFallScale() {
            return this.slimeFallPercent / DEFAULT_INCLINATION_PERCENT;
        }

        BonePhysicsSpec merge(BonePhysicsSpec other) {
            if (other == null) {
                return this;
            }
            return new BonePhysicsSpec(this.enableCreaturePhysics || other.enableCreaturePhysics, this.enableSlimePhysics || other.enableSlimePhysics, choosePendulumMode(this.pendulumMode, other.pendulumMode), mergePendulumSettings(this.pendulumSettings, other.pendulumSettings), choosePercent(this.impactInclinationPercent, other.impactInclinationPercent), choosePercent(this.fallInclinationPercent, other.fallInclinationPercent), choosePercent(this.impactTranslationPercent, other.impactTranslationPercent), choosePercent(this.fallTranslationPercent, other.fallTranslationPercent), choosePercent(this.slimeImpactPercent, other.slimeImpactPercent), choosePercent(this.slimeFallPercent, other.slimeFallPercent));
        }

        static BonePhysicsSpec fromJson(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }
            if (element.isJsonArray()) {
                BonePhysicsSpec merged = null;
                for (JsonElement child : element.getAsJsonArray()) {
                    BonePhysicsSpec childSpec = fromJson(child);
                    if (childSpec != null) {
                        merged = merged == null ? childSpec : merged.merge(childSpec);
                    }
                }
                return merged;
            }
            if (element.isJsonPrimitive()) {
                boolean enabled = BedrockItemDefinition.readBoolean(element, false);
                return new BonePhysicsSpec(enabled, enabled, ToolTuning.PhysicsMode.NONE, null, DEFAULT_INCLINATION_PERCENT, DEFAULT_INCLINATION_PERCENT, DEFAULT_INCLINATION_PERCENT, DEFAULT_INCLINATION_PERCENT, DEFAULT_INCLINATION_PERCENT, DEFAULT_INCLINATION_PERCENT);
            }
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject obj = element.getAsJsonObject();
            boolean creature = false;
            boolean slime = false;
            if (obj.has("enableCreaturePhysics")) {
                creature = BedrockItemDefinition.readBoolean(obj.get("enableCreaturePhysics"), false);
            } else if (obj.has("creature")) {
                creature = BedrockItemDefinition.readBoolean(obj.get("creature"), false);
            } else if (obj.has("puffer")) {
                creature = BedrockItemDefinition.readBoolean(obj.get("puffer"), false);
            }
            if (obj.has("enableSlimePhysics")) {
                slime = BedrockItemDefinition.readBoolean(obj.get("enableSlimePhysics"), false);
            } else if (obj.has("slime")) {
                slime = BedrockItemDefinition.readBoolean(obj.get("slime"), false);
            }
            ToolTuning.PhysicsMode pendulumMode = readPendulumMode(obj);
            ToolTuning.PhysicsSettings pendulumSettings = readPendulumSettings(obj);
            float impactPercent = readPercent(obj, "impactInclinationPercent", "impact_inclination_percent", "impactInclination", "impact_inclination", "landingInclinationPercent", "landing_inclination_percent", "landingInclination", "landing_inclination");
            float fallPercent = readPercent(obj, "fallInclinationPercent", "fall_inclination_percent", "fallInclination", "fall_inclination", "upwardInclinationPercent", "upward_inclination_percent", "upwardInclination", "upward_inclination");
            float impactTranslationPercent = readPercent(obj, "impactTranslationPercent", "impact_translation_percent", "landingTranslationPercent", "landing_translation_percent", "impactTranslation", "impact_translation", "landingTranslation", "landing_translation");
            float fallTranslationPercent = readPercent(obj, "fallTranslationPercent", "fall_translation_percent", "upwardTranslationPercent", "upward_translation_percent", "fallTranslation", "fall_translation", "upwardTranslation", "upward_translation");
            float slimeImpactPercent = readPercent(obj, "slimeImpactPercent", "slime_impact_percent", "impactSlimePercent", "impact_slime_percent", "slimeImpact", "slime_impact");
            float slimeFallPercent = readPercent(obj, "slimeFallPercent", "slime_fall_percent", "fallSlimePercent", "fall_slime_percent", "slimeFall", "slime_fall");
            return new BonePhysicsSpec(creature, slime, pendulumMode, pendulumSettings, impactPercent, fallPercent, impactTranslationPercent, fallTranslationPercent, slimeImpactPercent, slimeFallPercent);
        }

        private static float choosePercent(float current, float other) {
            return other != DEFAULT_INCLINATION_PERCENT ? other : current;
        }

        private static ToolTuning.PhysicsMode choosePendulumMode(ToolTuning.PhysicsMode current, ToolTuning.PhysicsMode other) {
            return (other == null || other == ToolTuning.PhysicsMode.NONE) ? current : other;
        }

        private static ToolTuning.PhysicsSettings mergePendulumSettings(ToolTuning.PhysicsSettings current, ToolTuning.PhysicsSettings other) {
            if (current == null) {
                if (other == null) {
                    return null;
                }
                return other.copy();
            }
            if (other == null) {
                return current.copy();
            }
            ToolTuning.PhysicsSettings merged = current.copy();
            if (other.limitDeg != null) {
                merged.limitDeg = other.limitDeg;
            }
            if (other.limitDegUp != null) {
                merged.limitDegUp = other.limitDegUp;
            }
            if (other.limitDegDown != null) {
                merged.limitDegDown = other.limitDegDown;
            }
            if (other.force != null) {
                merged.force = other.force;
            }
            if (other.speed != null) {
                merged.speed = other.speed;
            }
            if (other.cameraSens != null) {
                merged.cameraSens = other.cameraSens;
            }
            if (other.gravity != null) {
                merged.gravity = other.gravity;
            }
            if (other.motionSens != null) {
                merged.motionSens = other.motionSens;
            }
            if (other.invert != null) {
                merged.invert = other.invert;
            }
            return merged;
        }

        private static float readPercent(JsonObject obj, String... keys) {
            JsonElement value;
            if (obj == null || keys == null) {
                return DEFAULT_INCLINATION_PERCENT;
            }
            for (String key : keys) {
                if (key != null && obj.has(key) && (value = obj.get(key)) != null && !value.isJsonNull()) {
                    try {
                        return class_3532.method_15363(value.getAsFloat(), 0.0f, DEFAULT_INCLINATION_PERCENT);
                    } catch (RuntimeException e) {
                    }
                }
            }
            return DEFAULT_INCLINATION_PERCENT;
        }

        private static ToolTuning.PhysicsMode readPendulumMode(JsonObject obj) {
            if (obj == null) {
                return ToolTuning.PhysicsMode.NONE;
            }
            ToolTuning.PhysicsMode parsed = parsePendulumMode(obj.get("pendulum_mode"));
            if (parsed != ToolTuning.PhysicsMode.NONE) {
                return parsed;
            }
            ToolTuning.PhysicsMode parsed2 = parsePendulumMode(obj.get("pendulumMode"));
            if (parsed2 != ToolTuning.PhysicsMode.NONE) {
                return parsed2;
            }
            ToolTuning.PhysicsMode parsed3 = parsePendulumMode(obj.get("pendulum"));
            if (parsed3 != ToolTuning.PhysicsMode.NONE) {
                return parsed3;
            }
            return parsePendulumMode(obj.get("physics_mode"));
        }

        private static ToolTuning.PhysicsMode parsePendulumMode(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return ToolTuning.PhysicsMode.NONE;
            }
            try {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                    return element.getAsBoolean() ? ToolTuning.PhysicsMode.FULL_PENDULUM : ToolTuning.PhysicsMode.NONE;
                }
                String value = element.getAsString();
                if (value == null) {
                    return ToolTuning.PhysicsMode.NONE;
                }
                String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
                switch (normalized) {
                }
            } catch (RuntimeException e) {
                return ToolTuning.PhysicsMode.NONE;
            }
        }

        private static ToolTuning.PhysicsSettings readPendulumSettings(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            ToolTuning.PhysicsSettings settings = new ToolTuning.PhysicsSettings();
            settings.limitDeg = readOptionalFloat(obj, "limit_deg", "limitDeg", "pendulum_limit_deg", "pendulumLimitDeg");
            settings.limitDegUp = readOptionalFloat(obj, "limit_deg_up", "limitDegUp", "pendulum_limit_deg_up", "pendulumLimitDegUp");
            settings.limitDegDown = readOptionalFloat(obj, "limit_deg_down", "limitDegDown", "pendulum_limit_deg_down", "pendulumLimitDegDown");
            settings.force = readOptionalFloat(obj, "force", "pendulum_force", "pendulumForce");
            settings.speed = readOptionalFloat(obj, "speed", "pendulum_speed", "pendulumSpeed");
            settings.cameraSens = readOptionalFloat(obj, "camera_sens", "cameraSens", "pendulum_camera_sens", "pendulumCameraSens");
            settings.gravity = readOptionalFloat(obj, "gravity", "pendulum_gravity", "pendulumGravity");
            settings.motionSens = readOptionalFloat(obj, "motion_sens", "motionSens", "pendulum_motion_sens", "pendulumMotionSens");
            settings.invert = readOptionalBoolean(obj, "invert", "pendulum_invert", "pendulumInvert");
            if (hasPendulumSettings(settings)) {
                return settings;
            }
            return null;
        }

        private static boolean hasPendulumSettings(ToolTuning.PhysicsSettings settings) {
            return (settings == null || (settings.limitDeg == null && settings.limitDegUp == null && settings.limitDegDown == null && settings.force == null && settings.speed == null && settings.cameraSens == null && settings.gravity == null && settings.motionSens == null && settings.invert == null)) ? false : true;
        }

        private static Float readOptionalFloat(JsonObject obj, String... keys) {
            JsonElement value;
            if (obj == null || keys == null) {
                return null;
            }
            for (String key : keys) {
                if (key != null && obj.has(key) && (value = obj.get(key)) != null && !value.isJsonNull()) {
                    try {
                        return Float.valueOf(value.getAsFloat());
                    } catch (RuntimeException e) {
                    }
                }
            }
            return null;
        }

        private static Boolean readOptionalBoolean(JsonObject obj, String... keys) {
            JsonElement value;
            if (obj == null || keys == null) {
                return null;
            }
            for (String key : keys) {
                if (key != null && obj.has(key) && (value = obj.get(key)) != null && !value.isJsonNull()) {
                    try {
                        return Boolean.valueOf(value.getAsBoolean());
                    } catch (RuntimeException e) {
                    }
                }
            }
            return null;
        }
    }
}
