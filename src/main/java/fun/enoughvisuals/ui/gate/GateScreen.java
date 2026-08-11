package fun.enoughvisuals.ui.gate;

import fun.enoughvisuals.systems.network.RemoteConfig;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen, resolution-independent gate shown when the backend reports
 * a ban, a global client block or a required update. Styled after iOS 26
 * (glassy rounded card, pill button). The player cannot dismiss it.
 */
public final class GateScreen extends Screen {

    private enum Kind { UPDATE, BLOCK, BAN }

    private static final int CARD_BG = 0xF22E2E3A;
    private static final int CARD_BORDER = 0x28FFFFFF;
    private static final int SHADOW = 0x55000000;
    private static final int TITLE_RED = 0xFFFF6157;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFC8C8D2;
    private static final int BTN_A = 0xFF6B5CFF;
    private static final int BTN_B = 0xFF8B7BFF;

    private final long openMs = System.currentTimeMillis();

    private Kind kind;
    private String title = "";
    private String message = "";
    private String buttonLabel = "";
    private String buttonUrl = "";

    // button bounds (virtual screen coords), updated each render
    private float btnX, btnY, btnW, btnH;
    private boolean btnHover;

    public GateScreen() {
        super(Text.literal("EnoughVisuals"));
    }

    /** True if the gate is still required given the latest config. */
    public boolean stillRequired() {
        return RemoteConfig.getInstance().isGated();
    }

    private void resolve() {
        RemoteConfig rc = RemoteConfig.getInstance();
        if (rc.isBanned()) {
            kind = Kind.BAN;
            title = ClientLanguage.text("ui.gate.ban_title", "Доступ запрещён");
            String reason = rc.getBanReason();
            message = (reason == null || reason.isBlank())
                    ? ClientLanguage.text("ui.gate.ban_message", "Ваше устройство заблокировано.")
                    : reason;
            buttonLabel = ClientLanguage.text("ui.gate.support", "Поддержка");
            buttonUrl = orDefault(rc.getBlockUrl());
        } else if (rc.isClientBlocked()) {
            kind = Kind.BLOCK;
            title = ClientLanguage.text("ui.gate.block_title", "Клиент заблокирован");
            String m = rc.getBlockMessage();
            message = (m == null || m.isBlank())
                    ? ClientLanguage.text("ui.gate.block_message", "Клиент временно недоступен. Следите за новостями в нашем Telegram.")
                    : m;
            buttonLabel = ClientLanguage.text("ui.gate.details", "Подробнее");
            buttonUrl = orDefault(rc.getBlockUrl());
        } else {
            kind = Kind.UPDATE;
            title = ClientLanguage.text("ui.gate.update_title", "Требуется обновление");
            String m = rc.getUpdateMessage();
            message = (m == null || m.isBlank())
                    ? ClientLanguage.text("ui.gate.update_message", "Эта версия клиента устарела. Установите последнюю версию, чтобы продолжить игру.")
                    : m;
            buttonLabel = ClientLanguage.text("ui.gate.update", "Обновить");
            buttonUrl = orDefault(rc.getUpdateUrl());
        }
    }

