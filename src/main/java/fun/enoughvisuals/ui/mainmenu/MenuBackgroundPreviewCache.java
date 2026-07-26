package fun.enoughvisuals.ui.mainmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Small, lazy thumbnails. Full-resolution files are never fetched here. */
public final class MenuBackgroundPreviewCache {
    public static final int WIDTH = 360;
    public static final int HEIGHT = 203;
    private static final int MAX_BYTES = 512 * 1024;
    private static final String RESIZER = "https://wsrv.nl/?w=" + WIDTH + "&h=" + HEIGHT
            + "&fit=cover&output=png&q=78&url=";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<MenuBackgroundManager.Entry, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Set<MenuBackgroundManager.Entry> REQUESTED = ConcurrentHashMap.newKeySet();
    private static final Map<MenuBackgroundManager.Entry, Long> RETRY_AFTER = new ConcurrentHashMap<>();

    private MenuBackgroundPreviewCache() {}

    public static Identifier texture(MenuBackgroundManager.Entry entry) {
        if (entry == MenuBackgroundManager.DEFAULT) return MenuBackgroundManager.DEFAULT_TEXTURE;
        return TEXTURES.get(entry);
    }

    public static void request(MinecraftClient client, MenuBackgroundManager.Entry entry) {
        if (client == null || entry == MenuBackgroundManager.DEFAULT) return;
        if (System.currentTimeMillis() < RETRY_AFTER.getOrDefault(entry, 0L) || !REQUESTED.add(entry)) return;
        CompletableFuture.runAsync(() -> load(client, entry));
    }

    private static void load(MinecraftClient client, MenuBackgroundManager.Entry entry) {
        try {
            Path cache = client.runDirectory.toPath().resolve("enoughvisuals")
                    .resolve("backgrounds").resolve("previews").resolve(entry.id() + ".png");
            byte[] bytes;
            if (Files.isRegularFile(cache) && Files.size(cache) > 0 && Files.size(cache) <= MAX_BYTES) {
                bytes = Files.readAllBytes(cache);
            } else {
                String source = URLEncoder.encode(entry.url(), StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder(URI.create(RESIZER + source))
                        .timeout(Duration.ofSeconds(20)).header("User-Agent", "EnoughVisuals").GET().build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2 || response.body().length == 0
                        || response.body().length > MAX_BYTES) return;
                bytes = response.body();
                Files.createDirectories(cache.getParent());
                Files.write(cache, bytes);
            }
            NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(bytes));
            client.execute(() -> register(client, entry, image));
        } catch (Exception ignored) {
            REQUESTED.remove(entry);
            RETRY_AFTER.put(entry, System.currentTimeMillis() + 15_000L);
        }
    }

    private static void register(MinecraftClient client, MenuBackgroundManager.Entry entry, NativeImage image) {
        try {
            Identifier id = Identifier.of("enough", "mainmenu/preview_" + entry.id().replace('-', '_'));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            texture.setFilter(true, true);
            client.getTextureManager().registerTexture(id, texture);
            TEXTURES.put(entry, id);
            RETRY_AFTER.remove(entry);
        } catch (Exception ex) {
            image.close();
        }
    }
}
