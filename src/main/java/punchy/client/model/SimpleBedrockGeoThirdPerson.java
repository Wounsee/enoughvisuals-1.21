package punchy.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_5603;
import net.minecraft.class_5606;
import net.minecraft.class_5607;
import net.minecraft.class_5609;
import net.minecraft.class_5610;
import net.minecraft.class_630;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/model/SimpleBedrockGeoThirdPerson.class */
public class SimpleBedrockGeoThirdPerson {
    public static Map<String, class_630> loadedParts = new HashMap();
    public static Map<String, Vector3f> bedrockLocalPivots = new HashMap();
    public static Map<String, String> parentByName = new HashMap();
    public static Map<String, Matrix4f> restWorldByName = new HashMap();

    public static void load(class_2960 geoId) {
        float[] pPivot;
        try {
            class_3298 resource = class_310.method_1551().method_1478().getResourceOrThrow(geoId);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(resource.method_14482())).getAsJsonObject();
            JsonArray geos = json.getAsJsonArray("minecraft:geometry");
            if (geos == null || geos.isEmpty()) {
                return;
            }
            JsonObject geo = geos.get(0).getAsJsonObject();
            JsonArray<JsonElement> bones = geo.getAsJsonArray("bones");
            if (bones == null) {
                return;
            }
            class_5609 mesh = new class_5609();
            class_5610 root = mesh.method_32111();
            Map<String, class_5610> definitions = new HashMap<>();
            Map<String, float[]> pivotByName = new HashMap<>();
            bedrockLocalPivots.clear();
            parentByName.clear();
            for (JsonElement boneEl : bones) {
                JsonObject bone = boneEl.getAsJsonObject();
                String name = bone.get("name").getAsString();
                if (bone.has("parent")) {
                    parentByName.put(name, bone.get("parent").getAsString());
                }
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
                bedrockLocalPivots.put(name2, new Vector3f(localX, localY, localZ));
                class_5606 cubes = class_5606.method_32108();
                if (bone2.has("cubes")) {
                    for (JsonElement cubeEl : bone2.getAsJsonArray("cubes")) {
                        JsonObject cube = cubeEl.getAsJsonObject();
                        JsonArray origin = cube.getAsJsonArray("origin");
                        JsonArray size = cube.getAsJsonArray("size");
                        JsonArray uv = cube.getAsJsonArray("uv");
                        float ox = origin.get(0).getAsFloat();
                        float oy = origin.get(1).getAsFloat();
                        float oz = origin.get(2).getAsFloat();
                        float sx = size.get(0).getAsFloat();
                        float sy = size.get(1).getAsFloat();
                        float sz = size.get(2).getAsFloat();
                        int u = uv.get(0).getAsInt();
                        int v = uv.get(1).getAsInt();
                        float mcBoxX = ox - px2;
                        float mcBoxY = py2 - (oy + sy);
                        float mcBoxZ = oz - pz2;
                        cubes.method_32101(u, v).method_32097(mcBoxX, mcBoxY, mcBoxZ, sx, sy, sz);
                    }
                }
                class_5610 parentDef = parentName != null ? definitions.get(parentName) : root;
                if (parentDef == null) {
                    parentDef = root;
                }
                float renderPX = px2;
                float renderPY = 28.0f - py2;
                float renderPZ = pz2;
                if (parentName != null) {
                    JsonObject pBone = null;
                    Iterator it = bones.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        JsonElement el = (JsonElement) it.next();
                        if (el.getAsJsonObject().get("name").getAsString().equals(parentName)) {
                            pBone = el.getAsJsonObject();
                            break;
                        }
                    }
                    if (pBone != null && pBone.has("pivot")) {
                        JsonArray pPivot2 = pBone.getAsJsonArray("pivot");
                        renderPX = px2 - pPivot2.get(0).getAsFloat();
                        renderPY = pPivot2.get(1).getAsFloat() - py2;
                        renderPZ = pPivot2.get(2).getAsFloat() - pz2;
                    }
                }
                float radX = (float) Math.toRadians(rx);
                float radY = (float) Math.toRadians(ry);
                float radZ = (float) Math.toRadians(rz);
                definitions.put(name2, parentDef.method_32117(name2, cubes, class_5603.method_32091(renderPX, renderPY, -renderPZ, radX, radY, radZ)));
            }
            class_630 rootPart = class_5607.method_32110(mesh, 64, 64).method_32109();
            loadedParts.clear();
            restWorldByName.clear();
            for (JsonElement boneEl3 : bones) {
                String name3 = boneEl3.getAsJsonObject().get("name").getAsString();
                try {
                    class_630 part = findChildRecursive(rootPart, name3);
                    if (part != null) {
                        loadedParts.put(name3, part);
                    }
                } catch (Exception e) {
                }
            }
            for (String boneName : loadedParts.keySet()) {
                Matrix4f world = buildRestWorldMatrix(boneName, new HashSet());
                if (world != null) {
                    restWorldByName.put(boneName, world);
                }
            }
        } catch (Exception e2) {
        }
    }

    private static class_630 findChildRecursive(class_630 root, String name) {
        if (root == null || name == null) {
            return null;
        }
        try {
            class_630 direct = root.method_32086(name);
            if (direct != null) {
                return direct;
            }
        } catch (Throwable th) {
        }
        Map<String, class_630> children = getChildren(root);
        if (children == null || children.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, class_630> entry : children.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.equals(name)) {
                return entry.getValue();
            }
        }
        for (class_630 child : children.values()) {
            class_630 found = findChildRecursive(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Map<String, class_630> getChildren(class_630 part) {
        Class<?> superclass = part.getClass();
        while (true) {
            Class<?> type = superclass;
            if (type != null && type != Object.class) {
                for (Field field : type.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(part);
                            if (value instanceof Map) {
                                return (Map) value;
                            }
                            continue;
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
                superclass = type.getSuperclass();
            } else {
                return null;
            }
        }
    }

    private static Matrix4f buildRestWorldMatrix(String boneName, Set<String> visited) {
        if (boneName == null || visited.contains(boneName)) {
            return null;
        }
        visited.add(boneName);
        class_630 part = loadedParts.get(boneName);
        if (part == null) {
            return null;
        }
        Matrix4f local = new Matrix4f().translate(part.field_3657, part.field_3656, part.field_3655).rotateZ(part.field_3674).rotateY(part.field_3675).rotateX(part.field_3654).scale(part.field_37938, part.field_37939, part.field_37940);
        String parent = parentByName.get(boneName);
        if (parent == null) {
            return local;
        }
        Matrix4f parentWorld = buildRestWorldMatrix(parent, visited);
        if (parentWorld == null) {
            return local;
        }
        return parentWorld.mul(local, new Matrix4f());
    }
}
