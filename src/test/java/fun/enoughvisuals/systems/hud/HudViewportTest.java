package fun.enoughvisuals.systems.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudViewportTest {

    @Test
    void remapsPositionWithoutChangingElementSize() {
        float oldViewport = 960f;
        float newViewport = 1280f;
        float elementSize = 220f;
        float oldPosition = 370f;

        float remapped = HudViewport.remap(oldPosition, elementSize, oldViewport, newViewport);

        assertEquals(
                oldPosition / (oldViewport - elementSize),
                remapped / (newViewport - elementSize),
                0.0001f
        );
    }

    @Test
    void preservesExactScreenEdges() {
        assertEquals(0f, HudViewport.remap(0f, 100f, 960f, 1280f), 0.0001f);
        assertEquals(1180f, HudViewport.remap(860f, 100f, 960f, 1280f), 0.0001f);
    }

    @Test
    void clampsLegacyOutOfBoundsPositions() {
        assertEquals(0f, HudViewport.normalizedPosition(-20f, 100f, 960f), 0.0001f);
        assertEquals(1f, HudViewport.normalizedPosition(1200f, 100f, 960f), 0.0001f);
    }
}
