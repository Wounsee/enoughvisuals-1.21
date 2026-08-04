package punchy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import net.minecraft.class_156;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3283;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_7923;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import punchy.Punchy;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.io.PunchyAnimationLoader;
import punchy.client.render.ChestProxyTuning;
import punchy.client.render.EnchantParticleProxyTuning;
import punchy.client.render.FishingLineProxyTuning;
import punchy.client.render.FluidProxyTuning;
import punchy.client.render.PaddleProxyTuning;
import punchy.client.render.ToolKindResolver;
import punchy.client.render.ToolTuning;
import punchy.client.renderer.layer.ArmMeshTuning;
import punchy.client.renderer.layer.ArmToolTuning;
import punchy.client.renderer.layer.PlayerArmModelLayer;
import punchy.client.renderer.layer.VanillaFirstPersonItemLayer;
import punchy.client.state.SpearStateMachine;
import punchy.client.tuning.TuningProfileManager;
import punchy.config.PunchyConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig.class */
public final class PunchyTuningConfig {
    private static final String FILE_NAME = "punchy_tuning.json";
    private static final String MERGED_ANIMATION_SELECTION_FILE = "merged_animation_selection.json";
    private static final String COMPAT_DIR_NAME = "minecraft/punchy/compat";
    private static final float EPSILON = 1.0E-6f;
    private static final String ARM_MESH_SIDE_KEY = "global";
    private static final String MIX_DEFAULT_OPTION_ID = "punchy_default";
    private static final String BUILTIN_PACK_OPTION_ID = "pack:punchy:punchy";
    private static List<String> cachedSelectedResourcePackIdsHighToLow;
    private static class_3300 cachedSelectedResourcePackResourceManager;
    private static String cachedActiveBuiltInPackId;
    private static class_3300 cachedActiveBuiltInPackResourceManager;
    private static long selectedPackLookupWindowStartedNanos;
    private static int selectedPackLookupCount;
    private static int selectedPackCacheMissCount;
    private static int builtInPackLookupCount;
    private static Data BASELINE_STATE;
    private static Data INITIAL_STATE;
    private static Data DEFAULTS_RESOURCE_STATE;
    private static boolean APPLYING_COMPAT_OVERLAY;
    private static final Logger LOGGER = LoggerFactory.getLogger("Punchy/MixPacks");
    private static final boolean PERF_DEBUG = Boolean.getBoolean("punchy.perfDebug");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final class_2960 DEFAULTS_ID = class_2960.method_60655(Punchy.MOD_ID, "default_tuning.json");
    private static final String[] BUNDLED_COMPAT_PROFILES = {"items_compat"};
    private static final Set<String> MIX_HIDDEN_SPECIFIC_IDS = Set.of("adorablehamsterpets:cheese");
    private static final Map<String, List<String>> REGISTRY_CUSTOM_GROUPS = new HashMap();
    private static MixCatalog MIX_CATALOG = new MixCatalog();
    private static Set<String> MIX_COMPAT_OPTION_IDS = new LinkedHashSet();
    private static Set<String> MIX_PUNCHY_OPTION_IDS = new LinkedHashSet();
    private static final EnumMap<VanillaFirstPersonItemLayer.ToolKind, ItemRuntimeFlags> GROUP_RUNTIME_FLAGS = new EnumMap<>(VanillaFirstPersonItemLayer.ToolKind.class);
    private static final Map<class_2960, ItemRuntimeFlags> SPECIFIC_RUNTIME_FLAGS = new HashMap();
    private static final Set<String> SPEAR_MIX_CLIP_NAMES = Set.of("attack_spear", "use_spear_pose_1", "use_spear_pose_2", "use_spear_pose_3", "use_spear_pose_3_left", "use_spear_pose_3_right");
    private static final Set<String> SWORD_PASSIVE_CLIP_NAMES = Set.of((Object[]) new String[]{"attack", "attack_sword", "mining", "mining_foward", "mining_forward", "mining_sideways", "mining_digging", "mining_use_foward", "mining_use_forward", "mining_use_digging", "use_item", "useitem", "interaction", "interact", "inspect", "hand_in", "hand_out"});
    private static final Set<String> GENERIC_PASSIVE_CLIP_NAMES = Set.of("attack", "use_item", "useitem", "interaction", "interact", "inspect", "hand_in", "hand_out");
    private static final Map<String, List<String>> MIX_TYPE_CLIP_ALIASES = createMixTypeClipAliases();

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$AnimationRef.class */
    public static final class AnimationRef {
        public String type;
        public String name;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$BetterCombatEntry.class */
    public static final class BetterCombatEntry {
        public String type;
        public String attack_1;
        public String attack_2;
        public String attack_3;
        public String attack_4;
        public String attack_5;
        public String attack_6;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$BiomeDetection.class */
    public static final class BiomeDetection {
        public String biome;
        public List<String> biomes;
        public Integer radius;
        public Integer offsetX;
        public Integer offsetY;
        public Integer offsetZ;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$BlockDetection.class */
    public static final class BlockDetection {
        public String block;
        public List<String> blocks;
        public Integer radius;
        public Integer offsetX;
        public Integer offsetY;
        public Integer offsetZ;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$CustomAnimationEntry.class */
    public static final class CustomAnimationEntry {
        public String type;
        public List<BetterCombatEntry> betterCombat;
        public String key;

        @SerializedName(value = "min_blocks", alternate = {"minBlocks", "falling_min_blocks", "trigger_falling_min_blocks"})
        public Float min_blocks;
        public String var_1;
        public String var_2;
        public String var_3;
        public String attack_1;
        public String attack_2;
        public String attack_3;
        public String attack_4;
        public String attack_5;
        public String attack_6;
        public String critical_hit;
        public String name;
        public String animation;
        public String start;
        public String looping;
        public String end;

        @SerializedName(value = "throw", alternate = {"throw_animation", "cast", "start_cast"})
        public String throw_animation;

        @SerializedName(value = "pull", alternate = {"pull_animation", "reel", "reel_in"})
        public String pull_animation;

        @SerializedName(value = "disable_walk_on_water", alternate = {"disable_walkonwater", "disable_walk_on_water_v2", "disable_walk_on_water_animation"})
        public Boolean disable_walk_on_water;

        @SerializedName(value = "disable_dry_out", alternate = {"disable_dryout", "disable_dryout_animation"})
        public Boolean disable_dry_out;

        @SerializedName(value = "stage_1", alternate = {"stage1"})
        public String stage_1;

        @SerializedName(value = "stage_2", alternate = {"stage2"})
        public String stage_2;

        @SerializedName(value = "stage_3", alternate = {"stage3"})
        public String stage_3;
        public String keep_looping;
        public List<String> items;
        public JsonObject blockState;
        public Boolean in_water;
        public Boolean inside_water;
        public BlockDetection block_detection;
        public WeatherDetection weather_detection;
        public TimeDetection time_detection;
        public BiomeDetection biome_detection;
        public Boolean skip_blend_in;
        public Boolean skip_blend_out;

        @SerializedName(value = "blend_from_current_origin", alternate = {"blendFromCurrentOrigin", "rebase_blend_in", "rebaseBlendIn"})
        public Boolean blend_from_current_origin;
        public Boolean disable_swim_animation;
        public Boolean disable_all_particles;

        @SerializedName(value = "disable_physics_bones", alternate = {"disable_pendulum_bones"})
        public List<String> disable_physics_bones;
        public Float interfaceDelay;
        public Boolean stop_immediate;

        @SerializedName(value = "skip_hand_equip", alternate = {"disable_hand_equip", "no_hand_equip"})
        public Boolean skip_hand_equip;
        public Float loop_speed_max;
        public Float loop_speed_ramp;
        public Boolean isJavaBlock;
        public transient String mix_pack_option;
        public CustomFlags customFlags;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$CustomFlags.class */
    public static final class CustomFlags {

        @SerializedName(value = "sync_procedural_arm_animations", alternate = {"syncProceduralArmAnimations"})
        public Boolean syncProceduralArmAnimations;

        @SerializedName(value = "walk_and_sprint_force", alternate = {"walkAndSprintForce"})
        public Integer walkAndSprintForce;

        @SerializedName(value = "remaining_procedural_animations_force", alternate = {"remainingProceduralAnimationsForce"})
        public Integer remainingProceduralAnimationsForce;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$CustomGroupDefinition.class */
    public static final class CustomGroupDefinition {
        public String name;
        public List<String> items;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$Data.class */
    public static final class Data {

        @SerializedName(value = "default_animations", alternate = {"defaultAnimations"})
        public List<CustomAnimationEntry> default_animations;
        public SideSet globalItem;
        public SideSet globalArm;
        public EmptyHandEntry emptyHand;

        @SerializedName(value = "customGroups", alternate = {"custom_groups"})
        public List<CustomGroupDefinition> customGroups;
        public Map<String, GroupEntry> item = new HashMap();
        public Map<String, SideSet> armMesh = new HashMap();
        public Map<String, Pose> chestProxy = new HashMap();
        public Map<String, Pose> fluidProxy = new HashMap();
        public Map<String, Pose> enchantParticleProxy = new HashMap();
        public Map<String, Pose> paddleProxy = new HashMap();
        public Map<String, Pose> fishingLineProxy = new HashMap();
        public Map<String, SpecificEntry> itemSpecific = new HashMap();
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$EmptyHandEntry.class */
    public static final class EmptyHandEntry {
        public SideSet transforms;
        public JsonElement physics;

        @SerializedName(value = "disable_sprint_lateral_tilt", alternate = {"disableSprintLateralTilt"})
        public Boolean disableSprintLateralTilt;

        @SerializedName(value = "keep_arm_tuning", alternate = {"keepArmTuning"})
        public Boolean keepArmTuning;

        @SerializedName(value = "keep_trigger_arm_tuning", alternate = {"keepTriggerArmTuning", "keep_main_arm_tuning", "keepMainArmTuning"})
        public Boolean keepTriggerArmTuning;

        @SerializedName(value = "keep_arm_tuning_l", alternate = {"keepArmTuningL"})
        public Boolean keepArmTuningL;

        @SerializedName(value = "keep_arm_tuning_r", alternate = {"keepArmTuningR"})
        public Boolean keepArmTuningR;
        public SideSet armTransforms;
        public List<CustomAnimationEntry> customAnimation;
        public List<CustomAnimationEntry> customItemAnimations;
        public List<BetterCombatEntry> betterCombat;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$EnchantParticleProxySide.class */
    private enum EnchantParticleProxySide {
        LEFT,
        RIGHT,
        BOTH
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$FishingLineProxySide.class */
    private enum FishingLineProxySide {
        LEFT,
        RIGHT,
        BOTH
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$GroupEntry.class */
    public static final class GroupEntry {
        public SideSet transforms;
        public SideSet armTransforms;
        public JsonElement physics;

        @SerializedName(value = "disable_sprint_lateral_tilt", alternate = {"disableSprintLateralTilt"})
        public Boolean disableSprintLateralTilt;

        @SerializedName(value = "keep_arm_tuning", alternate = {"keepArmTuning"})
        public Boolean keepArmTuning;

        @SerializedName(value = "keep_trigger_arm_tuning", alternate = {"keepTriggerArmTuning", "keep_main_arm_tuning", "keepMainArmTuning"})
        public Boolean keepTriggerArmTuning;

        @SerializedName(value = "keep_arm_tuning_l", alternate = {"keepArmTuningL"})
        public Boolean keepArmTuningL;

        @SerializedName(value = "keep_arm_tuning_r", alternate = {"keepArmTuningR"})
        public Boolean keepArmTuningR;
        public List<CustomAnimationEntry> customAnimation;
        public List<CustomAnimationEntry> customItemAnimations;

        @SerializedName(value = "disable_walk_on_water", alternate = {"disable_walkonwater", "disable_walk_on_water_v2", "disable_walk_on_water_animation"})
        public Boolean disableWalkOnWater;

        @SerializedName(value = "disable_dry_out", alternate = {"disable_dryout", "disable_dryout_animation"})
        public Boolean disableDryOut;

        @SerializedName(value = "disable_swiming", alternate = {"disable_swimming", "disable_swim", "disable_swim_animation"})
        public Boolean disableSwiming;

        @SerializedName(value = "particle_emitters", alternate = {"particleEmitters"})
        public JsonElement particleEmitters;
        public JsonElement particle;
        public JsonElement particles;

        @SerializedName(value = "particle_effects", alternate = {"particleEffects"})
        public JsonElement particleEffects;

        @SerializedName(value = "particle_emission", alternate = {"particleEmission"})
        public JsonElement particleEmission;
        public List<BetterCombatEntry> betterCombat;
        public Pose right;
        public Pose left;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$MixSlotType.class */
    private enum MixSlotType {
        KIND,
        SPECIFIC
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$PaddleProxySide.class */
    private enum PaddleProxySide {
        LEFT,
        RIGHT,
        BOTH
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$PhysicsEntry.class */
    public static final class PhysicsEntry {
        public String name;
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
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$SideSet.class */
    public static final class SideSet {
        public Pose right;
        public Pose left;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$SpecificEntry.class */
    public static final class SpecificEntry {
        public String kind;
        public String armKind;
        public String renamed;
        public JsonElement physics;

        @SerializedName(value = "disable_sprint_lateral_tilt", alternate = {"disableSprintLateralTilt"})
        public Boolean disableSprintLateralTilt;

        @SerializedName(value = "keep_arm_tuning", alternate = {"keepArmTuning"})
        public Boolean keepArmTuning;

        @SerializedName(value = "keep_trigger_arm_tuning", alternate = {"keepTriggerArmTuning", "keep_main_arm_tuning", "keepMainArmTuning"})
        public Boolean keepTriggerArmTuning;

        @SerializedName(value = "keep_arm_tuning_l", alternate = {"keepArmTuningL"})
        public Boolean keepArmTuningL;

        @SerializedName(value = "keep_arm_tuning_r", alternate = {"keepArmTuningR"})
        public Boolean keepArmTuningR;
        public String model;

        @SerializedName(value = "customModelData", alternate = {"custom_model_data", "custommodeldata"})
        public Integer customModelData;
        public List<AnimationRef> animation;
        public List<CustomAnimationEntry> customAnimation;
        public List<CustomAnimationEntry> customItemAnimations;

        @SerializedName(value = "disable_walk_on_water", alternate = {"disable_walkonwater", "disable_walk_on_water_v2", "disable_walk_on_water_animation"})
        public Boolean disableWalkOnWater;

        @SerializedName(value = "disable_dry_out", alternate = {"disable_dryout", "disable_dryout_animation"})
        public Boolean disableDryOut;

        @SerializedName(value = "disable_swiming", alternate = {"disable_swimming", "disable_swim", "disable_swim_animation"})
        public Boolean disableSwiming;

        @SerializedName(value = "particle_emitters", alternate = {"particleEmitters"})
        public JsonElement particleEmitters;
        public JsonElement particle;
        public JsonElement particles;

        @SerializedName(value = "particle_effects", alternate = {"particleEffects"})
        public JsonElement particleEffects;

        @SerializedName(value = "particle_emission", alternate = {"particleEmission"})
        public JsonElement particleEmission;
        public List<BetterCombatEntry> betterCombat;

        @SerializedName(value = "bone_physics", alternate = {"bonePhysics"})
        public JsonElement bonePhysics;
        public SideSet transforms;
        public SideSet armTransforms;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$TimeDetection.class */
    public static final class TimeDetection {
        public Integer min;
        public Integer max;
        public List<Integer> range;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$WeatherDetection.class */
    public static final class WeatherDetection {
        public Boolean raining;
        public Boolean thundering;
        public Boolean clear;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$Pose.class */
    public static final class Pose {
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
        public String physics;

        @SerializedName(value = "disable_sprint_lateral_tilt", alternate = {"disableSprintLateralTilt"})
        public Boolean disableSprintLateralTilt;

        @SerializedName(value = "keep_arm_tuning", alternate = {"keepArmTuning"})
        public Boolean keepArmTuning;

        @SerializedName(value = "keep_trigger_arm_tuning", alternate = {"keepTriggerArmTuning", "keep_main_arm_tuning", "keepMainArmTuning"})
        public Boolean keepTriggerArmTuning;

        @SerializedName(value = "keep_arm_tuning_l", alternate = {"keepArmTuningL"})
        public Boolean keepArmTuningL;

        @SerializedName(value = "keep_arm_tuning_r", alternate = {"keepArmTuningR"})
        public Boolean keepArmTuningR;

        public Pose() {
        }

        public Pose(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ) {
            this(scale, offX, offY, offZ, rotX, rotY, rotZ, 0.0f, 0.0f, 0.0f, null);
        }

        public Pose(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ, float pivotX, float pivotY, float pivotZ) {
            this(scale, offX, offY, offZ, rotX, rotY, rotZ, pivotX, pivotY, pivotZ, null);
        }

        public Pose(float scale, float offX, float offY, float offZ, float rotX, float rotY, float rotZ, float pivotX, float pivotY, float pivotZ, String physics) {
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
            this.physics = physics;
        }

        public static Pose fromTool(ToolTuning.Profile p) {
            String phys = (p.physicsMode == null || p.physicsMode == ToolTuning.PhysicsMode.NONE) ? null : p.physicsMode.name();
            Pose pose = new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ, p.pivotX, p.pivotY, p.pivotZ, phys);
            pose.disableSprintLateralTilt = p.disableSprintLateralTilt;
            pose.keepArmTuning = p.keepArmTuning;
            pose.keepTriggerArmTuning = p.keepTriggerArmTuning;
            return pose;
        }

