package punchy.client.bedrockitems;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_2350;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_5603;
import net.minecraft.class_5606;
import net.minecraft.class_5607;
import net.minecraft.class_5609;
import net.minecraft.class_5610;
import net.minecraft.class_630;
import punchy.client.bedrockitems.BedrockItemGeoModel;
import punchy.mixin.client.accessor.ModelPartAccessor;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/bedrockitems/BedrockItemGeoLoader.class */
final class BedrockItemGeoLoader {
    private BedrockItemGeoLoader() {
    }

    static BedrockItemGeoModel load(class_3300 manager, class_2960 geoId) {
        JsonObject geo;
        JsonArray<JsonElement> bones;
        float[] pPivot;
        JsonArray uv;
        JsonArray cubeRot;
        JsonArray cubePivot;
        if (manager == null || geoId == null) {
            return null;
        }
        try {
            class_3298 resource = manager.getResourceOrThrow(geoId);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(resource.method_14482())).getAsJsonObject();
            JsonArray geos = json.getAsJsonArray("minecraft:geometry");
            if (geos == null || geos.isEmpty() || (bones = (geo = geos.get(0).getAsJsonObject()).getAsJsonArray("bones")) == null) {
                return null;
            }
            int texW = 64;
            int texH = 64;
            if (geo.has("description")) {
                JsonObject desc = geo.getAsJsonObject("description");
                if (desc != null) {
                    if (desc.has("texture_width")) {
                        texW = desc.get("texture_width").getAsInt();
                    }
                    if (desc.has("texture_height")) {
                        texH = desc.get("texture_height").getAsInt();
                    }
                }
            } else {
                if (geo.has("texture_width")) {
                    texW = geo.get("texture_width").getAsInt();
                }
                if (geo.has("texture_height")) {
                    texH = geo.get("texture_height").getAsInt();
                }
            }
            class_5609 mesh = new class_5609();
            class_5610 root = mesh.method_32111();
            Map<String, class_5610> definitions = new HashMap<>();
            Map<String, float[]> pivotByName = new HashMap<>();
            Map<String, String> parentByBone = new HashMap<>();
            Map<String, List<BedrockItemGeoModel.BedrockCube>> cubesByBone = new HashMap<>();
            for (JsonElement boneEl : bones) {
                JsonObject bone = boneEl.getAsJsonObject();
                String name = bone.get("name").getAsString();
                float px = 0.0f;
                float py = 0.0f;
                float pz = 0.0f;
                if (bone.has("pivot")) {
                    JsonArray pivot = bone.getAsJsonArray("pivot");
                    px = getFloat(pivot, 0);
                    py = getFloat(pivot, 1);
                    pz = getFloat(pivot, 2);
                }
                pivotByName.put(name, new float[]{px, py, pz});
            }
            for (JsonElement boneEl2 : bones) {
                JsonObject bone2 = boneEl2.getAsJsonObject();
                String name2 = bone2.get("name").getAsString();
                String parentName = bone2.has("parent") ? bone2.get("parent").getAsString() : null;
                if (parentName != null) {
                    parentByBone.put(name2, parentName);
                }
                float px2 = 0.0f;
                float py2 = 0.0f;
                float pz2 = 0.0f;
                float rx = 0.0f;
                float ry = 0.0f;
                float rz = 0.0f;
                if (bone2.has("pivot")) {
                    JsonArray pivot2 = bone2.getAsJsonArray("pivot");
                    px2 = getFloat(pivot2, 0);
                    py2 = getFloat(pivot2, 1);
                    pz2 = getFloat(pivot2, 2);
                }
                if (bone2.has("rotation")) {
                    JsonArray rotation = bone2.getAsJsonArray("rotation");
                    rx = -getFloat(rotation, 0);
                    ry = -getFloat(rotation, 1);
                    rz = getFloat(rotation, 2);
                }
                class_5606 cubes = class_5606.method_32108();
                if (bone2.has("cubes")) {
                    for (JsonElement cubeEl : bone2.getAsJsonArray("cubes")) {
                        JsonObject cube = cubeEl.getAsJsonObject();
                        JsonArray origin = cube.getAsJsonArray("origin");
                        JsonArray size = cube.getAsJsonArray("size");
                        if (origin != null && size != null) {
                            float ox = getFloat(origin, 0);
                            float oy = getFloat(origin, 1);
                            float oz = getFloat(origin, 2);
                            float sx = getFloat(size, 0);
                            float sy = getFloat(size, 1);
                            float sz = getFloat(size, 2);
                            float inflate = 0.0f;
                            if (cube.has("inflate")) {
                                inflate = cube.get("inflate").getAsFloat();
                                if (inflate < 0.0f) {
                                    inflate = 0.0f;
                                }
                            }
                            boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
                            float pivotX = ox;
                            float pivotY = oy;
                            float pivotZ = oz;
                            if (cube.has("pivot") && (cubePivot = cube.getAsJsonArray("pivot")) != null && cubePivot.size() >= 3) {
                                pivotX = getFloat(cubePivot, 0);
                                pivotY = getFloat(cubePivot, 1);
                                pivotZ = getFloat(cubePivot, 2);
                            }
                            float rotX = 0.0f;
                            float rotY = 0.0f;
                            float rotZ = 0.0f;
                            if (cube.has("rotation") && (cubeRot = cube.getAsJsonArray("rotation")) != null && cubeRot.size() >= 3) {
                                rotX = -getFloat(cubeRot, 0);
                                rotY = -getFloat(cubeRot, 1);
                                rotZ = getFloat(cubeRot, 2);
                            }
                            if (inflate > 0.0f) {
                                ox -= inflate;
                                oy -= inflate;
                                oz -= inflate;
                                sx += inflate * 2.0f;
                                sy += inflate * 2.0f;
                                sz += inflate * 2.0f;
                            }
                            float localOx = ox - px2;
                            float localOy = oy - py2;
                            float localOz = oz - pz2;
                            float mcBoxX = toModelX(localOx + sx);
                            float mcBoxY = toModelY(localOy);
                            float mcBoxZ = toModelZ(localOz);
                            float x2 = mcBoxX + sx;
                            float y2 = mcBoxY + sy;
                            float z2 = mcBoxZ + sz;
                            float localPivotX = toModelX(pivotX - px2);
                            float localPivotY = toModelY(pivotY - py2);
                            float localPivotZ = toModelZ(pivotZ - pz2);
                            float radX = (float) Math.toRadians(rotX);
                            float radY = (float) Math.toRadians(rotY);
                            float radZ = (float) Math.toRadians(rotZ);
                            if (cube.has("uv") && cube.get("uv").isJsonObject()) {
                                JsonObject uvObj = cube.getAsJsonObject("uv");
                                Map<class_2350, BedrockItemGeoModel.BedrockFace> faces = parseFaces(uvObj);
                                if (!faces.isEmpty()) {
                                    cubesByBone.computeIfAbsent(name2, key -> {
                                        return new ArrayList();
                                    }).add(new BedrockItemGeoModel.BedrockCube(mcBoxX, mcBoxY, mcBoxZ, x2, y2, z2, faces, mirror, localPivotX, localPivotY, localPivotZ, radX, radY, radZ));
                                }
                            }
                            if (cube.has("uv") && cube.get("uv").isJsonArray() && (uv = cube.getAsJsonArray("uv")) != null && uv.size() >= 2) {
                                int u = uv.get(0).getAsInt();
                                int v = uv.get(1).getAsInt();
                                cubes.method_32101(u, v).method_32097(mcBoxX, mcBoxY, mcBoxZ, sx, sy, sz);
                            }
                        }
                    }
                }
                class_5610 parentDef = parentName != null ? definitions.get(parentName) : root;
                if (parentDef == null) {
                    parentDef = root;
                }
                float localPX = px2;
                float localPY = py2;
                float localPZ = pz2;
                if (parentName != null && (pPivot = pivotByName.get(parentName)) != null) {
                    localPX = px2 - pPivot[0];
                    localPY = py2 - pPivot[1];
                    localPZ = pz2 - pPivot[2];
                }
                float renderPX = toModelX(localPX);
                float renderPY = toModelY(localPY);
                float renderPZ = toModelZ(localPZ);
                float radX2 = (float) Math.toRadians(rx);
                float radY2 = (float) Math.toRadians(ry);
                float radZ2 = (float) Math.toRadians(rz);
                definitions.put(name2, parentDef.method_32117(name2, cubes, class_5603.method_32091(renderPX, renderPY, renderPZ, radX2, radY2, radZ2)));
            }
            class_630 rootPart = class_5607.method_32110(mesh, texW, texH).method_32109();
            Map<String, class_630> parts = new HashMap<>();
            collectParts(rootPart, parts);
            return new BedrockItemGeoModel(rootPart, parts, texW, texH, parentByBone, cubesByBone);
        } catch (Exception e) {
            return null;
        }
    }

    private static float getFloat(JsonArray array, int index) {
        if (array == null || index < 0 || index >= array.size()) {
            return 0.0f;
        }
        return array.get(index).getAsFloat();
    }

    private static float toModelX(float x) {
        return -x;
    }

    private static float toModelY(float y) {
        return y;
    }

    private static float toModelZ(float z) {
        return z;
    }

    private static void collectParts(class_630 part, Map<String, class_630> out) {
        if (part != null && (part instanceof ModelPartAccessor)) {
            ModelPartAccessor accessor = (ModelPartAccessor) part;
            Map<String, class_630> children = accessor.punchy$getChildren();
            if (children != null) {
                for (Map.Entry<String, class_630> entry : children.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        out.put(entry.getKey(), entry.getValue());
                        collectParts(entry.getValue(), out);
                    }
                }
            }
        }
    }

    private static Map<class_2350, BedrockItemGeoModel.BedrockFace> parseFaces(JsonObject uvObj) {
        Map<class_2350, BedrockItemGeoModel.BedrockFace> faces = new HashMap<>();
        if (uvObj == null) {
            return faces;
        }
        for (Map.Entry<String, JsonElement> entry : uvObj.entrySet()) {
            class_2350 dir = parseDirection(entry.getKey());
            if (dir != null && entry.getValue().isJsonObject()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                if (face.has("uv") && face.has("uv_size")) {
                    JsonArray uv = face.getAsJsonArray("uv");
                    JsonArray size = face.getAsJsonArray("uv_size");
                    if (uv != null && size != null && uv.size() >= 2 && size.size() >= 2) {
                        float u = uv.get(0).getAsFloat();
                        float v = uv.get(1).getAsFloat();
                        float uSize = size.get(0).getAsFloat();
                        float vSize = size.get(1).getAsFloat();
                        int rotation = 0;
                        if (face.has("uv_rotation")) {
                            rotation = face.get("uv_rotation").getAsInt();
                        } else if (face.has("rotation")) {
                            rotation = face.get("rotation").getAsInt();
                        }
                        faces.put(dir, new BedrockItemGeoModel.BedrockFace(u, v, uSize, vSize, rotation));
                    }
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
}
