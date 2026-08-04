package punchy.compat;

import net.minecraft.class_1657;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/CrawlCompat.class */
public final class CrawlCompat {
    private static final String MOD_ID = "crawl";
    private static boolean checked;
    private static boolean available;

    private CrawlCompat() {
    }

    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            available = Services.PLATFORM != null && Services.PLATFORM.isModLoaded(MOD_ID);
        }
        return available;
    }

    public static boolean isCrawling(class_1657 player) {
        if (player == null) {
            return false;
        }
        String pose = player.method_18376().name();
        if ("CRAWLING".equals(pose)) {
            return true;
        }
        return (!"SWIMMING".equals(pose) || player.method_5799() || player.method_5869()) ? false : true;
    }
}
