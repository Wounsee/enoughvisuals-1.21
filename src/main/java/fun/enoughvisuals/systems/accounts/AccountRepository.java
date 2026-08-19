package fun.enoughvisuals.systems.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
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
        AccountData data = readData();
        List<AccountProfile> out = new ArrayList<>();
        for (StoredAccount account : data.accounts) {
            if (account == null || !isValidName(account.name)) continue;
            UUID uuid = account.uuid == null ? AccountSessionManager.offlineUuid(account.name) : account.uuid;
            long createdAt = account.createdAt <= 0L ? System.currentTimeMillis() : account.createdAt;
            out.add(new AccountProfile(account.name, uuid, createdAt));
        }
        out.sort(Comparator.comparingLong(AccountProfile::createdAt));
        return out;
    }

    public static synchronized void save(List<AccountProfile> accounts) {
        AccountData data = readData();
        data.accounts = new ArrayList<>();
        for (AccountProfile account : accounts) {
            if (account == null || !isValidName(account.name())) continue;
            StoredAccount stored = new StoredAccount();
            stored.name = account.name();
            stored.uuid = account.uuid();
            stored.createdAt = account.createdAt();
            data.accounts.add(stored);
        }
        writeData(data);
    }

    public static String selectedName() {
        String selected = readData().selectedName;
        return isValidName(selected) ? selected : "";
    }

    public static synchronized void saveSelected(String name) {
        AccountData data = readData();
        data.selectedName = isValidName(name) ? name : null;
        writeData(data);
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

    private static AccountData readData() {
        if (!Files.exists(FILE)) return new AccountData();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            AccountData data = GSON.fromJson(reader, AccountData.class);
            if (data == null) return new AccountData();
            if (data.accounts == null) data.accounts = new ArrayList<>();
            return data;
        } catch (IOException | JsonSyntaxException ignored) {
            return new AccountData();
        }
    }

    private static void writeData(AccountData data) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static final class AccountData {
        @SerializedName("accounts")
        private List<StoredAccount> accounts = new ArrayList<>();
        @SerializedName("selectedName")
        private String selectedName;
    }

    private static final class StoredAccount {
        @SerializedName("name")
        private String name;
        @SerializedName("uuid")
        private UUID uuid;
        @SerializedName("createdAt")
        private long createdAt;
    }
}
