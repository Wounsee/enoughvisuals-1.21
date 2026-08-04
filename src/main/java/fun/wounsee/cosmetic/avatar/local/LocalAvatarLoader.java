package fun.wounsee.cosmetic.avatar.local;

import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import fun.wounsee.cosmetic.CosmeticMod;
import fun.wounsee.cosmetic.avatar.AvatarManager;
import fun.wounsee.cosmetic.avatar.UserData;
import fun.wounsee.cosmetic.gui.FiguraToast;
import fun.wounsee.cosmetic.parsers.AvatarMetadataParser;
import fun.wounsee.cosmetic.parsers.BlockbenchModelParser;
import fun.wounsee.cosmetic.parsers.LuaScriptParser;
import fun.wounsee.cosmetic.utils.FiguraResourceListener;
import fun.wounsee.cosmetic.utils.FiguraText;
import fun.wounsee.cosmetic.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * class used to load avatars from a file
 * and used for hot-swapping
 */
public class LocalAvatarLoader {

    public static final boolean IS_WINDOWS = Util.getPlatform() == Util.OS.WINDOWS;
    private static final HashMap<Path, WatchKey> KEYS = new HashMap<>();

    private static CompletableFuture<Void> tasks;
    private static Path lastLoadedPath;
    private static List<Path> lastLoadedPaths = List.of();
    private static LoadState loadState = LoadState.UNKNOWN;
    private static String loadError;

    private static WatchService watcher;

