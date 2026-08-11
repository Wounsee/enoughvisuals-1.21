package fun.enoughvisuals.systems.cosmetic.pulse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Measures Pulse cosmetics in the upright, player-relative space their renderer draws into.
 *
 * <p>Worn cosmetics are drawn in Minecraft's inverted entity model space, so the Z flip the Pulse
 * renderer bakes into every draw call is what makes them stand upright and cancels out here. Pets
 * are drawn straight into world space, so their flip is part of the measurement. Either way the
 * result is a Y-up box that can centre a GUI preview or stand a pet on the ground.
 */
public final class PulseModelGeometry {
    /** Axis-aligned bounds in blocks, measured with the model's idle pose. */
    public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public float width() { return maxX - minX; }
        public float height() { return maxY - minY; }
        public float depth() { return maxZ - minZ; }
        public float centerX() { return (minX + maxX) * 0.5f; }
        public float centerY() { return (minY + maxY) * 0.5f; }
        public float centerZ() { return (minZ + maxZ) * 0.5f; }
        public boolean usable() { return height() > 1.0e-4f && Math.max(width(), depth()) > 1.0e-4f; }
    }

    private static final Map<Long, Bounds> CACHE = new ConcurrentHashMap<>();
    private static final Bounds MISSING = new Bounds(0f, 0f, 0f, 0f, 0f, 0f);
    private static final int MAX_BONE_DEPTH = 64;

    private PulseModelGeometry() {}

    /** Returns the idle bounds of a cosmetic, or {@code null} when the model cannot be measured. */
    public static Bounds bounds(int id, boolean pet) {
        if (id < 0) return null;
        long key = (long) id << 1 | (pet ? 1L : 0L);
        Bounds cached = CACHE.computeIfAbsent(key, ignored -> {
            Bounds measured = measure(id, pet);
            return measured == null ? MISSING : measured;
        });
        return cached == MISSING ? null : cached;
    }

    /** Measures a raw Pulse {@code model.json}. Exposed so the transform can be verified offline. */
    public static Bounds measure(String modelJson, boolean pet) {
        if (modelJson == null || modelJson.isBlank()) return null;
        try {
            JsonElement parsed = JsonParser.parseString(modelJson);
            return parsed.isJsonObject() ? measure(parsed.getAsJsonObject(), pet) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Bounds measure(int id, boolean pet) {
        JsonObject root = readModel(id);
        return root == null ? null : measure(root, pet);
    }

    private static Bounds measure(JsonObject root, boolean pet) {
        JsonArray bones = bones(root);
        if (bones == null || bones.isEmpty()) return null;

        Matrix4f base = new Matrix4f();
        float scale = number(root, "scale", 1f);
        float yaw = number(root, "yaw", 0f);
        float pitch = number(root, "pitch", 0f);
        float roll = number(root, "roll", 0f);
        if (pet) {
            // Pets are drawn straight into world space, so the renderer's own flip stays in
            // place; it then ignores the authored offset and mirrors the model around its
            // root pivot instead.
            Vector3f pivot = rootPivot(bones);
            base.rotateZ((float) Math.PI)
                    .rotateY(radians(yaw)).rotateX(radians(pitch)).rotateZ(radians(roll)).scale(scale)
                    .translate(pivot.x, pivot.y, pivot.z)
                    .rotateY((float) Math.PI)
                    .scale(1f, -1f, 1f)
                    .translate(-pivot.x, -pivot.y, -pivot.z);
        } else {
            // Worn cosmetics are drawn in entity model space, where the renderer's flip is
            // what turns the model upright, so it cancels out of the measurement.
            base.translate(number(root, "x", 0f), number(root, "y", 0f), number(root, "z", 0f))
                    .rotateY(radians(yaw)).rotateX(radians(pitch)).rotateZ(radians(roll)).scale(scale);
        }

        Accumulator accumulator = new Accumulator();
        for (JsonObject bone : roots(bones)) walk(bone, bones, base, accumulator, 0);
        return accumulator.result();
    }

    private static void walk(JsonObject bone, JsonArray bones, Matrix4f parent, Accumulator accumulator, int depth) {
        if (depth > MAX_BONE_DEPTH) return;
        Vector3f pivot = pivot(bone);
        Vector3f rotation = rotation(bone);
        Matrix4f matrix = new Matrix4f(parent)
                .translate(pivot.x, pivot.y, pivot.z)
                .rotateZ(rotation.z).rotateY(rotation.y).rotateX(rotation.x)
                .translate(-pivot.x, -pivot.y, -pivot.z);

        JsonElement cubes = bone.get("cubes");
        if (cubes != null && cubes.isJsonArray()) {
            for (JsonElement element : cubes.getAsJsonArray()) {
                if (element.isJsonObject()) accumulate(element.getAsJsonObject(), matrix, accumulator);
            }
        }
        for (JsonObject child : children(bones, name(bone))) walk(child, bones, matrix, accumulator, depth + 1);
    }

    private static void accumulate(JsonObject cube, Matrix4f parent, Accumulator accumulator) {
        float[] origin = floats(cube, "origin", new float[]{0f, 0f, 0f});
        float[] size = floats(cube, "size", new float[]{1f, 1f, 1f});
        float[] pivot = floats(cube, "pivot", origin);
        float[] rotation = floats(cube, "rotation", new float[]{0f, 0f, 0f});
        float inflate = number(cube, "inflate", 0f);

        Matrix4f matrix = new Matrix4f(parent)
                .translate(-pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f)
                .rotateZ(radians(rotation[2])).rotateY(radians(-rotation[1])).rotateX(radians(-rotation[0]))
                .translate(pivot[0] / 16f, -pivot[1] / 16f, -pivot[2] / 16f);

        // The Pulse mesh builder mirrors X and expands the box by the inflate margin.
        float minX = -(origin[0] + size[0] + inflate) / 16f;
        float maxX = (-origin[0] + inflate) / 16f;
        float minY = (origin[1] - inflate) / 16f;
        float maxY = (origin[1] + size[1] + inflate) / 16f;
        float minZ = (origin[2] - inflate) / 16f;
        float maxZ = (origin[2] + size[2] + inflate) / 16f;

        for (int corner = 0; corner < 8; corner++) {
            Vector4f point = new Vector4f(
                    (corner & 1) == 0 ? minX : maxX,
                    (corner & 2) == 0 ? minY : maxY,
                    (corner & 4) == 0 ? minZ : maxZ,
                    1f);
            matrix.transform(point);
            accumulator.add(point.x, point.y, point.z);
        }
    }

    private static JsonObject readModel(int id) {
        String resource;
        try {
            resource = PulseCosmeticBridge.modelResource(id);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (resource == null || resource.isBlank()) return null;
        try (InputStream stream = PulseModelGeometry.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) return null;
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray bones(JsonObject root) {
        JsonObject model = object(root, "model");
        JsonElement geometries = model == null ? null : model.get("minecraft:geometry");
        if (geometries == null || !geometries.isJsonArray() || geometries.getAsJsonArray().isEmpty()) return null;
        JsonElement first = geometries.getAsJsonArray().get(0);
        if (!first.isJsonObject()) return null;
        JsonElement bones = first.getAsJsonObject().get("bones");
        return bones != null && bones.isJsonArray() ? bones.getAsJsonArray() : null;
    }

    private static List<JsonObject> roots(JsonArray bones) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : bones) {
            if (!element.isJsonObject()) continue;
            JsonObject bone = element.getAsJsonObject();
            if (!bone.has("parent")) result.add(bone);
        }
        return result;
    }

    private static List<JsonObject> children(JsonArray bones, String parent) {
        List<JsonObject> result = new ArrayList<>();
        if (parent == null) return result;
        for (JsonElement element : bones) {
            if (!element.isJsonObject()) continue;
            JsonObject bone = element.getAsJsonObject();
            JsonElement owner = bone.get("parent");
            if (owner != null && owner.isJsonPrimitive() && parent.equals(owner.getAsString())) result.add(bone);
        }
        return result;
    }

    private static Vector3f rootPivot(JsonArray bones) {
        List<JsonObject> roots = roots(bones);
        return roots.isEmpty() ? new Vector3f() : pivot(roots.get(0));
    }

    private static Vector3f pivot(JsonObject bone) {
        float[] pivot = floats(bone, "pivot", new float[]{0f, 0f, 0f});
        return new Vector3f(-pivot[0] / 16f, pivot[1] / 16f, pivot[2] / 16f);
    }

    private static Vector3f rotation(JsonObject bone) {
        float[] rotation = floats(bone, "rotation", new float[]{0f, 0f, 0f});
        return new Vector3f(radians(-rotation[0]), radians(-rotation[1]), radians(rotation[2]));
    }

    private static String name(JsonObject bone) {
        JsonElement name = bone.get("name");
        return name != null && name.isJsonPrimitive() ? name.getAsString() : null;
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonElement value = root == null ? null : root.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static float number(JsonObject root, String key, float fallback) {
        try {
            JsonElement value = root.get(key);
            return value != null && value.isJsonPrimitive() ? value.getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float[] floats(JsonObject root, String key, float[] fallback) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) return fallback;
        JsonArray array = value.getAsJsonArray();
        float[] result = fallback.clone();
        for (int index = 0; index < Math.min(result.length, array.size()); index++) {
            try {
                result[index] = array.get(index).getAsFloat();
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static final class Accumulator {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        void add(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        Bounds result() {
            if (!Float.isFinite(minX) || !Float.isFinite(maxX)) return null;
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
