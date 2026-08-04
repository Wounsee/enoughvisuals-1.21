package punchy.client.animation;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_1109;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import net.minecraft.class_3414;
import net.minecraft.class_3532;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import punchy.Punchy;
import punchy.client.PunchyClient;
import punchy.client.animation.data.AnimationClip;
import punchy.client.animation.io.PunchyAnimationLoader;
import punchy.client.render.UseAnimationSuppressor;
import punchy.client.state.ConsumeStateMachine;
import punchy.client.state.HandEquipStateMachine;
import punchy.config.PunchyConfig;
import punchy.config.PunchyTuningConfig;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyAnimationManager.class */
public class PunchyAnimationManager {
    private static final String EFFECT_DUAL_HANDED = "dual_handed";
    private static final String EFFECT_DUAL_HANDED_IF_HAS_OFF_ITEM = "dual_handed_if_has_off_item";
    private static final String EFFECT_START_WITH_OFFHAND = "start_here_if_off_hand_item";
    private static final String EFFECT_START_WITH_OFFHAND_ALIAS = "start_if_offhand_item";
    private static final String EFFECT_STOP_WITH_OFFHAND = "stop_if_offhand_item";
    public static AnimationClip PUNCH_CLIP;
    public static AnimationClip INTERACT_CLIP;
    public static AnimationClip USE_BOW_RIGHT_CLIP;
    public static AnimationClip USE_BOW_LEFT_CLIP;
    public static AnimationClip ATTACK_CLIP;
    public static AnimationClip ATTACK_SPEAR_CLIP;
    public static AnimationClip ATTACK_TRIDENT_CLIP;
    public static AnimationClip ATTACK_MACE_CLIP;
    public static AnimationClip USE_SPEAR_POSE_1;
    public static AnimationClip USE_SPEAR_POSE_2;
    public static AnimationClip USE_SPEAR_POSE_3_RIGHT;
    public static AnimationClip USE_SPEAR_POSE_3_LEFT;
    public static AnimationClip USE_SHIELD_RIGHT_CLIP;
    public static AnimationClip USE_SHIELD_LEFT_CLIP;
    public static AnimationClip USE_CROSSBOW_RIGHT_CLIP;
    public static AnimationClip USE_CROSSBOW_LEFT_CLIP;
    public static AnimationClip USE_CROSSBOW_SHOT_CLIP;
    public static AnimationClip USE_TRIDENT_CLIP;
    public static AnimationClip USE_TRIDENT_THROW_CLIP;
    public static AnimationClip USE_EAT_CLIP;
    public static AnimationClip USE_DRINK_CLIP;
    public static AnimationClip SWIMING_CLIP;
    public static AnimationClip SWIMING_ABOVE_CLIP;
    public static AnimationClip ROWING_CLIP;
    public static AnimationClip ROWING_BACK_CLIP;
    public static AnimationClip FALLING_IN_WATER_CLIP;
    public static AnimationClip USE_TRIDENT_THROW_LOYALTY_CLIP;
    public static AnimationClip USE_TRIDENT_THROW_RIPTIDE_CLIP;
    public static AnimationClip MINING_FORWARD_CLIP;
    public static AnimationClip MINING_SIDEWAYS_CLIP;
    public static AnimationClip MINING_DIGGING_CLIP;
    public static AnimationClip MINING_USE_FORWARD_CLIP;
    public static AnimationClip MINING_USE_DIGGING_CLIP;
    public static AnimationClip PUT_OUT_FIRE_CLIP;
    public static AnimationClip HAND_IN_CLIP;
    public static AnimationClip HAND_OUT_CLIP;
    public static AnimationClip WAKE_UP_CLIP;
    private static class_1306 activeSourceArm;
    private static AnimationClip lastSoundClip;
    private static float lastSoundTime;
    private static boolean suppressOffhandForAttack;
    private static AnimationClip suppressOffhandClip;
    private static final String LEFT_SWITCH_PREFIX = "left_switch_item_";
    private static final String RIGHT_SWITCH_PREFIX = "right_switch_item_";
    private static final String HIDE_LEFT_SWITCH_PREFIX = "hide_left_switch_item_";
    private static final String HIDE_RIGHT_SWITCH_PREFIX = "hide_right_switch_item_";
    private static final String SHOW_LEFT_SWITCH = "show_left_switch_item";
    private static final String SHOW_RIGHT_SWITCH = "show_right_switch_item";
    private static final String SHOW_LEFT_SWITCH_PREFIX = "show_left_switch_item_";
    private static final String SHOW_RIGHT_SWITCH_PREFIX = "show_right_switch_item_";
    private static final String HIDE_ITEM_EFFECT = "hide_item";
    private static final long MIX_RELOAD_DEBOUNCE_NANOS = 150000000;
    public static final PoseHandler POSE_HANDLER = new PoseHandler();
    public static final PoseHandler SWIM_HANDLER = new PoseHandler();
    public static float ANIMATION_SPEED = 5.5f;
    private static final EnumMap<class_1306, ItemOverrideState> ITEM_OVERRIDES = new EnumMap<>(class_1306.class);
    private static final EnumMap<class_1306, EquipOverrideState> EQUIP_OVERRIDES = new EnumMap<>(class_1306.class);
    public static boolean SOUND_ENABLED = false;
    private static final Map<String, AnimationClip> ALL_CLIPS = new HashMap();
    private static final Map<String, LinkedHashMap<String, AnimationClip>> ALL_CLIP_VARIANTS = new HashMap();
    private static final Map<String, List<String>> TYPE_CLIP_ALIASES = buildTypeClipAliases();
    private static final Map<String, ClipResolution> CLIP_RESOLUTION_CACHE = new HashMap();
    private static boolean MIX_RELOAD_PENDING = false;
    private static long MIX_RELOAD_DEADLINE_NANOS = 0;

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyAnimationManager$PackOption.class */
    public static final class PackOption extends Record {
        private final String optionId;
        private final String label;