    public static final HashMap<ResourceLocation, CompoundTag> CEM_AVATARS = new HashMap<>();
    public static final FiguraResourceListener AVATAR_LISTENER = FiguraResourceListener.createResourceListener("cem", manager -> {
        CEM_AVATARS.clear();
        AvatarManager.clearCEMAvatars();

        for (Map.Entry<ResourceLocation, Resource> cem : manager.listResources("cem", location -> location.getNamespace().equals(CosmeticMod.MOD_ID) && location.getPath().endsWith(".nbt")).entrySet()) {
            // id
            ResourceLocation key = cem.getKey();
            String[] split = key.getPath().split("/");
            if (split.length <= 1)
                continue;

            String namespace = split[split.length - 2];
            String path = split[split.length - 1];
            // This is a 4 because .nbt has 4 characters
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path.substring(0, path.length() - 4));

            // nbt
            CompoundTag nbt;
            try {
                nbt = NbtIo.readCompressed(cem.getValue().open(), NbtAccounter.unlimitedHeap());
            } catch (Exception e) {
                CosmeticMod.LOGGER.error("Failed to load " + id + " avatar", e);
                continue;
            }

            // insert
            CosmeticMod.LOGGER.info("Loaded CEM model for " + id);
            CEM_AVATARS.put(id, nbt);
        }
    });

    static {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (Exception e) {
            CosmeticMod.LOGGER.error("Failed to initialize the watcher service", e);
        }
    }

    protected static void async(Runnable toRun) {
        if (tasks == null || tasks.isDone()) {
            tasks = CompletableFuture.runAsync(toRun);
        } else {
            tasks.thenRun(toRun);
        }
    }

    /**
     * Loads an NbtCompound from the specified path
     *
     * @param path - the file/folder for loading the avatar
     */
    public static void loadAvatar(Path path, UserData target) {
        loadError = null;
        loadState = LoadState.UNKNOWN;
        resetWatchKeys();
        try {
            path = path == null ? null : path.getFileSystem() == FileSystems.getDefault() ? Path.of(path.toFile().getCanonicalPath()) : path.normalize();
        } catch (IOException e) {
        }
        lastLoadedPath = path;
        lastLoadedPaths = path == null ? List.of() : List.of(path);

        if (path == null || target == null)
            return;

        addWatchKey(path, KEYS::put);

        Path finalPath = path;
        async(() -> {
            try {
                // load as folder
                CompoundTag nbt = new CompoundTag();

                // scripts
                loadState = LoadState.SCRIPTS;
                loadScripts(finalPath, nbt);

                // custom sounds
                loadState = LoadState.SOUNDS;
                loadSounds(finalPath, nbt);

                // models
                CompoundTag textures = new CompoundTag();
                ListTag animations = new ListTag();
                BlockbenchModelParser modelParser = new BlockbenchModelParser();

                loadState = LoadState.MODELS;
                CompoundTag models = loadModels(finalPath, finalPath, modelParser, textures, animations, "");
                models.putString("name", "models");

                // metadata
                loadState = LoadState.METADATA;
                String _meta = IOUtils.readFile(finalPath.resolve("avatar.json"));
				var metadata = AvatarMetadataParser.read(_meta);

				CompoundTag metaNBT = AvatarMetadataParser.parse(metadata,_meta, IOUtils.getFileNameOrEmpty(finalPath));
				nbt.put("metadata", metaNBT);
				metaNBT.putString("uuid",target.id.toString());

				AvatarMetadataParser.injectToModels(metadata, models);
				AvatarMetadataParser.injectToTextures(metadata, textures);

                // return :3
                if (!models.isEmpty())
                    nbt.put("models", models);
                if (!textures.isEmpty())
                    nbt.put("textures", textures);
                if (!animations.isEmpty())
                    nbt.put("animations", animations);
                CompoundTag metadataTag = nbt.getCompound("metadata");
                if (metadataTag.contains("resources_paths")) {
                    loadResources(nbt, metadataTag.getList("resources_paths", Tag.TAG_STRING), finalPath);
                    metadataTag.remove("resource_paths");
                }

                // load
                target.loadAvatar(nbt);
            } catch (Throwable e) {
                loadError = e.getMessage();
                CosmeticMod.LOGGER.error("Failed to load avatar from " + finalPath, e);
                FiguraToast.sendToast(FiguraText.of("toast.load_error"), FiguraText.of("gui.load_error." + LocalAvatarLoader.getLoadState()), FiguraToast.ToastType.ERROR);
            }
        });
    }

    /** Loads several catalog parts as one avatar while keeping every part's Lua, textures and animations. */
    public static void loadCompositeAvatar(List<Path> sourcePaths, UserData target, boolean watchFiles) {
        loadError = null;
        loadState = LoadState.UNKNOWN;
        if (watchFiles) resetWatchKeys();

        List<Path> paths = new ArrayList<>();
        if (sourcePaths != null) {
            for (Path source : sourcePaths) {
                if (source == null) continue;
                try {
                    paths.add(source.getFileSystem() == FileSystems.getDefault()
                            ? Path.of(source.toFile().getCanonicalPath()) : source.normalize());
                } catch (IOException ignored) {
                    paths.add(source.normalize());
                }
            }
        }
        if (watchFiles) {
            lastLoadedPaths = List.copyOf(paths);
            lastLoadedPath = paths.isEmpty() ? null : paths.get(0);
            for (Path path : paths) addWatchKey(path, KEYS::put);
        }
        if (paths.isEmpty() || target == null) return;

        async(() -> {
            try {
                CompoundTag nbt = new CompoundTag();
                CompoundTag scripts = new CompoundTag();
                CompoundTag sounds = new CompoundTag();
                CompoundTag textures = new CompoundTag();
                ListTag animations = new ListTag();
                ListTag modelChildren = new ListTag();
                ListTag autoScripts = new ListTag();
                BlockbenchModelParser modelParser = new BlockbenchModelParser();
                CompoundTag rootMetadata = null;
                Set<String> usedModelRoots = new HashSet<>();

                for (int index = 0; index < paths.size(); index++) {
                    Path path = paths.get(index);
                    String namespace = "part" + index;

                    loadState = LoadState.MODELS;
                    CompoundTag partModels = loadModels(path, path, modelParser, textures, animations, "");

                    loadState = LoadState.METADATA;
                    String rawMetadata = IOUtils.readFile(path.resolve("avatar.json"));
                    AvatarMetadataParser.Metadata metadata = AvatarMetadataParser.read(rawMetadata);
                    CompoundTag metadataNbt = AvatarMetadataParser.parse(metadata, rawMetadata, IOUtils.getFileNameOrEmpty(path));
                    AvatarMetadataParser.injectToModels(metadata, partModels);
                    AvatarMetadataParser.injectToTextures(metadata, textures);
                    if (rootMetadata == null) rootMetadata = metadataNbt;

                    Map<String, String> renamedRoots = renameDuplicateRoots(partModels, namespace, usedModelRoots);
                    loadState = LoadState.SCRIPTS;
                    mergeScripts(path, namespace, renamedRoots, scripts, autoScripts);
                    loadState = LoadState.SOUNDS;
                    mergeSounds(path, namespace, sounds);

                    if (partModels.contains("chld")) {
                        modelChildren.addAll(partModels.getList("chld", Tag.TAG_COMPOUND));
                    }
                }

                if (!scripts.isEmpty()) nbt.put("scripts", scripts);
                if (!sounds.isEmpty()) nbt.put("sounds", sounds);
                if (!modelChildren.isEmpty()) {
                    CompoundTag models = new CompoundTag();
                    models.putString("name", "models");
                    models.put("chld", modelChildren);
                    nbt.put("models", models);
                }
                if (!textures.isEmpty()) nbt.put("textures", textures);
                if (!animations.isEmpty()) nbt.put("animations", animations);
                if (rootMetadata == null) rootMetadata = new CompoundTag();
                rootMetadata.putString("uuid", target.id.toString());
                rootMetadata.put("autoScripts", autoScripts);
                nbt.put("metadata", rootMetadata);
                target.loadAvatar(nbt);
            } catch (Throwable exception) {
                loadError = exception.getMessage();
                CosmeticMod.LOGGER.error("Failed to load composite avatar from " + paths, exception);
                FiguraToast.sendToast(FiguraText.of("toast.load_error"),
                        FiguraText.of("gui.load_error." + getLoadState()), FiguraToast.ToastType.ERROR);
            }
        });
    }

    private static Map<String, String> renameDuplicateRoots(CompoundTag models, String namespace,
                                                             Set<String> usedNames) {
        Map<String, String> renamed = new HashMap<>();
        if (!models.contains("chld")) return renamed;
        ListTag children = models.getList("chld", Tag.TAG_COMPOUND);
        for (int i = 0; i < children.size(); i++) {
            CompoundTag child = children.getCompound(i);
            String original = child.getString("name");
            String resolved = original;
            if (usedNames.contains(resolved)) {
                resolved = namespace + "_" + original;
                int suffix = 2;
                while (usedNames.contains(resolved)) resolved = namespace + "_" + original + suffix++;
                child.putString("name", resolved);
                renamed.put(original, resolved);
            }
            usedNames.add(resolved);
        }
        return renamed;
    }

    private static void mergeScripts(Path path, String namespace, Map<String, String> renamedRoots,
                                     CompoundTag destination,
                                     ListTag autoScripts) throws IOException {
        List<Path> scripts = IOUtils.getFilesByExtension(path, ".lua");
        String separator = path.toString().isEmpty() ? "\\Q\\E"
                : Pattern.quote(path + path.getFileSystem().getSeparator());
        Map<Path, String> sourceNames = new LinkedHashMap<>();
        Map<String, String> resolvedNames = new HashMap<>();
        for (Path script : scripts) {
            String name = script.toString().replaceFirst(separator, "").replaceAll("[/\\\\]", ".");
            name = name.substring(0, name.length() - 4);
            sourceNames.put(script, name);
            boolean collision = destination.contains(name);
            resolvedNames.put(name, collision && isEntryScript(name) ? namespace + "." + name : name);
        }
        for (Map.Entry<Path, String> script : sourceNames.entrySet()) {
            String name = script.getValue();
            String resolved = resolvedNames.get(name);
            if (destination.contains(resolved)) continue;
            String source = IOUtils.readFile(script.getKey());
            for (Map.Entry<String, String> rename : renamedRoots.entrySet()) {
                source = source.replaceAll("models\\." + Pattern.quote(rename.getKey()) + "(?=[^A-Za-z0-9_]|$)",
                        java.util.regex.Matcher.quoteReplacement("models." + rename.getValue()));
            }
            for (Map.Entry<String, String> scriptName : resolvedNames.entrySet()) {
                if (scriptName.getKey().equals(scriptName.getValue())) continue;
                source = rewriteRequire(source, scriptName.getKey(), scriptName.getValue());
            }
            destination.put(resolved, LuaScriptParser.parseScript(resolved, source));
            autoScripts.add(StringTag.valueOf(resolved));
        }
    }

    private static boolean isEntryScript(String name) {
        int separator = name.lastIndexOf('.');
        String leaf = separator < 0 ? name : name.substring(separator + 1);
        return leaf.equalsIgnoreCase("script") || leaf.equalsIgnoreCase("main")
                || leaf.equalsIgnoreCase("init");
    }

    private static String rewriteRequire(String source, String original, String resolved) {
        return source
                .replace("require(\"" + original + "\")", "require(\"" + resolved + "\")")
                .replace("require('" + original + "')", "require('" + resolved + "')")
                .replace("require \"" + original + "\"", "require \"" + resolved + "\"")
                .replace("require '" + original + "'", "require '" + resolved + "'");
    }

    private static void mergeSounds(Path path, String namespace, CompoundTag destination) throws IOException {
        List<Path> sounds = IOUtils.getFilesByExtension(path, ".ogg");
        String separator = Pattern.quote(path.toString().isEmpty() ? path.toString()
                : path + path.getFileSystem().getSeparator());
        for (Path sound : sounds) {
            String name = sound.toString().replaceFirst(separator, "").replaceAll("[/\\\\]", ".");
            name = name.substring(0, name.length() - 4);
            byte[] bytes = IOUtils.readFileBytes(sound);
            destination.putByteArray(namespace + "." + name, bytes);
            if (!destination.contains(name)) destination.putByteArray(name, bytes);
        }
    }
    private static void loadResources(CompoundTag nbt, ListTag pathsTag, Path parentPath) {
        ArrayList<PathMatcher> pathMatchers = new ArrayList<>();
        FileSystem fs = FileSystems.getDefault();
        for (int i = 0; i < pathsTag.size(); i++) {
            pathMatchers.add(fs.getPathMatcher("glob:".concat(pathsTag.getString(i))));
        }
        Map<String, Path> pathMap = new HashMap<>();
        matchPathsRecursive(pathMap, parentPath, parentPath, pathMatchers);
        CompoundTag resourcesTag = new CompoundTag();
        for (String p:
                pathMap.keySet()) {
            try (FileInputStream fis = new FileInputStream(pathMap.get(p).toFile())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gos = new GZIPOutputStream(baos);
                int i;
                while ((i = fis.read()) != -1) {
                    gos.write(i);
                }
                gos.close();
                resourcesTag.put(unixifyPath(p), new ByteArrayTag(baos.toByteArray()));
                baos.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        nbt.put("resources", resourcesTag);
    }

    private static String unixifyPath(String original) {
        Path p = Path.of(original);
        String[] components = new String[p.getNameCount()];
        for (int i = 0; i < components.length; i++) {
            components[i] = p.getName(i).toString();
        }
        return String.join("/", components);
    }

    private static void matchPathsRecursive(Map<String, Path> pathMap, Path parent, Path current, ArrayList<PathMatcher> matchers) {
        File f = current.toFile();
        if (f.isFile()) {
            Path relative = parent.toAbsolutePath().relativize(current.toAbsolutePath()).normalize();
            for (PathMatcher m :
                    matchers) {
                if (m.matches(relative)) {
                    pathMap.put(relative.toString(), current);
                    break;
                }
            }
        }
        else {
            for (File fl :
                    current.toFile().listFiles()) {
                matchPathsRecursive(pathMap, parent, fl.toPath(), matchers);
            }
        }
    }

    private static void loadScripts(Path path, CompoundTag nbt) throws IOException {
        List<Path> scripts = IOUtils.getFilesByExtension(path, ".lua");
        if (scripts.size() > 0) {
            CompoundTag scriptsNbt = new CompoundTag();
            String pathRegex = path.toString().isEmpty() ? "\\Q\\E" : Pattern.quote(path + path.getFileSystem().getSeparator());
            for (Path script : scripts) {
                String name = script.toString()
                        .replaceFirst(pathRegex, "")
                        .replaceAll("[/\\\\]", ".");
                name = name.substring(0, name.length() - 4);
                scriptsNbt.put(name, LuaScriptParser.parseScript(name, IOUtils.readFile(script)));
            }
            nbt.put("scripts", scriptsNbt);
        }
    }

    private static void loadSounds(Path path, CompoundTag nbt) throws IOException {
        List<Path> sounds = IOUtils.getFilesByExtension(path, ".ogg");
        if (sounds.size() > 0) {
            CompoundTag soundsNbt = new CompoundTag();
            String pathRegex = Pattern.quote(path.toString().isEmpty() ? path.toString() : path + path.getFileSystem().getSeparator());
            for (Path sound : sounds) {
                String name = sound.toString()
                        .replaceFirst(pathRegex, "")
                        .replaceAll("[/\\\\]", ".");
                name = name.substring(0, name.length() - 4);
                soundsNbt.putByteArray(name, IOUtils.readFileBytes(sound));
            }
            nbt.put("sounds", soundsNbt);
        }
    }

    private static CompoundTag loadModels(Path avatarFolder, Path currentFile, BlockbenchModelParser parser, CompoundTag textures, ListTag animations, String folders) throws Exception {
        CompoundTag result = new CompoundTag();
        List<Path> subFiles = IOUtils.listPaths(currentFile);
        ListTag children = new ListTag();
        if (subFiles != null)
            for (Path file : subFiles) {
                if (IOUtils.isHidden(file))
                    continue;
                String name = IOUtils.getFileNameOrEmpty(file);
                if (Files.isDirectory(file)) {
                    CompoundTag subfolder = loadModels(avatarFolder, file, parser, textures, animations, folders + name + ".");
                    if (!subfolder.isEmpty()) {
                        subfolder.putString("name", name);
                        BlockbenchModelParser.parseParent(name, subfolder);
                        children.add(subfolder);
                    }
                } else if (file.toString().toLowerCase(Locale.US).endsWith(".bbmodel")) {
                    BlockbenchModelParser.ModelData data = parser.parseModel(avatarFolder, file, IOUtils.readFile(file), name.substring(0, name.length() - 8), folders);
                    children.add(data.modelNbt());
                    animations.addAll(data.animationList());

                    CompoundTag dataTag = data.textures();
                    if (dataTag.isEmpty())
                        continue;

                    if (textures.isEmpty()) {
                        textures.put("data", new ListTag());
                        textures.put("src", new CompoundTag());
                    }

                    textures.getList("data", Tag.TAG_COMPOUND).addAll(dataTag.getList("data", Tag.TAG_COMPOUND));
                    textures.getCompound("src").merge(dataTag.getCompound("src"));
                }
            }

        if (children.size() > 0)
            result.put("chld", children);

        return result;
    }

    /**
     * Tick the watched key for hotswapping avatars
     */
    public static void tick() {
        WatchEvent<?> event = null;
        boolean reload = false;

        for (Map.Entry<Path, WatchKey> entry : KEYS.entrySet()) {
            WatchKey key = entry.getValue();
            if (!key.isValid())
                continue;

            for (WatchEvent<?> watchEvent : key.pollEvents()) {
                WatchEvent.Kind<?> kind = watchEvent.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW)
                    continue;

                event = watchEvent;
                Path path = entry.getKey().resolve((Path) event.context());
                String name = IOUtils.getFileNameOrEmpty(path);

                if (IOUtils.isHiddenAvatarResource(path) || !(Files.isDirectory(path) || name.matches("(.*(\\.lua|\\.bbmodel|\\.ogg|\\.png)$|avatar\\.json)")))
                    continue;

                if (kind == StandardWatchEventKinds.ENTRY_CREATE && !IS_WINDOWS)
                    addWatchKey(path, KEYS::put);

                reload = true;
                break;
            }

            if (reload)
                break;
        }

        // reload avatar
        if (reload) {
            CosmeticMod.debug("Detected file changes in the Avatar directory (" + event.context().toString() + "), reloading!");
            if (lastLoadedPaths.size() > 1) AvatarManager.loadLocalAvatars(lastLoadedPaths);
            else AvatarManager.loadLocalAvatar(lastLoadedPath);
        }
    }

    public static void resetWatchKeys() {
        lastLoadedPath = null;
        lastLoadedPaths = List.of();
        for (WatchKey key : KEYS.values())
            key.cancel();
        KEYS.clear();
    }

    /**
     * register new watch keys
     *
     * @param path the path to register the watch key
     * @param consumer a consumer that will process the watch key and its path
     */
    protected static void addWatchKey(Path path, BiConsumer<Path, WatchKey> consumer) {
        if (watcher == null || path == null || path.getFileSystem() != FileSystems.getDefault())
            return;

        if (!Files.isDirectory(path) || IOUtils.isHidden(path))
            return;

        try {
            WatchEvent.Kind<?>[] events = {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY};
            WatchKey key = IS_WINDOWS ? path.register(watcher, events, com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE) : path.register(watcher, events);

            consumer.accept(path, key);

            List<Path> children = IOUtils.listPaths(path);
            if (children == null || IS_WINDOWS)
                return;

            for (Path child : children)
                addWatchKey(child, consumer);
        } catch (Exception e) {
            CosmeticMod.LOGGER.error("Failed to register watcher for " + path, e);
        }
    }

    public static Path getLastLoadedPath() {
        return lastLoadedPath;
    }

    public static String getLoadState() {
        return loadState.name().toLowerCase(Locale.US);
    }

    public static String getLoadError() {
        return loadError;
    }

    private enum LoadState {
        UNKNOWN,
        SCRIPTS,
        SOUNDS,
        MODELS,
        METADATA
    }
}
