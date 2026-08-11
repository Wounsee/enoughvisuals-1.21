package fun.enoughvisuals.systems.emotions.render;

import fun.enoughvisuals.systems.emotions.PulseEmotionModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deformation-regression and Minecraft buffer-contract checks for the Pulse BOBJ renderer. */
class BobjSimpleVaoParityTest {
    private static final float EPSILON = 2.0e-6f;

    @Test
    void minecraftEntityLayerConsumesQuadsRatherThanTriangles() {
        RenderLayer layer = RenderLayer.getEntityTranslucent(
                Identifier.of("enough", "textures/emotions/test.png"));
        assertEquals(VertexFormat.DrawMode.QUADS, layer.getDrawMode());
    }

    @Test
    void concreteNowkieBakePosePreservesPreTopologyDeformationAndTerminatesEveryTriangleAsAQuad() throws Exception {
        BobjRig rig = loadRig("assets/enough/emotions/rig/default.bobj");
        PulseEmotionModel emotion = loadEmotion("assets/enough/emotions/pulse/nowkie_bake/model.json");
        BobjRig.Pose pose = rig.evaluate(emotion.sampleFrame(150.0), 1.0f);

        ReferenceMesh expected = deformLikePreTopologyRenderer(rig, pose);
        BobjRig.DeformedMesh actual = rig.deformForTest(pose);

        assertArrayEquals(expected.positions, actual.positions(), EPSILON,
                "The topology fix must not change the previously working animated pose");
        assertArrayEquals(expected.normals, actual.normals(), EPSILON,
                "The topology fix must not change the previously working animated normals");
        assertFalse(allEqual(rig.meshForTest().positions, actual.positions()),
                "The selected Pulse frame must be a real animated pose");

        RecordingVertexConsumer consumer = new RecordingVertexConsumer();
        rig.render(pose, new MatrixStack(), consumer, 0, 0, 1.0f);

        assertEquals(rig.triangleCount() * 4, consumer.vertices.size(),
                "Entity RenderLayers use QUADS, so every BOBJ triangle needs four submitted vertices");
        assertDegenerateQuadPerTriangle(consumer.vertices);

        for (int triangle = 0; triangle < rig.triangleCount(); triangle++) {
            int sourceVertex = triangle * 3;
            int emittedVertex = triangle * 4;
            for (int corner = 0; corner < 3; corner++) {
                RecordedVertex submitted = consumer.vertices.get(emittedVertex + corner);
                int offset = (sourceVertex + corner) * 3;
                assertEquals(expected.positions[offset], submitted.x, EPSILON);
                assertEquals(1.5f - expected.positions[offset + 1], submitted.y, EPSILON);
                assertEquals(-expected.positions[offset + 2], submitted.z, EPSILON);
            }
        }
    }

    @Test
    void pulsePropTrianglesAlsoRespectTheEntityQuadBufferContract() throws Exception {
        BobjRig rig = loadRig("assets/enough/emotions/rig/default.bobj");
        PulseEmotionModel emotion = loadEmotion("assets/enough/emotions/pulse/67/model.json");
        assertFalse(emotion.propModels().isEmpty(), "67 is expected to exercise embedded Pulse props");

        BobjPropMesh prop = BobjPropMesh.parse(emotion.propModels().get(0).objSource());
        BobjRig.Pose pose = rig.evaluate(emotion.sampleFrame(10.0), 1.0f);
        RecordingVertexConsumer consumer = new RecordingVertexConsumer();
        prop.render(pose, new MatrixStack(), consumer, 0, 0, 1.0f);

        assertTrue(prop.triangleCount() > 0);
        assertEquals(prop.triangleCount() * 4, consumer.vertices.size());
        assertDegenerateQuadPerTriangle(consumer.vertices);
    }

