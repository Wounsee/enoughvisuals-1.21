package fun.wounsee.cosmetic.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.backend2.NetworkStuff;
import fun.wounsee.cosmetic.config.Configs;
import fun.wounsee.cosmetic.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FiguraRuntimeResources {

    private static String ASSETS_VERSION;
    public static final PathPackResources PACK = new PathPackResources(new PackLocationInfo(CosmeticMod.MOD_NAME + " runtime resource pack", Component.literal("Figura Runtime Resources"), PackSource.BUILT_IN, Optional.empty()), getRootDirectory());

    public static Path getRootDirectory() {
        return IOUtils.getOrCreateDir(CosmeticMod.getCacheDirectory(), "resources");
    }

    public static Path getAssetsDirectory() {
        return IOUtils.getOrCreateDir(getRootDirectory(), "assets/" + CosmeticMod.MOD_ID);
    }

    public static String getAssetsVersion() {
        if (ASSETS_VERSION == null)
             ASSETS_VERSION = CosmeticMod.METADATA.getCustomValueAsString("assets_version");
        return ASSETS_VERSION;
    }

    private static CompletableFuture<Void> future;

    public static void clearCache() {
        IOUtils.deleteFile(getRootDirectory());
    }

    public static CompletableFuture<Void> init() {
        return future = CompletableFuture.runAsync(() -> {
            CosmeticMod.LOGGER.info("Fetching backend resources...");

            JsonObject hashes, oldHashes;

            // get old hashes
            Path hashesPath = getRootDirectory().resolve("hashes.json");
            try (BufferedReader reader = Files.newBufferedReader(hashesPath)) {
                oldHashes = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception ignored) {
                oldHashes = new JsonObject();
            }

            // get new hashes
            try (InputStream stream = NetworkStuff.getResourcesHashes(getAssetsVersion())) {
                byte[] bytes = stream.readAllBytes();
                String s = new String(bytes);
                hashes = JsonParser.parseString(s).getAsJsonObject();

                // save new hashes
                try (OutputStream fs = Files.newOutputStream(hashesPath)) {
                    fs.write(bytes);
                } catch (Exception e) {
                    CosmeticMod.LOGGER.error("Failed to save resource hashes", e);
                }
            } catch (Exception ignored) {
                CosmeticMod.LOGGER.warn("Failed to fetch backend resources");
                return;
            }

            // compare hashes
            for (Map.Entry<String, JsonElement> entry : hashes.entrySet()) {
                String key = entry.getKey();
                JsonElement oldHash = oldHashes.get(key);
                try {
                    if (oldHash == null || !oldHash.getAsString().equals(entry.getValue().getAsString())) {
                        getAndSaveResource(key);
                    }
                } catch (Exception e) {
                    CosmeticMod.debug("Failed to download resource \"" + key + "\"", e);
                }
            }
            File assetsDirectoryFile = getAssetsDirectory().toFile();
            JsonObject directoryJsonObject = constructAssetsDirectoryJsonObject(assetsDirectoryFile.listFiles());
            for (Map.Entry<String, JsonElement> entry : directoryJsonObject.entrySet()) {
                if (!hashes.has(entry.getKey()) && !Configs.LOCAL_ASSETS.value) {
                    IOUtils.deleteFile(getAssetsDirectory().resolve(entry.getKey()));
                }
            }
        });
    }

    public static JsonObject constructAssetsDirectoryJsonObject(File[] files) {
        JsonObject object = new JsonObject();
        for (File file : files) {
            if (file.isDirectory()) {
                for (Map.Entry<String, JsonElement> entry : constructAssetsDirectoryJsonObject(file.listFiles()).entrySet()) {
                    object.addProperty(entry.getKey(), entry.getValue().getAsString());
                }
            } else if (!file.isHidden()){
                object.addProperty(getAssetsDirectory().toUri().relativize(file.toURI()).getPath(), "0");
            }
        }
        return object;
    }
    private static void getAndSaveResource(String path) throws Exception {
        if (Configs.LOCAL_ASSETS.value) return;
        Path target = getAssetsDirectory().resolve(path);
        IOUtils.createDirIfNeeded(target.getParent());
        try (InputStream resource = NetworkStuff.getResource(getAssetsVersion(), path); OutputStream fs = Files.newOutputStream(target)) {
            fs.write(resource.readAllBytes());
            CosmeticMod.debug("Downloaded resource \"" + path + "\"");
        }
    }

    public static void joinFuture() {
        if (future != null && !future.isDone())
            future.join();
    }
}
