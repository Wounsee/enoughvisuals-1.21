package fun.wounsee.cosmetic.avatar;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.local.LocalAvatarLoader;
import fun.wounsee.cosmetic.backend2.NetworkStuff;
import fun.wounsee.cosmetic.ducks.FiguraEntityRenderStateExtension;
import fun.wounsee.cosmetic.gui.FiguraToast;
import fun.wounsee.cosmetic.gui.widgets.lists.AvatarList;
import fun.wounsee.cosmetic.lua.api.particle.ParticleAPI;
import fun.wounsee.cosmetic.lua.api.sound.SoundAPI;
import fun.wounsee.cosmetic.lua.api.world.WorldAPI;
import fun.wounsee.cosmetic.utils.EntityUtils;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;
import fun.wounsee.cosmetic.utils.FiguraResourceListener;
import fun.wounsee.cosmetic.utils.FiguraText;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages all the avatars that are currently loaded in memory, and also
 * handles getting the avatars of entities. If an entity does not have a loaded avatar,
 * the AvatarManager will fetch the avatar and cache it.
 */
public class AvatarManager {

    private static final Map<UUID, UserData> LOADED_USERS = new ConcurrentHashMap<>();
    private static final Set<UUID> FETCHED_USERS = new HashSet<>();
    private static final Set<UUID> PREVIEW_USERS = ConcurrentHashMap.newKeySet();

    private static final Int2ObjectMap<Avatar> LOADED_CEM = new Int2ObjectOpenHashMap<>();

    public static final FiguraResourceListener RESOURCE_RELOAD_EVENT = FiguraResourceListener.createResourceListener("resource_reload_event", manager -> executeAll("resourceReloadEvent", Avatar::resourceReloadEvent));

    public static boolean localUploaded = true; // init as true :3
    public static boolean panic = false;

    // Added to reduce look up for entities + fixes trident and arrow that no longer exist in world not being rendered while picked up
    public static Int2ObjectMap<Entity> ENTITY_CACHE = new Int2ObjectOpenHashMap<>();

    // -- panic mode -- // 

    public static void togglePanic() {
        AvatarManager.panic = !AvatarManager.panic;
        FiguraToast.sendToast(FiguraText.of(AvatarManager.panic ? "toast.panic_enabled" : "toast.panic_disabled"), FiguraToast.ToastType.WARNING);
        SoundAPI.getSoundEngine().figura$stopAllSounds();
        ParticleAPI.getParticleEngine().figura$clearParticles(null);
    }

    // -- avatar events -- // 