    private static String orDefault(String url) {
        return (url == null || url.isBlank()) ? "https://t.me/enoughvisuals" : url;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        resolve();
        super.renderBackground(ctx, mouseX, mouseY, delta);

        float sw = this.width;
        float sh = this.height;

        // intro animation
        float t = Math.min(1f, (System.currentTimeMillis() - openMs) / 220f);
        float ease = 1f - (1f - t) * (1f - t);
        int dim = (int) (0x80 * ease) << 24;
        RenderUtil.drawRect(ctx, 0, 0, sw, sh, dim);

        // layout (virtual coords -> resolution independent)
        float cardW = Math.min(sw * 0.7f, 280f);
        float pad = 16f;
        float titleSize = 14.5f;
        float bodySize = 10.5f;
        float lineGap = 3.5f;

        float innerW = cardW - pad * 2f;
        List<String> lines = wrap(message, innerW, bodySize, FontWeight.REGULAR);
        float bodyLineH = CustomFontRenderer.getTextHeight(bodySize, FontWeight.REGULAR);

        float iconR = 6.5f;
        float headerH = Math.max(iconR * 2f, CustomFontRenderer.getTextHeight(titleSize, FontWeight.BOLD));
        btnH = 29f;

        float contentH = headerH + 12f
                + lines.size() * (bodyLineH + lineGap)
                + 14f + btnH;
        float cardH = contentH + pad * 2f;

        float scale = 0.96f + 0.04f * ease;
        float cardX = (sw - cardW) / 2f;
        float cardY = (sh - cardH) / 2f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(sw / 2f, sh / 2f, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-sw / 2f, -sh / 2f, 0);

        // soft shadow + card
        RenderUtil.drawRoundedRect(ctx, cardX, cardY + 5f, cardW, cardH, 20f, SHADOW);
        RenderUtil.drawRoundedRect(ctx, cardX, cardY, cardW, cardH, 20f, CARD_BG);
        RenderUtil.drawRoundedOutline(ctx, cardX, cardY, cardW, cardH, 20f, 1f, CARD_BORDER);

        float cx = sw / 2f;
        float cursorY = cardY + pad;

        // header: red icon + title, centered as a group
        float titleW = CustomFontRenderer.getTextWidth(title, titleSize, FontWeight.BOLD);
        float groupW = iconR * 2f + 8f + titleW;
        float gx = cx - groupW / 2f;
        float iconCx = gx + iconR;
        float iconCy = cursorY + headerH / 2f;
        RenderUtil.drawCircle(ctx, iconCx, iconCy, iconR, TITLE_RED);
        CustomFontRenderer.drawCenteredText(ctx, "!", iconCx, iconCy - CustomFontRenderer.getTextHeight(11f, FontWeight.BOLD) / 2f, 11f, 0xFFFFFFFF, FontWeight.BOLD);
        CustomFontRenderer.drawText(ctx, title, gx + iconR * 2f + 8f,
                iconCy - CustomFontRenderer.getTextHeight(titleSize, FontWeight.BOLD) / 2f,
                titleSize, TITLE_RED, FontWeight.BOLD);

        cursorY += headerH + 12f;

        // body (centered, wrapped)
        for (String line : lines) {
            CustomFontRenderer.drawCenteredText(ctx, line, cx, cursorY, bodySize, MUTED, FontWeight.REGULAR);
            cursorY += bodyLineH + lineGap;
        }

        cursorY += 14f - lineGap;

        // pill button
        btnW = Math.min(innerW, 180f);
        btnX = cx - btnW / 2f;
        btnY = cursorY;
        float radius = btnH / 2f;
        int a = btnHover ? 0xFF : 0xF0;
        RenderUtil.drawRoundedGradientH(ctx, btnX, btnY, btnW, btnH, radius,
                (a << 24) | (BTN_A & 0xFFFFFF), (a << 24) | (BTN_B & 0xFFFFFF));
        float lblSize = 11.5f;
        CustomFontRenderer.drawCenteredText(ctx, buttonLabel, cx,
                btnY + (btnH - CustomFontRenderer.getTextHeight(lblSize, FontWeight.SEMIBOLD)) / 2f,
                lblSize, 0xFFFFFFFF, FontWeight.SEMIBOLD);

        ctx.getMatrices().pop();

        // hover test (in unscaled virtual coords approximation)
        btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
    }

    private List<String> wrap(String text, float maxWidth, float size, FontWeight weight) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String paragraph : text.split("\n", -1)) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (CustomFontRenderer.getTextWidth(candidate, size, weight) > maxWidth && line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            openLink();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openLink() {
        try {
            Util.getOperatingSystem().open(URI.create(buttonUrl));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
