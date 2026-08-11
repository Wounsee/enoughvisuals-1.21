package fun.enoughvisuals.utils.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NowPlayingServiceTest {
    @Test
    void progressUsesSecondBasedMediaTimelineWithSubSecondInterpolation() {
        long sampledAt = 10_000L;
        NowPlayingService.TrackInfo track = new NowPlayingService.TrackInfo(
                true, "Track", "Artist", true,
                30L, 120L, sampledAt, false, null);

        assertEquals(0.2625f, track.progressAt(sampledAt + 1_500L), 0.0001f);
    }

    @Test
    void pausedTimelineDoesNotAdvance() {
        NowPlayingService.TrackInfo track = new NowPlayingService.TrackInfo(
                true, "Track", "Artist", false,
                30L, 120L, 10_000L, false, null);

        assertEquals(0.25f, track.progressAt(99_000L), 0.0001f);
    }
}
