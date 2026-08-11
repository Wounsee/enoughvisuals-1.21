package fun.enoughvisuals.systems.emotions.render;

import fun.enoughvisuals.systems.emotions.BobjAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable BOBJ armature and flattened triangle mesh.
 *
 * <p>The source rig uses Blender's Y-up player coordinates (feet at {@code y=0}, head top at
 * {@code y=2}, front at positive Z). Minecraft's entity model matrix expects model-space Y to
 * point down, with the feet at {@code y=1.5}, and its front face is negative Z; that conversion
 * is applied only while emitting vertices. Bone matrices exposed by {@link Pose} intentionally
 * remain in the original BOBJ coordinate system.</p>
 */
@Environment(EnvType.CLIENT)
public final class BobjRig {
    /** Vanilla's standing model is 24 pixels, or 1.5 blocks, from head pivot to feet. */
    private static final float MINECRAFT_MODEL_FLOOR_Y = 1.5f;

    private final List<Bone> bones;
    private final Map<String, Integer> boneIndices;
    private final Mesh mesh;
    private static final ThreadLocal<DeformationScratch> DEFORMATION_SCRATCH =
            ThreadLocal.withInitial(DeformationScratch::new);

    BobjRig(List<Bone> bones, Mesh mesh) {
        if (bones.isEmpty()) throw new IllegalArgumentException("BOBJ rig has no bones");
        this.bones = List.copyOf(bones);
        this.mesh = mesh;

        Map<String, Integer> indices = new LinkedHashMap<>();
        for (Bone bone : bones) {
            Integer previous = indices.put(bone.name, bone.index);
            if (previous != null) throw new IllegalArgumentException("Duplicate BOBJ bone: " + bone.name);
        }
        this.boneIndices = Collections.unmodifiableMap(indices);
    }

    public int boneCount() {
        return bones.size();
    }

    public int triangleCount() {
        return mesh.vertexCount() / 3;
    }

    int triangleCount(SkinLayer layer) {
        int count = 0;
        for (SkinLayer triangleLayer : mesh.skinLayers) {
            if (triangleLayer == layer) count++;
        }
        return count;
    }

    public List<String> boneNames() {
        return List.copyOf(boneIndices.keySet());
    }

    /** Evaluates the hierarchy and creates skinning matrices for one sampled animation pose. */
    public Pose evaluate(Map<String, BobjAnimation.BoneTransform> transforms, float blend) {
        float clampedBlend = clamp01(blend);
        Matrix4f[] world = new Matrix4f[bones.size()];
        Matrix4f[] skinning = new Matrix4f[bones.size()];
        byte[] visitState = new byte[bones.size()];

        for (int i = 0; i < bones.size(); i++) {
            evaluateBone(i, transforms, clampedBlend, world, skinning, visitState);
        }

        return new Pose(boneIndices, world, skinning);
    }

    private void evaluateBone(
            int index,
            Map<String, BobjAnimation.BoneTransform> transforms,
            float blend,
            Matrix4f[] world,
            Matrix4f[] skinning,
            byte[] visitState
    ) {
        if (visitState[index] == 2) return;
        if (visitState[index] == 1) throw new IllegalStateException("Cycle in BOBJ armature at " + bones.get(index).name);
        visitState[index] = 1;

        Bone bone = bones.get(index);
        Matrix4f local = new Matrix4f(bone.relativeBind);
        BobjAnimation.BoneTransform transform = transforms.get(bone.name);
        if (transform != null && blend > 0.0f) {
            BobjAnimation.Vec3 location = transform.location();
            BobjAnimation.Vec3 rotation = transform.rotation();
            BobjAnimation.Vec3 scale = transform.scale();

            local.translate(
                    (float) location.x() * blend,
                    (float) location.y() * blend,
                    (float) location.z() * blend);
            local.rotateZ((float) rotation.z() * blend);
            local.rotateY((float) rotation.y() * blend);
            local.rotateX((float) rotation.x() * blend);
            local.scale(
                    1.0f + ((float) scale.x() - 1.0f) * blend,
                    1.0f + ((float) scale.y() - 1.0f) * blend,
                    1.0f + ((float) scale.z() - 1.0f) * blend);
        }

        if (bone.parentIndex >= 0) {
            evaluateBone(bone.parentIndex, transforms, blend, world, skinning, visitState);
            world[index] = new Matrix4f(world[bone.parentIndex]).mul(local);
        } else {
            world[index] = local;
        }
        skinning[index] = new Matrix4f(world[index]).mul(bone.inverseBind);
        visitState[index] = 2;
    }

    /** Emits the already-evaluated pose into Minecraft's normal entity vertex path. */
    public void render(
            Pose pose,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            float alpha
    ) {
        render(pose, matrices, vertices, light, overlay, alpha, SkinLayerVisibility.ALL);
    }

