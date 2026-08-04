package punchy.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import org.joml.Vector3f;
import punchy.Punchy;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ResourcePackGeoRegistry.class */
public final class ResourcePackGeoRegistry {
    private static final Map<class_2960, BedrockGeoModel> GEO_MODELS = new HashMap();
    private static final Map<class_2960, BedrockGeoModel> MODEL_BONES = new HashMap();
    private static final Map<class_2960, class_2960> TARGET_TO_GEO = new HashMap();
    private static final Map<class_2960, class_2960> TARGET_TO_MODEL = new HashMap();
    private static final Map<class_2960, List<ModelCondition>> TARGET_TO_CONDITIONAL_MODEL = new HashMap();

    private ResourcePackGeoRegistry() {
    }

    public static void load(class_3300 manager) {
        GEO_MODELS.clear();
        MODEL_BONES.clear();
        TARGET_TO_GEO.clear();
        TARGET_TO_MODEL.clear();
        TARGET_TO_CONDITIONAL_MODEL.clear();
        if (manager == null) {
            return;
        }
        loadGeoModels(manager);
        loadModelBones(manager);
        loadMappings(manager);
    }

    public static BedrockGeoModel getGeoModel(class_2960 geoId) {
        if (geoId != null) {
            return GEO_MODELS.get(geoId);
        }
        return null;
    }

    public static class_2960 getGeoForTarget(class_2960 targetId) {
        if (targetId != null) {
            return TARGET_TO_GEO.get(targetId);
        }
        return null;
    }

    public static class_2960 getModelForTarget(class_2960 targetId) {
        if (targetId != null) {
            return TARGET_TO_MODEL.get(targetId);
        }
        return null;
    }

