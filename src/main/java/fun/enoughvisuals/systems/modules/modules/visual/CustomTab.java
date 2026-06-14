package fun.enoughvisuals.systems.modules.modules.visual;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Register(name = "Custom Tab", id = "custom_tab", description = "Кастомный список игроков", category = Category.VISUAL)
public class CustomTab extends Module {
    public static CustomTab INSTANCE;

    public final BooleanSetting showPing = add(new BooleanSetting("Пинг", "show_ping", "Показывать пинг", true));
    public final SliderSetting maxPlayers = add(new SliderSetting("Игроки", "max_players", "Максимум отображаемых игроков", 120, 20, 240, 1));

    private static final int NAME_DEFAULT = 0xFFEFF4F7;
    private static final int HEADER_COLOR = 0xFFDDE7EC;
    private static final int SUB_HEADER_COLOR = 0xFF95A3AC;
    private static final Pattern SERVER_ANARCHY_PATTERN = Pattern.compile("(?iu)(анархия|anarchy)\\s*[-—–:#№]?\\s*(\\d{1,4})");
    private static final Map<String, Integer> PRIVILEGE_RANK = new HashMap<>();

    private static final float NAME_SIZE = 6.0f;
    private static final float PING_SIZE = 5.75f;
    private static final float HEADER_SIZE = 6.1f;
    private static final float ROW_H = 9.9f;
    private static final float COL_GAP = 11.5f;
    private static final float PAD = 8f;
    private static final float TOP_PAD = 7.5f;
    private static final float HEADER_H = 10.4f;
    private static final float BOTTOM_PAD = 7f;
    private static boolean clipActive = false;
    private static float clipX = 0f;
    private static float clipY = 0f;
    private static float clipW = 0f;
    private static float clipH = 0f;

    private float anim = 0f;

    static {
        PRIVILEGE_RANK.put("owner", 130);
        PRIVILEGE_RANK.put("владелец", 130);
        PRIVILEGE_RANK.put("creator", 125);
        PRIVILEGE_RANK.put("создатель", 125);
        PRIVILEGE_RANK.put("admin", 120);
        PRIVILEGE_RANK.put("administrator", 120);
        PRIVILEGE_RANK.put("админ", 120);
        PRIVILEGE_RANK.put("moder", 115);
        PRIVILEGE_RANK.put("mod", 115);
        PRIVILEGE_RANK.put("модер", 115);
        PRIVILEGE_RANK.put("helper", 110);
        PRIVILEGE_RANK.put("хелпер", 110);

        PRIVILEGE_RANK.put("герцог", 100);
        PRIVILEGE_RANK.put("князь", 95);
        PRIVILEGE_RANK.put("принц", 90);
        PRIVILEGE_RANK.put("титан", 85);
        PRIVILEGE_RANK.put("барон", 80);
        PRIVILEGE_RANK.put("лорд", 75);
        PRIVILEGE_RANK.put("элита", 70);
        PRIVILEGE_RANK.put("сквид", 65);
        PRIVILEGE_RANK.put("страж", 60);
        PRIVILEGE_RANK.put("игрок", 10);
    }

    public CustomTab() {
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        anim = 0f;
    }