    void render(
            Pose pose,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            float alpha,
            SkinLayerVisibility visibility
    ) {
        if (pose.skinning.length != bones.size()) {
            throw new IllegalArgumentException("Pose belongs to a different BOBJ rig");
        }

        int alphaByte = Math.round(clamp01(alpha) * 255.0f);
        DeformationScratch scratch = DEFORMATION_SCRATCH.get();
        deform(pose, scratch);
        MatrixStack.Entry entry = matrices.peek();

        for (int triangle = 0; triangle < mesh.skinLayers.length; triangle++) {
            if (!visibility.isVisible(mesh.skinLayers[triangle])) continue;
            int firstVertex = triangle * 3;
            for (int corner = 0; corner < 3; corner++) {
                int vertexIndex = firstVertex + corner;
                emitVertex(vertices, entry, scratch.positions, scratch.normals,
                        scratch.finalNormal, vertexIndex, light, overlay, alphaByte);
                /*
                 * Every built-in entity RenderLayer uses DrawMode.QUADS. BOBJ is triangles.
                 * Supplying three vertices made BufferBuilder group vertices from adjacent BOBJ
                 * faces into unrelated quads, which is the actual source of the long holes and
                 * cross-body triangles. Repeating the third corner creates one degenerate quad:
                 * its first generated triangle is the BOBJ face and its second is zero-area.
                 */
                if (corner == 2) {
                    emitVertex(vertices, entry, scratch.positions, scratch.normals,
                            scratch.finalNormal, vertexIndex, light, overlay, alphaByte);
                }
            }
        }
    }

