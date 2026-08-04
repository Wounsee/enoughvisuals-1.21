package punchy.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_7923;
import net.minecraft.class_804;
import net.minecraft.class_811;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ResourcePackItemTransformResolver.class */
public final class ResourcePackItemTransformResolver {
    private static class_3300 cachedManager;
    private static final Map<class_2960, JsonObject> ITEM_DEFINITIONS = new HashMap();
    private static final Map<class_2960, JsonObject> MODEL_DEFINITIONS = new HashMap();

    private ResourcePackItemTransformResolver() {
    }

    public static class_804 resolve(class_1799 stack, class_811 context) {
        class_310 mc;
        class_2960 modelId;
        if (stack == null || stack.method_7960() || context == null || (mc = class_310.method_1551()) == null || mc.method_1478() == null) {
            return null;
        }
        class_3300 manager = mc.method_1478();
        if (manager != cachedManager) {
            cachedManager = manager;
            ITEM_DEFINITIONS.clear();
            MODEL_DEFINITIONS.clear();
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId == null) {
            return null;
        }
        class_2960 itemDefId = class_2960.method_60655(itemId.method_12836(), "items/" + itemId.method_12832() + ".json");
        JsonObject itemDef = loadPreferredResourceJson(manager, itemDefId, ITEM_DEFINITIONS);
        if (itemDef == null || !itemDef.has("model") || !itemDef.get("model").isJsonObject() || (modelId = resolveModelId(itemDef.getAsJsonObject("model"), stack, context)) == null) {
            return null;
        }
        return loadDisplayTransform(manager, modelId, context);
    }

    private static class_2960 resolveModelId(JsonObject node, class_1799 stack, class_811 context) {
        class_2960 resolved;
        if (node == null) {
            return null;
        }
        String type = getString(node, "type");
        if ("minecraft:model".equals(type)) {
            String model = getString(node, "model");
            if (model == null) {
                return null;
            }
            return class_2960.method_12829(model);
        }
        if ("minecraft:condition".equals(type)) {
            if ("minecraft:using_item".equals(getString(node, "property"))) {
                class_2960 onFalse = resolveBranch(node, "on_false", stack, context);
                if (onFalse != null) {
                    return onFalse;
                }
                return resolveBranch(node, "on_true", stack, context);
            }
            class_2960 onFalse2 = resolveBranch(node, "on_false", stack, context);
            if (onFalse2 != null) {
                return onFalse2;
            }
            return resolveBranch(node, "on_true", stack, context);
        }
        if ("minecraft:range_dispatch".equals(type)) {
            JsonArray<JsonElement> entries = node.getAsJsonArray("entries");
            if (entries != null) {
                for (JsonElement entryEl : entries) {
                    if (entryEl.isJsonObject()) {
                        JsonObject entry = entryEl.getAsJsonObject();
                        if (entry.has("model") && entry.get("model").isJsonObject() && (resolved = resolveModelId(entry.getAsJsonObject("model"), stack, context)) != null) {
                            return resolved;
                        }
                    }
                }
            }
            return resolveFallback(node, stack, context);
        }
        if ("minecraft:select".equals(type)) {
            String property = getString(node, "property");
            if (property == null) {
                return resolveFallback(node, stack, context);
            }
            if ("minecraft:display_context".equals(property)) {
                String contextName = toDisplayContextName(context);
                JsonArray<JsonElement> cases = node.getAsJsonArray("cases");
                if (cases != null) {
                    for (JsonElement caseEl : cases) {
                        if (caseEl.isJsonObject()) {
                            JsonObject caseObj = caseEl.getAsJsonObject();
                            if (matchesDisplayContext(caseObj.get("when"), contextName) && caseObj.has("model") && caseObj.get("model").isJsonObject()) {
                                return resolveModelId(caseObj.getAsJsonObject("model"), stack, context);
                            }
                        }
                    }
                }
                return resolveFallback(node, stack, context);
            }
            if ("minecraft:component".equals(property)) {
                return resolveFallback(node, stack, context);
            }
        }
        return resolveFallback(node, stack, context);
    }

    private static class_2960 resolveFallback(JsonObject node, class_1799 stack, class_811 context) {
        if (node == null || !node.has("fallback") || !node.get("fallback").isJsonObject()) {
            return null;
        }
        return resolveModelId(node.getAsJsonObject("fallback"), stack, context);
    }

    private static class_2960 resolveBranch(JsonObject node, String branchKey, class_1799 stack, class_811 context) {
        if (node == null || branchKey == null || !node.has(branchKey) || !node.get(branchKey).isJsonObject()) {
            return null;
        }
        return resolveModelId(node.getAsJsonObject(branchKey), stack, context);
    }

