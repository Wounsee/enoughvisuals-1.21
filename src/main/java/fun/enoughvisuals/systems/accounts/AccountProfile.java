package fun.enoughvisuals.systems.accounts;

import java.util.UUID;

public record AccountProfile(String name, UUID uuid, long createdAt) {
    public static AccountProfile offline(String name) {
        return new AccountProfile(name, AccountSessionManager.offlineUuid(name), System.currentTimeMillis());
    }
}
