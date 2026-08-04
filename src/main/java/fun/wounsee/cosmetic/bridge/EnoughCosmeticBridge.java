package fun.wounsee.cosmetic.bridge;

import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.avatar.local.LocalAvatarLoader;
import fun.wounsee.cosmetic.utils.ui.UIHelper;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Stable API used by EnoughVisuals without exposing the original Figura UI. */
public final class EnoughCosmeticBridge {
    private EnoughCosmeticBridge() {}

    public static Path avatarsDirectory() {
        return CosmeticMod.getFiguraDirectory().resolve("avatars");
    }

    public static Path equippedAvatar() {
        return LocalAvatarLoader.getLastLoadedPath();
    }

    public static void equip(Path avatar) {
        AvatarManager.loadLocalAvatar(avatar == null ? null : avatar.toAbsolutePath().normalize());
    }

    public static void equipComposite(List<Path> avatars) {
        AvatarManager.loadLocalAvatars(normalize(avatars));
    }

    public static void equipRemote(UUID player, List<Path> avatars) {
        if (player != null && !CosmeticMod.isLocal(player)) {
            AvatarManager.loadRemoteAvatars(player, normalize(avatars));
        }
    }

    public static void equipPreview(UUID player, List<Path> avatars) {
        if (player != null && !CosmeticMod.isLocal(player)) {
            AvatarManager.loadPreviewAvatars(player, normalize(avatars));
        }
    }

    public static void bindPreviewEntity(UUID player, Object entity) {
        AvatarManager.bindPreviewEntity(player, entity);
    }

    public static void unbindPreviewEntity(UUID player, Object entity) {
        AvatarManager.unbindPreviewEntity(player, entity);
    }

    public static void setPreviewAlpha(float alpha) {
        UIHelper.setPreviewAlpha(alpha);
    }

    public static boolean previewReady(UUID player) {
        var avatar = player == null ? null : AvatarManager.getLoadedAvatar(player);
        return avatar != null && avatar.nbt != null && avatar.renderer != null;
    }

    public static void clearRemote(UUID player) {
        if (player != null && !CosmeticMod.isLocal(player)) AvatarManager.clearAvatars(player);
    }

    public static void reload() {
        AvatarManager.reloadAvatar(CosmeticMod.getLocalPlayerUUID());
    }

    public static void unequip() {
        AvatarManager.clearAvatars(CosmeticMod.getLocalPlayerUUID());
        LocalAvatarLoader.resetWatchKeys();
    }

    private static List<Path> normalize(List<Path> avatars) {
        if (avatars == null) return List.of();
        return avatars.stream().filter(java.util.Objects::nonNull)
                .map(path -> path.toAbsolutePath().normalize()).toList();
    }
}
