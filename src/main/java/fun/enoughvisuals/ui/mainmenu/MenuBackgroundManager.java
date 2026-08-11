package fun.enoughvisuals.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.enoughvisuals.utils.render.RenderUtil;
import fun.enoughvisuals.utils.system.ClientLanguage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/** Downloads menu backgrounds on demand and keeps them outside the mod jar. */
public final class MenuBackgroundManager {
    public static final Identifier DEFAULT_TEXTURE = Identifier.of("enough", "textures/mainmenu/bg.png");
    public static final Entry DEFAULT = new Entry("default", "Текущий фон", null);
    public static final List<Entry> ENTRIES = List.of(
            DEFAULT,
            entry("ancient-blue-hall"), entry("astral-forest"), entry("autumn-lake"),
            entry("blossom-city"), entry("blue-cavern"), entry("castle-garden"),
            entry("cave-lanterns"), entry("cavern-wheel"), entry("chain-cavern"),
            entry("cherry-village"), entry("crimson-arcade"), entry("crystal-tree"),
            entry("deep-cavern"), entry("ember-cavern"), entry("enchanted-lake"),
            entry("fire-temple"), entry("floating-islands"), entry("glowing-cavern"),
            entry("golden-street"), entry("lantern-gate"), entry("launcher-blue-cavern"),
            entry("market-arcade"), entry("modern-villa-day"), entry("modern-villa-rain"),
            entry("modern-villa-sunrise"), entry("moonlit-pavilion"), entry("mountain-lake"),
            entry("nether-portal-hall"), entry("riverside-house"), entry("sunrise-pagoda-lake"),
            entry("sunrise-pagoda"), entry("sunset-fantasy-village"), entry("sunset-ruins"),
            entry("village-workshop"), entry("white-city"), entry("winter-pagoda")
    );

    private static final String RAW_ROOT = "https://raw.githubusercontent.com/Wounsee/storage/main/backgrounds/";
    private static final long MAX_DOWNLOAD_BYTES = 12L * 1024L * 1024L;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<String, Boolean> CACHED = new ConcurrentHashMap<>();

    private static Entry selected = DEFAULT;
    private static Identifier texture = DEFAULT_TEXTURE;
    private static int textureWidth = 1920;
    private static int textureHeight = 1080;
    private static volatile float progress;
    private static volatile String error;
    private static volatile boolean loading;
    private static volatile Entry pending;
    private static boolean initialized;

    private MenuBackgroundManager() {}

    public static void initialize(MinecraftClient client) {
        if (initialized) return;
        initialized = true;
        String id = readSelection(client);
        ENTRIES.stream().filter(e -> e.id().equals(id)).findFirst().ifPresent(e -> select(client, e));
    }

    public static void select(MinecraftClient client, Entry entry) {
        if (loading || client == null) return;
        error = null;
        if (entry == DEFAULT) {
            selected = DEFAULT;
            texture = DEFAULT_TEXTURE;
            textureWidth = 1920;
            textureHeight = 1080;
            saveSelection(client, DEFAULT.id());
            return;
        }

        loading = true;
        pending = entry;
        progress = 0.04f;
        CompletableFuture.runAsync(() -> {
            try {
                Path file = cacheDir(client).resolve(entry.id() + ".png");
                if (!Files.isRegularFile(file) || Files.size(file) == 0L) download(entry, file);
                progress = 0.92f;
                try (InputStream input = Files.newInputStream(file)) {
                    NativeImage image = NativeImage.read(input);
                    client.execute(() -> register(client, entry, image));
                }
            } catch (Exception ex) {
                loading = false;
                pending = null;
                progress = 0f;
                error = ClientLanguage.text("ui.background.download_failed", "Не удалось загрузить фон");
            }
        });
    }