    public static void tickLoadedAvatars() {
        if (panic)
            return;

        // tick the avatars
        for (UserData user : LOADED_USERS.values()) {
            Avatar avatar = user.getMainAvatar();
            if (avatar != null) {
                CosmeticMod.pushProfiler(avatar);
                avatar.tick();
                CosmeticMod.popProfiler();
            }
        }

        // CEM
        if (LOADED_CEM.isEmpty())
            return;

        // unload entities
        IntSet toBeRemoved = new IntOpenHashSet();

        for (int entityId : LOADED_CEM.keySet()) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null && entity.isRemoved()) {
                toBeRemoved.add(entityId);
                ENTITY_CACHE.remove(entityId);
            } else if (entity == null) {
                ENTITY_CACHE.remove(entityId);
            }
        }

        for (int entity : toBeRemoved)
            LOADED_CEM.remove(entity);

        // tick entities
        for (Avatar avatar : LOADED_CEM.values()) {
            if (avatar != null) {
                CosmeticMod.pushProfiler(avatar);
                avatar.tick();
                CosmeticMod.popProfiler();
            }
        }
    }

    public static void executeAll(String src, Consumer<Avatar> consumer) {
        if (panic) return;

        CosmeticMod.pushProfiler(CosmeticMod.MOD_ID);
        CosmeticMod.pushProfiler(src);

        for (UserData user : LOADED_USERS.values()) {
            if (PREVIEW_USERS.contains(user.id)) continue;
            Avatar avatar = user.getMainAvatar();
            if (avatar != null) {
                CosmeticMod.pushProfiler(avatar);
                consumer.accept(avatar);
                CosmeticMod.popProfiler();
            }
        }

        for (Avatar avatar : LOADED_CEM.values()) {
            if (avatar != null) {
                CosmeticMod.pushProfiler(avatar);
                consumer.accept(avatar);
                CosmeticMod.popProfiler();
            }
        }

        CosmeticMod.popProfiler(2);
    }

    // -- avatar getters -- // 

    // player will also attempt to load from network, if possible
    public static Avatar getAvatarForPlayer(UUID player) {
        if (panic || Minecraft.getInstance().level == null)
            return null;

        fetchBackend(player);

        UserData user = LOADED_USERS.get(player);
        return user == null ? null : user.getMainAvatar();
    }

    private static Avatar getAvatarForEntity(Entity entity) {
        // get loaded
        Avatar loaded = LOADED_CEM.get(entity.getId());
        if (loaded != null)
            return loaded;

        // new avatar
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CompoundTag nbt = LocalAvatarLoader.CEM_AVATARS.get(type);
        return nbt == null ? null : loadEntityAvatar(entity, nbt);
    }

    public static Avatar getAvatar(EntityRenderState state) {
        if (panic || Minecraft.getInstance().level == null || state == null) return null;
        return getAvatar(getEntity(state));
    }

    public static Entity getEntity(EntityRenderState state) {
        if (Minecraft.getInstance().level == null || state == null) return null;

        Integer id = state instanceof PlayerRenderState playerRenderState
                ? playerRenderState.id
                : ((FiguraEntityRenderStateExtension)state).figura$getEntityId();
        if (id == null) return null;
        Entity worldEntity = Minecraft.getInstance().level.getEntity(id);
        return worldEntity != null ? worldEntity : ENTITY_CACHE.get(id);
    }

    public static void bindPreviewEntity(UUID owner, Object candidate) {
        if (!(candidate instanceof Entity entity) || owner == null || !owner.equals(entity.getUUID())) return;
        ENTITY_CACHE.put(entity.getId(), entity);
    }

    public static void unbindPreviewEntity(UUID owner, Object candidate) {
        if (!(candidate instanceof Entity entity) || owner == null || !owner.equals(entity.getUUID())) return;
        Entity cached = ENTITY_CACHE.get(entity.getId());
        if (cached == entity) ENTITY_CACHE.remove(entity.getId());
    }

    // tries to get data from an entity
    public static Avatar getAvatar(Entity entity) {
        if (panic || Minecraft.getInstance().level == null || entity == null)
            return null;

        UUID uuid = entity.getUUID();

        // load from player (fetch backend) if is a player
        if (entity instanceof Player){
            Avatar avatar = getAvatarForPlayer(uuid);
            if (avatar != null)
                return avatar;
        }

        // otherwise check for CEM
        return getAvatarForEntity(entity);
    }

    // get a loaded avatar without fetching backend or creating a new one
    public static Avatar getLoadedAvatar(UUID owner) {
        if (panic || Minecraft.getInstance().level == null)
            return null;

        UserData user = LOADED_USERS.get(owner);
        return user == null ? null : user.getMainAvatar();
    }

    // get all main loaded avatars
    public static List<Avatar> getLoadedAvatars() {
        List<Avatar> list = new ArrayList<>();
        for (UserData user : LOADED_USERS.values()) {
            Avatar avatar = user.getMainAvatar();
            if (avatar != null && avatar.nbt != null)
                list.add(avatar);
        }
        return list;
    }

    // -- avatar management -- // 

    // removes an loaded avatar
    public static void clearAvatars(UUID id) {
        FETCHED_USERS.remove(id);
        PREVIEW_USERS.remove(id);

        UserData user = LOADED_USERS.get(id);
        if (user != null) user.clear();

        NetworkStuff.clear(id);
        CosmeticMod.debug("Cleared avatars of " + id);
    }

    public static void clearCEMAvatars() {
        for (Avatar avatar : LOADED_CEM.values())
            avatar.clean();
        LOADED_CEM.clear();
    }

    // clears ALL loaded avatars, including local
    public static void clearAllAvatars() {
        for (UUID id : LOADED_USERS.keySet())
            clearAvatars(id);

        LOADED_USERS.clear();
        FETCHED_USERS.clear();
        PREVIEW_USERS.clear();
        ENTITY_CACHE.clear();
        clearCEMAvatars();

        localUploaded = true;
        AvatarList.selectedEntry = null;
        LocalAvatarLoader.loadAvatar(null, null);
        CosmeticMod.LOGGER.info("Cleared all avatars");
    }

    // reloads an avatar
    public static void reloadAvatar(UUID id) {
        if (!localUploaded && CosmeticMod.isLocal(id))
            loadLocalAvatar(LocalAvatarLoader.getLastLoadedPath());
        else
            clearAvatars(id);
    }

    // load the local player avatar
    public static void loadLocalAvatar(Path path) {
        UUID id = CosmeticMod.getLocalPlayerUUID();

        // clear
        clearAvatars(id);
        FETCHED_USERS.add(id);

        // load
        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        LocalAvatarLoader.loadAvatar(path, user);

        // mark as not uploaded
        localUploaded = false;
    }

    public static void loadLocalAvatars(List<Path> paths) {
        UUID id = CosmeticMod.getLocalPlayerUUID();
        clearAvatars(id);
        FETCHED_USERS.add(id);
        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        LocalAvatarLoader.loadCompositeAvatar(paths, user, true);
        localUploaded = false;
    }

    public static void loadRemoteAvatars(UUID id, List<Path> paths) {
        if (id == null || CosmeticMod.isLocal(id)) return;
        clearAvatars(id);
        FETCHED_USERS.add(id);
        if (paths == null || paths.isEmpty()) return;
        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        LocalAvatarLoader.loadCompositeAvatar(paths, user, false);
    }

    public static void loadPreviewAvatars(UUID id, List<Path> paths) {
        if (id == null || CosmeticMod.isLocal(id)) return;
        clearAvatars(id);
        FETCHED_USERS.add(id);
        PREVIEW_USERS.add(id);
        if (paths == null || paths.isEmpty()) return;
        UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
        LocalAvatarLoader.loadCompositeAvatar(paths, user, true);
    }

    // load CEM avatar
    public static Avatar loadEntityAvatar(Entity entity, CompoundTag nbt) {
        Avatar targetAvatar = new Avatar(entity);
        targetAvatar.load(nbt);
        LOADED_CEM.put(entity.getId(), targetAvatar);
        AvatarManager.ENTITY_CACHE.putIfAbsent(entity.getId(), entity);
        return targetAvatar;
    }

    // load CEM avatar
    public static Avatar loadEntityAvatar(EntityRenderState entity, CompoundTag nbt) {
        Avatar targetAvatar = new Avatar(entity);
        targetAvatar.load(nbt);
        Integer id = entity instanceof PlayerRenderState playerRenderState ? playerRenderState.id : ((FiguraEntityRenderStateExtension)entity).figura$getEntityId();
        LOADED_CEM.put(id, targetAvatar);
        AvatarManager.ENTITY_CACHE.putIfAbsent(id, WorldAPI.getCurrentWorld().getEntity(id));
        return targetAvatar;
    }

    // set an user's avatar
    public static void setAvatar(UUID id, CompoundTag nbt) {
        try {
            UserData user = LOADED_USERS.computeIfAbsent(id, UserData::new);
            clearAvatars(id);
            user.loadAvatar(nbt);
        } catch (Exception e) {
            CosmeticMod.LOGGER.error("Failed to set avatar for " + id, e);
        }
    }

    // get avatar from the backend
    private static void fetchBackend(UUID id) {
        // Remote avatar discovery is deliberately disabled in the embedded client.
        // The local player's avatar is installed explicitly through EnoughCosmeticBridge.
        FETCHED_USERS.add(id);
    }

    // -- badges -- // 

    public static Pair<BitSet, BitSet> getBadges(UUID id) {
        UserData user = LOADED_USERS.get(id);
        if (user == null)
            return null;

        Pair<BitSet, BitSet> badges = user.getBadges();
        if (badges != null)
            return badges;

        badges = Badges.emptyBadges();
        user.loadBadges(badges);
        return badges;
    }

    // -- command -- // 

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        // root
        LiteralArgumentBuilder<FiguraClientCommandSource> root = LiteralArgumentBuilder.literal("set_avatar");

        // source
        RequiredArgumentBuilder<FiguraClientCommandSource, String> target = RequiredArgumentBuilder.argument("target", StringArgumentType.word());

        // target
        RequiredArgumentBuilder<FiguraClientCommandSource, String> source = RequiredArgumentBuilder.argument("source", StringArgumentType.word());
        source.executes(context -> {
            String s = StringArgumentType.getString(context, "source");
            String t = StringArgumentType.getString(context, "target");

            UUID sourceUUID, targetUUID;
            try {
                sourceUUID = UUID.fromString(s);
                targetUUID = UUID.fromString(t);
            } catch (Exception e) {
                context.getSource().figura$sendError(Component.literal("Failed to parse uuids"));
                return 0;
            }

            UserData user = LOADED_USERS.get(sourceUUID);
            Avatar avatar = user == null ? null : user.getMainAvatar();
            if (avatar == null || avatar.nbt == null) {
                context.getSource().figura$sendError(Component.literal("No source Avatar found"));
                return 0;
            }

            if (LOADED_USERS.get(targetUUID) != null) {
                setAvatar(targetUUID, avatar.nbt);
                if (CosmeticMod.isLocal(targetUUID))
                    localUploaded = true;
                context.getSource().figura$sendFeedback(Component.literal("Set avatar for " + t));
                return 1;
            }

            Entity targetEntity = EntityUtils.getEntityByUUID(targetUUID);
            if (targetEntity == null) {
                context.getSource().figura$sendError(Component.literal("Target entity not found"));
                return 0;
            }

            loadEntityAvatar(targetEntity, avatar.nbt);
            return 1;
        });
        target.then(source);

        // build root
        root.then(target);
        return root;
    }
}
