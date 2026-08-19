package fun.enoughvisuals.systems.accounts;

import fun.enoughvisuals.mixins.accessors.MinecraftClientAccessor;
import fun.enoughvisuals.systems.network.RemoteConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AccountSessionManager {
    private static Session originalSession;
    private static ProfileKeys originalProfileKeys;

    private AccountSessionManager() {
    }

    public static void switchTo(AccountProfile profile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || profile == null) return;

        captureOriginal(client);
        if (isOriginal(profile)) {
            apply(client, originalSession, originalProfileKeys);
            AccountRepository.saveSelected(profile.name());
            return;
        }

        Session session = new Session(
                profile.name(),
                profile.uuid(),
                "",
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
        );
        apply(client, session, ProfileKeys.MISSING);
        AccountRepository.saveSelected(profile.name());
    }

    public static void restoreLastSelected() {
        String selectedName = AccountRepository.selectedName();
        if (selectedName.isBlank()) return;

        List<AccountProfile> accounts = AccountRepository.load();
        for (AccountProfile account : accounts) {
            if (account.name().equalsIgnoreCase(selectedName)) {
                switchTo(account);
                return;
            }
        }
        AccountRepository.saveSelected(null);
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static void captureOriginal(MinecraftClient client) {
        if (originalSession != null) return;
        originalSession = client.getSession();
        originalProfileKeys = client.getProfileKeys();
    }

    private static boolean isOriginal(AccountProfile profile) {
        return originalSession != null
                && originalSession.getUsername().equalsIgnoreCase(profile.name())
                && originalSession.getUuidOrNull() != null
                && originalSession.getUuidOrNull().equals(profile.uuid());
    }

    private static void apply(MinecraftClient client, Session session, ProfileKeys profileKeys) {
        MinecraftClientAccessor accessor = (MinecraftClientAccessor) client;
        accessor.enough$setSession(session);
        accessor.enough$setProfileKeys(profileKeys == null ? ProfileKeys.MISSING : profileKeys);
        RemoteConfig.getInstance().requestImmediateSync();
    }
}
