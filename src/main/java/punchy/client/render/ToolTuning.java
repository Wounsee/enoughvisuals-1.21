package punchy.client.render;

import com.google.gson.JsonElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_7923;
import net.minecraft.class_9334;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning.class */
public final class ToolTuning {
    private static Profile GLOBAL_RIGHT;
    private static Profile GLOBAL_LEFT;
    private static Profile EMPTY_R;
    private static Profile EMPTY_L;
    private static Profile AXE_R;
    private static Profile AXE_L;
    private static Profile PICKAXE_R;
    private static Profile PICKAXE_L;
    private static Profile HOE_R;
    private static Profile HOE_L;
    private static Profile SWORD_R;
    private static Profile SWORD_L;
    private static Profile SHOVEL_R;
    private static Profile SHOVEL_L;
    private static Profile SHIELD_R;
    private static Profile SHIELD_L;
    private static Profile BLOCK_R;
    private static Profile BLOCK_L;
    private static Profile FULL_BLOCK_R;
    private static Profile FULL_BLOCK_L;
    private static Profile BUILDING_BLOCKS_R;
    private static Profile BUILDING_BLOCKS_L;
    private static Profile COLORED_BLOCKS_R;
    private static Profile COLORED_BLOCKS_L;
    private static Profile NATURAL_BLOCKS_R;
    private static Profile NATURAL_BLOCKS_L;
    private static Profile FUNCTIONAL_BLOCKS_R;
    private static Profile FUNCTIONAL_BLOCKS_L;
    private static Profile CHEST_R;
    private static Profile CHEST_L;
    private static Profile FLOWER_PENDULUM_R;
    private static Profile FLOWER_PENDULUM_L;
    private static Profile BOW_R;
    private static Profile BOW_L;
    private static Profile ARROW_R;
    private static Profile ARROW_L;
    private static Profile CROSSBOW_R;
    private static Profile CROSSBOW_L;
    private static Profile SPYGLASS_R;
    private static Profile SPYGLASS_L;
    private static Profile FLINT_R;
    private static Profile FLINT_L;
    private static Profile FOOD_R;
    private static Profile FOOD_L;
    private static Profile SHUBBA_R;
    private static Profile SHUBBA_L;
    private static Profile MACE_R;
    private static Profile MACE_L;
    private static Profile SPEAR_R;
    private static Profile SPEAR_L;
    private static Profile TRIDENT_R;
    private static Profile TRIDENT_L;
    private static Profile FISHING_ROD_R;
    private static Profile FISHING_ROD_L;
    private static Profile TORCH_R;
    private static Profile TORCH_L;
    private static Profile LANTERN_R;
    private static Profile LANTERN_L;
    private static Profile REDSTONE_R;
    private static Profile REDSTONE_L;
    private static Profile CREATURE_BUCKET_R;
    private static Profile CREATURE_BUCKET_L;
    private static Profile BUCKET_R;
    private static Profile BUCKET_L;
    private static Profile MINECART_R;
    private static Profile MINECART_L;
    private static Profile BOAT_R;
    private static Profile BOAT_L;
    private static Profile BUTTON_PLATE_R;
    private static Profile BUTTON_PLATE_L;
    private static Profile TRAPDOOR_R;
    private static Profile TRAPDOOR_L;
    private static Profile CARPET_R;
    private static Profile CARPET_L;
    private static Profile BED_R;
    private static Profile BED_L;
    private static Profile INGREDIENT_R;
    private static Profile INGREDIENT_L;
    private static final Map<VanillaFirstPersonItemLayer.ToolKind, TransformOverride> OVERRIDE_RIGHT = new EnumMap(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final Map<VanillaFirstPersonItemLayer.ToolKind, TransformOverride> OVERRIDE_LEFT = new EnumMap(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final Map<class_2960, SpecificEntry> ITEM_SPECIFICS = new HashMap();
    private static final Map<class_2960, Map<Integer, SpecificEntry>> ITEM_SPECIFICS_BY_MODEL = new HashMap();
    private static final Map<class_2960, Map<String, SpecificEntry>> ITEM_SPECIFICS_BY_RENAMED = new HashMap();
    private static final Map<class_2960, Map<Integer, Map<String, SpecificEntry>>> ITEM_SPECIFICS_BY_MODEL_AND_RENAMED = new HashMap();
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, Boolean> ATTACHED_RIGHT = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, Boolean> ATTACHED_LEFT = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, List<PunchyTuningConfig.CustomAnimationEntry>> GROUP_ANIMATIONS = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, List<PunchyTuningConfig.CustomAnimationEntry>> GROUP_ITEM_ANIMATIONS = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, List<String>> GROUP_PARTICLE_EMITTERS = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, List<PunchyTuningConfig.BetterCombatEntry>> GROUP_BETTER_COMBAT = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static List<PunchyTuningConfig.CustomAnimationEntry> DEFAULT_ANIMATIONS;
    private static List<PunchyTuningConfig.BetterCombatEntry> DEFAULT_BETTER_COMBAT;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$PhysicsMode.class */
    public enum PhysicsMode {
        NONE,
        FULL_PENDULUM,
        FORWARD_PENDULUM
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$RotationOrder.class */
    public enum RotationOrder {
        XYZ,
        ZXY
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$PhysicsSettings.class */
    public static final class PhysicsSettings {
        public Float limitDeg;
        public Float limitDegUp;
        public Float limitDegDown;
        public Float force;
        public Float speed;
        public Float cameraSens;
        public Float gravity;
        public Float motionSens;
        public Boolean invert;
        public Boolean handSpace;
        public Boolean enableCreaturePhysics;

        public PhysicsSettings copy() {
            PhysicsSettings copy = new PhysicsSettings();
            copy.limitDeg = this.limitDeg;
            copy.limitDegUp = this.limitDegUp;
            copy.limitDegDown = this.limitDegDown;
            copy.force = this.force;
            copy.speed = this.speed;
            copy.cameraSens = this.cameraSens;
            copy.gravity = this.gravity;
            copy.motionSens = this.motionSens;
            copy.invert = this.invert;
            copy.handSpace = this.handSpace;
            copy.enableCreaturePhysics = this.enableCreaturePhysics;
            return copy;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$Profile.class */
    public static final class Profile {
        public float scale;
        public float offX;
        public float offY;
        public float offZ;
        public float rotX;
        public float rotY;
        public float rotZ;
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public Boolean disableSprintLateralTilt;
        public Boolean keepArmTuning;
        public Boolean keepTriggerArmTuning;
        public PhysicsMode physicsMode;
        public PhysicsSettings physicsSettings;
        private float rotXRad;
        private float rotYRad;
        private float rotZRad;

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ) {
            this(scale, offX, offY, offZ, rotX, rotY, rotZ, 0.0f, 0.0f, 0.0f, PhysicsMode.NONE);
        }

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ, PhysicsMode physicsMode) {
            this(scale, offX, offY, offZ, rotX, rotY, rotZ, 0.0f, 0.0f, 0.0f, physicsMode);
        }

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ, float pivotX, float pivotY, float pivotZ) {
            this(scale, offX, offY, offZ, rotX, rotY, rotZ, pivotX, pivotY, pivotZ, PhysicsMode.NONE);
        }

        public Profile(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ, float pivotX, float pivotY, float pivotZ, PhysicsMode physicsMode) {
            this.scale = scale;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.physicsMode = physicsMode != null ? physicsMode : PhysicsMode.NONE;
            recomputeRadians();
        }

        public void recomputeRadians() {
            this.rotXRad = (float) Math.toRadians(this.rotX);
            this.rotYRad = (float) Math.toRadians(this.rotY);
            this.rotZRad = (float) Math.toRadians(this.rotZ);
        }

        public float rotXRad() {
            return this.rotXRad;
        }

        public float rotYRad() {
            return this.rotYRad;
        }

        public float rotZRad() {
            return this.rotZRad;
        }

        public Profile mirror() {
            Profile mirrored = new Profile(this.scale, -this.offX, this.offY, this.offZ, this.rotX, -this.rotY, -this.rotZ, -this.pivotX, this.pivotY, this.pivotZ, this.physicsMode);
            mirrored.disableSprintLateralTilt = this.disableSprintLateralTilt;
            mirrored.keepArmTuning = this.keepArmTuning;
            mirrored.keepTriggerArmTuning = this.keepTriggerArmTuning;
            if (this.physicsSettings != null) {
                mirrored.physicsSettings = this.physicsSettings.copy();
            }
            return mirrored;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$SpecificSnapshot.class */
    public static final class SpecificSnapshot {
        private final VanillaFirstPersonItemLayer.ToolKind kind;
        private final Profile right;
        private final Profile left;

        public SpecificSnapshot(VanillaFirstPersonItemLayer.ToolKind kind, Profile right, Profile left) {
            this.kind = kind;
            this.right = right;
            this.left = left;
        }

        public VanillaFirstPersonItemLayer.ToolKind kind() {
            return this.kind;
        }

        public Profile right() {
            return this.right;
        }

        public Profile left() {
            return this.left;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$SpecificSnapshotDetailed.class */
    public static final class SpecificSnapshotDetailed {
        private final class_2960 itemId;
        private final Integer customModelData;
        private final String renamed;
        private final String model;
        private final VanillaFirstPersonItemLayer.ToolKind kind;
        private final Profile right;
        private final Profile left;

        public SpecificSnapshotDetailed(class_2960 itemId, Integer customModelData, String renamed, String model, VanillaFirstPersonItemLayer.ToolKind kind, Profile right, Profile left) {
            this.itemId = itemId;
            this.customModelData = customModelData;
            this.renamed = renamed;
            this.model = model;
            this.kind = kind;
            this.right = right;
            this.left = left;
        }

        public class_2960 itemId() {
            return this.itemId;
        }

        public Integer customModelData() {
            return this.customModelData;
        }

        public String renamed() {
            return this.renamed;
        }

        public String model() {
            return this.model;
        }

        public VanillaFirstPersonItemLayer.ToolKind kind() {
            return this.kind;
        }

        public Profile right() {
            return this.right;
        }

        public Profile left() {
            return this.left;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$TransformOverride.class */
    private static final class TransformOverride {
        float scale;
        float offX;
        float offY;
        float offZ;
        float rotX;
        float rotY;
        float rotZ;
        float pivotX;
        float pivotY;
        float pivotZ;

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
            this.pivotX = target.pivotX - base.pivotX;
            this.pivotY = target.pivotY - base.pivotY;
            this.pivotZ = target.pivotZ - base.pivotZ;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/render/ToolTuning$SpecificEntry.class */
    private static final class SpecificEntry {
        VanillaFirstPersonItemLayer.ToolKind kind;
        String model;
        String renamed;
        List<PunchyTuningConfig.CustomAnimationEntry> customAnimation;
        List<PunchyTuningConfig.CustomAnimationEntry> customItemAnimations;
        List<String> particleEmitters;
        List<PunchyTuningConfig.BetterCombatEntry> betterCombat;
        JsonElement bonePhysics;
        Profile right;
        Profile left;

        SpecificEntry(VanillaFirstPersonItemLayer.ToolKind kind) {
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
    }

    static {
        resetDefaults();
    }

    private ToolTuning() {
    }

    public static RotationOrder rotationOrderForItemId(class_2960 itemId) {
        if (itemId == null) {
            return RotationOrder.XYZ;
        }
        String path = itemId.method_12832();
        if ("chest".equals(path) || "trapped_chest".equals(path) || "ender_chest".equals(path) || "piston".equals(path) || "sticky_piston".equals(path)) {
            return RotationOrder.ZXY;
        }
        return RotationOrder.XYZ;
    }

    public static void rotateProfileLocal(Profile profile, RotationOrder order, char axis, float deltaDegrees) {
        if (profile == null || order == null || deltaDegrees == 0.0f) {
            return;
        }
        Quaternionf rotation = quaternionFromProfile(profile, order);
        float radians = (float) Math.toRadians(deltaDegrees);
        Quaternionf delta = new Quaternionf();
        switch (Character.toUpperCase(axis)) {
            case 'X':
                delta.rotationX(radians);
                break;
            case 'Y':
                delta.rotationY(radians);
                break;
            case 'Z':
                delta.rotationZ(radians);
                break;
            default:
                return;
        }
        rotation.mul(delta);
        Vector3f euler = eulerFromQuaternion(rotation, order);
        profile.rotX = normalizeDegrees((float) Math.toDegrees(euler.x));
        profile.rotY = normalizeDegrees((float) Math.toDegrees(euler.y));
        profile.rotZ = normalizeDegrees((float) Math.toDegrees(euler.z));
        profile.recomputeRadians();
    }

    private static Quaternionf quaternionFromProfile(Profile profile, RotationOrder order) {
        Quaternionf rotation = new Quaternionf().identity();
        float rotX = (float) Math.toRadians(profile.rotX);
        float rotY = (float) Math.toRadians(profile.rotY);
        float rotZ = (float) Math.toRadians(profile.rotZ);
        if (order == RotationOrder.ZXY) {
            rotation.rotateZ(rotZ).rotateX(rotX).rotateY(rotY);
        } else {
            rotation.rotateX(rotX).rotateY(rotY).rotateZ(rotZ);
        }
        return rotation;
    }

    private static Vector3f eulerFromQuaternion(Quaternionf rotation, RotationOrder order) {
        Matrix3f matrix = new Matrix3f().set(rotation);
        return order == RotationOrder.ZXY ? extractEulerZxy(matrix) : extractEulerXyz(matrix);
    }

    private static Vector3f extractEulerXyz(Matrix3f matrix) {
        float x;
        float z;
        float y = (float) Math.asin(clampUnit(matrix.m02()));
        float cy = (float) Math.cos(y);
        if (Math.abs(cy) > 1.0E-5f) {
            x = (float) Math.atan2(-matrix.m12(), matrix.m22());
            z = (float) Math.atan2(-matrix.m01(), matrix.m00());
        } else {
            x = (float) Math.atan2(matrix.m21(), matrix.m11());
            z = 0.0f;
        }
        return new Vector3f(x, y, z);
    }

    private static Vector3f extractEulerZxy(Matrix3f matrix) {
        float y;
        float z;
        float x = (float) Math.asin(clampUnit(matrix.m21()));
        float cx = (float) Math.cos(x);
        if (Math.abs(cx) > 1.0E-5f) {
            y = (float) Math.atan2(-matrix.m20(), matrix.m22());
            z = (float) Math.atan2(-matrix.m01(), matrix.m11());
        } else {
            y = 0.0f;
            z = (float) Math.atan2(matrix.m10(), matrix.m00());
        }
        return new Vector3f(x, y, z);
    }

    private static float clampUnit(float value) {
        return Math.max(-1.0f, Math.min(1.0f, value));
    }

    private static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0f;
        if (normalized > 180.0f) {
            normalized -= 360.0f;
        } else if (normalized < -180.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    public static void resetDefaults() {
        resetAttachments();
        GROUP_ANIMATIONS.clear();
        GROUP_ITEM_ANIMATIONS.clear();
        GROUP_PARTICLE_EMITTERS.clear();
        GROUP_BETTER_COMBAT.clear();
        DEFAULT_ANIMATIONS = null;
        DEFAULT_BETTER_COMBAT = null;
        GLOBAL_RIGHT = new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        GLOBAL_LEFT = GLOBAL_RIGHT.mirror();
        EMPTY_R = blankProfile();
        EMPTY_L = blankProfile();
        AXE_R = blankProfile();
        AXE_L = blankProfile();
        PICKAXE_R = blankProfile();
        PICKAXE_L = blankProfile();
        HOE_R = blankProfile();
        HOE_L = blankProfile();
        SWORD_R = blankProfile();
        SWORD_L = blankProfile();
        SHOVEL_R = blankProfile();
        SHOVEL_L = blankProfile();
        SHIELD_R = blankProfile();
        SHIELD_L = blankProfile();
        BLOCK_R = blankProfile();
        BLOCK_L = blankProfile();
        FULL_BLOCK_R = blankProfile();
        FULL_BLOCK_L = blankProfile();
        BUILDING_BLOCKS_R = blankProfile();
        BUILDING_BLOCKS_L = blankProfile();
        COLORED_BLOCKS_R = blankProfile();
        COLORED_BLOCKS_L = blankProfile();
        NATURAL_BLOCKS_R = blankProfile();
        NATURAL_BLOCKS_L = blankProfile();
        FUNCTIONAL_BLOCKS_R = blankProfile();
        FUNCTIONAL_BLOCKS_L = blankProfile();
        CHEST_R = blankProfile();
        CHEST_L = blankProfile();
        FLOWER_PENDULUM_R = new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, PhysicsMode.FORWARD_PENDULUM);
        FLOWER_PENDULUM_L = FLOWER_PENDULUM_R.mirror();
        TORCH_R = blankProfile();
        TORCH_L = blankProfile();
        LANTERN_R = blankProfile();
        LANTERN_L = LANTERN_R.mirror();
        REDSTONE_R = blankProfile();
        REDSTONE_L = blankProfile();
        CREATURE_BUCKET_R = blankProfile();
        CREATURE_BUCKET_L = blankProfile();
        BUCKET_R = blankProfile();
        BUCKET_L = blankProfile();
        MINECART_R = blankProfile();
        MINECART_L = blankProfile();
        BOAT_R = blankProfile();
        BOAT_L = blankProfile();
        BUTTON_PLATE_R = blankProfile();
        BUTTON_PLATE_L = blankProfile();
        TRAPDOOR_R = blankProfile();
        TRAPDOOR_L = blankProfile();
        CARPET_R = blankProfile();
        CARPET_L = blankProfile();
        BED_R = blankProfile();
        BED_L = blankProfile();
        INGREDIENT_R = blankProfile();
        INGREDIENT_L = blankProfile();
        BOW_R = blankProfile();
        BOW_L = blankProfile();
        ARROW_R = blankProfile();
        ARROW_L = blankProfile();
        CROSSBOW_R = blankProfile();
        CROSSBOW_L = blankProfile();
        SPYGLASS_R = blankProfile();
        SPYGLASS_L = blankProfile();
        FLINT_R = blankProfile();
        FLINT_L = blankProfile();
        FOOD_R = blankProfile();
        FOOD_L = blankProfile();
        SHUBBA_R = blankProfile();
        SHUBBA_L = blankProfile();
        MACE_R = blankProfile();
        MACE_L = blankProfile();
        SPEAR_R = blankProfile();
        SPEAR_L = blankProfile();
        TRIDENT_R = blankProfile();
        TRIDENT_L = blankProfile();
        FISHING_ROD_R = blankProfile();
        FISHING_ROD_L = blankProfile();
        OVERRIDE_RIGHT.clear();
        OVERRIDE_LEFT.clear();
        for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
            OVERRIDE_RIGHT.put(kind, new TransformOverride());
            OVERRIDE_LEFT.put(kind, new TransformOverride());
        }
        ITEM_SPECIFICS.clear();
        refreshAll();
    }

    public static void applyGroupAnimation(VanillaFirstPersonItemLayer.ToolKind kind, List<PunchyTuningConfig.CustomAnimationEntry> anims) {
        if (kind != null && anims != null) {
            GROUP_ANIMATIONS.put(kind, anims);
        }
    }

    public static void setDefaultAnimations(List<PunchyTuningConfig.CustomAnimationEntry> anims) {
        if (anims == null || anims.isEmpty()) {
            DEFAULT_ANIMATIONS = null;
        } else {
            DEFAULT_ANIMATIONS = anims;
        }
    }

    public static void setDefaultBetterCombat(List<PunchyTuningConfig.BetterCombatEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            DEFAULT_BETTER_COMBAT = null;
        } else {
            DEFAULT_BETTER_COMBAT = entries;
        }
    }

    public static void setGroupAnimation(VanillaFirstPersonItemLayer.ToolKind kind, List<PunchyTuningConfig.CustomAnimationEntry> anims) {
        if (kind == null) {
            return;
        }
        if (anims == null || anims.isEmpty()) {
            GROUP_ANIMATIONS.remove(kind);
        } else {
            GROUP_ANIMATIONS.put(kind, anims);
        }
    }

    public static void applyGroupItemAnimation(VanillaFirstPersonItemLayer.ToolKind kind, List<PunchyTuningConfig.CustomAnimationEntry> anims) {
        if (kind != null && anims != null) {
            GROUP_ITEM_ANIMATIONS.put(kind, anims);
        }
    }

    public static void setGroupItemAnimation(VanillaFirstPersonItemLayer.ToolKind kind, List<PunchyTuningConfig.CustomAnimationEntry> anims) {
        if (kind == null) {
            return;
        }
        if (anims == null || anims.isEmpty()) {
            GROUP_ITEM_ANIMATIONS.remove(kind);
        } else {
            GROUP_ITEM_ANIMATIONS.put(kind, anims);
        }
    }

    public static void applyGroupParticleEmitters(VanillaFirstPersonItemLayer.ToolKind kind, List<String> emitters) {
        if (kind != null && emitters != null) {
            GROUP_PARTICLE_EMITTERS.put(kind, new ArrayList(emitters));
        }
    }

    public static void applyGroupBetterCombat(VanillaFirstPersonItemLayer.ToolKind kind, List<PunchyTuningConfig.BetterCombatEntry> anims) {
        if (kind != null && anims != null) {
            GROUP_BETTER_COMBAT.put(kind, anims);
        }
    }

    public static void resetAttachments() {
        for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
            ATTACHED_RIGHT.put(kind, true);
            ATTACHED_LEFT.put(kind, true);
        }
    }

    private static void setAttachmentState(VanillaFirstPersonItemLayer.ToolKind kind, boolean left, boolean attached) {
        if (kind == null) {
            return;
        }
        EnumMap<VanillaFirstPersonItemLayer.ToolKind, Boolean> map = left ? ATTACHED_LEFT : ATTACHED_RIGHT;
        map.put(kind, Boolean.valueOf(attached));
    }

    public static boolean isKindAttached(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return true;
        }
        return ((Boolean) (left ? ATTACHED_LEFT : ATTACHED_RIGHT).getOrDefault(kind, true)).booleanValue();
    }

    public static void markKindDetached(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        setAttachmentState(kind, left, false);
    }

    public static void markKindAttached(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        setAttachmentState(kind, left, true);
        refreshKind(kind, left);
    }

    private static Profile blankProfile() {
        return new Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static Profile get(VanillaFirstPersonItemLayer.ToolKind kind) {
        return get(kind, false);
    }

    public static Profile get(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        switch (kind) {
            case EMPTY_HAND:
                return left ? EMPTY_L : EMPTY_R;
            case AXE:
                return left ? AXE_L : AXE_R;
            case PICKAXE:
                return left ? PICKAXE_L : PICKAXE_R;
            case HOE:
                return left ? HOE_L : HOE_R;
            case SWORD:
                return left ? SWORD_L : SWORD_R;
            case SHOVEL:
                return left ? SHOVEL_L : SHOVEL_R;
            case SHIELD:
                return left ? SHIELD_L : SHIELD_R;
            case BLOCK:
                return left ? BLOCK_L : BLOCK_R;
            case FULL_BLOCK:
                return left ? FULL_BLOCK_L : FULL_BLOCK_R;
            case BUILDING_BLOCKS:
                return left ? BUILDING_BLOCKS_L : BUILDING_BLOCKS_R;
            case COLORED_BLOCKS:
                return left ? COLORED_BLOCKS_L : COLORED_BLOCKS_R;
            case NATURAL_BLOCKS:
                return left ? NATURAL_BLOCKS_L : NATURAL_BLOCKS_R;
            case FUNCTIONAL_BLOCKS:
                return left ? FUNCTIONAL_BLOCKS_L : FUNCTIONAL_BLOCKS_R;
            case CHEST:
                return left ? CHEST_L : CHEST_R;
            case FLOWER_PENDULUM:
                return left ? FLOWER_PENDULUM_L : FLOWER_PENDULUM_R;
            case CREATURE_BUCKET:
                return left ? CREATURE_BUCKET_L : CREATURE_BUCKET_R;
            case BUCKET:
                return left ? BUCKET_L : BUCKET_R;
            case MINECART:
                return left ? MINECART_L : MINECART_R;
            case BOAT:
                return left ? BOAT_L : BOAT_R;
            case BUTTON_PLATE:
                return left ? BUTTON_PLATE_L : BUTTON_PLATE_R;
            case TRAPDOOR:
                return left ? TRAPDOOR_L : TRAPDOOR_R;
            case CARPET:
                return left ? CARPET_L : CARPET_R;
            case BED:
                return left ? BED_L : BED_R;
            case TORCH:
                return left ? TORCH_L : TORCH_R;
            case LANTERN:
                return left ? LANTERN_L : LANTERN_R;
            case REDSTONE:
                return left ? REDSTONE_L : REDSTONE_R;
            case INGREDIENT:
                return left ? INGREDIENT_L : INGREDIENT_R;
            case BOW:
                return left ? BOW_L : BOW_R;
            case ARROW:
                return left ? ARROW_L : ARROW_R;
            case CROSSBOW:
                return left ? CROSSBOW_L : CROSSBOW_R;
            case SPYGLASS:
                return left ? SPYGLASS_L : SPYGLASS_R;
            case FLINT:
                return left ? FLINT_L : FLINT_R;
            case FOOD:
                return left ? FOOD_L : FOOD_R;
            case SHUBBA_DUCK:
                return left ? SHUBBA_L : SHUBBA_R;
            case MACE:
                return left ? MACE_L : MACE_R;
            case SPEAR:
                return left ? SPEAR_L : SPEAR_R;
            case TRIDENT:
                return left ? TRIDENT_L : TRIDENT_R;
            case FISHING_ROD:
                return left ? FISHING_ROD_L : FISHING_ROD_R;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public static Profile getGlobal(boolean left) {
        return left ? GLOBAL_LEFT : GLOBAL_RIGHT;
    }

    public static void applyGlobal(boolean left, Profile p) {
        if (p == null) {
            return;
        }
        Profile dst = getGlobal(left);
        copyAll(p, dst);
        dst.recomputeRadians();
        refresh(left);
    }

    public static Profile snapshotGlobal(boolean left) {
        Profile base = getGlobal(left);
        return copyProfile(base);
    }

    public static void commitGlobal(boolean left) {
        getGlobal(left).recomputeRadians();
        refresh(left);
    }

    public static void apply(VanillaFirstPersonItemLayer.ToolKind kind, boolean left, Profile p) {
        if (p == null) {
            return;
        }
        Profile dst = get(kind, left);
        copyAll(p, dst);
        dst.recomputeRadians();
        commitKind(kind, left);
    }

    public static void commitKind(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Profile base = getGlobal(left);
        Profile dst = get(kind, left);
        TransformOverride override = getOverride(kind, left);
        override.set(base, dst);
        refreshKind(kind, left);
    }

    public static Profile snapshot(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Profile s = get(kind, left);
        return copyProfile(s);
    }

    private static void refresh(boolean left) {
        for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
            refreshKind(kind, left);
        }
    }

    public static void refreshAll() {
        refresh(false);
        refresh(true);
    }

    private static void refreshKind(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Profile dst = get(kind, left);
        if (dst == null) {
            return;
        }
        if (!isKindAttached(kind, left)) {
            dst.recomputeRadians();
            return;
        }
        Profile base = getGlobal(left);
        TransformOverride override = getOverride(kind, left);
        if (base == null || override == null) {
            return;
        }
        copyAnchor(base, dst);
        dst.scale += override.scale;
        dst.offX += override.offX;
        dst.offY += override.offY;
        dst.offZ += override.offZ;
        dst.pivotX += override.pivotX;
        dst.pivotY += override.pivotY;
        dst.pivotZ += override.pivotZ;
        dst.rotX = base.rotX + override.rotX;
        dst.rotY = base.rotY + override.rotY;
        dst.rotZ = base.rotZ + override.rotZ;
        dst.recomputeRadians();
    }

    private static TransformOverride getOverride(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Map<VanillaFirstPersonItemLayer.ToolKind, TransformOverride> map = left ? OVERRIDE_LEFT : OVERRIDE_RIGHT;
        TransformOverride ovr = map.get(kind);
        if (ovr == null) {
            ovr = new TransformOverride();
            map.put(kind, ovr);
        }
        return ovr;
    }

    private static void copyAnchor(Profile src, Profile dst) {
        dst.scale = src.scale;
        dst.offX = src.offX;
        dst.offY = src.offY;
        dst.offZ = src.offZ;
        dst.pivotX = src.pivotX;
        dst.pivotY = src.pivotY;
        dst.pivotZ = src.pivotZ;
    }

    private static void copyAll(Profile src, Profile dst) {
        copyAnchor(src, dst);
        dst.rotX = src.rotX;
        dst.rotY = src.rotY;
        dst.rotZ = src.rotZ;
        dst.disableSprintLateralTilt = src.disableSprintLateralTilt;
        dst.keepArmTuning = src.keepArmTuning;
        dst.keepTriggerArmTuning = src.keepTriggerArmTuning;
        dst.physicsMode = src.physicsMode;
        dst.physicsSettings = src.physicsSettings != null ? src.physicsSettings.copy() : null;
    }

    private static Profile copyProfile(Profile src) {
        Profile copy = new Profile(src.scale, src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ, src.pivotX, src.pivotY, src.pivotZ, src.physicsMode);
        copy.disableSprintLateralTilt = src.disableSprintLateralTilt;
        copy.keepArmTuning = src.keepArmTuning;
        copy.keepTriggerArmTuning = src.keepTriggerArmTuning;
        if (src.physicsSettings != null) {
            copy.physicsSettings = src.physicsSettings.copy();
        }
        return copy;
    }

    private static Profile createSpecificSeedProfile(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Profile seeded = copyProfile(snapshot(kind, left));
        if (kind == VanillaFirstPersonItemLayer.ToolKind.BUCKET || kind == VanillaFirstPersonItemLayer.ToolKind.CREATURE_BUCKET) {
            seeded.physicsMode = PhysicsMode.NONE;
            seeded.physicsSettings = null;
        }
        return seeded;
    }

    private static class_2960 idFor(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        return class_7923.field_41178.method_10221(stack.method_7909());
    }

    public static VanillaFirstPersonItemLayer.ToolKind resolveOverriddenKind(class_1799 stack) {
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && entry.kind != null) {
            return entry.kind;
        }
        return null;
    }

    public static Profile resolveProfile(class_1799 stack, VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        Profile profile;
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && (profile = entry.get(left)) != null) {
            return profile;
        }
        return get(kind, left);
    }

    public static boolean hasSpecificProfile(class_1799 stack) {
        return getSpecificEntry(stack) != null;
    }

    public static JsonElement getSpecificBonePhysics(class_1799 stack) {
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry == null || entry.bonePhysics == null || entry.bonePhysics.isJsonNull()) {
            return null;
        }
        return entry.bonePhysics.deepCopy();
    }

    public static Profile getSpecificProfile(class_2960 id, VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
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
            profile = createSpecificSeedProfile(kind, left);
            entry.set(left, profile);
        }
        return profile;
    }

    public static Profile getSpecificProfile(class_1799 stack, VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        SpecificEntry entry;
        SpecificEntry entry2;
        Map<String, SpecificEntry> byRename;
        SpecificEntry entry3;
        class_2960 id = idFor(stack);
        if (id == null) {
            return null;
        }
        Integer cmd = resolveCustomModelData(stack);
        String renamed = resolveStackRenamedKey(stack);
        if (cmd != null) {
            applySpecific(id, cmd, kind, renamed, (String) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<PunchyTuningConfig.CustomAnimationEntry>) null, (List<String>) null, (List<PunchyTuningConfig.BetterCombatEntry>) null, (Profile) null, (Profile) null);
            Map<Integer, Map<String, SpecificEntry>> perModelAndRename = ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.get(id);
            if (renamed != null && perModelAndRename != null && (byRename = perModelAndRename.get(cmd)) != null && (entry3 = byRename.get(renamed)) != null) {
                Profile profile = entry3.get(left);
                if (profile == null) {
                    profile = createSpecificSeedProfile(kind, left);
                    entry3.set(left, profile);
                }
                return profile;
            }
            Map<Integer, SpecificEntry> perModel = ITEM_SPECIFICS_BY_MODEL.get(id);
            if (perModel != null && (entry2 = perModel.get(cmd)) != null) {
                Profile profile2 = entry2.get(left);
                if (profile2 == null) {
                    profile2 = createSpecificSeedProfile(kind, left);
                    entry2.set(left, profile2);
                }
                return profile2;
            }
        }
        if (renamed != null) {
            applySpecific(id, kind, renamed, null, null, null, null, null, null, null);
            Map<String, SpecificEntry> perRename = ITEM_SPECIFICS_BY_RENAMED.get(id);
            if (perRename != null && (entry = perRename.get(renamed)) != null) {
                Profile profile3 = entry.get(left);
                if (profile3 == null) {
                    profile3 = createSpecificSeedProfile(kind, left);
                    entry.set(left, profile3);
                }
                return profile3;
            }
        }
        return getSpecificProfile(id, kind, left);
    }

    public static Map<class_2960, SpecificSnapshot> snapshotSpecifics() {
        Map<class_2960, SpecificSnapshot> copy = new HashMap<>();
        ITEM_SPECIFICS.forEach((id, entry) -> {
            copy.put(id, new SpecificSnapshot(entry.kind, entry.right != null ? copyProfile(entry.right) : null, entry.left != null ? copyProfile(entry.left) : null));
        });
        return copy;
    }

    public static List<SpecificSnapshotDetailed> snapshotSpecificsDetailed() {
        List<SpecificSnapshotDetailed> out = new ArrayList<>();
        ITEM_SPECIFICS.forEach((id, entry) -> {
            out.add(toDetailedSnapshot(id, null, null, entry));
        });
        ITEM_SPECIFICS_BY_MODEL.forEach((id2, perModel) -> {
            if (perModel == null) {
                return;
            }
            perModel.forEach((cmd, entry2) -> {
                out.add(toDetailedSnapshot(id2, cmd, null, entry2));
            });
        });
        ITEM_SPECIFICS_BY_RENAMED.forEach((id3, perRename) -> {
            if (perRename == null) {
                return;
            }
            perRename.forEach((renamed, entry2) -> {
                out.add(toDetailedSnapshot(id3, null, renamed, entry2));
            });
        });
        ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.forEach((id4, perModelAndRename) -> {
            if (perModelAndRename == null) {
                return;
            }
            perModelAndRename.forEach((cmd, perRename2) -> {
                if (perRename2 == null) {
                    return;
                }
                perRename2.forEach((renamed, entry2) -> {
                    out.add(toDetailedSnapshot(id4, cmd, renamed, entry2));
                });
            });
        });
        return out;
    }

    private static SpecificSnapshotDetailed toDetailedSnapshot(class_2960 id, Integer cmd, String renamed, SpecificEntry entry) {
        if (entry == null) {
            return null;
        }
        return new SpecificSnapshotDetailed(id, cmd, renamed != null ? renamed : entry.renamed, entry.model, entry.kind, entry.right != null ? copyProfile(entry.right) : null, entry.left != null ? copyProfile(entry.left) : null);
    }

    public static List<PunchyTuningConfig.CustomAnimationEntry> getCustomAnimation(class_1799 stack) {
        return getCustomAnimationInternal(stack, true);
    }

    public static List<PunchyTuningConfig.CustomAnimationEntry> getCustomAnimationWithoutDefaults(class_1799 stack) {
        return getCustomAnimationInternal(stack, false);
    }

    private static List<PunchyTuningConfig.CustomAnimationEntry> getCustomAnimationInternal(class_1799 stack, boolean includeDefaults) {
        VanillaFirstPersonItemLayer.ToolKind kind;
        List<PunchyTuningConfig.CustomAnimationEntry> group;
        List<PunchyTuningConfig.CustomAnimationEntry> resolved = null;
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && entry.customAnimation != null) {
            resolved = entry.customAnimation;
        }
        if (resolved == null && (kind = ToolKindResolver.resolveItemKind(stack, null)) != null && (group = GROUP_ANIMATIONS.get(kind)) != null) {
            resolved = group;
        }
        return includeDefaults ? mergeAnimationEntries(resolved, DEFAULT_ANIMATIONS) : resolved;
    }

    public static boolean hasCustomAnimationType(class_1799 stack, String targetType) {
        List<PunchyTuningConfig.CustomAnimationEntry> entries;
        if (targetType == null || targetType.isBlank() || (entries = getCustomAnimation(stack)) == null || entries.isEmpty()) {
            return false;
        }
        String normalizedTarget = targetType.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        for (PunchyTuningConfig.CustomAnimationEntry entry : entries) {
            if (entry != null && entry.type != null) {
                String normalizedType = entry.type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
                if (normalizedTarget.equals(normalizedType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<PunchyTuningConfig.CustomAnimationEntry> getCustomItemAnimation(class_1799 stack) {
        VanillaFirstPersonItemLayer.ToolKind kind;
        SpecificEntry entry = getSpecificEntry(stack);
        List<PunchyTuningConfig.CustomAnimationEntry> resolved = null;
        if (entry != null) {
            if (entry.customItemAnimations != null) {
                resolved = entry.customItemAnimations;
            } else if (entry.customAnimation != null) {
                resolved = entry.customAnimation;
            }
        }
        if (resolved == null && (kind = ToolKindResolver.resolveItemKind(stack, null)) != null) {
            List<PunchyTuningConfig.CustomAnimationEntry> itemAnimations = GROUP_ITEM_ANIMATIONS.get(kind);
            if (itemAnimations != null) {
                resolved = itemAnimations;
            } else {
                resolved = GROUP_ANIMATIONS.get(kind);
            }
        }
        return mergeAnimationEntries(resolved, DEFAULT_ANIMATIONS);
    }

    private static List<PunchyTuningConfig.CustomAnimationEntry> mergeAnimationEntries(List<PunchyTuningConfig.CustomAnimationEntry> primary, List<PunchyTuningConfig.CustomAnimationEntry> defaults) {
        if ((primary == null || primary.isEmpty()) && (defaults == null || defaults.isEmpty())) {
            return null;
        }
        if (primary == null || primary.isEmpty()) {
            return defaults;
        }
        if (defaults == null || defaults.isEmpty()) {
            return primary;
        }
        LinkedHashMap<String, PunchyTuningConfig.CustomAnimationEntry> merged = new LinkedHashMap<>();
        for (PunchyTuningConfig.CustomAnimationEntry entry : primary) {
            String key = animationMergeKey(entry);
            if (key != null) {
                merged.put(key, entry);
            }
        }
        for (PunchyTuningConfig.CustomAnimationEntry entry2 : defaults) {
            String key2 = animationMergeKey(entry2);
            if (key2 != null && !merged.containsKey(key2)) {
                merged.put(key2, entry2);
            }
        }
        return new ArrayList(merged.values());
    }

    private static String animationMergeKey(PunchyTuningConfig.CustomAnimationEntry entry) {
        if (entry == null || entry.type == null) {
            return null;
        }
        String type = entry.type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        String key = entry.key == null ? "" : entry.key.trim().toLowerCase(Locale.ROOT);
        return type + "|" + key;
    }

    public static List<PunchyTuningConfig.BetterCombatEntry> getBetterCombat(class_1799 stack) {
        List<PunchyTuningConfig.BetterCombatEntry> group;
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && entry.betterCombat != null) {
            return entry.betterCombat;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, null);
        if (kind != null && (group = GROUP_BETTER_COMBAT.get(kind)) != null && !group.isEmpty()) {
            return group;
        }
        return DEFAULT_BETTER_COMBAT;
    }

    public static List<String> getParticleEmitters(class_1799 stack) {
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && entry.particleEmitters != null) {
            return entry.particleEmitters;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = ToolKindResolver.resolveItemKind(stack, null);
        if (kind != null) {
            return GROUP_PARTICLE_EMITTERS.get(kind);
        }
        return null;
    }

    public static List<PunchyTuningConfig.CustomAnimationEntry> getSpecificCustomAnimation(class_1799 stack) {
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null && entry.customAnimation != null) {
            return entry.customAnimation;
        }
        return null;
    }

    public static List<PunchyTuningConfig.CustomAnimationEntry> getSpecificCustomItemAnimation(class_1799 stack) {
        SpecificEntry entry = getSpecificEntry(stack);
        if (entry != null) {
            if (entry.customItemAnimations != null) {
                return entry.customItemAnimations;
            }
            if (entry.customAnimation != null) {
                return entry.customAnimation;
            }
            return null;
        }
        return null;
    }

    public static void applySpecific(class_2960 id, VanillaFirstPersonItemLayer.ToolKind kind, String model, List<PunchyTuningConfig.CustomAnimationEntry> customAnim, List<PunchyTuningConfig.CustomAnimationEntry> customItemAnim, List<String> particleEmitters, List<PunchyTuningConfig.BetterCombatEntry> betterCombat, Profile right, Profile left) {
        applySpecific(id, kind, (String) null, model, customAnim, customItemAnim, particleEmitters, betterCombat, (JsonElement) null, right, left);
    }

    public static void applySpecific(class_2960 id, VanillaFirstPersonItemLayer.ToolKind kind, String renamed, String model, List<PunchyTuningConfig.CustomAnimationEntry> customAnim, List<PunchyTuningConfig.CustomAnimationEntry> customItemAnim, List<String> particleEmitters, List<PunchyTuningConfig.BetterCombatEntry> betterCombat, Profile right, Profile left) {
        applySpecific(id, kind, renamed, model, customAnim, customItemAnim, particleEmitters, betterCombat, (JsonElement) null, right, left);
    }

    public static void applySpecific(class_2960 id, VanillaFirstPersonItemLayer.ToolKind kind, String renamed, String model, List<PunchyTuningConfig.CustomAnimationEntry> customAnim, List<PunchyTuningConfig.CustomAnimationEntry> customItemAnim, List<String> particleEmitters, List<PunchyTuningConfig.BetterCombatEntry> betterCombat, JsonElement bonePhysics, Profile right, Profile left) {
        SpecificEntry entry;
        if (id == null || kind == null) {
            return;
        }
        String renamedKey = normalizeRenamed(renamed);
        if (renamedKey != null) {
            Map<String, SpecificEntry> perRename = ITEM_SPECIFICS_BY_RENAMED.computeIfAbsent(id, key -> {
                return new HashMap();
            });
            entry = perRename.get(renamedKey);
            if (entry == null) {
                entry = new SpecificEntry(kind);
                perRename.put(renamedKey, entry);
            } else if (entry.kind == null) {
                entry.kind = kind;
            }
        } else {
            entry = ITEM_SPECIFICS.get(id);
            if (entry == null) {
                entry = new SpecificEntry(kind);
                ITEM_SPECIFICS.put(id, entry);
            } else if (entry.kind == null) {
                entry.kind = kind;
            }
        }
        entry.renamed = renamedKey != null ? renamedKey : entry.renamed;
        entry.model = model != null ? model : entry.model;
        entry.customAnimation = customAnim != null ? customAnim : entry.customAnimation;
        entry.customItemAnimations = customItemAnim != null ? customItemAnim : entry.customItemAnimations;
        entry.particleEmitters = particleEmitters != null ? new ArrayList<>(particleEmitters) : entry.particleEmitters;
        entry.betterCombat = betterCombat != null ? betterCombat : entry.betterCombat;
        entry.bonePhysics = (bonePhysics == null || bonePhysics.isJsonNull()) ? entry.bonePhysics : bonePhysics.deepCopy();
        if (right != null) {
            entry.right = copyProfile(right);
        }
        if (left != null) {
            entry.left = copyProfile(left);
        }
    }

    public static void applySpecific(class_2960 id, VanillaFirstPersonItemLayer.ToolKind kind, String model, Profile right, Profile left) {
        applySpecific(id, kind, model, null, null, null, null, right, left);
    }

    public static void applySpecific(class_2960 id, Integer customModelData, VanillaFirstPersonItemLayer.ToolKind kind, String renamed, String model, List<PunchyTuningConfig.CustomAnimationEntry> customAnim, List<PunchyTuningConfig.CustomAnimationEntry> customItemAnim, List<String> particleEmitters, List<PunchyTuningConfig.BetterCombatEntry> betterCombat, Profile right, Profile left) {
        applySpecific(id, customModelData, kind, renamed, model, customAnim, customItemAnim, particleEmitters, betterCombat, null, right, left);
    }

    public static void applySpecific(class_2960 id, Integer customModelData, VanillaFirstPersonItemLayer.ToolKind kind, String renamed, String model, List<PunchyTuningConfig.CustomAnimationEntry> customAnim, List<PunchyTuningConfig.CustomAnimationEntry> customItemAnim, List<String> particleEmitters, List<PunchyTuningConfig.BetterCombatEntry> betterCombat, JsonElement bonePhysics, Profile right, Profile left) {
        SpecificEntry entry;
        if (id == null || kind == null) {
            return;
        }
        if (customModelData == null) {
            applySpecific(id, kind, renamed, model, customAnim, customItemAnim, particleEmitters, betterCombat, bonePhysics, right, left);
            return;
        }
        String renamedKey = normalizeRenamed(renamed);
        if (renamedKey != null) {
            Map<Integer, Map<String, SpecificEntry>> perModelAndRename = ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.computeIfAbsent(id, key -> {
                return new HashMap();
            });
            Map<String, SpecificEntry> perRename = perModelAndRename.computeIfAbsent(customModelData, key2 -> {
                return new HashMap();
            });
            entry = perRename.get(renamedKey);
            if (entry == null) {
                entry = new SpecificEntry(kind);
                perRename.put(renamedKey, entry);
            } else if (entry.kind == null) {
                entry.kind = kind;
            }
        } else {
            Map<Integer, SpecificEntry> perModel = ITEM_SPECIFICS_BY_MODEL.computeIfAbsent(id, key3 -> {
                return new HashMap();
            });
            entry = perModel.get(customModelData);
            if (entry == null) {
                entry = new SpecificEntry(kind);
                perModel.put(customModelData, entry);
            } else if (entry.kind == null) {
                entry.kind = kind;
            }
        }
        entry.renamed = renamedKey != null ? renamedKey : entry.renamed;
        entry.model = model != null ? model : entry.model;
        entry.customAnimation = customAnim != null ? customAnim : entry.customAnimation;
        entry.customItemAnimations = customItemAnim != null ? customItemAnim : entry.customItemAnimations;
        entry.particleEmitters = particleEmitters != null ? new ArrayList<>(particleEmitters) : entry.particleEmitters;
        entry.betterCombat = betterCombat != null ? betterCombat : entry.betterCombat;
        entry.bonePhysics = (bonePhysics == null || bonePhysics.isJsonNull()) ? entry.bonePhysics : bonePhysics.deepCopy();
        if (right != null) {
            entry.right = copyProfile(right);
        }
        if (left != null) {
            entry.left = copyProfile(left);
        }
    }

    public static void clearSpecifics() {
        ITEM_SPECIFICS.clear();
        ITEM_SPECIFICS_BY_MODEL.clear();
        ITEM_SPECIFICS_BY_RENAMED.clear();
        ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.clear();
    }

    public static void clearSpecificOverride(class_2960 id) {
        if (id == null) {
            return;
        }
        ITEM_SPECIFICS.remove(id);
        ITEM_SPECIFICS_BY_MODEL.remove(id);
        ITEM_SPECIFICS_BY_RENAMED.remove(id);
        ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.remove(id);
    }

    public static void clearSpecificAnimationOverrides(class_2960 id) {
        if (id == null) {
            return;
        }
        SpecificEntry entry = ITEM_SPECIFICS.get(id);
        if (entry != null) {
            entry.customAnimation = null;
            entry.customItemAnimations = null;
        }
        Map<Integer, SpecificEntry> perModel = ITEM_SPECIFICS_BY_MODEL.get(id);
        if (perModel != null) {
            for (SpecificEntry modelEntry : perModel.values()) {
                if (modelEntry != null) {
                    modelEntry.customAnimation = null;
                    modelEntry.customItemAnimations = null;
                }
            }
        }
        Map<String, SpecificEntry> perRename = ITEM_SPECIFICS_BY_RENAMED.get(id);
        if (perRename != null) {
            for (SpecificEntry renamedEntry : perRename.values()) {
                if (renamedEntry != null) {
                    renamedEntry.customAnimation = null;
                    renamedEntry.customItemAnimations = null;
                }
            }
        }
        Map<Integer, Map<String, SpecificEntry>> perModelAndRename = ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.get(id);
        if (perModelAndRename == null) {
            return;
        }
        for (Map<String, SpecificEntry> byRename : perModelAndRename.values()) {
            if (byRename != null) {
                for (SpecificEntry renamedEntry2 : byRename.values()) {
                    if (renamedEntry2 != null) {
                        renamedEntry2.customAnimation = null;
                        renamedEntry2.customItemAnimations = null;
                    }
                }
            }
        }
    }

    public static void clearGroups() {
        GROUP_ANIMATIONS.clear();
        GROUP_ITEM_ANIMATIONS.clear();
        GROUP_PARTICLE_EMITTERS.clear();
        GROUP_BETTER_COMBAT.clear();
        DEFAULT_ANIMATIONS = null;
        DEFAULT_BETTER_COMBAT = null;
    }

    private static SpecificEntry getSpecificEntry(class_1799 stack) {
        Map<Integer, SpecificEntry> perModel;
        SpecificEntry entry;
        Map<String, SpecificEntry> perRename;
        SpecificEntry entry2;
        Map<Integer, Map<String, SpecificEntry>> perModelAndRename;
        Map<String, SpecificEntry> perRename2;
        SpecificEntry entry3;
        class_2960 id = idFor(stack);
        if (id == null) {
            return null;
        }
        Integer cmd = resolveCustomModelData(stack);
        String renamedKey = resolveStackRenamedKey(stack);
        if (cmd != null && renamedKey != null && (perModelAndRename = ITEM_SPECIFICS_BY_MODEL_AND_RENAMED.get(id)) != null && (perRename2 = perModelAndRename.get(cmd)) != null && (entry3 = perRename2.get(renamedKey)) != null) {
            return entry3;
        }
        if (renamedKey != null && (perRename = ITEM_SPECIFICS_BY_RENAMED.get(id)) != null && (entry2 = perRename.get(renamedKey)) != null) {
            return entry2;
        }
        if (cmd != null && (perModel = ITEM_SPECIFICS_BY_MODEL.get(id)) != null && (entry = perModel.get(cmd)) != null) {
            return entry;
        }
        return ITEM_SPECIFICS.get(id);
    }

    private static String resolveStackRenamedKey(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        try {
            if (!stack.method_57826(class_9334.field_49631)) {
                return null;
            }
            try {
                return normalizeRenamed(stack.method_7964().getString());
            } catch (Throwable th) {
                return null;
            }
        } catch (Throwable th2) {
            return null;
        }
    }

    public static String resolveStackRenamedKeyForDebug(class_1799 stack) {
        return resolveStackRenamedKey(stack);
    }

    private static String normalizeRenamed(String renamed) {
        if (renamed == null) {
            return null;
        }
        String normalized = renamed.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private static Integer resolveCustomModelData(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return null;
        }
        try {
            Object cmd = stack.method_58694(class_9334.field_49637);
            if (cmd == null) {
                return null;
            }
            if (cmd instanceof Integer) {
                return (Integer) cmd;
            }
            try {
                Method method = cmd.getClass().getMethod("value", new Class[0]);
                Object value = method.invoke(cmd, new Object[0]);
                if (value instanceof Integer) {
                    Integer i = (Integer) value;
                    return i;
                }
            } catch (Throwable th) {
            }
            try {
                Method method2 = cmd.getClass().getMethod("getValue", new Class[0]);
                Object value2 = method2.invoke(cmd, new Object[0]);
                if (value2 instanceof Integer) {
                    Integer i2 = (Integer) value2;
                    return i2;
                }
            } catch (Throwable th2) {
            }
            return null;
        } catch (Throwable th3) {
            return null;
        }
    }

    public static Integer resolveCustomModelDataForDebug(class_1799 stack) {
        return resolveCustomModelData(stack);
    }
}
