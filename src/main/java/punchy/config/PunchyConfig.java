package punchy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_7923;
import punchy.Punchy;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyConfig.class */
public final class PunchyConfig {
    private static final String FILE_NAME = "punchy_config.json";
    public static final float ANIMATION_SPEED_DEFAULT = 5.5f;
    public static final float ANIMATION_SPEED_MIN = 0.5f;
    public static final float ANIMATION_SPEED_MAX = 7.0f;
    public static final float FIRSTPERSON_HIDE_PITCH_DEFAULT = 60.0f;
    public static final float FIRSTPERSON_HIDE_PITCH_MIN = 0.0f;
    public static final float FIRSTPERSON_HIDE_PITCH_MAX = 90.0f;
    public static final float FIRSTPERSON_HIDE_PITCH_NEVER_THRESHOLD = 85.0f;
    public static final float SPRINT_ARM_SWING_INTENSITY_DEFAULT = 1.0f;
    public static final float SPRINT_ARM_SWING_INTENSITY_MIN = -1.0f;
    public static final float SPRINT_ARM_SWING_INTENSITY_MAX = 1.0f;
    public static final float SPRINT_ARM_SWING_SPEED_DEFAULT = 0.0f;
    public static final float SPRINT_ARM_SWING_SPEED_MIN = -1.0f;
    public static final float SPRINT_ARM_SWING_SPEED_MAX = 1.0f;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data DATA = new Data();

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/config/PunchyConfig$Data.class */
    public static final class Data {
        public boolean renderArmorArmsFP = true;
        public boolean interactiveStuffCompat = false;

        @SerializedName(value = "bettercombatCompat", alternate = {"BetterCombatCompat", "betterCombatCompat"})
        public boolean bettercombatCompat = false;

        @SerializedName(value = "swordBlockingCompat", alternate = {"SwordBlockingCompat", "swordblockingCompat"})
        public boolean swordBlockingCompat = false;
        public float animationSpeed = 5.5f;
        public boolean enableMod = true;
        public boolean enableTuning = false;
        public boolean disableResourcePackModelParts = false;
        public boolean disableArmPhysics = false;
        public boolean disableNativeItemPhysics = false;
        public boolean disableBoatMinecartRaftModels = false;
        public boolean disablePistonModels = false;
        public boolean disableChestModels = false;
        public boolean disableEnchantingTableModels = false;
        public boolean disableBoatFirstPersonAnimations = false;
        public boolean disableEnhancedFireArmEffects = false;
        public boolean enableCrawlAnimation = true;
        public boolean enableClimbAnimation = true;
        public boolean enableSwimAnimation = true;
        public boolean enableElytraFlightAnimation = true;
        public boolean enableCustomWalk = true;
        public boolean enableSprintArmSwing = true;
        public boolean enableFallArmAnimation = true;
        public float sprintArmSwingIntensity = 1.0f;
        public float sprintArmSwingSpeed = 0.0f;

        @SerializedName(value = "enableFreezeShake", alternate = {"disableFreezeShake"})
        public boolean enableFreezeShake = true;

        @SerializedName(value = "enableFreezeArmOverlay", alternate = {"disableFreezeArmOverlay"})
        public boolean enableFreezeArmOverlay = true;
        public boolean enableMud = true;
        public boolean enableSweat = true;
        public boolean enableBurn = true;
        public boolean enableParticles = true;
        public boolean enableModelPartsGlow = false;
        public boolean enableBlurGlowAtDark = false;
        public boolean firstPersonModelHideEnabled = true;
        public float firstPersonModelHidePitch = 60.0f;
        public List<String> itemBlacklist = new ArrayList();
        public Map<String, Boolean> blacklistApplyDualHanded = new LinkedHashMap();
        public Map<String, String> animationPackSelections = new LinkedHashMap();
        public Map<String, String> animationPackTypeSelections = new LinkedHashMap();
        public Map<String, Boolean> mixSpecificForceOverride = new LinkedHashMap();
    }

    private PunchyConfig() {
    }

