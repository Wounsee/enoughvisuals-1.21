package fun.enoughvisuals.systems.emotions.render;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BobjRigLoaderTest {
    @Test
    void parsesFullWideAndSlimPlayerRigs() throws Exception {
        BobjRig wide = load("assets/enough/emotions/rig/default.bobj");
        BobjRig slim = load("assets/enough/emotions/rig/slim.bobj");

        assertEquals(20, wide.boneCount());
        assertEquals(20, slim.boneCount());
        assertEquals(5016, wide.triangleCount());
        assertEquals(4760, slim.triangleCount());
        assertTrue(wide.boneNames().contains("low_left_arm"));
        assertTrue(wide.boneNames().contains("low_leg_right"));

        assertEquals(2508, wide.triangleCount(BobjRig.SkinLayer.BASE));
        assertEquals(12, wide.triangleCount(BobjRig.SkinLayer.HAT));
        assertEquals(704, wide.triangleCount(BobjRig.SkinLayer.JACKET));
        assertEquals(448, wide.triangleCount(BobjRig.SkinLayer.RIGHT_SLEEVE));
        assertEquals(448, wide.triangleCount(BobjRig.SkinLayer.LEFT_SLEEVE));
        assertEquals(448, wide.triangleCount(BobjRig.SkinLayer.RIGHT_PANTS));
        assertEquals(448, wide.triangleCount(BobjRig.SkinLayer.LEFT_PANTS));

        assertEquals(2380, slim.triangleCount(BobjRig.SkinLayer.BASE));
        assertEquals(12, slim.triangleCount(BobjRig.SkinLayer.HAT));
        assertEquals(704, slim.triangleCount(BobjRig.SkinLayer.JACKET));
        assertEquals(384, slim.triangleCount(BobjRig.SkinLayer.RIGHT_SLEEVE));
        assertEquals(384, slim.triangleCount(BobjRig.SkinLayer.LEFT_SLEEVE));
        assertEquals(448, slim.triangleCount(BobjRig.SkinLayer.RIGHT_PANTS));
        assertEquals(448, slim.triangleCount(BobjRig.SkinLayer.LEFT_PANTS));
    }

    @Test
    void baseLayerStaysVisibleWhenEveryOptionalPlayerPartIsDisabled() {
        BobjRig.SkinLayerVisibility none =
                new BobjRig.SkinLayerVisibility(false, false, false, false, false, false);

        assertTrue(none.isVisible(BobjRig.SkinLayer.BASE));
        assertFalse(none.isVisible(BobjRig.SkinLayer.HAT));
        assertFalse(none.isVisible(BobjRig.SkinLayer.JACKET));
        assertFalse(none.isVisible(BobjRig.SkinLayer.RIGHT_SLEEVE));
        assertFalse(none.isVisible(BobjRig.SkinLayer.LEFT_SLEEVE));
        assertFalse(none.isVisible(BobjRig.SkinLayer.RIGHT_PANTS));
        assertFalse(none.isVisible(BobjRig.SkinLayer.LEFT_PANTS));
    }

    private static BobjRig load(String path) throws Exception {
        try (InputStream stream = BobjRigLoaderTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return BobjRigLoader.load(stream);
        }
    }
}
