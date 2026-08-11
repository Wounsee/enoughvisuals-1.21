package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudDesign;
import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.hud.HudRollingText;
import fun.enoughvisuals.systems.hud.HudTopInset;
import fun.enoughvisuals.systems.modules.modules.utils.NameProtect;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Exact implementation of Main / HUD / Watermark (Figma node 159:330). */
public final class WatermarkHud extends HudElement {
    private static final float BASE_WIDTH = 228f;
    private static final float BASE_HEIGHT = 48.5f;
    private static final float TOP_HEIGHT = 22f;
    private static final float DEFAULT_LOWER_WIDTH = 138.5f;
    private static final float DEFAULT_COORDINATE_DIVIDER = 92.5f;
    private static final float LOWER_Y = 26.5f;
    private static final float FONT_SIZE = 7f;

    public final SliderSetting scale = addSetting(HudDesign.scaleSetting());
    public final BooleanSetting hideCoordinates = addSetting(new BooleanSetting(
            "Скрывать координаты", "hide_coordinates",
            "Размывает координаты в ватермарке", false));

    private final HudRollingText fpsValue = new HudRollingText("0");
    private final HudRollingText pingValue = new HudRollingText("0");
    private final HudRollingText xValue = new HudRollingText("0");
    private final HudRollingText yValue = new HudRollingText("0");
    private final HudRollingText zValue = new HudRollingText("0");
    private final HudRollingText speedValue = new HudRollingText("0");

    private int fps;
    private int ping;
    private float speed;
    private double lastX;
    private double lastZ;
    private long lastPositionMs;
    private long lastStatsMs;
    private float coordinatesBlur;
    private float lowerWidth = DEFAULT_LOWER_WIDTH;
    private float coordinateDivider = DEFAULT_COORDINATE_DIVIDER;
    private float topInset;

    public WatermarkHud(float x, float y) {
        super(x, y, BASE_WIDTH, BASE_HEIGHT);
    }

