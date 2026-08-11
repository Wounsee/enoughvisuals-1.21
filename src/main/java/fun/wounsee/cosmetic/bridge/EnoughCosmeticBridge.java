package fun.wounsee.cosmetic.bridge;

import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.avatar.local.LocalAvatarLoader;
import fun.wounsee.cosmetic.utils.ui.UIHelper;
import fun.wounsee.cosmetic.model.rendering.EntityRenderMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

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

    public static void setPreviewHideVanillaModel(boolean hide) {
        UIHelper.setPreviewHideVanillaModel(hide);
    }

    public static void drawPreviewEntity(Object graphics, Object candidate, float x, float y,
                                         float scale, float pitch, float yaw) {
        drawPreviewEntity(graphics, candidate, x, y, scale, pitch, yaw, false);
    }

    public static void drawPreviewEntity(Object graphics, Object candidate, float x, float y,
                                         float scale, float pitch, float yaw, boolean lockHeadToBody) {
        if (!(graphics instanceof GuiGraphics gui) || !(candidate instanceof LivingEntity entity)) return;
        Vector3f offset = new Vector3f(0f, -(entity.getBbHeight() / 2f + 0.0625f), 0f);
        UIHelper.drawEntity(x, y, scale, pitch, yaw, entity, gui, offset,
                EntityRenderMode.MINECRAFT_GUI, lockHeadToBody);
    }

    public static boolean previewReady(UUID player) {
        var avatar = player == null ? null : AvatarManager.getLoadedAvatar(player);
        return avatar != null && avatar.nbt != null && avatar.renderer != null;
    }

    /** Current EnoughVisuals catalog avatars are full base-body replacements. */
    public static boolean hasRenderReadyAvatar(Object candidate) {
        if (!(candidate instanceof EntityRenderState state)) return false;
        return renderReady(AvatarManager.getAvatar(state));
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

    private static boolean renderReady(fun.wounsee.cosmetic.avatar.Avatar avatar) {
        return avatar != null && avatar.loaded && avatar.nbt != null && avatar.renderer != null;
    }
}
