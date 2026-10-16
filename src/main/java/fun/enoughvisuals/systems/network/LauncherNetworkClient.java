package fun.enoughvisuals.systems.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.enoughvisuals.Info;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.InvocationTargetException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Shared access to network operations normally owned by EnoughLauncher.
 *
 * <p>The authenticated loopback bridge injected by the protected loader is
 * preferred. When it is unavailable (a plain built jar, or Loom's
 * {@code runClient}) the client talks to the backend directly and reads the
 * FunTime Events key from local, never-committed sources.
 */
public final class LauncherNetworkClient {
    public static final String FUNTIME_EVENTS_BASE_URL = "https://api.funtimeevents.su/v1";

    private static final URI SYNC_ENDPOINT = URI.create("https://api.enoughvisuals.fun/v1/sync");
    private static final String FUNTIME_KEY_ENV = "ENOUGH_FUNTIME_EVENTS_API_KEY";
    private static final String FUNTIME_KEY_PROPERTY = "enoughvisuals.funtimeEvents.apiKey";
    private static final Pattern FUNTIME_HOST = Pattern.compile(
            "^(?:(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*)funtime\\.(?:su|sh)$",
            Pattern.CASE_INSENSITIVE);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private LauncherNetworkClient() {
    }

    /** Performs the legacy {@code POST /v1/sync} operation. */
    public static String sync(String body) throws Exception {
        try {
            return bridgeRequest(body);
        } catch (Exception bridgeFailure) {
            try {
                return directSync(body);
            } catch (Exception directFailure) {
                directFailure.addSuppressed(bridgeFailure);
                throw directFailure;
            }
        }
    }

    /**
     * Obtains the FunTime Events credential without embedding it in the mod.
     * The launcher session leases it when present; otherwise it comes from an
     * environment variable, a JVM property or {@code enoughvisuals-dev.properties}.
     */
    public static String funtimeEventsApiKey(String serverHost) throws Exception {
        String host = serverHost == null ? "" : serverHost.trim().toLowerCase(Locale.ROOT);
        if (!FUNTIME_HOST.matcher(host).matches()) {
            throw new IllegalArgumentException("FunTime Events lease requires a supported FunTime host");
        }

        try {
            JsonObject request = new JsonObject();
            request.addProperty("bridge_action", "funtime_events_lease");
            request.addProperty("server", host);
            return parseFuntimeKey(bridgeRequest(request.toString()));
        } catch (Exception bridgeFailure) {
            String key = firstNonBlank(
                    System.getenv(FUNTIME_KEY_ENV),
                    firstNonBlank(System.getProperty(FUNTIME_KEY_PROPERTY), readLocalKeyFile()));
            if (key.isBlank()) {
                throw new IllegalStateException(
                        "FunTime Events key is missing; set " + FUNTIME_KEY_ENV
                                + " or -D" + FUNTIME_KEY_PROPERTY + " without committing the value",
                        bridgeFailure);
            }
            return validateFuntimeKey(key);
        }
    }

    private static String bridgeRequest(String body) throws Exception {
        try {
            Class<?> bridge = Class.forName("net.fabricmc.loader.impl.secure.SecureModStore");
            Object response = bridge.getMethod("launcherSync", String.class).invoke(null, body);
            if (!(response instanceof String text) || text.isBlank()) {
                throw new IllegalStateException("launcher network bridge returned an empty response");
            }
            return text;
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("launcher network bridge failed", cause);
        }
    }

    private static String directSync(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(SYNC_ENDPOINT)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("User-Agent", "EnoughVisuals/" + Info.VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("backend sync returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String parseFuntimeKey(String response) {
        JsonObject envelope = JsonParser.parseString(response).getAsJsonObject();
        String key = envelope.has("api_key") && !envelope.get("api_key").isJsonNull()
                ? envelope.get("api_key").getAsString()
                : "";
        return validateFuntimeKey(key);
    }

    private static String validateFuntimeKey(String raw) {
        String key = raw == null ? "" : raw.trim();
        if (!key.startsWith("sk-fte-") || key.length() < 16 || key.length() > 1024) {
            throw new IllegalStateException("FunTime Events credential has an invalid format");
        }
        return key;
    }

    /** Optional git-ignored key file in the game directory; never packaged in the JAR. */
    private static String readLocalKeyFile() {
        try {
            Path path = FabricLoader.getInstance().getGameDir().resolve("enoughvisuals-dev.properties");
            if (!Files.isRegularFile(path)) return "";
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            }
            return properties.getProperty("funtime.events.api-key", "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second == null ? "" : second;
    }
}