        public static Pose fromArm(ArmToolTuning.Profile p) {
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public ToolTuning.Profile toTool() {
            Boolean bool;
            ToolTuning.PhysicsMode mode = ToolTuning.PhysicsMode.NONE;
            if (this.physics != null && !this.physics.isBlank()) {
                try {
                    mode = ToolTuning.PhysicsMode.valueOf(this.physics.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                }
            }
            ToolTuning.Profile profile = new ToolTuning.Profile(this.scale, this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.pivotX, this.pivotY, this.pivotZ, mode);
            profile.disableSprintLateralTilt = this.disableSprintLateralTilt;
            if (this.keepArmTuning != null) {
                bool = this.keepArmTuning;
            } else {
                bool = this.keepArmTuningL != null ? this.keepArmTuningL : this.keepArmTuningR;
            }
            profile.keepArmTuning = bool;
            profile.keepTriggerArmTuning = this.keepTriggerArmTuning;
            return profile;
        }

        public ArmToolTuning.Profile toArm() {
            return new ArmToolTuning.Profile(this.scale, this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ);
        }

        public static Pose fromArmMesh(ArmMeshTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public ArmMeshTuning.Profile toArmMesh() {
            return new ArmMeshTuning.Profile(this.scale, this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ);
        }

        public static Pose fromChestProxy(ChestProxyTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public ChestProxyTuning.Profile toChestProxy() {
            return new ChestProxyTuning.Profile(this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.scale);
        }

        public static Pose fromFluidProxy(FluidProxyTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public FluidProxyTuning.Profile toFluidProxy() {
            return new FluidProxyTuning.Profile(this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.scale);
        }

        public static Pose fromEnchantParticleProxy(EnchantParticleProxyTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public EnchantParticleProxyTuning.Profile toEnchantParticleProxy() {
            return new EnchantParticleProxyTuning.Profile(this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.scale);
        }

        public static Pose fromPaddleProxy(PaddleProxyTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public PaddleProxyTuning.Profile toPaddleProxy() {
            return new PaddleProxyTuning.Profile(this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.scale);
        }

        public static Pose fromFishingLineProxy(FishingLineProxyTuning.Profile p) {
            if (p == null) {
                return null;
            }
            return new Pose(p.scale, p.offX, p.offY, p.offZ, p.rotX, p.rotY, p.rotZ);
        }

        public FishingLineProxyTuning.Profile toFishingLineProxy() {
            return new FishingLineProxyTuning.Profile(this.offX, this.offY, this.offZ, this.rotX, this.rotY, this.rotZ, this.scale);
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$MixSlotPayload.class */
    private static final class MixSlotPayload {
        VanillaFirstPersonItemLayer.ToolKind kind;
        GroupEntry groupEntry;
        EmptyHandEntry emptyHandEntry;
        SpecificEntry specificEntry;

        private MixSlotPayload() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$MixSlotDef.class */
    private static final class MixSlotDef {
        String slotId;
        String label;
        MixSlotType type;
        VanillaFirstPersonItemLayer.ToolKind kind;
        class_2960 itemId;
        final LinkedHashSet<String> optionOrder = new LinkedHashSet<>();
        final LinkedHashMap<String, MixSlotPayload> optionPayloads = new LinkedHashMap<>();
        final LinkedHashSet<class_2960> mergedSpecificItems = new LinkedHashSet<>();
        final LinkedHashMap<String, LinkedHashMap<class_2960, MixSlotPayload>> optionSpecificPayloads = new LinkedHashMap<>();
        final LinkedHashMap<String, LinkedHashSet<String>> optionClipNames = new LinkedHashMap<>();
        String resourceOrderOption;

        private MixSlotDef() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$MixCatalog.class */
    private static final class MixCatalog {
        final LinkedHashMap<String, MixSlotDef> slots = new LinkedHashMap<>();

        private MixCatalog() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$MixCatalogOrdering.class */
    private static final class MixCatalogOrdering {
        final LinkedHashMap<String, Integer> optionPriorities = new LinkedHashMap<>();
        final ArrayList<String> selectedOptionIdsByPriority = new ArrayList<>();

        private MixCatalogOrdering() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$CompatResourceEntry.class */
    private static final class CompatResourceEntry {
        final class_2960 id;
        final String profile;
        final class_3298 resource;
        final int packPriority;
        final int discoveryOrder;
        final int stackOrder;

        CompatResourceEntry(class_2960 id, String profile, class_3298 resource, int packPriority, int discoveryOrder, int stackOrder) {
            this.id = id;
            this.profile = profile;
            this.resource = resource;
            this.packPriority = packPriority;
            this.discoveryOrder = discoveryOrder;
            this.stackOrder = stackOrder;
        }
    }

    private PunchyTuningConfig() {
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyTuningConfig$ItemRuntimeFlags.class */
    private static final class ItemRuntimeFlags {
        Boolean disableWalkOnWater;
        Boolean disableDryOut;
        Boolean disableSwiming;

        private ItemRuntimeFlags() {
        }
    }

    private static void diag(String message) {
        if (message == null || message.isBlank()) {
        }
    }

    private static Map<String, List<String>> createMixTypeClipAliases() {
        Map<String, List<String>> aliases = new HashMap<>();
        aliases.put("attack", List.of("attack", "attack_sword", "attack_spear"));
        aliases.put("mining", List.of("mining", "mining_foward", "mining_forward", "mining_sideways", "mining_digging"));
        aliases.put("mining_v2", List.of("mining", "mining_foward", "mining_forward", "mining_sideways", "mining_digging"));
        aliases.put("useitem", List.of("use_item", "useitem", "interaction", "interact", "mining_use_foward", "mining_use_forward", "mining_use_digging"));
        aliases.put("interaction", List.of("interaction", "interact", "use_item", "useitem"));
        aliases.put("spear_charge", List.of("use_spear_pose_1", "use_spear_pose_1_left", "use_spear_pose_1_right", "use_spear_pose_2", "use_spear_pose_2_left", "use_spear_pose_2_right", "use_spear_pose_3", "use_spear_pose_3_left", "use_spear_pose_3_right"));
        aliases.put("inspect", List.of("inspect"));
        aliases.put("hand_in", List.of("hand_in", "handin"));
        aliases.put("hand_out", List.of("hand_out", "handout"));
        aliases.put("falling", List.of("falling"));
        aliases.put("triggerable", List.of("triggerable"));
        aliases.put("use_shield", List.of("use_shield"));
        aliases.put("use_bow", List.of("use_bow"));
        aliases.put("use_crossbow", List.of("use_crossbow"));
        aliases.put("use_crossbow_shot", List.of("use_crossbow_shot"));
        aliases.put("useeat", List.of("use_eat", "useeat"));
        aliases.put("usedrink", List.of("use_drink", "usedrink"));
        aliases.put("idle", List.of("idle"));
        return aliases;
    }

    private static boolean isMixThePacksEnabled() {
        return true;
    }

    public static List<String> getMixPackSlotIds() {
        if (!isMixThePacksEnabled()) {
            return List.of();
        }
        if (MIX_CATALOG.slots.isEmpty()) {
            return List.of();
        }
        return new ArrayList(MIX_CATALOG.slots.keySet());
    }

    public static String getMixPackSlotLabel(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return "";
        }
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        if (slot == null) {
            return slotId;
        }
        if (slot.type == MixSlotType.SPECIFIC && !isSpecificSlotForced(slot) && !shouldAutoUseSpecificSlot(slot)) {
            return slot.label + " (Inherit Kind)";
        }
        return slot.label;
    }

    public static List<String> getMixPackSlotOptionIds(String slotId) {
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        if (slot == null) {
            return List.of();
        }
        Set<String> activeOptions = getActiveMixPackOptionIds();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        List<String> ids = new ArrayList<>();
        ids.add("punchy_default");
        unique.add("punchy_default");
        if (slot.optionOrder != null && !slot.optionOrder.isEmpty()) {
            for (String optionId : slot.optionOrder) {
                if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId) && activeOptions.contains(optionId) && slotHasOptionPayload(slot, optionId) && unique.add(optionId)) {
                    ids.add(optionId);
                }
            }
        }
        return ids;
    }

    private static boolean slotHasOptionPayload(MixSlotDef slot, String optionId) {
        if (slot == null || optionId == null || optionId.isBlank() || "punchy_default".equals(optionId)) {
            return false;
        }
        MixSlotPayload payload = slot.optionPayloads.get(optionId);
        if (payload != null) {
            return true;
        }
        Map<class_2960, MixSlotPayload> specificPayloads = slot.optionSpecificPayloads.get(optionId);
        return (specificPayloads == null || specificPayloads.isEmpty()) ? false : true;
    }

    public static boolean isMixCompatOption(String optionId) {
        String normalized = normalizeMixOption(optionId);
        if (normalized == null || normalized.isBlank() || "punchy_default".equals(normalized)) {
            return false;
        }
        return MIX_COMPAT_OPTION_IDS.contains(normalized);
    }

    private static boolean isUserResourcePackId(String packId) {
        String normalized = normalizeMixOption(packId);
        return (normalized == null || normalized.isBlank() || "punchy_default".equals(normalized)) ? false : true;
    }

    public static String getMixPackSelectedOption(String slotId) {
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        if (slot == null) {
            return "punchy_default";
        }
        if (slot.type == MixSlotType.SPECIFIC && !isSpecificSlotForced(slot) && !shouldAutoUseSpecificSlot(slot)) {
            String inherited = resolveInheritedKindOption(slot);
            return inherited == null ? "punchy_default" : inherited;
        }
        return resolveSelectedOption(slot);
    }

    public static boolean setMixPackSelection(String slotId, String optionId) {
        MixSlotDef slot;
        String normalized;
        if (!isMixThePacksEnabled() || (slot = MIX_CATALOG.slots.get(slotId)) == null || (normalized = normalizeMixOption(optionId)) == null) {
            return false;
        }
        List<String> allowedOptions = getMixPackSlotOptionIds(slotId);
        if (!"punchy_default".equals(normalized) && !allowedOptions.contains(normalized)) {
            return false;
        }
        PunchyConfig.getAnimationPackSelection(slot.slotId);
        PunchyConfig.setAnimationPackSelection(slot.slotId, normalized);
        PunchyConfig.clearAnimationPackTypeSelections(slot.slotId);
        if (slot.type == MixSlotType.SPECIFIC) {
            boolean force = !"punchy_default".equals(normalized);
            PunchyConfig.setMixSpecificForceOverride(slot.slotId, force);
        }
        persistMixSelectionsToActiveProfile();
        return true;
    }

    public static List<String> getMixPackSlotEntryTypes(String slotId) {
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        if (slot == null) {
            return List.of();
        }
        LinkedHashSet<String> types = new LinkedHashSet<>();
        MixSlotPayload baseline = baselinePayloadForSlot(slot);
        collectEntryTypes(types, payloadCustomAnimation(baseline));
        collectEntryTypes(types, payloadCustomItemAnimations(baseline));
        for (MixSlotPayload payload : slot.optionPayloads.values()) {
            collectEntryTypes(types, payloadCustomAnimation(payload));
            collectEntryTypes(types, payloadCustomItemAnimations(payload));
        }
        return new ArrayList(types);
    }

    public static List<String> getMixPackExpandableEntryTypes(String slotId) {
        List<String> types = getMixPackSlotEntryTypes(slotId);
        if (types.isEmpty()) {
            return List.of();
        }
        List<String> expandable = new ArrayList<>();
        for (String type : types) {
            if (getMixPackSlotEntryTypeOptionIds(slotId, type).size() > 1) {
                expandable.add(type);
            }
        }
        return expandable;
    }

    public static List<String> getMixPackSlotEntryTypeOptionIds(String slotId, String entryType) {
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        String normalizedType = normalizeEntryType(entryType);
        if (slot == null || normalizedType == null) {
            return List.of();
        }
        Set<String> activeOptions = getActiveMixPackOptionIds();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add("punchy_default");
        for (String optionId : slot.optionOrder) {
            if (activeOptions.contains(optionId) && optionSupportsType(slot, optionId, normalizedType)) {
                ids.add(optionId);
            }
        }
        return new ArrayList(ids);
    }

    public static String getMixPackSelectedEntryTypeOption(String slotId, String entryType) {
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        String normalizedType = normalizeEntryType(entryType);
        if (slot == null || normalizedType == null) {
            return "punchy_default";
        }
        String main = getMixPackSelectedOption(slotId);
        return resolveSelectedTypeOption(slot, normalizedType, main);
    }

    public static boolean hasMixPackEntryTypeOverride(String slotId, String entryType) {
        String selected;
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        String normalizedType = normalizeEntryType(entryType);
        return (slot == null || normalizedType == null || (selected = PunchyConfig.getAnimationPackTypeSelection(slot.slotId, normalizedType)) == null || selected.isBlank()) ? false : true;
    }

    public static boolean setMixPackEntryTypeSelection(String slotId, String entryType, String optionId) {
        String strNormalizeMixOption;
        MixSlotDef slot = MIX_CATALOG.slots.get(slotId);
        String normalizedType = normalizeEntryType(entryType);
        if (slot == null || normalizedType == null || (strNormalizeMixOption = normalizeMixOption(optionId)) == null) {
            return false;
        }
        List<String> allowed = getMixPackSlotEntryTypeOptionIds(slotId, normalizedType);
        String mixPackSelectedOption = getMixPackSelectedOption(slotId);
        if (!allowed.contains(strNormalizeMixOption) && (mixPackSelectedOption == null || mixPackSelectedOption.isBlank() || !mixPackSelectedOption.equals(strNormalizeMixOption))) {
            return false;
        }
        PunchyConfig.getAnimationPackTypeSelection(slot.slotId, normalizedType);
        if (strNormalizeMixOption.equals(mixPackSelectedOption) || "punchy_default".equals(strNormalizeMixOption)) {
            PunchyConfig.setAnimationPackTypeSelection(slot.slotId, normalizedType, null);
        } else {
            PunchyConfig.setAnimationPackTypeSelection(slot.slotId, normalizedType, strNormalizeMixOption);
        }
        persistMixSelectionsToActiveProfile();
        PunchyConfig.getAnimationPackTypeSelection(slot.slotId, normalizedType);
        return true;
    }

    public static String getMixPreferredOptionForStack(class_1799 stack) {
        MixSlotDef slot = resolveMixSlotForStack(stack);
        if (slot == null) {
            return "punchy_default";
        }
        return resolveSelectedOption(slot);
    }

    public static String getGlobalMixPriorityOptionId() {
        for (String packId : getSelectedResourcePackIdsHighToLow()) {
            String optionId = normalizeMixOption(packId);
            if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                return optionId;
            }
        }
        return "punchy_default";
    }

    public static String getMixPreferredOptionForType(class_1799 stack, String entryType) {
        MixSlotDef slot = resolveMixSlotForStack(stack);
        String normalizedType = normalizeEntryType(entryType);
        if (slot == null || normalizedType == null) {
            return "punchy_default";
        }
        return resolveSelectedTypeOption(slot, normalizedType, resolveSelectedOption(slot));
    }

    public static String getMixPreferredOptionForEntry(class_1799 stack, CustomAnimationEntry entry) {
        String normalized;
        if (entry != null && entry.mix_pack_option != null && !entry.mix_pack_option.isBlank() && (normalized = normalizeMixOption(entry.mix_pack_option)) != null && !normalized.isBlank()) {
            return normalized;
        }
        MixSlotDef slot = resolveMixSlotForStack(stack);
        if (slot == null) {
            return "punchy_default";
        }
        return resolveSelectedOption(slot);
    }

    private static MixSlotDef resolveMixSlotForStack(class_1799 stack) {
        if (MIX_CATALOG == null || MIX_CATALOG.slots.isEmpty()) {
            return null;
        }
        if (stack == null || stack.method_7960()) {
            return MIX_CATALOG.slots.get("kind:empty_hand");
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId == null) {
            return null;
        }
        String specificSlotId = "specific:" + String.valueOf(itemId);
        MixSlotDef specificSlot = MIX_CATALOG.slots.get(specificSlotId);
        if (specificSlot != null && (isSpecificSlotForced(specificSlot) || shouldAutoUseSpecificSlot(specificSlot))) {
            return specificSlot;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = resolveMixKindForStack(stack);
        if (kind == null) {
            return null;
        }
        String kindSlotId = "kind:" + kind.name().toLowerCase(Locale.ROOT);
        return MIX_CATALOG.slots.get(kindSlotId);
    }

    public static void resetMixPackSelectionsToResourcepackOrder() {
        PunchyConfig.clearAnimationPackSelections();
        persistMixSelectionsToActiveProfile();
    }

    public static void clearGroupsRegistry() {
        REGISTRY_CUSTOM_GROUPS.clear();
    }

    public static boolean matchesItemList(class_1799 stack, List<String> items) {
        if (stack == null || items == null || items.isEmpty()) {
            return false;
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        String current = itemId == null ? null : itemId.toString();
        if (current == null || current.isBlank()) {
            return false;
        }
        for (String entry : items) {
            if (entry != null) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                } else {
                    List<String> groupItems = resolveCustomGroupItems(trimmed);
                    if (groupItems != null && groupItems.contains(current)) {
                        return true;
                    }
                    class_2960 parsed = class_2960.method_12829(trimmed);
                    if (parsed != null && current.equals(parsed.toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<String> resolveCustomGroupItems(String name) {
        if (name == null) {
            return null;
        }
        List<String> items = REGISTRY_CUSTOM_GROUPS.get(name);
        if (items != null) {
            return items;
        }
        for (Map.Entry<String, List<String>> entry : REGISTRY_CUSTOM_GROUPS.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void applyItemLevelFishingRodFlags(List<CustomAnimationEntry> entries, Boolean disableWalkOnWater, Boolean disableDryOut, Boolean disableSwiming) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (CustomAnimationEntry entry : entries) {
            if (entry != null && entry.type != null) {
                String normalized = entry.type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
                if ("fishingrod".equals(normalized)) {
                    if (entry.disable_walk_on_water == null && disableWalkOnWater != null) {
                        entry.disable_walk_on_water = disableWalkOnWater;
                    }
                    if (entry.disable_dry_out == null && disableDryOut != null) {
                        entry.disable_dry_out = disableDryOut;
                    }
                    if (entry.disable_swim_animation == null && disableSwiming != null) {
                        entry.disable_swim_animation = disableSwiming;
                    }
                }
            }
        }
    }

    private static void applyGroupRuntimeFlags(VanillaFirstPersonItemLayer.ToolKind kind, Boolean disableWalkOnWater, Boolean disableDryOut, Boolean disableSwiming) {
        if (kind == null) {
            return;
        }
        if (disableWalkOnWater == null && disableDryOut == null && disableSwiming == null) {
            return;
        }
        ItemRuntimeFlags flags = (ItemRuntimeFlags) GROUP_RUNTIME_FLAGS.computeIfAbsent(kind, ignored -> {
            return new ItemRuntimeFlags();
        });
        if (disableWalkOnWater != null) {
            flags.disableWalkOnWater = disableWalkOnWater;
        }
        if (disableDryOut != null) {
            flags.disableDryOut = disableDryOut;
        }
        if (disableSwiming != null) {
            flags.disableSwiming = disableSwiming;
        }
    }

    private static void applySpecificRuntimeFlags(class_2960 itemId, Boolean disableWalkOnWater, Boolean disableDryOut, Boolean disableSwiming) {
        if (itemId == null) {
            return;
        }
        if (disableWalkOnWater == null && disableDryOut == null && disableSwiming == null) {
            return;
        }
        ItemRuntimeFlags flags = SPECIFIC_RUNTIME_FLAGS.computeIfAbsent(itemId, ignored -> {
            return new ItemRuntimeFlags();
        });
        if (disableWalkOnWater != null) {
            flags.disableWalkOnWater = disableWalkOnWater;
        }
        if (disableDryOut != null) {
            flags.disableDryOut = disableDryOut;
        }
        if (disableSwiming != null) {
            flags.disableSwiming = disableSwiming;
        }
    }

    public static boolean isWalkOnWaterDisabledForStack(class_1799 stack) {
        ItemRuntimeFlags group;
        ItemRuntimeFlags specific;
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId != null && (specific = SPECIFIC_RUNTIME_FLAGS.get(itemId)) != null && Boolean.TRUE.equals(specific.disableWalkOnWater)) {
            return true;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = resolveMixKindForStack(stack);
        return (kind == null || (group = GROUP_RUNTIME_FLAGS.get(kind)) == null || !Boolean.TRUE.equals(group.disableWalkOnWater)) ? false : true;
    }

    public static boolean isDryOutDisabledForStack(class_1799 stack) {
        ItemRuntimeFlags group;
        ItemRuntimeFlags specific;
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId != null && (specific = SPECIFIC_RUNTIME_FLAGS.get(itemId)) != null && Boolean.TRUE.equals(specific.disableDryOut)) {
            return true;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = resolveMixKindForStack(stack);
        return (kind == null || (group = GROUP_RUNTIME_FLAGS.get(kind)) == null || !Boolean.TRUE.equals(group.disableDryOut)) ? false : true;
    }

    public static boolean isSwimingDisabledForStack(class_1799 stack) {
        ItemRuntimeFlags group;
        ItemRuntimeFlags specific;
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_2960 itemId = class_7923.field_41178.method_10221(stack.method_7909());
        if (itemId != null && (specific = SPECIFIC_RUNTIME_FLAGS.get(itemId)) != null && Boolean.TRUE.equals(specific.disableSwiming)) {
            return true;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = resolveMixKindForStack(stack);
        return (kind == null || (group = GROUP_RUNTIME_FLAGS.get(kind)) == null || !Boolean.TRUE.equals(group.disableSwiming)) ? false : true;
    }

    private static List<String> readCompatParticleEffects(SpecificEntry entry) {
        if (entry == null) {
            return null;
        }
        List<String> out = new ArrayList<>();
        appendCompatParticleEffects(out, entry.particleEmitters);
        appendCompatParticleEffects(out, entry.particle);
        appendCompatParticleEffects(out, entry.particles);
        appendCompatParticleEffects(out, entry.particleEffects);
        appendCompatParticleEffects(out, entry.particleEmission);
        if (out.isEmpty()) {
            return null;
        }
        return out;
    }

    private static List<String> readCompatParticleEffects(GroupEntry entry) {
        if (entry == null) {
            return null;
        }
        List<String> out = new ArrayList<>();
        appendCompatParticleEffects(out, entry.particleEmitters);
        appendCompatParticleEffects(out, entry.particle);
        appendCompatParticleEffects(out, entry.particles);
        appendCompatParticleEffects(out, entry.particleEffects);
        appendCompatParticleEffects(out, entry.particleEmission);
        if (out.isEmpty()) {
            return null;
        }
        return out;
    }

    private static void appendCompatParticleEffects(List<String> out, JsonElement element) {
        String normalized;
        if (out == null || element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                appendCompatParticleEffects(out, child);
            }
            return;
        }
        if (element.isJsonObject()) {
            String script = compatParticleObjectToScript(element.getAsJsonObject());
            if (script != null && !script.isBlank()) {
                out.add(script);
                return;
            }
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && (normalized = normalizeCompatParticleEffect(element.getAsString())) != null && !normalized.isBlank()) {
            out.add(normalized);
        }
    }

    private static String normalizeCompatParticleEffect(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("particle(") && trimmed.endsWith(")")) {
            return trimmed;
        }
        if (trimmed.contains("=")) {
            return "particle(" + trimmed + ")";
        }
        class_2960 parsed = class_2960.method_12829(trimmed);
        if (parsed != null) {
            return "particle(type=" + String.valueOf(parsed) + ")";
        }
        return null;
    }

    private static String compatParticleObjectToScript(JsonObject object) {
        if (object == null) {
            return null;
        }
        if (object.has("enabled") && object.get("enabled").isJsonPrimitive() && !readCompatBoolean(object.get("enabled"), true)) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank() && !"enabled".equalsIgnoreCase(key) && !isIgnoredCompatParticleRenderHint(key)) {
                if ("triggers".equalsIgnoreCase(key) && entry.getValue() != null && !entry.getValue().isJsonNull()) {
                    appendCompatTriggerTokens(joiner, entry.getValue());
                } else if ("on_animation".equalsIgnoreCase(key) && entry.getValue() != null && entry.getValue().isJsonObject()) {
                    appendCompatOnAnimationTokens(joiner, entry.getValue().getAsJsonObject());
                } else {
                    String value = compatParticleValueToToken(entry.getValue());
                    if (value != null && !value.isBlank()) {
                        joiner.add(key.trim() + "=" + value);
                    }
                }
            }
        }
        String body = joiner.toString();
        if (body.isBlank()) {
            return null;
        }
        return "particle(" + body + ")";
    }

    private static boolean isIgnoredCompatParticleRenderHint(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return "emissive_strength".equals(normalized) || "emission_strength".equals(normalized) || "emissive_blur".equals(normalized) || "emission_blur".equals(normalized) || "emissive_clamp".equals(normalized) || "emission_clamp".equals(normalized);
    }

    private static String compatParticleValueToToken(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        if (value.isJsonArray()) {
            JsonArray<JsonElement> arr = value.getAsJsonArray();
            boolean allPrimitive = true;
            for (JsonElement part : arr) {
                if (part == null || !part.isJsonPrimitive()) {
                    allPrimitive = false;
                    break;
                }
            }
            if (!allPrimitive) {
                return null;
            }
            if (arr.size() == 3) {
                StringJoiner vecJoiner = new StringJoiner(":");
                for (JsonElement part2 : arr) {
                    vecJoiner.add(part2.getAsString());
                }
                return vecJoiner.toString();
            }
            StringJoiner listJoiner = new StringJoiner("|");
            for (JsonElement part3 : arr) {
                listJoiner.add(part3.getAsString());
            }
            return listJoiner.toString();
        }
        if (value.isJsonObject()) {
            JsonObject obj = value.getAsJsonObject();
            if (obj.has("x") && obj.has("y") && obj.has("z") && obj.get("x").isJsonPrimitive() && obj.get("y").isJsonPrimitive() && obj.get("z").isJsonPrimitive()) {
                return obj.get("x").getAsString() + ":" + obj.get("y").getAsString() + ":" + obj.get("z").getAsString();
            }
            return null;
        }
        return null;
    }

    private static void appendCompatOnAnimationTokens(StringJoiner joiner, JsonObject onAnimation) {
        String n;
        if (joiner == null || onAnimation == null) {
            return;
        }
        JsonElement name = onAnimation.get("name");
        JsonElement timecode = onAnimation.get("timecode");
        if (name != null && name.isJsonPrimitive() && (n = name.getAsString()) != null && !n.isBlank()) {
            joiner.add("on_animation_name=" + n.trim());
        }
        if (timecode != null && timecode.isJsonPrimitive()) {
            joiner.add("on_animation_timecode=" + timecode.getAsString());
        }
    }

    private static void appendCompatTriggerTokens(StringJoiner joiner, JsonElement triggersElement) {
        if (joiner == null || triggersElement == null || triggersElement.isJsonNull()) {
            return;
        }
        if (triggersElement.isJsonObject()) {
            appendCompatTriggerTokens(joiner, triggersElement.getAsJsonObject());
        } else if (triggersElement.isJsonArray()) {
            appendCompatTriggerList(joiner, triggersElement.getAsJsonArray());
        } else if (triggersElement.isJsonPrimitive()) {
            appendCompatNamedTrigger(joiner, triggersElement.getAsString());
        }
    }

    private static void appendCompatTriggerTokens(StringJoiner joiner, JsonObject triggers) {
        if (joiner == null || triggers == null) {
            return;
        }
        appendCompatTrueTrigger(joiner, triggers, "trigger_jump", "jump", "trigger_jump", "triggerJump");
        appendCompatTrueTrigger(joiner, triggers, "trigger_landing", "landing", "trigger_landing", "triggerLanding");
        appendCompatTrueTrigger(joiner, triggers, "trigger_walking", "walking", "walkgin", "trigger_walking", "triggerWalking");
        appendCompatTrueTrigger(joiner, triggers, "trigger_sprinting", "sprinting", "trigger_sprinting", "triggerSprinting");
        appendCompatTrueTrigger(joiner, triggers, "trigger_swimming", "swimming", "swiming", "trigger_swimming", "triggerSwimming");
        appendCompatTrueTrigger(joiner, triggers, "trigger_attacking", "attacking", "attack", "trigger_attacking", "triggerAttacking");
        appendCompatTrueTrigger(joiner, triggers, "trigger_use_item", "use_item", "useitem", "useItem", "trigger_use_item", "triggerUseItem", "trigger_useitem", "triggerUseitem");
        appendCompatTrueTrigger(joiner, triggers, "trigger_mining", "mining", "trigger_mining", "triggerMining");
        JsonElement falling = triggers.get("falling");
        if (falling != null && !falling.isJsonNull()) {
            if (falling.isJsonPrimitive()) {
                if (readCompatBoolean(falling, false)) {
                    joiner.add("trigger_falling=true");
                }
            } else if (falling.isJsonObject()) {
                joiner.add("trigger_falling=true");
                Float minBlocks = readCompatFallingMinBlocks(falling.getAsJsonObject());
                if (minBlocks != null) {
                    joiner.add("trigger_falling_min_blocks=" + minBlocks);
                }
            }
        }
        List<String> biomes = readCompatTriggerBiomeNames(triggers.get("biome"));
        if (!biomes.isEmpty()) {
            joiner.add("trigger_biomes=" + String.join("|", biomes));
        }
    }

    private static void appendCompatTriggerList(StringJoiner joiner, JsonArray triggers) {
        if (joiner == null || triggers == null) {
            return;
        }
        joiner.add("trigger_mode=any");
        Iterator it = triggers.iterator();
        while (it.hasNext()) {
            JsonElement element = (JsonElement) it.next();
            if (element != null && !element.isJsonNull() && element.isJsonPrimitive()) {
                appendCompatNamedTrigger(joiner, element.getAsString());
            }
        }
    }

    private static void appendCompatNamedTrigger(StringJoiner joiner, String name) {
        if (joiner == null || name == null || name.isBlank()) {
            return;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "jump":
            case "trigger_jump":
            case "triggerjump":
                joiner.add("trigger_jump=true");
                break;
            case "landing":
            case "trigger_landing":
            case "triggerlanding":
                joiner.add("trigger_landing=true");
                break;
            case "walking":
            case "walkgin":
            case "trigger_walking":
            case "triggerwalking":
                joiner.add("trigger_walking=true");
                break;
            case "sprinting":
            case "trigger_sprinting":
            case "triggersprinting":
                joiner.add("trigger_sprinting=true");
                break;
            case "swimming":
            case "swiming":
            case "trigger_swimming":
            case "triggerswimming":
                joiner.add("trigger_swimming=true");
                break;
            case "attacking":
            case "attack":
            case "trigger_attacking":
            case "triggerattacking":
                joiner.add("trigger_attacking=true");
                break;
            case "use_item":
            case "useitem":
            case "trigger_use_item":
            case "triggeruseitem":
                joiner.add("trigger_use_item=true");
                break;
            case "mining":
            case "trigger_mining":
            case "triggermining":
                joiner.add("trigger_mining=true");
                break;
            case "falling":
            case "trigger_falling":
            case "triggerfalling":
                joiner.add("trigger_falling=true");
                break;
        }
    }

    private static void appendCompatTrueTrigger(StringJoiner joiner, JsonObject triggers, String tokenKey, String... keys) {
        if (joiner == null || triggers == null || tokenKey == null || keys == null) {
            return;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank() && readCompatBoolean(triggers.get(key), false)) {
                joiner.add(tokenKey + "=true");
                return;
            }
        }
    }

    private static boolean readCompatBoolean(JsonElement element, boolean fallback) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        if (element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt() != 0;
        }
        String value = element.getAsString();
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static Float readCompatFallingMinBlocks(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        String[] keys = {"min_blocks", "minBlocks", "min_height", "minHeight", "min"};
        for (String key : keys) {
            JsonElement element = obj.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                float value = element.getAsFloat();
                return Float.valueOf(Math.max(0.0f, value));
            }
        }
        return null;
    }

    private static List<String> readCompatTriggerBiomeNames(JsonElement biome) {
        String value;
        if (biome == null || biome.isJsonNull()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if (biome.isJsonPrimitive() && biome.getAsJsonPrimitive().isString()) {
            String value2 = biome.getAsString();
            if (value2 != null && !value2.isBlank()) {
                out.add(value2.trim());
            }
            return out;
        }
        if (biome.isJsonArray()) {
            for (JsonElement el : biome.getAsJsonArray()) {
                if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() && (value = el.getAsString()) != null && !value.isBlank()) {
                    out.add(value.trim());
                }
            }
            return out;
        }
        if (!biome.isJsonObject()) {
            return out;
        }
        JsonObject obj = biome.getAsJsonObject();
        JsonElement names = obj.has("names") ? obj.get("names") : obj.get("biomes");
        if (names != null) {
            out.addAll(readCompatTriggerBiomeNames(names));
            return out;
        }
        JsonElement one = obj.has("name") ? obj.get("name") : obj.get("biome");
        if (one != null) {
            out.addAll(readCompatTriggerBiomeNames(one));
        }
        return out;
    }

    private static File getFile() {
        Path configDir = getConfigDirectory();
        try {
            Files.createDirectories(configDir, new FileAttribute[0]);
        } catch (Exception e) {
        }
        return configDir.resolve(Punchy.MOD_ID).resolve(FILE_NAME).toFile();
    }

    public static void loadAndApply() {
        loadAndApply(class_310.method_1551() != null ? class_310.method_1551().method_1478() : null);
    }

    public static void loadAndApply(class_3300 resourceManager) {
        try {
            invalidateSelectedResourcePackCache();
            clearGroupsRegistry();
            resetAllTunings();
            DEFAULTS_RESOURCE_STATE = null;
            JsonObject activeProfileJson = null;
            Path activeProfile = TuningProfileManager.getActiveProfileFilePath();
            if (activeProfile != null && Files.isRegularFile(activeProfile, new LinkOption[0])) {
                FileReader reader = new FileReader(activeProfile.toFile());
                try {
                    JsonElement profileElement = (JsonElement) GSON.fromJson(reader, JsonElement.class);
                    if (profileElement != null && profileElement.isJsonObject()) {
                        activeProfileJson = profileElement.getAsJsonObject();
                    }
                    reader.close();
                } finally {
                }
            }
            Data baseData = buildBaseDefaults(resourceManager);
            DEFAULTS_RESOURCE_STATE = copyData(baseData);
            if (baseData != null) {
                if (baseData.customGroups != null) {
                    for (CustomGroupDefinition g : baseData.customGroups) {
                        if (g != null && g.name != null && g.items != null) {
                            REGISTRY_CUSTOM_GROUPS.put(g.name, g.items);
                        }
                    }
                }
                applyData(copyData(baseData));
            }
            Data activeProfileData = parseData(activeProfileJson);
            if (activeProfileData != null) {
                if (activeProfileData.customGroups != null) {
                    for (CustomGroupDefinition g2 : activeProfileData.customGroups) {
                        if (g2 != null && g2.name != null && g2.items != null) {
                            REGISTRY_CUSTOM_GROUPS.put(g2.name, g2.items);
                        }
                    }
                }
                applyData(copyData(activeProfileData));
                saveData(activeProfileData);
            }
            Set<class_2960> compatIds = discoverCompatResourceIds(resourceManager).keySet();
            MIX_COMPAT_OPTION_IDS = resolveMixCompatOptionIds(resourceManager, compatIds);
            MIX_PUNCHY_OPTION_IDS = discoverPunchyOptionIds(resourceManager, compatIds);
            MIX_CATALOG = buildMixCatalog(resourceManager, discoverCompatProfilesFromResourcePacks(resourceManager));
            applyMixSelections();
            reapplyActiveProfileSpecificTransforms(activeProfileData);
            rememberBaseline();
            INITIAL_STATE = captureCurrent();
        } catch (Exception e) {
        }
    }

    public static void reloadActiveProfileFast(class_3300 resourceManager) {
        try {
            invalidateSelectedResourcePackCache();
            if (DEFAULTS_RESOURCE_STATE == null) {
                loadAndApply(resourceManager);
                return;
            }
            clearGroupsRegistry();
            resetAllTunings();
            Data freshBaseData = buildBaseDefaults(resourceManager);
            if (freshBaseData != null) {
                DEFAULTS_RESOURCE_STATE = copyData(freshBaseData);
            }
            Data baseData = copyData(DEFAULTS_RESOURCE_STATE);
            if (baseData != null) {
                if (baseData.customGroups != null) {
                    for (CustomGroupDefinition g : baseData.customGroups) {
                        if (g != null && g.name != null && g.items != null) {
                            REGISTRY_CUSTOM_GROUPS.put(g.name, g.items);
                        }
                    }
                }
                applyData(copyData(baseData));
            }
            JsonObject activeProfileJson = null;
            Path activeProfile = TuningProfileManager.getActiveProfileFilePath();
            if (activeProfile != null && Files.isRegularFile(activeProfile, new LinkOption[0])) {
                FileReader reader = new FileReader(activeProfile.toFile());
                try {
                    JsonElement profileElement = (JsonElement) GSON.fromJson(reader, JsonElement.class);
                    if (profileElement != null && profileElement.isJsonObject()) {
                        activeProfileJson = profileElement.getAsJsonObject();
                    }
                    reader.close();
                } finally {
                }
            }
            Data activeProfileData = parseData(activeProfileJson);
            if (activeProfileData != null) {
                if (activeProfileData.customGroups != null) {
                    for (CustomGroupDefinition g2 : activeProfileData.customGroups) {
                        if (g2 != null && g2.name != null && g2.items != null) {
                            REGISTRY_CUSTOM_GROUPS.put(g2.name, g2.items);
                        }
                    }
                }
                applyData(copyData(activeProfileData));
                saveData(activeProfileData);
            }
            applyMixSelections();
            reapplyActiveProfileSpecificTransforms(activeProfileData);
            rememberBaseline();
            INITIAL_STATE = captureCurrent();
        } catch (Exception e) {
        }
    }

    private static JsonObject loadDefaultsJsonFromResource(class_3300 resourceManager) {
        if (resourceManager == null) {
            return null;
        }
        try {
            class_3298 resource = resourceManager.getResourceOrThrow(DEFAULTS_ID);
            InputStream stream = resource.method_14482();
            try {
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                try {
                    JsonElement element = (JsonElement) GSON.fromJson(reader, JsonElement.class);
                    if (element != null && element.isJsonObject()) {
                        JsonObject asJsonObject = element.getAsJsonObject();
                        reader.close();
                        if (stream != null) {
                            stream.close();
                        }
                        return asJsonObject;
                    }
                    reader.close();
                    if (stream != null) {
                        stream.close();
                    }
                    return null;
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Data buildBaseDefaults(class_3300 resourceManager) {
        Data resourceDefaults = parseData(loadDefaultsJsonFromResource(resourceManager));
        Data mergedCompat = buildMergedCompatOverlay(resourceManager);
        return mergeData(copyData(resourceDefaults), copyData(mergedCompat));
    }

    private static boolean isPunchyDefaultPackId(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }
        String normalized = packId.trim().toLowerCase(Locale.ROOT);
        return Punchy.MOD_ID.equals(normalized) || normalized.startsWith("punchy:") || normalized.endsWith(":punchy") || normalized.endsWith("/punchy") || normalized.endsWith("\\punchy") || "builtin/punchy".equals(normalized) || "builtin\\punchy".equals(normalized);
    }

    private static Data parseData(JsonObject json) {
        if (json == null) {
            return null;
        }
        return (Data) GSON.fromJson(json, Data.class);
    }

    private static JsonObject deepMergeJson(JsonObject base, JsonObject overlay) {
        if (base == null) {
            if (overlay == null) {
                return null;
            }
            return overlay.deepCopy();
        }
        JsonObject merged = base.deepCopy();
        if (overlay == null) {
            return merged;
        }
        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            JsonElement overlayValue = entry.getValue();
            if (overlayValue == null || overlayValue.isJsonNull()) {
                merged.add(entry.getKey(), JsonNull.INSTANCE);
            } else {
                JsonElement baseValue = merged.get(entry.getKey());
                if (baseValue != null && baseValue.isJsonObject() && overlayValue.isJsonObject()) {
                    merged.add(entry.getKey(), deepMergeJson(baseValue.getAsJsonObject(), overlayValue.getAsJsonObject()));
                } else {
                    merged.add(entry.getKey(), overlayValue.deepCopy());
                }
            }
        }
        return merged;
    }

    private static PhysicsEntry resolvePhysicsEntry(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String name = element.getAsString();
                if (name == null || name.isBlank()) {
                    return null;
                }
                PhysicsEntry entry = new PhysicsEntry();
                entry.name = name;
                return entry;
            }
            if (element.isJsonObject()) {
                PhysicsEntry entry2 = (PhysicsEntry) GSON.fromJson(element, PhysicsEntry.class);
                if (entry2 != null && entry2.name != null && entry2.name.isBlank()) {
                    entry2.name = null;
                }
                return entry2;
            }
            if (element.isJsonArray()) {
                for (JsonElement entryElement : element.getAsJsonArray()) {
                    PhysicsEntry entry3 = resolvePhysicsEntry(entryElement);
                    if (entry3 != null && entry3.name != null && !entry3.name.isBlank()) {
                        return entry3;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static ToolTuning.PhysicsMode resolvePhysicsMode(PhysicsEntry entry) {
        if (entry == null || entry.name == null || entry.name.isBlank()) {
            return null;
        }
        try {
            return ToolTuning.PhysicsMode.valueOf(entry.name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ToolTuning.PhysicsSettings resolvePhysicsSettings(PhysicsEntry entry) {
        if (entry == null) {
            return null;
        }
        ToolTuning.PhysicsSettings settings = new ToolTuning.PhysicsSettings();
        boolean hasValue = false;
        if (entry.limitDeg != null) {
            settings.limitDeg = entry.limitDeg;
            hasValue = true;
        }
        if (entry.limitDegUp != null) {
            settings.limitDegUp = entry.limitDegUp;
            hasValue = true;
        }
        if (entry.limitDegDown != null) {
            settings.limitDegDown = entry.limitDegDown;
            hasValue = true;
        }
        if (entry.force != null) {
            settings.force = entry.force;
            hasValue = true;
        }
        if (entry.speed != null) {
            settings.speed = entry.speed;
            hasValue = true;
        }
        if (entry.cameraSens != null) {
            settings.cameraSens = entry.cameraSens;
            hasValue = true;
        }
        if (entry.gravity != null) {
            settings.gravity = entry.gravity;
            hasValue = true;
        }
        if (entry.motionSens != null) {
            settings.motionSens = entry.motionSens;
            hasValue = true;
        }
        if (entry.invert != null) {
            settings.invert = entry.invert;
            hasValue = true;
        }
        if (entry.handSpace != null) {
            settings.handSpace = entry.handSpace;
            hasValue = true;
        }
        if (entry.enableCreaturePhysics != null) {
            settings.enableCreaturePhysics = entry.enableCreaturePhysics;
            hasValue = true;
        }
        if (hasValue) {
            return settings;
        }
        return null;
    }

    private static void applyData(Data data) {
        applyData(data, true);
    }

    private static void applyData(Data data, boolean resetRuntimeFlags) {
        class_2960 id;
        ToolTuning.Profile profile;
        ToolTuning.Profile profile2;
        ArmToolTuning.Profile profileDeserializeSpecificArmProfileFromSource;
        ArmToolTuning.Profile profileDeserializeSpecificArmProfileFromSource2;
        ToolTuning.Profile profile3;
        ToolTuning.Profile profile4;
        ToolTuning.Profile profile5;
        ToolTuning.Profile profile6;
        ToolTuning.Profile profile7;
        ToolTuning.Profile profile8;
        class_1792 item;
        SideSet meshSet;
        ToolTuning.Profile profile9;
        ToolTuning.Profile profile10;
        ToolTuning.Profile profile11;
        ToolTuning.Profile profile12;
        ToolTuning.Profile profile13;
        ToolTuning.Profile profile14;
        ToolTuning.Profile profile15;
        ToolTuning.Profile profile16;
        ToolTuning.Profile profile17;
        ToolTuning.Profile profile18;
        ToolTuning.Profile profile19;
        ToolTuning.Profile profile20;
        ToolTuning.Profile profile21;
        ToolTuning.Profile profile22;
        ToolTuning.Profile profile23;
        ToolTuning.Profile profile24;
        List<String> items;
        class_1792 item2;
        ArmToolTuning.Profile profileDeserializeSpecificArmProfileForRuntime;
        ArmToolTuning.Profile profileDeserializeSpecificArmProfileForRuntime2;
        ToolTuning.Profile profile25;
        ToolTuning.Profile profile26;
        if (data == null) {
            return;
        }
        if (data.item == null) {
            data.item = new HashMap();
        }
        if (resetRuntimeFlags) {
            GROUP_RUNTIME_FLAGS.clear();
            SPECIFIC_RUNTIME_FLAGS.clear();
        }
        for (Map.Entry<String, GroupEntry> entry : data.item.entrySet()) {
            String key = entry.getKey();
            GroupEntry groupConfig = entry.getValue();
            if (groupConfig != null && (items = REGISTRY_CUSTOM_GROUPS.get(key)) != null) {
                if (groupConfig.transforms == null && (groupConfig.right != null || groupConfig.left != null)) {
                    groupConfig.transforms = new SideSet();
                    groupConfig.transforms.right = groupConfig.right;
                    groupConfig.transforms.left = groupConfig.left;
                }
                applyItemLevelFishingRodFlags(groupConfig.customAnimation, groupConfig.disableWalkOnWater, groupConfig.disableDryOut, groupConfig.disableSwiming);
                for (String itemId : items) {
                    class_2960 id2 = class_2960.method_12829(itemId);
                    if (id2 != null && (item2 = (class_1792) class_7923.field_41178.method_17966(id2).orElse(class_1802.field_8162)) != class_1802.field_8162) {
                        class_1799 dummyStack = new class_1799(item2);
                        VanillaFirstPersonItemLayer.ToolKind resolvedKind = ToolKindResolver.resolveItemKind(dummyStack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
                        applySpecificRuntimeFlags(id2, groupConfig.disableWalkOnWater, groupConfig.disableDryOut, groupConfig.disableSwiming);
                        applyGroupRuntimeFlags(resolvedKind, groupConfig.disableWalkOnWater, groupConfig.disableDryOut, groupConfig.disableSwiming);
                        ToolTuning.Profile right = (groupConfig.transforms == null || groupConfig.transforms.right == null) ? null : groupConfig.transforms.right.toTool();
                        ToolTuning.Profile left = (groupConfig.transforms == null || groupConfig.transforms.left == null) ? null : groupConfig.transforms.left.toTool();
                        PhysicsEntry physicsEntry = resolvePhysicsEntry(groupConfig.physics);
                        ToolTuning.PhysicsMode mode = resolvePhysicsMode(physicsEntry);
                        ToolTuning.PhysicsSettings settings = resolvePhysicsSettings(physicsEntry);
                        if (mode != null) {
                            if (right != null) {
                                right.physicsMode = mode;
                                if (settings != null) {
                                    right.physicsSettings = settings.copy();
                                }
                            }
                            if (left != null) {
                                left.physicsMode = mode;
                                if (settings != null) {
                                    left.physicsSettings = settings.copy();
                                }
                            }
                        }
                        ToolTuning.applySpecific(id2, resolvedKind, null, groupConfig.customAnimation, groupConfig.customItemAnimations, readCompatParticleEffects(groupConfig), groupConfig.betterCombat, right, left);
                        if (mode != null) {
                            if (right == null && (profile26 = ToolTuning.getSpecificProfile(id2, resolvedKind, false)) != null) {
                                profile26.physicsMode = mode;
                                if (settings != null) {
                                    profile26.physicsSettings = settings.copy();
                                }
                            }
                            if (left == null && (profile25 = ToolTuning.getSpecificProfile(id2, resolvedKind, true)) != null) {
                                profile25.physicsMode = mode;
                                if (settings != null) {
                                    profile25.physicsSettings = settings.copy();
                                }
                            }
                        }
                        if (groupConfig.armTransforms != null) {
                            PlayerArmModelLayer.ToolKind armKind = ToolKindResolver.resolveArmKind(dummyStack, PlayerArmModelLayer.ToolKind.AXE);
                            if (groupConfig.armTransforms.right != null) {
                                profileDeserializeSpecificArmProfileForRuntime = deserializeSpecificArmProfileForRuntime(armKind, false, groupConfig.armTransforms.right.toArm());
                            } else {
                                profileDeserializeSpecificArmProfileForRuntime = null;
                            }
                            ArmToolTuning.Profile armRight = profileDeserializeSpecificArmProfileForRuntime;
                            if (groupConfig.armTransforms.left != null) {
                                profileDeserializeSpecificArmProfileForRuntime2 = deserializeSpecificArmProfileForRuntime(armKind, true, groupConfig.armTransforms.left.toArm());
                            } else {
                                profileDeserializeSpecificArmProfileForRuntime2 = null;
                            }
                            ArmToolTuning.Profile armLeft = profileDeserializeSpecificArmProfileForRuntime2;
                            ArmToolTuning.applySpecific(id2, armKind, armRight, armLeft);
                        }
                    }
                }
            }
        }
        SideSet globalItem = resolveGlobalItem(data);
        if (globalItem != null) {
            if (globalItem.right != null) {
                ToolTuning.applyGlobal(false, globalItem.right.toTool());
            }
            if (globalItem.left != null) {
                ToolTuning.applyGlobal(true, globalItem.left.toTool());
            }
        }
        if (data.default_animations != null && !data.default_animations.isEmpty()) {
            List<BetterCombatEntry> defaultBetterCombat = extractDefaultBetterCombatFromDefaultAnimations(data.default_animations);
            List<CustomAnimationEntry> filteredDefaults = stripBetterCombatFromDefaultAnimations(data.default_animations);
            ToolTuning.setDefaultAnimations(filteredDefaults);
            ToolTuning.setDefaultBetterCombat(defaultBetterCombat);
        }
        for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
            GroupEntry entry2 = data.item.get(kind.name());
            if (entry2 != null) {
                applyGroupRuntimeFlags(kind, entry2.disableWalkOnWater, entry2.disableDryOut, entry2.disableSwiming);
                if (entry2.transforms == null && (entry2.right != null || entry2.left != null)) {
                    entry2.transforms = new SideSet();
                    entry2.transforms.right = entry2.right;
                    entry2.transforms.left = entry2.left;
                }
                if (entry2.customAnimation != null) {
                    applyItemLevelFishingRodFlags(entry2.customAnimation, entry2.disableWalkOnWater, entry2.disableDryOut, entry2.disableSwiming);
                    ToolTuning.applyGroupAnimation(kind, entry2.customAnimation);
                }
                if (entry2.customItemAnimations != null) {
                    ToolTuning.applyGroupItemAnimation(kind, entry2.customItemAnimations);
                }
                List<String> groupParticles = readCompatParticleEffects(entry2);
                if (groupParticles != null) {
                    ToolTuning.applyGroupParticleEmitters(kind, groupParticles);
                }
                if (entry2.betterCombat != null) {
                    ToolTuning.applyGroupBetterCombat(kind, entry2.betterCombat);
                }
                SideSet set = entry2.transforms;
                PhysicsEntry physicsEntry2 = resolvePhysicsEntry(entry2.physics);
                ToolTuning.PhysicsMode groupPhysics = resolvePhysicsMode(physicsEntry2);
                ToolTuning.PhysicsSettings groupSettings = resolvePhysicsSettings(physicsEntry2);
                Boolean groupDisableSprintLateralTilt = entry2.disableSprintLateralTilt;
                Boolean groupKeepMainArmTuning = entry2.keepTriggerArmTuning;
                Boolean groupKeepArmTuningRight = resolveSideKeepArmTuning(entry2.keepArmTuning, entry2.keepArmTuningR);
                Boolean groupKeepArmTuningLeft = resolveSideKeepArmTuning(entry2.keepArmTuning, entry2.keepArmTuningL);
                if (groupPhysics != null) {
                    if (set != null) {
                        if (set.right != null && (set.right.physics == null || set.right.physics.isBlank())) {
                            set.right.physics = groupPhysics.name();
                        }
                        if (set.left != null && (set.left.physics == null || set.left.physics.isBlank())) {
                            set.left.physics = groupPhysics.name();
                        }
                    }
                    if ((set == null || set.right == null) && (profile23 = ToolTuning.get(kind, false)) != null) {
                        profile23.physicsMode = groupPhysics;
                        if (groupSettings != null) {
                            profile23.physicsSettings = groupSettings.copy();
                        }
                        ToolTuning.commitKind(kind, false);
                    }
                    if ((set == null || set.left == null) && (profile24 = ToolTuning.get(kind, true)) != null) {
                        profile24.physicsMode = groupPhysics;
                        if (groupSettings != null) {
                            profile24.physicsSettings = groupSettings.copy();
                        }
                        ToolTuning.commitKind(kind, true);
                    }
                }
                if (groupDisableSprintLateralTilt != null) {
                    if (set != null) {
                        if (set.right != null && set.right.disableSprintLateralTilt == null) {
                            set.right.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        }
                        if (set.left != null && set.left.disableSprintLateralTilt == null) {
                            set.left.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        }
                    }
                    if ((set == null || set.right == null) && (profile21 = ToolTuning.get(kind, false)) != null) {
                        profile21.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        ToolTuning.commitKind(kind, false);
                    }
                    if ((set == null || set.left == null) && (profile22 = ToolTuning.get(kind, true)) != null) {
                        profile22.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        ToolTuning.commitKind(kind, true);
                    }
                }
                if (groupKeepArmTuningRight != null || groupKeepArmTuningLeft != null) {
                    if (set != null) {
                        if (set.right != null && set.right.keepArmTuning == null && groupKeepArmTuningRight != null) {
                            set.right.keepArmTuning = groupKeepArmTuningRight;
                        }
                        if (set.left != null && set.left.keepArmTuning == null && groupKeepArmTuningLeft != null) {
                            set.left.keepArmTuning = groupKeepArmTuningLeft;
                        }
                    }
                    if ((set == null || set.right == null) && groupKeepArmTuningRight != null && (profile17 = ToolTuning.get(kind, false)) != null) {
                        profile17.keepArmTuning = groupKeepArmTuningRight;
                        ToolTuning.commitKind(kind, false);
                    }
                    if ((set == null || set.left == null) && groupKeepArmTuningLeft != null && (profile18 = ToolTuning.get(kind, true)) != null) {
                        profile18.keepArmTuning = groupKeepArmTuningLeft;
                        ToolTuning.commitKind(kind, true);
                    }
                }
                if (groupKeepMainArmTuning != null) {
                    if (set != null) {
                        if (set.right != null && set.right.keepTriggerArmTuning == null) {
                            set.right.keepTriggerArmTuning = groupKeepMainArmTuning;
                        }
                        if (set.left != null && set.left.keepTriggerArmTuning == null) {
                            set.left.keepTriggerArmTuning = groupKeepMainArmTuning;
                        }
                    }
                    if ((set == null || set.right == null) && (profile19 = ToolTuning.get(kind, false)) != null) {
                        profile19.keepTriggerArmTuning = groupKeepMainArmTuning;
                        ToolTuning.commitKind(kind, false);
                    }
                    if ((set == null || set.left == null) && (profile20 = ToolTuning.get(kind, true)) != null) {
                        profile20.keepTriggerArmTuning = groupKeepMainArmTuning;
                        ToolTuning.commitKind(kind, true);
                    }
                }
                if (set != null) {
                    if (set.right != null) {
                        ToolTuning.Profile profile27 = set.right.toTool();
                        if (groupDisableSprintLateralTilt != null && set.right.disableSprintLateralTilt == null) {
                            profile27.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        }
                        if (groupKeepArmTuningRight != null && set.right.keepArmTuning == null) {
                            profile27.keepArmTuning = groupKeepArmTuningRight;
                        }
                        if (groupKeepMainArmTuning != null && set.right.keepTriggerArmTuning == null) {
                            profile27.keepTriggerArmTuning = groupKeepMainArmTuning;
                        }
                        if (groupPhysics != null && (set.right.physics == null || set.right.physics.isBlank())) {
                            profile27.physicsMode = groupPhysics;
                        }
                        if (groupSettings != null && groupPhysics != null && profile27.physicsMode == groupPhysics) {
                            profile27.physicsSettings = groupSettings.copy();
                        }
                        ToolTuning.apply(kind, false, profile27);
                        ToolTuning.markKindDetached(kind, false);
                    }
                    if (set.left != null) {
                        ToolTuning.Profile profile28 = set.left.toTool();
                        if (groupDisableSprintLateralTilt != null && set.left.disableSprintLateralTilt == null) {
                            profile28.disableSprintLateralTilt = groupDisableSprintLateralTilt;
                        }
                        if (groupKeepArmTuningLeft != null && set.left.keepArmTuning == null) {
                            profile28.keepArmTuning = groupKeepArmTuningLeft;
                        }
                        if (groupKeepMainArmTuning != null && set.left.keepTriggerArmTuning == null) {
                            profile28.keepTriggerArmTuning = groupKeepMainArmTuning;
                        }
                        if (groupPhysics != null && (set.left.physics == null || set.left.physics.isBlank())) {
                            profile28.physicsMode = groupPhysics;
                        }
                        if (groupSettings != null && groupPhysics != null && profile28.physicsMode == groupPhysics) {
                            profile28.physicsSettings = groupSettings.copy();
                        }
                        ToolTuning.apply(kind, true, profile28);
                        ToolTuning.markKindDetached(kind, true);
                    }
                }
                SideSet armSet = entry2.armTransforms;
                if (armSet != null) {
                    PlayerArmModelLayer.ToolKind armKind2 = null;
                    try {
                        armKind2 = PlayerArmModelLayer.ToolKind.valueOf(kind.name());
                    } catch (IllegalArgumentException e) {
                    }
                    if (armKind2 != null) {
                        if (armSet.right != null) {
                            ArmToolTuning.apply(armKind2, false, deserializeKindArmProfileForRuntime(false, armSet.right.toArm()));
                            ArmToolTuning.markKindDetached(armKind2, false);
                        }
                        if (armSet.left != null) {
                            ArmToolTuning.apply(armKind2, true, deserializeKindArmProfileForRuntime(true, armSet.left.toArm()));
                            ArmToolTuning.markKindDetached(armKind2, true);
                        }
                    }
                }
            }
        }
        if (data.emptyHand != null) {
            if (data.emptyHand.customAnimation != null) {
                ToolTuning.applyGroupAnimation(VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND, data.emptyHand.customAnimation);
            }
            if (data.emptyHand.customItemAnimations != null) {
                ToolTuning.applyGroupItemAnimation(VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND, data.emptyHand.customItemAnimations);
            }
            if (data.emptyHand.betterCombat != null) {
                ToolTuning.applyGroupBetterCombat(VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND, data.emptyHand.betterCombat);
            }
            if (data.emptyHand.transforms != null || data.emptyHand.physics != null) {
                VanillaFirstPersonItemLayer.ToolKind kind2 = VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND;
                SideSet set2 = data.emptyHand.transforms;
                PhysicsEntry physicsEntry3 = resolvePhysicsEntry(data.emptyHand.physics);
                ToolTuning.PhysicsMode groupPhysics2 = resolvePhysicsMode(physicsEntry3);
                ToolTuning.PhysicsSettings groupSettings2 = resolvePhysicsSettings(physicsEntry3);
                Boolean groupDisableSprintLateralTilt2 = data.emptyHand.disableSprintLateralTilt;
                Boolean groupKeepMainArmTuning2 = data.emptyHand.keepTriggerArmTuning;
                Boolean groupKeepArmTuningRight2 = resolveSideKeepArmTuning(data.emptyHand.keepArmTuning, data.emptyHand.keepArmTuningR);
                Boolean groupKeepArmTuningLeft2 = resolveSideKeepArmTuning(data.emptyHand.keepArmTuning, data.emptyHand.keepArmTuningL);
                if (groupPhysics2 != null) {
                    if (set2 != null) {
                        if (set2.right != null && (set2.right.physics == null || set2.right.physics.isBlank())) {
                            set2.right.physics = groupPhysics2.name();
                        }
                        if (set2.left != null && (set2.left.physics == null || set2.left.physics.isBlank())) {
                            set2.left.physics = groupPhysics2.name();
                        }
                    }
                    if ((set2 == null || set2.right == null) && (profile15 = ToolTuning.get(kind2, false)) != null) {
                        profile15.physicsMode = groupPhysics2;
                        if (groupSettings2 != null) {
                            profile15.physicsSettings = groupSettings2.copy();
                        }
                        ToolTuning.commitKind(kind2, false);
                    }
                    if ((set2 == null || set2.left == null) && (profile16 = ToolTuning.get(kind2, true)) != null) {
                        profile16.physicsMode = groupPhysics2;
                        if (groupSettings2 != null) {
                            profile16.physicsSettings = groupSettings2.copy();
                        }
                        ToolTuning.commitKind(kind2, true);
                    }
                }
                if (groupDisableSprintLateralTilt2 != null) {
                    if (set2 != null) {
                        if (set2.right != null && set2.right.disableSprintLateralTilt == null) {
                            set2.right.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        }
                        if (set2.left != null && set2.left.disableSprintLateralTilt == null) {
                            set2.left.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        }
                    }
                    if ((set2 == null || set2.right == null) && (profile13 = ToolTuning.get(kind2, false)) != null) {
                        profile13.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        ToolTuning.commitKind(kind2, false);
                    }
                    if ((set2 == null || set2.left == null) && (profile14 = ToolTuning.get(kind2, true)) != null) {
                        profile14.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        ToolTuning.commitKind(kind2, true);
                    }
                }
                if (groupKeepArmTuningRight2 != null || groupKeepArmTuningLeft2 != null) {
                    if (set2 != null) {
                        if (set2.right != null && set2.right.keepArmTuning == null && groupKeepArmTuningRight2 != null) {
                            set2.right.keepArmTuning = groupKeepArmTuningRight2;
                        }
                        if (set2.left != null && set2.left.keepArmTuning == null && groupKeepArmTuningLeft2 != null) {
                            set2.left.keepArmTuning = groupKeepArmTuningLeft2;
                        }
                    }
                    if ((set2 == null || set2.right == null) && groupKeepArmTuningRight2 != null && (profile9 = ToolTuning.get(kind2, false)) != null) {
                        profile9.keepArmTuning = groupKeepArmTuningRight2;
                        ToolTuning.commitKind(kind2, false);
                    }
                    if ((set2 == null || set2.left == null) && groupKeepArmTuningLeft2 != null && (profile10 = ToolTuning.get(kind2, true)) != null) {
                        profile10.keepArmTuning = groupKeepArmTuningLeft2;
                        ToolTuning.commitKind(kind2, true);
                    }
                }
                if (groupKeepMainArmTuning2 != null) {
                    if (set2 != null) {
                        if (set2.right != null && set2.right.keepTriggerArmTuning == null) {
                            set2.right.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        }
                        if (set2.left != null && set2.left.keepTriggerArmTuning == null) {
                            set2.left.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        }
                    }
                    if ((set2 == null || set2.right == null) && (profile11 = ToolTuning.get(kind2, false)) != null) {
                        profile11.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        ToolTuning.commitKind(kind2, false);
                    }
                    if ((set2 == null || set2.left == null) && (profile12 = ToolTuning.get(kind2, true)) != null) {
                        profile12.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        ToolTuning.commitKind(kind2, true);
                    }
                }
                if (set2 != null) {
                    if (set2.right != null) {
                        ToolTuning.Profile profile29 = set2.right.toTool();
                        if (groupDisableSprintLateralTilt2 != null && set2.right.disableSprintLateralTilt == null) {
                            profile29.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        }
                        if (groupKeepArmTuningRight2 != null && set2.right.keepArmTuning == null) {
                            profile29.keepArmTuning = groupKeepArmTuningRight2;
                        }
                        if (groupKeepMainArmTuning2 != null && set2.right.keepTriggerArmTuning == null) {
                            profile29.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        }
                        if (groupPhysics2 != null && (set2.right.physics == null || set2.right.physics.isBlank())) {
                            profile29.physicsMode = groupPhysics2;
                        }
                        if (groupSettings2 != null && groupPhysics2 != null && profile29.physicsMode == groupPhysics2) {
                            profile29.physicsSettings = groupSettings2.copy();
                        }
                        ToolTuning.apply(kind2, false, profile29);
                        ToolTuning.markKindDetached(kind2, false);
                    }
                    if (set2.left != null) {
                        ToolTuning.Profile profile30 = set2.left.toTool();
                        if (groupDisableSprintLateralTilt2 != null && set2.left.disableSprintLateralTilt == null) {
                            profile30.disableSprintLateralTilt = groupDisableSprintLateralTilt2;
                        }
                        if (groupKeepArmTuningLeft2 != null && set2.left.keepArmTuning == null) {
                            profile30.keepArmTuning = groupKeepArmTuningLeft2;
                        }
                        if (groupKeepMainArmTuning2 != null && set2.left.keepTriggerArmTuning == null) {
                            profile30.keepTriggerArmTuning = groupKeepMainArmTuning2;
                        }
                        if (groupPhysics2 != null && (set2.left.physics == null || set2.left.physics.isBlank())) {
                            profile30.physicsMode = groupPhysics2;
                        }
                        if (groupSettings2 != null && groupPhysics2 != null && profile30.physicsMode == groupPhysics2) {
                            profile30.physicsSettings = groupSettings2.copy();
                        }
                        ToolTuning.apply(kind2, true, profile30);
                        ToolTuning.markKindDetached(kind2, true);
                    }
                }
            }
            if (data.emptyHand.armTransforms != null) {
                SideSet armSet2 = data.emptyHand.armTransforms;
                PlayerArmModelLayer.ToolKind armKind3 = PlayerArmModelLayer.ToolKind.EMPTY_HAND;
                if (armSet2.right != null) {
                    ArmToolTuning.apply(armKind3, false, deserializeKindArmProfileForRuntime(false, armSet2.right.toArm()));
                    ArmToolTuning.markKindDetached(armKind3, false);
                }
                if (armSet2.left != null) {
                    ArmToolTuning.apply(armKind3, true, deserializeKindArmProfileForRuntime(true, armSet2.left.toArm()));
                    ArmToolTuning.markKindDetached(armKind3, true);
                }
            }
        }
        SideSet globalArm = resolveGlobalArm(data);
        if (globalArm != null) {
            if (globalArm.right != null) {
                ArmToolTuning.applyGlobal(false, globalArm.right.toArm());
            }
            if (globalArm.left != null) {
                ArmToolTuning.applyGlobal(true, globalArm.left.toArm());
            }
            ArmMeshTuning.resetGlobal(false);
            ArmMeshTuning.resetGlobal(true);
        }
        if (data.armMesh != null && (meshSet = data.armMesh.get(ARM_MESH_SIDE_KEY)) != null) {
            if (meshSet.right != null) {
                ArmMeshTuning.applyGlobal(false, meshSet.right.toArmMesh());
            }
            if (meshSet.left != null) {
                ArmMeshTuning.applyGlobal(true, meshSet.left.toArmMesh());
            }
        }
        if (data.chestProxy != null) {
            applyChestProxy(data.chestProxy);
        }
        if (data.fluidProxy != null) {
            applyFluidProxy(data.fluidProxy);
        }
        if (data.enchantParticleProxy != null) {
            applyEnchantParticleProxy(data.enchantParticleProxy);
        }
        if (data.paddleProxy != null) {
            applyPaddleProxy(data.paddleProxy);
        }
        if (data.fishingLineProxy != null) {
            applyFishingLineProxy(data.fishingLineProxy);
        }
        if (data.itemSpecific != null) {
            for (Map.Entry<String, SpecificEntry> entry3 : data.itemSpecific.entrySet()) {
                if (entry3 != null && entry3.getValue() != null && (id = resolveSpecificItemId(entry3.getKey())) != null) {
                    VanillaFirstPersonItemLayer.ToolKind kind3 = null;
                    if (entry3.getValue().kind != null) {
                        try {
                            kind3 = VanillaFirstPersonItemLayer.ToolKind.valueOf(entry3.getValue().kind);
                        } catch (IllegalArgumentException e2) {
                        }
                    }
                    if (kind3 == null && (item = (class_1792) class_7923.field_41178.method_17966(id).orElse(class_1802.field_8162)) != class_1802.field_8162) {
                        kind3 = ToolKindResolver.resolveItemKind(new class_1799(item), VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
                    }
                    SideSet transforms = entry3.getValue().transforms;
                    ToolTuning.Profile right2 = (transforms == null || transforms.right == null) ? null : transforms.right.toTool();
                    ToolTuning.Profile left2 = (transforms == null || transforms.left == null) ? null : transforms.left.toTool();
                    Boolean specificDisableSprintLateralTilt = entry3.getValue().disableSprintLateralTilt;
                    Boolean specificKeepMainArmTuning = entry3.getValue().keepTriggerArmTuning;
                    Boolean specificKeepArmTuningRight = resolveSideKeepArmTuning(entry3.getValue().keepArmTuning, entry3.getValue().keepArmTuningR);
                    Boolean specificKeepArmTuningLeft = resolveSideKeepArmTuning(entry3.getValue().keepArmTuning, entry3.getValue().keepArmTuningL);
                    if (specificDisableSprintLateralTilt != null) {
                        if (transforms != null) {
                            if (transforms.right != null && transforms.right.disableSprintLateralTilt == null && right2 != null) {
                                right2.disableSprintLateralTilt = specificDisableSprintLateralTilt;
                            }
                            if (transforms.left != null && transforms.left.disableSprintLateralTilt == null && left2 != null) {
                                left2.disableSprintLateralTilt = specificDisableSprintLateralTilt;
                            }
                        }
                        if ((transforms == null || transforms.right == null) && kind3 != null && (profile7 = ToolTuning.getSpecificProfile(id, kind3, false)) != null) {
                            profile7.disableSprintLateralTilt = specificDisableSprintLateralTilt;
                        }
                        if ((transforms == null || transforms.left == null) && kind3 != null && (profile8 = ToolTuning.getSpecificProfile(id, kind3, true)) != null) {
                            profile8.disableSprintLateralTilt = specificDisableSprintLateralTilt;
                        }
                    }
                    if (specificKeepArmTuningRight != null || specificKeepArmTuningLeft != null) {
                        if (transforms != null) {
                            if (transforms.right != null && transforms.right.keepArmTuning == null && right2 != null && specificKeepArmTuningRight != null) {
                                right2.keepArmTuning = specificKeepArmTuningRight;
                            }
                            if (transforms.left != null && transforms.left.keepArmTuning == null && left2 != null && specificKeepArmTuningLeft != null) {
                                left2.keepArmTuning = specificKeepArmTuningLeft;
                            }
                        }
                        if ((transforms == null || transforms.right == null) && specificKeepArmTuningRight != null && kind3 != null && (profile = ToolTuning.getSpecificProfile(id, kind3, false)) != null) {
                            profile.keepArmTuning = specificKeepArmTuningRight;
                        }
                        if ((transforms == null || transforms.left == null) && specificKeepArmTuningLeft != null && kind3 != null && (profile2 = ToolTuning.getSpecificProfile(id, kind3, true)) != null) {
                            profile2.keepArmTuning = specificKeepArmTuningLeft;
                        }
                    }
                    if (specificKeepMainArmTuning != null) {
                        if (transforms != null) {
                            if (transforms.right != null && transforms.right.keepTriggerArmTuning == null && right2 != null) {
                                right2.keepTriggerArmTuning = specificKeepMainArmTuning;
                            }
                            if (transforms.left != null && transforms.left.keepTriggerArmTuning == null && left2 != null) {
                                left2.keepTriggerArmTuning = specificKeepMainArmTuning;
                            }
                        }
                        if ((transforms == null || transforms.right == null) && kind3 != null && (profile5 = ToolTuning.getSpecificProfile(id, kind3, false)) != null) {
                            profile5.keepTriggerArmTuning = specificKeepMainArmTuning;
                        }
                        if ((transforms == null || transforms.left == null) && kind3 != null && (profile6 = ToolTuning.getSpecificProfile(id, kind3, true)) != null) {
                            profile6.keepTriggerArmTuning = specificKeepMainArmTuning;
                        }
                    }
                    PhysicsEntry physicsEntry4 = resolvePhysicsEntry(entry3.getValue().physics);
                    ToolTuning.PhysicsMode specificPhysics = resolvePhysicsMode(physicsEntry4);
                    ToolTuning.PhysicsSettings specificSettings = resolvePhysicsSettings(physicsEntry4);
                    if (specificPhysics != null) {
                        if (right2 != null && (transforms.right == null || transforms.right.physics == null)) {
                            right2.physicsMode = specificPhysics;
                        }
                        if (left2 != null && (transforms.left == null || transforms.left.physics == null)) {
                            left2.physicsMode = specificPhysics;
                        }
                        if (kind3 != null) {
                            if ((transforms == null || transforms.right == null) && (profile3 = ToolTuning.getSpecificProfile(id, kind3, false)) != null) {
                                profile3.physicsMode = specificPhysics;
                                if (specificSettings != null) {
                                    profile3.physicsSettings = specificSettings.copy();
                                }
                            }
                            if ((transforms == null || transforms.left == null) && (profile4 = ToolTuning.getSpecificProfile(id, kind3, true)) != null) {
                                profile4.physicsMode = specificPhysics;
                                if (specificSettings != null) {
                                    profile4.physicsSettings = specificSettings.copy();
                                }
                            }
                        }
                    }
                    if (specificSettings != null && specificPhysics != null) {
                        if (right2 != null && right2.physicsMode == specificPhysics) {
                            right2.physicsSettings = specificSettings.copy();
                        }
                        if (left2 != null && left2.physicsMode == specificPhysics) {
                            left2.physicsSettings = specificSettings.copy();
                        }
                    }
                    if (kind3 != null) {
                        applySpecificRuntimeFlags(id, entry3.getValue().disableWalkOnWater, entry3.getValue().disableDryOut, entry3.getValue().disableSwiming);
                        applyGroupRuntimeFlags(kind3, entry3.getValue().disableWalkOnWater, entry3.getValue().disableDryOut, entry3.getValue().disableSwiming);
                        applyItemLevelFishingRodFlags(entry3.getValue().customAnimation, entry3.getValue().disableWalkOnWater, entry3.getValue().disableDryOut, entry3.getValue().disableSwiming);
                        ToolTuning.applySpecific(id, entry3.getValue().customModelData, kind3, entry3.getValue().renamed, entry3.getValue().model, entry3.getValue().customAnimation, entry3.getValue().customItemAnimations, readCompatParticleEffects(entry3.getValue()), entry3.getValue().betterCombat, entry3.getValue().bonePhysics, right2, left2);
                    }
                    SideSet armTransforms = entry3.getValue().armTransforms;
                    if (armTransforms != null) {
                        PlayerArmModelLayer.ToolKind armKind4 = null;
                        if (entry3.getValue().armKind != null) {
                            try {
                                armKind4 = PlayerArmModelLayer.ToolKind.valueOf(entry3.getValue().armKind);
                            } catch (IllegalArgumentException e3) {
                            }
                        }
                        if (armKind4 == null && entry3.getValue().kind != null) {
                            try {
                                armKind4 = PlayerArmModelLayer.ToolKind.valueOf(entry3.getValue().kind);
                            } catch (IllegalArgumentException e4) {
                                armKind4 = PlayerArmModelLayer.ToolKind.AXE;
                            }
                        }
                        if (armKind4 == null) {
                            armKind4 = PlayerArmModelLayer.ToolKind.AXE;
                        }
                        if (armTransforms.right != null) {
                            profileDeserializeSpecificArmProfileFromSource = deserializeSpecificArmProfileFromSource(armKind4, false, armTransforms.right.toArm());
                        } else {
                            profileDeserializeSpecificArmProfileFromSource = null;
                        }
                        ArmToolTuning.Profile armRight2 = profileDeserializeSpecificArmProfileFromSource;
                        if (armTransforms.left != null) {
                            profileDeserializeSpecificArmProfileFromSource2 = deserializeSpecificArmProfileFromSource(armKind4, true, armTransforms.left.toArm());
                        } else {
                            profileDeserializeSpecificArmProfileFromSource2 = null;
                        }
                        ArmToolTuning.Profile armLeft2 = profileDeserializeSpecificArmProfileFromSource2;
                        ArmToolTuning.applySpecific(id, armKind4, armRight2, armLeft2);
                    }
                }
            }
        }
        ToolTuning.refreshAll();
    }

    private static void applyChestProxy(Map<String, Pose> proxyData) {
        ChestProxyTuning.Kind kind;
        if (proxyData == null) {
            return;
        }
        for (Map.Entry<String, Pose> entry : proxyData.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && (kind = parseChestProxyKind(entry.getKey())) != null) {
                ChestProxyTuning.Profile profile = entry.getValue().toChestProxy();
                ChestProxyTuning.apply(kind, profile);
            }
        }
    }

    private static void applyFluidProxy(Map<String, Pose> proxyData) {
        FluidProxyTuning.Kind kind;
        if (proxyData == null) {
            return;
        }
        for (Map.Entry<String, Pose> entry : proxyData.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && (kind = parseFluidProxyKind(entry.getKey())) != null) {
                FluidProxyTuning.Profile profile = entry.getValue().toFluidProxy();
                FluidProxyTuning.apply(kind, profile);
            }
        }
    }

    private static void applyEnchantParticleProxy(Map<String, Pose> proxyData) {
        if (proxyData == null) {
            return;
        }
        for (Map.Entry<String, Pose> entry : proxyData.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                EnchantParticleProxySide side = parseEnchantParticleProxySide(entry.getKey());
                EnchantParticleProxyTuning.Kind kind = parseEnchantParticleProxyKind(entry.getKey());
                if (kind != null) {
                    EnchantParticleProxyTuning.Profile profile = entry.getValue().toEnchantParticleProxy();
                    if (side == EnchantParticleProxySide.LEFT) {
                        EnchantParticleProxyTuning.apply(kind, true, profile);
                    } else if (side == EnchantParticleProxySide.RIGHT) {
                        EnchantParticleProxyTuning.apply(kind, false, profile);
                    } else {
                        EnchantParticleProxyTuning.apply(kind, false, profile);
                        EnchantParticleProxyTuning.apply(kind, true, profile);
                    }
                }
            }
        }
    }

    private static void applyPaddleProxy(Map<String, Pose> proxyData) {
        if (proxyData == null) {
            return;
        }
        for (Map.Entry<String, Pose> entry : proxyData.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                PaddleProxySide side = parsePaddleProxySide(entry.getKey());
                PaddleProxyTuning.Kind kind = parsePaddleProxyKind(entry.getKey());
                if (kind != null) {
                    PaddleProxyTuning.Profile profile = entry.getValue().toPaddleProxy();
                    if (side == PaddleProxySide.LEFT) {
                        PaddleProxyTuning.apply(kind, true, profile);
                    } else if (side == PaddleProxySide.RIGHT) {
                        PaddleProxyTuning.apply(kind, false, profile);
                    } else {
                        PaddleProxyTuning.apply(kind, false, profile);
                        PaddleProxyTuning.apply(kind, true, profile);
                    }
                }
            }
        }
    }

    private static void applyFishingLineProxy(Map<String, Pose> proxyData) {
        if (proxyData == null) {
            return;
        }
        for (Map.Entry<String, Pose> entry : proxyData.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                FishingLineProxySide side = parseFishingLineProxySide(entry.getKey());
                FishingLineProxyTuning.Kind kind = parseFishingLineProxyKind(entry.getKey());
                if (kind != null) {
                    FishingLineProxyTuning.Profile profile = entry.getValue().toFishingLineProxy();
                    if (side == FishingLineProxySide.LEFT) {
                        FishingLineProxyTuning.apply(kind, true, profile);
                    } else if (side == FishingLineProxySide.RIGHT) {
                        FishingLineProxyTuning.apply(kind, false, profile);
                    } else {
                        FishingLineProxyTuning.apply(kind, false, profile);
                        FishingLineProxyTuning.apply(kind, true, profile);
                    }
                }
            }
        }
    }

    private static Map<String, Pose> captureChestProxy() {
        Map<String, Pose> out = new HashMap<>();
        for (ChestProxyTuning.Kind kind : ChestProxyTuning.Kind.values()) {
            ChestProxyTuning.Profile profile = ChestProxyTuning.get(kind);
            if (profile != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT), Pose.fromChestProxy(profile));
            }
        }
        return out;
    }

    private static Map<String, Pose> captureFluidProxy() {
        Map<String, Pose> out = new HashMap<>();
        for (FluidProxyTuning.Kind kind : FluidProxyTuning.Kind.values()) {
            FluidProxyTuning.Profile profile = FluidProxyTuning.get(kind);
            if (profile != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT), Pose.fromFluidProxy(profile));
            }
        }
        return out;
    }

    private static Map<String, Pose> captureEnchantParticleProxy() {
        Map<String, Pose> out = new HashMap<>();
        for (EnchantParticleProxyTuning.Kind kind : EnchantParticleProxyTuning.Kind.values()) {
            EnchantParticleProxyTuning.Profile right = EnchantParticleProxyTuning.get(kind, false);
            EnchantParticleProxyTuning.Profile left = EnchantParticleProxyTuning.get(kind, true);
            if (right != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_right", Pose.fromEnchantParticleProxy(right));
            }
            if (left != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_left", Pose.fromEnchantParticleProxy(left));
            }
        }
        return out;
    }

    private static Map<String, Pose> capturePaddleProxy() {
        Map<String, Pose> out = new HashMap<>();
        for (PaddleProxyTuning.Kind kind : PaddleProxyTuning.Kind.values()) {
            PaddleProxyTuning.Profile right = PaddleProxyTuning.get(kind, false);
            PaddleProxyTuning.Profile left = PaddleProxyTuning.get(kind, true);
            if (right != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_right", Pose.fromPaddleProxy(right));
            }
            if (left != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_left", Pose.fromPaddleProxy(left));
            }
        }
        return out;
    }

    private static Map<String, Pose> captureFishingLineProxy() {
        Map<String, Pose> out = new HashMap<>();
        for (FishingLineProxyTuning.Kind kind : FishingLineProxyTuning.Kind.values()) {
            FishingLineProxyTuning.Profile right = FishingLineProxyTuning.get(kind, false);
            FishingLineProxyTuning.Profile left = FishingLineProxyTuning.get(kind, true);
            if (right != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_right", Pose.fromFishingLineProxy(right));
            }
            if (left != null) {
                out.put(kind.name().toLowerCase(Locale.ROOT) + "_left", Pose.fromFishingLineProxy(left));
            }
        }
        return out;
    }

    private static ChestProxyTuning.Kind parseChestProxyKind(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        switch (key) {
            case "boat":
                return ChestProxyTuning.Kind.BOAT;
            case "raft":
                return ChestProxyTuning.Kind.RAFT;
            case "minecart":
                return ChestProxyTuning.Kind.MINECART;
            default:
                return null;
        }
    }

    private static FluidProxyTuning.Kind parseFluidProxyKind(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        switch (key) {
            case "water":
                return FluidProxyTuning.Kind.WATER;
            case "lava":
                return FluidProxyTuning.Kind.LAVA;
            case "milk":
                return FluidProxyTuning.Kind.MILK;
            default:
                return null;
        }
    }

    private static EnchantParticleProxyTuning.Kind parseEnchantParticleProxyKind(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_right")) {
            key = key.substring(0, key.length() - "_right".length());
        } else if (key.endsWith("_left")) {
            key = key.substring(0, key.length() - "_left".length());
        }
        switch (key) {
            case "book":
                return EnchantParticleProxyTuning.Kind.BOOK;
            default:
                return null;
        }
    }

    private static PaddleProxyTuning.Kind parsePaddleProxyKind(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_right")) {
            key = key.substring(0, key.length() - "_right".length());
        } else if (key.endsWith("_left")) {
            key = key.substring(0, key.length() - "_left".length());
        }
        switch (key) {
            case "boat":
                return PaddleProxyTuning.Kind.BOAT;
            case "raft":
                return PaddleProxyTuning.Kind.RAFT;
            default:
                return null;
        }
    }

    private static FishingLineProxyTuning.Kind parseFishingLineProxyKind(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_right")) {
            key = key.substring(0, key.length() - "_right".length());
        } else if (key.endsWith("_left")) {
            key = key.substring(0, key.length() - "_left".length());
        }
        switch (key) {
            case "rod":
            case "fishing_rod":
                return FishingLineProxyTuning.Kind.ROD;
            default:
                return null;
        }
    }

    private static PaddleProxySide parsePaddleProxySide(String raw) {
        if (raw == null) {
            return PaddleProxySide.BOTH;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_left")) {
            return PaddleProxySide.LEFT;
        }
        if (key.endsWith("_right")) {
            return PaddleProxySide.RIGHT;
        }
        return PaddleProxySide.BOTH;
    }

    private static FishingLineProxySide parseFishingLineProxySide(String raw) {
        if (raw == null) {
            return FishingLineProxySide.BOTH;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_left")) {
            return FishingLineProxySide.LEFT;
        }
        if (key.endsWith("_right")) {
            return FishingLineProxySide.RIGHT;
        }
        return FishingLineProxySide.BOTH;
    }

    private static EnchantParticleProxySide parseEnchantParticleProxySide(String raw) {
        if (raw == null) {
            return EnchantParticleProxySide.BOTH;
        }
        String key = raw.toLowerCase(Locale.ROOT);
        if (key.endsWith("_left")) {
            return EnchantParticleProxySide.LEFT;
        }
        if (key.endsWith("_right")) {
            return EnchantParticleProxySide.RIGHT;
        }
        return EnchantParticleProxySide.BOTH;
    }

    public static void saveCurrent() {
        try {
            Data data = captureCurrent();
            if (INITIAL_STATE != null) {
                Data sessionDelta = computeDelta(INITIAL_STATE, data);
                sessionDelta.globalItem = null;
                sessionDelta.globalArm = null;
                sessionDelta.armMesh = null;
                saveHelperData(sessionDelta);
            }
            String compatTarget = Punchy.ACTIVE_COMPAT_EDIT_TARGET;
            if (compatTarget != null && !compatTarget.isBlank()) {
                Data toSave = BASELINE_STATE != null ? computeDelta(BASELINE_STATE, data) : data;
                saveCompatData(compatTarget, toSave);
            } else {
                Path activeProfile = TuningProfileManager.getActiveProfileFilePath();
                if (activeProfile != null) {
                    saveCurrentProfileDeltaToFile(activeProfile);
                } else {
                    Data toSave2 = INITIAL_STATE != null ? computeDelta(INITIAL_STATE, data) : data;
                    saveData(toSave2);
                }
                BASELINE_STATE = data;
            }
        } catch (Exception e) {
        }
    }

    public static boolean saveCurrentDeltaToFile(Path targetFile) {
        if (targetFile == null) {
            return false;
        }
        try {
            Data current = captureCurrent();
            Data toSave = BASELINE_STATE != null ? computeDelta(BASELINE_STATE, current) : current;
            if (toSave == null) {
                toSave = new Data();
            }
            pruneEmptyCollections(toSave);
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            FileWriter writer = new FileWriter(targetFile.toFile());
            try {
                GSON.toJson(toSave, writer);
                writer.close();
                INITIAL_STATE = copyData(current);
                return true;
            } finally {
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean saveCurrentProfileDeltaToFile(Path targetFile) {
        if (targetFile == null) {
            return false;
        }
        try {
            Data current = captureCurrent();
            Data toSave = buildProfileSaveData(targetFile, current);
            if (toSave == null) {
                toSave = new Data();
            }
            pruneEmptyCollections(toSave);
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent, new FileAttribute[0]);
            }
            FileWriter writer = new FileWriter(targetFile.toFile());
            try {
                GSON.toJson(toSave, writer);
                writer.close();
                return true;
            } finally {
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static Data buildProfileSaveData(Path targetFile, Data current) {
        Data sessionDelta = INITIAL_STATE != null ? computeDelta(INITIAL_STATE, current) : copyData(current);
        if (sessionDelta == null) {
            sessionDelta = new Data();
        }
        Data mergedProfile = mergeData(readDataFile(targetFile), copyData(sessionDelta));
        if (mergedProfile == null) {
            mergedProfile = new Data();
        }
        if (DEFAULTS_RESOURCE_STATE == null) {
            pruneEmptyCollections(mergedProfile);
            return mergedProfile;
        }
        Data effective = mergeData(copyData(DEFAULTS_RESOURCE_STATE), copyData(mergedProfile));
        Data canonical = computeDelta(DEFAULTS_RESOURCE_STATE, effective);
        if (canonical == null) {
            canonical = new Data();
        }
        if (mergedProfile.itemSpecific != null && !mergedProfile.itemSpecific.isEmpty()) {
            canonical.itemSpecific = copySpecificMapForProfile(mergedProfile.itemSpecific);
        }
        pruneEmptyCollections(canonical);
        return canonical;
    }

    private static Map<String, SpecificEntry> copySpecificMapForProfile(Map<String, SpecificEntry> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, SpecificEntry> copy = new HashMap<>();
        for (Map.Entry<String, SpecificEntry> entry : source.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), copySpecificEntryForProfile(entry.getValue()));
            }
        }
        if (copy.isEmpty()) {
            return null;
        }
        return copy;
    }

    private static Data readDataFile(Path file) {
        if (file == null || !Files.isRegularFile(file, new LinkOption[0])) {
            return null;
        }
        try {
            FileReader reader = new FileReader(file.toFile());
            try {
                Data data = (Data) GSON.fromJson(reader, Data.class);
                reader.close();
                return data;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyMixSelectionsFromActiveProfile(JsonObject activeProfileJson) {
    }

    private static void persistMixSelectionsToActiveProfile() {
    }

    private static JsonObject readJsonObjectFile(Path file) {
        if (file == null || !Files.isRegularFile(file, new LinkOption[0])) {
            return null;
        }
        try {
            FileReader reader = new FileReader(file.toFile());
            try {
                JsonElement element = (JsonElement) GSON.fromJson(reader, JsonElement.class);
                JsonObject asJsonObject = (element == null || !element.isJsonObject()) ? null : element.getAsJsonObject();
                reader.close();
                return asJsonObject;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void injectMixSelectionsIntoProfileJson(JsonObject root, PunchyConfig.Data snapshot) {
        if (root == null) {
            return;
        }
        if (snapshot == null) {
            root.remove("animationPackSelections");
            root.remove("animationPackTypeSelections");
            root.remove("mixSpecificForceOverride");
        } else {
            writeStringMap(root, "animationPackSelections", snapshot.animationPackSelections);
            writeStringMap(root, "animationPackTypeSelections", snapshot.animationPackTypeSelections);
            writeBooleanMap(root, "mixSpecificForceOverride", snapshot.mixSpecificForceOverride);
        }
    }

    private static LinkedHashMap<String, String> readStringMap(JsonObject root, String key) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (root == null || key == null || !root.has(key) || !root.get(key).isJsonObject()) {
            return result;
        }
        JsonObject obj = root.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && !entry.getValue().isJsonNull()) {
                try {
                    String value = entry.getValue().getAsString();
                    if (value != null && !value.isBlank()) {
                        result.put(entry.getKey(), value);
                    }
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    private static LinkedHashMap<String, Boolean> readBooleanMap(JsonObject root, String key) {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        if (root == null || key == null || !root.has(key) || !root.get(key).isJsonObject()) {
            return result;
        }
        JsonObject obj = root.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && !entry.getValue().isJsonNull()) {
                try {
                    result.put(entry.getKey(), Boolean.valueOf(entry.getValue().getAsBoolean()));
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    private static void writeStringMap(JsonObject root, String key, Map<String, String> values) {
        if (root == null || key == null) {
            return;
        }
        if (values == null || values.isEmpty()) {
            root.remove(key);
            return;
        }
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && !entry.getValue().isBlank()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        if (obj.size() == 0) {
            root.remove(key);
        } else {
            root.add(key, obj);
        }
    }

    private static void writeBooleanMap(JsonObject root, String key, Map<String, Boolean> values) {
        if (root == null || key == null) {
            return;
        }
        if (values == null || values.isEmpty()) {
            root.remove(key);
            return;
        }
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        if (obj.size() == 0) {
            root.remove(key);
        } else {
            root.add(key, obj);
        }
    }

    public static boolean exportTuningHelperDevAndOpen() {
        try {
            Path exportDir = getConfigDirectory().resolve(Punchy.MOD_ID).resolve("dev_tuning_helper");
            Files.createDirectories(exportDir, new FileAttribute[0]);
            Path target = exportDir.resolve("tuning_helper_dev.json");
            if (!saveCurrentDeltaToFile(target)) {
                return false;
            }
            class_156.method_668().method_60932(exportDir);
            return true;
        } catch (Exception e) {
            try {
                Path exportDir2 = getConfigDirectory().resolve(Punchy.MOD_ID).resolve("dev_tuning_helper");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(exportDir2.toFile());
                    return true;
                }
                return false;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private static void saveHelperData(Data data) {
        Path configDir = getConfigDirectory();
        try {
            Files.createDirectories(configDir.resolve(Punchy.MOD_ID), new FileAttribute[0]);
            File file = configDir.resolve(Punchy.MOD_ID).resolve("creator_tuning_helper.json").toFile();
            FileWriter writer = new FileWriter(file);
            try {
                GSON.toJson(data, writer);
                writer.close();
            } finally {
            }
        } catch (Exception e) {
        }
    }

    private static Data captureCurrent() {
        Data d = new Data();
        for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
            GroupEntry entry = new GroupEntry();
            boolean hasData = false;
            SideSet ss = null;
            if (!ToolTuning.isKindAttached(kind, false)) {
                ss = new SideSet();
                ss.right = Pose.fromTool(ToolTuning.snapshot(kind, false));
            }
            if (!ToolTuning.isKindAttached(kind, true)) {
                if (ss == null) {
                    ss = new SideSet();
                }
                ss.left = Pose.fromTool(ToolTuning.snapshot(kind, true));
            }
            if (ss != null) {
                entry.transforms = ss;
                hasData = true;
            }
            PlayerArmModelLayer.ToolKind armKind = null;
            try {
                armKind = PlayerArmModelLayer.ToolKind.valueOf(kind.name());
            } catch (IllegalArgumentException e) {
            }
            if (armKind != null) {
                SideSet armSS = null;
                if (!ArmToolTuning.isKindAttached(armKind, false)) {
                    ArmToolTuning.Profile rightProfile = ArmToolTuning.snapshot(armKind, false);
                    if (!isImplicitEmptyHandArmProfile(armKind, false, rightProfile)) {
                        armSS = new SideSet();
                        armSS.right = Pose.fromArm(normalizeKindArmProfileForSerialization(armKind, false, rightProfile));
                    }
                }
                if (!ArmToolTuning.isKindAttached(armKind, true)) {
                    ArmToolTuning.Profile leftProfile = ArmToolTuning.snapshot(armKind, true);
                    if (!isImplicitEmptyHandArmProfile(armKind, true, leftProfile)) {
                        if (armSS == null) {
                            armSS = new SideSet();
                        }
                        armSS.left = Pose.fromArm(normalizeKindArmProfileForSerialization(armKind, true, leftProfile));
                    }
                }
                if (armSS != null) {
                    entry.armTransforms = armSS;
                    hasData = true;
                }
            }
            if (hasData) {
                d.item.put(kind.name(), entry);
            }
        }
        SideSet globalItem = new SideSet();
        globalItem.right = Pose.fromTool(ToolTuning.snapshotGlobal(false));
        globalItem.left = Pose.fromTool(ToolTuning.snapshotGlobal(true));
        d.globalItem = globalItem;
        SideSet globalArm = new SideSet();
        globalArm.right = Pose.fromArm(ArmToolTuning.snapshotGlobal(false));
        globalArm.left = Pose.fromArm(ArmToolTuning.snapshotGlobal(true));
        d.globalArm = globalArm;
        SideSet meshSet = new SideSet();
        meshSet.right = Pose.fromArmMesh(ArmMeshTuning.snapshotGlobal(false));
        meshSet.left = Pose.fromArmMesh(ArmMeshTuning.snapshotGlobal(true));
        d.armMesh.put(ARM_MESH_SIDE_KEY, meshSet);
        d.itemSpecific = captureSpecificCurrent();
        d.chestProxy = captureChestProxy();
        d.fluidProxy = captureFluidProxy();
        d.enchantParticleProxy = captureEnchantParticleProxy();
        d.paddleProxy = capturePaddleProxy();
        d.fishingLineProxy = captureFishingLineProxy();
        return d;
    }

    private static void rememberBaseline() {
        BASELINE_STATE = captureCurrent();
    }

    private static boolean isImplicitEmptyHandArmProfile(PlayerArmModelLayer.ToolKind kind, boolean left, ArmToolTuning.Profile current) {
        if (kind != PlayerArmModelLayer.ToolKind.EMPTY_HAND || current == null) {
            return false;
        }
        ArmToolTuning.Profile defaultEmpty = getDefaultArmEditorBase(kind, left);
        ArmToolTuning.Profile defaultGlobal = getDefaultGlobalArmBase(left);
        ArmToolTuning.Profile currentGlobal = ArmToolTuning.snapshotGlobal(left);
        return defaultEmpty != null && defaultGlobal != null && currentGlobal != null && nearlyEqual(current.scale, currentGlobal.scale + (defaultEmpty.scale - defaultGlobal.scale)) && nearlyEqual(current.offX, currentGlobal.offX + (defaultEmpty.offX - defaultGlobal.offX)) && nearlyEqual(current.offY, currentGlobal.offY + (defaultEmpty.offY - defaultGlobal.offY)) && nearlyEqual(current.offZ, currentGlobal.offZ + (defaultEmpty.offZ - defaultGlobal.offZ)) && nearlyEqual(current.rotX, defaultEmpty.rotX) && nearlyEqual(current.rotY, defaultEmpty.rotY) && nearlyEqual(current.rotZ, defaultEmpty.rotZ);
    }

    public static ArmToolTuning.Profile getDefaultArmEditorBase(PlayerArmModelLayer.ToolKind kind, boolean left) {
        GroupEntry group;
        Data defaults = DEFAULTS_RESOURCE_STATE;
        if (defaults == null || kind == null) {
            return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        SideSet direct = null;
        if (kind == PlayerArmModelLayer.ToolKind.EMPTY_HAND && defaults.emptyHand != null) {
            direct = defaults.emptyHand.armTransforms;
        }
        if (direct == null && defaults.item != null && (group = defaults.item.get(kind.name())) != null) {
            direct = group.armTransforms;
        }
        Pose pose = direct != null ? left ? direct.left : direct.right : null;
        if (pose != null) {
            return pose.toArm();
        }
        SideSet global = defaults.globalArm;
        Pose globalPose = global != null ? left ? global.left : global.right : null;
        if (globalPose != null) {
            return globalPose.toArm();
        }
        return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static ArmToolTuning.Profile getDefaultGlobalArmBase(boolean left) {
        Data defaults = DEFAULTS_RESOURCE_STATE;
        if (defaults == null || defaults.globalArm == null) {
            return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        Pose pose = left ? defaults.globalArm.left : defaults.globalArm.right;
        if (pose != null) {
            return pose.toArm();
        }
        return new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static Map<String, SpecificEntry> captureSpecificCurrent() {
        Map<String, SpecificEntry> out = new HashMap<>();
        List<ToolTuning.SpecificSnapshotDetailed> itemSnapshotDetailed = ToolTuning.snapshotSpecificsDetailed();
        Map<class_2960, ArmToolTuning.SpecificSnapshot> armSnapshot = ArmToolTuning.snapshotSpecifics();
        Set<class_2960> capturedItemIds = new HashSet<>();
        Iterator<ToolTuning.SpecificSnapshotDetailed> it = itemSnapshotDetailed.iterator();
        while (it.hasNext()) {
            ToolTuning.SpecificSnapshotDetailed itemSpec = it.next();
            if (itemSpec != null && itemSpec.itemId() != null) {
                class_2960 id = itemSpec.itemId();
                SpecificEntry entry = new SpecificEntry();
                VanillaFirstPersonItemLayer.ToolKind resolvedItemKind = (itemSpec == null || itemSpec.kind() == null) ? resolveCurrentItemKind(id) : itemSpec.kind();
                if (resolvedItemKind != null) {
                    entry.kind = resolvedItemKind.name();
                }
                ToolTuning.Profile itemRight = resolveEffectiveSpecificItemProfile(itemSpec.right(), resolvedItemKind, false);
                ToolTuning.Profile itemLeft = resolveEffectiveSpecificItemProfile(itemSpec.left(), resolvedItemKind, true);
                entry.transforms = captureSpecificItemTransforms(itemRight, itemLeft);
                applySpecificToolMetadata(entry, entry.transforms, itemRight, itemLeft);
                entry.customModelData = itemSpec.customModelData();
                entry.renamed = itemSpec.renamed();
                if (itemSpec.model() != null && !itemSpec.model().isBlank()) {
                    entry.model = itemSpec.model();
                }
                ArmToolTuning.SpecificSnapshot armSpec = armSnapshot.get(id);
                PlayerArmModelLayer.ToolKind resolvedArmKind = (armSpec == null || armSpec.kind() == null) ? resolveCurrentArmKind(id) : armSpec.kind();
                if (resolvedArmKind != null) {
                    entry.armKind = resolvedArmKind.name();
                }
                ArmToolTuning.Profile armRight = resolveEffectiveSpecificArmProfile(armSpec != null ? armSpec.right() : null, resolvedArmKind, false);
                ArmToolTuning.Profile armLeft = resolveEffectiveSpecificArmProfile(armSpec != null ? armSpec.left() : null, resolvedArmKind, true);
                entry.armTransforms = captureSpecificArmTransforms(resolvedArmKind, armRight, armLeft);
                if (entry.transforms != null || entry.armTransforms != null || entry.kind != null || entry.armKind != null) {
                    String key = specificStorageKey(id, entry.customModelData, entry.renamed);
                    if (key != null && !key.isBlank()) {
                        out.put(key, entry);
                        capturedItemIds.add(id);
                    }
                }
            }
        }
        for (Map.Entry<class_2960, ArmToolTuning.SpecificSnapshot> armEntry : armSnapshot.entrySet()) {
            class_2960 id2 = armEntry.getKey();
            ArmToolTuning.SpecificSnapshot armSpec2 = armEntry.getValue();
            if (id2 != null && armSpec2 != null && !capturedItemIds.contains(id2)) {
                String armKey = id2.toString();
                SpecificEntry entry2 = out.getOrDefault(armKey, new SpecificEntry());
                PlayerArmModelLayer.ToolKind resolvedArmKind2 = armSpec2.kind() != null ? armSpec2.kind() : resolveCurrentArmKind(id2);
                if (resolvedArmKind2 != null) {
                    entry2.armKind = resolvedArmKind2.name();
                }
                ArmToolTuning.Profile armRight2 = resolveEffectiveSpecificArmProfile(armSpec2.right(), resolvedArmKind2, false);
                ArmToolTuning.Profile armLeft2 = resolveEffectiveSpecificArmProfile(armSpec2.left(), resolvedArmKind2, true);
                entry2.armTransforms = captureSpecificArmTransforms(resolvedArmKind2, armRight2, armLeft2);
                if (entry2.transforms != null || entry2.armTransforms != null || entry2.kind != null || entry2.armKind != null) {
                    out.put(armKey, entry2);
                }
            }
        }
        return out;
    }

    private static String specificStorageKey(class_2960 itemId, Integer customModelData, String renamed) {
        if (itemId == null) {
            return null;
        }
        StringBuilder key = new StringBuilder(itemId.toString());
        if (customModelData != null) {
            key.append('#').append(customModelData);
        }
        if (renamed != null && !renamed.isBlank()) {
            key.append('|').append(renamed);
        }
        return key.toString();
    }

    private static VanillaFirstPersonItemLayer.ToolKind resolveCurrentItemKind(class_2960 id) {
        class_1792 item;
        if (id == null || (item = (class_1792) class_7923.field_41178.method_17966(id).orElse(class_1802.field_8162)) == class_1802.field_8162) {
            return null;
        }
        class_1799 stack = new class_1799(item);
        return ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
    }

    private static PlayerArmModelLayer.ToolKind resolveCurrentArmKind(class_2960 id) {
        class_1792 item;
        if (id == null || (item = (class_1792) class_7923.field_41178.method_17966(id).orElse(class_1802.field_8162)) == class_1802.field_8162) {
            return null;
        }
        class_1799 stack = new class_1799(item);
        return ToolKindResolver.resolveArmKind(stack, PlayerArmModelLayer.ToolKind.AXE);
    }

    private static SideSet captureCurrentItemKindTransforms(VanillaFirstPersonItemLayer.ToolKind kind) {
        if (kind == null) {
            return null;
        }
        SideSet set = new SideSet();
        if (shouldCaptureCurrentItemKindSide(kind, false)) {
            set.right = Pose.fromTool(ToolTuning.snapshot(kind, false));
        }
        if (shouldCaptureCurrentItemKindSide(kind, true)) {
            set.left = Pose.fromTool(ToolTuning.snapshot(kind, true));
        }
        if (set.right == null && set.left == null) {
            return null;
        }
        return set;
    }

    private static SideSet captureCurrentArmKindTransforms(PlayerArmModelLayer.ToolKind kind) {
        if (kind == null) {
            return null;
        }
        SideSet set = new SideSet();
        if (!ArmToolTuning.isKindAttached(kind, false)) {
            set.right = Pose.fromArm(ArmToolTuning.snapshot(kind, false));
        }
        if (!ArmToolTuning.isKindAttached(kind, true)) {
            set.left = Pose.fromArm(ArmToolTuning.snapshot(kind, true));
        }
        if (set.right == null && set.left == null) {
            return null;
        }
        return set;
    }

    private static boolean shouldCaptureCurrentItemKindSide(VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        if (kind == null) {
            return false;
        }
        if (!ToolTuning.isKindAttached(kind, left)) {
            return true;
        }
        return hasSerializableToolMetadata(ToolTuning.get(kind, left));
    }

    private static boolean hasSerializableToolMetadata(ToolTuning.Profile profile) {
        if (profile == null) {
            return false;
        }
        return (profile.disableSprintLateralTilt == null && profile.keepArmTuning == null && profile.keepTriggerArmTuning == null && (profile.physicsMode == null || profile.physicsMode == ToolTuning.PhysicsMode.NONE) && profile.physicsSettings == null) ? false : true;
    }

    private static ToolTuning.Profile resolveEffectiveSpecificItemProfile(ToolTuning.Profile specific, VanillaFirstPersonItemLayer.ToolKind kind, boolean left) {
        ToolTuning.Profile fallback = null;
        if (kind != null && shouldCaptureCurrentItemKindSide(kind, left)) {
            fallback = ToolTuning.snapshot(kind, left);
        }
        if (specific == null) {
            return fallback;
        }
        ToolTuning.Profile merged = copyToolProfile(specific);
        if (fallback == null) {
            return merged;
        }
        if (merged.disableSprintLateralTilt == null) {
            merged.disableSprintLateralTilt = fallback.disableSprintLateralTilt;
        }
        if (merged.keepArmTuning == null) {
            merged.keepArmTuning = fallback.keepArmTuning;
        }
        if (merged.keepTriggerArmTuning == null) {
            merged.keepTriggerArmTuning = fallback.keepTriggerArmTuning;
        }
        return merged;
    }

    private static ArmToolTuning.Profile resolveEffectiveSpecificArmProfile(ArmToolTuning.Profile specific, PlayerArmModelLayer.ToolKind kind, boolean left) {
        ArmToolTuning.Profile fallback = kind != null ? ArmToolTuning.snapshot(kind, left) : null;
        if (specific == null) {
            return fallback;
        }
        return specific;
    }

    private static SideSet captureSpecificItemTransforms(ToolTuning.Profile right, ToolTuning.Profile left) {
        if (right == null && left == null) {
            return null;
        }
        SideSet set = new SideSet();
        if (right != null) {
            set.right = Pose.fromTool(right);
        }
        if (left != null) {
            set.left = Pose.fromTool(left);
        }
        return set;
    }

    private static SideSet captureSpecificArmTransforms(PlayerArmModelLayer.ToolKind kind, ArmToolTuning.Profile right, ArmToolTuning.Profile left) {
        if (right == null && left == null) {
            return null;
        }
        SideSet set = new SideSet();
        if (right != null) {
            set.right = Pose.fromArm(normalizeSpecificArmProfileForSerialization(kind, false, right));
        }
        if (left != null) {
            set.left = Pose.fromArm(normalizeSpecificArmProfileForSerialization(kind, true, left));
        }
        return set;
    }

    private static ArmToolTuning.Profile normalizeKindArmProfileForSerialization(PlayerArmModelLayer.ToolKind kind, boolean left, ArmToolTuning.Profile current) {
        return rebaseArmProfileWithGlobalOffsetsOnly(current, ArmToolTuning.getGlobal(left), getDefaultGlobalArmBase(left));
    }

    private static ArmToolTuning.Profile normalizeSpecificArmProfileForSerialization(PlayerArmModelLayer.ToolKind kind, boolean left, ArmToolTuning.Profile current) {
        return stripGlobalOffsetsFromSpecificArmProfile(current, ArmToolTuning.getGlobal(left));
    }

    private static ArmToolTuning.Profile deserializeKindArmProfileForRuntime(boolean left, ArmToolTuning.Profile stored) {
        return rebaseArmProfileWithGlobalOffsetsOnly(stored, getDefaultGlobalArmBase(left), ArmToolTuning.getGlobal(left));
    }

    private static ArmToolTuning.Profile deserializeSpecificArmProfileForRuntime(PlayerArmModelLayer.ToolKind kind, boolean left, ArmToolTuning.Profile stored) {
        return applyGlobalOffsetsToSpecificArmProfile(stored, ArmToolTuning.getGlobal(left));
    }

    private static ArmToolTuning.Profile deserializeSpecificArmProfileFromSource(PlayerArmModelLayer.ToolKind kind, boolean left, ArmToolTuning.Profile stored) {
        if (stored == null) {
            return null;
        }
        return deserializeSpecificArmProfileForRuntime(kind, left, stored);
    }

    private static ArmToolTuning.Profile rebaseArmProfileWithGlobalOffsetsOnly(ArmToolTuning.Profile current, ArmToolTuning.Profile currentBase, ArmToolTuning.Profile targetBase) {
        ArmToolTuning.Profile profile;
        ArmToolTuning.Profile profile2;
        if (current == null) {
            return null;
        }
        if (currentBase != null) {
            profile = currentBase;
        } else {
            profile = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile sourceBase = profile;
        if (targetBase != null) {
            profile2 = targetBase;
        } else {
            profile2 = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile destBase = profile2;
        return new ArmToolTuning.Profile(destBase.scale + (current.scale - sourceBase.scale), destBase.offX + (current.offX - sourceBase.offX), destBase.offY + (current.offY - sourceBase.offY), destBase.offZ + (current.offZ - sourceBase.offZ), current.rotX, current.rotY, current.rotZ);
    }

    private static ArmToolTuning.Profile stripGlobalOffsetsFromSpecificArmProfile(ArmToolTuning.Profile current, ArmToolTuning.Profile globalBase) {
        ArmToolTuning.Profile profile;
        if (current == null) {
            return null;
        }
        if (globalBase != null) {
            profile = globalBase;
        } else {
            profile = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile global = profile;
        return new ArmToolTuning.Profile(1.0f + (current.scale - global.scale), current.offX - global.offX, current.offY - global.offY, current.offZ - global.offZ, current.rotX, current.rotY, current.rotZ);
    }

    private static ArmToolTuning.Profile applyGlobalOffsetsToSpecificArmProfile(ArmToolTuning.Profile stored, ArmToolTuning.Profile globalBase) {
        ArmToolTuning.Profile profile;
        if (stored == null) {
            return null;
        }
        if (globalBase != null) {
            profile = globalBase;
        } else {
            profile = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile global = profile;
        return new ArmToolTuning.Profile(global.scale + (stored.scale - 1.0f), global.offX + stored.offX, global.offY + stored.offY, global.offZ + stored.offZ, stored.rotX, stored.rotY, stored.rotZ);
    }

    private static ArmToolTuning.Profile rebaseArmProfile(ArmToolTuning.Profile current, ArmToolTuning.Profile currentBase, ArmToolTuning.Profile targetBase) {
        ArmToolTuning.Profile profile;
        ArmToolTuning.Profile profile2;
        if (current == null) {
            return null;
        }
        if (currentBase != null) {
            profile = currentBase;
        } else {
            profile = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile sourceBase = profile;
        if (targetBase != null) {
            profile2 = targetBase;
        } else {
            profile2 = new ArmToolTuning.Profile(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }
        ArmToolTuning.Profile destBase = profile2;
        return new ArmToolTuning.Profile(destBase.scale + (current.scale - sourceBase.scale), destBase.offX + (current.offX - sourceBase.offX), destBase.offY + (current.offY - sourceBase.offY), destBase.offZ + (current.offZ - sourceBase.offZ), destBase.rotX + (current.rotX - sourceBase.rotX), destBase.rotY + (current.rotY - sourceBase.rotY), destBase.rotZ + (current.rotZ - sourceBase.rotZ));
    }

    private static void applySpecificToolMetadata(SpecificEntry entry, SideSet transforms, ToolTuning.Profile right, ToolTuning.Profile left) {
        if (entry == null) {
            return;
        }
        Boolean rightDisable = right != null ? right.disableSprintLateralTilt : null;
        Boolean leftDisable = left != null ? left.disableSprintLateralTilt : null;
        if (right != null && left != null && Objects.equals(rightDisable, leftDisable) && rightDisable != null) {
            entry.disableSprintLateralTilt = rightDisable;
            clearPoseDisableSprint(transforms);
        }
        Boolean rightKeep = right != null ? right.keepArmTuning : null;
        Boolean leftKeep = left != null ? left.keepArmTuning : null;
        if (right != null && left != null) {
            if (Objects.equals(rightKeep, leftKeep) && rightKeep != null) {
                entry.keepArmTuning = rightKeep;
                clearPoseKeepArmTuning(transforms);
            } else {
                if (rightKeep != null) {
                    entry.keepArmTuningR = rightKeep;
                    if (transforms != null && transforms.right != null) {
                        transforms.right.keepArmTuning = null;
                    }
                }
                if (leftKeep != null) {
                    entry.keepArmTuningL = leftKeep;
                    if (transforms != null && transforms.left != null) {
                        transforms.left.keepArmTuning = null;
                    }
                }
            }
        }
        Boolean rightTrigger = right != null ? right.keepTriggerArmTuning : null;
        Boolean leftTrigger = left != null ? left.keepTriggerArmTuning : null;
        if (right != null && left != null && Objects.equals(rightTrigger, leftTrigger) && rightTrigger != null) {
            entry.keepTriggerArmTuning = rightTrigger;
            clearPoseKeepTriggerArmTuning(transforms);
        }
        JsonElement physics = buildSharedPhysicsDefinition(right, left);
        if (physics != null) {
            entry.physics = physics;
        }
    }

    private static void clearPoseDisableSprint(SideSet transforms) {
        if (transforms == null) {
            return;
        }
        if (transforms.right != null) {
            transforms.right.disableSprintLateralTilt = null;
        }
        if (transforms.left != null) {
            transforms.left.disableSprintLateralTilt = null;
        }
    }

    private static void clearPoseKeepArmTuning(SideSet transforms) {
        if (transforms == null) {
            return;
        }
        if (transforms.right != null) {
            transforms.right.keepArmTuning = null;
        }
        if (transforms.left != null) {
            transforms.left.keepArmTuning = null;
        }
    }

    private static void clearPoseKeepTriggerArmTuning(SideSet transforms) {
        if (transforms == null) {
            return;
        }
        if (transforms.right != null) {
            transforms.right.keepTriggerArmTuning = null;
        }
        if (transforms.left != null) {
            transforms.left.keepTriggerArmTuning = null;
        }
    }

    private static JsonElement buildSharedPhysicsDefinition(ToolTuning.Profile right, ToolTuning.Profile left) {
        ToolTuning.Profile source = right != null ? right : left;
        if (source == null) {
            return null;
        }
        if (right != null && left != null && !samePhysicsDefinition(right, left)) {
            return null;
        }
        boolean hasMode = (source.physicsMode == null || source.physicsMode == ToolTuning.PhysicsMode.NONE) ? false : true;
        if (!hasMode && source.physicsSettings == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        if (hasMode) {
            obj.addProperty("name", source.physicsMode.name());
        }
        ToolTuning.PhysicsSettings settings = source.physicsSettings;
        if (settings != null) {
            if (settings.limitDeg != null) {
                obj.addProperty("limitDeg", settings.limitDeg);
            }
            if (settings.limitDegUp != null) {
                obj.addProperty("limitDegUp", settings.limitDegUp);
            }
            if (settings.limitDegDown != null) {
                obj.addProperty("limitDegDown", settings.limitDegDown);
            }
            if (settings.force != null) {
                obj.addProperty("force", settings.force);
            }
            if (settings.speed != null) {
                obj.addProperty("speed", settings.speed);
            }
            if (settings.cameraSens != null) {
                obj.addProperty("cameraSens", settings.cameraSens);
            }
            if (settings.gravity != null) {
                obj.addProperty("gravity", settings.gravity);
            }
            if (settings.motionSens != null) {
                obj.addProperty("motionSens", settings.motionSens);
            }
            if (settings.invert != null) {
                obj.addProperty("invert", settings.invert);
            }
            if (settings.handSpace != null) {
                obj.addProperty("handSpace", settings.handSpace);
            }
            if (settings.enableCreaturePhysics != null) {
                obj.addProperty("enableCreaturePhysics", settings.enableCreaturePhysics);
            }
        }
        if (obj.entrySet().isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray();
        array.add(obj);
        return array;
    }

    private static boolean samePhysicsDefinition(ToolTuning.Profile a, ToolTuning.Profile b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.physicsMode != b.physicsMode) {
            return false;
        }
        ToolTuning.PhysicsSettings as = a.physicsSettings;
        ToolTuning.PhysicsSettings bs = b.physicsSettings;
        if (as == bs) {
            return true;
        }
        return as != null && bs != null && Objects.equals(as.limitDeg, bs.limitDeg) && Objects.equals(as.limitDegUp, bs.limitDegUp) && Objects.equals(as.limitDegDown, bs.limitDegDown) && Objects.equals(as.force, bs.force) && Objects.equals(as.speed, bs.speed) && Objects.equals(as.cameraSens, bs.cameraSens) && Objects.equals(as.gravity, bs.gravity) && Objects.equals(as.motionSens, bs.motionSens) && Objects.equals(as.invert, bs.invert) && Objects.equals(as.handSpace, bs.handSpace) && Objects.equals(as.enableCreaturePhysics, bs.enableCreaturePhysics);
    }

    private static Data computeDelta(Data base, Data current) {
        SpecificEntry baseEntry;
        JsonElement jsonElementDiffJson;
        Boolean bool;
        Boolean bool2;
        Boolean bool3;
        Boolean bool4;
        Boolean bool5;
        SideSet transforms;
        SideSet armTransforms;
        if (base == null) {
            return current;
        }
        Data delta = new Data();
        boolean hasData = false;
        delta.globalItem = diffSideSet(current.globalItem, base.globalItem);
        if (delta.globalItem != null) {
            hasData = true;
        }
        delta.globalArm = diffSideSet(current.globalArm, base.globalArm);
        if (delta.globalArm != null) {
            hasData = true;
        }
        Map<String, GroupEntry> itemDiff = new HashMap<>();
        if (current.item != null) {
            for (VanillaFirstPersonItemLayer.ToolKind kind : VanillaFirstPersonItemLayer.ToolKind.values()) {
                GroupEntry currentEntry = current.item.get(kind.name());
                GroupEntry baseEntry2 = base.item != null ? base.item.get(kind.name()) : null;
                SideSet diffTransforms = null;
                SideSet diffArmTransforms = null;
                if (currentEntry != null) {
                    diffTransforms = diffSideSet(currentEntry.transforms, baseEntry2 != null ? baseEntry2.transforms : null);
                    diffArmTransforms = diffSideSet(currentEntry.armTransforms, baseEntry2 != null ? baseEntry2.armTransforms : null);
                }
                if (diffTransforms != null || diffArmTransforms != null) {
                    GroupEntry diffEntry = new GroupEntry();
                    diffEntry.transforms = diffTransforms;
                    diffEntry.armTransforms = diffArmTransforms;
                    itemDiff.put(kind.name(), diffEntry);
                    hasData = true;
                }
            }
        }
        delta.item = itemDiff.isEmpty() ? null : itemDiff;
        Map<String, SideSet> meshDiff = new HashMap<>();
        if (current.armMesh != null) {
            SideSet currentSet = current.armMesh.get(ARM_MESH_SIDE_KEY);
            SideSet baseSet = base.armMesh != null ? base.armMesh.get(ARM_MESH_SIDE_KEY) : null;
            SideSet diff = diffSideSet(currentSet, baseSet);
            if (diff != null) {
                meshDiff.put(ARM_MESH_SIDE_KEY, diff);
                hasData = true;
            }
        }
        delta.armMesh = meshDiff.isEmpty() ? null : meshDiff;
        Map<String, Pose> chestProxyDiff = diffPoseMap(current.chestProxy, base.chestProxy);
        delta.chestProxy = (chestProxyDiff == null || chestProxyDiff.isEmpty()) ? null : chestProxyDiff;
        Map<String, Pose> fluidProxyDiff = diffPoseMap(current.fluidProxy, base.fluidProxy);
        delta.fluidProxy = (fluidProxyDiff == null || fluidProxyDiff.isEmpty()) ? null : fluidProxyDiff;
        Map<String, Pose> enchantParticleProxyDiff = diffPoseMap(current.enchantParticleProxy, base.enchantParticleProxy);
        delta.enchantParticleProxy = (enchantParticleProxyDiff == null || enchantParticleProxyDiff.isEmpty()) ? null : enchantParticleProxyDiff;
        Map<String, Pose> paddleProxyDiff = diffPoseMap(current.paddleProxy, base.paddleProxy);
        delta.paddleProxy = (paddleProxyDiff == null || paddleProxyDiff.isEmpty()) ? null : paddleProxyDiff;
        Map<String, Pose> fishingLineProxyDiff = diffPoseMap(current.fishingLineProxy, base.fishingLineProxy);
        delta.fishingLineProxy = (fishingLineProxyDiff == null || fishingLineProxyDiff.isEmpty()) ? null : fishingLineProxyDiff;
        if (delta.chestProxy != null || delta.fluidProxy != null || delta.enchantParticleProxy != null || delta.paddleProxy != null || delta.fishingLineProxy != null) {
            hasData = true;
        }
        Map<String, SpecificEntry> specificDiff = new HashMap<>();
        Map<String, SpecificEntry> currentSpecific = current.itemSpecific != null ? current.itemSpecific : Collections.emptyMap();
        Map<String, SpecificEntry> baseSpecific = base.itemSpecific != null ? base.itemSpecific : Collections.emptyMap();
        for (Map.Entry<String, SpecificEntry> entry : currentSpecific.entrySet()) {
            SpecificEntry currentEntry2 = entry.getValue();
            if (currentEntry2 != null && ((baseEntry = baseSpecific.get(entry.getKey())) != null || !isRedundantResolvedSpecificEntry(entry.getKey(), currentEntry2))) {
                if (baseEntry == null) {
                    jsonElementDiffJson = copyJson(currentEntry2.physics);
                } else {
                    jsonElementDiffJson = diffJson(currentEntry2.physics, baseEntry.physics);
                }
                JsonElement physics = jsonElementDiffJson;
                boolean physicsDiffers = physics != null;
                if (baseEntry == null) {
                    bool = currentEntry2.disableSprintLateralTilt;
                } else {
                    bool = (Boolean) diffNullable(currentEntry2.disableSprintLateralTilt, baseEntry.disableSprintLateralTilt);
                }
                Boolean disableSprintLateralTilt = bool;
                if (baseEntry == null) {
                    bool2 = currentEntry2.keepArmTuning;
                } else {
                    bool2 = (Boolean) diffNullable(currentEntry2.keepArmTuning, baseEntry.keepArmTuning);
                }
                Boolean keepArmTuning = bool2;
                if (baseEntry == null) {
                    bool3 = currentEntry2.keepTriggerArmTuning;
                } else {
                    bool3 = (Boolean) diffNullable(currentEntry2.keepTriggerArmTuning, baseEntry.keepTriggerArmTuning);
                }
                Boolean keepTriggerArmTuning = bool3;
                if (baseEntry == null) {
                    bool4 = currentEntry2.keepArmTuningL;
                } else {
                    bool4 = (Boolean) diffNullable(currentEntry2.keepArmTuningL, baseEntry.keepArmTuningL);
                }
                Boolean keepArmTuningL = bool4;
                if (baseEntry == null) {
                    bool5 = currentEntry2.keepArmTuningR;
                } else {
                    bool5 = (Boolean) diffNullable(currentEntry2.keepArmTuningR, baseEntry.keepArmTuningR);
                }
                Boolean keepArmTuningR = bool5;
                if (baseEntry == null) {
                    transforms = copySideSet(currentEntry2.transforms);
                } else {
                    transforms = diffSideSet(currentEntry2.transforms, baseEntry.transforms);
                }
                boolean kindDiffers = baseEntry == null ? currentEntry2.kind != null : !Objects.equals(currentEntry2.kind, baseEntry.kind);
                if (baseEntry == null) {
                    armTransforms = copySideSet(currentEntry2.armTransforms);
                } else {
                    armTransforms = diffSideSet(currentEntry2.armTransforms, baseEntry.armTransforms);
                }
                boolean armKindDiffers = baseEntry == null ? currentEntry2.armKind != null : !Objects.equals(currentEntry2.armKind, baseEntry.armKind);
                if (transforms != null || armTransforms != null || kindDiffers || armKindDiffers || physicsDiffers || disableSprintLateralTilt != null || keepArmTuning != null || keepTriggerArmTuning != null || keepArmTuningL != null || keepArmTuningR != null) {
                    SpecificEntry diffEntry2 = new SpecificEntry();
                    if (kindDiffers) {
                        diffEntry2.kind = currentEntry2.kind;
                    }
                    if (armKindDiffers) {
                        diffEntry2.armKind = currentEntry2.armKind;
                    }
                    if (physicsDiffers) {
                        diffEntry2.physics = physics;
                    }
                    diffEntry2.disableSprintLateralTilt = disableSprintLateralTilt;
                    diffEntry2.keepArmTuning = keepArmTuning;
                    diffEntry2.keepTriggerArmTuning = keepTriggerArmTuning;
                    diffEntry2.keepArmTuningL = keepArmTuningL;
                    diffEntry2.keepArmTuningR = keepArmTuningR;
                    diffEntry2.transforms = transforms;
                    diffEntry2.armTransforms = armTransforms;
                    specificDiff.put(entry.getKey(), diffEntry2);
                    hasData = true;
                }
            }
        }
        delta.itemSpecific = specificDiff.isEmpty() ? null : specificDiff;
        pruneEmptyCollections(delta);
        if (!hasData && delta.itemSpecific == null && delta.item == null && delta.globalItem == null && delta.globalArm == null) {
            return new Data();
        }
        return delta;
    }

    private static boolean isRedundantResolvedSpecificEntry(String itemKey, SpecificEntry entry) {
        VanillaFirstPersonItemLayer.ToolKind toolKindResolveCurrentItemKind;
        PlayerArmModelLayer.ToolKind toolKindResolveCurrentArmKind;
        if (entry == null || hasExplicitSpecificMetadata(entry)) {
            return false;
        }
        class_2960 itemId = class_2960.method_12829(itemKey);
        if (entry.kind != null) {
            toolKindResolveCurrentItemKind = parseItemKind(entry.kind);
        } else {
            toolKindResolveCurrentItemKind = resolveCurrentItemKind(itemId);
        }
        VanillaFirstPersonItemLayer.ToolKind resolvedItemKind = toolKindResolveCurrentItemKind;
        if (entry.armKind != null) {
            toolKindResolveCurrentArmKind = parseArmKind(entry.armKind);
        } else {
            toolKindResolveCurrentArmKind = resolveCurrentArmKind(itemId);
        }
        PlayerArmModelLayer.ToolKind resolvedArmKind = toolKindResolveCurrentArmKind;
        String resolvedItemKindName = resolvedItemKind != null ? resolvedItemKind.name() : null;
        String resolvedArmKindName = resolvedArmKind != null ? resolvedArmKind.name() : null;
        if (!Objects.equals(entry.kind, resolvedItemKindName) || !Objects.equals(entry.armKind, resolvedArmKindName)) {
            return false;
        }
        SideSet itemBase = captureResolvedSpecificItemBase(resolvedItemKind);
        if (!isSideSetEqual(entry.transforms, itemBase)) {
            return false;
        }
        SideSet armBase = captureResolvedSpecificArmBase(resolvedArmKind);
        return isSideSetEqual(entry.armTransforms, armBase);
    }

    private static VanillaFirstPersonItemLayer.ToolKind parseItemKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return VanillaFirstPersonItemLayer.ToolKind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static PlayerArmModelLayer.ToolKind parseArmKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PlayerArmModelLayer.ToolKind.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static SideSet captureResolvedSpecificItemBase(VanillaFirstPersonItemLayer.ToolKind kind) {
        if (kind == null) {
            return null;
        }
        SideSet set = new SideSet();
        set.right = Pose.fromTool(ToolTuning.snapshot(kind, false));
        set.left = Pose.fromTool(ToolTuning.snapshot(kind, true));
        if (set.right == null && set.left == null) {
            return null;
        }
        return set;
    }

    private static SideSet captureResolvedSpecificArmBase(PlayerArmModelLayer.ToolKind kind) {
        if (kind == null) {
            return null;
        }
        SideSet set = new SideSet();
        ArmToolTuning.Profile right = ArmToolTuning.get(kind, false);
        ArmToolTuning.Profile left = ArmToolTuning.get(kind, true);
        if (right != null) {
            set.right = Pose.fromArm(normalizeSpecificArmProfileForSerialization(kind, false, right));
        }
        if (left != null) {
            set.left = Pose.fromArm(normalizeSpecificArmProfileForSerialization(kind, true, left));
        }
        if (set.right == null && set.left == null) {
            return null;
        }
        return set;
    }

    private static boolean hasExplicitSpecificMetadata(SpecificEntry entry) {
        return (entry.renamed == null && entry.model == null && entry.customModelData == null && entry.animation == null && (entry.customAnimation == null || entry.customAnimation.isEmpty()) && ((entry.customItemAnimations == null || entry.customItemAnimations.isEmpty()) && ((entry.betterCombat == null || entry.betterCombat.isEmpty()) && ((entry.physics == null || entry.physics.isJsonNull()) && ((entry.particleEmitters == null || entry.particleEmitters.isJsonNull()) && ((entry.particle == null || entry.particle.isJsonNull()) && ((entry.particles == null || entry.particles.isJsonNull()) && ((entry.particleEffects == null || entry.particleEffects.isJsonNull()) && ((entry.particleEmission == null || entry.particleEmission.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null))))))))) ? false : true;
    }

    private static SideSet diffSideSet(SideSet current, SideSet base) {
        if (current == null) {
            return null;
        }
        SideSet diff = new SideSet();
        if (current.right != null) {
            diff.right = diffPose(current.right, base != null ? base.right : null);
        }
        if (current.left != null) {
            diff.left = diffPose(current.left, base != null ? base.left : null);
        }
        if (diff.right == null && diff.left == null) {
            return null;
        }
        return diff;
    }

    private static Map<String, Pose> diffPoseMap(Map<String, Pose> current, Map<String, Pose> base) {
        Pose value;
        if (current == null) {
            return null;
        }
        Map<String, Pose> diff = new HashMap<>();
        for (Map.Entry<String, Pose> entry : current.entrySet()) {
            if (entry != null && (value = entry.getValue()) != null) {
                Pose baseValue = base != null ? base.get(entry.getKey()) : null;
                Pose delta = diffPose(value, baseValue);
                if (delta != null) {
                    diff.put(entry.getKey(), delta);
                }
            }
        }
        if (diff.isEmpty()) {
            return null;
        }
        return diff;
    }

    private static JsonElement diffJson(JsonElement current, JsonElement base) {
        if (current == null || current.isJsonNull()) {
            return null;
        }
        if (base == null || base.isJsonNull() || !current.equals(base)) {
            return current.deepCopy();
        }
        return null;
    }

    private static JsonElement copyJson(JsonElement src) {
        if (src == null || src.isJsonNull()) {
            return null;
        }
        return src.deepCopy();
    }

    private static <T> T diffNullable(T current, T base) {
        if (!Objects.equals(current, base)) {
            return current;
        }
        return null;
    }

    private static SpecificEntry copySpecificEntryForProfile(SpecificEntry src) {
        if (src == null) {
            return null;
        }
        SpecificEntry copy = new SpecificEntry();
        copy.kind = src.kind;
        copy.armKind = src.armKind;
        copy.renamed = src.renamed;
        copy.model = src.model;
        copy.customModelData = src.customModelData;
        copy.animation = src.animation;
        copy.customAnimation = src.customAnimation;
        copy.customItemAnimations = src.customItemAnimations;
        copy.particleEmitters = copyJson(src.particleEmitters);
        copy.particle = copyJson(src.particle);
        copy.particles = copyJson(src.particles);
        copy.particleEffects = copyJson(src.particleEffects);
        copy.particleEmission = copyJson(src.particleEmission);
        copy.betterCombat = src.betterCombat;
        copy.physics = copyJson(src.physics);
        copy.disableSprintLateralTilt = src.disableSprintLateralTilt;
        copy.keepArmTuning = src.keepArmTuning;
        copy.keepTriggerArmTuning = src.keepTriggerArmTuning;
        copy.keepArmTuningL = src.keepArmTuningL;
        copy.keepArmTuningR = src.keepArmTuningR;
        copy.transforms = copySideSet(src.transforms);
        copy.armTransforms = copySideSet(src.armTransforms);
        return copy;
    }

    private static ToolTuning.Profile copyToolProfile(ToolTuning.Profile src) {
        if (src == null) {
            return null;
        }
        ToolTuning.Profile copy = new ToolTuning.Profile(src.scale, src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ, src.pivotX, src.pivotY, src.pivotZ, src.physicsMode);
        copy.disableSprintLateralTilt = src.disableSprintLateralTilt;
        copy.keepArmTuning = src.keepArmTuning;
        copy.keepTriggerArmTuning = src.keepTriggerArmTuning;
        if (src.physicsSettings != null) {
            copy.physicsSettings = src.physicsSettings.copy();
        }
        return copy;
    }

    private static Pose diffPose(Pose current, Pose base) {
        if (current == null) {
            return null;
        }
        if (base == null || !poseEquals(current, base)) {
            return copyPose(current);
        }
        return null;
    }

    private static Pose copyPose(Pose src) {
        if (src == null) {
            return null;
        }
        Pose copy = new Pose(src.scale, src.offX, src.offY, src.offZ, src.rotX, src.rotY, src.rotZ, src.pivotX, src.pivotY, src.pivotZ, src.physics);
        copy.disableSprintLateralTilt = src.disableSprintLateralTilt;
        copy.keepArmTuning = src.keepArmTuning;
        copy.keepTriggerArmTuning = src.keepTriggerArmTuning;
        copy.keepArmTuningL = src.keepArmTuningL;
        copy.keepArmTuningR = src.keepArmTuningR;
        return copy;
    }

    private static SideSet copySideSet(SideSet src) {
        if (src == null) {
            return null;
        }
        SideSet copy = new SideSet();
        copy.right = copyPose(src.right);
        copy.left = copyPose(src.left);
        if (copy.right == null && copy.left == null) {
            return null;
        }
        return copy;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean poseEquals(Pose a, Pose b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && nearlyEqual(a.scale, b.scale) && nearlyEqual(a.offX, b.offX) && nearlyEqual(a.offY, b.offY) && nearlyEqual(a.offZ, b.offZ) && nearlyEqual(a.rotX, b.rotX) && nearlyEqual(a.rotY, b.rotY) && nearlyEqual(a.rotZ, b.rotZ) && nearlyEqual(a.pivotX, b.pivotX) && nearlyEqual(a.pivotY, b.pivotY) && nearlyEqual(a.pivotZ, b.pivotZ) && Objects.equals(a.physics, b.physics) && Objects.equals(a.disableSprintLateralTilt, b.disableSprintLateralTilt) && Objects.equals(a.keepArmTuning, b.keepArmTuning) && Objects.equals(a.keepTriggerArmTuning, b.keepTriggerArmTuning) && Objects.equals(a.keepArmTuningL, b.keepArmTuningL) && Objects.equals(a.keepArmTuningR, b.keepArmTuningR);
    }

    private static boolean nearlyEqual(float a, float b) {
        return Math.abs(a - b) <= EPSILON;
    }

    private static Boolean resolveSideKeepArmTuning(Boolean common, Boolean sideSpecific) {
        return sideSpecific != null ? sideSpecific : common;
    }

    private static void pruneEmptyCollections(Data data) {
        if (data.item != null && data.item.isEmpty()) {
            data.item = null;
        }
        if (data.armMesh != null && data.armMesh.isEmpty()) {
            data.armMesh = null;
        }
        if (data.chestProxy != null && data.chestProxy.isEmpty()) {
            data.chestProxy = null;
        }
        if (data.fluidProxy != null && data.fluidProxy.isEmpty()) {
            data.fluidProxy = null;
        }
        if (data.enchantParticleProxy != null && data.enchantParticleProxy.isEmpty()) {
            data.enchantParticleProxy = null;
        }
        if (data.paddleProxy != null && data.paddleProxy.isEmpty()) {
            data.paddleProxy = null;
        }
        if (data.fishingLineProxy != null && data.fishingLineProxy.isEmpty()) {
            data.fishingLineProxy = null;
        }
        if (data.itemSpecific != null && data.itemSpecific.isEmpty()) {
            data.itemSpecific = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isSideSetEqual(SideSet a, SideSet b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && poseEquals(a.right, b.right) && poseEquals(a.left, b.left);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isGroupEntryEqual(GroupEntry a, GroupEntry b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && isSideSetEqual(a.transforms, b.transforms) && isSideSetEqual(a.armTransforms, b.armTransforms) && Objects.equals(a.disableSprintLateralTilt, b.disableSprintLateralTilt) && Objects.equals(a.keepArmTuning, b.keepArmTuning) && Objects.equals(a.keepTriggerArmTuning, b.keepTriggerArmTuning) && Objects.equals(a.keepArmTuningL, b.keepArmTuningL) && Objects.equals(a.keepArmTuningR, b.keepArmTuningR);
    }

    private static boolean isDataEmpty(Data data) {
        if (data == null) {
            return true;
        }
        boolean globalsEmpty = (data.globalItem == null || (data.globalItem.right == null && data.globalItem.left == null)) && (data.globalArm == null || (data.globalArm.right == null && data.globalArm.left == null));
        boolean mapsEmpty = (data.item == null || data.item.isEmpty()) && (data.itemSpecific == null || data.itemSpecific.isEmpty()) && ((data.chestProxy == null || data.chestProxy.isEmpty()) && ((data.fluidProxy == null || data.fluidProxy.isEmpty()) && ((data.enchantParticleProxy == null || data.enchantParticleProxy.isEmpty()) && ((data.paddleProxy == null || data.paddleProxy.isEmpty()) && (data.fishingLineProxy == null || data.fishingLineProxy.isEmpty())))));
        return globalsEmpty && mapsEmpty;
    }

    private static void stripRedundantEntries(Data data) {
        if (data == null || BASELINE_STATE == null) {
            return;
        }
        if (isSideSetEqual(data.globalItem, BASELINE_STATE.globalItem)) {
            data.globalItem = null;
        }
        if (isSideSetEqual(data.globalArm, BASELINE_STATE.globalArm)) {
            data.globalArm = null;
        }
        if (data.item != null) {
            Map<String, GroupEntry> base = BASELINE_STATE.item;
            data.item.entrySet().removeIf(entry -> {
                return isGroupEntryEqual((GroupEntry) entry.getValue(), base != null ? (GroupEntry) base.get(entry.getKey()) : null);
            });
            if (data.item.isEmpty()) {
                data.item = null;
            }
        }
        if (data.armMesh != null) {
            Map<String, SideSet> base2 = BASELINE_STATE.armMesh;
            data.armMesh.entrySet().removeIf(entry2 -> {
                return isSideSetEqual((SideSet) entry2.getValue(), base2 != null ? (SideSet) base2.get(entry2.getKey()) : null);
            });
            if (data.armMesh.isEmpty()) {
                data.armMesh = null;
            }
        }
        if (data.chestProxy != null) {
            Map<String, Pose> base3 = BASELINE_STATE.chestProxy;
            data.chestProxy.entrySet().removeIf(entry3 -> {
                return poseEquals((Pose) entry3.getValue(), base3 != null ? (Pose) base3.get(entry3.getKey()) : null);
            });
            if (data.chestProxy.isEmpty()) {
                data.chestProxy = null;
            }
        }
        if (data.fluidProxy != null) {
            Map<String, Pose> base4 = BASELINE_STATE.fluidProxy;
            data.fluidProxy.entrySet().removeIf(entry4 -> {
                return poseEquals((Pose) entry4.getValue(), base4 != null ? (Pose) base4.get(entry4.getKey()) : null);
            });
            if (data.fluidProxy.isEmpty()) {
                data.fluidProxy = null;
            }
        }
        if (data.enchantParticleProxy != null) {
            Map<String, Pose> base5 = BASELINE_STATE.enchantParticleProxy;
            data.enchantParticleProxy.entrySet().removeIf(entry5 -> {
                return poseEquals((Pose) entry5.getValue(), base5 != null ? (Pose) base5.get(entry5.getKey()) : null);
            });
            if (data.enchantParticleProxy.isEmpty()) {
                data.enchantParticleProxy = null;
            }
        }
        if (data.paddleProxy != null) {
            Map<String, Pose> base6 = BASELINE_STATE.paddleProxy;
            data.paddleProxy.entrySet().removeIf(entry6 -> {
                return poseEquals((Pose) entry6.getValue(), base6 != null ? (Pose) base6.get(entry6.getKey()) : null);
            });
            if (data.paddleProxy.isEmpty()) {
                data.paddleProxy = null;
            }
        }
        if (data.fishingLineProxy != null) {
            Map<String, Pose> base7 = BASELINE_STATE.fishingLineProxy;
            data.fishingLineProxy.entrySet().removeIf(entry7 -> {
                return poseEquals((Pose) entry7.getValue(), base7 != null ? (Pose) base7.get(entry7.getKey()) : null);
            });
            if (data.fishingLineProxy.isEmpty()) {
                data.fishingLineProxy = null;
            }
        }
        if (data.itemSpecific != null) {
            Map<String, SpecificEntry> baseSpecific = BASELINE_STATE.itemSpecific;
            data.itemSpecific.entrySet().removeIf(entry8 -> {
                SpecificEntry base8 = baseSpecific != null ? (SpecificEntry) baseSpecific.get(entry8.getKey()) : null;
                boolean kindMatches = Objects.equals(((SpecificEntry) entry8.getValue()).kind, base8 != null ? base8.kind : null);
                boolean armKindMatches = Objects.equals(((SpecificEntry) entry8.getValue()).armKind, base8 != null ? base8.armKind : null);
                boolean renamedMatches = Objects.equals(((SpecificEntry) entry8.getValue()).renamed, base8 != null ? base8.renamed : null);
                boolean disableSprintTiltMatches = Objects.equals(((SpecificEntry) entry8.getValue()).disableSprintLateralTilt, base8 != null ? base8.disableSprintLateralTilt : null);
                boolean keepArmTuningMatches = Objects.equals(((SpecificEntry) entry8.getValue()).keepArmTuning, base8 != null ? base8.keepArmTuning : null);
                boolean keepTriggerArmTuningMatches = Objects.equals(((SpecificEntry) entry8.getValue()).keepTriggerArmTuning, base8 != null ? base8.keepTriggerArmTuning : null);
                boolean keepArmTuningLeftMatches = Objects.equals(((SpecificEntry) entry8.getValue()).keepArmTuningL, base8 != null ? base8.keepArmTuningL : null);
                boolean keepArmTuningRightMatches = Objects.equals(((SpecificEntry) entry8.getValue()).keepArmTuningR, base8 != null ? base8.keepArmTuningR : null);
                boolean transformsMatch = isSideSetEqual(((SpecificEntry) entry8.getValue()).transforms, base8 != null ? base8.transforms : null);
                boolean armTransformsMatch = isSideSetEqual(((SpecificEntry) entry8.getValue()).armTransforms, base8 != null ? base8.armTransforms : null);
                return kindMatches && armKindMatches && renamedMatches && disableSprintTiltMatches && keepArmTuningMatches && keepTriggerArmTuningMatches && keepArmTuningLeftMatches && keepArmTuningRightMatches && transformsMatch && armTransformsMatch;
            });
            if (data.itemSpecific.isEmpty()) {
                data.itemSpecific = null;
            }
        }
        pruneEmptyCollections(data);
    }

    private static void saveData(Data data) {
        File file = getFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            FileWriter writer = new FileWriter(file);
            try {
                GSON.toJson(data, writer);
                writer.close();
            } finally {
            }
        } catch (Exception e) {
        }
    }

    private static void saveCompatData(String profile, Data data) {
        Path compatDir = getCompatDirectory();
        try {
            Files.createDirectories(compatDir, new FileAttribute[0]);
            Path target = compatDir.resolve(profile + ".json");
            FileWriter writer = new FileWriter(target.toFile());
            try {
                GSON.toJson(data, writer);
                writer.close();
                mirrorCompatResource(profile, target);
            } finally {
            }
        } catch (Exception e) {
        }
    }

    private static void mirrorCompatResource(String profile, Path source) {
        if (!"items_compat".equals(profile) || source == null) {
            return;
        }
        try {
            Path workspace = Path.of("", new String[0]).toAbsolutePath();
            Path srcRoot = workspace.resolve("src");
            if (!Files.exists(srcRoot, new LinkOption[0])) {
                return;
            }
            Path compatDir = srcRoot.resolve("main").resolve("resources").resolve("assets").resolve("minecraft").resolve(Punchy.MOD_ID).resolve("compat");
            Files.createDirectories(compatDir, new FileAttribute[0]);
            Path dest = compatDir.resolve(source.getFileName());
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
        }
    }

    private static void applyCompatOverlays(class_3300 resourceManager) {
        Data baseData = buildBaseDefaults(resourceManager);
        DEFAULTS_RESOURCE_STATE = copyData(baseData);
        if (baseData != null) {
            applyData(copyData(baseData));
        }
        Set<class_2960> compatIds = discoverCompatResourceIds(resourceManager).keySet();
        MIX_COMPAT_OPTION_IDS = resolveMixCompatOptionIds(resourceManager, compatIds);
        MIX_PUNCHY_OPTION_IDS = discoverPunchyOptionIds(resourceManager, compatIds);
        MIX_CATALOG = buildMixCatalog(resourceManager, discoverCompatProfilesFromResourcePacks(resourceManager));
        applyMixSelections();
    }

    private static void reapplyActiveProfilePostCompatOverrides() {
        Data activeOverrides = readActiveProfilePostCompatOverrides();
        if (activeOverrides == null || isDataEmpty(activeOverrides)) {
            return;
        }
        try {
            applyData(activeOverrides, false);
        } catch (Exception e) {
        }
    }

    private static Data readActiveProfilePostCompatOverrides() {
        Path activeProfile = TuningProfileManager.getActiveProfileFilePath();
        if (activeProfile == null || !Files.isRegularFile(activeProfile, new LinkOption[0])) {
            return null;
        }
        try {
            FileReader reader = new FileReader(activeProfile.toFile());
            try {
                Data active = (Data) GSON.fromJson(reader, Data.class);
                reader.close();
                if (active == null) {
                    return null;
                }
                Data overrides = new Data();
                if (hasEmptyHandOverrideData(active.emptyHand)) {
                    overrides.emptyHand = copyEmptyHandEntry(active.emptyHand);
                }
                if (active.item != null) {
                    GroupEntry emptyGroup = active.item.get(VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND.name());
                    if (hasGroupOverrideData(emptyGroup)) {
                        overrides.item = new HashMap();
                        overrides.item.put(VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND.name(), copyGroupEntry(emptyGroup));
                    }
                }
                if (active.itemSpecific != null && !active.itemSpecific.isEmpty()) {
                    for (Map.Entry<String, SpecificEntry> entry : active.itemSpecific.entrySet()) {
                        if (entry != null && entry.getKey() != null && entry.getValue() != null && hasMixPayload(entry.getValue())) {
                            if (overrides.itemSpecific == null) {
                                overrides.itemSpecific = new HashMap();
                            }
                            overrides.itemSpecific.put(entry.getKey(), copySpecificEntry(entry.getValue()));
                        }
                    }
                }
                pruneEmptyCollections(overrides);
                if (isDataEmpty(overrides)) {
                    return null;
                }
                return overrides;
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasEmptyHandOverrideData(EmptyHandEntry entry) {
        return (entry == null || (entry.transforms == null && entry.armTransforms == null && !hasAnimationPayload(entry.customAnimation, entry.customItemAnimations) && ((entry.physics == null || entry.physics.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null && (entry.betterCombat == null || entry.betterCombat.isEmpty())))) ? false : true;
    }

    private static boolean hasGroupOverrideData(GroupEntry entry) {
        return (entry == null || (entry.transforms == null && entry.armTransforms == null && !hasAnimationPayload(entry.customAnimation, entry.customItemAnimations) && ((entry.physics == null || entry.physics.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null && (entry.betterCombat == null || entry.betterCombat.isEmpty())))) ? false : true;
    }

    private static Data buildMergedCompatOverlay(class_3300 resourceManager) {
        Data data;
        List<CompatResourceEntry> entries = collectCompatResourceEntries(resourceManager);
        if (entries.isEmpty()) {
            return null;
        }
        Data merged = null;
        for (CompatResourceEntry entry : entries) {
            if (entry != null && entry.resource != null && (data = readCompatResource(entry.resource)) != null) {
                merged = mergeDataPreferBase(merged, copyData(data));
            }
        }
        return finalizeCompatData("selected", merged);
    }

    private static List<CompatResourceEntry> collectCompatResourceEntries(class_3300 resourceManager) {
        Map<class_2960, String> compatResources = discoverCompatResourceIds(resourceManager);
        if (compatResources.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> packPriorities = resolveCompatPackPriorities(resourceManager, compatResources.keySet());
        List<CompatResourceEntry> entries = new ArrayList<>();
        AtomicInteger nextFallbackPriority = new AtomicInteger(packPriorities.values().stream().mapToInt((v0) -> {
            return v0.intValue();
        }).min().orElse(0) - 1);
        int discoveryOrder = 0;
        for (Map.Entry<class_2960, String> compatEntry : compatResources.entrySet()) {
            class_2960 id = compatEntry.getKey();
            String profile = compatEntry.getValue();
            List<class_3298> stack = tryGetResourceStack(resourceManager, id);
            if ((stack == null || stack.isEmpty()) && resourceManager != null) {
                String searchPath = id.method_12832().contains("/") ? id.method_12832().substring(0, id.method_12832().lastIndexOf(47)) : id.method_12832();
                Map<class_2960, List<class_3298>> stacks = tryListResourceStacks(resourceManager, searchPath);
                if (stacks != null && !stacks.isEmpty()) {
                    stack = stacks.get(id);
                }
            }
            if (stack != null && !stack.isEmpty()) {
                int stackOrder = 0;
                for (int i = stack.size() - 1; i >= 0; i--) {
                    class_3298 resource = stack.get(i);
                    if (resource != null) {
                        int packPriority = getOrAllocatePackPriority(packPriorities, resource.method_14480(), nextFallbackPriority);
                        int i2 = stackOrder;
                        stackOrder++;
                        entries.add(new CompatResourceEntry(id, profile, resource, packPriority, discoveryOrder, i2));
                    }
                }
                discoveryOrder++;
            } else {
                try {
                    Optional<class_3298> resource2 = resourceManager.method_14486(id);
                    if (resource2.isPresent()) {
                        class_3298 resolved = resource2.get();
                        int packPriority2 = getOrAllocatePackPriority(packPriorities, resolved.method_14480(), nextFallbackPriority);
                        entries.add(new CompatResourceEntry(id, profile, resolved, packPriority2, discoveryOrder, 0));
                    }
                } catch (Exception e) {
                }
                discoveryOrder++;
            }
        }
        entries.sort(Comparator.comparingInt(entry -> {
            return entry.packPriority;
        }).reversed().thenComparingInt(entry2 -> {
            return entry2.discoveryOrder;
        }).thenComparingInt(entry3 -> {
            return entry3.stackOrder;
        }).thenComparing(entry4 -> {
            return entry4.id.toString();
        }));
        if (!entries.isEmpty()) {
            StringJoiner joiner = new StringJoiner(" | ");
            for (CompatResourceEntry entry5 : entries) {
                if (entry5 != null && entry5.resource != null) {
                    joiner.add(entry5.resource.method_14480() + "#" + entry5.id.method_12832() + "(p=" + entry5.packPriority + ",d=" + entry5.discoveryOrder + ",s=" + entry5.stackOrder + ")");
                }
            }
        }
        return entries;
    }

    private static int getOrAllocatePackPriority(Map<String, Integer> packPriorities, String packId, AtomicInteger nextFallbackPriority) {
        if (packId == null || packId.isBlank()) {
            return nextFallbackPriority.getAndDecrement();
        }
        Integer existing = packPriorities.get(packId);
        if (existing != null) {
            return existing.intValue();
        }
        int allocated = nextFallbackPriority.getAndDecrement();
        packPriorities.put(packId, Integer.valueOf(allocated));
        return allocated;
    }

    private static Map<class_2960, String> discoverCompatResourceIds(class_3300 resourceManager) {
        Map<class_2960, String> result = new LinkedHashMap<>();
        if (resourceManager == null) {
            return result;
        }
        collectCompatResourceIds(resourceManager, "punchy/compat", "minecraft", "punchy/compat/", result);
        collectCompatResourceIds(resourceManager, "compat", Punchy.MOD_ID, "compat/", result);
        return result;
    }

    private static void collectCompatResourceIds(class_3300 manager, String searchPath, String namespace, String prefix, Map<class_2960, String> result) {
        Map<class_2960, class_3298> found = manager.method_14488(searchPath, loc -> {
            return loc.method_12836().equals(namespace) && loc.method_12832().startsWith(prefix) && loc.method_12832().endsWith(".json");
        });
        for (class_2960 id : found.keySet()) {
            String path = id.method_12832();
            String file = path.substring(prefix.length());
            if (file.endsWith(".json")) {
                file = file.substring(0, file.length() - 5);
            }
            if (!file.isBlank()) {
                result.putIfAbsent(id, file.toLowerCase(Locale.ROOT));
            }
        }
    }

    private static Map<String, Integer> resolveCompatPackPriorities(class_3300 resourceManager, Set<class_2960> compatIds) {
        Map<String, Integer> priorities = new LinkedHashMap<>();
        List<String> selected = getSelectedResourcePackIdsHighToLow();
        int weight = selected.size();
        for (String packId : selected) {
            if (packId != null && !packId.isBlank()) {
                int i = weight;
                weight--;
                priorities.putIfAbsent(packId, Integer.valueOf(i));
            }
        }
        return priorities;
    }

    private static Set<String> resolveMixCompatOptionIds(class_3300 resourceManager, Set<class_2960> compatIds) {
        String optionId;
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (CompatResourceEntry entry : collectCompatResourceEntries(resourceManager)) {
            if (entry != null && entry.resource != null && (optionId = normalizeMixOption(entry.resource.method_14480())) != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                result.add(optionId);
            }
        }
        return result;
    }

    private static Set<String> discoverPunchyOptionIds(class_3300 resourceManager, Set<class_2960> compatIds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String packId : getSelectedResourcePackIdsHighToLow()) {
            String optionId = normalizeMixOption(packId);
            if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                result.add(optionId);
            }
        }
        result.addAll(resolveMixCompatOptionIds(resourceManager, compatIds));
        return result;
    }

    private static List<String> getSelectedResourcePackIds() {
        List<String> highToLow = getSelectedResourcePackIdsHighToLow();
        if (highToLow.isEmpty()) {
            return List.of();
        }
        List<String> lowToHigh = new ArrayList<>(highToLow);
        Collections.reverse(lowToHigh);
        return lowToHigh;
    }

    private static List<String> getSelectedResourcePackIdsHighToLow() {
        class_310 client = class_310.method_1551();
        if (client == null) {
            return List.of();
        }
        class_3300 resourceManager = client.method_1478();
        if (cachedSelectedResourcePackIdsHighToLow != null && cachedSelectedResourcePackResourceManager == resourceManager) {
            recordSelectedPackLookup(false);
            return cachedSelectedResourcePackIdsHighToLow;
        }
        recordSelectedPackLookup(true);
        List<String> result = new ArrayList<>();
        try {
            class_3283 repository = client.method_1520();
            if (repository != null) {
                for (String packId : repository.method_29210()) {
                    if (packId != null && !packId.isBlank()) {
                        result.add(packId);
                    }
                }
            }
        } catch (Exception e) {
        }
        Collections.reverse(result);
        String builtinPackId = resolveActiveBuiltInPackId(client);
        if (builtinPackId != null && !builtinPackId.isBlank() && !result.contains(builtinPackId)) {
            result.add(0, builtinPackId);
        }
        cachedSelectedResourcePackResourceManager = resourceManager;
        cachedSelectedResourcePackIdsHighToLow = List.copyOf(result);
        return cachedSelectedResourcePackIdsHighToLow;
    }

    private static Set<String> getActiveMixPackOptionIds() {
        LinkedHashSet<String> active = new LinkedHashSet<>();
        for (String packId : getSelectedResourcePackIdsHighToLow()) {
            String optionId = normalizeMixOption(packId);
            if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                active.add(optionId);
            }
        }
        return active;
    }

    private static String resolveActiveBuiltInPackId(class_310 client) {
        if (client == null || client.method_1478() == null) {
            return null;
        }
        class_3300 resourceManager = client.method_1478();
        if (cachedActiveBuiltInPackResourceManager == resourceManager) {
            return cachedActiveBuiltInPackId;
        }
        builtInPackLookupCount++;
        try {
            class_2960 id = class_2960.method_60655("minecraft", "punchy/compat/default_tuning.json");
            cachedActiveBuiltInPackResourceManager = resourceManager;
            cachedActiveBuiltInPackId = (String) resourceManager.method_14486(id).map((v0) -> {
                return v0.method_14480();
            }).filter(packId -> {
                return (packId == null || packId.isBlank()) ? false : true;
            }).orElse(null);
            return cachedActiveBuiltInPackId;
        } catch (Exception e) {
            cachedActiveBuiltInPackResourceManager = resourceManager;
            cachedActiveBuiltInPackId = null;
            return null;
        }
    }

    private static void invalidateSelectedResourcePackCache() {
        if (cachedSelectedResourcePackIdsHighToLow != null || cachedActiveBuiltInPackResourceManager != null) {
        }
        cachedSelectedResourcePackIdsHighToLow = null;
        cachedSelectedResourcePackResourceManager = null;
        cachedActiveBuiltInPackId = null;
        cachedActiveBuiltInPackResourceManager = null;
    }

    private static void recordSelectedPackLookup(boolean cacheMiss) {
        long now = System.nanoTime();
        if (selectedPackLookupWindowStartedNanos == 0) {
            selectedPackLookupWindowStartedNanos = now;
        }
        selectedPackLookupCount++;
        if (cacheMiss) {
            selectedPackCacheMissCount++;
        }
        if (now - selectedPackLookupWindowStartedNanos < 1000000000) {
            return;
        }
        if (selectedPackLookupCount > 120 || selectedPackCacheMissCount > 1 || builtInPackLookupCount > 0) {
        }
        selectedPackLookupWindowStartedNanos = now;
        selectedPackLookupCount = 0;
        selectedPackCacheMissCount = 0;
        builtInPackLookupCount = 0;
    }

    private static String describeSelectedPackLookupCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (!className.equals(PunchyTuningConfig.class.getName()) && !className.equals(Thread.class.getName())) {
                return element.toString();
            }
        }
        return "unknown";
    }

    private static Set<String> discoverCompatProfilesFromResourcePacks(class_3300 resourceManager) {
        return new LinkedHashSet(discoverCompatResourceIds(resourceManager).values());
    }

    private static MixCatalog buildMixCatalog(class_3300 resourceManager, Set<String> profiles) {
        MixCatalog catalog = new MixCatalog();
        if (DEFAULTS_RESOURCE_STATE != null) {
            collectMixSlotsFromData(catalog, DEFAULTS_RESOURCE_STATE, "punchy_default");
            collectMixSlotsFromData(catalog, copyData(DEFAULTS_RESOURCE_STATE), BUILTIN_PACK_OPTION_ID);
        }
        if (profiles != null) {
            for (String profile : profiles) {
                if (profile != null && !profile.isBlank()) {
                    collectMixCatalogProfile(resourceManager, catalog, profile, "minecraft", "punchy/compat/" + profile + ".json");
                    collectMixCatalogProfile(resourceManager, catalog, profile, Punchy.MOD_ID, "compat/" + profile + ".json");
                }
            }
        }
        finalizeMixCatalog(resourceManager, catalog);
        return catalog;
    }

    private static void collectMixCatalogProfile(class_3300 resourceManager, MixCatalog catalog, String profile, String namespace, String path) {
        Data data;
        Data data2;
        class_2960 id = class_2960.method_60655(namespace, path);
        List<class_3298> stack = tryGetResourceStack(resourceManager, id);
        if ((stack == null || stack.isEmpty()) && resourceManager != null) {
            String searchPath = path.contains("/") ? path.substring(0, path.lastIndexOf(47)) : path;
            Map<class_2960, List<class_3298>> stacks = tryListResourceStacks(resourceManager, searchPath);
            if (stacks != null && !stacks.isEmpty()) {
                stack = stacks.get(id);
            }
        }
        if (stack == null || stack.isEmpty()) {
            try {
                Optional<class_3298> resource = resourceManager.method_14486(id);
                if (resource.isPresent() && (data = readCompatResource(resource.get())) != null) {
                    String optionId = normalizeMixOption(resource.get().method_14480());
                    collectMixSlotsFromData(catalog, data, optionId);
                    return;
                }
                return;
            } catch (Exception e) {
                return;
            }
        }
        for (int i = 0; i < stack.size(); i++) {
            class_3298 resource2 = stack.get(i);
            if (resource2 != null && (data2 = readCompatResource(resource2)) != null) {
                String optionId2 = normalizeMixOption(resource2.method_14480());
                collectMixSlotsFromData(catalog, copyData(data2), optionId2);
            }
        }
    }

    private static void collectMixSlotsFromData(MixCatalog catalog, Data data, String optionId) {
        class_2960 itemId;
        VanillaFirstPersonItemLayer.ToolKind kind;
        if (catalog == null || data == null || optionId == null || optionId.isBlank()) {
            return;
        }
        if (data.item != null) {
            for (Map.Entry<String, GroupEntry> entry : data.item.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                    GroupEntry group = entry.getValue();
                    if (hasAnimationMixPayload(group) && (kind = parseToolKind(entry.getKey())) != null) {
                        String slotId = "kind:" + kind.name().toLowerCase(Locale.ROOT);
                        MixSlotDef slot = catalog.slots.computeIfAbsent(slotId, key -> {
                            MixSlotDef def = new MixSlotDef();
                            def.slotId = key;
                            def.type = MixSlotType.KIND;
                            def.kind = kind;
                            def.label = prettySlotLabel(kind.name());
                            return def;
                        });
                        if (!"punchy_default".equals(optionId)) {
                            registerMixSlotOption(slot, optionId);
                            registerMixSlotPayload(slot, optionId, payloadFromGroup(group, kind, optionId));
                        }
                    }
                }
            }
        }
        if (data.emptyHand != null && hasAnimationMixPayload(data.emptyHand)) {
            VanillaFirstPersonItemLayer.ToolKind kind2 = VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND;
            String slotId2 = "kind:" + kind2.name().toLowerCase(Locale.ROOT);
            MixSlotDef slot2 = catalog.slots.computeIfAbsent(slotId2, key2 -> {
                MixSlotDef def = new MixSlotDef();
                def.slotId = key2;
                def.type = MixSlotType.KIND;
                def.kind = kind2;
                def.label = prettySlotLabel(kind2.name());
                return def;
            });
            if (!"punchy_default".equals(optionId)) {
                registerMixSlotOption(slot2, optionId);
                registerMixSlotPayload(slot2, optionId, payloadFromEmptyHand(data.emptyHand, kind2, optionId));
            }
        }
        if (data.itemSpecific != null) {
            for (Map.Entry<String, SpecificEntry> entry2 : data.itemSpecific.entrySet()) {
                if (entry2 != null && entry2.getKey() != null && entry2.getValue() != null) {
                    SpecificEntry specific = entry2.getValue();
                    if (hasAnimationMixPayload(specific) && (itemId = resolveSpecificItemId(entry2.getKey())) != null && !isMixSpecificHidden(itemId)) {
                        VanillaFirstPersonItemLayer.ToolKind kind3 = parseToolKind(specific.kind);
                        if (kind3 == null) {
                            kind3 = resolveKindForItem(itemId);
                        }
                        if (kind3 != null) {
                            VanillaFirstPersonItemLayer.ToolKind resolvedKind = kind3;
                            String kindSlotId = "kind:" + kind3.name().toLowerCase(Locale.ROOT);
                            MixSlotDef slot3 = catalog.slots.computeIfAbsent(kindSlotId, key3 -> {
                                MixSlotDef def = new MixSlotDef();
                                def.slotId = key3;
                                def.type = MixSlotType.KIND;
                                def.kind = resolvedKind;
                                def.label = prettySlotLabel(resolvedKind.name());
                                return def;
                            });
                            slot3.mergedSpecificItems.add(itemId);
                            if (!"punchy_default".equals(optionId)) {
                                registerMixSlotOption(slot3, optionId);
                                registerMixSlotSpecificPayload(slot3, optionId, itemId, payloadFromSpecific(specific, resolvedKind, optionId));
                            }
                        } else {
                            String slotId3 = "specific:" + String.valueOf(itemId);
                            MixSlotDef slot4 = catalog.slots.computeIfAbsent(slotId3, key4 -> {
                                MixSlotDef def = new MixSlotDef();
                                def.slotId = key4;
                                def.type = MixSlotType.SPECIFIC;
                                def.itemId = itemId;
                                def.label = "Item Specific: " + String.valueOf(itemId);
                                return def;
                            });
                            if (!"punchy_default".equals(optionId)) {
                                registerMixSlotOption(slot4, optionId);
                                registerMixSlotPayload(slot4, optionId, payloadFromSpecific(specific, null, optionId));
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isMixSpecificHidden(class_2960 itemId) {
        if (itemId == null) {
            return false;
        }
        return MIX_HIDDEN_SPECIFIC_IDS.contains(itemId.toString().toLowerCase(Locale.ROOT));
    }

    private static void registerMixSlotOption(MixSlotDef slot, String optionId) {
        if (slot != null && optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId) && !slot.optionOrder.contains(optionId)) {
            slot.optionOrder.add(optionId);
        }
    }

    private static void finalizeMixCatalog(class_3300 resourceManager, MixCatalog catalog) {
        if (catalog == null || catalog.slots.isEmpty()) {
            return;
        }
        MixCatalogOrdering ordering = resolveMixCatalogOrdering(resourceManager);
        registerAnimationOnlyOptions(catalog, ordering, resourceManager);
        for (MixSlotDef slot : catalog.slots.values()) {
            if (slot != null && !slot.optionOrder.isEmpty()) {
                List<String> sortedOptions = new ArrayList<>(slot.optionOrder);
                sortedOptions.sort((left, right) -> {
                    int leftPriority = ordering.optionPriorities.getOrDefault(left, Integer.MIN_VALUE).intValue();
                    int rightPriority = ordering.optionPriorities.getOrDefault(right, Integer.MIN_VALUE).intValue();
                    if (leftPriority != rightPriority) {
                        return Integer.compare(rightPriority, leftPriority);
                    }
                    return left.compareTo(right);
                });
                slot.optionOrder.clear();
                slot.optionOrder.addAll(sortedOptions);
                slot.resourceOrderOption = resolveResourceOrderOption(sortedOptions, ordering);
            }
        }
    }

    private static String resolveResourceOrderOption(List<String> sortedOptions, MixCatalogOrdering ordering) {
        if (sortedOptions == null || sortedOptions.isEmpty()) {
            return null;
        }
        if (ordering != null && !ordering.selectedOptionIdsByPriority.isEmpty()) {
            for (String optionId : ordering.selectedOptionIdsByPriority) {
                if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId) && sortedOptions.contains(optionId)) {
                    return optionId;
                }
            }
        }
        return (String) sortedOptions.getFirst();
    }

    private static MixCatalogOrdering resolveMixCatalogOrdering(class_3300 resourceManager) {
        MixCatalogOrdering ordering = new MixCatalogOrdering();
        List<String> selected = getSelectedResourcePackIdsHighToLow();
        int priority = selected.size();
        for (String packId : selected) {
            String optionId = normalizeMixOption(packId);
            if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                ordering.selectedOptionIdsByPriority.add(optionId);
                int i = priority;
                priority--;
                ordering.optionPriorities.putIfAbsent(optionId, Integer.valueOf(i));
            }
        }
        return ordering;
    }

    private static void registerAnimationOnlyOptions(MixCatalog catalog, MixCatalogOrdering ordering, class_3300 resourceManager) {
        if (catalog == null || ordering == null || resourceManager == null) {
            return;
        }
        Map<String, LinkedHashMap<String, AnimationClip>> variants = PunchyAnimationLoader.loadAllWithPackVariants(resourceManager);
        if (variants.isEmpty()) {
            return;
        }
        for (MixSlotDef slot : catalog.slots.values()) {
            if (slot != null) {
                Set<String> clipNames = collectReferencedClipNamesForSlot(slot);
                if (!clipNames.isEmpty()) {
                    for (String clipName : clipNames) {
                        LinkedHashMap<String, AnimationClip> clipVariants = variants.get(clipName);
                        if (clipVariants != null && !clipVariants.isEmpty()) {
                            for (String optionId : clipVariants.keySet()) {
                                String normalizedOption = normalizeMixOption(optionId);
                                if (normalizedOption != null && !normalizedOption.isBlank()) {
                                    registerMixSlotClipVariant(slot, normalizedOption, clipName);
                                    if (!"punchy_default".equals(normalizedOption) && (MIX_PUNCHY_OPTION_IDS.isEmpty() || MIX_PUNCHY_OPTION_IDS.contains(normalizedOption))) {
                                        registerMixSlotOption(slot, normalizedOption);
                                        ordering.optionPriorities.putIfAbsent(normalizedOption, Integer.valueOf(ordering.optionPriorities.size()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void registerMixSlotClipVariant(MixSlotDef slot, String optionId, String clipName) {
        if (slot == null || optionId == null || optionId.isBlank() || clipName == null) {
            return;
        }
        String normalizedClip = clipName.trim().toLowerCase(Locale.ROOT);
        if (normalizedClip.isBlank()) {
            return;
        }
        LinkedHashSet<String> names = slot.optionClipNames.computeIfAbsent(optionId, ignored -> {
            return new LinkedHashSet();
        });
        names.add(normalizedClip);
    }

    private static Set<String> collectReferencedClipNamesForSlot(MixSlotDef slot) {
        if (slot == null) {
            return Set.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        collectPayloadClipNames(names, baselinePayloadForSlot(slot));
        for (MixSlotPayload payload : slot.optionPayloads.values()) {
            collectPayloadClipNames(names, payload);
        }
        if (slot.optionSpecificPayloads != null && !slot.optionSpecificPayloads.isEmpty()) {
            for (Map<class_2960, MixSlotPayload> specificMap : slot.optionSpecificPayloads.values()) {
                if (specificMap != null && !specificMap.isEmpty()) {
                    for (MixSlotPayload payload2 : specificMap.values()) {
                        collectPayloadClipNames(names, payload2);
                    }
                }
            }
        }
        collectPassiveDefaultClipNamesForSlot(slot, names);
        return names;
    }

    private static void collectPassiveDefaultClipNamesForSlot(MixSlotDef slot, Set<String> names) {
        if (slot == null || names == null) {
            return;
        }
        names.addAll(GENERIC_PASSIVE_CLIP_NAMES);
        if (slot.kind == VanillaFirstPersonItemLayer.ToolKind.SPEAR) {
            names.addAll(SPEAR_MIX_CLIP_NAMES);
        } else if (slot.kind == VanillaFirstPersonItemLayer.ToolKind.SWORD) {
            names.addAll(SWORD_PASSIVE_CLIP_NAMES);
        }
    }

    private static void collectPayloadClipNames(Set<String> out, MixSlotPayload payload) {
        if (out == null || payload == null) {
            return;
        }
        collectEntryClipNames(out, payloadCustomAnimation(payload));
        collectEntryClipNames(out, payloadCustomItemAnimations(payload));
    }

    private static void collectEntryClipNames(Set<String> out, List<CustomAnimationEntry> entries) {
        if (out == null || entries == null || entries.isEmpty()) {
            return;
        }
        for (CustomAnimationEntry entry : entries) {
            if (entry != null) {
                addClipName(out, entry.name);
                addClipName(out, entry.animation);
                addClipName(out, entry.start);
                addClipName(out, entry.looping);
                addClipName(out, entry.end);
                addClipName(out, entry.keep_looping);
                addClipName(out, entry.stage_1);
                addClipName(out, entry.stage_2);
                addClipName(out, entry.stage_3);
                addClipName(out, entry.var_1);
                addClipName(out, entry.var_2);
                addClipName(out, entry.var_3);
                addClipName(out, entry.critical_hit);
            }
        }
    }

    private static void addClipName(Set<String> out, String clipName) {
        if (out == null || clipName == null) {
            return;
        }
        String normalized = clipName.trim();
        if (!normalized.isBlank()) {
            out.add(normalized);
        }
    }

    private static void registerMixSlotPayload(MixSlotDef slot, String optionId, MixSlotPayload payload) {
        if (slot == null || optionId == null || optionId.isBlank() || payload == null) {
            return;
        }
        MixSlotPayload existing = slot.optionPayloads.get(optionId);
        slot.optionPayloads.put(optionId, mergeMixSlotPayload(existing, payload));
    }

    private static void registerMixSlotSpecificPayload(MixSlotDef slot, String optionId, class_2960 itemId, MixSlotPayload payload) {
        if (slot == null || optionId == null || optionId.isBlank() || itemId == null || payload == null) {
            return;
        }
        LinkedHashMap<class_2960, MixSlotPayload> items = slot.optionSpecificPayloads.computeIfAbsent(optionId, key -> {
            return new LinkedHashMap();
        });
        MixSlotPayload existing = items.get(itemId);
        items.put(itemId, mergeMixSlotPayload(existing, payload));
    }

    private static MixSlotPayload mergeMixSlotPayload(MixSlotPayload base, MixSlotPayload overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            MixSlotPayload copy = new MixSlotPayload();
            copy.kind = overlay.kind;
            copy.groupEntry = copyGroupEntry(overlay.groupEntry);
            copy.emptyHandEntry = copyEmptyHandEntry(overlay.emptyHandEntry);
            copy.specificEntry = copySpecificEntry(overlay.specificEntry);
            return copy;
        }
        MixSlotPayload merged = new MixSlotPayload();
        merged.kind = overlay.kind != null ? overlay.kind : base.kind;
        merged.groupEntry = mergeGroupEntry(copyGroupEntry(base.groupEntry), copyGroupEntry(overlay.groupEntry));
        merged.emptyHandEntry = mergeEmptyHand(copyEmptyHandEntry(base.emptyHandEntry), copyEmptyHandEntry(overlay.emptyHandEntry));
        merged.specificEntry = mergeSpecificEntry(copySpecificEntry(base.specificEntry), copySpecificEntry(overlay.specificEntry));
        return merged;
    }

    private static boolean hasAnimationPayload(List<CustomAnimationEntry> customAnimation, List<CustomAnimationEntry> customItemAnimations) {
        return ((customAnimation == null || customAnimation.isEmpty()) && (customItemAnimations == null || customItemAnimations.isEmpty())) ? false : true;
    }

    private static boolean hasMixPayload(GroupEntry entry) {
        return (entry == null || (!hasAnimationPayload(entry.customAnimation, entry.customItemAnimations) && entry.transforms == null && entry.armTransforms == null && ((entry.physics == null || entry.physics.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null && readCompatParticleEffects(entry) == null && (entry.betterCombat == null || entry.betterCombat.isEmpty())))) ? false : true;
    }

    private static boolean hasAnimationMixPayload(GroupEntry entry) {
        return entry != null && hasAnimationPayload(entry.customAnimation, entry.customItemAnimations);
    }

    private static boolean hasMixPayload(EmptyHandEntry entry) {
        return (entry == null || (!hasAnimationPayload(entry.customAnimation, entry.customItemAnimations) && entry.transforms == null && entry.armTransforms == null && ((entry.physics == null || entry.physics.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null && (entry.betterCombat == null || entry.betterCombat.isEmpty())))) ? false : true;
    }

    private static boolean hasAnimationMixPayload(EmptyHandEntry entry) {
        return entry != null && hasAnimationPayload(entry.customAnimation, entry.customItemAnimations);
    }

    private static boolean hasMixPayload(SpecificEntry entry) {
        return (entry == null || (!hasAnimationPayload(entry.customAnimation, entry.customItemAnimations) && entry.kind == null && entry.armKind == null && entry.renamed == null && entry.model == null && entry.customModelData == null && ((entry.physics == null || entry.physics.isJsonNull()) && entry.disableSprintLateralTilt == null && entry.keepArmTuning == null && entry.keepTriggerArmTuning == null && entry.keepArmTuningL == null && entry.keepArmTuningR == null && ((entry.animation == null || entry.animation.isEmpty()) && entry.transforms == null && entry.armTransforms == null && readCompatParticleEffects(entry) == null && (entry.betterCombat == null || entry.betterCombat.isEmpty()))))) ? false : true;
    }

    private static boolean hasAnimationMixPayload(SpecificEntry entry) {
        return hasMixPayload(entry);
    }

    private static List<CustomAnimationEntry> payloadCustomAnimation(MixSlotPayload payload) {
        if (payload == null) {
            return null;
        }
        if (payload.groupEntry != null) {
            return payload.groupEntry.customAnimation;
        }
        if (payload.emptyHandEntry != null) {
            return payload.emptyHandEntry.customAnimation;
        }
        if (payload.specificEntry != null) {
            return payload.specificEntry.customAnimation;
        }
        return null;
    }

    private static List<CustomAnimationEntry> payloadCustomItemAnimations(MixSlotPayload payload) {
        if (payload == null) {
            return null;
        }
        if (payload.groupEntry != null) {
            return payload.groupEntry.customItemAnimations;
        }
        if (payload.emptyHandEntry != null) {
            return payload.emptyHandEntry.customItemAnimations;
        }
        if (payload.specificEntry != null) {
            return payload.specificEntry.customItemAnimations;
        }
        return null;
    }

    private static void setPayloadCustomAnimation(MixSlotPayload payload, List<CustomAnimationEntry> entries) {
        if (payload == null) {
            return;
        }
        if (payload.groupEntry != null) {
            payload.groupEntry.customAnimation = entries;
        } else if (payload.emptyHandEntry != null) {
            payload.emptyHandEntry.customAnimation = entries;
        } else if (payload.specificEntry != null) {
            payload.specificEntry.customAnimation = entries;
        }
    }

    private static void setPayloadCustomItemAnimations(MixSlotPayload payload, List<CustomAnimationEntry> entries) {
        if (payload == null) {
            return;
        }
        if (payload.groupEntry != null) {
            payload.groupEntry.customItemAnimations = entries;
        } else if (payload.emptyHandEntry != null) {
            payload.emptyHandEntry.customItemAnimations = entries;
        } else if (payload.specificEntry != null) {
            payload.specificEntry.customItemAnimations = entries;
        }
    }

    private static MixSlotPayload payloadFromGroup(GroupEntry entry, VanillaFirstPersonItemLayer.ToolKind kind, String mixPackOption) {
        MixSlotPayload payload = new MixSlotPayload();
        payload.kind = kind;
        payload.groupEntry = copyGroupEntry(entry);
        if (payload.groupEntry != null) {
            payload.groupEntry.customAnimation = copyCustomAnimationList(entry.customAnimation, mixPackOption);
            payload.groupEntry.customItemAnimations = copyCustomAnimationList(entry.customItemAnimations, mixPackOption);
        }
        return payload;
    }

    private static MixSlotPayload payloadFromEmptyHand(EmptyHandEntry entry, VanillaFirstPersonItemLayer.ToolKind kind, String mixPackOption) {
        MixSlotPayload payload = new MixSlotPayload();
        payload.kind = kind;
        payload.emptyHandEntry = copyEmptyHandEntry(entry);
        if (payload.emptyHandEntry != null) {
            payload.emptyHandEntry.customAnimation = copyCustomAnimationList(entry.customAnimation, mixPackOption);
            payload.emptyHandEntry.customItemAnimations = copyCustomAnimationList(entry.customItemAnimations, mixPackOption);
        }
        return payload;
    }

    private static MixSlotPayload payloadFromSpecific(SpecificEntry entry, VanillaFirstPersonItemLayer.ToolKind kind, String mixPackOption) {
        MixSlotPayload payload = new MixSlotPayload();
        payload.kind = kind;
        payload.specificEntry = copySpecificEntry(entry);
        if (payload.specificEntry != null) {
            payload.specificEntry.customAnimation = copyCustomAnimationList(entry.customAnimation, mixPackOption);
            payload.specificEntry.customItemAnimations = copyCustomAnimationList(entry.customItemAnimations, mixPackOption);
        }
        return payload;
    }

    private static List<CustomAnimationEntry> copyCustomAnimationList(List<CustomAnimationEntry> input, String mixPackOption) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        List<CustomAnimationEntry> copy = new ArrayList<>(input.size());
        String normalizedOption = normalizeMixOption(mixPackOption);
        for (CustomAnimationEntry entry : input) {
            if (entry != null) {
                CustomAnimationEntry cloned = (CustomAnimationEntry) GSON.fromJson(GSON.toJsonTree(entry), CustomAnimationEntry.class);
                cloned.mix_pack_option = normalizedOption;
                copy.add(cloned);
            }
        }
        if (copy.isEmpty()) {
            return null;
        }
        return copy;
    }

    private static String summarizeEntryTypes(List<CustomAnimationEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        LinkedHashSet<String> types = new LinkedHashSet<>();
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            String type = normalizeEntryType(entry == null ? null : entry.type);
            if (type != null) {
                types.add(type);
            }
        }
        return types.toString();
    }

    private static String summarizeSideSet(SideSet set) {
        if (set == null) {
            return "none";
        }
        return "R=" + (set.right != null) + ",L=" + (set.left != null);
    }

    private static String summarizeGroupEntry(GroupEntry entry) {
        if (entry == null) {
            return "null";
        }
        List<String> particles = readCompatParticleEffects(entry);
        return "transforms{" + summarizeSideSet(entry.transforms) + "} arm{" + summarizeSideSet(entry.armTransforms) + "} physics=" + ((entry.physics == null || entry.physics.isJsonNull()) ? false : true) + " anim=" + (entry.customAnimation == null ? 0 : entry.customAnimation.size()) + summarizeEntryTypes(entry.customAnimation) + " itemAnim=" + (entry.customItemAnimations == null ? 0 : entry.customItemAnimations.size()) + summarizeEntryTypes(entry.customItemAnimations) + " particles=" + (particles == null ? 0 : particles.size()) + " betterCombat=" + (entry.betterCombat == null ? 0 : entry.betterCombat.size()) + " flags={disable=" + entry.disableSprintLateralTilt + ",keep=" + entry.keepArmTuning + ",keepMain=" + entry.keepTriggerArmTuning + ",keepL=" + entry.keepArmTuningL + ",keepR=" + entry.keepArmTuningR + "}";
    }

    private static String summarizeEmptyHandEntry(EmptyHandEntry entry) {
        if (entry == null) {
            return "null";
        }
        return "transforms{" + summarizeSideSet(entry.transforms) + "} arm{" + summarizeSideSet(entry.armTransforms) + "} physics=" + ((entry.physics == null || entry.physics.isJsonNull()) ? false : true) + " anim=" + (entry.customAnimation == null ? 0 : entry.customAnimation.size()) + summarizeEntryTypes(entry.customAnimation) + " itemAnim=" + (entry.customItemAnimations == null ? 0 : entry.customItemAnimations.size()) + summarizeEntryTypes(entry.customItemAnimations) + " betterCombat=" + (entry.betterCombat == null ? 0 : entry.betterCombat.size()) + " flags={disable=" + entry.disableSprintLateralTilt + ",keep=" + entry.keepArmTuning + ",keepMain=" + entry.keepTriggerArmTuning + ",keepL=" + entry.keepArmTuningL + ",keepR=" + entry.keepArmTuningR + "}";
    }

    private static String summarizeSpecificEntry(SpecificEntry entry) {
        if (entry == null) {
            return "null";
        }
        List<String> particles = readCompatParticleEffects(entry);
        return "kind=" + entry.kind + " armKind=" + entry.armKind + " renamed=" + entry.renamed + " model=" + entry.model + " cmd=" + entry.customModelData + " transforms{" + summarizeSideSet(entry.transforms) + "} arm{" + summarizeSideSet(entry.armTransforms) + "} physics=" + ((entry.physics == null || entry.physics.isJsonNull()) ? false : true) + " anim=" + (entry.customAnimation == null ? 0 : entry.customAnimation.size()) + summarizeEntryTypes(entry.customAnimation) + " itemAnim=" + (entry.customItemAnimations == null ? 0 : entry.customItemAnimations.size()) + summarizeEntryTypes(entry.customItemAnimations) + " particles=" + (particles == null ? 0 : particles.size()) + " betterCombat=" + (entry.betterCombat == null ? 0 : entry.betterCombat.size()) + " flags={disable=" + entry.disableSprintLateralTilt + ",keep=" + entry.keepArmTuning + ",keepMain=" + entry.keepTriggerArmTuning + ",keepL=" + entry.keepArmTuningL + ",keepR=" + entry.keepArmTuningR + "}";
    }

    private static String summarizePayload(MixSlotPayload payload) {
        if (payload == null) {
            return "null";
        }
        if (payload.groupEntry != null) {
            return "kind=" + String.valueOf(payload.kind) + " group{" + summarizeGroupEntry(payload.groupEntry) + "}";
        }
        if (payload.emptyHandEntry != null) {
            return "kind=" + String.valueOf(payload.kind) + " empty{" + summarizeEmptyHandEntry(payload.emptyHandEntry) + "}";
        }
        if (payload.specificEntry != null) {
            return "kind=" + String.valueOf(payload.kind) + " specific{" + summarizeSpecificEntry(payload.specificEntry) + "}";
        }
        return "kind=" + String.valueOf(payload.kind) + " emptyPayload=true";
    }

    private static String summarizeData(Data data) {
        if (data == null) {
            return "null";
        }
        StringJoiner joiner = new StringJoiner("; ");
        if (data.emptyHand != null) {
            joiner.add("emptyHand{" + summarizeEmptyHandEntry(data.emptyHand) + "}");
        }
        if (data.item != null && !data.item.isEmpty()) {
            int count = 0;
            StringJoiner items = new StringJoiner(", ", "items[", "]");
            for (Map.Entry<String, GroupEntry> entry : data.item.entrySet()) {
                if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                    items.add(entry.getKey() + "{" + summarizeGroupEntry(entry.getValue()) + "}");
                    count++;
                    if (count >= 6) {
                        break;
                    }
                }
            }
            joiner.add(items.toString());
        }
        if (data.itemSpecific != null && !data.itemSpecific.isEmpty()) {
            int count2 = 0;
            StringJoiner specifics = new StringJoiner(", ", "specific[", "]");
            for (Map.Entry<String, SpecificEntry> entry2 : data.itemSpecific.entrySet()) {
                if (entry2 != null && entry2.getKey() != null && entry2.getValue() != null) {
                    specifics.add(entry2.getKey() + "{" + summarizeSpecificEntry(entry2.getValue()) + "}");
                    count2++;
                    if (count2 >= 6) {
                        break;
                    }
                }
            }
            joiner.add(specifics.toString());
        }
        if (joiner.length() == 0) {
            return "empty";
        }
        return joiner.toString();
    }

    private static String prettySlotLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    builder.append(part.substring(1));
                }
            }
        }
        return builder.toString();
    }

    private static VanillaFirstPersonItemLayer.ToolKind parseToolKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return VanillaFirstPersonItemLayer.ToolKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static VanillaFirstPersonItemLayer.ToolKind resolveKindForItem(class_2960 itemId) {
        class_1792 item;
        if (itemId == null || (item = (class_1792) class_7923.field_41178.method_17966(itemId).orElse(null)) == null) {
            return null;
        }
        return resolveMixKindForStack(new class_1799(item));
    }

    private static VanillaFirstPersonItemLayer.ToolKind resolveMixKindForStack(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return VanillaFirstPersonItemLayer.ToolKind.INGREDIENT;
        }
        if (SpearStateMachine.isSpearStack(stack)) {
            return VanillaFirstPersonItemLayer.ToolKind.SPEAR;
        }
        return ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
    }

    private static String normalizeMixOption(String packId) {
        if (packId == null || packId.isBlank()) {
            return "punchy_default";
        }
        String normalized = packId.trim().toLowerCase(Locale.ROOT);
        if ("punchy_default".equals(normalized)) {
            return "punchy_default";
        }
        if (normalized.startsWith("pack:")) {
            normalized = normalized.substring("pack:".length());
        }
        if (isPunchyDefaultPackId(normalized)) {
            return BUILTIN_PACK_OPTION_ID;
        }
        if ("vanilla".equals(normalized) || "mod_resources".equals(normalized)) {
            return "punchy_default";
        }
        return "pack:" + normalized;
    }

    private static String resolveSelectedOption(MixSlotDef slot) {
        if (slot == null) {
            return "punchy_default";
        }
        List<String> allowedOptions = getMixPackSlotOptionIds(slot.slotId);
        String rawSelected = PunchyConfig.getAnimationPackSelection(slot.slotId);
        if (rawSelected == null || rawSelected.isBlank()) {
            if (slot.resourceOrderOption != null && allowedOptions.contains(slot.resourceOrderOption)) {
                return slot.resourceOrderOption;
            }
            for (String optionId : allowedOptions) {
                if (optionId != null && !optionId.isBlank() && !"punchy_default".equals(optionId)) {
                    return optionId;
                }
            }
            return "punchy_default";
        }
        String selected = normalizeMixOption(rawSelected);
        if ("punchy_default".equals(selected)) {
            if (slot.resourceOrderOption != null && allowedOptions.contains(slot.resourceOrderOption)) {
                return slot.resourceOrderOption;
            }
            for (String optionId2 : allowedOptions) {
                if (optionId2 != null && !optionId2.isBlank() && !"punchy_default".equals(optionId2)) {
                    return optionId2;
                }
            }
            return "punchy_default";
        }
        if (selected != null && allowedOptions.contains(selected)) {
            return selected;
        }
        if (slot.resourceOrderOption != null && allowedOptions.contains(slot.resourceOrderOption)) {
            return slot.resourceOrderOption;
        }
        for (String optionId3 : allowedOptions) {
            if (optionId3 != null && !optionId3.isBlank() && !"punchy_default".equals(optionId3)) {
                return optionId3;
            }
        }
        return "punchy_default";
    }

    private static String resolveSelectedTypeOption(MixSlotDef slot, String normalizedType, String selectedMain) {
        String strNormalizeMixOption;
        if (slot == null || normalizedType == null) {
            return "punchy_default";
        }
        MixSlotPayload baselinePayload = baselinePayloadForSlot(slot);
        boolean baselineHasType = payloadHasType(baselinePayload, normalizedType);
        boolean enforceGlobalMain = isGlobalHandType(normalizedType);
        String rawSelected = PunchyConfig.getAnimationPackTypeSelection(slot.slotId, normalizedType);
        if (rawSelected == null || rawSelected.isBlank()) {
            strNormalizeMixOption = null;
        } else {
            strNormalizeMixOption = normalizeMixOption(rawSelected);
        }
        String selected = strNormalizeMixOption;
        if (enforceGlobalMain) {
            if (selected != null && !selected.isBlank() && !"punchy_default".equals(selected) && slot.optionOrder.contains(selected) && optionDefinesTypeExactly(slot, selected, normalizedType)) {
                return selected;
            }
            if (selectedMain != null && !selectedMain.isBlank() && optionDefinesTypeExactly(slot, selectedMain, normalizedType)) {
                return selectedMain;
            }
            String globalOption = getGlobalMixPriorityOptionId();
            if (globalOption != null && !globalOption.isBlank()) {
                return globalOption;
            }
            return "punchy_default";
        }
        if (selected != null && !selected.isBlank()) {
            if ("punchy_default".equals(selected)) {
                if (baselineHasType) {
                    return "punchy_default";
                }
                return "punchy_default";
            }
            if (slot.optionOrder.contains(selected) && optionSupportsType(slot, selected, normalizedType)) {
                return selected;
            }
            if (selectedMain == null || selectedMain.isBlank() || "punchy_default".equals(selectedMain)) {
                return baselineHasType ? "punchy_default" : "punchy_default";
            }
            return selectedMain;
        }
        if (selectedMain != null && !selectedMain.isBlank() && !"punchy_default".equals(selectedMain) && optionSupportsType(slot, selectedMain, normalizedType)) {
            return selectedMain;
        }
        String fallback = resolveAnimationTypeFallbackOption(slot, normalizedType, selectedMain, baselineHasType, optionId -> {
            return null;
        });
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        if (selectedMain != null && !selectedMain.isBlank() && !"punchy_default".equals(selectedMain)) {
            return selectedMain;
        }
        return "punchy_default";
    }

    private static String resolveSelectedAnimationTypeOption(MixSlotDef slot, String normalizedType, String selectedMain, List<CustomAnimationEntry> baselineEntries, Function<String, List<CustomAnimationEntry>> entriesForOption) {
        String strNormalizeMixOption;
        if (slot == null || normalizedType == null || entriesForOption == null) {
            return "punchy_default";
        }
        boolean baselineHasType = listHasType(baselineEntries, normalizedType);
        boolean enforceGlobalMain = isGlobalHandType(normalizedType);
        boolean mainHasType = (selectedMain == null || selectedMain.isBlank() || (!listHasType(entriesForOption.apply(selectedMain), normalizedType) && !optionSupportsType(slot, selectedMain, normalizedType))) ? false : true;
        String rawSelected = PunchyConfig.getAnimationPackTypeSelection(slot.slotId, normalizedType);
        if (rawSelected == null || rawSelected.isBlank()) {
            strNormalizeMixOption = null;
        } else {
            strNormalizeMixOption = normalizeMixOption(rawSelected);
        }
        String selected = strNormalizeMixOption;
        if (enforceGlobalMain) {
            if (selected != null && !selected.isBlank() && !"punchy_default".equals(selected) && slot.optionOrder.contains(selected) && listHasExactType(entriesForOption.apply(selected), normalizedType)) {
                return selected;
            }
            if (selectedMain != null && !selectedMain.isBlank() && listHasExactType(entriesForOption.apply(selectedMain), normalizedType)) {
                return selectedMain;
            }
            String globalOption = getGlobalMixPriorityOptionId();
            if (globalOption != null && !globalOption.isBlank()) {
                return globalOption;
            }
            return "punchy_default";
        }
        if (selected != null && !selected.isBlank()) {
            if ("punchy_default".equals(selected)) {
                if (baselineHasType) {
                    return "punchy_default";
                }
                return "punchy_default";
            }
            if (slot.optionOrder.contains(selected) && (listHasType(entriesForOption.apply(selected), normalizedType) || optionSupportsType(slot, selected, normalizedType))) {
                return selected;
            }
            if (!mainHasType || selectedMain == null || selectedMain.isBlank()) {
                return baselineHasType ? "punchy_default" : "punchy_default";
            }
            return selectedMain;
        }
        if (mainHasType && selectedMain != null && !selectedMain.isBlank()) {
            return selectedMain;
        }
        return resolveAnimationTypeFallbackOption(slot, normalizedType, selectedMain, baselineHasType, entriesForOption);
    }

    private static String resolveAnimationTypeFallbackOption(MixSlotDef slot, String normalizedType, String selectedMain, boolean baselineHasType, Function<String, List<CustomAnimationEntry>> entriesForOption) {
        if (slot != null && normalizedType != null && entriesForOption != null) {
            List<String> orderedOptions = new ArrayList<>(slot.optionOrder);
            for (int i = 0; i < orderedOptions.size(); i++) {
                String optionId = orderedOptions.get(i);
                if (optionId != null && !optionId.isBlank() && !optionId.equals(selectedMain) && listHasType(entriesForOption.apply(optionId), normalizedType)) {
                    return optionId;
                }
            }
            for (int i2 = 0; i2 < orderedOptions.size(); i2++) {
                String optionId2 = orderedOptions.get(i2);
                if (optionId2 != null && !optionId2.isBlank() && !optionId2.equals(selectedMain) && optionSupportsType(slot, optionId2, normalizedType)) {
                    return optionId2;
                }
            }
        }
        return baselineHasType ? "punchy_default" : "punchy_default";
    }

    private static boolean optionSupportsType(MixSlotDef slot, String optionId, String normalizedType) {
        if (slot == null || optionId == null || optionId.isBlank() || normalizedType == null || normalizedType.isBlank()) {
            return false;
        }
        if ("punchy_default".equals(optionId)) {
            MixSlotPayload baseline = baselinePayloadForSlot(slot);
            if (payloadHasType(baseline, normalizedType)) {
                return true;
            }
            return false;
        }
        MixSlotPayload payload = slot.optionPayloads.get(optionId);
        if (payloadHasType(payload, normalizedType)) {
            return true;
        }
        return false;
    }

    private static boolean slotOptionHasTypeFallbackClip(MixSlotDef slot, String optionId, String normalizedType) {
        List<String> aliases;
        Set<String> names;
        if (slot == null || optionId == null || optionId.isBlank() || normalizedType == null || normalizedType.isBlank() || (aliases = MIX_TYPE_CLIP_ALIASES.get(normalizedType)) == null || aliases.isEmpty() || (names = slot.optionClipNames.get(optionId)) == null || names.isEmpty()) {
            return false;
        }
        for (String alias : aliases) {
            if (alias != null && !alias.isBlank() && names.contains(alias.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeEntryType(String entryType) {
        if (entryType == null || entryType.isBlank()) {
            return null;
        }
        return entryType.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isGlobalHandType(String normalizedType) {
        return false;
    }

    private static String normalizeEntryTypeFamily(String entryType) {
        String normalized = normalizeEntryType(entryType);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String compact = normalized.replace("_", "").replace("-", "").replace(" ", "");
        if ("mining".equals(compact) || "miningv2".equals(compact)) {
            return "mining";
        }
        return compact;
    }

    private static boolean isSameEntryTypeFamily(String leftType, String rightType) {
        String leftFamily = normalizeEntryTypeFamily(leftType);
        String rightFamily = normalizeEntryTypeFamily(rightType);
        return leftFamily != null && leftFamily.equals(rightFamily);
    }

    private static void collectEntryTypes(Set<String> out, List<CustomAnimationEntry> entries) {
        if (out == null || entries == null || entries.isEmpty()) {
            return;
        }
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            String type = normalizeEntryType(entry == null ? null : entry.type);
            if (type != null) {
                out.add(type);
            }
        }
    }

    private static boolean payloadHasType(MixSlotPayload payload, String normalizedType) {
        if (payload == null || normalizedType == null) {
            return false;
        }
        return listHasType(payloadCustomAnimation(payload), normalizedType) || listHasType(payloadCustomItemAnimations(payload), normalizedType);
    }

    private static boolean listHasType(List<CustomAnimationEntry> entries, String normalizedType) {
        if (entries == null || entries.isEmpty() || normalizedType == null) {
            return false;
        }
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            if (isSameEntryTypeFamily(normalizedType, entry == null ? null : entry.type)) {
                return true;
            }
        }
        return false;
    }

    private static List<CustomAnimationEntry> composeCustomAnimationByType(MixSlotDef slot, String selectedMain, List<CustomAnimationEntry> base, Function<String, List<CustomAnimationEntry>> entriesForOption) {
        String selectedForType;
        if (slot == null || entriesForOption == null) {
            return null;
        }
        List<CustomAnimationEntry> safeBase = base == null ? List.of() : base;
        List<String> entryTypes = getMixPackSlotEntryTypes(slot.slotId);
        if (entryTypes.isEmpty()) {
            if (safeBase.isEmpty()) {
                return null;
            }
            return new ArrayList(safeBase);
        }
        LinkedHashMap<String, List<CustomAnimationEntry>> overrides = new LinkedHashMap<>();
        for (String type : entryTypes) {
            String normalizedType = normalizeEntryType(type);
            if (normalizedType != null && (selectedForType = resolveSelectedAnimationTypeOption(slot, normalizedType, selectedMain, safeBase, entriesForOption)) != null && !selectedForType.isBlank() && !selectedForType.equals(selectedMain)) {
                List<CustomAnimationEntry> source = entriesForOption.apply(selectedForType);
                List<CustomAnimationEntry> entriesForType = filterCustomAnimationByType(source, normalizedType);
                if (!entriesForType.isEmpty()) {
                    overrides.put(normalizedType, entriesForType);
                }
            }
        }
        return mergeCustomAnimationByType(safeBase, overrides);
    }

    private static MixSlotPayload composePayloadByType(MixSlotDef slot, String selectedMain, MixSlotPayload payload) {
        if (slot == null || payload == null) {
            return payload;
        }
        MixSlotPayload composed = mergeMixSlotPayload(null, payload);
        setPayloadCustomAnimation(composed, composeCustomAnimationByType(slot, selectedMain, payloadCustomAnimation(payload), optionId -> {
            MixSlotPayload mixSlotPayloadBaselinePayloadForSlot;
            if ("punchy_default".equals(optionId)) {
                mixSlotPayloadBaselinePayloadForSlot = baselinePayloadForSlot(slot);
            } else {
                mixSlotPayloadBaselinePayloadForSlot = slot.optionPayloads.get(optionId);
            }
            MixSlotPayload source = mixSlotPayloadBaselinePayloadForSlot;
            return payloadCustomAnimation(source);
        }));
        setPayloadCustomItemAnimations(composed, composeCustomAnimationByType(slot, selectedMain, payloadCustomItemAnimations(payload), optionId2 -> {
            MixSlotPayload mixSlotPayloadBaselinePayloadForSlot;
            if ("punchy_default".equals(optionId2)) {
                mixSlotPayloadBaselinePayloadForSlot = baselinePayloadForSlot(slot);
            } else {
                mixSlotPayloadBaselinePayloadForSlot = slot.optionPayloads.get(optionId2);
            }
            MixSlotPayload source = mixSlotPayloadBaselinePayloadForSlot;
            return payloadCustomItemAnimations(source);
        }));
        logTypeComposition(slot, selectedMain, payloadCustomAnimation(composed), payloadCustomItemAnimations(composed));
        return composed;
    }

    private static MixSlotPayload composeMergedSpecificPayloadByType(MixSlotDef hostSlot, String selectedMain, class_2960 itemId, MixSlotPayload payload) {
        if (hostSlot == null || itemId == null || payload == null) {
            return payload;
        }
        MixSlotPayload composed = mergeMixSlotPayload(null, payload);
        setPayloadCustomAnimation(composed, composeCustomAnimationByType(hostSlot, selectedMain, payloadCustomAnimation(payload), optionId -> {
            MixSlotPayload mixSlotPayloadBaselineSpecificPayloadForItem;
            Map<class_2960, MixSlotPayload> optionPayloads = hostSlot.optionSpecificPayloads.get(optionId);
            if ("punchy_default".equals(optionId)) {
                mixSlotPayloadBaselineSpecificPayloadForItem = baselineSpecificPayloadForItem(itemId, hostSlot.kind);
            } else {
                mixSlotPayloadBaselineSpecificPayloadForItem = optionPayloads == null ? null : optionPayloads.get(itemId);
            }
            MixSlotPayload source = mixSlotPayloadBaselineSpecificPayloadForItem;
            return payloadCustomAnimation(source);
        }));
        setPayloadCustomItemAnimations(composed, composeCustomAnimationByType(hostSlot, selectedMain, payloadCustomItemAnimations(payload), optionId2 -> {
            MixSlotPayload mixSlotPayloadBaselineSpecificPayloadForItem;
            Map<class_2960, MixSlotPayload> optionPayloads = hostSlot.optionSpecificPayloads.get(optionId2);
            if ("punchy_default".equals(optionId2)) {
                mixSlotPayloadBaselineSpecificPayloadForItem = baselineSpecificPayloadForItem(itemId, hostSlot.kind);
            } else {
                mixSlotPayloadBaselineSpecificPayloadForItem = optionPayloads == null ? null : optionPayloads.get(itemId);
            }
            MixSlotPayload source = mixSlotPayloadBaselineSpecificPayloadForItem;
            return payloadCustomItemAnimations(source);
        }));
        logTypeComposition(hostSlot, selectedMain, payloadCustomAnimation(composed), payloadCustomItemAnimations(composed));
        return composed;
    }

    private static void logTypeComposition(MixSlotDef slot, String selectedMain, List<CustomAnimationEntry> customAnimation, List<CustomAnimationEntry> customItemAnimations) {
        String selectedType;
        if (slot == null) {
            return;
        }
        for (String type : getMixPackSlotEntryTypes(slot.slotId)) {
            String normalizedType = normalizeEntryType(type);
            if (normalizedType != null && (selectedType = resolveSelectedTypeOption(slot, normalizedType, selectedMain)) != null && !selectedType.isBlank() && !selectedType.equals(selectedMain)) {
                filterCustomAnimationByType(customAnimation, normalizedType).size();
                filterCustomAnimationByType(customItemAnimations, normalizedType).size();
            }
        }
    }

    private static List<CustomAnimationEntry> filterCustomAnimationByType(List<CustomAnimationEntry> entries, String normalizedType) {
        if (entries == null || entries.isEmpty() || normalizedType == null) {
            return List.of();
        }
        List<CustomAnimationEntry> result = new ArrayList<>();
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            if (isSameEntryTypeFamily(normalizedType, entry == null ? null : entry.type)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static List<CustomAnimationEntry> mergeCustomAnimationByType(List<CustomAnimationEntry> base, Map<String, List<CustomAnimationEntry>> overrides) {
        List<CustomAnimationEntry> replacement;
        List<CustomAnimationEntry> safeBase = base == null ? List.of() : base;
        if (overrides == null || overrides.isEmpty()) {
            if (safeBase.isEmpty()) {
                return null;
            }
            return new ArrayList(safeBase);
        }
        List<CustomAnimationEntry> result = new ArrayList<>();
        HashSet<String> appliedTypes = new HashSet<>();
        Iterator<CustomAnimationEntry> it = safeBase.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            String type = normalizeEntryType(entry == null ? null : entry.type);
            if (type != null && overrides.containsKey(type)) {
                if (!appliedTypes.contains(type)) {
                    List<CustomAnimationEntry> replacement2 = overrides.get(type);
                    if (replacement2 != null && !replacement2.isEmpty()) {
                        result.addAll(replacement2);
                    }
                    appliedTypes.add(type);
                }
            } else {
                result.add(entry);
            }
        }
        for (Map.Entry<String, List<CustomAnimationEntry>> override : overrides.entrySet()) {
            if (override != null && override.getKey() != null && !appliedTypes.contains(override.getKey()) && (replacement = override.getValue()) != null && !replacement.isEmpty()) {
                result.addAll(replacement);
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static String customAnimationMergeKey(CustomAnimationEntry entry) {
        String familyType = normalizeEntryTypeFamily(entry == null ? null : entry.type);
        return familyType != null ? familyType : "__untyped__";
    }

    private static List<CustomAnimationEntry> mergeCustomAnimationEntryLists(List<CustomAnimationEntry> base, List<CustomAnimationEntry> overlay) {
        List<CustomAnimationEntry> safeBase = base == null ? List.of() : base;
        if (overlay == null || overlay.isEmpty()) {
            if (safeBase.isEmpty()) {
                return null;
            }
            return new ArrayList(safeBase);
        }
        LinkedHashMap<String, List<CustomAnimationEntry>> overlayGroups = new LinkedHashMap<>();
        for (CustomAnimationEntry entry : overlay) {
            overlayGroups.computeIfAbsent(customAnimationMergeKey(entry), ignored -> {
                return new ArrayList();
            }).add(entry);
        }
        List<CustomAnimationEntry> result = new ArrayList<>();
        HashSet<String> applied = new HashSet<>();
        for (CustomAnimationEntry entry2 : safeBase) {
            String key = customAnimationMergeKey(entry2);
            if (overlayGroups.containsKey(key)) {
                if (applied.add(key)) {
                    result.addAll(overlayGroups.get(key));
                }
            } else {
                result.add(entry2);
            }
        }
        for (Map.Entry<String, List<CustomAnimationEntry>> entry3 : overlayGroups.entrySet()) {
            if (entry3 != null && entry3.getKey() != null && applied.add(entry3.getKey())) {
                result.addAll(entry3.getValue());
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static List<CustomAnimationEntry> mergeCustomAnimationEntryListsPreferBase(List<CustomAnimationEntry> base, List<CustomAnimationEntry> overlay) {
        List<CustomAnimationEntry> safeBase = base == null ? List.of() : base;
        if (overlay == null || overlay.isEmpty()) {
            if (safeBase.isEmpty()) {
                return null;
            }
            return new ArrayList(safeBase);
        }
        if (safeBase.isEmpty()) {
            return new ArrayList(overlay);
        }
        LinkedHashSet<String> existing = new LinkedHashSet<>();
        Iterator<CustomAnimationEntry> it = safeBase.iterator();
        while (it.hasNext()) {
            existing.add(customAnimationMergeKey(it.next()));
        }
        List<CustomAnimationEntry> result = new ArrayList<>(safeBase);
        for (CustomAnimationEntry entry : overlay) {
            if (entry != null) {
                String key = customAnimationMergeKey(entry);
                if (!existing.contains(key)) {
                    result.add(entry);
                    existing.add(key);
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static boolean isSpecificSlotForced(MixSlotDef slot) {
        if (slot == null || slot.type != MixSlotType.SPECIFIC) {
            return false;
        }
        return PunchyConfig.isMixSpecificForceOverride(slot.slotId);
    }

    private static boolean shouldAutoUseSpecificSlot(MixSlotDef slot) {
        return slot != null && slot.type == MixSlotType.SPECIFIC && !isSpecificSlotForced(slot) && slot.optionOrder.size() == 1 && slot.resourceOrderOption != null && slot.optionOrder.contains(slot.resourceOrderOption);
    }

    private static String resolveInheritedKindOption(MixSlotDef slot) {
        if (slot == null) {
            return null;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = slot.kind;
        if (kind == null && slot.itemId != null) {
            kind = resolveKindForItem(slot.itemId);
        }
        if (kind == null) {
            return null;
        }
        String kindSlotId = "kind:" + kind.name().toLowerCase(Locale.ROOT);
        MixSlotDef kindSlot = MIX_CATALOG.slots.get(kindSlotId);
        if (kindSlot == null) {
            return "punchy_default";
        }
        return resolveSelectedOption(kindSlot);
    }

    private static void applyMixSelections() {
        if (MIX_CATALOG == null || MIX_CATALOG.slots.isEmpty()) {
            writeMergedAnimationSelectionSnapshot();
            return;
        }
        for (MixSlotDef slot : MIX_CATALOG.slots.values()) {
            if (slot != null && slot.type == MixSlotType.KIND && !slot.mergedSpecificItems.isEmpty()) {
                for (class_2960 itemId : slot.mergedSpecificItems) {
                    if (itemId != null) {
                        clearSpecificMixState(itemId);
                    }
                }
            }
        }
        for (MixSlotDef slot2 : MIX_CATALOG.slots.values()) {
            if (slot2 != null && slot2.type == MixSlotType.SPECIFIC && slot2.itemId != null && !isSpecificSlotForced(slot2) && !shouldAutoUseSpecificSlot(slot2)) {
                clearSpecificMixState(slot2.itemId);
            }
        }
        for (MixSlotDef slot3 : MIX_CATALOG.slots.values()) {
            if (slot3 != null && slot3.type == MixSlotType.KIND) {
                String selected = resolveSelectedOption(slot3);
                MixSlotPayload payload = resolveSelectedPayload(slot3, selected);
                MixSlotPayload composedPayload = composePayloadByType(slot3, selected, payload);
                applyMixSlotPayload(slot3, composedPayload);
                if (!slot3.mergedSpecificItems.isEmpty()) {
                    for (class_2960 itemId2 : slot3.mergedSpecificItems) {
                        if (itemId2 != null) {
                            MixSlotPayload specificPayload = resolveSelectedSpecificPayload(slot3, selected, itemId2, slot3.kind);
                            applySpecificPayload(itemId2, slot3.kind, composeMergedSpecificPayloadByType(slot3, selected, itemId2, specificPayload));
                        }
                    }
                }
            }
        }
        for (MixSlotDef slot4 : MIX_CATALOG.slots.values()) {
            if (slot4 != null && slot4.type == MixSlotType.SPECIFIC && (isSpecificSlotForced(slot4) || shouldAutoUseSpecificSlot(slot4))) {
                String selected2 = resolveSelectedOption(slot4);
                MixSlotPayload payload2 = resolveSelectedPayload(slot4, selected2);
                applyMixSlotPayload(slot4, composePayloadByType(slot4, selected2, payload2));
            }
        }
        writeMergedAnimationSelectionSnapshot();
    }

    private static void writeMergedAnimationSelectionSnapshot() {
        String normalized;
        try {
            Path outputDir = getConfigDirectory().resolve(Punchy.MOD_ID);
            Files.createDirectories(outputDir, new FileAttribute[0]);
            Path output = outputDir.resolve(MERGED_ANIMATION_SELECTION_FILE);
            JsonObject root = new JsonObject();
            JsonArray selectedAnimations = new JsonArray();
            root.add("selected_animations", selectedAnimations);
            Map<String, Set<String>> exportedItemTypesByKindSlot = new LinkedHashMap<>();
            if (MIX_CATALOG != null && MIX_CATALOG.slots != null && !MIX_CATALOG.slots.isEmpty()) {
                for (MixSlotDef slot : MIX_CATALOG.slots.values()) {
                    if (slot != null && slot.type == MixSlotType.KIND && slot.kind != null) {
                        JsonObject animations = collectSelectedAnimationTypesForSlot(slot, null);
                        if (animations.size() != 0) {
                            JsonObject entry = new JsonObject();
                            entry.addProperty("kind", "item");
                            entry.addProperty("definition", slot.kind.name());
                            entry.add("animations", animations);
                            selectedAnimations.add(entry);
                            Set<String> keys = new LinkedHashSet<>();
                            for (Map.Entry<String, JsonElement> animationEntry : animations.entrySet()) {
                                if (animationEntry != null && animationEntry.getKey() != null && (normalized = normalizeEntryType(animationEntry.getKey())) != null && !normalized.isBlank()) {
                                    keys.add(normalized);
                                }
                            }
                            exportedItemTypesByKindSlot.put(slot.slotId, keys);
                        }
                    }
                }
                for (MixSlotDef slot2 : MIX_CATALOG.slots.values()) {
                    if (slot2 != null && slot2.type == MixSlotType.SPECIFIC && slot2.itemId != null) {
                        JsonObject animations2 = collectSelectedAnimationTypesForSlot(slot2, null);
                        if (animations2.size() != 0) {
                            JsonObject entry2 = new JsonObject();
                            entry2.addProperty("kind", "itemSpecific");
                            entry2.addProperty("definition", slot2.itemId.toString());
                            entry2.add("animations", animations2);
                            selectedAnimations.add(entry2);
                        }
                    }
                }
            }
            FileWriter writer = new FileWriter(output.toFile());
            try {
                GSON.toJson(root, writer);
                writer.close();
            } finally {
            }
        } catch (Exception e) {
        }
    }

    private static void reapplyActiveProfileSpecificTransforms(Data activeProfileData) {
        if (activeProfileData == null || activeProfileData.itemSpecific == null || activeProfileData.itemSpecific.isEmpty()) {
            return;
        }
        Data overrides = new Data();
        overrides.itemSpecific = new HashMap();
        for (Map.Entry<String, SpecificEntry> entry : activeProfileData.itemSpecific.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                SpecificEntry src = entry.getValue();
                boolean hasTransforms = hasSideSetData(src.transforms);
                boolean hasArmTransforms = hasSideSetData(src.armTransforms);
                boolean hasPhysics = (src.physics == null || src.physics.isJsonNull()) ? false : true;
                boolean hasMetadata = (src.disableSprintLateralTilt == null && src.keepArmTuning == null && src.keepTriggerArmTuning == null && src.keepArmTuningL == null && src.keepArmTuningR == null) ? false : true;
                if (hasTransforms || hasArmTransforms || hasPhysics || hasMetadata) {
                    SpecificEntry copy = new SpecificEntry();
                    copy.kind = src.kind;
                    copy.armKind = src.armKind;
                    copy.customModelData = src.customModelData;
                    copy.renamed = src.renamed;
                    copy.model = src.model;
                    copy.transforms = copySideSet(src.transforms);
                    copy.armTransforms = copySideSet(src.armTransforms);
                    copy.physics = copyJson(src.physics);
                    copy.disableSprintLateralTilt = src.disableSprintLateralTilt;
                    copy.keepArmTuning = src.keepArmTuning;
                    copy.keepTriggerArmTuning = src.keepTriggerArmTuning;
                    copy.keepArmTuningL = src.keepArmTuningL;
                    copy.keepArmTuningR = src.keepArmTuningR;
                    overrides.itemSpecific.put(entry.getKey(), copy);
                }
            }
        }
        if (overrides.itemSpecific.isEmpty()) {
            return;
        }
        try {
            applyData(overrides, false);
        } catch (Exception e) {
        }
    }

    private static JsonObject collectSelectedAnimationTypesForSlot(MixSlotDef slot, Set<String> suppressedTypes) {
        String normalizedType;
        JsonObject animations = new JsonObject();
        if (slot == null || slot.slotId == null || slot.slotId.isBlank()) {
            return animations;
        }
        List<String> types = getMixPackSlotEntryTypes(slot.slotId);
        if (types == null || types.isEmpty()) {
            return animations;
        }
        Iterator<String> it = types.iterator();
        while (it.hasNext()) {
            String type = it.next();
            String outputType = type == null ? null : type.trim();
            if (outputType != null && !outputType.isBlank() && (normalizedType = normalizeEntryType(type)) != null && !normalizedType.isBlank() && (suppressedTypes == null || !suppressedTypes.contains(normalizedType))) {
                String selectedPack = getMixPackSelectedEntryTypeOption(slot.slotId, normalizedType);
                if (selectedPack != null && !selectedPack.isBlank() && (isGlobalHandType(normalizedType) || optionDefinesTypeExactly(slot, selectedPack, normalizedType))) {
                    animations.addProperty(outputType, selectedPack);
                }
            }
        }
        return animations;
    }

    private static boolean optionDefinesTypeExactly(MixSlotDef slot, String optionId, String normalizedType) {
        MixSlotPayload mixSlotPayloadBaselinePayloadForSlot;
        if (slot == null || optionId == null || optionId.isBlank() || normalizedType == null || normalizedType.isBlank()) {
            return false;
        }
        if ("punchy_default".equals(optionId)) {
            mixSlotPayloadBaselinePayloadForSlot = baselinePayloadForSlot(slot);
        } else {
            mixSlotPayloadBaselinePayloadForSlot = slot.optionPayloads.get(optionId);
        }
        MixSlotPayload payload = mixSlotPayloadBaselinePayloadForSlot;
        if (payload == null) {
            return false;
        }
        return listHasExactType(payloadCustomAnimation(payload), normalizedType) || listHasExactType(payloadCustomItemAnimations(payload), normalizedType);
    }

    private static boolean listHasExactType(List<CustomAnimationEntry> entries, String normalizedType) {
        if (entries == null || entries.isEmpty() || normalizedType == null || normalizedType.isBlank()) {
            return false;
        }
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            if (normalizedType.equals(normalizeEntryType(entry == null ? null : entry.type))) {
                return true;
            }
        }
        return false;
    }

    private static String summarizeEntryByType(List<CustomAnimationEntry> entries, String type) {
        if (entries == null || entries.isEmpty()) {
            return "none";
        }
        String normalizedType = normalizeEntryType(type);
        for (CustomAnimationEntry entry : entries) {
            if (entry != null && Objects.equals(normalizedType, normalizeEntryType(entry.type))) {
                StringBuilder builder = new StringBuilder();
                builder.append("type=").append(entry.type);
                if (entry.name != null) {
                    builder.append(",name=").append(entry.name);
                }
                if (entry.start != null) {
                    builder.append(",start=").append(entry.start);
                }
                if (entry.looping != null) {
                    builder.append(",looping=").append(entry.looping);
                }
                if (entry.end != null) {
                    builder.append(",end=").append(entry.end);
                }
                if (entry.var_1 != null) {
                    builder.append(",var_1=").append(entry.var_1);
                }
                if (entry.mix_pack_option != null) {
                    builder.append(",mix=").append(entry.mix_pack_option);
                }
                return builder.toString();
            }
        }
        return "missing";
    }

    private static List<BetterCombatEntry> extractDefaultBetterCombatFromDefaultAnimations(List<CustomAnimationEntry> entries) {
        BetterCombatEntry combo;
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        List<BetterCombatEntry> out = new ArrayList<>();
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            if (entry != null && entry.betterCombat != null && !entry.betterCombat.isEmpty()) {
                for (BetterCombatEntry nested : entry.betterCombat) {
                    if (nested != null && isMeleeComboType(nested.type) && !allBlank(nested.attack_1, nested.attack_2, nested.attack_3, nested.attack_4, nested.attack_5, nested.attack_6)) {
                        out.add(nested);
                    }
                }
            }
            if (isMeleeComboType(entry == null ? null : entry.type) && (combo = toBetterCombatEntry(entry)) != null) {
                out.add(combo);
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        return out;
    }

    private static List<CustomAnimationEntry> stripBetterCombatFromDefaultAnimations(List<CustomAnimationEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        List<CustomAnimationEntry> filtered = new ArrayList<>();
        Iterator<CustomAnimationEntry> it = entries.iterator();
        while (it.hasNext()) {
            CustomAnimationEntry entry = it.next();
            if (entry == null || entry.betterCombat == null || entry.betterCombat.isEmpty()) {
                if (!isMeleeComboType(entry == null ? null : entry.type)) {
                    filtered.add(entry);
                }
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        return filtered;
    }

    private static boolean isMeleeComboType(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        return "meleecombo".equals(normalized);
    }

    private static BetterCombatEntry toBetterCombatEntry(CustomAnimationEntry entry) {
        if (entry == null) {
            return null;
        }
        BetterCombatEntry out = new BetterCombatEntry();
        out.type = "melee_combo";
        out.attack_1 = firstNonBlank(entry.attack_1, entry.var_1);
        out.attack_2 = firstNonBlank(entry.attack_2, entry.var_2);
        out.attack_3 = firstNonBlank(entry.attack_3, entry.var_3);
        out.attack_4 = firstNonBlank(entry.attack_4, null);
        out.attack_5 = firstNonBlank(entry.attack_5, null);
        out.attack_6 = firstNonBlank(entry.attack_6, null);
        if (allBlank(out.attack_1, out.attack_2, out.attack_3, out.attack_4, out.attack_5, out.attack_6)) {
            return null;
        }
        return out;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private static boolean allBlank(String... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String summarizeGroupTypeEntry(Data data, VanillaFirstPersonItemLayer.ToolKind kind, String type) {
        if (data == null || kind == null || type == null || data.item == null) {
            return "none";
        }
        GroupEntry group = data.item.get(kind.name());
        if (group == null) {
            return "group-missing";
        }
        String fromAnim = summarizeEntryByType(group.customAnimation, type);
        String fromItemAnim = summarizeEntryByType(group.customItemAnimations, type);
        return "anim={" + fromAnim + "} itemAnim={" + fromItemAnim + "}";
    }

    private static MixSlotPayload resolveSelectedPayload(MixSlotDef slot, String selected) {
        if (slot == null || selected == null || selected.isBlank() || "punchy_default".equals(selected)) {
            return baselinePayloadForSlot(slot);
        }
        int selectedIndex = new ArrayList(slot.optionOrder).indexOf(selected);
        if (selectedIndex >= 0) {
            MixSlotPayload merged = mergeMixSlotPayload(null, baselinePayloadForSlot(slot));
            List<String> orderedOptions = new ArrayList<>(slot.optionOrder);
            for (int index = orderedOptions.size() - 1; index >= selectedIndex; index--) {
                String optionId = orderedOptions.get(index);
                MixSlotPayload optionPayload = slot.optionPayloads.get(optionId);
                merged = mergeMixSlotPayload(merged, optionPayload);
            }
            return merged;
        }
        MixSlotPayload baseline = baselinePayloadForSlot(slot);
        if (baseline == null) {
            return null;
        }
        return mergeMixSlotPayload(null, baseline);
    }

    private static MixSlotPayload resolveSelectedSpecificPayload(MixSlotDef slot, String selected, class_2960 itemId, VanillaFirstPersonItemLayer.ToolKind fallbackKind) {
        if (slot == null || itemId == null) {
            return null;
        }
        MixSlotPayload merged = mergeMixSlotPayload(null, baselineSpecificPayloadForItem(itemId, fallbackKind));
        if (selected == null || selected.isBlank() || "punchy_default".equals(selected)) {
            return merged;
        }
        List<String> orderedOptions = new ArrayList<>(slot.optionOrder);
        int selectedIndex = orderedOptions.indexOf(selected);
        if (selectedIndex < 0) {
            return merged;
        }
        for (int index = orderedOptions.size() - 1; index >= selectedIndex; index--) {
            String optionId = orderedOptions.get(index);
            Map<class_2960, MixSlotPayload> optionPayloads = slot.optionSpecificPayloads.get(optionId);
            MixSlotPayload optionPayload = optionPayloads == null ? null : optionPayloads.get(itemId);
            merged = mergeMixSlotPayload(merged, optionPayload);
        }
        return merged;
    }

    private static MixSlotPayload baselineSpecificPayloadForItem(class_2960 itemId, VanillaFirstPersonItemLayer.ToolKind fallbackKind) {
        if (itemId == null || DEFAULTS_RESOURCE_STATE == null || DEFAULTS_RESOURCE_STATE.itemSpecific == null || DEFAULTS_RESOURCE_STATE.itemSpecific.isEmpty()) {
            return null;
        }
        SpecificEntry entry = DEFAULTS_RESOURCE_STATE.itemSpecific.get(itemId.toString());
        if (entry == null) {
            Iterator<Map.Entry<String, SpecificEntry>> it = DEFAULTS_RESOURCE_STATE.itemSpecific.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<String, SpecificEntry> candidate = it.next();
                if (candidate != null && candidate.getKey() != null && candidate.getValue() != null && specificKeyTargetsItem(candidate.getKey(), itemId)) {
                    entry = candidate.getValue();
                    break;
                }
            }
        }
        if (entry == null) {
            return null;
        }
        VanillaFirstPersonItemLayer.ToolKind kind = parseToolKind(entry.kind);
        if (kind == null) {
            kind = fallbackKind != null ? fallbackKind : resolveKindForItem(itemId);
        }
        return payloadFromSpecific(entry, kind, "punchy_default");
    }

    private static void applySpecificPayload(class_2960 itemId, VanillaFirstPersonItemLayer.ToolKind fallbackKind, MixSlotPayload payload) {
        if (itemId == null) {
            return;
        }
        clearSpecificMixState(itemId);
        if (payload == null) {
            return;
        }
        SpecificEntry entry = payload.specificEntry != null ? copySpecificEntry(payload.specificEntry) : null;
        if (entry == null) {
            return;
        }
        if ((entry.kind == null || entry.kind.isBlank()) && fallbackKind != null) {
            entry.kind = fallbackKind.name();
        }
        Data temp = new Data();
        temp.itemSpecific.put(itemId.toString(), entry);
        applyData(temp, false);
    }

    private static MixSlotPayload baselinePayloadForSlot(MixSlotDef slot) {
        GroupEntry entry;
        if (slot == null || DEFAULTS_RESOURCE_STATE == null) {
            return null;
        }
        if (slot.type == MixSlotType.KIND) {
            if (slot.kind == VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND && DEFAULTS_RESOURCE_STATE.emptyHand != null) {
                return payloadFromEmptyHand(DEFAULTS_RESOURCE_STATE.emptyHand, VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND, "punchy_default");
            }
            if (DEFAULTS_RESOURCE_STATE.item == null || slot.kind == null || (entry = DEFAULTS_RESOURCE_STATE.item.get(slot.kind.name())) == null) {
                return null;
            }
            return payloadFromGroup(entry, slot.kind, "punchy_default");
        }
        if (slot.type == MixSlotType.SPECIFIC && slot.itemId != null && DEFAULTS_RESOURCE_STATE.itemSpecific != null) {
            SpecificEntry entry2 = DEFAULTS_RESOURCE_STATE.itemSpecific.get(slot.itemId.toString());
            if (entry2 == null) {
                Iterator<Map.Entry<String, SpecificEntry>> it = DEFAULTS_RESOURCE_STATE.itemSpecific.entrySet().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Map.Entry<String, SpecificEntry> base = it.next();
                    if (base != null && base.getKey() != null && base.getValue() != null && specificKeyTargetsItem(base.getKey(), slot.itemId)) {
                        entry2 = base.getValue();
                        break;
                    }
                }
            }
            if (entry2 == null) {
                return null;
            }
            VanillaFirstPersonItemLayer.ToolKind kind = parseToolKind(entry2.kind);
            if (kind == null) {
                kind = slot.kind != null ? slot.kind : resolveKindForItem(slot.itemId);
            }
            return payloadFromSpecific(entry2, kind, "punchy_default");
        }
        return null;
    }

    private static void applyMixSlotPayload(MixSlotDef slot, MixSlotPayload payload) {
        EmptyHandEntry emptyHandEntryEmptyHandEntryFromGroup;
        if (slot == null) {
            return;
        }
        if (slot.type == MixSlotType.KIND) {
            if (slot.kind == null) {
                return;
            }
            resetKindMixState(slot.kind);
            if (payload == null) {
                return;
            }
            Data temp = new Data();
            if (slot.kind == VanillaFirstPersonItemLayer.ToolKind.EMPTY_HAND) {
                if (payload.emptyHandEntry != null) {
                    emptyHandEntryEmptyHandEntryFromGroup = copyEmptyHandEntry(payload.emptyHandEntry);
                } else {
                    emptyHandEntryEmptyHandEntryFromGroup = emptyHandEntryFromGroup(payload.groupEntry);
                }
                EmptyHandEntry emptyPayload = emptyHandEntryEmptyHandEntryFromGroup;
                if (emptyPayload == null) {
                    return;
                } else {
                    temp.emptyHand = emptyPayload;
                }
            } else if (payload.groupEntry == null) {
                return;
            } else {
                temp.item.put(slot.kind.name(), copyGroupEntry(payload.groupEntry));
            }
            applyData(temp, false);
            return;
        }
        if (slot.type != MixSlotType.SPECIFIC || slot.itemId == null) {
            return;
        }
        clearSpecificMixState(slot.itemId);
        if (payload == null) {
            return;
        }
        SpecificEntry entry = payload.specificEntry != null ? copySpecificEntry(payload.specificEntry) : null;
        if (entry == null) {
            return;
        }
        if ((entry.kind == null || entry.kind.isBlank()) && slot.kind != null) {
            entry.kind = slot.kind.name();
        }
        Data temp2 = new Data();
        temp2.itemSpecific.put(slot.itemId.toString(), entry);
        applyData(temp2, false);
    }

    private static EmptyHandEntry emptyHandEntryFromGroup(GroupEntry group) {
        if (group == null) {
            return null;
        }
        try {
            return (EmptyHandEntry) GSON.fromJson(GSON.toJsonTree(group), EmptyHandEntry.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static void clearSpecificMixState(class_2960 itemId) {
        if (itemId == null) {
            return;
        }
        ToolTuning.clearSpecificOverride(itemId);
    }

    private static void resetKindMixState(VanillaFirstPersonItemLayer.ToolKind kind) {
        if (kind == null) {
            return;
        }
        ToolTuning.setGroupAnimation(kind, null);
        ToolTuning.setGroupItemAnimation(kind, null);
    }

    private static void collectCompatProfiles(class_3300 manager, String searchPath, String namespace, String prefix, Set<String> result) {
        Map<class_2960, class_3298> found = manager.method_14488(searchPath, loc -> {
            return loc.method_12836().equals(namespace) && loc.method_12832().startsWith(prefix) && loc.method_12832().endsWith(".json");
        });
        for (class_2960 id : found.keySet()) {
            String path = id.method_12832();
            String file = path.substring(prefix.length());
            if (file.endsWith(".json")) {
                file = file.substring(0, file.length() - 5);
            }
            if (!file.isBlank()) {
                result.add(file.toLowerCase(Locale.ROOT));
            }
        }
    }

    private static SideSet resolveGlobalArm(Data data) {
        if (data == null) {
            return null;
        }
        return data.globalArm;
    }

    private static SideSet resolveGlobalItem(Data data) {
        if (data == null) {
            return null;
        }
        return data.globalItem;
    }

    private static Data loadCompatProfile(class_3300 resourceManager, String profile) {
        String normalized = profile.endsWith(".json") ? profile.substring(0, profile.length() - 5) : profile;
        Data merged = null;
        Path compatDir = getCompatDirectory();
        try {
            Files.createDirectories(compatDir, new FileAttribute[0]);
        } catch (Exception e) {
        }
        Path filePath = compatDir.resolve(normalized + ".json");
        if (Files.exists(filePath, new LinkOption[0])) {
            try {
                FileReader reader = new FileReader(filePath.toFile());
                try {
                    Data local = (Data) GSON.fromJson(reader, Data.class);
                    merged = mergeData(null, local);
                    reader.close();
                } finally {
                }
            } catch (Exception e2) {
            }
        }
        return finalizeCompatData(normalized, merged);
    }

    private static Data loadCompatProfileFromResource(class_3300 resourceManager, String namespace, String path, String profile) {
        return null;
    }

    private static List<class_3298> tryGetResourceStack(class_3300 resourceManager, class_2960 id) {
        if (resourceManager == null || id == null) {
            return null;
        }
        try {
            List<class_3298> stack = resourceManager.method_14489(id);
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            return new ArrayList(stack);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<class_2960, List<class_3298>> tryListResourceStacks(class_3300 resourceManager, String searchPath) {
        if (resourceManager == null || searchPath == null || searchPath.isBlank()) {
            return null;
        }
        try {
            Map<class_2960, List<class_3298>> value = resourceManager.method_41265(searchPath, loc -> {
                return true;
            });
            if (value == null || value.isEmpty()) {
                return null;
            }
            Map<class_2960, List<class_3298>> result = new LinkedHashMap<>();
            for (Map.Entry<class_2960, List<class_3298>> entry : value.entrySet()) {
                class_2960 id = entry.getKey();
                List<class_3298> list = entry.getValue();
                if (id != null && list != null && !list.isEmpty()) {
                    result.put(id, new ArrayList<>(list));
                }
            }
            if (result.isEmpty()) {
                return null;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static Data readCompatResource(class_3298 resource) {
        if (resource == null) {
            return null;
        }
        try {
            InputStream stream = resource.method_14482();
            try {
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                try {
                    Data data = (Data) GSON.fromJson(reader, Data.class);
                    reader.close();
                    if (stream != null) {
                        stream.close();
                    }
                    return data;
                } catch (Throwable th) {
                    try {
                        reader.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } finally {
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Data copyData(Data input) {
        if (input == null) {
            return null;
        }
        return (Data) GSON.fromJson(GSON.toJsonTree(input), Data.class);
    }

    private static GroupEntry copyGroupEntry(GroupEntry input) {
        if (input == null) {
            return null;
        }
        return (GroupEntry) GSON.fromJson(GSON.toJsonTree(input), GroupEntry.class);
    }

    private static EmptyHandEntry copyEmptyHandEntry(EmptyHandEntry input) {
        if (input == null) {
            return null;
        }
        return (EmptyHandEntry) GSON.fromJson(GSON.toJsonTree(input), EmptyHandEntry.class);
    }

    private static SpecificEntry copySpecificEntry(SpecificEntry input) {
        if (input == null) {
            return null;
        }
        return (SpecificEntry) GSON.fromJson(GSON.toJsonTree(input), SpecificEntry.class);
    }

    private static Data mergeDataPreferBase(Data base, Data overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay;
        }
        base.globalItem = mergeSideSetPreferBase(base.globalItem, overlay.globalItem);
        base.globalArm = mergeSideSetPreferBase(base.globalArm, overlay.globalArm);
        base.default_animations = mergeCustomAnimationEntryListsPreferBase(base.default_animations, overlay.default_animations);
        base.emptyHand = mergeEmptyHandPreferBase(base.emptyHand, overlay.emptyHand);
        base.item = mergeGroupEntryMapPreferBase(base.item, overlay.item);
        base.armMesh = mergeSideSetMapPreferBase(base.armMesh, overlay.armMesh);
        base.chestProxy = mergePoseMapPreferBase(base.chestProxy, overlay.chestProxy);
        base.fluidProxy = mergePoseMapPreferBase(base.fluidProxy, overlay.fluidProxy);
        base.enchantParticleProxy = mergePoseMapPreferBase(base.enchantParticleProxy, overlay.enchantParticleProxy);
        base.paddleProxy = mergePoseMapPreferBase(base.paddleProxy, overlay.paddleProxy);
        base.fishingLineProxy = mergePoseMapPreferBase(base.fishingLineProxy, overlay.fishingLineProxy);
        base.itemSpecific = mergeSpecificMapPreferBase(base, base.itemSpecific, overlay.itemSpecific);
        base.customGroups = mergeCustomGroupsPreferBase(base.customGroups, overlay.customGroups);
        return base;
    }

    private static SideSet mergeSideSetPreferBase(SideSet base, SideSet overlay) {
        if (overlay == null || (overlay.right == null && overlay.left == null)) {
            return base;
        }
        if (base == null) {
            base = new SideSet();
        }
        if (base.right == null && overlay.right != null) {
            base.right = overlay.right;
        }
        if (base.left == null && overlay.left != null) {
            base.left = overlay.left;
        }
        return base;
    }

    private static EmptyHandEntry mergeEmptyHandPreferBase(EmptyHandEntry base, EmptyHandEntry overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            base = new EmptyHandEntry();
        }
        base.transforms = mergeSideSetPreferBase(base.transforms, overlay.transforms);
        if ((base.physics == null || base.physics.isJsonNull()) && overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (base.disableSprintLateralTilt == null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (base.keepArmTuning == null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (base.keepTriggerArmTuning == null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (base.keepArmTuningL == null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (base.keepArmTuningR == null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        base.armTransforms = mergeSideSetPreferBase(base.armTransforms, overlay.armTransforms);
        base.customAnimation = mergeCustomAnimationEntryListsPreferBase(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryListsPreferBase(base.customItemAnimations, overlay.customItemAnimations);
        if ((base.betterCombat == null || base.betterCombat.isEmpty()) && overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        return base;
    }

    private static Map<String, SideSet> mergeSideSetMapPreferBase(Map<String, SideSet> base, Map<String, SideSet> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, SideSet> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                SideSet existing = base.get(entry.getKey());
                if (existing == null) {
                    base.put(entry.getKey(), entry.getValue());
                } else {
                    base.put(entry.getKey(), mergeSideSetPreferBase(existing, entry.getValue()));
                }
            }
        }
        return base;
    }

    private static Map<String, Pose> mergePoseMapPreferBase(Map<String, Pose> base, Map<String, Pose> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, Pose> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                base.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return base;
    }

    private static Map<String, GroupEntry> mergeGroupEntryMapPreferBase(Map<String, GroupEntry> base, Map<String, GroupEntry> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, GroupEntry> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                GroupEntry merged = mergeGroupEntryPreferBase(base.get(entry.getKey()), entry.getValue());
                base.put(entry.getKey(), merged);
            }
        }
        return base;
    }

    private static GroupEntry mergeGroupEntryPreferBase(GroupEntry base, GroupEntry overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay;
        }
        if (overlay.transforms == null && (overlay.right != null || overlay.left != null)) {
            overlay.transforms = new SideSet();
            overlay.transforms.right = overlay.right;
            overlay.transforms.left = overlay.left;
        }
        base.transforms = mergeSideSetPreferBase(base.transforms, overlay.transforms);
        base.armTransforms = mergeSideSetPreferBase(base.armTransforms, overlay.armTransforms);
        if ((base.physics == null || base.physics.isJsonNull()) && overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (base.disableSprintLateralTilt == null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (base.keepArmTuning == null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (base.keepTriggerArmTuning == null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (base.keepArmTuningL == null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (base.keepArmTuningR == null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        if (base.disableWalkOnWater == null) {
            base.disableWalkOnWater = overlay.disableWalkOnWater;
        }
        if (base.disableDryOut == null) {
            base.disableDryOut = overlay.disableDryOut;
        }
        if (base.disableSwiming == null) {
            base.disableSwiming = overlay.disableSwiming;
        }
        base.customAnimation = mergeCustomAnimationEntryListsPreferBase(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryListsPreferBase(base.customItemAnimations, overlay.customItemAnimations);
        if ((base.particleEmitters == null || base.particleEmitters.isJsonNull()) && overlay.particleEmitters != null && !overlay.particleEmitters.isJsonNull()) {
            base.particleEmitters = overlay.particleEmitters;
        }
        if ((base.particle == null || base.particle.isJsonNull()) && overlay.particle != null && !overlay.particle.isJsonNull()) {
            base.particle = overlay.particle;
        }
        if ((base.particles == null || base.particles.isJsonNull()) && overlay.particles != null && !overlay.particles.isJsonNull()) {
            base.particles = overlay.particles;
        }
        if ((base.particleEffects == null || base.particleEffects.isJsonNull()) && overlay.particleEffects != null && !overlay.particleEffects.isJsonNull()) {
            base.particleEffects = overlay.particleEffects;
        }
        if ((base.particleEmission == null || base.particleEmission.isJsonNull()) && overlay.particleEmission != null && !overlay.particleEmission.isJsonNull()) {
            base.particleEmission = overlay.particleEmission;
        }
        if ((base.betterCombat == null || base.betterCombat.isEmpty()) && overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        return base;
    }

    private static Map<String, SpecificEntry> mergeSpecificMapPreferBase(Data baseData, Map<String, SpecificEntry> base, Map<String, SpecificEntry> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, SpecificEntry> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                class_2960 itemId = resolveSpecificItemId(entry.getKey());
                boolean protectedTransforms = hasHigherPriorityTransformsForItem(baseData, itemId);
                boolean protectedArmTransforms = hasHigherPriorityArmTransformsForItem(baseData, itemId);
                SpecificEntry merged = mergeSpecificEntryPreferBase(base.get(entry.getKey()), entry.getValue(), protectedTransforms, protectedArmTransforms);
                base.put(entry.getKey(), merged);
            }
        }
        return base;
    }

    private static SpecificEntry mergeSpecificEntryPreferBase(SpecificEntry base, SpecificEntry overlay) {
        return mergeSpecificEntryPreferBase(base, overlay, false, false);
    }

    private static SpecificEntry mergeSpecificEntryPreferBase(SpecificEntry base, SpecificEntry overlay, boolean protectTransforms, boolean protectArmTransforms) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            if (!protectTransforms && !protectArmTransforms) {
                return overlay;
            }
            SpecificEntry copy = copySpecificEntry(overlay);
            if (protectTransforms) {
                copy.transforms = null;
            }
            if (protectArmTransforms) {
                copy.armTransforms = null;
            }
            return copy;
        }
        if (protectTransforms && protectArmTransforms) {
            return base;
        }
        if (base.kind == null) {
            base.kind = overlay.kind;
        }
        if (base.armKind == null) {
            base.armKind = overlay.armKind;
        }
        if (base.renamed == null) {
            base.renamed = overlay.renamed;
        }
        if (base.model == null) {
            base.model = overlay.model;
        }
        if (base.customModelData == null) {
            base.customModelData = overlay.customModelData;
        }
        if ((base.physics == null || base.physics.isJsonNull()) && overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (base.disableSprintLateralTilt == null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (base.keepArmTuning == null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (base.keepTriggerArmTuning == null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (base.keepArmTuningL == null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (base.keepArmTuningR == null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        if (base.disableWalkOnWater == null) {
            base.disableWalkOnWater = overlay.disableWalkOnWater;
        }
        if (base.disableDryOut == null) {
            base.disableDryOut = overlay.disableDryOut;
        }
        if (base.disableSwiming == null) {
            base.disableSwiming = overlay.disableSwiming;
        }
        if ((base.animation == null || base.animation.isEmpty()) && overlay.animation != null && !overlay.animation.isEmpty()) {
            base.animation = overlay.animation;
        }
        base.customAnimation = mergeCustomAnimationEntryListsPreferBase(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryListsPreferBase(base.customItemAnimations, overlay.customItemAnimations);
        if ((base.particleEmitters == null || base.particleEmitters.isJsonNull()) && overlay.particleEmitters != null && !overlay.particleEmitters.isJsonNull()) {
            base.particleEmitters = overlay.particleEmitters;
        }
        if ((base.particle == null || base.particle.isJsonNull()) && overlay.particle != null && !overlay.particle.isJsonNull()) {
            base.particle = overlay.particle;
        }
        if ((base.particles == null || base.particles.isJsonNull()) && overlay.particles != null && !overlay.particles.isJsonNull()) {
            base.particles = overlay.particles;
        }
        if ((base.particleEffects == null || base.particleEffects.isJsonNull()) && overlay.particleEffects != null && !overlay.particleEffects.isJsonNull()) {
            base.particleEffects = overlay.particleEffects;
        }
        if ((base.particleEmission == null || base.particleEmission.isJsonNull()) && overlay.particleEmission != null && !overlay.particleEmission.isJsonNull()) {
            base.particleEmission = overlay.particleEmission;
        }
        if ((base.betterCombat == null || base.betterCombat.isEmpty()) && overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        if (!protectTransforms) {
            base.transforms = mergeSideSetPreferBase(base.transforms, overlay.transforms);
        }
        if (!protectArmTransforms) {
            base.armTransforms = mergeSideSetPreferBase(base.armTransforms, overlay.armTransforms);
        }
        return base;
    }

    private static boolean hasHigherPriorityTransformsForItem(Data data, class_2960 itemId) {
        if (data == null || itemId == null) {
            return false;
        }
        if (data.itemSpecific != null && !data.itemSpecific.isEmpty()) {
            for (Map.Entry<String, SpecificEntry> entry : data.itemSpecific.entrySet()) {
                if (entry != null && entry.getValue() != null && specificKeyTargetsItem(entry.getKey(), itemId) && hasSideSetData(entry.getValue().transforms)) {
                    return true;
                }
            }
        }
        GroupEntry group = resolveHighestPriorityGroupEntryForItem(data, itemId);
        return group != null && hasSideSetData(group.transforms);
    }

    private static boolean hasHigherPriorityArmTransformsForItem(Data data, class_2960 itemId) {
        if (data == null || itemId == null) {
            return false;
        }
        if (data.itemSpecific != null && !data.itemSpecific.isEmpty()) {
            for (Map.Entry<String, SpecificEntry> entry : data.itemSpecific.entrySet()) {
                if (entry != null && entry.getValue() != null && specificKeyTargetsItem(entry.getKey(), itemId) && hasSideSetData(entry.getValue().armTransforms)) {
                    return true;
                }
            }
        }
        GroupEntry group = resolveHighestPriorityGroupEntryForItem(data, itemId);
        return group != null && hasSideSetData(group.armTransforms);
    }

    private static GroupEntry resolveHighestPriorityGroupEntryForItem(Data data, class_2960 itemId) {
        if (data == null || itemId == null || data.item == null || data.item.isEmpty()) {
            return null;
        }
        String itemKey = itemId.toString();
        for (Map.Entry<String, GroupEntry> entry : data.item.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && groupEntryMatchesItem(data, entry.getKey(), itemKey, itemId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean groupEntryMatchesItem(Data data, String groupKey, String itemKey, class_2960 itemId) {
        VanillaFirstPersonItemLayer.ToolKind toolKindResolveItemKind;
        if (groupKey == null || itemKey == null || itemId == null) {
            return false;
        }
        List<String> customGroupItems = resolveCustomGroupItems(data != null ? data.customGroups : null, groupKey);
        if (customGroupItems != null && customGroupItems.contains(itemKey)) {
            return true;
        }
        try {
            VanillaFirstPersonItemLayer.ToolKind kind = VanillaFirstPersonItemLayer.ToolKind.valueOf(groupKey);
            class_1792 item = (class_1792) class_7923.field_41178.method_17966(itemId).orElse(class_1802.field_8162);
            if (item == class_1802.field_8162) {
                return false;
            }
            class_1799 stack = new class_1799(item);
            if (stack != null) {
                toolKindResolveItemKind = ToolKindResolver.resolveItemKind(stack, VanillaFirstPersonItemLayer.ToolKind.INGREDIENT);
            } else {
                toolKindResolveItemKind = VanillaFirstPersonItemLayer.ToolKind.INGREDIENT;
            }
            VanillaFirstPersonItemLayer.ToolKind resolvedKind = toolKindResolveItemKind;
            return resolvedKind == kind;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static List<String> resolveCustomGroupItems(List<CustomGroupDefinition> customGroups, String name) {
        if (customGroups == null || customGroups.isEmpty() || name == null) {
            return null;
        }
        for (CustomGroupDefinition entry : customGroups) {
            if (entry != null && entry.name != null && entry.items != null && entry.name.equalsIgnoreCase(name)) {
                return entry.items;
            }
        }
        return null;
    }

    private static boolean hasSideSetData(SideSet set) {
        return (set == null || (set.right == null && set.left == null)) ? false : true;
    }

    private static List<CustomGroupDefinition> mergeCustomGroupsPreferBase(List<CustomGroupDefinition> base, List<CustomGroupDefinition> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new ArrayList();
        }
        Set<String> knownNames = new HashSet<>();
        for (CustomGroupDefinition entry : base) {
            if (entry != null && entry.name != null && !entry.name.isBlank()) {
                knownNames.add(entry.name.toLowerCase(Locale.ROOT));
            }
        }
        for (CustomGroupDefinition entry2 : overlay) {
            if (entry2 != null) {
                if (entry2.name == null || entry2.name.isBlank()) {
                    base.add(entry2);
                } else {
                    String key = entry2.name.toLowerCase(Locale.ROOT);
                    if (knownNames.add(key)) {
                        base.add(entry2);
                    }
                }
            }
        }
        return base;
    }

    private static Data mergeData(Data base, Data overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay;
        }
        base.globalItem = mergeSideSet(base.globalItem, overlay.globalItem);
        base.globalArm = mergeSideSet(base.globalArm, overlay.globalArm);
        base.default_animations = mergeCustomAnimationEntryLists(base.default_animations, overlay.default_animations);
        base.emptyHand = mergeEmptyHand(base.emptyHand, overlay.emptyHand);
        base.item = mergeGroupEntryMap(base.item, overlay.item);
        base.armMesh = mergeSideSetMap(base.armMesh, overlay.armMesh);
        base.chestProxy = mergePoseMap(base.chestProxy, overlay.chestProxy);
        base.fluidProxy = mergePoseMap(base.fluidProxy, overlay.fluidProxy);
        base.enchantParticleProxy = mergePoseMap(base.enchantParticleProxy, overlay.enchantParticleProxy);
        base.paddleProxy = mergePoseMap(base.paddleProxy, overlay.paddleProxy);
        base.fishingLineProxy = mergePoseMap(base.fishingLineProxy, overlay.fishingLineProxy);
        base.itemSpecific = mergeSpecificMap(base.itemSpecific, overlay.itemSpecific);
        if (overlay.customGroups != null && !overlay.customGroups.isEmpty()) {
            if (base.customGroups == null) {
                base.customGroups = new ArrayList();
            }
            base.customGroups.addAll(overlay.customGroups);
        }
        return base;
    }

    private static SideSet mergeSideSet(SideSet base, SideSet overlay) {
        if (overlay == null || (overlay.right == null && overlay.left == null)) {
            return base;
        }
        if (base == null) {
            base = new SideSet();
        }
        if (overlay.right != null) {
            base.right = overlay.right;
        }
        if (overlay.left != null) {
            base.left = overlay.left;
        }
        return base;
    }

    private static EmptyHandEntry mergeEmptyHand(EmptyHandEntry base, EmptyHandEntry overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            base = new EmptyHandEntry();
        }
        base.transforms = mergeSideSet(base.transforms, overlay.transforms);
        if (overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (overlay.disableSprintLateralTilt != null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (overlay.keepArmTuning != null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (overlay.keepTriggerArmTuning != null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (overlay.keepArmTuningL != null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (overlay.keepArmTuningR != null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        base.armTransforms = mergeSideSet(base.armTransforms, overlay.armTransforms);
        base.customAnimation = mergeCustomAnimationEntryLists(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryLists(base.customItemAnimations, overlay.customItemAnimations);
        if (overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        return base;
    }

    private static Map<String, SideSet> mergeSideSetMap(Map<String, SideSet> base, Map<String, SideSet> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, SideSet> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                base.put(entry.getKey(), entry.getValue());
            }
        }
        return base;
    }

    private static Map<String, Pose> mergePoseMap(Map<String, Pose> base, Map<String, Pose> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, Pose> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                base.put(entry.getKey(), entry.getValue());
            }
        }
        return base;
    }

    private static Map<String, GroupEntry> mergeGroupEntryMap(Map<String, GroupEntry> base, Map<String, GroupEntry> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, GroupEntry> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                GroupEntry merged = mergeGroupEntry(base.get(entry.getKey()), entry.getValue());
                base.put(entry.getKey(), merged);
            }
        }
        return base;
    }

    private static GroupEntry mergeGroupEntry(GroupEntry base, GroupEntry overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay;
        }
        if (overlay.transforms == null && (overlay.right != null || overlay.left != null)) {
            overlay.transforms = new SideSet();
            overlay.transforms.right = overlay.right;
            overlay.transforms.left = overlay.left;
        }
        base.transforms = mergeSideSet(base.transforms, overlay.transforms);
        base.armTransforms = mergeSideSet(base.armTransforms, overlay.armTransforms);
        if (overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (overlay.disableSprintLateralTilt != null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (overlay.keepArmTuning != null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (overlay.keepTriggerArmTuning != null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (overlay.keepArmTuningL != null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (overlay.keepArmTuningR != null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        if (overlay.disableWalkOnWater != null) {
            base.disableWalkOnWater = overlay.disableWalkOnWater;
        }
        if (overlay.disableDryOut != null) {
            base.disableDryOut = overlay.disableDryOut;
        }
        if (overlay.disableSwiming != null) {
            base.disableSwiming = overlay.disableSwiming;
        }
        base.customAnimation = mergeCustomAnimationEntryLists(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryLists(base.customItemAnimations, overlay.customItemAnimations);
        if (overlay.particleEmitters != null && !overlay.particleEmitters.isJsonNull()) {
            base.particleEmitters = overlay.particleEmitters;
        }
        if (overlay.particle != null && !overlay.particle.isJsonNull()) {
            base.particle = overlay.particle;
        }
        if (overlay.particles != null && !overlay.particles.isJsonNull()) {
            base.particles = overlay.particles;
        }
        if (overlay.particleEffects != null && !overlay.particleEffects.isJsonNull()) {
            base.particleEffects = overlay.particleEffects;
        }
        if (overlay.particleEmission != null && !overlay.particleEmission.isJsonNull()) {
            base.particleEmission = overlay.particleEmission;
        }
        if (overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        return base;
    }

    private static Map<String, SpecificEntry> mergeSpecificMap(Map<String, SpecificEntry> base, Map<String, SpecificEntry> overlay) {
        if (overlay == null || overlay.isEmpty()) {
            return base;
        }
        if (base == null) {
            base = new HashMap();
        }
        for (Map.Entry<String, SpecificEntry> entry : overlay.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null) {
                SpecificEntry merged = mergeSpecificEntry(base.get(entry.getKey()), entry.getValue());
                base.put(entry.getKey(), merged);
            }
        }
        return base;
    }

    private static SpecificEntry mergeSpecificEntry(SpecificEntry base, SpecificEntry overlay) {
        if (overlay == null) {
            return base;
        }
        if (base == null) {
            return overlay;
        }
        if (overlay.kind != null) {
            base.kind = overlay.kind;
        }
        if (overlay.armKind != null) {
            base.armKind = overlay.armKind;
        }
        if (overlay.renamed != null) {
            base.renamed = overlay.renamed;
        }
        if (overlay.model != null) {
            base.model = overlay.model;
        }
        if (overlay.customModelData != null) {
            base.customModelData = overlay.customModelData;
        }
        if (overlay.physics != null && !overlay.physics.isJsonNull()) {
            base.physics = overlay.physics;
        }
        if (overlay.disableSprintLateralTilt != null) {
            base.disableSprintLateralTilt = overlay.disableSprintLateralTilt;
        }
        if (overlay.keepArmTuning != null) {
            base.keepArmTuning = overlay.keepArmTuning;
        }
        if (overlay.keepTriggerArmTuning != null) {
            base.keepTriggerArmTuning = overlay.keepTriggerArmTuning;
        }
        if (overlay.keepArmTuningL != null) {
            base.keepArmTuningL = overlay.keepArmTuningL;
        }
        if (overlay.keepArmTuningR != null) {
            base.keepArmTuningR = overlay.keepArmTuningR;
        }
        if (overlay.disableWalkOnWater != null) {
            base.disableWalkOnWater = overlay.disableWalkOnWater;
        }
        if (overlay.disableDryOut != null) {
            base.disableDryOut = overlay.disableDryOut;
        }
        if (overlay.disableSwiming != null) {
            base.disableSwiming = overlay.disableSwiming;
        }
        if (overlay.animation != null && !overlay.animation.isEmpty()) {
            base.animation = overlay.animation;
        }
        base.customAnimation = mergeCustomAnimationEntryLists(base.customAnimation, overlay.customAnimation);
        base.customItemAnimations = mergeCustomAnimationEntryLists(base.customItemAnimations, overlay.customItemAnimations);
        if (overlay.particleEmitters != null && !overlay.particleEmitters.isJsonNull()) {
            base.particleEmitters = overlay.particleEmitters;
        }
        if (overlay.particle != null && !overlay.particle.isJsonNull()) {
            base.particle = overlay.particle;
        }
        if (overlay.particles != null && !overlay.particles.isJsonNull()) {
            base.particles = overlay.particles;
        }
        if (overlay.particleEffects != null && !overlay.particleEffects.isJsonNull()) {
            base.particleEffects = overlay.particleEffects;
        }
        if (overlay.particleEmission != null && !overlay.particleEmission.isJsonNull()) {
            base.particleEmission = overlay.particleEmission;
        }
        if (overlay.betterCombat != null && !overlay.betterCombat.isEmpty()) {
            base.betterCombat = overlay.betterCombat;
        }
        base.transforms = mergeSideSet(base.transforms, overlay.transforms);
        base.armTransforms = mergeSideSet(base.armTransforms, overlay.armTransforms);
        return base;
    }

    private static class_2960 resolveSpecificItemId(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        String trimmed = rawKey.trim();
        class_2960 direct = class_2960.method_12829(trimmed);
        if (direct != null) {
            return direct;
        }
        int cut = -1;
        int hash = trimmed.indexOf(35);
        int pipe = trimmed.indexOf(124);
        int at = trimmed.indexOf(64);
        if (hash > 0) {
            cut = hash;
        }
        if (pipe > 0 && (cut < 0 || pipe < cut)) {
            cut = pipe;
        }
        if (at > 0 && (cut < 0 || at < cut)) {
            cut = at;
        }
        if (cut <= 0) {
            return null;
        }
        return class_2960.method_12829(trimmed.substring(0, cut).trim());
    }

    private static boolean specificKeyTargetsItem(String rawKey, class_2960 itemId) {
        class_2960 parsed;
        return (itemId == null || (parsed = resolveSpecificItemId(rawKey)) == null || !parsed.equals(itemId)) ? false : true;
    }

    private static Data finalizeCompatData(String profile, Data data) {
        return data;
    }

    private static Path getCompatDirectory() {
        Path configDir = getConfigDirectory();
        return configDir.resolve(COMPAT_DIR_NAME);
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private static void resetAllTunings() throws MatchException {
        ToolTuning.resetDefaults();
        ArmToolTuning.resetDefaults();
        ArmMeshTuning.resetDefaults();
        for (ChestProxyTuning.Kind kind : ChestProxyTuning.Kind.values()) {
            ChestProxyTuning.reset(kind);
        }
        for (FluidProxyTuning.Kind kind2 : FluidProxyTuning.Kind.values()) {
            FluidProxyTuning.reset(kind2);
        }
        for (EnchantParticleProxyTuning.Kind kind3 : EnchantParticleProxyTuning.Kind.values()) {
            EnchantParticleProxyTuning.reset(kind3, false);
            EnchantParticleProxyTuning.reset(kind3, true);
        }
        for (PaddleProxyTuning.Kind kind4 : PaddleProxyTuning.Kind.values()) {
            PaddleProxyTuning.reset(kind4, false);
            PaddleProxyTuning.reset(kind4, true);
        }
        for (FishingLineProxyTuning.Kind kind5 : FishingLineProxyTuning.Kind.values()) {
            FishingLineProxyTuning.reset(kind5, false);
            FishingLineProxyTuning.reset(kind5, true);
        }
    }

    private static Path getConfigDirectory() {
        class_310 client = class_310.method_1551();
        if (client != null) {
            return client.field_1697.toPath().resolve("config");
        }
        return Path.of("config", new String[0]);
    }

    private static void installBundledCompatProfiles() {
        for (String profile : BUNDLED_COMPAT_PROFILES) {
            copyBundledCompatProfile(profile);
        }
    }

    private static void copyBundledCompatProfile(String profile) {
        try {
            InputStream stream = openBundledCompatStream(profile);
            if (stream != null) {
                try {
                    Path compatDir = getCompatDirectory();
                    Files.createDirectories(compatDir, new FileAttribute[0]);
                    Path target = compatDir.resolve(profile + ".json");
                    Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                    if (stream != null) {
                        stream.close();
                    }
                    return;
                } finally {
                }
            }
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e) {
        }
    }

    private static InputStream openBundledCompatStream(String profile) {
        return null;
    }
}
