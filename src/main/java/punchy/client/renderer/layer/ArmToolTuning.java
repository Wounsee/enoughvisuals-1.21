package punchy.client.renderer.layer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import punchy.client.renderer.layer.PlayerArmModelLayer;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmToolTuning.class */
public final class ArmToolTuning {
    private static Profile RIGHT_GLOBAL;
    private static Profile LEFT_GLOBAL;
    private static final EnumMap<PlayerArmModelLayer.ToolKind, Profile> PROFILES_RIGHT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);
    private static final EnumMap<PlayerArmModelLayer.ToolKind, Profile> PROFILES_LEFT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);
    private static final EnumMap<PlayerArmModelLayer.ToolKind, TransformOverride> OVERRIDES_RIGHT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);
    private static final EnumMap<PlayerArmModelLayer.ToolKind, TransformOverride> OVERRIDES_LEFT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);
    private static final Map<class_2960, SpecificEntry> ITEM_SPECIFICS = new HashMap();
    private static final EnumMap<PlayerArmModelLayer.ToolKind, Boolean> ATTACHED_RIGHT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);
    private static final EnumMap<PlayerArmModelLayer.ToolKind, Boolean> ATTACHED_LEFT = new EnumMap<>(PlayerArmModelLayer.ToolKind.class);

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmToolTuning$Profile.class */
    public static final class Profile {
        public float scale;
        public float offX;
        public float offY;
        public float offZ;
        public float rotX;
        public float rotY;
        public float rotZ;

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ) {
            this.scale = scale;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
        }

        public Profile mirror() {
            return new Profile(this.scale, -this.offX, this.offY, this.offZ, this.rotX, -this.rotY, -this.rotZ);
        }

        public void copyFrom(Profile other) {
            this.scale = other.scale;
            this.offX = other.offX;
            this.offY = other.offY;
            this.offZ = other.offZ;
            this.rotX = other.rotX;
            this.rotY = other.rotY;
            this.rotZ = other.rotZ;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmToolTuning$SpecificEntry.class */
    public static final class SpecificEntry {
        PlayerArmModelLayer.ToolKind kind;
        Profile right;
        Profile left;
        TransformOverride rightOverride = new TransformOverride();
        TransformOverride leftOverride = new TransformOverride();

        SpecificEntry(PlayerArmModelLayer.ToolKind kind) {
            this.kind = kind;
        }

        Profile get(boolean leftHand) {
            return leftHand ? this.left : this.right;
        }

        void set(boolean leftHand, Profile profile) {
            if (leftHand) {
                this.left = profile;
            } else {
                this.right = profile;
            }
        }

        TransformOverride getOverride(boolean leftHand) {
            return leftHand ? this.leftOverride : this.rightOverride;
        }

        public PlayerArmModelLayer.ToolKind kind() {
            return this.kind;
        }

        public Profile right() {
            return this.right;
        }

        public Profile left() {
            return this.left;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmToolTuning$SpecificSnapshot.class */
    public static final class SpecificSnapshot {
        private final PlayerArmModelLayer.ToolKind kind;
        private final Profile right;
        private final Profile left;

        public SpecificSnapshot(PlayerArmModelLayer.ToolKind kind, Profile right, Profile left) {
            this.kind = kind;
            this.right = right;
            this.left = left;
        }

        public PlayerArmModelLayer.ToolKind kind() {
            return this.kind;
        }

        public Profile right() {
            return this.right;
        }

        public Profile left() {
            return this.left;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/renderer/layer/ArmToolTuning$TransformOverride.class */
    private static final class TransformOverride {
        float scale;
        float offX;
        float offY;
        float offZ;
        float rotX;
        float rotY;
        float rotZ;

        private TransformOverride() {
        }

        void set(Profile base, Profile target) {
            this.scale = target.scale - base.scale;
            this.offX = target.offX - base.offX;
            this.offY = target.offY - base.offY;
            this.offZ = target.offZ - base.offZ;
            this.rotX = target.rotX - base.rotX;
            this.rotY = target.rotY - base.rotY;
            this.rotZ = target.rotZ - base.rotZ;
        }
    }

    static {
        resetDefaults();
    }

    private ArmToolTuning() {
    }

    public static void resetDefaults() {
        RIGHT_GLOBAL = new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        LEFT_GLOBAL = RIGHT_GLOBAL.mirror();
        PROFILES_RIGHT.clear();
        PROFILES_LEFT.clear();
        OVERRIDES_RIGHT.clear();
        OVERRIDES_LEFT.clear();
        ATTACHED_RIGHT.clear();
        ATTACHED_LEFT.clear();
        for (PlayerArmModelLayer.ToolKind kind : PlayerArmModelLayer.ToolKind.values()) {
            PROFILES_RIGHT.put(kind, copyProfile(RIGHT_GLOBAL));
            PROFILES_LEFT.put(kind, copyProfile(LEFT_GLOBAL));
            OVERRIDES_RIGHT.put(kind, new TransformOverride());
            OVERRIDES_LEFT.put(kind, new TransformOverride());
            ATTACHED_RIGHT.put(kind, true);
            ATTACHED_LEFT.put(kind, true);
        }
        ITEM_SPECIFICS.clear();
    }

    public static boolean isKindAttached(PlayerArmModelLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return true;
        }
        return ((Boolean) (left ? ATTACHED_LEFT : ATTACHED_RIGHT).getOrDefault(kind, true)).booleanValue();
    }

    public static void markKindDetached(PlayerArmModelLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return;
        }
        (left ? ATTACHED_LEFT : ATTACHED_RIGHT).put(kind, false);
    }

    public static void markKindAttached(PlayerArmModelLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return;
        }
        (left ? ATTACHED_LEFT : ATTACHED_RIGHT).put(kind, true);
        refreshKind(kind, left);
    }

    public static Profile get(PlayerArmModelLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return getGlobal(left);
        }
        return (left ? PROFILES_LEFT : PROFILES_RIGHT).get(kind);
    }

    public static Profile getGlobal(boolean left) {
        return left ? LEFT_GLOBAL : RIGHT_GLOBAL;
    }

    public static void apply(PlayerArmModelLayer.ToolKind kind, boolean left, Profile p) {
        if (p == null) {
            return;
        }
        if (kind == null) {
            applyGlobal(left, p);
            return;
        }
        Profile dst = get(kind, left);
        if (dst != null) {
            dst.copyFrom(p);
            commitKind(kind, left);
        }
    }

    public static void applyGlobal(boolean left, Profile p) {
        if (p == null) {
            return;
        }
        getGlobal(left).copyFrom(p);
        refresh(left);
    }

    public static void commitGlobal(boolean left) {
        refresh(left);
    }

    public static Profile snapshot(PlayerArmModelLayer.ToolKind kind, boolean left) {
        Profile s = get(kind, left);
        return copyProfile(s);
    }

    public static Profile snapshotGlobal(boolean left) {
        return snapshot(null, left);
    }

    private static Profile copyProfile(Profile src) {
        return new Profile(src.scale, src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ);
    }

    public static void commitKind(PlayerArmModelLayer.ToolKind kind, boolean left) {
        Profile base = getGlobal(left);
        Profile dst = get(kind, left);
        TransformOverride override = getOverride(kind, left);
        override.scale = dst.scale - base.scale;
        override.offX = dst.offX - base.offX;
        override.offY = dst.offY - base.offY;
        override.offZ = dst.offZ - base.offZ;
        override.rotX = dst.rotX;
        override.rotY = dst.rotY;
        override.rotZ = dst.rotZ;
    }

    private static TransformOverride getOverride(PlayerArmModelLayer.ToolKind kind, boolean left) {
        return (left ? OVERRIDES_LEFT : OVERRIDES_RIGHT).get(kind);
    }

    private static void refresh(boolean left) {
        for (PlayerArmModelLayer.ToolKind kind : PlayerArmModelLayer.ToolKind.values()) {
            refreshKind(kind, left);
        }
    }

    private static void refreshKind(PlayerArmModelLayer.ToolKind kind, boolean left) {
        Profile dst = get(kind, left);
        Profile base = getGlobal(left);
        if (dst == null || base == null) {
            return;
        }
        if (isKindAttached(kind, left)) {
            dst.copyFrom(base);
            return;
        }
        TransformOverride override = getOverride(kind, left);
        if (override == null) {
            return;
        }
        dst.scale = base.scale + override.scale;
        dst.offX = base.offX + override.offX;
        dst.offY = base.offY + override.offY;
        dst.offZ = base.offZ + override.offZ;
        dst.rotX = override.rotX;
        dst.rotY = override.rotY;
        dst.rotZ = override.rotZ;
    }

    private static void refreshSpecifics(boolean left) {
        ITEM_SPECIFICS.values().forEach(entry -> {
            refreshSpecificEntry(entry, left);
        });
    }

    private static void refreshSpecifics(PlayerArmModelLayer.ToolKind kind, boolean left) {
        ITEM_SPECIFICS.values().forEach(entry -> {
            if (entry != null && entry.kind == kind) {
                refreshSpecificEntry(entry, left);
            }
        });
    }

    private static Profile getSpecificBase(boolean left) {
        Profile global = getGlobal(left);
        if (global == null) {
            return new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        return new Profile(global.scale, global.offX, global.offY, global.offZ, 0.0f, 0.0f, 0.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void refreshSpecificEntry(SpecificEntry entry, boolean left) {
        Profile dst;
        Profile base;
        if (entry == null || (dst = entry.get(left)) == null || (base = getSpecificBase(left)) == null) {
            return;
        }
        TransformOverride override = entry.getOverride(left);
        dst.scale = base.scale + override.scale;
        dst.offX = base.offX + override.offX;
        dst.offY = base.offY + override.offY;
        dst.offZ = base.offZ + override.offZ;
        dst.rotX = override.rotX;
        dst.rotY = override.rotY;
        dst.rotZ = override.rotZ;
    }

    public static void commitSpecific(class_2960 id, boolean left) {
        SpecificEntry entry;
        Profile profile;
        Profile base;
        if (id == null || (entry = ITEM_SPECIFICS.get(id)) == null || (profile = entry.get(left)) == null || (base = getSpecificBase(left)) == null) {
            return;
        }
        TransformOverride override = entry.getOverride(left);
        override.scale = profile.scale - base.scale;
        override.offX = profile.offX - base.offX;
        override.offY = profile.offY - base.offY;
        override.offZ = profile.offZ - base.offZ;
        override.rotX = profile.rotX;
        override.rotY = profile.rotY;
        override.rotZ = profile.rotZ;
        refreshSpecificEntry(entry, left);
        pruneSpecificEntry(id);
    }

    private static class_2960 idFor(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        return class_7923.field_41178.method_10221(stack.method_7909());
    }

    public static Profile resolveProfile(class_1799 stack, PlayerArmModelLayer.ToolKind kind, boolean left) {
        SpecificEntry entry;
        Profile profile;
        class_2960 id = idFor(stack);
        if (id != null && (entry = ITEM_SPECIFICS.get(id)) != null && (profile = entry.get(left)) != null) {
            refreshSpecificEntry(entry, left);
            return profile;
        }
        return get(kind, left);
    }

    public static boolean hasSpecificProfile(class_1799 stack, boolean left) {
        SpecificEntry entry;
        class_2960 id = idFor(stack);
        return (id == null || (entry = ITEM_SPECIFICS.get(id)) == null || entry.get(left) == null) ? false : true;
    }

    public static void applySpecific(class_2960 id, PlayerArmModelLayer.ToolKind kind, Profile right, Profile left) {
        if (id == null || kind == null) {
            return;
        }
        if (right == null && left == null) {
            ITEM_SPECIFICS.remove(id);
            return;
        }
        SpecificEntry entry = ITEM_SPECIFICS.computeIfAbsent(id, key -> {
            return new SpecificEntry(kind);
        });
        entry.kind = kind;
        entry.right = right != null ? copyProfile(right) : null;
        entry.left = left != null ? copyProfile(left) : null;
        if (entry.right != null) {
            Profile base = getSpecificBase(false);
            if (base != null) {
                entry.rightOverride.scale = entry.right.scale - base.scale;
                entry.rightOverride.offX = entry.right.offX - base.offX;
                entry.rightOverride.offY = entry.right.offY - base.offY;
                entry.rightOverride.offZ = entry.right.offZ - base.offZ;
                entry.rightOverride.rotX = entry.right.rotX;
                entry.rightOverride.rotY = entry.right.rotY;
                entry.rightOverride.rotZ = entry.right.rotZ;
            }
            refreshSpecificEntry(entry, false);
        }
        if (entry.left != null) {
            Profile base2 = getSpecificBase(true);
            if (base2 != null) {
                entry.leftOverride.scale = entry.left.scale - base2.scale;
                entry.leftOverride.offX = entry.left.offX - base2.offX;
                entry.leftOverride.offY = entry.left.offY - base2.offY;
                entry.leftOverride.offZ = entry.left.offZ - base2.offZ;
                entry.leftOverride.rotX = entry.left.rotX;
                entry.leftOverride.rotY = entry.left.rotY;
                entry.leftOverride.rotZ = entry.left.rotZ;
            }
            refreshSpecificEntry(entry, true);
        }
        pruneSpecificEntry(id);
    }

    public static Profile getSpecificProfile(class_2960 id, PlayerArmModelLayer.ToolKind kind, boolean left) {
        if (id == null) {
            return null;
        }
        SpecificEntry entry = ITEM_SPECIFICS.computeIfAbsent(id, key -> {
            return new SpecificEntry(kind);
        });
        if (entry.kind == null) {
            entry.kind = kind;
        }
        Profile profile = entry.get(left);
        if (profile == null) {
            Profile seed = kind != null ? snapshot(kind, left) : getSpecificBase(left);
            profile = seed != null ? copyProfile(seed) : copyProfile(getSpecificBase(left));
            entry.set(left, profile);
            commitSpecific(id, left);
        } else {
            refreshSpecificEntry(entry, left);
        }
        return profile;
    }

    public static Map<class_2960, SpecificSnapshot> snapshotSpecifics() {
        Map<class_2960, SpecificSnapshot> copy = new HashMap<>();
        ITEM_SPECIFICS.forEach((id, entry) -> {
            refreshSpecificEntry(entry, false);
            refreshSpecificEntry(entry, true);
            copy.put(id, new SpecificSnapshot(entry.kind, entry.right != null ? copyProfile(entry.right) : null, entry.left != null ? copyProfile(entry.left) : null));
        });
        return copy;
    }

    public static void clearSpecificOverride(class_2960 id) {
        if (id == null) {
            return;
        }
        ITEM_SPECIFICS.remove(id);
    }

    public static void clearSpecifics() {
        ITEM_SPECIFICS.clear();
    }

    private static void pruneSpecificEntry(class_2960 id) {
        SpecificEntry entry = ITEM_SPECIFICS.get(id);
        if (entry != null && entry.right == null && entry.left == null) {
            ITEM_SPECIFICS.remove(id);
        }
    }

    private static boolean profileMatchesBase(Profile profile, Profile base) {
        return profile != null && base != null && nearlyEqual(profile.scale, base.scale) && nearlyEqual(profile.offX, base.offX) && nearlyEqual(profile.offY, base.offY) && nearlyEqual(profile.offZ, base.offZ) && nearlyEqual(profile.rotX, base.rotX) && nearlyEqual(profile.rotY, base.rotY) && nearlyEqual(profile.rotZ, base.rotZ);
    }

    private static boolean nearlyEqual(float a, float b) {
        return Math.abs(a - b) <= 1.0E-6f;
    }
}
