package fun.enoughvisuals.ui.hud;

import fun.enoughvisuals.systems.hud.HudElement;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.modules.settings.ModeSetting;
import fun.enoughvisuals.systems.modules.settings.SliderSetting;
import fun.enoughvisuals.utils.font.CustomFontRenderer;
import fun.enoughvisuals.utils.font.CustomFontRenderer.FontWeight;
import fun.enoughvisuals.utils.render.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomScoreboardHud extends HudElement {
    private static final float PAD = 8f;
    private static final float HEADER_H = 18f;
    private static final float ROW_H = 10.5f;

    public final SliderSetting scale = addSetting(new SliderSetting("Scale", "scale", 1.0, 0.65, 1.5, 0.05));
    public final ModeSetting font = addSetting(new ModeSetting("Font", "font", "Scoreboard font",
            "semibold", "regular", "semibold", "bold", "light"));
    public final BooleanSetting scores = addSetting(new BooleanSetting("Scores", "scores", true));
    public final SliderSetting maxLines = addSetting(new SliderSetting("Lines", "lines", 12, 3, 15, 1));

    public CustomScoreboardHud(float x, float y) {
        super(x, y, 150f, 86f);
    }

    @Override
    protected void update(float delta) {
        float frameDelta = RenderUtil.getFrameDelta();
        float s = scale.get().floatValue();
        List<Line> lines = collectLines();
        boolean editing = MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
        boolean visible = !lines.isEmpty() || editing;
        setContentVisibleTarget(visible);

        FontWeight weight = fontWeight();
        float titleW = CustomFontRenderer.getTextWidth(titleText(), 7.0f * s, FontWeight.SEMIBOLD);
        float maxW = titleW;
        if (lines.isEmpty() && editing) {
            maxW = Math.max(maxW, CustomFontRenderer.getTextWidth("No scoreboard", 6.2f * s, weight));
        } else {
            for (Line line : lines) {
                float lineW = CustomFontRenderer.getTextWidth(line.name(), 6.2f * s, weight);
                if (scores.get()) {
                    lineW += 10f * s + CustomFontRenderer.getTextWidth(line.score(), 5.8f * s, FontWeight.REGULAR);
                }
                maxW = Math.max(maxW, lineW);
            }
        }

        float targetW = Math.max(108f * s, maxW + PAD * 2f * s);
        float targetH = (HEADER_H + 6f + Math.max(1, Math.max(lines.size(), editing ? 1 : 0)) * ROW_H) * s;
        if (!visible) {
            targetH = 0f;
        }
        setWidth(RenderUtil.animate(getWidth(), targetW, 0.22f, frameDelta));
        setHeight(RenderUtil.animate(getHeight(), targetH, 0.22f, frameDelta));
    }

    @Override
    protected void renderContent(DrawContext ctx, float x, float y, float w, float h, float delta, float alpha) {
        if (h < 5f) {
            return;
        }

        float s = scale.get().floatValue();
        FontWeight weight = fontWeight();
        drawMenuStyleBackground(ctx, x, y, w, h, 7f * s, alpha);
        RenderUtil.enableScissor(x, y, w, h);

        CustomFontRenderer.drawText(ctx, titleText(), x + PAD * s, y + 5.3f * s,
                7.0f * s, RenderUtil.withAlpha(0xFFFFFF, alpha), FontWeight.SEMIBOLD);
        RenderUtil.drawRect(ctx, x + PAD * s, y + HEADER_H * s - 1f,
                w - PAD * 2f * s, 0.5f, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.14f));

        float rowY = y + (HEADER_H + 4f) * s;
        List<Line> lines = collectLines();
        if (lines.isEmpty()) {
            CustomFontRenderer.drawText(ctx, "No scoreboard", x + PAD * s, rowY + 1.5f * s,
                    6.2f * s, RenderUtil.withAlpha(0xAAAAAA, alpha), weight);
        } else {
            for (Line line : lines) {
                String name = CustomFontRenderer.trimToWidth(line.name(),
                        scores.get() ? w - PAD * 2f * s - 34f * s : w - PAD * 2f * s,
                        6.2f * s, weight);
                CustomFontRenderer.drawText(ctx, name, x + PAD * s, rowY + 1.4f * s,
                        6.2f * s, RenderUtil.withAlpha(0xFFFFFF, alpha * 0.95f), weight);
                if (scores.get()) {
                    CustomFontRenderer.drawRightText(ctx, line.score(), x + w - PAD * s, rowY + 1.4f * s,
                            5.8f * s, RenderUtil.withAlpha(0xB9B9C7, alpha * 0.9f), FontWeight.REGULAR);
                }
                rowY += ROW_H * s;
            }
        }

        RenderUtil.disableScissor();
    }

    private List<Line> collectLines() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return List.of();
        }
        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) {
            return List.of();
        }
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return List.of();
        }

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.removeIf(ScoreboardEntry::hidden);
        entries.sort(Comparator.comparingInt(ScoreboardEntry::value));
        int max = Math.max(1, maxLines.get().intValue());
        if (entries.size() > max) {
            entries = new ArrayList<>(entries.subList(entries.size() - max, entries.size()));
        }

        List<Line> out = new ArrayList<>();
        for (ScoreboardEntry entry : entries) {
            String name = entry.name().getString();
            String score = entry.formatted(objective.getNumberFormatOr(
                    entry.numberFormatOverride() == null ? StyledNumberFormat.EMPTY : entry.numberFormatOverride()
            )).getString();
            if (!name.isBlank()) {
                out.add(new Line(name, score));
            }
        }
        return out;
    }

    private String titleText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return "Scoreboard";
        }
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard == null ? null : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return "Scoreboard";
        }
        String title = objective.getDisplayName().getString();
        return title.isBlank() ? "Scoreboard" : title;
    }

    private FontWeight fontWeight() {
        return switch (font.get()) {
            case "regular" -> FontWeight.REGULAR;
            case "bold" -> FontWeight.BOLD;
            case "light" -> FontWeight.LIGHT;
            default -> FontWeight.SEMIBOLD;
        };
    }

    private record Line(String name, String score) {
    }
}