    @Override
    protected void update(float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        updateStats(client);
        float s = HudDesign.scale(scale, 0.95f);
        setWidth(BASE_WIDTH * s);
        setHeight(BASE_HEIGHT * s);
        setX((HudElement.hudScreenWidth() - getWidth()) * 0.5f);
        setY(5f * s + stepAsideOffset(s));
        coordinatesBlur = RenderUtil.animate(coordinatesBlur, hideCoordinates.get() ? 1f : 0f,
                0.20f, RenderUtil.getFrameDelta());

        fpsValue.set(Integer.toString(fps));
        pingValue.set(Integer.toString(ping));
        speedValue.set(Integer.toString(Math.max(0, Math.round(speed))));
        if (client.player != null) {
            xValue.set(Integer.toString((int) Math.floor(client.player.getX())));
            yValue.set(Integer.toString((int) Math.floor(client.player.getY())));
            zValue.set(Integer.toString((int) Math.floor(client.player.getZ())));
        } else {
            xValue.set("0");
            yValue.set("0");
            zValue.set("0");
        }

        float frameDelta = RenderUtil.getFrameDelta();
        float targetDivider = 16.5f + coordinateTextWidth() + 6f;
        float targetTail = Math.max(46f, 23f + speedValue.width(FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("BPS", FONT_SIZE, FontWeight.MEDIUM));
        coordinateDivider = RenderUtil.animate(coordinateDivider, targetDivider, 0.18f, frameDelta);
        lowerWidth = RenderUtil.animate(lowerWidth,
                Math.min(BASE_WIDTH, targetDivider + targetTail), 0.18f, frameDelta);
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
        if (client.player == null) {
            lastPositionMs = 0L;
            speed = 0f;
            return;
        }
        if (lastPositionMs == 0L) {
            lastX = client.player.getX();
            lastZ = client.player.getZ();
            lastPositionMs = now;
            return;
        }
        long elapsed = now - lastPositionMs;
        if (elapsed >= 100L) {
            double dx = client.player.getX() - lastX;
            double dz = client.player.getZ() - lastZ;
            float measured = (float) (Math.sqrt(dx * dx + dz * dz) * 1_000d / elapsed);
            speed = RenderUtil.animate(speed, measured, 0.22f, RenderUtil.getFrameDelta());
            lastX = client.player.getX();
            lastZ = client.player.getZ();
            lastPositionMs = now;
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h,
                                 float delta, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        float s = HudDesign.scale(scale, 0.95f);

        // The top row is one capsule. The logo is an inset left tile, not a
        // separate panel: this is the most visible mismatch from the old port.
        HudDesign.drawRounderPanel(ctx, x, y, BASE_WIDTH * s, TOP_HEIGHT * s, 16f * s, alpha);
        HudDesign.drawRounderRect(ctx, x, y, 22f * s, TOP_HEIGHT * s,
                16f * s, 0f, 0f, 16f * s,
                RenderUtil.withAlpha(0xFF18181B, alpha * 0.72f));
        drawIcon(ctx, HudIcons.LOGO, x + 6.5f * s, y + 6.5f * s, 10f * s, alpha);

        drawIcon(ctx, HudIcons.USER, x + 28.5f * s, y + 7f * s, 8f * s, alpha);
        drawClippedText(ctx, userName(client), x + 38f * s, y + 6.75f * s,
                35f * s, FONT_SIZE * s, alpha);

        drawIcon(ctx, HudIcons.SERVER, x + 76f * s, y + 7.25f * s, 8f * s, alpha);
        drawClippedText(ctx, serverName(client), x + 86f * s, y + 6.75f * s,
                49f * s, FONT_SIZE * s, alpha);

        drawVerticalDivider(ctx, x + 138f * s, y + 6f * s, 10f * s, s, alpha);

        drawIcon(ctx, HudIcons.FPS, x + 145.5f * s, y + 7f * s, 8f * s, alpha);
        float fpsX = x + 155.5f * s;
        float fpsWidth = fpsValue.draw(ctx, fpsX, y + 6.75f * s,
                23f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "FPS", fpsX + fpsWidth, y + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);

        drawIcon(ctx, HudIcons.PING, x + 191f * s, y + 7f * s, 8f * s, alpha);
        float pingX = x + 201f * s;
        float pingWidth = pingValue.draw(ctx, pingX, y + 6.75f * s,
                14f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "MS", pingX + pingWidth, y + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);

        float lowerX = x + (BASE_WIDTH - lowerWidth) * 0.5f * s;
        float lowerY = y + LOWER_Y * s;
        HudDesign.drawRounderPanel(ctx, lowerX, lowerY, lowerWidth * s, TOP_HEIGHT * s,
                16f * s, alpha);
        drawIcon(ctx, HudIcons.LOCATION, lowerX + 7f * s, lowerY + 7.25f * s, 8f * s, alpha);
        drawCoordinates(ctx, lowerX + 16.5f * s, lowerY + 6.75f * s, s, alpha);
        if (coordinatesBlur > 0.01f) {
            ctx.draw();
            GlassUtil.beginFrame();
            float blurWidth = Math.max(8f, coordinateDivider - 15.5f);
            if (GlassUtil.isReady()) {
                GlassUtil.drawBlurPanel(ctx, lowerX + 15.5f * s, lowerY + 4f * s,
                        blurWidth * s, 14f * s, 5f * s, alpha * coordinatesBlur);
            } else {
                drawCoordinateFallback(ctx, lowerX + 17f * s, lowerY + 8f * s,
                        Math.max(6f, blurWidth - 3f) * s, 6f * s, alpha * coordinatesBlur);
            }
        }
        drawVerticalDivider(ctx, lowerX + coordinateDivider * s, lowerY + 6f * s, 10f * s, s, alpha);
        drawIcon(ctx, HudIcons.SPEED, lowerX + (coordinateDivider + 7.5f) * s,
                lowerY + 7.25f * s, 8f * s, alpha);
        float bpsX = lowerX + (coordinateDivider + 17f) * s;
        float bpsWidth = speedValue.draw(ctx, bpsX, lowerY + 6.75f * s,
                14f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        drawText(ctx, "BPS", bpsX + bpsWidth, lowerY + 6.75f * s, FONT_SIZE * s,
                HudDesign.ACCENT, alpha);
    }

    private void drawCoordinates(DrawContext ctx, float x, float y, float s, float alpha) {
        if (hideCoordinates.get()) {
            drawText(ctx, "x: •••  y: •••  z: •••", x, y, FONT_SIZE * s,
                    HudDesign.TEXT_DIM, alpha);
            return;
        }

        float cursor = x;
        cursor = drawText(ctx, "x:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        cursor += xValue.draw(ctx, cursor, y, 20f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        cursor += 2.5f * s;
        cursor = drawText(ctx, "y:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        cursor += yValue.draw(ctx, cursor, y, 14f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
        cursor += 2.5f * s;
        cursor = drawText(ctx, "z:", cursor, y, FONT_SIZE * s, HudDesign.ACCENT, alpha) + 1.5f * s;
        zValue.draw(ctx, cursor, y, 20f * s, FONT_SIZE * s, alpha, HudDesign.TEXT, FontWeight.MEDIUM);
    }

    private float coordinateTextWidth() {
        if (hideCoordinates.get()) {
            return CustomFontRenderer.getTextWidth(
                    "x: •••  y: •••  z: •••", FONT_SIZE, FontWeight.MEDIUM);
        }
        float labels = CustomFontRenderer.getTextWidth("x:", FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("y:", FONT_SIZE, FontWeight.MEDIUM)
                + CustomFontRenderer.getTextWidth("z:", FONT_SIZE, FontWeight.MEDIUM);
        return labels + xValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + yValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + zValue.layoutWidth(FONT_SIZE, FontWeight.MEDIUM)
                + 9.5f;
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

    private static void drawCoordinateFallback(DrawContext ctx, float x, float y,
                                               float width, float height, float alpha) {
        RenderUtil.drawRoundedRect(ctx, x - 0.7f, y, width + 1.4f, height,
                height * 0.5f, RenderUtil.withAlpha(HudDesign.TEXT_DIM, alpha * 0.16f));
        RenderUtil.drawRoundedRect(ctx, x, y - 0.6f, width, height + 1.2f,
                height * 0.5f, RenderUtil.withAlpha(HudDesign.PANEL, alpha * 0.74f));
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