    private void deform(Pose pose, DeformationScratch scratch) {
        int coordinateCount = mesh.vertexCount() * 3;
        scratch.ensureCapacity(coordinateCount, pose.skinning.length);
        Matrix3f[] normalMatrices = scratch.normalMatrices;
        for (int i = 0; i < pose.skinning.length; i++) {
            normalMatrices[i].set(pose.skinning[i]);
        }

        Vector4f transformedPosition = scratch.transformedPosition;
        Vector3f transformedNormal = scratch.transformedNormal;
        for (int vertexIndex = 0; vertexIndex < mesh.vertexCount(); vertexIndex++) {
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
                float weight = mesh.weights[weightOffset + slot];
                int boneIndex = mesh.boneIndices[weightOffset + slot];
                if (weight <= 0.0f || boneIndex < 0) continue;

                transformedPosition.set(
                        mesh.positions[positionOffset],
                        mesh.positions[positionOffset + 1],
                        mesh.positions[positionOffset + 2],
                        1.0f);
                pose.skinning[boneIndex].transform(transformedPosition);
                sumX += transformedPosition.x * weight;
                sumY += transformedPosition.y * weight;
                sumZ += transformedPosition.z * weight;
                sumW += transformedPosition.w * weight;

                transformedNormal.set(
                        mesh.normals[positionOffset],
                        mesh.normals[positionOffset + 1],
                        mesh.normals[positionOffset + 2]);
                normalMatrices[boneIndex].transform(transformedNormal);
                normalX += transformedNormal.x * weight;
                normalY += transformedNormal.y * weight;
                normalZ += transformedNormal.z * weight;
            }

            if (sumW <= 1.0e-7f) {
                sumX = mesh.positions[positionOffset];
                sumY = mesh.positions[positionOffset + 1];
                sumZ = mesh.positions[positionOffset + 2];
                normalX = mesh.normals[positionOffset];
                normalY = mesh.normals[positionOffset + 1];
                normalZ = mesh.normals[positionOffset + 2];
            } else {
                sumX /= sumW;
                sumY /= sumW;
                sumZ /= sumW;
            }

            scratch.positions[positionOffset] = sumX;
            scratch.positions[positionOffset + 1] = sumY;
            scratch.positions[positionOffset + 2] = sumZ;
            scratch.normals[positionOffset] = normalX;
            scratch.normals[positionOffset + 1] = normalY;
            scratch.normals[positionOffset + 2] = normalZ;
        }
    }

    private void emitVertex(VertexConsumer vertices, MatrixStack.Entry entry,
                            float[] positions, float[] normals, Vector3f normal, int vertexIndex,
                            int light, int overlay, int alphaByte) {
        int coordinateOffset = vertexIndex * 3;
        int textureOffset = vertexIndex * 2;
        normal.set(
                normals[coordinateOffset],
                -normals[coordinateOffset + 1],
                -normals[coordinateOffset + 2]);
        if (normal.lengthSquared() > 1.0e-10f) normal.normalize();
        else normal.set(0.0f, -1.0f, 0.0f);

        vertices.vertex(
                        entry,
                        positions[coordinateOffset],
                        MINECRAFT_MODEL_FLOOR_Y - positions[coordinateOffset + 1],
                        -positions[coordinateOffset + 2])
                .color(255, 255, 255, alphaByte)
                .texture(mesh.uvs[textureOffset], mesh.uvs[textureOffset + 1])
                .overlay(overlay)
                .light(light)
                .normal(entry, normal.x, normal.y, normal.z);
    }

    DeformedMesh deformForTest(Pose pose) {
        DeformationScratch scratch = new DeformationScratch();
        deform(pose, scratch);
        int length = mesh.vertexCount() * 3;
        return new DeformedMesh(
                Arrays.copyOf(scratch.positions, length),
                Arrays.copyOf(scratch.normals, length));
    }

    Mesh meshForTest() {
        return mesh;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    record DeformedMesh(float[] positions, float[] normals) {
    }

    private static final class DeformationScratch {
        private float[] positions = new float[0];
        private float[] normals = new float[0];
        private Matrix3f[] normalMatrices = new Matrix3f[0];
        private final Vector4f transformedPosition = new Vector4f();
        private final Vector3f transformedNormal = new Vector3f();
        private final Vector3f finalNormal = new Vector3f();

        private void ensureCapacity(int coordinateCount, int boneCount) {
            if (positions.length < coordinateCount) positions = new float[coordinateCount];
            if (normals.length < coordinateCount) normals = new float[coordinateCount];
            if (normalMatrices.length < boneCount) {
                Matrix3f[] expanded = Arrays.copyOf(normalMatrices, boneCount);
                for (int i = normalMatrices.length; i < expanded.length; i++) {
                    expanded[i] = new Matrix3f();
                }
                normalMatrices = expanded;
            }
        }
    }

    static final class Bone {
        final int index;
        final String name;
        final int parentIndex;
        final Matrix4f inverseBind;
        final Matrix4f relativeBind;

        Bone(int index, String name, int parentIndex, Matrix4f inverseBind, Matrix4f relativeBind) {
            this.index = index;
            this.name = name;
            this.parentIndex = parentIndex;
            this.inverseBind = new Matrix4f(inverseBind);
            this.relativeBind = new Matrix4f(relativeBind);
        }
    }

    static final class Mesh {
        final float[] positions;
        final float[] uvs;
        final float[] normals;
        final float[] weights;
        final int[] boneIndices;
        final SkinLayer[] skinLayers;

        Mesh(
                float[] positions,
                float[] uvs,
                float[] normals,
                float[] weights,
                int[] boneIndices,
                SkinLayer[] skinLayers
        ) {
            int vertexCount = positions.length / 3;
            if (positions.length % 9 != 0
                    || uvs.length != vertexCount * 2
                    || normals.length != vertexCount * 3
                    || weights.length != vertexCount * 4
                    || boneIndices.length != vertexCount * 4
                    || skinLayers.length != vertexCount / 3) {
                throw new IllegalArgumentException("Inconsistent BOBJ mesh arrays");
            }
            this.positions = positions;
            this.uvs = uvs;
            this.normals = normals;
            this.weights = weights;
            this.boneIndices = boneIndices;
            this.skinLayers = skinLayers;
        }

        int vertexCount() {
            return positions.length / 3;
        }
    }

    enum SkinLayer {
        BASE,
        HAT,
        JACKET,
        RIGHT_SLEEVE,
        LEFT_SLEEVE,
        RIGHT_PANTS,
        LEFT_PANTS
    }

    record SkinLayerVisibility(
            boolean hat,
            boolean jacket,
            boolean rightSleeve,
            boolean leftSleeve,
            boolean rightPants,
            boolean leftPants
    ) {
        static final SkinLayerVisibility ALL = new SkinLayerVisibility(true, true, true, true, true, true);

        boolean isVisible(SkinLayer layer) {
            return switch (layer) {
                case BASE -> true;
                case HAT -> hat;
                case JACKET -> jacket;
                case RIGHT_SLEEVE -> rightSleeve;
                case LEFT_SLEEVE -> leftSleeve;
                case RIGHT_PANTS -> rightPants;
                case LEFT_PANTS -> leftPants;
            };
        }
    }

    /** A hierarchy snapshot. Returned matrices are defensive copies. */
    public static final class Pose {
        private final Map<String, Integer> boneIndices;
        private final Matrix4f[] world;
        private final Matrix4f[] skinning;

        private Pose(Map<String, Integer> boneIndices, Matrix4f[] world, Matrix4f[] skinning) {
            this.boneIndices = boneIndices;
            this.world = world;
            this.skinning = skinning;
        }

        /** Bone-to-BOBJ-world matrix before inverse-bind multiplication. */
        public Optional<Matrix4f> boneWorldMatrix(String boneName) {
            Integer index = boneIndices.get(boneName);
            return index == null ? Optional.empty() : Optional.of(new Matrix4f(world[index]));
        }

        /** Final linear-blend-skinning matrix for the named bone. */
        public Optional<Matrix4f> skinningMatrix(String boneName) {
            Integer index = boneIndices.get(boneName);
            return index == null ? Optional.empty() : Optional.of(new Matrix4f(skinning[index]));
        }

        /** Internal zero-allocation lookup used for the tiny embedded prop meshes. */
        Matrix4f skinningMatrixView(String boneName) {
            Integer index = boneIndices.get(boneName);
            return index == null ? null : skinning[index];
        }

        Matrix4f skinningMatrixView(int boneIndex) {
            return skinning[boneIndex];
        }

        public List<String> boneNames() {
            return List.copyOf(boneIndices.keySet());
        }

        @Override
        public String toString() {
            return "BobjRig.Pose" + Arrays.toString(boneIndices.keySet().toArray());
        }
    }
}
