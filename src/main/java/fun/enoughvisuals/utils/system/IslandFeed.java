package fun.enoughvisuals.utils.system;

public final class IslandFeed {
    public record Entry(String text, long expiresAtMs) {}

    private static volatile Entry active;

    private IslandFeed() {}

    public static void push(String text, long showMs) {
        if (text == null || text.isBlank()) return;
        long ttl = Math.max(400L, showMs);
        active = new Entry(text, System.currentTimeMillis() + ttl);
    }

    public static Entry getActive() {
        Entry entry = active;
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAtMs()) {
            active = null;
            return null;
        }
        return entry;
    }
}
