package fun.wounsee.cosmetic.lua;

import fun.wounsee.cosmetic.animation.Animation;
import fun.wounsee.cosmetic.entries.FiguraAPI;
import fun.wounsee.cosmetic.lua.api.*;
import fun.wounsee.cosmetic.lua.api.data.*;
import fun.wounsee.cosmetic.lua.api.json.*;
import fun.wounsee.cosmetic.lua.api.entity.*;
import fun.wounsee.cosmetic.lua.api.net.HttpRequestsAPI;
import fun.wounsee.cosmetic.lua.api.net.NetworkingAPI;
import fun.wounsee.cosmetic.model.rendertasks.*;
import fun.wounsee.cosmetic.lua.api.action_wheel.Action;
import fun.wounsee.cosmetic.lua.api.action_wheel.ActionWheelAPI;
import fun.wounsee.cosmetic.lua.api.action_wheel.Page;
import fun.wounsee.cosmetic.lua.api.event.EventsAPI;
import fun.wounsee.cosmetic.lua.api.event.LuaEvent;
import fun.wounsee.cosmetic.lua.api.keybind.FiguraKeybind;
import fun.wounsee.cosmetic.lua.api.keybind.KeybindAPI;
import fun.wounsee.cosmetic.lua.api.math.MatricesAPI;
import fun.wounsee.cosmetic.lua.api.math.VectorsAPI;
import fun.wounsee.cosmetic.lua.api.nameplate.EntityNameplateCustomization;
import fun.wounsee.cosmetic.lua.api.nameplate.NameplateAPI;
import fun.wounsee.cosmetic.lua.api.nameplate.NameplateCustomization;
import fun.wounsee.cosmetic.lua.api.nameplate.NameplateCustomizationGroup;
import fun.wounsee.cosmetic.lua.api.particle.LuaParticle;
import fun.wounsee.cosmetic.lua.api.particle.ParticleAPI;
import fun.wounsee.cosmetic.lua.api.ping.PingAPI;
import fun.wounsee.cosmetic.lua.api.ping.PingFunction;
import fun.wounsee.cosmetic.lua.api.sound.LuaSound;
import fun.wounsee.cosmetic.lua.api.sound.SoundAPI;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaGroupPart;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaModelAPI;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaModelPart;
import fun.wounsee.cosmetic.lua.api.vanilla_model.VanillaPart;
import fun.wounsee.cosmetic.lua.api.world.BiomeAPI;
import fun.wounsee.cosmetic.lua.api.world.BlockStateAPI;
import fun.wounsee.cosmetic.lua.api.world.ItemStackAPI;
import fun.wounsee.cosmetic.lua.api.world.WorldAPI;
import fun.wounsee.cosmetic.math.matrix.FiguraMat2;
import fun.wounsee.cosmetic.math.matrix.FiguraMat3;
import fun.wounsee.cosmetic.math.matrix.FiguraMat4;
import fun.wounsee.cosmetic.math.vector.FiguraVec2;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.math.vector.FiguraVec4;
import fun.wounsee.cosmetic.model.CosmeticModelPart;
import fun.wounsee.cosmetic.model.rendering.Vertex;
import fun.wounsee.cosmetic.model.rendering.texture.FiguraTexture;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A set of Globals of which there is only one in the MC instance.
 * This set of Globals is used to compile and run other scripts.
 */
public class FiguraAPIManager {