    private static void download(Entry entry, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        Files.deleteIfExists(temporary);
        HttpRequest request = HttpRequest.newBuilder(URI.create(entry.url()))
                .timeout(Duration.ofSeconds(45)).header("User-Agent", "EnoughVisuals").GET().build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
        long expected = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (expected > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("Background is too large");
        try (InputStream in = response.body(); var out = Files.newOutputStream(temporary)) {
            byte[] buffer = new byte[32 * 1024];
            long received = 0L;
            for (int read; (read = in.read(buffer)) >= 0;) {
                received += read;
                if (received > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("Background is too large");
                out.write(buffer, 0, read);
                progress = expected > 0 ? Math.min(0.88f, 0.08f + received / (float) expected * 0.80f) : 0.35f;
            }
        } catch (Exception ex) {
            Files.deleteIfExists(temporary);
            throw ex;
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        CACHED.put(entry.id(), true);
    }

    private static void register(MinecraftClient client, Entry entry, NativeImage image) {
        try {
            Identifier id = Identifier.of("enough", "mainmenu/remote_" + entry.id().replace('-', '_'));
            NativeImageBackedTexture backed = new NativeImageBackedTexture(image);
            backed.setFilter(true, true);
            client.getTextureManager().registerTexture(id, backed);
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            texture = id;
            selected = entry;
            saveSelection(client, entry.id());
            progress = 1f;
        } catch (Exception ex) {
            image.close();
            error = ClientLanguage.text("ui.background.open_failed", "Не удалось открыть фон");
        } finally {
            loading = false;
            pending = null;
        }
    }

    private static Path cacheDir(MinecraftClient client) {
        return client.runDirectory.toPath().resolve("enoughvisuals").resolve("backgrounds");
    }

    private static Path settingsFile(MinecraftClient client) {
        return cacheDir(client).resolve("selection.properties");
    }

    private static String readSelection(MinecraftClient client) {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(settingsFile(client))) {
            properties.load(in);
            return properties.getProperty("selected", DEFAULT.id());
        } catch (Exception ignored) {
            return DEFAULT.id();
        }
    }

    private static void saveSelection(MinecraftClient client, String id) {
        try {
            Files.createDirectories(cacheDir(client));
            Properties properties = new Properties();
            properties.setProperty("selected", id);
            try (var out = Files.newOutputStream(settingsFile(client))) {
                properties.store(out, "Enough Visuals main menu");
            }
        } catch (Exception ignored) {}
    }

    private static Entry entry(String id) {
        String[] words = id.split("-");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (!title.isEmpty()) title.append(' ');
            title.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return new Entry(id, title.toString(), RAW_ROOT + id + ".png");
    }

    public static Entry selected() { return selected; }
    public static Identifier texture() { return texture; }
    public static int textureWidth() { return textureWidth; }
    public static int textureHeight() { return textureHeight; }
    public static boolean loading() { return loading; }
    public static float progress() { return progress; }
    public static Entry pending() { return pending; }
    public static String error() { return error; }

    /** Draws the same selected gallery image as the main menu, cropped to cover the screen. */
    public static void drawSelectedBackground(DrawContext context, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier background = texture;
        try {
            AbstractTexture loaded = client.getTextureManager().getTexture(background);
            if (loaded != null) loaded.setFilter(true, true);
        } catch (Throwable ignored) {
        }

        int imageWidth = Math.max(1, textureWidth);
        int imageHeight = Math.max(1, textureHeight);
        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = imageWidth / (float) imageHeight;
        float sourceX = 0f;
        float sourceY = 0f;
        int sourceWidth = imageWidth;
        int sourceHeight = imageHeight;
        if (screenAspect > textureAspect) {
            sourceHeight = Math.round(imageWidth / screenAspect);
            sourceY = (imageHeight - sourceHeight) * 0.5f;
        } else if (screenAspect < textureAspect) {
            sourceWidth = Math.round(imageHeight * screenAspect);
            sourceX = (imageWidth - sourceWidth) * 0.5f;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.drawTexture(RenderLayer::getGuiTextured, background, 0, 0, sourceX, sourceY,
                width, height, sourceWidth, sourceHeight, imageWidth, imageHeight);
        context.draw();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderUtil.drawRect(context, 0f, 0f, width, height, 0x4A000000);
        RenderUtil.drawGradientRect(context, 0f, 0f, width, height * 0.30f,
                0x5C000000, 0x00000000);
        RenderUtil.drawGradientRect(context, 0f, height * 0.66f, width, height * 0.34f,
                0x00000000, 0x6A000000);
    }

    public static boolean isCached(MinecraftClient client, Entry entry) {
        return entry == DEFAULT || CACHED.computeIfAbsent(entry.id(), id ->
                Files.isRegularFile(cacheDir(client).resolve(id + ".png")));
    }

    public record Entry(String id, String title, String url) {
        public String displayTitle() {
            if ("default".equals(id)) return ClientLanguage.text("ui.background.current", "Текущий фон");
            return ClientLanguage.text("ui.background.entry." + id, title);
        }
    }
}
