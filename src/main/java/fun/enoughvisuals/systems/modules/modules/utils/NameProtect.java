package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Register(name = "Name Protect", id = "name_protect", description = "Скрывает ник", category = Category.UTILS)
public class NameProtect extends Module {
    public static NameProtect INSTANCE;

    public final StringSetting nickname = add(new StringSetting("Ник", "nick", "Замененный ник", "Player", 64, "Ваш ник", StringSetting.InputMode.ANY));

    public NameProtect() {
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static String getMaskedName() {
        if (INSTANCE == null) return "Player";
        String value = INSTANCE.nickname.get();
        if (value == null || value.isBlank()) return "Player";
        return value;
    }

    public static String protect(String text) {
        if (!isActive() && !StreamerMode.isActive()) return text;
        text = StreamerMode.filter(text);
        if (!isActive() || text == null || text.isEmpty()) return text;

        String result = text;
        String replacement = getMaskedName();

        for (String rawName : collectOwnNames()) {
            if (rawName == null || rawName.isBlank()) continue;
            if (rawName.equalsIgnoreCase(replacement)) continue;

            Pattern pattern = Pattern.compile(Pattern.quote(rawName), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            result = pattern.matcher(result).replaceAll(replacement);
        }

        return result;
    }

    public static Text protect(Text text) {
        if (!isActive() && !StreamerMode.isActive()) return text;
        text = StreamerMode.filter(text);
        if (!isActive() || text == null) return text;
        List<StyledChar> chars = fromText(text);
        ReplaceResult replaced = replaceInStyled(chars);
        if (!replaced.changed) return text;
        return toMutableText(replaced.chars);
    }

    public static OrderedText protect(OrderedText orderedText) {
        if (!isActive() && !StreamerMode.isActive()) return orderedText;
        orderedText = StreamerMode.filter(orderedText);
        if (!isActive() || orderedText == null) return orderedText;
        List<StyledChar> chars = fromOrderedText(orderedText);
        ReplaceResult replaced = replaceInStyled(chars);
        if (!replaced.changed) return orderedText;
        return toMutableText(replaced.chars).asOrderedText();
    }

    private static Set<String> collectOwnNames() {
        Set<String> names = new LinkedHashSet<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return names;

        try {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                names.add(mc.getSession().getUsername());
            }
        } catch (Exception ignored) {
        }

        if (mc.player != null) {
            try {
                names.add(mc.player.getName().getString());
            } catch (Exception ignored) {
            }
            try {
                if (mc.player.getGameProfile() != null && mc.player.getGameProfile().getName() != null) {
                    names.add(mc.player.getGameProfile().getName());
                }
            } catch (Exception ignored) {
            }
        }

        Set<String> finalNames = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null) continue;
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                finalNames.add(trimmed);
            }
        }
        return finalNames;
    }

    private static List<StyledChar> fromText(Text text) {
        List<StyledChar> out = new ArrayList<>();
        text.visit((style, string) -> {
            appendStyledString(out, string, style);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static List<StyledChar> fromOrderedText(OrderedText orderedText) {
        List<StyledChar> out = new ArrayList<>();
        orderedText.accept((index, style, codePoint) -> {
            char[] chars = Character.toChars(codePoint);
            for (char chr : chars) {
                out.add(new StyledChar(chr, style));
            }
            return true;
        });
        return out;
    }

    private static void appendStyledString(List<StyledChar> out, String string, Style style) {
        if (string == null || string.isEmpty()) return;
        for (int i = 0; i < string.length(); i++) {
            out.add(new StyledChar(string.charAt(i), style));
        }
    }

    private static ReplaceResult replaceInStyled(List<StyledChar> source) {
        if (source.isEmpty()) return new ReplaceResult(source, false);

        List<String> targets = new ArrayList<>();
        for (String own : collectOwnNames()) {
            if (own == null || own.isBlank()) continue;
            targets.add(own);
        }
        if (targets.isEmpty()) return new ReplaceResult(source, false);

        targets.sort((a, b) -> Integer.compare(b.length(), a.length()));

        List<StyledChar> out = new ArrayList<>(source.size());
        String replacement = getMaskedName();
        boolean changed = false;

        for (int i = 0; i < source.size(); ) {
            String matched = null;
            int matchedLen = 0;

            for (String target : targets) {
                if (target.isEmpty()) continue;
                if (regionMatchesIgnoreCase(source, i, target)) {
                    matched = target;
                    matchedLen = target.length();
                    break;
                }
            }

            if (matched != null) {
                changed = true;
                Style style = source.get(i).style;
                for (int j = 0; j < replacement.length(); j++) {
                    out.add(new StyledChar(replacement.charAt(j), style));
                }
                i += matchedLen;
                continue;
            }

            out.add(source.get(i));
            i++;
        }

        return new ReplaceResult(out, changed);
    }

    private static boolean regionMatchesIgnoreCase(List<StyledChar> source, int offset, String target) {
        if (offset < 0 || target == null) return false;
        if (offset + target.length() > source.size()) return false;
        for (int i = 0; i < target.length(); i++) {
            char a = source.get(offset + i).chr;
            char b = target.charAt(i);
            if (Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
        }
        return true;
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

        if (segment.length() > 0) {
            out.append(Text.literal(segment.toString()).setStyle(currentStyle));
        }

        return out;
    }

    private record StyledChar(char chr, Style style) {}

    private record ReplaceResult(List<StyledChar> chars, boolean changed) {}
}
