package punchy.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1306;
import net.minecraft.class_2350;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import org.joml.Vector3f;
import punchy.client.model.ItemModelDefinition;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/ResourcePackItemModelRegistry.class */
public final class ResourcePackItemModelRegistry {
    private static final Map<class_2960, ItemModelDefinition> MODELS = new HashMap();

    private ResourcePackItemModelRegistry() {
    }

    public static void load(class_3300 manager) {
        MODELS.clear();
        if (manager == null) {
            return;
        }
        for (class_2960 modelId : ResourcePackGeoRegistry.getMappedModelIds()) {
            if (modelId != null) {
                try {
                    class_3298 resource = manager.getResourceOrThrow(modelId);
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(resource.method_14482())).getAsJsonObject();
                    ItemModelDefinition model = parseModel(modelId, json);
                    if (model != null) {
                        MODELS.put(modelId, model);
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    public static ItemModelDefinition getModel(class_2960 modelId) {
        if (modelId == null) {
            return null;
        }
        return MODELS.get(modelId);
    }

    private static ItemModelDefinition parseModel(class_2960 modelId, JsonObject json) {
        JsonArray texSize;
        if (modelId == null || json == null || !json.has("elements")) {
            return null;
        }
        int texWidth = 16;
        int texHeight = 16;
        if (json.has("texture_size") && (texSize = json.getAsJsonArray("texture_size")) != null && texSize.size() >= 2) {
            texWidth = texSize.get(0).getAsInt();
            texHeight = texSize.get(1).getAsInt();
        }
        Map<String, String> textures = new HashMap<>();
        if (json.has("textures")) {
            JsonObject texturesObj = json.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : texturesObj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    textures.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        Map<Integer, String> elementParents = new HashMap<>();
        Map<String, Vector3f> groupPivots = new HashMap<>();
        Map<String, Vector3f> groupRotations = new HashMap<>();
        Map<String, String> groupParents = new HashMap<>();
        Map<String, ItemModelDefinition.BoneDef> bones = new LinkedHashMap<>();
        Map<String, String> parentByBone = new HashMap<>();
        Set<String> usedNames = new HashSet<>();
        if (json.has("groups")) {
            parseGroups(json.getAsJsonArray("groups"), null, groupPivots, groupRotations, groupParents, elementParents);
            for (Map.Entry<String, Vector3f> entry2 : groupPivots.entrySet()) {
                String name = entry2.getKey();
                String parent = groupParents.get(name);
                bones.put(name, new ItemModelDefinition.BoneDef(name, parent, entry2.getValue(), groupRotations.getOrDefault(name, new Vector3f())));
                parentByBone.put(name, parent);
                usedNames.add(name);
            }
        }
        JsonArray<JsonElement> elements = json.getAsJsonArray("elements");
        List<ItemModelDefinition.Element> modelElements = new ArrayList<>();
        if (elements != null) {
            int index = 0;
            for (JsonElement el : elements) {
                JsonObject elem = el.getAsJsonObject();
                String rawName = elem.has("name") ? elem.get("name").getAsString() : "cube_" + index;
                String name2 = uniqueName(rawName, usedNames, index);
                Vector3f from = readVec3(elem, "from", new Vector3f(0.0f, 0.0f, 0.0f));
                Vector3f to = readVec3(elem, "to", new Vector3f(0.0f, 0.0f, 0.0f));
                Vector3f pivot = new Vector3f((from.x + to.x) * 0.5f, (from.y + to.y) * 0.5f, (from.z + to.z) * 0.5f);
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
                        pivot = readVec3(rot, "origin", pivot);
                    }
                }
                String parent2 = elementParents.get(Integer.valueOf(index));
                bones.put(name2, new ItemModelDefinition.BoneDef(name2, parent2, pivot, rotation));
                parentByBone.put(name2, parent2);
                usedNames.add(name2);
                Vector3f localFrom = new Vector3f(from.x - pivot.x, from.y - pivot.y, from.z - pivot.z);
                Vector3f localTo = new Vector3f(to.x - pivot.x, to.y - pivot.y, to.z - pivot.z);
                EnumMap<class_2350, ItemModelDefinition.Face> faces = parseFaces(elem.getAsJsonObject("faces"));
                int lightEmission = 0;
                if (elem.has("light_emission")) {
                    lightEmission = elem.get("light_emission").getAsInt();
                    if (lightEmission < 0) {
                        lightEmission = 0;
                    } else if (lightEmission > 15) {
                        lightEmission = 15;
                    }
                }
                boolean worldLight = elem.has("world_light") && elem.get("world_light").getAsBoolean();
                modelElements.add(new ItemModelDefinition.Element(name2, localFrom, localTo, faces, lightEmission, worldLight));
                index++;
            }
        }
        ItemModelDefinition model = new ItemModelDefinition(modelId.method_12836(), texWidth, texHeight, textures, modelElements, bones, parentByBone, ItemModelDefinition.buildPaths(parentByBone));
        parseDisplay(json, model);
        return model;
    }

    private static void parseGroups(JsonArray groups, String parentName, Map<String, Vector3f> groupPivots, Map<String, Vector3f> groupRotations, Map<String, String> groupParents, Map<Integer, String> elementParents) {
        if (groups == null) {
            return;
        }
        Iterator it = groups.iterator();
        while (it.hasNext()) {
            JsonElement groupEl = (JsonElement) it.next();
            if (groupEl.isJsonObject()) {
                JsonObject group = groupEl.getAsJsonObject();
                if (group.has("name")) {
                    String name = group.get("name").getAsString();
                    Vector3f origin = readVec3(group, "origin", new Vector3f(0.0f, 0.0f, 0.0f));
                    Vector3f rotation = readVec3(group, "rotation", new Vector3f(0.0f, 0.0f, 0.0f));
                    groupPivots.put(name, origin);
                    groupRotations.put(name, rotation);
                    if (parentName != null) {
                        groupParents.put(name, parentName);
                    }
                    if (group.has("children")) {
                        JsonArray<JsonElement> children = group.getAsJsonArray("children");
                        for (JsonElement child : children) {
                            if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isNumber()) {
                                elementParents.put(Integer.valueOf(child.getAsInt()), name);
                            } else if (child.isJsonObject()) {
                                JsonArray nested = new JsonArray();
                                nested.add(child);
                                parseGroups(nested, name, groupPivots, groupRotations, groupParents, elementParents);
                            }
                        }
                    }
                }
            }
        }
    }

    private static EnumMap<class_2350, ItemModelDefinition.Face> parseFaces(JsonObject facesObj) {
        JsonArray uv;
        EnumMap<class_2350, ItemModelDefinition.Face> faces = new EnumMap<>(class_2350.class);
        if (facesObj == null) {
            return faces;
        }
        for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
            class_2350 dir = parseDirection(entry.getKey());
            if (dir != null && entry.getValue().isJsonObject()) {
                JsonObject faceObj = entry.getValue().getAsJsonObject();
                if (faceObj.has("uv") && faceObj.has("texture") && (uv = faceObj.getAsJsonArray("uv")) != null && uv.size() >= 4) {
                    float u0 = uv.get(0).getAsFloat();
                    float v0 = uv.get(1).getAsFloat();
                    float u1 = uv.get(2).getAsFloat();
                    float v1 = uv.get(3).getAsFloat();
                    int rotation = faceObj.has("rotation") ? faceObj.get("rotation").getAsInt() : 0;
                    String texture = faceObj.get("texture").getAsString();
                    faces.put(dir, new ItemModelDefinition.Face(u0, v0, u1, v1, rotation, texture));
                }
            }
        }
        return faces;
    }

    private static class_2350 parseDirection(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toLowerCase()) {
            case "north":
                return class_2350.field_11043;
            case "south":
                return class_2350.field_11035;
            case "east":
                return class_2350.field_11034;
            case "west":
                return class_2350.field_11039;
            case "up":
                return class_2350.field_11036;
            case "down":
                return class_2350.field_11033;
            default:
                return null;
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

    private static void parseDisplay(JsonObject json, ItemModelDefinition model) {
        if (json == null || model == null || !json.has("display")) {
            return;
        }
        JsonObject display = json.getAsJsonObject("display");
        boolean rightThird = parseDisplayEntry(display, "thirdperson_righthand", class_1306.field_6183, model);
        boolean leftThird = parseDisplayEntry(display, "thirdperson_lefthand", class_1306.field_6182, model);
        if (!rightThird) {
            parseDisplayEntry(display, "firstperson_righthand", class_1306.field_6183, model);
        }
        if (!leftThird) {
            parseDisplayEntry(display, "firstperson_lefthand", class_1306.field_6182, model);
        }
    }

    private static boolean parseDisplayEntry(JsonObject display, String key, class_1306 arm, ItemModelDefinition model) {
        if (!display.has(key)) {
            return false;
        }
        JsonObject entry = display.getAsJsonObject(key);
        Vector3f rot = readVec3(entry, "rotation", new Vector3f(0.0f, 0.0f, 0.0f));
        Vector3f trans = readVec3(entry, "translation", new Vector3f(0.0f, 0.0f, 0.0f));
        Vector3f scale = readVec3(entry, "scale", new Vector3f(1.0f, 1.0f, 1.0f));
        model.setDisplayTransform(arm, new ItemModelDefinition.DisplayTransform(rot, trans, scale));
        return true;
    }

    private static String uniqueName(String name, Set<String> used, int index) {
        if (name == null || name.isBlank()) {
            name = "cube_" + index;
        }
        if (!used.contains(name)) {
            return name;
        }
        String candidate = name + "_" + index;
        if (!used.contains(candidate)) {
            return candidate;
        }
        String candidate2 = "cube_" + index;
        used.add(candidate2);
        return candidate2;
    }
}
