package fun.enoughvisuals.utils.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClientLanguage {
    private static final Logger LOGGER = LoggerFactory.getLogger("EnoughVisuals/Language");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FALLBACK_CODE = "ru_ru";
    private static final String TRANSLATION_SOURCE = "en_us";
    private static final String GOOGLE_TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single";
    private static final String BATCH_SEPARATOR = "\uE000\uE001";
    private static final int TRANSLATION_BATCH_SIZE = 96;
    private static final int CACHE_VERSION = 2;
    private static final Pattern PROTECTED_TOKEN = Pattern.compile(
            "%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z%]|§.|https?://\\S+|[.]?[a-z]+\\s+(?:add|remove|load|save)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();
    private static final Map<String, Map<String, String>> LITERAL_CACHE = new HashMap<>();
    private static final ExecutorService TRANSLATION_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EnoughVisuals-GoogleTranslate");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private static final List<LanguageInfo> LANGUAGES = List.of(
            new LanguageInfo("ru_ru", "ru", "Русский", "russia", true),
            new LanguageInfo("uk_ua", "uk", "Українська", "ukraine", true),
            new LanguageInfo("en_us", "en", "English (US)", "united_states", true),
            new LanguageInfo("de_de", "de", "Deutsch", "germany", false),
            new LanguageInfo("fr_fr", "fr", "Français", "france", false),
            new LanguageInfo("es_es", "es", "Español", "spain", false),
            new LanguageInfo("it_it", "it", "Italiano", "italy", false),
            new LanguageInfo("pt_br", "pt", "Português (Brasil)", "brazil", false),
            new LanguageInfo("pl_pl", "pl", "Polski", "poland", false),
            new LanguageInfo("nl_nl", "nl", "Nederlands", "netherlands", false),
            new LanguageInfo("cs_cz", "cs", "Čeština", "czechia", false),
            new LanguageInfo("ro_ro", "ro", "Română", "romania", false),
            new LanguageInfo("hu_hu", "hu", "Magyar", "hungary", false),
            new LanguageInfo("bg_bg", "bg", "Български", "bulgaria", false),
            new LanguageInfo("id_id", "id", "Bahasa Indonesia", "indonesia", false),
            new LanguageInfo("vi_vn", "vi", "Tiếng Việt", "vietnam", false),
            new LanguageInfo("lt_lt", "lt", "Lietuvių", "lithuania", false),
            new LanguageInfo("et_ee", "et", "Eesti", "estonia", false),
            new LanguageInfo("ga_ie", "ga", "Gaeilge", "ireland", false),
            new LanguageInfo("fil_ph", "tl", "Filipino", "philippines", false),
            new LanguageInfo("sv_se", "sv", "Svenska", "sweden", false),
            new LanguageInfo("da_dk", "da", "Dansk", "denmark", false),
            new LanguageInfo("fi_fi", "fi", "Suomi", "finland", false),
            new LanguageInfo("nb_no", "nb", "Norsk", "norway", false)
    );

    private static boolean initialized;
    private static Path languageDir;
    private static Path stateFile;
    private static String selectedCode = "auto";
    private static volatile String generatingCode;
    private static volatile String lastError;
    private static volatile int translationDone;
    private static volatile int translationTotal;

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

    /**
     * Compatibility path for older UI surfaces which still pass a finished
     * Russian or English label instead of a translation key. Exact catalog
     * values are resolved to the active language; user text and unknown values
     * are left untouched.
     */
    public static synchronized String localizeLiteral(String text) {
        if (text == null || text.isEmpty()) return text;
        String active = activeLanguageCode();
        Map<String, String> literals = LITERAL_CACHE.computeIfAbsent(active, ClientLanguage::buildLiteralMap);
        return literals.getOrDefault(text, text);
    }

    private static Map<String, String> buildLiteralMap(String active) {
        Map<String, String> russian = load(FALLBACK_CODE);
        Map<String, String> english = load(TRANSLATION_SOURCE);
        Map<String, String> target = load(active);
        Map<String, String> literals = new HashMap<>();
        for (String key : english.keySet()) {
            String translated = target.get(key);
            if (translated == null && !TRANSLATION_SOURCE.equals(active)) translated = english.get(key);
            if (translated == null || translated.isBlank()) continue;
            String ru = russian.get(key);
            String en = english.get(key);
            if (ru != null && !ru.isBlank()) literals.putIfAbsent(ru, translated);
            if (en != null && !en.isBlank()) literals.putIfAbsent(en, translated);
            literals.putIfAbsent(translated, translated);
        }
        return Collections.unmodifiableMap(literals);
    }

    public static synchronized List<LanguageInfo> availableLanguages() {
        return LANGUAGES;
    }

    public static synchronized String selectedLanguageCode() {
        initialize();
        return selectedCode;
    }

    public static synchronized String activeLanguageCode() {
        initialize();
        return resolveCode(selectedCode);
    }

    public static synchronized String currentLanguageCode() {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            String language = client.getLanguageManager().getLanguage();
            if (language != null && !language.isBlank()) return normalizeCode(language);
        } catch (Throwable ignored) {
        }
        try {
            String language = client.options.language;
            if (language != null && !language.isBlank()) return normalizeCode(language);
        } catch (Throwable ignored) {
        }
        return normalizeCode(Locale.getDefault().toLanguageTag());
    }

    public static CompletableFuture<Boolean> selectLanguage(String code) {
        initialize();
        LanguageInfo info = findLanguage(code);
        if (info == null) return CompletableFuture.completedFuture(false);
        synchronized (ClientLanguage.class) {
            lastError = null;
            selectedCode = info.code;
            if (info.curated || isMachineCacheCurrent(info.code)) {
                CACHE.remove(info.code);
                LITERAL_CACHE.clear();
                writeState();
                return CompletableFuture.completedFuture(true);
            }
            if (generatingCode != null) {
                lastError = "translation_busy";
                return CompletableFuture.completedFuture(false);
            }
            LITERAL_CACHE.clear();
            writeState();
        }
        // Translation runs inside the client process. Only the catalog text is
        // sent to Google once; the completed language is then served from disk.
        return generateMachineLanguage(info);
    }

    public static synchronized void useAutomaticLanguage() {
        initialize();
        selectedCode = "auto";
        LITERAL_CACHE.clear();
        lastError = null;
        writeState();
    }

    public static boolean isGenerating(String code) {
        return code != null && code.equals(generatingCode);
    }

    public static String lastError() {
        return lastError;
    }

    public static float translationProgress() {
        int total = translationTotal;
        return total <= 0 ? 0f : Math.min(1f, translationDone / (float) total);
    }

    private static String translate(String key, String fallback) {
        String value = translateOrNull(key);
        return value == null ? fallback : value;
    }

    private static String translateOrNull(String key) {
        String active = activeLanguageCode();
        String value = load(active).get(key);
        if (value != null) return value;
        if (FALLBACK_CODE.equals(active)) return null;
        if (!TRANSLATION_SOURCE.equals(active)) return load(TRANSLATION_SOURCE).get(key);
        return null;
    }

    private static synchronized void initialize() {
        if (initialized) return;
        Path gameDir;
        try {
            gameDir = MinecraftClient.getInstance().runDirectory.toPath();
        } catch (Throwable ignored) {
            gameDir = FabricLoader.getInstance().getGameDir();
        }
        languageDir = gameDir.resolve("enoughvisuals").resolve("translations");
        stateFile = gameDir.resolve("enoughvisuals").resolve("language.json");
        try {
            Files.createDirectories(languageDir);
            if (Files.exists(stateFile)) {
                try (Reader reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
                    LanguageState state = GSON.fromJson(reader, LanguageState.class);
                    if (state != null && ("auto".equals(state.selected) || findLanguage(state.selected) != null)) {
                        selectedCode = state.selected;
                    }
                }
            }
        } catch (Exception error) {
            LOGGER.warn("Could not initialize language preferences", error);
        }
        initialized = true;
    }

    private static String resolveCode(String requested) {
        if (requested != null && !"auto".equals(requested)) {
            LanguageInfo requestedInfo = findLanguage(requested);
            if (requestedInfo != null) return requestedInfo.code;
            return TRANSLATION_SOURCE;
        }
        String detected = currentLanguageCode();
        String language = detected.contains("_") ? detected.substring(0, detected.indexOf('_')) : detected;
        if ("ru".equals(language)) return "ru_ru";
        if ("uk".equals(language)) return "uk_ua";
        if ("en".equals(language)) return "en_us";
        for (LanguageInfo info : LANGUAGES) {
            if (info.translationCode.equals(language)) return info.code;
        }
        return FALLBACK_CODE;
    }

    private static synchronized Map<String, String> load(String code) {
        return CACHE.computeIfAbsent(code, ClientLanguage::loadFile);
    }

    private static Map<String, String> loadFile(String code) {
        LanguageInfo info = findLanguage(code);
        if (info != null && !info.curated) {
            Map<String, String> cached = loadMachineCache(code);
            if (!cached.isEmpty()) return cached;
        }
        return loadBundled(code);
    }

    private static Map<String, String> loadBundled(String code) {
        Map<String, String> values = new LinkedHashMap<>();
        Path path = FabricLoader.getInstance()
                .getModContainer("enoughvisuals")
                .flatMap(container -> container.findPath("assets/enough/lang/" + code + ".json"))
                .orElse(null);
        if (path == null) return values;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) values.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception error) {
            LOGGER.warn("Could not load bundled language {}", code, error);
        }
        return values;
    }

    private static CompletableFuture<Boolean> generateMachineLanguage(LanguageInfo info) {
        synchronized (ClientLanguage.class) {
            if (generatingCode != null) {
                lastError = "translation_busy";
                return CompletableFuture.completedFuture(false);
            }
            generatingCode = info.code;
            translationDone = 0;
            translationTotal = loadBundled(TRANSLATION_SOURCE).size();
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> source = loadBundled(TRANSLATION_SOURCE);
                LinkedHashMap<String, String> translated = new LinkedHashMap<>();
                List<Map.Entry<String, String>> entries = new ArrayList<>(source.entrySet());
                for (int from = 0; from < entries.size(); from += TRANSLATION_BATCH_SIZE) {
                    int to = Math.min(entries.size(), from + TRANSLATION_BATCH_SIZE);
                    List<String> protectedTexts = new ArrayList<>(to - from);
                    List<List<String>> tokens = new ArrayList<>(to - from);
                    for (int index = from; index < to; index++) {
                        ProtectedText protectedText = protect(entries.get(index).getValue());
                        protectedTexts.add(protectedText.text);
                        tokens.add(protectedText.tokens);
                    }
                    List<String> result = translateBatch(protectedTexts, info.translationCode);
                    if (result.size() != protectedTexts.size()) throw new IllegalStateException("Unexpected batch size");
                    for (int index = 0; index < result.size(); index++) {
                        translated.put(entries.get(from + index).getKey(), restore(result.get(index), tokens.get(index)));
                    }
                    translationDone = to;
                }
                writeMachineCache(info.code, sourceHash(source), translated);
                synchronized (ClientLanguage.class) {
                    CACHE.put(info.code, Collections.unmodifiableMap(translated));
                    LITERAL_CACHE.remove(info.code);
                    lastError = null;
                }
                return true;
            } catch (Exception error) {
                LOGGER.warn("Google translation failed for {}: {}", info.code, error.toString());
                lastError = error.getMessage() == null ? "translation_failed" : error.getMessage();
                return false;
            } finally {
                generatingCode = null;
            }
        }, TRANSLATION_EXECUTOR);
    }

    private static List<String> translateBatch(List<String> texts, String target) throws Exception {
        String joined = String.join("\n" + BATCH_SEPARATOR + "\n", texts);
        String body = "client=gtx&sl=en&tl=" + encode(target)
                + "&dt=t&q=" + encode(joined);
        HttpRequest request = HttpRequest.newBuilder(URI.create(GOOGLE_TRANSLATE_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Google Translate HTTP " + response.statusCode());
        }

        JsonArray root = JsonParser.parseString(response.body()).getAsJsonArray();
        if (root.size() == 0 || !root.get(0).isJsonArray()) {
            throw new IllegalStateException("Unexpected Google Translate response");
        }
        StringBuilder translated = new StringBuilder(joined.length());
        for (JsonElement sentenceElement : root.get(0).getAsJsonArray()) {
            if (!sentenceElement.isJsonArray()) continue;
            JsonArray sentence = sentenceElement.getAsJsonArray();
            if (sentence.size() > 0 && sentence.get(0).isJsonPrimitive()) {
                translated.append(sentence.get(0).getAsString());
            }
        }

        List<String> result = new ArrayList<>();
        String[] parts = translated.toString().split(Pattern.quote(BATCH_SEPARATOR), -1);
        for (String part : parts) {
            if (part.startsWith("\n")) part = part.substring(1);
            if (part.endsWith("\n")) part = part.substring(0, part.length() - 1);
            result.add(part);
        }
        return result;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> loadMachineCache(String code) {
        initialize();
        Path file = languageDir.resolve(code + ".json");
        if (!Files.exists(file)) return Map.of();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            TranslationCache cached = GSON.fromJson(reader, TranslationCache.class);
            Map<String, String> source = loadBundled(TRANSLATION_SOURCE);
            if (cached == null || cached.version != CACHE_VERSION || cached.values == null
                    || !sourceHash(source).equals(cached.sourceHash)) return Map.of();
            return new LinkedHashMap<>(cached.values);
        } catch (Exception error) {
            LOGGER.warn("Could not read cached translation {}", code, error);
            return Map.of();
        }
    }

    private static boolean isMachineCacheCurrent(String code) {
        return !loadMachineCache(code).isEmpty();
    }

    private static void writeMachineCache(String code, String sourceHash, Map<String, String> values) throws Exception {
        TranslationCache cache = new TranslationCache();
        cache.version = CACHE_VERSION;
        cache.sourceHash = sourceHash;
        cache.generatedAt = System.currentTimeMillis();
        cache.values = new LinkedHashMap<>(values);
        writeAtomically(languageDir.resolve(code + ".json"), GSON.toJson(cache));
    }

    private static synchronized void writeState() {
        if (stateFile == null) return;
        try {
            LanguageState state = new LanguageState();
            state.selected = selectedCode;
            writeAtomically(stateFile, GSON.toJson(state));
        } catch (Exception error) {
            LOGGER.warn("Could not save language preference", error);
        }
    }

    private static void writeAtomically(Path target, String contents) throws Exception {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(temporary,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static ProtectedText protect(String source) {
        Matcher matcher = PROTECTED_TOKEN.matcher(source);
        StringBuffer output = new StringBuffer();
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String marker = "__EV" + tokens.size() + "__";
            tokens.add(matcher.group());
            matcher.appendReplacement(output, Matcher.quoteReplacement(marker));
        }
        matcher.appendTail(output);
        return new ProtectedText(output.toString(), tokens);
    }

    private static String restore(String translated, List<String> tokens) {
        String result = translated;
        for (int index = 0; index < tokens.size(); index++) {
            result = result.replace("__EV" + index + "__", tokens.get(index))
                    .replace("__ev" + index + "__", tokens.get(index));
        }
        return result;
    }

    private static String sourceHash(Map<String, String> source) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (Map.Entry<String, String> entry : source.entrySet()) {
            digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static LanguageInfo findLanguage(String code) {
        if (code == null) return null;
        String normalized = normalizeCode(code);
        for (LanguageInfo info : LANGUAGES) if (info.code.equals(normalized)) return info;
        return null;
    }

    private static String normalizeCode(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "empty";
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char chr = Character.toLowerCase(raw.charAt(i));
            if (Character.isLetterOrDigit(chr)) out.append(chr);
            else if (chr == '+' || chr == '&') out.append('_');
            else if (out.length() == 0 || out.charAt(out.length() - 1) != '_') out.append('_');
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '_') out.setLength(out.length() - 1);
        return out.isEmpty() ? "empty" : out.toString();
    }

    public record LanguageInfo(String code, String translationCode, String nativeName, String flagAsset,
                               boolean curated) {
        public String displayName() {
            return nativeName + (curated ? "" : " *");
        }
    }

    private record ProtectedText(String text, List<String> tokens) {
    }

    private static final class LanguageState {
        @SerializedName("selected")
        private String selected = "auto";
    }

    private static final class TranslationCache {
        @SerializedName("version")
        private int version = CACHE_VERSION;
        @SerializedName("sourceHash")
        private String sourceHash = "";
        @SerializedName("generatedAt")
        private long generatedAt;
        @SerializedName("values")
        private Map<String, String> values = new LinkedHashMap<>();
    }
}
