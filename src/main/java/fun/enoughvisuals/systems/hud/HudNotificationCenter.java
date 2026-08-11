package fun.enoughvisuals.systems.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe HUD notification queue shared by modules and client tools. */
public final class HudNotificationCenter {
    public enum Type {
        INFO, WARNING, CONFIG, MODULE_ENABLED, MODULE_DISABLED
    }

    public record Notification(long id, Type type, String text, long createdAtMs, long expiresAtMs) {
        public String key() {
            return Long.toString(id);
        }
    }

    private static final int MAX_VISIBLE = 5;
    private static final long DEFAULT_DURATION = 3_800L;
    private static final AtomicLong IDS = new AtomicLong();
    private static final ArrayDeque<Notification> QUEUE = new ArrayDeque<>();

    private HudNotificationCenter() {
    }

    public static void info(String text) {
        post(Type.INFO, text);
    }

    public static void warning(String text) {
        post(Type.WARNING, text);
    }

    public static void config(String text) {
        post(Type.CONFIG, text);
    }

    public static void module(String moduleName, boolean enabled) {
        post(enabled ? Type.MODULE_ENABLED : Type.MODULE_DISABLED,
                (enabled ? "Включено " : "Выключено ") + moduleName);
    }

    public static synchronized void post(Type type, String text) {
        if (text == null || text.isBlank()) return;
        long now = System.currentTimeMillis();
        while (QUEUE.size() >= MAX_VISIBLE) QUEUE.removeFirst();
        QUEUE.addLast(new Notification(IDS.incrementAndGet(), type, text.trim(), now,
                now + DEFAULT_DURATION));
    }

    public static synchronized List<Notification> active() {
        long now = System.currentTimeMillis();
        while (!QUEUE.isEmpty() && QUEUE.peekFirst().expiresAtMs() <= now) QUEUE.removeFirst();
        return new ArrayList<>(QUEUE);
    }
}
