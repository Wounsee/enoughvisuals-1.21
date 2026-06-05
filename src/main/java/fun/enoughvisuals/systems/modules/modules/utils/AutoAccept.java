package fun.enoughvisuals.systems.modules.modules.utils;

import fun.enoughvisuals.systems.friends.FriendRepo;
import fun.enoughvisuals.systems.modules.Module;
import fun.enoughvisuals.systems.modules.api.Category;
import fun.enoughvisuals.systems.modules.api.Register;
import fun.enoughvisuals.systems.modules.settings.BooleanSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Register(name = "Auto Accept", id = "auto_accept", description = "Автоматически принимает телепорты и клан-инвайты", category = Category.UTILS)
public class AutoAccept extends Module {

    private static final Pattern NICK_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,16})(?![A-Za-z0-9_])");
    private static boolean messageEventsRegistered = false;

    private final BooleanSetting onlyFriends = add(new BooleanSetting(
            "Только друзья",
            "only_friends",
            "Принимать запросы только от игроков из .friend",
            true
    ));

    private String lastInviter;

    public AutoAccept() {
        if (!messageEventsRegistered) {
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleIncomingMessage(message));
            ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handleIncomingMessage(message));
            messageEventsRegistered = true;
        }
    }

    private void handleIncomingMessage(Text message) {
        if (!isEnabled() || mc.player == null || mc.world == null || message == null) {
            return;
        }

        String plain = message.getString();
        if (plain == null || plain.isBlank()) {
            return;
        }

        if (isTeleportInvite(plain)) {
            String inviter = solveName(plain);
            if (canAcceptFrom(inviter)) {
                acceptTeleport();
            }
            return;
        }

        if (isClanInviteSource(plain)) {
            lastInviter = solveName(plain);
            if (!canAcceptFrom(lastInviter)) {
                lastInviter = null;
            }
            return;
        }

        if (lastInviter != null && containsClanButtons(plain) && canAcceptFrom(lastInviter)) {
            clickClanAcceptButton(message);
            lastInviter = null;
        }
    }

    private boolean canAcceptFrom(String inviter) {
        if (!onlyFriends.get()) {
            return true;
        }

        return inviter != null && !inviter.isBlank() && FriendRepo.isFriend(inviter);
    }

    private boolean isTeleportInvite(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("/tpaccept")
                || lower.contains("tpaccept")
                || lower.contains("телепорт")
                || lower.contains("teleport")
                || lower.contains("tpa");
    }

    private boolean isClanInviteSource(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("приглашает вас в клан")
                || lower.contains("пригласил вас в клан")
                || lower.contains("invites you to join the clan")
                || lower.contains("clan invite");
    }

    private boolean containsClanButtons(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("вступить")
                || lower.contains("принять")
                || lower.contains("accept");
    }

    private void acceptTeleport() {
        mc.player.networkHandler.sendChatCommand("tpaccept");
    }

    private void clickClanAcceptButton(Text text) {
        for (Text component : getTextComponents(text)) {
            Style style = component.getStyle();
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent == null) {
                continue;
            }

            String content = component.getString().toLowerCase(Locale.ROOT);
            if (!content.contains("вступить") && !content.contains("принять") && !content.contains("accept")) {
                continue;
            }

            if (clickEvent.getAction() != ClickEvent.Action.RUN_COMMAND
                    && clickEvent.getAction() != ClickEvent.Action.SUGGEST_COMMAND) {
                continue;
            }

            String command = clickEvent.getValue();
            if (command == null || command.isBlank()) {
                continue;
            }

            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            mc.player.networkHandler.sendChatCommand(command);
            return;
        }
    }

    private List<Text> getTextComponents(Text text) {
        List<Text> components = new ArrayList<>();
        components.add(text);
        for (Text sibling : text.getSiblings()) {
            components.addAll(getTextComponents(sibling));
        }
        return components;
    }

    private String solveName(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }

        String lower = rawMessage.toLowerCase(Locale.ROOT);
        if (mc.getNetworkHandler() != null) {
            for (var entry : mc.getNetworkHandler().getPlayerList()) {
                String playerName = entry.getProfile().getName();
                if (playerName != null && lower.contains(playerName.toLowerCase(Locale.ROOT))) {
                    return playerName;
                }
            }
        }

        Matcher matcher = NICK_PATTERN.matcher(rawMessage);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            String candidateLower = candidate.toLowerCase(Locale.ROOT);
            if (!candidateLower.equals("tpaccept")
                    && !candidateLower.equals("accept")
                    && !candidateLower.equals("clan")
                    && !candidateLower.equals("tpa")) {
                return candidate;
            }
        }

        return "";
    }
}
