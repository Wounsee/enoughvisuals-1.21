package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Register(name = "Streamer Mode", id = "streamer_mode", description = "Скрывает координаты и номер анархии", category = Category.UTILS)
public class StreamerMode extends Module {
    public static StreamerMode INSTANCE;

    private static final String REPLACEMENT_DOMAIN = "enough";
    private static final Pattern ANARCHY_WITH_ID = Pattern.compile("(?iu)(?:анархия|anarchy)\\s*[-—–:#№]?\\s*(\\d{1,4})");
    private static final Pattern ANY_NUMBER = Pattern.compile("\\d+");

    private static final Pattern XYZ_SLASH = Pattern.compile("(?iu)\\bXYZ\\s*:\\s*(-?\\d+(?:[\\.,]\\d+)?)(\\s*/\\s*)(-?\\d+(?:[\\.,]\\d+)?)(\\s*/\\s*)(-?\\d+(?:[\\.,]\\d+)?)");
    private static final Pattern XYZ_SINGLE = Pattern.compile("(?iu)\\b([XYZ])\\s*[:=]\\s*(-?\\d+(?:[\\.,]\\d+)?)");
    private static final Pattern COORD_TRIPLE = Pattern.compile("(?<!\\d)(-?\\d{1,7})(\\s*[,; ]\\s*)(-?\\d{1,4})(\\s*[,; ]\\s*)(-?\\d{1,7})(?!\\d)");
    private static final Pattern COORD_CONTEXT = Pattern.compile("(?iu)(\\bXYZ\\b|\\bcoords?\\b|\\bpos\\b|targeted\\s+(?:block|fluid|entity)|\\bblock\\s*:\\s*-?\\d|коорд|позици|координат)");

    private static volatile String trackedAnarchy = null;

    public StreamerMode() {
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static void captureFromScoreboard(ScoreboardObjective objective) {
        if (!isActive() || objective == null) return;
        captureAnarchyId(objective.getName());
        captureAnarchyId(objective.getDisplayName().getString());
    }

    public static String filter(String input) {
        if (input == null || input.isEmpty()) return input;
        if (!isActive()) return input;
        captureAnarchyId(input);

        String out = replaceAnarchyToken(input);
        String tracked = trackedAnarchy;
        if (tracked != null && !tracked.isEmpty()) {
            out = replaceTrackedNumber(out, tracked);
        }
        return maskCoordinates(out);
    }

    public static Text filter(Text text) {
        if (text == null) return null;
        if (!isActive()) return text;
        String plain = text.getString();
        captureAnarchyId(plain);

        List<StyledChar> chars = fromText(text);
        ReplaceResult replaced = replaceInStyled(chars);
        if (!replaced.changed) return text;
        return toMutableText(replaced.chars);
    }

    public static OrderedText filter(OrderedText orderedText) {
        if (orderedText == null) return null;
        if (!isActive()) return orderedText;

        List<StyledChar> chars = fromOrderedText(orderedText);
        if (chars.isEmpty()) return orderedText;
        String plain = toPlain(chars);
        captureAnarchyId(plain);

        ReplaceResult replaced = replaceInStyled(chars);
        if (!replaced.changed) return orderedText;
        return toMutableText(replaced.chars).asOrderedText();
    }

    private static ReplaceResult replaceInStyled(List<StyledChar> source) {
        if (source.isEmpty()) return new ReplaceResult(source, false);

        String plain = toPlain(source);
        List<Span> spans = new ArrayList<>();
        boolean[] covered = new boolean[plain.length()];

        collectRegexSpans(plain, ANARCHY_WITH_ID, covered, spans, m -> REPLACEMENT_DOMAIN);
        collectRegexSpans(plain, XYZ_SLASH, covered, spans, m -> "XYZ: 0" + m.group(2) + "0" + m.group(4) + "0");
        collectRegexSpans(plain, XYZ_SINGLE, covered, spans, m -> m.group(1) + ": 0");
        if (hasCoordinateContext(plain)) {
            collectRegexSpans(plain, COORD_TRIPLE, covered, spans, m -> "0" + m.group(2) + "0" + m.group(4) + "0");
        }

        String tracked = trackedAnarchy;
        if (tracked != null && !tracked.isEmpty()) {
            Matcher numMatcher = ANY_NUMBER.matcher(plain);
            while (numMatcher.find()) {
                int start = numMatcher.start();
                int end = numMatcher.end();
                if (!tracked.equals(plain.substring(start, end))) continue;
                if (!isNumberBoundary(plain, start, end)) continue;
                Span span = new Span(start, end, "0");
                if (tryCover(covered, span.start, span.end)) {
                    spans.add(span);
                }
            }
        }

        if (spans.isEmpty()) {
            return new ReplaceResult(source, false);
        }

        spans.sort((a, b) -> Integer.compare(a.start, b.start));
        List<StyledChar> out = new ArrayList<>(source.size());

        int index = 0;
        int spanIndex = 0;
        while (index < source.size()) {
            if (spanIndex < spans.size() && spans.get(spanIndex).start == index) {
                Span span = spans.get(spanIndex);
                Style style = source.get(index).style;
                for (int j = 0; j < span.replacement.length(); j++) {
                    out.add(new StyledChar(span.replacement.charAt(j), style));
                }
                index = span.end;
                spanIndex++;
                continue;
            }

            out.add(source.get(index));
            index++;
        }

        return new ReplaceResult(out, true);
    }

    private static String replaceAnarchyToken(String input) {
        return ANARCHY_WITH_ID.matcher(input).replaceAll(REPLACEMENT_DOMAIN);
    }

    private static String replaceTrackedNumber(String input, String tracked) {
        Matcher matcher = ANY_NUMBER.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        int cursor = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String value = input.substring(start, end);
            boolean match = tracked.equals(value) && isNumberBoundary(input, start, end);
            if (!match) continue;

            out.append(input, cursor, start);
            out.append('0');
            cursor = end;
        }
        if (cursor == 0) return input;
        out.append(input, cursor, input.length());
        return out.toString();
    }

