package fun.wounsee.cosmetic.lua.docs;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.animation.Animation;
import fun.wounsee.cosmetic.entries.FiguraAPI;
import fun.wounsee.cosmetic.lua.api.*;
import fun.wounsee.cosmetic.lua.api.action_wheel.Action;
import fun.wounsee.cosmetic.lua.api.action_wheel.ActionWheelAPI;
import fun.wounsee.cosmetic.lua.api.action_wheel.Page;
import fun.wounsee.cosmetic.lua.api.data.*;
import fun.wounsee.cosmetic.lua.api.json.*;
import fun.wounsee.cosmetic.lua.api.entity.EntityAPI;
import fun.wounsee.cosmetic.lua.api.entity.LivingEntityAPI;
import fun.wounsee.cosmetic.lua.api.entity.PlayerAPI;
import fun.wounsee.cosmetic.lua.api.entity.ViewerAPI;
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
import fun.wounsee.cosmetic.lua.api.net.HttpRequestsAPI;
import fun.wounsee.cosmetic.lua.api.net.NetworkingAPI;
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
import fun.wounsee.cosmetic.math.matrix.FiguraMatrix;
import fun.wounsee.cosmetic.math.vector.FiguraVec2;
import fun.wounsee.cosmetic.math.vector.FiguraVec3;
import fun.wounsee.cosmetic.math.vector.FiguraVec4;
import fun.wounsee.cosmetic.math.vector.FiguraVector;
import fun.wounsee.cosmetic.model.CosmeticModelPart;
import fun.wounsee.cosmetic.model.rendering.Vertex;
import fun.wounsee.cosmetic.model.rendering.texture.FiguraTexture;
import fun.wounsee.cosmetic.model.rendertasks.*;
import fun.wounsee.cosmetic.utils.FiguraClientCommandSource;
import fun.wounsee.cosmetic.utils.FiguraText;
import org.luaj.vm2.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FiguraDocsManager {

    // class name map
    private static final Map<Class<?>, String> NAME_MAP = new HashMap<>() {{
        // Built in type names, even for things that don't have docs
        put(Double.class, "Number");
        put(double.class, "Number");
        put(Float.class, "Number");
        put(float.class, "Number");
        put(Number.class, "Number");

        put(Integer.class, "Integer");
        put(int.class, "Integer");
        put(Long.class, "Integer");
        put(long.class, "Integer");

        put(void.class, "nil");

        put(String.class, "String");

        put(Object.class, "AnyType");
        put(LuaUserdata.class, "Userdata");

        put(Boolean.class, "Boolean");
        put(boolean.class, "Boolean");

        // Lua things
        put(LuaFunction.class, "Function");
        put(LuaTable.class, "Table");
        put(LuaValue.class, "AnyType");
        put(Varargs.class, "Varargs");

        // converted things
        put(Map.class, "Table");
        put(HashMap.class, "Table");
        put(List.class, "Table");
        put(ArrayList.class, "Table");

        // Figura types
        put(FiguraVector.class, "Vector");
        put(FiguraMatrix.class, "Matrix");
    }};
    private static final Map<Class<?>, String> CLASS_COMMAND_MAP = new HashMap<>();

    // -- docs generator data -- // 

    private static final Map<String, Collection<Class<?>>> GLOBAL_CHILDREN = new HashMap<>() {{
        put("action_wheel", List.of(
                ActionWheelAPI.class,
                Page.class,
                Action.class
        ));

        put("animations", List.of(
                AnimationAPI.class,
                Animation.class
        ));

        put("nameplate", List.of(
                NameplateAPI.class,
                NameplateCustomization.class,
                EntityNameplateCustomization.class,
                NameplateCustomizationGroup.class
        ));

        put("world", List.of(
                WorldAPI.class,
                BiomeAPI.class,
                BlockStateAPI.class,
                ItemStackAPI.class
        ));

        put("vanilla_model", List.of(
                VanillaModelAPI.class,
                VanillaPart.class,
                VanillaModelPart.class,
                VanillaGroupPart.class
        ));

        put("models", List.of(
                Vertex.class,
                CosmeticModelPart.class,
                RenderTask.class,
                BlockTask.class,
                ItemTask.class,
                TextTask.class,
                SpriteTask.class,
                EntityTask.class
        ));

        put("player", List.of(
                EntityAPI.class,
                LivingEntityAPI.class,
                PlayerAPI.class,
                ViewerAPI.class
        ));

        put("events", List.of(
                EventsAPI.class,
                LuaEvent.class
        ));

        put("keybinds", List.of(
                KeybindAPI.class,
                FiguraKeybind.class
        ));

        put("vectors", List.of(
                VectorsAPI.class,
                FiguraVec2.class,
                FiguraVec3.class,
                FiguraVec4.class
        ));

        put("matrices", List.of(
                MatricesAPI.class,
                FiguraMat2.class,
                FiguraMat3.class,
                FiguraMat4.class
        ));

        put("client", List.of(
                ClientAPI.class
        ));

        put("host", List.of(
                HostAPI.class
        ));

        put("avatar", List.of(
                AvatarAPI.class
        ));

        put("particles", List.of(
                ParticleAPI.class,
                LuaParticle.class
        ));

        put("sounds", List.of(
                SoundAPI.class,
                LuaSound.class
        ));

        put("renderer", List.of(
                RendererAPI.class
        ));

        put("pings", List.of(
                PingAPI.class,
                PingFunction.class
        ));

        put("textures", List.of(
                TextureAPI.class,
                FiguraTexture.class,
                TextureAtlasAPI.class
        ));

        put("config", List.of(
                ConfigAPI.class
        ));

        put("data", List.of(
                DataAPI.class,
                FiguraInputStream.class,
                FiguraOutputStream.class,
                FiguraBuffer.class,
                FiguraFuture.class
        ));

        put("net", List.of(
                NetworkingAPI.class,
                HttpRequestsAPI.class,
                HttpRequestsAPI.HttpResponse.class,
                HttpRequestsAPI.HttpRequestBuilder.class
        ));

        put("file", List.of(
                FileAPI.class
        ));

        put("json", List.of(
                JsonAPI.class,
                FiguraJsonBuilder.class,
                FiguraJsonSerializer.class,
                FiguraJsonObject.class,
                FiguraJsonArray.class
        ));

        put("resources", List.of(
                ResourcesAPI.class
        ));
        put("raycast", List.of(
                RaycastAPI.class
        ));
    }};
    private static final Map<String, List<FiguraDoc>> GENERATED_CHILDREN = new HashMap<>();

    private static FiguraDoc.ClassDoc global;

    private static final List<Class<?>> LUA_LIB_OVERRIDES = List.of(
            FiguraMathDocs.class
    );
    private static final List<FiguraDoc> GENERATED_LIB_OVERRIDES = new ArrayList<>();

    public static void init() {
        // generate children override
        for (Map.Entry<String, Collection<Class<?>>> packageEntry : GLOBAL_CHILDREN.entrySet()) {
            for (Class<?> documentedClass : packageEntry.getValue()) {
                FiguraDoc.ClassDoc doc = generateDocFor(documentedClass, "globals " + packageEntry.getKey());
                if (doc != null)
                    GENERATED_CHILDREN.computeIfAbsent(packageEntry.getKey(), s -> new ArrayList<>()).add(doc);
            }
        }

        // generate standard libraries overrides
        for (Class<?> lib : LUA_LIB_OVERRIDES) {
            FiguraDoc.ClassDoc libDoc = generateDocFor(lib, null);
            if (libDoc != null)
                GENERATED_LIB_OVERRIDES.add(libDoc);
        }

        // generate globals
        Class<?> globalClass = FiguraGlobalsDocs.class;
        global = new FiguraDoc.ClassDoc(globalClass, globalClass.getAnnotation(LuaTypeDoc.class), GENERATED_CHILDREN);
    }

    public static void initEntryPoints(Set<FiguraAPI> set) {
        for (FiguraAPI api : set)
            GLOBAL_CHILDREN.put(api.getName(), api.getDocsClasses());
    }

    private static FiguraDoc.ClassDoc generateDocFor(Class<?> documentedClass, String pack) {
        if (!documentedClass.isAnnotationPresent(LuaTypeDoc.class))
            return null;

        FiguraDoc.ClassDoc doc = new FiguraDoc.ClassDoc(documentedClass, documentedClass.getAnnotation(LuaTypeDoc.class));
        NAME_MAP.put(documentedClass, doc.name);
        CLASS_COMMAND_MAP.put(documentedClass, "/figura docs " + (pack == null ? "" : pack) + " " + doc.name);
        return doc;
    }

    public static String getNameFor(Class<?> clazz) {
        return NAME_MAP.computeIfAbsent(clazz, aClass -> {
            if (clazz.isAnnotationPresent(LuaTypeDoc.class))
                return clazz.getAnnotation(LuaTypeDoc.class).name();
            else if (clazz.getName().startsWith("["))
                return "Varargs";
            else
                return clazz.getName();
        });
    }

    public static MutableComponent getClassText(Class<?> clazz) {
        String name = getNameFor(clazz);
        String doc = CLASS_COMMAND_MAP.get(clazz);

        MutableComponent text = Component.literal(name);
        if (doc == null)
            return text;

        text.setStyle(
                Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, doc))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, FiguraText.of("command.docs_type_hover", Component.literal(name).withStyle(ChatFormatting.DARK_PURPLE))))
                .withUnderlined(true));
        return text;
    }

    // -- commands -- // 

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getCommand() {
        // root
        LiteralArgumentBuilder<FiguraClientCommandSource> root = LiteralArgumentBuilder.literal("docs");
        root.executes(context -> FiguraDoc.printRoot());

        // globals
        LiteralArgumentBuilder<FiguraClientCommandSource> globals = global == null ? LiteralArgumentBuilder.literal("globals") : global.getCommand();
        root.then(globals);

        // library overrides
        for (FiguraDoc figuraDoc : GENERATED_LIB_OVERRIDES)
            root.then(figuraDoc.getCommand());

        // list docs
        root.then(FiguraListDocs.getCommand());

        return root;
    }

    public static LiteralArgumentBuilder<FiguraClientCommandSource> getExportCommand() {
        LiteralArgumentBuilder<FiguraClientCommandSource> root = LiteralArgumentBuilder.literal("docs");
        root.executes(context -> exportDocsFunction(context, true));

        RequiredArgumentBuilder<FiguraClientCommandSource, Boolean> e = RequiredArgumentBuilder.argument("translate", BoolArgumentType.bool());
        e.executes(context -> exportDocsFunction(context, BoolArgumentType.getBool(context, "translate")));
        root.then(e);

        return root;
    }

    // -- export -- // 

    private static int exportDocsFunction(CommandContext<FiguraClientCommandSource> context, boolean translate) {
        try {
            // get path
            Path targetPath = CosmeticMod.getFiguraDirectory().resolve("exported_docs.json");

            // create file
            if (!Files.exists(targetPath))
                Files.createFile(targetPath);

            // write file
            OutputStream fs = Files.newOutputStream(targetPath);
            fs.write(exportAsJsonString(translate).getBytes());
            fs.close();

            // feedback
            context.getSource().figura$sendFeedback(
                    FiguraText.of("command.docs_export.success")
                            .append(" ")
                            .append(FiguraText.of("command.click_to_open")
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, targetPath.toString())).withUnderlined(true))
                            )
            );
            return 1;
        } catch (Exception e) {
            context.getSource().figura$sendError(FiguraText.of("command.docs_export.error"));
            CosmeticMod.LOGGER.error("Failed to export docs!", e);
            return 0;
        }
    }

    public static String exportAsJsonString(boolean translate) {
        // root
        JsonObject root = new JsonObject();

        // globals
        JsonObject globals = global == null ? new JsonObject() : global.toJson(translate);
        root.add("globals", globals);

        // library overrides
        for (FiguraDoc figuraDoc : GENERATED_LIB_OVERRIDES)
            root.add(figuraDoc.name, figuraDoc.toJson(translate));

        // lists
        root.add("lists", FiguraListDocs.toJson(translate));

        // return as string
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(root);
    }
}
