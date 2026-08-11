package fun.enoughvisuals.utils.system;

import fun.enoughvisuals.systems.gps.GpsSystem;
import fun.enoughvisuals.systems.hud.HudNotificationCenter;
import fun.enoughvisuals.utils.sound.EnoughSoundPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FuntimeEventSystem {
    private static final FuntimeEventSystem INSTANCE = new FuntimeEventSystem();

    private static final Pattern ANARCHY_PATTERN = Pattern.compile("анархия\\s*[- ]?\\s*(\\d+)?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern BRACKET_EVENT_PATTERN = Pattern.compile("\\[(.+?)]");
    private static final Pattern COORDS_PATTERN = Pattern.compile("\\[?(-?\\d{1,6})\\s+(-?\\d{1,4})\\s+(-?\\d{1,6})]?");
    private static final Pattern TIME_MIN_SEC_PATTERN = Pattern.compile("(\\d+)\\s*мин\\s*(\\d+)\\s*сек", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TIME_SEC_PATTERN = Pattern.compile("(\\d+)\\s*сек", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LOOT_PATTERN = Pattern.compile("уровень\\s+лута\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final long STATUS_SHOW_MS = 5_000L;
    private static final Set<Integer> REMINDER_MINUTES = Set.of(20, 10, 5, 1);

    private final Map<String, ScopeState> states = new HashMap<>();
    private String activeScope;

    private FuntimeEventSystem() {}

    public static FuntimeEventSystem getInstance() {
        return INSTANCE;
    }

    public void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            activeScope = null;
            return;
        }

        ScopeData scopeData = detectScope(client);
        if (!scopeData.valid()) {
            activeScope = null;
            return;
        }

        activeScope = scopeData.scope();
        ScopeState state = states.computeIfAbsent(activeScope, k -> new ScopeState());

        if (!state.initialDelayRequested) {
            if (client.player.networkHandler != null) {
                client.player.networkHandler.sendChatCommand("event delay");
                state.initialDelayRequested = true;
            }
        }

        if (state.eventStartAtMs <= 0L) return;

        long now = System.currentTimeMillis();
        if (state.eventStartAtMs <= now) return;

        long secondsLeft = (state.eventStartAtMs - now) / 1000L;
        for (int minute : REMINDER_MINUTES) {
            long threshold = minute * 60L;
            if (secondsLeft <= threshold && !state.notifiedMinutes.contains(minute)) {
                state.notifiedMinutes.add(minute);
                HudNotificationCenter.info("До ивента " + formatMinSec(secondsLeft));
                EnoughSoundPlayer.playWarn();
            }
        }
    }

    public void onChatLine(String line) {
        if (line == null || line.isBlank() || activeScope == null) return;

        ScopeState state = states.computeIfAbsent(activeScope, k -> new ScopeState());
        String normalized = normalize(line);
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (shouldIgnore(lower)) return;

        parseHeadingAndLoot(state, normalized, lower);

        if (lower.contains("до следующего ивента")) {
            int sec = parseTimeSeconds(lower);
            if (sec > 0) {
                armCountdown(state, sec);
                HudNotificationCenter.info("До ивента " + formatMinSec(sec));
            }
            return;
        }

        if (lower.contains("начн") && lower.contains("через")) {
            int sec = parseTimeSeconds(lower);
            if (sec > 0) {
                armCountdown(state, sec);
                HudNotificationCenter.info("До ивента " + formatMinSec(sec));
            }
            return;
        }

        if (lower.contains("сундук") && lower.contains("до открытия")) {
            markEventStarted(state, "Ивент идет");
            return;
        }

        if (lower.contains("появил") && lower.contains("координат")) {
            String eventName = (state.pendingEventName == null || state.pendingEventName.isBlank())
                    ? "Ивент" : capitalize(state.pendingEventName);
            String loot = (state.pendingLootType == null || state.pendingLootType.isBlank())
                    ? "Обычный" : capitalize(state.pendingLootType);
            String coords = extractCoords(normalized);

            markEventStarted(state, loot + " " + eventName + " появился на " + coords);
        }
    }

    private static boolean shouldIgnore(String lower) {
        return lower.contains("загадочный маяк")
                || lower.contains("голосование")
                || lower.contains("остров пэпэшнейна");
    }

    private static void parseHeadingAndLoot(ScopeState state, String normalized, String lower) {
        Matcher m = BRACKET_EVENT_PATTERN.matcher(normalized);
        while (m.find()) {
            String candidate = m.group(1).trim();
            if (candidate.isBlank()) continue;
            String cLower = candidate.toLowerCase(Locale.ROOT);
            if (cLower.equals("ивенты") || cLower.equals("голосовать")) continue;
            if (cLower.contains("координат")) continue;
            state.pendingEventName = candidate;
            break;
        }

        Matcher lootMatcher = LOOT_PATTERN.matcher(lower);
        if (lootMatcher.find()) {
            String loot = normalizeLoot(lootMatcher.group(1));
            state.pendingLootType = loot;
            if (loot != null && loot.toLowerCase(Locale.ROOT).contains("элит")) {
                MineStatusFeed.push("Лут: Элитный", STATUS_SHOW_MS);
            }
        }
    }

    private static void armCountdown(ScopeState state, int seconds) {
        state.eventStartAtMs = System.currentTimeMillis() + seconds * 1000L;
        state.notifiedMinutes.clear();
        state.eventStartedAnnounced = false;
    }

    private static void markEventStarted(ScopeState state, String islandText) {
        if (state.eventStartedAnnounced) return;
        state.eventStartedAnnounced = true;
        state.eventStartAtMs = 0L;
        state.notifiedMinutes.clear();

        Matcher m = COORDS_PATTERN.matcher(islandText);
        if (m.find()) {
            try {
                double x = Double.parseDouble(m.group(1));
                double y = Double.parseDouble(m.group(2));
                double z = Double.parseDouble(m.group(3));

                String eventName = (state.pendingEventName == null || state.pendingEventName.isBlank())
                        ? "Ивент" : capitalize(state.pendingEventName);

                                                            
                GpsSystem.getInstance().addWaypoint("Ивент: " + eventName, x, y, z, true);
            } catch (Exception ignored) {}
        }

        HudNotificationCenter.info(islandText);
        EnoughSoundPlayer.playEvent();
    }

    private static int parseTimeSeconds(String lower) {
        Matcher minSec = TIME_MIN_SEC_PATTERN.matcher(lower);
        if (minSec.find()) {
            int min = safeInt(minSec.group(1));
            int sec = safeInt(minSec.group(2));
            if (min >= 0 && sec >= 0) return min * 60 + sec;
        }
        Matcher secOnly = TIME_SEC_PATTERN.matcher(lower);
        if (secOnly.find()) {
            return safeInt(secOnly.group(1));
        }
        return -1;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static String normalizeLoot(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.endsWith("!")) value = value.substring(0, value.length() - 1).trim();
        return value;
    }

    private static String extractCoords(String normalized) {
        Matcher matcher = COORDS_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3);
        }
        return "? ? ?";
    }

    private static String formatMinSec(long seconds) {
        long min = seconds / 60L;
        long sec = seconds % 60L;
        return min + " мин " + sec + " сек";
    }

    private static String normalize(String line) {
        return line.replace("\\n", " ").replace("  ", " ").trim();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private static ScopeData detectScope(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info == null || info.address == null) return ScopeData.invalid();

        String raw = info.address.toLowerCase(Locale.ROOT).trim();
        if (!raw.contains("funtime.su")) return ScopeData.invalid();

        String host = raw;
        int port = 25565;
        int idx = raw.lastIndexOf(':');
        if (idx > 0 && idx < raw.length() - 1) {
            host = raw.substring(0, idx);
            try {
                port = Integer.parseInt(raw.substring(idx + 1));
            } catch (Exception ignored) {
                port = 25565;
            }
        }

        String anarchyPart = "anarchy-unknown";
        String title = getSidebarTitle(client);
        if (title != null) {
            Matcher matcher = ANARCHY_PATTERN.matcher(title.toLowerCase(Locale.ROOT));
            if (matcher.find()) {
                String num = matcher.group(1);
                anarchyPart = (num == null || num.isBlank()) ? "anarchy-unknown" : "anarchy-" + num;
            }
        }

        return new ScopeData(true, host + ":" + port + "|" + anarchyPart);
    }

    private static String getSidebarTitle(MinecraftClient client) {
        try {
            Scoreboard scoreboard = client.world == null ? null : client.world.getScoreboard();
            if (scoreboard == null) return null;
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective == null) return null;
            Text name = objective.getDisplayName();
            return name == null ? null : name.getString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record ScopeData(boolean valid, String scope) {
        static ScopeData invalid() { return new ScopeData(false, null); }
    }

    private static final class ScopeState {
        boolean initialDelayRequested;
        long eventStartAtMs;
        boolean eventStartedAnnounced;
        String pendingEventName;
        String pendingLootType;
        final Set<Integer> notifiedMinutes = new HashSet<>();
    }
}