    private static String maskCoordinates(String input) {
        String out = replaceRegex(input, XYZ_SLASH, m -> "XYZ: 0" + m.group(2) + "0" + m.group(4) + "0");
        out = replaceRegex(out, XYZ_SINGLE, m -> m.group(1) + ": 0");
        if (hasCoordinateContext(out)) {
            out = replaceRegex(out, COORD_TRIPLE, m -> "0" + m.group(2) + "0" + m.group(4) + "0");
        }
        return out;
    }

    private static boolean hasCoordinateContext(String input) {
        return input != null && COORD_CONTEXT.matcher(input).find();
    }

    private static String replaceRegex(String input, Pattern pattern, Function<Matcher, String> replacementFactory) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer(input.length());
        boolean changed = false;
        while (matcher.find()) {
            String replacement = replacementFactory.apply(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            changed = true;
        }
        if (!changed) return input;
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void collectRegexSpans(String plain,
                                          Pattern pattern,
                                          boolean[] covered,
                                          List<Span> spans,
                                          Function<Matcher, String> replacementFactory) {
        Matcher matcher = pattern.matcher(plain);
        while (matcher.find()) {
            Span span = new Span(matcher.start(), matcher.end(), replacementFactory.apply(matcher));
            if (tryCover(covered, span.start, span.end)) {
                spans.add(span);
            }
        }
    }

    private static void captureAnarchyId(String text) {
        if (text == null || text.isEmpty()) return;
        Matcher matcher = ANARCHY_WITH_ID.matcher(text);
        if (matcher.find()) {
            String id = matcher.group(1);
            if (id != null && !id.isBlank()) {
                trackedAnarchy = id;
            }
        }
    }

    private static boolean tryCover(boolean[] covered, int start, int end) {
        if (start < 0 || end > covered.length || start >= end) return false;
        for (int i = start; i < end; i++) {
            if (covered[i]) return false;
        }
        for (int i = start; i < end; i++) {
            covered[i] = true;
        }
        return true;
    }

    private static boolean isNumberBoundary(String text, int start, int end) {
        if (start < 0 || end > text.length() || start >= end) return false;
        char left = start > 0 ? text.charAt(start - 1) : ' ';
        char right = end < text.length() ? text.charAt(end) : ' ';
        return !Character.isDigit(left) && !Character.isDigit(right);
    }

    private static List<StyledChar> fromText(Text text) {
        List<StyledChar> out = new ArrayList<>();
        text.visit((style, string) -> {
            if (string != null && !string.isEmpty()) {
                for (int i = 0; i < string.length(); i++) {
                    out.add(new StyledChar(string.charAt(i), style));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static List<StyledChar> fromOrderedText(OrderedText orderedText) {
        List<StyledChar> out = new ArrayList<>();
        orderedText.accept((index, style, codePoint) -> {
            char[] chars = Character.toChars(codePoint);
            for (char c : chars) {
                out.add(new StyledChar(c, style));
            }
            return true;
        });
        return out;
    }

    private static String toPlain(List<StyledChar> chars) {
        StringBuilder sb = new StringBuilder(chars.size());
        for (StyledChar ch : chars) {
            sb.append(ch.chr);
        }
        return sb.toString();
    }

    private static MutableText toMutableText(List<StyledChar> chars) {
        MutableText out = Text.empty();
        if (chars.isEmpty()) return out;

        Style currentStyle = chars.get(0).style;
        StringBuilder segment = new StringBuilder();

        for (StyledChar ch : chars) {
            if (!ch.style.equals(currentStyle)) {
                out.append(Text.literal(segment.toString()).setStyle(currentStyle));
                segment.setLength(0);
                currentStyle = ch.style;
            }
            segment.append(ch.chr);
        }

        if (!segment.isEmpty()) {
            out.append(Text.literal(segment.toString()).setStyle(currentStyle));
        }

        return out;
    }

    private record StyledChar(char chr, Style style) {}
    private record Span(int start, int end, String replacement) {}
    private record ReplaceResult(List<StyledChar> chars, boolean changed) {}
}
