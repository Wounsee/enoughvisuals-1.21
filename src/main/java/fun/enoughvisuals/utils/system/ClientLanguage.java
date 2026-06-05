package fun.enoughvisuals.utils.system;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ClientLanguage {
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private ClientLanguage() {
    }

    public static String moduleName(String moduleId, String fallback) {
        return translate("modules." + moduleId + ".name", fallback);
    }

    public static String moduleDescription(String moduleId, String fallback) {
        return translate("modules." + moduleId + ".description", fallback);
    }

    public static String categoryName(String categoryId, String fallback) {
        return translate("categories." + categoryId + ".name", fallback);
    }

    public static String categoryDescription(String categoryId, String fallback) {
        return translate("categories." + categoryId + ".description", fallback);
    }

    public static String settingName(String moduleId, String settingId, String fallback) {
        if (moduleId != null && !moduleId.isBlank()) {
            String direct = translateOrNull("settings." + moduleId + "." + settingId + ".name");
            if (direct != null) return direct;
        }
        return translate("settings.common." + settingId + ".name", fallback);
    }

    public static String settingDescription(String moduleId, String settingId, String fallback) {
        if (moduleId != null && !moduleId.isBlank()) {
            String direct = translateOrNull("settings." + moduleId + "." + settingId + ".description");
            if (direct != null) return direct;
        }
        return translate("settings.common." + settingId + ".description", fallback);
    }

    public static String modeValue(String moduleId, String settingId, String rawValue) {
        String normalized = normalize(rawValue);
        if (moduleId != null && !moduleId.isBlank()) {
            String direct = translateOrNull("settings." + moduleId + "." + settingId + ".values." + normalized);
            if (direct != null) return direct;
        }
        String common = translateOrNull("values." + normalized);
        return common == null ? rawValue : common;
    }

    public static String text(String key, String fallback) {
        return translate(key, fallback);
    }

    public static String currentLanguageCode() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            String language = client.getLanguageManager().getLanguage();
            if (language != null && !language.isBlank()) return language.toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
        }
        try {
            String language = client.options.language;
            if (language != null && !language.isBlank()) return language.toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
        }
        return Locale.getDefault().toLanguageTag().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String translate(String key, String fallback) {
        String value = translateOrNull(key);
        return value == null ? fallback : value;
    }

    private static String translateOrNull(String key) {
        Map<String, String> language = load(activeFileCode());
        String value = language.get(key);
        if (value != null) return value;

        if (!"en_en".equals(activeFileCode())) {
            value = load("en_en").get(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String activeFileCode() {
        String code = currentLanguageCode();
        return code.startsWith("ru") ? "ru_ru" : "en_en";
    }

    private static Map<String, String> load(String code) {
        return CACHE.computeIfAbsent(code, ClientLanguage::loadFile);
    }

    private static Map<String, String> loadFile(String code) {
        Map<String, String> values = new HashMap<>();
        Path path = FabricLoader.getInstance()
                .getModContainer("enoughvisuals")
                .flatMap(container -> container.findPath("assets/enough/lang/" + code + ".json"))
                .orElse(null);
        if (path == null && "en_en".equals(code)) {
            path = FabricLoader.getInstance()
                    .getModContainer("enoughvisuals")
                    .flatMap(container -> container.findPath("assets/enough/lang/en_us.json"))
                    .orElse(null);
        }
        if (path == null) return values;

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    values.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return new HashMap<>();
        }
        return values;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "empty";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char chr = Character.toLowerCase(raw.charAt(i));
            if (Character.isLetterOrDigit(chr)) {
                out.append(chr);
            } else if (chr == '+' || chr == '&') {
                out.append('_');
            } else if (out.length() == 0 || out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.setLength(out.length() - 1);
        }
        return out.isEmpty() ? "empty" : out.toString();
    }
}
