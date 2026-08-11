package fun.enoughvisuals.systems.emotions.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A small weighted OBJ embedded in a Pulse emotion's {@code props} array. */
@Environment(EnvType.CLIENT)
final class BobjPropMesh {
    private static final float MINECRAFT_MODEL_FLOOR_Y = 1.5f;

    private final float[] positions;
    private final float[] uvs;
    private final float[] normals;
    private final float[] weights;
    private final String[] bones;

    private BobjPropMesh(float[] positions, float[] uvs, float[] normals, float[] weights, String[] bones) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.weights = weights;
        this.bones = bones;
    }

    static BobjPropMesh parse(String source) {
        List<SourceVertex> sourceVertices = new ArrayList<>();
        List<Vector2f> sourceUvs = new ArrayList<>();
        List<Vector3f> sourceNormals = new ArrayList<>();
        List<FaceVertex> triangles = new ArrayList<>();
        SourceVertex current = null;

        String[] lines = source.split("\\R", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] tokens = line.split("\\s+");
            try {
                switch (tokens[0]) {
                    case "v" -> {
                        require(tokens.length >= 4, "v requires xyz");
                        finishWeights(current);
                        current = new SourceVertex(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3]));
                        sourceVertices.add(current);
                    }
                    case "vw" -> {
                        require(current != null, "vw appears before v");
                        require(tokens.length >= 3, "vw requires bone and weight");
                        float weight = Float.parseFloat(tokens[2]);
                        if (weight != 0.0f) current.weights.add(new NamedWeight(tokens[1], weight));
                    }
                    case "vt" -> {
                        require(tokens.length >= 3, "vt requires uv");
                        sourceUvs.add(new Vector2f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])));
                    }
                    case "vn" -> {
                        require(tokens.length >= 4, "vn requires xyz");
                        sourceNormals.add(new Vector3f(
                                Float.parseFloat(tokens[1]),
                                Float.parseFloat(tokens[2]),
                                Float.parseFloat(tokens[3])));
                    }
                    case "f" -> {
                        require(tokens.length >= 4, "f requires at least three vertices");
                        FaceVertex first = parseFaceVertex(tokens[1], sourceVertices.size(), sourceUvs.size(), sourceNormals.size());
                        FaceVertex previous = parseFaceVertex(tokens[2], sourceVertices.size(), sourceUvs.size(), sourceNormals.size());
                        for (int i = 3; i < tokens.length; i++) {
                            FaceVertex next = parseFaceVertex(tokens[i], sourceVertices.size(), sourceUvs.size(), sourceNormals.size());
                            triangles.add(first);
                            triangles.add(previous);
                            triangles.add(next);
                            previous = next;
                        }
                    }
                    default -> throw new IllegalArgumentException("Unsupported prop OBJ directive: " + tokens[0]);
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(
                        "Invalid Pulse prop OBJ at line " + (lineIndex + 1) + ": " + line, exception);
            }
        }
        finishWeights(current);
        require(!triangles.isEmpty(), "Pulse prop OBJ has no faces");

        int vertexCount = triangles.size();
        float[] positions = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];
        float[] normals = new float[vertexCount * 3];
        float[] weights = new float[vertexCount * 4];
        String[] bones = new String[vertexCount * 4];

        for (int i = 0; i < vertexCount; i++) {
            FaceVertex face = triangles.get(i);
            SourceVertex sourceVertex = sourceVertices.get(face.position);
            int positionOffset = i * 3;
            positions[positionOffset] = sourceVertex.x;
            positions[positionOffset + 1] = sourceVertex.y;
            positions[positionOffset + 2] = sourceVertex.z;

            if (face.uv >= 0) {
                Vector2f uv = sourceUvs.get(face.uv);
                uvs[i * 2] = uv.x;
                uvs[i * 2 + 1] = 1.0f - uv.y;
            }
            if (face.normal >= 0) {
                Vector3f normal = sourceNormals.get(face.normal);
                normals[positionOffset] = normal.x;
                normals[positionOffset + 1] = normal.y;
                normals[positionOffset + 2] = normal.z;
            } else {
                normals[positionOffset + 2] = 1.0f;
            }

            for (int slot = 0; slot < Math.min(4, sourceVertex.weights.size()); slot++) {
                NamedWeight weight = sourceVertex.weights.get(slot);
                weights[i * 4 + slot] = weight.factor;
                bones[i * 4 + slot] = weight.bone;
            }
        }

        return new BobjPropMesh(positions, uvs, normals, weights, bones);
    }

    void render(
            BobjRig.Pose pose,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            float alpha
    ) {
        Map<String, Matrix3f> normalMatrices = new HashMap<>();
        MatrixStack.Entry entry = matrices.peek();
        Vector4f transformedPosition = new Vector4f();
        Vector3f transformedNormal = new Vector3f();
        Vector3f finalNormal = new Vector3f();
        int alphaByte = Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f);

        for (int vertexIndex = 0; vertexIndex < positions.length / 3; vertexIndex++) {
            int positionOffset = vertexIndex * 3;
            int weightOffset = vertexIndex * 4;
            float sumX = 0.0f;
            float sumY = 0.0f;
            float sumZ = 0.0f;
            float sumW = 0.0f;
            float normalX = 0.0f;
            float normalY = 0.0f;
            float normalZ = 0.0f;

            for (int slot = 0; slot < 4; slot++) {
                float weight = weights[weightOffset + slot];
                String boneName = bones[weightOffset + slot];
                if (weight <= 0.0f || boneName == null) continue;
                Matrix4f skinning = pose.skinningMatrixView(boneName);
                if (skinning == null) continue;

                transformedPosition.set(
                        positions[positionOffset],
                        positions[positionOffset + 1],
                        positions[positionOffset + 2],
                        1.0f);
                skinning.transform(transformedPosition);
                sumX += transformedPosition.x * weight;
                sumY += transformedPosition.y * weight;
                sumZ += transformedPosition.z * weight;
                sumW += transformedPosition.w * weight;

                transformedNormal.set(
                        normals[positionOffset],
                        normals[positionOffset + 1],
                        normals[positionOffset + 2]);
                normalMatrices.computeIfAbsent(boneName, ignored -> new Matrix3f(skinning)).transform(transformedNormal);
                normalX += transformedNormal.x * weight;
                normalY += transformedNormal.y * weight;
                normalZ += transformedNormal.z * weight;
            }

            if (sumW <= 1.0e-7f) {
                sumX = positions[positionOffset];
                sumY = positions[positionOffset + 1];
                sumZ = positions[positionOffset + 2];
                normalX = normals[positionOffset];
                normalY = normals[positionOffset + 1];
                normalZ = normals[positionOffset + 2];
            } else {
                sumX /= sumW;
                sumY /= sumW;
                sumZ /= sumW;
            }

            finalNormal.set(normalX, -normalY, -normalZ);
            if (finalNormal.lengthSquared() > 1.0e-10f) finalNormal.normalize();
            else finalNormal.set(0.0f, 0.0f, -1.0f);

            int uvOffset = vertexIndex * 2;
            emitVertex(vertices, entry, sumX, sumY, sumZ,
                    uvs[uvOffset], uvs[uvOffset + 1], finalNormal,
                    light, overlay, alphaByte);
            // Entity cutout/translucent layers are QUADS; terminate every OBJ triangle as a
            // degenerate quad instead of letting BufferBuilder connect it to the next face.
            if (vertexIndex % 3 == 2) {
                emitVertex(vertices, entry, sumX, sumY, sumZ,
                        uvs[uvOffset], uvs[uvOffset + 1], finalNormal,
                        light, overlay, alphaByte);
            }
        }
    }

    private static void emitVertex(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float x,
            float y,
            float z,
            float u,
            float v,
            Vector3f normal,
            int light,
            int overlay,
            int alphaByte
    ) {
        vertices.vertex(entry, x, MINECRAFT_MODEL_FLOOR_Y - y, -z)
                .color(255, 255, 255, alphaByte)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, normal.x, normal.y, normal.z);
    }

    int triangleCount() {
        return positions.length / 9;
    }

    private static FaceVertex parseFaceVertex(String token, int positions, int uvs, int normals) {
        String[] parts = token.split("/", -1);
        int position = resolveIndex(parts[0], positions);
        int uv = parts.length > 1 && !parts[1].isEmpty() ? resolveIndex(parts[1], uvs) : -1;
        int normal = parts.length > 2 && !parts[2].isEmpty() ? resolveIndex(parts[2], normals) : -1;
        return new FaceVertex(position, uv, normal);
    }

    private static int resolveIndex(String token, int size) {
        int index = Integer.parseInt(token);
        int resolved = index > 0 ? index - 1 : size + index;
        if (resolved < 0 || resolved >= size) throw new IllegalArgumentException("OBJ index out of range: " + index);
        return resolved;
    }

    private static void finishWeights(SourceVertex vertex) {
        if (vertex == null || vertex.weightsFinished) return;
        vertex.weights.removeIf(weight -> weight.factor < 0.01f);
        float total = 0.0f;
        for (NamedWeight weight : vertex.weights) total += weight.factor;
        if (total > 0.0f && total != 1.0f) {
            for (NamedWeight weight : vertex.weights) weight.factor /= total;
        }
        vertex.weightsFinished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private record FaceVertex(int position, int uv, int normal) {
    }

    private static final class SourceVertex {
        final float x;
        final float y;
        final float z;
        final List<NamedWeight> weights = new ArrayList<>();
        boolean weightsFinished;

        SourceVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class NamedWeight {
        final String bone;
        float factor;

        NamedWeight(String bone, float factor) {
            this.bone = bone;
            this.factor = factor;
        }
    }
}
