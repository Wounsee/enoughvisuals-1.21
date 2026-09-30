package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.HudIcons;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public final class VanillaScreenStyle {
    private static final int BUTTON = 0xFF151515;
    private static final int BUTTON_HOVER = 0xFF1F1F1F;
    private static final int TEXT = 0xFFF7F4FC;
    private static final int TEXT_DISABLED = 0xFF817D89;
    private static Screen lastScreen;
    private static long screenOpenedAt;

    private VanillaScreenStyle() {
    }

    public static boolean shouldStyle() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen instanceof GameMenuScreen;
    }

    public static boolean shouldStyleButton(ClickableWidget widget) {
        if (shouldStyle()) return true;

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof SelectWorldScreen || screen instanceof MultiplayerScreen
                || screen instanceof CreateWorldScreen || screen instanceof AddServerScreen)) {
            return false;
        }
        if (widget.getY() < screen.height - 55) return false;
        if (!(widget.getMessage().getContent() instanceof TranslatableTextContent content)) return false;

        return switch (content.getKey()) {
            case "selectWorld.select", "selectWorld.create", "selectWorld.edit", "selectWorld.delete",
                    "selectWorld.recreate", "selectServer.select", "selectServer.add", "selectServer.direct",
                    "selectServer.edit", "selectServer.delete", "selectServer.refresh", "addServer.add",
                    "gui.back", "gui.cancel" -> true;
            default -> false;
        };
    }

    public static void drawButton(DrawContext context, ClickableWidget widget, float alpha, float hover) {
        pushButtonHover(context, widget, hover);
        try {
            drawButtonBody(context, widget, alpha, hover);
        } finally {
            context.getMatrices().pop();
        }
    }

    private static void drawButtonBody(DrawContext context, ClickableWidget widget, float alpha, float hover) {
        float x = widget.getX();
        float y = widget.getY();
        float width = widget.getWidth();
        float height = widget.getHeight();
        float opacity = alpha * (widget.active ? 1f : 0.46f);

        context.getMatrices().push();
        RenderUtil.drawRoundedRect(context, x, y + 1f, width, height, 6f,
                RenderUtil.withAlpha(0xFF000000, opacity * (0.18f + hover * 0.06f)));
        RenderUtil.drawRoundedRect(context, x, y, width, height, 6f,
                RenderUtil.withAlpha(RenderUtil.lerpColor(BUTTON, BUTTON_HOVER, hover), opacity * 0.92f));

        Icon icon = iconFor(widget.getMessage());
        float textSize = 6.45f;
        float maxTextWidth = width - (icon == null ? 13f : 31f);
        String label = CustomFontRenderer.trimToWidth(widget.getMessage().getString(), maxTextWidth,
                textSize, FontWeight.SEMIBOLD);
        float textY = y + (height - CustomFontRenderer.getTextHeight(textSize, FontWeight.SEMIBOLD)) * 0.5f - 0.15f;
        float textCenter = x + width * 0.5f;
        CustomFontRenderer.drawCenteredText(context, label, textCenter, textY, textSize,
                RenderUtil.withAlpha(widget.active ? TEXT : TEXT_DISABLED, opacity), FontWeight.SEMIBOLD);

        if (icon != null) {
            float iconSize = icon.weight == FontWeight.MENU ? 8.6f : 9f;
            float iconY = y + (height - CustomFontRenderer.getTextHeight(iconSize, icon.weight)) * 0.5f - 0.25f;
            CustomFontRenderer.drawCenteredText(context, icon.glyph, x + 10f, iconY, iconSize,
                    RenderUtil.withAlpha(RenderUtil.lerpColor(0xFFB9B3C7, TEXT, hover), opacity), icon.weight);
        }
        context.getMatrices().pop();
    }

    public static void drawSlider(DrawContext context, ClickableWidget widget, double value,
                                  float alpha, float hover) {
        pushButtonHover(context, widget, hover);
        try {
            drawButtonBody(context, widget, alpha, hover);
            float x = widget.getX() + 5f;
            float width = Math.max(1f, widget.getWidth() - 10f);
            float y = widget.getY() + widget.getHeight() - 3.7f;
            float progress = (float) Math.max(0.0, Math.min(1.0, value));
            float opacity = alpha;
            RenderUtil.drawRoundedRect(context, x, y, width, 1.45f, 0.73f,
                    RenderUtil.withAlpha(accent(), opacity * 0.18f));
            if (progress > 0.001f) {
                float fill = Math.max(1.45f, width * progress);
                RenderUtil.drawRoundedRect(context, x, y, fill, 1.45f, 0.73f,
                        RenderUtil.withAlpha(accent(), opacity * 0.96f));
            }
        } finally {
            context.getMatrices().pop();
        }
    }

    private static void pushButtonHover(DrawContext context, ClickableWidget widget, float hover) {
        float scale = 1f + hover * 0.026f;
        float centerX = widget.getX() + widget.getWidth() * 0.5f;
        float centerY = widget.getY() + widget.getHeight() * 0.5f;
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0f);
    }

    public static float animateHover(float current, boolean hovered, long elapsedNanos) {
        float dt = Math.min(0.05f, Math.max(0f, elapsedNanos / 1_000_000_000f));
        float response = 1f - (float) Math.exp(-15f * dt);
        return current + ((hovered ? 1f : 0f) - current) * response;
    }

    public static float appearance() {
        ensureScreenState();
        long now = System.nanoTime();
        float progress = clamp((now - screenOpenedAt) / 430_000_000f);
        return RenderUtil.easeOutCubic(progress);
    }

    private static void ensureScreenState() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == lastScreen) return;
        lastScreen = screen;
        screenOpenedAt = System.nanoTime();
    }

    private static float clamp(float value) {
        return Math.min(1f, Math.max(0f, value));
    }

    private static int accent() {
        try {
            return CustomTheme.guiAccent();
        } catch (Throwable ignored) {
            return 0xFF7F94DD;
        }
    }

    private static Icon iconFor(Text message) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof GameMenuScreen)) return null;
        if (!(message.getContent() instanceof TranslatableTextContent content)) return null;
        return switch (content.getKey()) {
            case "menu.returnToGame" -> solar(SolarIcons.PLAY);
            case "gui.advancements" -> solar(SolarIcons.MEDAL_RIBBON_STAR);
            case "gui.stats" -> solar(SolarIcons.CHAT_SQUARE_STATS);
            case "menu.sendFeedback", "menu.feedback" -> solar(SolarIcons.FEEDBACK_STAR);
            case "menu.reportBugs" -> solar(SolarIcons.BUG);
            case "menu.options" -> new Icon("\uF001", FontWeight.MENU);
            case "menu.shareToLan" -> new Icon(HudIcons.SERVER, FontWeight.HUD_ICONS);
            case "menu.playerReporting" -> solar(SolarIcons.USER);
            case "menu.server_links" -> solar(SolarIcons.LINK);
            case "menu.returnToMenu", "menu.disconnect" -> new Icon("\uF000", FontWeight.MENU);
            default -> null;
        };
    }

    private static Icon solar(String glyph) {
        return new Icon(glyph, FontWeight.SOLAR_ICONS);
    }

    private record Icon(String glyph, FontWeight weight) {
    }
}
