package fun.enoughvisuals.systems.cosmetic.pulse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PulseModelGeometryTest {
    private static final Path PULSE_JAR = Path.of("libs", "pulse-cosmetics-local-1.21.4.jar");
    private static final float TOLERANCE = 0.03f;

    @Test
    void wornCosmeticsAreMeasuredUprightAroundTheirJoint() throws IOException {
        // The Bro9I mask hangs off the head joint: symmetric around the head and
        // growing upwards, which is only true once the renderer's flip is undone.
        PulseModelGeometry.Bounds mask = measure("hat", 85, false);
        assertNotNull(mask);
        assertEquals(0f, mask.minY(), TOLERANCE);
        assertEquals(0.59f, mask.height(), TOLERANCE);
        assertEquals(0f, mask.centerX(), TOLERANCE);
        assertEquals(0f, mask.centerZ(), TOLERANCE);
    }

    @Test
    void wingsAreMeasuredBehindTheWearer() throws IOException {
        PulseModelGeometry.Bounds wings = measure("wings", 82, false);
        assertNotNull(wings);
        assertEquals(2.32f, wings.width(), TOLERANCE);
        assertEquals(0.91f, wings.height(), TOLERANCE);
        assertTrue(wings.centerZ() > 0.05f, "wings should sit behind the body: " + wings.centerZ());
    }

    @Test
    void petsAreMeasuredUprightDespiteTheirMirroredRoot() throws IOException {
        // renderPet mirrors pets around their root pivot, which both flips them and
        // drags them away from the anchor point; the measured box has to follow along.
        PulseModelGeometry.Bounds bee = measure("pet", 93, true);
        assertNotNull(bee);
        assertEquals(0.47f, bee.height(), TOLERANCE);
        assertEquals(-1.47f, bee.minY(), TOLERANCE);

        PulseModelGeometry.Bounds radish = measure("pet", 47, true);
        assertNotNull(radish);
        assertEquals(0.69f, radish.height(), TOLERANCE);
        assertEquals(-0.24f, radish.minY(), TOLERANCE);
    }

    @Test
    void everyShippedPetCanBeFramed() throws IOException {
        for (int id : new int[]{45, 46, 47, 84, 93, 133, 134, 135}) {
            PulseModelGeometry.Bounds bounds = measure("pet", id, true);
            assertNotNull(bounds, "pet " + id);
            assertTrue(bounds.usable(), "pet " + id + " cannot be framed");
            assertTrue(bounds.height() > 0.2f && bounds.height() < 3f,
                    "pet " + id + " height " + bounds.height());
        }
    }

    private static PulseModelGeometry.Bounds measure(String category, int id, boolean pet) throws IOException {
        assertTrue(Files.isRegularFile(PULSE_JAR), "missing " + PULSE_JAR.toAbsolutePath());
        try (ZipFile zip = new ZipFile(PULSE_JAR.toFile())) {
            ZipEntry entry = zip.getEntry("assets/pulsecosmetics/cosmetics/" + category + "/" + id + "/model.json");
            assertNotNull(entry, category + "/" + id);
            try (InputStream stream = zip.getInputStream(entry)) {
                return PulseModelGeometry.measure(new String(stream.readAllBytes(), StandardCharsets.UTF_8), pet);
            }
        }
    }
}
