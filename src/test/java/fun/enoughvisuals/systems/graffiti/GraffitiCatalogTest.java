package fun.enoughvisuals.systems.graffiti;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraffitiCatalogTest {
    @Test
    void importedCatalogContainsStaticAndAnimatedAssets() {
        assertEquals(36, GraffitiCatalog.entries().size());
        assertTrue(GraffitiCatalog.entries().stream().anyMatch(entry -> entry.frames() == 60));
        assertTrue(GraffitiCatalog.entries().stream().anyMatch(entry -> entry.frames() == 45));
        for (GraffitiCatalog.Entry entry : GraffitiCatalog.entries()) {
            assertNotNull(GraffitiCatalog.find(entry.id()));
            assertTrue(entry.columns() * entry.rows() >= entry.frames());
        }
    }

    @Test
    void animatedFrameSelectionLoopsFromServerTimestamp() {
        GraffitiCatalog.Entry animated = GraffitiCatalog.entries().stream()
                .filter(entry -> entry.frames() == 60)
                .findFirst().orElseThrow();
        assertEquals(0, animated.frame(0));
        assertEquals(0, animated.frame(3_000));
        assertEquals(1, animated.frame(3_050));
    }

    @Test
    void surfaceBasisIsOrthonormalAndNeverMirrored() {
        for (Direction direction : Direction.values()) {
            for (float rotation : new float[]{-179f, -45f, 0f, 81f, 179f}) {
                GraffitiManager.Basis basis = GraffitiManager.basis(direction, rotation);
                assertEquals(1.0, basis.right().length(), 1.0e-7);
                assertEquals(1.0, basis.up().length(), 1.0e-7);
                assertEquals(0.0, basis.right().dotProduct(basis.up()), 1.0e-7);
                Vec3d windingNormal = basis.right().crossProduct(basis.up());
                assertEquals(1.0, windingNormal.dotProduct(basis.normal()), 1.0e-7,
                        "mirrored basis for " + direction + " @ " + rotation);
            }
        }
    }

    @Test
    void overlapDetectionRejectsIntersectingDecalsButAllowsNeighboursAndOtherFaces() {
        Vec3d origin = new Vec3d(10.0, 64.0, 4.0);
        assertTrue(GraffitiManager.overlaps(origin, Direction.NORTH, 0f,
                origin.add(0.5, 0.0, 0.0), Direction.NORTH, 0f));
        assertFalse(GraffitiManager.overlaps(origin, Direction.NORTH, 0f,
                origin.add(1.6, 0.0, 0.0), Direction.NORTH, 0f));
        assertFalse(GraffitiManager.overlaps(origin, Direction.NORTH, 0f,
                origin, Direction.EAST, 0f));
        assertTrue(GraffitiManager.overlaps(origin, Direction.UP, 45f,
                origin.add(0.35, 0.0, 0.35), Direction.UP, -30f));
    }
}
