package fun.enoughvisuals.systems.webpanel;

import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Premium Apple Vision style glass address & search modal for WebPanels.
 * Features MSDF fonts, Solar icons, Kawase background blur, and quick bookmarks.
 */
public class WebUrlScreen extends Screen {

    private final String initial;
    private final Consumer<String> onSubmit;
    private String text;
    private int cursor;
    private long openTime;

    private static final record Bookmark(String name, String url, String icon) {}
    private static final List<Bookmark> BOOKMARKS = List.of(
            new Bookmark("YouTube", "https://youtube.com", SolarIcons.PLAY),
            new Bookmark("Google", "https://google.com", SolarIcons.SEARCH),
            new Bookmark("ChatGPT", "https://chatgpt.com", SolarIcons.ROBOT),
            new Bookmark("Twitch", "https://twitch.tv", SolarIcons.GAMEPAD),
            new Bookmark("GitHub", "https://github.com", SolarIcons.CODE)
    );

    public WebUrlScreen(String initial, Consumer<String> onSubmit) {
        super(Text.literal("Адрес"));
        this.initial = initial == null ? "" : initial;
        this.onSubmit = onSubmit;
        this.text = this.initial;
        this.cursor = this.text.length();
    }

    @Override
    protected void init() {
        super.init();
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float elapsed = (System.currentTimeMillis() - openTime) / 180f;
        float anim = Math.min(1f, elapsed);
        float alpha = anim;

        // Subtle backdrop tint (no full-screen blur to keep world clear)
        context.fill(0, 0, this.width, this.height, RenderUtil.withAlpha(0x000000, 0.45f * alpha));

        // Main modal dialog
        int w = 460;
        int h = 180;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        // Glass background with subtle border
        GlassUtil.drawGlassPanel(context, x, y, w, h, 12f, 0xFF0E1015, 0.88f * alpha, 0.85f * alpha);
        RenderUtil.drawRoundedOutline(context, x, y, w, h, 12f, 1f, RenderUtil.withAlpha(MenuScreen.ACCENT, 0.35f * alpha));

        // Header: Global icon + Title + Subtitle
        CustomFontRenderer.drawText(context, SolarIcons.GLOBAL, x + 16, y + 14, 9f, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha), FontWeight.SOLAR_ICONS);
        CustomFontRenderer.drawText(context, "Веб-навигация", x + 34, y + 13, 8f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.BOLD);
        CustomFontRenderer.drawText(context, "Введите URL сайта или поисковый запрос",
                x + 34, y + 26, 5.5f, RenderUtil.withAlpha(0xFF8E95A5, alpha), FontWeight.REGULAR);

        // Close button (X) on top right
        boolean hoverClose = mouseX >= x + w - 28 && mouseX <= x + w - 10 && mouseY >= y + 10 && mouseY <= y + 28;
        int closeColor = hoverClose ? 0xFFFF5555 : 0xFF7A8090;
        CustomFontRenderer.drawCenteredText(context, SolarIcons.CLOSE, x + w - 19, y + 14, 7f, RenderUtil.withAlpha(closeColor, alpha), FontWeight.SOLAR_ICONS);

        // Search / URL Input Box
        int boxX = x + 16;
        int boxY = y + 42;
        int boxW = w - 32;
        int boxH = 28;