    /** Independent reconstruction of the visually validated deformation path used before the
     * triangle-to-quad buffer fix. Topology and deformation are deliberately tested separately. */
    private static ReferenceMesh deformLikePreTopologyRenderer(BobjRig rig, BobjRig.Pose pose) {
        BobjRig.Mesh mesh = rig.meshForTest();
        float[] positions = new float[mesh.positions.length];
        float[] normals = new float[mesh.normals.length];
        Vector4f point = new Vector4f();
        Vector3f normal = new Vector3f();

        for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
            Vector4f sum = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
            Vector3f normalSum = new Vector3f();
            int weightedBones = 0;
            for (int slot = 0; slot < 4; slot++) {
                int weightOffset = vertex * 4 + slot;
                float weight = mesh.weights[weightOffset];
                int bone = mesh.boneIndices[weightOffset];
                if (weight <= 0.0f || bone < 0) continue;

                int coordinateOffset = vertex * 3;
                point.set(mesh.positions[coordinateOffset], mesh.positions[coordinateOffset + 1],
                        mesh.positions[coordinateOffset + 2], 1.0f);
                pose.skinningMatrixView(bone).transform(point);
                sum.add(new Vector4f(point).mul(weight));

                normal.set(mesh.normals[coordinateOffset], mesh.normals[coordinateOffset + 1],
                        mesh.normals[coordinateOffset + 2]);
                new Matrix3f(pose.skinningMatrixView(bone)).transform(normal);
                normalSum.add(new Vector3f(normal).mul(weight));
                weightedBones++;
            }

            int offset = vertex * 3;
            if (weightedBones == 0) {
                sum.set(mesh.positions[offset], mesh.positions[offset + 1], mesh.positions[offset + 2], 1.0f);
                normalSum.set(mesh.normals[offset], mesh.normals[offset + 1], mesh.normals[offset + 2]);
            }
            positions[offset] = sum.x / sum.w;
            positions[offset + 1] = sum.y / sum.w;
            positions[offset + 2] = sum.z / sum.w;
            normals[offset] = normalSum.x;
            normals[offset + 1] = normalSum.y;
            normals[offset + 2] = normalSum.z;
        }
        return new ReferenceMesh(positions, normals);
    }

    private static boolean allEqual(float[] first, float[] second) {
        return Arrays.equals(first, second);
    }

    private static void assertDegenerateQuadPerTriangle(List<RecordedVertex> vertices) {
        assertEquals(0, vertices.size() % 4);
        for (int first = 0; first < vertices.size(); first += 4) {
            assertEquals(vertices.get(first + 2), vertices.get(first + 3),
                    "The fourth vertex must duplicate the triangle's third corner at quad " + (first / 4));
        }
    }

    private static BobjRig loadRig(String path) throws Exception {
        try (InputStream stream = BobjSimpleVaoParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return BobjRigLoader.load(stream);
        }
    }

    private static PulseEmotionModel loadEmotion(String path) throws Exception {
        try (InputStream stream = BobjSimpleVaoParityTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return PulseEmotionModel.parse(reader);
            }
        }
    }

    private record ReferenceMesh(float[] positions, float[] normals) {
    }

    private record RecordedVertex(float x, float y, float z, int red, int green, int blue, int alpha,
                                  float u, float v, int overlayU, int overlayV, int lightU, int lightV,
                                  float normalX, float normalY, float normalZ) {
    }

    private static final class RecordingVertexConsumer implements VertexConsumer {
        private final List<RecordedVertex> vertices = new ArrayList<>();
        private float x;
        private float y;
        private float z;
        private int red;
        private int green;
        private int blue;
        private int alpha;
        private float u;
        private float v;
        private int overlayU;
        private int overlayV;
        private int lightU;
        private int lightV;

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.overlayU = u;
            this.overlayV = v;
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.lightU = u;
            this.lightV = v;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            vertices.add(new RecordedVertex(this.x, this.y, this.z, red, green, blue, alpha,
                    u, v, overlayU, overlayV, lightU, lightV, x, y, z));
            return this;
        }
    }
}
