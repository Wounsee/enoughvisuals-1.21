package fun.enoughvisuals.systems.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudSpeedMeterTest {
    @Test
    void stopsOnFirstStationarySampleEvenAfterFastFlight() {
        var meter = new HudSpeedMeter();
        assertEquals(0f, meter.sample(0, 0, 0));
        assertEquals(1000f, meter.sample(100, 0, 100_000_000L));
        assertEquals(0f, meter.sample(100, 0, 200_000_000L));
    }

    @Test
    void renderFrequencyDoesNotChangeSpeed() {
        var slow = new HudSpeedMeter();
        var fast = new HudSpeedMeter();
        slow.sample(0, 0, 0);
        fast.sample(0, 0, 0);
        for (int i = 1; i < 100; i++) fast.sample(i * 0.01, 0, i * 1_000_000L);
        assertEquals(slow.sample(1, 0, 100_000_000L), fast.sample(1, 0, 100_000_000L));
        fast.reset();
        assertEquals(0f, fast.sample(90000, -90000, 200_000_000L));
    }
}
