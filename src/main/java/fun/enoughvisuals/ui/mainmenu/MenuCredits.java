package fun.enoughvisuals.ui.mainmenu;

import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Year;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact two-line Figma credit block with link hit boxes matching the bright spans. */
public final class MenuCredits {
    private static final String KIDCADE = "@kidcade";
    private static final String WOUNSEE = "@wounsee";
    private static final String PROJECT = "EnoughVisuals";
    private static final String KIDCADE_URL = "https://t.me/kidcade";
    private static final String WOUNSEE_URL = "https://t.me/wounsee";
    private static final String PROJECT_URL = "https://t.me/enoughvisuals";
    private static final int WHITE = 0xFFFFFFFF;
    private static final int FALLBACK_YEAR = 2026;
    private static final Pattern ONLINE_YEAR = Pattern.compile("\\\"year\\\"\\s*:\\s*(\\d{4})");
    private static volatile int copyrightYear = resolveLocalYear();

    private MenuCredits() {
    }

    public static void render(DrawContext context, float virtualWidth, float virtualHeight,
                              float scale, float alpha, double mouseX, double mouseY) {
        String year = Integer.toString(copyrightYear);
        float size = 6f * scale;
        float firstY = virtualHeight - 29f * scale;
        float secondY = virtualHeight - 18.5f * scale;
        drawCenteredRuns(context, virtualWidth * 0.5f, firstY, size, alpha,
                Run.text("Designed by ", false),
                Run.text(KIDCADE, true),
                Run.text(", made by ", false),
                Run.text(WOUNSEE, true),
                Run.text(".", false));
        drawCenteredRuns(context, virtualWidth * 0.5f, secondY, size, alpha,
                Run.icon(SolarIcons.COPYRIGHT),
                Run.text(" " + year + " ", false),
                Run.text(PROJECT, true),
                Run.text(". All rights reserved.", false));
    }

    public static boolean click(double mouseX, double mouseY, float virtualWidth,
                                float virtualHeight, float scale) {
        float size = 6f * scale;
        float firstY = virtualHeight - 29f * scale;
        float secondY = virtualHeight - 18.5f * scale;
        String first = "Designed by " + KIDCADE + ", made by " + WOUNSEE + ".";
        float firstX = virtualWidth * 0.5f
                - CustomFontRenderer.getTextWidth(first, size, FontWeight.SEMIBOLD) * 0.5f;
        float kidcadeX = firstX + width("Designed by ", size);
        if (insideText(mouseX, mouseY, kidcadeX, firstY, KIDCADE, size)) {
            open(KIDCADE_URL);
            return true;
        }
        float wounseeX = kidcadeX + width(KIDCADE + ", made by ", size);
        if (insideText(mouseX, mouseY, wounseeX, firstY, WOUNSEE, size)) {
            open(WOUNSEE_URL);
            return true;
        }

        float secondWidth = width(SolarIcons.COPYRIGHT, size, FontWeight.SOLAR_ICONS)
                + width(" " + copyrightYear + " " + PROJECT + ". All rights reserved.", size);
        float secondX = virtualWidth * 0.5f - secondWidth * 0.5f;
        float projectX = secondX + width(SolarIcons.COPYRIGHT, size, FontWeight.SOLAR_ICONS)
                + width(" " + copyrightYear + " ", size);
        if (insideText(mouseX, mouseY, projectX, secondY, PROJECT, size)) {
            open(PROJECT_URL);
            return true;
        }
        return false;
    }

    private static void drawCenteredRuns(DrawContext context, float centerX, float y,
                                         float size, float alpha, Run... runs) {
        float total = 0f;
        for (Run run : runs) total += width(run.text, size, run.weight);
        float x = centerX - total * 0.5f;
        for (Run run : runs) {
            float runY = y;
            if (run.weight == FontWeight.SOLAR_ICONS) {
                runY += (CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD)
                        - CustomFontRenderer.getTextHeight(size, run.weight)) * 0.5f;
            }
            CustomFontRenderer.drawText(context, run.text, x, runY, size,
                    RenderUtil.withAlpha(WHITE, alpha * (run.link ? 0.50f : 0.20f)),
                    run.weight);
            x += width(run.text, size, run.weight);
        }
    }

    private static boolean insideText(double mouseX, double mouseY, float x, float y,
                                      String text, float size) {
        float width = width(text, size);
        float height = CustomFontRenderer.getTextHeight(size, FontWeight.SEMIBOLD) + 1f;
        return mouseX >= x && mouseX <= x + width && mouseY >= y - 1f && mouseY <= y + height;
    }

    private static boolean inside(double mouseX, double mouseY, float x, float y,
                                  float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static float width(String text, float size) {
        return width(text, size, FontWeight.SEMIBOLD);
    }

    private static float width(String text, float size, FontWeight weight) {
        return CustomFontRenderer.getTextWidth(text, size, weight);
    }

    private static void open(String url) {
        Util.getOperatingSystem().open(URI.create(url));
    }

    private static int resolveLocalYear() {
        try {
            int year = Year.now(ZoneId.systemDefault()).getValue();
            if (year >= 2020 && year <= 9999) return year;
        } catch (Throwable ignored) {
        }
        resolveMoscowYearOnline();
        return FALLBACK_YEAR;
    }

    private static void resolveMoscowYearOnline() {
        Thread worker = new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(
                                "https://timeapi.io/api/time/current/zone?timeZone=Europe%2FMoscow"))
                        .timeout(Duration.ofSeconds(4)).GET().build();
                String body = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
                        .send(request, HttpResponse.BodyHandlers.ofString()).body();
                Matcher matcher = ONLINE_YEAR.matcher(body);
                if (matcher.find()) {
                    int year = Integer.parseInt(matcher.group(1));
                    if (year >= 2020 && year <= 9999) copyrightYear = year;
                }
            } catch (Throwable ignored) {
            }
        }, "EnoughVisuals-CopyrightYear");
        worker.setDaemon(true);
        worker.start();
    }

    private record Run(String text, boolean link, FontWeight weight) {
        private static Run text(String text, boolean link) {
            return new Run(text, link, FontWeight.SEMIBOLD);
        }

        private static Run icon(String icon) {
            return new Run(icon, false, FontWeight.SOLAR_ICONS);
        }
    }
}
