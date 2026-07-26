package fun.enoughvisuals.port;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceUtils {
    private ResourceUtils() {}

    public static String resolve(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream stream = ResourceUtils.class.getClassLoader().getResourceAsStream(normalized)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource: " + path, exception);
        }
    }
}