        public PackOption(String optionId, String label) {
            this.optionId = optionId;
            this.label = label;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, PackOption.class), PackOption.class, "optionId;label", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->label:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, PackOption.class), PackOption.class, "optionId;label", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->label:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, PackOption.class, Object.class), PackOption.class, "optionId;label", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$PackOption;->label:Ljava/lang/String;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public String optionId() {
            return this.optionId;
        }

        public String label() {
            return this.label;
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyAnimationManager$ClipResolution.class */
    public static final class ClipResolution extends Record {
        private final AnimationClip clip;
        private final String optionId;
        private final String note;

        public ClipResolution(AnimationClip clip, String optionId, String note) {
            this.clip = clip;
            this.optionId = optionId;
            this.note = note;
        }

        @Override // java.lang.Record
        public final String toString() {
            return (String) ObjectMethods.bootstrap(MethodHandles.lookup(), "toString", MethodType.methodType(String.class, ClipResolution.class), ClipResolution.class, "clip;optionId;note", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->note:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final int hashCode() {
            return (int) ObjectMethods.bootstrap(MethodHandles.lookup(), "hashCode", MethodType.methodType(Integer.TYPE, ClipResolution.class), ClipResolution.class, "clip;optionId;note", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->note:Ljava/lang/String;").dynamicInvoker().invoke(this) /* invoke-custom */;
        }

        @Override // java.lang.Record
        public final boolean equals(Object o) {
            return (boolean) ObjectMethods.bootstrap(MethodHandles.lookup(), "equals", MethodType.methodType(Boolean.TYPE, ClipResolution.class, Object.class), ClipResolution.class, "clip;optionId;note", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->clip:Lpunchy/client/animation/data/AnimationClip;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->optionId:Ljava/lang/String;", "FIELD:Lpunchy/client/animation/PunchyAnimationManager$ClipResolution;->note:Ljava/lang/String;").dynamicInvoker().invoke(this, o) /* invoke-custom */;
        }

        public AnimationClip clip() {
            return this.clip;
        }

        public String optionId() {
            return this.optionId;
        }

        public String note() {
            return this.note;
        }
    }

    private static Map<String, List<String>> buildTypeClipAliases() {
        Map<String, List<String>> aliases = new HashMap<>();
        aliases.put(normalizeTypeKey("attack"), List.of("attack"));
        aliases.put(normalizeTypeKey("mining"), List.of("mining", "mining_foward", "mining_forward", "mining_sideways", "mining_digging"));
        aliases.put(normalizeTypeKey("mining_v2"), List.of("mining", "mining_foward", "mining_forward", "mining_sideways", "mining_digging"));
        aliases.put(normalizeTypeKey("useItem"), List.of("use_item", "useitem", "interaction", "interact", "mining_use_foward", "mining_use_forward", "mining_use_digging"));
        aliases.put(normalizeTypeKey("interaction"), List.of("interaction", "interact", "use_item", "useitem"));
        aliases.put(normalizeTypeKey("spear_charge"), List.of("use_spear_pose_1", "use_spear_pose_1_left", "use_spear_pose_1_right", "use_spear_pose_2", "use_spear_pose_2_left", "use_spear_pose_2_right", "use_spear_pose_3", "use_spear_pose_3_left", "use_spear_pose_3_right"));
        aliases.put(normalizeTypeKey("inspect"), List.of("inspect"));
        aliases.put(normalizeTypeKey("hand_in"), List.of("hand_in", "handin"));
        aliases.put(normalizeTypeKey("hand_out"), List.of("hand_out", "handout"));
        aliases.put(normalizeTypeKey("falling"), List.of("falling"));
        aliases.put(normalizeTypeKey("triggerable"), List.of("triggerable"));
        aliases.put(normalizeTypeKey("use_shield"), List.of("use_shield"));
        aliases.put(normalizeTypeKey("use_bow"), List.of("use_bow"));
        aliases.put(normalizeTypeKey("use_crossbow"), List.of("use_crossbow"));
        aliases.put(normalizeTypeKey("use_crossbow_shot"), List.of("use_crossbow_shot"));
        aliases.put(normalizeTypeKey("useEat"), List.of("use_eat", "useeat"));
        aliases.put(normalizeTypeKey("useDrink"), List.of("use_drink", "usedrink"));
        aliases.put(normalizeTypeKey("idle"), List.of("idle"));
        return aliases;
    }

    public static void loadAnimations(class_3300 manager) {
        ALL_CLIPS.clear();
        ALL_CLIP_VARIANTS.clear();
        CLIP_RESOLUTION_CACHE.clear();
        SOUND_ENABLED = false;
        lastSoundClip = null;
        lastSoundTime = 0.0f;
        POSE_HANDLER.stopImmediateWithBlendOut();
        SWIM_HANDLER.stopImmediateWithBlendOut();
        Map<String, LinkedHashMap<String, AnimationClip>> loadedVariants = PunchyAnimationLoader.loadAllWithPackVariants(manager);
        for (Map.Entry<String, LinkedHashMap<String, AnimationClip>> entry : loadedVariants.entrySet()) {
            if (entry != null && entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                LinkedHashMap<String, AnimationClip> variants = new LinkedHashMap<>();
                for (Map.Entry<String, AnimationClip> variant : entry.getValue().entrySet()) {
                    if (variant != null && variant.getKey() != null && variant.getValue() != null) {
                        if (!variant.getValue().getSoundEffects().isEmpty()) {
                            SOUND_ENABLED = true;
                        }
                        variants.put(variant.getKey(), AnimationClip.copyOf(variant.getValue()));
                    }
                }
                if (!variants.isEmpty()) {
                    ALL_CLIP_VARIANTS.put(entry.getKey(), variants);
                    AnimationClip merged = variants.get(PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID);
                    if (merged == null) {
                        merged = variants.values().iterator().next();
                    }
                    if (merged != null) {
                        ALL_CLIPS.put(entry.getKey(), AnimationClip.copyOf(merged));
                    }
                }
            }
        }
        assignKnownClips();
    }

    public static List<String> getMixPackAnimationNames() {
        return PunchyTuningConfig.getMixPackSlotIds();
    }

    public static String getMixPackEntryLabel(String entryId) {
        return PunchyTuningConfig.getMixPackSlotLabel(entryId);
    }

    public static List<PackOption> getMixPackOptions(String animationName) {
        List<String> optionIds = PunchyTuningConfig.getMixPackSlotOptionIds(animationName);
        if (optionIds == null || optionIds.isEmpty()) {
            return List.of();
        }
        List<PackOption> options = new ArrayList<>();
        for (String optionId : optionIds) {
            if (!PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(optionId)) {
                options.add(new PackOption(optionId, packOptionLabel(optionId)));
            }
        }
        return options;
    }

    public static String getSelectedMixPackOption(String animationName) {
        return PunchyTuningConfig.getMixPackSelectedOption(animationName);
    }

    public static boolean selectMixPackOption(String animationName, String optionId) {
        boolean changed = PunchyTuningConfig.setMixPackSelection(animationName, optionId);
        if (!changed) {
            return false;
        }
        scheduleMixReload();
        return true;
    }

    public static List<String> getMixPackEntryTypes(String animationName) {
        return PunchyTuningConfig.getMixPackSlotEntryTypes(animationName);
    }

    public static List<String> getMixPackExpandableEntryTypes(String animationName) {
        return PunchyTuningConfig.getMixPackExpandableEntryTypes(animationName);
    }

    public static List<PackOption> getMixPackTypeOptions(String animationName, String entryType) {
        List<String> optionIds = PunchyTuningConfig.getMixPackSlotEntryTypeOptionIds(animationName, entryType);
        if (optionIds == null) {
            return List.of();
        }
        List<String> normalizedOptionIds = new ArrayList<>(optionIds);
        String selectedMain = getSelectedMixPackOption(animationName);
        if (selectedMain != null && !selectedMain.isBlank() && !normalizedOptionIds.contains(selectedMain)) {
            normalizedOptionIds.add(0, selectedMain);
        }
        if (normalizedOptionIds.isEmpty()) {
            return List.of();
        }
        List<PackOption> options = new ArrayList<>();
        for (String optionId : normalizedOptionIds) {
            if (!PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(optionId)) {
                options.add(new PackOption(optionId, packOptionLabel(optionId)));
            }
        }
        return options;
    }

    public static String getSelectedMixPackTypeOption(String animationName, String entryType) {
        return PunchyTuningConfig.getMixPackSelectedEntryTypeOption(animationName, entryType);
    }

    public static boolean hasExplicitMixPackTypeOption(String animationName, String entryType) {
        return PunchyTuningConfig.hasMixPackEntryTypeOverride(animationName, entryType);
    }

    public static boolean selectMixPackTypeOption(String animationName, String entryType, String optionId) {
        boolean changed = PunchyTuningConfig.setMixPackEntryTypeSelection(animationName, entryType, optionId);
        if (!changed) {
            return false;
        }
        scheduleMixReload();
        return true;
    }

    public static void resetMixPacksToResourcePackOrder() {
        PunchyTuningConfig.resetMixPackSelectionsToResourcepackOrder();
        scheduleMixReload();
    }

    public static void flushPendingMixReload(class_310 client) {
        if (!MIX_RELOAD_PENDING) {
            return;
        }
        long now = System.nanoTime();
        if (now < MIX_RELOAD_DEADLINE_NANOS) {
            return;
        }
        applyMixReloadNow(client);
    }

    public static void requestTuningReload() {
        scheduleMixReload();
    }

    private static void scheduleMixReload() {
        MIX_RELOAD_PENDING = true;
        MIX_RELOAD_DEADLINE_NANOS = System.nanoTime() + MIX_RELOAD_DEBOUNCE_NANOS;
    }

    private static void applyMixReloadNow(class_310 client) {
        MIX_RELOAD_PENDING = false;
        MIX_RELOAD_DEADLINE_NANOS = 0L;
        if (client != null && client.method_1478() != null) {
            PunchyTuningConfig.reloadActiveProfileFast(client.method_1478());
        } else {
            PunchyTuningConfig.reloadActiveProfileFast(null);
        }
        rebuildActiveClipsFromSelections();
    }

    private static void rebuildActiveClipsFromSelections() {
        CLIP_RESOLUTION_CACHE.clear();
        assignKnownClips();
    }

    private static void assignKnownClips() {
        PUNCH_CLIP = ALL_CLIPS.get("punch");
        INTERACT_CLIP = ALL_CLIPS.get("interact");
        USE_BOW_RIGHT_CLIP = ALL_CLIPS.get("use_bow_right");
        USE_BOW_LEFT_CLIP = ALL_CLIPS.get("use_bow_left");
        ATTACK_CLIP = ALL_CLIPS.get("attack");
        ATTACK_SPEAR_CLIP = ALL_CLIPS.get("attack_spear");
        ATTACK_TRIDENT_CLIP = ALL_CLIPS.get("attack_trident");
        ATTACK_MACE_CLIP = ALL_CLIPS.get("attack_mace");
        USE_SPEAR_POSE_1 = ALL_CLIPS.get("use_spear_pose_1");
        USE_SPEAR_POSE_2 = ALL_CLIPS.get("use_spear_pose_2");
        USE_SPEAR_POSE_3_RIGHT = ALL_CLIPS.get("use_spear_pose_3_right");
        USE_SPEAR_POSE_3_LEFT = ALL_CLIPS.get("use_spear_pose_3_left");
        USE_SHIELD_RIGHT_CLIP = ALL_CLIPS.get("use_shield_right");
        USE_SHIELD_LEFT_CLIP = ALL_CLIPS.get("use_shield_left");
        USE_CROSSBOW_RIGHT_CLIP = ALL_CLIPS.get("use_crossbow_right");
        USE_CROSSBOW_LEFT_CLIP = ALL_CLIPS.get("use_crossbow_left");
        USE_CROSSBOW_SHOT_CLIP = ALL_CLIPS.get("use_crossbow_shot");
        USE_TRIDENT_CLIP = ALL_CLIPS.get("use_trident");
        USE_TRIDENT_THROW_CLIP = ALL_CLIPS.get("use_trident_throw");
        USE_EAT_CLIP = ALL_CLIPS.get("use_eat");
        USE_DRINK_CLIP = ALL_CLIPS.get("use_drink");
        SWIMING_CLIP = ALL_CLIPS.get("swiming");
        SWIMING_ABOVE_CLIP = ALL_CLIPS.get("swiming_above");
        ROWING_CLIP = ALL_CLIPS.get("rowing");
        ROWING_BACK_CLIP = ALL_CLIPS.get("rowing_back");
        FALLING_IN_WATER_CLIP = ALL_CLIPS.get("falling_in_water");
        USE_TRIDENT_THROW_LOYALTY_CLIP = ALL_CLIPS.get("use_trident_throw_loyalty");
        USE_TRIDENT_THROW_RIPTIDE_CLIP = ALL_CLIPS.get("use_trident_throw_riptide");
        MINING_FORWARD_CLIP = ALL_CLIPS.get("mining_foward");
        MINING_SIDEWAYS_CLIP = ALL_CLIPS.get("mining_sideways");
        MINING_DIGGING_CLIP = ALL_CLIPS.get("mining_digging");
        MINING_USE_FORWARD_CLIP = ALL_CLIPS.get("mining_use_foward");
        MINING_USE_DIGGING_CLIP = ALL_CLIPS.get("mining_use_digging");
        PUT_OUT_FIRE_CLIP = ALL_CLIPS.get("put_out_fire");
        HAND_IN_CLIP = ALL_CLIPS.get("hand_in");
        HAND_OUT_CLIP = ALL_CLIPS.get("hand_out");
        WAKE_UP_CLIP = ALL_CLIPS.get("wake_up");
    }

    private static String normalizeOptionId(String optionId) {
        if (optionId == null || optionId.isBlank()) {
            return null;
        }
        return optionId.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeTypeKey(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        if ("miningv2".equals(normalized) || "mining".equals(normalized)) {
            return "mining";
        }
        return normalized;
    }

    private static String packOptionLabel(String optionId) {
        if (optionId == null || optionId.isBlank()) {
            return "Unknown";
        }
        if ("pack:punchy:punchy".equals(optionId)) {
            return "§bPunchy! §eDefault";
        }
        if (PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(optionId)) {
            return "§7Default";
        }
        String cleaned = optionId;
        if (cleaned.startsWith("pack:")) {
            cleaned = cleaned.substring("pack:".length());
        }
        if (cleaned.startsWith("file/")) {
            cleaned = cleaned.substring("file/".length());
        }
        if (cleaned.startsWith("file\\")) {
            cleaned = cleaned.substring("file\\".length());
        }
        if (cleaned.endsWith(".zip")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        if (cleaned.endsWith(".jar")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        if (cleaned.isBlank()) {
            return "Unknown";
        }
        String[] parts = cleaned.replace('_', ' ').replace('-', ' ').replace('/', ' ').replace('\\', ' ').trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank() && !"file".equalsIgnoreCase(part)) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                String lowered = part.toLowerCase(Locale.ROOT);
                builder.append(Character.toUpperCase(lowered.charAt(0)));
                if (lowered.length() > 1) {
                    builder.append(lowered.substring(1));
                }
            }
        }
        return builder.isEmpty() ? "Unknown" : builder.toString();
    }

    private static AnimationClip resolveClipByOption(String clipName, String preferredOptionId) {
        return resolveClipByOptionWithSource(clipName, preferredOptionId).clip();
    }

    public static AnimationClip resolveCustomClip(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim();
        return ALL_CLIPS.get(key);
    }

    public static AnimationClip resolveHandedClip(String baseName, class_310 client, class_1268 hand) {
        return resolveHandedClip(baseName, client, hand, null);
    }

    public static AnimationClip resolveHandedClip(String baseName, class_310 client, class_1268 hand, String preferredOptionId) {
        if (baseName == null || baseName.isBlank()) {
            return null;
        }
        String preferred = preferredOptionId;
        if (preferred == null || preferred.isBlank()) {
            preferred = inferPreferredOptionFromHand(client, hand);
        }
        String key = baseName.trim();
        AnimationClip base = resolveClipByOption(key, preferred);
        AnimationClip right = resolveClipByOption(key + "_right", preferred);
        AnimationClip left = resolveClipByOption(key + "_left", preferred);
        boolean hasSuffixed = (right == null && left == null) ? false : true;
        if (hasSuffixed) {
            if (client == null || client.field_1724 == null || hand == null) {
                return base != null ? base : right != null ? right : left;
            }
            boolean useRight = (hand == class_1268.field_5808) == (client.field_1724.method_6068() == class_1306.field_6183);
            if (useRight) {
                if (right != null) {
                    return right;
                }
                if (base != null) {
                    return base;
                }
                return left;
            }
            if (left != null) {
                return left;
            }
            if (base != null) {
                return base;
            }
            return right;
        }
        return base;
    }

    public static AnimationClip resolveNamedClip(String name, class_310 client, class_1268 hand) {
        return resolveNamedClip(name, client, hand, null);
    }

    public static AnimationClip resolveNamedClip(String name, class_310 client, class_1268 hand, String preferredOptionId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String preferred = preferredOptionId;
        if (preferred == null || preferred.isBlank()) {
            preferred = inferPreferredOptionFromHand(client, hand);
        }
        String key = name.trim();
        if (key.endsWith("_left") || key.endsWith("_right")) {
            return resolveClipByOption(key, preferred);
        }
        return resolveHandedClip(key, client, hand, preferred);
    }

    public static ClipResolution resolveNamedClipWithSource(String name, class_310 client, class_1268 hand, String preferredOptionId) {
        if (name == null || name.isBlank()) {
            return new ClipResolution(null, null, "name_blank");
        }
        String preferred = preferredOptionId;
        if (preferred == null || preferred.isBlank()) {
            preferred = inferPreferredOptionFromHand(client, hand);
        }
        String key = name.trim();
        if (key.endsWith("_left") || key.endsWith("_right")) {
            return resolveClipByOptionWithSource(key, preferred);
        }
        return resolveHandedClipWithSource(key, client, hand, preferred);
    }

    public static ClipResolution resolveHandedClipWithSource(String baseName, class_310 client, class_1268 hand, String preferredOptionId) {
        if (baseName == null || baseName.isBlank()) {
            return new ClipResolution(null, null, "base_blank");
        }
        String preferred = preferredOptionId;
        if (preferred == null || preferred.isBlank()) {
            preferred = inferPreferredOptionFromHand(client, hand);
        }
        String key = baseName.trim();
        ClipResolution base = resolveClipByOptionWithSource(key, preferred);
        ClipResolution right = resolveClipByOptionWithSource(key + "_right", preferred);
        ClipResolution left = resolveClipByOptionWithSource(key + "_left", preferred);
        boolean hasSuffixed = (right.clip() == null && left.clip() == null) ? false : true;
        if (hasSuffixed) {
            if (client == null || client.field_1724 == null || hand == null) {
                if (base.clip() != null) {
                    return new ClipResolution(base.clip(), base.optionId(), "suffixed_no_context_base:" + base.note());
                }
                if (right.clip() != null) {
                    return new ClipResolution(right.clip(), right.optionId(), "suffixed_no_context_right:" + right.note());
                }
                if (left.clip() != null) {
                    return new ClipResolution(left.clip(), left.optionId(), "suffixed_no_context_left:" + left.note());
                }
                return new ClipResolution(null, null, "suffixed_no_context_missing");
            }
            boolean useRight = (hand == class_1268.field_5808) == (client.field_1724.method_6068() == class_1306.field_6183);
            if (useRight) {
                if (right.clip() != null) {
                    return right;
                }
                if (base.clip() != null) {
                    return new ClipResolution(base.clip(), base.optionId(), "suffixed_main_fallback_base:" + base.note());
                }
                if (left.clip() != null) {
                    return new ClipResolution(left.clip(), left.optionId(), "suffixed_main_fallback_left:" + left.note());
                }
                return new ClipResolution(null, null, "suffixed_main_missing");
            }
            if (left.clip() != null) {
                return left;
            }
            if (base.clip() != null) {
                return new ClipResolution(base.clip(), base.optionId(), "suffixed_off_fallback_base:" + base.note());
            }
            if (right.clip() != null) {
                return new ClipResolution(right.clip(), right.optionId(), "suffixed_off_fallback_right:" + right.note());
            }
            return new ClipResolution(null, null, "suffixed_off_missing");
        }
        return base;
    }

    public static AnimationClip resolveTypeFallbackClip(String type, class_310 client, class_1268 hand) {
        return resolveTypeFallbackClip(type, client, hand, null);
    }

    public static AnimationClip resolveTypeFallbackClip(String type, class_310 client, class_1268 hand, String preferredOptionId) {
        return resolveTypeFallbackClipWithSource(type, client, hand, preferredOptionId).clip();
    }

    public static ClipResolution resolveTypeFallbackClipWithSource(String type, class_310 client, class_1268 hand, String preferredOptionId) {
        ClipResolution resolution;
        List<String> candidates = TYPE_CLIP_ALIASES.get(normalizeTypeKey(type));
        if (candidates == null || candidates.isEmpty()) {
            return new ClipResolution(null, null, "type_unmapped");
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && (resolution = resolveNamedClipWithSource(candidate, client, hand, preferredOptionId)) != null && resolution.clip() != null) {
                return new ClipResolution(resolution.clip(), resolution.optionId(), "type_fallback:" + candidate + ":" + resolution.note());
            }
        }
        return new ClipResolution(null, null, "type_missing");
    }

    public static boolean hasTypeFallbackClip(String type, String preferredOptionId) {
        List<String> candidates = TYPE_CLIP_ALIASES.get(normalizeTypeKey(type));
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (hasNamedOrHandedClip(candidate, preferredOptionId)) {
                return true;
            }
        }
        return false;
    }

    private static ClipResolution resolveClipByOptionWithSource(String clipName, String preferredOptionId) {
        ClipResolution resolved;
        if (clipName == null || clipName.isBlank()) {
            return new ClipResolution(null, null, "clip_blank");
        }
        String normalized = normalizeOptionId(preferredOptionId);
        String cacheKey = clipName + "|" + (normalized == null ? "__auto__" : normalized);
        ClipResolution cached = CLIP_RESOLUTION_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        LinkedHashMap<String, AnimationClip> variants = ALL_CLIP_VARIANTS.get(clipName);
        if (normalized != null && !normalized.isBlank() && !PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID.equals(normalized)) {
            AnimationClip preferred = variants == null ? null : variants.get(normalized);
            if (preferred != null) {
                resolved = new ClipResolution(preferred, normalized, "preferred_option_variant");
            } else {
                AnimationClip fallback = ALL_CLIPS.get(clipName);
                resolved = new ClipResolution(fallback, fallback == null ? null : PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID, fallback == null ? "preferred_option_missing" : "preferred_option_fallback_default");
            }
        } else {
            AnimationClip clip = ALL_CLIPS.get(clipName);
            resolved = new ClipResolution(clip, clip == null ? null : PunchyAnimationLoader.DEFAULT_PACK_OPTION_ID, clip == null ? "clip_missing" : "resource_order_default");
        }
        CLIP_RESOLUTION_CACHE.put(cacheKey, resolved);
        return resolved;
    }

    private static boolean hasNamedOrHandedClip(String clipName, String preferredOptionId) {
        if (clipName == null || clipName.isBlank()) {
            return false;
        }
        return (resolveClipByOption(clipName, preferredOptionId) == null && resolveClipByOption(clipName + "_left", preferredOptionId) == null && resolveClipByOption(clipName + "_right", preferredOptionId) == null) ? false : true;
    }

    private static String inferPreferredOptionFromHand(class_310 client, class_1268 hand) {
        if (client == null || client.field_1724 == null || hand == null) {
            return null;
        }
        class_1799 stack = client.field_1724.method_5998(hand);
        return PunchyTuningConfig.getMixPreferredOptionForEntry(stack, null);
    }

    public static void logMixSelectionTrace(String context, String key, String clipName, String preferredOptionId, ClipResolution resolution) {
    }

    public static boolean isClipNamed(AnimationClip clip, String baseName) {
        if (clip == null || baseName == null || baseName.isBlank()) {
            return false;
        }
        String name = clip.getName();
        String key = baseName.trim();
        return key.equals(name) || (key + "_left").equals(name) || (key + "_right").equals(name);
    }

    public static boolean isHandBlacklisted(class_1306 arm) {
        class_310 mc;
        if (arm == null || (mc = class_310.method_1551()) == null || mc.field_1724 == null) {
            return false;
        }
        class_1268 hand = getHandForArm(mc, arm);
        return PunchyConfig.isHandBlacklisted(mc.field_1724, hand);
    }

    public static boolean isHandBlacklisted(class_310 client, class_1268 hand) {
        if (client == null || client.field_1724 == null) {
            return false;
        }
        class_1268 resolved = hand == null ? class_1268.field_5808 : hand;
        return PunchyConfig.isHandBlacklisted(client.field_1724, resolved);
    }

    public static boolean isAnyHandBlacklisted() {
        return isHandBlacklisted(class_1306.field_6183) || isHandBlacklisted(class_1306.field_6182);
    }

    public static boolean shouldBlockClip(AnimationClip clip, class_1306 activeArm) {
        if (clip == null) {
            return false;
        }
        if (activeArm != null && isHandBlacklisted(activeArm)) {
            return true;
        }
        if (!isDualHandedActive(clip, resolveDualHandedEffectTime(clip)) || !isAnyHandBlacklisted()) {
            return false;
        }
        class_1306 sourceArm = getSourceArm();
        if (sourceArm == null) {
            return true;
        }
        return isHandBlacklisted(sourceArm);
    }

    public static void enforceBlacklist(class_310 client) {
        AnimationClip clip;
        if (client == null || client.field_1724 == null || (clip = POSE_HANDLER.getCurrentClip()) == null || !POSE_HANDLER.isPlaying()) {
            return;
        }
        class_1306 arm = POSE_HANDLER.getActiveArm();
        if (shouldBlockClip(clip, arm)) {
            POSE_HANDLER.setLooping(false);
            POSE_HANDLER.stopImmediateWithBlendOut();
        }
    }

    private static class_1799 getStackForArm(class_1657 player, class_1306 arm) {
        if (player == null || arm == null) {
            return class_1799.field_8037;
        }
        boolean mainRight = player.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? player.method_6047() : player.method_6079() : mainRight ? player.method_6079() : player.method_6047();
    }

    public static void setAnimationSpeed(float speed) {
        ANIMATION_SPEED = class_3532.method_15363(speed, 0.5f, 7.0f);
    }

    public static boolean isDualHandedActive() {
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (clip == null) {
            return false;
        }
        float time = POSE_HANDLER.getCurrentTime();
        return isDualHandedActive(clip, time);
    }

    public static boolean hasAnyDualHandedEffect(AnimationClip clip) {
        return clip != null && (clip.hasEffectAny(EFFECT_DUAL_HANDED) || clip.hasEffectAny(EFFECT_DUAL_HANDED_IF_HAS_OFF_ITEM));
    }

    public static boolean isDualHandedActive(AnimationClip clip, float time) {
        return isDualHandedActive(class_310.method_1551(), clip, time);
    }

    public static boolean isDualHandedActive(class_310 client, AnimationClip clip, float time) {
        if (clip == null) {
            return false;
        }
        if (clip.isEffectActive(EFFECT_DUAL_HANDED, time)) {
            return isConditionalOffhandWindowActive(client, clip, time);
        }
        return hasDualHandedOffhandContext(client) && clip.isEffectActive(EFFECT_DUAL_HANDED_IF_HAS_OFF_ITEM, time);
    }

    private static boolean isConditionalOffhandWindowActive(class_310 client, AnimationClip clip, float time) {
        if (clip == null) {
            return false;
        }
        if (!hasOffhandItem(client)) {
            return true;
        }
        float lastStart = resolveLastOffhandStartTime(clip, time);
        float lastStop = clip.getLastEffectTime(EFFECT_STOP_WITH_OFFHAND, time);
        boolean hasStart = !Float.isNaN(lastStart);
        boolean hasStop = !Float.isNaN(lastStop);
        if (hasStart || hasStop) {
            return !hasStart ? !hasStop : !hasStop || lastStart > lastStop;
        }
        return true;
    }

    private static float resolveLastOffhandStartTime(AnimationClip clip, float time) {
        if (clip == null) {
            return Float.NaN;
        }
        float canonical = clip.getLastEffectTime(EFFECT_START_WITH_OFFHAND, time);
        float alias = clip.getLastEffectTime(EFFECT_START_WITH_OFFHAND_ALIAS, time);
        if (Float.isNaN(canonical)) {
            return alias;
        }
        if (Float.isNaN(alias)) {
            return canonical;
        }
        return Math.max(canonical, alias);
    }

    public static boolean isSwimDualHandedActive() {
        AnimationClip clip;
        if (!SWIM_HANDLER.isPlaying() || SWIM_HANDLER.getActiveArm() != null || (clip = SWIM_HANDLER.getCurrentClip()) == null) {
            return false;
        }
        return isDualHandedActive(clip, SWIM_HANDLER.getCurrentTime());
    }

    public static boolean isOffhandHiddenIfEmptyActive() {
        return POSE_HANDLER.isOffhandHiddenIfEmptyActive() || SWIM_HANDLER.isOffhandHiddenIfEmptyActive();
    }

    public static boolean isOffhandHiddenIfEmptyActive(float partialTicks) {
        return POSE_HANDLER.isOffhandHiddenIfEmptyActive(partialTicks) || SWIM_HANDLER.isOffhandHiddenIfEmptyActive(partialTicks);
    }

    public static boolean shouldKeepBlacklistedOppositeHandForDualHanded(class_310 client, class_1268 hand) {
        class_1306 sourceArm;
        if (client == null || client.field_1724 == null || hand == null || !PunchyConfig.isHandBlacklisted(client.field_1724, hand) || !POSE_HANDLER.isPlaying()) {
            return false;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        return (!isDualHandedActive(clip, POSE_HANDLER.getCurrentTime()) || (sourceArm = getSourceArm()) == null || getHandForArm(client, sourceArm) == hand) ? false : true;
    }

    public static void updateUseMeshSuppression(class_310 client) {
        if (client == null || client.field_1724 == null) {
            UseAnimationSuppressor.clear();
            return;
        }
        if (client.field_1724.method_6115() && PunchyConfig.isHandBlacklisted(client.field_1724, client.field_1724.method_6058())) {
            UseAnimationSuppressor.clear();
            UseAnimationSuppressor.force(false);
            return;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (clip == null) {
            UseAnimationSuppressor.clear();
            return;
        }
        class_1268 activeHand = resolveMeshSuppressorHand(client);
        if (activeHand == null) {
            UseAnimationSuppressor.clear();
            return;
        }
        class_1268 oppositeHand = activeHand == class_1268.field_5808 ? class_1268.field_5810 : class_1268.field_5808;
        UseAnimationSuppressor.clear(oppositeHand);
        if (isClipNamed(clip, "use_shield") || isClipNamed(clip, "use_trident")) {
            UseAnimationSuppressor.suppress(activeHand);
            return;
        }
        float time = POSE_HANDLER.getCurrentTime();
        boolean hasTrigger = clip.hasEffectAny("start_mesh_animation") || clip.hasEffectAny("loadstart") || clip.hasEffectAny("load_start") || clip.hasEffectAny("suppress_mesh_animation");
        if (!hasTrigger) {
            UseAnimationSuppressor.clear();
            return;
        }
        float lastSuppress = getLastEffectTime(clip, "suppress_mesh_animation", time);
        float lastStart = Math.max(getLastEffectTime(clip, "start_mesh_animation", time), Math.max(getLastEffectTime(clip, "loadstart", time), getLastEffectTime(clip, "load_start", time)));
        if (lastSuppress > lastStart) {
            UseAnimationSuppressor.suppress(activeHand);
            return;
        }
        boolean started = lastStart >= 0.0f;
        if (!started) {
            UseAnimationSuppressor.suppress(activeHand);
            return;
        }
        int remaining = client.field_1724.method_6115() ? client.field_1724.method_6014() : 0;
        if (client.field_1724.method_6115()) {
            class_1799 useStack = client.field_1724.method_6030();
            if (!useStack.method_7960()) {
                remaining = useStack.method_7935(client.field_1724);
            }
        }
        UseAnimationSuppressor.release(activeHand, client.field_1687.method_8510(), remaining);
    }

    private static class_1268 resolveMeshSuppressorHand(class_310 client) {
        if (client == null || client.field_1724 == null) {
            return null;
        }
        if (client.field_1724.method_6115()) {
            return client.field_1724.method_6058();
        }
        if (activeSourceArm == null) {
            return null;
        }
        return getHandForArm(client, activeSourceArm);
    }

    private static float getLastEffectTime(AnimationClip clip, String effect, float time) {
        if (clip == null || effect == null || effect.isEmpty()) {
            return Float.NEGATIVE_INFINITY;
        }
        if (clip.hasEffect(effect)) {
            return Float.POSITIVE_INFINITY;
        }
        float lastTime = Float.NEGATIVE_INFINITY;
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffectsUpTo(time)) {
            if (entry.getValue().contains(effect)) {
                lastTime = entry.getKey().floatValue();
            }
        }
        return lastTime;
    }

    public static void updateItemOverrides(class_310 client) {
        if (client == null || client.field_1724 == null) {
            clearItemOverrides();
            return;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (clip == null || !POSE_HANDLER.isPlaying()) {
            clearItemOverrides();
            return;
        }
        float time = POSE_HANDLER.getCurrentTime();
        ItemOverrideState left = new ItemOverrideState();
        ItemOverrideState right = new ItemOverrideState();
        applyItemEffectsOrdered(clip.getEffects(), left, right);
        for (Map.Entry<Float, Set<String>> entry : clip.getTimelineEffectsUpTo(time)) {
            applyItemEffectsOrdered(entry.getValue(), left, right);
        }
        ITEM_OVERRIDES.put(class_1306.field_6183, right);
        ITEM_OVERRIDES.put(class_1306.field_6182, left);
    }

    public static void updateSoundEffects(class_310 client) {
        if (!SOUND_ENABLED) {
            lastSoundClip = null;
            lastSoundTime = 0.0f;
            return;
        }
        if (client == null) {
            lastSoundClip = null;
            lastSoundTime = 0.0f;
            return;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (clip == null || !POSE_HANDLER.isPlaying()) {
            lastSoundClip = null;
            lastSoundTime = 0.0f;
            return;
        }
        float currentTime = POSE_HANDLER.getCurrentTime();
        if (clip != lastSoundClip) {
            lastSoundClip = clip;
            lastSoundTime = 0.0f;
            playSoundsAtTime(clip, 0.0f, client);
        }
        if (currentTime < lastSoundTime) {
            playSoundsBetween(clip, 0.0f, currentTime, client);
        } else {
            playSoundsBetween(clip, lastSoundTime, currentTime, client);
        }
        lastSoundTime = currentTime;
    }

    public static class_1799 resolveRenderStack(class_1657 player, class_1306 arm, class_1799 baseStack) {
        class_1799 visualOffhand;
        if (player == null || arm == null) {
            return baseStack;
        }
        ItemOverrideState override = ITEM_OVERRIDES.get(arm);
        boolean forceShow = override != null && override.forceShow;
        if (player.method_6115()) {
            class_1268 usedHand = player.method_6058();
            class_1306 usedArm = usedHand == class_1268.field_5808 ? player.method_6068() : opposite(player.method_6068());
            if (usedArm == arm) {
                return baseStack;
            }
        }
        class_1799 forcedConsumeStack = ConsumeStateMachine.getForcedRenderStack(arm);
        if (forcedConsumeStack != null && !forcedConsumeStack.method_7960()) {
            return forcedConsumeStack;
        }
        if (!forceShow && PunchyClient.shouldSuppressOffhandRender(player)) {
            if (arm == (player.method_6068() == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183)) {
                return class_1799.field_8037;
            }
        }
        class_1306 offArm = player.method_6068() == class_1306.field_6183 ? class_1306.field_6182 : class_1306.field_6183;
        if (arm == offArm && HandEquipStateMachine.shouldHideOffhandTridentThrowCacheRender()) {
            return class_1799.field_8037;
        }
        if (arm == offArm && HandEquipStateMachine.isOffhandEquipTransitionActive() && (visualOffhand = HandEquipStateMachine.getVisualOffhandStack((class_746) player)) != null && !visualOffhand.method_7960()) {
            return visualOffhand;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if ((isClipNamed(clip, "use_trident_throw") || isClipNamed(clip, "use_trident_throw_loyalty") || isClipNamed(clip, "use_trident_throw_riptide")) && activeSourceArm != null && arm == activeSourceArm) {
            return class_1799.field_8037;
        }
        if (override != null && override.hidden) {
            return class_1799.field_8037;
        }
        if (override != null && !override.stack.method_7960()) {
            return override.stack;
        }
        EquipOverrideState equipOverride = EQUIP_OVERRIDES.get(arm);
        return (equipOverride == null || !equipOverride.active) ? shouldHideBaseItem(player, arm) ? class_1799.field_8037 : baseStack : equipOverride.stack;
    }

    private static boolean shouldHideBaseItem(class_1657 player, class_1306 arm) {
        if (player == null || arm == null) {
            return false;
        }
        ItemOverrideState override = ITEM_OVERRIDES.get(arm);
        if ((override != null && override.forceShow) || !isDualHandedActive() || activeSourceArm == null) {
            return false;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        return (isClipNamed(clip, "use_shield") || arm == activeSourceArm) ? false : true;
    }

    public static void setSourceHand(class_310 client, class_1268 hand) {
        activeSourceArm = getUseArm(client, hand);
    }

    public static void clearSourceHand() {
        activeSourceArm = null;
    }

    public static class_1306 getSourceArm() {
        return activeSourceArm;
    }

    public static void syncSourceHandToActiveClip() {
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (!POSE_HANDLER.isPlaying() || !isDualHandedActive(clip, POSE_HANDLER.getCurrentTime())) {
            activeSourceArm = null;
        }
    }

    private static boolean hasOffhandItem(class_310 client) {
        return (client == null || client.field_1724 == null || client.field_1724.method_6079() == null || client.field_1724.method_6079().method_7960()) ? false : true;
    }

    private static boolean hasDualHandedOffhandContext(class_310 client) {
        if (hasOffhandItem(client)) {
            return true;
        }
        if (client == null) {
            return false;
        }
        class_746 localPlayer = client.field_1724;
        if (localPlayer instanceof class_746) {
            return PunchyClient.shouldSuppressOffhandRender(localPlayer) || PunchyClient.shouldLowerOffhand(localPlayer) || HandEquipStateMachine.shouldKeepOffhandVisibleUntilHandOutEnds() || HandEquipStateMachine.isOffhandEquipTransitionActive();
        }
        return false;
    }

    private static float resolveDualHandedEffectTime(AnimationClip clip) {
        if (clip == null) {
            return 0.0f;
        }
        if (POSE_HANDLER.getCurrentClip() == clip && POSE_HANDLER.isPlaying()) {
            return POSE_HANDLER.getCurrentTime();
        }
        if (SWIM_HANDLER.getCurrentClip() == clip && SWIM_HANDLER.isPlaying()) {
            return SWIM_HANDLER.getCurrentTime();
        }
        return 0.0f;
    }

    public static void setSuppressOffhandForAttack(AnimationClip clip, boolean enable) {
        if (enable && clip != null) {
            suppressOffhandForAttack = true;
            suppressOffhandClip = clip;
        } else {
            suppressOffhandForAttack = false;
            suppressOffhandClip = null;
        }
    }

    public static boolean shouldSuppressOffhandForAttack() {
        if (!suppressOffhandForAttack) {
            return false;
        }
        if (!POSE_HANDLER.isPlaying()) {
            suppressOffhandForAttack = false;
            suppressOffhandClip = null;
            return false;
        }
        AnimationClip clip = POSE_HANDLER.getCurrentClip();
        if (clip == null || clip != suppressOffhandClip) {
            suppressOffhandForAttack = false;
            suppressOffhandClip = null;
            return false;
        }
        return true;
    }

    public static boolean isItemForceShown(class_1306 arm) {
        ItemOverrideState state;
        return (arm == null || (state = ITEM_OVERRIDES.get(arm)) == null || !state.forceShow) ? false : true;
    }

    private static void clearItemOverrides() {
        ITEM_OVERRIDES.clear();
    }

    private static void playSoundsBetween(AnimationClip clip, float start, float end, class_310 client) {
        if (clip == null || client == null) {
            return;
        }
        for (Map.Entry<Float, Set<String>> entry : clip.getSoundEffectsBetween(start, end).entrySet()) {
            for (String name : entry.getValue()) {
                playAnimationSound(name, client);
            }
        }
    }

    private static void playSoundsAtTime(AnimationClip clip, float time, class_310 client) {
        if (clip == null || client == null) {
            return;
        }
        for (String name : clip.getSoundEffectsAt(time)) {
            playAnimationSound(name, client);
        }
    }

    private static void playAnimationSound(String name, class_310 client) {
        if (name == null || name.isBlank() || client == null) {
            return;
        }
        String clean = name.trim();
        class_2960 fileId = class_2960.method_60655(Punchy.MOD_ID, "sounds/animationssounds/" + clean + ".ogg");
        try {
            if (client.method_1478().method_14486(fileId).isEmpty()) {
                return;
            }
            class_2960 soundId = class_2960.method_60655(Punchy.MOD_ID, "animationssounds/" + clean);
            class_3414 event = class_3414.method_47908(soundId);
            client.method_1483().method_4873(class_1109.method_4758(event, 1.0f));
        } catch (Exception e) {
        }
    }

    public static void setEquipOverride(class_1306 arm, class_1799 stack) {
        if (arm == null) {
            return;
        }
        EquipOverrideState state = (EquipOverrideState) EQUIP_OVERRIDES.computeIfAbsent(arm, key -> {
            return new EquipOverrideState();
        });
        state.active = true;
        state.stack = stack == null ? class_1799.field_8037 : stack;
    }

    public static void clearEquipOverride(class_1306 arm) {
        EquipOverrideState state;
        if (arm == null || (state = EQUIP_OVERRIDES.get(arm)) == null) {
            return;
        }
        state.active = false;
        state.stack = class_1799.field_8037;
    }

    public static void clearAllEquipOverrides() {
        for (class_1306 arm : class_1306.values()) {
            clearEquipOverride(arm);
        }
    }

    public static boolean isEquipOverrideActive(class_1306 arm) {
        EquipOverrideState state;
        return (arm == null || (state = EQUIP_OVERRIDES.get(arm)) == null || !state.active) ? false : true;
    }

    public static boolean hasRenderableEquipOverride(class_1306 arm) {
        EquipOverrideState state;
        return (arm == null || (state = EQUIP_OVERRIDES.get(arm)) == null || !state.active || state.stack == null || state.stack.method_7960()) ? false : true;
    }

    private static void applyItemEffectsOrdered(Iterable<String> effects, ItemOverrideState left, ItemOverrideState right) {
        if (effects == null) {
            return;
        }
        for (String effect : effects) {
            if (effect != null && !effect.isBlank()) {
                String normalized = effect.toLowerCase(Locale.ROOT).trim();
                if (normalized.startsWith(LEFT_SWITCH_PREFIX) || normalized.startsWith(RIGHT_SWITCH_PREFIX)) {
                    applyItemEffect(normalized, left, right);
                }
            }
        }
        for (String effect2 : effects) {
            if (effect2 != null && !effect2.isBlank()) {
                String normalized2 = effect2.toLowerCase(Locale.ROOT).trim();
                if (normalized2.startsWith(HIDE_LEFT_SWITCH_PREFIX) || normalized2.startsWith(HIDE_RIGHT_SWITCH_PREFIX)) {
                    applyItemEffect(normalized2, left, right);
                }
                if (normalized2.equals(SHOW_LEFT_SWITCH) || normalized2.equals(SHOW_RIGHT_SWITCH) || normalized2.startsWith(SHOW_LEFT_SWITCH_PREFIX) || normalized2.startsWith(SHOW_RIGHT_SWITCH_PREFIX)) {
                    applyItemEffect(normalized2, left, right);
                }
                if (HIDE_ITEM_EFFECT.equals(normalized2)) {
                    applyHideItemEffect(left, right);
                }
            }
        }
    }

    private static void applyItemEffect(String effect, ItemOverrideState left, ItemOverrideState right) {
        if (effect == null || effect.isBlank()) {
            return;
        }
        String normalized = effect.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith(LEFT_SWITCH_PREFIX)) {
            left.stack = resolveItemStack(normalized.substring(LEFT_SWITCH_PREFIX.length()));
            left.hidden = false;
            left.forceShow = true;
            return;
        }
        if (normalized.startsWith(RIGHT_SWITCH_PREFIX)) {
            right.stack = resolveItemStack(normalized.substring(RIGHT_SWITCH_PREFIX.length()));
            right.hidden = false;
            right.forceShow = true;
            return;
        }
        if (normalized.startsWith(HIDE_LEFT_SWITCH_PREFIX)) {
            left.stack = class_1799.field_8037;
            left.hidden = true;
            left.forceShow = false;
            return;
        }
        if (normalized.startsWith(HIDE_RIGHT_SWITCH_PREFIX)) {
            right.stack = class_1799.field_8037;
            right.hidden = true;
            right.forceShow = false;
            return;
        }
        if (normalized.equals(SHOW_LEFT_SWITCH)) {
            left.stack = class_1799.field_8037;
            left.hidden = false;
            left.forceShow = true;
            return;
        }
        if (normalized.equals(SHOW_RIGHT_SWITCH)) {
            right.stack = class_1799.field_8037;
            right.hidden = false;
            right.forceShow = true;
            return;
        }
        if (normalized.startsWith(SHOW_LEFT_SWITCH_PREFIX)) {
            String suffix = normalized.substring(SHOW_LEFT_SWITCH_PREFIX.length()).trim();
            if (!suffix.isBlank() && !"*".equals(suffix)) {
                left.stack = resolveItemStack(suffix);
            } else {
                left.stack = class_1799.field_8037;
            }
            left.hidden = false;
            left.forceShow = true;
            return;
        }
        if (normalized.startsWith(SHOW_RIGHT_SWITCH_PREFIX)) {
            String suffix2 = normalized.substring(SHOW_RIGHT_SWITCH_PREFIX.length()).trim();
            if (!suffix2.isBlank() && !"*".equals(suffix2)) {
                right.stack = resolveItemStack(suffix2);
            } else {
                right.stack = class_1799.field_8037;
            }
            right.hidden = false;
            right.forceShow = true;
        }
    }

    private static void applyHideItemEffect(ItemOverrideState left, ItemOverrideState right) {
        if (activeSourceArm == null) {
            return;
        }
        if (activeSourceArm == class_1306.field_6183) {
            left.stack = class_1799.field_8037;
            left.hidden = true;
        } else {
            right.stack = class_1799.field_8037;
            right.hidden = true;
        }
    }

    private static class_1799 resolveItemStack(String itemId) {
        class_1792 item;
        if (itemId == null || itemId.isBlank()) {
            return class_1799.field_8037;
        }
        class_2960 id = class_2960.method_12829(itemId.trim());
        if (id != null && (item = (class_1792) class_7923.field_41178.method_17966(id).orElse(class_1802.field_8162)) != class_1802.field_8162) {
            return new class_1799(item);
        }
        return class_1799.field_8037;
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyAnimationManager$ItemOverrideState.class */
    private static final class ItemOverrideState {
        class_1799 stack = class_1799.field_8037;
        boolean hidden;
        boolean forceShow;

        private ItemOverrideState() {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/client/animation/PunchyAnimationManager$EquipOverrideState.class */
    private static final class EquipOverrideState {
        class_1799 stack = class_1799.field_8037;
        boolean active;

        private EquipOverrideState() {
        }
    }

    public static float clipSpeedMultiplier(AnimationClip clip) {
        if (clip == null) {
            return 1.0f;
        }
        if (isClipNamed(clip, "use_bow") || isClipNamed(clip, "use_shield") || isClipNamed(clip, "use_crossbow") || isClipNamed(clip, "use_crossbow_shot")) {
            return 1.0f / Math.max(0.01f, ANIMATION_SPEED);
        }
        if (isClipNamed(clip, "use_trident") || isClipNamed(clip, "use_trident_throw") || isClipNamed(clip, "use_trident_throw_loyalty") || isClipNamed(clip, "use_trident_throw_riptide") || isClipNamed(clip, "attack_mace")) {
            return 1.0f / Math.max(0.01f, ANIMATION_SPEED);
        }
        if (isClipNamed(clip, "use_eat") || isClipNamed(clip, "use_drink")) {
            return 1.0f;
        }
        if (clip == SWIMING_CLIP) {
            return 1.0f / Math.max(0.01f, ANIMATION_SPEED);
        }
        if (isClipNamed(clip, "falling_in_water")) {
            return 1.0f;
        }
        if (isClipNamed(clip, "dry_out")) {
            return 1.0f / Math.max(0.01f, ANIMATION_SPEED);
        }
        if (isClipNamed(clip, "hand_in") || isClipNamed(clip, "hand_out")) {
            return 1.0f / Math.max(0.01f, ANIMATION_SPEED);
        }
        if (isClipNamed(clip, "interact")) {
            return 5.4f / Math.max(0.01f, ANIMATION_SPEED);
        }
        return 1.0f;
    }

    public static class_1306 getMiningArm() {
        class_310 mc = class_310.method_1551();
        return mc.field_1724 == null ? class_1306.field_6183 : mc.field_1724.method_6068();
    }

    public static class_1306 getUseArm(class_310 mc, class_1268 hand) {
        return (mc == null || mc.field_1724 == null) ? class_1306.field_6183 : hand == class_1268.field_5810 ? opposite(mc.field_1724.method_6068()) : mc.field_1724.method_6068();
    }

    public static class_1268 getHandForArm(class_310 mc, class_1306 arm) {
        if (mc == null || mc.field_1724 == null || arm == null) {
            return class_1268.field_5808;
        }
        boolean mainRight = mc.field_1724.method_6068() == class_1306.field_6183;
        return arm == class_1306.field_6183 ? mainRight ? class_1268.field_5808 : class_1268.field_5810 : mainRight ? class_1268.field_5810 : class_1268.field_5808;
    }

    private static class_1306 opposite(class_1306 arm) {
        return arm == class_1306.field_6182 ? class_1306.field_6183 : class_1306.field_6182;
    }
}
