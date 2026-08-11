package fun.enoughvisuals.systems.emotions.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parser for the armature and weighted triangle subset of McHorse BOBJ. */
@Environment(EnvType.CLIENT)
public final class BobjRigLoader {
    private BobjRigLoader() {
    }

    public static BobjRig load(InputStream stream) throws IOException {
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public static BobjRig load(Reader source) throws IOException {
        List<ProtoBone> protoBones = new ArrayList<>();
        List<SourceVertex> positions = new ArrayList<>();
        List<Vector2f> textureCoordinates = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<FaceVertex> triangleVertices = new ArrayList<>();
        SourceVertex currentVertex = null;

        try (BufferedReader reader = source instanceof BufferedReader buffered ? buffered : new BufferedReader(source)) {
            String rawLine;
            int lineNumber = 0;
            while ((rawLine = reader.readLine()) != null) {
                lineNumber++;
                String line = rawLine.stripTrailing();
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;

                try {
                    if (line.startsWith("arm_bone ")) {
                        // Deliberately split one whitespace character at a time. BOBJ encodes a root
                        // bone with an empty parent token: "arm_bone anchor  0 ...".
                        String[] tokens = line.split("\\s", -1);
                        require(tokens.length >= 22, "arm_bone requires name, parent, tail and matrix");
                        int index = protoBones.size();
                        String name = tokens[1];
                        String parent = tokens[2];
                        float[] values = new float[16];
                        for (int i = 0; i < 16; i++) values[i] = Float.parseFloat(tokens[6 + i]);
                        Matrix4f bind = new Matrix4f().set(values).transpose();
                        protoBones.add(new ProtoBone(index, name, parent, bind));
                        continue;
                    }

                    String stripped = line.strip();
                    String[] tokens = stripped.split("\\s+");
                    switch (tokens[0]) {
                        case "v" -> {
                            require(tokens.length >= 4, "v requires xyz");
                            finishWeights(currentVertex);
                            currentVertex = new SourceVertex(
                                    Float.parseFloat(tokens[1]),
                                    Float.parseFloat(tokens[2]),
                                    Float.parseFloat(tokens[3]));
                            positions.add(currentVertex);
                        }
                        case "vw" -> {
                            require(currentVertex != null, "vw appears before v");
                            require(tokens.length >= 3, "vw requires bone and weight");
                            float weight = Float.parseFloat(tokens[2]);
                            if (weight != 0.0f) currentVertex.weights.add(new NamedWeight(tokens[1], weight));
                        }
                        case "vt" -> {
                            require(tokens.length >= 3, "vt requires uv");
                            textureCoordinates.add(new Vector2f(
                                    Float.parseFloat(tokens[1]),
                                    Float.parseFloat(tokens[2])));
                        }
                        case "vn" -> {
                            require(tokens.length >= 4, "vn requires xyz");
                            normals.add(new Vector3f(
                                    Float.parseFloat(tokens[1]),
                                    Float.parseFloat(tokens[2]),
                                    Float.parseFloat(tokens[3])));
                        }
                        case "f" -> {
                            require(tokens.length >= 4, "f requires at least three vertices");
                            FaceVertex first = parseFaceVertex(tokens[1], positions.size(), textureCoordinates.size(), normals.size());
                            FaceVertex previous = parseFaceVertex(tokens[2], positions.size(), textureCoordinates.size(), normals.size());
                            for (int i = 3; i < tokens.length; i++) {
                                FaceVertex next = parseFaceVertex(tokens[i], positions.size(), textureCoordinates.size(), normals.size());
                                triangleVertices.add(first);
                                triangleVertices.add(previous);
                                triangleVertices.add(next);
                                previous = next;
                            }
                        }
                        // Model/armature declarations and embedded actions are not needed by this
                        // static player rig. Animation is loaded from Pulse's separate model.json.
                        case "arm_name", "arm_action", "o", "o_arm", "an", "ao", "ag", "kf" -> {
                        }
                        default -> throw new IllegalArgumentException("Unsupported BOBJ directive: " + tokens[0]);
                    }
                } catch (RuntimeException exception) {
                    throw new IOException("Invalid BOBJ rig at line " + lineNumber + ": " + line, exception);
                }
            }
        }
        finishWeights(currentVertex);

        require(!protoBones.isEmpty(), "BOBJ rig has no armature");
        require(!positions.isEmpty(), "BOBJ rig has no vertices");
        require(!triangleVertices.isEmpty(), "BOBJ rig has no faces");
        return compile(protoBones, positions, textureCoordinates, normals, triangleVertices);
    }

    private static BobjRig compile(
            List<ProtoBone> protoBones,
            List<SourceVertex> positions,
            List<Vector2f> textureCoordinates,
            List<Vector3f> normals,
            List<FaceVertex> triangleVertices
    ) throws IOException {
        Map<String, ProtoBone> bonesByName = new LinkedHashMap<>();
        for (ProtoBone bone : protoBones) {
            if (bonesByName.put(bone.name, bone) != null) {
                throw new IOException("Duplicate BOBJ bone: " + bone.name);
            }
        }

        List<BobjRig.Bone> bones = new ArrayList<>(protoBones.size());
        for (ProtoBone bone : protoBones) {
            int parentIndex = -1;
            Matrix4f relativeBind = new Matrix4f(bone.bind);
            if (!bone.parent.isEmpty()) {
                ProtoBone parent = bonesByName.get(bone.parent);
                if (parent == null) throw new IOException("Missing parent bone '" + bone.parent + "' for " + bone.name);
                parentIndex = parent.index;
                relativeBind.set(parent.bind).invert().mul(bone.bind);
            }
            bones.add(new BobjRig.Bone(
                    bone.index,
                    bone.name,
                    parentIndex,
                    new Matrix4f(bone.bind).invert(),
                    relativeBind));
        }
        bones.sort(Comparator.comparingInt(bone -> bone.index));

        Map<String, Integer> resolvedBoneIndices = new HashMap<>();
        for (BobjRig.Bone bone : bones) resolvedBoneIndices.put(bone.name, bone.index);

        int vertexCount = triangleVertices.size();
        float[] compiledPositions = new float[vertexCount * 3];
        float[] compiledUvs = new float[vertexCount * 2];
        float[] compiledNormals = new float[vertexCount * 3];
        float[] compiledWeights = new float[vertexCount * 4];
        int[] compiledBoneIndices = new int[vertexCount * 4];
        BobjRig.SkinLayer[] compiledSkinLayers = new BobjRig.SkinLayer[vertexCount / 3];
        Arrays.fill(compiledBoneIndices, -1);

        for (int i = 0; i < vertexCount; i++) {
            FaceVertex faceVertex = triangleVertices.get(i);
            SourceVertex position = positions.get(faceVertex.position);
            int positionOffset = i * 3;
            compiledPositions[positionOffset] = position.x;
            compiledPositions[positionOffset + 1] = position.y;
            compiledPositions[positionOffset + 2] = position.z;

            if (faceVertex.texture >= 0) {
                Vector2f uv = textureCoordinates.get(faceVertex.texture);
                compiledUvs[i * 2] = uv.x;
                compiledUvs[i * 2 + 1] = 1.0f - uv.y;
            }
            if (faceVertex.normal >= 0) {
                Vector3f normal = normals.get(faceVertex.normal);
                compiledNormals[positionOffset] = normal.x;
                compiledNormals[positionOffset + 1] = normal.y;
                compiledNormals[positionOffset + 2] = normal.z;
            } else {
                compiledNormals[positionOffset + 1] = 1.0f;
            }

            int influenceCount = Math.min(4, position.weights.size());
            for (int slot = 0; slot < influenceCount; slot++) {
                NamedWeight namedWeight = position.weights.get(slot);
                Integer boneIndex = resolvedBoneIndices.get(namedWeight.bone);
                if (boneIndex == null) continue;
                compiledWeights[i * 4 + slot] = namedWeight.weight;
                compiledBoneIndices[i * 4 + slot] = boneIndex;
            }
        }

        for (int triangle = 0; triangle < compiledSkinLayers.length; triangle++) {
            compiledSkinLayers[triangle] = classifySkinLayer(compiledUvs, triangle);
        }

        return new BobjRig(bones, new BobjRig.Mesh(
                compiledPositions,
                compiledUvs,
                compiledNormals,
                compiledWeights,
                compiledBoneIndices,
                compiledSkinLayers));
    }

    /**
     * The full Emoticons rig is a single BOBJ object, but its base and optional second layer use
     * Minecraft's canonical 64x64 player-skin UV islands. The triangle centroid is safely inside
     * one island even when a face touches an island border.
     */
    private static BobjRig.SkinLayer classifySkinLayer(float[] uvs, int triangle) {
        int firstVertex = triangle * 3;
        float u = 0.0f;
        float v = 0.0f;
        for (int corner = 0; corner < 3; corner++) {
            int uvOffset = (firstVertex + corner) * 2;
            u += uvs[uvOffset];
            v += uvs[uvOffset + 1];
        }
        float pixelX = u / 3.0f * 64.0f;
        float pixelY = v / 3.0f * 64.0f;

        if (pixelY < 16.0f && pixelX >= 32.0f) return BobjRig.SkinLayer.HAT;
        if (pixelY >= 32.0f && pixelY < 48.0f) {
            if (pixelX < 16.0f) return BobjRig.SkinLayer.RIGHT_PANTS;
            if (pixelX < 40.0f) return BobjRig.SkinLayer.JACKET;
            if (pixelX < 56.0f) return BobjRig.SkinLayer.RIGHT_SLEEVE;
        }
        if (pixelY >= 48.0f) {
            if (pixelX < 16.0f) return BobjRig.SkinLayer.LEFT_PANTS;
            if (pixelX >= 48.0f) return BobjRig.SkinLayer.LEFT_SLEEVE;
        }
        return BobjRig.SkinLayer.BASE;
    }

    private static FaceVertex parseFaceVertex(String token, int positions, int textures, int normals) {
        String[] indices = token.split("/", -1);
        int position = resolveObjIndex(indices[0], positions);
        int texture = indices.length > 1 && !indices[1].isEmpty() ? resolveObjIndex(indices[1], textures) : -1;
        int normal = indices.length > 2 && !indices[2].isEmpty() ? resolveObjIndex(indices[2], normals) : -1;
        return new FaceVertex(position, texture, normal);
    }

    private static int resolveObjIndex(String token, int size) {
        int objIndex = Integer.parseInt(token);
        int resolved = objIndex > 0 ? objIndex - 1 : size + objIndex;
        if (resolved < 0 || resolved >= size) throw new IllegalArgumentException("OBJ index out of range: " + objIndex);
        return resolved;
    }

    /** Mirrors BBS: discard tiny influences, then normalize the remaining source weights. */
    private static void finishWeights(SourceVertex vertex) {
        if (vertex == null || vertex.weightsFinished) return;
        vertex.weights.removeIf(weight -> weight.weight < 0.01f);
        float total = 0.0f;
        for (NamedWeight weight : vertex.weights) total += weight.weight;
        if (total > 0.0f && total != 1.0f) {
            for (NamedWeight weight : vertex.weights) weight.weight /= total;
        }
        vertex.weightsFinished = true;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private record ProtoBone(int index, String name, String parent, Matrix4f bind) {
    }

    private record FaceVertex(int position, int texture, int normal) {
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
        float weight;

        NamedWeight(String bone, float weight) {
            this.bone = bone;
            this.weight = weight;
        }
    }
}
