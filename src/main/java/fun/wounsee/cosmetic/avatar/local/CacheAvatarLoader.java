package fun.wounsee.cosmetic.avatar.local;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.UserData;
import fun.wounsee.cosmetic.utils.IOUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheAvatarLoader {

    public static void init() {
        LocalAvatarLoader.async(() -> {
            Path file = getAvatarCacheDirectory();
            if (!(Files.exists(file) && Files.isDirectory(file)))
                return;

            List<Path> children = IOUtils.listPaths(file);
            if (children == null)
                return;

            for (Path child : children) {
                try {
                    FileTime time = Files.getLastModifiedTime(child);
                    long diff = System.currentTimeMillis() - time.toMillis();
                    long elapsed = TimeUnit.MILLISECONDS.toDays(diff);
                    if (elapsed > 7) {
                        if (Files.deleteIfExists(child)) {
                            CosmeticMod.debug("Successfully deleted cache avatar \"{}\" with \"{}\" days old", IOUtils.getFileNameOrEmpty(child), elapsed);
                        } else {
                            throw new Exception();
                        }
                    }
                } catch (Exception ignored) {
                    CosmeticMod.debug("Failed to delete cache avatar \"{}\"", IOUtils.getFileNameOrEmpty(child));
                }
            }
        });
    }

    public static boolean checkAndLoad(String hash, UserData target) {
        Path p = getAvatarCacheDirectory();
        p = p.resolve(hash + ".nbt");

        if (Files.exists(p)) {
            load(hash, target);
            return true;
        }

        return false;
    }

    public static void load(String hash, UserData target) {
        LocalAvatarLoader.async(() -> {
            Path path = getAvatarCacheDirectory().resolve(hash + ".nbt");
            try {
                target.loadAvatar(NbtIo.readCompressed(Files.newInputStream(path), NbtAccounter.unlimitedHeap()));
                CosmeticMod.debug("Loaded avatar \"{}\" from cache to \"{}\"", hash, target.id);
            } catch (Exception e) {
                CosmeticMod.LOGGER.error("Failed to load cache avatar: " + hash, e);
            }
        });
    }

    public static void save(String hash, CompoundTag nbt) {
        LocalAvatarLoader.async(() -> {
            Path file = getAvatarCacheDirectory().resolve(hash + ".nbt");
            try {
                NbtIo.writeCompressed(nbt, Files.newOutputStream(file));
                CosmeticMod.debug("Saved avatar \"{}\" on cache", hash);
            } catch (Exception e) {
                CosmeticMod.LOGGER.error("Failed to save avatar on cache: " + hash, e);
            }
        });
    }

    public static void clearCache() {
        LocalAvatarLoader.async(() -> {
            Path file = getAvatarCacheDirectory();

            if (!(Files.exists(file) && Files.isDirectory(file)))
                return;

            List<Path> children = IOUtils.listPaths(file);
            if (children == null)
                return;

            for (Path child : children) {
                try {
                    if (!Files.deleteIfExists(child))
                        throw new Exception();
                } catch (Exception ignored) {
                    CosmeticMod.debug("Failed to delete cache avatar \"{}\"", IOUtils.getFileNameOrEmpty(child));
                }
            }

            CosmeticMod.debug("Finished clearing avatar cache");
        });
    }

    // cache directory
    public static Path getAvatarCacheDirectory() {
        return IOUtils.getOrCreateDir(CosmeticMod.getCacheDirectory(), "avatars");
    }
}
