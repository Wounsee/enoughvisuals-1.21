package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.Info;
import net.minecraft.client.MinecraftClient;

/** Lightweight typewriter animation for the text following the title separator. */
public final class WindowTitleAnimator {
    private static final String BASE = Info.DISPLAY_NAME + " " + Info.VERSION + " | ";
    private static final String[] PHRASES = {"t.me/enoughvisuals", "Made by wounsee"};
    private static final long TYPE_MS = 72L;
    private static final long HOLD_MS = 1900L;
    private static final long ERASE_MS = 38L;
    private static final long PAUSE_MS = 360L;
    private static final long STARTED_AT = System.currentTimeMillis();

    private static String lastApplied = "";

    private WindowTitleAnimator() {
    }

    public static String currentTitle() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - STARTED_AT);
        for (;;) {
            for (String phrase : PHRASES) {
                long typing = phrase.length() * TYPE_MS;
                long erasing = phrase.length() * ERASE_MS;
                long cycle = typing + HOLD_MS + erasing + PAUSE_MS;
                if (elapsed >= cycle) {
                    elapsed -= cycle;
                    continue;
                }
                int characters;
                if (elapsed < typing) {
                    characters = Math.min(phrase.length(), (int) (elapsed / TYPE_MS) + 1);
                } else if (elapsed < typing + HOLD_MS) {
                    characters = phrase.length();
                } else if (elapsed < typing + HOLD_MS + erasing) {
                    long eraseTime = elapsed - typing - HOLD_MS;
                    characters = Math.max(0, phrase.length() - (int) (eraseTime / ERASE_MS) - 1);
                } else {
                    characters = 0;
                }
                return BASE + phrase.substring(0, characters);
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) return;
        String title = currentTitle();
        if (title.equals(lastApplied)) return;
        lastApplied = title;
        client.updateWindowTitle();
    }
}
