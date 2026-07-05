package fun.enoughvisuals.systems.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AccountRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("enoughvisuals")
            .resolve("accounts.json");

    private AccountRepository() {
    }

    public static List<AccountProfile> load() {
        if (!Files.exists(FILE)) {
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(FILE)) {
            AccountData data = GSON.fromJson(reader, AccountData.class);
            if (data == null || data.accounts == null) {
                return new ArrayList<>();
            }

            List<AccountProfile> out = new ArrayList<>();
            for (StoredAccount account : data.accounts) {
                if (account == null || !isValidName(account.name)) continue;
                UUID uuid = account.uuid == null ? AccountSessionManager.offlineUuid(account.name) : account.uuid;
                long createdAt = account.createdAt <= 0L ? System.currentTimeMillis() : account.createdAt;
                out.add(new AccountProfile(account.name, uuid, createdAt));
            }
            out.sort(Comparator.comparingLong(AccountProfile::createdAt));
            return out;
        } catch (IOException | JsonSyntaxException ignored) {
            return new ArrayList<>();
        }
    }

    public static void save(List<AccountProfile> accounts) {
        try {
            Files.createDirectories(FILE.getParent());
            AccountData data = new AccountData();
            data.accounts = new ArrayList<>();
            for (AccountProfile account : accounts) {
                if (account == null || !isValidName(account.name())) continue;
                StoredAccount stored = new StoredAccount();
                stored.name = account.name();
                stored.uuid = account.uuid();
                stored.createdAt = account.createdAt();
                data.accounts.add(stored);
            }
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}");
    }

    public static boolean containsName(List<AccountProfile> accounts, String name) {
        String normalized = normalize(name);
        for (AccountProfile account : accounts) {
            if (normalize(account.name()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class AccountData {
        private List<StoredAccount> accounts = new ArrayList<>();
    }

    private static final class StoredAccount {
        private String name;
        private UUID uuid;
        private long createdAt;
    }
}