        RenderUtil.drawRoundedRect(context, boxX, boxY, boxW, boxH, 7f, RenderUtil.withAlpha(0xFF141720, alpha));
        RenderUtil.drawRoundedOutline(context, boxX, boxY, boxW, boxH, 7f, 1f, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, 0.75f * alpha));

        // Search icon inside box
        CustomFontRenderer.drawText(context, SolarIcons.SEARCH, boxX + 10, boxY + 8, 7.5f, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, 0.85f * alpha), FontWeight.SOLAR_ICONS);

        // Input text or placeholder
        if (text.isEmpty()) {
            CustomFontRenderer.drawText(context, "https:// или поисковый запрос Google...", boxX + 30, boxY + 8, 6.5f, RenderUtil.withAlpha(0xFF606676, alpha), FontWeight.MEDIUM);
        } else {
            CustomFontRenderer.drawText(context, text, boxX + 30, boxY + 8, 6.5f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.MEDIUM);
        }

        // Blinking text cursor
        if ((System.currentTimeMillis() / 450) % 2 == 0) {
            float textWidth = text.isEmpty() ? 0 : CustomFontRenderer.getTextWidth(text.substring(0, Math.min(cursor, text.length())), 6.5f, FontWeight.MEDIUM);
            RenderUtil.drawRect(context, boxX + 30 + textWidth, boxY + 6, 1.2f, 16, RenderUtil.withAlpha(0xFFFFFFFF, alpha));
        }

        // Clear text button (X) inside input box if not empty
        if (!text.isEmpty()) {
            boolean hoverClear = mouseX >= boxX + boxW - 24 && mouseX <= boxX + boxW - 6 && mouseY >= boxY + 4 && mouseY <= boxY + 24;
            int clrCol = hoverClear ? 0xFFFFFFFF : 0xFF7A8090;
            CustomFontRenderer.drawCenteredText(context, SolarIcons.CLOSE, boxX + boxW - 14, boxY + 9, 6.5f, RenderUtil.withAlpha(clrCol, alpha), FontWeight.SOLAR_ICONS);
        }

        // Bookmarks row
        int chipX = boxX;
        int chipY = y + 80;
        for (Bookmark bm : BOOKMARKS) {
            float chipW = CustomFontRenderer.getTextWidth(bm.name, 5.5f, FontWeight.MEDIUM) + 24f;
            int chipH = 18;
            boolean hoverBm = mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + chipH;

            int chipBg = hoverBm ? 0xFF242A38 : 0xFF171A24;
            RenderUtil.drawRoundedRect(context, chipX, chipY, chipW, chipH, 5f, RenderUtil.withAlpha(chipBg, alpha));
            if (hoverBm) {
                RenderUtil.drawRoundedOutline(context, chipX, chipY, chipW, chipH, 5f, 1f, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, 0.60f * alpha));
            }

            CustomFontRenderer.drawText(context, bm.icon, chipX + 6, chipY + 4, 6f, RenderUtil.withAlpha(hoverBm ? MenuScreen.ACCENT_LIGHT : 0xFF8E95A5, alpha), FontWeight.SOLAR_ICONS);
            CustomFontRenderer.drawText(context, bm.name, chipX + 18, chipY + 4.5f, 5.5f, RenderUtil.withAlpha(hoverBm ? 0xFFFFFFFF : 0xFFB0B6C4, alpha), FontWeight.MEDIUM);

            chipX += chipW + 6;
        }

        // Action Buttons: "Перейти" (Navigate) and "Отмена" (Cancel)
        int btnH = 22;
        int btnW = 85;
        int submitX = x + w - 16 - btnW;
        int submitY = y + h - 16 - btnH;
        int cancelX = submitX - 75;
        int cancelY = submitY;

        // Cancel button
        boolean hoverCancel = mouseX >= cancelX && mouseX <= cancelX + 65 && mouseY >= cancelY && mouseY <= cancelY + btnH;
        int cancelBg = hoverCancel ? 0xFF222632 : 0xFF161922;
        RenderUtil.drawRoundedRect(context, cancelX, cancelY, 65, btnH, 6f, RenderUtil.withAlpha(cancelBg, alpha));
        CustomFontRenderer.drawCenteredText(context, "Отмена", cancelX + 32, cancelY + 6.5f, 5.5f, RenderUtil.withAlpha(hoverCancel ? 0xFFFFFFFF : 0xFF8E95A5, alpha), FontWeight.MEDIUM);

        // Submit button
        boolean hoverSubmit = mouseX >= submitX && mouseX <= submitX + btnW && mouseY >= submitY && mouseY <= submitY + btnH;
        int submitBg = hoverSubmit ? MenuScreen.ACCENT_LIGHT : MenuScreen.ACCENT;
        RenderUtil.drawRoundedRect(context, submitX, submitY, btnW, btnH, 6f, RenderUtil.withAlpha(submitBg, alpha));
        CustomFontRenderer.drawText(context, SolarIcons.LINK, submitX + 10, submitY + 6, 6.5f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.SOLAR_ICONS);
        CustomFontRenderer.drawText(context, "Перейти", submitX + 26, submitY + 6.5f, 6f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.SEMIBOLD);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int w = 460;
            int h = 180;
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;

            // Close button (X)
            if (mouseX >= x + w - 28 && mouseX <= x + w - 10 && mouseY >= y + 10 && mouseY <= y + 28) {
                close();
                return true;
            }

            int boxX = x + 16;
            int boxY = y + 42;
            int boxW = w - 32;

            // Clear text button (X)
            if (!text.isEmpty() && mouseX >= boxX + boxW - 24 && mouseX <= boxX + boxW - 6 && mouseY >= boxY + 4 && mouseY <= boxY + 24) {
                text = "";
                cursor = 0;
                return true;
            }

            // Bookmarks
            int chipX = boxX;
            int chipY = y + 80;
            for (Bookmark bm : BOOKMARKS) {
                float chipW = CustomFontRenderer.getTextWidth(bm.name, 5.5f, FontWeight.MEDIUM) + 24f;
                int chipH = 18;
                if (mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + chipH) {
                    if (onSubmit != null) {
                        onSubmit.accept(bm.url);
                    }
                    close();
                    return true;
                }
                chipX += chipW + 6;
            }

            // Cancel button
            int btnH = 22;
            int btnW = 85;
            int submitX = x + w - 16 - btnW;
            int submitY = y + h - 16 - btnH;
            int cancelX = submitX - 75;
            int cancelY = submitY;

            if (mouseX >= cancelX && mouseX <= cancelX + 65 && mouseY >= cancelY && mouseY <= cancelY + btnH) {
                close();
                return true;
            }

            // Submit button
            if (mouseX >= submitX && mouseX <= submitX + btnW && mouseY >= submitY && mouseY <= submitY + btnH) {
                submit();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!text.isEmpty() && cursor > 0) {
                text = text.substring(0, cursor - 1) + text.substring(cursor);
                cursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursor < text.length()) {
                text = text.substring(0, cursor) + text.substring(cursor + 1);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursor = Math.max(0, cursor - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursor = Math.min(text.length(), cursor + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                text = text.substring(0, cursor) + clip + text.substring(cursor);
                cursor += clip.length();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127) {
            text = text.substring(0, cursor) + chr + text.substring(cursor);
            cursor++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void submit() {
        String url = text.trim();
        if (!url.isEmpty()) {
            if (url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
                // valid full URL
            } else if (url.contains(" ") || !url.contains(".")) {
                url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            } else {
                url = "https://" + url;
            }
            if (onSubmit != null) {
                onSubmit.accept(url);
            }
        }
        close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
