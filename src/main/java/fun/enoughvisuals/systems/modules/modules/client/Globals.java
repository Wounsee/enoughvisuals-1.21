package fun.enoughvisuals.systems.modules.modules.client;

import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import fun.enoughvisuals.systems.network.RemoteConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Register(name = "Globals", id = "globals", description = "Shows EnoughVisuals users in nametags and chat", category = Category.CLIENT, enabledByDefault = true)
public class Globals extends Module {
    public static final String LOGO_ICON = "\uF002";
    public static final int LOGO_COLOR = 0xFFC88DFF;
    public static final String LOGO_SPACES = "   ";
    private static final long ALIAS_TTL_MS = 120_000L;
    private static final Pattern PLAYER_TOKEN = Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]{2,15}");
    private static final Map<String, Alias> visibleAliases = new ConcurrentHashMap<>();

    public static Globals INSTANCE;

    public final BooleanSetting nametags = add(new BooleanSetting(
            "Nametags", "nametags", "Shows the logo near player nametags", true
    ));
    public final BooleanSetting chat = add(new BooleanSetting(
            "Chat", "chat", "Shows the logo near names in chat", true
    ));

    public Globals() {
        INSTANCE = this;
        enableSilent();
    }

    public static boolean nametagsEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.nametags.get();
    }

    public static boolean chatEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.chat.get();
    }

    public static boolean isEnoughUser(String username) {
        if (username == null || username.isBlank()) return false;
        if (RemoteConfig.getInstance().isEnoughUser(username)) return true;
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            return mc != null && mc.getSession() != null
                    && username.equalsIgnoreCase(mc.getSession().getUsername());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isEnoughUuid(String uuid) {
        return uuid != null && !uuid.isBlank() && RemoteConfig.getInstance().isEnoughUuid(uuid);
    }

    public static void rememberVisibleAlias(String decoratedLabel, String profileName) {
        rememberVisibleAlias(decoratedLabel, profileName, "");
    }

    public static void rememberVisibleAlias(String decoratedLabel, String profileName, String profileUuid) {
        if (decoratedLabel == null || profileName == null
                || (!isEnoughUser(profileName) && !isEnoughUuid(profileUuid))) return;
        String plain = decoratedLabel.replace(LOGO_SPACES, " ").trim();
        if (plain.isEmpty() || plain.equalsIgnoreCase(profileName)) return;

        Matcher matcher = PLAYER_TOKEN.matcher(plain);
        String candidate = "";
        int bestPrefix = -1;
        while (matcher.find()) {
            String token = matcher.group();
            if (token.equalsIgnoreCase(profileName) || isRankToken(token)) continue;
            int prefix = commonPrefix(token, profileName);
            if (prefix >= bestPrefix) {
                bestPrefix = prefix;
                candidate = token;
            }
        }
        if (candidate.isBlank()) return;
        visibleAliases.put(candidate.toLowerCase(Locale.ROOT),
                new Alias(profileName.toLowerCase(Locale.ROOT), profileUuid,
                        System.currentTimeMillis() + ALIAS_TTL_MS));
    }

    /** Learns server nick aliases from the tab list before a chat line is transformed. */
    public static void refreshVisibleAliasesFromPlayerList() {
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc == null || mc.getNetworkHandler() == null) return;
            for (net.minecraft.client.network.PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry == null || entry.getProfile() == null) continue;
                String name = entry.getProfile().getName();
                String uuid = entry.getProfile().getId().toString();
                if (!isEnoughUser(name) && !isEnoughUuid(uuid)) continue;
                Text display = entry.getDisplayName();
                rememberVisibleAlias(display == null ? name : display.getString(), name, uuid);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Match findEnoughUser(String text) {
        if (text == null || text.isBlank()) return Match.NONE;
        Set<String> users = RemoteConfig.getInstance().getEnoughUsers();
        long now = System.currentTimeMillis();
        visibleAliases.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now
                || (!isEnoughUser(entry.getValue().profileName())
                && !isEnoughUuid(entry.getValue().profileUuid())));
        if ((users == null || users.isEmpty()) && visibleAliases.isEmpty()) return Match.NONE;

        String lowerText = text.toLowerCase(Locale.ROOT);
        Match best = Match.NONE;
        Set<String> candidates = new java.util.HashSet<>();
        if (users != null) candidates.addAll(users);
        candidates.addAll(visibleAliases.keySet());
        for (String user : candidates) {
            if (user == null || user.isBlank()) continue;
            String lowerUser = user.toLowerCase(Locale.ROOT);
            int from = 0;
            while (from < lowerText.length()) {
                int index = lowerText.indexOf(lowerUser, from);
                if (index < 0) break;
                int end = index + lowerUser.length();
                if (hasNameBoundary(text, index, end) && (best == Match.NONE || index < best.start())) {
                    best = new Match(user, index, end);
                    break;
                }
                from = Math.max(index + 1, end);
            }
        }
        return best;
    }

    public static Text withLogoSpace(Text text) {
        if (text == null) return text;
        Match match = findEnoughUser(text.getString());
        if (!match.found()) return text;

        MutableText result = Text.empty();
        int[] cursor = {0};
        boolean[] inserted = {false};
        text.visit((style, segment) -> {
            int segmentStart = cursor[0];
            int segmentEnd = segmentStart + segment.length();
            if (!inserted[0] && match.start() >= segmentStart && match.start() <= segmentEnd) {
                int local = match.start() - segmentStart;
                if (local > 0) result.append(Text.literal(segment.substring(0, local)).setStyle(style));
                result.append(Text.literal(LOGO_SPACES).setStyle(style));
                if (local < segment.length()) result.append(Text.literal(segment.substring(local)).setStyle(style));
                inserted[0] = true;
            } else {
                result.append(Text.literal(segment).setStyle(style));
            }
            cursor[0] = segmentEnd;
            return Optional.empty();
        }, Style.EMPTY);
        return inserted[0] ? result : text;
    }

    private static boolean hasNameBoundary(String text, int start, int end) {
        return isBoundary(text, start - 1) && isBoundary(text, end);
    }

    private static boolean isBoundary(String text, int index) {
        if (index < 0 || index >= text.length()) return true;
        char chr = text.charAt(index);
        return !Character.isLetterOrDigit(chr) && chr != '_';
    }

    private static int commonPrefix(String first, String second) {
        int limit = Math.min(first.length(), second.length());
        int count = 0;
        while (count < limit && Character.toLowerCase(first.charAt(count))
                == Character.toLowerCase(second.charAt(count))) count++;
        return count;
    }

    private static boolean isRankToken(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "player", "admin", "moder", "helper", "owner", "vip", "premium", "legend", "titan" -> true;
            default -> false;
        };
    }

    private record Alias(String profileName, String profileUuid, long expiresAt) {
    }

    public record Match(String username, int start, int end) {
        public static final Match NONE = new Match("", -1, -1);

        public boolean found() {
            return start >= 0 && end > start;
        }
    }
}