    public static void tickAnimation(float delta) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        boolean show = shouldRenderCustomTab();
        INSTANCE.anim = RenderUtil.animate(INSTANCE.anim, show ? 1f : 0f, show ? 0.46f : 0.34f, RenderUtil.getFrameDelta());
    }

    public static float getAnimationProgress() {
        return INSTANCE == null ? 0f : INSTANCE.anim;
    }

    public static boolean shouldRenderCustomTab() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
        if (mc == null || mc.getWindow() == null || mc.player == null || mc.world == null) return false;
        if (mc.currentScreen != null) return false;
        long window = mc.getWindow().getHandle();
        return window != 0L && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS;
    }

    public static float[] getPreferredSize() {
        TabData data = prepareData();
        if (data == null) return null;
        return new float[]{data.panelW, data.panelH};
    }

    public static void renderInPanel(DrawContext ctx, float x, float y, float w, float h, float alpha) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return;
        TabData data = prepareData();
        if (data == null) return;

        float tabFade = INSTANCE == null ? 1f : RenderUtil.easeInOutCubic(Math.max(0f, Math.min(1f, INSTANCE.anim)));
        float contentAlpha = Math.max(0f, Math.min(1f, alpha)) * tabFade;
        clipActive = true;
        clipX = x;
        clipY = y;
        clipW = w;
        clipH = h;
        RenderUtil.enableScissor(x, y, w, h);
        try {

        float headerX = x + PAD;
        float headerY = y + TOP_PAD;
        CustomFontRenderer.drawText(ctx, "Онлайн: " + data.online, headerX, headerY, HEADER_SIZE,
                RenderUtil.withAlpha(HEADER_COLOR, contentAlpha * 0.98f), FontWeight.SEMIBOLD);

        String serverText = trimToWidth(resolveCurrentServerLabel(), w * 0.48f, 5.45f, FontWeight.REGULAR);
        float serverW = CustomFontRenderer.getTextWidth(serverText, 5.45f, FontWeight.REGULAR);
        CustomFontRenderer.drawText(ctx, serverText, x + w - PAD - serverW, headerY + 0.28f, 5.45f,
                RenderUtil.withAlpha(SUB_HEADER_COLOR, contentAlpha * 0.90f), FontWeight.REGULAR);

        float contentW = contentWidth(data);
        float startX = x + Math.max(PAD, (w - contentW) * 0.5f);
        float baseY = y + TOP_PAD + HEADER_H;

        float[] colX = new float[data.columns];
        float cursorX = startX;
        for (int c = 0; c < data.columns; c++) {
            colX[c] = cursorX;
            cursorX += data.colNameW[c] + (data.withPing ? (6f + data.colPingW[c]) : 0f);
            if (c < data.columns - 1) cursorX += COL_GAP;
        }

        for (int i = 0; i < data.shown; i++) {
            PlayerLine line = data.lines.get(i);
            int col = i / data.rows;
            int row = i % data.rows;
            float rowY = baseY + row * ROW_H;
            float nameX = colX[col];

            drawFormatted(ctx, line.parts, nameX, rowY, NAME_SIZE, FontWeight.SEMIBOLD, contentAlpha * 0.95f, NAME_DEFAULT);

            if (data.withPing) {
                String ping = line.entry.getLatency() + "ms";
                float pingW = CustomFontRenderer.getTextWidth(ping, PING_SIZE, FontWeight.REGULAR);
                float pingX = colX[col] + data.colNameW[col] + 6f + (data.colPingW[col] - pingW);
                CustomFontRenderer.drawText(ctx, ping, pingX, rowY + 0.1f, PING_SIZE,
                        pingColor(line.entry.getLatency(), contentAlpha), FontWeight.REGULAR);
            }
        }

        } finally {
            RenderUtil.disableScissor();
            clipActive = false;
        }
    }

    private static TabData prepareData() {
        if (mc == null || mc.getNetworkHandler() == null || mc.player == null) return null;
        List<PlayerListEntry> allEntries = new ArrayList<>(mc.getNetworkHandler().getPlayerList());
        if (allEntries.isEmpty()) return null;

        List<PlayerLine> parsed = new ArrayList<>(allEntries.size());
        for (PlayerListEntry entry : allEntries) {
            parsed.add(parsePlayer(entry));
        }

        parsed.sort(Comparator
                .comparingInt((PlayerLine p) -> p.rank).reversed()
                .thenComparing(p -> p.privilegeKey)
                .thenComparing(p -> p.plainName, String.CASE_INSENSITIVE_ORDER));

        int online = allEntries.size();
        int limit = INSTANCE == null ? online : Math.max(1, INSTANCE.maxPlayers.get().intValue());
        if (parsed.size() > limit) {
            parsed = new ArrayList<>(parsed.subList(0, limit));
        }

        boolean withPing = INSTANCE != null && INSTANCE.showPing.get();
        int shown = parsed.size();
        int columns = shown > 88 ? 5 : shown > 58 ? 4 : shown > 34 ? 3 : shown > 16 ? 2 : 1;
        int rows = (int) Math.ceil(shown / (double) columns);

        float[] colNameW = new float[columns];
        float[] colPingW = new float[columns];
        for (int i = 0; i < shown; i++) {
            PlayerLine line = parsed.get(i);
            int col = i / rows;
            colNameW[col] = Math.max(colNameW[col], measureFormatted(line.parts, NAME_SIZE, FontWeight.SEMIBOLD));
            if (withPing) {
                String ping = line.entry.getLatency() + "ms";
                colPingW[col] = Math.max(colPingW[col], CustomFontRenderer.getTextWidth(ping, PING_SIZE, FontWeight.REGULAR));
            }
        }

        float panelW = PAD * 2f;
        for (int c = 0; c < columns; c++) {
            panelW += colNameW[c];
            if (withPing) panelW += 6f + colPingW[c];
            if (c < columns - 1) panelW += COL_GAP;
        }
        float panelH = TOP_PAD + HEADER_H + rows * ROW_H + BOTTOM_PAD;

        return new TabData(parsed, online, shown, columns, rows, withPing, colNameW, colPingW, panelW, panelH);
    }

    private static float contentWidth(TabData data) {
        float value = 0f;
        for (int c = 0; c < data.columns; c++) {
            value += data.colNameW[c];
            if (data.withPing) value += 6f + data.colPingW[c];
            if (c < data.columns - 1) value += COL_GAP;
        }
        return value;
    }

    private static PlayerLine parsePlayer(PlayerListEntry entry) {
        List<TextPart> parts = new ArrayList<>();
        Text display = entry.getDisplayName();
        if (display != null) {
            display.visit((style, string) -> {
                int styleColor = colorFromStyle(style, NAME_DEFAULT);
                parts.addAll(parseLegacySegments(string, styleColor, styleColor));
                return Optional.empty();
            }, Style.EMPTY);
        }

        if (parts.isEmpty()) {
            String fallback = entry.getProfile() != null && entry.getProfile().getName() != null
                    ? entry.getProfile().getName()
                    : "Unknown";
            parts.addAll(parseLegacySegments(fallback, NAME_DEFAULT, NAME_DEFAULT));
        }

        String plain = buildPlain(parts);
        String privilege = extractPrivilege(plain);
        int rank = PRIVILEGE_RANK.getOrDefault(privilege, 0);
        return new PlayerLine(entry, parts, plain, privilege, rank);
    }

    private static int colorFromStyle(Style style, int fallback) {
        try {
            if (style != null && style.getColor() != null) {
                return 0xFF000000 | style.getColor().getRgb();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static List<TextPart> parseLegacySegments(String input, int baseColor, int resetColor) {
        List<TextPart> out = new ArrayList<>();
        if (input == null || input.isEmpty()) return out;

        int currentColor = baseColor;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch == '&' || ch == '\u00A7') && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                Integer color = legacyColor(code);
                boolean isReset = code == 'r';
                boolean isFormat = "klmno".indexOf(code) >= 0;
                if (color != null || isReset || isFormat) {
                    if (sb.length() > 0) {
                        out.add(new TextPart(sb.toString(), currentColor));
                        sb.setLength(0);
                    }
                    if (color != null) currentColor = color;
                    else if (isReset) currentColor = resetColor;
                    i++;
                    continue;
                }
            }
            sb.append(ch);
        }

        if (sb.length() > 0) out.add(new TextPart(sb.toString(), currentColor));
        return out;
    }

    private static Integer legacyColor(char code) {
        return switch (code) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            default -> null;
        };
    }

    private static String buildPlain(List<TextPart> parts) {
        StringBuilder sb = new StringBuilder();
        for (TextPart part : parts) sb.append(part.text);
        return sb.toString().trim();
    }

    private static String extractPrivilege(String plainName) {
        if (plainName == null || plainName.isBlank()) return "";
        String[] tokens = plainName.trim().split("\\s+");
        int checks = Math.min(tokens.length, 3);
        for (int i = 0; i < checks; i++) {
            String normalized = tokens[i].toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "");
            if (normalized.isEmpty()) continue;
            if (PRIVILEGE_RANK.containsKey(normalized)) return normalized;
        }
        return "";
    }

    private static float measureFormatted(List<TextPart> parts, float size, FontWeight weight) {
        float w = 0f;
        for (TextPart part : parts) {
            if (part.text == null || part.text.isEmpty()) continue;
            w += measureStringWithFallback(part.text, size, weight);
        }
        return w;
    }

    private static void drawFormatted(DrawContext ctx, List<TextPart> parts, float x, float y, float size,
                                      FontWeight weight, float alpha, int fallbackColor) {
        float cursor = x;
        for (TextPart part : parts) {
            if (part.text == null || part.text.isEmpty()) continue;
            int color = RenderUtil.withAlpha(part.color == 0 ? fallbackColor : part.color, alpha);
            cursor += drawStringWithFallback(ctx, part.text, cursor, y, size, color, weight);
        }
    }

    private static float measureStringWithFallback(String text, float size, FontWeight weight) {
        if (text == null || text.isEmpty()) return 0f;
        float width = 0f;
        float vanillaScale = size / 9f;
        StringBuilder run = new StringBuilder();
        boolean runVanilla = false;
        boolean hasRun = false;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            boolean vanilla = shouldUseVanillaGlyph(cp);
            if (!hasRun) {
                runVanilla = vanilla;
                hasRun = true;
            }
            if (vanilla != runVanilla) {
                width += measureRun(run.toString(), runVanilla, size, vanillaScale, weight);
                run.setLength(0);
                runVanilla = vanilla;
            }
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
        }

        if (run.length() > 0) width += measureRun(run.toString(), runVanilla, size, vanillaScale, weight);
        return width;
    }

    private static float drawStringWithFallback(DrawContext ctx, String text, float x, float y, float size, int color, FontWeight weight) {
        if (text == null || text.isEmpty()) return 0f;
        float cursor = x;
        float vanillaScale = size / 9f;
        StringBuilder run = new StringBuilder();
        boolean runVanilla = false;
        boolean hasRun = false;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            boolean vanilla = shouldUseVanillaGlyph(cp);
            if (!hasRun) {
                runVanilla = vanilla;
                hasRun = true;
            }
            if (vanilla != runVanilla) {
                cursor += drawRun(ctx, run.toString(), cursor, y, color, runVanilla, size, vanillaScale, weight);
                run.setLength(0);
                runVanilla = vanilla;
            }
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
        }

        if (run.length() > 0) cursor += drawRun(ctx, run.toString(), cursor, y, color, runVanilla, size, vanillaScale, weight);
        return cursor - x;
    }

    private static float drawRun(DrawContext ctx, String run, float x, float y, int color,
                                 boolean vanilla, float size, float vanillaScale, FontWeight weight) {
        if (run.isEmpty()) return 0f;
        if (!vanilla) {
            CustomFontRenderer.drawText(ctx, run, x, y, size, color, weight);
            return CustomFontRenderer.getTextWidth(run, size, weight);
        }

        float w = mc.textRenderer.getWidth(run) * vanillaScale;
        if (clipActive) {
            RenderUtil.enableScissor(clipX, clipY, clipW, clipH);
        }
        ctx.getMatrices().push();
        try {
            ctx.getMatrices().translate(x, y + 0.35f, 80f);
            ctx.getMatrices().scale(vanillaScale, vanillaScale, 1f);
            ctx.drawText(mc.textRenderer, run, 0, 0, color, false);
        } finally {
            ctx.getMatrices().pop();
            if (clipActive) {
                RenderUtil.disableScissor();
            }
        }
        return w;
    }

    private static float measureRun(String run, boolean vanilla, float size, float vanillaScale, FontWeight weight) {
        if (run.isEmpty()) return 0f;
        if (!vanilla) return CustomFontRenderer.getTextWidth(run, size, weight);
        return mc.textRenderer.getWidth(run) * vanillaScale;
    }

    private static boolean shouldUseVanillaGlyph(int codePoint) {
        return codePoint == 0x26A1 || codePoint == 0xFE0F;
    }

    private static String resolveCurrentServerLabel() {
        try {
            if (mc != null && mc.world != null) {
                Scoreboard scoreboard = mc.world.getScoreboard();
                if (scoreboard != null) {
                    ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                    if (objective != null && objective.getDisplayName() != null) {
                        String sidebar = objective.getDisplayName().getString();
                        if (sidebar != null && !sidebar.isBlank()) {
                            Matcher matcher = SERVER_ANARCHY_PATTERN.matcher(sidebar);
                            if (matcher.find()) {
                                String kind = matcher.group(1);
                                String num = matcher.group(2);
                                if (kind != null && num != null) {
                                    String prefix = kind.toLowerCase(Locale.ROOT).contains("anarchy") ? "Anarchy" : "Анархия";
                                    return prefix + "-" + num;
                                }
                            }
                            return sidebar.trim();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            ServerInfo info = mc == null ? null : mc.getCurrentServerEntry();
            if (info != null && info.address != null && !info.address.isBlank()) {
                String address = info.address.trim();
                int idx = address.lastIndexOf(':');
                return idx > 0 ? address.substring(0, idx) : address;
            }
        } catch (Throwable ignored) {
        }
        return "?";
    }

    private static String trimToWidth(String text, float maxWidth, float size, FontWeight weight) {
        if (text == null || text.isEmpty()) return "";
        if (CustomFontRenderer.getTextWidth(text, size, weight) <= maxWidth) return text;
        String base = text;
        while (base.length() > 1) {
            base = base.substring(0, base.length() - 1);
            String candidate = base + "...";
            if (CustomFontRenderer.getTextWidth(candidate, size, weight) <= maxWidth) return candidate;
        }
        return "...";
    }

    private static int pingColor(int ping, float alpha) {
        int color;
        if (ping <= 55) color = 0xFF7CFF9B;
        else if (ping <= 100) color = 0xFFE3FF7C;
        else if (ping <= 170) color = 0xFFFFC16F;
        else color = 0xFFFF7C7C;
        return RenderUtil.withAlpha(color, alpha * 0.9f);
    }

    private record TextPart(String text, int color) {}
    private record PlayerLine(PlayerListEntry entry, List<TextPart> parts, String plainName, String privilegeKey, int rank) {}
    private record TabData(List<PlayerLine> lines, int online, int shown, int columns, int rows, boolean withPing,
                           float[] colNameW, float[] colPingW, float panelW, float panelH) {}
}