    public static void load() {
        Path dir = getConfigDirectory().resolve(Punchy.MOD_ID);
        Path file = dir.resolve(FILE_NAME);
        Data loaded = null;
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
            if (Files.exists(file, new LinkOption[0])) {
                FileReader reader = new FileReader(file.toFile());
                try {
                    loaded = (Data) GSON.fromJson(reader, Data.class);
                    reader.close();
                } finally {
                }
            }
        } catch (Exception e) {
        }
        if (loaded == null) {
            loaded = new Data();
            write(file, loaded);
        }
        DATA = sanitize(loaded);
    }

    public static boolean renderArmorArmsFP() {
        return DATA != null && DATA.renderArmorArmsFP;
    }

    public static boolean isInteractiveStuffCompatEnabled() {
        return DATA != null && DATA.interactiveStuffCompat;
    }

    public static boolean isBetterCombatCompatEnabled() {
        return DATA != null && DATA.bettercombatCompat;
    }

    public static boolean isSwordBlockingCompatEnabled() {
        return DATA != null && DATA.swordBlockingCompat;
    }

    public static float getAnimationSpeed() {
        if (DATA == null) {
            return 5.5f;
        }
        return DATA.animationSpeed;
    }

    public static float getFirstPersonModelHidePitch() {
        if (DATA == null) {
            return 60.0f;
        }
        return DATA.firstPersonModelHidePitch;
    }

    public static boolean isFirstPersonModelHideNever() {
        return getFirstPersonModelHidePitch() > 85.0f;
    }

    public static boolean isFirstPersonModelHideEnabled() {
        return DATA == null || DATA.firstPersonModelHideEnabled;
    }

    public static boolean isModEnabled() {
        return DATA == null || DATA.enableMod;
    }

    public static boolean isTuningEnabled() {
        return DATA != null && DATA.enableTuning;
    }

    public static boolean isItemBlacklisted(class_1799 stack) {
        return (DATA == null || DATA.itemBlacklist == null || stack == null || stack.method_7960() || findBlacklistMatch(stack) == null) ? false : true;
    }

    public static boolean isItemBlacklistDualHanded(class_1799 stack) {
        String match;
        if (DATA == null || DATA.blacklistApplyDualHanded == null || stack == null || stack.method_7960() || (match = findBlacklistMatch(stack)) == null) {
            return false;
        }
        return Boolean.TRUE.equals(DATA.blacklistApplyDualHanded.get(match));
    }

    public static boolean isHandBlacklisted(class_1657 player, class_1268 hand) {
        if (player == null) {
            return false;
        }
        class_1268 resolved = hand == null ? class_1268.field_5808 : hand;
        class_1799 handStack = player.method_5998(resolved);
        if (isItemBlacklisted(handStack)) {
            return true;
        }
        class_1268 opposite = resolved == class_1268.field_5808 ? class_1268.field_5810 : class_1268.field_5808;
        class_1799 oppositeStack = player.method_5998(opposite);
        return isItemBlacklistDualHanded(oppositeStack);
    }

    private static boolean matchesBlacklistEntry(String entry, String key, String namespace) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        String trimmed = entry.trim().toLowerCase(Locale.ROOT);
        if (trimmed.endsWith(":")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return false;
        }
        boolean hasWildcard = trimmed.indexOf(42) >= 0 || trimmed.indexOf(63) >= 0;
        boolean hasNamespace = trimmed.indexOf(58) >= 0;
        if (hasNamespace) {
            if (!hasWildcard) {
                return key.equals(trimmed);
            }
            return globMatch(trimmed, key);
        }
        if (!hasWildcard) {
            return namespace.equals(trimmed);
        }
        return globMatch(trimmed, namespace);
    }

    private static boolean globMatch(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }
        StringBuilder regex = new StringBuilder(pattern.length() * 2);
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '$':
                    regex.append("\\$");
                    break;
                case '(':
                    regex.append("\\(");
                    break;
                case ')':
                    regex.append("\\)");
                    break;
                case '*':
                    regex.append(".*");
                    break;
                case '+':
                    regex.append("\\+");
                    break;
                case '.':
                    regex.append("\\.");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '[':
                    regex.append("\\[");
                    break;
                case '\\':
                    regex.append("\\\\");
                    break;
                case ']':
                    regex.append("\\]");
                    break;
                case '^':
                    regex.append("\\^");
                    break;
                case '{':
                    regex.append("\\{");
                    break;
                case '|':
                    regex.append("\\|");
                    break;
                case '}':
                    regex.append("\\}");
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        return value.matches(regex.toString());
    }

    public static boolean disableResourcePackModelParts() {
        return DATA != null && DATA.disableResourcePackModelParts;
    }

    public static boolean disableArmPhysics() {
        return DATA != null && DATA.disableArmPhysics;
    }

    public static boolean disableNativeItemPhysics() {
        return DATA != null && DATA.disableNativeItemPhysics;
    }

    public static boolean disableBoatMinecartRaftModels() {
        return DATA != null && (DATA.disableBoatMinecartRaftModels || DATA.interactiveStuffCompat);
    }

    public static boolean disablePistonModels() {
        return DATA != null && (DATA.disablePistonModels || DATA.interactiveStuffCompat);
    }

    public static boolean disableChestModels() {
        return DATA != null && (DATA.disableChestModels || DATA.interactiveStuffCompat);
    }

    public static boolean disableEnchantingTableModels() {
        return DATA != null && (DATA.disableEnchantingTableModels || DATA.interactiveStuffCompat);
    }

    public static boolean disableBoatFirstPersonAnimations() {
        return DATA != null && DATA.disableBoatFirstPersonAnimations;
    }

    public static boolean disableEnhancedFireArmEffects() {
        return DATA != null && DATA.disableEnhancedFireArmEffects;
    }

    public static boolean enableCrawlAnimation() {
        return DATA != null && DATA.enableCrawlAnimation;
    }

    public static boolean enableClimbAnimation() {
        return DATA != null && DATA.enableClimbAnimation;
    }

    public static boolean enableSwimAnimation() {
        return DATA != null && DATA.enableSwimAnimation;
    }

    public static boolean enableElytraFlightAnimation() {
        return DATA == null || DATA.enableElytraFlightAnimation;
    }

    public static boolean enableCustomWalk() {
        return DATA == null || DATA.enableCustomWalk;
    }

    public static boolean enableSprintArmSwing() {
        return DATA == null || DATA.enableSprintArmSwing;
    }

    public static boolean enableFallArmAnimation() {
        return DATA == null || DATA.enableFallArmAnimation;
    }

    public static float getSprintArmSwingIntensity() {
        if (DATA == null) {
            return 1.0f;
        }
        return DATA.sprintArmSwingIntensity;
    }

    public static float getSprintArmSwingSpeed() {
        if (DATA == null) {
            return 0.0f;
        }
        return DATA.sprintArmSwingSpeed;
    }

    public static boolean enableFreezeShake() {
        return DATA == null || DATA.enableFreezeShake;
    }

    public static boolean enableFreezeArmOverlay() {
        return DATA == null || DATA.enableFreezeArmOverlay;
    }

    public static boolean enableMud() {
        return DATA == null || DATA.enableMud;
    }

    public static boolean enableSweat() {
        return false;
    }

    public static boolean enableBurn() {
        return DATA == null || DATA.enableBurn;
    }

    public static boolean enableParticles() {
        return DATA == null || DATA.enableParticles;
    }

    public static boolean enableModelPartsGlow() {
        return DATA != null && DATA.enableModelPartsGlow;
    }

    public static boolean enableBlurGlowAtDark() {
        return DATA != null && DATA.enableBlurGlowAtDark;
    }

    public static Data snapshot() {
        LinkedHashMap linkedHashMap;
        LinkedHashMap linkedHashMap2;
        LinkedHashMap linkedHashMap3;
        LinkedHashMap linkedHashMap4;
        Data copy = new Data();
        if (DATA != null) {
            copy.renderArmorArmsFP = DATA.renderArmorArmsFP;
            copy.interactiveStuffCompat = DATA.interactiveStuffCompat;
            copy.bettercombatCompat = DATA.bettercombatCompat;
            copy.swordBlockingCompat = DATA.swordBlockingCompat;
            copy.animationSpeed = DATA.animationSpeed;
            copy.enableMod = DATA.enableMod;
            copy.enableTuning = DATA.enableTuning;
            copy.disableResourcePackModelParts = DATA.disableResourcePackModelParts;
            copy.disableArmPhysics = DATA.disableArmPhysics;
            copy.disableNativeItemPhysics = DATA.disableNativeItemPhysics;
            copy.disableBoatMinecartRaftModels = DATA.disableBoatMinecartRaftModels;
            copy.disablePistonModels = DATA.disablePistonModels;
            copy.disableChestModels = DATA.disableChestModels;
            copy.disableEnchantingTableModels = DATA.disableEnchantingTableModels;
            copy.disableBoatFirstPersonAnimations = DATA.disableBoatFirstPersonAnimations;
            copy.disableEnhancedFireArmEffects = DATA.disableEnhancedFireArmEffects;
            copy.enableCrawlAnimation = DATA.enableCrawlAnimation;
            copy.enableClimbAnimation = DATA.enableClimbAnimation;
            copy.enableSwimAnimation = DATA.enableSwimAnimation;
            copy.enableElytraFlightAnimation = DATA.enableElytraFlightAnimation;
            copy.enableCustomWalk = DATA.enableCustomWalk;
            copy.enableSprintArmSwing = DATA.enableSprintArmSwing;
            copy.enableFallArmAnimation = DATA.enableFallArmAnimation;
            copy.sprintArmSwingIntensity = DATA.sprintArmSwingIntensity;
            copy.sprintArmSwingSpeed = DATA.sprintArmSwingSpeed;
            copy.enableFreezeShake = DATA.enableFreezeShake;
            copy.enableFreezeArmOverlay = DATA.enableFreezeArmOverlay;
            copy.enableMud = DATA.enableMud;
            copy.enableSweat = DATA.enableSweat;
            copy.enableBurn = DATA.enableBurn;
            copy.enableParticles = DATA.enableParticles;
            copy.enableModelPartsGlow = DATA.enableModelPartsGlow;
            copy.enableBlurGlowAtDark = DATA.enableBlurGlowAtDark;
            copy.firstPersonModelHideEnabled = DATA.firstPersonModelHideEnabled;
            copy.firstPersonModelHidePitch = DATA.firstPersonModelHidePitch;
            copy.itemBlacklist = DATA.itemBlacklist == null ? new ArrayList() : new ArrayList(DATA.itemBlacklist);
            if (DATA.blacklistApplyDualHanded == null) {
                linkedHashMap = new LinkedHashMap();
            } else {
                linkedHashMap = new LinkedHashMap(DATA.blacklistApplyDualHanded);
            }
            copy.blacklistApplyDualHanded = linkedHashMap;
            if (DATA.animationPackSelections == null) {
                linkedHashMap2 = new LinkedHashMap();
            } else {
                linkedHashMap2 = new LinkedHashMap(DATA.animationPackSelections);
            }
            copy.animationPackSelections = linkedHashMap2;
            if (DATA.animationPackTypeSelections == null) {
                linkedHashMap3 = new LinkedHashMap();
            } else {
                linkedHashMap3 = new LinkedHashMap(DATA.animationPackTypeSelections);
            }
            copy.animationPackTypeSelections = linkedHashMap3;
            if (DATA.mixSpecificForceOverride == null) {
                linkedHashMap4 = new LinkedHashMap();
            } else {
                linkedHashMap4 = new LinkedHashMap(DATA.mixSpecificForceOverride);
            }
            copy.mixSpecificForceOverride = linkedHashMap4;
        }
        return copy;
    }

    public static String getAnimationPackSelection(String animationName) {
        if (animationName == null || animationName.isBlank() || DATA == null || DATA.animationPackSelections == null) {
            return null;
        }
        return DATA.animationPackSelections.get(animationName);
    }

    public static void setAnimationPackSelection(String animationName, String optionId) {
        if (animationName == null || animationName.isBlank()) {
            return;
        }
        if (DATA == null) {
            DATA = new Data();
        }
        if (DATA.animationPackSelections == null) {
            DATA.animationPackSelections = new LinkedHashMap();
        }
        if (optionId == null || optionId.isBlank()) {
            DATA.animationPackSelections.remove(animationName);
        } else {
            DATA.animationPackSelections.put(animationName, optionId.trim().toLowerCase(Locale.ROOT));
        }
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static String getAnimationPackTypeSelection(String slotId, String typeId) {
        if (slotId == null || slotId.isBlank() || typeId == null || typeId.isBlank() || DATA == null || DATA.animationPackTypeSelections == null) {
            return null;
        }
        return DATA.animationPackTypeSelections.get(slotId + "|" + typeId.trim().toLowerCase(Locale.ROOT));
    }

    public static void setAnimationPackTypeSelection(String slotId, String typeId, String optionId) {
        if (slotId == null || slotId.isBlank() || typeId == null || typeId.isBlank()) {
            return;
        }
        if (DATA == null) {
            DATA = new Data();
        }
        if (DATA.animationPackTypeSelections == null) {
            DATA.animationPackTypeSelections = new LinkedHashMap();
        }
        String key = slotId + "|" + typeId.trim().toLowerCase(Locale.ROOT);
        if (optionId == null || optionId.isBlank()) {
            DATA.animationPackTypeSelections.remove(key);
        } else {
            DATA.animationPackTypeSelections.put(key, optionId.trim().toLowerCase(Locale.ROOT));
        }
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static void clearAnimationPackTypeSelections(String slotId) {
        if (slotId == null || slotId.isBlank() || DATA == null || DATA.animationPackTypeSelections == null) {
            return;
        }
        String prefix = slotId + "|";
        DATA.animationPackTypeSelections.entrySet().removeIf(entry -> {
            return (entry == null || entry.getKey() == null || !((String) entry.getKey()).startsWith(prefix)) ? false : true;
        });
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static void clearAnimationPackSelections() {
        if (DATA == null) {
            DATA = new Data();
        }
        if (DATA.animationPackSelections == null) {
            DATA.animationPackSelections = new LinkedHashMap();
        } else {
            DATA.animationPackSelections.clear();
        }
        if (DATA.animationPackTypeSelections == null) {
            DATA.animationPackTypeSelections = new LinkedHashMap();
        } else {
            DATA.animationPackTypeSelections.clear();
        }
        if (DATA.mixSpecificForceOverride == null) {
            DATA.mixSpecificForceOverride = new LinkedHashMap();
        } else {
            DATA.mixSpecificForceOverride.clear();
        }
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static void replaceAnimationPackSelections(Map<String, String> selections, Map<String, String> typeSelections, Map<String, Boolean> forceOverrides) {
        LinkedHashMap linkedHashMap;
        LinkedHashMap linkedHashMap2;
        LinkedHashMap linkedHashMap3;
        if (DATA == null) {
            DATA = new Data();
        }
        Data data = DATA;
        if (selections == null) {
            linkedHashMap = new LinkedHashMap();
        } else {
            linkedHashMap = new LinkedHashMap(selections);
        }
        data.animationPackSelections = linkedHashMap;
        Data data2 = DATA;
        if (typeSelections == null) {
            linkedHashMap2 = new LinkedHashMap();
        } else {
            linkedHashMap2 = new LinkedHashMap(typeSelections);
        }
        data2.animationPackTypeSelections = linkedHashMap2;
        Data data3 = DATA;
        if (forceOverrides == null) {
            linkedHashMap3 = new LinkedHashMap();
        } else {
            linkedHashMap3 = new LinkedHashMap(forceOverrides);
        }
        data3.mixSpecificForceOverride = linkedHashMap3;
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static boolean isMixSpecificForceOverride(String slotId) {
        if (slotId == null || slotId.isBlank() || DATA == null || DATA.mixSpecificForceOverride == null) {
            return false;
        }
        return Boolean.TRUE.equals(DATA.mixSpecificForceOverride.get(slotId));
    }

    public static void setMixSpecificForceOverride(String slotId, boolean forced) {
        if (slotId == null || slotId.isBlank()) {
            return;
        }
        if (DATA == null) {
            DATA = new Data();
        }
        if (DATA.mixSpecificForceOverride == null) {
            DATA.mixSpecificForceOverride = new LinkedHashMap();
        }
        if (forced) {
            DATA.mixSpecificForceOverride.put(slotId, true);
        } else {
            DATA.mixSpecificForceOverride.remove(slotId);
        }
        DATA = sanitize(DATA);
        write(getConfigFile(), DATA);
    }

    public static void applyAndSave(Data data) {
        if (data == null) {
            return;
        }
        DATA = sanitize(data);
        write(getConfigFile(), DATA);
    }

    private static void write(Path file, Data data) {
        try {
            FileWriter writer = new FileWriter(file.toFile());
            try {
                GSON.toJson(data, writer);
                writer.close();
            } finally {
            }
        } catch (Exception e) {
        }
    }

    private static Path getConfigFile() {
        return getConfigDirectory().resolve(Punchy.MOD_ID).resolve(FILE_NAME);
    }

    private static Path getConfigDirectory() {
        class_310 client = class_310.method_1551();
        if (client != null) {
            return client.field_1697.toPath().resolve("config");
        }
        return Path.of("config", new String[0]);
    }

    private static Data sanitize(Data data) {
        if (data == null) {
            return new Data();
        }
        data.animationSpeed = class_3532.method_15363(data.animationSpeed, 0.5f, 7.0f);
        data.firstPersonModelHidePitch = class_3532.method_15363(data.firstPersonModelHidePitch, 0.0f, 90.0f);
        data.sprintArmSwingIntensity = class_3532.method_15363(data.sprintArmSwingIntensity, -1.0f, 1.0f);
        data.sprintArmSwingSpeed = class_3532.method_15363(data.sprintArmSwingSpeed, -1.0f, 1.0f);
        if (data.itemBlacklist == null) {
            data.itemBlacklist = new ArrayList();
        }
        if (data.blacklistApplyDualHanded == null) {
            data.blacklistApplyDualHanded = new LinkedHashMap();
        }
        if (data.animationPackSelections == null) {
            data.animationPackSelections = new LinkedHashMap();
        }
        if (data.animationPackTypeSelections == null) {
            data.animationPackTypeSelections = new LinkedHashMap();
        }
        if (data.mixSpecificForceOverride == null) {
            data.mixSpecificForceOverride = new LinkedHashMap();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String entry : data.itemBlacklist) {
            if (entry != null) {
                String trimmed = entry.trim().toLowerCase(Locale.ROOT);
                if (trimmed.endsWith(":")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        data.itemBlacklist = new ArrayList(normalized);
        LinkedHashMap<String, Boolean> normalizedDualInput = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry2 : data.blacklistApplyDualHanded.entrySet()) {
            if (entry2 != null && entry2.getKey() != null) {
                String key = entry2.getKey().trim().toLowerCase(Locale.ROOT);
                if (key.endsWith(":")) {
                    key = key.substring(0, key.length() - 1);
                }
                if (!key.isEmpty()) {
                    normalizedDualInput.put(key, Boolean.valueOf(Boolean.TRUE.equals(entry2.getValue())));
                }
            }
        }
        LinkedHashMap<String, Boolean> normalizedDual = new LinkedHashMap<>();
        for (String entry3 : data.itemBlacklist) {
            normalizedDual.put(entry3, Boolean.valueOf(Boolean.TRUE.equals(normalizedDualInput.get(entry3))));
        }
        data.blacklistApplyDualHanded = normalizedDual;
        LinkedHashMap<String, String> normalizedPackSelections = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry4 : data.animationPackSelections.entrySet()) {
            if (entry4 != null && entry4.getKey() != null && entry4.getValue() != null) {
                String animation = entry4.getKey().trim();
                String option = entry4.getValue().trim().toLowerCase(Locale.ROOT);
                if (!animation.isEmpty() && !option.isEmpty()) {
                    normalizedPackSelections.put(animation, option);
                }
            }
        }
        data.animationPackSelections = normalizedPackSelections;
        LinkedHashMap<String, String> normalizedTypePackSelections = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry5 : data.animationPackTypeSelections.entrySet()) {
            if (entry5 != null && entry5.getKey() != null && entry5.getValue() != null) {
                String key2 = entry5.getKey().trim().toLowerCase(Locale.ROOT);
                String option2 = entry5.getValue().trim().toLowerCase(Locale.ROOT);
                if (!key2.isEmpty() && !option2.isEmpty() && key2.contains("|")) {
                    normalizedTypePackSelections.put(key2, option2);
                }
            }
        }
        data.animationPackTypeSelections = normalizedTypePackSelections;
        LinkedHashMap<String, Boolean> normalizedSpecificForce = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry6 : data.mixSpecificForceOverride.entrySet()) {
            if (entry6 != null && entry6.getKey() != null) {
                String key3 = entry6.getKey().trim();
                if (!key3.isEmpty() && Boolean.TRUE.equals(entry6.getValue())) {
                    normalizedSpecificForce.put(key3, true);
                }
            }
        }
        data.mixSpecificForceOverride = normalizedSpecificForce;
        return data;
    }

    private static String findBlacklistMatch(class_1799 stack) {
        class_2960 id;
        if (DATA == null || DATA.itemBlacklist == null || stack == null || stack.method_7960() || (id = class_7923.field_41178.method_10221(stack.method_7909())) == null) {
            return null;
        }
        String key = id.toString().toLowerCase(Locale.ROOT);
        String namespace = id.method_12836().toLowerCase(Locale.ROOT);
        for (String entry : DATA.itemBlacklist) {
            if (matchesBlacklistEntry(entry, key, namespace)) {
                return entry;
            }
        }
        return null;
    }
}
