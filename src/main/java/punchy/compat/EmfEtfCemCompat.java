package punchy.compat;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_2248;
import net.minecraft.class_2336;
import net.minecraft.class_2531;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_4739;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/EmfEtfCemCompat.class */
public final class EmfEtfCemCompat {
    private static final String[] EMF_IDS = {"entity_model_features", "emf"};
    private static final String[] ETF_IDS = {"entity_texture_features", "etf"};
    private static final String[] CEM_IDS = {"cem"};
    private static final String[] CEM_PATHS = {"emf/cem", "optifine/cem", "cem"};
    private static final String[] ETF_PATHS = {"etf", "optifine/random/entity", "optifine/emissive"};
    private static final EnumSet<ProxyPhysicsTarget> DISABLED = EnumSet.noneOf(ProxyPhysicsTarget.class);
    private static boolean ACTIVE;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/EmfEtfCemCompat$ProxyPhysicsTarget.class */
    public enum ProxyPhysicsTarget {
        ENCHANTING_TABLE,
        PISTON,
        CHEST,
        TRAPPED_CHEST,
        ENDER_CHEST
    }

    private EmfEtfCemCompat() {
    }

    public static void onResourceReload(class_3300 manager) {
        DISABLED.clear();
        ACTIVE = isAnyModLoaded();
        if (!ACTIVE || manager == null) {
            return;
        }
        for (String path : CEM_PATHS) {
            scan(manager, path, true);
        }
        if (isEtfLoaded()) {
            for (String path2 : ETF_PATHS) {
                scan(manager, path2, false);
            }
        }
    }

    public static boolean disableProxyPhysics(ProxyPhysicsTarget target) {
        return ACTIVE && DISABLED.contains(target);
    }

    public static ProxyPhysicsTarget resolveChestTarget(class_2248 block) {
        if (block instanceof class_2336) {
            return ProxyPhysicsTarget.ENDER_CHEST;
        }
        if (block instanceof class_2531) {
            return ProxyPhysicsTarget.TRAPPED_CHEST;
        }
        if (block instanceof class_4739) {
            return ProxyPhysicsTarget.CHEST;
        }
        return null;
    }

    private static boolean isAnyModLoaded() {
        return isEmfLoaded() || isEtfLoaded() || isCemLoaded();
    }

    private static boolean isEmfLoaded() {
        return isAnyLoaded(EMF_IDS);
    }

    private static boolean isEtfLoaded() {
        return isAnyLoaded(ETF_IDS);
    }

    private static boolean isCemLoaded() {
        return isAnyLoaded(CEM_IDS);
    }

    private static boolean isAnyLoaded(String[] ids) {
        if (ids == null || ids.length == 0) {
            return false;
        }
        for (String id : ids) {
            if (id != null && !id.isBlank() && Services.PLATFORM.isModLoaded(id)) {
                return true;
            }
        }
        return false;
    }

    private static void scan(class_3300 manager, String searchPath, boolean modelFilesOnly) {
        String path;
        Map<class_2960, class_3298> found = manager.method_14488(searchPath, id -> {
            return true;
        });
        if (found == null || found.isEmpty()) {
            return;
        }
        for (class_2960 id2 : found.keySet()) {
            if (id2 != null && (path = id2.method_12832()) != null) {
                String normalized = path.toLowerCase(Locale.ROOT);
                if (matchesExtension(normalized, modelFilesOnly)) {
                    registerMatch(normalized);
                }
            }
        }
    }

    private static boolean matchesExtension(String path, boolean modelFilesOnly) {
        if (path.endsWith(".jem") || path.endsWith(".jpm")) {
            return true;
        }
        if (modelFilesOnly) {
            return false;
        }
        if (path.endsWith(".properties") || path.endsWith(".png")) {
            return true;
        }
        return false;
    }

    private static void registerMatch(String path) {
        String name = stripExtension(fileName(path));
        if (name.isEmpty()) {
            return;
        }
        if (containsAny(name, "ender_chest")) {
            DISABLED.add(ProxyPhysicsTarget.ENDER_CHEST);
        }
        if (containsAny(name, "trapped_chest", "trapped_chest_left", "trapped_chest_right")) {
            DISABLED.add(ProxyPhysicsTarget.TRAPPED_CHEST);
        }
        if (containsAny(name, "chest", "chest_left", "chest_right")) {
            DISABLED.add(ProxyPhysicsTarget.CHEST);
        }
        if (containsAny(name, "enchanting_table", "enchantment_table")) {
            DISABLED.add(ProxyPhysicsTarget.ENCHANTING_TABLE);
        }
        if (containsAny(name, "piston", "sticky_piston")) {
            DISABLED.add(ProxyPhysicsTarget.PISTON);
        }
    }

    private static boolean containsAny(String name, String... tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && name.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf(47);
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf(46);
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