    private static boolean matchesDisplayContext(JsonElement whenElement, String contextName) {
        if (whenElement == null || contextName == null) {
            return false;
        }
        if (whenElement.isJsonPrimitive()) {
            return contextName.equals(whenElement.getAsString());
        }
        if (whenElement.isJsonArray()) {
            for (JsonElement element : whenElement.getAsJsonArray()) {
                if (element != null && element.isJsonPrimitive() && contextName.equals(element.getAsString())) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: renamed from: punchy.client.render.ResourcePackItemTransformResolver$1, reason: invalid class name */
    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ResourcePackItemTransformResolver$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$minecraft$world$item$ItemDisplayContext = new int[class_811.values().length];

        static {
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4321.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4322.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4323.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4320.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4316.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4317.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4318.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$net$minecraft$world$item$ItemDisplayContext[class_811.field_4319.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    private static String toDisplayContextName(class_811 context) {
        switch (AnonymousClass1.$SwitchMap$net$minecraft$world$item$ItemDisplayContext[context.ordinal()]) {
            case 1:
                return "firstperson_lefthand";
            case 2:
                return "firstperson_righthand";
            case 3:
                return "thirdperson_lefthand";
            case 4:
                return "thirdperson_righthand";
            case 5:
                return "head";
            case 6:
                return "gui";
            case 7:
                return "ground";
            case 8:
                return "fixed";
            default:
                return context.name().toLowerCase(Locale.ROOT);
        }
    }

    private static class_804 loadDisplayTransform(class_3300 manager, class_2960 modelId, class_811 context) {
        class_2960 parentId;
        class_2960 modelResId = class_2960.method_60655(modelId.method_12836(), "models/" + modelId.method_12832() + ".json");
        JsonObject modelJson = loadPreferredResourceJson(manager, modelResId, MODEL_DEFINITIONS);
        if (modelJson == null) {
            return null;
        }
        class_804 direct = parseDisplayTransform(modelJson, context);
        if (direct != null) {
            return direct;
        }
        String parent = getString(modelJson, "parent");
        if (parent == null || parent.isBlank() || (parentId = class_2960.method_12829(parent)) == null || parentId.equals(modelId)) {
            return null;
        }
        return loadDisplayTransform(manager, parentId, context);
    }

    private static class_804 parseDisplayTransform(JsonObject modelJson, class_811 context) {
        if (modelJson == null || context == null || !modelJson.has("display") || !modelJson.get("display").isJsonObject()) {
            return null;
        }
        JsonObject display = modelJson.getAsJsonObject("display");
        String key = toDisplayContextName(context);
        if (!display.has(key) || !display.get(key).isJsonObject()) {
            return null;
        }
        JsonObject entry = display.getAsJsonObject(key);
        Vector3f rotation = readVec3(entry, "rotation", new Vector3f());
        Vector3f translation = readVec3(entry, "translation", new Vector3f()).mul(0.0625f);
        Vector3f scale = readVec3(entry, "scale", new Vector3f(1.0f, 1.0f, 1.0f));
        return new class_804(rotation, translation, scale);
    }

    private static JsonObject loadPreferredResourceJson(class_3300 manager, class_2960 id, Map<class_2960, JsonObject> cache) {
        if (manager == null || id == null) {
            return null;
        }
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        try {
            List<class_3298> stack = manager.method_14489(id);
            if (stack == null || stack.isEmpty()) {
                cache.put(id, null);
                return null;
            }
            class_3298 preferred = pickPreferredResource(stack);
            if (preferred == null) {
                cache.put(id, null);
                return null;
            }
            InputStreamReader reader = new InputStreamReader(preferred.method_14482());
            try {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                cache.put(id, json);
                reader.close();
                return json;
            } finally {
            }
        } catch (Exception e) {
            cache.put(id, null);
            return null;
        }
    }

    private static class_3298 pickPreferredResource(List<class_3298> stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        class_3298 fallback = null;
        for (int i = stack.size() - 1; i >= 0; i--) {
            class_3298 resource = stack.get(i);
            if (resource != null) {
                if (fallback == null) {
                    fallback = resource;
                }
                String packId = resource.method_14480();
                if (!isFaPlayerPack(packId)) {
                    return resource;
                }
            }
        }
        return fallback;
    }

    private static boolean isFaPlayerPack(String packId) {
        return packId != null && packId.toLowerCase(Locale.ROOT).contains("faplayer");
    }

    private static Vector3f readVec3(JsonObject obj, String key, Vector3f fallback) {
        if (obj == null || key == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray array = obj.getAsJsonArray(key);
        if (array.size() < 3) {
            return fallback;
        }
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return null;
        }
        return obj.get(key).getAsString();
    }
}
