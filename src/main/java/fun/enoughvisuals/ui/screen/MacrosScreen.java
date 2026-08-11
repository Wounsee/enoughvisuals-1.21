package fun.enoughvisuals.ui.screen;

import fun.enoughvisuals.systems.modules.modules.utils.Macros;
import fun.enoughvisuals.systems.modules.modules.client.CustomTheme;
import fun.enoughvisuals.ui.clickgui.MenuScreen;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.font.SolarIcons;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.render.blur.GlassUtil;
import fun.enoughvisuals.utils.system.BindKeyUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MacrosScreen extends Screen {
    private static final int PANEL_BG = 0xCC000000;
    private static final int HEADER_BG = 0x8C000000;
    private static final int FIELD_BG = 0xD9101217;
    private static final int FIELD_ACTIVE = 0xE5191C23;
    private static final int LIST_BG = 0xA6000000;
    private static final int ROW_BG = 0xB80C0E12;
    private static final int ROW_HOVER = 0xC3151820;
    private static final int BUTTON_ACTIVE = 0xD3181B22;
    private static final int BUTTON_HOVER = 0xE0212630;

    private final Macros module;
    private int selectedKey = BindKeyUtil.NONE;
    private boolean binding;
    private boolean commandFocused;
    private String commandText = "";
    private int cursorIndex;
    private int scroll;
    private long caretStartMs;

    public MacrosScreen(Macros module) {
        super(Text.literal("Macros"));
        this.module = module;
        this.caretStartMs = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MenuScreen.syncTheme();
        GlassUtil.BLUR_OFFSET = CustomTheme.clickGuiBlurLevel();
        if (CustomTheme.clickGuiBlurEnabled()) {
            GlassUtil.beginFrame();
        } else {
            GlassUtil.skipFrame();
        }

        Layout l = layout();
        GlassUtil.drawGlassPanel(ctx, l.x, l.y, l.w, l.h, 7f, 0x000000, 0.68f, 1f);
        RenderUtil.drawRoundedRect(ctx, l.x, l.y, l.w, l.h, 7f, PANEL_BG);
        RenderUtil.drawRoundedRect(ctx, l.x, l.y, l.w, 34f, 7f, 7f, 0f, 0f, HEADER_BG);

        CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.macros.title", "Macros"),
                l.x + 14f, l.y + 11f, 8.8f, MenuScreen.TEXT_WHITE, FontWeight.SEMIBOLD);

        drawLabel(ctx, ClientLanguage.text("ui.macros.key", "Key"), l.keyX, l.formY - 11f);
        drawLabel(ctx, ClientLanguage.text("ui.macros.action", "Command / message"), l.fieldX, l.formY - 11f);
        drawKeyButton(ctx, mouseX, mouseY, l.keyX, l.formY, l.keyW, l.controlH);
        drawCommandField(ctx, l.fieldX, l.formY, l.fieldW, l.controlH);
        drawAddButton(ctx, mouseX, mouseY, l.addX, l.formY, l.addW, l.controlH);
        drawList(ctx, mouseX, mouseY, l.listX, l.listY, l.listW, l.listH);
    }

    private void drawLabel(DrawContext ctx, String text, float x, float y) {
        CustomFontRenderer.drawText(ctx, text, x, y, 6.4f, RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.9f), FontWeight.REGULAR);
    }

    private void drawKeyButton(DrawContext ctx, int mouseX, int mouseY, float x, float y, float w, float h) {
        boolean hovered = hovered(mouseX, mouseY, x, y, w, h);
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 6f,
                binding ? FIELD_ACTIVE : (hovered ? BUTTON_ACTIVE : FIELD_BG));
        CustomFontRenderer.drawCenteredText(ctx, binding ? "..." : BindKeyUtil.getBindName(selectedKey),
                x + w * 0.5f, y + 6.1f, 6.6f, MenuScreen.TEXT_WHITE, FontWeight.SEMIBOLD);
    }

    private void drawCommandField(DrawContext ctx, float x, float y, float w, float h) {
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 6f, commandFocused ? FIELD_ACTIVE : FIELD_BG);

        float textX = x + 9f;
        float textY = y + 6.0f;
        float maxTextW = w - 18f;
        if (commandText.isEmpty() && !commandFocused) {
            CustomFontRenderer.drawText(ctx, ClientLanguage.text("ui.macros.command_placeholder", "/spawn or message"),
                    textX, textY, 6.7f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.78f), FontWeight.REGULAR);
            return;
        }

        String before = commandText.substring(0, Math.max(0, Math.min(cursorIndex, commandText.length())));
        String visibleBefore = tailToWidth(before, maxTextW, 6.7f, FontWeight.REGULAR);
        float beforeW = CustomFontRenderer.getTextWidth(visibleBefore, 6.7f, FontWeight.REGULAR);
        String visibleAfter = CustomFontRenderer.trimToWidth(commandText.substring(cursorIndex), Math.max(0f, maxTextW - beforeW), 6.7f, FontWeight.REGULAR);

        CustomFontRenderer.drawText(ctx, visibleBefore + visibleAfter, textX, textY, 6.7f, MenuScreen.TEXT_WHITE, FontWeight.REGULAR);
        if (commandFocused && ((System.currentTimeMillis() - caretStartMs) / 520L) % 2L == 0L) {
            RenderUtil.drawRoundedRect(ctx, textX + beforeW + 1f, y + 5.2f, 1f, h - 10.4f, 0.5f, MenuScreen.TEXT_WHITE);
        }
    }

    private void drawAddButton(DrawContext ctx, int mouseX, int mouseY, float x, float y, float w, float h) {
        boolean enabled = selectedKey != BindKeyUtil.NONE && !commandText.isBlank();
        boolean hovered = enabled && hovered(mouseX, mouseY, x, y, w, h);
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 6f,
                enabled ? (hovered ? BUTTON_HOVER : BUTTON_ACTIVE) : FIELD_BG);
        CustomFontRenderer.drawCenteredText(ctx, ClientLanguage.text("ui.macros.add", "Add"),
                x + w * 0.5f, y + 6.1f, 6.6f,
                RenderUtil.withAlpha(MenuScreen.TEXT_WHITE, enabled ? 1f : 0.42f), FontWeight.SEMIBOLD);
    }

    private void drawList(DrawContext ctx, int mouseX, int mouseY, float x, float y, float w, float h) {
        RenderUtil.drawRoundedRect(ctx, x, y, w, h, 7f, LIST_BG);

        List<Macros.MacroEntry> macros = module.getMacros();
        int visibleRows = Math.max(1, (int) ((h - 8f) / 25f));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, macros.size() - visibleRows)));

        if (macros.isEmpty()) {
            CustomFontRenderer.drawCenteredText(ctx, ClientLanguage.text("ui.macros.empty", "No macros added"),
                    x + w * 0.5f, y + h * 0.5f - 4f, 7f, RenderUtil.withAlpha(MenuScreen.TEXT_DIM, 0.86f), FontWeight.REGULAR);
            return;
        }

        RenderUtil.enableScissor(x, y, w, h);
        for (int i = scroll; i < macros.size() && i < scroll + visibleRows; i++) {
            float rowY = y + 5f + (i - scroll) * 25f;
            Macros.MacroEntry macro = macros.get(i);
            boolean rowHovered = hovered(mouseX, mouseY, x + 5f, rowY, w - 10f, 21f);
            RenderUtil.drawRoundedRect(ctx, x + 5f, rowY, w - 10f, 21f, 5f, rowHovered ? ROW_HOVER : ROW_BG);

            CustomFontRenderer.drawText(ctx, BindKeyUtil.getBindName(macro.key()), x + 13f, rowY + 6.8f,
                    6.6f, MenuScreen.TEXT_WHITE, FontWeight.SEMIBOLD);
            String action = CustomFontRenderer.trimToWidth(macro.action(), w - 105f, 6.5f, FontWeight.REGULAR);
            CustomFontRenderer.drawText(ctx, action, x + 75f, rowY + 6.8f,
                    6.5f, RenderUtil.withAlpha(MenuScreen.TEXT_GRAY, 0.94f), FontWeight.REGULAR);

            float delX = x + w - 27f;
            float delY = rowY + 4.5f;
            CustomFontRenderer.drawCenteredText(ctx, SolarIcons.DELETE, delX + 6f, delY + 2.4f,
                    8.2f, RenderUtil.withAlpha(0xFFFF6F87, rowHovered ? 1f : 0.68f), FontWeight.SOLAR_ICONS);
        }
        RenderUtil.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (binding) {
            selectedKey = BindKeyUtil.mouseButtonToCode(button);
            binding = false;
            return true;
        }

        Layout l = layout();
        if (hovered(mouseX, mouseY, l.keyX, l.formY, l.keyW, l.controlH)) {
            binding = true;
            commandFocused = false;
            return true;
        }
        if (hovered(mouseX, mouseY, l.fieldX, l.formY, l.fieldW, l.controlH)) {
            commandFocused = true;
            cursorIndex = commandText.length();
            caretStartMs = System.currentTimeMillis();
            return true;
        }
        commandFocused = false;

        if (hovered(mouseX, mouseY, l.addX, l.formY, l.addW, l.controlH)) {
            addMacro();
            return true;
        }
        if (clickDelete((int) mouseX, (int) mouseY, l)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout l = layout();
        if (hovered(mouseX, mouseY, l.listX, l.listY, l.listW, l.listH)) {
            int visibleRows = Math.max(1, (int) ((l.listH - 8f) / 25f));
            int max = Math.max(0, module.getMacros().size() - visibleRows);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                selectedKey = BindKeyUtil.NONE;
            } else {
                selectedKey = keyCode;
            }
            binding = false;
            return true;
        }

        if (commandFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                commandFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                addMacro();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && cursorIndex > 0) {
                commandText = commandText.substring(0, cursorIndex - 1) + commandText.substring(cursorIndex);
                cursorIndex--;
                resetCaret();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE && cursorIndex < commandText.length()) {
                commandText = commandText.substring(0, cursorIndex) + commandText.substring(cursorIndex + 1);
                resetCaret();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                cursorIndex = Math.max(0, cursorIndex - 1);
                resetCaret();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                cursorIndex = Math.min(commandText.length(), cursorIndex + 1);
                resetCaret();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                cursorIndex = 0;
                resetCaret();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                cursorIndex = commandText.length();
                resetCaret();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!commandFocused || Character.isISOControl(chr) || commandText.length() >= 256) {
            return super.charTyped(chr, modifiers);
        }
        commandText = commandText.substring(0, cursorIndex) + chr + commandText.substring(cursorIndex);
        cursorIndex++;
        resetCaret();
        return true;
    }

    private boolean clickDelete(int mouseX, int mouseY, Layout l) {
        List<Macros.MacroEntry> macros = module.getMacros();
        int visibleRows = Math.max(1, (int) ((l.listH - 8f) / 25f));
        for (int i = scroll; i < macros.size() && i < scroll + visibleRows; i++) {
            float rowY = l.listY + 5f + (i - scroll) * 25f;
            float delX = l.listX + l.listW - 31f;
            if (hovered(mouseX, mouseY, delX, rowY, 24f, 21f)) {
                module.removeMacro(i);
                return true;
            }
        }
        return false;
    }

    private void addMacro() {
        if (selectedKey == BindKeyUtil.NONE || commandText == null || commandText.isBlank()) return;
        module.addMacro(selectedKey, commandText);
        commandText = "";
        cursorIndex = 0;
        selectedKey = BindKeyUtil.NONE;
        resetCaret();
    }

    private void resetCaret() {
        caretStartMs = System.currentTimeMillis();
    }

    private String tailToWidth(String text, float maxWidth, float size, FontWeight weight) {
        if (CustomFontRenderer.getTextWidth(text, size, weight) <= maxWidth) return text;
        StringBuilder out = new StringBuilder();
        for (int i = text.length() - 1; i >= 0; i--) {
            String candidate = text.charAt(i) + out.toString();
            if (CustomFontRenderer.getTextWidth(candidate, size, weight) > maxWidth) break;
            out.insert(0, text.charAt(i));
        }
        return out.toString();
    }

    private Layout layout() {
        float w = Math.min(374f, width - 28f);
        float h = Math.min(244f, height - 28f);
        float x = (width - w) * 0.5f;
        float y = (height - h) * 0.5f;
        return new Layout(x, y, w, h);
    }

    private boolean hovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static class Layout {
        final float x;
        final float y;
        final float w;
        final float h;
        final float formY;
        final float keyX;
        final float keyW;
        final float fieldX;
        final float fieldW;
        final float addX;
        final float addW;
        final float controlH;
        final float listX;
        final float listY;
        final float listW;
        final float listH;

        Layout(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.formY = y + 43f;
            this.keyX = x + 14f;
            this.keyW = 78f;
            this.addW = 62f;
            this.controlH = 20f;
            this.fieldX = keyX + keyW + 8f;
            this.addX = x + w - 14f - addW;
            this.fieldW = Math.max(88f, addX - fieldX - 8f);
            this.listX = x + 14f;
            this.listY = y + 76f;
            this.listW = w - 28f;
            this.listH = h - 90f;
        }
    }
}
