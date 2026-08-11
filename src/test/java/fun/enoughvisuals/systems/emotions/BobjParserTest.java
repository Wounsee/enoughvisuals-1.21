package fun.enoughvisuals.systems.emotions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BobjParserTest {
    private static final double EPSILON = 1.0e-6;

    @Test
    void samplesLinearConstantAndScaleDefaults() {
        BobjAnimation animation = BobjParser.parse("""
                an test
                ao body
                ag location 0
                kf 0 2 LINEAR
                kf 10 6 LINEAR
                ag rotation 1
                kf 0 3 CONSTANT
                kf 10 9 LINEAR
                """);

        BobjAnimation.BoneTransform pose = animation.sampleBone("body", 2.5);
        assertEquals(3.0, pose.location().x(), EPSILON);
        assertEquals(0.0, pose.location().y(), EPSILON);
        assertEquals(3.0, pose.rotation().y(), EPSILON);
        assertEquals(1.0, pose.scale().x(), EPSILON);
        assertEquals(1.0, pose.scale().y(), EPSILON);
        assertEquals(1.0, pose.scale().z(), EPSILON);
    }

    @Test
    void solvesBezierTimeHandlesInsteadOfTreatingTimeAsBezierParameter() {
        BobjAnimation animation = BobjParser.parse("""
                an test
                ao body
                ag location 0
                kf 0 0 BEZIER 0 0 0 0
                kf 1 1 BEZIER 0 1 1 1
                """);

        // x(u) = u^3, so x=0.125 means u=0.5 and y(u)=0.5. Treating
        // the frame ratio as u directly would incorrectly produce 0.04296875.
        double value = animation.track("body", BobjAnimation.Channel.LOCATION, 0)
                .orElseThrow()
                .sample(0.125);
        assertEquals(0.5, value, EPSILON);
    }

    @Test
    void duplicateKeyframeUsesLastValueAtTheSharedFrame() {
        BobjAnimation animation = BobjParser.parse("""
                an test
                ao body
                ag rotation 0
                kf 0 0 LINEAR
                kf 5 1 CONSTANT
                kf 5 8 LINEAR
                kf 10 10 LINEAR
                """);

        BobjAnimation.Track track = animation.track("body", BobjAnimation.Channel.ROTATION, 0)
                .orElseThrow();
        assertEquals(0.0, track.sample(4.999), EPSILON);
        assertEquals(8.0, track.sample(5.0), EPSILON);
        assertEquals(9.0, track.sample(7.5), EPSILON);
    }

    @Test
    void rejectsBezierWithoutHandles() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BobjParser.parse("""
                an test
                ao body
                ag location 0
                kf 0 0 BEZIER
                """));
        assertTrue(error.getMessage().contains("line 4"));
    }

    @Test
    void modelNormalizesLoopingFramesAndPreservesProps() {
        PulseEmotionModel model = PulseEmotionModel.parse(new StringReader("""
                {
                  "format": "bobj/1",
                  "name": "test",
                  "action": "emote_test",
                  "duration": 10,
                  "looping": true,
                  "props": [{"name": "prop", "obj": "v 0 0 0", "texture": "AA=="}],
                  "bobj": "an emote_test\\nao body\\nag location 0\\nkf 0 0 LINEAR\\nkf 10 10 LINEAR"
                }
                """));

        assertEquals(20.0, PulseEmotionModel.FRAMES_PER_SECOND, EPSILON);
        assertEquals(0.5, model.durationSeconds(), EPSILON);
        assertEquals(2.5, model.normalizeFrame(12.5), EPSILON);
        assertEquals(7.5, model.normalizeFrame(-2.5), EPSILON);
        assertTrue(model.props().orElseThrow().isJsonArray());
        assertEquals(2.5, model.sampleFrame(12.5).get("body").location().x(), EPSILON);
    }

    @Test
    void completeCatalogLoadsAndParses() throws IOException {
        assertEquals(13, PulseEmotionCatalog.entries().size());
        for (PulseEmotionCatalog.Entry entry : PulseEmotionCatalog.entries()) {
            PulseEmotionModel model = PulseEmotionModel.load(entry);
            assertEquals("bobj/1", model.format());
            assertTrue(model.durationFrames() > 0.0, entry.id());
            assertTrue(!model.animation().tracks().isEmpty(), entry.id());
        }
    }

    @Test
    void sixtySevenPropsAreDecodedOnceIntoTypedModels() throws IOException {
        PulseEmotionModel model = PulseEmotionModel.load(PulseEmotionCatalog.find("67").orElseThrow());

        assertEquals(2, model.propModels().size());
        PulseEmotionProp first = model.propModels().get(0);
        assertEquals("prop_67_7", first.name());
        assertTrue(first.objSource().contains("vw misc_gbone_2 1.000000"));
        assertEquals(168, first.textureByteLength());
        byte[] png = first.texturePng();
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);

        png[0] = 0;
        assertEquals((byte) 0x89, first.texturePng()[0], "texture bytes must be defensive copies");
    }
}
