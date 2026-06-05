package fun.enoughvisuals.utils.system;

public final class MineStatusFeed {
    private static volatile String text;
    private static volatile long expiresAtMs;

    private MineStatusFeed() {}

    public static void push(String value, long showMs) {
        if (value == null || value.isBlank()) return;
        text = value;
        expiresAtMs = System.currentTimeMillis() + Math.max(400L, showMs);
    }

    public static String getNextMineText() {
        if (text == null) return null;
        if (System.currentTimeMillis() > expiresAtMs) {
            text = null;
            return null;
        }
        return text;
    }
}
