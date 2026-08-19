package fun.enoughvisuals.systems.webpanel;

import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Modern glass address bar for WebPanels.
 * Type a URL or a search query and press Enter to navigate.
 */
public class WebUrlScreen extends Screen {

    private final String initial;
    private final Consumer<String> onSubmit;
    private String text;
    private int cursor;
    private float anim;
    private long openTime;

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
        anim = Math.min(1f, elapsed);
        float alpha = anim;

        int w = 380;
        int h = 100;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        GlassUtil.drawGlassPanel(context, x, y, w, h, 10f, 0xFF101216, 0.85f * alpha, 0.60f * alpha);

        CustomFontRenderer.drawText(context, "Веб-адрес", x + 16, y + 14, 7f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.BOLD);
        CustomFontRenderer.drawText(context, "Введите URL (например, https://youtube.com) или запрос для поиска",
                x + 16, y + 27, 4.5f, RenderUtil.withAlpha(0xFF8E929E, alpha), FontWeight.REGULAR);

        int boxX = x + 16;
        int boxY = y + 42;
        int boxW = w - 32;
        int boxH = 24;

        RenderUtil.drawRoundedRect(context, boxX, boxY, boxW, boxH, 6f, RenderUtil.withAlpha(0xFF1B1E26, alpha));
        RenderUtil.drawRoundedOutline(context, boxX, boxY, boxW, boxH, 6f, 1f, RenderUtil.withAlpha(MenuScreen.ACCENT_LIGHT, alpha * 0.8f));

        String displayText = text.isEmpty() ? "https://" : text;
        int textColor = text.isEmpty() ? 0xFF656A78 : 0xFFFFFFFF;
        CustomFontRenderer.drawText(context, displayText, boxX + 8, boxY + 7, 5.5f, RenderUtil.withAlpha(textColor, alpha), FontWeight.MEDIUM);

        // Blinking cursor
        if ((System.currentTimeMillis() / 450) % 2 == 0) {
            float textWidth = text.isEmpty() ? 0 : CustomFontRenderer.getTextWidth(text.substring(0, Math.min(cursor, text.length())), 5.5f, FontWeight.MEDIUM);
            RenderUtil.drawRect(context, boxX + 8 + textWidth, boxY + 5, 1f, 14, RenderUtil.withAlpha(0xFFFFFFFF, alpha));
        }

        // Enter button
        boolean hoverEnter = mouseX >= boxX + boxW - 60 && mouseX <= boxX + boxW && mouseY >= y + 72 && mouseY <= y + 90;
        int btnColor = hoverEnter ? MenuScreen.ACCENT_LIGHT : MenuScreen.ACCENT;
        RenderUtil.drawRoundedRect(context, boxX + boxW - 60, y + 72, 60, 18, 5f, RenderUtil.withAlpha(btnColor, alpha));
        CustomFontRenderer.drawCenteredText(context, "Перейти", boxX + boxW - 30, y + 76, 5f, RenderUtil.withAlpha(0xFFFFFFFF, alpha), FontWeight.SEMIBOLD);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = 380;
        int h = 100;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        int boxX = x + 16;
        int boxW = w - 32;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= boxX + boxW - 60 && mouseX <= boxX + boxW && mouseY >= y + 72 && mouseY <= y + 90) {
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