    /**
     * Addon mods simply need to add their classes to the WHITELISTED_CLASSES set,
     * and whichever global vars they want to set into the API_GETTERS map.
     */
    public static final Set<Class<?>> WHITELISTED_CLASSES = new HashSet<>() {{
        add(FiguraVec2.class);
        add(FiguraVec3.class);
        add(FiguraVec4.class);

        add(FiguraMat2.class);
        add(FiguraMat3.class);
        add(FiguraMat4.class);

        add(NullEntity.class);
        add(EntityAPI.class);
        add(LivingEntityAPI.class);
        add(PlayerAPI.class);
        add(ViewerAPI.class);

        add(EventsAPI.class);
        add(LuaEvent.class);

        add(Vertex.class);
        add(CosmeticModelPart.class);
        add(RenderTask.class);
        add(ItemTask.class);
        add(BlockTask.class);
        add(EntityTask.class);
        add(TextTask.class);
        add(SpriteTask.class);

        add(SoundAPI.class);
        add(LuaSound.class);

        add(ParticleAPI.class);
        add(LuaParticle.class);

        add(VanillaModelAPI.class);
        add(VanillaPart.class);
        add(VanillaGroupPart.class);
        add(VanillaModelPart.class);

        add(KeybindAPI.class);
        add(FiguraKeybind.class);

        add(NameplateAPI.class);
        add(NameplateCustomization.class);
        add(EntityNameplateCustomization.class);
        add(NameplateCustomizationGroup.class);

        add(ActionWheelAPI.class);
        add(Page.class);
        add(Action.class);

        add(VectorsAPI.class);
        add(MatricesAPI.class);

        add(WorldAPI.class);
        add(BiomeAPI.class);
        add(BlockStateAPI.class);
        add(ItemStackAPI.class);

        add(PingAPI.class);
        add(PingFunction.class);

        add(TextureAPI.class);
        add(FiguraTexture.class);

        add(AnimationAPI.class);
        add(Animation.class);

        add(HostAPI.class);

        add(RendererAPI.class);

        add(ClientAPI.class);

        add(AvatarAPI.class);

        add(ConfigAPI.class);

        add(TextureAtlasAPI.class);

        add(FiguraInputStream.class);
        add(FiguraOutputStream.class);

        add(DataAPI.class);

        add(FiguraBuffer.class);

        add(FileAPI.class);

        add(JsonAPI.class);
        add(FiguraJsonBuilder.class);
        add(FiguraJsonSerializer.class);
        add(FiguraJsonArray.class);
        add(FiguraJsonObject.class);

        add(ResourcesAPI.class);

        add(NetworkingAPI.class);

        add(HttpRequestsAPI.class);
        add(HttpRequestsAPI.HttpRequestBuilder.class);
        add(HttpRequestsAPI.HttpResponse.class);

        add(FiguraFuture.class);

        add(RaycastAPI.class);
    }};

    public static final Map<String, Function<FiguraLuaRuntime, Object>> API_GETTERS = new LinkedHashMap<>() {{
        put("events", r -> r.events = new EventsAPI());
        put("sounds", r -> new SoundAPI(r.owner));
        put("vanilla_model", r -> r.vanilla_model = new VanillaModelAPI(r.owner));
        put("keybinds", r -> r.keybinds = new KeybindAPI(r.owner));
        put("host", r -> r.host = new HostAPI(r.owner));
        put("nameplate", r -> r.nameplate = new NameplateAPI());
        put("renderer", r -> r.renderer = new RendererAPI(r.owner));
        put("action_wheel", r -> r.action_wheel = new ActionWheelAPI(r.owner));
        put("animations", r -> new AnimationAPI(r.owner));
        put("client", r -> ClientAPI.INSTANCE);
        put("particles", r -> new ParticleAPI(r.owner));
        put("avatar", r -> r.avatar_meta = new AvatarAPI(r.owner));
        put("vectors", r -> VectorsAPI.INSTANCE);
        put("matrices", r -> MatricesAPI.INSTANCE);
        put("world", r -> WorldAPI.INSTANCE);
        put("pings", r -> r.ping = new PingAPI(r.owner));
        put("textures", r -> r.texture = new TextureAPI(r.owner));
        put("config", r -> new ConfigAPI(r.owner));
        put("data", r -> new DataAPI(r.owner));
        put("file", r -> new FileAPI(r.owner));
        put("json", r -> JsonAPI.INSTANCE);
        put("resources", r -> new ResourcesAPI(r.owner));
        put("net", r -> new NetworkingAPI(r.owner));
        put("raycast", r -> new RaycastAPI(r.owner));
    }};

    private static final Set<FiguraAPI> ENTRYPOINTS = new HashSet<>();

    public static void initEntryPoints(Set<FiguraAPI> set) {
        for (FiguraAPI api : set) {
            ENTRYPOINTS.add(api);
            WHITELISTED_CLASSES.addAll(api.getWhitelistedClasses());
        }
    }

    public static void setupTypesAndAPIs(FiguraLuaRuntime runtime) {
        for (Class<?> clazz : WHITELISTED_CLASSES)
            runtime.registerClass(clazz);
        for (Map.Entry<String, Function<FiguraLuaRuntime, Object>> entry : API_GETTERS.entrySet())
            runtime.setGlobal(entry.getKey(), entry.getValue().apply(runtime));
        for (FiguraAPI api : ENTRYPOINTS) {
            String name = api.getName();
            if (name != null)
                runtime.setGlobal(name, api.build(runtime.owner));
        }
    }
}
