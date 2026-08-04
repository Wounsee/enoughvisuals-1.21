package punchy.client.state;

import java.util.Locale;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3486;
import net.minecraft.class_3532;
import net.minecraft.class_6862;
import net.minecraft.class_6880;
import net.minecraft.class_7924;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/state/SweatStateMachine.class */
public final class SweatStateMachine {
    private static final int STAGE_COUNT = 4;
    private static final int STAGE_TICKS = 5;
    private static final int FADE_TICKS = 40;
    private static int ticks;
    private static final class_6862<class_1959> DESERT_BIOME_TAG = class_6862.method_40092(class_7924.field_41236, class_2960.method_60655("minecraft", "is_desert"));
    private static int stage = 0;
    private static float stageProgress = 0.0f;
    private static float fadeAlpha = 0.0f;
    private static boolean active = false;
    private static boolean fadingOut = false;
    private static boolean wetActive = false;
    private static boolean wetAnimate = false;
    private static int fadeTicks = 0;
    private static int fadeOutTicks = 0;
    private static boolean suppressInWater = false;

    public void tick(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return;
        }
        if (!PunchyConfig.enableSweat()) {
            reset();
            return;
        }
        suppressInWater = client.field_1724.method_5777(class_3486.field_15517);
        if (wetActive) {
            if (fadeTicks < FADE_TICKS) {
                fadeTicks++;
                fadeAlpha = class_3532.method_15363(fadeTicks / 40.0f, 0.0f, 1.0f);
                stage = 1;
                stageProgress = 0.0f;
                return;
            }
            fadeAlpha = 1.0f;
            if (wetAnimate) {
                ticks++;
                int frame = (ticks / STAGE_TICKS) % STAGE_COUNT;
                stage = frame + 1;
                stageProgress = (ticks % STAGE_TICKS) / 5.0f;
                return;
            }
            stage = 1;
            stageProgress = 0.0f;
            return;
        }
        boolean inDesert = isInDesert(client);
        if (!inDesert) {
            if (!active) {
                return;
            }
            if (!fadingOut) {
                fadingOut = true;
                fadeOutTicks = FADE_TICKS;
            }
            if (fadeOutTicks > 0) {
                fadeOutTicks--;
                fadeAlpha = class_3532.method_15363(fadeOutTicks / 40.0f, 0.0f, 1.0f);
                return;
            } else {
                reset();
                return;
            }
        }
        if (fadingOut) {
            fadingOut = false;
            fadeTicks = class_3532.method_15340((int) (fadeAlpha * 40.0f), 0, FADE_TICKS);
        }
        if (!active) {
            active = true;
            fadeTicks = 0;
        }
        if (fadeTicks < FADE_TICKS) {
            fadeTicks++;
            fadeAlpha = class_3532.method_15363(fadeTicks / 40.0f, 0.0f, 1.0f);
            stage = 1;
            stageProgress = 0.0f;
            return;
        }
        ticks++;
        fadeAlpha = 1.0f;
        int frame2 = (ticks / STAGE_TICKS) % STAGE_COUNT;
        stage = frame2 + 1;
        stageProgress = (ticks % STAGE_TICKS) / 5.0f;
    }

    public static int getStage() {
        return stage;
    }

    public static float getStageProgress() {
        return stageProgress;
    }

    public static float getFadeAlpha() {
        if (suppressInWater) {
            return 0.0f;
        }
        return fadeAlpha;
    }

    public static void startWet(boolean animateAfterFade) {
        if (wetActive) {
            wetAnimate = animateAfterFade;
            return;
        }
        wetActive = true;
        wetAnimate = animateAfterFade;
        active = true;
        fadingOut = false;
        fadeTicks = 0;
        fadeOutTicks = 0;
        fadeAlpha = 0.0f;
        ticks = 0;
        stage = 1;
        stageProgress = 0.0f;
    }

    public static void startWet() {
        startWet(true);
    }

    public static void stopWet() {
        if (!wetActive) {
            return;
        }
        wetActive = false;
        wetAnimate = false;
        reset();
    }

    private static void reset() {
        ticks = 0;
        stage = 0;
        stageProgress = 0.0f;
        fadeAlpha = 0.0f;
        active = false;
        fadeTicks = 0;
        fadingOut = false;
        fadeOutTicks = 0;
        wetActive = false;
        wetAnimate = false;
        suppressInWater = false;
    }

    private static boolean isInDesert(class_310 client) {
        class_1937 level = client.field_1724.method_37908();
        if (level == null) {
            return false;
        }
        class_2338 pos = client.field_1724.method_24515();
        class_6880<class_1959> holder = level.method_23753(pos);
        if (holder.method_40220(DESERT_BIOME_TAG)) {
            return true;
        }
        String biomeName = String.valueOf(holder).toLowerCase(Locale.ROOT);
        return biomeName.contains("desert");
    }
}
