package fun.enoughvisuals.systems.events;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventFeedAlgorithmsTest {
    @Test
    void deduplicatesOverlappingSnapshotsByStableIdentity() {
        record Entry(String key, long updatedAt) {}
        String key = EventFeedAlgorithms.stableKey("funtime", "ANARCHY", 103, "beacon", "UPCOMING");
        List<Entry> result = EventFeedAlgorithms.deduplicate(
                List.of(new Entry(key, 10L), new Entry(key, 20L)),
                Entry::key,
                (first, second) -> second.updatedAt() >= first.updatedAt() ? second : first);
        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().updatedAt());
    }

    @Test
    void keepsDeadlineUntilSnapshotActuallyChanges() {
        long previous = 120_000L;
        assertEquals(previous, EventFeedAlgorithms.resolveDeadline(previous, 10L, 10L, 90_200L, 30));
        assertEquals(150_000L, EventFeedAlgorithms.resolveDeadline(previous, 10L, 11L, 100_000L, 50));
        assertEquals(140_000L, EventFeedAlgorithms.resolveDeadline(previous, 10L, 10L, 100_000L, 40));
    }

    @Test
    void countdownUsesAbsoluteDeadlineAndExpiresAtZero() {
        assertEquals(3, EventFeedAlgorithms.remainingSeconds(12_001L, 10_000L));
        assertEquals(1, EventFeedAlgorithms.remainingSeconds(10_001L, 10_000L));
        assertEquals(0, EventFeedAlgorithms.remainingSeconds(10_000L, 10_000L));
        assertEquals(0, EventFeedAlgorithms.remainingSeconds(9_999L, 10_000L));
    }

    @Test
    void currentServerSortsBeforeOtherAnarchies() {
        assertEquals(0, EventFeedAlgorithms.currentServerRank(
                "funtime", "ANARCHY", 103, "FUNTIME", "anarchy", 103));
        assertEquals(1, EventFeedAlgorithms.currentServerRank(
                "funtime", "ANARCHY", 103, "funtime", "anarchy", 101));
    }

    @Test
    void mapsProviderServersToJoinCommands() {
        assertEquals("an101", EventFeedAlgorithms.connectCommand(
                EventFeedService.Source.FUNTIME, "ANARCHY", 101));
        assertEquals("lite40", EventFeedAlgorithms.connectCommand(
                EventFeedService.Source.HOLYWORLD, "LITE_ANARCHY_40", 40));
        assertEquals("newlite2", EventFeedAlgorithms.connectCommand(
                EventFeedService.Source.HOLYWORLD, "LITE_NEW_ANARCHY_2", 2));
    }
}
