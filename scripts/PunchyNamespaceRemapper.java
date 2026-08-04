import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.Arrays;

/** Remaps the deterministically patched Punchy payload for Loom's named dev namespace. */
public final class PunchyNamespaceRemapper {
    private PunchyNamespaceRemapper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: PunchyNamespaceRemapper <input> <output> <mappings.tiny> <source-classpath>");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path mappings = Path.of(args[2]);
        Path[] sourceClasspath = Arrays.stream(args[3].split(
                        java.util.regex.Pattern.quote(File.pathSeparator)))
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .toArray(Path[]::new);
        Files.createDirectories(output);

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappings, "intermediary", "named"))
                .extension(new MixinExtension())
                .renameInvalidLocals(true)
                .rebuildSourceFilenames(true)
                .resolveMissing(true)
                .build();

        try (OutputConsumerPath consumer = new OutputConsumerPath(output)) {
            consumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);
            remapper.readClassPath(sourceClasspath);
            remapper.readInputs(input);
            remapper.apply(consumer);
        } finally {
            remapper.finish();
        }
    }
}
