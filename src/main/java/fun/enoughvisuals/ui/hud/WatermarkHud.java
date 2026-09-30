package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudRollingText;
import fun.enoughvisuals.systems.hud.HudTopInset;
import fun.enoughvisuals.systems.hud.HudSpeedMeter;
import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import fun.enoughvisuals.systems.modules.modules.utils.StreamerMode;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.images.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Exact implementation of Main / HUD / Watermark (Figma node 159:330). */
public final class WatermarkHud extends HudElement {
    private static final float BASE_WIDTH = 228f;
    private static final float BASE_HEIGHT = 48.5f;
    private static final float TOP_HEIGHT = 22f;
    private static final float DEFAULT_LOWER_WIDTH = 138.5f;
    private static final float DEFAULT_COORDINATE_DIVIDER = 92.5f;
    private static final float LOWER_Y = 26.5f;
    private static final float FONT_SIZE = 7f;
    private static final String HIDDEN_COORDINATES = "x: •••  y: •••  z: •••";
    private static final Identifier COORDINATE_BLUR = Identifier.of(
            "enough", "textures/hud/coordinate_blur.png");

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());
    public final BooleanSetting hideCoordinates = addSetting(new BooleanSetting(
            "Скрывать координаты", "hide_coordinates",
            "Размывает координаты в ватермарке", false));

    private final HudRollingText fpsValue = new HudRollingText("0");
    private final HudRollingText pingValue = new HudRollingText("0");
    private final HudRollingText xValue = new HudRollingText("0");
    private final HudRollingText yValue = new HudRollingText("0");
    private final HudRollingText zValue = new HudRollingText("0");
    private final HudRollingText speedValue = new HudRollingText("0.0");

    private int fps;
    private int ping;
    private float speed;
    private final HudSpeedMeter speedMeter = new HudSpeedMeter();
    private Object speedPlayer;
    private float serverExtraWidth;
    private long lastStatsMs;
    private float coordinatesBlur;
    private float lowerWidth = DEFAULT_LOWER_WIDTH;
    private float coordinateDivider = DEFAULT_COORDINATE_DIVIDER;
    private float topInset;
    private float fpsExtraWidth;
    private float pingExtraWidth;
    private Boolean lastStreamerMode;

    public WatermarkHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        updateStats(client);
        syncStreamerMode();
        float s = HudDesign.scale(scale, 0.95f);
        setWidth(BASE_WIDTH * s);
        setHeight(BASE_HEIGHT * s);
        setX((HudElement.hudScreenWidth() - getWidth()) * 0.5f);
        setY(5f * s + stepAsideOffset(s));
        // Frame-rate-independent fade; reaches the new state in roughly 0.2 s.
        float hideTarget = hideCoordinates.get() ? 1f : 0f;
        float fadeStep = 1f - (float) Math.exp(-Math.max(0f, RenderUtil.getFrameDelta()) / 4.2f);
        coordinatesBlur += (hideTarget - coordinatesBlur) * fadeStep;
        if (Math.abs(coordinatesBlur - hideTarget) < 0.002f) coordinatesBlur = hideTarget;

        fpsValue.set(Integer.toString(fps));
        pingValue.set(Integer.toString(ping));
        speedValue.set(String.format(java.util.Locale.ROOT, "%.1f", Math.max(0f, speed)));
        if (client.player != null) {
            xValue.set(Integer.toString((int) Math.floor(client.player.getX())));
            yValue.set(Integer.toString((int) Math.floor(client.player.getY())));
            zValue.set(Integer.toString((int) Math.floor(client.player.getZ())));
        } else {
            xValue.set("0");
            yValue.set("0");
            zValue.set("0");
        }

        float targetDivider = 16.5f + coordinateTextWidth() + 6f;
        float targetTail = Math.max(46f, 23f + speedValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("BPS", FONT_SIZE, FontWeight.MEDIUM));
        // The text already animates its width. A second layout animation trails
        // behind it and makes large coordinates run into the speed counter.
        coordinateDivider = targetDivider;
        lowerWidth = targetDivider + targetTail;
        fpsExtraWidth = Math.max(0f, fpsValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM) - 23f);
        pingExtraWidth = Math.max(0f, pingValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM) - 14f);
        serverExtraWidth = Math.max(0f, CustomFontRenderer.getTextWidth(
                serverName(client), FONT_SIZE, FontWeight.MEDIUM) - 49f);
        setWidth(Math.max(BASE_WIDTH + serverExtraWidth + fpsExtraWidth + pingExtraWidth, lowerWidth) * s);
        setX((HudElement.hudScreenWidth() - getWidth()) * 0.5f);
    }

    /**
     * Slides the watermark under the boss bars or the player list instead of
     * sitting on top of them. The offset is capped so a long player list cannot
     * push the watermark off the playable area; past that point the two simply
     * share the strip again.
     */
    private float stepAsideOffset(float s) {
        float occupied = HudElement.screenToHud(HudTopInset.occupiedBottom());
        float target = occupied <= 0f ? 0f : Math.max(0f, occupied + 4f - 5f * s);
        float ceiling = Math.max(0f, HudElement.hudScreenHeight() * 0.45f - getHeight());
        topInset = RenderUtil.animate(topInset, Math.min(target, ceiling),
                0.20f, RenderUtil.getFrameDelta());
        return topInset;
    }

    private void updateStats(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastStatsMs >= 200L) {
            fps = Math.max(0, client.getCurrentFps());
            if (client.player != null && client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) ping = Math.max(0, entry.getLatency());
            }
            lastStatsMs = now;
        }
        if (speedPlayer != client.player) {
            speedPlayer = client.player;
            speedMeter.reset();
        }
        speed = client.player == null ? 0f : speedMeter.sample(
                client.player.getX(), client.player.getZ(), System.nanoTime());
    }

    /** Streamer Mode owns the automatic toggle; manual changes to this setting
     * never write back to the module. */
    private void syncStreamerMode() {
        boolean streamer = StreamerMode.isActive();
        if (lastStreamerMode == null || lastStreamerMode != streamer) {
            hideCoordinates.set(streamer);
            lastStreamerMode = streamer;
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        float s = HudDesign.scale(scale, 0.95f);
        float topWidth = BASE_WIDTH + serverExtraWidth + fpsExtraWidth + pingExtraWidth;
        x += (w - topWidth * s) * 0.5f;

        // The top row is one capsule. The logo is an inset left tile, not a
        // separate panel: this is the most visible mismatch from the old port.
        HudDesign.drawRounderPanel(ctx, x, y, topWidth * s,
                TOP_HEIGHT * s, 16f * s, alpha);
        HudDesign.drawRounderRect(ctx, x, y, 22f * s, TOP_HEIGHT * s,
                16f * s, 0f, 0f, 16f * s,
                RenderUtil.withAlpha(0xFF18181B, alpha * 0.72f));
        drawIcon(ctx, HudIcons.LOGO, x + 6.5f * s, y + 6.2f * s, 10f * s, alpha);

        drawIcon(ctx, HudIcons.USER, x + 28.5f * s, y + 7f * s, 8f * s, alpha);
        drawClippedText(ctx, userName(client), x + 38f * s, y + 6.75f * s,
                35f * s, FONT_SIZE * s, alpha);

        drawIcon(ctx, HudIcons.SERVER, x + 76f * s, y + 7.25f * s, 8f * s, alpha);
        drawText(ctx, serverName(client), x + 86f * s, y + 6.75f * s,
                FONT_SIZE * s, HudDesign.TEXT, alpha);

        drawVerticalDivider(ctx, x + (138f + serverExtraWidth) * s, y + 6f * s, 10f * s, s, alpha);

        drawIcon(ctx, HudIcons.FPS, x + (145.5f + serverExtraWidth) * s, y + 7f * s, 8f * s, alpha);
        float fpsX = x + (155.5f + serverExtraWidth) * s;
        float fpsWidth = fpsValue.draw(ctx, fpsX, y + 6.75f * s,
                (23f + fpsExtraWidth) * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "FPS", fpsX + fpsWidth, y + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);

        drawIcon(ctx, HudIcons.PING, x + (191f + serverExtraWidth + fpsExtraWidth) * s, y + 7f * s, 8f * s, alpha);
        float pingX = x + (201f + serverExtraWidth + fpsExtraWidth) * s;
        float pingWidth = pingValue.draw(ctx, pingX, y + 6.75f * s,
                (14f + pingExtraWidth) * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "MS", pingX + pingWidth, y + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);

        float lowerX = x + (topWidth - lowerWidth) * 0.5f * s;
        float lowerY = y + LOWER_Y * s;
        HudDesign.drawRounderPanel(ctx, lowerX, lowerY, lowerWidth * s, TOP_HEIGHT * s,
                16f * s, alpha);
        drawIcon(ctx, HudIcons.LOCATION, lowerX + 7f * s, lowerY + 7.25f * s, 8f * s, alpha);
        if (coordinatesBlur < 0.999f) {
            RenderUtil.enableScissor(lowerX + 16f * s, lowerY + 3f * s,
                    Math.max(0f, coordinateDivider - 17f) * s, 16f * s);
            try {
                drawCoordinates(ctx, lowerX + 16.5f * s, lowerY + 6.75f * s,
                        s, alpha * (1f - coordinatesBlur));
            } finally {
                RenderUtil.disableScissor();
            }
        }
        if (coordinatesBlur > 0.001f) {
            drawHiddenCoordinates(ctx, lowerX + 15.5f * s, lowerY + 3f * s,
                    Math.max(8f, coordinateDivider - 18.5f) * s, s, alpha * coordinatesBlur);
        }
        drawVerticalDivider(ctx, lowerX + coordinateDivider * s, lowerY + 6f * s, 10f * s, s, alpha);
        drawIcon(ctx, HudIcons.SPEED, lowerX + (coordinateDivider + 7.5f) * s,
                lowerY + 7.25f * s, 8f * s, alpha);
        float bpsX = lowerX + (coordinateDivider + 17f) * s;
        float bpsSuffixWidth = CustomFontRenderer.getTextWidth(
                "BPS", FONT_SIZE * s, FontWeight.MEDIUM);
        float bpsClipWidth = Math.max(0f,
                lowerX + lowerWidth * s - 6f * s - bpsSuffixWidth - bpsX);
        float bpsWidth = speedValue.draw(ctx, bpsX, lowerY + 6.75f * s,
                bpsClipWidth, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "BPS", bpsX + bpsWidth, lowerY + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);
    }

    private void drawCoordinates(DrawContext ctx, float x, float y, float s, float alpha) {
        float cursor = x;
        cursor = drawText(ctx, "x:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        cursor += xValue.draw(ctx, cursor, y, Math.max(xValue.width(FONT_SIZE * s, FontWeight.MEDIUM), xValue.layoutWidth(FONT_SIZE * s, FontWeight.MEDIUM)), FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        cursor += 2.5f * s;
        cursor = drawText(ctx, "y:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        cursor += yValue.draw(ctx, cursor, y, Math.max(yValue.width(FONT_SIZE * s, FontWeight.MEDIUM), yValue.layoutWidth(FONT_SIZE * s, FontWeight.MEDIUM)), FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        cursor += 2.5f * s;
        cursor = drawText(ctx, "z:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        zValue.draw(ctx, cursor, y, Math.max(zValue.width(FONT_SIZE * s, FontWeight.MEDIUM), zValue.layoutWidth(FONT_SIZE * s, FontWeight.MEDIUM)), FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
    }

    private float coordinateTextWidth() {
        float labels = CustomFontRenderer.getTextWidth("x:", FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("y:", FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("z:", FONT_SIZE, FontWeight.MEDIUM);
        float visibleWidth = labels + xValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + yValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + zValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + 9.5f;
        float hiddenWidth = CustomFontRenderer.getTextWidth(
                HIDDEN_COORDINATES, FONT_SIZE, FontWeight.MEDIUM);
        return visibleWidth + (hiddenWidth - visibleWidth) * coordinatesBlur;
    }

    private static void drawClippedText(DrawContext ctx, String text, float x, float y,
                                        float maxWidth, float size, float alpha) {
        if (CustomFontRenderer.getTextWidth(text, size, FontWeight.MEDIUM) <= maxWidth) {
            drawText(ctx, text, x, y, size, HudDesign.TEXT, alpha);
        } else {
            RenderUtil.drawMarqueeText(ctx, text, x, y, maxWidth, size + 2f, size,
                    RenderUtil.withAlpha(HudDesign.TEXT, alpha), FontWeight.MEDIUM, 16f);
        }
    }

    private static void drawHiddenCoordinates(DrawContext ctx, float x, float y,
                                               float width, float s, float alpha) {
        float height = 16f * s;

        // A Gaussian-blurred synthetic text layer, baked at 8x resolution.
        // One filtered image avoids the lattice from overlapping glyph copies.
        // It never samples the world or submits actual player coordinates.
        Draw.image(ctx, COORDINATE_BLUR, x + 2f * s, y + 2f * s,
                Math.max(0f, width - 4f * s), height - 4f * s, alpha * 0.85f);
    }

    private static float drawText(DrawContext ctx, String text, float x, float y,
                                  float size, int color, float alpha) {
        CustomFontRenderer.drawText(ctx, text, x, y, size,
                RenderUtil.withAlpha(color, alpha), FontWeight.MEDIUM);
        return x + CustomFontRenderer.getTextWidth(text, size, FontWeight.MEDIUM);
    }

    private static void drawIcon(DrawContext ctx, String icon, float x, float y,
                                 float size, float alpha) {
        CustomFontRenderer.drawText(ctx, icon, x, y, size,
                RenderUtil.withAlpha(HudDesign.ACCENT, alpha), FontWeight.HUD_ICONS);
    }

    private static void drawVerticalDivider(DrawContext ctx, float x, float y,
                                            float height, float s, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x, y, 1f * s, height, 0.5f * s,
                RenderUtil.withAlpha(0xFF2D2D2F, alpha));
    }

    private static String userName(MinecraftClient client) {
        String name = client.player != null
                ? client.player.getName().getString()
                : client.getSession().getUsername();
        return NameProtect.protect(name);
    }

    private static String serverName(MinecraftClient client) {
        if (client.isInSingleplayer()) return "Singleplayer";
        return client.getCurrentServerEntry() == null ? "Server" : client.getCurrentServerEntry().address;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

}