    public static class_2960 getModelForTarget(class_1799 stack, class_1657 player) {
        class_2960 itemId;
        if (stack == null || stack.method_7960() || player == null || (itemId = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return null;
        }
        class_2960 targetId = class_2960.method_60655(itemId.method_12836(), "item/" + itemId.method_12832());
        List<ModelCondition> conditional = TARGET_TO_CONDITIONAL_MODEL.get(targetId);
        if (conditional != null && !conditional.isEmpty()) {
            for (ModelCondition entry : conditional) {
                if (entry != null && entry.modelId != null && (entry.requiredEnchant == null || hasEnchantment(player, stack, entry.requiredEnchant))) {
                    return entry.modelId;
                }
            }
        }
        return TARGET_TO_MODEL.get(targetId);
    }

    public static Collection<class_2960> getMappedModelIds() {
        List<class_2960> ids = new ArrayList<>((Collection<? extends class_2960>) TARGET_TO_MODEL.values());
        for (List<ModelCondition> entries : TARGET_TO_CONDITIONAL_MODEL.values()) {
            if (entries != null) {
                for (ModelCondition entry : entries) {
                    if (entry != null && entry.modelId != null) {
                        ids.add(entry.modelId);
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(ids);
    }

    public static BedrockGeoModel getBonesForTarget(class_2960 targetId) {
        BedrockGeoModel geo;
        class_2960 geoId = getGeoForTarget(targetId);
        if (geoId != null && (geo = getGeoModel(geoId)) != null) {
            return geo;
        }
        class_2960 modelId = getModelForTarget(targetId);
        if (modelId != null) {
            return MODEL_BONES.get(modelId);
        }
        return null;
    }

    private static void loadGeoModels(class_3300 manager) {
        Map<class_2960, class_3298> geos = manager.method_14488("geo", loc -> {
            return loc.method_12832().startsWith("geo/") && loc.method_12832().endsWith(".geo.json");
        });
        for (Map.Entry<class_2960, class_3298> entry : geos.entrySet()) {
            class_2960 id = entry.getKey();
            try {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(entry.getValue().method_14482())).getAsJsonObject();
                BedrockGeoModel model = parseGeo(json);
                if (model != null) {
                    GEO_MODELS.put(id, model);
                }
            } catch (Exception e) {
            }
        }
    }

    private static void loadModelBones(class_3300 manager) {
        Map<class_2960, class_3298> models = manager.method_14488("models", loc -> {
            return loc.method_12832().startsWith("models/") && loc.method_12832().endsWith(".json");
        });
        for (Map.Entry<class_2960, class_3298> entry : models.entrySet()) {
            class_2960 id = entry.getKey();
            try {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(entry.getValue().method_14482())).getAsJsonObject();
                BedrockGeoModel model = parseModelAsBones(json);
                if (model != null) {
                    MODEL_BONES.put(id, model);
                }
            } catch (Exception e) {
            }
        }
    }

    private static BedrockGeoModel parseGeo(JsonObject json) {
        JsonArray geos;
        float[] pPivot;
        if (json == null || (geos = json.getAsJsonArray("minecraft:geometry")) == null || geos.isEmpty()) {
            return null;
        }
        JsonObject geo = geos.get(0).getAsJsonObject();
        JsonArray<JsonElement> bones = geo.getAsJsonArray("bones");
        if (bones == null) {
            return null;
        }
        Map<String, float[]> pivotByName = new HashMap<>();
        for (JsonElement boneEl : bones) {
            JsonObject bone = boneEl.getAsJsonObject();
            String name = bone.get("name").getAsString();
            float px = 0.0f;
            float py = 0.0f;
            float pz = 0.0f;
            if (bone.has("pivot")) {
                JsonArray pivot = bone.getAsJsonArray("pivot");
                px = pivot.get(0).getAsFloat();
                py = pivot.get(1).getAsFloat();
                pz = pivot.get(2).getAsFloat();
            }
            pivotByName.put(name, new float[]{px, py, pz});
        }
        BedrockGeoModel model = new BedrockGeoModel();
        for (JsonElement boneEl2 : bones) {
            JsonObject bone2 = boneEl2.getAsJsonObject();
            String name2 = bone2.get("name").getAsString();
            String parentName = bone2.has("parent") ? bone2.get("parent").getAsString() : null;
            float px2 = 0.0f;
            float py2 = 0.0f;
            float pz2 = 0.0f;
            float rx = 0.0f;
            float ry = 0.0f;
            float rz = 0.0f;
            if (bone2.has("pivot")) {
                JsonArray pivot2 = bone2.getAsJsonArray("pivot");
                px2 = pivot2.get(0).getAsFloat();
                py2 = pivot2.get(1).getAsFloat();
                pz2 = pivot2.get(2).getAsFloat();
            }
            if (bone2.has("rotation")) {
                JsonArray rotation = bone2.getAsJsonArray("rotation");
                rx = rotation.get(0).getAsFloat();
                ry = rotation.get(1).getAsFloat();
                rz = rotation.get(2).getAsFloat();
            }
            float localX = px2;
            float localY = py2;
            float localZ = pz2;
            if (parentName != null && (pPivot = pivotByName.get(parentName)) != null) {
                localX = px2 - pPivot[0];
                localY = py2 - pPivot[1];
                localZ = pz2 - pPivot[2];
            }
            BedrockBone bedrockBone = new BedrockBone(name2, parentName, new Vector3f(px2, py2, pz2), new Vector3f(rx, ry, rz), new Vector3f(localX, localY, localZ));
            model.addBone(bedrockBone);
        }
        return model;
    }

    private static BedrockGeoModel parseModelAsBones(JsonObject json) {
        Vector3f pivot;
        Vector3f parentPivot;
        if (json == null) {
            return null;
        }
        if (!json.has("elements") && !json.has("groups")) {
            return null;
        }
        JsonArray<JsonElement> elements = json.getAsJsonArray("elements");
        JsonArray groups = json.getAsJsonArray("groups");
        Map<Integer, String> elementParents = new HashMap<>();
        Map<String, Vector3f> groupPivots = new HashMap<>();
        Map<String, String> groupParents = new HashMap<>();
        BedrockGeoModel model = new BedrockGeoModel();
        if (groups != null) {
            parseGroups(groups, null, groupPivots, groupParents, elementParents, model);
        }
        int index = 0;
        if (elements == null) {
            return model;
        }
        for (JsonElement el : elements) {
            JsonObject elem = el.getAsJsonObject();
            String name = elem.has("name") ? elem.get("name").getAsString() : "cube_" + index;
            Vector3f from = readVec3(elem, "from", new Vector3f(0.0f, 0.0f, 0.0f));
            Vector3f to = readVec3(elem, "to", new Vector3f(0.0f, 0.0f, 0.0f));
            Vector3f rotation = new Vector3f(0.0f, 0.0f, 0.0f);
            if (elem.has("rotation")) {
                JsonObject rot = elem.getAsJsonObject("rotation");
                float angle = rot.has("angle") ? rot.get("angle").getAsFloat() : 0.0f;
                String axis = rot.has("axis") ? rot.get("axis").getAsString() : "y";
                if ("x".equals(axis)) {
                    rotation.x = angle;
                }
                if ("y".equals(axis)) {
                    rotation.y = angle;
                }
                if ("z".equals(axis)) {
                    rotation.z = angle;
                }
                if (rot.has("origin")) {
                    JsonArray origin = rot.getAsJsonArray("origin");
                    pivot = new Vector3f(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat());
                } else {
                    pivot = new Vector3f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f, (from.z + to.z) * 0.5f);
                }
            } else {
                pivot = new Vector3f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f, (from.z + to.z) * 0.5f);
            }
            String parent = elementParents.get(Integer.valueOf(index));
            Vector3f localPivot = pivot;
            if (parent != null && (parentPivot = groupPivots.get(parent)) != null) {
                localPivot = new Vector3f(pivot.x - parentPivot.x, pivot.y - parentPivot.y, pivot.z - parentPivot.z);
            }
            BedrockBone bone = new BedrockBone(name, parent, pivot, rotation, localPivot);
            model.addBone(bone);
            index++;
        }
        return model;
    }

    private static void parseGroups(JsonArray groups, String parentName, Map<String, Vector3f> groupPivots, Map<String, String> groupParents, Map<Integer, String> elementParents, BedrockGeoModel model) {
        Vector3f parentPivot;
        Iterator it = groups.iterator();
        while (it.hasNext()) {
            JsonElement groupEl = (JsonElement) it.next();
            if (groupEl.isJsonObject()) {
                JsonObject group = groupEl.getAsJsonObject();
                if (group.has("name")) {
                    String name = group.get("name").getAsString();
                    Vector3f origin = readVec3(group, "origin", new Vector3f(0.0f, 0.0f, 0.0f));
                    Vector3f rotation = readVec3(group, "rotation", new Vector3f(0.0f, 0.0f, 0.0f));
                    Vector3f localPivot = origin;
                    if (parentName != null && (parentPivot = groupPivots.get(parentName)) != null) {
                        localPivot = new Vector3f(origin.x - parentPivot.x, origin.y - parentPivot.y, origin.z - parentPivot.z);
                    }
                    groupPivots.put(name, origin);
                    if (parentName != null) {
                        groupParents.put(name, parentName);
                    }
                    model.addBone(new BedrockBone(name, parentName, origin, rotation, localPivot));
                    if (group.has("children")) {
                        JsonArray<JsonElement> children = group.getAsJsonArray("children");
                        for (JsonElement childEl : children) {
                            if (childEl.isJsonPrimitive() && childEl.getAsJsonPrimitive().isNumber()) {
                                elementParents.put(Integer.valueOf(childEl.getAsInt()), name);
                            } else if (childEl.isJsonObject()) {
                                JsonArray nested = new JsonArray();
                                nested.add(childEl);
                                parseGroups(nested, name, groupPivots, groupParents, elementParents, model);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Vector3f readVec3(JsonObject obj, String key, Vector3f fallback) {
        if (!obj.has(key)) {
            return fallback;
        }
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr == null || arr.size() < 3) {
            return fallback;
        }
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private static void loadMappings(class_3300 manager) {
        class_2960 targetId;
        class_2960 modelId;
        class_2960 geoId;
        Map<class_2960, class_3298> maps = manager.method_14488(Punchy.MOD_ID, loc -> {
            return loc.method_12832().startsWith("punchy/") && loc.method_12832().endsWith("rp_geo_map.json");
        });
        for (Map.Entry<class_2960, class_3298> entry : maps.entrySet()) {
            try {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(entry.getValue().method_14482())).getAsJsonObject();
                JsonArray<JsonElement> mappings = json.getAsJsonArray("mappings");
                if (mappings != null) {
                    for (JsonElement mapEl : mappings) {
                        JsonObject map = mapEl.getAsJsonObject();
                        if (map.has("target") && (targetId = class_2960.method_12829(map.get("target").getAsString())) != null) {
                            if (map.has("geo") && (geoId = class_2960.method_12829(map.get("geo").getAsString())) != null) {
                                TARGET_TO_GEO.put(targetId, geoId);
                            }
                            if (map.has("model") && (modelId = resolveModelId(map.get("model").getAsString())) != null) {
                                class_2960 requiredEnchant = null;
                                if (map.has("require_enchant")) {
                                    requiredEnchant = class_2960.method_12829(map.get("require_enchant").getAsString());
                                }
                                if (requiredEnchant != null) {
                                    TARGET_TO_CONDITIONAL_MODEL.computeIfAbsent(targetId, key -> {
                                        return new ArrayList();
                                    }).add(new ModelCondition(modelId, requiredEnchant));
                                } else {
                                    TARGET_TO_MODEL.put(targetId, modelId);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private static class_2960 resolveModelId(String modelId) {
        class_2960 raw = class_2960.method_12829(modelId);
        if (raw == null) {
            return null;
        }
        String path = raw.method_12832();
        if (!path.startsWith("models/")) {
            path = "models/" + path;
        }
        if (!path.endsWith(".json")) {
            path = path + ".json";
        }
        return class_2960.method_60655(raw.method_12836(), path);
    }

    private static boolean hasEnchantment(class_1657 player, class_1799 stack, class_2960 enchantId) {
        if (player == null || stack == null) {
            return false;
        }
        try {
            if (stack.method_7960() || enchantId == null) {
                return false;
            }
            class_2378<class_1887> registry = player.method_37908().method_30349().method_30530(class_7924.field_41265);
            class_5321<class_1887> key = class_5321.method_29179(class_7924.field_41265, enchantId);
            class_6880.class_6883<class_1887> holder = registry.method_46747(key);
            return class_1890.method_8225(holder, stack) > 0;
        } catch (Throwable th) {
            return false;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ResourcePackGeoRegistry$ModelCondition.class */
    private static final class ModelCondition {
        private final class_2960 modelId;
        private final class_2960 requiredEnchant;

        private ModelCondition(class_2960 modelId, class_2960 requiredEnchant) {
            this.modelId = modelId;
            this.requiredEnchant = requiredEnchant;
        }
    }
}
