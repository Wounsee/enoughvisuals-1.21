package fun.enoughvisuals.systems.friends;

import fun.enoughvisuals.Info;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class FriendRepo {

    private static final Set<String> FRIENDS = new HashSet<>();
    private static Path friendsFile;
    private static long lastModified = Long.MIN_VALUE;
    private static long lastLoadCheckMs = 0L;

    private FriendRepo() {
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        ensureLoaded();
        return FRIENDS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean handleCommand(String message, boolean addToHistory) {
        if (message == null) {
            return false;
        }

        String trimmed = message.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(".friend")) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && addToHistory && client.inGameHud != null) {
            client.inGameHud.getChatHud().addToMessageHistory(message);
        }

        String tail = trimmed.length() > 7 ? trimmed.substring(7).trim() : "";
        if (tail.isEmpty()) {
            sendHelp();
            return true;
        }

        String[] args = tail.split("\\s+");
        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    sendMessage(ClientLanguage.text("chat.friend.usage_add", "Использование: .friend add <ник>"));
                    return true;
                }

                String name = args[1];
                if (addFriend(name)) {
                    sendMessage(String.format(ClientLanguage.text("chat.friend.added", "Друг добавлен: %s"), name));
                } else {
                    sendMessage(String.format(ClientLanguage.text("chat.friend.already", "Этот ник уже в друзьях: %s"), name));
                }
                return true;
            }
            case "remove" -> {
                if (args.length < 2) {
                    sendMessage(ClientLanguage.text("chat.friend.usage_remove", "Использование: .friend remove <ник>"));
                    return true;
                }

                String name = args[1];
                if (removeFriend(name)) {
                    sendMessage(String.format(ClientLanguage.text("chat.friend.removed", "Друг удален: %s"), name));
                } else {
                    sendMessage(String.format(ClientLanguage.text("chat.friend.missing", "Такого друга нет: %s"), name));
                }
                return true;
            }
            case "list" -> {
                List<String> friends = listFriends();
                if (friends.isEmpty()) {
                    sendMessage(ClientLanguage.text("chat.friend.empty", "Список друзей пуст."));
                    return true;
                }

                sendMessage(ClientLanguage.text("chat.friend.list", "Список друзей:"));
                for (String friend : friends) {
                    sendMessage(friend);
                }
                return true;
            }
            default -> {
                sendHelp();
                return true;
            }
        }
    }

    public static boolean addFriend(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return false;
        }

        ensureLoaded();
        if (!FRIENDS.add(normalized)) {
            return false;
        }

        save();
        return true;
    }

    public static boolean removeFriend(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return false;
        }

        ensureLoaded();
        if (!FRIENDS.remove(normalized)) {
            return false;
        }

        save();
        return true;
    }

    public static boolean toggleFriend(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return false;
        }

        ensureLoaded();
        boolean added;
        if (FRIENDS.contains(normalized)) {
            FRIENDS.remove(normalized);
            added = false;
        } else {
            FRIENDS.add(normalized);
            added = true;
        }

        save();
        sendMessage(String.format(ClientLanguage.text(added ? "chat.friend.quick_added" : "chat.friend.quick_removed",
                added ? "Добавлен новый друг: %s" : "Друг удален: %s"), name));
        return added;
    }

    public static List<String> listFriends() {
        ensureLoaded();
        List<String> friends = new ArrayList<>(FRIENDS);
        Collections.sort(friends);
        return friends;
    }

    public static String getSuggestionSuffix(String message) {
        if (message == null) {
            return null;
        }

        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(".friend")) {
            return null;
        }

        String sourcePrefix;
        List<String> source;
        if (lower.startsWith(".friend add")) {
            sourcePrefix = ".friend add";
            source = getOnlinePlayerNames();
        } else if (lower.startsWith(".friend remove")) {
            sourcePrefix = ".friend remove";
            source = listFriends();
        } else {
            return null;
        }

        String afterPrefix = trimmed.length() > sourcePrefix.length()
                ? trimmed.substring(sourcePrefix.length()).trim()
                : "";

        if (!trimmed.endsWith(" ") && afterPrefix.contains(" ")) {
            return null;
        }

        String typedName = afterPrefix;
        String match = source.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typedName.toLowerCase(Locale.ROOT)))
                .filter(name -> !name.equalsIgnoreCase(typedName))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return null;
        }

        return typedName.isEmpty() ? match : match.substring(Math.min(typedName.length(), match.length()));
    }

    private static void ensureLoaded() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return;
            }

            if (friendsFile == null) {
                friendsFile = client.runDirectory.toPath()
                        .resolve("enoughvisuals")
                        .resolve("friends.txt");
            }

            long now = System.currentTimeMillis();
            if (now - lastLoadCheckMs < 1000L && lastModified != Long.MIN_VALUE) {
                return;
            }
            lastLoadCheckMs = now;

            Path parent = friendsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(friendsFile)) {
                Files.writeString(friendsFile, "", StandardCharsets.UTF_8);
                FRIENDS.clear();
                lastModified = Files.getLastModifiedTime(friendsFile).toMillis();
                return;
            }

            long currentModified = Files.getLastModifiedTime(friendsFile).toMillis();
            if (currentModified == lastModified) {
                return;
            }

            List<String> lines = Files.readAllLines(friendsFile, StandardCharsets.UTF_8);
            FRIENDS.clear();
            for (String line : lines) {
                String normalized = normalize(line);
                if (normalized != null) {
                    FRIENDS.add(normalized);
                }
            }

            lastModified = currentModified;
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            if (friendsFile == null) {
                ensureLoaded();
            }

            if (friendsFile == null) {
                return;
            }

            Path parent = friendsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = listFriends();
            try (Writer writer = Files.newBufferedWriter(friendsFile, StandardCharsets.UTF_8)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            }

            lastModified = Files.getLastModifiedTime(friendsFile).toMillis();
        } catch (Exception ignored) {
        }
    }

    private static String normalize(String line) {
        if (line == null) {
            return null;
        }

        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) {
            return null;
        }

        return value.toLowerCase(Locale.ROOT);
    }

    private static void sendHelp() {
        sendMessage(ClientLanguage.text("chat.friend.help", "Команды друзей:"));
        sendMessage(".friend add <ник>");
        sendMessage(".friend remove <ник>");
        sendMessage(".friend list");
    }

    private static void sendMessage(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.inGameHud == null) {
            return;
        }

        MutableText prefix = fun.enoughvisuals.utils.font.TextGradientUtil.gradient(Info.CHAT_PREFIX, 0x6D3BFF, 0xD2D5FF);
        MutableText message = Text.literal(text).setStyle(Style.EMPTY.withColor(0xFFFFFF));
        client.inGameHud.getChatHud().addMessage(prefix.append(message));
    }

    private static List<String> getOnlinePlayerNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return Collections.emptyList();
        }

        return client.getNetworkHandler().getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(profile -> profile.getName())
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> client.player == null || !name.equalsIgnoreCase(client.player.getName().getString()))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }
}
